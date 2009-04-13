// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "yield/platform/yunit.h"
#include "org/xtreemfs/client/path.h"
using namespace org::xtreemfs::client;


#define TEST_VOLUME_NAME "testvol"
#define TEST_UNIX_PATH "somedir/somefile.txt"
#ifdef _WIN32
#define TEST_LOCAL_PATH "somedir\\somefile.txt"
#else
#define TEST_LOCAL_PATH TEST_UNIX_PATH
#endif
#define TEST_FILE_NAME "somefile.txt"


TEST_SUITE( Path )

class PathTestCase : public YIELD::TestCase
{
public:
  PathTestCase( const char* short_description, YIELD::TestSuite& __test_suite )
    : YIELD::TestCase( short_description, __test_suite )
  { }

protected:
  void checkPath( const Path& path )
  {
    ASSERT_EQUAL( path.get_volume_name(), TEST_VOLUME_NAME );
    ASSERT_EQUAL( path.get_global_path(), std::string( TEST_VOLUME_NAME ) + "/" + TEST_UNIX_PATH );
    ASSERT_EQUAL( path.get_local_path(), TEST_LOCAL_PATH );
    ASSERT_EQUAL( path.get_local_path().split().second, TEST_FILE_NAME );
  }
};

TEST_EX( Path_fromglobal, PathTestCase, Path )
{
  checkPath( Path( std::string( TEST_VOLUME_NAME ) + "/" + TEST_UNIX_PATH ) );
}

TEST_EX( Path_fromlocal, PathTestCase, Path )
{
  checkPath( Path( TEST_VOLUME_NAME, TEST_LOCAL_PATH ) );
}

TEST_EX( Path_emptylocal, PathTestCase, Path )
{
  Path path( TEST_VOLUME_NAME, "" );
  ASSERT_EQUAL( path.get_volume_name(), TEST_VOLUME_NAME );
  ASSERT_EQUAL( path.get_global_path(), std::string( TEST_VOLUME_NAME ) + "/" );
  ASSERT_TRUE( path.get_local_path().empty() );
}

TEST_MAIN( Path )

