#include "yieldfs.h"
using namespace yieldfs;


// tracing_directory.h
namespace yieldfs
{
  class TracingDirectory : public StackableDirectory
  {
  public:
    TracingDirectory
    (
      Log& log,
      const Path& path,
      Directory& underlying_directory
    );
    virtual ~TracingDirectory();
    // yield::platform::Directory
    YIELD_PLATFORM_DIRECTORY_PROTOTYPES;
  private:
    Log& log;
    Path path;
  };
};


// tracing_file.h
namespace yieldfs
{
  class TracingFile : public StackableFile
  {
  public:
    TracingFile( Log& log, const Path& path, File& underlying_file );
    virtual ~TracingFile();
    // yield::platform::File
    YIELD_PLATFORM_FILE_PROTOTYPES;
  private:
    Log& log;
    Path path;
  };
};


// unix_fuse.h
#define FUSE_USE_VERSION 26
#endif


namespace yieldfs
{
#ifndef _WIN32
  class UnixFUSE : public FUSE
  {
  public:
    UnixFUSE( uint32_t flags, Volume& volume );

    // FUSE;
    int main( char* argv0, const Path& mount_point );
    int main( struct fuse_args& fuse_args_, const Path& mount_point );

  private:
    static int access( const char* path, int mode );
    static int chmod( const char* path, mode_t mode );
    static int chown( const char* path, uid_t uid, gid_t gid );

    static int
    create
    (
      const char* path,
      mode_t mode,
      struct fuse_file_info* fi
    );

    static int
    fgetattr
    (
      const char* path,
      struct stat *stbuf,
      struct fuse_file_info* fi
     );

    static int
    fsync
    (
      const char* path,
      int isdatasync,
      struct fuse_file_info* fi
    );

    static int
    ftruncate
    (
      const char* path,
      off_t size,
      struct fuse_file_info* fi
    );

    static int getattr( const char* path, struct stat *stbuf );

    static inline yield::platform::File* get_file( fuse_file_info* fi )
    {
      return fi != NULL ? reinterpret_cast<File*>( fi->fh ) : NULL;
    }

    static inline uint32_t get_flags()
    {
      return static_cast<FUSE*>
             (
               fuse_get_context()->private_data
             )->get_flags();
    }

    static inline Volume& get_volume()
    {
      return static_cast<FUSE*>
             (
               fuse_get_context()->private_data
             )->get_volume();
    }

    static int
    getxattr
    (
      const char* path,
      const char* name,
      char *value,
      size_t size
    );

    static int link( const char* path, const char* linkpath );
    static int listxattr( const char* path, char *list, size_t size );

    static int
    lock
    (
      const char* path,
      struct fuse_file_info *fi,
      int cmd,
      struct flock* flock_
    );


    static int mkdir( const char* path, mode_t mode );
    static int open( const char* path, struct fuse_file_info* fi );

    static int
    read
    (
      const char* path,
      char *rbuf,
      size_t size,
      off_t offset,
      struct fuse_file_info* fi
    );

    static int
    readdir
    (
      const char* path,
      void* buf,
      fuse_fill_dir_t filler,
      off_t offset,
      struct fuse_file_info* fi
    );

    static int readlink( const char* path, char *linkbuf, size_t size );
    static int release( const char* path, struct fuse_file_info* fi );
    static int rename( const char* path, const char *topath );
    static int removexattr( const char* path, const char* name );
    static int rmdir( const char* path );

    static int
    setxattr
    (
      const char* path,
      const char* name,
      const char *value,
      size_t size,
      int flags
    );

    static int statfs( const char* path, struct statvfs* sbuf );
    static int symlink( const char* path, const char* linkpath );
    static int truncate( const char* path, off_t size );
    static int unlink( const char* path );
    static int utimens( const char* path, const timespec tv[2] );

    static int
    write
    (
      const char* path,
      const char *wbuf,
      size_t size,
      off_t offset,
      struct fuse_file_info* fi
    );
  };
#endif
};


