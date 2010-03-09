// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the XtreemFS project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the XtreemFS project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#include "xtreemfs/main.h"
using namespace xtreemfs;

#include "yieldfs.h"

#include <vector>
#include <exception>

#ifndef _WIN32
#define FUSE_USE_VERSION 26
#include <fuse.h>
#include <unistd.h>
#endif


namespace mount_xtreemfs
{
  class Main : public xtreemfs::Main
  {
  public:
    Main()
      : xtreemfs::Main
        (
          "mount.xtreemfs",
          "mount an XtreemFS volume",
          "[oncrpc://]<dir host>[:port]/<volume name> <mount point>"
        )
    {
      addOption
      (
        MOUNT_XTREEMFS_OPTION_FOREGROUND,
        "-f",
        "--foreground"
      );
      foreground = false;

      addOption
      (
        MOUNT_XTREEMFS_OPTION_FUSE_OPTION,
        "-o",
        NULL,
        "<fuse_option>"
      );

#if FUSE_MAJOR_VERSION > 2 || ( FUSE_MAJOR_VERSION == 2 && FUSE_MINOR_VERSION >= 8 )
      addOption
      (
        MOUNT_XTREEMFS_OPTION_NO_BIG_WRITES,
        "--no-big-writes"
      );
      no_big_writes = false;
#endif

      addOption
      (
        MOUNT_XTREEMFS_OPTION_TRACE_FILE_IO,
        "--trace-file-io"
      );

      addOption
      (
        MOUNT_XTREEMFS_OPTION_TRACE_VOLUME_OPERATIONS,
        "--trace-volume-operations"
      );
      trace_volume_operations = false;

      addOption
      (
        MOUNT_XTREEMFS_OPTION_VIVALDI_COORDINATES_FILE_PATH,
        "--vivaldi-coordinates-file-path",
        NULL,
        "path to Vivaldi coordinates file produced by xtfs_vivaldi"
      );

      addOption
      (
        MOUNT_XTREEMFS_OPTION_WRITE_BACK_DATA_CACHE,
        "--write-back-data-cache"
      );

      addOption
      (
        MOUNT_XTREEMFS_OPTION_WRITE_BACK_STAT_CACHE,
        "--write-back-stat-cache"
      );

      addOption
      (
        MOUNT_XTREEMFS_OPTION_WRITE_THROUGH_DATA_CACHE,
        "--write-through-data-cache"
      );

      addOption
      (
        MOUNT_XTREEMFS_OPTION_WRITE_THROUGH_FILE_SIZE_CACHE,
        "--write-through-file-size-cache"
      );

      addOption
      (
        MOUNT_XTREEMFS_OPTION_WRITE_THROUGH_STAT_CACHE,
        "--write-through-stat-cache"
      );

      fuse_flags = yieldfs::FUSE::FLAGS_DEFAULT;
      volume_flags = Volume::VOLUME_FLAGS_DEFAULT;
    }

  private:
    enum
    {
      MOUNT_XTREEMFS_OPTION_DIRECT_IO = 20,
      MOUNT_XTREEMFS_OPTION_FOREGROUND = 21,
      MOUNT_XTREEMFS_OPTION_FUSE_OPTION = 22,
      MOUNT_XTREEMFS_OPTION_METADATA_CACHE = 23,
#if FUSE_MAJOR_VERSION > 2 || ( FUSE_MAJOR_VERSION == 2 && FUSE_MINOR_VERSION >= 8 )
      MOUNT_XTREEMFS_OPTION_NO_BIG_WRITES = 24,
#endif
      MOUNT_XTREEMFS_OPTION_TRACE_DATA_CACHE = 25,
      MOUNT_XTREEMFS_OPTION_TRACE_FILE_IO = 26,
      MOUNT_XTREEMFS_OPTION_TRACE_STAT_CACHE = 27,
      MOUNT_XTREEMFS_OPTION_TRACE_VOLUME_OPERATIONS = 28,
      MOUNT_XTREEMFS_OPTION_VIVALDI_COORDINATES_FILE_PATH = 29,
      MOUNT_XTREEMFS_OPTION_WRITE_BACK_DATA_CACHE = 30,
      MOUNT_XTREEMFS_OPTION_WRITE_BACK_STAT_CACHE = 31,
      MOUNT_XTREEMFS_OPTION_WRITE_THROUGH_DATA_CACHE = 32,
      MOUNT_XTREEMFS_OPTION_WRITE_THROUGH_FILE_SIZE_CACHE = 33,
      MOUNT_XTREEMFS_OPTION_WRITE_THROUGH_STAT_CACHE = 34
    };

