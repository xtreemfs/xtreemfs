// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/proxy.h"
#include "org/xtreemfs/client/dir_proxy.h"
#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/client/osd_proxy.h"
using namespace org::xtreemfs::client;


#ifdef _WIN32
#include <windows.h>
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


template <class ProxyType, class InterfaceType>
Proxy<ProxyType, InterfaceType>::Proxy( const YIELD::URI& absolute_uri, YIELD::auto_Log log, uint8_t operation_retries_max, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, YIELD::auto_Object<YIELD::SSLContext> ssl_context )
  : YIELD::ONCRPCClient<InterfaceType>( absolute_uri, log, operation_retries_max, operation_timeout, peer_sockaddr, ssl_context ), log( log )
{
  get_user_credentials_from_passwd = NULL;
  get_passwd_from_user_credentials = NULL;

  std::vector<YIELD::Path> policy_dir_paths;
  policy_dir_paths.push_back( YIELD::Path() );
  policy_dir_paths.push_back( "policies" );
  policy_dir_paths.push_back( "lib" );
#ifndef _WIN32
  policy_dir_paths.push_back( "/lib/xtreemfs/policies/" );
#endif

  YIELD::auto_Volume volume = new YIELD::Volume;
  for ( std::vector<YIELD::Path>::iterator policy_dir_path_i = policy_dir_paths.begin(); policy_dir_path_i != policy_dir_paths.end(); policy_dir_path_i++ )
  {
    log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: scanning " << *policy_dir_path_i << " for policy shared libraries.";
    std::vector<YIELD::Path> file_names;
    volume->listdir( *policy_dir_path_i, file_names );
    for ( std::vector<YIELD::Path>::iterator file_name_i = file_names.begin(); file_name_i != file_names.end(); file_name_i++ )
    {
      const std::string& file_name = static_cast<const std::string&>( *file_name_i );      
      std::string::size_type dll_pos = file_name.find( SHLIBSUFFIX );
      if ( dll_pos != std::string::npos && dll_pos != 0 && file_name[dll_pos-1] == '.' )
      {        
        YIELD::Path policy_shared_library_path = *policy_dir_path_i  + file_name;
        log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: checking " << policy_shared_library_path << " for policy functions.";
        YIELD::auto_Object<YIELD::SharedLibrary> policy_shared_library = YIELD::SharedLibrary::open( policy_shared_library_path );
        if ( policy_shared_library != NULL )
        {
          bool found_policy_function = false;
          found_policy_function |= getPolicyFunction( policy_shared_library_path, policy_shared_library, "get_passwd_from_user_credentials", get_passwd_from_user_credentials );
          found_policy_function |= getPolicyFunction( policy_shared_library_path, policy_shared_library, "get_user_credentials_from_passwd", get_user_credentials_from_passwd );
          if ( found_policy_function )
            policy_shared_libraries.push_back( policy_shared_library.release() );
        }
      }
    }
  }
}

