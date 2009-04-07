// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client.h"
#include "xtfs_bin.h"
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
      class xtfs_mount : public xtfs_bin
      {
      public:
        xtfs_mount()
          : xtfs_bin( "xtfs_mount", "mount an XtreemFS volume", "[oncrpc[s]://]<dir host>[:dir port]/<volume name> <mount point>" )
        {
          addOption( XTFS_MOUNT_OPTION_CACHE, "-c", "--cache" );
          cache = false;

          direct_io = false;

          addOption( XTFS_MOUNT_OPTION_FOREGROUND, "-f", "--foreground" );
          foreground = false;

          addOption( XTFS_MOUNT_OPTION_FUSE_OPTION, "-o", NULL, "<fuse_option>" );
        }

      private:
        enum
        {
          XTFS_MOUNT_OPTION_CACHE = 10,
          XTFS_MOUNT_OPTION_DIRECT_IO = 11,
          XTFS_MOUNT_OPTION_FOREGROUND = 12,
          XTFS_MOUNT_OPTION_FUSE_OPTION = 13
        };

        bool cache;
        bool direct_io;
        std::auto_ptr<YIELD::URI> dir_uri;
        bool foreground;
        std::string fuse_o_args;
        std::string mount_point, volume_name;


        // xtfs_bin
        int _main()
        {
          YIELD::SEDAStageGroup& main_stage_group = YIELD::SEDAStageGroup::createStageGroup();

          // Create the DIRProxy
          YIELD::auto_SharedObject<DIRProxy> dir_proxy = createProxy<DIRProxy>( *dir_uri.get() );
          main_stage_group.createStage( *dir_proxy.get() );

          // Create the MRCProxy
          YIELD::URI mrc_uri = dir_proxy.get()->getVolumeURIFromVolumeName( volume_name );
          YIELD::auto_SharedObject<MRCProxy> mrc_proxy = createProxy<MRCProxy>( mrc_uri );
          main_stage_group.createStage( *mrc_proxy.get() );

          // Create the OSDProxyFactory
          OSDProxyFactory osd_proxy_factory( *dir_proxy.get(), main_stage_group );

          // Start FUSE with an XtreemFS volume
          YIELD::Volume* xtreemfs_volume = new Volume( volume_name, *dir_proxy.get(), *mrc_proxy.get(), osd_proxy_factory );

          if ( cache )
            xtreemfs_volume = new yieldfs::TTLCachedVolume( YIELD::SharedObject::incRef( *xtreemfs_volume ) );
          if ( get_debug_level() >= DEBUG_LEVEL_TRACE )
            xtreemfs_volume = new yieldfs::TracingVolume( YIELD::SharedObject::incRef( *xtreemfs_volume ) );

          uint32_t fuse_flags = yieldfs::FUSE::FUSE_FLAGS_DEFAULT;
          if ( get_debug_level() >= DEBUG_LEVEL_DEBUG )
    	      fuse_flags |= yieldfs::FUSE::FUSE_FLAG_DEBUG;
          if ( direct_io )
    	      fuse_flags |= yieldfs::FUSE::FUSE_FLAG_DIRECT_IO;
          if ( foreground )
    	      fuse_flags |= yieldfs::FUSE::FUSE_FLAG_FOREGROUND;

          yieldfs::FUSE fuse( *xtreemfs_volume, fuse_flags );
          int ret;
#ifdef _WIN32
          ret = fuse.main( mount_point.c_str() );
#else
          std::vector<char*> argvv;
    //      argvv.push_back( argv[0] );
          if ( get_debug_level() >= DEBUG_LEVEL_DEBUG )
            argvv.push_back( const_cast<char*>( "-d" ) );
          if ( foreground )
            argvv.push_back( const_cast<char*>( "-f" ) );
          if ( !fuse_o_args.empty() )
          {
            std::string fuse_o_args( "-o" );
            fuse_o_args.append( this->fuse_o_args );
            argvv.push_back( const_cast<char*>( fuse_o_args.c_str() ) );
          }
          argvv.push_back( NULL );
          struct fuse_args fuse_args_ = FUSE_ARGS_INIT( argvv.size() - 1 , &argvv[0] );
          ret = fuse.main( fuse_args_, mount_point.c_str() );
#endif

          YIELD::SharedObject::decRef( *xtreemfs_volume );

          YIELD::SEDAStageGroup::destroyStageGroup( main_stage_group ); // Must destroy the stage group before the event handlers go out of scope so the stages aren't holding dead pointers

          return ret;
        }

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
  return xtfs_mount().main( argc, argv );
}
