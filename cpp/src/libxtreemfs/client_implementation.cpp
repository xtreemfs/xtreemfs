/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *               2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/client_implementation.h"

#include <cstdlib>

#include <boost/bind.hpp>
#include <boost/thread/thread.hpp>

#include "libxtreemfs/async_write_handler.h"
#include "libxtreemfs/execute_sync_request.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/pbrpc_url.h"
#include "libxtreemfs/uuid_iterator.h"
#include "libxtreemfs/volume_implementation.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"
#include "util/error_log.h"
#include "xtreemfs/DIRServiceClient.h"
#include "xtreemfs/MRCServiceClient.h"
#include "xtreemfs/OSDServiceClient.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {

ClientImplementation::ClientImplementation(
    const ServiceAddresses& dir_service_addresses,
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const xtreemfs::rpc::SSLOptions* ssl_options,
    const Options& options)
    : was_shutdown_(false),
      dir_service_auth_(),
      dir_service_user_credentials_(user_credentials),
      dir_service_ssl_options_(ssl_options),
      options_(options),
      list_open_volumes_(),
      uuid_cache_(),
      network_client_(NULL),
      network_client_thread_(NULL),
      dir_service_client_(NULL),
      client_uuid_("") {
  for (ServiceAddresses::const_iterator iter
           = dir_service_addresses.begin();
       iter != dir_service_addresses.end();
       ++iter) {
    dir_service_addresses_.AddUUID(*iter);
  }

  // Set bogus auth object.
  auth_bogus_.set_auth_type(AUTH_NONE);

  // Currently no AUTH is needed to access the DIR.
  dir_service_auth_.set_auth_type(AUTH_NONE);

  initialize_logger(options.log_level_string,
                    options.log_file_path,
                    LEVEL_WARN);
  initialize_error_log(20);

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "Created a new libxtreemfs Client "
        "object (version " << options.version_string << ")" << endl;
  }
}

ClientImplementation::~ClientImplementation() {
  Shutdown();
  if (!list_open_volumes_.empty()) {
    string error = "Client::~Client(): Not all XtreemFS volumes were closed."
        " Did you forget to call Client::Shutdown()? Memory leaks are the"
        " consequence.";
    Logging::log->getLog(LEVEL_ERROR) << error << endl;
    ErrorLog::error_log->AppendError(error);
  }

  network_client_->shutdown();
  network_client_thread_->join();

  // Since we wait for outstanding requests, the RPC client (network_client_)
  // has to shutdown first and then we can wait for the Vivaldi thread.
  // The other way around a deadlock might occur.
  if (vivaldi_thread_.get() && vivaldi_thread_->joinable()) {
    vivaldi_thread_->join();
  }

  atexit(google::protobuf::ShutdownProtobufLibrary);

  shutdown_logger();
  shutdown_error_log();
}

void ClientImplementation::Start() {
  // start network (rpc) client
  network_client_.reset(new xtreemfs::rpc::Client(
      options_.connect_timeout_s,
      options_.request_timeout_s,
      options_.linger_timeout_s,
      dir_service_ssl_options_));

  network_client_thread_.reset(
      new boost::thread(boost::bind(&xtreemfs::rpc::Client::run,
                                    network_client_.get())));

  dir_service_client_.reset(new DIRServiceClient(network_client_.get()));

  GenerateVersion4UUID(&client_uuid_);
  assert(!client_uuid_.empty());

  // Start vivaldi thread if configured
  if (options_.vivaldi_enable) {
    if (Logging::log->loggingActive(LEVEL_INFO)) {
      Logging::log->getLog(LEVEL_INFO)
          << "Starting vivaldi..." << endl;
    }
    vivaldi_.reset(new Vivaldi(network_client_.get(),
                               dir_service_client_.get(),
                               &dir_service_addresses_,
                               this->GetUUIDResolver(),
                               options_));
    vivaldi_thread_.reset(new boost::thread(boost::bind(&xtreemfs::Vivaldi::Run,
                                                        vivaldi_.get())));
  }

  async_write_callback_thread_.reset(
      new boost::thread(&xtreemfs::AsyncWriteHandler::ProcessCallbacks));
}

