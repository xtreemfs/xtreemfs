// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/proxy.h"
#include "xtreemfs/dir_proxy.h"
#include "xtreemfs/mrc_proxy.h"
#include "xtreemfs/osd_proxy.h"
#include "policy_container.h"
using namespace xtreemfs;


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
Proxy<ProxyType, InterfaceType>::Proxy( const YIELD::ipc::URI& absolute_uri, uint32_t flags, YIELD::platform::auto_Log log, const YIELD::platform::Time& operation_timeout, YIELD::ipc::Socket::auto_Address peer_sockaddr, YIELD::ipc::auto_SSLContext ssl_context )
  : YIELD::ipc::ONCRPCClient<InterfaceType>( absolute_uri, flags, log, operation_timeout, peer_sockaddr, ssl_context ), log( log )
{
#ifndef _WIN32
  policy_container = new PolicyContainer;
  get_user_credentials_from_passwd = ( get_user_credentials_from_passwd_t )policy_container->getPolicyFunction( "get_user_credentials_from_passwd" );
  get_passwd_from_user_credentials = ( get_passwd_from_user_credentials_t )policy_container->getPolicyFunction( "get_passwd_from_user_credentials" );
#endif
}

template <class ProxyType, class InterfaceType>
Proxy<ProxyType, InterfaceType>::~Proxy()
{
#ifndef _WIN32
  delete policy_container;

  for ( std::map<std::string,std::map<std::string,std::pair<int,int>*>*>::iterator i = user_credentials_to_passwd_cache.begin(); i != user_credentials_to_passwd_cache.end(); i++ )
  {
    for ( std::map<std::string,std::pair<int,int>*>::iterator j = i->second->begin(); j != i->second->end(); j++ )
      delete j->second;
    delete i->second;
  }

  for ( std::map<int,std::map<int,org::xtreemfs::interfaces::UserCredentials*>*>::iterator i = passwd_to_user_credentials_cache.begin(); i != passwd_to_user_credentials_cache.end(); i++ )
  {
    for ( std::map<int,org::xtreemfs::interfaces::UserCredentials*>::iterator j = i->second->begin(); j != i->second->end(); j++ )
      delete j->second;
    delete i->second;
  }
#endif
}

template <class ProxyType, class InterfaceType>
void Proxy<ProxyType, InterfaceType>::send( YIELD::concurrency::Event& ev )
{
  if ( InterfaceType::checkRequest( ev ) != NULL )
  {
    yidl::runtime::auto_Object<org::xtreemfs::interfaces::UserCredentials> user_credentials = new org::xtreemfs::interfaces::UserCredentials;
    getCurrentUserCredentials( *user_credentials.get() );
    yidl::runtime::auto_Object<YIELD::ipc::ONCRPCRequest> oncrpc_request = new YIELD::ipc::ONCRPCRequest( this->incRef(), org::xtreemfs::interfaces::ONCRPC_AUTH_FLAVOR, user_credentials.release(), ev );
    YIELD::ipc::ONCRPCClient<InterfaceType>::send( *oncrpc_request.release() );
  }
  else
    YIELD::ipc::ONCRPCClient<InterfaceType>::send( ev );
}

template <class ProxyType, class InterfaceType>
void Proxy<ProxyType, InterfaceType>::getCurrentUserCredentials( org::xtreemfs::interfaces::UserCredentials& out_user_credentials )
{
#ifdef _DEBUG
  if ( ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH && log != NULL )  
    log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "xtreemfs::Proxy: getting current user credentials.";
#endif

#ifdef _WIN32
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

    throw YIELD::platform::Exception( "could not retrieve user_id and group_id" );
  }
#else
  uid_t caller_uid = yieldfs::FUSE::geteuid();
  gid_t caller_gid = yieldfs::FUSE::getegid();
  if ( caller_uid != static_cast<uid_t>( -1 ) && caller_gid != static_cast<gid_t>( -1 ) &&
       getUserCredentialsFrompasswd( caller_uid, caller_gid, out_user_credentials ) )
     return;
  else
  {
    caller_uid = ::geteuid();
    caller_gid = ::getegid();
    if ( getUserCredentialsFrompasswd( caller_uid, caller_gid, out_user_credentials )  )
      return;
    else
      throw YIELD::platform::Exception();
  }
#endif
}

