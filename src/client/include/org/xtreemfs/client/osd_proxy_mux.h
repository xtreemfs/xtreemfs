// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ORG_XTREEMFS_CLIENT_OSD_PROXY_MUX_H_
#define _ORG_XTREEMFS_CLIENT_OSD_PROXY_MUX_H_

#include "org/xtreemfs/client/dir_proxy.h"
#include "org/xtreemfs/client/osd_proxy.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class OSDProxyMux : public org::xtreemfs::interfaces::OSDInterface
      {
      public:
        static YIELD::auto_Object<OSDProxyMux> create( YIELD::auto_Object<DIRProxy> dir_proxy,
                                                       uint32_t flags = 0,
                                                       YIELD::auto_Log log = NULL,
                                                       const YIELD::Time& operation_timeout = YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::OPERATION_TIMEOUT_DEFAULT,
                                                       YIELD::auto_SSLContext ssl_context = NULL,
                                                       YIELD::auto_StageGroup stage_group = NULL )
        {
          return new OSDProxyMux( dir_proxy, flags, log, operation_timeout, ssl_context, stage_group );
        }

        // YIELD::Object
        OSDProxyMux& incRef() { return YIELD::Object::incRef( *this ); }

        // YIELD::EventHandler
        // void handleEvent( YIELD::Event& );

      private:
        OSDProxyMux( YIELD::auto_Object<DIRProxy> dir_proxy, uint32_t flags, YIELD::auto_Log log, const YIELD::Time& operation_timeout, YIELD::auto_SSLContext ssl_context, YIELD::auto_StageGroup stage_group );
        ~OSDProxyMux();

        YIELD::auto_Object<DIRProxy> dir_proxy;        
        uint32_t flags;
        YIELD::auto_Log log;
        YIELD::Time operation_timeout;
        YIELD::auto_SSLContext ssl_context;
        YIELD::auto_StageGroup stage_group;

        typedef std::map< std::string, std::pair<OSDProxy*, OSDProxy*> > OSDProxyMap;
        OSDProxyMap osd_proxies;

        // Policies callbacks
        get_osd_ping_interval_s_t get_osd_ping_interval_s;
        select_file_replica_t select_file_replica;
        std::vector<YIELD::SharedLibrary*> policy_shared_libraries;

        YIELD::auto_Object<OSDProxy> getTCPOSDProxy( OSDProxyRequest& osd_proxy_request, const org::xtreemfs::interfaces::FileCredentials& file_credentials, uint64_t object_number );
        YIELD::auto_Object<OSDProxy> getTCPOSDProxy( const std::string& osd_uuid );        
        //void pingOSD( YIELD::auto_Object<OSDProxy> udp_osd_proxy );

        // org::xtreemfs::interfaces::OSDInterface
        void handlereadRequest( readRequest& req );
        void handletruncateRequest( truncateRequest& req );
        void handleunlinkRequest( unlinkRequest& req );
        void handlewriteRequest( writeRequest& req );

        // Response targets
        class ReadResponseTarget;
        class TruncateResponseTarget;
      };
    };
  };
};

#endif
