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
  using yield::ipc::SSLContext;
  using yield::ipc::SSLSocket;


  class GridSSLSocket : public SSLSocket
  {
  public:
    virtual ~GridSSLSocket() { }

    static GridSSLSocket* create( SSLContext& ssl_context );
    static GridSSLSocket* create( int domain, SSLContext& ssl_context );

    // yield::ipc::Socket
    ssize_t recv( void* buf, size_t buflen, int );
    ssize_t send( const void* buf, size_t buflen, int );
    ssize_t sendmsg( const struct iovec* buffers, uint32_t buffers_count, int );
    bool shutdown();
    bool want_recv() const;
    bool want_send() const;

  private:
    GridSSLSocket( int domain, yield::platform::socket_t, SSL*, SSLContext& );

  private:
    bool did_handshake;
    bool check_handshake();
  };


  class GridSSLSocketFactory : public yield::ipc::TCPSocketFactory
  {
  public:
    GridSSLSocketFactory( SSLContext& ssl_context )
      : ssl_context( ssl_context.inc_ref() )
    { }

    ~GridSSLSocketFactory()
    {
      SSLContext::dec_ref( ssl_context );
    }

    // TCPSocketFactory
    yield::platform::TCPSocket* createSocket()
    {
      return GridSSLSocket::create( ssl_context );
    }

  private:
    SSLContext& ssl_context;
  };
};

#endif
