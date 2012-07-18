/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_UUID_ITERATOR_H_
#define CPP_INCLUDE_LIBXTREEMFS_UUID_ITERATOR_H_

#include <boost/thread/mutex.hpp>
#include <gtest/gtest_prod.h>
#include <list>
#include <string>

#include "libxtreemfs/uuid_item.h"
#include "libxtreemfs/uuid_container.h"

namespace xtreemfs {

/** Stores a list of all UUIDs of a replicated service and allows to iterate
 *  through them.
 *
 *  If an UUID was marked as failed and this is the current UUID, the next
 *  call of GetUUID() will return another available, not as failed marked,
 *  UUID.
 *
 *  If the last UUID in the list is marked as failed, the status of all entries
 *  will be reset and the current UUID is set to the first in the list.
 *
 *  Additionally, it is allowed to set the current UUID to a specific one,
 *  regardless of its current state. This is needed in case a service did
 *  redirect a request to another UUID.
 */
class UUIDIterator {
 public:
  UUIDIterator();

  virtual ~UUIDIterator();

  /** Get the current UUID (by default the first in the list).
   *
   * @throws UUIDIteratorListIsEmpyException
   */
  virtual void GetUUID(std::string* result);

  /** Marks "uuid" as failed. Use this function to advance to the next in the
   *  list. */
  virtual void MarkUUIDAsFailed(const std::string& uuid);

  /** Sets "uuid" as current UUID. If uuid was not found in the list of UUIDs,
   *  it will be added to the UUIDIterator. */
  virtual void SetCurrentUUID(const std::string& uuid) = 0;

  /** Clear the list. */
  virtual void Clear() = 0;

  /** Returns the list of UUIDs and their status. */
  virtual std::string DebugString();

 protected:
  /** Obtain a lock on this when accessing uuids_ or current_uuid_. */
  boost::mutex mutex_;

  /** Current UUID (advanced if entries are marked as failed).
   *
   * Please note: "Lists have the important property that insertion and splicing
   *               do not invalidate iterators to list elements [...]"
   *              (http://www.sgi.com/tech/stl/List.html)
   */
  std::list<UUIDItem*>::iterator current_uuid_;

  /** List of UUIDs. */
  std::list<UUIDItem*> uuids_;

  template<typename T>
  FRIEND_TEST(UUIDIteratorTest, ResetAfterEndOfList);
  template<typename T>
  FRIEND_TEST(UUIDIteratorTest, SetCurrentUUID);
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_UUID_ITERATOR_H_
