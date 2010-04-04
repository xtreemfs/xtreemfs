// Copyright (c) 2010 Minor Gordon
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
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#include "xtreemfs/volume.h"
#include "directory.h"
#include "file.h"
#include "stat.h"
#include "stat_cache.h"
#include "user_credentials_cache.h"
#include "xtreemfs/mrc_proxy.h"
#include "xtreemfs/options.h"
#include "xtreemfs/osd_proxy.h"
using namespace xtreemfs;
using org::xtreemfs::interfaces::DirectoryEntrySet;
using org::xtreemfs::interfaces::FileCredentials;
using org::xtreemfs::interfaces::FileCredentialsSet;
using org::xtreemfs::interfaces::ReplicaSet;
using org::xtreemfs::interfaces::StatSet;
using org::xtreemfs::interfaces::StatVFSSet;
using org::xtreemfs::interfaces::StringSet;
using org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_RDONLY;
using org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_WRONLY;
using org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_RDWR;
using org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_APPEND;
using org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_CREAT;
using org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_TRUNC;
using org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_EXCL;
using org::xtreemfs::interfaces::SYSTEM_V_FCNTL_H_O_SYNC;

#include "yidl.h"
using yidl::runtime::auto_Object;

#include "yield.h"
using yield::concurrency::StageGroup;
using yield::platform::iconv;
using yield::platform::StackBuffer;
using yield::platform::UUID;
using yield::platform::XDRUnmarshaller;

#include <errno.h>
#ifdef _WIN32
#include <windows.h>
#else
#include <sys/statvfs.h>
#endif


#define VOLUME_OPERATION_BEGIN( OperationName ) \
  try

