/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include <gtest/gtest.h>

#include <sys/time.h>

#include <boost/foreach.hpp>
#include <string>
#include <vector>

#include "common/test_environment.h"
#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/hash_tree_ad.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"

namespace xtreemfs {

std::string RandomVolumeName(const int length) {
#ifdef WIN32
  srand(GetTickCount());
#else
  struct timeval time;
  gettimeofday(&time, NULL);
  srand((time.tv_sec * 1000) + (time.tv_usec / 1000));
#endif  // WIN32

  std::string result = "volume_implementation_test_";
  char c;
  for (int i = 1; i <= length; i++) {
    while (!std::isalnum(c = static_cast<char>(std::rand() % 256))) {
    }  // NOLINT
    result += c;
  }
  return result;
}

class OnlineTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
#ifdef __linux__
    const char* dir_url_env = getenv("XTREEMFS_DIR_URL");
    if (dir_url_env) {
      dir_url_.xtreemfs_url = std::string(dir_url_env);
    }
    const char* mrc_url_env = getenv("XTREEMFS_MRC_URL");
    if (mrc_url_env) {
      mrc_url_.xtreemfs_url = std::string(mrc_url_env);
    }
#endif  // __linux__

    if (dir_url_.xtreemfs_url.empty()) {
      dir_url_.xtreemfs_url = "pbrpc://localhost:32638/";
    }
    if (mrc_url_.xtreemfs_url.empty()) {
      mrc_url_.xtreemfs_url = "pbrpc://localhost:32636/";
    }

    dir_url_.ParseURL(kDIR);
    mrc_url_.ParseURL(kMRC);

    auth_.set_auth_type(xtreemfs::pbrpc::AUTH_NONE);

    // Every operation is executed in the context of a given user and his groups
    // The UserCredentials object does store this information.
    user_credentials_.set_username("volume_implementation_test");
    user_credentials_.add_groups("volume_implementation_test");

    // Create a new instance of a client using the DIR service at 'localhost'
    // at port 32638 using the default implementation.
    options_.log_level_string = "WARN";
    client_ = Client::CreateClient(dir_url_.service_addresses,
                                   user_credentials_,
                                   dir_url_.GenerateSSLOptions(), options_);

    // Start the client (a connection to the DIR service will be setup).
    client_->Start();

    // Create volume with a random name.
    volume_name_ = RandomVolumeName(5);
    client_->CreateVolume(mrc_url_.service_addresses, auth_, user_credentials_,
                          volume_name_);

    // Mount volume.
    volume_ = client_->OpenVolume(volume_name_, NULL, options_);
  }

  virtual void TearDown() {
    volume_->Close();

    client_->DeleteVolume(mrc_url_.service_addresses, auth_, user_credentials_,
                          volume_name_);

    client_->Shutdown();
    delete client_;
  }

  xtreemfs::Client* client_;
  xtreemfs::Options options_;
  /** Only used to store and parse the DIR URL. */
  xtreemfs::Options dir_url_;
  /** Only used to store and parse the MRC URL. */
  xtreemfs::Options mrc_url_;
  xtreemfs::Volume* volume_;
  std::string volume_name_;

  xtreemfs::pbrpc::Auth auth_;
  xtreemfs::pbrpc::UserCredentials user_credentials_;
};

class OfflineTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
//    initialize_logger (LEVEL_WARN);
    test_env.options.connect_timeout_s = 3;
    test_env.options.request_timeout_s = 3;
    test_env.options.retry_delay_s = 3;
    test_env.options.enable_async_writes = true;
    test_env.options.async_writes_max_request_size_kb = 128;
    test_env.options.async_writes_max_requests = 8;

    test_env.options.periodic_xcap_renewal_interval_s = 2;
    ASSERT_TRUE(test_env.Start());

    // Open a volume
    volume_ = test_env.client->OpenVolume(test_env.volume_name_,
    NULL,  // No SSL options.
                                          test_env.options);
    user_credentials_ = test_env.user_credentials;
  }
  virtual void TearDown() {
    // volume->Close();
    test_env.Stop();
  }

  TestEnvironment test_env;
  xtreemfs::Volume* volume_;
  xtreemfs::pbrpc::UserCredentials user_credentials_;
};

class HashTreeADTest : public OnlineTest {
 protected:
  virtual void SetUp() {
    OnlineTest::SetUp();

    // Open a file.
    file =
        volume_->OpenFile(
            user_credentials_,
            "/test_meta_file",
            static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_CREAT // NOLINT
                | xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_TRUNC
                | xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDWR),
            0777, true);

