/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

// FIXME hack to work around occasional failing assert
// during mutex unlocking in debug mode
#define BOOST_DISABLE_ASSERTS

#include <gtest/gtest.h>

#include <algorithm>
#include <vector>

#include "common/test_environment.h"
#include "common/test_rpc_server_dir.h"
#include "common/test_rpc_server_mrc.h"
#include "common/test_rpc_server_osd.h"
#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "rpc/client.h"
#include "xtreemfs/OSDServiceConstants.h"

using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {
namespace rpc {

class AsyncWriteHandlerTest : public ::testing::Test {
 protected:
  static const int kBlockSize = 1024 * 128;

  virtual void SetUp() {
    initialize_logger(LEVEL_WARN);
    test_env.options.connect_timeout_s = 3;
    test_env.options.request_timeout_s = 3;
    test_env.options.retry_delay_s = 3;
    test_env.options.enable_async_writes = true;
    test_env.options.async_writes_max_request_size_kb = 128;
    test_env.options.async_writes_max_requests = 8;

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

  TestEnvironment test_env;
  Volume* volume;
  FileHandle* file;
};


/** A normal async write with nothing special */
TEST_F(AsyncWriteHandlerTest, NormalWrite) {
  size_t blocks = 5;
  size_t buffer_size = kBlockSize * blocks;
  boost::scoped_array<char> write_buf(new char[buffer_size]());

  vector<WriteEntry> expected(blocks);
  for (size_t i = 0; i < blocks; ++i) {
    expected[i] = WriteEntry(i, 0, kBlockSize);
  }

  ASSERT_NO_THROW(file->Write(write_buf.get(), buffer_size, 0));
  ASSERT_NO_THROW(file->Flush());

  EXPECT_TRUE(equal(expected.begin(),
                    expected.end(),
                    test_env.osds[0]->GetReceivedWrites().end() - blocks));

  ASSERT_NO_THROW(file->Close());
}

/** Let the first write request fail. The write should be retried and finally
 *  succeed. */
TEST_F(AsyncWriteHandlerTest, FirstWriteFail) {
  size_t blocks = 5;
  size_t buffer_size = kBlockSize * blocks;
  boost::scoped_array<char> write_buf(new char[buffer_size]());

  vector<WriteEntry> expected_tail(blocks);
  for (size_t i = 0; i < blocks; ++i) {
    expected_tail[i] = WriteEntry(i, 0, kBlockSize);
  }

  test_env.osds[0]->AddDropRule(
      new ProcIDFilterRule(xtreemfs::pbrpc::PROC_ID_WRITE, new DropNRule(1)));

  ASSERT_NO_THROW(file->Write(write_buf.get(), buffer_size, 0));
  ASSERT_NO_THROW(file->Flush());

  EXPECT_TRUE(equal(expected_tail.begin(), expected_tail.end(),
      test_env.osds[0]->GetReceivedWrites().end() - expected_tail.size()));

  ASSERT_NO_THROW(file->Close());
}

/** Let the last write request fail. The write should be retried and finally
 *  succeed. */
TEST_F(AsyncWriteHandlerTest, LastWriteFail) {
  size_t blocks = 5;
  size_t buffer_size = kBlockSize * blocks;
  boost::scoped_array<char> write_buf(new char[buffer_size]());

  vector<WriteEntry> expected_front(blocks - 1);
  vector<WriteEntry> expected_tail(1);
  for (size_t i = 0; i < blocks - 1; ++i) {
    expected_front[i] = WriteEntry(i, 0, kBlockSize);
  }

  expected_tail[0] = WriteEntry(blocks - 1, 0, kBlockSize);

  test_env.osds[0]->AddDropRule(
      new ProcIDFilterRule(xtreemfs::pbrpc::PROC_ID_WRITE,
                           new SkipMDropNRule(blocks - 1, 1)));

  ASSERT_NO_THROW(file->Write(write_buf.get(), buffer_size, 0));
  ASSERT_NO_THROW(file->Flush());

  EXPECT_TRUE(equal(expected_front.begin(),
                    expected_front.end(),
                    test_env.osds[0]->GetReceivedWrites().begin()));
  EXPECT_TRUE(equal(expected_tail.begin(),
                    expected_tail.end(),
                    test_env.osds[0]->GetReceivedWrites().end() -
                        expected_tail.size()));

  ASSERT_NO_THROW(file->Close());
}


/** Let the intermediate write request fail. The write should be retried and
 *  finally succeed. */
TEST_F(AsyncWriteHandlerTest, IntermediateWriteFail) {
  size_t blocks = 5;
  size_t buffer_size = kBlockSize * blocks;
  size_t middle = blocks / 2;
  boost::scoped_array<char> write_buf(new char[buffer_size]());

  vector<WriteEntry> expected_front(middle);
  vector<WriteEntry> expected_tail(blocks - middle);
  for (size_t i = 0; i < middle; ++i) {
    expected_front[i] = WriteEntry(i, 0, kBlockSize);
  }

  for (size_t i = middle; i < blocks; ++i) {
    expected_tail[i - middle] = WriteEntry(i, 0, kBlockSize);
  }

  test_env.osds[0]->AddDropRule(
      new ProcIDFilterRule(xtreemfs::pbrpc::PROC_ID_WRITE,
                           new SkipMDropNRule(middle, 1)));

  ASSERT_NO_THROW(file->Write(write_buf.get(), buffer_size, 0));
  ASSERT_NO_THROW(file->Flush());

  EXPECT_TRUE(equal(expected_front.begin(),
                    expected_front.end(),
                    test_env.osds[0]->GetReceivedWrites().begin()));
  EXPECT_TRUE(equal(expected_tail.begin(),
                    expected_tail.end(),
                    test_env.osds[0]->GetReceivedWrites().end() -
                        expected_tail.size()));

  ASSERT_NO_THROW(file->Close());
}


/** Let the all writes request fail. The write should be retried and finally
 *  succeed. */
TEST_F(AsyncWriteHandlerTest, AllWritesFail) {
  size_t blocks = 5;
  size_t buffer_size = kBlockSize * blocks;
  boost::scoped_array<char> write_buf(new char[buffer_size]());

  vector<WriteEntry> expected_tail(blocks);
  for (size_t i = 0; i < blocks; ++i) {
    expected_tail[i] = WriteEntry(i, 0, kBlockSize);
  }

  test_env.osds[0]->AddDropRule(
      new ProcIDFilterRule(xtreemfs::pbrpc::PROC_ID_WRITE,
                           new DropNRule(blocks)));

  ASSERT_NO_THROW(file->Write(write_buf.get(), buffer_size, 0));
  ASSERT_NO_THROW(file->Flush());

  EXPECT_TRUE(equal(expected_tail.begin(),
                    expected_tail.end(),
                    test_env.osds[0]->GetReceivedWrites().end() -
                        expected_tail.size()));

  ASSERT_NO_THROW(file->Close());
}



/** Let the first write request fail when there are more writes than
 *  writeahead allows.  The write should be retried and finally succeed. */
TEST_F(AsyncWriteHandlerTest, FirstWriteFailLong) {
  size_t blocks = 2 * test_env.options.async_writes_max_requests;
  size_t buffer_size = kBlockSize * blocks;
  boost::scoped_array<char> write_buf(new char[buffer_size]());

  vector<WriteEntry> expected_tail(blocks);
  for (size_t i = 0; i < blocks; ++i) {
    expected_tail[i] = WriteEntry(i, 0, kBlockSize);
  }

  test_env.osds[0]->AddDropRule(
      new ProcIDFilterRule(xtreemfs::pbrpc::PROC_ID_WRITE, new DropNRule(1)));

  ASSERT_NO_THROW(file->Write(write_buf.get(), buffer_size, 0));
  ASSERT_NO_THROW(file->Flush());

  EXPECT_TRUE(equal(expected_tail.begin(),
                    expected_tail.end(),
                    test_env.osds[0]->GetReceivedWrites().end() -
                        expected_tail.size()));

  ASSERT_NO_THROW(file->Close());
}

/** TODO(mno): Maybe let all future requests fail to test the retry count. */

}  // namespace rpc
}  // namespace xtreemfs