// win32_fuse.h
#ifdef _WIN32
#include <windows.h>
#include <dokan.h>
#pragma comment( lib, "dokan.lib" )
#endif
namespace yieldfs
{
#ifdef _WIN32
  class Win32FUSE : public FUSE
  {
  public:
    Win32FUSE( uint32_t flags, Volume& volume );
    // FUSE
    int main( const Path& mount_point );
  private:
    static int DOKAN_CALLBACK
    CreateFile
    (
      LPCWSTR FileName,
      DWORD DesiredAccess,
      DWORD, // ShareMode,
      DWORD CreationDisposition,
      DWORD FlagsAndAttributes,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    CreateDirectory
    (
      LPCWSTR FileName,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    CloseFile
    (
      LPCWSTR, // FileName,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    Cleanup
    (
      LPCWSTR FileName,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    DeleteDirectory
    (
      LPCWSTR FileName,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    DeleteFile
    (
      LPCWSTR, // FileName,
      PDOKAN_FILE_INFO
    );
    static int DOKAN_CALLBACK
    FindFiles
    (
      LPCWSTR FileName,
      PFillFindData FillFindData,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    FlushFileBuffers
    (
      LPCWSTR, // FileName,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    GetDiskFreeSpace
    (
      PULONGLONG FreeBytesAvailable,
      PULONGLONG TotalNumberOfBytes,
      PULONGLONG TotalNumberOfFreeBytes,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static inline File* get_file( PDOKAN_FILE_INFO DokanFileInfo )
    {
      return reinterpret_cast<File*>( DokanFileInfo->Context );
    }
    static int DOKAN_CALLBACK
    GetFileInformation
    (
      LPCWSTR FileName,
      LPBY_HANDLE_FILE_INFORMATION HandleFileInformation,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static inline Volume& get_volume( PDOKAN_FILE_INFO DokanFileInfo )
    {
      return reinterpret_cast<FUSE*>
      (
        DokanFileInfo->DokanOptions->GlobalContext
      )->get_volume();
    }
    static int DOKAN_CALLBACK
    GetVolumeInformation
    (
      LPWSTR VolumeNameBuffer,
      DWORD, // VolumeNameSize,
      LPDWORD VolumeSerialNumber,
      LPDWORD MaximumComponentLength,
      LPDWORD FileSystemFlags,
      LPWSTR FileSystemNameBuffer,
      DWORD, // FileSystemNameSize,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    LockFile
    (
      LPCWSTR, // FileName,
      LONGLONG ByteOffset,
      LONGLONG Length,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    MoveFile
    (
      LPCWSTR FileName,
      LPCWSTR NewFileName,
      BOOL ReplaceIfExisting,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    OpenDirectory
    (
      LPCWSTR FileName,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    ReadFile
    (
      LPCWSTR FileName,
      LPVOID Buffer,
      DWORD BufferLength,
      LPDWORD ReadLength,
      LONGLONG Offset,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    SetEndOfFile
    (
      LPCWSTR FileName,
      LONGLONG ByteOffset,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    SetFileAttributes
    (
      LPCWSTR FileName,
      DWORD FileAttributes,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    SetFileTime
    (
      LPCWSTR FileName,
      CONST FILETIME* CreationTime,
      CONST FILETIME*  LastAccessTime,
      CONST FILETIME*  LastWriteTime,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    UnlockFile
    (
      LPCWSTR, // FileName,
      LONGLONG ByteOffset,
      LONGLONG Length,
      PDOKAN_FILE_INFO DokanFileInfo
    );
    static int DOKAN_CALLBACK
    Unmount( PDOKAN_FILE_INFO );
    static int DOKAN_CALLBACK
    WriteFile
    (
      LPCWSTR FileName,
      LPCVOID Buffer,
      DWORD NumberOfBytesToWrite,
      LPDWORD NumberOfBytesWritten,
      LONGLONG Offset,
      PDOKAN_FILE_INFO DokanFileInfo
    );
  };
#endif
};


// fuse.cpp
#ifdef _WIN32
#else
#endif


#ifndef _WIN32
bool FUSE::is_running = false;
#endif


FUSE::FUSE( uint32_t flags, Volume& volume )
  : flags( flags ), volume( volume )
{ }

FUSE::~FUSE()
{
  Volume::dec_ref( volume );
}

FUSE& FUSE::create( Volume& volume, uint32_t flags )
{
#ifdef _WIN32
  return *new Win32FUSE( flags, volume );
#else
  return *new UnixFUSE( flags, volume );
#endif
}

#ifndef _WIN32
uid_t FUSE::geteuid()
{
  if ( is_running )
  {
    struct fuse_context* ctx = fuse_get_context();
    if ( ctx && ctx->pid != 0 && ctx->private_data != NULL )
      return ctx->uid;
    else
      return static_cast<uid_t>( -1 );
  }
  else
    return static_cast<uid_t>( -1 );
}

gid_t FUSE::getegid()
{
  if ( is_running )
  {
    struct fuse_context* ctx = fuse_get_context();
    if ( ctx && ctx->pid != 0 && ctx->private_data != NULL )
      return ctx->gid;
    else
      return static_cast<gid_t>( -1 );
  }
  else
    return static_cast<gid_t>( -1 );
}
#endif

uint32_t FUSE::getpid()
{
#ifdef _WIN32
  return ::GetCurrentProcessId();
#else
  if ( is_running )
  {
    struct fuse_context* ctx = fuse_get_context();
    if ( ctx && ctx->pid != 0 && ctx->private_data != NULL )
      return static_cast<uint32_t>( ctx->pid );
    else
      return static_cast<uint32_t>( ::getpid() );
  }
  else
    return static_cast<uint32_t>( ::getpid() );
#endif
}


// stackable_directory.cpp
StackableDirectory::StackableDirectory( Directory& underlying_directory )
  : underlying_directory( underlying_directory.inc_ref() )
{ }

StackableDirectory::~StackableDirectory()
{
  Directory::dec_ref( underlying_directory );
}

Directory::Entry* StackableDirectory::readdir()
{
  return underlying_directory.readdir();
}


// stackable_file.cpp
StackableFile::StackableFile( File& underlying_file )
  : underlying_file( underlying_file.inc_ref() )
{ }

StackableFile::~StackableFile()
{
  File::dec_ref( underlying_file );
}

bool StackableFile::close()
{
  return underlying_file.close();
}

bool StackableFile::datasync()
{
  return underlying_file.datasync();
}

Stat* StackableFile::getattr()
{
  return underlying_file.getattr();
}

bool StackableFile::getlk( bool exclusive, uint64_t offset, uint64_t length )
{
  return underlying_file.getlk( exclusive, offset, length );
}

bool StackableFile::getxattr( const string& name, string& out_value )
{
  return underlying_file.getxattr( name, out_value );
}

bool StackableFile::listxattr( vector<string>& out_names )
{
  return underlying_file.listxattr( out_names );
}

ssize_t StackableFile::read( void* buf, size_t buflen, uint64_t offset )
{
  return underlying_file.read( buf, buflen, offset );
}

bool StackableFile::removexattr( const string& name )
{
  return underlying_file.removexattr( name );
}

bool StackableFile::setlk( bool exclusive, uint64_t offset, uint64_t length )
{
  return underlying_file.setlk( exclusive, offset, length );
}

bool StackableFile::setlkw( bool exclusive, uint64_t offset, uint64_t length )
{
  return underlying_file.setlkw( exclusive, offset, length );
}

bool
StackableFile::setxattr
(
  const string& name,
  const string& value,
  int flags
)
{
  return underlying_file.setxattr( name, value, flags );
}

bool StackableFile::sync()
{
  return underlying_file.sync();
}

bool StackableFile::truncate( uint64_t offset )
{
  return underlying_file.truncate( offset );
}

bool StackableFile::unlk( uint64_t offset, uint64_t length )
{
  return underlying_file.unlk( offset, length );
}

ssize_t StackableFile::write( const void* buf, size_t buflen, uint64_t offset )
{
  return underlying_file.write( buf, buflen, offset );
}


// stackable_volume.cpp
StackableVolume::StackableVolume( Volume& underlying_volume )
  : underlying_volume( underlying_volume.inc_ref() )
{ }

StackableVolume::~StackableVolume()
{
  Volume::dec_ref( underlying_volume );
}

bool StackableVolume::access( const Path& path, int amode )
{
  return underlying_volume.access( path, amode );
}

Stat* StackableVolume::getattr( const Path& path )
{
  return underlying_volume.getattr( path );
}

bool
StackableVolume::getxattr
(
  const Path& path,
  const string& name,
  string& out_value
)
{
  return underlying_volume.getxattr( path, name, out_value );
}

bool StackableVolume::link( const Path& old_path, const Path& new_path )
{
  return underlying_volume.link( old_path, new_path );
}

bool StackableVolume::listxattr
(
  const Path& path,
  vector<string>& out_names
)
{
  return underlying_volume.listxattr( path, out_names );
}

bool StackableVolume::mkdir( const Path& path, mode_t mode )
{
  return underlying_volume.mkdir( path, mode );
}

File*
StackableVolume::open
(
  const Path& path,
  uint32_t flags,
  mode_t mode,
  uint32_t attributes
)
{
  return underlying_volume.open( path, flags, mode, attributes );
}

Directory* StackableVolume::opendir( const Path& path )
{
  return underlying_volume.opendir( path );
}

Path* StackableVolume::readlink( const Path& path )
{
  return underlying_volume.readlink( path );
}

bool StackableVolume::removexattr( const Path& path, const string& name )
{
  return underlying_volume.removexattr( path, name );
}

bool StackableVolume::rename( const Path& from_path, const Path& to_path )
{
  return underlying_volume.rename( from_path, to_path );
}

bool StackableVolume::rmdir( const Path& path )
{
  return underlying_volume.rmdir( path );
}

bool
StackableVolume::setattr
(
  const Path& path,
  const Stat& stbuf,
  uint32_t to_set
)
{
  return underlying_volume.setattr( path, stbuf, to_set );
}

bool
StackableVolume::setxattr
(
  const Path& path,
  const string& name,
  const string& value,
  int flags
)
{
  return underlying_volume.setxattr( path, name, value, flags );
}

bool StackableVolume::statvfs( const Path& path, struct statvfs& stvfsbuf )
{
  return underlying_volume.statvfs( path, stvfsbuf );
}

bool StackableVolume::symlink( const Path& old_path, const Path& new_path )
{
  return underlying_volume.symlink( old_path, new_path );
}

bool StackableVolume::truncate( const Path& path, uint64_t new_size )
{
  return underlying_volume.truncate( path, new_size );
}

bool StackableVolume::unlink( const Path& path )
{
  return underlying_volume.unlink( path );
}

Path StackableVolume::volname( const Path& path )
{
  return underlying_volume.volname( path );
}


// tracing_directory.cpp
#include <sstream>


TracingDirectory::TracingDirectory
(
  Log& log,
  const Path& path,
  Directory& underlying_directory
)
  : StackableDirectory( underlying_directory ),
    log( log.inc_ref() ),
    path( path )
{ }

TracingDirectory::~TracingDirectory()
{
  log.get_stream( Log::LOG_INFO ) <<
    "yieldfs::TracingDirectory::closedir( " << path << " )";
  Log::dec_ref( log );
}

Directory::Entry* TracingDirectory::readdir()
{
  Directory::Entry* dirent = StackableDirectory::readdir();
  if ( dirent != NULL )
  {
    if ( log.get_level() >= Log::LOG_DEBUG && dirent->get_stat() != NULL )
    {
      ostringstream stat_oss;
      stat_oss << *dirent->get_stat();
      log.get_stream( Log::LOG_DEBUG ) <<
        "yieldfs::TracingDirectory::readdir( " << path << " ) -> " <<
        dirent->get_name() << ": " << stat_oss.str();
    }
    else
    {
      log.get_stream( Log::LOG_INFO ) <<
        "yieldfs::TracingDirectory::readdir( " << path << " ) -> " <<
        dirent->get_name();
    }

    return dirent;
  }
  else
    return NULL;
}


// tracing_file.cpp
TracingFile::TracingFile( Log& log, const Path& path, File& underlying_file )
  : StackableFile( underlying_file ),
    log( log.inc_ref() ),
    path( path )
{ }

TracingFile::~TracingFile()
{
  close();
  Log::dec_ref( log );
}

bool TracingFile::close()
{
  return TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::close",
    path,
    StackableFile::close()
  );
}

bool TracingFile::datasync()
{
  return TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::datasync",
    path,
    StackableFile::datasync()
  );
}

Stat* TracingFile::getattr()
{
  Stat* stbuf = StackableFile::getattr();

  TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::getattr",
    path,
    stbuf != NULL
  );

  return stbuf;
}

bool TracingFile::getlk( bool exclusive, uint64_t offset, uint64_t length )
{
  return TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::getlk",
    path,
    offset,
    length,
    StackableFile::getlk( exclusive, offset, length )
  );
}

bool TracingFile::getxattr( const string& name, string& out_value )
{
  return TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::getxattr",
    path,
    name,
    StackableFile::getxattr( name, out_value )
  );
}


bool TracingFile::listxattr( vector<string>& out_names )
{
  return TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::listxattr",
    path,
    StackableFile::listxattr( out_names )
  );
}

ssize_t TracingFile::read( void* buf, size_t buflen, uint64_t offset )
{
  ssize_t read_ret = StackableFile::read( buf, buflen, offset );
  TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::read",
    path,
    buflen,
    offset,
    read_ret >= 0
  );
  return read_ret;
}

bool TracingFile::removexattr( const string& name )
{
  return TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::removexattr",
    path,
    name,
    StackableFile::removexattr( name )
  );
}

bool TracingFile::setlk( bool exclusive, uint64_t offset, uint64_t length )
{
  return TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::setlk",
    path,
    offset,
    length,
    StackableFile::setlk( exclusive, offset, length )
  );
}

bool TracingFile::setlkw( bool exclusive, uint64_t offset, uint64_t length )
{
  return TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::setlkw",
    path,
    offset,
    length,
    StackableFile::setlkw( exclusive, offset, length )
  );
}

bool TracingFile::setxattr
(
  const string& name,
  const string& value,
  int flags
)
{
  return TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::setxattr",
    path,
    name,
    StackableFile::setxattr( name, value, flags )
  );
}

bool TracingFile::sync()
{
  return TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::sync",
    path,
    StackableFile::sync()
  );
}

bool TracingFile::truncate( uint64_t new_size )
{
  return TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::truncate",
    path,
    0,
    new_size,
    StackableFile::truncate( new_size )
  );
}

bool TracingFile::unlk( uint64_t offset, uint64_t length )
{
  return TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::unlk",
    path,
    offset,
    length,
    StackableFile::unlk( offset, length )
  );
}

ssize_t TracingFile::write( const void* buf, size_t buflen, uint64_t offset )
{
  ssize_t write_ret = StackableFile::write( buf, buflen, offset );

  TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::write",
    path,
    buflen,
    offset,
    write_ret == static_cast<ssize_t>( buflen )
  );

  return write_ret;
}


// tracing_volume.cpp
#include <iostream>


TracingVolume::TracingVolume()
  : StackableVolume( *new Volume ),
    log( Log::open( cout, Log::LOG_INFO ) )
{ }

TracingVolume::TracingVolume( Volume& underlying_volume )
  : StackableVolume( underlying_volume ),
    log( Log::open( cout, Log::LOG_INFO ) )
{ }

TracingVolume::TracingVolume( Log& log, Volume& underlying_volume )
  : StackableVolume( underlying_volume ), log( log.inc_ref() )
{ }

bool TracingVolume::access( const Path& path, int amode )
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::access",
    path,
    static_cast<mode_t>( amode ),
    StackableVolume::access( path, amode )
  );
}

