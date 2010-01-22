// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/osd_proxy.h"
using namespace org::xtreemfs::interfaces;
using namespace xtreemfs;


auto_OSDProxy 
OSDProxy::create
( 
  const YIELD::ipc::URI& absolute_uri,
  uint16_t concurrency_level,
  uint32_t flags,
  YIELD::platform::auto_Log log,
  const YIELD::platform::Time& operation_timeout,
  uint8_t reconnect_tries_max,
  YIELD::ipc::auto_SSLContext ssl_context,
  auto_UserCredentialsCache user_credentials_cache
)
{
  YIELD::ipc::URI checked_uri( absolute_uri );

  if ( checked_uri.get_port() == 0 )
  {
    if ( checked_uri.get_scheme() == ONCRPCG_SCHEME )
      checked_uri.set_port( ONCRPCG_PORT_DEFAULT );
    else if ( checked_uri.get_scheme() == ONCRPCS_SCHEME )
      checked_uri.set_port( ONCRPCS_PORT_DEFAULT );
    else if ( checked_uri.get_scheme() == ONCRPCU_SCHEME )
      checked_uri.set_port( ONCRPCU_PORT_DEFAULT );
    else
      checked_uri.set_port( ONCRPC_PORT_DEFAULT );
  }  

  if ( user_credentials_cache == NULL )
    user_credentials_cache = new UserCredentialsCache;

  return new OSDProxy
  ( 
    concurrency_level,
    flags, 
    log, 
    operation_timeout, 
    YIELD::ipc::SocketAddress::create( checked_uri ), 
    reconnect_tries_max, 
    createSocketFactory( checked_uri, ssl_context ),
    user_credentials_cache
  );
}


bool operator>
( 
  const org::xtreemfs::interfaces::OSDWriteResponse& left, 
  const org::xtreemfs::interfaces::OSDWriteResponse& right 
)
{
  if ( left.get_new_file_size().empty() )
    return false;

  else if ( right.get_new_file_size().empty() )
    return true;

  else if 
  ( 
    left.get_new_file_size()[0].get_truncate_epoch() > 
    right.get_new_file_size()[0].get_truncate_epoch() 
  )
    return true;

  else if 
  ( 
    left.get_new_file_size()[0].get_truncate_epoch() == 
      right.get_new_file_size()[0].get_truncate_epoch() 
    &&
    left.get_new_file_size()[0].get_size_in_bytes() > 
      right.get_new_file_size()[0].get_size_in_bytes() 
  )
    return true;

  else
    return false;
}
