/*
 * Copyright (c) 2014 by Robert Schmidtke, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <gtest/gtest.h>

#include <boost/algorithm/string/predicate.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/thread.hpp>
#include <fstream>
#include <stdio.h>
#include <string>

#include "libxtreemfs/client.h"
#include "libxtreemfs/client_implementation.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "pbrpc/RPC.pb.h"
#include "util/logging.h"


using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {
namespace rpc {
  
class ExternalService {
public:
  ExternalService(
      std::string config_file_name)
      : config_file_name_(config_file_name) {
    
    char *java_home = getenv("JAVA_HOME");
    if (java_home == NULL || (java_home_ = strdup(java_home)).empty()) {
      if (Logging::log->loggingActive(LEVEL_WARN)) {
        Logging::log->getLog(LEVEL_WARN) << "JAVA_HOME is empty."
            << std::endl;
      }
    }
    
    if (!boost::algorithm::ends_with(java_home_, "/")) {
      java_home_ += "/";
    }
    
    classpath_ = "java/servers/dist/XtreemFS.jar";
    classpath_ += ":java/foundation/dist/Foundation.jar";
    classpath_ += ":java/flease/dist/Flease.jar";
    classpath_ += ":java/lib/*";
    
    service_pid_ = -1;
  }
  
  void Start(std::string service_class) {
    char *argv[] = {
      strdup((java_home_ + "bin/java").c_str()),
      strdup("-ea"),
      strdup("-cp"),
      strdup(classpath_.c_str()),
      strdup(service_class.c_str()),
      strdup(config_file_name_.c_str()),
      NULL
    };
    
    char *envp[] = { NULL };
    
    service_pid_ = fork();
    if (service_pid_ == 0) {
      // Executed by the child.
      execve((java_home_ + "bin/java").c_str(), argv, envp);
      exit(EXIT_SUCCESS);
    }
  }
  
  void Shutdown() {
    if (service_pid_ > 0) {
      // Executed by the parent.
      kill(service_pid_, 2);
      waitpid(service_pid_, NULL, 0);
      service_pid_ = -1;
    }
  }

private:
  std::string config_file_name_;
  std::string java_home_;
  std::string classpath_;
  
  pid_t service_pid_;
};
  
class ExternalDIR : public ExternalService {
public:
  ExternalDIR(std::string config_file_name)
  : ExternalService(config_file_name) {}
  
  void Start() {
    ExternalService::Start("org.xtreemfs.dir.DIR");
  }
};

class ExternalMRC : public ExternalService {
public:
  ExternalMRC(std::string config_file_name)
  : ExternalService(config_file_name) {}
  
  void Start() {
    ExternalService::Start("org.xtreemfs.mrc.MRC");
  }
};

class ExternalOSD : public ExternalService {
public:
  ExternalOSD(std::string config_file_name)
  : ExternalService(config_file_name) {}
  
  void Start() {
    ExternalService::Start("org.xtreemfs.osd.OSD");
  }
};

class ClientTest : public ::testing::Test {
protected:
  virtual void SetUp() {
    initialize_logger(options_.log_level_string,
                      options_.log_file_path,
                      LEVEL_WARN);
    
    external_dir_.reset(new ExternalDIR(dir_config_file_));
    external_dir_->Start();
    external_mrc_.reset(new ExternalMRC(mrc_config_file_));
    external_mrc_->Start();
    external_osd_.reset(new ExternalOSD(osd_config_file_));
    external_osd_->Start();
    
    auth_.set_auth_type(AUTH_NONE);
    user_credentials_.set_username("client_ssl_test");
    user_credentials_.add_groups("client_ssl_test");
    
    options_.retry_delay_s = 5;
    
    mrc_url_.ParseURL(kMRC);
    dir_url_.ParseURL(kDIR);
    client_.reset(xtreemfs::Client::CreateClient(dir_url_.service_addresses,
                                                 user_credentials_,
                                                 options_.GenerateSSLOptions(),
                                                 options_));

    client_->Start();
  }

  virtual void TearDown() {
    client_->Shutdown();
    
    external_osd_->Shutdown();
    external_mrc_->Shutdown();
    external_dir_->Shutdown();
  }
  
  void CreateOpenDeleteVolume(std::string volume_name) {
    client_->CreateVolume(mrc_url_.service_addresses,
                        auth_,
                        user_credentials_,
                        volume_name);
    client_->OpenVolume(volume_name,
                        options_.GenerateSSLOptions(),
                        options_);
    client_->DeleteVolume(mrc_url_.service_addresses,
                          auth_,
                          user_credentials_,
                          volume_name);
  }
  
  size_t count_occurrences_in_file(std::string file_path, std::string s) {
    std::ifstream in(file_path.c_str(), std::ios_base::in);
    size_t occurences = 0;
    while (!in.eof()) {
      std::string line;
      std::getline(in, line);
      occurences += line.find(s) == std::string::npos ? 0 : 1;
    }
    in.close();
    return occurences;
  }
  
  boost::scoped_ptr<ExternalDIR> external_dir_;
  boost::scoped_ptr<ExternalMRC> external_mrc_;
  boost::scoped_ptr<ExternalOSD> external_osd_;
  
  boost::scoped_ptr<xtreemfs::Client> client_;
  xtreemfs::Options options_;
  
  xtreemfs::Options dir_url_;
  xtreemfs::Options mrc_url_;
  
  std::string dir_config_file_;
  std::string mrc_config_file_;
  std::string osd_config_file_;

  xtreemfs::pbrpc::Auth auth_;
  xtreemfs::pbrpc::UserCredentials user_credentials_;
};

class ClientNoSSLTest : public ClientTest {
protected:
  virtual void SetUp() {
    dir_config_file_ = "tests/configs/dirconfig_no_ssl.test";
    mrc_config_file_ = "tests/configs/mrcconfig_no_ssl.test";
    osd_config_file_ = "tests/configs/osdconfig_no_ssl.test";
        
    dir_url_.xtreemfs_url = "pbrpc://localhost:42638/";
    mrc_url_.xtreemfs_url = "pbrpc://localhost:42636/";
    
    options_.log_level_string = "DEBUG";
    options_.log_file_path = "/tmp/xtreemfs_client_ssl_test_no_ssl";
    
    ClientTest::SetUp();
  }
  
  virtual void TearDown() {
    ClientTest::TearDown();
    unlink(options_.log_file_path.c_str());
  }
};

class ClientSSLTestShortChain : public ClientTest {
protected:
  virtual void SetUp() {
    // Root signed, root trusted
    dir_config_file_ = "tests/configs/dirconfig_ssl_short_chain.test";
    mrc_config_file_ = "tests/configs/mrcconfig_ssl_short_chain.test";
    osd_config_file_ = "tests/configs/osdconfig_ssl_short_chain.test";
        
    dir_url_.xtreemfs_url = "pbrpcs://localhost:42638/";
    mrc_url_.xtreemfs_url = "pbrpcs://localhost:42636/";
    
    options_.log_level_string = "DEBUG";
    options_.log_file_path = "/tmp/xtreemfs_client_ssl_test_short_chain";
    
    // Root signed, only root as additional certificate.
    options_.ssl_pkcs12_path = "tests/certs/client_ssl_test/Client_Root_Root.p12";
    options_.ssl_verify_certificates = true;
    
    ClientTest::SetUp();
  }
  
  virtual void TearDown() {
    ClientTest::TearDown();
    unlink(options_.log_file_path.c_str());
  }
};

class ClientSSLTestLongChain : public ClientTest {
protected:
  virtual void SetUp() {
    // All service certificates are signed with Leaf CA, which is signed with
    // Intermediate CA, which is signed with Root CA. The keystore contains
    // only the Leaf CA.
    dir_config_file_ = "tests/configs/dirconfig_ssl_long_chain.test";
    mrc_config_file_ = "tests/configs/mrcconfig_ssl_long_chain.test";
    osd_config_file_ = "tests/configs/osdconfig_ssl_long_chain.test";
        
    dir_url_.xtreemfs_url = "pbrpcs://localhost:42638/";
    mrc_url_.xtreemfs_url = "pbrpcs://localhost:42636/";
    
    options_.log_level_string = "DEBUG";
    options_.log_file_path = "/tmp/xtreemfs_client_ssl_test_long_chain";
    
    // Client certificate is signed with Leaf CA. Contains the entire chain
    // as additional certificates.
    options_.ssl_pkcs12_path = "tests/certs/client_ssl_test/Client_Leaf_Chain.p12";
    options_.ssl_verify_certificates = true;
    
    ClientTest::SetUp();
  }
  
  virtual void TearDown() {
    ClientTest::TearDown();
    unlink(options_.log_file_path.c_str());
  }
};

class ClientSSLTestShortChainVerification : public ClientTest {
protected:
  virtual void SetUp() {
    dir_config_file_ = "tests/configs/dirconfig_ssl_short_chain.test";
    mrc_config_file_ = "tests/configs/mrcconfig_ssl_short_chain.test";
    osd_config_file_ = "tests/configs/osdconfig_ssl_short_chain.test";
    
    dir_url_.xtreemfs_url = "pbrpcs://localhost:42638/";
    mrc_url_.xtreemfs_url = "pbrpcs://localhost:42636/";
    
    options_.log_level_string = "DEBUG";
    options_.log_file_path = "/tmp/xtreemfs_client_ssl_test_verification";
    
    // Server does not know client's certificate, client does not know server's
    // certificate.
    options_.ssl_pkcs12_path = "tests/certs/client_ssl_test/Client_Leaf.p12";
    options_.ssl_verify_certificates = true;
    
    // Need this to avoid too many reconnects upon SSL errors.
    options_.max_tries = 3;
        
    ClientTest::SetUp();
  }
  
  virtual void TearDown() {
    ClientTest::TearDown();
    unlink(options_.log_file_path.c_str());
  }
};

class ClientSSLTestLongChainVerificationIgnoreErrors : public ClientTest {
protected:
  virtual void SetUp() {
    dir_config_file_ = "tests/configs/dirconfig_ssl_ignore_errors.test";
    mrc_config_file_ = "tests/configs/mrcconfig_ssl_ignore_errors.test";
    osd_config_file_ = "tests/configs/osdconfig_ssl_ignore_errors.test";
    
    dir_url_.xtreemfs_url = "pbrpcs://localhost:42638/";
    mrc_url_.xtreemfs_url = "pbrpcs://localhost:42636/";
    
    options_.log_level_string = "DEBUG";
    options_.log_file_path = "/tmp/xtreemfs_client_ssl_test_verification_ignore_errors";
    
    // Server knows client's certificate, client does not know server's
    // certificate.
    options_.ssl_pkcs12_path = "tests/certs/client_ssl_test/Client_Leaf_Root.p12";
    options_.ssl_verify_certificates = true;
    
    // The issuer certificate could not be found: this occurs if the issuer
    // certificate of an untrusted certificate cannot be found.
    options_.ssl_ignore_verify_errors.push_back(20);
    // The root CA is not marked as trusted for the specified purpose.
    options_.ssl_ignore_verify_errors.push_back(27);
    // No signatures could be verified because the chain contains only one
    // certificate and it is not self signed.
    options_.ssl_ignore_verify_errors.push_back(21);
            
    ClientTest::SetUp();
  }
  
  virtual void TearDown() {
    ClientTest::TearDown();
    unlink(options_.log_file_path.c_str());
  }
};

class ClientSSLTestLongChainNoVerification : public ClientTest {
protected:
  virtual void SetUp() {
    dir_config_file_ = "tests/configs/dirconfig_ssl_no_verification.test";
    mrc_config_file_ = "tests/configs/mrcconfig_ssl_no_verification.test";
    osd_config_file_ = "tests/configs/osdconfig_ssl_no_verification.test";
    
    dir_url_.xtreemfs_url = "pbrpcs://localhost:42638/";
    mrc_url_.xtreemfs_url = "pbrpcs://localhost:42636/";
    
    options_.log_level_string = "DEBUG";
    options_.log_file_path = "/tmp/xtreemfs_client_ssl_test_no_verification";
    
    // Server knows client's certificate, client does not know all of server's
    // certificate.
    options_.ssl_pkcs12_path = "tests/certs/client_ssl_test/Client_Leaf_Leaf.p12";
                
    ClientTest::SetUp();
  }
  
  virtual void TearDown() {
    ClientTest::TearDown();
    unlink(options_.log_file_path.c_str());
  }
};

TEST_F(ClientNoSSLTest, TestNoSSL) {
  CreateOpenDeleteVolume("test_no_ssl");
  ASSERT_EQ(0, count_occurrences_in_file(options_.log_file_path, "SSL"));
}

TEST_F(ClientSSLTestShortChain, TestVerifyShortChain) {
  CreateOpenDeleteVolume("test_ssl_short_chain");
  
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "SSL support activated"));
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "SSL support using PKCS#12 file "
      "tests/certs/client_ssl_test/Client_Root_Root.p12"));
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "Writing 1 verification certificates to /tmp/ca"));
  
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "Verification of subject '/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=Root CA' "
      "was successful."));
  ASSERT_EQ(0, count_occurrences_in_file(
      options_.log_file_path,
      "/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=Intermediate CA"));
  ASSERT_EQ(0, count_occurrences_in_file(
      options_.log_file_path,
      "/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=Leaf CA"));
  
  ASSERT_EQ(1, count_occurrences_in_file(
      options_.log_file_path,
      "Verification of subject '/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=MRC (Root)' "
      "was successful"));
  ASSERT_EQ(1, count_occurrences_in_file(
      options_.log_file_path,
      "Verification of subject '/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=DIR (Root)' "
      "was successful."));
}

TEST_F(ClientSSLTestLongChain, TestVerifyLongChain) {
  CreateOpenDeleteVolume("test_ssl_long_chain");
  
  // Once for MRC and once for DIR.
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "SSL support activated"));
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "SSL support using PKCS#12 file "
      "tests/certs/client_ssl_test/Client_Leaf_Chain.p12"));
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "Writing 3 verification certificates to /tmp/ca"));
  
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "Verification of subject '/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=Root CA' "
      "was successful."));
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "Verification of subject '/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=Intermediate "
      "CA' was successful."));
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "Verification of subject '/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=Leaf CA' was "
      "successful."));
  
  ASSERT_EQ(1, count_occurrences_in_file(
      options_.log_file_path,
      "Verification of subject '/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=MRC (Leaf)' "
      "was successful"));
  ASSERT_EQ(1, count_occurrences_in_file(
      options_.log_file_path,
      "Verification of subject '/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=DIR (Leaf)' "
      "was successful."));
}

TEST_F(ClientSSLTestShortChainVerification, TestVerificationFail) {
  // Server does not accept our certificate.
  std::string exception_text;
  try {
    CreateOpenDeleteVolume("test_ssl_verification");
  } catch (xtreemfs::IOException& e) {
    exception_text = e.what();
  }
  ASSERT_TRUE(exception_text.find("could not connect to host") != std::string::npos);
  
  // We do not accept the server's certificate.
  ASSERT_TRUE(count_occurrences_in_file(
      options_.log_file_path,
      "OpenSSL verify error: 20") > 0);  // Issuer certificate of untrusted
                                         // certificate cannot be found.
  ASSERT_TRUE(count_occurrences_in_file(
      options_.log_file_path,
      "Verification of subject '/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=MRC (Root)' "
      "was unsuccessful.") > 0);
}

TEST_F(ClientSSLTestLongChainVerificationIgnoreErrors, TestVerificationIgnoreErrors) {
  CreateOpenDeleteVolume("test_ssl_verification_ignore_errors");
  
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "Ignoring OpenSSL verify error: 20 because of user settings."));
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "Ignoring OpenSSL verify error: 27 because of user settings."));
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "Ignoring OpenSSL verify error: 21 because of user settings."));
  
  ASSERT_EQ(3, count_occurrences_in_file(
      options_.log_file_path,
      "Verification of subject '/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=MRC (Leaf)' "
      "was unsuccessful. Overriding because of user settings."));
  ASSERT_EQ(3, count_occurrences_in_file(
      options_.log_file_path,
      "Verification of subject '/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=DIR (Leaf)' "
      "was unsuccessful. Overriding because of user settings."));
}

TEST_F(ClientSSLTestLongChainNoVerification, TestNoVerification) {
  CreateOpenDeleteVolume("test_ssl_no_verification");
  
  // The issuer certificate of a looked up certificate could not be found.
  // This normally means the list of trusted certificates is not complete.
  ASSERT_EQ(2, count_occurrences_in_file(
      options_.log_file_path,
      "Ignoring OpenSSL verify error: 2 because of user settings."));
  
  // Twice for MRC, twice for DIR.
  ASSERT_EQ(4, count_occurrences_in_file(
      options_.log_file_path,
      "Ignoring OpenSSL verify error: 27 because of user settings."));
  
  // Succeed because the client can verify the leaf certificates, but not their
  // issuer certificates.
  ASSERT_EQ(1, count_occurrences_in_file(
      options_.log_file_path,
      "Verification of subject '/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=DIR (Leaf)' "
      "was successful."));
  ASSERT_EQ(1, count_occurrences_in_file(
      options_.log_file_path,
      "Verification of subject '/C=DE/ST=Berlin/L=Berlin/O=ZIB/CN=MRC (Leaf)' "
      "was successful."));
}

}  // namespace rpc
}  // namespace xtreemfs
