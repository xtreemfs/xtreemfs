/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *               2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef WIN32
#include "libxtreemfs/system_user_mapping_unix.h"

#include <grp.h>
#include <pwd.h>
#include <sys/types.h>

#include <boost/cstdint.hpp>
#include <boost/lexical_cast.hpp>
#include <fstream>
#include <iostream>

#include "util/logging.h"
#include "pbrpc/RPC.pb.h"

using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

void SystemUserMappingUnix::GetUserCredentialsForCurrentUser(
    xtreemfs::pbrpc::UserCredentials* user_credentials) {
  user_credentials->set_username(UIDToUsername(geteuid()));
  user_credentials->add_groups(GIDToGroupname(getegid()));
}

std::string SystemUserMappingUnix::UIDToUsername(uid_t uid) {
  if (uid == static_cast<uid_t>(-1)) {
    return string("-1");
  }

  string username;
  // Retrieve username.
  size_t bufsize = sysconf(_SC_GETPW_R_SIZE_MAX);
  if (bufsize == -1) {
    // Max size unknown, use safe value.
    bufsize = 16384;
  }
  char* buf = new char[bufsize];
  struct passwd pwd;
  struct passwd* result = NULL;
  int s = getpwuid_r(uid, &pwd, buf, bufsize, &result);
  if (result == NULL) {
    if (s == 0) {
      if (Logging::log->loggingActive(LEVEL_INFO)) {
        Logging::log->getLog(LEVEL_INFO)
                << "no mapping for uid " << uid << std::endl;
      }
    } else {
      Logging::log->getLog(LEVEL_ERROR)
              << "failed to retrieve passwd entry for uid: " << uid << endl;
    }
    // Return uid as name if no mapping found.
    try {
      username = boost::lexical_cast<string>(uid);
    } catch(const boost::bad_lexical_cast&) {
      Logging::log->getLog(LEVEL_ERROR)
                    << "failed to use uid for usermapping: " << uid << endl;
      username = "nobody";
    }
  } else {
    username = string(pwd.pw_name);
  }
  delete[] buf;

  if (additional_user_mapping_.get()) {
    string username_local(username);
    additional_user_mapping_->LocalToGlobalUsername(username_local, &username);
  }

  return username;
}

uid_t SystemUserMappingUnix::UsernameToUID(const std::string& username) {
  string local_username(username);
  if (additional_user_mapping_.get()) {
    additional_user_mapping_->GlobalToLocalUsername(username, &local_username);
  }

  uid_t uid = 65534;  // nobody.

  // Retrieve uid.
  size_t bufsize = sysconf(_SC_GETPW_R_SIZE_MAX);
  if (bufsize == -1) {
    // Max size unknown, use safe value.
    bufsize = 16384;
  }
  char* buf = new char[bufsize];
  struct passwd pwd;
  struct passwd* result = NULL;
  int s = getpwnam_r(local_username.c_str(), &pwd, buf, bufsize, &result);
  if (result == NULL) {
    if (s == 0) {
      if (Logging::log->loggingActive(LEVEL_INFO)) {
        Logging::log->getLog(LEVEL_INFO)
            << "no mapping for username: " << local_username << endl;
      }
    } else {
      Logging::log->getLog(LEVEL_ERROR)
          << "failed to retrieve passwd entry for username: "
          << local_username<< endl;
    }
    // Map reserved value -1 to nobody.
    if (local_username == "-1") {
      uid = 65534;  // nobody.
    } else {
      // Try to convert the username into an integer. (Needed if an integer was
      // stored in the first place because there was no username found for the
      // uid at the creation of the file.)
      try {
        uid = boost::lexical_cast<uid_t>(local_username);
      } catch(const boost::bad_lexical_cast&) {
        uid = 65534;  // nobody.
      }
      // boost::lexical_cast silently converts negative values into unsigned
      // integers. Check if username actually contains a negative value.
      if (uid != 65534) {
        try {
          // It's needed to use a 64 bit signed integer to detect a -(2^31)-1
          // as a negative value and not as an overflowed unsigned integer of
          // value 2^32-1.
          int64_t uid_signed = boost::lexical_cast<int64_t>(local_username);
          if (uid_signed < 0) {
            uid = 65534;  // nobody.
          }
        } catch(const boost::bad_lexical_cast&) {
          // Leave uid as it is if lexical_cast failed.
        }
      }
    }
  } else {
    uid = pwd.pw_uid;
  }
  delete[] buf;

  return uid;
}

