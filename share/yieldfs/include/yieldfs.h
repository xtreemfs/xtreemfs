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
struct fuse_args;
#endif


namespace yieldfs
{
  using yield::platform::Directory;
  using yield::platform::File;
  using yield::platform::Log;
  using yield::platform::Path;
  using yield::platform::Stat;
  using yield::platform::Volume;


  class FUSE
  {
  public:
    virtual ~FUSE();

    const static uint32_t FLAG_DEBUG = 1;
    const static uint32_t FLAG_DIRECT_IO = 2;
    const static uint32_t FLAGS_DEFAULT = 0;

    static FUSE& create( Volume& volume, uint32_t flags = FLAGS_DEFAULT );

    uint32_t get_flags() const { return flags; }
    Volume& get_volume() const { return volume; }

    static uint32_t getpid();
#ifdef _WIN32
    virtual int main( const Path& mount_point ) = 0;
#else
    static uid_t geteuid();
    static gid_t getegid();
    virtual int main( char* argv0, const Path& mount_point ) = 0;
    virtual int main( struct fuse_args&, const Path& mount_point ) = 0;
#endif

  protected:
    FUSE( uint32_t flags, Volume& volume ); // Steals the reference to volume
  
  protected:
#ifndef _WIN32
    static bool is_running;
#endif
    uint32_t flags;
    Volume& volume;
  };


  class StackableDirectory : public Directory
  {
  public:
    // yield::platform::Directory
    // virtual yield::platform::Directory methods that 
    // delegate to underlying_directory
    YIELD_PLATFORM_DIRECTORY_PROTOTYPES;

  protected:
    StackableDirectory( Directory& underlying_directory ); // Steals the ref
    virtual ~StackableDirectory();

  private:
    Directory& underlying_directory;
  };


  class StackableFile : public File
  {
  public:
    // yield::platform::File
    // virtual yield::platform::File methods that delegate to underlying_file
    YIELD_PLATFORM_FILE_PROTOTYPES;

  protected:
    StackableFile( File& underlying_file ); // Steals the ref
    virtual ~StackableFile();

  private:
    File& underlying_file;
  };


  class StackableVolume : public Volume
  {
  public:
    // yield::platform::Volume methods that delegate to underlying_volume
    YIELD_PLATFORM_VOLUME_PROTOTYPES;

  protected:
    StackableVolume( Volume& underlying_volume ); // Steals the ref
    virtual ~StackableVolume();

  private:
    Volume& underlying_volume;
  };


  static inline ostream& operator<<( ostream& os, const Stat& stbuf )
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


  class TracingVolume : public StackableVolume
  {
  public:
    TracingVolume(); // For testing
    // Steals references to underlying_volume
    TracingVolume( Volume& underlying_volume ); // Log to cout
    TracingVolume( Log& log, Volume& underlying_volume );

    // static trace methods for TracingDirectory and TracingFile to share
    static bool
    trace
    (
      Log& log,
      const char* operation_name,
      const Path& path,
      bool operation_result
    );

    static bool
    trace
    (
      Log& log,
      const char* operation_name,
      const Path& path,
      mode_t mode,
      bool operation_result
    );

    static bool
    trace
    (
      Log& log,
      const char* operation_name,
      const Path& old_path,
      const Path& new_path,
      bool operation_result
    );

    static bool
    trace
    (
      Log& log,
      const char* operation_name,
      const Path& path,
      const string& xattr_name,
      const string& xattr_value,
      bool operation_result
    );

    static bool
    trace
    (
      Log& log,
      const char* operation_name,
      const Path& path,
      uint64_t size,
      uint64_t offset,
      bool operation_result
    );

    static bool
    trace
    (
      Log::Stream& log_stream,
      bool operation_result
    );

    // yield::platform::Volume
    YIELD_PLATFORM_VOLUME_PROTOTYPES;

  private:
    Log& log;
  };
};

#endif
