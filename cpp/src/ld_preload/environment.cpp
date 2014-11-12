/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "ld_preload/environment.h"

#include <cstdlib>
#include <cstring>
#include <stdio.h>
#include <fcntl.h>
#include <list>
#include <string>
#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/pbrpc_url.h"
#include "util/logging.h"

#include "ld_preload/misc.h"
#include "ld_preload/preload_options.h"

Environment::Environment() : volume_(NULL) {
  xprintf("Environment::Environment()\n");

  // get options string from the environment
  const char program_name_dummy[] = "program "; // prefix dummy for options string
  const char* options_env = std::getenv("XTREEMFS_PRELOAD_OPTIONS");
  xprintf("Environment::Environment(): getenv returned: %p\n", options_env);
  if (!options_env) {
    exit(-1);
  }
  //const char options_env[] = "demo.xtreemfs.org/demo /home/b/bemnoack/bqcd/remote-test";

  // test for null-string and exit
  if (!options_env) {
    xprintf("Environment::Environment(): error: XTREEMFS_PRELOAD_OPTIONS environment variable not set or empty.\n");
  }
  size_t options_c_str_length = std::strlen(options_env) + std::strlen(program_name_dummy);
  char* options_c_str = new char[options_c_str_length + 1];
  xprintf("Environment::Environment(): XTREEMFS_PRELOAD_OPTIONS='%s'\n", options_env);

  std::strcpy(options_c_str, program_name_dummy);
  std::strcat(options_c_str, options_env);
  xprintf("Environment::Environment(): options_c_str='%s'\n", options_c_str);
  // tokenise by space (overwrite first space after each token with null-termination)
  char *pos;
  std::vector<char*> arg_vector;
  pos = strtok(options_c_str, " ");
  arg_vector.push_back(pos);
  while (pos != NULL) {
    //xprintf("Environment::Environment(): token: %s\n", pos);
    pos = strtok(NULL, " ");
    arg_vector.push_back(pos); // last strtok returns NULL, by standard: argv[argc] == 0
  }
  xprintf("Environment::Environment()\n");
  char** argv = new char*[arg_vector.size()];
  for (int i = 0; i < arg_vector.size(); ++i) {
    argv[i] = arg_vector[i];
  }
  int argc = arg_vector.size() - 1; // last NULL entry does not count
  xprintf("Environment::Environment()\n");
  // generate argc, argv like arguments for command line parsing
  options_.ParseCommandLine(argc, argv);
  xprintf("Environment::Environment()\n");
  delete [] argv;
  delete [] options_c_str;

  xtreemfs::util::initialize_logger(options_.log_level_string,
                        options_.log_file_path,
                        xtreemfs::util::LEVEL_WARN);

  xprintf("enable_async_writes: %d\n", options_.enable_async_writes);

  // user credentials:
  uid_t uid = getuid();
  gid_t gid = getgid();
  pid_t pid = getpid();

  user_creds_.set_username(system_user_mapping_.UIDToUsername(uid));
  std::list<std::string> groupnames;
  system_user_mapping_.GetGroupnames(uid, gid, pid, &groupnames);
  for (std::list<std::string>::iterator it = groupnames.begin();
       it != groupnames.end(); ++it) {
    user_creds_.add_groups(*it);
  }

  // client setup
  xprintf("Environment::Environment(): Client setup start\n");
  client_ = xtreemfs::Client::CreateClient(options_.service_addresses, user_creds_, NULL, options_);
  client_->Start();
  xprintf("Environment::Environment(): Client setup end\n");

  // open volume
  xprintf("Environment::Environment(): Opening volume %s\n", options_.volume_name.c_str());
  volume_ = client_->OpenVolume(options_.volume_name, NULL, options_);
  volume_name_ = options_.volume_name;

  std::string prefix_env(options_.mount_point);
  Path::SetXtreemFSPrefix(prefix_env);
}

Environment::~Environment() {
  xprintf("Environment::~Environment(): Closing volume.\n");
//  volume_ = client_->CloseVolume(volume_); // not in the Client interface, but performed implicitly in ClientImplementation::Shutdown
  client_->Shutdown();
  xprintf("Environment::~Environment()\n");
}

xtreemfs::Volume* Environment::GetVolume() {
  return volume_;
}

xtreemfs::SystemUserMappingUnix& Environment::GetSystemUserMapping() {
  return system_user_mapping_;
}
