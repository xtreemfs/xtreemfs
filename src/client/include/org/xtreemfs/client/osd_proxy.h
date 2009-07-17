// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ORG_XTREEMFS_CLIENT_OSD_PROXY_H_
#define _ORG_XTREEMFS_CLIENT_OSD_PROXY_H_

#include "org/xtreemfs/client/proxy.h"
#include "org/xtreemfs/client/osd_proxy_request.h"
#include "org/xtreemfs/client/osd_proxy_response.h"

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
      public:
        const static uint64_t PING_INTERVAL_DEFAULT = 0; // No pings


        static YIELD::auto_Object<OSDProxy> create( const YIELD::URI& absolute_uri,
                                                    YIELD::auto_Object<YIELD::StageGroup> stage_group,
                                                    const std::string& uuid,
                                                    uint32_t flags = 0,
                                                    YIELD::auto_Log log = NULL,
                                                    const YIELD::Time& operation_timeout = YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::OPERATION_TIMEOUT_DEFAULT,
                                                    const YIELD::Time& ping_interval = PING_INTERVAL_DEFAULT,
                                                    YIELD::auto_SSLContext ssl_context = NULL );

        const YIELD::Time& get_ping_interval() const { return ping_interval; }
        const YIELD::Time& get_rtt() const { return rtt; }
        const std::string& get_uuid() const { return uuid; }
        const org::xtreemfs::interfaces::VivaldiCoordinates& get_vivaldi_coordinates() const { return vivaldi_coordinates; }
        void set_ping_interval( const YIELD::Time& ping_interval ) { this->ping_interval = ping_interval; }
        void set_rtt( const YIELD::Time& rtt ) { this->rtt = rtt; }
        void set_vivaldi_coordinates( const org::xtreemfs::interfaces::VivaldiCoordinates& vivaldi_coordinates ) { this->vivaldi_coordinates = vivaldi_coordinates; }
        
        // YIELD::Object
        OSDProxy& incRef() { return YIELD::Object::incRef( *this ); }

        // YIELD::EventTarget
        void send( YIELD::Event& ev ) 
        { 
          // Bypass Proxy so no credentials are attached; the credentials for OSD operations are in FileCredentials passed explicitly to the operation
          YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::send( ev ); 
        }

      private:
        friend class YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>;

        OSDProxy( const YIELD::URI& absolute_uri, uint32_t flags, YIELD::auto_Log log, const YIELD::Time& operation_timeout, YIELD::auto_SocketAddress peer_sockaddr, YIELD::auto_SSLContext ssl_context )
          : Proxy<OSDProxy, org::xtreemfs::interfaces::OSDInterface>( absolute_uri, flags, log, operation_timeout, peer_sockaddr, ssl_context )
        { }

        ~OSDProxy() { }
        
        YIELD::Time ping_interval, rtt;
        std::string uuid;
        org::xtreemfs::interfaces::VivaldiCoordinates vivaldi_coordinates;
      };


      bool operator>( const org::xtreemfs::interfaces::OSDWriteResponse& left, const org::xtreemfs::interfaces::OSDWriteResponse& right );
    };
  };
};

#endif
