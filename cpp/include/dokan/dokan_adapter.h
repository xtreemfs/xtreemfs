/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_DOKAN_DOKAN_ADAPTER_H_
#define CPP_INCLUDE_DOKAN_DOKAN_ADAPTER_H_

#define WIN32_LEAN_AND_MEAN
#include <windows.h>  // Required by dokan.h
#include <dokan.h>

#include <boost/scoped_ptr.hpp>
//#include <list>
//#include <string>
//
#include "xtfsutil/xtfsutil_server.h"
//#include "xtreemfs/GlobalTypes.pb.h"
//
namespace xtreemfs {
class Client;
class DokanOptions;
class SystemUserMapping;
class Volume;

//namespace pbrpc {
//class Stat;
//class UserCredentials;
//}  // namespace pbrpc
//
class DokanAdapter {
 public:
  /** Creates a new instance of DokanAdapter, but does not create any 
   *  libxtreemfs Client yet.
   *
   *  Use Start() to actually create the client and mount the volume given in
   *  options. May modify options.
   */
  explicit DokanAdapter(DokanOptions* options);

  ~DokanAdapter();

  /** Create client, open volume and start needed threads. */
  void Start();

  /** Shutdown threads, close Volume and Client and blocks until all threads
   *  are stopped. */
  void Stop();

//  void GenerateUserCredentials(
//      uid_t uid,
//      gid_t gid,
//      pid_t pid,
//      xtreemfs::pbrpc::UserCredentials* user_credentials);
//
//  /** Generate UserCredentials using information from dokan context or the
//   *  current process (in that case set dokan_context to NULL). */
//  void GenerateUserCredentials(
//      struct dokan_context* dokan_context,
//      xtreemfs::pbrpc::UserCredentials* user_credentials);
//
//  /** Fill a Dokan stat object with information from an XtreemFS stat. */
//  void ConvertXtreemFSStatToDokan(const xtreemfs::pbrpc::Stat& xtreemfs_stat,
//                                 struct stat* dokan_stat);
//
//  /** Converts given UNIX file handle flags into XtreemFS symbols. */
//  xtreemfs::pbrpc::SYSTEM_V_FCNTL ConvertFlagsUnixToXtreemFS(int flags);
//
//  /** Converts from XtreemFS error codes to the system ones. */
//  int ConvertXtreemFSErrnoToDokan(xtreemfs::pbrpc::POSIXErrno xtreemfs_errno);
//
  // Dokan operations as called by placeholder functions in dokan_operations.h.
  int CreateFile(LPCWSTR path,
                 DWORD desired_access,
                 DWORD share_mode,
                 DWORD creation_disposition,
                 DWORD flags_and_attributes,
                 PDOKAN_FILE_INFO dokan_file_info);

  int OpenDirectory(
      LPCWSTR,				// FileName
      PDOKAN_FILE_INFO);

  int CreateDirectory(
      LPCWSTR,				// FileName
      PDOKAN_FILE_INFO);

  // When FileInfo->DeleteOnClose is true, you must delete the file in Cleanup.
  int Cleanup(
      LPCWSTR,      // FileName
      PDOKAN_FILE_INFO);

  int CloseFile(
      LPCWSTR,      // FileName
      PDOKAN_FILE_INFO);

  int ReadFile(
      LPCWSTR,  // FileName
      LPVOID,   // Buffer
      DWORD,    // NumberOfBytesToRead
      LPDWORD,  // NumberOfBytesRead
      LONGLONG, // Offset
      PDOKAN_FILE_INFO);

  int WriteFile(
      LPCWSTR,  // FileName
      LPCVOID,  // Buffer
      DWORD,    // NumberOfBytesToWrite
      LPDWORD,  // NumberOfBytesWritten
      LONGLONG, // Offset
      PDOKAN_FILE_INFO);

  int FlushFileBuffers(
      LPCWSTR, // FileName
      PDOKAN_FILE_INFO);

  int GetFileInformation(
      LPCWSTR,          // FileName
      LPBY_HANDLE_FILE_INFORMATION, // Buffer
      PDOKAN_FILE_INFO);

  int FindFiles(
      LPCWSTR,			// PathName
      PFillFindData,		// call this function with PWIN32_FIND_DATAW
      PDOKAN_FILE_INFO);  //  (see PFillFindData definition)

  // You should implement either FindFiles or FindFilesWithPattern
  int FindFilesWithPattern(
      LPCWSTR,			// PathName
      LPCWSTR,			// SearchPattern
      PFillFindData,		// call this function with PWIN32_FIND_DATAW
      PDOKAN_FILE_INFO);

  int SetFileAttributes(
      LPCWSTR, // FileName
      DWORD,   // FileAttributes
      PDOKAN_FILE_INFO);

  int SetFileTime(
      LPCWSTR,		// FileName
      CONST FILETIME*, // CreationTime
      CONST FILETIME*, // LastAccessTime
      CONST FILETIME*, // LastWriteTime
      PDOKAN_FILE_INFO);

  // You should not delete file on DeleteFile or DeleteDirectory.
  // When DeleteFile or DeleteDirectory, you must check whether
  // you can delete the file or not, and return 0 (when you can delete it)
  // or appropriate error codes such as -ERROR_DIR_NOT_EMPTY,
  // -ERROR_SHARING_VIOLATION.
  // When you return 0 (ERROR_SUCCESS), you get Cleanup with
  // FileInfo->DeleteOnClose set TRUE and you have to delete the
  // file in Close.
  int DeleteFile(
      LPCWSTR, // FileName
      PDOKAN_FILE_INFO);

