/*
 * Copyright (c) 2014 by Robert Schmidtke, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifdef HAS_OPENSSL

#include <gtest/gtest.h>

#include <boost/algorithm/string/predicate.hpp>
#include <boost/asio.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/thread.hpp>
#include <cerrno>
#include <fcntl.h>
#include <fstream>
#include <signal.h>
#include <stdio.h>
#include <stdexcept>
#include <string>
#include <openssl/opensslv.h>

#include "libxtreemfs/client.h"
#include "libxtreemfs/client_implementation.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "pbrpc/RPC.pb.h"
#include "util/logging.h"

// http://stackoverflow.com/questions/16491675/how-to-send-custom-message-in-google-c-testing-framework/
namespace testing {
namespace internal {
  enum GTestColor {
    COLOR_DEFAULT, COLOR_RED, COLOR_GREEN, COLOR_YELLOW
  };

  extern void ColoredPrintf(GTestColor color, const char* fmt, ...);
} // namespace internal
} // namespace testing
#define PRINTF(...)  do { testing::internal::ColoredPrintf(testing::internal::COLOR_GREEN, "[          ] "); testing::internal::ColoredPrintf(testing::internal::COLOR_YELLOW, __VA_ARGS__); } while(0)

/** 
 * The working directory is assumed to be cpp/build.
 */

