#include "open_file.h"
#include "file_replica.h"
using namespace org::xtreemfs::client;


OpenFile::OpenFile( const org::xtreemfs::interfaces::FileCredentials& file_credentials, FileReplica& attached_to_file_replica )
: file_credentials( file_credentials ), attached_to_file_replica( attached_to_file_replica )
{ }

OpenFile::~OpenFile()
{
  SharedObject::decRef( attached_to_file_replica.get_parent_shared_file() );
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
