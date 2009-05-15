// Revision: 133

#include "yieldfs.h"
using namespace yieldfs;


// cached_file.h
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).




namespace yieldfs
{
  class CachedPage;


  class CachedFile : public StackableFile
  {
  public:
    CachedFile( const YIELD::Path& path, YIELD::auto_Object<YIELD::File> underlying_file, YIELD::auto_Object<YIELD::Log> log = NULL )
      : StackableFile( path, underlying_file, log )
    { }

    YIELD_FILE_PROTOTYPES;

  private:
    ~CachedFile();

    typedef YIELD::STLHashMap<CachedPage*> CachedPageMap;
    CachedPageMap cached_pages;
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
    FUSEUnix( YIELD::auto_Object<YIELD::Volume> volume, uint32_t flags, YIELD::auto_Object<YIELD::Log> log )
      : volume( volume ), flags( flags ), log( log )
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
          flush,
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
          NULL, // lock
          utimens, // utimens
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
      YIELD::auto_Object<YIELD::Stat> yield_stbuf = get_file( fi )->getattr();
      if ( yield_stbuf != NULL )
      {
        *stbuf = *yield_stbuf;
        return 0;
      }
      else
        return -1 * errno;
    }

    static int flush( const char* path, struct fuse_file_info* fi )
    {
      return get_file( fi )->flush() ? 0 : ( -1 * errno );
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
      YIELD::auto_Object<YIELD::Stat> yield_stbuf = get_volume().getattr( path );
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
      size_t bytes_read;
      if ( get_file( fi )->read( rbuf, size, offset, &bytes_read ) == YIELD::Stream::STREAM_STATUS_OK )
        return static_cast<int>( bytes_read );
      else
        return -1 * errno;
    }

    class readdirCallback : public YIELD::Volume::readdirCallback
    {
    public:
      readdirCallback( void* buf, fuse_fill_dir_t filler )
        : buf( buf ), filler( filler )
      { }

      // YIELD::Volume::readdirCallback
      bool operator()( const YIELD::Path& name, YIELD::auto_Object<YIELD::Stat> stbuf )
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
      YIELD::Volume& volume = get_volume();

