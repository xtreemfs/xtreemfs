// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_OSD_PROXY_H
#define ORG_XTREEMFS_CLIENT_OSD_PROXY_H

#include "org/xtreemfs/client/proxy_exception_response.h"
#include "org/xtreemfs/interfaces/osd_interface.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class OSDProxy : public YIELD::ONCRPCClient
      {
      public:
        template <class StageGroupType>
        static YIELD::auto_Object<OSDProxy> create( YIELD::auto_Object<StageGroupType> stage_group, const YIELD::SocketAddress& peer_sockaddr, YIELD::auto_Object<YIELD::Log> log = NULL, const YIELD::Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, uint8_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT, YIELD::auto_Object<YIELD::SocketFactory> socket_factory = NULL )
        {
          return YIELD::Client::create<OSDProxy, StageGroupType>( stage_group, log, operation_timeout, peer_sockaddr, reconnect_tries_max, socket_factory );
        }

        void read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data );
        void truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );
        void unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id );
        void write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );

        // YIELD::Object
        OSDProxy& incRef() { return YIELD::Object::incRef( *this ); }

        // YIELD::EventHandler
        const char* getEventHandlerName() const { return "OSDProxy"; }

      private:
        friend class YIELD::Client;

        OSDProxy( YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, const YIELD::SocketAddress& peer_sockaddr, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::SocketFactory> socket_factory );
        ~OSDProxy() { }

        org::xtreemfs::interfaces::OSDInterface osd_interface;
      };
    };
  };
};

#endif
