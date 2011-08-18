/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "fuse/fuse_adapter.h"

#include <csignal>
#include <cstring>
#define FUSE_USE_VERSION 26
#include <fuse.h>
#include <sys/errno.h>
#include <sys/types.h>
#include <unistd.h>

#include <algorithm>
#include <boost/cstdint.hpp>
#include <boost/lexical_cast.hpp>
#include <fstream>
#include <list>
#include <string>

#include "fuse/cached_directory_entries.h"
#include "fuse/fuse_options.h"
#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/user_mapping.h"
#include "libxtreemfs/uuid_resolver.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"
#include "util/error_log.h"
#include "xtreemfs/MRC.pb.h"
#include "xtreemfs/OSD.pb.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {

FuseAdapter::FuseAdapter(FuseOptions* options) :
    options_(options),
    xctl_("/.xctl$$$")
{}

FuseAdapter::~FuseAdapter() {}

void FuseAdapter::Start(std::list<char*>* required_fuse_options) {
  // Set interrupt signal to SIGINT to allow the interruption of mount.xtreemfs.
  int preserved_interrupt_signal = options_->interrupt_signal;
  options_->interrupt_signal = SIGINT;

  // Start logging manually (altough it would be automatically started by
  // ClientImplementation()) as its required by UserMapping.
  initialize_logger(options_->log_level_string,
                    options_->log_file_path,
                    LEVEL_WARN);

#ifdef __APPLE__
  // If the system is newer than Tiger, reduce the number of retries and warn
  // the user about it.
  if (GetMacOSXKernelVersion() >= 9) {
    // At least 3 attempts should be possible. Reduce the timeout if necessary.
    int min_tries = 3;
    int max_tries = max(max(options_->max_tries, options_->max_read_tries),
                        options_->max_write_tries);
    if (options_->max_tries == 0 || options_->max_read_tries == 0 ||
        options_->max_write_tries == 0) {
      max_tries = 0;
    }
    int max_delay = max(max(options_->connect_timeout_s,
                            options_->request_timeout_s),
                        options_->retry_delay_s);
    // -1 seconds required because Fuse gets killed at daemon_timeout sharp.
    int allowed_max_timeout = (options_->daemon_timeout - 1);
    bool timeouts_reduced = false;

    if (max_tries == 0 || ((max_delay * max_tries) > allowed_max_timeout)) {
      // Reduce the number of tries.
      timeouts_reduced = true;
      max_tries = max(min_tries, allowed_max_timeout / max_delay);

      // If it did not work, also reduce the timeouts.
      if ((options_->retry_delay_s * max_tries) > allowed_max_timeout) {
        options_->retry_delay_s = max(1, allowed_max_timeout / max_tries);
      }
      if ((options_->connect_timeout_s * max_tries) > allowed_max_timeout) {
        options_->connect_timeout_s = max(1, allowed_max_timeout / max_tries);
      }

      // Overwrite read and write tries values by max_tries.
      options_->max_tries = max_tries;
      options_->max_read_tries = max_tries;
      options_->max_write_tries = max_tries;
    }

    // TODO(mberlin): Special handling of the request timeout required.
    //                Due to a limitation in the RPC client, the actual
    //                request timeout may be twice the time.
    string info_request_timeout_bug = "";
    if ((2 * options_->request_timeout_s * max_tries) > allowed_max_timeout) {
      options_->request_timeout_s = max(1, allowed_max_timeout / max_tries / 2);
      timeouts_reduced = true;
      info_request_timeout_bug = " (the actual value had to be halved due to a"
          " limitation in the current code)";
    }

    if (timeouts_reduced) {
      if (Logging::log->loggingActive(LEVEL_WARN)) {
        Logging::log->getLog(LEVEL_WARN)
            << "You are running MacOSX Leopard or newer. The timeout values"
               " and/or maximum number of attempts had to be reduced as follows"
               " to avoid unwanted terminations of mount.xtreemfs."
               "\n\n"
               "As the Finder blocks in case of stall operations (which may"
               " happen, for instance if a file replica went offline and the"
               " client has to retry the next one), MacFuse does terminate the"
               " blocking file system after the period specified by -o"
               " daemon_timeout=XX where XX defaults to 60 seconds for MacOSX"
               " versions newer than Tiger. You can increase this value by"
               " passing -o daemon_timeout=XX to mount.xtreemfs."
               "\n\n"
            << "Based on a daemon_timeout value of " << options_->daemon_timeout
            << ", the options were adjusted as follows:"
            << "\nmax-tries: " << options_->max_tries
            << "\nmax-read-tries: " << options_->max_read_tries
            << "\nmax-write-tries: " << options_->max_write_tries
            << "\nretry_delay: " << options_->retry_delay_s
            << "\nconnect-timeout: " << options_->connect_timeout_s
            << "\nrequest-timeout: " << options_->request_timeout_s
            << info_request_timeout_bug << endl;
      }
    }
  }
#endif  // __APPLE__

  // Setup Usermapping.
  user_mapping_.reset(UserMapping::CreateUserMapping(
      options_->user_mapping_type, UserMapping::kUnix, *options_));
  user_mapping_->Start();

  // Create new Client.
  UserCredentials client_user_credentials;
  GenerateUserCredentials(NULL, &client_user_credentials);
  client_.reset(Client::CreateClient(options_->service_address,
                                     client_user_credentials,
                                     options_->GenerateSSLOptions(),
                                     *options_,
                                     Client::kDefaultClient));
  client_->Start();
  // Open Volume.
  volume_ = client_->OpenVolume(options_->volume_name,
                                options_->GenerateSSLOptions(),
                                *options_);

  xctl_.set_volume(volume_);

  // Try to access Volume. If it fails, an error will be thrown.
  Stat stat;
  volume_->GetAttr(client_user_credentials, "/", &stat);

  // Check the attributes of the Volume.
  string mrc_uuid;
  client_->GetUUIDResolver()->VolumeNameToMRCUUID(options_->volume_name,
                                                  &mrc_uuid);
  string mrc_address;
  client_->GetUUIDResolver()->UUIDToAddress(mrc_uuid, &mrc_address);
  boost::scoped_ptr<Volumes> volumes(client_->ListVolumes(mrc_address));
  for (int i = 0; i < volumes->volumes_size(); i++) {
    // Found volume?
    if (volumes->volumes(i).name() == options_->volume_name) {
      // Enable Fuse POSIX checks only if the POSIX policy is set.
      if (volumes->volumes(i).access_control_policy() !=
          ACCESS_CONTROL_POLICY_POSIX) {
        options_->use_fuse_permission_checks = false;
        // Tell the user.
        Logging::log->getLog(LEVEL_INFO) << "Disabled Fuse POSIX checks (i. e."
            "not passing -odefault_permissions to Fuse) because the access "
            "policy is not set to ACCESS_CONTROL_POLICY_POSIX" << endl;
      }

      // Check attributes.
      UserMapping::UserMappingType current_user_mapping_type =
          options_->user_mapping_type;
      for (int j = 0; j < volumes->volumes(i).attrs_size(); j++) {
        // If type of gridmap file not explicitly specified, use information
        // from volume attributes.
        if (!options_->grid_auth_mode_globus &&
            !options_->grid_auth_mode_unicore) {
          if (volumes->volumes(i).attrs(j).key() == "globus_gridmap") {
            options_->grid_auth_mode_globus = true;
            options_->user_mapping_type = UserMapping::kGlobus;
            if (options_->grid_gridmap_location.empty()) {
              options_->grid_gridmap_location =
                  options_->grid_gridmap_location_default_globus;
            }
            Logging::log->getLog(LEVEL_INFO) << "Using Globus gridmap file "
                << options_->grid_gridmap_location << endl;
            break;
          }

          if (volumes->volumes(i).attrs(j).key() == "unicore_uudb") {
            options_->grid_auth_mode_unicore = true;
            options_->user_mapping_type = UserMapping::kUnicore;
            if (options_->grid_gridmap_location.empty()) {
              options_->grid_gridmap_location =
                  options_->grid_gridmap_location_default_unicore;
            }
            Logging::log->getLog(LEVEL_INFO) << "Using Unicore uudb file "
                << options_->grid_gridmap_location << endl;
            break;
          }
        }
      }
      // Reset user mapping if it has to be changed.
      if (current_user_mapping_type != options_->user_mapping_type) {
        user_mapping_->Stop();
        user_mapping_.reset(UserMapping::CreateUserMapping(
            options_->user_mapping_type,
            UserMapping::GetUserMappingSystemType(),
            *options_));
        user_mapping_->Start();
      }

      // Skip remaining volumes.
      break;
    }
  }

  // Restore original signal.
  options_->interrupt_signal = preserved_interrupt_signal;

  // Also do not enable Fuse's POSIX checks, if the Globus or Unicore
  // mode is active, because User Certificates may be used by the under-
  // lying SSL connection. If that's the case, the MRC will ignore the
  // UserCredentials field and use the DN from the certificate instead.
  // In this case Fuse POSIX checks cannot get applied.
  if (options_->grid_auth_mode_globus || options_->grid_auth_mode_unicore) {
    options_->use_fuse_permission_checks = false;
    // Tell the user.
    Logging::log->getLog(LEVEL_INFO) << "Disabled Fuse POSIX checks (i. e."
        "not passing -odefault_permissions to Fuse) because a Grid usermapping "
        "is used." << endl;
  }

  // Add Fuse default options.
  if (options_->interrupt_signal) {
    required_fuse_options->push_back(strdup("-ointr"));
    string signal = string("-ointr_signal=")
                    + boost::lexical_cast<string>(options_->interrupt_signal);
    required_fuse_options->push_back(strdup(signal.c_str()));
  }
  if (options_->use_fuse_permission_checks) {
    required_fuse_options->push_back(strdup("-odefault_permissions"));
  }
#ifdef __APPLE__
  // Add fancy icon and set volume name
  required_fuse_options->push_back(strdup(
      (string("-ovolname=\"XtreemFS (") + options_->xtreemfs_url
       + string(")\"")).c_str()));
  // Check if icon file exists.
  {
    string default_xtreemfs_icon_path
        = "/usr/local/share/xtreemfs/xtreemfs_logo_transparent.icns";
    ifstream icon_file(default_xtreemfs_icon_path.c_str());
    if (icon_file.good()) {
      required_fuse_options->push_back(strdup(
          (string("-ovolicon=") + default_xtreemfs_icon_path).c_str()));
    }
  }
#endif  // __APPLE__
#ifdef __linux
  #if FUSE_MAJOR_VERSION > 2 || \
      ( FUSE_MAJOR_VERSION == 2 && FUSE_MINOR_VERSION >= 8 )
  required_fuse_options->push_back(strdup("-obig_writes"));
  #endif  // Fuse >= 2.8
#endif  // __linux
  // Unfortunately Fuse does also cache the stat entries of hard links and
  // therefore returns incorrect results if hard links are "chained".
  // In consequence, we have to disable the Fuse stat cache at all.
  required_fuse_options->push_back(strdup("-oattr_timeout=0"));
  required_fuse_options->push_back(
      strdup("-ouse_ino,readdir_ino,noatime"));
  required_fuse_options->push_back(strdup(
      (string("-ofsname=xtreemfs@") + options_->xtreemfs_url).c_str()));
}

