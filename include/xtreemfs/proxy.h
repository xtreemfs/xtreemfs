// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_PROXY_H_
#define _XTREEMFS_PROXY_H_

#include "xtreemfs/proxy_exception_response.h"
#include "xtreemfs/interfaces/types.h"
#include "xtreemfs/user_credentials_cache.h"

#include "yield.h"


namespace xtreemfs
{
  class UserCredentialsCache;


  template <class ProxyType, class InterfaceType>
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
    virtual void send( YIELD::concurrency::Event& ev );

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
    );

    virtual ~Proxy()
    { }

    static YIELD::ipc::auto_SocketFactory 
    createSocketFactory
    ( 
      const YIELD::ipc::URI& absolute_uri, 
      YIELD::ipc::auto_SSLContext ssl_context 
    );

    void 
    getCurrentUserCredentials
    ( 
      org::xtreemfs::interfaces::UserCredentials& out_user_credentials 
    );

  private:
    YIELD::platform::auto_Log log;
    auto_UserCredentialsCache user_credentials_cache;
  };
};

#endif
