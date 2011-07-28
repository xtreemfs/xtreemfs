/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "rpc/client_connection.h"

#include <errno.h>
#include <boost/bind.hpp>
#include <iostream>
#include <string>
#include <vector>


#include "rpc/grid_ssl_socket_channel.h"
#include "rpc/ssl_socket_channel.h"
#include "rpc/tcp_socket_channel.h"
#include "util/logging.h"

namespace xtreemfs {
namespace rpc {

using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;
using namespace std;
using namespace boost;
using namespace google::protobuf;
using namespace boost::asio::ip;

ClientConnection::ClientConnection(
    const string& server_name,
    const string& port,
    asio::io_service& service,
    request_map *request_table,
    int32_t connect_timeout_s, int32_t max_reconnect_interval_s,
    bool use_gridssl, boost::asio::ssl::context* ssl_context)
    : receive_marker_(NULL),
      receive_hdr_(NULL),
      receive_msg_(NULL),
      receive_data_(NULL),
      connection_state_(IDLE),
      requests_(),
      current_request_(NULL),
      server_name_(server_name),
      server_port_(port),
      service_(service),
      resolver_(service),
      socket_(NULL),
      endpoint_(NULL),
      request_table_(request_table),
      timer_(service),
      connect_timeout_s_(connect_timeout_s),
      max_reconnect_interval_s_(max_reconnect_interval_s),
      next_reconnect_at_(boost::posix_time::not_a_date_time),
      reconnect_interval_s_(1),
      use_gridssl_(use_gridssl),
      ssl_context_(ssl_context) {
  receive_marker_buffer_ = new char[RecordMarker::get_size()];
  CreateChannel();
}

void ClientConnection::AddRequest(ClientRequest* request) {
  requests_.push(request);
  (*request_table_)[request->call_id()] = request;
}

void ClientConnection::SendError(POSIXErrno posix_errno,
                                 const string &error_message) {
  if (!requests_.empty()) {
    Logging::log->getLog(LEVEL_ERROR)
        << "operation failed: errno="
        << posix_errno << " message="
        << error_message << endl;
    RPCHeader::ErrorResponse *err = new RPCHeader::ErrorResponse();
    err->set_error_type(IO_ERROR);
    err->set_posix_errno(posix_errno);
    err->set_error_message(error_message);
    while (!requests_.empty()) {
      ClientRequest *request = requests_.front();
      request_table_->erase(request->call_id());
      requests_.pop();
      request->set_error(new RPCHeader::ErrorResponse(*err));
      request->ExecuteCallback();
    }
    delete err;
  }
}

void ClientConnection::DoProcess() {
  last_used_ = posix_time::second_clock::local_time();

  if (connection_state_ == IDLE) {
    if (endpoint_ == NULL) {
      Connect();
    } else {
      // Do write.
      SendRequest();
    }
  } else if (connection_state_ == WAIT_FOR_RECONNECT) {
    posix_time::ptime now = posix_time::second_clock::local_time();
    if (next_reconnect_at_ <= now) {
      next_reconnect_at_ = posix_time::not_a_date_time;

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG) << "trying reconnect..." << endl;
      }

      Connect();
    } else {
      SendError(POSIX_ERROR_EIO,
                string("cannot connect to server, reconnect blocked"));
    }
  }
}

void ClientConnection::CreateChannel() {
  if (socket_ != NULL) {
    socket_->close();
    delete socket_;
  }
  if (ssl_context_ == NULL) {
    socket_ = new TCPSocketChannel(service_);
  } else if (use_gridssl_) {
    socket_ = new GridSSLSocketChannel(service_, *ssl_context_);
  } else {
    socket_ = new SSLSocketChannel(service_, *ssl_context_);
  }
}

void ClientConnection::Connect() {
  connection_state_ = CONNECTING;
  asio::ip::tcp::resolver::query query(server_name_, server_port_);
  resolver_.async_resolve(query,
                          bind(&ClientConnection::PostResolve, this,
                               asio::placeholders::error,
                               asio::placeholders::iterator));
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "connect timeout is "
        << connect_timeout_s_ << " seconds\n";
  }
}

void ClientConnection::OnConnectTimeout(const boost::system::error_code& err) {
  if (err != asio::error::operation_aborted) {
    Reset();
    SendError(POSIX_ERROR_EIO, string("connection to '")
        + server_name_ + ":" + server_port_ + "' timed out");
  }
}

