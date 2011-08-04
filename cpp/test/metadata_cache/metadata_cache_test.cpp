/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <boost/cstdint.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/scoped_ptr.hpp>
#include <string>

#include "libxtreemfs/metadata_cache.h"
#include "libxtreemfs/helper.h"
#include "util/logging.h"
#include "xtreemfs/MRC.pb.h"

using namespace std;
using namespace xtreemfs;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

class MetadataCacheTestSize2 : public ::testing::Test {
 protected:
  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);

    metadata_cache_ = new MetadataCache(2, 3600);  // Max 2 entries, 1 hour.
  }

  virtual void TearDown() {
    delete metadata_cache_;

    google::protobuf::ShutdownProtobufLibrary();

    shutdown_logger();
  }

  MetadataCache* metadata_cache_;
};

class MetadataCacheTestSize1024 : public ::testing::Test {
 protected:
  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);

    metadata_cache_ = new MetadataCache(1024, 3600);  // Max 1k entries, 1 hour.
  }

  virtual void TearDown() {
    delete metadata_cache_;

    google::protobuf::ShutdownProtobufLibrary();

    shutdown_logger();
  }

  MetadataCache* metadata_cache_;
};

/** If a Stat entry gets updated through UpdateStatTime(), the new timeout must
 *  be respected in case of an eviction. */
TEST_F(MetadataCacheTestSize2, UpdateStatTimeKeepsSequentialTimeoutOrder) {
  Stat a, b, c;
  InitializeStat(&a);
  InitializeStat(&b);
  InitializeStat(&c);
  a.set_ino(0);
  b.set_ino(1);
  c.set_ino(2);

  metadata_cache_->UpdateStat("/a", a);
  metadata_cache_->UpdateStat("/b", b);
  // Cache is full now. a would be first item to get evicted.
  metadata_cache_->UpdateStatTime("/a", 0, SETATTR_MTIME);
  // "b" should be the oldest Stat element now and get evicted.
  metadata_cache_->UpdateStat("/c", c);
  // Was "a" found or did "b" survive?
  EXPECT_TRUE(metadata_cache_->GetStat("/a", &a));
  EXPECT_EQ(0, a.ino());
  // "c" is also still there.
  EXPECT_TRUE(metadata_cache_->GetStat("/c", &c));
  EXPECT_EQ(2, c.ino());
}

/** If a Stat entry gets updated through UpdateStat(), the new timeout must be
 *  respected in case of an eviction. */
TEST_F(MetadataCacheTestSize2, UpdateStatKeepsSequentialTimeoutOrder) {
  Stat a, b, c;
  InitializeStat(&a);
  InitializeStat(&b);
  InitializeStat(&c);
  a.set_ino(0);
  b.set_ino(1);
  c.set_ino(2);

  metadata_cache_->UpdateStat("/a", a);
  metadata_cache_->UpdateStat("/b", b);
  // Cache is full now. a would be first item to get evicted.
  metadata_cache_->UpdateStat("/a", a);
  // "b" should be the oldest Stat element now and get evicted.
  metadata_cache_->UpdateStat("/c", c);
  // Was "a" found or did "b" survive?
  EXPECT_TRUE(metadata_cache_->GetStat("/a", &a));
  EXPECT_EQ(0, a.ino());
  // "c" is also still there.
  EXPECT_TRUE(metadata_cache_->GetStat("/c", &c));
  EXPECT_EQ(2, c.ino());
}

/** Test if Size is updated correctly after UpdateStat() or Invalidate(). */
TEST_F(MetadataCacheTestSize2, CheckSizeAfterUpdateAndInvalidate) {
  Stat a, b, c;
  InitializeStat(&a);
  InitializeStat(&b);
  InitializeStat(&c);
  EXPECT_EQ(0, metadata_cache_->Size());
  metadata_cache_->UpdateStat("/a", a);
  EXPECT_EQ(1, metadata_cache_->Size());
  metadata_cache_->UpdateStat("/b", b);
  EXPECT_EQ(2, metadata_cache_->Size());
  metadata_cache_->UpdateStat("/c", b);
  // Cache has only room for two entries.
  EXPECT_EQ(2, metadata_cache_->Size());

  metadata_cache_->Invalidate("/b");
  EXPECT_EQ(1, metadata_cache_->Size());
  metadata_cache_->Invalidate("/c");
  EXPECT_EQ(0, metadata_cache_->Size());
}

/** Test (Get|Update)DirEntries methods. */
TEST_F(MetadataCacheTestSize2, UpdateAndGetDirEntries) {
  DirectoryEntries dir_entries;
  int chunk_size = 1024;
  int entry_count = chunk_size;
  string dir = "/";

  // Fill dir_entries;
  for (int i = 0; i < entry_count; i++) {
    Stat a;
    a.set_ino(i);
    dir_entries.add_entries();
    dir_entries.mutable_entries(dir_entries.entries_size()-1)
        ->mutable_stbuf()->CopyFrom(a);
    string path_to_stat = dir + boost::lexical_cast<std::string>(i);
    dir_entries.mutable_entries(dir_entries.entries_size()-1)
        ->set_name(path_to_stat);
  }
  metadata_cache_->UpdateDirEntries(dir, dir_entries);

  boost::scoped_ptr<DirectoryEntries> dir_entries_read;

  // Read all entries.
  dir_entries_read.reset(metadata_cache_->GetDirEntries(dir, 0, entry_count));
  EXPECT_EQ(entry_count, dir_entries_read->entries_size());
  for (int i = 0; i < dir_entries_read->entries_size(); i++) {
    string path_to_stat = dir + boost::lexical_cast<std::string>(i);
    EXPECT_EQ(i, dir_entries_read->entries(i).stbuf().ino());
    EXPECT_EQ(path_to_stat, dir_entries_read->entries(i).name());
  }

  // Read a subset.
  dir_entries_read.reset(metadata_cache_
      ->GetDirEntries(dir, entry_count/2, entry_count/2-1));
  EXPECT_EQ(entry_count/2-1, dir_entries_read->entries_size());
  int offset = entry_count/2;
  for (int i = 0; i < dir_entries_read->entries_size(); i++) {
    string path_to_stat = dir + boost::lexical_cast<std::string>(offset);
    EXPECT_EQ(path_to_stat, dir_entries_read->entries(i).name());
    EXPECT_EQ(offset, dir_entries_read->entries(i).stbuf().ino());
    offset++;
  }
}

