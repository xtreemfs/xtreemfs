/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include <gtest/gtest.h>

#include <sys/time.h>

#include <boost/foreach.hpp>
#include <string>
#include <utility>
#include <vector>

#include "util/crypto/cipher.h"
#include "util/logging.h"

namespace xtreemfs {

class CryptoTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    util::initialize_logger(util::LEVEL_WARN);
  }

  virtual void TearDown() {
    util::shutdown_logger();
  }
};

/*
 */
TEST_F(CryptoTest, Cipher_AES_CTR) {
  std::string key_str = "01234567890123456789012345678901";
  std::vector<unsigned char> key(key_str.begin(), key_str.end());
  std::string plaintext("Plaintext test");
  std::vector<unsigned char> ciphertext(plaintext.length());
  Cipher cipher("aes-256-ctr");

  std::pair<std::vector<unsigned char>, int> encrypt_res = cipher.encrypt(
      boost::asio::buffer(plaintext), key, boost::asio::buffer(ciphertext));
  std::vector<unsigned char> iv = encrypt_res.first;

  std::vector<unsigned char> plaintext_dec(plaintext.length());
  cipher.decrypt(boost::asio::buffer(ciphertext), key, iv,
                 boost::asio::buffer(plaintext_dec));

  EXPECT_TRUE(
      std::vector<unsigned char>(plaintext.begin(), plaintext.end())
          == plaintext_dec);
}

}  // namespace xtreemfs

