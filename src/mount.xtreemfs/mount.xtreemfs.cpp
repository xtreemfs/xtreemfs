// Copyright (c) 2010 NEC HPC Europe
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
// DISCLAIMED. IN NO EVENT SHALL NEC HPC Europe BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#include <memory>
using std::auto_ptr;

#include "xtreemfs.h"
using namespace xtreemfs;

#include "yield.h"
using yield::platform::Process;
using yield::platform::Thread;

#include "yield/platform/directory_test.h"
using yield::platform::DirectoryTestSuite;

#include "yield/platform/file_test.h"
using yield::platform::FileTestSuite;

#include "yield/platform/volume_test.h"
using yield::platform::VolumeTestSuite;

#include "yieldfs.h"
using yieldfs::FUSE;

#include "yunit.h"
using yunit::TestRunner;

#ifndef _WIN32
#define FUSE_USE_VERSION 26
#include <fuse.h>
#include <unistd.h>
#endif


int main( int argc, char** argv )
{
  OptionParser::Options mount_options;
  mount_options.add( "-f", "run in the foreground", false );
  mount_options.add( "--foreground", "run in the foreground", false );
  mount_options.add( "-o", "<FUSE option>" );
#if FUSE_MAJOR_VERSION > 2 || ( FUSE_MAJOR_VERSION == 2 && FUSE_MINOR_VERSION >= 8 )
  mount_options.add( "--no-big-writes", false );
#endif
  mount_options.add( "--test", false );
  mount_options.add
  (
    "--vivaldi-coordinates-file-path",
    "path to Vivaldi coordinates file produced by xtfs_vivaldi"
  );
  mount_options.add( "--write-back-data-cache", false );
  mount_options.add( "--write-back-stat-cache", false );
  mount_options.add( "--write-through-data-cache", false );
  mount_options.add( "--write-through-file-size-cache", false );
  mount_options.add( "--write-through-stat-cache", false );

  if ( argc == 1 )
  {
    cout << "mount.xtreemfs: mount an XtreemFS volume" << endl;
    cout << "Usage: mount.xtreemfs <options>" <<
            " [oncrpc://]<dir host>[:port]/<volume name> <mount point>" <<
            endl;
    cout << Options::usage( mount_options );
    return 0;
  }

  try
  {
    bool foreground = false;
    uint32_t fuse_flags = FUSE::FLAGS_DEFAULT;
    string fuse_o_args;
    string mount_point;
#if FUSE_MAJOR_VERSION > 2 || ( FUSE_MAJOR_VERSION == 2 && FUSE_MINOR_VERSION >= 8 )
    bool no_big_writes = false;
#endif
    bool test = false;
    Path vivaldi_coordinates_file_path;
    uint32_t volume_flags = Volume::FLAGS_DEFAULT;

    Options options = Options::parse( argc, argv, mount_options );

    for 
    (
      Options::const_iterator parsed_option_i = options.begin();
      parsed_option_i != options.end();
      ++parsed_option_i
    )
    {
      const OptionParser::ParsedOption& popt = *parsed_option_i;
      
      if ( popt == "-f" || popt == "--foreground" )
        foreground = true;
      else if ( popt == "-o" )
      {
        if ( !fuse_o_args.empty() )
          fuse_o_args.append( "," );
        fuse_o_args.append( popt.get_argument() );

        if ( popt.get_argument().find_first_of( "direct_io" ) != string::npos )
          fuse_flags |= FUSE::FLAG_DIRECT_IO;  
      }
      else if ( popt == "--test" )
      {
        foreground = true;
        test = true;
      }
      else if ( popt == "--vivaldi-coordinates-file-path" )
        vivaldi_coordinates_file_path = popt.get_argument();
      else if ( popt == "--write-back-data-cache" )
        volume_flags |= Volume::FLAG_WRITE_BACK_DATA_CACHE;
      else if ( popt == "--write-back-stat-cache" )
        volume_flags |= Volume::FLAG_WRITE_BACK_STAT_CACHE;
      else if ( popt == "--write-through-data-cache" )
        volume_flags |= Volume::FLAG_WRITE_THROUGH_DATA_CACHE;
      else if( popt == "--write-through-file-size-cache" )
      {
        if
        (
          ( volume_flags & Volume::FLAG_WRITE_BACK_FILE_SIZE_CACHE )
            == Volume::FLAG_WRITE_BACK_FILE_SIZE_CACHE
        )
          volume_flags ^= Volume::FLAG_WRITE_BACK_FILE_SIZE_CACHE;

        volume_flags |= Volume::FLAG_WRITE_THROUGH_FILE_SIZE_CACHE;
      }
      else if ( popt == "--write-through-stat-cache" )
        volume_flags |= Volume::FLAG_WRITE_THROUGH_STAT_CACHE;
    }

    if ( options.get_trace_log() != NULL )
      fuse_flags |= FUSE::FLAG_DEBUG;

    if ( !test )
    {
      if ( !options.get_positional_arguments().empty() )
        mount_point = options.get_positional_arguments()[0];
      else
        throw Exception( 1, "must specify mount point" );
    }

    // Create the XtreemFS volume in the parent as well as the child process
    // so that the parent will fail on most common errors
    // (like failed connections) before the child is created
    yield::platform::Volume* volume 
      = &Volume::create
        ( 
          options, 
          volume_flags, 
          vivaldi_coordinates_file_path 
        );

    if ( foreground )
    {
      if ( options.get_trace_log() != NULL )
      {
        volume 
          = new yieldfs::TracingVolume
                ( 
                  *options.get_trace_log(), 
                  *volume 
                );
      }

      if ( test )
      {
        int failed_test_case_count = 0;
        TestRunner test_runner;

        DirectoryTestSuite
          directory_test_suite( "DirectoryTestSuite", &volume->inc_ref() );
        failed_test_case_count += test_runner.run( directory_test_suite );

        FileTestSuite
          file_test_suite( "FileTestSuite", &volume->inc_ref() );
        failed_test_case_count += test_runner.run( file_test_suite );

        VolumeTestSuite 
          volume_test_suite( "VolumeTestSuite", &volume->inc_ref() );
        failed_test_case_count += test_runner.run( volume_test_suite );

        return failed_test_case_count;
      }
      else
      {
        auto_ptr<FUSE> fuse( &FUSE::create( *volume, fuse_flags ) );

#ifdef _WIN32
        return fuse->main( mount_point.c_str() );
#else
        std::vector<char*> fuse_argvv;
        fuse_argvv.push_back( argv[0] );
        if ( ( fuse_flags & FUSE::FLAG_DEBUG ) == FUSE::FLAG_DEBUG )
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
        fuse_argvv.push_back( NULL );
        struct fuse_args fuse_args_
          = FUSE_ARGS_INIT( fuse_argvv.size() - 1 , &fuse_argvv[0] );

        ::close( STDIN_FILENO );
        ::close( STDOUT_FILENO );
        ::close( STDERR_FILENO );

        return fuse->main( fuse_args_, mount_point.c_str() );
#endif
      }
    }
    else // !foreground
    {
      std::vector<char*> child_argv;

      for ( int arg_i = 0; arg_i < argc; arg_i++ )
        child_argv.push_back( argv[arg_i] );

      child_argv.push_back( "-f" );

      child_argv.push_back( "--log-file-path" );
#ifdef _WIN32
      string log_file_path( options.get_error_log_file_path() );
#else
      string log_file_path
             (
               static_cast<const string&>( options.get_error_log_file_path() ) 
             );
#endif
      if ( log_file_path.empty() )
      {
        ostringstream log_file_path_oss;
        log_file_path_oss << "mount.xtreemfs-";
        log_file_path_oss << Process::getpid();
        log_file_path_oss << ".log";
        log_file_path = log_file_path_oss.str();
      }
      child_argv.push_back( const_cast<char*>( log_file_path.c_str() ) );

#ifndef _WIN32
      ::close( STDIN_FILENO );
      ::close( STDOUT_FILENO );
      ::close( STDERR_FILENO );
#endif

      Process& child_process = Process::create( child_argv );

      int child_ret = 0;
#ifndef _WIN32
      string xtreemfs_url;
#endif
      for ( uint8_t poll_i = 0; poll_i < 10; poll_i++ )
      {
        if ( child_process.poll( &child_ret ) )
          break;
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
        {
          child_ret = 0;
          break;
        }
#endif
        else
          Thread::nanosleep( 0.1 );
      }

      return child_ret;
    }
  }
  catch ( Exception& exception )
  {
    cerr << "mount.xtreemfs: error: " << exception.what() << endl;
    return exception.get_error_code();
  }
}
