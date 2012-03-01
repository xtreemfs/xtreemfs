/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/system_user_mapping.h"

#ifndef WIN32
#include "libxtreemfs/system_user_mapping_unix.h"
#else
#include "libxtreemfs/system_user_mapping_windows.h"
#endif // !WIN32
#include "libxtreemfs/user_mapping.h"

namespace xtreemfs {

SystemUserMapping* SystemUserMapping::GetSystemUserMapping() {
#ifdef WIN32
  return new SystemUserMappingWindows();
#else
  return new SystemUserMappingUnix();
#endif // WIN32
}

}  // namespace xtreemfs