Stat* TracingVolume::getattr( const Path& path )
{
  Stat* stbuf = StackableVolume::getattr( path );
  trace( log, "yieldfs::TracingVolume::getattr", path, stbuf != NULL );
  if ( stbuf != NULL )
  {
    ostringstream stbuf_oss;
    stbuf_oss << *stbuf;
    log.get_stream( Log::LOG_DEBUG ) <<
      "yieldfs::TracingVolume::getattr returning Stat " << stbuf_oss.str();
  }
  return stbuf;
}

bool TracingVolume::getxattr
(
  const Path& path,
  const string& name,
  string& out_value
)
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::getxattr",
    path,
    name,
    out_value,
    StackableVolume::getxattr( path, name, out_value )
  );
}

bool TracingVolume::link( const Path& old_path, const Path& new_path )
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::link",
    old_path,
    new_path,
    StackableVolume::link( old_path, new_path )
  );
}

bool
TracingVolume::listxattr
(
  const Path& path,
  vector<string>& out_names
)
{
  if
  (
    trace
    (
      log,
      "yieldfs::TracingVolume::listxattr",
      path,
      StackableVolume::listxattr( path, out_names )
    )
  )
  {
    Log::Stream log_stream =
      log.get_stream( Log::LOG_INFO );
    log_stream << "  yieldfs::TracingVolume: xattr names: ";
    for
    (
      vector<string>::const_iterator name_i = out_names.begin();
      name_i != out_names.end();
      name_i++
    )
      log_stream << *name_i << " ";
    return true;
  }
  else
    return false;
}

bool TracingVolume::mkdir( const Path& path, mode_t mode )
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::mkdir",
    path,
    mode,
    StackableVolume::mkdir( path, mode )
  );
}

File*
TracingVolume::open
(
  const Path& path,
  uint32_t flags,
  mode_t mode,
  uint32_t attributes
)
{
  File* file = StackableVolume::open( path, flags, mode, attributes );
  if ( file != NULL )
    file = new TracingFile( log, path, *file );

  Log::Stream log_stream =
    log.get_stream( Log::LOG_INFO );
  log_stream << "yieldfs::TracingVolume::open( " << path <<
    ", " << flags << ", " << mode << ", " << attributes << " )";

  trace( log_stream, file != NULL );

  return file;
}

