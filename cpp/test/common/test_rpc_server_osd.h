/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_TEST_RPC_SERVER_OSD_CPP_
#define CPP_TEST_COMMON_TEST_RPC_SERVER_OSD_CPP_

#include "common/test_rpc_server.h"

namespace google {
namespace protobuf {
class Message;
}  // namespace protobuf
}  // namespace google

namespace xtreemfs {
namespace rpc {

class TestRPCServerOSD : public TestRPCServer<TestRPCServerOSD> {
 public:
  TestRPCServerOSD();

 private:

};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_TEST_COMMON_TEST_RPC_SERVER_OSD_CPP_
