// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_OSD_PROXY_H_
#define _XTREEMFS_OSD_PROXY_H_

#include "xtreemfs/proxy.h"
#include "xtreemfs/osd_proxy_request.h"
#include "xtreemfs/osd_proxy_response.h"

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "xtreemfs/interfaces/osd_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif


namespace xtreemfs
{
  class OSDProxy 
    : public Proxy<OSDProxy, org::xtreemfs::interfaces::OSDInterface>
  {
  public:
    static yidl::runtime::auto_Object<OSDProxy> 
      create
      ( 
        const YIELD::ipc::URI& absolute_uri,
        uint16_t concurrency_level 
          = CONCURRENCY_LEVEL_DEFAULT,
        uint32_t flags 
          = 0,
        YIELD::platform::auto_Log log 
          = NULL,
        const YIELD::platform::Time& operation_timeout = 
          OPERATION_TIMEOUT_DEFAULT,
        uint8_t reconnect_tries_max = 
          RECONNECT_TRIES_MAX_DEFAULT,
        YIELD::ipc::auto_SSLContext ssl_context 
          = NULL,
        auto_UserCredentialsCache user_credentials_cache 
          = NULL
      );

    // yidl::runtime::Object
    OSDProxy& incRef() { return yidl::runtime::Object::incRef( *this ); }

    // YIELD::concurrency::EventTarget
    void send( YIELD::concurrency::Event& ev )
    {
      // Bypass Proxy so no credentials are attached; 
      // the credentials for OSD operations are in FileCredentials passed 
      // explicitly to the operation
      YIELD::ipc::
        ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::send( ev );
    }

  private:
    OSDProxy
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
      : Proxy<OSDProxy, org::xtreemfs::interfaces::OSDInterface>
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

    ~OSDProxy() { }
  };

  typedef yidl::runtime::auto_Object<OSDProxy> auto_OSDProxy;
};


bool operator>
( 
  const org::xtreemfs::interfaces::OSDWriteResponse& left, 
  const org::xtreemfs::interfaces::OSDWriteResponse& right 
);

#endif
