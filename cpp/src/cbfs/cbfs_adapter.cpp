/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "cbfs/cbfs_adapter.h"

#include <string>

#include "cbfs/cbfs_enumeration_context.h"
#include "cbfs/cbfs_license.h"
#include "cbfs/cbfs_options.h"
#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/system_user_mapping.h"
#include "libxtreemfs/user_mapping.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"
#include "util/error_log.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {

#define DelegateCheckFlagExt(val, flag, pre, post) \
    if ((val) & flag) { DbgPrint(L"%s" L#flag L"%s", (pre), (post)); }

#define DelegateCheckFlag(val, flag) \
    DelegateCheckFlagExt((val), ##flag, L"\t", L"\n")

#define CATCH_AND_CONVERT_ERRORS \
catch (const PosixErrorException& e) { \
  throw ECBFSError(ConvertXtreemFSErrnoToWindows(e.posix_errno())); \
} catch (const XtreemFSException&) { \
  throw ECBFSError(EIO); \
} catch (const exception& e) { \
  ErrorLog::error_log->AppendError( \
      "A non-XtreemFS exception occurred: " \
      + string(e.what())); \
  throw ECBFSError(EIO); \
}

/** Calls Sender->GetTag() to return the current CbFSAdapter instance. */
static xtreemfs::CbFSAdapter* Adapter(CallbackFileSystem* Sender) {
  return reinterpret_cast<xtreemfs::CbFSAdapter*>(Sender->GetTag());
}

/** Unimplemented. */
static void DelegateMount(CallbackFileSystem* sender) {}

/** Unimplemented. */
static void DelegateUnmount(CallbackFileSystem* sender) {}

static void DelegateGetVolumeSize(CallbackFileSystem* Sender,
                                  __int64* TotalNumberOfSectors,
                                  __int64* NumberOfFreeSectors) {
  Adapter(Sender)->GetVolumeSize(Sender,TotalNumberOfSectors, NumberOfFreeSectors);  // NOLINT
}

static void DelegateGetVolumeLabel(CallbackFileSystem* Sender,
                                   LPTSTR VolumeLabel) {
  Adapter(Sender)->GetVolumeLabel(Sender, VolumeLabel);
}

static void DelegateSetVolumeLabel(CallbackFileSystem* Sender,
                                   LPCTSTR VolumeLabel) {
  throw ECBFSError(ERROR_NOT_SUPPORTED);
}

static void DelegateGetVolumeId(CallbackFileSystem* Sender, PDWORD VolumeID) {
  Adapter(Sender)->GetVolumeId(Sender, VolumeID);
}

static void DelegateCreateFile(CallbackFileSystem* Sender,
                               LPCTSTR FileName,
                               ACCESS_MASK DesiredAccess,
                               DWORD FileAttributes,
                               DWORD ShareMode,
                               CbFsFileInfo* FileInfo,
                               CbFsHandleInfo* HandleInfo) {
  Adapter(Sender)->CreateFile(Sender, FileName, DesiredAccess, FileAttributes, ShareMode, FileInfo, HandleInfo);  // NOLINT

  CbFSAdapter::DebugPrintCreateFile(L"CreateFile", FileName, DesiredAccess, FileAttributes, ShareMode, FileInfo, HandleInfo);
}

static void DelegateOpenFile(CallbackFileSystem* Sender,
                             LPCTSTR FileName,
                             ACCESS_MASK DesiredAccess,
                             DWORD FileAttributes,
                             DWORD ShareMode,
                             CbFsFileInfo* FileInfo,
                             CbFsHandleInfo* HandleInfo) {
  Adapter(Sender)->OpenFile(Sender, FileName, DesiredAccess, FileAttributes, ShareMode, FileInfo, HandleInfo);  // NOLINT

  CbFSAdapter::DebugPrintCreateFile(L"OpenFile", FileName, DesiredAccess, FileAttributes, ShareMode, FileInfo, HandleInfo);
}

static void DelegateCloseFile(CallbackFileSystem* Sender,
                              CbFsFileInfo* FileInfo,
                              CbFsHandleInfo* HandleInfo) {
  if (HandleInfo != NULL && Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "CloseFile " << CbFSAdapter::WindowsPathToUTF8Unix(FileInfo->get_FileNameBuffer())
        << " handle: 0x" << HandleInfo->get_UserContext() << endl;
  }
  Adapter(Sender)->CloseFile(Sender, FileInfo, HandleInfo);
}

static void DelegateGetFileInfo(CallbackFileSystem* Sender,
                                LPCTSTR FileName,
                                LPBOOL FileExists,
                                PFILETIME CreationTime,
                                PFILETIME LastAccessTime,
                                PFILETIME LastWriteTime,
                                PFILETIME ChangeTime,
                                __int64* EndOfFile,
                                __int64* AllocationSize,
                                __int64* FileId,
                                PDWORD FileAttributes,
                                PDWORD NumberOfLinks,
                                LPTSTR ShortFileName OPTIONAL,
                                PWORD ShortFileNameLength OPTIONAL,
                                LPTSTR RealFileName OPTIONAL,
                                PWORD RealFileNameLength OPTIONAL) {
  Adapter(Sender)->GetFileInfo(Sender, FileName, FileExists, CreationTime, LastAccessTime, LastWriteTime, ChangeTime, EndOfFile, AllocationSize, FileId, FileAttributes, ShortFileName, ShortFileNameLength, RealFileName, RealFileNameLength);  // NOLINT

  CbFSAdapter::DebugPrintCreateFile(L"GetFileInfo", FileName, 0, *FileAttributes, 0, 0, 0);
}

static void DelegateEnumerateDirectory(CallbackFileSystem* Sender,
                                       CbFsFileInfo* DirectoryInfo,
                                       CbFsHandleInfo *HandleInfo,
                                       CbFsDirectoryEnumerationInfo* EnumerationInfo,
                                       LPCTSTR Mask,
                                       INT Index,
                                       BOOL Restart,
                                       LPBOOL FileFound,
                                       LPTSTR FileName,
                                       PDWORD FileNameLength,
                                       LPTSTR ShortFileName OPTIONAL,
                                       PUCHAR ShortFileNameLength OPTIONAL,
                                       PFILETIME CreationTime,
                                       PFILETIME LastAccessTime,
                                       PFILETIME LastWriteTime,
                                       PFILETIME ChangeTime,
                                       __int64* EndOfFile,
                                       __int64* AllocationSize,
                                       __int64* FileId,
                                       PDWORD FileAttributes) {
  Adapter(Sender)->EnumerateDirectory(Sender, DirectoryInfo, HandleInfo, EnumerationInfo, Mask, Index, Restart, FileFound, FileName, FileNameLength, ShortFileName, ShortFileNameLength, CreationTime, LastAccessTime, LastWriteTime, ChangeTime, EndOfFile, AllocationSize, FileId, FileAttributes);  // NOLINT
}

static void DelegateCloseDirectoryEnumeration(CallbackFileSystem* Sender,
                                     CbFsFileInfo* DirectoryInfo,
                                     CbFsDirectoryEnumerationInfo* EnumerationInfo) {
  Adapter(Sender)->CloseDirectoryEnumeration(Sender, DirectoryInfo, EnumerationInfo);
}

static void DelegateSetFileAttributes(CallbackFileSystem* Sender,
                                      CbFsFileInfo* FileInfo,
                                      CbFsHandleInfo* HandleInfo,
                                      PFILETIME CreationTime,
                                      PFILETIME LastAccessTime,
                                      PFILETIME LastWriteTime,
                                      PFILETIME ChangeTime,
                                      DWORD FileAttributes) {
  CbFSAdapter::DebugPrintCreateFile(L"SetFileAttributes", FileInfo->get_FileNameBuffer(), 0, FileAttributes, 0, 0, 0);

  Adapter(Sender)->SetFileAttributes(Sender, FileInfo, HandleInfo, CreationTime, LastAccessTime, LastWriteTime, ChangeTime, FileAttributes);  // NOLINT
}

static void DelegateCanFileBeDeleted(CallbackFileSystem* Sender,
                                     CbFsFileInfo* FileInfo,
                                     CbFsHandleInfo* HandleInfo,
                                     BOOL* CanBeDeleted) {
  Adapter(Sender)->CanFileBeDeleted(Sender, FileInfo, HandleInfo, CanBeDeleted);
}


static void DelegateDeleteFile(CallbackFileSystem* Sender,
                               CbFsFileInfo* FileInfo) {
  Adapter(Sender)->DeleteFile(Sender, FileInfo);
}

/** Unimplemented. */
static void DelegateSetAllocationSize(CallbackFileSystem* Sender,
                                      CbFsFileInfo* FileInfo,
                                      __int64 AllocationSize) {}

static void DelegateSetEndOfFile(CallbackFileSystem* Sender,
                                 CbFsFileInfo* FileInfo,
                                 __int64 EndOfFile) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "SetEndOfFile " << CbFSAdapter::WindowsPathToUTF8Unix(FileInfo->get_FileNameBuffer())
        << " handle: 0x" << FileInfo->get_UserContext() << " s: " << EndOfFile << endl;
  }

  Adapter(Sender)->SetEndOfFile(Sender, FileInfo, EndOfFile);
}

static void DelegateRenameOrMoveFile(CallbackFileSystem* Sender,
                                     CbFsFileInfo* FileInfo,
                                     LPCTSTR NewFileName) {
  Adapter(Sender)->RenameOrMoveFile(Sender, FileInfo, NewFileName);
}

static void DelegateReadFile(CallbackFileSystem* Sender,
                             CbFsFileInfo* FileInfo,
                             __int64 Position,
                             PVOID Buffer, 
                             DWORD BytesToRead,
                             PDWORD BytesRead) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "ReadFile " << CbFSAdapter::WindowsPathToUTF8Unix(FileInfo->get_FileNameBuffer())
        << " handle: 0x" << FileInfo->get_UserContext() << " s: " << BytesToRead <<  " o:" << Position << endl;
  }

  Adapter(Sender)->ReadFile(Sender, FileInfo, Position, Buffer, BytesToRead, BytesRead);  // NOLINT

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "ReadFile succeeded " << CbFSAdapter::WindowsPathToUTF8Unix(FileInfo->get_FileNameBuffer())
        << " handle: 0x" << FileInfo->get_UserContext() << " s:" << BytesToRead <<  " o:" << Position << " r:" << *BytesRead << endl;
  }
}

