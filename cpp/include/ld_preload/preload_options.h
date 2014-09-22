/*
 * Copyright (c) 2014 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LD_PRELOAD_PRELOAD_OPTIONS_H_
#define CPP_INCLUDE_LD_PRELOAD_PRELOAD_OPTIONS_H_

#include "libxtreemfs/options.h"

#include <boost/program_options.hpp>
#include <string>
#include <vector>

namespace xtreemfs {

class PreloadOptions : public Options {
 public:
  /** Sets the default values. */
  PreloadOptions();

  /** Set options parsed from command line which must contain at least the URL
   *  to an XtreemFS volume.
   *
   *  Calls Options::ParseCommandLine() to parse general options.
   *
   * @throws InvalidCommandLineParametersException
   * @throws InvalidURLException */
  void ParseCommandLine(int argc, char** argv);

  /** Outputs usage of the command line parameters. */
  virtual std::string ShowCommandLineHelp();

  // TODO: add preload-specific options here

 private:
  /** Contains all available preload options and its descriptions. */
  boost::program_options::options_description preload_descriptions_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LD_PRELOAD_PRELOAD_OPTIONS_H_
