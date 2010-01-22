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
      const org::xtreemfs::interfaces::FileCredentials& file_credentials,
      bool exclusive, 
      uint64_t offset, 
      uint64_t length 
    );

    bool getxattr( const std::string& name, std::string& out_value );

    bool listxattr( std::vector<std::string>& out_names );

    ssize_t 
    read
    ( 
      const org::xtreemfs::interfaces::FileCredentials& file_credentials,
      void* buffer, 
      size_t buffer_len, 
      uint64_t offset 
    );

    bool removexattr( const std::string& name );

    bool
    setlk
    ( 
      const org::xtreemfs::interfaces::FileCredentials& file_credentials, 
      bool exclusive, 
      uint64_t offset, 
      uint64_t length 
    );

    bool
    setlkw
    ( 
      const org::xtreemfs::interfaces::FileCredentials& file_credentials,
      bool exclusive, 
      uint64_t offset, 
      uint64_t length 
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
      const org::xtreemfs::interfaces::FileCredentials& file_credentials 
    );

    bool 
    truncate
    ( 
      org::xtreemfs::interfaces::FileCredentials& file_credentials,
      uint64_t offset 
    );

    bool 
    unlk
    ( 
      const org::xtreemfs::interfaces::FileCredentials& file_credentials,
      uint64_t offset, 
      uint64_t length 
    );

    ssize_t
    write
    ( 
      const org::xtreemfs::interfaces::FileCredentials& file_credentials,
      const void* buffer,
      size_t buffer_len,
      uint64_t offset
    );
    
  private:
    friend class Volume;

    class ReadBuffer;
    class ReadRequest;
    class ReadResponse;
    class WriteBuffer;


    SharedFile
    (
      auto_Volume parent_volume,
      const YIELD::platform::Path& path // The first, "authoritative" path 
                                        // used to open this file
    );


    std::vector<org::xtreemfs::interfaces::Lock> locks;
    uint32_t open_file_count;
    YIELD::platform::Path path;
    auto_Volume parent_volume;
    ssize_t selected_file_replica;
  };

  typedef yidl::runtime::auto_Object<SharedFile> auto_SharedFile;
};

#endif
