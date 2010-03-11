// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the XtreemFS project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the XtreemFS project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#include "xtreemfs/options.h"
using namespace xtreemfs;
#include "xtreemfs/interfaces/constants.h"
using org::xtreemfs::interfaces::ONCRPC_SCHEME;
using org::xtreemfs::interfaces::ONCRPCS_SCHEME;

#include "yield.h"
using yield::ipc::ONCRPCClient;
using yield::platform::Path;


Options::Options
(
  const string& log_file_path,
  const string& log_level,
  const vector<OptionParser::ParsedOption>& parsed_options,
  const vector<string>& positional_arguments,
  uint32_t proxy_flags,
  SSLContext* ssl_context,
  const Time& timeout,
  URI* uri
)
  : log_file_path( log_file_path ),
    log_level( log_level ),
    positional_arguments( positional_arguments ),
    proxy_flags( proxy_flags ),
    ssl_context( ssl_context ),
    timeout( timeout ),
    uri( uri )
{
  assign( parsed_options.begin(), parsed_options.end() );
}

Options::~Options()
{
  SSLContext::dec_ref( ssl_context );
  URI::dec_ref( uri );
}

const vector<string>& Options::get_positional_arguments() const
{
  return positional_arguments;
}

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
  vector<string> positional_arguments;
  option_parser.parse_args( argc, argv, parsed_options, positional_arguments );

  for
  (
    const_iterator parsed_option_i = parsed_options.begin();
    parsed_option_i != parsed_options.end();
    ++parsed_option_i
  )
  {
    const OptionParser::ParsedOption& option = *parsed_option_i;

    if ( option == "--log-file-path" )
      log_file_path = option.get_argument();
    else if 
    ( 
      option == "-d" 
      || 
      option == "--debug" 
      || 
      option == "--log-level" 
    )
      log_level = option.get_argument();
    else if ( option == "-t" )
    {
      double timeout_ms = atof( option.get_argument().c_str() );
      if ( timeout_ms != 0 )
        timeout_ns = timeout_ms * Time::NS_IN_MS;
    }
    else if ( option == "--cert" || option == "--pem_certificate_file_path" )
      pem_certificate_file_path = option.get_argument();
    else if ( option == "--pkey" || option == "--pem-private-key-file-path" )
      pem_private_key_file_path = option.get_argument();
    else if ( option == "--pass" || option == "--pem-private-key-passphrase" )
      pem_private_key_passphrase = option.get_argument();
    else if ( option == "--pkcs12-file-path" )
      pkcs12_file_path = option.get_argument();
    else if ( option == "--pkcs12-passphrase" )
      pkcs12_passphrase = option.get_argument();
    //else if ( option == "--trace-auth" )
    //  proxy_flags |= ONCPCClient::FLAG_TRACE_AUTH;
    else if ( option == "--trace-network-io" )
      proxy_flags |= ONCRPCClient::FLAG_TRACE_NETWORK_IO;
    else if ( option == "--trace-network-operations" )
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

  URI* uri;
  if ( !positional_arguments.empty() )
  {
    string uri_string( positional_arguments[0] );
    if ( uri_string.find( "://" ) == string::npos )
    {
      if ( ssl_context != NULL )
        uri_string = ONCRPCS_SCHEME + string( "://" ) + uri_string;
      else
        uri_string = ONCRPC_SCHEME + string( "://" ) + uri_string;
    }

    uri = URI::parse( uri_string );

    if ( uri != NULL )
      positional_arguments.erase( positional_arguments.begin() );
  }

  return *new Options
              (
                log_file_path,
                log_level,
                parsed_options,
                positional_arguments,
                proxy_flags,
                ssl_context,
                timeout_ns,
                uri
              );
}
