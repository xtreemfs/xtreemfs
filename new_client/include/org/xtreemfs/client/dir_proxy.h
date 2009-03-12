#ifndef ORG_XTREEMFS_CLIENT_DIR_PROXY_H
#define ORG_XTREEMFS_CLIENT_DIR_PROXY_H

#include "org/xtreemfs/client/proxy.h"
#define ORG_XTREEMFS_INTERFACES_DIRINTERFACE_INTERFACE_PARENT_CLASS org::xtreemfs::client::Proxy
#include "org/xtreemfs/interfaces/dir_interface.h"
#include "org/xtreemfs/client/proxy.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class DIRProxy : public org::xtreemfs::interfaces::DIRInterface
      {
      public:
        DIRProxy( const YIELD::URI& uri, uint8_t reconnect_tries_max = 3 );

        // EventHandler
        virtual void handleEvent( YIELD::Event& ev ) { Proxy::handleEvent( ev ); }

      private:
        ORG_XTREEMFS_INTERFACES_DIRINTERFACE_DUMMY_DEFINITIONS;
      };
    };
  };
};

#endif
