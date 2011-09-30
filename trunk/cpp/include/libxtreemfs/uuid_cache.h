/*
 * Copyright (c) 2009-2011 by Patrick Schaefer, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#ifndef CPP_INCLUDE_LIBXTREEMFS_UUID_CACHE_H_
#define CPP_INCLUDE_LIBXTREEMFS_UUID_CACHE_H_

#include <boost/thread/mutex.hpp>

#include <map>
#include <string>

namespace xtreemfs {

class UUIDCache {
 public:

  void update(const std::string& uuid, const std::string& address,
      const uint32_t port, const time_t timeout);

  std::string get(const std::string& uuid);

 private:
  struct UUIDMapping {
    std::string uuid;
    std::string address;
    uint32_t port;
    time_t timeout;
  };

  std::map<std::string, UUIDMapping > cache_;
  boost::mutex mutex_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_UUID_CACHE_H_
