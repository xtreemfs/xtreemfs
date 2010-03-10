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


#ifndef _XTREEMFS_MRC_PROXY_H_
#define _XTREEMFS_MRC_PROXY_H_

#include "xtreemfs/proxy.h"

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "xtreemfs/interfaces/mrc_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif


namespace xtreemfs
{
  class MRCProxy
    : public Proxy
             <
               org::xtreemfs::interfaces::MRCInterface,
               org::xtreemfs::interfaces::MRCInterfaceEventFactory,
               org::xtreemfs::interfaces::MRCInterfaceEventSender
             >
  {
  public:
    static MRCProxy&
    create
    (
      const URI& absolute_uri,
      uint16_t concurrency_level = CONCURRENCY_LEVEL_DEFAULT,
      uint32_t flags = FLAGS_DEFAULT,
      Log* log = NULL,
      const Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT,
      const string& password = "",
      uint16_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT,
      SSLContext* ssl_context = NULL, // Steals this reference
      UserCredentialsCache* user_credentials_cache = NULL
    );

    // yidl::runtime::Object
    MRCProxy& inc_ref() { return Object::inc_ref( *this ); }

  private:
    MRCProxy
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
    );

    ~MRCProxy() { }

    // yield::ipc::ONCRPCClient
    ONCRPCRequest& createONCRPCRequest( MarshallableObject& body );

  private:
    string password;
  };
};

#endif
