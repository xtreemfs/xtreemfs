/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_HELPER_H_
#define CPP_INCLUDE_LIBXTREEMFS_HELPER_H_

#include <stdint.h>

#include <boost/unordered_set.hpp>
#include <string>

#include "xtreemfs/GlobalTypes.pb.h"

#include <libxtreemfs/execute_sync_request.h>

#ifdef __linux__
#include <ifaddrs.h>
#endif  // __linux__

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

/** The global file id  contains the Volume UUID and File ID concatenated by a ":". */
uint64_t ExtractFileIdFromGlobalFileId(std::string global_file_id);

/** The XCap contains the global file id. */
uint64_t ExtractFileIdFromXCap(const xtreemfs::pbrpc::XCap& xcap);

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

/** Convert StripePolicyType to string */
std::string StripePolicyTypeToString(xtreemfs::pbrpc::StripingPolicyType policy);

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

/** Adapter to create RPCOptions from an Options object */
RPCOptions RPCOptionsFromOptions(const Options& options);

#ifdef __APPLE__
/** Returns the MacOSX Kernel Version (8 = Tiger, 9 = Leopard, 10 = Snow Leopard). */
int GetMacOSXKernelVersion();
#endif  // __APPLE__

#ifdef WIN32
/** Convert a Windows Multibyte string (e.g. a path or username) into
 *  an UTF8 string and returns it.
 */
std::string ConvertWindowsToUTF8(const wchar_t* windows_string);

/** Convert a Windows Multibyte string (e.g. a path or username) into
 *  an UTF8 string and stores it in utf8_string.
 */
void ConvertWindowsToUTF8(const wchar_t* windows_string,
                          std::string* utf8_string);

/** Convert an UTF8 string (e.g. a path or username) into
 *  a Windows Multibyte string.
 *
 * @param buffer_size Size, including the null character.
 */
void ConvertUTF8ToWindows(const std::string& utf8,
                          wchar_t* buf,
                          int buffer_size);

void ConvertUTF8ToWindows(const std::string& utf8, std::wstring* win);

std::wstring ConvertUTF8ToWindows(const std::string& utf8);

#endif  // WIN32

/** Returns the set of available networks (for each local network interface).
 *
 *  Each entry has the form "<network address>/<prefix length>".
 *
 *  Currently, only Linux is supported. For other OS, the list is empty.
 */
boost::unordered_set<std::string> GetNetworks();

/** Returns for the "struct ifaddrs" the network prefix (e.g. 127.0.0.1/8).
 *
 * @throws XtreemFSException if the conversion fails.
 */
#ifdef __linux__
std::string GetNetworkStringUnix(const struct ifaddrs* ifaddr);
#endif  // __linux__

/**
 *  Parses human-readable byte numbers to byte counts
 */
long parseByteNumber(std::string byte_number);

/**
 *  Logs the error_msg and then throws a XtreemFSException
 */
void LogAndThrowXtreemFSException(std::string error_msg);

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_HELPER_H_
