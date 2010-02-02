// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _LIBXTREEMFS_SHARED_FILE_H_
#define _LIBXTREEMFS_SHARED_FILE_H_

#include "xtreemfs/volume.h"


namespace xtreemfs
{
  class OpenFile;


  class SharedFile : public yidl::runtime::Object
  {
  public:
    void close( OpenFile& open_file );

    auto_Volume get_parent_volume() const { return parent_volume; }
    const YIELD::platform::Path& get_path() const { return path; }
    org::xtreemfs::interfaces::XLocSet get_xlocs() const { return xlocs; }
    
    YIELD::platform::auto_File
    open
    ( 
      org::xtreemfs::interfaces::FileCredentials& file_credentials 
    );

    // yidl::runtime::Object
    YIDL_RUNTIME_OBJECT_PROTOTYPES( SharedFile, 0 );

    // YIELD::platform::File
    yidl::runtime::auto_Object<YIELD::platform::Stat> getattr();

    bool
    getlk
    (       
      bool exclusive, 
      uint64_t offset, 
      uint64_t length,
      const org::xtreemfs::interfaces::XCap& xcap
    );

    bool getxattr( const std::string& name, std::string& out_value );

    bool listxattr( std::vector<std::string>& out_names );

    ssize_t 
    read
    ( 
      void* buffer, 
      size_t buffer_len, 
      uint64_t offset,
      const org::xtreemfs::interfaces::XCap& xcap
    );

    bool removexattr( const std::string& name );

    bool
    setlk
    (       
      bool exclusive, 
      uint64_t offset, 
      uint64_t length,
      const org::xtreemfs::interfaces::XCap& xcap
    );

    bool
    setlkw
    (       
      bool exclusive, 
      uint64_t offset, 
      uint64_t length,
      const org::xtreemfs::interfaces::XCap& xcap
    );

    bool
    setxattr
    (
      const std::string& name,
      const std::string& value,
      int flags
    );

    bool 
    sync
    ( 
      const org::xtreemfs::interfaces::XCap& xcap 
    );

    bool 
    truncate
    (       
      uint64_t offset,
      org::xtreemfs::interfaces::XCap& xcap
    );

    bool 
    unlk
    ( 
      uint64_t offset, 
      uint64_t length,
      const org::xtreemfs::interfaces::XCap& xcap
    );

    ssize_t
    write
    (       
      const void* buffer,
      size_t buffer_len,
      uint64_t offset,
      const org::xtreemfs::interfaces::XCap& xcap
    );
    
  private:
    friend class Volume;

    class ReadBuffer;
    class ReadRequest;
    class ReadResponse;
    class WriteBuffer;


    SharedFile
    (
      YIELD::platform::auto_Log log,
      auto_Volume parent_volume,
      const YIELD::platform::Path& path // The first, "authoritative" path 
                                        // used to open this file
    );


    YIELD::platform::auto_Log log;
    auto_Volume parent_volume;
    YIELD::platform::Path path;

    uint32_t reader_count, writer_count;
    ssize_t selected_file_replica;
    org::xtreemfs::interfaces::XLocSet xlocs;
  };

  typedef yidl::runtime::auto_Object<SharedFile> auto_SharedFile;
};

#endif
