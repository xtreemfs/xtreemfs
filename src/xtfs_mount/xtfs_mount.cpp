// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/main.h"

#include "yieldfs.h"

#include <vector>
#include <exception>

#ifndef _WIN32
#define FUSE_USE_VERSION 26
#include <fuse.h>
#include <unistd.h>
#endif


namespace xtfs_mount
{
  class Main : public xtreemfs::Main
  {
  public:
    Main()
      : xtreemfs::Main( "xtfs_mount", "mount an XtreemFS volume", "[oncrpc[s]://]<dir host>[:dir port]/<volume name> <mount point>" )
    {
      direct_io = false;

      addOption( XTFS_MOUNT_OPTION_FOREGROUND, "-f", "--foreground" );
      foreground = false;

      addOption( XTFS_MOUNT_OPTION_FUSE_OPTION, "-o", NULL, "<fuse_option>" );

      addOption( XTFS_MOUNT_OPTION_METADATA_CACHE, "--metadata-cache" );
      metadata_cache = false;

      addOption( XTFS_MOUNT_OPTION_TRACE_DATA_CACHE, "--trace-data-cache" );
      trace_data_cache = false;

      addOption( XTFS_MOUNT_OPTION_TRACE_FILE_IO, "--trace-file-io" );
      trace_file_io = false;

      addOption( XTFS_MOUNT_OPTION_TRACE_METADATA_CACHE, "--trace-metadata-cache" );
      trace_metadata_cache = false;

      addOption( XTFS_MOUNT_OPTION_TRACE_VOLUME_OPERATIONS, "--trace-volume-operations" );
      trace_volume_operations = false;

      addOption( XTFS_MOUNT_OPTION_VIVALDI_COORDINATES_FILE_PATH, "--vivaldi-coordinates-file-path", NULL, "path to Vivaldi coordinates file produced by xtfs_vivaldi" );

      addOption( XTFS_MOUNT_OPTION_WRITE_BACK_CACHE, "--write-back-cache" );
      write_back_cache = false;

      addOption( XTFS_MOUNT_OPTION_WRITE_THROUGH_CACHE, "--write-through-cache" );
      write_through_cache = false;
    }

  private:
    enum
    {
      XTFS_MOUNT_OPTION_DIRECT_IO = 20,
      XTFS_MOUNT_OPTION_FOREGROUND = 21,
      XTFS_MOUNT_OPTION_FUSE_OPTION = 22,
      XTFS_MOUNT_OPTION_METADATA_CACHE = 23,
      XTFS_MOUNT_OPTION_TRACE_DATA_CACHE = 24,
      XTFS_MOUNT_OPTION_TRACE_FILE_IO = 25,
      XTFS_MOUNT_OPTION_TRACE_METADATA_CACHE = 26,
      XTFS_MOUNT_OPTION_TRACE_VOLUME_OPERATIONS = 27,
      XTFS_MOUNT_OPTION_VIVALDI_COORDINATES_FILE_PATH = 28,
      XTFS_MOUNT_OPTION_WRITE_BACK_CACHE = 29,
      XTFS_MOUNT_OPTION_WRITE_THROUGH_CACHE = 30
    };

    bool direct_io;
    YIELD::ipc::auto_URI dir_uri;
    bool foreground;
    std::string fuse_o_args;
    bool metadata_cache;
    std::string mount_point, volume_name;
    bool trace_data_cache, trace_file_io, trace_metadata_cache, trace_volume_operations;
    YIELD::platform::Path vivaldi_coordinates_file_path;
    bool write_back_cache, write_through_cache;


