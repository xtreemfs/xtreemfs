#include "open_file.h"
using namespace org::xtreemfs::client;


OpenFile::OpenFile( FileReplica& attached_to_file_replica, uint64_t open_flags, const org::xtreemfs::interfaces::FileCredentials& file_credentials )
: attached_to_file_replica( attached_to_file_replica ), open_flags( open_flags ), file_credentials( file_credentials )
{ }

OpenFile::~OpenFile()
{
  SharedObject::decRef( attached_to_file_replica.get_parent_shared_file() );
}

YIELD::Stat OpenFile::getattr()
{
  return attached_to_file_replica.getattr();
}

size_t OpenFile::read( char* rbuf, size_t size, off_t offset )
{
  return attached_to_file_replica.read( rbuf, size, offset, file_credentials );
}

void OpenFile::truncate( off_t new_size )
{
  attached_to_file_replica.truncate( new_size, file_credentials );
}

size_t OpenFile::write( const char* wbuf, size_t size, off_t offset )
{
  return attached_to_file_replica.write( wbuf, size, offset, file_credentials );
}
