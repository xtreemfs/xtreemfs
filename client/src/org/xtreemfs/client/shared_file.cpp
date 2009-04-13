// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "shared_file.h"
#include "file_replica.h"
#include "open_file.h"
#include "org/xtreemfs/client/osd_proxy_factory.h"
#include "org/xtreemfs/client/volume.h"
using namespace org::xtreemfs::client;


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

  static_cast<SharedFileCallbackInterface&>( parent_volume ).release( *this );
}

OpenFile& SharedFile::open( const org::xtreemfs::interfaces::FileCredentials& file_credentials )
{
  OpenFile* open_file = new OpenFile( file_credentials, *file_replicas[0] );
  Object::incRef( *this );
  return *open_file;
}
