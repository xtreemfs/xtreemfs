/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *                    2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/options.h"

#include <boost/program_options/cmdline.hpp>
#include <iostream>
#include <string>

#ifdef __APPLE__
  // for getpwuid
  #include <sys/types.h>
  #include <pwd.h>
#else
  // for getenv
  #include <cstdlib>
#endif

#include "rpc/ssl_options.h"
#include "libxtreemfs/pbrpc_url.h"
#include "libxtreemfs/version_management.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"
#include "xtreemfs/GlobalTypes.pb.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace po = boost::program_options;
namespace style = boost::program_options::command_line_style;

namespace xtreemfs {

Options::Options()
    : general_("General options"),
      optimizations_("Optimizations"),
      error_handling_("Error Handling options"),
      ssl_options_("SSL options"),
      grid_options_("Grid Support options"),
      vivaldi_options_("Vivaldi Options"),
      xtreemfs_advanced_options_("XtreemFS Advanced options") {
  version_string = XTREEMFS_VERSION_STRING;

  // XtreemFS URL Options.
  xtreemfs_url = "";
  volume_name = "";
  protocol = "";
  mount_point = "";

  // General options.
  log_level_string = "WARN";
  log_file_path = "";
  show_help = false;
  empty_arguments_list = false;
  show_version = false;

  // Optimizations.
  metadata_cache_size = 100000;
  metadata_cache_ttl_s = 120;
  enable_async_writes = false;
  async_writes_max_request_size_kb = 128;  // default object size in kB.
  async_writes_max_requests = 10;  // Only 10 pending requests allowed by default.
  readdir_chunk_size = 1024;
  enable_atime = false;
  object_cache_size = 0;

  // Error Handling options.
  // A RPC call may be retried up to "max{_read|_write|}_tries" times. The
  // different parameters are considered depending on the operation
  // (read, write, rest). Different parameters were introduced because Fuse
  // under Linux does not allow to interrupt read() requests if the disk cache
  // is involved and therefore it's not wise to retry read() requests by
  // default.
  //
  // A RPC call will block at least for the minimum out of
  // ("retry_delay_s", "connect_timeout_s", "request_timeout_s") and at most
  // for the maximum out of the three parameters.
  //
  // The parameter "retry_delay_s" exists to enforce a lower bound and avoid
  // flooding the server. For example, an unsuccessful connect may return much
  // earlier than "connect_timeout_s" (e.g. in most cases a connect with the
  // error 'connection refused' returns immediately.).
  max_tries = 40;
  max_read_tries = 40;
  max_write_tries = 40;
  retry_delay_s = 15;
  connect_timeout_s = 15;
  request_timeout_s = 15;
  linger_timeout_s = 600;  // 10 Minutes.

  // SSL options.
  ssl_pem_cert_path = "";
  ssl_pem_key_path = "";
  ssl_pem_key_pass = "";
  ssl_pkcs12_path = "";
  ssl_pkcs12_pass = "";

  // Grid Support options.
  grid_ssl = false;
  grid_auth_mode_globus = false;
  grid_auth_mode_unicore = false;
  grid_gridmap_location = "";
  grid_gridmap_location_default_globus = "/etc/grid-security/grid-mapfile";
  grid_gridmap_location_default_unicore = "/etc/grid-security/d-grid_uudb";
  grid_gridmap_reload_interval_m = 60;  // 60 Minutes = 1 Hour.

  // Vivaldi Options
  vivaldi_enable = false;
  vivaldi_enable_dir_updates = false;
#ifdef __linux__
  char* home_dir = getenv("HOME");
  if (home_dir) {
    vivaldi_filename = string(home_dir) + "/.xtreemfs_vivaldi_coordinates";
  } else {
    vivaldi_filename = ".xtreemfs_vivaldi_coordinates";
  }
#elif defined __APPLE__
  struct passwd* pwd = getpwuid(getuid());
  if (pwd) {
    vivaldi_filename = string(pwd->pw_dir) + "/.xtreemfs_vivaldi_coordinates";
  } else {
    vivaldi_filename = ".xtreemfs_vivaldi_coordinates";
  }
#elif defined WIN32
  char* home_drive = getenv("HOMEDRIVE");
  char* home_path = getenv("HOMEPATH");
  if (home_drive && home_path) {
    vivaldi_filename = string(home_drive) + string(home_path)
                       + "/.xtreemfs_vivaldi_coordinates";
  } else {
    vivaldi_filename = ".xtreemfs_vivaldi_coordinates";
  }
#else
  vivaldi_filename = ".xtreemfs_vivaldi_coordinates";
#endif
  vivaldi_recalculation_interval_s = 300;
  vivaldi_recalculation_epsilon_s = 30;
  vivaldi_max_iterations_before_updating = 12;
  vivaldi_max_request_retries = 2;

  // Advanced XtreemFS options.
  periodic_file_size_updates_interval_s = 60;  // Default: 1 Minute.
  periodic_xcap_renewal_interval_s = 60;  // Default: 1 Minute.
  vivaldi_zipf_generator_skew = 0.5;

  // Internal options, not available from the command line interface.
  was_interrupted_function = NULL;

  // NOTE: Deprecated options are no longer needed as members

  // No additional user mapping is used by default.
  additional_user_mapping_type = UserMapping::kNone;

  all_descriptions_initialized_ = false;
}

void Options::GenerateProgramOptionsDescriptions() {
  if (all_descriptions_initialized_) {
    return;
  }

  // Init boost::program_options specific things, define options.
  general_.add_options()
    ("log-level,d",
        po::value(&log_level_string)->default_value(log_level_string),
        "EMERG|ALERT|CRIT|ERR|WARNING|NOTICE|INFO|DEBUG")
    ("log-file-path,l",
        po::value(&log_file_path)->default_value(log_file_path),
        "Path to log file.")
    ("help,h",
        po::value(&show_help)->zero_tokens(),
        "Display this text.")
    ("version,V",
        po::value(&show_version)->zero_tokens(),
        "Shows the version number.");

  optimizations_.add_options()
    ("metadata-cache-size",
        po::value(&metadata_cache_size)->default_value(metadata_cache_size),
        "Number of entries which will be cached."
        "\n(Set to 0 to disable the cache.)")
    ("metadata-cache-ttl-s",
        po::value(&metadata_cache_ttl_s)->default_value(metadata_cache_ttl_s),
        "Time to live after which cached entries will expire.")
    ("enable-async-writes",
        po::value(&enable_async_writes)
          ->default_value(enable_async_writes)->zero_tokens(),
        "Enables asynchronous writes.")
    ("async-writes-max-reqs",
        po::value(&async_writes_max_requests)
            ->implicit_value(async_writes_max_requests),
        "Maximum number of pending write requests per file. Asynchronous writes"
        " will block if this limit is reached first.")
    ("readdir-chunk-size",
        po::value(&readdir_chunk_size)->default_value(readdir_chunk_size),
        "Number of entries requested per readdir.")
    ("object-cache-size",
        po::value(&object_cache_size)->default_value(object_cache_size),
        "Number of cached objects per file.");

  error_handling_.add_options()
    ("max-tries",
        po::value(&max_tries)->default_value(max_tries),
        "Maximum number of attempts to send a request (0 means infinite).")
    ("max-read-tries",
        po::value(&max_read_tries)->default_value(max_read_tries),
        "Maximum number of attempts to execute a read command (0 means infinite"
        ")."
#ifdef __linux
        "\n(If you use Fuse it's not possible to interrupt a read request, i.e."
        " do not set this value too high or to infinite.)"
#endif  // __linux
        )
    ("max-write-tries",
        po::value(&max_write_tries)->default_value(max_write_tries),
        "Maximum number of attempts to execute a write command (0 means "
        "infinite)."
#ifdef __linux
        "\n(Unlike read requests, write requests can get interrupted in "
        "Fuse.)"
#endif  // __linux
        )
    ("retry-delay",
        po::value(&retry_delay_s)->default_value(retry_delay_s),
        "Wait time after a request failed until next attempt (in seconds).")
    ("connect-timeout",
        po::value(&connect_timeout_s)->default_value(connect_timeout_s),
        "Timeout after which a connection attempt will be retried "
        "(in seconds).")
    ("request-timeout",
        po::value(&request_timeout_s)->default_value(request_timeout_s),
        "Timeout after which a request will be retried (in seconds).")
    ("linger-timeout",
        po::value(&linger_timeout_s)->default_value(linger_timeout_s),
        "Time after which idle connections will be closed (in seconds).");

  ssl_options_.add_options()
    ("pem-certificate-file-path",
        po::value(&ssl_pem_cert_path)->default_value(ssl_pem_cert_path),
        "PEM certificate file path")
    ("pem-private-key-file-path",
        po::value(&ssl_pem_key_path)->default_value(ssl_pem_key_path),
        "PEM private key file path")
    ("pem-private-key-passphrase",
        po::value(&ssl_pem_key_pass)->default_value(ssl_pem_key_pass),
        "PEM private key passphrase  (If the argument is set to '-', the user"
        " will be prompted for the passphrase.)")
    ("pkcs12-file-path",
        po::value(&ssl_pkcs12_path)->default_value(ssl_pkcs12_path),
        "PKCS#12 file path")
    ("pkcs12-passphrase",
        po::value(&ssl_pkcs12_pass)->default_value(ssl_pkcs12_pass),
        "PKCS#12 passphrase (If the argument is set to '-', the user will be"
        " prompted for the passphrase.)");

  grid_options_.add_options()
    ("grid-ssl",
        po::value(&grid_ssl)->zero_tokens(),
        "Explicitly use the XtreemFS Grid-SSL mode. Same as specifying "
        "pbrpcg:// in the volume URL.")
    ("globus-gridmap",
        po::value(&grid_auth_mode_globus)->zero_tokens(),
        "Authorize using globus gridmap file.")
    ("unicore-gridmap",
        po::value(&grid_auth_mode_unicore)->zero_tokens(),
        "Authorize using unicore gridmap file.")
    ("gridmap-location",
        po::value(&grid_gridmap_location)->default_value(grid_gridmap_location),
        string("Location of the gridmap file.\n"
        "unicore default: " + grid_gridmap_location_default_unicore + "\n"
        "globus default: " + grid_gridmap_location_default_globus).c_str())
    ("gridmap-reload-interval-m",
        po::value(&grid_gridmap_reload_interval_m)
            ->default_value(grid_gridmap_reload_interval_m),
        "Interval (in minutes) after which the gridmap file will be checked for"
        " changes and reloaded if necessary.");

  vivaldi_options_.add_options()
      ("vivaldi-enable",
          po::value(&vivaldi_enable)->default_value(vivaldi_enable)
            ->zero_tokens(),
          "Enables the vivaldi coordinate calculation for the client.")
      ("vivaldi-enable-dir-updates",
          po::value(&vivaldi_enable_dir_updates)
            ->default_value(vivaldi_enable_dir_updates)->zero_tokens(),
          "Enables sending the coordinates to the DIR after each recalculation."
          " This is only needed to add the clients to the vivaldi visualization"
          " at the cost of some additional traffic between client and DIR.")
      ("vivaldi-filename",
          po::value(&vivaldi_filename)->default_value(vivaldi_filename),
          "The file where the vivaldi coordinates should be saved after each "
          "recalculation.")
      ("vivaldi-recalculation-interval",
          po::value(&vivaldi_recalculation_interval_s)
            ->default_value(vivaldi_recalculation_interval_s),
          "The interval between coordinate recalculations in seconds. "
          "Also see vivaldi-recalculation-epsilon.")
      ("vivaldi-recalculation-epsilon",
          po::value(&vivaldi_recalculation_epsilon_s)
            ->default_value(vivaldi_recalculation_epsilon_s),
          "The recalculation interval will be randomly chosen from"
          " vivaldi-recalculation-inverval +/- vivaldi-recalculation-epsilon "
          "(Both in seconds).")
      ("vivaldi-max-iterations-before-updating",
          po::value(&vivaldi_max_iterations_before_updating)
            ->default_value(vivaldi_max_iterations_before_updating),
          "Number of coordinate recalculations before updating the list of OSDs.")
      ("vivaldi-max-request-retries",
          po::value(&vivaldi_max_request_retries)
            ->default_value(vivaldi_max_request_retries),
          "Maximal number of retries when requesting coordinates from another "
          "vivaldi node.");

  xtreemfs_advanced_options_.add_options()
    ("periodic-filesize-update-interval",
        po::value(&periodic_file_size_updates_interval_s),
        "Pause time (in seconds) between two invocations of the thread which "
        "writes back file size updates to the MRC in the background.")
    ("periodic-xcap-renewal-interval",
        po::value(&periodic_xcap_renewal_interval_s),
        "Pause time (in seconds) between two invocations of the thread which "
        "renews the XCap of all open file handles.")
    ("async-writes-max-reqsize-kb",
        po::value(&async_writes_max_request_size_kb)
            ->implicit_value(async_writes_max_request_size_kb),
        "Maximum size per write request in kB (1 kB = 1024 bytes). Usually the"
        "object size or another system specific upper bound.")
    ("vivaldi-zipf-generator-skew",
        po::value(&vivaldi_zipf_generator_skew)
          ->default_value(vivaldi_zipf_generator_skew),
        "Skewness of the Zipf distribution used for vivaldi OSD selection.")
    ("enable-atime",
        po::value(&enable_atime)->default_value(enable_atime)->zero_tokens(),
        "Enable updates of atime attribute in Fuse and metadata cache.");

  deprecated_options_.add_options()
    ("interrupt-signal",
        po::value<int>()->notifier(MsgOptionHandler<int>(
        "'interrupt-signal' is no longer supported")),
        "DEPRECATED (has no effect) - Retry of a request was interrupted if "
        "this signal was sent in earlier versions."
        );

  // These options are parsed
  all_descriptions_.add(general_).add(optimizations_).add(error_handling_)
      .add(ssl_options_).add(grid_options_).add(vivaldi_options_)
      .add(xtreemfs_advanced_options_).add(deprecated_options_);
  // These options are shown in the "-h" output
  visible_descriptions_.add(general_).add(optimizations_).add(error_handling_)
      .add(ssl_options_).add(grid_options_).add(vivaldi_options_);


  all_descriptions_initialized_ = true;
}

std::vector<std::string> Options::ParseCommandLine(int argc, char** argv) {
  GenerateProgramOptionsDescriptions();

  // Parse command line.
  boost::program_options::variables_map vm;
  try {
    po::store(po::command_line_parser(argc, argv)
        .options(all_descriptions_)
        .allow_unregistered()
        .style(style::default_style & ~style::allow_guessing)
        .run(), vm);
    po::notify(vm);
  } catch(const std::exception& e) {
    // Rethrow boost errors due to invalid command line parameters.
    throw InvalidCommandLineParametersException(string(e.what()));
  }

  po::parsed_options parsed = po::command_line_parser(argc, argv)
  .options(all_descriptions_)
  .allow_unregistered()
  .style(style::default_style & ~style::allow_guessing)
  .run();

  if (metadata_cache_size < readdir_chunk_size && metadata_cache_size != 0) {
    cerr << "Warning: Please set the metadata cache size at least as high as "
            "the readdir chunk size. (Currently: " << metadata_cache_size <<
            " < " << readdir_chunk_size << "). Otherwise you might experience"
            " a degraded performance."
         << endl << endl;
  }

  if (async_writes_max_requests < 1) {
    throw InvalidCommandLineParametersException("The maximum number of pending"
        " asynchronous writes (async-writes-max-reqs) must be greater 0.");
  }

  if (!enable_async_writes && (vm.count("async-writes-max-reqsize-kb") ||
      vm.count("async-writes-max-reqs"))) {
    throw InvalidCommandLineParametersException("You specified async-writes-*"
        " options but did not set enable-async-writes.");
  }

  if (enable_async_writes && object_cache_size > 0) {
    throw InvalidCommandLineParametersException(
        "Only one of async. writes and the object cache may be enabled.");
  }

  // Show help if no arguments given.
  if (argc == 1) {
    empty_arguments_list = true;
  }

  if (grid_auth_mode_globus && grid_auth_mode_unicore) {
    throw InvalidCommandLineParametersException("You can only use a Globus "
        "OR a Unicore gridmap file at the same time.");
  }
  if (grid_auth_mode_globus) {
    additional_user_mapping_type = UserMapping::kGlobus;
    if (grid_gridmap_location.empty()) {
      grid_gridmap_location = grid_gridmap_location_default_globus;
    }
  }
  if (grid_auth_mode_unicore) {
    additional_user_mapping_type = UserMapping::kUnicore;
    if (grid_gridmap_location.empty()) {
      grid_gridmap_location = grid_gridmap_location_default_unicore;
    }
  }

  // PEM certificate _and_ private key are both required.
  if ((!ssl_pem_cert_path.empty() && ssl_pem_key_path.empty()) ||
      (!ssl_pem_key_path.empty() && ssl_pem_cert_path.empty())) {
    throw InvalidCommandLineParametersException(
        "If you use SSL and PEM files, you have to specify both the PEM"
        " certificate and the PEM private key.");
  }

  // PKCS#12 and PEM files are mutually exclusive.
  if (!ssl_pem_key_path.empty() && !ssl_pkcs12_path.empty()) {
    throw InvalidCommandLineParametersException("You can only use PEM files"
        " OR a PKCS#12 certificate. However, you specified both.");
  }

  // PKCS#12 and PEM Private Key password are mutually exclusive.
  if (!ssl_pem_key_pass.empty() && !ssl_pkcs12_pass.empty()) {
    throw InvalidCommandLineParametersException("You can only use PEM files"
        " OR a PKCS#12 certificate. However, you specified the password option"
        " for both.");
  }

  // If a SSL password was given via command line, clean the value from args.
  string to_be_cleaned_password;
  if (!ssl_pem_key_pass.empty() && ssl_pem_key_pass != "-") {
    to_be_cleaned_password = ssl_pem_key_pass;
  }
  if (!ssl_pkcs12_pass.empty() && ssl_pkcs12_pass != "-") {
    to_be_cleaned_password = ssl_pkcs12_pass;
  }
  if (!to_be_cleaned_password.empty()) {
    // Replace the password in all command line arguments. We don't know from
    // which argv[i] it was actually parsed, so we try them all.
    for (int i = 1; i < argc; i++) {
      const string arg(argv[i]);
      if (arg.find(to_be_cleaned_password) != string::npos) {
        memset(argv[i], 0, arg.length());
      }
    }
  }

  // If the passphrase parameter was specified, but not set, mark that the
  // password shall be read from stdin.
  if (!ssl_pem_key_path.empty() && ssl_pem_key_pass == "-") {
    ReadPasswordFromStdin(
        "No PEM private key passphrase was given. Please enter it now:",
        &ssl_pem_key_pass);
  }
  if (!ssl_pkcs12_path.empty() && ssl_pkcs12_pass == "-") {
    ReadPasswordFromStdin(
        "No PKCS#12 certificate passphrase was given. Please enter it now:",
        &ssl_pkcs12_pass);
  }

  // Return unparsed options.
  return po::collect_unrecognized(parsed.options, po::include_positional);
}

void Options::ParseURL(XtreemFSServiceType service_type) {
  int default_port;
  switch(service_type) {
    case kMRC:
      default_port = MRC_PBRPC_PORT_DEFAULT;
      break;
    case kDIR:
    default:
      default_port = DIR_PBRPC_PORT_DEFAULT;
      break;
  }

  PBRPCURL url_parser;
  url_parser.ParseURL(xtreemfs_url, PBRPCURL::SCHEME_PBRPC, default_port);
  volume_name = url_parser.volume();
  service_addresses = url_parser.GetAddresses();
  protocol = url_parser.scheme();
}

std::string Options::ShowCommandLineHelp() {
  GenerateProgramOptionsDescriptions();
  ostringstream stream;
  stream << visible_descriptions_;
  return stream.str();
}

std::string Options::ShowCommandLineHelpVolumeCreationAndDeletion() {
  GenerateProgramOptionsDescriptions();
  ostringstream stream;
  stream << general_ << endl
         << ssl_options_ << endl
         << grid_options_;
  return stream.str();
}

std::string Options::ShowCommandLineHelpVolumeListing() {
  GenerateProgramOptionsDescriptions();
  ostringstream stream;
  stream << general_ << endl
         << ssl_options_;
  return stream.str();
}

std::string Options::ShowVersion(const std::string& component) {
  return component + " " + version_string;
}

bool Options::SSLEnabled() const {
  return !ssl_pem_cert_path.empty() || !ssl_pkcs12_path.empty();
}

xtreemfs::rpc::SSLOptions* Options::GenerateSSLOptions() const {
  xtreemfs::rpc::SSLOptions* opts = NULL;
  if (SSLEnabled()) {
    opts = new xtreemfs::rpc::SSLOptions(
        ssl_pem_key_path, ssl_pem_cert_path, ssl_pem_key_pass,  // PEM.
        ssl_pkcs12_path, ssl_pkcs12_pass,  // PKCS12.
        boost::asio::ssl::context::pem,
        grid_ssl || protocol == PBRPCURL::SCHEME_PBRPCG);
  }

  return opts;
}

void Options::ReadPasswordFromStdin(const std::string& msg,
                                    std::string* password) {
  cout << msg << endl;
  getline(cin, *password);
}

}  // namespace xtreemfs