template <class ProxyType, class InterfaceType>
Proxy<ProxyType, InterfaceType>::~Proxy()
{
  for ( std::vector<YIELD::SharedLibrary*>::iterator policy_shared_library_i = policy_shared_libraries.begin(); policy_shared_library_i != policy_shared_libraries.end(); policy_shared_library_i++ )
    YIELD::Object::decRef( **policy_shared_library_i );

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

template <class ProxyType, class InterfaceType>
void Proxy<ProxyType, InterfaceType>::handleEvent( YIELD::Event& ev )
{
  if ( InterfaceType::checkRequest( ev ) != NULL )
  {
    YIELD::auto_Object<org::xtreemfs::interfaces::UserCredentials> user_credentials = new org::xtreemfs::interfaces::UserCredentials;
    getCurrentUserCredentials( *user_credentials.get() );
    YIELD::auto_Object<YIELD::ONCRPCRequest> oncrpc_request = new YIELD::ONCRPCRequest( this->incRef(), 0x20000000 + InterfaceType::get_tag(), ev.get_tag(), InterfaceType::get_tag(), org::xtreemfs::interfaces::ONCRPC_AUTH_FLAVOR, user_credentials.release(), ev );
    YIELD::ONCRPCClient<InterfaceType>::handleEvent( *oncrpc_request.release() );
  }
  else
    YIELD::ONCRPCClient<InterfaceType>::handleEvent( ev );
}

template <class ProxyType, class InterfaceType>
void Proxy<ProxyType, InterfaceType>::getCurrentUserCredentials( org::xtreemfs::interfaces::UserCredentials& out_user_credentials )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: getting current user credentials.";

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
        int username_wcslen = static_cast<int>( wcsnlen( user_info->wkui1_username, UINT16_MAX ) );
        int username_strlen = WideCharToMultiByte( GetACP(), 0, user_info->wkui1_username, username_wcslen, NULL, 0, 0, NULL );
        char* user_id = new char[username_strlen+1];
        WideCharToMultiByte( GetACP(), 0, user_info->wkui1_username, username_wcslen, user_id, username_strlen+1, 0, NULL );
        out_user_credentials.set_user_id( user_id, username_strlen );
        delete [] user_id;

        int logon_domain_wcslen = static_cast<int>( wcsnlen( user_info->wkui1_logon_domain, UINT16_MAX ) );
        int logon_domain_strlen = WideCharToMultiByte( GetACP(), 0, user_info->wkui1_logon_domain, logon_domain_wcslen, NULL, 0, 0, NULL );
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

    throw YIELD::Exception( "could not retrieve user_id and group_id" );
  }
#else
  int caller_uid = yieldfs::FUSE::geteuid();
  if ( caller_uid < 0 ) caller_uid = ::geteuid();
  int caller_gid = yieldfs::FUSE::getegid();
  if ( caller_gid < 0 ) caller_gid = ::getegid();
  getUserCredentialsFrompasswd( caller_uid, caller_gid, out_user_credentials );
#endif
}

template <class ProxyType, class InterfaceType>
void Proxy<ProxyType, InterfaceType>::getpasswdFromUserCredentials( const std::string& user_id, const std::string& group_id, int& out_uid, int& out_gid )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: getting passwd from UserCredentials (user_id=" << user_id << ", group_id=" << group_id << ").";

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
      log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: found user and group IDs in cache, " << user_id << "=" << out_uid << ", " << group_id << "=" << out_gid << ".";
      return;
    }
  }

  bool have_passwd = false;
  if ( get_passwd_from_user_credentials )
  {
    log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: calling get_passwd_from_user_credentials_ret with user_id=" << user_id << ", group_id=" << group_id << ".";
    int get_passwd_from_user_credentials_ret = get_passwd_from_user_credentials( user_id.c_str(), group_id.c_str(), &out_uid, &out_gid );
    if ( get_passwd_from_user_credentials_ret >= 0 )
      have_passwd = true;
    else
      log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: get_passwd_from_user_credentials_ret with user_id=" << user_id << ", group_id=" << group_id << " failed with errno=" << ( get_passwd_from_user_credentials_ret * -1 );
//      throw YIELD::Exception( get_passwd_from_user_credentials_ret * -1 );
  }

  if ( !have_passwd )
  {
#ifdef _WIN32
    YIELD::DebugBreak();
#else
    log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: calling getpwnam_r and getgrnam_r with user_id=" << user_id << ", group_id=" << group_id << ".";

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
      //    throw YIELD::Exception();
      out_uid = 0;
      out_gid = 0;
      log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: getpwnam_r and getgrnam_r with user_id=" << user_id << ", group_id=" << group_id << " failed, errno=" << errno << ", setting user/group to root.";
    }
#endif
  }

  log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: " << user_id << "=" << out_uid << ", " << group_id << "=" << out_gid << ", storing in cache.";

  if ( user_id_to_passwd_cache == NULL )
  {
    user_id_to_passwd_cache = new YIELD::STLHashMap<std::pair<int,int>*>;
    user_credentials_to_passwd_cache.insert( group_id_hash, user_id_to_passwd_cache );
  }

  user_id_to_passwd_cache->insert( user_id_hash, new std::pair<int,int>( out_uid, out_gid ) );
}