      YIELD::auto_Object<YIELD::Stat> yield_stbuf = volume.getattr( path );
      if ( yield_stbuf != NULL )
      {
        struct stat stbuf = *yield_stbuf;
        filler( buf, ".", &stbuf, 0 );

        if ( strcmp( path, "/" ) != 0 )
        {
          yield_stbuf = volume.getattr( YIELD::Path( path ).split().first );
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
      YIELD::auto_Object<YIELD::Path> linkpath = get_volume().readlink( path );
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
      YIELD::Object::decRef( get_file( fi ) );
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
      return get_volume().statvfs( path, sbuf ) ? 0 : ( -1 * errno );
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
      size_t bytes_written;
      if ( get_file( fi )->write( wbuf, size, offset, &bytes_written ) == YIELD::Stream::STREAM_STATUS_OK )
        return static_cast<int>( bytes_written );
      else
        return -1 * errno;
    }

  private:
    YIELD::auto_Object<YIELD::Volume> volume;
    uint32_t flags;
    YIELD::auto_Object<YIELD::Log> log;

    static inline YIELD::File* get_file( fuse_file_info* fi )
    {
      return fi ? reinterpret_cast<YIELD::File*>( fi->fh ) : NULL;
    }

    static inline uint32_t get_flags()
    {
      return static_cast<FUSEUnix*>( fuse_get_context()->private_data )->flags;
    }

    static inline YIELD::Volume& get_volume()
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
    FUSEWin32( YIELD::auto_Object<YIELD::Volume> volume, uint32_t flags, YIELD::auto_Object<YIELD::Log> log )
      : volume( volume ), flags( flags ), log( log )
    { }

    int main( const char* mount_point )
    {
      DOKAN_OPTIONS options;
      memset( &options, 0, sizeof( options ) );
      options.DriveLetter = static_cast<const wchar_t*>( YIELD::Path( mount_point ) )[0];
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
      YIELD::Volume& volume = get_volume( DokanFileInfo );
      YIELD::File* file = NULL;

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
          file = volume.open( FileName, open_flags|O_CREAT|O_EXCL, YIELD::Volume::DEFAULT_FILE_MODE, file_attributes ).release();
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

          file = volume.open( FileName, open_flags|O_CREAT, YIELD::Volume::DEFAULT_FILE_MODE, file_attributes ).release();
        }
        break;

        case OPEN_ALWAYS: // Open an existing file or create it if it doesn't exist
        {
          file = volume.open( FileName, open_flags|O_CREAT, YIELD::Volume::DEFAULT_FILE_MODE, file_attributes ).release();
        }
        break;

        case OPEN_EXISTING: // Only open an existing file
        {
          if ( YIELD::Path( FileName ) == PATH_SEPARATOR_WIDE_STRING )
          {
            DokanFileInfo->IsDirectory = TRUE;
            return ERROR_SUCCESS;
          }
          else
          {
            YIELD::auto_Object<YIELD::Stat> stbuf = volume.getattr( FileName );
            if ( stbuf!= NULL )
            {
              if ( stbuf->ISDIR() )
              {
                DokanFileInfo->IsDirectory = TRUE;
                return ERROR_SUCCESS;
              }
              else
                file = volume.open( FileName, open_flags, YIELD::Volume::DEFAULT_FILE_MODE, file_attributes ).release();
            }
            else
              return -1 * ::GetLastError();
          }
        }
        break;

        case TRUNCATE_EXISTING: // Only open an existing file and truncate it
        {
          file = volume.open( FileName, open_flags|O_TRUNC, YIELD::Volume::DEFAULT_FILE_MODE, file_attributes ).release();
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
      YIELD::Object::decRef( get_file( DokanFileInfo ) );
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

    class readdirCallback : public YIELD::Volume::readdirCallback
    {
    public:
      readdirCallback( PFillFindData FillFindData, PDOKAN_FILE_INFO	DokanFileInfo )
        : FillFindData( FillFindData ), DokanFileInfo( DokanFileInfo )
      { }

      // YIELD::Volume::readdirCallback
      bool operator()( const YIELD::Path& path, YIELD::auto_Object<YIELD::Stat> stbuf )
      {
        WIN32_FIND_DATA find_data = *stbuf;
        wcsncpy( find_data.cFileName, path, 260 );
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
      if ( get_file( DokanFileInfo )->flush() )
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
      if ( get_volume( DokanFileInfo ).statvfs( YIELD::Path(), &stbuf ) )
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
      YIELD::auto_Object<YIELD::Stat> stbuf = get_volume( DokanFileInfo ).getattr( FileName );
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
      YIELD::Path name = get_volume( DokanFileInfo ).volname( YIELD::Path( PATH_SEPARATOR_STRING ) );
      struct statvfs stbuf;
      if ( get_volume( DokanFileInfo ).statvfs( YIELD::Path(), &stbuf ) )
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
      return ERROR_SUCCESS;
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
        YIELD::auto_Object<YIELD::Stat> stbuf = get_volume( DokanFileInfo ).getattr( FileName );
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
      YIELD::File* file = get_file( DokanFileInfo );

      size_t bytes_read;

      if ( file )
      {
        if ( file->read( Buffer, BufferLength, Offset, &bytes_read ) != YIELD::Stream::STREAM_STATUS_OK )
          return -1 * ::GetLastError();
      }
      else
      {
        file = get_volume( DokanFileInfo ).open( FileName, O_RDONLY ).release();
        if ( file != NULL )
        {
          if ( file->read( Buffer, BufferLength, Offset, &bytes_read ) == YIELD::Stream::STREAM_STATUS_OK )
            YIELD::Object::decRef( *file );
          else
          {
            YIELD::Object::decRef( *file );
            return -1 * ::GetLastError();
          }
        }
        else
          return -1 * ::GetLastError();
      }

      if ( ReadLength )
        *ReadLength = static_cast<DWORD>( bytes_read );

      return ERROR_SUCCESS;
    }

    static int DOKAN_CALLBACK
    SetEndOfFile(
	    LPCWSTR				FileName,
	    LONGLONG			ByteOffset,
	    PDOKAN_FILE_INFO	DokanFileInfo )
    {
      YIELD::File* file = get_file( DokanFileInfo );

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
      return ERROR_SUCCESS;
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
      YIELD::File* file = get_file( DokanFileInfo );

      size_t bytes_written;

      if ( file )
      {
        if ( file->write( Buffer, NumberOfBytesToWrite, Offset, &bytes_written ) != YIELD::Stream::STREAM_STATUS_OK )
          return -1 * ::GetLastError();
      }
      else
      {
        file = get_volume( DokanFileInfo ).open( FileName, O_CREAT|O_WRONLY ).release();
        if ( file != NULL )
        {
          if ( file->write( Buffer, NumberOfBytesToWrite, Offset, &bytes_written ) == YIELD::Stream::STREAM_STATUS_OK )
            YIELD::Object::decRef( *file );
          else
          {
            YIELD::Object::decRef( *file );
            return -1 * ::GetLastError();
          }
        }
        else
          return -1 * ::GetLastError();
      }

      if ( NumberOfBytesWritten )
        *NumberOfBytesWritten = static_cast<DWORD>( bytes_written );

      return ERROR_SUCCESS;
    }

  private:
    YIELD::auto_Object<YIELD::Volume> volume;
    uint32_t flags;
    YIELD::auto_Object<YIELD::Log> log;


    static inline YIELD::File* get_file( PDOKAN_FILE_INFO DokanFileInfo )
    {
      return reinterpret_cast<YIELD::File*>( DokanFileInfo->Context );
    }

    static inline YIELD::Volume& get_volume( PDOKAN_FILE_INFO DokanFileInfo )
    {
      return *reinterpret_cast<FUSEWin32*>( DokanFileInfo->DokanOptions->GlobalContext )->volume;
    }
  };
#endif
};


// tracing_file.h
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).




namespace yieldfs
{
  class TracingFile : public StackableFile
  {
  public:
    TracingFile( const YIELD::Path& path, YIELD::auto_Object<YIELD::File> underlying_file, YIELD::auto_Object<YIELD::Log> log );

    YIELD_FILE_PROTOTYPES;

