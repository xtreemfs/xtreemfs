/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "common/rpc_server_dummy.h"

#include <iostream>
#include <boost/bind.hpp>
#include <boost/thread/thread.hpp>

using boost::asio::ip::tcp;

namespace xtreemfs {
namespace pbrpc {

void RPCServerDummy::Session(
    boost::shared_ptr<boost::asio::ip::tcp::socket> sock) {
  try {
    for (;;) {
      char data[max_length];

      boost::system::error_code error;
      size_t length = sock->read_some(boost::asio::buffer(data), error);
      if (error == boost::asio::error::eof) {
        break; // Connection closed cleanly by peer.
      } else if (error) {
        throw boost::system::system_error(error); // Some other error.
      }

      // Process package.

      // Send response.
      boost::asio::write(*sock, boost::asio::buffer(data, length));
    }
  } catch (const std::exception& e) {
    std::cerr << "Exception in thread: " << e.what() << "\n";
  }
}

void RPCServerDummy::Run(short port) {
  tcp::acceptor a(io_service, tcp::endpoint(tcp::v4(), port));
  for (;;) {
    boost::shared_ptr<tcp::socket> sock(new tcp::socket(io_service));
    a.accept(*sock);
    boost::thread t(boost::bind(&RPCServerDummy::Session, this, sock));
  }
}

void RPCServerDummy::Start(short port) {
  daemon_.reset(new boost::thread(boost::bind(&RPCServerDummy::Run,
                                              this,
                                              port)));
}

void RPCServerDummy::Stop() {
  daemon_->interrupt();
  daemon_->join();
}

}  // namespace pbrpc
}  // namespace xtreemfs
