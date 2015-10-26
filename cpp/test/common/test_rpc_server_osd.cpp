/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "common/test_rpc_server_osd.h"

#include "util/logging.h"
#include "xtreemfs/OSD.pb.h"
#include "xtreemfs/OSDServiceConstants.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using xtreemfs::util::Logging;

namespace xtreemfs {
namespace rpc {

const int kMaxFileSize = 10 * 1024 * 1024;

TestRPCServerOSD::TestRPCServerOSD() : file_size_(0) {
  interface_id_ = INTERFACE_ID_OSD;
  // Register available operations.
  operations_[PROC_ID_TRUNCATE]
      = Op(this, &TestRPCServerOSD::TruncateOperation);
  operations_[PROC_ID_WRITE]
      = Op(this, &TestRPCServerOSD::WriteOperation);
  operations_[PROC_ID_READ]
      = Op(this, &TestRPCServerOSD::ReadOperation);
  operations_[PROC_ID_XTREEMFS_FINALIZE_VOUCHERS]
      = Op(this, &TestRPCServerOSD::FinalizeVoucherOperation);
  data_.reset(new char[kMaxFileSize]);
}

const std::vector<WriteEntry> TestRPCServerOSD::GetReceivedWrites() const {
  boost::mutex::scoped_lock lock(mutex_);
  return received_writes_;
}

google::protobuf::Message* TestRPCServerOSD::TruncateOperation(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len,
    boost::scoped_array<char>* response_data,
    uint32_t* response_data_len) {
  boost::mutex::scoped_lock lock(mutex_);
  const truncateRequest* rq
      = static_cast<const truncateRequest*>(&request);

  file_size_ = rq->new_file_size();
  assert(file_size_ <= kMaxFileSize);

  OSDWriteResponse* response = new OSDWriteResponse();
  response->set_size_in_bytes(rq->new_file_size());
  response->set_truncate_epoch(0);

  return response;
}

google::protobuf::Message* TestRPCServerOSD::ReadOperation(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len,
    boost::scoped_array<char>* response_data,
    uint32_t* response_data_len) {
  boost::mutex::scoped_lock lock(mutex_);
  const readRequest* rq
      = static_cast<const readRequest*>(&request);

  const int64_t object_size =
      rq->file_credentials().xlocs().replicas(0).
          striping_policy().stripe_size() * 1024;
  const int64_t offset = rq->object_number() * object_size +
                          rq->offset();
  const int64_t bytes_to_read =
      std::min(static_cast<int64_t>(rq->length()), file_size_ - offset);

  if (Logging::log->loggingActive(xtreemfs::util::LEVEL_DEBUG)) {
    Logging::log->getLog(xtreemfs::util::LEVEL_DEBUG)
      << "Received read: object_number: " << rq->object_number()
      << ", offset: " << offset
      << ", read length: " << rq->length()
      << ", object_size: " << object_size
      << ", file_size: " << file_size_
      << ", sending: " << bytes_to_read << " bytes"
      << std::endl;
  }

  if (bytes_to_read > 0) {
    response_data->reset(new char[bytes_to_read]);
    *response_data_len = bytes_to_read;
    memcpy(response_data->get(), &data_[offset], bytes_to_read);
  }

  ObjectData* response = new ObjectData();
  response->set_zero_padding(0);
  response->set_invalid_checksum_on_osd(false);
  response->set_checksum(0);
  return response;
}

google::protobuf::Message* TestRPCServerOSD::WriteOperation(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len,
    boost::scoped_array<char>* response_data,
    uint32_t* response_data_len) {
  boost::mutex::scoped_lock lock(mutex_);
  const writeRequest* rq
      = static_cast<const writeRequest*>(&request);

  received_writes_.push_back(
      WriteEntry(rq->object_number(), rq->offset(), data_len));

  if (Logging::log->loggingActive(xtreemfs::util::LEVEL_DEBUG)) {
    Logging::log->getLog(xtreemfs::util::LEVEL_DEBUG)
      << "Received write: object_number: " << rq->object_number()
      << ", offset: " << rq->offset()
      << ", data_len: " << data_len << std::endl;
  }

  const uint64_t object_size =
      rq->file_credentials().xlocs().replicas(0).
          striping_policy().stripe_size() * 1024;
  const uint64_t offset = rq->object_number() * object_size + rq->offset();

  file_size_ = offset + data_len;
  assert(file_size_ <= kMaxFileSize);

  memcpy(&data_[offset], data, data_len);

  OSDWriteResponse* response = new OSDWriteResponse();
  response->set_size_in_bytes(file_size_);
  response->set_truncate_epoch(0);

  return response;
}

google::protobuf::Message* TestRPCServerOSD::FinalizeVoucherOperation(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len,
    boost::scoped_array<char>* response_data,
    uint32_t* response_data_len) {
  boost::mutex::scoped_lock lock(mutex_);

  OSDFinalizeVouchersResponse* response = new OSDFinalizeVouchersResponse();
  response->set_osd_uuid("osd_uuid");
  response->set_server_signature("signature");
  response->set_size_in_bytes(file_size_);
  response->set_truncate_epoch(0);

  return response;
}

}  // namespace rpc
}  // namespace xtreemfs
