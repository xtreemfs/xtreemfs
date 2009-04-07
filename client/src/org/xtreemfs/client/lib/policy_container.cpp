// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "policy_container.h"
using namespace org::xtreemfs::client;

#include "org/xtreemfs/interfaces/exceptions.h"

#ifdef _WIN32
#include "yield/platform/windows.h"
#include <lm.h>
#pragma comment( lib, "Netapi32.lib" )
#else
#include "yieldfs.h"
#include <errno.h>
#include <unistd.h>
#include <pwd.h>
#define PWD_BUF_LEN 256
#include <grp.h>
#define GRP_BUF_LEN 1024
#endif


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class PolicyContainerreaddirCallback : public YIELD::Volume::readdirCallback
      {
      public:
        PolicyContainerreaddirCallback( PolicyContainer& policy_container, const YIELD::Path& root_dir_path )
          : policy_container( policy_container ), root_dir_path( root_dir_path )
        { }

        // YIELD::Volume::readdirCallback
        bool operator()( const YIELD::Path& name, const YIELD::Stat& stbuf )
        {
          std::string::size_type dll_pos = name.get_host_charset_path().find( SHLIBSUFFIX );
          if ( dll_pos != std::string::npos && dll_pos != 0 && name.get_host_charset_path()[dll_pos-1] == '.' )
            policy_container.loadPolicySharedLibrary( root_dir_path + name );
          return true;
        }

      private:
        PolicyContainer& policy_container;
        YIELD::Path root_dir_path;
      };
    };
  };
};


PolicyContainer::PolicyContainer()
{
  this->get_user_credentials_from_passwd = NULL;
  this->get_passwd_from_user_credentials = NULL;

  loadPolicySharedLibraries( "policies" );
  loadPolicySharedLibraries( "lib" );
  loadPolicySharedLibraries( YIELD::Path() );
}

PolicyContainer::~PolicyContainer()
{
  for ( std::vector<YIELD::SharedLibrary*>::iterator policy_shared_library_i = policy_shared_libraries.begin(); policy_shared_library_i != policy_shared_libraries.end(); policy_shared_library_i++ )
    delete *policy_shared_library_i;

  for ( YIELD::STLHashMap<YIELD::STLHashMap<std::pair<int,int>*>*>::iterator i = user_credentials_to_passwd_cache.begin(); i != user_credentials_to_passwd_cache.end(); i++ )
  {
    for ( YIELD::STLHashMap<std::pair<int,int>*>::iterator j = i->second->begin(); j != i->second->end(); j++ )      
      delete j->second;
    delete i->second;
  }

  for ( YIELD::STLHashMap<YIELD::STLHashMap<org::xtreemfs::interfaces::UserCredentials*>*>::iterator i = passwd_to_user_credentials_cache.begin(); i != passwd_to_user_credentials_cache.end(); i++ )
  {
    for ( YIELD::STLHashMap<org::xtreemfs::interfaces::UserCredentials*>::iterator j = i->second->begin(); j != i->second->end(); j++ )
      delete j->second;
    delete i->second;
  }
}

void PolicyContainer::loadPolicySharedLibraries( const YIELD::Path& policy_shared_libraries_dir_path )
{
  PolicyContainerreaddirCallback readdir_callback( *this, policy_shared_libraries_dir_path );
  YIELD::Volume().readdir( policy_shared_libraries_dir_path, readdir_callback );
}

void PolicyContainer::loadPolicySharedLibrary( const YIELD::Path& policy_shared_library_file_path )
{
  YIELD::SharedLibrary* policy_shared_library = YIELD::SharedLibrary::open( policy_shared_library_file_path );
  if ( policy_shared_library )
  {
    get_passwd_from_user_credentials_t get_passwd_from_user_credentials = ( get_passwd_from_user_credentials_t )policy_shared_library->getFunction( "get_passwd_from_user_credentials" );
    if ( get_passwd_from_user_credentials )
      this->get_passwd_from_user_credentials = get_passwd_from_user_credentials;

    get_user_credentials_from_passwd_t get_user_credentials_from_passwd = ( get_user_credentials_from_passwd_t )policy_shared_library->getFunction( "get_user_credentials_from_passwd" );
    if ( get_user_credentials_from_passwd )
      this->get_user_credentials_from_passwd = get_user_credentials_from_passwd;

    policy_shared_libraries.push_back( policy_shared_library );
  }
}

