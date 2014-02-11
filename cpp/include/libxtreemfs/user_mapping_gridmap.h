/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_GRIDMAP_H_
#define CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_GRIDMAP_H_

#include <sys/types.h>

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
  UserMappingGridmap(const std::string& gridmap_file,
                     int gridmap_reload_interval_s);

  virtual void Start();

  virtual void Stop();

  virtual void LocalToGlobalUsername(const std::string& username_local,
                                     std::string* username_global);

  virtual void LocalToGlobalGroupname(const std::string& groupname_local,
                                      std::string* groupname_global);

  virtual void GlobalToLocalUsername(const std::string& username_global,
                                     std::string* username_local);

  virtual void GlobalToLocalGroupname(const std::string& groupname_local,
                                      std::string* groupname_global);

  virtual void GetGroupnames(
      const std::string& username_local,
      xtreemfs::pbrpc::UserCredentials* user_credentials);

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
  std::string UsernameToDN(const std::string& username);

  /** Looks up a DN in the mapfile and returns the username if found, else "".*/
  std::string DNToUsername(const std::string& dn);

  /** Adds OUs as groups from the DN. */
  void DNToOUs(const std::string& dn,
               xtreemfs::pbrpc::UserCredentials* user_credentials);

  /** Path to grid map file which will be periodically re-read. */
  std::string gridmap_file_;

  /** Thread which reloads the gridmap file. */
  boost::scoped_ptr<boost::thread> monitor_thread_;

  /** Interval at which the gridmap file will be periodically reread.
   *
   * @remarks   Start() and Stop() have to be executed to start and stop the
   *            thread responsible for the periodic reread. */
  int gridmap_reload_interval_s_;

  /** Last time the gridmap file was modified. If changed, fill be reloaded. */
  time_t date_;
  /** Last known size of gridmap file. If changed, fill be reloaded. */
  off_t size_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_GRIDMAP_H_
