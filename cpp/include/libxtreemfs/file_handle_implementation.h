/*
 * Copyright (c) 2011 by Michael Berlin,
 *               2015 by Robert Bärhold
 *                    Zuse Institute Berlin
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
#include <list>
#include <map>
#include <string>
#include <vector>

#include "pbrpc/RPC.pb.h"
#include "rpc/callback_interface.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/MRC.pb.h"
#include "xtreemfs/OSD.pb.h"
#include "libxtreemfs/client_implementation.h"
#include "libxtreemfs/file_handle.h"
#include "libxtreemfs/interrupt.h"
#include "libxtreemfs/xcap_handler.h"
#include "libxtreemfs/xtreemfs_exception.h"

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
class VoucherManager;
class VoucherManagerCallback;

class VoucherManager : public rpc::CallbackInterface<xtreemfs::pbrpc::OSDFinalizeVouchersResponse> {
 public:
  VoucherManager(FileInfo* file_info, XCapManager* xcap_manager,
                 pbrpc::MRCServiceClient* mrc_service_client,
                 pbrpc::OSDServiceClient* osd_service_client_,
                 UUIDResolver* uuid_resolver, UUIDIterator* mrc_uuid_iterator,
                 UUIDIterator* osd_uuid_iterator,
                 const Options& volume_options,
                 const pbrpc::Auth& auth_bogus,
                 const pbrpc::UserCredentials& user_credentials_bogus);

  /** Handles the overall process of the finalize and clear voucher protocol. */
  void finalizeAndClear();
 private:
  /** Sends out the finalize voucher request in an asynchronous manner to all relevant OSDs. */
  void finalizeVoucher(xtreemfs::pbrpc::xtreemfs_finalize_vouchersRequest* finalizeVouchersRequest,
                       VoucherManagerCallback* callback);

  /** Sends out the clear voucher request to the MRC containing all OSD responses. */
  void clearVoucher(xtreemfs::pbrpc::xtreemfs_clear_vouchersRequest* clearVouchersRequest);

  /** Checks the consistency of all finalize OSD responses and returns true on equality. */
  bool checkResponseConsistency();

  /** Deletes every object in the osdFinalizeVoucherResponseVector_ and clears it. */
  void cleanupOSDResponses();

  /** Implements callback for the finalize voucher requests from the OSDs,
   * saving all responses in the osdFinalizeVoucherResponseVector_. */
  virtual void CallFinished(xtreemfs::pbrpc::OSDFinalizeVouchersResponse* response_message,
                            char* data,
                            uint32_t data_length,
                            pbrpc::RPCHeader::ErrorResponse* error,
                            void* context);

  /** Use this mutex guarantee a single call of finalize and clear. */
  boost::mutex mutex_;

  /** Used to wait on the condition. */
  boost::mutex cond_mutex_;

  /** Used to wait for finalize voucher respones of used OSDs. */
  boost::condition osd_finalize_pending_cond;

  /** number of osds, we expect a reponse of. */
  int osdCount;

  /** Used to save current finalize voucher responses from the OSDs. */
  std::vector<xtreemfs::pbrpc::OSDFinalizeVouchersResponse*> osdFinalizeVoucherResponseVector_;


  /** Multiple FileHandle may refer to the same File and therefore unique file
   * properties (e.g. Path, FileId, XlocSet) are stored in a FileInfo object. */
  FileInfo* file_info_;

  /** Pointer to the XCapManager instance of the file handle. */
  XCapManager* xcap_manager_;

  /** Pointer to object owned by VolumeImplemention */
  pbrpc::MRCServiceClient* mrc_service_client_;

  /** Pointer to object owned by VolumeImplemention */
  pbrpc::OSDServiceClient* osd_service_client_;

  /** UUID resolver*/
  UUIDResolver* uuid_resolver_;

  /** UUIDIterator of the MRC. */
  UUIDIterator* mrc_uuid_iterator_;

  /** UUIDIterator which contains the UUIDs of all replicas. */
  UUIDIterator* osd_uuid_iterator_;

  /** Volume options used in the requests. */
  const Options& volume_options_;

  /** Auth needed for ServiceClients. Always set to AUTH_NONE by Volume. */
  const pbrpc::Auth& auth_bogus_;

  /** For same reason needed as auth_bogus_. Always set to user "xtreemfs". */
  const pbrpc::UserCredentials& user_credentials_bogus_;
};

