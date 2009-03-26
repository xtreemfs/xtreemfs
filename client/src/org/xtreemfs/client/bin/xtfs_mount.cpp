#include "org/xtreemfs/client.h"
#include "options.h"
using namespace org::xtreemfs::client;

#include "yield.h"
#include "yieldfs.h"

#include <vector>
#include <exception>

#ifndef _WIN32
#define FUSE_USE_VERSION 26
#include <fuse.h>
#endif


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
          direct_io = false;

          addOption( XTFS_MOUNT_OPTION_FOREGROUND, "-f", "--foreground" );
          foreground = false;

          addOption( XTFS_MOUNT_OPTION_FUSE_OPTION, "-o", NULL, "<fuse_option>" );

          dir_uri = NULL;

          parseOptions( argc, argv );
        }

        ~xtfs_mountOptions()
        {
          delete dir_uri;
        }

        const bool get_direct_io() const { return direct_io; }
        const YIELD::URI& get_dir_uri() const { return *dir_uri; }
        const std::string& get_fuse_o_args() const { return fuse_o_args; }
        bool get_foreground() const { return foreground; }
        const std::string& get_mount_point() const { return mount_point; }
        const std::string& get_volume_name() const { return volume_name; }

      private:
        YIELD::URI* dir_uri;
        bool direct_io, foreground;
        std::string fuse_o_args;
        std::string mount_point, volume_name;

        enum
        {
          XTFS_MOUNT_OPTION_DIRECT_IO = 10,
          XTFS_MOUNT_OPTION_FOREGROUND = 11,
          XTFS_MOUNT_OPTION_FUSE_OPTION = 12
        };

        // OptionParser
        void parseOption( int id, char* arg )
        {
          switch ( id )
          {
            case XTFS_MOUNT_OPTION_FOREGROUND:
            {
              foreground = true;
            }
            break;

            case XTFS_MOUNT_OPTION_FUSE_OPTION:
            {
              if ( !fuse_o_args.empty() )
                fuse_o_args.append( "," );
              fuse_o_args.append( arg );

              if ( strstr( arg, "direct_io" ) != NULL )
                direct_io = true;
            }
            break;
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

      YIELD::Volume* fuse_volume;
      if ( options.get_debug() )
        fuse_volume = new yieldfs::TracingVolume( xtreemfs_volume );
      else
        fuse_volume = &xtreemfs_volume;

      yieldfs::FUSE fuse( *fuse_volume, fuse_flags );
      int ret;
#ifdef _WIN32
      ret = fuse.main( options.get_mount_point().c_str() );
#else
      std::vector<char*> argvv;
//      argvv.push_back( argv[0] );
      if ( options.get_debug() )
        argvv.push_back( const_cast<char*>( "-d" ) );
      if ( options.get_foreground() )
        argvv.push_back( const_cast<char*>( "-f" ) );
      if ( !options.get_fuse_o_args().empty() )
      {
        std::string fuse_o_args( "-o" );
        fuse_o_args.append( options.get_fuse_o_args() );
        argvv.push_back( const_cast<char*>( fuse_o_args.c_str() ) );
      }
      argvv.push_back( NULL );
      struct fuse_args fuse_args_ = FUSE_ARGS_INIT( argvv.size() - 1 , &argvv[0] );
      ret = fuse.main( fuse_args_, options.get_mount_point().c_str() );
#endif

      if ( options.get_debug() )
        delete fuse_volume;

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
