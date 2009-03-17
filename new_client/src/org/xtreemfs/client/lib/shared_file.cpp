#include "shared_file.h"
#include "file_replica.h"
#include "open_file.h"
#include "org/xtreemfs/client/osd_proxy_factory.h"
using namespace org::xtreemfs::client;

#include <sstream>


SharedFile::SharedFile( Volume& parent_volume, const Path& path, const org::xtreemfs::interfaces::XLocSet& xlocs )
: parent_volume( parent_volume ), path( path ), xlocs( xlocs )
{
  for ( org::xtreemfs::interfaces::ReplicaSet::const_iterator replica_i = xlocs.get_replicas().begin(); replica_i != xlocs.get_replicas().end(); replica_i++ )
    file_replicas.push_back( new FileReplica( *this, ( *replica_i ).get_striping_policy(), ( *replica_i ).get_osd_uuids() ) );
}

SharedFile::~SharedFile()
{
  for ( std::vector<FileReplica*>::iterator file_replica_i = file_replicas.begin(); file_replica_i != file_replicas.end(); file_replica_i++ )
    delete *file_replica_i;

  static_cast<SharedFileCallbackInterface&>( parent_volume ).close( *this );
}

YIELD::Stat SharedFile::getattr()
{
  return parent_volume.getattr( path );
}

OpenFile& SharedFile::open( uint64_t open_flags, const org::xtreemfs::interfaces::FileCredentials& file_credentials )
{
  OpenFile* open_file = new OpenFile( *file_replicas[0], open_flags, file_credentials );
  SharedObject::incRef( *this );
  return *open_file;
}

size_t SharedFile::read( char* rbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials )
{
  YIELD::DebugBreak();
  return 0;
}

void SharedFile::truncate( off_t new_size, const org::xtreemfs::interfaces::FileCredentials& file_credentials )
{
  YIELD::DebugBreak();
}

size_t SharedFile::write( const char* wbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials )
{
  YIELD::DebugBreak();
  return 0;
}

