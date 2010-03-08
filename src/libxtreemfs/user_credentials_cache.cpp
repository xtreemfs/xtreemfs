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


#include "xtreemfs/user_credentials_cache.h"
using yield::platform::iconv;
using yield::platform::Path;
using yield::platform::SharedLibrary;
using namespace xtreemfs;

#ifdef _WIN32
#include <windows.h>
#include <lm.h>
#pragma comment( lib, "Netapi32.lib" )
#else
#include "yieldfs.h"
#include <grp.h>
#include <pwd.h>
#endif


UserCredentialsCache::UserCredentialsCache()
{
#ifndef _WIN32
  get_user_credentials_from_passwd
    = reinterpret_cast<get_user_credentials_from_passwd_t>
      (
        getPolicyFunction
        (
          "get_user_credentials_from_passwd"
        )
      );

  get_passwd_from_user_credentials
    = reinterpret_cast<get_passwd_from_user_credentials_t>
      (
        getPolicyFunction
        (
          "get_passwd_from_user_credentials"
        )
      );
#endif

}

UserCredentialsCache::~UserCredentialsCache()
{
  for
  (
    std::vector<SharedLibrary*>::iterator 
      policy_shared_library_i = policy_shared_libraries.begin();
    policy_shared_library_i != policy_shared_libraries.end();
    policy_shared_library_i++
  )
    yield::platform::SharedLibrary::dec_ref( **policy_shared_library_i );

#ifndef _WIN32
  for
  (
    std::map<std::string,std::map<std::string,std::pair<uid_t,gid_t>*>*>::iterator
      i = user_credentials_to_passwd_cache.begin();
    i != user_credentials_to_passwd_cache.end();
    i++
  )
  {
    for
    (
      std::map<std::string,std::pair<uid_t,gid_t>*>::iterator
        j = i->second->begin();
      j != i->second->end();
      j++
    )
      delete j->second;

    delete i->second;
  }

  for
  (
    std::map<gid_t,std::map<uid_t,UserCredentials*>*>::iterator
      i = passwd_to_user_credentials_cache.begin();
    i != passwd_to_user_credentials_cache.end();
    i++
  )
  {
    for
    (
      std::map<uid_t,UserCredentials*>::iterator
        j = i->second->begin();
      j != i->second->end();
      j++
    )
      delete j->second;

    delete i->second;
  }
#endif
}

UserCredentials* UserCredentialsCache::getCurrentUserCredentials()
{
//#ifdef _DEBUG
//  if
//  (
//    ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) ==
//    PROXY_FLAG_TRACE_AUTH && log != NULL
//  )
//    log->getStream( yield::platform::Log::LOG_DEBUG ) <<
//      "xtreemfs::Proxy: getting current user credentials.";
//#endif

#ifdef _WIN32
  DWORD dwLevel = 1;
  LPWKSTA_USER_INFO_1 user_info = NULL;
  if
  (
    NetWkstaUserGetInfo
    (
      NULL,
      dwLevel,
      ( LPBYTE *)&user_info
    ) == NERR_Success
    &&
    user_info != NULL
  )
  {    
    iconv* iconv = iconv::open( iconv::CODE_CHAR, iconv::CODE_UTF8 );
    if ( iconv != NULL )
    {
      std::string group_id, user_id;
      ( *iconv )( user_info->wkui1_username, user_id );
      ( *iconv )( user_info->wkui1_logon_domain, group_id );
      UserCredentials* user_credentials
        = new UserCredentials( user_id, group_id, "" );

      iconv::dec_ref( *iconv );
      NetApiBufferFree( user_info );

      return user_credentials;
    }
  }

    return NULL;
#else
  uid_t caller_uid = yieldfs::FUSE::geteuid();
  if ( caller_uid == static_cast<uid_t>( -1 ) )
    caller_uid = ::geteuid();

  gid_t caller_gid = yieldfs::FUSE::getegid();
  if ( caller_gid == static_cast<gid_t>( -1 ) )
    caller_gid = ::getegid();

  return getUserCredentialsFrompasswd( caller_uid, caller_gid );
#endif
}


