/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *               2011-2014 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/helper.h"

#include <cstdio>
#include <cstdlib>
#include <stdint.h>

#include <boost/lexical_cast.hpp>
#include <iostream>
#include <string>

#include "libxtreemfs/options.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include <boost/algorithm/string.hpp>
#include "rpc/sync_callback.h"
#include "util/error_log.h"
#include "util/logging.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/MRC.pb.h"
#include "xtreemfs/OSD.pb.h"

#ifdef __APPLE__
#include <sys/utsname.h>
#endif  // __APPLE__

#ifdef WIN32
#define NOMINMAX
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#endif  // WIN32

#ifdef __linux__
#include <arpa/inet.h>
#include <ifaddrs.h>
#include <netdb.h>
#include <sys/socket.h>
#endif  // __linux__

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {

int CompareOSDWriteResponses(
    const xtreemfs::pbrpc::OSDWriteResponse* new_response,
    const xtreemfs::pbrpc::OSDWriteResponse* current_response) {
  if (new_response == NULL && current_response == NULL) {
    return 0;
  } else if (new_response != NULL && current_response == NULL) {
    // new_response > current_response.
    return 1;
  } else if (new_response == NULL && current_response != NULL) {
    // new_response < current_response.
    return -1;
  } else if (
      new_response->truncate_epoch() > current_response->truncate_epoch() ||
      (new_response->truncate_epoch() == current_response->truncate_epoch()
       && new_response->size_in_bytes() > current_response->size_in_bytes())) {
    // new_response > current_response.
    return 1;
  } else if (
      new_response->truncate_epoch() < current_response->truncate_epoch() ||
      (new_response->truncate_epoch() == current_response->truncate_epoch()
       && new_response->size_in_bytes() < current_response->size_in_bytes())) {
    // new_response < current_response.
    return -1;
  } else {
    // new_response == current_response.
    return 0;
  }
}

/** The global file id  contains the Volume UUID and File ID concatenated by a ":". */
uint64_t ExtractFileIdFromGlobalFileId(std::string global_file_id) {
  int start = global_file_id.find(":") + 1;
  int length = global_file_id.length() - start;
  return boost::lexical_cast<uint64_t>(
      global_file_id.substr(start, length));
}

/** The XCap contains the global file id. */
uint64_t ExtractFileIdFromXCap(const xtreemfs::pbrpc::XCap& xcap) {
  string global_file_id = xcap.file_id();
  return ExtractFileIdFromGlobalFileId(global_file_id);
}

std::string ResolveParentDirectory(const std::string& path) {
  int last_slash = path.find_last_of("/");
  if (path == "/" || last_slash == 0) {
    return "/";
  } else {
    return path.substr(0, last_slash);
  }
}

std::string GetBasename(const std::string& path) {
  int last_slash = path.find_last_of("/");
  if (path == "/") {
    return "/";
  } else {
    // We don't allow path to have a trailing "/".
    assert(last_slash != (path.length() - 1));

    return path.substr(last_slash + 1);
  }
}

std::string ConcatenatePath(const std::string& directory,
                            const std::string& file) {
  // handle .. and .
  if (file == ".") {
    return directory;
  } else if (file == "..") {
    if (directory == "/") {
      return directory;
    }
    return directory.substr(0, directory.find_last_of("/"));
  }

  if (directory == "/") {
    return "/" + file;
  } else {
    return directory + "/" + file;
  }
}

std::string GetOSDUUIDFromXlocSet(const xtreemfs::pbrpc::XLocSet& xlocs,
                                  uint32_t replica_index,
                                  uint32_t stripe_index) {
  if (xlocs.replicas_size() == 0) {
    Logging::log->getLog(LEVEL_ERROR)
       << "GetOSDUUIDFromXlocSet: Empty replicas list in XlocSet: "
       <<  xlocs.DebugString() << endl;
    return "";
  }

  const xtreemfs::pbrpc::Replica& replica = xlocs.replicas(replica_index);
  if (replica.osd_uuids_size() == 0) {
    Logging::log->getLog(LEVEL_ERROR)
        << "GetOSDUUIDFromXlocSet: No head OSD available in XlocSet:"
        <<  xlocs.DebugString() << endl;
    return "";
  }

  return replica.osd_uuids(stripe_index);
}

std::string GetOSDUUIDFromXlocSet(
    const xtreemfs::pbrpc::XLocSet& xlocs) {
  // Get the UUID for the first replica (r=0) and the head OSD (i.e. the first
  // chunk, c=0).
  return GetOSDUUIDFromXlocSet(xlocs, 0, 0);
}

std::string StripePolicyTypeToString(xtreemfs::pbrpc::StripingPolicyType policy) {
  std::string policyMap[] = { "STRIPING_POLICY_RAID0" };
  return policyMap[policy];
}

/**
 * By default this function does read random data from /dev/urandom and falls
 * back to using C's rand() if /dev/random is not available.
 */
void GenerateVersion4UUID(std::string* result) {
  FILE *urandom = fopen("/dev/urandom", "r");
  if (!urandom) {
    // Use rand() instead if /dev/urandom not available.
    srand(static_cast<unsigned int>(time(NULL)));
  }

  // Base62 characters for UUID generation.
  char set[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

  uint32_t block_length[] = {8, 4, 4, 4, 12};
  uint32_t block_length_count = 5;
  char uuid[37];

  uint64_t random_value;
  int pos = 0;
  for (uint32_t j = 0; j < block_length_count; j++) {
    for (uint32_t i = 0; i < block_length[j]; i++) {
      // Read random number.
      if (urandom) {
        fread(&random_value, 1, sizeof(random_value), urandom);
      } else {
        // Use C's rand() if /dev/urandom not available.
        random_value = rand();  // NOLINT
      }

      uuid[pos] = set[random_value % 62];
      pos++;
    }
    uuid[pos++] = '-';
  }

  uuid[36] = '\0';
  *result = string(uuid);

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) <<
        "Generated client UUID: " << uuid << endl;
  }

  if (urandom) {
    fclose(urandom);
  }
}