  private:
    ~TracingFile() { }
  };
};


// cached_file.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
#define CACHED_PAGE_SIZE 4096
namespace yieldfs
{
  class CachedPage
  {
  public:
    CachedPage()
    {
      memset( data, 0, sizeof( data ) );
      data_len = 0;
      dirty_bit = false;
    }
    inline char* get_data() { return data; }
    inline uint16_t get_data_len() const{ return data_len; }
    inline bool get_dirty_bit() const { return dirty_bit; }
    inline void set_data_len( uint16_t data_len ) { this->data_len = data_len; }
    inline void set_dirty_bit() { this->dirty_bit = true; }
  private:
    bool dirty_bit;
    char data[CACHED_PAGE_SIZE];
    uint16_t data_len;
  };
};
CachedFile::~CachedFile()
{
  flush();
}
bool CachedFile::close()
{
  flush();
  return underlying_file->close();
}
bool CachedFile::datasync()
{
  flush();
  underlying_file->datasync();
  return true;
}
bool CachedFile::flush()
{
  if ( log != NULL )
    log->getStream( YIELD::Log::LOG_INFO ) << "CachedFile: flush().";
  for ( CachedPageMap::iterator cached_page_i = cached_pages.begin(); cached_page_i != cached_pages.end(); cached_page_i++ )
  {
    if ( cached_page_i->second->get_dirty_bit() )
    {
      underlying_file->write( cached_page_i->second->get_data(), cached_page_i->second->get_data_len(), cached_page_i->first * CACHED_PAGE_SIZE, NULL );
      if ( log != NULL )
        log->getStream( YIELD::Log::LOG_INFO ) << "CachedFile: flushing page " << cached_page_i->first << ".";
    }
    delete cached_page_i->second;
  }
  cached_pages.clear();
  underlying_file->flush();
  return true;
}
YIELD::auto_Object<YIELD::Stat> CachedFile::getattr()
{
  return underlying_file->getattr();
}
bool CachedFile::getxattr( const std::string& name, std::string& out_value )
{
  return underlying_file->getxattr( name, out_value );
}
bool CachedFile::listxattr( std::vector<std::string>& out_names )
{
  return underlying_file->listxattr( out_names );
}
YIELD::Stream::Status CachedFile::read( void* buffer, size_t buffer_len, uint64_t offset, size_t* out_bytes_read )
{
  if ( offset % CACHED_PAGE_SIZE != 0 )
    YIELD::DebugBreak();
  char* read_to_buffer_p = static_cast<char*>( buffer );
  size_t remaining_buffer_len = buffer_len;
  while ( remaining_buffer_len > 0 )
  {
    uint32_t cached_page_i = static_cast<uint32_t>( offset / CACHED_PAGE_SIZE );
    CachedPage* cached_page = cached_pages.find( cached_page_i );
    if ( cached_page != NULL )
    {
      if ( log != NULL )
        log->getStream( YIELD::Log::LOG_INFO ) << "CachedFile: read hit on page " << cached_page_i << " with length " << cached_page->get_data_len() << ".";
    }
    else
    {
      if ( log != NULL )
        log->getStream( YIELD::Log::LOG_INFO ) << "CachedFile: read miss on page " << cached_page_i << ".";
      cached_page = new CachedPage;
      size_t bytes_read;
      YIELD::Stream::Status read_status = underlying_file->read( cached_page->get_data(), CACHED_PAGE_SIZE, offset, &bytes_read );
      if ( read_status == STREAM_STATUS_OK )
      {
        if ( log != NULL )
          log->getStream( YIELD::Log::LOG_INFO ) << "CachedFile: read " << cached_page->get_data_len() << " bytes into page " << cached_page_i << ".";
        cached_page->set_data_len( static_cast<uint16_t>( bytes_read ) );
        cached_pages.insert( cached_page_i, cached_page );
      }
      else
      {
        if ( log != NULL )
          log->getStream( YIELD::Log::LOG_INFO ) << "CachedFile: read on page " << cached_page_i << " failed";
        delete cached_page;
        return read_status;
      }
    }
    if ( remaining_buffer_len > cached_page->get_data_len() )
    {
      memcpy( read_to_buffer_p, cached_page->get_data(), cached_page->get_data_len() );
      read_to_buffer_p += cached_page->get_data_len();
      if ( cached_page->get_data_len() == CACHED_PAGE_SIZE )
      {
        remaining_buffer_len -= cached_page->get_data_len();
        offset += CACHED_PAGE_SIZE;
      }
      else
        break;
    }
    else
    {
      memcpy( read_to_buffer_p, cached_page->get_data(), remaining_buffer_len );
      read_to_buffer_p += remaining_buffer_len;
      break;
    }
  }
  if ( out_bytes_read )
    *out_bytes_read = static_cast<size_t>( read_to_buffer_p - static_cast<char*>( buffer ) );
  return STREAM_STATUS_OK;
}
bool CachedFile::removexattr( const std::string& name )
{
  return underlying_file->removexattr( name );
}
bool CachedFile::setxattr( const std::string& name, const std::string& value, int flags )
{
  return underlying_file->setxattr( name, value, flags );
}
bool CachedFile::sync()
{
  flush();
  underlying_file->sync();
  return true;
}
bool CachedFile::truncate( uint64_t offset )
{
  flush();
  return underlying_file->truncate( offset );
}
YIELD::Stream::Status CachedFile::writev( const struct iovec* buffers, uint32_t buffers_count, uint64_t offset, size_t* out_bytes_written )
{
  if ( offset % CACHED_PAGE_SIZE != 0 )
    YIELD::DebugBreak();
  size_t bytes_written = 0;
  for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
  {
    char* wrote_to_buffer_p = static_cast<char*>( buffers[buffer_i].iov_base );
    size_t remaining_buffer_len = buffers[buffer_i].iov_len;
    while ( remaining_buffer_len > 0 )
    {
      uint32_t cached_page_i = static_cast<uint32_t>( offset / CACHED_PAGE_SIZE );
      CachedPage* cached_page = cached_pages.find( cached_page_i );
      if ( cached_page != NULL )
      {
        if ( log != NULL )
          log->getStream( YIELD::Log::LOG_INFO ) << "CachedFile: write hit on page " << cached_page_i << ".";
      }
      else
      {
        if ( log != NULL )
          log->getStream( YIELD::Log::LOG_INFO ) << "CachedFile: write miss on page " << cached_page_i << ".";
        cached_page = new CachedPage;
        if ( remaining_buffer_len < CACHED_PAGE_SIZE ) // The buffer is smaller than a page, so we have to read the whole page and then overwrite part of it
        {
          if ( log != NULL )
            log->getStream( YIELD::Log::LOG_INFO ) << "CachedFile: writing partial page " << cached_page_i << ", must read from underlying file system.";
          size_t bytes_read;
          YIELD::Stream::Status read_status = underlying_file->read( cached_page->get_data(), CACHED_PAGE_SIZE, offset, &bytes_read );
          if ( read_status == STREAM_STATUS_OK )
            cached_page->set_data_len( static_cast<uint16_t>( bytes_read ) );
          else
          {
            if ( log != NULL )
              log->getStream( YIELD::Log::LOG_INFO ) << "CachedFile: read on page " << cached_page_i << " failed";
            delete cached_page;
            return read_status;
          }
        }
        cached_pages.insert( cached_page_i, cached_page );
      }
      cached_page->set_dirty_bit();
      if ( remaining_buffer_len > CACHED_PAGE_SIZE )
      {
        if ( log != NULL )
          log->getStream( YIELD::Log::LOG_INFO ) << "CachedFile: filling page " << cached_page_i << ".";
        memcpy( cached_page->get_data(), wrote_to_buffer_p, CACHED_PAGE_SIZE );
        cached_page->set_data_len( CACHED_PAGE_SIZE );
        cached_pages.insert( cached_page_i, cached_page );
        wrote_to_buffer_p += CACHED_PAGE_SIZE;
        remaining_buffer_len -= CACHED_PAGE_SIZE;
        offset += CACHED_PAGE_SIZE;
        bytes_written += CACHED_PAGE_SIZE;
      }
      else
      {
        if ( log != NULL )
          log->getStream( YIELD::Log::LOG_INFO ) << "CachedFile: partially filling page " << cached_page_i << ".";
        memcpy( cached_page->get_data(), wrote_to_buffer_p, remaining_buffer_len );
        if ( remaining_buffer_len > cached_page->get_data_len() )
          cached_page->set_data_len( static_cast<uint16_t>( remaining_buffer_len ) );
        bytes_written += remaining_buffer_len;
        break;
      }
    }
  }
  if ( out_bytes_written )
    *out_bytes_written = bytes_written;
  return STREAM_STATUS_OK;
}


// file_caching_volume.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
YIELD::auto_Object<YIELD::File> FileCachingVolume::open( const YIELD::Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
{
  YIELD::auto_Object<YIELD::File> file = underlying_volume->open( path, flags, mode, attributes );
  if ( file != NULL )
    return new CachedFile( path, file, log );
  else
    return NULL;
}


// fuse.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
#ifdef _WIN32
#else
#endif
/*
#ifndef _WIN32
int FUSE::geteuid()
{
  if ( is_running )
  {
    struct fuse_context* ctx = fuse_get_context();
    if ( ctx && ctx->pid != 0 && ctx->private_data != NULL )
      return ctx->uid;
    else
      return -1;
  }
  else
    return -1;
}
int FUSE::getegid()
{
  if ( is_running )
  {
    struct fuse_context* ctx = fuse_get_context();
    if ( ctx && ctx->pid != 0 && ctx->private_data != NULL )
      return ctx->gid;
    else
      return -1;
  }
  else
    return -1;
}
#endif
*/
FUSE::FUSE( YIELD::auto_Object<YIELD::Volume> volume, uint32_t flags, YIELD::auto_Object<YIELD::Log> log )
{
#ifdef _WIN32
  fuse_win32 = new FUSEWin32( volume, flags, log );
#else
  fuse_unix = new FUSEUnix( volume, flags, log );
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
  return fuse_unix->main( argv0, mount_point );
}
int FUSE::main( struct fuse_args& fuse_args_, const char* mount_point )
{
  return fuse_unix->main( fuse_args_, mount_point );
}
#endif


// stat_caching_volume.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
namespace yieldfs
{
  class CachedStat : public YIELD::Object
  {
  public:
    CachedStat( const YIELD::Path& path, YIELD::auto_Object<YIELD::Stat> stbuf )
      : path( path ), stbuf( stbuf )
    {
      creation_epoch_time_s = YIELD::Time::getCurrentUnixTimeS();
    }
    const YIELD::Path& get_path() const { return path; }
    YIELD::auto_Object<YIELD::Stat> get_stat() const { return stbuf; }
    double get_creation_epoch_time_s() const { return creation_epoch_time_s; }
    // Object
    YIELD_OBJECT_PROTOTYPES( yieldfs::CachedStat, 2711482493 );
  private:
    YIELD::Path path;
    YIELD::auto_Object<YIELD::Stat> stbuf;
    double creation_epoch_time_s;
  };
  class StatCachingVolumereaddirCallback : public YIELD::Volume::listdirCallback, public YIELD::Volume::readdirCallback
  {
  public:
    StatCachingVolumereaddirCallback( StatCachingVolume& volume, YIELD::Volume::readdirCallback& copy_to_readdir_callback )
      : volume( volume ), copy_to_readdir_callback( copy_to_readdir_callback )
    { }
    ~StatCachingVolumereaddirCallback()
    {
      for ( std::vector<CachedStat*>::iterator cached_stat_i = cached_stats.begin(); cached_stat_i != cached_stats.end(); cached_stat_i++ )
        YIELD::Object::decRef( **cached_stat_i );
    }
    void flush()
    {
      for ( std::vector<CachedStat*>::iterator cached_stat_i = cached_stats.begin(); cached_stat_i != cached_stats.end(); cached_stat_i++ )
        copy_to_readdir_callback( ( *cached_stat_i )->get_path(), ( *cached_stat_i )->get_stat() );
    }
    // YIELD::Volume::listdirCallback
    bool operator()( const YIELD::Path& path )
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
    // YIELD::Volume::readdirCallback
    bool operator()( const YIELD::Path& path, YIELD::auto_Object<YIELD::Stat> stbuf )
    {
      CachedStat* cached_stat = static_cast<CachedStat*>( volume.find( path ).release() );
      if ( cached_stat == NULL )
        volume.insert( path, new CachedStat( path, stbuf ) );
      return copy_to_readdir_callback( path, stbuf );
    }
  private:
    StatCachingVolume& volume;
    YIELD::Volume::readdirCallback& copy_to_readdir_callback;
    std::vector<CachedStat*> cached_stats;
  };
};
StatCachingVolume::StatCachingVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume, double ttl_s )
: StackableVolume( underlying_volume ), ttl_s( ttl_s )
{ }
StatCachingVolume::StatCachingVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume, YIELD::auto_Object<YIELD::Log> log, double ttl_s )
: StackableVolume( underlying_volume, log ), ttl_s( ttl_s )
{ }
StatCachingVolume::~StatCachingVolume()
{
//  for ( YIELD::HashMap<CachedStat*>::iterator directory_entry_i = directory_entry_cache.begin(); directory_entry_i != directory_entry_cache.end(); directory_entry_i++ )
//    YIELD::Object::decRef( directory_entry_i->second );
}
void StatCachingVolume::evict( const YIELD::Path& path )
{
  lock.acquire();
  CachedStat* cached_stat = YIELD::HATTrie<CachedStat*>::erase( static_cast<const std::string&>( path ) );
  lock.release();
  if ( cached_stat )
  {
    if ( log != NULL )
      log->getStream( YIELD::Log::LOG_INFO ) << "StatCachingVolume: evicted " << path;
    YIELD::Object::decRef( *cached_stat );
  }
}
/*
void StatCachingVolume::evicttree( const YIELD::Path& path )
{
  std::vector<CachedStat*> cached_stats;
  lock.acquire();
  YIELD::HATTrie<CachedStat*>::erase_by_prefix( static_cast<const std::string&>( path ), &cached_stats );
  lock.release();
  if ( log && log->get_level() >= YIELD::Log::LOG_INFO )
  {
    for ( std::vector<CachedStat*>::iterator directory_entry_i = cached_stats.begin(); directory_entry_i != cached_stats.end(); directory_entry_i++ )
    {
      log->getStream( YIELD::Log::LOG_INFO ) << "StatCachingVolume: evicted " << ( *directory_entry_i )->get_path();
      YIELD::Object::decRef( **directory_entry_i );
    }
  }
  else
  {
    for ( std::vector<CachedStat*>::iterator directory_entry_i = cached_stats.begin(); directory_entry_i != cached_stats.end(); directory_entry_i++ )
      YIELD::Object::decRef( **directory_entry_i );
  }
}
*/
YIELD::auto_Object<> StatCachingVolume::find( const YIELD::Path& path )
{
  lock.acquire();
  CachedStat* cached_stat = YIELD::HATTrie<CachedStat*>::find( static_cast<const std::string&>( path ) );
  if ( cached_stat )
  {
    if ( YIELD::Time::getCurrentUnixTimeS() - cached_stat->get_creation_epoch_time_s() < ttl_s )
    {
      cached_stat->incRef(); // Must incRef before releasing the lock in case another thread wants to erase this entry
      lock.release();
      if ( log != NULL )
        log->getStream( YIELD::Log::LOG_INFO ) << "StatCachingVolume: hit " << path << ".";
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
    if ( log != NULL )
      log->getStream( YIELD::Log::LOG_INFO ) << "StatCachingVolume: miss " << path << ".";
    return NULL;
  }
}
void StatCachingVolume::insert( const YIELD::Path& path, CachedStat* cached_stat )
{
  lock.acquire();
  YIELD::HATTrie<CachedStat*>::insert( static_cast<const std::string&>( cached_stat->get_path() ), &cached_stat->incRef() );
  if ( log != NULL )
    log->getStream( YIELD::Log::LOG_INFO ) << "StatCachingVolume: caching " << cached_stat->get_path() << ".";
  lock.release();
}
bool StatCachingVolume::chmod( const YIELD::Path& path, mode_t mode )
{
  evict( path );
  return underlying_volume->chmod( path, mode );
}
bool StatCachingVolume::chown( const YIELD::Path& path, int32_t uid, int32_t gid )
{
  evict( path );
  return underlying_volume->chown( path, uid, gid );
}
YIELD::auto_Object<YIELD::Stat> StatCachingVolume::getattr( const YIELD::Path& path )
{
  YIELD::auto_Object<> cached_stat = find( path );
  if ( cached_stat != NULL )
    return static_cast<CachedStat*>( cached_stat.get() )->get_stat();
  else
  {
    YIELD::auto_Object<YIELD::Stat> stbuf = underlying_volume->getattr( path );
    if ( stbuf != NULL )
    {
      insert( path, new CachedStat( path, stbuf ) );
      return stbuf;
    }
    else
      return NULL;
  }
}
bool StatCachingVolume::link( const YIELD::Path& old_path, const YIELD::Path& new_path )
{
  // evicttree( new_path );
  evict( new_path );
  evict( old_path );
  return underlying_volume->link( old_path, new_path );
}
bool StatCachingVolume::mkdir( const YIELD::Path& path, mode_t mode )
{
  evict( getParentDirectoryPath( path ) );
  return underlying_volume->mkdir( path, mode );
}
YIELD::auto_Object<YIELD::File> StatCachingVolume::open( const YIELD::Path& path, uint32_t flags, mode_t mode )
{
  if ( ( flags & O_CREAT ) == O_CREAT )
    evict( getParentDirectoryPath( path ) );
  evict( path );
  return underlying_volume->open( path, flags, mode );
}
bool StatCachingVolume::readdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, YIELD::Volume::readdirCallback& callback )
{
  StatCachingVolumereaddirCallback ttl_cached_readdir_callback( *this, callback );
  if ( underlying_volume->listdir( path, match_file_name_prefix, ttl_cached_readdir_callback ) )
  {
    ttl_cached_readdir_callback.flush();
    return true;
  }
  else
    return underlying_volume->readdir( path, match_file_name_prefix, ttl_cached_readdir_callback );
}
bool StatCachingVolume::removexattr( const YIELD::Path& path, const std::string& name )
{
  evict( path );
  return underlying_volume->removexattr( path, name );
}
bool StatCachingVolume::rename( const YIELD::Path& from_path, const YIELD::Path& to_path )
{
//  evicttree( getParentDirectoryPath( from_path ) );
//  evicttree( getParentDirectoryPath( to_path ) );
  evict( from_path );
  evict( getParentDirectoryPath( from_path ) );
  evict( to_path );
  evict( getParentDirectoryPath( to_path ) );
  return underlying_volume->rename( from_path, to_path );
}
bool StatCachingVolume::rmdir( const YIELD::Path& path )
{
  evict( path );
  return underlying_volume->rmdir( path );
}
bool StatCachingVolume::setattr( const YIELD::Path& path, uint32_t file_attributes )
{
  evict( path );
  return underlying_volume->setattr( path, file_attributes );
}
bool StatCachingVolume::setxattr( const YIELD::Path& path, const std::string& name, const std::string& value, int32_t flags )
{
  evict( path );
  return underlying_volume->setxattr( path, name, value, flags );
}
bool StatCachingVolume::symlink( const YIELD::Path& to_path, const YIELD::Path& from_path )
{
  evict( from_path );
  evict( getParentDirectoryPath( from_path ) );
  return underlying_volume->symlink( to_path, from_path );
}
bool StatCachingVolume::truncate( const YIELD::Path& path, uint64_t new_size )
{
  evict( path );
  return underlying_volume->truncate( path, new_size );
}
bool StatCachingVolume::unlink( const YIELD::Path& path )
{
  evict( path );
  evict( getParentDirectoryPath( path ) );
  return underlying_volume->unlink( path );
}
bool StatCachingVolume::utimens( const YIELD::Path& path, const YIELD::Time& atime, const YIELD::Time& mtime, const YIELD::Time& ctime )
{
  evict( path );
  return underlying_volume->utimens( path, atime, mtime, ctime );
}
YIELD::Path StatCachingVolume::getParentDirectoryPath( const YIELD::Path& path )
{
  if ( path != PATH_SEPARATOR_STRING )
  {
    std::vector<YIELD::Path> path_parts;
    path.split_all( path_parts );
    if ( path_parts.size() > 1 )
      return path_parts[path_parts.size()-2];
    else
      return YIELD::Path( PATH_SEPARATOR_STRING );
  }
  else
    return YIELD::Path( PATH_SEPARATOR_STRING );
}


// tracing_file.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).
#include <iostream>
TracingFile::TracingFile( const YIELD::Path& path, YIELD::auto_Object<YIELD::File> underlying_file, YIELD::auto_Object<YIELD::Log> log )
  : StackableFile( path, underlying_file, log )
{
  if ( this->log == NULL )
    this->log = new YIELD::Log( std::cout, YIELD::Log::LOG_INFO );
}
bool TracingFile::close()
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingFile: close( " << path << " )";
  return underlying_file->close();
}
bool TracingFile::datasync()
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingFile: fdatasync( " << path << " )";
  return underlying_file->datasync();
}
bool TracingFile::flush()
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingFile: fflush( " << path << " )";
  return underlying_file->flush();
}
YIELD::auto_Object<YIELD::Stat> TracingFile::getattr()
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingFile: fgetattr( " << path << " )";
  return underlying_file->getattr();
}
bool TracingFile::getxattr( const std::string& name, std::string& out_value )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingFile: fgetxattr( " << path << ", " << name << " )";
  return underlying_file->getxattr( name, out_value );
}
bool TracingFile::listxattr( std::vector<std::string>& out_names )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingFile: flistxattr( " << path << " )";
  return underlying_file->listxattr( out_names );
}
YIELD::Stream::Status TracingFile::read( void* rbuf, size_t size, uint64_t offset, size_t* out_bytes_read )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingFile: read( " << path << ", rbuf, " << size << ", " << offset << " )";
  return underlying_file->read( rbuf, size, offset, out_bytes_read );
}
bool TracingFile::removexattr( const std::string& name )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingFile: fremovexattr( " << path << ", " << name << " )";
  return underlying_file->removexattr( name );
}
bool TracingFile::setxattr( const std::string& name, const std::string& value, int flags )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingFile: fsetxattr( " << path << ", " << name << ", " << value << ", " << flags << " )";
  return underlying_file->setxattr( name, value, flags );
}
bool TracingFile::sync()
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingFile: fsync( " << path << " )";
  return underlying_file->sync();
}
bool TracingFile::truncate( uint64_t new_size )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingFile: ftruncate( " << path << ", " << new_size << " )";
  return underlying_file->truncate( new_size );
}
YIELD::Stream::Status TracingFile::writev( const iovec* buffers, uint32_t buffers_count, uint64_t offset, size_t* out_bytes_written )
{
  if ( buffers_count == 1 )
    log->getStream( YIELD::Log::LOG_INFO ) << "TracingFile: write( " << path << ", wbuf, " << buffers[0].iov_len << ", " << offset << " )";
  else
    log->getStream( YIELD::Log::LOG_INFO ) << "TracingFile: writev( " << path << ", buffers, " << buffers_count << ", " << offset << " )";
  return underlying_file->writev( buffers, buffers_count, offset, out_bytes_written );
}