static void DelegateWriteFile(CallbackFileSystem* Sender,
                              CbFsFileInfo* FileInfo,
                              __int64 Position,
                              PVOID Buffer, 
                              DWORD BytesToWrite,
                              PDWORD BytesWritten) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)  << "WriteFile " << CbFSAdapter::WindowsPathToUTF8Unix(FileInfo->get_FileNameBuffer())
        << " handle: 0x" << FileInfo->get_UserContext() << " s: " << BytesToWrite <<  " o:" << Position << endl;
  }

  Adapter(Sender)->WriteFile(Sender, FileInfo, Position, Buffer, BytesToWrite, BytesWritten);  // NOLINT

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "WriteFile succeeded " << CbFSAdapter::WindowsPathToUTF8Unix(FileInfo->get_FileNameBuffer())
        << " handle: 0x" << FileInfo->get_UserContext() << " s:" << BytesToWrite <<  " o:" << Position << " w:" << *BytesWritten << endl;
  }
}

static void DelegateIsDirectoryEmpty(CallbackFileSystem* Sender,
                                     CbFsFileInfo* DirectoryInfo,
                                     LPCWSTR FileName,
                                     LPBOOL IsEmpty) {
  Adapter(Sender)->IsDirectoryEmpty(Sender, DirectoryInfo, FileName, IsEmpty);
}

static void DelegateStorageEjected(CallbackFileSystem* Sender) {
  Adapter(Sender)->StorageEjected(Sender);
}

void CbFSAdapter::DbgPrint(LPCWSTR format, ...) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    WCHAR buffer[512];
    va_list argp;
    va_start(argp, format);
    vswprintf_s(buffer, sizeof(buffer)/sizeof(WCHAR), format, argp);
    va_end(argp);

    Logging::log->getLog(LEVEL_DEBUG) << ConvertWindowsToUTF8(buffer);
  }
}

void CbFSAdapter::GetVolumeSize(CallbackFileSystem* Sender,
                                __int64* TotalNumberOfSectors,
                                __int64* NumberOfFreeSectors) {
  try {  
    boost::scoped_ptr<StatVFS> stat(volume_->StatFS(user_credentials_));
    *NumberOfFreeSectors =
        stat->bavail() * stat->bsize() / cbfs_.GetSectorSize();
    *TotalNumberOfSectors =
        stat->blocks() * stat->bsize() / cbfs_.GetSectorSize();
  } CATCH_AND_CONVERT_ERRORS
}

void CbFSAdapter::GetVolumeLabel(CallbackFileSystem* Sender,
                                 LPTSTR VolumeLabel) {
  wcsncpy(VolumeLabel, volume_label_.c_str(), kMaxVolumeLabelLength);
  VolumeLabel[kMaxVolumeLabelLength] = '\0';
}

void CbFSAdapter::GetVolumeId(CallbackFileSystem* Sender, PDWORD VolumeID) {
  // TODO(mberlin): What to fill in here?
  *VolumeID = 0x12345678;
}

