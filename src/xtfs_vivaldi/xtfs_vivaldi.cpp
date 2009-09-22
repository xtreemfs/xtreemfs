// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/main.h"
using namespace xtreemfs;

#include <cstdio>
#include <cstdlib>
#include <ctime>


#ifdef _WIN32
YIELD::platform::CountingSemaphore YIELD::Main::pause_semaphore;
#endif


namespace xtfs_vivaldi
{
  class Main : public xtreemfs::Main, public YIELD::platform::Thread
  {
  public:
    Main()
      : xtreemfs::Main( "xtfs_vivaldi", "start the XtreemFS Vivaldi service", "[oncrpc[s]://]<dir host>[:dir port]/<volume name> <path to Vivaldi coordinates output file>" )
    {
      std::srand( static_cast<unsigned int>( std::time( NULL ) ) );
    }

  private:
    auto_DIRProxy dir_proxy;
    YIELD::platform::Path vivaldi_coordinates_file_path;

    // YIELD::Main
    int _main( int, char** )
    {
      YIELD::platform::Thread::start();

      YIELD::Main::pause();

      return 0;
    }

    void parseFiles( int file_count, char** files )
    {
      if ( file_count >= 2 )
      {
        YIELD::ipc::auto_URI dir_uri = parseURI( files[0] );
        if ( dir_uri->get_port() == 0 )
          dir_uri->set_port( org::xtreemfs::interfaces::DIRInterface::DEFAULT_ONCRPC_PORT );
        dir_proxy = createDIRProxy( *dir_uri );
        vivaldi_coordinates_file_path = files[1];
      }
      else
        throw YIELD::platform::Exception( "must specify dir_host and a Vivaldi coordinates output file path" );
    }

    // YIELD::Thread
    void run()
    {
      org::xtreemfs::interfaces::VivaldiCoordinates my_vivaldi_coordinates( 1, 1, 0 );

      for ( ;; )
      {
        try
        {
          org::xtreemfs::interfaces::ServiceSet osd_services;
          dir_proxy->xtreemfs_service_get_by_type( org::xtreemfs::interfaces::SERVICE_TYPE_OSD, osd_services );

          if ( !osd_services.empty() )
          {
            const org::xtreemfs::interfaces::Service& random_osd_service = osd_services[std::rand() % osd_services.size()];
            yidl::runtime::auto_Object<org::xtreemfs::interfaces::AddressMappingSet> random_osd_address_mappings = dir_proxy->getAddressMappingsFromUUID( random_osd_service.get_uuid() );
            for ( org::xtreemfs::interfaces::AddressMappingSet::iterator random_osd_address_mapping_i = random_osd_address_mappings->begin(); random_osd_address_mapping_i != random_osd_address_mappings->end(); random_osd_address_mapping_i++ )
            {
              if ( ( *random_osd_address_mapping_i ).get_protocol() == org::xtreemfs::interfaces::ONCRPCU_SCHEME )
              {
                auto_OSDProxy osd_proxy = OSDProxy::create( ( *random_osd_address_mapping_i ).get_uri(), get_proxy_flags(), get_log(), get_operation_timeout() );              
                org::xtreemfs::interfaces::VivaldiCoordinates random_osd_vivaldi_coordinates;
                YIELD::platform::Time start_time;
                osd_proxy->xtreemfs_ping( org::xtreemfs::interfaces::VivaldiCoordinates(), random_osd_vivaldi_coordinates );
                YIELD::platform::Time rtt( YIELD::platform::Time() - start_time );

                // TODO: calculate my_vivaldi_coordinates here
              }
            }
          }
        }
        catch ( std::exception& exc )
        {
          get_log()->getStream( YIELD::platform::Log::LOG_ERR ) << "xtfs_vivaldi: error pinging OSDs: " << exc.what() << ".";
          continue;
        }

        YIELD::platform::auto_File vivaldi_coordinates_file = YIELD::platform::Volume().open( vivaldi_coordinates_file_path, O_CREAT|O_TRUNC|O_WRONLY );
        if ( vivaldi_coordinates_file != NULL )
        {
          YIELD::platform::XDRMarshaller xdr_marshaller;
          my_vivaldi_coordinates.marshal( xdr_marshaller );
          vivaldi_coordinates_file->write( xdr_marshaller.get_buffer().release() );
        }
  
        YIELD::platform::Thread::sleep( 5 * NS_IN_S );
      }
    }
  };
};


int main( int argc, char** argv )
{
  return xtfs_vivaldi::Main().main( argc, argv );
}
