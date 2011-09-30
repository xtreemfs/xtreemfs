/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include <google/protobuf/message.h>

#include <string>

#include "rpc/client_request.h"
#include "util/logging.h"
#include "pbrpc/RPC.pb.h"

namespace xtreemfs {
namespace rpc {
using namespace xtreemfs::pbrpc;
using namespace xtreemfs::util;
using namespace std;
using namespace boost;
using namespace google::protobuf;

ClientRequest::ClientRequest(const string& address,
                             uint32_t call_id,
                             uint32_t interface_id,
                             uint32_t proc_id,
                             const UserCredentials& userCreds,
                             const Auth& auth,
                             const Message* request_message,
                             const char* request_data,
                             int data_length,
                             Message* response_message,
                             void *context,
                             ClientRequestCallbackInterface *callback)
    : call_id_(call_id),
      context_(context),
      callback_(callback),
      address_(address),
      canceled_(false),
      callback_executed_(false),
      error_(NULL),
      resp_header_(NULL),
      resp_message_(response_message),
      resp_data_(NULL),
      resp_data_len_(0) {
  RPCHeader header = RPCHeader();
  header.set_message_type(xtreemfs::pbrpc::RPC_REQUEST);
  header.set_call_id(call_id);
  header.mutable_request_header()->set_interface_id(interface_id);
  header.mutable_request_header()->set_proc_id(proc_id);
  header.mutable_request_header()->mutable_user_creds()->
      MergeFrom(userCreds);
  header.mutable_request_header()->mutable_auth_data()->MergeFrom(auth);

  assert(callback_ != NULL);

  uint32_t msg_len =
      (request_message == NULL) ? 0 : request_message->ByteSize();
  this->request_marker_ = new RecordMarker(header.ByteSize(),
      msg_len, data_length);
  this->rq_hdr_msg_ = new char[RecordMarker::get_size()
      + this->request_marker_->header_len()
      + request_marker_->message_len()];
  char *hdrPtr = this->rq_hdr_msg_ + RecordMarker::get_size();
  char *msgPtr = hdrPtr + request_marker_->header_len();
  request_marker_->serialize(rq_hdr_msg_);
  header.SerializeToArray(hdrPtr, request_marker_->header_len());
  if (msg_len > 0) {
    request_message->SerializeToArray(msgPtr, request_marker_->message_len());
    if (!request_message->IsInitialized()) {
      string errmsg = string("message is not valid. Not all required "
                             "fields have been initialized: ") +
          request_message->InitializationErrorString();
      xtreemfs::util::Logging::log->getLog(xtreemfs::util::LEVEL_ERROR)
          << errmsg << endl;
      throw std::runtime_error(errmsg);
    }
  }

  this->rq_data_ = request_data;
}

void ClientRequest::deleteInternalBuffers() {
  if (request_marker_)
    delete request_marker_;
  if (rq_hdr_msg_)
    delete[] rq_hdr_msg_;
  if (resp_header_)
    delete resp_header_;
}

ClientRequest::~ClientRequest() {
  deleteInternalBuffers();
}

void ClientRequest::ExecuteCallback() {
  if (!callback_executed_) {
    callback_executed_ = true;
    callback_->RequestCompleted(this);
  }
}

void ClientRequest::RequestSent() {
  time_sent_ = posix_time::microsec_clock::local_time();
}

}  // namespace rpc
}  // namespace xtreemfs
