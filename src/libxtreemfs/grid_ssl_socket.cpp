#include "xtreemfs/grid_ssl_socket.h"
using namespace xtreemfs;

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#include <mswsock.h>
#pragma warning( pop )
#else
#include <errno.h>
#include <netinet/in.h>
#include <netinet/tcp.h> 
#include <sys/socket.h>
#include <unistd.h>
#endif


GridSSLSocket::GridSSLSocket
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

bool GridSSLSocket::check_handshake()
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

yidl::runtime::auto_Object<GridSSLSocket> 
GridSSLSocket::create
( 
  YIELD::ipc::auto_SSLContext ctx 
)
{
  return create( AF_INET6, ctx );
}

yidl::runtime::auto_Object<GridSSLSocket> 
GridSSLSocket::create
( 
  int domain, 
  YIELD::ipc::auto_SSLContext ctx 
) 
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

ssize_t GridSSLSocket::read( void* buffer, size_t buffer_len )
{
  if ( check_handshake() )
    return YIELD::ipc::TCPSocket::read( buffer, buffer_len );             
  else
    return -1;
}

bool GridSSLSocket::shutdown()
{
  return YIELD::ipc::TCPSocket::shutdown();
}

bool GridSSLSocket::want_read() const
{
  if ( !did_handshake )
    return SSL_get_error( ssl, -1 ) == SSL_ERROR_WANT_READ;
  else
    return YIELD::ipc::TCPSocket::want_read();
}

bool GridSSLSocket::want_write() const
{
  if ( !did_handshake )
    return SSL_get_error( ssl, -1 ) == SSL_ERROR_WANT_WRITE;
  else
    return YIELD::ipc::TCPSocket::want_write();
}

ssize_t GridSSLSocket::write( const void* buffer, size_t buffer_len )
{
  if ( check_handshake() )
    return YIELD::ipc::TCPSocket::write( buffer, buffer_len );
  else
    return -1;
}

ssize_t GridSSLSocket::writev( const struct iovec* buffers, uint32_t buffers_count )
{
  if ( check_handshake() )
    return YIELD::ipc::TCPSocket::writev( buffers, buffers_count );
  else
    return -1;
}