#ifndef _WIN32
template <class ProxyType, class InterfaceType>
void Proxy<ProxyType, InterfaceType>::getpasswdFromUserCredentials( const std::string& user_id, const std::string& group_id, int& out_uid, int& out_gid )
{
#ifdef _DEBUG
  if ( ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH && log != NULL )
    log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "xtreemfs::Proxy: getting passwd from UserCredentials (user_id=" << user_id << ", group_id=" << group_id << ").";
#endif

  std::map<std::string,std::map<std::string,std::pair<int, int>*>*>::iterator group_i = user_credentials_to_passwd_cache.find( group_id );
  if ( group_i != user_credentials_to_passwd_cache.end() )
  {
    std::map<std::string,std::pair<int,int>*>::iterator user_i = group_i->second->find( user_id );
    if ( user_i != group_i->second->end() )
    {
      out_uid = user_i->second->first;
      out_gid = user_i->second->second;
#ifdef _DEBUG
      if ( ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH && log != NULL )
        log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "xtreemfs::Proxy: found user and group IDs in cache, " << user_id << "=" << out_uid << ", " << group_id << "=" << out_gid << ".";
#endif
      return;
    }
  }

  bool have_passwd = false;
  if ( get_passwd_from_user_credentials )
  {
#ifdef _DEBUG
    if ( ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH && log != NULL )
      log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "xtreemfs::Proxy: calling get_passwd_from_user_credentials_ret with user_id=" << user_id << ", group_id=" << group_id << ".";
#endif
    int get_passwd_from_user_credentials_ret = get_passwd_from_user_credentials( user_id.c_str(), group_id.c_str(), &out_uid, &out_gid );
    if ( get_passwd_from_user_credentials_ret >= 0 )
      have_passwd = true;
    else if ( ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH && log != NULL )
      log->getStream( YIELD::platform::Log::LOG_ERR ) << "xtreemfs::Proxy: get_passwd_from_user_credentials_ret with user_id=" << user_id << ", group_id=" << group_id << " failed with errno=" << ( get_passwd_from_user_credentials_ret * -1 );
  }

  if ( !have_passwd )
  {
#ifdef _DEBUG
    if ( ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH && log != NULL )
      log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "xtreemfs::Proxy: calling getpwnam_r and getgrnam_r with user_id=" << user_id << ", group_id=" << group_id << ".";
#endif

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
      out_uid = 0;
      out_gid = 0;
      if ( log != NULL )
        log->getStream( YIELD::platform::Log::LOG_WARNING ) << "xtreemfs::Proxy: getpwnam_r and getgrnam_r with user_id=" << user_id << ", group_id=" << group_id << " failed, errno=" << errno << ", setting user/group to root.";
    }
  }

#ifdef _DEBUG
  if ( ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH && log != NULL )
    log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "xtreemfs::Proxy: " << user_id << "=" << out_uid << ", " << group_id << "=" << out_gid << ", storing in cache.";
#endif

  if ( group_i != user_credentials_to_passwd_cache.end() )
    group_i->second->insert( std::make_pair( user_id, new std::pair<int,int>( out_uid, out_gid ) ) );
  else
  {
    user_credentials_to_passwd_cache[group_id] = new std::map<std::string,std::pair<int,int>*>;
    user_credentials_to_passwd_cache[group_id]->insert( std::make_pair( user_id, new std::pair<int,int>( out_uid, out_gid ) ) );
  }
}

