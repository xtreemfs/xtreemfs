#include "org/xtreemfs/client.h"
#include "options.h"
using namespace org::xtreemfs::client;

#include "yield.h"
#include "yieldfs.h"

#include <exception>


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class xtfs_mountOptions : public Options
      {
      public:
        xtfs_mountOptions( int argc, char** argv )
          : Options( "xtfs_mount", "mount an XtreemFS volume", "<directory service URI> <volume_name> <mount point>" )
        {
  		  addOption( XTFS_MOUNT_OPTION_DIRECT_IO, "--direct_io", "--direct-io" );
		  direct_io = false;

          addOption( XTFS_MOUNT_OPTION_FOREGROUND, "-f", "--foreground" );
          foreground = false;

          dir_uri = NULL;

          parseOptions( argc, argv );
        }

        ~xtfs_mountOptions()
        {
          delete dir_uri;
        }

        bool get_direct_io() const { return direct_io; }
        const YIELD::URI& get_dir_uri() const { return *dir_uri; }
        bool get_foreground() const { return foreground; }
        const std::string& get_mount_point() const { return mount_point; }
        const std::string& get_volume_name() const { return volume_name; }

      private:
        bool direct_io, foreground;
        YIELD::URI* dir_uri;
        std::string mount_point, volume_name;

        enum
        {
          XTFS_MOUNT_OPTION_DIRECT_IO = 10,
          XTFS_MOUNT_OPTION_FOREGROUND = 11
        };

        // OptionParser
        void parseOption( int id, const char* )
        {
          switch ( id )
          {
            case XTFS_MOUNT_OPTION_DIRECT_IO: direct_io = true; break;
            case XTFS_MOUNT_OPTION_FOREGROUND: foreground = true; break;
          }
        }

        void parseFiles( int file_count, char** files )
        {
          if ( file_count >= 3 )
          {
            dir_uri = parseURI( files[0] );
            volume_name = files[1];
            mount_point = files[2];
          }
          else
            throw YIELD::Exception( "must specify directory service URI, volume name, and mount point" );
        }
      };
    };
  };
};


int main( int argc, char** argv )
{
  try
  {
    xtfs_mountOptions options( argc, argv );

    if ( options.get_help() )
      options.printUsage();
    else
    {
      if ( options.get_debug() )
        YIELD::SocketConnection::setTraceSocketIO( true );

      YIELD::SEDAStageGroup& main_stage_group = YIELD::SEDAStageGroup::createStageGroup();

      // Create the DIRProxy
      DIRProxy dir_proxy( options.get_dir_uri() );
      main_stage_group.createStage( dir_proxy );

      // Create the MRCProxy
      YIELD::URI mrc_uri = dir_proxy.getVolumeURIFromVolumeName( options.get_volume_name() );
      MRCProxy mrc_proxy( mrc_uri );
      main_stage_group.createStage( mrc_proxy );

      // Create the OSDProxyFactory
      OSDProxyFactory osd_proxy_factory( dir_proxy, main_stage_group );

      // Start FUSE with an XtreemFS volume
      Volume xtreemfs_volume( options.get_volume_name(), dir_proxy, mrc_proxy, osd_proxy_factory );

      uint32_t fuse_flags = yieldfs::FUSE::FUSE_FLAGS_DEFAULT;
      if ( options.get_debug() )
    	  fuse_flags |= yieldfs::FUSE::FUSE_FLAG_DEBUG;
      if ( options.get_direct_io() )
    	  fuse_flags |= yieldfs::FUSE::FUSE_FLAG_DIRECT_IO;
      if ( options.get_foreground() )
    	  fuse_flags |= yieldfs::FUSE::FUSE_FLAG_FOREGROUND;

      int ret;
      if ( options.get_debug() )
      {
        yieldfs::TracingVolume tracing_xtreemfs_volume( xtreemfs_volume );
        ret = yieldfs::FUSE( tracing_xtreemfs_volume, fuse_flags ).main( argv[0], options.get_mount_point().c_str() );
      }
      else
        ret = yieldfs::FUSE( xtreemfs_volume, fuse_flags ).main( argv[0], options.get_mount_point().c_str() );

      YIELD::SEDAStageGroup::destroyStageGroup( main_stage_group ); // Must destroy the stage group before the event handlers go out of scope so the stages aren't holding dead pointers

      return ret;
    }
  }
  catch ( std::exception& exc )
  {
    std::cerr << "Error mounting volume: " << exc.what() << std::endl;

    return 1;
  }
}
