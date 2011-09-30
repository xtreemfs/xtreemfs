/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *                    2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#ifndef CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_H_
#define CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_H_

#include <list>
#include <string>

namespace xtreemfs {

class Options;

class UserMapping {
 public:
  /** Available UserMappings which are allowed by CreateUserMapping(). */
  enum UserMappingType {
    kUnix, kUnicore, kGlobus
  };

  /** Returns an instance of the chosen UserMapping.
   *
   * @param type        Type of the user mapping to be created.
   * @param system_type Type of user mapping suitable for the current system
   *                    (e.g. Unix) which may be needed as base for advanced
   *                    UserMappings like Unicore or Globus.
   * @param options     Options object which has to contain additional members
   *                    depending on the constructor of the user mapping.
   */
  static UserMapping* CreateUserMapping(UserMappingType type,
                                        UserMappingType system_type,
                                        const Options& options);

  /** Returns CreateUserMapping(type, type, options) whereas options is a
   *  default Options object. */
  static UserMapping* CreateUserMapping(UserMappingType type);

  /** Tries to detect the current system and returns the corresponding type for
   *  the UserMapping. */
  static UserMappingType GetUserMappingSystemType();

  virtual ~UserMapping() {}

  /** Has to be called after creating a UserMapping object and can be used to
   *  start needed thread. */
  virtual void Start() = 0;
  /** Has to be called before the deletion of a UserMapping object and can be
   *  used to stop created threads. */
  virtual void Stop() = 0;

  virtual std::string UIDToUsername(uid_t uid) = 0;

  virtual uid_t       UsernameToUID(const std::string& username) = 0;

  virtual std::string GIDToGroupname(gid_t gid) = 0;

  virtual gid_t       GroupnameToGID(const std::string& groupname) = 0;

  /** Fills groups with the groupnames the given id belongs to. The groupname
   *  belonging to "gid" should always be the first in the list.
   *
   * The implementation can choose the process or user id to retrieve the group
   * names. */
  virtual void        GetGroupnames(uid_t uid,
                                    gid_t gid,
                                    pid_t pid,
                                    std::list<std::string>* groupnames) = 0;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_H_
