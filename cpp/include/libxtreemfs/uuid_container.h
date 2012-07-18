/*
 * Copyright (c) 2011 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_UUID_CONTAINER_H_
#define CPP_INCLUDE_LIBXTREEMFS_UUID_CONTAINER_H_

#include <boost/thread/mutex.hpp>
#include <gtest/gtest_prod.h>
#include <list>
#include <string>
#include <vector>

#include "libxtreemfs/uuid_item.h"
#include "xtreemfs/GlobalTypes.pb.h"

namespace xtreemfs {

class ContainerUUIDIterator;

/** Stores a list of all UUIDs of a striped replica and generates UUID Iterators
 *  for a specific stripe index.
 *
 *  It also manages the failure-state of the stored UUIDs.
 */
class UUIDContainer {
 public:
  UUIDContainer(const xtreemfs::pbrpc::XLocSet& xlocs);

  ~UUIDContainer();

  /** Get the current UUID (by default the first in the list).
   *
   * @throws UUIDContainerListIsEmpyException
   */
  void GetUUIDIterator(ContainerUUIDIterator* uuid_iterator, std::vector<size_t> offsets);

 private:
  typedef std::vector<UUIDItem*> innerContainer;
  typedef std::vector<UUIDItem*>::iterator innerIterator;
  typedef std::vector<innerContainer> container;
  typedef std::vector<innerContainer>::iterator iterator;

  void GetOSDUUIDsFromXlocSet(const xtreemfs::pbrpc::XLocSet& xlocs);

  /** Obtain a lock on this when accessing uuids_ or current_uuid_. */
  boost::mutex mutex_;

  /** List of List of UUIDs. */
  container uuids_;

};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_UUID_CONTAINER_H_
