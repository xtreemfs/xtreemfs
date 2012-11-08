/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#ifndef CPP_INCLUDE_LIBXTREEMFS_SYSTEM_USER_MAPPING_WINDOWS_H_
#define CPP_INCLUDE_LIBXTREEMFS_SYSTEM_USER_MAPPING_WINDOWS_H_

#include <boost/scoped_ptr.hpp>

#include "libxtreemfs/system_user_mapping.h"

namespace xtreemfs {

class UserMapping;

/** Implements SystemUserMapping and returns UserCredentials for the current
 *  user.
 */
class SystemUserMappingWindows : public SystemUserMapping {
 public:
   SystemUserMappingWindows() {}

  /** Fills "user_credentials" with the user and group names of the current
   *  system user. */
  virtual void GetUserCredentialsForCurrentUser(
      xtreemfs::pbrpc::UserCredentials* user_credentials);

  virtual void RegisterAdditionalUserMapping(UserMapping* mapping);

  virtual void StartAdditionalUserMapping();

  virtual void StopAdditionalUserMapping();

 private:
  /** Used for custom transformation between local and global names. */
  boost::scoped_ptr<UserMapping> additional_user_mapping_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_SYSTEM_USER_MAPPING_WINDOWS_H_