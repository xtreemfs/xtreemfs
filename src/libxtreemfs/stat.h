// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _LIBXTREEMFS_STAT_H_
#define _LIBXTREEMFS_STAT_H_

#include "xtreemfs/mrc_proxy.h"


namespace xtreemfs
{
  class Stat : public YIELD::platform::Stat
  {
  public:
    void set_mode( mode_t mode );
#ifndef _WIN32
    void set_nlink( nlink_t nlink );
    void set_uid( uid_t uid );
    void set_gid( gid_t gid);
#endif
    void set_size( uint64_t size );
    void set_atime( const YIELD::platform::Time& atime );
    void set_mtime( const YIELD::platform::Time& mtime );
    void set_ctime( const YIELD::platform::Time& ctime );
#ifdef _WIN32
    void set_attributes( uint32_t attributes );
#else
    void set_blksize( blksize_t blksize );
    void set_blocks( blkcnt_t blocks );
#endif

    // YIELD::platform::Stat
    mode_t get_mode() const;
#ifndef _WIN32
    nlink_t get_nlink() const;
    uid_t get_uid() const;
    gid_t get_gid() const;
#endif
    uint64_t get_size() const;
    const YIELD::platform::Time& get_atime() const;
    const YIELD::platform::Time& get_mtime() const;
    const YIELD::platform::Time& get_ctime() const;
#ifdef _WIN32
    uint32_t get_attributes() const;
#else
    blksize_t get_blksize() const;
    blkcnt_t get_blocks() const;
#endif

  private:
    friend class Volume;

    Stat( const org::xtreemfs::interfaces::Stat& stbuf );
  };
};

#endif
