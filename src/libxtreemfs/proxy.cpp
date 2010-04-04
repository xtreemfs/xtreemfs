#include "xtreemfs/proxy.h"
#include "grid_ssl_socket.h"
using namespace xtreemfs;

#include "xtreemfs/interfaces/constants.h"
using namespace org::xtreemfs::interfaces;

#include "yield.h"
using yield::ipc::ONCRPCSSLSocketClient;
using yield::ipc::ONCRPCTCPSocketClient;
using yield::ipc::ONCRPCUDPSocketClient;
using yield::platform::NBIOQueue;
using yield::platform::SocketAddress;


EventHandler&
Proxy::createONCRPCClient
(
  const URI& absolute_uri,
  MessageFactory& message_factory,
  uint16_t port_default,
  uint32_t prog,
  uint32_t vers,
  Log* error_log,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
  SSLContext* ssl_context,
#endif
  Log* trace_log
)
{
  const string& scheme = absolute_uri.get_scheme();

  URI absolute_uri_with_port( absolute_uri );
  if ( absolute_uri_with_port.get_port() == 0 )
    absolute_uri_with_port.set_port( port_default );

  if ( scheme == "oncrpcu" )
  {
    return ONCRPCUDPSocketClient::create
           (
             absolute_uri_with_port,
             message_factory,
             prog,
             vers,
             error_log,
             ONCRPCUDPSocketClient::RECV_TIMEOUT_DEFAULT,
             trace_log
           );
  }
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
  else if ( scheme == "oncrpcg" && ssl_context != NULL )
  {
    SocketAddress* peername = 
      SocketAddress::create
      ( 
        absolute_uri_with_port.get_host().c_str(),
        absolute_uri_with_port.get_port() 
      );

    if ( peername != NULL )
    {
      GridSSLSocket* grid_ssl_socket = GridSSLSocket::create( *ssl_context );
      if ( grid_ssl_socket != NULL )
      {
        NBIOQueue* nbio_queue = NBIOQueue::create();
        if ( nbio_queue != NULL && grid_ssl_socket->associate( *nbio_queue ) )
        {
          NBIOQueue::dec_ref( *nbio_queue );
          return *new ONCRPCSSLSocketClient
                      (
                        message_factory,
                        *peername,
                        prog,
                        *grid_ssl_socket,
                        vers,
                        NULL,
                        error_log,
                        trace_log
                      );                        
        }
        else
          NBIOQueue::dec_ref( nbio_queue );
      }
    }

    throw Exception();
  }
  else if ( scheme == "oncrpcs" && ssl_context != NULL )
  {
    return ONCRPCSSLSocketClient::create
           (
             absolute_uri_with_port,
             message_factory,
             prog,
             vers,
             NULL,
             error_log,
             ssl_context,
             trace_log
           );
  }
#endif
  else
  {
    return ONCRPCTCPSocketClient::create
           (
             absolute_uri_with_port,
             message_factory,
             prog,
             vers,
             NULL,
             error_log,
             trace_log
           );
  }
}
