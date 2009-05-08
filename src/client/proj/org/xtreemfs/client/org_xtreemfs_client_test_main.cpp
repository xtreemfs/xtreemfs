// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "yield/main.h"


extern YIELD::TestSuite& FileTestSuite();
extern YIELD::TestSuite& PathTestSuite();
extern YIELD::TestSuite& VolumeTestSuite();


class org_xtreemfs_client_testMain : public YIELD::Main
{
public:
  org_xtreemfs_client_testMain()
    : YIELD::Main( "org_xtreemfs_client_test" )
    { }

  // YIELD::Main
  int _main( int argc, char** argv )
  {
    YIELD::TestRunner test_runner;
    int run_ret = 0;

    run_ret |= test_runner.run( FileTestSuite() );
    run_ret |= test_runner.run( PathTestSuite() );
    run_ret |= test_runner.run( VolumeTestSuite() );

    return run_ret;
  }
};

int main( int argc, char** argv )
{
  return org_xtreemfs_client_testMain().main( argc, argv );
}