    tree = new HashTreeAD(file, 4);
  }

  virtual void TearDown() {
    delete tree;

    OnlineTest::TearDown();
  }

  FileHandle* file;
  HashTreeAD* tree;
};

class HashTreeADTest_Offline : public OfflineTest {
 protected:
  virtual void SetUp() {
    OfflineTest::SetUp();

    // Open a file.
    file =
        volume_->OpenFile(
            user_credentials_,
            "/test_file",
            static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_CREAT // NOLINT
                | xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_TRUNC
                | xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDWR));

    tree = new HashTreeAD(file, 4);
  }

  virtual void TearDown() {
    delete tree;

    OfflineTest::TearDown();
  }

  FileHandle* file;
  HashTreeAD* tree;
};

class HashTreeADTest_Node : public HashTreeADTest_Offline {
 protected:
  virtual void SetUp() {
    HashTreeADTest_Offline::SetUp();
  }

  virtual void TearDown() {
    HashTreeADTest_Offline::TearDown();
  }
};

/*
 */
TEST_F(HashTreeADTest_Node, Sibling) {
  HashTreeAD::Node x(0, 0);

  x = HashTreeAD::Node(0, 2).LeftSibling();
  EXPECT_EQ(0, x.level);
  EXPECT_EQ(2, x.n);

  x = HashTreeAD::Node(0, 11).LeftSibling();
  EXPECT_EQ(0, x.level);
  EXPECT_EQ(10, x.n);

  x = HashTreeAD::Node(2, 1).LeftSibling();
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(0, x.n);

  tree->SetSize(15);
  x = HashTreeAD::Node(0, 2).RightSibling(tree);
  EXPECT_EQ(0, x.level);
  EXPECT_EQ(3, x.n);

  x = HashTreeAD::Node(0, 11).RightSibling(tree);
  EXPECT_EQ(0, x.level);
  EXPECT_EQ(11, x.n);

  x = HashTreeAD::Node(2, 1).RightSibling(tree);
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(1, x.n);
}

TEST_F(HashTreeADTest_Node, Parent) {
  tree->SetSize(15);
  HashTreeAD::Node x(0, 0);

  x = HashTreeAD::Node(0, 5).Parent(tree);
  EXPECT_EQ(1, x.level);
  EXPECT_EQ(2, x.n);

  x = HashTreeAD::Node(1, 7).Parent(tree);
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(3, x.n);

  x = HashTreeAD::Node(3, 0).Parent(tree);
  EXPECT_EQ(4, x.level);
  EXPECT_EQ(0, x.n);

  tree->SetSize(2);

  x = HashTreeAD::Node(0, 2).Parent(tree);
  EXPECT_EQ(1, x.level);
  EXPECT_EQ(1, x.n);

  x = HashTreeAD::Node(1, 1).Parent(tree);
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(0, x.n);

  tree->SetSize(4);

  x = HashTreeAD::Node(0, 4).Parent(tree);
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(1, x.n);

  x = HashTreeAD::Node(2, 1).Parent(tree);
  EXPECT_EQ(3, x.level);
  EXPECT_EQ(0, x.n);

  tree->SetSize(5);
  EXPECT_EQ(10, tree->max_node_number_);

  x = HashTreeAD::Node(0, 4).Parent(tree);
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(1, x.n);

  x = HashTreeAD::Node(0, 5).Parent(tree);
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(1, x.n);
}

TEST_F(HashTreeADTest_Node, Child) {
  tree->SetSize(15);
  HashTreeAD::Node x(0, 0);

  x = HashTreeAD::Node(1, 5).LeftChild(tree);
  EXPECT_EQ(x.level, 0);
  EXPECT_EQ(x.n, 10);

  x = HashTreeAD::Node(3, 1).LeftChild(tree);
  EXPECT_EQ(x.level, 2);
  EXPECT_EQ(x.n, 2);

  x = HashTreeAD::Node(1, 7).RightChild(tree);
  EXPECT_EQ(x.level, 0);
  EXPECT_EQ(x.n, 15);

  tree->SetSize(4);

  x = HashTreeAD::Node(2, 1).LeftChild(tree);
  EXPECT_EQ(0, x.level);
  EXPECT_EQ(4, x.n);

  x = HashTreeAD::Node(2, 1).RightChild(tree);
  EXPECT_EQ(0, x.level);
  EXPECT_EQ(4, x.n);
}