void* UserCredentialsCache::getPolicyFunction( const char* name )
{
  for
  (
    std::vector<SharedLibrary*>::iterator
      policy_shared_library_i = policy_shared_libraries.begin();
    policy_shared_library_i != policy_shared_libraries.end();
    policy_shared_library_i++
  )
  {
    void* policy_function = ( *policy_shared_library_i )->getFunction( name );
    if ( policy_function != NULL )
      return policy_function;
  }

  std::vector<Path> policy_dir_paths;
  policy_dir_paths.push_back( Path() );
#ifdef _WIN32
  policy_dir_paths.push_back( "src\\policies\\lib" );
#else
  policy_dir_paths.push_back( "src/policies/lib" );
  policy_dir_paths.push_back( "/lib/xtreemfs/policies/" );
#endif

  yield::platform::Volume* volume = new yield::platform::Volume;
  for
  (
    std::vector<Path>::iterator
      policy_dir_path_i = policy_dir_paths.begin();
    policy_dir_path_i != policy_dir_paths.end();
    policy_dir_path_i++
  )
  {
    yield::platform::Directory* dir = volume->opendir( *policy_dir_path_i );

    if ( dir != NULL )
    {
      yield::platform::Directory::Entry* dirent = dir->readdir();

      while ( dirent != NULL )
      {
        if ( volume->isfile( *policy_dir_path_i / dirent->get_name() ) )
        {
          const Path::string_type& file_name = dirent->get_name();
          Path::string_type::size_type dll_pos 
            = file_name.find( SharedLibrary::SHLIBSUFFIX );

          if
          (
            dll_pos != Path::string_type::npos &&
            dll_pos != 0 &&
            file_name[dll_pos-1] == '.'
          )
          {
            SharedLibrary* policy_shared_library
              = SharedLibrary::open( *policy_dir_path_i / file_name  );

            if ( policy_shared_library != NULL )
            {
              void* policy_function 
                = policy_shared_library->getFunction( name );
              if ( policy_function != NULL )
                policy_shared_libraries.push_back( policy_shared_library );
              return policy_function;
            }
          }

          yield::platform::Directory::Entry::dec_ref( *dirent );

          dirent = dir->readdir();
        }
      }

      yield::platform::Directory::dec_ref( *dir );
    }
  }

  return NULL;
}

#ifndef _WIN32
void
UserCredentialsCache::getpasswdFromUserCredentials
(
  const std::string& user_id,
  const std::string& group_id,
  uid_t& out_uid,
  gid_t& out_gid
)
{
//#ifdef _DEBUG
//  if
//  (
//    ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH &&
//    log != NULL
//  )
//    log->getStream( yield::platform::Log::LOG_DEBUG ) <<
//      "xtreemfs::Proxy: getting passwd from UserCredentials (user_id=" <<
//      user_id << ", group_id=" << group_id << ").";
//#endif

  user_credentials_to_passwd_cache_lock.acquire();

  std::map<std::string,std::map<std::string,std::pair<uid_t, gid_t>*>*>::iterator
    group_i = user_credentials_to_passwd_cache.find( group_id );

  if ( group_i != user_credentials_to_passwd_cache.end() )
  {
    std::map<std::string,std::pair<uid_t,gid_t>*>::iterator user_i =
      group_i->second->find( user_id );

    if ( user_i != group_i->second->end() )
    {
      out_uid = user_i->second->first;
      out_gid = user_i->second->second;

      user_credentials_to_passwd_cache_lock.release();

//#ifdef _DEBUG
//      if
//      (
//        ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH &&
//        log != NULL
//      )
//        log->getStream( yield::platform::Log::LOG_DEBUG ) <<
//          "xtreemfs::Proxy: found user and group IDs in cache, " <<
//          user_id << "=" << out_uid << ", " << group_id << "=" <<
//          out_gid << ".";
//#endif


      return;
    }
  }

  user_credentials_to_passwd_cache_lock.release();


  bool have_passwd = false;
  if ( get_passwd_from_user_credentials )
  {
//#ifdef _DEBUG
//    if
//    (
//      ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH &&
//      log != NULL
//    )
//      log->getStream( yield::platform::Log::LOG_DEBUG ) <<
//        "xtreemfs::Proxy: calling get_passwd_from_user_credentials_ret " <<
//        "with user_id=" << user_id << ", group_id=" << group_id << ".";
//#endif

    int get_passwd_from_user_credentials_ret =
      get_passwd_from_user_credentials
      (
        user_id.c_str(),
        group_id.c_str(),
        &out_uid,
        &out_gid
      );
    if ( get_passwd_from_user_credentials_ret >= 0 )
      have_passwd = true;
    //else if
    //(
    //  ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) ==
    //  PROXY_FLAG_TRACE_AUTH && log != NULL
    //)
    //  log->getStream( yield::platform::Log::LOG_ERR ) <<
    //    "xtreemfs::Proxy: get_passwd_from_user_credentials_ret with user_id="
    //    << user_id << ", group_id=" << group_id << " failed with errno=" <<
    //    ( get_passwd_from_user_credentials_ret * -1 );
  }

  if ( !have_passwd )
  {
//#ifdef _DEBUG
//    if
//    (
//      ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH &&
//      log != NULL
//    )
//      log->getStream( yield::platform::Log::LOG_DEBUG ) <<
//        "xtreemfs::Proxy: calling getpwnam_r and getgrnam_r with user_id=" <<
//        user_id << ", group_id=" << group_id << ".";
//#endif

    struct passwd pwd, *pwd_res;
    int pwd_buf_len = sysconf( _SC_GETPW_R_SIZE_MAX );
    if ( pwd_buf_len <= 0 ) pwd_buf_len = 1024;
    char* pwd_buf = new char[pwd_buf_len];

    struct group grp, *grp_res;
    int grp_buf_len = sysconf( _SC_GETGR_R_SIZE_MAX );
    if ( grp_buf_len <= 0 ) grp_buf_len = 1024;
    char* grp_buf = new char[grp_buf_len];

    if
    (
      getpwnam_r
      (
        user_id.c_str(),
        &pwd,
        pwd_buf,
        pwd_buf_len,
        &pwd_res
      ) == 0 && pwd_res != NULL
      &&
      getgrnam_r
      (
        group_id.c_str(),
        &grp,
        grp_buf,
        grp_buf_len,
        &grp_res
      ) == 0 && grp_res != NULL
    )
    {
      out_uid = pwd_res->pw_uid;
      out_gid = grp_res->gr_gid;
    }
    else
    {
      out_uid = 0;
      out_gid = 0;
      //if ( log != NULL )
      //  log->getStream( yield::platform::Log::LOG_WARNING ) <<
      //    "xtreemfs::Proxy: getpwnam_r and getgrnam_r with user_id=" <<
      //    user_id << ", group_id=" << group_id << " failed, errno=" <<
      //    errno << ", setting user/group to root.";
    }

    delete [] pwd_buf;
    delete [] grp_buf;
  }

//#ifdef _DEBUG
//  if
//  (
//    ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH &&
//      log != NULL
//  )
//    log->getStream( yield::platform::Log::LOG_DEBUG ) << "xtreemfs::Proxy: " <<
//      user_id << "=" << out_uid << ", " <<
//      group_id << "=" << out_gid <<
//      ", storing in cache.";
//#endif

  user_credentials_to_passwd_cache_lock.acquire();

  if ( group_i != user_credentials_to_passwd_cache.end() )
  {
    group_i->second->insert
    (
      std::make_pair
      (
        user_id,
        new std::pair<uid_t,gid_t>( out_uid, out_gid )
      )
    );
  }
  else
  {
    user_credentials_to_passwd_cache[group_id] =
      new std::map<std::string,std::pair<uid_t,gid_t>*>;

    user_credentials_to_passwd_cache[group_id]->insert
    (
      std::make_pair( user_id, new std::pair<uid_t,gid_t>( out_uid, out_gid ) )
    );
  }

  user_credentials_to_passwd_cache_lock.release();
}

