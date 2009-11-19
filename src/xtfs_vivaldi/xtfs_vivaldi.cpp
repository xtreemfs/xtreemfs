// Copyright 2009 Juan Gonz�lez de Benito.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

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
 * The recalculation period is randomly determined and is always included between
 * the minimum and the maximum period.
 */
#define MIN_RECALCULATION_IN_MS 1000 * 270

/*
 * Maximum recalculation period.
 * 
 * The recalculation period is randomly determined and is always included between
 * the minimum and the maximum period.
 */
#define MAX_RECALCULATION_IN_MS 1000 * 330

/*
 * Number of times the node recalculates its position before updating
 * its list of existent OSDs.
 */
#define ITERATIONS_BEFORE_UPDATING 10

#define MAX_RETRIES_FOR_A_REQUEST 3

#define MAX_REQUEST_TIMEOUT_IN_NS 1000000000 * 30

/*
 * REMOVE AFTER EVALUATING
 */
#define EVALUATION_ENABLED  true
#define NUMBER_OF_FILES     1
#define REPLICAS_PER_FILE   40
#define CHECK_EVERY_ITERATIONS 2
#define RES_FILE_NAME "res-%s-%d"
#define RECAL_FILE_NAME "recal-%s"
#ifndef _WIN32
  #define SPRINTF_VIV(buff,size,format,...) snprintf(buff,size,format,__VA_ARGS__)
#else
  #define SPRINTF_VIV(buff,size,format,...) sprintf_s(buff,size,format,__VA_ARGS__)
