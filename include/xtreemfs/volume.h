// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_VOLUME_H_
#define _XTREEMFS_VOLUME_H_

#include "xtreemfs/osd_proxy_mux.h"


namespace xtreemfs
{
  class DIRProxy;
  class MRCProxy;
  class Path;


  class Volume : public YIELD::platform::Volume
  {
  public:
    const static uint32_t VOLUME_FLAG_CACHE_METADATA = 1;
    const static uint32_t VOLUME_FLAG_TRACE_FILE_IO = 2;

    static yidl::runtime::auto_Object<Volume> create( 
                                             const YIELD::ipc::URI& dir_uri,
                                             const std::string& name,
                                             uint32_t flags = 0,
                                             YIELD::platform::auto_Log log = NULL,
                                             uint32_t proxy_flags = 0,
                                             const YIELD::platform::Time& proxy_operation_timeout = DIRProxy::OPERATION_TIMEOUT_DEFAULT,
                                             YIELD::ipc::auto_SSLContext proxy_ssl_context = NULL,
                                             const YIELD::platform::Path& vivaldi_coordinates_file_path = YIELD::platform::Path()
                                            );

    uint32_t get_flags() const { return flags; }
    YIELD::platform::auto_Log get_log() const { return log; }
    yidl::runtime::auto_Object<MRCProxy> get_mrc_proxy() const { return mrc_proxy; }
    yidl::runtime::auto_Object<OSDProxyMux> get_osd_proxy_mux() const { return osd_proxy_mux; }
    const std::string& get_uuid() const { return uuid; }

    // yidl::runtime::Object
    YIDL_RUNTIME_OBJECT_PROTOTYPES( Volume, 0 );

    // YIELD::platform::Volume
    YIELD_PLATFORM_VOLUME_PROTOTYPES;
    bool listdir( const YIELD::platform::Path& path, listdirCallback& callback ) { return listdir( path, YIELD::platform::Path(), callback ); }
    bool listdir( const YIELD::platform::Path& path, const YIELD::platform::Path& match_file_name_prefix, listdirCallback& callback );
    YIELD::platform::auto_Stat stat( const Path& path );

  private:
    Volume( yidl::runtime::auto_Object<DIRProxy> dir_proxy, uint32_t flags, YIELD::platform::auto_Log log, yidl::runtime::auto_Object<MRCProxy> mrc_proxy, const std::string& name, yidl::runtime::auto_Object<OSDProxyMux> osd_proxy_mux, YIELD::concurrency::auto_StageGroup stage_group, const YIELD::platform::Path& vivaldi_coordinates_file_path );
    ~Volume() { }

    yidl::runtime::auto_Object<DIRProxy> dir_proxy;
    uint32_t flags;
    YIELD::platform::auto_Log log;
    yidl::runtime::auto_Object<MRCProxy> mrc_proxy;
    std::string name;
    yidl::runtime::auto_Object<OSDProxyMux> osd_proxy_mux;
    YIELD::concurrency::auto_StageGroup stage_group;
    std::string uuid;
    YIELD::platform::Path vivaldi_coordinates_file_path;

    void osd_unlink( const org::xtreemfs::interfaces::FileCredentialsSet& );
    void set_errno( const char* operation_name, ProxyExceptionResponse& proxy_exception_response );
    void set_errno( const char* operation_name, std::exception& exc );
  };

  typedef yidl::runtime::auto_Object<Volume> auto_Volume;
};

#endif
