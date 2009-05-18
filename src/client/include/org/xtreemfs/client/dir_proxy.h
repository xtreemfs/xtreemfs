// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_DIR_PROXY_H
#define ORG_XTREEMFS_CLIENT_DIR_PROXY_H

#include "org/xtreemfs/client/proxy_exception_response.h"

#include <map>
#include <string>


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
      class DIRInterface;
    };


    namespace client
    {
      class PolicyContainer;


      class DIRProxy : public YIELD::ONCRPCClient
      {
      public:
        template <class StageGroupType>
        static YIELD::auto_Object<DIRProxy> create( YIELD::auto_Object<StageGroupType> stage_group, const YIELD::URI& uri, YIELD::auto_Object<YIELD::Log> log = NULL, const YIELD::Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, uint8_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT, YIELD::auto_Object<YIELD::SocketFactory> socket_factory = NULL )
        {
          return YIELD::Client::create<DIRProxy, StageGroupType>( stage_group, log, operation_timeout, uri, reconnect_tries_max, socket_factory );
        }

        YIELD::auto_Object<YIELD::URI> getURIFromUUID( const std::string& uuid );
        YIELD::auto_Object<YIELD::URI> getVolumeURIFromVolumeName( const std::string& volume_name );

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( org::xtreemfs::client::DIRProxy, 547158291UL );

      private:
        friend class YIELD::Client;

        DIRProxy( YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::SocketFactory> socket_factory );
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


        // YIELD::Client
        YIELD::auto_Object<YIELD::Request> createProtocolRequest( YIELD::auto_Object<YIELD::Request> body );
      };
    };
  };
};

#endif
