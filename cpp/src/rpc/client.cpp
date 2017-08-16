/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *                    2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "rpc/client.h"

#include <cstdio>
#include <cstdlib>

#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/interprocess/detail/atomic.hpp>
#include <fstream>
#include <iostream>
#include <utility>
#include <set>
#include <string>

#include "util/logging.h"

#ifdef HAS_OPENSSL
#include <boost/asio/ssl.hpp>
#include <openssl/err.h>
#include <openssl/pem.h>
#include <openssl/pkcs12.h>
#include <openssl/rand.h>
#include <openssl/x509.h>
#endif  // HAS_OPENSSL
#ifdef WIN32
#include <tchar.h>
#else
#include <unistd.h>
#endif  // WIN32

using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;
using namespace std;
using namespace boost;
using namespace google::protobuf;

#if (BOOST_VERSION < 104800)
using boost::interprocess::detail::atomic_inc32;
#else
using boost::interprocess::ipcdetail::atomic_inc32;
#endif  // BOOST_VERSION < 104800

#ifdef _MSC_VER
// Disable "warning C4996: 'strdup': The POSIX name for this item is deprecated. Instead, use the ISO C++ conformant name: _strdup.  // NOLINT
#pragma warning(push)
#pragma warning(disable:4996)
#endif  // _MSC_VER

