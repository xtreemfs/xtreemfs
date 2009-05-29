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
        template <class StageGroupType>
        static YIELD::auto_Object<OSDProxyMux> create( YIELD::auto_Object<DIRProxy> dir_proxy,
                                                       YIELD::auto_Object<StageGroupType> stage_group,
                                                       YIELD::auto_Object<YIELD::Log> log = NULL,
                                                       const YIELD::Time& operation_timeout = YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::OPERATION_TIMEOUT_DEFAULT,
                                                       uint8_t reconnect_tries_max = YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::RECONNECT_TRIES_MAX_DEFAULT,
                                                       YIELD::auto_Object<YIELD::SSLContext> ssl_context = NULL )
        {
          YIELD::auto_Object<YIELD::FDAndInternalEventQueue> fd_event_queue = new YIELD::FDAndInternalEventQueue;
          YIELD::auto_Object<OSDProxyMux> osd_proxy_mux = new OSDProxyMux( dir_proxy, fd_event_queue, log, operation_timeout, reconnect_tries_max, ssl_context, stage_group );                                                                            
          stage_group->createStage( osd_proxy_mux->incRef(), 1, fd_event_queue->incRef() );
          return osd_proxy_mux;
        }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( OSDProxyMux, 0 );

        // YIELD::EventHandler
        void handleEvent( YIELD::Event& );

      private:
        OSDProxyMux( YIELD::auto_Object<DIRProxy> dir_proxy, YIELD::auto_Object<YIELD::FDAndInternalEventQueue> fd_event_queue, YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::SSLContext> ssl_context, YIELD::auto_Object<YIELD::StageGroup> stage_group );
        ~OSDProxyMux();

        YIELD::auto_Object<DIRProxy> dir_proxy;
        YIELD::auto_Object<YIELD::FDAndInternalEventQueue> fd_event_queue;
        YIELD::auto_Object<YIELD::Log> log;
        YIELD::Time operation_timeout;
        uint8_t reconnect_tries_max;
        YIELD::auto_Object<YIELD::SSLContext> ssl_context;
        YIELD::auto_Object<YIELD::StageGroup> stage_group;

        typedef std::map< std::string, std::pair<OSDProxy*, OSDProxy*> > OSDProxyMap;
        OSDProxyMap osd_proxies;

        // Policies callbacks
        get_osd_ping_interval_s_t get_osd_ping_interval_s;
        select_file_replica_t select_file_replica;
        std::vector<YIELD::SharedLibrary*> policy_shared_libraries;


        YIELD::auto_Object<OSDProxy> getTCPOSDProxy( const org::xtreemfs::interfaces::FileCredentials& file_credentials, uint64_t object_number );
        YIELD::auto_Object<OSDProxy> getTCPOSDProxy( const std::string& osd_uuid );        
        void pingOSD( YIELD::auto_Object<OSDProxy> udp_osd_proxy );

        // org::xtreemfs::interfaces::OSDInterface
        void handlereadRequest( readRequest& req );
        void handletruncateRequest( truncateRequest& req );
        void handleunlinkRequest( unlinkRequest& req );
        void handlewriteRequest( writeRequest& req );
      };
    };
  };
};

#endif
