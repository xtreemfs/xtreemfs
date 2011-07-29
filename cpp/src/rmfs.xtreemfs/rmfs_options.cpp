/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "rmfs.xtreemfs/rmfs_options.h"

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

RmfsOptions::RmfsOptions() : Options() {
  // Modify default options of Options().
  max_tries = 1;

  helptext_usage_ =
      "rmfs.xtreemfs: Delete an XtreemFS Volume on a specific MRC.\n"
      "\n"
      "Usage:\n"
      "\trmfs.xtreemfs [options] [pbrpc[g|s]://]<mrc-host>[:port]/<volume-name>\n"  // NOLINT
      "\n"
      "  Example: rmfs.xtreemfs localhost/myVolume\n";

  // Password.
  admin_password = "";

  po::options_description password_descriptions("Admin Password");
  password_descriptions.add_options()
      ("admin_password",
        po::value(&admin_password)->default_value(admin_password),
        "MRC's admin_password (not required if not set at the MRC).");

  // TODO(mberlin): Add an option to specify the triggering of the cleanup
  //                process to immediately free (now) orphaned objects.

  rmfs_descriptions_.add(password_descriptions);
}

void RmfsOptions::ParseCommandLine(int argc, char** argv) {
  // Parse general options and retrieve unregistered options for own parsing.
  vector<string> options = Options::ParseCommandLine(argc, argv);

  // Read Volume URL from command line.
  po::positional_options_description p;
  p.add("mrc_volume_url", 1);
  po::options_description positional_options("Delete Volume URL");
  positional_options.add_options()
    ("mrc_volume_url", po::value(&xtreemfs_url), "volume to delete");

  // Parse command line.
  po::options_description all_descriptions;
  all_descriptions.add(positional_options).add(rmfs_descriptions_);
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
  if (show_help || empty_arguments_list) {
    return;
  }

  // Extract information from command line.
  Options::ParseURL(kMRC);

  // Check for required parameters.
  if (service_address.empty()) {
    throw InvalidCommandLineParametersException("missing MRC host.");
  }
  if (volume_name.empty()) {
    throw InvalidCommandLineParametersException("missing volume name.");
  }
}

std::string RmfsOptions::ShowCommandLineUsage() {
  return helptext_usage_
      + "\nFor complete list of options, please specify -h or --help.\n";
}

std::string RmfsOptions::ShowCommandLineHelp() {
  ostringstream stream;
  // No help text given in descriptions for positional mount options. Instead
  // the usage is explained here.
  stream << helptext_usage_
         // Descriptions of this class.
         << rmfs_descriptions_
         // Descriptions of the general options.
         << endl
         << Options::ShowCommandLineHelpVolumeDeletionAndListing();
  return stream.str();
}

}  // namespace xtreemfs
