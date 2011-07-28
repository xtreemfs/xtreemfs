/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_HELPER_H_
#define CPP_INCLUDE_LIBXTREEMFS_HELPER_H_

#include <boost/cstdint.hpp>
#include <string>

namespace xtreemfs {

namespace pbrpc {
class Lock;
class OSDWriteResponse;
class Stat;
class XCap;
class XLocSet;
}  // namespace pbrpc

/** Returns -1, 0 or 1 if "new_response" is less than, equal or greater than
 *  "current_response" in "XtreemFS terms".
 *
 *  Those terms are:
 *  - two responses are equal if their truncate_epoch and file size are equal.
 *  - a response is greater if a) its truncate epoch is higher OR
 *                             b) if both truncate epochs are equal and its
 *                                file size is higher. */
int CompareOSDWriteResponses(
    const xtreemfs::pbrpc::OSDWriteResponse* new_response,
    const xtreemfs::pbrpc::OSDWriteResponse* current_response);

boost::uint64_t ExtractFileIdFromXCap(const xtreemfs::pbrpc::XCap& xcap);

/** Same as dirname(): Returns the path to the parent directory of a path. */
std::string ResolveParentDirectory(const std::string& path);

/** Same as basename(): Returns the last component of a path. */
std::string GetBasename(const std::string& path);

/** Concatenates a given directory and file and returns the correct full path to
 *  the file. */
std::string ConcatenatePath(const std::string& directory,
                            const std::string& file);

/** Returns the OSD UUID for the given replica and the given block within the
 * striping pattern.
 *
 * @param xlocs         List of replicas.
 * @param replica_index Index of the replica in the XlocSet (starting from 0).
 * @param stripe_index  Index of the OSD in the striping pattern (where 0 is the
 *                      head OSD).
 * @return returns string("") if there is no OSD available.
 */
std::string GetOSDUUIDFromXlocSet(const xtreemfs::pbrpc::XLocSet& xlocs,
                                  uint32_t replica_index,
                                  uint32_t stripe_index);

/** Returns UUID of the head OSD (block = 0) of the first replica (r = 0). */
std::string GetOSDUUIDFromXlocSet(const xtreemfs::pbrpc::XLocSet& xlocs);

/** Generates a random UUID (needed to distinguish clients for locks). */
void GenerateVersion4UUID(std::string* result);

/** Sets all required members of a Stat object to 0 or "". */
void InitializeStat(xtreemfs::pbrpc::Stat* stat);

/** Returns true if both locks aren't NULL and all members are identical. */
bool CheckIfLocksAreEqual(const xtreemfs::pbrpc::Lock& lock1,
                          const xtreemfs::pbrpc::Lock& lock2);

/** Returns true if lock2 conflicts with lock1. */
bool CheckIfLocksDoConflict(const xtreemfs::pbrpc::Lock& lock1,
                            const xtreemfs::pbrpc::Lock& lock2);

/** Tests if string is a numeric (positive) value. */
bool CheckIfUnsignedInteger(const std::string& string);

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_HELPER_H_
