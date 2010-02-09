/* Copyright 2009 Juan González de Benito.
 * This source comes from the XtreemFS project. It is licensed under the GPLv2
 * (see COPYING for terms and conditions).
 */

#include "xtreemfs/main.h"
using namespace xtreemfs;

#include "vivaldi_node.h"
#include "zipf_generator.h"

#include <cstdio>
#include <cstdlib>
#include <ctime>
#include <limits>
#include <cstring>

#include <typeinfo>

#include <string>
#include <list>

#include <sstream>

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
#define ITERATIONS_BEFORE_UPDATING 12
/*
 * Number of retries sent before accepting a extremely high RTT.
 */
#define MAX_RETRIES_FOR_A_REQUEST 2

/*
 * Skew for ZipfGenerator
 */
#define ZIPFGENERATOR_SKEW 0.5

#ifndef _WIN32
  #define SPRINTF_VIV(buff,size,format,...) \
    snprintf(buff,size,format,__VA_ARGS__)
#else
  #define SPRINTF_VIV(buff,size,format,...) \
    sprintf_s(buff,size,format,__VA_ARGS__)
#endif


namespace xtfs_vivaldi
{
  class KnownOSD
  {

  public:
    KnownOSD( const std::string uuid,
              const org::xtreemfs::interfaces::VivaldiCoordinates &coordinates) : \
        uuid(uuid),
        coordinates(coordinates) { }

    org::xtreemfs::interfaces::VivaldiCoordinates *get_coordinates()
    {
      return &this->coordinates;
    }
    std::string get_uuid()
    {
      return this->uuid;
    }
    void set_coordinates( org::xtreemfs::interfaces::VivaldiCoordinates new_coords )
    {
      this->coordinates = new_coords;
    }
  private:
    std::string uuid;
    org::xtreemfs::interfaces::VivaldiCoordinates coordinates;
  };

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
      {
        throw YIELD::platform::Exception( "must specify dir_host and a "\
                                          "Vivaldi coordinates output file path");
      }
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

          if (  vivaldi_coordinates_file->read( xdr_buffer.incRef() ) == 3*sizeof(double) )
          {

            YIELD::platform::XDRUnmarshaller xdr_unmarshaller( xdr_buffer.incRef() );
            my_vivaldi_coordinates.unmarshal( xdr_unmarshaller );

            get_log()->getStream( YIELD::platform::Log::LOG_DEBUG ) <<
                "xtfs_vivaldi:coordinates readed from file:(" <<
                my_vivaldi_coordinates.get_x_coordinate() <<
                "," << my_vivaldi_coordinates.get_y_coordinate() << ")";

          }
          else
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

      std::list<KnownOSD> known_osds;
      bool valid_known_osds = false;

      std::vector<uint64_t> currentRetries;
      int retriesInARow = 0;


      KnownOSD *chosen_osd_service;

      ZipfGenerator rankGen(ZIPFGENERATOR_SKEW);