/** If a Stat entry gets updated through UpdateStat(), the new timeout must be
 *  respected in case of an eviction. */
TEST_F(MetadataCacheTestSize1024, InvalidatePrefix) {
  Stat a, b, c, d;
  InitializeStat(&a);
  InitializeStat(&b);
  InitializeStat(&c);
  InitializeStat(&d);
  a.set_ino(0);
  b.set_ino(1);
  c.set_ino(2);
  d.set_ino(3);

  metadata_cache_->UpdateStat("/dir", a);
  metadata_cache_->UpdateStat("/dir/file1", b);
  metadata_cache_->UpdateStat("/dir.file1", c);
  metadata_cache_->UpdateStat("/dirZfile1", d);

  metadata_cache_->InvalidatePrefix("/dir");

  // Invalidate of all matching entries successful?
  EXPECT_FALSE(metadata_cache_->GetStat("/dir", &a));
  EXPECT_FALSE(metadata_cache_->GetStat("/dir/file1", &b));

  // Similiar entries which do not match the prefix "/dir/" have not been
  // invalidated.
  EXPECT_TRUE(metadata_cache_->GetStat("/dir.file1", &c));
  EXPECT_EQ(2, c.ino());
  EXPECT_TRUE(metadata_cache_->GetStat("/dirZfile1", &d));
  EXPECT_EQ(3, d.ino());
}

/** If a Stat entry gets updated through UpdateStat(), the new timeout must be
 *  respected in case of an eviction. */
TEST_F(MetadataCacheTestSize1024, RenamePrefix) {
  Stat a, b, c, d;
  InitializeStat(&a);
  InitializeStat(&b);
  InitializeStat(&c);
  InitializeStat(&d);
  a.set_ino(0);
  b.set_ino(1);
  c.set_ino(2);
  d.set_ino(3);

  metadata_cache_->UpdateStat("/dir", a);
  metadata_cache_->UpdateStat("/dir/file1", b);
  metadata_cache_->UpdateStat("/dir.file1", c);
  metadata_cache_->UpdateStat("/dirZfile1", d);
  EXPECT_EQ(4, metadata_cache_->Size());

  metadata_cache_->RenamePrefix("/dir", "/newdir");
  EXPECT_EQ(4, metadata_cache_->Size());

  // Rename of all matching entries successful?
  EXPECT_TRUE(metadata_cache_->GetStat("/newdir", &a));
  EXPECT_EQ(0, a.ino());
  EXPECT_TRUE(metadata_cache_->GetStat("/newdir/file1", &b));
  EXPECT_EQ(1, b.ino());

  // Similiar entries which do not match the prefix "/dir/" have not been
  // renamed.
  EXPECT_TRUE(metadata_cache_->GetStat("/dir.file1", &c));
  EXPECT_EQ(2, c.ino());
  EXPECT_TRUE(metadata_cache_->GetStat("/dirZfile1", &d));
  EXPECT_EQ(3, d.ino());
}

/** Are large nanoseconds values correctly updated by
 *  UpdateStatAttributes? */
TEST_F(MetadataCacheTestSize1024, UpdateStatAttributes) {
  string path = "/file";
  Stat stat, update_stat;
  InitializeStat(&stat);
  InitializeStat(&update_stat);
  stat.set_ino(0);
  update_stat.set_ino(1);

  metadata_cache_->UpdateStat(path, stat);
  EXPECT_EQ(1, metadata_cache_->Size());
  EXPECT_TRUE(metadata_cache_->GetStat(path, &stat));
  EXPECT_EQ(0, stat.ino());
  EXPECT_EQ(0, stat.mtime_ns());

  boost::uint64_t time = 1234567890;
  time *= 1000000000;
  update_stat.set_atime_ns(time);
  update_stat.set_mtime_ns(time);
  metadata_cache_->UpdateStatAttributes(
      path,
      update_stat,
      static_cast<Setattrs>(SETATTR_ATIME | SETATTR_MTIME));
  EXPECT_EQ(1, metadata_cache_->Size());
  EXPECT_TRUE(metadata_cache_->GetStat(path, &stat));
  EXPECT_EQ(0, stat.ino());
  EXPECT_EQ(time, stat.atime_ns());
  EXPECT_EQ(time, stat.mtime_ns());
}

/** Ideas:
 *
 * test TTL expiration.
 * test invalidateprefix.
 *
 * test correct setting of maximum ttl for every operation
 *
 */
