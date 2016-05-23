/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *                    2012 by Michael Berlin, Zuse Institute Berlin
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

#ifdef HAS_VALGRIND
#include <valgrind/memcheck.h>
#include <valgrind/valgrind.h>
#endif  // HAS_VALGRIND

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
    int32_t connect_timeout_s,
    int32_t max_reconnect_interval_s
#ifdef HAS_OPENSSL
    ,bool use_gridssl,
    boost::asio::ssl::context* ssl_context
#endif  // HAS_OPENSSL
    )
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
      last_connect_was_at_(boost::posix_time::not_a_date_time),
      reconnect_interval_s_(1)
#ifdef HAS_OPENSSL
      ,use_gridssl_(use_gridssl),
      ssl_context_(ssl_context)
#endif  // HAS_OPENSSL
{
  receive_marker_buffer_ = new char[RecordMarker::get_size()];
  CreateChannel();
}

void ClientConnection::AddRequest(ClientRequest* request) {
  request->set_client_connection(this);
  requests_.push(PendingRequest(request->call_id(), request));
  (*request_table_)[request->call_id()] = request;
}

void ClientConnection::SendError(POSIXErrno posix_errno,
                                 const string &error_message) {
  if (!requests_.empty()) {
    RPCHeader::ErrorResponse err;
    err.set_error_type(IO_ERROR);
    err.set_posix_errno(posix_errno);
    err.set_error_message(error_message);

    while (!requests_.empty()) {
      uint32_t call_id = requests_.front().call_id;
      request_map::iterator iter = request_table_->find(call_id);
      if (iter != request_table_->end()) {
        // ClientRequest still exists in request_table_, it's safe to access it.
        ClientRequest *request = requests_.front().rq;
        request->set_error(new RPCHeader::ErrorResponse(err));
        request->ExecuteCallback();
        request_table_->erase(call_id);

        Logging::log->getLog(LEVEL_ERROR)
            << "operation failed: call_id=" << call_id
            << " errno=" << posix_errno
            << " message=" << error_message << endl;
      }
      requests_.pop();
    }
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
                "cannot connect to server '" + server_name_ + ":" + server_port_
                    + "', reconnect blocked locally"
                    " to avoid flooding the server");
    }
  }
}

void ClientConnection::DelayedSocketDeletionHandler(
    AbstractSocketChannel* socket) {
  delete socket;
}

void ClientConnection::CreateChannel() {
  if (socket_ != NULL) {
      socket_->close();

    // In case of SSL connections, boost::asio tries to write to the socket
    // after the SSL stream and the socket was shutdown. Therefore, we delay
    // the deletion and hope that no segmentation fault is triggered. The
    // correct way would have been to use a shared_ptr for the socket.
    service_.post(boost::bind(&ClientConnection::DelayedSocketDeletionHandler,
                              socket_));
    socket_ = NULL;
  }
#ifndef HAS_OPENSSL
  socket_ = new TCPSocketChannel(service_);
#else
  if (ssl_context_ == NULL) {
    socket_ = new TCPSocketChannel(service_);
  } else if (use_gridssl_) {
    socket_ = new GridSSLSocketChannel(service_, *ssl_context_);
  } else {
    socket_ = new SSLSocketChannel(service_, *ssl_context_);
  }
#endif  // !HAS_OPENSSL
}

void ClientConnection::Connect() {
  connection_state_ = CONNECTING;
  last_connect_was_at_ = posix_time::second_clock::local_time();
#if (BOOST_VERSION > 104200)
  asio::ip::tcp::resolver::query query(
      server_name_,
      server_port_,
      static_cast<asio::ip::resolver_query_base::flags>(0) /* no flags */);
#else
  asio::ip::tcp::resolver::query query(server_name_, server_port_);
#endif
  resolver_.async_resolve(query,
                          boost::bind(&ClientConnection::PostResolve,
                                      this,
                                      asio::placeholders::error,
                                      asio::placeholders::iterator));
  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG) << "connect timeout is "
        << connect_timeout_s_ << " seconds\n";
  }
}

void ClientConnection::OnConnectTimeout(const boost::system::error_code& err) {
  if (err == asio::error::operation_aborted || err == asio::error::eof
      || connection_state_ == CLOSED) {
    return;
  }
  Reset();
  SendError(POSIX_ERROR_EIO,
            "connection to '" + server_name_ + ":" + server_port_
                + "' timed out");
}

