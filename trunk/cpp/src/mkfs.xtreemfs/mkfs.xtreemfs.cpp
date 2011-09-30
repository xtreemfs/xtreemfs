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
#include "mkfs.xtreemfs/mkfs_options.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

int main(int argc, char* argv[]) {
  // Parse command line options.
  MkfsOptions options;
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
  // Show only the version.
  if (options.show_version) {
    cout << options.ShowVersion("mkfs.xtreemfs") << endl;
    return 1;
  }

  // Set user_credentials.
  boost::scoped_ptr<UserMapping> user_mapping(UserMapping::CreateUserMapping(
      options.user_mapping_type,
      UserMapping::kUnix,
      options));
  if (options.owner_username.empty()) {
    options.owner_username = user_mapping->UIDToUsername(geteuid());
    if (CheckIfUnsignedInteger(options.owner_username)) {
      if (Logging::log->loggingActive(LEVEL_WARN)) {
        Logging::log->getLog(LEVEL_WARN)
            << "Failed to map the UID "
            << geteuid() << " to a username."
            " Now the value \"" << options.owner_username << "\" will be set"
            " as owner of the volume."
            " Keep in mind that mount.xtreemfs does"
            " always try to map UIDs to names. If this is not consistent over"
            " all your systems (the UID does not always get mapped to the same"
            " name), you may run into permission problems." << endl;
      }
    }
    if (options.owner_username.empty()) {
      cout << "Error: No name found for the current user (using the configured "
          "UserMapping: " << options.user_mapping_type << ")\n";
      return 1;
    }
  }
  if (options.owner_groupname.empty()) {
    options.owner_groupname = user_mapping->GIDToGroupname(getegid());
    if (CheckIfUnsignedInteger(options.owner_groupname)) {
      if (Logging::log->loggingActive(LEVEL_WARN)) {
        Logging::log->getLog(LEVEL_WARN)
            << "Failed to map the GID "
            << getegid() << " to a groupname."
            " Now the value \"" << options.owner_groupname << "\" will be set"
            " as owning group of the volume."
            " Keep in mind that mount.xtreemfs does"
            " always try to map GIDs to names. If this is not consistent over"
            " all your systems (the GID does not always get mapped to the same"
            " group name), you may run into permission problems." << endl;
      }
    }
    if (options.owner_groupname.empty()) {
      cout << "Error: No name found for the primary group of the current user "
          "(using "
          "the configured UserMapping: " << options.user_mapping_type << ")\n";
      return 1;
    }
  }
  UserCredentials user_credentials;
  user_credentials.set_username(options.owner_username);
  user_credentials.add_groups(options.owner_groupname);
  Auth auth;
  if (options.admin_password.empty()) {
    auth.set_auth_type(AUTH_NONE);
  } else {
    auth.set_auth_type(AUTH_PASSWORD);
    auth.mutable_auth_passwd()->set_password(options.admin_password);
  }

  // Repeat the used options.
  cout << "Trying to create the volume: " << options.xtreemfs_url << "\n"
       << "\n"
       << "Using options:\n"
       << "  Owner:\t\t\t" << options.owner_username << "\n"
       << "  Owning group:\t\t\t" << options.owner_groupname << "\n"
       << "  Mode:\t\t\t\t" << options.volume_mode_octal << "\n"
       << "  Access Control Policy:\t" << options.access_policy_type_string
           << "\n"
       << "\n"
       << "  Default striping policy:\t\t"
           << options.default_striping_policy_type_string << "\n"
       << "  Default stripe size (object size):\t"
           << options.default_stripe_size << "\n"
       << "  Default stripe width (# OSDs):\t"
           << options.default_stripe_width << "\n"
       << "\n";
  if (options.volume_attributes.size() > 0) {
    cout << "  Volume attributes (Name = Value)" << endl;
    for (list<KeyValuePair*>::iterator it = options.volume_attributes.begin();
         it != options.volume_attributes.end();
         ++it) {
      cout << "    " << (*it)->key() << " = " << (*it)->value() << endl;
    }
    cout << endl;
  }

  // Create a new client and start it.
  boost::scoped_ptr<Client> client(Client::CreateClient(
      "DIR-host-not-required-for-mkfs",  // Using a bogus value as DIR address.
      user_credentials,
      options.GenerateSSLOptions(),
      options));
  client->Start();

  // Create the volume.
  bool success = true;
  try {
    client->CreateVolume(options.service_address,
                         auth,
                         user_credentials,
                         options.volume_name,
                         options.volume_mode_decimal,
                         options.owner_username,
                         options.owner_groupname,
                         options.access_policy_type,
                         options.default_striping_policy_type,
                         options.default_stripe_size,
                         options.default_stripe_width,
                         options.volume_attributes);
  } catch (const XtreemFSException& e) {
    success = false;
    cout << "Failed to create the volume, error:\n"
         << "\t" << e.what() << endl;
  }

  // Cleanup.
  client->Shutdown();

  if (success) {
    cout << "Successfully created volume \"" << options.volume_name << "\" at "
        "MRC: " << options.xtreemfs_url << endl;
    return 0;
  } else {
    return 1;
  }
}
