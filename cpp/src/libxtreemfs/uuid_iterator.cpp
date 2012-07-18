/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/uuid_iterator.h"
#include "util/logging.h"

#include <sstream>

#include "libxtreemfs/uuid_container.h"
#include "libxtreemfs/xtreemfs_exception.h"

using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

UUIDIterator::UUIDIterator() {
  // Point to the past-the-end element in case of an empty list.
  current_uuid_ = uuids_.end();
}

UUIDIterator::~UUIDIterator() {
}

void UUIDIterator::GetUUID(std::string* result) {
  assert(result);
  boost::mutex::scoped_lock lock(mutex_);

  if (current_uuid_ == uuids_.end()) {
    throw UUIDIteratorListIsEmpyException("GetUUID() failed as no current "
        " UUID is set. Size of list of UUIDs: " + uuids_.size());
  } else {
    assert(!(*current_uuid_)->IsFailed());
    *result = (*current_uuid_)->uuid;
  }
}

std::string UUIDIterator::DebugString() {
  ostringstream stream;

  stream << "[ ";

  boost::mutex::scoped_lock lock(mutex_);
  for (list<UUIDItem*>::iterator it = uuids_.begin();
       it != uuids_.end();
       ++it) {
    if (it != uuids_.begin()) {
      stream << ", ";
    }
    stream << "[ " << (*it)->uuid << ", " << (*it)->IsFailed() << "]";
  }

  stream << " ]";

  return stream.str();
}

void UUIDIterator::MarkUUIDAsFailed(const std::string& uuid) {
  boost::mutex::scoped_lock lock(mutex_);

  // Only take actions if "uuid" is the current UUID.
  if (current_uuid_ != uuids_.end() && (*current_uuid_)->uuid == uuid) {
    (*current_uuid_)->MarkAsFailed();

    current_uuid_++;
    if (current_uuid_ == uuids_.end()) {
      // Reset the status of all entries and set the first as current UUID.
      for (list<UUIDItem*>::iterator it = uuids_.begin();
           it != uuids_.end();
           ++it) {
        (*it)->Reset();
      }
      current_uuid_ = uuids_.begin();
    } else {
      // Reset the current UUID to make sure it is not marked as failed.
      (*current_uuid_)->Reset();
    }
  }
}

}  // namespace xtreemfs
