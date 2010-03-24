#include "nettest_proxy.h"
using namespace xtreemfs;

#include "yield.h"
using yield::platform::Exception;


NettestProxy::NettestProxy
(
  Configuration& configuration,
  Log* error_log,
  IOQueue& io_queue,
  SocketAddress& peername,
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
    configuration,
    error_log,
    io_queue,
    peername,
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
                    *new Configuration,
                    options.get_error_log(),
                    yield::platform::NBIOQueue::create(),
                    createSocketAddress( *options.get_uri() ),
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