namespace xtreemfs {
namespace rpc {

Client::Client(int32_t connect_timeout_s,
               int32_t request_timeout_s,
               int32_t max_con_linger,
               const SSLOptions* options)
    : service_(),
      stopped_(false),
      stopped_ioservice_only_(false),
      callid_counter_(1),
      rq_timeout_timer_(service_),
      rq_timeout_s_(request_timeout_s),
      connect_timeout_s_(connect_timeout_s),
      max_con_linger_(max_con_linger)
#ifndef HAS_OPENSSL
{
  // Delete SSL options because they are not used when not compiled with SSL.
  delete options;
}
#else
      ,use_gridssl_(false),
      ssl_options(options),
      pemFileName(NULL),
      certFileName(NULL),
      trustedCAsFileName(NULL),
      ssl_context_(NULL) {
  // Check if ssl options were passed.
  if (options != NULL) {
    if (Logging::log->loggingActive(LEVEL_INFO)) {
      Logging::log->getLog(LEVEL_INFO) << "SSL support activated." << endl;
    }

    use_gridssl_ = options->use_grid_ssl();
    ssl_context_ = new boost::asio::ssl::context(
        service_,
        string_to_ssl_method(
            options->ssl_method_string(),
            boost::asio::ssl::context_base::sslv23_client));
    ssl_context_->set_options(boost::asio::ssl::context::no_sslv2);
#if (BOOST_VERSION > 104601)
    // Verify certificate callback can be conveniently specified from
    // Boost 1.47.0 onwards.
    ssl_context_->set_verify_mode(boost::asio::ssl::context::verify_peer |
                                  boost::asio::ssl::context::verify_fail_if_no_peer_cert);
    ssl_context_->set_verify_callback(boost::bind(&Client::verify_certificate_callback,
                                                  this, _1, _2));
#else  // BOOST_VERSION > 104601
    // The verify callback is not a Client member here, so make sure it can
    // retrieve the ssl_options later.
    SSL_CTX_set_app_data(ssl_context_->impl(), ssl_options);
    SSL_CTX_set_verify(ssl_context_->impl(),
                       boost::asio::ssl::context::verify_peer |
                       boost::asio::ssl::context::verify_fail_if_no_peer_cert,
                       &xtreemfs::rpc::verify_certificate_callback);
#endif  // BOOST_VERSION > 104601

    OpenSSL_add_all_algorithms();
    OpenSSL_add_all_ciphers();
    OpenSSL_add_all_digests();
    SSL_load_error_strings();

    // check if pkcs12 was entered
    // the pkcs12 file has to be read using openssl
    // afterwards the pem- and cert-files are written to temporary files on disk
    // these can be accessed by boost::assio::ssl
    if (!options->pkcs12_file_name().empty()) {
      if (Logging::log->loggingActive(LEVEL_INFO)) {
        Logging::log->getLog(LEVEL_INFO) << "SSL support using PKCS#12 file "
            << options->pkcs12_file_name() << endl;
      }

      std::string pemFileTemplate = "pmXXXXXX";
      std::string certFileTemplate = "ctXXXXXX";

      FILE *p12_file = fopen(options->pkcs12_file_name().c_str(), "rb");

      // read the pkcs12 file
      if (!p12_file) {
        Logging::log->getLog(LEVEL_ERROR) << "Error opening PKCS#12 file: "
            << options->pkcs12_file_name() << ". (file not found)" << endl;
        //TODO(mberlin): Use a better approach than exit - throw?
        exit(1);
      }
      PKCS12* p12 = d2i_PKCS12_fp(p12_file, NULL);
      fclose(p12_file);

      if (!p12) {
        Logging::log->getLog(LEVEL_ERROR) << "Error reading PKCS#12 file: "
            << options->pkcs12_file_name() << ". (no access rights?)" << endl;
        ERR_print_errors_fp(stderr);
        //TODO(mberlin): Use a better approach than exit - throw?
        exit(1);
      }

      EVP_PKEY* pkey = NULL;
      X509* cert = NULL;
      STACK_OF(X509)* ca = NULL;

      // parse pkcs12 file
      if (!PKCS12_parse(p12,
                        options->pkcs12_file_password().c_str(),
                        &pkey,
                        &cert,
                        &ca)) {
        Logging::log->getLog(LEVEL_ERROR) << "Error parsing PKCS#12 file: "
            << options->pkcs12_file_name()
            << " Please check if the supplied certificate password is correct."
            << endl;
        ERR_print_errors_fp(stderr);
        //TODO(mberlin): Use a better approach than exit - throw?
        exit(1);
      }
      PKCS12_free(p12);
      
      if (ca == NULL) {
        if (Logging::log->loggingActive(LEVEL_WARN)) {
          Logging::log->getLog(LEVEL_WARN) << "Expected one or more additional "
              "certificates in " << options->pkcs12_file_name() << " in order "
              "to verify the services' certificates." << endl;
        }
      } else if (sk_X509_num(ca) > 0) {
        // Setup any additional certificates as trusted root CAs in one file.   
        std::string trusted_cas_template("caXXXXXX");
        FILE* trusted_cas_file =
            create_and_open_temporary_ssl_file(&trusted_cas_template, "ab+");
        trustedCAsFileName = strdup(trusted_cas_template.c_str());
        
        if (Logging::log->loggingActive(LEVEL_INFO)) {
          Logging::log->getLog(LEVEL_INFO) << "Writing " << sk_X509_num(ca)
              << " verification certificates to " << trustedCAsFileName << endl;
        }
        
        while (sk_X509_num(ca) > 0) {
          X509* ca_cert = sk_X509_pop(ca);
          // _AUX writes trusted certificates.
          if (PEM_write_X509_AUX(trusted_cas_file, ca_cert)) { 
          } else {
            if (Logging::log->loggingActive(LEVEL_WARN)) {
              Logging::log->getLog(LEVEL_WARN) << "Error writing a CA to file "
                  << trusted_cas_template << ", continuing without it." << endl;
            }
          }
          X509_free(ca_cert);
        }
        
        fclose(trusted_cas_file);
      }
      sk_X509_free(ca);

      // create two tmp files containing the PEM certificates.
      // these which be deleted when exiting the program
      FILE* pemFile = create_and_open_temporary_ssl_file(&pemFileTemplate, "wb+");
      FILE* certFile = create_and_open_temporary_ssl_file(&certFileTemplate, "wb+");
      if (pemFile == NULL || certFile == NULL) {
        Logging::log->getLog(LEVEL_ERROR) << "Error creating temporary "
            "certificates" << endl;
        //TODO(mberlin): Use a better approach than exit - throw?
        exit(1);
      }

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG) << "tmp file name:"
            << pemFileTemplate << " " << certFileTemplate << endl;
      }

      // write private key
      // use the pkcs12 password as the pem password
      char* password = strdup(options->pkcs12_file_password().c_str());
      if (!PEM_write_PrivateKey(pemFile, pkey, NULL, NULL, 0, 0, password)) {
        Logging::log->getLog(LEVEL_ERROR)
            << "Error writing pem file:" << pemFileTemplate << endl;
        free(password);
        EVP_PKEY_free(pkey);

        unlink(pemFileTemplate.c_str());
        unlink(certFileTemplate.c_str());
        //TODO(mberlin): Use a better approach than exit - throw?
        exit(1);
      }
      free(password);
      EVP_PKEY_free(pkey);

      // write ca certificate
      if (!PEM_write_X509(certFile, cert)) {
        Logging::log->getLog(LEVEL_ERROR) << "Error writing cert file:"
            << certFileTemplate << endl;

        X509_free(cert);
        unlink(pemFileTemplate.c_str());
        unlink(certFileTemplate.c_str());
        //TODO(mberlin): Use a better approach than exit - throw?
        exit(1);
      }
      X509_free(cert);

      fclose(pemFile);
      fclose(certFile);

      pemFileName = strdup(pemFileTemplate.c_str());
      certFileName = strdup(certFileTemplate.c_str());

      ssl_context_->set_password_callback(
          boost::bind(&Client::get_pkcs12_password_callback, this));
      ssl_context_->use_private_key_file(pemFileName, boost::asio::ssl::context::pem);
      ssl_context_->use_certificate_chain_file(certFileName);

#if (BOOST_VERSION > 104601)
      // Use system default path for trusted root CAs and any supplied certificates.
      ssl_context_->set_default_verify_paths();
#endif  // BOOST_VERSION > 104601
      if (trustedCAsFileName != NULL) {
        ssl_context_->load_verify_file(trustedCAsFileName);
      }

      // FIXME(ps) make sure that the temporary files are deleted!
    } else if (!options->pem_file_name().empty()) {
      // otherwise use the pem files
      if (Logging::log->loggingActive(LEVEL_INFO)) {
        Logging::log->getLog(LEVEL_INFO) << "SSL support using PEM private key"
            " file " << options->pem_file_name() << endl;
      }

      try {
        ssl_context_->set_password_callback(
            boost::bind(&Client::get_pem_password_callback, this));
        ssl_context_->use_private_key_file(options->pem_file_name(),
                                           options->cert_format());
        ssl_context_->use_certificate_chain_file(options->pem_cert_name());
        
#if (BOOST_VERSION > 104601)
        // Use system default path for trusted root CAs and any supplied certificates.
        ssl_context_->set_default_verify_paths();
#endif  // BOOST_VERSION > 104601
        if (options->pem_trusted_certs_file_name().empty()) {
          if (Logging::log->loggingActive(LEVEL_WARN)) {
            Logging::log->getLog(LEVEL_WARN) << "Not using any additional "
                "certificates in order to verify the services' certificates."
                << endl;
          }
        } else {
          ssl_context_->load_verify_file(options->pem_trusted_certs_file_name());
        }
      } catch(invalid_argument& ia) {
         cerr << "Invalid argument: " << ia.what() << endl;
         cerr << "Please check your private key and certificate file."<< endl;
         //TODO(mberlin): Use a better approach than exit - throw?
         exit(1);
      }
    }

