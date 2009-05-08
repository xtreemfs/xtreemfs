// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "yield/platform/volume_test.h"
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
          volume = new org::xtreemfs::client::Volume( "localhost", "test" );
          addTests( *volume );
        }

      private:
        YIELD::auto_Object<org::xtreemfs::client::Volume> volume;
      };
    };
  };
};

TEST_SUITE_EX( Volume, org::xtreemfs::client::VolumeTestSuite )

TEST_MAIN( Volume )
