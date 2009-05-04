// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "yield/platform/file_test.h"
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
          volume = new org::xtreemfs::client::Volume( "test", "localhost" );
        }

        // YIELD::FileTestSuite
        YIELD::File* createFile( const YIELD::Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
        {
          return volume->open( path, flags, mode, attributes ).release();
        }

      private:
        YIELD::auto_Object<org::xtreemfs::client::Volume> volume;
      };
    };
  };
};

TEST_SUITE_EX( File, org::xtreemfs::client::FileTestSuite )

TEST_MAIN( File )
