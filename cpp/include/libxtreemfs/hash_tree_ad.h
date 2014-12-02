/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_HASH_TREE_AD_H_
#define CPP_INCLUDE_LIBXTREEMFS_HASH_TREE_AD_H_

#include <boost/asio/buffer.hpp>
#include <boost/icl/interval_set.hpp>
#include <map>
#include <vector>

#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/options.h"
#include "util/crypto/message_digest.h"
#include "util/crypto/sign_algorithm.h"
#include "xtreemfs/GlobalTypes.pb.h"

namespace xtreemfs {

class FileHandleImplementation;

/**
 * A binary hash tree which contains not only the hashes, but can also contain
 * additional data in its leafs an nodes
 */
class HashTreeAD {
 public:
  HashTreeAD(FileHandle* meta_file, SignAlgorithm* sign_algo, int block_size,
             std::string hash, int leaf_adata_size,
             std::string concurrent_write);

  void Init();

  void StartRead(int start_leaf, int end_leaf);

  void StartWrite(int start_leaf, bool complete_start_leaf, int end_leaf,
                  bool complete_end_leaf, bool complete_max_leaf = false);

  void FinishWrite();

  void StartTruncate(int max_leaf_number, bool complete_leaf);

  void FinishTruncate(const xtreemfs::pbrpc::UserCredentials& user_credentials);

  std::vector<unsigned char> GetLeaf(int leaf, boost::asio::const_buffer data);

  void SetLeaf(int leaf, std::vector<unsigned char> adata,
               boost::asio::const_buffer data);

  int64_t file_version();

  int64_t file_size();

  void set_file_size(int64_t file_size);

 private:
  friend class HashTreeADTest_Node_Sibling_Test;
  friend class HashTreeADTest_Node_Parent_Test;
  friend class HashTreeADTest_Node_Child_Test;
  friend class HashTreeADTest_Node_NodeNumber_Test;
  friend class HashTreeADTest_Node_NumberOfNodes_Test;
  friend class HashTreeADTest_Node_CommonAncestor_Test;
  friend class HashTreeADTest_Offline_RequiredNodesForRead_Test;
  friend class HashTreeADTest_Offline_RequiredNodesForWrite_Test;
  friend class HashTreeADTest_SetGetLeaf_Test;

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

    Node RightSibling(const HashTreeAD* tree) const;

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

  friend bool operator!=(Node const& lhs, Node const& rhs);

  void ChangeSize(int max_leaf_number);

  void SetSize(int max_leaf_number);

  bool ReadRootNodeFromFile();

  void ReadNodesFromFile(boost::icl::interval_set<int> nodeNumbers);

  void WriteNodesToFile();

  boost::icl::interval_set<int> RequiredNodesForRead(int start_leaf,
                                                     int end_leaf);

  boost::icl::interval_set<int> RequiredNodesForWrite(int start_leaf,
                                                      bool complete_start_leaf,
                                                      int end_leaf,
                                                      bool complete_end_leaf,
                                                      bool complete_max_leaf =
                                                          false);

  void ValidateTree();

  void UpdateTree(int start_leaf, int end_leaf, int max_leaf);

  std::vector<unsigned char> HashOfNode(Node left_child, Node right_child);

  int GetNodeStartInBytes(int node_number);

  static int LevelStart(int level);

  static int NodeGroupDistance(int level);

  int NodeSize(int level);

  int NumberOfNodes(int level, int pos);

  /**
   * Version of the file.
   */
  int64_t file_version_;

  /**
   * Size of the file.
   */
  int64_t file_size_;

  /**
   * Max leaf number in the hash tree.
   * -1 for empty file.
   * -2 for newly created file.
   */
  int max_leaf_number_;

  /**
   * Max level of the hash tree.
   * 0 for empty file.
   * -1 for non existing tree.
   */
  int max_level_;

  /**
   * Max node number.
   * 0 for empty file.
   * -1 for non existing tree.
   */
  int max_node_number_;

  /**
   * Helper variable for write, storing the start_leaf parameter of StartWrite.
   */
  int start_leaf_;

  /**
   * Helper variable for write, storing the end_leaf parameter of StartWrite.
   */
  int end_leaf_;

  /**
   * Helper variable for write, storing the complete_max_leaf parameter of
   * StartWrite.
   */
  bool complete_max_leaf_;

  /**
   * Helper variable for a size change of tree, storing the old max leaf number.
   */
  int old_max_leaf_;

  /**
   * The hash containted in the root node.
   */
  std::vector<unsigned char> root_hash_;

  /**
   * Storage for the nodes.
   */
  typedef std::map<Node, std::vector<unsigned char> > Nodes_t;
  Nodes_t nodes_;

  /**
   * Storage for the changed leafs.
   */
  Nodes_t changed_leafs_;

  /**
   * The node numbers of the changed nodes.
   */
  boost::icl::interval_set<int> changed_nodes_;

  /**
   * The block size used for encryption in bytes.
   */
  int block_size_;

  /**
   * The hash algorithm used for the hash tree.
   */
  MessageDigest hasher_;

  /**
   * Size (in bytes) of additional data in leaf.
   */
  int leaf_adata_size_;

  /**
   * The method used to ensure consistency for concurrent write
   */
  std::string concurrent_write_;

  /**
   * File handle for the meta file. Not owned by this class. Not closed by this
   * class.
   */
  FileHandleImplementation* meta_file_;

  /**
   * The algorithm used to sign the root node. Not owned by class.
   */
  SignAlgorithm* sign_algo_;
};

} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_LIBXTREEMFS_HASH_TREE_AD_H_
