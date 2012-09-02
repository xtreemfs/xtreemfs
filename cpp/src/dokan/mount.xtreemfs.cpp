/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *

Copyright (c) 2007, 2008 Hiroki Asakawa info@dokan-dev.net

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include "dokan.h"

#include <cstring>

#include <iostream>
#include <string>

#include "dokan/dokan_adapter.h"
#include "dokan/dokan_operations.h"
#include "dokan/dokan_options.h"

#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "pbrpc/RPC.pb.h"  // xtreemfs::pbrpc::UserCredentials
#include "xtreemfs/MRC.pb.h"  // xtreemfs::pbrpc::Stat
#include "util/logging.h"

using namespace std;
using namespace xtreemfs::util;

BOOL g_UseStdErr = FALSE;
BOOL g_DebugMode = TRUE;

static void DbgPrint(LPCWSTR format, ...) {
  WCHAR buffer[512];
  va_list argp;
  va_start(argp, format);
  vswprintf_s(buffer, sizeof(buffer)/sizeof(WCHAR), format, argp);
  va_end(argp);
  GET_LOG(LEVEL_DEBUG) << buffer; 
  if (g_DebugMode) {
    if (g_UseStdErr) {
      fwprintf(stderr, buffer);
    } else {
      OutputDebugStringW(buffer);
      OutputDebugStringW(L"\n");
    }
  }
}

static void PrintUserName(PDOKAN_FILE_INFO DokanFileInfo) {
  HANDLE handle;
  UCHAR buffer[1024];
  DWORD returnLength;
  WCHAR accountName[256];
  WCHAR domainName[256];
  DWORD accountLength = sizeof(accountName) / sizeof(WCHAR);
  DWORD domainLength = sizeof(domainName) / sizeof(WCHAR);
  PTOKEN_USER tokenUser;
  SID_NAME_USE snu;

  handle = DokanOpenRequestorToken(DokanFileInfo);
  if (handle == INVALID_HANDLE_VALUE) {
    DbgPrint(L"  DokanOpenRequestorToken failed ");
    return;
  }

  if (!GetTokenInformation(handle, TokenUser, buffer, sizeof(buffer),
                           &returnLength)) {
    DbgPrint(L"  GetTokenInformation failed: %d ", GetLastError());
    CloseHandle(handle);
    return;
  }

  CloseHandle(handle);

  tokenUser = (PTOKEN_USER)buffer;
  if (!LookupAccountSid(NULL, tokenUser->User.Sid, accountName,
      &accountLength, domainName, &domainLength, &snu)) {
    DbgPrint(L"  LookupAccountSid failed: %d ", GetLastError());
    return;
  }

  DbgPrint(L"  AccountName: %s, DomainName: %s ", accountName, domainName);
}

