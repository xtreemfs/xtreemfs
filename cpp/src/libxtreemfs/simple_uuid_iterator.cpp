/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *               2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/simple_uuid_iterator.h"

#include "libxtreemfs/uuid_container.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

SimpleUUIDIterator::SimpleUUIDIterator(const xtreemfs::pbrpc::XLocSet& xlocs) {
  ClearAndGetOSDUUIDsFromXlocSet(xlocs);
}

SimpleUUIDIterator::SimpleUUIDIterator(const ServiceAddresses& service_addresses) {
  AddUUIDs(service_addresses);
}

SimpleUUIDIterator::~SimpleUUIDIterator() {
  for (list<UUIDItem*>::iterator it = uuids_.begin();
       it != uuids_.end();
       ++it) {
    delete (*it);
  }
}

void SimpleUUIDIterator::AddUUID(const std::string& uuid) {
  boost::mutex::scoped_lock lock(mutex_);

  UUIDItem* entry = new UUIDItem(uuid);
  uuids_.push_back(entry);

  // If its the first element, set the current UUID to the first element.
  if (uuids_.size() == 1) {
    current_uuid_ = uuids_.begin();
  }
}

void SimpleUUIDIterator::AddUUIDs(const ServiceAddresses& service_addresses) {
  boost::mutex::scoped_lock lock(mutex_);

  ServiceAddresses::Addresses as_list = service_addresses.GetAddresses();
  for (ServiceAddresses::Addresses::const_iterator iter = as_list.begin();
       iter != as_list.end(); ++iter) {
    UUIDItem* entry = new UUIDItem(*iter);
    uuids_.push_back(entry);
  }

  // If the UUIDIterator has been empty before, set the first element as the current UUID.
  if (uuids_.size() == service_addresses.size()) {
    current_uuid_ = uuids_.begin();
  }
}

void SimpleUUIDIterator::ClearAndGetOSDUUIDsFromXlocSet(const xtreemfs::pbrpc::XLocSet& xlocs) {
  boost::mutex::scoped_lock lock(mutex_);

  if (xlocs.replicas_size() == 0) {
    throw EmptyReplicaListInXlocSet("UUIDContainer::GetOSDUUIDFromXlocSet: "
        "Empty replica list in XlocSet: " + xlocs.DebugString());
  }

  // Clear the list.
  for (list<UUIDItem*>::iterator it = uuids_.begin();
       it != uuids_.end();
       ++it) {
    delete (*it);
  }
  uuids_.clear();


  for (int replica_index = 0;
       replica_index < xlocs.replicas_size();
       ++replica_index) {
    const xtreemfs::pbrpc::Replica& replica = xlocs.replicas(replica_index);

    if (replica.osd_uuids_size() == 0) {
      throw NoHeadOSDInXlocSet("UUIDContainer::GetOSDUUIDFromXlocSet: "
          "No head OSD available in XlocSet: " + xlocs.DebugString());
    }

    if (replica.striping_policy().type() == xtreemfs::pbrpc::STRIPING_POLICY_ERASURECODE) {
      // Add every OSD to the list if EC is enabled
      for (int i = 0; i < replica.osd_uuids_size(); ++i) {
        UUIDItem* entry = new UUIDItem(replica.osd_uuids(i));
        uuids_.push_back(entry);
      }
    } else {
      // Add the head OSD of each replica to the list.
      UUIDItem* entry = new UUIDItem(replica.osd_uuids(0));
      uuids_.push_back(entry);
    }
  }

  // Set the current UUID to the first element.
  current_uuid_ = uuids_.begin();
}

void SimpleUUIDIterator::Clear() {
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

void SimpleUUIDIterator::SetCurrentUUID(const std::string& uuid) {
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

  // UUID was not found, add it.
  UUIDItem* entry = new UUIDItem(uuid);
  uuids_.push_back(entry);
  // Add current UUID to the added, last UUID.
  list<UUIDItem*>::iterator it = uuids_.end();
  current_uuid_ = --it;
}

}  // namespace xtreemfs
