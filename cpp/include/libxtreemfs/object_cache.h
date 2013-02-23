/*
 * Copyright (c) 2013 by Felix Hupfeld.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_OBJECT_CACHE_H_
#define CPP_INCLUDE_LIBXTREEMFS_OBJECT_CACHE_H_

#include <stdint.h>

#include <boost/scoped_array.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/function.hpp>
#include <deque>
#include <map>

// TODO(fhupfeld): define annotations for clang and move them to a separate file
#define GUARDED_BY(x)
#define EXCLUSIVE_LOCKS_REQUIRED(x)
#define LOCKS_EXCLUDED(x)

namespace boost {
class condition_variable;
}

namespace xtreemfs {

/** These are injected functions that provide object read and write
  * functionality for complete objects. */
typedef boost::function<int (int object_no, char* data)>
    ObjectReaderFunction;
typedef boost::function<void (int object_no, const char* data, int size)>
    ObjectWriterFunction;

class CachedObject {
 public:
  /** Create the object in ReadPending state. */
  CachedObject(int object_no, int object_size);
  ~CachedObject();

  /** Flush data and free memory. */
  void FlushAndErase(const ObjectWriterFunction& writer)
      LOCKS_EXCLUDED(mutex_);

  /** Free memory without flushing to storage. */
  void Drop() LOCKS_EXCLUDED(mutex_);

  int Read(int offset_in_object,
           char* buffer,
           int bytes_to_read,
           const ObjectReaderFunction& reader)
      LOCKS_EXCLUDED(mutex_);

  void Write(int offset_in_object,
             const char* buffer,
             int bytes_to_write,
             const ObjectReaderFunction& reader)
      LOCKS_EXCLUDED(mutex_);

  void Flush(const ObjectWriterFunction& writer)
      LOCKS_EXCLUDED(mutex_);

  void Truncate(int new_object_size)
      LOCKS_EXCLUDED(mutex_);

  uint64_t last_access()
      LOCKS_EXCLUDED(mutex_);

  bool is_dirty()
      LOCKS_EXCLUDED(mutex_);

 private:
  /** Caller must hold mutex_ */
  void DropLocked() EXCLUSIVE_LOCKS_REQUIRED(mutex_);
  void ReadInternal(boost::unique_lock<boost::mutex>& lock,
                    const ObjectReaderFunction& reader)
     EXCLUSIVE_LOCKS_REQUIRED(mutex_);

  void WriteObjectToOSD(const ObjectWriterFunction& writer)
     EXCLUSIVE_LOCKS_REQUIRED(mutex_);

  /** Mutex that protects all non const data member. */
  boost::mutex mutex_;
  std::deque<boost::condition_variable*> read_queue_
      GUARDED_BY(mutex_);

  const int object_no_;
  const int object_size_;
  /** Our buffer, always object_size_ large. */
  boost::scoped_array<char> data_ GUARDED_BY(mutex_);
  /** The last object has fewer bytes than object_size_. If data has not been
      fetched from the OSD yet, actual_size is -1.
  */
  int actual_size_ GUARDED_BY(mutex_);
  /** Data is dirty and must be written back. */
  bool is_dirty_ GUARDED_BY(mutex_);
  /** Timestamp of last access, for LRU expunge policy. */
  uint64_t last_access_ GUARDED_BY(mutex_);
};

class ObjectCache {
 public:
  ObjectCache(size_t max_objects, int object_size);
  ~ObjectCache();

  /** Read within a specific object */
  int Read(int object_no, int offset_in_object,
           char* buffer, int bytes_to_read,
           const ObjectReaderFunction& reader,
           const ObjectWriterFunction& writer)
      LOCKS_EXCLUDED(mutex_);

  /** Write within a specific object */
  void Write(int object_no, int offset_in_object,
             const char* buffer, int bytes_to_write,
             const ObjectReaderFunction& reader,
             const ObjectWriterFunction& writer)
      LOCKS_EXCLUDED(mutex_);

  void Flush(const ObjectWriterFunction& writer)
      LOCKS_EXCLUDED(mutex_);

  void Truncate(int64_t new_size)
      LOCKS_EXCLUDED(mutex_);

  int object_size() const;

 private:
  CachedObject* LookupObject(int object_no,
                             const ObjectWriterFunction& writer);
      LOCKS_EXCLUDED(mutex_);

  void EvictObjects(const ObjectWriterFunction& writer)
      EXCLUSIVE_LOCKS_REQUIRED(mutex_);

  /** Protects all non-const members of this class. */
  boost::mutex mutex_;
  /** Map of object number to cached object. */
  typedef std::map<uint64_t, CachedObject*> Cache;
  Cache cache_ GUARDED_BY(mutex_);
  /** Maximum number of objects to cache. */
  const size_t max_objects_;
  const int object_size_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_OBJECT_CACHE_H_
