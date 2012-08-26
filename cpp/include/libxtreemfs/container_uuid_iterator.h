/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_CONTAINER_UUID_ITERATOR_H_
#define CPP_INCLUDE_LIBXTREEMFS_CONTAINER_UUID_ITERATOR_H_

#include <boost/thread/mutex.hpp>
#include <gtest/gtest_prod.h>
#include <list>
#include <string>

#include "libxtreemfs/uuid_item.h"
#include "libxtreemfs/uuid_iterator.h"
#include "libxtreemfs/uuid_container.h"

namespace xtreemfs {

/** This class is a UUIDIterator that does not own its UUIDItems. Instead it
 *  references a subset of UUIDItems stored in a UUIDContainer. The iterator
 *  is initialized on construction and does not change during its lifetime.
 *  The main use-case of this class is the OSD-addressing in a striped setup.
 *  Such a setup can include multiple replicas, each with a different striping
 *  configuration. So each object can have its individual list of OSDs over all
 *  replicas. This list is needed in order to support redirection and automatic
 *  fail-over. ContainerUUIDIterator stores this list. */
class ContainerUUIDIterator : public UUIDIterator {
 public:
  /** This ctor initializes the iterator from a given UUIDContainer and a
   *  vector of offsets. The offsets specify indices in the two-dimensional
   *  container. */
  ContainerUUIDIterator(UUIDContainer* uuid_container,
                        std::vector<size_t> offsets) {
    uuid_container->FillUUIDIterator(this, offsets);
  }
  virtual void SetCurrentUUID(const std::string& uuid);

 private:
  // UUIDContainer is a friend of this class
  friend void UUIDContainer::FillUUIDIterator(
      ContainerUUIDIterator* uuid_iterator, std::vector<size_t> offsets);

  // the following is for testing
  template<class T> friend struct UUIDAdder;
  template<class T> friend UUIDIterator* CreateUUIDIterator();

  /** Only for testing and UUIDContainer */
  ContainerUUIDIterator() {}
  /** Only for testing and UUIDContainer */
  virtual void Clear();

  /** Add an existing UUIDItem. Ownership is NOT transferred.
   *  It can only be called by UUIDContainer::GetUUIDIterator, hence the
   *  friend-declaration above. */
  void AddUUIDItem(UUIDItem* uuid);
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_CONTAINER_UUID_ITERATOR_H_
