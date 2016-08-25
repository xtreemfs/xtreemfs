/*
 * Copyright (c) 2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_CBFS_CBFS_ADAPTER_H_
#define CPP_INCLUDE_CBFS_CBFS_ADAPTER_H_

#define WIN32_LEAN_AND_MEAN
#include <windows.h>  // required for CbFS.h

#include <boost/cstdint.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/thread/condition.hpp>
#include <boost/thread/mutex.hpp>
#include <CbFS.h>

#include "xtfsutil/xtfsutil_server.h"
#include "xtreemfs/GlobalTypes.pb.h"

namespace xtreemfs {

namespace pbrpc {
class Stat;
}  // namespace pbrpc

class CbFSOptions;
class Client;
class SystemUserMapping;

class CbFSAdapter {
 public:
  /** Replace "\" path separator by "/" and convert UTF-16 to UTF-8. */
  static std::string WindowsPathToUTF8Unix(const wchar_t* from);

  static void DebugPrintCreateFile(
    LPCWSTR OperationType,
    LPCTSTR FileName,
    ACCESS_MASK DesiredAccess,
    DWORD FileAttributes,
    DWORD ShareMode,
    CbFsFileInfo* FileInfo,
    CbFsHandleInfo* HandleInfo);

  /** Creates a new instance of CbFSAdapter, but does not create any 
   *  libxtreemfs Client yet.
   *
   *  Use Start() to actually create the client and mount the volume given in
   *  options. May modify options.
   */
  explicit CbFSAdapter(CbFSOptions* options);

  ~CbFSAdapter();

  /** Create client, open volume and start needed threads. */
  void Start();

  /** Shutdown threads, close Volume and Client and blocks until all threads
   *  are stopped. */
  void Stop();

  /** Same as Stop(), but does not call cbfs_.UnmountMedia(). */
  void StopWithoutUnmount();

  /** Same as StopWithoutUnmount, but does not call cbfs_.DeleteStorage(). */
  void StopWithoutUnmountAndWithoutDelete();

  void GetVolumeSize(CallbackFileSystem* Sender,
                     __int64* TotalNumberOfSectors,
                     __int64* NumberOfFreeSectors);

  void GetVolumeLabel(CallbackFileSystem* Sender, LPTSTR VolumeLabel);

  void GetVolumeId(CallbackFileSystem* Sender, PDWORD VolumeID);

  void CreateFile(CallbackFileSystem* Sender,
                  LPCTSTR FileName,
                  ACCESS_MASK DesiredAccess,
                  DWORD FileAttributes,
                  DWORD ShareMode,
                  CbFsFileInfo* FileInfo,
                  CbFsHandleInfo* HandleInfo);

  void OpenFile(CallbackFileSystem* Sender,
                LPCTSTR FileName,
                ACCESS_MASK DesiredAccess,
                DWORD FileAttributes,
                DWORD ShareMode,
                CbFsFileInfo* FileInfo,
                CbFsHandleInfo* HandleInfo);

  void CloseFile(CallbackFileSystem* Sender,
                 CbFsFileInfo* FileInfo,
                 CbFsHandleInfo* HandleInfo);

  void GetFileInfo(CallbackFileSystem* Sender,
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
                   PWORD RealFileNameLength OPTIONAL);

  void EnumerateDirectory(CallbackFileSystem* Sender,
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
                          PDWORD FileAttributes);
    
  void CloseDirectoryEnumeration(CallbackFileSystem* Sender,
                                 CbFsFileInfo* DirectoryInfo,
                                 CbFsDirectoryEnumerationInfo* EnumerationInfo);

  void SetAllocationSize(CallbackFileSystem* Sender,
                         CbFsFileInfo* FileInfo,
                         PVOID FileHandleContext,
                         __int64 AllocationSize);

  void SetEndOfFile(CallbackFileSystem* Sender,
                    CbFsFileInfo* FileInfo,
                    __int64 EndOfFile);
    
  void SetFileAttributes(CallbackFileSystem* Sender,
                         CbFsFileInfo* FileInfo,
                         CbFsHandleInfo* HandleInfo,
                         PFILETIME CreationTime,
                         PFILETIME LastAccessTime,
                         PFILETIME LastWriteTime,
                         PFILETIME ChangeTime,
                         DWORD FileAttributes);
  
  void CanFileBeDeleted(CallbackFileSystem* Sender,
                        CbFsFileInfo* FileInfo,
                        CbFsHandleInfo* HandleInfo,
                        BOOL* CanBeDeleted);

  void DeleteFile(CallbackFileSystem* Sender, CbFsFileInfo* FileInfo);

  void RenameOrMoveFile(CallbackFileSystem* Sender,
                        CbFsFileInfo* FileInfo,
                        LPCTSTR NewFileName);

  void ReadFile(CallbackFileSystem* Sender,
                CbFsFileInfo* FileInfo,
                __int64 Position,
                PVOID Buffer, 
                DWORD BytesToRead,
                PDWORD BytesRead);
    
  void WriteFile(CallbackFileSystem* Sender,
                 CbFsFileInfo* FileInfo,
                 __int64 Position,
                 PVOID Buffer, 
                 DWORD BytesToWrite,
                 PDWORD BytesWritten);
    
  void IsDirectoryEmpty(CallbackFileSystem* Sender,
                        CbFsFileInfo* DirectoryInfo,
                        LPCWSTR FileName,
                        LPBOOL IsEmpty);

  void Unmount(CallbackFileSystem* Sender);

  void StorageEjected(CallbackFileSystem* Sender);

  /** Blocks until device was ejected by user. */
  void WaitForEjection();

 private:
  static const DWORD kMaxFileNameLength = 32767;

  static const size_t kMaxVolumeLabelLength = 32;

  /** Print debug output to stdout. */
  static void DbgPrint(LPCWSTR format, ...);

  /** Output exception thrown by CBFS as string. */
  static std::string ECBFSErrorToString(ECBFSError& e);

  /** Maps XtreemFS return values to Windows specific ones. */
  static int ConvertXtreemFSErrnoToWindows(
      xtreemfs::pbrpc::POSIXErrno xtreemfs_errno);

  /** Convert UNIX timestamp (in nanoseconds) to Windows time format. */
  static void XtreemFSTimeToWinTime(uint64_t utime_ns,
                                    DWORD* lower,
                                    DWORD* upper);

  /** Convert Windows timestamp to UNIX timestamp (in nanoseconds). */
  static boost::uint64_t ConvertWinTimeToXtreemFSTime(DWORD lower,
                                                      DWORD upper);

  /** Returns true if "stat" is a directory. */
  static bool IsDirectory(const xtreemfs::pbrpc::Stat& stat);

  /** Convert "desired_access", passed at Create and Open, to e.g., O_RDWR. */
  static xtreemfs::pbrpc::SYSTEM_V_FCNTL ConvertFlagsWindowsToXtreemFS(
      const ACCESS_MASK desired_access);

  /** Provides the CBFS library the required license key. */
  void SetRegistrationKey();

  /** Returns true if "path" is a directory.
    * @throws AddressToUUIDNotFoundException
    * @throws IOException
    * @throws PosixErrorException
    * @throws UnknownAddressSchemeException
    */
  bool IsDirectory(const std::string& path);

  /** Same as GetFileInfo(), except that "path" is an UTF8 encoded UNIX path. */
  void ConvertXtreemFSStatToCbFS(const xtreemfs::pbrpc::Stat& stat,
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
                                 PWORD RealFileNameLength OPTIONAL);

  /** Contains all needed options to mount the requested volume. */
  CbFSOptions* options_;

  /** Volume Label generated from the given XtreemFS URL. */
  std::wstring volume_label_;

  /** The chosen UserMapping provides methods to translate between local and
   *  remote usernames and groups. */
  boost::scoped_ptr<SystemUserMapping> system_user_mapping_;

  /** Username and domain name of user executing the Dokan client. Will be used
   *  by all XtreemFS operations. */
  xtreemfs::pbrpc::UserCredentials user_credentials_;

  /** Created libxtreemfs Client. */
  boost::scoped_ptr<Client> client_;

  /** Opened libxtreemfs Volume. */
  Volume* volume_;

  /** Server for processing commands sent from the xtfsutil tool
      via xctl files. */
  XtfsUtilServer xctl_;

  /** Callback Filesystem instance which provides a Windows user space
   *  file system interface. */
  CallbackFileSystem cbfs_;

  /** True if device was unmounted by user. */
  bool device_unmounted_;

  /** True if device was ejected by user. */
  bool device_ejected_;

  /** Guards device_unmounted_. */
  boost::mutex device_unmounted_mutex_;

  /** Guards device_ejected_. */
  boost::mutex device_ejected_mutex_;

  /** Used when waiting for a change of device_ummounted_ and device_ejected_. */
  boost::condition device_unmounted_or_ejected_cond_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_CBFS_CBFS_ADAPTER_H_
