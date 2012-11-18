/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <boost/scoped_ptr.hpp>
#include <iostream>
#include <string>

#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/system_user_mapping.h"
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
  if (options.empty_arguments_list || invalid_commandline_parameters) {
    cout << options.ShowCommandLineUsage() << endl;
    return 1;
  }
  if (options.show_help) {
    cout << options.ShowCommandLineHelp() << endl;
    return 1;
  }
  // Show only the version.
  if (options.show_version) {
    cout << options.ShowVersion("rmfs.xtreemfs") << endl;
    return 1;
  }

  // Safety question
  if (!options.force) {
    string answer;
    cout << "Do you really want to delete the volume: \""
         << options.xtreemfs_url << "\"?" << endl
         << "Answer with \"YES\" to proceed: ";

    getline(cin, answer);
    if (answer != "YES") {
      return 1;
    }
  }


  bool success = true;
  boost::scoped_ptr<SystemUserMapping> system_user_mapping;
  boost::scoped_ptr<Client> client;
  try {
    // Start logging manually (although it would be automatically started by
    // ClientImplementation()) as its required by UserMapping.
    initialize_logger(options.log_level_string,
                      options.log_file_path,
                      LEVEL_WARN);

    // Set user_credentials.
    system_user_mapping.reset(SystemUserMapping::GetSystemUserMapping());
    // Check if the user specified an additional user mapping in options.
    UserMapping* additional_um = UserMapping::CreateUserMapping(
        options.additional_user_mapping_type,
        options);
    if (additional_um) {
      system_user_mapping->RegisterAdditionalUserMapping(additional_um);
      system_user_mapping->StartAdditionalUserMapping();
    }

    UserCredentials user_credentials;
    system_user_mapping->GetUserCredentialsForCurrentUser(&user_credentials);
    if (user_credentials.username().empty()) {
      cout << "Error: No name found for the current user (using the configured "
          "UserMapping: " << options.additional_user_mapping_type << ")\n";
      return 1;
    }
    // The groups won't be checked and therefore may be empty.

    // Create a new client and start it.
    client.reset(Client::CreateClient(
        "DIR-host-not-required-for-rmfs",  // Using a bogus value as DIR address.  // NOLINT
        user_credentials,
        options.GenerateSSLOptions(),
        options));
    client->Start();

    // Delete the volume.
    Auth auth;
    if (options.admin_password.empty()) {
      auth.set_auth_type(AUTH_NONE);
    } else {
      auth.set_auth_type(AUTH_PASSWORD);
      auth.mutable_auth_passwd()->set_password(options.admin_password);
    }
    cout << "Trying to delete the volume: " << options.xtreemfs_url << endl;

    client->DeleteVolume(options.mrc_service_address,
                         auth,
                         user_credentials,
                         options.volume_name);
  } catch (const XtreemFSException& e) {
    success = false;
    cout << "Failed to delete the volume, error:\n"
         << "\t" << e.what() << endl;
  }

  // Cleanup.
  if (client) {
    client->Shutdown();
  }
  system_user_mapping->StopAdditionalUserMapping();

  if (success) {
    cout << "Successfully deleted the volume \"" << options.volume_name
             << "\" at MRC: " << options.mrc_service_address << "\n"
         << "\n"
         << "The disk space on the OSDs, occupied by the objects of the\n"
            "files of the deleted volume, is not freed yet.\n"
         << "Run the tool 'xtfs_cleanup' to free it." << endl;
    return 0;
  } else {
    return 1;
  }
}