using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {
namespace rpc {

/**
 * Represents a DIR, MRC or OSD started via command line. Not the stripped
 * down test environment services.
 */
class ExternalService {
public:
  ExternalService(
      std::string service_class,
      std::string config_file_name,
      std::string log_file_name,
      std::string startup_phrase)
      : service_class_(service_class),
        config_file_name_(config_file_name),
        log_file_name_(log_file_name),
        startup_phrase_(startup_phrase),
        java_home_(""),
        classpath_(""),
        argv_(NULL),
        service_pid_(-1) {
    
    char *java_home = getenv("JAVA_HOME");
    if (java_home == NULL || (java_home_ = java_home).empty()) {
      if (Logging::log->loggingActive(LEVEL_WARN)) {
        Logging::log->getLog(LEVEL_WARN) << "JAVA_HOME is empty."
            << std::endl;
      }
    }
    
    if (!boost::algorithm::ends_with(java_home_, "/")) {
      java_home_ += "/";
    }
    
    classpath_ = "../../java/servers/dist/XtreemFS.jar";
    classpath_ += ":../../java/foundation/dist/Foundation.jar";
    classpath_ += ":../../java/flease/dist/Flease.jar";
    classpath_ += ":../../java/lib/*";
  }
      
  void cleanup() {
    service_pid_ = -1;
    if (argv_ != NULL) {
      for (size_t i = 0; i < 6; ++i) {
        free(argv_[i]);
      }
      delete[] argv_;
      argv_ = NULL;
    }
  }
  
  int JavaMajorVersion() {
    // java -version prints to stderr
    FILE *pipe = popen((java_home_ + "bin/java -version 2>&1").c_str(), "r");
    char buf[256];
    std::string output("");
    while (!feof(pipe)) {
      if (fgets(buf, 256, pipe) != NULL) {
        output += buf;
      }
    }
    pclose(pipe);
    // Output starts with: java version "1.X.Y_Z"
    size_t a = output.find('.', 0);
    size_t b = output.find('.', a + 1);
    return atoi(output.substr(a + 1, b - a - 1).c_str());
  }
  
  bool Start() {
    argv_ = new char*[7];
    argv_[0] = strdup((java_home_ + "bin/java").c_str());
    argv_[1] = strdup("-ea");
    argv_[2] = strdup("-cp");
    argv_[3] = strdup(classpath_.c_str());
    argv_[4] = strdup(service_class_.c_str());
    argv_[5] = strdup(config_file_name_.c_str());
    argv_[6] = NULL;
    
    char *envp[] = { NULL };
    
    service_pid_ = fork();
    if (service_pid_ == 0) {
      /* This block is executed by the child and is blocking. */
      
      // Redirect stdout and stderr to file
      int log_fd = open(log_file_name_.c_str(),
                        O_WRONLY | O_CREAT, S_IRUSR | S_IWUSR);
      if (log_fd >= 0) {
        dup2(log_fd, 1);
        dup2(log_fd, 2);
        close(log_fd);
      
        // execve does not return control upon successful completion.
        execve((java_home_ + "bin/java").c_str(), argv_, envp);
      }
      exit(errno);
    } else {
      /* This block is executed by the parent. */
      
      // Wait until the child has created the log file.
      int log_fd = -1;
      while ((log_fd = open(log_file_name_.c_str(), O_RDONLY)) == -1)
        ;
      
      // Listen to the log file until the startup phrase has occurred
      // and the service process is still running.
      std::string output("");
      bool service_alive = true;
      while (output.find(startup_phrase_) == std::string::npos && service_alive) {
        char buffer[1024];
        ssize_t n = read(log_fd, buffer, sizeof(buffer));
        if (n > 0) {
          output.append(buffer, n);
        } else if (n == -1) {
          throw std::runtime_error(
              "Could not read log file '" + log_file_name_ + "'.");
        }
        
        // Read status of child process using wait because otherwise it would
        // remain in the process table, even if exited to allow the parent to
        // read the exit status using wait.
        int status;
        pid_t p = waitpid(service_pid_, &status, WNOHANG);
        if (p == service_pid_) {
          service_alive = !WIFEXITED(status);
        }
      }
      close(log_fd);
      
      if (!service_alive) {
        std::cerr << service_class_ << " did not start properly: " << errno
            << std::endl;
        cleanup();
        return false;
      }
    }
    return true;
  }
  
  void Shutdown() {
    if (service_pid_ > 0) {
      // This block is executed by the parent. Interrupt the child and wait
      // for completion.
      kill(service_pid_, 2);
      waitpid(service_pid_, NULL, 0);
      cleanup();
    }
  }

private:
  std::string service_class_;
  std::string config_file_name_;
  std::string log_file_name_;
  std::string startup_phrase_;
  std::string java_home_;
  std::string classpath_;
  
  char **argv_;
  pid_t service_pid_;
};
  
class ExternalDIR : public ExternalService {
public:
  ExternalDIR(std::string config_file_name, std::string log_file_name)
  : ExternalService("org.xtreemfs.dir.DIR", config_file_name, log_file_name,
                    "PBRPC Srv 48638 ready") {}
};

class ExternalMRC : public ExternalService {
public:
  ExternalMRC(std::string config_file_name, std::string log_file_name)
  : ExternalService("org.xtreemfs.mrc.MRC", config_file_name, log_file_name,
                    "PBRPC Srv 48636 ready") {}
};

class ExternalOSD : public ExternalService {
public:
  ExternalOSD(std::string config_file_name, std::string log_file_name)
  : ExternalService("org.xtreemfs.osd.OSD", config_file_name, log_file_name,
                    "PBRPC Srv 48640 ready") {}
};

enum TestCertificateType {
  kPKCS12, kPEM
};

char g_ssl_tls_version_sslv3[] = "sslv3";
char g_ssl_tls_version_ssltls[] = "ssltls";
char g_ssl_tls_version_tlsv1[] = "tlsv1";
char g_ssl_tls_version_tlsv11[] = "tlsv11";
char g_ssl_tls_version_tlsv12[] = "tlsv12";

class ClientTest : public ::testing::Test {
protected:  
  ClientTest() {
    char *xtreemfs_test_dir = getenv("XTREEMFS_TEST_DIR");
    if (xtreemfs_test_dir == NULL ||
        (xtreemfs_test_dir_ = xtreemfs_test_dir).empty()) {
      xtreemfs_test_dir_ = "/tmp/";
    } else {
      if (!boost::algorithm::ends_with(xtreemfs_test_dir_, "/")) {
        xtreemfs_test_dir_ += "/";
      }
      xtreemfs_test_dir_ += "log/";
    }

    char *tmpdir = getenv("TMPDIR");
    if (tmpdir != NULL) {
      tmpdir_.assign(tmpdir);
      if (!boost::algorithm::ends_with(tmpdir_, "/")) {
        tmpdir_.push_back('/');
      }
    } else {
      tmpdir_ = "/tmp/";
    }
  }
  
  virtual void SetUp() {
    initialize_logger(options_.log_level_string,
                      options_.log_file_path,
                      LEVEL_DEBUG);
    
    dir_log_file_name_ = options_.log_file_path + "_dir";
    mrc_log_file_name_ = options_.log_file_path + "_mrc";
    osd_log_file_name_ = options_.log_file_path + "_osd";
    
    external_dir_.reset(new ExternalDIR(dir_config_file_, dir_log_file_name_));
    ASSERT_TRUE(external_dir_->Start());
    external_mrc_.reset(new ExternalMRC(mrc_config_file_, mrc_log_file_name_));
    ASSERT_TRUE(external_mrc_->Start());
    external_osd_.reset(new ExternalOSD(osd_config_file_, osd_log_file_name_));
    ASSERT_TRUE(external_osd_->Start());
    
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
    if (client_.get() != NULL) {
      client_->Shutdown();
    }
    if (external_osd_.get() != NULL) {
      external_osd_->Shutdown();
    }
    if (external_mrc_.get() != NULL) {
      external_mrc_->Shutdown();
    }
    if (external_dir_.get() != NULL) {
      external_dir_->Shutdown();
    }

    const char *logfiles[4];
    logfiles[0] = options_.log_file_path.c_str();
    logfiles[1] = dir_log_file_name_.c_str();
    logfiles[2] = mrc_log_file_name_.c_str();
    logfiles[3] = osd_log_file_name_.c_str();

    if (HasFailure()) {
      for (int i = 0; i < 4; ++i) {
        PRINTF("Showing log file '%s'\n", logfiles[i]);
        std::ifstream logfile(logfiles[i]);
        if (logfile.is_open()) {
          std::string line;
          while (getline(logfile, line)) {
            PRINTF("%s\n", line.c_str());
          }
          logfile.close();
        } else {
          PRINTF("Could not open log file '%s'\n", logfiles[i]);
        }
      }
    }
    
    unlink(options_.log_file_path.c_str());
    unlink(dir_log_file_name_.c_str());
    unlink(mrc_log_file_name_.c_str());
    unlink(osd_log_file_name_.c_str());

    shutdown_logger();
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
  
  std::string cert_path(std::string cert) {
    return "../../tests/certs/client_ssl_test/" + cert;
  }
  
  std::string config_path(std::string config) {
    return "../../tests/configs/" + config;
  }
  
  std::string log_path(std::string log) {
    return xtreemfs_test_dir_ + log;
  }
  
  boost::scoped_ptr<ExternalDIR> external_dir_;
  boost::scoped_ptr<ExternalMRC> external_mrc_;
  boost::scoped_ptr<ExternalOSD> external_osd_;
  
  boost::scoped_ptr<xtreemfs::Client> client_;
  xtreemfs::Options options_;
  std::string dir_log_file_name_;
  std::string mrc_log_file_name_;
  std::string osd_log_file_name_;
  
  std::string tmpdir_;

  xtreemfs::Options dir_url_;
  xtreemfs::Options mrc_url_;
  
  std::string dir_config_file_;
  std::string mrc_config_file_;
  std::string osd_config_file_;
  
  std::string xtreemfs_test_dir_;

  xtreemfs::pbrpc::Auth auth_;
  xtreemfs::pbrpc::UserCredentials user_credentials_;
};

class ClientNoSSLTest : public ClientTest {
protected:
  virtual void SetUp() {
    dir_config_file_ = config_path("dirconfig_no_ssl.test");
    mrc_config_file_ = config_path("mrcconfig_no_ssl.test");
    osd_config_file_ = config_path("osdconfig_no_ssl.test");
        
    dir_url_.xtreemfs_url = "pbrpc://localhost:48638/";
    mrc_url_.xtreemfs_url = "pbrpc://localhost:48636/";
    
    options_.log_level_string = "DEBUG";
    options_.log_file_path = log_path("xtreemfs_client_ssl_test_no_ssl");
    
    ClientTest::SetUp();
  }
};

template<TestCertificateType t>
class ClientSSLTestShortChain : public ClientTest {
protected:
  virtual void set_cert_log_paths () = 0;
  
  virtual void assert_cert_occurences () = 0;
  
  virtual void SetUp() {
    // Root signed, root trusted
    dir_config_file_ = config_path("dirconfig_ssl_short_chain.test");
    mrc_config_file_ = config_path("mrcconfig_ssl_short_chain.test");
    osd_config_file_ = config_path("osdconfig_ssl_short_chain.test");
        
    dir_url_.xtreemfs_url = "pbrpcs://localhost:48638/";
    mrc_url_.xtreemfs_url = "pbrpcs://localhost:48636/";
    
    options_.log_level_string = "DEBUG";
    
    // Root signed, only root as additional certificate.
    set_cert_log_paths();
    
    options_.ssl_verify_certificates = true;
        
    ClientTest::SetUp();
  }
  
  void DoTest() {
    CreateOpenDeleteVolume("test_ssl_short_chain");
  
    ASSERT_EQ(2, count_occurrences_in_file(
        options_.log_file_path,
        "SSL support activated"));
    
    assert_cert_occurences();

    ASSERT_EQ(2, count_occurrences_in_file(
        options_.log_file_path,
        "Verification of subject 'CN=Root CA,O=ZIB,L=Berlin,ST=Berlin,C=DE' "
        "was successful."));
    ASSERT_EQ(0, count_occurrences_in_file(
        options_.log_file_path,
        "CN=Intermediate CA,O=ZIB,L=Berlin,ST=Berlin,C=DE"));
    ASSERT_EQ(0, count_occurrences_in_file(
        options_.log_file_path,
        "CN=Leaf CA,O=ZIB,L=Berlin,ST=Berlin,C=DE"));

    ASSERT_EQ(1, count_occurrences_in_file(
        options_.log_file_path,
        "Verification of subject 'CN=MRC (Root),O=ZIB,L=Berlin,ST=Berlin,C=DE' "
        "was successful"));
    ASSERT_EQ(1, count_occurrences_in_file(
        options_.log_file_path,
        "Verification of subject 'CN=DIR (Root),O=ZIB,L=Berlin,ST=Berlin,C=DE' "
        "was successful."));
  }
};

class ClientSSLTestShortChainPKCS12 : public ClientSSLTestShortChain<kPKCS12> {
protected:
  void set_cert_log_paths () {
    options_.log_file_path = log_path("xtreemfs_client_ssl_test_short_chain_pkcs12");
    options_.ssl_pkcs12_path = cert_path("Client_Root_Root.p12");
  }
  
  void assert_cert_occurences () {
    ASSERT_EQ(2, count_occurrences_in_file(
        options_.log_file_path,
        "SSL support using PKCS#12 file "
        "../../tests/certs/client_ssl_test/Client_Root_Root.p12"));
    ASSERT_EQ(2, count_occurrences_in_file(
        options_.log_file_path,
        "Writing 1 verification certificates to " + tmpdir_ + "ca"));
  }
};

class ClientSSLTestShortChainPEM : public ClientSSLTestShortChain<kPEM> {
protected:
  void set_cert_log_paths () {
    options_.log_file_path = log_path("xtreemfs_client_ssl_test_short_chain_pem");
    options_.ssl_pem_cert_path = cert_path("Client_Root.pem");
    options_.ssl_pem_key_path = cert_path("Client_Root.key");
    options_.ssl_pem_trusted_certs_path = cert_path("CA_Root.pem");
  }
  
  void assert_cert_occurences () {
    ASSERT_EQ(2, count_occurrences_in_file(
        options_.log_file_path,
        "SSL support using PEM private key file "
        "../../tests/certs/client_ssl_test/Client_Root.key"));
  }
};

template<TestCertificateType t>
class ClientSSLTestLongChain : public ClientTest {
protected:
  virtual void set_cert_log_paths() = 0;
  
  virtual void assert_cert_occurences() = 0;
  
  virtual void SetUp() {
    // All service certificates are signed with Leaf CA, which is signed with
    // Intermediate CA, which is signed with Root CA. The keystore contains
    // only the Leaf CA.
    dir_config_file_ = config_path("dirconfig_ssl_long_chain.test");
    mrc_config_file_ = config_path("mrcconfig_ssl_long_chain.test");
    osd_config_file_ = config_path("osdconfig_ssl_long_chain.test");
        
    dir_url_.xtreemfs_url = "pbrpcs://localhost:48638/";
    mrc_url_.xtreemfs_url = "pbrpcs://localhost:48636/";
    
    options_.log_level_string = "DEBUG";
    
    // Client certificate is signed with Leaf CA. Contains the entire chain
    // as additional certificates.
    set_cert_log_paths();
    
    options_.ssl_verify_certificates = true;
    
    ClientTest::SetUp();
  }
  
  void DoTest() {
    CreateOpenDeleteVolume("test_ssl_long_chain");
  
    // Once for MRC and once for DIR.
    ASSERT_EQ(2, count_occurrences_in_file(
        options_.log_file_path,
        "SSL support activated"));
    
    assert_cert_occurences();

    ASSERT_EQ(2, count_occurrences_in_file(
        options_.log_file_path,
        "Verification of subject 'CN=Root CA,O=ZIB,L=Berlin,ST=Berlin,C=DE' "
        "was successful."));
    ASSERT_EQ(2, count_occurrences_in_file(
        options_.log_file_path,
        "Verification of subject 'CN=Intermediate CA,O=ZIB,L=Berlin,ST=Berlin,C=DE' "
        "was successful."));
    ASSERT_EQ(2, count_occurrences_in_file(
        options_.log_file_path,
        "Verification of subject 'CN=Leaf CA,O=ZIB,L=Berlin,ST=Berlin,C=DE' was "
        "successful."));

    ASSERT_EQ(1, count_occurrences_in_file(
        options_.log_file_path,
        "Verification of subject 'CN=MRC (Leaf),O=ZIB,L=Berlin,ST=Berlin,C=DE' "
        "was successful"));
    ASSERT_EQ(1, count_occurrences_in_file(
        options_.log_file_path,
        "Verification of subject 'CN=DIR (Leaf),O=ZIB,L=Berlin,ST=Berlin,C=DE' "
        "was successful."));
  }
};

class ClientSSLTestLongChainPKCS12 : public ClientSSLTestLongChain<kPKCS12> {
protected:
  void set_cert_log_paths () {
    options_.log_file_path = log_path("xtreemfs_client_ssl_test_long_chain_pkcs12");
    options_.ssl_pkcs12_path = cert_path("Client_Leaf_Chain.p12");
  }
  
  void assert_cert_occurences () {
    ASSERT_EQ(2, count_occurrences_in_file(
        options_.log_file_path,
        "SSL support using PKCS#12 file "
        "../../tests/certs/client_ssl_test/Client_Leaf_Chain.p12"));
    ASSERT_EQ(2, count_occurrences_in_file(
        options_.log_file_path,
        "Writing 3 verification certificates to " + tmpdir_ + "ca"));
  }
};

class ClientSSLTestLongChainPEM : public ClientSSLTestLongChain<kPEM> {
protected:
  void set_cert_log_paths () {
    options_.log_file_path = log_path("xtreemfs_client_ssl_test_long_chain_pem");
    options_.ssl_pem_cert_path = cert_path("Client_Leaf.pem");
    options_.ssl_pem_key_path = cert_path("Client_Leaf.key");
    options_.ssl_pem_trusted_certs_path = cert_path("CA_Chain.pem");
  }
  
  void assert_cert_occurences () {
    ASSERT_EQ(2, count_occurrences_in_file(
        options_.log_file_path,
        "SSL support using PEM private key file "
        "../../tests/certs/client_ssl_test/Client_Leaf.key"));
  }
};

template<TestCertificateType t>
class ClientSSLTestShortChainVerification : public ClientTest {
protected:
  virtual void set_cert_log_paths() = 0;
  
  virtual void SetUp() {
    dir_config_file_ = config_path("dirconfig_ssl_short_chain.test");
    mrc_config_file_ = config_path("mrcconfig_ssl_short_chain.test");
    osd_config_file_ = config_path("osdconfig_ssl_short_chain.test");
    
    dir_url_.xtreemfs_url = "pbrpcs://localhost:48638/";
    mrc_url_.xtreemfs_url = "pbrpcs://localhost:48636/";
    
    options_.log_level_string = "DEBUG";
    
    // Server does not know client's certificate, client does not know server's
    // certificate.
    set_cert_log_paths();
    
    options_.ssl_verify_certificates = true;
    
    // Need this to avoid too many reconnects upon SSL errors.
    options_.max_tries = 3;
        
    ClientTest::SetUp();
  }
  
  void DoTest() {
    // Server does not accept our certificate.
    std::string exception_text;
    try {
      CreateOpenDeleteVolume("test_ssl_verification");
    } catch (xtreemfs::IOException& e) {
      exception_text = e.what();
    }
    // Depending on whether the error occurs on initial connect or reconnect,
    // the error message varies. This depends on how quick the services start
    // up, such that the first connect might happen before the services are
    // operational.
    ASSERT_TRUE(
        exception_text.find("could not connect to host") != std::string::npos ||
        exception_text.find("cannot connect to server") != std::string::npos);

    // We do not accept the server's certificate.
    ASSERT_TRUE(count_occurrences_in_file(
        options_.log_file_path,
        "OpenSSL verify error: 20") > 0);  // Issuer certificate of untrusted
                                           // certificate cannot be found.
    ASSERT_TRUE(count_occurrences_in_file(
        options_.log_file_path,
        "Verification of subject 'CN=MRC (Root),O=ZIB,L=Berlin,ST=Berlin,C=DE' "
        "was unsuccessful.") > 0);
  }
};

class ClientSSLTestShortChainVerificationPKCS12 :
    public ClientSSLTestShortChainVerification<kPKCS12> {
protected:
  void set_cert_log_paths () {
    options_.log_file_path = log_path("xtreemfs_client_ssl_test_verification_pkcs12");		
    options_.ssl_pkcs12_path = cert_path("Client_Leaf.p12");
  }
};

class ClientSSLTestShortChainVerificationPEM :
    public ClientSSLTestShortChainVerification<kPEM> {
protected:
  void set_cert_log_paths () {
    options_.log_file_path = log_path("xtreemfs_client_ssl_test_verification_pem");		
    options_.ssl_pem_cert_path = cert_path("Client_Leaf.pem");		
    options_.ssl_pem_key_path = cert_path("Client_Leaf.key");
  }
};

template<TestCertificateType t>
class ClientSSLTestLongChainVerificationIgnoreErrors : public ClientTest {
protected:
  virtual void set_cert_log_paths() = 0;
  
  virtual void SetUp() {
    dir_config_file_ = config_path("dirconfig_ssl_ignore_errors.test");
    mrc_config_file_ = config_path("mrcconfig_ssl_ignore_errors.test");
    osd_config_file_ = config_path("osdconfig_ssl_ignore_errors.test");
    
    dir_url_.xtreemfs_url = "pbrpcs://localhost:48638/";
    mrc_url_.xtreemfs_url = "pbrpcs://localhost:48636/";
    
    options_.log_level_string = "DEBUG";
    
    // Server knows client's certificate, client does not know server's
    // certificate.
    set_cert_log_paths();
    
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
  
  void DoTest() {
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
        "Verification of subject 'CN=MRC (Leaf),O=ZIB,L=Berlin,ST=Berlin,C=DE' "
        "was unsuccessful. Overriding because of user settings."));
    ASSERT_EQ(3, count_occurrences_in_file(
        options_.log_file_path,
        "Verification of subject 'CN=DIR (Leaf),O=ZIB,L=Berlin,ST=Berlin,C=DE' "
        "was unsuccessful. Overriding because of user settings."));
  }
};

class ClientSSLTestLongChainVerificationIgnoreErrorsPKCS12 :
    public ClientSSLTestLongChainVerificationIgnoreErrors<kPKCS12> {
protected:
  void set_cert_log_paths () {
    options_.log_file_path =
        log_path("xtreemfs_client_ssl_test_verification_ignore_errors_pkcs12");
    options_.ssl_pkcs12_path = cert_path("Client_Leaf_Root.p12");
  }
};
    
class ClientSSLTestLongChainVerificationIgnoreErrorsPEM :
    public ClientSSLTestLongChainVerificationIgnoreErrors<kPEM> {
protected:
  void set_cert_log_paths() {
    options_.log_file_path =
        log_path("xtreemfs_client_ssl_test_verification_ignore_errors_pem");
    options_.ssl_pem_cert_path = cert_path("Client_Leaf.pem");
    options_.ssl_pem_key_path = cert_path("Client_Leaf.key");
    options_.ssl_pem_trusted_certs_path = cert_path("CA_Root.pem");
  }
};

template<TestCertificateType t>
class ClientSSLTestLongChainNoVerification : public ClientTest {
protected:
  virtual void set_cert_log_paths () = 0;
  
  virtual void SetUp() {
    dir_config_file_ = config_path("dirconfig_ssl_no_verification.test");
    mrc_config_file_ = config_path("mrcconfig_ssl_no_verification.test");
    osd_config_file_ = config_path("osdconfig_ssl_no_verification.test");
    
    dir_url_.xtreemfs_url = "pbrpcs://localhost:48638/";
    mrc_url_.xtreemfs_url = "pbrpcs://localhost:48636/";
    
    options_.log_level_string = "DEBUG";
    
    // Server knows client's certificate, client does not know all of server's
    // certificate.
    set_cert_log_paths();
                
    ClientTest::SetUp();
  }
  
  void DoTest() {
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
        "Verification of subject 'CN=DIR (Leaf),O=ZIB,L=Berlin,ST=Berlin,C=DE' "
        "was successful."));
    ASSERT_EQ(1, count_occurrences_in_file(
        options_.log_file_path,
        "Verification of subject 'CN=DIR (Leaf),O=ZIB,L=Berlin,ST=Berlin,C=DE' "
        "was successful."));
  }
};

