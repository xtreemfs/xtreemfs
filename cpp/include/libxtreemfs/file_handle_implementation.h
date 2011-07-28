/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_FILE_HANDLE_IMPLEMENTATION_H_
#define CPP_INCLUDE_LIBXTREEMFS_FILE_HANDLE_IMPLEMENTATION_H_

#include <boost/cstdint.hpp>
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

#include "libxtreemfs/file_handle.h"

namespace xtreemfs {

namespace pbrpc {
class Lock;
class MRCServiceClient;
class OSDServiceClient;
}  // namespace pbrpc

class FileInfo;
class Options;
class StripeTranslator;
class UUIDResolver;
class Volume;

/** Default implementation of the FileHandle Interface. */
class FileHandleImplementation
  : public FileHandle,
    public xtreemfs::rpc::CallbackInterface<xtreemfs::pbrpc::timestampResponse>,
    public xtreemfs::rpc::CallbackInterface<xtreemfs::pbrpc::XCap> {
 public:
  FileHandleImplementation(
      const std::string& client_uuid,
      const std::string& mrc_uuid,
      FileInfo* file_info,
      const xtreemfs::pbrpc::XCap& xcap,
      UUIDResolver* uuid_resolver,
      xtreemfs::pbrpc::MRCServiceClient* mrc_service_client,
      xtreemfs::pbrpc::OSDServiceClient* osd_service_client,
      const std::map<xtreemfs::pbrpc::StripingPolicyType,
                     StripeTranslator*>& stripe_translators,
      const Options& options,
      const xtreemfs::pbrpc::Auth& auth_bogus,
      const xtreemfs::pbrpc::UserCredentials& user_credentials_bogus);

  virtual ~FileHandleImplementation();

  virtual int Read(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      char *buf,
      size_t count,
      off_t offset);

  virtual int Write(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const char *buf,
      size_t count,
      off_t offset);

  virtual void Flush();

  virtual void Truncate(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      off_t new_file_size);

  /** Used by Truncate() and Volume->OpenFile() to truncate the file to
   *  "new_file_size" on the OSD and update the file size at the MRC.
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   **/
  void TruncatePhaseTwoAndThree(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      off_t new_file_size);

  virtual void GetAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      xtreemfs::pbrpc::Stat* stat);

  virtual xtreemfs::pbrpc::Lock* AcquireLock(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      int process_id,
      boost::uint64_t offset,
      boost::uint64_t length,
      bool exclusive,
      bool wait_for_lock);

  virtual xtreemfs::pbrpc::Lock* CheckLock(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      int process_id,
      boost::uint64_t offset,
      boost::uint64_t length,
      bool exclusive);

  virtual void ReleaseLock(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const xtreemfs::pbrpc::Lock& lock);

  virtual void ReleaseLock(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      int process_id,
      boost::uint64_t offset,
      boost::uint64_t length,
      bool exclusive);

  virtual void PingReplica(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& osd_uuid);

  virtual void Close();

  /** Extracts the file_id from the stored xcap_. */
  boost::uint64_t GetFileId();

  /** Writes the path, which is stored in file_info_, into result. */
  void GetPath(std::string* result);

  /** Returns the StripingPolicy object for a given type (e.g. Raid0).
   *
   *  @remark Ownership is NOT transferred to the caller.
   */
  const StripeTranslator* GetStripeTranslator(
      xtreemfs::pbrpc::StripingPolicyType type);

  /** Used by FileInfo->CloseFileHandle() to free active locks. */
  void ReleaseLock(const xtreemfs::pbrpc::Lock& lock, bool update_cache);

  /** Sends pending file size updates synchronous (needed for flush/close).
   *
   * @throws AddressToUUIDNotFoundException
   * @throws IOException
   * @throws PosixErrorException
   * @throws UnknownAddressSchemeException
   */
  void WriteBackFileSize(const xtreemfs::pbrpc::OSDWriteResponse& owr,
                         bool close_file);

