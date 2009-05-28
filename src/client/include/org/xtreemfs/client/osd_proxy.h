// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ORG_XTREEMFS_CLIENT_OSD_PROXY_H_
#define _ORG_XTREEMFS_CLIENT_OSD_PROXY_H_

#include "org/xtreemfs/client/proxy.h"

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "org/xtreemfs/interfaces/osd_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class OSDProxy : public Proxy<OSDProxy, org::xtreemfs::interfaces::OSDInterface>
      {
      private:
        friend class YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>;

        OSDProxy( YIELD::auto_Object<YIELD::FDAndInternalEventQueue> fd_event_queue, YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::Socket> _socket );
        ~OSDProxy() { }

        // YIELD::ONCRPCClient
        YIELD::auto_Object<YIELD::ONCRPCRequest> createProtocolRequest( YIELD::auto_Object<YIELD::Request> body );
      };
    };
  };
};

#endif
