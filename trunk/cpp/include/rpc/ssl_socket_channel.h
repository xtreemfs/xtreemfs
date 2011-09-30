/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_SSL_SOCKET_CHANNEL_H_
#define CPP_INCLUDE_RPC_SSL_SOCKET_CHANNEL_H_

#include <boost/system/error_code.hpp>
#include <boost/asio.hpp>
#include <boost/asio/ssl.hpp>

#include <vector>

#include "pbrpc/RPC.pb.h"
#include "rpc/abstract_socket_channel.h"

namespace xtreemfs {
namespace rpc {

class SSLSocketChannel : public AbstractSocketChannel {
 public:
  SSLSocketChannel(boost::asio::io_service& service,
                   boost::asio::ssl::context& context)
      : ssl_stream_(service, context) {
  }

  virtual ~SSLSocketChannel() {
  }

  virtual void async_connect(
      const boost::asio::ip::tcp::endpoint& peer_endpoint,
      ConnectHandler handler) {
    connect_handler_ = handler;
    ssl_stream_.lowest_layer().async_connect(
        peer_endpoint,
        boost::bind(&SSLSocketChannel::internal_do_handshake,
                    this,
                    boost::asio::placeholders::error));
  }

  void internal_do_handshake(const boost::system::error_code& error) {
    if (error) {
      connect_handler_(error);
    } else {
      ssl_stream_.async_handshake(
          boost::asio::ssl::stream<boost::asio::ip::tcp::socket>::client,
          connect_handler_);
    }
  }

  virtual void async_read(
      const std::vector<boost::asio::mutable_buffer>& buffers,
      ReadWriteHandler handler) {
    boost::asio::async_read(ssl_stream_, buffers, handler);
  }

  virtual void async_read(
      const boost::asio::mutable_buffers_1& buffer,
      ReadWriteHandler handler) {
    boost::asio::async_read(ssl_stream_, buffer, handler);
  }

  virtual void async_write(
      const std::vector<boost::asio::const_buffer> & buffers,
      ReadWriteHandler handler) {
    boost::asio::async_write(ssl_stream_, buffers, handler);
  }

  virtual void close() {
    ssl_stream_.lowest_layer().close();
  }

 private:
  boost::asio::ssl::stream<boost::asio::ip::tcp::socket> ssl_stream_;
  ConnectHandler connect_handler_;
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_SSL_SOCKET_CHANNEL_H_