void ClientConnection::PostResolve(
    const boost::system::error_code& err,
    tcp::resolver::iterator endpoint_iterator) {
  if (err) {
    Reset();
    SendError(POSIX_ERROR_EIO,
        std::string("could not connect to '")
        + server_name_ + ":" + server_port_
        + "': " + err.message());
  }
  if (endpoint_iterator != tcp::resolver::iterator()) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG) << "resolved: "
          << (*endpoint_iterator).host_name() << endl;
    }

    endpoint_ = new tcp::endpoint(*endpoint_iterator);

    timer_.expires_from_now(posix_time::seconds(connect_timeout_s_));
    timer_.async_wait(bind(&ClientConnection::OnConnectTimeout,
                           this,
                           asio::placeholders::error));
    socket_->async_connect(*endpoint_,
                           bind(&ClientConnection::PostConnect,
                                this,
                                asio::placeholders::error,
                                endpoint_iterator));
  } else {
    SendError(POSIX_ERROR_EINVAL, string("cannot resolve hostname: '")
        + this->server_name_ + ":" + server_port_ + string("'"));
  }
}

void ClientConnection::PostConnect(
    const boost::system::error_code& err,
    tcp::resolver::iterator endpoint_iterator) {
  timer_.cancel();
  if (err) {
    if (err == asio::error::operation_aborted) {
      return;
    }

    if (++endpoint_iterator != tcp::resolver::iterator()) {
      // Try next endpoint.
      CreateChannel();
      delete endpoint_;
      endpoint_ = NULL;

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG) << "failed: next endpoint"
            << err.message() << "\n";
      }

      PostResolve(err, endpoint_iterator);
    } else {
      Reset();
      SendError(POSIX_ERROR_EIO, string("could not connect to host name '")
          + server_name_ + "': " + err.message());
    }
  } else {
    // Do something useful.
    reconnect_interval_s_ = 1;
    next_reconnect_at_ = posix_time::not_a_date_time;

    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG) << "connected: "
          << (*endpoint_iterator).host_name() << endl;
    }

    connection_state_ = IDLE;
    if (!requests_.empty()) {
      SendRequest();
      ReceiveRequest();
    }
  }
}

void ClientConnection::SendRequest() {
  if (!requests_.empty()) {
    connection_state_ = ACTIVE;

    ClientRequest* rq = requests_.front();
    assert(rq != NULL);

    if (rq->cancelled()) {
      delete rq;
      // The element must be poped from the queue.
      requests_.pop();
      SendRequest();
    }

    const RecordMarker* rrm = rq->request_marker();

    vector<boost::asio::const_buffer> bufs;
    bufs.push_back(boost::asio::buffer(
        reinterpret_cast<const void*>(rq->rq_hdr_msg()),
        RecordMarker::get_size() + rrm->header_len() + rrm->message_len()));

    if (rrm->data_len() > 0) {
      bufs.push_back(boost::asio::buffer(
        reinterpret_cast<const void*> (rq->rq_data()), rrm->data_len()));
    }

    socket_->async_write(bufs, bind(&ClientConnection::PostWrite, this,
        asio::placeholders::error, asio::placeholders::bytes_transferred));
  } else {
    connection_state_ = IDLE;
  }
}

void ClientConnection::ReceiveRequest() {
  if (endpoint_) {
    socket_->async_read(asio::buffer(receive_marker_buffer_,
                                     RecordMarker::get_size()),
                        bind(&ClientConnection::PostReadRecordMarker,
                             this,
                             asio::placeholders::error));
  }
}

void ClientConnection::Reset() {
  CreateChannel();
  delete endpoint_;
  endpoint_ = NULL;
  connection_state_ = WAIT_FOR_RECONNECT;

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
        << "connection reset, next reconnect in " << reconnect_interval_s_
        << "s " << endl;
  }

  next_reconnect_at_ = posix_time::second_clock::local_time()
      + posix_time::seconds(reconnect_interval_s_);
  reconnect_interval_s_ = reconnect_interval_s_ * 2;
}

void ClientConnection::Close() {
  socket_->close();
  delete socket_;
  socket_ = NULL;
  connection_state_ = CLOSED;
  SendError(POSIX_ERROR_EIO, "connection to '" + server_name_
      + "' closed locally");
}

void ClientConnection::PostWrite(const boost::system::error_code& err,
                                 size_t bytes_written) {
  if (err) {
    Reset();
    SendError(POSIX_ERROR_EIO, "could not send request to '"
        + server_name_+":"+server_port_ + "': " + err.message());
  } else {
    // Send next?
    requests_.pop();
    connection_state_ = IDLE;
    SendRequest();
  }
}

