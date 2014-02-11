/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *               2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/user_mapping_gridmap.h"

#include <boost/algorithm/string/trim.hpp>
#include <boost/tokenizer.hpp>
#include <iostream>
#include <sys/stat.h>

#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"

using namespace boost;
using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {
UserMappingGridmap::UserMappingGridmap(const std::string& gridmap_file,
                                       int gridmap_reload_interval_m)
    : gridmap_file_(gridmap_file),
      gridmap_reload_interval_s_(gridmap_reload_interval_m),
      date_(0),
      size_(0) {}

void UserMappingGridmap::LocalToGlobalUsername(
    const std::string& username_local,
    std::string* username_global) {
  // map username to dn using the gridmap-file
  *username_global = UsernameToDN(username_local);

  if (username_global->empty()) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "gridmap: no mapping for username " << username_local << endl;
    }
    *username_global = username_local;
  }
}

std::string UserMappingGridmap::UsernameToDN(const std::string& username) {
  boost::mutex::scoped_lock lock(mutex);
  boost::bimap< std::string, std::string >::right_const_iterator iter
    = dn_username.right.find(username);
  if (iter != dn_username.right.end()) {
    return iter->second;
  }
  return "";
}

void UserMappingGridmap::GlobalToLocalUsername(
    const std::string& username_global,
    std::string* username_local) {
  // The username is actually a DN.
  const string& dn = username_global;

  // map dn to username using the gridmap-file
  *username_local = DNToUsername(dn);

  if (username_local->empty()) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "gridmap: no mapping for dn " << dn << std::endl;
    }
    *username_local = dn;
  }
}

std::string UserMappingGridmap::DNToUsername(const std::string& dn) {
  boost::mutex::scoped_lock lock(mutex);
  boost::bimap< std::string, std::string >::left_const_iterator iter
    = dn_username.left.find(dn);
  if (iter != dn_username.left.end()) {
    return iter->second;
  }
  return "";
}

/** It is currently not possible to map between OU-entries and local groups. */
void UserMappingGridmap::LocalToGlobalGroupname(
    const std::string& groupname_local,
    std::string* groupname_global) {
  *groupname_global = "root";
}

/** It is currently not possible to map between OU-entries and local groups. */
void UserMappingGridmap::GlobalToLocalGroupname(
    const std::string& groupname_global,
    std::string* groupname_local) {
  *groupname_local = "root";
}

void UserMappingGridmap::GetGroupnames(
    const std::string& username_local,
    xtreemfs::pbrpc::UserCredentials* user_credentials) {
  // obtain dn of current process
  string dn;
  LocalToGlobalUsername(username_local, &dn);

  // map username to ou using gridmap-file
  DNToOUs(dn, user_credentials);
}

void UserMappingGridmap::DNToOUs(
    const std::string& dn,
    xtreemfs::pbrpc::UserCredentials* user_credentials) {
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
    user_credentials->add_groups(iter->second);
  }
}

void UserMappingGridmap::Start() {
  struct stat st;

  if (stat(gridmap_file_.c_str(), &st) != 0) {
    throw XtreemFSException("Failed to open gridmap file: " + gridmap_file_);
  }

  // read the grid-map-file once
  ReadGridmapFile();
  date_ = st.st_mtime;
  size_ = st.st_size;

  // monitor changes to the gridmap-file
  monitor_thread_.reset(new boost::thread(
      boost::bind(&UserMappingGridmap::PeriodicGridmapFileReload, this)));
}

void UserMappingGridmap::Stop() {
  if (monitor_thread_) {
    monitor_thread_->interrupt();
    monitor_thread_->join();
  }
}

void UserMappingGridmap::PeriodicGridmapFileReload() {
  struct stat st;
  // Monitor changes to the gridmap file.
  while (true) {
    boost::posix_time::seconds sleep_time(gridmap_reload_interval_s_);
    boost::this_thread::sleep(sleep_time);

    int ierr = stat(gridmap_file_.c_str(), &st);
    if (ierr) {
      if (Logging::log->loggingActive(LEVEL_WARN)) {
        Logging::log->getLog(LEVEL_WARN)
            << "Failed to check if the gridmap file has changed."
               " Is it temporarily not available? Path to file: "
            << gridmap_file_ << " Error: " << ierr << endl;
      }
      continue;
    }

    if (st.st_mtime != date_ || st.st_size != size_) {
      if (Logging::log->loggingActive(LEVEL_INFO)) {
        Logging::log->getLog(LEVEL_INFO)
            << "File changed. Updating all entries." << endl;
      }
      ReadGridmapFile();
      date_ = st.st_mtime;
      size_ = st.st_size;
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