#define VOLUME_OPERATION_END( OperationName ) \
  catch ( Exception& exception ) \
  { \
    set_errno( #OperationName, exception ); \
  } \
  catch ( std::exception& exception ) \
  { \
    set_errno( #OperationName, exception ); \
  }


#ifdef _WIN32
uint32_t Volume::ERROR_CODE_DEFAULT = ERROR_ACCESS_DENIED;
#else
uint32_t Volume::ERROR_CODE_DEFAULT = EIO;
#endif


class Volume::FileState
{
public:
  FileState( uint32_t reader_count, uint32_t writer_count )
    : reader_count( reader_count ), writer_count( writer_count )
  { }

  uint32_t get_reader_count() const { return reader_count; }
  uint32_t get_writer_count() const { return writer_count; }
  void inc_reader_count() { ++reader_count; }
  void inc_writer_count() { ++writer_count; }
  uint32_t dec_reader_count() { return --reader_count; }
  uint32_t dec_writer_count() { return --writer_count; }

private:
  uint32_t reader_count, writer_count;
};


Volume::Volume
(
  DIRProxy& dir_proxy,
  Log* error_log,
  uint32_t flags,
  MRCProxy& mrc_proxy,
  const string& name_utf8,
  OSDProxies& osd_proxies,
  StageGroup& stage_group,
  Log* trace_log,
  UserCredentialsCache& user_credentials_cache,
  const Path& vivaldi_coordinates_file_path
)
  : dir_proxy( dir_proxy ),
    error_log( Object::inc_ref( error_log ) ),
    flags( flags ),
    mrc_proxy( mrc_proxy ),
    name_utf8( name_utf8 ),
    osd_proxies( osd_proxies ),
    stage_group( stage_group ),
    trace_log( Object::inc_ref( trace_log ) ),
    user_credentials_cache( user_credentials_cache ),
    vivaldi_coordinates_file_path( vivaldi_coordinates_file_path )
{
  double stat_cache_read_ttl_s;
  uint32_t stat_cache_write_back_attrs;

  if ( flags & FLAG_WRITE_BACK_FILE_SIZE_CACHE ) 
  {
    stat_cache_read_ttl_s = 0; // Don't cache read Stats
    stat_cache_write_back_attrs = yield::platform::Volume::SETATTR_SIZE;
  }
  else if ( flags & FLAG_WRITE_BACK_STAT_CACHE )
  {
    stat_cache_read_ttl_s = 5;
    stat_cache_write_back_attrs = yield::platform::Volume::SETATTR_SIZE;
  }
  else if ( flags & FLAG_WRITE_THROUGH_STAT_CACHE )
  {
    stat_cache_read_ttl_s = 5;
    stat_cache_write_back_attrs = 0;
  }
  else
  {
    stat_cache_read_ttl_s = 0;
    stat_cache_write_back_attrs = 0;
  }

  stat_cache
    = new StatCache
          (
            mrc_proxy,
            stat_cache_read_ttl_s,
            user_credentials_cache,
            name_utf8,
            stat_cache_write_back_attrs
          );

  uuid = UUID();
}

Volume::~Volume()
{
  DIRProxy::dec_ref( dir_proxy );
  Log::dec_ref( error_log );
  for 
  ( 
    FileStateMap::iterator file_state_i = file_state_map.begin();
    file_state_i != file_state_map.end();
    ++file_state_i
  )
    delete file_state_i->second;
  OSDProxies::dec_ref( osd_proxies );
  StageGroup::dec_ref( stage_group );
  delete stat_cache;
  Log::dec_ref( trace_log );
  UserCredentialsCache::dec_ref( user_credentials_cache );
}

bool Volume::access( const Path&, int )
{
  return true;
}

void Volume::close( File& file )
{
  file_state_map_lock.acquire();

  FileStateMap::iterator file_state_i 
    = file_state_map.find( file.get_xcap().get_file_id() );

  if ( file_state_i != file_state_map.end() )
  {
    FileState* file_state = file_state_i->second;

    if 
    ( 
      ( file.get_xcap().get_access_mode() & SYSTEM_V_FCNTL_H_O_WRONLY )
        == SYSTEM_V_FCNTL_H_O_WRONLY
      ||
      ( file.get_xcap().get_access_mode() & SYSTEM_V_FCNTL_H_O_RDWR )
        == SYSTEM_V_FCNTL_H_O_RDWR
    )
    {
      // Writer
      if ( file_state->dec_writer_count() == 0 )
      {
        metadatasync( file.get_path(), file.get_xcap() );

        if ( file_state->get_reader_count() == 0 )
        {
          file_state_map.erase( file_state_i );
          delete file_state;
        }
      }
    }
    else
    {
      // Reader
      if 
      ( 
        file_state->dec_reader_count() == 0
        &&
        file_state->get_writer_count() == 0
      )
      {
        file_state_map.erase( file_state_i );
        delete file_state;
      }
    }
  }
  else
    DebugBreak();
  
  file_state_map_lock.release();
}

Volume&
Volume::create
(
  const Options& options,
  uint32_t flags,
  const Path& vivaldi_coordinates_file_path
)
{
  URI* dir_uri = options.get_uri();
  if ( dir_uri == NULL || dir_uri->get_resource() == "/" )
    throw Exception( "must specify the <DIR>/<volume name> URI" );
  string name_utf8 = dir_uri->get_resource().substr( 1 );

  return create
         (
           *dir_uri,
           name_utf8,
           options.get_error_log(),
           flags,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
           options.get_ssl_context(),
#endif
           options.get_trace_log(),
           vivaldi_coordinates_file_path           
         );
}

Volume&
Volume::create
(
  const URI& dir_uri,
  const string& name_utf8,
  Log* error_log,
  uint32_t flags,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
  SSLContext* proxy_ssl_context,
#endif
  Log* trace_log,
  const Path& vivaldi_coordinates_file_path
)
{
  UserCredentialsCache* user_credentials_cache = new UserCredentialsCache;

  DIRProxy& dir_proxy
    = DIRProxy::create
      (
        dir_uri,
        error_log,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
        proxy_ssl_context,
#endif
        trace_log
      );

  URI mrc_uri;
  string::size_type at_pos = name_utf8.find( '@' );
  if ( at_pos != string::npos )
    mrc_uri = dir_proxy.getVolumeURIFromVolumeName( name_utf8.substr( 0, at_pos ) );
  else
    mrc_uri = dir_proxy.getVolumeURIFromVolumeName( name_utf8 );

  MRCProxy& mrc_proxy
    = MRCProxy::create
      (
        mrc_uri,
        error_log,
        "",
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
        proxy_ssl_context,
#endif
        trace_log,
        user_credentials_cache
      );

  StatSet stbuf;
  mrc_proxy.getattr( name_utf8, "", 0, stbuf );

  StageGroup* stage_group = new yield::concurrency::SEDAStageGroup;
  //stage_group->createStage( dir_proxy );
  //stage_group->createStage( mrc_proxy );

  OSDProxies* osd_proxies
    = new OSDProxies
          (
            dir_proxy,
            error_log,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
            proxy_ssl_context,
#endif
            stage_group,
            trace_log
          );

  return *new Volume
              (
                dir_proxy,
                error_log,
                flags,
                mrc_proxy,
                name_utf8,
                *osd_proxies,
                *stage_group,
                trace_log,
                *user_credentials_cache,
                vivaldi_coordinates_file_path
              );
}

void
Volume::fsetattr
(
  const Path& path,
  const Stat& stbuf,
  uint32_t to_set,
  const XCap& write_xcap
)
{
  stat_cache->fsetattr
  (
    path.encode( iconv::CODE_UTF8 ),
    stbuf,
    to_set,
    write_xcap
  );
}

yield::platform::Stat* Volume::getattr( const Path& path )
{
  VOLUME_OPERATION_BEGIN( stat )
  {
    return stat_cache->getattr( path );
  }
  VOLUME_OPERATION_END( stat );
  return NULL;
}

UserCredentialsCache& Volume::get_user_credentials_cache() const
{
  return user_credentials_cache;
}

VivaldiCoordinates
Volume::get_vivaldi_coordinates() const
{
  VivaldiCoordinates vivaldi_coordinates;

  if ( !vivaldi_coordinates_file_path.empty() )
  {
    yield::platform::File* vivaldi_coordinates_file =
      yield::platform::Volume().open( vivaldi_coordinates_file_path );

    if ( vivaldi_coordinates_file != NULL )
    {
      StackBuffer<sizeof( VivaldiCoordinates )> vivaldi_coordinates_buffer;

      vivaldi_coordinates_file->read( vivaldi_coordinates_buffer );
      yield::platform::File::dec_ref( *vivaldi_coordinates_file );

      XDRUnmarshaller xdr_unmarshaller( vivaldi_coordinates_buffer );
      vivaldi_coordinates.unmarshal( xdr_unmarshaller );
    }
  }

  return vivaldi_coordinates;
}

bool
Volume::getxattr
(
  const Path& path,
  const string& name,
  string& out_value
)
{
  // Always pass through

  VOLUME_OPERATION_BEGIN( getxattr )
  {
    mrc_proxy.getxattr
    (
      this->name_utf8,
      path.encode( iconv::CODE_UTF8 ),
      name,
      out_value
    );
    return true;
  }
  VOLUME_OPERATION_END( getxattr );
  return false;
}

bool
Volume::link
(
  const Path& old_path,
  const Path& new_path
)
{
  // Don't stat_cache->evict( old_path ) in case there are file size updates
  // Just let it expire

  VOLUME_OPERATION_BEGIN( link )
  {
    mrc_proxy.link
    (
      name_utf8,
      old_path.encode( iconv::CODE_UTF8 ),
      new_path.encode( iconv::CODE_UTF8 )
    );

    return true;
  }
  VOLUME_OPERATION_END( link );
  return false;
}

bool
Volume::listxattr
(
  const Path& path,
  vector<string>& out_names
)
{
  VOLUME_OPERATION_BEGIN( listxattr )
  {
    StringSet names;
    mrc_proxy.listxattr( name_utf8, path.encode( iconv::CODE_UTF8 ), names );
    out_names.assign( names.begin(), names.end() );
    return true;
  }
  VOLUME_OPERATION_END( listxattr );
  return false;
}

void
Volume::metadatasync
(
  const Path& path,
  const XCap& write_xcap
)
{
  VOLUME_OPERATION_BEGIN( metadatasync )
  {
    stat_cache->metadatasync( path, write_xcap );
  }
  VOLUME_OPERATION_END( metadatasync );
}

bool Volume::mkdir( const Path& path, mode_t mode )
{
  stat_cache->evict( path.parent_path() );

  VOLUME_OPERATION_BEGIN( mkdir )
  {
    mrc_proxy.mkdir( name_utf8, path.encode( iconv::CODE_UTF8 ), mode );
    return true;
  }
  VOLUME_OPERATION_END( mkdir );
  return false;
}

yield::platform::File*
Volume::open
(
  const Path& path,
  uint32_t flags,
  mode_t mode,
  uint32_t attributes
)
{
  VOLUME_OPERATION_BEGIN( open )
  {
    uint32_t system_v_flags;

#ifdef _WIN32
    system_v_flags = flags;
#else
    system_v_flags = 0;

    if ( ( flags & O_SYNC ) == O_SYNC )
    {
      system_v_flags |= SYSTEM_V_FCNTL_H_O_SYNC;
      flags ^= O_SYNC;
    }

    // Solaris and Windows have System V O_* constants;
    // on other systems we have to translate from the
    // local constants to System V
#if defined(__FreeBSD__) || defined(__linux__) || defined(__MACH__)
    if ( ( flags & O_WRONLY ) == O_WRONLY )
    {
      system_v_flags |= SYSTEM_V_FCNTL_H_O_WRONLY;
      flags ^= O_WRONLY;
    }

    if ( ( flags & O_RDWR ) == O_RDWR )
    {
      system_v_flags |= SYSTEM_V_FCNTL_H_O_RDWR;
      flags ^= O_RDWR;
    }

    if ( ( flags & O_APPEND ) == O_APPEND )
    {
      system_v_flags |= SYSTEM_V_FCNTL_H_O_APPEND;
      flags ^= O_APPEND;
    }

    if ( ( flags & O_CREAT ) == O_CREAT )
    {
      system_v_flags |= SYSTEM_V_FCNTL_H_O_CREAT;
      flags ^= O_CREAT;
    }

    if ( ( flags & O_TRUNC ) == O_TRUNC )
    {
      system_v_flags |= SYSTEM_V_FCNTL_H_O_TRUNC;
      flags ^= O_TRUNC;
    }

    if ( ( flags & O_EXCL ) == O_EXCL )
    {
      system_v_flags |= SYSTEM_V_FCNTL_H_O_EXCL;
      flags ^= O_EXCL;
    }
#endif
    system_v_flags |= flags;
#endif

    FileCredentials file_credentials;
    mrc_proxy.open
    (
      name_utf8,
      path.encode( iconv::CODE_UTF8 ),
      system_v_flags,
      mode,
      attributes,
      get_vivaldi_coordinates(),
      file_credentials
    );

    file_state_map_lock.acquire();
    FileStateMap::const_iterator file_state_i 
      = file_state_map.find( file_credentials.get_xcap().get_file_id() );
    if ( file_state_i != file_state_map.end() )
    {
      FileState* file_state = file_state_i->second;
      if ( flags == O_RDONLY )
        file_state->inc_reader_count();
      else
        file_state->inc_writer_count();
    }
    else
    {
      FileState* file_state;
      if ( flags == O_RDONLY )
        file_state = new FileState( 1, 0 );
      else
        file_state = new FileState( 0, 1 );
      file_state_map[file_credentials.get_xcap().get_file_id()] = file_state;
    }
    file_state_map_lock.release();

    return new File
               (
                 *this,
                 path,
                 file_credentials.get_xcap(),
                 file_credentials.get_xlocs()
               );
  }
  VOLUME_OPERATION_END( open );
  return NULL;
}

yield::platform::Directory* Volume::opendir( const Path& path  )
{
  VOLUME_OPERATION_BEGIN( opendir )
  {
    DirectoryEntrySet first_directory_entries;

    mrc_proxy.readdir
    (
      name_utf8,
      path.encode( iconv::CODE_UTF8 ),
      0, // known_etag
      Directory::LIMIT_DIRECTORY_ENTRIES_COUNT_DEFAULT,
      false, // names_only
      0, // seen_directory_entries_count
      first_directory_entries
    );

#ifdef _DEBUG
    if ( first_directory_entries.empty() )
      DebugBreak();
#endif

    return new Directory
               (
                 first_directory_entries,
                 false,
                 *this,
                 path
               );
  }
  VOLUME_OPERATION_END( opendir );
  return NULL;
}

void Volume::osd_unlink( const FileCredentials& file_credentials )
{
  const string& file_id = file_credentials.get_xcap().get_file_id();

  for
  (
    ReplicaSet::const_iterator replica_i 
      = file_credentials.get_xlocs().get_replicas().begin();
    replica_i != file_credentials.get_xlocs().get_replicas().end();
    ++replica_i
  )
  {
    auto_Object<OSDProxy> osd_proxy
      = osd_proxies.get_osd_proxy( ( *replica_i ).get_osd_uuids()[0] );
    osd_proxy->unlink( file_credentials, file_id );
  }    
}

Path* Volume::readlink( const Path& path )
{
  VOLUME_OPERATION_BEGIN( readlink )
  {
    string link_target_path;
    mrc_proxy.readlink
    (
      name_utf8,
      path.encode( iconv::CODE_UTF8 ),
      link_target_path
    );
    return new Path( link_target_path, iconv::CODE_UTF8 );
  }
  VOLUME_OPERATION_END( readlink );
  return NULL;
}

bool Volume::removexattr( const Path& path, const string& name )
{
  VOLUME_OPERATION_BEGIN( removexattr )
  {
    mrc_proxy.removexattr( name_utf8, path.encode( iconv::CODE_UTF8 ), name );
    return true;
  }
  VOLUME_OPERATION_END( removexattr );
  return false;
}

bool Volume::rename( const Path& from_path, const Path& to_path )
{
  // Don't evict from_path or to_path in case there are
  // outstanding file size updates
  stat_cache->evict( from_path.parent_path() );
  stat_cache->evict( to_path.parent_path() );

  VOLUME_OPERATION_BEGIN( rename )
  {
    FileCredentialsSet file_credentials_set;

    mrc_proxy.rename
    (
      name_utf8,
      from_path.encode( iconv::CODE_UTF8 ),
      to_path.encode( iconv::CODE_UTF8 ),
      file_credentials_set
    );

    if ( !file_credentials_set.empty() )
      osd_unlink( file_credentials_set[0] );

    return true;
  }
  VOLUME_OPERATION_END( rename );
  return false;
}

bool Volume::rmdir( const Path& path )
{
  VOLUME_OPERATION_BEGIN( rmdir )
  {
    mrc_proxy.rmdir( name_utf8, path.encode( iconv::CODE_UTF8 ) );
    stat_cache->evict( path );
    return true;
  }
  VOLUME_OPERATION_END( rmdir );
  return false;
}

bool
Volume::setattr
(
  const Path& path,
  const yield::platform::Stat& _stbuf,
  uint32_t to_set
)
{
  VOLUME_OPERATION_BEGIN( setattr )
  {
    Stat stbuf( _stbuf );
#ifndef _WIN32
    if
    (
      ( to_set & SETATTR_UID ) == SETATTR_UID &&
      ( to_set & SETATTR_GID ) == SETATTR_GID
    )
    {
      UserCredentials* user_credentials
        = user_credentials_cache.getUserCredentialsFrompasswd
          (
            stbuf.get_uid(),
            stbuf.get_gid()
          );

      if ( user_credentials != NULL )
      {
        stbuf.set_user_id( user_credentials->get_user_id() );
        stbuf.set_group_id( user_credentials->get_group_ids()[0] );
        UserCredentials::dec_ref( *user_credentials );
      }
      else
        throw Exception( "could not look up uid and gid" );
    }
    else if ( ( to_set & SETATTR_UID ) == SETATTR_UID )
    {
      UserCredentials* user_credentials
        = user_credentials_cache.getUserCredentialsFrompasswd
          (
            stbuf.get_uid(),
            stbuf.get_gid()
          );

      if ( user_credentials != NULL )
      {
        stbuf.set_user_id( user_credentials->get_user_id() );
        UserCredentials::dec_ref( *user_credentials );
      }
      else
        throw Exception( "could not look up uid" );
    }
    else if ( ( to_set & SETATTR_GID ) == SETATTR_GID )
    {
      UserCredentials* user_credentials
        = user_credentials_cache.getUserCredentialsFrompasswd
          (
            stbuf.get_uid(),
            stbuf.get_gid()
          );

      if ( user_credentials != NULL )
      {
        stbuf.set_group_id( user_credentials->get_group_ids()[0] );
        UserCredentials::dec_ref( *user_credentials );
      }
      else
        throw Exception( "could not look up gid" );
    }
#endif

    stat_cache->setattr( path, stbuf, to_set );

    return true;
  }
  VOLUME_OPERATION_END( setattr );
  return false;
}

void Volume::set_errno( const char* operation_name, Exception& exception )
{
  uint32_t error_code = exception.get_error_code();

  switch( error_code )
  {
#if defined(_WIN32)
    case EACCES: error_code = ERROR_ACCESS_DENIED;
    case EEXIST: error_code = ERROR_ALREADY_EXISTS;
    case EINVAL: error_code = ERROR_INVALID_PARAMETER;
#elif defined(__FreeBSD__) || defined(__MACH__)
    case 11: return EAGAIN; // Not sure why they renumbered this one.
    case 39: return ENOTEMPTY; // 39 is EDESTADDRREQ on FreeBSD
    case 61: return ENOATTR; // 61 is ENODATA on Linux,
                             // returned when an xattr is not present
#endif
  }

  if ( error_log != NULL )
  {
    switch ( error_code )
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
        error_log->get_stream( Log::LOG_ERR ) <<
          "xtreemfs: caught exception on " <<
          operation_name << ": " << exception;
      }
      break;
    }
  }

#ifdef _WIN32
  SetLastError( error_code );
#else
  errno = static_cast<int>( error_code );
#endif
}

