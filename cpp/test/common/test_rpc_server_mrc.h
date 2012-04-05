/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_TEST_RPC_SERVER_MRC_CPP_
#define CPP_TEST_COMMON_TEST_RPC_SERVER_MRC_CPP_

#include "common/test_rpc_server.h"

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

 private:

};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_TEST_COMMON_TEST_RPC_SERVER_MRC_CPP_
