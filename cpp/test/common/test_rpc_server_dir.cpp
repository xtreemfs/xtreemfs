/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "common/test_rpc_server_dir.h"

#include "libxtreemfs/pbrpc_url.h"
#include "xtreemfs/DIR.pb.h"
#include "xtreemfs/DIRServiceConstants.h"

using namespace std;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {
namespace rpc {

TestRPCServerDIR::TestRPCServerDIR() {
  interface_id_ = INTERFACE_ID_DIR;
  // Register available operations.

  operations_[PROC_ID_XTREEMFS_SERVICE_GET_BY_NAME]
      = Op(this, &TestRPCServerDIR::GetServiceByNameOperation);
  operations_[PROC_ID_XTREEMFS_SERVICE_GET_BY_UUID]
      = Op(this, &TestRPCServerDIR::GetServiceByUUIDOperation);
  operations_[PROC_ID_XTREEMFS_ADDRESS_MAPPINGS_GET]
      = Op(this, &TestRPCServerDIR::GetAddressMappingOperation);

}

void TestRPCServerDIR::RegisterVolume(const std::string& volume_name,
                                      const std::string& mrc_uuid) {
  boost::mutex::scoped_lock lock(mutex_);
  known_volumes_[volume_name] = mrc_uuid;
}

google::protobuf::Message* TestRPCServerDIR::GetServiceByNameOperation(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len,
    boost::scoped_array<char>* response_data,
    uint32_t* response_data_len) {
  const serviceGetByNameRequest* rq
      = reinterpret_cast<const serviceGetByNameRequest*>(&request);

  string mrc_uuid;
  {
    boost::mutex::scoped_lock lock(mutex_);
    map<string, string>::iterator iter = known_volumes_.find(rq->name());
    if (iter != known_volumes_.end()) {
      mrc_uuid = iter->second;
    }
  }

  ServiceSet* response = new ServiceSet();
  if (!mrc_uuid.empty()) {
    Service* new_entry = response->add_services();
    new_entry->set_type(SERVICE_TYPE_VOLUME);
    new_entry->set_uuid(mrc_uuid);
    new_entry->set_version(0);
    new_entry->set_name(rq->name());
    new_entry->set_last_updated_s(0);
    KeyValuePair* new_data = new_entry->mutable_data()->add_data();
    new_data->set_key("mrc");
    new_data->set_value(mrc_uuid);
  }
  return response;
}

google::protobuf::Message* TestRPCServerDIR::GetServiceByUUIDOperation(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len,
    boost::scoped_array<char>* response_data,
    uint32_t* response_data_len) {
  const serviceGetByUUIDRequest* rq
      = reinterpret_cast<const serviceGetByUUIDRequest*>(&request);

  string mrc_uuid = rq->name();
  string name;
  {
    boost::mutex::scoped_lock lock(mutex_);
    map<string, string>::iterator iter;

    for (iter = known_volumes_.begin(); iter != known_volumes_.end(); ++iter) {
        if (iter->second == mrc_uuid) {
            name = iter->first;
            mrc_uuid = iter->second;
            break;
        }
    }
  }

  ServiceSet* response = new ServiceSet();
  if (!name.empty()) {
    Service* new_entry = response->add_services();
    new_entry->set_type(SERVICE_TYPE_VOLUME);
    new_entry->set_uuid(mrc_uuid);
    new_entry->set_version(0);
    new_entry->set_name(name);
    new_entry->set_last_updated_s(0);
    KeyValuePair* new_data = new_entry->mutable_data()->add_data();
    new_data->set_key("mrc");
    new_data->set_value(mrc_uuid);
  }
  return response;
}

google::protobuf::Message* TestRPCServerDIR::GetAddressMappingOperation(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len,
    boost::scoped_array<char>* response_data,
    uint32_t* response_data_len) {
  const addressMappingGetRequest* rq
      = reinterpret_cast<const addressMappingGetRequest*>(&request);

  AddressMappingSet* response = new AddressMappingSet();

  AddressMapping* mapping = response->add_mappings();
  size_t uuid_split_pos = rq->uuid().find_last_of(":");
  mapping->set_uuid(rq->uuid());
  mapping->set_version(0);
  mapping->set_protocol(PBRPCURL::GetSchemePBRPC());
  mapping->set_address(rq->uuid().substr(0, uuid_split_pos));
  mapping->set_port(atoi(rq->uuid().substr(uuid_split_pos+1).c_str()));
  mapping->set_match_network("*");
  mapping->set_ttl_s(3600);
  mapping->set_uri("");

  return response;
}


}  // namespace rpc
}  // namespace xtreemfs