class ClientSSLTestLongChainNoVerificationPKCS12 :
    public ClientSSLTestLongChainNoVerification<kPKCS12> {
protected:
  void set_cert_log_paths() {
    options_.log_file_path = log_path("xtreemfs_client_ssl_test_no_verification_pkcs12");
    options_.ssl_pkcs12_path = cert_path("Client_Leaf_Leaf.p12");
  }
};
    
class ClientSSLTestLongChainNoVerificationPEM :
    public ClientSSLTestLongChainNoVerification<kPEM> {
protected:
  void set_cert_log_paths() {
    options_.log_file_path = log_path("xtreemfs_client_ssl_test_no_verification_pem");
    options_.ssl_pem_cert_path = cert_path("Client_Leaf.pem");
    options_.ssl_pem_key_path = cert_path("Client_Leaf.key");
    options_.ssl_pem_trusted_certs_path = cert_path("CA_Leaf.pem");
  }
};
    
template<char const *client_ssl_method_string,
         char const *server_ssl_method_string>
class ClientSSLTestSSLVersion : public ClientTest {
public:
  ClientSSLTestSSLVersion() {
    // Grab the Java version that all services run on so we know what TLS
    // capabilities to expect.
    java_major_version_ = ExternalService("", "", "", "").JavaMajorVersion();
  }
  
protected:
  virtual void SetUp() {
    ASSERT_GE(java_major_version_, 6);
    
    if (strcmp(server_ssl_method_string, "ssltls") == 0) {
      dir_config_file_ = config_path("dirconfig_ssl_version.test");
      mrc_config_file_ = config_path("mrcconfig_ssl_version.test");
      osd_config_file_ = config_path("osdconfig_ssl_version.test");
    } else if (strcmp(server_ssl_method_string, "sslv3") == 0) {
      dir_config_file_ = config_path("dirconfig_ssl_version_sslv3.test");
      mrc_config_file_ = config_path("mrcconfig_ssl_version_sslv3.test");
      osd_config_file_ = config_path("osdconfig_ssl_version_sslv3.test");
    } else if (strcmp(server_ssl_method_string, "tlsv1") == 0) {
      dir_config_file_ = config_path("dirconfig_ssl_version_tlsv1.test");
      mrc_config_file_ = config_path("mrcconfig_ssl_version_tlsv1.test");
      osd_config_file_ = config_path("osdconfig_ssl_version_tlsv1.test");
    } else if (strcmp(server_ssl_method_string, "tlsv11") == 0) {
      dir_config_file_ = config_path("dirconfig_ssl_version_tlsv11.test");
      mrc_config_file_ = config_path("mrcconfig_ssl_version_tlsv11.test");
      osd_config_file_ = config_path("osdconfig_ssl_version_tlsv11.test");
    } else if (strcmp(server_ssl_method_string, "tlsv12") == 0) {
      dir_config_file_ = config_path("dirconfig_ssl_version_tlsv12.test");
      mrc_config_file_ = config_path("mrcconfig_ssl_version_tlsv12.test");
      osd_config_file_ = config_path("osdconfig_ssl_version_tlsv12.test");
    } else {
      FAIL() << "Unsupported server SSL method.";
    }
    
    dir_url_.xtreemfs_url = "pbrpcs://localhost:48638/";
    mrc_url_.xtreemfs_url = "pbrpcs://localhost:48636/";
    
    options_.log_level_string = "DEBUG";
    
    options_.log_file_path =
        log_path("xtreemfs_client_ssl_test_version_pkcs12");
    options_.ssl_pkcs12_path = cert_path("Client_Root_Root.p12");
    
    options_.log_file_path.append("_").append(client_ssl_method_string);
    options_.log_file_path.append("_").append(server_ssl_method_string);
    
    options_.ssl_method_string = client_ssl_method_string;
    options_.ssl_verify_certificates = true;
    
    options_.max_tries = 3;
                
    ClientTest::SetUp();
  }
  