void CbFSAdapter::DebugPrintCreateFile(
    LPCWSTR OperationType,
    LPCTSTR FileName,
    ACCESS_MASK DesiredAccess,
    DWORD FileAttributes,
    DWORD ShareMode,
    CbFsFileInfo* FileInfo,
    CbFsHandleInfo* HandleInfo) {
  DbgPrint(L"%s : %s File : 0x%x Handle : 0x%x\n", OperationType, FileName, FileInfo ? FileInfo->get_UserContext() : NULL, HandleInfo ? HandleInfo->get_UserContext() : NULL);

  DbgPrint(L"\tShareMode = 0x%x\n", ShareMode);

  DelegateCheckFlag(ShareMode, FILE_SHARE_READ);
  DelegateCheckFlag(ShareMode, FILE_SHARE_WRITE);
  DelegateCheckFlag(ShareMode, FILE_SHARE_DELETE);

  DbgPrint(L"\tDesiredAccess = 0x%x\n", DesiredAccess);

  DelegateCheckFlag(DesiredAccess, GENERIC_READ);
  DelegateCheckFlag(DesiredAccess, GENERIC_WRITE);
  DelegateCheckFlag(DesiredAccess, GENERIC_EXECUTE);
  
  DelegateCheckFlag(DesiredAccess, DELETE);
  DelegateCheckFlag(DesiredAccess, FILE_READ_DATA);
  DelegateCheckFlag(DesiredAccess, FILE_READ_ATTRIBUTES);
  DelegateCheckFlag(DesiredAccess, FILE_READ_EA);
  DelegateCheckFlag(DesiredAccess, READ_CONTROL);
  DelegateCheckFlag(DesiredAccess, FILE_WRITE_DATA);
  DelegateCheckFlag(DesiredAccess, FILE_WRITE_ATTRIBUTES);
  DelegateCheckFlag(DesiredAccess, FILE_WRITE_EA);
  DelegateCheckFlag(DesiredAccess, FILE_APPEND_DATA);
  DelegateCheckFlag(DesiredAccess, WRITE_DAC);
  DelegateCheckFlag(DesiredAccess, WRITE_OWNER);
  DelegateCheckFlag(DesiredAccess, SYNCHRONIZE);
  DelegateCheckFlag(DesiredAccess, FILE_EXECUTE);
  DelegateCheckFlag(DesiredAccess, STANDARD_RIGHTS_READ);
  DelegateCheckFlag(DesiredAccess, STANDARD_RIGHTS_WRITE);
  DelegateCheckFlag(DesiredAccess, STANDARD_RIGHTS_EXECUTE);

  DbgPrint(L"\tFileAttributes = 0x%x\n", FileAttributes);

  DelegateCheckFlag(FileAttributes, FILE_ATTRIBUTE_ARCHIVE);
  DelegateCheckFlag(FileAttributes, FILE_ATTRIBUTE_ENCRYPTED);
  DelegateCheckFlag(FileAttributes, FILE_ATTRIBUTE_HIDDEN);
  DelegateCheckFlag(FileAttributes, FILE_ATTRIBUTE_NORMAL);
  DelegateCheckFlag(FileAttributes, FILE_ATTRIBUTE_NOT_CONTENT_INDEXED);
  DelegateCheckFlag(FileAttributes, FILE_ATTRIBUTE_OFFLINE);
  DelegateCheckFlag(FileAttributes, FILE_ATTRIBUTE_READONLY);
  DelegateCheckFlag(FileAttributes, FILE_ATTRIBUTE_SYSTEM);
  DelegateCheckFlag(FileAttributes, FILE_ATTRIBUTE_TEMPORARY);
  DelegateCheckFlag(FileAttributes, FILE_FLAG_WRITE_THROUGH);
  DelegateCheckFlag(FileAttributes, FILE_FLAG_OVERLAPPED);
  DelegateCheckFlag(FileAttributes, FILE_FLAG_NO_BUFFERING);
  DelegateCheckFlag(FileAttributes, FILE_FLAG_RANDOM_ACCESS);
  DelegateCheckFlag(FileAttributes, FILE_FLAG_SEQUENTIAL_SCAN);
  DelegateCheckFlag(FileAttributes, FILE_FLAG_DELETE_ON_CLOSE);
  DelegateCheckFlag(FileAttributes, FILE_FLAG_BACKUP_SEMANTICS);
  DelegateCheckFlag(FileAttributes, FILE_FLAG_POSIX_SEMANTICS);
  DelegateCheckFlag(FileAttributes, FILE_FLAG_OPEN_REPARSE_POINT);
  DelegateCheckFlag(FileAttributes, FILE_FLAG_OPEN_NO_RECALL);
  DelegateCheckFlag(FileAttributes, SECURITY_ANONYMOUS);
  DelegateCheckFlag(FileAttributes, SECURITY_IDENTIFICATION);
  DelegateCheckFlag(FileAttributes, SECURITY_IMPERSONATION);
  DelegateCheckFlag(FileAttributes, SECURITY_DELEGATION);
  DelegateCheckFlag(FileAttributes, SECURITY_CONTEXT_TRACKING);
  DelegateCheckFlag(FileAttributes, SECURITY_EFFECTIVE_ONLY);
  DelegateCheckFlag(FileAttributes, SECURITY_SQOS_PRESENT);

  DbgPrint(L"\n");
}

void CbFSAdapter::CreateFile(CallbackFileSystem* Sender,
                             LPCTSTR FileName,
                             ACCESS_MASK DesiredAccess,
                             DWORD FileAttributes,
                             DWORD ShareMode,
                             CbFsFileInfo* FileInfo,
                             CbFsHandleInfo* HandleInfo) {
  string path(WindowsPathToUTF8Unix(FileName));

  if (FileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
    try {
      // TODO(mberlin): Let user specify a different default mode.
      // TODO(mberlin): Store attributes for directories?
      volume_->MakeDirectory(user_credentials_, path, 0777);
    } CATCH_AND_CONVERT_ERRORS
  } else {
    // What to do if file exists and this is called? Use O_TRUNC, because:
    // CbFS FAQ:
    // "So if your OnCreateFile handler is called, you need to create the file.
    //  If the file exists and CBFS knows it, receiving OnCreateFile means that
    //  the file was requested for opening with "CreateAlways" flag, which
    //  means that you need to truncate the existing file."
    // Source: http://www.eldos.com/cbfs/articles/6747.php

    // TODO(mberlin): What happens if path is a dir and a file shall be created?

    // TODO(mberlin): Evaluation of ShareMode can only be implemented by locking
    //                the file. See MSDN description of CreateFile:
    // http://msdn.microsoft.com/en-us/library/windows/desktop/aa363858%28v=vs.85%29.aspx // NOLINT
    try {
      // TODO(mberlin): Let user specify a different default mode.
      uint32_t mode = 0666;
      if ((FileAttributes & FILE_ATTRIBUTE_READONLY) != 0) {
        mode = 0444;
      }

      int open_flags = ConvertFlagsWindowsToXtreemFS(DesiredAccess)
          | SYSTEM_V_FCNTL_H_O_CREAT
          | SYSTEM_V_FCNTL_H_O_TRUNC;

      FileHandle* file_handle = volume_->OpenFile(
          user_credentials_,
          path,
          static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(open_flags),
          mode,
          FileAttributes);
      FileInfo->set_UserContext(reinterpret_cast<PVOID>(file_handle));
    } CATCH_AND_CONVERT_ERRORS
  }
}

//-----------------------------------------------------------------------------------------------------------