void FuseAdapter::Stop() {
  // Close UserMapping.
  user_mapping_->Stop();

  // Shutdown() Client. That does also invoke a volume->Close().
  client_->Shutdown();
}

void FuseAdapter::GenerateUserCredentials(
    struct fuse_context* fuse_context,
    xtreemfs::pbrpc::UserCredentials* user_credentials) {
  // No fuse_context known, use information of current process.
  if (fuse_context == NULL) {
    GenerateUserCredentials(getuid(), getgid(), getpid(), user_credentials);
  } else {
    GenerateUserCredentials(fuse_context->uid,
                            fuse_context->gid,
                            fuse_context->pid,
                            user_credentials);
  }
}

void FuseAdapter::GenerateUserCredentials(
    uid_t uid,
    gid_t gid,
    pid_t pid,
    xtreemfs::pbrpc::UserCredentials* user_credentials) {
  user_credentials->set_username(user_mapping_->UIDToUsername(uid));

  list<string> groupnames;
  user_mapping_->GetGroupnames(uid, gid, pid, &groupnames);
  for (list<string>::iterator it = groupnames.begin();
       it != groupnames.end(); ++it) {
    user_credentials->add_groups(*it);
  }
}

void FuseAdapter::ConvertXtreemFSStatToFuse(
    const xtreemfs::pbrpc::Stat& xtreemfs_stat, struct stat* fuse_stat) {
  fuse_stat->st_dev = xtreemfs_stat.dev();
  fuse_stat->st_blksize = 8 * 128 * 1024;
  fuse_stat->st_ino = xtreemfs_stat.ino();  // = fileId
  fuse_stat->st_mode = xtreemfs_stat.mode();
  fuse_stat->st_nlink = xtreemfs_stat.nlink();

  // Map user- and groupnames.
  fuse_stat->st_uid = user_mapping_->UsernameToUID(xtreemfs_stat.user_id());
  fuse_stat->st_gid = user_mapping_->GroupnameToGID(xtreemfs_stat.group_id());

  fuse_stat->st_size = xtreemfs_stat.size();
#ifdef __linux
  fuse_stat->st_atim.tv_sec  = xtreemfs_stat.atime_ns() / 1000000000;
  fuse_stat->st_atim.tv_nsec = xtreemfs_stat.atime_ns() % 1000000000;
  fuse_stat->st_mtim.tv_sec  = xtreemfs_stat.mtime_ns() / 1000000000;
  fuse_stat->st_mtim.tv_nsec = xtreemfs_stat.mtime_ns() % 1000000000;
  fuse_stat->st_ctim.tv_sec  = xtreemfs_stat.ctime_ns() / 1000000000;
  fuse_stat->st_ctim.tv_nsec = xtreemfs_stat.ctime_ns() % 1000000000;
#elif __APPLE__
  fuse_stat->st_atimespec.tv_sec  = xtreemfs_stat.atime_ns() / 1000000000;
  fuse_stat->st_atimespec.tv_nsec = xtreemfs_stat.atime_ns() % 1000000000;
  fuse_stat->st_mtimespec.tv_sec  = xtreemfs_stat.mtime_ns() / 1000000000;
  fuse_stat->st_mtimespec.tv_nsec = xtreemfs_stat.mtime_ns() % 1000000000;
  fuse_stat->st_ctimespec.tv_sec  = xtreemfs_stat.ctime_ns() / 1000000000;
  fuse_stat->st_ctimespec.tv_nsec = xtreemfs_stat.ctime_ns() % 1000000000;
#endif

  fuse_stat->st_rdev = 0;
  fuse_stat->st_blocks = 0;
}

