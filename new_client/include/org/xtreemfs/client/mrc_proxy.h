#ifndef ORG_XTREEMFS_CLIENT_MRC_PROXY_H
#define ORG_XTREEMFS_CLIENT_MRC_PROXY_H

#include "org/xtreemfs/client/proxy.h"
#define ORG_XTREEMFS_INTERFACES_MRCINTERFACE_PARENT_CLASS xtreemfs::client::Proxy
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
        MRCProxy( const YIELD::URI& uri, uint8_t reconnect_tries_max = 3 )
        {
          Proxy::init( uri, reconnect_tries_max );
          org::xtreemfs::interfaces::MRCInterface::registerSerializableFactories( serializable_factories ); 
        }

        // EventHandler
        virtual void handleEvent( YIELD::Event& ev ) { Proxy::handleEvent( ev ); }

      private:
        ORG_XTREEMFS_INTERFACES_MRCINTERFACE_DUMMY_DEFINITIONS;
      };
    };
  };
};

#endif
