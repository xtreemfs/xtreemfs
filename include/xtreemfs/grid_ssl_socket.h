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


#ifndef _XTREEMFS_GRID_SSL_SOCKET_H_
#define _XTREEMFS_GRID_SSL_SOCKET_H_

#include "yield.h"


namespace xtreemfs
{
  class GridSSLSocket : public YIELD::ipc::SSLSocket
  {
  public:
    static yidl::runtime::auto_Object<GridSSLSocket>
    create
    (
      YIELD::ipc::auto_SSLContext
    );

    static yidl::runtime::auto_Object<GridSSLSocket>
    create
    (
      int domain,
      YIELD::ipc::auto_SSLContext
    );

    // YIELD::ipc::Socket
    ssize_t read( void* buffer, size_t buffer_len );
    bool shutdown();
    bool want_read() const;
    bool want_write() const;
    ssize_t write( const void* buffer, size_t buffer_len );
    ssize_t writev( const struct iovec* buffers, uint32_t buffers_count );

  private:
    GridSSLSocket
    (
      int domain,
#if defined(_WIN64)
      uint64_t socket_,
#elif defined(_WIN32)
      uint32_t socket_,
#else
      int socket_,
#endif
      YIELD::ipc::auto_SSLContext,
      SSL* ssl
    );

    bool did_handshake;
    bool check_handshake();
  };


  class GridSSLSocketFactory : public YIELD::ipc::SocketFactory
  {
  public:
    GridSSLSocketFactory( YIELD::ipc::auto_SSLContext ssl_context )
      : ssl_context( ssl_context )
    { }

    // YIELD::ipc::SocketFactory
    YIELD::platform::auto_Socket createSocket()
    {
      return GridSSLSocket::create( ssl_context ).release();
    }

  private:
    YIELD::ipc::auto_SSLContext ssl_context;
  };
};

#endif