template <class ProxyType, class InterfaceType>
bool Proxy<ProxyType, InterfaceType>::getUserCredentialsFrompasswd( int uid, int gid, org::xtreemfs::interfaces::UserCredentials& out_user_credentials )
{
#ifdef _DEBUG
  if ( ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH && log != NULL )
    log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "xtreemfs::Proxy: getting UserCredentials from passwd (uid=" << uid << ", gid=" << gid << ").";
#endif

  std::map<int,std::map<int,org::xtreemfs::interfaces::UserCredentials*>*>::iterator group_i = passwd_to_user_credentials_cache.find( gid );
  if ( group_i != passwd_to_user_credentials_cache.end() )
  {
    std::map<int,org::xtreemfs::interfaces::UserCredentials*>::iterator user_i = group_i->second->find( uid );
    if ( user_i != group_i->second->end() )
    {
      out_user_credentials = *user_i->second;
#ifdef _DEBUG
      if ( ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH && log != NULL )
        log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "xtreemfs::Proxy: found UserCredentials in cache, " << uid << "=" << out_user_credentials.get_user_id() << ", " << gid << "=" << out_user_credentials.get_group_ids()[0] << ".";

#endif
      return true;
    }
  }

  if ( get_user_credentials_from_passwd )
  {
#ifdef _DEBUG
    if ( ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH && log != NULL )
      log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "xtreemfs::Proxy: calling get_user_credentials_from_passwd with uid=" << uid << ", gid=" << gid << ".";
#endif

    size_t user_id_len = 0, group_ids_len = 0;
    int get_user_credentials_from_passwd_ret = get_user_credentials_from_passwd( uid, gid, NULL, &user_id_len, NULL, &group_ids_len );
    if ( get_user_credentials_from_passwd_ret >= 0 )
    {
#ifdef _DEBUG
      if ( ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH && log != NULL )
        log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "xtreemfs::Proxy: calling get_user_credentials_from_passwd with uid=" << uid << ", gid=" << gid << " returned " << get_user_credentials_from_passwd_ret << ", allocating space for UserCredentials.";
#endif

      if ( user_id_len > 0 ) // group_ids_len can be 0
      {
        char* user_id = new char[user_id_len];
        char* group_ids = group_ids_len > 0 ? new char[group_ids_len] : NULL;

        get_user_credentials_from_passwd_ret = get_user_credentials_from_passwd( uid, gid, user_id, &user_id_len, group_ids, &group_ids_len );
        if ( get_user_credentials_from_passwd_ret >= 0 )
        {
          out_user_credentials.set_user_id( user_id );

          if ( group_ids_len > 0 )
          {
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
            out_user_credentials.set_group_ids( org::xtreemfs::interfaces::StringSet( "" ) );

#ifdef _DEBUG
          if ( ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == PROXY_FLAG_TRACE_AUTH && log != NULL )
            log->getStream( YIELD::platform::Log::LOG_DEBUG ) << "xtreemfs::Proxy: get_user_credentials_from_passwd succeeded, " << uid << "=" << out_user_credentials.get_user_id() << ", " << gid << "=" << out_user_credentials.get_group_ids()[0] << ".";
#endif

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
    struct passwd pwd, *pwd_res;
    char pwd_buf[PWD_BUF_LEN]; int pwd_buf_len = sizeof( pwd_buf );
    struct group grp, *grp_res;
    char grp_buf[GRP_BUF_LEN]; int grp_buf_len = sizeof( grp_buf );

    if ( uid != -1 )
    {
      if ( getpwuid_r( uid, &pwd, pwd_buf, pwd_buf_len, &pwd_res ) == 0 )
      {
        if ( pwd_res != NULL && pwd_res->pw_name != NULL )
        {
          out_user_credentials.set_user_id( pwd_res->pw_name );
        } 
        else
          return false;
      } 
      else
        return false;
    } 
    else
      out_user_credentials.set_user_id( "" );

    if ( gid != -1 )
    {
      if ( getgrgid_r( gid, &grp, grp_buf, grp_buf_len, &grp_res ) == 0 )
      {
        if ( grp_res != NULL && grp_res->gr_name != NULL )
          out_user_credentials.set_group_ids( org::xtreemfs::interfaces::StringSet( grp_res->gr_name ) );
        // Drop down to insert the credentials into the cache
        else
          return false;
      } 
      else
        return false;
    } 
    else
      out_user_credentials.set_group_ids( org::xtreemfs::interfaces::StringSet( "" ) );
      // Drop down to insert the credentials into the cache
  }
  
  if ( group_i != passwd_to_user_credentials_cache.end() )
    group_i->second->insert( std::make_pair( uid, new org::xtreemfs::interfaces::UserCredentials( out_user_credentials ) ) );
  else
  {
    passwd_to_user_credentials_cache[gid] = new std::map<int,org::xtreemfs::interfaces::UserCredentials*>;
    passwd_to_user_credentials_cache[gid]->insert( std::make_pair( uid, new org::xtreemfs::interfaces::UserCredentials( out_user_credentials ) ) );
  }

  return true;
}
#endif

template class Proxy<DIRProxy, org::xtreemfs::interfaces::DIRInterface>;
template class Proxy<MRCProxy, org::xtreemfs::interfaces::MRCInterface>;
template class Proxy<OSDProxy, org::xtreemfs::interfaces::OSDInterface>;
