/*
 * Copyright (c) 2010-2011 by Patrick Schaefer, Zuse Institute Berlin
 *                    2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
#ifndef CPP_INCLUDE_LIBXTREEMFS_METADATA_CACHE_H_
#define CPP_INCLUDE_LIBXTREEMFS_METADATA_CACHE_H_

#include <boost/multi_index_container.hpp>
#include <boost/multi_index/hashed_index.hpp>
#include <boost/multi_index/ordered_index.hpp>
#include <boost/multi_index/member.hpp>
#include <boost/multi_index/sequenced_index.hpp>
#include <boost/thread/mutex.hpp>
#include <string>

#include "libxtreemfs/metadata_cache_entry.h"
#include "xtreemfs/MRC.pb.h"

namespace xtreemfs {

namespace pbrpc {
class OSDWriteResponse;
}

class MetadataCache {
 public:
  // Tags needed to address the different indexes.
  struct IndexList {};
  struct IndexMap {};
  struct IndexHash {};

  typedef boost::multi_index_container<
    MetadataCacheEntry*,
    boost::multi_index::indexed_by<
        // list-like: Order
        boost::multi_index::sequenced<
            boost::multi_index::tag<IndexList> >,
        // map-like: Sort entries by path for InvalidatePrefix().
        boost::multi_index::ordered_unique<
            boost::multi_index::tag<IndexMap>,
            boost::multi_index::member<
                MetadataCacheEntry,
                std::string,
                &MetadataCacheEntry::path> >,
        // unordered_map-like: Hash based access for fast Get* calls.
        boost::multi_index::hashed_non_unique<
            boost::multi_index::tag<IndexHash>,
            boost::multi_index::member<
                MetadataCacheEntry,
                std::string,
                &MetadataCacheEntry::path> >
    >
  > Cache;

  typedef Cache::index<IndexList>::type by_list;
  typedef Cache::index<IndexMap>::type by_map;
  typedef Cache::index<IndexHash>::type by_hash;

  MetadataCache(boost::uint64_t size, boost::uint64_t ttl_s);

  /** Frees all MetadataCacheEntry objects. */
  ~MetadataCache();

  /** Removes MetadataCacheEntry for path from cache_. */
  void Invalidate(const std::string& path);

  /** Removes MetadataCacheEntry for path and any objects matching path+"/". */
  void InvalidatePrefix(const std::string& path);

  /** Renames path to new_path and any object's path matching path+"/". */
  void RenamePrefix(const std::string& path, const std::string& new_path);

  /** Returns true if there is a Stat object for path in cache and fills stat.*/
  bool GetStat(const std::string& path, xtreemfs::pbrpc::Stat* stat);

  /** Stores/updates stat in cache for path. */
  void UpdateStat(const std::string& path, const xtreemfs::pbrpc::Stat& stat);

  /** Updates timestamp of the cached stat object.
   * Values for to_set: SETATTR_ATIME, SETATTR_MTIME, SETATTR_CTIME
   */
  void UpdateStatTime(const std::string& path,
                      boost::uint64_t timestamp,
                      xtreemfs::pbrpc::Setattrs to_set);

  /** Updates the attributes given in "stat" and selected by "to_set". */
  void UpdateStatAttributes(const std::string& path,
                            const xtreemfs::pbrpc::Stat& stat,
                            xtreemfs::pbrpc::Setattrs to_set);

  /** Updates file size and truncate epoch from an OSDWriteResponse. */
  void UpdateStatFromOSDWriteResponse(
      const std::string& path,
      const xtreemfs::pbrpc::OSDWriteResponse& response);

  /** Returns a DirectoryEntries object (if it's found for "path") limited to
   *  entries starting from "offset" up to "count" (or the maximum)S.
   *
   * @remark Ownership is transferred to the caller.
   */
  xtreemfs::pbrpc::DirectoryEntries* GetDirEntries(const std::string& path,
                     boost::uint64_t offset,
                     boost::uint32_t count);

  /** Invalidates the stat entry stored for "path". */
  void InvalidateStat(const std::string& path);

  /** Stores/updates DirectoryEntries in cache for path.
   *
   * @note  This implementation assumes that dir_entries is always complete,
   *        i.e. it must be guaranteed that it contains all entries.*/
  void UpdateDirEntries(const std::string& path,
                        const xtreemfs::pbrpc::DirectoryEntries& dir_entries);

  /** Removes "entry_name" from the cached directory "path_to_directory". */
  void InvalidateDirEntry(const std::string& path_to_directory,
                          const std::string& entry_name);

  /** Remove cached DirectoryEntries in cache for path. */
  void InvalidateDirEntries(const std::string& path);

  /** Writes value for an XAttribute with "name" stored for "path" in "value".
   *  Returns true if found, false otherwise. */
  bool GetXAttr(const std::string& path, const std::string& name,
                std::string* value, bool* xattrs_cached);

  /** Stores the size of a value (string length) of an XAttribute "name" cached
   *  for "path" in "size". */
  bool GetXAttrSize(const std::string& path, const std::string& name,
                    int* size, bool* xattrs_cached);

  /** Get all extended attributes cached for "path".
   *
   * @remark Ownership is transferred to the caller.
   */
  xtreemfs::pbrpc::listxattrResponse* GetXAttrs(const std::string& path);

  /** Updates the "value" for the attribute "name" of "path" if the list of
   *  attributes for "path" is already cached.
   *
   *  @remark   This function does not extend the TTL of the xattr list. */
  void UpdateXAttr(const std::string& path, const std::string& name,
                   const std::string& value);

  /** Stores/updates XAttrs in cache for path.
   *
   * @note  This implementation assumes that the list of extended attributes is
   *        always complete.*/
  void UpdateXAttrs(const std::string& path,
                    const xtreemfs::pbrpc::listxattrResponse& xattrs);

  /** Removes "name" from the list of extended attributes cached for "path". */
  void InvalidateXAttr(const std::string& path, const std::string& name);

  /** Remove cached XAttrs in cache for path. */
  void InvalidateXAttrs(const std::string& path);

  /** Returns the current number of elements. */
  boost::uint64_t Size();

  /** Returns the maximum number of elements. */
  boost::uint64_t Capacity() { return size_; }

 private:
  /** Evicts first n oldest entries from cache_. */
  void EvictUnmutexed(int n);

  bool enabled;

  boost::uint64_t size_;

  boost::uint64_t ttl_s_;

  boost::mutex mutex_;

  Cache cache_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_METADATA_CACHE_H_
