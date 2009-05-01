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


      class OSDProxyFactory : public YIELD::Object
      {
      public:
        OSDProxyFactory( YIELD::auto_Object<DIRProxy> dir_proxy, YIELD::StageGroup& osd_proxy_stage_group );

        YIELD::auto_Object<OSDProxy> createOSDProxy( const std::string& uuid );

      private:
        ~OSDProxyFactory();

        YIELD::auto_Object<DIRProxy> dir_proxy;
        YIELD::StageGroup& osd_proxy_stage_group;

        YIELD::STLHashMap<OSDProxy*> osd_proxy_cache;
        YIELD::Mutex osd_proxy_cache_lock;

        YIELD::auto_Object<OSDProxy> createOSDProxy( const YIELD::URI& uri );
      };
    };
  };
};

#endif
