// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_MAIN_H_
#define _XTREEMFS_MAIN_H_

#include "xtreemfs.h"

#include "yield/main.h"

#if defined(_WIN32)
#include "client/windows/handler/exception_handler.h"
#define XTREEMFS_HAVE_GOOGLE_BREAKPAD 1
//#ifdef _DEBUG
//#include <vld.h>
//#endif
#elif defined(__linux) && !defined(__x86_64__)
#include "client/linux/handler/exception_handler.h"
#define XTREEMFS_HAVE_GOOGLE_BREAKPAD 1
#endif


namespace xtreemfs
{
  class Main : public YIELD::Main
  {
  public:
    virtual int main( int argc, char** argv )
    {
      if ( argc == 1 )
      {
        printUsage();
        return 0;
      }
      else
      {
        YIELD::ipc::Socket::init();

#ifdef XTREEMFS_HAVE_GOOGLE_BREAKPAD
        google_breakpad::ExceptionHandler* exception_handler;
        void* MinidumpCallback_context = this;
#if defined(_WIN32)
//            if ( !IsDebuggerPresent() )            {
          exception_handler = 
            new google_breakpad::ExceptionHandler
            ( 
              YIELD::platform::Path( "." ) + PATH_SEPARATOR_STRING, 
              NULL, 
              MinidumpCallback, 
              MinidumpCallback_context, 
              google_breakpad::ExceptionHandler::HANDLER_ALL 
            );
#elif defined(__linux)
        exception_handler = 
          new google_breakpad::ExceptionHandler
          ( 
            YIELD::platform::Path( "." ) + PATH_SEPARATOR_STRING, 
            NULL, 
            MinidumpCallback, 
            MinidumpCallback_context, 
            true 
          );
#else
#error
#endif
#endif
        int ret = YIELD::Main::main( argc, argv );

        // YIELD::ipc::Socket::destroy();

        return ret;
      }
    }

  protected:
    enum
    {
      OPTION_LOG_FILE_PATH = 1,
      OPTION_LOG_LEVEL = 2,
      OPTION_OPERATION_TIMEOUT_MS = 3,
      OPTION_PKCS12_FILE_PATH = 4,
      OPTION_PKCS12_PASSPHRASE = 5,
      OPTION_PEM_CERTIFICATE_FILE_PATH = 6,
      OPTION_PEM_PRIVATE_KEY_FILE_PATH = 7,
      OPTION_PEM_PRIVATE_KEY_PASSPHRASE = 8,
      OPTION_TRACE_AUTH = 9,
      OPTION_TRACE_NETWORK_IO = 10,
      OPTION_TRACE_NETWORK_OPERATIONS = 11
    };


    Main
    ( 
      const char* program_name, 
      const char* program_description, 
      const char* files_usage = NULL 
    )
      : YIELD::Main( program_name, program_description, files_usage )
    {
      addOption( OPTION_LOG_FILE_PATH, "-l", "--log-file-path", "<path>" );

      addOption( OPTION_LOG_LEVEL, "-d", "--log-level", "EMERG|ALERT|CRIT|ERR|WARNING|NOTICE|INFO|DEBUG" );
      log_level = YIELD::platform::Log::LOG_WARNING;

      addOption( OPTION_OPERATION_TIMEOUT_MS, "-t", "--operation-timeout-ms", "n" );
      operation_timeout = static_cast<uint64_t>( DIRProxy::OPERATION_TIMEOUT_DEFAULT );

#ifdef YIELD_HAVE_OPENSSL
      addOption( OPTION_PEM_CERTIFICATE_FILE_PATH, "--cert", "--pem-certificate-file-path", "PEM certificate file path" );
      addOption( OPTION_PEM_PRIVATE_KEY_FILE_PATH, "--pkey", "--pem-private-key-file-path", "PEM private key file path" );
      addOption( OPTION_PEM_PRIVATE_KEY_PASSPHRASE, "--pass", "--pem-private-key-passphrase", "PEM private key passphrase" );
      addOption( OPTION_PKCS12_FILE_PATH, "--pkcs12-file-path", NULL, "PKCS#12 file path" );
      addOption( OPTION_PKCS12_PASSPHRASE, "--pkcs12-passphrase", NULL, "PKCS#12 passphrase" );
#endif

      addOption( OPTION_TRACE_AUTH, "--trace-auth" );
      trace_auth = false;

      addOption( OPTION_TRACE_NETWORK_IO, "--trace-network-io" );
      trace_network_io = false;

      addOption( OPTION_TRACE_NETWORK_OPERATIONS, "--trace-network-operations" );
      trace_network_operations = false;
    }

    virtual ~Main()
    { }

    auto_DIRProxy createDIRProxy( const YIELD::ipc::URI& absolute_uri )
    {       
      return DIRProxy::create
      ( 
        absolute_uri, 
        DIRProxy::CONCURRENCY_LEVEL_DEFAULT,
        get_proxy_flags(), 
        get_log(), 
        operation_timeout, 
        DIRProxy::RECONNECT_TRIES_MAX_DEFAULT, 
        get_proxy_ssl_context() 
      );
    }    

