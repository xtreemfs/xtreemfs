// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_FILE_H_
#define _XTREEMFS_FILE_H_

#include "xtreemfs/osd_proxy.h"


namespace xtreemfs
{
  class MRCProxy;
  class Volume;


  class File : public YIELD::platform::File
  {
  public:
    // yidl::runtime::Object
    YIDL_RUNTIME_OBJECT_PROTOTYPES( File, 0 );

    // YIELD::platform::File
    YIELD_PLATFORM_FILE_PROTOTYPES;
    virtual size_t getpagesize();

  private:
    friend class Volume;
    friend class XCapTimer;

    class ReadBuffer;
    class ReadRequest;
    class ReadResponse;
    class WriteBuffer;
    class XCapTimer;


    File
    ( 
      yidl::runtime::auto_Object<Volume> parent_volume, 
      const YIELD::platform::Path& path, 
      const org::xtreemfs::interfaces::FileCredentials& file_credentials 
    );

    ~File();


    yidl::runtime::auto_Object<Volume> parent_volume;
    YIELD::platform::Path path;
    org::xtreemfs::interfaces::FileCredentials file_credentials;

    bool closed;

    org::xtreemfs::interfaces::OSDWriteResponse latest_osd_write_response;
    std::vector<org::xtreemfs::interfaces::Lock> locks;
    ssize_t selected_file_replica;

    yidl::runtime::auto_Object<XCapTimer> xcap_timer;
  };

  typedef yidl::runtime::auto_Object<File> auto_File;
};

#endif