void InitializeStat(xtreemfs::pbrpc::Stat* stat) {
  stat->set_dev(0);
  stat->set_ino(0);
  stat->set_mode(0);
  // If not set to 1, an assertion in the metadata cache will be triggered.
  stat->set_nlink(1);
  stat->set_user_id("");
  stat->set_group_id("");
  stat->set_size(0);
  stat->set_atime_ns(0);
  stat->set_mtime_ns(0);
  stat->set_ctime_ns(0);
  stat->set_blksize(0);
  stat->set_truncate_epoch(0);
}

bool CheckIfLocksAreEqual(const xtreemfs::pbrpc::Lock& lock1,
                          const xtreemfs::pbrpc::Lock& lock2) {
  return //lock1 != NULL && lock2 != NULL &&
      lock1.client_uuid() == lock2.client_uuid()
      && lock1.client_pid() == lock2.client_pid()
      && lock1.offset() == lock2.offset()
      && lock1.length() == lock2.length();
}

bool CheckIfLocksDoConflict(const xtreemfs::pbrpc::Lock& lock1,
                            const xtreemfs::pbrpc::Lock& lock2) {
  // 0 means to lock till the end of the file.
  // the end is not included
  uint64_t lock1_end = lock1.length() == 0 ? 0 :
      lock1.offset() + lock1.length();
  uint64_t lock2_end = lock2.length() == 0 ? 0 :
      lock2.offset() + lock2.length();

  // Check for overlaps.
  if (lock1_end == 0) {
    if (lock2_end > lock1.offset() || lock2_end == 0) {
      return true;
    }
  }
  if (lock2_end == 0) {
    if (lock1_end > lock2.offset() || lock1_end == 0) {
      return true;
    }
  }
  // Overlapping?
  if (!(lock1_end <= lock2.offset() || lock1.offset() >= lock2_end)) {
    // Does overlap, check for conflicting modes.
    return lock1.exclusive() || lock2.exclusive();
  }

  return false;
}