std::string SystemUserMappingUnix::GIDToGroupname(gid_t gid) {
  if (gid == static_cast<gid_t>(-1)) {
    return string("-1");
  }


  string groupname;
  // Retrieve username.
  size_t bufsize = sysconf(_SC_GETGR_R_SIZE_MAX);
  if (bufsize == -1) {
    // Max size unknown, use safe value.
    bufsize = 16384;
  }
  char* buf = new char[bufsize];
  struct group grp;
  struct group* result = NULL;
  int s = getgrgid_r(gid, &grp, buf, bufsize, &result);
  if (result == NULL) {
    if (s == 0) {
      if (Logging::log->loggingActive(LEVEL_INFO)) {
        Logging::log->getLog(LEVEL_INFO)
                << "no mapping for gid " << gid << endl;
      }
    } else {
      Logging::log->getLog(LEVEL_ERROR)
              << "failed to retrieve group entry for gid: " << gid << endl;
    }
    // Return uid as name if no mapping found.
    try {
      groupname = boost::lexical_cast<string>(gid);
    } catch(const boost::bad_lexical_cast&) {
      Logging::log->getLog(LEVEL_ERROR)
                    << "failed to use gid for usermapping: " << gid << endl;
      groupname = "nobody";
    }
  } else {
    groupname = string(grp.gr_name);
  }
  delete[] buf;

  if (additional_user_mapping_.get()) {
    string local_groupname(groupname);
    additional_user_mapping_->LocalToGlobalGroupname(local_groupname,
                                                     &groupname);
  }

  return groupname;
}

gid_t SystemUserMappingUnix::GroupnameToGID(const std::string& groupname) {
  string local_groupname(groupname);
  if (additional_user_mapping_.get()) {
    additional_user_mapping_->GlobalToLocalGroupname(groupname,
                                                     &local_groupname);
  }

  gid_t gid = 65534;  // nobody.

  // Retrieve gid.
  size_t bufsize = sysconf(_SC_GETPW_R_SIZE_MAX);
  if (bufsize == -1) {
    // Max size unknown, use safe value.
    bufsize = 131072;
  }
  char* buf = new char[bufsize];
  struct group grp;
  struct group* result = NULL;
  int s = getgrnam_r(local_groupname.c_str(), &grp, buf, bufsize, &result);
  if (result == NULL) {
    if (s == 0) {
      if (Logging::log->loggingActive(LEVEL_INFO)) {
        Logging::log->getLog(LEVEL_INFO)
            << "no mapping for groupname: " << local_groupname << endl;
      }
    } else {
      Logging::log->getLog(LEVEL_ERROR)
          << "failed to retrieve passwd entry for groupname: "
          << local_groupname << " (getgrnam_r returned " << s << ")" << endl;
    }
    // Map reserved value -1 to nobody.
    if (local_groupname == "-1") {
      gid = 65534;  // nobody.
    } else {
      // Try to convert the groupname into an integer. (Needed if an integer was
      // stored in the first place because there was no groupname found for the
      // gid at the creation of the file.)
      try {
        gid = boost::lexical_cast<gid_t>(local_groupname);
      } catch(const boost::bad_lexical_cast&) {
        gid = 65534;  // nobody.
      }
      // boost::lexical_cast silently converts negative values into unsigned
      // integers. Check if groupname actually contains a negative value.
      if (gid != 65534) {
        try {
          // It's needed to use a 64 bit signed integer to detect a -(2^31)-1
          // as a negative value and not as an overflowed unsigned integer of
          // value 2^32-1.
          int64_t gid_signed = boost::lexical_cast<int64_t>(local_groupname);
          if (gid_signed < 0) {
            gid = 65534;  // nobody.
          }
        } catch(const boost::bad_lexical_cast&) {
          // Leave gid as it is if lexical_cast failed.
        }
      }
    }
  } else {
    gid = grp.gr_gid;
  }
  delete[] buf;

  return gid;
}

void SystemUserMappingUnix::GetGroupnames(uid_t uid,
                                          gid_t gid,
                                          pid_t pid,
                                          std::list<std::string>* groupnames) {
  groupnames->push_back(GIDToGroupname(gid));

#ifdef __linux__
  // Parse /proc/<pid>/task/<pid>/status like fuse_req_getgroups.
  string filename = "/proc/" + boost::lexical_cast<string>(pid) + "/task/"
  + boost::lexical_cast<string>(pid) + "/status";
  ifstream in(filename.c_str());
  string line;
  // C++ getline() does check for failbit or badbit of the istream. If of these
  // bits are set, it does break from the while loop, for instance if the file
  // does not exist. In this case no additional groups are added.
  while (getline(in, line)) {
    if (line.length() >= 8 && line.substr(0, 8) == "Groups:\t") {
      // "Groups: " entry found, read all groups
      std::stringstream stringstream(line.substr(8, line.length() - 8 - 1));
      std::string group_id;
      while (getline(stringstream, group_id, ' ')) {
        gid_t supplementary_gid = boost::lexical_cast<gid_t>(group_id);
        if (supplementary_gid != gid) {
          groupnames->push_back(GIDToGroupname(supplementary_gid));
        }
      }
      break;
    }
  }
#endif
}

}  // namespace xtreemfs
#endif // !WIN32
