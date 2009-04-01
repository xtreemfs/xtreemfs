#include "yield/platform/yunit.h"
using namespace YIELD;

#ifdef _WIN32
// #include <vld.h>
#endif

extern YIELD::TestSuite& OpenFileTestSuite();
extern YIELD::TestSuite& PathTestSuite();
extern YIELD::TestSuite& VolumeTestSuite();


int main( int argc, char** argv )
{
  int run_ret = 0;

  run_ret |= TestRunner().run( OpenFileTestSuite() );
  run_ret |= TestRunner().run( PathTestSuite() );
  run_ret |= TestRunner().run( VolumeTestSuite() );

  return run_ret;
}
