/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_UUID_RESOLVER_DUMMY_H_
#define CPP_TEST_COMMON_UUID_RESOLVER_DUMMY_H_

#include <string>

#include "libxtreemfs/uuid_resolver.h"

namespace xtreemfs {

class UUIDIterator;

/** Testing class which returns the given UUID as address or volume name. */
class UUIDResolverDummy : public UUIDResolver {
  virtual void UUIDToAddress(const std::string& uuid, std::string* address);

  virtual void VolumeNameToMRCUUID(const std::string& volume_name,
                                   std::string* mrc_uuid);

  virtual void VolumeNameToMRCUUID(const std::string& volume_name,
                                   UUIDIterator* uuid_iterator);
};

}  // namespace xtreemfs

#endif  // CPP_TEST_COMMON_UUID_RESOLVER_DUMMY_H_
