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
    static yidl::auto_Object<OSDProxyMux> create( yidl::auto_Object<DIRProxy> dir_proxy,
                                                   uint32_t flags = 0,
                                                   YIELD::auto_Log log = NULL,
                                                   const YIELD::Time& operation_timeout = YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::OPERATION_TIMEOUT_DEFAULT,
                                                   YIELD::auto_SSLContext ssl_context = NULL )
    {
      return new OSDProxyMux( dir_proxy, flags, log, operation_timeout, ssl_context );
    }

    // yidl::Object
    OSDProxyMux& incRef() { return yidl::Object::incRef( *this ); }

    // YIELD::EventHandler
     void handleEvent( YIELD::Event& );

  private:
    OSDProxyMux( yidl::auto_Object<DIRProxy> dir_proxy, uint32_t flags, YIELD::auto_Log log, const YIELD::Time& operation_timeout, YIELD::auto_SSLContext ssl_context );
    ~OSDProxyMux();

    yidl::auto_Object<DIRProxy> dir_proxy;
    uint32_t flags;
    YIELD::auto_Log log;
    YIELD::Time operation_timeout;
    YIELD::auto_SSLContext ssl_context;

    typedef std::map< std::string, std::pair<OSDProxy*, OSDProxy*> > OSDProxyMap;
    OSDProxyMap osd_proxies;
    YIELD::auto_StageGroup osd_proxy_stage_group;

    // Policies callbacks
    get_osd_ping_interval_s_t get_osd_ping_interval_s;
    select_file_replica_t select_file_replica;
    PolicyContainer* policy_container;

    yidl::auto_Object<OSDProxy> getTCPOSDProxy( OSDProxyRequest& osd_proxy_request, const org::xtreemfs::interfaces::FileCredentials& file_credentials, uint64_t object_number );
    yidl::auto_Object<OSDProxy> getTCPOSDProxy( const std::string& osd_uuid );

    // org::xtreemfs::interfaces::OSDInterface
    void handlereadRequest( readRequest& req );
    void handletruncateRequest( truncateRequest& req );
    void handleunlinkRequest( unlinkRequest& req );
    void handlewriteRequest( writeRequest& req );
    void handlextreemfs_lock_acquireRequest( xtreemfs_lock_acquireRequest& req );
    void handlextreemfs_lock_checkRequest( xtreemfs_lock_checkRequest& req );
    void handlextreemfs_lock_releaseRequest( xtreemfs_lock_releaseRequest& req );

    class PingRequest;
    class PingResponse;
    class PingResponseTarget;
    class PingTimer;
    class ReadResponseTarget;
    class TruncateResponseTarget;
  };

  typedef yidl::auto_Object<OSDProxyMux> auto_OSDProxyMux;
};

#endif
