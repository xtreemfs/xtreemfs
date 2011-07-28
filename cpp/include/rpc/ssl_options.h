/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_SSL_OPTIONS_H_
#define CPP_INCLUDE_RPC_SSL_OPTIONS_H_

#include <boost/asio/ssl.hpp>

#include <string>

namespace xtreemfs {
namespace rpc {

class SSLOptions {
 public:
  SSLOptions(const std::string ssl_pem_path,
             const std::string ssl_pem_cert_path,
             const std::string ssl_pem_key_pass,
             const std::string ssl_pkcs12_path,
             const std::string ssl_pkcs12_pass,
             const boost::asio::ssl::context::file_format format,
             const bool use_grid_ssl)
     : pem_file_name_(ssl_pem_path),
       pem_file_pass_(ssl_pem_key_pass),
       pem_cert_name_(ssl_pem_cert_path),
       pkcs12_file_name_(ssl_pkcs12_path),
       pkcs12_file_pass_(ssl_pkcs12_pass),
       cert_format_(format),
       use_grid_ssl_(use_grid_ssl) {}

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

 private:
  std::string pem_file_name_;
  std::string pem_file_pass_;
  std::string pem_cert_name_;
  std::string pkcs12_file_name_;
  std::string pkcs12_file_pass_;

  boost::asio::ssl::context::file_format cert_format_;
  bool use_grid_ssl_;
};
}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_SSL_OPTIONS_H_

