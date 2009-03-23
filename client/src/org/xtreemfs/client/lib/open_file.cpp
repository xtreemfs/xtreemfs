#include "open_file.h"
#include "file_replica.h"
#include "org/xtreemfs/client/mrc_proxy.h"
using namespace org::xtreemfs::client;


OpenFile::OpenFile( const org::xtreemfs::interfaces::FileCredentials& file_credentials, FileReplica& attached_to_file_replica )
: file_credentials( file_credentials ), attached_to_file_replica( attached_to_file_replica )
{ }

OpenFile::~OpenFile()
{
  SharedObject::decRef( attached_to_file_replica.get_parent_shared_file() );
}

YIELD::Stat* OpenFile::getattr()
{
  org::xtreemfs::interfaces::stat_ stbuf;
  attached_to_file_replica.get_mrc_proxy().getattr( attached_to_file_replica.get_parent_shared_file().get_path(), stbuf );
  return new YIELD::Stat( stbuf.get_mode(), stbuf.get_size(), stbuf.get_mtime(), stbuf.get_ctime(), stbuf.get_atime(),
#ifdef _WIN32
                      stbuf.get_attributes()
#else
                      stbuf.get_nlink()
#endif
                    );
}

ssize_t OpenFile::read( void* rbuf, size_t size, off_t offset )
{
  return attached_to_file_replica.read( file_credentials, rbuf, size, offset );
}

bool OpenFile::truncate( uint64_t new_size )
{
  attached_to_file_replica.truncate( file_credentials, new_size );
  return true;
}

ssize_t OpenFile::write( const void* wbuf, size_t size, off_t offset )
{
  return attached_to_file_replica.write( file_credentials, wbuf, size, offset );
}
