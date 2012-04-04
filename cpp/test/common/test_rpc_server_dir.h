/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_TEST_RPC_SERVER_DIR_CPP_
#define CPP_TEST_COMMON_TEST_RPC_SERVER_DIR_CPP_

#include "common/test_rpc_server.h"

#include <boost/cstdint.hpp>

#include <map>

namespace google {
namespace protobuf {
class Message;
}  // namespace protobuf
}  // namespace google

namespace xtreemfs {
namespace rpc {

class TestRPCServerDIR : public TestRPCServer {
 public:
  TestRPCServerDIR();

 private:
  static google::protobuf::Message* GetServiceByNameOperation(
      const google::protobuf::Message& request);
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_TEST_COMMON_TEST_RPC_SERVER_DIR_CPP_
