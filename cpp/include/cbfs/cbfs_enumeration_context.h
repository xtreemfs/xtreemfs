/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_CBFS_CBFS_ENUMERATION_CONTEXT_H_
#define CPP_INCLUDE_CBFS_CBFS_ENUMERATION_CONTEXT_H_

#include <boost/cstdint.hpp>

namespace xtreemfs {

namespace pbrpc {

class DirectoryEntries;

}  // namespace pbrpc

struct CbFSEnumerationContext {
  CbFSEnumerationContext();

  ~CbFSEnumerationContext();
  
  /** Index in the complete directory listing where dir_entries starts. */
  boost::uint64_t offset;
  xtreemfs::pbrpc::DirectoryEntries* dir_entries;
  /** Index of next entry which will be returned from dir_entries (starting from
   *  0).
   *
   *  @attention  This is relative to dir_entries, not to the complete dir.
   */
  int next_index;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_CBFS_CBFS_ENUMERATION_CONTEXT_H_
