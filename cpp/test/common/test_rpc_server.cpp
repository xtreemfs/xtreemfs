/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "common/test_rpc_server.h"

#ifndef WIN32
#include <csignal>
#include <pthread.h>
#endif  // !WIN32

#include <boost/bind.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/scoped_array.hpp>
#include <iostream>
#include <vector>

#include "include/Common.pb.h"
#include "pbrpc/RPC.pb.h"
#include "rpc/record_marker.h"
#include "util/logging.h"
#include "xtreemfs/get_request_message.h"
#include "xtreemfs/DIR.pb.h"

using boost::asio::ip::tcp;
using namespace std;
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;

namespace xtreemfs {
namespace rpc {

TestRPCServer::TestRPCServer()
    : interface_id_(0),
      to_be_dropped_requests_(0),
      to_be_dropped_request_by_proc_id_(kDropRequestByProcIDDisable) {}

google::protobuf::Message* TestRPCServer::executeOperation(
    boost::uint32_t proc_id,
    const google::protobuf::Message& request) {
  map<boost::uint32_t, Operation>::const_iterator iter
      = operations_.find(proc_id);
  if (iter == operations_.end()) {
    return NULL;
  }

  return (iter->second) (request);
}

void TestRPCServer::Session(
    boost::shared_ptr<boost::asio::ip::tcp::socket> sock) {
  try {
    string remote_address = sock->remote_endpoint().address().to_string() + ":"
        + boost::lexical_cast<string>(sock->remote_endpoint().port());
    boost::scoped_array<char> record_marker_buffer(
        new char[RecordMarker::get_size()]);
    boost::scoped_array<char> header_buffer;
    boost::scoped_array<char> message_buffer;
    boost::scoped_array<char> data_buffer;
      
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
        << "New client connection from: " << remote_address << endl;
    }

    for (;;) {
      if (boost::this_thread::interruption_requested()) {
        if (Logging::log->loggingActive(LEVEL_DEBUG)) {
          Logging::log->getLog(LEVEL_DEBUG)
            << "Received interrupt, aborting test server connection to: "
            << remote_address << endl;
        }
        break;
      }

      size_t length = 0;
      boost::scoped_ptr<RecordMarker> request_rm;
      try {
        // Read record marker.
        length = boost::asio::read(
            *sock,
            boost::asio::buffer(record_marker_buffer.get(),
            RecordMarker::get_size()));
        if (length < RecordMarker::get_size()) {
          if (Logging::log->loggingActive(LEVEL_WARN)) {
            Logging::log->getLog(LEVEL_WARN)
              << "Read invalid record marker from: " << remote_address << endl;
          }
          break;
        }
        request_rm.reset(new RecordMarker(record_marker_buffer.get()));

        // Read header, message and data.
        vector<boost::asio::mutable_buffer> bufs;
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
          if (Logging::log->loggingActive(LEVEL_WARN)) {
            Logging::log->getLog(LEVEL_WARN)
                << "Received a request with an empty message from: "
                << remote_address << endl;
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
          if (Logging::log->loggingActive(LEVEL_WARN)) {
            Logging::log->getLog(LEVEL_WARN)
              << "Failed to read a complete request from: "
              << remote_address << endl;
          }
          break;
        }
      } catch (const boost::system::error_code& error) {
        if (error == boost::asio::error::eof) {
          break; // Connection closed cleanly by peer.
        } else {
          throw;
        }
      }

      // Parse header and message.
      RPCHeader request_rpc_header;
      if (!request_rpc_header.ParseFromArray(
              reinterpret_cast<void*>(header_buffer.get()),
              request_rm->header_len())) {
        if (Logging::log->loggingActive(LEVEL_WARN)) {
          Logging::log->getLog(LEVEL_WARN)
              << "Failed to parse request header received from: "
              << remote_address << endl;
        }
        break;
      }

      boost::uint32_t interface_id
          = request_rpc_header.request_header().interface_id();
      boost::uint32_t proc_id = request_rpc_header.request_header().proc_id();
      if (interface_id != interface_id_) {
        if (Logging::log->loggingActive(LEVEL_WARN)) {
          Logging::log->getLog(LEVEL_WARN)
              << "Received a message which was not intended for this service"
                 " with interface id: " << interface_id_ << " (Message from = "
              << remote_address << " (interface id = " << interface_id
              << ", proc id = " << proc_id << ")" << endl;
        }
        break;
      }

      boost::scoped_ptr<google::protobuf::Message> request_message(
          GetMessageForProcID(interface_id, proc_id));
      if (!request_message.get()){
        if (Logging::log->loggingActive(LEVEL_WARN)) {
          Logging::log->getLog(LEVEL_WARN)
              << "Failed to find a suitable request message type for message"
                  " received from: " << remote_address << " (interface id = "
              << interface_id << ", proc id = " << proc_id << ")" << endl;
        }
        break;
      }

      if (!request_message->ParseFromArray(
              reinterpret_cast<void*>(message_buffer.get()),
              request_rm->message_len())) {
        if (Logging::log->loggingActive(LEVEL_WARN)) {
          Logging::log->getLog(LEVEL_WARN)
              << "Failed to parse request message received from: "
              << remote_address << endl;
          break;
        }
      }

      // Check if the request should be dropped.
      if (CheckIfRequestShallBeDropped(proc_id)) {
        continue;
      }

      // Process request.
      boost::scoped_ptr<google::protobuf::Message> response_message(
          executeOperation(proc_id, *request_message));
      if (!response_message.get()){
        Logging::log->getLog(xtreemfs::util::LEVEL_ERROR)
            << "No response was generated. Operation with proc id = " << proc_id
            << " is probably not implemented?" << endl;
        break;
      }
      if (!response_message->IsInitialized()){
        Logging::log->getLog(xtreemfs::util::LEVEL_ERROR)
            << "Response message is not valid."
               " Not all required fields have been initialized: "
            << response_message->InitializationErrorString() << endl;
        break;
      }

      // Send response.
      RPCHeader response_header(request_rpc_header);
      RecordMarker response_rm(response_header.ByteSize(),
                               response_message->ByteSize(),
                               0);  // TODO(mberlin): Also add "data".

      size_t response_bytes_size = RecordMarker::get_size()
                                   + response_rm.header_len()
                                   + response_rm.message_len()
                                   + response_rm.data_len();
      boost::scoped_array<char> response_bytes(new char[response_bytes_size]);
      char* response = response_bytes.get();
      response_rm.serialize(response);
      response += RecordMarker::get_size();

      response_header.SerializeToArray(response, response_rm.header_len());
      response += response_rm.header_len();

      response_message->SerializeToArray(response, response_rm.message_len());

      vector<boost::asio::mutable_buffer> write_bufs;
      write_bufs.push_back(
          boost::asio::buffer(reinterpret_cast<void*> (response_bytes.get()),
          response_bytes_size));
      // TODO(mberlin): Also add "data" here.

      boost::asio::write(*sock, write_bufs);
    }
  } catch (const std::exception& e) {
    if (Logging::log->loggingActive(LEVEL_WARN)) {
      Logging::log->getLog(LEVEL_WARN)
          << "Exception in thread: " << e.what() << endl;
    }
  }
  {
    boost::mutex::scoped_lock lock(active_sessions_mutex_);
    active_sessions_.erase(boost::this_thread::get_id());
    active_sessions_socks_.erase(boost::this_thread::get_id());
  }
}

void TestRPCServer::DropNextRequests(int count) {
  boost::mutex::scoped_lock lock(to_be_dropped_requests_mutex_);
  to_be_dropped_requests_ += count;
}

void TestRPCServer::DropRequestByProcId(boost::uint32_t proc_id) {
  boost::mutex::scoped_lock lock(to_be_dropped_requests_mutex_);
  to_be_dropped_request_by_proc_id_ = proc_id;
}

void DummySignalHandler(int signal) {
  // See comment at TestRPCServer::Stop() why this is needed.
}

void TestRPCServer::Run() {
  signal(SIGUSR2, DummySignalHandler);
  for (;;) {
    if (boost::this_thread::interruption_requested()) {
      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
          << "Received interrupt, aborting server listener." << endl;
      }
      break;
    }
    boost::shared_ptr<tcp::socket> sock(new tcp::socket(io_service));
    try {
      acceptor_->accept(*sock);
      if (CheckIfRequestShallBeDropped(kDropRequestByProcIDNewConnection)) {
        sock->shutdown(boost::asio::ip::tcp::socket::shutdown_both);
        continue;
      }
      boost::shared_ptr<boost::thread> new_thread(new boost::thread(
          boost::bind(&TestRPCServer::Session, this, sock)));
      {
        boost::mutex::scoped_lock lock(active_sessions_mutex_);
        active_sessions_[new_thread->get_id()] = new_thread;
        active_sessions_socks_[new_thread->get_id()] = sock;
      }
    } catch (boost::system::system_error& ec) {
      break;
    }
  }
}

