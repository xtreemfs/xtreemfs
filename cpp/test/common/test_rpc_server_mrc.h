/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_TEST_RPC_SERVER_MRC_CPP_
#define CPP_TEST_COMMON_TEST_RPC_SERVER_MRC_CPP_

#include "common/test_rpc_server.h"

#include <boost/thread/mutex.hpp>

namespace google {
namespace protobuf {
class Message;
}  // namespace protobuf
}  // namespace google

namespace xtreemfs {
namespace rpc {

class TestRPCServerMRC : public TestRPCServer<TestRPCServerMRC> {
 public:
  TestRPCServerMRC();

  void setFileSize(uint64_t size);
  void RegisterOSD(std::string uuid);
  std::list<std::string>* getOSDUUIDs();


 private:
  google::protobuf::Message* OpenOperation(
      const pbrpc::Auth& auth,
      const pbrpc::UserCredentials& user_credentials,
      const google::protobuf::Message& request,
      const char* data,
      boost::uint32_t data_len);

  google::protobuf::Message* UpdateFileSizeOperation(
      const pbrpc::Auth& auth,
      const pbrpc::UserCredentials& user_credentials,
      const google::protobuf::Message& request,
      const char* data,
      boost::uint32_t data_len);

  google::protobuf::Message* RenewCapabilityOperation(
      const pbrpc::Auth& auth,
      const pbrpc::UserCredentials& user_credentials,
      const google::protobuf::Message& request,
      const char* data,
      boost::uint32_t data_len);

  boost::mutex mutex_;

  size_t file_size_;
  std::vector<std::string> osd_uuids_;
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_TEST_COMMON_TEST_RPC_SERVER_MRC_CPP_
