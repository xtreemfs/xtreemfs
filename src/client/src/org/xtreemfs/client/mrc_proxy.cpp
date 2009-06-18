// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/mrc_proxy.h"
using namespace org::xtreemfs::client;


void MRCProxy::chown( const Path& path, int uid, int gid )
{
#ifdef _WIN32
  YIELD::DebugBreak();
#else
  org::xtreemfs::interfaces::UserCredentials user_credentials;
  this->getUserCredentialsFrompasswd( uid, gid, user_credentials );
  org::xtreemfs::interfaces::MRCInterface::chown( path, user_credentials.get_user_id(), user_credentials.get_group_ids()[0] );
#endif
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
