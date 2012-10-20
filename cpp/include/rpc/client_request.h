/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *                    2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_CLIENT_REQUEST_H_
#define CPP_INCLUDE_RPC_CLIENT_REQUEST_H_

#include <boost/date_time/posix_time/posix_time_types.hpp>
#include <stdint.h>
#include <string>

#include "include/Common.pb.h"
#include "pbrpc/RPC.pb.h"

namespace xtreemfs {
namespace rpc {

class ClientConnection;
class ClientRequest;
class ClientRequestCallbackInterface;
class RecordMarker;

class ClientRequest {
 public:
  static const int ERR_NOERR = 0;

  ClientRequest(const std::string& address,
                const uint32_t call_id,
                const uint32_t interface_id,
                const uint32_t proc_id,
                const xtreemfs::pbrpc::UserCredentials& userCreds,
                const xtreemfs::pbrpc::Auth& auth,
                const google::protobuf::Message* request_message,
                const char* request_data,
                const int data_length,
                google::protobuf::Message* response_message,
                void *context,
                ClientRequestCallbackInterface* callback);

  virtual ~ClientRequest();

  void ExecuteCallback();

  void RequestSent();

  /** Used by Client::handleTimeout() to find the respective ClientConnection.
   *
   * @remarks This object does not have the ownership of "client_connection_",
   *          so it does not get transferred.
   */
  ClientConnection* client_connection() {
    return client_connection_;
  }

  /**
   * @remarks Ownership is not transferred. Instead, it's assumed that this
   *          ClientRequests exists as long as "client_connection".
   */
  void set_client_connection(ClientConnection* client_connection) {
    client_connection_ = client_connection;
  }

  void set_rq_data(const char* rq_data) {
    this->rq_data_ = rq_data;
  }

  const char* rq_data() const {
    return rq_data_;
  }

  void set_rq_hdr_msg(char* rq_hdr_msg) {
    this->rq_hdr_msg_ = rq_hdr_msg;
  }

  char* rq_hdr_msg() const {
    return rq_hdr_msg_;
  }

  void set_request_marker(RecordMarker* request_marker) {
    this->request_marker_ = request_marker;
  }

  RecordMarker* request_marker() const {
    return request_marker_;
  }

  void set_resp_data(char* resp_data) {
    this->resp_data_ = resp_data;
  }

  char* resp_data() const {
    return resp_data_;
  }

  void clear_resp_data() {
    delete[] resp_data_;
    resp_data_ = NULL;
    resp_data_len_ = 0;
  }

  void set_resp_header(xtreemfs::pbrpc::RPCHeader* resp_header) {
    this->resp_header_ = resp_header;
  }

  xtreemfs::pbrpc::RPCHeader* resp_header() const {
    return resp_header_;
  }

  void set_address(std::string address) {
    this->address_ = address_;
  }

  std::string address() const {
    return address_;
  }

  uint32_t call_id() const {
    return call_id_;
  }

  uint32_t interface_id() const {
    return interface_id_;
  }

  uint32_t proc_id() const {
    return proc_id_;
  }

  boost::posix_time::ptime time_sent() const {
    return time_sent_;
  }

  google::protobuf::Message* resp_message() const {
    return resp_message_;
  }

  void clear_resp_message() {
    delete resp_message_;
    resp_message_ = NULL;
  }

  void set_error(xtreemfs::pbrpc::RPCHeader::ErrorResponse* error) {
    if (!error_) {
      // Process first error only.
      this->error_ = error;
    } else {
      delete error;
    }
  }

  void clear_error() {
    delete error_;
    error_ = NULL;
  }

  xtreemfs::pbrpc::RPCHeader::ErrorResponse* error() const {
    return error_;
  }

  void* context() const {
    return context_;
  }

  void set_resp_data_len(uint32_t resp_data_len_) {
    this->resp_data_len_ = resp_data_len_;
  }

  uint32_t resp_data_len() const {
    return resp_data_len_;
  }

 private:
  /** Pointer to the ClientConnection which is responsible for this object. */
  ClientConnection* client_connection_;

  /** ID of the request to match received responses to sent requests. */
  const uint32_t call_id_;
  /** Type of interface (service) which will be contacted. */
  const uint32_t interface_id_;
  /** Number of the operation which will be executed. */
  const uint32_t proc_id_;
  void *context_;
  ClientRequestCallbackInterface *callback_;
  std::string address_;
  boost::posix_time::ptime time_sent_;
  bool callback_executed_;

  /** Internal buffers (will be deleted with the object). */
  RecordMarker *request_marker_;
  char *rq_hdr_msg_;

  /** Buffers which are passed to the callback. */
  xtreemfs::pbrpc::RPCHeader::ErrorResponse *error_;
  const char *rq_data_;
  xtreemfs::pbrpc::RPCHeader *resp_header_;
  google::protobuf::Message *resp_message_;
  char *resp_data_;
  uint32_t resp_data_len_;

  void deleteInternalBuffers();
};

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_CLIENT_REQUEST_H_