void Volume::set_errno( const char* operation_name, std::exception& exception )
{
  if ( error_log != NULL )
  {
    error_log->get_stream( Log::LOG_ERR ) <<
      "xtreemfs::Volume: caught exception on " <<
      operation_name << ": " << exception.what();
  }

#ifdef _WIN32
  ::SetLastError( ERROR_CODE_DEFAULT );
#else
  errno = ERROR_CODE_DEFAULT;
#endif
}

bool
Volume::setxattr
(
  const Path& path,
  const string& name,
  const string& value,
  int flags
)
{
  VOLUME_OPERATION_BEGIN( setxattr )
  {
    mrc_proxy.setxattr
    (
      name_utf8,
      path.encode( iconv::CODE_UTF8 ),
      name,
      value,
      flags
    );
    return true;
  }
  VOLUME_OPERATION_END( setxattr );
  return false;
}

bool Volume::statvfs( const Path&, struct statvfs& statvfsbuf )
{
  VOLUME_OPERATION_BEGIN( statvfs )
  {
    StatVFSSet xtreemfs_statvfsbuf;
    mrc_proxy.statvfs( name_utf8, 0, xtreemfs_statvfsbuf );
    memset( &statvfsbuf, 0, sizeof( statvfsbuf ) );
    statvfsbuf.f_bavail = xtreemfs_statvfsbuf[0].get_bavail();
    statvfsbuf.f_bfree = xtreemfs_statvfsbuf[0].get_bavail();
    statvfsbuf.f_blocks = xtreemfs_statvfsbuf[0].get_blocks();
    statvfsbuf.f_bsize = xtreemfs_statvfsbuf[0].get_bsize();
    statvfsbuf.f_namemax = xtreemfs_statvfsbuf[0].get_namemax();
    return true;
  }
  VOLUME_OPERATION_END( statvfs );
  return false;
}

