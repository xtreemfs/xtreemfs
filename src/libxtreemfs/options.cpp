// Copyright (c) 2010 NEC HPC Europe
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
// DISCLAIMED. IN NO EVENT SHALL NEC HPC Europe BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#include "xtreemfs/options.h"
using namespace xtreemfs;

#include "yield.h"
using yield::ipc::ONCRPCClient;
using yield::platform::Path;


Options::Options
(
  Log* error_log,
  const Path& error_log_file_path,
  const Log::Level& error_log_level,
  const OptionParser::ParsedOptions& parsed_options,
  const vector<string>& positional_arguments,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
  SSLContext* ssl_context,
#endif
  Log* trace_log,
  URI* uri
)
  : error_log( error_log ),
    error_log_file_path( error_log_file_path ),
    error_log_level( error_log_level ),
    positional_arguments( positional_arguments ),
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
    ssl_context( ssl_context ),
#endif
    trace_log( trace_log ),
    uri( uri )
{
  assign( parsed_options.begin(), parsed_options.end() );
}

Options::Options( const Options& other )
  : OptionParser::ParsedOptions( other ),
    error_log( yidl::runtime::Object::inc_ref( other.error_log ) ),
    error_log_file_path( other.error_log_file_path ),
    error_log_level( other.error_log_level ),
    positional_arguments( other.positional_arguments ),
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
    ssl_context( yidl::runtime::Object::inc_ref( other.ssl_context ) ),
#endif
    trace_log( yidl::runtime::Object::inc_ref( other.trace_log ) ),
    uri( yidl::runtime::Object::inc_ref( other.uri ) )
{ }

Options::~Options()
{
  Log::dec_ref( error_log );
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
  SSLContext::dec_ref( ssl_context );
#endif
  Log::dec_ref( trace_log );
  URI::dec_ref( uri );
}

void Options::add_global_options( OptionParser& option_parser )
{
  option_parser.add_option( "-l", "log file path" );
  option_parser.add_option( "--log-file-path", "log file path" );
  const char* log_level_args[3] = { "-d", "--debug", "--log-level" };
  for ( uint8_t log_level_arg_i = 0; log_level_arg_i < 3; log_level_arg_i++ )
  {
    option_parser.add_option
    (
      log_level_args[log_level_arg_i],
      "log level: EMERG|ALERT|CRIT|ERR|WARNING|NOTICE|INFO|DEBUG"
    );
  }

#ifdef YIELD_PLATFORM_HAVE_OPENSSL
  option_parser.add_option( "--cert", "PEM certificate file path" );
  option_parser.add_option( "--pem-certificate-file-path" );
  option_parser.add_option( "--pkey", "PEM private key file path" );
  option_parser.add_option( "--pem-private-key-file-path" );
  option_parser.add_option( "--pass", "PEM private key passphrase" );
  option_parser.add_option( "--pem-private-key-passphrase" );
  option_parser.add_option( "--pkcs12-file-path" );
  option_parser.add_option( "--pkcs12-passphrase" );
#endif
}

const vector<string>& Options::get_positional_arguments() const
{
  return positional_arguments;
}

Options Options::parse( int argc, char** argv )
{
  return parse( argc, argv, OptionParser::Options() );
}

Options 
Options::parse
( 
   int argc, 
   char** argv, 
   const OptionParser::Options& other_options 
)
{
  string error_log_file_path;
  Log::Level error_log_level( Log::LOG_ERR );
  Log* trace_log = NULL;

  string pem_certificate_file_path,
         pem_private_key_file_path,
         pem_private_key_passphrase;
  string pkcs12_file_path, pkcs12_passphrase;

  OptionParser option_parser;
  add_global_options( option_parser );
  option_parser.add_options( other_options );

  vector<OptionParser::ParsedOption> parsed_options;
  vector<string> positional_arguments;
  option_parser.parse_args( argc, argv, parsed_options, positional_arguments );

  for
  (
    iterator parsed_option_i = parsed_options.begin();
    parsed_option_i != parsed_options.end();
    ++parsed_option_i
  )
  {
    const OptionParser::ParsedOption& option = *parsed_option_i;

    if ( option == "-d" || option == "--debug" )
      trace_log = &Log::open( std::cout, Log::Level( option.get_argument() ) );
    if ( option == "--log-file-path" )
      error_log_file_path = option.get_argument();
    else if ( option == "--log-level" )
      error_log_level = Log::Level( option.get_argument() );
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
    else
      continue;

    parsed_options.erase( parsed_option_i );
  }


  // TODO: have a silent mode where no logging is done?
  Log* error_log = &Log::open( error_log_file_path, error_log_level );
  

#ifdef YIELD_PLATFORM_HAVE_OPENSSL
  SSLContext* ssl_context;
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
    ssl_context = NULL;
#endif


  URI* uri;
  if ( !positional_arguments.empty() )
  {
    string uri_string( positional_arguments[0] );
    if ( uri_string.find( "://" ) == string::npos )
    {
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
      if ( ssl_context != NULL )
        uri_string = string( "oncrpcs://" ) + uri_string;
      else
#endif
        uri_string = string( "oncrpc://" ) + uri_string;
    }

    uri = URI::parse( uri_string );

    if ( uri != NULL )
      positional_arguments.erase( positional_arguments.begin() );
  }
  else
    uri = NULL;


  return *new Options
              (
                error_log,
                error_log_file_path,
                error_log_level,
                parsed_options,
                positional_arguments,
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
                ssl_context,
#endif
                trace_log,
                uri
              );
}

string Options::usage()
{
  return usage( OptionParser::Options() );
}

string Options::usage( const OptionParser::Options& other_options )
{
  OptionParser option_parser;
  add_global_options( option_parser );
  option_parser.add_options( other_options );
  return option_parser.usage();
}
