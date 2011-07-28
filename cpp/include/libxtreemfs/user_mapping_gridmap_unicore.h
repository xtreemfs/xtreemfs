/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *                    2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_GRIDMAP_UNICORE_H_
#define CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_GRIDMAP_UNICORE_H_

#include <boost/bimap.hpp>
#include <fstream>
#include <string>
#include <map>

#include "libxtreemfs/user_mapping_gridmap.h"

namespace xtreemfs {

class UserMappingGridmapUnicore : public UserMappingGridmap {
 public:
  UserMappingGridmapUnicore(UserMappingType user_mapping_type_system,
                           const std::string& gridmap_file,
                           int gridmap_reload_interval_s);

 protected:
  virtual void ReadGridmapFile();

  /** Parses the unicore gridmap-file for version < 6. */
  void ReadGridmapFileUnicore(
      std::ifstream &in,
      boost::bimap< std::string, std::string > &new_username,
      std::multimap< std::string, std::string > &new_groupname);

  /** Parses the unicore gridmap-file for version 6. */
  void ReadGridmapFileUnicore6(
      std::ifstream &in,
      boost::bimap< std::string, std::string > &new_username,
      std::multimap< std::string, std::string > &new_groupname);
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_GRIDMAP_UNICORE_H_
