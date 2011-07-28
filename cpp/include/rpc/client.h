/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_CLIENT_H_
#define CPP_INCLUDE_RPC_CLIENT_H_

#include <boost/asio.hpp>
#include <boost/asio/ssl.hpp>
#include <boost/cstdint.hpp>
#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/system/error_code.hpp>
#include <boost/thread/mutex.hpp>
#include <queue>
#include <string>

#include "rpc/client_connection.h"
#include "rpc/client_request.h"
#include "rpc/ssl_options.h"

#if (BOOST_VERSION / 100000 > 1) || (BOOST_VERSION / 100 % 1000 > 35)
#include <boost/unordered_map.hpp>
#else
#include <map>
#endif

namespace xtreemfs {
namespace rpc {
using std::string;
using boost::int32_t;
using boost::uint32_t;

// Boost introduced unordered_map in version 1.36 but we need to support
// older versions for Debian 5.
// TODO(bjko): Remove this typedef when support for Debian 5 is dropped.
#if (BOOST_VERSION / 100000 > 1) || (BOOST_VERSION / 100 % 1000 > 35)
typedef boost::unordered_map<string, ClientConnection*> connection_map;
#else
typedef std::map<string, ClientConnection*> connection_map;
#endif

class Client {
 public:
  Client(int32_t connect_timeout_s,
         int32_t request_timeout_s,
         int32_t max_con_linger,
         const SSLOptions* options);

  virtual ~Client();

  void run();

  void shutdown();

  void sendRequest(const string& address,
                   int32_t interface_id,
                   int32_t proc_id,
                   const xtreemfs::pbrpc::UserCredentials& userCreds,
                   const xtreemfs::pbrpc::Auth& auth,
                   const google::protobuf::Message* message,
                   const char* data,
                   int data_length,
                   google::protobuf::Message* response_message,
                   void* context,
                   ClientRequestCallbackInterface *callback);

 private:
  void handleTimeout(const boost::system::error_code& error);

  void sendInternalRequest();

  std::string get_pem_password_callback() const;
  std::string get_pkcs12_password_callback() const;

  boost::asio::io_service service_;
  boost::asio::ip::tcp::resolver resolver_;
  bool use_gridssl_;
  boost::asio::ssl::context* ssl_context_;
  boost::mutex requests_lock_;
  connection_map connections_;
  request_map request_table_;
  std::queue<ClientRequest*> requests_;
  boost::uint32_t callid_counter_;
  boost::asio::deadline_timer rq_timeout_timer_;
  boost::int32_t rq_timeout_s_;
  boost::int32_t connect_timeout_s_;
  boost::int32_t max_con_linger_;

  const SSLOptions* ssl_options;

  char* pemFileName;
  char* certFileName;
};
}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_CLIENT_H_

