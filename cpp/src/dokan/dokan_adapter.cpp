/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "dokan/dokan_adapter.h"

//#include <csignal>
//#include <cstring>
//#define DOKAN_USE_VERSION 26
//#include <dokan.h>
//#include <sys/errno.h>
//#include <sys/types.h>
//#include <unistd.h>
//
//#include <algorithm>
//#include <boost/cstdint.hpp>
//#include <boost/lexical_cast.hpp>
//#include <fstream>
//#include <list>
//#include <string>
//
//#include "dokan/cached_directory_entries.h"
#include "dokan/dokan_options.h"
#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
//#include "libxtreemfs/helper.h"
#include "libxtreemfs/system_user_mapping.h"
#include "libxtreemfs/user_mapping.h"
#include "libxtreemfs/simple_uuid_iterator.h"
#include "libxtreemfs/uuid_iterator.h"
#include "libxtreemfs/uuid_resolver.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"
#include "util/error_log.h"
//#include "xtreemfs/MRC.pb.h"
//#include "xtreemfs/OSD.pb.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {

static int ConvertXtreemFSErrnoToDokan(
    xtreemfs::pbrpc::POSIXErrno xtreemfs_errno);

static string WideCharToUTF8(LPCWSTR from) {
	char buffer[1024];
  if (WideCharToMultiByte(CP_UTF8, 0, from, -1, buffer, 1024, 0, 0) > 1024)  {
    return "error";
  }
	
	char* pos = buffer;
	while(*pos != 0) { // replace windows path delimiters
		if (pos[0] == '\\' && pos[1] == 0 && pos != buffer) {	// suppress trailing slash
			pos[0] = 0;
    }	else if(*pos == '\\') { // convert
			*pos = '/';
    }
		pos++;
	}

	return buffer;
}

static int UTF8ToWideChar(const std::string& utf8, wchar_t* buffer, int buffer_size) {
	return MultiByteToWideChar(CP_UTF8, 0, utf8.c_str(), -1, buffer, buffer_size);
}

DokanAdapter::DokanAdapter(DokanOptions* options)
    : options_(options),
      xctl_("/.xctl$$$")
{}

DokanAdapter::~DokanAdapter() {}

int DokanAdapter::CreateFile(LPCWSTR path,
                             DWORD desired_access,
                             DWORD share_mode,
                             DWORD creation_disposition,
                             DWORD flags_and_attributes,
                             PDOKAN_FILE_INFO dokan_file_info) {
  return -ERROR_ACCESS_DENIED;
}