    yield::ipc::auto_URI dir_uri;
    bool foreground;
    uint32_t fuse_flags;
    std::string fuse_o_args;
    std::string mount_point;
#if FUSE_MAJOR_VERSION > 2 || ( FUSE_MAJOR_VERSION == 2 && FUSE_MINOR_VERSION >= 8 )
    bool no_big_writes;
#endif
    bool trace_volume_operations;
    yield::platform::Path vivaldi_coordinates_file_path;
    std::string volume_name;
    uint32_t volume_flags;


    // yield::Main
    int _main( int argc, char** argv )
    {
      // Make sure the log level is set high enough for any
      // --trace options to show up
      if 
      ( 
        get_log_level() < Log::LOG_INFO &&
        (
          get_proxy_flags() != 0
          ||
          ( volume_flags & Volume::FLAG_TRACE_DATA_CACHE )
              == Volume::FLAG_TRACE_DATA_CACHE
          ||
          ( volume_flags & Volume::FLAG_TRACE_FILE_IO )
              == Volume::FLAG_TRACE_FILE_IO
          ||
          ( volume_flags & Volume::FLAG_TRACE_STAT_CACHE )
              == Volume::FLAG_TRACE_STAT_CACHE
        )
      )
        get_log()->set_level( Log::LOG_INFO );

      // Create the XtreemFS volume in the parent as well as the child process
      // so that the parent will fail on most common errors
      // (like failed connections) before the child is created
      yield::platform::auto_Volume volume =
        Volume::create
        (
          *dir_uri,
          volume_name,
          volume_flags,
          get_log(),
          get_proxy_flags(),
          get_operation_timeout(),
          DIRProxy::RECONNECT_TRIES_MAX_DEFAULT,
          get_proxy_ssl_context(),
          vivaldi_coordinates_file_path
        ).release();

      if ( foreground )
      {
        if ( trace_volume_operations )
          volume = new yieldfs::TracingVolume( get_log(), volume );

        std::auto_ptr<yieldfs::FUSE>
          fuse( new yieldfs::FUSE( volume, fuse_flags ) );

#ifdef _WIN32
        return fuse->main( mount_point.c_str() );
#else
        std::vector<char*> fuse_argvv;
        fuse_argvv.push_back( argv[0] );
        if ( ( fuse_flags & yieldfs::FUSE::FLAG_DEBUG ) ==
             yieldfs::FUSE::FLAG_DEBUG )
          fuse_argvv.push_back( "-d" );
        fuse_argvv.push_back( "-o" );
        if ( !fuse_o_args.empty() )
          fuse_o_args.append( "," );
        fuse_o_args.append( "use_ino,fsname=xtreemfs" );
#if FUSE_MAJOR_VERSION > 2 || ( FUSE_MAJOR_VERSION == 2 && FUSE_MINOR_VERSION >= 8 )
        if ( !no_big_writes )
          fuse_o_args.append( ",big_writes" );
#endif
        fuse_argvv.push_back( const_cast<char*>( fuse_o_args.c_str() ) );
        get_log()->get_stream( Log::LOG_INFO ) <<
            get_program_name() << ": passing -o " << fuse_o_args <<
            " to FUSE.";
        fuse_argvv.push_back( NULL );
        struct fuse_args fuse_args_ =
          FUSE_ARGS_INIT( fuse_argvv.size() - 1 , &fuse_argvv[0] );

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
        std::string log_file_path( get_log_file_path() );
        if ( log_file_path.empty() )
        {
          std::ostringstream log_file_path_oss;
          log_file_path_oss << "mount.xtreemfs-";
          log_file_path_oss << yield::ipc::Process::getpid();
          log_file_path_oss << ".log";
          log_file_path = log_file_path_oss.str();
        }
        child_argvv.push_back( const_cast<char*>( log_file_path.c_str() ) );

        child_argvv.push_back( NULL );


        yield::ipc::auto_Process child_process =
          yield::ipc::Process::create
          (
            argv[0],
            const_cast<const char**>( &child_argvv[0] )
          );

        if ( child_process != NULL )
        {
          int child_ret = 0;
#ifndef _WIN32
          std::string xtreemfs_url;
#endif
          for ( uint8_t poll_i = 0; poll_i < 10; poll_i++ )
          {
            if ( child_process->poll( &child_ret ) )
              return child_ret; // Child failed
#ifndef _WIN32
            else if
            (
              yield::platform::Volume().getxattr
              (
                mount_point,
                "xtreemfs.url",
                xtreemfs_url
              )
            )
              return 0; // Child started successfully
#endif
            else
             yield::platform::Thread::nanosleep( 0.1 );
          }

          return 0; // Assume the child started successfully
        }
        else
        {
          get_log()->get_stream( Log::LOG_ERR ) <<
            get_program_name() << ": error creating child process: " <<
            yield::platform::Exception() << ".";
          return 1;
        }
      }
    }

