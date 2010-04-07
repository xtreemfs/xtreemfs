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


#include "directory.h"
#include "stat.h"
#include "user_database.h"
#include "xtreemfs/volume.h"
using namespace xtreemfs;
using org::xtreemfs::interfaces::DirectoryEntry;

#include "yield.h"
using yield::platform::iconv;


Directory::Directory
(
  const DirectoryEntrySet& first_directory_entries,
  bool names_only,
  Volume& parent_volume,
  const Path& path
)
: directory_entries( first_directory_entries ),
  names_only( names_only ),
  parent_volume( parent_volume.inc_ref() ),
  path( path )
{
  read_directory_entry_i = 0;
  seen_directory_entries_count = first_directory_entries.size();
}

Directory::~Directory()
{
  Volume::dec_ref( parent_volume );
}

yield::platform::Directory::Entry* Directory::readdir()
{
  if ( read_directory_entry_i >= directory_entries.size() )
  {
    // ^ We've read all of the entries we got from the server
    if ( directory_entries.size() == LIMIT_DIRECTORY_ENTRIES_COUNT_DEFAULT )
    {
      // ^ The last server readdir returned the maximum number of entries,
      // try to read again

      directory_entries.clear();

      try
      {
        parent_volume.get_mrc_proxy().readdir
        (
          parent_volume.get_name(),
          path.encode( iconv::CODE_UTF8 ),
          0, // known_etag
          LIMIT_DIRECTORY_ENTRIES_COUNT_DEFAULT,
          false, // names_only
          seen_directory_entries_count,
          directory_entries
        );
      }
      catch ( Exception& exception )
      {
        parent_volume.set_errno( "readdir", exception );
        return NULL;
      }
      catch ( std::exception& exception ) \
      {
        parent_volume.set_errno( "readdir", exception );
        return NULL;
      }

      if ( !directory_entries.empty() )
      {
        read_directory_entry_i = 0;
        seen_directory_entries_count += directory_entries.size();
      }
      else // No more directory entries from the server
        return NULL;
    }
    else           // The last server readdir returned fewer than
      return NULL; // the requested renumber of entries
  }

  const DirectoryEntry& read_directory_entry
    = directory_entries[read_directory_entry_i];
  ++read_directory_entry_i;

  if ( names_only )
  {
    return new Entry
               (
                 Path( read_directory_entry.get_name(), iconv::CODE_UTF8 )
               );
  }
  else
  {
    Stat* stbuf = new Stat( read_directory_entry.get_stbuf()[0] );
#ifndef _WIN32
    uid_t uid; gid_t gid;
    parent_volume.get_user_database().getpasswdFromUserCredentials
    (
      read_directory_entry.get_stbuf()[0].get_user_id(),
      read_directory_entry.get_stbuf()[0].get_group_id(),
      uid,
      gid
    );
    stbuf->set_uid( uid );
    stbuf->set_gid( gid );
#endif
    return new Entry
               (
                 Path( read_directory_entry.get_name(), iconv::CODE_UTF8 ),
                 *stbuf
               );
  }
}
