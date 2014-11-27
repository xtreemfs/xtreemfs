/*
 * Copyright (c) 2014 by Philippe Lieser, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 */

#ifndef CPP_INCLUDE_UTIL_CRYPTO_OPENSSL_ERROR_H_
#define CPP_INCLUDE_UTIL_CRYPTO_OPENSSL_ERROR_H_

#include <string>

namespace xtreemfs {
namespace util {

void LogAndThrowOpenSSLError(std::string error_msg = "");

} /* namespace util */
} /* namespace xtreemfs */

#endif  // CPP_INCLUDE_UTIL_CRYPTO_OPENSSL_ERROR_H_
