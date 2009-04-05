// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/volume.h"
#include "org/xtreemfs/client/dir_proxy.h"
#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/client/osd_proxy_factory.h"
#include "org/xtreemfs/client/path.h"
#include "open_file.h"
#include "shared_file.h"
#include "policy_container.h"
using namespace org::xtreemfs::client;

#include <errno.h>
#ifndef _WIN32
#include <sys/statvfs.h>
#endif


Volume::Volume( const std::string& name, DIRProxy& dir_proxy, MRCProxy& mrc_proxy, OSDProxyFactory& osd_proxy_factory )
: name( name ), dir_proxy( dir_proxy ), mrc_proxy( mrc_proxy ), osd_proxy_factory( osd_proxy_factory )
{ }

bool Volume::access( const YIELD::Path& path, int amode )
{
  return mrc_proxy.access( Path( this->name, path ), amode );
}

bool Volume::chmod( const YIELD::Path& path, mode_t mode )
{
  mrc_proxy.chmod( Path( this->name, path ), mode );
  return true;
}

bool Volume::chown( const YIELD::Path& path, int uid, int gid )
{
  mrc_proxy.chown( Path( this->name, path ), uid, gid );
  return true;
}

YIELD::auto_SharedObject<YIELD::Stat> Volume::getattr( const YIELD::Path& path )
{
  return getattr( Path( this->name, path ) );
}

YIELD::auto_SharedObject<YIELD::Stat> Volume::getattr( const Path& path )
{
  org::xtreemfs::interfaces::Stat stbuf;
  mrc_proxy.getattr( path, stbuf );
#ifdef _WIN32
  return new YIELD::Stat( stbuf.get_mode(), stbuf.get_size(), stbuf.get_atime_ns(), stbuf.get_mtime_ns(), stbuf.get_ctime_ns(), stbuf.get_attributes() );
#else
  return new YIELD::Stat( stbuf.get_mode(), stbuf.get_nlink(), stbuf.get_uid(), stbuf.get_gid(), stbuf.get_size(), stbuf.get_atime_ns(), stbuf.get_mtime_ns(), stbuf.get_ctime_ns() );
#endif
}

bool Volume::getxattr( const YIELD::Path& path, const std::string& name, std::string& out_value )
{
  mrc_proxy.getxattr( Path( this->name, path ), name, out_value );
  return true;
}

bool Volume::link( const YIELD::Path& old_path, const YIELD::Path& new_path )
{
  mrc_proxy.link( Path( this->name, old_path ), Path( this->name, new_path ) );
  return true;
}

bool Volume::listxattr( const YIELD::Path& path, std::vector<std::string>& out_names )
{
  xtreemfs::interfaces::StringSet names;
  mrc_proxy.listxattr( Path( this->name, path ), names );
  out_names.assign( names.begin(), names.end() );
  return true;
}

bool Volume::mkdir( const YIELD::Path& path, mode_t mode )
{
  mrc_proxy.mkdir( Path( this->name, path ), mode );
  return true;
}

YIELD::auto_SharedObject<YIELD::File> Volume::open( const YIELD::Path& _path, uint32_t flags, mode_t mode )
{
  Path path( this->name, _path );

  uint32_t system_v_flags = 0;
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
  system_v_flags |= flags;
#else
  system_v_flags = flags;
#endif

  org::xtreemfs::interfaces::FileCredentials file_credentials;
  mrc_proxy.open( path, system_v_flags, mode, file_credentials );
  uint32_t path_hash = YIELD::string_hash( path );

  SharedFile* shared_file = in_use_shared_files.find( path_hash );
  if ( shared_file == NULL )
  {
    shared_file = new SharedFile( *this, path, file_credentials.get_xlocs() );
    in_use_shared_files.insert( path_hash, shared_file );
    OpenFile& open_file = shared_file->open( file_credentials );
    YIELD::SharedObject::decRef( *shared_file ); // every open creates a new reference to the SharedFile; decRef the original reference here so that the last released'd OpenFile will delete the SharedFile
    return &open_file;
  }
  else
    return &shared_file->open( file_credentials );
}

bool Volume::readdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, YIELD::Volume::readdirCallback& callback )
{
  org::xtreemfs::interfaces::DirectoryEntrySet directory_entries;
  mrc_proxy.readdir( Path( this->name, path ), directory_entries );
  for ( org::xtreemfs::interfaces::DirectoryEntrySet::const_iterator directory_entry_i = directory_entries.begin(); directory_entry_i != directory_entries.end(); directory_entry_i++ )
  {
    const org::xtreemfs::interfaces::Stat& xtreemfs_stat = ( *directory_entry_i ).get_stbuf();
#ifdef _WIN32
    YIELD::Stat yield_stat( xtreemfs_stat.get_mode(), xtreemfs_stat.get_size(), xtreemfs_stat.get_atime_ns(), xtreemfs_stat.get_mtime_ns(), xtreemfs_stat.get_ctime_ns(), xtreemfs_stat.get_attributes() );
#else
    YIELD::Stat yield_stat( xtreemfs_stat.get_mode(), xtreemfs_stat.get_nlink(), xtreemfs_stat.get_uid(), xtreemfs_stat.get_gid(), xtreemfs_stat.get_size(), xtreemfs_stat.get_atime_ns(), xtreemfs_stat.get_mtime_ns(), xtreemfs_stat.get_ctime_ns() );
#endif
    if ( !callback( ( *directory_entry_i ).get_name(), yield_stat ) )
      return false;
  }
  return true;
}

