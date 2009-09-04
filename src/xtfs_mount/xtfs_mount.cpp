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
      addOption( XTFS_MOUNT_OPTION_CACHE_FILE_READS, "--cache-file-reads" );
      cache_file_reads = false;

      addOption( XTFS_MOUNT_OPTION_CACHE_FILE_WRITES, "--cache-file-writes" );
      cache_file_writes = false;

      addOption( XTFS_MOUNT_OPTION_CACHE_METADATA, "--cache-metadata" );
      cache_metadata = false;

      direct_io = false;

      addOption( XTFS_MOUNT_OPTION_FOREGROUND, "-f", "--foreground" );
      foreground = false;

      addOption( XTFS_MOUNT_OPTION_FUSE_OPTION, "-o", NULL, "<fuse_option>" );

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
      XTFS_MOUNT_OPTION_CACHE_FILE_READS = 20,
      XTFS_MOUNT_OPTION_CACHE_FILE_WRITES = 21,
      XTFS_MOUNT_OPTION_CACHE_METADATA = 22,
      XTFS_MOUNT_OPTION_DIRECT_IO = 23,
      XTFS_MOUNT_OPTION_FOREGROUND = 24,
      XTFS_MOUNT_OPTION_FUSE_OPTION = 25,
      XTFS_MOUNT_OPTION_TRACE_DATA_CACHE = 26,
      XTFS_MOUNT_OPTION_TRACE_FILE_IO = 27,
      XTFS_MOUNT_OPTION_TRACE_METADATA_CACHE = 28,
      XTFS_MOUNT_OPTION_TRACE_VOLUME_OPERATIONS = 29
    };

    bool cache_file_reads, cache_file_writes, cache_metadata;
    bool direct_io;
    YIELD::auto_URI dir_uri;
    bool foreground;
    std::string fuse_o_args;
    std::string mount_point, volume_name;
    bool trace_data_cache, trace_file_io, trace_metadata_cache, trace_volume_operations;


    // YIELD::Main
    int _main( int argc, char** argv )
    {
      // Make sure the log level is set high enough for any --trace options to show up
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

      // Fill volume_flags from options
      uint32_t volume_flags = 0;
      if ( cache_metadata )
        volume_flags |= xtreemfs::Volume::VOLUME_FLAG_CACHE_METADATA;
      if ( trace_file_io )
        volume_flags |= xtreemfs::Volume::VOLUME_FLAG_TRACE_FILE_IO;

      // Create the XtreemFS volume in the parent as well as the child process so that the parent will fail on most common errors (like failed connections) before the child is created
      YIELD::auto_Volume volume = xtreemfs::Volume::create( *dir_uri, volume_name, volume_flags, get_log(), get_proxy_flags(), get_operation_timeout(), get_proxy_ssl_context() ).release();

      if ( foreground )
      {
        // Stack volumes
        if ( cache_file_reads )
        {
          volume = new yieldfs::ReadCachingVolume( volume, trace_data_cache ? get_log() : NULL );
          get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": caching file reads.";
        }

        if ( cache_file_writes )
        {
          volume = new yieldfs::WritebackCachingVolume( volume, trace_data_cache ? get_log() : NULL );
          get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": caching file writes.";
        }

        if ( cache_metadata )
        {
          volume = new yieldfs::MetadataCachingVolume( volume, trace_metadata_cache ? get_log() : NULL, 5 );
          get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": caching metadata.";
        }

        if ( direct_io )
          get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": enabling FUSE direct I/O.";

        if ( trace_volume_operations && get_log_level() >= YIELD::Log::LOG_INFO )
        {
          volume = new yieldfs::TracingVolume( volume, get_log() );
          get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": tracing volume operations.";
        }

        uint32_t fuse_flags = 0;
        if ( direct_io )
          fuse_flags |= yieldfs::FUSE::FUSE_FLAG_DIRECT_IO;
        if ( trace_volume_operations && get_log_level() >= YIELD::Log::LOG_INFO )
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
        get_log()->getStream( YIELD::Log::LOG_INFO ) << get_program_name() << ": passing -o " << fuse_o_args << " to FUSE.";
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
        child_argvv.push_back( NULL );

        yidl::auto_Object<YIELD::Process> child_process = YIELD::Process::create( argv[0], ( const char** )&child_argvv[0] );

        if ( child_process != NULL )
        { 
          YIELD::Thread::sleep( 100 * NS_IN_MS ); // Wait for the child process to start
          int child_ret = 0;
          child_process->poll( &child_ret ); // Will set child_ret if the child failed and exited, otherwise child_ret will stay 0
          return child_ret;
        }
        else 
        {
          get_log()->getStream( YIELD::Log::LOG_ERR ) << get_program_name() << ": error creating child process: " << YIELD::Exception::strerror() << ".";
          return 1;
        }
      }
    }

    void parseOption( int id, char* arg )
    {
      switch ( id )
      {
        case XTFS_MOUNT_OPTION_CACHE_FILE_READS: cache_file_reads = true; break;
        case XTFS_MOUNT_OPTION_CACHE_FILE_WRITES: cache_file_writes = true; break;
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

        case XTFS_MOUNT_OPTION_TRACE_DATA_CACHE: trace_data_cache = true; break;
        case XTFS_MOUNT_OPTION_TRACE_FILE_IO: trace_file_io = true; break;
        case XTFS_MOUNT_OPTION_TRACE_METADATA_CACHE: trace_metadata_cache = true; break;
        case XTFS_MOUNT_OPTION_TRACE_VOLUME_OPERATIONS: trace_volume_operations = true; break;
        
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

      throw YIELD::Exception( "must specify dir_host/volume name and mount point" );
    }
  };
};


int main( int argc, char** argv )
{
  return xtfs_mount::Main().main( argc, argv );
}