    // Cleanup thread-local OpenSSL state.
    ERR_free_strings();
#if (OPENSSL_VERSION_NUMBER < 0x1000000fL)
    ERR_remove_state(0);
#else  // OPENSSL_VERSION_NUMBER < 0x1000000fL
    ERR_remove_thread_state(NULL);
#endif  // OPENSSL_VERSION_NUMBER < 0x1000000fL
  }
}

std::string Client::get_pem_password_callback() const {
  return ssl_options->pem_file_password();
}

std::string Client::get_pkcs12_password_callback() const {
  return ssl_options->pkcs12_file_password();
}

#if (BOOST_VERSION > 104601)
bool Client::verify_certificate_callback(bool preverified,
                                         boost::asio::ssl::verify_context& context) const {
  X509_STORE_CTX *sctx = context.native_handle();
#else  // BOOST_VERSION > 104601
int verify_certificate_callback(int preverify_ok, X509_STORE_CTX *sctx) {   
  bool preverified = preverify_ok == 1;
  SSL *ssl = static_cast<SSL*>(X509_STORE_CTX_get_ex_data(
          sctx,
          SSL_get_ex_data_X509_STORE_CTX_idx()));
  SSL_CTX *ctx = SSL_get_SSL_CTX(ssl);
  SSLOptions *ssl_options = static_cast<SSLOptions*>(SSL_CTX_get_app_data(ctx));
#endif
  X509* cert = X509_STORE_CTX_get_current_cert(sctx);
  
  X509_NAME *subject_name = X509_get_subject_name(cert);
  BIO *subject_name_out = BIO_new(BIO_s_mem());
  X509_NAME_print_ex(subject_name_out, subject_name, 0, XN_FLAG_RFC2253);
  
  char *subject_start = NULL, *subject = NULL;
  long subject_length = BIO_get_mem_data(subject_name_out, &subject_start);
  subject = new char[subject_length + 1];
  memcpy(subject, subject_start, subject_length);
  subject[subject_length] = '\0';
  
  BIO_free(subject_name_out);
  
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "Verifying subject '" << subject
        << "'." << endl;
  }

  bool override = false;
  int sctx_error = X509_STORE_CTX_get_error(sctx);
  if (sctx_error != 0) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG) << "OpenSSL verify error: "
          << sctx_error << endl;
    }
    
    // Ignore error if verification is turned off in general or the error has
    // been disabled specifically.
    if (!ssl_options->verify_certificates() ||
        ssl_options->ignore_verify_error(sctx_error)) {
      if (Logging::log->loggingActive(LEVEL_WARN)) {
        Logging::log->getLog(LEVEL_WARN) << "Ignoring OpenSSL verify error: "
            << sctx_error << " because of user settings." << endl;
      }
      override = true;
    }
  }
  
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "Verification of subject '" << subject
        << "' was " << (preverified ? "successful." : "unsuccessful.")
        << ((!preverified && override) ? " Overriding because of user settings." : "")
        << endl;
  }
  
  delete[] subject;
  