class VoucherManagerCallback : public rpc::CallbackInterface<xtreemfs::pbrpc::OSDFinalizeVouchersResponse> {
 public:
  VoucherManagerCallback(VoucherManager* voucherManager,
                         const int tryNo,
                         const int osdCount);

  /** Unregisters the VoucherManager that created the Callback.
   * If there are finalize voucher requests in flight, the Callback
   * will be kept in memory until every response has arrived.
   * Otherwise the Callback will destroy itself. */
  void unregisterManager();

 private:
  /** Implements callback for the finalize voucher requests from the OSDs.
   * Redirects every response to the registered VoucherManager CallFinished.
   * If no VoucherManager is registered the responses are discarded/freed.
   * If no VoucherManager is registered and every finalize voucher response
   * has arrived, the VoucherManagerCallback destroys itself. */
  virtual void CallFinished(xtreemfs::pbrpc::OSDFinalizeVouchersResponse* response_message,
                            char* data,
                            uint32_t data_length,
                            pbrpc::RPCHeader::ErrorResponse* error,
                            void* context);

  /** Use this mutex to guard changes/checks to voucherManager_. */
  boost::mutex mutex_;

  /** The VoucherManager that created this Callback.
   * Or NULL if it has been unregistered. */
  rpc::CallbackInterface<xtreemfs::pbrpc::OSDFinalizeVouchersResponse>* voucherManager_;
  /** The number of the try on which this callback was created. */
  const int tryNo_;
  /** The number of OSDs and respective number of requests sent for this try. */
  const int osdCount_;
  /** The number of responses for this try. */
  int respCount_;
};

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

  virtual ~XCapManager();

  /** Renew xcap_ asynchronously. */
  void RenewXCapAsync(const RPCOptions& options);

  /** Renew xcap_ asynchronously. Add writeback, in case of an error */
  void RenewXCapAsync(const RPCOptions& options, const bool increaseVoucher,
                      PosixErrorException* writeback);

  /** Blocks until the callback has completed (if an XCapRenewal is pending). */
  void WaitForPendingXCapRenewal();

  /** XCapHandler: Get current capability.*/
  virtual void GetXCap(xtreemfs::pbrpc::XCap* xcap);

  /** Update the capability with the provided one. */
  void SetXCap(const xtreemfs::pbrpc::XCap& xcap);

  /** Get the file id from the capability. */
  uint64_t GetFileId();

  /** Returns the list of old expire times. */
  std::list< ::google::protobuf::uint64>& GetOldExpireTimes();

  /** Acquires the mutex related to list of old expire times. */
  void acquireOldExpireTimesMutex();

  /** Releases the mutex related to list of old expire times. */
  void releaseOldExpireTimesMutex();

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

  /** Used to keep track of possible writebacks of errros, occured at the renewal. */
  std::list<PosixErrorException*> xcap_renewal_error_writebacks_;

  /** Any modification on the xcap_renewal_error_writebacks_ list have to obtain this lock first. */
  boost::mutex xcap_renewal_error_writebacks_mutex_;

  /** Used to keep track of old expire times to finalize voucher requests. **/
  std::list< ::google::protobuf::uint64> old_expire_times_;

  /** Use this to protect old_expire_times. */
  boost::mutex old_expire_times_mutex_;

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

  virtual std::string GetLastOSDAddress();

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

  /** Mutex used for writing last OSD address. */
  boost::mutex last_osd_mutex_;

  /** Address of the OSD that was last used for reading or writing. */
  std::string last_osd_address_;

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