void ClientImplementation::Shutdown() {
  if (!was_shutdown_) {
    was_shutdown_ = true;
    boost::mutex::scoped_lock lock(list_open_volumes_mutex_);

    // Issue Close() on every Volume and remove it's pointer.
    list<VolumeImplementation*>::iterator it;
    while (!list_open_volumes_.empty()) {
      it = list_open_volumes_.begin();
      (*it)->CloseInternal();
      delete *it;
      it = list_open_volumes_.erase(it);
    }

    if (async_write_callback_thread_->joinable()) {
      async_write_callback_thread_->interrupt();
      async_write_callback_thread_->join();
    }

    // Stop vivaldi thread if running
    if (vivaldi_thread_.get() && vivaldi_thread_->joinable()) {
      vivaldi_thread_->interrupt();
    }
  }
}

Volume* ClientImplementation::OpenVolume(
    const std::string& volume_name,
    const xtreemfs::rpc::SSLOptions* ssl_options,
    const Options& options) {
  // TODO(mberlin): Fix possible leak through the use of scoped_ptr and swap().
  SimpleUUIDIterator* mrc_uuid_iterator = new SimpleUUIDIterator;
  VolumeNameToMRCUUID(volume_name, mrc_uuid_iterator);

  VolumeImplementation* volume = new VolumeImplementation(
      this,
      client_uuid_,
      mrc_uuid_iterator,
      volume_name,
      ssl_options,
      options);
  {
    boost::mutex::scoped_lock lock(list_open_volumes_mutex_);
    list_open_volumes_.push_back(volume);
  }
  volume->Start();

  return volume;
}

void ClientImplementation::CloseVolume(xtreemfs::Volume* volume) {
  boost::mutex::scoped_lock lock(list_open_volumes_mutex_);

  // Find given volume pointer address in list of open volumes and erase it.
  // Free it afterwards.
  list<VolumeImplementation*>::iterator it;
  for (it = list_open_volumes_.begin(); it != list_open_volumes_.end(); ++it) {
    if (*it == volume) {
      // Free Volume object.
      delete *it;
      it = list_open_volumes_.erase(it);
    }
  }
}

void ClientImplementation::CreateVolume(
    const std::string& mrc_address,
    const xtreemfs::pbrpc::Auth& auth,
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& volume_name,
    int mode,
    const std::string& owner_username,
    const std::string& owner_groupname,
    const xtreemfs::pbrpc::AccessControlPolicyType& access_policy,
    const xtreemfs::pbrpc::StripingPolicyType& default_striping_policy_type,
    int default_stripe_size,
    int default_stripe_width,
    const std::list<xtreemfs::pbrpc::KeyValuePair*>& volume_attributes) {
  MRCServiceClient mrc_service_client(network_client_.get());

  xtreemfs::pbrpc::Volume new_volume;
  new_volume.set_id("");
  new_volume.set_mode(mode);
  new_volume.set_name(volume_name);
  new_volume.set_owner_user_id(owner_username);
  new_volume.set_owner_group_id(owner_groupname);
  new_volume.set_access_control_policy(access_policy);
  new_volume.mutable_default_striping_policy()
      ->set_type(default_striping_policy_type);
  new_volume.mutable_default_striping_policy()
      ->set_stripe_size(default_stripe_size);
  new_volume.mutable_default_striping_policy()->set_width(default_stripe_width);
  for (list<KeyValuePair*>::const_iterator it = volume_attributes.begin();
       it != volume_attributes.end();
       ++it) {
    new_volume.add_attrs();
    new_volume.mutable_attrs(new_volume.attrs_size() - 1)->set_key((*it)->key());
    new_volume.mutable_attrs(new_volume.attrs_size() - 1)
        ->set_value((*it)->value());
  }

  SimpleUUIDIterator temp_uuid_iterator_with_addresses;
  temp_uuid_iterator_with_addresses.AddUUID(mrc_address);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::xtreemfs_mkvol_sync,
              &mrc_service_client,
              _1,
              boost::cref(auth),
              boost::cref(user_credentials),
              &new_volume),
          &temp_uuid_iterator_with_addresses,
          NULL,
          options_.max_tries,
          options_,
          true,
          false));
  response->DeleteBuffers();
}