#if (BOOST_VERSION > 104601)
  return preverified || override;
#else  // BOOST_VERSION > 104601
  return (preverified || override) ? 1 : 0;
#endif  // BOOST_VERSION > 104601
}
#endif  // HAS_OPENSSL

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
  uint32_t call_id = atomic_inc32(&callid_counter_);
  ClientRequest* request = new ClientRequest(address,
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

  boost::mutex::scoped_lock lock(requests_mutex_);
  if (stopped_) {
    lock.unlock();

    AbortClientRequest(request,
                       "Request aborted since RPC client was stopped.");
  } else {
    bool wasEmpty = requests_.empty();
    requests_.push(request);
    if (wasEmpty) {
      service_.post(boost::bind(&Client::sendInternalRequest, this));
    }
  }
}

void Client::sendInternalRequest() {
  if (stopped_ioservice_only_) {
    return;
  }
  // Process requests.
  do {
    ClientRequest *rq = NULL;
    {
      boost::mutex::scoped_lock lock(requests_mutex_);
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

      const std::string &addr = rq->address();
      int colonpos = addr.find_last_of(":");
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
                                     connect_timeout_s_
#ifdef HAS_OPENSSL
                                     ,use_gridssl_,
                                     ssl_context_
#endif  // HAS_OPENSSL
                                     );

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
  // Do nothing when the timer was canceled.
  if (error == boost::asio::error::operation_aborted
      || stopped_ioservice_only_) {
    return;
  }

  try {
    posix_time::ptime deadline = posix_time::microsec_clock::local_time()
        - posix_time::seconds(rq_timeout_s_);

    // Connections which have timed out requests have to be reset later.
    set<ClientConnection*> to_be_reset_cons;

    // Remove all timed out requests.
    request_map::iterator iter = request_table_.begin();
    while (iter != request_table_.end()) {
      ClientRequest* rq = iter->second;
      if (rq->time_sent() < deadline) {
        ClientConnection* respective_con = rq->client_connection();
        assert(respective_con);
        to_be_reset_cons.insert(respective_con);

        string error = "Request timed out (call id = "
            + boost::lexical_cast<string>(rq->call_id())
            + ", interface id = "
            + boost::lexical_cast<string>(rq->interface_id())
            + ", proc id = " + boost::lexical_cast<string>(rq->proc_id())
            + ", server = " + respective_con->GetServerAddress()
            + ").";
        RPCHeader::ErrorResponse* err = new RPCHeader::ErrorResponse();
        err->set_error_message(error);
        err->set_error_type(IO_ERROR);
        err->set_posix_errno(POSIX_ERROR_EINVAL);
        rq->set_error(err);
        rq->ExecuteCallback();
        request_table_.erase(iter++);
        if (Logging::log->loggingActive(LEVEL_INFO)) {
          Logging::log->getLog(LEVEL_INFO) << error << endl;
        }
      } else {
        ++iter;
      }
    }

    // Reset all connections which had timed out requests.
    for (set<ClientConnection*>::iterator iter = to_be_reset_cons.begin();
         iter != to_be_reset_cons.end();
         iter++) {
      // Since this request timed out, its callback will be executed.
      // The callback may delete rq.rq_data() while boost::asio is still
      // trying to send this data. To avoid possible segmentation faults,
      // all pending boost::asio async_write for the request's connection
      // are aborted by closing the connection. This is the only portable
      // way to cancel a pending request.
      // See the remarks here: http://www.boost.org/doc/libs/1_45_0/doc/html/boost_asio/reference/basic_stream_socket/cancel/overload2.html  // NOLINT
      // Closing the connection would be required anyway if the timeout
      // was caused by a network connection problem which would result in
      // an aborted TCP connection. Only, if the time out was caused by
      // an overloaded server, we would close the connection when it was
      // not needed.
      string error = "Another request of this requests's connection timed out. "
          "Therefore the connection had to be closed and this request aborted.";
      (*iter)->Reset();
      (*iter)->SendError(POSIX_ERROR_EIO, error);
    }

    // Close inactive connections.
    posix_time::ptime linger_deadline = posix_time::microsec_clock::local_time()
        - posix_time::seconds(max_con_linger_);

    connection_map::iterator iter2 = connections_.begin();
    while (iter2 != connections_.end()) {
      ClientConnection* con = iter2->second;
      assert(con != NULL);
      if (con->last_used() < linger_deadline) {
        string error = "Connection was inactive for more than "
            + boost::lexical_cast<string>(max_con_linger_)
            + " seconds.";
        if (Logging::log->loggingActive(LEVEL_INFO)) {
          Logging::log->getLog(LEVEL_INFO) << "Closing connection to '"
              << iter2->first << "' since it " << error.substr(11) << endl;
        }
        con->Close(error);
        delete con;
        connections_.erase(iter2++);
      } else {
        ++iter2;
      }
    }
  } catch (std::exception &e) {
    Logging::log->getLog(LEVEL_ERROR) << "An exception occurred while checking"
        " for timed out requests and connections: " << e.what() << endl;
  }
  rq_timeout_timer_.expires_from_now(posix_time::seconds(rq_timeout_s_));
  rq_timeout_timer_.async_wait(boost::bind(&Client::handleTimeout,
                                           this,
                                           asio::placeholders::error));
}

void Client::AbortClientRequest(ClientRequest* request,
                                const std::string& error) {
  // Error response for canceled requests.
  POSIXErrno posix_errno = POSIX_ERROR_EIO;

  RPCHeader::ErrorResponse err;
  err.set_error_type(IO_ERROR);
  err.set_posix_errno(posix_errno);
  err.set_error_message(error);

  request->set_error(new RPCHeader::ErrorResponse(err));
  request->ExecuteCallback();

  Logging::log->getLog(LEVEL_ERROR)
      << "operation failed: errno=" << posix_errno
      << " message=" << error << endl;
}

void Client::run() {
  rq_timeout_timer_.expires_from_now(posix_time::seconds(rq_timeout_s_));
  rq_timeout_timer_.async_wait(boost::bind(&Client::handleTimeout,
                                           this,
                                           asio::placeholders::error));

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "Starting RPC client." << endl;
#ifndef HAS_OPENSSL
    Logging::log->getLog(LEVEL_DEBUG) << "Running in plain TCP mode."<< endl;
#else
    if (ssl_context_ != NULL) {
      if (use_gridssl_) {
        Logging::log->getLog(LEVEL_DEBUG) << "Running in GRID SSL mode." << endl;
      } else {
        Logging::log->getLog(LEVEL_DEBUG) << "Running in SSL mode." << endl;
      }
    } else {
      Logging::log->getLog(LEVEL_DEBUG) << "Running in plain TCP mode."<< endl;
    }
#endif  // !HAS_OPENSSL
  }

  // Does not return as long as there are running timers (e.g.,
  // rq_timeout_timer_) or pending boost::asio callbacks.
  service_.run();

  // Delete the ClientConnection object of all open connections.
  for (connection_map::iterator iter = connections_.begin();
       iter != connections_.end();
       ++iter) {
    delete iter->second;
  }
  connections_.clear();

  // A request may not have made it from requests_ to request_table_. Cancel
  // those, too.
  {
    boost::mutex::scoped_lock lock(requests_mutex_);
    while (requests_.size()) {
      ClientRequest* request = requests_.front();
      requests_.pop();

      AbortClientRequest(request,
                         "Request aborted since RPC client was stopped.");
    }
  }

  // Delete requests which were successfully sent, but not response was received
  // for them.
  for (request_map::iterator iter = request_table_.begin();
       iter != request_table_.end();
       ++iter) {
    AbortClientRequest(iter->second,
                       "Request aborted since RPC client was stopped.");
  }
  request_table_.clear();

#ifdef HAS_OPENSSL
  // Cleanup thread-local OpenSSL state.
  ERR_remove_state(0);
#endif  // HAS_OPENSSL
}

