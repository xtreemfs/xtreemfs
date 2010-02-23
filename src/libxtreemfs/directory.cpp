#include "directory.h"
#include "stat.h"
#include "xtreemfs/volume.h"
using namespace org::xtreemfs::interfaces;
using namespace xtreemfs;


Directory::Directory
( 
  const org::xtreemfs::interfaces::DirectoryEntrySet& first_directory_entries,
  YIELD::platform::auto_Log log,
  auto_MRCProxy mrc_proxy,
  bool names_only,
  const Path& path
)
: directory_entries( first_directory_entries ),
  log( log ),
  mrc_proxy( mrc_proxy ),
  names_only( names_only ),
  path( path )
{
  seen_directory_entries_count = first_directory_entries.size();
}

YIELD::platform::Directory::auto_Entry Directory::readdir()
{
  if ( directory_entries.empty() )
  {
    try
    {
      mrc_proxy->readdir
      ( 
        path,
        0, // known_etag
        LIMIT_DIRECTORY_ENTRIES_COUNT_DEFAULT,
        false, // names_only
        0, // seen_directory_entries_count
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
  }

  Stat* stbuf;
  if ( names_only )
    stbuf = NULL;
  else
  {
    stbuf = new Stat( directory_entries[0].get_stbuf()[0] );
#ifndef _WIN32
    uid_t uid; gid_t gid;
    user_credentials_cache->getpasswdFromUserCredentials
    (
      directory_entries[0].get_stat().get_user_id(),
      directory_entries[0].get_stat().get_group_id(),
      uid,
      gid
    );
    stbuf->set_uid( uid );
    stbuf->set_gid( gid );
#endif
  }

  YIELD::platform::Directory::Entry* dirent
    = new YIELD::platform::Directory::Entry
          ( 
            directory_entries[0].get_name(), 
            stbuf 
          );

  directory_entries.erase( directory_entries.begin() );

  return dirent;
}
