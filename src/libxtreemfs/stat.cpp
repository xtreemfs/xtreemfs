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
  user_credentials_cache.getpasswdFromUserCredentials
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