void CbFSAdapter::OpenFile(CallbackFileSystem* Sender,
                           LPCTSTR FileName,
                           ACCESS_MASK DesiredAccess,
                           DWORD FileAttributes,
                           DWORD ShareMode,
                           CbFsFileInfo* FileInfo,
                           CbFsHandleInfo* HandleInfo) {
  string path(WindowsPathToUTF8Unix(FileName));

  try {
    // TODO(mberlin): What to do here when the function throws?
    // This should throw an error when the path does not exist.
    if (IsDirectory(path)) {
      return;
    }

    //if (DesiredAccess == FILE_READ_ATTRIBUTES) {
    //  // No need to open files solely for reading attributes.
    //  return;
    //}

    // TODO(mberlin): Handle situations where desiredAccess is only set to SYNCHRONIZE. // NOLINT

    uint32_t mode = 0666;
    if ((FileAttributes & FILE_ATTRIBUTE_READONLY) != 0) {
        mode = 0444;
    }

    FileHandle* file_handle = volume_->OpenFile(
        user_credentials_,
        path,
        ConvertFlagsWindowsToXtreemFS(DesiredAccess),
        mode,
        FileAttributes);
    FileInfo->set_UserContext(reinterpret_cast<PVOID>(file_handle));
  } CATCH_AND_CONVERT_ERRORS
}

//-----------------------------------------------------------------------------------------------------------

void CbFSAdapter::CloseFile(CallbackFileSystem* Sender,
                            CbFsFileInfo* FileInfo,
                            CbFsHandleInfo* HandleInfo) {
  FileHandle* file_handle = reinterpret_cast<FileHandle*>(FileInfo->get_UserContext());

  if (file_handle == NULL) {
    return;
  }

  try {  
    file_handle->Close();
  } CATCH_AND_CONVERT_ERRORS
}

//-----------------------------------------------------------------------------------------------------------

void CbFSAdapter::GetFileInfo(CallbackFileSystem* Sender,
                              LPCTSTR FileName,
                              LPBOOL FileExists,
                              PFILETIME CreationTime,
                              PFILETIME LastAccessTime,
                              PFILETIME LastWriteTime,
                              PFILETIME ChangeTime,
                              __int64* EndOfFile,
                              __int64* AllocationSize,
                              __int64* FileId,
                              PDWORD FileAttributes,
                              LPTSTR ShortFileName OPTIONAL,
                              PWORD ShortFileNameLength OPTIONAL,
                              LPTSTR RealFileName OPTIONAL,
                              PWORD RealFileNameLength OPTIONAL) {
  string path(WindowsPathToUTF8Unix(FileName));

  *FileExists = false;

  if (!xctl_.checkXctlFile(path)) {
    Stat stat;

    try {
      volume_->GetAttr(user_credentials_, path, &stat);

      *FileExists = true;

      ConvertXtreemFSStatToCbFS(stat,
                                CreationTime,
                                LastAccessTime,
                                LastWriteTime,
                                ChangeTime,
                                EndOfFile,
                                AllocationSize,
                                FileId,
                                FileAttributes,
                                ShortFileName,
                                ShortFileNameLength,
                                RealFileName,
                                RealFileNameLength);
    } CATCH_AND_CONVERT_ERRORS
  }
  //} else {
  //  // TODO(mberlin): Fix this for Windows.
  //  return xctl_.getattr(0, 0, path, statbuf);
  //}
}

void CbFSAdapter::ConvertXtreemFSStatToCbFS(const xtreemfs::pbrpc::Stat& stat,
                                            PFILETIME CreationTime,
                                            PFILETIME LastAccessTime,
                                            PFILETIME LastWriteTime,
                                            PFILETIME ChangeTime,
                                            __int64* EndOfFile,
                                            __int64* AllocationSize,
                                            __int64* FileId,
                                            PDWORD FileAttributes,
                                            LPTSTR ShortFileName OPTIONAL,
                                            PWORD ShortFileNameLength OPTIONAL,
                                            LPTSTR RealFileName OPTIONAL,
                                            PWORD RealFileNameLength OPTIONAL) {
      XtreemFSTimeToWinTime(stat.ctime_ns(),
                            &CreationTime->dwLowDateTime,
                            &CreationTime->dwHighDateTime);
      XtreemFSTimeToWinTime(stat.atime_ns(),
                            &LastAccessTime->dwLowDateTime,
                            &LastAccessTime->dwHighDateTime);
      XtreemFSTimeToWinTime(stat.mtime_ns(),
                            &LastWriteTime->dwLowDateTime,
                            &LastWriteTime->dwHighDateTime);

      *EndOfFile = stat.size();
      *AllocationSize = *EndOfFile;
      *FileId = stat.ino();
      
      // TODO(mberlin): Merge read only attribute if file cannot be edited?
      *FileAttributes = stat.attributes();
      if (*FileAttributes == 0) {
        // TODO(mberlin): Remove this if it's not needed.
        //*FileAttributes = FILE_ATTRIBUTE_NORMAL;
        if (IsDirectory(stat)) {
          *FileAttributes = FILE_ATTRIBUTE_DIRECTORY;
        }
      }
}

//-----------------------------------------------------------------------------------------------------------