void ClientImplementation::DeleteVolume(
    const std::string& mrc_address,
    const xtreemfs::pbrpc::Auth& auth,
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& volume_name) {
  MRCServiceClient mrc_service_client(network_client_.get());

  xtreemfs_rmvolRequest rmvol_request;
  rmvol_request.set_volume_name(volume_name);

  SimpleUUIDIterator temp_uuid_iterator_with_addresses;
  temp_uuid_iterator_with_addresses.AddUUID(mrc_address);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::xtreemfs_rmvol_sync,
              &mrc_service_client,
              _1,
              boost::cref(auth),
              boost::cref(user_credentials),
              &rmvol_request),
          &temp_uuid_iterator_with_addresses,
          NULL,
          options_.max_tries,
          options_,
          true,
          false));
  response->DeleteBuffers();
}

xtreemfs::pbrpc::Volumes* ClientImplementation::ListVolumes(
    const std::string& mrc_address,
    const xtreemfs::pbrpc::Auth& auth) {
  SimpleUUIDIterator temp_uuid_iterator_with_addresses;
  temp_uuid_iterator_with_addresses.AddUUID(mrc_address);

  return ListVolumes(&temp_uuid_iterator_with_addresses, auth);
}

xtreemfs::pbrpc::Volumes* ClientImplementation::ListVolumes(
    UUIDIterator* uuid_iterator_with_mrc_addresses,
    const xtreemfs::pbrpc::Auth& auth) {
  // Create a MRCServiceClient
  MRCServiceClient mrc_service_client(network_client_.get());
  // Use bogus user_credentials;
  UserCredentials user_credentials;
  user_credentials.set_username("xtreemfs");

  // Retrieve the list of volumes from the MRC.
  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::xtreemfs_lsvol_sync,
              &mrc_service_client,
              _1,
              boost::cref(auth),
              boost::cref(user_credentials)),
          uuid_iterator_with_mrc_addresses,
          NULL,
          options_.max_tries,
          options_,
          true,
          false));

  // Delete everything except the response.
  delete[] response->data();
  delete response->error();

  // Return the list of volumes.
  return static_cast<xtreemfs::pbrpc::Volumes*>(response->response());
}

void ClientImplementation::UUIDToAddress(const std::string& uuid,
                                         std::string* address) {
  UUIDToAddress(uuid, address, options_);
}

void ClientImplementation::UUIDToAddress(const std::string& uuid,
                                         std::string* address,
                                         const Options& options) {
  // The UUID must never be empty.
  assert(!uuid.empty());

  // Try to search in cache.
  *address = uuid_cache_.get(uuid);
  if (!address->empty()) {
    return;  // Cache-Hit.
  }

  addressMappingGetRequest rq = addressMappingGetRequest();
  rq.set_uuid(uuid);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::DIRServiceClient::
                  xtreemfs_address_mappings_get_sync,
              dir_service_client_.get(),
              _1,
              boost::cref(dir_service_auth_),
              boost::cref(dir_service_user_credentials_),
              &rq),
          &dir_service_addresses_,
          NULL,
          options.max_tries,
          options,
          true,
          false));

  AddressMappingSet* set = static_cast<AddressMappingSet*>(
      response->response());
  for (int i = 0; i < set->mappings_size(); i++) {
    if (set->mappings(i).protocol() == PBRPCURL::SCHEME_PBRPC
        || set->mappings(i).protocol() == PBRPCURL::SCHEME_PBRPCS
        || set->mappings(i).protocol() == PBRPCURL::SCHEME_PBRPCG
        || set->mappings(i).protocol() == PBRPCURL::SCHEME_PBRPCU) {
      AddressMapping a = set->mappings(i);
      uuid_cache_.update(uuid, a.address(), a.port(), a.ttl_s());

      ostringstream s;
      s << set->mappings(i).address() << ":" << set->mappings(i).port();

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
            << "UUID: found service for uuid " << s.str() << endl;
      }
      response->DeleteBuffers();
      *address = s.str();
      return;
    } else {
      Logging::log->getLog(LEVEL_ERROR)
          << "Unknown scheme: " << set->mappings(i).protocol() << endl;
      response->DeleteBuffers();
      throw UnknownAddressSchemeException("Unknown scheme: " +
          set->mappings(i).protocol());
    }
  }

  Logging::log->getLog(LEVEL_ERROR)
      << "UUID: service not found for uuid " << uuid << endl;
  response->DeleteBuffers();
  throw AddressToUUIDNotFoundException(uuid);
}

