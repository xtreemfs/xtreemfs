/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef PRELOAD_ENVIRONMENT_H_
#define PRELOAD_ENVIRONMENT_H_

#include <string>
#include <stdint.h>
#include <sys/types.h>

#include "libxtreemfs/system_user_mapping_unix.h"
#include "libxtreemfs/volume_implementation.h"

#include "ld_preload/open_file_table.h"
#include "ld_preload/path.h"
#include "ld_preload/preload_options.h"

namespace xtreemfs {
class Client;
class VolumeHandle;
}

class Environment {
 public:
  Environment();
  ~Environment();
  //xtreemfs::Volume* GetVolume(const std::string& volume_name);
  xtreemfs::Volume* GetVolume();
  xtreemfs::SystemUserMappingUnix& GetSystemUserMapping();


  xtreemfs::PreloadOptions options_;
  xtreemfs::Client* client_;
  xtreemfs::Volume* volume_;
  std::string volume_name_;

  /** Translates between local and remote usernames and groups. */
  xtreemfs::SystemUserMappingUnix system_user_mapping_;
  xtreemfs::pbrpc::UserCredentials user_creds_;
  OpenFileTable open_file_table_;
};

#endif  // PRELOAD_ENVIRONMENT_H_