// tracing_volume.cpp
// Copyright 2009 Minor Gordon.
// This source comes from the YieldFS project. It is licensed under the New BSD license (see COPYING for terms and conditions).


#include <iostream>


namespace yieldfs
{
  class TracingVolumelistdirCallback : public YIELD::Volume::listdirCallback
  {
  public:
    TracingVolumelistdirCallback( YIELD::Volume::listdirCallback& user_listdir_callback, YIELD::auto_Object<YIELD::Log> log )
      : user_listdir_callback( user_listdir_callback ), log( log )
    { }

    // YIELD::Volume::listdirCallback
    bool operator()( const YIELD::Path& path )
    {
      log->getStream( YIELD::Log::LOG_DEBUG ) << "TracingVolume: listdir: returning path " << path << ".";
      return user_listdir_callback( path );
    }

  private:
    YIELD::Volume::listdirCallback& user_listdir_callback;
    YIELD::auto_Object<YIELD::Log> log;
  };


  class TracingVolumereaddirCallback : public YIELD::Volume::readdirCallback
  {
  public:
    TracingVolumereaddirCallback( YIELD::Volume::readdirCallback& user_readdir_callback, YIELD::auto_Object<YIELD::Log> log )
      : user_readdir_callback( user_readdir_callback ), log( log )
    { }

