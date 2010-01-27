// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/proxy.h"
#include "xtreemfs/dir_proxy.h"
#include "xtreemfs/mrc_proxy.h"
#include "xtreemfs/osd_proxy.h"
using namespace org::xtreemfs::interfaces;
using namespace xtreemfs;


#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#include <mswsock.h>
#pragma warning( pop )
#include <windows.h>
#include <lm.h>
#pragma comment( lib, "Netapi32.lib" )
#else
#include "yieldfs.h"
#include <errno.h>
#include <netinet/in.h>
#include <netinet/tcp.h> 
#include <sys/socket.h>
#include <unistd.h>
#endif


namespace xtreemfs
{
  class GridSSLSocket : public YIELD::ipc::SSLSocket
  {
  public:
    static yidl::runtime::auto_Object<GridSSLSocket> 
    create( YIELD::ipc::auto_SSLContext ctx )
    {
      return create( AF_INET6, ctx );
    }

    static yidl::runtime::auto_Object<GridSSLSocket> 
    create( int domain, YIELD::ipc::auto_SSLContext ctx ) 
    {
      SSL* ssl = SSL_new( ctx->get_ssl_ctx() );
      if ( ssl != NULL )
      {
    #ifdef _WIN32
        SOCKET socket_ 
          = YIELD::ipc::Socket::create( domain, SOCK_STREAM, IPPROTO_TCP );
        if ( socket_ != INVALID_SOCKET )
    #else
        int socket_ 
          = YIELD::ipc::Socket::create( domain, SOCK_STREAM, IPPROTO_TCP );
        if ( socket_ != -1 )
    #endif
          return new GridSSLSocket( domain, socket_, ctx, ssl );
        else
          return NULL;
      }
      else
        return NULL;
    }

    // YIELD::ipc::Socket
    ssize_t read( void* buffer, size_t buffer_len )
    {
      if ( check_handshake() )
        return YIELD::ipc::TCPSocket::read( buffer, buffer_len );             
      else
        return -1;
    }

    bool shutdown()
    {
      return YIELD::ipc::TCPSocket::shutdown();
    }

    bool want_read() const
    {
      if ( !did_handshake )
        return SSL_get_error( ssl, -1 ) == SSL_ERROR_WANT_READ;
      else
        return YIELD::ipc::TCPSocket::want_read();
    }

    bool want_write() const
    {
      if ( !did_handshake )
        return SSL_get_error( ssl, -1 ) == SSL_ERROR_WANT_WRITE;
      else
        return YIELD::ipc::TCPSocket::want_write();
    }

    ssize_t write( const void* buffer, size_t buffer_len )
    {
      if ( check_handshake() )
        return YIELD::ipc::TCPSocket::write( buffer, buffer_len );
      else
        return -1;
    }

    ssize_t writev( const struct iovec* buffers, uint32_t buffers_count )
    {
      if ( check_handshake() )
        return YIELD::ipc::TCPSocket::writev( buffers, buffers_count );
      else
        return -1;
    }

  private:
    GridSSLSocket
    ( 
      int domain,
#ifdef _WIN32
      SOCKET socket_,
#else
      int socket_,
#endif
      YIELD::ipc::auto_SSLContext ctx, 
      SSL* ssl
    )
      : YIELD::ipc::SSLSocket( domain, socket_, ctx, ssl )
    {
      did_handshake = false;
    }

    bool did_handshake;

    inline bool check_handshake()
    {
      if ( did_handshake )
        return true;
      else
      {
        int SSL_do_handshake_ret = SSL_do_handshake( ssl );
        if ( SSL_do_handshake_ret == 1 )
        {
          did_handshake = true;
          return true;
        }
        else if ( SSL_do_handshake_ret == 0 )
          return false;
        else
          return false;
      }
    }
  };


  class GridSSLSocketFactory : public YIELD::ipc::SocketFactory
  {
  public:
    GridSSLSocketFactory( YIELD::ipc::auto_SSLContext ssl_context )
      : ssl_context( ssl_context )
    { }

    // yidl::runtime::Object
    YIDL_RUNTIME_OBJECT_PROTOTYPES( GridSSLSocketFactory, 0 );

    // YIELD::ipc::SocketFactory
    YIELD::ipc::auto_Socket createSocket()
    {
      return GridSSLSocket::create( ssl_context ).release();
    }

  private:
    YIELD::ipc::auto_SSLContext ssl_context;
  };
};


template <class ProxyType, class InterfaceType>
Proxy<ProxyType, InterfaceType>::Proxy
( 
  uint16_t concurrency_level,
  uint32_t flags, 
  YIELD::platform::auto_Log log, 
  const YIELD::platform::Time& operation_timeout, 
  YIELD::ipc::auto_SocketAddress peername, 
  uint8_t reconnect_tries_max,
  YIELD::ipc::auto_SocketFactory socket_factory,
  auto_UserCredentialsCache user_credentials_cache
)
  : YIELD::ipc::ONCRPCClient<InterfaceType>
    ( 
      concurrency_level, 
      flags, 
      log, 
      operation_timeout, 
      peername, 
      reconnect_tries_max, 
      socket_factory
    ), 
    log( log ),
    user_credentials_cache( user_credentials_cache )
{ }