#define DelegateCheckFlag(val, flag) \
    if (val&flag) { DbgPrint(L" " L#flag L" "); }

#define TRACE(function) \
    DbgPrint(L"%s: %s %d", function, FileName, DokanFileInfo->Context);

static xtreemfs::DokanAdapter* adapter(PDOKAN_FILE_INFO DokanFileInfo) {
  return reinterpret_cast<xtreemfs::DokanAdapter*>(
      DokanFileInfo->DokanOptions->GlobalContext);
}

static void DebugPrintCreateFile(
    LPCWSTR     FileName,
    DWORD     AccessMode,
    DWORD     ShareMode,
    DWORD     CreationDisposition,
    DWORD     FlagsAndAttributes,
    PDOKAN_FILE_INFO  DokanFileInfo) {
  TRACE(L"CreateFile");

  PrintUserName(DokanFileInfo);

  if (CreationDisposition == CREATE_NEW)
    DbgPrint(L" CREATE_NEW ");
  if (CreationDisposition == OPEN_ALWAYS)
    DbgPrint(L" OPEN_ALWAYS ");
  if (CreationDisposition == CREATE_ALWAYS)
    DbgPrint(L" CREATE_ALWAYS ");
  if (CreationDisposition == OPEN_EXISTING)
    DbgPrint(L" OPEN_EXISTING ");
  if (CreationDisposition == TRUNCATE_EXISTING)
    DbgPrint(L" TRUNCATE_EXISTING ");

  /*
  if (ShareMode == 0 && AccessMode & FILE_WRITE_DATA)
    ShareMode = FILE_SHARE_WRITE;
  else if (ShareMode == 0)
    ShareMode = FILE_SHARE_READ;
  */

  DbgPrint(L" ShareMode = 0x%x ", ShareMode);

  DelegateCheckFlag(ShareMode, FILE_SHARE_READ);
  DelegateCheckFlag(ShareMode, FILE_SHARE_WRITE);
  DelegateCheckFlag(ShareMode, FILE_SHARE_DELETE);

  DbgPrint(L" AccessMode = 0x%x ", AccessMode);

  DelegateCheckFlag(AccessMode, GENERIC_READ);
  DelegateCheckFlag(AccessMode, GENERIC_WRITE);
  DelegateCheckFlag(AccessMode, GENERIC_EXECUTE);
  
  DelegateCheckFlag(AccessMode, DELETE);
  DelegateCheckFlag(AccessMode, FILE_READ_DATA);
  DelegateCheckFlag(AccessMode, FILE_READ_ATTRIBUTES);
  DelegateCheckFlag(AccessMode, FILE_READ_EA);
  DelegateCheckFlag(AccessMode, READ_CONTROL);
  DelegateCheckFlag(AccessMode, FILE_WRITE_DATA);
  DelegateCheckFlag(AccessMode, FILE_WRITE_ATTRIBUTES);
  DelegateCheckFlag(AccessMode, FILE_WRITE_EA);
  DelegateCheckFlag(AccessMode, FILE_APPEND_DATA);
  DelegateCheckFlag(AccessMode, WRITE_DAC);
  DelegateCheckFlag(AccessMode, WRITE_OWNER);
  DelegateCheckFlag(AccessMode, SYNCHRONIZE);
  DelegateCheckFlag(AccessMode, FILE_EXECUTE);
  DelegateCheckFlag(AccessMode, STANDARD_RIGHTS_READ);
  DelegateCheckFlag(AccessMode, STANDARD_RIGHTS_WRITE);
  DelegateCheckFlag(AccessMode, STANDARD_RIGHTS_EXECUTE);

  DbgPrint(L" FlagsAndAttributes = 0x%x ", FlagsAndAttributes);

  DelegateCheckFlag(FlagsAndAttributes, FILE_ATTRIBUTE_ARCHIVE);
  DelegateCheckFlag(FlagsAndAttributes, FILE_ATTRIBUTE_ENCRYPTED);
  DelegateCheckFlag(FlagsAndAttributes, FILE_ATTRIBUTE_HIDDEN);
  DelegateCheckFlag(FlagsAndAttributes, FILE_ATTRIBUTE_NORMAL);
  DelegateCheckFlag(FlagsAndAttributes, FILE_ATTRIBUTE_NOT_CONTENT_INDEXED);
  DelegateCheckFlag(FlagsAndAttributes, FILE_ATTRIBUTE_OFFLINE);
  DelegateCheckFlag(FlagsAndAttributes, FILE_ATTRIBUTE_READONLY);
  DelegateCheckFlag(FlagsAndAttributes, FILE_ATTRIBUTE_SYSTEM);
  DelegateCheckFlag(FlagsAndAttributes, FILE_ATTRIBUTE_TEMPORARY);
  DelegateCheckFlag(FlagsAndAttributes, FILE_FLAG_WRITE_THROUGH);
  DelegateCheckFlag(FlagsAndAttributes, FILE_FLAG_OVERLAPPED);
  DelegateCheckFlag(FlagsAndAttributes, FILE_FLAG_NO_BUFFERING);
  DelegateCheckFlag(FlagsAndAttributes, FILE_FLAG_RANDOM_ACCESS);
  DelegateCheckFlag(FlagsAndAttributes, FILE_FLAG_SEQUENTIAL_SCAN);
  DelegateCheckFlag(FlagsAndAttributes, FILE_FLAG_DELETE_ON_CLOSE);
  DelegateCheckFlag(FlagsAndAttributes, FILE_FLAG_BACKUP_SEMANTICS);
  DelegateCheckFlag(FlagsAndAttributes, FILE_FLAG_POSIX_SEMANTICS);
  DelegateCheckFlag(FlagsAndAttributes, FILE_FLAG_OPEN_REPARSE_POINT);
  DelegateCheckFlag(FlagsAndAttributes, FILE_FLAG_OPEN_NO_RECALL);
  DelegateCheckFlag(FlagsAndAttributes, SECURITY_ANONYMOUS);
  DelegateCheckFlag(FlagsAndAttributes, SECURITY_IDENTIFICATION);
  DelegateCheckFlag(FlagsAndAttributes, SECURITY_IMPERSONATION);
  DelegateCheckFlag(FlagsAndAttributes, SECURITY_DELEGATION);
  DelegateCheckFlag(FlagsAndAttributes, SECURITY_CONTEXT_TRACKING);
  DelegateCheckFlag(FlagsAndAttributes, SECURITY_EFFECTIVE_ONLY);
  DelegateCheckFlag(FlagsAndAttributes, SECURITY_SQOS_PRESENT);

  DbgPrint(L" ");
}

static int __stdcall DelegateCreateFile(
    LPCWSTR FileName,
    DWORD AccessMode,
    DWORD ShareMode,
    DWORD CreationDisposition,
    DWORD FlagsAndAttributes,
    PDOKAN_FILE_INFO DokanFileInfo) {
  DebugPrintCreateFile(
      FileName, AccessMode, ShareMode, CreationDisposition, 
      FlagsAndAttributes, DokanFileInfo);
  return adapter(DokanFileInfo)->CreateFile(
      FileName, AccessMode, ShareMode, CreationDisposition, 
      FlagsAndAttributes, DokanFileInfo);
}

static int __stdcall DelegateCreateDirectory(
    LPCWSTR FileName,
    PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"CreateDirectory");
  return adapter(DokanFileInfo)->CreateDirectory(
      FileName, DokanFileInfo);
}

static int __stdcall DelegateOpenDirectory(
  LPCWSTR FileName,
  PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"OpenDirectory");
  return adapter(DokanFileInfo)->OpenDirectory(
      FileName, DokanFileInfo);
}

