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


#include "xtreemfs/grid_ssl_socket.h"
using namespace xtreemfs;

#ifdef _WIN32
#undef INVALID_SOCKET
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#include <mswsock.h>
#pragma warning( pop )
#define INVALID_SOCKET  (SOCKET)(~0)
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
    YIELD::platform::socket_t socket_
      = YIELD::platform::Socket::create( &domain, SOCK_STREAM, IPPROTO_TCP );

    if ( socket_ != INVALID_SOCKET )
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
    return YIELD::platform::TCPSocket::read( buffer, buffer_len );
  else
    return -1;
}

bool GridSSLSocket::shutdown()
{
  return YIELD::platform::TCPSocket::shutdown();
}

bool GridSSLSocket::want_read() const
{
  if ( !did_handshake )
    return SSL_get_error( ssl, -1 ) == SSL_ERROR_WANT_READ;
  else
    return YIELD::platform::TCPSocket::want_read();
}

bool GridSSLSocket::want_write() const
{
  if ( !did_handshake )
    return SSL_get_error( ssl, -1 ) == SSL_ERROR_WANT_WRITE;
  else
    return YIELD::platform::TCPSocket::want_write();
}

ssize_t GridSSLSocket::write( const void* buffer, size_t buffer_len )
{
  if ( check_handshake() )
    return YIELD::platform::TCPSocket::write( buffer, buffer_len );
  else
    return -1;
}

ssize_t GridSSLSocket::writev( const struct iovec* buffers, uint32_t buffers_count )
{
  if ( check_handshake() )
    return YIELD::platform::TCPSocket::writev( buffers, buffers_count );
  else
    return -1;
}
