/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "rpc/client.h"

#include <openssl/pem.h>
#include <openssl/err.h>
#include <openssl/pkcs12.h>
#include <openssl/bio.h>
#include <openssl/rand.h>
#include <openssl/x509.h>
#include <openssl/evp.h>
#include <unistd.h>

#include <boost/algorithm/string/trim.hpp>
#include <boost/interprocess/detail/atomic.hpp>

#include <cstdio>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <list>
#include <utility>
#include <string>

#include "util/logging.h"


namespace xtreemfs {
namespace rpc {
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;
using namespace std;
using namespace boost;
using namespace google::protobuf;

Client::Client(int32_t connect_timeout_s,
               int32_t request_timeout_s,
               int32_t max_con_linger,
               const SSLOptions* options)
    : service_(),
      resolver_(service_),
      use_gridssl_(false),
      ssl_context_(NULL),
      callid_counter_(1),
      rq_timeout_timer_(service_),
      rq_timeout_s_(request_timeout_s),
      connect_timeout_s_(connect_timeout_s),
      max_con_linger_(max_con_linger),
      ssl_options(options),
      pemFileName(NULL),
      certFileName(NULL) {
  // Check if ssl options were passed.
  if (options != NULL) {
    Logging::log->getLog(LEVEL_INFO) << "SSL support activated" << endl;

    use_gridssl_ = options->use_grid_ssl();
    ssl_context_ =
        new boost::asio::ssl::context(service_,
                                      boost::asio::ssl::context::sslv23);
    ssl_context_->set_options(boost::asio::ssl::context::no_tlsv1);
    ssl_context_->set_verify_mode(boost::asio::ssl::context::verify_none);

    OpenSSL_add_all_algorithms();
    OpenSSL_add_all_ciphers();
    OpenSSL_add_all_digests();
    SSL_load_error_strings();

    // check if pkcs12 was entered
    // the pkcs12 file has to be read using openssl
    // afterwards the pem- and cert-files are written to temporary files on disk
    // these can be accessed by boost::assio::ssl
    if (!options->pkcs12_file_name().empty()) {
      Logging::log->getLog(LEVEL_INFO) << "SSL suport using pkcs12 file "
          << options->pkcs12_file_name() << endl;

      char tmplate1[] = "/tmp/pmXXXXXX";
      char tmplate2[] = "/tmp/ctXXXXXX";

      const char* fp = options->pkcs12_file_name().c_str();
      FILE *p12_file = fopen(fp, "rb");

      // read the pkcs12 file
      if (!p12_file) {
        Logging::log->getLog(LEVEL_ERROR) << "Error opening pkcs12 file:"
            << options->pkcs12_file_name() << ". (file not found)" << endl;
        exit(1);
      }
      PKCS12 *p12 = d2i_PKCS12_fp(p12_file, NULL);
      fclose(p12_file);

      if (!p12) {
        Logging::log->getLog(LEVEL_ERROR) << "Error reading pkcs12 file:"
            << options->pkcs12_file_name() << ". (no access rights?)" << endl;
        ERR_print_errors_fp(stderr);
        exit(1);
      }

      EVP_PKEY *pkey = NULL;
      X509 *cert = NULL;
      STACK_OF(X509) *ca = NULL;

      // parse pkcs12 file
      if (!PKCS12_parse(p12,
                        options->pkcs12_file_password().c_str(),
                        &pkey,
                        &cert,
                        &ca)) {
        Logging::log->getLog(LEVEL_ERROR) << "Error parsing pkcs12 file:"
            << options->pkcs12_file_name() << endl;
        ERR_print_errors_fp(stderr);
        exit(1);
      }
      PKCS12_free(p12);

      // create two tmp files containing the PEM certificates.
      // these which be deleted when exiting the program
      int tmpPem = mkstemp(tmplate1);
      int tmpCert = mkstemp(tmplate2);
      if (tmpPem == -1 || tmpCert == -1) {
        std::cerr << "Couldn't create temp file name.\n";
        exit(1);
      }
      FILE* pemFile = fdopen(tmpPem, "wb+");
      FILE* certFile = fdopen(tmpCert, "wb+");

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG) << "tmp file name:"
            << tmplate1 << " " << tmplate2 << endl;
      }

      // write private key
      // use the pkcs12 password as the pem password
      char* password = strdup(options->pkcs12_file_password().c_str());
      if (!PEM_write_PrivateKey(pemFile, pkey, NULL, NULL, 0, 0, password)) {
        Logging::log->getLog(LEVEL_ERROR)
            << "Error writing pem file:" << tmplate1 << endl;
        unlink(tmplate1);
        unlink(tmplate2);
        exit(1);
      }

      // write ca certificate
      if (!PEM_write_X509(certFile, cert)) {
        Logging::log->getLog(LEVEL_ERROR) << "Error writing cert file:"
            << tmplate2 << endl;
        unlink(tmplate1);
        unlink(tmplate2);
        exit(1);
      }

      fclose(pemFile);
      fclose(certFile);

      pemFileName = tmplate1;
      certFileName = tmplate2;

      ssl_context_->set_password_callback(
          boost::bind(&Client::get_pkcs12_password_callback, this));
      ssl_context_->use_private_key_file(pemFileName, options->cert_format());
      ssl_context_->use_certificate_chain_file(certFileName);

      // FIXME(ps) make sure that the temporary files are deleted!
    } else if (!options->pem_file_name().empty()) {
      // otherwise use the pem files
      Logging::log->getLog(LEVEL_INFO) << "SSL support using key file "
          << options->pem_file_name() << endl;

      try {
        ssl_context_->set_password_callback(
            boost::bind(&Client::get_pem_password_callback, this));
        ssl_context_->use_private_key_file(options->pem_file_name(),
                                           options->cert_format());
        ssl_context_->use_certificate_chain_file(options->pem_cert_name());
      } catch(invalid_argument& ia) {
         cerr << "Invalid argument: " << ia.what() << endl;
         cerr << "Please check your private key and certificate file."<< endl;
         exit(1);
      }
    }
  }
}