    // YIELD::Main
    int _main( int argc, char** argv )
    {
      // Make sure the log level is set high enough for any --trace options to show up
      if ( get_log_level() >= YIELD::platform::Log::LOG_INFO )
        trace_volume_operations = true;              
      if ( get_log_level() >= YIELD::platform::Log::LOG_DEBUG )
      {
        trace_data_cache = true;
        trace_file_io = true;
        trace_metadata_cache = true;
      }

      if ( get_log_level() < YIELD::platform::Log::LOG_INFO &&
           ( trace_data_cache || 
             trace_file_io || 
             trace_metadata_cache || 
             get_proxy_flags() != 0 || 
             trace_volume_operations ) )
        get_log()->set_level( YIELD::platform::Log::LOG_INFO );

      // Fill volume_flags from options
      uint32_t volume_flags = 0;
      if ( metadata_cache )
        volume_flags |= xtreemfs::Volume::VOLUME_FLAG_CACHE_METADATA;
      if ( trace_file_io )
        volume_flags |= xtreemfs::Volume::VOLUME_FLAG_TRACE_FILE_IO;

      // Create the XtreemFS volume in the parent as well as the child process so that the parent will fail on most common errors (like failed connections) before the child is created
      YIELD::platform::auto_Volume volume = xtreemfs::Volume::create( *dir_uri, volume_name, volume_flags, get_log(), get_proxy_flags(), get_operation_timeout(), get_proxy_ssl_context(), vivaldi_coordinates_file_path ).release();

      if ( foreground )
      {
        if ( direct_io )
          get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << get_program_name() << ": enabling FUSE direct I/O.";

        // Stack volumes
        if ( metadata_cache )
        {
          volume = new yieldfs::MetadataCachingVolume( volume, trace_metadata_cache ? get_log() : NULL, 5 );
          get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << get_program_name() << ": caching metadata.";
        }

        if ( write_back_cache )
        {
          volume = new yieldfs::WriteBackCachingVolume( 256 * 1024 * 1024, 5000, volume, trace_data_cache ? get_log() : NULL );
          get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << get_program_name() << ": caching file reads.";
        }
        else if ( write_through_cache )
        {
          volume = new yieldfs::WriteThroughCachingVolume( volume, trace_data_cache ? get_log() : NULL );
          get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << get_program_name() << ": caching file writes.";
        }

        if ( trace_volume_operations && get_log_level() >= YIELD::platform::Log::LOG_INFO )
        {
          volume = new yieldfs::TracingVolume( volume, get_log() );
          get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << get_program_name() << ": tracing volume operations.";
        }

        uint32_t fuse_flags = 0;
        if ( direct_io )
          fuse_flags |= yieldfs::FUSE::FUSE_FLAG_DIRECT_IO;
        if ( trace_volume_operations && get_log_level() >= YIELD::platform::Log::LOG_INFO )
          fuse_flags |= yieldfs::FUSE::FUSE_FLAG_DEBUG;

        std::auto_ptr<yieldfs::FUSE> fuse( new yieldfs::FUSE( volume, fuse_flags ) );

#ifdef _WIN32
        return fuse->main( mount_point.c_str() );
#else
        std::vector<char*> fuse_argvv;
        fuse_argvv.push_back( argv[0] );
        if ( ( fuse_flags & yieldfs::FUSE::FUSE_FLAG_DEBUG ) == yieldfs::FUSE::FUSE_FLAG_DEBUG )
          fuse_argvv.push_back( "-d" );
        fuse_argvv.push_back( "-o" );
        if ( !fuse_o_args.empty() )
          fuse_o_args.append( "," );
        fuse_o_args.append( "use_ino" );            
        fuse_argvv.push_back( const_cast<char*>( fuse_o_args.c_str() ) );
        get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << get_program_name() << ": passing -o " << fuse_o_args << " to FUSE.";
        fuse_argvv.push_back( NULL );
        struct fuse_args fuse_args_ = FUSE_ARGS_INIT( fuse_argvv.size() - 1 , &fuse_argvv[0] );

        return fuse->main( fuse_args_, mount_point.c_str() );
#endif
      }
      else // !foreground
      {
        std::vector<char*> child_argvv;
        for ( int arg_i = 1; arg_i < argc; arg_i++ )
          child_argvv.push_back( argv[arg_i] );
        child_argvv.push_back( "-f" );
        child_argvv.push_back( "--log-file-path" );
        if ( !log_file_path.empty() )
          child_argvv.push_back( const_cast<char*>( log_file_path.c_str() ) );
        else          
          child_argvv.push_back( "xtfs_mount.log" );
        child_argvv.push_back( NULL );

        YIELD::ipc::auto_Process child_process = YIELD::ipc::Process::create( argv[0], ( const char** )&child_argvv[0] );

        if ( child_process != NULL )
        { 
          YIELD::platform::Thread::sleep( 100 * NS_IN_MS ); // Wait for the child process to start
          int child_ret = 0;
          child_process->poll( &child_ret ); // Will set child_ret if the child failed and exited, otherwise child_ret will stay 0
          return child_ret;
        }
        else 
        {
          get_log()->getStream( YIELD::platform::Log::LOG_ERR ) << get_program_name() << ": error creating child process: " << YIELD::platform::Exception::strerror() << ".";
          return 1;
        }
      }
    }

    void parseOption( int id, char* arg )
    {
      switch ( id )
      {
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

        case XTFS_MOUNT_OPTION_METADATA_CACHE: metadata_cache = true; break;
        case XTFS_MOUNT_OPTION_TRACE_DATA_CACHE: trace_data_cache = true; break;
        case XTFS_MOUNT_OPTION_TRACE_FILE_IO: trace_file_io = true; break;
        case XTFS_MOUNT_OPTION_TRACE_METADATA_CACHE: trace_metadata_cache = true; break;
        case XTFS_MOUNT_OPTION_TRACE_VOLUME_OPERATIONS: trace_volume_operations = true; break;
        case XTFS_MOUNT_OPTION_VIVALDI_COORDINATES_FILE_PATH: vivaldi_coordinates_file_path = arg; break;
        case XTFS_MOUNT_OPTION_WRITE_BACK_CACHE: write_back_cache = true; break;
        case XTFS_MOUNT_OPTION_WRITE_THROUGH_CACHE: write_through_cache = true; break;
        default: xtreemfs::Main::parseOption( id, arg ); break;
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

      throw YIELD::platform::Exception( "must specify dir_host/volume name and mount point" );
    }
  };
};


int main( int argc, char** argv )
{
  return xtfs_mount::Main().main( argc, argv );
}
