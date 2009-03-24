#ifndef ORG_XTREEMFS_CLIENT_OPTIONS_H
#define ORG_XTREEMFS_CLIENT_OPTIONS_H

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
          addOption( OPTION_PARSER_OPT_DEBUG, "-d", "--debug" ); 
          debug = false;

          addOption( OPTION_PARSER_OPT_HELP, "-h", "--help" );
          help = false;

          addOption( OPTION_PARSER_OPT_TIMEOUT_MS, "-t", "--timeout-ms", "n" ); 
          timeout_ms = Proxy::PROXY_DEFAULT_OPERATION_TIMEOUT_MS;
        }

        void addOption( int id, const char* short_arg, const char* long_arg = NULL, const char* default_values = NULL )
        {
          options.push_back( Option( id, short_arg, long_arg, default_values ) );
        }

        void printUsage()
        {
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
        }

        // Accessors for built-in options
        bool get_debug() const { return debug; }
        bool get_help() const { return help; } // Lassie?
        uint64_t get_timeout_ms() const { return timeout_ms; }

      protected:
        void parseOptions( int argc, char** argv )
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

          CSimpleOpt args( argc, argv, &simpleopt_options[0] );

          while ( args.Next() )
          {
            if ( args.LastError() == SO_SUCCESS )
            {
              switch ( args.OptionId() )
              {
                case OPTION_PARSER_OPT_DEBUG: debug = true; break;
                case OPTION_PARSER_OPT_HELP: help = true; return;

                case OPTION_PARSER_OPT_TIMEOUT_MS:
                {
                  timeout_ms = atol( args.OptionArg() );
                  if ( timeout_ms == 0 )
                    timeout_ms = Proxy::PROXY_DEFAULT_OPERATION_TIMEOUT_MS;
                }
                break;

                default:
                {
                  if ( !help )
                    parseOption( args.OptionId(), args.OptionArg() );
                }
                break;
              }
            }
          }

          if ( !help && args.FileCount() > 0 )
            parseFiles( args.FileCount(), args.Files() );
        }

        virtual void parseOption( int id, const char* sep_arg )
        { }

        virtual void parseFiles( int file_count, char** files )
        { }

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

          bool operator<( const Option& other )
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
        uint64_t timeout_ms;

        enum
        {
          OPTION_PARSER_OPT_DEBUG = 1,
          OPTION_PARSER_OPT_HELP = 2,
          OPTION_PARSER_OPT_TIMEOUT_MS = 3
        };
      };
    };
  };
};

#endif