    // YIELD::Volume::readdirCallback
    bool operator()( const YIELD::Path& path, YIELD::auto_Object<YIELD::Stat> stbuf )
    {
      log->getStream( YIELD::Log::LOG_DEBUG ) << "TracingVolume: readdir: returning directory entry " << path << ": " << static_cast<std::string>( *stbuf ) << ".";
      return user_readdir_callback( path, stbuf );
    }

  private:
    YIELD::Volume::readdirCallback& user_readdir_callback;
    YIELD::auto_Object<YIELD::Log> log;
  };
};

TracingVolume::TracingVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume )
  : StackableVolume( underlying_volume )
{
  log = new YIELD::Log( std::cout, YIELD::Log::LOG_INFO );
}

TracingVolume::TracingVolume( YIELD::auto_Object<YIELD::Volume> underlying_volume, YIELD::auto_Object<YIELD::Log> log )
  : StackableVolume( underlying_volume, log )
{ }

bool TracingVolume::access( const YIELD::Path& path, int amode )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: access( " << path << ", " << amode << " )";
  return underlying_volume->access( path, amode );
}

bool TracingVolume::chmod( const YIELD::Path& path, mode_t mode )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: chmod( " << path << ", " << mode << " )";
  return underlying_volume->chmod( path, mode );
}

