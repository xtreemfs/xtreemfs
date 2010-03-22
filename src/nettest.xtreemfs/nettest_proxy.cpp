#include "nettest_proxy.h"
using namespace xtreemfs;

#include "yield.h"
using yield::platform::Exception;


NettestProxy::NettestProxy
(
  uint16_t concurrency_level,
  Log* error_log,
  IOQueue& io_queue,
  const Time& operation_timeout,
  SocketAddress& peername,
  uint16_t reconnect_tries_max,
  SocketFactory& socket_factory,
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
    error_log,
    io_queue,
    operation_timeout,
    peername,
    reconnect_tries_max,
    socket_factory,
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
                    options.get_error_log(),
                    yield::platform::NBIOQueue::create(),
                    options.get_timeout(),
                    createSocketAddress( *options.get_uri() ),
                    RECONNECT_TRIES_MAX_DEFAULT,
                    createSocketFactory
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
