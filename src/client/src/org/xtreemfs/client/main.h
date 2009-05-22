// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef CLIENT_SRC_ORG_XTREEMFS_CLIENT_MAIN_H
#define CLIENT_SRC_ORG_XTREEMFS_CLIENT_MAIN_H

#include "yield/main.h"

#include "org/xtreemfs/client.h"

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "org/xtreemfs/interfaces/dir_interface.h"
#include "org/xtreemfs/interfaces/mrc_interface.h"
#include "org/xtreemfs/interfaces/osd_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif

#if defined(_WIN32)
#include "client/windows/handler/exception_handler.h"
#define ORG_XTREEMFS_CLIENT_HAVE_GOOGLE_BREAKPAD 1
//#ifdef _DEBUG
//#include <vld.h>
//#endif
#elif defined(__linux) && !defined(__x86_64__)
#include "client/linux/handler/exception_handler.h"
#define ORG_XTREEMFS_CLIENT_HAVE_GOOGLE_BREAKPAD 1
#endif


namespace org
{
  namespace xtreemfs
  {
    namespace client
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
#ifdef ORG_XTREEMFS_CLIENT_HAVE_GOOGLE_BREAKPAD
            google_breakpad::ExceptionHandler* exception_handler;
            void* MinidumpCallback_context = this;
#if defined(_WIN32)
//            if ( !IsDebuggerPresent() )            {
              exception_handler = new google_breakpad::ExceptionHandler( YIELD::Path( "." ) + PATH_SEPARATOR_STRING, NULL, MinidumpCallback, MinidumpCallback_context, google_breakpad::ExceptionHandler::HANDLER_ALL );
#elif defined(__linux)
            exception_handler = new google_breakpad::ExceptionHandler( YIELD::Path( "." ) + PATH_SEPARATOR_STRING, NULL, MinidumpCallback, MinidumpCallback_context, true );
#else
#error
#endif
#endif
            return YIELD::Main::main( argc, argv );
          }
        }

      protected:
        enum
        {
          OPTION_PKCS12_FILE_PATH = 3,
          OPTION_PKCS12_PASSPHRASE = 4,
          OPTION_PEM_CERTIFICATE_FILE_PATH = 5,
          OPTION_PEM_PRIVATE_KEY_FILE_PATH = 6,
          OPTION_PEM_PRIVATE_KEY_PASSPHRASE = 7,
          OPTION_TIMEOUT_MS = 8
        };


        Main( const char* program_name, const char* program_description, const char* files_usage = NULL )
          : YIELD::Main( program_name, program_description, files_usage )
        {
          stage_group = new YIELD::SEDAStageGroup( "XtreemFS StageGroup" );

          addOption( OPTION_PEM_CERTIFICATE_FILE_PATH, "--cert", "--pem-certificate-file-path", "PEM certificate file path" );
          addOption( OPTION_PEM_PRIVATE_KEY_FILE_PATH, "--pkey", "--pem-private-key-file-path", "PEM private key file path" );
          addOption( OPTION_PEM_PRIVATE_KEY_PASSPHRASE, "--pass", "--pem-private-key-passphrase", "PEM private key passphrase" );
          addOption( OPTION_PKCS12_FILE_PATH, "--pkcs12-file-path", NULL, "PKCS#12 file path" );
          addOption( OPTION_PKCS12_PASSPHRASE, "--pkcs12-passphrase", NULL, "PKCS#12 passphrase" );

          addOption( OPTION_TIMEOUT_MS, "-t", "--timeout-ms", "n" );
          operation_timeout = static_cast<uint64_t>( 5 * NS_IN_S );
        }

        virtual ~Main()
        { }

        YIELD::auto_Object<DIRProxy> createDIRProxy( const YIELD::URI& uri )
        {
          return createProxy<DIRProxy>( uri, org::xtreemfs::interfaces::DIRInterface::DEFAULT_ONCRPC_PORT );
        }

        YIELD::auto_Object<MRCProxy> createMRCProxy( const YIELD::URI& uri )
        {
          return createProxy<MRCProxy>( uri, org::xtreemfs::interfaces::MRCInterface::DEFAULT_ONCRPC_PORT );
        }

        YIELD::auto_Object<OSDProxy> createOSDProxy( const YIELD::URI& uri )
        {
          return createProxy<OSDProxy>( uri, org::xtreemfs::interfaces::MRCInterface::DEFAULT_ONCRPC_PORT );
        }

        YIELD::auto_Object<YIELD::Log> get_log()
        {
          if ( log == NULL )
            log = new YIELD::Log( std::cout, get_log_level() );
          return log;
        }

        YIELD::auto_Object<YIELD::SocketFactory> get_socket_factory()
        {
          if ( socket_factory == NULL )
          {
#ifdef YIELD_HAVE_OPENSSL
            if ( !pkcs12_file_path.empty() )
              socket_factory = new YIELD::SSLSocketFactory( new YIELD::SSLContext( SSLv3_client_method(), pkcs12_file_path, pkcs12_passphrase ), get_log() );
            else if ( !pem_certificate_file_path.empty() && !pem_private_key_file_path.empty() )
              socket_factory = new YIELD::SSLSocketFactory( new YIELD::SSLContext( SSLv3_client_method(), pem_certificate_file_path, pem_private_key_file_path, pem_private_key_passphrase ), get_log() );
            else
#endif
              socket_factory = new YIELD::TCPSocketFactory( get_log() );
          }

          return socket_factory;
        }

        YIELD::auto_Object<YIELD::URI> parseURI( const char* uri_c_str )
        {
          std::string uri_str( uri_c_str );
          if ( uri_str.find( "://" ) == std::string::npos )
          {
            if ( !pkcs12_file_path.empty() || ( !pem_certificate_file_path.empty() && !pem_private_key_file_path.empty() ) )
              uri_str = org::xtreemfs::interfaces::ONCRPCS_SCHEME + std::string( "://" ) + uri_str;
            else
              uri_str = org::xtreemfs::interfaces::ONCRPC_SCHEME + std::string( "://" ) + uri_str;
          }

          return YIELD::auto_Object<YIELD::URI>( new YIELD::URI( uri_str ) );
        }

        YIELD::auto_Object<YIELD::URI> parseVolumeURI( const char* volume_uri_c_str, std::string& volume_name )
        {
          YIELD::auto_Object<YIELD::URI> volume_uri = parseURI( volume_uri_c_str );
          if ( volume_uri->get_resource().size() > 1 )
          {
            volume_name = volume_uri->get_resource().c_str() + 1;
            return volume_uri;
          }
          else
            throw YIELD::Exception( "invalid volume URI" );
        }

        // YIELD::Main
        virtual void parseOption( int id, char* arg )
        {
          switch ( id )
          {
            case OPTION_PEM_CERTIFICATE_FILE_PATH: pem_certificate_file_path = arg; break;
            case OPTION_PEM_PRIVATE_KEY_FILE_PATH: pem_private_key_file_path = arg; break;
            case OPTION_PEM_PRIVATE_KEY_PASSPHRASE: pem_private_key_passphrase = arg; break;
            case OPTION_PKCS12_FILE_PATH: pkcs12_file_path = arg; break;
            case OPTION_PKCS12_PASSPHRASE: pkcs12_passphrase = arg; break;

            case OPTION_TIMEOUT_MS:
            {
              double timeout_ms = atof( arg );
              if ( timeout_ms != 0 )
                operation_timeout = YIELD::Time( static_cast<uint64_t>( timeout_ms * NS_IN_MS ) );
            }
            break;

            default: YIELD::Main::parseOption( id, arg ); break;
          }
        }

      private:
        std::string pem_certificate_file_path, pem_private_key_file_path, pem_private_key_passphrase;
        std::string pkcs12_file_path, pkcs12_passphrase;
        YIELD::Time operation_timeout;

        YIELD::auto_Object<YIELD::Log> log;
        YIELD::auto_Object<YIELD::SocketFactory> socket_factory;
        YIELD::auto_Object<YIELD::StageGroup> stage_group;


        template <class ProxyType>
        YIELD::auto_Object<ProxyType> createProxy( const YIELD::URI& uri, uint16_t default_port )
        {
          YIELD::URI checked_uri( uri );
          if ( checked_uri.get_port() == 0 )
            checked_uri.set_port( default_port );          
          YIELD::auto_Object<ProxyType> proxy = ProxyType::create( checked_uri, get_socket_factory(), stage_group, get_log(), operation_timeout, 3 );
          return proxy;
        }

