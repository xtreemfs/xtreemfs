/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#ifndef CPP_INCLUDE_LIBXTREEMFS_SYSTEM_USER_MAPPING_WINDOWS_H_
#define CPP_INCLUDE_LIBXTREEMFS_SYSTEM_USER_MAPPING_WINDOWS_H_

#include "libxtreemfs/system_user_mapping.h"

namespace xtreemfs {

class SystemUserMappingWindows : public SystemUserMapping {
 public:
  SystemUserMappingWindows() {}

  virtual void GetUserCredentialsForCurrentUser(
      xtreemfs::pbrpc::UserCredentials* user_credentials);
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_SYSTEM_USER_MAPPING_WINDOWS_H_
