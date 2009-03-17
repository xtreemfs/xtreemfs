#include "org/xtreemfs/client/osd_proxy_factory.h"
#include "org/xtreemfs/client/dir_proxy.h"
using namespace org::xtreemfs::client;



OSDProxyFactory::OSDProxyFactory( DIRProxy& dir_proxy, YIELD::StageGroup& osd_proxy_stage_group, uint8_t osd_proxy_reconnect_tries_max, uint32_t osd_proxy_flags )
  : dir_proxy( dir_proxy ), osd_proxy_stage_group( osd_proxy_stage_group ), osd_proxy_reconnect_tries_max( osd_proxy_reconnect_tries_max ), osd_proxy_flags( osd_proxy_flags )
{ }

OSDProxy& OSDProxyFactory::createOSDProxy( const std::string& uuid, uint64_t timeout_ms )
{
  return createOSDProxy( dir_proxy.get_uri_from_uuid( uuid, timeout_ms ) );
}

OSDProxy& OSDProxyFactory::createOSDProxy( const YIELD::URI& uri )
{
  OSDProxy* osd_proxy = new OSDProxy( uri, osd_proxy_reconnect_tries_max, osd_proxy_flags );
  osd_proxy_stage_group.createStage( *osd_proxy );
  return *osd_proxy;
}
