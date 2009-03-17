#ifndef ORG_XTREEMFS_CLIENT_DIR_PROXY_H
#define ORG_XTREEMFS_CLIENT_DIR_PROXY_H

#include "org/xtreemfs/client/proxy.h"
#define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS org::xtreemfs::client::Proxy
#include "org/xtreemfs/interfaces/dir_interface.h"
#include "org/xtreemfs/client/proxy.h"

#include <map>
#include <string>


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class DIRProxy : public org::xtreemfs::interfaces::DIRInterface
      {
      public:
        DIRProxy( const YIELD::URI& uri, uint8_t reconnect_tries_max = PROXY_DEFAULT_RECONNECT_TRIES_MAX, uint32_t flags = PROXY_DEFAULT_FLAGS );
        virtual ~DIRProxy();

        YIELD::URI get_uri_from_uuid( const std::string& uuid, uint64_t timeout_ms = static_cast<uint64_t>( -1 ) );

        // EventHandler
        virtual void handleEvent( YIELD::Event& ev ) { Proxy::handleEvent( ev ); }

      private:
        ORG_XTREEMFS_INTERFACES_DIRINTERFACE_DUMMY_DEFINITIONS;

        class CachedAddressMappingURI : public YIELD::URI
        {
        public:
          CachedAddressMappingURI( const std::string& uri, uint32_t ttl_s )
            : YIELD::URI( uri ), ttl_s( ttl_s )
          {
            creation_epoch_time_s = YIELD::Time::getCurrentEpochTimeS();
          }

          uint32_t get_ttl_s() const { return ttl_s; }
          double get_creation_epoch_time_s() const { return creation_epoch_time_s; }

        private:
          uint32_t ttl_s;
          double creation_epoch_time_s;
        };

        std::map<std::string, CachedAddressMappingURI*> uuid_to_uri_cache; 
        YIELD::Mutex uuid_to_uri_cache_lock;
      };
    };
  };
};

#endif
