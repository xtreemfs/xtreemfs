/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 *  Licensed under the BSD License, see LICENSE file for details.
 */

/*
 * Each leaf represents an encrypted block in form of it's hash (and optional
 * additional data).
 * The other nodes are the hashes of their children (and optional additional
 * data).
 *
 * The Date layout of the tree is designed to group sibling nodes together and
 * the ability to add/delete leafs at the end without the need to rearrange
 * nodes.
 * This minimizes the needed reads/writes.
 *
 * Example tree:
 * =============
 *
 * Level 3:               3.0
 *                   /           \
 * Level 2:       2.0             2.1
 *              /     \         /     \
 * Level 1:   1.0     1.1     1.2     1.3
 *           /   \   /   \   /   \   /   \
 * Level 0: 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7
 *
 * Data layout of example tree:
 * ============================
 * #0  #1    #2  #3    #4  #5     #6  #7     #8  #9    #10 #11   #12 #13     #14
 * 0.0|0.1 ||1.0|1.1|| 0.2|0.3 |||2.0|2.1||| 0.4|0.5 ||1.2|1.3|| 0.6|0.7 ||||3.0
 */

#include "libxtreemfs/hash_tree_ad.h"

#include <algorithm>
#include <boost/foreach.hpp>
#include <boost/smart_ptr/scoped_array.hpp>
#include <math.h>

