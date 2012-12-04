/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_TYPEDEFS_H_
#define CPP_INCLUDE_LIBXTREEMFS_TYPEDEFS_H_

#include <vector>
#include <string>

namespace xtreemfs {

/** This file contains typedefs which are used by different classes.
 * @file
 */

/** List of network addresses. Addresses have the form "hostname:port". */
class ServiceAddresses {
 public:
  ServiceAddresses() {}
  ServiceAddresses(const char* address) {
    addresses_.push_back(address);
  }
  ServiceAddresses(const std::string& address) {
    addresses_.push_back(address);
  }
  explicit ServiceAddresses(const std::vector<std::string>& addresses) {
    addresses_ = addresses;
  }
  void Add(const std::string& address) {
    addresses_.push_back(address);
  }
  typedef std::vector<std::string> Addresses;
  Addresses GetAddresses() const {
    return addresses_;
  }
  bool empty() const {
    return addresses_.empty();
  }
  size_t size() const {
    return addresses_.size();
  }
  bool IsAddressList() const {
    return addresses_.size() > 1;
  }
 private:
  Addresses addresses_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_TYPEDEFS_H_
