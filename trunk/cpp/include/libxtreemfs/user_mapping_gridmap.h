/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_GRIDMAP_H_
#define CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_GRIDMAP_H_

#include <boost/bimap.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include <list>
#include <map>
#include <string>

#include "libxtreemfs/user_mapping.h"

namespace xtreemfs {

class UserMappingGridmap : public UserMapping {
 public:
  UserMappingGridmap(UserMappingType user_mapping_type_system,
                     const std::string& gridmap_file,
                     int gridmap_reload_interval_s);

  virtual void Start();

  virtual void Stop();

  virtual std::string UIDToUsername(uid_t uid);

  virtual uid_t       UsernameToUID(const std::string& username);

  virtual std::string GIDToGroupname(gid_t gid);

  virtual gid_t       GroupnameToGID(const std::string& groupname);

  virtual void        GetGroupnames(uid_t uid,
                                    gid_t gid,
                                    pid_t pid,
                                    std::list<std::string>* groupnames);

 protected:
  /** Parses the file gridmap_file_ and stores the information in the maps
   *  dn_username_ and dn_groupname_. */
  virtual void ReadGridmapFile() = 0;

  /** Stores dn, user and extracts the groups (all entries starting with OU=)
   *  from the dn-string.
   *
   *  This is a helper function provided for ReadGridMapFile implementations. */
  void Store(std::string dn,
             std::string users,
             std::string user_seperator,
             boost::bimap< std::string, std::string > &new_username,
             std::multimap< std::string, std::string > &new_groupname);

  /** Accessor for derived classes. */
  inline std::string gridmap_file() {
    return gridmap_file_;
  }

  /** Contains the mappings DN <-> Username. */
  boost::bimap< std::string, std::string > dn_username;

  /** Contains the mappings DN <-> OU whereas multiple DN entries may exist. */
  std::multimap< std::string, std::string > dn_groupname;

  /** Use this when accessing the maps dn_username_ or dn_groupname_. */
  boost::mutex mutex;

 private:
  /** Executed in an extra thread at intervals of the length
   *  gridmap_reload_interval_s_. The thread has to be started and stopped with
   *  Start() and Stop() after the creation and before the deletion of this
   *  object.
   *
   *  The thread checks the modification time of the gridmap file to decide if
   *  the file shall be reloaded.
   */
  void PeriodicGridmapFileReload();

  /** Looks up a username in the mapfile and returns the DN if found, else "".*/
  std::string UsernameToDN(std::string username);

  /** Looks up a DN in the mapfile and returns the username if found, else "".*/
  std::string DNToUsername(std::string dn);

  /** Fills ous with the OUs of the DN. */
  void DNToOUs(std::string dn, std::list<std::string>* ous);

  /** Path to grid map file which will be periodically re-read. */
  std::string gridmap_file_;

  /** Thread which reloads the gridmap file. */
  boost::scoped_ptr<boost::thread> monitor_thread_;

  /** Interval at which the gridmap file will be periodically reread.
   *
   * @remarks   Start() and Stop() have to be executed to start and stop the
   *            thread responsible for the periodic reread. */
  int gridmap_reload_interval_s_;

  /** Required base usermapping to retrieve IDs or names from the system. */
  boost::scoped_ptr<UserMapping> system_user_mapping_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_GRIDMAP_H_
