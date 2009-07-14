// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _YIELD_MAIN_H_
#define _YIELD_MAIN_H_

#include "yield.h"

#include <algorithm>
#include <cstdlib> // For srand
#include <cstring>
#include <ctime> // For time()
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4995 )
#endif
#include "SimpleOpt.h"
#ifdef _WIN32
#pragma warning( pop )
#endif

#if defined(_WIN32)
#include <winsock2.h>
#pragma comment( lib, "ws2_32.lib" )
#elif defined(__MACH__)
#include <mach-o/dyld.h> // For _NSGetExecutablePath
#include <signal.h>
#else
#include <signal.h>
#endif


namespace YIELD
{
  class Main
  {
  public:
    Log::Level get_log_level() const { return log_level; }
    const char* get_program_name() const { return program_name; }

#ifdef _WIN32
    virtual int main( char* args ) // From WinMain
    {
      std::vector<char*> argvv;

      char argv0[PATH_MAX];
      GetModuleFileNameA( NULL, argv0, PATH_MAX );
      argvv.push_back( argv0 );

      const char *start_args_p = args, *args_p = start_args_p;
      while ( *args_p != 0 )
      {
        while ( *args_p != ' ' && *args_p != 0 ) args_p++;
        size_t arg_len = args_p - start_args_p;
        char* arg = new char[arg_len+1];
        memcpy_s( arg, arg_len, start_args_p, arg_len );
        arg[arg_len] = 0;
        argvv.push_back( arg );
        if ( *args_p != 0 )
        {
          args_p++;
          start_args_p = args_p;
        }
      }

      int ret = main( static_cast<int>( argvv.size() ), &argvv[0] );

      for ( std::vector<char*>::size_type argvv_i = 1; argvv_i < argvv.size(); argvv_i++ )
        delete [] argvv[argvv_i];

      return ret;
    }
#endif

