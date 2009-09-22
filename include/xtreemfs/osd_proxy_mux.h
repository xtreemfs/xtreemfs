// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_OSD_PROXY_MUX_H_
#define _XTREEMFS_OSD_PROXY_MUX_H_

#include "xtreemfs/dir_proxy.h"
#include "xtreemfs/osd_proxy.h"


namespace xtreemfs
{
  class PolicyContainer;


  class OSDProxyMux : public org::xtreemfs::interfaces::OSDInterface
  {
  public:
    static yidl::runtime::auto_Object<OSDProxyMux> create( yidl::runtime::auto_Object<DIRProxy> dir_proxy,
                                                   uint32_t flags = 0,
                                                   YIELD::platform::auto_Log log = NULL,
                                                   const YIELD::platform::Time& operation_timeout = YIELD::ipc::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::OPERATION_TIMEOUT_DEFAULT,
                                                   YIELD::ipc::auto_SSLContext ssl_context = NULL )
    {
      return new OSDProxyMux( dir_proxy, flags, log, operation_timeout, ssl_context );
    }

    // yidl::runtime::Object
    OSDProxyMux& incRef() { return yidl::runtime::Object::incRef( *this ); }

  private:
    OSDProxyMux( yidl::runtime::auto_Object<DIRProxy> dir_proxy, uint32_t flags, YIELD::platform::auto_Log log, const YIELD::platform::Time& operation_timeout, YIELD::ipc::auto_SSLContext ssl_context );
    ~OSDProxyMux();

    auto_DIRProxy dir_proxy;
    uint32_t flags;
    YIELD::platform::auto_Log log;
    YIELD::platform::Time operation_timeout;
    YIELD::ipc::auto_SSLContext ssl_context;

    typedef std::map<std::string, OSDProxy*> OSDProxyMap;
    OSDProxyMap osd_proxies;
    YIELD::concurrency::auto_StageGroup osd_proxy_stage_group;

    auto_OSDProxy getOSDProxy( OSDProxyRequest& osd_proxy_request, const org::xtreemfs::interfaces::FileCredentials& file_credentials, uint64_t object_number );
    auto_OSDProxy getOSDProxy( const std::string& osd_uuid );

    // org::xtreemfs::interfaces::OSDInterface
    void handlereadRequest( readRequest& req );
    void handletruncateRequest( truncateRequest& req );
    void handleunlinkRequest( unlinkRequest& req );
    void handlewriteRequest( writeRequest& req );
    void handlextreemfs_lock_acquireRequest( xtreemfs_lock_acquireRequest& req );
    void handlextreemfs_lock_checkRequest( xtreemfs_lock_checkRequest& req );
    void handlextreemfs_lock_releaseRequest( xtreemfs_lock_releaseRequest& req );

    class ReadResponseTarget;
    class TruncateResponseTarget;
  };

  typedef yidl::runtime::auto_Object<OSDProxyMux> auto_OSDProxyMux;
};

#endif
