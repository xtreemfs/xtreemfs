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
          addOption( XTFS_MOUNT_OPTION_CACHE_DATA, "--cache-data" );
          cache_data = false;

          addOption( XTFS_MOUNT_OPTION_CACHE_METADATA, "--cache-metadata" );
          cache_metadata = false;

          direct_io = false;

          addOption( XTFS_MOUNT_OPTION_FOREGROUND, "-f", "--foreground" );
          foreground = false;

          addOption( XTFS_MOUNT_OPTION_FUSE_OPTION, "-o", NULL, "<fuse_option>" );

          addOption( XTFS_MOUNT_OPTION_PARENT_NAMED_PIPE_PATH, "--parent-named-pipe-path", NULL, "internal only" );

          addOption( XTFS_MOUNT_OPTION_TRACE_DATA_CACHE, "--trace-data-cache" );
          trace_data_cache = false;

          addOption( XTFS_MOUNT_OPTION_TRACE_FILE_IO, "--trace-file-io" );
          trace_file_io = false;

          addOption( XTFS_MOUNT_OPTION_TRACE_METADATA_CACHE, "--trace-metadata-cache" );
          trace_metadata_cache = false;

          addOption( XTFS_MOUNT_OPTION_TRACE_VOLUME_OPERATIONS, "--trace-volume-operations" );
          trace_volume_operations = false;
        }

      private:
        enum
        {
          XTFS_MOUNT_OPTION_CACHE_DATA = 20,
          XTFS_MOUNT_OPTION_CACHE_METADATA = 21,
          XTFS_MOUNT_OPTION_DIRECT_IO = 22,
          XTFS_MOUNT_OPTION_FOREGROUND = 23,
          XTFS_MOUNT_OPTION_FUSE_OPTION = 24,
          XTFS_MOUNT_OPTION_PARENT_NAMED_PIPE_PATH = 25,
          XTFS_MOUNT_OPTION_TRACE_DATA_CACHE = 26,
          XTFS_MOUNT_OPTION_TRACE_FILE_IO = 27,
          XTFS_MOUNT_OPTION_TRACE_METADATA_CACHE = 28,
          XTFS_MOUNT_OPTION_TRACE_VOLUME_OPERATIONS = 29
        };

        bool cache_data, cache_metadata;
        bool direct_io;
        YIELD::auto_URI dir_uri;
        bool foreground;
        std::string fuse_o_args;
        std::string mount_point, volume_name;
        YIELD::Path parent_named_pipe_path;
        bool trace_data_cache, trace_file_io, trace_metadata_cache, trace_volume_operations;


        // YIELD::Main
        int _main( int argc, char** argv )
        {
          int ret = 0;

          if ( foreground )
          {
            uint32_t fuse_flags = 0, volume_flags = 0;

            if ( get_log_level() >= YIELD::Log::LOG_INFO )
              trace_volume_operations = true;              
            if ( get_log_level() >= YIELD::Log::LOG_DEBUG )
            {
              trace_data_cache = true;
              trace_file_io = true;
              trace_metadata_cache = true;
            }

            if ( get_log_level() < YIELD::Log::LOG_INFO &&
                 ( trace_data_cache || 
                   trace_file_io || 
                   trace_metadata_cache || 
                   get_proxy_flags() != 0 || 
                   trace_volume_operations ) )
              get_log()->set_level( YIELD::Log::LOG_INFO );

            if ( cache_data )
              volume_flags |= Volume::VOLUME_FLAG_CACHE_FILES;
            if ( cache_metadata )
              volume_flags |= Volume::VOLUME_FLAG_CACHE_METADATA;
            if ( trace_file_io )
              volume_flags |= Volume::VOLUME_FLAG_TRACE_FILE_IO;

            YIELD::auto_Volume volume = new Volume( *dir_uri, volume_name, volume_flags, get_log(), get_proxy_flags(), get_ssl_context() );

            // Stack volumes as indicated
            if ( cache_data )
            {
              volume = new yieldfs::DataCachingVolume( volume, trace_data_cache ? get_log() : NULL );
              get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": caching data.";
            }

            if ( cache_metadata )
            {
              volume = new yieldfs::MetadataCachingVolume( volume, trace_metadata_cache ? get_log() : NULL, 5 );
              get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": caching metadata.";
            }

            if ( direct_io )
            {
              fuse_flags |= yieldfs::FUSE::FUSE_FLAG_DIRECT_IO;
              get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": enabling FUSE direct I/O.";
            }

            if ( trace_volume_operations && get_log_level() >= YIELD::Log::LOG_INFO )
            {
              volume = new yieldfs::TracingVolume( volume, get_log() );
              fuse_flags |= yieldfs::FUSE::FUSE_FLAG_DEBUG;
              get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": tracing volume operations.";
            }

            YIELD::auto_NamedPipe parent_named_pipe; // Outside the if so it stays in scope (and open) while the client is running
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
            std::auto_ptr<yieldfs::FUSE> fuse( new yieldfs::FUSE( volume, fuse_flags ) );
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
                if ( server_named_pipe->read( &ret, sizeof( ret ) ) != sizeof( ret ) )
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
            case XTFS_MOUNT_OPTION_CACHE_DATA: cache_data = true; break;
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

            case XTFS_MOUNT_OPTION_TRACE_DATA_CACHE: trace_data_cache = true; break;
            case XTFS_MOUNT_OPTION_TRACE_FILE_IO: trace_file_io = true; break;
            case XTFS_MOUNT_OPTION_TRACE_METADATA_CACHE: trace_metadata_cache = true; break;
            case XTFS_MOUNT_OPTION_TRACE_VOLUME_OPERATIONS: trace_volume_operations = true; break;
            
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
