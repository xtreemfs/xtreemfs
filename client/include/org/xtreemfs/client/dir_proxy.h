// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_DIR_PROXY_H
#define ORG_XTREEMFS_CLIENT_DIR_PROXY_H

#include "org/xtreemfs/client/proxy_exception_response.h"
#include "org/xtreemfs/interfaces/dir_interface.h"

#include <map>
#include <string>


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class PolicyContainer;


      class DIRProxy : public YIELD::ONCRPCClient
      {
      public:
        static YIELD::auto_Object<DIRProxy> create( YIELD::auto_Object<YIELD::StageGroup> stage_group, const YIELD::SocketAddress& peer_sockaddr, YIELD::auto_Object<YIELD::SocketFactory> socket_factory = NULL, YIELD::auto_Object<YIELD::Log> log = NULL )
        {
          YIELD::auto_Object<DIRProxy> proxy = new DIRProxy( peer_sockaddr, socket_factory, log );
          stage_group->createStage( proxy, YIELD::auto_Object<YIELD::FDAndInternalEventQueue>( new YIELD::FDAndInternalEventQueue ), log );
          return proxy;
        }       

        YIELD::auto_Object<YIELD::URI> getURIFromUUID( const std::string& uuid );
        YIELD::auto_Object<YIELD::URI> getVolumeURIFromVolumeName( const std::string& volume_name );

        // YIELD::EventHandler
        const char* getEventHandlerName() const { return "DIRProxy"; }

      private:
        DIRProxy( const YIELD::SocketAddress& peer_sockaddr, YIELD::auto_Object<YIELD::SocketFactory> socket_factory, YIELD::auto_Object<YIELD::Log> log );
        ~DIRProxy();


        org::xtreemfs::interfaces::DIRInterface dir_interface;
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


        // YIELD::Client
        YIELD::auto_Object<YIELD::Request> createProtocolRequest( YIELD::auto_Object<> body );
      };
    };
  };
};

#endif