  void assert_ssl_tls_in_log(std::string ssl_tls, std::string ex) {
    ASSERT_EQ(2, count_occurrences_in_file(
        options_.log_file_path, "Using SSL/TLS version '" + ssl_tls + "'."));
    ASSERT_EQ(0, ex.size());
  }
  
  void assert_no_ssl_tls_in_log(std::string ex) {
    // Different errors depending on boost version and client/server SSL/TLS
    // versions.
    size_t ssl_error = ex.find(
        "could not connect to host 'localhost:48636': asio.ssl error");
    size_t c_timeout = ex.find(
        "connection to 'localhost:48636' timed out");
    size_t r_timeout = ex.find("Request timed out");
    size_t connect_error = ex.find(
        "cannot connect to server 'localhost:48636'");
    ASSERT_TRUE(ssl_error != std::string::npos ||
                c_timeout != std::string::npos ||
                r_timeout != std::string::npos ||
                connect_error != std::string::npos);
  }
  
  void assert_ssl_tls_in_log_or_nothing(std::string ssl_tls, std::string ex) {
    size_t c1 = count_occurrences_in_file(
        options_.log_file_path, "Using SSL/TLS version '" + ssl_tls + "'.");
    size_t ssl_error = ex.find(
        "could not connect to host 'localhost:48636': asio.ssl error");
    size_t c_timeout = ex.find(
        "connection to 'localhost:48636' timed out");
    size_t r_timeout = ex.find("Request timed out");
    size_t connect_error = ex.find(
        "cannot connect to server 'localhost:48636'");
    ASSERT_TRUE(c1 == 2 ||
                ssl_error != std::string::npos ||
                c_timeout != std::string::npos ||
                r_timeout != std::string::npos ||
                connect_error != std::string::npos);
  }
  
