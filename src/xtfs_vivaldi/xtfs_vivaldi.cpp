/* Copyright 2009 Juan González de Benito.
 * This source comes from the XtreemFS project. It is licensed under the GPLv2 
 * (see COPYING for terms and conditions).
 */

#include "xtreemfs/main.h"
using namespace xtreemfs;

#include "vivaldi_node.h"

#include <cstdio>
#include <cstdlib>
#include <ctime>
#include <limits>
#include <cstring>

#include <typeinfo>

#ifdef _WIN32
YIELD::platform::CountingSemaphore YIELD::Main::pause_semaphore;
#endif

/*
 * Minimum recalculation period.
 * 
 * The recalculation period is randomly determined and is always included 
 * between the minimum and the maximum period.
 */
#define MIN_RECALCULATION_IN_MS 1000 * 270

/*
 * Maximum recalculation period.
 * 
 * The recalculation period is randomly determined and is always included 
 * between the minimum and the maximum period.
 */
#define MAX_RECALCULATION_IN_MS 1000 * 330

/*
 * Number of times the node recalculates its position before updating
 * its list of existent OSDs.
 */
#define ITERATIONS_BEFORE_UPDATING 24 

/*
 * Number of retries sent before accepting a high RTT.
 */
#define MAX_RETRIES_FOR_A_REQUEST 2


#ifndef _WIN32
  #define SPRINTF_VIV(buff,size,format,...) \
          snprintf(buff,size,format,__VA_ARGS__)
#else
  #define SPRINTF_VIV(buff,size,format,...) \
          sprintf_s(buff,size,format,__VA_ARGS__)
#endif


