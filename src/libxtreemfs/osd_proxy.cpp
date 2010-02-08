// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the XtreemFS project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the XtreemFS project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


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


bool
operator>
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
