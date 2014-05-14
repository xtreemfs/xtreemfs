/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 *  Licensed under the BSD License, see LICENSE file for details.
 */

#include <gtest/gtest.h>

#include "libxtreemfs/hash_tree_ad.h"

namespace xtreemfs {

class HashTreeADTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
  }

  virtual void TearDown() {
  }
};

class HashTreeADTest_Node : public HashTreeADTest {
 protected:
  virtual void SetUp() {
    HashTreeADTest::SetUp();
  }

  virtual void TearDown() {
    HashTreeADTest::TearDown();
  }
};

/*
 */
TEST_F(HashTreeADTest_Node, Sibling) {
  HashTreeAD tree(15);
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

  x = HashTreeAD::Node(0, 2).RightSibling();
  EXPECT_EQ(0, x.level);
  EXPECT_EQ(3, x.n);

  x = HashTreeAD::Node(0, 11).RightSibling();
  EXPECT_EQ(0, x.level);
  EXPECT_EQ(11, x.n);

  x = HashTreeAD::Node(2, 1).RightSibling();
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(1, x.n);
}

TEST_F(HashTreeADTest_Node, Parent) {
  HashTreeAD tree(15);
  HashTreeAD::Node x(0, 0);

  x = HashTreeAD::Node(0, 5).Parent(&tree);
  EXPECT_EQ(1, x.level);
  EXPECT_EQ(2, x.n);

  x = HashTreeAD::Node(1, 7).Parent(&tree);
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(3, x.n);

  x = HashTreeAD::Node(3, 0).Parent(&tree);
  EXPECT_EQ(4, x.level);
  EXPECT_EQ(0, x.n);

  tree = HashTreeAD(2);

  x = HashTreeAD::Node(0, 2).Parent(&tree);
  EXPECT_EQ(1, x.level);
  EXPECT_EQ(1, x.n);

  x = HashTreeAD::Node(1, 1).Parent(&tree);
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(0, x.n);

  tree = HashTreeAD(4);

  x = HashTreeAD::Node(0, 4).Parent(&tree);
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(1, x.n);

  x = HashTreeAD::Node(2, 1).Parent(&tree);
  EXPECT_EQ(3, x.level);
  EXPECT_EQ(0, x.n);

  tree = HashTreeAD(5);
  EXPECT_EQ(10, tree.max_node_number);

  x = HashTreeAD::Node(0, 4).Parent(&tree);
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(1, x.n);

  x = HashTreeAD::Node(0, 5).Parent(&tree);
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(1, x.n);
}

TEST_F(HashTreeADTest_Node, Child) {
  HashTreeAD tree(15);
  HashTreeAD::Node x(0, 0);

  x = HashTreeAD::Node(1, 5).LeftChild(&tree);
  EXPECT_EQ(x.level, 0);
  EXPECT_EQ(x.n, 10);

  x = HashTreeAD::Node(3, 1).LeftChild(&tree);
  EXPECT_EQ(x.level, 2);
  EXPECT_EQ(x.n, 2);

  x = HashTreeAD::Node(1, 7).RightChild(&tree);
  EXPECT_EQ(x.level, 0);
  EXPECT_EQ(x.n, 15);

  tree = HashTreeAD(4);

  x = HashTreeAD::Node(2, 1).LeftChild(&tree);
  EXPECT_EQ(0, x.level);
  EXPECT_EQ(4, x.n);

  x = HashTreeAD::Node(2, 1).RightChild(&tree);
  EXPECT_EQ(0, x.level);
  EXPECT_EQ(4, x.n);
}

TEST_F(HashTreeADTest_Node, NodeNumber) {
  HashTreeAD tree(15);
  int x;

  x = HashTreeAD::Node(0, 2).NodeNumber(&tree);
  EXPECT_EQ(4, x);

  x = HashTreeAD::Node(0, 7).NodeNumber(&tree);
  EXPECT_EQ(13, x);

  x = HashTreeAD::Node(1, 6).NodeNumber(&tree);
  EXPECT_EQ(26, x);

  x = HashTreeAD::Node(2, 1).NodeNumber(&tree);
  EXPECT_EQ(7, x);

  tree = HashTreeAD(2);
  EXPECT_EQ(5, tree.max_node_number);

  x = HashTreeAD::Node(1, 5).NodeNumber(&tree);
  EXPECT_EQ(19, x);

  x = HashTreeAD::Node(2, 0).NodeNumber(&tree);
  EXPECT_EQ(5, x);

  tree = HashTreeAD(10);
  EXPECT_EQ(21, tree.max_node_number);

  x = HashTreeAD::Node(0, 1).NodeNumber(&tree);
  EXPECT_EQ(1, x);

  x = HashTreeAD::Node(0, 2).NodeNumber(&tree);
  EXPECT_EQ(4, x);

  x = HashTreeAD::Node(1, 0).NodeNumber(&tree);
  EXPECT_EQ(2, x);

  x = HashTreeAD::Node(4, 0).NodeNumber(&tree);
  EXPECT_EQ(21, x);
}

