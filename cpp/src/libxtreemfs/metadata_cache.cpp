/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/metadata_cache.h"

#include "util/logging.h"
#include "xtreemfs/OSD.pb.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

/** MetadataCache for Stat, listxattrResponse and XAttr objects per path.
 * \file
 *
 * This MetaDataCache implementation extensively uses boost::multi_index instead
 * of writing an own combination of map-, hash- and list-like indexes.
 *
 * Depending on the called function, different indexes are used to access the
 * cache and the complexity varies. In general the total complexity is the sum
 * of the complexity of every's index insert/update/delete function.
 *
 * Get* functions:    They use the hash-like index.
 *                    Complexity: O(1) in general.
 * Update* functions: They use the map-like index to find an existing entry,
 *                    erase it and insert it again.
 *                    Complexity: O(log n)
 * Invalidate:        Complexity: O(1) in general (using the hash-like index).
 * InvalidatePrefix:  Complexity: O(log n) (searches for the first occurrence of
 *                    the prefix and deletes all following affected entries).
 *
 * @note index.replace() cannot get used in the Update* functions to update an
 * existing entry because the timestamp order in the list-like index will not be
 * updated. The list-like index is used to determine the oldest element (at the
 * front of the list). Therefore an updated entry has to get removed and
 * inserted at the front again. This is realized by re-inserting the entry into
 * the map-like index (the new entry is automatically added to the front of the
 * list-like index). However, the insert into the map-like index is quite time
 * consuming (even though using hinted insertion).
 *
 * @note Benchmark results of this Implementation (for 1024*1024 entries):
 * - There is almost no difference between using the hash or map index in the
 *   Update* functions (<=1ms).
 * - Hinted insertion only slightly reduces the runtime (<=1ms).
 * - Using the map instead of the hash index to find an element does
 *   significantly increase the run time.
 * - (While benchmarking subsequent requests for the same element have to be
 *   avoided due to caching effects.)
 */