TEST_F(HashTreeADTest_Node, NodeNumber) {
  tree->SetSize(15);
  int x;

  x = HashTreeAD::Node(0, 2).NodeNumber(tree);
  EXPECT_EQ(4, x);

  x = HashTreeAD::Node(0, 7).NodeNumber(tree);
  EXPECT_EQ(13, x);

  x = HashTreeAD::Node(1, 6).NodeNumber(tree);
  EXPECT_EQ(26, x);

  x = HashTreeAD::Node(2, 1).NodeNumber(tree);
  EXPECT_EQ(7, x);

  tree->SetSize(2);
  EXPECT_EQ(5, tree->max_node_number_);

  x = HashTreeAD::Node(1, 5).NodeNumber(tree);
  EXPECT_EQ(19, x);

  x = HashTreeAD::Node(2, 0).NodeNumber(tree);
  EXPECT_EQ(5, x);

  tree->SetSize(10);
  EXPECT_EQ(21, tree->max_node_number_);

  x = HashTreeAD::Node(0, 1).NodeNumber(tree);
  EXPECT_EQ(1, x);

  x = HashTreeAD::Node(0, 2).NodeNumber(tree);
  EXPECT_EQ(4, x);

  x = HashTreeAD::Node(1, 0).NodeNumber(tree);
  EXPECT_EQ(2, x);

  x = HashTreeAD::Node(4, 0).NodeNumber(tree);
  EXPECT_EQ(21, x);
}

TEST_F(HashTreeADTest_Node, CommonAncestor) {
  tree->SetSize(15);
  HashTreeAD::Node x(0, 0);

  x = HashTreeAD::Node(0, 3).CommonAncestor(tree, HashTreeAD::Node(0, 7));
  EXPECT_EQ(3, x.level);
  EXPECT_EQ(0, x.n);

  x = HashTreeAD::Node(0, 9).CommonAncestor(tree, HashTreeAD::Node(0, 10));
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(2, x.n);

  x = HashTreeAD::Node(0, 5).CommonAncestor(tree, HashTreeAD::Node(0, 10));
  EXPECT_EQ(4, x.level);
  EXPECT_EQ(0, x.n);

  x = HashTreeAD::Node(0, 0).CommonAncestor(tree, HashTreeAD::Node(0, 0));
  EXPECT_EQ(1, x.level);
  EXPECT_EQ(0, x.n);

  x = HashTreeAD::Node(0, 9).CommonAncestor(tree, HashTreeAD::Node(0, 9));
  EXPECT_EQ(1, x.level);
  EXPECT_EQ(4, x.n);

  tree->SetSize(5);

  x = HashTreeAD::Node(0, 4).CommonAncestor(tree, HashTreeAD::Node(0, 5));
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(1, x.n);
}

TEST_F(HashTreeADTest_Node, NumberOfNodes) {
  tree->SetSize(15);
  int x;

  x = tree->NumberOfNodes(0, 0);
  EXPECT_EQ(0, x);

  x = tree->NumberOfNodes(0, 1);
  EXPECT_EQ(1, x);

  x = tree->NumberOfNodes(0, 2);
  EXPECT_EQ(2, x);

  x = tree->NumberOfNodes(0, 3);
  EXPECT_EQ(2, x);

  x = tree->NumberOfNodes(0, 4);
  EXPECT_EQ(2, x);

  x = tree->NumberOfNodes(0, 7);
  EXPECT_EQ(4, x);

  x = tree->NumberOfNodes(1, 4);
  EXPECT_EQ(2, x);

  x = tree->NumberOfNodes(1, 6);
  EXPECT_EQ(2, x);

  x = tree->NumberOfNodes(2, 16);
  EXPECT_EQ(2, x);
}

TEST_F(HashTreeADTest_Offline, RequiredNodesForRead) {
  boost::icl::interval_set<int> nodeNumbers;
  boost::icl::interval_set<int> x;

  tree->SetSize(2);
  EXPECT_EQ(5, tree->max_node_number_);

  x = tree->RequiredNodesForRead(0, 2);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(0, 5));
  EXPECT_EQ(nodeNumbers, x);

  tree->SetSize(11);

  x = tree->RequiredNodesForRead(11, 11);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(14, 15));
  nodeNumbers.add(boost::icl::interval<int>::closed(18, 22));
  EXPECT_EQ(nodeNumbers, x);

  tree->SetSize(15);

  x = tree->RequiredNodesForRead(1, 1);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(0, 3));
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(boost::icl::interval<int>::closed(14, 15));
  nodeNumbers.add(boost::icl::interval<int>::closed(30, 30));
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForRead(5, 6);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 15));
  nodeNumbers.add(boost::icl::interval<int>::closed(30, 30));
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForRead(5, 10);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 23));
  nodeNumbers.add(boost::icl::interval<int>::closed(30, 30));
  EXPECT_EQ(nodeNumbers, x);
}

