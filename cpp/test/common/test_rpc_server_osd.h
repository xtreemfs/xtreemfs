/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_TEST_RPC_SERVER_OSD_CPP_
#define CPP_TEST_COMMON_TEST_RPC_SERVER_OSD_CPP_

#include "common/test_rpc_server.h"

#include <boost/thread/mutex.hpp>

namespace google {
namespace protobuf {
class Message;
}  // namespace protobuf
}  // namespace google

namespace xtreemfs {
namespace rpc {

class WriteEntry {
 public:
  WriteEntry()
      : objectNumber_(0), offset_(0), data_len_(0) { }

  WriteEntry(uint64_t objectNumber, boost::uint32_t offset, boost::uint32_t data_len)
    : objectNumber_(objectNumber), offset_(offset), data_len_(data_len) { }

  bool operator==(const WriteEntry& other) const {
    return (other.objectNumber_ == this->objectNumber_)
        && (other.offset_ == this->offset_)
        && (other.data_len_ == this->data_len_);
  }

  boost::uint64_t objectNumber_;
  boost::uint32_t offset_;
  boost::uint32_t data_len_;
};

class TestRPCServerOSD : public TestRPCServer<TestRPCServerOSD> {
 public:
  TestRPCServerOSD();
  const std::vector<WriteEntry>& getReceivedWrites() const;

 private:
  google::protobuf::Message* TruncateOperation(
      const pbrpc::Auth& auth,
      const pbrpc::UserCredentials& user_credentials,
      const google::protobuf::Message& request,
      const char* data,
      boost::uint32_t data_len);

  google::protobuf::Message* WriteOperation(
      const pbrpc::Auth& auth,
      const pbrpc::UserCredentials& user_credentials,
      const google::protobuf::Message& request,
      const char* data,
      boost::uint32_t data_len);

  boost::mutex mutex_;

  size_t file_size_;
  std::vector<WriteEntry> received_writes_;
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_TEST_COMMON_TEST_RPC_SERVER_OSD_CPP_
