#ifndef ORG_XTREEMFS_CLIENT_OSD_PROXY_FACTORY_IMPL
#define ORG_XTREEMFS_CLIENT_OSD_PROXY_FACTORY_IMPL

#include "org/xtreemfs/client/osd_proxy.h"

#include <map>
#include <string>


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
        OSDProxyFactory( DIRProxy& dir_proxy, YIELD::StageGroup& osd_proxy_stage_group )
          : dir_proxy( dir_proxy ), osd_proxy_stage_group( osd_proxy_stage_group )
        { }
        virtual ~OSDProxyFactory();

        OSDProxy& createOSDProxy( const std::string& uuid, uint64_t version );

      private:
        DIRProxy& dir_proxy;
        YIELD::StageGroup& osd_proxy_stage_group;

        std::map<std::string, VersionedURI*> uuid_to_uri_map; YIELD::Mutex uuid_to_uri_map_lock;

        OSDProxy& createOSDProxy( const YIELD::URI& uri );
      };
    };
  };
};

#endif