namespace xtfs_vivaldi
{
  class Main : public xtreemfs::Main, public YIELD::platform::Thread
  {
  public:
    Main()
      : xtreemfs::Main( "xtfs_vivaldi", 
                        "start the XtreemFS Vivaldi service ",
                        "<dir host>[:port] <path to Vivaldi coordinates output file>" )
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
        dir_proxy = createDIRProxy( *dir_uri );
        vivaldi_coordinates_file_path = files[1];
      }
      else
        throw YIELD::platform::Exception("must specify dir_host and a "\
                                         "Vivaldi coordinates output file path");
    }

    // YIELD::Thread
    void run()
    {
      
      //Initialized to (0,0) by default
      org::xtreemfs::interfaces::VivaldiCoordinates my_vivaldi_coordinates( 0, 0, 0 );

      //Try to read coordinates from local file      
      YIELD::platform::auto_File vivaldi_coordinates_file = \
          YIELD::platform::Volume().open( vivaldi_coordinates_file_path, O_RDONLY );
      
      if ( vivaldi_coordinates_file != NULL )
      {
        for ( ;; )
        {
          //x,y,local_error => 3 doubles
          yidl::runtime::StackBuffer<3*sizeof(double)> xdr_buffer;
          
          if (  vivaldi_coordinates_file->read( xdr_buffer.incRef() ) == 
                3*sizeof(double) )
          {
            
            YIELD::platform::XDRUnmarshaller xdr_unmarshaller( xdr_buffer.incRef() );
            my_vivaldi_coordinates.unmarshal( xdr_unmarshaller );
            
            get_log()->getStream( YIELD::platform::Log::LOG_DEBUG ) << 
                "xtfs_vivaldi:coordinates readed from file:(" << 
                my_vivaldi_coordinates.get_x_coordinate() << 
                "," << my_vivaldi_coordinates.get_y_coordinate() << ")";
                        
          }else
          {
            break;
          }          
        }
        vivaldi_coordinates_file->close();
      }
      else
      {
        get_log()->getStream( YIELD::platform::Log::LOG_DEBUG ) << 
            "xtfs_vivaldi:impossible to read coordinates from file."\
            "Initializing them by default...";
      }

      VivaldiNode own_node(my_vivaldi_coordinates);
      
      long vivaldiIterations = 0;
      
      org::xtreemfs::interfaces::ServiceSet osd_services;
      
      std::vector<uint64_t> currentRetries;
      int retriesInARow = 0;
      
      org::xtreemfs::interfaces::Service *random_osd_service;
      
      for ( ;; )
      {
        try
        {

          //Get a list of OSDs from the DS
          if( (vivaldiIterations%ITERATIONS_BEFORE_UPDATING) == 1)
          {
            updateKnownOSDs(osd_services);
            currentRetries.clear(); /*The pending retries are discarded, because
                                    the old OSDs might not be in the new list*/
            retriesInARow = 0;
          }
          
          if ( !osd_services.empty() )
          {
            
            if(retriesInARow==0){
              //Choose one OSD randomly, only if there's no pending retry
              random_osd_service = &osd_services[std::rand() % osd_services.size()];
            }

            yidl::runtime::auto_Object<org::xtreemfs::interfaces::AddressMappingSet> \
              random_osd_address_mappings = 
                dir_proxy->getAddressMappingsFromUUID(random_osd_service->get_uuid());
  
            //Several mappings for the same UUID
            for ( org::xtreemfs::interfaces::AddressMappingSet::iterator \
                  random_osd_address_mapping_i = random_osd_address_mappings->begin();
                  random_osd_address_mapping_i != random_osd_address_mappings->end();
                  random_osd_address_mapping_i++ )
              {

              if (  (*random_osd_address_mapping_i).get_protocol() == 
                    org::xtreemfs::interfaces::ONCRPCU_SCHEME )
              {
                auto_OSDProxy osd_proxy = \
                    OSDProxy::create( ( *random_osd_address_mapping_i ).get_uri(), 
                                      OSDProxy::CONCURRENCY_LEVEL_DEFAULT, 
                                      get_proxy_flags(), 
                                      get_log(), 
                                      get_operation_timeout() );

                org::xtreemfs::interfaces::VivaldiCoordinates random_osd_vivaldi_coordinates;
                
                //Send the request and measure the RTT
                get_log()->getStream( YIELD::platform::Log::LOG_DEBUG ) << 
                    "xtfs_vivaldi:recalculating against " << 
                    random_osd_service->get_uuid();

                YIELD::platform::Time start_time;

                osd_proxy->xtreemfs_ping( org::xtreemfs::interfaces::VivaldiCoordinates(),
                                          random_osd_vivaldi_coordinates );
                
                YIELD::platform::Time rtt( YIELD::platform::Time() - start_time );
                
                get_log()->getStream( YIELD::platform::Log::LOG_DEBUG ) << 
                    "xtfs_vivaldi:ping response received";
                
                //Next code is not executed if the ping request times out
                
                uint64_t measuredRTT = rtt.as_unix_time_ms();
                
                bool retried = false;
                // Recalculate coordinates here
                if( retriesInARow < MAX_RETRIES_FOR_A_REQUEST )
                {
                  if( !own_node.recalculatePosition(random_osd_vivaldi_coordinates,
                                                    measuredRTT,
                                                    false) )
                  {
                    
                    /*The movement has been postponed because the measured RTT
                    seems to be a peak*/
                    currentRetries.push_back(measuredRTT);
                    retriesInARow++;
                    retried = true;
                    
                  }
                  else
                  {
                    
                    //The movement has been accepted
                    currentRetries.clear();
                    retriesInARow = 0;
                    
                  }  
                }
                else
                {
                 
                  //Choose the lowest RTT
                  uint64_t lowestOne = measuredRTT;
                  for(  std::vector<uint64_t>::iterator it = currentRetries.begin();
                        it<currentRetries.end();
                        it++)
                  {
                          
                    if( (*it) < lowestOne )
                    {
                      lowestOne = (*it);
                    }
                    
                  }
                  
                  //Forcing recalculation because we've retried too many times
                  own_node.recalculatePosition( random_osd_vivaldi_coordinates,
                                                lowestOne,
                                                true);
                  currentRetries.clear();
                  retriesInARow = 0;
                  
                  //This is just to include in the trace the definitive RTT
                  measuredRTT = lowestOne;

                }
                
                //Print trace
                char auxStr[256];
                SPRINTF_VIV(  auxStr,
                              256,
 
                              "%s:%lld(Viv:%.3f) Own:(%.3f,%.3f) lE=%.3f "\
                                                "Rem:(%.3f,%.3f) rE=%.3f %s",
                              retried?"RETRY":"RTT",
                              static_cast<long long int>(measuredRTT),
                              own_node.calculateDistance((*own_node.getCoordinates()),
                                                          random_osd_vivaldi_coordinates),
                              own_node.getCoordinates()->get_x_coordinate(),
                              own_node.getCoordinates()->get_y_coordinate(),
                              own_node.getCoordinates()->get_local_error(),
                              random_osd_vivaldi_coordinates.get_x_coordinate(),
                              random_osd_vivaldi_coordinates.get_y_coordinate(),
                              random_osd_vivaldi_coordinates.get_local_error(),
                              random_osd_service->get_uuid().data());
                
                
                get_log()->getStream( YIELD::platform::Log::LOG_DEBUG ) << 
                    "xtfs_vivaldi:" << auxStr;
  
              }
            }
          }
          else
          {
            
              get_log()->getStream( YIELD::platform::Log::LOG_DEBUG ) << 
                  "xtfs_vivaldi:no OSD available";
          }
        }
        catch ( std::exception& exc )
        {
          get_log()->getStream( YIELD::platform::Log::LOG_ERR ) << 
              "xtfs_vivaldi: error pinging OSDs: " << exc.what() << ".";
          
          //TOFIX:This must be done only for timeout exceptions
          
          //We must avoid to keep retrying indefinitely against an OSD which is not responding
          if(retriesInARow && (++retriesInARow >= MAX_RETRIES_FOR_A_REQUEST) )
          {
            //If the last retry times out all the previous retries are discarded
            currentRetries.clear();
            retriesInARow = 0;
          }
        }
  
        //Store the new coordinates in a local file
        get_log()->getStream( YIELD::platform::Log::LOG_DEBUG ) << 
            "xtfs_vivaldi:storing coordinates in file:(" << 
            own_node.getCoordinates()->get_x_coordinate() << 
            "," << own_node.getCoordinates()->get_y_coordinate() << ")";
        
        vivaldi_coordinates_file = \
            YIELD::platform::Volume().open( vivaldi_coordinates_file_path, 
                                            O_CREAT|O_TRUNC|O_WRONLY );
                                          
        if ( vivaldi_coordinates_file != NULL )
        {
          
          YIELD::platform::XDRMarshaller xdr_marshaller;
          own_node.getCoordinates()->marshal( xdr_marshaller );
          vivaldi_coordinates_file->write( xdr_marshaller.get_buffer().release() );
          vivaldi_coordinates_file->close();
          
        }
    
        //Sleep until the next iteration
        uint64_t sleep_in_ms = \
                MIN_RECALCULATION_IN_MS + 
                ( (static_cast<double>(std::rand())/(RAND_MAX-1)) * 
                (MAX_RECALCULATION_IN_MS - MIN_RECALCULATION_IN_MS) );
                
        get_log()->getStream( YIELD::platform::Log::LOG_DEBUG ) << 
            "xtfs_vivaldi:sleeping during " << sleep_in_ms << " ms.";
        
        YIELD::platform::Thread::sleep( sleep_in_ms * NS_IN_MS );
        
        vivaldiIterations = (vivaldiIterations+1)%LONG_MAX;

      }
    }
    
    /* Retrieves a list of available OSDs from the DS
     */
    void updateKnownOSDs(org::xtreemfs::interfaces::ServiceSet &osds)
    {
      
      try
      {
        
        dir_proxy->xtreemfs_service_get_by_type( \
            org::xtreemfs::interfaces::SERVICE_TYPE_OSD, 
            osds );
  
        org::xtreemfs::interfaces::ServiceSet::iterator ss_iterator = osds.begin();
        
        while( ss_iterator != osds.end() )
        {
          
          if( (*ss_iterator).get_last_updated_s() == 0)
          {
            osds.erase(ss_iterator);
          }
          else
          {
            ss_iterator++;
          }
          
        }
      }
      catch( std::exception ex )
      {
        
        get_log()->getStream( YIELD::platform::Log::LOG_ERR ) << 
            "xtfs_vivaldi:Impossible to update known OSDs";
            
      }
    }
  };
};


int main( int argc, char** argv )
{
  return xtfs_vivaldi::Main().main( argc, argv );
}
