/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_FUSE_FUSE_ADAPTER_H_
#define CPP_INCLUDE_FUSE_FUSE_ADAPTER_H_

#include <sys/types.h>
#define FUSE_USE_VERSION 26
#include <fuse.h>

#include <boost/scoped_ptr.hpp>
#include <list>
#include <string>

#include "xtfsutil/xtfsutil_server.h"
#include "xtreemfs/GlobalTypes.pb.h"

namespace xtreemfs {
class Client;
class FuseOptions;
class UserMapping;
class Volume;

namespace pbrpc {
class Stat;
class UserCredentials;
}  // namespace pbrpc

class FuseAdapter {
 public:
  /** Creates a new instance of FuseAdapter, but does not create any libxtreemfs
   *  Client yet.
   *
   *  Use Start() to actually create the client and mount the volume given in
   *  options. May modify options.
   */
  explicit FuseAdapter(FuseOptions* options);

  ~FuseAdapter();

  /** Create client, open volume and start needed threads.
   * @return Returns a list of additional "-o<option>" Fuse options which may be
   *         generated after processing the "options" parameter and have to be
   *         considered before starting Fuse.
   * @remark Ownership of the list elements is transferred to the caller. */
  void Start(std::list<char*>* required_fuse_options);

  /** Shutdown threads, close Volume and Client and blocks until all threads are
   *  stopped. */
  void Stop();

  void GenerateUserCredentials(
      uid_t uid,
      gid_t gid,
      pid_t pid,
      xtreemfs::pbrpc::UserCredentials* user_credentials);

  /** Generate UserCredentials using information from fuse context or the
   *  current process (in that case set fuse_context to NULL). */
  void GenerateUserCredentials(
      struct fuse_context* fuse_context,
      xtreemfs::pbrpc::UserCredentials* user_credentials);

  /** Fill a Fuse stat object with information from an XtreemFS stat. */
  void ConvertXtreemFSStatToFuse(const xtreemfs::pbrpc::Stat& xtreemfs_stat,
                                 struct stat* fuse_stat);

  /** Converts given UNIX file handle flags into XtreemFS symbols. */
  xtreemfs::pbrpc::SYSTEM_V_FCNTL ConvertFlagsUnixToXtreemFS(int flags);

  /** Converts from XtreemFS error codes to the system ones. */
  int ConvertXtreemFSErrnoToFuse(xtreemfs::pbrpc::POSIXErrno xtreemfs_errno);

  // Fuse operations as called by placeholder functions in fuse_operations.h. */
  int statfs(const char *path, struct statvfs *statv);
  int getattr(const char *path, struct stat *statbuf);
  int getxattr(const char *path, const char *name, char *value, size_t size);

  /** Creates CachedDirectoryEntries struct and let fi->fh point to it. */
  int opendir(const char *path, struct fuse_file_info *fi);

  /** Uses the Fuse readdir offset approach to handle readdir requests in chunks
   *  instead of one large request. */
  int readdir(const char *path, void *buf, fuse_fill_dir_t filler, off_t offset,
              struct fuse_file_info *fi);

  /** Deletes CachedDirectoryEntries struct which is hold by fi->fh. */
  int releasedir(const char *path, struct fuse_file_info *fi);

  int utime(const char *path, struct utimbuf *ubuf);
  int utimens(const char *path, const struct timespec tv[2]);
  int create(const char *path, mode_t mode, struct fuse_file_info *fi);
  int mknod(const char *path, mode_t mode, dev_t device);
  int mkdir(const char *path, mode_t mode);
  int open(const char *path, struct fuse_file_info *fi);
  int truncate(const char *path, off_t newsize);
  int ftruncate(const char *path, off_t offset, struct fuse_file_info *fi);
  int write(const char *path, const char *buf, size_t size, off_t offset,
            struct fuse_file_info *fi);
  int flush(const char *path, struct fuse_file_info *fi);
  int read(const char *path, char *buf, size_t size, off_t offset,
           struct fuse_file_info *fi);
  int access(const char *path, int mask);
  int unlink(const char *path);
  int fgetattr(const char *path, struct stat *statbuf,
               struct fuse_file_info *fi);
  int release(const char *path, struct fuse_file_info *fi);

  int readlink(const char *path, char *buf, size_t size);
  int rmdir(const char *path);
  int symlink(const char *path, const char *link);
  int rename(const char *path, const char *newpath);
  int link(const char *path, const char *newpath);
  int chmod(const char *path, mode_t mode);
  int chown(const char *path, uid_t uid, gid_t gid);

  int setxattr(const char *path, const char *name, const char *value,
               size_t size, int flags);
  int listxattr(const char *path, char *list, size_t size);
  int removexattr(const char *path, const char *name);

  int lock(const char* path, struct fuse_file_info *fi, int cmd,
           struct flock* flock);

 private:
  /** Contains all needed options to mount the requested volume. */
  FuseOptions* options_;

  /** The chosen UserMapping provides methods to translate between local and
   *  remote usernames and groups. */
  boost::scoped_ptr<UserMapping> user_mapping_;

  /** Created libxtreemfs Client. */
  boost::scoped_ptr<Client> client_;

  /** Opened libxtreemfs Volume. */
  Volume* volume_;

  /** Server for processing commands sent from the xtfsutil tool
      via xctl files. */
  XtfsUtilServer xctl_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_FUSE_FUSE_ADAPTER_H_
