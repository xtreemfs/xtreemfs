/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
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

/** This class stores a list of all UUIDs of a striped replica and is used to
 *  construct ContainerUUIDIterators for a specific stripe index.
 *  It also manages the failure-state of the stored UUIDs. */
class UUIDContainer {
 public:
  UUIDContainer(const xtreemfs::pbrpc::XLocSet& xlocs);

  ~UUIDContainer();

 private:
  typedef std::vector<UUIDItem*> InnerContainer;
  typedef std::vector<UUIDItem*>::iterator InnerIterator;
  typedef std::vector<InnerContainer> Container;
  typedef std::vector<InnerContainer>::iterator Iterator;

  void GetOSDUUIDsFromXlocSet(const xtreemfs::pbrpc::XLocSet& xlocs);

  /** Fills the given uuid_iterator with the UUIDs corresponding to the
   *  given offsets. This is private because it is only called by
   *  ContainerUUIDIterator's ctor. */
  void FillUUIDIterator(ContainerUUIDIterator* uuid_iterator,
                        std::vector<size_t> offsets);

  /** Obtain a lock on this when accessing uuids_ */
  boost::mutex mutex_;

  /** List of List of UUIDs. */
  Container uuids_;

  /** This is needed to only allow ContainerUUIDIterator to call
   *  FillUUIDIterator. A more strict friend statement which would only
   *  declare ContainerUUIDIterator's ctor does not work due to circular
   *  include dependencies.
   */
  friend class ContainerUUIDIterator;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_UUID_CONTAINER_H_
