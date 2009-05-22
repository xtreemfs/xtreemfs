// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_DIR_PROXY_H
#define ORG_XTREEMFS_CLIENT_DIR_PROXY_H

#include "org/xtreemfs/client/proxy_exception_response.h"

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "org/xtreemfs/interfaces/dir_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif

#include <map>
#include <string>


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class PolicyContainer;


      class DIRProxy : public YIELD::ONCRPCClient<org::xtreemfs::interfaces::DIRInterface>
      {
      public:
        template <class StageGroupType>
        static YIELD::auto_Object<DIRProxy> create( const YIELD::URI& absolute_uri, 
                                                    YIELD::auto_Object<YIELD::SocketFactory> socket_factory,
                                                    YIELD::auto_Object<StageGroupType> stage_group,                                                     
                                                    YIELD::auto_Object<YIELD::Log> log = NULL, 
                                                    const YIELD::Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, 
                                                    uint8_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT )
        {
          YIELD::auto_Object<YIELD::FDAndInternalEventQueue> fd_event_queue = new YIELD::FDAndInternalEventQueue;
          YIELD::auto_Object<DIRProxy> dir_proxy = new DIRProxy( fd_event_queue, log, operation_timeout, new YIELD::SocketAddress( absolute_uri ), reconnect_tries_max, socket_factory );
          stage_group->createStage( dir_proxy->incRef(), 1, fd_event_queue->incRef() );
          return dir_proxy;
        }

        YIELD::auto_Object<YIELD::URI> getURIFromUUID( const std::string& uuid );
        YIELD::auto_Object<YIELD::URI> getVolumeURIFromVolumeName( const std::string& volume_name );

      private:
        DIRProxy( YIELD::auto_Object<YIELD::FDAndInternalEventQueue> fd_event_queue, YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::SocketFactory> socket_factory );
        ~DIRProxy();


        PolicyContainer* policies;


        class CachedAddressMappingURI : public YIELD::URI
        {
        public:
          CachedAddressMappingURI( const std::string& uri, uint32_t ttl_s )
            : YIELD::URI( uri ), ttl_s( ttl_s )
          {
            creation_epoch_time_s = YIELD::Time::getCurrentUnixTimeS();
          }

          uint32_t get_ttl_s() const { return ttl_s; }
          double get_creation_epoch_time_s() const { return creation_epoch_time_s; }

          // Object
          CachedAddressMappingURI& incRef() { return YIELD::Object::incRef( *this ); }

        private:
          ~CachedAddressMappingURI() { }

          uint32_t ttl_s;
          double creation_epoch_time_s;
        };

        std::map<std::string, CachedAddressMappingURI*> uuid_to_uri_cache;
        YIELD::Mutex uuid_to_uri_cache_lock;


        // YIELD::ONCRPCClient
        YIELD::auto_Object<YIELD::ONCRPCRequest> createProtocolRequest( YIELD::auto_Object<YIELD::Request> body );
      };
    };
  };
};

#endif
