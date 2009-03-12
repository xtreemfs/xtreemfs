#include "org/xtreemfs/client/mrc_proxy.h"
using namespace org::xtreemfs::client;


MRCProxy::MRCProxy( const YIELD::URI& uri, uint8_t reconnect_tries_max )
{
  Proxy::init( uri, reconnect_tries_max );
  if ( this->uri->getPort() == 0 )
  {
    if ( strcmp( this->uri->getScheme(), org::xtreemfs::interfaces::ONCRPC_SCHEME ) == 0 )
      this->uri->setPort( org::xtreemfs::interfaces::MRCInterface::DEFAULT_ONCRPC_PORT );
    else if ( strcmp( this->uri->getScheme(), org::xtreemfs::interfaces::ONCRPCS_SCHEME ) == 0 )
      this->uri->setPort( org::xtreemfs::interfaces::MRCInterface::DEFAULT_ONCRPCS_PORT );
    else
      YIELD::DebugBreak();
  }
  org::xtreemfs::interfaces::MRCInterface::registerSerializableFactories( serializable_factories ); 
}