bool
Volume::symlink
(
  const Path& to_path,
  const Path& from_path
)
{
  VOLUME_OPERATION_BEGIN( symlink )
  {
    mrc_proxy.symlink
    (
      name_utf8,
      to_path.encode( iconv::CODE_UTF8 ),
      from_path.encode( iconv::CODE_UTF8 )
    );
    stat_cache->evict( from_path.parent_path() );
    return true;
  }
  VOLUME_OPERATION_END( symlink );
  return false;
}

bool Volume::truncate( const Path& path, uint64_t new_size )
{
  VOLUME_OPERATION_BEGIN( truncate )
  {
    yield::platform::File* file
      = yield::platform::Volume::open( path, O_TRUNC );

    if ( file != NULL )
    {
      bool ret = file->truncate( new_size );
      yield::platform::File::dec_ref( *file );
      return ret;
    }
    else
      return false;
  }
  VOLUME_OPERATION_END( truncate );
  return false;
}

bool Volume::unlink( const Path& path )
{
  VOLUME_OPERATION_BEGIN( unlink )
  {
    FileCredentialsSet file_credentials_set;
    mrc_proxy.unlink
    (
      name_utf8,
      path.encode( iconv::CODE_UTF8 ),
      file_credentials_set
    );

    if ( !file_credentials_set.empty() )
      osd_unlink( file_credentials_set[0] );

    // Don't stat_cache->evict( path ) in case there are
    // outstanding file size updates

    return true;
  }
  VOLUME_OPERATION_END( unlink );
  return false;
}

Path Volume::volname( const Path& )
{
  return name_utf8;
}
