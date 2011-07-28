/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *                    2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/user_mapping_gridmap_globus.h"

#include <boost/bimap.hpp>
#include <boost/tokenizer.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <iostream>
#include <fstream>
#include <map>
#include <vector>

#include "util/logging.h"
#include "libxtreemfs/xtreemfs_exception.h"

using namespace boost;
using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

UserMappingGridmapGlobus::UserMappingGridmapGlobus(
    UserMappingType user_mapping_type_system,
    const std::string& gridmap_file,
    int gridmap_reload_interval_s)
    : UserMappingGridmap(user_mapping_type_system,
                         gridmap_file,
                         gridmap_reload_interval_s) {}

void UserMappingGridmapGlobus::ReadGridmapFile() {
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

  std::vector< std::string > vec;
  std::string line;

  string separator1("");   // no escaping
  string separator2(" ");  // split dn on spaces
  string separator3("\""); // the dn is enclosed by "dn"
  escaped_list_separator<char> els(separator1, separator2, separator3);

  // seperator for the dn-string
  escaped_list_separator<char> els_dn("", "/", "");

  // read lines
  while (getline(in, line)) {
    tokenizer< escaped_list_separator<char> > tok(line, els);
    vec.clear();
    vec.assign(tok.begin(), tok.end());

    // are there two entries available?
    if (vec.size() < 2) {
      Logging::log->getLog(LEVEL_ERROR)
          << "gridmap: could not parse line: " << line << std::endl;
      continue;
    }

    boost::trim(vec[0]);  // dn
    boost::trim(vec[1]);  // username

    // reformat globus-dn to unicore-dn
    // there is no reverse iterator, so we reverse the characters instead.
    std::reverse(vec[0].begin(), vec[0].end());
    tokenizer< escaped_list_separator<char> > tok_dn(vec[0], els_dn);
    std::stringstream dn_stream;

    // reverse order and separate elements using ","
    for (tokenizer<escaped_list_separator<char> >::iterator beg
          = tok_dn.begin(); beg != tok_dn.end(); ++beg) {
      std::string word = *beg;
      std::reverse(word.begin(), word.end());
      dn_stream << "," << word;
    }

    // store the dn, groups and username
    std::string dn_parsed = dn_stream.str();
    Store(dn_parsed.substr(1, dn_parsed.length()-2),
          std::string(vec[1]), ",", new_username, new_groupname);
  }

  // update changes
  boost::mutex::scoped_lock lock(mutex);
  dn_username.clear();
  dn_groupname.clear();

  dn_username.insert(new_username.begin(), new_username.end());
  dn_groupname.insert(new_groupname.begin(), new_groupname.end());
}

}  // namespace xtreemfs
