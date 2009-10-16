// Copyright 2009 Juan González de Benito.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/main.h"
using namespace xtreemfs;

#include "vivaldi_node.h"

#include <cstdio>
#include <cstdlib>
#include <ctime>

//Remove after evaluating
#include <cstring>

#ifdef _WIN32
YIELD::platform::CountingSemaphore YIELD::Main::pause_semaphore;
#endif

/**
 * Minimum recalculation period.
 * 
 * The recalculation period is randomly determined and is always included between
 * the minimum and the maximum period.
 */
#define MIN_RECALCULATION_IN_MS 1000 * 5

/**
 * Maximum recalculation period.
 * 
 * The recalculation period is randomly determined and is always included between
 * the minimum and the maximum period.
 */
#define MAX_RECALCULATION_IN_MS 1000 * 10

namespace xtfs_vivaldi
{
  class Main : public xtreemfs::Main, public YIELD::platform::Thread
  {
  public:
    Main()
      : xtreemfs::Main( "xtfs_vivaldi", "start the XtreemFS Vivaldi service", "<dir host>[:port]/<volume name> <path to Vivaldi coordinates output file>" )
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
    
      org::xtreemfs::interfaces::VivaldiCoordinates my_vivaldi_coordinates( 0, 0, 0 );
      
    //TODO:Read and initialize our own coordinates -> own VivaldiNode
      YIELD::platform::auto_File vivaldi_coordinates_file = YIELD::platform::Volume().open( vivaldi_coordinates_file_path, O_RDONLY );
  		if ( vivaldi_coordinates_file != NULL )
  		{
        for ( ;; )
        {
          yidl::runtime::StackBuffer<3*sizeof(double)> xdr_buffer;
          if ( vivaldi_coordinates_file->read( xdr_buffer.incRef() ) == 3*sizeof(double) )
          {
            
            YIELD::platform::XDRUnmarshaller xdr_unmarshaller( xdr_buffer.incRef() );
            my_vivaldi_coordinates.unmarshal( xdr_unmarshaller );
            
            get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "Coordinates readed:("<<my_vivaldi_coordinates.get_x_coordinate()<<","<<my_vivaldi_coordinates.get_y_coordinate()<<")";
                        
          }else
          {
            break;
          }          
        }
  		}else{
        get_log()->getStream( YIELD::platform::Log::LOG_ERR ) << "Impossible to read coordinates";
      }
  
      VivaldiNode own_node(my_vivaldi_coordinates);
      
  		for ( ;; )
  		{
  			try
  			{
          org::xtreemfs::interfaces::ServiceSet osd_services;
  				dir_proxy->xtreemfs_service_get_by_type( org::xtreemfs::interfaces::SERVICE_TYPE_OSD, osd_services );
  
  				if ( !osd_services.empty() )
  				{
  					//Choose one OSD randomly
  					const org::xtreemfs::interfaces::Service& random_osd_service = osd_services[std::rand() % osd_services.size()];
  					yidl::runtime::auto_Object<org::xtreemfs::interfaces::AddressMappingSet> random_osd_address_mappings = dir_proxy->getAddressMappingsFromUUID( random_osd_service.get_uuid() );
              
  					//Several mappings for the same UUID
  					for ( org::xtreemfs::interfaces::AddressMappingSet::iterator random_osd_address_mapping_i = random_osd_address_mappings->begin(); random_osd_address_mapping_i != random_osd_address_mappings->end(); random_osd_address_mapping_i++ )
  					{
              	
  						if ( ( *random_osd_address_mapping_i ).get_protocol() == org::xtreemfs::interfaces::ONCRPCU_SCHEME )
  						{
  
  							auto_OSDProxy osd_proxy = OSDProxy::create( ( *random_osd_address_mapping_i ).get_uri(), get_proxy_flags(), get_log(), get_operation_timeout() );              
  							org::xtreemfs::interfaces::VivaldiCoordinates random_osd_vivaldi_coordinates;
  							
  							//Send the request and measure the RTT
  							YIELD::platform::Time start_time;
  							osd_proxy->xtreemfs_ping( org::xtreemfs::interfaces::VivaldiCoordinates(), random_osd_vivaldi_coordinates );
  							YIELD::platform::Time rtt( YIELD::platform::Time() - start_time );
  							
  							// TODO: calculate my_vivaldi_coordinates here
                
                std::cout << "Received: (" << random_osd_vivaldi_coordinates.get_x_coordinate() << "," << random_osd_vivaldi_coordinates.get_y_coordinate() << ") Own:(" << own_node.getCoordinates()->get_x_coordinate() << "," << own_node.getCoordinates()->get_y_coordinate() << ")\n";
  							own_node.recalculatePosition(random_osd_vivaldi_coordinates,rtt.as_unix_time_ms(),true);
                std::cout << "New own coordinates:(" << own_node.getCoordinates()->get_x_coordinate() << "," << own_node.getCoordinates()->get_x_coordinate() << ") ";
  							std::cout <<" UUID "<<random_osd_service.get_uuid() <<" RTT=" << rtt.as_unix_time_ms() << "\n";
  
  						}
  					}
  				}
  			}
  			catch ( std::exception& exc )
  			{
  				get_log()->getStream( YIELD::platform::Log::LOG_ERR ) << "xtfs_vivaldi: error pinging OSDs: " << exc.what() << ".";
  				continue;
  			}
  
        //Store the new coordinates in a local file
        get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "Storing coordinates:("<<own_node.getCoordinates()->get_x_coordinate()<<","<<own_node.getCoordinates()->get_y_coordinate()<<")";
        
  			YIELD::platform::auto_File vivaldi_coordinates_file = YIELD::platform::Volume().open( vivaldi_coordinates_file_path, O_CREAT|O_TRUNC|O_WRONLY );
  			if ( vivaldi_coordinates_file != NULL )
  			{
  				YIELD::platform::XDRMarshaller xdr_marshaller;
  				own_node.getCoordinates()->marshal( xdr_marshaller );
  				vivaldi_coordinates_file->write( xdr_marshaller.get_buffer().release() );
  			}
    
        //Sleep until the next iteration
        uint64_t sleep_in_ms = MIN_RECALCULATION_IN_MS + ( (static_cast<double>(std::rand())/RAND_MAX) * (MAX_RECALCULATION_IN_MS - MIN_RECALCULATION_IN_MS) );
        get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "Sleeping during "<<sleep_in_ms<<" ms.";
      	YIELD::platform::Thread::sleep( sleep_in_ms * NS_IN_MS );
  		}
    }
    