void ClientConnection::PostResolve(const boost::system::error_code& err,
                                   tcp::resolver::iterator endpoint_iterator) {
  if (err == asio::error::operation_aborted || err == asio::error::eof
      || connection_state_ == CLOSED) {
    return;
  }
  if (err) {
    Reset();
    SendError(POSIX_ERROR_EIO,
              "could not connect to '" + server_name_ + ":" + server_port_
                  + "': " + err.message());
  }
  if (endpoint_iterator != tcp::resolver::iterator()) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG) << "resolved: "
          << (*endpoint_iterator).host_name() << endl;
    }

    if (endpoint_ != NULL) {
      delete endpoint_;
    }
    endpoint_ = new tcp::endpoint(*endpoint_iterator);

    timer_.expires_from_now(posix_time::seconds(connect_timeout_s_));
    timer_.async_wait(boost::bind(&ClientConnection::OnConnectTimeout,
                                  this,
                                  asio::placeholders::error));
    socket_->async_connect(*endpoint_,
                           boost::bind(&ClientConnection::PostConnect,
                                       this,
                                       asio::placeholders::error,
                                       endpoint_iterator));
  } else {
    SendError(POSIX_ERROR_EINVAL, string("cannot resolve hostname: '")
        + this->server_name_ + ":" + server_port_ + string("'"));
  }
}

void ClientConnection::PostConnect(const boost::system::error_code& err,
                                   tcp::resolver::iterator endpoint_iterator) {
  if (err == asio::error::operation_aborted || err == asio::error::eof
      || connection_state_ == CLOSED) {
    return;
  }
  timer_.cancel();
  if (err) {
    delete endpoint_;
    endpoint_ = NULL;

    if (++endpoint_iterator != tcp::resolver::iterator()) {
      // Try next endpoint.
      CreateChannel();

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG) << "failed: next endpoint"
            << err.message() << "\n";
      }

      PostResolve(boost::system::error_code(), endpoint_iterator);
    } else {
      Reset();
      string ssl_error_info;
#ifdef HAS_OPENSSL
      if (err.category() == asio::error::ssl_category) {
        ostringstream oss;
        oss
          << "Boost error message: '" << err.message() << "' (value: '" << err.value() << "')"
          << ", OpenSSL library number: '" << ERR_GET_LIB(err.value()) << "'"
          << ", OpenSSL function code: '" << ERR_GET_FUNC(err.value()) << "'"
          << ", OpenSSL reason code: '" << ERR_GET_REASON(err.value()) << "'";
        char buf[512];
        ERR_error_string_n(err.value(), buf, sizeof(buf));
        oss << ", OpenSSL error string: '" << buf << "'";
        ssl_error_info = oss.str();
      }
#endif  // HAS_OPENSSL
      SendError(POSIX_ERROR_EIO,
                "could not connect to host '" + server_name_ + ":"
                    + server_port_ + "': " + err.message()+" "+ssl_error_info);
    }
  } else {
    // Do something useful.
    reconnect_interval_s_ = 1;
    next_reconnect_at_ = posix_time::not_a_date_time;

    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG) << "connected to "
          << (*endpoint_iterator).host_name() << ":"
          << (*endpoint_iterator).service_name() << endl;
#ifdef HAS_OPENSSL
      if (ssl_context_ != NULL) {
        Logging::log->getLog(LEVEL_DEBUG) << "Using SSL/TLS version '"
            << ((SSLSocketChannel*) socket_)->ssl_tls_version() << "'." << endl;
      }
#endif  // HAS_OPENSSL
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

    uint32_t call_id = requests_.front().call_id;
    ClientRequest* rq = requests_.front().rq;
    assert(rq != NULL);

    // If the request is no longer present in request_table_, it was already
    // deleted meanwhile (e.g. by Client::handleTimeout()).
    // Get request from table.
    request_map::iterator iter = request_table_->find(call_id);
    if (iter == request_table_->end()) {
      // ClientRequest was already deleted, stop here.
      requests_.pop();
      SendRequest();
    } else {
      // Process ClientRequest.
      const RecordMarker* rrm = rq->request_marker();

      vector<boost::asio::const_buffer> bufs;
      bufs.push_back(boost::asio::buffer(
          reinterpret_cast<const void*>(rq->rq_hdr_msg()),
          RecordMarker::get_size() + rrm->header_len() + rrm->message_len()));

      if (rrm->data_len() > 0) {
        bufs.push_back(boost::asio::buffer(
            reinterpret_cast<const void*>(rq->rq_data()), rrm->data_len()));
      }

      socket_->async_write(bufs, boost::bind(
          &ClientConnection::PostWrite,
          this,
          asio::placeholders::error,
          asio::placeholders::bytes_transferred));
    }
  } else {
    connection_state_ = IDLE;
  }
}

void ClientConnection::ReceiveRequest() {
  if (endpoint_) {
    socket_->async_read(asio::buffer(receive_marker_buffer_,
                                     RecordMarker::get_size()),
                        boost::bind(&ClientConnection::PostReadRecordMarker,
                                    this,
                                    asio::placeholders::error));
  }
}