int DokanAdapter::OpenDirectory(
    LPCWSTR,				// FileName
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::CreateDirectory(
    LPCWSTR,				// FileName
    PDOKAN_FILE_INFO) {
  return 0;
}

// When FileInfo->DeleteOnClose is true, you must delete the file in Cleanup.
int DokanAdapter::Cleanup(
    LPCWSTR,      // FileName
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::CloseFile(
    LPCWSTR,      // FileName
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::ReadFile(
    LPCWSTR,  // FileName
    LPVOID,   // Buffer
    DWORD,    // NumberOfBytesToRead
    LPDWORD,  // NumberOfBytesRead
    LONGLONG, // Offset
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::WriteFile(
    LPCWSTR,  // FileName
    LPCVOID,  // Buffer
    DWORD,    // NumberOfBytesToWrite
    LPDWORD,  // NumberOfBytesWritten
    LONGLONG, // Offset
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::FlushFileBuffers(
    LPCWSTR, // FileName
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::GetFileInformation(
    LPCWSTR,          // FileName
    LPBY_HANDLE_FILE_INFORMATION, // Buffer
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::FindFiles(
    LPCWSTR path,
    PFillFindData callback,
    PDOKAN_FILE_INFO dokan_file_info) {
  UserCredentials user_credentials;
  system_user_mapping_->GetUserCredentialsForCurrentUser(
      &user_credentials);

  try {
    for (uint64_t offset = 0; ; offset += options_->readdir_chunk_size) {
      xtreemfs::pbrpc::DirectoryEntries* entries = volume_->ReadDir(
          user_credentials, WideCharToUTF8(path), offset,
          options_->readdir_chunk_size, false);

      for (int i = 0; i < entries->entries_size(); ++i) {
        const xtreemfs::pbrpc::DirectoryEntry& entry = entries->entries(i);
        WIN32_FIND_DATA find_data;

        UTF8ToWideChar(entry.name(), find_data.cFileName, 260);
        // ... other fields

        callback(&find_data, dokan_file_info);
      }
    }
  } catch(const PosixErrorException& e) {
    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
  } catch(const XtreemFSException&) {
    return -1 * EIO;
  } catch(const exception& e) {
    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
              + string(e.what()));
    return -1 * EIO;
  }

  return 0;
}

// You should implement either FindFiles or FindFilesWithPattern
int DokanAdapter::FindFilesWithPattern(
    LPCWSTR,			// PathName
    LPCWSTR,			// SearchPattern
    PFillFindData,		// call this function with PWIN32_FIND_DATAW
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::SetFileAttributes(
    LPCWSTR, // FileName
    DWORD,   // FileAttributes
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::SetFileTime(
    LPCWSTR,		// FileName
    CONST FILETIME*, // CreationTime
    CONST FILETIME*, // LastAccessTime
    CONST FILETIME*, // LastWriteTime
    PDOKAN_FILE_INFO) {
  return 0;
}

// You should not delete file on DeleteFile or DeleteDirectory.
// When DeleteFile or DeleteDirectory, you must check whether
// you can delete the file or not, and return 0 (when you can delete it)
// or appropriate error codes such as -ERROR_DIR_NOT_EMPTY,
// -ERROR_SHARING_VIOLATION.
// When you return 0 (ERROR_SUCCESS), you get Cleanup with
// FileInfo->DeleteOnClose set TRUE and you have to delete the
// file in Close.
int DokanAdapter::DeleteFile(
    LPCWSTR, // FileName
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::DeleteDirectory( 
    LPCWSTR, // FileName
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::MoveFile(
    LPCWSTR, // ExistingFileName
    LPCWSTR, // NewFileName
    BOOL,	// ReplaceExisiting
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::SetEndOfFile(
    LPCWSTR,  // FileName
    LONGLONG, // Length
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::SetAllocationSize(
    LPCWSTR,  // FileName
    LONGLONG, // Length
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::LockFile(
    LPCWSTR, // FileName
    LONGLONG, // ByteOffset
    LONGLONG, // Length
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::UnlockFile(
    LPCWSTR, // FileName
    LONGLONG,// ByteOffset
    LONGLONG,// Length
    PDOKAN_FILE_INFO) {
  return 0;
}

// Neither GetDiskFreeSpace nor GetVolumeInformation
// save the DokanFileContext->Context.
// Before these methods are called, CreateFile may not be called.
// (ditto CloseFile and Cleanup)

// see Win32 API GetDiskFreeSpaceEx
int DokanAdapter::GetDiskFreeSpace(
    PULONGLONG, // FreeBytesAvailable
    PULONGLONG, // TotalNumberOfBytes
    PULONGLONG, // TotalNumberOfFreeBytes
    PDOKAN_FILE_INFO) {
  return 0;
}

// see Win32 API GetVolumeInformation
int DokanAdapter::GetVolumeInformation(
    LPWSTR, // VolumeNameBuffer
    DWORD,	// VolumeNameSize in num of chars
    LPDWORD,// VolumeSerialNumber
    LPDWORD,// MaximumComponentLength in num of chars
    LPDWORD,// FileSystemFlags
    LPWSTR,	// FileSystemNameBuffer
    DWORD,	// FileSystemNameSize in num of chars
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::Unmount(
    PDOKAN_FILE_INFO) {
  return 0;
}

// Suported since 0.6.0. You must specify the version at DOKAN_OPTIONS.Version.
int DokanAdapter::GetFileSecurity(
    LPCWSTR, // FileName
    // A pointer to SECURITY_INFORMATION value being requested
    PSECURITY_INFORMATION,
      // A pointer to SECURITY_DESCRIPTOR buffer to be filled
    PSECURITY_DESCRIPTOR,
    ULONG, // length of Security descriptor buffer
    PULONG, // LengthNeeded
    PDOKAN_FILE_INFO) {
  return 0;
}

int DokanAdapter::SetFileSecurity(
    LPCWSTR, // FileName
    PSECURITY_INFORMATION,
    PSECURITY_DESCRIPTOR, // SecurityDescriptor
    ULONG, // SecurityDescriptor length
    PDOKAN_FILE_INFO) {
  return 0;
}

void DokanAdapter::Start() {
  // Start logging manually (although it would be automatically started by
  // ClientImplementation()) as its required by UserMapping.
  initialize_logger(options_->log_level_string,
                    options_->log_file_path,
                    LEVEL_WARN);

  // Setup Usermapping.
  system_user_mapping_.reset(SystemUserMapping::GetSystemUserMapping());

  // Create new Client.
  UserCredentials client_user_credentials;
  system_user_mapping_->GetUserCredentialsForCurrentUser(
      &client_user_credentials);
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
  xctl_.set_uuid_resolver(client_->GetUUIDResolver());

  // Try to access Volume. If it fails, an error will be thrown.
  Stat stat;
  volume_->GetAttr(client_user_credentials, "/", &stat);

  // Check the attributes of the Volume.
  // Ugly trick to get the addresses of all MRC UUIDs and pass them to
  // ListVolumes().
  SimpleUUIDIterator mrc_uuids;
  SimpleUUIDIterator mrc_addresses;
  client_->GetUUIDResolver()->VolumeNameToMRCUUID(options_->volume_name,
                                                  &mrc_uuids);
  string first_mrc_uuid = "";
  string current_mrc_uuid;
  string current_mrc_address;
  while (true) {
    mrc_uuids.GetUUID(&current_mrc_uuid);
    if (first_mrc_uuid == "") {
      first_mrc_uuid = current_mrc_uuid;
    } else if (first_mrc_uuid == current_mrc_uuid) {
      break;
    }
    client_->GetUUIDResolver()->UUIDToAddress(current_mrc_uuid,
                                              &current_mrc_address);
    mrc_uuids.MarkUUIDAsFailed(current_mrc_uuid);
    mrc_addresses.AddUUID(current_mrc_address);
  }

  // Get volume information as xattr.
  // Check attributes.
  boost::scoped_ptr<listxattrResponse> xattrs(
      volume_->ListXAttrs(client_user_credentials, "/", false));
  for (int i = 0; i < xattrs->xattrs_size(); ++i) {
    const xtreemfs::pbrpc::XAttr& xattr = xattrs->xattrs(i);

    // First grid user mapping wins.
    if (!options_->grid_auth_mode_globus && !options_->grid_auth_mode_unicore) {
      if (xattr.name() == "xtreemfs.volattr.globus_gridmap") {
        options_->grid_auth_mode_globus = true;
        options_->user_mapping_type = UserMapping::kGlobus;
        if (options_->grid_gridmap_location.empty()) {
          options_->grid_gridmap_location =
              options_->grid_gridmap_location_default_globus;
        }
        Logging::log->getLog(LEVEL_INFO) << "Using Globus gridmap file "
            << options_->grid_gridmap_location << endl;
      }

      if (xattr.name() == "xtreemfs.volattr.unicore_uudb") {
        options_->grid_auth_mode_unicore = true;
        options_->user_mapping_type = UserMapping::kUnicore;
        if (options_->grid_gridmap_location.empty()) {
          options_->grid_gridmap_location =
              options_->grid_gridmap_location_default_unicore;
        }
        Logging::log->getLog(LEVEL_INFO) << "Using Unicore uudb file "
            << options_->grid_gridmap_location << endl;
      }
    }
  }

  // Check if the user specified an additional user mapping in options.
  UserMapping* additional_user_mapping = UserMapping::CreateUserMapping(
      options_->user_mapping_type,
      *options_);
  if (additional_user_mapping) {
    system_user_mapping_->RegisterAdditionalUserMapping(
      additional_user_mapping);
    system_user_mapping_->StartAdditionalUserMapping();
  }
}

void DokanAdapter::Stop() {
  // Close UserMapping.
  system_user_mapping_->StopAdditionalUserMapping();

  // Shutdown() Client. That does also invoke a volume->Close().
  client_->Shutdown();
}

//void DokanAdapter::GenerateUserCredentials(
//    struct dokan_context* dokan_context,
//    xtreemfs::pbrpc::UserCredentials* user_credentials) {
//  // No dokan_context known, use information of current process.
//  if (dokan_context == NULL) {
//    GenerateUserCredentials(getuid(), getgid(), getpid(), user_credentials);
//  } else {
//    GenerateUserCredentials(dokan_context->uid,
//                            dokan_context->gid,
//                            dokan_context->pid,
//                            user_credentials);
//  }
//}
//
//void DokanAdapter::GenerateUserCredentials(
//    uid_t uid,
//    gid_t gid,
//    pid_t pid,
//    xtreemfs::pbrpc::UserCredentials* user_credentials) {
//  user_credentials->set_username(user_mapping_->UIDToUsername(uid));
//
//  list<string> groupnames;
//  user_mapping_->GetGroupnames(uid, gid, pid, &groupnames);
//  for (list<string>::iterator it = groupnames.begin();
//       it != groupnames.end(); ++it) {
//    user_credentials->add_groups(*it);
//  }
//}
//
//void DokanAdapter::ConvertXtreemFSStatToDokan(
//    const xtreemfs::pbrpc::Stat& xtreemfs_stat, struct stat* dokan_stat) {
//  dokan_stat->st_dev = xtreemfs_stat.dev();
//  dokan_stat->st_blksize = 8 * 128 * 1024;
//  dokan_stat->st_ino = xtreemfs_stat.ino();  // = fileId
//  dokan_stat->st_mode = xtreemfs_stat.mode();
//  dokan_stat->st_nlink = xtreemfs_stat.nlink();
//
//  // Map user- and groupnames.
//  dokan_stat->st_uid = user_mapping_->UsernameToUID(xtreemfs_stat.user_id());
//  dokan_stat->st_gid = user_mapping_->GroupnameToGID(xtreemfs_stat.group_id());
//
//  dokan_stat->st_size = xtreemfs_stat.size();
//#ifdef __linux
//  dokan_stat->st_atim.tv_sec  = xtreemfs_stat.atime_ns() / 1000000000;
//  dokan_stat->st_atim.tv_nsec = xtreemfs_stat.atime_ns() % 1000000000;
//  dokan_stat->st_mtim.tv_sec  = xtreemfs_stat.mtime_ns() / 1000000000;
//  dokan_stat->st_mtim.tv_nsec = xtreemfs_stat.mtime_ns() % 1000000000;
//  dokan_stat->st_ctim.tv_sec  = xtreemfs_stat.ctime_ns() / 1000000000;
//  dokan_stat->st_ctim.tv_nsec = xtreemfs_stat.ctime_ns() % 1000000000;
//#elif __APPLE__
//  dokan_stat->st_atimespec.tv_sec  = xtreemfs_stat.atime_ns() / 1000000000;
//  dokan_stat->st_atimespec.tv_nsec = xtreemfs_stat.atime_ns() % 1000000000;
//  dokan_stat->st_mtimespec.tv_sec  = xtreemfs_stat.mtime_ns() / 1000000000;
//  dokan_stat->st_mtimespec.tv_nsec = xtreemfs_stat.mtime_ns() % 1000000000;
//  dokan_stat->st_ctimespec.tv_sec  = xtreemfs_stat.ctime_ns() / 1000000000;
//  dokan_stat->st_ctimespec.tv_nsec = xtreemfs_stat.ctime_ns() % 1000000000;
//#endif
//
//  dokan_stat->st_rdev = 0;
//  dokan_stat->st_blocks = 0;
//}
//
//xtreemfs::pbrpc::SYSTEM_V_FCNTL DokanAdapter::ConvertFlagsUnixToXtreemFS(
//    int flags) {
//  int result = 0;
//
//#define CHECK(result, flags, unix, proto) { \
//  if ((flags & unix) != 0) result |= proto; \
//}
//  CHECK(result, flags, O_RDONLY   , SYSTEM_V_FCNTL_H_O_RDONLY);
//  CHECK(result, flags, O_WRONLY   , SYSTEM_V_FCNTL_H_O_WRONLY);
//  CHECK(result, flags, O_RDWR     , SYSTEM_V_FCNTL_H_O_RDWR);
//  CHECK(result, flags, O_APPEND   , SYSTEM_V_FCNTL_H_O_APPEND);
//  CHECK(result, flags, O_CREAT    , SYSTEM_V_FCNTL_H_O_CREAT);
//  CHECK(result, flags, O_TRUNC    , SYSTEM_V_FCNTL_H_O_TRUNC);
//  CHECK(result, flags, O_EXCL     , SYSTEM_V_FCNTL_H_O_EXCL);
//  CHECK(result, flags, O_SYNC     , SYSTEM_V_FCNTL_H_O_SYNC);
//#ifdef __linux
//  CHECK(result, flags, O_DSYNC    , SYSTEM_V_FCNTL_H_O_SYNC);
//#endif
//
//  return xtreemfs::pbrpc::SYSTEM_V_FCNTL(result);
//}
//
static int ConvertXtreemFSErrnoToDokan(
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
//
//int DokanAdapter::statfs(const char *path, struct statvfs *statv) {
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    boost::scoped_ptr<StatVFS> stat_vfs(
//            volume_->StatFS(user_credentials));
//
//    // According to Dokan.h: "The 'f_frsize', 'f_favail', 'f_fsid' and 'f_flag'
//    //                       fields are ignored"
//    statv->f_bsize   = stat_vfs->bsize();  // file system block size
//    statv->f_bfree   = stat_vfs->bavail();  // # free blocks
//    // # free blocks for unprivileged users
//    statv->f_bavail  = stat_vfs->bavail();
//    statv->f_files   = 2048;  // # inodes (we use here a bogus number)
//    // Total number of blocks in file system.
//    statv->f_blocks  = stat_vfs->blocks();
//    statv->f_ffree   = 2048;  // # free inodes (we use here a bogus number)
//    statv->f_namemax = stat_vfs->namemax();  // maximum filename length
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//        + string(string(e.what())));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
//int DokanAdapter::getattr(const char *path, struct stat *statbuf) {
//  const string path_str(path);
//  if (!xctl_.checkXctlFile(path_str)) {
//    Stat stat;
//    UserCredentials user_credentials;
//    GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//    try {
//      volume_->GetAttr(user_credentials, path_str, &stat);
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//          + string(e.what()));
//      return -1 * EIO;
//    }
//
//    ConvertXtreemFSStatToDokan(stat, statbuf);
//    return 0;
//  } else {
//    dokan_context* ctx = dokan_get_context();
//    return xctl_.getattr(ctx->uid, ctx->gid, path_str, statbuf);
//  }
//}
//
///** Returns the size of a value of an extended attribute (if size == 0) or fills
// *  "value" with the value of this attribute if it's size does not exceed "size"
// *  - 1 (due to null termination). */
//int DokanAdapter::getxattr(
//    const char *path, const char *name, char *value, size_t size) {
//  const string path_str(path);
//
//  // No getxattr for xtfsutil control files.
//  if (xctl_.checkXctlFile(path_str)) {
//      return -1 * ENOATTR;
//  }
//
//  bool xtreemfs_attribute_requested = !strncmp(name, "xtreemfs.", 9);
//  if (!options_->enable_xattrs && !xtreemfs_attribute_requested) {
//    return -1 * ENOTSUP;
//  }
//
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    if (size == 0) {
//      int result = 0;
//      if (volume_->GetXAttrSize(user_credentials, path_str,
//          string(name), &result)) {
//        return result;
//      } else {
//        return -1 * ENOATTR;
//      }
//    } else {
//      string value_string;
//      if (volume_->GetXAttr(user_credentials, path_str,
//                            string(name), &value_string)) {
//        if (value_string.size() <= size) {
//          // XAttrs are actually binary data and do not require a
//          // null-terminating character.
//          memcpy(value, value_string.c_str(), value_string.size());
//          return value_string.size();
//        } else {
//          return -1 * ERANGE;
//        }
//      } else {
//        return -1 * ENOATTR;
//      }
//    }
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
///* Creates a CachedDirectoryEntries struct on the heap which can hold the
// * cached directory entries and provides a mutex in order serialize the access
// * to this struct. Will be released on releasedir().
// */
//int DokanAdapter::opendir(const char *path, struct dokan_file_info *fi) {
//  // No default POSIX permissions: Check if it's allowed to enter the dir.
//  if (!options_->use_dokan_permission_checks) {
//    UserCredentials user_credentials;
//    GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//    // TODO(mberlin): Wait for change of access method and check for X_OK.
//    try {
//      volume_->Access(user_credentials,
//                      string(path),
//                      ACCESS_FLAGS_R_OK);
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//                + string(e.what()));
//      return -1 * EIO;
//    }
//  }
//
//  CachedDirectoryEntries* cached_direntries = new CachedDirectoryEntries;
//  cached_direntries->dir_entries = NULL;
//  // @note The uint64_t cast is needed as Dokan does use a uint64_t instead of
//  //       a void* to store a pointer.
//  fi->fh = reinterpret_cast<uint64_t>(cached_direntries);
//
//  return 0;
//}
//
///**
// *  Dokan's filler() method called with offset=0 returns 1 only if an error
// *  happened. In practice, this allows to fill complete directories with one
// *  readdir() call. (According to the Dokan 2.8.5 source code, it will stop if
// *  the complete buffer exceeded 2 GiB.)
// *
// *  Called with (offset != 0), filler returns 1 after filling the initial 4k
// *  buffer (observed in Dokan 2.8.5). As entries are aligned to 32 Byte
// *  boundaries, a maximum of 128 entries (or less if filenames are very long?)
// *  is possible.
// *  However, the libxtreemfs default readdir chunksize is 1024 entries.
// *  Therefore, we temporary store unprocessed items in fi->fh.
// *  Another reason why we cache the directory entries is due to the way Dokan
// *  detects no more readdir() calls are needed:
// *  If filler returns with 1 due to a full buffer, Dokan will call readdir()
// *  again with the new offset value. If this execution of readdir does not call
// *  filler() again, Dokan knows the directory was completely read. In order to
// *  know if you don't have to call filler again, we need the cached directory
// *  entries.
// *
// *  @attention If this function is executed with offset>0, duplicate or missing
// *             entries may show up in case files were created or deleted during
// *             two readdir calls.
// */
//int DokanAdapter::readdir(const char *path, void *buf, dokan_fill_dir_t filler,
//    off_t offset, struct dokan_file_info *fi) {
//  DirectoryEntries* dir_entries = NULL;
//  boost::uint64_t dir_entries_offset = 0;
//
//  // Look up if there are some unprocessed directory entries.
//  CachedDirectoryEntries* cached_direntries
//      = reinterpret_cast<CachedDirectoryEntries*>(fi->fh);
//  // Struct was created at opendir and deleted at releasedir().
//  assert(cached_direntries != NULL);
//  boost::mutex::scoped_lock lock(cached_direntries->mutex);
//
//  // Use cached entries first if applicable
//  if (cached_direntries->dir_entries != NULL
//      && cached_direntries->offset <= offset) {
//    dir_entries = cached_direntries->dir_entries;
//    // It may happen that the offset of the last cached entry is below the
//    // requested "offset". This is fine, because filler() won't get called and
//    // Dokan knows to stop executing further readdir()s.
//    dir_entries_offset = cached_direntries->offset;
//  }
//
//  // Fetch entries from MRC.
//  if (dir_entries == NULL) {
//    UserCredentials user_credentials;
//    GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//    try {
//      // libxtreemfs itself may have cached the readdir response, too.
//      dir_entries = volume_->ReadDir(user_credentials,
//                                     string(path),
//                                     offset,
//                                     options_->readdir_chunk_size,
//                                     false);
//      dir_entries_offset = offset;
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//                + string(e.what()));
//      return -1 * EIO;
//    }
//  }
//
//  // The directory entries must start at least at "offset".
//  assert(dir_entries_offset <= offset);
//
//  // Let Dokan fill the buffers.
//  struct stat dokan_statbuf;
//  memset(&dokan_statbuf, 0, sizeof(dokan_statbuf));
//
//  int i;
//  for (i = offset; i < dir_entries_offset + dir_entries->entries_size(); i++) {
//    boost::uint64_t dir_entries_index = i -  dir_entries_offset;
//    if (dir_entries->entries(dir_entries_index).has_stbuf()) {
//      // Only set here st_ino and st_mode for the struct dirent.
//      dokan_statbuf.st_ino
//          = dir_entries->entries(dir_entries_index).stbuf().ino();
//      dokan_statbuf.st_mode
//          = dir_entries->entries(dir_entries_index).stbuf().mode();
//
//      if (filler(buf,
//                 dir_entries->entries(dir_entries_index).name().c_str(),
//                 &dokan_statbuf,
//                 i + 1)) {
//        break;
//      }
//    } else {
//      if (filler(buf,
//                 dir_entries->entries(dir_entries_index).name().c_str(),
//                 NULL,
//                 i + 1)) {
//        break;
//      }
//    }
//  }
//
//  bool chunk_completely_read
//      = (dir_entries_offset + dir_entries->entries_size() == i);
//  bool definetely_last_chunk
//      = (dir_entries->entries_size() < options_->readdir_chunk_size);
//  bool no_filler_called
//      = (offset == dir_entries_offset + dir_entries->entries_size());
//
//  // Dokan did read all directory entries or this was not the last chunk
//  // => don't cache it.
//  if (no_filler_called || (!definetely_last_chunk && chunk_completely_read)) {
//    delete dir_entries;
//    if (cached_direntries->dir_entries == dir_entries) {
//      cached_direntries->dir_entries = NULL;
//    }
//  } else {
//    // Cache directory entries (if not already cached).
//    if (cached_direntries->dir_entries != dir_entries) {
//      // Free previously cached entries.
//      if (cached_direntries->dir_entries != NULL) {
//        delete cached_direntries->dir_entries;
//        cached_direntries->dir_entries = NULL;
//      }
//
//      cached_direntries->dir_entries = dir_entries;
//      cached_direntries->offset = dir_entries_offset;
//    }
//  }
//
//  return 0;
//}
//
//int DokanAdapter::releasedir(const char *path, struct dokan_file_info *fi) {
//  CachedDirectoryEntries* cached_direntries
//      = reinterpret_cast<CachedDirectoryEntries*>(fi->fh);
//  assert(cached_direntries != NULL);
//  delete cached_direntries->dir_entries;
//  delete cached_direntries;
//  fi->fh = 0;  // NULL.
//
//  return 0;
//}
//
//int DokanAdapter::utime(const char *path, struct utimbuf *ubuf) {
//  Stat stat;
//  InitializeStat(&stat);
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  // Convert seconds to nanoseconds.
//  if (ubuf != NULL) {
//    stat.set_atime_ns(static_cast<boost::uint64_t>(ubuf->actime) * 1000000000);
//    stat.set_mtime_ns(static_cast<boost::uint64_t>(ubuf->modtime) * 1000000000);
//  } else {
//    // POSIX: If times is a null pointer, the access and modification
//    //        times of the file shall be set to the current time.
//    boost::uint64_t current_time = time(NULL);
//    stat.set_atime_ns(current_time * 1000000000);
//    stat.set_mtime_ns(current_time * 1000000000);
//  }
//
//  try {
//    volume_->SetAttr(user_credentials, string(path), stat,
//                     static_cast<Setattrs>(SETATTR_ATIME | SETATTR_MTIME));
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//int DokanAdapter::utimens(const char *path, const struct timespec tv[2]) {
//  Stat stat;
//  InitializeStat(&stat);
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  // Convert seconds to nanoseconds.
//  if (tv != NULL) {
//    stat.set_atime_ns(static_cast<boost::uint64_t>(tv[0].tv_sec)
//                      * 1000000000 + tv[0].tv_nsec);
//    stat.set_mtime_ns(static_cast<boost::uint64_t>(tv[1].tv_sec)
//                      * 1000000000 + tv[1].tv_nsec);
//  } else {
//    // POSIX: If times is a null pointer, the access and modification
//    //        times of the file shall be set to the current time.
//    boost::uint64_t current_time = time(NULL);
//    stat.set_atime_ns(current_time * 1000000000);
//    stat.set_mtime_ns(current_time * 1000000000);
//  }
//
//  try {
//    volume_->SetAttr(user_credentials, string(path), stat,
//                     static_cast<Setattrs>(SETATTR_ATIME | SETATTR_MTIME));
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
//int DokanAdapter::access(const char *path, int mask) {
//  if (!options_->use_dokan_permission_checks) {
//    UserCredentials user_credentials;
//    GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//    try {
//      volume_->Access(user_credentials,
//                      string(path),
//                      static_cast<ACCESS_FLAGS>(mask));
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//                + string(e.what()));
//      return -1 * EIO;
//    }
//  }
//
//  return 0;
//}
//
///**
// * @remark This function will also get called if the file does not exist and
// *         open was called with O_CREAT.
// *
// *         The mode O_EXCL will not be included in mode. Instead Dokan does
// *         handle it internally as it calls a getattr first to check if the file
// *         exists. If it does not exist, create() is called - otherwise Dokan
// *         will return an error.
// *         See http://www.cs.nmsu.edu/~pfeiffer/dokan-tutorial/unclear.html for
// *         an analysis of the file creation flags handling in Dokan.
// */
//int DokanAdapter::create(const char *path, mode_t mode,
//    struct dokan_file_info *fi) {
//  const string path_str(path);
//  if (!xctl_.checkXctlFile(path_str)) {
//    UserCredentials user_credentials;
//    GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//    try {
//      // Open FileHandle and register it in dokan_file_info.
//      FileHandle* file_handle = volume_->OpenFile(
//          user_credentials,
//          string(path),
//          ConvertFlagsUnixToXtreemFS(fi->flags),
//          mode);
//      // @note The uint64_t cast is needed as Dokan does use a uint64_t instead of
//      //       a void* to store a pointer.
//      fi->fh = reinterpret_cast<uint64_t>(file_handle);
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//                + string(e.what()));
//      return -1 * EIO;
//    }
//  } else {
//    dokan_context* ctx = dokan_get_context();
//    return xctl_.create(ctx->uid, ctx->gid, path_str);
//  }
//
//  return 0;
//}
//
//int DokanAdapter::mknod(const char *path, mode_t mode, dev_t device) {
//  const string path_str(path);
//  if (!xctl_.checkXctlFile(path_str)) {
//    UserCredentials user_credentials;
//    GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//    try {
//      // Open a temporary filehandle with O_CREAT and close it again.
//      FileHandle* file_handle = volume_->OpenFile(
//          user_credentials,
//          string(path),
//          static_cast<SYSTEM_V_FCNTL>(
//              SYSTEM_V_FCNTL_H_O_CREAT | SYSTEM_V_FCNTL_H_O_EXCL),
//          mode);
//      file_handle->Close();
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//                + string(e.what()));
//      return -1 * EIO;
//    }
//  } else {
//    dokan_context* ctx = dokan_get_context();
//    return xctl_.create(ctx->uid, ctx->gid, path_str);
//  }
//
//  return 0;
//}
//
//int DokanAdapter::mkdir(const char *path, mode_t mode) {
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    volume_->MakeDirectory(user_credentials, string(path), mode);
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
///**
// * @remark Dokan always calls getattr before an open() to determine if the file
// *         exists.
// *         O_CREAT and O_EXCL stripped from the open flags.
// *         O_TRUNC gets also stripped if -o atomic_o_trunc is not specified. In
// *         this case truncate() is called first by Dokan.
// */
//int DokanAdapter::open(const char *path, struct dokan_file_info *fi) {
//  const string path_str(path);
//  if (xctl_.checkXctlFile(path_str)) {
//    // Ignore open for xctl files.
//    return 0;
//  }
//
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    // Open FileHandle and register it in dokan_file_info.
//    FileHandle* file_handle = volume_->OpenFile(
//        user_credentials,
//        path_str,
//        ConvertFlagsUnixToXtreemFS(fi->flags));
//    // @note The uint64_t cast is needed as Dokan does use a uint64_t instead of
//    //       a void* to store a pointer.
//    fi->fh = reinterpret_cast<uint64_t>(file_handle);
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
//int DokanAdapter::truncate(const char *path, off_t new_file_size) {
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    volume_->Truncate(user_credentials, string(path), new_file_size);
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
//int DokanAdapter::ftruncate(const char *path,
//                           off_t new_file_size,
//                           struct dokan_file_info *fi) {
//  const string path_str(path);
//  if (!xctl_.checkXctlFile(path_str)) {
//    UserCredentials user_credentials;
//    GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//    try {
//      FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);
//      file_handle->Truncate(user_credentials, new_file_size);
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//                + string(e.what()));
//      return -1 * EIO;
//    }
//  }
//
//  return 0;
//}
//
//int DokanAdapter::write(
//    const char *path, const char *buf, size_t size, off_t offset,
//    struct dokan_file_info *fi) {
//  const string path_str(path);
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  if (!xctl_.checkXctlFile(path_str)) {
//    int result;
//    try {
//      FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);
//      result = file_handle->Write(user_credentials, buf, size, offset);
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//                + string(e.what()));
//      return -1 * EIO;
//    }
//
//    return result;
//  } else {
//    dokan_context* ctx = dokan_get_context();
//    return xctl_.write(ctx->uid,
//                       ctx->gid,
//                       user_credentials,
//                       path_str,
//                       buf,
//                       size);
//  }
//}
//
//int DokanAdapter::flush(const char *path, struct dokan_file_info *fi) {
//  const string path_str(path);
//  if (!xctl_.checkXctlFile(path_str)) {
//    try {
//      FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);
//      file_handle->Flush();
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//                + string(e.what()));
//      return -1 * EIO;
//    }
//  }
//
//  return 0;
//}
//
//int DokanAdapter::read(const char *path, char *buf, size_t size, off_t offset,
//         struct dokan_file_info *fi) {
//  const string path_str(path);
//  if (!xctl_.checkXctlFile(path_str)) {
//    UserCredentials user_credentials;
//    GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//    try {
//      FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);
//      return file_handle->Read(user_credentials, buf, size, offset);
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//                + string(e.what()));
//      return -1 * EIO;
//    }
//  } else {
//    dokan_context* ctx = dokan_get_context();
//    return xctl_.read(ctx->uid, ctx->gid, path_str, buf, size, offset);
//  }
//
//  return 0;
//}
//
//int DokanAdapter::unlink(const char *path) {
//  const string path_str(path);
//  if (!xctl_.checkXctlFile(path_str)) {
//    UserCredentials user_credentials;
//    GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//    try {
//      volume_->Unlink(user_credentials, path);
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//                + string(e.what()));
//      return -1 * EIO;
//    }
//
//    return 0;
//  } else {
//    dokan_context* ctx = dokan_get_context();
//    return xctl_.unlink(ctx->uid, ctx->gid, path_str);
//  }
//}
//
//int DokanAdapter::fgetattr(
//    const char *path, struct stat *statbuf, struct dokan_file_info *fi) {
//  const string path_str(path);
//  if (!xctl_.checkXctlFile(path_str)) {
//    Stat stat;
//    UserCredentials user_credentials;
//    GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//    try {
//      FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);
//      file_handle->GetAttr(user_credentials, &stat);
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//                + string(e.what()));
//      return -1 * EIO;
//    }
//
//    ConvertXtreemFSStatToDokan(stat, statbuf);
//    return 0;
//  } else {
//    dokan_context* ctx = dokan_get_context();
//    return xctl_.getattr(ctx->uid, ctx->gid, path_str, statbuf);
//  }
//}
//
//int DokanAdapter::release(const char *path, struct dokan_file_info *fi) {
//  const string path_str(path);
//  if (!xctl_.checkXctlFile(path_str)) {
//    FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);
//    assert(file_handle != NULL);
//
//    // Ensure POSIX semantics and release all locks of the filehandle's process.
//    try {
//      file_handle->ReleaseLockOfProcess(dokan_get_context()->pid);
//    } catch(const XtreemFSException& e) {
//      // We dont care if errors occured.
//    }
//
//    try {
//      file_handle->Close();
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//                + string(e.what()));
//      return -1 * EIO;
//    }
//  }
//  return 0;
//}
//
//int DokanAdapter::readlink(const char *path, char *buf, size_t size) {
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    string target_path = "";
//    volume_->ReadLink(user_credentials, string(path), &target_path);
//
//    int max_string_length = min(size - 1, target_path.size());
//    strncpy(buf, target_path.c_str(), max_string_length);
//    buf[max_string_length] = '\0';
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
//int DokanAdapter::rmdir(const char *path) {
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    volume_->DeleteDirectory(user_credentials, string(path));
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
//int DokanAdapter::symlink(const char *path, const char *link) {
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    volume_->Symlink(user_credentials, string(path), string(link));
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
//int DokanAdapter::rename(const char *path, const char *newpath) {
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    volume_->Rename(user_credentials, string(path), string(newpath));
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
//int DokanAdapter::link(const char *path, const char *newpath) {
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    volume_->Link(user_credentials, string(path), string(newpath));
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
//int DokanAdapter::chmod(const char *path, mode_t mode) {
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  Stat stat;
//  InitializeStat(&stat);
//  stat.set_mode(mode);
//
//  try {
//    volume_->SetAttr(user_credentials, string(path), stat,
//                     static_cast<Setattrs>(SETATTR_MODE));
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
//int DokanAdapter::chown(const char *path, uid_t uid, gid_t gid) {
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  Setattrs to_set = static_cast<Setattrs>(0);
//  if (uid != static_cast<uid_t>(-1)) {
//    to_set = static_cast<Setattrs>(to_set | SETATTR_UID);
//  }
//  if (gid != static_cast<gid_t>(-1)) {
//    to_set = static_cast<Setattrs>(to_set | SETATTR_GID);
//  }
//
//  if (to_set == 0) {
//    // Skip execution if both values are -1.
//    // "If both owner and group are -1, the times need not be updated."
//    // see http://pubs.opengroup.org/onlinepubs/009695399/functions/chown.html
//    // Don't forget to set the variable "fs" in
//    // pjd-fstest-20090130-RC/tests/conf to something != "ext3"
//    //  - otherwise chmod/00.t # 156 won't pass.
//    return 0;
//  }
//
//  Stat stat;
//  InitializeStat(&stat);
//  if ((to_set &  SETATTR_UID)) {
//    stat.set_user_id(user_mapping_->UIDToUsername(uid));
//  }
//  if ((to_set &  SETATTR_GID)) {
//    stat.set_group_id(user_mapping_->GIDToGroupname(gid));
//  }
//
//  try {
//    volume_->SetAttr(user_credentials, string(path), stat,
//                     static_cast<Setattrs>(to_set));
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
//int DokanAdapter::setxattr(
//    const char *path, const char *name, const char *value,
//    size_t size, int flags) {
//  bool xtreemfs_attribute_requested = !strncmp(name, "xtreemfs.", 9);
//  if (!options_->enable_xattrs && !xtreemfs_attribute_requested) {
//    return -1 * ENOTSUP;
//  }
//
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    // Dokan reuses the value parameter. Therefore we may only use "size"
//    // (null-termination is not counted) characters of value.
//    volume_->SetXAttr(user_credentials,
//                      path,
//                      string(name),
//                      string(value, size),
//                      static_cast<xtreemfs::pbrpc::XATTR_FLAGS>(flags));
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//  return 0;
//}
//
//int DokanAdapter::listxattr(const char *path, char *list, size_t size) {
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    boost::scoped_ptr<listxattrResponse> xattrs(
//        volume_->ListXAttrs(user_credentials, path));
//
//    int needed_size = 0;
//    for (int i = 0; i < xattrs->xattrs_size(); i++) {
//      needed_size += xattrs->xattrs(i).name().size() + 1;
//    }
//
//    if (size == 0) {
//      return needed_size;
//    } else {
//      if (size < needed_size) {
//        return -1 * ERANGE;
//      }
//
//      for (int i = 0; i < xattrs->xattrs_size(); i++) {
//        // If xattrs are not enabled, only copy "xtreemfs." attributes.
//        if (options_->enable_xattrs
//            || !strncmp(xattrs->xattrs(i).name().c_str(), "xtreemfs.", 9)) {
//          memcpy(list,
//                 xattrs->xattrs(i).name().c_str(),
//                 xattrs->xattrs(i).name().size() + 1);
//          list += xattrs->xattrs(i).name().size() + 1;
//        }
//      }
//      return needed_size;
//    }
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}
//
//int DokanAdapter::removexattr(const char *path, const char *name) {
//  bool xtreemfs_attribute_requested = !strncmp(name, "xtreemfs.", 9);
//  if (!options_->enable_xattrs && !xtreemfs_attribute_requested) {
//    return -1 * ENOTSUP;
//  }
//
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    volume_->RemoveXAttr(user_credentials, path, string(name));
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//  return 0;
//}
//
//int DokanAdapter::lock(const char* path, struct dokan_file_info *fi, int cmd,
//                      struct flock* flock) {
//  const string path_str(path);
//  if (xctl_.checkXctlFile(path_str)) {
//    // Locking not supported for xctl files.
//    // Some apps such as echo try to lock the file before writing.
//    return -1 * EIO;
//  }
//  // Dokan sets flock->l_whence always to SEEK_SET, meaning the offset has to be
//  // interpreted relative to the start of the file.
//  UserCredentials user_credentials;
//  GenerateUserCredentials(dokan_get_context(), &user_credentials);
//
//  try {
//    FileHandle* file_handle = reinterpret_cast<FileHandle*>(fi->fh);
//
//    if (cmd == F_GETLK) {
//      // Only check if the lock could get acquired.
//      boost::shared_ptr<Lock> checked_lock(file_handle->CheckLock(
//          user_credentials,
//          flock->l_pid,
//          flock->l_start,
//          flock->l_len,
//          (flock->l_type == F_WRLCK)));
//      if (checked_lock->client_pid() == flock->l_pid) {
//        flock->l_type = F_UNLCK;
//      } else {
//        // There exists a conflicting lock, tell which one:
//        flock->l_type = checked_lock->exclusive() ? F_WRLCK : F_RDLCK;
//        flock->l_start = checked_lock->offset();
//        flock->l_len = checked_lock->length();
//        flock->l_pid = checked_lock->client_pid();
//      }
//    } else if (cmd == F_SETLK || cmd == F_SETLKW) {
//      // Set the lock (type = F_RDLCK|F_WRLCK) or release it (type = F_UNLCK).
//      if (flock->l_type == F_RDLCK || flock->l_type == F_WRLCK) {
//        boost::shared_ptr<Lock> checked_lock(file_handle->AcquireLock(
//            user_credentials,
//            flock->l_pid,
//            flock->l_start,
//            flock->l_len,
//            (flock->l_type == F_WRLCK),
//            (cmd == F_SETLKW)));
//      } else if (flock->l_type == F_UNLCK) {
//        file_handle->ReleaseLock(user_credentials,
//                                 flock->l_pid,
//                                 flock->l_start,
//                                 flock->l_len,
//                                 (flock->l_type == F_WRLCK));
//      } else {
//        Logging::log->getLog(LEVEL_ERROR) << "Unknown flock->l_type set:"
//            << flock->l_type << endl;
//        return -1 * EIO;
//      }
//    }
//  } catch(const PosixErrorException& e) {
//    return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//  } catch(const XtreemFSException& e) {
//    return -1 * EIO;
//  } catch(const exception& e) {
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occured: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}

}  // namespace xtreemfs
