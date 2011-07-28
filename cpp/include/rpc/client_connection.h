/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_CLIENT_CONNECTION_H_
#define CPP_INCLUDE_RPC_CLIENT_CONNECTION_H_

#include <boost/asio.hpp>
#include <boost/cstdint.hpp>
#include <boost/function.hpp>
#include <boost/system/error_code.hpp>
#include <boost/version.hpp>
#include <queue>
#include <string>

#include "pbrpc/RPC.pb.h"
#include "rpc/abstract_socket_channel.h"
#include "rpc/client_request.h"
#include "rpc/record_marker.h"
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
typedef boost::unordered_map<int32_t, ClientRequest*> request_map;
#else
typedef std::map<int32_t, ClientRequest*> request_map;
#endif

class ClientConnection {
 public:
  enum State {
    CONNECTING,
    IDLE,
    ACTIVE,
    CLOSED,
    WAIT_FOR_RECONNECT
  };

  ClientConnection(const string& server_name,
                   const string& port,
                   boost::asio::io_service& service,
                   request_map *request_table,
                   int32_t connect_timeout_s,
                   int32_t max_reconnect_interval_s,
                   bool use_gridssl,
                   boost::asio::ssl::context* ssl_context);

  virtual ~ClientConnection();

  void DoProcess();
  void AddRequest(ClientRequest *request);
  void Close();

  boost::posix_time::ptime last_used() const {
      return last_used_;
  }

 private:
  RecordMarker *receive_marker_;
  char *receive_hdr_, *receive_msg_, *receive_data_;

  char *receive_marker_buffer_;

  State connection_state_;
  std::queue<ClientRequest*> requests_;
  ClientRequest* current_request_;

  const string server_name_;
  const string server_port_;
  boost::asio::io_service &service_;
  boost::asio::ip::tcp::resolver resolver_;
  AbstractSocketChannel *socket_;

  boost::asio::ip::tcp::endpoint *endpoint_;
  request_map *request_table_;
  boost::asio::deadline_timer timer_;
  int32_t connect_timeout_s_;
  int32_t max_reconnect_interval_s_;
  boost::posix_time::ptime next_reconnect_at_;
  int32_t reconnect_interval_s_;
  boost::posix_time::ptime last_used_;

  bool use_gridssl_;
  boost::asio::ssl::context* ssl_context_;

  void Connect();
  void SendRequest();
  void ReceiveRequest();
  void PostResolve(const boost::system::error_code& err,
          boost::asio::ip::tcp::resolver::iterator endpoint_iterator);
  void PostConnect(const boost::system::error_code& err,
          boost::asio::ip::tcp::resolver::iterator endpoint_iterator);
  void OnConnectTimeout(const boost::system::error_code& err);
  void PostReadMessage(const boost::system::error_code& err);
  void PostReadRecordMarker(const boost::system::error_code& err);
  void PostWrite(const boost::system::error_code& err,
          std::size_t bytes_written);

  void SendError(xtreemfs::pbrpc::POSIXErrno posix_errno,
          const string &error_message);
  void Reset();
  void DeleteInternalBuffers();

  void CreateChannel();
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_CLIENT_CONNECTION_H_

