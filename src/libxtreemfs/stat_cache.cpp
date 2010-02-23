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
) 
: mrc_proxy( mrc_proxy ), 
  read_ttl( read_ttl ), 
  user_credentials_cache( user_credentials_cache ),
  volume_name( volume_name ),
  write_back_attrs( write_back_attrs )
{ }

StatCache::~StatCache()
{
  for 
  ( 
    EntryMap::iterator entry_i = entries.begin(); 
    entry_i != entries.end(); 
    entry_i++ 
  )
    delete entry_i->second;
}

void StatCache::evict( const YIELD::platform::Path& path )
{
  entries_lock.acquire();

  EntryMap::iterator entry_i = entries.find( path );
  if ( entry_i != entries.end() )
  {
    delete entry_i->second;
    entries.erase( entry_i );
  }

  entries_lock.release();
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
  uint32_t write_through_attrs = ~write_back_attrs & to_set;
  if ( write_through_attrs != 0 )
  {    
    mrc_proxy->fsetattr
    ( 
      *stbuf,
      write_through_attrs,
      write_xcap
    );
  }

  _setattr( path, stbuf, to_set );
}

YIELD::platform::auto_Stat
StatCache::getattr
( 
  const YIELD::platform::Path& path
)
{
  entries_lock.acquire();

  Entry* entry;
  EntryMap::iterator entry_i = entries.find( path );
  if ( entry_i != entries.end() )
  {
    entry = entry_i->second;
    // Check if entry came from the server at some point
    if ( entry->get_refresh_time() > static_cast<uint64_t>( 0 ) )
    {
      if ( YIELD::platform::Time() - entry->get_refresh_time() < read_ttl )
      {
        Stat* stbuf = entry->get_stbuf().release(); 
        entries_lock.release();
        if ( stbuf != NULL )
          return stbuf;
        else // Placeholder for a missing file
          throw org::xtreemfs::interfaces::MRCInterface::
                  MRCException( 2, "file not found", "" );
      }
      // else the entry has expired
    }
    // else the entry has an stbuf that did not come from the server
  }
  else
    entry = NULL;

  // Here we are getting a new Stat from the server
  // (because the old entry expired, there never was one, etc.)

  // Hold the entries_lock through the RPC so that another thread doesn't try
  // to fill and insert entry in parallel
  org::xtreemfs::interfaces::StatSet if_stbuf;
  try
  {
    mrc_proxy->getattr( Path( volume_name, path ), 0, if_stbuf );
  }
  catch ( ... ) // Probably not found
  {
    if ( entry == NULL ) // No entry for this path in the cache
      entries[path] = new Entry; // Placeholder for a missing file
    else if ( entry->get_stbuf() != NULL ) // i.e. this is not a placeholder       
    {                                      // for a missing file
      delete entry; // Discard any unflushed changes
      entry_i->second = new Entry;
    }
    else // This is a placeholder for a missing file
      entry->refresh(); // Update the refresh_time

    entries_lock.release();

    throw;
  }

  // Here the server getattr has been successful
  // Create an entry for the Stat if one doesn't exist already
  if ( entry == NULL )
  {
    entry = new Entry( if_stbuf[0] );
    entries[path] = entry;
  }
  else
    entry->refresh( if_stbuf[0] );

  Stat* entry_stbuf = entry->get_stbuf().release();

#ifndef _WIN32
  // Translate the user_id and group_id from the server Stat to uid and gid
  uid_t uid; gid_t gid;
  user_credentials_cache->getpasswdFromUserCredentials
  (
    if_stbuf.get_user_id(),
    if_stbuf.get_group_id(),
    uid,
    gid
  );

  entry_stbuf->set_uid( uid );
  entry_stbuf->set_gid( gid );
#endif
  
  entries_lock.release();

  return entry_stbuf;
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
    entries_lock.acquire();

    EntryMap::iterator entry_i = entries.find( path );
    if ( entry_i != entries.end() )
    {
      Entry* entry = entry_i->second;
      if ( entry->get_write_back_attrs() != 0 )
      {
        try
        {
          mrc_proxy->fsetattr
          ( 
            *entry->get_stbuf(),
            entry->get_write_back_attrs(),
            write_xcap
          );

          entry->set_write_back_attrs( 0 );
        }
        catch ( ... )
        {
          delete entry;
          entries.erase( entry_i );
          entries_lock.release();
          throw;
        }
      }
      // else nothing to be written, leave it
    }

    entries_lock.release();
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


  uint32_t write_through_attrs = ~write_back_attrs & to_set;
  if ( write_through_attrs != 0 )
    mrc_proxy->setattr( Path( volume_name, path ), *stbuf, write_through_attrs );

  _setattr( path, stbuf, to_set );
}

void StatCache::_setattr
(
  const YIELD::platform::Path& path,
  auto_Stat stbuf, 
  uint32_t to_set
)
{
  uint32_t write_back_attrs = this->write_back_attrs & to_set;

  entries_lock.acquire();

  EntryMap::iterator entry_i = entries.find( path );
  if ( entry_i != entries.end() )
  {
    entry_i->second->change( stbuf, to_set );

    entry_i->second->set_write_back_attrs
    ( 
      entry_i->second->get_write_back_attrs() | write_back_attrs 
    );
  }
  else
    entries[path] = new Entry( stbuf, write_back_attrs );

  entries_lock.release();
}