void CbFSAdapter::EnumerateDirectory(CallbackFileSystem* Sender,
                                     CbFsFileInfo* DirectoryInfo,
                                     CbFsHandleInfo* HandleInfo,
                                     CbFsDirectoryEnumerationInfo* EnumerationInfo,
                                     LPCTSTR Mask,
                                     INT Index,
                                     BOOL Restart,
                                     LPBOOL FileFound,
                                     LPTSTR FileName,
                                     PDWORD FileNameLength,
                                     LPTSTR ShortFileName OPTIONAL,
                                     PUCHAR ShortFileNameLength OPTIONAL,
                                     PFILETIME CreationTime,
                                     PFILETIME LastAccessTime,
                                     PFILETIME LastWriteTime,
                                     PFILETIME ChangeTime,
                                     __int64* EndOfFile,
                                     __int64* AllocationSize,
                                     __int64* FileId,
                                     PDWORD FileAttributes) {
  try {
    // Windows is looking for a specific file if the mask has no "*" or "?".
    if (wstring(Mask).find_first_of(TEXT("*?")) == string::npos) {
      if (!Restart) {
        // We probably answered that already.
        *FileFound = false;
        return;
      }

      wstring full_path(DirectoryInfo->get_FileNameBuffer());
      if (full_path != TEXT("\\")) {
        // Except for the root directory, the trailing slash has to be added.
        full_path += TEXT("\\");
      }
      full_path += wstring(Mask);

      wcsncpy(FileName, Mask, kMaxFileNameLength);
      *FileNameLength = wcslen(FileName);

      GetFileInfo(Sender,
                  full_path.c_str(),
                  FileFound,
                  CreationTime,
                  LastAccessTime,
                  LastWriteTime,
                  ChangeTime,
                  EndOfFile,
                  AllocationSize,
                  FileId,
                  FileAttributes,
                  NULL,
                  NULL,
                  NULL,
                  NULL);
      return;
    }

    string path_directory(WindowsPathToUTF8Unix(
        DirectoryInfo->get_FileNameBuffer()));
    *FileFound = false;
    CbFSEnumerationContext* enum_ctx =
        reinterpret_cast<CbFSEnumerationContext*>(EnumerationInfo->get_UserContext());
      
    if (Restart && enum_ctx != NULL) {
      // Directory was already opened, but we have to read it from the start now.
      if (enum_ctx->offset == 0) {
        enum_ctx->next_index = 0;
      } else {
        delete enum_ctx;
        enum_ctx = NULL;
        EnumerationInfo->set_UserContext(NULL);
      }
    }

    if (enum_ctx == NULL) {
      // No context created yet.
      enum_ctx = new CbFSEnumerationContext();
      EnumerationInfo->set_UserContext(reinterpret_cast<PVOID>(enum_ctx));
    }

    bool dot_dir_seen = false;
    do {
      if (enum_ctx->dir_entries != NULL &&
          enum_ctx->next_index == enum_ctx->dir_entries->entries_size() &&
          enum_ctx->next_index < options_->readdir_chunk_size) {
        // We reached the end of the dir listing.
        *FileFound = false;
        break;
      }

      // Existing chunk was completely processed, drop it.
      if (enum_ctx->dir_entries != NULL &&
          enum_ctx->next_index == options_->readdir_chunk_size) {
        delete enum_ctx->dir_entries;
        enum_ctx->dir_entries = NULL;
        enum_ctx->offset += options_->readdir_chunk_size;
      }

      // Directory entries not available yet or we have to get the next chunk.
      if (enum_ctx->dir_entries == NULL) {
        enum_ctx->dir_entries = volume_->ReadDir(
            user_credentials_,
            path_directory,
            enum_ctx->offset,
            options_->readdir_chunk_size,
            false);  // names_only = false
        enum_ctx->next_index = 0;
        if (enum_ctx->dir_entries->entries_size() == 0) {
          // We reached the end of the dir listing.
          *FileFound = false;
          break;
        }
      }

      const DirectoryEntry& entry =
          enum_ctx->dir_entries->entries(enum_ctx->next_index);
      enum_ctx->next_index++;

      if (entry.name() == "." || entry.name() == "..") {
        dot_dir_seen = true;
      } else {
        dot_dir_seen = false;
      }

      if (!dot_dir_seen) {
        *FileFound = true;
        // TODO(mberlin): Parse mask. Maybe use the function from here: http://msdn.microsoft.com/en-us/library/bb773727%28VS.85%29.aspx // NOLINT

        ConvertUTF8ToWindows(entry.name(), FileName, kMaxFileNameLength);
        *FileNameLength = wcslen(FileName);

        ConvertXtreemFSStatToCbFS(entry.stbuf(),
                                  CreationTime,
                                  LastAccessTime,
                                  LastWriteTime,
                                  ChangeTime,
                                  EndOfFile,
                                  AllocationSize,
                                  FileId,
                                  FileAttributes,
                                  NULL,
                                  NULL,
                                  NULL,
                                  NULL);
      }
    } while (dot_dir_seen);
  } CATCH_AND_CONVERT_ERRORS
}
//-----------------------------------------------------------------------------------------------------------

void CbFSAdapter::CloseDirectoryEnumeration(CallbackFileSystem* Sender,
                                            CbFsFileInfo* DirectoryInfo,
                                            CbFsDirectoryEnumerationInfo* EnumerationInfo) {
  delete reinterpret_cast<CbFSEnumerationContext*>(EnumerationInfo->get_UserContext());
}

void CbFSAdapter::SetEndOfFile(CallbackFileSystem* Sender,
                               CbFsFileInfo* FileInfo,
                               __int64 EndOfFile) {
  FileHandle* file_handle = reinterpret_cast<FileHandle*>(FileInfo->get_UserContext());

  if (file_handle == NULL) {
    Logging::log->getLog(LEVEL_ERROR)
        << "Crashing since CbFS tried to execute truncate() on a file"
            " which was not opened. Path: "
        << WindowsPathToUTF8Unix(FileInfo->get_FileNameBuffer()) << endl;
    assert(file_handle != NULL);
  }

  try {
    file_handle->Truncate(user_credentials_, EndOfFile);
  } CATCH_AND_CONVERT_ERRORS
}

void CbFSAdapter::SetFileAttributes(CallbackFileSystem* Sender,
                                    CbFsFileInfo* FileInfo,
                                    CbFsHandleInfo* HandleInfo,
                                    PFILETIME CreationTime,
                                    PFILETIME LastAccessTime,
                                    PFILETIME LastWriteTime,
                                    PFILETIME ChangeTime,
                                    DWORD FileAttributes) {
  try {
    string path(WindowsPathToUTF8Unix(FileInfo->get_FileNameBuffer()));

    Stat stat;
    InitializeStat(&stat);
    int to_set = 0;
    if (CreationTime != NULL) {
      stat.set_ctime_ns(ConvertWinTimeToXtreemFSTime(CreationTime->dwLowDateTime, CreationTime->dwHighDateTime));
      to_set |= SETATTR_CTIME;
    }
    if (LastAccessTime != NULL) {
      stat.set_atime_ns(ConvertWinTimeToXtreemFSTime(LastAccessTime->dwLowDateTime, LastAccessTime->dwHighDateTime));
      to_set |= SETATTR_ATIME;
    }
    if (LastWriteTime != NULL) {
      stat.set_mtime_ns(ConvertWinTimeToXtreemFSTime(LastWriteTime->dwLowDateTime, LastWriteTime->dwHighDateTime));
      to_set |= SETATTR_MTIME;
    }

    if (FileAttributes != 0) {
      stat.set_attributes(FileAttributes);
      to_set |= SETATTR_ATTRIBUTES;
    }

    if (to_set != 0) {
      // TODO(mberlin): Do not update stat if cached entry is identical.
      volume_->SetAttr(user_credentials_,
                       path,
                       stat,
                       static_cast<Setattrs>(to_set));
    }
  } CATCH_AND_CONVERT_ERRORS
}

//-----------------------------------------------------------------------------------------------------------

void CbFSAdapter::CanFileBeDeleted(CallbackFileSystem* Sender,
                                   CbFsFileInfo* FileInfo,
                                   CbFsHandleInfo* HandleInfo,
                                   BOOL* CanBeDeleted) {
  // TODO(mberlin): For now we skip this check. However, according to the docu
  //                Windows cannot return an error when deleting files and
  //                therefore checks beforehand if it's allowed to.
  *CanBeDeleted = TRUE;
}

//-----------------------------------------------------------------------------------------------------------

