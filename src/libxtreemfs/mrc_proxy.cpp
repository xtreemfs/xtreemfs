// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/mrc_proxy.h"
#include "xtreemfs/path.h"
using namespace xtreemfs;


void MRCProxy::chown( const Path& path, int uid, int gid )
{
#ifdef _WIN32
  DebugBreak();
#else
  org::xtreemfs::interfaces::UserCredentials user_credentials;
  if ( this->getUserCredentialsFrompasswd( uid, gid, user_credentials ) )
    org::xtreemfs::interfaces::MRCInterface::chown( path, user_credentials.get_user_id(), user_credentials.get_group_ids()[0] );
  else
    throw YIELD::platform::Exception();
#endif
}

auto_MRCProxy MRCProxy::create
( 
  const YIELD::ipc::URI& absolute_uri,
  uint32_t flags,
  YIELD::platform::auto_Log log,
  const YIELD::platform::Time& operation_timeout,
  const char* password,
  uint8_t reconnect_tries_max,
  YIELD::ipc::auto_SSLContext ssl_context 
)
{
  YIELD::ipc::URI checked_uri( absolute_uri );

  if ( checked_uri.get_port() == 0 )
  {
    if ( checked_uri.get_scheme() == org::xtreemfs::interfaces::ONCRPCG_SCHEME )
      checked_uri.set_port( org::xtreemfs::interfaces::MRCInterface::ONCRPCG_PORT_DEFAULT );
    else if ( checked_uri.get_scheme() == org::xtreemfs::interfaces::ONCRPCS_SCHEME )
      checked_uri.set_port( org::xtreemfs::interfaces::MRCInterface::ONCRPCS_PORT_DEFAULT );
    else if ( checked_uri.get_scheme() == org::xtreemfs::interfaces::ONCRPCU_SCHEME )
      checked_uri.set_port( org::xtreemfs::interfaces::MRCInterface::ONCRPCU_PORT_DEFAULT );
    else
      checked_uri.set_port( org::xtreemfs::interfaces::MRCInterface::ONCRPC_PORT_DEFAULT );
  }  

  YIELD::ipc::auto_SocketAddress peername = YIELD::ipc::SocketAddress::create( checked_uri );
  if ( peername != NULL )
    return new MRCProxy( flags, log, operation_timeout, password, peername, reconnect_tries_max, createSocketFactory( checked_uri, ssl_context ) );
  else
    throw YIELD::platform::Exception();
}

void MRCProxy::getattr( const Path& path, org::xtreemfs::interfaces::Stat& stbuf )
{
  org::xtreemfs::interfaces::MRCInterface::getattr( path, stbuf );
#ifndef _WIN32
  int uid, gid;
  this->getpasswdFromUserCredentials( stbuf.get_user_id(), stbuf.get_group_id(), uid, gid );
  stbuf.set_uid( uid );
  stbuf.set_gid( gid );
#endif
}

void MRCProxy::getCurrentUserCredentials( org::xtreemfs::interfaces::UserCredentials& out_user_credentials )
{
  Proxy<MRCProxy, org::xtreemfs::interfaces::MRCInterface>::getCurrentUserCredentials( out_user_credentials );
  out_user_credentials.set_password( password );
}

void MRCProxy::readdir( const Path& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries )
{
  org::xtreemfs::interfaces::MRCInterface::readdir( path, directory_entries );
#ifndef _WIN32
  for ( org::xtreemfs::interfaces::DirectoryEntrySet::iterator directory_entry_i = directory_entries.begin(); directory_entry_i != directory_entries.end(); directory_entry_i++ )
  {
    int uid, gid;
    this->getpasswdFromUserCredentials( ( *directory_entry_i ).get_stbuf().get_user_id(), ( *directory_entry_i ).get_stbuf().get_group_id(), uid, gid );
    org::xtreemfs::interfaces::Stat new_stbuf = ( *directory_entry_i ).get_stbuf();
    new_stbuf.set_uid( uid );
    new_stbuf.set_gid( gid );
    ( *directory_entry_i ).set_stbuf( new_stbuf );
  }
#endif
}
