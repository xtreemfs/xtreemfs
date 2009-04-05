#include "yield/platform/yunit.h"


extern YIELD::TestSuite& VolumeTestSuite();
extern YIELD::TestSuite& PathTestSuite();
extern YIELD::TestSuite& VolumeTestSuite();


int main( int argc, char** argv )
{
  YIELD::TestRunner test_runner; 
  int run_ret = 0;

  run_ret |= test_runner.run( VolumeTestSuite() );
  run_ret |= test_runner.run( PathTestSuite() );
  run_ret |= test_runner.run( VolumeTestSuite() );

  return run_ret;
}
