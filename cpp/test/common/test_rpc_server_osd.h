/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_TEST_RPC_SERVER_OSD_CPP_
#define CPP_TEST_COMMON_TEST_RPC_SERVER_OSD_CPP_

#include "common/test_rpc_server.h"

#include <stdint.h>

#include <boost/thread/mutex.hpp>
#include <vector>

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
      : object_number_(0), offset_(0), data_len_(0) { }

  WriteEntry(uint64_t objectNumber, uint32_t offset, uint32_t data_len)
    : object_number_(objectNumber), offset_(offset), data_len_(data_len) { }

  bool operator==(const WriteEntry& other) const {
    return (other.object_number_ == this->object_number_)
        && (other.offset_ == this->offset_)
        && (other.data_len_ == this->data_len_);
  }

  uint64_t object_number_;
  uint32_t offset_;
  uint32_t data_len_;
};

class TestRPCServerOSD : public TestRPCServer<TestRPCServerOSD> {
 public:
  TestRPCServerOSD();
  const std::vector<WriteEntry> GetReceivedWrites() const;

 private:
  google::protobuf::Message* TruncateOperation(
      const pbrpc::Auth& auth,
      const pbrpc::UserCredentials& user_credentials,
      const google::protobuf::Message& request,
      const char* data,
      uint32_t data_len,
      boost::scoped_array<char>* response_data,
      uint32_t* response_data_len);

  google::protobuf::Message* ReadOperation(
      const pbrpc::Auth& auth,
      const pbrpc::UserCredentials& user_credentials,
      const google::protobuf::Message& request,
      const char* data,
      uint32_t data_len,
      boost::scoped_array<char>* response_data,
      uint32_t* response_data_len);

  google::protobuf::Message* WriteOperation(
      const pbrpc::Auth& auth,
      const pbrpc::UserCredentials& user_credentials,
      const google::protobuf::Message& request,
      const char* data,
      uint32_t data_len,
      boost::scoped_array<char>* response_data,
      uint32_t* response_data_len);


  google::protobuf::Message* FinalizeVoucherOperation(
      const pbrpc::Auth& auth,
      const pbrpc::UserCredentials& user_credentials,
      const google::protobuf::Message& request,
      const char* data,
      uint32_t data_len,
      boost::scoped_array<char>* response_data,
      uint32_t* response_data_len);

  /** Mutex used to protect all member variables from concurrent access. */
  mutable boost::mutex mutex_;

  /** A single file size is remembered between requests. */
  int64_t file_size_;

  /** The file data. */
  boost::scoped_array<char> data_;

  /** A list of received write requests that can be used to check against an
   *  expected result.
   */
  std::vector<WriteEntry> received_writes_;
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_TEST_COMMON_TEST_RPC_SERVER_OSD_CPP_