namespace xtreemfs {

/**
 * @param level   The level of the node.
 * @param n       The number of the node in the level.
 */
HashTreeAD::Node::Node(int level, int n)
    : level(level),
      n(n) {
}

/**
 * @param node_number   The node number.
 */
HashTreeAD::Node::Node(int node_number)
    : level(0) {
  int node_distance;
  // find level
  while (true) {
    node_distance = NodeGroupDistance(level);
    if ((node_number % node_distance) <= 1) {
      break;
    }
    level++;
    // set node_number to original input node_number minus the current level
    // start
    node_number -= pow(2, level);
  }
  // Calculate n
  n = 2 * (node_number / node_distance) + node_number % 2;
}

/**
 * @param tree    The tree the node belongs to.
 * @return The node number of the node. This is not the same as Node::n.
 */
int HashTreeAD::Node::NodeNumber(const HashTreeAD* tree) const {
  if (level == tree->max_level) {
    return tree->max_node_number;
  }
  return LevelStart(level) + (n / 2) * NodeGroupDistance(level) + n % 2;
}

/**
 * @param tree    The tree the node belongs to.
 * @return The Parent of the node.
 */
HashTreeAD::Node HashTreeAD::Node::Parent(const HashTreeAD* tree) const {
  assert(level < tree->max_level);
  Node parent = Node(level + 1, n / 2);
  if (parent.level != tree->max_level
      && parent.NodeNumber(tree) >= tree->max_node_number) {
    return parent.Parent(tree);
  } else {
    return parent;
  }
}

/**
 * @param tree    The tree the node belongs to.
 * @param node    Node must be of the same level.
 * @return The common ancestor (with the lowest level) of the node and the
 *         given on.
 */
HashTreeAD::Node HashTreeAD::Node::CommonAncestor(
    const HashTreeAD* tree, HashTreeAD::Node node) const {
  assert(level == node.level);
  // Let r be the position of the most significant bit there the n of both
  // nodes differs from each other.
  // When the (r+1)th parent is the common ancestor
  int x = n ^ node.n;
  int r = MostSignificantBitSet(x);
  // return (r+1)th parent
  if (r) {
    return Node(level + r, n / (2 * r)).Parent(tree);
  } else {
    return Parent(tree);
  }
}

/**
 * @param tree    The tree the node belongs to.
 * @return The left child of the node.
 */
HashTreeAD::Node HashTreeAD::Node::LeftChild(
    const HashTreeAD* tree) const {
  assert(level > 0);
  Node leftChild = Node(level - 1, n * 2);
  if (leftChild.NodeNumber(tree) >= tree->max_node_number) {
    return leftChild.LeftChild(tree);
  } else {
    return leftChild;
  }
}

/**
 * @param tree    The tree the node belongs to.
 * @return The right child of the node. If the right child does not exist, the
 *         left is returned.
 */
HashTreeAD::Node HashTreeAD::Node::RightChild(
    const HashTreeAD* tree) const {
  assert(level > 0);
  Node rightChild = Node(level - 1, (n * 2) + 1);
  if (rightChild.NodeNumber(tree) >= tree->max_node_number) {
    return LeftChild(tree);
  } else {
    return rightChild;
  }
}

/**
 * @return The left sibling of the node or the node itself if it is the left
 *         one.
 */
HashTreeAD::Node HashTreeAD::Node::LeftSibling() const {
  return Node(level, n - n % 2);
}

/**
 * @return The right sibling of the node or the node itself if it is the right
 *         one.
 *         The node is not guaranteed to exist.
 */
HashTreeAD::Node HashTreeAD::Node::RightSibling() const {
  return Node(level, n + (n + 1) % 2);
}

HashTreeAD::HashTreeAD(int max_leaf_number)
    : max_leaf_number(max_leaf_number),
      max_level(MostSignificantBitSet(max_leaf_number) + 1),
      max_node_number(Node(0, max_leaf_number).NodeNumber(this) + 1) {
}

void HashTreeAD::FetchHashes(int start_leaf, int end_leaf) {
  boost::icl::interval_set<int> nodeNumbers = RequiredNodes(start_leaf,
                                                            end_leaf);

  // read nodes from meta file
  int read_size;
  boost::scoped_array<char> buffer;
  int buffer_size = 0;
  BOOST_FOREACH(boost::icl::interval_set<int>::interval_type range, nodeNumbers) {
    read_size = range.upper() - range.lower();
    if (buffer_size < read_size) {
      buffer.reset(new char[read_size]);
    }
    // TODO (plieser): construct nodes from buffer
//    meta_file.Read(buffer.get(), range.lower(), read_size);
//    std::cout << range << std::endl;
//    std::cout << "start in bytes:" << GetNodeStartInBytes(range.lower())
//              << std::endl;
//    std::cout << "end in bytes:" << GetNodeStartInBytes(range.upper() + 1)
//              << std::endl;
  }

  // TODO (plieser): validate hash tree
}

/**
 * @param start_leaf
 * @param end_leaf
 * @return The node numbers of the required nodes to validate the leafs between
 *         start_leaf and end_leaf.
 */
boost::icl::interval_set<int> HashTreeAD::RequiredNodes(int start_leaf,
                                                          int end_leaf) {
  Node start_node(0, start_leaf);
  Node end_node(0, end_leaf);

// 1. All needed nodes under the common ancestor from start_leaf and end_leaf
// are stored in a continuous block.
  boost::icl::interval_set<int> nodeNumbers;
  nodeNumbers.add(
      boost::icl::interval<int>::closed(
          std::max(start_node.LeftSibling().NodeNumber(this) - 2, 0),
          std::min(end_node.RightSibling().NodeNumber(this) + 2,
                   max_node_number)));
// 2. Get the rest of the needed nodes. This are, starting from the common
// ancestor: the sibling and the parent. The same for the parent, until the
// root-node is reached
  Node i_node = start_node.CommonAncestor(this, end_node);
  int i_node_number;
  while (i_node.level < max_level) {
    i_node_number = i_node.LeftSibling().NodeNumber(this);
    nodeNumbers.add(
        boost::icl::interval<int>::closed(i_node_number, i_node_number + 1));
    i_node = i_node.Parent(this);
  }
  i_node_number = i_node.NodeNumber(this);
  nodeNumbers.add(
      boost::icl::interval<int>::closed(i_node_number, i_node_number));

  return nodeNumbers;
}

/**
 * @param node_number   The number of the node
 * @return The start of the node in bytes.
 */
int HashTreeAD::GetNodeStartInBytes(int node_number) {
  int start = 0;
  int level = 0;
  int x;
  while ((x = NumberOfNodes(level, node_number))) {
    start += x * NodeSize(level);
    level++;
  }
  return start;
}

/**
 * @param level
 * @return The position of the fist node of the level in number of nodes.
 */
int HashTreeAD::LevelStart(int level) {
  return pow(2, level + 1) - 2;
}

/**
 * @param level
 * @return The distance between the beginning of groups of sibling nodes of the
 *         level in number of nodes.
 */
int HashTreeAD::NodeGroupDistance(int level) {
  return pow(2, level + 2);
}

/**
 * @param level
 * @return The size of a node of the level in bytes.
 */
int HashTreeAD::NodeSize(int level) {
  // TODO (plieser): return size based on size of hash and additional data
  return pow(10, level);
}

/**
 * @param level   The level of the nodes which numbers should be returned.
 * @param pos     Position in number of nodes.
 * @return The number of nodes of the level which are before the given position.
 */
int HashTreeAD::NumberOfNodes(int level, int pos) {
  pos -= LevelStart(level);
  if (pos <= 0) {
    return 0;
  }
  int node_distance = NodeGroupDistance(level);
  return 2 * (pos / node_distance) + std::min(pos % node_distance, 2);
}

/**
 * @param x
 * @return Position of most significant bit set to 1. Position begins with 0.
 */
int HashTreeAD::MostSignificantBitSet(int x) {
  int r = 0;
  while ((x = x / 2)) {
    r++;
  }
  return r;
}

} /* namespace xtreemfs */
