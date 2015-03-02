/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "rpc/sync_callback.h"

#include "rpc/client_request.h"

namespace xtreemfs {
namespace rpc {

SyncCallbackBase::SyncCallbackBase() : request_(NULL) {
}

SyncCallbackBase::~SyncCallbackBase() {
  // TODO(mberlin): Is a lock here really needed?!
  boost::lock_guard<boost::mutex> lock(cond_lock_);
  delete request_;
}

void SyncCallbackBase::RequestCompleted(ClientRequest* rq) {
  boost::lock_guard<boost::mutex> lock(cond_lock_);
  request_ = rq;
  response_avail_.notify_all();
}

void SyncCallbackBase::WaitForResponse() {
  boost::unique_lock<boost::mutex> lock(cond_lock_);
  while (!request_) {
    response_avail_.wait(lock);
  }
}

bool SyncCallbackBase::HasFinished() {
  boost::unique_lock<boost::mutex> lock(cond_lock_);
  return (request_ != NULL);
}

bool SyncCallbackBase::HasFailed() {
  WaitForResponse();
  return (request_->error() != NULL);
}

uint32_t SyncCallbackBase::data_length() {
  WaitForResponse();
  return (request_->resp_data_len());
}

char* SyncCallbackBase::data() {
  WaitForResponse();
  return (request_->resp_data());
}

xtreemfs::pbrpc::RPCHeader::ErrorResponse* SyncCallbackBase::error() {
  WaitForResponse();
  return request_->error();
}

::google::protobuf::Message* SyncCallbackBase::response() {
  WaitForResponse();
  return request_->resp_message();
}

void SyncCallbackBase::DeleteBuffers() {
  if (request_) {
    request_->clear_error();
    request_->clear_resp_message();
    request_->clear_resp_data();
  }
}

}  // namespace rpc
}  // namespace xtreemfs

