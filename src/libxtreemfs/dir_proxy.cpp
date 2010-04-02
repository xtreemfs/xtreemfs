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
#include "xtreemfs/options.h"
using org::xtreemfs::interfaces::ServiceSet;
using org::xtreemfs::interfaces::ServiceDataMap;
using namespace xtreemfs;


class DIRProxy::CachedAddressMappings : public AddressMappingSet
{
public:
  CachedAddressMappings
  (
    const AddressMappingSet& address_mappings,
    uint32_t ttl_s
  )
  : AddressMappingSet( address_mappings ),
    ttl_s( ttl_s )
  { }

  const Time& get_creation_time() const { return creation_time; }
  uint32_t get_ttl_s() const { return ttl_s; }

  // yidl::runtime::Object
  CachedAddressMappings& inc_ref() { return Object::inc_ref( *this ); }

private:
  Time creation_time;
  uint32_t ttl_s;
};


DIRProxy::DIRProxy( EventHandler& request_handler )
  : DIRInterfaceProxy( request_handler )
{ }

DIRProxy::~DIRProxy()
{
  for
  (
    std::map<string, CachedAddressMappings*>::iterator
      uuid_to_address_mappings_i = uuid_to_address_mappings_cache.begin();
    uuid_to_address_mappings_i != uuid_to_address_mappings_cache.end();
    uuid_to_address_mappings_i++
  )
    CachedAddressMappings::dec_ref( *uuid_to_address_mappings_i->second );
}

DIRProxy& DIRProxy::create( const Options& options )
{
  if ( options.get_uri() != NULL )    
  {
    return create
           (
             *options.get_uri(),
             options.get_error_log(),
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
             options.get_ssl_context(),
#endif
             options.get_trace_log()
           );
  }
  else
    throw Exception( "must specify a DIR[/volume] URI" );
}

DIRProxy&
DIRProxy::create
(
  const URI& absolute_uri,
  Log* error_log,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
  SSLContext* ssl_context,
#endif
  Log* trace_log
)
{
  return *new DIRProxy
              (
                createONCRPCClient
                (
                  absolute_uri,
                  *new org::xtreemfs::interfaces::DIRInterfaceMessageFactory,
                  ONC_RPC_PORT_DEFAULT,
                  0x20000000 + TAG,
                  TAG,
                  error_log,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
                  ssl_context,
#endif
                  trace_log
                )
              );
}

AddressMappingSet&
DIRProxy::getAddressMappingsFromUUID
(
  const string& uuid
)
{
  if ( uuid_to_address_mappings_cache_lock.try_acquire() )
  {
    std::map<string, CachedAddressMappings*>::iterator
      uuid_to_address_mappings_i = uuid_to_address_mappings_cache.find( uuid );

    if ( uuid_to_address_mappings_i != uuid_to_address_mappings_cache.end() )
    {
      CachedAddressMappings* cached_address_mappings =
        uuid_to_address_mappings_i->second;

      uint32_t cached_address_mappings_age_s 
        = static_cast<uint32_t> 
          (
            ( 
              Time() -
              cached_address_mappings->get_creation_time()
            ).as_unix_time_s()
          );

      if
      (
        cached_address_mappings_age_s <
        cached_address_mappings->get_ttl_s()
      )
      {
        cached_address_mappings->inc_ref();
        uuid_to_address_mappings_cache_lock.release();
        return *cached_address_mappings;
      }
      else
      {
        CachedAddressMappings::dec_ref( cached_address_mappings );
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
    uuid_to_address_mappings_cache[uuid] = &cached_address_mappings->inc_ref();
    uuid_to_address_mappings_cache_lock.release();

    return *cached_address_mappings;
  }
  else
    throw Exception( "could not find address mappings for UUID" );
}

URI DIRProxy::getVolumeURIFromVolumeName( const string& volume_name_utf8 )
{
  ServiceSet services;
  xtreemfs_service_get_by_name( volume_name_utf8, services  );
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
            if ( ( *address_mapping_i ).get_protocol() == "oncrpc" )
              return URI( ( *address_mapping_i ).get_uri() );
          }

          // Then GridSSL
          for
          (
            AddressMappingSet::const_iterator address_mapping_i = address_mappings->begin();
            address_mapping_i != address_mappings->end();
            address_mapping_i++
          )
          {
            if ( ( *address_mapping_i ).get_protocol() == "oncrpcg" )
              return URI( ( *address_mapping_i ).get_uri() );
          }

          // Then SSL
          for
          (
            AddressMappingSet::const_iterator address_mapping_i = address_mappings->begin();
            address_mapping_i != address_mappings->end();
            address_mapping_i++
          )
          {
            if ( ( *address_mapping_i ).get_protocol() == "oncrpcs" )
              return URI( ( *address_mapping_i ).get_uri() );
          }

          // Then UDP
          for
          (
            AddressMappingSet::const_iterator address_mapping_i = address_mappings->begin();
            address_mapping_i != address_mappings->end();
            address_mapping_i++
          )
          {
            if ( ( *address_mapping_i ).get_protocol() == "oncrpcu" )
              return URI( ( *address_mapping_i ).get_uri() );
          }
        }
      }
    }
  }

  throw Exception( "unknown volume" );
}
