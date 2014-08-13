/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
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

#include <boost/algorithm/cxx11/all_of.hpp>
#include <boost/foreach.hpp>
#include <boost/smart_ptr/scoped_array.hpp>
#include <algorithm>
#include <vector>

#include "libxtreemfs/xtreemfs_exception.h"

namespace {
/**
 * @param x
 * @return Position of most significant bit set to 1. Position begins with 0.
 */
int MostSignificantBitSet(int x) {
  int r = 0;
  while ((x = x / 2)) {
    r++;
  }
  return r;
}

/**
 * @param x
 * @return Position of least significant bit set to 1. Position begins with 0.
 *         Returns -1 for 0.
 */
int LeastSignificantBitSet(int x) {
  if (x == 0) {
    return -1;
  }

  int r = 0;
  while ((x % 2) == 0) {
    x = x / 2;
    r++;
  }
  return r;
}

/**
 * @param x
 * @return Position of least significant bit set to 0. Position begins with 0.
 *         Returns -1 for 0.
 */
int LeastSignificantBitUnset(int x) {
  int r = 0;
  while ((x % 2) == 1) {
    x = x / 2;
    r++;
  }
  return r;
}

bool IsPowerOfTwo(int x) {
  assert(x > 0);
  return (x != 0) && ((x & (x - 1)) == 0);
}
}  // namespace unnamed

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