std::string Client::get_pem_password_callback() const {
  return ssl_options->pem_file_password();
}

std::string Client::get_pkcs12_password_callback() const {
  return ssl_options->pkcs12_file_password();
}

void Client::sendRequest(const string& address,
                         int32_t interface_id,
                         int32_t proc_id,
                         const UserCredentials& userCreds,
                         const Auth& auth,
                         const Message* message,
                         const char* data,
                         int data_length,
                         Message* response_message,
                         void* context,
                         ClientRequestCallbackInterface *callback) {
  uint32_t call_id =
      boost::interprocess::detail::atomic_inc32(&callid_counter_);
  ClientRequest *rq = new ClientRequest(address,
                                        call_id,
                                        interface_id,
                                        proc_id,
                                        userCreds,
                                        auth,
                                        message,
                                        data,
                                        data_length,
                                        response_message,
                                        context,
                                        callback);

  mutex::scoped_lock lock(this->requests_lock_);
  bool wasEmpty = requests_.empty();
  requests_.push(rq);
  if (wasEmpty) {
    service_.post(bind(&Client::sendInternalRequest, this));
  }
}

void Client::sendInternalRequest() {
  // Process requests.
  do {
    ClientRequest *rq = NULL;
    {
      mutex::scoped_lock lock(this->requests_lock_);
      if (requests_.empty())
        break;
      rq = requests_.front();
      requests_.pop();
    }
    assert(rq != NULL);

    rq->RequestSent();

    ClientConnection *con = NULL;
    connection_map::iterator iter = connections_.find(rq->address());
    if (iter != connections_.end())
      con = iter->second;
    if (con) {
      con->AddRequest(rq);
      con->DoProcess();
    } else {
      // New connection.

      // FIXME(bjk) start resolving the address
      const std::string &addr = rq->address();
      int colonpos = addr.find(":");
      if (colonpos < 0) {
        RPCHeader::ErrorResponse* err = new RPCHeader::ErrorResponse();
        err->set_error_message(std::string("invalid address: ") + addr);
        err->set_error_type(IO_ERROR);
        err->set_posix_errno(POSIX_ERROR_EINVAL);
        rq->set_error(err);
        rq->ExecuteCallback();
      } else {
        try {
          std::string server = addr.substr(0, colonpos);
          std::string port = addr.substr(colonpos + 1);

          con = new ClientConnection(server,
                                     port,
                                     service_,
                                     &request_table_,
                                     connect_timeout_s_,
                                     connect_timeout_s_,
                                     use_gridssl_,
                                     ssl_context_);

          if (Logging::log->loggingActive(LEVEL_DEBUG)) {
            Logging::log->getLog(LEVEL_DEBUG) << "new connection for "
                << addr << endl;
          }

          connections_[addr] = con;
          con->AddRequest(rq);
          con->DoProcess();
        } catch(std::out_of_range &exception) {
          RPCHeader::ErrorResponse* err = new RPCHeader::ErrorResponse();
          err->set_error_message(std::string("exception: ")
              + exception.what());
          err->set_error_type(ERRNO);
          err->set_posix_errno(POSIX_ERROR_EINVAL);
          rq->set_error(err);
          rq->ExecuteCallback();
        }
      }
    }
  } while (true);
}