    virtual int main( int argc, char** argv )
    {
#ifdef _WIN32
      WORD wVersionRequested = MAKEWORD( 2, 2 );
      WSADATA wsaData;
      WSAStartup( wVersionRequested, &wsaData );
#else
      signal( SIGPIPE, SIG_IGN );
#endif

      srand( static_cast<unsigned int>( std::time( 0 ) ) );

      try
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

        // Make copies of the strings in argv so that SimpleOpt can punch holes in them
        std::vector<char*> argvv;
        for ( int arg_i = 0; arg_i < argc; arg_i++ )
        {
          size_t arg_len = strnlen( argv[arg_i], SIZE_MAX ) + 1;
          char* arg = new char[arg_len];
          memcpy_s( arg, arg_len, argv[arg_i], arg_len );
          argvv.push_back( arg ); 
        }

        CSimpleOpt args( argc, &argvv[0], &simpleopt_options[0] );

        while ( args.Next() )
        {
          if ( args.LastError() == SO_SUCCESS )
          {
            switch ( args.OptionId() )
            {
              case OPTION_LOG_LEVEL:
              {
                uint8_t log_level_uint8 = static_cast<uint8_t>( atoi( args.OptionArg() ) );
                if ( log_level_uint8 == 0 )
                {
                  std::string arg( args.OptionArg() );
                  if ( arg == "LOG_EMERG" || arg == "EMERG" || arg == "EMERGENCY" || arg == "FATAL" || arg == "FAIL" )
                    log_level = Log::LOG_EMERG;
                  else if ( arg == "LOG_ALERT" || arg == "ALERT" )
                    log_level = Log::LOG_ALERT;
                  else if ( arg == "LOG_CRIT" || arg == "CRIT" || arg == "CRITICAL" )
                    log_level = Log::LOG_CRIT;
                  else if ( arg == "LOG_ERR" || arg == "ERR" || arg == "ERROR" )
                    log_level = Log::LOG_ERR;
                  else if ( arg == "LOG_WARNING" || arg == "WARNING" || arg == "WARN" )
                    log_level = Log::LOG_WARNING;
                  else if ( arg == "LOG_NOTICE" || arg == "NOTICE" )
                    log_level = Log::LOG_NOTICE;
                  else if ( arg == "LOG_INFO" || arg == "INFO" )
                    log_level = Log::LOG_INFO;
                  else if ( arg == "LOG_DEBUG" || arg == "DEBUG" || arg == "TRACE" )
                    log_level = Log::LOG_DEBUG;
                  else
                    log_level = Log::LOG_EMERG;
                }
                else if ( log_level_uint8 <= Log::LOG_DEBUG )
                  log_level = static_cast<Log::Level>( log_level_uint8 );
                else
                  log_level = Log::LOG_DEBUG;
              }
              break;

              case OPTION_HELP:
              {
                printUsage();
                return 0;
              }
              break;
            }

            parseOption( args.OptionId(), args.OptionArg() );
          }
        }

        parseFiles( args.FileCount(), args.Files() );

        for ( std::vector<char*>::iterator arg_i = argvv.begin(); arg_i != argvv.end(); arg_i++ )
          delete [] *arg_i;
        argvv.clear();

        // Replace argv[0] with the absolute path to the executable
#if defined(_WIN32)
        char argv0[PATH_MAX];
        if ( GetModuleFileNameA( NULL, argv0, PATH_MAX ) )
          argvv.push_back( argv0 );
        else
          argvv.push_back( argv[0] );
#elif defined(__linux__)
        char argv0[PATH_MAX];
        int ret;
        if ( ( ret = readlink( "/proc/self/exe", argv0, PATH_MAX ) ) != -1 )
        {
          argv0[ret] = 0;
          argvv.push_back( argv0 );
        }
        else
          argvv.push_back( argv[0] );
#elif defined(__MACH__)
        char argv0[PATH_MAX];
        uint32_t bufsize = PATH_MAX;
        if ( _NSGetExecutablePath( argv0, &bufsize ) == 0 )
        {
          argv0[bufsize] = 0;

          char linked_argv0[PATH_MAX]; int ret;
          if ( ( ret = readlink( argv0, linked_argv0, PATH_MAX ) ) != -1 )
          {
            linked_argv0[ret] = 0;
            argvv.push_back( linked_argv0 );
          }
          else
          {
            char absolute_argv0[PATH_MAX];
            if ( realpath( argv0, absolute_argv0 ) != NULL )
              argvv.push_back( absolute_argv0 );
            else
              argvv.push_back( argv0 );
          }
        }
        else
          argvv.push_back( argv[0] );
#elif defined(__sun)
        argvv.push_back( const_cast<char*>( getexecname() ) );
#else
        argvv.push_back( argv[0] );
#endif

        for ( int arg_i = 1; arg_i < argc; arg_i++ )
          argvv.push_back( argv[arg_i] );

        // Pass the original argv to _main instead of the copies SimpleOpt punched holes in
        return _main( static_cast<int>( argvv.size() ), &argvv[0] );
      }
      catch ( YIELD::Exception& exc ) // Don't catch std::exceptions like bad_alloc
      {
        std::cerr << exc.what() << std::endl;
        return 1;
      }
    }

  protected:
    enum
    {
      OPTION_LOG_LEVEL = 1,
      OPTION_HELP = 2,
    };


    Main( const char* program_name, const char* program_description = NULL, const char* files_usage = NULL )
      : program_name( program_name ), program_description( program_description ), files_usage( files_usage )
    {
      std::ostringstream log_level_default_ss;
      log_level_default_ss << static_cast<uint16_t>( 0 );
      log_level_default_str = log_level_default_ss.str();
      addOption( OPTION_LOG_LEVEL, "-d", "--debug", log_level_default_str.c_str() );
      log_level = Log::LOG_EMERG;

      addOption( OPTION_HELP, "-h", "--help" );
    }

    virtual ~Main() { }

    void addOption( int id, const char* short_arg, const char* long_arg = NULL, const char* default_values = NULL )
    {
      options.push_back( Option( id, short_arg, long_arg, default_values ) );
    }

