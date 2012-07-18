/*
 * Copyright (c) 2011 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/uuid_container.h"

#include <sstream>

#include "libxtreemfs/container_uuid_iterator.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

UUIDContainer::UUIDContainer(const xtreemfs::pbrpc::XLocSet& xlocs) {
  // Point to the past-the-end element in case of an empty list.
  //current_uuid_ = uuids_.end();
  GetOSDUUIDsFromXlocSet(xlocs);
}

UUIDContainer::~UUIDContainer() {
  // delete all uuids
  for (iterator it = uuids_.begin(); it != uuids_.end(); ++it)
      for (innerIterator iIt = it->begin(); iIt != it->end(); ++iIt) {
    delete (*iIt);
  }
}

void UUIDContainer::GetOSDUUIDsFromXlocSet(const xtreemfs::pbrpc::XLocSet& xlocs) {
  boost::mutex::scoped_lock lock(mutex_);

  if (xlocs.replicas_size() == 0) {
    Logging::log->getLog(LEVEL_ERROR)
       << "GetOSDUUIDFromXlocSet: Empty replicas list in XlocSet: "
       <<  xlocs.DebugString() << std::endl;
    // TODO(mno): throw something?
  }

  for(uint32_t replica_index = 0; replica_index < xlocs.replicas_size(); ++replica_index) {
    const xtreemfs::pbrpc::Replica& replica = xlocs.replicas(replica_index);
    if (replica.osd_uuids_size() == 0) {
      Logging::log->getLog(LEVEL_WARN)
          << "GetOSDUUIDFromXlocSet: No head OSD available in XlocSet:"
          <<  xlocs.DebugString() << std::endl;
    }
    uuids_.push_back(innerContainer());
    for(uint32_t stripe_index = 0; stripe_index < replica.osd_uuids_size(); ++stripe_index) {
      this->uuids_[replica_index].push_back(new UUIDItem(
          replica.osd_uuids(stripe_index)));
    }
  }

}

void UUIDContainer::GetUUIDIterator(ContainerUUIDIterator* uuid_iterator, std::vector<size_t> offsets) {
  assert(offsets.size() == uuids_.size());
  boost::mutex::scoped_lock lock(mutex_);

  //uuid_iterator->Clear();

  iterator replica_iterator = uuids_.begin();
  std::vector<size_t>::iterator offset_iterator = offsets.begin();

  for(; replica_iterator != uuids_.end(); ++replica_iterator, ++offset_iterator) {
      uuid_iterator->AddUUIDItem((*replica_iterator)[*offset_iterator]);
  }
}

}  // namespace xtreemfs
