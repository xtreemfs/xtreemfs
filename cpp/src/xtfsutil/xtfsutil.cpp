/*
 * Copyright (c) 2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *               2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#if defined __linux || defined __APPLE__
#include <sys/xattr.h>
#endif  // __linux || __APPLE__
#include <unistd.h>

#include <boost/algorithm/string/case_conv.hpp>
#include <boost/program_options.hpp>
#include <boost/regex.hpp>
#include <fstream>
#include <string>
#include <vector>
#include <iomanip>

#include "json/json.h"
#include "libxtreemfs/version_management.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "xtreemfs/GlobalTypes.pb.h"

using namespace std;
using namespace boost::program_options;
namespace style = boost::program_options::command_line_style;

std::string kVersionString = XTREEMFS_VERSION_STRING;

// Execute an operation via xctl file.
bool executeOperation(const string& xctl_file,
                      const Json::Value& request,
                      Json::Value* response) {
  Json::FastWriter writer;
  const string json_out = writer.write(request);

  int fd = open(xctl_file.c_str(), O_CREAT | O_RDWR, S_IRUSR | S_IWUSR);
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

  // Let Fuse refresh its cached file size - otherwise the read is incomplete.
  // See Issue 234: http://code.google.com/p/xtreemfs/issues/detail?id=234
  struct stat stat_temp;
  stat(xctl_file.c_str(), &stat_temp);

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
    if (string(result) == json_out) {
      cerr << "xtfsutil read back the same text which was written into the"
              " pseudo control file: " << result << endl
           << "This means a file content cache prevents xtfsutil from working"
              " correctly." << endl;
#ifdef __sun
      cerr << "This is a known issue on Solaris." << endl;
#endif  // __sun
#ifdef __FreeBSD__
      cerr << "This is a known issue of FUSE for FreeBSD 0.4.4." << endl;
#endif  // __FreeBSD__
    } else {
      cerr << "Read invalid JSON from xctl file: " << result << endl;
    }
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
  } else if (bytes < (1LL << 40)) {
    return boost::lexical_cast<string>(bytes/(1 << 30)) + " GB";
  } else if (bytes < (1LL << 50)) {
    return boost::lexical_cast<string>((float)bytes/(1LL << 40)) + " TB";
  } else {
    return boost::lexical_cast<string>((float)bytes/(1LL << 50)) + " PB";
  }
}

// checks whether the quota is unlimited and returns unlimted
// or a formatted string
string parseUnlimitedQuota(uint64_t quota){
  if (quota == 0) { // unlimited
      return "unlimited";
    } else {
      return formatBytes(quota);
    }
}

// checks wehter the quota is unlimited and no space is blocked and
// returns a dash as indicator, that there can not be blocked space
// otherwise it returns a formatted string
string parseBlockedSpace(uint64_t quota, uint64_t blockedSpace) {
  if (quota == 0 && blockedSpace == 0) {
    return "-";
  } else {
    return formatBytes(blockedSpace);
  }
}

void OutputACLs(Json::Value* stat) {
  if ((*stat).isMember("acl")) {
    if ((*stat)["acl"].isObject() && !(*stat)["acl"].empty()) {
      bool no_output_yet = true;
      const size_t kPaddingCount = 21;
      const string kACLOutputPrefix = "ACLs";
      Json::Value::Members acl_entry_names;

      const size_t kACLClassesCount = 4;
      string acl_classes[kACLClassesCount] = {"u:", "g:", "m:", "o:"};

      for (size_t i = 0; i < kACLClassesCount; i++) {
        const string& prefix = acl_classes[i];

        // Output default entry first.
        if ((*stat)["acl"].isMember(prefix)) {
          cout << (no_output_yet ? kACLOutputPrefix : string(kACLOutputPrefix.length(), ' '))  // NOLINT
               << string(kPaddingCount - kACLOutputPrefix.length(), ' ')
               << prefix << ":" << (*stat)["acl"][prefix].asString() << endl;
          no_output_yet = false;

          (*stat)["acl"].removeMember(prefix);
        }

        // Output remaining entries of the same class.
        acl_entry_names = (*stat)["acl"].getMemberNames();
        for (Json::Value::Members::const_iterator iter
                 = acl_entry_names.begin();
             iter != acl_entry_names.end();
             ++iter) {
          if ((*iter).substr(0, prefix.length()).compare(prefix) == 0) {
            cout << (no_output_yet ? kACLOutputPrefix : string(kACLOutputPrefix.length(), ' '))  // NOLINT
                 << string(kPaddingCount - kACLOutputPrefix.length(), ' ')
                 << *iter << ":" << (*stat)["acl"][*iter].asString() << endl;
            no_output_yet = false;
          }
        }
      }
    } else if ((*stat)["acl"].isString()
               && !(*stat)["acl"].asString().empty()) {
      cout << "ACL                  " << (*stat)["acl"].asString() << endl;
    }  // else: do not output empty ACLs.
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
    OutputACLs(&stat);
    cout << "Type                 ";
    int type = boost::lexical_cast<int>(stat["object_type"].asString());
    switch (type) {
      case 1 : {
        cout << "file" << endl;

        bool is_ronly =
            (stat["locations"]["update-policy"].asString() == "ronly");
        cout << "Replication policy   ";
        if (!stat["locations"]["update-policy"].asString().empty()) {
          cout << stat["locations"]["update-policy"].asString() << endl;
        } else {
          cout << "none (not replicated)" << endl;
        }
        cout << "XLoc version         "
            << stat["locations"]["version"].asInt() << endl;
        cout << "Replicas:" << endl;
        for (Json::ArrayIndex i = 0;
             i < stat["locations"]["replicas"].size();
             i++) {
          Json::Value& replica = stat["locations"]["replicas"][i];
          cout << "  Replica " << (i+1) << endl;
          cout << "     Striping policy     "
              << replica["striping-policy"]["pattern"].asString()
              << " / " << replica["striping-policy"]["width"].asInt()
              << " / " <<replica["striping-policy"]["size"].asInt()
              << "kB" << endl;
          if (is_ronly) {
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
          for (Json::ArrayIndex j = 0; j < replica["osds"].size(); j++) {
            cout << "     OSD " << (j+1) << "               "
                << replica["osds"][j]["uuid"].asString()
                << " (" << replica["osds"][j]["address"].asString() << ")"
                << endl;
          }
        }
        break;
      }
      case 2 : {
        if (path == "/") {
          cout << "volume" << endl;

          if(!stat["usable_space"].isNull()) {
            cout
            << "Available Space      "
            << formatBytes(
                    boost::lexical_cast<uint64_t>(
                            stat["usable_space"].asString()))
            << endl;
          }

          if(!stat["quota"].isNull() && !stat["usedspace"].isNull()) {
            cout
            << "Quota / Used Space   "
            << parseUnlimitedQuota(
                    boost::lexical_cast<uint64_t>(stat["quota"].asString()))
            << " / "
            << formatBytes(
                    boost::lexical_cast<uint64_t>(stat["usedspace"].asString()))
            << endl;
          }

          if(!stat["vouchersize"].isNull()) {
            cout
            << "Voucher Size         "
            << formatBytes(
                    boost::lexical_cast<uint64_t>(stat["vouchersize"].asString()))
            << endl;
          }

          if(!stat["defaultuserquota"].isNull()) {
            cout
            << "Default User Quota   "
            << parseUnlimitedQuota(
                    boost::lexical_cast<uint64_t>(
                            stat["defaultuserquota"].asString()))
            << endl;
          }

          if(!stat["defaultgroupquota"].isNull()) {
            cout
            << "Default GroupQuota   "
            << parseUnlimitedQuota(
                    boost::lexical_cast<uint64_t>(
                            stat["defaultgroupquota"].asString()))
            << endl;
          }

          if(!stat["num_files"].isNull() && !stat["num_dirs"].isNull()) {
            cout << "Num. Files/Dirs      "
            << stat["num_files"].asString()
            << " / " << stat["num_dirs"].asString() << endl;
          }

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
        if (stat.isMember("default_rp")
            && !stat["default_rp"]["update-policy"].asString().empty()) {
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

        cout << "Snapshots enabled    ";
        if (stat.isMember("snapshots_enabled")) {
          cout << (stat["snapshots_enabled"].asString() == "true" ?
                       "yes" : "no") << endl;
        } else {
          cout << "unknown" << endl;
        }

        cout << "Tracing enabled      ";
        if (stat.isMember("tracing_enabled")) {
          if(stat["tracing_enabled"].asString() == "true" &&
            stat.isMember("tracing_policy_config") && stat.isMember("tracing_policy")) {
            cout << "yes" << endl;
            cout << "Tracing policy config         "
              << stat["tracing_policy_config"].asString() << endl;
            cout << "Tracing policy       "
              << stat["tracing_policy"].asString() << endl;
          } else {
            cout << "no" << endl;
          }
        } else {
          cout << "unknown" << endl;
        }

        if (path == "/") {
          cout << "Selectable OSDs      ";
          if (stat.isMember("usable_osds") && stat["usable_osds"].size() > 0) {
            Json::Value& usable_osds = stat["usable_osds"];
            for(Json::ValueIterator it = usable_osds.begin();
                it != usable_osds.end();
                ++it) {
              if (it != usable_osds.begin()) {
                cout << endl << "                     ";
              }
              cout << it.key().asString() << " (" << (*it).asString() << ")";
            }
            cout << endl;

          } else {
            cout << "none available" << endl;
          }
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
  string policy;
  if (vm.count("striping-policy") == 0) {
    // If -p was left out, use the default policy "RAID0".
    policy = "RAID0";
  } else {
    policy = vm["striping-policy"].as<string>();
  }

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
    cerr << "Setting Default Striping Policy FAILED" << endl;
    return false;
  }
}

// Sets the quota for a volume, user or group
bool SetQuota(const string& xctl_file,
                  const string& path,
                  const variables_map& vm) {

  const string value = vm["set-quota"].as<string>();

  Json::Value request(Json::objectValue);
  request["operation"] = "setQuota";
  request["path"] = path;

  request["value"] = value;
  request["type"] = "";
  request["specifiedName"] = "";

  if (vm.count("user") > 0) {
    request["type"] = "user";
    request["specifiedName"] = vm["user"].as<string>();
  } else if (vm.count("group") > 0) {
    request["type"] = "group";
    request["specifiedName"] = vm["group"].as<string>();
  } else if(vm.count("volume") > 0) {
    request["type"] = "volume";
  } else{
    cerr << "--set-quota needs a sub option: volume, user or group!" << endl;
    return false;
  }

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Set quota to " << value << " for the " << request["type"].asString() << "."
         << endl;
    return true;
  } else {
    cerr << "Setting quota value has FAILED" << endl;
    return false;
  }
}

// Sets the voucher size or the default quota for users or groups
bool SetQuotaRelatedValue(const string& xctl_file, const string& path,
                          const variables_map& vm) {

  Json::Value request(Json::objectValue);
  request["operation"] = "setQuotaRelatedValue";
  request["path"] = path;

  request["type"] = "";
  request["value"] = "";

  if (vm.count("set-voucher-size") > 0) {
    request["type"] = "vouchersize";
    request["value"] = Json::Value(vm["set-voucher-size"].as<string>());
  } else if (vm.count("set-default-user-quota") > 0) {
    request["type"] = "defaultuserquota";
    request["value"] = Json::Value(vm["set-default-user-quota"].as<string>());
  } else if (vm.count("set-default-group-quota") > 0) {
    request["type"] = "defaultgroupquota";
    request["value"] = Json::Value(vm["set-default-group-quota"].as<string>());
  } else {
    cerr << "Unsupported type on set quota related value!" << endl;
    return false;
  }

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Set value to " << request["value"].asString() << " for the "
         << request["type"].asString() << "." << endl;
    return true;
  } else {
    cerr << "Setting " << request["type"].asString() << " has FAILED" << endl;
    return false;
  }
}

// gets the quota value(s) for the volume, user(s) and/or group(s)
bool GetQuota(const string& xctl_file,
              const string& path,
              const variables_map& vm) {

  Json::Value request(Json::objectValue);
  request["operation"] = "getQuota";
  request["path"] = path;

  request["type"] = "all";
  request["specifiedName"] = "";

  if (vm.count("user") > 0) {
    request["type"] = "user";
    request["specifiedName"] = vm["user"].as<string>();
  } else if (vm.count("all-users") > 0) {
    request["type"] = "user";
  } else if (vm.count("group") > 0) {
    request["type"] = "group";
    request["specifiedName"] = vm["group"].as<string>();
  } else if (vm.count("all-groups") > 0) {
    request["type"] = "group";
  } else if( vm.count("volume") > 0){
    request["type"] = "volume";
  }

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    const Json::Value& stat = response["result"];
    const int setwValue1st = 35;
    const int setwValue = 15;

    bool showVolume = false;
    bool showUser = false;
    bool showGroup = false;

    if (request["type"].asString() == "volume") {
      showVolume = true;
    } else if (request["type"].asString() == "user") {
      showUser = true;
    } else if (request["type"].asString() == "group") {
      showGroup = true;
    } else if (request["type"].asString() == "all") {
      showVolume = true;
      showUser = true;
      showGroup = true;
    }

    // Output
    cout << setw(setwValue1st) << ""
         << setw(setwValue) << "Quota"
         << setw(setwValue) << "Used Space"
         << setw(setwValue) << "Blocked Space"<< endl;

    if(showVolume){
      const string name = "Volume";

      cout << setw(setwValue1st) << name
          << setw(setwValue)
          << parseUnlimitedQuota(
              boost::lexical_cast<uint64_t>(stat["volume"]["quota"].asString()))
          << setw(setwValue)
          << formatBytes(
              boost::lexical_cast<uint64_t>(stat["volume"]["used"].asString()))
          << setw(setwValue)
          << parseBlockedSpace(
              boost::lexical_cast<uint64_t>(stat["volume"]["quota"].asString()),
              boost::lexical_cast<uint64_t>(
                  stat["volume"]["blocked"].asString()))
          << endl;
    }

    if (showUser) {
      if (stat["user"].asString().empty()) {
        if (!request["specifiedName"].asString().empty()) {
          cout << "There is no quota related entry for user: "
               << request["specifiedName"].asString() << endl;
        } else {
          cout << "There is no quota related entry for any user!" << endl;
        }
      } else {
        // parse user entries
        Json::Reader reader;
        Json::Value entries;
        if (reader.parse(stat["user"].asString(), entries, false)) {
          cout << "User Quota";
          if (entries.size() > 1) {
            cout << " Entries";
          }
          cout << ":" << endl;

          for (Json::ValueIterator it = entries.begin(); it != entries.end();
              ++it) {
            cout << setw(setwValue1st) << it.key().asString() << setw(setwValue)
                 << parseUnlimitedQuota((*it)["quota"].asUInt64())
                 << setw(setwValue) << formatBytes((*it)["used"].asUInt64())
                << setw(setwValue)
                << parseBlockedSpace((*it)["quota"].asUInt64(),
                                     (*it)["blocked"].asUInt64())
                << endl;
          }  // for
        }
      }
    }

    if (showGroup) {
      if (stat["group"].asString().empty()) {
        if (!request["specifiedName"].asString().empty()) {
          cout << "There is no quota related entry for group: "
               << request["specifiedName"].asString() << endl;
        } else {
          cout << "There is no quota related entry for any group!" << endl;
        }
      } else {
        // parse group entries
        Json::Reader reader;
        Json::Value entries;
        if (reader.parse(stat["group"].asString(), entries, false)) {
          cout << "Group Quota";
          if (entries.size() > 1) {
            cout << " Entries";
          }
          cout << ":" << endl;

          for (Json::ValueIterator it = entries.begin(); it != entries.end();
              ++it) {
            cout << setw(setwValue1st) << it.key().asString() << setw(setwValue)
                 << parseUnlimitedQuota((*it)["quota"].asUInt64())
                 << setw(setwValue) << formatBytes((*it)["used"].asUInt64())
                 << setw(setwValue)
                 << parseBlockedSpace((*it)["quota"].asUInt64(),
                                      (*it)["blocked"].asUInt64())
                 << endl;
          }  // for
        }
      }
    }

    return true;
  } else {
    cerr << "Getting quota value(s) has FAILED!" << endl;
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

  const string policy =
      boost::to_upper_copy(vm["replication-policy"].as<string>());

  int factor;
  if (vm.count("replication-factor") == 0) {
    if (policy == "NONE") {
      factor = 1;
    } else {
      cerr << "replication-factor must be set" << endl;
      return false;
    }
  } else {
    factor = vm["replication-factor"].as<int>();
  }

  if (factor <= 1 && policy != "NONE") {
    cerr << "The minimal replication-factor must be 2 (was set to: "
         << factor << ")." << endl;
    return false;
  }

  const bool is_full = vm.count("full") > 0;

  Json::Value request(Json::objectValue);
  request["operation"] = "setDefaultRP";
  request["path"] = path;
  if (policy == "RONLY" || policy == "READONLY") {
    request["update-policy"] = "ronly";
  } else if (policy == "WQRQ" || policy == "QUORUM") {
    request["update-policy"] = "WqRq";
  } else if (policy == "WAR1" || policy == "ALL") {
    request["update-policy"] = "WaR1";
  } else if (policy == "NONE") {
    request["update-policy"] = "";
  } else {
    cerr << "Unknown replication policy: " << policy << endl;
    return false;
  }
  request["replication-factor"] = factor;
  request["replication-flags"] = 0;
  if (request["update-policy"] == "ronly" && is_full) {
    request["replication-flags"] = xtreemfs::pbrpc::REPL_FLAG_FULL_REPLICA
        | xtreemfs::pbrpc::REPL_FLAG_STRATEGY_RAREST_FIRST;
  } else if (request["update-policy"] == "ronly" && !is_full) {
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
    cerr << "Setting Default Replication Policy FAILED" << endl;
    return false;
  }
}

// Sets the replication policy for a file.
bool SetReplicationPolicy(const string& xctl_file,
                          const string& path,
                          const variables_map& vm) {


  const string policy_uppercase =
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

  Json::Value request(Json::objectValue);
  request["operation"] = "setReplicationPolicy";
  request["path"] = path;
  if (policy_uppercase == "RONLY" || policy_uppercase == "READONLY") {
    request["policy"] = "ronly";
  } else if (policy_uppercase == "WQRQ" || policy_uppercase == "QUORUM") {
    request["policy"] = "WqRq";
  } else if (policy_uppercase == "WAR1" || policy_uppercase == "ALL") {
    request["policy"] = "WaR1";
  } else if (policy_uppercase == "NONE") {
    request["policy"] = "";
  } else {
    cerr << "Unknown replication policy: " << policy_uppercase << endl;
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
    cout << "Changed replication policy to: "
         << (request["policy"].asString().empty() ? "NONE"
                 : request["policy"].asString())
         << endl;
    if (request["policy"].asString() == "WaR1") {
      cout << "\n"
           << "Please note that manually adding replicas does not work if the"
               " 'all' (WaR1) replication policy is used.\n"
               "See issue 226 for more details:"
               " http://code.google.com/p/xtreemfs/issues/detail?id=226"
           << endl;
    }
    return true;
  } else {
    cerr << "Setting Replication Policy FAILED" << endl;
    return false;
  }
}

// Adds a replica and selects an OSD.
bool AddReplica(const string& xctl_file,
                const string& path,
                const variables_map& vm,
                string osd_uuid) {
  if (boost::to_upper_copy(osd_uuid) == "AUTO") {
    osd_uuid = "AUTO";
  }
  Json::Value request(Json::objectValue);
  request["operation"] = "addReplica";
  request["path"] = path;
  request["osd"] = osd_uuid;
  request["replication-flags"] = 0;
  if (vm.count("full") > 0) {
    request["replication-flags"] = xtreemfs::pbrpc::REPL_FLAG_FULL_REPLICA
        | xtreemfs::pbrpc::REPL_FLAG_STRATEGY_RAREST_FIRST;
  } else {
    request["replication-flags"] =
        xtreemfs::pbrpc::REPL_FLAG_STRATEGY_SEQUENTIAL_PREFETCHING;
  }

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Added new replica on OSD: "
        << response["result"]["osd"].asString() << endl;
    return true;
  } else {
    cerr << "Adding Replica FAILED" << endl;
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
    cerr << "Deleting Replica FAILED" << endl;
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
    for (Json::ArrayIndex i = 0; i < response["result"].size(); i++) {
      cout << "> " << response["result"][i].asString() << endl;
    }
    cout << endl;
    return true;
  } else {
    cerr << "Showing Errors FAILED" << endl;
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
    for (Json::ArrayIndex i = 0; i < response["result"]["osds"].size(); i++) {
      cout << "  " << response["result"]["osds"][i].asString() << endl;
    }
    return true;
  } else {
    cerr << "Getting Suitable OSDs FAILED" << endl;
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
    request["policy"] = boost::lexical_cast<string>(
        xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT)
        + ","
        + boost::lexical_cast<string>(
            xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_RANDOM);
  } else if (policy_uc == "FQDN") {
    request["policy"] = boost::lexical_cast<string>(
        xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT)
        + ","
        + boost::lexical_cast<string>(
            xtreemfs::pbrpc::OSD_SELECTION_POLICY_GROUP_FQDN);
  } else if (policy_uc == "UUID") {
    request["policy"] = boost::lexical_cast<string>(
        xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT)
        + ","
        + boost::lexical_cast<string>(
            xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_UUID);
  } else if (policy_uc == "DCMAP") {
    request["policy"] = boost::lexical_cast<string>(
        xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT)
        + ","
        + boost::lexical_cast<string>(
            xtreemfs::pbrpc::OSD_SELECTION_POLICY_GROUP_DCMAP);
  } else if (policy_uc == "VIVALDI") {
    request["policy"] = boost::lexical_cast<string>(
        xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT)
        + ","
        + boost::lexical_cast<string>(
            xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_VIVALDI);
  } else if (policy_uc == "ROUNDROBIN") {
      request["policy"] = boost::lexical_cast<string>(
          xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT)
          + ","
          + boost::lexical_cast<string>(
              xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_HOST_ROUND_ROBIN);
  } else if (policy_uc == "LASTUPDATED") {
      request["policy"] = boost::lexical_cast<string>(
          xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT)
          + ","
          + boost::lexical_cast<string>(
              xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_LAST_UPDATED);
  } else if (policy_uc == "PREFERRED") {
      request["policy"] = boost::lexical_cast<string>(
          xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT)
          + ","
          + boost::lexical_cast<string>(
              xtreemfs::pbrpc::OSD_SELECTION_POLICY_PREFERRED_UUID);
  } else if (policy_uc == "PREFIX") {
      request["policy"] = boost::lexical_cast<string>(
          xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILTER_DEFAULT)
          + ","
          + boost::lexical_cast<string>(
              xtreemfs::pbrpc::OSD_SELECTION_POLICY_FILENAME_PREFIX);
  } else {
    request["policy"] = policy;
  }

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Updates OSD selection policy to: "
        << policy << endl;
    return true;
  } else {
    cerr << "Setting OSD Selection Policy FAILED" << endl;
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
    // By default, no RSP is set.
    request["policy"] = "";
  } else if (policy_uc == "FQDN") {
    request["policy"] = boost::lexical_cast<string>(
        xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_FQDN);
  } else if (policy_uc == "DCMAP") {
    request["policy"] = boost::lexical_cast<string>(
        xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_DCMAP);
  } else if (policy_uc == "VIVALDI") {
    request["policy"] = boost::lexical_cast<string>(
        xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_VIVALDI);
  } else if (policy_uc == "ROUNDROBIN") {
    request["policy"] = boost::lexical_cast<string>(
        xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_HOST_ROUND_ROBIN);
  } else if (policy_uc == "LASTUPDATED") {
    request["policy"] = boost::lexical_cast<string>(
        xtreemfs::pbrpc::OSD_SELECTION_POLICY_SORT_LAST_UPDATED);
  } else {
    request["policy"] = policy;
  }

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Updates Replica selection policy to: "
        << policy << endl;
    return true;
  } else {
    cerr << "Setting Replica Selection Policy FAILED" << endl;
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
    cerr << "Setting Policy Attribute FAILED" << endl;
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
    const Json::Value::Members& keys = response["result"].getMemberNames();
    for (size_t i = 0; i < keys.size(); i++) {
      cout << keys[i] << " = " << response["result"][keys[i]] << endl;
    }
    return true;
  } else {
    cerr << "Listing Policy Attributes FAILED" << endl;
    return false;
  }
}

bool EnableDisableSnapshots(const string& xctl_file,
                     const string& path,
                     const variables_map& vm) {
  Json::Value request(Json::objectValue);
  request["operation"] = "enableDisableSnapshots";
  request["path"] = path;

  if (vm.count("enable-snapshots") > 0) {
    request["snapshots_enabled"] = "true";
  } else if (vm.count("disable-snapshots") > 0) {
    request["snapshots_enabled"] = "false";
  } else {
    return false;
  }

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Enabling/Disabling Snapshot succeeded." << endl;
    return true;
  } else {
    cerr << "Enabling/Disabling Snapshot FAILED" << endl;
    return false;
  }
}

bool ListSnapshots(const string& xctl_file,
                     const string& path,
                     const variables_map& vm) {
  Json::Value request(Json::objectValue);
  request["operation"] = "listSnapshots";
  request["path"] = path;

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    if (response["result"]["list_snapshots"].empty() ||
        // .empty() does not work for "", check the string directly.
        (response["result"]["list_snapshots"].isString() &&
         response["result"]["list_snapshots"].asString().empty())) {
      cout << "No snapshots available." << endl;
    } else {
      if (response["result"]["list_snapshots"].isString()) {
        cout << "List of available snapshots: "
             << response["result"]["list_snapshots"].asString()
             << endl;
      } else if (response["result"]["list_snapshots"].isArray()) {
        cout << "List of available snapshots:" << endl;
        const Json::Value& snapshots = response["result"]["list_snapshots"];
        for (int i = 0; i < snapshots.size(); i++) {
          cout << "- " << snapshots[i].asString() << endl;
        }
      } else {
        cerr << "Listing Snapshots FAILED (to parse the list of snapshots)" << endl;
        return false;
      }
    }
    return true;
  } else {
    cerr << "Listing Snapshots FAILED" << endl;
    return false;
  }
}

bool CreateDeleteSnapshot(const string& xctl_file,
                          const string& path,
                          const variables_map& vm) {
  Json::Value request(Json::objectValue);
  request["operation"] = "createDeleteSnapshot";
  request["path"] = path;
  if (vm.count("create-snapshot") > 0) {
    request["snapshots"] = "cr " + vm["create-snapshot"].as<string>();
  } else if (vm.count("create-snapshot-non-recursive") > 0) {
    request["snapshots"] = "c "
        + vm["create-snapshot-non-recursive"].as<string>();
  } else if (vm.count("delete-snapshot") > 0) {
    request["snapshots"] = "d " + vm["delete-snapshot"].as<string>();
  } else {
    return false;
  }

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Creating/Deleting Snapshot succeeded." << endl;
    return true;
  } else {
    cerr << "Creating/Deleting Snapshot FAILED" << endl;
    return false;
  }
}

bool EnableDisableTracing(const string& xctl_file,
                     const string& path,
                     const variables_map& vm) {
  Json::Value request(Json::objectValue);
  request["operation"] = "enableDisableTracing";
  if (vm.count("disable-tracing") > 0) {
    request["enable_tracing"] = "0";
  }
  if (vm.count("enable-tracing") > 0) {
    request["enable_tracing"] = "1";
    if (vm.count("tracing-policy-config") > 0 ) {
      request["tracing_policy_config"] = vm["tracing-policy-config"].as<string>();
    }
    if (vm.count("tracing-policy") > 0) {
      request["tracing_policy"] = vm["tracing-policy"].as<string>();
    } else {
      request["tracing_policy"] = "default";
    }
  }
  request["path"] = path;

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Success." << endl;
    return true;
  } else {
    cerr << "FAILED" << endl;
    return false;
  }
}

// Sets/Modifies/Removes the ACL.
bool SetRemoveACL(const string& xctl_file,
                  const string& path,
                  const variables_map& vm) {
  string contents;
  if (vm.count("set-acl") > 0) {
    contents = "m " + vm["set-acl"].as<string>();
  } else if (vm.count("del-acl") > 0) {
    contents = "x " + vm["del-acl"].as<string>();
  } else {
    return false;
  }

  Json::Value request(Json::objectValue);
  request["operation"] = "setRemoveACL";
  request["path"] = path;
  request["acl"] = contents;

  Json::Value response;
  if (executeOperation(xctl_file, request, &response)) {
    cout << "Setting/Modifying/Removing ACL succeeded." << endl;
    return true;
  } else {
    cerr << "Setting/Modifying/Removong ACL FAILED" << endl;
    return false;
  }
}

string GetPathOnVolume(const char* real_path_cstr) {
  string path_on_volume;
#ifdef __sun
  // Solaris' Fuse seems to have no working xattr support. Parse /etc/mnttab.
  const char mtab_file[] = "/etc/mnttab";
  ifstream in(mtab_file);
  if (!in.is_open()) {
    throw xtreemfs::XtreemFSException(
        "Could not determine the path on the volume."
        " Failed to open the file: " + string(mtab_file));
  }

  string real_path = string(real_path_cstr);
  std::string line;
  const boost::regex mtab_mount_point_re("^xtreemfs@[^\\t]+\\t([^\\t]+)\\tfuse\\t");  // NOLINT
  bool entry_found = false;
  while (getline(in, line)) {
    boost::smatch matcher;
    if (boost::regex_search(line, matcher, mtab_mount_point_re)) {
      string mount_point = matcher[1];
      if (real_path.substr(0, mount_point.length()) == mount_point) {
        path_on_volume = real_path.substr(mount_point.length());
        entry_found = true;
        break;
      }
    }
  }
  if (!entry_found) {
    throw xtreemfs::XtreemFSException("No matching mounted XtreemFS volume"
        " found in " + string(mtab_file) +
        " for path: " + string(real_path_cstr));
  }
#elif defined __linux || __APPLE__
  // get xtreemfs.url xattr.
  char xtfs_url[2048];
  int length = -1;
#ifdef __linux
  length = getxattr(real_path_cstr, "xtreemfs.url", xtfs_url, 2048);
#elif __APPLE__
  length = getxattr(real_path_cstr, "xtreemfs.url", xtfs_url, 2048, 0, 0);
#endif

  if (length <= 0) {
    struct stat sb;
    if (stat(real_path_cstr, &sb)) {
      // Show more meaningful error message if path does not exist at all.
      throw xtreemfs::XtreemFSException("File/Directory does not exist: "
          + string(real_path_cstr));
    } else {
        throw xtreemfs::XtreemFSException("Path doesn't point to an entity on"
            " an XtreemFS volume!\nxattr xtreemfs.url is missing.");
    }
  }

  string url(xtfs_url, length);
  const boost::regex pure_path_re("pbrpc.?://[^/]+/[^/]+(.*)");
  boost::smatch matcher;
  if (!boost::regex_match(url, matcher, pure_path_re)) {
    throw xtreemfs::XtreemFSException("Invalid XtreemFS url!");
  }
  path_on_volume = matcher[1];
#elif __FreeBSD__
  string real_path = string(real_path_cstr);
  FILE* in;
  char buf[1024];

  if (!(in = popen("mount", "r"))) {
    pclose(in);
    throw xtreemfs::XtreemFSException("Failed to run the 'mount' command to"
        " find out the path relative to the volume root.");
  }

  const boost::regex mount_point_re("^/dev/fuse[0-9] on ([^ ]+) \\(fusefs");  // NOLINT
  bool entry_found = false;
  while (fgets(buf, sizeof(buf), in) != NULL) {
    string line(buf);

    boost::smatch matcher;
    if (boost::regex_search(line, matcher, mount_point_re)) {
      string mount_point = matcher[1];
      if (real_path.substr(0, mount_point.length()) == mount_point) {
        path_on_volume = real_path.substr(mount_point.length());
        entry_found = true;
        break;
      }
    }
  }
  pclose(in);

  if (!entry_found) {
    throw xtreemfs::XtreemFSException("No matching mounted XtreemFS volume"
        " found in 'mount' output for path: " + string(real_path_cstr));
  }
#else
  #error "Plattform not supported yet by xtfsutil. Please add plattform-specific code to GetPathOnVolume() or disable compilation of xtfsutil in the CMake specification."  // NOLINT
#endif

  return path_on_volume;
}

int main(int argc, char **argv) {
  string option_add_replica, option_path;

  options_description hidden("Hidden positional option");
  hidden.add_options()
      ("path", value(&option_path), "path on mounted XtreemFS volume");

  options_description desc("Allowed options");
  desc.add_options()
      ("help,h", "produce help message")
      ("version,V", "Show the version number.")
      ("errors", "show client errors for a volume")
      ("set-dsp", "set (change) the default striping policy (volume)")
      ("striping-policy,p",
       value<string>()->implicit_value("RAID0"),
       "striping policy (always RAID0)")
      ("striping-policy-width,w", value<int>(),
       "striping width (number of OSDs)")
      ("striping-policy-stripe-size,s", value<int>(),
       "stripe size in kB (object size)")
      ("set-drp", "set (change) the default replication policy (volume)")
      ("replication-policy", value<string>(),
       "RONLY, WqRq, WaR1 or NONE to disable replication. The aliases"
       " 'readonly', 'quorum' and 'all' are also allowed.")
      ("replication-factor", value<int>(),
       "number of replicas to create for a file")
      ("full", "full replica (readonly replication only, only allowed if"
               " --set-drp or --add-replica is set)")
      ("set-replication-policy,r", value<string>(),
       "set (change) the replication policy for a file: RONLY, WqRq, WaR1 or"
       " NONE to disable replication. The aliases"
       " 'readonly', 'quorum' and 'all' are also allowed.")
      ("add-replica,a", value(&option_add_replica)->implicit_value("AUTO"),
       "adds a new replica on the osd with the given UUID or AUTO "
       "for automatics OSD selection")
      ("delete-replica,d", value<string>(),
       "deletes the replica on the OSD with the given UUID")
      ("list-osds,l", "list suitable OSDs for a file")
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

  options_description quota_desc("Quota Options");
  quota_desc.add_options()
      ("get-quotas", "get the quotainformation for volume, all users and all groups, "
          "or if specified, only a specific value")
       ("all-users", "extends --get-quota to show the quota of all users")
       ("all-groups", "extends --get-quota to show the quota of all groups")
      ("set-quota", value<string>(),
       "sets the value as quota on volume, user or group level (needs sub option)"
       "Value will be specified in bytes "
       "(0 means unlimited/disabled), format: <value>[K|M|...|E]")
       ("volume,v", "option will be processed for the volume")
       ("user,u", value<string>(), "option will be processed for the given user")
       ("group,g", value<string>(), "option will be processed for the given group")
       ("set-voucher-size", value<string>(), "sets the value as voucher")
       ("set-default-user-quota", value<string>(), "option will be processed for the default user quota")
       ("set-default-group-quota", value<string>(), "option will be processed for the default group quota");


  options_description snapshot_desc("Snapshot Options");
  snapshot_desc.add_options()
      ("enable-snapshots",
       "Enable snapshots on the volume.")
      ("disable-snapshots",
       "Disable snapshots on the volume.")
      ("list-snapshots",
       "List all available snapshots.")
      ("create-snapshot",
       value<string>(),
       "Create a snapshot of the volume/directory with the name <arg>. If the argument is \"\", the current server time will be used as snapshot name.")
      ("create-snapshot-non-recursive",
       value<string>(),
       "Same as --create-snapshot, however sub-directories are excluded.")
      ("delete-snapshot",
       value<string>(),
       "Delete the snapshot with the name given as argument.");


  options_description tracing_desc("Tracing Options");
  tracing_desc.add_options()
      ("enable-tracing",
       "Enable tracing on the volume.")
      ("disable-tracing",
       "disable tracing on the volume.")
      ("tracing-policy-config",
       value<string>(),
       "Volume to write trace")
      ("tracing-policy",
       value<string>(),
       "Tracing policy");

  positional_options_description pd;
  pd.add("path", 1);

  options_description cmdline_options;
  cmdline_options.add(desc).add(quota_desc).add(snapshot_desc).add(tracing_desc).add(hidden);
  variables_map vm;
  try {
    store(command_line_parser(argc, argv)
              .positional(pd)
              .options(cmdline_options)
              .run(),
          vm);
    notify(vm);
  } catch(const std::exception& e) {
    cerr << "Invalid command line options: " << e.what() << endl
         << endl
         << "Usage: xtfsutil <path>" << endl
         << "See xtfsutil -h for the complete list of available options."
         << endl;
    return 1;
  }

  if (vm.count("version")) {
    cout << "xtfsutil " << kVersionString << endl;
    return 1;
  }

  // Work around problem of omitted -a AUTO argument in which case the path
  // ends up in vm["add-replica"].
  if (!option_add_replica.empty() &&
      !vm.count("help") && option_path.empty()) {
    // path not set - may be it is accidentally stored in add-replica.
    struct stat sb;
    if (!stat(option_add_replica.c_str(), &sb)) {
      // File exists, move path from add-replica to variable path.
      option_path = option_add_replica;
      option_add_replica = "AUTO";
    }
  }

  if (vm.count("help") || option_path.empty()) {
    cerr << "Usage: xtfsutil <path>" << endl;
    cerr << desc << quota_desc << snapshot_desc << tracing_desc << endl;
    return 1;
  }

  // Do some checks on the allowed parameter combinations.
  if (vm.count("full") > 0) {
    if (vm.count("add-replica") == 0 && vm.count("set-drp") == 0) {
      cerr << "--full is only allowed in conjunction with --add-replica or"
              " --set-drp" << endl
           << endl
           << "Usage: xtfsutil <path>" << endl
           << desc << endl;
      return 1;
    }
  }
  if (vm.count("replication-policy") > 0 ||
      vm.count("replication-factor") > 0) {
    if (vm.count("set-drp") == 0) {
      cerr << "--replication-policy or --replication-factor are only allowed in"
              " conjunction with --set-drp" << endl
           << endl
           << "Usage: xtfsutil <path>" << endl
           << desc << endl;
      return 1;
    }
  }
  if (vm.count("striping-policy") > 0 ||
      vm.count("striping-policy-width") > 0 ||
      vm.count("striping-policy-stripe-size") > 0) {
    if (vm.count("set-dsp") == 0) {
      cerr << "--striping-policy, --striping-policy-width or"
              " --striping-policy-stripe-size are only allowed in"
              " conjunction with --set-dsp" << endl
           << endl
           << "Usage: xtfsutil <path>" << endl
           << desc << endl;
      return 1;
    }
  }
  if (vm.count("value") > 0) {
    if (vm.count("set-pattr") == 0) {
      cerr << "--value is only allowed in conjunction with --set-pattr" << endl
           << endl
           << "Usage: xtfsutil <path>" << endl
           << desc << endl;
      return 1;
    }
  }
  if (vm.count("volume") > 0 || vm.count("user") > 0 || vm.count("group") > 0
      || vm.count("all-users") > 0 || vm.count("all-groups") > 0) {

    if (vm.count("set-quota") == 0 && vm.count("get-quotas") == 0) {
      cerr << "--volume, --user, --group are only allowed in conjunction "
           "with --set-quota and --get-quotas." << endl
           << "--all-users and --all-groups are only allowed with --get-quotas"
           << endl << endl << "Usage: xtfsutil <path>" << endl << quota_desc << endl;
      return 1;
    }

    if ((vm.count("all-users") > 0 || vm.count("all-groups") > 0)
        && vm.count("get-quotas") == 0) {
      cerr << "--all-users and all-groups are only allowed in conjunction "
           "with --get-quotas"
           << endl << endl << "Usage: xtfsutil <path>" << endl << quota_desc << endl;
      return 1;
    }

    int sum = vm.count("volume") + vm.count("user") + vm.count("group")
        + vm.count("all-users") + vm.count("all-groups");
    if (sum != 1) {
      cerr
          << "Only one of the following options can be specified on a single request: "
          << endl << "--volume, --user, --group, --all-users and --all-groups"
          << endl << endl << "Usage: xtfsutil <path>" << endl << quota_desc << endl;
      return 1;
    }
  }

  char* real_path_cstr = realpath(option_path.c_str(), NULL);
  if (!real_path_cstr) {
    cerr << "xtfsutil failed to find the absolute path of: "
         << option_path
         << " Maybe the path does not exist?"
         << endl;
    return 1;
  }
  string path_on_volume;
  try {
    path_on_volume = GetPathOnVolume(real_path_cstr);
  } catch (const xtreemfs::XtreemFSException &e) {
    cerr << "xtfsutil failed: " << e.what() << endl;
    return 1;
  }

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

  size_t operationsCount = 0, failedOperationsCount = 0;
  if (vm.count("set-dsp") > 0) {
    ++operationsCount;
    failedOperationsCount += SetDefaultSP(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("set-drp") > 0) {
    ++operationsCount;
    failedOperationsCount += SetDefaultRP(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("set-osp") > 0) {
    ++operationsCount;
    failedOperationsCount += SetOSP(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("set-rsp") > 0) {
    ++operationsCount;
    failedOperationsCount += SetRSP(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("set-pattr") > 0) {
    ++operationsCount;
    failedOperationsCount += SetPolicyAttr(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("list-pattrs") > 0) {
    ++operationsCount;
    failedOperationsCount += ListPolicyAttrs(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("set-replication-policy") > 0) {
    ++operationsCount;
    failedOperationsCount += SetReplicationPolicy(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("add-replica") > 0) {
    ++operationsCount;
    failedOperationsCount += AddReplica(xctl_file, path_on_volume, vm, option_add_replica) ? 0
           : 1;
  }
  if (vm.count("delete-replica") > 0) {
    ++operationsCount;
    failedOperationsCount += DeleteReplica(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("list-osds") > 0) {
    ++operationsCount;
    failedOperationsCount += GetSuitableOSDs(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("set-acl") > 0 ||
      vm.count("del-acl") > 0) {
    ++operationsCount;
    failedOperationsCount += SetRemoveACL(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("enable-snapshots") > 0 ||
      vm.count("disable-snapshots") > 0) {
    ++operationsCount;
    failedOperationsCount += EnableDisableSnapshots(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("list-snapshots") > 0) {
    ++operationsCount;
    failedOperationsCount += ListSnapshots(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("create-snapshot") > 0 ||
      vm.count("create-snapshot-non-recursive") > 0 ||
      vm.count("delete-snapshot") > 0) {
    ++operationsCount;
    failedOperationsCount += CreateDeleteSnapshot(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("errors") > 0) {
    ++operationsCount;
    failedOperationsCount += ShowErrors(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("set-quota") > 0) {
    ++operationsCount;
    failedOperationsCount += SetQuota(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("get-quotas") > 0) {
    ++operationsCount;
    failedOperationsCount += GetQuota(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("set-voucher-size") > 0
      || vm.count("set-default-user-quota") > 0
      || vm.count("set-default-group-quota") > 0) {
    ++operationsCount;
    failedOperationsCount += SetQuotaRelatedValue(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if (vm.count("enable-tracing") > 0 ||
      vm.count("disable-tracing") > 0) {
    ++operationsCount;
    failedOperationsCount += EnableDisableTracing(xctl_file, path_on_volume, vm) ? 0 : 1;
  }
  if(operationsCount == 0){
    ++operationsCount;
    failedOperationsCount += getattr(xctl_file, path_on_volume) ? 0 : 1;
  }

  if(failedOperationsCount > 0) {
    cerr << failedOperationsCount << " of " << operationsCount << " operation(s) failed." << endl;
    return 1;
  } else {
    return 0;
  }
}
