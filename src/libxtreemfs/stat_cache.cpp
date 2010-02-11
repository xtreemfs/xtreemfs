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


#include "stat_cache.h"
#include "xtreemfs/path.h"
using namespace xtreemfs;


StatCache::StatCache
( 
  auto_MRCProxy mrc_proxy, 
  const YIELD::platform::Time& read_ttl, 
  auto_UserCredentialsCache user_credentials_cache,
  const std::string& volume_name,
  uint32_t write_back_attrs
) : mrc_proxy( mrc_proxy ), 
    read_ttl( read_ttl ), 
    user_credentials_cache( user_credentials_cache ),
    volume_name( volume_name ),
    write_back_attrs( write_back_attrs )
{ }

StatCache::~StatCache()
{
  for ( iterator stat_i = begin(); stat_i != end(); stat_i++ )
    delete stat_i->second;
}

void StatCache::evict( const YIELD::platform::Path& path )
{
  lock.acquire();

  iterator stat_i = find( path );
  if ( stat_i != end() )
  {
#ifdef _DEBUG
    if ( stat_i->second->get_changed_members() != 0 )
      DebugBreak();
#endif

    delete stat_i->second;
    erase( stat_i );
  }

  lock.release();
}

void 
StatCache::fsetattr
( 
  const YIELD::platform::Path& path,
  auto_Stat stbuf,
  uint32_t to_set,
  const org::xtreemfs::interfaces::XCap& write_xcap
)
{
  if ( should_write_through( to_set ) )
  {    
    mrc_proxy->fsetattr
    ( 
      write_xcap, 
      *stbuf,
      to_set
    );

    _setattr( path, stbuf, to_set, true );
  }
  else
    _setattr( path, stbuf, to_set, false );
}

YIELD::platform::auto_Stat
StatCache::getattr
( 
  const YIELD::platform::Path& path
)
{
  lock.acquire();

  Stat* stbuf;
  iterator stat_i = find( path );
  if ( stat_i != end() )
  {
    stbuf = stat_i->second;
    // Check if stbuf came from the server at some point
    if ( stbuf->get_refresh_time() > static_cast<uint64_t>( 0 ) ) 
    {
      if ( YIELD::platform::Time() - stbuf->get_refresh_time() < read_ttl )
      {
        stbuf->incRef();
        lock.release();
        return stbuf;
      }
      // else stbuf has expired
    }
    // else stbuf did not come from the server
  }
  else
    stbuf = NULL;


  // Hold the lock through the RPC so that another thread doesn't try
  // to fill and insert entry in parallel
  org::xtreemfs::interfaces::Stat if_stbuf;
  try
  {
    mrc_proxy->getattr( Path( volume_name, path ), if_stbuf );
  }
  catch ( ... )
  {
    delete stbuf; // Discard any unflushed changes
    if ( stat_i != end() )     
      erase( stat_i );
    lock.release();
    throw;
  }

  if ( stbuf == NULL )
  {
    stbuf = new Stat( if_stbuf );
    operator[]( path ) = stbuf;
  }
  else
    *stbuf = if_stbuf;

#ifndef _WIN32
  uid_t uid; gid_t gid;
  user_credentials_cache->getpasswdFromUserCredentials
  (
    stbuf->get_user_id(),
    stbuf->get_group_id(),
    uid,
    gid
  );

  stbuf->set_uid( uid );
  stbuf->set_gid( gid );
#endif

  stbuf->incRef();  
  
  lock.release();

  return stbuf;
}

void 
StatCache::metadatasync
( 
  const YIELD::platform::Path& path, 
  const org::xtreemfs::interfaces::XCap& write_xcap 
)
{
  if ( write_back_attrs != 0 )
  {
    lock.acquire();

    iterator stat_i = find( path );
    if ( stat_i != end() )
    {
      Stat* stbuf = stat_i->second;
      if ( stbuf->get_changed_members() != 0 )
      {
        try
        {
          mrc_proxy->fsetattr
          ( 
            write_xcap, 
            *stbuf,
            stbuf->get_changed_members()
          );

          stbuf->set_changed_members( 0 );
        }
        catch ( ... )
        {
          delete stbuf;
          erase( stat_i );
          lock.release();
          throw;
        }
      }
      // else nothing to be written, leave it
    }

    lock.release();
  }
}

void 
StatCache::setattr
( 
  const YIELD::platform::Path& path,
  auto_Stat stbuf, 
  uint32_t to_set
)
{
#ifdef _DEBUG
  if 
  ( 
    ( to_set & YIELD::platform::Volume::SETATTR_SIZE ) 
      == YIELD::platform::Volume::SETATTR_SIZE 
  )
    DebugBreak();
#endif

  if ( should_write_through( to_set ) )
  {
    mrc_proxy->setattr( Path( volume_name, path ), *stbuf, to_set );
    _setattr( path, stbuf, to_set, true );
  }
  else
    _setattr( path, stbuf, to_set, false );
}

void StatCache::_setattr
(
  const YIELD::platform::Path& path,
  auto_Stat stbuf, 
  uint32_t to_set,
  bool wrote_through
)
{
  lock.acquire();

  iterator stat_i = find( path );
  if ( stat_i != end() )
  {
    if ( !wrote_through )
    {
      stat_i->second->set_changed_members
      ( 
        stat_i->second->get_changed_members() | to_set 
      );
    }

    stat_i->second->set( *stbuf, to_set );
  }
  else
  {
    if ( !wrote_through )
    {
#ifdef _DEBUG
      if ( stbuf->get_changed_members() != 0 )
        DebugBreak();
#endif

      stbuf->set_changed_members( to_set );
    }

    operator[]( path ) = stbuf.release();
  }

  lock.release();
}

bool StatCache::should_write_through( uint32_t to_set ) const
{
  if ( write_back_attrs == 0 )
    return true;
  else
  {
    for ( uint8_t setattr_bit_i = 0; setattr_bit_i < 31; setattr_bit_i++ )
    {
      uint32_t setattr_bit = 1 << setattr_bit_i;
      if 
      ( 
        ( to_set & setattr_bit ) == setattr_bit
        && 
        ( write_back_attrs & setattr_bit ) != setattr_bit
      )
      {
        // There is a SETATTR_* in to_set that is not in write_back_attrs
        // -> write through
        return true;
      }
    }

    return false;
  }
}