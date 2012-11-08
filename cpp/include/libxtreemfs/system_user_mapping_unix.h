/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_SYSTEM_USER_MAPPING_UNIX_H_
#define CPP_INCLUDE_LIBXTREEMFS_SYSTEM_USER_MAPPING_UNIX_H_

#ifndef WIN32

#include <list>
#include <string>

#include "libxtreemfs/system_user_mapping.h"

namespace xtreemfs {

class SystemUserMappingUnix : public SystemUserMapping {
 public:
  SystemUserMappingUnix() {}

  virtual void GetUserCredentialsForCurrentUser(
      xtreemfs::pbrpc::UserCredentials* user_credentials);

  std::string  UIDToUsername(uid_t uid);

  uid_t        UsernameToUID(const std::string& username);

  std::string  GIDToGroupname(gid_t gid);

  gid_t        GroupnameToGID(const std::string& groupname);

  void         GetGroupnames(uid_t uid,
                             gid_t gid,
                             pid_t pid,
                             std::list<std::string>* groupnames);

};

}  // namespace xtreemfs

#endif  // !WIN32

#endif  // CPP_INCLUDE_LIBXTREEMFS_SYSTEM_USER_MAPPING_UNIX_H_
