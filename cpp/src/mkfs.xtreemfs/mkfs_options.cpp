/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "mkfs.xtreemfs/mkfs_options.h"

#include <cmath>

#include <boost/algorithm/string.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/program_options/cmdline.hpp>
#include <iostream>
#include <sstream>

#include "libxtreemfs/helper.h"
#include "libxtreemfs/pbrpc_url.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace po = boost::program_options;
namespace style = boost::program_options::command_line_style;

namespace xtreemfs {

MkfsOptions::MkfsOptions() : Options() {
  // Modify default options of Options().
  max_tries = 1;

  helptext_usage_ =
      "mkfs.xtreemfs: Create an XtreemFS Volume on a specific MRC.\n"
      "\n"
      "Usage:\n"
      "\tmkfs.xtreemfs [options] [pbrpc[g|s]://]<mrc-host>[:port]/<new-volume-name>\n"  // NOLINT
      "\n"
      "  Example: mkfs.xtreemfs localhost/myVolume\n";

  // Password.
  admin_password = "";

  po::options_description password_descriptions("Admin Password");
  password_descriptions.add_options()
      ("admin_password",
        po::value(&admin_password)->default_value(admin_password),
        "MRC's admin_password (not required if not set at the MRC).");

  // Volume options.
  volume_mode_decimal = 511;
  volume_mode_octal = 777;
  volume_quota = "0";
  volume_priority = "0";
  owner_username = "";
  owner_groupname = "";
  access_policy_type = xtreemfs::pbrpc::ACCESS_CONTROL_POLICY_POSIX;
  access_policy_type_string = "POSIX";

  po::options_description volume_descriptions("Volume Options");
  volume_descriptions.add_options()
    ("mode,m",
      po::value(&volume_mode_octal)->default_value(volume_mode_octal),
     "Mode of the volume's root directory.")
    ("owner-username,u",
     po::value(&owner_username), "Owner of the new volume "
     "(by default it is the username of the effective user id).")
    ("owner-groupname,g",
     po::value(&owner_groupname), "Owning group of the new"
     " volume (by default it is the groupname of the effective group id).")
    ("access-control-policy,a",
     po::value(&access_policy_type_string)
         ->default_value(access_policy_type_string),
     "Access-control-policy=NULL|POSIX|VOLUME")
     ("quota,q", po::value(&volume_quota)->default_value(volume_quota),
       "Quota of the volume in bytes (default value 0, i.e. quota is disabled), format: <value>M|G|T"),
     ("priority", po::value(&volume_priority)->default_value(volume_priority),
       "Request priority of OSD requests for the volume (default priority 0).");

  // Striping policy options.
  default_striping_policy_type = xtreemfs::pbrpc::STRIPING_POLICY_RAID0;
  default_striping_policy_type_string = "RAID0";
  default_stripe_size = 128;
  default_stripe_width = 1;

  po::options_description striping_policy_descriptions_(
      "Striping Policy Options");
  striping_policy_descriptions_.add_options()
      ("striping-policy,p",
       po::value(&default_striping_policy_type_string)
           ->default_value(default_striping_policy_type_string),
       "Striping policy=RAID0")
      ("striping-policy-stripe-size,s",
       po::value(&default_stripe_size)->default_value(default_stripe_size),
       "Stripe size in kB.")
      ("striping-policy-width,w",
       po::value(&default_stripe_width)->default_value(default_stripe_width),
       "Number of OSDs (stripes) per replica.");

  // Volume Attributes.
  chown_non_root = false;

  po::options_description volume_attributes_descriptions_(
        "Volume Attributes");
  volume_attributes_descriptions_.add_options()
    ("volume-attribute",
     po::value< vector<string> >(&volume_attributes_strings),
     "Define volume specific attributes of the form name=value, e.g. "
     "\"chown_non_root=true\".")
    ("chown-non-root",
     po::value(&chown_non_root)->zero_tokens(),
     "Shortcut for --volume-attribute chown_non_root=true. If this attribute is"
     " not set, regular users (everybody except root) are not allowed to change"
     " the ownership of their _own_ files.");

  mkfs_descriptions_.add(password_descriptions)
                    .add(volume_descriptions)
                    .add(striping_policy_descriptions_)
                    .add(volume_attributes_descriptions_);
}

MkfsOptions::~MkfsOptions() {
}


void MkfsOptions::ParseCommandLine(int argc, char** argv) {
  // Parse general options and retrieve unregistered options for own parsing.
  vector<string> options = Options::ParseCommandLine(argc, argv);

  // Read Volume URL from command line.
  po::positional_options_description p;
  p.add("mrc_volume_url", 1);
  po::options_description positional_options("Create Volume URL");
  positional_options.add_options()
    ("mrc_volume_url", po::value(&xtreemfs_url), "volume to create");

  // Parse command line.
  po::options_description all_descriptions;
  all_descriptions.add(positional_options).add(mkfs_descriptions_);
  po::variables_map vm;
  try {
    po::store(po::command_line_parser(options)
        .options(all_descriptions)
        .positional(p)
        .style(style::default_style & ~style::allow_guessing)
        .run(), vm);
    po::notify(vm);
  } catch(const std::exception& e) {
    // Rethrow boost errors due to invalid command line parameters.
    throw InvalidCommandLineParametersException(string(e.what()));
  }

  // Do not check parameters if the help shall be shown.
  if (show_help || empty_arguments_list || show_version) {
    return;
  }

  // Extract information from command line.
  Options::ParseURL(kMRC);

  // Check for MRC host
  if(service_addresses.empty()) {
    throw InvalidCommandLineParametersException("missing MRC host.");
  } else if (service_addresses.IsAddressList()) {
    throw InvalidCommandLineParametersException(
        "more than one MRC host was specified.");
  } else {
    mrc_service_address = service_addresses.GetAddresses().front();
  }

  // Check for required parameters.
  if (volume_name.empty()) {
    throw InvalidCommandLineParametersException("missing volume name.");
  }

  // Abort the user explicitly specified numeric ids as owner.
  if (CheckIfUnsignedInteger(owner_username)) {
    throw InvalidCommandLineParametersException("Do not use numeric IDs as "
        "owner. Use names instead, e.g. \"root\" instead of \"0\".");
  }
  if (CheckIfUnsignedInteger(owner_groupname)) {
    throw InvalidCommandLineParametersException("Do not use numeric IDs as "
        "owner group. Use names instead, e.g. \"root\" instead of \"0\".");
  }

  // Convert the mode from octal to decimal.
  volume_mode_decimal = OctalToDecimal(volume_mode_octal);

  if (boost::iequals(access_policy_type_string, "NULL")) {
    access_policy_type = xtreemfs::pbrpc::ACCESS_CONTROL_POLICY_NULL;
  } else if (boost::iequals(access_policy_type_string, "POSIX")) {
    access_policy_type = xtreemfs::pbrpc::ACCESS_CONTROL_POLICY_POSIX;
  } else if (boost::iequals(access_policy_type_string, "VOLUME")) {
    access_policy_type = xtreemfs::pbrpc::ACCESS_CONTROL_POLICY_VOLUME;
  } else {
    throw InvalidCommandLineParametersException("Unknown access policy (" +
        access_policy_type_string + ") specified.");
  }

  if (boost::iequals(default_striping_policy_type_string, "RAID0")) {
    default_striping_policy_type = xtreemfs::pbrpc::STRIPING_POLICY_RAID0;
  } else {
    throw InvalidCommandLineParametersException("Currently the RAID0 striping"
        "policy is the only one available. Set the stripe width (see -w) to 1"
        " to disable striping at all.");
  }

  // Process volume attributes shortcuts.
  if (chown_non_root) {
    volume_attributes_strings.push_back("chown_non_root=true");
  }
  if (grid_auth_mode_globus) {
    volume_attributes_strings.push_back("globus_gridmap=true");
  }
  if (grid_auth_mode_unicore) {
    volume_attributes_strings.push_back("unicore_uudb=true");
  }

  // Parse list of volume attributes.
  for (size_t i = 0; i < volume_attributes_strings.size(); i++) {
    // Check if there is exactly one "=" delimiter.
    size_t first_match = volume_attributes_strings[i].find_first_of("=");
    if (first_match == string::npos) {
      throw InvalidCommandLineParametersException("The attribute key/value pair"
          " " + volume_attributes_strings[i] + " misses a \"=\".");
    }
    size_t next_match = volume_attributes_strings[i].find_first_of(
        "=",
        first_match + 1);
    if (next_match != string::npos) {
      throw InvalidCommandLineParametersException("The attribute key/value pair"
          " " + volume_attributes_strings[i] + " must not contain"
          " multiple \"=\".");
    }

    // Parse attribute.
    const std::string key = volume_attributes_strings[i].substr(0, first_match);
    const std::string value = volume_attributes_strings[i].substr(
        min(first_match + 1, volume_attributes_strings[i].length()),
        max(static_cast<size_t>(0),
            volume_attributes_strings[i].length() - first_match));
    volume_attributes[key] = value;
  }
}

std::string MkfsOptions::ShowCommandLineUsage() {
  return helptext_usage_
      + "\nFor complete list of options, please specify -h or --help.\n";
}

std::string MkfsOptions::ShowCommandLineHelp() {
  ostringstream stream;
  // No help text given in descriptions for positional mount options. Instead
  // the usage is explained here.
  stream << helptext_usage_
         // Descriptions of this class.
         << mkfs_descriptions_
         // Descriptions of the general options.
         << endl
         << Options::ShowCommandLineHelpVolumeCreationAndDeletion();
  return stream.str();
}

int MkfsOptions::OctalToDecimal(int octal) {
  int result = 0;
  for(int i = 0; octal != 0; i++) {
    int remainder = octal % 10;
    result += remainder * static_cast<int>(pow(static_cast<double>(8), i));
    octal /= 10;
  }
  return result;
}

}  // namespace xtreemfs