Directory* TracingVolume::opendir( const Path& path )
{
  Directory* directory = StackableVolume::opendir( path );
  if ( directory != NULL )
    directory = new TracingDirectory( log, path, *directory );

  trace( log, "yieldfs::TracingVolume::opendir", path, directory != NULL );

  return directory;
}

Path* TracingVolume::readlink( const Path& path )
{
  Path* link_path = StackableVolume::readlink( path );
  trace( log, "yieldfs::TracingVolume::readlink", path, link_path != NULL );
  return link_path;
}

bool TracingVolume::removexattr( const Path& path, const string& name )
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::removexattr",
    path,
    name,
    StackableVolume::removexattr( path, name )
  );
}

bool TracingVolume::rename( const Path& from_path, const Path& to_path )
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::rename",
    from_path,
    to_path,
    StackableVolume::rename( from_path, to_path )
  );
}

bool TracingVolume::rmdir( const Path& path )
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::rmdir",
    path,
    StackableVolume::rmdir( path )
  );
}

bool
TracingVolume::setattr
(
  const Path& path,
  const Stat& stbuf,
  uint32_t to_set
)
{
  Log::Stream log_stream =
    log.get_stream( Log::LOG_INFO );
  log_stream << "yieldfs::TracingVolume::setattr( ";
  log_stream << path << ", ";
  ostringstream stbuf_oss;
  stbuf_oss << stbuf;
  log_stream << stbuf_oss.str() << ", ";
  log_stream << to_set;
  log_stream << " )";
  return trace
  (
    log_stream,
    StackableVolume::setattr( path, stbuf, to_set )
  );
}

bool
TracingVolume::setxattr
(
  const Path& path,
  const string& name,
  const string& value,
  int32_t flags
)
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::setxattr",
    path,
    name,
    value,
    StackableVolume::setxattr( path, name, value, flags )
  );
}

bool TracingVolume::statvfs( const Path& path, struct statvfs& buf )
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::statvfs",
    path,
    StackableVolume::statvfs( path, buf )
  );
}

bool TracingVolume::symlink( const Path& to_path, const Path& from_path )
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::symlink",
    to_path,
    from_path,
    StackableVolume::symlink( to_path, from_path )
  );
}

bool
TracingVolume::trace
(
  Log& log,
  const char* operation_name,
  const Path& path,
  bool operation_result
)
{
  Log::Stream log_stream =
    log.get_stream( Log::LOG_INFO );
  log_stream << operation_name << "( " << path << " )";
  return trace( log_stream, operation_result );
}

bool
TracingVolume::trace
(
  Log& log,
  const char* operation_name,
  const Path& path,
  mode_t mode,
  bool operation_result
)
{
  Log::Stream log_stream =
    log.get_stream( Log::LOG_INFO );
  log_stream << operation_name << "( " << path << ", " << mode << " )";
  return trace( log_stream, operation_result );
}

bool
TracingVolume::trace
(
  Log& log,
  const char* operation_name,
  const Path& old_path,
  const Path& new_path,
  bool operation_result
)
{
  Log::Stream log_stream =
    log.get_stream( Log::LOG_INFO );
  log_stream << operation_name << "( " << old_path << ", " << new_path << " )";
  return trace( log_stream, operation_result );
}

bool
TracingVolume::trace
(
  Log& log,
  const char* operation_name,
  const Path& path,
  const string& xattr_name,
  const string& xattr_value,
  bool operation_result
)
{
  Log::Stream log_stream =
    log.get_stream( Log::LOG_INFO );
  log_stream << operation_name << "( " << path << ", "
    << xattr_name << ", " << xattr_value << " )";
  return trace( log_stream, operation_result );
}

bool
TracingVolume::trace
(
  Log& log,
  const char* operation_name,
  const Path& path,
  uint64_t size,
  uint64_t offset,
  bool operation_result
)
{
  Log::Stream log_stream =
    log.get_stream( Log::LOG_INFO );
  log_stream << operation_name << "( " << path << ", "
    << size << ", " << offset << " )";
  return trace( log_stream, operation_result );
}

bool
TracingVolume::trace
(
  Log::Stream& log_stream,
  bool operation_result
)
{
  if ( operation_result )
   log_stream << " -> success.";
  else
   log_stream << " -> failed: " << yield::platform::Exception() << ".";

  return operation_result;
}

bool TracingVolume::truncate( const Path& path, uint64_t new_size )
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::truncate",
    path,
    0,
    new_size,
    StackableVolume::truncate( path, new_size )
  );
}

bool TracingVolume::unlink( const Path& path )
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::unlink",
    path,
    StackableVolume::unlink( path )
  );
}

Path TracingVolume::volname( const Path& path )
{
  log.get_stream( Log::LOG_INFO ) <<
    "yieldfs::TracingVolume::volname( " << path << " ) -> success.";
  return StackableVolume::volname( path );
}


// unix_fuse.cpp
#ifndef _WIN32

UnixFUSE::UnixFUSE( uint32_t flags, Volume& volume )
  : FUSE( flags, volume )
{ }

int UnixFUSE::access( const char* path, int mode )
{
  return get_volume().access( path, mode ) ? 0 : -EACCES;
}

int UnixFUSE::chmod( const char* path, mode_t mode )
{
  return get_volume().chmod( path, mode ) ? 0 : ( -1 * errno );
}

int UnixFUSE::chown( const char* path, uid_t uid, gid_t gid )
{
  return get_volume().chown( path, uid, gid ) ? 0 : ( -1 * errno );
}

int
UnixFUSE::create
(
  const char* path,
  mode_t mode,
  struct fuse_file_info* fi
)
{
  uint32_t flags = O_CREAT|O_WRONLY|O_TRUNC;
#ifdef O_DIRECT
  if ( ( get_flags() & FLAG_DIRECT_IO ) == FLAG_DIRECT_IO )
    flags |= O_DIRECT;
#endif

  File* file = get_volume().open( path, flags, mode );
  if ( file != NULL )
  {
    fi->fh = reinterpret_cast<uint64_t>( file );
    if ( ( get_flags() & FLAG_DIRECT_IO ) == FLAG_DIRECT_IO )
      fi->direct_io = 1;
    return 0;
  }
  else
    return -1 * errno;
}

int
UnixFUSE::fgetattr
(
  const char*,
  struct stat *stbuf,
  struct fuse_file_info* fi
)
{
  Stat* yield_stbuf = get_file( fi )->getattr();
  if ( yield_stbuf != NULL )
  {
    *stbuf = *yield_stbuf;
    Stat::dec_ref( *yield_stbuf );
    return 0;
  }
  else
    return -1 * errno;
}

int UnixFUSE::fsync( const char*, int isdatasync, struct fuse_file_info* fi )
{
  if ( isdatasync )
  {
    if ( get_file( fi )->datasync() )
      return 0;
  }
  else
  {
    if ( get_file( fi )->sync() )
      return 0;
  }

  return -1 * errno;
}

int
UnixFUSE::ftruncate
(
  const char* path,
  off_t size,
  struct fuse_file_info* fi
)
{
  if ( get_file( fi ) )
  {
    if ( get_file( fi )->truncate( size ) )
      return 0;
  }
  else if ( get_volume().truncate( path, size ) )
    return 0;

  return -1 * errno;
}

int UnixFUSE::getattr( const char* path, struct stat *stbuf )
{
  Stat* yield_stbuf = get_volume().stat( path );
  if ( yield_stbuf != NULL )
  {
    *stbuf = *yield_stbuf;
    Stat::dec_ref( *yield_stbuf );
    return 0;
  }
  else
    return -1 * errno;
}

