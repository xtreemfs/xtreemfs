/*
 * Copyright (c)  2009 Juan Gonzalez de Benito,
 *                2011 Bjoern Kolbeck (Zuse Institute Berlin),
 *                2012 Matthias Noack (Zuse Institute Berlin)
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/vivaldi.h"

#include <boost/bind.hpp>
#include <boost/date_time/posix_time/posix_time_types.hpp>
#include <boost/lexical_cast.hpp>
#include <algorithm>
#include <fstream>
#include <string>
#include <vector>

#include "libxtreemfs/execute_sync_request.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/pbrpc_url.h"
#include "libxtreemfs/simple_uuid_iterator.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"
#include "util/zipf_generator.h"
#include "xtreemfs/DIRServiceClient.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/OSDServiceClient.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {
    
Vivaldi::Vivaldi(
    SimpleUUIDIterator& dir_uuid_iterator,
    UUIDResolver* uuid_resolver,
    const Options& options)
    : dir_uuid_iterator_(dir_uuid_iterator),
      uuid_resolver_(uuid_resolver),
      vivaldi_options_(options) {
  srand(static_cast<unsigned int>(time(NULL)));
  // Set AuthType to AUTH_NONE as it's currently not used.
  auth_bogus_.set_auth_type(AUTH_NONE);
  // Set username "xtreemfs" as it does not get checked at server side.
  user_credentials_bogus_.set_username("xtreemfs");

  // Vivaldi requests do not have to be retried nor interrupted.
  vivaldi_options_.max_tries = 1;
  vivaldi_options_.was_interrupted_function = NULL;
}

void Vivaldi::Initialize(rpc::Client* rpc_client) {
  dir_client_.reset(new pbrpc::DIRServiceClient(rpc_client));
  osd_client_.reset(new pbrpc::OSDServiceClient(rpc_client));
}

void Vivaldi::Run() {
  assert(dir_client_.get() != NULL);
  assert(osd_client_.get() != NULL);

  bool loaded_from_file = false;
  ifstream vivaldi_coordinates_file(vivaldi_options_.vivaldi_filename.c_str());
  if (vivaldi_coordinates_file.is_open()) {
    my_vivaldi_coordinates_.ParseFromIstream(&vivaldi_coordinates_file);
    loaded_from_file = my_vivaldi_coordinates_.IsInitialized();
    if (!loaded_from_file) {
        Logging::log->getLog(LEVEL_ERROR)
            << "Vivaldi: Could not load coordinates from file: "
            << my_vivaldi_coordinates_.InitializationErrorString() << endl;
      my_vivaldi_coordinates_.Clear();
    }
    vivaldi_coordinates_file.close();
  }

  if (!loaded_from_file) {
    if (Logging::log->loggingActive(LEVEL_INFO)) {
      Logging::log->getLog(LEVEL_INFO)
          << "Vivaldi: Coordinates file does not exist or could not be parsed,"
          << "starting with empty coordinates." << endl
          << "Initialization might take some time." << endl;
    }

    // Initialize coordinates to (0,0) by default
    my_vivaldi_coordinates_.set_local_error(0.0);
    my_vivaldi_coordinates_.set_x_coordinate(0.0);
    my_vivaldi_coordinates_.set_y_coordinate(0.0);
  }

  VivaldiNode own_node(my_vivaldi_coordinates_);

  uint64_t vivaldi_iterations = 0;

  list<KnownOSD> known_osds;
  bool valid_known_osds = false;

  vector<uint64_t> current_retries;
  int retries_in_a_row = 0;
  list<KnownOSD>::iterator chosen_osd_service;
  ZipfGenerator rank_generator(vivaldi_options_.vivaldi_zipf_generator_skew);

  for (;;) {
    boost::scoped_ptr<rpc::SyncCallbackBase> ping_response;
    try {
      // Get a list of OSDs from the DIR(s)
      if ((vivaldi_iterations %
               vivaldi_options_.vivaldi_max_iterations_before_updating) == 0) {
        valid_known_osds = UpdateKnownOSDs(&known_osds, own_node);
        if (valid_known_osds && !known_osds.empty()) {
          rank_generator.set_size(known_osds.size());
        }
        // The pending retries are discarded, because the old OSDs might not
        // be in the new list
        current_retries.clear();

        retries_in_a_row = 0;
        chosen_osd_service = known_osds.begin();
      }

      // There are known OSDs, ping one of them.
      if (valid_known_osds && !known_osds.empty()) {
        // Choose an OSD, only if there's no pending retry
        if (retries_in_a_row == 0) {
          int index = rank_generator.next();

          list<KnownOSD>::iterator known_iterator = known_osds.begin();
          for (int i = 0;
               (i < index) && (known_iterator != known_osds.end());
               known_iterator++, i++) {
            // Move the iterator over the chosen service
          }
          chosen_osd_service = known_iterator;
        }

        // Ping chosen OSD.
        xtreemfs_pingMesssage ping_message;
        ping_message.set_request_response(true);
        ping_message.mutable_coordinates()
            ->MergeFrom(*own_node.GetCoordinates());

        VivaldiCoordinates* random_osd_vivaldi_coordinates;

        if (Logging::log->loggingActive(LEVEL_DEBUG)) {
          Logging::log->getLog(LEVEL_DEBUG)
              << "Vivaldi: recalculating against: "
              << chosen_osd_service->GetUUID() << endl;
        }

        SimpleUUIDIterator pinged_osd;
        pinged_osd.AddUUID(chosen_osd_service->GetUUID());

        // execute sync ping
        try {
          // start timing
          boost::posix_time::ptime start_time(boost::posix_time
              ::microsec_clock::local_time());

          ping_response.reset(
              ExecuteSyncRequest(
                  boost::bind(
                      &xtreemfs::pbrpc::OSDServiceClient::xtreemfs_ping_sync,
                      osd_client_.get(),
                      _1,
                      boost::cref(auth_bogus_),
                      boost::cref(user_credentials_bogus_),
                      &ping_message),
                  &pinged_osd,
                  uuid_resolver_,
                  RPCOptionsFromOptions(vivaldi_options_)));

          // stop timing
          boost::posix_time::ptime end_time(
              boost::posix_time::microsec_clock::local_time());
          boost::posix_time::time_duration rtt = end_time - start_time;
          uint64_t measured_rtt = rtt.total_milliseconds();

          xtreemfs::pbrpc::xtreemfs_pingMesssage* ping_response_obj =
              static_cast<xtreemfs::pbrpc::xtreemfs_pingMesssage*>(
              ping_response->response());
          random_osd_vivaldi_coordinates = ping_response_obj->mutable_coordinates();

          if (Logging::log->loggingActive(LEVEL_DEBUG)) {
            Logging::log->getLog(LEVEL_DEBUG)
                << "Vivaldi: ping response received. Measured time: "
                << measured_rtt << " ms" << endl;
          }

          // Recalculate coordinates here
          if (retries_in_a_row < vivaldi_options_.vivaldi_max_request_retries) {
            if (!own_node.RecalculatePosition(*random_osd_vivaldi_coordinates,
                                              measured_rtt,
                                              false)) {
              // The movement has been postponed because the measured RTT
              // seems to be a peak
              current_retries.push_back(measured_rtt);
              retries_in_a_row++;
            } else {
              // The movement has been accepted
              current_retries.clear();
              retries_in_a_row = 0;
            }
          } else {
            // Choose the lowest RTT
            uint64_t lowest_rtt = measured_rtt;
            for (vector<uint64_t>::iterator retries_iterator =
                     current_retries.begin();
                retries_iterator < current_retries.end();
                ++retries_iterator) {
              if (*retries_iterator < lowest_rtt) {
                lowest_rtt = *retries_iterator;
              }
            }

            // Force recalculation after too many retries
            own_node.RecalculatePosition(*random_osd_vivaldi_coordinates,
                                         lowest_rtt,
                                         true);
            current_retries.clear();
            retries_in_a_row = 0;

            // set measured_rtt to the actually used one for trace output
            measured_rtt = lowest_rtt;
          }
        } catch (const XtreemFSException& e) {
          if (ping_response.get()) {
           ping_response->DeleteBuffers();
          }

          Logging::log->getLog(LEVEL_ERROR)
               << "Vivaldi: could not ping OSDs: " << e.what() << endl;

          // We must avoid to keep retrying indefinitely against an OSD which is not
          // responding
          if (retries_in_a_row > 0
             && (++retries_in_a_row >=
                 vivaldi_options_.vivaldi_max_request_retries)) {
           // If the last retry times out all the previous retries are discarded
           current_retries.clear();
           retries_in_a_row = 0;
          }
        }

        // update local coordinate copy here
        {
          boost::mutex::scoped_lock lock(coordinate_mutex_);
          my_vivaldi_coordinates_.CopyFrom(*own_node.GetCoordinates());
        }

        // Store the new coordinates in a local file
        if (Logging::log->loggingActive(LEVEL_DEBUG)) {
          Logging::log->getLog(LEVEL_DEBUG)
              << "Vivaldi: storing coordinates to file: ("
              << own_node.GetCoordinates()->x_coordinate() << ", "
              << own_node.GetCoordinates()->y_coordinate() << ")" << endl;
        }
        ofstream file_out(vivaldi_options_.vivaldi_filename.c_str(),
            ios_base::binary | ios_base::trunc);
        own_node.GetCoordinates()->SerializePartialToOstream(&file_out);
        file_out.close();

        // Update client coordinates at the DIR
        if (vivaldi_options_.vivaldi_enable_dir_updates) {
          if (Logging::log->loggingActive(LEVEL_DEBUG)) {
            Logging::log->getLog(LEVEL_DEBUG)
                << "Vivaldi: Sending coordinates to DIR." << endl;
          }
          boost::scoped_ptr<rpc::SyncCallbackBase> response;
          try {
            response.reset(
                ExecuteSyncRequest(
                    boost::bind(
                        &xtreemfs::pbrpc::DIRServiceClient
                            ::xtreemfs_vivaldi_client_update_sync,
                        dir_client_.get(),
                        _1,
                        boost::cref(auth_bogus_),
                        boost::cref(user_credentials_bogus_),
                        own_node.GetCoordinates()),
                    &dir_uuid_iterator_,
                    NULL,
                    RPCOptionsFromOptions(vivaldi_options_),
                    true));
            response->DeleteBuffers();
          } catch (const XtreemFSException& e) {
            if (response.get()) {
              response->DeleteBuffers();
            }
            if (Logging::log->loggingActive(LEVEL_INFO)) {
              Logging::log->getLog(LEVEL_INFO)
                  << "Vivaldi: Failed to send the updated client"
                      " coordinates to the DIR, error: "
                  << e.what() << endl;
            }
          }
        }
//        //Print a trace
//        char auxStr[256];
//        SPRINTF_VIV(auxStr,
//                    256,
//                    "%s:%lld(Viv:%.3f) Own:(%.3f,%.3f) lE=%.3f "
//                        "Rem:(%.3f,%.3f) rE=%.3f %s\n",
//                    retried ? "RETRY" : "RTT",
//                    static_cast<long long int> (measured_rtt),
//                    own_node.calculateDistance(
//                        (*own_node.getCoordinates()),
//                        random_osd_vivaldi_coordinates.get()),
//                    own_node.getCoordinates()->x_coordinate(),
//                    own_node.getCoordinates()->y_coordinate(),
//                    own_node.getCoordinates()->local_error(),
//                    random_osd_vivaldi_coordinates->x_coordinate(),
//                    random_osd_vivaldi_coordinates->y_coordinate(),
//                    random_osd_vivaldi_coordinates->local_error(),
//                    chosen_osd_service->get_uuid().data());
//        get_log()->getStream(YIELD::platform::Log::LOG_INFO) <<
//            "Vivaldi: " << auxStr;

        // Update OSD's coordinates
        chosen_osd_service->SetCoordinates(*random_osd_vivaldi_coordinates);

        // Re-sort known_osds
        // TODO(mno): Use a more efficient sort approach.
        list<KnownOSD> aux_osd_list(known_osds);
        KnownOSD chosen_osd_service_value = *chosen_osd_service;
        known_osds.clear();  // NOTE: this invalidates all ptrs and itrs

        for (list<KnownOSD>::reverse_iterator aux_iterator
                 = aux_osd_list.rbegin();
             aux_iterator != aux_osd_list.rend();
             aux_iterator++) {
          double new_osd_distance =
              own_node.CalculateDistance(
                  *(aux_iterator->GetCoordinates()),
                  *own_node.GetCoordinates());

          list<KnownOSD>::iterator known_iterator = known_osds.begin();

          while (known_iterator != known_osds.end()) {
            double old_osd_distance =  \
                  own_node.CalculateDistance(
                      *(known_iterator->GetCoordinates()),
                      *own_node.GetCoordinates());
            if (old_osd_distance >= new_osd_distance) {
              known_osds.insert(known_iterator, (*aux_iterator));
              break;
            } else {
              known_iterator++;
            }
          }

          if (known_iterator == known_osds.end()) {
            known_osds.push_back((*aux_iterator));
          }
        }  // end re-sorting

        // find the chosen OSD in the resorted list
        chosen_osd_service = find(known_osds.begin(),
                                  known_osds.end(),
                                  chosen_osd_service_value);
        assert(chosen_osd_service != known_osds.end());
        ping_response->DeleteBuffers();
      } else {
          if (Logging::log->loggingActive(LEVEL_WARN)) {
            Logging::log->getLog(LEVEL_WARN)
                << "Vivaldi: no OSD available." << endl;
          }
      }

      vivaldi_iterations = (vivaldi_iterations + 1) % LONG_MAX;

      // Sleep until the next iteration
      uint32_t sleep_in_s = static_cast<uint32_t>(
          vivaldi_options_.vivaldi_recalculation_interval_s -
          vivaldi_options_.vivaldi_recalculation_epsilon_s +
          (static_cast<double>(rand()) / (RAND_MAX - 1)) *
           2.0 * vivaldi_options_.vivaldi_recalculation_epsilon_s);

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
            << "Vivaldi: sleeping during " << sleep_in_s << " s." << endl;
      }

      boost::this_thread::sleep(boost::posix_time::seconds(sleep_in_s));
    } catch(const boost::thread_interrupted&) {
      if (ping_response.get()) {
        ping_response->DeleteBuffers();
      }
      break;
    }
  }
}  // Run()


bool Vivaldi::UpdateKnownOSDs(list<KnownOSD>* updated_osds,
                              const VivaldiNode& own_node) {
  // TODO(mno): Requesting the list of all OSDs does not scale with the number
  //            of services. Therefore, request only a subset of it.
  bool retval = true;
  boost::scoped_ptr<rpc::SyncCallbackBase> response;

  try {
    serviceGetByTypeRequest request;
    request.set_type(SERVICE_TYPE_OSD);

    response.reset(ExecuteSyncRequest(
        boost::bind(
            &xtreemfs::pbrpc::DIRServiceClient
                ::xtreemfs_service_get_by_type_sync,
            dir_client_.get(),
            _1,
            boost::cref(auth_bogus_),
            boost::cref(user_credentials_bogus_),
            &request),
        &dir_uuid_iterator_,
        NULL,
        RPCOptionsFromOptions(vivaldi_options_),
        true));

    ServiceSet* received_osds = static_cast<ServiceSet*>(response->response());
    updated_osds->clear();

    // Fill the list, ignoring every offline OSD
    for (int i = 0; i < received_osds->services_size(); i++) {
      const Service& service = received_osds->services(i);
      if (service.last_updated_s() > 0) {  // only online OSDs
        const ServiceDataMap& sdm = service.data();
        const string* coordinates_string = NULL;
        for (int j = 0; j < sdm.data_size(); ++j) {
          if (sdm.data(j).key() == "vivaldi_coordinates") {
            coordinates_string = &sdm.data(j).value();
            break;
          }
        }

        // If the DIR does not have the OSD's coordinates, we discard this
        // entry
        if (coordinates_string) {
          // Parse the coordinates provided by the DIR
          VivaldiCoordinates osd_coords;
          OutputUtils::StringToCoordinates(*coordinates_string, osd_coords);
          KnownOSD new_osd(service.uuid(), osd_coords);

          // Calculate the current distance from the client to the new OSD
          double new_osd_distance = own_node.CalculateDistance(
               *(own_node.GetCoordinates()),
               osd_coords);

          list<KnownOSD>::iterator up_iterator = updated_osds->begin();
          while (up_iterator != updated_osds->end()) {
            double old_osd_distance =
                own_node.CalculateDistance(*up_iterator->GetCoordinates(),
                                           *(own_node.GetCoordinates()));
            if (old_osd_distance >= new_osd_distance) {
              updated_osds->insert(up_iterator, new_osd);
              break;
            } else {
              up_iterator++;
            }
          }

          if (up_iterator == updated_osds->end()) {
            updated_osds->push_back(new_osd);
          }
        }  // if (coordinates_string)
      }
    }  // for
    response->DeleteBuffers();
  } catch (const XtreemFSException& e) {
    if (response.get()) {
      response->DeleteBuffers();
    }
    Logging::log->getLog(LEVEL_ERROR)
       << "Vivaldi: Failed to update known OSDs: " << e.what() << endl;
    retval = false;
  }

  return retval;
}  // update_known_osds

const VivaldiCoordinates& Vivaldi::GetVivaldiCoordinates() const {
  boost::mutex::scoped_lock lock(coordinate_mutex_);
  return my_vivaldi_coordinates_;
}

}  // namespace xtreemfs