StatCache::Entry::Entry()
{
  // refresh_time = current_time
  // stbuf = NULL = missing file
  write_back_attrs = 0;
}

StatCache::Entry::Entry( org::xtreemfs::interfaces::Stat stbuf )
  : stbuf( new Stat( stbuf ) )
{
  // refresh_time = current_time
  write_back_attrs = 0;
}

StatCache::Entry::Entry( auto_Stat stbuf, uint32_t write_back_attrs )
  : refresh_time( static_cast<uint64_t>( 0 ) ), 
    stbuf( stbuf ), 
    write_back_attrs( write_back_attrs )
{
  // refresh_time = 0 = this entry has never come from the server
}

void StatCache::Entry::change( auto_Stat stbuf, uint32_t to_set )
{
  if ( this->stbuf != NULL )
    this->stbuf->set( *stbuf, to_set );
  else // This is an entry for a missing file, 
       // which now has metadata set on it?!
    this->stbuf = stbuf;
}

const YIELD::platform::Time& StatCache::Entry::get_refresh_time() const
{
  return refresh_time;
}

void StatCache::Entry::refresh()
{
  refresh_time = YIELD::platform::Time();
}

void StatCache::Entry::refresh( const org::xtreemfs::interfaces::Stat& stbuf )
{
  if ( this->stbuf != NULL )
  {
#ifndef _WIN32
    this->stbuf->set_dev( stbuf.get_dev() );
    this->stbuf->set_ino( stbuf.get_ino() );
#endif

    if 
    ( 
      ( write_back_attrs & YIELD::platform::Volume::SETATTR_MODE )
         != YIELD::platform::Volume::SETATTR_MODE
    )
      this->stbuf->set_mode( static_cast<mode_t>( stbuf.get_mode() ) );

    this->stbuf->set_nlink( static_cast<nlink_t>( stbuf.get_nlink() ) );

#ifndef _WIN32
    if 
    ( 
      ( write_back_attrs & YIELD::platform::Volume::SETATTR_GID )
         != YIELD::platform::Volume::SETATTR_GID
    )
      this->stbuf->set_group_id( stbuf.get_group_id() );

    if 
    ( 
      ( write_back_attrs & YIELD::platform::Volume::SETATTR_UID )
         != YIELD::platform::Volume::SETATTR_UID
    )
      this->stbuf->set_user_id( stbuf.get_user_id() );
#endif

    if ( stbuf.get_truncate_epoch() > this->stbuf->get_truncate_epoch() )
      this->stbuf->set_size( stbuf.get_size() );
    else if ( stbuf.get_truncate_epoch() == this->stbuf->get_truncate_epoch() )
    {
      if ( stbuf.get_size() > this->stbuf->get_size() )
      {
        this->stbuf->set_size( stbuf.get_size() );

        // Pretend we never changed the size
        if 
        ( 
          ( write_back_attrs & YIELD::platform::Volume::SETATTR_SIZE )
             == YIELD::platform::Volume::SETATTR_SIZE
        )
          write_back_attrs ^= YIELD::platform::Volume::SETATTR_SIZE;
      }
    }
    else // A truncate_epoch we got from the server is less than our own?!
      DebugBreak();

    if 
    ( 
      ( write_back_attrs & YIELD::platform::Volume::SETATTR_ATIME )
         != YIELD::platform::Volume::SETATTR_ATIME
    )
      this->stbuf->set_atime( stbuf.get_atime_ns() );

    if 
    ( 
      ( write_back_attrs & YIELD::platform::Volume::SETATTR_MTIME )
         != YIELD::platform::Volume::SETATTR_MTIME
    )
      this->stbuf->set_mtime( stbuf.get_mtime_ns() );

    if 
    ( 
      ( write_back_attrs & YIELD::platform::Volume::SETATTR_CTIME )
         != YIELD::platform::Volume::SETATTR_CTIME
    )
      this->stbuf->set_ctime( stbuf.get_ctime_ns() );

#ifdef _WIN32
    if 
    ( 
      ( write_back_attrs & YIELD::platform::Volume::SETATTR_ATTRIBUTES )
         != YIELD::platform::Volume::SETATTR_ATTRIBUTES
    )
      this->stbuf->set_attributes( stbuf.get_attributes() );
#else
    this->stbuf->set_blksize( stbuf.get_blksize() );
#endif

    this->stbuf->set_truncate_epoch( stbuf.get_truncate_epoch() );

    this->refresh_time = YIELD::platform::Time();
  }
  else // stbuf = NULL, so this entry was a placeholder for a missing file
    this->stbuf = new Stat( stbuf ); // The file exists now
}

void StatCache::Entry::set_write_back_attrs( uint32_t write_back_attrs ) 
{ 
  this->write_back_attrs = write_back_attrs; 
}
