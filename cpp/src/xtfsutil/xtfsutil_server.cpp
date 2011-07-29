/*
 * Copyright (c) 2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <boost/algorithm/string/case_conv.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/thread/mutex.hpp>
#include <cassert>
#include <errno.h>
#include <list>
#include <sys/types.h>
#include <sys/stat.h>

#include "json/json.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/error_log.h"
#include "util/logging.h"
#include "xtfsutil/xtfsutil_server.h"

using namespace std;
using namespace xtreemfs::util;

namespace xtreemfs {

XtfsUtilServer::XtfsUtilServer(const string& prefix) : prefix_(prefix) {
}

XtfsUtilServer::~XtfsUtilServer() {
  for (map<std::string, XCtlFile*>::iterator iter = xctl_files_.begin();
      iter != xctl_files_.end();
      ++iter) {
    delete iter->second;
  }
}

void XtfsUtilServer::set_volume(Volume* volume) {
  volume_ = volume;
}

void XtfsUtilServer::ParseAndExecute(const xtreemfs::pbrpc::UserCredentials& uc,
                                     const std::string& input_str,
                                     XCtlFile* file) {
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "xctl op: " << input_str << endl;
  }
  // Parse json input and validate.
  Json::Reader reader;
  Json::Value input;
  if (!reader.parse(input_str, input, false)) {
    file->set_last_result("{ \"error\":\"Input is not valid JSON\" }");
    return;
  }
  if (!input.isObject()
      || !input.isMember("operation")
      || !input["operation"].isString()) {
    file->set_last_result("{ \"error\":\"Input is not valid JSON. "
                          "Expected object with operation field.\" }");
    return;
  }


  string op_name = input["operation"].asString();
  Json::Value result(Json::objectValue);

  try {
    if (op_name == "getErrors") {
      OpGetErrors(uc, input, &result);
    } else if (op_name == "getattr") {
      OpStat(uc, input, &result);
    } else if (op_name == "setDefaultSP") {
      OpSetDefaultSP(uc, input, &result);
    } else if (op_name == "setDefaultRP") {
      OpSetDefaultRP(uc, input, &result);
    } else if (op_name == "setOSP") {
      OpSetOSP(uc, input, &result);
    } else if (op_name == "setRSP") {
      OpSetRSP(uc, input, &result);
    } else if (op_name == "addReplica") {
      OpAddReplica(uc, input, &result);
    } else if (op_name == "removeReplica") {
      OpRemoveReplica(uc, input, &result);
    } else if (op_name == "getSuitableOSDs") {
      OpGetSuitableOSDs(uc, input, &result);
    } else if (op_name == "setPolicyAttr") {
      OpSetPolicyAttr(uc, input, &result);
    } else if (op_name == "listPolicyAttrs") {
      OpListPolicyAttr(uc, input, &result);
    } else if (op_name == "setReplicationPolicy") {
      OpSetReplicationPolicy(uc, input, &result);
    } else {
      file->set_last_result("{ \"error\":\"Unknown operation.\" }");
      return;
    }
  } catch (const XtreemFSException &e) {
    result["error"] = Json::Value(e.what());
  } catch (const exception &e) {
    result["error"] = Json::Value(string("Unknown error: ") + e.what());
  }
  
  Json::FastWriter writer;
  file->set_last_result(writer.write(result));
}

void XtfsUtilServer::OpGetErrors(const xtreemfs::pbrpc::UserCredentials& uc,
                                 const Json::Value& input,
                                 Json::Value* output) {
  Json::Value result = Json::Value(Json::arrayValue);
  list<string> errors = xtreemfs::util::ErrorLog::error_log->error_messages();
  for (list<string>::iterator iter = errors.begin();
       iter != errors.end(); ++iter) {
    result.append(*iter);
  }
  (*output)["result"] = result;
}

void XtfsUtilServer::OpStat(const xtreemfs::pbrpc::UserCredentials& uc,
                            const Json::Value& input,
                            Json::Value* output) {
  if (!input.isMember("path")
      || !input["path"].isString()) {
    (*output)["error"] = Json::Value("'path' field is missing.");
    return;
  }
  const string path = input["path"].asString();

  boost::scoped_ptr<xtreemfs::pbrpc::listxattrResponse>
      xattrs(volume_->ListXAttrs(uc, path, false));
  map<string, string> xtfs_attrs;
  for (int i = 0; i < xattrs->xattrs_size(); ++i) {
    const xtreemfs::pbrpc::XAttr& xattr = xattrs->xattrs(i);
    if (boost::starts_with(xattr.name(),"xtreemfs.")) {
      xtfs_attrs[xattr.name()] = xattr.value();
    }
  }

  Json::Value result(Json::objectValue);

  result["fileId"] = Json::Value(xtfs_attrs["xtreemfs.file_id"]);
  result["url"] = Json::Value(xtfs_attrs["xtreemfs.url"]);
  result["object_type"] = Json::Value(xtfs_attrs["xtreemfs.object_type"]);
  result["group"] = Json::Value(xtfs_attrs["xtreemfs.group"]);
  result["owner"] = Json::Value(xtfs_attrs["xtreemfs.owner"]);
  result["acl"] = Json::Value(xtfs_attrs["xtreemfs.acl"]);

  if (xtfs_attrs["xtreemfs.object_type"] == "1") {
    // File.
    Json::Reader reader;
    Json::Value loc_json;
    if (reader.parse(xtfs_attrs["xtreemfs.locations"], loc_json, false)) {
      result["locations"] = loc_json;
    }
  } else if (xtfs_attrs["xtreemfs.object_type"] == "2") {
    // Directory.
    Json::Reader reader;
    Json::Value sp_json;
    if (reader.parse(xtfs_attrs["xtreemfs.default_sp"], sp_json, false)) {
      result["default_sp"] = sp_json;
    }
    Json::Value rp_json;
    if (reader.parse(xtfs_attrs["xtreemfs.default_rp"], rp_json, false)) {
      result["default_rp"] = rp_json;
    }
    if (path == "/") {
      // Get more volume details.
      result["free_space"] = Json::Value(xtfs_attrs["xtreemfs.free_space"]);
      result["used_space"] = Json::Value(xtfs_attrs["xtreemfs.used_space"]);
      result["ac_policy_id"] =
          Json::Value(xtfs_attrs["xtreemfs.ac_policy_id"]);
      result["osel_policy"] = Json::Value(xtfs_attrs["xtreemfs.osel_policy"]);
      result["rsel_policy"] = Json::Value(xtfs_attrs["xtreemfs.rsel_policy"]);
      result["num_dirs"] = Json::Value(xtfs_attrs["xtreemfs.num_dirs"]);
      result["num_files"] = Json::Value(xtfs_attrs["xtreemfs.num_files"]);
      result["usable_osds"] = Json::Value(xtfs_attrs["xtreemfs.usable_osds"]);
    }
  } else if (xtfs_attrs["xtreemfs.object_type"] == "3") {
    // Softlink.
    string link_target;
    volume_->ReadLink(uc, path, &link_target);
    result["link_target"] = Json::Value(link_target);
  }

  (*output)["result"] = result;

}

void XtfsUtilServer::OpSetDefaultSP(const xtreemfs::pbrpc::UserCredentials& uc,
                                    const Json::Value& input,
                                    Json::Value* output) {
  if (!input.isMember("path")
      || !input["path"].isString()
      || !input.isMember("pattern")
      || !input["pattern"].isString()
      || !input.isMember("width")
      || !input["width"].isInt()
      || !input.isMember("size")
      || !input["size"].isInt()) {
    (*output)["error"] = Json::Value("One of the following fields is missing or"
                                     " has an invalid value: path, pattern, "
                                     "width, member");
    return;
  }
  const string path = input["path"].asString();

  Json::Value xattr_value(Json::objectValue);
  xattr_value["pattern"] = input["pattern"];
  xattr_value["size"] = input["size"];
  xattr_value["width"] = input["width"];

  Json::FastWriter writer;
  volume_->SetXAttr(uc,
                    path,
                    "xtreemfs.default_sp",
                    writer.write(xattr_value),
                    xtreemfs::pbrpc::XATTR_FLAGS_REPLACE);
  (*output)["result"] = Json::Value(Json::objectValue);
}

void XtfsUtilServer::OpSetDefaultRP(const xtreemfs::pbrpc::UserCredentials& uc,
                                    const Json::Value& input,
                                    Json::Value* output) {
  if (!input.isMember("path")
      || !input["path"].isString()
      || !input.isMember("replication-factor")
      || !input["replication-factor"].isInt()
      || !input.isMember("update-policy")
      || !input["update-policy"].isString()
      || !input.isMember("replication-flags")
      || !input["replication-flags"].isInt()) {
    (*output)["error"] = Json::Value("One of the following fields is missing or"
                                     " has an invalid value: path, factor, "
                                     "update-policy, replication-flags");
    return;
  }
  const string path = input["path"].asString();

  Json::Value xattr_value(Json::objectValue);
  xattr_value["replication-factor"] = input["replication-factor"];
  xattr_value["update-policy"] = input["update-policy"];
  xattr_value["replication-flags"] = input["replication-flags"];

  Json::FastWriter writer;
  volume_->SetXAttr(uc,
                    path,
                    "xtreemfs.default_rp",
                    writer.write(xattr_value),
                    xtreemfs::pbrpc::XATTR_FLAGS_REPLACE);
  (*output)["result"] = Json::Value(Json::objectValue);
}

void XtfsUtilServer::OpSetOSP(const xtreemfs::pbrpc::UserCredentials& uc,
                              const Json::Value& input,
                              Json::Value* output) {
  if (!input.isMember("path")
      || !input["path"].isString()
      || !input.isMember("policy")
      || !input["policy"].isString()) {
    (*output)["error"] = Json::Value("One of the following fields is missing or"
                                     " has an invalid value: path, policy");
    return;
  }
  const string path = input["path"].asString();

  volume_->SetXAttr(uc,
                    path,
                    "xtreemfs.osel_policy",
                    input["policy"].asString(),
                    xtreemfs::pbrpc::XATTR_FLAGS_REPLACE);
  (*output)["result"] = Json::Value(Json::objectValue);
}

void XtfsUtilServer::OpSetRSP(const xtreemfs::pbrpc::UserCredentials& uc,
                              const Json::Value& input,
                              Json::Value* output) {
  if (!input.isMember("path")
      || !input["path"].isString()
      || !input.isMember("policy")
      || !input["policy"].isString()) {
    (*output)["error"] = Json::Value("One of the following fields is missing or"
                                     " has an invalid value: path, policy");
    return;
  }
  const string path = input["path"].asString();

  volume_->SetXAttr(uc,
                    path,
                    "xtreemfs.rsel_policy",
                    input["policy"].asString(),
                    xtreemfs::pbrpc::XATTR_FLAGS_REPLACE);
  (*output)["result"] = Json::Value(Json::objectValue);
}

void XtfsUtilServer::OpSetPolicyAttr(const xtreemfs::pbrpc::UserCredentials& uc,
                                     const Json::Value& input,
                                     Json::Value* output) {
  if (!input.isMember("path")
      || !input["path"].isString()
      || !input.isMember("attribute")
      || !input["attribute"].isString()
      || !input.isMember("value")
      || !input["value"].isString()) {
    (*output)["error"] = Json::Value("One of the following fields is missing or"
                                     " has an invalid value: path, attribute,"
                                     " value.");
    return;
  }
  const string path = input["path"].asString();

  volume_->SetXAttr(uc,
                    path,
                    "xtreemfs.policies." + input["attribute"].asString(),
                    input["value"].asString(),
                    xtreemfs::pbrpc::XATTR_FLAGS_REPLACE);
  (*output)["result"] = Json::Value(Json::objectValue);
}

void XtfsUtilServer::OpListPolicyAttr(
    const xtreemfs::pbrpc::UserCredentials& uc,
    const Json::Value& input,
    Json::Value* output) {
  if (!input.isMember("path")
      || !input["path"].isString()) {
    (*output)["error"] = Json::Value("One of the following fields is missing or"
                                     " has an invalid value: path.");
    return;
  }
  const string path = input["path"].asString();

  boost::scoped_ptr<xtreemfs::pbrpc::listxattrResponse>
      xattrs(volume_->ListXAttrs(uc, path, false));
  (*output)["result"] = Json::Value(Json::objectValue);
  for (int i = 0; i < xattrs->xattrs_size(); ++i) {
    if (boost::starts_with(xattrs->xattrs(i).name(),"xtreemfs.policy.")) {
      (*output)["result"][xattrs->xattrs(i).name()] = xattrs->xattrs(i).value();
    }
  }
}

void XtfsUtilServer::OpSetReplicationPolicy(
    const xtreemfs::pbrpc::UserCredentials& uc,
    const Json::Value& input,
    Json::Value* output) {
  if (!input.isMember("path")
      || !input["path"].isString()
      || !input.isMember("policy")
      || !input["policy"].isString()) {
    (*output)["error"] = Json::Value("One of the following fields is missing or"
                                     " has an invalid value: path, policy");
    return;
  }
  const string policy_name = input["policy"].asString();

  if (policy_name != "ronly"
      && policy_name != "WqRq"
      && policy_name != "WaRa"
      && policy_name != "") {
    (*output)["error"] = Json::Value("Policy must be one of the following: "
                                     "<empty string>, ronly, WaRa, WqRq");
    return;
  }

  const string path = input["path"].asString();
  volume_->SetXAttr(uc,
                    path,
                    "xtreemfs.set_repl_update_policy",
                    policy_name,
                    xtreemfs::pbrpc::XATTR_FLAGS_REPLACE);
  (*output)["result"] = Json::Value(Json::objectValue);
}

void XtfsUtilServer::OpAddReplica(
    const xtreemfs::pbrpc::UserCredentials& uc,
    const Json::Value& input,
    Json::Value* output) {
  if (!input.isMember("path")
      || !input["path"].isString()
      || !input.isMember("osd")
      || !input["osd"].isString()
      || !input.isMember("replication-flags")
      || !input["replication-flags"].isInt()) {
    (*output)["error"] = Json::Value("One of the following fields is missing or"
                                     " has an invalid value: path, osd, "
                                     "replication-flags");
    return;
  }
  
  const int repl_flags = input["replication-flags"].asInt();
  const string path = input["path"].asString();

  string osd_name = input["osd"].asString();
  const bool auto_select = boost::to_upper_copy(osd_name) == "AUTO";
  if (auto_select) {
    list<string> osds;
    volume_->GetSuitableOSDs(uc, path, 1, &osds);
    if (osds.size() == 0) {
      (*output)["error"] = "No suitable OSD available for new replica.";
      return;
    }
    osd_name = osds.front();
  }

  // Get the stripe size from the first replica of the file.
  // Stripe size must be the same for all replicas.
  string json_loc;
  volume_->GetXAttr(uc, path, "xtreemfs.locations", &json_loc);

  Json::Reader reader;
  Json::Value xloc;
  if (!reader.parse(json_loc, xloc, false)) {
    (*output)["error"] = "Cannot read locations list for file. Invalid JSON.";
    return;
  }
  if (xloc["replicas"].size() == 0) {
    (*output)["error"] = "Cannot add replica for a non-assigned file.";
    return;
  }
  const int stripe_size =
      xloc["replicas"][0]["striping-policy"]["size"].asInt();

  xtreemfs::pbrpc::Replica new_replica;
  new_replica.add_osd_uuids(osd_name);
  new_replica.set_replication_flags(repl_flags);
  new_replica.mutable_striping_policy()->set_width(1);
  new_replica.mutable_striping_policy()->set_type(
      xtreemfs::pbrpc::STRIPING_POLICY_RAID0);
  new_replica.mutable_striping_policy()->set_stripe_size(stripe_size);
  volume_->AddReplica(uc, path, new_replica);

  (*output)["result"] = Json::Value(Json::objectValue);
  (*output)["result"]["osd"] = osd_name;
}

void XtfsUtilServer::OpRemoveReplica(
    const xtreemfs::pbrpc::UserCredentials& uc,
    const Json::Value& input,
    Json::Value* output) {
  if (!input.isMember("path")
      || !input["path"].isString()
      || !input.isMember("osd")
      || !input["osd"].isString()) {
    (*output)["error"] = Json::Value("One of the following fields is missing or"
                                     " has an invalid value: path, osd");
    return;
  }
  const string osd_name = input["osd"].asString();
  const string path = input["path"].asString();

  volume_->RemoveReplica(uc, path, osd_name);

  (*output)["result"] = Json::Value(Json::objectValue);
}

void XtfsUtilServer::OpGetSuitableOSDs(
    const xtreemfs::pbrpc::UserCredentials& uc,
    const Json::Value& input,
    Json::Value* output) {
  if (!input.isMember("path")
      || !input["path"].isString()) {
    (*output)["error"] = Json::Value("One of the following fields is missing or"
                                     " has an invalid value: path");
    return;
  }
  const string path = input["path"].asString();

  list<string> osds;
  volume_->GetSuitableOSDs(uc, path, 10, &osds);

  (*output)["result"] = Json::Value(Json::objectValue);
  (*output)["result"]["osds"] = Json::Value(Json::arrayValue);
  for (list<string>::iterator iter = osds.begin();
       iter != osds.end(); ++iter) {
    (*output)["result"]["osds"].append(*iter);
  }
}

bool XtfsUtilServer::checkXctlFile(const std::string& path) {
  return boost::starts_with(path, prefix_);
}

XCtlFile* XtfsUtilServer::FindFile(uid_t uid,
                                   gid_t gid,
                                   const std::string& path,
                                   bool create) {
  boost::mutex::scoped_lock lock(xctl_files_mutex_);
  map<std::string, XCtlFile*>::iterator iter = xctl_files_.find(path);
  if (iter == xctl_files_.end()) {
    if (create) {
      XCtlFile* file = new XCtlFile();
      file->set_user(uid, gid);
      xctl_files_[path] = file;
      return file;
    } else {
      return NULL;
    }
  } else {
    XCtlFile* file = iter->second;
    if (file->is_owner(uid,gid)) {
      return file;
    } else {
      return NULL;
    }
  }
}

int XtfsUtilServer::create(uid_t uid,
                           gid_t gid,
                           const std::string& path) {
  XCtlFile* file = FindFile(uid, gid, path, true);
  if (!file) {
    // A file with this name exists but belongs to another user.
    return -1 * EEXIST;
  }
  if (file->in_use()) {
    return -1 * EAGAIN;
  }
  return 0;
}

int XtfsUtilServer::read(uid_t uid,
                         gid_t gid,
                         const std::string& path,
                         char* buf,
                         size_t size,
                         off_t offset) {
  // FIXME(bjko): Support partial full reads.
  XCtlFile* file = FindFile(uid, gid, path, false);
  if (!file) {
    return -1 * ENOENT;
  }
  const int length = file->last_result().size();
  if (size < length) {
    return -1 * EINVAL;
  }
  memcpy(buf, file->last_result().c_str(), length);
  return length;
}

int XtfsUtilServer::write(uid_t uid,
                          gid_t gid,
                          const xtreemfs::pbrpc::UserCredentials& uc,
                          const std::string& path,
                          const char *buf,
                          size_t size) {
  XCtlFile* file = FindFile(uid, gid, path, true);
  assert(file);
  if (file->in_use()) {
    return -1 * EAGAIN;
  }
  file->set_in_use(true);
  string input_str(buf, size);
  ParseAndExecute(uc, input_str, file);
  file->set_in_use(false);
  return size;
}

int XtfsUtilServer::getattr(uid_t uid,
                            gid_t gid,
                            const std::string& path,
                            struct stat* st_buf) {
  XCtlFile* file = FindFile(uid, gid, path, false);
  if (!file) {
    return -1 * ENOENT;
  }
  
#ifdef __linux
  st_buf->st_atim.tv_sec = 0;
  st_buf->st_atim.tv_nsec = 0;
  st_buf->st_ctim.tv_sec = 0;
  st_buf->st_ctim.tv_nsec = 0;
  st_buf->st_mtim.tv_sec = 0;
  st_buf->st_mtim.tv_nsec = 0;
#elif __APPLE__
  st_buf->st_atimespec.tv_sec = 0;
  st_buf->st_atimespec.tv_nsec = 0;
  st_buf->st_ctimespec.tv_sec = 0;
  st_buf->st_ctimespec.tv_nsec = 0;
  st_buf->st_mtimespec.tv_sec = 0;
  st_buf->st_mtimespec.tv_nsec = 0;
#endif

  st_buf->st_blksize = 1024;
  st_buf->st_blocks = 0;
  st_buf->st_dev = 0;
  st_buf->st_gid = file->get_gid();
  st_buf->st_ino = 1;
  st_buf->st_mode = S_IFREG | S_IWUSR | S_IRUSR;
  st_buf->st_nlink = 1;
  st_buf->st_rdev = 0;
  st_buf->st_uid = file->get_uid();
  st_buf->st_size = file->last_result().size();
  return 0;
}

int XtfsUtilServer::unlink(uid_t uid,
                           gid_t gid,
                           const std::string& path) {
  XCtlFile* file = FindFile(uid, gid, path, false);
  if (!file) {
    return -1 * ENOENT;
  }
  delete xctl_files_[path];
  xctl_files_.erase(path);
  return 0;
}

}  // namespace xtreemfs
