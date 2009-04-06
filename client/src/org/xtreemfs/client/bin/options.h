// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ECTS_XTREEMFS_CLIENT_SRC_ORG_XTREEMFS_CLIENT_BIN_OPTIONS_H
#define ECTS_XTREEMFS_CLIENT_SRC_ORG_XTREEMFS_CLIENT_BIN_OPTIONS_H

#include "org/xtreemfs/client/proxy.h"

#include <string>
#include <vector>
#include <iostream>

#include "SimpleOpt.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class Options
      {
      public:
        Options( const char* program_name, const char* program_description, const char* files_usage = NULL )
          : program_name( program_name ), program_description( program_description ), files_usage( files_usage )
        {
          addOption( OPTION_DEBUG, "-d", "--debug" );
          debug = false;

          addOption( OPTION_HELP, "-h", "--help" );
          help = false;

          addOption( OPTION_PKCS12_FILE_PATH, "--pkcs12-file-path", NULL, "PKCS#12 file path" );
          addOption( OPTION_PKCS12_PASSPHRASE, "--pkcs12-passphrase", NULL, "PKCS#12 passphrase" );

          addOption( OPTION_TIMEOUT_MS, "-t", "--timeout-ms", "n" );
          timeout_ms = Proxy::PROXY_DEFAULT_OPERATION_TIMEOUT_MS;

          addOption( OPTION_TRACE_SOCKET_IO, "--trace-socket-io" );
        }

        void addOption( int id, const char* short_arg, const char* long_arg = NULL, const char* default_values = NULL )
        {
          options.push_back( Option( id, short_arg, long_arg, default_values ) );
        }

        // TODO: this should really be in a singleton class inherited by all binaries
        template <class ProxyType>
        ProxyType* createProxy( const YIELD::URI& uri )
        {
          if ( !get_pkcs12_file_path().empty() )
            return new ProxyType( uri, get_pkcs12_file_path(), get_pkcs12_passphrase() );
          else
            return new ProxyType( uri );
        }

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
          std::cout << "Options:" << std::endl;
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

        // Accessors for built-in options
        bool get_debug() const { return debug; }
        bool get_help() const { return help; } // Lassie?
        const std::string& get_pkcs12_file_path() const { return pkcs12_file_path; }
        const std::string& get_pkcs12_passphrase() const { return pkcs12_passphrase; }
        uint64_t get_timeout_ms() const { return timeout_ms; }

      protected:
        enum
        {
          OPTION_DEBUG = 1,
          OPTION_HELP = 2,
          OPTION_PKCS12_FILE_PATH = 3,
          OPTION_PKCS12_PASSPHRASE = 4,
          OPTION_TIMEOUT_MS = 5,
          OPTION_TRACE_SOCKET_IO = 6
        };

        void parseOptions( int argc, char** argv )
        {
          if ( argc == 1 )
          {
            help = true;
            return;
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
                  case OPTION_DEBUG: debug = true; break;
                  case OPTION_HELP: help = true; return;
                  case OPTION_PKCS12_FILE_PATH: pkcs12_file_path = args.OptionArg(); break;
                  case OPTION_PKCS12_PASSPHRASE: pkcs12_passphrase = args.OptionArg(); break;

                  case OPTION_TIMEOUT_MS:
                  {
                    timeout_ms = atol( args.OptionArg() );
                    if ( timeout_ms == 0 )
                      timeout_ms = Proxy::PROXY_DEFAULT_OPERATION_TIMEOUT_MS;
                  }
                  break;

                  case OPTION_TRACE_SOCKET_IO: YIELD::SocketConnection::set_trace_socket_io_onoff( true ); break;
                }

                if ( !help )
                  parseOption( args.OptionId(), args.OptionArg() );
              }
            }

            if ( !help && args.FileCount() > 0 )
              parseFiles( args.FileCount(), args.Files() );
          }
          else
            parseFiles( argc - 1, argv+1 );
        }

        virtual void parseOption( int id, char* sep_arg )
        { }

        virtual void parseFiles( int file_count, char** files )
        { }

        YIELD::URI* parseURI( const char* uri_c_str )
        {
          std::string uri_str( uri_c_str );
          if ( uri_str.find( "://" ) == std::string::npos )
          {
            if ( !get_pkcs12_file_path().empty() )
              uri_str = org::xtreemfs::interfaces::ONCRPCS_SCHEME + std::string( "://" ) + uri_str;
            else
              uri_str = org::xtreemfs::interfaces::ONCRPC_SCHEME + std::string( "://" ) + uri_str;
          }

          return new YIELD::URI( uri_str );
        }

      private:
        const char *program_name, *program_description, *files_usage;

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

        // Built-in options
        bool debug, help;
        std::string pkcs12_file_path, pkcs12_passphrase;
        uint64_t timeout_ms;
      };
    };
  };
};

#endif
