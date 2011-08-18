/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_ASYNC_WRITE_HANDLER_H_
#define CPP_INCLUDE_LIBXTREEMFS_ASYNC_WRITE_HANDLER_H_

#include <boost/thread/condition.hpp>
#include <boost/thread/mutex.hpp>
#include <list>

#include "rpc/callback_interface.h"

namespace xtreemfs {

class AsyncWriteBuffer;
class FileInfo;
class UUIDResolver;
class UUIDIterator;

namespace pbrpc {
class OSDServiceClient;
class OSDWriteResponse;
}  // namespace pbrpc

class AsyncWriteHandler
    : public xtreemfs::rpc::CallbackInterface<
          xtreemfs::pbrpc::OSDWriteResponse> {
 public:
  AsyncWriteHandler(
      FileInfo* file_info,
      UUIDIterator* uuid_iterator,
      UUIDResolver* uuid_resolver,
      xtreemfs::pbrpc::OSDServiceClient* osd_service_client,
      const xtreemfs::pbrpc::Auth& auth_bogus,
      const xtreemfs::pbrpc::UserCredentials& user_credentials_bogus,
      int max_writeahead,
      int max_writeahead_requests,
      int max_write_tries);

  ~AsyncWriteHandler();

  /** Adds write_buffer to the list of pending writes and sends it to the OSD
   *  specified by write_buffer->uuid_iterator (or write_buffer->osd_uuid if
   *  write_buffer->use_uuid_iterator is false).
   *
   *  Blocks if the number of pending bytes exceeds the maximum write-ahead
   *  or WaitForPendingWrites{NonBlocking}() was called beforehand.
   */
  void Write(AsyncWriteBuffer* write_buffer);

  /** Blocks until state changes back to IDLE and prevents allowing new writes.
   *  by blocking further Write() calls. */
  void WaitForPendingWrites();

  /** If waiting for pending writes would block, it returns true and adds
   *  the parameters to the list waiting_observers_ and calls notify_one()
   *  on condition_variable once state_ changed back to IDLE. */
  bool WaitForPendingWritesNonBlocking(boost::condition* condition_variable,
                                       bool* wait_completed,
                                       boost::mutex* wait_completed_mutex);

 private:
  /** Possible states of this object. */
  enum State {
    IDLE, WRITES_PENDING
  };

  /** Contains information about observer who has to be notified once all
   *  currently pending writes have finished. */
  struct WaitForCompletionObserver {
    WaitForCompletionObserver(boost::condition* condition_variable,
                              bool* wait_completed,
                              boost::mutex* wait_completed_mutex)
        : condition_variable(condition_variable),
          wait_completed(wait_completed),
          wait_completed_mutex(wait_completed_mutex) {
      assert(condition_variable && wait_completed && wait_completed_mutex);
    }
    boost::condition* condition_variable;
    bool* wait_completed;
    boost::mutex* wait_completed_mutex;
  };

  /** Implements callback for an async write request. */
  virtual void CallFinished(xtreemfs::pbrpc::OSDWriteResponse* response_message,
                            char* data, boost::uint32_t data_length,
                            xtreemfs::pbrpc::RPCHeader::ErrorResponse* error,
                            void* context);

  /** Helper function which adds "write_buffer" to the list writes_in_flight_,
   *  increases the number of pending bytes and takes care of state changes.
   *
   *  @remark   Ownership is not transferred to the caller.
   *  @remark   Requires a lock on mutex_.
   */
  void IncreasePendingBytesHelper(AsyncWriteBuffer* write_buffer,
                                  boost::mutex::scoped_lock* lock);

  /** Helper function which removes "write_buffer" from the list
   *  writes_in_flight_, deletes "write_buffer", reduces the number of pending
   *  bytes and takes care of state changes.
   *
   *  @remark   Ownership of "write_buffer" is transferred to the caller.
   *  @remark   Requires a lock on mutex_.
   */
  void DecreasePendingBytesHelper(AsyncWriteBuffer* write_buffer,
                                  boost::mutex::scoped_lock* lock);

  /** Calls notify_one() on all observers in waiting_observers_, frees each
   *  element in the list and clears the list afterwards.
   *
   *  @remark   Requires a lock on mutex_.
   */
  void NotifyWaitingObserversAndClearAll(boost::mutex::scoped_lock* lock);

  /** Use this when modifying the object. */
  boost::mutex mutex_;

  /** State of this object. */
  State state_;

  /** List of pending writes. */
  // TODO(mberlin): Limit the size of writes in flight to avoid flooding.
  std::list<AsyncWriteBuffer*> writes_in_flight_;

  /** Number of pending bytes. */
  int pending_bytes_;

  /** Set by WaitForPendingWrites{NonBlocking}() to true if there are
   *  temporarily no new async writes allowed and will be set to false again
   *  once the state IDLE is reached. */
  bool writing_paused_;
  
  /** Used to notify blocked WaitForPendingWrites() callers for the state change
   *  back to IDLE. */
  boost::condition all_pending_writes_did_complete_;

  /** Number of threads blocked by WaitForPendingWrites() waiting on
   *  all_pending_writes_did_complete_ for a state change back to IDLE.
   *
   *  This does not include the number of waiting threads which did call
   *  WaitForPendingWritesNonBlocking(). Therefore, see "waiting_observers_".
   *  The total number of all waiting threads is:
   *    waiting_blocking_threads_count_ + waiting_observers_.size()
   */
  int waiting_blocking_threads_count_;

  /** Used to notify blocked Write() callers that the number of pending bytes
   *  has decreased. */
  boost::condition pending_bytes_were_decreased_;

  /** List of WaitForPendingWritesNonBlocking() observers (specified by their
   *  boost::condition variable and their bool value which will be set to true
   *  if the state changed back to IDLE). */
  std::list<WaitForCompletionObserver*> waiting_observers_;

  /** FileInfo object to which this AsyncWriteHandler does belong. Accessed for
   *  file size updates. */
  FileInfo* file_info_;

  /** Pointer to the UUIDIterator of the FileInfo object. */
  UUIDIterator* uuid_iterator_;

  /** Required for resolving UUIDs to addresses. */
  UUIDResolver* uuid_resolver_;

  /** Client which is used to send out the writes. */
  xtreemfs::pbrpc::OSDServiceClient* osd_service_client_;

  /** Auth needed for ServiceClients. Always set to AUTH_NONE by Volume. */
  const xtreemfs::pbrpc::Auth& auth_bogus_;

  /** For same reason needed as auth_bogus_. Always set to user "xtreemfs". */
  const xtreemfs::pbrpc::UserCredentials& user_credentials_bogus_;

  /** Maximum number in bytes which may be pending. */
  const int max_writeahead_;

  /** Maximum number of pending write requests. */
  const int max_writeahead_requests_;

  /** Maximum number of attempts a write will be tried. */
  const int max_write_tries_;
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_ASYNC_WRITE_HANDLER_H_
