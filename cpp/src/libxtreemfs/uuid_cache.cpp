/*
 * Copyright (c) 2009-2011 by Patrick Schaefer, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#include "libxtreemfs/uuid_cache.h"

#include <time.h>

#include <map>
#include <string>
#include <vector>

#include "util/logging.h"

using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

void UUIDCache::update(
    const std::string& uuid,
    const std::string& address,
    const uint32_t port,
    const time_t ttls) {
  boost::mutex::scoped_lock lock(mutex_);

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "UUID: registering new UUID "
        << uuid << " "
        << address << ":" << port
        << endl;
  }

  UUIDMapping uuidMapping = UUIDMapping();
  uuidMapping.address = address;
  uuidMapping.uuid = uuid;
  uuidMapping.port = port;
  uuidMapping.timeout = time(NULL) + ttls;  // calc timeout in seconds

  // address contains update-time to evict old entries
  cache_[uuid] = uuidMapping;
}

/**
 * Old UUIDs are invalidated but there is no active pulling of new UUIDs.
 */
std::string UUIDCache::get(const std::string& uuid) {
  boost::mutex::scoped_lock lock(mutex_);

  std::map<string, UUIDMapping >::iterator it = cache_.find(uuid);

  // entry found?
  if (it != cache_.end()) {
    // entry timed out?
    const UUIDMapping mapping = it->second;
    if (time(NULL) < mapping.timeout) {
      // Build ip-address:port from AddressMapping.
      ostringstream s;
      s << mapping.address << ":" << mapping.port;
      return s.str();
    } else {
      // Expired => Remove from cache_.
      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)  << "UUID expired:" << uuid << endl;
      }
      cache_.erase(it);
    }
  }

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "UUID cache miss:" << uuid << endl;
  }
  return "";
}

}  // namespace xtreemfs
