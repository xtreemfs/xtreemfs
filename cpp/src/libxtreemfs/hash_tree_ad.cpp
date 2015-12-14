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
 * #14/-1  #0  #1    #2  #3    #4  #5     #6  #7     #8  #9    #10 #11   #12 #13
 * 3.0|||| 0.0|0.1 ||1.0|1.1|| 0.2|0.3 |||2.0|2.1||| 0.4|0.5 ||1.2|1.3|| 0.6|0.7
 *
 * Data layout of root node:
 * ============================
 * file version (as uint64_t big-endian) [8 bytes] |
 *   file size (as uint64_t big-endian) [8 bytes] | hash | signature
 *
 * Data layout of other nodes:
 * ============================
 * leaf version (as uint64_t big-endian) [8 bytes] |
 *   additional data of leaf | hash
 */

#include "libxtreemfs/hash_tree_ad.h"

#include <endian.h>
#include <math.h>

#include <boost/foreach.hpp>
#include <boost/smart_ptr/scoped_array.hpp>
#include <algorithm>
#include <vector>
#include <string>

#include "libxtreemfs/file_handle_implementation.h"
#include "libxtreemfs/helper.h"

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

template<typename InputIterator>
bool all_zero(InputIterator begin, InputIterator end) {
  for (; begin != end; begin++) {
    if (*begin != 0) {
      return false;
    }
  }
  return true;
}

bool all_zero(const boost::asio::const_buffer& buffer) {
  return all_zero(
      boost::asio::buffer_cast<const unsigned char*>(buffer),
      boost::asio::buffer_cast<const unsigned char*>(buffer)
          + boost::asio::buffer_size(buffer));
}

