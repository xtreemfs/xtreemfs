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


GridSSLSocket::GridSSLSocket
( 
  int domain, 
  yield::platform::socket_t socket_, 
  SSL* ssl,
  SSLContext& ssl_context
)
  : SSLSocket( domain, socket_, ssl, ssl_context )
{
  did_handshake = false;
}

bool GridSSLSocket::check_handshake()
{
  if ( did_handshake )
    return true;
  else
  {
    int SSL_do_handshake_ret = SSL_do_handshake( *this );
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

GridSSLSocket* GridSSLSocket::create( SSLContext& ssl_context )
{
  return create( DOMAIN_DEFAULT, ssl_context );
}

GridSSLSocket* GridSSLSocket::create( int domain, SSLContext& ssl_context )
{
  SSL* ssl = SSL_new( ssl_context );
  if ( ssl != NULL )
  {
    yield::platform::socket_t socket_
      = Socket::create( &domain, TYPE, PROTOCOL );

    if ( socket_ != INVALID_SOCKET )
      return new GridSSLSocket( domain, socket_, ssl, ssl_context );
    else
      return NULL;
  }
  else
    return NULL;
}

ssize_t GridSSLSocket::recv( void* buf, size_t buflen, int flags )
{
  if ( check_handshake() )
    return TCPSocket::recv( buffer, buffer_len, flags );
  else
    return -1;
}

ssize_t GridSSLSocket::send( const void* buf, size_t buflen, int flags )
{
  if ( check_handshake() )
    return TCPSocket::send( buffer, buffer_len, flags );
  else
    return -1;
}

ssize_t GridSSLSocket::sendmsg
( 
  const struct iovec* iov, 
  uint32_t iovlen, 
  int flags 
)
{
  if ( check_handshake() )
    return TCPSocket::sendmsg( iov, iovlen, flags );
  else
    return -1;
}

bool GridSSLSocket::shutdown()
{
  return TCPSocket::shutdown();
}

bool GridSSLSocket::want_read() const
{
  if ( !did_handshake )
    return SSL_get_error( *this, -1 ) == SSL_ERROR_WANT_READ;
  else
    return TCPSocket::want_read();
}

bool GridSSLSocket::want_write() const
{
  if ( !did_handshake )
    return SSL_get_error( *this, -1 ) == SSL_ERROR_WANT_WRITE;
  else
    return TCPSocket::want_write();
}
