/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_UUID_ITEM_H_
#define CPP_INCLUDE_LIBXTREEMFS_UUID_ITEM_H_

#include <stdint.h>

#include <boost/interprocess/detail/atomic.hpp>
#include <boost/version.hpp>
#include <string>

#if (BOOST_VERSION < 104800)
using boost::interprocess::detail::atomic_read32;
using boost::interprocess::detail::atomic_write32;
#else
using boost::interprocess::ipcdetail::atomic_read32;
using boost::interprocess::ipcdetail::atomic_write32;
#endif  // BOOST_VERSION < 104800

namespace xtreemfs {

/** Entry object per UUID in list of UUIDs. */
class UUIDItem {
 public:
  UUIDItem(const std::string& add_uuid)
      : uuid(add_uuid),
        marked_as_failed(0) {}

  bool IsFailed() {
    return atomic_read32(&marked_as_failed) > 0;
  }

  void MarkAsFailed() {
    atomic_write32(&marked_as_failed, 1);
  }

  void Reset() {
    atomic_write32(&marked_as_failed, 0);
  };

  const std::string uuid;

 private:
  uint32_t marked_as_failed;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_UUID_ITEM_H_