bool CheckIfUnsignedInteger(const std::string& string) {
  if (string.empty()) {
    return false;
  }

  try {
    // It's needed to use a 64 bit signed integer to detect a -(2^31)-1
    // as a negative value and not as an overflowed unsigned integer of
    // value 2^32-1.
    int64_t integer = boost::lexical_cast<int64_t>(string);
    // If casted to uint, no bad_lexical_cast is thrown for negative values -
    // therefore we check for them on our own.
    if (integer < 0) {
      return false;
    }
  } catch(const boost::bad_lexical_cast&) {
    return false;
  }

  return true;  // It actually was an unsigned integer.
}

RPCOptions RPCOptionsFromOptions(const Options& options) {
  return RPCOptions(options.max_tries,
                    options.retry_delay_s,
                    false,  // do not delay last attempt
                    options.was_interrupted_function);
}

#ifdef __APPLE__
int GetMacOSXKernelVersion() {
  int darwin_kernel_version = -1;

  struct utsname uname_result;
  uname(&uname_result);
  string darwin_release(uname_result.release);
  size_t first_dot = darwin_release.find_first_of(".");
  try {
    darwin_kernel_version = boost::lexical_cast<int>(
        darwin_release.substr(0, first_dot));
  } catch(const boost::bad_lexical_cast& e) {
    if (Logging::log->loggingActive(LEVEL_WARN)) {
      Logging::log->getLog(LEVEL_WARN) << "Failed to retrieve the kernel "
          "version, got: " << darwin_kernel_version << endl;
    }
  }

  return darwin_kernel_version;
}
#endif  // __APPLE__

#ifdef WIN32
std::string ConvertWindowsToUTF8(const wchar_t* windows_string) {
  string utf8;
  ConvertWindowsToUTF8(windows_string, &utf8);

  return utf8;
}

void ConvertWindowsToUTF8(const wchar_t* from,
                          std::string* utf8) {
  // Assume that most strings will fit into a kDefaultBufferSize sized buffer.
  // If not, the buffer will be increased.
  const size_t kDefaultBufferSize = 1024;
  // resize() does not count the null-terminating char, WideCharTo... does.
  utf8->resize(kDefaultBufferSize - 1);

  int r = WideCharToMultiByte(CP_UTF8,
                              0,
                              from,
                              -1,
                              &((*utf8)[0]),
                              kDefaultBufferSize,
                              0,
                              0);

  if (r == 0) {
    throw XtreemFSException("Failed to convert a UTF-16"
        " (wide character) string to an UTF8 string."
        " Error code: "
        + boost::lexical_cast<string>(::GetLastError()));
  }

  utf8->resize(r - 1);
  if (r > kDefaultBufferSize) {
    int r2 = WideCharToMultiByte(CP_UTF8, 0, from, -1, &((*utf8)[0]), r, 0, 0);
    if (r != r2 || r2 == 0) {
      throw XtreemFSException("Failed to convert a UTF-16"
         " (wide character) string to an UTF8 string."
         " Error code: "
         + boost::lexical_cast<string>(::GetLastError()));
    }
  }
}

void ConvertUTF8ToWindows(const std::string& utf8,
                          wchar_t* buf,
                          int buffer_size) {
  int r = MultiByteToWideChar(CP_UTF8, 0, utf8.c_str(), -1, buf, buffer_size);
  if (r == 0) {
    throw XtreemFSException("Failed to convert this UTF8 string to a UTF-16"
        " (wide character) string: " + utf8
        + " Error code: "
        + boost::lexical_cast<string>(::GetLastError()));
  }
}

std::wstring ConvertUTF8ToWindows(const std::string& utf8) {
  wstring win;
  ConvertUTF8ToWindows(utf8, &win);
  return win;
}