    auto_MRCProxy createMRCProxy( const YIELD::ipc::URI& absolute_uri, const char* password = "" )
    {
      return MRCProxy::create
      ( 
        absolute_uri, 
        MRCProxy::CONCURRENCY_LEVEL_DEFAULT,
        get_proxy_flags(), 
        get_log(), 
        operation_timeout, 
        password, 
        MRCProxy::RECONNECT_TRIES_MAX_DEFAULT, 
        get_proxy_ssl_context() 
      );
    }

    YIELD::platform::auto_Log get_log()
    {
      if ( log == NULL )
      {
        if ( log_file_path.empty() || log_file_path == "-" )
          log = YIELD::platform::Log::open( std::cerr, get_log_level() );
        else 
          log = YIELD::platform::Log::open( log_file_path, get_log_level(), true ); // true = lazy open
      }

      return log;
    }

    const std::string& get_log_file_path() const 
    {
      return log_file_path;
    }

    YIELD::platform::Log::Level get_log_level() const 
    {
      return log_level;
    }

    const YIELD::platform::Time& get_operation_timeout() const
    {
      return operation_timeout;
    }

    YIELD::ipc::auto_SSLContext get_proxy_ssl_context()
    {
      if ( ssl_context == NULL )
      {
#ifdef YIELD_HAVE_OPENSSL
        if ( !pkcs12_file_path.empty() )
          ssl_context = YIELD::ipc::SSLContext::create( SSLv3_client_method(), pkcs12_file_path, pkcs12_passphrase );
        else if ( !pem_certificate_file_path.empty() && !pem_private_key_file_path.empty() )
          ssl_context = YIELD::ipc::SSLContext::create( SSLv3_client_method(), pem_certificate_file_path, pem_private_key_file_path, pem_private_key_passphrase );
#else
        ssl_context = YIELD::ipc::SSLContext::create();
#endif
      }

      return ssl_context;
    }

    uint32_t get_proxy_flags() const
    {
      uint32_t proxy_flags = 0;

      if ( trace_auth )
        proxy_flags |= DIRProxy::PROXY_FLAG_TRACE_AUTH;
      if ( trace_network_io )
        proxy_flags |= DIRProxy::PROXY_FLAG_TRACE_IO;
      if ( trace_network_operations )
        proxy_flags |= DIRProxy::PROXY_FLAG_TRACE_OPERATIONS;

      return proxy_flags;
    }

    YIELD::ipc::auto_URI parseURI( const char* uri_c_str )
    {
      std::string uri_str( uri_c_str );
      if ( uri_str.find( "://" ) == std::string::npos )
      {
        if ( !pkcs12_file_path.empty() || ( !pem_certificate_file_path.empty() && !pem_private_key_file_path.empty() ) )
          uri_str = org::xtreemfs::interfaces::ONCRPCS_SCHEME + std::string( "://" ) + uri_str;
        else
          uri_str = org::xtreemfs::interfaces::ONCRPC_SCHEME + std::string( "://" ) + uri_str;
      }

      return YIELD::ipc::auto_URI( new YIELD::ipc::URI( uri_str ) );
    }

    YIELD::ipc::auto_URI parseVolumeURI( const char* volume_uri_c_str, std::string& volume_name )
    {
      YIELD::ipc::auto_URI volume_uri = parseURI( volume_uri_c_str );
      if ( volume_uri->get_resource().size() > 1 )
      {
        volume_name = volume_uri->get_resource().c_str() + 1;
        return volume_uri;
      }
      else
        throw YIELD::platform::Exception( "invalid volume URI" );
    }

