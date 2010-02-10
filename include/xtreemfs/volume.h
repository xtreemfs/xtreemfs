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


#ifndef _XTREEMFS_VOLUME_H_
#define _XTREEMFS_VOLUME_H_

#include "xtreemfs/dir_proxy.h"
#include "xtreemfs/mrc_proxy.h"
#include "xtreemfs/osd_proxy_mux.h"


namespace xtreemfs
{
  class SharedFile;
  class Stat;
  class StatCache;


  class Volume : public YIELD::platform::Volume
  {
  public:
    const static uint32_t VOLUME_FLAG_WRITE_BACK_DATA_CACHE = 1;
    const static uint32_t VOLUME_FLAG_WRITE_BACK_FILE_SIZE_CACHE = 2;
    const static uint32_t VOLUME_FLAG_WRITE_BACK_STAT_CACHE = 4;
    const static uint32_t VOLUME_FLAG_WRITE_THROUGH_DATA_CACHE = 8;
    const static uint32_t VOLUME_FLAG_WRITE_THROUGH_FILE_SIZE_CACHE = 16;
    const static uint32_t VOLUME_FLAG_WRITE_THROUGH_STAT_CACHE = 32;
    const static uint32_t VOLUME_FLAG_TRACE_FILE_IO = 64;


    static yidl::runtime::auto_Object<Volume>
    create
    (
      const YIELD::ipc::URI& dir_uri,
      const std::string& name,
      uint32_t flags = 0,
      YIELD::platform::auto_Log log = NULL,
      uint32_t proxy_flags = 0,
      const YIELD::platform::Time& proxy_operation_timeout
        = DIRProxy::OPERATION_TIMEOUT_DEFAULT,
      uint8_t proxy_reconnect_tries_max
        = DIRProxy::RECONNECT_TRIES_MAX_DEFAULT,
      YIELD::ipc::auto_SSLContext proxy_ssl_context = NULL,
      const YIELD::platform::Path& vivaldi_coordinates_file_path
        = YIELD::platform::Path()
    );

    // fsetattr is used for setting the file size
    void fsetattr
    (
      const YIELD::platform::Path& path,
      const Stat& stbuf,
      uint32_t to_set,
      const org::xtreemfs::interfaces::XCap& write_xcap
    );

    uint32_t get_flags() const { return flags; }
    YIELD::platform::auto_Log get_log() const { return log; }
    auto_MRCProxy get_mrc_proxy() const { return mrc_proxy; }
    auto_OSDProxyMux get_osd_proxy_mux() const { return osd_proxy_mux; }
    const std::string& get_uuid() const { return uuid; }

    org::xtreemfs::interfaces::VivaldiCoordinates
    get_vivaldi_coordinates() const;

    void metadatasync
    (
      const YIELD::platform::Path& path,
      const org::xtreemfs::interfaces::XCap& write_xcap
    );

    void release( SharedFile& );

    // yidl::runtime::Object
    YIDL_RUNTIME_OBJECT_PROTOTYPES( Volume, 0 );

    // YIELD::platform::Volume
    YIELD_PLATFORM_VOLUME_PROTOTYPES;

    bool listdir
    (
      const YIELD::platform::Path& path,
      listdirCallback& callback
    )
    {
      return listdir( path, YIELD::platform::Path(), callback );
    }

    bool listdir
    (
      const YIELD::platform::Path& path,
      const YIELD::platform::Path& match_file_name_prefix,
      listdirCallback& callback
    );

    static void
    set_errno
    (
      YIELD::platform::Log* log,
      const char* operation_name,
      ProxyExceptionResponse&
    );

    static void
    set_errno
    (
      YIELD::platform::Log* log,
      const char* operation_name,
      std::exception&
    );

  private:
    Volume
    (
      auto_DIRProxy dir_proxy,
      uint32_t flags,
      YIELD::platform::auto_Log log,
      auto_MRCProxy mrc_proxy,
      const std::string& name,
      auto_OSDProxyMux osd_proxy_mux,
      YIELD::concurrency::auto_StageGroup stage_group,
      auto_UserCredentialsCache user_credentials_cache,
      const YIELD::platform::Path& vivaldi_coordinates_file_path
    );

    ~Volume();


    auto_DIRProxy dir_proxy;
    uint32_t flags;
    YIELD::platform::auto_Log log;
    auto_MRCProxy mrc_proxy;
    std::string name;
    auto_OSDProxyMux osd_proxy_mux;
    std::map<std::string, SharedFile*> shared_files;
    YIELD::platform::Mutex shared_files_lock;
    YIELD::concurrency::auto_StageGroup stage_group;
    std::string uuid;
    StatCache* stat_cache;
    auto_UserCredentialsCache user_credentials_cache;
    YIELD::platform::Path vivaldi_coordinates_file_path;


    yidl::runtime::auto_Object<SharedFile>
    get_shared_file
    (
      const YIELD::platform::Path& path
    );
  };

  typedef yidl::runtime::auto_Object<Volume> auto_Volume;
};

#endif
