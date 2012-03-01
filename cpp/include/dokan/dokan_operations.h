/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_DOKAN_DOKAN_OPERATIONS_H_
#define CPP_INCLUDE_DOKAN_DOKAN_OPERATIONS_H_

#define WIN32_LEAN_AND_MEAN
#include <windows.h>  // Required by dokan.h
#include <dokan.h>

namespace xtreemfs {
class DokanAdapter;
}

/** Contains functions which are passed into DOKAN_OPERATIONS struct.
 * @file
 *
 * The functions in this file are merely placeholders which call the actual
 * functions of the DokanAdapter instance pointed to by dokan_adapter.
 */

/** Points to the DokanAdapter instance created by mount.xtreemfs.cpp. */
extern xtreemfs::DokanAdapter* dokan_adapter;

extern "C" static int __stdcall XtreemFSDokanCreateFile(
    LPCWSTR path,
    DWORD desired_access,
    DWORD share_mode,
    DWORD creation_disposition,
    DWORD flags_and_attributes,
    PDOKAN_FILE_INFO dokan_file_info);

extern "C" static int __stdcall XtreemFSDokanOpenDirectory(
    LPCWSTR,				// FileName
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanCreateDirectory(
    LPCWSTR,				// FileName
    PDOKAN_FILE_INFO);

// When FileInfo->DeleteOnClose is true, you must delete the file in Cleanup.
extern "C" static int __stdcall XtreemFSDokanCleanup(
    LPCWSTR,      // FileName
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanCloseFile(
    LPCWSTR,      // FileName
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanReadFile(
    LPCWSTR,  // FileName
    LPVOID,   // Buffer
    DWORD,    // NumberOfBytesToRead
    LPDWORD,  // NumberOfBytesRead
    LONGLONG, // Offset
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanWriteFile(
    LPCWSTR,  // FileName
    LPCVOID,  // Buffer
    DWORD,    // NumberOfBytesToWrite
    LPDWORD,  // NumberOfBytesWritten
    LONGLONG, // Offset
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanFlushFileBuffers(
    LPCWSTR, // FileName
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanGetFileInformation(
    LPCWSTR,          // FileName
    LPBY_HANDLE_FILE_INFORMATION, // Buffer
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanFindFiles(
    LPCWSTR,			// PathName
    PFillFindData,		// call this function with PWIN32_FIND_DATAW
    PDOKAN_FILE_INFO);  //  (see PFillFindData definition)

// You should implement either FindFiles or FindFilesWithPattern
extern "C" static int __stdcall XtreemFSDokanFindFilesWithPattern(
    LPCWSTR,			// PathName
    LPCWSTR,			// SearchPattern
    PFillFindData,		// call this function with PWIN32_FIND_DATAW
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanSetFileAttributes(
    LPCWSTR, // FileName
    DWORD,   // FileAttributes
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanSetFileTime(
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
extern "C" static int __stdcall XtreemFSDokanDeleteFile(
    LPCWSTR, // FileName
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanDeleteDirectory( 
    LPCWSTR, // FileName
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanMoveFile(
    LPCWSTR, // ExistingFileName
    LPCWSTR, // NewFileName
    BOOL,	// ReplaceExisiting
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanSetEndOfFile(
    LPCWSTR,  // FileName
    LONGLONG, // Length
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanSetAllocationSize(
    LPCWSTR,  // FileName
    LONGLONG, // Length
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanLockFile(
    LPCWSTR, // FileName
    LONGLONG, // ByteOffset
    LONGLONG, // Length
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanUnlockFile(
    LPCWSTR, // FileName
    LONGLONG,// ByteOffset
    LONGLONG,// Length
    PDOKAN_FILE_INFO);

// Neither GetDiskFreeSpace nor GetVolumeInformation
// save the DokanFileContext->Context.
// Before these methods are called, CreateFile may not be called.
// (ditto CloseFile and Cleanup)

// see Win32 API GetDiskFreeSpaceEx
extern "C" static int __stdcall XtreemFSDokanGetDiskFreeSpace(
    PULONGLONG, // FreeBytesAvailable
    PULONGLONG, // TotalNumberOfBytes
    PULONGLONG, // TotalNumberOfFreeBytes
    PDOKAN_FILE_INFO);

// see Win32 API GetVolumeInformation
extern "C" static int __stdcall XtreemFSDokanGetVolumeInformation(
    LPWSTR, // VolumeNameBuffer
    DWORD,	// VolumeNameSize in num of chars
    LPDWORD,// VolumeSerialNumber
    LPDWORD,// MaximumComponentLength in num of chars
    LPDWORD,// FileSystemFlags
    LPWSTR,	// FileSystemNameBuffer
    DWORD,	// FileSystemNameSize in num of chars
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanUnmount(
    PDOKAN_FILE_INFO);

// Suported since 0.6.0. You must specify the version at DOKAN_OPTIONS.Version.
extern "C" static int __stdcall XtreemFSDokanGetFileSecurity(
    LPCWSTR, // FileName
    // A pointer to SECURITY_INFORMATION value being requested
    PSECURITY_INFORMATION,
     // A pointer to SECURITY_DESCRIPTOR buffer to be filled
    PSECURITY_DESCRIPTOR,
    ULONG, // length of Security descriptor buffer
    PULONG, // LengthNeeded
    PDOKAN_FILE_INFO);

extern "C" static int __stdcall XtreemFSDokanSetFileSecurity(
    LPCWSTR, // FileName
    PSECURITY_INFORMATION,
    PSECURITY_DESCRIPTOR, // SecurityDescriptor
    ULONG, // SecurityDescriptor length
    PDOKAN_FILE_INFO);

/*extern "C" int XtreemFSDokangetattr(const char *path, struct stat *statbuf);
extern "C" int XtreemFSDokanreadlink(const char *path, char *link,
                                      size_t size);
extern "C" int XtreemFSDokanmknod(const char *path, mode_t mode, dev_t dev);
extern "C" int XtreemFSDokanmkdir(const char *path, mode_t mode);
extern "C" int XtreemFSDokanunlink(const char *path);
extern "C" int XtreemFSDokanrmdir(const char *path);
extern "C" int XtreemFSDokansymlink(const char *path, const char *link);
extern "C" int XtreemFSDokanrename(const char *path, const char *newpath);
extern "C" int XtreemFSDokanlink(const char *path, const char *newpath);
extern "C" int XtreemFSDokanchmod(const char *path, mode_t mode);
extern "C" int XtreemFSDokanchown(const char *path, uid_t uid, gid_t gid);
extern "C" int XtreemFSDokantruncate(const char *path, off_t new_file_size);
extern "C" int XtreemFSDokanutime(const char *path, struct utimbuf *ubuf);
extern "C" int XtreemFSDokanlock(const char *, struct dokan_file_info *,
                                  int cmd, struct flock *);
extern "C" int XtreemFSDokanutimens(const char *path,
                                     const struct timespec tv[2]);
extern "C" int XtreemFSDokanopen(const char *path, struct dokan_file_info *fi);
extern "C" int XtreemFSDokanread(const char *path, char *buf, size_t size,
                                  off_t offset, struct dokan_file_info *fi);
extern "C" int XtreemFSDokanwrite(
    const char *path,
    const char *buf,
    size_t size,
    off_t offset,
    struct dokan_file_info *fi);
extern "C" int XtreemFSDokanstatfs(const char *path, struct statvfs *statv);
extern "C" int XtreemFSDokanflush(const char *path, struct dokan_file_info *fi);
extern "C" int XtreemFSDokanrelease(const char *path,
                                     struct dokan_file_info *fi);
extern "C" int XtreemFSDokanfsync(const char *path, int datasync,
                                   struct dokan_file_info *fi);
extern "C" int XtreemFSDokansetxattr(
    const char *path,
    const char *name,
    const char *value,
    size_t size,
    int flags);
extern "C" int XtreemFSDokangetxattr(const char *path, const char *name,
                                      char *value, size_t size);
extern "C" int XtreemFSDokanlistxattr(const char *path, char *list,
                                       size_t size);
extern "C" int XtreemFSDokanremovexattr(const char *path, const char *name);
extern "C" int XtreemFSDokanopendir(const char *path,
                                     struct dokan_file_info *fi);
extern "C" int XtreemFSDokanreaddir(
    const char *path,
    void *buf,
    dokan_fill_dir_t filler,
    off_t offset,
    struct dokan_file_info *fi);
extern "C" int XtreemFSDokanreleasedir(const char *path,
                                        struct dokan_file_info *fi);
extern "C" int XtreemFSDokanfsyncdir(const char *path, int datasync,
                                      struct dokan_file_info *fi);
extern "C" void *XtreemFSDokaninit(struct dokan_conn_info *conn);
extern "C" void XtreemFSDokandestroy(void *userdata);
extern "C" int XtreemFSDokanaccess(const char *path, int mask);
extern "C" int XtreemFSDokancreate(const char *path, mode_t mode,
                                    struct dokan_file_info *fi);
extern "C" int XtreemFSDokanftruncate(const char *path, off_t new_file_size,
                                       struct dokan_file_info *fi);
extern "C" int XtreemFSDokanfgetattr(const char *path, struct stat *statbuf,
                                      struct dokan_file_info *fi);
extern "C" int XtreemFSDokanlock(const char* path, struct dokan_file_info *fi,
                                  int cmd, struct flock* flock_);*/

#endif  // CPP_INCLUDE_DOKAN_DOKAN_OPERATIONS_H_