int
UnixFUSE::getxattr
(
   const char* path,
   const char* name,
   char *value,
   size_t size
)
{
  string value_str;
  if ( get_volume().getxattr( path, name, value_str ) )
  {
    if ( size == 0 )
      return value_str.size();
    else if ( size >= value_str.size() )
    {
      memcpy( value, value_str.c_str(), value_str.size() );
      return value_str.size();
    }
    else
      return -1 * ERANGE;
  }
  else
    return -1 * errno;
}

int UnixFUSE::link( const char* path, const char* linkpath )
{
  return get_volume().link( path, linkpath ) ? 0 : ( -1 * errno );
}

int UnixFUSE::listxattr( const char* path, char *list, size_t size )
{
  vector<string> xattr_names;
  if ( get_volume().listxattr( path, xattr_names ) )
  {
    if ( list != NULL && size > 0 )
    {
      size_t list_size_consumed = 0;
      for
      (
        vector<string>::const_iterator
          xattr_name_i = xattr_names.begin();
        xattr_name_i != xattr_names.end();
        xattr_name_i++
      )
      {
        const string& xattr_name = *xattr_name_i;
        if ( xattr_name.size()+1 <= ( size - list_size_consumed ) )
        {
          memcpy
          (
            list+list_size_consumed,
            xattr_name.c_str(),
            xattr_name.size()+1
          ); // Include the trailing \0

          list_size_consumed += xattr_name.size()+1;
        }
        else
          return -1 * ERANGE;
      }

      return static_cast<int>( list_size_consumed );
    }
    else
    {
      size = 0;

      for
      (
        vector<string>::const_iterator
          xattr_name_i = xattr_names.begin();
        xattr_name_i != xattr_names.end();
        xattr_name_i++
      )
        size += ( *xattr_name_i ).size()+1;

      return static_cast<int>( size );
    }
  }
  else
    return -1 * errno;
}

int
UnixFUSE::lock
(
  const char* path,
  struct fuse_file_info *fi,
  int cmd,
  struct flock* flock_
)
{
  bool exclusive = flock_->l_type == F_WRLCK;
  uint64_t offset = flock_->l_start;
  uint64_t length = flock_->l_len;

  switch ( cmd )
  {
    case F_GETLK:
    {
      if ( !get_file( fi )->getlk( exclusive, offset, length ) )
        flock_->l_type = F_UNLCK;

      return 0;
    }
    break;

    case F_SETLK:
    case F_SETLKW:
    {
      switch ( flock_->l_type )
      {
        case F_RDLCK:
        case F_WRLCK:
        {
          if ( cmd == F_SETLK )
          {
            if ( get_file( fi )->setlk( exclusive, offset, length ) )
              return 0;
          }
          else
          {
            if ( get_file( fi )->setlkw( exclusive, offset, length ) )
              return 0;
          }
        }
        break;

        case F_UNLCK:
        {
          if ( get_file( fi )->unlk( offset, length ) )
            return 0;
        }
        break;

        default: DebugBreak();
      }
    }
    break;

    default: DebugBreak();
  }

  return -1 * errno;
}

int UnixFUSE::main( char* argv0, const Path& mount_point )
{
  char* argv[] = { argv0, NULL };
  struct fuse_args fuse_args_ = FUSE_ARGS_INIT( 1, argv );
  return main( fuse_args_, mount_point );
}

int UnixFUSE::main( struct fuse_args& fuse_args_, const Path& mount_point )
{
  struct fuse_chan* fuse_chan_ = fuse_mount( mount_point, &fuse_args_ );
  if ( fuse_chan_ )
  {
    struct fuse_operations operations =
    {
      getattr,
      readlink,
      NULL, // getdir
      NULL, // mknod
      mkdir,
      unlink,
      rmdir,
      symlink,
      rename,
      link,
      chmod,
      chown,
      truncate,
      NULL, // utime, deprecated for utimens
      open,
      read,
      write,
      statfs,
      NULL, // flush
      release,
      fsync,
      setxattr,
      getxattr,
      listxattr,
      removexattr,
      NULL, // opendir
      readdir,
      NULL, // releasedir
      NULL, // fsyncdir
      NULL, // finit
      NULL, // destroy
      access,
      create,
      ftruncate,
      fgetattr,
      lock,
      utimens,
      NULL // bmap
    };

    struct fuse* fuse_ =
      fuse_new
      (
        fuse_chan_,
        &fuse_args_,
        &operations,
        sizeof( operations ),
        this
      );

    if ( fuse_ )
    {
      fuse_set_signal_handlers( fuse_get_session( fuse_ ) );
      return fuse_loop_mt( fuse_ );
//          return fuse_loop( fuse_ );
    }
    else
      return errno;
  }
  else
    return errno;
}

int UnixFUSE::mkdir( const char* path, mode_t mode )
{
  return get_volume().mkdir( path, mode ) ? 0 : ( -1 * errno );
}

int UnixFUSE::open( const char* path, struct fuse_file_info* fi )
{
  uint32_t flags = fi->flags;
  if ( ( flags & S_IFREG ) == S_IFREG )
    flags ^= S_IFREG;

#ifdef O_DIRECT
  if ( ( flags & O_DIRECT ) == O_DIRECT )
    fi->direct_io = 1;
  else if ( ( get_flags() & FLAG_DIRECT_IO ) == FLAG_DIRECT_IO )
  {
    fi->direct_io = 1;
    flags |= O_DIRECT;
  }
#else
  if ( ( get_flags() & FLAG_DIRECT_IO ) == FLAG_DIRECT_IO )
    fi->direct_io = 1;
#endif

  File* file = get_volume().open( path, flags );
  if ( file != NULL )
  {
    fi->fh = reinterpret_cast<uint64_t>( file );
    return 0;
  }
  else
    return -1 * errno;
}

int
UnixFUSE::read
(
  const char* path,
  char *rbuf,
  size_t size,
  off_t offset,
  struct fuse_file_info* fi
 )
{
  ssize_t read_ret = get_file( fi )->read( rbuf, size, offset );
  if ( read_ret >= 0 )
    return static_cast<int>( read_ret );
  else
    return -1 * errno;
}

int
UnixFUSE::readdir
(
  const char* path,
  void* buf,
  fuse_fill_dir_t filler,
  off_t offset,
  struct fuse_file_info* fi
)
{
  Directory* dir = get_volume().opendir( path );
  if ( dir != NULL )
  {
    Directory::Entry* dirent = dir->readdir();

    while ( dirent != NULL )
    {
      struct stat stbuf;
      if ( dirent->get_stat() != NULL )
        stbuf = *dirent->get_stat();
      else
        DebugBreak();

      filler
      (
        buf,
        dirent->get_name(),
        &stbuf, 0
      );

      Directory::Entry::dec_ref( *dirent );

      dirent = dir->readdir();
    }

    Directory::dec_ref( *dir );

    return 0;
  }
  else
    return -1 * errno;
}

int UnixFUSE::readlink( const char* path, char *linkbuf, size_t size )
{
  Path* linkpath = get_volume().readlink( path );
  if ( linkpath != NULL )
  {
    if ( size > linkpath->size() + 1 )
      size = linkpath->size() + 1; // FUSE wants the terminating \0,
                                   // even though the readlink
                                   // system call doesn't
    memcpy
    (
      linkbuf,
      *linkpath,
      size
    );

    Path::dec_ref( *linkpath );

    // Don't return size here,
    // FUSE disagrees with the system call about that, too
    return 0;
  }
  else
    return -1 * errno;
}

