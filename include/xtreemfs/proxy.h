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


#ifndef _XTREEMFS_PROXY_H_
#define _XTREEMFS_PROXY_H_

#include "xtreemfs/grid_ssl_socket.h"
#include "xtreemfs/interfaces/constants.h"
#include "xtreemfs/interfaces/types.h"
#include "xtreemfs/user_credentials_cache.h"


namespace xtreemfs
{
  using org::xtreemfs::interfaces::ONCRPC_SCHEME;
  using org::xtreemfs::interfaces::ONCRPCG_SCHEME;
  using org::xtreemfs::interfaces::ONCRPCS_SCHEME;
  using org::xtreemfs::interfaces::ONCRPCU_SCHEME;

  using yidl::runtime::MarshallableObject;
  using yidl::runtime::MarshallableObjectFactory;

  using yield::ipc::ONCRPCClient;
  using yield::ipc::ONCRPCRequest;
  using yield::ipc::SocketFactory;
  using yield::ipc::SSLContext;
  using yield::ipc::URI;

  using yield::platform::iconv;
  using yield::platform::IOQueue;
  using yield::platform::Log;
  using yield::platform::Path;
  using yield::platform::SocketAddress;
  using yield::platform::Time;


  template
  <
    class InterfaceType,
    class InterfaceEventFactoryType,
    class InterfaceEventSenderType
  >
  class Proxy
    : public InterfaceEventSenderType,
      public yield::ipc::ONCRPCClient
  {
  protected:
    // Steals all references except for user_credentials_cache
    Proxy
    (
      uint16_t concurrency_level,
      uint32_t flags,
      IOQueue& io_queue,
      Log* log,
      const Time& operation_timeout,
      SocketAddress& peername,
      uint16_t reconnect_tries_max,
      SocketFactory& socket_factory,
      UserCredentialsCache* user_credentials_cache
    )
    : ONCRPCClient
      (
        concurrency_level,
        flags,
        io_queue,
        log,
        *new InterfaceEventFactoryType,
        operation_timeout,
        peername,
        0x20000000 + InterfaceType::TAG,
        reconnect_tries_max,
        socket_factory,
        InterfaceType::TAG
      )
    {
      InterfaceEventSenderType::set_event_target( *this );

      if ( user_credentials_cache != NULL )
        this->user_credentials_cache = Object::inc_ref( user_credentials_cache );
      else
        this->user_credentials_cache = new UserCredentialsCache;
    }

    virtual ~Proxy()
    {
      UserCredentialsCache::dec_ref( *user_credentials_cache );
    }

    // ONCRPCClient
    ONCRPCRequest& createONCRPCRequest( MarshallableObject& body )
    {
      UserCredentials* user_credentials;
      if ( user_credentials_cache != NULL )
        user_credentials = user_credentials_cache->getCurrentUserCredentials();
      else
        user_credentials = NULL;

      return *new ONCRPCRequest
                  (
                    body,
                    body.get_type_id(),
                    get_prog(),
                    get_vers(),
                    user_credentials
                  );
    }

    // Helper methods for subclasses
    static SocketAddress& createSocketAddress( const URI& absolute_uri )
    {
      const string& scheme = absolute_uri.get_scheme();

      URI checked_absolute_uri( absolute_uri );
      if ( checked_absolute_uri.get_port() == 0 )
      {
        if ( scheme == ONCRPCG_SCHEME )
          checked_absolute_uri.set_port( InterfaceType::ONCRPCG_PORT_DEFAULT );
        else if ( scheme == ONCRPCS_SCHEME )
          checked_absolute_uri.set_port( InterfaceType::ONCRPCS_PORT_DEFAULT );
        else if ( scheme == ONCRPCU_SCHEME )
          checked_absolute_uri.set_port( InterfaceType::ONCRPCU_PORT_DEFAULT );
        else
          checked_absolute_uri.set_port( InterfaceType::ONCRPC_PORT_DEFAULT );
      }

      return ONCRPCClient::createSocketAddress( checked_absolute_uri );
    }

    static SocketFactory&
    createSocketFactory
    (
      const URI& absolute_uri,
      SSLContext* ssl_context
    )
    {
      const string& scheme = absolute_uri.get_scheme();

      URI checked_absolute_uri( absolute_uri );
      if ( checked_absolute_uri.get_port() == 0 )
      {
        if ( scheme == ONCRPCG_SCHEME )
          checked_absolute_uri.set_port( InterfaceType::ONCRPCG_PORT_DEFAULT );
        else if ( scheme == ONCRPCS_SCHEME )
          checked_absolute_uri.set_port( InterfaceType::ONCRPCS_PORT_DEFAULT );
        else if ( scheme == ONCRPCU_SCHEME )
          checked_absolute_uri.set_port( InterfaceType::ONCRPCU_PORT_DEFAULT );
        else
          checked_absolute_uri.set_port( InterfaceType::ONCRPC_PORT_DEFAULT );
      }

      if ( scheme == ONCRPCG_SCHEME && ssl_context != NULL )
        return *new GridSSLSocketFactory( *ssl_context );
      else if ( scheme == ONCRPCS_SCHEME  && ssl_context != NULL )
        return *new yield::ipc::SSLSocketFactory( *ssl_context );
      else if ( scheme == ONCRPCU_SCHEME )
        return *new yield::ipc::UDPSocketFactory;
      else
        return *new yield::ipc::TCPSocketFactory;
    }

    UserCredentialsCache* get_user_credentials_cache() const
    {
      return user_credentials_cache;
    }

  private:
    UserCredentialsCache* user_credentials_cache;
  };
};

#endif
