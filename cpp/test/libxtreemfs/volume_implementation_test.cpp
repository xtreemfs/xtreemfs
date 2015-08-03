/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <boost/scoped_ptr.hpp>
#include <boost/thread/thread.hpp>
#include <cstdlib>
#include <cstdio>
#include <cctype>
#include <string>

#include "libxtreemfs/client.h"
#include "libxtreemfs/file_info.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/file_handle_implementation.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/volume_implementation.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "util/logging.h"
#include "xtreemfs/MRC.pb.h"
#include "xtreemfs/OSD.pb.h"

#ifdef WIN32
#include <windows.h>  // For GetTickCount
#define snprintf _snprintf
#endif  // WIN32

/** Assumes a running XtreemFS installation listing to localhost at the default
 *  ports.
 *
 * @file
 */

using namespace std;
using namespace xtreemfs;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {

std::string RandomVolumeName(const int length) {
#ifdef WIN32
  srand(GetTickCount());
#else
  struct timeval time;
  gettimeofday(&time, NULL);
  srand((time.tv_sec * 1000) + (time.tv_usec / 1000));
#endif  // WIN32

  std::string result = "volume_implementation_test_";
  char c;
  for (int i = 1; i <= length; i++) {
    while (!std::isalnum(c = static_cast<char>(std::rand() % 256))) {} // NOLINT
    result += c;
  }
  return result;
}

class VolumeImplementationTest : public ::testing::Test {
 public:
  void CheckFileSize(const std::string& path, size_t size) {
    Stat stat;
    volume_->GetAttr(user_credentials_, path, &stat);
    EXPECT_EQ(size, stat.size());
  }

 protected:
  virtual void SetUp() {
#ifdef __linux__
    const char* dir_url_env = getenv("XTREEMFS_DIR_URL");
    if (dir_url_env) {
      dir_url_.xtreemfs_url = string(dir_url_env);
    }
    const char* mrc_url_env = getenv("XTREEMFS_MRC_URL");
    if (mrc_url_env) {
      mrc_url_.xtreemfs_url = string(mrc_url_env);
    }
#endif  // __linux__

    if (dir_url_.xtreemfs_url.empty()) {
      dir_url_.xtreemfs_url = "pbrpc://localhost:32638/";
    }
    if (mrc_url_.xtreemfs_url.empty()) {
      mrc_url_.xtreemfs_url = "pbrpc://localhost:32636/";
    }

    dir_url_.ParseURL(kDIR);
    mrc_url_.ParseURL(kMRC);

    auth_.set_auth_type(AUTH_NONE);

    // Every operation is executed in the context of a given user and his groups
    // The UserCredentials object does store this information.
    user_credentials_.set_username("volume_implementation_test");
    user_credentials_.add_groups("volume_implementation_test");

    // Create a new instance of a client using the DIR service at 'localhost'
    // at port 32638 using the default implementation.
    options_.log_level_string = "WARN";
    client_ = Client::CreateClient(
        dir_url_.service_addresses,
        user_credentials_,
        dir_url_.GenerateSSLOptions(),
        options_);

    // Start the client (a connection to the DIR service will be setup).
    client_->Start();

    // Create volume with a random name.
    volume_name_ = RandomVolumeName(5);
    client_->CreateVolume(mrc_url_.service_addresses,
                          auth_,
                          user_credentials_,
                          volume_name_);

    // Mount volume.
    volume_ = client_->OpenVolume(volume_name_, NULL, options_);
  }

  virtual void TearDown() {
    volume_->Close();

    client_->DeleteVolume(mrc_url_.service_addresses,
                          auth_,
                          user_credentials_,
                          volume_name_);

    client_->Shutdown();
    delete client_;
  }

  xtreemfs::Client* client_;
  xtreemfs::Options options_;
  /** Only used to store and parse the DIR URL. */
  xtreemfs::Options dir_url_;
  /** Only used to store and parse the MRC URL. */
  xtreemfs::Options mrc_url_;
  xtreemfs::Volume* volume_;
  std::string volume_name_;

  xtreemfs::pbrpc::Auth auth_;
  xtreemfs::pbrpc::UserCredentials user_credentials_;
};

class VolumeImplementationTestFastPeriodicFileSizeUpdate
  : public VolumeImplementationTest {
 protected:
  virtual void SetUp() {
    options_.periodic_file_size_updates_interval_s = 1;
    options_.log_level_string = "DEBUG";
    VolumeImplementationTest::SetUp();
  }
};

class VolumeImplementationTestFastPeriodicXCapRenewal
  : public VolumeImplementationTest {
 protected:
  virtual void SetUp() {
    options_.periodic_xcap_renewal_interval_s = 1;
    options_.log_level_string = "DEBUG";
    VolumeImplementationTest::SetUp();
  }
};

class VolumeImplementationTestNoMetadataCache
  : public VolumeImplementationTest {
 protected:
  virtual void SetUp() {
    options_.metadata_cache_size = 0;
    options_.log_level_string = "DEBUG";
    VolumeImplementationTest::SetUp();
  }
};

/** Creates 1023 files in the root directory and tries to read the directory.
 *  Additionally two more files will be created and two more readdir's called.*/
TEST_F(VolumeImplementationTest, ReadDirChunkSize) {
  int readdir_chunk_size = 1024;
  int files_count = readdir_chunk_size - 1;
  string dir = "/";

  options_.readdir_chunk_size = readdir_chunk_size;

  for (int i = 0; i < files_count; i++) {
    // Create files.
    char name[9];
    snprintf(name, sizeof(name), "%08d", i);
    string path_to_file = dir + name;
    ASSERT_NO_THROW({
      FileHandle* file_handle = volume_->OpenFile(user_credentials_,
                                                  path_to_file,
                                                  SYSTEM_V_FCNTL_H_O_CREAT);
      file_handle->Close();
    });
  }

  boost::scoped_ptr<DirectoryEntries> dir_entries;
  // Read directory and check for files.
  ASSERT_NO_THROW({
    dir_entries.reset(volume_->ReadDir(user_credentials_, dir, 0, -1, false));
  });
  EXPECT_EQ(files_count + 2, dir_entries->entries_size());  // +2 due to . & ..
  for (int i = 0; i < (dir_entries->entries_size() - 2); i++) {
    char name[9];
    snprintf(name, sizeof(name), "%08d", i);
    EXPECT_EQ(string(name), dir_entries->entries(i + 2).name());
  }

  // Create another file (the 1024th).
  ASSERT_NO_THROW({
    char name[9];
    snprintf(name, sizeof(name), "%08d", files_count);
    string path_to_file = dir + name;
    FileHandle* file_handle = volume_->OpenFile(user_credentials_,
                                                path_to_file,
                                                SYSTEM_V_FCNTL_H_O_CREAT);
    file_handle->Close();
  });
  files_count++;

  // Read directory and check for files.
  ASSERT_NO_THROW({
    dir_entries.reset(volume_->ReadDir(user_credentials_, dir, 0, -1, false));
  });
  EXPECT_EQ(files_count + 2, dir_entries->entries_size());  // +2 due to . & ..
  for (int i = 0; i < (dir_entries->entries_size() - 2); i++) {
    char name[9];
    snprintf(name, sizeof(name), "%08d", i);
    EXPECT_EQ(string(name), dir_entries->entries(i + 2).name());
  }


  // Create another file (the 1025th).
  ASSERT_NO_THROW({
    char name[9];
    snprintf(name, sizeof(name), "%08d", files_count);
    string path_to_file = dir + name;
    FileHandle* file_handle = volume_->OpenFile(user_credentials_,
                                                path_to_file,
                                                SYSTEM_V_FCNTL_H_O_CREAT);
    file_handle->Close();
  });
  files_count++;

  // Read directory and check for files.
  ASSERT_NO_THROW({
    dir_entries.reset(volume_->ReadDir(user_credentials_, dir, 0, -1, false));
  });
  EXPECT_EQ(files_count + 2, dir_entries->entries_size());  // +2 due to . & ..
  for (int i = 0; i < (dir_entries->entries_size() - 2); i++) {
    char name[9];
    snprintf(name, sizeof(name), "%08d", i);
    EXPECT_EQ(string(name), dir_entries->entries(i + 2).name());
  }
}

/** Test if the file size is successfully sent to the MRC. */
TEST_F(VolumeImplementationTestNoMetadataCache,
       FilesizeUpdateAfterClose) {
  string path_to_file = "test";

  ASSERT_NO_THROW({
    FileHandle* file_handle = volume_->OpenFile(user_credentials_,
                                                path_to_file,
                                                SYSTEM_V_FCNTL_H_O_CREAT);
    Stat stat;

    // File has a size of 0 bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(0, stat.size());

    const char* buf = "a";
    EXPECT_EQ(strlen(buf), file_handle->Write(buf, strlen(buf), 0));
    file_handle->Close();

    // File has a size of 1 bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(strlen(buf), stat.size());

    volume_->Unlink(user_credentials_, path_to_file);
  });
}

/** File sizes updates are written back in background while files are open. */
TEST_F(VolumeImplementationTestFastPeriodicFileSizeUpdate,
       WorkingPendingFileSizeUpdates) {
  string path_to_file = "test";

  ASSERT_NO_THROW({
    FileHandle* file_handle = volume_->OpenFile(
        user_credentials_,
        path_to_file,
        static_cast<SYSTEM_V_FCNTL>(
            SYSTEM_V_FCNTL_H_O_CREAT | SYSTEM_V_FCNTL_H_O_SYNC));
    Stat stat;

    // File has a size of 0 bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(0, stat.size());

    const char* buf = "a";
    EXPECT_EQ(strlen(buf), file_handle->Write(buf, strlen(buf), 0));
    // The resulting OSDWriteResponse has not been written back yet (kDirty)
    // or the periodic thread did already pick it up and and initiated a
    // file size update (kDirtyAndAsyncPending).
    // kClean == kDirty || kDirtyAndAsyncPending
    EXPECT_NE(kClean, static_cast<FileHandleImplementation*>(file_handle)
        ->file_info_->osd_write_response_status_);
    // Wait for the periodic file size update thread.
    boost::this_thread::sleep(boost::posix_time::seconds(2));
    EXPECT_EQ(kClean, static_cast<FileHandleImplementation*>(file_handle)
        ->file_info_->osd_write_response_status_);
    file_handle->Close();

    // File has a size of 1 bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(strlen(buf), stat.size());

    volume_->Unlink(user_credentials_, path_to_file);
  });
}

/** XCaps of open files are periodically renewed in the background. */
TEST_F(VolumeImplementationTestFastPeriodicXCapRenewal,
       WorkingXCapRenewal) {
  string path_to_file = "test";

  ASSERT_NO_THROW({
    FileHandle* file_handle = volume_->OpenFile(user_credentials_,
                                                path_to_file,
                                                SYSTEM_V_FCNTL_H_O_CREAT);

    // Wait for the periodic xcap renewal thread.
    xtreemfs::pbrpc::XCap xcap_one;
    static_cast<FileHandleImplementation*>(file_handle)->GetXCap(&xcap_one);
    boost::this_thread::sleep(boost::posix_time::seconds(2));
    xtreemfs::pbrpc::XCap xcap_two;
    static_cast<FileHandleImplementation*>(file_handle)->GetXCap(&xcap_two);
    EXPECT_LT(xcap_one.expire_time_s(), xcap_two.expire_time_s());

    // Cleanup.
    file_handle->Close();
    volume_->Unlink(user_credentials_, path_to_file);
  });
}

/** File sizes updates are written back after Flush(). */
TEST_F(VolumeImplementationTest, FileSizeUpdateAfterFlush) {
  string path_to_file = "test";

  ASSERT_NO_THROW({
    FileHandle* file_handle = volume_->OpenFile(
        user_credentials_,
        path_to_file,
        static_cast<SYSTEM_V_FCNTL>(
            SYSTEM_V_FCNTL_H_O_CREAT | SYSTEM_V_FCNTL_H_O_SYNC));
    Stat stat;

    // File has a size of 0 bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(0, stat.size());

    const char* buf = "a";
    EXPECT_EQ(strlen(buf), file_handle->Write(buf, strlen(buf), 0));
    // The resulting OSDWriteResponse has not been written back yet (kDirty)
    // or the periodic thread did already pick it up and and initiated a
    // file size update (kDirtyAndAsyncPending).
    // kClean == kDirty || kDirtyAndAsyncPending
    EXPECT_NE(kClean, static_cast<FileHandleImplementation*>(file_handle)
        ->file_info_->osd_write_response_status_);
    // Flush has to block until the file size update has been written back.
    file_handle->Flush();
    EXPECT_EQ(kClean, static_cast<FileHandleImplementation*>(file_handle)
        ->file_info_->osd_write_response_status_);
    file_handle->Close();

    // File has a size of 1 bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(strlen(buf), stat.size());

    volume_->Unlink(user_credentials_, path_to_file);
  });
}

/** File sizes updates are written back after Flush(). */
TEST_F(VolumeImplementationTestFastPeriodicFileSizeUpdate,
       FileSizeUpdateAfterFlushWaitsForPendingUpdates) {
  string path_to_file = "test";

  ASSERT_NO_THROW({
    FileHandle* file_handle = volume_->OpenFile(
        user_credentials_,
        path_to_file,
        static_cast<SYSTEM_V_FCNTL>(
            SYSTEM_V_FCNTL_H_O_CREAT | SYSTEM_V_FCNTL_H_O_SYNC));
    Stat stat;

    // File has a size of 0 bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(0, stat.size());

    const char* buf = "a";
    EXPECT_EQ(strlen(buf), file_handle->Write(buf, strlen(buf), 0));
    // The resulting OSDWriteResponse has not been written back yet (kDirty)
    // or the periodic thread did already pick it up and and initiated a
    // file size update (kDirtyAndAsyncPending).
    // kClean == kDirty || kDirtyAndAsyncPending
    EXPECT_NE(kClean, static_cast<FileHandleImplementation*>(file_handle)
        ->file_info_->osd_write_response_status_);
    while (static_cast<FileHandleImplementation*>(file_handle)
               ->file_info_->osd_write_response_status_ == kDirty) {
      boost::posix_time::milliseconds wait_time(1);
      boost::this_thread::sleep(wait_time);
    }
    file_handle->Flush();
    EXPECT_EQ(kClean, static_cast<FileHandleImplementation*>(file_handle)
        ->file_info_->osd_write_response_status_);
    file_handle->Close();

    // File has a size of 1 bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(strlen(buf), stat.size());

    volume_->Unlink(user_credentials_, path_to_file);
  });
}

/** GetAttr() calls for an open file, which has an unwritten file size update
 *  pending in its corresponding FileInfo object, return the correct file size.
 */
TEST_F(VolumeImplementationTest, GetAttrMergesPendingFileSizeUpdates) {
  string path_to_file = "test";

  ASSERT_NO_THROW({
    FileHandle* file_handle = volume_->OpenFile(user_credentials_,
                                                path_to_file,
                                                SYSTEM_V_FCNTL_H_O_CREAT);
    Stat stat;

    // File has a size of 0 bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(0, stat.size());

    const char* buf = "a";
    EXPECT_EQ(strlen(buf), file_handle->Write(buf, strlen(buf), 0));
    // File has a size of 1 bytes (file size update was not written back yet as
    // the thread does wake up only 60 seconds by default).
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(strlen(buf), stat.size());

    file_handle->Close();

    volume_->Unlink(user_credentials_, path_to_file);
  });
}

/** After writing and closing a file, the new file size has to be written back
 *  to the stat cache. */
TEST_F(VolumeImplementationTestFastPeriodicFileSizeUpdate,
       StatCacheCorrectlyUpdatedAfterFileSizeUpdate) {
  string path_to_file = "test";

  ASSERT_NO_THROW({
    FileHandle* file_handle = volume_->OpenFile(user_credentials_,
                                                path_to_file,
                                                SYSTEM_V_FCNTL_H_O_CREAT);
    Stat stat;

    // File has a size of 0 bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(0, stat.size());

    const char* buf = "a";
    EXPECT_EQ(strlen(buf), file_handle->Write(buf, strlen(buf), 0));
    file_handle->Close();

    // File has a size of 1 bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(strlen(buf), stat.size());

    volume_->Unlink(user_credentials_, path_to_file);
  });
}

/** After writing and closing a file, the new file size has to be written back
 *  to the stat cache, even if the file was renamed while it was open. */
TEST_F(VolumeImplementationTest,
       StatCacheCorrectlyUpdatedAfterRenameWriteAndClose) {
  string old_path = "/test";
  string new_path = "/test2";

  ASSERT_NO_THROW({
    FileHandle* file_handle = volume_->OpenFile(user_credentials_,
                                                old_path,
                                                SYSTEM_V_FCNTL_H_O_CREAT);
    Stat stat;

    // File has a size of 0 bytes.
    volume_->GetAttr(user_credentials_, old_path, &stat);
    EXPECT_EQ(0, stat.size());
    EXPECT_EQ(1, static_cast<VolumeImplementation*>(volume_)->
        metadata_cache_.Size());
    EXPECT_EQ(MetadataCache::kStatCached,
              static_cast<VolumeImplementation*>(volume_)->metadata_cache_.
                  GetStat(old_path, &stat));

    volume_->Rename(user_credentials_, old_path, new_path);
    EXPECT_EQ(1, static_cast<VolumeImplementation*>(volume_)->
        metadata_cache_.Size());
    EXPECT_EQ(MetadataCache::kStatCached,
        static_cast<VolumeImplementation*>(volume_)->metadata_cache_.
            GetStat(new_path, &stat));

    const char* buf = "a";
    EXPECT_EQ(strlen(buf), file_handle->Write(buf, strlen(buf), 0));
    file_handle->Close();

    // File has a size of 1 bytes.
    volume_->GetAttr(user_credentials_, new_path, &stat);
    EXPECT_EQ(strlen(buf), stat.size());

    volume_->Unlink(user_credentials_, new_path);
  });
}

/** Test advisory locking of files. */
TEST_F(VolumeImplementationTest, FilesLocking) {
  string path = "/test";

  FileHandle* file_handle;
  FileHandle* file_handle2;
  boost::scoped_ptr<Lock> lock1;
  boost::scoped_ptr<Lock> lock2;
  boost::scoped_ptr<Lock> lock3;
  boost::scoped_ptr<Lock> lock4;

  ASSERT_NO_THROW({
    // Open first file.
    file_handle = volume_->OpenFile(user_credentials_,
                                    path,
                                    SYSTEM_V_FCNTL_H_O_CREAT,
                                    448);  // octal 700.
    // Lock file for writing.
    lock1.reset(file_handle->AcquireLock(1,  // PID.
                                         0,
                                         1,  // Range in bytes: [0, 0]
                                         true,
                                         false));
    // Open second file.
    file_handle2 = volume_->OpenFile(user_credentials_,
                                     path,
                                     SYSTEM_V_FCNTL_H_O_RDWR);
  });

  // CheckLock should indicate the previously acquired lock.
  ASSERT_NO_THROW({
    lock2.reset(file_handle2->CheckLock(2,  // PID.
                                        0,
                                        0,  // Range in bytes: [0, EOF]
                                        false));
    EXPECT_EQ(1, lock2->client_pid());
    EXPECT_EQ(0, lock2->offset());
    EXPECT_EQ(1, lock2->length());
    EXPECT_TRUE(lock2->exclusive());
  });

  // The previous lock may be overridden by the same process.
  ASSERT_NO_THROW({
    lock1.reset(file_handle->CheckLock(1,  // PID.
                                       0,
                                       2,  // Range in bytes: [0, 1]
                                       true));
    EXPECT_EQ(1, lock1->client_pid());
    EXPECT_EQ(0, lock1->offset());
    EXPECT_EQ(2, lock1->length());
    EXPECT_TRUE(lock1->exclusive());
  });

  // AcquireLock of another process must fail.
  EXPECT_THROW({
    lock2.reset(file_handle2->AcquireLock(2,  // PID.
                                          0,
                                          2,
                                          true,
                                          false));
  }, PosixErrorException);

  // Acquiring a non-conflicting lock must succeed.
  ASSERT_NO_THROW({
    lock2.reset(file_handle2->AcquireLock(2,  // PID.
                                          2,
                                          2,  // Range in bytes: [2, 3]
                                          false,
                                          false));
    EXPECT_EQ(2, lock2->client_pid());
    EXPECT_EQ(2, lock2->offset());
    EXPECT_EQ(2, lock2->length());
    EXPECT_FALSE(lock2->exclusive());
  });

  // Read locks can be acquired on the same range.
  ASSERT_NO_THROW({
    lock3.reset(file_handle->AcquireLock(3,  // PID.
                                         4,
                                         4,  // Range in bytes: [4, 7]
                                         false,
                                         false));
    EXPECT_EQ(3, lock3->client_pid());
    EXPECT_EQ(4, lock3->offset());
    EXPECT_EQ(4, lock3->length());
    EXPECT_FALSE(lock3->exclusive());
    lock4.reset(file_handle2->AcquireLock(4,  // PID.
                                          5,
                                          2,  // Range in bytes: [5, 6]
                                          false,
                                          false));
    EXPECT_EQ(4, lock4->client_pid());
    EXPECT_EQ(5, lock4->offset());
    EXPECT_EQ(2, lock4->length());
    EXPECT_FALSE(lock4->exclusive());
  });

  // Cleanup.
  ASSERT_NO_THROW({
    file_handle2->Close();
    file_handle->Close();
    volume_->Unlink(user_credentials_, path);
  });
}

/** Releasing a lock which does not exist does not throw. */
TEST_F(VolumeImplementationTest, FilesLockingReleaseNonExistantLock) {
  string path = "/test";

  FileHandle* file_handle;
  Lock lock;

  ASSERT_NO_THROW({
    // Open file.
    file_handle = volume_->OpenFile(user_credentials_,
                                    path,
                                    SYSTEM_V_FCNTL_H_O_CREAT,
                                    448);  // octal 700.
    EXPECT_EQ(0, static_cast<FileHandleImplementation*>(file_handle)
        ->file_info_->active_locks_.size());
    file_handle->ReleaseLock(23,
                             0,
                             0,
                             true);
    EXPECT_EQ(0, static_cast<FileHandleImplementation*>(file_handle)
            ->file_info_->active_locks_.size());
    file_handle->Close();
    volume_->Unlink(user_credentials_, path);
  });
}

/** Releasing a lock which does exist does not throw. */
TEST_F(VolumeImplementationTest, FilesLockingReleaseExistantLock) {
  string path = "/test";

  FileHandle* file_handle;
  boost::scoped_ptr<Lock> lock;

  ASSERT_NO_THROW({
    // Open file.
    file_handle = volume_->OpenFile(user_credentials_,
                                    path,
                                    SYSTEM_V_FCNTL_H_O_CREAT,
                                    448);  // octal 700.
    EXPECT_EQ(0, static_cast<FileHandleImplementation*>(file_handle)
        ->file_info_->active_locks_.size());

    lock.reset(file_handle->AcquireLock(1,  // PID.
                                        0,
                                        1,  // Range in bytes: [0, 1]
                                        true,
                                        false));
    EXPECT_EQ(1, static_cast<FileHandleImplementation*>(file_handle)
        ->file_info_->active_locks_.size());

    file_handle->ReleaseLock(1,
                             0,
                             0,
                             true);
    EXPECT_EQ(0, static_cast<FileHandleImplementation*>(file_handle)
            ->file_info_->active_locks_.size());
    file_handle->Close();

    // Open file again.
    file_handle = volume_->OpenFile(user_credentials_,
                                    path,
                                    SYSTEM_V_FCNTL_H_O_CREAT);
    // Make sure there are no locks for the file at the OSD.
    lock.reset(file_handle->CheckLock(3,  // PID.
                                      0,
                                      0,  // Range in bytes: [0, EOF]
                                      true));
    EXPECT_EQ(3, lock->client_pid());
    file_handle->Close();

    volume_->Unlink(user_credentials_, path);
  });
}

/** Obtain locks on a file and close the file handles. Reopen the file and
 *  check if the OSD still has any locks for it. */
TEST_F(VolumeImplementationTest, FilesLockingLastCloseReleasesAllLocks) {
  string path = "/test";

  FileHandle* file_handle;
  FileHandle* file_handle2;
  boost::scoped_ptr<Lock> lock1;
  boost::scoped_ptr<Lock> lock2;

  ASSERT_NO_THROW({
    // Open first file.
    file_handle = volume_->OpenFile(user_credentials_,
                                    path,
                                    SYSTEM_V_FCNTL_H_O_CREAT,
                                    448);  // octal 700.

    // No locks yet.
    EXPECT_EQ(0, static_cast<FileHandleImplementation*>(file_handle)
        ->file_info_->active_locks_.size());

    // Lock first file for writing.
    lock1.reset(file_handle->AcquireLock(1,  // PID.
                                         0,
                                         1,  // Range in bytes: [0, 1]
                                         true,
                                         false));
    EXPECT_EQ(1, static_cast<FileHandleImplementation*>(file_handle)
          ->file_info_->active_locks_.size());

    // Open second file.
    file_handle2 = volume_->OpenFile(user_credentials_,
                                     path,
                                     SYSTEM_V_FCNTL_H_O_RDWR);
    // Obtain a second lock.
    lock2.reset(file_handle2->AcquireLock(2,  // PID.
                                          2,
                                          1,  // Range in bytes: [2, 3]
                                          true,
                                          false));
    EXPECT_EQ(2, static_cast<FileHandleImplementation*>(file_handle)
          ->file_info_->active_locks_.size());

    file_handle2->Close();
    // Still two locks reaming as we did not explicitly call
    // file_handle->ReleaseLockOfProcess().
    EXPECT_EQ(2, static_cast<FileHandleImplementation*>(file_handle)
            ->file_info_->active_locks_.size());
    file_handle->Close();

    // Open file again.
    file_handle = volume_->OpenFile(user_credentials_,
                                    path,
                                    SYSTEM_V_FCNTL_H_O_CREAT);
    // Make sure there are no locks for the file at the OSD.
    lock1.reset(file_handle->CheckLock(3,  // PID.
                                       0,
                                       0,  // Range in bytes: [0, EOF]
                                       true));
    EXPECT_EQ(3, lock1->client_pid());
    file_handle->Close();

    // Cleanup.
    volume_->Unlink(user_credentials_, path);
  });
}

/** ReleaseLockOfProcess releases only the locks of the specified process id. */
TEST_F(VolumeImplementationTest, FilesLockingReleaseLockOfProcess) {
  string path = "/test";

  FileHandle* file_handle;
  FileHandle* file_handle2;
  boost::scoped_ptr<Lock> lock1;
  boost::scoped_ptr<Lock> lock2;

  ASSERT_NO_THROW({
    // Open first file.
    file_handle = volume_->OpenFile(user_credentials_,
                                    path,
                                    SYSTEM_V_FCNTL_H_O_CREAT,
                                    448);  // octal 700.

    // No locks yet.
    EXPECT_EQ(0, static_cast<FileHandleImplementation*>(file_handle)
        ->file_info_->active_locks_.size());

    // Lock first file for writing.
    lock1.reset(file_handle->AcquireLock(1,  // PID.
                                         0,
                                         1,  // Range in bytes: [0, 1]
                                         true,
                                         false));
    EXPECT_EQ(1, static_cast<FileHandleImplementation*>(file_handle)
          ->file_info_->active_locks_.size());

    // Open second file.
    file_handle2 = volume_->OpenFile(user_credentials_,
                                     path,
                                     SYSTEM_V_FCNTL_H_O_RDWR);
    // Obtain a second lock.
    lock2.reset(file_handle2->AcquireLock(2,  // PID.
                                          2,
                                          1,  // Range in bytes: [2, 3]
                                          true,
                                          false));
    EXPECT_EQ(2, static_cast<FileHandleImplementation*>(file_handle)
          ->file_info_->active_locks_.size());

    // Explicitly call file_handle->ReleaseLockOfProcess().
    file_handle2->ReleaseLockOfProcess(2);
    EXPECT_EQ(1, static_cast<FileHandleImplementation*>(file_handle)
            ->file_info_->active_locks_.size());
    file_handle2->Close();
    EXPECT_EQ(1, static_cast<FileHandleImplementation*>(file_handle)
                ->file_info_->active_locks_.size());

    file_handle->ReleaseLockOfProcess(1);
    EXPECT_EQ(0, static_cast<FileHandleImplementation*>(file_handle)
                ->file_info_->active_locks_.size());
    file_handle->Close();

    // Open file again.
    file_handle = volume_->OpenFile(user_credentials_,
                                    path,
                                    SYSTEM_V_FCNTL_H_O_CREAT);
    // Make sure there are no locks for the file at the OSD.
    lock1.reset(file_handle->CheckLock(3,  // PID.
                                       0,
                                       0,  // Range in bytes: [0, EOF]
                                       true));
    EXPECT_EQ(3, lock1->client_pid());
    file_handle->Close();

    // Cleanup.
    volume_->Unlink(user_credentials_, path);
  });
}

/** The directory entries cache is correctly updated after a rmdir. */
TEST_F(VolumeImplementationTest, CorrectDirectoryEntriesUpdateAfterRmDir) {
  string path = "/test";

  ASSERT_NO_THROW({
    // Create dir.
    volume_->MakeDirectory(user_credentials_, path, 448);

    // List directory (and thereby caching it).
    boost::scoped_ptr<DirectoryEntries> dentries(volume_->ReadDir(
        user_credentials_, "/", 0, options_.readdir_chunk_size, false));
    EXPECT_EQ(3, dentries->entries_size());

    // Delete dir.
    volume_->DeleteDirectory(user_credentials_, path);

    // List directory again and check for correct update.
    dentries.reset(volume_->ReadDir(
        user_credentials_, "/", 0, options_.readdir_chunk_size, false));
    EXPECT_EQ(2, dentries->entries_size());
  });
}

/** After enabling On-Close Replication at the Volume, a File is set to read_only after
 *  closing it. */
TEST_F(VolumeImplementationTest, ExplicitCloseSent) {
  string path_to_file = "/test";

  ASSERT_NO_THROW({
    // Enable on-close replication impclitely by increasing the replication
    // factor of the volume.
    string old_repl_factor;
    volume_->GetXAttr(user_credentials_,
                      "/",
                      "xtreemfs.default_rp",
                      &old_repl_factor);
    // No default replication policy by default.
    EXPECT_EQ("", old_repl_factor);
    volume_->SetXAttr(user_credentials_,
                      "/",
                      "xtreemfs.default_rp",
                      "{\"replication-factor\":2,\"replication-flags\":32,\"update-policy\":\"ronly\"}",  // NOLINT
                      XATTR_FLAGS_REPLACE);
    // Test if the increase was successful.
    string new_default_rp_policy;
    volume_->GetXAttr(user_credentials_,
                      "/",
                      "xtreemfs.default_rp",
                      &new_default_rp_policy);
    EXPECT_TRUE(new_default_rp_policy.find("\"replication-factor\":2,"));
    EXPECT_TRUE(new_default_rp_policy.find("\"replication-flags\":32,"));
    EXPECT_TRUE(new_default_rp_policy.find("\"update-policy\":\"ronly\"}"));

    // Open and close a file.
    FileHandle* file_handle = volume_->OpenFile(user_credentials_,
                                                    path_to_file,
                                                    SYSTEM_V_FCNTL_H_O_CREAT);
    string read_only_status;
    volume_->GetXAttr(user_credentials_,
                      path_to_file,
                      "xtreemfs.read_only",
                      &read_only_status);
    EXPECT_EQ("false", read_only_status);
    file_handle->Close();

    // The file has to be marked as read-only now.
    string read_only_status_after_explicit_close;
    volume_->GetXAttr(user_credentials_,
                      path_to_file,
                      "xtreemfs.read_only",
                      &read_only_status_after_explicit_close);
    EXPECT_EQ("true", read_only_status_after_explicit_close);
  });
}

/** GetAttr() does block until all pending async writes were completed, thus
 *  always considering the file size _after_ all previous (possibly still)
 *  pending async writes. */
TEST_F(VolumeImplementationTest, GetAttrAfterWriteAsync) {
  string path_to_file = "test";

  ASSERT_NO_THROW({
    FileHandle* file_handle = volume_->OpenFile(
        user_credentials_,
        path_to_file,
        SYSTEM_V_FCNTL_H_O_CREAT);
    Stat stat;

    // File has a size of 0 bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(0, stat.size());

    const char* buf = "a";
    file_handle->Write(buf, strlen(buf), 0);

    // File has a size of 1 bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(strlen(buf), stat.size());

    file_handle->Close();

    volume_->Unlink(user_credentials_, path_to_file);
  });
}

/** Parallel to a write + close sequence, a second thread should always
 *  see the correct file size after a getattr.
 *
 *  The difficulty here is that a pending file size is only known locally
 *  as long as the file is open. If the subsequent close is faster than
 *  the concurrent getattr (after it finished waiting for the write
 *  completion), the pending file size will be no longer available as the
 *  FileInfo object was already removed from the open_file_table of the
 *  volume.
 *  In this case, GetAttr() has to read the current file size one more time
 *  from the MRC or stat cache.
 */
TEST_F(VolumeImplementationTest, ConcurrentGetAttrAndWriteAsyncPlusClose) {
  string path_to_file = "test";
  FileHandle* file_handle;
  Stat stat;
  // Try to write 100 times and run a concurrent getattr.
  int iterations = 100;
  char* buf = new char[iterations + 1];
  buf[0] = '\0';

  ASSERT_NO_THROW({
    for (int i = 0; i < iterations; i++) {
      strncat(buf + i, "a", 1);
      ASSERT_EQ(strlen(buf), i + 1);

      file_handle = volume_->OpenFile(user_credentials_,
                                      path_to_file,
                                      SYSTEM_V_FCNTL_H_O_CREAT,
                                      448);  // Octal 700.
      // File has a size of strlen(buf) - 1 bytes.
      volume_->GetAttr(user_credentials_, path_to_file, &stat);
      EXPECT_EQ(strlen(buf) - 1, stat.size());

      // File was not opend with O_SYNC, so async writes *should* be used.
      file_handle->Write(buf, strlen(buf), 0);
      boost::thread concurrent_getattr(boost::bind(
          &xtreemfs::VolumeImplementationTest::CheckFileSize,
          this,
          boost::cref(path_to_file),
          strlen(buf)));
      file_handle->Close();
      concurrent_getattr.join();
    }

    volume_->Unlink(user_credentials_, path_to_file);
  });
  delete[] buf;
  buf = NULL;
}

/** Make sure the result of Truncate() is correctly returned despite the
 *  metadata cache. */
TEST_F(VolumeImplementationTestFastPeriodicFileSizeUpdate, Truncate) {
  string path_to_file = "test2";
  int kNewFileSize = 23;

  ASSERT_NO_THROW({
    FileHandle* file_handle = volume_->OpenFile(
        user_credentials_,
        path_to_file,
        static_cast<SYSTEM_V_FCNTL>(
            SYSTEM_V_FCNTL_H_O_CREAT |SYSTEM_V_FCNTL_H_O_RDWR),
        511);
    Stat stat;

    // File has a size of 0 bytes.
    file_handle->GetAttr(user_credentials_, &stat);
    EXPECT_EQ(0, stat.size());

    file_handle->Truncate(user_credentials_, kNewFileSize);

    // Wait until the file size was written back to the MRC.
    boost::this_thread::sleep(boost::posix_time::seconds(2));

    // File has a size of kNewFileSize bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(kNewFileSize, stat.size());

    file_handle->Close();

    // File has a size of kNewFileSize bytes.
    volume_->GetAttr(user_credentials_, path_to_file, &stat);
    EXPECT_EQ(kNewFileSize, stat.size());

    volume_->Unlink(user_credentials_, path_to_file);
  });
}

}  // namespace xtreemfs
