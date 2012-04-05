/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "common/test_rpc_server_dir.h"

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
}

void TestRPCServerDIR::RegisterVolume(const std::string& volume_name,
                                      const std::string& mrc_uuid) {
  boost::mutex::scoped_lock lock(mutex_);
  known_volumes_[volume_name] = mrc_uuid;
}

google::protobuf::Message* TestRPCServerDIR::GetServiceByNameOperation(
    const google::protobuf::Message& request) {
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
    new_entry->set_uuid(rq->name());
    new_entry->set_version(0);
    new_entry->set_name(rq->name());
    new_entry->set_last_updated_s(0);
    KeyValuePair* new_data = new_entry->mutable_data()->add_data();
    new_data->set_key("mrc");
    new_data->set_value(mrc_uuid);
  }
  return response;
}

}  // namespace rpc
}  // namespace xtreemfs
