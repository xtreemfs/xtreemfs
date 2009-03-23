#include "org/xtreemfs/client.h"
using namespace org::xtreemfs::client;

#include "yield.h"

#include "yieldfs.h"

#include <string>
#include <vector>
#include <exception>
#include <iostream>

#include "SimpleOpt.h"

enum { OPT_FOREGROUND, OPT_DEBUG };

CSimpleOpt::SOption options[] = {
  { OPT_DEBUG, "-d", SO_NONE },
  { OPT_DEBUG, "--debug", SO_NONE },
  { OPT_FOREGROUND, "-f", SO_NONE },
  SO_END_OF_OPTIONS
};


int main( int argc, char** argv )
{
  // Options to fill
  bool foreground = false, debug = false;
  std::string mount_point, volume_name;

  try
  {
    CSimpleOpt args( argc, argv, options );

    // - options
    while ( args.Next() )
    {
      if ( args.LastError() == SO_SUCCESS )
      {
        switch ( args.OptionId() )
        {
          case OPT_FOREGROUND: foreground = true; break;
          case OPT_DEBUG: debug = true; break;
        }
      }
    }

    if ( args.FileCount() >= 3 )
    {
      if ( debug )
        YIELD::SocketConnection::setTraceSocketIO( true );

      YIELD::SEDAStageGroup& main_stage_group = YIELD::SEDAStageGroup::createStageGroup();

      // Create the DIRProxy
      std::string dir_uri( args.Files()[0] );
      if ( dir_uri.find( "://" ) == std::string::npos )
        dir_uri = "oncrpc://" + dir_uri;
      DIRProxy dir_proxy( dir_uri ); 
      main_stage_group.createStage( dir_proxy );

      // Create the MRCProxy
      std::string volume_name( args.Files()[1] );
      YIELD::URI mrc_uri = dir_proxy.getVolumeURIFromVolumeName( volume_name );
      MRCProxy mrc_proxy( mrc_uri );
      main_stage_group.createStage( mrc_proxy );

      // Create the OSDProxyFactory
      OSDProxyFactory osd_proxy_factory( dir_proxy, main_stage_group );

      // Start FUSE with the XtreemFS Volume
      Volume xtreemfs_volume( volume_name, dir_proxy, mrc_proxy, osd_proxy_factory );
      std::string mount_point( args.Files()[2] );
      int ret = yieldfs::FUSE( xtreemfs_volume ).main( argv[0], mount_point.c_str(), foreground, debug );

      YIELD::SEDAStageGroup::destroyStageGroup( main_stage_group ); // Must destroy the stage group before the event handlers go out of scope so the stages aren't holding dead pointers

      return ret;
    }
    else
      throw YIELD::Exception( "must specify directory service, volume_name, and mount point" );
  }
  catch ( std::exception& exc )
  {
    std::cerr << "Error mounting volume: " << exc.what() << std::endl;

    return 1;
  }
}
