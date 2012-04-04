/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_TEST_RPC_SERVER_H_
#define CPP_TEST_COMMON_TEST_RPC_SERVER_H_

#include <boost/asio.hpp>
#include <boost/cstdint.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/smart_ptr.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include <map>

namespace google {
namespace protobuf {
class Message;
}  // namespace protobuf
}  // namespace google

namespace xtreemfs {
namespace rpc {

/** Base class which allows to implement a test XtreemFS server.
 *
 * Implementations have to set interface_id_ accordingly and add
 * implemented operations to operations_.
 */
class TestRPCServer {
 public:
  /** Special value for to_be_dropped_request_by_proc_id_. */
  static const boost::uint32_t kDropRequestByProcIDDisable = 0;

  /** Special value for to_be_dropped_request_by_proc_id_. */
  static const boost::uint32_t kDropRequestByProcIDNewConnection = 12345;

  TestRPCServer();

  virtual ~TestRPCServer() {}

  /** Starts the RPC Server at free, random port. False if not successful. */
  bool Start();

  /** Stops the server again. */
  void Stop();

  /** Returns the address of the service in the format "ip-address:port". */
  std::string GetAddress();

  /** Ignore the next "count" requests. */
  void DropNextRequests(int count);

  /** Ignore the next request which has "proc_id". */
  void DropRequestByProcId(boost::uint32_t proc_id);

 protected:
  /** Function pointer an implemented server operation. */
  typedef google::protobuf::Message* (*Operation)(
      const google::protobuf::Message& request);

  /** Interface ID of the implemented XtreemFS service. */
  boost::uint32_t interface_id_;

  /** Implementations have to register implemented operations with the
   *  corresponding proc_id here. */
  std::map<boost::uint32_t, Operation> operations_;

 private:
  /** Returns true if the request shall be dropped. */
  bool CheckIfRequestShallBeDropped(boost::uint32_t proc_id);
  
  /** Processes the "request" and returns a response.
   *
   * @remarks Ownership of return value is transferred to caller.
   */
  google::protobuf::Message* executeOperation(
      boost::uint32_t proc_id,
      const google::protobuf::Message& request);

  /** Daemon function which accepts new connections. */
  void Run();

  /** Connection handler. */
  void Session(boost::shared_ptr<boost::asio::ip::tcp::socket> sock);
  
  /** Guards access to active_sessions_ and active_sessions_socks_. */
  boost::mutex active_sessions_mutex_;

  /** Active client connections (thread id -> thread). */
  std::map< boost::thread::id,
            boost::shared_ptr<boost::thread> > active_sessions_;
    
  /** Socket of active client connections. Needed to cleanly shut them down. */
  std::map< boost::thread::id,
            boost::shared_ptr<boost::asio::ip::tcp::socket> >
      active_sessions_socks_;

  /** Thread which listens on the server socket and accepts new connections. */
  boost::scoped_ptr<boost::thread> daemon_;

  boost::asio::io_service io_service;

  boost::scoped_ptr<boost::asio::ip::tcp::acceptor> acceptor_;

  /** Guards access to the shared state of to be dropped requests. */
  boost::mutex to_be_dropped_requests_mutex_;

  /** Number of requests which shall be dropped. */
  int to_be_dropped_requests_;

  /** Proc ID of next request which shall be dropped.
   *
   * Please note that this implementation assumes that the special values
   * kDropRequestByProcIDDisable and kDropRequestByProcIDNewConnection
   * are not used otherwise.
   */
  boost::uint32_t to_be_dropped_request_by_proc_id_;
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_TEST_COMMON_TEST_RPC_SERVER_H_
