/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
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
  GetOSDUUIDsFromXlocSet(xlocs);
}

UUIDContainer::~UUIDContainer() {
  // delete all uuids
  for (Iterator it = uuids_.begin(); it != uuids_.end(); ++it) {
    for (InnerIterator iIt = it->begin(); iIt != it->end(); ++iIt) {
      delete (*iIt);
    }
  }
}

void UUIDContainer::GetOSDUUIDsFromXlocSet(
    const xtreemfs::pbrpc::XLocSet& xlocs) {
  boost::mutex::scoped_lock lock(mutex_);

  if (xlocs.replicas_size() == 0) {
    throw EmptyReplicaListInXlocSet("UUIDContainer::GetOSDUUIDFromXlocSet: "
        "Empty replica list in XlocSet: " + xlocs.DebugString());
  }

  for (int replica_index = 0;
       replica_index < xlocs.replicas_size();
       ++replica_index) {
    const xtreemfs::pbrpc::Replica& replica = xlocs.replicas(replica_index);

    if (replica.osd_uuids_size() == 0) {
      throw NoHeadOSDInXlocSet("UUIDContainer::GetOSDUUIDFromXlocSet: "
          "No head OSD available in XlocSet: " + xlocs.DebugString());
    }

    uuids_.push_back(InnerContainer());
    for (int stripe_index = 0;
         stripe_index < replica.osd_uuids_size();
         ++stripe_index) {
      this->uuids_[replica_index].push_back(new UUIDItem(
          replica.osd_uuids(stripe_index)));
    }
  }

}

void UUIDContainer::FillUUIDIterator(ContainerUUIDIterator* uuid_iterator,
                                     std::vector<size_t> offsets) {
  assert(offsets.size() == uuids_.size());
  boost::mutex::scoped_lock lock(mutex_);

  // NOTE: if this method would be used in another context than the construction
  //       of ContainerUUIDIterator, the following line would be needed:
  //       uuid_iterator->Clear();

  Iterator replica_iterator = uuids_.begin();
  std::vector<size_t>::iterator offset_iterator = offsets.begin();

  for (; replica_iterator != uuids_.end();
       ++replica_iterator, ++offset_iterator) {
    uuid_iterator->AddUUIDItem((*replica_iterator)[*offset_iterator]);
  }
}

}  // namespace xtreemfs
