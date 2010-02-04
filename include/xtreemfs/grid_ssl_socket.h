// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

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

#endif
