/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *                    2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/user_mapping_gridmap_unicore.h"

#include <boost/tokenizer.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <vector>

#include "util/logging.h"
#include "libxtreemfs/xtreemfs_exception.h"

using namespace boost;
using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

UserMappingGridmapUnicore::UserMappingGridmapUnicore(
    UserMappingType user_mapping_type_system,
    const std::string& gridmap_file,
    int gridmap_reload_interval_s)
    : UserMappingGridmap(user_mapping_type_system,
                         gridmap_file,
                         gridmap_reload_interval_s) {}

/**
 * uudb-format (Unicore < 6)
 *  dgls0050:dgms0006=CN=Patrick Schaefer,OU=CSR,O=GridGermany,C=DE
 *
 * xuudb-format (Unicore 6)
 *  225;zib;dgms0006:dgls0050;user;mosgrid:lifescience;
 *  CN=Patrick Schaefer,OU=CSR,O=GridGermany,C=DE
 *
 */
void UserMappingGridmapUnicore::ReadGridmapFile() {
  boost::bimap< std::string, std::string > new_username;
  std::multimap< std::string, std::string > new_groupname;

  ifstream in(gridmap_file().c_str());
  if (!in.is_open()) {
    throw XtreemFSException("gridmap: could not open gridmap-file: "
        + gridmap_file());
  }

  if (Logging::log->loggingActive(LEVEL_INFO)) {
    Logging::log->getLog(LEVEL_INFO)
        << "gridmap: loading users and groups from file: "
        << gridmap_file() << endl;
  }

  std::string line;
  getline(in, line);  // read first line to determine format
  in.clear();  // reset ifstream
  in.seekg(0);
  if (std::count(line.begin(), line.end(), ';')>3) {
    // unicore 6
    ReadGridmapFileUnicore6(in, new_username, new_groupname);
  }
  else {
    // unicore <6
    ReadGridmapFileUnicore(in, new_username, new_groupname);
  }


  // update changes
  boost::mutex::scoped_lock lock(mutex);
  dn_username.clear();
  dn_groupname.clear();

  dn_username.insert(new_username.begin(), new_username.end());
  dn_groupname.insert(new_groupname.begin(), new_groupname.end());
}

/**
 * uudb-format (Unicore < 6)
 *  dgls0050:dgms0006=CN=Patrick Schaefer,OU=CSR,O=GridGermany,C=DE
 */
void UserMappingGridmapUnicore::ReadGridmapFileUnicore(
    std::ifstream &in,
    boost::bimap< std::string, std::string > &new_username,
    std::multimap< std::string, std::string > &new_groupname) {
  std::vector< std::string > vec;
  std::string line;

  // read lines
  while(getline(in, line)) {
    vec.clear();

    // split string at first '='
    size_t end_of_users_pos = line.find("=");
    vec.push_back(line.substr(0, end_of_users_pos));
    vec.push_back(line.substr(end_of_users_pos+1,
        line.size()-end_of_users_pos-1));

    // are there two entries available?
    if (vec.size() < 2) {
      Logging::log->getLog(LEVEL_ERROR)
          << "gridmap: could not parse line: " << line << std::endl;
      continue;
    }

    trim(vec[1]);  // dn
    trim(vec[0]);  // usernames

    // store the dn, groups and username
    Store(std::string(vec[1]), std::string(vec[0]), ":",
        new_username, new_groupname);
  }
}

/**
 * xuudb-format (Unicore 6)
 *  225;zib;dgms0006:dgls0050;user;mosgrid:lifescience;CN=Patrick Schaefer,OU=CSR,O=GridGermany,C=DE
 */
void UserMappingGridmapUnicore::ReadGridmapFileUnicore6(
    std::ifstream &in,
    boost::bimap< std::string, std::string > &new_username,
    std::multimap< std::string, std::string > &new_groupname) {
  std::vector< std::string > vec;
  std::string line;

  string separator1("");   // no escaping
  string separator2(";");  // split dn on ;
  string separator3("");   // the dn is not enclosed
  escaped_list_separator<char> els(separator1, separator2, separator3);

  // read lines
  while(getline(in, line)) {
    tokenizer< escaped_list_separator<char> > tok(line, els);
    vec.clear();
    vec.assign(tok.begin(), tok.end());

    // are there two entries available?
    if (vec.size() < 6) {
      Logging::log->getLog(LEVEL_ERROR)
          << "gridmap: could not parse line: " << line << std::endl;
      continue;
    }

    trim(vec[5]);  // dn
    trim(vec[2]);  // username

    // store the dn, groups and username
    Store(std::string(vec[5]), std::string(vec[2]), ":",
        new_username, new_groupname);
  }
}

}  // namespace xtreemfs
