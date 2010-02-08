// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the XtreemFS project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the XtreemFS project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#include "xtreemfs/dir_proxy.h"
using namespace org::xtreemfs::interfaces;
using namespace xtreemfs;


DIRProxy::~DIRProxy()
{
  for
  (
    std::map<std::string, CachedAddressMappings*>::iterator
      uuid_to_address_mappings_i = uuid_to_address_mappings_cache.begin();
    uuid_to_address_mappings_i != uuid_to_address_mappings_cache.end();
    uuid_to_address_mappings_i++
  )
    CachedAddressMappings::decRef( *uuid_to_address_mappings_i->second );
}

auto_DIRProxy
DIRProxy::create
(
  const YIELD::ipc::URI& absolute_uri,
  uint16_t concurrency_level,
  uint32_t flags,
  YIELD::platform::auto_Log log,
  const YIELD::platform::Time& operation_timeout,
  uint8_t reconnect_tries_max,
  YIELD::ipc::auto_SSLContext ssl_context,
  auto_UserCredentialsCache user_credentials_cache
)
{
  YIELD::ipc::URI checked_uri( absolute_uri );

  if ( checked_uri.get_port() == 0 )
  {
    if ( checked_uri.get_scheme() == ONCRPCG_SCHEME )
      checked_uri.set_port( ONCRPCG_PORT_DEFAULT );
    else if ( checked_uri.get_scheme() == ONCRPCS_SCHEME )
      checked_uri.set_port( ONCRPCS_PORT_DEFAULT );
    else if ( checked_uri.get_scheme() == ONCRPCU_SCHEME )
      checked_uri.set_port( ONCRPCU_PORT_DEFAULT );
    else
      checked_uri.set_port( ONCRPC_PORT_DEFAULT );
  }

  if ( user_credentials_cache == NULL )
    user_credentials_cache = new UserCredentialsCache;

  return new DIRProxy
  (
    concurrency_level,
    flags,
    log,
    operation_timeout,
    YIELD::ipc::SocketAddress::create( checked_uri ),
    reconnect_tries_max,
    createSocketFactory( checked_uri, ssl_context ),
    user_credentials_cache
  );
}

yidl::runtime::auto_Object<AddressMappingSet>
DIRProxy::getAddressMappingsFromUUID
(
  const std::string& uuid
)
{
  if ( uuid_to_address_mappings_cache_lock.try_acquire() )
  {
    std::map<std::string, CachedAddressMappings*>::iterator
      uuid_to_address_mappings_i = uuid_to_address_mappings_cache.find( uuid );

    if ( uuid_to_address_mappings_i != uuid_to_address_mappings_cache.end() )
    {
      CachedAddressMappings* cached_address_mappings =
        uuid_to_address_mappings_i->second;

      uint32_t cached_address_mappings_age_s =
        (
          YIELD::platform::Time()-
          cached_address_mappings->get_creation_time()
        ).as_unix_time_s();

      if
      (
        cached_address_mappings_age_s <
        cached_address_mappings->get_ttl_s()
      )
      {
        cached_address_mappings->incRef();
        uuid_to_address_mappings_cache_lock.release();
        return cached_address_mappings;
      }
      else
      {
        CachedAddressMappings::decRef( cached_address_mappings );
        uuid_to_address_mappings_cache.erase( uuid_to_address_mappings_i );
        uuid_to_address_mappings_cache_lock.release();
      }
    }
    else
      uuid_to_address_mappings_cache_lock.release();
  }

  AddressMappingSet address_mappings;
  xtreemfs_address_mappings_get( uuid, address_mappings );
  if ( !address_mappings.empty() )
  {
    CachedAddressMappings* cached_address_mappings =
      new CachedAddressMappings
      (
        address_mappings, address_mappings[0].get_ttl_s()
      );

    uuid_to_address_mappings_cache_lock.acquire();
    uuid_to_address_mappings_cache[uuid] = &cached_address_mappings->incRef();
    uuid_to_address_mappings_cache_lock.release();

    return cached_address_mappings;
  }
  else
    throw YIELD::platform::Exception( "could not find address mappings for UUID" );
}


YIELD::ipc::auto_URI
DIRProxy::getVolumeURIFromVolumeName
(
  const std::string& volume_name
)
{
  ServiceSet services;
  xtreemfs_service_get_by_name( volume_name, services );
  if ( !services.empty() )
  {
    for
    (
      ServiceSet::const_iterator service_i = services.begin();
      service_i != services.end();
      service_i++
    )
    {
      const ServiceDataMap& data =
        ( *service_i ).get_data();

      for
      (
        ServiceDataMap::const_iterator service_data_i = data.begin();
        service_data_i != data.end();
        service_data_i++
      )
      {
        if ( service_data_i->first == "mrc" )
        {
          yidl::runtime::auto_Object<AddressMappingSet>
            address_mappings =
              getAddressMappingsFromUUID( service_data_i->second );

          // Prefer TCP URIs first
          for
          (
            AddressMappingSet::const_iterator address_mapping_i = address_mappings->begin();
            address_mapping_i != address_mappings->end();
            address_mapping_i++
          )
          {
            if ( ( *address_mapping_i ).get_protocol() == ONCRPC_SCHEME )
              return new YIELD::ipc::URI( ( *address_mapping_i ).get_uri() );
          }

          // Then GridSSL
          for
          (
            AddressMappingSet::const_iterator address_mapping_i = address_mappings->begin();
            address_mapping_i != address_mappings->end();
            address_mapping_i++
          )
          {
            if ( ( *address_mapping_i ).get_protocol() == ONCRPCG_SCHEME )
              return new YIELD::ipc::URI( ( *address_mapping_i ).get_uri() );
          }

          // Then SSL
          for
          (
            AddressMappingSet::const_iterator address_mapping_i = address_mappings->begin();
            address_mapping_i != address_mappings->end();
            address_mapping_i++
          )
          {
            if ( ( *address_mapping_i ).get_protocol() == ONCRPCS_SCHEME )
              return new YIELD::ipc::URI( ( *address_mapping_i ).get_uri() );
          }

          // Then UDP
          for
          (
            AddressMappingSet::const_iterator address_mapping_i = address_mappings->begin();
            address_mapping_i != address_mappings->end();
            address_mapping_i++
          )
          {
            if ( ( *address_mapping_i ).get_protocol() == ONCRPCU_SCHEME )
              return new YIELD::ipc::URI( ( *address_mapping_i ).get_uri() );
          }
        }
      }
    }
  }

  throw YIELD::platform::Exception( "unknown volume" );
}
