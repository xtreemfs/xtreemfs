/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "uuid_resolver_dummy.h"

#include "libxtreemfs/uuid_iterator.h"

namespace xtreemfs {

void UUIDResolverDummy::UUIDToAddress(
    const std::string& uuid, std::string* address) {
  *address = uuid;
}

void UUIDResolverDummy::VolumeNameToMRCUUID(const std::string& volume_name,
                                            std::string* mrc_uuid) {
  *mrc_uuid = volume_name;
}

void UUIDResolverDummy::VolumeNameToMRCUUID(const std::string& volume_name,
                                            UUIDIterator* uuid_iterator) {
  uuid_iterator->AddUUID(volume_name);
}

}  // namespace xtreemfs