bool operator!=(HashTreeAD::Node const& lhs, HashTreeAD::Node const& rhs) {
  return lhs.level != rhs.level || lhs.n != rhs.n;
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
  if (r == 0) {
    return Node(*this);
  }
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
    // instead.
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
    // instead of the right child.
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

HashTreeAD::HashTreeAD(FileHandle* meta_file, int leaf_adata_size)
    : max_leaf_number_(-1),
      max_level_(0),
      max_node_number_(-1),
      leaf_adata_size_(leaf_adata_size),
      state_(0),
      start_leaf_(0),
      end_leaf_(0),
      old_max_leaf_(0),
      meta_file_(meta_file),
      hasher_("sha256") {
  assert(meta_file);
  // TODO(plieser): set member variables according to file size; difference
  //                between created file an file of file size 0
}

HashTreeAD::~HashTreeAD() {
  meta_file_->Close();
}

/**
 * Starts a read from start_leaf to end_leaf.
 *
 * @throws XtreemFSException    If stored hash tree is invalid.
 */
void HashTreeAD::StartRead(int start_leaf, int end_leaf) {
  assert(state_ == 0);
  assert(start_leaf <= end_leaf);
  end_leaf = std::min(end_leaf, max_leaf_number_);

  boost::icl::interval_set<int> nodeNumbers = RequiredNodesForRead(start_leaf,
                                                                   end_leaf);
  ReadNodesFromFile(nodeNumbers);
  ValidateTree();
}

/**
 * Starts a write from start_leaf to end_leaf.
 *
 * @param start_leaf    The beginning of the write.
 * @param complete_start_leaf   true if complete start leaf will be overwritten.
 * @param end_leaf    The end of the write.
 * @param complete_end_leaf   true if complete end leaf will be overwritten.
 *
 * @throws XtreemFSException    If stored hash tree is invalid.
 */
void HashTreeAD::StartWrite(int start_leaf, bool complete_start_leaf,
                            int end_leaf, bool complete_end_leaf) {
  assert(state_ == 0);
  assert(start_leaf <= end_leaf);

  boost::icl::interval_set<int> nodeNumbers = RequiredNodesForWrite(
      start_leaf, complete_start_leaf, end_leaf, complete_end_leaf);
  ReadNodesFromFile(nodeNumbers);
  ValidateTree();

  if (end_leaf > max_leaf_number_) {
    ChangeSize(end_leaf);
  } else {
    old_max_leaf_ = -1;
  }

  state_ = 1;
  // needed by FinishWrite
  start_leaf_ = start_leaf;
  end_leaf_ = end_leaf;
}

/**
 * Finishes a write.
 */
void HashTreeAD::FinishWrite() {
  assert(state_ == 1);
  UpdateTree(start_leaf_, end_leaf_, old_max_leaf_);
  WriteNodesToFile();
  state_ = 0;
}

/**
 * Starts a truncate to max_leaf_number.
 *
 * @param max_leaf_number   -1 for empty tree.
 * @param complete_leaf   For shrinking. True if max leaf will not be changed.
 */
void HashTreeAD::StartTruncate(int max_leaf_number, bool complete_leaf) {
  assert(state_ == 0);
  assert(max_leaf_number >= -1);

  if (max_leaf_number > 0) {
    boost::icl::interval_set<int> nodeNumbers;

    if (max_leaf_number < max_leaf_number_) {
      nodeNumbers = RequiredNodesForWrite(0, true, max_leaf_number,
                                          complete_leaf);
    } else {
      nodeNumbers = RequiredNodesForWrite(max_leaf_number, true,
                                          max_leaf_number, true);
    }

    ReadNodesFromFile(nodeNumbers);
    ValidateTree();
  } else {
    nodes_.clear();
  }

  ChangeSize(max_leaf_number);
  state_ = 2;
}

/**
 * Finishes a truncate.
 */
void HashTreeAD::FinishTruncate(
    const xtreemfs::pbrpc::UserCredentials& user_credentials) {
  assert(state_ == 2);
  UpdateTree(-1, -1, old_max_leaf_);
  meta_file_->Truncate(user_credentials,
                       GetNodeStartInBytes(max_node_number_ + 1));
  WriteNodesToFile();
  state_ = 0;
}

/**
 * Validates the hash of the data and returns the additional data of a leaf.
 * StartRead must be called first.
 * Returns empty vector in case of the unencrypted 0 of a sparse file are read.
 *
 * @param leaf    Leaf number.
 * @param data    Data of the leaf.
 *
 * @throws XtreemFSException    If hash of the data is incorrect.
 * @throws std::out_of_range    If StartRead was not called, resulting in leaf
 *                              not being fetched from meta file.
 */
std::vector<unsigned char> HashTreeAD::GetLeaf(int leaf,
                                               boost::asio::const_buffer data) {
  assert(leaf <= max_leaf_number_);

  std::vector<unsigned char> hash_value = hasher_.digest(data);
  std::vector<unsigned char> leaf_value = nodes_.at(Node(0, leaf));
  if (!std::equal(leaf_value.begin() + leaf_adata_size_, leaf_value.end(),
                  hash_value.begin())) {
    // hash 0 indicates special case in which all data under node is 0
    if (boost::algorithm::all_of_equal(leaf_value, 0)
        && boost::algorithm::all_of_equal(
            boost::asio::buffer_cast<const unsigned char*>(data),
            boost::asio::buffer_cast<const unsigned char*>(data)
                + boost::asio::buffer_size(data),
            0)) {
      return std::vector<unsigned char>();
    } else {
      throw XtreemFSException("Hash mismatch in leaf of hash tree");
    }
  }
  return std::vector<unsigned char>(leaf_value.begin(),
                                    leaf_value.begin() + leaf_adata_size_);
}

/**
 * Stores the additional data and the hash of the date of a leaf in the hash
 * tree.
 * StartWrite must be called first.
 *
 * @param leaf    Leaf number.
 * @param adata   Additional data of the leaf.
 * @param data    Data of the leaf.
 */
void HashTreeAD::SetLeaf(int leaf, std::vector<unsigned char> adata,
                         boost::asio::const_buffer data) {
  assert(leaf <= max_leaf_number_);
  assert(adata.size() == leaf_adata_size_);

  std::vector<unsigned char> leaf_value = adata;
  std::vector<unsigned char> hash_value = hasher_.digest(data);
  leaf_value.insert(leaf_value.end(), hash_value.begin(), hash_value.end());

  nodes_[Node(0, leaf)] = leaf_value;
  changed_nodes_ += Node(0, leaf).NodeNumber(this);
}

/**
 * Changes the size of the hash tree given the max leaf number.
 */
void HashTreeAD::ChangeSize(int max_leaf_number) {
  assert(max_leaf_number >= -1);

  old_max_leaf_ = max_leaf_number_;
  if (max_leaf_number > old_max_leaf_) {
    // TODO(plieser): overwrite old root node if layout is different from normal
    //                node.
    if (max_node_number_ > 0) {
      std::vector<unsigned char> leaf_value(NodeSize(0));
      nodes_[Node(0, old_max_leaf_ + 1)] = leaf_value;
      changed_nodes_ += max_node_number_;
    }

    SetSize(max_leaf_number);
    if (old_max_leaf_ >= 0 && !IsPowerOfTwo(old_max_leaf_ + 1)) {
      int r = LeastSignificantBitUnset(old_max_leaf_);
      if (r > 0) {
        Node tmp_node = Node(0, old_max_leaf_).Parent(this, r);
        nodes_[tmp_node] = nodes_.at(tmp_node.Parent(this));
        changed_nodes_ += tmp_node.NodeNumber(this);
      }
    }
  } else if (max_leaf_number < old_max_leaf_) {
    if (max_leaf_number >= 0 && !IsPowerOfTwo(max_leaf_number + 1)) {
      int r = LeastSignificantBitUnset(max_leaf_number);
      if (r > 0) {
        Node tmp_node = Node(0, max_leaf_number).Parent(this, r);
        nodes_[tmp_node.Parent(this)] = nodes_.at(tmp_node);
        changed_nodes_ += tmp_node.NodeNumber(this);
      }
    }
    SetSize(max_leaf_number);
  }
}

/**
 * Sets the size of the hash tree given the max leaf number.
 * Only sets the internal variables max_leaf_number_, max_level_ and
 * max_node_number_.
 */
void HashTreeAD::SetSize(int max_leaf_number) {
  assert(max_leaf_number >= -1);

  max_leaf_number_ = max_leaf_number;
  if (max_leaf_number != -1) {
    max_level_ = MostSignificantBitSet(max_leaf_number) + 1;
    max_node_number_ = Node(0, max_leaf_number).NodeNumber(this) + 1;
  } else {
    max_node_number_ = 0;
    max_level_ = 0;
  }
}

/**
 * Reads nodes from the meta file and stores them locally.
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
              std::vector<unsigned char>(
                  buffer.get() + read_count,
                  buffer.get() + read_count + node_size)));
      read_count += node_size;
    }
    assert(read_count == bytes_read);
  }
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
  changed_nodes_.clear();
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
boost::icl::interval_set<int> HashTreeAD::RequiredNodesForWrite(
    int start_leaf, bool complete_start_leaf, int end_leaf,
    bool complete_end_leaf) {
  assert(start_leaf <= end_leaf);

  if (max_node_number_ < 0) {
    return boost::icl::interval_set<int>();
  }

  boost::icl::interval_set<int> nodeNumbers;

  if (max_leaf_number_ < 0) {
    nodeNumbers += max_node_number_;
  } else if (start_leaf > max_leaf_number_) {
    nodeNumbers += Node(0, max_leaf_number_).AncestorsWithSiblings(this);
    // TODO(plieser): optimization if old last block was complete
//      nodeNumbers += Node(0, max_leaf_number_).Parent(
//          this, LeastSignificantBitUnset(max_leaf_number_))
//          .AncestorsWithSiblings(this);
  } else {
    if (complete_start_leaf) {
      if (start_leaf == 0) {
        nodeNumbers += max_node_number_;
      } else {
        nodeNumbers += Node(0, start_leaf).Parent(
            this, LeastSignificantBitSet(start_leaf)).AncestorsWithSiblings(
            this);
      }
    } else {
      nodeNumbers += Node(0, start_leaf).AncestorsWithSiblings(this);
    }
  }

  if (complete_end_leaf) {
    if (end_leaf < max_leaf_number_) {
      nodeNumbers += Node(0, end_leaf).Parent(
          this, LeastSignificantBitUnset(end_leaf)).AncestorsWithSiblings(this);
    }
  } else {
    if (end_leaf <= max_leaf_number_) {
      nodeNumbers += Node(0, end_leaf).AncestorsWithSiblings(this);
    }
  }

  return nodeNumbers;
}

/**
 * Validates the part of the hash tree stored locally.
 *
 * @throws XtreemFSException    If stored hash tree is invalid.
 */
void HashTreeAD::ValidateTree() {
  // TODO(plieser): validate siganture of root hash
  if (max_node_number_ == 0) {
    std::vector<unsigned char> stored_hash = nodes_.at(Node(0, 0));
    std::vector<unsigned char> calc_hash = hasher_.digest(
        boost::asio::const_buffer());
    if (nodes_.at(Node(0, 0)) != hasher_.digest(boost::asio::const_buffer())) {
      throw XtreemFSException("Hash mismatch in hash tree");
    }
    return;
  }

  BOOST_FOREACH(
      Nodes_t::value_type node,
      nodes_) {
    if (node.first.level == 0) {
      continue;
    }
    Node left_child = node.first.LeftChild(this);
    if (nodes_.count(left_child) == 0) {
      // node's children were not fetched, so don't check it's hash
      continue;
    }
    Node right_child = node.first.RightChild(this);

    // hash 0 indicates special case in which all data under node is 0
    if (boost::algorithm::all_of_equal(node.second, 0)) {
      if (boost::algorithm::all_of_equal(nodes_.at(left_child), 0)
          && boost::algorithm::all_of_equal(nodes_.at(right_child), 0)) {
        continue;
      }
    }

    if (node.second != HashOfNode(left_child, right_child)) {
      throw XtreemFSException("Hash mismatch in hash tree");
    }
  }
}

/**
 * Updates the hash tree.
 *
 * @param start_leaf    Beginning of the changed leafs. -1 if no leafs changed.
 * @param end_leaf      End of the changed leafs.
 * @param old_max_leaf  Old max leaf number if it was changed, otherwise -1
 */
void HashTreeAD::UpdateTree(int start_leaf, int end_leaf, int old_max_leaf) {
  if (start_leaf > -1) {
    // update the ancestors hashes for the leafs written to
    int start_n = start_leaf_;
    int end_n = end_leaf_;
    Node skiped_node = Node(0, 0);
    for (int level = 1; level < max_level_; level++) {
      start_n = Node(level - 1, start_n).Parent(this).n;
      Node end_parent = Node(level - 1, end_n).Parent(this);
      if (skiped_node.level == level) {
        end_n = skiped_node.n;
      } else if (end_parent.level == level) {
        end_n = end_parent.n;
      } else {
        assert(skiped_node.level < level);
        skiped_node = end_parent;
        end_n = Node(level - 1, end_n - 2).Parent(this).n;
      }
      for (int n = start_n; n <= end_n; n++) {
        Node node(level, n);
        nodes_[node] = HashOfNode(node.LeftChild(this), node.RightChild(this));
        changed_nodes_ += node.NodeNumber(this);
      }
    }
  }

  if (old_max_leaf != -1
      && (old_max_leaf + 1 < start_leaf || (old_max_leaf % 2) == 1)) {
    // the write was behind the last max leaf
    // update the ancestors of the old max leaf as 0's may have been added to
    // complete it
    // TODO(plieser): optimization if old last block was complete
    Node node(0, old_max_leaf);
    for (int level = 0; level < max_level_ - 1; level++) {
      Node parent = node.Parent(this);
      nodes_[parent] = HashOfNode(node.LeftSibling(), node.RightSibling());
      changed_nodes_ += parent.NodeNumber(this);
      node = parent;
    }
  }

  Node root(max_level_, 0);
  if (max_level_ > 0) {
    nodes_[root] = HashOfNode(root.LeftChild(this), root.RightChild(this));
  } else {
    nodes_[root] = hasher_.digest(boost::asio::const_buffer());
  }
  changed_nodes_ += max_node_number_;

  // TODO(plieser): sign root node
}

/**
 * Returns the hash of a node given it's left and right child.
 */
std::vector<unsigned char> HashTreeAD::HashOfNode(Node left_child,
                                                  Node right_child) {
  boost::asio::const_buffer left_value_buffer;
  boost::asio::const_buffer right_value_buffer;
  std::vector<unsigned char> zero_vector;
  if (nodes_.count(left_child) == 0) {
    // A write behind the file size happened, leaving a hole in the data.
    // Under this node all data is 0.
    zero_vector.resize(NodeSize(left_child.level));
    left_value_buffer = boost::asio::buffer(zero_vector);
  } else {
    left_value_buffer = boost::asio::buffer(nodes_.at(left_child));
  }
  if (left_child != right_child) {
    if (nodes_.count(right_child) == 0) {
      // A write behind the file size happened, leaving a hole in the data.
      // Under this node all data is 0.
      zero_vector.resize(NodeSize(right_child.level));
      right_value_buffer = boost::asio::buffer(zero_vector);
    } else {
      right_value_buffer = boost::asio::buffer(nodes_.at(right_child));
    }
  }

  return hasher_.digest(left_value_buffer, right_value_buffer);
}

/**
 * @param node_number   The number of the node
 * @return The start of the node in bytes.
 */
int HashTreeAD::GetNodeStartInBytes(int node_number) {
  if (node_number > max_node_number_) {
    return GetNodeStartInBytes(max_node_number_) + NodeSize(max_level_);
  }

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
  assert(level <= max_level_);

  if (level == max_level_) {
    // TODO(plieser): size of root node
    return hasher_.digest_size();
  } else if (level == 0) {
    return hasher_.digest_size() + leaf_adata_size_;
  } else {
    return hasher_.digest_size();
  }
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

}  // namespace xtreemfs