void Client::shutdown() {
  bool already_stopped = false;
  {
    boost::mutex::scoped_lock lock(requests_mutex_);
    already_stopped = stopped_;
    stopped_ = true;
  }

  if (!already_stopped) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG) << "RPC client stopped." << endl;
    }
    service_.post(boost::bind(&Client::ShutdownHandler, this));
  } else {
    if (Logging::log->loggingActive(LEVEL_WARN)) {
      Logging::log->getLog(LEVEL_WARN)
          << "Tried to stop the RPC client although it was already stopped."
          << endl;
    }
  }
}

void Client::ShutdownHandler() {
  stopped_ioservice_only_ = true;
  rq_timeout_timer_.cancel();

  for (connection_map::iterator iter = connections_.begin();
       iter != connections_.end();
       ++iter) {
    ClientConnection *con = iter->second;
    assert(con != NULL);
    con->Close("RPC client was stopped.");
  }
}

FILE* Client::create_and_open_temporary_ssl_file(std::string *filename_template,
                                                 const char* mode) {
  if (filename_template == NULL || mode == NULL) {
    return NULL;
  }
#ifdef WIN32
  // Gets the temp path env string (no guarantee it's a valid path).
  char temp_path[MAX_PATH];
  char filename_temp[MAX_PATH];

  DWORD dwRetVal = 0;
  dwRetVal = GetTempPath(MAX_PATH,    // length of the buffer
                         LPTSTR(temp_path));  // buffer for path
  if (dwRetVal > MAX_PATH || (dwRetVal == 0)) {
    strncpy(temp_path, ".", 1);
  }

  //  Generates a temporary file name.
  if (!GetTempFileName(LPTSTR(temp_path),		  // directory for tmp files
                       TEXT("xfs"),               // temp file name prefix, max 3 char
                       0,                         // create unique name
                       LPWSTR(filename_temp))) {  // buffer for name
    std::cerr << "Couldn't create temp file name.\n";
    return NULL;
  }
  *filename_template = std::string(filename_temp);
  return _tfopen(LPTSTR(filename_temp), LPTSTR(mode));
#else
  // Place file in TMPDIR or /tmp if not specified as absolute path.
  std::string filename = *filename_template;
  if (!boost::algorithm::starts_with<std::string, std::string>(filename, "/")) {
    char *tmpdir = getenv("TMPDIR");
    if (tmpdir != NULL) {
      std::string tmp(tmpdir);
      if (!boost::algorithm::ends_with(tmp, "/")) {
        tmp += "/";
      }
      filename = tmp + filename;
    } else {
      filename = "/tmp/" + filename;
    }
  }
  
  char *temporary_filename = strdup(filename.c_str());
  int tmp = mkstemp(temporary_filename);
  if (tmp == -1) {
    std::cerr << "Couldn't create temp file name.\n";
    free(temporary_filename);
    return NULL;
  }
  
  *filename_template = std::string(temporary_filename);
  free(temporary_filename);
  return fdopen(tmp, mode);
#endif  // WIN32
}

