/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *               2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_OPTIONS_H_
#define CPP_INCLUDE_LIBXTREEMFS_OPTIONS_H_

#include <stdint.h>

#include <boost/function.hpp>
#include <boost/program_options.hpp>
#include <iostream>
#include <string>
#include <vector>

#include "libxtreemfs/typedefs.h"
#include "libxtreemfs/user_mapping.h"

namespace xtreemfs {

namespace rpc {
class SSLOptions;
}  // namespace rpc

enum XtreemFSServiceType {
  kDIR, kMRC
};

class Options {
 public:
  /** Query function which returns 1 when the request was interrupted.
   *
   * @note the boost::function typedef could be replaced with
   *       typedef int (*query_function)(void);
   *       which would also works without changes, but would not support
   *       functor objects
   */
  typedef boost::function0<int> CheckIfInterruptedQueryFunction;

  /** Sets the default values. */
  Options();

  virtual ~Options() {}

  /** Generates boost::program_options description texts. */
  void GenerateProgramOptionsDescriptions();

  /** Set options parsed from command line.
   *
   * However, it does not set dir_volume_url and does not call
   * ParseVolumeAndDir().
   *
   * @throws InvalidCommandLineParametersException
   * @throws InvalidURLException */
  std::vector<std::string> ParseCommandLine(int argc, char** argv);

  /** Extract volume name and dir service address from dir_volume_url. */
  void ParseURL(XtreemFSServiceType service_type);

  /** Outputs usage of the command line parameters of all options. */
  virtual std::string ShowCommandLineHelp();

  /** Outputs usage of the command line parameters of volume creation
   *  relevant options. */
  std::string ShowCommandLineHelpVolumeCreationAndDeletion();

  /** Outputs usage of the command line parameters of volume deletion/listing
   *  relevant options. */
  std::string ShowCommandLineHelpVolumeListing();

  /** Returns the version string and prepends "component". */
  std::string ShowVersion(const std::string& component);

  /** Returns true if required SSL options are set. */
  bool SSLEnabled() const;

  /** Creates a new SSLOptions object based on the value of the members:
   *  - ssl_pem_key_path
   *  - ssl_pem_cert_path
   *  - ssl_pem_key_pass
   *  - ssl_pem_trusted_certs_path
   *  - ssl_pkcs12_path
   *  - ssl_pkcs12_pass
   *  - grid_ssl || protocol
   *  - verify_certificates
   *  - ignore_verify_errors
   *  - ssl_method
   *
   * @remark Ownership is transferred to caller. May be NULL.
   */
  xtreemfs::rpc::SSLOptions* GenerateSSLOptions() const;

  // Version information.
  std::string version_string;

  // XtreemFS URL Options.
  /** URL to the Volume.
   *
   * Format:[pbrpc://]service-hostname[:port](,[pbrpc://]service-hostname2[:port])*[/volume_name].  // NOLINT
   *
   * Depending on the type of operation the service-hostname has to point to the
   * DIR (to open/"mount" a volume) or the MRC (create/delete/list volumes).
   * Depending on this type, the default port differs (DIR: 32638; MRC: 32636).
   */
  std::string xtreemfs_url;
  /** Usually extracted from xtreemfs_url (Form: ip-address:port).
   *
   * Depending on the application, it may contain the addresses of DIR replicas
   * (e.g., mount.xtreemfs) or MRC replicas (e.g., mkfs.xtreemfs). */
  ServiceAddresses service_addresses;
  /** Usually extracted from xtreemfs_url. */
  std::string volume_name;
  /** Usually extracted from xtreemfs_url. */
  std::string protocol;
  /** Mount point on local system (set by ParseCommandLine()). */
  std::string mount_point;

  // General options.
  /** Log level as string (EMERG|ALERT|CRIT|ERR|WARNING|NOTICE|INFO|DEBUG). */
  std::string log_level_string;
  /** If not empty, the output will be logged to a file. */
  std::string log_file_path;
  /** True, if "-h" was specified. */
  bool show_help;
  /** True, if argc == 1 was at ParseCommandLine(). */
  bool empty_arguments_list;
  /** True, if -V/--version was specified and the version will be shown only .*/
  bool show_version;

  // Optimizations.
  /** Maximum number of entries of the StatCache */
  uint64_t metadata_cache_size;
  /** Time to live for MetadataCache entries. */
  uint64_t metadata_cache_ttl_s;
  /** Enable asynchronous writes */
  bool enable_async_writes;
  /** Maximum number of pending async write requests per file. */
  int async_writes_max_requests;
  /** Maximum write request size per async write. Should be equal to the lowest
   *  upper bound in the system (e.g. an object size, or the FUSE limit). */
  int async_writes_max_request_size_kb;
  /** Number of retrieved entries per readdir request. */
  int readdir_chunk_size;
  /** True, if atime requests are enabled in Fuse/not ignored by the library. */
  bool enable_atime;

  // Error Handling options.
  /** How often shall a failed operation get retried? */
  int max_tries;
  /** How often shall a failed read operation get retried? */
  int max_read_tries;
  /** How often shall a failed write operation get retried? */
  int max_write_tries;
  /** How often shall a view be tried to renewed? */
  int max_view_renewals;
  /** How long to wait after a failed request at least? */
  int retry_delay_s;
  /** Maximum time until a connection attempt will be aborted. */
  int32_t connect_timeout_s;
  /** Maximum time until a request will be aborted and the response returned. */
  int32_t request_timeout_s;
  /** The RPC Client closes connections after "linger_timeout_s" time of
   *  inactivity. */
  int32_t linger_timeout_s;

#ifdef HAS_OPENSSL
  // SSL options.
  std::string ssl_pem_cert_path;
  std::string ssl_pem_key_path;
  std::string ssl_pem_key_pass;
  std::string ssl_pem_trusted_certs_path;
  std::string ssl_pkcs12_path;
  std::string ssl_pkcs12_pass;
  /** True, if the XtreemFS Grid-SSL Mode (only SSL handshake, no encryption of
   *  data itself) shall be used. */
  bool grid_ssl;

