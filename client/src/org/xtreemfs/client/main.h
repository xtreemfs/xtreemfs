// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ECTS_XTREEMFS_CLIENT_SRC_ORG_XTREEMFS_CLIENT_BIN_OPTIONS_H
#define ECTS_XTREEMFS_CLIENT_SRC_ORG_XTREEMFS_CLIENT_BIN_OPTIONS_H

#include "yield/main.h"

#include "org/xtreemfs/client/dir_proxy.h"
#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/interfaces/constants.h"


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
            return YIELD::Main::main( argc, argv );
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
          log = NULL;
          stage_group = &YIELD::SEDAStageGroup::createStageGroup();

          addOption( OPTION_PEM_CERTIFICATE_FILE_PATH, "--cert", "--pem-certificate-file-path", "PEM certificate file path" );
          addOption( OPTION_PEM_PRIVATE_KEY_FILE_PATH, "--pkey", "--pem-private-key-file-path", "PEM private key file path" );
          addOption( OPTION_PEM_PRIVATE_KEY_PASSPHRASE, "--pass", "--pem-private-key-passphrase", "PEM private key passphrase" );
          addOption( OPTION_PKCS12_FILE_PATH, "--pkcs12-file-path", NULL, "PKCS#12 file path" );
          addOption( OPTION_PKCS12_PASSPHRASE, "--pkcs12-passphrase", NULL, "PKCS#12 passphrase" );
          ssl_context = NULL;

          addOption( OPTION_TIMEOUT_MS, "-t", "--timeout-ms", "n" );
          timeout_ms = YIELD::ONCRPCProxy::DEFAULT_REQUEST_TIMEOUT_MS;
        }

        virtual ~Main()
        {
          YIELD::StageGroup::destroyStageGroup( *stage_group );
          YIELD::Object::decRef( log );
          YIELD::Object::decRef( ssl_context );         
        }
        
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

        YIELD::auto_Object<OSDProxyFactory> createOSDProxyFactory( const YIELD::URI& dir_uri )
        {
          return new OSDProxyFactory( *createDIRProxy( dir_uri ).release(), *stage_group );
        }

        YIELD::Log& get_log()
        {
          if ( log == NULL )
            log = new YIELD::Log( std::cout, get_log_level() );
          return *log;
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
          if ( volume_uri.get()->get_resource().size() > 1 )
          {
            volume_name = volume_uri.get()->get_resource().c_str() + 1;
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
            /*
            case OPTION_LOG_LEVEL:
            {
              if ( get_log_level() >= YIELD::Log::LOG_DEBUG )
                YIELD::TCPSocket::set_trace_socket_io_onoff( true );
            }
            break;
            */

            case OPTION_PEM_CERTIFICATE_FILE_PATH: pem_certificate_file_path = arg; break;                  
            case OPTION_PEM_PRIVATE_KEY_FILE_PATH: pem_private_key_file_path = arg; break;
            case OPTION_PEM_PRIVATE_KEY_PASSPHRASE: pem_private_key_passphrase = arg; break;
            case OPTION_PKCS12_FILE_PATH: pkcs12_file_path = arg; break;
            case OPTION_PKCS12_PASSPHRASE: pkcs12_passphrase = arg; break;

            case OPTION_TIMEOUT_MS:
            {
              timeout_ms = atol( arg );
              if ( timeout_ms == 0 )
                timeout_ms = YIELD::ONCRPCProxy::DEFAULT_REQUEST_TIMEOUT_MS;
            }
            break;

            default: YIELD::Main::parseOption( id, arg ); break;
          }
        }

      private:
        const char *program_name, *program_description, *files_usage;

        std::string pem_certificate_file_path, pem_private_key_file_path, pem_private_key_passphrase;
        std::string pkcs12_file_path, pkcs12_passphrase;
        uint64_t timeout_ms;

        YIELD::Log* log;
        YIELD::SSLContext* ssl_context;
        YIELD::StageGroup* stage_group;


        template <class ProxyType>
        YIELD::auto_Object<ProxyType> createProxy( const YIELD::URI& uri, uint16_t default_port )
        {
          YIELD::URI checked_uri( uri );
          if ( checked_uri.get_port() == 0 )
            checked_uri.set_port( default_port );
          ProxyType* proxy = new ProxyType( checked_uri, YIELD::Object::incRef( get_ssl_context() ), &get_log().incRef() );
          proxy->set_request_timeout_ms( timeout_ms );
          stage_group->createStage( proxy->incRef(), new YIELD::FDAndInternalEventQueue, &get_log() );
          return proxy;
        }

        YIELD::SSLContext* get_ssl_context()
        {
          if ( ssl_context == NULL )
          {
            if ( !pkcs12_file_path.empty() )
              ssl_context = new YIELD::SSLContext( SSLv3_client_method(), pkcs12_file_path, pkcs12_passphrase );
            else if ( !pem_certificate_file_path.empty() && !pem_private_key_file_path.empty() )
              ssl_context = new YIELD::SSLContext( SSLv3_client_method(), pem_certificate_file_path, pem_private_key_file_path, pem_private_key_passphrase );
          }

          return ssl_context;
        }
      };
    };
  };
};

#endif
