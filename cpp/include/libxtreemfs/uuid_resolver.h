/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_UUID_RESOLVER_H_
#define CPP_INCLUDE_LIBXTREEMFS_UUID_RESOLVER_H_

#include <string>
#include <vector>

namespace xtreemfs {

class RPCOptions;
class SimpleUUIDIterator;

/** Abstract base class which defines the interface to resolve UUIDs from the
 *  DIR service. */
class UUIDResolver {
 public:
  virtual ~UUIDResolver() {}

  /** Resolves the address (ip-address:port) for a given UUID.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws UnknownAddressSchemeException
   */
  virtual void UUIDToAddress(const std::string& uuid, std::string* address) = 0;

  /** Resolves the address (ip-address:port) for a given UUID, using "options".
   *
   * @throws AddressToUUIDNotFoundException
   * @throws UnknownAddressSchemeException
   */
  virtual void UUIDToAddressWithOptions(const std::string& uuid,
                                        std::string* address,
                                        const RPCOptions& options) = 0;

  /** Resolves the UUID for a given volume name.
   *
   * @throws VolumeNotFoundException
   */
  virtual void VolumeNameToMRCUUID(const std::string& volume_name,
                                   std::string* mrc_uuid) = 0;

  /** Resolves the list of UUIDs of the MRC replicas and adds them to the
   *  uuid_iterator object.
   *
   *  @throws VolumeNotFoundException
   */
  virtual void VolumeNameToMRCUUID(const std::string& volume_name,
                                   SimpleUUIDIterator* uuid_iterator) = 0;

  /** Resolves the list of UUIDs of the MRC replicas and returns them as a vector.
   *
   *  @throws VolumeNotFoundException
   */
  virtual std::vector<std::string> VolumeNameToMRCUUIDs(const std::string& volume_name) = 0;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_UUID_RESOLVER_H_
