/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_FILE_HANDLE_IMPLEMENTATION_H_
#define CPP_INCLUDE_LIBXTREEMFS_FILE_HANDLE_IMPLEMENTATION_H_

#include <stdint.h>

#include <boost/function.hpp>
#include <boost/thread/condition.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/scoped_ptr.hpp>
#include <gtest/gtest_prod.h>
#include <map>
#include <string>

#include "pbrpc/RPC.pb.h"
#include "rpc/callback_interface.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/MRC.pb.h"
#include "libxtreemfs/client_implementation.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/interrupt.h"
#include "libxtreemfs/xcap_handler.h"

namespace xtreemfs {

namespace rpc {
class SyncCallbackBase;
}  // namespace rpc

namespace pbrpc {
class FileCredentials;
class Lock;
class MRCServiceClient;
class OSDServiceClient;
class readRequest;
class writeRequest;
}  // namespace pbrpc

class FileInfo;
class Options;
class StripeTranslator;
class UUIDIterator;
class UUIDResolver;
class Volume;
class XCapManager;

class XCapManager :
    public rpc::CallbackInterface<xtreemfs::pbrpc::XCap>,
    public XCapHandler {
 public:
  XCapManager(
      const xtreemfs::pbrpc::XCap& xcap,
      pbrpc::MRCServiceClient* mrc_service_client,
      UUIDResolver* uuid_resolver,
      UUIDIterator* mrc_uuid_iterator,
      const pbrpc::Auth& auth_bogus,
      const pbrpc::UserCredentials& user_credentials_bogus);

  /** Renew xcap_ asynchronously. */
  void RenewXCapAsync(const RPCOptions& options);

  /** Blocks until the callback has completed (if an XCapRenewal is pending). */
  void WaitForPendingXCapRenewal();

  /** XCapHandler: Get current capability.*/
  virtual void GetXCap(xtreemfs::pbrpc::XCap* xcap);

  /** Update the capability with the provided one. */
  void SetXCap(const xtreemfs::pbrpc::XCap& xcap);

  /** Get the file id from the capability. */
  uint64_t GetFileId();

 private:
  /** Implements callback for an async xtreemfs_renew_capability request. */
  virtual void CallFinished(xtreemfs::pbrpc::XCap* new_xcap,
                            char* data,
                            uint32_t data_length,
                            pbrpc::RPCHeader::ErrorResponse* error,
                            void* context);

  /** Any modification to the object must obtain a lock first. */
  boost::mutex mutex_;

  /** Capabilitiy for the file, used to authorize against services */
  xtreemfs::pbrpc::XCap xcap_;

  /** True if there is an outstanding xcap_renew callback. */
  bool xcap_renewal_pending_;

  /** Used to wait for pending XCap renewal callbacks. */
  boost::condition xcap_renewal_pending_cond_;

  /** UUIDIterator of the MRC. */
  pbrpc::MRCServiceClient* mrc_service_client_;
  UUIDResolver* uuid_resolver_;
  UUIDIterator* mrc_uuid_iterator_;

  /** Auth needed for ServiceClients. Always set to AUTH_NONE by Volume. */
  const pbrpc::Auth auth_bogus_;

  /** For same reason needed as auth_bogus_. Always set to user "xtreemfs". */
  const pbrpc::UserCredentials user_credentials_bogus_;
};

/** Default implementation of the FileHandle Interface. */
class FileHandleImplementation
    : public FileHandle,
      public XCapHandler,
      public rpc::CallbackInterface<pbrpc::timestampResponse> {
 public:
  FileHandleImplementation(
      ClientImplementation* client,
      const std::string& client_uuid,
      FileInfo* file_info,
      const pbrpc::XCap& xcap,
      UUIDIterator* mrc_uuid_iterator,
      UUIDIterator* osd_uuid_iterator,
      UUIDResolver* uuid_resolver,
      pbrpc::MRCServiceClient* mrc_service_client,
      pbrpc::OSDServiceClient* osd_service_client,
      const std::map<pbrpc::StripingPolicyType,
                     StripeTranslator*>& stripe_translators,
      bool async_writes_enabled,
      const Options& options,
      const pbrpc::Auth& auth_bogus,
      const pbrpc::UserCredentials& user_credentials_bogus);

  virtual ~FileHandleImplementation();

  virtual int Read(char *buf, size_t count, int64_t offset);

  virtual int Write(const char *buf, size_t count, int64_t offset);

  virtual void Flush();

  virtual void Truncate(
      const pbrpc::UserCredentials& user_credentials,
      int64_t new_file_size);

  /** Used by Truncate() and Volume->OpenFile() to truncate the file to
   *  "new_file_size" on the OSD and update the file size at the MRC.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   **/
  void TruncatePhaseTwoAndThree(int64_t new_file_size);

  virtual void GetAttr(
      const pbrpc::UserCredentials& user_credentials,
      pbrpc::Stat* stat);

  virtual xtreemfs::pbrpc::Lock* AcquireLock(
      int process_id,
      uint64_t offset,
      uint64_t length,
      bool exclusive,
      bool wait_for_lock);

  virtual xtreemfs::pbrpc::Lock* CheckLock(
      int process_id,
      uint64_t offset,
      uint64_t length,
      bool exclusive);

  virtual void ReleaseLock(
      int process_id,
      uint64_t offset,
      uint64_t length,
      bool exclusive);

  /** Also used by FileInfo object to free active locks. */
  void ReleaseLock(const pbrpc::Lock& lock);

  virtual void ReleaseLockOfProcess(int process_id);

  virtual void PingReplica(const std::string& osd_uuid);

  virtual void Close();

  /** Returns the StripingPolicy object for a given type (e.g. Raid0).
   *
   *  @remark Ownership is NOT transferred to the caller.
   */
  const StripeTranslator* GetStripeTranslator(
      pbrpc::StripingPolicyType type);

  /** Sets async_writes_failed_ to true. */
  void MarkAsyncWritesAsFailed();
  /** Thread-safe check if async_writes_failed_ */
  bool DidAsyncWritesFail();
  /** Thread-safe check and throw if async_writes_failed_ */
  void ThrowIfAsyncWritesFailed();

  /** Sends pending file size updates synchronous (needed for flush/close).
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  void WriteBackFileSize(const pbrpc::OSDWriteResponse& owr,
                         bool close_file);

  /** Sends osd_write_response_for_async_write_back_ asynchronously. */
  void WriteBackFileSizeAsync(const RPCOptions& options);

  /** Overwrites the current osd_write_response_ with "owr". */
  void set_osd_write_response_for_async_write_back(
      const pbrpc::OSDWriteResponse& owr);

  /** Wait for all asyncronous operations to finish */
  void WaitForAsyncOperations();

  /** Execute period tasks */
  void ExecutePeriodTasks(const RPCOptions& options);
  
  /** XCapHandler: Get current capability. */
  virtual void GetXCap(xtreemfs::pbrpc::XCap* xcap);

 private:
  /**
   * Execute the operation and check on invalid view exceptions.
   * If the operation was executed with an outdated, the view
   * will be renewed and the operation retried.
   */
  template<typename T>
  T ExecuteViewCheckedOperation(boost::function<T()> operation);

  /** Renew the xLocSet synchronously. */
  void RenewXLocSet();

  /** Implements callback for an async xtreemfs_update_file_size request. */
  virtual void CallFinished(
      pbrpc::timestampResponse* response_message,
      char* data,
      uint32_t data_length,
      pbrpc::RPCHeader::ErrorResponse* error,
      void* context);

  /** Same as Flush(), takes special actions if called by Close(). */
  void Flush(bool close_file);

  /** Actual implementation of Flush(). */
  void DoFlush(bool close_file);

  /** Actual implementation of Read(). */
  int DoRead(
      char *buf,
      size_t count,
      int64_t offset);

  /** Read data from the OSD. Objects owned by the caller. */
  int ReadFromOSD(
      UUIDIterator* uuid_iterator,
      const pbrpc::FileCredentials& file_credentials,
      int object_no,
      char* buffer,
      int offset_in_object,
      int bytes_to_read);

  /** Actual implementation of Write(). */
  int DoWrite(
      const char *buf,
      size_t count,
      int64_t offset);

  /** Write data to the OSD. Objects owned by the caller. */
  void WriteToOSD(
      UUIDIterator* uuid_iterator,
      const pbrpc::FileCredentials& file_credentials,
      int object_no,
      int offset_in_object,
      const char* buffer,
      int bytes_to_write);

  /** Acutal implementation of TruncatePhaseTwoAndThree(). */
  void DoTruncatePhaseTwoAndThree(int64_t new_file_size);

  /** Actual implementation of AcquireLock(). */
  xtreemfs::pbrpc::Lock* DoAcquireLock(
      int process_id,
      uint64_t offset,
      uint64_t length,
      bool exclusive,
      bool wait_for_lock);

  /** Actual implementation of CheckLock(). */
  xtreemfs::pbrpc::Lock* DoCheckLock(
      int process_id,
      uint64_t offset,
      uint64_t length,
      bool exclusive);

  /** Actual implementation of ReleaseLock(). */
  void DoReleaseLock(const pbrpc::Lock& lock);

  /** Actual implementation of PingReplica(). */
  void DoPingReplica(const std::string& osd_uuid);

  /** Any modification to the object must obtain a lock first. */
  boost::mutex mutex_;

  /** Reference to Client which did open this volume. */
  ClientImplementation* client_;

  /** UUID of the Client (needed to distinguish Locks of different clients). */
  const std::string& client_uuid_;

  /** UUIDIterator of the MRC. */
  UUIDIterator* mrc_uuid_iterator_;

  /** UUIDIterator which contains the UUIDs of all replicas. */
  UUIDIterator* osd_uuid_iterator_;

  /** Needed to resolve UUIDs. */
  UUIDResolver* uuid_resolver_;

  /** Multiple FileHandle may refer to the same File and therefore unique file
   * properties (e.g. Path, FileId, XlocSet) are stored in a FileInfo object. */
  FileInfo* file_info_;

  // TODO(mberlin): Add flags member.

  /** Contains a file size update which has to be written back (or NULL). */
  boost::scoped_ptr<pbrpc::OSDWriteResponse>
      osd_write_response_for_async_write_back_;

  /** Pointer to object owned by VolumeImplemention */
  pbrpc::MRCServiceClient* mrc_service_client_;

  /** Pointer to object owned by VolumeImplemention */
  pbrpc::OSDServiceClient* osd_service_client_;

  const std::map<pbrpc::StripingPolicyType,
                 StripeTranslator*>& stripe_translators_;

  /** Set to true if async writes (max requests > 0, no O_SYNC) are enabled. */
  const bool async_writes_enabled_;

  /** Set to true if an async write of this file_handle failed. If true, this
   *  file_handle is broken and no further writes/reads/truncates are possible.
   */
  bool async_writes_failed_;

  const Options& volume_options_;

  /** Auth needed for ServiceClients. Always set to AUTH_NONE by Volume. */
  const pbrpc::Auth& auth_bogus_;

  /** For same reason needed as auth_bogus_. Always set to user "xtreemfs". */
  const pbrpc::UserCredentials& user_credentials_bogus_;

  XCapManager xcap_manager_;

  FRIEND_TEST(VolumeImplementationTestFastPeriodicFileSizeUpdate,
              WorkingPendingFileSizeUpdates);
  FRIEND_TEST(VolumeImplementationTest, FileSizeUpdateAfterFlush);
  FRIEND_TEST(VolumeImplementationTestFastPeriodicFileSizeUpdate,
              FileSizeUpdateAfterFlushWaitsForPendingUpdates);
  FRIEND_TEST(VolumeImplementationTestFastPeriodicXCapRenewal,
              WorkingXCapRenewal);
  FRIEND_TEST(VolumeImplementationTest, FilesLockingReleaseNonExistantLock);
  FRIEND_TEST(VolumeImplementationTest, FilesLockingReleaseExistantLock);
  FRIEND_TEST(VolumeImplementationTest, FilesLockingLastCloseReleasesAllLocks);
  FRIEND_TEST(VolumeImplementationTest, FilesLockingReleaseLockOfProcess);
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_FILE_HANDLE_IMPLEMENTATION_H_