#ifdef ORG_XTREEMFS_CLIENT_HAVE_GOOGLE_BREAKPAD
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
        bool MinidumpCallback( const YIELD::Path& dump_path, const YIELD::Path& minidump_id, bool succeeded )
        {
          if ( succeeded )
          {
            YIELD::Path dump_file_name( minidump_id ); dump_file_name = static_cast<const std::string&>( dump_file_name ) + ".dmp";
            YIELD::Path dump_file_path( dump_path ); dump_file_path = dump_file_path + dump_file_name;
            std::string dump_absolute_uri( "http://www.xtreemfs.org/dump/dump.php?f=" );
            dump_absolute_uri.append( static_cast<const std::string&>( dump_file_name ) );

            get_log()->getStream( YIELD::Log::LOG_EMERG ) << get_program_name() << ": crashed on unknown exception, dumping to " << dump_file_path << " and trying to send to " << dump_absolute_uri;

            try
            {
              YIELD::HTTPClient::PUT( dump_absolute_uri, dump_file_path, get_log() );
            }
            catch ( std::exception& exc )
            {
              get_log()->getStream( YIELD::Log::LOG_EMERG ) << get_program_name() << ": exception trying to send dump to the server: " << exc.what();
            }
          }

          return succeeded;
        }
#endif
      };
    };
  };
};

#endif