void CbFSAdapter::DeleteFile(CallbackFileSystem* Sender,
                             CbFsFileInfo* FileInfo) {
  string path(WindowsPathToUTF8Unix(FileInfo->get_FileNameBuffer()));

  if (IsDirectory(path)) {
    try {
      volume_->DeleteDirectory(user_credentials_, path);
    } CATCH_AND_CONVERT_ERRORS
  } else {
    try {
      volume_->Unlink(user_credentials_, path);
    } CATCH_AND_CONVERT_ERRORS
  }
}

//-----------------------------------------------------------------------------------------------------------

void CbFSAdapter::RenameOrMoveFile(CallbackFileSystem* Sender,
                                   CbFsFileInfo* FileInfo,
                                   LPCTSTR NewFileName) {
  try {
    volume_->Rename(user_credentials_,
                    WindowsPathToUTF8Unix(FileInfo->get_FileNameBuffer()),
                    WindowsPathToUTF8Unix(NewFileName));
  } CATCH_AND_CONVERT_ERRORS
}
//-----------------------------------------------------------------------------------------------------------

void CbFSAdapter::ReadFile(CallbackFileSystem* Sender,
                           CbFsFileInfo* FileInfo,
                           __int64 Position,
                           PVOID Buffer, 
                           DWORD BytesToRead,
                           PDWORD BytesRead) {
  FileHandle* file_handle = reinterpret_cast<FileHandle*>(FileInfo->get_UserContext());

  try {
    //bool close_file_after_read = false;
    //string path(WindowsPathToUTF8Unix(FileInfo->get_FileNameBuffer()));
    //if (file_handle == NULL) {
    //  file_handle = volume_->OpenFile(user_credentials_,
    //                                  path,
    //                                  SYSTEM_V_FCNTL_H_O_RDONLY);
    //  close_file_after_read = true;
    //  if (Logging::log->loggingActive(LEVEL_INFO)) {
    //    Logging::log->getLog(LEVEL_INFO) << "Had to open a file temporarily for"
    //        " reading since it was not opened in OpenFile(). Path: " << path <<
    //        " Read request: s: " << BytesToRead << " o: " << Position << endl;
    //  }
    //}

    *BytesRead = file_handle->Read(reinterpret_cast<char*>(Buffer),
                                   BytesToRead,
                                   Position);

    //if (close_file_after_read) {
    //  file_handle->Close();
    //}
  } CATCH_AND_CONVERT_ERRORS
}
//-----------------------------------------------------------------------------------------------------------

void CbFSAdapter::WriteFile(CallbackFileSystem* Sender,
                            CbFsFileInfo* FileInfo,
                            __int64 Position,
                            PVOID Buffer, 
                            DWORD BytesToWrite,
                            PDWORD BytesWritten) {
  FileHandle* file_handle = reinterpret_cast<FileHandle*>(FileInfo->get_UserContext());

  try {
    *BytesWritten = file_handle->Write(reinterpret_cast<char*>(Buffer),
                                       BytesToWrite,
                                       Position);
  } CATCH_AND_CONVERT_ERRORS
}
//-----------------------------------------------------------------------------------------------------------

void CbFSAdapter::IsDirectoryEmpty(CallbackFileSystem* Sender,
                                   CbFsFileInfo* DirectoryInfo,
                                   LPCWSTR FileName,
                                   LPBOOL IsEmpty) {
  try {
    // TODO(mberlin): Find out how often this gets called and if it makes more
    //                sense to read the whole directory and let it get cached
    //                or only read one entry.
    boost::scoped_ptr<DirectoryEntries> entries(volume_->ReadDir(
        user_credentials_,
        WindowsPathToUTF8Unix(FileName),
        0,
        3,  // Three entries since "." and ".." are the first two ones.
        true));  // names_only
    // Empty if the directory has only "." and ".." as entries.
    *IsEmpty = (entries->entries_size() == 2);
  } CATCH_AND_CONVERT_ERRORS
}

void CbFSAdapter::StorageEjected(CallbackFileSystem* Sender) {
  boost::mutex::scoped_lock lock(device_ejected_mutex_);
  device_ejected_ = true;
  device_ejected_cond_.notify_all();
}

void CbFSAdapter::WaitForEjection() {
  boost::mutex::scoped_lock lock(device_ejected_mutex_);
  while (!device_ejected_) {
    device_ejected_cond_.wait(lock);
  }
}

CbFSAdapter::CbFSAdapter(CbFSOptions* options)
    : options_(options), xctl_("/.xctl$$$"), device_ejected_(false) {
  wstring volume_label_prefix(L"XtreemFS (");
  wstring volume_label_suffix(L")");
  wstring volume_label_dots(L"...");
  wstring xtreemfs_url = ConvertUTF8ToWindows(options_->xtreemfs_url);

  volume_label_ = volume_label_prefix;
  size_t chars_left = kMaxVolumeLabelLength - volume_label_prefix.size()
                                            - volume_label_suffix.size();
  if (xtreemfs_url.size() <= chars_left) {
    volume_label_ += xtreemfs_url;
  } else {
    volume_label_ += xtreemfs_url.substr(0,
                                         chars_left - volume_label_dots.size())
                     + volume_label_dots;
  }
  volume_label_ += volume_label_suffix;

  // Required for CbFS library. kCbFSRegistrationKey is defined in
  // cbfs_license.h which is not publicly available. If you want to help
  // developing the CbFSAdapter, kindly ask the 
  cbfs_.SetRegistrationKey(kCbFSRegistrationKey);  // NOLINT

  cbfs_.SetTag(this);

  cbfs_.SetMaxFileNameLength(kMaxFileNameLength);

  cbfs_.SetStorageType(CallbackFileSystem::stDiskPnP);
  cbfs_.SetStorageCharacteristics(
    static_cast<CallbackFileSystem::CbFsStorageCharacteristics>(
    cbfs_.GetStorageCharacteristics() | CallbackFileSystem::scShowInEjectionTray | CallbackFileSystem::scAllowEjection)
    );

  cbfs_.SetChangeTimeAttributeSupported(false);

  cbfs_.SetOnMount(DelegateMount);
  cbfs_.SetOnUnmount(DelegateUnmount);
  cbfs_.SetOnGetVolumeSize(DelegateGetVolumeSize);
  cbfs_.SetOnGetVolumeLabel(DelegateGetVolumeLabel);
  cbfs_.SetOnSetVolumeLabel(DelegateSetVolumeLabel);
  cbfs_.SetOnGetVolumeId(DelegateGetVolumeId);
  cbfs_.SetOnCreateFile(DelegateCreateFile);
  cbfs_.SetOnOpenFile(DelegateOpenFile);
  cbfs_.SetOnCloseFile(DelegateCloseFile);
  cbfs_.SetOnGetFileInfo(DelegateGetFileInfo);
  cbfs_.SetOnEnumerateDirectory(DelegateEnumerateDirectory);
  cbfs_.SetOnSetAllocationSize(DelegateSetAllocationSize);
  cbfs_.SetOnCloseDirectoryEnumeration(DelegateCloseDirectoryEnumeration);
  cbfs_.SetOnSetEndOfFile(DelegateSetEndOfFile);
  cbfs_.SetOnSetFileAttributes(DelegateSetFileAttributes);
  cbfs_.SetOnCanFileBeDeleted(DelegateCanFileBeDeleted);
  cbfs_.SetOnDeleteFile(DelegateDeleteFile);
  cbfs_.SetOnRenameOrMoveFile(DelegateRenameOrMoveFile);
  cbfs_.SetOnReadFile(DelegateReadFile);
  cbfs_.SetOnWriteFile(DelegateWriteFile);
  cbfs_.SetOnIsDirectoryEmpty(DelegateIsDirectoryEmpty);
  cbfs_.SetOnStorageEjected(DelegateStorageEjected);
}

