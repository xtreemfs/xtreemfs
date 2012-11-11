/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "cbfs/cbfs_options.h"

#include <boost/lexical_cast.hpp>
#include <boost/program_options/cmdline.hpp>
#include <boost/tokenizer.hpp>
#include <iostream>
#include <sstream>

#include "libxtreemfs/helper.h"
#include "libxtreemfs/xtreemfs_exception.h"

using namespace std;

namespace po = boost::program_options;
namespace style = boost::program_options::command_line_style;

namespace xtreemfs {

CbFSOptions::CbFSOptions() : Options(), cbfs_descriptions_("CbFS Options") {
  // Windows Explorer copies files in 1 MB chunks and therefore more than
  // the default 128 kB.
  async_writes_max_request_size_kb = 1024 * 1024;

  // CbFS options.
  helptext_usage_ =
      "mount.xtreemfs: Mounts an XtreemFS Volume.\n"
      "\n"
      "Usage: \n"
      "\tmount.xtreemfs [options] [pbrpc[g|s]://]<dir-host>[:port]"
          "/<volume-name> <drive letter>\n"
      "\n"
      "  Example: mount.xtreemfs localhost/myVolume X:\n";
}

void CbFSOptions::ParseCommandLine(int argc, char** argv) {
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
  all_descriptions_.add(mount).add(cbfs_descriptions_);
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
  if (show_help || empty_arguments_list || show_version) {
    return;
  }

  // Extract information from command line.
  Options::ParseURL(kDIR);

  // Check for required parameters.
  if (service_addresses.empty()) {
    throw InvalidCommandLineParametersException("missing DIR host.");
  }
  if (volume_name.empty()) {
    throw InvalidCommandLineParametersException("missing volume name.");
  }
  if (mount_point.empty()) {
    throw InvalidCommandLineParametersException("missing mount point.");
  }
}

std::string CbFSOptions::ShowCommandLineUsage() {
  return helptext_usage_
      + "\nFor complete list of options, please specify -h or --help.\n";
}

std::string CbFSOptions::ShowCommandLineHelp() {
  ostringstream stream;
  // No help text given in descriptions for positional mount options. Instead
  // the usage is explained here.
  stream << helptext_usage_
         << endl
         // Descriptions of this class.
         << cbfs_descriptions_
         // Descriptions of the general options.
         << Options::ShowCommandLineHelp();
  return stream.str();
}

}  // namespace xtreemfs