void ClientConnection::PostReadRecordMarker(
    const boost::system::error_code& err) {
  if (err) {
    Reset();
    string msg = "could not read record marker in response from '"
        + server_name_ + ":" + server_port_ + "': " + err.message();
    SendError(POSIX_ERROR_EIO, msg);
  } else {
    // Do read.
    receive_marker_ = new RecordMarker(receive_marker_buffer_);

    vector<boost::asio::mutable_buffer> bufs;
    receive_hdr_ = new char[receive_marker_->header_len()];
    bufs.push_back(asio::buffer(reinterpret_cast<void*> (receive_hdr_),
                                receive_marker_->header_len()));
    if (receive_marker_->message_len() > 0) {
      receive_msg_ = new char[receive_marker_->message_len()];
      bufs.push_back(asio::buffer(reinterpret_cast<void*> (receive_msg_),
                                  receive_marker_->message_len()));
    } else {
      receive_msg_ = NULL;
    }
    if (receive_marker_->data_len() > 0) {
      receive_data_ = new char[receive_marker_->data_len()];
      bufs.push_back(asio::buffer(reinterpret_cast<void*> (receive_data_),
                                  receive_marker_->data_len()));
    } else {
      receive_data_ = NULL;
    }
    socket_->async_read(bufs,
                        bind(&ClientConnection::PostReadMessage,
                             this,
                             asio::placeholders::error));
  }
}

void ClientConnection::PostReadMessage(const boost::system::error_code& err) {
  if (err) {
    Reset();
    SendError(POSIX_ERROR_EIO, "could not read response from '" +
        server_name_ + "': " + err.message());
  } else {
    // Parse header.
    RPCHeader *respHdr = new RPCHeader();
    if (respHdr->ParseFromArray(receive_hdr_, receive_marker_->header_len())) {
      delete[] receive_hdr_;
      receive_hdr_ = NULL;
    } else {
      // Error parsing the header.
      DeleteInternalBuffers();
      delete respHdr;
      Reset();
      SendError(POSIX_ERROR_EINVAL, "received garbage header from '" +
          server_name_ + ", closing connection");
      return;
    }

    // Get request from table.
    request_map::iterator iter = request_table_->find(respHdr->call_id());
    ClientRequest *rq;
    if (iter != request_table_->end()) {
      rq = iter->second;
    } else {
      if (Logging::log->loggingActive(LEVEL_WARN)) {
        Logging::log->getLog(LEVEL_WARN)
            << "Received response for unknown request id: "
            << respHdr->call_id() << " from " << server_name_ << std::endl;
      }
      DeleteInternalBuffers();
      delete respHdr;
      return;
    }

    uint32 call_id = respHdr->call_id();

    if (respHdr->has_error_response()) {
      // Error response.
      rq->set_error(new RPCHeader::ErrorResponse(respHdr->error_response()));
      // Manually cleanup response header.
      delete respHdr;
    } else {
      // Parse message, if exists.
      if (receive_marker_->message_len() > 0) {
        if (!rq->resp_message()) {
          // Not prepared to receive a message.
          // Print error and discard data.
          Logging::log->getLog(LEVEL_WARN)
            << "Received an unexpected response message (expected size 0, got "
            << receive_marker_->message_len() << " bytes) from "
            << server_name_ << std::endl;
        } else {
          assert(receive_msg_ != NULL);
          if (!rq->resp_message()->ParseFromArray(
              receive_msg_,
              receive_marker_->message_len())) {
            // Parsing message failed. Generate error.
            RPCHeader::ErrorResponse *err = new RPCHeader::ErrorResponse();
            err->set_error_type(GARBAGE_ARGS);
            err->set_posix_errno(POSIX_ERROR_NONE);
            err->set_error_message(string("cannot parse message data: ")
                + rq->resp_message()->InitializationErrorString());
            rq->set_error(err);

            // manually cleanup response header
            delete respHdr;
          } else {
            // Message successfully parsed, set data.
            // Hand over responsibility for receive_data_ to request object.
            rq->set_resp_data(receive_data_);
            rq->set_resp_data_len(receive_marker_->data_len());
            receive_data_ = NULL;
          }
        }
      }
      // Always set response header.
      rq->set_resp_header(respHdr);
    }

    // Remove from table and clean up buffers.
    request_table_->erase(call_id);
    DeleteInternalBuffers();
    rq->ExecuteCallback();

    // Receive next request.
    ReceiveRequest();
  }
}

void ClientConnection::DeleteInternalBuffers() {
  delete[] receive_hdr_;
  receive_hdr_ = NULL;
  delete[] receive_msg_;
  receive_msg_ = NULL;
  delete[] receive_data_;
  receive_data_ = NULL;
  delete receive_marker_;
  receive_marker_ = NULL;
}

ClientConnection::~ClientConnection() {
  delete endpoint_;
  delete socket_;
  delete[] receive_marker_buffer_;
  DeleteInternalBuffers();
}

}  // namespace rpc
}  // namespace xtreemfs
