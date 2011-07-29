/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "fuse/fuse_options.h"

#include <boost/program_options/cmdline.hpp>
#include <iostream>
#include <sstream>

#include "libxtreemfs/xtreemfs_exception.h"

using namespace std;

namespace po = boost::program_options;
namespace style = boost::program_options::command_line_style;

namespace xtreemfs {

FuseOptions::FuseOptions() : Options(), fuse_descriptions_("Fuse Options") {
  // Overwrite certain members of Options().
  // Fuse's default interrupt signal is SIGUSR1 = 10.
  interrupt_signal = 10;
  // Never give up to execute a request.
  max_tries = 0;
  max_write_tries = 0;

  // Default Fuse options.
  enable_xattrs = false;
  foreground = false;
  use_fuse_permission_checks = true;

  fuse_descriptions_.add_options()
    ("foreground,f", po::value(&foreground)->zero_tokens(),
        "Do not fork into background.")
    ("fuse_option,o",
        po::value< vector<string> >(&fuse_options),
        "Passes -o=<option> to Fuse.");
  po::options_description fuse_acl_information(
      "ACL and extended attributes Support:\n"
      "  -o xtreemfs_acl Enable the correct evaluation of XtreemFS ACLs.\n"
      "                  (Note that you cannot use the system tools getfattr\n"
      "                   and setfattr; use 'xtfsutil' instead to set and\n"
      "                   retrieve ACLs.)\n"
      "  -o user_xattr   Enable user defined extended attributes.");
  fuse_descriptions_.add(fuse_acl_information);

  helptext_usage_ =
      "mount.xtreemfs: Mounts an XtreemFS Volume.\n"
      "\n"
      "Usage: \n"
      "\tmount.xtreemfs [options] [pbrpc[g|s]://]<dir-host>[:port]/<volume-name>"
          " <mount point>\n"
      "\n"
      "  Example: mount.xtreemfs localhost/myVolume ~/xtreemfs\n";
}

void FuseOptions::ParseCommandLine(int argc, char** argv) {
  // Parse general options and retrieve unregistered options for own parsing.
  vector<string> options = Options::ParseCommandLine(argc, argv);

  // Read Volume URL and mount point from command line.
  po::positional_options_description p;
  p.add("dir_volume_url", 1);
  p.add("mount_point", 1);
  po::options_description mount("Mount options");
  mount.add_options()
    ("dir_volume_url", po::value(&xtreemfs_url), "volume to mount")
    ("mount_point", po::value(&mount_point), "where to mount the volume");

  // Parse command line.
  po::options_description all_descriptions_;
  all_descriptions_.add(mount).add(fuse_descriptions_);
  po::variables_map vm;
  try {
    po::store(po::command_line_parser(options)
        .options(all_descriptions_)
        .positional(p)
        .style(style::default_style & ~style::allow_guessing)
        .run(), vm);
    po::notify(vm);
  } catch(const std::exception& e) {
    // Rethrow boost errors due to invalid command line parameters.
    throw InvalidCommandLineParametersException(string(e.what()));
  }

  // Do not check parameters if the help shall be shown.
  if (show_help || empty_arguments_list) {
    return;
  }

  // Enable extended attributes if -o acl or -o user_xattr is given.
  for (int i = 0; i < fuse_options.size(); i++) {
    if (fuse_options[i] == "acl") {
      throw InvalidCommandLineParametersException(
          "The option -o acl is not supported. Specify -o xtreemfs_acl instead."
          "\n\nWe do not allow -o acl because XtreemFS does not support the "
          "getfacl and setfacl tools. You have to use 'xtfs_acl' instead "
          "to set and retrieve ACLs.");
    }
    if (fuse_options[i] == "user_xattr") {
      enable_xattrs = true;
      // Don't send this option to Fuse.
      fuse_options.erase(fuse_options.begin() + i);
      i--;
      break;
    }
    if (fuse_options[i] == "xtreemfs_acl") {
      // Fuse may prevent operations based on the evaluation of stat records
      // altough a user is allowed to due to further ACLs, so we disable this
      // Fuse feature here.
      use_fuse_permission_checks = false;
      // Don't send this option to Fuse.
      fuse_options.erase(fuse_options.begin() + i);
      i--;
      break;
    }
    if (fuse_options[i] == "intr") {
      // Don't send this option to Fuse.
      fuse_options.erase(fuse_options.begin() + i);
      i--;
      throw InvalidCommandLineParametersException(
          "The option -o intr will be ignored as command line parameter and"
          " not passed through to Fuse. Use --interrupt-signal instead.");
    }
    if (fuse_options[i].substr(0, 12) == "intr_signal=") {
      // Don't send this option to Fuse.
      fuse_options.erase(fuse_options.begin() + i);
      i--;
      throw InvalidCommandLineParametersException(
          "The option -o intr_signal will be ignored as command line "
          "parameter and not passed through to Fuse. Use --interrupt-signal"
          "instead.");
    }
  }

  // Extract information from command line.
  Options::ParseURL(kDIR);

  // Check for required parameters.
  if (service_address.empty()) {
    throw InvalidCommandLineParametersException("missing DIR host.");
  }
  if (volume_name.empty()) {
    throw InvalidCommandLineParametersException("missing volume name.");
  }
  if (mount_point.empty()) {
    throw InvalidCommandLineParametersException("missing mount point.");
  }
}

std::string FuseOptions::ShowCommandLineUsage() {
  return helptext_usage_
      + "\nFor complete list of options, please specify -h or --help.\n";
}

std::string FuseOptions::ShowCommandLineHelp() {
  ostringstream stream;
  // No help text given in descriptions for positional mount options. Instead
  // the usage is explained here.
  stream << helptext_usage_
         << endl
         // Descriptions of this class.
         << fuse_descriptions_
         // Descriptions of the general options.
         << Options::ShowCommandLineHelp();
  return stream.str();
}

}  // namespace xtreemfs
