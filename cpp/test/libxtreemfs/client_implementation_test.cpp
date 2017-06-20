/*
 * Copyright (c) 2014 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <boost/scoped_ptr.hpp>
#include <map>

#include "common/test_rpc_server_dir.h"
#include "libxtreemfs/client.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/pbrpc_url.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "xtreemfs/DIR.pb.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {

class ClientImplementationTest : public ::testing::Test {
 protected:
  /** Mock-up DIR server where custom address mappings can be registered. */
  class TestRPCServerDIRCustomMapping : public xtreemfs::rpc::TestRPCServerDIR {
   public:
    void AddMapping(const string& uuid, const AddressMapping& mapping) {
      mappings_.insert(pair<string, AddressMapping>(uuid, mapping));
    }

   private:
    /** Map from UUID to AddressMapping(s). */
    std::multimap<string, AddressMapping> mappings_;

    virtual google::protobuf::Message* GetAddressMappingOperation(
        const pbrpc::Auth& auth,
        const pbrpc::UserCredentials& user_credentials,
        const google::protobuf::Message& request,
        const char* data,
        uint32_t data_len,
        boost::scoped_array<char>* response_data,
        uint32_t* response_data_len) {
      const addressMappingGetRequest* rq
          = reinterpret_cast<const addressMappingGetRequest*>(&request);

      AddressMappingSet* response = new AddressMappingSet();

      pair<multimap<string,AddressMapping>::iterator,
           multimap<string,AddressMapping>::iterator> ret;
      ret = mappings_.equal_range(rq->uuid());
      for (multimap<string,AddressMapping>::iterator it = ret.first;
           it != ret.second;
           ++it) {
        AddressMapping* mapping = response->add_mappings();
        mapping->CopyFrom(it->second);
      }

      return response;
    }
  };

  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);

    kTestUUID_ = "uuid";
    kTestPort_ = 12345;

    user_credentials_.set_username("ClientImplementationTest");
    user_credentials_.add_groups("ClientImplementationTest");


    dir_.reset(new TestRPCServerDIRCustomMapping());
    ASSERT_TRUE(dir_->Start());

    client_.reset(Client::CreateClient(
        dir_->GetAddress(),
        user_credentials_,
        NULL,  // No SSL options.
        options_));

    // Start the client (a connection to the DIR service will be setup).
    client_->Start();
  }

  virtual void TearDown() {
    if (dir_.get()) {
      client_->Shutdown();
    }
    if (dir_.get()) {
      dir_->Stop();
    }

    shutdown_logger();
  }

  void AddAddressMapping(const std::string& match_network,
                         const std::string& hostname) {
    AddressMapping mapping;
    mapping.set_uuid(kTestUUID_);
    mapping.set_version(0);
    mapping.set_protocol(PBRPCURL::GetSchemePBRPC());
    mapping.set_address(hostname);
    mapping.set_port(kTestPort_);
    mapping.set_match_network(match_network);
    mapping.set_ttl_s(3600);
    mapping.set_uri("");

    dir_->AddMapping(kTestUUID_, mapping);
  }

  std::string ExpectedAddress(const std::string& hostname) {
    return hostname + ":" + boost::lexical_cast<string>(kTestPort_);
  }

  boost::scoped_ptr<TestRPCServerDIRCustomMapping> dir_;

  boost::scoped_ptr<Client> client_;

  Options options_;
  UserCredentials user_credentials_;

  /** Test constant used for all operations. */
  std::string kTestUUID_;
  int kTestPort_;
};


/** For the requested UUID is exactly one default entry available. */
TEST_F(ClientImplementationTest, UUIDToAddressDefaultOnly) {
  AddAddressMapping("*", "default");

  EXPECT_EQ(ExpectedAddress("default"), client_->UUIDToAddress(kTestUUID_));
}

/** For the requested UUID is no address known. */
TEST_F(ClientImplementationTest, UUIDToAddressNoService) {
  AddAddressMapping("*", "default");

  ASSERT_THROW(client_->UUIDToAddress("unknown-UUID"),
               AddressToUUIDNotFoundException);
}

/** For the requested UUID is a local network and a default one available. */
#ifdef __linux__
TEST_F(ClientImplementationTest, UUIDToAddressLocalNetworkAndDefault) {
  boost::unordered_set<string> local_networks = GetNetworks();
  // TODO(mberlin): This effectively requires to run the unit test with at
  //                least one network interface. Put an if around this if this
  //                results into flaky tests.
  ASSERT_GE(local_networks.size(), 1);

  AddAddressMapping(*local_networks.begin(), "local-network");
  AddAddressMapping("*", "default");

  EXPECT_EQ(ExpectedAddress("local-network"),
            client_->UUIDToAddress(kTestUUID_));
}
#endif  // __linux__

/** Same as "UUIDToAddressLocalNetworkAndDefault", but reverse list order. */
#ifdef __linux__
TEST_F(ClientImplementationTest, UUIDToAddressDefaultAndLocalNetwork) {
  boost::unordered_set<string> local_networks = GetNetworks();
  // TODO(mberlin): This effectively requires to run the unit test with at
  //                least one network interface. Put an if around this if this
  //                results into flaky tests.
  ASSERT_GE(local_networks.size(), 1);

  AddAddressMapping("*", "default");
  AddAddressMapping(*local_networks.begin(), "local-network");

  EXPECT_EQ(ExpectedAddress("local-network"),
            client_->UUIDToAddress(kTestUUID_));
}
#endif  // __linux__

}  // namespace xtreemfs
