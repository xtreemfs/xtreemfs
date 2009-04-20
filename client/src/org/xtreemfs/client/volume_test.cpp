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
          : YIELD::VolumeTestSuite<Volume>( test_suite_name, volume ),
            stage_group( YIELD::SEDAStageGroup::createStageGroup() ),
            dir_proxy( "oncrpc://outtolunch/" ),
            mrc_proxy( "oncrpc://outtolunch/" ),
            osd_proxy_factory( dir_proxy, stage_group ),
            volume( "test", dir_proxy, mrc_proxy, osd_proxy_factory )
        {
          stage_group.createStage( dir_proxy );
          stage_group.createStage( mrc_proxy );
        }

        virtual ~VolumeTestSuite()
        {
          YIELD::SEDAStageGroup::destroyStageGroup( stage_group );
        }

      private:
        YIELD::SEDAStageGroup& stage_group;
        DIRProxy dir_proxy;
        MRCProxy mrc_proxy;
        OSDProxyFactory osd_proxy_factory;
        org::xtreemfs::client::Volume volume;
      };
    };
  };
};

TEST_SUITE_EX( Volume, org::xtreemfs::client::VolumeTestSuite )

TEST_MAIN( Volume )
