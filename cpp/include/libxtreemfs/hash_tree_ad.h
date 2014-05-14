/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 *  Licensed under the BSD License, see LICENSE file for details.
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_HASH_TREE_AD_H_
#define CPP_INCLUDE_LIBXTREEMFS_HASH_TREE_AD_H_

#include <boost/icl/interval_set.hpp>
#include <map>
#include <string>
#include <vector>

#include "libxtreemfs/file_handle.h"

namespace xtreemfs {

/**
 * A binary hash tree which contains not only the hashes, but can also contain
 * additional data in its leafs an nodes
 */
class HashTreeAD {
 public:
  HashTreeAD(int max_leaf_number);

  void FetchHashes(int start_leaf, int end_leaf);

 private:
  friend class HashTreeADTest_Node_Sibling_Test;
  friend class HashTreeADTest_Node_Parent_Test;
  friend class HashTreeADTest_Node_Child_Test;
  friend class HashTreeADTest_Node_NodeNumber_Test;
  friend class HashTreeADTest_Node_NumberOfNodes_Test;
  friend class HashTreeADTest_Node_CommonAncestor_Test;
  friend class HashTreeADTest_RequiredNodes_Test;

  /**
   * Abstract representation of a node in the hash tree.
   * Does not contain the value (hash + optional additional data) of the node.
   */
  class Node {
   public:
    Node(int level, int n);

    Node(int node_number);

    int NodeNumber(const HashTreeAD* tree) const;

    Node Parent(const HashTreeAD* tree) const;

    Node CommonAncestor(const HashTreeAD* tree, Node) const;

    Node LeftChild(const HashTreeAD* tree) const;

    Node RightChild(const HashTreeAD* tree) const;

    Node LeftSibling() const;

    Node RightSibling() const;

    /**
     * The level of the node. The lowest level is 0.
     */
    int level;

    /**
     * The number of the node in it's level, starting with 0.
     */
    int n;
  };

  boost::icl::interval_set<int> RequiredNodes(int start_leaf, int end_leaf);

  int GetNodeStartInBytes(int node_number);

  std::pair<int, int> GetRangeOfNode(Node);

  static int LevelStart(int level);

  int LevelStartInBytes(int level);

  static int NodeGroupDistance(int level);

  int NodeGroupDistanceInBytes(int level);

  int NodeSize(int level);

  int NumberOfNodes(int level, int pos);

  static int MostSignificantBitSet(int x);

  /**
   * Number of leafs in the hash tree. Equivalent to the File size in Number
   * of encrypted Blocks.
   */
  int max_leaf_number;

  int max_level;

  int max_node_number;

  /**
   * Storage for the nodes.
   */
  std::map<Node, std::string> nodes;

//  FileHandleImplementation meta_file;
};

} /* namespace xtreemfs */

#endif /* CPP_INCLUDE_LIBXTREEMFS_HASH_TREE_AD_H_ */
