// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ORG_XTREEMFS_CLIENT_DIR_PROXY_H_
#define _ORG_XTREEMFS_CLIENT_DIR_PROXY_H_

#include "org/xtreemfs/client/proxy.h"

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
      class DIRProxy : public Proxy<DIRProxy, org::xtreemfs::interfaces::DIRInterface>
      {
      public:
        static YIELD::auto_Object<DIRProxy> create( const YIELD::URI& absolute_uri,
                                                    YIELD::auto_Object<YIELD::StageGroup> stage_group,
                                                    uint32_t flags = 0,
                                                    YIELD::auto_Log log = NULL,
                                                    const YIELD::Time& operation_timeout = YIELD::ONCRPCClient<org::xtreemfs::interfaces::DIRInterface>::OPERATION_TIMEOUT_DEFAULT,
                                                    YIELD::auto_SSLContext ssl_context = NULL )
        {
          return YIELD::ONCRPCClient<org::xtreemfs::interfaces::DIRInterface>::create<DIRProxy>( absolute_uri, stage_group, flags, log, operation_timeout, ssl_context );
        }

        YIELD::auto_Object<org::xtreemfs::interfaces::AddressMappingSet> getAddressMappingsFromUUID( const std::string& uuid );
        YIELD::auto_Object<YIELD::URI> getVolumeURIFromVolumeName( const std::string& volume_name );

      private:
        friend class YIELD::ONCRPCClient<org::xtreemfs::interfaces::DIRInterface>;

        DIRProxy( const YIELD::URI& absolute_uri, uint32_t flags, YIELD::auto_Log log, const YIELD::Time& operation_timeout, YIELD::auto_SocketAddress peer_sockaddr, YIELD::auto_SSLContext ssl_context )
            : Proxy<DIRProxy, org::xtreemfs::interfaces::DIRInterface>( absolute_uri, flags, log, operation_timeout, peer_sockaddr, ssl_context )
        { }

        ~DIRProxy();


        class CachedAddressMappings : public org::xtreemfs::interfaces::AddressMappingSet
        {
        public:
          CachedAddressMappings( const org::xtreemfs::interfaces::AddressMappingSet& address_mappings, uint32_t ttl_s )
            : org::xtreemfs::interfaces::AddressMappingSet( address_mappings ), ttl_s( ttl_s )
          { }

          const YIELD::Time& get_creation_time() const { return creation_time; }
          uint32_t get_ttl_s() const { return ttl_s; }         
          
          // YIELD::Object
          YIELD_OBJECT_PROTOTYPES( CachedAddressMappings, 0 );

        private:
          ~CachedAddressMappings() { }

          uint32_t ttl_s;

          YIELD::Time creation_time;
        };

        std::map<std::string, CachedAddressMappings*> uuid_to_address_mappings_cache;
        YIELD::Mutex uuid_to_address_mappings_cache_lock;
      };      
    };
  };
};

#endif
