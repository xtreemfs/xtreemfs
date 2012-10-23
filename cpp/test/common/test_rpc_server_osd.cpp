/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "common/test_rpc_server_osd.h"

#include "xtreemfs/OSD.pb.h"
#include "xtreemfs/OSDServiceConstants.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using xtreemfs::util::Logging;

namespace xtreemfs {
namespace rpc {

TestRPCServerOSD::TestRPCServerOSD() : file_size_(1024 * 1024) {
  interface_id_ = INTERFACE_ID_OSD;
  // Register available operations.
  operations_[PROC_ID_TRUNCATE]
      = Op(this, &TestRPCServerOSD::TruncateOperation);
  operations_[PROC_ID_WRITE]
      = Op(this, &TestRPCServerOSD::WriteOperation);
}

const std::vector<WriteEntry>& TestRPCServerOSD::GetReceivedWrites() const {
  boost::mutex::scoped_lock lock(mutex_);
  return received_writes_;
}

google::protobuf::Message* TestRPCServerOSD::TruncateOperation(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len) {
  const truncateRequest* rq
      = reinterpret_cast<const truncateRequest*>(&request);

  OSDWriteResponse* response = new OSDWriteResponse();

  {
     boost::mutex::scoped_lock lock(mutex_);
     file_size_ = rq->new_file_size();
  }

  response->set_size_in_bytes(rq->new_file_size());
  response->set_truncate_epoch(0);

  return response;
}

google::protobuf::Message* TestRPCServerOSD::WriteOperation(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len) {
  const writeRequest* rq
      = reinterpret_cast<const writeRequest*>(&request);

  OSDWriteResponse* response = new OSDWriteResponse();

  {
     boost::mutex::scoped_lock lock(mutex_);
     received_writes_.push_back(WriteEntry(rq->object_number(), rq->offset(), data_len));
  }

  if (Logging::log->loggingActive(xtreemfs::util::LEVEL_DEBUG)) {
    Logging::log->getLog(xtreemfs::util::LEVEL_DEBUG)
      << "Received write: object_number: " << rq->object_number() << ", offset: " << rq->offset() << ", data_len: " << data_len << std::endl;
  }

  {
    boost::mutex::scoped_lock lock(mutex_);
    response->set_size_in_bytes(file_size_);
  }
  response->set_truncate_epoch(0);

  return response;
}

}  // namespace rpc
}  // namespace xtreemfs
