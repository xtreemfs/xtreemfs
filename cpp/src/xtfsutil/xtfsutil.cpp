/*
 * Copyright (c) 2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <boost/program_options.hpp>
#include <boost/regex.hpp>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string>
#include <string.h>
#include <sys/types.h>
#include <sys/xattr.h>
#include <unistd.h>
#include <vector>
#include <boost/algorithm/string/case_conv.hpp>

#include "json/json.h"
#include "xtreemfs/GlobalTypes.pb.h"

using namespace std;
using namespace boost::program_options;

// Execute an operation via xctl file.
bool executeOperation(const string& xctl_file,
                      const Json::Value& request,
                      Json::Value* response) {
  Json::FastWriter writer;
  const string json_out = writer.write(request);

  int fd = open(xctl_file.c_str(), O_CREAT | O_RDWR);
  if (fd == -1) {
    cerr << "Cannot open xctl file: " << strerror(errno) << endl;
    unlink(xctl_file.c_str());
    return false;
  }

  int bytes_written = pwrite(fd, json_out.c_str(), json_out.size(), 0);
  if (bytes_written <= 0) {
    cerr << "Cannot write command to xctl file: " << strerror(errno) << endl;
    close(fd);
    unlink(xctl_file.c_str());
    return false;
  }

  char result[1 << 20];
  int bytes_read = pread(fd, result, (1 << 20) - 1, 0);
  if (bytes_read <= 0) {
    cerr << "Cannot read result to xctl file: " << strerror(errno) << endl;
    close(fd);
    unlink(xctl_file.c_str());
    return false;
  }
  result[bytes_read] = 0;

  close(fd);
  unlink(xctl_file.c_str());

  Json::Reader reader;
  if (!reader.parse(result, result + bytes_read - 1, *response, false)) {
    cerr << "Read invalid JSON from xctl file: " << result << endl;
    return false;
  }
  if (!response->isObject()) {
    cerr << "Read invalid JSON from xctl file: " << result << endl;
    return false;
  }
  if (response->isMember("error")) {
    cerr << "The XtreemFS call failed: "
        << (*response)["error"].asString() << endl;
    return false;
  }
  if (!response->isMember("result")) {
    cerr << "Read invalid JSON from xctl file: " << result << endl;
    return false;
  }
  return true;
}

// Format bytes in human readable format (kB, MB, GB...)
string formatBytes(uint64_t bytes) {
  if (bytes < 1024) {
    return boost::lexical_cast<string>(bytes) + " bytes";
  } else if (bytes < (1 << 20)) {
    return boost::lexical_cast<string>(bytes/1024) + " kB";
  } else if (bytes < (1 << 30)) {
    return boost::lexical_cast<string>(bytes/(1 << 20)) + " MB";
  } else if (bytes < (1l << 40)) {
    return boost::lexical_cast<string>(bytes/(1 << 30)) + " GB";
  } else if (bytes < (1l << 50)) {
    return boost::lexical_cast<string>(bytes/(1l << 40)) + " TB";
  } else {
    return boost::lexical_cast<string>(bytes/(1l << 50)) + " EB";
  }
}

// Stat a file/directory/volume.
bool getattr(const string& xctl_file,
             const string& path) {
  Json::Value request(Json::objectValue);
  request["operation"] = "getattr";
  request["path"] = path;

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    Json::Value& stat = response["result"];
    cout << "Path (on volume)     " << path << endl;
    cout << "XtreemFS file Id     " << stat["fileId"].asString() << endl;
    cout << "XtreemFS URL         " << stat["url"].asString() << endl;
    cout << "Owner                " << stat["owner"].asString() << endl;
    cout << "Group                " << stat["group"].asString() << endl;
    if (stat.isMember("acl") && !stat["acl"].asString().empty()) {
      cout << "ACL                  " << stat["acl"].asString() << endl;
    }
    cout << "Type                 ";
    int type = boost::lexical_cast<int>(stat["object_type"].asString());
    switch (type) {
      case 1 : {
        cout << "file" << endl;

        bool is_replicated = false;
        cout << "Replication policy   ";
        if (!stat["locations"]["update-policy"].asString().empty()) {
          is_replicated = true;
          cout << stat["locations"]["update-policy"].asString() << endl;
        } else {
          cout << "none (not replicated)" << endl;
        }
        cout << "XLoc version         "
            << stat["locations"]["version"].asInt() << endl;
        cout << "Replicas:" << endl;
        for (int i = 0; i < stat["locations"]["replicas"].size(); ++i) {
          Json::Value& replica = stat["locations"]["replicas"][i];
          cout << "  Replica " << (i+1) << endl;
          cout << "     Striping policy     "
              << replica["striping-policy"]["pattern"].asString()
              << " / " << replica["striping-policy"]["width"].asInt()
              << " / " <<replica["striping-policy"]["size"].asInt()
              << "kB" << endl;
          if (is_replicated) {
            cout << "     Replication Flags   ";
            int repl_flags = replica["replication-flags"].asInt();
            if (repl_flags & xtreemfs::pbrpc::REPL_FLAG_FULL_REPLICA) {
              cout << "full";
            } else {
              cout << "partial";
            }
            if (repl_flags & xtreemfs::pbrpc::REPL_FLAG_IS_COMPLETE) {
              cout << " + complete";
            }
            cout << endl;
          }
          for (int j = 0; j < replica["osds"].size(); ++j) {
            cout << "     OSD " << (j+1) << "               "
                << replica["osds"][j]["uuid"].asString()
                << "/" << replica["osds"][j]["address"].asString() << endl;
          }
        }
        break;
      }
      case 2 : {
        if (path == "/") {
          cout << "volume" << endl;
          cout << "Free/Used Space      "
            << formatBytes(boost::lexical_cast<uint64_t>(
                stat["free_space"].asString()))
            << " / " << formatBytes(boost::lexical_cast<uint64_t>(
                stat["used_space"].asString())) << endl;
          cout << "Num. Files/Dirs      "
            << stat["num_files"].asString()
            << " / " << stat["num_dirs"].asString() << endl;
          
          cout << "Access Control p.    ";
          if (stat["ac_policy_id"].asString() == "1") {
            cout << "Null Policy (no access control)";
          } else if (stat["ac_policy_id"].asString() == "2") {
            cout << "POSIX (permissions & ACLs)";
          } else if (stat["ac_policy_id"].asString() == "3") {
            cout << "Volume (permissions & ACLs)";
          } else {
            cout << "custom (" << stat["ac_policy_id"].asString() << ")";
          }
          cout << endl;

          cout << "OSD Selection p.     "
            << stat["osel_policy"].asString() << endl;
          cout << "Replica Selection p. ";
          if (stat.isMember("rsel_policy") &&
              !stat["rsel_policy"].asString().empty()) {
            cout << stat["rsel_policy"].asString() << endl;
          } else {
            cout << "default" << endl;
          }
        } else {
          cout << "directory" << endl;
        }

        cout << "Default Striping p.  ";
        if (stat.isMember("default_sp")) {
          cout << stat["default_sp"]["pattern"].asString()
              << " / " << stat["default_sp"]["width"].asInt()
              << " / " <<stat["default_sp"]["size"].asInt()
              << "kB" << endl;
        } else {
          cout << "not set" << endl;
        }

        cout << "Default Repl. p.     ";
        if (stat.isMember("default_rp")) {
          cout << stat["default_rp"]["update-policy"].asString()
              << " with " << stat["default_rp"]["replication-factor"].asInt()
              << " replicas";
          int flags = stat["default_rp"]["replication-flags"].asInt();
          if (stat["default_rp"]["update-policy"].asString() == "ronly"
              && flags > 0) {
            if (flags & xtreemfs::pbrpc::REPL_FLAG_FULL_REPLICA) {
              cout << ", full replicas";
            } else {
              cout << ", partial replicas";
            }
          }
          cout << endl;

        } else {
          cout << "not set" << endl;
        }

        break;
      }
      case 3 : {
        cout << "softlink" << endl;
        cout << "Target               "
            << stat["link_target"].asString() << endl;
        break;
      }
    }
    return true;
  } else {
    return false;
  }
}

// Sets the default striping policy.
bool SetDefaultSP(const string& xctl_file,
                  const string& path,
                  const variables_map& vm) {
  if (vm.count("striping-policy-width") == 0) {
    cerr << "striping-policy-width must be set" << endl;
    return false;
  }

  if (vm.count("striping-policy-stripe-size") == 0) {
    cerr << "striping-policy-stripe-size must be set" << endl;
    return false;
  }

  const string policy = vm["striping-policy"].as<string>();
  const int width = vm["striping-policy-width"].as<int>();
  const int size = vm["striping-policy-stripe-size"].as<int>();

  Json::Value request(Json::objectValue);
  request["operation"] = "setDefaultSP";
  request["path"] = path;
  if (boost::to_upper_copy(policy) == "RAID0") {
    request["pattern"] = "STRIPING_POLICY_RAID0";
  } else {
    cerr << "Striping policy must be RAID0." << endl;
    return false;
  }
  request["width"] = width;
  request["size"] = size;
  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Updated default striping policy to: "
        << request["pattern"].asString() << " / "
        << width << " / " << size << "kB" << endl;
    return true;
  } else {
    cerr << "FAILED" << endl;
    return false;
  }
}

// Sets the default replication policy.
bool SetDefaultRP(const string& xctl_file,
                  const string& path,
                  const variables_map& vm) {
  if (vm.count("replication-policy") == 0) {
    cerr << "replication-policy must be set" << endl;
    return false;
  }

  if (vm.count("replication-factor") == 0) {
    cerr << "replication-factor must be set" << endl;
    return false;
  }

  const string policy =
      boost::to_upper_copy(vm["replication-policy"].as<string>());
  const int factor = vm["replication-factor"].as<int>();
  const bool is_full = vm.count("full") > 0;

  Json::Value request(Json::objectValue);
  request["operation"] = "setDefaultRP";
  request["path"] = path;
  if (policy == "RONLY") {
    request["update-policy"] = "ronly";
  } else if (policy == "WQRQ") {
    request["update-policy"] = "WqRq";
  } else if (policy == "WARA") {
    request["update-policy"] = "WaRa";
  } else if (policy == "NONE") {
    request["update-policy"] = "";
  } else {
    cerr << "Unknown replication policy: " << policy << endl;
    return false;
  }
  request["replication-factor"] = factor;
  request["replication-flags"] = 0;
  if (policy == "RONLY" && is_full) {
    request["replication-flags"] = xtreemfs::pbrpc::REPL_FLAG_FULL_REPLICA
        | xtreemfs::pbrpc::REPL_FLAG_STRATEGY_RAREST_FIRST;
  } else if (policy == "RONLY" && !is_full) {
    request["replication-flags"] =
        xtreemfs::pbrpc::REPL_FLAG_STRATEGY_SEQUENTIAL_PREFETCHING;
  }
  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Updated default replication policy to: "
        <<  policy << " with "
        << factor << " replicas" << endl;
    return true;
  } else {
    cerr << "FAILED" << endl;
    return false;
  }
}

// Sets the replication policy for a file.
bool SetReplicationPolicy(const string& xctl_file,
                          const string& path,
                          const variables_map& vm) {


  const string policy =
      boost::to_upper_copy(vm["set-replication-policy"].as<string>());

  // Check file.
  Json::Value getattr_request(Json::objectValue);
  getattr_request["operation"] = "getattr";
  getattr_request["path"] = path;

  Json::Value getattr_response;
  if (!executeOperation(xctl_file, getattr_request, &getattr_response)) {
    cerr << "Cannot retrieve file information!" << endl;
    return false;
  }
  const bool has_replicas =
      getattr_response["result"]["locations"]["replicas"].size() > 1;
  string current_policy =
      getattr_response["result"]["locations"]["update-policy"].asString();
  boost::to_upper(current_policy);

  Json::Value request(Json::objectValue);
  request["operation"] = "setReplicationPolicy";
  request["path"] = path;
  if (policy == "RONLY") {
    request["policy"] = "ronly";
  } else if (policy == "WQRQ") {
    request["policy"] = "WqRq";
  } else if (policy == "WARA") {
    request["policy"] = "WaRa";
  } else if (policy == "NONE") {
    request["policy"] = "";
  } else {
    cerr << "Unknown replication policy: " << policy << endl;
    return false;
  }

  if (request["policy"].asString() == current_policy) {
    cout << "File is already in mode: ";
    if (current_policy.empty()) {
      cout <<"NONE (not replicated)";
    } else {
      cout << current_policy;
    }
    cout << endl;
    return true;
  }

  if (!current_policy.empty() && has_replicas) {
    cerr << "Cannot change policy for a file which is already replicated."
        << endl;
    cerr << "Please remove all replicas except for one before changing "
        << "the replication policy." << endl;
    return false;
  }
  
  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Changed replication policy to: " << policy << endl;
    return true;
  } else {
    cerr << "FAILED" << endl;
    return false;
  }
}

// Adds a replica and selects an OSD.
bool AddReplica(const string& xctl_file,
                const string& path,
                const variables_map& vm) {
  string osd_uuid = vm["add-replica"].as<string>();
  if (boost::to_upper_copy(osd_uuid) == "AUTO") {
    osd_uuid = "AUTO";
  }
  Json::Value request(Json::objectValue);
  request["operation"] = "addReplica";
  request["path"] = path;
  request["osd"] = osd_uuid;
  request["replication-flags"] = 0;
  if (vm.count("full") > 0) {
    request["replication-flags"] = xtreemfs::pbrpc::REPL_FLAG_FULL_REPLICA;
  }

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Added new replica on OSD: "
        << response["result"]["osd"].asString() << endl;
    return true;
  } else {
    cerr << "FAILED" << endl;
    return false;
  }
}

// Deletes a replica.
bool DeleteReplica(const string& xctl_file,
                   const string& path,
                   const variables_map& vm) {
  string osd_uuid = vm["delete-replica"].as<string>();
  Json::Value request(Json::objectValue);
  request["operation"] = "removeReplica";
  request["path"] = path;
  request["osd"] = osd_uuid;

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Deleted replica on OSD: "
        << osd_uuid << endl;
    return true;
  } else {
    cerr << "FAILED" << endl;
    return false;
  }
}

// Shows error messages from the client.
bool ShowErrors(const string& xctl_file,
                const string& path,
                const variables_map& vm) {
  Json::Value request(Json::objectValue);
  request["operation"] = "getErrors";

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    for (int i = 0; i < response["result"].size(); ++i) {
      cout << "> " << response["result"][i].asString() << endl;
    }
    cout << endl;
    return true;
  } else {
    cerr << "FAILED" << endl;
    return false;
  }
}

// Returns a list of OSDs suitable for a new replica.
bool GetSuitableOSDs(const string& xctl_file,
                     const string& path,
                     const variables_map& vm) {
  Json::Value request(Json::objectValue);
  request["operation"] = "getSuitableOSDs";
  request["path"] = path;

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "OSDs suitable for new replicas: " << endl;
    for (int i = 0; i < response["result"]["osds"].size(); ++i) {
      cout << "  " << response["result"]["osds"][i].asString() << endl;
    }
    return true;
  } else {
    cerr << "FAILED" << endl;
    return false;
  }
}

// Sets the OSD selection policy for a volume.
bool SetOSP(const string& xctl_file,
                const string& path,
                const variables_map& vm) {
  string policy = vm["set-osp"].as<string>();
  string policy_uc = boost::to_upper_copy(policy);
  Json::Value request(Json::objectValue);
  request["operation"] = "setOSP";
  request["path"] = path;
  if (policy_uc == "DEFAULT") {
    request["policy"] = xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT
        | xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_RANDOM;
  } else if (policy_uc == "FQDN") {
    request["policy"] = xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT
        | xtreemfs::pbrpc::OSD_SELECTION_POLICY_GROUP_FQDN;
  } else if (policy_uc == "UUID") {
    request["policy"] = xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT
        | xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_UUID;
  } else if (policy_uc == "DCMAP") {
    request["policy"] = xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT
        | xtreemfs::pbrpc::OSD_SELECTION_POLICY_GROUP_DCMAP;
  } else if (policy_uc == "VIVALDI") {
    request["policy"] = xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT
        | xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_VIVALDI;
  } else {
    request["policy"] = policy;
  }

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Updates OSD selection policy to: "
        << policy << endl;
    return true;
  } else {
    cerr << "FAILED" << endl;
    return false;
  }
}

// Sets the Replica selection policy for a volume.
bool SetRSP(const string& xctl_file,
                const string& path,
                const variables_map& vm) {
  string policy = vm["set-rsp"].as<string>();
  string policy_uc = boost::to_upper_copy(policy);
  Json::Value request(Json::objectValue);
  request["operation"] = "setRSP";
  request["path"] = path;
  if (policy_uc == "DEFAULT") {
    request["policy"] = xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_RANDOM;
  } else if (policy_uc == "FQDN") {
    request["policy"] = xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_FQDN;
  } else if (policy_uc == "DCMAP") {
    request["policy"] = xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_DCMAP;
  } else if (policy_uc == "VIVALDI") {
    request["policy"] = xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_VIVALDI;
  } else {
    request["policy"] = policy;
  }

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Updates Replica selection policy to: "
        << policy << endl;
    return true;
  } else {
    cerr << "FAILED" << endl;
    return false;
  }
}

// Sets a policy attribute.
bool SetPolicyAttr(const string& xctl_file,
                   const string& path,
                   const variables_map& vm) {
  string attr = vm["set-pattr"].as<string>();
  if (vm.count("value") == 0) {
    cerr << "--value must be specified" << endl;
    return false;
  }
  Json::Value request(Json::objectValue);
  request["operation"] = "setPolicyAttr";
  request["path"] = path;
  request["attribute"] = attr;
  request["value"] = vm["value"].as<string>();

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Set policy attribute " << attr << " to "
        << vm["value"].as<string>() << endl;
    return true;
  } else {
    cerr << "FAILED" << endl;
    return false;
  }
}

// Lists all policy attributes and their values.
bool ListPolicyAttrs(const string& xctl_file,
                     const string& path,
                     const variables_map& vm) {
  Json::Value request(Json::objectValue);
  request["operation"] = "listPolicyAttrs";
  request["path"] = path;

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    for (int i = 0; i < response["result"].getMemberNames().size(); ++i) {
      const string& key = response["result"].getMemberNames()[i];
      cout << key << " = " << response["result"][key] << endl;
    }
    return true;
  } else {
    cerr << "FAILED" << endl;
    return false;
  }
}

// Sets/Modifies/Removes the ACL.
bool SetRemoveACL(const string& full_path,
                  const variables_map& vm) {
  string contents;
  if (vm.count("set-acl") > 0) {
    contents = "m " + vm["set-acl"].as<string>();
  } else if (vm.count("del-acl") > 0) {
    contents = "x " + vm["del-acl"].as<string>();
  } else {
    return false;
  }
#ifdef __linux
  int result = setxattr(full_path.c_str(),
                        "xtreemfs.acl",
                        contents.c_str(),
                        contents.size(),
                        0);
#elif __APPLE__
  int result = setxattr(full_path.c_str(),
                        "xtreemfs.acl",
                        contents.c_str(),
                        contents.size(),
                        0,
                        0);
#endif
  if (result != 0) {
    cerr << "Cannot add/modify/delete ACL entry: " << strerror(errno) << endl;
    return false;
  }
  cout << "Success." << endl;
  return true;
}

int main(int argc, char **argv) {
  options_description desc("Allowed options");
  desc.add_options()
      ("help,h", "produce help message")
      ("path", value<string>(), "path on mounted XtreemFS volume")
      ("errors", "show client errors for a volume")
      ("set-dsp", "set (change) the default striping policy (volume)")
      ("striping-policy,p",
       value<string>()->default_value("RAID0"),
       "striping policy (always RAID0)")
      ("striping-policy-width,w", value<int>(),
       "striping width (number of OSDs)")
      ("striping-policy-stripe-size,s", value<int>(),
       "stripe size in kB (object size)")
      ("set-drp", "set (change) the replication striping policy (volume)")
      ("set-replication-policy,r", value<string>(),
       "set (change) the replication policy for a file")
      ("add-replica,a", value<string>()->implicit_value("AUTO"),
       "adds a new replica on the osd with the given UUID or AUTO "
       "for automatics OSD selection")
      ("delete-replica,d", value<string>(),
       "deletes the replica on the OSD with the given UUID")
      ("list-osds,l", "list suitable OSDs for a file")
      ("replication-policy", value<string>(),
       "RONLY, WQRQ, WARA or NONE to disable replication")
      ("replication-factor", value<int>(),
       "number of replicas to create for a file")
      ("full", "full replica (readonly replication only)")
      ("set-osp", value<string>(),
       "set (change) the OSD Selection Policy (volume)")
      ("set-rsp", value<string>(),
       "set (change) the Replica Selection Policy (volume)")
      ("set-pattr", value<string>(),
       "set (change) a policy attribute (volume)")
      ("value", value<string>(),
       "value for the policy attribute (volume)")
      ("list-pattrs",
       "list all policy attributes (volume)")
      ("set-acl", value<string>(),
       "adds/modifies an ACL entry, format: u|g|m|o:[<name>]:[<rwx>|<octal>]")
      ("del-acl", value<string>(),
       "removes an ACL entry, format: u|g|m|o:<name>");
  positional_options_description pd;
  pd.add("path", 1);
  
  variables_map vm;
  store(command_line_parser(argc, argv).options(desc).positional(pd).run(), vm);
  notify(vm);

  if (vm.count("help") || !vm.count("path")) {
    cerr << "usage: xtfsutil <path>" << endl;
    cerr << desc << endl;
    return 1;
  }

  char* real_path_cstr = realpath(vm["path"].as<string>().c_str(), NULL);
  
  // get xtreemfs.url xattr.
  char xtfs_url[2048];
#ifdef __linux
  int length = getxattr(real_path_cstr, "xtreemfs.url", xtfs_url, 2048);
#elif __APPLE__
  int length = getxattr(real_path_cstr, "xtreemfs.url", xtfs_url, 2048, 0, 0);
#endif
  if (length <= 0) {
    cerr << "Path doesn't point to an entity on an XtreemFS volume!" << endl;
    cerr << "xattr xtreemfs.url is missing." << endl << endl;
    return 1;
  }

  string url(xtfs_url, length);
  const boost::regex pure_path_re("pbrpc.?://[^/]+/[^/]+(.*)");
  boost::smatch matcher;
  if (!boost::regex_match(url, matcher, pure_path_re)) {
    cerr << "Invalid XtreemFS url!" << endl;
    return 1;
  }
  string path_on_volume = matcher[1];

  string mount_point = real_path_cstr;
  mount_point = mount_point.substr(0,
                                   mount_point.size() - path_on_volume.size());
  if (mount_point[mount_point.size() - 1] != '/') {
    mount_point.append("/");
  }

  if (path_on_volume.empty()) {
    path_on_volume = "/";
  }

  const string xctl_file = mount_point
                           + ".xctl$$$"
                           + boost::lexical_cast<string>(getuid())
                           + "-"
                           + boost::lexical_cast<string>(getpid());

  if (vm.count("set-dsp") > 0) {
    return SetDefaultSP(xctl_file, path_on_volume, vm) ? 0 : 1;
  } else if (vm.count("set-drp") > 0) {
    return SetDefaultRP(xctl_file, path_on_volume, vm) ? 0 : 1;
  } else if (vm.count("set-osp") > 0) {
    return SetOSP(xctl_file, path_on_volume, vm) ? 0 : 1;
  } else if (vm.count("set-rsp") > 0) {
    return SetRSP(xctl_file, path_on_volume, vm) ? 0 : 1;
  } else if (vm.count("set-pattr") > 0) {
    return SetPolicyAttr(xctl_file, path_on_volume, vm) ? 0 : 1;
  } else if (vm.count("list-pattrs") > 0) {
    return ListPolicyAttrs(xctl_file, path_on_volume, vm) ? 0 : 1;
  } else if (vm.count("set-replication-policy") > 0) {
    return SetReplicationPolicy(xctl_file, path_on_volume, vm) ? 0 : 1;
  } else if (vm.count("add-replica") > 0) {
    return AddReplica(xctl_file, path_on_volume, vm) ? 0 : 1;
  } else if (vm.count("delete-replica") > 0) {
    return DeleteReplica(xctl_file, path_on_volume, vm) ? 0 : 1;
  } else if (vm.count("list-osds") > 0) {
    return GetSuitableOSDs(xctl_file, path_on_volume, vm) ? 0 : 1;
  } else if (vm.count("set-acl") > 0
             || vm.count("del-acl") > 0) {
    return SetRemoveACL(string(real_path_cstr), vm) ? 0 : 1;
  } else if (vm.count("errors") > 0) {
    return ShowErrors(xctl_file, path_on_volume, vm) ? 0 : 1;
  } else {
    return getattr(xctl_file, path_on_volume) ? 0 : 1;
  }
}