  void assert_ssl_tls_in_log_or_other(std::string ssl_tls1, std::string ssl_tls2,
                                      std::string ex) {
    size_t c1 = count_occurrences_in_file(
        options_.log_file_path, "Using SSL/TLS version '" + ssl_tls1 + "'.");
    size_t c2 = count_occurrences_in_file(
        options_.log_file_path, "Using SSL/TLS version '" + ssl_tls2 + "'.");
    ASSERT_TRUE(c1 == 2 || c2 == 2);
    ASSERT_EQ(0, ex.size());
  }
  
  void DoTest() {
    std::string ex("");
    try {
      CreateOpenDeleteVolume("test_ssl_version");
    } catch (xtreemfs::IOException& e) {
      ex = e.what();
    }
    
    /* JDK 6 supports SSLv3, TLSv1 and possibly TLSv11. If it is set to TLSv11
     * but does not support it, it will default to TLS which supports all
     * other protocols. JDK 7 additionally supports TLSv11 and TLSv12.
     */
    
    if (strcmp(client_ssl_method_string, "sslv3") == 0) {
      if (strcmp(server_ssl_method_string, "sslv3") == 0 ||
          strcmp(server_ssl_method_string, "ssltls") == 0) {
        assert_ssl_tls_in_log("SSLv3", ex);
      } else {
        if (java_major_version_ == 6) {
          if (strcmp(server_ssl_method_string, "tlsv1") == 0) {
            assert_no_ssl_tls_in_log(ex);
          } else if (strcmp(server_ssl_method_string, "tlsv11") == 0) {
            assert_ssl_tls_in_log_or_nothing("SSLv3", ex);
          } else if (strcmp(server_ssl_method_string, "tlsv12") == 0) {
            assert_ssl_tls_in_log("SSLv3", ex);
          } else {
            FAIL() << "Unsupported server SSL method.";
          }
        } else {
          if (strcmp(server_ssl_method_string, "tlsv1") == 0 ||
              strcmp(server_ssl_method_string, "tlsv11") == 0 ||
              strcmp(server_ssl_method_string, "tlsv12") == 0) {
            assert_no_ssl_tls_in_log(ex);
          } else {
            FAIL() << "Unsupported server SSL method.";
          }
        }
      }
    } else if (strcmp(client_ssl_method_string, "tlsv1") == 0) {
      if (strcmp(server_ssl_method_string, "sslv3") == 0) {
        assert_no_ssl_tls_in_log(ex);
      } else if (strcmp(server_ssl_method_string, "tlsv1") == 0 ||
                 strcmp(server_ssl_method_string, "ssltls") == 0) {
        assert_ssl_tls_in_log("TLSv1", ex);
      } else {
        if (java_major_version_ == 6) {
          if (strcmp(server_ssl_method_string, "tlsv11") == 0) {
            assert_ssl_tls_in_log_or_nothing("TLSv1", ex);
          } else if (strcmp(server_ssl_method_string, "tlsv12") == 0) {
            assert_ssl_tls_in_log("TLSv1", ex);
          } else {
            FAIL() << "Unsupported server SSL method.";
          }
        } else {
          if (strcmp(server_ssl_method_string, "tlsv11") == 0 ||
              strcmp(server_ssl_method_string, "tlsv12") == 0) {
            assert_no_ssl_tls_in_log(ex);
          } else {
            FAIL() << "Unsupported server SSL method.";
          }
        }
      }
    } else {
#if (OPENSSL_VERSION_NUMBER < 0x1000100fL)
      /* OpenSSL < 1.0.1 supports SSLv3 and TLSv1 only. This implies a Boost
       * version smaller than 1.54, in which case tlsv11 and tlsv12 default to
       * ssltls behavior in the client.
       */

      if (strcmp(client_ssl_method_string, "tlsv11") == 0 ||
          strcmp(client_ssl_method_string, "tlsv12") == 0 ||
          strcmp(client_ssl_method_string, "ssltls") == 0) {
        if (strcmp(server_ssl_method_string, "sslv3") == 0) {
          assert_ssl_tls_in_log("SSLv3", ex);
        } else if (strcmp(server_ssl_method_string, "tlsv1") == 0 ||
                   strcmp(server_ssl_method_string, "ssltls") == 0) {
          assert_ssl_tls_in_log("TLSv1", ex);
        } else {
          if (java_major_version_ == 6) {
            if (strcmp(server_ssl_method_string, "tlsv11") == 0) {
              assert_ssl_tls_in_log_or_nothing("TLSv1", ex);
            } else if (strcmp(server_ssl_method_string, "tlsv12") == 0) {
              assert_ssl_tls_in_log("TLSv1", ex);
            }
          } else {
            if (strcmp(server_ssl_method_string, "tlsv11") == 0 ||
                strcmp(server_ssl_method_string, "tlsv12") == 0) {
              assert_no_ssl_tls_in_log(ex);
            } else {
              FAIL() << "Unsupported server SSL method.";
            }
          }
        }
      } else {
        FAIL() << "Unsupported client SSL method.";
      }
#else  // OPENSSL_VERSION_NUMBER < 0x1000100fL
      /* The client is capable of all methods, now it depends on the Java version
       * of the servers, see above. Boost >= 1.54 can enforce TLSv1.1 and TLSv1.2.
       * Note that this does not imply a Boost version >= 1.54 here. For smaller
       * Boost versions, tlsv11 and tlsv12 default to ssltls behavior in the client.
       */
      
      bool ssltls_behavior;
#if (BOOST_VERSION >= 105400)
      ssltls_behavior = false;
#else  // BOOST_VERSION >= 105400
      ssltls_behavior = true;
#endif  // BOOST_VERSION >= 105400

      if (strcmp(client_ssl_method_string, "tlsv11") == 0 &&
          !ssltls_behavior) {
        if (java_major_version_ == 6) {
          if (strcmp(server_ssl_method_string, "sslv3") == 0 ||
              strcmp(server_ssl_method_string, "tlsv1") == 0) {
            assert_no_ssl_tls_in_log(ex);
          } else if (strcmp(server_ssl_method_string, "tlsv11") == 0 ||
                     strcmp(server_ssl_method_string, "tlsv12") == 0 ||
                     strcmp(server_ssl_method_string, "ssltls") == 0) {
            assert_ssl_tls_in_log_or_nothing("TLSv1.1", ex);
          } else {
            FAIL() << "Unsupported server SSL method.";
          }
        } else {
          if (strcmp(server_ssl_method_string, "sslv3") == 0 ||
              strcmp(server_ssl_method_string, "tlsv1") == 0 ||
              strcmp(server_ssl_method_string, "tlsv12") == 0) {
            assert_no_ssl_tls_in_log(ex);
          } else if (strcmp(server_ssl_method_string, "tlsv11") == 0 ||
                     strcmp(server_ssl_method_string, "ssltls") == 0) {
            assert_ssl_tls_in_log("TLSv1.1", ex);
          } else {
            FAIL() << "Unsupported server SSL method.";
          }
        }
      } else if (strcmp(client_ssl_method_string, "tlsv12") == 0 &&
                 !ssltls_behavior) {
        if (java_major_version_ == 6) {
          if (strcmp(server_ssl_method_string, "sslv3") == 0 ||
              strcmp(server_ssl_method_string, "tlsv1") == 0 ||
              strcmp(server_ssl_method_string, "tlsv11") == 0 ||
              strcmp(server_ssl_method_string, "tlsv12") == 0 ||
              strcmp(server_ssl_method_string, "ssltls") == 0) {
            assert_no_ssl_tls_in_log(ex);
          } else {
            FAIL() << "Unsupported server SSL method.";
          }
        } else {
          if (strcmp(server_ssl_method_string, "sslv3") == 0 ||
              strcmp(server_ssl_method_string, "tlsv1") == 0 ||
              strcmp(server_ssl_method_string, "tlsv11") == 0) {
            assert_no_ssl_tls_in_log(ex);
          } else if (strcmp(server_ssl_method_string, "tlsv12") == 0 ||
                     strcmp(server_ssl_method_string, "ssltls") == 0) {
            assert_ssl_tls_in_log("TLSv1.2", ex);
          } else {
            FAIL() << "Unsupported server SSL method.";
          }
        }
      } else if (strcmp(client_ssl_method_string, "ssltls") == 0 ||
                 ssltls_behavior) {
        if (strcmp(server_ssl_method_string, "sslv3") == 0) {
          assert_ssl_tls_in_log("SSLv3", ex);
        } else if (strcmp(server_ssl_method_string, "tlsv1") == 0) {
          assert_ssl_tls_in_log("TLSv1", ex);
        } else {
          if (java_major_version_ == 6) {
            if (strcmp(server_ssl_method_string, "tlsv11") == 0 ||
                strcmp(server_ssl_method_string, "tlsv12") == 0 ||
                strcmp(server_ssl_method_string, "ssltls") == 0) {
              assert_ssl_tls_in_log_or_other("TLSv1", "TLSv1.1", ex);
            } else {
              FAIL() << "Unsupported server SSL method.";
            }
          } else {
            if (strcmp(server_ssl_method_string, "tlsv11") == 0) {
              assert_ssl_tls_in_log("TLSv1.1", ex);
            } else if (strcmp(server_ssl_method_string, "tlsv12") == 0) {
              // In Ubuntu 12.04 TLSv1.2 is disabled by default, so maybe
              // expect a fail here.
              assert_ssl_tls_in_log_or_nothing("TLSv1.2", ex);
            } else if (strcmp(server_ssl_method_string, "ssltls") == 0) {
              // Ubuntu 12.04 only goes up to TLSv1.1
              assert_ssl_tls_in_log_or_other("TLSv1.2", "TLSv1.1", ex);
            } else {
              FAIL() << "Unsupported server SSL method.";
            }
          }
        }
      } else {
        FAIL() << "Unsupported client SSL method.";
      }
#endif  // OPENSSL_VERSION_NUMBER < 0x1000100fL
    }
  }
  
private:
  int java_major_version_;
};