static int __stdcall DelegateCloseFile(
    LPCWSTR FileName,
    PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"CloseFile");
  return adapter(DokanFileInfo)->CloseFile(
      FileName, DokanFileInfo);
  return 0;
}

static int __stdcall DelegateCleanup(
    LPCWSTR FileName,
    PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"Cleanup");
  return adapter(DokanFileInfo)->Cleanup(
      FileName, DokanFileInfo);
}

static int __stdcall DelegateReadFile(
    LPCWSTR FileName,
    LPVOID Buffer,
    DWORD BufferLength,
    LPDWORD ReadLength,
    LONGLONG Offset,
    PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"ReadFile");
  return adapter(DokanFileInfo)->ReadFile(
      FileName, Buffer, BufferLength, ReadLength, Offset,
      DokanFileInfo);
}

static int __stdcall DelegateWriteFile(
    LPCWSTR FileName,
    LPCVOID Buffer,
    DWORD NumberOfBytesToWrite,
    LPDWORD NumberOfBytesWritten,
    LONGLONG Offset,
    PDOKAN_FILE_INFO  DokanFileInfo) {
  TRACE(L"WriteFile");
  return adapter(DokanFileInfo)->WriteFile(
      FileName, Buffer, NumberOfBytesToWrite, NumberOfBytesWritten, Offset,
      DokanFileInfo);
}

static int __stdcall DelegateFlushFileBuffers(
    LPCWSTR FileName,
    PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"FlushFileBuffers");
  return adapter(DokanFileInfo)->FlushFileBuffers(
      FileName, DokanFileInfo);
}

static int __stdcall DelegateGetFileInformation(
    LPCWSTR FileName,
    LPBY_HANDLE_FILE_INFORMATION HandleFileInformation,
    PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"GetFileInformation");
  return adapter(DokanFileInfo)->GetFileInformation(
      FileName, HandleFileInformation, DokanFileInfo);
}

static int __stdcall DelegateFindFiles(
    LPCWSTR FileName,
    PFillFindData FillFindData,
    PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"FindFiles");
  return adapter(DokanFileInfo)->FindFiles(
      FileName, FillFindData, DokanFileInfo);
}

static int __stdcall DelegateDeleteFile(
    LPCWSTR FileName,
    PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"DeleteFile");
    return adapter(DokanFileInfo)->DeleteFile(
      FileName, DokanFileInfo);
}

static int __stdcall DelegateDeleteDirectory(
    LPCWSTR    FileName,
    PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"DeleteDirectory");
    return adapter(DokanFileInfo)->DeleteDirectory(
      FileName, DokanFileInfo);
}

