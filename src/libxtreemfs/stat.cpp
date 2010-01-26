#include "stat.h"
#include "xtreemfs/user_credentials_cache.h"
using namespace xtreemfs;

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "xtreemfs/interfaces/mrc_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif


Stat::Stat( const YIELD::platform::Stat& stbuf) 
  : YIELD::platform::Stat( stbuf )
{
  truncate_epoch = 0;
}

Stat::Stat
( 
  const org::xtreemfs::interfaces::Stat& xtreemfs_stbuf,
  UserCredentialsCache& user_credentials_cache
)
: YIELD::platform::Stat  
  (
#ifdef _WIN32
    xtreemfs_stbuf.get_mode(),
    xtreemfs_stbuf.get_nlink(),
    xtreemfs_stbuf.get_size(), 
    xtreemfs_stbuf.get_atime_ns(), 
    xtreemfs_stbuf.get_mtime_ns(), 
    xtreemfs_stbuf.get_ctime_ns(), 
    xtreemfs_stbuf.get_attributes() 
#else
    xtreemfs_stbuf.get_dev(), 
    xtreemfs_stbuf.get_ino(), 
    xtreemfs_stbuf.get_mode(), 
    xtreemfs_stbuf.get_nlink(),
    0, // uid
    0, // gid
    0, // rdev
    xtreemfs_stbuf.get_size(), 
    xtreemfs_stbuf.get_atime_ns(), 
    xtreemfs_stbuf.get_mtime_ns(), 
    xtreemfs_stbuf.get_ctime_ns(),
    xtreemfs_stbuf.get_blksize(),
    0 // blocks
#endif
  ),
  truncate_epoch( xtreemfs_stbuf.get_truncate_epoch() )
{
#ifndef _WIN32
  uid_t uid; gid_t gid;
  getpasswdFromUserCredentials
  ( 
    xtreemfs_stbuf.get_user_id(), 
    xtreemfs_stbuf.get_group_id(), 
    uid, 
    gid 
  );
  set_uid( uid );
  set_gid( gid );
#endif
}

Stat::Stat
( 
  const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response 
)
{
  if ( osd_write_response.get_new_file_size().empty() )
    DebugBreak();

  set_size( osd_write_response.get_new_file_size()[0].get_size_in_bytes() );

  truncate_epoch 
    = osd_write_response.get_new_file_size()[0].get_truncate_epoch();
}

Stat::operator org::xtreemfs::interfaces::Stat() const
{
  return org::xtreemfs::interfaces::Stat
  (
#ifdef _WIN32
    0, // dev
    0, // ino
#else
    get_dev(),
    get_ino(),
#endif
    get_mode(),
#ifdef _WIN32
    0,
#else
    get_nlink(),
#endif
    user_id,
    group_id,
    get_size(),
    get_atime(),
    get_mtime(),
    get_ctime(),
#ifdef _WIN32
    0,
#else
    get_blksize(),
#endif
    truncate_epoch,
#ifdef _WIN32
    get_attributes()
#else
    0
#endif
  );
}