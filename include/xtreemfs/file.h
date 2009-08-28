// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_FILE_H_
#define _XTREEMFS_FILE_H_

#include "xtreemfs/osd_proxy.h"


namespace xtreemfs
{
  class MRCProxy;
  class Volume;


  class File : public YIELD::File
  {
  public:
    YIELD_FILE_PROTOTYPES;
    virtual size_t getpagesize();
    virtual uint64_t get_size();

  private:
    friend class Volume;

    File( yidl::auto_Object<Volume> parent_volume, yidl::auto_Object<MRCProxy> mrc_proxy, const YIELD::Path& path, const org::xtreemfs::interfaces::FileCredentials& file_credentials );
    ~File();

    yidl::auto_Object<Volume> parent_volume;
    yidl::auto_Object<MRCProxy> mrc_proxy;
    YIELD::Path path;
    org::xtreemfs::interfaces::FileCredentials file_credentials;

    org::xtreemfs::interfaces::OSDWriteResponse latest_osd_write_response;
    std::vector<org::xtreemfs::interfaces::Lock> locks;
    ssize_t selected_file_replica;
  };

  typedef yidl::auto_Object<File> auto_File;
};

#endif