void PolicyContainer::getCurrentUserCredentials( org::xtreemfs::interfaces::UserCredentials& out_user_credentials )
{
#ifdef _WIN32
  if ( get_user_credentials_from_passwd )
    getUserCredentialsFrompasswd( -1, -1, out_user_credentials );
  else
  {
    DWORD dwLevel = 1;
    LPWKSTA_USER_INFO_1 user_info = NULL;
    if ( NetWkstaUserGetInfo( NULL, dwLevel, (LPBYTE *)&user_info ) == NERR_Success )
    {
      if ( user_info !=NULL )
      {
        size_t username_wcslen = wcslen( user_info->wkui1_username );
        size_t username_strlen = WideCharToMultiByte( GetACP(), 0, user_info->wkui1_username, username_wcslen, NULL, 0, 0, NULL );
        char* user_id = new char[username_strlen+1];
        WideCharToMultiByte( GetACP(), 0, user_info->wkui1_username, username_wcslen, user_id, username_strlen+1, 0, NULL );
        out_user_credentials.set_user_id( user_id, username_strlen );
        delete [] user_id;

        size_t logon_domain_wcslen = wcslen( user_info->wkui1_logon_domain );
        size_t logon_domain_strlen = WideCharToMultiByte( GetACP(), 0, user_info->wkui1_logon_domain, logon_domain_wcslen, NULL, 0, 0, NULL );
        char* group_id = new char[logon_domain_strlen+1];
        WideCharToMultiByte( GetACP(), 0, user_info->wkui1_logon_domain, logon_domain_wcslen, group_id, logon_domain_strlen+1, 0, NULL );
        std::string group_id_str( group_id, logon_domain_strlen );
        delete [] group_id;
        org::xtreemfs::interfaces::StringSet group_ids;
        group_ids.push_back( group_id_str );
        out_user_credentials.set_group_ids( group_ids );

        NetApiBufferFree( user_info );

        return;
      }
    }

    throw YIELD::PlatformException( ERROR_ACCESS_DENIED, "could not retrieve user_id and group_id" );
  }
#else
//  int caller_uid = yieldfs::FUSE::geteuid();
//  if ( caller_uid < 0 ) caller_uid = ::geteuid();
//  int caller_gid = yieldfs::FUSE::getegid();
 // if ( caller_gid < 0 ) caller_gid = ::getegid();
  int caller_uid = ::geteuid();
  int caller_gid = ::getegid();
  getUserCredentialsFrompasswd( caller_uid, caller_gid, out_user_credentials );
#endif
}

void PolicyContainer::getpasswdFromUserCredentials( const std::string& user_id, const std::string& group_id, int& out_uid, int& out_gid )
{
  out_uid = 0;
  out_gid = 0;

  uint32_t user_id_hash = YIELD::string_hash( user_id.c_str() );
  uint32_t group_id_hash = YIELD::string_hash( group_id.c_str() );

  YIELD::STLHashMap<std::pair<int, int>*>* user_id_to_passwd_cache = user_credentials_to_passwd_cache.find( group_id_hash );
  if ( user_id_to_passwd_cache != NULL )
  {
    std::pair<int,int>* passwd = user_id_to_passwd_cache->find( user_id_hash );
    if ( passwd != NULL )
    {
      out_uid = passwd->first;
      out_gid = passwd->second;
      return;
    }
  }

  if ( get_passwd_from_user_credentials )
  {
    int get_passwd_from_user_credentials_ret = get_passwd_from_user_credentials( user_id.c_str(), group_id.c_str(), &out_uid, &out_gid );
    if ( get_passwd_from_user_credentials_ret < 0 )      
      throw YIELD::PlatformException( get_passwd_from_user_credentials_ret * -1 );
  }
  else
  {
#ifdef _WIN32
    YIELD::DebugBreak();
#else
    struct passwd pwd, *pwd_res;
    char pwd_buf[PWD_BUF_LEN]; int pwd_buf_len = sizeof( pwd_buf );
    struct group grp, *grp_res;
    char grp_buf[GRP_BUF_LEN]; int grp_buf_len = sizeof( grp_buf );

    if ( getpwnam_r( user_id.c_str(), &pwd, pwd_buf, pwd_buf_len, &pwd_res ) == 0 && pwd_res != NULL &&
         getgrnam_r( group_id.c_str(), &grp, grp_buf, grp_buf_len, &grp_res ) == 0 && grp_res != NULL )
    {
      out_uid = pwd_res->pw_uid;
      out_gid = grp_res->gr_gid;
    }
    else
    {
      //    throw YIELD::PlatformException();
      out_uid = 0;
      out_gid = 0;
    }
#endif
  }

  if ( user_id_to_passwd_cache == NULL )
  {
    user_id_to_passwd_cache = new YIELD::STLHashMap<std::pair<int,int>*>;
    user_credentials_to_passwd_cache.insert( group_id_hash, user_id_to_passwd_cache );
  }

  user_id_to_passwd_cache->insert( user_id_hash, new std::pair<int,int>( out_uid, out_gid ) );
}

