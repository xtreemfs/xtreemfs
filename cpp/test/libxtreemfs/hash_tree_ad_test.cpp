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
#include "util/crypto/asym_key.h"
#include "util/crypto/sign_algorithm.h"

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

class HashTreeADTest_Offline : public OfflineTest {
 protected:
  virtual void SetUp() {
    OfflineTest::SetUp();

    test_env.options.encryption_hash = "sha256";

    // Open a file.
    file =
        volume_->OpenFile(
            user_credentials_,
            "/test_file",
            static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_CREAT // NOLINT
                | xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_TRUNC
                | xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDWR));

    sign_algo_ = new SignAlgorithm(AsymKey("RSA"),
                                   test_env.options.encryption_hash);
    tree = new HashTreeAD(file, sign_algo_, 4, "sha256", 4, "none");
  }

  virtual void TearDown() {
    delete tree;
    delete sign_algo_;
    file->Close();

    OfflineTest::TearDown();
  }

  FileHandle* file;
  SignAlgorithm* sign_algo_;
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

  tree->SetSize(40);
  EXPECT_EQ(81, tree->max_node_number_);

  x = HashTreeAD::Node(0, 39).Parent(tree, 0);
  EXPECT_EQ(0, x.level);
  EXPECT_EQ(39, x.n);

  x = HashTreeAD::Node(0, 39).Parent(tree, 1);
  EXPECT_EQ(1, x.level);
  EXPECT_EQ(19, x.n);

  x = HashTreeAD::Node(0, 39).Parent(tree, 2);
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(9, x.n);

  x = HashTreeAD::Node(0, 39).Parent(tree, 3);
  EXPECT_EQ(3, x.level);
  EXPECT_EQ(4, x.n);

  x = HashTreeAD::Node(0, 39).Parent(tree, 4);
  EXPECT_EQ(5, x.level);
  EXPECT_EQ(1, x.n);

  x = HashTreeAD::Node(0, 39).Parent(tree, 5);
  EXPECT_EQ(5, x.level);
  EXPECT_EQ(1, x.n);

  x = HashTreeAD::Node(0, 39).Parent(tree, 6);
  EXPECT_EQ(6, x.level);
  EXPECT_EQ(0, x.n);
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
  // TODO(plieser): fix compiler error: ambiguous overload for ‘operator<<’
  // EXPECT_EQ(nodeNumbers, x);
  EXPECT_TRUE(nodeNumbers == x);

  tree->SetSize(11);

  x = tree->RequiredNodesForRead(11, 11);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(14, 15));
  nodeNumbers.add(boost::icl::interval<int>::closed(18, 22));
  EXPECT_TRUE(nodeNumbers == x);

  tree->SetSize(15);

  x = tree->RequiredNodesForRead(1, 1);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(0, 3));
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(boost::icl::interval<int>::closed(14, 15));
  nodeNumbers.add(boost::icl::interval<int>::closed(30, 30));
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForRead(5, 6);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 15));
  nodeNumbers.add(boost::icl::interval<int>::closed(30, 30));
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForRead(5, 10);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 23));
  nodeNumbers.add(boost::icl::interval<int>::closed(30, 30));
  EXPECT_TRUE(nodeNumbers == x);
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
  EXPECT_TRUE(nodeNumbers == x);

  tree->SetSize(-1);
  EXPECT_EQ(0, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(1, true, 2, true);
  nodeNumbers.clear();
  nodeNumbers.add(0);
  EXPECT_TRUE(nodeNumbers == x);

  tree->SetSize(0);
  EXPECT_EQ(1, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(1, true, 2, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(0, 1));
  EXPECT_TRUE(nodeNumbers == x);

  tree->SetSize(1);
  EXPECT_EQ(2, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(4, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(0, 2));
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(4, true, 6, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(0, 2));
  EXPECT_TRUE(nodeNumbers == x);

  tree->SetSize(2);
  EXPECT_EQ(5, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(0, true, 2, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(5, 5));
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(0, true, 3, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(5, 5));
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(1, true, 2, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(0, 3));
  nodeNumbers.add(5);
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(2, true, 2, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 3));
  nodeNumbers.add(5);
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(3, true, 3, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 5));
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(4, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 5));
  EXPECT_TRUE(nodeNumbers == x);

  tree->SetSize(3);
  EXPECT_EQ(6, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(4, true, 4, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 6));
  EXPECT_TRUE(nodeNumbers == x);

  tree->SetSize(4);
  EXPECT_EQ(9, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(4, true, 4, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(9);
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(4, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(boost::icl::interval<int>::closed(9, 9));
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(5, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 9));
//  EXPECT_EQ(nodeNumbers, x);
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(5, true, 5, true, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 9));
//  EXPECT_EQ(nodeNumbers, x);
  EXPECT_TRUE(nodeNumbers == x);

  tree->SetSize(5);
  EXPECT_EQ(10, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(0, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(10);
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(2, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 3));
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(10);
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(4, true, 4, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 10));
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(4, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(10);
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(6, true, 6, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 10));
  EXPECT_TRUE(nodeNumbers == x);

  tree->SetSize(7);
  EXPECT_EQ(14, tree->max_node_number_);

  x = tree->RequiredNodesForWrite(2, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 3));
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(boost::icl::interval<int>::closed(10, 11));
  nodeNumbers.add(14);
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(4, true, 7, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(14);
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(2, true, 3, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 3));
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(14);
  EXPECT_TRUE(nodeNumbers == x);

  x = tree->RequiredNodesForWrite(3, true, 5, true);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(2, 7));
  nodeNumbers.add(boost::icl::interval<int>::closed(10, 11));
  nodeNumbers.add(14);
  EXPECT_TRUE(nodeNumbers == x);
}

}  // namespace xtreemfs

