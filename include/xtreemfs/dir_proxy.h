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


#ifndef _XTREEMFS_DIR_PROXY_H_
#define _XTREEMFS_DIR_PROXY_H_

#include "xtreemfs/proxy.h"

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "xtreemfs/interfaces/dir_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif

#include <map>
#include <string>


namespace xtreemfs
{
  class DIRProxy : public Proxy<org::xtreemfs::interfaces::DIRInterface>
  {
  public:
    static yidl::runtime::auto_Object<DIRProxy>
    create
    (
      const YIELD::ipc::URI& absolute_uri,
      uint16_t concurrency_level = CONCURRENCY_LEVEL_DEFAULT,
      uint32_t flags = 0,
      YIELD::platform::auto_Log log = NULL,
      const YIELD::platform::Time& operation_timeout =
        OPERATION_TIMEOUT_DEFAULT,
      uint8_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT,
      YIELD::ipc::auto_SSLContext ssl_context = NULL,
      auto_UserCredentialsCache user_credentials_cache = NULL
    );

    yidl::runtime::auto_Object<org::xtreemfs::interfaces::AddressMappingSet>
    getAddressMappingsFromUUID( const std::string& uuid );

    YIELD::ipc::auto_URI
    getVolumeURIFromVolumeName( const std::string& volume_name );

  private:
    DIRProxy
    (
      uint16_t concurrency_level,
      uint32_t flags,
      YIELD::platform::auto_Log log,
      const YIELD::platform::Time& operation_timeout,
      YIELD::ipc::auto_SocketAddress peername,
      uint8_t reconnect_tries_max,
      YIELD::ipc::auto_SocketFactory socket_factory,
      auto_UserCredentialsCache user_credentials_cache
    )
    : Proxy<org::xtreemfs::interfaces::DIRInterface>
      (
        concurrency_level,
        flags,
        log,
        operation_timeout,
        peername,
        reconnect_tries_max,
        socket_factory,
        user_credentials_cache
      )
    { }

    ~DIRProxy();


    class CachedAddressMappings
      : public org::xtreemfs::interfaces::AddressMappingSet
    {
    public:
      CachedAddressMappings
      (
        const org::xtreemfs::interfaces::AddressMappingSet& address_mappings,
        uint32_t ttl_s
      )
      : org::xtreemfs::interfaces::AddressMappingSet( address_mappings ),
        ttl_s( ttl_s )
      { }

      const YIELD::platform::Time& get_creation_time() const
      {
        return creation_time;
      }

      uint32_t get_ttl_s() const
      {
        return ttl_s;
      }

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( CachedAddressMappings, 0 );

    private:
      ~CachedAddressMappings() { }

      uint32_t ttl_s;

      YIELD::platform::Time creation_time;
    };

    std::map<std::string, CachedAddressMappings*> uuid_to_address_mappings_cache;
    YIELD::platform::Mutex uuid_to_address_mappings_cache_lock;
  };

  typedef yidl::runtime::auto_Object<DIRProxy> auto_DIRProxy;
};

#endif
