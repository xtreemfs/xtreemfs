#include "org/xtreemfs/client/volume.h"
#include "org/xtreemfs/client/dir_proxy.h"
#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/client/osd_proxy_factory.h"
#include "org/xtreemfs/client/path.h"
#include "open_file.h"
using namespace org::xtreemfs::client;

#include <errno.h>


Volume::Volume( const std::string& name, DIRProxy& dir_proxy, MRCProxy& mrc_proxy, OSDProxyFactory& osd_proxy_factory )
: name( name ), dir_proxy( dir_proxy ), mrc_proxy( mrc_proxy ), osd_proxy_factory( osd_proxy_factory )
{
// dir_proxy_operations_timeout_ms = 2000;
//  mrc_proxy_operation_timeout_ms = 2000;
//  osd_proxy_operation_timeout_ms = 2000;
  dir_proxy_operation_timeout_ms = static_cast<uint64_t>( -1 );
  mrc_proxy_operation_timeout_ms = static_cast<uint64_t>( -1 );
  osd_proxy_operation_timeout_ms = static_cast<uint64_t>( -1 );
  test_context.set_user_id( "test" );
  org::xtreemfs::interfaces::StringSet group_ids; group_ids.push_back( "test" );
  test_context.set_group_ids( group_ids );
}

YIELD::Path Volume::get_name() const
{
  return name;
}

bool Volume::access( const YIELD::Path& path, mode_t mode )
{
  return mrc_proxy.access( test_context, Path( this->name, path ), mode, mrc_proxy_operation_timeout_ms );
}

void Volume::create( const YIELD::Path& path, mode_t mode )
{
  mrc_proxy.create( test_context, Path( this->name, path ), mode, mrc_proxy_operation_timeout_ms );
}

void Volume::chmod( const YIELD::Path& path, mode_t mode )
{
  mrc_proxy.chmod( test_context, Path( this->name, path ), mode );
}

void Volume::chown( const YIELD::Path&, int uid, int gid )
{
}

YIELD::Stat Volume::getattr( const YIELD::Path& path )
{
  return getattr( Path( this->name, path ) );
}

YIELD::Stat Volume::getattr( const Path& path )
{
  xtreemfs::interfaces::stat_ stbuf;
  mrc_proxy.getattr( test_context, path, stbuf, mrc_proxy_operation_timeout_ms );
  return YIELD::Stat( stbuf.get_mode(), stbuf.get_size(), stbuf.get_mtime(), stbuf.get_ctime(), stbuf.get_atime(), 
#ifdef _WIN32
                      stbuf.get_attributes()
#else
                      stbuf.get_nlink()
#endif
                    );
}

std::string Volume::getxattr( const YIELD::Path& path, const std::string& name )
{
  return mrc_proxy.getxattr( test_context, Path( this->name, path ), name, mrc_proxy_operation_timeout_ms );
}

void Volume::link( const YIELD::Path& old_path, const YIELD::Path& new_path )
{
  mrc_proxy.link( test_context, Path( this->name, old_path ), Path( this->name, new_path ), mrc_proxy_operation_timeout_ms );
}

void Volume::listxattr( const YIELD::Path& path, yieldfs::StringSet& out_names )
{
  xtreemfs::interfaces::StringSet names;
  mrc_proxy.listxattr( test_context, Path( this->name, path ), names );
  out_names.assign( names.begin(), names.end() );
}

void Volume::mkdir( const YIELD::Path& path, mode_t mode )
{
  mrc_proxy.mkdir( test_context, Path( this->name, path ), mode, mrc_proxy_operation_timeout_ms );
}

yieldfs::FileInterface& Volume::open( const YIELD::Path& path, uint32_t flags, mode_t mode )
{
  return mrc_and_local_open( Path( this->name, path ), flags, mode );
}

void Volume::readdir( const YIELD::Path& path, yieldfs::DirectoryFillerInterface& directory_filler )
{
  org::xtreemfs::interfaces::DirectoryEntrySet directory_entries;
  mrc_proxy.readdir( test_context, Path( this->name, path ), directory_entries );
  for ( org::xtreemfs::interfaces::DirectoryEntrySet::const_iterator directory_entry_i = directory_entries.begin(); directory_entry_i != directory_entries.end(); directory_entry_i++ )
  {
    YIELD::DirectoryEntry directory_entry( ( *directory_entry_i ).get_entry_name(),
                                          ( *directory_entry_i ).get_stbuf().get_mode(),
                                          ( *directory_entry_i ).get_stbuf().get_size(),
                                          ( *directory_entry_i ).get_stbuf().get_mtime(),
                                          ( *directory_entry_i ).get_stbuf().get_ctime(),
                                          ( *directory_entry_i ).get_stbuf().get_atime(),
#ifdef _WIN32
                                          ( *directory_entry_i ).get_stbuf().get_attributes()
#else
                                          ( *directory_entry_i ).get_stbuf().get_nlink()
#endif
                                         );

    directory_filler.fill( directory_entry );
  }
}

