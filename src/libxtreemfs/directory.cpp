#include "directory.h"
#include "stat.h"
#include "xtreemfs/volume.h"
using namespace xtreemfs;


Directory::Directory
( 
  const DirectoryEntrySet& first_directory_entries,
  Log& log,
  MRCProxy& mrc_proxy,
  bool names_only,
  const Path& path,
  UserCredentialsCache* user_credentials_cache
)
: directory_entries( first_directory_entries ),
  log( log ),
  mrc_proxy( mrc_proxy ),
  names_only( names_only ),
  path( path ),
  user_credentials_cache( user_credentials_cache )
{
  read_directory_entry_i = 0;
  seen_directory_entries_count = first_directory_entries.size();
}

yield::platform::Directory::auto_Entry Directory::readdir()
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
        mrc_proxy->readdir
        ( 
          path,
          0, // known_etag
          LIMIT_DIRECTORY_ENTRIES_COUNT_DEFAULT,
          false, // names_only
          seen_directory_entries_count,
          directory_entries 
        );
      }
      catch ( ProxyExceptionResponse& proxy_exception_response )
      {
        Volume::set_errno( log.get(), "readdir", proxy_exception_response );
        return NULL;
      }
      catch ( std::exception& exc ) \
      {
        Volume::set_errno( log.get(), "readdir", exc );
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

  Stat* stbuf;
  if ( names_only )
    stbuf = NULL;
  else
  {

    stbuf = new Stat( read_directory_entry.get_stbuf()[0] );
#ifndef _WIN32
    uid_t uid; gid_t gid;
    user_credentials_cache->getpasswdFromUserCredentials
    (
      read_directory_entry.get_stbuf()[0].get_user_id(),
      read_directory_entry.get_stbuf()[0].get_group_id(),
      uid,
      gid
    );
    stbuf->set_uid( uid );
    stbuf->set_gid( gid );
#endif
  }
  
  return new yield::platform::Directory::Entry
             ( 
               read_directory_entry.get_name(), 
               stbuf 
             );
}
