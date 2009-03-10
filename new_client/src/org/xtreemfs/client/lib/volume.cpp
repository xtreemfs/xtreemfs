#include "org/xtreemfs/client/volume.h"
#include "org/xtreemfs/client/dir_proxy.h"
#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/client/osd_proxy_factory.h"
#include "org/xtreemfs/client/path.h"
#include "open_file.h"
using namespace org::xtreemfs::client;

using namespace org::xtreemfs::interfaces;

#include <errno.h>


Volume::Volume( const std::string& name, DIRProxy& dir_proxy, MRCProxy& mrc_proxy, OSDProxyFactory& osd_proxy_factory )
: name( name ), dir_proxy( dir_proxy ), mrc_proxy( mrc_proxy ), osd_proxy_factory( osd_proxy_factory )
{
  mrc_proxy_operation_timeout_ms = 2000;
  osd_proxy_operation_timeout_ms = 2000;
}

YIELD::Path Volume::get_name() const
{
  return name;
}

bool Volume::access( const YIELD::Path& path, mode_t mode )
{
  return mrc_proxy.access( Context(), Path( this->name, path ), mode, mrc_proxy_operation_timeout_ms );
}

void Volume::create( const YIELD::Path& path, mode_t mode )
{
  mrc_proxy.create( Context(), xtreemfs::client::Path ( this->name, path ), mode, mrc_proxy_operation_timeout_ms );
}

void Volume::chmod( const YIELD::Path& path, mode_t mode )
{
  mrc_proxy.chmod( Context(), Path( this->name, path ), mode );
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
  mrc_proxy.getattr( Context(), path, stbuf, mrc_proxy_operation_timeout_ms );
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
  return mrc_proxy.getxattr( Context(), Path( this->name, path ), name, mrc_proxy_operation_timeout_ms );
}

void Volume::link( const YIELD::Path& from_path, const YIELD::Path& to_path )
{
  mrc_proxy.link( Context(), Path( this->name, to_path ), Path( this->name, from_path ), mrc_proxy_operation_timeout_ms );
}

void Volume::listxattr( const YIELD::Path& path, yieldfs::xAttrNameSet& xattr_names )
{
  xtreemfs::interfaces::StringSet names;
  mrc_proxy.listxattr( Context(), Path( this->name, path ), names );
  xattr_names.assign( names.begin(), names.end() );
}

void Volume::mkdir( const YIELD::Path& path, mode_t mode )
{
  mrc_proxy.mkdir( Context(), Path( this->name, path ), mode, mrc_proxy_operation_timeout_ms );
}

yieldfs::FileInterface& Volume::open( const YIELD::Path& path, uint32_t flags, mode_t mode )
{
  return mrc_and_local_open( xtreemfs::client::Path( this->name, path ), flags, mode );
}

void Volume::readdir( const YIELD::Path& path, yieldfs::DirectoryFillerInterface& directory_filler )
{
  /*
  xtreemfs::interfaces::DirectoryEntrySet mrc_directory_entries;
  mrc_proxy.readdir( Path( this->name, path ), mrc_directory_entries );
  for ( DirectoryEntrySet::const_iterator mrc_directory_entry_i = mrc_directory_entries.begin(); mrc_directory_entry_i != mrc_directory_entries.end(); mrc_directory_entry_i++ )
  {

    directory_entries.push_back( YIELD::DirectoryEntry( mrc_directory_entry_i->get_path(),
                                                        mrc_directory_entry_i->get_stbuf().get_mode(),
                                                        mrc_directory_entry_i->get_stbuf().get_size(),
                                                        mrc_directory_entry_i->get_stbuf().get_mtime(),
                                                        mrc_directory_entry_i->get_stbuf().get_ctime(),
                                                        mrc_directory_entry_i->get_stbuf().get_atime(),
#ifdef _WIN32
                                                        mrc_directory_entry_i->get_stbuf().get_attributes()
#else
                                                        mrc_directory_entry_i->get_stbuf().get_nlink()
#endif
                                                       )
                               );
  }
  */
}

YIELD::Path Volume::readlink( const YIELD::Path& path )
{
  stat_ stbuf;
  mrc_proxy.getattr( Context(), Path( this->name, path ), stbuf, mrc_proxy_operation_timeout_ms );
  return stbuf.get_link_target();
}

