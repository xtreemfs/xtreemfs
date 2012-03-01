/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_GRIDMAP_GLOBUS_H_
#define CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_GRIDMAP_GLOBUS_H_

#include <string>

#include "libxtreemfs/user_mapping_gridmap.h"

namespace xtreemfs {

class UserMappingGridmapGlobus : public UserMappingGridmap {
 public:
  UserMappingGridmapGlobus(const std::string& gridmap_file,
                           int gridmap_reload_interval_s);

 protected:
  virtual void ReadGridmapFile();
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_USER_MAPPING_GRIDMAP_GLOBUS_H_
