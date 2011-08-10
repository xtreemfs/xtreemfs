/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *                    2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/helper.h"

#include <cstdio>
#include <cstdlib>
#ifdef __APPLE__
#include <sys/utsname.h>
#endif  // __APPLE__

#include <boost/lexical_cast.hpp>
#include <string>

#include "libxtreemfs/xtreemfs_exception.h"
#include "rpc/sync_callback.h"
#include "util/logging.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/MRC.pb.h"
#include "xtreemfs/OSD.pb.h"

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

/** The XCap contains the Volume UUID and File ID concatenated by a ":". */
boost::uint64_t ExtractFileIdFromXCap(const xtreemfs::pbrpc::XCap& xcap) {
  string string = xcap.file_id();

  int start = string.find(":") + 1;
  int length = string.length() - start;
  return boost::lexical_cast<boost::uint64_t>(
      string.substr(start, length));
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
       <<  xlocs.DebugString() << std::endl;
    return "";
  }

  const xtreemfs::pbrpc::Replica& replica = xlocs.replicas(replica_index);
  if (replica.osd_uuids_size() == 0) {
    Logging::log->getLog(LEVEL_ERROR)
        << "GetOSDUUIDFromXlocSet: No head OSD available in XlocSet:"
        <<  xlocs.DebugString() << std::endl;
    return "";
  }

  return replica.osd_uuids(stripe_index);
}

std::string GetOSDUUIDFromXlocSet(
    const xtreemfs::pbrpc::XLocSet& xlocs) {
  // Get the UUID for the first replica (r=0) and the head OSD (i.e. the first
  // stripe, s=0).
  return GetOSDUUIDFromXlocSet(xlocs, 0, 0);
}

/**
 * By default this function does read random data from /dev/urandom and falls
 * back to using C's rand() if /dev/random is not available.
 */
void GenerateVersion4UUID(std::string* result) {
  FILE *urandom = fopen("/dev/urandom", "r");
  if (!urandom) {
    srand(time(NULL));  // Use rand() instead if /dev/urandom not available.
  }

  // Base62 characters for UUID generation.
  char set[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

  uint32_t block_length[] = {8, 4, 4, 4, 12};
  int block_length_count = 5;
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
        "Generated client UUID:" << uuid << endl;
  }

  if (urandom) {
    fclose(urandom);
  }
}

void InitializeStat(xtreemfs::pbrpc::Stat* stat) {
  stat->set_dev(0);
  stat->set_ino(0);
  stat->set_mode(0);
  // If not set to 1, a assertion in the metadata cache get triggered.
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
  boost::uint64_t lock1_end = lock1.length() == 0 ? 0 :
      lock1.offset() + lock1.length();
  boost::uint64_t lock2_end = lock2.length() == 0 ? 0 :
        lock2.offset() + lock2.length();

  // Check for overlaps.
  if (lock1_end == 0) {
    if (lock2_end >= lock1.offset() || lock2_end == 0) {
      return true;
    }
  }
  if (lock2_end == 0) {
    if (lock1_end >= lock2.offset() || lock1_end == 0) {
      return true;
    }
  }
  // Overlapping?
  if (!(lock1_end < lock2.offset() || lock1.offset() > lock2_end)) {
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
    boost::int64_t integer = boost::lexical_cast<boost::int64_t>(string);
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

}  // namespace xtreemfs
