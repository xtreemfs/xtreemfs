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


#include <memory>
using std::auto_ptr;

#include "user_database.h"
using org::xtreemfs::interfaces::StringSet;
using namespace xtreemfs;

#include "yield.h"
using yield::platform::Directory;
using yield::platform::iconv;
using yield::platform::LockHolder;
using yield::platform::Path;
using yield::platform::SharedLibrary;

#ifdef _WIN32
#include <windows.h>
#include <lm.h>
#pragma comment( lib, "Netapi32.lib" )
#else
#include "yieldfs.h"
#include <grp.h>
#include <pwd.h>
#endif


UserDatabase::UserDatabase( Log* trace_log )
  : trace_log( Object::inc_ref( trace_log ) )
{
#ifndef _WIN32
  _get_user_credentials
    = reinterpret_cast<get_user_credentials_t>
      (
        get_policy_function
        (
          "get_user_credentials"
        )
      );

  _get_passwd
    = reinterpret_cast<get_passwd_t>
      (
        get_policy_function
        (
          "get_passwd"
        )
      );
#endif
}

UserDatabase::~UserDatabase()
{
  Log::dec_ref( trace_log );

  for
  (
    vector<SharedLibrary*>::iterator
      policy_shared_library_i = policy_shared_libraries.begin();
    policy_shared_library_i != policy_shared_libraries.end();
    policy_shared_library_i++
  )
    yield::platform::SharedLibrary::dec_ref( **policy_shared_library_i );

#ifndef _WIN32
  for
  (
    map<string,map<string,pair<uid_t,gid_t>*>*>::iterator
      i = passwd_cache.begin();
    i != passwd_cache.end();
    i++
  )
  {
    for
    (
      map<string,pair<uid_t,gid_t>*>::iterator
        j = i->second->begin();
      j != i->second->end();
      j++
    )
      delete j->second;

    delete i->second;
  }

  for
  (
    map<gid_t,map<uid_t,UserCredentials*>*>::iterator
      i = user_credentials_cache.begin();
    i != user_credentials_cache.end();
    i++
  )
  {
    for
    (
      map<uid_t,UserCredentials*>::iterator
        j = i->second->begin();
      j != i->second->end();
      j++
    )
      delete j->second;

    delete i->second;
  }
#endif
}

UserCredentials* UserDatabase::get_current_user_credentials()
{
  if ( trace_log != NULL )
    trace_log->get_stream( Log::LOG_DEBUG ) <<
      "xtreemfs::UserDatabase: getting current user credentials.";

#ifdef _WIN32
  DWORD dwLevel = 1;
  LPWKSTA_USER_INFO_1 user_info = NULL;
  if
  (
    NetWkstaUserGetInfo( NULL, dwLevel, ( LPBYTE *)&user_info ) == NERR_Success
    &&
    user_info != NULL
  )
  {
    iconv* iconv = iconv::open( iconv::CODE_CHAR, iconv::CODE_UTF8 );
    if ( iconv != NULL )
    {
      string group_id, user_id;
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

  return get_user_credentials( caller_uid, caller_gid );
#endif
}


void* UserDatabase::get_policy_function( const char* name )
{
  for
  (
    vector<SharedLibrary*>::iterator
      policy_shared_library_i = policy_shared_libraries.begin();
    policy_shared_library_i != policy_shared_libraries.end();
    policy_shared_library_i++
  )
  {
    void* policy_function = ( *policy_shared_library_i )->sym( name );
    if ( policy_function != NULL )
      return policy_function;
  }

  vector<Path> policy_dir_paths;
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
    vector<Path>::iterator policy_dir_path_i = policy_dir_paths.begin();
    policy_dir_path_i != policy_dir_paths.end();
    policy_dir_path_i++
  )
  {
    Directory* dir = volume->opendir( *policy_dir_path_i );

    if ( dir != NULL )
    {
      Directory::Entry* dirent = dir->read();

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
              void* policy_function = policy_shared_library->sym( name );

              if ( policy_function != NULL )
              {
                if ( trace_log != NULL )
                {
                  trace_log->get_stream( Log::LOG_INFO ) <<
                    "xtreemfs::UserDatabase: found a " << name <<
                    " policy function in " << file_name << ".";
                }

                policy_shared_libraries.push_back( policy_shared_library );
              }

              return policy_function;
            }
          }

          yield::platform::Directory::Entry::dec_ref( *dirent );

          dirent = dir->read();
        }
      }

      yield::platform::Directory::dec_ref( *dir );
    }
  }

  return NULL;
}

