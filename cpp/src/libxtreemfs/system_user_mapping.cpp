/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/system_user_mapping.h"

#ifdef WIN32
#include "libxtreemfs/system_user_mapping_windows.h"
#else
#include "libxtreemfs/system_user_mapping_unix.h"
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

void SystemUserMapping::RegisterAdditionalUserMapping(UserMapping* mapping) {
  additional_user_mapping_.reset(mapping);
}

void SystemUserMapping::StartAdditionalUserMapping() {
  if (additional_user_mapping_.get()) {
    additional_user_mapping_->Start();
  }
}

void SystemUserMapping::StopAdditionalUserMapping() {
  if (additional_user_mapping_.get()) {
    additional_user_mapping_->Stop();
  }
}

}  // namespace xtreemfs
