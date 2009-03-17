#ifndef ORG_XTREEMFS_CLIENT_MRC_PROXY_H
#define ORG_XTREEMFS_CLIENT_MRC_PROXY_H

#include "org/xtreemfs/client/proxy.h"
#define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_INTERFACE_PARENT_CLASS org::xtreemfs::client::Proxy
#include "org/xtreemfs/interfaces/mrc_interface.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class MRCProxy : public org::xtreemfs::interfaces::MRCInterface
      {
      public:
        MRCProxy( const YIELD::URI& uri, uint8_t reconnect_tries_max = PROXY_DEFAULT_RECONNECT_TRIES_MAX, uint32_t flags = PROXY_DEFAULT_FLAGS );

        // EventHandler
        virtual void handleEvent( YIELD::Event& ev ) { Proxy::handleEvent( ev ); }

      private:
        ORG_XTREEMFS_INTERFACES_MRCINTERFACE_DUMMY_DEFINITIONS;
      };
    };
  };
};

#endif
