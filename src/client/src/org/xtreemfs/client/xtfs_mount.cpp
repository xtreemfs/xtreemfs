// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "main.h"
using namespace org::xtreemfs::client;

#include "org/xtreemfs/interfaces/constants.h"

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

          addOption( XTFS_MOUNT_OPTION_PARENT_NAMED_PIPE_PATH, "--parent-named-pipe-path", NULL, "internal only" );
        }

      private:
        enum
        {
          XTFS_MOUNT_OPTION_CACHE_FILES = 10,
          XTFS_MOUNT_OPTION_CACHE_METADATA = 11,
          XTFS_MOUNT_OPTION_DIRECT_IO = 12,
          XTFS_MOUNT_OPTION_FOREGROUND = 13,
          XTFS_MOUNT_OPTION_FUSE_OPTION = 14,
          XTFS_MOUNT_OPTION_PARENT_NAMED_PIPE_PATH = 15
        };

        bool cache_files, cache_metadata;
        bool direct_io;
        YIELD::auto_Object<YIELD::URI> dir_uri;
        bool foreground;
        std::string fuse_o_args;
        std::string mount_point, volume_name;
        YIELD::Path parent_named_pipe_path;


        // YIELD::Main
        int _main( int argc, char** argv )
        {
          int ret = 0;

          if ( foreground )
          {
            uint32_t volume_flags = 0;
            if ( cache_files )
              volume_flags |= Volume::VOLUME_FLAG_CACHE_FILES;
            if ( cache_metadata )
              volume_flags |= Volume::VOLUME_FLAG_CACHE_METADATA;

            YIELD::auto_Object<YIELD::Volume> volume = new Volume( *dir_uri, volume_name, volume_flags, get_log()
#ifdef YIELD_HAVE_OPENSSL
                                                                   , get_ssl_context() 
#endif
                                                                 );

            // Stack volumes as indicated
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

            // Set flags to pass to FUSE based on command line options
            uint32_t fuse_flags = 0;
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

            YIELD::auto_Object<YIELD::NamedPipe> parent_named_pipe; // Outside the if so it stays in scope (and open) while the client is running
            if ( !parent_named_pipe_path.empty() )
            {
              parent_named_pipe = YIELD::NamedPipe::open( parent_named_pipe_path );
              if ( parent_named_pipe != NULL )
              {
                int parent_ret = 0;
                parent_named_pipe->write( &parent_ret, sizeof( parent_ret ) );
              }
            }

            // Create the FUSE object then run forever in its main()
            YIELD::auto_Object<yieldfs::FUSE> fuse = new yieldfs::FUSE( volume, fuse_flags, get_log() );
#ifdef _WIN32
            ret = fuse->main( mount_point.c_str() );
#else
            if ( fuse_o_args.empty() && fuse_flags == 0 )
              ret = fuse->main( argv[0], mount_point.c_str() );
            else
            {
              std::vector<char*> argvv;
              argvv.push_back( argv[0] );

              if ( ( fuse_flags & yieldfs::FUSE::FUSE_FLAG_DEBUG ) == yieldfs::FUSE::FUSE_FLAG_DEBUG )
                argvv.push_back( "-d" );

              if ( !fuse_o_args.empty() )
              {
                argvv.push_back( "-o" );
                argvv.push_back( const_cast<char*>( fuse_o_args.c_str() ) );
                get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": passing -o " << fuse_o_args << " to FUSE.";
              }

              argvv.push_back( NULL );

              struct fuse_args fuse_args_ = FUSE_ARGS_INIT( argvv.size() - 1 , &argvv[0] );
              ret = fuse->main( fuse_args_, mount_point.c_str() );
            }
#endif

            get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": returning exit code " << ret << ".";
          }
          else // !foreground
          {
            YIELD::Path named_pipe_path( "xtfs_mount" );
            YIELD::auto_Object<YIELD::NamedPipe> server_named_pipe = YIELD::NamedPipe::open( named_pipe_path, O_CREAT|O_RDWR );
            std::vector<char*> argvv;
            argvv.push_back( const_cast<char*>( "--parent-named-pipe-path" ) );
            argvv.push_back( const_cast<char*>( static_cast<const char*>( named_pipe_path ) ) );
            for ( int arg_i = 1; arg_i < argc; arg_i++ )
            {
              if ( strcmp( argv[arg_i], "-f" ) != 0 )
                argvv.push_back( argv[arg_i] );
            }
            argvv.push_back( NULL );
            YIELD::auto_Object<YIELD::Process> child_process = YIELD::Process::create( argv[0], ( const char** )&argvv[0] );
            if ( child_process != NULL )
            { 
              YIELD::Thread::sleep( 100 * NS_IN_MS );
              if ( !child_process->poll( &ret ) )
              {
                YIELD::Stream::Status read_status = server_named_pipe->read( &ret, sizeof( ret ) );
                if ( read_status != YIELD::Stream::STREAM_STATUS_OK )
                {
                  get_log()->getStream( YIELD::Log::LOG_ERR ) << get_program_name() << ": parent xtfs_mount could not read from named pipe to client, error: " << YIELD::Exception::strerror();
                  ret = 1;
                }
              }
            }
          }

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

            case XTFS_MOUNT_OPTION_PARENT_NAMED_PIPE_PATH:
            {
              parent_named_pipe_path = arg;
              foreground = true;
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
