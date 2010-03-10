#include "xtreemfs/options.h"
using namespace xtreemfs;

#include "yield.h"
using yield::ipc::ONCRPCClient;
using yield::platform::Path;


Options Options::parse( int argc, char** argv )
{
  OptionParser option_parser;
  return parse( argc, argv, option_parser );
}

Options Options::parse( int argc, char** argv, OptionParser& option_parser )
{
  string log_file_path;
  option_parser.add_option( "-l", "log file path", true );
  option_parser.add_option( "--log-file-path", "log file path", true );

  string log_level;
  const char* log_level_args[3] = { "-d", "--debug", "--log-level" };
  for ( uint8_t log_level_arg_i = 0; log_level_arg_i < 3; log_level_arg_i++ )
  {
    option_parser.add_option
    ( 
      log_level_args[log_level_arg_i],
      "log level: EMERG|ALERT|CRIT|ERR|WARNING|NOTICE|INFO|DEBUG", 
      true 
    );
  }

  double timeout_ns;
  option_parser.add_option( "-t", "timeout (ms)", true );
  timeout_ns = ONCRPCClient::OPERATION_TIMEOUT_DEFAULT;

#ifdef YIELD_IPC_HAVE_OPENSSL
  string pem_certificate_file_path,
         pem_private_key_file_path,
         pem_private_key_passphrase;
  option_parser.add_option( "--cert", "PEM certificate file path", true );
  option_parser.add_option( "--pem-certificate-file-path", true );
  option_parser.add_option( "--pkey", "PEM private key file path", true );
  option_parser.add_option( "--pem-private-key-file-path", true );
  option_parser.add_option( "--pass", "PEM private key passphrase", true );
  option_parser.add_option( "--pem-private-key-passphrase", true );
  
  string pkcs12_file_path, pkcs12_passphrase;
  option_parser.add_option( "--pkcs12-file-path", true );
  option_parser.add_option( "--pkcs12-passphrase", true );
#endif

  uint32_t proxy_flags = 0;
  option_parser.add_option( "--trace-auth" );
  option_parser.add_option( "--trace-network-io" );
  option_parser.add_option( "--trace-network-operations" );

  vector<OptionParser::ParsedOption> parsed_options;
  option_parser.parse_args( argc, argv, parsed_options );

  for 
  ( 
    const_iterator parsed_option_i = parsed_options.begin();
    parsed_option_i != parsed_options.end();
    ++parsed_option_i
  )
  {
    const OptionParser::ParsedOption& parsed_option = *parsed_option_i;
    const std::string& arg = parsed_option.get_arg();
    const std::string& value = parsed_option.get_value();

    if ( arg == "--log-file-path" )
      log_file_path = value;
    else if ( arg == "-d" || arg == "--debug" || arg == "--log-level" )
      log_level = value;
    else if ( arg == "-t" )
    {
      double timeout_ms = atof( value.c_str() );
      if ( timeout_ms != 0 )
        timeout_ns = timeout_ms * Time::NS_IN_MS;
    }
    else if ( arg == "--cert" || arg == "--pem_certificate_file_path" )
      pem_certificate_file_path = arg;
    else if ( arg == "--pkey" || arg == "--pem-private-key-file-path" )
      pem_private_key_file_path = value;
    else if ( arg == "--pass" || arg == "--pem-private-key-passphrase" )
      pem_private_key_passphrase = value;
    else if ( arg == "--pkcs12-file-path" )
      pkcs12_file_path = value;
    else if ( arg == "--pkcs12-passphrase" )
      pkcs12_passphrase = value;
    //else if ( arg == "--trace-auth" )
    //  proxy_flags |= ONCPCClient::FLAG_TRACE_AUTH;
    else if ( arg == "--trace-network-io" )
      proxy_flags |= ONCRPCClient::FLAG_TRACE_NETWORK_IO;
    else if ( arg == "--trace-network-operations" )
      proxy_flags |= ONCRPCClient::FLAG_TRACE_OPERATIONS;
  }

  SSLContext* ssl_context;
#ifdef YIELD_IPC_HAVE_OPENSSL
  if ( !pkcs12_file_path.empty() )
  {
    ssl_context =
      &SSLContext::create
      (
        SSLv3_client_method(),
        Path( pkcs12_file_path ),
        pkcs12_passphrase
      );
  }
  else if
  (
    !pem_certificate_file_path.empty() 
    &&
    !pem_private_key_file_path.empty()
  )
  {
    ssl_context =
      &SSLContext::create
      (
        SSLv3_client_method(),
        Path( pem_certificate_file_path ),
        Path( pem_private_key_file_path ),
        pem_private_key_passphrase
      );
  }
  else
#endif
    ssl_context = NULL;

  return *new Options 
              (
                log_file_path,
                log_level,
                parsed_options,
                proxy_flags,
                ssl_context,
                timeout_ns
              );
}