int UnixFUSE::release( const char* path, struct fuse_file_info* fi )
{
  // close explicitly to trigger replication in XtreemFS
  // there may be extra references to the File (e.g. timers) ->
  // the dec_ref may not destroy the reference ->
  // close() may not be called for a while if we don't call it here
  File* file = get_file( fi );
  if ( file != NULL )
  {
    fi->fh = 0;
    int ret = file->close() ? 0 : -1 * errno;
    File::dec_ref( *file );
    return ret;
  }
  else
    return 0;
}

int UnixFUSE::rename( const char* path, const char *topath )
{
  return get_volume().rename( path, topath ) ? 0 : ( -1 * errno );
}

int UnixFUSE::removexattr( const char* path, const char* name )
{
  return get_volume().removexattr( path, name ) ? 0 : ( -1 * errno );
}

int UnixFUSE::rmdir( const char* path )
{
  return get_volume().rmdir( path ) ? 0 : ( -1 * errno );
}

int
UnixFUSE::setxattr
(
  const char* path,
  const char* name,
  const char *value,
  size_t size,
  int flags
)
{
  string value_str( value, size );
  if ( get_volume().setxattr( path, name, value_str, flags ) )
    return 0;
  else
    return -1 * errno;
}

int UnixFUSE::statfs( const char* path, struct statvfs* sbuf )
{
  if ( sbuf )
    return get_volume().statvfs( path, *sbuf ) ? 0 : ( -1 * errno );
  else
    return -1 * EFAULT;
}

int UnixFUSE::symlink( const char* path, const char* linkpath )
{
  return get_volume().symlink( path, linkpath ) ? 0 : ( -1 * errno );
}

int UnixFUSE::truncate( const char* path, off_t size )
{
  return get_volume().truncate( path, size ) ? 0 : ( -1 * errno );
}

int UnixFUSE::unlink( const char* path )
{
  return get_volume().unlink( path ) ? 0 : ( -1 * errno );
}

int UnixFUSE::utimens( const char* path, const timespec tv[2] )
{
  return get_volume().utime( path, tv[0], tv[1] ) ? 0 : ( -1 * errno );
}

int
UnixFUSE::write
(
  const char* path,
  const char *wbuf,
  size_t size,
  off_t offset,
  struct fuse_file_info* fi
)
{
  size_t write_ret = get_file( fi )->write( wbuf, size, offset );
  if ( write_ret >= 0 )
    return static_cast<int>( write_ret );
  else
    return -1 * errno;
}

#endif


// win32_fuse.cpp
#ifdef _WIN32

Win32FUSE::Win32FUSE( uint32_t flags, Volume& volume )
  : FUSE( flags, volume )
{ }

int DOKAN_CALLBACK
Win32FUSE::CreateFile
(
  LPCWSTR FileName,
  DWORD DesiredAccess,
  DWORD, // ShareMode,
  DWORD CreationDisposition,
  DWORD FlagsAndAttributes,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  Volume& volume = get_volume( DokanFileInfo );

  unsigned long open_flags = 0;

  // Create a file in a directory
  //if ( ( DesiredAccess & FILE_ADD_FILE ) == FILE_ADD_FILE )
  //  ;

  // Create a subdirectory
  //if ( ( DesiredAccess & FILE_ADD_SUBDIRECTORY ) == FILE_ADD_SUBDIRECTORY )
  //  ;

  if ( ( DesiredAccess & FILE_APPEND_DATA ) == FILE_APPEND_DATA )
    open_flags |= O_APPEND;

  // For a directory, the right to delete a directory
  // and all the files it contains
  //if ( ( DesiredAccess & FILE_DELETE_CHILD ) == FILE_DELETE_CHILD )
  //  ;

  //if ( ( DesiredAccess & FILE_EXECUTE ) == FILE_EXECUTE )
  //  ;

  //if ( ( DesiredAccess & FILE_LIST_DIRECTORY ) == FILE_LIST_DIRECTORY )
  //  ;

  if ( ( DesiredAccess & FILE_READ_ATTRIBUTES ) == FILE_READ_ATTRIBUTES )
    open_flags |= O_RDONLY;

  if ( ( DesiredAccess & FILE_READ_DATA ) == FILE_READ_DATA )
    open_flags |= O_RDONLY;

  if ( ( DesiredAccess & FILE_READ_EA ) == FILE_READ_EA )
    open_flags |= O_RDONLY;

  //if ( ( DesiredAccess & FILE_TRAVERSE ) == FILE_TRAVERSE )
  //  ;

  if ( ( DesiredAccess & FILE_WRITE_ATTRIBUTES ) == FILE_WRITE_ATTRIBUTES )
    open_flags |= O_WRONLY;

  if ( ( DesiredAccess & FILE_WRITE_DATA ) == FILE_WRITE_DATA )
    open_flags |= O_WRONLY;

  if ( ( DesiredAccess & FILE_WRITE_EA ) == FILE_WRITE_EA )
    open_flags |= O_WRONLY;

  if ( ( open_flags & O_RDONLY ) == O_RDONLY &&
       ( open_flags & O_WRONLY ) == O_WRONLY )
  {
    open_flags ^= ( O_RDONLY|O_WRONLY );
    open_flags |= O_RDWR;
  }


  uint32_t file_attributes = 0;

  if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_ARCHIVE ) ==
       FILE_ATTRIBUTE_ARCHIVE )
    file_attributes |= FILE_ATTRIBUTE_ARCHIVE;

  if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_ENCRYPTED ) ==
       FILE_ATTRIBUTE_ENCRYPTED )
    file_attributes |= FILE_ATTRIBUTE_ENCRYPTED;

  if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_HIDDEN ) ==
       FILE_ATTRIBUTE_HIDDEN )
    file_attributes |= FILE_ATTRIBUTE_HIDDEN;

  if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_NORMAL ) ==
       FILE_ATTRIBUTE_NORMAL )
    file_attributes |= FILE_ATTRIBUTE_NORMAL;

  if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_OFFLINE ) ==
       FILE_ATTRIBUTE_OFFLINE )
    file_attributes |= FILE_ATTRIBUTE_OFFLINE;

  if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_READONLY ) ==
       FILE_ATTRIBUTE_READONLY )
    file_attributes |= FILE_ATTRIBUTE_READONLY;

  if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_SYSTEM ) ==
       FILE_ATTRIBUTE_SYSTEM )
    file_attributes |= FILE_ATTRIBUTE_SYSTEM;

  if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_TEMPORARY ) ==
       FILE_ATTRIBUTE_TEMPORARY )
    file_attributes |= FILE_ATTRIBUTE_TEMPORARY;

  if ( file_attributes == 0 )
    file_attributes = FILE_ATTRIBUTE_NORMAL;

  DokanFileInfo->IsDirectory = FALSE;

  File* file = NULL;

  switch ( CreationDisposition )
  {
    case CREATE_NEW: // Create the file only if it does not exist already
    {
      file = volume.open
             (
               FileName,
               open_flags|O_CREAT|O_EXCL,
               File::MODE_DEFAULT,
               file_attributes
             );
    }
    break;

    case CREATE_ALWAYS: // Create the file and overwrite it if it exists
    {
      Path path( FileName );

      if ( !volume.unlink( path ) && volume.rmdir( path ) )
      {
        DokanFileInfo->IsDirectory = TRUE;
        return ERROR_SUCCESS;
      }
      else
      {
        file = volume.open
               (
                 path,
                 open_flags|O_CREAT,
                 File::MODE_DEFAULT,
                 file_attributes
               );
      }
    }
    break;

    case OPEN_ALWAYS: // Open an existing file; create if it doesn't exist
    {
      file = volume.open
             (
               FileName,
               open_flags|O_CREAT,
               File::MODE_DEFAULT,
               file_attributes
             );
    }
    break;

    case OPEN_EXISTING: // Only open an existing file
    {
      Path path( FileName );

      if ( path == Path::SEPARATOR )
      {
        DokanFileInfo->IsDirectory = TRUE;
        return ERROR_SUCCESS;
      }
      else if ( volume.isdir( path ) )
      {
        DokanFileInfo->IsDirectory = TRUE;
        return ERROR_SUCCESS;
      }
      else
      {
        file = volume.open
               (
                 path,
                 open_flags,
                 File::MODE_DEFAULT,
                 file_attributes
               );
      }
    }
    break;

    case TRUNCATE_EXISTING: // Only open an existing file and truncate it
    {
      file = volume.open
             (
               FileName,
               open_flags|O_TRUNC,
               File::MODE_DEFAULT,
               file_attributes
             );

      if ( file != NULL )
        file->truncate( 0 );
    }
    break;

    default: ::DebugBreak();
  }

  if ( file != NULL )
  {
    DokanFileInfo->Context = reinterpret_cast<UINT64>( file );
    return ERROR_SUCCESS;
  }

  return -1 * ::GetLastError();
}

