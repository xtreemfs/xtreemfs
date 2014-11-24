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
#include <stdio.h>
#include <string>

#include "libxtreemfs/client.h"
#include "libxtreemfs/client_implementation.h"
#include "libxtreemfs/options.h"
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
    initialize_logger(LEVEL_WARN);
    
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
    client_.reset(new ClientImplementation(dir_url_.service_addresses,
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
    dir_config_file_ = "tests/configs/dirconfig_nossl.test";
    mrc_config_file_ = "tests/configs/mrcconfig_nossl.test";
    osd_config_file_ = "tests/configs/osdconfig_nossl.test";
        
    dir_url_.xtreemfs_url = "pbrpc://localhost:42638/";
    mrc_url_.xtreemfs_url = "pbrpc://localhost:42636/";
    
    ClientTest::SetUp();
  }
};

class ClientSSLTest : public ClientTest {
protected:
  virtual void SetUp() {
    dir_config_file_ = "tests/configs/dirconfig_ssl.test";
    mrc_config_file_ = "tests/configs/mrcconfig_ssl.test";
    osd_config_file_ = "tests/configs/osdconfig_ssl.test";
        
    dir_url_.xtreemfs_url = "pbrpcs://localhost:42638/";
    mrc_url_.xtreemfs_url = "pbrpcs://localhost:42636/";
    
    options_.ssl_pkcs12_path = "tests/certs/client_ssl_test/Client_Leaf_Chain.p12";
    
    ClientTest::SetUp();
  }
};

TEST_F(ClientNoSSLTest, TestNoSSL) {
  CreateOpenDeleteVolume("test_no_ssl");
}

TEST_F(ClientSSLTest, TestNoSSL) {
  CreateOpenDeleteVolume("test_ssl");
}

}  // namespace rpc
}  // namespace xtreemfs