xtreemfs::pbrpc::SYSTEM_V_FCNTL FuseAdapter::ConvertFlagsUnixToXtreemFS(
    int flags) {
  int result = 0;

#define CHECK(result, flags, unix, proto) { \
  if ((flags & unix) != 0) result |= proto; \
}
  CHECK(result, flags, O_RDONLY   , SYSTEM_V_FCNTL_H_O_RDONLY);
  CHECK(result, flags, O_WRONLY   , SYSTEM_V_FCNTL_H_O_WRONLY);
  CHECK(result, flags, O_RDWR     , SYSTEM_V_FCNTL_H_O_RDWR);
  CHECK(result, flags, O_APPEND   , SYSTEM_V_FCNTL_H_O_APPEND);
  CHECK(result, flags, O_CREAT    , SYSTEM_V_FCNTL_H_O_CREAT);
  CHECK(result, flags, O_TRUNC    , SYSTEM_V_FCNTL_H_O_TRUNC);
  CHECK(result, flags, O_EXCL     , SYSTEM_V_FCNTL_H_O_EXCL);
#ifdef __linux
  CHECK(result, flags, O_DSYNC    , SYSTEM_V_FCNTL_H_O_SYNC);
#endif

  return xtreemfs::pbrpc::SYSTEM_V_FCNTL(result);
}

int FuseAdapter::ConvertXtreemFSErrnoToFuse(
    xtreemfs::pbrpc::POSIXErrno xtreemfs_errno) {
  switch (xtreemfs_errno) {
    case POSIX_ERROR_EPERM:
      return EPERM;
    case POSIX_ERROR_ENOENT:
      return ENOENT;
    case POSIX_ERROR_EINTR:
      return EINTR;
    case POSIX_ERROR_EIO:
      return EIO;
    case POSIX_ERROR_EAGAIN:
      return EAGAIN;
    case POSIX_ERROR_EACCES:
      return EACCES;
    case POSIX_ERROR_EEXIST:
      return EEXIST;
    case POSIX_ERROR_EXDEV:
      return EXDEV;
    case POSIX_ERROR_ENODEV:
      return ENODEV;
    case POSIX_ERROR_ENOTDIR:
      return ENOTDIR;
    case POSIX_ERROR_EISDIR:
      return EISDIR;
    case POSIX_ERROR_EINVAL:
      return EINVAL;
    case POSIX_ERROR_ENOTEMPTY:
      return ENOTEMPTY;
    case POSIX_ERROR_ENODATA:
      return ENODATA;

    default:
      return xtreemfs_errno;
  }
}