    /********************************************
     * Only to evaluate the results
     */
    void executeEvaluation(org::xtreemfs::interfaces::ServiceSet osds,VivaldiNode &own_node){
  
      if ( !osds.empty() )
      {
        //Create arrays
        uint64_t *rtts = new uint64_t[osds.get_size()];
        for(size_t i=0;i<osds.get_size();i++)
        {
          rtts[i]=-1;
        }
         
        org::xtreemfs::interfaces::VivaldiCoordinates *remoteCoordinates = new org::xtreemfs::interfaces::VivaldiCoordinates[osds.get_size()];
        
        for(size_t i=0;i<osds.get_size();i++){
          
          const org::xtreemfs::interfaces::Service& one_osd = osds[i];
          
          yidl::runtime::auto_Object<org::xtreemfs::interfaces::AddressMappingSet> one_osd_address_mappings = dir_proxy->getAddressMappingsFromUUID( one_osd.get_uuid() );
        
          //Several mappings for the same UUID
          for ( org::xtreemfs::interfaces::AddressMappingSet::iterator one_osd_address_mapping_i = one_osd_address_mappings->begin(); one_osd_address_mapping_i != one_osd_address_mappings->end(); one_osd_address_mapping_i++ )
          {
            if ( ( *one_osd_address_mapping_i ).get_protocol() == org::xtreemfs::interfaces::ONCRPCU_SCHEME )
            {
              auto_OSDProxy osd_proxy = OSDProxy::create( ( *one_osd_address_mapping_i ).get_uri(), get_proxy_flags(), get_log(), get_operation_timeout() );
              
              //The sent coordinates are irrelevant
              org::xtreemfs::interfaces::VivaldiCoordinates ownCoords;
              YIELD::platform::Time start_time;
              osd_proxy->xtreemfs_ping(ownCoords , remoteCoordinates[i]);
              YIELD::platform::Time rtt( YIELD::platform::Time() - start_time );
              
              rtts[i] = rtt.as_unix_time_ms();
            }
          }
        }
        
        //Store and manage the observed results.
        time_t rawTime;
        time(&rawTime);
        struct tm *tmStruct = localtime(&rawTime);
        tmStruct->tm_year += 1900;
        tmStruct->tm_mon += 1;
        
        int rightMeasures = 0;
        for(size_t i=0;i<osds.get_size();i++)
        {
          if(rtts[i]>0) rightMeasures++;
        }
        
        char auxStr[64];
        
        
        std::string strWr("");
        strWr += "CLIENT";
        sprintf(auxStr," %d",rightMeasures);
        strWr += auxStr;
        sprintf(auxStr," %d/%d/%d-%d:%d:%d", tmStruct->tm_mday,tmStruct->tm_mon,tmStruct->tm_year, tmStruct->tm_hour, tmStruct->tm_min,tmStruct->tm_sec);
        strWr += auxStr;
        org::xtreemfs::interfaces::VivaldiCoordinates *localCoords = own_node.getCoordinates();
        sprintf(auxStr," Coordinates:%.3f,%.3f\n",localCoords->get_x_coordinate(),localCoords->get_y_coordinate());
        strWr += auxStr;
        
        for(size_t i=0;i<osds.get_size();i++)
        {
          if(rtts[i]>0)
          {
            double distance = own_node.caltulateDistance((*localCoords),remoteCoordinates[i]);
            sprintf(auxStr,"%ld\t%.3f\t%.3f,%.3f\n", rtts[i],distance,remoteCoordinates[i].get_x_coordinate(),remoteCoordinates[i].get_y_coordinate());
            strWr += auxStr;
          }
        }
        
        
        std::cout << strWr;
        
        delete[] rtts;
        delete[] remoteCoordinates;
      }
    }
    
  };
};


int main( int argc, char** argv )
{
  return xtfs_vivaldi::Main().main( argc, argv );
}
