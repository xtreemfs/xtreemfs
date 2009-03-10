#include "shared_file.h"
#include "file_replica.h"
#include "open_file.h"
#include "org/xtreemfs/client/osd_proxy_factory.h"
using namespace org::xtreemfs::client;

using namespace org::xtreemfs::interfaces;

#include <sstream>


SharedFile::SharedFile( Volume& parent_volume, const Path& path, const XLocSet& xlocs )
: parent_volume( parent_volume ), path( path ), xlocs( xlocs )
{
  for ( ReplicaSet::const_iterator replica_i = xlocs.get_replicas().begin(); replica_i != xlocs.get_replicas().end(); replica_i++ )
  {
    const StringSet& osd_uuids = ( *replica_i ).get_osd_uuids();
    const StripingPolicy& striping_policy = ( *replica_i ).get_striping_policy();
    for ( StringSet::const_iterator osd_uuid_i = osd_uuids.begin(); osd_uuid_i != osd_uuids.end(); osd_uuid_i++ )
    {
      FileReplica* file_replica = new FileReplica( *this, *osd_uuid_i, xlocs.get_version(), striping_policy );
      file_replicas.push_back( file_replica );
    }
  }
}

SharedFile::~SharedFile()
{
  for ( std::vector<FileReplica*>::iterator file_replica_i = file_replicas.begin(); file_replica_i != file_replicas.end(); file_replica_i++ )
    delete *file_replica_i;

  static_cast<SharedFileCallbackInterface&>( parent_volume ).close( *this );
}

YIELD::Stat SharedFile::fgetattr()
{
  return parent_volume.getattr( path );
}

void SharedFile::ftruncate( uint64_t new_size, const FileCredentials& file_credentials )
{
  YIELD::DebugBreak();
}

OpenFile& SharedFile::open( uint64_t open_flags, const FileCredentials& file_credentials )
{
  OpenFile* open_file = new OpenFile( *file_replicas[0], open_flags, file_credentials );
  SharedObject::incRef( *this );
  return *open_file;
}

size_t SharedFile::read( char* rbuf, size_t size, off_t offset, const FileCredentials& file_credentials )
{
  YIELD::DebugBreak();
  return 0;
}

size_t SharedFile::write( const char* wbuf, size_t size, off_t offset, const FileCredentials& file_credentials )
{
  YIELD::DebugBreak();
  return 0;
}