    // YIELD::Main
    virtual void parseOption( int id, char* arg )
    {
      switch ( id )
      {
        case OPTION_LOG_FILE_PATH: log_file_path = arg; break;

        case OPTION_LOG_LEVEL:
        {
          uint8_t log_level_uint8 = static_cast<uint8_t>( atoi( arg ) );
          if ( log_level_uint8 == 0 )
          {
            if ( strcmp( arg, "LOG_EMERG" ) == 0 || strcmp( arg, "EMERG" ) == 0 || strcmp( arg, "EMERGENCY" ) == 0 || strcmp( arg, "FATAL" ) == 0 || strcmp( arg, "FAIL" ) == 0 )
              log_level = YIELD::platform::Log::LOG_EMERG;
            else if ( strcmp( arg, "LOG_ALERT" ) == 0 || strcmp( arg, "ALERT" ) == 0 )
              log_level = YIELD::platform::Log::LOG_ALERT;
            else if ( strcmp( arg, "LOG_CRIT" ) == 0 || strcmp( arg, "CRIT" ) == 0 || strcmp( arg, "CRITICAL" ) == 0 )
              log_level = YIELD::platform::Log::LOG_CRIT;
            else if ( strcmp( arg, "LOG_ERR" ) == 0 || strcmp( arg, "ERR" ) == 0 || strcmp( arg, "ERROR" ) == 0 )
              log_level = YIELD::platform::Log::LOG_ERR;
            else if ( strcmp( arg, "LOG_WARNING" ) == 0 || strcmp( arg, "WARNING" ) == 0 || strcmp( arg, "WARN" ) == 0 )
              log_level = YIELD::platform::Log::LOG_WARNING;
            else if ( strcmp( arg, "LOG_NOTICE" ) == 0 || strcmp( arg, "NOTICE" ) == 0 )
              log_level = YIELD::platform::Log::LOG_NOTICE;
            else if ( strcmp( arg, "LOG_INFO" ) == 0 || strcmp( arg, "INFO" ) == 0 )
              log_level = YIELD::platform::Log::LOG_INFO;
            else if ( strcmp( arg, "LOG_DEBUG" ) == 0 || strcmp( arg, "DEBUG" ) == 0 || strcmp( arg, "TRACE" ) == 0 )
              log_level = YIELD::platform::Log::LOG_DEBUG;
            else
              log_level = YIELD::platform::Log::LOG_EMERG;
          }
          else if ( log_level_uint8 <= YIELD::platform::Log::LOG_DEBUG )
            log_level = static_cast<YIELD::platform::Log::Level>( log_level_uint8 );
          else
            log_level = YIELD::platform::Log::LOG_DEBUG;
        }
        break;

        case OPTION_OPERATION_TIMEOUT_MS:
        {
          double operation_timeout_ms = atof( arg );
          if ( operation_timeout_ms != 0 )
            operation_timeout = YIELD::platform::Time( static_cast<uint64_t>( operation_timeout_ms * NS_IN_MS ) );
        }
        break;

        case OPTION_PEM_CERTIFICATE_FILE_PATH: pem_certificate_file_path = arg; break;
        case OPTION_PEM_PRIVATE_KEY_FILE_PATH: pem_private_key_file_path = arg; break;
        case OPTION_PEM_PRIVATE_KEY_PASSPHRASE: pem_private_key_passphrase = arg; break;
        case OPTION_PKCS12_FILE_PATH: pkcs12_file_path = arg; break;
        case OPTION_PKCS12_PASSPHRASE: pkcs12_passphrase = arg; break;
        case OPTION_TRACE_AUTH: trace_auth = true; break;
        case OPTION_TRACE_NETWORK_IO: trace_network_io = true; break;
        case OPTION_TRACE_NETWORK_OPERATIONS: trace_network_operations = true; break;

        default: YIELD::Main::parseOption( id, arg ); break;
      }
    }

  private:
    std::string log_file_path;
    YIELD::platform::Log::Level log_level;
    std::string log_level_default_str;
    YIELD::platform::Time operation_timeout;
    std::string pem_certificate_file_path, pem_private_key_file_path, pem_private_key_passphrase;
    std::string pkcs12_file_path, pkcs12_passphrase;
    bool trace_auth, trace_network_io, trace_network_operations;

    YIELD::platform::auto_Log log;
    YIELD::ipc::auto_SSLContext ssl_context;

#ifdef XTREEMFS_HAVE_GOOGLE_BREAKPAD
#if defined(_WIN32)
    static bool MinidumpCallback( const wchar_t* dump_path, const wchar_t* minidump_id, void* context, EXCEPTION_POINTERS*, MDRawAssertionInfo*, bool succeeded )
    {
      return static_cast<Main*>( context )->MinidumpCallback( dump_path, minidump_id, succeeded );
    }
#elif defined(__linux)
    static bool MinidumpCallback( const char *dump_path, const char *minidump_id, void* context, bool succeeded )
    {
      return static_cast<Main*>( context )->MinidumpCallback( dump_path, minidump_id, succeeded );
    }
#else
#error
#endif
    bool MinidumpCallback( const YIELD::platform::Path& dump_path, const YIELD::platform::Path& minidump_id, bool succeeded )
    {
      if ( succeeded )
      {
        YIELD::platform::Path dump_file_name( minidump_id ); dump_file_name = static_cast<const std::string&>( dump_file_name ) + ".dmp";
        YIELD::platform::Path dump_file_path( dump_path ); dump_file_path = dump_file_path + dump_file_name;
        std::string dump_absolute_uri( "http://www.xtreemfs.org/dump/dump.php?f=" );
        dump_absolute_uri.append( static_cast<const std::string&>( dump_file_name ) );

        get_log()->getStream( YIELD::platform::Log::LOG_EMERG ) << get_program_name() << ": crashed on unknown exception, dumping to " << dump_file_path << " and trying to send to " << dump_absolute_uri;

        try
        {
          YIELD::ipc::HTTPClient::PUT( dump_absolute_uri, dump_file_path, get_log() );
        }
        catch ( std::exception& exc )
        {
          get_log()->getStream( YIELD::platform::Log::LOG_EMERG ) << get_program_name() << ": exception trying to send dump to the server: " << exc.what();
        }
      }

      return succeeded;
    }
#endif
  };
};

#endif
