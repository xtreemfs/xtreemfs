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
      = &GetServiceByNameOperation;
}

google::protobuf::Message* TestRPCServerDIR::GetServiceByNameOperation(
    const google::protobuf::Message& request) {
  const serviceGetByNameRequest* rq
      = reinterpret_cast<const serviceGetByNameRequest*>(&request);

  ServiceSet* response = new ServiceSet();
  Service* new_entry = response->add_services();
  new_entry->set_type(SERVICE_TYPE_VOLUME);
  new_entry->set_uuid(rq->name());
  new_entry->set_version(0);
  new_entry->set_name(rq->name());
  new_entry->set_last_updated_s(0);
  KeyValuePair* new_data = new_entry->mutable_data()->add_data();
  new_data->set_key("mrc");
  new_data->set_value(rq->name());
  return response;
}

}  // namespace rpc
}  // namespace xtreemfs
