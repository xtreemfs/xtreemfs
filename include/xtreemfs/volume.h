// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_VOLUME_H_
#define _XTREEMFS_VOLUME_H_

#include "xtreemfs/dir_proxy.h"
#include "xtreemfs/mrc_proxy.h"
#include "xtreemfs/osd_proxy_mux.h"


namespace xtreemfs
{
  class SharedFile;


  class Volume : public YIELD::platform::Volume
  {
  public:
    const static uint32_t VOLUME_FLAG_CACHE_METADATA = 1;
    const static uint32_t VOLUME_FLAG_TRACE_FILE_IO = 2;


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

    uint32_t get_flags() const { return flags; }
    YIELD::platform::auto_Log get_log() const { return log; }
    auto_MRCProxy get_mrc_proxy() const { return mrc_proxy; }
    auto_OSDProxyMux get_osd_proxy_mux() const { return osd_proxy_mux; }
    const std::string& get_uuid() const { return uuid; }

    org::xtreemfs::interfaces::VivaldiCoordinates 
      get_vivaldi_coordinates() const;

    void release( SharedFile& );

    // yidl::runtime::Object
    YIDL_RUNTIME_OBJECT_PROTOTYPES( Volume, 0 );

    // YIELD::platform::Volume
    YIELD_PLATFORM_VOLUME_PROTOTYPES;

    YIELD::platform::auto_Stat getattr( const Path& path );

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

    ~Volume() { }


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
    auto_UserCredentialsCache user_credentials_cache;
    YIELD::platform::Path vivaldi_coordinates_file_path;


    yidl::runtime::auto_Object<SharedFile> 
    get_shared_file
    ( 
      const YIELD::platform::Path& path
    );

    void osd_unlink( const org::xtreemfs::interfaces::FileCredentialsSet& );
    void set_errno( const char* operation_name, ProxyExceptionResponse& );
    void set_errno( const char* operation_name, std::exception& );
  };

  typedef yidl::runtime::auto_Object<Volume> auto_Volume;
};

#endif
