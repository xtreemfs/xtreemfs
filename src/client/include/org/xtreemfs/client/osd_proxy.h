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
      public:
        const static uint64_t PING_INTERVAL_DEFAULT = 0; // No pings


        static YIELD::auto_Object<OSDProxy> create( const YIELD::URI& absolute_uri,
                                                    YIELD::auto_Object<YIELD::StageGroup> stage_group,
                                                    const std::string& uuid,
                                                    YIELD::auto_Object<YIELD::Log> log = NULL,
                                                    uint8_t operation_retries_max = YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::OPERATION_RETRIES_MAX_DEFAULT,
                                                    const YIELD::Time& operation_timeout = YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::OPERATION_TIMEOUT_DEFAULT,
                                                    const YIELD::Time& ping_interval = PING_INTERVAL_DEFAULT,
                                                    YIELD::auto_Object<YIELD::SSLContext> ssl_context = NULL )
        {
          YIELD::auto_Object<OSDProxy> osd_proxy = YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::create<OSDProxy>( absolute_uri, stage_group, log, operation_timeout, operation_retries_max, ssl_context );
          osd_proxy->set_ping_interval( ping_interval );
          osd_proxy->set_uuid( uuid );
          return osd_proxy;
        }

        const YIELD::Time& get_ping_interval() const { return ping_interval; }
        const YIELD::Time& get_rtt() const { return rtt; }
        const std::string& get_uuid() const { return uuid; }
        const org::xtreemfs::interfaces::VivaldiCoordinates& get_vivaldi_coordinates() const { return vivaldi_coordinates; }
        void set_ping_interval( const YIELD::Time& ping_interval ) { this->ping_interval = ping_interval; }
        void set_rtt( const YIELD::Time& rtt ) { this->rtt = rtt; }
        void set_vivaldi_coordinates( const org::xtreemfs::interfaces::VivaldiCoordinates& vivaldi_coordinates ) { this->vivaldi_coordinates = vivaldi_coordinates; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( OSDProxy, 0 );

        // YIELD::EventHandler
        void handleEvent( YIELD::Event& ev ) { YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::handleEvent( ev ); }

      private:
        friend class YIELD::ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>;

        OSDProxy( const YIELD::URI& absolute_uri, YIELD::auto_Object<YIELD::Log> log, uint8_t operation_retries_max, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, YIELD::auto_Object<YIELD::SSLContext> ssl_context )
          : Proxy<OSDProxy, org::xtreemfs::interfaces::OSDInterface>( absolute_uri, log, operation_retries_max, operation_timeout, peer_sockaddr, ssl_context )
        { }

        ~OSDProxy() { }

        
        YIELD::Time ping_interval, rtt;
        std::string uuid;
        org::xtreemfs::interfaces::VivaldiCoordinates vivaldi_coordinates;


        void set_uuid( const std::string& uuid ) { this->uuid = uuid; }

        // YIELD::ONCRPCClient
        YIELD::auto_Object<YIELD::ONCRPCRequest> createProtocolRequest( YIELD::auto_Object<YIELD::Request> body );
      };


      static inline bool operator>( const org::xtreemfs::interfaces::OSDWriteResponse& left, const org::xtreemfs::interfaces::OSDWriteResponse& right )
      {
        if ( left.get_new_file_size().empty() )
          return false;
        else if ( right.get_new_file_size().empty() )
          return true;
        else if ( left.get_new_file_size()[0].get_truncate_epoch() > right.get_new_file_size()[0].get_truncate_epoch() )
          return true;
        else if ( left.get_new_file_size()[0].get_truncate_epoch() == right.get_new_file_size()[0].get_truncate_epoch() &&
                  left.get_new_file_size()[0].get_size_in_bytes() > right.get_new_file_size()[0].get_size_in_bytes() )
          return true;
        else
          return false;
      }
    };
  };
};

#endif
