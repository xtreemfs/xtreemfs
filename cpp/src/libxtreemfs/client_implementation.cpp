/*
 * Copyright (c) 2011-2014 by Michael Berlin, Zuse Institute Berlin
 *               2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/client_implementation.h"

#include <cstdlib>

#include <boost/bind.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/thread/thread.hpp>

#include "libxtreemfs/async_write_handler.h"
#include "libxtreemfs/execute_sync_request.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/pbrpc_url.h"
#include "libxtreemfs/uuid_iterator.h"
#include "libxtreemfs/vivaldi.h"
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

DIRUUIDResolver::DIRUUIDResolver(
    SimpleUUIDIterator& dir_uuid_iterator,
    const pbrpc::UserCredentials& user_credentials,
    const Options& options)
    : dir_uuid_iterator_(dir_uuid_iterator),
      dir_service_user_credentials_(user_credentials),
      options_(options) {
  // Currently no AUTH is needed to access the DIR.
  dir_service_auth_.set_auth_type(AUTH_NONE);
}

void DIRUUIDResolver::Initialize(xtreemfs::rpc::Client* network_client) {
  dir_service_client_.reset(new DIRServiceClient(network_client));
}

void DIRUUIDResolver::UUIDToAddress(const std::string& uuid,
                                    std::string* address) {
  UUIDToAddressWithOptions(uuid, address, RPCOptionsFromOptions(options_));
}

void DIRUUIDResolver::UUIDToAddressWithOptions(const std::string& uuid,
                                               std::string* address,
                                               const RPCOptions& options) {
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
          &dir_uuid_iterator_,
          NULL,
          options,
          true));

  boost::unordered_set<string> local_networks = GetNetworks();
  AddressMappingSet* set = static_cast<AddressMappingSet*>(
      response->response());
  AddressMapping found_address_mapping;
  for (int i = 0; i < set->mappings_size(); i++) {
    const AddressMapping& am = set->mappings(i);
    if (am.protocol() != PBRPCURL::GetSchemePBRPC()
        && am.protocol() != PBRPCURL::GetSchemePBRPCS()
        && am.protocol() != PBRPCURL::GetSchemePBRPCG()
        && am.protocol() != PBRPCURL::GetSchemePBRPCU()) {
      Logging::log->getLog(LEVEL_ERROR)
          << "Unknown scheme: " << am.protocol() << endl;
      response->DeleteBuffers();
      throw UnknownAddressSchemeException("Unknown scheme: " + am.protocol());
    }

    const string& network = am.match_network();
    // Prefer the UUID for a matching network, use the default otherwise.
    if (network == "*") {
      found_address_mapping = am;
    } else {
      boost::unordered_set<string>::const_iterator local_network
        = local_networks.find(network);
      if (local_network != local_networks.end()) {
        found_address_mapping = am;
        break;
      }
    }
  }

  if (found_address_mapping.IsInitialized()) {
    uuid_cache_.update(uuid,
                       found_address_mapping.address(),
                       found_address_mapping.port(),
                       found_address_mapping.ttl_s());

    ostringstream s;
    s << found_address_mapping.address() << ":" << found_address_mapping.port();

    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "Service found for UUID: " << s.str() << endl;
    }
    response->DeleteBuffers();
    *address = s.str();
  } else {
    Logging::log->getLog(LEVEL_ERROR)
        << "Service not found for UUID: " << uuid << endl;
    response->DeleteBuffers();
    throw AddressToUUIDNotFoundException(uuid);
  }
}

string parse_volume_name(const std::string& volume_name) {
  // Check if there is a @ in the volume_name.
  // Everything behind the @ has to be removed as it identifies the snapshot.
  string parsed_volume_name = volume_name;
  size_t at_pos = volume_name.find("@");
  if (at_pos != string::npos) {
    parsed_volume_name = volume_name.substr(0, at_pos);
  }

  return parsed_volume_name;
}

void DIRUUIDResolver::VolumeNameToMRCUUID(const std::string& volume_name,
                                          std::string* mrc_uuid) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG)
         << "MRC: searching volume on MRC: " << volume_name << endl;
  }

  string parsed_volume_name = parse_volume_name(volume_name);
  boost::scoped_ptr<ServiceSet> service_set(GetServicesByName(parsed_volume_name));

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

  if (mrc_uuid->empty()) {
    Logging::log->getLog(LEVEL_ERROR) << "No MRC found for volume: "
        << volume_name << std::endl;
    throw VolumeNotFoundException(volume_name);
  }
}

void DIRUUIDResolver::VolumeNameToMRCUUID(const std::string& volume_name,
                                          SimpleUUIDIterator* uuid_iterator) {
  assert(uuid_iterator);

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG)
         << "MRC: searching volume on MRC: " << volume_name << endl;
  }

  string parsed_volume_name = parse_volume_name(volume_name);
  boost::scoped_ptr<ServiceSet> service_set(GetServicesByName(parsed_volume_name));

  bool mrc_found = false;
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

  if (!mrc_found) {
    Logging::log->getLog(LEVEL_ERROR) << "No MRC found for volume: "
        << volume_name << std::endl;
    throw VolumeNotFoundException(volume_name);
  }
}

vector<string> DIRUUIDResolver::VolumeNameToMRCUUIDs(const string& volume_name) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
     Logging::log->getLog(LEVEL_DEBUG)
         << "MRC: searching volume on MRC: " << volume_name << endl;
  }

  string parsed_volume_name = parse_volume_name(volume_name);
  boost::scoped_ptr<ServiceSet> service_set(GetServicesByName(parsed_volume_name));

  vector<string> result;
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
          result.push_back(data.data(j).value());
        }
      }
    }
  }

  if (result.empty()) {
    Logging::log->getLog(LEVEL_ERROR) << "No MRC found for volume: "
        << volume_name << std::endl;
    throw VolumeNotFoundException(volume_name);
  }

  return result;
}


ServiceSet* DIRUUIDResolver::GetServicesByName(const std::string& volume_name) {
  boost::scoped_ptr<rpc::SyncCallbackBase> response;
  try {
    serviceGetByNameRequest rq = serviceGetByNameRequest();
    rq.set_name(volume_name);

    response.reset(ExecuteSyncRequest(
        boost::bind(
            &xtreemfs::pbrpc::DIRServiceClient::
                xtreemfs_service_get_by_name_sync,
            dir_service_client_.get(),
            _1,
            boost::cref(dir_service_auth_),
            boost::cref(dir_service_user_credentials_),
            &rq),
        &dir_uuid_iterator_,
        NULL,
        RPCOptionsFromOptions(options_),
        true));

  } catch (const XtreemFSException& e) {
    if (response.get()) {
      response->DeleteBuffers();
    }
    throw;
  }

  // Delete everything except the response.
  delete[] response->data();
  delete response->error();

  return static_cast<ServiceSet*>(response->response());
}



ClientImplementation::ClientImplementation(
    const ServiceAddresses& dir_service_addresses,
    const pbrpc::UserCredentials& user_credentials,
    const rpc::SSLOptions* ssl_options,
    const Options& options)
    : was_shutdown_(false),
      dir_service_user_credentials_(user_credentials),
      options_(options),
      dir_service_ssl_options_(ssl_options),
      dir_uuid_iterator_(dir_service_addresses),
      uuid_resolver_(dir_uuid_iterator_,
                     user_credentials,
                     options) {

  // Set bogus auth object.
  auth_bogus_.set_auth_type(AUTH_NONE);

  // Currently no AUTH is needed to access the DIR.
  dir_service_auth_.set_auth_type(AUTH_NONE);

  initialize_logger(options.log_level_string,
                    options.log_file_path,
                    LEVEL_WARN);
  initialize_error_log(20);

  if (options_.vivaldi_enable) {
    vivaldi_.reset(new Vivaldi(dir_uuid_iterator_,
                               GetUUIDResolver(),
                               options_));
  }

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

  GenerateVersion4UUID(&client_uuid_);
  assert(!client_uuid_.empty());

  dir_service_client_.reset(new DIRServiceClient(network_client_.get()));
  uuid_resolver_.Initialize(network_client_.get());

  // Start vivaldi thread if configured
  if (options_.vivaldi_enable) {
    if (Logging::log->loggingActive(LEVEL_INFO)) {
      Logging::log->getLog(LEVEL_INFO)
          << "Starting vivaldi..." << endl;
    }
    vivaldi_->Initialize(network_client_.get());
    vivaldi_thread_.reset(new boost::thread(boost::bind(&xtreemfs::Vivaldi::Run,
                                                        vivaldi_.get())));
  }

  async_write_callback_thread_.reset(
      new boost::thread(&xtreemfs::AsyncWriteHandler::ProcessCallbacks, 
                        boost::ref(async_write_callback_queue_)));
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
  uuid_resolver_.VolumeNameToMRCUUID(volume_name, mrc_uuid_iterator);

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

pbrpc::ServiceSet* ClientImplementation::GetServicesByType(const xtreemfs::pbrpc::ServiceType service_type) {
  boost::scoped_ptr<rpc::SyncCallbackBase> response;
  try {
    serviceGetByTypeRequest request;
    request.set_type(service_type);

    response.reset(ExecuteSyncRequest(
        boost::bind(
            &xtreemfs::pbrpc::DIRServiceClient
                ::xtreemfs_service_get_by_type_sync,
            dir_service_client_.get(),
            _1,
            boost::cref(dir_service_auth_),
            boost::cref(dir_service_user_credentials_),
            &request),
        &dir_uuid_iterator_,
        NULL,
        RPCOptionsFromOptions(options_),
        true));
  } catch (const XtreemFSException& e) {
    if (response.get()) {
      response->DeleteBuffers();
    }
    throw;
  }

  // Delete everything except the response.
  delete[] response->data();
  delete response->error();

  return static_cast<ServiceSet*>(response->response());
}

pbrpc::ServiceSet* ClientImplementation::GetServicesByName(const std::string service_name) {
  boost::scoped_ptr<rpc::SyncCallbackBase> response;
  try {
    serviceGetByNameRequest request;
    request.set_name(service_name);

    response.reset(ExecuteSyncRequest(
        boost::bind(
            &xtreemfs::pbrpc::DIRServiceClient
                ::xtreemfs_service_get_by_name_sync,
            dir_service_client_.get(),
            _1,
            boost::cref(dir_service_auth_),
            boost::cref(dir_service_user_credentials_),
            &request),
        &dir_uuid_iterator_,
        NULL,
        RPCOptionsFromOptions(options_),
        true));
  } catch (const XtreemFSException& e) {
    if (response.get()) {
      response->DeleteBuffers();
    }
    throw;
  }

  // Delete everything except the response.
  delete[] response->data();
  delete response->error();

  return static_cast<ServiceSet*>(response->response());
}

void ClientImplementation::CreateVolume(
    const ServiceAddresses& mrc_address,
    const xtreemfs::pbrpc::Auth& auth,
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& volume_name,
    int mode,
    const std::string& owner_username,
    const std::string& owner_groupname,
    const xtreemfs::pbrpc::AccessControlPolicyType& access_policy_type,
    long volume_quota,
    const xtreemfs::pbrpc::StripingPolicyType& default_striping_policy_type,
    int default_stripe_size,
    int default_stripe_width,
    const std::list<xtreemfs::pbrpc::KeyValuePair*>& volume_attributes) {

  std::map<std::string, std::string> volume_attributes_map;
  for (list<KeyValuePair*>::const_iterator it = volume_attributes.begin();
       it != volume_attributes.end();
       ++it) {
    volume_attributes_map[(*it)->key()] = (*it)->value();
  }

  return CreateVolume(mrc_address, auth, user_credentials, volume_name, mode,
                      owner_username, owner_groupname, access_policy_type,
                      volume_quota, default_striping_policy_type,
                      default_stripe_size, default_stripe_width,
                      volume_attributes_map);
}

void ClientImplementation::CreateVolume(
    const ServiceAddresses& mrc_address,
    const xtreemfs::pbrpc::Auth& auth,
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& volume_name,
    int mode,
    const std::string& owner_username,
    const std::string& owner_groupname,
    const xtreemfs::pbrpc::AccessControlPolicyType& access_policy_type,
    long volume_quota,
    const xtreemfs::pbrpc::StripingPolicyType& default_striping_policy_type,
    int default_stripe_size,
    int default_stripe_width,
    const std::map<std::string, std::string>& volume_attributes) {
  MRCServiceClient mrc_service_client(network_client_.get());

  xtreemfs::pbrpc::Volume new_volume;
  new_volume.set_id("");
  new_volume.set_mode(mode);
  new_volume.set_name(volume_name);
  new_volume.set_owner_user_id(owner_username);
  new_volume.set_owner_group_id(owner_groupname);
  new_volume.set_access_control_policy(access_policy_type);
  new_volume.set_quota(volume_quota);
  new_volume.mutable_default_striping_policy()
      ->set_type(default_striping_policy_type);
  new_volume.mutable_default_striping_policy()
      ->set_stripe_size(default_stripe_size);
  new_volume.mutable_default_striping_policy()->set_width(default_stripe_width);

  for (std::map<std::string, std::string>::const_iterator it = volume_attributes.begin();
      it != volume_attributes.end();
      ++it) {
    KeyValuePair* attr = new_volume.add_attrs();
    attr->set_key(it->first);
    attr->set_value(it->second);
  }

  if (options_.encryption) {
    KeyValuePair* attribute = new_volume.add_attrs();
    attribute->set_key("encryption");
    attribute->set_value("true");

    attribute = new_volume.add_attrs();
    attribute->set_key("encryption_block_size");
    attribute->set_value(
        boost::lexical_cast<std::string>(options_.encryption_block_size));

    attribute = new_volume.add_attrs();
    attribute->set_key("encryption_cipher");
    attribute->set_value(options_.encryption_cipher);

    attribute = new_volume.add_attrs();
    attribute->set_key("encryption_hash");
    attribute->set_value(options_.encryption_hash);

    attribute = new_volume.add_attrs();
    attribute->set_key("encryption_cw");
    attribute->set_value(options_.encryption_cw);
  }

  SimpleUUIDIterator temp_uuid_iterator_with_addresses(mrc_address);

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
          RPCOptionsFromOptions(options_),
          true));
  response->DeleteBuffers();
}

void ClientImplementation::CreateVolume(
    const xtreemfs::pbrpc::Auth& auth,
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& volume_name,
    int mode,
    const std::string& owner_username,
    const std::string& owner_groupname,
    const xtreemfs::pbrpc::AccessControlPolicyType& access_policy_type,
    long volume_quota,
    const xtreemfs::pbrpc::StripingPolicyType& default_striping_policy_type,
    int default_stripe_size,
    int default_stripe_width,
    const std::map<std::string, std::string>& volume_attributes) {

  boost::scoped_ptr<ServiceSet> service_set(GetServicesByType(SERVICE_TYPE_MRC));
  if (service_set->services_size() == 0) {
    throw IOException("no MRC available for volume creation");
  }

  ServiceAddresses mrc_address;
  for (int i = 0; i < service_set->services_size(); i++) {
    const Service& service = service_set->services(i);
    std::string address = UUIDToAddress(service.uuid());
    mrc_address.Add(address);
  }

  CreateVolume(mrc_address, auth, user_credentials, volume_name, mode,
               owner_username, owner_groupname, access_policy_type, volume_quota,
               default_striping_policy_type, default_stripe_size,
               default_stripe_width, volume_attributes);
}

void ClientImplementation::DeleteVolume(
    const ServiceAddresses& mrc_address,
    const xtreemfs::pbrpc::Auth& auth,
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& volume_name) {
  MRCServiceClient mrc_service_client(network_client_.get());

  xtreemfs_rmvolRequest rmvol_request;
  rmvol_request.set_volume_name(volume_name);

  SimpleUUIDIterator temp_uuid_iterator_with_addresses(mrc_address);

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
          RPCOptionsFromOptions(options_),
          true));
  response->DeleteBuffers();
}

void ClientImplementation::DeleteVolume(
    const xtreemfs::pbrpc::Auth& auth,
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& volume_name) {

  boost::scoped_ptr<ServiceSet> service_set(GetServicesByName(volume_name));
  if (service_set->services_size() == 0) {
    throw IOException("volume '" + volume_name + "' does not exist");
  }

  const Service& service = service_set->services(0);
  if (service.type() != SERVICE_TYPE_VOLUME) {
    throw IOException("service '" + volume_name + "' is not a volume");
  }

  const ServiceDataMap& data_map = service.data();

  std::string mrc_uuid;
  for (int i = 0, l = data_map.data_size(); i < l; ++i) {
    const KeyValuePair& pair = data_map.data(i);
    if (pair.key() == "mrc") {
      mrc_uuid = pair.value();
      break;
    }
  }

  ServiceAddresses mrc_address(UUIDToAddress(mrc_uuid));
  DeleteVolume(mrc_address, auth, user_credentials, volume_name);
}

xtreemfs::pbrpc::Volumes* ClientImplementation::ListVolumes(
    const ServiceAddresses& mrc_addresses,
    const xtreemfs::pbrpc::Auth& auth) {
  // Create a MRCServiceClient
  MRCServiceClient mrc_service_client(network_client_.get());
  // Use bogus user_credentials;
  UserCredentials user_credentials;
  user_credentials.set_username("xtreemfs");

  SimpleUUIDIterator mrc_service_addresses_(mrc_addresses);

  // Retrieve the list of volumes from the MRC.
  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::xtreemfs_lsvol_sync,
              &mrc_service_client,
              _1,
              boost::cref(auth),
              boost::cref(user_credentials)),
          &mrc_service_addresses_,
          NULL,
          RPCOptionsFromOptions(options_),
          true));

  // Delete everything except the response.
  delete[] response->data();
  delete response->error();

  // Return the list of volumes.
  return static_cast<xtreemfs::pbrpc::Volumes*>(response->response());
}

std::vector<std::string> ClientImplementation::ListVolumeNames() {
  boost::scoped_ptr<ServiceSet> volumes(GetServicesByType(SERVICE_TYPE_VOLUME));
  const int size = volumes->services_size();
  std::vector<std::string> names(size);

  for (int i = 0; i < size; ++i) {
    const Service& volume = volumes->services(i);
    names[i] = volume.name();
  }

  return names;
}

/** ClientImplementation already implements UUIDResolver and therefore this
 *  returns just a cast to this. */
UUIDResolver* ClientImplementation::GetUUIDResolver() {
  return &uuid_resolver_;
}

std::string ClientImplementation::UUIDToAddress(const std::string& uuid) {
  std::string result;
  uuid_resolver_.UUIDToAddress(uuid, &result);
  return result;
}

const VivaldiCoordinates& ClientImplementation::GetVivaldiCoordinates() const {
  return vivaldi_->GetVivaldiCoordinates();
}

util::SynchronizedQueue<AsyncWriteHandler::CallbackEntry>& ClientImplementation::GetAsyncWriteCallbackQueue() {
  return async_write_callback_queue_;
}

}  // namespace xtreemfs
