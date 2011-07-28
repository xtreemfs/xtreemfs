/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *                    2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/user_mapping_gridmap.h"

#include <boost/algorithm/string/trim.hpp>
#include <boost/tokenizer.hpp>
#include <iostream>
#include <sys/stat.h>
#include <sys/types.h>

#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"

using namespace boost;
using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

UserMappingGridmap::UserMappingGridmap(UserMappingType user_mapping_type_system,
                                       const std::string& gridmap_file,
                                       int gridmap_reload_interval_m)
    : gridmap_file_(gridmap_file),
      gridmap_reload_interval_s_(gridmap_reload_interval_m) {
  // Initialize required base usermapping to retrieve IDs or names
  // from the system.
  system_user_mapping_.reset(
      UserMapping::CreateUserMapping(user_mapping_type_system));
}

std::string UserMappingGridmap::UIDToUsername(uid_t uid) {
  // map uid to username
  std::string username = system_user_mapping_->UIDToUsername(uid);

  // map username to dn using the gridmap-file
  std::string dn = UsernameToDN(username);

  if (dn.empty()) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "gridmap: no mapping for username " << username << std::endl;
    }
    return username;
  }

  return dn;
}

std::string UserMappingGridmap::UsernameToDN(std::string username) {
  boost::mutex::scoped_lock lock(mutex);
  boost::bimap< std::string, std::string >::right_const_iterator iter
    = dn_username.right.find(username);
  if (iter != dn_username.right.end()) {
    return iter->second;
  }
  return "";
}

uid_t UserMappingGridmap::UsernameToUID(const std::string& username) {
  // The username is actually a DN.
  const string& dn = username;

  // map dn to username using the gridmap-file
  std::string grid_username = DNToUsername(dn);

  if (grid_username.empty()) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "gridmap: no mapping for dn " << dn << std::endl;
    }
    grid_username = dn;
  }

  // map username to id
  uid_t uid = system_user_mapping_->UsernameToUID(grid_username);

  return uid;
}

std::string UserMappingGridmap::DNToUsername(std::string dn) {
  boost::mutex::scoped_lock lock(mutex);
  boost::bimap< std::string, std::string >::left_const_iterator iter
    = dn_username.left.find(dn);
  if (iter != dn_username.left.end()) {
    return iter->second;
  }
  return "";
}

/** It is currently not possible to map the OU-entries to local groups. */
std::string UserMappingGridmap::GIDToGroupname(gid_t gid) {
  return "root";
}

/** It is currently not possible to map the OU-entries to local groups. */
gid_t UserMappingGridmap::GroupnameToGID(const std::string& groupname) {
  return 0;
}

void UserMappingGridmap::GetGroupnames(uid_t uid,
                                      gid_t gid,
                                      pid_t pid,
                                      std::list<std::string>* groupnames) {
  // obtain dn of current process
  std::string dn = UIDToUsername(uid);

  // map username to ou using gridmap-file
  DNToOUs(dn, groupnames);
}

void UserMappingGridmap::DNToOUs(std::string dn, std::list<std::string>* ous) {
  // find groups for current user

  boost::mutex::scoped_lock lock(mutex);
  multimap<string, string>::iterator iter;
  pair<
    multimap<string, string>::iterator,
    multimap<string, string>::iterator > range
      = dn_groupname.equal_range(dn);

  for (iter = range.first;  iter != range.second;  ++iter) {
//        Logging::log->getLog(LEVEL_DEBUG)
//            << "group: " << iter->second << std::endl;
    ous->push_back(iter->second);
  }
}

void UserMappingGridmap::Start() {
  // read the grid-map-file once
  ReadGridmapFile();

  // monitor changes to the gridmap-file
  monitor_thread_.reset(new boost::thread(
      boost::bind(&UserMappingGridmap::PeriodicGridmapFileReload, this)));
}

void UserMappingGridmap::Stop() {
  monitor_thread_->interrupt();
  monitor_thread_->join();
}

void UserMappingGridmap::PeriodicGridmapFileReload() {
  struct stat st;

  int ierr = stat(gridmap_file_.c_str(), &st);
  if (ierr != 0) {
    throw XtreemFSException("Failed to open gridmap file: " + gridmap_file_);
  }
  int date = st.st_mtime;

  // monitor changes to the gridmap file
  while (true) {
    // send thread to sleep for user_group_monitor_m minutes
    boost::posix_time::seconds workTime(gridmap_reload_interval_s_);
    boost::this_thread::sleep(workTime);

    ierr = stat(gridmap_file_.c_str(), &st);
    if (st.st_mtime != date) {
      if (Logging::log->loggingActive(LEVEL_INFO)) {
        Logging::log->getLog(LEVEL_INFO)
            << "file changed. updating all entries." << endl;
      }
      ReadGridmapFile();
      date = st.st_mtime;
    }
  }
}

void UserMappingGridmap::Store(
    std::string dn,
    std::string users,
    std::string user_seperator,
    boost::bimap< std::string, std::string > &new_username,
    std::multimap< std::string, std::string > &new_groupname) {
  // if there are several usernames, use only the first one
  escaped_list_separator<char> els2("", user_seperator.c_str(), "");
  tokenizer< escaped_list_separator<char> > tok_user(users, els2);
  tokenizer< escaped_list_separator<char> >::iterator first_username
    = tok_user.begin();
  std::string user = std::string(*first_username);

//      cout << "gridmap: dn: '" << dn << "'" << std::endl;
//      cout << "gridmap: user: " << user << std::endl;

  new_username.insert(bimap< string, string >::value_type(dn, user));

  // find groups (starting with OU=)
  size_t ou_pos = dn.find("OU=", 0);
  while (ou_pos != string::npos) {
    size_t end_pos = dn.find(",", ou_pos+1);
    std::string ou = dn.substr(ou_pos+3, end_pos-ou_pos-3);

//        cout << "gridmap: group: " << ou << std::endl;

    // add one usergroup (OU=...)
    new_groupname.insert(std::pair<std::string, std::string>(dn, ou));

    // search in the remaining string
    ou_pos = dn.find("OU=", ou_pos+1);
  }
}

}  // namespace xtreemfs
