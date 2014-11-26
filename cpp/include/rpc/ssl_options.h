/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_SSL_OPTIONS_H_
#define CPP_INCLUDE_RPC_SSL_OPTIONS_H_

#ifdef HAS_OPENSSL
#include <boost/asio/ssl.hpp>
#endif  // HAS_OPENSSL

#include <algorithm>  // std::find
#include <string>
#include <vector>

namespace xtreemfs {
namespace rpc {

class SSLOptions {
#ifdef HAS_OPENSSL
 public:
  SSLOptions(const std::string ssl_pem_path,
             const std::string ssl_pem_cert_path,
             const std::string ssl_pem_key_pass,
             const std::string ssl_pem_trusted_certs_path,
             const std::string ssl_pkcs12_path,
             const std::string ssl_pkcs12_pass,
             const boost::asio::ssl::context::file_format format,
             const bool use_grid_ssl,
             const bool ssl_verify_certificates,
             const std::vector<int> ssl_ignore_verify_errors,
             const std::string ssl_min_method_string)
     : pem_file_name_(ssl_pem_path),
       pem_file_pass_(ssl_pem_key_pass),
       pem_cert_name_(ssl_pem_cert_path),
       pem_trusted_certs_file_name_(ssl_pem_trusted_certs_path),
       pkcs12_file_name_(ssl_pkcs12_path),
       pkcs12_file_pass_(ssl_pkcs12_pass),
       cert_format_(format),
       use_grid_ssl_(use_grid_ssl),
       verify_certificates_(ssl_verify_certificates),
       ignore_verify_errors_(ssl_ignore_verify_errors),
       min_method_string_(ssl_min_method_string) {}

  virtual ~SSLOptions() {
  }

  std::string pem_file_name() const {
    return pem_file_name_;
  }

  std::string pem_cert_name() const {
    return pem_cert_name_;
  }

  std::string pem_file_password() const {
    return pem_file_pass_;
  }
  
  std::string pem_trusted_certs_file_name() const {
    return pem_trusted_certs_file_name_;
  }

  std::string pkcs12_file_name() const {
    return pkcs12_file_name_;
  }

  std::string pkcs12_file_password() const {
    return pkcs12_file_pass_;
  }

  boost::asio::ssl::context::file_format cert_format() const {
    return cert_format_;
  }

  bool use_grid_ssl() const {
    return use_grid_ssl_;
  }
  
  bool verify_certificates() const {
    return verify_certificates_;
  }
  
  bool ignore_verify_error(int verify_error) const {
    return std::find(ignore_verify_errors_.begin(),
                     ignore_verify_errors_.end(),
                     verify_error) != ignore_verify_errors_.end();
  }
  
  std::string min_method_string() const {
    return min_method_string_;
  }

 private:
  std::string pem_file_name_;
  std::string pem_file_pass_;
  std::string pem_cert_name_;
  std::string pem_trusted_certs_file_name_;
  std::string pkcs12_file_name_;
  std::string pkcs12_file_pass_;

  boost::asio::ssl::context::file_format cert_format_;
  bool use_grid_ssl_;
  bool verify_certificates_;
  std::vector<int> ignore_verify_errors_;
  std::string min_method_string_;
#endif  // HAS_OPENSSL
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_SSL_OPTIONS_H_

