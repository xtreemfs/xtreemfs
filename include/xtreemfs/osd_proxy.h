// Copyright 2009 Minor Gordon.
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
  class OSDProxy : public Proxy<OSDProxy, org::xtreemfs::interfaces::OSDInterface>
  {
  public:
    static yidl::runtime::auto_Object<OSDProxy> create( const YIELD::ipc::URI& absolute_uri,
                                               uint32_t flags = 0,
                                               YIELD::platform::auto_Log log = NULL,
                                               const YIELD::platform::Time& operation_timeout = YIELD::ipc::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::OPERATION_TIMEOUT_DEFAULT,
                                               YIELD::ipc::auto_SSLContext ssl_context = NULL )
    {
      return YIELD::ipc::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::create<OSDProxy>( absolute_uri, flags, log, operation_timeout, ssl_context );
    }

    // yidl::runtime::Object
    OSDProxy& incRef() { return yidl::runtime::Object::incRef( *this ); }

    // YIELD::concurrency::EventTarget
    void send( YIELD::concurrency::Event& ev )
    {
      // Bypass Proxy so no credentials are attached; the credentials for OSD operations are in FileCredentials passed explicitly to the operation
      YIELD::ipc::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::send( ev );
    }

  private:
    friend class YIELD::ipc::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>;

    OSDProxy( const YIELD::ipc::URI& absolute_uri, uint32_t flags, YIELD::platform::auto_Log log, const YIELD::platform::Time& operation_timeout, YIELD::ipc::Socket::auto_Address peer_sockaddr, YIELD::ipc::auto_SSLContext ssl_context )
      : Proxy<OSDProxy, org::xtreemfs::interfaces::OSDInterface>( absolute_uri, flags, log, operation_timeout, peer_sockaddr, ssl_context )
    { }

    ~OSDProxy() { }
  };

  typedef yidl::runtime::auto_Object<OSDProxy> auto_OSDProxy;
};


bool operator>( const org::xtreemfs::interfaces::OSDWriteResponse& left, const org::xtreemfs::interfaces::OSDWriteResponse& right );

#endif
