/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_TCP_SOCKET_CHANNEL_H_
#define CPP_INCLUDE_RPC_TCP_SOCKET_CHANNEL_H_

#include <boost/asio.hpp>
#include <boost/system/error_code.hpp>

#include <vector>

#include "pbrpc/RPC.pb.h"
#include "rpc/abstract_socket_channel.h"

namespace xtreemfs {
namespace rpc {

class TCPSocketChannel : public AbstractSocketChannel {
 public:
  explicit TCPSocketChannel(boost::asio::io_service& service) {
    socket_ = new boost::asio::ip::tcp::socket(service);
  }

  virtual ~TCPSocketChannel() {
    delete socket_;
  }

  virtual void async_connect(
      const boost::asio::ip::tcp::endpoint& peer_endpoint,
      ConnectHandler handler) {
    socket_->async_connect(peer_endpoint, handler);
  }

  virtual void async_read(
      const std::vector<boost::asio::mutable_buffer>& buffers,
      ReadWriteHandler handler) {
    boost::asio::async_read(*socket_, buffers, handler);
  }

  virtual void async_read(
      const boost::asio::mutable_buffers_1& buffer,
      ReadWriteHandler handler) {
    boost::asio::async_read(*socket_, buffer, handler);
  }

  virtual void async_write(
      const std::vector<boost::asio::const_buffer> & buffers,
      ReadWriteHandler handler) {
    boost::asio::async_write(*socket_, buffers, handler);
  }

  virtual void close() {
    socket_->close();
  }

 protected:
  boost::asio::ip::tcp::socket *socket_;
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_TCP_SOCKET_CHANNEL_H_