bool TracingVolume::chown( const YIELD::Path& path, int32_t uid, int32_t gid )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: chown( " << path << ", " << uid << ", " << gid << " )";
  return underlying_volume->chown( path, uid, gid );
}

bool TracingVolume::exists( const YIELD::Path& path )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: exists( " << path << " )";
  return underlying_volume->exists( path );
}

YIELD::auto_Object<YIELD::Stat> TracingVolume::getattr( const YIELD::Path& path )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: getattr( " << path << " )";
  YIELD::auto_Object<YIELD::Stat> stbuf = underlying_volume->getattr( path );
  if ( stbuf != NULL )
    log->getStream( YIELD::Log::LOG_DEBUG ) << "TracingVolume: getattr returning Stat " << static_cast<std::string>( *stbuf ) << ".";
  return stbuf;
}

bool TracingVolume::getxattr( const YIELD::Path& path, const std::string& name, std::string& out_value )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: getxattr( " << path << ", " << name << " )";
  return underlying_volume->getxattr( path, name, out_value );
}

bool TracingVolume::link( const YIELD::Path& old_path, const YIELD::Path& new_path )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: link( " << old_path << ", " << new_path << " )";
  return underlying_volume->link( old_path, new_path );
}

bool TracingVolume::listdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, listdirCallback& callback )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: listdir( " << path << ", " << match_file_name_prefix << " )";
  TracingVolumelistdirCallback tracing_volume_listdir_callback( callback, log );
  return underlying_volume->listdir( path, match_file_name_prefix, tracing_volume_listdir_callback );
}

