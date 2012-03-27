/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_RPC_SERVER_DUMMY_H_
#define CPP_TEST_COMMON_RPC_SERVER_DUMMY_H_

#include <boost/asio.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/smart_ptr.hpp>

namespace boost {
class thread;
}

namespace xtreemfs {
namespace pbrpc {

class RPCServerDummy {
 public:
  /** Starts the RPC Server at "port". */
  void Start(short port);

  /** Stops the server again. */
  void Stop();

 private:
  /** Daemon function which accepts new connections. */
  void Run(short port);

  /** Connection handler. */
  void Session(boost::shared_ptr<boost::asio::ip::tcp::socket> sock);

  boost::scoped_ptr<boost::thread> daemon_;

  boost::asio::io_service io_service;

  // TODO(mberlin): Remove me.
  const static int max_length = 1024;
};

}  // namespace pbrpc
}  // namespace xtreemfs

#endif  // CPP_TEST_COMMON_RPC_SERVER_DUMMY_H_
