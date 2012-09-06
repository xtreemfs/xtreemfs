/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *               2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_SIMPLE_UUID_ITERATOR_H_
#define CPP_INCLUDE_LIBXTREEMFS_SIMPLE_UUID_ITERATOR_H_

#include <boost/thread/mutex.hpp>
#include <gtest/gtest_prod.h>
#include <list>
#include <string>

#include "libxtreemfs/uuid_item.h"
#include "libxtreemfs/uuid_iterator.h"
#include "libxtreemfs/uuid_container.h"

namespace xtreemfs {

class SimpleUUIDIterator : public UUIDIterator {
 public:
  virtual ~SimpleUUIDIterator();
  virtual void SetCurrentUUID(const std::string& uuid);
  virtual void Clear();
  /** Appends "uuid" to the list of UUIDs. Does not change the current UUID. */
  virtual void AddUUID(const std::string& uuid);
  /** Atomically clears the list and adds "uuid" to avoid an empty list. */
  virtual void ClearAndAddUUID(const std::string& uuid);

  FRIEND_TEST(SimpleUUIDIteratorTest, ClearAndAddUUID);
  FRIEND_TEST(SimpleUUIDIteratorTest, ConcurrentSetAndMarkAsFailed);
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_SIMPLE_UUID_ITERATOR_H_
