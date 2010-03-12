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


#include "xtreemfs/mrc_proxy.h"
#include "xtreemfs/options.h"
using namespace xtreemfs;

#include "yield.h"
using yield::platform::Exception;


MRCProxy::MRCProxy
(
  uint16_t concurrency_level,
  uint32_t flags,
  IOQueue& io_queue,
  Log* log,
  const Time& operation_timeout,
  const string& password,
  SocketAddress& peername,
  uint16_t reconnect_tries_max,
  SocketFactory& socket_factory,
  UserCredentialsCache* user_credentials_cache
)
: Proxy
  <
    org::xtreemfs::interfaces::MRCInterface,
    org::xtreemfs::interfaces::MRCInterfaceEventFactory,
    org::xtreemfs::interfaces::MRCInterfaceEventSender
  >
  (
    concurrency_level,
    flags,
    io_queue,
    log,
    operation_timeout,
    peername,
    reconnect_tries_max,
    socket_factory,
    user_credentials_cache
  ),
  password( password )
{ }

MRCProxy& 
MRCProxy::create
( 
  const URI& absolute_uri,
  const Options& options,
  const string& password
)
{
  return create
         (
           absolute_uri,
           CONCURRENCY_LEVEL_DEFAULT,
           options.get_proxy_flags(),
           options.get_log(),
           options.get_timeout(),
           password,
           RECONNECT_TRIES_MAX_DEFAULT,
           options.get_ssl_context(),
           NULL
         );
}

MRCProxy&
MRCProxy::create
(
  const URI& absolute_uri,
  uint16_t concurrency_level,
  uint32_t flags,
  Log* log,
  const Time& operation_timeout,
  const string& password,
  uint16_t reconnect_tries_max,
  SSLContext* ssl_context,
  UserCredentialsCache* user_credentials_cache
)
{
  return *new MRCProxy
              (
                concurrency_level,
                flags,
                yield::platform::NBIOQueue::create(),
                log,
                operation_timeout,
                password,
                createSocketAddress( absolute_uri ),
                reconnect_tries_max,
                createSocketFactory( absolute_uri, ssl_context ),
                user_credentials_cache
              );
}

ONCRPCRequest& MRCProxy::createONCRPCRequest( MarshallableObject& body )
{
  UserCredentials* user_credentials;
  if ( get_user_credentials_cache() != NULL )
  {
    user_credentials
      = get_user_credentials_cache()->getCurrentUserCredentials();

    if ( user_credentials != NULL )
      user_credentials->set_password( password );
  }
  else
    user_credentials = NULL;

  return *new ONCRPCRequest
              (
                body,
                body.get_type_id(),
                get_prog(),
                get_vers(),
                user_credentials
              );
}
