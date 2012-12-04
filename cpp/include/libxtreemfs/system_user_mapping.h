/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#ifndef CPP_INCLUDE_LIBXTREEMFS_SYSTEM_USER_MAPPING_H_
#define CPP_INCLUDE_LIBXTREEMFS_SYSTEM_USER_MAPPING_H_

#include <boost/scoped_ptr.hpp>

#include "libxtreemfs/user_mapping.h"

namespace xtreemfs {
namespace pbrpc {
class UserCredentials;
}  // namespace pbrpc

/**
 * Allows to retrieve the UserCredentials for the current system user.
 * Additionally, a UserMapping can be registered to transform local usernames
 * and groupnames e.g., convert a local username into a global name.
 */
class SystemUserMapping {
 public:
  /** Returns a SystemSystemUserMapping for the current plattform.
   *
   * Currently only Windows and Unix (suitable for Linux, MacOSX, Solaris)
   * SystemUserMapping implementations are available.
   *
   * @remark Ownership is transferred to the caller.
   */
  static SystemUserMapping* GetSystemUserMapping();

  virtual ~SystemUserMapping() {}

  /** Fills "user_credentials" with the user and group names of the current
   *  system user. */
  virtual void GetUserCredentialsForCurrentUser(
      xtreemfs::pbrpc::UserCredentials* user_credentials) = 0;

  /** Register an additional user mapping to transform user and group names
   *  before returning them to the system or XtreemFS.
   *
   * @attention The implementation of this function is not required to be
   *            thread-safe i.e., register an additional user mapping before
   *            making the system user mapping available to multiple threads.
   *
   * @remark Ownership is transferred to the caller.
   */
  void RegisterAdditionalUserMapping(UserMapping* mapping);

  /** Executes the Start() method of the registered additional user mapping. */
  void StartAdditionalUserMapping();

  /** Executes the Stop() method of the registered additional user mapping. */
  void StopAdditionalUserMapping();

 protected:
  /** Used for custom transformation between local and global names. */
  boost::scoped_ptr<UserMapping> additional_user_mapping_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_SYSTEM_USER_MAPPING_H_
