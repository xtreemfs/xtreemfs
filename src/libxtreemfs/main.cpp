#include "xtreemfs/main.h"
#include "xtreemfs/options.h"
using namespace xtreemfs;


Main::~Main()
{
  Log::dec_ref( log );
}

DIRProxy& Main::createDIRProxy( const URI& absolute_uri, Options& options )
{
  return DIRProxy::create
  (
    absolute_uri,
    DIRProxy::CONCURRENCY_LEVEL_DEFAULT,
    options.get_proxy_flags(),
    &get_log( options ),
    options.get_timeout(),
    DIRProxy::RECONNECT_TRIES_MAX_DEFAULT,
    options.get_ssl_context()
  );
}

MRCProxy& 
Main::createMRCProxy
(
  const URI& absolute_uri,
  Options& options,
  const char* password
)
{
  return MRCProxy::create
  (
    absolute_uri,
    MRCProxy::CONCURRENCY_LEVEL_DEFAULT,
    options.get_proxy_flags(),
    &get_log( options ),
    options.get_timeout(),
    password,
    MRCProxy::RECONNECT_TRIES_MAX_DEFAULT,
    options.get_ssl_context()
  );
}

Log& Main::get_log( Options& options )
{
  if ( log == NULL )
  {
    if 
    ( 
      options.get_log_file_path().empty()
      || 
      options.get_log_file_path() == "-" 
    )
      log = &Log::open( std::cerr, options.get_log_level() );
    else
    {
#ifndef _WIN32
      ::close( STDIN_FILENO );
      ::close( STDOUT_FILENO );
      ::close( STDERR_FILENO );
#endif
      log =
        &Log::open
        (
          options.get_log_file_path(),
          options.get_log_level(),
          true
        ); // true = lazy open
    }
  }

  return *log;
}