int DOKAN_CALLBACK
Win32FUSE::CreateDirectory
(
  LPCWSTR FileName,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  Path path( FileName );
  if ( path != Path::SEPARATOR )
  {
    if ( get_volume( DokanFileInfo ).mkdir( path ) )
      return ERROR_SUCCESS;
    else
      return -1 * ::GetLastError();
  }
  else
    return -1 * ERROR_ALREADY_EXISTS;
}

int DOKAN_CALLBACK
Win32FUSE::CloseFile
(
  LPCWSTR, // FileName,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  // close() explicitly to trigger replication in XtreemFS
  // there may be extra references to the File (e.g. timers) ->
  // the dec_ref may not destroy the reference ->
  // close() may not be called for a while if we don't call it here
  File* file = get_file( DokanFileInfo );
  if ( file != NULL )
  {
    DokanFileInfo->Context = NULL;

    int ret;
    if ( file->close() )
      ret = ERROR_SUCCESS;
    else
      ret = -1 * ::GetLastError();

    File::dec_ref( *file );

    return ret;
  }
  else
    return ERROR_SUCCESS;
}

int DOKAN_CALLBACK
Win32FUSE::Cleanup
(
  LPCWSTR FileName,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  if ( DokanFileInfo->DeleteOnClose )
  {
    if ( get_volume( DokanFileInfo ).unlink( FileName ) )
      return ERROR_SUCCESS;
    else
      return -1 * ::GetLastError();
  }
  else
    return ERROR_SUCCESS;
}

int DOKAN_CALLBACK
Win32FUSE::DeleteDirectory
(
  LPCWSTR FileName,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  if ( get_volume( DokanFileInfo ).rmdir( FileName ) )
    return ERROR_SUCCESS;
  else
    return -1 * ::GetLastError();
}

int DOKAN_CALLBACK
Win32FUSE::DeleteFile
(
  LPCWSTR, // FileName,
  PDOKAN_FILE_INFO
)
{
  // Don't actually unlink the file here,
  // simply check whether we have access to delete it.
  // The unlink will be in cleanup, when DeleteOnClose is set
  return ERROR_SUCCESS;
}

int DOKAN_CALLBACK
Win32FUSE::FindFiles
(
  LPCWSTR FileName,
  PFillFindData FillFindData,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  Directory* dir = get_volume( DokanFileInfo ).opendir( FileName );

  if ( dir != NULL )
  {
    Directory::Entry* dirent = dir->readdir();
    while ( dirent != NULL )
    {
      if ( dirent->get_name() != L"." && dirent->get_name() != L".." )
      {
        WIN32_FIND_DATA find_data = *dirent->get_stat();
        wcsncpy_s
        (
          find_data.cFileName,
          260,
          dirent->get_name(),
          dirent->get_name().size()
        );

        FillFindData( &find_data, DokanFileInfo );
      }

      Directory::Entry::dec_ref( *dirent );

      dirent = dir->readdir();
    }

    Directory::dec_ref( *dir );

    return ERROR_SUCCESS;
  }
  else
    return -1 * ::GetLastError();
}

int DOKAN_CALLBACK
Win32FUSE::FlushFileBuffers
(
  LPCWSTR, // FileName,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  if ( get_file( DokanFileInfo )->datasync() )
    return ERROR_SUCCESS;
  else
    return -1 * ::GetLastError();
}

int DOKAN_CALLBACK
Win32FUSE::GetDiskFreeSpace
(
  PULONGLONG FreeBytesAvailable,
  PULONGLONG TotalNumberOfBytes,
  PULONGLONG TotalNumberOfFreeBytes,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  struct statvfs stbuf;
  if
  (
    get_volume( DokanFileInfo ).statvfs( Path(), stbuf )
  )
  {
    if ( FreeBytesAvailable )
      *FreeBytesAvailable = stbuf.f_bsize * stbuf.f_bavail;
    if ( TotalNumberOfBytes )
      *TotalNumberOfBytes = stbuf.f_bsize * stbuf.f_blocks;
    if ( TotalNumberOfFreeBytes )
      *TotalNumberOfFreeBytes = stbuf.f_bsize * stbuf.f_bfree;

    return ERROR_SUCCESS;
  }
  else
    return -1 * ::GetLastError();
}

int DOKAN_CALLBACK
Win32FUSE::GetFileInformation
(
  LPCWSTR FileName,
  LPBY_HANDLE_FILE_INFORMATION HandleFileInformation,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  Stat* stbuf = get_volume( DokanFileInfo ).stat( FileName );
  if ( stbuf != NULL )
  {
    *HandleFileInformation = *stbuf;
    Stat::dec_ref( *stbuf );
    return ERROR_SUCCESS;
  }
  else
    return -1 * ::GetLastError();
}

int DOKAN_CALLBACK
Win32FUSE::GetVolumeInformation
(
  LPWSTR VolumeNameBuffer,
  DWORD, // VolumeNameSize,
  LPDWORD VolumeSerialNumber,
  LPDWORD MaximumComponentLength,
  LPDWORD FileSystemFlags,
  LPWSTR FileSystemNameBuffer,
  DWORD, // FileSystemNameSize,
  PDOKAN_FILE_INFO DokanFileInfo )
{
  Path name =
   get_volume( DokanFileInfo ).volname( Path::SEPARATOR );

  struct statvfs stbuf;
  if
  (
    get_volume( DokanFileInfo ).statvfs
    (
      Path(), stbuf
    )
  )
  {
    // wcsncpy causes strange problems, so use wcscpy
    wcscpy( VolumeNameBuffer, name );

    if ( VolumeSerialNumber )
      *VolumeSerialNumber = 0;

    if ( MaximumComponentLength )
      *MaximumComponentLength = stbuf.f_namemax;

    if ( FileSystemFlags )
    {
      *FileSystemFlags = FILE_CASE_PRESERVED_NAMES |
                         FILE_CASE_SENSITIVE_SEARCH |
                         FILE_UNICODE_ON_DISK;
    }

    // wcsncpy causes strange problems, so use wcscpy
    wcscpy( FileSystemNameBuffer, L"yieldfs_FUSE" );

    return ERROR_SUCCESS;
  }
  else
    return -1 * ::GetLastError();
}

