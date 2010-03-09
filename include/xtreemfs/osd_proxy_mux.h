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

#include "xtreemfs/osd_proxy.h"


namespace xtreemfs
{
  class DIRProxy;
  using org::xtreemfs::interfaces::FileCredentials;


  class OSDProxyMux 
    : public org::xtreemfs::interfaces::OSDInterfaceEventHandler
  {
  public:
    ~OSDProxyMux();

    static OSDProxyMux&
    create
    (
      DIRProxy& dir_proxy,
      uint16_t concurrency_level = OSDProxy::CONCURRENCY_LEVEL_DEFAULT,
      uint32_t flags = OSDProxy::FLAGS_DEFAULT,
      Log* log = NULL,
      const Time& operation_timeout = OSDProxy::OPERATION_TIMEOUT_DEFAULT,
      uint8_t reconnect_tries_max = OSDProxy::RECONNECT_TRIES_MAX_DEFAULT,
      SSLContext* ssl_context = NULL,
      UserCredentialsCache* user_credentials_cache = NULL
    );

    // yidl::runtime::Object
    OSDProxyMux& inc_ref() { return yidl::runtime::Object::inc_ref( *this ); }

  private:
    OSDProxyMux
    (
      uint16_t concurrency_level,
      DIRProxy& dir_proxy,
      uint32_t flags,
      Log* log,
      const Time& operation_timeout,
      uint8_t reconnect_tries_max,
      SSLContext* ssl_context,
      UserCredentialsCache* user_credentials_cache
    );

    OSDProxy& get_osd_proxy
    (
      OSDProxyRequest& osd_proxy_request,
      const FileCredentials& file_credentials,
      uint64_t object_number
    );

    OSDProxy& get_osd_proxy( const string& osd_uuid );

    // org::xtreemfs::interfaces::OSDInterface
    void handlereadRequest( readRequest& req );
    void handletruncateRequest( truncateRequest& req );
    void handleunlinkRequest( unlinkRequest& req );
    void handlewriteRequest( writeRequest& req );
    void handlextreemfs_lock_acquireRequest( xtreemfs_lock_acquireRequest& req );
    void handlextreemfs_lock_checkRequest( xtreemfs_lock_checkRequest& req );
    void handlextreemfs_lock_releaseRequest( xtreemfs_lock_releaseRequest& req );

  private:
    class ReadResponseTarget;
    class TruncateResponseTarget;

  private:
    uint16_t concurrency_level;
    DIRProxy& dir_proxy;
    uint32_t flags;
    Log* log;
    Time operation_timeout;
    uint8_t reconnect_tries_max;
    SSLContext* ssl_context;
    UserCredentialsCache* user_credentials_cache;

    typedef std::map<string, OSDProxy*> OSDProxyMap;
    OSDProxyMap osd_proxies;
    yield::concurrency::StageGroup& osd_proxy_stage_group;
  };
};

#endif
