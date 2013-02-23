/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_TEST_RPC_SERVER_H_
#define CPP_TEST_COMMON_TEST_RPC_SERVER_H_

#include <stdint.h>
#ifndef WIN32
#include <csignal>
#include <pthread.h>
#endif  // !WIN32

#include <boost/asio.hpp>
#include <boost/bind.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/scoped_array.hpp>
#include <boost/smart_ptr.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include <iostream>
#include <map>
#include <string>
#include <vector>

#include "include/Common.pb.h"
#include "pbrpc/RPC.pb.h"
#include "rpc/record_marker.h"
#include "util/logging.h"
#include "xtreemfs/DIR.pb.h"
#include "xtreemfs/get_request_message.h"
#include "drop_rules.h"

namespace xtreemfs {
namespace rpc {

/** Base class which allows to implement a test XtreemFS server.
 *
 * Implementations have to set interface_id_ accordingly and add
 * implemented operations to operations_.
 */
template <class Derived> class TestRPCServer {
 public:
  TestRPCServer()
      : interface_id_(0),
        drop_connection_(false) {}

  virtual ~TestRPCServer() {}

  /** Starts the RPC Server at free, random port. False if not successful. */
  bool Start() {
    using boost::asio::ip::tcp;
    using xtreemfs::util::Logging;

    xtreemfs::util::initialize_logger(xtreemfs::util::LEVEL_WARN);

    if (interface_id_ == 0) {
      Logging::log->getLog(xtreemfs::util::LEVEL_ERROR)
          << "You forgot to set interface_id_ in"
             " your TestRPCServer implementation."<< std::endl;
      return false;
    }

    if (operations_.size() == 0) {
      Logging::log->getLog(xtreemfs::util::LEVEL_ERROR)
          << "You have not registered any implemented operations." << std::endl;
      return false;
    }

    try {
      tcp::endpoint localhost(
          boost::asio::ip::address::from_string("127.0.0.1"), 0);
      acceptor_.reset(
          new tcp::acceptor(io_service, localhost));
      daemon_.reset(new boost::thread(boost::bind(&TestRPCServer<Derived>::Run,
                                                  this)));
    } catch (const std::exception& e) {
      if (Logging::log->loggingActive(xtreemfs::util::LEVEL_WARN)) {
        Logging::log->getLog(xtreemfs::util::LEVEL_WARN)
            << "Failed to start the server: " << e.what() << std::endl;
      }
      return false;
    }
    return true;
  }

  /** Stops the server again. */
  void Stop() {
    using xtreemfs::util::Logging;

    // Stop running Session() threads.
    std::map< boost::thread::id,
         boost::shared_ptr<boost::thread> > active_sessions_copy;
    {
      boost::mutex::scoped_lock lock(active_sessions_mutex_);
      active_sessions_copy = active_sessions_;

      // Close active sessions.
      for (std::map< boost::thread::id,
                boost::shared_ptr<boost::asio::ip::tcp::socket> >::iterator iter
               = active_sessions_socks_.begin();
           iter != active_sessions_socks_.end();
           iter++) {
        iter->second->shutdown(boost::asio::ip::tcp::socket::shutdown_both);
      }
    }
    // Wait for all closed sessions to exit.
    for (std::map< boost::thread::id,
                   boost::shared_ptr<boost::thread> >::iterator iter
             = active_sessions_copy.begin();
         iter != active_sessions_copy.end();
         iter++) {
      iter->second->join();
    }
    {
      boost::mutex::scoped_lock lock(active_sessions_mutex_);
      if (active_sessions_.size() > 0) {
        if (Logging::log->loggingActive(xtreemfs::util::LEVEL_WARN)) {
          Logging::log->getLog(xtreemfs::util::LEVEL_WARN)
              << "There are open sessions left ("
              << active_sessions_.size() << ")" << std::endl;
        }
      }
    }

    {
      boost::mutex::scoped_lock lock(drop_rules_mutex_);
      for (std::list<DropRule*>::iterator it = drop_rules_.begin();
           it != drop_rules_.end();
           ++it) {
        delete *it;
      }
      drop_rules_.clear();
    }

    // Unfortunately, boost::asio does not allow to abort synchronous operations
    // See: https://svn.boost.org/trac/boost/ticket/2832
    // Randomly acceptor_->close(ec); succeeds, but not guaranteed. Even things
    // like shutdown(acceptor_->native(), SHUT_RDWR); did not work.
    // During debugging in Eclipse, I noticed that the daemon_ thread exits from
    // the blocking acceptor_->accept() if it was interrupted by the debugger.
    // Therefore, we call a no-op signal handler in the thread now and accept()
    // becomes unblocked, effectively allowing to stop the daemon_ thread.
    if (daemon_.get()) {
      daemon_->interrupt();
      // Ignore errors.
      boost::system::error_code ec;
      acceptor_->close(ec);
      if (!daemon_->timed_join(boost::posix_time::milliseconds(10))) {
#ifdef WIN32
        Logging::log->getLog(xtreemfs::util::LEVEL_ERROR)
            << "Failed to stop the server, daemon thread won't unblock. "
               "Abort manually with Ctrl + C." << std::endl;
#else
        pthread_kill(daemon_->native_handle(), SIGUSR2);
        daemon_->join();
#endif
      }
    }
  }

  /** Returns the address of the service in the format "ip-address:port". */
  std::string GetAddress() {
    return acceptor_->local_endpoint().address().to_string() + ":"
        + boost::lexical_cast<std::string>(acceptor_->local_endpoint().port());
  }

  /** Add a DropRule, ownership is transferred to the callee. */
  void AddDropRule(DropRule* rule) {
    boost::mutex::scoped_lock lock(drop_rules_mutex_);
    drop_rules_.push_back(rule);
    RemovePointlessDropRules(lock);
  }

  /** The connection will be shut down once after this call. */
  void DropConnection() {
    boost::mutex::scoped_lock lock(drop_connection_mutex_);
    drop_connection_ = true;
  }

 protected:
  /** Function pointer an implemented server operation. */
  typedef google::protobuf::Message* (Derived::*Operation)(
      const pbrpc::Auth& auth,
      const pbrpc::UserCredentials& user_credentials,
      const google::protobuf::Message& request,
      const char* data,
      uint32_t data_len,
      boost::scoped_array<char>* response_data,
      uint32_t* response_data_len);

  struct Op {
    Op() {}

    Op(Derived* server, Operation op)
        : server(server), op(op) {}

    /** Pointer to the implementation of TestRPCServer. */
    Derived* server;
    /** Function pointer to the implemented operation. */
    Operation op;
  };

  /** Interface ID of the implemented XtreemFS service. */
  uint32_t interface_id_;

  /** Implementations have to register implemented operations with the
   *  corresponding proc_id here. */
  std::map<uint32_t, Op> operations_;

 private:
  /** Delete old rules, should be called from a locked context */
  void RemovePointlessDropRules(const boost::mutex::scoped_lock& lock) {
    std::remove_if(drop_rules_.begin(), drop_rules_.end(), &DropRule::IsPointlessPred);
  }

  /** Returns true if the request shall be dropped. */
  bool CheckIfRequestShallBeDropped(uint32_t proc_id) {
    using xtreemfs::util::Logging;
    boost::mutex::scoped_lock lock(drop_rules_mutex_);

    bool drop_request = false;
    // NOTE: all drop rules must be asked even if drop_request is already
    //       true. This is important for counting rules or other side effects.
    for (std::list<DropRule*>::iterator it = drop_rules_.begin();
         it != drop_rules_.end();
         ++it) {
      drop_request = drop_request || (*it)->DropRequest(proc_id);
    }

    if (drop_request) {
      if (Logging::log->loggingActive(xtreemfs::util::LEVEL_DEBUG)) {
        Logging::log->getLog(xtreemfs::util::LEVEL_DEBUG)
            << "Dropping request (proc_id = " << proc_id << ")" << std::endl;
      }
    }

    return drop_request;
  }

  static void DummySignalHandler(int signal) {
    // See comment at TestRPCServer::Stop() why this is needed.
  }

  /** Processes the "request" and returns a response.
   *
   * @remarks Ownership of return value is transferred to caller.
   */
  google::protobuf::Message* ExecuteOperation(
      uint32_t proc_id,
      const pbrpc::Auth& auth,
      const pbrpc::UserCredentials& user_credentials,
      const google::protobuf::Message& request,
      const char* data,
      uint32_t data_len,
      boost::scoped_array<char>* response_data,
      uint32_t* response_data_len) {
    typename std::map<uint32_t, Op>::iterator iter
        = operations_.find(proc_id);
    if (iter == operations_.end()) {
      return NULL;
    }

    Op& entry = iter->second;
    return (entry.server->*(entry.op))
        (auth, user_credentials, request, data, data_len,
        response_data, response_data_len);
  }

  /** Daemon function which accepts new connections. */
  void Run() {
    using boost::asio::ip::tcp;
    using xtreemfs::util::Logging;

#ifndef WIN32
    signal(SIGUSR2, DummySignalHandler);
#endif

    for (;;) {
      if (boost::this_thread::interruption_requested()) {
        if (Logging::log->loggingActive(xtreemfs::util::LEVEL_DEBUG)) {
          Logging::log->getLog(xtreemfs::util::LEVEL_DEBUG)
            << "Received interrupt, aborting server listener." << std::endl;
        }
        break;
      }
      boost::shared_ptr<tcp::socket> sock(new tcp::socket(io_service));
      try {
        acceptor_->accept(*sock);
        {
          boost::mutex::scoped_lock lock(drop_connection_mutex_);
          if (drop_connection_) {
            drop_connection_ = false;
            sock->shutdown(tcp::socket::shutdown_both);
            continue;
          }
        }
        boost::shared_ptr<boost::thread> new_thread(new boost::thread(
            boost::bind(&TestRPCServer<Derived>::Session, this, sock)));
        {
          boost::mutex::scoped_lock lock(active_sessions_mutex_);
          active_sessions_[new_thread->get_id()] = new_thread;
          active_sessions_socks_[new_thread->get_id()] = sock;
        }
      } catch (const boost::system::system_error&) {
        break;
      }
    }
  }

  /** Connection handler. */
  void Session(boost::shared_ptr<boost::asio::ip::tcp::socket> sock) {
    using xtreemfs::util::Logging;

    try {
      std::string remote_address =
          sock->remote_endpoint().address().to_string() + ":"
          + boost::lexical_cast<std::string>(sock->remote_endpoint().port());
      boost::scoped_array<char> record_marker_buffer(
          new char[xtreemfs::rpc::RecordMarker::get_size()]);
      boost::scoped_array<char> header_buffer;
      boost::scoped_array<char> message_buffer;
      boost::scoped_array<char> data_buffer;

      if (Logging::log->loggingActive(xtreemfs::util::LEVEL_DEBUG)) {
        Logging::log->getLog(xtreemfs::util::LEVEL_DEBUG)
          << "New client connection from: " << remote_address << std::endl;
      }

      for (;;) {
        if (boost::this_thread::interruption_requested()) {
          if (Logging::log->loggingActive(xtreemfs::util::LEVEL_DEBUG)) {
            Logging::log->getLog(xtreemfs::util::LEVEL_DEBUG)
              << "Received interrupt, aborting test server connection to: "
              << remote_address << std::endl;
          }
          break;
        }

        size_t length = 0;
        boost::scoped_ptr<xtreemfs::rpc::RecordMarker> request_rm;
        try {
          // Read record marker.
          length = boost::asio::read(
              *sock,
              boost::asio::buffer(record_marker_buffer.get(),
              xtreemfs::rpc::RecordMarker::get_size()));
          if (length < xtreemfs::rpc::RecordMarker::get_size()) {
            if (Logging::log->loggingActive(xtreemfs::util::LEVEL_WARN)) {
              Logging::log->getLog(xtreemfs::util::LEVEL_WARN)
                  << "Read invalid record marker from: "
                  << remote_address << std::endl;
            }
            break;
          }
          request_rm.reset(
              new xtreemfs::rpc::RecordMarker(record_marker_buffer.get()));

          // Read header, message and data.
          std::vector<boost::asio::mutable_buffer> bufs;
          header_buffer.reset(new char[request_rm->header_len()]);
          bufs.push_back(
              boost::asio::buffer(reinterpret_cast<void*>(header_buffer.get()),
              request_rm->header_len()));
          if (request_rm->message_len() > 0) {
            message_buffer.reset(new char[request_rm->message_len()]);
            bufs.push_back(
                boost::asio::buffer(
                    reinterpret_cast<void*>(message_buffer.get()),
                request_rm->message_len()));
          } else {
            if (Logging::log->loggingActive(xtreemfs::util::LEVEL_WARN)) {
              Logging::log->getLog(xtreemfs::util::LEVEL_WARN)
                  << "Received a request with an empty message from: "
                  << remote_address << std::endl;
            }
            break;
          }
          if (request_rm->data_len() > 0) {
            data_buffer.reset(new char[request_rm->data_len()]);
            bufs.push_back(
                boost::asio::buffer(reinterpret_cast<void*>(data_buffer.get()),
                request_rm->data_len()));
          } else {
            data_buffer.reset(NULL);
          }

          length = boost::asio::read(*sock, bufs);

          if (length != (request_rm->header_len()
                         + request_rm->message_len()
                         + request_rm->data_len())) {
            if (Logging::log->loggingActive(xtreemfs::util::LEVEL_WARN)) {
              Logging::log->getLog(xtreemfs::util::LEVEL_WARN)
                << "Failed to read a complete request from: "
                << remote_address << std::endl;
            }
            break;
          }
        } catch (const boost::system::error_code& error) {
          if (error == boost::asio::error::eof) {
            break;  // Connection closed cleanly by peer.
          } else {
            throw;
          }
        }

        // Parse header and message.
        xtreemfs::pbrpc::RPCHeader request_rpc_header;
        if (!request_rpc_header.ParseFromArray(
                reinterpret_cast<void*>(header_buffer.get()),
                request_rm->header_len())) {
          if (Logging::log->loggingActive(xtreemfs::util::LEVEL_WARN)) {
            Logging::log->getLog(xtreemfs::util::LEVEL_WARN)
                << "Failed to parse request header received from: "
                << remote_address << std::endl;
          }
          break;
        }

        uint32_t interface_id
            = request_rpc_header.request_header().interface_id();
        uint32_t proc_id = request_rpc_header.request_header().proc_id();
        if (interface_id != interface_id_) {
          if (Logging::log->loggingActive(xtreemfs::util::LEVEL_WARN)) {
            Logging::log->getLog(xtreemfs::util::LEVEL_WARN)
                << "Received a message which was not intended for this service"
                   " with interface id: " << interface_id_ << " (Message from"
                   " = " << remote_address
                << " (interface id = " << interface_id << ", proc id = "
                << proc_id << ")" << std::endl;
          }
          break;
        }

        boost::scoped_ptr<google::protobuf::Message> request_message(
            xtreemfs::pbrpc::GetMessageForProcID(interface_id, proc_id));
        if (!request_message.get()) {
          if (Logging::log->loggingActive(xtreemfs::util::LEVEL_WARN)) {
            Logging::log->getLog(xtreemfs::util::LEVEL_WARN)
                << "Failed to find a suitable request message type for message"
                    " received from: " << remote_address << " (interface id = "
                << interface_id
                << ", proc id = " << proc_id << ")" << std::endl;
          }
          break;
        }

        if (!request_message->ParseFromArray(
                reinterpret_cast<void*>(message_buffer.get()),
                request_rm->message_len())) {
          if (Logging::log->loggingActive(xtreemfs::util::LEVEL_WARN)) {
            Logging::log->getLog(xtreemfs::util::LEVEL_WARN)
                << "Failed to parse request message received from: "
                << remote_address << std::endl;
            break;
          }
        }

        // Check if the request should be dropped.
        if (CheckIfRequestShallBeDropped(proc_id)) {
          continue;
        }

        // Process request.
        boost::scoped_array<char> response_data;
        uint32_t response_data_len = 0;
        boost::scoped_ptr<google::protobuf::Message> response_message(
            ExecuteOperation(proc_id,
                request_rpc_header.request_header().auth_data(),
                request_rpc_header.request_header().user_creds(),
                *request_message,
                data_buffer.get(),
                request_rm->data_len(),
                &response_data,
                &response_data_len));
        if (!response_message.get()) {
          Logging::log->getLog(xtreemfs::util::LEVEL_ERROR)
              << "No response was generated. Operation with proc id = "
              << proc_id << " is probably not implemented? (interface_id = "
              << interface_id_ << ")" << std::endl;
          break;
        }
        if (!response_message->IsInitialized()) {
          Logging::log->getLog(xtreemfs::util::LEVEL_ERROR)
              << "Response message is not valid."
                 " Not all required fields have been initialized: "
              << response_message->InitializationErrorString() << std::endl;
          break;
        }

        // Send response.
        xtreemfs::pbrpc::RPCHeader response_header(request_rpc_header);
        xtreemfs::rpc::RecordMarker response_rm(
            response_header.ByteSize(),
            response_message->ByteSize(),
            response_data_len);

        size_t response_bytes_size = xtreemfs::rpc::RecordMarker::get_size()
                                     + response_rm.header_len()
                                     + response_rm.message_len()
                                     + response_rm.data_len();
        boost::scoped_array<char> response_bytes(new char[response_bytes_size]);
        char* response = response_bytes.get();
        response_rm.serialize(response);
        response += xtreemfs::rpc::RecordMarker::get_size();

        response_header.CheckInitialized();
        if (!response_header.SerializeToArray(response, response_rm.header_len())) {
            Logging::log->getLog(xtreemfs::util::LEVEL_ERROR)
                << "Failed to serialize header" << std::endl;
            break;
        }
        response += response_rm.header_len();

        response_message->CheckInitialized();
        if (!response_message->SerializeToArray(response, response_rm.message_len())) {
            Logging::log->getLog(xtreemfs::util::LEVEL_ERROR)
                << "Failed to serialize message" << std::endl;
            break;
        }

        response += response_rm.message_len();

        if (response_data.get() != NULL) {
          memcpy(response, response_data.get(), response_data_len);
        }

        std::vector<boost::asio::mutable_buffer> write_bufs;
        write_bufs.push_back(
            boost::asio::buffer(reinterpret_cast<void*>(response_bytes.get()),
            response_bytes_size));

        boost::asio::write(*sock, write_bufs);
      }
    } catch (const boost::system::system_error& e) {
      if (e.code() != boost::asio::error::eof) {
        if (Logging::log->loggingActive(xtreemfs::util::LEVEL_WARN)) {
          Logging::log->getLog(xtreemfs::util::LEVEL_WARN)
              << "Exception in thread: " << e.what() << std::endl;
        }
      }
    }
    {
      boost::mutex::scoped_lock lock(active_sessions_mutex_);
      active_sessions_.erase(boost::this_thread::get_id());
      active_sessions_socks_.erase(boost::this_thread::get_id());
    }
  }

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

  /** Drop rules for incoming requests. */
  std::list<DropRule*> drop_rules_;

  /** Guards access drop_rules_. */
  boost::mutex drop_rules_mutex_;

  /** Used to drop the next connection. */
  bool drop_connection_;

  /** Guards access drop_connection_. */
  boost::mutex drop_connection_mutex_;
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_TEST_COMMON_TEST_RPC_SERVER_H_
