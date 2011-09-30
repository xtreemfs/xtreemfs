/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/user_mapping.h"

#include "libxtreemfs/options.h"
#include "libxtreemfs/user_mapping_unix.h"
#include "libxtreemfs/user_mapping_gridmap_globus.h"
#include "libxtreemfs/user_mapping_gridmap_unicore.h"

namespace xtreemfs {

UserMapping* UserMapping::CreateUserMapping(UserMappingType type) {
  Options options;
  return CreateUserMapping(type, type, options);
}

UserMapping* UserMapping::CreateUserMapping(UserMappingType type,
                                            UserMappingType system_type,
                                            const Options& options) {
  switch (type) {
    case kUnix:
      return new UserMappingUnix();
    case kGlobus:
      return new UserMappingGridmapGlobus(
          system_type,
          options.grid_gridmap_location,
          options.grid_gridmap_reload_interval_m * 60);  // Min -> Seconds.
    case kUnicore:
      return new UserMappingGridmapUnicore(
          system_type,
          options.grid_gridmap_location,
          options.grid_gridmap_reload_interval_m * 60);  // Min -> Seconds.
    default:
      return NULL;
  }
}

UserMapping::UserMappingType UserMapping::GetUserMappingSystemType() {
  // TODO(mberlin):: Add switches for other plattforms.
  return kUnix;
}

}  // namespace xtreemfs
