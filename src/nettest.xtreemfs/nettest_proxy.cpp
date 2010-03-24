#include "nettest_proxy.h"
using namespace xtreemfs;

#include "yield.h"
using yield::platform::Exception;


NettestProxy::NettestProxy
(
  uint16_t concurrency_level,
  const Time& connect_timeout,
  Log* error_log,
  IOQueue& io_queue,
  SocketAddress& peername,
  uint16_t reconnect_tries_max,
  const Time& recv_timeout,
  const Time& send_timeout,
  TCPSocketFactory& tcp_socket_factory,
  Log* trace_log,
  UserCredentialsCache* user_credentials_cache
)
: Proxy
  <
    org::xtreemfs::interfaces::NettestInterface,
    org::xtreemfs::interfaces::NettestInterfaceMessageFactory,
    org::xtreemfs::interfaces::NettestInterfaceMessageSender
  >
  (
    concurrency_level,
    connect_timeout,
    error_log,
    io_queue,
    peername,
    reconnect_tries_max,
    recv_timeout,
    send_timeout,
    tcp_socket_factory,
    trace_log,
    user_credentials_cache
  )
{ }

NettestProxy& NettestProxy::create( const Options& options )
{
  if ( options.get_uri() != NULL )
  {
    if ( options.get_uri()->get_port() != 0 )
    {
      return *new NettestProxy
                  (
                    CONCURRENCY_LEVEL_DEFAULT,
                    CONNECT_TIMEOUT_DEFAULT,
                    options.get_error_log(),
                    yield::platform::NBIOQueue::create(),
                    createSocketAddress( *options.get_uri() ),
                    RECONNECT_TRIES_MAX_DEFAULT,
                    RECV_TIMEOUT_DEFAULT,
                    SEND_TIMEOUT_DEFAULT,
                    createTCPSocketFactory
                    ( 
                      *options.get_uri(), 
                      options.get_ssl_context() 
                    ),
                    options.get_trace_log(),
                    NULL
                  );
    }
    else
      throw Exception( "NettestProxy: must specify port in URI" );
  }
  else
    throw Exception( "NettestProxy: must specify a URI" );
}