int FuseAdapter::statfs(const char *path, struct statvfs *statv) {
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    boost::scoped_ptr<StatVFS> stat_vfs(
            volume_->StatFS(user_credentials));

    // According to Fuse.h: "The 'f_frsize', 'f_favail', 'f_fsid' and 'f_flag'
    //                       fields are ignored"
    statv->f_bsize   = stat_vfs->bsize();  // file system block size
    statv->f_bfree   = stat_vfs->bavail();  // # free blocks
    // # free blocks for unprivileged users
    statv->f_bavail  = stat_vfs->bavail();
    statv->f_files   = 2048;  // # inodes (we use here a bogus number)
    // Total number of blocks in file system.
    statv->f_blocks  = stat_vfs->blocks();
    statv->f_ffree   = 2048;  // # free inodes (we use here a bogus number)
    statv->f_namemax = stat_vfs->namemax();  // maximum filename length
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
        + string(string(e.what())));
    return -1 * EIO;
  }

  return 0;
}

int FuseAdapter::getattr(const char *path, struct stat *statbuf) {
  const string path_str(path);
  if (!xctl_.checkXctlFile(path_str)) {
    Stat stat;
    UserCredentials user_credentials;
    GenerateUserCredentials(fuse_get_context(), &user_credentials);

    try {
      volume_->GetAttr(user_credentials, path_str, &stat);
    } catch(const PosixErrorException& e) {
      return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
    } catch(const XtreemFSException& e) {
      return -1 * EIO;
    } catch(const exception& e) {
      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
          + string(e.what()));
      return -1 * EIO;
    }

    ConvertXtreemFSStatToFuse(stat, statbuf);
    return 0;
  } else {
    fuse_context* ctx = fuse_get_context();
    return xctl_.getattr(ctx->uid, ctx->gid, path_str, statbuf);
  }
}

/** Returns the size of a value of an extended attribute (if size == 0) or fills
 *  "value" with the value of this attribute if it's size does not exceed "size"
 *  - 1 (due to null termination). */
int FuseAdapter::getxattr(
    const char *path, const char *name, char *value, size_t size) {
  bool xtreemfs_attribute_requested = !strncmp(name, "xtreemfs.", 9);
  if (!options_->enable_xattrs && !xtreemfs_attribute_requested) {
    return -1 * ENOTSUP;
  }

  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    if (size == 0) {
      int result = 0;
      if (volume_->GetXAttrSize(user_credentials, string(path),
          string(name), &result)) {
        return result;
      } else {
#ifdef __linux
        return -1 * ENODATA;  // Linux has no ENOATTR.
#else
        return -1 * ENOATTR;
#endif
      }
    } else {
      string value_string;
      if (volume_->GetXAttr(user_credentials, string(path),
                            string(name), &value_string)) {
        if (value_string.size() <= size) {
          // XAttrs are actually binary data and do not require a
          // null-terminating character.
          memcpy(value, value_string.c_str(), value_string.size());
          return value_string.size();
        } else {
          return -1 * ERANGE;
        }
      } else {
#ifdef __linux
        return -1 * ENODATA;  // Linux has no ENOATTR.
#else
        return -1 * ENOATTR;
#endif
      }
    }
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

/* Creates a CachedDirectoryEntries struct on the heap which can hold the
 * cached directory entries and provides a mutex in order serialize the access
 * to this struct. Will be released on releasedir().
 */
int FuseAdapter::opendir(const char *path, struct fuse_file_info *fi) {
  // No default POSIX permissions: Check if it's allowed to enter the dir.
  if (!options_->use_fuse_permission_checks) {
    UserCredentials user_credentials;
    GenerateUserCredentials(fuse_get_context(), &user_credentials);

    // TODO(mberlin): Wait for change of access method and check for X_OK.
    try {
      volume_->Access(user_credentials,
                      string(path),
                      ACCESS_FLAGS_R_OK);
    } catch(const PosixErrorException& e) {
      return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
    } catch(const XtreemFSException& e) {
      return -1 * EIO;
    } catch(const exception& e) {
      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
                + string(e.what()));
      return -1 * EIO;
    }
  }

  CachedDirectoryEntries* cached_direntries = new CachedDirectoryEntries;
  cached_direntries->dir_entries = NULL;
  // @note The uint64_t cast is needed as Fuse does use a uint64_t instead of
  //       a void* to store a pointer.
  fi->fh = reinterpret_cast<uint64_t>(cached_direntries);

  return 0;
}

/**
 *  Fuse's filler() method called with offset=0 returns 1 only if an error
 *  happened. In practice, this allows to fill complete directories with one
 *  readdir() call. (According to the Fuse 2.8.5 source code, it will stop if
 *  the complete buffer exceeded 2 GiB.)
 *
 *  Called with (offset != 0), filler returns 1 after filling the initial 4k
 *  buffer (observed in Fuse 2.8.5). As entries are aligned to 32 Byte
 *  boundaries, a maximum of 128 entries (or less if filenames are very long?)
 *  is possible.
 *  However, the libxtreemfs default readdir chunksize is 1024 entries.
 *  Therefore, we temporary store unprocessed items in fi->fh.
 *  Another reason why we cache the directory entries is due to the way Fuse
 *  detects no more readdir() calls are needed:
 *  If filler returns with 1 due to a full buffer, Fuse will call readdir()
 *  again with the new offset value. If this execution of readdir does not call
 *  filler() again, Fuse knows the directory was completely read. In order to
 *  know if you don't have to call filler again, we need the cached directory
 *  entries.
 *
 *  @attention If this function is executed with offset>0, duplicate or missing
 *             entries may show up in case files were created or deleted during
 *             two readdir calls.
 */