TEST_F(HashTreeADTest_Offline, RequiredNodesForWrite) {
  boost::icl::interval_set<int> nodeNumbers;
  boost::icl::interval_set<int> x;

  tree->max_leaf_number_ = -1;
  tree->max_level_ = 0;
  tree->max_node_number_ = -1;
  EXPECT_EQ(-1, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(1, true, 2, true);
  nodeNumbers.clear();
  EXPECT_EQ(nodeNumbers, x);

  tree->SetSize(-1);
  EXPECT_EQ(0, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(1, true, 2, true);
  nodeNumbers.clear();
  nodeNumbers.add(0);
  EXPECT_EQ(nodeNumbers, x);

  tree->SetSize(0);
  EXPECT_EQ(1, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(1, true, 2, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(0, 1));
  EXPECT_EQ(nodeNumbers, x);

  tree->SetSize(1);
  EXPECT_EQ(2, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(4, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(0, 2));
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(4, true, 6, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(0, 2));
  EXPECT_EQ(nodeNumbers, x);

  tree->SetSize(2);
  EXPECT_EQ(5, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(0, true, 2, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(5, 5));
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(0, true, 3, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(5, 5));
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(1, true, 2, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(0, 3));
  nodeNumbers.add(5);
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(2, true, 2, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 3));
  nodeNumbers.add(5);
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(3, true, 3, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 5));
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(4, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 5));
  EXPECT_EQ(nodeNumbers, x);

  tree->SetSize(3);
  EXPECT_EQ(6, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(4, true, 4, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 6));
  EXPECT_EQ(nodeNumbers, x);

  tree->SetSize(4);
  EXPECT_EQ(9, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(4, true, 4, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(9);
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(5, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 9));
  EXPECT_EQ(nodeNumbers, x);

  tree->SetSize(5);
  EXPECT_EQ(10, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(0, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(10);
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(2, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 3));
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(10);
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(4, true, 4, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 10));
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(4, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(10);
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(6, true, 6, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 10));
  EXPECT_EQ(nodeNumbers, x);

  tree->SetSize(7);
  EXPECT_EQ(14, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(2, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 3));
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(boost::icl::interval<int>::closed(10, 11));
  nodeNumbers.add(14);
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(4, true, 7, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(14);
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(2, true, 3, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 3));
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(14);
  EXPECT_EQ(nodeNumbers, x);

  x = tree->RequiredNodesForWrite(3, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 7));
  nodeNumbers.add(boost::icl::interval<int>::closed(10, 11));
  nodeNumbers.add(14);
  EXPECT_EQ(nodeNumbers, x);
}

TEST_F(HashTreeADTest_Offline, DISABLED_TmpTestOffline) {
}

TEST_F(HashTreeADTest, TmpTest) {
  std::vector<unsigned char> x(4);
  x[0] = '#';
  x[1] = '0';
  x[2] = '0';

  ASSERT_NO_THROW({
    tree->StartWrite(1, true, 2, true);
  });
  x[3] = '1';
  tree->SetLeaf(1, x, boost::asio::buffer("#001"));
  x[3] = '2';
  tree->SetLeaf(2, x, boost::asio::buffer("#002"));
  tree->FinishWrite();

  ASSERT_NO_THROW({
    tree->StartRead(0, 1);
  });
  ASSERT_NO_THROW({
    tree->GetLeaf(0, boost::asio::buffer(std::string(32, '\0')));
  });
  ASSERT_NO_THROW({
    tree->GetLeaf(1, boost::asio::buffer("#001"));
  });
  ASSERT_THROW({
    tree->GetLeaf(1, boost::asio::buffer("#002"));
  }, XtreemFSException);

  tree->StartTruncate(-1, true);
  tree->FinishTruncate(user_credentials_);

  ASSERT_NO_THROW({
    tree->StartWrite(0, true, 0, true);
  });
  x[3] = '0';
  tree->SetLeaf(0, x, boost::asio::buffer("#000"));
  tree->FinishWrite();
}

}  // namespace xtreemfs