YIELD::Path Volume::readlink( const YIELD::Path& path )
{
  org::xtreemfs::interfaces::Stat stbuf;
  mrc_proxy.getattr( Path( this->name, path ), stbuf );
  return stbuf.get_link_target();
}

bool Volume::rename( const YIELD::Path& from_path, const YIELD::Path& to_path )
{
  Path from_xtreemfs_path( this->name, from_path ), to_xtreemfs_path( this->name, to_path );
  org::xtreemfs::interfaces::FileCredentialsSet file_credentials_set;
  mrc_proxy.rename( from_xtreemfs_path, to_xtreemfs_path, file_credentials_set );
  osd_unlink( file_credentials_set );
  return true;
}

bool Volume::rmdir( const YIELD::Path& path )
{
  mrc_proxy.rmdir( Path( this->name, path ) );
  return true;
}

bool Volume::removexattr( const YIELD::Path& path, const std::string& name )
{
  mrc_proxy.removexattr( Path( this->name, path ), name );
  return true;
}

bool Volume::setattr( const YIELD::Path& path, uint32_t file_attributes )
{
  xtreemfs::interfaces::Stat stbuf;
  stbuf.set_attributes( file_attributes );
  mrc_proxy.setattr( Path( this->name, path ), stbuf );
  return true;
}

bool Volume::setxattr( const YIELD::Path& path, const std::string& name, const std::string& value, int flags )
{
  mrc_proxy.setxattr( Path( this->name, path ), name, value, flags );
  return true;
}

bool Volume::statvfs( const YIELD::Path& path, struct statvfs* statvfsbuf )
{
  if ( statvfsbuf )
  {
    memset( statvfsbuf, 0, sizeof( *statvfsbuf ) );
    xtreemfs::interfaces::StatVFS xtreemfs_statvfsbuf;
    mrc_proxy.statvfs( this->name, xtreemfs_statvfsbuf );
    statvfsbuf->f_bsize = xtreemfs_statvfsbuf.get_bsize();
    statvfsbuf->f_bavail = statvfsbuf->f_bfree = xtreemfs_statvfsbuf.get_bfree();
    statvfsbuf->f_blocks = xtreemfs_statvfsbuf.get_bfree() * 1024;
    statvfsbuf->f_namemax = xtreemfs_statvfsbuf.get_namelen();
    return true;
  }
  else
    return false;
}

bool Volume::symlink( const YIELD::Path& to_path, const YIELD::Path& from_path )
{
  mrc_proxy.symlink( to_path, Path( this->name, from_path ) );
  return true;
}

bool Volume::truncate( const YIELD::Path& path, uint64_t new_size )
{
  YIELD::File* file = this->open( path, O_TRUNC, 0 ).release();
  file->truncate( new_size );
  YIELD::SharedObject::decRef( file );
  return true;
}

bool Volume::unlink( const YIELD::Path& path )
{
  org::xtreemfs::interfaces::FileCredentialsSet file_credentials_set;
  mrc_proxy.unlink( Path( this->name, path ), file_credentials_set );
  osd_unlink( file_credentials_set );
  return true;
}

bool Volume::utimens( const YIELD::Path& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns )
{
  mrc_proxy.utimens( Path( this->name, path ), atime_ns, mtime_ns, ctime_ns );
  return true;
}

YIELD::Path Volume::volname( const YIELD::Path& )
{
  return name;
}

void Volume::release( SharedFile& shared_file )
{
  uint32_t path_hash = YIELD::string_hash( shared_file.get_path() );
  in_use_shared_files.remove( path_hash );
}

void Volume::osd_unlink( const org::xtreemfs::interfaces::FileCredentialsSet& file_credentials_set )
{
  if ( !file_credentials_set.empty() ) // We have to delete files on replica OSDs
  {
    const org::xtreemfs::interfaces::FileCredentials& file_credentials = file_credentials_set[0];
    const std::string& file_id = file_credentials.get_xcap().get_file_id();
    const org::xtreemfs::interfaces::ReplicaSet& replicas = file_credentials.get_xlocs().get_replicas();
    for ( org::xtreemfs::interfaces::ReplicaSet::const_iterator replica_i = replicas.begin(); replica_i != replicas.end(); replica_i++ )
    {
      OSDProxy& osd_proxy = osd_proxy_factory.createOSDProxy( ( *replica_i ).get_osd_uuids()[0] );
      osd_proxy.unlink( file_credentials, file_id );
      YIELD::SharedObject::decRef( osd_proxy );
    }
  }
}
