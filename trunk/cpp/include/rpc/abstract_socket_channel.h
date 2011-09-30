/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_ABSTRACT_SOCKET_CHANNEL_H_
#define CPP_INCLUDE_RPC_ABSTRACT_SOCKET_CHANNEL_H_

#include <boost/function.hpp>
#include <boost/asio.hpp>
#include <boost/asio/ssl.hpp>
#include <boost/system/error_code.hpp>

#include <vector>

namespace xtreemfs {
namespace rpc {

typedef boost::function2<void, const boost::system::error_code&,
std::size_t> ReadWriteHandler;

typedef boost::function1<void, const boost::system::error_code&>
ConnectHandler;

class AbstractSocketChannel {
 public:
  virtual ~AbstractSocketChannel() {}

  virtual void async_connect(
      const boost::asio::ip::tcp::endpoint& peer_endpoint,
      ConnectHandler handler) = 0;

  virtual void async_read(
      const std::vector<boost::asio::mutable_buffer>& buffers,
      ReadWriteHandler handler) = 0;

  virtual void async_read(
      const boost::asio::mutable_buffers_1& buffer,
      ReadWriteHandler handler) = 0;

  virtual void async_write(
      const std::vector<boost::asio::const_buffer> & buffers,
      ReadWriteHandler handler) = 0;

  virtual void close() = 0;
};

}  // namespace rpc
}  // namespace xtreemfs
#endif  // CPP_INCLUDE_RPC_ABSTRACT_SOCKET_CHANNEL_H_

