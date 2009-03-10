#ifndef ORG_XTREEMFS_CLIENT_OSD_PROXY_H
#define ORG_XTREEMFS_CLIENT_OSD_PROXY_H

#include "org/xtreemfs/client/proxy.h"
#define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_PARENT_CLASS xtreemfs::client::Proxy
#include "org/xtreemfs/interfaces/osd_interface.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class OSDProxy : public org::xtreemfs::interfaces::OSDInterface
      {
      public:
        OSDProxy( const YIELD::URI& uri, uint8_t reconnect_tries_max = 3 )
        {
          Proxy::init( uri, reconnect_tries_max );
          org::xtreemfs::interfaces::OSDInterface::registerSerializableFactories( serializable_factories );
        }

      private:
        ORG_XTREEMFS_INTERFACES_OSDINTERFACE_DUMMY_DEFINITIONS;
      };
    };
  };
};

#endif