class ClientSSLTestSSLVersionPKCS12_SSLv3_SSLv3 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_sslv3, g_ssl_tls_version_sslv3> {};
class ClientSSLTestSSLVersionPKCS12_SSLTLS_SSLv3 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_ssltls, g_ssl_tls_version_sslv3> {};
class ClientSSLTestSSLVersionPKCS12_TLSv1_SSLv3 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv1, g_ssl_tls_version_sslv3> {};
class ClientSSLTestSSLVersionPKCS12_TLSv11_SSLv3 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv11, g_ssl_tls_version_sslv3> {};
class ClientSSLTestSSLVersionPKCS12_TLSv12_SSLv3 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv12, g_ssl_tls_version_sslv3> {};

class ClientSSLTestSSLVersionPKCS12_SSLv3_TLSv1 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_sslv3, g_ssl_tls_version_tlsv1> {};
class ClientSSLTestSSLVersionPKCS12_SSLTLS_TLSv1 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_ssltls, g_ssl_tls_version_tlsv1> {};
class ClientSSLTestSSLVersionPKCS12_TLSv1_TLSv1 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv1, g_ssl_tls_version_tlsv1> {};
class ClientSSLTestSSLVersionPKCS12_TLSv11_TLSv1 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv11, g_ssl_tls_version_tlsv1> {};
class ClientSSLTestSSLVersionPKCS12_TLSv12_TLSv1 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv12, g_ssl_tls_version_tlsv1> {};

