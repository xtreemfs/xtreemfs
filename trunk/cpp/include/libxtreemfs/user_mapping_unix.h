/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_UNIX_H_
#define CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_UNIX_H_

#include <list>
#include <string>

#include "libxtreemfs/user_mapping.h"

namespace xtreemfs {

class UserMappingUnix : public UserMapping {
 public:
  UserMappingUnix() {}

  /** Left unimplemented. */
  virtual void Start() {}

  /** Left unimplemented. */
  virtual void Stop() {}

  virtual std::string UIDToUsername(uid_t uid);

  virtual uid_t       UsernameToUID(const std::string& username);

  virtual std::string GIDToGroupname(gid_t gid);

  virtual gid_t       GroupnameToGID(const std::string& groupname);

  virtual void        GetGroupnames(uid_t uid,
                                    gid_t gid,
                                    pid_t pid,
                                    std::list<std::string>* groupnames);
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_UNIX_H_