#ifndef _WIN32

struct passwd
UserDatabase::get_passwd
( 
  const string& user_id,
  const string& group_id
)
{
  if ( trace_log != NULL )
  {
    trace_log->get_stream( Log::LOG_DEBUG ) <<
      "xtreemfs::UserDatabase: getting passwd from UserCredentials (user_id=" <<
      user_id << ", group_id=" << group_id << ").";
  }

  struct passwd pwd;
  memset( &pwd, 0, sizeof( pwd ) );
  bool have_passwd = false;

  LockHolder<Mutex> lock_holder( lock );

  map<string,map<string,pair<uid_t,gid_t>*>*>::iterator
    group_i = passwd_cache.find( group_id );

  if ( group_i != passwd_cache.end() )
  {
    map<string,pair<uid_t,gid_t>*>::iterator user_i =
      group_i->second->find( user_id );

    if ( user_i != group_i->second->end() )
    {
      pwd.pw_uid = user_i->second->first;
      pwd.pw_gid = user_i->second->second;

      if ( trace_log != NULL )
        trace_log->get_stream( Log::LOG_DEBUG ) <<
          "xtreemfs::UserDatabase: found user and group IDs in cache, " <<
          user_id << "=" << pwd.pw_uid << ", " << group_id << "=" <<
          pwd.pw_gid << ".";

      return pwd;
    }
  }

  if ( _get_passwd != NULL )
  {
    if ( trace_log != NULL )
    {
      trace_log->get_stream( Log::LOG_DEBUG ) <<
        "xtreemfs::UserDatabase: calling get_passwd" <<
        " with user_id=" << user_id << ", group_id=" << group_id << ".";
    }

    int get_passwd_ret =
      _get_passwd
      (
        user_id.c_str(),
        group_id.c_str(),
        &pwd.pw_uid,
        &pwd.pw_gid
      );

    if ( get_passwd_ret >= 0 )
      have_passwd = true;
    else if ( trace_log != NULL )
    {
      trace_log->get_stream( Log::LOG_ERR ) <<
        "xtreemfs::UserDatabase: get_passwd_ret with user_id="
        << user_id << ", group_id=" << group_id << 
        " failed with errno=" << ( get_passwd_ret * -1 );
    }
  }

  if ( !have_passwd )
  {
    if ( trace_log != NULL )
    {
      trace_log->get_stream( Log::LOG_DEBUG ) <<
        "xtreemfs::UserDatabase: calling getpwnam_r and getgrnam_r"
        " with user_id=" << user_id << ", group_id=" << group_id << ".";
    }

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
      getpwnam_r( user_id.c_str(), &pwd, pwd_buf, pwd_buf_len, &pwd_res ) == 0
      && 
      pwd_res != NULL
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
      pwd.pw_uid = pwd_res->pw_uid;
      pwd.pw_gid = grp_res->gr_gid;
    }
    else if ( trace_log != NULL )
    {
      trace_log->get_stream( Log::LOG_WARNING ) <<
        "xtreemfs::UserDatabase: getpwnam_r and getgrnam_r with user_id=" <<
        user_id << ", group_id=" << group_id << " failed, errno=" <<
        errno << ", setting user/group to root.";
    }

    delete [] pwd_buf;
    delete [] grp_buf;
  }

  if ( trace_log != NULL )
  {
    trace_log->get_stream( Log::LOG_DEBUG ) << "xtreemfs::UserDatabase: " <<
      user_id << "=" << pwd.pw_uid << ", " <<
      group_id << "=" << pwd.pw_gid <<
      ", storing in cache.";
  }

  if ( group_i == passwd_cache.end() )
  {
    passwd_cache[group_id] = new map<string, struct passwd>;
    group_i = passwd_cache->begin();
  }

  group_i->second->insert( make_pair( user_id, pwd ) );
}


