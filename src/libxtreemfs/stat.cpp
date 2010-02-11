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


Stat::Stat( const YIELD::platform::Stat& stbuf )
: YIELD::platform::Stat( stbuf ), refresh_time( static_cast<uint64_t>( 0 ) )
{
  changed_members = 0;
  truncate_epoch = 0;
}

Stat::Stat( const org::xtreemfs::interfaces::Stat& stbuf )
: YIELD::platform::Stat
  (
#ifdef _WIN32
    static_cast<mode_t>( stbuf.get_mode() ),
    static_cast<nlink_t>( stbuf.get_nlink() ),
    stbuf.get_size(),
    stbuf.get_atime_ns(),
    stbuf.get_mtime_ns(),
    stbuf.get_ctime_ns(),
    stbuf.get_attributes()
#else
    stbuf.get_dev(),
    stbuf.get_ino(),
    stbuf.get_mode(),
    stbuf.get_nlink(),
    0, // uid
    0, // gid
    0, // rdev
    stbuf.get_size(),
    stbuf.get_atime_ns(),
    stbuf.get_mtime_ns(),
    stbuf.get_ctime_ns(),
    stbuf.get_blksize(),
    0 // blocks
#endif
  ),
  group_id( stbuf.get_group_id() ),
  truncate_epoch( stbuf.get_truncate_epoch() ),
  user_id( stbuf.get_user_id() )
{
  changed_members = 0;
  // refresh_time is the current time
}

Stat::Stat
(
  const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response
)
: refresh_time( static_cast<uint64_t>( 0 ) )
{
  if ( osd_write_response.get_new_file_size().empty() )
    DebugBreak();

  set_size( osd_write_response.get_new_file_size()[0].get_size_in_bytes() );
  changed_members = YIELD::platform::Volume::SETATTR_SIZE;

  truncate_epoch
    = osd_write_response.get_new_file_size()[0].get_truncate_epoch();
}

const YIELD::platform::Time& Stat::get_refresh_time() const 
{ 
  return refresh_time; 
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

Stat& Stat::operator=( const org::xtreemfs::interfaces::Stat& stbuf )
{
#ifndef _WIN32
  set_dev( stbuf.get_dev() );
  set_ino( stbuf.get_ino() );
#endif

  if 
  ( 
    ( changed_members & YIELD::platform::Volume::SETATTR_MODE )
       != YIELD::platform::Volume::SETATTR_MODE
  )
    set_mode( static_cast<mode_t>( stbuf.get_mode() ) );

  set_nlink( static_cast<nlink_t>( stbuf.get_nlink() ) );

#ifndef _WIN32
  if 
  ( 
    ( changed_members & YIELD::platform::Volume::SETATTR_GID )
       != YIELD::platform::Volume::SETATTR_GID
  )
    set_group_id( stbuf.get_group_id() );

  if 
  ( 
    ( changed_members & YIELD::platform::Volume::SETATTR_UID )
       != YIELD::platform::Volume::SETATTR_UID
  )
    set_user_id( stbuf.get_user_id() );
#endif

  if ( stbuf.get_truncate_epoch() > truncate_epoch )
    set_size( stbuf.get_size() );
  else if ( stbuf.get_truncate_epoch() == truncate_epoch )
  {
    if ( stbuf.get_size() > get_size() )
    {
      set_size( stbuf.get_size() );

      // Pretend we never changed the size
      if 
      ( 
        ( changed_members & YIELD::platform::Volume::SETATTR_SIZE )
           == YIELD::platform::Volume::SETATTR_SIZE
      )
        changed_members ^= YIELD::platform::Volume::SETATTR_SIZE;
    }
  }
  else // A truncate_epoch we got from the server is less than our own?!
    DebugBreak();

  if 
  ( 
    ( changed_members & YIELD::platform::Volume::SETATTR_ATIME )
       != YIELD::platform::Volume::SETATTR_ATIME
  )
    set_atime( stbuf.get_atime_ns() );

  if 
  ( 
    ( changed_members & YIELD::platform::Volume::SETATTR_MTIME )
       != YIELD::platform::Volume::SETATTR_MTIME
  )
    set_mtime( stbuf.get_mtime_ns() );

  if 
  ( 
    ( changed_members & YIELD::platform::Volume::SETATTR_CTIME )
       != YIELD::platform::Volume::SETATTR_CTIME
  )
    set_ctime( stbuf.get_ctime_ns() );

#ifdef _WIN32
  if 
  ( 
    ( changed_members & YIELD::platform::Volume::SETATTR_ATTRIBUTES )
       != YIELD::platform::Volume::SETATTR_ATTRIBUTES
  )
    set_attributes( stbuf.get_attributes() );
#else
  set_blksize( stbuf.get_blksize() );
#endif


  truncate_epoch = stbuf.get_truncate_epoch();

  refresh_time = YIELD::platform::Time();

  return *this;
}

void Stat::set( const YIELD::platform::Stat& other, uint32_t to_set )
{
  YIELD::platform::Stat::set( other, to_set );
  changed_members |= to_set;
}

void Stat::set_group_id( const std::string& group_id ) 
{ 
  this->group_id = group_id; 
}

void Stat::set_user_id( const std::string& user_id )
{ 
  this->user_id = user_id; 
}
