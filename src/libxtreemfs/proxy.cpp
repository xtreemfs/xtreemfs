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
  Configuration* configuration,
  Log* error_log,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
  SSLContext* ssl_context,
#endif
  Log* trace_log
)
{
  const string& scheme = absolute_uri.get_scheme();

  const string& host = absolute_uri.get_host();
  uint16_t port;
  if ( absolute_uri.get_port() != 0 )
    port = absolute_uri.get_port();
  else
    port = port_default;

  SocketAddress* peername = SocketAddress::create( host.c_str(), port );
  if ( peername != NULL )
  {
    if ( scheme == ONCRPCU_SCHEME )
    {
      return ONCRPCUDPSocketClient::create
             (
               absolute_uri,
               message_factory,
               prog,
               vers,
               NULL,
               error_log,
               ONCRPCUDPSocketClient::RECV_TIMEOUT_DEFAULT,
               trace_log
             );
    }
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
    else if ( scheme == ONCRPCG_SCHEME && ssl_context != NULL )
    {
      GridSSLSocket* grid_ssl_socket = GridSSLSocket::create( *ssl_context );
      if ( grid_ssl_socket != NULL )
      {
        NBIOQueue& io_queue = NBIOQueue::create();
        if ( grid_ssl_socket->associate( io_queue ) )
        {
          NBIOQueue::dec_ref( io_queue );
          return *new ONCRPCSSLSocketClient
                      (
                        message_factory,
                        *peername,
                        prog,
                        *grid_ssl_socket,
                        vers,
                        configuration,
                        NULL,
                        error_log,
                        trace_log
                      );                        
        }
        else
        {
          NBIOQueue::dec_ref( io_queue );
          throw Exception();
        }
      }
      else
        throw Exception();
    }
    else if ( scheme == ONCRPCS_SCHEME && ssl_context != NULL )
    {
      return ONCRPCSSLSocketClient::create
             (
               absolute_uri,
               message_factory,
               *ssl_context,
               prog,
               vers,
               configuration,
               NULL,
               error_log,
               trace_log
             );
    }
#endif
    else
    {
      return ONCRPCTCPSocketClient::create
             (
               absolute_uri,
               message_factory,
               prog,
               vers,
               configuration,
               NULL,
               error_log,
               trace_log
             );
    }
  }
  else
    throw Exception();
}
