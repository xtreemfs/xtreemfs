// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/interfaces/exceptions.h"
#include "policy_container.h"
using namespace org::xtreemfs::client;


MRCProxy::MRCProxy( const YIELD::SocketAddress& peer_sockaddr, YIELD::auto_Object<YIELD::SocketFactory> socket_factory, YIELD::auto_Object<YIELD::Log> log )
  : YIELD::ONCRPCClient( peer_sockaddr, socket_factory, log )
{
  mrc_interface.registerObjectFactories( *object_factories );
  org::xtreemfs::interfaces::Exceptions().registerObjectFactories( *object_factories );
  policies = new PolicyContainer;
}

MRCProxy::~MRCProxy()
{
  YIELD::Object::decRef( policies );
}

YIELD::auto_Object<YIELD::Request> MRCProxy::createProtocolRequest( YIELD::auto_Object<> body )
{
  YIELD::auto_Object<org::xtreemfs::interfaces::UserCredentials> user_credentials = new org::xtreemfs::interfaces::UserCredentials;
  policies->getCurrentUserCredentials( *user_credentials.get() );
  return new YIELD::ONCRPCRequest( org::xtreemfs::interfaces::ONCRPC_AUTH_FLAVOR, user_credentials.release(), body, get_log() );
}

bool MRCProxy::access( const Path& path, uint32_t mode )
{
  return mrc_interface.access( path, mode, this );
}

void MRCProxy::chmod( const Path& path, uint32_t mode )
{
  mrc_interface.chmod( path, mode, this );
}

void MRCProxy::chown( const Path& path, int uid, int gid )
{
#ifdef _WIN32
  YIELD::DebugBreak();
#else
  org::xtreemfs::interfaces::UserCredentials user_credentials;
  policies->getUserCredentialsFrompasswd( uid, gid, user_credentials );
  chown( path, user_credentials.get_user_id(), user_credentials.get_group_ids()[0] );
#endif
}

void MRCProxy::chown( const Path& path, const std::string& user_id, const std::string& group_id )
{
  mrc_interface.chown( path, user_id, group_id, this );
}

void MRCProxy::create( const Path& path, uint32_t mode )
{
  mrc_interface.create( path, mode, this );
}

void MRCProxy::ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap )
{
  mrc_interface.ftruncate( write_xcap, truncate_xcap, this );
}

void MRCProxy::getattr( const Path& path, org::xtreemfs::interfaces::Stat& stbuf )
{
  mrc_interface.getattr( path, stbuf, this );
#ifndef _WIN32
  int uid, gid;
  policies->getpasswdFromUserCredentials( stbuf.get_user_id(), stbuf.get_group_id(), uid, gid );
  stbuf.set_uid( uid );
  stbuf.set_gid( gid );
#endif
}

void MRCProxy::getxattr( const Path& path, const std::string& name, std::string& value )
{
  mrc_interface.getxattr( path, name, value, this );
}

void MRCProxy::link( const std::string& target_path, const std::string& link_path )
{
  mrc_interface.link( target_path, link_path, this );
}

void MRCProxy::listdir( const Path& path, org::xtreemfs::interfaces::StringSet& names )
{
  mrc_interface.xtreemfs_listdir( path, names, this );
}

void MRCProxy::listxattr( const Path& path, org::xtreemfs::interfaces::StringSet& names )
{
  mrc_interface.listxattr( path, names, this );
}

void MRCProxy::lsvol( org::xtreemfs::interfaces::VolumeSet& volumes )
{
  mrc_interface.xtreemfs_lsvol( volumes, this );
}

void MRCProxy::mkdir( const Path& path, uint32_t mode )
{
  mrc_interface.mkdir( path, mode, this );
}

void MRCProxy::mkvol( const org::xtreemfs::interfaces::Volume& volume )
{
  mrc_interface.xtreemfs_mkvol( volume, this );
}

void MRCProxy::open( const Path& path, uint32_t flags, uint32_t mode, uint32_t attributes, org::xtreemfs::interfaces::FileCredentials& file_credentials )
{
  mrc_interface.open( path, flags, mode, attributes, file_credentials, this );
}

void MRCProxy::readdir( const Path& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries )
{
  mrc_interface.readdir( path, directory_entries, this );
#ifndef _WIN32
  for ( org::xtreemfs::interfaces::DirectoryEntrySet::iterator directory_entry_i = directory_entries.begin(); directory_entry_i != directory_entries.end(); directory_entry_i++ )
  {
    int uid, gid;
    policies->getpasswdFromUserCredentials( ( *directory_entry_i ).get_stbuf().get_user_id(), ( *directory_entry_i ).get_stbuf().get_group_id(), uid, gid );
    org::xtreemfs::interfaces::Stat new_stbuf = ( *directory_entry_i ).get_stbuf();
    new_stbuf.set_uid( uid );
    new_stbuf.set_gid( gid );
    ( *directory_entry_i ).set_stbuf( new_stbuf );
  }
#endif
}

void MRCProxy::removexattr( const Path& path, const std::string& name )
{
  mrc_interface.removexattr( path, name, this );
}

void MRCProxy::rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials )
{
  mrc_interface.rename( source_path, target_path, file_credentials, this );
}

void MRCProxy::renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap )
{
  mrc_interface.xtreemfs_renew_capability( old_xcap, renewed_xcap, this );
}

void MRCProxy::replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas )
{
  mrc_interface.xtreemfs_replica_list( file_id, replicas, this );
}

void MRCProxy::rmdir( const Path& path )
{
  mrc_interface.rmdir( path, this );
}

void MRCProxy::rmvol( const std::string& volume_name )
{
  mrc_interface.xtreemfs_rmvol( volume_name, this );
}

void MRCProxy::setattr( const Path& path, const org::xtreemfs::interfaces::Stat& stbuf )
{
  mrc_interface.setattr( path, stbuf, this );
}

void MRCProxy::setxattr( const Path& path, const std::string& name, const std::string& value, int32_t flags )
{
  mrc_interface.setxattr( path, name, value, flags, this );
}

void MRCProxy::statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& buf )
{
  mrc_interface.statvfs( volume_name, buf, this );
}

void MRCProxy::symlink( const std::string& target_path, const std::string& link_path )
{
  mrc_interface.symlink( target_path, link_path, this );
}

void MRCProxy::unlink( const Path& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials )
{
  mrc_interface.unlink( path, file_credentials, this );
}

void MRCProxy::update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response )
{
  mrc_interface.xtreemfs_update_file_size( xcap, osd_write_response, this );
}

void MRCProxy::utimens( const Path& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns )
{
  mrc_interface.utimens( path, atime_ns, mtime_ns, ctime_ns, this );
}

