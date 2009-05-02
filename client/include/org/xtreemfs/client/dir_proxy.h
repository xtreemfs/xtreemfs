// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_DIR_PROXY_H
#define ORG_XTREEMFS_CLIENT_DIR_PROXY_H

#include "org/xtreemfs/client/proxy_exception_event.h"
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
        static YIELD::auto_Object<DIRProxy> create( YIELD::StageGroup& stage_group, const YIELD::SocketAddress& peer_sockaddr, YIELD::auto_Object<YIELD::SSLContext> ssl_context = NULL, YIELD::auto_Object<YIELD::Log> log = NULL )
        {
          YIELD::auto_Object<DIRProxy> proxy = new DIRProxy( peer_sockaddr, ssl_context, log );
          stage_group.createStage( proxy, YIELD::auto_Object<YIELD::FDAndInternalEventQueue>( new YIELD::FDAndInternalEventQueue ), log );
          return proxy;
        }       

        YIELD::URI getURIFromUUID( const std::string& uuid );
        YIELD::URI getVolumeURIFromVolumeName( const std::string& volume_name );

        // YIELD::EventHandler
        const char* getEventHandlerName() const { return "DIRProxy"; }

      private:
        DIRProxy( const YIELD::SocketAddress& peer_sockaddr, YIELD::auto_Object<YIELD::SSLContext> ssl_context, YIELD::auto_Object<YIELD::Log> log );
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

        private:
          uint32_t ttl_s;
          double creation_epoch_time_s;
        };

        std::map<std::string, CachedAddressMappingURI*> uuid_to_uri_cache;
        YIELD::Mutex uuid_to_uri_cache_lock;


        // YIELD::Proxy
        YIELD::ONCRPCRequest* createONCRPCRequest( YIELD::auto_Object<YIELD::Object> out_body );
      };
    };
  };
};

#endif