void Client::handleTimeout(const boost::system::error_code& error) {
  try {
    request_map::iterator iter = request_table_.begin();
    list<int32_t> to_be_removed1;
    posix_time::ptime deadline = posix_time::microsec_clock::local_time()
        - posix_time::seconds(rq_timeout_s_);

    for (; iter != request_table_.end(); ++iter) {
      ClientRequest *rq = iter->second;
      if (rq->time_sent() < deadline) {
        if (Logging::log->loggingActive(LEVEL_DEBUG)) {
          Logging::log->getLog(LEVEL_DEBUG) << "request timed out: "
              << rq->call_id() << endl;
        }

        RPCHeader::ErrorResponse* err = new RPCHeader::ErrorResponse();
        err->set_error_message(string("request timed out"));
        err->set_error_type(IO_ERROR);
        err->set_posix_errno(POSIX_ERROR_EINVAL);
        rq->set_error(err);
        rq->ExecuteCallback();
        to_be_removed1.push_back(iter->first);
      }
    }
    for (list<int32_t>::iterator it = to_be_removed1.begin();
        it != to_be_removed1.end(); it++) {
      request_table_.erase(*it);
    }

    connection_map::iterator iter2 = connections_.begin();
    list<std::string> to_be_removed2;
    posix_time::ptime deadline2 = posix_time::microsec_clock::local_time()
        - posix_time::seconds(max_con_linger_);

    for (; iter2 != connections_.end(); iter2++) {
      pair<string, ClientConnection*> entry = *iter2;
      ClientConnection *con = entry.second;
      string addr = entry.first;
      assert(con != NULL);
      if (con->last_used() < deadline2) {
        if (Logging::log->loggingActive(LEVEL_DEBUG)) {
          Logging::log->getLog(LEVEL_DEBUG)
              << "connection not used, closing... " << addr << endl;
        }

        to_be_removed2.push_back(addr);
        con->Close();
      }
    }

    for (list<string>::iterator it = to_be_removed2.begin();
        it != to_be_removed2.end(); it++) {
      connections_.erase(*it);
    }
  } catch(std::exception &e) {
    cerr << "exception: " << e.what() << endl;
  }
  rq_timeout_timer_.expires_from_now(posix_time::seconds(rq_timeout_s_));
  rq_timeout_timer_.async_wait(bind(&Client::handleTimeout,
                                    this,
                                    asio::placeholders::error));
}

void Client::run() {
  asio::io_service::work work(service_);
  rq_timeout_timer_.expires_from_now(posix_time::seconds(rq_timeout_s_));
  rq_timeout_timer_.async_wait(bind(&Client::handleTimeout,
                                    this,
                                    asio::placeholders::error));

  if (Logging::log->loggingActive(LEVEL_INFO)) {
    Logging::log->getLog(LEVEL_INFO) << "starting rpc client" << endl;
    if (ssl_context_ != NULL) {
      if (use_gridssl_) {
        Logging::log->getLog(LEVEL_INFO) << "running in GRID SSL mode"
            << endl;
      } else {
        Logging::log->getLog(LEVEL_INFO) << "running in SSL mode"
            << endl;
      }
    } else {
      Logging::log->getLog(LEVEL_INFO) << "running in plain TPC mode"
          << endl;
    }
  }

  service_.run();

  connection_map::iterator iter2 = connections_.begin();
  for (; iter2 != connections_.end(); iter2++) {
    ClientConnection *con = iter2->second;
    assert(con != NULL);
    con->Close();
    delete con;
  }
}

void Client::shutdown() {
  service_.stop();
  if (Logging::log->loggingActive(LEVEL_INFO)) {
    Logging::log->getLog(LEVEL_INFO) << "rpc client stopped" << endl;
  }
}

Client::~Client() {
  delete ssl_options;
  // remove temporary cert and pem files
  if (pemFileName != NULL) {
    unlink(pemFileName);
  }
  if (certFileName != NULL) {
    unlink(certFileName);
  }
}

}  // namespace rpc
}  // namespace xtreemfs
