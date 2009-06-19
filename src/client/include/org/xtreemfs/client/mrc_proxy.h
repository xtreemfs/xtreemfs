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
        static YIELD::auto_Object<MRCProxy> create( const YIELD::URI& absolute_uri,
                                                    YIELD::auto_Object<YIELD::StageGroup> stage_group,
                                                    YIELD::auto_Log log = NULL,
                                                    uint8_t operation_retries_max = YIELD::ONCRPCClient<org::xtreemfs::interfaces::MRCInterface>::OPERATION_RETRIES_MAX_DEFAULT,
                                                    const YIELD::Time& operation_timeout = YIELD::ONCRPCClient<org::xtreemfs::interfaces::MRCInterface>::OPERATION_TIMEOUT_DEFAULT,
                                                    YIELD::auto_Object<YIELD::SSLContext> ssl_context = NULL )
        {
          return YIELD::ONCRPCClient<org::xtreemfs::interfaces::MRCInterface>::create<MRCProxy>( absolute_uri, stage_group, log, operation_timeout, operation_retries_max, ssl_context );
        }

        // org::xtreemfs::interfaces::MRCInterface
        void chown( const Path& path, int uid, int gid );
        void getattr( const Path& path, org::xtreemfs::interfaces::Stat& stbuf );
        void readdir( const Path& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries );

      private:
        friend class YIELD::ONCRPCClient<org::xtreemfs::interfaces::MRCInterface>;

        MRCProxy( const YIELD::URI& absolute_uri, YIELD::auto_Log log, uint8_t operation_retries_max, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, YIELD::auto_Object<YIELD::SSLContext> ssl_context )
          : Proxy<MRCProxy, org::xtreemfs::interfaces::MRCInterface>( absolute_uri, log, operation_retries_max, operation_timeout, peer_sockaddr, ssl_context )
        { }

        ~MRCProxy() { }
      };
    };
  };
};

#endif
