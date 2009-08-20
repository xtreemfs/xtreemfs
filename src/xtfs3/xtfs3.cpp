// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "http_request_handler.h"
using namespace xtfs3;

#include "xtreemfs/main.h"


YIELD::CountingSemaphore YIELD::Main::pause_semaphore;


namespace xtfs3
{
  class Main : public xtreemfs::Main
  {
  public:
    Main() 
      : xtreemfs::Main( "xtfs3", "mount an XtreemFS volume via S3", "[<XtreemFS dir host>[:dir port]/<XtreemFS volume name>]" )
    {
      addOption( XTFS3_OPTION_PORT, "-p", "--port", "n" );
      port = 8080;

      addOption( XTFS3_OPTION_VIRTUAL_HOST_NAME, "--virtual-host-name", NULL, "localhost" );
      virtual_host_name = "localhost";
    }

  private:
    enum
    {
      XTFS3_OPTION_PORT = 20,
      XTFS3_OPTION_VIRTUAL_HOST_NAME = 21
    };


    YIELD::auto_URI dir_uri;
    uint16_t port;
    std::string virtual_host_name;
    std::string volume_name;


    // YIELD::Main
    int _main( int, char** )
    {   
      YIELD::auto_StageGroup stage_group( new YIELD::SEDAStageGroup );

      YIELD::auto_Volume volume;
      if ( dir_uri != NULL )
        volume = new xtreemfs::Volume( *dir_uri, volume_name, 0, get_log(), get_proxy_flags(), get_operation_timeout(), get_proxy_ssl_context() );
      else
        volume = new YIELD::Volume;

      auto_HTTPRequestHandler http_request_handler = new HTTPRequestHandler( virtual_host_name, volume );
      YIELD::auto_Stage http_request_handler_stage = stage_group->createStage( http_request_handler );

      YIELD::auto_HTTPServer http_server = YIELD::HTTPServer::create( YIELD::URI( "http", "0.0.0.0", port ), http_request_handler_stage->incRef(), get_log() );

      YIELD::Main::pause();

      return 0;
    }

    void parseFiles( int file_count, char** files )
    {
      if ( file_count >= 1 )
      {
        dir_uri = parseVolumeURI( files[0], volume_name );
        if ( dir_uri->get_port() == 0 )
          dir_uri->set_port( org::xtreemfs::interfaces::DIRInterface::DEFAULT_ONCRPC_PORT );
      }
    }

    void parseOption( int id, char* arg )
    {
      switch ( id )
      {
        case XTFS3_OPTION_PORT:
        {
          port = static_cast<uint16_t>( atoi( arg ) );
          if ( port == 0 )
            port = 8080;
        }
        break;

        default: xtreemfs::Main::parseOption( id, arg ); break;
      }
    }
  };
};

int main( int argc, char** argv )
{
  return xtfs3::Main().main( argc, argv );
}
