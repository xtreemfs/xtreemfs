// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_PROXY_H_
#define _XTREEMFS_PROXY_H_

#include "xtreemfs/grid_ssl_socket.h"
#include "xtreemfs/interfaces/constants.h"
#include "xtreemfs/proxy_exception_response.h"
#include "xtreemfs/interfaces/types.h"
#include "xtreemfs/user_credentials_cache.h"

#include "yield.h"


namespace xtreemfs
{
  class UserCredentialsCache;


  template <class InterfaceType>
  class Proxy : public YIELD::ipc::ONCRPCClient<InterfaceType>
  {
  public:
    const static uint32_t PROXY_FLAG_TRACE_IO = 
      YIELD::ipc::Client<YIELD::ipc::ONCRPCRequest, YIELD::ipc::ONCRPCResponse>
        ::CLIENT_FLAG_TRACE_IO;

    const static uint32_t PROXY_FLAG_TRACE_OPERATIONS = 
      YIELD::ipc::Client<YIELD::ipc::ONCRPCRequest, YIELD::ipc::ONCRPCResponse>
        ::CLIENT_FLAG_TRACE_OPERATIONS;

    const static uint32_t PROXY_FLAG_TRACE_AUTH = 8;

    // YIELD::concurrency::EventTarget
    virtual void send( YIELD::concurrency::Event& ev )
    {
      if ( InterfaceType::checkRequest( ev ) != NULL )
      {
        org::xtreemfs::interfaces::UserCredentials* user_credentials 
          = new org::xtreemfs::interfaces::UserCredentials;

        getCurrentUserCredentials( *user_credentials );

        YIELD::ipc::ONCRPCRequest* oncrpc_request =
            new YIELD::ipc::ONCRPCRequest
            ( 
              this->incRef(), 
              org::xtreemfs::interfaces::ONCRPC_AUTH_FLAVOR, 
              user_credentials, 
              ev 
            );

        YIELD::ipc::ONCRPCClient<InterfaceType>::send( *oncrpc_request );
      }
      else
        YIELD::ipc::ONCRPCClient<InterfaceType>::send( ev );

    }

  protected:
    Proxy
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
      : YIELD::ipc::ONCRPCClient<InterfaceType>
        ( 
          concurrency_level, 
          flags, 
          log, 
          operation_timeout, 
          peername, 
          reconnect_tries_max, 
          socket_factory
        ), 
        log( log ),
        user_credentials_cache( user_credentials_cache )
    { }

    virtual ~Proxy()
    { }

    static YIELD::ipc::auto_SocketFactory 
    createSocketFactory
    ( 
      const YIELD::ipc::URI& absolute_uri, 
      YIELD::ipc::auto_SSLContext ssl_context 
    )
    {
      if 
      ( 
        absolute_uri.get_scheme() 
          == org::xtreemfs::interfaces::ONCRPCG_SCHEME && 
        ssl_context != NULL 
      )
        return new GridSSLSocketFactory( ssl_context );

      else if 
      ( 
        absolute_uri.get_scheme() 
          == org::xtreemfs::interfaces::ONCRPCS_SCHEME &&
        ssl_context != NULL 
      )      
        return new YIELD::ipc::SSLSocketFactory( ssl_context );

      else if 
      ( 
        absolute_uri.get_scheme() == org::xtreemfs::interfaces::ONCRPCU_SCHEME
      )
        return new YIELD::ipc::UDPSocketFactory;

      else
        return new YIELD::ipc::TCPSocketFactory;
    }

    void 
    getCurrentUserCredentials
    ( 
      org::xtreemfs::interfaces::UserCredentials& out_user_credentials 
    )
    {
      if ( user_credentials_cache != NULL )
      {
        user_credentials_cache->getCurrentUserCredentials
        ( 
          out_user_credentials 
        );
      }
    }

  private:
    YIELD::platform::auto_Log log;
    auto_UserCredentialsCache user_credentials_cache;
  };
};

#endif
