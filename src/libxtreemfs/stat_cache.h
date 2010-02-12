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

#include "stat.h"
#include "xtreemfs/mrc_proxy.h"
#include "xtreemfs/user_credentials_cache.h"


namespace xtreemfs
{ 
  class StatCache
  {
  public:
    StatCache
    ( 
      auto_MRCProxy mrc_proxy, 
      const YIELD::platform::Time& read_ttl,
      auto_UserCredentialsCache user_credentials_cache,
      const std::string& volume_name,
      uint32_t write_back_attrs = YIELD::platform::Volume::SETATTR_SIZE
    );

    ~StatCache();

    void evict( const YIELD::platform::Path& path );

    void 
    fsetattr
    ( 
      const YIELD::platform::Path& path,
      auto_Stat stbuf,
      uint32_t to_set,
      const org::xtreemfs::interfaces::XCap& write_xcap
    );

    YIELD::platform::auto_Stat
    getattr
    ( 
      const YIELD::platform::Path& path 
    );

    void 
    metadatasync
    ( 
      const YIELD::platform::Path& path, 
      const org::xtreemfs::interfaces::XCap& write_xcap
    );

    void 
    setattr
    ( 
      const YIELD::platform::Path& path, 
      auto_Stat stbuf,
      uint32_t to_set
    );

  private:    
    auto_MRCProxy mrc_proxy;
    YIELD::platform::Time read_ttl; // Time to keep Stats read from the server
    auto_UserCredentialsCache user_credentials_cache;
    std::string volume_name;
    uint32_t write_back_attrs; // SETATTR_ types to write back


    class Entry
    {
    public:
      Entry(); // Missing file
      Entry( org::xtreemfs::interfaces::Stat stbuf ); // From a getattr
      Entry( auto_Stat stbuf, uint32_t write_back_attrs ); // From a setattr

      void change( auto_Stat stbuf, uint32_t to_set ); // On setattr
      auto_Stat get_stbuf() const { return stbuf; }      
      uint32_t get_write_back_attrs() const { return write_back_attrs; }
      const YIELD::platform::Time& get_refresh_time() const;
      void refresh(); // On getattr for a missing file
      void refresh( const org::xtreemfs::interfaces::Stat& stbuf ); // On getattr
      void set_write_back_attrs( uint32_t write_back_attrs );

    private:
      YIELD::platform::Time refresh_time; // Last time the Stat was set from
                                          // an org::xtreemfs::interfaces::Stat
                                          // or 0 if the contents did not come
                                          // from the server (i.e. on setattr)
      auto_Stat stbuf;
      uint32_t write_back_attrs; // Union of SETATTR_* changes
    };

    typedef std::map<YIELD::platform::Path, Entry*> EntryMap;
    EntryMap entries;
    YIELD::platform::Mutex entries_lock;


    void _setattr 
    (
      const YIELD::platform::Path& path,
      auto_Stat stbuf, 
      uint32_t to_set
    );  
  };
};

#endif
