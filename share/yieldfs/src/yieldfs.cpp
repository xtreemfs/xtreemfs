// Revision: 194

#include "yield.h"
#include "yieldfs.h"
using namespace yieldfs;


// cached_page.h
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).




namespace yieldfs
{
  class CachedPage : public yidl::runtime::HeapBuffer
  {
  public:
    CachedPage( const YIELD::platform::Path& file_path, size_t number, size_t size )
      : yidl::runtime::HeapBuffer( size ), file_path( file_path ), number( number )
    {
      dirty_bit = false;
    }

    void clear_dirty_bit() { dirty_bit = false; }
    const YIELD::platform::Path& get_file_path() const { return file_path; }
    bool get_dirty_bit() const { return dirty_bit; }
    size_t get_number() const { return number; }
    bool operator==( const CachedPage& other ) const { return file_path == other.file_path && number == other.number; }
    void set_dirty_bit() { dirty_bit = true; }

    // yidl::runtime::Object
    YIDL_RUNTIME_OBJECT_PROTOTYPES( CachedPage, 0 );

  private:
    YIELD::platform::Path file_path;
    size_t number;

    bool dirty_bit;
  };

  typedef yidl::runtime::auto_Object<CachedPage> auto_CachedPage;
};


// cached_stat.h
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).




namespace yieldfs
{
  class CachedStat : public yidl::runtime::Object
  {
  public:
    CachedStat( const YIELD::platform::Path& path, YIELD::platform::auto_Stat stbuf )
      : path( path ), stbuf( stbuf )
    {
      creation_epoch_time_s = YIELD::platform::Time::getCurrentUnixTimeS();
    }

    const YIELD::platform::Path& get_path() const { return path; }
    YIELD::platform::auto_Stat get_stat() const { return stbuf; }
    double get_creation_epoch_time_s() const { return creation_epoch_time_s; }

    // Object
    YIDL_RUNTIME_OBJECT_PROTOTYPES( yieldfs::CachedStat, 0 );

  private:
    YIELD::platform::Path path;
    YIELD::platform::auto_Stat stbuf;

    double creation_epoch_time_s;
  };
};


// data_caching_file.h
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).




namespace yieldfs
{
  class PageCache;


  class DataCachingFile : public StackableFile
  {
  public:
    DataCachingFile& operator=( const DataCachingFile& ) { return *this; }

    // YIELD::platform::File
    bool datasync();
    virtual ssize_t read( void* buffer, size_t buffer_len, uint64_t offset );
    bool sync();
    bool truncate( uint64_t offset );
    virtual ssize_t write( const void* buffer, size_t buffer_len, uint64_t offset );

  protected:
    DataCachingFile( PageCache& page_cache, const YIELD::platform::Path& path, YIELD::platform::auto_File underlying_file, YIELD::platform::auto_Log log = NULL );
    ~DataCachingFile();

    PageCache& page_cache;

  private:
    size_t pagesize;
  };
};


// fuse_unix.h
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).



#ifndef _WIN32
#define FUSE_USE_VERSION 26
#include <fuse.h>
#endif


namespace yieldfs
{
#ifndef _WIN32
  class FUSEUnix
  {
  public:
    FUSEUnix( YIELD::platform::auto_Volume volume, uint32_t flags )
      : volume( volume ), flags( flags )
    { }

    int main( char* argv0, const char* mount_point )
    {
      char* argv[] = { argv0, NULL };
      struct fuse_args fuse_args_ = FUSE_ARGS_INIT( 1, argv );
      return main( fuse_args_, mount_point );
    }

