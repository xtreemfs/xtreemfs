/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/user_mapping.h"

#include "libxtreemfs/options.h"
#include "libxtreemfs/user_mapping_gridmap_globus.h"
#include "libxtreemfs/user_mapping_gridmap_unicore.h"

namespace xtreemfs {

UserMapping* UserMapping::CreateUserMapping(UserMappingType type) {
  Options options;
  return CreateUserMapping(type, options);
}

UserMapping* UserMapping::CreateUserMapping(UserMappingType type,
                                            const Options& options) {
  switch (type) {
    case kNone:
      return NULL;
    case kGlobus:
      return new UserMappingGridmapGlobus(
          options.grid_gridmap_location,
          options.grid_gridmap_reload_interval_m * 60);  // Min -> Seconds.
    case kUnicore:
      return new UserMappingGridmapUnicore(
          options.grid_gridmap_location,
          options.grid_gridmap_reload_interval_m * 60);  // Min -> Seconds.
    default:
      return NULL;
  }
}

}  // namespace xtreemfs