int FuseAdapter::readdir(const char *path, void *buf, fuse_fill_dir_t filler,
    off_t offset, struct fuse_file_info *fi) {
  DirectoryEntries* dir_entries = NULL;
  boost::uint64_t dir_entries_offset = 0;

  // Look up if there are some unprocessed directory entries.
  CachedDirectoryEntries* cached_direntries
      = reinterpret_cast<CachedDirectoryEntries*>(fi->fh);
  // Struct was created at opendir and deleted at releasedir().
  assert(cached_direntries != NULL);
  boost::mutex::scoped_lock lock(cached_direntries->mutex);

  // Use cached entries first if applicable
  if (cached_direntries->dir_entries != NULL
      && cached_direntries->offset <= offset) {
    dir_entries = cached_direntries->dir_entries;
    // It may happen that the offset of the last cached entry is below the
    // requested "offset". This is fine, because filler() won't get called and
    // Fuse knows to stop executing further readdir()s.
    dir_entries_offset = cached_direntries->offset;
  }

  // Fetch entries from MRC.
  if (dir_entries == NULL) {
    UserCredentials user_credentials;
    GenerateUserCredentials(fuse_get_context(), &user_credentials);

    try {
      // libxtreemfs itself may have cached the readdir response, too.
      dir_entries = volume_->ReadDir(user_credentials,
                                     string(path),
                                     offset,
                                     options_->readdir_chunk_size,
                                     false);
      dir_entries_offset = offset;
    } catch(const PosixErrorException& e) {
      return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
    } catch(const XtreemFSException& e) {
      return -1 * EIO;
    } catch(const exception& e) {
      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
                + string(e.what()));
      return -1 * EIO;
    }
  }

  // The directory entries must start at least at "offset".
  assert(dir_entries_offset <= offset);

  // Let Fuse fill the buffers.
  struct stat fuse_statbuf;
  memset(&fuse_statbuf, 0, sizeof(fuse_statbuf));

  int i;
  for (i = offset; i < dir_entries_offset + dir_entries->entries_size(); i++) {
    boost::uint64_t dir_entries_index = i -  dir_entries_offset;
    assert(dir_entries->entries(dir_entries_index).has_stbuf());
    // Only set here st_ino and st_mode for the struct dirent.
    fuse_statbuf.st_ino
        = dir_entries->entries(dir_entries_index).stbuf().ino();
    fuse_statbuf.st_mode
        = dir_entries->entries(dir_entries_index).stbuf().mode();

    if (filler(buf,
               dir_entries->entries(dir_entries_index).name().c_str(),
               &fuse_statbuf,
               i + 1)) {
      break;
    }
  }

  bool chunk_completely_read
      = (dir_entries_offset + dir_entries->entries_size() == i);
  bool definetely_last_chunk
      = (dir_entries->entries_size() < options_->readdir_chunk_size);
  bool no_filler_called
      = (offset == dir_entries_offset + dir_entries->entries_size());

  // Fuse did read all directory entries or this was not the last chunk
  // => don't cache it.
  if (no_filler_called || (!definetely_last_chunk && chunk_completely_read)) {
    delete dir_entries;
    if (cached_direntries->dir_entries == dir_entries) {
      cached_direntries->dir_entries = NULL;
    }
  } else {
    // Cache directory entries (if not already cached).
    if (cached_direntries->dir_entries != dir_entries) {
      // Free previously cached entries.
      if (cached_direntries->dir_entries != NULL) {
        delete cached_direntries->dir_entries;
        cached_direntries->dir_entries = NULL;
      }

      cached_direntries->dir_entries = dir_entries;
      cached_direntries->offset = dir_entries_offset;
    }
  }

  return 0;
}

int FuseAdapter::releasedir(const char *path, struct fuse_file_info *fi) {
  CachedDirectoryEntries* cached_direntries
      = reinterpret_cast<CachedDirectoryEntries*>(fi->fh);
  assert(cached_direntries != NULL);
  delete cached_direntries->dir_entries;
  delete cached_direntries;
  fi->fh = NULL;

  return 0;
}