namespace xtreemfs {

MetadataCache::MetadataCache(boost::uint64_t size, boost::uint64_t ttl_s)
    : size_(size), ttl_s_(ttl_s) {
  enabled = size > 0 ? true : false;
}

MetadataCache::~MetadataCache() {
  // Free all objects.
  boost::mutex::scoped_lock lock(mutex_);

  by_list& index = cache_.get<IndexList>();
  for (by_list::iterator it_list = index.begin();
       it_list != index.end(); ++it_list) {
    delete *it_list;
  }
}


void MetadataCache::Invalidate(const std::string& path) {
  if (path.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  by_hash& index = cache_.get<IndexHash>();
  by_hash::iterator it_hash = index.find(path);
  if (it_hash != index.end()) {
    // Free MetadataCacheEntry object.
    delete *it_hash;
    index.erase(it_hash);
  }
}


void MetadataCache::InvalidatePrefix(const std::string& path) {
  if (path.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  by_map& index = cache_.get<IndexMap>();
  by_map::iterator it_map = index.find(path);
  if (it_map != index.end()) {
    // Free MetadataCacheEntry object.
    delete *it_map;
    it_map = index.erase(it_map);
  }

  // Clean any possible cached contents of the directory "path".
  const std::string prefix = path + "/";
  // Here it's not possible to reuse it_map as there may be additional entries
  // between path and path+"/" (for instance path+".").
  it_map = index.lower_bound(prefix);
  while (it_map != index.end()) {
    MetadataCacheEntry* cached_entry = *it_map;
    if (cached_entry->path.find(prefix) != 0) {
      break;
    }
    delete *it_map;
    it_map = index.erase(it_map);
  }
}

void MetadataCache::RenamePrefix(const std::string& path,
                                 const std::string& new_path) {
  if (path.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  by_map& index = cache_.get<IndexMap>();
  by_map::iterator it_map = index.find(path);
  if (it_map != index.end()) {
    MetadataCacheEntry* cached_entry = *it_map;
    cached_entry->path = new_path;
    it_map = index.erase(it_map);
    index.insert(cached_entry);
  }

  // Clean any possible cached contents of the directory "path".
  const std::string prefix = path + "/";
  const std::string prefix_new = new_path + "/";
  it_map = index.lower_bound(prefix);
  while (it_map != index.end()) {
    MetadataCacheEntry* cached_entry = *it_map;
    if (cached_entry->path.find(prefix) != 0) {
      break;
    }
    // Change prefix.
    cached_entry->path.replace(0, prefix.length(), prefix_new);
    it_map = index.erase(it_map);
    index.insert(cached_entry);
  }
}


bool MetadataCache::GetStat(const std::string& path,
                            xtreemfs::pbrpc::Stat* stat) {
  if (path.empty() || !enabled) {
    return false;
  }

  boost::mutex::scoped_lock lock(mutex_);

  by_hash& index = cache_.get<IndexHash>();
  by_hash::iterator it_hash = index.find(path);
  if (it_hash != index.end()) {
    MetadataCacheEntry* cache_entry = *it_hash;
    // We must never have cached a hard link.
    assert(cache_entry->stat == NULL || cache_entry->stat->nlink() == 1);
    // Entry found for path, check timeout of Stat value.
    boost::uint64_t current_time_s = time(NULL);
    if (cache_entry->stat_timeout_s >= current_time_s) {
      if (cache_entry->stat != NULL) {
        stat->CopyFrom(*(cache_entry->stat));
        return true;
      } else {
        return false;
      }
    } else {
      // Expired => remove from cache.
      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
            << "MetadataCache GetStat expired: " << path << endl;
      }
      // Only delete object, if the maximum timeout of all three objects is
      // reached.
      if (cache_entry->timeout_s < current_time_s) {
        // Free MetadataCacheEntry and delete from Index. This increases the
        // run time of GetStat() roughly by factor 3.
        delete *it_hash;
        index.erase(it_hash);
      }
    }
  } else {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
        << "MetadataCache GetStat miss: " << path << " ["
        << cache_.size() << "]" << endl;
    }
  }

  return false;
}

void MetadataCache::UpdateStat(const std::string& path,
                               const xtreemfs::pbrpc::Stat& stat) {
  if (path.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  MetadataCacheEntry* cache_entry = NULL;
  // Check if there's already an Entry for path.
  by_map& index = cache_.get<IndexMap>();
  by_map::iterator it_map = index.find(path);
  if (it_map != index.end()) {
    cache_entry = *it_map;
  } else {
    // Create new entry
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "metadata cache: registering " << path << endl;
    }

    cache_entry = new MetadataCacheEntry();
    cache_entry->path = path;
  }

  if (cache_entry->stat == NULL) {
    cache_entry->stat = new Stat;
  }
  cache_entry->stat->CopyFrom(stat);
  cache_entry->stat_timeout_s = time(NULL) + ttl_s_;
  cache_entry->timeout_s = cache_entry->stat_timeout_s;

  if (it_map != index.end()) {
    // Replace existing entry.
    it_map = index.erase(it_map);
    index.insert(it_map, cache_entry);
  } else {
    EvictUnmutexed(1);
    index.insert(cache_entry);
  }
}

// TODO(mberlin): Also update the stat entry in the direntry of the parent dir.
void MetadataCache::UpdateStatTime(const std::string& path,
                                   boost::uint64_t timestamp_s,
                                   xtreemfs::pbrpc::Setattrs to_set) {
  if (path.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  by_map& index = cache_.get<IndexMap>();
  by_map::iterator it_map = index.find(path);
  if (it_map != index.end()) {
    MetadataCacheEntry* cache_entry = *it_map;
    Stat* cached_stat = cache_entry->stat;
    if (cached_stat == NULL) {
      return;
    }
    uint64_t time_ns = timestamp_s * 1000000000;
    if ((to_set &  SETATTR_ATIME)
        && time_ns > cached_stat->atime_ns()) {
      cached_stat->set_atime_ns(time_ns);
    }
    if ((to_set &  SETATTR_MTIME)
        && time_ns > cached_stat->mtime_ns()) {
      cached_stat->set_mtime_ns(time_ns);
    }
    if ((to_set &  SETATTR_CTIME)
        && time_ns > cached_stat->ctime_ns()) {
      cached_stat->set_ctime_ns(time_ns);
    }
    cache_entry->stat_timeout_s = time(NULL) + ttl_s_;
    cache_entry->timeout_s = cache_entry->stat_timeout_s;
    it_map = index.erase(it_map);
    index.insert(it_map, cache_entry);
  }
}

// TODO(mberlin): Also update the stat entry in the direntry of the parent dir.
void MetadataCache::UpdateStatAttributes(const std::string& path,
                                         const xtreemfs::pbrpc::Stat& stat,
                                         xtreemfs::pbrpc::Setattrs to_set) {
  if (path.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  by_map& index = cache_.get<IndexMap>();
  by_map::iterator it_map = index.find(path);
  if (it_map != index.end()) {
    MetadataCacheEntry* cache_entry = *it_map;
    Stat* cached_stat = cache_entry->stat;
    if (cached_stat == NULL) {
      return;
    }

    if ((to_set &  SETATTR_ATTRIBUTES)) {
      cached_stat->set_attributes(stat.attributes());
    }
    if ((to_set &  SETATTR_MODE)) {
      cached_stat->set_mode(stat.mode());
    }
    if ((to_set &  SETATTR_UID)) {
      cached_stat->set_user_id(stat.user_id());
    }
    if ((to_set &  SETATTR_GID)) {
      cached_stat->set_group_id(stat.group_id());
    }
    if ((to_set &  SETATTR_SIZE)) {
      if (stat.has_truncate_epoch() &&
          stat.truncate_epoch() > cached_stat->truncate_epoch()) {
        cached_stat->set_size(stat.size());
        cached_stat->set_truncate_epoch(stat.truncate_epoch());
      } else if (stat.has_truncate_epoch() &&
                 stat.truncate_epoch() == cached_stat->truncate_epoch() &&
                 stat.size() > cached_stat->size()) {
        cached_stat->set_size(stat.size());
      }
      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
            << "MetadataCache UpdateStatAttributes SETATTR_SIZE: new size: "
            << cached_stat->size() << " truncate epoch: "
            << cached_stat->truncate_epoch() << endl;
      }
    }
    if ((to_set &  SETATTR_ATIME)) {
      cached_stat->set_atime_ns(stat.atime_ns());
    }
    if ((to_set &  SETATTR_MTIME)) {
      cached_stat->set_mtime_ns(stat.mtime_ns());
    }
    if ((to_set &  SETATTR_CTIME)) {
      cached_stat->set_ctime_ns(stat.ctime_ns());
    }

    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "MetadataCache UpdateStatAttributes: " << path << " to_set: "
          << to_set << endl;
    }

    cache_entry->stat_timeout_s = time(NULL) + ttl_s_;
    cache_entry->timeout_s = cache_entry->stat_timeout_s;
    it_map = index.erase(it_map);
    index.insert(it_map, cache_entry);
  }
}

// TODO(mberlin): Also update the stat entry in the direntry of the parent dir.
void MetadataCache::UpdateStatFromOSDWriteResponse(
    const std::string& path,
    const xtreemfs::pbrpc::OSDWriteResponse& response) {
  if (path.empty()
      || !enabled
      || !response.has_size_in_bytes()
      || !response.has_truncate_epoch()) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  by_map& index = cache_.get<IndexMap>();
  by_map::iterator it_map = index.find(path);
  if (it_map != index.end()) {
    MetadataCacheEntry* cache_entry = *it_map;
    Stat* cached_stat = cache_entry->stat;
    if (cached_stat != NULL) {
      if (response.truncate_epoch() > cached_stat->truncate_epoch() ||
          (response.truncate_epoch() == cached_stat->truncate_epoch() &&
           response.size_in_bytes() > cached_stat->size())) {
        cached_stat->set_size(response.size_in_bytes());
        cached_stat->set_truncate_epoch(response.truncate_epoch());
      }
    }
  }
}

void MetadataCache::InvalidateStat(const std::string& path) {
  if (path.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  by_hash& index = cache_.get<IndexHash>();
  by_hash::iterator it_hash = index.find(path);
  if (it_hash != index.end()) {
    delete (*it_hash)->stat;
    (*it_hash)->stat = NULL;
  }
}

xtreemfs::pbrpc::DirectoryEntries* MetadataCache::GetDirEntries(
    const std::string& path,
    boost::uint64_t offset,
    boost::uint32_t count) {
  boost::mutex::scoped_lock lock(mutex_);

  by_hash& index = cache_.get<IndexHash>();
  by_hash::iterator it_hash = index.find(path);
  if (it_hash != index.end()) {
    // Entry found for path, check timeout of DirectoryEntries value.
    MetadataCacheEntry* cache_entry = *it_hash;
    boost::uint64_t current_time_s = time(NULL);
    if (cache_entry->dir_entries != NULL) {
      if (cache_entry->dir_entries_timeout_s >= current_time_s) {
        DirectoryEntries* cached_dentries = cache_entry->dir_entries;
        DirectoryEntries* result = new DirectoryEntries;

        // Copy all entries from cache.
        if (offset == 0 && count >= cached_dentries->entries_size()) {
          if (Logging::log->loggingActive(LEVEL_DEBUG)) {
            Logging::log->getLog(LEVEL_DEBUG)
              << "MetadataCache GetDirEntries hit: " << path << " ["
              << cache_.size() << "]" << endl;
          }
          result->CopyFrom(*cached_dentries);
        } else {
          // Copy only selected entries from cache.
          if (Logging::log->loggingActive(LEVEL_DEBUG)) {
            Logging::log->getLog(LEVEL_DEBUG)
              << "MetadataCache GetDirEntries hit (partial copy): " << path
              << " [" << cache_.size() << "] offset: " << offset
              << "count: " << count << endl;
          }
          for (boost::uint64_t i = offset; i < offset + count; i++) {
            result->add_entries()->CopyFrom(cached_dentries->entries(i));
          }
        }
        return result;
      } else {
        // Expired => remove from cache.
        if (Logging::log->loggingActive(LEVEL_DEBUG)) {
          Logging::log->getLog(LEVEL_DEBUG)
              << "MetadataCache GetDirEntries expired: " << path << endl;
        }
        // Only delete object, if the maximum timeout is reached.
        if (cache_entry->timeout_s < current_time_s) {
          delete *it_hash;
          index.erase(it_hash);
        }
        return NULL;
      }
    }
  }

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
      << "MetadataCache GetDirEntries miss: " << path << " ["
      << cache_.size() << "]" << endl;
  }

  return NULL;
}

void MetadataCache::UpdateDirEntries(
    const std::string& path,
    const xtreemfs::pbrpc::DirectoryEntries& dir_entries) {
  if (path.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  MetadataCacheEntry* cache_entry = NULL;
  // Check if there's already an Entry for path.
  by_map& index = cache_.get<IndexMap>();
  by_map::iterator it_map = index.find(path);
  if (it_map != index.end()) {
    cache_entry = *it_map;
  } else {
  // Create new entry
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
       << "MetadataCache UpdateDirEntries: new CacheEntry " << path << endl;
  }

    cache_entry = new MetadataCacheEntry();
    cache_entry->path = path;
  }

  if (cache_entry->dir_entries == NULL) {
    cache_entry->dir_entries = new DirectoryEntries;
  }
  cache_entry->dir_entries->CopyFrom(dir_entries);
  cache_entry->dir_entries_timeout_s = time(NULL) + ttl_s_;
  cache_entry->timeout_s = cache_entry->dir_entries_timeout_s;

  if (it_map != index.end()) {
    // Replace existing entry.
    it_map = index.erase(it_map);
    index.insert(it_map, cache_entry);
  } else {
    EvictUnmutexed(1);
    index.insert(cache_entry);
  }
}

void MetadataCache::InvalidateDirEntry(const std::string& path_to_directory,
                                       const std::string& entry_name) {
  if (path_to_directory.empty() || entry_name.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  by_hash& index = cache_.get<IndexHash>();
  by_hash::iterator it_hash = index.find(path_to_directory);
  if (it_hash != index.end()) {
    DirectoryEntries* cached_dentries = (*it_hash)->dir_entries;
    if (cached_dentries == NULL) {
      return;
    }

    // Copy new DirectoryEntries without entry "name".
    DirectoryEntries* new_dentries = new DirectoryEntries;
    for (int i = 0; i < cached_dentries->entries_size(); i++) {
      if (cached_dentries->entries(i).name() != entry_name) {
        new_dentries->add_entries()->CopyFrom(cached_dentries->entries(i));
      }
    }
    delete (*it_hash)->dir_entries;
    (*it_hash)->dir_entries = new_dentries;
  }
}

void MetadataCache::InvalidateDirEntries(const std::string& path) {
  if (path.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  by_hash& index = cache_.get<IndexHash>();
  by_hash::iterator it_hash = index.find(path);
  if (it_hash != index.end()) {
    delete (*it_hash)->dir_entries;
    (*it_hash)->dir_entries = NULL;
  }
}

bool MetadataCache::GetXAttr(const std::string& path, const std::string& name,
                             std::string* value, bool* xattrs_cached) {
  assert(xattrs_cached != NULL);
  boost::mutex::scoped_lock lock(mutex_);

  *xattrs_cached = false;
  by_hash& index = cache_.get<IndexHash>();
  by_hash::iterator it_hash = index.find(path);
  if (it_hash != index.end()) {
    // Entry found for path, check timeout of listxattrResponse value.
    MetadataCacheEntry* cache_entry = *it_hash;
    boost::uint64_t current_time_s = time(NULL);
    if (cache_entry->xattrs != NULL) {
      if (cache_entry->xattrs_timeout_s >= current_time_s) {
        *xattrs_cached = true;
        listxattrResponse* cached_xattrs = cache_entry->xattrs;

        for (int i = 0; i < cached_xattrs->xattrs_size(); i++) {
          if (cached_xattrs->xattrs(i).name() == name) {
            if (Logging::log->loggingActive(LEVEL_DEBUG)) {
              Logging::log->getLog(LEVEL_DEBUG)
                << "MetadataCache GetXAttr hit: " << path << " ["
                << cache_.size() << "]" << endl;
            }
            *value = cached_xattrs->xattrs(i).value();
            break;
          }
        }
        return true;
      } else {
        // Expired => remove from cache.
        if (Logging::log->loggingActive(LEVEL_DEBUG)) {
          Logging::log->getLog(LEVEL_DEBUG)
              << "MetadataCache GetXAttr expired: " << path << endl;
        }
        // Only delete object, if the maximum timeout is reached.
        if (cache_entry->timeout_s < current_time_s) {
          delete *it_hash;
          index.erase(it_hash);
        }
        return false;
      }
    }
  }

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
      << "MetadataCache GetXAttr miss: " << path << " ["
      << cache_.size() << "]" << endl;
  }

  return false;
}

bool MetadataCache::GetXAttrSize(const std::string& path,
                                 const std::string& name,
                                 int* size,
                                 bool* xattrs_cached) {
  assert(xattrs_cached != NULL);
  boost::mutex::scoped_lock lock(mutex_);

  *xattrs_cached = false;
  by_hash& index = cache_.get<IndexHash>();
  by_hash::iterator it_hash = index.find(path);
  if (it_hash != index.end()) {
    // Entry found for path, check timeout of listxattrResponse value.
    MetadataCacheEntry* cache_entry = *it_hash;
    boost::uint64_t current_time_s = time(NULL);
    if (cache_entry->xattrs != NULL) {
      if (cache_entry->xattrs_timeout_s >= current_time_s) {
        *xattrs_cached = true;
        listxattrResponse* cached_xattrs = cache_entry->xattrs;

        for (int i = 0; i < cached_xattrs->xattrs_size(); i++) {
          if (cached_xattrs->xattrs(i).name() == name) {
            if (Logging::log->loggingActive(LEVEL_DEBUG)) {
              Logging::log->getLog(LEVEL_DEBUG)
                << "MetadataCache GetXAttrSize hit: " << path << " ["
                << cache_.size() << "]" << endl;
            }
            *size = cached_xattrs->xattrs(i).value().size();
            return true;
          }
        }
        return false;
      } else {
        // Expired => remove from cache.
        if (Logging::log->loggingActive(LEVEL_DEBUG)) {
          Logging::log->getLog(LEVEL_DEBUG)
              << "MetadataCache GetXAttrSize expired: " << path << endl;
        }
        // Only delete object, if the maximum timeout is reached.
        if (cache_entry->timeout_s < current_time_s) {
          delete *it_hash;
          index.erase(it_hash);
        }
        return false;
      }
    }
  }

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
      << "MetadataCache GetXAttrSize miss: " << path << " ["
      << cache_.size() << "]" << endl;
  }

  return false;
}

xtreemfs::pbrpc::listxattrResponse* MetadataCache::GetXAttrs(
    const std::string& path) {
  boost::mutex::scoped_lock lock(mutex_);

  by_hash& index = cache_.get<IndexHash>();
  by_hash::iterator it_hash = index.find(path);
  if (it_hash != index.end()) {
    // Entry found for path, check timeout of listxattrResponse value.
    MetadataCacheEntry* cache_entry = *it_hash;
    boost::uint64_t current_time_s = time(NULL);
    if (cache_entry->xattrs != NULL) {
      if (cache_entry->xattrs_timeout_s >= current_time_s) {
        // Create copy of object.
        if (Logging::log->loggingActive(LEVEL_DEBUG)) {
          Logging::log->getLog(LEVEL_DEBUG)
            << "MetadataCache GetXAttrs hit: " << path << " ["
            << cache_.size() << "]" << endl;
        }

        listxattrResponse* result = new listxattrResponse(*cache_entry->xattrs);
        return result;
      } else {
        // Expired => remove from cache.
        if (Logging::log->loggingActive(LEVEL_DEBUG)) {
          Logging::log->getLog(LEVEL_DEBUG)
              << "MetadataCache GetXAttrs expired: " << path << endl;
        }
        // Only delete object, if the maximum timeout is reached.
        if (cache_entry->timeout_s < current_time_s) {
          delete *it_hash;
          index.erase(it_hash);
        }
        return NULL;
      }
    }
  }

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
      << "MetadataCache GetXAttrs miss: " << path << " ["
      << cache_.size() << "]" << endl;
  }

  return NULL;
}

void MetadataCache::UpdateXAttr(
    const std::string& path,
    const std::string& name,
    const std::string& value) {
  if (path.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  MetadataCacheEntry* cache_entry = NULL;
  // Check if there's already an Entry for path.
  by_map& index = cache_.get<IndexMap>();
  by_map::iterator it_map = index.find(path);
  if (it_map != index.end()) {
    cache_entry = *it_map;
  } else {
    // Don't create a new entry with an incomplete xattr list.
    return;
  }

  if (cache_entry->xattrs == NULL) {
    // Don't create a new xattr list for an existing cache entry.
    return;
  }
  if (cache_entry->xattrs_timeout_s < time(NULL)) {
    return;  // Do not update expired xattrs.
  }


  // Find "name" and update it
  bool name_found = false;
  for (int i = 0; i < cache_entry->xattrs->xattrs_size(); i++) {
    if (cache_entry->xattrs->xattrs(i).name() == name) {
      cache_entry->xattrs->mutable_xattrs(i)->set_value(value);
      name_found = true;
      break;
    }
  }

  if (!name_found) {
    cache_entry->xattrs->add_xattrs();
    cache_entry->xattrs->mutable_xattrs(cache_entry->xattrs->xattrs_size() - 1)
        ->set_name(name);
    cache_entry->xattrs->mutable_xattrs(cache_entry->xattrs->xattrs_size() - 1)
        ->set_value(value);
  }

  // Replace existing entry - do not update TTL.
  index.replace(it_map, cache_entry);
}

void MetadataCache::UpdateXAttrs(
    const std::string& path,
    const xtreemfs::pbrpc::listxattrResponse& xattrs) {
  if (path.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  MetadataCacheEntry* cache_entry = NULL;
  // Check if there's already an Entry for path.
  by_map& index = cache_.get<IndexMap>();
  by_map::iterator it_map = index.find(path);
  if (it_map != index.end()) {
    cache_entry = *it_map;
  } else {
    // Create new entry
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "MetadataCache UpdateXAttrs: new CacheEntry " << path << endl;
    }

    cache_entry = new MetadataCacheEntry();
    cache_entry->path = path;
  }

  if (cache_entry->xattrs == NULL) {
    cache_entry->xattrs = new listxattrResponse;
  }
  cache_entry->xattrs->CopyFrom(xattrs);
  cache_entry->xattrs_timeout_s = time(NULL) + ttl_s_;
  cache_entry->timeout_s = cache_entry->xattrs_timeout_s;

  if (it_map != index.end()) {
    // Replace existing entry.
    it_map = index.erase(it_map);
    index.insert(it_map, cache_entry);
  } else {
    EvictUnmutexed(1);
    index.insert(cache_entry);
  }
}

void MetadataCache::InvalidateXAttr(const std::string& path,
                                    const std::string& name) {
  if (path.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  MetadataCacheEntry* cache_entry = NULL;
  // Check if there's already an Entry for path.
  by_map& index = cache_.get<IndexMap>();
  by_map::iterator it_map = index.find(path);
  if (it_map != index.end()) {
    cache_entry = *it_map;
  } else {
    // Don't create a new entry with an incomplete xattr list.
    return;
  }

  if (cache_entry->xattrs == NULL) {
    // Don't create a new xattr list for an existing cache entry.
    return;
  }
  if (cache_entry->xattrs_timeout_s < time(NULL)) {
    return;  // Do not update expired xattrs.
  }

  // Copy new xattr without entry "name".
  listxattrResponse* new_xattrs = new listxattrResponse;
  for (int i = 0; i < cache_entry->xattrs->xattrs_size(); i++) {
    if (cache_entry->xattrs->xattrs(i).name() != name) {
      new_xattrs->add_xattrs()->CopyFrom(cache_entry->xattrs->xattrs(i));
    }
  }
  delete cache_entry->xattrs;
  cache_entry->xattrs = new_xattrs;
}

void MetadataCache::InvalidateXAttrs(const std::string& path) {
  if (path.empty() || !enabled) {
    return;
  }

  boost::mutex::scoped_lock lock(mutex_);

  by_hash& index = cache_.get<IndexHash>();
  by_hash::iterator it_hash = index.find(path);
  if (it_hash != index.end()) {
    delete (*it_hash)->xattrs;
    (*it_hash)->xattrs = NULL;
  }
}

boost::uint64_t MetadataCache::Size() {
  boost::mutex::scoped_lock lock(mutex_);
  return cache_.size();
}

void MetadataCache::EvictUnmutexed(int n) {
  // Evict one entry from cache if it's full.
  while (cache_.size() > size_ - n) {
    // remove cache entry
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "MetadataCache EvictUnmutexed: Deleting at least " << n
          << " entries from " << cache_.size() << " entries in total." << endl;
    }
    by_list& index = cache_.get<IndexList>();
    by_list::iterator it_list = index.begin();
    delete *it_list;
    cache_.erase(it_list);
  }
}

}  // namespace xtreemfs