int DOKAN_CALLBACK
Win32FUSE::LockFile
(
  LPCWSTR, // FileName,
  LONGLONG ByteOffset,
  LONGLONG Length,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  if ( get_file( DokanFileInfo ) != NULL )
  {
    if ( get_file( DokanFileInfo )->setlkw( true, ByteOffset, Length ) )
      return ERROR_SUCCESS;
    else
      return -1 * ::GetLastError();
  }
  else // ?!
    return ERROR_SUCCESS;
}

int Win32FUSE::main( const Path& mount_point )
{
  DOKAN_OPTIONS options;
  memset( &options, 0, sizeof( options ) );
  options.DriveLetter = static_cast<const wchar_t*>( mount_point )[0];
  options.ThreadCount = 4;
  if ( ( get_flags() & FLAG_DEBUG ) == FLAG_DEBUG )
  {
    options.DebugMode = 1;
    options.UseStdErr = 1;
  }
  options.GlobalContext = ( ULONG64 )this;

  DOKAN_OPERATIONS operations =
  {
    CreateFile,
    OpenDirectory,
    CreateDirectory,
    Cleanup,
    CloseFile,
    ReadFile,
    WriteFile,
    FlushFileBuffers,
    GetFileInformation,
    FindFiles,
    NULL, // FindFilesWithPattern
    SetFileAttributes,
    SetFileTime,
    DeleteFile,
    DeleteDirectory,
    MoveFile,
    SetEndOfFile,
    LockFile,
    UnlockFile,
    GetDiskFreeSpace,
    GetVolumeInformation,
    Unmount
  };

  if ( DokanMain( &options, &operations ) )
    return 0;
  else
    return ( int )GetLastError();
}

int DOKAN_CALLBACK
Win32FUSE::MoveFile
(
  LPCWSTR FileName,
  LPCWSTR NewFileName,
  BOOL ReplaceIfExisting,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  if ( ReplaceIfExisting )
    DebugBreak(); // TODO: Implement ReplaceIfExisting.. atomically?

  if ( get_volume( DokanFileInfo ).rename( FileName, NewFileName ) )
    return ERROR_SUCCESS;
  else
    return -1 * ::GetLastError();
}

int DOKAN_CALLBACK
Win32FUSE::OpenDirectory
(
  LPCWSTR FileName,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  Path path( FileName );

  if ( path == Path::SEPARATOR )
  {
    DokanFileInfo->IsDirectory = TRUE;
    return ERROR_SUCCESS;
  }
  else if ( get_volume( DokanFileInfo ).isdir( path ) )
  {
    DokanFileInfo->IsDirectory = TRUE;
    return ERROR_SUCCESS;
  }
  else
    return -1 * ERROR_FILE_NOT_FOUND;
}

int DOKAN_CALLBACK
Win32FUSE::ReadFile
(
  LPCWSTR FileName,
  LPVOID Buffer,
  DWORD BufferLength,
  LPDWORD ReadLength,
  LONGLONG Offset,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  File* file = get_file( DokanFileInfo );

  ssize_t read_ret;

  if ( file )
    read_ret = file->read( Buffer, BufferLength, Offset );
  else
  {
    file = get_volume( DokanFileInfo ).open( FileName, O_RDONLY );

    if ( file != NULL )
    {
      read_ret = file->read( Buffer, BufferLength, Offset );
      file->close(); // See note in CloseFile; assume this succeeds
      File::dec_ref( *file );
    }
    else
      return -1 * ::GetLastError();
  }

  if ( read_ret >= 0 )
  {
    if ( ReadLength )
      *ReadLength = static_cast<DWORD>( read_ret );
    return ERROR_SUCCESS;
  }
  else
    return -1 * ::GetLastError();
}

int DOKAN_CALLBACK
Win32FUSE::SetEndOfFile
(
  LPCWSTR FileName,
  LONGLONG ByteOffset,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  File* file = get_file( DokanFileInfo );

  if ( file )
  {
    if ( file->truncate( ByteOffset ) )
      return ERROR_SUCCESS;
  }
  else
  {
    if ( get_volume( DokanFileInfo ).truncate( FileName, ByteOffset ) )
      return ERROR_SUCCESS;
  }

  return -1 * ::GetLastError();
}

int DOKAN_CALLBACK
Win32FUSE::SetFileAttributes
(
  LPCWSTR FileName,
  DWORD FileAttributes,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  Stat stbuf;
  stbuf.set_attributes( FileAttributes );
  if
  (
    get_volume( DokanFileInfo ).setattr
    (
      FileName,
      stbuf,
      Volume::SETATTR_ATTRIBUTES
    )
  )
    return ERROR_SUCCESS;
  else
    return -1 * ::GetLastError();
}

int DOKAN_CALLBACK
Win32FUSE::SetFileTime
(
  LPCWSTR FileName,
  CONST FILETIME* CreationTime,
  CONST FILETIME*  LastAccessTime,
  CONST FILETIME*  LastWriteTime,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  if
  (
    get_volume( DokanFileInfo ).utime
    (
      FileName,
      LastAccessTime,
      LastWriteTime,
      CreationTime
    )
  )
    return ERROR_SUCCESS;
  else
    return -1 * ::GetLastError();
}

int DOKAN_CALLBACK
Win32FUSE::UnlockFile
(
  LPCWSTR, // FileName,
  LONGLONG ByteOffset,
  LONGLONG Length,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  if ( get_file( DokanFileInfo ) != NULL )
  {
    if ( get_file( DokanFileInfo )->unlk( ByteOffset, Length ) )
      return ERROR_SUCCESS;
    else
      return -1 * ::GetLastError();
  }
  else // ?!
    return ERROR_SUCCESS;
}

int DOKAN_CALLBACK
Win32FUSE::Unmount
(
  PDOKAN_FILE_INFO
)
{
  return ERROR_SUCCESS;
}

int DOKAN_CALLBACK
Win32FUSE::WriteFile
(
  LPCWSTR FileName,
  LPCVOID Buffer,
  DWORD NumberOfBytesToWrite,
  LPDWORD NumberOfBytesWritten,
  LONGLONG Offset,
  PDOKAN_FILE_INFO DokanFileInfo
)
{
  File* file = get_file( DokanFileInfo );

  ssize_t write_ret;

  if ( file )
    write_ret = file->write( Buffer, NumberOfBytesToWrite, Offset );
  else
  {
    file = get_volume( DokanFileInfo ).open( FileName, O_CREAT|O_WRONLY );
    if ( file != NULL )
    {
      write_ret = file->write( Buffer, NumberOfBytesToWrite, Offset );
      file->close(); // See note in CloseFile; assume this succeeds
      File::dec_ref( *file );
    }
    else
      return -1 * ::GetLastError();
  }

  if ( write_ret >= 0 )
  {
    if ( NumberOfBytesWritten )
      *NumberOfBytesWritten = static_cast<DWORD>( write_ret );
    return ERROR_SUCCESS;
  }
  else
    return -1 * ::GetLastError();
}

#endif

