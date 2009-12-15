// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/volume.h"
#include "xtreemfs/file.h"
#include "xtreemfs/mrc_proxy.h"
#include "xtreemfs/osd_proxy.h"
#include "xtreemfs/path.h"
// No using namespace org::xtreemfs::interfaces since Volume is a class there
using namespace xtreemfs;

#include <errno.h>
#ifdef _WIN32
#include <windows.h>
#else
#include <sys/statvfs.h>
#endif


#define VOLUME_OPERATION_BEGIN( OperationName ) \
  try

#define VOLUME_OPERATION_END( OperationName ) \
  catch ( ProxyExceptionResponse& proxy_exception_response ) \
  { \
    set_errno( #OperationName, proxy_exception_response ); \
  } \
  catch ( std::exception& exc ) \
  { \
    set_errno( #OperationName, exc ); \
  } \


// Helper functions
static YIELD::platform::auto_Stat xtreemfs_Stat_as_YIELD_platform_Stat
( 
  const org::xtreemfs::interfaces::Stat& xtreemfs_stat 
)
{
  return new YIELD::platform::Stat
  (
#ifdef _WIN32
    xtreemfs_stat.get_mode(), 
    xtreemfs_stat.get_size(), 
    xtreemfs_stat.get_atime_ns(), 
    xtreemfs_stat.get_mtime_ns(), 
    xtreemfs_stat.get_ctime_ns(), 
    xtreemfs_stat.get_attributes() 
#else
    xtreemfs_stat.get_dev(), 
    xtreemfs_stat.get_ino(), 
    xtreemfs_stat.get_mode(), 
    xtreemfs_stat.get_nlink(), 
    xtreemfs_stat.get_uid(), 
    xtreemfs_stat.get_gid(),
    0, // rdev
    xtreemfs_stat.get_size(), 
    xtreemfs_stat.get_atime_ns(), 
    xtreemfs_stat.get_mtime_ns(), 
    xtreemfs_stat.get_ctime_ns(),
    xtreemfs_stat.get_blksize(),
    0 // blocks
#endif
  );
}


Volume::Volume
( 
  auto_DIRProxy dir_proxy, 
  uint32_t flags, 
  YIELD::platform::auto_Log log, 
  auto_MRCProxy mrc_proxy, 
  const std::string& name,
  auto_OSDProxyMux osd_proxy_mux, 
  YIELD::concurrency::auto_StageGroup stage_group, 
  const YIELD::platform::Path& vivaldi_coordinates_file_path 
)
  : dir_proxy( dir_proxy ), 
    flags( flags ), 
    log( log ), 
    mrc_proxy( mrc_proxy ), 
    name( name ), 
    osd_proxy_mux( osd_proxy_mux ),
    stage_group( stage_group ), 
    vivaldi_coordinates_file_path( vivaldi_coordinates_file_path )
{
  uuid = YIELD::ipc::UUID();
}

bool Volume::access( const YIELD::platform::Path&, int )
{
  return true;

  //VOLUME_OPERATION_BEGIN( access )
  //{
  //  // return mrc_proxy->access( Path( this->name, path ), amode );
  //}
  //VOLUME_OPERATION_END( access );
  //return false;
}

bool Volume::chmod( const YIELD::platform::Path& path, mode_t mode )
{
  VOLUME_OPERATION_BEGIN( chmod )
  {
    mrc_proxy->chmod( Path( this->name, path ), mode );
    return true;
  }
  VOLUME_OPERATION_END( chmod )
  return false;
}

bool Volume::chown( const YIELD::platform::Path& path, int uid, int gid )
{
  VOLUME_OPERATION_BEGIN( chown )
  {
    mrc_proxy->chown( Path( this->name, path ), uid, gid );
    return true;
  }
  VOLUME_OPERATION_END( chown );
  return false;
}

auto_Volume Volume::create
( 
  const YIELD::ipc::URI& dir_uri, 
  const std::string& name, 
  uint32_t flags, 
  YIELD::platform::auto_Log log, 
  uint32_t proxy_flags, 
  const YIELD::platform::Time& proxy_operation_timeout, 
  uint8_t proxy_reconnect_tries_max,
  YIELD::ipc::auto_SSLContext proxy_ssl_context, 
  const YIELD::platform::Path& vivaldi_coordinates_file_path 
)
{
  auto_DIRProxy dir_proxy = DIRProxy::create
  ( 
    dir_uri,
    DIRProxy::CONCURRENCY_LEVEL_DEFAULT,
    proxy_flags, 
    log, 
    proxy_operation_timeout, 
    proxy_reconnect_tries_max, 
    proxy_ssl_context 
  );

  YIELD::ipc::auto_URI mrc_uri; 
  std::string::size_type at_pos = name.find( '@' );
  if ( at_pos != std::string::npos )
    mrc_uri = dir_proxy->getVolumeURIFromVolumeName( name.substr( 0, at_pos ) );
  else
    mrc_uri = dir_proxy->getVolumeURIFromVolumeName( name );

  auto_MRCProxy mrc_proxy = MRCProxy::create
  ( 
    *mrc_uri, 
    MRCProxy::CONCURRENCY_LEVEL_DEFAULT,
    proxy_flags, 
    log, 
    proxy_operation_timeout, 
    "", 
    proxy_reconnect_tries_max, 
    proxy_ssl_context 
  );

  org::xtreemfs::interfaces::Stat stbuf;
  mrc_proxy->getattr( name + "/", stbuf );

  auto_OSDProxyMux osd_proxy_mux = OSDProxyMux::create
  ( 
    dir_proxy, 
    OSDProxy::CONCURRENCY_LEVEL_DEFAULT,
    proxy_flags, 
    log, 
    proxy_operation_timeout, 
    proxy_reconnect_tries_max, 
    proxy_ssl_context 
  );

  YIELD::concurrency::auto_StageGroup stage_group = 
    new YIELD::concurrency::SEDAStageGroup;
  stage_group->createStage( dir_proxy );
  stage_group->createStage( mrc_proxy );
  stage_group->createStage( osd_proxy_mux );

  return new Volume
  ( 
    dir_proxy, 
    flags, log, 
    mrc_proxy, 
    name, 
    osd_proxy_mux, 
    stage_group, 
    vivaldi_coordinates_file_path 
  );
}

org::xtreemfs::interfaces::VivaldiCoordinates 
  Volume::get_vivaldi_coordinates() const
{
  org::xtreemfs::interfaces::VivaldiCoordinates vivaldi_coordinates;

  if ( !vivaldi_coordinates_file_path.empty() )
  {
    YIELD::platform::auto_File vivaldi_coordinates_file = 
      YIELD::platform::Volume().open( vivaldi_coordinates_file_path );
    yidl::runtime::auto_Buffer vivaldi_coordinates_buffer
      ( 
        new yidl::runtime::StackBuffer
          <sizeof( org::xtreemfs::interfaces::VivaldiCoordinates)>
      );
    vivaldi_coordinates_file->read( vivaldi_coordinates_buffer );
    YIELD::platform::XDRUnmarshaller xdr_unmarshaller( vivaldi_coordinates_buffer );
    vivaldi_coordinates.unmarshal( xdr_unmarshaller );
  }

  return vivaldi_coordinates;
}

bool Volume::getxattr
( 
  const YIELD::platform::Path& path, 
  const std::string& name, 
  std::string& out_value 
)
{
  VOLUME_OPERATION_BEGIN( getxattr )
  {
    mrc_proxy->getxattr( Path( this->name, path ), name, out_value );
    return true;
  }
  VOLUME_OPERATION_END( getxattr );
  return false;
}

bool Volume::link
( 
  const YIELD::platform::Path& old_path, 
  const YIELD::platform::Path& new_path 
)
{
  VOLUME_OPERATION_BEGIN( link )
  {
    mrc_proxy->link
    ( 
      Path( this->name, old_path ), 
      Path( this->name, new_path ) 
    );

    return true;
  }
  VOLUME_OPERATION_END( link );
  return false;
}

bool Volume::listdir
( 
  const YIELD::platform::Path& path, 
  const YIELD::platform::Path&, 
  listdirCallback& callback 
)
{
  VOLUME_OPERATION_BEGIN( listdir )
  {
    org::xtreemfs::interfaces::StringSet names;
    mrc_proxy->xtreemfs_listdir( Path( this->name, path ), names );
    for 
    ( 
      org::xtreemfs::interfaces::StringSet::const_iterator 
        name_i = names.begin(); 
      name_i != names.end(); 
      name_i++ 
    )
    {
      if ( !callback( *name_i ) )
        return false;
    }
    return true;
  }
  VOLUME_OPERATION_END( listdir );
  return false;
}

bool Volume::listxattr
( 
  const YIELD::platform::Path& path, 
  std::vector<std::string>& out_names 
)
{
  VOLUME_OPERATION_BEGIN( listxattr )
  {
    org::xtreemfs::interfaces::StringSet names;
    mrc_proxy->listxattr( Path( this->name, path ), names );
    out_names.assign( names.begin(), names.end() );
    return true;
  }
  VOLUME_OPERATION_END( listxattr );
  return false;
}

bool Volume::mkdir( const YIELD::platform::Path& path, mode_t mode )
{
  VOLUME_OPERATION_BEGIN( mkdir )
  {
    mrc_proxy->mkdir( Path( this->name, path ), mode );
    return true;
  }
  VOLUME_OPERATION_END( mkdir );
  return false;
}

YIELD::platform::auto_File Volume::open
( 
  const YIELD::platform::Path& _path, 
  uint32_t flags, 
  mode_t mode, 
  uint32_t attributes 
)
{
  VOLUME_OPERATION_BEGIN( open )
  {
    Path path( this->name, _path );

    uint32_t system_v_flags;

#ifdef _WIN32
    system_v_flags = flags;
#else
    system_v_flags = 0;

    if ( ( flags & O_SYNC ) == O_SYNC )
    {
      system_v_flags |= org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_SYNC;
      flags ^= O_SYNC;
    }

    // Solaris and Windows have System V O_* constants; 
    // on other systems we have to translate from the 
    // local constants to System V
#if defined(__FreeBSD__) || defined(__linux__) || defined(__MACH__)
    if ( ( flags & O_WRONLY ) == O_WRONLY )
    {
	    system_v_flags |= org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_WRONLY;
	    flags ^= O_WRONLY;
    }

    if ( ( flags & O_RDWR ) == O_RDWR )
    {
	    system_v_flags |= org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_RDWR;
	    flags ^= O_RDWR;
    }

    if ( ( flags & O_APPEND ) == O_APPEND )
    {
	    system_v_flags |= org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_APPEND;
	    flags ^= O_APPEND;
    }

    if ( ( flags & O_CREAT ) == O_CREAT )
    {
	    system_v_flags |= org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_CREAT;
	    flags ^= O_CREAT;
    }

    if ( ( flags & O_TRUNC ) == O_TRUNC )
    {
	    system_v_flags |= org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_TRUNC;
	    flags ^= O_TRUNC;
    }

    if ( ( flags & O_EXCL ) == O_EXCL )
    {
	    system_v_flags |= org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_EXCL;
	    flags ^= O_EXCL;
    }
#endif
    system_v_flags |= flags;
#endif

    org::xtreemfs::interfaces::FileCredentials file_credentials;
    mrc_proxy->open
    ( 
      path, 
      system_v_flags, 
      mode, 
      attributes, 
      get_vivaldi_coordinates(), 
      file_credentials 
    );

    return new File( incRef(), path, file_credentials );
  }
  VOLUME_OPERATION_END( open );
  return NULL;
}

void Volume::osd_unlink
( 
  const org::xtreemfs::interfaces::FileCredentialsSet& file_credentials_set 
)
{
  if ( !file_credentials_set.empty() ) 
  { 
    // We have to delete files on all replica OSDs
    const org::xtreemfs::interfaces::FileCredentials& file_credentials = 
      file_credentials_set[0];
    const std::string& file_id = file_credentials.get_xcap().get_file_id();
    osd_proxy_mux->unlink( file_credentials, file_id );
  }
}

bool Volume::readdir
( 
  const YIELD::platform::Path& path, 
  const YIELD::platform::Path&, 
  YIELD::platform::Volume::readdirCallback& callback 
)
{
  VOLUME_OPERATION_BEGIN( readdir )
  {
    org::xtreemfs::interfaces::DirectoryEntrySet directory_entries;
    mrc_proxy->readdir( Path( this->name, path ), directory_entries );
    for
    ( 
      org::xtreemfs::interfaces::DirectoryEntrySet::const_iterator 
        directory_entry_i = directory_entries.begin(); 
      directory_entry_i != directory_entries.end(); 
      directory_entry_i++ 
     )
    {
      if 
      ( 
        !callback
        ( 
          ( *directory_entry_i ).get_name(),
          xtreemfs_Stat_as_YIELD_platform_Stat
          ( 
            ( *directory_entry_i ).get_stbuf() 
          )
        )            
      )
        return false;
    }
    return true;
  }
  VOLUME_OPERATION_END( readdir );
  return false;
}

YIELD::platform::auto_Path Volume::readlink
( 
  const YIELD::platform::Path& path 
)
{
  VOLUME_OPERATION_BEGIN( readlink )
  {
    org::xtreemfs::interfaces::Stat stbuf;
    mrc_proxy->getattr( Path( this->name, path ), stbuf );
    return new YIELD::platform::Path( stbuf.get_link_target() );
  }
  VOLUME_OPERATION_END( readlink );
  return NULL;
}

bool Volume::rename
( 
  const YIELD::platform::Path& from_path, 
  const YIELD::platform::Path& to_path 
)
{
  VOLUME_OPERATION_BEGIN( rename )
  {
    org::xtreemfs::interfaces::FileCredentialsSet file_credentials_set;

    mrc_proxy->rename
    ( 
      Path( this->name, from_path ), 
      Path( this->name, to_path ), 
      file_credentials_set 
    );

    osd_unlink( file_credentials_set );

    return true;
  }
  VOLUME_OPERATION_END( rename );
  return false;
}

bool Volume::rmdir( const YIELD::platform::Path& path )
{
  VOLUME_OPERATION_BEGIN( rmdir )
  {
    mrc_proxy->rmdir( Path( this->name, path ) );
    return true;
  }
  VOLUME_OPERATION_END( rmdir );
  return false;
}

bool Volume::removexattr
( 
  const YIELD::platform::Path& path, 
  const std::string& name 
)
{
  VOLUME_OPERATION_BEGIN( removexattr )
  {
    mrc_proxy->removexattr( Path( this->name, path ), name );
    return true;
  }
  VOLUME_OPERATION_END( removexattr );
  return false;
}

bool Volume::setattr
( 
  const YIELD::platform::Path& path, 
  uint32_t file_attributes 
)
{
  VOLUME_OPERATION_BEGIN( setattr )
  {
    org::xtreemfs::interfaces::Stat stbuf;
    stbuf.set_attributes( file_attributes );
    mrc_proxy->setattr( Path( this->name, path ), stbuf );
    return true;
  }
  VOLUME_OPERATION_END( setattr );
  return false;
}

void Volume::set_errno
( 
  const char* operation_name, 
  ProxyExceptionResponse& proxy_exception_response 
)
{
  if ( log != NULL )
  {
    switch ( proxy_exception_response.get_platform_error_code() )
    {
#ifdef _WIN32
      case ERROR_FILE_NOT_FOUND:
      case ERROR_FILE_EXISTS: break;
#else
#ifdef __FreeBSD__
      case ENOATTR:
#else
      case ENODATA:
#endif
      case ENOENT:
      case EEXIST: break;
#endif
      default:
      {
        log->getStream( YIELD::platform::Log::LOG_ERR ) << 
          "xtreemfs::Volume: caught exception on " << 
          operation_name << ": " << proxy_exception_response.what();
      }
      break;
    }
  }
    
  YIELD::platform::Exception::set_errno
  ( 
    proxy_exception_response.get_platform_error_code() 
  );
}

void Volume::set_errno( const char* operation_name, std::exception& exc )
{
  if ( log != NULL )
    log->getStream( YIELD::platform::Log::LOG_ERR ) << 
      "xtreemfs::Volume: caught exception on " << 
      operation_name << ": " << exc.what();

#ifdef _WIN32
  YIELD::platform::Exception::set_errno( ERROR_ACCESS_DENIED );
#else
  YIELD::platform::Exception::set_errno( EIO );
#endif  
}

bool Volume::setxattr
( 
  const YIELD::platform::Path& path, 
  const std::string& name, 
  const std::string& value, 
  int flags 
)
{
  VOLUME_OPERATION_BEGIN( setxattr )
  {
    mrc_proxy->setxattr( Path( this->name, path ), name, value, flags );
    return true;
  }
  VOLUME_OPERATION_END( setxattr );
  return false;
}

YIELD::platform::auto_Stat Volume::stat( const YIELD::platform::Path& path )
{
  VOLUME_OPERATION_BEGIN( getattr )
  {
    return stat( Path( this->name, path ) );
  }
  VOLUME_OPERATION_END( getattr );
  return NULL;
}

YIELD::platform::auto_Stat Volume::stat( const Path& path )
{
  org::xtreemfs::interfaces::Stat xtreemfs_stat;
  mrc_proxy->getattr( path, xtreemfs_stat );
  return xtreemfs_Stat_as_YIELD_platform_Stat( xtreemfs_stat );
}

bool Volume::statvfs
( 
  const YIELD::platform::Path&, 
  struct statvfs& statvfsbuf 
)
{
  VOLUME_OPERATION_BEGIN( statvfs )
  {
    org::xtreemfs::interfaces::StatVFS xtreemfs_statvfsbuf;
    mrc_proxy->statvfs( this->name, xtreemfs_statvfsbuf );
    memset( &statvfsbuf, 0, sizeof( statvfsbuf ) );
    statvfsbuf.f_bavail = xtreemfs_statvfsbuf.get_bavail();
    statvfsbuf.f_bfree = xtreemfs_statvfsbuf.get_bavail();
    statvfsbuf.f_blocks = xtreemfs_statvfsbuf.get_blocks();
    statvfsbuf.f_bsize = xtreemfs_statvfsbuf.get_bsize();     
    statvfsbuf.f_namemax = xtreemfs_statvfsbuf.get_namelen();
    return true;
  }
  VOLUME_OPERATION_END( statvfs );
  return false;
}

bool Volume::symlink
( 
  const YIELD::platform::Path& to_path, 
  const YIELD::platform::Path& from_path 
)
{
  VOLUME_OPERATION_BEGIN( symlink )
  {
    mrc_proxy->symlink( to_path, Path( this->name, from_path ) );
    return true;
  }
  VOLUME_OPERATION_END( symlink );
  return false;
}

bool Volume::truncate( const YIELD::platform::Path& path, uint64_t new_size )
{
  VOLUME_OPERATION_BEGIN( truncate )
  {
    YIELD::platform::auto_File file = 
      YIELD::platform::Volume::open( path, O_TRUNC );
    if ( file != NULL )
      return file->truncate( new_size );
    else
      return false;
  }
  VOLUME_OPERATION_END( truncate );
  return false;
}

bool Volume::unlink( const YIELD::platform::Path& path )
{
  VOLUME_OPERATION_BEGIN( unlink )
  {
    org::xtreemfs::interfaces::FileCredentialsSet file_credentials_set;
    mrc_proxy->unlink( Path( this->name, path ), file_credentials_set );
    osd_unlink( file_credentials_set );
    return true;
  }
  VOLUME_OPERATION_END( unlink );
  return false;
}

bool Volume::utimens
( 
  const YIELD::platform::Path& path, 
  const YIELD::platform::Time& atime, 
  const YIELD::platform::Time& mtime, 
  const YIELD::platform::Time& ctime 
)
{
  VOLUME_OPERATION_BEGIN( utimens )
  {
    mrc_proxy->utimens( Path( this->name, path ), atime, mtime, ctime );
    return true;
  }
  VOLUME_OPERATION_END( utimens );
  return false;
}

YIELD::platform::Path Volume::volname( const YIELD::platform::Path& )
{
  return name;
}

