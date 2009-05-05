// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client.h"
#include "main.h"
using namespace org::xtreemfs::client;

#include "org/xtreemfs/interfaces/constants.h"

#include "yield.h"
#include "yieldfs.h"

#include <vector>
#include <exception>

#ifndef _WIN32
#define FUSE_USE_VERSION 26
#include <fuse.h>
#include <unistd.h>
#endif


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class xtfs_mount : public Main
      {
      public:
        xtfs_mount()
          : Main( "xtfs_mount", "mount an XtreemFS volume", "[oncrpc[s]://]<dir host>[:dir port]/<volume name> <mount point>" )
        {
          addOption( XTFS_MOUNT_OPTION_CACHE_FILES, "--cache-files" );
          cache_files = false;

          addOption( XTFS_MOUNT_OPTION_CACHE_METADATA, "--cache-metadata" );
          cache_metadata = false;

          direct_io = false;

          addOption( XTFS_MOUNT_OPTION_FOREGROUND, "-f", "--foreground" );
          foreground = false;

          addOption( XTFS_MOUNT_OPTION_FUSE_OPTION, "-o", NULL, "<fuse_option>" );
        }

      private:
        enum
        {
          XTFS_MOUNT_OPTION_CACHE_FILES = 10,
          XTFS_MOUNT_OPTION_CACHE_METADATA = 11,
          XTFS_MOUNT_OPTION_DIRECT_IO = 12,
          XTFS_MOUNT_OPTION_FOREGROUND = 13,
          XTFS_MOUNT_OPTION_FUSE_OPTION = 14
        };

        bool cache_files, cache_metadata;
        bool direct_io;
        YIELD::auto_Object<YIELD::URI> dir_uri;
        bool foreground;
        std::string fuse_o_args;
        std::string mount_point, volume_name;


        // YIELD::Main
        int _main( int argc, char** argv )
        {
          if ( foreground )
            get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": running in foreground.";
          else
          {
#ifndef _WIN32
            if ( daemon( 1, 0 ) == -1 )
              return errno;
#endif
          }

          uint32_t volume_flags = 0;
          if ( cache_files )
            volume_flags |= Volume::VOLUME_FLAG_CACHE_FILES;
          if ( cache_metadata )
            volume_flags |= Volume::VOLUME_FLAG_CACHE_METADATA;

          YIELD::auto_Object<YIELD::Volume> volume = new Volume( *dir_uri, volume_name, get_ssl_context(), volume_flags, get_log() );

          if ( cache_files )
          {
            volume = new yieldfs::FileCachingVolume( volume, get_log() );
            get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": caching files.";
          }

          if ( cache_metadata )
          {
            volume = new yieldfs::StatCachingVolume( volume, get_log(), 5 );
            get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": caching metadata.";
          }

          if ( get_log_level() >= YIELD::Log::LOG_INFO )
          {
            volume = new yieldfs::TracingVolume( volume, get_log() );
            get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": tracing volume operations.";
          }

          uint32_t fuse_flags = yieldfs::FUSE::FUSE_FLAGS_DEFAULT;
          if ( get_log_level() >= YIELD::Log::LOG_INFO )
          {
    	      fuse_flags |= yieldfs::FUSE::FUSE_FLAG_DEBUG;
    	      get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": enabling FUSE debugging.";
          }
          if ( direct_io )
          {
    	      fuse_flags |= yieldfs::FUSE::FUSE_FLAG_DIRECT_IO;
            get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": enabling FUSE direct I/O.";
          }

          YIELD::auto_Object<yieldfs::FUSE> fuse = new yieldfs::FUSE( volume, get_log(), fuse_flags );
          int ret;
#ifdef _WIN32
          ret = fuse->main( mount_point.c_str() );
#else
          if ( fuse_o_args.empty() )
            ret = fuse->main( argv[0], mount_point.c_str() );
          else
          {
            std::vector<char*> argvv;
            argvv.push_back( argv[0] );
            argvv.push_back( "-o" );
            argvv.push_back( const_cast<char*>( fuse_o_args.c_str() ) );
            argvv.push_back( NULL );
            get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": passing -o " << fuse_o_args << " to FUSE.";
            struct fuse_args fuse_args_ = FUSE_ARGS_INIT( argvv.size() - 1 , &argvv[0] );
            ret = fuse->main( fuse_args_, mount_point.c_str() );
          }
#endif

          get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": returning exit code " << ret << ".";

          return ret;
        }

        void parseOption( int id, char* arg )
        {
          switch ( id )
          {
            case XTFS_MOUNT_OPTION_CACHE_FILES: cache_files = true; break;
            case XTFS_MOUNT_OPTION_CACHE_METADATA: cache_metadata = true; break;
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

            default: Main::parseOption( id, arg ); break;
          }
        }

        void parseFiles( int file_count, char** files )
        {
          if ( file_count >= 2 )
          {
            dir_uri = parseVolumeURI( files[0], volume_name );
            if ( dir_uri->get_port() == 0 )
              dir_uri->set_port( org::xtreemfs::interfaces::DIRInterface::DEFAULT_ONCRPC_PORT );
            mount_point = files[1];
            return;
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