    void pause()
    {
#ifdef _WIN32
      SetConsoleCtrlHandler( ConsoleCtrlHandler, TRUE );
      pause_semaphore.acquire();
#else
      ::pause();
#endif
    }

    void printUsage()
    {
      std::cout << std::endl;
      std::cout << program_name;
      if ( program_description )
        std::cout << ": " << program_description;
      std::cout << std::endl;
      std::cout << std::endl;
      std::cout << "Usage:" << std::endl;
      std::cout << "  " << program_name << " [options]";
      if ( files_usage )
        std::cout << " " << files_usage;
      std::cout << std::endl;
      std::cout << std::endl;
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

    // Methods for subclasses to override
    virtual int _main( int argc, char** argv ) = 0;
    virtual void parseOption( int, char* ) { }
    virtual void parseFiles( int, char** ) { }

  private:
    const char *program_name, *program_description, *files_usage;

    // Built-in options
    Log::Level log_level;std::string log_level_default_str;


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
        return std::strcmp( get_short_arg(), other.get_short_arg() ) < 0;
      }

    private:
      int id;
      const char *short_arg, *long_arg, *default_values;
    };

    std::vector<Option> options;

#ifdef _WIN32
    static CountingSemaphore pause_semaphore;

    static BOOL WINAPI ConsoleCtrlHandler( DWORD fdwCtrlType )
    {
      if ( fdwCtrlType == CTRL_C_EVENT )
      {
        pause_semaphore.release();
        return TRUE;
      }
      else
        return FALSE;
    }
