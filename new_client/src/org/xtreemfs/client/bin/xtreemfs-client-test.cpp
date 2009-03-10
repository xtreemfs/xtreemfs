#include "yield/platform/yunit.h"
using namespace YIELD;


//extern YIELD::TestSuite& VolumeTestSuite();
extern YIELD::TestSuite& PathTestSuite();


int main( int argc, char** argv )
{
//  return TestRunner().run( VolumeTestSuite() );
  return TestRunner().run( PathTestSuite() );
}
