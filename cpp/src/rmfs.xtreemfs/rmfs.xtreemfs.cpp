/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <boost/scoped_ptr.hpp>
#include <iostream>
#include <string>
#include <unistd.h>

#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/user_mapping.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "rmfs.xtreemfs/rmfs_options.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

int main(int argc, char* argv[]) {
  // Parse command line options.
  RmfsOptions options;
  bool invalid_commandline_parameters = false;
  try {
    options.ParseCommandLine(argc, argv);
  } catch(const XtreemFSException& e) {
    cout << "Invalid parameters found, error: " << e.what() << endl << endl;
    invalid_commandline_parameters = true;
  }
  // Display help if needed.
  if (options.empty_arguments_list) {
    cout << options.ShowCommandLineUsage() << endl;
    return 1;
  }
  if (options.show_help || invalid_commandline_parameters) {
    cout << options.ShowCommandLineHelp() << endl;
    return 1;
  }

  // Set user_credentials.
  boost::scoped_ptr<UserMapping> user_mapping(UserMapping::CreateUserMapping(
      options.user_mapping_type,
      UserMapping::kUnix,
      options));
  UserCredentials user_credentials;
  user_credentials.set_username(user_mapping->UIDToUsername(geteuid()));
  if (user_credentials.username().empty()) {
    cout << "Error: No name found for the current user (using the configured "
        "UserMapping: " << options.user_mapping_type << ")\n";
    return 1;
  }
  // The groups won't be checked and maybe therefore empty.
  user_credentials.add_groups(user_mapping->GIDToGroupname(getegid()));

  // Create a new client and start it.
  boost::scoped_ptr<Client> client(Client::CreateClient(
      "DIR-host-not-required-for-rmfs",  // Using a bogus value as DIR address.
      user_credentials,
      options.GenerateSSLOptions(),
      options));
  client->Start();

  // Create the volume.
  Auth auth;
  if (options.admin_password.empty()) {
    auth.set_auth_type(AUTH_NONE);
  } else {
    auth.set_auth_type(AUTH_PASSWORD);
    auth.set_auth_data(options.admin_password);
  }
  cout << "Trying to delete the volume: " << options.xtreemfs_url << endl;

  bool success = true;
  try {
    client->DeleteVolume(options.service_address,
                         auth,
                         user_credentials,
                         options.volume_name);
  } catch (const XtreemFSException& e) {
    success = false;
    cout << "Failed to delete the volume, error:\n"
         << "\t" << e.what() << endl;
  }

  // Cleanup.
  client->Shutdown();

  if (success) {
    cout << "Successfully deleted the volume \"" << options.volume_name
             << "\" at MRC: " << options.service_address << "\n"
         << "\n"
         << "The disk space on the OSDs, occupied by the objects of the\n"
            "files of the deleted volume, is not freed yet.\n"
         << "Run the tool \"xtfs_cleanup\" to free it." << endl;
    return 0;
  } else {
    return 1;
  }
}