bool TracingVolume::listxattr( const YIELD::Path& path, std::vector<std::string>& out_names )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: listxattr( " << path << " )";
  return underlying_volume->listxattr( path, out_names );
}

bool TracingVolume::mkdir( const YIELD::Path& path, mode_t mode )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: mkdir( " << path << ", " << mode << " )";
  return underlying_volume->mkdir( path, mode );
}

bool TracingVolume::mktree( const YIELD::Path& path, mode_t mode )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: mktree( " << path << ", " << mode << " )";
  return underlying_volume->mktree( path, mode );
}

YIELD::auto_Object<YIELD::File> TracingVolume::open( const YIELD::Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: open( " << path << ", " << flags << ", " << mode << ", " << attributes << " )";
  YIELD::auto_Object<YIELD::File> file = underlying_volume->open( path, flags, mode, attributes );
  if( file != NULL )
    return new TracingFile( path, file, log );
  else
    return NULL;
}

bool TracingVolume::readdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, YIELD::Volume::readdirCallback& callback )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: readdir( " << path << ", " << match_file_name_prefix << " )";
  TracingVolumereaddirCallback tracing_volume_readdir_callback( callback, log );
  return underlying_volume->readdir( path, match_file_name_prefix, tracing_volume_readdir_callback );
}

YIELD::auto_Object<YIELD::Path> TracingVolume::readlink( const YIELD::Path& path )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: readlink( " << path << " )";
  return underlying_volume->readlink( path );
}

