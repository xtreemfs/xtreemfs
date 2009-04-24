// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/osd_proxy_factory.h"
#include "org/xtreemfs/client/dir_proxy.h"
using namespace org::xtreemfs::client;


OSDProxyFactory::OSDProxyFactory( DIRProxy& dir_proxy, YIELD::StageGroup& osd_proxy_stage_group, uint8_t osd_proxy_reconnect_tries_max )
  : dir_proxy( dir_proxy ), osd_proxy_stage_group( osd_proxy_stage_group ), osd_proxy_reconnect_tries_max( osd_proxy_reconnect_tries_max )
{ }

OSDProxyFactory::~OSDProxyFactory()
{
  osd_proxy_cache_lock.acquire();
  for ( YIELD::STLHashMap<OSDProxy*>::iterator osd_proxy_i = osd_proxy_cache.begin(); osd_proxy_i != osd_proxy_cache.end(); osd_proxy_i++ )
    YIELD::Object::decRef( *osd_proxy_i->second );
  osd_proxy_cache.clear();
  osd_proxy_cache_lock.release();
}

OSDProxy& OSDProxyFactory::createOSDProxy( const std::string& uuid )
{
  return createOSDProxy( dir_proxy.getURIFromUUID( uuid ) );
}

OSDProxy& OSDProxyFactory::createOSDProxy( const YIELD::URI& uri )
{
  uint32_t uri_hash = YIELD::string_hash( uri.get_decoded_uri() );
  osd_proxy_cache_lock.acquire();
  OSDProxy* osd_proxy = osd_proxy_cache.find( uri_hash );
  osd_proxy_cache_lock.release();
  if ( osd_proxy != NULL )
    return YIELD::Object::incRef( *osd_proxy );
  else
  {
    osd_proxy = new OSDProxy( uri, YIELD::Object::incRef( dir_proxy.get_ssl_context() ), YIELD::Object::incRef( dir_proxy.get_log() ) );
    osd_proxy_stage_group.createStage( *osd_proxy, 1, new YIELD::FDAndInternalEventQueue );
    YIELD::Object::incRef( *osd_proxy ); // For the cache
    osd_proxy_cache_lock.acquire();
    osd_proxy_cache.insert( uri_hash, osd_proxy );
    osd_proxy_cache_lock.release();
    return *osd_proxy;
  }
}
