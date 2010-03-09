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


#ifndef _LIBXTREEMFS_SHARED_FILE_H_
#define _LIBXTREEMFS_SHARED_FILE_H_

#include "xtreemfs/volume.h"


namespace xtreemfs
{ 
  class OpenFile;

  using org::xtreemfs::interfaces::XLocSet;
  using org::xtreemfs::interfaces::FileCredentials;



  class SharedFile : public yidl::runtime::Object
  {
  public:
    SharedFile( Volume& parent_volume, const Path& path );
    ~SharedFile() { } // See note below re: parent_volume

    void close( OpenFile& open_file );

    Volume& get_parent_volume() const { return parent_volume; }
    const Path& get_path() const { return path; }
    XLocSet get_xlocs() const { return xlocs; }

    OpenFile& open( const FileCredentials& file_credentials );

    // yield::platform::File
    yield::platform::Stat* getattr();

    bool 
    getlk
    (
      bool exclusive,
      uint64_t offset,
      uint64_t length,
      const XCap& xcap
    );

    bool getxattr( const string& name, string& out_value );
    bool listxattr( vector<string>& out_names );

    ssize_t 
    read
    (
      void* buf,
      size_t buflen,
      uint64_t offset,
      const XCap& xcap
    );

    bool removexattr( const string& name );

    bool
    setlk
    (
      bool exclusive,
      uint64_t offset,
      uint64_t length,
      const XCap& xcap
    );

    bool
    setlkw
    (
      bool exclusive,
      uint64_t offset,
      uint64_t length,
      const XCap& xcap
    );

    bool
    setxattr
    (
      const string& name,
      const string& value,
      int flags
    );

    bool sync( const XCap& xcap );
    bool truncate( uint64_t offset, XCap& xcap );
    bool unlk( uint64_t offset, uint64_t length, const XCap& xcap );

    ssize_t
    write
    (
      const void* buf,
      size_t buflen,
      uint64_t offset,
      const XCap& xcap
    );

  private:
    class ReadBuffer;
    class ReadRequest;
    class ReadResponse;
    class WriteBuffer;

  private:
    Volume& parent_volume; // This is not a new reference, since that would
                           // create a reference cycle with Volume
    Path path; // The first, "authoritative" path used to open this file

    uint32_t reader_count, writer_count;
    ssize_t selected_file_replica;
    org::xtreemfs::interfaces::XLocSet xlocs;
  };
};

#endif
