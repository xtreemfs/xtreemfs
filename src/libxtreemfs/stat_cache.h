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


#ifndef _LIBXTREEMFS_STAT_CACHE_H_
#define _LIBXTREEMFS_STAT_CACHE_H_

#include <map>
using std::map;

#include "stat.h"

#include "xtreemfs/mrc_proxy.h"
#include "xtreemfs/user_credentials_cache.h"


namespace xtreemfs
{
  using org::xtreemfs::interfaces::XCap;
  using yield::platform::Path;


  class StatCache
  {
  public:
    StatCache
    (
      MRCProxy& mrc_proxy,
      const Time& read_ttl,
      UserCredentialsCache& user_credentials_cache,
      const string& volume_name_utf8,
      uint32_t write_back_attrs = yield::platform::Volume::SETATTR_SIZE
    );

    ~StatCache();

    void evict( const Path& path );

    void
    fsetattr
    (
      const Path& path,
      const Stat& stbuf,
      uint32_t to_set,
      const XCap& write_xcap
    );

    Stat* getattr( const Path& path );

    void metadatasync( const Path& path, const XCap& write_xcap );

    void
    setattr
    (
      const Path& path,
      const Stat& stbuf,
      uint32_t to_set
    );

  private:
    void _setattr( const Path& path, const Stat& stbuf, uint32_t to_set );

  private:
    MRCProxy& mrc_proxy;
    Time read_ttl; // Time to keep Stats read from the server
    UserCredentialsCache& user_credentials_cache;
    string volume_name_utf8;
    uint32_t write_back_attrs; // SETATTR_ types to write back

    class Entry;
    typedef std::map<Path, Entry*> EntryMap;
    EntryMap entries;
    yield::platform::Mutex entries_lock;
  };
};

#endif
