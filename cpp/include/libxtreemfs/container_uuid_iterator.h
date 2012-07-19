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

class ContainerUUIDIterator : public UUIDIterator {
 public:
  ContainerUUIDIterator(UUIDContainer* uuid_container, std::vector<size_t> offsets) {
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
   *  It can only called by UUIDContainer::GetUUIDIterator, hence the
   *  fried-declaration below. */
  void AddUUIDItem(UUIDItem* uuid);
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_CONTAINER_UUID_ITERATOR_H_
