/*
 * Copyright (c) 2014 by Robert Schmidtke, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <openssl/pem.h>
#include <openssl/rsa.h>
#include <openssl/x509.h>

#include "common/test_environment.h"
#include "rpc/client.h"
#include "util/logging.h"

using namespace xtreemfs::util;

namespace xtreemfs {
namespace rpc {

class ClientSSLTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    //initialize_logger(LEVEL_WARN);
    //ASSERT_TRUE(test_env.Start());
    
    X509_REQ *req_ca_root = X509_REQ_new();
    
    BIGNUM *exponent = BN_new();
    BN_set_word(exponent, RSA_F4);
    
    RSA *rsa = RSA_new();
    RSA_generate_key_ex(rsa, 1024, exponent, NULL);
    
    X509_REQ_set_version(req_ca_root, 1);
    
    X509_NAME *x509_name = X509_REQ_get_subject_name(req_ca_root);
    X509_NAME_add_entry_by_txt(x509_name, "C", MBSTRING_ASC, (const unsigned char*) "Germany", -1, -1, 0);
    X509_NAME_add_entry_by_txt(x509_name, "ST", MBSTRING_ASC, (const unsigned char*) "Berlin", -1, -1, 0);
    X509_NAME_add_entry_by_txt(x509_name, "L", MBSTRING_ASC, (const unsigned char*) "Berlin", -1, -1, 0);
    X509_NAME_add_entry_by_txt(x509_name, "O", MBSTRING_ASC, (const unsigned char*) "ZIB", -1, -1, 0);
    X509_NAME_add_entry_by_txt(x509_name, "CN", MBSTRING_ASC, (const unsigned char*) "Root CA", -1, -1, 0);
    
    EVP_PKEY *public_key = EVP_PKEY_new();
    EVP_PKEY_assign_RSA(public_key, rsa);
    X509_REQ_set_pubkey(req_ca_root, public_key);
    
    X509_REQ_sign(req_ca_root, public_key, EVP_sha1());
    
    char tmplt[] = "/tmp/testmeXXXXXX";
    int f = mkstemp(tmplt);
    FILE *file = fdopen(f, "wb+");
    PEM_write_X509_REQ(file, req_ca_root);
    fclose(file);
  }

  virtual void TearDown() {
    //shutdown_logger();
    //test_env.Stop();
  }

  TestEnvironment test_env;
};

TEST_F(ClientSSLTest, TestNoSSL) {
  
}

}  // namespace rpc
}  // namespace xtreemfs