template <class ProxyType, class InterfaceType>
YIELD::ipc::auto_SocketFactory 
Proxy<ProxyType, InterfaceType>::createSocketFactory
( 
  const YIELD::ipc::URI& absolute_uri, 
  YIELD::ipc::auto_SSLContext ssl_context 
)
{  
  if 
  ( 
    absolute_uri.get_scheme() == ONCRPCG_SCHEME && 
    ssl_context != NULL 
  )
    return new GridSSLSocketFactory( ssl_context );

  else if 
  ( 
    absolute_uri.get_scheme() == ONCRPCS_SCHEME && 
    ssl_context != NULL 
  )      
    return new YIELD::ipc::SSLSocketFactory( ssl_context );

  else if 
  ( 
    absolute_uri.get_scheme() == ONCRPCU_SCHEME 
  )
    return new YIELD::ipc::UDPSocketFactory;

  else
    return new YIELD::ipc::TCPSocketFactory;
}

template <class ProxyType, class InterfaceType>
void 
Proxy<ProxyType, InterfaceType>::getCurrentUserCredentials
( 
  UserCredentials& out_user_credentials 
)
{
#ifdef _DEBUG
  if 
  ( 
    ( this->get_flags() & PROXY_FLAG_TRACE_AUTH ) == 
    PROXY_FLAG_TRACE_AUTH && log != NULL 
  )  
    log->getStream( YIELD::platform::Log::LOG_DEBUG ) << 
      "xtreemfs::Proxy: getting current user credentials.";
#endif

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
  )
  {
    if ( user_info !=NULL )
    {
      int username_wcslen = 
        static_cast<int>( wcsnlen( user_info->wkui1_username, UINT16_MAX ) );
      int username_strlen = 
        WideCharToMultiByte
        ( 
          GetACP(), 
          0,
          user_info->wkui1_username, 
          username_wcslen, 
          NULL, 
          0, 
          0, 
          NULL 
        );

      char* user_id = new char[username_strlen+1];
      WideCharToMultiByte
      ( 
        GetACP(), 
        0, 
        user_info->wkui1_username, 
        username_wcslen, 
        user_id, 
        username_strlen+1, 
        0, 
        NULL 
      );

      out_user_credentials.set_user_id( user_id, username_strlen );
      delete [] user_id;

      int logon_domain_wcslen = 
        static_cast<int>( wcsnlen( user_info->wkui1_logon_domain, UINT16_MAX ) );
      int logon_domain_strlen = 
        WideCharToMultiByte
        ( 
          GetACP(), 
          0, 
          user_info->wkui1_logon_domain, 
          logon_domain_wcslen, 
          NULL, 
          0, 
          0, 
          NULL 
        );

      char* group_id = new char[logon_domain_strlen+1];
      WideCharToMultiByte
      ( 
        GetACP(), 
        0, 
        user_info->wkui1_logon_domain, 
        logon_domain_wcslen, 
        group_id, 
        logon_domain_strlen+1, 
        0, 
        NULL 
      );
      std::string group_id_str( group_id, logon_domain_strlen );
      delete [] group_id;
      StringSet group_ids;
      group_ids.push_back( group_id_str );
      out_user_credentials.set_group_ids( group_ids );

      NetApiBufferFree( user_info );

      return;
    }

    throw 
      YIELD::platform::Exception( "could not retrieve user_id and group_id" );
  }
#else
  uid_t caller_uid = yieldfs::FUSE::geteuid();
  gid_t caller_gid = yieldfs::FUSE::getegid();

  if 
  ( 
    caller_uid != static_cast<uid_t>( -1 ) && 
    caller_gid != static_cast<gid_t>( -1 ) &&
    user_credentials_cache->getUserCredentialsFrompasswd
    ( 
      caller_uid, 
      caller_gid, 
      out_user_credentials 
    ) 
  )
     return;

  else
  {
    caller_uid = ::geteuid();
    caller_gid = ::getegid();

    if 
    ( 
      user_credentials_cache->getUserCredentialsFrompasswd
      ( 
        caller_uid, 
        caller_gid, 
        out_user_credentials 
      )  
    )
      return;
    else
      throw YIELD::platform::Exception();
  }
#endif
}

template <class ProxyType, class InterfaceType>
void Proxy<ProxyType, InterfaceType>::send( YIELD::concurrency::Event& ev )
{
  if ( InterfaceType::checkRequest( ev ) != NULL )
  {
    yidl::runtime::auto_Object<UserCredentials> 
      user_credentials = new UserCredentials;
    getCurrentUserCredentials( *user_credentials.get() );

    yidl::runtime::auto_Object<YIELD::ipc::ONCRPCRequest> oncrpc_request = 
        new YIELD::ipc::ONCRPCRequest
        ( 
          this->incRef(), 
          ONCRPC_AUTH_FLAVOR, 
          user_credentials.release(), 
          ev 
        );
    YIELD::ipc::ONCRPCClient<InterfaceType>::send( *oncrpc_request.release() );
  }
  else
    YIELD::ipc::ONCRPCClient<InterfaceType>::send( ev );
}

template class Proxy<DIRProxy, DIRInterface>;
template class Proxy<MRCProxy, MRCInterface>;
template class Proxy<OSDProxy, OSDInterface>;