void ConvertUTF8ToWindows(const std::string& utf8,
                          std::wstring* win) {
  // Assume that most strings will fit into a kDefaultBufferSize sized buffer.
  // If not, the buffer will be increased.
  const size_t kDefaultBufferSize = 1024;
  // resize() does not count the null-terminating char, MultiByteToWide... does.
  win->resize(kDefaultBufferSize - 1);

  int r = MultiByteToWideChar(CP_UTF8,
                              0,
                              utf8.c_str(),
                              -1,
                              &((*win)[0]),
                              kDefaultBufferSize);

  if (r == 0) {
    throw XtreemFSException("Failed to convert a UTF-8"
        " string to an UTF16 string (wide character)."
        " Error code: "
        + boost::lexical_cast<string>(::GetLastError()));
  }

  win->resize(r - 1);
  if (r > kDefaultBufferSize) {
    int r2 = MultiByteToWideChar(CP_UTF8, 0, utf8.c_str(), -1, &((*win)[0]), r);
    if (r != r2 || r2 == 0) {
      throw XtreemFSException("Failed to convert a UTF-8"
         " string to an UTF16 string (wide character)."
         " Error code: "
         + boost::lexical_cast<string>(::GetLastError()));
    }
  }
}
#endif  // WIN32

/** Returns the number of ones in the array "netmask" as network prefix. */
int GetNetworkPrefixUnix(const uint32_t* netmask, size_t length) {
  // Iterate over "netmask" in chunks of uint32_t.
  int c = 0;
  for (uint32_t i = 0; i < length / sizeof(uint32_t); i++) {
    uint32_t v = *(reinterpret_cast<const uint32_t*>(netmask + i));
    for (; v; c++) {
      v &= v - 1;  // Clear the least significant bit set.
    }
  }
  return c;
}

/** Masks "address" with "netmask" (AND) and produces "network_address". */
void BitwiseAndOfAddressses(
    char* address, char* netmask, char* network_address, size_t length) {
  // Process data in chunks of 1 byte chars.
  for (size_t i = 0; i < length / sizeof(char); i++) {
    network_address[i] = address[i] & netmask[i];
  }
}

#ifdef __linux__
std::string GetNetworkStringUnix(const struct ifaddrs* ifaddr) {
  assert(ifaddr->ifa_addr);
  assert(ifaddr->ifa_netmask);
  assert(ifaddr->ifa_addr->sa_family == ifaddr->ifa_netmask->sa_family);

  ostringstream network;

  // Network address.
  char ip_printable[NI_MAXHOST];
  int result = -1;
  if (ifaddr->ifa_netmask->sa_family == AF_INET) {
    struct sockaddr_in network_address = {};
    network_address.sin_family = ifaddr->ifa_netmask->sa_family;
    BitwiseAndOfAddressses(
        reinterpret_cast<char*>(&reinterpret_cast<struct sockaddr_in*>(
            ifaddr->ifa_addr)->sin_addr),
        reinterpret_cast<char*>(&reinterpret_cast<struct sockaddr_in*>(
            ifaddr->ifa_netmask)->sin_addr),
        reinterpret_cast<char*>(&network_address.sin_addr),
        sizeof(network_address.sin_addr));
    result = getnameinfo(reinterpret_cast<struct sockaddr*>(&network_address),
                         sizeof(network_address),
                         ip_printable,
                         NI_MAXHOST,
                         NULL,
                         0,
                         NI_NUMERICHOST);
  } else if (ifaddr->ifa_netmask->sa_family == AF_INET6) {
    struct sockaddr_in6 network_address = {};
    network_address.sin6_family = ifaddr->ifa_netmask->sa_family;
    BitwiseAndOfAddressses(
        reinterpret_cast<char*>(&reinterpret_cast<struct sockaddr_in6*>(
            ifaddr->ifa_addr)->sin6_addr),
        reinterpret_cast<char*>(&reinterpret_cast<struct sockaddr_in6*>(
            ifaddr->ifa_netmask)->sin6_addr),
        reinterpret_cast<char*>(&network_address.sin6_addr),
        sizeof(network_address.sin6_addr));
    result = getnameinfo(reinterpret_cast<struct sockaddr*>(&network_address),
                         sizeof(network_address),
                         ip_printable,
                         NI_MAXHOST,
                         NULL,
                         0,
                         NI_NUMERICHOST);
  } else {
    assert(ifaddr->ifa_netmask->sa_family == AF_INET ||
           ifaddr->ifa_netmask->sa_family == AF_INET6);
  }
  if (result == 0) {
    network << ip_printable;
  } else {
    throw XtreemFSException("Failed to convert an IP address from the internal"
        " network order representation to the printable text presentation."
        " Error: " + boost::lexical_cast<string>(result));
  }

  // Separator.
  network << "/";

  // Prefix.
  if (ifaddr->ifa_netmask->sa_family == AF_INET) {
    struct in_addr netmask =
        reinterpret_cast<struct sockaddr_in*>(ifaddr->ifa_netmask)->sin_addr;
    network << GetNetworkPrefixUnix(reinterpret_cast<uint32_t*>(&netmask),
                                    sizeof(netmask));
  } else if (ifaddr->ifa_netmask->sa_family == AF_INET6) {
    struct in6_addr netmask =
        reinterpret_cast<struct sockaddr_in6*>(ifaddr->ifa_netmask)->sin6_addr;
    network << GetNetworkPrefixUnix(reinterpret_cast<uint32_t*>(&netmask),
                                    sizeof(netmask));
  } else {
    assert(ifaddr->ifa_netmask->sa_family == AF_INET ||
           ifaddr->ifa_netmask->sa_family == AF_INET6);
  }

  return network.str();
}
#endif  // __linux__