template <class ProxyType, class InterfaceType>
void Proxy<ProxyType, InterfaceType>::getUserCredentialsFrompasswd( int uid, int gid, org::xtreemfs::interfaces::UserCredentials& out_user_credentials )
{
  log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: getting UserCredentials from passwd (uid=" << uid << ", gid=" << gid << ").";

  YIELD::STLHashMap<org::xtreemfs::interfaces::UserCredentials*>* uid_to_user_credentials_cache = passwd_to_user_credentials_cache.find( static_cast<uint32_t>( gid ) );
  if ( uid_to_user_credentials_cache != NULL )
  {
    org::xtreemfs::interfaces::UserCredentials* user_credentials = uid_to_user_credentials_cache->find( static_cast<uint32_t>( uid ) );
    if ( user_credentials != NULL )
    {
      out_user_credentials = *user_credentials;
      log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: found UserCredentials in cache, " << uid << "=" << out_user_credentials.get_user_id() << ", " << gid << "=" << out_user_credentials.get_group_ids()[0] << ".";
      return;
    }
  }

  if ( get_user_credentials_from_passwd )
  {
    log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: calling get_user_credentials_from_passwd with uid=" << uid << ", gid=" << gid << ".";

    size_t user_id_len = 0, group_ids_len = 0;
    int get_user_credentials_from_passwd_ret = get_user_credentials_from_passwd( uid, gid, NULL, &user_id_len, NULL, &group_ids_len );
    if ( get_user_credentials_from_passwd_ret >= 0 )
    {
      log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: calling get_user_credentials_from_passwd with uid=" << uid << ", gid=" << gid << " returned " << get_user_credentials_from_passwd_ret << ", allocating space for UserCredentials.";

      if ( user_id_len > 0 && group_ids_len > 0 )
      {
        char* user_id = new char[user_id_len];
        char* group_ids = new char[group_ids_len];

        get_user_credentials_from_passwd_ret = get_user_credentials_from_passwd( uid, gid, user_id, &user_id_len, group_ids, &group_ids_len );
        if ( get_user_credentials_from_passwd_ret >= 0 )
        {
          log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::Proxy: get_user_credentials_from_passwd succeeded, " << uid << "=" << out_user_credentials.get_user_id() << ", " << gid << "=" << out_user_credentials.get_group_ids()[0] << ".";

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
          throw YIELD::Exception( get_user_credentials_from_passwd_ret * -1 );
      }
    }
    else
      throw YIELD::Exception( get_user_credentials_from_passwd_ret * -1 );
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

    if ( getpwuid_r( uid, &pwd, pwd_buf, pwd_buf_len, &pwd_res ) == 0 )
    {
      if ( pwd_res != NULL && pwd_res->pw_name != NULL )
      {
        out_user_credentials.set_user_id( pwd_res->pw_name );

        if ( gid != -1 )
        {
          if ( getgrgid_r( gid, &grp, grp_buf, grp_buf_len, &grp_res ) == 0 )
          {
            if ( grp_res != NULL && grp_res->gr_name != NULL )
              out_user_credentials.set_group_ids( org::xtreemfs::interfaces::StringSet( grp_res->gr_name ) );
            else
              throw YIELD::Exception( "no such gid" );
          }
          else
            throw YIELD::Exception();
        }
        else
          out_user_credentials.set_group_ids( org::xtreemfs::interfaces::StringSet( "" ) );
      }
      else
        throw YIELD::Exception( "no such uid" );
    }
    else
      throw YIELD::Exception();
#endif
  }

  if ( uid_to_user_credentials_cache == NULL )
  {
    uid_to_user_credentials_cache = new YIELD::STLHashMap<org::xtreemfs::interfaces::UserCredentials*>;
    passwd_to_user_credentials_cache.insert( static_cast<uint32_t>( gid ), uid_to_user_credentials_cache );
  }

  uid_to_user_credentials_cache->insert( static_cast<uint32_t>( uid ), new org::xtreemfs::interfaces::UserCredentials( out_user_credentials ) );
}


template class Proxy<DIRProxy, org::xtreemfs::interfaces::DIRInterface>;
template class Proxy<MRCProxy, org::xtreemfs::interfaces::MRCInterface>;
template class Proxy<OSDProxy, org::xtreemfs::interfaces::OSDInterface>;
