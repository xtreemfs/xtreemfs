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

#include "xtreemfs/proxy.h"
#include "xtreemfs/osd_proxy_request.h"
#include "xtreemfs/osd_proxy_response.h"

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "xtreemfs/interfaces/osd_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif


namespace xtreemfs
{
  class OSDProxy : public Proxy<org::xtreemfs::interfaces::OSDInterface>
  {
  public:
    static yidl::runtime::auto_Object<OSDProxy>
    create
    (
      const YIELD::ipc::URI& absolute_uri,
      uint16_t concurrency_level = CONCURRENCY_LEVEL_DEFAULT,
      uint32_t flags = 0,
      YIELD::platform::auto_Log log = NULL,
      const YIELD::platform::Time& operation_timeout =
        OPERATION_TIMEOUT_DEFAULT,
      uint8_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT,
      YIELD::ipc::auto_SSLContext ssl_context = NULL,
      auto_UserCredentialsCache user_credentials_cache = NULL
    );

    // yidl::runtime::Object
    OSDProxy& incRef() { return yidl::runtime::Object::incRef( *this ); }

    // YIELD::concurrency::EventTarget
    void send( YIELD::concurrency::Event& ev )
    {
      // Bypass Proxy so no credentials are attached;
      // the credentials for OSD operations are in FileCredentials passed
      // explicitly to the operation
      YIELD::ipc::
        ONCRPCClient<org::xtreemfs::interfaces::OSDInterface>::send( ev );
    }

  private:
    OSDProxy
    (
      uint16_t concurrency_level,
      uint32_t flags,
      YIELD::platform::auto_Log log,
      const YIELD::platform::Time& operation_timeout,
      YIELD::ipc::auto_SocketAddress peername,
      uint8_t reconnect_tries_max,
      YIELD::ipc::auto_SocketFactory socket_factory,
      auto_UserCredentialsCache user_credentials_cache
    )
      : Proxy<org::xtreemfs::interfaces::OSDInterface>
      (
        concurrency_level,
        flags,
        log,
        operation_timeout,
        peername,
        reconnect_tries_max,
        socket_factory,
        user_credentials_cache
      )
    { }

    ~OSDProxy() { }
  };

  typedef yidl::runtime::auto_Object<OSDProxy> auto_OSDProxy;
};


bool operator>
(
  const org::xtreemfs::interfaces::OSDWriteResponse& left,
  const org::xtreemfs::interfaces::OSDWriteResponse& right
);

#endif
