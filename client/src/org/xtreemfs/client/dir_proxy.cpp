// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/dir_proxy.h"
#include "org/xtreemfs/interfaces/exceptions.h"
#include "policy_container.h"
using namespace org::xtreemfs::client;


DIRProxy::DIRProxy( const YIELD::SocketAddress& peer_sockaddr, YIELD::auto_Object<YIELD::SSLContext> ssl_context, YIELD::auto_Object<YIELD::Log> log )
  : YIELD::ONCRPCClient( peer_sockaddr, ssl_context, log )
{
  dir_interface.registerObjectFactories( *object_factories );
  org::xtreemfs::interfaces::Exceptions().registerObjectFactories( *object_factories );
  policies = new PolicyContainer;
}

DIRProxy::~DIRProxy()
{
  YIELD::Object::decRef( policies );
  for ( std::map<std::string, CachedAddressMappingURI*>::iterator uuid_to_uri_i = uuid_to_uri_cache.begin(); uuid_to_uri_i != uuid_to_uri_cache.end(); uuid_to_uri_i++ )
    delete uuid_to_uri_i->second;
}

YIELD::auto_Object<YIELD::Request> DIRProxy::createProtocolRequest( YIELD::auto_Object<> body )
{
  YIELD::auto_Object<org::xtreemfs::interfaces::UserCredentials> user_credentials = new org::xtreemfs::interfaces::UserCredentials;
  policies->getCurrentUserCredentials( *user_credentials.get() );
  return new YIELD::ONCRPCRequest( org::xtreemfs::interfaces::ONCRPC_AUTH_FLAVOR, user_credentials.release(), body, get_log() );
}

YIELD::URI DIRProxy::getURIFromUUID( const std::string& uuid )
{
  if ( uuid_to_uri_cache_lock.try_acquire() )
  {
    std::map<std::string, CachedAddressMappingURI*>::iterator uuid_to_uri_i = uuid_to_uri_cache.find( uuid );
    if ( uuid_to_uri_i != uuid_to_uri_cache.end() )
    {
      CachedAddressMappingURI* uri = uuid_to_uri_i->second;
      double uri_age_s = YIELD::Time::getCurrentUnixTimeS()- uri->get_creation_epoch_time_s();
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
    uri_str << address_mapping.get_protocol() << "://";
    if ( address_mapping.get_address().find( ':' ) != std::string::npos ) // IPv6
    {
      uri_str << "[";
      std::string::size_type percent_sign_pos = address_mapping.get_address().find( '%' );
      if ( percent_sign_pos != std::string::npos ) // Cut off the "zone index, %n at the end of the IPv6 representation
        uri_str << address_mapping.get_address().substr( 0, percent_sign_pos );
      else
        uri_str << address_mapping.get_address();
      uri_str << "]";
    }
    else
      uri_str << address_mapping.get_address();
    uri_str << ":" << address_mapping.get_port();
//    uri_str << address_mapping.get_protocol() << "://" << "localhost" << ":" << address_mapping.get_port();
    CachedAddressMappingURI* uri = new CachedAddressMappingURI( uri_str.str(), address_mapping.get_ttl_s() );
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

