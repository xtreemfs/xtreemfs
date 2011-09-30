/*
 * Copyright (c) 2009-2010 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_RPC_SYNC_CALLBACK_H_
#define CPP_INCLUDE_RPC_SYNC_CALLBACK_H_

#include <boost/cstdint.hpp>
#include <boost/thread.hpp>

#include "pbrpc/RPC.pb.h"
#include "rpc/client_request_callback_interface.h"
#include "rpc/client_request.h"

#include "util/logging.h"

namespace xtreemfs {
namespace rpc {
using boost::int32_t;
using boost::uint32_t;

template <class ReturnMessageType> class SyncCallback
    : public ClientRequestCallbackInterface {
 public:
  SyncCallback();
  virtual ~SyncCallback();

  /**
   * Returns if the rpc has finished (response was received or error).
   * This operation does not block.
   * @return true, if the RPC has finished
   */
  bool HasFinished();

  /**
   * Returns true if the request has failed. Blocks until
   * response is available.
   * @return true if an error occured
   */
  bool HasFailed();

  /**
   * Returns a pointer to the error or NULL if the request was successful.
   * Blocks until response is available.
   * @return pointer to ErrorResponse, caller is responsible for deleting
   * the object or calling deleteBuffers
   */
  xtreemfs::pbrpc::RPCHeader::ErrorResponse* error();

  /**
   * Returns a pointer to the response message. Blocks until response is
   * available.
   * @return pointer to response message, caller is responsible for
   * deleting the object or calling deleteBuffers
   */
  ReturnMessageType* response();

  /**
   * Returns the length of the response data or 0.
   * Blocks until response is available.
   * @return ength of the response data or 0
   */
  uint32_t data_length();

  /**
   * Returns a pointer to the response data. Blocks until response
   * is available.
   * @return pointer to response data, caller is responsible for
   * deleting[] the data or calling deleteBuffers
   */
  char* data();

  /**
   * Deletes the response objects (message, response, data)
   * This is not done automatically when the SyncCallback is deleted!
   */
  void DeleteBuffers();

  /** internal callback, ignore */
  virtual void RequestCompleted(ClientRequest *rq);

 private:
  boost::mutex cond_lock_;
  boost::condition_variable response_avail_;
  ClientRequest *request_;

  void WaitForResponse();
};

template <class ReturnMessageType>
SyncCallback<ReturnMessageType>::SyncCallback()
    : cond_lock_(),
      response_avail_(),
      request_(NULL) {
}

template <class ReturnMessageType>
SyncCallback<ReturnMessageType>::~SyncCallback() {
  boost::lock_guard<boost::mutex> lock(cond_lock_);
  delete request_;
}

template <class ReturnMessageType>
void SyncCallback<ReturnMessageType>::RequestCompleted(ClientRequest* rq) {
  boost::lock_guard<boost::mutex> lock(cond_lock_);
  request_ = rq;
  response_avail_.notify_all();
}

template <class ReturnMessageType>
void SyncCallback<ReturnMessageType>::WaitForResponse() {
  boost::unique_lock<boost::mutex> lock(cond_lock_);
  while (!request_) {
    response_avail_.wait(lock);
  }
}

template <class ReturnMessageType>
bool SyncCallback<ReturnMessageType>::HasFinished() {
  boost::unique_lock<boost::mutex> lock(cond_lock_);
  return (request_ != NULL);
}

template <class ReturnMessageType>
bool SyncCallback<ReturnMessageType>::HasFailed() {
  WaitForResponse();
  return (request_->error() != NULL);
}

template <class ReturnMessageType>
uint32_t SyncCallback<ReturnMessageType>::data_length() {
  WaitForResponse();
  return (request_->resp_data_len());
}

template <class ReturnMessageType>
char* SyncCallback<ReturnMessageType>::data() {
  WaitForResponse();
  return (request_->resp_data());
}

template <class ReturnMessageType>
xtreemfs::pbrpc::RPCHeader::ErrorResponse*
SyncCallback<ReturnMessageType>::error() {
  WaitForResponse();
  return request_->error();
}

template <class ReturnMessageType>
ReturnMessageType* SyncCallback<ReturnMessageType>::response() {
  WaitForResponse();
  return reinterpret_cast<ReturnMessageType*>(request_->resp_message());
}

template <class ReturnMessageType>
void SyncCallback<ReturnMessageType>::DeleteBuffers() {
  delete[] request_->resp_data();
  delete request_->error();
  delete request_->resp_message();
}

}  // namespace rpc
}  // namespace xtreemfs

#endif  // CPP_INCLUDE_RPC_SYNC_CALLBACK_H_
