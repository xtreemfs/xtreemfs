// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ORG_XTREEMFS_CLIENT_MRC_PROXY_H_
#define _ORG_XTREEMFS_CLIENT_MRC_PROXY_H_

#include "org/xtreemfs/client/path.h"
#include "org/xtreemfs/client/proxy.h"

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
      class MRCProxy : public Proxy<MRCProxy, org::xtreemfs::interfaces::MRCInterface>
      {
      public:
        // org::xtreemfs::interfaces::MRCInterface
        void chown( const Path& path, int uid, int gid );
        void getattr( const Path& path, org::xtreemfs::interfaces::Stat& stbuf );
        void readdir( const Path& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries );

      private:
        friend class YIELD::ONCRPCClient<org::xtreemfs::interfaces::MRCInterface>;

        MRCProxy( YIELD::auto_Object<YIELD::FDAndInternalEventQueue> fd_event_queue, YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::Socket> _socket );
        ~MRCProxy() { }
      };
    };
  };
};

#endif