bool all_zero(const std::vector<unsigned char>& buffer) {
  return all_zero(buffer.data(), buffer.data() + buffer.size());
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
 *                      -1 for root node.
 */
HashTreeAD::Node::Node(int node_number, const HashTreeAD* tree)
    : level(0) {
  assert(node_number >= -1);

  // special case for root node
  if (node_number >= tree->max_node_number_ || node_number == -1) {
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
  if (level == tree->max_level_ && n == 0) {
    return tree->max_node_number_;
  }
  // special case for all nodes behind root node
  if (level >= tree->max_level_) {
    return tree->max_node_number_ + 1;
  }
  return LevelStart(level) + (n / 2) * NodeGroupDistance(level) + n % 2;
}

/**
 * @param tree    The tree the node belongs to.
 * @return The lowest ancestor at least r level above the node.
 */
HashTreeAD::Node HashTreeAD::Node::Parent(const HashTreeAD* tree, int r) const {
  assert(level + r <= tree->max_level_);
  if (r == 0) {
    return Node(*this);
  }
  Node parent = Node(level + r, n / pow(2, r));
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
    return LeftChild(tree).RightSibling(tree);
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
 * @param tree    The tree the node belongs to.
 * @return The right sibling of the node or the node itself if it is the right
 *         one.
 */
HashTreeAD::Node HashTreeAD::Node::RightSibling(const HashTreeAD* tree) const {
  Node tmp_node = Node(level, n + (n + 1) % 2);
  if (tmp_node.level == 0 && tmp_node.n > tree->max_leaf_number_) {
    assert(tmp_node.n - 1 == tree->max_leaf_number_);
    return Node(0, tmp_node.n - 1);
  }
  return tmp_node;
}

HashTreeAD::HashTreeAD(FileHandle* meta_file, SignAlgorithm* sign_algo,
                       int block_size, std::string hash, int leaf_adata_size,
                       std::string concurrent_write)
    : file_version_(0),
      file_size_(0),
      max_leaf_number_(-1),
      max_level_(0),
      max_node_number_(0),
      old_max_leaf_(-1),
      new_max_leaf_(-1),
      min_max_leaf_(-1),
      block_size_(block_size),
      hasher_(hash),
      leaf_adata_size_(leaf_adata_size),
      concurrent_write_(concurrent_write),
      meta_file_(static_cast<FileHandleImplementation*>(meta_file)),
      sign_algo_(sign_algo) {
  assert(meta_file_ && sign_algo_);
  assert(block_size_ > 0);
}

void HashTreeAD::Init() {
  int max_leaf_number;
  if (ReadRootNodeFromFile()) {
    if (file_size_ > 0) {
      max_leaf_number = (file_size_ - 1) / block_size_;
    } else if (file_size_ == 0) {
      max_leaf_number = -1;
    }
  } else {
    max_leaf_number = -2;
  }

  SetSize(max_leaf_number);
  old_max_leaf_ = max_leaf_number;
  new_max_leaf_ = max_leaf_number;
  min_max_leaf_ = max_leaf_number;
}

/**
 * Starts a read from start_leaf to end_leaf.
 *
 * Init() needs to be called first.
 *
 * @throws XtreemFSException    If stored hash tree is invalid.
 */
void HashTreeAD::StartRead(int start_leaf, int end_leaf) {
  assert(start_leaf <= end_leaf);

  if (start_leaf > new_max_leaf_) {
    // read behind current max object
    return;
  }

  if (min_max_leaf_ >= 0) {
    boost::icl::interval_set<int> nodeNumbers = RequiredNodesForRead(
        start_leaf, std::min(end_leaf, min_max_leaf_));
    ValidateTree(ReadNodesFromFile(nodeNumbers));
  }
}

/**
 * Starts a write from start_leaf to end_leaf.
 *
 * Init() needs to be called first.
 *
 * @param start_leaf    The beginning of the write.
 * @param complete_start_leaf   true if complete start leaf will be overwritten.
 * @param end_leaf    The end of the write.
 * @param complete_end_leaf   true if complete end leaf will be overwritten.
 * @param complete_max_leaf   True if old max leaf will not be changed.
 *
 * @throws XtreemFSException    If stored hash tree is invalid.
 */
void HashTreeAD::StartWrite(int start_leaf, bool complete_start_leaf,
                            int end_leaf, bool complete_end_leaf,
                            bool complete_max_leaf) {
  assert(start_leaf <= end_leaf);

  boost::icl::interval_set<int> nodeNumbers;
  if (concurrent_write_ != "client") {
    nodeNumbers = RequiredNodesForWrite(start_leaf, complete_start_leaf,
                                        end_leaf, complete_end_leaf,
                                        complete_max_leaf);
  } else {
    if (!complete_start_leaf
        && !boost::icl::contains(changed_leafs_numbers_, start_leaf)
        && start_leaf <= min_max_leaf_) {
      nodeNumbers += RequiredNodesForRead(start_leaf, start_leaf);
    }
    if (!complete_end_leaf
        && !boost::icl::contains(changed_leafs_numbers_, end_leaf)
        && end_leaf <= min_max_leaf_) {
      nodeNumbers += RequiredNodesForRead(end_leaf, end_leaf);
    }
  }
  ValidateTree(ReadNodesFromFile(nodeNumbers));

  new_max_leaf_ = std::max(new_max_leaf_, end_leaf);
}

/**
 * Finishes a write.
 * Parameters must be identical to the ones used in the StartWrite() call.
 *
 * @param start_leaf    The beginning of the write.
 * @param end_leaf    The end of the write.
 * @param complete_max_leaf   True if old max leaf will not be changed.
 */
void HashTreeAD::FinishWrite(int start_leaf, int end_leaf,
                             bool complete_max_leaf) {
  if (concurrent_write_ == "locks" || concurrent_write_ == "partial-cow"
      || concurrent_write_ == "cow") {
    std::vector<unsigned char> root_hash(root_hash_);
    // store variables that are changed by Init() and we need to restore the
    // current state
    int new_max_leaf = new_max_leaf_;
    int64_t file_size = file_size_;
    // get newest root hash
    Init();
    if (root_hash != root_hash_) {
      // a concurrent write/truncate occurred
      if (max_leaf_number_ <= end_leaf) {
        // write is to the last enc block, this write could have influence on
        // the file size so restore it in case Init() changed it
        file_size_ = file_size;
      }
      // get the new hash tree
      StartWrite(start_leaf, true, end_leaf, true, complete_max_leaf);
    } else {
      // restore old sate in case Init() changed it
      new_max_leaf_ = new_max_leaf;
      file_size_ = file_size;
    }
  }

  if (concurrent_write_ != "client") {
    UpdateTree();
    WriteNodesToFile();
  }
}

/**
 * Starts a truncate to max_leaf_number.
 *
 * Init() needs to be called first.
 *
 * @param max_leaf_number   -1 for empty tree.
 * @param complete_leaf   For shrinking, true if new max leaf will not be
 *                        changed.
 *                        For extending, true if old max leaf will not be
 *                        changed.
 */
void HashTreeAD::StartTruncate(int max_leaf_number, bool complete_leaf) {
  assert(max_leaf_number >= -1);

  if (max_leaf_number >= 0) {
    boost::icl::interval_set<int> nodeNumbers;

    if (concurrent_write_ != "client") {
      if (max_leaf_number <= max_leaf_number_) {
        nodeNumbers = RequiredNodesForWrite(0, true, max_leaf_number,
                                            complete_leaf);
      } else {
        nodeNumbers = RequiredNodesForWrite(max_leaf_number, true,
                                            max_leaf_number, true,
                                            complete_leaf);
      }
    } else {
      int max_leaf = std::min(max_leaf_number, max_leaf_number_);
      if (!complete_leaf
          && !boost::icl::contains(changed_leafs_numbers_, max_leaf)
          && max_leaf >= 0 && max_leaf <= min_max_leaf_) {
        nodeNumbers += RequiredNodesForRead(max_leaf, max_leaf);
      }
      if (max_leaf_number < new_max_leaf_) {
        // remove changed leafs which are truncated
        changed_leafs_numbers_ -= boost::icl::interval<int>::left_open(
            max_leaf_number, boost::icl::last(changed_leafs_numbers_));
        for (Nodes_t::iterator iter = changed_leafs_.begin();
            iter != changed_leafs_.end(); iter++) {
          if (iter->first.n > max_leaf_number) {
            changed_leafs_.erase(iter);
          }
        }
      }
    }
    ValidateTree(ReadNodesFromFile(nodeNumbers));
  } else {
    nodes_.clear();
    stored_nodes_.clear();
    changed_nodes_.clear();
    changed_leafs_.clear();
    changed_leafs_numbers_.clear();
  }

  new_max_leaf_ = max_leaf_number;
  min_max_leaf_ = std::min(min_max_leaf_, max_leaf_number);
}

/**
 * Finishes a truncate.
 */
void HashTreeAD::FinishTruncate(
    const xtreemfs::pbrpc::UserCredentials& user_credentials) {
  if (concurrent_write_ != "client") {
    if (min_max_leaf_ < old_max_leaf_) {
      // The minimum of the max node is smaller then the current one.
      // We need to truncate the meta file to ensure all nodes between them and
      // which are not overwritten will be set to zero.
      // TODO(plieser): object version for truncate
      meta_file_->Truncate(
          user_credentials,
          GetNodeStartInBytes(
              min_max_leaf_ >= 0 ?
                  Node(0, min_max_leaf_).NodeNumber(this) + 1 : 0));
    }
    UpdateTree();
    WriteNodesToFile();
  }
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
 */
std::vector<unsigned char> HashTreeAD::GetLeaf(int leaf,
                                               boost::asio::const_buffer data) {
  std::vector<unsigned char> hash_value = hasher_.digest(data);
  std::vector<unsigned char> leaf_value = GetLeafRAW(leaf);
  if (!std::equal(leaf_value.begin() + sizeof(uint64_t) + leaf_adata_size_,
                  leaf_value.end(), hash_value.begin())) {
    // hash 0 indicates special case in which all data under node is 0
    if (all_zero(leaf_value) && all_zero(data)) {
      return std::vector<unsigned char>();
    } else {
      LogAndThrowXtreemFSException("Hash mismatch in leaf of hash tree");
    }
  }
  return std::vector<unsigned char>(
      leaf_value.begin() + sizeof(uint64_t),
      leaf_value.begin() + sizeof(uint64_t) + leaf_adata_size_);
}

/**
 * Returns the additional data of a leaf.
 * StartRead must be called first.
 * Returns empty vector in case of the unencrypted 0 of a sparse file are read.
 *
 * @param leaf    Leaf number.
 */
std::vector<unsigned char> HashTreeAD::GetLeaf(int leaf) {
  std::vector<unsigned char> leaf_value = GetLeafRAW(leaf);
  // hash 0 indicates special case in which all data under node is 0
  if (all_zero(leaf_value)) {
    return std::vector<unsigned char>();
  } else {
    return std::vector<unsigned char>(
        leaf_value.begin() + sizeof(uint64_t),
        leaf_value.begin() + sizeof(uint64_t) + leaf_adata_size_);
  }
}

int HashTreeAD::GetLeafVersion(int leaf) {
  if (leaf > old_max_leaf_) {
    return 0;
  }
  std::vector<unsigned char> leaf_value = GetLeafRAW(leaf);
  // get file version
  uint64_t leaf_version = *reinterpret_cast<uint64_t*>(leaf_value.data());
  return be64toh(leaf_version);
}

std::vector<unsigned char> HashTreeAD::GetLeafRAW(int leaf) {
  assert(leaf <= new_max_leaf_);
  if (boost::icl::contains(changed_leafs_numbers_, leaf)) {
    return changed_leafs_.at(Node(0, leaf));
  } else if (leaf > min_max_leaf_) {
    return std::vector<unsigned char>();
  } else {
    return nodes_.at(Node(0, leaf));
  }
}

int64_t HashTreeAD::file_size() {
  return file_size_;
}

void HashTreeAD::set_file_size(int64_t file_size) {
  file_size_ = file_size;
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
  assert(leaf <= new_max_leaf_);
  assert(adata.size() == leaf_adata_size_);

  uint64_t leaf_version = htobe64((concurrent_write_ == "cow") ?
      file_version_ + 1 : file_version_);
  std::vector<unsigned char> leaf_value = std::vector<unsigned char>(
      reinterpret_cast<unsigned char*>(&leaf_version),
      reinterpret_cast<unsigned char*>(&leaf_version) + sizeof(uint64_t));

  leaf_value.insert(leaf_value.end(), adata.begin(), adata.end());

  std::vector<unsigned char> hash_value = hasher_.digest(data);
  leaf_value.insert(leaf_value.end(), hash_value.begin(), hash_value.end());

  changed_leafs_[Node(0, leaf)] = leaf_value;
  changed_leafs_numbers_ += leaf;
}

void HashTreeAD::SetLeafAdata(int leaf, std::vector<unsigned char> adata) {
  assert(leaf <= new_max_leaf_);
  assert(adata.size() == leaf_adata_size_);

  if (!boost::icl::contains(changed_leafs_numbers_, leaf)) {
    changed_leafs_[Node(0, leaf)] = nodes_.at(Node(0, leaf));
    changed_leafs_numbers_ += leaf;
  }
  std::copy(adata.begin(), adata.end(),
            changed_leafs_.at(Node(0, leaf)).begin() + sizeof(uint64_t));
}

void HashTreeAD::Flush(const xtreemfs::pbrpc::UserCredentials& user_credentials,
                       bool force_flush) {
  if (!force_flush && boost::icl::is_empty(changed_nodes_)
      && boost::icl::is_empty(changed_leafs_numbers_)
      && old_max_leaf_ == new_max_leaf_ && min_max_leaf_ == old_max_leaf_) {
    // no write or truncate occurred
    return;
  }
  if (new_max_leaf_ == -2) {
    new_max_leaf_ = -1;
  }
  if (min_max_leaf_ < old_max_leaf_) {
    // The minimum of the max node is smaller then the current one.
    // We need to truncate the meta file to ensure all nodes between them and
    // which are not overwritten will be set to zero.
    // TODO(plieser): object version for truncate
    meta_file_->Truncate(
        user_credentials,
        GetNodeStartInBytes(
            min_max_leaf_ >= 0 ?
                Node(0, min_max_leaf_).NodeNumber(this) + 1 : 0));
  }
  UpdateTree();
  WriteNodesToFile();
}

/**
 * Changes the size of the hash tree given the max leaf number.
 */
void HashTreeAD::ChangeSize(int max_leaf_number) {
  assert(max_leaf_number >= -1);

  old_max_leaf_ = max_leaf_number_;
  bool move_node = false;
  Node node_to_move(0, 0);
  if (max_leaf_number > old_max_leaf_) {
    int old_max_node_number = max_node_number_;
    if (old_max_leaf_ >= 0) {
      // old tree was more then just the root
      if (old_max_leaf_ != 0 && IsPowerOfTwo(old_max_leaf_ + 1)) {
        // old tree was complete
        // store old root hash in the now normal node
        changed_nodes_ += old_max_node_number;
      } else {
        // old tree was incomplete
        int r = LeastSignificantBitUnset(old_max_leaf_);
        if (r > 0) {
          // move node downwards
          move_node = true;
          node_to_move = Node(0, old_max_leaf_).Parent(this, r);
        }
      }
    } else {
      // remove old root node(0,0)
      nodes_.clear();
      stored_nodes_.clear();
    }
    SetSize(max_leaf_number);
    if (move_node) {
      Node tmp_node = Node(0, old_max_leaf_).Parent(
          this, LeastSignificantBitUnset(old_max_leaf_));
      nodes_[tmp_node] = nodes_.at(node_to_move);
      changed_nodes_ += tmp_node.NodeNumber(this);
    }
  } else if (max_leaf_number < old_max_leaf_) {
    if (max_leaf_number >= 0 && !IsPowerOfTwo(max_leaf_number + 1)) {
      // new tree is incomplete
      int r = LeastSignificantBitUnset(max_leaf_number);
      if (r > 0) {
        // move node upwards
        move_node = true;
        node_to_move = Node(0, max_leaf_number).Parent(this, r);
      }
    }
    SetSize(max_leaf_number);
    if (move_node) {
      Node tmp_node = Node(0, max_leaf_number).Parent(
          this, LeastSignificantBitUnset(max_leaf_number));
      nodes_[tmp_node] = nodes_.at(node_to_move);
      changed_nodes_ += tmp_node.NodeNumber(this);
    }
  }
}

/**
 * Sets the size of the hash tree given the max leaf number.
 * Only sets the internal variables max_leaf_number_, max_level_ and
 * max_node_number_.
 */
void HashTreeAD::SetSize(int max_leaf_number) {
  assert(max_leaf_number >= -2);

  max_leaf_number_ = max_leaf_number;
  if (max_leaf_number >= 0) {
    max_level_ = MostSignificantBitSet(max_leaf_number) + 1;
    max_node_number_ = Node(0, max_leaf_number).NodeNumber(this) + 1;
  } else {
    if (max_leaf_number == -2) {
      max_node_number_ = -1;
      max_level_ = -1;
    } else {
      max_node_number_ = 0;
      max_level_ = 0;
    }
  }
}

/**
 * Reads root node from the meta file.
 *
 * @return false if root node does not exist.
 *
 * @throws XtreemFSException   If signature of root node is invalid.
 */
bool HashTreeAD::ReadRootNodeFromFile() {
  std::vector<char> buffer(NodeSize(-1));
  int bytes_read = meta_file_->Read(buffer.data(), buffer.size(), 0);
  if (bytes_read == 0 || all_zero(buffer.begin(), buffer.end())) {
    // file didn't exist yet
    // TODO(plieser): file didn't exist yet is not yet a trustable input
    file_size_ = 0;
    return false;
  }
  assert(bytes_read == buffer.size());

  // validate signature of root node
  if (!sign_algo_->Verify(
      boost::asio::buffer(buffer,
                          buffer.size() - sign_algo_->get_signature_size()),
      boost::asio::buffer(
          buffer.data() + buffer.size() - sign_algo_->get_signature_size(),
          sign_algo_->get_signature_size()))) {
    LogAndThrowXtreemFSException("Invalid signature of root node");
  }

  // get file version
  uint64_t file_version = *reinterpret_cast<uint64_t*>(buffer.data());
  file_version_ = be64toh(file_version);

  // get file size
  uint64_t file_size = *reinterpret_cast<uint64_t*>(buffer.data()
      + sizeof(uint64_t));
  file_size_ = be64toh(file_size);

  // store root hash
  root_hash_ = std::vector<unsigned char>(
      buffer.data() + sizeof(uint64_t) + sizeof(uint64_t),
      buffer.data() + buffer.size() - sign_algo_->get_signature_size());

  return true;
}

/**
 * Reads nodes from the meta file and stores them locally.
 *
 * @param nodeNumbers   The node numbers of the nodes to read.
 * @return  The node numbers of the nodes which there actually read and not
 *          already cached.
 */
boost::icl::interval_set<int> HashTreeAD::ReadNodesFromFile(
    boost::icl::interval_set<int> nodeNumbers) {
  bool root_nood_read = false;

  if (concurrent_write_ == "client") {
    nodeNumbers -= stored_nodes_;
    stored_nodes_ += nodeNumbers;
  } else {
    nodes_.clear();
    stored_nodes_ = nodeNumbers;
    changed_nodes_.clear();
  }

  boost::scoped_array<char> buffer;
  int buffer_size = 0;

  if (boost::icl::contains(nodeNumbers, max_node_number_)) {
    // root node is read seperatly
    nodes_.insert(Nodes_t::value_type(Node(max_level_, 0), root_hash_));
    nodeNumbers -= max_node_number_;
    root_nood_read = true;
  }

  BOOST_FOREACH(
      boost::icl::interval_set<int>::interval_type range,
      nodeNumbers) {
    int read_start = GetNodeStartInBytes(boost::icl::first(range));
    int read_end = GetNodeStartInBytes(boost::icl::last(range) + 1);
    int read_size = read_end - read_start;
    int read_count = 0;
    if (buffer_size < read_size) {
      buffer.reset(new char[read_size]);
    }
    int bytes_read = meta_file_->Read(buffer.get(), read_size, read_start,
                                      file_version_);

    for (int i = boost::icl::first(range); i <= boost::icl::last(range); i++) {
      Node node(i, this);
      int node_size = NodeSize(node.level);
      if (read_count < bytes_read) {
        // read node from buffer
        assert(read_count + node_size <= bytes_read);
        nodes_.insert(
            Nodes_t::value_type(
                node,
                std::vector<unsigned char>(
                    buffer.get() + read_count,
                    buffer.get() + read_count + node_size)));
        read_count += node_size;
      } else {
        // node contains only zeros
        nodes_.insert(
            Nodes_t::value_type(node, std::vector<unsigned char>(node_size)));
      }
    }
    assert(read_count == bytes_read);
  }

  if (root_nood_read) {
    nodeNumbers += max_node_number_;
  }

  return nodeNumbers;
}

/**
 * Writes changed nodes to meta file.
 */
void HashTreeAD::WriteNodesToFile() {
  // root node is at the beginning of the file and will be writen last
  assert(boost::icl::contains(changed_nodes_, max_node_number_));
  changed_nodes_.subtract(max_node_number_);

  BOOST_FOREACH(
      boost::icl::interval_set<int>::interval_type range,
      changed_nodes_) {
    int write_start = GetNodeStartInBytes(boost::icl::first(range));
    std::vector<char> buffer;
    for (int i = boost::icl::first(range); i <= boost::icl::last(range); i++) {
      Node node(i, this);
      buffer.insert(buffer.end(), nodes_[node].begin(), nodes_[node].end());
    }
    meta_file_->Write(buffer.data(), buffer.size(), write_start, file_version_);
  }

  // write root node
  meta_file_->Write(reinterpret_cast<char*>(root_node_.data()),
                    root_node_.size(), 0, file_version_);

  // reset changed nodes
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
          std::min(end_node.RightSibling(this).NodeNumber(this) + 2,
                   max_node_number_)));
  // 2. Get the rest of the needed nodes. This are, starting from the common
  // ancestor: the sibling and the parent. The same for the parent, until the
  // root-node is reached
  nodeNumbers +=
      start_node.CommonAncestor(this, end_node).AncestorsWithSiblings(this);

  return nodeNumbers;
}

/**
 * @param start_leaf  Start leaf of write. Must be smaller or equal to end_leaf.
 * @param complete_start_leaf   True if complete start leaf will be overwritten.
 * @param end_leaf    End leaf of write.
 * @param complete_end_leaf   True if complete end leaf will be overwritten.
 * @param complete_max_leaf   Optional, uses for write after current max leaf.
 *                            True if old max leaf will not be changed.
 * @return The node numbers of the required nodes to fetch for a write between
 *         start_leaf and end_leaf.
 */
boost::icl::interval_set<int> HashTreeAD::RequiredNodesForWrite(
    int start_leaf, bool complete_start_leaf, int end_leaf,
    bool complete_end_leaf, bool complete_max_leaf) {
  assert(start_leaf <= end_leaf);

  boost::icl::interval_set<int> nodeNumbers;

  if (max_node_number_ <= 0) {
    if (max_node_number_ == -1) {
      // newly created empty file, hash tree does not exist yet
      return nodeNumbers;
    }
    // emty file, only root node exists
    nodeNumbers += 0;
    return nodeNumbers;
  }

  if (start_leaf > max_leaf_number_) {
    // write behind current max leaf
    if (complete_max_leaf) {
      // Last leaf will not be changed, only get the lowest nodes that that will
      // get changed and it ancestors with their siblings.
      nodeNumbers += Node(0, max_leaf_number_).Parent(
          this, LeastSignificantBitUnset(max_leaf_number_))
          .AncestorsWithSiblings(this);
    } else {
      // Last leaf will be changed, required nodes are the last leaf and it's
      // ancestors with their siblings.
      nodeNumbers += Node(0, max_leaf_number_).AncestorsWithSiblings(this);
    }
  } else {
    // start of write is before or at current max leaf
    if (complete_start_leaf) {
      // Start leaf will be completely overwritten and therefore does not
      // necessarily needs to be read.
      // The lowest node required to be read is the lowest ancestor of the
      // start leaf that is not the left sibling.
      if (start_leaf == 0) {
        nodeNumbers += max_node_number_;
      } else {
        nodeNumbers += Node(0, start_leaf).Parent(
            this, LeastSignificantBitSet(start_leaf)).AncestorsWithSiblings(
            this);
      }
    } else {
      // Start leaf only gets partially overwritten, required nodes are the
      // start leaf an it's ancestors with their siblings.
      nodeNumbers += Node(0, start_leaf).AncestorsWithSiblings(this);
    }
  }

  if (complete_end_leaf) {
    // End leaf will be completely overwritten and therefore does not
    // necessarily needs to be read.
    // The lowest node required to be read is the lowest ancestor of the
    // end leaf that is not the right sibling.
    // No nodes to read if end of write is at or behind current max leaf.
    if (end_leaf < max_leaf_number_) {
      nodeNumbers += Node(0, end_leaf).Parent(
          this, LeastSignificantBitUnset(end_leaf)).AncestorsWithSiblings(this);
    }
  } else {
    // Start leaf only gets partially overwritten, required nodes are the
    // start leaf an it's ancestors with their siblings.
    // No nodes to read if end of write is behind current max leaf.
    if (end_leaf <= max_leaf_number_) {
      nodeNumbers += Node(0, end_leaf).AncestorsWithSiblings(this);
    }
  }

  return nodeNumbers;
}

/**
 * Validates the part of the hash tree stored locally.
 *
 * @param nodeNumbers   The nodes of the tree to validate.
 * @throws XtreemFSException    If stored hash tree is invalid.
 */
void HashTreeAD::ValidateTree(boost::icl::interval_set<int> nodeNumbers) {
  // only validate tree if it exist
  if (max_node_number_ == -1) {
    return;
  }

  // special case for empty file
  if (max_node_number_ == 0) {
    std::vector<unsigned char> stored_hash = nodes_.at(Node(0, 0));
    std::vector<unsigned char> calc_hash = hasher_.digest(
        boost::asio::const_buffer());
    if (nodes_.at(Node(0, 0)) != hasher_.digest(boost::asio::const_buffer())) {
      LogAndThrowXtreemFSException("Hash mismatch in hash tree");
    }
    return;
  }

  BOOST_FOREACH(
      boost::icl::interval_set<int>::interval_type range,
      nodeNumbers) {
    for (int i = boost::icl::first(range); i <= boost::icl::last(range); i++) {
      Node node(i, this);

      if (node.level == 0) {
        // don't validate leafs
        continue;
      }

      Node left_child = node.LeftChild(this);
      if (nodes_.count(left_child) == 0) {
        // node's children were not fetched, so don't check it's hash
        continue;
      }
      Node right_child = node.RightChild(this);

      std::vector<unsigned char> node_hash = nodes_[node];

      // hash 0 indicates special case in which all data under node is 0
      if (all_zero(node_hash)) {
        if (all_zero(nodes_.at(left_child))
            && all_zero(nodes_.at(right_child))) {
          continue;
        }
      }

      if (node_hash != HashOfNode(left_child, right_child)) {
        LogAndThrowXtreemFSException("Hash mismatch in hash tree");
      }
    }
  }
}

/**
 * Updates the hash tree.
 */
void HashTreeAD::UpdateTree() {
  boost::icl::interval_set<int> nodes_to_update = changed_leafs_numbers_;

  ChangeSize(new_max_leaf_);

  // remove stored nodes which are now obsolete
  changed_nodes_ -= boost::icl::interval<int>::left_open(
      max_node_number_, boost::icl::last(changed_nodes_));
  for (Nodes_t::iterator iter = nodes_.begin(); iter != nodes_.end(); iter++) {
    if (iter->first.NodeNumber(this) > max_node_number_) {
      nodes_.erase(iter);
    }
  }

  // insert changed leafs into nodes_
  BOOST_FOREACH(Nodes_t::value_type node, changed_leafs_) {
    nodes_[node.first] = node.second;
  }

  // compute some of the nodes that will be changed
  // for performance only
  BOOST_FOREACH(
      boost::icl::interval_set<int>::interval_type range,
      changed_leafs_numbers_) {
    changed_nodes_ += boost::icl::interval<int>::closed(
        Node(0, boost::icl::first(range)).NodeNumber(this),
        Node(0, boost::icl::last(range)).NodeNumber(this));
  }

  Node skiped_min_max_leaf_ancestor(0, 0);
  if (min_max_leaf_ >= 0 && old_max_leaf_ != max_leaf_number_) {
    // the write was behind the last max leaf or truncate
    // update the ancestors (root node excluded) of the old/new max leaf
    Node node(0, min_max_leaf_);
    if (!boost::icl::contains(changed_nodes_, node.NodeNumber(this))) {
      // the old max leaf was not changed, start at the lowest ancestors that
      // needs to be changed
      skiped_min_max_leaf_ancestor = node.Parent(
          this, LeastSignificantBitUnset(min_max_leaf_));
      if (skiped_min_max_leaf_ancestor.level < max_level_) {
        skiped_min_max_leaf_ancestor = skiped_min_max_leaf_ancestor.Parent(
            this);
      }
    } else {
      nodes_to_update += min_max_leaf_;
    }
  }

  // update the nodes, root node excluded
  Node skiped_end_node = Node(0, 0);
  for (int level = 1; level < max_level_; level++) {
    boost::icl::interval_set<int> old_nodes_to_update = nodes_to_update;
    nodes_to_update.clear();

    // add skipped nodes to the notes to update
    if (skiped_end_node.level == level) {
      nodes_to_update += skiped_end_node.n;
    }
    if (skiped_min_max_leaf_ancestor.level == level) {
      nodes_to_update += skiped_min_max_leaf_ancestor.n;
    }

    // set nodes to update based on the nodes changed on the level below
    BOOST_FOREACH(
        boost::icl::interval_set<int>::interval_type range,
        old_nodes_to_update) {
      int start_n;
      int end_n;
      Node start_parent = Node(level - 1, boost::icl::first(range)).Parent(
          this);
      start_n = start_parent.n;
      if (start_parent.level != level) {
        assert(skiped_end_node.level < level);
        skiped_end_node = start_parent;
      } else {
        Node end_parent = Node(level - 1, boost::icl::last(range)).Parent(this);
        if (end_parent.level == level) {
          end_n = end_parent.n;
        } else {
          assert(skiped_end_node.level < level);
          skiped_end_node = end_parent;
          end_n = Node(level - 1, boost::icl::last(range) - 2).Parent(this).n;
        }
        nodes_to_update += boost::icl::interval<int>::closed(start_n, end_n);
      }
    }

    // update nodes
    BOOST_FOREACH(
        boost::icl::interval_set<int>::interval_type range,
        nodes_to_update) {
      for (int n = boost::icl::first(range); n <= boost::icl::last(range);
          n++) {
        Node node(level, n);
        nodes_[node] = HashOfNode(node.LeftChild(this), node.RightChild(this));
        changed_nodes_ += node.NodeNumber(this);
      }
    }
  }

  if (concurrent_write_ == "partial-cow" || concurrent_write_ == "cow") {
    file_version_++;
  }

  // store file version in root node
  uint64_t file_version = htobe64(file_version_);
  root_node_ = std::vector<unsigned char>(
      reinterpret_cast<unsigned char*>(&file_version),
      reinterpret_cast<unsigned char*>(&file_version) + sizeof(uint64_t));

  // store file size in root node
  uint64_t file_size = htobe64(file_size_);
  root_node_.insert(
      root_node_.end(), reinterpret_cast<unsigned char*>(&file_size),
      reinterpret_cast<unsigned char*>(&file_size) + sizeof(uint64_t));

  // get root node hash
  Node root(max_level_, 0);
  if (max_level_ > 0) {
    // don't update the root hash if shrink truncate to complete tree and
    // last leaf was not changed
    if (min_max_leaf_ == max_leaf_number_
        && (IsPowerOfTwo(max_leaf_number_ + 1) && max_leaf_number_ != 0)
        && boost::icl::is_empty(changed_nodes_)) {
      root_hash_ = nodes_[root];
    } else {
      root_hash_ = HashOfNode(root.LeftChild(this), root.RightChild(this));
    }
  } else {
    root_hash_ = hasher_.digest(boost::asio::const_buffer());
  }
  nodes_[root] = root_hash_;
  root_node_.insert(root_node_.end(), root_hash_.begin(), root_hash_.end());

  // sign root node
  root_node_.resize(NodeSize(-1));
  sign_algo_->Sign(
      boost::asio::buffer(root_node_,
                          root_node_.size() - sign_algo_->get_signature_size()),
      boost::asio::buffer(
          root_node_.data() + root_node_.size()
              - sign_algo_->get_signature_size(),
          sign_algo_->get_signature_size()));

  changed_nodes_ += max_node_number_;

  // reset helper variables
  old_max_leaf_ = new_max_leaf_;
  min_max_leaf_ = new_max_leaf_;
  changed_leafs_.clear();
  changed_leafs_numbers_.clear();
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
  assert(node_number >= -1);
  assert(node_number <= max_node_number_);
  if (node_number == -1) {
    return 0;
  }

  int start = NodeSize(-1);
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
 * @param level   -1 for root node
 * @return The size of a node of the level in bytes.
 */
int HashTreeAD::NodeSize(int level) {
//  assert(level <= max_level_);

  if (level == -1) {
    return sizeof(uint64_t) + sizeof(uint64_t) + hasher_.digest_size()
        + sign_algo_->get_signature_size();
  } else if (level == 0) {
    return sizeof(uint64_t) + hasher_.digest_size() + leaf_adata_size_;
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
