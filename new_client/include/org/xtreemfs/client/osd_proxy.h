#ifndef ORG_XTREEMFS_CLIENT_OSD_PROXY_H
#define ORG_XTREEMFS_CLIENT_OSD_PROXY_H

#include "org/xtreemfs/client/proxy.h"
#define ORG_XTREEMFS_INTERFACES_OSDINTERFACE_INTERFACE_PARENT_CLASS org::xtreemfs::client::Proxy
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
        OSDProxy( const YIELD::URI& uri, uint8_t reconnect_tries_max = PROXY_DEFAULT_RECONNECT_TRIES_MAX, uint32_t flags = PROXY_DEFAULT_FLAGS );

        // EventHandler
        virtual void handleEvent( YIELD::Event& ev ) { Proxy::handleEvent( ev ); }

      private:
        ORG_XTREEMFS_INTERFACES_OSDINTERFACE_DUMMY_DEFINITIONS;
      };
    };
  };
};

#endif