void Volume::rename( const YIELD::Path& from_path, const YIELD::Path& to_path )
{
  Path from_xtreemfs_path( this->name, from_path ), to_xtreemfs_path( this->name, to_path );
  FileCredentialsSet file_credentials_set;
  mrc_proxy.rename( Context(), from_xtreemfs_path, to_xtreemfs_path, file_credentials_set, mrc_proxy_operation_timeout_ms );
/*
  if ( !file_credentials.get_xcap().get_file_id().empty() ) // The target path existed, so we have to delete it at the OSDs
  {
    OSDProxy& osd_proxy = osd_proxy_factory.createOSDProxy( file_credentials.get_xlocs().get_replicas()[0].get_osd_uuids()[0], file_credentials.get_xlocs().get_version() );
    osd_proxy.unlink( file_credentials.get_xcap().get_file_id(), file_credentials, osd_proxy_operation_timeout_ms );
  }
  */
}

void Volume::rmdir( const YIELD::Path& path )
{
  mrc_proxy.rmdir( Context(), Path( this->name, path ), mrc_proxy_operation_timeout_ms );
}

void Volume::removexattr( const YIELD::Path& path, const std::string& name )
{
  mrc_proxy.removexattr( Context(), Path( this->name, path ), name, mrc_proxy_operation_timeout_ms );
}

void Volume::setattr( const YIELD::Path& path, uint32_t file_attributes )
{
  xtreemfs::interfaces::stat_ stbuf; stbuf.set_attributes( file_attributes );
  mrc_proxy.setattr( Context(), Path( this->name, path ), stbuf );
}

void Volume::setxattr( const YIELD::Path& path, const std::string& name, const std::string& value, int flags )
{
  mrc_proxy.setxattr( Context(), Path( this->name, path ), name, value, flags );
}

void Volume::statfs( uint32_t& out_block_size, uint64_t& out_blocks_free, std::string& out_volumeID, uint32_t& out_namelen )
{
  xtreemfs::interfaces::statfs_ statfsbuf;
  mrc_proxy.statfs( Context(), this->name, statfsbuf );
  out_block_size = statfsbuf.get_bsize();
  out_blocks_free = statfsbuf.get_bfree();
  out_volumeID = statfsbuf.get_fsid();
  out_namelen = statfsbuf.get_namelen();
}

void Volume::symlink( const YIELD::Path& to_path, const YIELD::Path& from_path )
{
  mrc_proxy.symlink( Context(), to_path, Path( this->name, from_path ), mrc_proxy_operation_timeout_ms );
}

void Volume::truncate( const YIELD::Path& path, uint64_t new_size )
{
  OpenFile& open_file = mrc_and_local_open( Path( this->name, path ), O_TRUNC, 0 );
  open_file.ftruncate( new_size );
  YIELD::SharedObject::decRef( open_file );
}

void Volume::unlink( const YIELD::Path& path )
{
  FileCredentialsSet file_credentials_set;
  mrc_proxy.unlink( Context(), Path( this->name, path ), file_credentials_set, mrc_proxy_operation_timeout_ms );
  /*
  if ( !xlocs.get_replicas().empty() )
  {
    OSDProxy& osd_proxy = osd_proxy_factory.createOSDProxy( xlocs.get_replicas()[0].get_osd_uuids()[0], xlocs.get_version() );

    try
    {
      osd_proxy.unlink( file_credentials.get_xcap().get_file_id(), file_credentials, osd_proxy_operation_timeout_ms );
    }
    catch ( YIELD::Exception& )
    { }
  }
  */
}

void Volume::utime( const YIELD::Path& path, uint64_t _ctime, uint64_t _atime, uint64_t _mtime )
{
  mrc_proxy.utime( Context(), Path( this->name, path ), _ctime, _atime, _mtime, mrc_proxy_operation_timeout_ms );
}

OpenFile& Volume::mrc_and_local_open( const Path& path, uint32_t flags, mode_t mode )
{
  FileCredentials file_credentials;
  mrc_proxy.open( Context(), path, flags, mode, file_credentials, mrc_proxy_operation_timeout_ms );
  return local_open( path, flags, file_credentials );
}

OpenFile& Volume::local_open( const Path& path, uint64_t open_flags, const FileCredentials& file_credentials )
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