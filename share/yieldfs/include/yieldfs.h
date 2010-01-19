// Copyright 2009-2010 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).

#ifndef _YIELDFS_H_
#define _YIELDFS_H_

#include "yield/platform.h"

#ifndef _WIN32
#include <unistd.h>
#endif

#include <map>
#include <string>

#ifndef _WIN32
struct fuse_args;
#endif


namespace yieldfs
{
#ifdef _WIN32
  class FUSEWin32;
#else
  class FUSEUnix;
#endif


  class FUSE
  {
  public:
    const static uint32_t FUSE_FLAG_DEBUG = 1;
    const static uint32_t FUSE_FLAG_DIRECT_IO = 2;

    FUSE( YIELD::platform::auto_Volume volume, uint32_t flags = 0 );
    ~FUSE();

    static uint32_t getpid();
#ifdef _WIN32
    int main( const char* drive_letter );
#else
    static uid_t geteuid();
    static gid_t getegid();
    int main( char* argv0, const char* mount_point );
    int main( struct fuse_args&, const char* mount_point );
#endif

  protected:
#ifdef _WIN32
    FUSEWin32* fuse_win32;
#else
    FUSEUnix* fuse_unix;
    static bool is_running;
#endif
  };


  class StackableFile : public YIELD::platform::File
  {
  public:
    const YIELD::platform::Path& get_path() const { return path; }

    // YIELD::platform::File
    // virtual YIELD::platform::File methods that delegate to underlying_file
    YIELD_PLATFORM_FILE_PROTOTYPES;

  protected:
    StackableFile
    (
      YIELD::platform::auto_Log log,
      const YIELD::platform::Path& path, 
      YIELD::platform::auto_File underlying_file 
     )
      : log( log ), path( path ), underlying_file( underlying_file )
    { }

    virtual ~StackableFile()
    { }


    YIELD::platform::auto_Log log;
    YIELD::platform::Path path;
    YIELD::platform::auto_File underlying_file;
  };


  class StackableVolume : public YIELD::platform::Volume
  {
  public:
    // YIELD::platform::Volume methods that delegate to underlying_volume
    YIELD_PLATFORM_VOLUME_PROTOTYPES;

  protected:
    StackableVolume()
    {
      underlying_volume = new YIELD::platform::Volume;
    }

    StackableVolume( YIELD::platform::auto_Volume underlying_volume )
      : underlying_volume( underlying_volume )
    { }

    StackableVolume
    ( 
      YIELD::platform::auto_Log log, 
      YIELD::platform::auto_Volume underlying_volume 
    )
      : log( log ), underlying_volume( underlying_volume )
    { }


    YIELD::platform::auto_Log log;
    YIELD::platform::auto_Volume underlying_volume;
  };


  class TracingVolume : public StackableVolume
  {
  public:
    TracingVolume(); // For testing

    TracingVolume
    ( 
      YIELD::platform::auto_Volume underlying_volume 
    ); // Log to std::cout

    TracingVolume
    ( 
      YIELD::platform::auto_Log log,
      YIELD::platform::auto_Volume underlying_volume 
    );

    // YIELD::platform::Volume
    YIELD_PLATFORM_VOLUME_PROTOTYPES;
    bool exists( const YIELD::platform::Path& path );

    bool listdir
    ( 
      const YIELD::platform::Path& path, 
      const YIELD::platform::Path& match_file_name_prefix, 
      listdirCallback& callback 
    );

    bool mktree( const YIELD::platform::Path& path, mode_t mode );
    bool rmtree( const YIELD::platform::Path& path );

  private:
    class listdirCallback;
    class readdirCallback;
    friend class TracingFile;

    ~TracingVolume() { }

    static bool 
    trace
    ( 
      YIELD::platform::auto_Log log, 
      const char* operation_name, 
      const YIELD::platform::Path& path, 
      bool operation_result 
    );

    static bool 
    trace
    ( 
      YIELD::platform::auto_Log log, 
      const char* operation_name, 
      const YIELD::platform::Path& path, 
      mode_t mode, 
      bool operation_result 
    );

    static bool 
    trace
    ( 
      YIELD::platform::auto_Log log, 
      const char* operation_name, 
      const YIELD::platform::Path& old_path, 
      const YIELD::platform::Path& new_path, 
      bool operation_result 
    );

    static bool 
    trace
    ( 
      YIELD::platform::auto_Log log, 
      const char* operation_name, 
      const YIELD::platform::Path& path, 
      const std::string& xattr_name, 
      const std::string& xattr_value, 
      bool operation_result 
    );

    static bool
    trace
    ( 
      YIELD::platform::auto_Log log, 
      const char* operation_name, 
      const YIELD::platform::Path& path, 
      uint64_t size, 
      uint64_t offset, 
      bool operation_result 
    );

    static bool
    trace
    ( 
      YIELD::platform::Log::Stream& log_stream, 
      bool operation_result 
    );
  };
};

#endif
