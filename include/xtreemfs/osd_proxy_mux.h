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


#ifndef _XTREEMFS_OSD_PROXY_MUX_H_
#define _XTREEMFS_OSD_PROXY_MUX_H_

#include "xtreemfs/dir_proxy.h"
#include "xtreemfs/osd_proxy.h"


namespace xtreemfs
{
  class PolicyContainer;


  class OSDProxyMux : public org::xtreemfs::interfaces::OSDInterface
  {
  public:
    static yidl::runtime::auto_Object<OSDProxyMux>
    create
    (
      auto_DIRProxy dir_proxy,
      uint16_t concurrency_level
        = OSDProxy::CONCURRENCY_LEVEL_DEFAULT,
      uint32_t flags
        = 0,
      YIELD::platform::auto_Log log
        = NULL,
      const YIELD::platform::Time& operation_timeout
        = OSDProxy::OPERATION_TIMEOUT_DEFAULT,
      uint8_t reconnect_tries_max
        = OSDProxy::RECONNECT_TRIES_MAX_DEFAULT,
      YIELD::ipc::auto_SSLContext ssl_context
        = NULL,
      auto_UserCredentialsCache user_credentials_cache
        = NULL
    );

    // yidl::runtime::Object
    OSDProxyMux& incRef() { return yidl::runtime::Object::incRef( *this ); }

  private:
    OSDProxyMux
    (
      uint16_t concurrency_level,
      auto_DIRProxy dir_proxy,
      uint32_t flags,
      YIELD::platform::auto_Log log,
      const YIELD::platform::Time& operation_timeout,
      uint8_t reconnect_tries_max,
      YIELD::ipc::auto_SSLContext ssl_context,
      auto_UserCredentialsCache user_credentials_cache
    );

    ~OSDProxyMux();

    uint16_t concurrency_level;
    auto_DIRProxy dir_proxy;
    uint32_t flags;
    YIELD::platform::auto_Log log;
    YIELD::platform::Time operation_timeout;
    uint8_t reconnect_tries_max;
    YIELD::ipc::auto_SSLContext ssl_context;
    auto_UserCredentialsCache user_credentials_cache;

    typedef std::map<std::string, OSDProxy*> OSDProxyMap;
    OSDProxyMap osd_proxies;
    YIELD::concurrency::auto_StageGroup osd_proxy_stage_group;

    auto_OSDProxy getOSDProxy
    (
      OSDProxyRequest& osd_proxy_request,
      const org::xtreemfs::interfaces::FileCredentials& file_credentials,
      uint64_t object_number
    );

    auto_OSDProxy getOSDProxy( const std::string& osd_uuid );

    // org::xtreemfs::interfaces::OSDInterface
    void handlereadRequest( readRequest& req );
    void handletruncateRequest( truncateRequest& req );
    void handleunlinkRequest( unlinkRequest& req );
    void handlewriteRequest( writeRequest& req );
    void handlextreemfs_lock_acquireRequest( xtreemfs_lock_acquireRequest& req );
    void handlextreemfs_lock_checkRequest( xtreemfs_lock_checkRequest& req );
    void handlextreemfs_lock_releaseRequest( xtreemfs_lock_releaseRequest& req );

    class ReadResponseTarget;
    class TruncateResponseTarget;
  };

  typedef yidl::runtime::auto_Object<OSDProxyMux> auto_OSDProxyMux;
};

#endif
