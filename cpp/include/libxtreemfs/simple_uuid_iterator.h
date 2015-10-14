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

#include "libxtreemfs/typedefs.h"
#include "libxtreemfs/uuid_item.h"
#include "libxtreemfs/uuid_iterator.h"
#include "libxtreemfs/uuid_container.h"
#include "xtreemfs/GlobalTypes.pb.h"

namespace xtreemfs {

class SimpleUUIDIterator : public UUIDIterator {
 public:
  SimpleUUIDIterator() {};
  SimpleUUIDIterator(const xtreemfs::pbrpc::XLocSet& xlocs);
  SimpleUUIDIterator(const ServiceAddresses& service_addresses);
  virtual ~SimpleUUIDIterator();
  virtual void SetCurrentUUID(const std::string& uuid);
  virtual void Clear();
  /** Appends "uuid" to the list of UUIDs. Does not change the current UUID. */
  virtual void AddUUID(const std::string& uuid);

  /** Appends every "uuid" from service_addresses to the list of the UUIDs. Does not change the current UUID. */
  void AddUUIDs(const ServiceAddresses& service_addresses);

  /** Clear the list and add the head OSD UUIDs of all replicas from the xLocSet. */
  void ClearAndGetOSDUUIDsFromXlocSet(const xtreemfs::pbrpc::XLocSet& xlocs);


  FRIEND_TEST(SimpleUUIDIteratorTest, ConcurrentSetAndMarkAsFailed);
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_SIMPLE_UUID_ITERATOR_H_
