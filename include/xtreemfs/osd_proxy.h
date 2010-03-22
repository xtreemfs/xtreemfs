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


#ifndef _XTREEMFS_OSD_PROXY_H_
#define _XTREEMFS_OSD_PROXY_H_

#include "xtreemfs/interfaces/osd_interface.h"
#include "xtreemfs/proxy.h"


namespace xtreemfs
{
  class OSDProxy
    : public Proxy
             <
               org::xtreemfs::interfaces::OSDInterface,
               org::xtreemfs::interfaces::OSDInterfaceMessageFactory,
               org::xtreemfs::interfaces::OSDInterfaceMessageSender
             >
  {
  public:
    virtual ~OSDProxy() { }

    static OSDProxy&
    create
    (
      const URI& absolute_uri,
      uint16_t concurrency_level = CONCURRENCY_LEVEL_DEFAULT,
      Log* error_log = NULL,
      const Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT,
      uint16_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT,
      SSLContext* ssl_context = NULL, // Steals this reference
      Log* trace_log = NULL,
      UserCredentialsCache* user_credentials_cache = NULL
    );

    // yidl::runtime::Object
    OSDProxy& inc_ref() { return yidl::runtime::Object::inc_ref( *this ); }

  private:
    OSDProxy
    (
      uint16_t concurrency_level,
      Log* error_log,
      IOQueue& io_queue,
      const Time& operation_timeout,
      SocketAddress& peername,
      uint16_t reconnect_tries_max,
      SocketFactory& socket_factory,
      Log* trace_log,
      UserCredentialsCache* user_credentials_cache
    );
  };
};


bool operator>
(
  const org::xtreemfs::interfaces::OSDWriteResponse& left,
  const org::xtreemfs::interfaces::OSDWriteResponse& right
);

#endif
