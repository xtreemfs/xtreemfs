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

#include "fcntl.h"
#include "dokan/dokan_options.h"
#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/helper.h"
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

#define CATCH_AND_CONVERT_ERRORS \
  catch (const PosixErrorException& e) { \
    return ConvertXtreemFSErrnoToDokan(e.posix_errno()); \
  } catch (const XtreemFSException&) { \
    return -1 * EIO; \
  } catch (const exception& e) { \
    ErrorLog::error_log->AppendError( \
        "A non-XtreemFS exception occurred: " \
        + string(e.what())); \
    return -1 * EIO; \
  }

namespace xtreemfs {

static int ConvertXtreemFSErrnoToDokan(
    xtreemfs::pbrpc::POSIXErrno xtreemfs_errno) {
  switch (xtreemfs_errno) {
    case POSIX_ERROR_EACCES:
      return -ERROR_ACCESS_DENIED;
    case POSIX_ERROR_EEXIST:
      return -ERROR_ALREADY_EXISTS;
    case POSIX_ERROR_ENOENT:
      return -ERROR_FILE_NOT_FOUND;
    // TODO: map remaining ones
    case POSIX_ERROR_EPERM:
      return EPERM;
    case POSIX_ERROR_EINTR:
      return EINTR;
    case POSIX_ERROR_EIO:
      return EIO;
    case POSIX_ERROR_EAGAIN:
      return EAGAIN;
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

static std::string WindowsPathToUTF8Unix(const wchar_t* from) {
  string utf8;
  ConvertWindowsToUTF8(from, &utf8);

  // Suppress trailing slash.
  if (utf8.length() > 1 && utf8[utf8.length() - 1] == '\\') {
    utf8.resize(utf8.length() - 1);
  }
  
  // Replace Windows path delimiters with Unix ones.
  char* pos = &(utf8[0]);
  while (*pos != 0) { 
    if (*pos == '\\') {
      *pos = '/';
    }
    pos++;
  }

  return utf8;
}

static void UTF8UnixPathEntryToWindows(const std::string& utf8,
                                       wchar_t* buf,
                                       int buffer_size) {
  ConvertUTF8ToWindows(utf8, buf, buffer_size);
}

static xtreemfs::FileHandle* file_handle(PDOKAN_FILE_INFO dokan_file_info) {
  return reinterpret_cast<xtreemfs::FileHandle*>(dokan_file_info->Context);
}

static void XtreemFSTimeToWinTime(uint64_t utime_ns,
                                  unsigned long* lower,
                                  unsigned long* upper) {
  // TODO(fhupfeld): apparently XtreemFS time is not nanoseconds since epoch?
  utime_ns = utime_ns / 1000000000;  // to seconds
  uint64_t wintime = (utime_ns + 11644473600) * 10000000;
  *lower = static_cast<DWORD>(wintime & 0xFFFFffff);
  *upper = static_cast<DWORD>(wintime >> 32);
}

static uint64_t WinTimeToUnixTime(unsigned long upper,
                                  unsigned long lower) {
  if (lower == 0 && upper == 0) {
    return 0;
  }
  if (upper == 0xFFFFFFFF && lower == 0xFFFFFFFF) {
    return -1;  // Error
  }

  int64_t utime = static_cast<uint64_t>(upper) << 32 | 
      static_cast<uint64_t>(lower);
  return utime / 10000000 - 11644473600;
}

DokanAdapter::DokanAdapter(DokanOptions* options)
    : options_(options), xctl_("/.xctl$$$") {
}

DokanAdapter::~DokanAdapter() {}

int DokanAdapter::CreateFile(LPCWSTR path,
                             DWORD desired_access,
                             DWORD share_mode,
                             DWORD creation_disposition,
                             DWORD flags_and_attributes,
                             PDOKAN_FILE_INFO dokan_file_info) {
  UserCredentials user_credentials;
  system_user_mapping_->GetUserCredentialsForCurrentUser(
      &user_credentials);
  
  bool read = false;
  bool write = false;

  read |= (desired_access & FILE_READ_ATTRIBUTES ) != 0;
  read |= (desired_access & FILE_READ_DATA ) != 0;
  read |= (desired_access & FILE_READ_EA ) != 0;
  write |= (desired_access & FILE_WRITE_ATTRIBUTES ) != 0;
  write |= (desired_access & FILE_WRITE_DATA ) != 0;
  write |= (desired_access & FILE_WRITE_EA ) != 0;
  
  int open_flags = 0;
  if (read && write) {
    open_flags = SYSTEM_V_FCNTL_H_O_RDWR;
  } else if (read) {
    open_flags = SYSTEM_V_FCNTL_H_O_RDONLY;
  } else if (write) {
    open_flags = SYSTEM_V_FCNTL_H_O_WRONLY;
  }

  if ((desired_access & FILE_APPEND_DATA ) != 0) {
    open_flags |= SYSTEM_V_FCNTL_H_O_APPEND;
  }

  uint32_t file_attributes = 0600;
  if ((flags_and_attributes & FILE_ATTRIBUTE_READONLY) != 0) {
    file_attributes |= 0400;
  }

  dokan_file_info->IsDirectory = FALSE;
  dokan_file_info->Context = NULL;

  if (creation_disposition == CREATE_NEW) {
    // Create the file only if it does not exist already
    open_flags |= SYSTEM_V_FCNTL_H_O_CREAT | SYSTEM_V_FCNTL_H_O_EXCL;
  } else if (creation_disposition == CREATE_ALWAYS) {
     // Create the file and overwrite it if it exists
    open_flags |= SYSTEM_V_FCNTL_H_O_CREAT;
  } else if (creation_disposition == OPEN_ALWAYS) {
    // Open an existing file; create if it doesn't exist
    open_flags |= SYSTEM_V_FCNTL_H_O_CREAT | SYSTEM_V_FCNTL_H_O_EXCL;
  } else if (creation_disposition == TRUNCATE_EXISTING) {
    // Only open an existing file and truncate it
    open_flags |= SYSTEM_V_FCNTL_H_O_TRUNC;
  }

  xtreemfs::pbrpc::SYSTEM_V_FCNTL sys_v_open =
      static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(open_flags);
  
  try {
    if (creation_disposition == CREATE_ALWAYS) {
      try {
        volume_->Unlink(user_credentials,
                        WindowsPathToUTF8Unix(path));
      } catch(const PosixErrorException&) {
        try {
          volume_->DeleteDirectory(user_credentials,
                                    WindowsPathToUTF8Unix(path));
          dokan_file_info->IsDirectory = TRUE;
          return ERROR_SUCCESS;
        } catch(const PosixErrorException&) {
        }
      }
    }

    if (creation_disposition == OPEN_EXISTING) {
      if (WindowsPathToUTF8Unix(path) == "/") {
        dokan_file_info->IsDirectory = TRUE;
        return 0;
      }
       
      // Don't catch: if it does not exist, escalate.
      xtreemfs::pbrpc::Stat stat;
      volume_->GetAttr(user_credentials,
                        WindowsPathToUTF8Unix(path),
                        true,  // bypass cache
                        &stat);

      if (stat.attributes()) {
        dokan_file_info->IsDirectory = TRUE;
        return 0;
      }
    }

    xtreemfs::FileHandle* file_handle = 
        volume_->OpenFile(user_credentials, 
                          WindowsPathToUTF8Unix(path),
                          sys_v_open,
                          file_attributes);
    // Needed?
    if (creation_disposition == TRUNCATE_EXISTING) {
      volume_->Truncate(user_credentials, WindowsPathToUTF8Unix(path), 0);
    }

    dokan_file_info->Context = reinterpret_cast<UINT64>(file_handle);
    return 0;  // always?
  } CATCH_AND_CONVERT_ERRORS
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
    LPCWSTR file_name, PDOKAN_FILE_INFO dokan_file_info) {
  xtreemfs::FileHandle* file = file_handle(dokan_file_info);
  if (file == NULL) {
    return -1;  // TODO
  }

  try {
    file->Close();
    dokan_file_info->Context = NULL;
    return 0;
  } CATCH_AND_CONVERT_ERRORS
}

int DokanAdapter::ReadFile(
    LPCWSTR FileName,
    LPVOID Buffer,
    DWORD NumberOfBytesToRead,
    LPDWORD NumberOfBytesRead,
    LONGLONG Offset,
    PDOKAN_FILE_INFO dokan_file_info) {
  UserCredentials user_credentials;
  system_user_mapping_->GetUserCredentialsForCurrentUser(
      &user_credentials);
  xtreemfs::FileHandle* file = file_handle(dokan_file_info);
  if (file == NULL) {
    return -1;  // TODO
  }

  try {
    *NumberOfBytesRead = file->Read(user_credentials, 
                                    static_cast<char*>(Buffer), 
                                    NumberOfBytesToRead, 
                                    static_cast<off_t>(Offset));
    dokan_file_info->Context = NULL;
    return 0;
  } CATCH_AND_CONVERT_ERRORS
}

int DokanAdapter::WriteFile(
    LPCWSTR FileName,
    LPCVOID Buffer,
    DWORD NumberOfBytesToWrite,
    LPDWORD NumberOfBytesWritten,
    LONGLONG Offset,
    PDOKAN_FILE_INFO dokan_file_info) {
  UserCredentials user_credentials;
  system_user_mapping_->GetUserCredentialsForCurrentUser(
      &user_credentials);
  xtreemfs::FileHandle* file = file_handle(dokan_file_info);
  if (file == NULL) {
    return -1;  // TODO
  }

  try {
    *NumberOfBytesWritten = file->Write(user_credentials, 
                                        static_cast<const char*>(Buffer), 
                                        NumberOfBytesToWrite, 
                                        static_cast<off_t>(Offset));
    dokan_file_info->Context = NULL;
    return 0;
  } CATCH_AND_CONVERT_ERRORS
}

int DokanAdapter::FlushFileBuffers(
    LPCWSTR,
    PDOKAN_FILE_INFO dokan_file_info) {
  xtreemfs::FileHandle* file = file_handle(dokan_file_info);
  if (file == NULL) {
    return -1;  // TODO
  }

  try {
    file->Flush();
    dokan_file_info->Context = NULL;
    return 0;
  } CATCH_AND_CONVERT_ERRORS
}

int DokanAdapter::GetFileInformation(
    LPCWSTR path,
    LPBY_HANDLE_FILE_INFORMATION file_info,
    PDOKAN_FILE_INFO dokan_file_info) {
  UserCredentials user_credentials;
  system_user_mapping_->GetUserCredentialsForCurrentUser(
      &user_credentials);
  
  try {
    xtreemfs::pbrpc::Stat stat;
    volume_->GetAttr(
        user_credentials,
        WindowsPathToUTF8Unix(path),
        &stat);
    file_info->nFileSizeHigh = static_cast<DWORD>(stat.size() >> 32);
    file_info->nFileSizeLow = stat.size() & 0xFFFFffff;
    XtreemFSTimeToWinTime(stat.atime_ns(),
        &file_info->ftLastAccessTime.dwHighDateTime,
        &file_info->ftLastAccessTime.dwLowDateTime);
    XtreemFSTimeToWinTime(stat.ctime_ns(),
        &file_info->ftCreationTime.dwHighDateTime,
        &file_info->ftCreationTime.dwLowDateTime);
    XtreemFSTimeToWinTime(stat.mtime_ns(),
        &file_info->ftLastWriteTime.dwHighDateTime,
        &file_info->ftLastWriteTime.dwLowDateTime);
    // TODO: not complete yet.
    return 0;
  } CATCH_AND_CONVERT_ERRORS
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
          user_credentials,
          WindowsPathToUTF8Unix(path),
          offset,
          options_->readdir_chunk_size,
          false);

      for (int i = 0; i < entries->entries_size(); i++) {
        const DirectoryEntry& entry = entries->entries(i);

        if (entry.name() == "." || entry.name() == "..") {
          continue;
        }

        WIN32_FIND_DATA find_data;
        UTF8UnixPathEntryToWindows(entry.name(), find_data.cFileName, 260);
        find_data.nFileSizeHigh = static_cast<DWORD>(entry.stbuf().size() >> 32);
        find_data.nFileSizeLow = entry.stbuf().size() & 0xFFFFffff;
        XtreemFSTimeToWinTime(entry.stbuf().atime_ns(),
            &find_data.ftLastAccessTime.dwHighDateTime,
            &find_data.ftLastAccessTime.dwLowDateTime);
        XtreemFSTimeToWinTime(entry.stbuf().ctime_ns(),
            &find_data.ftCreationTime.dwHighDateTime,
            &find_data.ftCreationTime.dwLowDateTime);
        XtreemFSTimeToWinTime(entry.stbuf().mtime_ns(),
            &find_data.ftLastWriteTime.dwHighDateTime,
            &find_data.ftLastWriteTime.dwLowDateTime);
        // TODO: not complete yet.
        callback(&find_data, dokan_file_info);
      }

      if (entries->entries_size() == 0 ||
          entries->entries_size() < options_->readdir_chunk_size) {
        break;
      }
    }
    return 0;
  } CATCH_AND_CONVERT_ERRORS
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
    LPWSTR VolumeNameBuffer,
    DWORD VolumeNameSize,
    LPDWORD VolumeSerialNumber,
    LPDWORD MaximumComponentLength,
    LPDWORD FileSystemFlags,
    LPWSTR FileSystemNameBuffer,
    DWORD FileSystemNameSize,
    PDOKAN_FILE_INFO) {
  ConvertUTF8ToWindows(
      options_->volume_name, VolumeNameBuffer, VolumeNameSize);
  FileSystemNameBuffer = L"XtreemFS";
  *VolumeSerialNumber = 0;  // TODO(fhupfeld): volume uuid
  *MaximumComponentLength = 256;
  *FileSystemFlags = FILE_CASE_PRESERVED_NAMES | 
      FILE_CASE_SENSITIVE_SEARCH | FILE_UNICODE_ON_DISK;
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

//
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
//              + string(e.what()));
//    return -1 * EIO;
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//      // We dont care if errors occurred.
//    }
//
//    try {
//      file_handle->Close();
//    } catch(const PosixErrorException& e) {
//      return -1 * ConvertXtreemFSErrnoToDokan(e.posix_errno());
//    } catch(const XtreemFSException& e) {
//      return -1 * EIO;
//    } catch(const exception& e) {
//      ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
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
//    ErrorLog::error_log->AppendError("A non-XtreemFS exception occurred: "
//              + string(e.what()));
//    return -1 * EIO;
//  }
//
//  return 0;
//}

}  // namespace xtreemfs
