#include "org/xtreemfs/client/dir_proxy.h"
#include "policy_container.h"
using namespace org::xtreemfs::client;


DIRProxy::DIRProxy( const YIELD::URI& uri ) 
  : Proxy( uri, org::xtreemfs::interfaces::DIRInterface::DEFAULT_ONCRPC_PORT, org::xtreemfs::interfaces::DIRInterface::DEFAULT_ONCRPCS_PORT )
{
  policies = new PolicyContainer;
  dir_interface.registerSerializableFactories( serializable_factories );
}

DIRProxy::~DIRProxy()
{
  delete policies;
  for ( std::map<std::string, CachedAddressMappingURI*>::iterator uuid_to_uri_i = uuid_to_uri_cache.begin(); uuid_to_uri_i != uuid_to_uri_cache.end(); uuid_to_uri_i++ )
    delete uuid_to_uri_i->second;
}

YIELD::auto_SharedObject<org::xtreemfs::interfaces::UserCredentials> DIRProxy::get_user_credentials() const
{
  return policies->get_user_credentials();
}

YIELD::URI DIRProxy::getURIFromUUID( const std::string& uuid )
{
  if ( uuid_to_uri_cache_lock.try_acquire() )
  {
    std::map<std::string, CachedAddressMappingURI*>::iterator uuid_to_uri_i = uuid_to_uri_cache.find( uuid );
    if ( uuid_to_uri_i != uuid_to_uri_cache.end() )
    {
      CachedAddressMappingURI* uri = uuid_to_uri_i->second;   
      double uri_age_s = YIELD::Time::getCurrentEpochTimeS()- uri->get_creation_epoch_time_s();
      if ( uri_age_s < uri->get_ttl_s() )
      {
        uuid_to_uri_cache_lock.release();
        return *uri;
      }
      else
      {
        delete uri;
        uuid_to_uri_cache.erase( uuid_to_uri_i );        
        uuid_to_uri_cache_lock.release();
      }
    }
    else
      uuid_to_uri_cache_lock.release();
  }

  org::xtreemfs::interfaces::AddressMappingSet address_mappings;
  dir_interface.xtreemfs_address_mappings_get( uuid, address_mappings, this );
  if ( !address_mappings.empty() )
  {
    const org::xtreemfs::interfaces::AddressMapping& address_mapping = address_mappings[0];
    std::ostringstream uri_str;
    uri_str << address_mapping.get_protocol() << "://" << address_mapping.get_address() << ":" << address_mapping.get_port();
    CachedAddressMappingURI* uri = new CachedAddressMappingURI( uri_str.str(), address_mapping.get_ttl() );  
    uuid_to_uri_cache_lock.acquire();
    uuid_to_uri_cache[uuid] = uri;
    uuid_to_uri_cache_lock.release();
    return *uri;
  }
  else 
    throw YIELD::Exception( "could not find address mapping for UUID" );
}

YIELD::URI DIRProxy::getVolumeURIFromVolumeName( const std::string& volume_name )
{
  org::xtreemfs::interfaces::ServiceSet services;
  dir_interface.xtreemfs_service_get_by_name( volume_name, services, this );
  if ( !services.empty() )
  {
    for ( org::xtreemfs::interfaces::ServiceSet::const_iterator service_i = services.begin(); service_i != services.end(); service_i++ )
    {
      const org::xtreemfs::interfaces::ServiceDataMap& data = ( *service_i ).get_data();
      for ( org::xtreemfs::interfaces::ServiceDataMap::const_iterator service_data_i = data.begin(); service_data_i != data.end(); service_data_i++ )
      {
        if ( service_data_i->first == "mrc" )
          return YIELD::URI( getURIFromUUID( service_data_i->second ) );
      }
    }

    throw YIELD::Exception( "unknown volume" );
  }
  else
    throw YIELD::Exception( "unknown volume" );
}

