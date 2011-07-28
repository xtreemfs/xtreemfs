/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_FUSE_CACHED_DIRECTORY_ENTRIES_H_
#define CPP_INCLUDE_FUSE_CACHED_DIRECTORY_ENTRIES_H_

#include <boost/cstdint.hpp>
#include <boost/thread/mutex.hpp>

namespace xtreemfs {

namespace pbrpc {

class DirectoryEntries;

}  // namespace pbrpc

struct CachedDirectoryEntries {
  boost::uint64_t offset;
  xtreemfs::pbrpc::DirectoryEntries* dir_entries;
  boost::mutex mutex;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_FUSE_CACHED_DIRECTORY_ENTRIES_H_
