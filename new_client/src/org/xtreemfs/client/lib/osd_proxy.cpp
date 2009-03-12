#include "org/xtreemfs/client/osd_proxy.h"
using namespace org::xtreemfs::client;


OSDProxy::OSDProxy( const YIELD::URI& uri, uint8_t reconnect_tries_max )
{
  Proxy::init( uri, reconnect_tries_max );
  if ( this->uri->getPort() == 0 )
  {
    if ( strcmp( this->uri->getScheme(), org::xtreemfs::interfaces::ONCRPC_SCHEME ) == 0 )
      this->uri->setPort( org::xtreemfs::interfaces::OSDInterface::DEFAULT_ONCRPC_PORT );
    else if ( strcmp( this->uri->getScheme(), org::xtreemfs::interfaces::ONCRPCS_SCHEME ) == 0 )
      this->uri->setPort( org::xtreemfs::interfaces::OSDInterface::DEFAULT_ONCRPCS_PORT );
    else
      YIELD::DebugBreak();
  }
  org::xtreemfs::interfaces::OSDInterface::registerSerializableFactories( serializable_factories );
}
