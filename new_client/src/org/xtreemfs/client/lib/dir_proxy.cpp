#include "org/xtreemfs/client/dir_proxy.h"
using namespace org::xtreemfs::client;


DIRProxy::DIRProxy( const YIELD::URI& uri, uint8_t reconnect_tries_max, uint32_t flags )
{
  Proxy::init( uri, reconnect_tries_max, flags );
  if ( this->uri->getPort() == 0 )
  {
    if ( strcmp( this->uri->getScheme(), org::xtreemfs::interfaces::ONCRPC_SCHEME ) == 0 )
      this->uri->setPort( org::xtreemfs::interfaces::DIRInterface::DEFAULT_ONCRPC_PORT );
    else if ( strcmp( this->uri->getScheme(), org::xtreemfs::interfaces::ONCRPCS_SCHEME ) == 0 )
      this->uri->setPort( org::xtreemfs::interfaces::DIRInterface::DEFAULT_ONCRPCS_PORT );
    else
      YIELD::DebugBreak();
  }
  xtreemfs::interfaces::DIRInterface::registerSerializableFactories( serializable_factories );
}

DIRProxy::~DIRProxy()
{
  for ( std::map<std::string, CachedAddressMappingURI*>::iterator uuid_to_uri_i = uuid_to_uri_cache.begin(); uuid_to_uri_i != uuid_to_uri_cache.end(); uuid_to_uri_i++ )
    delete uuid_to_uri_i->second;
}

YIELD::URI DIRProxy::get_uri_from_uuid( const std::string& uuid, uint64_t timeout_ms )
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
  this->address_mappings_get( uuid, address_mappings, timeout_ms );
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
