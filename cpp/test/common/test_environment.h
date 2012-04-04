/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_TEST_ENVIRONMENT_H_
#define CPP_TEST_COMMON_TEST_ENVIRONMENT_H_

#include <boost/scoped_ptr.hpp>

#include "libxtreemfs/options.h"
#include "pbrpc/RPC.pb.h"  // xtreemfs::pbrpc::UserCredentials

namespace xtreemfs {

namespace rpc {
class TestRPCServer;
}  // namespace rpc

class Client;

/** Aggregator class which bundles Client and TestRPCServer objects to
 *  run unit tests.
 *  
 *  Modify options accordingly before executing Start() to influence
 *  the client.
 */
class TestEnvironment {
 public:
  TestEnvironment();

  void Start();
  void Stop();

  boost::scoped_ptr<Client> client;
  Options options;
  xtreemfs::pbrpc::UserCredentials user_credentials;

  boost::scoped_ptr<xtreemfs::rpc::TestRPCServer> dir;
};

}  // namespace xtreemfs

#endif  // CPP_TEST_COMMON_TEST_ENVIRONMENT_H_
