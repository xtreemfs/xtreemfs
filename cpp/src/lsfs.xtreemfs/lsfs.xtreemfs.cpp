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
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "lsfs.xtreemfs/lsfs_options.h"
#include "util/logging.h"
#include "xtreemfs/MRC.pb.h"

using namespace std;
using namespace xtreemfs;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

int main(int argc, char* argv[]) {
  // Parse command line options.
  LsfsOptions options;
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
    cout << options.ShowVersion("lsfs.xtreemfs") << endl;
    return 1;
  }

  // Set user_credentials.
  UserCredentials user_credentials;
  user_credentials.set_username("xtreemfs");
  user_credentials.add_groups("xtreemfs");

  Auth auth;
  if (options.admin_password.empty()) {
    auth.set_auth_type(AUTH_NONE);
  } else {
    auth.set_auth_type(AUTH_PASSWORD);
    auth.mutable_auth_passwd()->set_password(options.admin_password);
  }

  // Create a new client and start it.
  boost::scoped_ptr<Client> client(Client::CreateClient(
      "DIR-host-not-required-for-lsfs",  // Using a bogus value as DIR address.  // NOLINT
      user_credentials,
      options.GenerateSSLOptions(),
      options));
  client->Start();

  // Create the volume.
  cout << "Listing all volumes of the MRC: " << options.xtreemfs_url << endl;

  bool success = true;
  boost::scoped_ptr<xtreemfs::pbrpc::Volumes> volumes(NULL);
  try {
    volumes.reset(client->ListVolumes(options.mrc_service_address, auth));
  } catch (const XtreemFSException& e) {
    success = false;
    cout << "Failed to list the volumes, error:\n"
         << "\t" << e.what() << endl;
  }

  // Cleanup.
  client->Shutdown();

  if (success) {
    cout << "Volumes on " << options.mrc_service_address
         << " (Format: volume name -> volume UUID):" << endl;
    for (int i = 0; i < volumes->volumes_size(); i++) {
      const xtreemfs::pbrpc::Volume& volume = volumes->volumes(i);
      cout << "\t" << volume.name() << "\t->\t" << volume.id() << endl;
    }
    cout << "End of List." << endl;
    return 0;
  } else {
    return 1;
  }
}
