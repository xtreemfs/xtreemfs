/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "dokan/dokan_operations.h"

#include "dokan/dokan_adapter.h"
#include "util/logging.h"

using namespace std;
using namespace xtreemfs::util;

xtreemfs::DokanAdapter* dokan_adapter = NULL;

int __stdcall XtreemFSDokanCreateFile(LPCWSTR path,
                                      DWORD desired_access,
                                      DWORD share_mode,
                                      DWORD creation_disposition,
                                      DWORD flags_and_attributes,
                                      PDOKAN_FILE_INFO dokan_file_info) {
  dokan_adapter->CreateFile(path,
                            desired_access,
                            share_mode,
                            creation_disposition,
                            flags_and_attributes,
                            dokan_file_info);
}

int __stdcall XtreemFSDokanOpenDirectory(
    LPCWSTR path,
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanCreateDirectory(
    LPCWSTR path,
    PDOKAN_FILE_INFO dokan_file_info);

// When FileInfo->DeleteOnClose is true, you must delete the file in Cleanup.
int __stdcall XtreemFSDokanCleanup(
    LPCWSTR path,
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanCloseFile(
    LPCWSTR path,
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanReadFile(
    LPCWSTR path,
    LPVOID,   // Buffer
    DWORD,    // NumberOfBytesToRead
    LPDWORD,  // NumberOfBytesRead
    LONGLONG, // Offset
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanWriteFile(
    LPCWSTR path,
    LPCVOID,  // Buffer
    DWORD,    // NumberOfBytesToWrite
    LPDWORD,  // NumberOfBytesWritten
    LONGLONG, // Offset
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanFlushFileBuffers(
    LPCWSTR path,
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanGetFileInformation(
    LPCWSTR path,
    LPBY_HANDLE_FILE_INFORMATION, // Buffer
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanFindFiles(
    LPCWSTR path,
    PFillFindData,		// call this function with PWIN32_FIND_DATAW
    PDOKAN_FILE_INFO dokan_file_info);  //  (see PFillFindData definition)

// You should implement either FindFiles or FindFilesWithPattern
int __stdcall XtreemFSDokanFindFilesWithPattern(
    LPCWSTR path,
    LPCWSTR,			// SearchPattern
    PFillFindData,		// call this function with PWIN32_FIND_DATAW
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanSetFileAttributes(
    LPCWSTR path,
    DWORD,   // FileAttributes
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanSetFileTime(
    LPCWSTR path,
    CONST FILETIME*, // CreationTime
    CONST FILETIME*, // LastAccessTime
    CONST FILETIME*, // LastWriteTime
    PDOKAN_FILE_INFO dokan_file_info);

// You should not delete file on DeleteFile or DeleteDirectory.
// When DeleteFile or DeleteDirectory, you must check whether
// you can delete the file or not, and return 0 (when you can delete it)
// or appropriate error codes such as -ERROR_DIR_NOT_EMPTY,
// -ERROR_SHARING_VIOLATION.
// When you return 0 (ERROR_SUCCESS), you get Cleanup with
// FileInfo->DeleteOnClose set TRUE and you have to delete the
// file in Close.
int __stdcall XtreemFSDokanDeleteFile(
    LPCWSTR path,
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanDeleteDirectory( 
    LPCWSTR path,
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanMoveFile(
    LPCWSTR path,
    LPCWSTR new_path,
    BOOL,	// ReplaceExisiting
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanSetEndOfFile(
    LPCWSTR path,
    LONGLONG, // Length
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanSetAllocationSize(
    LPCWSTR path,
    LONGLONG, // Length
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanLockFile(
    LPCWSTR path,
    LONGLONG, // ByteOffset
    LONGLONG, // Length
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanUnlockFile(
    LPCWSTR path,
    LONGLONG,// ByteOffset
    LONGLONG,// Length
    PDOKAN_FILE_INFO dokan_file_info);

// Neither GetDiskFreeSpace nor GetVolumeInformation
// save the DokanFileContext->Context.
// Before these methods are called, CreateFile may not be called.
// (ditto CloseFile and Cleanup)

// see Win32 API GetDiskFreeSpaceEx
int __stdcall XtreemFSDokanGetDiskFreeSpace(
    PULONGLONG, // FreeBytesAvailable
    PULONGLONG, // TotalNumberOfBytes
    PULONGLONG, // TotalNumberOfFreeBytes
    PDOKAN_FILE_INFO dokan_file_info);

// see Win32 API GetVolumeInformation
int __stdcall XtreemFSDokanGetVolumeInformation(
    LPWSTR, // VolumeNameBuffer
    DWORD,	// VolumeNameSize in num of chars
    LPDWORD,// VolumeSerialNumber
    LPDWORD,// MaximumComponentLength in num of chars
    LPDWORD,// FileSystemFlags
    LPWSTR,	// FileSystemNameBuffer
    DWORD,	// FileSystemNameSize in num of chars
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanUnmount(
    PDOKAN_FILE_INFO dokan_file_info);

// Suported since 0.6.0. You must specify the version at DOKAN_OPTIONS.Version.
int __stdcall XtreemFSDokanGetFileSecurity(
    LPCWSTR path,
    // A pointer to SECURITY_INFORMATION value being requested
    PSECURITY_INFORMATION,
     // A pointer to SECURITY_DESCRIPTOR buffer to be filled
    PSECURITY_DESCRIPTOR,
    ULONG, // length of Security descriptor buffer
    PULONG, // LengthNeeded
    PDOKAN_FILE_INFO dokan_file_info);

int __stdcall XtreemFSDokanSetFileSecurity(
    LPCWSTR path,
    PSECURITY_INFORMATION,
    PSECURITY_DESCRIPTOR, // SecurityDescriptor
    ULONG, // SecurityDescriptor length
    PDOKAN_FILE_INFO dokan_file_info);

//int xtreemfs_dokan_getattr(const char *path, struct stat *statbuf) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//     Logging::log->getLog(LEVEL_DEBUG) << "getattr on path " << path << endl;
//  }
//  return dokan_adapter->getattr(path, statbuf);
//}
//
//int xtreemfs_dokan_readlink(const char *path, char *link, size_t size) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//     Logging::log->getLog(LEVEL_DEBUG)
//         << "xtreemfs_dokan_readlink on path " << path << endl;
//  }
//  return dokan_adapter->readlink(path, link, size);
//}
//
//int xtreemfs_dokan_mknod(const char *path, mode_t mode, dev_t dev) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//     Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_dokan_mknod on path "
//         << path << endl;
//  }
//  return dokan_adapter->mknod(path, mode, dev);
//}
//
//int xtreemfs_dokan_mkdir(const char *path, mode_t mode) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//     Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_dokan_mkdir on path "
//         << path << endl;
//  }
//  return dokan_adapter->mkdir(path, mode);
//}
//
//int xtreemfs_dokan_unlink(const char *path) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//     Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_dokan_unlink " << path
//         << endl;
//  }
//  return dokan_adapter->unlink(path);
//}
//
//int xtreemfs_dokan_rmdir(const char *path) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_dokan_rmdir on path " << path
//        << endl;
//  }
//  return dokan_adapter->rmdir(path);
//}
//
//int xtreemfs_dokan_symlink(const char *path, const char *link) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_dokan_symlink on path "
//        << path << endl;
//  }
//  return dokan_adapter->symlink(path, link);
//}
//
//int xtreemfs_dokan_rename(const char *path, const char *newpath) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)
//        << "xtreemfs_dokan_rename on path " << path << " to " << newpath <<
//        endl;
//  }
//  return dokan_adapter->rename(path, newpath);
//}
//
//int xtreemfs_dokan_link(const char *path, const char *newpath) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)
//        << "xtreemfs_dokan_link on path " << path << " " << newpath << endl;
//  }
//  return dokan_adapter->link(path, newpath);
//}
//
//int xtreemfs_dokan_chmod(const char *path, mode_t mode) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_chmod on path " << path
//        << endl;
//  }
//  return dokan_adapter->chmod(path, mode);
//}
//
//int xtreemfs_dokan_lock(const char* path,
//                       struct dokan_file_info *fi,
//                       int cmd,
//                       struct flock* flock_) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    string log_command;
//    switch(cmd) {
//      case F_GETLK:
//        log_command = "check lock";
//        break;
//      case F_SETLK:
//        log_command = "set lock";
//        break;
//      case F_SETLKW:
//        log_command = "set lock and wait";
//        break;
//      default:
//        log_command = "unknown lock command";
//        break;
//    }
//    string log_type;
//    switch(flock_->l_type) {
//      case F_UNLCK:
//        log_type = "unlock";
//        break;
//      case F_RDLCK:
//        log_type = "read lock";
//        break;
//      case F_WRLCK:
//        log_type = "write lock";
//        break;
//      default:
//        log_type = "unknown lock type";
//        break;
//    }
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_lock on path " << path
//        << " command: " << log_command << " type: " << log_type << " start: "
//        << flock_->l_start << " length: "<< flock_->l_len << " pid: "
//        << flock_->l_pid << endl;
//  }
//  return dokan_adapter->lock(path, fi, cmd, flock_);
//}
//
//int xtreemfs_dokan_chown(const char *path, uid_t uid, gid_t gid) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_chown on path " << path
//        << endl;
//  }
//  return dokan_adapter->chown(path, uid, gid);
//}
//
//int xtreemfs_dokan_truncate(const char *path, off_t new_file_size) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)
//        << "xtreemfs_dokan_truncate on path " << path
//        << " size:" << new_file_size << endl;
//  }
//  return dokan_adapter->truncate(path, new_file_size);
//}
//
//int xtreemfs_dokan_utime(const char *path, struct utimbuf *ubuf) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_utime on path " << path
//        << endl;
//  }
//  return dokan_adapter->utime(path, ubuf);
//}
//
//int xtreemfs_dokan_utimens(const char *path, const struct timespec tv[2]) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_dokan_utimens on path "
//        << path << endl;
//  }
//  return dokan_adapter->utimens(path, tv);
//}
//
//int xtreemfs_dokan_open(const char *path, struct dokan_file_info *fi) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//     Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_open on path " << path
//         << endl;
//  }
//  return dokan_adapter->open(path, fi);
//}
//
//int xtreemfs_dokan_release(const char *path, struct dokan_file_info *fi) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//     Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_release " << path
//         << endl;
//  }
//  return dokan_adapter->release(path, fi);
//}
//
//
//int xtreemfs_dokan_read(
//    const char *path, char *buf,
//    size_t size, off_t offset, struct dokan_file_info *fi) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_dokan_read " << path
//        << " s:" << size <<  " o:" << offset << endl;
//  }
//  int count = dokan_adapter->read(path, buf, size, offset, fi);
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_dokan_read finished " << path
//        << " s:" << size <<  " o:" << offset << " r:" << count << endl;
//  }
//  return count;
//}
//
//int xtreemfs_dokan_write(const char *path, const char *buf, size_t size,
//    off_t offset, struct dokan_file_info *fi) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_write " << path
//        << " s: " << size <<  " o:" << offset << endl;
//  }
//  int count = dokan_adapter->write(path, buf, size, offset, fi);
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG) << "xtreemfs_dokan_write finished " << path
//        << " s:" << size <<  " o:" << offset << " w:" << count << endl;
//  }
//  return count;
//}
//
//int xtreemfs_dokan_statfs(const char *path, struct statvfs *statv) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_statfs " << path
//        << endl;
//  }
//  return dokan_adapter->statfs(path, statv);
//}
//
///** Unlink fsync(), flush() requests are NOT initiated from the user.
// *
// * Instead, flush() is a Dokan internal mechanism to avoid the problem that
// * the return value of release() will be ignored.
// *
// * Therefore, a flush() will be called by Dokan with every close() executed by
// * the user. Only errors returned by this flush() operation can be returned
// * to the close() of the user.
// */
//int xtreemfs_dokan_flush(const char *path, struct dokan_file_info *fi) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_flush " << path
//        << endl;
//  }
//  return dokan_adapter->flush(path, fi);
//}
//
//int xtreemfs_dokan_fsync(const char *path, int datasync,
//    struct dokan_file_info *fi) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_fsync " << path
//        << endl;
//  }
//
//  // We ignore the datasync parameter as all metadata operations are
//  // synchronous and therefore never have to be flushed.
//  return dokan_adapter->flush(path, fi);
//}
//
//int xtreemfs_dokan_setxattr(
//    const char *path, const char *name,
//    const char *value, size_t size, int flags) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//     Logging::log->getLog(LEVEL_DEBUG)
//         << "xtreemfs_dokan_setxattr " << " " << path << " " << name << endl;
//  }
//  return dokan_adapter->setxattr(path, name, value, size, flags);
//}
//
//int xtreemfs_dokan_getxattr(
//    const char *path, const char *name, char *value, size_t size) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)
//        << "xtreemfs_dokan_getxattr " << " " << path << " " << name << " "
//        << size << endl;
//  }
//  return dokan_adapter->getxattr(path, name, value, size);
//}
//
//int xtreemfs_dokan_listxattr(const char *path, char *list, size_t size) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//     Logging::log->getLog(LEVEL_DEBUG)
//         << "xtreemfs_dokan_listxattr " << path << " " << size << endl;
//  }
//  return dokan_adapter->listxattr(path, list, size);
//}
//
//int xtreemfs_dokan_removexattr(const char *path, const char *name) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//     Logging::log->getLog(LEVEL_DEBUG)
//         << "xtreemfs_dokan_removexattr " << " " << path << " " << name << endl;
//  }
//  return dokan_adapter->removexattr(path, name);
//}
//
//int xtreemfs_dokan_opendir(const char *path, struct dokan_file_info *fi) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_opendir " << path
//        << endl;
//  }
//  return dokan_adapter->opendir(path, fi);
//}
//
//int xtreemfs_dokan_readdir(
//    const char *path, void *buf,
//    dokan_fill_dir_t filler, off_t offset, struct dokan_file_info *fi) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_readdir " << path
//        << endl;
//  }
//  return dokan_adapter->readdir(path, buf, filler, offset, fi);
//}
//
//int xtreemfs_dokan_releasedir(const char *path, struct dokan_file_info *fi) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG) && path != NULL) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_releasedir " << path
//        << endl;
//  }
//  return dokan_adapter->releasedir(path, fi);
//}
//
//int xtreemfs_dokan_fsyncdir(
//    const char *path, int datasync, struct dokan_file_info *fi) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_fsyncdir " << path
//        << endl;
//  }
//
//  // Like fsync, but for directories - not required for XtreemFS.
//  return 0;
//}
//
//void *xtreemfs_dokan_init(struct dokan_conn_info *conn) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_init " << endl;
//  }
//
//  // http://sourceforge.net/apps/mediawiki/dokan/index.php?title=Dokan_file_info
//  // TODO(mberlin): Check for valid parameters.
//  conn->async_read = 5;
//  conn->max_readahead = 10 * 128 * 1024;
//  conn->max_write = 128 * 1024;
//
//#if DOKAN_MAJOR_VERSION > 2 || (DOKAN_MAJOR_VERSION == 2 && DOKAN_MINOR_VERSION >= 8)  // NOLINT
//  conn->capable
//    = DOKAN_CAP_ASYNC_READ | DOKAN_CAP_BIG_WRITES
//      | DOKAN_CAP_ATOMIC_O_TRUNC | DOKAN_CAP_POSIX_LOCKS;
//  conn->want
//    = DOKAN_CAP_ASYNC_READ | DOKAN_CAP_BIG_WRITES
//      | DOKAN_CAP_ATOMIC_O_TRUNC | DOKAN_CAP_POSIX_LOCKS;
//#endif
//
//  struct dokan_context* context = dokan_get_context();
//  return context->private_data;
//}
//
//void xtreemfs_dokan_destroy(void *userdata) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_destroy " << endl;
//  }
//}
//
///**
// * This method will only be called by Dokan if "-o default_permissions" is not
// * send to Dokan (for instance before changing the working directory).
// *
// * If "-o default_permissions" is enabled, Dokan does determine on its own, based
// * on the result of the getattr, if the user is allowed to access the directory.
// */
//int xtreemfs_dokan_access(const char *path, int mask) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)  << "xtreemfs_dokan_access " << path
//        << endl;
//  }
//  return dokan_adapter->access(path, mask);
//}
//
//int xtreemfs_dokan_create(const char *path, mode_t mode,
//    struct dokan_file_info *fi) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//     Logging::log->getLog(LEVEL_DEBUG)  << "create on path " << path << endl;
//  }
//  return dokan_adapter->create(path, mode, fi);
//}
//
//int xtreemfs_dokan_ftruncate(
//    const char *path, off_t new_file_size, struct dokan_file_info *fi) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//    Logging::log->getLog(LEVEL_DEBUG)
//        << "xtreemfs_dokan_ftruncate on path " << path
//        << " size:" << new_file_size << endl;
//  }
//  return dokan_adapter->ftruncate(path, new_file_size, fi);
//}
//
//int xtreemfs_dokan_fgetattr(
//    const char *path, struct stat *statbuf, struct dokan_file_info *fi) {
//  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
//     Logging::log->getLog(LEVEL_DEBUG)  << "fgetattr on path " << path << endl;
//  }
//  return dokan_adapter->fgetattr(path, statbuf, fi);
//}
