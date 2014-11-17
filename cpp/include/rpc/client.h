/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *                    2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_CLIENT_H_
#define CPP_INCLUDE_RPC_CLIENT_H_

#include <stdint.h>

#include <boost/asio.hpp>
#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/system/error_code.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/version.hpp>
#include <gtest/gtest_prod.h>
#include <queue>
#include <string>

#include "rpc/client_connection.h"
#include "rpc/client_request.h"
#include "rpc/ssl_options.h"

#ifdef HAS_OPENSSL
#include <boost/asio/ssl.hpp>
#endif  // HAS_OPENSSL

#if (BOOST_VERSION / 100000 > 1) || (BOOST_VERSION / 100 % 1000 > 35)
#include <boost/unordered_map.hpp>
#else
#include <map>
#endif

namespace xtreemfs {
namespace rpc {

// Boost introduced unordered_map in version 1.36 but we need to support
// older versions for Debian 5.
// TODO(bjko): Remove this typedef when support for Debian 5 is dropped.
#if (BOOST_VERSION / 100000 > 1) || (BOOST_VERSION / 100 % 1000 > 35)
typedef boost::unordered_map<std::string, ClientConnection*> connection_map;
#else
typedef std::map<std::string, ClientConnection*> connection_map;
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

  void sendRequest(const std::string& address,
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
  /** Helper function which aborts a ClientRequest with "error".
   *
   * @remarks    Ownership of "request" is not transferred.
   */
  void AbortClientRequest(ClientRequest* request, const std::string& error);

  void handleTimeout(const boost::system::error_code& error);

  void sendInternalRequest();

  void ShutdownHandler();

  boost::asio::io_service service_;

  connection_map connections_;
  /** Contains all pending requests which are uniquely identified by their
   *  call id.
   *
   *  Requests to this table are added when sending them and removed by the
   *  handleTimeout() function and the callback processing.
   *
   *  @remark All accesses to this object have to be executed in the context of
   *          service_ and therefore do not require further synchronization.
   */
  request_map request_table_;
  /** Guards access to requests_ and stopped_. */
  boost::mutex requests_mutex_;
  /** Global queue where all requests queue up before the required
   *  ClientConnection is available.
   *
   *  Once a ClientRequest was removed from this queue, it will be added to the
   *  requests_table_ and the queue ClientConnection::requests_.
   */
  std::queue<ClientRequest*> requests_;
  /** True when the RPC client was stopped and no new requests are accepted. */
  bool stopped_;
  /** True when the RPC client was stopped, only accessed in the context of
   *  io_service::run. */
  bool stopped_ioservice_only_;
  uint32_t callid_counter_;
  boost::asio::deadline_timer rq_timeout_timer_;
  int32_t rq_timeout_s_;
  int32_t connect_timeout_s_;
  int32_t max_con_linger_;

#ifdef HAS_OPENSSL
  std::string get_pem_password_callback() const;
  std::string get_pkcs12_password_callback() const;
  
  bool verify_certificate_callback(bool preverfied,
                                   boost::asio::ssl::verify_context& context) const;

  bool use_gridssl_;
  const SSLOptions* ssl_options;
  char* pemFileName;
  char* certFileName;
  boost::asio::ssl::context* ssl_context_;
#endif  // HAS_OPENSSL

  FRIEND_TEST(ClientTestFastLingerTimeout, LingerTests);
  FRIEND_TEST(ClientTestFastLingerTimeoutConnectTimeout, LingerTests);
};
}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_CLIENT_H_