class ClientSSLTestSSLVersionPKCS12_SSLv3_TLSv11 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_sslv3, g_ssl_tls_version_tlsv11> {};
class ClientSSLTestSSLVersionPKCS12_SSLTLS_TLSv11 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_ssltls, g_ssl_tls_version_tlsv11> {};
class ClientSSLTestSSLVersionPKCS12_TLSv1_TLSv11 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv1, g_ssl_tls_version_tlsv11> {};
class ClientSSLTestSSLVersionPKCS12_TLSv11_TLSv11 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv11, g_ssl_tls_version_tlsv11> {};
class ClientSSLTestSSLVersionPKCS12_TLSv12_TLSv11 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv12, g_ssl_tls_version_tlsv11> {};

class ClientSSLTestSSLVersionPKCS12_SSLv3_TLSv12 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_sslv3, g_ssl_tls_version_tlsv12> {};
class ClientSSLTestSSLVersionPKCS12_SSLTLS_TLSv12 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_ssltls, g_ssl_tls_version_tlsv12> {};
class ClientSSLTestSSLVersionPKCS12_TLSv1_TLSv12 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv1, g_ssl_tls_version_tlsv12> {};
class ClientSSLTestSSLVersionPKCS12_TLSv11_TLSv12 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv11, g_ssl_tls_version_tlsv12> {};
class ClientSSLTestSSLVersionPKCS12_TLSv12_TLSv12 : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv12, g_ssl_tls_version_tlsv12> {};