void ClientImplementation::VolumeNameToMRCUUID(const std::string& volume_name,
                                               std::string* mrc_uuid) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG)
         << "MRC: searching volume on MRC: " << volume_name << endl;
  }

  // Check if there is a @ in the volume_name.
  // Everything behind the @ has to be removed as it identifies the snapshot.
  string parsed_volume_name = volume_name;
  size_t at_pos = volume_name.find("@");
  if (at_pos != string::npos) {
    parsed_volume_name = volume_name.substr(0, at_pos);
  }

  serviceGetByNameRequest rq = serviceGetByNameRequest();
  rq.set_name(parsed_volume_name);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::DIRServiceClient::
                  xtreemfs_service_get_by_name_sync,
              dir_service_client_.get(),
              _1,
              boost::cref(dir_service_auth_),
              boost::cref(dir_service_user_credentials_),
              &rq),
          &dir_service_addresses_,
          NULL,
          options_.max_tries,
          options_,
          true,
          false));

  ServiceSet* service_set = static_cast<ServiceSet*>(response->response());
  *mrc_uuid = "";
  for (int i = 0; i < service_set->services_size(); i++) {
    Service service = service_set->services(i);
    if ((service.type() == SERVICE_TYPE_VOLUME)
          && (service.name() == parsed_volume_name)) {
      const ServiceDataMap& data = service.data();
      for (int j = 0; j < data.data_size(); j++) {
        if (data.data(j).key() == "mrc") {
          *mrc_uuid = data.data(j).value();
          break;
        }
      }
    }
  }

  response->DeleteBuffers();
  if (mrc_uuid->empty()) {
    Logging::log->getLog(LEVEL_ERROR) << "No MRC found for volume: "
        << volume_name << std::endl;
    throw VolumeNotFoundException(volume_name);
  }
}

void ClientImplementation::VolumeNameToMRCUUID(const std::string& volume_name,
                                               SimpleUUIDIterator* uuid_iterator) {
  assert(uuid_iterator);

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG)
         << "MRC: searching volume on MRC: " << volume_name << endl;
  }

  // Check if there is a @ in the volume_name.
  // Everything behind the @ has to be removed as it identifies the snapshot.
  string parsed_volume_name = volume_name;
  size_t at_pos = volume_name.find("@");
  if (at_pos != string::npos) {
    parsed_volume_name = volume_name.substr(0, at_pos);
  }

  serviceGetByNameRequest rq = serviceGetByNameRequest();
  rq.set_name(parsed_volume_name);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::DIRServiceClient::
                  xtreemfs_service_get_by_name_sync,
              dir_service_client_.get(),
              _1,
              boost::cref(dir_service_auth_),
              boost::cref(dir_service_user_credentials_),
              &rq),
          &dir_service_addresses_,
          NULL,
          options_.max_tries,
          options_,
          true,
          false));

  bool mrc_found = false;
  ServiceSet* service_set = static_cast<ServiceSet*>(response->response());
  for (int i = 0; i < service_set->services_size(); i++) {
    Service service = service_set->services(i);
    if ((service.type() == SERVICE_TYPE_VOLUME)
          && (service.name() == parsed_volume_name)) {
      const ServiceDataMap& data = service.data();
      for (int j = 0; j < data.data_size(); j++) {
        if (data.data(j).key().substr(0, 3) == "mrc") {
          if (Logging::log->loggingActive(LEVEL_DEBUG)) {
            Logging::log->getLog(LEVEL_DEBUG)
                << "MRC with UUID: " << data.data(j).value()
                << " added (key: " << data.data(j).key() << ")." << std::endl;
          }
          uuid_iterator->AddUUID(data.data(j).value());
          mrc_found = true;
        }
      }
    }
  }

  response->DeleteBuffers();
  if (!mrc_found) {
    Logging::log->getLog(LEVEL_ERROR) << "No MRC found for volume: "
        << volume_name << std::endl;
    throw VolumeNotFoundException(volume_name);
  }
}

/** ClientImplementation already implements UUIDResolver and therefore this
 *  returns just a cast to this. */
UUIDResolver* ClientImplementation::GetUUIDResolver() {
  return static_cast<UUIDResolver*>(this);
}


const VivaldiCoordinates& ClientImplementation::GetVivaldiCoordinates() const {
  return vivaldi_->GetVivaldiCoordinates();
}

}  // namespace xtreemfs