static int __stdcall DelegateMoveFile(
  LPCWSTR    FileName, // existing file name
  LPCWSTR    NewFileName,
  BOOL    ReplaceIfExisting,
  PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"MoveFile");
    return adapter(DokanFileInfo)->MoveFile(
      FileName, NewFileName, ReplaceIfExisting, DokanFileInfo);
}

static int __stdcall DelegateLockFile(
  LPCWSTR    FileName,
  LONGLONG   ByteOffset,
  LONGLONG   Length,
  PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"LockFile");
    return adapter(DokanFileInfo)->LockFile(
      FileName, ByteOffset, Length, DokanFileInfo);
}

static int __stdcall DelegateSetEndOfFile(
  LPCWSTR    FileName,
  LONGLONG   ByteOffset,
  PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"SetEndOfFile");
    return adapter(DokanFileInfo)->SetEndOfFile(
      FileName, ByteOffset, DokanFileInfo);
}

static int __stdcall DelegateSetAllocationSize(
  LPCWSTR    FileName,
  LONGLONG   AllocSize,
  PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"SetAllocationSize");
    return adapter(DokanFileInfo)->SetAllocationSize(
      FileName, AllocSize, DokanFileInfo);
}

static int __stdcall DelegateSetFileAttributes(
  LPCWSTR    FileName,
  DWORD    FileAttributes,
  PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"SetFileAttributes");
    return adapter(DokanFileInfo)->SetFileAttributes(
      FileName, FileAttributes, DokanFileInfo);
}

static int __stdcall DelegateSetFileTime(
  LPCWSTR    FileName,
  CONST FILETIME*  CreationTime,
  CONST FILETIME*  LastAccessTime,
  CONST FILETIME*  LastWriteTime,
  PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"SetFileTime");
    return adapter(DokanFileInfo)->SetFileTime(
      FileName, CreationTime, LastAccessTime, LastWriteTime, 
      DokanFileInfo);
}

static int __stdcall DelegateUnlockFile(
  LPCWSTR    FileName,
  LONGLONG   ByteOffset,
  LONGLONG   Length,
  PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"UnlockFile");
  return adapter(DokanFileInfo)->UnlockFile(
      FileName, ByteOffset, Length, DokanFileInfo);
}

static int __stdcall DelegateGetFileSecurity(
    LPCWSTR FileName,
    PSECURITY_INFORMATION SecurityInformation,
    PSECURITY_DESCRIPTOR SecurityDescriptor,
    ULONG    BufferLength,
    PULONG    LengthNeeded,
    PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"GetFileSecurity");
  return adapter(DokanFileInfo)->GetFileSecurity(
      FileName, SecurityInformation, SecurityDescriptor,
      BufferLength, LengthNeeded, DokanFileInfo);
}

static int __stdcall DelegateSetFileSecurity(
    LPCWSTR FileName,
    PSECURITY_INFORMATION SecurityInformation,
    PSECURITY_DESCRIPTOR SecurityDescriptor,
    ULONG SecurityDescriptorLength,
    PDOKAN_FILE_INFO DokanFileInfo) {
  TRACE(L"SetFileSecurity");
  return adapter(DokanFileInfo)->SetFileSecurity(
      FileName, SecurityInformation, SecurityDescriptor,
      SecurityDescriptorLength, DokanFileInfo);
}

static int __stdcall DelegateGetVolumeInformation(
    LPWSTR VolumeNameBuffer,
    DWORD VolumeNameSize,
    LPDWORD VolumeSerialNumber,
    LPDWORD MaximumComponentLength,
    LPDWORD FileSystemFlags,
    LPWSTR FileSystemNameBuffer,
    DWORD FileSystemNameSize,
    PDOKAN_FILE_INFO DokanFileInfo) {
  DbgPrint(L"GetVolumeInformation %d", DokanFileInfo->Context);
  return adapter(DokanFileInfo)->GetVolumeInformation(
      VolumeNameBuffer, VolumeNameSize, VolumeSerialNumber,
      MaximumComponentLength, FileSystemFlags, FileSystemNameBuffer,
      FileSystemNameSize, DokanFileInfo);
}

static int __stdcall DelegateUnmount(
    PDOKAN_FILE_INFO DokanFileInfo) {
  return adapter(DokanFileInfo)->Unmount(DokanFileInfo);
}