  /** Sends osd_write_response_for_async_write_back_ asynchronously. */
  void WriteBackFileSizeAsync();

  /** Overwrites the current osd_write_response_ with "owr". */
  void set_osd_write_response_for_async_write_back(
      const xtreemfs::pbrpc::OSDWriteResponse& owr);

  /** Renew xcap_ asynchronously. */
  void RenewXCapAsync();

  /** Blocks until the callback has completed (if an XCapRenewal is pending). */
  void WaitForPendingXCapRenewal();

 private:
  /** Implements callback for an async xtreemfs_update_file_size request. */
  void CallFinished(xtreemfs::pbrpc::timestampResponse* response_message,
                    char* data, boost::uint32_t data_length,
                    xtreemfs::pbrpc::RPCHeader::ErrorResponse* error,
                    void* context);

  /** Implements callback for an async xtreemfs_renew_capability request. */
  void CallFinished(xtreemfs::pbrpc::XCap* new_xcap,
                    char* data,
                    boost::uint32_t data_length,
                    xtreemfs::pbrpc::RPCHeader::ErrorResponse* error,
                    void* context);

  /** Actual implementation of ReleaseLock(). */
  void ReleaseLock(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const xtreemfs::pbrpc::Lock& lock,
      bool update_cache);

  /** Any modification to the object must obtain a lock first. */
  boost::mutex mutex_;

  /** UUID of the Client (needed to distinguish Locks of different clients). */
  const std::string& client_uuid_;

  /** UUID of the MRC. */
  std::string mrc_uuid_;

  /** Multiple FileHandle may refer to the same File and therefore unique file
   * properties (e.g. Path, FileId, XlocSet) are stored in a FileInfo object. */
  FileInfo* file_info_;

  // TODO(mberlin): Add flags member.

  /** Capabilitiy for the file, used to authorize against services */
  xtreemfs::pbrpc::XCap xcap_;

  /** True if there is an outstanding xcap_renew callback. */
  bool xcap_renewal_pending_;

  /** Used to wait for pending XCap renewal callbacks. */
  boost::condition xcap_renewal_pending_cond_;

  /** Contains a file size update which has to be written back (or NULL). */
  boost::scoped_ptr<xtreemfs::pbrpc::OSDWriteResponse>
      osd_write_response_for_async_write_back_;

  /** Used to resolve UUIDs. */
  UUIDResolver* uuid_resolver_;

  /** Pointer to object owned by VolumeImplemention */
  xtreemfs::pbrpc::MRCServiceClient* mrc_service_client_;

  /** Pointer to object owned by VolumeImplemention */
  xtreemfs::pbrpc::OSDServiceClient* osd_service_client_;

  const std::map<xtreemfs::pbrpc::StripingPolicyType,
           StripeTranslator*>& stripe_translators_;

  const Options& volume_options_;

  /** Auth needed for ServiceClients. Always set to AUTH_NONE by Volume. */
  const xtreemfs::pbrpc::Auth& auth_bogus_;

  /** For same reason needed as auth_bogus_. Always set to user "xtreemfs". */
  const xtreemfs::pbrpc::UserCredentials& user_credentials_bogus_;

  FRIEND_TEST(VolumeImplementationTestFastPeriodicFileSizeUpdate,
              WorkingPendingFileSizeUpdates);
  FRIEND_TEST(VolumeImplementationTest, FileSizeUpdateAfterFlush);
  FRIEND_TEST(VolumeImplementationTestFastPeriodicFileSizeUpdate,
              FileSizeUpdateAfterFlushWaitsForPendingUpdates);
  FRIEND_TEST(VolumeImplementationTestFastPeriodicXCapRenewal,
              WorkingXCapRenewal);
};

}  // namespace xtreemfs


#endif  // CPP_INCLUDE_LIBXTREEMFS_FILE_HANDLE_IMPLEMENTATION_H_