      for ( ;; )
      {
        try
        {
          //Get a list of OSDs from the DS
          if( (vivaldiIterations%ITERATIONS_BEFORE_UPDATING) == 0)
          {
            valid_known_osds = update_known_osds(known_osds, own_node);

            if( valid_known_osds && !known_osds.empty() )
            {
              rankGen.set_size( known_osds.size() );
            }

            currentRetries.clear(); /*The pending retries are discarded, because
                                    the old OSDs might not be in the new list*/
            retriesInARow = 0;
          }


          if ( valid_known_osds && !known_osds.empty() )
          {

            if(retriesInARow==0)
            {

              //Choose an OSD, only if there's no pending retry
              int ind = rankGen.next();

              std::list<KnownOSD>::iterator known_iterator = known_osds.begin();
              for( int i=0;
                    ( i<ind ) && (known_iterator != known_osds.end() );
                    known_iterator++, i++ )
              {
                //Move the iterator over the chosen service
              }

              chosen_osd_service = &(*known_iterator);
            }

            yidl::runtime::auto_Object<org::xtreemfs::interfaces::AddressMappingSet> \
                random_osd_address_mappings = \
                    dir_proxy->getAddressMappingsFromUUID(chosen_osd_service->get_uuid());

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
                    chosen_osd_service->get_uuid();

                YIELD::platform::Time start_time;

                osd_proxy->xtreemfs_ping( *own_node.getCoordinates(),
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
                  if( !own_node.recalculatePosition(  random_osd_vivaldi_coordinates,
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
                  for(  std::vector<uint64_t>::iterator retries_iterator = \
                            currentRetries.begin();
                        retries_iterator < currentRetries.end();
                        retries_iterator++)
                  {

                    if( (*retries_iterator) < lowestOne )
                    {
                      lowestOne = (*retries_iterator);
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

                //Print a trace
                char auxStr[256];
                SPRINTF_VIV(  auxStr,
                              256,
                              "%s:%lld(Viv:%.3f) Own:(%.3f,%.3f) lE=%.3f "\
                              "Rem:(%.3f,%.3f) rE=%.3f %s\n",
                                  retried?"RETRY":"RTT",
                                  static_cast<long long int>(measuredRTT),
                                  own_node.calculateDistance( (*own_node.getCoordinates()),
                                                              random_osd_vivaldi_coordinates),
                                  own_node.getCoordinates()->get_x_coordinate(),
                                  own_node.getCoordinates()->get_y_coordinate(),
                                  own_node.getCoordinates()->get_local_error(),
                                  random_osd_vivaldi_coordinates.get_x_coordinate(),
                                  random_osd_vivaldi_coordinates.get_y_coordinate(),
                                  random_osd_vivaldi_coordinates.get_local_error(),
                                  chosen_osd_service->get_uuid().data());
                get_log()->getStream( YIELD::platform::Log::LOG_INFO ) <<
                    "xtfs_vivaldi:" << auxStr;

                //Update OSD's coordinates
                chosen_osd_service->set_coordinates( random_osd_vivaldi_coordinates );


                //Re-sort known_osds

                std::list<KnownOSD> aux_osd_list( known_osds );
                known_osds.clear();

                for(  std::list<KnownOSD>::reverse_iterator aux_iterator = aux_osd_list.rbegin();
                      aux_iterator != aux_osd_list.rend();
                      aux_iterator++ )
                {

                  double new_osd_distance = \
                      own_node.calculateDistance( *(aux_iterator->get_coordinates()),
                                                  *own_node.getCoordinates() );

                  std::list<KnownOSD>::iterator known_iterator = known_osds.begin();

                  while( known_iterator != known_osds.end() )
                  {
                    double old_osd_distance = \
                        own_node.calculateDistance( *(known_iterator->get_coordinates()),
                                                    *own_node.getCoordinates() );
                    if( old_osd_distance >= new_osd_distance )
                    {
                      known_osds.insert( known_iterator, (*aux_iterator));
                      break;
                    }
                    else
                    {
                      known_iterator++;
                    }
                  }

                  if( known_iterator == known_osds.end() )
                  {
                    known_osds.push_back( (*aux_iterator) );
                  }
                }//End re-sorting
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
        uint64_t sleep_in_ms = static_cast<uint64_t>( \
                MIN_RECALCULATION_IN_MS +
                ( (static_cast<double>(std::rand())/(RAND_MAX-1)) *
                (MAX_RECALCULATION_IN_MS - MIN_RECALCULATION_IN_MS) ));

        get_log()->getStream( YIELD::platform::Log::LOG_DEBUG ) <<
            "xtfs_vivaldi:sleeping during " << sleep_in_ms << " ms.";

        YIELD::platform::Thread::nanosleep( sleep_in_ms * NS_IN_MS );

        vivaldiIterations = (vivaldiIterations+1)%LONG_MAX;

      }
    }

    /* Retrieves a list of available OSDs from the DS
     */
    bool update_known_osds(std::list<KnownOSD> &updated_osds,VivaldiNode &own_node){

      bool retval = true;

      try
      {
        org::xtreemfs::interfaces::ServiceSet received_osds;

        dir_proxy->xtreemfs_service_get_by_type(  org::xtreemfs::interfaces::SERVICE_TYPE_OSD,
                                                  received_osds );

        updated_osds.clear();

        //Fill the list, ignoring every offline OSD
        for(  org::xtreemfs::interfaces::ServiceSet::iterator ss_iterator = received_osds.begin();
              ss_iterator != received_osds.end();
              ss_iterator++)
        {
          if( (*ss_iterator).get_last_updated_s() > 0) //only online OSDs
          {

            org::xtreemfs::interfaces::ServiceDataMap sdm = (*ss_iterator).get_data();
            org::xtreemfs::interfaces::ServiceDataMap::iterator sdm_iterator = \
                sdm.find("vivaldi_coordinates");

            //If the DS does not have the OSD's coordinates, we discard this entry
            if( sdm_iterator != sdm.end() )
            {

              //Parse the coordinates provided by the DS
              org::xtreemfs::interfaces::VivaldiCoordinates osd_coords;
              OutputUtils::stringToCoordinates(sdm_iterator->second,osd_coords);

              KnownOSD new_osd( (*ss_iterator).get_uuid(), osd_coords);

              //Calculate the current distance from the client to the new OSD
              double new_osd_distance = own_node.calculateDistance( \
                  *(own_node.getCoordinates()),
                  osd_coords);

              std::list<KnownOSD>::iterator up_iterator = updated_osds.begin();
              while( up_iterator != updated_osds.end() )
              {
                double old_osd_distance = \
                    own_node.calculateDistance( *up_iterator->get_coordinates(),
                                                *(own_node.getCoordinates()) );
                if( old_osd_distance >= new_osd_distance )
                {
                  updated_osds.insert( up_iterator, new_osd );
                  break;
                }
                else
                {
                  up_iterator++;
                }
              }

              if( up_iterator == updated_osds.end() )
              {
                updated_osds.push_back( new_osd );
              }
            }
          }
        }
      }
      catch( std::exception ex )
      {

        get_log()->getStream( YIELD::platform::Log::LOG_ERR ) <<
            "xtfs_vivaldi:Impossible to update known OSDs:" << ex.what();
        retval = false;

      }

      return retval;
    }
  };
};


int main( int argc, char** argv )
{
  return xtfs_vivaldi::Main().main( argc, argv );
}