int FuseAdapter::utime(const char *path, struct utimbuf *ubuf) {
  Stat stat;
  InitializeStat(&stat);
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  // Convert seconds to nanoseconds.
  if (ubuf != NULL) {
    stat.set_atime_ns(static_cast<boost::uint64_t>(ubuf->actime) * 1000000000);
    stat.set_mtime_ns(static_cast<boost::uint64_t>(ubuf->modtime) * 1000000000);
  } else {
    // POSIX: If times is a null pointer, the access and modification
    //        times of the file shall be set to the current time.
    boost::uint64_t current_time = time(NULL);
    stat.set_atime_ns(current_time * 1000000000);
    stat.set_mtime_ns(current_time * 1000000000);
  }

  try {
    volume_->SetAttr(user_credentials, string(path), stat,
                     static_cast<Setattrs>(SETATTR_ATIME | SETATTR_MTIME));
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}
int FuseAdapter::utimens(const char *path, const struct timespec tv[2]) {
  Stat stat;
  InitializeStat(&stat);
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  // Convert seconds to nanoseconds.
  if (tv != NULL) {
    stat.set_atime_ns(static_cast<boost::uint64_t>(tv[0].tv_sec)
                      * 1000000000 + tv[0].tv_nsec);
    stat.set_mtime_ns(static_cast<boost::uint64_t>(tv[1].tv_sec)
                      * 1000000000 + tv[1].tv_nsec);
  } else {
    // POSIX: If times is a null pointer, the access and modification
    //        times of the file shall be set to the current time.
    boost::uint64_t current_time = time(NULL);
    stat.set_atime_ns(current_time * 1000000000);
    stat.set_mtime_ns(current_time * 1000000000);
  }

  try {
    volume_->SetAttr(user_credentials, string(path), stat,
                     static_cast<Setattrs>(SETATTR_ATIME | SETATTR_MTIME));
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

int FuseAdapter::access(const char *path, int mask) {
  if (!options_->use_fuse_permission_checks) {
    UserCredentials user_credentials;
    GenerateUserCredentials(fuse_get_context(), &user_credentials);

    try {
      volume_->Access(user_credentials,
                      string(path),
                      static_cast<ACCESS_FLAGS>(mask));
    } catch(const PosixErrorException& e) {
      return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
    } catch(const XtreemFSException& e) {
      return -1 * EIO;
    } catch(const exception& e) {
      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
                + string(e.what()));
      return -1 * EIO;
    }
  }

  return 0;
}

/**
 * @remark This function will also get called if the file does not exist and
 *         open was called with O_CREAT.
 *
 *         The mode O_EXCL will not be included in mode. Instead Fuse does
 *         handle it internally as it calls a getattr first to check if the file
 *         exists. If it does not exist, create() is called - otherwise Fuse
 *         will return an error.
 *         See http://www.cs.nmsu.edu/~pfeiffer/fuse-tutorial/unclear.html for
 *         an analysis of the file creation flags handling in Fuse.
 */
int FuseAdapter::create(const char *path, mode_t mode,
    struct fuse_file_info *fi) {
  const string path_str(path);
  if (!xctl_.checkXctlFile(path_str)) {
    UserCredentials user_credentials;
    GenerateUserCredentials(fuse_get_context(), &user_credentials);

    try {
      // Open FileHandle and register it in fuse_file_info.
      FileHandle* file_handle = volume_->OpenFile(
          user_credentials,
          string(path),
          ConvertFlagsUnixToXtreemFS(fi->flags),
          mode);
      // @note The uint64_t cast is needed as Fuse does use a uint64_t instead of
      //       a void* to store a pointer.
      fi->fh = reinterpret_cast<uint64_t>(file_handle);
    } catch(const PosixErrorException& e) {
      return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
    } catch(const XtreemFSException& e) {
      return -1 * EIO;
    } catch(const exception& e) {
      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
                + string(e.what()));
      return -1 * EIO;
    }
  } else {
    fuse_context* ctx = fuse_get_context();
    return xctl_.create(ctx->uid, ctx->gid, path_str);
  }

  return 0;
}

int FuseAdapter::mknod(const char *path, mode_t mode, dev_t device) {
  const string path_str(path);
  if (!xctl_.checkXctlFile(path_str)) {
    UserCredentials user_credentials;
    GenerateUserCredentials(fuse_get_context(), &user_credentials);

    try {
      // Open a temporary filehandle with O_CREAT and close it again.
      FileHandle* file_handle = volume_->OpenFile(
          user_credentials,
          string(path),
          static_cast<SYSTEM_V_FCNTL>(
              SYSTEM_V_FCNTL_H_O_CREAT | SYSTEM_V_FCNTL_H_O_EXCL),
          mode);
      file_handle->Close();
    } catch(const PosixErrorException& e) {
      return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
    } catch(const XtreemFSException& e) {
      return -1 * EIO;
    } catch(const exception& e) {
      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
                + string(e.what()));
      return -1 * EIO;
    }
  } else {
    fuse_context* ctx = fuse_get_context();
    return xctl_.create(ctx->uid, ctx->gid, path_str);
  }

  return 0;
}

int FuseAdapter::mkdir(const char *path, mode_t mode) {
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    volume_->CreateDirectory(user_credentials, string(path), mode);
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

/**
 * @remark Fuse always calls getattr before an open() to determine if the file
 *         exists.
 *         O_CREAT and O_EXCL stripped from the open flags.
 *         O_TRUNC gets also stripped if -o atomic_o_trunc is not specified. In
 *         this case truncate() is called first by Fuse.
 */
int FuseAdapter::open(const char *path, struct fuse_file_info *fi) {
  const string path_str(path);
  if (xctl_.checkXctlFile(path_str)) {
    // Ignore open for xctl files.
    return 0;
  }

  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    // Open FileHandle and register it in fuse_file_info.
    FileHandle* file_handle = volume_->OpenFile(
        user_credentials,
        path_str,
        ConvertFlagsUnixToXtreemFS(fi->flags));
    // @note The uint64_t cast is needed as Fuse does use a uint64_t instead of
    //       a void* to store a pointer.
    fi->fh = reinterpret_cast<uint64_t>(file_handle);
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

int FuseAdapter::truncate(const char *path, off_t new_file_size) {
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    volume_->Truncate(user_credentials, string(path), new_file_size);
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

int FuseAdapter::ftruncate(const char *path,
                           off_t new_file_size,
                           struct fuse_file_info *fi) {
  const string path_str(path);
  if (!xctl_.checkXctlFile(path_str)) {
    UserCredentials user_credentials;
    GenerateUserCredentials(fuse_get_context(), &user_credentials);

    try {
      FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);
      file_handle->Truncate(user_credentials, new_file_size);
    } catch(const PosixErrorException& e) {
      return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
    } catch(const XtreemFSException& e) {
      return -1 * EIO;
    } catch(const exception& e) {
      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
                + string(e.what()));
      return -1 * EIO;
    }
  }

  return 0;
}

int FuseAdapter::write(
    const char *path, const char *buf, size_t size, off_t offset,
    struct fuse_file_info *fi) {
  const string path_str(path);
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  if (!xctl_.checkXctlFile(path_str)) {
    int result;
    try {
      FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);
      result = file_handle->Write(user_credentials, buf, size, offset);
    } catch(const PosixErrorException& e) {
      return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
    } catch(const XtreemFSException& e) {
      return -1 * EIO;
    } catch(const exception& e) {
      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
                + string(e.what()));
      return -1 * EIO;
    }

    return result;
  } else {
    fuse_context* ctx = fuse_get_context();
    return xctl_.write(ctx->uid,
                       ctx->gid,
                       user_credentials,
                       path_str,
                       buf,
                       size);
  }
}