bool
UserCredentialsCache::getUserCredentialsFrompasswd
(
  uid_t uid,
  gid_t gid,
  UserCredentials& out_user_credentials
)
{
//#ifdef _DEBUG
//  if
//  (
//    ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH &&
//    log != NULL
//  )
//    log->getStream( yield::platform::Log::LOG_DEBUG ) <<
//      "xtreemfs::Proxy: getting UserCredentials from passwd (uid=" <<
//      uid << ", gid=" << gid << ").";
//#endif

  passwd_to_user_credentials_cache_lock.acquire();

  std::map<gid_t,std::map<uid_t,UserCredentials*>*>
    ::iterator group_i = passwd_to_user_credentials_cache.find( gid );

  if ( group_i != passwd_to_user_credentials_cache.end() )
  {
    std::map<uid_t,UserCredentials*>::iterator
      user_i = group_i->second->find( uid );

    if ( user_i != group_i->second->end() )
    {
      out_user_credentials = *user_i->second;

      passwd_to_user_credentials_cache_lock.release();

//#ifdef _DEBUG
//      if
//      (
//        ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH &&
//        log != NULL
//      )
//        log->getStream( yield::platform::Log::LOG_DEBUG ) <<
//          "xtreemfs::Proxy: found UserCredentials in cache, " <<
//          uid << "=" << out_user_credentials.get_user_id() << ", " <<
//          gid << "=" << out_user_credentials.get_group_ids()[0] << ".";
//#endif

      return true;
    }
  }

  passwd_to_user_credentials_cache_lock.release();


  if ( get_user_credentials_from_passwd )
  {
//#ifdef _DEBUG
//    if
//    (
//      ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH &&
//      log != NULL
//    )
//      log->getStream( yield::platform::Log::LOG_DEBUG ) <<
//        "xtreemfs::Proxy: calling get_user_credentials_from_passwd with uid="
//        << uid << ", gid=" << gid << ".";
//#endif

    size_t user_id_len = 0, group_ids_len = 0;
    int get_user_credentials_from_passwd_ret
      = get_user_credentials_from_passwd
        (
          uid,
          gid,
          NULL,
          &user_id_len,
          NULL,
          &group_ids_len
        );
    if ( get_user_credentials_from_passwd_ret >= 0 )
    {
//#ifdef _DEBUG
//      if
//      (
//        ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH &&
//        log != NULL
//      )
//        log->getStream( yield::platform::Log::LOG_DEBUG ) <<
//          "xtreemfs::Proxy: calling get_user_credentials_from_passwd " <<
//          "with uid=" << uid << ", gid=" << gid << " returned " <<
//          get_user_credentials_from_passwd_ret <<
//          ", allocating space for UserCredentials.";
//#endif

      if ( user_id_len > 0 ) // group_ids_len can be 0
      {
        char* user_id = new char[user_id_len];
        char* group_ids = group_ids_len > 0 ? new char[group_ids_len] : NULL;

        get_user_credentials_from_passwd_ret =
          get_user_credentials_from_passwd
          (
            uid,
            gid,
            user_id,
            &user_id_len,
            group_ids,
            &group_ids_len
          );
        if ( get_user_credentials_from_passwd_ret >= 0 )
        {
          out_user_credentials.set_user_id( user_id );

          if ( group_ids_len > 0 )
          {
            char* group_ids_p = group_ids;
            StringSet group_ids_ss;
            while
            (
              static_cast<size_t>( group_ids_p - group_ids ) < group_ids_len
            )
            {
              group_ids_ss.push_back( group_ids_p );
              group_ids_p += group_ids_ss.back().size() + 1;
            }

            out_user_credentials.set_group_ids( group_ids_ss );
          }
          else
            out_user_credentials.set_group_ids
            (
              StringSet( "" )
            );

//#ifdef _DEBUG
//          if
//          (
//            ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) ==
//            PROXY_FLAG_TRACE_AUTH &&
//            log != NULL
//          )
//            log->getStream( yield::platform::Log::LOG_DEBUG ) <<
//              "xtreemfs::Proxy: get_user_credentials_from_passwd succeeded, " <<
//              uid << "=" << out_user_credentials.get_user_id() << ", " <<
//              gid << "=" << out_user_credentials.get_group_ids()[0] << ".";
//#endif

          // Drop down to insert the credentials into the cache
        }
        else
          return false;
      }
    }
    else
      return false;
  }
  else
  {
    if ( uid != static_cast<uid_t>( -1 ) )
    {
      struct passwd pwd, *pwd_res;
      int pwd_buf_len = sysconf( _SC_GETPW_R_SIZE_MAX );
      if ( pwd_buf_len <= 0 ) pwd_buf_len = 1024;
      char* pwd_buf = new char[pwd_buf_len];

      if ( getpwuid_r( uid, &pwd, pwd_buf, pwd_buf_len, &pwd_res ) == 0 )
      {
        if ( pwd_res != NULL && pwd_res->pw_name != NULL )
        {
          out_user_credentials.set_user_id( pwd_res->pw_name );
          delete [] pwd_buf;
        }
        else
        {
          delete [] pwd_buf;
          return false;
        }
      }
      else
      {
        delete [] pwd_buf;
        return false;
      }
    }
    else
      out_user_credentials.set_user_id( "" );

    if ( gid != static_cast<gid_t>( -1 ) )
    {
      struct group grp, *grp_res;
      int grp_buf_len = sysconf( _SC_GETGR_R_SIZE_MAX );
      if ( grp_buf_len <= 0 ) grp_buf_len = 1024;
      char* grp_buf = new char[grp_buf_len];

      if ( getgrgid_r( gid, &grp, grp_buf, grp_buf_len, &grp_res ) == 0 )
      {
        if ( grp_res != NULL && grp_res->gr_name != NULL )
        {
          out_user_credentials.set_group_ids
          (
            StringSet( grp_res->gr_name )
          );
          delete [] grp_buf;
          // Drop down to insert the credentials into the cache
        }
        else
        {
          delete [] grp_buf;
          return false;
        }
      }
      else
      {
        delete [] grp_buf;
        return false;
      }
    }
    else
      out_user_credentials.set_group_ids
      (
        StringSet( "" )
      );
      // Drop down to insert the credentials into the cache
  }

  passwd_to_user_credentials_cache_lock.acquire();

  if ( group_i != passwd_to_user_credentials_cache.end() )
  {
    group_i->second->insert
    (
      std::make_pair
      (
        uid,
        new UserCredentials( out_user_credentials )
      )
    );
  }
  else
  {
    passwd_to_user_credentials_cache[gid] =
      new std::map<uid_t,UserCredentials*>;

    passwd_to_user_credentials_cache[gid]->insert
    (
      std::make_pair
      (
        uid,
        new UserCredentials( out_user_credentials )
      )
    );
  }

  passwd_to_user_credentials_cache_lock.release();

  return true;
}
#endif