#endif


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
      
      //Initialized to (0,0) by default
      org::xtreemfs::interfaces::VivaldiCoordinates my_vivaldi_coordinates( 0, 0, 0 );

      //Try to read coordinates from local file      
      YIELD::platform::auto_File vivaldi_coordinates_file = YIELD::platform::Volume().open( vivaldi_coordinates_file_path, O_RDONLY );
      if ( vivaldi_coordinates_file != NULL )
      {
        for ( ;; )
        {
          //x,y,local_error => 3 doubles
          yidl::runtime::StackBuffer<3*sizeof(double)> xdr_buffer;
          if ( vivaldi_coordinates_file->read( xdr_buffer.incRef() ) == 3*sizeof(double) )
          {
            
            YIELD::platform::XDRUnmarshaller xdr_unmarshaller( xdr_buffer.incRef() );
            my_vivaldi_coordinates.unmarshal( xdr_unmarshaller );
            
            //TOFIX: All these messages will be redirected to LOG_DEBUG instead of LOG_INFO
            get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "coordinates readed from file:("<<my_vivaldi_coordinates.get_x_coordinate()<<","<<my_vivaldi_coordinates.get_y_coordinate()<<")";
                        
          }else
          {
            break;
          }          
        }
        vivaldi_coordinates_file->close();
      }else{
        get_log()->getStream( YIELD::platform::Log::LOG_WARNING ) << "impossible to read coordinates from file.Initializing them by default...";
      }

      VivaldiNode own_node(my_vivaldi_coordinates);
      
      long vivaldiIterations = 0;
      
      org::xtreemfs::interfaces::ServiceSet osd_services;
      
      std::vector<uint64_t> currentRetries;
      int retriesInARow = 0;
      
      org::xtreemfs::interfaces::Service *random_osd_service;
    
      
      //TODO:Remove this code after evaluating the system
      org::xtreemfs::interfaces::ServiceSet testingSets[NUMBER_OF_FILES];
      
      if(EVALUATION_ENABLED){
        
        get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "evaluation is enabled";
        
        org::xtreemfs::interfaces::ServiceSet knownOSDs;
        updateKnownOSDs(knownOSDs);
        
        if(!knownOSDs.empty()){
          
          for(int i=0;i<NUMBER_OF_FILES;i++){
            
            //Create testing set for file i
            composeTestingSet(testingSets[i],knownOSDs,REPLICAS_PER_FILE);
            get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "testing set created:" << testingSets[i].size();
            
            for(org::xtreemfs::interfaces::ServiceSet::iterator iter=testingSets[i].begin();iter!=testingSets[i].end();iter++){
              get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << (*iter).get_uuid();              
            }
            get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "-------";
            
            // Create/truncate results file i
            char filename[128];
            memset(filename,0,128);
            const char *fPath=vivaldi_coordinates_file_path;
            SPRINTF_VIV(filename,128,RES_FILE_NAME,fPath,i);
            YIELD::platform::auto_File truncatedFile1 = YIELD::platform::Volume().open( filename, O_CREAT|O_TRUNC|O_WRONLY );
            
            char resContent[256];
            memset(resContent,0,256);
            SPRINTF_VIV(resContent,256, "#MAX_MOV_RAT:%.3f\n#RETRIES:%d\n#MIN:%ld\n#CHECK_EVERY:%d\n",
                                        MAX_MOVEMENT_RATIO,
                                        MAX_RETRIES_FOR_A_REQUEST,
                                        MIN_RECALCULATION_IN_MS,
                                        CHECK_EVERY_ITERATIONS);
            truncatedFile1->write(resContent,strlen(resContent));
            truncatedFile1->close();
            // Create/truncate recals file i
            SPRINTF_VIV(filename,128,RECAL_FILE_NAME,fPath);
            YIELD::platform::auto_File truncatedFile2 = YIELD::platform::Volume().open( filename, O_CREAT|O_TRUNC|O_WRONLY );
            truncatedFile2->close(); 

          }

        }
      }
      //-------------
  
      
  		for ( ;; )
  		{
  			try
  			{

          //Get a list of OSDs from the DS
          if( (vivaldiIterations%ITERATIONS_BEFORE_UPDATING) == 1)
          {
            updateKnownOSDs(osd_services);
            currentRetries.clear(); //The pending retries are discarded, beacause the old OSDs might not be in the new list
            retriesInARow = 0;
          }
          
  				if ( !osd_services.empty() )
  				{
  					
            if(retriesInARow==0){
              //Choose one OSD randomly, only if there's no pending retry
              random_osd_service = &osd_services[std::rand() % osd_services.size()];
            }

  					yidl::runtime::auto_Object<org::xtreemfs::interfaces::AddressMappingSet> random_osd_address_mappings = dir_proxy->getAddressMappingsFromUUID( random_osd_service->get_uuid() );
  
  					//Several mappings for the same UUID
  					for ( org::xtreemfs::interfaces::AddressMappingSet::iterator random_osd_address_mapping_i = random_osd_address_mappings->begin();
                  random_osd_address_mapping_i != random_osd_address_mappings->end();
                  random_osd_address_mapping_i++ )
  					{
              	
  						if ( ( *random_osd_address_mapping_i ).get_protocol() == org::xtreemfs::interfaces::ONCRPCU_SCHEME )
  						{
  
  							auto_OSDProxy osd_proxy = OSDProxy::create( ( *random_osd_address_mapping_i ).get_uri(), get_proxy_flags(), get_log(), get_operation_timeout() );              
  							org::xtreemfs::interfaces::VivaldiCoordinates random_osd_vivaldi_coordinates;
  							
  							//Send the request and measure the RTT
                //TOFIX:Output must be LOG_DEBUG
                get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "recalculating against " << random_osd_service->get_uuid();

  							YIELD::platform::Time start_time;
  							osd_proxy->xtreemfs_ping( org::xtreemfs::interfaces::VivaldiCoordinates(), random_osd_vivaldi_coordinates,MAX_REQUEST_TIMEOUT_IN_NS );
  							YIELD::platform::Time rtt( YIELD::platform::Time() - start_time );
                get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "Ping response received.";
                
                //Next code is not executed if the ping request times out
                
                uint64_t measuredRTT = rtt.as_unix_time_ms();
                
                //TOFIX:This bool is useless once the code is evaluated 
                bool retried = false;
  							// Recalculate coordinates here
                if( retriesInARow < MAX_RETRIES_FOR_A_REQUEST ){
                  if( !own_node.recalculatePosition(random_osd_vivaldi_coordinates,measuredRTT,false) ){
                    
                    //The movement has been postponed because the measured RTT seems to be a peak
                    currentRetries.push_back(measuredRTT);
                    retriesInARow++;
                    retried = true;
                    
                  }else{
                    
                    //The movement has been accepted
                    currentRetries.clear();
                    retriesInARow = 0;
                    
                  }  
                }else{
                 
                  //Choose the lowest RTT
                  uint64_t lowestOne = measuredRTT;
                  for(std::vector<uint64_t>::iterator it = currentRetries.begin(); it<currentRetries.end(); it++){
                    if( (*it) < lowestOne ){
                      lowestOne = (*it);
                    }
                  }
                  
                  //Forcing recalculation because we've retried too many times
                  own_node.recalculatePosition(random_osd_vivaldi_coordinates,lowestOne,true);
                  currentRetries.clear();
                  retriesInARow = 0;
                  
                  //This is just to include in the trace the definitive RTT
                  measuredRTT = lowestOne;

                }
                
                //Print trace
                char auxStr[128];
                SPRINTF_VIV( auxStr,
                          128,
                          "%s:%lld(Viv:%.3f) Own:(%.3f,%.3f) lE=%.3f Rem:(%.3f,%.3f) rE=%.3f %s",
                            retried?"RETRY":"RTT",
                            measuredRTT,
                            own_node.calculateDistance((*own_node.getCoordinates()),random_osd_vivaldi_coordinates),
                            own_node.getCoordinates()->get_x_coordinate(),
                            own_node.getCoordinates()->get_y_coordinate(),
                            own_node.getCoordinates()->get_local_error(),
                            random_osd_vivaldi_coordinates.get_x_coordinate(),
                            random_osd_vivaldi_coordinates.get_y_coordinate(),
                            random_osd_vivaldi_coordinates.get_local_error(),
                            random_osd_service->get_uuid().data());
                
                char filename[128];
                memset(filename,0,128);
                const char *fPath=vivaldi_coordinates_file_path;
                SPRINTF_VIV(filename,128,RECAL_FILE_NAME,fPath);
                YIELD::platform::auto_File recals_file = YIELD::platform::Volume().open( filename, O_CREAT|O_APPEND|O_WRONLY );
                if ( recals_file != NULL )
                {
                  recals_file->write( auxStr,strlen(auxStr) );
                  recals_file->close();
                }else{
                  get_log()->getStream( YIELD::platform::Log::LOG_ERR ) << "Impossible to open recals file";
                }
                
                get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << auxStr;
  
  						}
  					}
  				}else{
            get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "no OSD available";
          }
  			
        }catch ( std::exception& exc ){
  				get_log()->getStream( YIELD::platform::Log::LOG_ERR ) << "xtfs_vivaldi: error pinging OSDs: " << exc.what() << ".";
          
          //We must avoid to keep retrying indefinitely against an OSD which is not responding
  				if(retriesInARow && (retriesInARow++ >= MAX_RETRIES_FOR_A_REQUEST) ){
            retriesInARow = 0;
          }
  			}
  
        //Store the new coordinates in a local file
        //TOFIX:Uncomment once finished
        //get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "storing coordinates in file:("<<own_node.getCoordinates()->get_x_coordinate()<<","<<own_node.getCoordinates()->get_y_coordinate()<<")";
        
  			vivaldi_coordinates_file = YIELD::platform::Volume().open( vivaldi_coordinates_file_path, O_CREAT|O_TRUNC|O_WRONLY );
  			if ( vivaldi_coordinates_file != NULL )
  			{
  				YIELD::platform::XDRMarshaller xdr_marshaller;
  				own_node.getCoordinates()->marshal( xdr_marshaller );
  				vivaldi_coordinates_file->write( xdr_marshaller.get_buffer().release() );
          vivaldi_coordinates_file->close();
  			}
    
        //Sleep until the next iteration
        uint64_t sleep_in_ms = MIN_RECALCULATION_IN_MS + ( (static_cast<double>(std::rand())/(RAND_MAX-1)) * (MAX_RECALCULATION_IN_MS - MIN_RECALCULATION_IN_MS) );
        get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "sleeping during "<<sleep_in_ms<<" ms.";
      	YIELD::platform::Thread::sleep( sleep_in_ms * NS_IN_MS );
        
        vivaldiIterations = (vivaldiIterations+1)%LONG_MAX;
        
        //Remove this code after evaluating the results
        if( EVALUATION_ENABLED && (vivaldiIterations%CHECK_EVERY_ITERATIONS) == 0){
          
          get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "**EVALUATING(its.:"<<vivaldiIterations<<")**";
          for(int i=0;i<NUMBER_OF_FILES;i++){
            char filename[128];
            memset(filename,0,128);
            const char *fPath=vivaldi_coordinates_file_path;
            SPRINTF_VIV(filename,128,RES_FILE_NAME,fPath,i);
            //executeOneEvaluation( testingSets[i], own_node,filename); The test will run allways with the whole sample
            executeOneEvaluation( osd_services, own_node,filename);
          }
        }
        //------------------
  		}
    }
    
    /* Retrieves a list of available OSDs from the DS
     */
    void updateKnownOSDs(org::xtreemfs::interfaces::ServiceSet &osds){
      try{
        dir_proxy->xtreemfs_service_get_by_type( org::xtreemfs::interfaces::SERVICE_TYPE_OSD, osds );
  
        org::xtreemfs::interfaces::ServiceSet::iterator ss_iterator = osds.begin();
        while( ss_iterator != osds.end() ){
          
          if( (*ss_iterator).get_last_updated_s() == 0)
          {
            osds.erase(ss_iterator);
          }else
          {
            ss_iterator++;
          }
        }
      }catch( std::exception ex ){
        get_log()->getStream( YIELD::platform::Log::LOG_ERR ) << "Impossible to update known OSDs.";
      }
 
    }
    /********************************************
     * Only to evaluate the results. These methods will be removed
     */
    void composeTestingSet(org::xtreemfs::interfaces::ServiceSet &testingSet,org::xtreemfs::interfaces::ServiceSet knownOSDs,int replicas_per_file){

      org::xtreemfs::interfaces::ServiceSet::iterator it;
      for(int i=0;i<replicas_per_file;i++){

        int chosenIndex = static_cast<int>( (static_cast<double>(std::rand())/(RAND_MAX-1))*knownOSDs.size());
        it = knownOSDs.begin() + chosenIndex;
        
        testingSet.push_back(*it);
        
        knownOSDs.erase(it);
        
        if(knownOSDs.empty()){
          break;
        }
      }
    }
    
    /*
     * Evaluates the position of the node according to the RTTs measured against a set of OSDs and
     * its vivaldi distances to them.Moreover, it stores the results in a local file.
     */ 
    void executeOneEvaluation(const org::xtreemfs::interfaces::ServiceSet &osds,VivaldiNode &own_node,const char *fileName){
  
      if ( !osds.empty() )
      {
        //Create arrays
        std::vector<uint64_t> rtts(osds.get_size());
        std::vector<std::string> uuids(osds.get_size());

        std::vector<org::xtreemfs::interfaces::VivaldiCoordinates> remoteCoordinates(osds.get_size());

        for(size_t i=0;i<osds.get_size();i++){

          const org::xtreemfs::interfaces::Service& one_osd = osds[i];
          
          uuids[i] = one_osd.get_uuid();
          
          try{                    
          
            yidl::runtime::auto_Object<org::xtreemfs::interfaces::AddressMappingSet> one_osd_address_mappings = dir_proxy->getAddressMappingsFromUUID( one_osd.get_uuid() );
          
            //Several mappings for the same UUID
            for ( org::xtreemfs::interfaces::AddressMappingSet::iterator one_osd_address_mapping_i = one_osd_address_mappings->begin(); one_osd_address_mapping_i != one_osd_address_mappings->end(); one_osd_address_mapping_i++ )
            {
              if ( ( *one_osd_address_mapping_i ).get_protocol() == org::xtreemfs::interfaces::ONCRPCU_SCHEME )
              {
                auto_OSDProxy osd_proxy = OSDProxy::create( ( *one_osd_address_mapping_i ).get_uri(), get_proxy_flags(), get_log(), get_operation_timeout() );
  
                try{
                  //The sent coordinates are irrelevant
                  org::xtreemfs::interfaces::VivaldiCoordinates ownCoords;
                  YIELD::platform::Time start_time;
                  //TOFIX:Is this getting blocked indefinitely?
                  get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "Requesting:" << one_osd.get_uuid();
                  osd_proxy->xtreemfs_ping(ownCoords , remoteCoordinates[i]);
                  YIELD::platform::Time rtt( YIELD::platform::Time() - start_time );
                  get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << "Ping response received.";
                  
                  rtts[i] = rtt.as_unix_time_ms();
                  if(rtts[i]==0) rtts[i]=1;
                  
                }catch( std::exception er ){
                  rtts[i] = 0;
                  //std::cout<<"ExceptionResponse:"<< er.what() <<":::" << er.get_errno()<<"--->"<< er.get_type_name()<<":"<<"\n";
                  //continue;
                }
              }
            }
          }catch(std::exception ex){
            get_log()->getStream( YIELD::platform::Log::LOG_ERR ) << "Impossible to determine addressMappings";
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
          if(rtts[i]>0){ rightMeasures++; }
        }
        
        char auxStr[256];
        
        
        std::string strWr("");
        strWr += "CLIENT";
        SPRINTF_VIV(auxStr,256," %d",rightMeasures);
        strWr += auxStr;
        SPRINTF_VIV(auxStr,256," %d/%d/%d-%d:%d:%d", tmStruct->tm_mday,tmStruct->tm_mon,tmStruct->tm_year, tmStruct->tm_hour, tmStruct->tm_min,tmStruct->tm_sec);
        strWr += auxStr;
        org::xtreemfs::interfaces::VivaldiCoordinates *localCoords = own_node.getCoordinates();
        SPRINTF_VIV(auxStr,256," Coordinates:%.3f/%.3f/%.3f\n",localCoords->get_x_coordinate(),localCoords->get_y_coordinate(),localCoords->get_local_error());
        strWr += auxStr;
        
        for(size_t i=0;i<osds.get_size();i++)
        {
          if(rtts[i]>0)
          {
            double distance = own_node.calculateDistance((*localCoords),remoteCoordinates[i]);
            SPRINTF_VIV(auxStr,256,"%s %lld\t%.3f\t%.3f/%.3f/%.3f\n",uuids[i].data(), rtts[i],distance,remoteCoordinates[i].get_x_coordinate(),remoteCoordinates[i].get_y_coordinate(),remoteCoordinates[i].get_local_error());
            strWr += auxStr;
          }
        }
        
        std::cout << strWr;

        YIELD::platform::auto_File results_file = YIELD::platform::Volume().open( fileName, O_CREAT|O_APPEND|O_WRONLY );
        if ( results_file != NULL )
        {
          results_file->write( strWr.data(),strWr.length());
          results_file->close();
        }else{
          get_log()->getStream( YIELD::platform::Log::LOG_ERR ) << "Impossible to open results file";
        }

      }
    }
    
  };
};


int main( int argc, char** argv )
{
  return xtfs_vivaldi::Main().main( argc, argv );
}
