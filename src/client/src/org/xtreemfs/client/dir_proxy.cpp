// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/dir_proxy.h"
#include "org/xtreemfs/interfaces/exceptions.h"
#include "policy_container.h"
using namespace org::xtreemfs::client;


DIRProxy::DIRProxy( const YIELD::SocketAddress& peer_sockaddr, YIELD::auto_Object<YIELD::SocketFactory> socket_factory, YIELD::auto_Object<YIELD::Log> log )
  : YIELD::ONCRPCClient( peer_sockaddr, socket_factory, log )
{
  dir_interface.registerObjectFactories( *object_factories );
  org::xtreemfs::interfaces::Exceptions().registerObjectFactories( *object_factories );
  policies = new PolicyContainer;
}

DIRProxy::~DIRProxy()
{
  YIELD::Object::decRef( policies );
  for ( std::map<std::string, CachedAddressMappingURI*>::iterator uuid_to_uri_i = uuid_to_uri_cache.begin(); uuid_to_uri_i != uuid_to_uri_cache.end(); uuid_to_uri_i++ )
    YIELD::Object::decRef( *uuid_to_uri_i->second );
}

YIELD::auto_Object<YIELD::Request> DIRProxy::createProtocolRequest( YIELD::auto_Object<> body )
{
  YIELD::auto_Object<org::xtreemfs::interfaces::UserCredentials> user_credentials = new org::xtreemfs::interfaces::UserCredentials;
  policies->getCurrentUserCredentials( *user_credentials.get() );
  return new YIELD::ONCRPCRequest( org::xtreemfs::interfaces::ONCRPC_AUTH_FLAVOR, user_credentials.release(), body, get_log() );
}

YIELD::auto_Object<YIELD::URI> DIRProxy::getURIFromUUID( const std::string& uuid )
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
        uri->incRef();
        uuid_to_uri_cache_lock.release();
        return uri;
      }
      else
      {
        YIELD::Object::decRef( uri );
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
    CachedAddressMappingURI* uri = new CachedAddressMappingURI( address_mappings[0].get_uri(), address_mappings[0].get_ttl_s() );
    uuid_to_uri_cache_lock.acquire();
    uuid_to_uri_cache[uuid] = &uri->incRef();
    uuid_to_uri_cache_lock.release();
    return uri;
  }
  else
    throw YIELD::Exception( "could not find address mapping for UUID" );
}

YIELD::auto_Object<YIELD::URI> DIRProxy::getVolumeURIFromVolumeName( const std::string& volume_name )
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
          return getURIFromUUID( service_data_i->second );
      }
    }

    throw YIELD::Exception( "unknown volume" );
  }
  else
    throw YIELD::Exception( "unknown volume" );
}

