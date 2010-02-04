// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_NETTEST_PROXY_H_
#define _XTREEMFS_NETTEST_PROXY_H_

#include "xtreemfs/proxy.h"

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "nettest_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif


namespace nettest_xtreemfs
{
  class NettestProxy 
    : public xtreemfs::Proxy<org::xtreemfs::interfaces::NettestInterface>
  {
  public:
    static yidl::runtime::auto_Object<NettestProxy> 
    create
    ( 
      const YIELD::ipc::URI& absolute_uri,
      uint16_t concurrency_level = CONCURRENCY_LEVEL_DEFAULT,
      uint32_t flags = 0,
      YIELD::platform::auto_Log log = NULL,
      const YIELD::platform::Time& operation_timeout = 
        OPERATION_TIMEOUT_DEFAULT,
      uint8_t reconnect_tries_max = 
        RECONNECT_TRIES_MAX_DEFAULT,
      YIELD::ipc::auto_SSLContext ssl_context = NULL
    )
    {
      if ( absolute_uri.get_port() != 0 )
      {
        return new NettestProxy
        ( 
          concurrency_level,
          flags, 
          log, 
          operation_timeout, 
          YIELD::ipc::SocketAddress::create( absolute_uri ), 
          reconnect_tries_max,
          createSocketFactory( absolute_uri, ssl_context )
        );
      }
      else
        throw YIELD::platform::Exception( "must specify port in URI" );

    }

    // xtreemfs::Proxy
    void
    getCurrentUserCredentials
    ( 
      org::xtreemfs::interfaces::UserCredentials& 
    )
    { }

  private:
    NettestProxy
    ( 
      uint16_t concurrency_level,
      uint32_t flags, 
      YIELD::platform::auto_Log log, 
      const YIELD::platform::Time& operation_timeout,
      YIELD::ipc::auto_SocketAddress peername, 
      uint8_t reconnect_tries_max,
      YIELD::ipc::auto_SocketFactory socket_factory
    )
      : xtreemfs::Proxy<org::xtreemfs::interfaces::NettestInterface>
        ( 
          concurrency_level, 
          flags, 
          log, 
          operation_timeout, 
          peername, 
          reconnect_tries_max, 
          socket_factory,
          NULL // user_credentials_cache
        )
    { }

    ~NettestProxy() { }
  };

  typedef yidl::runtime::auto_Object<NettestProxy> auto_NettestProxy;
};

#endif
