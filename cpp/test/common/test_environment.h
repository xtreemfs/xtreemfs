/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_TEST_ENVIRONMENT_H_
#define CPP_TEST_COMMON_TEST_ENVIRONMENT_H_

#include <boost/scoped_ptr.hpp>
#include <string>
#include <vector>

#include "libxtreemfs/options.h"
#include "pbrpc/RPC.pb.h"  // xtreemfs::pbrpc::UserCredentials

namespace xtreemfs {

namespace rpc {
class TestRPCServerMRC;
class TestRPCServerDIR;
class TestRPCServerOSD;
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
  explicit TestEnvironment();
  ~TestEnvironment();

  bool Start();
  void Stop();

  /** Add num_of_osds additional OSDs before executing Start(). */
  void AddOSDs(int num_of_osds);

  boost::scoped_ptr<Client> client;
  Options options;
  xtreemfs::pbrpc::UserCredentials user_credentials;

  /** Volume name under which the MRC will be registered. */
  std::string volume_name_;

  boost::scoped_ptr<xtreemfs::rpc::TestRPCServerDIR> dir;
  boost::scoped_ptr<xtreemfs::rpc::TestRPCServerMRC> mrc;
  std::vector<xtreemfs::rpc::TestRPCServerOSD*> osds;
};

}  // namespace xtreemfs

#endif  // CPP_TEST_COMMON_TEST_ENVIRONMENT_H_