bool TestRPCServer::CheckIfRequestShallBeDropped(boost::uint32_t proc_id) {
  bool drop_request = false;
  boost::mutex::scoped_lock lock(to_be_dropped_requests_mutex_);
  if (proc_id != 0 && proc_id == to_be_dropped_request_by_proc_id_) {
    to_be_dropped_request_by_proc_id_ = kDropRequestByProcIDDisable;
    drop_request = true;
  }

  if (!drop_request && to_be_dropped_requests_ > 0) {
    to_be_dropped_requests_--;
    drop_request = true;
  }
  
  if (drop_request) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "Dropping request (proc_id = " << proc_id << ")" << endl;
    }
  }
  
  return drop_request;
}

std::string TestRPCServer::GetAddress() {
  return acceptor_->local_endpoint().address().to_string() + ":"
         + boost::lexical_cast<string>(acceptor_->local_endpoint().port());
}

bool TestRPCServer::Start() {
  initialize_logger(LEVEL_WARN);

  if (interface_id_ == 0) {
    Logging::log->getLog(LEVEL_ERROR)
        << "You forgot to set interface_id_ in"
           " your TestRPCServer implementation."<< endl;
    return false;
  }

  if (operations_.size() == 0) {
    Logging::log->getLog(LEVEL_ERROR)
        << "You have not registered any implemented operations." << endl;
    return false;
  }

  try {
    acceptor_.reset(
        new tcp::acceptor(io_service, tcp::endpoint(tcp::v4(), 0)));
    daemon_.reset(new boost::thread(boost::bind(&TestRPCServer::Run,
                                                this)));
  } catch (const std::exception& e) {
    if (Logging::log->loggingActive(LEVEL_WARN)) {
      Logging::log->getLog(LEVEL_WARN)
          << "Failed to start the server: " << e.what() << endl;
    }
    return false;
  }
  return true;
}