bool TracingVolume::removexattr( const YIELD::Path& path, const std::string& name )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: removexattr( " << path << ", " << name << " )";
  return underlying_volume->removexattr( path, name );
}

bool TracingVolume::rename( const YIELD::Path& from_path, const YIELD::Path& to_path )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: rename( " << from_path << ", " << to_path << " )";
  return underlying_volume->rename( from_path, to_path );
}

bool TracingVolume::rmdir( const YIELD::Path& path )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: rmdir( " << path << " )";
  return underlying_volume->rmdir( path );
}

bool TracingVolume::rmtree( const YIELD::Path& path )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: rmtree( " << path << " )";
  return underlying_volume->rmtree( path );
}

bool TracingVolume::setattr( const YIELD::Path& path, uint32_t file_attributes )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: setattr( " << path << ", " << file_attributes << " )";
  return underlying_volume->setattr( path, file_attributes );
}

bool TracingVolume::setxattr( const YIELD::Path& path, const std::string& name, const std::string& value, int32_t flags )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: setxattr( " << path << ", " << name << ", " << value << ", " << flags << " )";
  return underlying_volume->setxattr( path, name, value, flags );
}

bool TracingVolume::statvfs( const YIELD::Path& path, struct statvfs* buf )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: statvfs( " << path << " )";
  return underlying_volume->statvfs( path, buf );
}

bool TracingVolume::symlink( const YIELD::Path& to_path, const YIELD::Path& from_path )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: symlink( " << to_path << ", " << from_path << " )";
  return underlying_volume->symlink( to_path, from_path );
}

bool TracingVolume::truncate( const YIELD::Path& path, uint64_t new_size )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: truncate( " << path << ", " << new_size << " )";
  return underlying_volume->truncate( path, new_size );
}

bool TracingVolume::unlink( const YIELD::Path& path )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: unlink( " << path << " )";
  return underlying_volume->unlink( path );
}

bool TracingVolume::utimens( const YIELD::Path& path, const YIELD::Time& atime, const YIELD::Time& mtime, const YIELD::Time& ctime )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: utimens( " << path << ", " << atime << ", " << mtime << ", " << ctime << " )";
  return underlying_volume->utimens( path, atime, mtime, ctime );
}

YIELD::Path TracingVolume::volname( const YIELD::Path& path )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "TracingVolume: volname( " << path << " )";
  return underlying_volume->volname( path );
}