TEST_F(HashTreeADTest_Node, CommonAncestor) {
  HashTreeAD tree(15);
  HashTreeAD::Node x(0, 0);

  x = HashTreeAD::Node(0, 3).CommonAncestor(&tree, HashTreeAD::Node(0, 7));
  EXPECT_EQ(3, x.level);
  EXPECT_EQ(0, x.n);

  x = HashTreeAD::Node(0, 9).CommonAncestor(&tree, HashTreeAD::Node(0, 10));
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(2, x.n);

  x = HashTreeAD::Node(0, 5).CommonAncestor(&tree, HashTreeAD::Node(0, 10));
  EXPECT_EQ(4, x.level);
  EXPECT_EQ(0, x.n);

  x = HashTreeAD::Node(0, 0).CommonAncestor(&tree, HashTreeAD::Node(0, 0));
  EXPECT_EQ(1, x.level);
  EXPECT_EQ(0, x.n);

  x = HashTreeAD::Node(0, 9).CommonAncestor(&tree, HashTreeAD::Node(0, 9));
  EXPECT_EQ(1, x.level);
  EXPECT_EQ(4, x.n);

  tree = HashTreeAD(5);

  x = HashTreeAD::Node(0, 4).CommonAncestor(&tree, HashTreeAD::Node(0, 5));
  EXPECT_EQ(2, x.level);
  EXPECT_EQ(1, x.n);
}

TEST_F(HashTreeADTest_Node, NumberOfNodes) {
  HashTreeAD tree(15);
  int x;

  x = tree.NumberOfNodes(0, 0);
  EXPECT_EQ(0, x);

  x = tree.NumberOfNodes(0, 1);
  EXPECT_EQ(1, x);

  x = tree.NumberOfNodes(0, 2);
  EXPECT_EQ(2, x);

  x = tree.NumberOfNodes(0, 3);
  EXPECT_EQ(2, x);

  x = tree.NumberOfNodes(0, 4);
  EXPECT_EQ(2, x);

  x = tree.NumberOfNodes(0, 7);
  EXPECT_EQ(4, x);

  x = tree.NumberOfNodes(1, 4);
  EXPECT_EQ(2, x);

  x = tree.NumberOfNodes(1, 6);
  EXPECT_EQ(2, x);

  x = tree.NumberOfNodes(2, 16);
  EXPECT_EQ(2, x);
}

TEST_F(HashTreeADTest, RequiredNodes) {
  HashTreeAD tree(15);
  boost::icl::interval_set<int> nodeNumbers;
  boost::icl::interval_set<int> x;

  x = tree.RequiredNodes(1, 1);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(0, 3));
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 7));
  nodeNumbers.add(boost::icl::interval<int>::closed(14, 15));
  nodeNumbers.add(boost::icl::interval<int>::closed(30, 30));
  EXPECT_EQ(nodeNumbers, x);

  x = tree.RequiredNodes(5, 6);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 15));
  nodeNumbers.add(boost::icl::interval<int>::closed(30, 30));
  EXPECT_EQ(nodeNumbers, x);

  x = tree.RequiredNodes(5, 10);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(6, 23));
  nodeNumbers.add(boost::icl::interval<int>::closed(30, 30));
  EXPECT_EQ(nodeNumbers, x);

  tree = HashTreeAD(11);
  x = tree.RequiredNodes(11, 11);
  nodeNumbers.clear();
  nodeNumbers.add(boost::icl::interval<int>::closed(14, 15));
  nodeNumbers.add(boost::icl::interval<int>::closed(18, 22));
  EXPECT_EQ(nodeNumbers, x);
}

}  // namespace xtreemfs

