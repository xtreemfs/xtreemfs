/*
 * Copyright (c) 2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDEXTFSUTIL_XTFSUTIL_SERVER_H_
#define	CPP_INCLUDEXTFSUTIL_XTFSUTIL_SERVER_H_

#include <boost/thread/mutex.hpp>
#include <map>
#include <string>

#include "json/json-forwards.h"
#include "libxtreemfs/volume.h"

namespace xtreemfs {

/** Handle for a xcntl pseudo file used to communicate with
 * the xtfsutil server inside the client.
 */
class XCtlFile {
 public:
  XCtlFile() : in_use_(false), last_result_() {}

  void set_last_result(std::string _last_result) {
    this->last_result_ = _last_result;
  }
  std::string last_result() const {
    return last_result_;
  }
  void set_in_use(bool _in_use) {
    this->in_use_ = _in_use;
  }
  bool in_use() const {
    return in_use_;
  }
  void set_user(uid_t uid, gid_t gid) {
    uid_ = uid;
    gid_ = gid;
  }

  bool is_owner(uid_t uid, gid_t gid) {
    // Always allow root to read all files.
    // Required for APPLE.
    return (uid == 0 && gid == 0)
           || (uid == uid_ && gid == gid_);
  }

  uid_t get_uid() const {
    return uid_;
  }
  gid_t get_gid() const {
    return gid_;
  }
 private:
  /** True, if an operation is currently being executed for this file. */
  volatile bool in_use_;
  /** Result of last operation executed, encoded in JSON. */
  std::string last_result_;
  /** User who owns this file. */
  uid_t uid_;
  gid_t gid_;
};

/** part of the xtfsutil that runs in the client (FUSE...)
 * and handles all requests from the xtfsutil tool.
 * xtfsutil uses special files to communicate with the client
 * commands are executed using write and results are obtained via read.
 * A write will block until the operation has finished.
 */
class XtfsUtilServer {
 public:
  /** @param prefix is the path prefix used to identify xctl pseudo files. */
  XtfsUtilServer(const std::string& prefix);

  ~XtfsUtilServer();

  /** Sets the volume to be used. */
  void set_volume(Volume* volume);

  /** Returns true, if the path points to a xctl pseudo file. */
  bool checkXctlFile(const std::string& path);

  /** Reads the last response into buf.
   *  @returns 0 on success, -1*errno otherwise.
   */
  int read(uid_t uid,
           gid_t gid,
           const std::string& path,
           char *buf,
           size_t size,
           off_t offset);

  /** Parses and executes the command from buf.
   *  @returns 0 on success, -1*errno otherwise.
   */
  int write(uid_t uid,
            gid_t gid,
            const xtreemfs::pbrpc::UserCredentials& uc,
            const std::string& path,
            const char* buf,
            size_t size);

  /** Stats a xctl pseudo file. */
  int getattr(uid_t uid,
              gid_t gid,
              const std::string& path,
              struct stat* st_buf);

  /** Delets a xctl pseudo file. */
  int unlink(uid_t uid,
             gid_t gid,
             const std::string& path);

  /** Creates a xctl pseudo file. */
  int create(uid_t uid,
             gid_t gid,
             const std::string& path);

 private:
  /** Retrieves the file from the internal map. Creates it if it doesn't exist
   * and create is true.
   * @returns the file or NULL if the file does not exist or is owned by another
   * user.
   */
  XCtlFile* FindFile(uid_t uid,
                      gid_t gid,
                      const std::string& path,
                      bool create);

  /** Parses the input JSON and executes the operation.
   *  Stores the result in file.
   */
  void ParseAndExecute(const xtreemfs::pbrpc::UserCredentials& uc,
                       const std::string& input_str,
                       XCtlFile* file);

  /** Returns a list of errors. */
  void OpGetErrors(const xtreemfs::pbrpc::UserCredentials& uc,
                   const Json::Value& input,
                   Json::Value* output);

  /** Returns XtreemFS-specific attributes. */
  void OpStat(const xtreemfs::pbrpc::UserCredentials& uc,
              const Json::Value& input,
              Json::Value* output);

  /** Changes the default striping policy. Volumes only. */
  void OpSetDefaultSP(const xtreemfs::pbrpc::UserCredentials& uc,
                      const Json::Value& input,
                      Json::Value* output);

  /** Changes the default replication policy. Volumes only. */
  void OpSetDefaultRP(const xtreemfs::pbrpc::UserCredentials& uc,
                      const Json::Value& input,
                      Json::Value* output);

  /** Changes the OSD selection policy (OSP). Volume only. */
  void OpSetOSP(const xtreemfs::pbrpc::UserCredentials& uc,
                const Json::Value& input,
                Json::Value* output);

  /** Changes the Replica selection policy (RSP). Volume only. */
  void OpSetRSP(const xtreemfs::pbrpc::UserCredentials& uc,
                const Json::Value& input,
                Json::Value* output);

  void OpSetReplicationPolicy(const xtreemfs::pbrpc::UserCredentials& uc,
                              const Json::Value& input,
                              Json::Value* output);

  void OpAddReplica(const xtreemfs::pbrpc::UserCredentials& uc,
                    const Json::Value& input,
                    Json::Value* output);

  void OpRemoveReplica(const xtreemfs::pbrpc::UserCredentials& uc,
                       const Json::Value& input,
                       Json::Value* output);

  void OpGetSuitableOSDs(const xtreemfs::pbrpc::UserCredentials& uc,
                         const Json::Value& input,
                         Json::Value* output);

  void OpSetPolicyAttr(const xtreemfs::pbrpc::UserCredentials& uc,
                       const Json::Value& input,
                       Json::Value* output);

  void OpListPolicyAttr(const xtreemfs::pbrpc::UserCredentials& uc,
                        const Json::Value& input,
                        Json::Value* output);

  /** Mutex to protect xctl_files_. */
  boost::mutex xctl_files_mutex_;
  /** Map of xctl pseudo files. */
  std::map<std::string, XCtlFile*> xctl_files_;
  /** Path prefix. */
  std::string prefix_;
  /** Volume on which to execute operations. */
  Volume* volume_;
};

}  // namespace xtreemfs

#endif	// CPP_INCLUDEXTFSUTIL_XTFSUTIL_SERVER_H_