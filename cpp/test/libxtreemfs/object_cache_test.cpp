/*
 * Copyright (c) 2013 by Felix Hupfeld.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <stdint.h>

#include <boost/lexical_cast.hpp>
#include <boost/scoped_ptr.hpp>
#include <string>

#include "common/test_environment.h"
#include "common/test_rpc_server_dir.h"
#include "common/test_rpc_server_mrc.h"
#include "common/test_rpc_server_osd.h"
#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/object_cache.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "rpc/client.h"
#include "rpc/sync_callback.h"
#include "util/logging.h"
#include "xtreemfs/OSDServiceConstants.h"

namespace xtreemfs {

const int kObjectSize = 10;

class FakeOsdFile {
 public:
  FakeOsdFile() : size_(0), reads_(0), writes_(0) {
    data_.reset(new char[500]);
  }
  int Read(int object_no, char* buffer) {
    reads_++;
    const int offset = object_no * kObjectSize;
    const int bytes_to_read = std::min(kObjectSize, size_ - offset);
    if (bytes_to_read <= 0) {
      return 0;
    }
    memcpy(buffer, &data_[offset], bytes_to_read);
    return bytes_to_read;
  }
  void Write(int object_no, const char* data, int bytes_to_write) {
    writes_++;
    const int offset = object_no * kObjectSize;
    memcpy(&data_[offset], data, bytes_to_write);
    size_ = std::max(size_, offset  + bytes_to_write);
  }
  void Truncate(int new_size) {
    size_ = new_size;
  }
  boost::scoped_array<char> data_;
  int size_;
  int reads_;
  int writes_;
};

class ObjectCacheTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    util::initialize_logger(util::LEVEL_WARN);
    cache_.reset(new ObjectCache(2, kObjectSize));
    reader_ = boost::bind(&FakeOsdFile::Read, &osd_file_, _1, _2);
    writer_ = boost::bind(&FakeOsdFile::Write, &osd_file_, _1, _2, _3);
  }

  virtual void TearDown() {
    google::protobuf::ShutdownProtobufLibrary();
    util::shutdown_logger();
  }

  FakeOsdFile osd_file_;
  boost::scoped_ptr<ObjectCache> cache_;
  ObjectReaderFunction reader_;
  ObjectWriterFunction writer_;
};

TEST_F(ObjectCacheTest, BasicUse) {
  cache_->Write(0, 0, "TestData", 9, reader_, writer_);
  EXPECT_EQ(1, osd_file_.reads_);
  EXPECT_EQ(0, osd_file_.writes_);
  EXPECT_EQ(0, osd_file_.size_);

  char buffer[10];
  EXPECT_EQ(9, cache_->Read(0, 0, buffer, 10, reader_, writer_));
  EXPECT_EQ(0, strcmp(buffer, "TestData"));
  EXPECT_EQ(1, osd_file_.reads_);
  EXPECT_EQ(0, osd_file_.writes_);
  EXPECT_EQ(0, osd_file_.size_);

  cache_->Flush(writer_);
  EXPECT_EQ(1, osd_file_.reads_);
  EXPECT_EQ(1, osd_file_.writes_);
  EXPECT_EQ(9, osd_file_.size_);

  cache_->Flush(writer_);
  EXPECT_EQ(1, osd_file_.reads_);
  EXPECT_EQ(1, osd_file_.writes_);
  EXPECT_EQ(9, osd_file_.size_);
}

TEST_F(ObjectCacheTest, Truncate) {
  cache_->Write(0, 0, "TestDataTe", 10, reader_, writer_);
  cache_->Write(1, 0, "stData", 6, reader_, writer_);
  EXPECT_EQ(2, osd_file_.reads_);
  EXPECT_EQ(0, osd_file_.writes_);
  EXPECT_EQ(0, osd_file_.size_);

  // Shrink
  cache_->Truncate(12);
  osd_file_.Truncate(12);
  EXPECT_EQ(2, osd_file_.reads_);
  EXPECT_EQ(0, osd_file_.writes_);
  EXPECT_EQ(12, osd_file_.size_);

  char buffer[17];
  EXPECT_EQ(2, cache_->Read(1, 0, buffer, 10, reader_, writer_));
  EXPECT_EQ(0, strncmp(buffer, "st", 2));
  EXPECT_EQ(2, osd_file_.reads_);
  EXPECT_EQ(0, osd_file_.writes_);
  EXPECT_EQ(12, osd_file_.size_);

  cache_->Flush(writer_);
  EXPECT_EQ(2, osd_file_.reads_);
  EXPECT_EQ(2, osd_file_.writes_);
  EXPECT_EQ(12, osd_file_.size_);

  // Extend
  cache_->Truncate(20);
  osd_file_.Truncate(20);
  EXPECT_EQ(2, osd_file_.reads_);
  EXPECT_EQ(2, osd_file_.writes_);
  EXPECT_EQ(20, osd_file_.size_);

  char buffer2[10];
  EXPECT_EQ(10, cache_->Read(1, 0, buffer2, 10, reader_, writer_));
  EXPECT_EQ(0, strncmp(buffer2, "st\0\0\0\0\0\0", 10));
  EXPECT_EQ(2, osd_file_.reads_);
  EXPECT_EQ(2, osd_file_.writes_);
  EXPECT_EQ(20, osd_file_.size_);

  cache_->Flush(writer_);
  EXPECT_EQ(2, osd_file_.reads_);
  // We need not to flush out shrunk objects as the layer above us will
  // take care of it by sending a truncate to the OSD.
  EXPECT_EQ(2, osd_file_.writes_);
  EXPECT_EQ(20, osd_file_.size_);
}

TEST_F(ObjectCacheTest, WriteBack) {
  cache_->Write(0, 0, "TestData", 9, reader_, writer_);
  EXPECT_EQ(1, osd_file_.reads_);
  EXPECT_EQ(0, osd_file_.writes_);
  EXPECT_EQ(0, osd_file_.size_);

  cache_->Write(1, 0, "TestData", 9, reader_, writer_);
  EXPECT_EQ(2, osd_file_.reads_);
  EXPECT_EQ(0, osd_file_.writes_);
  EXPECT_EQ(0, osd_file_.size_);

  cache_->Write(2, 0, "TestData", 9, reader_, writer_);
  EXPECT_EQ(3, osd_file_.reads_);
  EXPECT_EQ(1, osd_file_.writes_);
  EXPECT_EQ(9, osd_file_.size_);

  cache_->Flush(writer_);
  EXPECT_EQ(3, osd_file_.reads_);
  EXPECT_EQ(3, osd_file_.writes_);
  EXPECT_EQ(29, osd_file_.size_);

  // Read back in
  char buffer[10];
  EXPECT_EQ(10, cache_->Read(0, 0, buffer, 10, reader_, writer_));
  EXPECT_EQ(0, strncmp(buffer, "TestData\0\0", 10));
  EXPECT_EQ(4, osd_file_.reads_);
  EXPECT_EQ(3, osd_file_.writes_);
  EXPECT_EQ(29, osd_file_.size_);
}

class ObjectCacheEndToEndTest : public ::testing::Test {
 protected:
  static const int BLOCK_SIZE = 1024 * 128;

  virtual void SetUp() {
    util::initialize_logger(util::LEVEL_DEBUG);
    test_env.options.connect_timeout_s = 15;
    test_env.options.request_timeout_s = 5;
    test_env.options.retry_delay_s = 5;
    test_env.options.object_cache_size = 2;

    test_env.options.periodic_xcap_renewal_interval_s = 2;
    ASSERT_TRUE(test_env.Start());

    // Open a volume
    volume = test_env.client->OpenVolume(
        test_env.volume_name_,
        NULL,  // No SSL options.
        test_env.options);

    // Open a file.
    file = volume->OpenFile(
        test_env.user_credentials,
        "/test_file",
        static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(
            xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_CREAT |
            xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_TRUNC |
            xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDWR));
  }

  virtual void TearDown() {
    //volume->Close();
    test_env.Stop();
  }

  void CheckData(char* data, int buffer_size) {
    for (int i = 0; i < buffer_size; ++i) {
      ASSERT_EQ('0' + (i % 3), data[i]);
    }
  }
  void CheckDataIsNull(char* data, int buffer_size) {
    for (int i = 0; i < buffer_size; ++i) {
      ASSERT_EQ(0, data[i]);
    }
  }
  void FillData(char* data, int buffer_size) {
    for (int i = 0; i < buffer_size; ++i) {
      data[i] = '0' + (i % 3);
    }
  }
  TestEnvironment test_env;
  Volume* volume;
  FileHandle* file;
};

TEST_F(ObjectCacheEndToEndTest, Persistence) {
  const size_t blocks = 5;
  size_t buffer_size = BLOCK_SIZE * blocks;
  boost::scoped_array<char> write_buf(new char[buffer_size]);
  FillData(write_buf.get(), buffer_size);

  EXPECT_EQ(buffer_size, file->Write(write_buf.get(), buffer_size, 0));
  ASSERT_NO_THROW(file->Close());

  file = volume->OpenFile(
      test_env.user_credentials,
      "/test_file",
      static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(
          xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_CREAT |
          xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDONLY));
  memset(write_buf.get(), 0, buffer_size);
  EXPECT_EQ(buffer_size, file->Read(write_buf.get(), buffer_size, 0));
  CheckData(write_buf.get(), buffer_size);
  ASSERT_NO_THROW(file->Close());
}

TEST_F(ObjectCacheEndToEndTest, NormalWrite) {
  const size_t blocks = 5;
  size_t buffer_size = BLOCK_SIZE * blocks;
  boost::scoped_array<char> write_buf(new char[buffer_size]);
  FillData(write_buf.get(), buffer_size);

  std::vector<rpc::WriteEntry> expected(blocks);
  for (size_t i = 0; i < blocks; ++i) {
    expected[i] = rpc::WriteEntry(i, 0, BLOCK_SIZE);
  }

  EXPECT_EQ(0, file->Read(write_buf.get(), buffer_size, 0));

  file->Write(write_buf.get(), buffer_size, 0);

  boost::this_thread::sleep(boost::posix_time::seconds(
      2 * test_env.options.request_timeout_s));

  EXPECT_EQ(3, test_env.osds[0]->GetReceivedWrites().size());

  EXPECT_EQ(buffer_size, file->Read(write_buf.get(), buffer_size + 5, 0));
  CheckData(write_buf.get(), buffer_size);

  ASSERT_NO_THROW(file->Flush());

  boost::this_thread::sleep(boost::posix_time::seconds(
      2 * test_env.options.request_timeout_s));

  EXPECT_TRUE(equal(expected.begin(),
                    expected.end(),
                    test_env.osds[0]->GetReceivedWrites().end() - blocks));

  ASSERT_NO_THROW(file->Truncate(test_env.user_credentials, 0));
  EXPECT_EQ(0, file->Read(write_buf.get(), buffer_size, 0));

  ASSERT_NO_THROW(file->Truncate(test_env.user_credentials, buffer_size));
  EXPECT_EQ(buffer_size, file->Read(write_buf.get(), buffer_size, 0));
  CheckDataIsNull(write_buf.get(), buffer_size);

  file->Write(write_buf.get(), buffer_size, 0);
  ASSERT_NO_THROW(file->Close());

  EXPECT_EQ(5 + 5, test_env.osds[0]->GetReceivedWrites().size());
}

}  // namespace xtreemfs