  int DeleteDirectory( 
      LPCWSTR, // FileName
      PDOKAN_FILE_INFO);

  int MoveFile(
      LPCWSTR, // ExistingFileName
      LPCWSTR, // NewFileName
      BOOL,	// ReplaceExisiting
      PDOKAN_FILE_INFO);

  int SetEndOfFile(
      LPCWSTR,  // FileName
      LONGLONG, // Length
      PDOKAN_FILE_INFO);

  int SetAllocationSize(
      LPCWSTR,  // FileName
      LONGLONG, // Length
      PDOKAN_FILE_INFO);

  int LockFile(
      LPCWSTR, // FileName
      LONGLONG, // ByteOffset
      LONGLONG, // Length
      PDOKAN_FILE_INFO);

  int UnlockFile(
      LPCWSTR, // FileName
      LONGLONG,// ByteOffset
      LONGLONG,// Length
      PDOKAN_FILE_INFO);

  // Neither GetDiskFreeSpace nor GetVolumeInformation
  // save the DokanFileContext->Context.
  // Before these methods are called, CreateFile may not be called.
  // (ditto CloseFile and Cleanup)

  // see Win32 API GetDiskFreeSpaceEx
  int GetDiskFreeSpace(
      PULONGLONG, // FreeBytesAvailable
      PULONGLONG, // TotalNumberOfBytes
      PULONGLONG, // TotalNumberOfFreeBytes
      PDOKAN_FILE_INFO);

  // see Win32 API GetVolumeInformation
  int GetVolumeInformation(
      LPWSTR, // VolumeNameBuffer
      DWORD,	// VolumeNameSize in num of chars
      LPDWORD,// VolumeSerialNumber
      LPDWORD,// MaximumComponentLength in num of chars
      LPDWORD,// FileSystemFlags
      LPWSTR,	// FileSystemNameBuffer
      DWORD,	// FileSystemNameSize in num of chars
      PDOKAN_FILE_INFO);

  int Unmount(
      PDOKAN_FILE_INFO);

  // Suported since 0.6.0. You must specify the version at DOKAN_OPTIONS.Version.
  int GetFileSecurity(
      LPCWSTR, // FileName
      // A pointer to SECURITY_INFORMATION value being requested
      PSECURITY_INFORMATION,
       // A pointer to SECURITY_DESCRIPTOR buffer to be filled
      PSECURITY_DESCRIPTOR,
      ULONG, // length of Security descriptor buffer
      PULONG, // LengthNeeded
      PDOKAN_FILE_INFO);

  int SetFileSecurity(
      LPCWSTR, // FileName
      PSECURITY_INFORMATION,
      PSECURITY_DESCRIPTOR, // SecurityDescriptor
      ULONG, // SecurityDescriptor length
      PDOKAN_FILE_INFO);
//  int statfs(const char *path, struct statvfs *statv);
//  int getattr(const char *path, struct stat *statbuf);
//  int getxattr(const char *path, const char *name, char *value, size_t size);
//
//  /** Creates CachedDirectoryEntries struct and let fi->fh point to it. */
//  int opendir(const char *path, struct dokan_file_info *fi);
//
//  /** Uses the Dokan readdir offset approach to handle readdir requests in chunks
//   *  instead of one large request. */
//  int readdir(const char *path, void *buf, dokan_fill_dir_t filler, off_t offset,
//              struct dokan_file_info *fi);
//
//  /** Deletes CachedDirectoryEntries struct which is hold by fi->fh. */
//  int releasedir(const char *path, struct dokan_file_info *fi);
//
//  int utime(const char *path, struct utimbuf *ubuf);
//  int utimens(const char *path, const struct timespec tv[2]);
//  int create(const char *path, mode_t mode, struct dokan_file_info *fi);
//  int mknod(const char *path, mode_t mode, dev_t device);
//  int mkdir(const char *path, mode_t mode);
//  int open(const char *path, struct dokan_file_info *fi);
//  int truncate(const char *path, off_t newsize);
//  int ftruncate(const char *path, off_t offset, struct dokan_file_info *fi);
//  int write(const char *path, const char *buf, size_t size, off_t offset,
//            struct dokan_file_info *fi);
//  int flush(const char *path, struct dokan_file_info *fi);
//  int read(const char *path, char *buf, size_t size, off_t offset,
//           struct dokan_file_info *fi);
//  int access(const char *path, int mask);
//  int unlink(const char *path);
//  int fgetattr(const char *path, struct stat *statbuf,
//               struct dokan_file_info *fi);
//  int release(const char *path, struct dokan_file_info *fi);
//
//  int readlink(const char *path, char *buf, size_t size);
//  int rmdir(const char *path);
//  int symlink(const char *path, const char *link);
//  int rename(const char *path, const char *newpath);
//  int link(const char *path, const char *newpath);
//  int chmod(const char *path, mode_t mode);
//  int chown(const char *path, uid_t uid, gid_t gid);
//
//  int setxattr(const char *path, const char *name, const char *value,
//               size_t size, int flags);
//  int listxattr(const char *path, char *list, size_t size);
//  int removexattr(const char *path, const char *name);
//
//  int lock(const char* path, struct dokan_file_info *fi, int cmd,
//           struct flock* flock);

 private:
  /** Contains all needed options to mount the requested volume. */
  DokanOptions* options_;

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
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_DOKAN_DOKAN_ADAPTER_H_
