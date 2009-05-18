// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/mrc_proxy.h"
#include "policy_container.h"
using namespace org::xtreemfs::client;

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "org/xtreemfs/interfaces/mrc_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif


MRCProxy::MRCProxy( YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::SocketFactory> socket_factory )
  : YIELD::ONCRPCClient( new org::xtreemfs::interfaces::MRCInterface, log, operation_timeout, peer_sockaddr, reconnect_tries_max, socket_factory )
{
  policies = new PolicyContainer;
}

MRCProxy::~MRCProxy()
{
  YIELD::Object::decRef( *policies );
}

YIELD::auto_Object<YIELD::Request> MRCProxy::createProtocolRequest( YIELD::auto_Object<YIELD::Request> request )
{
  YIELD::auto_Object<org::xtreemfs::interfaces::UserCredentials> user_credentials = new org::xtreemfs::interfaces::UserCredentials;
  policies->getCurrentUserCredentials( *user_credentials.get() );
  YIELD::auto_Object<YIELD::Request> oncrpc_request = YIELD::ONCRPCClient::createProtocolRequest( request );
  static_cast<YIELD::ONCRPCRequest*>( oncrpc_request.get() )->set_credential_auth_flavor( org::xtreemfs::interfaces::ONCRPC_AUTH_FLAVOR );
  static_cast<YIELD::ONCRPCRequest*>( oncrpc_request.get() )->set_credential( user_credentials.release() );
  return oncrpc_request;
}

bool MRCProxy::access( const Path& path, uint32_t mode )
{
  return static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).access( path, mode, this );
}

void MRCProxy::chmod( const Path& path, uint32_t mode )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).chmod( path, mode, this );
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
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).chown( path, user_id, group_id, this );
}

void MRCProxy::ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).ftruncate( write_xcap, truncate_xcap, this );
}

void MRCProxy::getattr( const Path& path, org::xtreemfs::interfaces::Stat& stbuf )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).getattr( path, stbuf, this );
#ifndef _WIN32
  int uid, gid;
  policies->getpasswdFromUserCredentials( stbuf.get_user_id(), stbuf.get_group_id(), uid, gid );
  stbuf.set_uid( uid );
  stbuf.set_gid( gid );
#endif
}

void MRCProxy::getxattr( const Path& path, const std::string& name, std::string& value )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).getxattr( path, name, value, this );
}

void MRCProxy::link( const std::string& target_path, const std::string& link_path )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).link( target_path, link_path, this );
}

void MRCProxy::listdir( const Path& path, org::xtreemfs::interfaces::StringSet& names )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).xtreemfs_listdir( path, names, this );
}

void MRCProxy::listxattr( const Path& path, org::xtreemfs::interfaces::StringSet& names )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).listxattr( path, names, this );
}

void MRCProxy::lsvol( org::xtreemfs::interfaces::VolumeSet& volumes )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).xtreemfs_lsvol( volumes, this );
}

void MRCProxy::mkdir( const Path& path, uint32_t mode )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).mkdir( path, mode, this );
}

void MRCProxy::mkvol( const org::xtreemfs::interfaces::Volume& volume )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).xtreemfs_mkvol( volume, this );
}

void MRCProxy::open( const Path& path, uint32_t flags, uint32_t mode, uint32_t attributes, org::xtreemfs::interfaces::FileCredentials& file_credentials )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).open( path, flags, mode, attributes, file_credentials, this );
}

void MRCProxy::readdir( const Path& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).readdir( path, directory_entries, this );
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
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).removexattr( path, name, this );
}

void MRCProxy::rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).rename( source_path, target_path, file_credentials, this );
}

void MRCProxy::renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).xtreemfs_renew_capability( old_xcap, renewed_xcap, this );
}

void MRCProxy::replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).xtreemfs_replica_list( file_id, replicas, this );
}

void MRCProxy::rmdir( const Path& path )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).rmdir( path, this );
}

void MRCProxy::rmvol( const std::string& volume_name )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).xtreemfs_rmvol( volume_name, this );
}

void MRCProxy::setattr( const Path& path, const org::xtreemfs::interfaces::Stat& stbuf )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).setattr( path, stbuf, this );
}

void MRCProxy::setxattr( const Path& path, const std::string& name, const std::string& value, int32_t flags )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).setxattr( path, name, value, flags, this );
}

void MRCProxy::statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& buf )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).statvfs( volume_name, buf, this );
}

void MRCProxy::symlink( const std::string& target_path, const std::string& link_path )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).symlink( target_path, link_path, this );
}

void MRCProxy::unlink( const Path& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).unlink( path, file_credentials, this );
}

void MRCProxy::update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).xtreemfs_update_file_size( xcap, osd_write_response, this );
}

void MRCProxy::utimens( const Path& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns )
{
  static_cast<org::xtreemfs::interfaces::MRCInterface&>( *_interface ).utimens( path, atime_ns, mtime_ns, ctime_ns, this );
}

