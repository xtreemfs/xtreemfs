// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the YieldFS project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the YieldFS project nor the
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
    const static uint32_t FUSE_FLAGS_DEFAULT = 0;

    FUSE
    (
      YIELD::platform::auto_Volume volume,
      uint32_t flags = FUSE_FLAGS_DEFAULT
    );
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


  static inline std::ostream& 
  operator<<
  ( 
    std::ostream& os, 
    const YIELD::platform::Stat& stbuf 
  )
  {
    os << "{ ";
#ifndef _WIN32
    os << "st_dev: " << stbuf.get_dev() << ", ";
    os << "st_ino: " << stbuf.get_ino() << ", ";
#endif
    os << "st_mode: " << stbuf.get_mode() << " (";
    if ( ( stbuf.get_mode() & S_IFDIR ) == S_IFDIR ) os << "S_IFDIR|";
    if ( ( stbuf.get_mode() & S_IFCHR ) == S_IFCHR ) os << "S_IFCHR|";
    if ( ( stbuf.get_mode() & S_IFREG ) == S_IFREG ) os << "S_IFREG|";
#ifdef _WIN32
    if ( ( stbuf.get_mode() & S_IREAD ) == S_IREAD ) os << "S_IREAD|";
    if ( ( stbuf.get_mode() & S_IWRITE ) == S_IWRITE ) os << "S_IWRITE|";
    if ( ( stbuf.get_mode() & S_IEXEC ) == S_IEXEC ) os << "S_IEXEC|";
#else
    if ( ( stbuf.get_mode() & S_IFBLK ) == S_IFBLK ) os << "S_IFBLK|";
    if ( ( stbuf.get_mode() & S_IFLNK ) == S_IFLNK ) os << "S_IFLNK|";
    if ( ( stbuf.get_mode() & S_IRUSR ) == S_IFDIR ) os << "S_IRUSR|";
    if ( ( stbuf.get_mode() & S_IWUSR ) == S_IWUSR ) os << "S_IWUSR|";
    if ( ( stbuf.get_mode() & S_IXUSR ) == S_IXUSR ) os << "S_IXUSR|";
    if ( ( stbuf.get_mode() & S_IRGRP ) == S_IRGRP ) os << "S_IRGRP|";
    if ( ( stbuf.get_mode() & S_IWGRP ) == S_IWGRP ) os << "S_IWGRP|";
    if ( ( stbuf.get_mode() & S_IXGRP ) == S_IXGRP ) os << "S_IXGRP|";
    if ( ( stbuf.get_mode() & S_IROTH ) == S_IROTH ) os << "S_IROTH|";
    if ( ( stbuf.get_mode() & S_IWOTH ) == S_IWOTH ) os << "S_IWOTH|";
    if ( ( stbuf.get_mode() & S_IXOTH ) == S_IXOTH ) os << "S_IXOTH|";
    if ( ( stbuf.get_mode() & S_ISUID ) == S_ISUID ) os << "S_ISUID|";
    if ( ( stbuf.get_mode() & S_ISGID ) == S_ISGID ) os << "S_ISGID|";
    if ( ( stbuf.get_mode() & S_ISVTX ) == S_ISVTX ) os << "S_ISVTX|";
#endif
    os << "0), ";
#ifndef _WIN32
    os << "st_nlink: " << stbuf.get_nlink() << ", ";
    os << "st_uid: " << stbuf.get_uid() << ", ";
    os << "st_gid: " << stbuf.get_gid() << ", ";
    os << "st_rdev: " << stbuf.get_rdev() << ", ";
#endif
    os << "st_size: " << stbuf.get_size() << ", ";
    os << "st_atime: " << stbuf.get_atime() << ", ";
    os << "st_mtime: " << stbuf.get_mtime() << ", ";
    os << "st_ctime: " << stbuf.get_ctime() << ", ";
#ifndef _WIN32
    os << "st_blksize: " << stbuf.get_blksize() << ", ";
    os << "st_blocks: " << stbuf.get_blocks() << ", ";
#else
    os << "attributes: " << stbuf.get_attributes() << ", ";
#endif
    os << " 0 }";
    return os;
  }



  class StackableDirectory : public YIELD::platform::Directory
  {
  public:
    // YIELD::platform::Directory
    // virtual YIELD::platform::Directory methods that 
    // delegate to underlying_directory
    YIELD_PLATFORM_DIRECTORY_PROTOTYPES;

  protected:
    StackableDirectory
    (
      YIELD::platform::auto_Directory underlying_directory
    )
      : underlying_directory( underlying_directory )
    { }

    virtual ~StackableDirectory()
    { }

  private:
    YIELD::platform::auto_Directory underlying_directory;
  };


  class StackableFile : public YIELD::platform::File
  {
  public:
    // YIELD::platform::File
    // virtual YIELD::platform::File methods that delegate to underlying_file
    YIELD_PLATFORM_FILE_PROTOTYPES;

  protected:
    StackableFile
    (
      YIELD::platform::auto_File underlying_file
    )
      : underlying_file( underlying_file )
    { }

    virtual ~StackableFile()
    { }

  private:
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

  private:
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

    // static trace methods for TracingDirectory and TracingFile to share
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

    // YIELD::platform::Volume
    YIELD_PLATFORM_VOLUME_PROTOTYPES;

  private:
    ~TracingVolume() { }

    YIELD::platform::auto_Log log;
  };
};

#endif
