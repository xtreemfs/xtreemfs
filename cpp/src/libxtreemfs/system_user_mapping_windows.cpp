/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *               2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/system_user_mapping_windows.h"

#ifdef WIN32
#pragma comment(lib, "Netapi32.lib")
#include <windows.h>
#include <lm.h>

#include "libxtreemfs/helper.h"
#include "libxtreemfs/user_mapping.h"
#include "pbrpc/RPC.pb.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

void SystemUserMappingWindows::GetUserCredentialsForCurrentUser(
    xtreemfs::pbrpc::UserCredentials* user_credentials) {
  LPWKSTA_USER_INFO_1 user_info = NULL;
  NET_API_STATUS result = NetWkstaUserGetInfo(
      NULL,
      1,
      reinterpret_cast<LPBYTE*>(&user_info));
  if (result == NERR_Success) {
    if (user_info != NULL) {
      ConvertWindowsToUTF8(user_info->wkui1_username,
                           user_credentials->mutable_username());
      ConvertWindowsToUTF8(user_info->wkui1_logon_domain,
                           user_credentials->add_groups());

      NetApiBufferFree(user_info);
    }
  } else {
     Logging::log->getLog(LEVEL_ERROR) <<
       "Failed to retrieve the current username and domain name, error"
       " code: " << result << endl;
  }
}

void SystemUserMappingWindows::RegisterAdditionalUserMapping(
    UserMapping* mapping) {
  additional_user_mapping_.reset(mapping);
}

void SystemUserMappingWindows::StartAdditionalUserMapping() {
  if (additional_user_mapping_.get()) {
    additional_user_mapping_->Start();
  }
}

void SystemUserMappingWindows::StopAdditionalUserMapping() {
  if (additional_user_mapping_.get()) {
    additional_user_mapping_->Stop();
  }
}

}  // namespace xtreemfs
#endif  // WIN32