YIELD::Path Volume::readlink( const YIELD::Path& path )
{
  org::xtreemfs::interfaces::stat_ stbuf;
  mrc_proxy.getattr( test_context, Path( this->name, path ), stbuf, mrc_proxy_operation_timeout_ms );
  return stbuf.get_link_target();
}

void Volume::rename( const YIELD::Path& from_path, const YIELD::Path& to_path )
{
  Path from_xtreemfs_path( this->name, from_path ), to_xtreemfs_path( this->name, to_path );
  org::xtreemfs::interfaces::FileCredentialsSet file_credentials_set;
  mrc_proxy.rename( test_context, from_xtreemfs_path, to_xtreemfs_path, file_credentials_set, mrc_proxy_operation_timeout_ms );
  osd_unlink( file_credentials_set );
}

void Volume::rmdir( const YIELD::Path& path )
{
  mrc_proxy.rmdir( test_context, Path( this->name, path ), mrc_proxy_operation_timeout_ms );
}

void Volume::removexattr( const YIELD::Path& path, const std::string& name )
{
  mrc_proxy.removexattr( test_context, Path( this->name, path ), name, mrc_proxy_operation_timeout_ms );
}

void Volume::setattr( const YIELD::Path& path, uint32_t file_attributes )
{
  xtreemfs::interfaces::stat_ stbuf; stbuf.set_attributes( file_attributes );
  mrc_proxy.setattr( test_context, Path( this->name, path ), stbuf );
}

void Volume::setxattr( const YIELD::Path& path, const std::string& name, const std::string& value, int flags )
{
  mrc_proxy.setxattr( test_context, Path( this->name, path ), name, value, flags );
}

void Volume::statfs( uint32_t& out_block_size, uint64_t& out_blocks_free, std::string& out_volumeID, uint32_t& out_namelen )
{
  xtreemfs::interfaces::statfs_ statfsbuf;
  mrc_proxy.statfs( test_context, this->name, statfsbuf );
  out_block_size = statfsbuf.get_bsize();
  out_blocks_free = statfsbuf.get_bfree();
  out_volumeID = statfsbuf.get_fsid();
  out_namelen = statfsbuf.get_namelen();
}

void Volume::symlink( const YIELD::Path& to_path, const YIELD::Path& from_path )
{
  mrc_proxy.symlink( test_context, to_path, Path( this->name, from_path ), mrc_proxy_operation_timeout_ms );
}

void Volume::truncate( const YIELD::Path& path, off_t new_size )
{
  OpenFile& open_file = mrc_and_local_open( Path( this->name, path ), O_TRUNC, 0 );
  open_file.truncate( new_size );
  YIELD::SharedObject::decRef( open_file );
}

void Volume::unlink( const YIELD::Path& path )
{
  org::xtreemfs::interfaces::FileCredentialsSet file_credentials_set;
  mrc_proxy.unlink( test_context, Path( this->name, path ), file_credentials_set, mrc_proxy_operation_timeout_ms );
  osd_unlink( file_credentials_set );
}

void Volume::utime( const YIELD::Path& path, uint64_t _ctime, uint64_t _atime, uint64_t _mtime )
{
  mrc_proxy.utime( test_context, Path( this->name, path ), _ctime, _atime, _mtime, mrc_proxy_operation_timeout_ms );
}

OpenFile& Volume::mrc_and_local_open( const Path& path, uint32_t flags, mode_t mode )
{
  org::xtreemfs::interfaces::FileCredentials file_credentials;
  mrc_proxy.open( test_context, path, flags, mode, file_credentials, mrc_proxy_operation_timeout_ms );
  return local_open( path, flags, file_credentials );
}

OpenFile& Volume::local_open( const Path& path, uint64_t open_flags, const org::xtreemfs::interfaces::FileCredentials& file_credentials )
{
  uint32_t path_hash = string_hash( path );

  SharedFile* shared_file = in_use_shared_files.find( path_hash );
  if ( shared_file == NULL )
  {
    shared_file = new SharedFile( *this, path, file_credentials.get_xlocs() );
    in_use_shared_files.insert( path_hash, shared_file );
  }

  OpenFile& open_file = shared_file->open( open_flags, file_credentials );

  return open_file;
}

void Volume::close( SharedFile& shared_file )
{
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
      osd_proxy.unlink( file_id, file_credentials, osd_proxy_operation_timeout_ms );
    }
  }
}
