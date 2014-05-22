/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 *  Licensed under the BSD License, see LICENSE file for details.
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_HASH_TREE_AD_H_
#define CPP_INCLUDE_LIBXTREEMFS_HASH_TREE_AD_H_

#include <boost/icl/interval_set.hpp>
#include <map>
#include <vector>

#include "libxtreemfs/file_handle.h"

namespace xtreemfs {

/**
 * A binary hash tree which contains not only the hashes, but can also contain
 * additional data in its leafs an nodes
 */
class HashTreeAD {
 public:
  explicit HashTreeAD(FileHandle* meta_file);

  ~HashTreeAD();

  void StartRead(int start_leaf, int end_leaf);

  void StartWrite(int start_leaf, int end_leaf);

  void FinishWrite();

  std::vector<char> GetLeaf(int leaf);

  void SetLeaf(int leaf, std::vector<char> leaf_value);

 private:
  friend class HashTreeADTest_Node_Sibling_Test;
  friend class HashTreeADTest_Node_Parent_Test;
  friend class HashTreeADTest_Node_Child_Test;
  friend class HashTreeADTest_Node_NodeNumber_Test;
  friend class HashTreeADTest_Node_NumberOfNodes_Test;
  friend class HashTreeADTest_Node_CommonAncestor_Test;
  friend class HashTreeADTest_Offline_RequiredNodes_Test;
  friend class HashTreeADTest_TmpTest_Test;

  /**
   * Abstract representation of a node in the hash tree.
   * Does not contain the value (hash + optional additional data) of the node.
   */
  class Node {
   public:
    Node(int level, int n);

    Node(int node_number, const HashTreeAD* tree);

    int NodeNumber(const HashTreeAD* tree) const;

    Node Parent(const HashTreeAD* tree, int r = 1) const;

    Node CommonAncestor(const HashTreeAD* tree, Node) const;

    boost::icl::interval_set<int> AncestorsWithSiblings(
        const HashTreeAD* tree) const;

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

  friend bool operator<(Node const& lhs, Node const& rhs);

  void SetSize(int max_leaf_number);

  void ReadNodesFromFile(boost::icl::interval_set<int> nodeNumbers);

  void WriteNodesToFile();

  boost::icl::interval_set<int> RequiredNodesForRead(int start_leaf,
                                                     int end_leaf);

  boost::icl::interval_set<int> RequiredNodesForWrite(int start_leaf,
                                                      int end_leaf);

  int GetNodeStartInBytes(int node_number);

  static int LevelStart(int level);

  static int NodeGroupDistance(int level);

  int NodeSize(int level);

  int NumberOfNodes(int level, int pos);

  static int MostSignificantBitSet(int x);

  /**
   * Number of leafs in the hash tree. Equivalent to the File size in Number
   * of encrypted Blocks.
   */
  int max_leaf_number_;

  int max_level_;

  int max_node_number_;

  /**
   * Storage for the nodes.
   */
  typedef std::map<Node, std::vector<char> > Nodes_t;
  Nodes_t nodes_;

  /**
   * The node numbers of the changed nodes.
   */
  boost::icl::interval_set<int> changed_nodes_;

  /**
   * File handle for the meta file. Not owned by this class. Close is called on
   * destruction.
   */
  FileHandle* meta_file_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_LIBXTREEMFS_HASH_TREE_AD_H_