void TestRPCServer::Stop() {
  // Stop running Session() threads.
  map< boost::thread::id,
       boost::shared_ptr<boost::thread> > active_sessions_copy;
  {
    boost::mutex::scoped_lock lock(active_sessions_mutex_);
    active_sessions_copy = active_sessions_;
    
    // Close active sessions.
    for (map< boost::thread::id,
              boost::shared_ptr<boost::asio::ip::tcp::socket> >::iterator iter
             = active_sessions_socks_.begin();
         iter != active_sessions_socks_.end();
         iter++) {
      iter->second->shutdown(boost::asio::ip::tcp::socket::shutdown_both);
    }
  }
  // Wait for all closed sessions to exit.
  for (map< boost::thread::id, boost::shared_ptr<boost::thread> >::iterator iter
           = active_sessions_copy.begin();
       iter != active_sessions_copy.end();
       iter++) {
    iter->second->join();
  }
  {
    boost::mutex::scoped_lock lock(active_sessions_mutex_);
    if (active_sessions_.size() > 0) {
      if (Logging::log->loggingActive(LEVEL_WARN)) {
        Logging::log->getLog(LEVEL_WARN) << "There are open sessions left ("
            << active_sessions_.size() << ")" << endl;
      }
    }
  }
  // Unfortunately, boost::asio does not allow to abort synchronous operations.
  // See: https://svn.boost.org/trac/boost/ticket/2832
  // Randomly acceptor_->close(ec); succeeds, but not guaranteed. Even things
  // like shutdown(acceptor_->native(), SHUT_RDWR); did not work.
  // During debugging in Eclipse, I noticed that the daemon_ thread exits from
  // the blocking acceptor_->accept() if it was interrupted by the debugger.
  // Therefore, we call a no-op signal handler in the thread now and accept()
  // becomes unblocked, effectively allowing to stop the daemon_ thread.
  daemon_->interrupt();
  // Ignore errors.
  boost::system::error_code ec;
  acceptor_->close(ec);
  if (!daemon_->timed_join(boost::posix_time::milliseconds(10))) {
#ifdef WIN32
    Logging::log->getLog(LEVEL_ERROR)
        << "Failed to stop the server, daemon thread won't unblock. "
           "Abort manually with Ctrl + C." << endl;
#else
    pthread_kill(daemon_->native_handle(), SIGUSR2);
    daemon_->join();
#endif
  }
}

}  // namespace rpc
}  // namespace xtreemfs