int __cdecl wmain(ULONG argc, PWCHAR argv[]) {
  initialize_logger(LEVEL_DEBUG, "mount.xtreemfs.log");
  
  xtreemfs::DokanOptions dokan_options;
  dokan_options.service_address = "demo.xtreemfs.org:32638";
  dokan_options.volume_name = "demo";
  GET_LOG(LEVEL_DEBUG) << "Starting client, mounting " 
                       << dokan_options.service_address
                       << " " << dokan_options.volume_name;
  boost::scoped_ptr<xtreemfs::DokanAdapter> dokan_adapter(
      new xtreemfs::DokanAdapter(&dokan_options));
  dokan_adapter->Start();
  
  DOKAN_OPTIONS dokanOptions;
  ZeroMemory(&dokanOptions, sizeof(DOKAN_OPTIONS));
  dokanOptions.Version = DOKAN_VERSION;
  dokanOptions.ThreadCount = 0; // use default
  dokanOptions.GlobalContext = reinterpret_cast<ULONG64>(dokan_adapter.get());
  
  dokanOptions.MountPoint = L"X:";
  dokanOptions.Options |= DOKAN_OPTION_DEBUG;
  dokanOptions.Options |= DOKAN_OPTION_STDERR;
  dokanOptions.Options |= DOKAN_OPTION_KEEP_ALIVE;
  
  DOKAN_OPERATIONS dokanOperations;
  ZeroMemory(&dokanOperations, sizeof(DOKAN_OPERATIONS));
  dokanOperations.CreateFile = DelegateCreateFile;
  dokanOperations.OpenDirectory = DelegateOpenDirectory;
  dokanOperations.CreateDirectory = DelegateCreateDirectory;
  dokanOperations.Cleanup = DelegateCleanup;
  dokanOperations.CloseFile = DelegateCloseFile;
  dokanOperations.ReadFile = DelegateReadFile;
  dokanOperations.WriteFile = DelegateWriteFile;
  dokanOperations.FlushFileBuffers = DelegateFlushFileBuffers;
  dokanOperations.GetFileInformation = DelegateGetFileInformation;
  dokanOperations.FindFiles = DelegateFindFiles;
  dokanOperations.FindFilesWithPattern = NULL;
  dokanOperations.SetFileAttributes = DelegateSetFileAttributes;
  dokanOperations.SetFileTime = DelegateSetFileTime;
  dokanOperations.DeleteFile = DelegateDeleteFile;
  dokanOperations.DeleteDirectory = DelegateDeleteDirectory;
  dokanOperations.MoveFile = DelegateMoveFile;
  dokanOperations.SetEndOfFile = DelegateSetEndOfFile;
  dokanOperations.SetAllocationSize = DelegateSetAllocationSize;
  dokanOperations.LockFile = DelegateLockFile;
  dokanOperations.UnlockFile = DelegateUnlockFile;
  dokanOperations.GetFileSecurity = DelegateGetFileSecurity;
  dokanOperations.SetFileSecurity = DelegateSetFileSecurity;
  dokanOperations.GetDiskFreeSpace = NULL;
  dokanOperations.GetVolumeInformation = DelegateGetVolumeInformation;
  dokanOperations.Unmount = DelegateUnmount;

  int status = DokanMain(&dokanOptions, &dokanOperations);
  switch (status) {
    case DOKAN_SUCCESS:
      GET_LOG(LEVEL_DEBUG) << "Success";
      break;
    case DOKAN_ERROR:
      GET_LOG(LEVEL_ERROR) << "Error";
      break;
    case DOKAN_DRIVE_LETTER_ERROR:
      GET_LOG(LEVEL_ERROR) << "Bad drive letter";
      break;
    case DOKAN_DRIVER_INSTALL_ERROR:
      GET_LOG(LEVEL_ERROR) << "Can't install driver";
      break;
    case DOKAN_START_ERROR:
      GET_LOG(LEVEL_ERROR) << "Driver: something wrong";
      break;
    case DOKAN_MOUNT_ERROR:
      GET_LOG(LEVEL_ERROR) << "Can't assign a drive letter";
      break;
    case DOKAN_MOUNT_POINT_ERROR:
      GET_LOG(LEVEL_ERROR) << "Mount point error";
      break;
    default:
      GET_LOG(LEVEL_ERROR) << "Unknown error: " << status;
      break;
  }

  dokan_adapter->Stop();
  GET_LOG(LEVEL_DEBUG)  << "Did shutdown the XtreemFS client.";
  // libxtreemfs shuts down logger.
  dokan_adapter.reset(NULL);

  return 0;
}
