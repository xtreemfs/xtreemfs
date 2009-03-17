#ifndef ORG_XTREEMFS_CLIENT_OSD_PROXY_FACTORY_IMPL
#define ORG_XTREEMFS_CLIENT_OSD_PROXY_FACTORY_IMPL

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
        OSDProxyFactory( DIRProxy& dir_proxy, YIELD::StageGroup& osd_proxy_stage_group, uint8_t osd_proxy_reconnect_tries_max = 3, uint32_t osd_proxy_flags = 0 );
        virtual ~OSDProxyFactory() { }

        OSDProxy& createOSDProxy( const std::string& uuid, uint64_t timeout_ms = static_cast<uint64_t>( -1 ) );

      private:
        DIRProxy& dir_proxy;
        YIELD::StageGroup& osd_proxy_stage_group;
        uint8_t osd_proxy_reconnect_tries_max;
        uint32_t osd_proxy_flags;

        OSDProxy& createOSDProxy( const YIELD::URI& uri );
      };
    };
  };
};

#endif
