#include "nettest_proxy.h"
using org::xtreemfs::interfaces::NettestInterfaceMessageFactory;
using namespace xtreemfs;


NettestProxy::NettestProxy( EventHandler& request_handler )
  : NettestInterfaceProxy( request_handler )
{ }

NettestProxy& NettestProxy::create( const Options& options )
{
  if ( options.get_uri() != NULL && options.get_uri()->get_port() != 0 )
  {
    return *new NettestProxy
                (
                  createONCRPCClient
                  (
                    *options.get_uri(),
                    *new NettestInterfaceMessageFactory,
                    0,
                    0x20000000 + TAG,
                    TAG,
                    NULL,
                    options.get_error_log(),
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
                    options.get_ssl_context(),
#endif
                    options.get_trace_log()
                  )
                );
  }
  else
    throw Exception( "NettestProxy: must specify a URI with a port" );
}