#ifdef HAS_OPENSSL
    boost::asio::ssl::context_base::method Client::string_to_ssl_method(
        std::string method_string,
        boost::asio::ssl::context_base::method default_method) {
      if (method_string == "sslv3") {
        return boost::asio::ssl::context_base::sslv3_client;
      } else if (method_string == "ssltls") {
        return boost::asio::ssl::context_base::sslv23_client;
      } else if (method_string == "tlsv1") {
        return boost::asio::ssl::context_base::tlsv1_client;
      }
#if (BOOST_VERSION > 105300)
      else if (method_string == "tlsv11") {
        return boost::asio::ssl::context_base::tlsv11_client;
      } else if (method_string == "tlsv12") {
        return boost::asio::ssl::context_base::tlsv12_client;
      }
#endif  // BOOST_VERSION > 105300
      else {
        if (Logging::log->loggingActive(LEVEL_WARN)) {
          Logging::log->getLog(LEVEL_WARN) << "Unknown SSL method: '"
              << method_string << "', using default." << endl;
        }
        return default_method;
      }
    }
#endif  // HAS_OPENSSL

Client::~Client() {
#ifdef HAS_OPENSSL
  // remove temporary cert and pem files
  if (pemFileName != NULL) {
    unlink(pemFileName);
  }
  if (certFileName != NULL) {
    unlink(certFileName);
  }
  if (trustedCAsFileName != NULL) {
    unlink(trustedCAsFileName);
  }

  // strdup initialized.
  free(pemFileName);
  free(certFileName);
  free(trustedCAsFileName);

  if (ssl_options) {
    ERR_remove_state(0);
    ERR_free_strings();
  }
  delete ssl_options;
  delete ssl_context_;
#endif  // HAS_OPENSSL
}

#ifdef _MSC_VER
#pragma warning(pop)
#endif  // _MSC_VER

}  // namespace rpc
}  // namespace xtreemfs
