// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

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
          : Options( "xtfs_mount", "mount an XtreemFS volume", "[oncrpc[s]://]<dir host>[:dir port]/<volume name> <mount point>" )
        {
          addOption( XTFS_MOUNT_OPTION_CACHE, "-c", "--cache" );
          cache = false;

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

        const bool get_cache() const { return cache; }
        const bool get_direct_io() const { return direct_io; }
        const YIELD::URI& get_dir_uri() const { return *dir_uri; }
        bool get_foreground() const { return foreground; }
        const std::string& get_fuse_o_args() const { return fuse_o_args; }
        const std::string& get_mount_point() const { return mount_point; }
        const std::string& get_volume_name() const { return volume_name; }

      private:
        bool cache;
        bool direct_io;
        YIELD::URI* dir_uri;
        bool foreground;
        std::string fuse_o_args;
        std::string mount_point, volume_name;

        enum
        {
          XTFS_MOUNT_OPTION_CACHE = 10,
          XTFS_MOUNT_OPTION_DIRECT_IO = 11,
          XTFS_MOUNT_OPTION_FOREGROUND = 12,
          XTFS_MOUNT_OPTION_FUSE_OPTION = 13
        };

        // OptionParser
        void parseOption( int id, char* arg )
        {
          switch ( id )
          {
            case XTFS_MOUNT_OPTION_CACHE: cache = true; break;
            case XTFS_MOUNT_OPTION_FOREGROUND: foreground = true; break;

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
          if ( file_count >= 2 )
          {
            dir_uri = parseURI( files[0] );
            if ( strlen( dir_uri->get_resource() ) > 1 )
            {
              volume_name = dir_uri->get_resource() + 1;
              mount_point = files[1];
              return;
            }
          }

          throw YIELD::Exception( "must specify dir_host/volume name and mount point" );
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
      YIELD::SEDAStageGroup& main_stage_group = YIELD::SEDAStageGroup::createStageGroup();

      // Create the DIRProxy
      YIELD::auto_SharedObject<DIRProxy> dir_proxy = options.createProxy<DIRProxy>( options.get_dir_uri() );
      main_stage_group.createStage( *dir_proxy.get() );

      // Create the MRCProxy
      YIELD::URI mrc_uri = dir_proxy.get()->getVolumeURIFromVolumeName( options.get_volume_name() );
      YIELD::auto_SharedObject<MRCProxy> mrc_proxy = options.createProxy<MRCProxy>( mrc_uri );
      main_stage_group.createStage( *mrc_proxy.get() );

      // Create the OSDProxyFactory
      OSDProxyFactory osd_proxy_factory( *dir_proxy.get(), main_stage_group );

      // Start FUSE with an XtreemFS volume
      YIELD::Volume* xtreemfs_volume = new Volume( options.get_volume_name(), *dir_proxy.get(), *mrc_proxy.get(), osd_proxy_factory );

      if ( options.get_cache() )
        xtreemfs_volume = new yieldfs::TimeCachedVolume( YIELD::SharedObject::incRef( *xtreemfs_volume ) );
      if ( options.get_debug() )
        xtreemfs_volume = new yieldfs::TracingVolume( YIELD::SharedObject::incRef( *xtreemfs_volume ) );

      uint32_t fuse_flags = yieldfs::FUSE::FUSE_FLAGS_DEFAULT;
      if ( options.get_debug() )
    	  fuse_flags |= yieldfs::FUSE::FUSE_FLAG_DEBUG;
      if ( options.get_direct_io() )
    	  fuse_flags |= yieldfs::FUSE::FUSE_FLAG_DIRECT_IO;
      if ( options.get_foreground() )
    	  fuse_flags |= yieldfs::FUSE::FUSE_FLAG_FOREGROUND;

      yieldfs::FUSE fuse( *xtreemfs_volume, fuse_flags );
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

      YIELD::SharedObject::decRef( *xtreemfs_volume );

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
