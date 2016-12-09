/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "common/test_rpc_server_mrc.h"

#include "xtreemfs/MRC.pb.h"
#include "xtreemfs/MRCServiceConstants.h"

#include <ctime>

using namespace std;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {
namespace rpc {

TestRPCServerMRC::TestRPCServerMRC() : file_size_(1024 * 1024) {
  interface_id_ = INTERFACE_ID_MRC;
  // Register available operations.
  operations_[PROC_ID_OPEN] = Op(this, &TestRPCServerMRC::OpenOperation);
  operations_[PROC_ID_XTREEMFS_RENEW_CAPABILITY_AND_VOUCHER] =
      Op(this, &TestRPCServerMRC::RenewCapabilityOperation);
  operations_[PROC_ID_XTREEMFS_UPDATE_FILE_SIZE] =
      Op(this, &TestRPCServerMRC::UpdateFileSizeOperation);
  operations_[PROC_ID_FTRUNCATE] =
      Op(this, &TestRPCServerMRC::FTruncate);
  operations_[PROC_ID_XTREEMFS_CLEAR_VOUCHERS] =
      Op(this, &TestRPCServerMRC::ClearVoucherOperation);
}

google::protobuf::Message* TestRPCServerMRC::OpenOperation(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len,
    boost::scoped_array<char>* response_data,
    uint32_t* response_data_len) {
  const openRequest* rq = reinterpret_cast<const openRequest*>(&request);

  openResponse* response = new openResponse();

  XCap* xcap = response->mutable_creds()->mutable_xcap();
  xcap->set_access_mode(rq->flags());
  xcap->set_client_identity("client_identity");
  xcap->set_expire_time_s(3600);
  xcap->set_expire_timeout_s(static_cast<uint32_t>(time(0)) + 3600);
  xcap->set_file_id(rq->volume_name() + ":0");
  xcap->set_replicate_on_close(false);
  xcap->set_server_signature("signature");
  xcap->set_snap_config(SNAP_CONFIG_SNAPS_DISABLED);
  xcap->set_snap_timestamp(0);
  xcap->set_truncate_epoch(0);
  xcap->set_voucher_size(0);

  struct timeval tp;
  gettimeofday(&tp, NULL);
  xcap->set_expire_time_ms(tp.tv_sec * 1000 + tp.tv_usec / 1000);

  XLocSet* xlocset = response->mutable_creds()->mutable_xlocs();
  xlocset->set_read_only_file_size(file_size_);
  xlocset->set_replica_update_policy("");  // "" = REPL_UPDATE_PC_NONE;
  xlocset->set_version(0);
  xlocset->add_replicas();

  Replica* replica = xlocset->mutable_replicas(0);
  replica->set_replication_flags(0);

  for (std::vector<std::string>::iterator it = osd_uuids_.begin();
       it != osd_uuids_.end();
       ++it) {
    replica->add_osd_uuids(*it);
  }

  replica->mutable_striping_policy()->set_type(STRIPING_POLICY_RAID0);
  replica->mutable_striping_policy()->set_stripe_size(128);
  replica->mutable_striping_policy()->set_width(1);

  response->set_timestamp_s(static_cast<uint32_t>(time(0)));

  return response;
}

google::protobuf::Message* TestRPCServerMRC::RenewCapabilityOperation(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len,
    boost::scoped_array<char>* response_data,
    uint32_t* response_data_len) {
  const xtreemfs_renew_capabilityRequest* rq = reinterpret_cast<const xtreemfs_renew_capabilityRequest*>(&request);

  XCap* response = new XCap(rq->xcap());

  response->set_expire_time_s(time(0) + 3600);
  response->set_expire_timeout_s(3600);

  struct timeval tp;
  gettimeofday(&tp, NULL);
  response->set_expire_time_ms(tp.tv_sec * 1000 + tp.tv_usec / 1000);

  return response;
}

google::protobuf::Message* TestRPCServerMRC::UpdateFileSizeOperation(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len,
    boost::scoped_array<char>* response_data,
    uint32_t* response_data_len) {
  //const xtreemfs_update_file_sizeRequest* rq =
  //    reinterpret_cast<const xtreemfs_update_file_sizeRequest*>(&request);

  timestampResponse* response = new timestampResponse();

  response->set_timestamp_s(static_cast<uint32_t>(time(0)));

  return response;
}

google::protobuf::Message* TestRPCServerMRC::FTruncate(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len,
    boost::scoped_array<char>* response_data,
    uint32_t* response_data_len) {
  const XCap* rq = reinterpret_cast<const XCap*>(&request);

  XCap* response = new XCap(*rq);
  response->set_expire_time_s(time(0) + 3600);
  response->set_expire_timeout_s(3600);

  return response;
}

google::protobuf::Message* TestRPCServerMRC::ClearVoucherOperation(
    const pbrpc::Auth& auth,
    const pbrpc::UserCredentials& user_credentials,
    const google::protobuf::Message& request,
    const char* data,
    uint32_t data_len,
    boost::scoped_array<char>* response_data,
    uint32_t* response_data_len) {

  return new emptyResponse();
}


void TestRPCServerMRC::SetFileSize(uint64_t size) {
  boost::mutex::scoped_lock lock(mutex_);
  file_size_ = size;
}

void TestRPCServerMRC::RegisterOSD(std::string uuid) {
  boost::mutex::scoped_lock lock(mutex_);
  osd_uuids_.push_back(uuid);
}

} // namespace rpc
} // namespace xtreemfs