#endif
  };


  class ServerMain : public Main
  {
  protected:
    ServerMain( const char* program_name, const char* program_description = NULL, const char* files_usage = NULL )
      : Main( program_name, program_description, files_usage )
    {
      addOption( SERVER_OPTION_LIMIT_PHYSICAL_PROCESSOR_MASK, "--limit-physical-processor-mask", NULL, "n" );

      addOption( SERVER_OPTION_PORT, "-p", "--port", "n" );
	    port = 27095;

      addOption( SERVER_OPTION_STAGE_GROUP_REPLICATION, "--stage-group-replication", NULL, "n" );
      stage_group_replication = 1;

      addOption( SERVER_OPTION_STAGE_GROUP_TYPE, "--scheduler", "--stage-group-type", "seda" );      

      addOption( SERVER_OPTION_THREADS, "-t", "--threads", "n" );

      addOption( SERVER_OPTION_THREADS_PER_PHYSICAL_PROCESSOR, "--threads-per-physical-processor", NULL, "n" );
      threads_per_physical_processor = 0;
    }

    virtual ~ServerMain()
    {
      for ( std::vector<StageGroup*>::iterator stage_group_i = stage_groups.begin(); stage_group_i != stage_groups.end(); stage_group_i++ )
        Object::decRef( *stage_group_i );
    }

    unsigned short get_port() const { return port; }

    const std::vector<StageGroup*>& get_stage_groups() 
    { 
      if ( stage_groups.empty() )
      {
        for ( uint8_t stage_group_i = 0; stage_group_i < stage_group_replication; stage_group_i++ )
        {
          auto_ProcessorSet stage_group_limit_physical_processor_set( limit_physical_processor_set );
          if ( stage_group_limit_physical_processor_set == NULL )
          {
            stage_group_limit_physical_processor_set = new ProcessorSet; 
            stage_group_limit_physical_processor_set->set( stage_group_i );
          }

          stage_groups.push_back( new SEDAStageGroup( stage_group_limit_physical_processor_set ) );
        }
      }

      return stage_groups; 
    }

    const std::vector<int16_t>& get_threads() const { return threads; }

    void parseOption( int id, char* arg )
    {
      switch ( id )
      {
			  case SERVER_OPTION_LIMIT_PHYSICAL_PROCESSOR_MASK:
			  {
				  uint32_t limit_physical_processor_mask = atoi( arg );
          if ( limit_physical_processor_mask != 0 )
            limit_physical_processor_set = new ProcessorSet( limit_physical_processor_mask );

          std::cout << get_program_name() << ": limiting physical processors to mask: " << limit_physical_processor_mask << std::endl;
			  }
			  break;

  			case SERVER_OPTION_PORT:
        {
          port = static_cast<uint16_t>( atoi( arg ) ); 
        }
        break;

        case SERVER_OPTION_STAGE_GROUP_REPLICATION:
        {
          stage_group_replication = static_cast<uint8_t>( atoi( arg ) );
        }
        break;

        case SERVER_OPTION_STAGE_GROUP_TYPE:
        {
			    if ( strcmp( arg, "cohort" ) == 0 )
		        std::cout << get_program_name() << ": main stage group = CohortStageGroup" << std::endl;
			    else if ( strcmp( arg, "mg1" ) == 0 )
		        std::cout << get_program_name() << ": main stage group = MG1StageGroup" << std::endl;
			    else if ( strcmp( arg, "seda" ) == 0 )
		        std::cout << get_program_name() << ": main stage group = SEDAStageGroup" << std::endl;
			    else if ( strcmp( arg, "srpt" ) == 0 )
            std::cout << get_program_name() << ": main stage group = SRPTStageGroup" << std::endl;

          for ( uint8_t stage_group_i = 0; stage_group_i < stage_group_replication; stage_group_i++ )
          {
            auto_ProcessorSet stage_group_limit_physical_processor_set( limit_physical_processor_set );
            if ( stage_group_limit_physical_processor_set == NULL )
            {
              stage_group_limit_physical_processor_set = new ProcessorSet; 
              stage_group_limit_physical_processor_set->set( stage_group_i );
            }

            if ( strcmp( arg, "cohort" ) == 0 )
              stage_groups.push_back( new CohortStageGroup( get_program_name(), stage_group_limit_physical_processor_set ) );
            else if ( strcmp( arg, "mg1" ) == 0 )
              stage_groups.push_back( new MG1StageGroup( get_program_name(), stage_group_limit_physical_processor_set ) );
            else if ( strcmp( arg, "seda" ) == 0 )
              stage_groups.push_back( new SEDAStageGroup( stage_group_limit_physical_processor_set ) );
            else if ( strcmp( arg, "srpt" ) == 0 )
              stage_groups.push_back( new SRPTStageGroup( get_program_name(), stage_group_limit_physical_processor_set ) );
          }
        }
        break;

			  case SERVER_OPTION_THREADS:
			  {	
          std::cout << get_program_name() << ": threads string: " << arg << std::endl;
				  const char* thread_p = arg;
				  while ( *thread_p != 0 )
				  {
					  if ( isdigit( *thread_p ) )
						  threads.push_back( *thread_p - '0' );
					  else if ( *thread_p == 'p' )
						  threads.push_back( -1 );
					  else
						  threads.push_back( 0 );

					  thread_p++;
				  }
        }
        break;

			  case SERVER_OPTION_THREADS_PER_PHYSICAL_PROCESSOR: 
			  {
				  if ( strcmp( arg, "p" ) == 0 )
					  threads_per_physical_processor = -1;
				  else
					  threads_per_physical_processor = static_cast<int16_t>( atoi( arg ) );

          std::cout << get_program_name() << ": threads per physical processor: " << threads_per_physical_processor << std::endl;
			  }
			  break;

        default: Main::parseOption( id, arg ); break;
      }
    }

  private:
    enum
    {
      SERVER_OPTION_LIMIT_PHYSICAL_PROCESSOR_MASK = 10,
      SERVER_OPTION_PORT = 11,
      SERVER_OPTION_STAGE_GROUP_REPLICATION = 12,
      SERVER_OPTION_STAGE_GROUP_TYPE = 13,
      SERVER_OPTION_THREADS = 14,
      SERVER_OPTION_THREADS_PER_PHYSICAL_PROCESSOR = 15,
    };

    auto_ProcessorSet limit_physical_processor_set;
  	uint16_t port;
    uint8_t stage_group_replication;
    std::vector<StageGroup*> stage_groups;
    std::vector<int16_t> threads;
	  int16_t threads_per_physical_processor;
  };
};

#endif
