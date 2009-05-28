// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ORG_XTREEMFS_CLIENT_OSD_PROXY_H_
#define _ORG_XTREEMFS_CLIENT_OSD_PROXY_H_

#include "org/xtreemfs/client/proxy_exception_response.h"

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
      class OSDProxy : public YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>
      {
      public:
        template <class StageGroupType>
        static YIELD::auto_Object<OSDProxy> create( const YIELD::URI& absolute_uri,
                                                    YIELD::auto_Object<StageGroupType> stage_group,
                                                    YIELD::auto_Object<YIELD::Log> log = NULL,
                                                    const YIELD::Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT,
                                                    uint8_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT
#ifdef YIELD_HAVE_OPENSSL
                                                    , YIELD::auto_Object<YIELD::SSLContext> ssl_context = NULL
#endif
                                                  )
        {
          return YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::create<OSDProxy>( absolute_uri, stage_group, log, operation_timeout, reconnect_tries_max
#ifdef YIELD_HAVE_OPENSSL
                                                                                                 , ssl_context
#endif
                                                                                               );
        }

      private:
        friend class YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>;

        OSDProxy( YIELD::auto_Object<YIELD::FDAndInternalEventQueue> fd_event_queue, YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::Socket> _socket )
          : YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>( fd_event_queue, log, operation_timeout, peer_sockaddr, reconnect_tries_max, _socket )
        { }

        ~OSDProxy() { }

        org::xtreemfs::interfaces::OSDInterface* osd_interface;
      };
    };
  };
};

#endif