    int main( struct fuse_args& fuse_args_, const char* mount_point )
    {
      struct fuse_chan* fuse_chan_ = fuse_mount( mount_point, &fuse_args_ );
      if ( fuse_chan_ )
      {
        static struct fuse_operations operations =
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

        struct fuse* fuse_ = fuse_new( fuse_chan_, &fuse_args_, &operations, sizeof( operations ), this );
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

    static int access( const char* path, int mode )
    {
      return get_volume().access( path, mode ) ? 0 : -EACCES;
    }

    static int chmod( const char* path, mode_t mode )
    {
      return get_volume().chmod( path, mode ) ? 0 : ( -1 * errno );
    }

    static int chown( const char* path, uid_t uid, gid_t gid )
    {
      return get_volume().chown( path, uid, gid ) ? 0 : ( -1 * errno );
    }

    static int create( const char* path, mode_t mode, struct fuse_file_info* fi )
    {
      uint32_t flags = O_CREAT|O_WRONLY|O_TRUNC;
#ifdef O_DIRECT
      if ( ( get_flags() & FUSE::FUSE_FLAG_DIRECT_IO ) == FUSE::FUSE_FLAG_DIRECT_IO )
        flags |= O_DIRECT;
#endif

      fi->fh = reinterpret_cast<uint64_t>( get_volume().open( path, flags, mode ).release() );
      if ( fi->fh != 0 )
      {
        if ( ( get_flags() & FUSE::FUSE_FLAG_DIRECT_IO ) == FUSE::FUSE_FLAG_DIRECT_IO )
          fi->direct_io = 1;

        return 0;
      }
      else
        return -1 * errno;
    }

    static int fgetattr( const char* path, struct stat *stbuf, struct fuse_file_info* fi )
    {
      YIELD::platform::auto_Stat yield_stbuf = get_file( fi )->stat();
      if ( yield_stbuf != NULL )
      {
        *stbuf = *yield_stbuf;
        return 0;
      }
      else
        return -1 * errno;
    }

    static int fsync( const char* path, int isdatasync, struct fuse_file_info* fi )
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

    static int ftruncate( const char* path, off_t size, struct fuse_file_info* fi )
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

    static int getattr( const char* path, struct stat *stbuf )
    {
      YIELD::platform::auto_Stat yield_stbuf = get_volume().stat( path );
      if ( yield_stbuf != NULL )
      {
        *stbuf = *yield_stbuf;
        return 0;
      }
      else
        return -1 * errno;
    }

    static int getxattr( const char* path, const char* name, char *value, size_t size )
    {
      std::string value_str;
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

    static int link( const char* path, const char* linkpath )
    {
      return get_volume().link( path, linkpath ) ? 0 : ( -1 * errno );
    }

    static int listxattr( const char* path, char *list, size_t size )
    {
      std::vector<std::string> xattr_names;
      if ( get_volume().listxattr( path, xattr_names ) )
      {
        if ( list != NULL && size > 0 )
        {
          size_t list_size_consumed = 0;
          for ( std::vector<std::string>::const_iterator xattr_name_i = xattr_names.begin(); xattr_name_i != xattr_names.end(); xattr_name_i++ )
          {
            const std::string& xattr_name = *xattr_name_i;
            if ( xattr_name.size()+1 <= ( size - list_size_consumed ) )
            {
              memcpy( list+list_size_consumed, xattr_name.c_str(), xattr_name.size()+1 ); // Include the trailing \0
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
          for ( std::vector<std::string>::const_iterator xattr_name_i = xattr_names.begin(); xattr_name_i != xattr_names.end(); xattr_name_i++ )
            size += ( *xattr_name_i ).size()+1;
          return static_cast<int>( size );
        }
      }
      else
        return -1 * errno;
    }

    static int lock( const char* path, struct fuse_file_info *fi, int cmd, struct flock* flock_ )
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

    static int mkdir( const char* path, mode_t mode )
    {
      return get_volume().mkdir( path, mode ) ? 0 : ( -1 * errno );
    }

    static int open( const char* path, struct fuse_file_info* fi )
    {
      uint32_t flags = fi->flags;
      if ( ( flags & S_IFREG ) == S_IFREG )
        flags ^= S_IFREG;

#ifdef O_DIRECT
      if ( ( flags & O_DIRECT ) == O_DIRECT )
        fi->direct_io = 1;
      else if ( ( get_flags() & FUSE::FUSE_FLAG_DIRECT_IO ) == FUSE::FUSE_FLAG_DIRECT_IO )
      {
        fi->direct_io = 1;
        flags |= O_DIRECT;
      }
#else
      if ( ( get_flags() & FUSE::FUSE_FLAG_DIRECT_IO ) == FUSE::FUSE_FLAG_DIRECT_IO )
        fi->direct_io = 1;
#endif

      fi->fh = reinterpret_cast<uint64_t>( get_volume().open( path, flags ).release() );
      if ( fi->fh != 0 )
        return 0;
      else
        return -1 * errno;
    }

    static int read( const char* path, char *rbuf, size_t size, off_t offset, struct fuse_file_info* fi )
    {
      ssize_t read_ret = get_file( fi )->read( rbuf, size, offset );
      if ( read_ret >= 0 )
        return static_cast<int>( read_ret );
      else
        return -1 * errno;
    }

    class readdirCallback : public YIELD::platform::Volume::readdirCallback
    {
    public:
      readdirCallback( void* buf, fuse_fill_dir_t filler )
        : buf( buf ), filler( filler )
      { }

      // YIELD::platform::Volume::readdirCallback
      bool operator()( const YIELD::platform::Path& name, YIELD::platform::auto_Stat stbuf )
      {
        struct stat struct_stat_stbuf = *stbuf;
        filler( buf, static_cast<const std::string&>( name ).c_str(), &struct_stat_stbuf, 0 );
        return true;
      }

    private:
      void* buf;
      fuse_fill_dir_t filler;
    };

    static int readdir( const char* path, void* buf, fuse_fill_dir_t filler, off_t offset, struct fuse_file_info* fi )
    {
      YIELD::platform::Volume& volume = get_volume();

      YIELD::platform::auto_Stat yield_stbuf = volume.stat( path );
      if ( yield_stbuf != NULL )
      {
        struct stat stbuf = *yield_stbuf;
        filler( buf, ".", &stbuf, 0 );

        if ( strcmp( path, "/" ) != 0 )
        {
          yield_stbuf = volume.stat( YIELD::platform::Path( path ).split().first );
          if ( yield_stbuf != NULL )
          {
            stbuf = *yield_stbuf;
            filler( buf, "..", &stbuf, 0 );
          }
          else
            return -1 * errno;
        }

        readdirCallback readdir_callback( buf, filler );
        if ( volume.readdir( path, readdir_callback ) )
          return 0;
        else if ( errno != 0 )
          return -1 * errno;
        else
          return -1 * EINTR;
      }
      else
        return -1 * errno;
    }

    static int readlink( const char* path, char *linkbuf, size_t size )
    {
      YIELD::platform::auto_Path linkpath = get_volume().readlink( path );
      if ( linkpath!= NULL )
      {
        if ( size > linkpath->size() + 1 )
          size = linkpath->size() + 1; // FUSE wants the terminating \0, even though the readlink system call doesn't
        memcpy( linkbuf, static_cast<const std::string&>( *linkpath ).c_str(), size );
        // Don't return size here, FUSE disagrees with the system call about that, too
        return 0;
      }
      else
        return -1 * errno;
    }

    static int release( const char* path, struct fuse_file_info* fi )
    {
      yidl::runtime::Object::decRef( get_file( fi ) );
      fi->fh = 0;
      return 0;
    }

    static int rename( const char* path, const char *topath )
    {
      return get_volume().rename( path, topath ) ? 0 : ( -1 * errno );
    }

    static int removexattr( const char* path, const char* name )
    {
      return get_volume().removexattr( path, name ) ? 0 : ( -1 * errno );
    }

    static int rmdir( const char* path )
    {
      return get_volume().rmdir( path ) ? 0 : ( -1 * errno );
    }

    static int setxattr( const char* path, const char* name, const char *value, size_t size, int flags )
    {
      std::string value_str( value, size );
      return get_volume().setxattr( path, name, value_str, flags ) ? 0 : ( -1 * errno );
    }

    static int statfs( const char* path, struct statvfs* sbuf )
    {
      if ( sbuf )
        return get_volume().statvfs( path, *sbuf ) ? 0 : ( -1 * errno );
      else
        return -1 * EFAULT;
    }

    static int symlink( const char* path, const char* linkpath )
    {
      return get_volume().symlink( path, linkpath ) ? 0 : ( -1 * errno );
    }

    static int truncate( const char* path, off_t size )
    {
      return get_volume().truncate( path, size ) ? 0 : ( -1 * errno );
    }

    static int unlink( const char* path )
    {
      // TODO: need to do reference counting here: mark the file "to be deleted", then delete it when we see the last reference
      return get_volume().unlink( path ) ? 0 : ( -1 * errno );
    }

    static int utimens( const char* path, const timespec tv[2] )
    {
      return get_volume().utimens( path, tv[0], tv[1], static_cast<uint64_t>( 0 ) ) ? 0 : ( -1 * errno );
    }

    static int write( const char* path, const char *wbuf, size_t size, off_t offset, struct fuse_file_info* fi )
    {
      size_t write_ret = get_file( fi )->write( wbuf, size, offset );
      if ( write_ret >= 0 )
        return static_cast<int>( write_ret );
      else
        return -1 * errno;
    }

  private:
    YIELD::platform::auto_Volume volume;
    uint32_t flags;


    static inline YIELD::platform::File* get_file( fuse_file_info* fi )
    {
      return fi ? reinterpret_cast<YIELD::platform::File*>( fi->fh ) : NULL;
    }

    static inline uint32_t get_flags()
    {
      return static_cast<FUSEUnix*>( fuse_get_context()->private_data )->flags;
    }

    static inline YIELD::platform::Volume& get_volume()
    {
      return *static_cast<FUSEUnix*>( fuse_get_context()->private_data )->volume;
    }
  };
#endif
};



// fuse_win32.h
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).



#ifdef _WIN32
#include <windows.h>
#include <dokan.h>
#pragma comment( lib, "dokan.lib" )
#endif


namespace yieldfs
{
#ifdef _WIN32
  class FUSEWin32
  {
  public:
    FUSEWin32( YIELD::platform::auto_Volume volume, uint32_t flags )
      : volume( volume ), flags( flags )
    { }

    int main( const char* mount_point )
    {
      DOKAN_OPTIONS options;
      memset( &options, 0, sizeof( options ) );
      options.DriveLetter = static_cast<const wchar_t*>( YIELD::platform::Path( mount_point ) )[0];
      options.ThreadCount = 4;
      if ( ( flags & FUSE::FUSE_FLAG_DEBUG ) == FUSE::FUSE_FLAG_DEBUG )
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

    static int DOKAN_CALLBACK
    CreateFile(
      LPCWSTR					FileName,
      DWORD					DesiredAccess,
      DWORD					ShareMode,
      DWORD					CreationDisposition,
      DWORD					FlagsAndAttributes,
      PDOKAN_FILE_INFO		DokanFileInfo )
    {
      YIELD::platform::Volume& volume = get_volume( DokanFileInfo );
      YIELD::platform::File* file = NULL;

      unsigned long open_flags = 0;
      //if ( ( DesiredAccess & FILE_ADD_FILE ) == FILE_ADD_FILE ) // Create a file in a directory
      //  ;
      //if ( ( DesiredAccess & FILE_ADD_SUBDIRECTORY ) == FILE_ADD_SUBDIRECTORY ) // Create a subdirectory
      //  ;
      if ( ( DesiredAccess & FILE_APPEND_DATA ) == FILE_APPEND_DATA )
        open_flags |= O_APPEND;
      //if ( ( DesiredAccess & FILE_DELETE_CHILD ) == FILE_DELETE_CHILD ) // For a directory, the right to delete a directory and all the files it contains
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

      if ( ( open_flags & O_RDONLY ) == O_RDONLY && ( open_flags & O_WRONLY ) == O_WRONLY )
      {
        open_flags ^= ( O_RDONLY|O_WRONLY );
        open_flags |= O_RDWR;
      }

      uint32_t file_attributes = 0;
      if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_ARCHIVE ) == FILE_ATTRIBUTE_ARCHIVE )
        file_attributes |= FILE_ATTRIBUTE_ARCHIVE;
      if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_ENCRYPTED ) == FILE_ATTRIBUTE_ENCRYPTED )
        file_attributes |= FILE_ATTRIBUTE_ENCRYPTED;
      if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_HIDDEN ) == FILE_ATTRIBUTE_HIDDEN )
        file_attributes |= FILE_ATTRIBUTE_HIDDEN;
      if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_NORMAL ) == FILE_ATTRIBUTE_NORMAL )
        file_attributes |= FILE_ATTRIBUTE_NORMAL;
      if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_OFFLINE ) == FILE_ATTRIBUTE_OFFLINE )
        file_attributes |= FILE_ATTRIBUTE_OFFLINE;
      if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_READONLY ) == FILE_ATTRIBUTE_READONLY )
        file_attributes |= FILE_ATTRIBUTE_READONLY;
      if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_SYSTEM ) == FILE_ATTRIBUTE_SYSTEM )
        file_attributes |= FILE_ATTRIBUTE_SYSTEM;
      if ( ( FlagsAndAttributes & FILE_ATTRIBUTE_TEMPORARY ) == FILE_ATTRIBUTE_TEMPORARY )
        file_attributes |= FILE_ATTRIBUTE_TEMPORARY;
      if ( file_attributes == 0 )
        file_attributes = FILE_ATTRIBUTE_NORMAL;

      DokanFileInfo->IsDirectory = FALSE;


      switch ( CreationDisposition )
      {
        case CREATE_NEW: // Create the file only if it does not exist already
        {
          file = volume.open( FileName, open_flags|O_CREAT|O_EXCL, YIELD::platform::File::DEFAULT_MODE, file_attributes ).release();
        }
        break;

        case CREATE_ALWAYS: // Create the file and overwrite it if it exists
        {
          if ( !volume.unlink( FileName ) )
          {
            if ( volume.rmdir( FileName ) )
            {
              DokanFileInfo->IsDirectory = TRUE;
              return ERROR_SUCCESS;
            }
          }

          file = volume.open( FileName, open_flags|O_CREAT, YIELD::platform::File::DEFAULT_MODE, file_attributes ).release();
        }
        break;

        case OPEN_ALWAYS: // Open an existing file or create it if it doesn't exist
        {
          file = volume.open( FileName, open_flags|O_CREAT, YIELD::platform::File::DEFAULT_MODE, file_attributes ).release();
        }
        break;

        case OPEN_EXISTING: // Only open an existing file
        {
          if ( YIELD::platform::Path( FileName ) == PATH_SEPARATOR_WIDE_STRING )
          {
            DokanFileInfo->IsDirectory = TRUE;
            return ERROR_SUCCESS;
          }
          else
          {
            YIELD::platform::auto_Stat stbuf = volume.stat( FileName );
            if ( stbuf!= NULL )
            {
              if ( stbuf->ISDIR() )
              {
                DokanFileInfo->IsDirectory = TRUE;
                return ERROR_SUCCESS;
              }
              else
                file = volume.open( FileName, open_flags, YIELD::platform::File::DEFAULT_MODE, file_attributes ).release();
            }
            else
              return -1 * ::GetLastError();
          }
        }
        break;

        case TRUNCATE_EXISTING: // Only open an existing file and truncate it
        {
          file = volume.open( FileName, open_flags|O_TRUNC, YIELD::platform::File::DEFAULT_MODE, file_attributes ).release();
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

    static int DOKAN_CALLBACK
    CreateDirectory(
	    LPCWSTR					FileName,
	    PDOKAN_FILE_INFO		DokanFileInfo )
    {
      if ( wcscmp( FileName, PATH_SEPARATOR_WIDE_STRING ) != 0 )
      {
        if ( get_volume( DokanFileInfo ).mkdir( FileName ) )
          return ERROR_SUCCESS;
        else
          return -1 * ::GetLastError();
      }
      else
        return -1 * ERROR_ALREADY_EXISTS;
    }

    static int DOKAN_CALLBACK
    CloseFile(
	    LPCWSTR					FileName,
	    PDOKAN_FILE_INFO		DokanFileInfo )
    {
      yidl::runtime::Object::decRef( get_file( DokanFileInfo ) );
      DokanFileInfo->Context = NULL;
      return ERROR_SUCCESS;
    }

    static int DOKAN_CALLBACK
    Cleanup(
	    LPCWSTR					FileName,
	    PDOKAN_FILE_INFO		DokanFileInfo )
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

    static int DOKAN_CALLBACK
    DeleteDirectory(
	    LPCWSTR				FileName,
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
      if ( get_volume( DokanFileInfo ).rmdir( FileName ) )
        return ERROR_SUCCESS;
      else
        return -1 * ::GetLastError();
    }

    static int DOKAN_CALLBACK
    DeleteFile(
	    LPCWSTR				FileName,
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
      // Don't actually unlink the file here, simply check whether we have access to delete it
      // The unlink will be in cleanup, when DeleteOnClose is set
      return ERROR_SUCCESS;
    }

    class readdirCallback : public YIELD::platform::Volume::readdirCallback
    {
    public:
      readdirCallback( PFillFindData FillFindData, PDOKAN_FILE_INFO	DokanFileInfo )
        : FillFindData( FillFindData ), DokanFileInfo( DokanFileInfo )
      { }

      // YIELD::platform::Volume::readdirCallback
      bool operator()( const YIELD::platform::Path& path, YIELD::platform::auto_Stat stbuf )
      {
        WIN32_FIND_DATA find_data = *stbuf;
        wcsncpy_s( find_data.cFileName, 260, path, path.size() );
        FillFindData( &find_data, DokanFileInfo );
        return true;
      }

    private:
      PFillFindData FillFindData;
      PDOKAN_FILE_INFO DokanFileInfo;
    };

    static int DOKAN_CALLBACK
    FindFiles(
	    LPCWSTR				FileName,
	    PFillFindData		FillFindData, // function pointer
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
      readdirCallback readdir_callback( FillFindData, DokanFileInfo );
      if ( get_volume( DokanFileInfo ).readdir( FileName, readdir_callback ) )
        return ERROR_SUCCESS;
      else
        return -1 * ::GetLastError();
    }

    static int DOKAN_CALLBACK
    FlushFileBuffers(
	    LPCWSTR		FileName,
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
      if ( get_file( DokanFileInfo )->datasync() )
        return ERROR_SUCCESS;
      else
        return -1 * ::GetLastError();
    }

    static int DOKAN_CALLBACK
    GetDiskFreeSpace(
	    PULONGLONG FreeBytesAvailable,
	    PULONGLONG TotalNumberOfBytes,
	    PULONGLONG TotalNumberOfFreeBytes,
	    PDOKAN_FILE_INFO DokanFileInfo )
    {
      struct statvfs stbuf;
      if ( get_volume( DokanFileInfo ).statvfs( YIELD::platform::Path(), stbuf ) )
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

    static int DOKAN_CALLBACK
    GetFileInformation(
	    LPCWSTR							FileName,
	    LPBY_HANDLE_FILE_INFORMATION	HandleFileInformation,
	    PDOKAN_FILE_INFO				DokanFileInfo )
    {
      YIELD::platform::auto_Stat stbuf = get_volume( DokanFileInfo ).stat( FileName );
      if ( stbuf != NULL )
      {
        *HandleFileInformation = *stbuf;
        return ERROR_SUCCESS;
      }
      else
        return -1 * ::GetLastError();
    }

    static int DOKAN_CALLBACK
    GetVolumeInformation(
	    LPWSTR  VolumeNameBuffer,
	    DWORD VolumeNameSize,
	    LPDWORD VolumeSerialNumber,
	    LPDWORD MaximumComponentLength,
	    LPDWORD FileSystemFlags,
	    LPWSTR FileSystemNameBuffer,
	    DWORD FileSystemNameSize,
	    PDOKAN_FILE_INFO DokanFileInfo )
    {
      YIELD::platform::Path name = get_volume( DokanFileInfo ).volname( YIELD::platform::Path( PATH_SEPARATOR_STRING ) );
      struct statvfs stbuf;
      if ( get_volume( DokanFileInfo ).statvfs( YIELD::platform::Path(), stbuf ) )
      {
        wcscpy( VolumeNameBuffer, name );
        if ( VolumeSerialNumber )
          *VolumeSerialNumber = 0;
        if ( MaximumComponentLength )
          *MaximumComponentLength = stbuf.f_namemax;
        if ( FileSystemFlags )
          *FileSystemFlags =  FILE_CASE_PRESERVED_NAMES|FILE_CASE_SENSITIVE_SEARCH|FILE_UNICODE_ON_DISK;
        wcscpy( FileSystemNameBuffer, L"yieldfs_FUSE" );

        return ERROR_SUCCESS;
      }
      else
        return -1 * ::GetLastError();
    }

    static int DOKAN_CALLBACK
    LockFile(
	    LPCWSTR				FileName,
	    LONGLONG			ByteOffset,
	    LONGLONG			Length,
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
      if ( get_file( DokanFileInfo )->setlkw( true, ByteOffset, Length ) )
        return ERROR_SUCCESS;
      else
        return -1 * ::GetLastError();
    }

    static int DOKAN_CALLBACK
    MoveFile(
	    LPCWSTR				FileName, // existing file name
	    LPCWSTR				NewFileName,
	    BOOL				ReplaceIfExisting,
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
      if ( get_volume( DokanFileInfo ).rename( FileName, NewFileName ) )
        return ERROR_SUCCESS;
      else
        return -1 * ::GetLastError();
    }

    static int DOKAN_CALLBACK
    OpenDirectory(
	    LPCWSTR					FileName,
	    PDOKAN_FILE_INFO		DokanFileInfo )
    {
      if ( wcscmp( FileName, PATH_SEPARATOR_WIDE_STRING ) == 0 )
      {
        DokanFileInfo->IsDirectory = TRUE;
        return ERROR_SUCCESS;
      }
      else
      {
        YIELD::platform::auto_Stat stbuf = get_volume( DokanFileInfo ).stat( FileName );
        if ( stbuf!= NULL )
        {
          if ( stbuf->ISDIR() )
          {
            DokanFileInfo->IsDirectory = TRUE;
            return ERROR_SUCCESS;
          }
          else
            return -1 * ERROR_FILE_NOT_FOUND;
        }
        else
          return -1 * ERROR_FILE_NOT_FOUND;
      }
    }

    static int DOKAN_CALLBACK
    ReadFile(
	    LPCWSTR				FileName,
	    LPVOID				Buffer,
	    DWORD				BufferLength,
	    LPDWORD				ReadLength,
	    LONGLONG			Offset,
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
      YIELD::platform::File* file = get_file( DokanFileInfo );

      ssize_t read_ret;

      if ( file )
        read_ret = file->read( Buffer, BufferLength, Offset );
      else
      {
        file = get_volume( DokanFileInfo ).open( FileName, O_RDONLY ).release();
        if ( file != NULL )
        {
          read_ret = file->read( Buffer, BufferLength, Offset );
          yidl::runtime::Object::decRef( *file );
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

    static int DOKAN_CALLBACK
    SetEndOfFile(
	    LPCWSTR				FileName,
	    LONGLONG			ByteOffset,
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
      YIELD::platform::File* file = get_file( DokanFileInfo );

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

    static int DOKAN_CALLBACK
    SetFileAttributes(
	    LPCWSTR				FileName,
	    DWORD				FileAttributes,
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
      if ( get_volume( DokanFileInfo ).setattr( FileName, FileAttributes ) )
        return ERROR_SUCCESS;
      else
        return -1 * ::GetLastError();
    }

    static int DOKAN_CALLBACK
    SetFileTime(
	    LPCWSTR				FileName,
	    CONST FILETIME*		CreationTime,
	    CONST FILETIME*		LastAccessTime,
	    CONST FILETIME*		LastWriteTime,
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
      if ( get_volume( DokanFileInfo ).utimens( FileName, LastAccessTime, LastWriteTime, CreationTime ) )
        return ERROR_SUCCESS;
      else
        return -1 * ::GetLastError();
    }

    static int DOKAN_CALLBACK
    UnlockFile(
	    LPCWSTR				FileName,
	    LONGLONG			ByteOffset,
	    LONGLONG			Length,
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
      if ( get_file( DokanFileInfo )->unlk( ByteOffset, Length ) )
        return ERROR_SUCCESS;
      else
        return -1 * ::GetLastError();
    }

    static int DOKAN_CALLBACK
    Unmount(
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
	    return ERROR_SUCCESS;
    }

    static int DOKAN_CALLBACK
    WriteFile(
	    LPCWSTR		FileName,
	    LPCVOID		Buffer,
	    DWORD		NumberOfBytesToWrite,
	    LPDWORD		NumberOfBytesWritten,
	    LONGLONG			Offset,
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
      YIELD::platform::File* file = get_file( DokanFileInfo );

      ssize_t write_ret;

      if ( file )
        write_ret = file->write( Buffer, NumberOfBytesToWrite, Offset );
      else
      {
        file = get_volume( DokanFileInfo ).open( FileName, O_CREAT|O_WRONLY ).release();
        if ( file != NULL )
        {
          write_ret = file->write( Buffer, NumberOfBytesToWrite, Offset );
          yidl::runtime::Object::decRef( *file );
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

  private:
    YIELD::platform::auto_Volume volume;
    uint32_t flags;


    static inline YIELD::platform::File* get_file( PDOKAN_FILE_INFO DokanFileInfo )
    {
      return reinterpret_cast<YIELD::platform::File*>( DokanFileInfo->Context );
    }

    static inline YIELD::platform::Volume& get_volume( PDOKAN_FILE_INFO DokanFileInfo )
    {
      return *reinterpret_cast<FUSEWin32*>( DokanFileInfo->DokanOptions->GlobalContext )->volume;
    }
  };
#endif
};


// metadata_caching_file.h
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).




namespace yieldfs
{
  class MetadataCachingFile : public StackableFile
  {
  public:
    bool truncate( uint64_t offset );
    ssize_t write( const void* buffer, size_t buffer_len, uint64_t offset );

  private:
    friend class MetadataCachingVolume;

    MetadataCachingFile( yidl::runtime::auto_Object<MetadataCachingVolume> parent_volume, const YIELD::platform::Path& path, YIELD::platform::auto_File underlying_file, YIELD::platform::auto_Log log = NULL )
      : StackableFile( path, underlying_file, log ), parent_volume( parent_volume )
    { }

    ~MetadataCachingFile() { }

    yidl::runtime::auto_Object<MetadataCachingVolume> parent_volume;
  };
};


// page_cache.h
#include <list>
#include <vector>


namespace yieldfs
{
  class CachedPage;


  class PageCache
  {
  public:
    PageCache( uint64_t capacity_bytes, uint32_t flush_timeout_ms, YIELD::platform::auto_Volume underlying_volume );
    ~PageCache();

    void dirty( CachedPage& page );
    yidl::runtime::auto_Object<CachedPage> find( const YIELD::platform::Path& file_path, size_t page_number );
    void flush( const YIELD::platform::Path& file_path );
    void flush( yidl::runtime::auto_Object<CachedPage> dirty_page );
    void insert( yidl::runtime::auto_Object<CachedPage> page );

  private:
    uint64_t capacity_bytes;
    uint32_t flush_timeout_ms;
    YIELD::platform::auto_Volume underlying_volume;

    std::vector<CachedPage*> dirty_pages;
    std::map< std::string, std::vector<CachedPage*> > in_use_pages;
    YIELD::platform::Mutex lock;
    std::list<CachedPage*> lru_pages;
    uint64_t size_bytes;

    class OldDirtyPageTimer;

    void flush( CachedPage& dirty_page );
  };
};


// tracing_file.h
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).




namespace yieldfs
{
  class TracingFile : public StackableFile
  {
  public:
    TracingFile( const YIELD::platform::Path& path, YIELD::platform::auto_File underlying_file, YIELD::platform::auto_Log log );

    YIELD_PLATFORM_FILE_PROTOTYPES;

  private:
    ~TracingFile() { }
  };
};


// write_back_caching_file.h
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).




namespace yieldfs
{
  class WriteBackCachingFile : public DataCachingFile
  {
  public:
    WriteBackCachingFile( PageCache& page_cache, const YIELD::platform::Path& path, YIELD::platform::auto_File underlying_file, YIELD::platform::auto_Log log = NULL );

  private:
    ~WriteBackCachingFile();
  };
};


// write_through_caching_file.h
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).




namespace yieldfs
{
  class WriteThroughCachingFile : public DataCachingFile
  {
  public:
    WriteThroughCachingFile( PageCache& page_cache, const YIELD::platform::Path& path, YIELD::platform::auto_File underlying_file, YIELD::platform::auto_Log log = NULL );

    // YIELD::platform::File
    ssize_t write( const void* buffer, size_t buffer_len, uint64_t offset );

  private:
    ~WriteThroughCachingFile() { }
  };
};


// data_caching_file.cpp
DataCachingFile::DataCachingFile( PageCache& page_cache, const YIELD::platform::Path& path, YIELD::platform::auto_File underlying_file, YIELD::platform::auto_Log log )
  : StackableFile( path, underlying_file, log ), page_cache( page_cache )
{
  pagesize = underlying_file->getpagesize();
}
DataCachingFile::~DataCachingFile()
{ }
bool DataCachingFile::datasync()
{
  page_cache.flush( get_path() );
  return underlying_file->datasync();
}
ssize_t DataCachingFile::read( void* rbuf, size_t size, uint64_t offset )
{
#ifdef _DEBUG
  if ( log != NULL )
     log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: read( rbuf, size=" << size << ", offset=" << offset << " )";
#endif
  char *rbuf_p = static_cast<char*>( rbuf ), *rbuf_end = static_cast<char*>( rbuf ) + size;
  while ( rbuf_p < rbuf_end )
  {
    size_t page_number = static_cast<size_t>( offset / pagesize );
    size_t page_offset = offset % pagesize;
    size_t copy_size = static_cast<size_t>( rbuf_end - rbuf_p );
    if ( page_offset + copy_size > pagesize )
      copy_size = pagesize - page_offset;
#ifdef _DEBUG
    if ( log != NULL )
       log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: looking up cached page " << page_number << " (offset=" << page_offset << ", copy_size=" << copy_size << ").";
#endif
    auto_CachedPage page = page_cache.find( get_path(), page_number );
    if ( page != NULL )
    {
#ifdef _DEBUG
      if ( log != NULL )
        log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: read hit on page " << page_number << " with length " << page->size() << ".";
#endif
    }
    else
    {
#ifdef _DEBUG
      if ( log != NULL )
        log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: read miss on page " << page_number << ".";
#endif
      page = new CachedPage( get_path(), page_number, pagesize );
      ssize_t read_ret = underlying_file->read( *page, page->capacity(), ( offset / pagesize ) * pagesize );
      if ( read_ret >= 0 )
      {
#ifdef _DEBUG
        if ( log != NULL )
          log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: read " << read_ret << " bytes into page " << page_number << ".";
#endif
        page->put( NULL, static_cast<size_t>( read_ret ) );
        page_cache.insert( page );
      }
      else
      {
#ifdef _DEBUG
        if ( log != NULL )
          log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: read on page " << page_number << " failed.";
#endif
        return read_ret;
      }
    }
    if ( page_offset < page->size() )
    {
      if ( copy_size <= page->size() )
      {
        memcpy_s( rbuf_p, rbuf_end - rbuf_p, static_cast<char*>( *page ) + page_offset, copy_size );
        rbuf_p += copy_size;
        offset += copy_size;
      }
      else
      {
        memcpy_s( rbuf_p, rbuf_end - rbuf_p, static_cast<char*>( *page ) + page_offset, page->size() );
        rbuf_p += page->size();
        break;
      }
    }
    else
      break;
  }
  ssize_t ret = static_cast<ssize_t>( rbuf_p - static_cast<char*>( rbuf ) );
#ifdef _DEBUG
  if ( log != NULL )
     log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: read( rbuf, " << size << ", offset ) => " << ret << ".";
#endif
  return ret;
}
bool DataCachingFile::sync()
{
  page_cache.flush( get_path() );
  return underlying_file->sync();
}
bool DataCachingFile::truncate( uint64_t offset )
{
  page_cache.flush( get_path() );
  return underlying_file->truncate( offset );
}
ssize_t DataCachingFile::write( const void* wbuf, size_t size, uint64_t offset )
{
#ifdef _DEBUG
  if ( log != NULL )
     log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: write( wbuf, size=" << size << ", offset=" << offset << " )";
#endif
  const char *wbuf_p = static_cast<const char*>( wbuf ), *wbuf_end = static_cast<const char*>( wbuf ) + size;
  while ( wbuf_p < wbuf_end )
  {
    size_t page_number = static_cast<size_t>( offset / pagesize );
    size_t page_offset = offset % pagesize;
    size_t copy_size = static_cast<size_t>( wbuf_end - wbuf_p );
    if ( page_offset + copy_size > pagesize )
      copy_size = pagesize - page_offset;
#ifdef _DEBUG
    if ( log != NULL )
       log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: looking up cached page " << page_number << " (offset=" << page_offset << ", copy_size=" << copy_size << ").";
#endif
    auto_CachedPage page = page_cache.find( path, page_number );
    if ( page != NULL ) // Replace part of a cached page
    {
#ifdef _DEBUG
      if ( log != NULL )
        log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: write hit on page " << page_number << " with length " << page->size() << ".";
#endif
    }
    else if ( copy_size == pagesize ) // Write a whole new page
    {
      page = new CachedPage( get_path(), page_number, pagesize );
      page_cache.insert( page );
    }
    else // Write part of a new page
    {
#ifdef _DEBUG
      if ( log != NULL )
        log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: write miss on page " << page_number << ".";
#endif
      page = new CachedPage( get_path(), page_number, pagesize );
      ssize_t read_ret = underlying_file->read( *page, page->capacity(), ( offset / pagesize ) * pagesize );
      if ( read_ret >= 0 )
      {
#ifdef _DEBUG
        if ( log != NULL )
          log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: read " << read_ret << " bytes into page " << page_number << ".";
#endif
        page->put( NULL, static_cast<size_t>( read_ret ) );
        page_cache.insert( page );
      }
//      else
//      {
//#ifdef _DEBUG
//        if ( log != NULL )
//          log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: read on page " << page_number << " failed.";
//#endif
//        return read_ret;
//      }
    }
    memcpy_s( static_cast<char*>( *page ) + page_offset, page->capacity() - page_offset, wbuf_p, copy_size );
    if ( page->size() < page_offset + copy_size )
      page->put( NULL, page_offset + copy_size - page->size() );
    page_cache.dirty( *page );
    wbuf_p += copy_size;
    offset += copy_size;
  }
  ssize_t ret = static_cast<ssize_t>( wbuf_p - static_cast<const char*>( wbuf ) );
#ifdef _DEBUG
  if ( log != NULL )
     log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::DataCachingFile: write( wbuf, " << size << ", offset ) => " << ret << ".";
#endif
  return ret;
}


// fuse.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
#ifdef _WIN32
#else
#endif
#ifndef _WIN32
bool FUSE::is_running = false;
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
FUSE::FUSE( YIELD::platform::auto_Volume volume, uint32_t flags )
{
#ifdef _WIN32
  fuse_win32 = new FUSEWin32( volume, flags );
#else
  fuse_unix = new FUSEUnix( volume, flags );
#endif
}
FUSE::~FUSE()
{
#ifdef _WIN32
  delete fuse_win32;
#else
  delete fuse_unix;
#endif
}
#ifdef _WIN32
int FUSE::main( const char* mount_point )
{
  return fuse_win32->main( mount_point );
}
#else
int FUSE::main( char* argv0, const char* mount_point )
{
  is_running = true;
  return fuse_unix->main( argv0, mount_point );
}
int FUSE::main( struct fuse_args& fuse_args_, const char* mount_point )
{
  is_running = true;
  return fuse_unix->main( fuse_args_, mount_point );
}
#endif


// metadata_caching_file.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
bool MetadataCachingFile::truncate( uint64_t offset )
{
  if ( underlying_file->truncate( offset ) )
  {
    parent_volume->updateCachedFileSize( get_path(), underlying_file->get_size() );
    return true;
  }
  else
    return false;
}
ssize_t MetadataCachingFile::write( const void* buffer, size_t buffer_len, uint64_t offset )
{
  ssize_t write_ret = underlying_file->write( buffer, buffer_len, offset );
  if ( write_ret >= 0 )
    parent_volume->updateCachedFileSize( get_path(), underlying_file->get_size() );
  return write_ret;
}


// metadata_caching_volume.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
namespace yieldfs
{
  class MetadataCachingVolumereaddirCallback : public YIELD::platform::Volume::listdirCallback, public YIELD::platform::Volume::readdirCallback
  {
  public:
    MetadataCachingVolumereaddirCallback( MetadataCachingVolume& volume, YIELD::platform::Volume::readdirCallback& copy_to_readdir_callback )
      : volume( volume ), copy_to_readdir_callback( copy_to_readdir_callback )
    { }
    ~MetadataCachingVolumereaddirCallback()
    {
      for ( std::vector<CachedStat*>::iterator cached_stat_i = cached_stats.begin(); cached_stat_i != cached_stats.end(); cached_stat_i++ )
        yidl::runtime::Object::decRef( **cached_stat_i );
    }
    MetadataCachingVolumereaddirCallback& operator=( const MetadataCachingVolumereaddirCallback& ) { return *this; }
    void flush()
    {
      for ( std::vector<CachedStat*>::iterator cached_stat_i = cached_stats.begin(); cached_stat_i != cached_stats.end(); cached_stat_i++ )
        copy_to_readdir_callback( ( *cached_stat_i )->get_path(), ( *cached_stat_i )->get_stat() );
    }
    // YIELD::platform::Volume::listdirCallback
    bool operator()( const YIELD::platform::Path& path )
    {
      CachedStat* cached_stat = static_cast<CachedStat*>( volume.find( path ).release() );
      if ( cached_stat != NULL )
      {
        cached_stats.push_back( cached_stat );
        return true;
      }
      else
        return false;
    }
    // YIELD::platform::Volume::readdirCallback
    bool operator()( const YIELD::platform::Path& path, YIELD::platform::auto_Stat stbuf )
    {
      CachedStat* cached_stat = static_cast<CachedStat*>( volume.find( path ).release() );
      if ( cached_stat == NULL )
        volume.insert( new CachedStat( path, stbuf ) );
      return copy_to_readdir_callback( path, stbuf );
    }
  private:
    MetadataCachingVolume& volume;
    YIELD::platform::Volume::readdirCallback& copy_to_readdir_callback;
    std::vector<CachedStat*> cached_stats;
  };
};
MetadataCachingVolume::MetadataCachingVolume()
: ttl_s( 5 )
{ }
MetadataCachingVolume::MetadataCachingVolume( YIELD::platform::auto_Volume underlying_volume, double ttl_s )
: StackableVolume( underlying_volume ), ttl_s( ttl_s )
{ }
MetadataCachingVolume::MetadataCachingVolume( YIELD::platform::auto_Volume underlying_volume, YIELD::platform::auto_Log log, double ttl_s )
: StackableVolume( underlying_volume, log ), ttl_s( ttl_s )
{ }
MetadataCachingVolume::~MetadataCachingVolume()
{
//  for ( YIELD::HashMap<CachedStat*>::iterator directory_entry_i = directory_entry_cache.begin(); directory_entry_i != directory_entry_cache.end(); directory_entry_i++ )
//    yidl::runtime::Object::decRef( directory_entry_i->second );
}
bool MetadataCachingVolume::chmod( const YIELD::platform::Path& path, mode_t mode )
{
  evict( path );
  return underlying_volume->chmod( path, mode );
}
bool MetadataCachingVolume::chown( const YIELD::platform::Path& path, int32_t uid, int32_t gid )
{
  evict( path );
  return underlying_volume->chown( path, uid, gid );
}
yidl::runtime::auto_Object<CachedStat> MetadataCachingVolume::evict( const YIELD::platform::Path& path )
{
  lock.acquire();
  CachedStat* cached_stat;
  // cached_stat = YIELD::HATTrie<CachedStat*>::erase( static_cast<const std::string&>( path ) );
  iterator cached_stat_i = std::map<std::string, CachedStat*>::find( static_cast<const std::string&>( path ) );
  if ( cached_stat_i != end() )
  {
    cached_stat = cached_stat_i->second;
    erase( cached_stat_i );
  }
  else
    cached_stat = NULL;
  lock.release();
#ifdef _DEBUG
  if ( cached_stat && log != NULL )
      log->getStream( YIELD::platform::Log::LOG_INFO ) << "MetadataCachingVolume: evicted " << path;
#endif
  return cached_stat;
}
yidl::runtime::auto_Object<CachedStat> MetadataCachingVolume::find( const YIELD::platform::Path& path )
{
  lock.acquire();
  //CachedStat* cached_stat = YIELD::HATTrie<CachedStat*>::find( static_cast<const std::string&>( path ) );
  //if ( cached_stat != NULL )
  const_iterator cached_stat_i = std::map<std::string, CachedStat*>::find( static_cast<const std::string&>( path ) );
  if ( cached_stat_i != end() )
  {
    CachedStat* cached_stat = cached_stat_i->second;
    if ( YIELD::platform::Time::getCurrentUnixTimeS() - cached_stat->get_creation_epoch_time_s() < ttl_s )
    {
      cached_stat->incRef(); // Must incRef before releasing the lock in case another thread wants to erase this entry
      lock.release();
#ifdef _DEBUG
      if ( log != NULL )
        log->getStream( YIELD::platform::Log::LOG_INFO ) << "MetadataCachingVolume: hit " << path << ".";
#endif
      return cached_stat;
    }
    else
    {
      lock.release();
      evict( path );
      /*
      if ( cached_stat->ISDIR() )
        evicttree( path );
      else
        evict( path );
        */
      return NULL;
    }
  }
  else
  {
    lock.release();
#ifdef _DEBUG
    if ( log != NULL )
      log->getStream( YIELD::platform::Log::LOG_INFO ) << "MetadataCachingVolume: miss " << path << ".";
#endif
    return NULL;
  }
}
YIELD::platform::Path MetadataCachingVolume::getParentDirectoryPath( const YIELD::platform::Path& path )
{
  if ( path != PATH_SEPARATOR_STRING )
  {
    std::vector<YIELD::platform::Path> path_parts;
    path.split_all( path_parts );
    if ( path_parts.size() > 1 )
      return path_parts[path_parts.size()-2];
    else
      return YIELD::platform::Path( PATH_SEPARATOR_STRING );
  }
  else
    return YIELD::platform::Path( PATH_SEPARATOR_STRING );
}
void MetadataCachingVolume::insert( CachedStat* cached_stat )
{
  lock.acquire();
  // YIELD::HATTrie<CachedStat*>::insert( static_cast<const std::string&>( cached_stat->get_path() ), &cached_stat->incRef() );
  const_iterator cached_stat_i = std::map<std::string, CachedStat*>::find( static_cast<const std::string&>( cached_stat->get_path() ) );
  if ( cached_stat_i == end() )
  {
#ifdef _DEBUG
    if ( log != NULL )
      log->getStream( YIELD::platform::Log::LOG_INFO ) << "MetadataCachingVolume: caching " << cached_stat->get_path() << ".";
#endif
    std::map<std::string, CachedStat*>::insert( std::make_pair( static_cast<const std::string&>( cached_stat->get_path() ), &cached_stat->incRef() ) );
  }
  else
  {
    if ( log != NULL )
      log->getStream( YIELD::platform::Log::LOG_WARNING ) << "MetadataCachingVolume::insert already have " << cached_stat->get_path() << " in cache. Race condition?";
  }
  lock.release();
}
bool MetadataCachingVolume::link( const YIELD::platform::Path& old_path, const YIELD::platform::Path& new_path )
{
  evict( new_path );
  evict( old_path );
  return underlying_volume->link( old_path, new_path );
}
bool MetadataCachingVolume::mkdir( const YIELD::platform::Path& path, mode_t mode )
{
  evict( getParentDirectoryPath( path ) );
  return underlying_volume->mkdir( path, mode );
}
YIELD::platform::auto_File MetadataCachingVolume::open( const YIELD::platform::Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
{
  YIELD::platform::auto_File file = underlying_volume->open( path, flags, mode, attributes );
  if ( file != NULL )
  {
    if ( ( flags & O_CREAT ) == O_CREAT )
      evict( getParentDirectoryPath( path ) );
    evict( path );
    return new MetadataCachingFile( incRef(), path, file, log );
  }
  else
    return NULL;
}
bool MetadataCachingVolume::readdir( const YIELD::platform::Path& path, const YIELD::platform::Path& match_file_name_prefix, YIELD::platform::Volume::readdirCallback& callback )
{
  MetadataCachingVolumereaddirCallback ttl_cached_readdir_callback( *this, callback );
  // Do a listdir first to gather CachedStats for the directory
  // This is an optimization for the common case of all of the children of a directory being in cache at once
  if ( underlying_volume->listdir( path, match_file_name_prefix, ttl_cached_readdir_callback ) )
  {
    // All of the child directory entries were in the cache
    ttl_cached_readdir_callback.flush(); // Copy them to the readdir callback
    return true;
  }
  else // One of the child directory entries wasn't in the cache; do a real readdir and cache everything it receives
    return underlying_volume->readdir( path, match_file_name_prefix, ttl_cached_readdir_callback );
}
bool MetadataCachingVolume::removexattr( const YIELD::platform::Path& path, const std::string& name )
{
  evict( path );
  return underlying_volume->removexattr( path, name );
}
bool MetadataCachingVolume::rename( const YIELD::platform::Path& from_path, const YIELD::platform::Path& to_path )
{
//  evicttree( getParentDirectoryPath( from_path ) );
//  evicttree( getParentDirectoryPath( to_path ) );
  evict( from_path );
  evict( getParentDirectoryPath( from_path ) );
  evict( to_path );
  evict( getParentDirectoryPath( to_path ) );
  return underlying_volume->rename( from_path, to_path );
}
bool MetadataCachingVolume::rmdir( const YIELD::platform::Path& path )
{
  evict( path );
  return underlying_volume->rmdir( path );
}
bool MetadataCachingVolume::setattr( const YIELD::platform::Path& path, uint32_t file_attributes )
{
  evict( path );
  return underlying_volume->setattr( path, file_attributes );
}
bool MetadataCachingVolume::setxattr( const YIELD::platform::Path& path, const std::string& name, const std::string& value, int32_t flags )
{
  evict( path );
  return underlying_volume->setxattr( path, name, value, flags );
}
YIELD::platform::auto_Stat MetadataCachingVolume::stat( const YIELD::platform::Path& path )
{
  yidl::runtime::auto_Object<CachedStat> cached_stat = find( path );
  if ( cached_stat != NULL )
    return static_cast<CachedStat*>( cached_stat.get() )->get_stat();
  else
  {
    YIELD::platform::auto_Stat stbuf = underlying_volume->stat( path );
    if ( stbuf != NULL )
    {
      insert( new CachedStat( path, stbuf ) );
      return stbuf;
    }
    else
      return NULL;
  }
}
bool MetadataCachingVolume::symlink( const YIELD::platform::Path& to_path, const YIELD::platform::Path& from_path )
{
  evict( from_path );
  evict( getParentDirectoryPath( from_path ) );
  return underlying_volume->symlink( to_path, from_path );
}
bool MetadataCachingVolume::truncate( const YIELD::platform::Path& path, uint64_t new_size )
{
  evict( path );
  return underlying_volume->truncate( path, new_size );
}
bool MetadataCachingVolume::unlink( const YIELD::platform::Path& path )
{
  evict( path );
  evict( getParentDirectoryPath( path ) );
  return underlying_volume->unlink( path );
}
void MetadataCachingVolume::updateCachedFileSize( const YIELD::platform::Path& path, uint64_t new_file_size )
{
  lock.acquire();
//  CachedStat* cached_stat = YIELD::HATTrie<CachedStat*>::find( static_cast<const std::string&>( path ) );
//  if ( cached_stat )
//    cached_stat->get_stat()->set_size( new_file_size );
  const_iterator cached_stat_i = std::map<std::string, CachedStat*>::find( static_cast<const std::string&>( path ) );
  if ( cached_stat_i != end() )
    cached_stat_i->second->get_stat()->set_size( new_file_size );
  lock.release();
}
bool MetadataCachingVolume::utimens( const YIELD::platform::Path& path, const YIELD::platform::Time& atime, const YIELD::platform::Time& mtime, const YIELD::platform::Time& ctime )
{
  evict( path );
  return underlying_volume->utimens( path, atime, mtime, ctime );
}


// page_cache.cpp
class PageCache::OldDirtyPageTimer : public YIELD::platform::TimerQueue::Timer
{
public:
  OldDirtyPageTimer( const YIELD::platform::Time& timeout, auto_CachedPage dirty_page, PageCache& page_cache )
    : YIELD::platform::TimerQueue::Timer( timeout ),
      dirty_page( dirty_page ), page_cache( page_cache )
  { }
  OldDirtyPageTimer& operator=( const OldDirtyPageTimer& ) { return *this; }
  bool fire( const YIELD::platform::Time& )
  {
    page_cache.flush( dirty_page );
    return true;
  }
private:
  auto_CachedPage dirty_page;
  PageCache& page_cache;
};
PageCache::PageCache( uint64_t capacity_bytes, uint32_t flush_timeout_ms, YIELD::platform::auto_Volume underlying_volume )
  : capacity_bytes( capacity_bytes ), flush_timeout_ms( flush_timeout_ms ), underlying_volume( underlying_volume )
{
  size_bytes = 0;
}
PageCache::~PageCache()
{
  lock.acquire();
  // Flush any dirty pages
  for ( std::vector<CachedPage*>::iterator dirty_page_i = dirty_pages.begin(); dirty_page_i != dirty_pages.end(); ++dirty_page_i )
    flush( **dirty_page_i );
  // Release the references held in in_use_pages
  for ( std::map< std::string, std::vector<CachedPage*> >::iterator in_use_pages_i = in_use_pages.begin(); in_use_pages_i != in_use_pages.end(); ++in_use_pages_i )
    for ( std::vector<CachedPage*>::iterator page_i = in_use_pages_i->second.begin(); page_i != in_use_pages_i->second.end(); page_i++ )
      yidl::runtime::Object::decRef( *page_i );
  lock.release();
}
void PageCache::dirty( CachedPage& page )
{
  lock.acquire();
  if ( !page.get_dirty_bit() )
  {
    page.set_dirty_bit();
    dirty_pages.push_back( &page.incRef() );
    YIELD::platform::TimerQueue::getDefaultTimerQueue().addTimer( new OldDirtyPageTimer( flush_timeout_ms * NS_IN_MS, page.incRef(), *this ) );
  }
  lock.release();
}
auto_CachedPage PageCache::find( const YIELD::platform::Path& file_path, size_t page_number )
{
  lock.acquire();
  CachedPage* page;
  std::map<std::string, std::vector<CachedPage*> >::iterator file_in_use_pages = in_use_pages.find( file_path );
  if ( file_in_use_pages != in_use_pages.end() )
  {
    if ( page_number < file_in_use_pages->second.size() )
    {
      page = file_in_use_pages->second[page_number];
      if ( page != NULL )
      {
        for ( std::list<CachedPage*>::iterator lru_page_i = lru_pages.begin(); lru_page_i != lru_pages.end(); ++lru_page_i )
        {
          if ( **lru_page_i == *page )
          {
            lru_pages.erase( lru_page_i );
            lru_pages.insert( lru_pages.end(), page );
            break;
          }
        }
      }
    }
    else
      page = NULL;
  }
  else
    page = NULL;
  lock.release();
  return yidl::runtime::Object::incRef( page );
}
void PageCache::flush( const YIELD::platform::Path& file_path )
{
  lock.acquire();
  for ( std::vector<CachedPage*>::iterator dirty_page_i = dirty_pages.begin(); dirty_page_i != dirty_pages.end(); )
  {
    if ( ( *dirty_page_i )->get_file_path() == file_path )
    {
      flush( **dirty_page_i );
      dirty_page_i = dirty_pages.erase( dirty_page_i );
    }
    else
       ++dirty_page_i;
  }
  lock.release();
}
void PageCache::flush( auto_CachedPage dirty_page )
{
  lock.acquire();
  if ( dirty_page->get_dirty_bit() )
  {
    for ( std::vector<CachedPage*>::iterator dirty_page_i = dirty_pages.begin(); dirty_page_i != dirty_pages.end(); ++dirty_page_i )
    {
      if ( **dirty_page_i == *dirty_page )
      {
        dirty_pages.erase( dirty_page_i );
        flush( *dirty_page );
        break;
      }
    }
    // Leave the page in in_use_pages and lru_pages
  }
  lock.release();
}
void PageCache::flush( CachedPage& dirty_page )
{
  if ( dirty_page.get_dirty_bit() )
  {
    YIELD::platform::auto_File file = underlying_volume->open( dirty_page.get_file_path(), O_CREAT|O_WRONLY );
    if ( file != NULL )
    {
      ssize_t write_ret = file->write( dirty_page, dirty_page.size(), dirty_page.get_number() * dirty_page.capacity() );
      if ( write_ret < 0 )
        DebugBreak();
    }
    else
      DebugBreak();
    dirty_page.clear_dirty_bit();
  }
}
void PageCache::insert( auto_CachedPage page )
{
  lock.acquire();
#ifdef _DEBUG
  if ( page->capacity() > capacity_bytes ) DebugBreak();
#endif
  while ( size_bytes + page->capacity() > capacity_bytes )
  {
    CachedPage* evict_page = *lru_pages.begin();
    lru_pages.erase( lru_pages.begin() );
    if ( evict_page->get_dirty_bit() )
      flush( *evict_page );
    in_use_pages.find( evict_page->get_file_path() )->second[evict_page->get_number()] = NULL;
    size_bytes -= evict_page->capacity();
    yidl::runtime::Object::decRef( *evict_page );
  }
  std::map< std::string, std::vector<CachedPage*> >::iterator in_use_pages_i = in_use_pages.find( page->get_file_path() );
  if ( in_use_pages_i != in_use_pages.end() )
  {
    std::vector<CachedPage*>& file_in_use_pages = in_use_pages_i->second;
    if ( file_in_use_pages.size() <= page->get_number() )
      file_in_use_pages.resize( page->get_number() + 1 );
    file_in_use_pages[page->get_number()] = &page->incRef();
  }
  else
  {
    std::vector<CachedPage*> file_in_use_pages( page->get_number() + 1 );
    file_in_use_pages[page->get_number()] = &page->incRef();
    in_use_pages[page->get_file_path()] = file_in_use_pages;
  }
  lru_pages.insert( lru_pages.end(), page.get() );
  size_bytes += page->capacity();
  lock.release();
}


// stackable_file.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
bool StackableFile::close()
{
  return underlying_file->close();
}
bool StackableFile::datasync()
{
  return underlying_file->datasync();
}
bool StackableFile::getlk( bool exclusive, uint64_t offset, uint64_t length )
{
  return underlying_file->getlk( exclusive, offset, length );
}
bool StackableFile::getxattr( const std::string& name, std::string& out_value )
{
  return underlying_file->getxattr( name, out_value );
}
bool StackableFile::listxattr( std::vector<std::string>& out_names )
{
  return underlying_file->listxattr( out_names );
}
ssize_t StackableFile::read( void* buffer, size_t buffer_len, uint64_t offset )
{
  return underlying_file->read( buffer, buffer_len, offset );
}
bool StackableFile::removexattr( const std::string& name )
{
  return underlying_file->removexattr( name );
}
bool StackableFile::setlk( bool exclusive, uint64_t offset, uint64_t length )
{
  return underlying_file->setlk( exclusive, offset, length );
}
bool StackableFile::setlkw( bool exclusive, uint64_t offset, uint64_t length )
{
  return underlying_file->setlkw( exclusive, offset, length );
}
bool StackableFile::setxattr( const std::string& name, const std::string& value, int flags )
{
  return underlying_file->setxattr( name, value, flags );
}
YIELD::platform::auto_Stat StackableFile::stat()
{
  return underlying_file->stat();
}
bool StackableFile::sync()
{
  return underlying_file->sync();
}
bool StackableFile::truncate( uint64_t offset )
{
  return underlying_file->truncate( offset );
}
bool StackableFile::unlk( uint64_t offset, uint64_t length )
{
  return underlying_file->unlk( offset, length );
}
ssize_t StackableFile::write( const void* buffer, size_t buffer_len, uint64_t offset )
{
  return underlying_file->write( buffer, buffer_len, offset );
}


// stackable_volume.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
bool StackableVolume::access( const YIELD::platform::Path& path, int amode )
{
  return underlying_volume->access( path, amode );
}
bool StackableVolume::chmod( const YIELD::platform::Path& path, mode_t mode )
{
  return underlying_volume->chmod( path, mode );
}
bool StackableVolume::chown( const YIELD::platform::Path& path, int32_t uid, int32_t gid )
{
  return underlying_volume->chown( path, uid, gid );
}
bool StackableVolume::getxattr( const YIELD::platform::Path& path, const std::string& name, std::string& out_value )
{
  return underlying_volume->getxattr( path, name, out_value );
}
bool StackableVolume::link( const YIELD::platform::Path& old_path, const YIELD::platform::Path& new_path )
{
  return underlying_volume->link( old_path, new_path );
}
bool StackableVolume::listxattr( const YIELD::platform::Path& path, std::vector<std::string>& out_names )
{
  return underlying_volume->listxattr( path, out_names );
}
bool StackableVolume::mkdir( const YIELD::platform::Path& path, mode_t mode )
{
  return underlying_volume->mkdir( path, mode );
}
YIELD::platform::auto_File StackableVolume::open( const YIELD::platform::Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
{
  return underlying_volume->open( path, flags, mode, attributes );
}
bool StackableVolume::readdir( const YIELD::platform::Path& path, const YIELD::platform::Path& match_file_name_prefix, YIELD::platform::Volume::readdirCallback& callback )
{
  return underlying_volume->readdir( path, match_file_name_prefix, callback );
}
YIELD::platform::auto_Path StackableVolume::readlink( const YIELD::platform::Path& path )
{
  return underlying_volume->readlink( path );
}
bool StackableVolume::removexattr( const YIELD::platform::Path& path, const std::string& name )
{
  return underlying_volume->removexattr( path, name );
}
bool StackableVolume::rename( const YIELD::platform::Path& from_path, const YIELD::platform::Path& to_path )
{
  return underlying_volume->rename( from_path, to_path );
}
bool StackableVolume::rmdir( const YIELD::platform::Path& path )
{
  return underlying_volume->rmdir( path );
}
bool StackableVolume::setattr( const YIELD::platform::Path& path, uint32_t file_attributes )
{
  return underlying_volume->setattr( path, file_attributes );
}
bool StackableVolume::setxattr( const YIELD::platform::Path& path, const std::string& name, const std::string& value, int flags )
{
  return underlying_volume->setxattr( path, name, value, flags );
}
YIELD::platform::auto_Stat StackableVolume::stat( const YIELD::platform::Path& path )
{
  return underlying_volume->stat( path );
}
bool StackableVolume::statvfs( const YIELD::platform::Path& path, struct statvfs& stvfsbuf )
{
  return underlying_volume->statvfs( path, stvfsbuf );
}
bool StackableVolume::symlink( const YIELD::platform::Path& old_path, const YIELD::platform::Path& new_path )
{
  return underlying_volume->symlink( old_path, new_path );
}
bool StackableVolume::truncate( const YIELD::platform::Path& path, uint64_t new_size )
{
  return underlying_volume->truncate( path, new_size );
}
bool StackableVolume::unlink( const YIELD::platform::Path& path )
{
  return underlying_volume->unlink( path );
}
bool StackableVolume::utimens( const YIELD::platform::Path& path, const YIELD::platform::Time& atime, const YIELD::platform::Time& mtime, const YIELD::platform::Time& ctime )
{
  return underlying_volume->utimens( path, atime, mtime, ctime );
}
YIELD::platform::Path StackableVolume::volname( const YIELD::platform::Path& path )
{
  return underlying_volume->volname( path );
}


// tracing_file.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
#include <iostream>
TracingFile::TracingFile( const YIELD::platform::Path& path, YIELD::platform::auto_File underlying_file, YIELD::platform::auto_Log log )
  : StackableFile( path, underlying_file, log )
{
  if ( this->log == NULL )
    this->log = YIELD::platform::Log::open( std::cout, YIELD::platform::Log::LOG_INFO );
}
bool TracingFile::close()
{
  return TracingVolume::trace( log, "yieldfs::TracingFile::close", path, underlying_file->close() );
}
bool TracingFile::datasync()
{
  return TracingVolume::trace( log, "yieldfs::TracingFile::datasync", path, underlying_file->datasync() );
}
bool TracingFile::getlk( bool exclusive, uint64_t offset, uint64_t length )
{
  return TracingVolume::trace( log, "yieldfs::TracingFile::getlk", path, offset, length, underlying_file->getlk( exclusive, offset, length ) );
}
bool TracingFile::getxattr( const std::string& name, std::string& out_value )
{
  return TracingVolume::trace( log, "yieldfs::TracingFile::getxattr", path, name, underlying_file->getxattr( name, out_value ) );
}
bool TracingFile::listxattr( std::vector<std::string>& out_names )
{
  return TracingVolume::trace( log, "yieldfs::TracingFile::listxattr", path, underlying_file->listxattr( out_names ) );
}
ssize_t TracingFile::read( void* rbuf, size_t size, uint64_t offset )
{
  ssize_t read_ret = underlying_file->read( rbuf, size, offset );
  TracingVolume::trace( log, "yieldfs::TracingFile::read", path, size, offset, read_ret >= 0 );
  return read_ret;
}
bool TracingFile::removexattr( const std::string& name )
{
  return TracingVolume::trace( log, "yieldfs::TracingFile::removexattr", path, name, underlying_file->removexattr( name ) );
}
bool TracingFile::setlk( bool exclusive, uint64_t offset, uint64_t length )
{
  return TracingVolume::trace( log, "yieldfs::TracingFile::setlk", path, offset, length, underlying_file->setlk( exclusive, offset, length ) );
}
bool TracingFile::setlkw( bool exclusive, uint64_t offset, uint64_t length )
{
  return TracingVolume::trace( log, "yieldfs::TracingFile::setlkw", path, offset, length, underlying_file->setlkw( exclusive, offset, length ) );
}
bool TracingFile::setxattr( const std::string& name, const std::string& value, int flags )
{
  return TracingVolume::trace( log, "yieldfs::TracingFile::setxattr", path, name, underlying_file->setxattr( name, value, flags ) );
}
YIELD::platform::auto_Stat TracingFile::stat()
{
  YIELD::platform::auto_Stat stbuf = underlying_file->stat();
  TracingVolume::trace( log, "yieldfs::TracingFile::stat", path, stbuf != NULL );
  return stbuf;
}
bool TracingFile::sync()
{
  return TracingVolume::trace( log, "yieldfs::TracingFile::sync", path, underlying_file->sync() );
}
bool TracingFile::truncate( uint64_t new_size )
{
  return TracingVolume::trace( log, "yieldfs::TracingFile::truncate", path, 0, new_size, underlying_file->truncate( new_size ) );
}
bool TracingFile::unlk( uint64_t offset, uint64_t length )
{
  return TracingVolume::trace( log, "yieldfs::TracingFile::unlk", path, offset, length, underlying_file->unlk( offset, length ) );
}
ssize_t TracingFile::write( const void* buffer, size_t buffer_len, uint64_t offset )
{
  ssize_t write_ret = underlying_file->write( buffer, buffer_len, offset );
  TracingVolume::trace( log, "yieldfs::TracingFile::write", path, buffer_len, offset, write_ret == static_cast<ssize_t>( buffer_len ) );
  return write_ret;
}


// tracing_volume.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).


#include <iostream>


namespace yieldfs
{
  class TracingVolumelistdirCallback : public YIELD::platform::Volume::listdirCallback
  {
  public:
    TracingVolumelistdirCallback( YIELD::platform::Volume::listdirCallback& user_listdir_callback, YIELD::platform::auto_Log log )
      : user_listdir_callback( user_listdir_callback ), log( log )
    { }

    TracingVolumelistdirCallback& operator=( const TracingVolumelistdirCallback& ) { return *this; }

    // YIELD::platform::Volume::listdirCallback
    bool operator()( const YIELD::platform::Path& path )
    {
      log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "TracingVolume: listdir: returning path " << path << ".";
      return user_listdir_callback( path );
    }

  private:
    YIELD::platform::Volume::listdirCallback& user_listdir_callback;
    YIELD::platform::auto_Log log;
  };


  class TracingVolumereaddirCallback : public YIELD::platform::Volume::readdirCallback
  {
  public:
    TracingVolumereaddirCallback( YIELD::platform::Volume::readdirCallback& user_readdir_callback, YIELD::platform::auto_Log log )
      : user_readdir_callback( user_readdir_callback ), log( log )
    { }

    TracingVolumereaddirCallback& operator=( const TracingVolumereaddirCallback& ) { return *this; }

    // YIELD::platform::Volume::readdirCallback
    bool operator()( const YIELD::platform::Path& path, YIELD::platform::auto_Stat stbuf )
    {
      log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "TracingVolume: readdir: returning directory entry " << path << ": " << static_cast<std::string>( *stbuf ) << ".";
      return user_readdir_callback( path, stbuf );
    }

  private:
    YIELD::platform::Volume::readdirCallback& user_readdir_callback;
    YIELD::platform::auto_Log log;
  };
};


TracingVolume::TracingVolume()
{
  log = YIELD::platform::Log::open( std::cout, YIELD::platform::Log::LOG_INFO );
}

TracingVolume::TracingVolume( YIELD::platform::auto_Volume underlying_volume )
  : StackableVolume( underlying_volume )
{
  log = YIELD::platform::Log::open( std::cout, YIELD::platform::Log::LOG_INFO );
}

TracingVolume::TracingVolume( YIELD::platform::auto_Volume underlying_volume, YIELD::platform::auto_Log log )
  : StackableVolume( underlying_volume, log )
{ }

bool TracingVolume::access( const YIELD::platform::Path& path, int amode )
{
  return trace( log, "yieldfs::TracingVolume::access", path, amode, underlying_volume->access( path, amode ) );
}

bool TracingVolume::chmod( const YIELD::platform::Path& path, mode_t mode )
{
  return trace( log, "yieldfs::TracingVolume::chmod", path, mode, underlying_volume->chmod( path, mode ) );
}

bool TracingVolume::chown( const YIELD::platform::Path& path, int32_t uid, int32_t gid )
{
  YIELD::platform::Log::Stream log_stream = log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << "yieldfs::TracingVolume::chown( " << path << ", " << uid << ", " << gid << " )";
  return trace( log_stream, underlying_volume->chown( path, uid, gid ) );
}

bool TracingVolume::exists( const YIELD::platform::Path& path )
{
  return trace( log, "yieldfs::TracingVolume::exists", path, underlying_volume->exists( path ) );
}

bool TracingVolume::getxattr( const YIELD::platform::Path& path, const std::string& name, std::string& out_value )
{
  return trace( log, "yieldfs::TracingVolume::getxattr", path, name, underlying_volume->getxattr( path, name, out_value ) );
}

bool TracingVolume::link( const YIELD::platform::Path& old_path, const YIELD::platform::Path& new_path )
{
  return trace( log, "yieldfs::TracingVolume::link", old_path, new_path, underlying_volume->link( old_path, new_path ) );
}

bool TracingVolume::listdir( const YIELD::platform::Path& path, const YIELD::platform::Path& match_file_name_prefix, listdirCallback& callback )
{
  log->getStream( YIELD::platform::Log::LOG_INFO ) << "TracingVolume: listdir( " << path << ", " << match_file_name_prefix << " )";
  TracingVolumelistdirCallback tracing_volume_listdir_callback( callback, log );
  return trace( log, "yieldfs::TracingVolume::listdir", path, underlying_volume->listdir( path, match_file_name_prefix, tracing_volume_listdir_callback ) );
}

bool TracingVolume::listxattr( const YIELD::platform::Path& path, std::vector<std::string>& out_names )
{
  return trace( log, "yieldfs::TracingVolume::listxattr", path, underlying_volume->listxattr( path, out_names ) );
}

bool TracingVolume::mkdir( const YIELD::platform::Path& path, mode_t mode )
{
  return trace( log, "yieldfs::TracingVolume::mkdir", path, mode, underlying_volume->mkdir( path, mode ) );
}

bool TracingVolume::mktree( const YIELD::platform::Path& path, mode_t mode )
{
  return trace( log, "yieldfs::TracingVolume::mktree", path, mode, underlying_volume->mktree( path, mode ) );
}

YIELD::platform::auto_File TracingVolume::open( const YIELD::platform::Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
{
  YIELD::platform::auto_File file = underlying_volume->open( path, flags, mode, attributes );
  if( file != NULL )
    file = new TracingFile( path, file, log );

  YIELD::platform::Log::Stream log_stream = log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << "yieldfs::TracingVolume::open( " << path << ", " << flags << ", " << mode << ", " << attributes << " )";
  trace( log_stream, file != NULL );

  return file;
}

bool TracingVolume::readdir( const YIELD::platform::Path& path, const YIELD::platform::Path& match_file_name_prefix, YIELD::platform::Volume::readdirCallback& callback )
{
  TracingVolumereaddirCallback tracing_volume_readdir_callback( callback, log );
  return trace( log, "yieldfs::TracingVolume::readdir", path, underlying_volume->readdir( path, match_file_name_prefix, tracing_volume_readdir_callback ) );
}

YIELD::platform::auto_Path TracingVolume::readlink( const YIELD::platform::Path& path )
{
  YIELD::platform::auto_Path link_path = underlying_volume->readlink( path );
  trace( log, "yieldfs::TracingVolume::readlink", path, link_path != NULL );
  return link_path;
}

bool TracingVolume::removexattr( const YIELD::platform::Path& path, const std::string& name )
{
  return trace( log, "yieldfs::TracingVolume::removexattr", path, name, underlying_volume->removexattr( path, name ) );
}

bool TracingVolume::rename( const YIELD::platform::Path& from_path, const YIELD::platform::Path& to_path )
{
  return trace( log, "yieldfs::TracingVolume::rename", from_path, to_path, underlying_volume->rename( from_path, to_path ) );
}

bool TracingVolume::rmdir( const YIELD::platform::Path& path )
{
  return trace( log, "yieldfs::TracingVolume::rmdir", path, underlying_volume->rmdir( path ) );
}

bool TracingVolume::rmtree( const YIELD::platform::Path& path )
{
  return trace( log, "yieldfs::TracingVolume::rmdir", path, underlying_volume->rmtree( path ) );
}

bool TracingVolume::setattr( const YIELD::platform::Path& path, uint32_t file_attributes )
{
  return trace( log, "yieldfs::TracingVolume::setattr", path, file_attributes, underlying_volume->setattr( path, file_attributes ) );
}

bool TracingVolume::setxattr( const YIELD::platform::Path& path, const std::string& name, const std::string& value, int32_t flags )
{
  return trace( log, "yieldfs::TracingVolume::setxattr", path, name, underlying_volume->setxattr( path, name, value, flags ) );
}

YIELD::platform::auto_Stat TracingVolume::stat( const YIELD::platform::Path& path )
{
  YIELD::platform::auto_Stat stbuf = underlying_volume->stat( path );
  trace( log, "yieldfs::TracingVolume::stat", path, stbuf != NULL );
  if ( stbuf != NULL )
    log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "yieldfs::TracingVolume::stat returning Stat " << static_cast<std::string>( *stbuf ) << ".";
  return stbuf;
}

bool TracingVolume::statvfs( const YIELD::platform::Path& path, struct statvfs& buf )
{
  return trace( log, "yieldfs::TracingVolume::statvfs", path, underlying_volume->statvfs( path, buf ) );
}

bool TracingVolume::symlink( const YIELD::platform::Path& to_path, const YIELD::platform::Path& from_path )
{
  return trace( log, "yieldfs::TracingVolume::symlink", to_path, from_path, underlying_volume->symlink( to_path, from_path ) );
}

bool TracingVolume::trace( YIELD::platform::auto_Log log, const char* operation_name, const YIELD::platform::Path& path, bool operation_result )
{
  YIELD::platform::Log::Stream log_stream = log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << operation_name << "( " << path << " )";
  return trace( log_stream, operation_result );
}

bool TracingVolume::trace( YIELD::platform::auto_Log log, const char* operation_name, const YIELD::platform::Path& path, mode_t mode, bool operation_result )
{
  YIELD::platform::Log::Stream log_stream = log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << operation_name << "( " << path << ", " << mode << " )";
  return trace( log_stream, operation_result );
}

bool TracingVolume::trace( YIELD::platform::auto_Log log, const char* operation_name, const YIELD::platform::Path& old_path, const YIELD::platform::Path& new_path, bool operation_result )
{
  YIELD::platform::Log::Stream log_stream = log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << operation_name << "( " << old_path << ", " << new_path << " )";
  return trace( log_stream, operation_result );
}

bool TracingVolume::trace( YIELD::platform::auto_Log log, const char* operation_name, const YIELD::platform::Path& path, const std::string& xattr_name, bool operation_result )
{
  YIELD::platform::Log::Stream log_stream = log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << operation_name << "( " << path << ", " << xattr_name << " )";
  return trace( log_stream, operation_result );
}

bool TracingVolume::trace( YIELD::platform::auto_Log log, const char* operation_name, const YIELD::platform::Path& path, uint64_t size, uint64_t offset, bool operation_result )
{
  YIELD::platform::Log::Stream log_stream = log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << operation_name << "( " << path << ", " << size << ", " << offset << " )";
  return trace( log_stream, operation_result );
}

bool TracingVolume::trace( YIELD::platform::Log::Stream& log_stream, bool operation_result )
{
  if ( operation_result )
   log_stream << " -> success.";
  else
   log_stream << " -> failed, errno = " << YIELD::platform::Exception::get_errno() << ", what = " << YIELD::platform::Exception::strerror();

  return operation_result;
}

bool TracingVolume::truncate( const YIELD::platform::Path& path, uint64_t new_size )
{
  return trace( log, "yieldfs::TracingVolume::truncate", path, 0, new_size, underlying_volume->truncate( path, new_size ) );
}

bool TracingVolume::unlink( const YIELD::platform::Path& path )
{
  return trace( log, "yieldfs::TracingVolume::unlink", path, underlying_volume->unlink( path ) );
}

bool TracingVolume::utimens( const YIELD::platform::Path& path, const YIELD::platform::Time& atime, const YIELD::platform::Time& mtime, const YIELD::platform::Time& ctime )
{
  YIELD::platform::Log::Stream log_stream = log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << "yieldfs::TracingVolume::utimens( " << path << ", " << atime << ", " << mtime << ", " << ctime << " )";
  return trace( log_stream, underlying_volume->utimens( path, atime, mtime, ctime ) );
}

YIELD::platform::Path TracingVolume::volname( const YIELD::platform::Path& path )
{
  log->getStream( YIELD::platform::Log::LOG_INFO ) << "yieldfs::TracingVolume::volname( " << path << " ) -> success.";
  return underlying_volume->volname( path );
}


// write_back_caching_file.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
WriteBackCachingFile::WriteBackCachingFile( PageCache& page_cache, const YIELD::platform::Path& path, YIELD::platform::auto_File underlying_file, YIELD::platform::auto_Log log )
  : DataCachingFile( page_cache, path, underlying_file, log )
{ }
WriteBackCachingFile::~WriteBackCachingFile()
{ }


// write_back_caching_volume.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
WriteBackCachingVolume::WriteBackCachingVolume() // For testing
{
  page_cache = new PageCache( 1 * 1024 * 1024, 5 * MS_IN_S, underlying_volume );
}
WriteBackCachingVolume::WriteBackCachingVolume( size_t cache_capacity_bytes, uint32_t cache_flush_timeout_ms, YIELD::platform::auto_Volume underlying_volume )
  : StackableVolume( underlying_volume )
{
  page_cache = new PageCache( cache_capacity_bytes, cache_flush_timeout_ms, underlying_volume );
}
WriteBackCachingVolume::WriteBackCachingVolume( size_t cache_capacity_bytes, uint32_t cache_flush_timeout_ms, YIELD::platform::auto_Volume underlying_volume, YIELD::platform::auto_Log log )
  : StackableVolume( underlying_volume, log )
{
  page_cache = new PageCache( cache_capacity_bytes, cache_flush_timeout_ms, underlying_volume );
}
WriteBackCachingVolume::~WriteBackCachingVolume()
{
  delete page_cache;
}
YIELD::platform::auto_File WriteBackCachingVolume::open( const YIELD::platform::Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
{
#ifdef __linux__
  if ( ( flags & O_DIRECT ) == O_DIRECT )
    flags ^= O_DIRECT;
#endif
  if ( ( flags & O_WRONLY ) == O_WRONLY )
  {
    flags ^= O_WRONLY;
    flags |= O_RDWR;
  }
  YIELD::platform::auto_File file = underlying_volume->open( path, flags, mode, attributes );
  if ( file != NULL )
    return new WriteBackCachingFile( *page_cache, path, file, log );
  else
    return NULL;
}


// write_through_caching_file.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
WriteThroughCachingFile::WriteThroughCachingFile( PageCache& page_cache, const YIELD::platform::Path& path, YIELD::platform::auto_File underlying_file, YIELD::platform::auto_Log log )
  : DataCachingFile( page_cache, path, underlying_file, log )
{ }
ssize_t WriteThroughCachingFile::write( const void* wbuf, size_t size, uint64_t offset )
{
  ssize_t write_ret = DataCachingFile::write( wbuf, size, offset ); // Keep written pages
  page_cache.flush( get_path() );
  return write_ret;
}


// write_through_caching_volume.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
WriteThroughCachingVolume::WriteThroughCachingVolume() // For testing
{
  page_cache = new PageCache( 1 * 1024 * 1024 * 1024, 5 * MS_IN_S, underlying_volume );
}
WriteThroughCachingVolume::WriteThroughCachingVolume( YIELD::platform::auto_Volume underlying_volume )
  : StackableVolume( underlying_volume )
{
  page_cache = new PageCache( 1 * 1024 * 1024 * 1024, 5 * MS_IN_S, underlying_volume );
}
WriteThroughCachingVolume::WriteThroughCachingVolume( YIELD::platform::auto_Volume underlying_volume, YIELD::platform::auto_Log log )
  : StackableVolume( underlying_volume, log )
{
  page_cache = new PageCache( 1 * 1024 * 1024 * 1024, 5 * MS_IN_S, underlying_volume );
}
WriteThroughCachingVolume::~WriteThroughCachingVolume()
{
  delete page_cache;
}
YIELD::platform::auto_File WriteThroughCachingVolume::open( const YIELD::platform::Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
{
  YIELD::platform::auto_File file = underlying_volume->open( path, flags, mode, attributes );
  if ( file != NULL )
    return new WriteThroughCachingFile( *page_cache, path, file, log );
  else
    return NULL;
}

