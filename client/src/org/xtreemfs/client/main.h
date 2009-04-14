// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ECTS_XTREEMFS_CLIENT_SRC_ORG_XTREEMFS_CLIENT_BIN_OPTIONS_H
#define ECTS_XTREEMFS_CLIENT_SRC_ORG_XTREEMFS_CLIENT_BIN_OPTIONS_H

#include "yield/main.h"

#include "org/xtreemfs/interfaces/constants.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class Main : public YIELD::Main
      {
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
          addOption( OPTION_PEM_CERTIFICATE_FILE_PATH, "--cert", "--pem-certificate-file-path", "PEM certificate file path" );
          addOption( OPTION_PEM_PRIVATE_KEY_FILE_PATH, "--pkey", "--pem-private-key-file-path", "PEM private key file path" );
          addOption( OPTION_PEM_PRIVATE_KEY_PASSPHRASE, "--pass", "--pem-private-key-passphrase", "PEM private key passphrase" );
          addOption( OPTION_PKCS12_FILE_PATH, "--pkcs12-file-path", NULL, "PKCS#12 file path" );
          addOption( OPTION_PKCS12_PASSPHRASE, "--pkcs12-passphrase", NULL, "PKCS#12 passphrase" );
          ssl_context = NULL;

          addOption( OPTION_TIMEOUT_MS, "-t", "--timeout-ms", "n" );
          timeout_ms = Proxy::PROXY_DEFAULT_OPERATION_TIMEOUT_MS;
        }

        virtual ~Main()
        {
          delete ssl_context;
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

        uint64_t get_timeout_ms() const { return timeout_ms; }

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

        // YIELD::Main
        virtual void parseOption( int id, char* arg )
        {
          switch ( id )
          {
            case OPTION_DEBUG_LEVEL:
            {
              if ( get_debug_level() >= org::xtreemfs::interfaces::DEBUG_LEVEL_TRACE )
                YIELD::TCPSocket::set_trace_socket_io_onoff( true );
            }
            break;

            case OPTION_PEM_CERTIFICATE_FILE_PATH: pem_certificate_file_path = arg; break;                  
            case OPTION_PEM_PRIVATE_KEY_FILE_PATH: pem_private_key_file_path = arg; break;
            case OPTION_PEM_PRIVATE_KEY_PASSPHRASE: pem_private_key_passphrase = arg; break;
            case OPTION_PKCS12_FILE_PATH: pkcs12_file_path = arg; break;
            case OPTION_PKCS12_PASSPHRASE: pkcs12_passphrase = arg; break;

            case OPTION_TIMEOUT_MS:
            {
              timeout_ms = atol( arg );
              if ( timeout_ms == 0 )
                timeout_ms = Proxy::PROXY_DEFAULT_OPERATION_TIMEOUT_MS;
            }
            break;

            default: YIELD::Main::parseOption( id, arg ); break;
          }
        }

      private:
        const char *program_name, *program_description, *files_usage;

        std::string pem_certificate_file_path, pem_private_key_file_path, pem_private_key_passphrase;
        std::string pkcs12_file_path, pkcs12_passphrase;
        YIELD::SSLContext* ssl_context;
        uint64_t timeout_ms;
      };
    };
  };
};

#endif
