#include "nettest_proxy.h"
using namespace xtreemfs;

#include "yield.h"
using yield::platform::Exception;


NettestProxy::NettestProxy
(
  uint16_t concurrency_level,
  uint32_t flags,
  IOQueue& io_queue,
  Log* log,
  const Time& operation_timeout,
  SocketAddress& peername,
  uint16_t reconnect_tries_max,
  SocketFactory& socket_factory,
  UserCredentialsCache* user_credentials_cache
)
: Proxy
  <
    org::xtreemfs::interfaces::NettestInterface,
    org::xtreemfs::interfaces::NettestInterfaceEventFactory,
    org::xtreemfs::interfaces::NettestInterfaceEventSender
  >
  (
    concurrency_level,
    flags,
    io_queue,
    log,
    operation_timeout,
    peername,
    reconnect_tries_max,
    socket_factory,
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
                    options.get_proxy_flags(),
                    yield::platform::NBIOQueue::create(),
                    options.get_log(),
                    options.get_timeout(),
                    createSocketAddress( *options.get_uri() ),
                    RECONNECT_TRIES_MAX_DEFAULT,
                    createSocketFactory
                    ( 
                      *options.get_uri(), 
                      options.get_ssl_context() 
                    ),
                    NULL
                  );
    }
    else
      throw Exception( "NettestProxy: must specify port in URI" );
  }
  else
    throw Exception( "NettestProxy: must specify a URI" );
}

ONCRPCRequest& NettestProxy::createONCRPCRequest( MarshallableObject& body )
{
  return ONCRPCClient::createONCRPCRequest( body );
}
