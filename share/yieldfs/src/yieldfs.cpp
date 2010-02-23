#include "yieldfs.h"
using namespace yieldfs;


// fuse_unix.h
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

    static int
    access( const char* path, int mode )
    {
      return get_volume().access( path, mode ) ? 0 : -EACCES;
    }

    static int
    chmod( const char* path, mode_t mode )
    {
      return get_volume().chmod( path, mode ) ? 0 : ( -1 * errno );
    }

    static int
    chown( const char* path, uid_t uid, gid_t gid )
    {
      return get_volume().chown( path, uid, gid ) ? 0 : ( -1 * errno );
    }

    static int
    create
    (
      const char* path,
      mode_t mode,
      struct fuse_file_info* fi
    )
    {
      uint32_t flags = O_CREAT|O_WRONLY|O_TRUNC;
#ifdef O_DIRECT
      if ( ( get_flags() & FUSE::FUSE_FLAG_DIRECT_IO ) ==
           FUSE::FUSE_FLAG_DIRECT_IO )
        flags |= O_DIRECT;
#endif

      fi->fh = reinterpret_cast<uint64_t>
      (
        get_volume().open( path, flags, mode ).release()
      );

      if ( fi->fh != 0 )
      {
        if ( ( get_flags() & FUSE::FUSE_FLAG_DIRECT_IO ) ==
             FUSE::FUSE_FLAG_DIRECT_IO )
          fi->direct_io = 1;

        return 0;
      }
      else
        return -1 * errno;
    }

    static int
    fgetattr( const char* path, struct stat *stbuf, struct fuse_file_info* fi )
    {
      YIELD::platform::auto_Stat yield_stbuf( get_file( fi )->getattr() );
      if ( yield_stbuf != NULL )
      {
        *stbuf = *yield_stbuf;
        return 0;
      }
      else
        return -1 * errno;
    }

    static int
    fsync( const char* path, int isdatasync, struct fuse_file_info* fi )
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

    static int
    ftruncate( const char* path, off_t size, struct fuse_file_info* fi )
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

    static int
    getattr( const char* path, struct stat *stbuf )
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

    static int
    getxattr( const char* path, const char* name, char *value, size_t size )
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

    static int
    link( const char* path, const char* linkpath )
    {
      return get_volume().link( path, linkpath ) ? 0 : ( -1 * errno );
    }

    static int
    listxattr( const char* path, char *list, size_t size )
    {
      std::vector<std::string> xattr_names;
      if ( get_volume().listxattr( path, xattr_names ) )
      {
        if ( list != NULL && size > 0 )
        {
          size_t list_size_consumed = 0;
          for
          (
            std::vector<std::string>::const_iterator
              xattr_name_i = xattr_names.begin();
            xattr_name_i != xattr_names.end();
            xattr_name_i++
          )
          {
            const std::string& xattr_name = *xattr_name_i;
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
            std::vector<std::string>::const_iterator
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

    static int
    lock
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

    static int
    mkdir( const char* path, mode_t mode )
    {
      return get_volume().mkdir( path, mode ) ? 0 : ( -1 * errno );
    }

    static int
    open( const char* path, struct fuse_file_info* fi )
    {
      uint32_t flags = fi->flags;
      if ( ( flags & S_IFREG ) == S_IFREG )
        flags ^= S_IFREG;

#ifdef O_DIRECT
      if ( ( flags & O_DIRECT ) == O_DIRECT )
        fi->direct_io = 1;
      else if ( ( get_flags() & FUSE::FUSE_FLAG_DIRECT_IO ) ==
                FUSE::FUSE_FLAG_DIRECT_IO )
      {
        fi->direct_io = 1;
        flags |= O_DIRECT;
      }
#else
      if ( ( get_flags() & FUSE::FUSE_FLAG_DIRECT_IO ) ==
           FUSE::FUSE_FLAG_DIRECT_IO )
        fi->direct_io = 1;
#endif

      fi->fh = reinterpret_cast<uint64_t>
               (
                 get_volume().open( path, flags ).release()
               );

      if ( fi->fh != 0 )
        return 0;
      else
        return -1 * errno;
    }

    static int
    read
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

    class readdirCallback : public YIELD::platform::Volume::readdirCallback
    {
    public:
      readdirCallback( void* buf, fuse_fill_dir_t filler )
        : buf( buf ), filler( filler )
      { }

      // YIELD::platform::Volume::readdirCallback
      bool operator()
      (
        const YIELD::platform::Path& name,
        YIELD::platform::auto_Stat stbuf
      )
      {
        struct stat struct_stat_stbuf = *stbuf;

        filler
        (
          buf,
          static_cast<const std::string&>( name ).c_str(),
          &struct_stat_stbuf, 0
        );

        return true;
      }

    private:
      void* buf;
      fuse_fill_dir_t filler;
    };

    static int
    readdir
    (
      const char* path,
      void* buf,
      fuse_fill_dir_t filler,
      off_t offset,
      struct fuse_file_info* fi
    )
    {
      YIELD::platform::Volume& volume = get_volume();

      YIELD::platform::auto_Stat yield_stbuf = volume.stat( path );
      if ( yield_stbuf != NULL )
      {
        struct stat stbuf = *yield_stbuf;
        filler( buf, ".", &stbuf, 0 );

        if ( strcmp( path, "/" ) != 0 )
        {
          yield_stbuf =
            volume.stat( YIELD::platform::Path( path ).split().first );

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

    static int
    readlink( const char* path, char *linkbuf, size_t size )
    {
      YIELD::platform::auto_Path linkpath = get_volume().readlink( path );
      if ( linkpath!= NULL )
      {
        if ( size > linkpath->size() + 1 )
          size = linkpath->size() + 1; // FUSE wants the terminating \0,
                                       // even though the readlink
                                       // system call doesn't
        memcpy
        (
          linkbuf,
          static_cast<const std::string&>( *linkpath ).c_str(),
          size
        );

        // Don't return size here,
        // FUSE disagrees with the system call about that, too
        return 0;
      }
      else
        return -1 * errno;
    }

    static int
    release( const char* path, struct fuse_file_info* fi )
    {
      // close explicitly to trigger replication in XtreemFS
      // there may be extra references to the File (e.g. timers) ->
      // the decRef may not destroy the reference ->
      // close() may not be called for a while if we don't call it here
      YIELD::platform::File* file = get_file( fi );
      if ( file != NULL )
      {
        fi->fh = 0;
        int ret = file->close() ? 0 : -1 * errno;
        YIELD::platform::File::decRef( *file );
        return ret;
      }
      else
        return 0;
    }

    static int
    rename( const char* path, const char *topath )
    {
      return get_volume().rename( path, topath ) ? 0 : ( -1 * errno );
    }

    static int
    removexattr( const char* path, const char* name )
    {
      return get_volume().removexattr( path, name ) ? 0 : ( -1 * errno );
    }

    static int rmdir( const char* path )
    {
      return get_volume().rmdir( path ) ? 0 : ( -1 * errno );
    }

    static int
    setxattr
    (
      const char* path,
      const char* name,
      const char *value,
      size_t size,
      int flags
    )
    {
      std::string value_str( value, size );
      if ( get_volume().setxattr( path, name, value_str, flags ) )
        return 0;
      else
        return -1 * errno;
    }

    static int
    statfs( const char* path, struct statvfs* sbuf )
    {
      if ( sbuf )
        return get_volume().statvfs( path, *sbuf ) ? 0 : ( -1 * errno );
      else
        return -1 * EFAULT;
    }

    static int
    symlink( const char* path, const char* linkpath )
    {
      return get_volume().symlink( path, linkpath ) ? 0 : ( -1 * errno );
    }

    static int
    truncate( const char* path, off_t size )
    {
      return get_volume().truncate( path, size ) ? 0 : ( -1 * errno );
    }

    static int
    unlink( const char* path )
    {
      return get_volume().unlink( path ) ? 0 : ( -1 * errno );
    }

    static int
    utimens( const char* path, const timespec tv[2] )
    {
      return get_volume().utime
             (
               path,
               tv[0],
               tv[1]
             ) ? 0 : ( -1 * errno );
    }

    static int
    write
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
      return *static_cast<FUSEUnix*>
             (
               fuse_get_context()->private_data
             )->volume;
    }
  };
#endif
};




// fuse_win32.h
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
      options.DriveLetter =
        static_cast<const wchar_t*>( YIELD::platform::Path( mount_point ) )[0];
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
    CreateFile
    (
      LPCWSTR FileName,
      DWORD DesiredAccess,
      DWORD, // ShareMode,
      DWORD CreationDisposition,
      DWORD FlagsAndAttributes,
      PDOKAN_FILE_INFO DokanFileInfo
    )
    {
      YIELD::platform::Volume& volume = get_volume( DokanFileInfo );
      YIELD::platform::File* file = NULL;

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


      switch ( CreationDisposition )
      {
        case CREATE_NEW: // Create the file only if it does not exist already
        {
          file = volume.open
                (
                  FileName,
                  open_flags|O_CREAT|O_EXCL,
                  YIELD::platform::File::MODE_DEFAULT,
                  file_attributes
                ).release();
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

          file = volume.open
                 (
                   FileName,
                   open_flags|O_CREAT,
                   YIELD::platform::File::MODE_DEFAULT,
                   file_attributes
                 ).release();
        }
        break;

        case OPEN_ALWAYS: // Open an existing file; create if it doesn't exist
        {
          file = volume.open
                 (
                   FileName,
                   open_flags|O_CREAT,
                   YIELD::platform::File::MODE_DEFAULT,
                   file_attributes
                 ).release();
        }
        break;

        case OPEN_EXISTING: // Only open an existing file
        {
          YIELD::platform::Path path( FileName );

          if ( path == YIELD::platform::Path::SEPARATOR )
          {
            DokanFileInfo->IsDirectory = TRUE;
            return ERROR_SUCCESS;
          }
          else
          {
            YIELD::platform::auto_Stat stbuf = volume.stat( path );
            if ( stbuf!= NULL )
            {
              if ( stbuf->ISDIR() )
              {
                DokanFileInfo->IsDirectory = TRUE;
                return ERROR_SUCCESS;
              }
              else
                file = volume.open
                       (
                         path,
                         open_flags,
                         YIELD::platform::File::MODE_DEFAULT,
                         file_attributes
                       ).release();
            }
            else
              return -1 * ::GetLastError();
          }
        }
        break;

        case TRUNCATE_EXISTING: // Only open an existing file and truncate it
        {
          file = volume.open
                 (
                   FileName,
                   open_flags|O_TRUNC,
                   YIELD::platform::File::MODE_DEFAULT,
                   file_attributes
                 ).release();

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
    CreateDirectory
    (
      LPCWSTR FileName,
      PDOKAN_FILE_INFO DokanFileInfo
    )
    {
      YIELD::platform::Path path( FileName );
      if ( path != YIELD::platform::Path::SEPARATOR )
      {
        if ( get_volume( DokanFileInfo ).mkdir( path ) )
          return ERROR_SUCCESS;
        else
          return -1 * ::GetLastError();
      }
      else
        return -1 * ERROR_ALREADY_EXISTS;
    }

    static int DOKAN_CALLBACK
    CloseFile
    (
      LPCWSTR, // FileName,
      PDOKAN_FILE_INFO DokanFileInfo
    )
    {
      // close() explicitly to trigger replication in XtreemFS
      // there may be extra references to the File (e.g. timers) ->
      // the decRef may not destroy the reference ->
      // close() may not be called for a while if we don't call it here
      YIELD::platform::File* file = get_file( DokanFileInfo );
      if ( file != NULL )
      {
        DokanFileInfo->Context = NULL;

        int ret;
        if ( file->close() )
          ret = ERROR_SUCCESS;
        else
          ret = -1 * ::GetLastError();

        YIELD::platform::File::decRef( *file );

        return ret;
      }
      else
        return ERROR_SUCCESS;
    }

    static int DOKAN_CALLBACK
    Cleanup
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

    static int DOKAN_CALLBACK
    DeleteDirectory
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

    static int DOKAN_CALLBACK
    DeleteFile
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

    static int DOKAN_CALLBACK
    FindFiles
    (
      LPCWSTR FileName,
      PFillFindData FillFindData,
      PDOKAN_FILE_INFO DokanFileInfo
    )
    {
      YIELD::platform::auto_Directory directory
        = get_volume( DokanFileInfo ).opendir( FileName );

      if ( directory != NULL )
      {
        YIELD::platform::Directory::auto_Entry dirent
          = directory->readdir();

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

          dirent = directory->readdir();
        }

        return ERROR_SUCCESS;
      }
      else
        return -1 * ::GetLastError();
    }

    static int DOKAN_CALLBACK
    FlushFileBuffers
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

    static int DOKAN_CALLBACK
    GetDiskFreeSpace
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
        get_volume( DokanFileInfo ).statvfs( YIELD::platform::Path(), stbuf )
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

    static int DOKAN_CALLBACK
    GetFileInformation
    (
      LPCWSTR FileName,
      LPBY_HANDLE_FILE_INFORMATION HandleFileInformation,
      PDOKAN_FILE_INFO DokanFileInfo
    )
    {
      YIELD::platform::auto_Stat stbuf =
        get_volume( DokanFileInfo ).stat( FileName );

      if ( stbuf != NULL )
      {
        *HandleFileInformation = *stbuf;
        return ERROR_SUCCESS;
      }
      else
        return -1 * ::GetLastError();
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
      PDOKAN_FILE_INFO DokanFileInfo )
    {
      YIELD::platform::Path name =
       get_volume( DokanFileInfo ).volname( YIELD::platform::Path::SEPARATOR );

      struct statvfs stbuf;
      if
      (
        get_volume( DokanFileInfo ).statvfs
        (
          YIELD::platform::Path(), stbuf
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

    static int DOKAN_CALLBACK
    LockFile
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

    static int DOKAN_CALLBACK
    MoveFile
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

    static int DOKAN_CALLBACK
    OpenDirectory
    (
      LPCWSTR FileName,
      PDOKAN_FILE_INFO DokanFileInfo
    )
    {
      YIELD::platform::Path path( FileName );

      if ( path == YIELD::platform::Path::SEPARATOR )
      {
        DokanFileInfo->IsDirectory = TRUE;
        return ERROR_SUCCESS;
      }
      else
      {
        YIELD::platform::auto_Stat stbuf =
            get_volume( DokanFileInfo ).stat( path );

        if ( stbuf!= NULL && stbuf->ISDIR() )
        {
          DokanFileInfo->IsDirectory = TRUE;
          return ERROR_SUCCESS;
        }
        else
          return -1 * ERROR_FILE_NOT_FOUND;
      }
    }

    static int DOKAN_CALLBACK
    ReadFile
    (
      LPCWSTR FileName,
      LPVOID Buffer,
      DWORD BufferLength,
      LPDWORD ReadLength,
      LONGLONG Offset,
      PDOKAN_FILE_INFO DokanFileInfo
    )
    {
      YIELD::platform::File* file = get_file( DokanFileInfo );

      ssize_t read_ret;

      if ( file )
        read_ret = file->read( Buffer, BufferLength, Offset );
      else
      {
        file = get_volume( DokanFileInfo ).
                open( FileName, O_RDONLY ).release();

        if ( file != NULL )
        {
          read_ret = file->read( Buffer, BufferLength, Offset );
          file->close(); // See note in CloseFile; assume this succeeds
          YIELD::platform::File::decRef( *file );
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
    SetEndOfFile
    (
      LPCWSTR FileName,
      LONGLONG ByteOffset,
      PDOKAN_FILE_INFO DokanFileInfo
    )
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
    SetFileAttributes
    (
      LPCWSTR FileName,
      DWORD FileAttributes,
      PDOKAN_FILE_INFO DokanFileInfo
    )
    {
      YIELD::platform::auto_Stat stbuf( new YIELD::platform::Stat );
      stbuf->set_attributes( FileAttributes );
      if
      (
        get_volume( DokanFileInfo ).setattr
        (
          FileName,
          stbuf,
          YIELD::platform::Volume::SETATTR_ATTRIBUTES
        )
      )
        return ERROR_SUCCESS;
      else
        return -1 * ::GetLastError();
    }

    static int DOKAN_CALLBACK
    SetFileTime
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

    static int DOKAN_CALLBACK
    UnlockFile
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

    static int DOKAN_CALLBACK
    Unmount
    (
      PDOKAN_FILE_INFO
    )
    {
      return ERROR_SUCCESS;
    }

    static int DOKAN_CALLBACK
    WriteFile
    (
      LPCWSTR FileName,
      LPCVOID Buffer,
      DWORD NumberOfBytesToWrite,
      LPDWORD NumberOfBytesWritten,
      LONGLONG Offset,
      PDOKAN_FILE_INFO DokanFileInfo
    )
    {
      YIELD::platform::File* file = get_file( DokanFileInfo );

      ssize_t write_ret;

      if ( file )
        write_ret = file->write( Buffer, NumberOfBytesToWrite, Offset );
      else
      {
        file = get_volume( DokanFileInfo ).
                 open( FileName, O_CREAT|O_WRONLY ).release();

        if ( file != NULL )
        {
          write_ret = file->write( Buffer, NumberOfBytesToWrite, Offset );
          file->close(); // See note in CloseFile; assume this succeeds
          YIELD::platform::File::decRef( *file );
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


    static inline YIELD::platform::File*
    get_file
    (
      PDOKAN_FILE_INFO DokanFileInfo
    )
    {
      return reinterpret_cast<YIELD::platform::File*>
      (
        DokanFileInfo->Context
      );
    }

    static inline YIELD::platform::Volume&
    get_volume
    (
      PDOKAN_FILE_INFO DokanFileInfo
    )
    {
      return *reinterpret_cast<FUSEWin32*>
      (
        DokanFileInfo->DokanOptions->GlobalContext
      )->volume;
    }
  };
#endif
};




// tracing_directory.h
namespace yieldfs
{
  class TracingDirectory : public StackableDirectory
  {
  public:
    TracingDirectory
    (
      YIELD::platform::auto_Log log,
      const YIELD::platform::Path& path,
      YIELD::platform::auto_Directory underlying_file
    );

    // YIELD::platform::Directory
    YIELD_PLATFORM_DIRECTORY_PROTOTYPES;

  private:
    ~TracingDirectory();

    YIELD::platform::auto_Log log;
    YIELD::platform::Path path;
  };
};



// tracing_file.h
namespace yieldfs
{
  class TracingFile : public StackableFile
  {
  public:
    TracingFile
    (
      YIELD::platform::auto_Log log,
      const YIELD::platform::Path& path,
      YIELD::platform::auto_File underlying_file
    );

    // YIELD::platform::File
    YIELD_PLATFORM_FILE_PROTOTYPES;

  private:
    ~TracingFile();

    YIELD::platform::auto_Log log;
    YIELD::platform::Path path;
  };
};



// fuse.cpp
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


// stackable_directory.cpp
YIELD::platform::Directory::auto_Entry StackableDirectory::readdir()
{
  return underlying_directory->readdir();
}


// stackable_file.cpp
bool StackableFile::close()
{
  return underlying_file->close();
}

bool StackableFile::datasync()
{
  return underlying_file->datasync();
}

YIELD::platform::auto_Stat StackableFile::getattr()
{
  return underlying_file->getattr();
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

bool
StackableFile::setxattr
(
  const std::string& name,
  const std::string& value,
  int flags
)
{
  return underlying_file->setxattr( name, value, flags );
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

ssize_t
StackableFile::write
(
  const void* buffer,
  size_t buffer_len,
  uint64_t offset
)
{
  return underlying_file->write( buffer, buffer_len, offset );
}


// stackable_volume.cpp
bool StackableVolume::access( const YIELD::platform::Path& path, int amode )
{
  return underlying_volume->access( path, amode );
}

YIELD::platform::auto_Stat
StackableVolume::getattr
(
  const YIELD::platform::Path& path
)
{
  return underlying_volume->getattr( path );
}

bool
StackableVolume::getxattr
(
  const YIELD::platform::Path& path,
  const std::string& name,
  std::string& out_value
)
{
  return underlying_volume->getxattr( path, name, out_value );
}

bool
StackableVolume::link
(
  const YIELD::platform::Path& old_path,
  const YIELD::platform::Path& new_path
)
{
  return underlying_volume->link( old_path, new_path );
}

bool
StackableVolume::listxattr
(
  const YIELD::platform::Path& path,
  std::vector<std::string>& out_names
)
{
  return underlying_volume->listxattr( path, out_names );
}

bool StackableVolume::mkdir( const YIELD::platform::Path& path, mode_t mode )
{
  return underlying_volume->mkdir( path, mode );
}

YIELD::platform::auto_File
StackableVolume::open
(
  const YIELD::platform::Path& path,
  uint32_t flags,
  mode_t mode,
  uint32_t attributes
)
{
  return underlying_volume->open( path, flags, mode, attributes );
}

YIELD::platform::auto_Directory
StackableVolume::opendir
(
  const YIELD::platform::Path& path
)
{
  return underlying_volume->opendir( path );
}

YIELD::platform::auto_Path
StackableVolume::readlink( const YIELD::platform::Path& path )
{
  return underlying_volume->readlink( path );
}

bool
StackableVolume::removexattr
(
  const YIELD::platform::Path& path,
  const std::string& name
)
{
  return underlying_volume->removexattr( path, name );
}

bool
StackableVolume::rename
(
  const YIELD::platform::Path& from_path,
  const YIELD::platform::Path& to_path
)
{
  return underlying_volume->rename( from_path, to_path );
}

bool
StackableVolume::rmdir( const YIELD::platform::Path& path )
{
  return underlying_volume->rmdir( path );
}

bool
StackableVolume::setattr
(
  const YIELD::platform::Path& path,
  YIELD::platform::auto_Stat stbuf,
  uint32_t to_set
)
{
  return underlying_volume->setattr( path, stbuf, to_set );
}

bool
StackableVolume::setxattr
(
  const YIELD::platform::Path& path,
  const std::string& name,
  const std::string& value,
  int flags
)
{
  return underlying_volume->setxattr( path, name, value, flags );
}

bool
StackableVolume::statvfs
(
  const YIELD::platform::Path& path,
  struct statvfs& stvfsbuf
)
{
  return underlying_volume->statvfs( path, stvfsbuf );
}

bool
StackableVolume::symlink
(
  const YIELD::platform::Path& old_path,
  const YIELD::platform::Path& new_path
)
{
  return underlying_volume->symlink( old_path, new_path );
}

bool
StackableVolume::truncate
(
  const YIELD::platform::Path& path,
  uint64_t new_size
)
{
  return underlying_volume->truncate( path, new_size );
}

bool StackableVolume::unlink( const YIELD::platform::Path& path )
{
  return underlying_volume->unlink( path );
}

YIELD::platform::Path
StackableVolume::volname
(
  const YIELD::platform::Path& path
)
{
  return underlying_volume->volname( path );
}


// tracing_directory.cpp
TracingDirectory::TracingDirectory
(
  YIELD::platform::auto_Log log,
  const YIELD::platform::Path& path,
  YIELD::platform::auto_Directory underlying_directory
)
  : StackableDirectory( underlying_directory ),
    log( log ),
    path( path )
{ }

TracingDirectory::~TracingDirectory()
{
  log->getStream( YIELD::platform::Log::LOG_INFO ) <<
    "yieldfs::TracingDirectory::closedir( " << path << " )";
}

YIELD::platform::Directory::auto_Entry TracingDirectory::readdir()
{
  auto_Entry dirent = StackableDirectory::readdir();
  if ( dirent != NULL )
  {
    if
    (
      log->get_level() >= YIELD::platform::Log::LOG_DEBUG
      &&
      dirent->get_stat() != NULL
    )
    {
      log->getStream( YIELD::platform::Log::LOG_DEBUG ) <<
        "yieldfs::TracingDirectory::readdir( " << path << " ) -> " <<
        dirent->get_name() << ": " <<
        static_cast<std::string>( *dirent->get_stat() );
    }
    else
    {
      log->getStream( YIELD::platform::Log::LOG_INFO ) <<
        "yieldfs::TracingDirectory::readdir( " << path << " ) -> " <<
        dirent->get_name();
    }

    return dirent;
  }
  else
    return NULL;
}


// tracing_file.cpp
TracingFile::TracingFile
(
  YIELD::platform::auto_Log log,
  const YIELD::platform::Path& path,
  YIELD::platform::auto_File underlying_file
)
  : StackableFile( underlying_file ),
    log( log ),
    path( path )
{ }

TracingFile::~TracingFile()
{
  close();
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

YIELD::platform::auto_Stat TracingFile::getattr()
{
  YIELD::platform::auto_Stat stbuf = StackableFile::getattr();
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

bool TracingFile::getxattr( const std::string& name, std::string& out_value )
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


bool TracingFile::listxattr( std::vector<std::string>& out_names )
{
  return TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::listxattr",
    path,
    StackableFile::listxattr( out_names )
  );
}

ssize_t TracingFile::read( void* rbuf, size_t size, uint64_t offset )
{
  ssize_t read_ret = StackableFile::read( rbuf, size, offset );
  TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::read",
    path,
    size,
    offset,
    read_ret >= 0
  );
  return read_ret;
}

bool TracingFile::removexattr( const std::string& name )
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
  const std::string& name,
  const std::string& value,
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

ssize_t
TracingFile::write
(
  const void* buffer,
  size_t buffer_len,
  uint64_t offset
)
{
  ssize_t write_ret = StackableFile::write( buffer, buffer_len, offset );

  TracingVolume::trace
  (
    log,
    "yieldfs::TracingFile::write",
    path,
    buffer_len,
    offset,
    write_ret == static_cast<ssize_t>( buffer_len )
  );

  return write_ret;
}


// tracing_volume.cpp
#include <iostream>


TracingVolume::TracingVolume()
{
  log =
    YIELD::platform::Log::open( std::cout, YIELD::platform::Log::LOG_INFO );
}

TracingVolume::TracingVolume( YIELD::platform::auto_Volume underlying_volume )
  : StackableVolume( underlying_volume )
{
  log =
    YIELD::platform::Log::open( std::cout, YIELD::platform::Log::LOG_INFO );
}

TracingVolume::TracingVolume
(
  YIELD::platform::auto_Log log,
  YIELD::platform::auto_Volume underlying_volume
)
  : StackableVolume( underlying_volume ),
    log( log )
{ }

bool TracingVolume::access( const YIELD::platform::Path& path, int amode )
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

YIELD::platform::auto_Stat
TracingVolume::getattr
(
 const YIELD::platform::Path& path
)
{
  YIELD::platform::auto_Stat stbuf = StackableVolume::getattr( path );
  trace( log, "yieldfs::TracingVolume::getattr", path, stbuf != NULL );
  if ( stbuf != NULL )
    log->getStream( YIELD::platform::Log::LOG_DEBUG ) <<
      "yieldfs::TracingVolume::getattr returning Stat " <<
      static_cast<std::string>( *stbuf ) << ".";
  return stbuf;
}

bool
TracingVolume::getxattr
(
  const YIELD::platform::Path& path,
  const std::string& name,
  std::string& out_value
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

bool
TracingVolume::link
(
  const YIELD::platform::Path& old_path,
  const YIELD::platform::Path& new_path
)
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
  const YIELD::platform::Path& path,
  std::vector<std::string>& out_names
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
    YIELD::platform::Log::Stream log_stream =
      log->getStream( YIELD::platform::Log::LOG_INFO );
    log_stream << "  yieldfs::TracingVolume: xattr names: ";
    for
    (
      std::vector<std::string>::const_iterator name_i = out_names.begin();
      name_i != out_names.end();
      name_i++
    )
      log_stream << *name_i << " ";
    return true;
  }
  else
    return false;
}

bool
TracingVolume::mkdir( const YIELD::platform::Path& path, mode_t mode )
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

YIELD::platform::auto_File
TracingVolume::open
(
  const YIELD::platform::Path& path,
  uint32_t flags,
  mode_t mode,
  uint32_t attributes
)
{
  YIELD::platform::auto_File file =
    StackableVolume::open( path, flags, mode, attributes );
  if ( file != NULL )
    file = new TracingFile( log, path, file );

  YIELD::platform::Log::Stream log_stream =
    log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << "yieldfs::TracingVolume::open( " << path <<
    ", " << flags << ", " << mode << ", " << attributes << " )";

  trace( log_stream, file != NULL );

  return file;
}

YIELD::platform::auto_Directory
TracingVolume::opendir
(
  const YIELD::platform::Path& path
)
{
  YIELD::platform::auto_Directory directory = StackableVolume::opendir( path );
  if ( directory != NULL )
    directory = new TracingDirectory( log, path, directory );

  trace( log, "yieldfs::TracingVolume::opendir", path, directory != NULL );

  return directory;
}

YIELD::platform::auto_Path
TracingVolume::readlink
(
  const YIELD::platform::Path& path
)
{
  YIELD::platform::auto_Path link_path = StackableVolume::readlink( path );
  trace( log, "yieldfs::TracingVolume::readlink", path, link_path != NULL );
  return link_path;
}

bool
TracingVolume::removexattr
(
  const YIELD::platform::Path& path,
  const std::string& name
)
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

bool
TracingVolume::rename
(
  const YIELD::platform::Path& from_path,
  const YIELD::platform::Path& to_path
)
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

bool
TracingVolume::rmdir( const YIELD::platform::Path& path )
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
  const YIELD::platform::Path& path,
  YIELD::platform::auto_Stat stbuf,
  uint32_t to_set
)
{
  YIELD::platform::Log::Stream log_stream =
    log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << "yieldfs::TracingVolume::setattr( ";
  log_stream << path << ", ";
  log_stream << static_cast<std::string>( *stbuf ) << ", ";
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
  const YIELD::platform::Path& path,
  const std::string& name,
  const std::string& value,
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

bool
TracingVolume::statvfs
(
  const YIELD::platform::Path& path,
  struct statvfs& buf
)
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::statvfs",
    path,
    StackableVolume::statvfs( path, buf )
  );
}

bool
TracingVolume::symlink
(
  const YIELD::platform::Path& to_path,
  const YIELD::platform::Path& from_path
)
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
  YIELD::platform::auto_Log log,
  const char* operation_name,
  const YIELD::platform::Path& path,
  bool operation_result
)
{
  YIELD::platform::Log::Stream log_stream =
    log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << operation_name << "( " << path << " )";
  return trace( log_stream, operation_result );
}

bool
TracingVolume::trace
(
  YIELD::platform::auto_Log log,
  const char* operation_name,
  const YIELD::platform::Path& path,
  mode_t mode,
  bool operation_result
)
{
  YIELD::platform::Log::Stream log_stream =
    log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << operation_name << "( " << path << ", " << mode << " )";
  return trace( log_stream, operation_result );
}

bool
TracingVolume::trace
(
  YIELD::platform::auto_Log log,
  const char* operation_name,
  const YIELD::platform::Path& old_path,
  const YIELD::platform::Path& new_path,
  bool operation_result
)
{
  YIELD::platform::Log::Stream log_stream =
    log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << operation_name << "( " << old_path << ", " << new_path << " )";
  return trace( log_stream, operation_result );
}

bool
TracingVolume::trace
(
  YIELD::platform::auto_Log log,
  const char* operation_name,
  const YIELD::platform::Path& path,
  const std::string& xattr_name,
  const std::string& xattr_value,
  bool operation_result
)
{
  YIELD::platform::Log::Stream log_stream =
    log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << operation_name << "( " << path << ", "
    << xattr_name << ", " << xattr_value << " )";
  return trace( log_stream, operation_result );
}

bool
TracingVolume::trace
(
  YIELD::platform::auto_Log log,
  const char* operation_name,
  const YIELD::platform::Path& path,
  uint64_t size,
  uint64_t offset,
  bool operation_result
)
{
  YIELD::platform::Log::Stream log_stream =
    log->getStream( YIELD::platform::Log::LOG_INFO );
  log_stream << operation_name << "( " << path << ", "
    << size << ", " << offset << " )";
  return trace( log_stream, operation_result );
}

bool
TracingVolume::trace
(
  YIELD::platform::Log::Stream& log_stream,
  bool operation_result
)
{
  if ( operation_result )
   log_stream << " -> success.";
  else
   log_stream << " -> failed: " << YIELD::platform::Exception() << ".";

  return operation_result;
}

bool
TracingVolume::truncate
(
  const YIELD::platform::Path& path,
  uint64_t new_size
)
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

bool
TracingVolume::unlink( const YIELD::platform::Path& path )
{
  return trace
  (
    log,
    "yieldfs::TracingVolume::unlink",
    path,
    StackableVolume::unlink( path )
  );
}

YIELD::platform::Path
TracingVolume::volname
(
  const YIELD::platform::Path& path
)
{
  log->getStream( YIELD::platform::Log::LOG_INFO ) <<
    "yieldfs::TracingVolume::volname( " << path << " ) -> success.";
  return StackableVolume::volname( path );
}

