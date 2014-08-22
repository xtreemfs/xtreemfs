/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include <gtest/gtest.h>

#include <sys/time.h>

#include <boost/foreach.hpp>
#include <string>
#include <vector>

#include "libxtreemfs/client.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/volume.h"
#include "libxtreemfs/xtreemfs_exception.h"

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
    while (!std::isalnum(c = static_cast<char>(std::rand() % 256))) {  // NOLINT
    }
    result += c;
  }
  return result;
}

class OnlineTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
#ifdef __linux__
    const char* dir_url_env = getenv("XTREEMFS_DIR_URL");
    if (dir_url_env) {
      dir_url_.xtreemfs_url = std::string(dir_url_env);
    }
    const char* mrc_url_env = getenv("XTREEMFS_MRC_URL");
    if (mrc_url_env) {
      mrc_url_.xtreemfs_url = std::string(mrc_url_env);
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

    auth_.set_auth_type(xtreemfs::pbrpc::AUTH_NONE);

    // Every operation is executed in the context of a given user and his groups
    // The UserCredentials object does store this information.
    user_credentials_.set_username("volume_implementation_test");
    user_credentials_.add_groups("volume_implementation_test");

    // set options for encryption
    options_.encryption = true;
    options_.encryption_block_size = 4;

    // Create a new instance of a client using the DIR service at 'localhost'
    // at port 32638 using the default implementation.
    options_.log_level_string = "WARN";
    client_ = Client::CreateClient(dir_url_.service_addresses,
                                   user_credentials_,
                                   dir_url_.GenerateSSLOptions(), options_);

    // Start the client (a connection to the DIR service will be setup).
    client_->Start();

    // Create volume with a random name.
    volume_name_ = RandomVolumeName(5);
    client_->CreateVolume(mrc_url_.service_addresses, auth_, user_credentials_,
                          volume_name_);

    // Mount volume.
    volume_ = client_->OpenVolume(volume_name_, NULL, options_);
  }

  virtual void TearDown() {
    volume_->Close();

    client_->DeleteVolume(mrc_url_.service_addresses, auth_, user_credentials_,
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

class EncryptionTest : public OnlineTest {
 protected:
  virtual void SetUp() {
    OnlineTest::SetUp();

    // Open a file.
    file =
        volume_->OpenFile(
            user_credentials_,
            "/test_file",
            static_cast<xtreemfs::pbrpc::SYSTEM_V_FCNTL>(xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_CREAT // NOLINT
                | xtreemfs::pbrpc::SYSTEM_V_FCNTL_H_O_RDWR));
  }

  virtual void TearDown() {
    file->Close();

    OnlineTest::TearDown();
  }

  FileHandle* file;
};

TEST_F(EncryptionTest, Read_01) {
  char buffer[50];
  int x;

  // full write to first enc block
  ASSERT_NO_THROW({
    file->Write("ABCD", 4, 0);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 4, 0);
  });
  EXPECT_EQ(4, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCD", buffer);

  // partial read at the beginning 1
  ASSERT_NO_THROW({
    x = file->Read(buffer, 1, 0);
  });
  EXPECT_EQ(1, x);
  buffer[x] = 0;
  EXPECT_STREQ("A", buffer);

  // partial read at the beginning 2
  ASSERT_NO_THROW({
    x = file->Read(buffer, 2, 0);
  });
  EXPECT_EQ(2, x);
  buffer[x] = 0;
  EXPECT_STREQ("AB", buffer);

  // partial read in the middle
  ASSERT_NO_THROW({
    x = file->Read(buffer, 2, 1);
  });
  EXPECT_EQ(2, x);
  buffer[x] = 0;
  EXPECT_STREQ("BC", buffer);

  // partial read at the end
  ASSERT_NO_THROW({
    x = file->Read(buffer, 2, 2);
  });
  EXPECT_EQ(2, x);
  buffer[x] = 0;
  EXPECT_STREQ("CD", buffer);

  // partial read at the end; over file size
  ASSERT_NO_THROW({
    x = file->Read(buffer, 3, 3);
  });
  EXPECT_EQ(1, x);
  buffer[x] = 0;
  EXPECT_STREQ("D", buffer);

  // read begin directly behind file size
  ASSERT_NO_THROW({
    x = file->Read(buffer, 3, 4);
  });
  EXPECT_EQ(0, x);

  // read begin behind file size
  ASSERT_NO_THROW({
    x = file->Read(buffer, 3, 5);
  });
  EXPECT_EQ(0, x);

  // read begin behind file size
  ASSERT_NO_THROW({
    x = file->Read(buffer, 3, 10);
  });
  EXPECT_EQ(0, x);

  // read at at the beginning 0 bytes
  ASSERT_NO_THROW({
    x = file->Read(buffer, 0, 0);
  });
  EXPECT_EQ(0, x);

  // read 0 bytes behind file size
  ASSERT_NO_THROW({
    x = file->Read(buffer, 0, 10);
  });
  EXPECT_EQ(0, x);
}

TEST_F(EncryptionTest, Read_02) {
  char buffer[50];
  int x;

  // full write to 1-3 enc block
  ASSERT_NO_THROW({
    file->Write("ABCDEFGHIJKL", 12, 0);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 12, 0);
  });
  EXPECT_EQ(12, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCDEFGHIJKL", buffer);

  // read start 1. block to middle 2. block
  ASSERT_NO_THROW({
    x = file->Read(buffer, 5, 0);
  });
  EXPECT_EQ(5, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCDE", buffer);

  // read start 1. block to end 2. block
  ASSERT_NO_THROW({
    x = file->Read(buffer, 8, 0);
  });
  EXPECT_EQ(8, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCDEFGH", buffer);

  // read start 1. block to middle 3. block
  ASSERT_NO_THROW({
    x = file->Read(buffer, 11, 0);
  });
  EXPECT_EQ(11, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCDEFGHIJK", buffer);

  // read middle 1. block to end 1. block
  ASSERT_NO_THROW({
    x = file->Read(buffer, 3, 1);
  });
  EXPECT_EQ(3, x);
  buffer[x] = 0;
  EXPECT_STREQ("BCD", buffer);

  // read middle 1. block to middle 2. block
  ASSERT_NO_THROW({
    x = file->Read(buffer, 3, 2);
  });
  EXPECT_EQ(3, x);
  buffer[x] = 0;
  EXPECT_STREQ("CDE", buffer);

  // read middle 1. block to middle 3. block
  ASSERT_NO_THROW({
    x = file->Read(buffer, 7, 2);
  });
  EXPECT_EQ(7, x);
  buffer[x] = 0;
  EXPECT_STREQ("CDEFGHI", buffer);
}

TEST_F(EncryptionTest, Write_01) {
  char buffer[50];
  int x;

  // full write to first 2. block
  ASSERT_NO_THROW({
    file->Write("EFGH", 4, 4);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 10, 0);
  });
  EXPECT_EQ(8, x);
  buffer[x] = 0;
  for (int i = 0; i < 4; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
  EXPECT_STREQ("EFGH", buffer+4);

  // write from middle 1. block to middle 2. block
  ASSERT_NO_THROW({
    file->Write("BCDefg", 6, 1);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 10, 0);
  });
  EXPECT_EQ(8, x);
  buffer[x] = 0;
  for (int i = 0; i < 1; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
  EXPECT_STREQ("BCDefgH", buffer+1);

  // write from middle 4. block to middle 6. block
  ASSERT_NO_THROW({
    file->Write("NOPQRSTU", 8, 13);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 40, 0);
  });
  EXPECT_EQ(21, x);
  buffer[x] = 0;
  for (int i = 0; i <= 0; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
  EXPECT_STREQ("BCDefgH", buffer+1);
  for (int i = 8; i <= 12; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
  EXPECT_STREQ("NOPQRSTU", buffer+13);
}

TEST_F(EncryptionTest, Write_02) {
  char buffer[50];
  int x;

  // write from middle 3. block to middle 3. block
  ASSERT_NO_THROW({
    file->Write("JK", 2, 9);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 30, 0);
  });
  EXPECT_EQ(11, x);
  buffer[x] = 0;
  for (int i = 0; i < 9; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
  EXPECT_STREQ("JK", buffer+9);

  // write from middle 1. block to middle 1. block
  ASSERT_NO_THROW({
    file->Write("C", 1, 2);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 30, 0);
  });
  EXPECT_EQ(11, x);
  buffer[x] = 0;
  for (int i = 0; i < 2; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
  EXPECT_STREQ("C", buffer+2);
  for (int i = 3; i < 9; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
  EXPECT_STREQ("JK", buffer+9);

  // write from middle 1. block to middle 2. block
  ASSERT_NO_THROW({
    file->Write("DEF", 3, 3);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 30, 0);
  });
  EXPECT_EQ(11, x);
  buffer[x] = 0;
  for (int i = 0; i < 2; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
  EXPECT_STREQ("CDEF", buffer+2);
  for (int i = 6; i < 9; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
  EXPECT_STREQ("JK", buffer+9);
}

TEST_F(EncryptionTest, Write_03) {
  char buffer[50];
  int x;

  // write from start 1. block to middle 1. block
  ASSERT_NO_THROW({
    file->Write("A", 1, 0);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 30, 0);
  });
  EXPECT_EQ(1, x);
  buffer[x] = 0;
  EXPECT_STREQ("A", buffer);

  // write from start 3. block to end 3. block
  ASSERT_NO_THROW({
    file->Write("IJKL", 4, 8);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 30, 0);
  });
  EXPECT_EQ(12, x);
  buffer[x] = 0;
  EXPECT_STREQ("A", buffer);
  for (int i = 1; i < 8; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
  EXPECT_STREQ("IJKL", buffer+8);
}

TEST_F(EncryptionTest, Write_04) {
  char buffer[50];
  int x;

  // write from start 1. block to end 10. block
  ASSERT_NO_THROW({
    file->Write("ABCDEFGHIJKLMNOPQRSTUVWXabcdefghijklmnop", 40, 0);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 50, 0);
  });
  EXPECT_EQ(40, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCDEFGHIJKLMNOPQRSTUVWXabcdefghijklmnop", buffer);

  // write from start 11. block to end 12. block
  ASSERT_NO_THROW({
    file->Write("qrstuvwx", 8, 40);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 50, 0);
  });
  EXPECT_EQ(48, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCDEFGHIJKLMNOPQRSTUVWXabcdefghijklmnopqrstuvwx", buffer);
}

TEST_F(EncryptionTest, Write_05) {
  char buffer[50];
  int x;

  // write from start 1. block to end middle 4. block
  ASSERT_NO_THROW({
    file->Write("ABCDEFGHIJKLMN", 14, 0);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 50, 0);
  });
  EXPECT_EQ(14, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCDEFGHIJKLMN", buffer);

  // write from start 3. block to middle 4. block
  ASSERT_NO_THROW({
    file->Write("jiklmn", 6, 8);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 50, 0);
  });
  EXPECT_EQ(14, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCDEFGHjiklmn", buffer);
}

TEST_F(EncryptionTest, Truncate_01) {
  char buffer[50];
  int x;

  // truncate to middle 2. block
  ASSERT_NO_THROW({
    file->Truncate(user_credentials_, 6);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 30, 0);
  });
  EXPECT_EQ(6, x);
  buffer[x] = 0;
  for (int i = 0; i <= 6; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }

  // write from middle 2. block to end 2. block
  ASSERT_NO_THROW({
    file->Write("FGH", 3, 5);
  });

  // truncate to middle 5. block
  ASSERT_NO_THROW({
    file->Truncate(user_credentials_, 16);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 30, 0);
  });
  EXPECT_EQ(16, x);
  buffer[x] = 0;
  for (int i = 0; i <= 4; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
  EXPECT_STREQ("FGH", buffer+5);
  for (int i = 8; i <= 16; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }

  // truncate to middle 2. block
  ASSERT_NO_THROW({
    file->Truncate(user_credentials_, 6);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 30, 0);
  });
  EXPECT_EQ(6, x);
  buffer[x] = 0;
  for (int i = 0; i <= 4; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
  EXPECT_STREQ("F", buffer+5);

  // truncate to empty file
  ASSERT_NO_THROW({
    file->Truncate(user_credentials_, 0);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 30, 0);
  });
  EXPECT_EQ(0, x);
}

TEST_F(EncryptionTest, Truncate_02) {
  char buffer[50];
  int x;

  // write from start 1. block to end 2. block
  ASSERT_NO_THROW({
    file->Write("ABCDEFGH", 8, 0);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 30, 0);
  });
  EXPECT_EQ(8, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCDEFGH", buffer);

  // truncate to end  1. block
  ASSERT_NO_THROW({
    file->Truncate(user_credentials_, 4);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 30, 0);
  });
  EXPECT_EQ(4, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCD", buffer);
}

TEST_F(EncryptionTest, Truncate_03) {
  char buffer[50];
  int x;

  // write from start 1. block to end 1. block
  ASSERT_NO_THROW({
    file->Write("ABCD", 4, 0);
  });

  // write from start 1. block to middle 1. block
  ASSERT_NO_THROW({
    file->Write("ab", 2, 0);
  });

  // truncate to middle 1. block
  ASSERT_NO_THROW({
    file->Truncate(user_credentials_, 2);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 30, 0);
  });
  EXPECT_EQ(2, x);
  buffer[x] = 0;
  EXPECT_STREQ("ab", buffer);
}

TEST_F(EncryptionTest, Truncate_04) {
  char buffer[50];
  int x;

  // truncate to start 13. block
  ASSERT_NO_THROW({
    file->Truncate(user_credentials_, 49);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 50, 0);
  });
  EXPECT_EQ(49, x);
  buffer[x] = 0;
  for (int i = 0; i < 49; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }

  // truncate to end 9. block
  ASSERT_NO_THROW({
    file->Truncate(user_credentials_, 36);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 50, 0);
  });
  EXPECT_EQ(36, x);
  buffer[x] = 0;
  for (int i = 0; i < 36; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
}

TEST_F(EncryptionTest, Truncate_05) {
  char buffer[50];
  int x;

  // write from start 1. block to start 13. block
  ASSERT_NO_THROW({
    file->Write("ABCDEFGHIJKLMNOPQRSTUVWXabcdefghijklmnopqrstuvwxy", 49, 0);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 50, 0);
  });
  EXPECT_EQ(49, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCDEFGHIJKLMNOPQRSTUVWXabcdefghijklmnopqrstuvwxy", buffer);

  // truncate to end 10. block
  ASSERT_NO_THROW({
    file->Truncate(user_credentials_, 40);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 50, 0);
  });
  EXPECT_EQ(40, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCDEFGHIJKLMNOPQRSTUVWXabcdefghijklmnop", buffer);
}

TEST_F(EncryptionTest, Truncate_06) {
  char buffer[200];
  int x;

  // truncate to end 18. block
  ASSERT_NO_THROW({
    file->Truncate(user_credentials_, 72);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 200, 0);
  });
  EXPECT_EQ(72, x);
  buffer[x] = 0;
  for (int i = 0; i < 72; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }

  // truncate to end 25. block
  ASSERT_NO_THROW({
    file->Truncate(user_credentials_, 100);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 200, 0);
  });
  EXPECT_EQ(100, x);
  buffer[x] = 0;
  for (int i = 0; i < 100; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }

  // truncate to end 18. block
  ASSERT_NO_THROW({
    file->Truncate(user_credentials_, 72);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 200, 0);
  });
  EXPECT_EQ(72, x);
  buffer[x] = 0;
  for (int i = 0; i < 72; i++) {
    EXPECT_EQ(*(buffer + i), 0);
  }
}

TEST_F(EncryptionTest, Truncate_07) {
  char buffer[200];
  int x;

  // write from start 1. block to start 13. block
  ASSERT_NO_THROW({
    file->Write("ABCDEFGHIJKLMNOPQRSTUVWXabcdefghijklmnopqrstuvwxy", 49, 0);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 50, 0);
  });
  EXPECT_EQ(49, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCDEFGHIJKLMNOPQRSTUVWXabcdefghijklmnopqrstuvwxy", buffer);

  // truncate to end 2. block
  ASSERT_NO_THROW({
    file->Truncate(user_credentials_, 8);
  });

  // full read
  ASSERT_NO_THROW({
     x = file->Read(buffer, 200, 0);
  });
  EXPECT_EQ(8, x);
  buffer[x] = 0;
  EXPECT_STREQ("ABCDEFGH", buffer);
}

}  // namespace xtreemfs

