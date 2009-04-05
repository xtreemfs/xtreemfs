// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_OSD_PROXY_FACTORY_H
#define ORG_XTREEMFS_CLIENT_OSD_PROXY_FACTORY_H

#include "org/xtreemfs/client/osd_proxy.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class DIRProxy;
      class VersionedURI;


      class OSDProxyFactory
      {
      public:
        OSDProxyFactory( DIRProxy& dir_proxy, YIELD::StageGroup& osd_proxy_stage_group, uint8_t osd_proxy_reconnect_tries_max = Proxy::PROXY_DEFAULT_RECONNECT_TRIES_MAX, uint32_t osd_proxy_flags = Proxy::PROXY_DEFAULT_FLAGS );
        virtual ~OSDProxyFactory();

        OSDProxy& createOSDProxy( const std::string& uuid );

      private:
        DIRProxy& dir_proxy;
        YIELD::StageGroup& osd_proxy_stage_group;
        uint8_t osd_proxy_reconnect_tries_max;
        uint32_t osd_proxy_flags;

        YIELD::STLHashMap<OSDProxy*> osd_proxy_cache;
        YIELD::Mutex osd_proxy_cache_lock;

        OSDProxy& createOSDProxy( const YIELD::URI& uri );
      };
    };
  };
};

#endif