    void parseOption( int id, char* arg )
    {
      switch ( id )
      {
        case MOUNT_XTREEMFS_OPTION_FOREGROUND: foreground = true; break;

        case MOUNT_XTREEMFS_OPTION_FUSE_OPTION:
        {
          if ( !fuse_o_args.empty() )
            fuse_o_args.append( "," );
          fuse_o_args.append( arg );

          if ( strstr( arg, "direct_io" ) != NULL )
            fuse_flags |= yieldfs::FUSE::FLAG_DIRECT_IO;
        }
        break;

        case MOUNT_XTREEMFS_OPTION_TRACE_DATA_CACHE:
        {
          volume_flags |= Volume::FLAG_TRACE_DATA_CACHE;
        }
        break;

        case MOUNT_XTREEMFS_OPTION_TRACE_FILE_IO:
        {
          volume_flags |= Volume::FLAG_TRACE_FILE_IO;
        }
        break;

        case MOUNT_XTREEMFS_OPTION_TRACE_STAT_CACHE:
        {
          volume_flags |= Volume::FLAG_TRACE_STAT_CACHE;
        }
        break;

        case MOUNT_XTREEMFS_OPTION_TRACE_VOLUME_OPERATIONS:
        {          
          trace_volume_operations = true;
          fuse_flags |= yieldfs::FUSE::FLAG_DEBUG;
        }
        break;

        case MOUNT_XTREEMFS_OPTION_VIVALDI_COORDINATES_FILE_PATH:
        {
          vivaldi_coordinates_file_path = arg;
        }
        break;

        case MOUNT_XTREEMFS_OPTION_WRITE_BACK_DATA_CACHE:
        {
          volume_flags |= Volume::FLAG_WRITE_BACK_DATA_CACHE;
        }
        break;

        case MOUNT_XTREEMFS_OPTION_WRITE_BACK_STAT_CACHE:
        {
          volume_flags |= Volume::FLAG_WRITE_BACK_STAT_CACHE;
        }
        break;

        case MOUNT_XTREEMFS_OPTION_WRITE_THROUGH_DATA_CACHE:
        {
          volume_flags |= Volume::FLAG_WRITE_THROUGH_DATA_CACHE;
        }
        break;

        case MOUNT_XTREEMFS_OPTION_WRITE_THROUGH_FILE_SIZE_CACHE:
        {
          if 
          ( 
            ( volume_flags & Volume::FLAG_WRITE_BACK_FILE_SIZE_CACHE )
              == Volume::FLAG_WRITE_BACK_FILE_SIZE_CACHE
          )
            volume_flags ^= Volume::FLAG_WRITE_BACK_FILE_SIZE_CACHE;

          volume_flags |= Volume::FLAG_WRITE_THROUGH_FILE_SIZE_CACHE;
        }
        break;

        case MOUNT_XTREEMFS_OPTION_WRITE_THROUGH_STAT_CACHE:
        {
          volume_flags |= Volume::FLAG_WRITE_THROUGH_STAT_CACHE;
        }
        break;

        default:
        {
          xtreemfs::Main::parseOption( id, arg );
        }
        break;
      }
    }

    void parseFiles( int file_count, char** files )
    {
      if ( file_count == 2 )
      {
        dir_uri = parseVolumeURI( files[0], volume_name );
        mount_point = files[1];
      }
      else if ( file_count < 2 )
      {
        throw yield::platform::Exception
        (
          "must specify a DIR/volume URI and a mount point"
        );
      }
      else
      {
        throw yield::platform::Exception
        (
          "extra parameters after the DIR/volume URI and mount point"
        );
      }
    }
  };
};


int main( int argc, char** argv )
{
  return mount_xtreemfs::Main().main( argc, argv );
}
