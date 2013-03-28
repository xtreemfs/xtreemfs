/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *                    2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_CLIENT_CONNECTION_H_
#define CPP_INCLUDE_RPC_CLIENT_CONNECTION_H_

#include <stdint.h>

#include <boost/asio.hpp>
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

// Boost introduced unordered_map in version 1.36 but we need to support
// older versions for Debian 5.
// TODO(bjko): Remove this typedef when support for Debian 5 is dropped.
#if (BOOST_VERSION / 100000 > 1) || (BOOST_VERSION / 100 % 1000 > 35)
typedef boost::unordered_map<int32_t, ClientRequest*> request_map;
#else
typedef std::map<int32_t, ClientRequest*> request_map;
#endif

/** Created by xtreemfs::rpc::Client for every connection.
 *
 * This class contains the per-connection data.
 *
 * @remarks Special care has to be taken regarding the boost::asio callback
 *          functions. In particular, every callback must not access members
 *          when the error_code equals asio::error::operation_aborted.
 *          Additionally, no further actions must be taken when
 *          connection_state_ is set to CLOSED.
 */
class ClientConnection {
 public:
  struct PendingRequest {
    PendingRequest(uint32_t call_id, ClientRequest* rq)
        : call_id(call_id), rq(rq) {}

    uint32_t call_id;
    ClientRequest* rq;
  };

  ClientConnection(const std::string& server_name,
                   const std::string& port,
                   boost::asio::io_service& service,
                   request_map *request_table,
                   int32_t connect_timeout_s,
                   int32_t max_reconnect_interval_s,
                   bool use_gridssl,
                   boost::asio::ssl::context* ssl_context);

  virtual ~ClientConnection();

  void DoProcess();
  void AddRequest(ClientRequest *request);
  void Close(const std::string& error);
  void SendError(xtreemfs::pbrpc::POSIXErrno posix_errno,
                 const std::string& error_message);
  void Reset();

  boost::posix_time::ptime last_used() const {
      return last_used_;
  }

  std::string GetServerAddress() const {
    return server_name_ + ":" + server_port_;
  }

 private:
  enum State {
    CONNECTING,
    IDLE,
    ACTIVE,
    CLOSED,
    WAIT_FOR_RECONNECT
  };

  RecordMarker *receive_marker_;
  char *receive_hdr_, *receive_msg_, *receive_data_;

  char *receive_marker_buffer_;

  State connection_state_;
  /** Queue of requests which have not been sent out yet. */
  std::queue<PendingRequest> requests_;
  ClientRequest* current_request_;

  const std::string server_name_;
  const std::string server_port_;
  boost::asio::io_service &service_;
  boost::asio::ip::tcp::resolver resolver_;
  AbstractSocketChannel* socket_;

  boost::asio::ip::tcp::endpoint* endpoint_;
  /** Points to the Client's request_table_. */
  request_map* request_table_;
  boost::asio::deadline_timer timer_;
  const int32_t connect_timeout_s_;
  const int32_t max_reconnect_interval_s_;
  boost::posix_time::ptime next_reconnect_at_;
  boost::posix_time::ptime last_connect_was_at_;
  int32_t reconnect_interval_s_;
  boost::posix_time::ptime last_used_;

  bool use_gridssl_;
  boost::asio::ssl::context* ssl_context_;

  /** Deletes "socket".
   *
   * @remark    Ownership of "socket" is transferred.
   */
  void static DelayedSocketDeletionHandler(AbstractSocketChannel* socket);

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
  void DeleteInternalBuffers();
  void CreateChannel();
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_CLIENT_CONNECTION_H_