  /** True if certificates shall be verified. */
  bool ssl_verify_certificates;
  /** List of openssl verify error codes to ignore during verification and
   * accept anyway. Only used when ssl_verify_certificates = true. */
  std::vector<int> ssl_ignore_verify_errors;
  
  /** SSL version that this client should accept. */
  std::string ssl_method_string;
#endif  // HAS_OPENSSL

  // Grid Support options.
  /** True if the Globus user mapping shall be used. */
  bool grid_auth_mode_globus;
  /** True if the Unicore user mapping shall be used. */
  bool grid_auth_mode_unicore;
  /** Location of the gridmap file. */
  std::string grid_gridmap_location;
  /** Default Location of the Globus gridmap file. */
  std::string grid_gridmap_location_default_globus;
  /** Default Location of the Unicore gridmap file. */
  std::string grid_gridmap_location_default_unicore;
  /** Periodic interval after which the gridmap file will be reloaded. */
  int grid_gridmap_reload_interval_m;

  // Vivaldi Options
  /** Enables the vivaldi coordinate calculation for the client. */
  bool vivaldi_enable;
  /** Enables sending the coordinates to the DIR after each recalculation. This
   *  is only needed to add the clients to the vivaldi visualization at the cost
   *  of some additional traffic between client and DIR.") */
  bool vivaldi_enable_dir_updates;
  /** The file where the vivaldi coordinates should be saved after each
   *  recalculation. */
  std::string vivaldi_filename;
  /** The interval between coordinate recalculations. Also see
   *  vivaldi_recalculation_epsilon_s. */
  int vivaldi_recalculation_interval_s;
  /** The recalculation interval will be randomly chosen from
   *  vivaldi_recalculation_inverval_s +/- vivaldi_recalculation_epsilon_s */
  int vivaldi_recalculation_epsilon_s;
  /** Number of coordinate recalculations before updating the list of OSDs. */
  int vivaldi_max_iterations_before_updating;
  /** Maximal number of retries when requesting coordinates from another
   *  vivaldi node. */
  int vivaldi_max_request_retries;

  // Advanced XtreemFS options.
  /** Interval for periodic file size updates in seconds. */
  int periodic_file_size_updates_interval_s;
  /** Interval for periodic xcap renewal in seconds. */
  int periodic_xcap_renewal_interval_s;
  /** Skewness of the Zipf distribution used for vivaldi OSD selection */
  double vivaldi_zipf_generator_skew;

  /** May contain all previous options in key=value pair lists. */
  std::vector<std::string> alternative_options_list;

  // Internal options, not available from the command line interface.
  /** If not NULL, called to find out if request was interrupted. */
  CheckIfInterruptedQueryFunction was_interrupted_function;

  // NOTE: Deprecated options are no longer needed as members

  // Additional User mapping.
  /** Type of the UserMapping used to translate between local/global names. */
  UserMapping::UserMappingType additional_user_mapping_type;

 private:
  /** Reads password from stdin and stores it in 'password'. */
  void ReadPasswordFromStdin(const std::string& msg, std::string* password);

  /** This functor template can be used as argument for the notifier() method
   *  of boost::options. It is specifically used to create a warning whenever
   *  a deprecated option is used, but is not limited to that purpose.
   *  The CreateMsgOptionHandler function template can be used to instantiate it
   *  without explicit template type specification. Instead the type inferred
   *  from the value given by the corresponding member variable.
   */
  template<typename T>
  class MsgOptionHandler {
   public:
    typedef void result_type;
    MsgOptionHandler(std::string msg)
     : msg_(msg) { }
    void operator()(const T& value) {
      std::cerr << "Warning: Deprecated option used: " << msg_ << std::endl;
    }
   private:
    const std::string msg_;
  };

  /** See MsgOptionHandler */
  template<typename T>
  MsgOptionHandler<T> CreateMsgOptionHandler(const T&, std::string msg) {
    return MsgOptionHandler<T>(msg);
  }

  // Sums of options.
  /** Contains all boost program options, needed for parsing. */
  boost::program_options::options_description all_descriptions_;

  /** Contains descriptions of all visible options (no advanced and
   *  deprecated options). Used by ShowCommandLineHelp().*/
  boost::program_options::options_description visible_descriptions_;

  /** Set to true if GenerateProgramOptionsDescriptions() was executed. */
  bool all_descriptions_initialized_;

  // Options itself.
  /** Description of general options (Logging, help). */
  boost::program_options::options_description general_;

  /** Description of options which improve performance. */
  boost::program_options::options_description optimizations_;

  /** Description of timeout options etc. */
  boost::program_options::options_description error_handling_;

#ifdef HAS_OPENSSL
  /** Description of SSL related options. */
  boost::program_options::options_description ssl_options_;
#endif  // HAS_OPENSSL

  /** Description of options of the Grid support. */
  boost::program_options::options_description grid_options_;

  /** Description of the Vivaldi options */
  boost::program_options::options_description vivaldi_options_;

  // Hidden options.
  /** Description of options of the Grid support. */
  boost::program_options::options_description xtreemfs_advanced_options_;

  /** Deprecated options which are kept to ensure backward compatibility. */
  boost::program_options::options_description deprecated_options_;

  /** Specify all previous options in key=value pair lists. */
  boost::program_options::options_description alternative_options_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_OPTIONS_H_
