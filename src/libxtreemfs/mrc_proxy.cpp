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
#include "user_credentials_cache.h"
using namespace xtreemfs;



MRCProxy::MRCProxy
(
  EventHandler& request_handler,
  const char* password,
  UserCredentialsCache* user_credentials_cache
) : MRCInterfaceProxy( request_handler ),
    password( password )
{
  if ( user_credentials_cache != NULL )
    this->user_credentials_cache = Object::inc_ref( user_credentials_cache );
  else
    this->user_credentials_cache = new UserCredentialsCache;
}

MRCProxy::~MRCProxy()
{
  UserCredentialsCache::dec_ref( *user_credentials_cache );
}

MRCProxy& 
MRCProxy::create
( 
  const URI& absolute_uri,
  const Options& options,
  const char* password
)
{
  return create
         ( 
           absolute_uri,
           NULL,
           options.get_error_log(),
           password,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
           options.get_ssl_context(),
#endif
           options.get_trace_log()
         );
}

MRCProxy&
MRCProxy::create
(
  const URI& absolute_uri,
  Configuration* configuration,
  Log* error_log,
  const char* password,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
  SSLContext* ssl_context,
#endif
  Log* trace_log,
  UserCredentialsCache* user_credentials_cache
)
{
  return *new MRCProxy
              (
                createONCRPCClient
                (
                  absolute_uri,
                  *new org::xtreemfs::interfaces::MRCInterfaceMessageFactory,
                  ONC_RPC_PORT_DEFAULT,
                  0x20000000 + TAG,
                  TAG,
                  configuration,
                  error_log,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
                  ssl_context,
#endif
                  trace_log
                ),
                password,  
                user_credentials_cache
              );
}

yidl::runtime::MarshallableObject* MRCProxy::get_cred()
{
  UserCredentials* user_credentials
    = user_credentials_cache->getCurrentUserCredentials();

  if ( user_credentials != NULL )
    user_credentials->set_password( password );

  return user_credentials;
}
