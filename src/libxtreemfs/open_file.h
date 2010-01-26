// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _LIBXTREEMFS_OPEN_FILE_H_
#define _LIBXTREEMFS_OPEN_FILE_H_

#include "shared_file.h"


namespace xtreemfs
{
  class OpenFile : public YIELD::platform::File
  {
  public:
    const org::xtreemfs::interfaces::XCap& get_xcap() const { return xcap; }

    // yidl::runtime::Object
    YIDL_RUNTIME_OBJECT_PROTOTYPES( OpenFile, 0 );

    // YIELD::platform::File
    YIELD_PLATFORM_FILE_PROTOTYPES;
    size_t getpagesize();

  private:
    friend class SharedFile;
    class XCapTimer;
    friend class XCapTimer;      

    OpenFile
    (
      auto_SharedFile parent_shared_file,
      const org::xtreemfs::interfaces::XCap& xcap
    );

    ~OpenFile();

    bool closed;
    auto_SharedFile parent_shared_file;
    org::xtreemfs::interfaces::XCap xcap;
    yidl::runtime::auto_Object<XCapTimer> xcap_timer;
  };

  typedef yidl::runtime::auto_Object<OpenFile> auto_OpenFile;
};

#endif