int FuseAdapter::flush(const char *path, struct fuse_file_info *fi) {
  const string path_str(path);
  if (!xctl_.checkXctlFile(path_str)) {
    try {
      FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);
      file_handle->Flush();
    } catch(const PosixErrorException& e) {
      return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
    } catch(const XtreemFSException& e) {
      return -1 * EIO;
    } catch(const exception& e) {
      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
                + string(e.what()));
      return -1 * EIO;
    }
  }

  return 0;
}

int FuseAdapter::read(const char *path, char *buf, size_t size, off_t offset,
         struct fuse_file_info *fi) {
  const string path_str(path);
  if (!xctl_.checkXctlFile(path_str)) {
    UserCredentials user_credentials;
    GenerateUserCredentials(fuse_get_context(), &user_credentials);

    try {
      FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);
      return file_handle->Read(user_credentials, buf, size, offset);
    } catch(const PosixErrorException& e) {
      return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
    } catch(const XtreemFSException& e) {
      return -1 * EIO;
    } catch(const exception& e) {
      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
                + string(e.what()));
      return -1 * EIO;
    }
  } else {
    fuse_context* ctx = fuse_get_context();
    return xctl_.read(ctx->uid, ctx->gid, path_str, buf, size, offset);
  }

  return 0;
}

int FuseAdapter::unlink(const char *path) {
  const string path_str(path);
  if (!xctl_.checkXctlFile(path_str)) {
    UserCredentials user_credentials;
    GenerateUserCredentials(fuse_get_context(), &user_credentials);

    try {
      volume_->Unlink(user_credentials, path);
    } catch(const PosixErrorException& e) {
      return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
    } catch(const XtreemFSException& e) {
      return -1 * EIO;
    } catch(const exception& e) {
      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
                + string(e.what()));
      return -1 * EIO;
    }

    return 0;
  } else {
    fuse_context* ctx = fuse_get_context();
    return xctl_.unlink(ctx->uid, ctx->gid, path_str);
  }
}

int FuseAdapter::fgetattr(
    const char *path, struct stat *statbuf, struct fuse_file_info *fi) {
  const string path_str(path);
  if (!xctl_.checkXctlFile(path_str)) {
    Stat stat;
    UserCredentials user_credentials;
    GenerateUserCredentials(fuse_get_context(), &user_credentials);

    try {
      FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);
      file_handle->GetAttr(user_credentials, &stat);
    } catch(const PosixErrorException& e) {
      return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
    } catch(const XtreemFSException& e) {
      return -1 * EIO;
    } catch(const exception& e) {
      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
                + string(e.what()));
      return -1 * EIO;
    }

    ConvertXtreemFSStatToFuse(stat, statbuf);
    return 0;
  } else {
    fuse_context* ctx = fuse_get_context();
    return xctl_.getattr(ctx->uid, ctx->gid, path_str, statbuf);
  }
}

int FuseAdapter::release(const char *path, struct fuse_file_info *fi) {
  const string path_str(path);
  if (!xctl_.checkXctlFile(path_str)) {
    FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);
    assert(file_handle != NULL);

    // Ensure POSIX semantics and release all locks of the filehandle's process.
    try {
      file_handle->ReleaseLockOfProcess(fuse_get_context()->pid);
    } catch(const XtreemFSException& e) {
      // We dont care if errors occured.
    }

    try {
      file_handle->Close();
    } catch(const PosixErrorException& e) {
      return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
    } catch(const XtreemFSException& e) {
      return -1 * EIO;
    } catch(const exception& e) {
      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
                + string(e.what()));
      return -1 * EIO;
    }
  }
  return 0;
}

