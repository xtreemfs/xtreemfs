// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/volume.h"
#include "org/xtreemfs/client/file.h"
#include "org/xtreemfs/client/osd_proxy.h"
#include "org/xtreemfs/client/path.h"
using namespace org::xtreemfs::client;

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "org/xtreemfs/interfaces/mrc_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif

#include <errno.h>
#ifdef _WIN32
#include <windows.h>
#else
#include <sys/statvfs.h>
#endif


#define ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( OperationName ) \
  try

#define ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( OperationName ) \
  catch ( ProxyExceptionResponse& proxy_exception_response ) \
  { \
    set_errno( #OperationName, proxy_exception_response ); \
  } \
  catch ( std::exception& exc ) \
  { \
    set_errno( #OperationName, exc ); \
  } \


Volume::Volume( const YIELD::URI& dir_uri, const std::string& name, uint32_t flags, YIELD::auto_Log log, uint32_t proxy_flags, YIELD::auto_SSLContext ssl_context )
  : name( name ), flags( flags ), log( log )
{
  stage_group = new YIELD::SEDAStageGroup;
  dir_proxy = DIRProxy::create( dir_uri, proxy_flags, log, 5 * NS_IN_S, ssl_context );
  stage_group->createStage( dir_proxy );
  YIELD::auto_URI mrc_uri = dir_proxy->getVolumeURIFromVolumeName( name );
  mrc_proxy = MRCProxy::create( *mrc_uri, proxy_flags, log, 5 * NS_IN_S, ssl_context );
  stage_group->createStage( mrc_proxy );
  osd_proxy_mux = OSDProxyMux::create( dir_proxy, proxy_flags, log, 10 * NS_IN_S, ssl_context, stage_group );
  stage_group->createStage( osd_proxy_mux );
}

bool Volume::access( const YIELD::Path& path, int amode )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( access )
  {
    return true;
    // return mrc_proxy->access( Path( this->name, path ), amode );
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( access );
  return false;
}

bool Volume::chmod( const YIELD::Path& path, mode_t mode )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( chmod )
  {
    mrc_proxy->chmod( Path( this->name, path ), mode );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( chmod )
  return false;
}

bool Volume::chown( const YIELD::Path& path, int uid, int gid )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( chown )
  {
    mrc_proxy->chown( Path( this->name, path ), uid, gid );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( chown );
  return false;
}

YIELD::auto_Stat Volume::getattr( const YIELD::Path& path )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( getattr )
  {
    return getattr( Path( this->name, path ) );
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( getattr );
  return NULL;
}

YIELD::auto_Stat Volume::getattr( const Path& path )
{
  org::xtreemfs::interfaces::Stat stbuf;
  mrc_proxy->getattr( path, stbuf );
#ifdef _WIN32
  return new YIELD::Stat( stbuf.get_mode(), stbuf.get_size(), stbuf.get_atime_ns(), stbuf.get_mtime_ns(), stbuf.get_ctime_ns(), stbuf.get_attributes() );
#else
  return new YIELD::Stat( stbuf.get_mode(), stbuf.get_nlink(), stbuf.get_uid(), stbuf.get_gid(), stbuf.get_size(), stbuf.get_atime_ns(), stbuf.get_mtime_ns(), stbuf.get_ctime_ns() );
#endif
}

bool Volume::getxattr( const YIELD::Path& path, const std::string& name, std::string& out_value )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( getxattr )
  {
    mrc_proxy->getxattr( Path( this->name, path ), name, out_value );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( getxattr );
  return false;
}

bool Volume::link( const YIELD::Path& old_path, const YIELD::Path& new_path )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( link )
  {
    mrc_proxy->link( Path( this->name, old_path ), Path( this->name, new_path ) );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( link );
  return false;
}

bool Volume::listdir( const YIELD::Path& path, const YIELD::Path&, listdirCallback& callback )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( listdir )
  {
    org::xtreemfs::interfaces::StringSet names;
    mrc_proxy->xtreemfs_listdir( Path( this->name, path ), names );
    for ( org::xtreemfs::interfaces::StringSet::const_iterator name_i = names.begin(); name_i != names.end(); name_i++ )
    {
      if ( !callback( *name_i ) )
        return false;
    }
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( listdir );
  return false;
}

bool Volume::listxattr( const YIELD::Path& path, std::vector<std::string>& out_names )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( listxattr )
  {
    xtreemfs::interfaces::StringSet names;
    mrc_proxy->listxattr( Path( this->name, path ), names );
    out_names.assign( names.begin(), names.end() );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( listxattr );
  return false;
}

bool Volume::mkdir( const YIELD::Path& path, mode_t mode )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( mkdir )
  {
    mrc_proxy->mkdir( Path( this->name, path ), mode );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( mkdir );
  return false;
}

YIELD::auto_File Volume::open( const YIELD::Path& _path, uint32_t flags, mode_t mode, uint32_t attributes )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( open )
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

#ifdef __linux__
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
    mrc_proxy->open( path, system_v_flags, mode, attributes, file_credentials );

    return new File( incRef(), mrc_proxy, path, file_credentials );
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( open );
  return NULL;
}

void Volume::osd_unlink( const org::xtreemfs::interfaces::FileCredentialsSet& file_credentials_set )
{
  if ( !file_credentials_set.empty() ) // We have to delete files on replica OSDs
  {
    const org::xtreemfs::interfaces::FileCredentials& file_credentials = file_credentials_set[0];
    const std::string& file_id = file_credentials.get_xcap().get_file_id();
    osd_proxy_mux->unlink( file_credentials, file_id );
  }
}

bool Volume::readdir( const YIELD::Path& path, const YIELD::Path&, YIELD::Volume::readdirCallback& callback )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( readdir )
  {
    org::xtreemfs::interfaces::DirectoryEntrySet directory_entries;
    mrc_proxy->readdir( Path( this->name, path ), directory_entries );
    for ( org::xtreemfs::interfaces::DirectoryEntrySet::const_iterator directory_entry_i = directory_entries.begin(); directory_entry_i != directory_entries.end(); directory_entry_i++ )
    {
      const org::xtreemfs::interfaces::Stat& xtreemfs_stat = ( *directory_entry_i ).get_stbuf();
#ifdef _WIN32
      if ( !callback( ( *directory_entry_i ).get_name(), new YIELD::Stat( xtreemfs_stat.get_mode(), xtreemfs_stat.get_size(), xtreemfs_stat.get_atime_ns(), xtreemfs_stat.get_mtime_ns(), xtreemfs_stat.get_ctime_ns(), xtreemfs_stat.get_attributes() ) ) )
#else
      if ( !callback( ( *directory_entry_i ).get_name(), new YIELD::Stat( xtreemfs_stat.get_mode(), xtreemfs_stat.get_nlink(), xtreemfs_stat.get_uid(), xtreemfs_stat.get_gid(), xtreemfs_stat.get_size(), xtreemfs_stat.get_atime_ns(), xtreemfs_stat.get_mtime_ns(), xtreemfs_stat.get_ctime_ns() ) ) )
#endif
        return false;
    }
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( readdir );
  return false;
}

YIELD::auto_Object<YIELD::Path> Volume::readlink( const YIELD::Path& path )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( readlink )
  {
    org::xtreemfs::interfaces::Stat stbuf;
    mrc_proxy->getattr( Path( this->name, path ), stbuf );
    return new YIELD::Path( stbuf.get_link_target() );
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( readlink );
  return NULL;
}

bool Volume::rename( const YIELD::Path& from_path, const YIELD::Path& to_path )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( rename )
  {
    Path from_xtreemfs_path( this->name, from_path ), to_xtreemfs_path( this->name, to_path );
    org::xtreemfs::interfaces::FileCredentialsSet file_credentials_set;
    mrc_proxy->rename( from_xtreemfs_path, to_xtreemfs_path, file_credentials_set );
    osd_unlink( file_credentials_set );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( rename );
  return false;
}

bool Volume::rmdir( const YIELD::Path& path )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( rmdir )
  {
    mrc_proxy->rmdir( Path( this->name, path ) );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( rmdir );
  return false;
}

bool Volume::removexattr( const YIELD::Path& path, const std::string& name )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( removexattr )
  {
    mrc_proxy->removexattr( Path( this->name, path ), name );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( removexattr );
  return false;
}

bool Volume::setattr( const YIELD::Path& path, uint32_t file_attributes )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( setattr )
  {
    xtreemfs::interfaces::Stat stbuf;
    stbuf.set_attributes( file_attributes );
    mrc_proxy->setattr( Path( this->name, path ), stbuf );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( setattr );
  return false;
}

void Volume::set_errno( const char* operation_name, ProxyExceptionResponse& proxy_exception_response )
{
#ifdef _DEBUG
  if ( log != NULL )
    log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Volume: caught exception on " << operation_name << ": " << proxy_exception_response.what();
#endif

  YIELD::Exception::set_errno( proxy_exception_response.get_platform_error_code() );
}

void Volume::set_errno( const char* operation_name, std::exception& exc )
{
#ifdef _DEBUG
  if ( log != NULL )
    log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Volume: caught exception on " << operation_name << ": " << exc.what();
#endif

#ifdef _WIN32
  YIELD::Exception::set_errno( ERROR_ACCESS_DENIED );
#else
  YIELD::Exception::set_errno( EIO );
#endif  
}

bool Volume::setxattr( const YIELD::Path& path, const std::string& name, const std::string& value, int flags )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( setxattr )
  {
    mrc_proxy->setxattr( Path( this->name, path ), name, value, flags );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( setxattr );
  return false;
}

bool Volume::statvfs( const YIELD::Path&, struct statvfs* statvfsbuf )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( statvfs )
  {
    if ( statvfsbuf )
    {
      memset( statvfsbuf, 0, sizeof( *statvfsbuf ) );
      xtreemfs::interfaces::StatVFS xtreemfs_statvfsbuf;
      mrc_proxy->statvfs( this->name, xtreemfs_statvfsbuf );
      statvfsbuf->f_bavail = xtreemfs_statvfsbuf.get_bavail();
      statvfsbuf->f_bfree = xtreemfs_statvfsbuf.get_bavail();
      statvfsbuf->f_blocks = xtreemfs_statvfsbuf.get_blocks();
      statvfsbuf->f_bsize = xtreemfs_statvfsbuf.get_bsize();     
      statvfsbuf->f_namemax = xtreemfs_statvfsbuf.get_namelen();
      return true;
    }
    else
      return false;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( statvfs );
  return false;
}

bool Volume::symlink( const YIELD::Path& to_path, const YIELD::Path& from_path )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( symlink )
  {
    mrc_proxy->symlink( to_path, Path( this->name, from_path ) );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( symlink );
  return false;
}

bool Volume::truncate( const YIELD::Path& path, uint64_t new_size )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( truncate )
  {
    YIELD::auto_File file = YIELD::Volume::open( path, O_TRUNC );
    if ( file != NULL )
      return file->truncate( new_size );
    else
      return false;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( truncate );
  return false;
}

bool Volume::unlink( const YIELD::Path& path )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( unlink )
  {
    org::xtreemfs::interfaces::FileCredentialsSet file_credentials_set;
    mrc_proxy->unlink( Path( this->name, path ), file_credentials_set );
    osd_unlink( file_credentials_set );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( unlink );
  return false;
}

bool Volume::utimens( const YIELD::Path& path, const YIELD::Time& atime, const YIELD::Time& mtime, const YIELD::Time& ctime )
{
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_BEGIN( utimens )
  {
    mrc_proxy->utimens( Path( this->name, path ), atime, mtime, ctime );
    return true;
  }
  ORG_XTREEMFS_CLIENT_VOLUME_OPERATION_END( utimens );
  return false;
}

YIELD::Path Volume::volname( const YIELD::Path& )
{
  return name;
}
