// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "yield/platform/volume_test.h"
#include "org/xtreemfs/client/dir_proxy.h"
#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/client/osd_proxy_factory.h"
#include "org/xtreemfs/client/volume.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class VolumeTestSuite : public YIELD::VolumeTestSuite<Volume>
      {
      public:
        VolumeTestSuite( const char* test_suite_name )
          : YIELD::VolumeTestSuite<Volume>( test_suite_name )
        {
          stage_group = new YIELD::SEDAStageGroup( "VolumeTestSuite StageGroup" );
          dir_proxy = org::xtreemfs::client::DIRProxy::create( *stage_group, YIELD::URI( "oncrpc://localhost/" ) );
          mrc_proxy = org::xtreemfs::client::MRCProxy::create( *stage_group, YIELD::URI( "oncrpc://localhost/" ) );
          osd_proxy_factory = new org::xtreemfs::client::OSDProxyFactory( dir_proxy, *stage_group );
          volume = new org::xtreemfs::client::Volume( "test", dir_proxy, mrc_proxy, osd_proxy_factory );
          addTests( *volume );
        }

      private:
        YIELD::auto_Object<YIELD::SEDAStageGroup> stage_group;
        YIELD::auto_Object<org::xtreemfs::client::DIRProxy> dir_proxy;
        YIELD::auto_Object<org::xtreemfs::client::MRCProxy> mrc_proxy;
        YIELD::auto_Object<org::xtreemfs::client::OSDProxyFactory> osd_proxy_factory;
        YIELD::auto_Object<org::xtreemfs::client::Volume> volume;
      };
    };
  };
};

TEST_SUITE_EX( Volume, org::xtreemfs::client::VolumeTestSuite )

TEST_MAIN( Volume )