int FuseAdapter::readlink(const char *path, char *buf, size_t size) {
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    string target_path = "";
    volume_->ReadLink(user_credentials, string(path), &target_path);

    int max_string_length = min(size - 1, target_path.size());
    strncpy(buf, target_path.c_str(), max_string_length);
    buf[max_string_length] = '\0';
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

int FuseAdapter::rmdir(const char *path) {
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    volume_->RemoveDirectory(user_credentials, string(path));
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

int FuseAdapter::symlink(const char *path, const char *link) {
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    volume_->Symlink(user_credentials, string(path), string(link));
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

int FuseAdapter::rename(const char *path, const char *newpath) {
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    volume_->Rename(user_credentials, string(path), string(newpath));
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

int FuseAdapter::link(const char *path, const char *newpath) {
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    volume_->Link(user_credentials, string(path), string(newpath));
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

int FuseAdapter::chmod(const char *path, mode_t mode) {
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  Stat stat;
  InitializeStat(&stat);
  stat.set_mode(mode);

  try {
    volume_->SetAttr(user_credentials, string(path), stat,
                     static_cast<Setattrs>(SETATTR_MODE));
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

int FuseAdapter::chown(const char *path, uid_t uid, gid_t gid) {
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  Setattrs to_set = static_cast<Setattrs>(0);
  if (uid != static_cast<uid_t>(-1)) {
    to_set = static_cast<Setattrs>(to_set | SETATTR_UID);
  }
  if (gid != static_cast<gid_t>(-1)) {
    to_set = static_cast<Setattrs>(to_set | SETATTR_GID);
  }

  if (to_set == 0) {
    // Skip execution if both values are -1.
    // "If both owner and group are -1, the times need not be updated."
    // see http://pubs.opengroup.org/onlinepubs/009695399/functions/chown.html
    // Don't forget to set the variable "fs" in
    // pjd-fstest-20090130-RC/tests/conf to something != "ext3"
    //  - otherwise chmod/00.t # 156 won't pass.
    return 0;
  }

  Stat stat;
  InitializeStat(&stat);
  if ((to_set &  SETATTR_UID)) {
    stat.set_user_id(user_mapping_->UIDToUsername(uid));
  }
  if ((to_set &  SETATTR_GID)) {
    stat.set_group_id(user_mapping_->GIDToGroupname(gid));
  }

  try {
    volume_->SetAttr(user_credentials, string(path), stat,
                     static_cast<Setattrs>(to_set));
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

int FuseAdapter::setxattr(
    const char *path, const char *name, const char *value,
    size_t size, int flags) {
  bool xtreemfs_attribute_requested = !strncmp(name, "xtreemfs.", 9);
  if (!options_->enable_xattrs && !xtreemfs_attribute_requested) {
    return -1 * ENOTSUP;
  }

  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    // Fuse reuses the value parameter. Therefore we may only use "size"
    // (null-termination is not counted) characters of value.
    volume_->SetXAttr(user_credentials,
                      path,
                      string(name),
                      string(value, size),
                      static_cast<xtreemfs::pbrpc::XATTR_FLAGS>(flags));
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }
  return 0;
}

int FuseAdapter::listxattr(const char *path, char *list, size_t size) {
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    boost::scoped_ptr<listxattrResponse> xattrs(
        volume_->ListXAttrs(user_credentials, path));

    int needed_size = 0;
    for (int i = 0; i < xattrs->xattrs_size(); i++) {
      needed_size += xattrs->xattrs(i).name().size() + 1;
    }

    if (size == 0) {
      return needed_size;
    } else {
      if (size < needed_size) {
        return -1 * ERANGE;
      }

      for (int i = 0; i < xattrs->xattrs_size(); i++) {
        // If xattrs are not enabled, only copy "xtreemfs." attributes.
        if (options_->enable_xattrs
            || !strncmp(xattrs->xattrs(i).name().c_str(), "xtreemfs.", 9)) {
          memcpy(list,
                 xattrs->xattrs(i).name().c_str(),
                 xattrs->xattrs(i).name().size() + 1);
          list += xattrs->xattrs(i).name().size() + 1;
        }
      }
      return needed_size;
    }
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

int FuseAdapter::removexattr(const char *path, const char *name) {
  bool xtreemfs_attribute_requested = !strncmp(name, "xtreemfs.", 9);
  if (!options_->enable_xattrs && !xtreemfs_attribute_requested) {
    return -1 * ENOTSUP;
  }

  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    volume_->RemoveXAttr(user_credentials, path, string(name));
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }
  return 0;
}

int FuseAdapter::lock(const char* path, struct fuse_file_info *fi, int cmd,
                      struct flock* flock) {
  const string path_str(path);
  if (xctl_.checkXctlFile(path_str)) {
    // Locking not supported for xctl files.
    // Some apps such as echo try to lock the file before writing.
    return -1 * EIO;
  }
  // Fuse sets flock->l_whence always to SEEK_SET, meaning the offset has to be
  // interpreted relative to the start of the file.
  UserCredentials user_credentials;
  GenerateUserCredentials(fuse_get_context(), &user_credentials);

  try {
    FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);

    if (cmd == F_GETLK) {
      // Only check if the lock could get acquired.
      boost::shared_ptr<Lock> checked_lock(file_handle->CheckLock(
          user_credentials,
          flock->l_pid,
          flock->l_start,
          flock->l_len,
          (flock->l_type == F_WRLCK)));
      if (checked_lock->client_pid() == flock->l_pid) {
        flock->l_type = F_UNLCK;
      } else {
        // There exists a conflicting lock, tell which one:
        flock->l_type = checked_lock->exclusive() ? F_WRLCK : F_RDLCK;
        flock->l_start = checked_lock->offset();
        flock->l_len = checked_lock->length();
        flock->l_pid = checked_lock->client_pid();
      }
    } else if (cmd == F_SETLK || cmd == F_SETLKW) {
      // Set the lock (type = F_RDLCK|F_WRLCK) or release it (type = F_UNLCK).
      if (flock->l_type == F_RDLCK || flock->l_type == F_WRLCK) {
        boost::shared_ptr<Lock> checked_lock(file_handle->AcquireLock(
            user_credentials,
            flock->l_pid,
            flock->l_start,
            flock->l_len,
            (flock->l_type == F_WRLCK),
            (cmd == F_SETLKW)));
      } else if (flock->l_type == F_UNLCK) {
        file_handle->ReleaseLock(user_credentials,
                                 flock->l_pid,
                                 flock->l_start,
                                 flock->l_len,
                                 (flock->l_type == F_WRLCK));
      } else {
        Logging::log->getLog(LEVEL_ERROR) << "Unknown flock->l_type set:"
            << flock->l_type << endl;
        return -1 * EIO;
      }
    }
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToFuse(e.posix_errno());
  } catch(const XtreemFSException& e) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

}  // namespace xtreemfs