void ClientConnection::Reset() {
  CreateChannel();
  delete endpoint_;
  endpoint_ = NULL;
  connection_state_ = WAIT_FOR_RECONNECT;

  posix_time::ptime now = posix_time::second_clock::local_time();
  posix_time::seconds reconnect_interval(reconnect_interval_s_);
  if (last_connect_was_at_ != boost::posix_time::not_a_date_time) {
    posix_time::time_duration elapsed_time_since_last_connect =
        now - last_connect_was_at_;
    if (elapsed_time_since_last_connect.is_negative()) {
      next_reconnect_at_ = now;
    } else if (elapsed_time_since_last_connect <= reconnect_interval) {
      next_reconnect_at_
          = now + reconnect_interval - elapsed_time_since_last_connect;
    } else {
      next_reconnect_at_ = now;
    }
  } else {
    next_reconnect_at_ = now + reconnect_interval;
  }

  if (Logging::log->loggingActive(LEVEL_DEBUG)) {
    Logging::log->getLog(LEVEL_DEBUG)
        << "Connection reset, next reconnect in "
        << (next_reconnect_at_ - now).seconds() << " seconds." << endl;
  }

  reconnect_interval_s_ *= 2;
  if (reconnect_interval_s_ > max_reconnect_interval_s_) {
    reconnect_interval_s_ = max_reconnect_interval_s_;
  }
}

void ClientConnection::Close(const std::string& error) {
  resolver_.cancel();
  timer_.cancel();

  if (socket_) {
      socket_->close();
    // In case of SSL connections, boost::asio tries to write to the socket
    // after the SSL stream and the socket was shutdown. Therefore, we delay
    // the deletion and hope that no segmentation fault is triggered. The
    // correct way would have been to use a shared_ptr for the socket.
    service_.post(boost::bind(&ClientConnection::DelayedSocketDeletionHandler,
                              socket_));
    socket_ = NULL;
  }

  connection_state_ = CLOSED;
  SendError(POSIX_ERROR_EIO,
            "Connection to '" + server_name_ + ":" + server_port_ + "' closed"
                " locally due to: " + error);
}

void ClientConnection::PostWrite(const boost::system::error_code& err,
                                 size_t bytes_written) {
  if (err == asio::error::operation_aborted || err == asio::error::eof
      || connection_state_ == CLOSED) {
    return;
  }
  if (err) {
    Reset();
    SendError(POSIX_ERROR_EIO,
              "Could not send request to '" + server_name_ + ":" +server_port_
                  + "': " + err.message());
  } else {
    // Pop sent request.
    if (!requests_.empty()) {
      requests_.pop();
      connection_state_ = IDLE;

      if (!requests_.empty()) {
        SendRequest();
      }
    }
  }
}

void ClientConnection::PostReadRecordMarker(
    const boost::system::error_code& err) {
  if (err == asio::error::operation_aborted || err == asio::error::eof
      || connection_state_ == CLOSED) {
    return;
  }
  if (err) {
    Reset();
    SendError(POSIX_ERROR_EIO,
              "could not read record marker in response from '" + server_name_
                  + ":" + server_port_ + "': " + err.message());
  } else {
#ifdef HAS_VALGRIND
    // On some OpenSSL versions with SSLv3 connections, Valgrind reports the
    // marker buffer as not initialized.
    if (RUNNING_ON_VALGRIND > 0) {
      VALGRIND_MAKE_MEM_DEFINED(receive_marker_buffer_,
                                RecordMarker::get_size());
    }
#endif  // HAS_VALGRIND
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
                        boost::bind(&ClientConnection::PostReadMessage,
                                    this,
                                    asio::placeholders::error));
  }
}

void ClientConnection::PostReadMessage(const boost::system::error_code& err) {
  if (err == asio::error::operation_aborted || err == asio::error::eof
      || connection_state_ == CLOSED) {
    return;
  }
  if (err) {
    DeleteInternalBuffers();
    Reset();
    SendError(POSIX_ERROR_EIO,
              "could not read response from '" + server_name_ + ":"
                  + server_port_ + "': " + err.message());
  } else {
#ifdef HAS_VALGRIND
    // On some OpenSSL versions with SSLv3 connections, Valgrind reports the
    // header buffer as not initialized.
    if (RUNNING_ON_VALGRIND > 0) {
      VALGRIND_MAKE_MEM_DEFINED(receive_hdr_, receive_marker_->header_len());
    }
#endif  // HAS_VALGRIND
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
      SendError(POSIX_ERROR_EINVAL,
                "received garbage header from '" + server_name_ + ":"
                    + server_port_ + "', closing connection");
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
            << "Received response for unknown request from "
               "'" << server_name_ << ":" << server_port_ << "'"
               " (call id = " << respHdr->call_id() << ")." << endl;
      }
      DeleteInternalBuffers();
      delete respHdr;

      // Receive next request.
      ReceiveRequest();

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
  delete[] receive_marker_buffer_;
  DeleteInternalBuffers();
}

}  // namespace rpc
}  // namespace xtreemfs
