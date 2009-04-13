// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ECTS_XTREEMFS_CLIENT_SRC_ORG_XTREEMFS_CLIENT_BIN_OPTIONS_H
#define ECTS_XTREEMFS_CLIENT_SRC_ORG_XTREEMFS_CLIENT_BIN_OPTIONS_H

#include "org/xtreemfs/client/proxy.h"

#include <iostream>
#include <memory>
#include <sstream>
#include <string>
#include <vector>

#include "SimpleOpt.h"

#include "org/xtreemfs/interfaces/constants.h"
using org::xtreemfs::interfaces::DEBUG_LEVEL_ERROR;
using org::xtreemfs::interfaces::DEBUG_LEVEL_WARN;
using org::xtreemfs::interfaces::DEBUG_LEVEL_INFO;
using org::xtreemfs::interfaces::DEBUG_LEVEL_DEBUG;
using org::xtreemfs::interfaces::DEBUG_LEVEL_TRACE;
using org::xtreemfs::interfaces::DEBUG_LEVEL_DEFAULT;
using org::xtreemfs::interfaces::DEBUG_LEVEL_MAX;


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class xtfs_bin
      {
      public:
        int main( int argc, char** argv )
        {
          try
          {
            if ( argc == 1 )
            {
              printUsage();
              return 0;
            }
            else if ( !options.empty() )
            {
              std::vector<CSimpleOpt::SOption> simpleopt_options;
              for ( std::vector<Option>::const_iterator option_i = options.begin(); option_i != options.end(); option_i++ )
              {
                const Option& option = *option_i;
                CSimpleOpt::SOption short_simpleopt_option = { option.get_id(), option.get_short_arg(), option.get_default_values() ? SO_REQ_SEP : SO_NONE };
                simpleopt_options.push_back( short_simpleopt_option );
                if ( option.get_long_arg() )
                {
                  CSimpleOpt::SOption long_simpleopt_option = { option.get_id(), option.get_long_arg(), option.get_default_values() ? SO_REQ_SEP : SO_NONE };
                  simpleopt_options.push_back( long_simpleopt_option );
                }
              }
              CSimpleOpt::SOption sentinel_simpleopt_option = SO_END_OF_OPTIONS;
              simpleopt_options.push_back( sentinel_simpleopt_option );

              CSimpleOpt args( argc, argv, &simpleopt_options[0] );

              while ( args.Next() )
              {
                if ( args.LastError() == SO_SUCCESS )
                {
                  switch ( args.OptionId() )
                  {
                    case OPTION_DEBUG_LEVEL:
                    {
                      debug_level = static_cast<uint8_t>( atoi( args.OptionArg() ) );
                      if ( debug_level > DEBUG_LEVEL_MAX )
                        debug_level = DEBUG_LEVEL_MAX;

                      if ( debug_level >= DEBUG_LEVEL_TRACE )
                        YIELD::TCPSocket::set_trace_socket_io_onoff( true );
                    }
                    break;

                    case OPTION_HELP: printUsage(); return 0;
                    case OPTION_PEM_CERTIFICATE_FILE_PATH: pem_certificate_file_path = args.OptionArg(); break;
                    case OPTION_PEM_PRIVATE_KEY_FILE_PATH: pem_private_key_file_path = args.OptionArg(); break;
                    case OPTION_PEM_PRIVATE_KEY_PASSPHRASE: pem_private_key_passphrase = args.OptionArg(); break;
                    case OPTION_PKCS12_FILE_PATH: pkcs12_file_path = args.OptionArg(); break;
                    case OPTION_PKCS12_PASSPHRASE: pkcs12_passphrase = args.OptionArg(); break;

                    case OPTION_TIMEOUT_MS:
                    {
                      timeout_ms = atol( args.OptionArg() );
                      if ( timeout_ms == 0 )
                        timeout_ms = Proxy::PROXY_DEFAULT_OPERATION_TIMEOUT_MS;
                    }
                    break;
                  }

                  parseOption( args.OptionId(), args.OptionArg() );
                }
              }

              parseFiles( args.FileCount(), args.Files() );
            }
            else
              parseFiles( argc - 1, argv+1 );

            _main( argc, argv );

            return 0;
          }
          catch ( YIELD::Exception& exc )
          {
            std::cerr << exc.what() << std::endl;

            if ( exc.get_error_code() > 0 )
              return exc.get_error_code();
            else
              return 1;
          }
          catch ( std::exception& exc )
          {
            std::cerr << exc.what() << std::endl;

            return 1;
          }
        }

      protected:
        enum
        {
          OPTION_DEBUG_LEVEL = 1,
          OPTION_HELP = 2,
          OPTION_PKCS12_FILE_PATH = 3,
          OPTION_PKCS12_PASSPHRASE = 4,
          OPTION_PEM_CERTIFICATE_FILE_PATH = 5,
          OPTION_PEM_PRIVATE_KEY_FILE_PATH = 6,
          OPTION_PEM_PRIVATE_KEY_PASSPHRASE = 7,
          OPTION_TIMEOUT_MS = 8
        };


        xtfs_bin( const char* program_name, const char* program_description, const char* files_usage = NULL )
          : program_name( program_name ), program_description( program_description ), files_usage( files_usage )
        {
          std::ostringstream debug_level_default_ss;
          debug_level_default_ss << static_cast<uint16_t>( DEBUG_LEVEL_DEFAULT );
          debug_level_default_str = debug_level_default_ss.str();
          addOption( OPTION_DEBUG_LEVEL, "-d", "--debug", debug_level_default_str.c_str() );
          addOption( OPTION_DEBUG_LEVEL, "--debug-level", "--debug_level", debug_level_default_str.c_str() );
          debug_level = 0;

          addOption( OPTION_HELP, "-h", "--help" );

          addOption( OPTION_PEM_CERTIFICATE_FILE_PATH, "--cert", "--pem-certificate-file-path", "PEM certificate file path" );
          addOption( OPTION_PEM_PRIVATE_KEY_FILE_PATH, "--pkey", "--pem-private-key-file-path", "PEM private key file path" );
          addOption( OPTION_PEM_PRIVATE_KEY_PASSPHRASE, "--pass", "--pem-private-key-passphrase", "PEM private key passphrase" );
          addOption( OPTION_PKCS12_FILE_PATH, "--pkcs12-file-path", NULL, "PKCS#12 file path" );
          addOption( OPTION_PKCS12_PASSPHRASE, "--pkcs12-passphrase", NULL, "PKCS#12 passphrase" );
          ssl_context = NULL;

          addOption( OPTION_TIMEOUT_MS, "-t", "--timeout-ms", "n" );
          timeout_ms = Proxy::PROXY_DEFAULT_OPERATION_TIMEOUT_MS;
        }

        virtual ~xtfs_bin()
        {
          delete ssl_context;
        }

        void addOption( int id, const char* short_arg, const char* long_arg = NULL, const char* default_values = NULL )
        {
          options.push_back( Option( id, short_arg, long_arg, default_values ) );
        }

        template <class ProxyType>
        ProxyType* createProxy( const YIELD::URI& uri )
        {
          if ( ssl_context == NULL )
          {
            if ( !pkcs12_file_path.empty() )
              ssl_context = new YIELD::SSLContext( SSLv3_client_method(), pkcs12_file_path, pkcs12_passphrase );
            else if ( !pem_certificate_file_path.empty() && !pem_private_key_file_path.empty() )
              ssl_context = new YIELD::SSLContext( SSLv3_client_method(), pem_certificate_file_path, pem_private_key_file_path, pem_private_key_passphrase );
          }

          ProxyType* proxy;
          if ( ssl_context != NULL )
            proxy = new ProxyType( uri, *ssl_context );
          else
            proxy = new ProxyType( uri );

          proxy->set_operation_timeout_ms( timeout_ms );

          return proxy;
        }

        // Accessors for built-in options
        uint8_t get_debug_level() const { return debug_level; }
        uint64_t get_timeout_ms() const { return timeout_ms; }

        virtual int _main( int argc, char** argv ) = 0;

        std::auto_ptr<YIELD::URI> parseURI( const char* uri_c_str )
        {
          std::string uri_str( uri_c_str );
          if ( uri_str.find( "://" ) == std::string::npos )
          {
            if ( !pkcs12_file_path.empty() || ( !pem_certificate_file_path.empty() && !pem_private_key_file_path.empty() ) )
              uri_str = org::xtreemfs::interfaces::ONCRPCS_SCHEME + std::string( "://" ) + uri_str;
            else
              uri_str = org::xtreemfs::interfaces::ONCRPC_SCHEME + std::string( "://" ) + uri_str;
          }

          return std::auto_ptr<YIELD::URI>( new YIELD::URI( uri_str ) );
        }

      private:
        const char *program_name, *program_description, *files_usage;

        // Built-in options
        uint8_t debug_level; std::string debug_level_default_str;
        std::string pem_certificate_file_path, pem_private_key_file_path, pem_private_key_passphrase;
        std::string pkcs12_file_path, pkcs12_passphrase;
        YIELD::SSLContext* ssl_context;
        uint64_t timeout_ms;

        class Option
        {
        public:
          Option( int id, const char* short_arg, const char* long_arg, const char* default_values )
            : id( id ), short_arg( short_arg ), long_arg( long_arg ), default_values( default_values )
          { }

          int get_id() const { return id; }
          const char* get_short_arg() const { return short_arg; }
          const char* get_long_arg() const { return long_arg; }
          const char* get_default_values() const { return default_values; }

          bool operator<( const Option& other ) const
          {
            return strcmp( get_short_arg(), other.get_short_arg() ) < 0;
          }

        private:
          int id;
          const char *short_arg, *long_arg, *default_values;
        };

        std::vector<Option> options;


        virtual void parseOption( int id, char* sep_arg )
        { }

        virtual void parseFiles( int file_count, char** files )
        { }

        void printUsage()
        {
          std::cout << std::endl;
          std::cout << program_name << ": " << program_description << std::endl;
          std::cout << std::endl;
          std::cout << "Usage:" << std::endl;
          std::cout << "  " << program_name << " [options]";
          if ( files_usage )
            std::cout << " " << files_usage;
          std::cout << std::endl;
          std::cout << std::endl;
          std::cout << "xtfs_bin:" << std::endl;
          std::sort( options.begin(), options.end() );
          for ( std::vector<Option>::const_iterator option_i = options.begin(); option_i != options.end(); option_i++ )
          {
            const Option& option = *option_i;
            std::cout << "  " << option.get_short_arg();
            if ( option.get_long_arg() )
              std::cout << "/" << option.get_long_arg();
            if ( option.get_default_values() )
              std::cout << "=" << option.get_default_values();
            std::cout << std::endl;
           }
           std::cout << std::endl;
        }
      };
    };
  };
};

#endif
