/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_MKFS_XTREEMFS_MKFS_OPTIONS_H_
#define CPP_INCLUDE_MKFS_XTREEMFS_MKFS_OPTIONS_H_

#include "libxtreemfs/options.h"

#include <boost/program_options.hpp>
#include <list>
#include <string>

#include "pbrpc/RPC.pb.h"
#include "xtreemfs/MRC.pb.h"

namespace xtreemfs {

class MkfsOptions : public Options {
 public:
  /** Sets the default values. */
  MkfsOptions();

  /** Frees memory of KeyValuePair objects created for the volume attributes. */
  ~MkfsOptions();

  /** Set options parsed from command line which must contain at least the URL
   *  to a XtreemFS volume.
   *
   *  Calls Options::ParseCommandLine() to parse general options.
   *
   * @throws InvalidCommandLineParametersException
   * @throws InvalidURLException */
  void ParseCommandLine(int argc, char** argv);

  /** Shows only the minimal help text describing the usage of mkfs.xtreemfs.*/
  std::string ShowCommandLineUsage();

  /** Outputs usage of the command line parameters. */
  virtual std::string ShowCommandLineHelp();

  /** Converts an octal value (e.g. 777) to a decimal value (e.g. ). */
  int OctalToDecimal(int octal);

  /** MRC admin_password as set in the MRC config. */
  std::string admin_password;

  /** The service_adresses of Options became service_address, so we can no
   *  longer use this option for the MRC address and need a new member.
   */
  std::string mrc_service_address;

  // Volume options.
  /** Permissions mode of "/".
   *
   * @attention This value is stored in decimal representation. On the other
   *            hand, it is assumed that the user does specify the value in octal
   *            representation at the command line. Therefore,
   *            ParseCommandLine() does convert it from octal to decimal.
   */
  int volume_mode_decimal;

  /** Volume mode in octal form as specified at the command line. */
  int volume_mode_octal;

  /** Quota of the volume*/
  std::string volume_quota;

  /** Name of the owner of the new volume. */
  std::string owner_username;

  /** Owning group of the new volume. */
  std::string owner_groupname;

  /** Enforced access control policy: NULL|POSIX|VOLUME. */
  xtreemfs::pbrpc::AccessControlPolicyType access_policy_type;

  /** Will be parsed by ParseCommandLine() to set access_policy_type. */
  std::string access_policy_type_string;

  /** Default striping policy for new files: NONE|RAID0. */
  xtreemfs::pbrpc::StripingPolicyType default_striping_policy_type;

  /** Will be parsed by ParseCommandLine() to set
   *  default_striping_policy_type. */
  std::string default_striping_policy_type_string;

  /** Default stripe size for new files (in kB). */
  int default_stripe_size;

  /** Default stripe width for new files (number of OSDs per replica). */
  int default_stripe_width;

  /** Default parity width for new files (number of parity OSDs per replica). */
  int default_parity_width;

  /** List of user defined volume attributes. */
  std::list<xtreemfs::pbrpc::KeyValuePair*> volume_attributes;

  /** Will be parsed by ParseCommandLine() to set volume_attributes. */
  std::vector<std::string> volume_attributes_strings;

  /** If true, chown_non_root=true will be added to the list of attributes. */
  bool chown_non_root;

 private:
  /** Contains all available mkfs options and its descriptions. */
  boost::program_options::options_description mkfs_descriptions_;

  /** Brief help text if there are no command line arguments. */
  std::string helptext_usage_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_MKFS_XTREEMFS_MKFS_OPTIONS_H_