boost::unordered_set<std::string> GetNetworks() {
  boost::unordered_set<std::string> result;

#ifdef __linux__
  struct ifaddrs* ifaddr = NULL;
  if (getifaddrs(&ifaddr) == -1) {
    freeifaddrs(ifaddr);
    throw XtreemFSException("Failed to get the list of network interfaces."
        " Error: " + boost::lexical_cast<string>(errno));
  }
  for (struct ifaddrs* ifa = ifaddr; ifa != NULL; ifa = ifa->ifa_next) {
    if (ifa->ifa_addr == NULL) {
      continue;
    }
    if (ifa->ifa_addr->sa_family == AF_INET ||
        ifa->ifa_addr->sa_family == AF_INET6) {
      try {
        result.insert(GetNetworkStringUnix(ifa));
      } catch (const XtreemFSException& e) {
        if (Logging::log->loggingActive(LEVEL_WARN)) {
          Logging::log->getLog(LEVEL_WARN) << "Converting the information about"
              " the network interface: " << ifa->ifa_name << " with the"
              " family: " << ifa->ifa_addr->sa_family << " failed."
              " Error: " << e.what() << " The device was ignored." << endl;
        }
      }
    }
  }
  freeifaddrs(ifaddr);
#endif  // __linux__

  return result;
}

/**
 *  Parses human-readable byte number to byte count. Returns -1 if byte_number is not parsable.
 */
long parseByteNumber(std::string byte_number) {
  std::string multiplier;
  long long coeff;
  std::stringstream ss;
  ss << byte_number;
  ss >> coeff;
  ss >> multiplier;
  boost::to_upper(multiplier);

  if (multiplier.length() == 0 || multiplier == "B"){
    return coeff;
  }

  if (multiplier.length() > 2 || (multiplier.length() == 2 && multiplier[1] != 'B')) {
    return -1;
  }

  long factor = 1L;

  switch (multiplier[0]) {
    case 'K': factor = 1024L; break;
    case 'M': factor = 1024L*1024L; break;
    case 'G': factor = 1024L*1024L*1024L; break;
    case 'T': factor = 1024L*1024L*1024L*1024L; break;
    case 'P': factor = 1024L*1024L*1024L*1024L*1024L; break;
    case 'E': factor = 1024L*1024L*1024L*1024L*1024L*1024L; break;
    default:   return -1;
  }
  return coeff * factor;
}

void LogAndThrowXtreemFSException(std::string error_msg) {
  util::Logging::log->getLog(util::LEVEL_ERROR) << error_msg
                                                << std::endl;
  ErrorLog::error_log->AppendError(error_msg);
  throw XtreemFSException(error_msg);
}

}  // namespace xtreemfs
