#include "open_file.h"
#include "file_replica.h"
#include "org/xtreemfs/client/volume.h"
using namespace org::xtreemfs::client;


OpenFile::OpenFile( const org::xtreemfs::interfaces::FileCredentials& file_credentials, FileReplica& attached_to_file_replica )
: file_credentials( file_credentials ), attached_to_file_replica( attached_to_file_replica )
{ }

OpenFile::~OpenFile()
{
  SharedObject::decRef( attached_to_file_replica.get_parent_shared_file() );
}

bool OpenFile::close()
{
  YIELD::DebugBreak();
  return false;
}

YIELD::auto_SharedObject<YIELD::Stat> OpenFile::getattr()
{
  return get_parent_volume().getattr( get_path() );
}

bool OpenFile::getxattr( const std::string& name, std::string out_value )
{
  return get_parent_volume().getxattr( get_path(), name, out_value );
}

bool OpenFile::listxattr( std::vector<std::string>& out_names )
{
  return get_parent_volume().listxattr( get_path(), out_names );
}

ssize_t OpenFile::read( void* rbuf, size_t size, off_t offset )
{
  return attached_to_file_replica.read( file_credentials, rbuf, size, offset );
}

bool OpenFile::removexattr( const std::string& name )
{
  return get_parent_volume().removexattr( get_path(), name );
}

bool OpenFile::setxattr( const std::string& name, const std::string& value, int flags )
{
  return get_parent_volume().setxattr( get_path(), name, value, flags );
}

bool OpenFile::truncate( uint64_t new_size )
{
  org::xtreemfs::interfaces::XCap truncate_xcap;
  get_parent_volume().get_mrc_proxy().ftruncate( file_credentials.get_xcap(), truncate_xcap );
  file_credentials.set_xcap( truncate_xcap );
  attached_to_file_replica.truncate( file_credentials, new_size );
  return true;
}

ssize_t OpenFile::write( const void* wbuf, size_t size, off_t offset )
{
  return attached_to_file_replica.write( file_credentials, wbuf, size, offset );
}