class ClientSSLTestSSLVersionPKCS12_SSLv3_SSLTLS : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_sslv3, g_ssl_tls_version_ssltls> {};
class ClientSSLTestSSLVersionPKCS12_SSLTLS_SSLTLS : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_ssltls, g_ssl_tls_version_ssltls> {};
class ClientSSLTestSSLVersionPKCS12_TLSv1_SSLTLS : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv1, g_ssl_tls_version_ssltls> {};
class ClientSSLTestSSLVersionPKCS12_TLSv11_SSLTLS : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv11, g_ssl_tls_version_ssltls> {};
class ClientSSLTestSSLVersionPKCS12_TLSv12_SSLTLS : public ClientSSLTestSSLVersion<
    g_ssl_tls_version_tlsv12, g_ssl_tls_version_ssltls> {};

TEST_F(ClientNoSSLTest, TestNoSSL) {
  CreateOpenDeleteVolume("test_no_ssl");
  ASSERT_EQ(0, count_occurrences_in_file(options_.log_file_path, "SSL"));
}

TEST_F(ClientSSLTestShortChainPKCS12, TestVerifyShortChain) { DoTest(); }

TEST_F(ClientSSLTestShortChainPEM, TestVerifyShortChain) { DoTest(); }

TEST_F(ClientSSLTestLongChainPKCS12, TestVerifyLongChain) { DoTest(); }

TEST_F(ClientSSLTestLongChainPEM, TestVerifyLongChain) { DoTest(); }

TEST_F(ClientSSLTestShortChainVerificationPKCS12, TestVerificationFail) {
  DoTest();
}

TEST_F(ClientSSLTestShortChainVerificationPEM, TestVerificationFail) {
  DoTest();
}

TEST_F(ClientSSLTestLongChainVerificationIgnoreErrorsPKCS12,
       TestVerificationIgnoreErrors) {
  DoTest();
}

TEST_F(ClientSSLTestLongChainVerificationIgnoreErrorsPEM,
       TestVerificationIgnoreErrors) {
  DoTest();
}

TEST_F(ClientSSLTestLongChainNoVerificationPKCS12, TestNoVerification) {
  DoTest();
}

TEST_F(ClientSSLTestLongChainNoVerificationPEM, TestNoVerification) { DoTest(); }

TEST_F(ClientSSLTestSSLVersionPKCS12_SSLv3_SSLv3, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_SSLTLS_SSLv3, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv1_SSLv3, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv11_SSLv3, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv12_SSLv3, TestSSLVersion) { DoTest(); }

TEST_F(ClientSSLTestSSLVersionPKCS12_SSLv3_TLSv1, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_SSLTLS_TLSv1, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv1_TLSv1, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv11_TLSv1, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv12_TLSv1, TestSSLVersion) { DoTest(); }

TEST_F(ClientSSLTestSSLVersionPKCS12_SSLv3_TLSv11, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_SSLTLS_TLSv11, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv1_TLSv11, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv11_TLSv11, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv12_TLSv11, TestSSLVersion) { DoTest(); }

TEST_F(ClientSSLTestSSLVersionPKCS12_SSLv3_TLSv12, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_SSLTLS_TLSv12, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv1_TLSv12, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv11_TLSv12, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv12_TLSv12, TestSSLVersion) { DoTest(); }

TEST_F(ClientSSLTestSSLVersionPKCS12_SSLv3_SSLTLS, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_SSLTLS_SSLTLS, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv1_SSLTLS, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv11_SSLTLS, TestSSLVersion) { DoTest(); }
TEST_F(ClientSSLTestSSLVersionPKCS12_TLSv12_SSLTLS, TestSSLVersion) { DoTest(); }

}  // namespace rpc
}  // namespace xtreemfs

#endif  // HAS_OPENSSL
