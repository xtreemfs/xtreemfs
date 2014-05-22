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

#include <math.h>

#include <boost/foreach.hpp>
#include <boost/smart_ptr/scoped_array.hpp>
#include <algorithm>

namespace xtreemfs {

/**
 * @param level   The level of the node.
 * @param n       The number of the node in the level.
 */
HashTreeAD::Node::Node(int level, int n)
    : level(level),
      n(n) {
  assert(level >= 0);
  assert(n >= 0);
}

/**
 * @param node_number   The node number. If bigger than the max node number,
 *                      the root node gets constructed.
 */
HashTreeAD::Node::Node(int node_number, const HashTreeAD* tree)
    : level(0) {
  assert(node_number >= 0);

  // special case for root node
  if (node_number >= tree->max_node_number_) {
    level = tree->max_level_;
    n = 0;
    return;
  }

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

bool operator<(HashTreeAD::Node const& lhs, HashTreeAD::Node const& rhs) {
  return lhs.level < rhs.level || (lhs.level == rhs.level && lhs.n < rhs.n);
}

/**
 * @param tree    The tree the node belongs to.
 * @return The node number of the node. This is not the same as Node::n.
 */
int HashTreeAD::Node::NodeNumber(const HashTreeAD* tree) const {
  // special case for root node
  if (level == tree->max_level_) {
    return tree->max_node_number_;
  }
  return LevelStart(level) + (n / 2) * NodeGroupDistance(level) + n % 2;
}

/**
 * @param tree    The tree the node belongs to.
 * @return The rth Parent of the node.
 */
HashTreeAD::Node HashTreeAD::Node::Parent(const HashTreeAD* tree, int r) const {
  assert(level + r <= tree->max_level_);
  Node parent = Node(level + r, n / (2 * r));
  if (parent.level != tree->max_level_
      && parent.NodeNumber(tree) >= tree->max_node_number_) {
    // Incomplete tree, the node does not exist, so return it's parent instead
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
HashTreeAD::Node HashTreeAD::Node::CommonAncestor(const HashTreeAD* tree,
                                                  HashTreeAD::Node node) const {
  assert(level == node.level);
  // Let r be the position of the most significant bit there the n of both
  // nodes differs from each other.
  // When the (r+1)th parent is the common ancestor
  int x = n ^ node.n;
  int r = MostSignificantBitSet(x);
  // return (r+1)th parent
  return Parent(tree, r + 1);
}

/**
 * @param tree    The tree the node belongs to.
 * @return The node numbers of all ancestors (including the node) an their
 *         siblings.
 */
boost::icl::interval_set<int> HashTreeAD::Node::AncestorsWithSiblings(
    const HashTreeAD* tree) const {
  Node i_node(*this);
  boost::icl::interval_set<int> nodeNumbers;
  int i_node_number;
  while (i_node.level < tree->max_level_) {
    i_node_number = i_node.LeftSibling().NodeNumber(tree);
    nodeNumbers.add(
        boost::icl::interval<int>::closed(i_node_number, i_node_number + 1));
    i_node = i_node.Parent(tree);
  }
  i_node_number = i_node.NodeNumber(tree);
  nodeNumbers.add(i_node_number);
  return nodeNumbers;
}

/**
 * @param tree    The tree the node belongs to.
 * @return The left child of the node.
 */
HashTreeAD::Node HashTreeAD::Node::LeftChild(const HashTreeAD* tree) const {
  assert(level > 0);
  Node leftChild = Node(level - 1, n * 2);
  if (leftChild.NodeNumber(tree) >= tree->max_node_number_) {
    // Incomplete tree, the node does not exist, so return it's left child
    // instead
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
HashTreeAD::Node HashTreeAD::Node::RightChild(const HashTreeAD* tree) const {
  assert(level > 0);
  Node rightChild = Node(level - 1, (n * 2) + 1);
  if (rightChild.NodeNumber(tree) >= tree->max_node_number_) {
    // Incomplete tree, the node does not exist, so return the left child
    // instead of the right child
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

HashTreeAD::HashTreeAD(FileHandle* meta_file)
    : max_leaf_number_(0),
      max_level_(0),
      max_node_number_(0),
      meta_file_(meta_file) {
  assert(meta_file);
}

HashTreeAD::~HashTreeAD() {
  meta_file_->Close();
}

void HashTreeAD::StartRead(int start_leaf, int end_leaf) {
  assert(start_leaf <= end_leaf);
  assert(end_leaf <= max_leaf_number_);

  boost::icl::interval_set<int> nodeNumbers = RequiredNodesForRead(start_leaf,
                                                                   end_leaf);
  ReadNodesFromFile(nodeNumbers);
  // TODO(plieser): validate hash tree
}

void HashTreeAD::StartWrite(int start_leaf, int end_leaf) {
  assert(start_leaf <= end_leaf);

  boost::icl::interval_set<int> nodeNumbers = RequiredNodesForRead(start_leaf,
                                                                   end_leaf);
  ReadNodesFromFile(nodeNumbers);
  // TODO(plieser): validate hash tree

  if (end_leaf > max_leaf_number_) {
    // TODO(plieser): update last root node necessary?
    changed_nodes_ += max_leaf_number_;
    SetSize(end_leaf);
  }
}

void HashTreeAD::FinishWrite() {
  // TODO(plieser): recalculate hash tree
  WriteNodesToFile();
}

/**
 * Returns the value of a leaf.
 * Should only be called after a call to StartRead.
 *
 * @throws std::out_of_range    If leaf is not fetched from meta file.
 */
std::vector<char> HashTreeAD::GetLeaf(int leaf) {
  assert(leaf <= max_leaf_number_);

  return nodes_.at(Node(0, leaf));
}

/**
 * Sets the value of a leaf.
 * Should only be called after a call to StartWrite.
 *
 * @param leaf
 * @param
 */
void HashTreeAD::SetLeaf(int leaf, std::vector<char> leaf_value) {
  assert(leaf <= max_leaf_number_);

  nodes_[Node(0, leaf)] = leaf_value;
}

void HashTreeAD::SetSize(int max_leaf_number) {
  max_leaf_number_ = max_leaf_number;
  max_level_ = MostSignificantBitSet(max_leaf_number) + 1;
  max_node_number_ = Node(0, max_leaf_number).NodeNumber(this) + 1;
}

/**
 * Reads nodes from the meta file and stores them.
 *
 * @param nodeNumbers   The node numbers of the nodes to read.
 */
void HashTreeAD::ReadNodesFromFile(boost::icl::interval_set<int> nodeNumbers) {
  nodes_.clear();
  boost::scoped_array<char> buffer;
  int buffer_size = 0;

  BOOST_FOREACH(
      boost::icl::interval_set<int>::interval_type range,
      nodeNumbers) {
    int read_start = GetNodeStartInBytes(range.lower());
    int read_end = GetNodeStartInBytes(range.upper() + 1);
    int read_size = read_end - read_start;
    int read_count = 0;
    if (buffer_size < read_size) {
      buffer.reset(new char[read_size]);
    }
    int bytes_read = meta_file_->Read(buffer.get(), read_size, read_start);
    assert(bytes_read == read_size);

    for (int i = range.lower(); i <= range.upper(); i++) {
      Node node(i, this);
      int node_size = NodeSize(node.level);
      nodes_.insert(
          Nodes_t::value_type(
              node,
              std::vector<char>(buffer.get() + read_count,
                                buffer.get() + read_count + node_size)));
      read_count += node_size;
    }
  }
  return;
}

/**
 * Writes changed nodes to meta file.
 */
void HashTreeAD::WriteNodesToFile() {
  BOOST_FOREACH(
      boost::icl::interval_set<int>::interval_type range,
      changed_nodes_) {
    int write_start = GetNodeStartInBytes(range.lower());
    std::vector<char> buffer;
    for (int i = range.lower(); i <= range.upper(); i++) {
      Node node(i, this);
      buffer.insert(buffer.end(), nodes_[node].begin(), nodes_[node].end());
    }
    meta_file_->Write(buffer.data(), buffer.size(), write_start);
  }
}

/**
 * @param start_leaf  Must be smaller or equal to end_leaf.
 * @param end_leaf    Must be smaller or equal to the max leaf number.
 * @return The node numbers of the required nodes to fetch for a read between
 *         start_leaf and end_leaf.
 */
boost::icl::interval_set<int> HashTreeAD::RequiredNodesForRead(int start_leaf,
                                                               int end_leaf) {
  assert(start_leaf <= end_leaf);
  assert(end_leaf <= max_leaf_number_);

  Node start_node(0, start_leaf);
  Node end_node(0, end_leaf);

  // 1. All needed nodes under the common ancestor from start_leaf and end_leaf
  // are stored in a continuous block.
  boost::icl::interval_set<int> nodeNumbers;
  nodeNumbers.add(
      boost::icl::interval<int>::closed(
          std::max(start_node.LeftSibling().NodeNumber(this) - 2, 0),
          std::min(end_node.RightSibling().NodeNumber(this) + 2,
                   max_node_number_)));
  // 2. Get the rest of the needed nodes. This are, starting from the common
  // ancestor: the sibling and the parent. The same for the parent, until the
  // root-node is reached
  nodeNumbers +=
      start_node.CommonAncestor(this, end_node).AncestorsWithSiblings(this);

  return nodeNumbers;
}

/**
 * @param start_leaf  Must be smaller or equal to end_leaf.
 * @param end_leaf
 * @return The node numbers of the required nodes to fetch for a write between
 *         start_leaf and end_leaf.
 */
boost::icl::interval_set<int> HashTreeAD::RequiredNodesForWrite(int start_leaf,
                                                                int end_leaf) {
  assert(start_leaf <= end_leaf);

  int node_number_left_to_start_leaf = Node(0, start_leaf).NodeNumber(this) - 1;
  Node node_right_to_end_leaf(Node(0, end_leaf).NodeNumber(this) + 1, this);

  boost::icl::interval_set<int> nodeNumbers = node_right_to_end_leaf
      .AncestorsWithSiblings(this);
  if (node_number_left_to_start_leaf >= 0) {
    nodeNumbers += Node(node_number_left_to_start_leaf, this)
        .AncestorsWithSiblings(this);
  }

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
  // TODO(plieser): return size based on size of hash and additional data
//  return pow(10, level);
  return 3;
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
