/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#include "util/crypto/rand.h"

#include <vector>

#include "util/crypto/openssl_error.h"

using xtreemfs::util::LogAndThrowOpenSSLError;

namespace xtreemfs {

/**
 * Generates cryptographically strong pseudo-random bytes.
 *
 * @param num  Number of bytes to generate.
 * @return Random bytes.
 */
std::vector<unsigned char> Rand::Bytes(int num) const {
  std::vector<unsigned char> bytes(num);
  if (1 != RAND_bytes(bytes.data(), num)) {
    LogAndThrowOpenSSLError();
  }
  return bytes;
}

} /* namespace xtreemfs */
