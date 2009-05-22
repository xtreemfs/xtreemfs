// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_MRC_PROXY_H
#define ORG_XTREEMFS_CLIENT_MRC_PROXY_H

#include "org/xtreemfs/client/path.h"
#include "org/xtreemfs/client/proxy_exception_response.h"

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "org/xtreemfs/interfaces/mrc_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {    
      class PolicyContainer;


      class MRCProxy : public YIELD::ONCRPCClient<org::xtreemfs::interfaces::MRCInterface>
      {
      public:       
        template <class StageGroupType>
        static YIELD::auto_Object<MRCProxy> create( const YIELD::URI& absolute_uri,
                                                    YIELD::auto_Object<YIELD::SocketFactory> socket_factory,
                                                    YIELD::auto_Object<StageGroupType> stage_group,                                                     
                                                    YIELD::auto_Object<YIELD::Log> log = NULL, 
                                                    const YIELD::Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, 
                                                    uint8_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT )
        {
          YIELD::auto_Object<YIELD::FDAndInternalEventQueue> fd_event_queue = new YIELD::FDAndInternalEventQueue;
          YIELD::auto_Object<MRCProxy> mrc_proxy = new MRCProxy( fd_event_queue, log, operation_timeout, new YIELD::SocketAddress( absolute_uri ), reconnect_tries_max, socket_factory );
          stage_group->createStage( mrc_proxy->incRef(), 1, fd_event_queue->incRef() );
          return mrc_proxy;
        }

        // org::xtreemfs::interfaces::MRCInterface
        void chown( const Path& path, int uid, int gid );
        void getattr( const Path& path, org::xtreemfs::interfaces::Stat& stbuf );
        void readdir( const Path& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries );

      private:
        MRCProxy( YIELD::auto_Object<YIELD::FDAndInternalEventQueue> fd_event_queue, YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::SocketFactory> socket_factory );
        ~MRCProxy();


        PolicyContainer* policies;


        // YIELD::ONCRPCClient
        YIELD::auto_Object<YIELD::ONCRPCRequest> createProtocolRequest( YIELD::auto_Object<YIELD::Request> body );
      };
    };
  };
};

#endif