void PolicyContainer::getUserCredentialsFrompasswd( int uid, int gid, org::xtreemfs::interfaces::UserCredentials& out_user_credentials )
{
  YIELD::STLHashMap<org::xtreemfs::interfaces::UserCredentials*>* uid_to_user_credentials_cache = passwd_to_user_credentials_cache.find( static_cast<uint32_t>( gid ) );
  if ( uid_to_user_credentials_cache != NULL )
  {
    org::xtreemfs::interfaces::UserCredentials* user_credentials = uid_to_user_credentials_cache->find( static_cast<uint32_t>( uid ) );
    if ( user_credentials != NULL )
    {
      out_user_credentials = *user_credentials;
      return;
    }
  }

  if ( get_user_credentials_from_passwd )
  {
    size_t user_id_len = 0, group_ids_len = 0;
    int get_user_credentials_from_passwd_ret = get_user_credentials_from_passwd( uid, gid, NULL, &user_id_len, NULL, &group_ids_len );
    if ( get_user_credentials_from_passwd_ret >= 0 )
    {
      if ( user_id_len > 0 && group_ids_len > 0 )
      {
        char* user_id = new char[user_id_len];
        char* group_ids = new char[group_ids_len];

        get_user_credentials_from_passwd_ret = get_user_credentials_from_passwd( uid, gid, user_id, &user_id_len, group_ids, &group_ids_len );
        if ( get_user_credentials_from_passwd_ret >= 0 )
        {
          out_user_credentials.set_user_id( user_id );

          char* group_ids_p = group_ids;
          org::xtreemfs::interfaces::StringSet group_ids_ss;
          while ( static_cast<size_t>( group_ids_p - group_ids ) < group_ids_len )
          {
            group_ids_ss.push_back( group_ids_p );
            group_ids_p += group_ids_ss.back().size() + 1;
          }
          out_user_credentials.set_group_ids( group_ids_ss );
        }
        else
          throw YIELD::PlatformException( get_user_credentials_from_passwd_ret * -1 );
      }
    }
    else
      throw YIELD::PlatformException( get_user_credentials_from_passwd_ret * -1 );
  }
  else
  {
#ifdef _WIN32
    YIELD::DebugBreak();
#else
    struct passwd pwd, *pwd_res;
    char pwd_buf[PWD_BUF_LEN]; int pwd_buf_len = sizeof( pwd_buf );
    struct group grp, *grp_res;
    char grp_buf[GRP_BUF_LEN]; int grp_buf_len = sizeof( grp_buf );

    if ( getpwuid_r( uid, &pwd, pwd_buf, pwd_buf_len, &pwd_res ) == 0 && pwd_res != NULL && pwd_res->pw_name != NULL &&
         getgrgid_r( gid, &grp, grp_buf, grp_buf_len, &grp_res ) == 0 && grp_res != NULL && grp_res->gr_name != NULL )
    {
      out_user_credentials.set_user_id( pwd_res->pw_name );
      out_user_credentials.set_group_ids( org::xtreemfs::interfaces::StringSet( grp_res->gr_name ) );
    }
    else
      throw YIELD::PlatformException();
#endif
  }

  if ( uid_to_user_credentials_cache == NULL )
  {
    uid_to_user_credentials_cache = new YIELD::STLHashMap<org::xtreemfs::interfaces::UserCredentials*>;
    passwd_to_user_credentials_cache.insert( static_cast<uint32_t>( gid ), uid_to_user_credentials_cache );
  }

  uid_to_user_credentials_cache->insert( static_cast<uint32_t>( uid ), new org::xtreemfs::interfaces::UserCredentials( out_user_credentials ) );
}

