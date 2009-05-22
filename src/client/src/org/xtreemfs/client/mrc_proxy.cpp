// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/mrc_proxy.h"
#include "policy_container.h"
using namespace org::xtreemfs::client;


MRCProxy::MRCProxy( YIELD::auto_Object<YIELD::FDAndInternalEventQueue> fd_event_queue, YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::SocketFactory> socket_factory )
  : YIELD::ONCRPCClient<org::xtreemfs::interfaces::MRCInterface>( fd_event_queue, log, operation_timeout, peer_sockaddr, reconnect_tries_max, socket_factory )
{
  policies = new PolicyContainer;
}

MRCProxy::~MRCProxy()
{
  YIELD::Object::decRef( *policies );
}

YIELD::auto_Object<YIELD::ONCRPCRequest> MRCProxy::createProtocolRequest( YIELD::auto_Object<YIELD::Request> request )
{
  YIELD::auto_Object<org::xtreemfs::interfaces::UserCredentials> user_credentials = new org::xtreemfs::interfaces::UserCredentials;
  policies->getCurrentUserCredentials( *user_credentials.get() );
  YIELD::auto_Object<YIELD::ONCRPCRequest> oncrpc_request = YIELD::ONCRPCClient<MRCInterface>::createProtocolRequest( request );
  oncrpc_request->set_credential_auth_flavor( org::xtreemfs::interfaces::ONCRPC_AUTH_FLAVOR );
  oncrpc_request->set_credential( user_credentials.release() );
  return oncrpc_request;
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

void MRCProxy::getattr( const Path& path, org::xtreemfs::interfaces::Stat& stbuf )
{
  org::xtreemfs::interfaces::MRCInterface::getattr( path, stbuf );
#ifndef _WIN32
  int uid, gid;
  policies->getpasswdFromUserCredentials( stbuf.get_user_id(), stbuf.get_group_id(), uid, gid );
  stbuf.set_uid( uid );
  stbuf.set_gid( gid );
#endif
}

void MRCProxy::readdir( const Path& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries )
{
  org::xtreemfs::interfaces::MRCInterface::readdir( path, directory_entries );
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
