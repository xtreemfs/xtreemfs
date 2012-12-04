/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
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
      string username = ConvertWindowsToUTF8(user_info->wkui1_username);
      string groupname = ConvertWindowsToUTF8(user_info->wkui1_logon_domain);
      NetApiBufferFree(user_info);

      if (additional_user_mapping_.get()) {
        string local_username(username);
        string local_groupname(groupname);
        additional_user_mapping_->LocalToGlobalUsername(local_username,
                                                        &username);
        additional_user_mapping_->LocalToGlobalGroupname(local_groupname,
                                                         &groupname);
      }

      user_credentials->set_username(username);
      user_credentials->add_groups(groupname);
    }
  } else {
     Logging::log->getLog(LEVEL_ERROR) <<
       "Failed to retrieve the current username and domain name, error"
       " code: " << result << endl;
  }
}

}  // namespace xtreemfs
#endif  // WIN32
