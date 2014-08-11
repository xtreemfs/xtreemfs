/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "ld_preload/preload_options.h"

#include <csignal>

#include <boost/lexical_cast.hpp>
#include <boost/program_options/cmdline.hpp>
#include <boost/tokenizer.hpp>
#include <iostream>
#include <sstream>

#include "libxtreemfs/helper.h"
#include "libxtreemfs/xtreemfs_exception.h"

#include "ld_preload/passthrough.h"

using namespace std;

namespace po = boost::program_options;
namespace style = boost::program_options::command_line_style;

namespace xtreemfs {

PreloadOptions::PreloadOptions() : Options(), preload_descriptions_("Preload Options") {
  // TODO:
  //preload_descriptions_.add_options()
}

void PreloadOptions::ParseCommandLine(int argc, char** argv) {
  // Parse general options and retrieve unregistered options for own parsing.
  vector<string> options = Options::ParseCommandLine(argc, argv);
  // Read Volume URL and mount point from command line.
  po::positional_options_description p;
  p.add("xtreemfs_url", 1);
  p.add("mount_point", 1);
  po::options_description mount("Mount options");
  mount.add_options()
    ("xtreemfs_url", po::value(&xtreemfs_url), "DIR to use (without volume)")
    ("mount_point", po::value(&mount_point), "where to virtually mount the volume")
    ;
  // Parse command line.
  po::options_description all_descriptions_;
  all_descriptions_.add(mount).add(preload_descriptions_);
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
  try {
    Options::ParseURL(kDIR);
  } catch(const xtreemfs::XtreemFSException& e) {
    xprintf("excpetion\n");
    cout << "ParseURL, error: " << e.what() << endl << endl;
  }

  // Check for required parameters.
  if (service_addresses.empty()) {
    throw InvalidCommandLineParametersException("missing DIR host.");
  }
  if (volume_name.empty()) {
    throw InvalidCommandLineParametersException("missing volume name.");
  }
  if (mount_point.empty()) {
    throw InvalidCommandLineParametersException("missing virtual mount point.");
  }
}

std::string PreloadOptions::ShowCommandLineHelp() {
  ostringstream stream;
  stream << preload_descriptions_
         << Options::ShowCommandLineHelp();
  return stream.str();
}

}  // namespace xtreemfs