CbFSAdapter::~CbFSAdapter() {
}

void CbFSAdapter::Start() {
  // Start logging manually (although it would be automatically started by
  // ClientImplementation()) as its required by UserMapping.
  initialize_logger(options_->log_level_string,
                    options_->log_file_path,
                    LEVEL_WARN);

  // Setup Usermapping.
  system_user_mapping_.reset(SystemUserMapping::GetSystemUserMapping());
  system_user_mapping_->GetUserCredentialsForCurrentUser(&user_credentials_);
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "Executing all operations on behalf of"
        " username: " << user_credentials_.username()
        << " and group: " << user_credentials_.groups(0) << endl;
  }

  // Create new Client.
  client_.reset(Client::CreateClient(options_->service_addresses,
                                     user_credentials_,
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
  delete volume_->ReadDir(user_credentials_,
                          "/",
                          0,  // offset
                          options_->readdir_chunk_size,  // count
                          false);  // Do not get stat entries = false.

  // TODO(mberlin): Put this into a common function used by Dokan- and
  //                FuseAdapter.
  // Check the attributes of the Volume.
  boost::scoped_ptr<listxattrResponse> xattrs(
      volume_->ListXAttrs(user_credentials_, "/", false));
  for (int i = 0; i < xattrs->xattrs_size(); ++i) {
    const xtreemfs::pbrpc::XAttr& xattr = xattrs->xattrs(i);

    // First grid user mapping wins.
    if (!options_->grid_auth_mode_globus && !options_->grid_auth_mode_unicore) {
      if (xattr.name() == "xtreemfs.volattr.globus_gridmap") {
        options_->grid_auth_mode_globus = true;
        options_->additional_user_mapping_type = UserMapping::kGlobus;
        if (options_->grid_gridmap_location.empty()) {
          options_->grid_gridmap_location =
              options_->grid_gridmap_location_default_globus;
        }
        Logging::log->getLog(LEVEL_INFO) << "Using Globus gridmap file "
            << options_->grid_gridmap_location << endl;
      }

      if (xattr.name() == "xtreemfs.volattr.unicore_uudb") {
        options_->grid_auth_mode_unicore = true;
        options_->additional_user_mapping_type = UserMapping::kUnicore;
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
      options_->additional_user_mapping_type,
      *options_);
  if (additional_user_mapping) {
    system_user_mapping_->RegisterAdditionalUserMapping(
      additional_user_mapping);
    system_user_mapping_->StartAdditionalUserMapping();

    // Retrieve user_credentials again, this time using the additional mapping.
    system_user_mapping_->GetUserCredentialsForCurrentUser(&user_credentials_);
  }

  // The user mapping is no longer needed since we don't refresh the credentials
  if (system_user_mapping_.get()) {
    system_user_mapping_->StopAdditionalUserMapping();
  }

  // Init CBFS.
  try {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      DWORD modules[] = { CBFS_MODULE_DRIVER, CBFS_MODULE_NET_REDIRECTOR_DLL, CBFS_MODULE_MOUNT_NOTIFIER_DLL };
      for (int i = 0; i < 3; ++i) {
        BOOL Installed = false;
        INT FileVersionHigh = 0, FileVersionLow = 0;
        SERVICE_STATUS ServiceStatus;
        CallbackFileSystem::GetModuleStatus(
            "EA8FA8CB-02C9-4028-8CBC-C109F9B8DFFA", modules[i],
            &Installed, &FileVersionHigh, &FileVersionLow, &ServiceStatus);

        DelegateCheckFlagExt(modules[i], CBFS_MODULE_DRIVER,
            L"Module ", Installed ? L" is installed.\n" : L" is not installed.\n");
        DelegateCheckFlagExt(modules[i], CBFS_MODULE_NET_REDIRECTOR_DLL,
            L"Module ", Installed ? L" is installed.\n" : L" is not installed.\n");
        DelegateCheckFlagExt(modules[i], CBFS_MODULE_MOUNT_NOTIFIER_DLL,
            L"Module ", Installed ? L" is installed.\n" : L" is not installed.\n");

        if (Installed) {
          DbgPrint(L"FileVersionHigh:         %d.%d\n", FileVersionHigh >> 16, FileVersionHigh & 0xFFFF);
          DbgPrint(L"FileVersionLow:          %d.%d\n", FileVersionLow >> 16, FileVersionLow & 0xFFFF);

          DbgPrint(L"ServiceType:             0x%x\n", ServiceStatus.dwServiceType);
          DelegateCheckFlag(ServiceStatus.dwServiceType, SERVICE_FILE_SYSTEM_DRIVER);
          DelegateCheckFlag(ServiceStatus.dwServiceType, SERVICE_KERNEL_DRIVER);
          DelegateCheckFlag(ServiceStatus.dwServiceType, SERVICE_WIN32_OWN_PROCESS);
          DelegateCheckFlag(ServiceStatus.dwServiceType, SERVICE_WIN32_SHARE_PROCESS);
          DelegateCheckFlag(ServiceStatus.dwServiceType, SERVICE_INTERACTIVE_PROCESS);

          DbgPrint(L"CurrentState:            0x%x\n", ServiceStatus.dwCurrentState);
          DelegateCheckFlag(ServiceStatus.dwCurrentState, SERVICE_CONTINUE_PENDING);
          DelegateCheckFlag(ServiceStatus.dwCurrentState, SERVICE_PAUSE_PENDING);
          DelegateCheckFlag(ServiceStatus.dwCurrentState, SERVICE_PAUSED);
          DelegateCheckFlag(ServiceStatus.dwCurrentState, SERVICE_RUNNING);
          DelegateCheckFlag(ServiceStatus.dwCurrentState, SERVICE_START_PENDING);
          DelegateCheckFlag(ServiceStatus.dwCurrentState, SERVICE_STOP_PENDING);
          DelegateCheckFlag(ServiceStatus.dwCurrentState, SERVICE_STOPPED);

          DbgPrint(L"ConrolIsAccepted:        0x%x\n", ServiceStatus.dwControlsAccepted);
          DelegateCheckFlag(ServiceStatus.dwControlsAccepted, SERVICE_ACCEPT_NETBINDCHANGE);
          DelegateCheckFlag(ServiceStatus.dwControlsAccepted, SERVICE_ACCEPT_PARAMCHANGE);
          DelegateCheckFlag(ServiceStatus.dwControlsAccepted, SERVICE_ACCEPT_PAUSE_CONTINUE);
          DelegateCheckFlag(ServiceStatus.dwControlsAccepted, SERVICE_ACCEPT_PRESHUTDOWN);
          DelegateCheckFlag(ServiceStatus.dwControlsAccepted, SERVICE_ACCEPT_SHUTDOWN);
          DelegateCheckFlag(ServiceStatus.dwControlsAccepted, SERVICE_ACCEPT_STOP);
          DelegateCheckFlag(ServiceStatus.dwControlsAccepted, SERVICE_ACCEPT_HARDWAREPROFILECHANGE);
          DelegateCheckFlag(ServiceStatus.dwControlsAccepted, SERVICE_ACCEPT_POWEREVENT);
          DelegateCheckFlag(ServiceStatus.dwControlsAccepted, SERVICE_ACCEPT_SESSIONCHANGE);
          // DelegateCheckFlag(ServiceStatus.dwControlsAccepted, SERVICE_ACCEPT_TIMECHANGE);
          // DelegateCheckFlag(ServiceStatus.dwControlsAccepted, SERVICE_ACCEPT_TRIGGEREVENT);
          // DelegateCheckFlag(ServiceStatus.dwControlsAccepted, SERVICE_ACCEPT_USERMODEREBOOT);

          DbgPrint(L"Win32ExitCode:           0x%x\n", ServiceStatus.dwWin32ExitCode);
          DbgPrint(L"ServiceSpecificExitCode: 0x%x\n", ServiceStatus.dwServiceSpecificExitCode);
          DbgPrint(L"CheckPoint:              0x%x\n", ServiceStatus.dwCheckPoint);
          DbgPrint(L"WaitHint:                0x%x\n", ServiceStatus.dwWaitHint);
        }
      }
    }

    // from the installer script
    CallbackFileSystem::Initialize("EA8FA8CB-02C9-4028-8CBC-C109F9B8DFFA");

    cbfs_.CreateStorage();
    cbfs_.MountMedia(1000 * max(options_->request_timeout_s,
                                options_->connect_timeout_s));

    // TODO(mberlin): Set XtreemFS logo as icon.
    const string first_dir_replica = 
        options_->service_addresses.GetAddresses().front();
    cbfs_.AddMountingPoint(ConvertUTF8ToWindows(
        options_->mount_point
            + ";"
            + first_dir_replica.substr(
                0,
                first_dir_replica.find_last_of(":"))
            + ";"
            + options_->volume_name).c_str(),
        CBFS_SYMLINK_NETWORK | CBFS_SYMLINK_NETWORK_ALLOW_MAP_AS_DRIVE,
        NULL);
  } catch (ECBFSError e) {
    string error = "Failed to mount the volume: " + ECBFSErrorToString(e);
    Logging::log->getLog(LEVEL_ERROR) << error << endl;
    throw XtreemFSException(error);
  }
}

void CbFSAdapter::Stop() {
  try {
    cbfs_.UnmountMedia(true);
  } catch (ECBFSError e) {
    Logging::log->getLog(LEVEL_ERROR)
        << "Failed to un-mount the volume. Make sure all open files are"
            " closed. " << ECBFSErrorToString(e) << endl;

  }

  StopWithoutUnmount();
}

void CbFSAdapter::StopWithoutUnmount() {
  try {
    cbfs_.DeleteStorage(true);
  } catch (ECBFSError e) {
    Logging::log->getLog(LEVEL_ERROR)
        << "Failed to delete the storage. Make sure all open files are"
            " closed. " << ECBFSErrorToString(e) << endl;

  }

  // Shutdown() Client. That does also invoke a volume->Close().
  if (client_.get()) {
    client_->Shutdown();
  }
}

std::string CbFSAdapter::ECBFSErrorToString(ECBFSError& e) {
  string error = ConvertWindowsToUTF8(e.Message());
  if (!error.empty() && error[error.length()-1] == '\n') {
    error.erase(error.length() - 1);
}

  return "Error code: " + boost::lexical_cast<string>(e.ErrorCode())
      + " Message: " + error;
}

int CbFSAdapter::ConvertXtreemFSErrnoToWindows(
    xtreemfs::pbrpc::POSIXErrno xtreemfs_errno) {
  switch (xtreemfs_errno) {
    case POSIX_ERROR_EACCES:
      return ERROR_ACCESS_DENIED;
    case POSIX_ERROR_EEXIST:
      return ERROR_ALREADY_EXISTS;
    case POSIX_ERROR_ENOENT:
      return ERROR_FILE_NOT_FOUND;
    // TODO(fhupfeld): map remaining ones
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

std::string CbFSAdapter::WindowsPathToUTF8Unix(const wchar_t* from) {
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

void CbFSAdapter::XtreemFSTimeToWinTime(uint64_t utime_ns,
                                        DWORD* lower,
                                        DWORD* upper) {
  // Windows starts on Jan 1, 1601 (UTC), counting in 100 nanoseconds steps.
  LONGLONG wintime = utime_ns / 100 + 116444736000000000LL;
  *lower = static_cast<DWORD>(wintime & 0xFFFFffff);
  *upper = static_cast<DWORD>(wintime >> 32);
}

boost::uint64_t CbFSAdapter::ConvertWinTimeToXtreemFSTime(DWORD lower,
                                                          DWORD upper) {
  if (lower == 0 && upper == 0) {
    return 0;
  }
  if (upper == 0xFFFFFFFF && lower == 0xFFFFFFFF) {
    return -1;  // Error
  }

  int64_t utime = static_cast<boost::int64_t>(upper) << 32 | 
      static_cast<boost::int64_t>(lower);
  return (utime - 116444736000000000LL) * 100;
}

bool CbFSAdapter::IsDirectory(const xtreemfs::pbrpc::Stat& stat) {
  return stat.mode() & SYSTEM_V_FCNTL_H_S_IFDIR ? true : false;
}

bool CbFSAdapter::IsDirectory(const std::string& path) {
  Stat stat;
  volume_->GetAttr(user_credentials_, path, &stat);
  return IsDirectory(stat);
}

xtreemfs::pbrpc::SYSTEM_V_FCNTL CbFSAdapter::ConvertFlagsWindowsToXtreemFS(
    const ACCESS_MASK desired_access) {
  bool read = false;
  bool write = false;
  read |= (desired_access & FILE_READ_ATTRIBUTES) != 0;
  read |= (desired_access & FILE_READ_DATA) != 0;
  read |= (desired_access & FILE_READ_EA) != 0;
  read |= (desired_access & STANDARD_RIGHTS_READ) != 0;
  write |= (desired_access & FILE_WRITE_ATTRIBUTES) != 0;
  write |= (desired_access & FILE_WRITE_DATA) != 0;
  write |= (desired_access & FILE_WRITE_EA) != 0;
  write |= (desired_access & STANDARD_RIGHTS_WRITE) != 0;
  
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

  return static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(open_flags);
}

}  // namespace xtreemfs
