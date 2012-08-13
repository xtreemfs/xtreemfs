/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/container_uuid_iterator.h"

#include "libxtreemfs/uuid_container.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

void ContainerUUIDIterator::Clear() {
  boost::mutex::scoped_lock lock(mutex_);
  uuids_.clear();
  // Empty list, i.e. current UUID is set to the past-the-end element.
  current_uuid_ = uuids_.end();
}

// NOTE: like simple iterator, BUT without implicit adding
void ContainerUUIDIterator::SetCurrentUUID(const string& uuid) {
  boost::mutex::scoped_lock lock(mutex_);

  // Search "uuid" in "uuids_" and set it to the current UUID.
  for (list<UUIDItem*>::iterator it = uuids_.begin();
       it != uuids_.end();
       ++it) {
    if ((*it)->uuid == uuid) {
      current_uuid_ = it;
      // Reset its current state.
      (*current_uuid_)->Reset();
      return;
    }
  }

  // UUID was not found, fail.
  Logging::log->getLog(LEVEL_ERROR)
     << "ContainerUUIDIterator::SetCurrentUUID: uuid not found. " << endl;
}

void ContainerUUIDIterator::AddUUIDItem(UUIDItem* uuid) {
  boost::mutex::scoped_lock lock(mutex_);
  uuids_.push_back(uuid);
  // If its the first element, set the current UUID to the first element.
  if (uuids_.size() == 1) {
    current_uuid_ = uuids_.begin();
  }
}

}  // namespace xtreemfs