UserCredentials*
UserDatabase::get_user_credentials
(
  uid_t uid,
  gid_t gid
)
{
  if ( trace_log != NULL )
  {
    trace_log->get_stream( Log::LOG_DEBUG ) <<
      "xtreemfs::UserDatabase: getting UserCredentials from passwd (uid=" <<
      uid << ", gid=" << gid << ").";
  }

  LockHolder<Mutex> lock_holder( lock );

  map<gid_t, map<uid_t, UserCredentials*>*>::const_iterator group_i
    = user_credentials_cache.find( gid );

  if ( group_i != user_credentials_cache.end() )
  {
    map<uid_t, UserCredentials*>::const_iterator user_i
      = group_i->second->find( uid );

    if ( user_i != group_i->second->end() )
    {
      UserCredentials* user_credentials = &user_i->second->inc_ref();

      if ( trace_log != NULL )
      {
        trace_log->get_stream( Log::LOG_DEBUG ) <<
          "xtreemfs::UserDatabase: found UserCredentials in cache, " <<
          uid << "=" << user_credentials->get_user_id() << ", " <<
          gid << "=" << user_credentials->get_group_ids()[0] << ".";
      }

      return user_credentials;
    }
  }

  auto_ptr<UserCredentials> user_credentials = new UserCredentials;

  if ( _get_user_credentials != NULL )
  {
    if ( trace_log != NULL )
    {
      trace_log->get_stream( Log::LOG_DEBUG ) <<
        "xtreemfs::UserDatabase: calling get_user_credentials with uid="
        << uid << ", gid=" << gid << ".";
    }

    size_t user_id_len = 0, group_ids_len = 0;
    int get_user_credentials_ret
      = _get_user_credentials
        (
          uid,
          gid,
          NULL,
          &user_id_len,
          NULL,
          &group_ids_len
        );

    if ( get_user_credentials_ret >= 0 )
    {
      if ( trace_log != NULL )
      {
        trace_log->get_stream( Log::LOG_DEBUG ) <<
          "xtreemfs::UserDatabase: calling get_user_credentials " <<
          "with uid=" << uid << ", gid=" << gid << " returned " <<
          get_user_credentials_ret <<
          ", allocating space for UserCredentials.";
      }

      if ( user_id_len > 0 ) // group_ids_len can be 0
      {
        char* user_id = new char[user_id_len];
        char* group_ids = group_ids_len > 0 ? new char[group_ids_len] : NULL;

        get_user_credentials_ret =
          _get_user_credentials
          (
            uid,
            gid,
            user_id,
            &user_id_len,
            group_ids,
            &group_ids_len
          );

        if ( get_user_credentials_ret >= 0 )
        {
          user_credentials->set_user_id( user_id );

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

            user_credentials->set_group_ids( group_ids_ss );
          }
          else
            user_credentials->set_group_ids( StringSet( "" ) );

          if ( trace_log != NULL )
          {
            trace_log->get_stream( Log::LOG_DEBUG ) <<
              "xtreemfs::UserDatabase: get_user_credentials succeeded, " <<
              uid << "=" << user_credentials->get_user_id() << ", " <<
              gid << "=" << user_credentials->get_group_ids()[0] << ".";
          }

          // Drop down to insert the credentials into the cache
        }
        else
          return NULL;
      }
    }
    else
      return NULL;
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
          user_credentials->set_user_id( pwd_res->pw_name );
          delete [] pwd_buf;
        }
        else
        {
          delete [] pwd_buf;
          return NULL;
        }
      }
      else
      {
        delete [] pwd_buf;
        return NULL;
      }
    }

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
          user_credentials->set_group_ids( StringSet( grp_res->gr_name ) );
          delete [] grp_buf;
          // Drop down to insert the credentials into the cache
        }
        else
        {
          delete [] grp_buf;
          return NULL;
        }
      }
      else
      {
        delete [] grp_buf;
        return NULL;
      }
    }
    else
      user_credentials->set_group_ids( StringSet( "" ) );
      // Drop down to insert the credentials into the cache
  }

  if ( group_i != user_credentials_cache.end() )
    group_i->second->insert( make_pair( uid, &user_credentials->inc_ref() ) );
  else
  {
    user_credentials_cache[gid] = new map<uid_t, UserCredentials*>;
    ( *user_credentials_cache[gid] )[uid] = &user_credentials->inc_ref();
  }

  return user_credentials.release();
}

#endif
