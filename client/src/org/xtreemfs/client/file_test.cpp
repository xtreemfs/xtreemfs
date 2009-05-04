// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "yield/platform/file_test.h"
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
      class FileTestSuite : public YIELD::FileTestSuite
      {
      public:
        FileTestSuite( const char* test_suite_name )
          : YIELD::FileTestSuite( test_suite_name )
        {
          stage_group = new YIELD::SEDAStageGroup( "VolumeTestSuite StageGroup" );
          dir_proxy = org::xtreemfs::client::DIRProxy::create( *stage_group, YIELD::URI( "oncrpc://localhost/" ) );
          mrc_proxy = org::xtreemfs::client::MRCProxy::create( *stage_group, YIELD::URI( "oncrpc://localhost/" ) );
          osd_proxy_factory = new org::xtreemfs::client::OSDProxyFactory( dir_proxy, *stage_group );
          volume = new org::xtreemfs::client::Volume( "test", dir_proxy, mrc_proxy, osd_proxy_factory );
        }

        // YIELD::FileTestSuite
        YIELD::File* createFile( const YIELD::Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
        {
          return volume->open( path, flags, mode, attributes ).release();
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

TEST_SUITE_EX( File, org::xtreemfs::client::FileTestSuite )

TEST_MAIN( File )
