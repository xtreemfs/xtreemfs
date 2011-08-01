/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/uuid_iterator.h"

#include "libxtreemfs/xtreemfs_exception.h"

using namespace std;

namespace xtreemfs {

UUIDIterator::UUIDIterator() {
  // Point to the past-the-end element in case of an empty list.
  current_uuid_ = uuids_.end();
}

UUIDIterator::~UUIDIterator() {
  for (list<UUIDItem*>::iterator it = uuids_.begin();
       it != uuids_.end();
       ++it) {
    delete (*it);
  }
}

void UUIDIterator::AddUUID(const std::string& uuid) {
  boost::mutex::scoped_lock lock(mutex_);

  UUIDItem* entry = new UUIDItem(uuid);
  uuids_.push_back(entry);

  // If its the first element, set the current UUID to the first element.
  if (uuids_.size() == 1) {
    current_uuid_ = uuids_.begin();
  }
}

void UUIDIterator::Clear() {
  boost::mutex::scoped_lock lock(mutex_);

  for (list<UUIDItem*>::iterator it = uuids_.begin();
       it != uuids_.end();
       ++it) {
    delete (*it);
  }
  uuids_.clear();
  // Empty list, i.e. current UUID is set to the past-the-end element.
  current_uuid_ = uuids_.end();
}

void UUIDIterator::GetUUID(std::string* result) {
  assert(result);
  boost::mutex::scoped_lock lock(mutex_);
  
  if (current_uuid_ == uuids_.end()) {
    throw UUIDIteratorListIsEmpyException("GetUUID() failed as no current "
        " UUID is set. Size of list of UUIDs: " + uuids_.size());
  } else {
    assert(!(*current_uuid_)->marked_as_failed);
    *result = (*current_uuid_)->uuid;
  }
}

void UUIDIterator::MarkUUIDAsFailed(const std::string& uuid) {
  boost::mutex::scoped_lock lock(mutex_);

  // Only take actions if "uuid" is the current UUID.
  if (current_uuid_ != uuids_.end() && (*current_uuid_)->uuid == uuid) {
    (*current_uuid_)->marked_as_failed = true;

    current_uuid_++;
    if (current_uuid_ == uuids_.end()) {
      // Reset the status of all entries and set the first as current UUID.
      for (list<UUIDItem*>::iterator it = uuids_.begin();
           it != uuids_.end();
           ++it) {
        (*it)->marked_as_failed = false;
      }
      current_uuid_ = uuids_.begin();
    }
  }
}

bool UUIDIterator::SetCurrentUUID(const std::string& uuid) {
  boost::mutex::scoped_lock lock(mutex_);
  
  // Search "uuid" in "uuids_" and set it to the current UUID.
  for (list<UUIDItem*>::iterator it = uuids_.begin();
       it != uuids_.end();
       ++it) {
    if ((*it)->uuid == uuid) {
      current_uuid_ = it;
      // Reset its current state.
      (*current_uuid_)->marked_as_failed = false;
      return true;
    }
  }

  return false;
}

}  // namespace xtreemfs
