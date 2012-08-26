/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *               2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#ifndef CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_H_
#define CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_H_

#include <sys/types.h>

#include <list>
#include <string>

namespace xtreemfs {
namespace pbrpc {
class UserCredentials;
}  // namespace pbrpc

class Options;

/** Allows to specify a transformation between local user and group names and
 *  its global counterparts stored in XtreemFS. */
class UserMapping {
 public:
  /** Available UserMappings which are allowed by CreateUserMapping(). */
  enum UserMappingType {
    kNone, kUnicore, kGlobus
  };

  /** Returns an instance of the chosen UserMapping.
   *
   * @param type        Type of the user mapping to be created.
   * @param options     Options object which has to contain additional members
   *                    depending on the constructor of the user mapping.
   */
  static UserMapping* CreateUserMapping(UserMappingType type,
                                        const Options& options);

  /** Returns CreateUserMapping(type, options) whereas options is a
   *  default Options object. */
  static UserMapping* CreateUserMapping(UserMappingType type);

  virtual ~UserMapping() {}

  /** Has to be called after creating a UserMapping object and can be used to
   *  start a needed thread. */
  virtual void Start() = 0;
  /** Has to be called before the deletion of a UserMapping object and can be
   *  used to stop a created threads. */
  virtual void Stop() = 0;

  virtual void LocalToGlobalUsername(const std::string& username_local,
                                     std::string* username_global) = 0;

  virtual void LocalToGlobalGroupname(const std::string& groupname_local,
                                      std::string* groupname_global) = 0;

  virtual void GlobalToLocalUsername(const std::string& username_global,
                                     std::string* username_local) = 0;

  virtual void GlobalToLocalGroupname(const std::string& groupname_global,
                                      std::string* groupname_local) = 0;

  /** Retrieve the list of known group names based on the local username. */
  virtual void GetGroupnames(
      const std::string& username_local,
      xtreemfs::pbrpc::UserCredentials* user_credentials) = 0;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_H_
