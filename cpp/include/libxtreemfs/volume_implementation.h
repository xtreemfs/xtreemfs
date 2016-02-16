/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_INCLUDE_LIBXTREEMFS_VOLUME_IMPLEMENTATION_H_
#define CPP_INCLUDE_LIBXTREEMFS_VOLUME_IMPLEMENTATION_H_

#include "libxtreemfs/volume.h"

#include <stdint.h>

#include <boost/scoped_ptr.hpp>
#include <boost/thread/mutex.hpp>
#include <gtest/gtest_prod.h>
#include <list>
#include <map>
#include <string>

#include "libxtreemfs/execute_sync_request.h"
#include "libxtreemfs/metadata_cache.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/uuid_iterator.h"
#include "rpc/sync_callback.h"

namespace boost {
class thread;
}  // namespace boost

namespace xtreemfs {

namespace pbrpc {
class MRCServiceClient;
class OSDServiceClient;
}  // namespace pbrpc

namespace rpc {
class Client;
class SSLOptions;
}  // namespace rpc

class ClientImplementation;
class FileHandleImplementation;
class FileInfo;
class StripeTranslator;
class UUIDResolver;

/**
 * Default implementation of an XtreemFS volume.
 */
class VolumeImplementation : public Volume {
 public:
  /**
   * @remark Ownership of mrc_uuid_iterator is transferred to this object.
   */
  VolumeImplementation(
      ClientImplementation* client,
      const std::string& client_uuid,
      UUIDIterator* mrc_uuid_iterator,
      const std::string& volume_name,
      const xtreemfs::rpc::SSLOptions* ssl_options,
      const Options& options);
  virtual ~VolumeImplementation();

  virtual void Close();

  virtual xtreemfs::pbrpc::StatVFS* StatFS(
      const xtreemfs::pbrpc::UserCredentials& user_credentials);

  virtual void ReadLink(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      std::string* link_target_path);

  virtual void Symlink(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& target_path,
      const std::string& link_path);

  virtual void Link(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& target_path,
      const std::string& link_path);

  virtual void Access(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const xtreemfs::pbrpc::ACCESS_FLAGS flags);

  virtual FileHandle* OpenFile(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags);

  virtual FileHandle* OpenFile(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags,
      uint32_t mode);

  virtual FileHandle* OpenFile(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags,
      uint32_t mode,
      uint32_t attributes);

  /** Used by Volume->Truncate(). Otherwise truncate_new_file_size = 0. */
  FileHandle* OpenFileWithTruncateSize(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags,
      uint32_t mode,
      uint32_t attributes,
      int truncate_new_file_size);

  virtual void Truncate(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      off_t new_file_size);

  virtual void GetAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      xtreemfs::pbrpc::Stat* stat);

  virtual void GetAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      bool ignore_metadata_cache,
      xtreemfs::pbrpc::Stat* stat);

  /** If file_info is unknown and set to NULL, GetFileInfo(path) is used. */
  void GetAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      bool ignore_metadata_cache,
      xtreemfs::pbrpc::Stat* stat_buffer,
      FileInfo* file_info);

  virtual void SetAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const xtreemfs::pbrpc::Stat& stat,
      xtreemfs::pbrpc::Setattrs to_set);

  virtual void Unlink(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path);

  /** Issue an unlink at the head OSD of every replica given in fc.xlocs(). */
  void UnlinkAtOSD(
      const xtreemfs::pbrpc::FileCredentials& fc, const std::string& path);

  virtual void Rename(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const std::string& new_path);

  virtual void MakeDirectory(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      unsigned int mode);

  virtual void DeleteDirectory(
        const xtreemfs::pbrpc::UserCredentials& user_credentials,
        const std::string& path);

  virtual xtreemfs::pbrpc::DirectoryEntries* ReadDir(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      uint64_t offset,
      uint32_t count,
      bool names_only);

  virtual xtreemfs::pbrpc::listxattrResponse* ListXAttrs(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path);

  virtual xtreemfs::pbrpc::listxattrResponse* ListXAttrs(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      bool use_cache);

  virtual void SetXAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const std::string& name,
      const std::string& value,
      xtreemfs::pbrpc::XATTR_FLAGS flags);

  virtual bool GetXAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const std::string& name,
      std::string* value);

  virtual bool GetXAttrSize(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const std::string& name,
      int* size);

  virtual void RemoveXAttr(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const std::string& name);

  virtual void AddReplica(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const xtreemfs::pbrpc::Replica& new_replica);

  virtual xtreemfs::pbrpc::Replicas* ListReplicas(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path);

  void GetXLocSet(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& file_id,
      xtreemfs::pbrpc::XLocSet* xlocset);

  virtual void RemoveReplica(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      const std::string& osd_uuid);

  virtual void GetSuitableOSDs(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      int number_of_osds,
      std::list<std::string>* list_of_osd_uuids);

  virtual void SetReplicaUpdatePolicy(
        const xtreemfs::pbrpc::UserCredentials& user_credentials,
        const std::string& path,
        const std::string& policy);

  /** Starts the network client of the volume and its wrappers MRCServiceClient
   *  and OSDServiceClient. */
  void Start();

  /** Shuts down threads, called by ClientImplementation::Shutdown(). */
  void CloseInternal();

  /** Called by FileHandle.Close() to remove file_handle from the list. */
  void CloseFile(uint64_t file_id,
                 FileInfo* file_info,
                 FileHandleImplementation* file_handle);

  const std::string& client_uuid() {
    return client_uuid_;
  }

  /**
   * @remark    Ownership is NOT transferred to the caller.
   */
  UUIDIterator* mrc_uuid_iterator() {
    return mrc_uuid_iterator_.get();
  }

  /**
   * @remark    Ownership is NOT transferred to the caller.
   */
  UUIDResolver* uuid_resolver() {
    return uuid_resolver_;
  }

  /**
   * @remark    Ownership is NOT transferred to the caller.
   */
  xtreemfs::pbrpc::MRCServiceClient* mrc_service_client() {
    return mrc_service_client_.get();
  }

  /**
   * @remark    Ownership is NOT transferred to the caller.
   */
  xtreemfs::pbrpc::OSDServiceClient* osd_service_client() {
    return osd_service_client_.get();
  }

  const Options& volume_options() {
    return volume_options_;
  }

  const xtreemfs::pbrpc::Auth& auth_bogus() {
    return auth_bogus_;
  }

  const xtreemfs::pbrpc::UserCredentials& user_credentials_bogus() {
    return user_credentials_bogus_;
  }

  const std::map<xtreemfs::pbrpc::StripingPolicyType,
                 StripeTranslator*>& stripe_translators() {
    return stripe_translators_;
  }

 private:
  /** Retrieves the stat object for file at "path" from MRC or cache.
   *  Does not query any open file for pending file size updates nor lock the
   *  open_file_table_.
   *
   *  @remark   Ownership of stat_buffer is not transferred to the caller.
   */
  void GetAttrHelper(const xtreemfs::pbrpc::UserCredentials& user_credentials,
                     const std::string& path,
                     bool ignore_metadata_cache,
                     xtreemfs::pbrpc::Stat* stat_buffer);

  /** Obtain or create a new FileInfo object in the open_file_table_
   *
   * @remark Ownership is NOT transferred to the caller. The object will be
   *         deleted by DecreaseFileInfoReferenceCount() if no further
   *         FileHandle references it. */
  FileInfo* GetFileInfoOrCreateUnmutexed(
      uint64_t file_id,
      const std::string& path,
      bool replicate_on_close,
      const xtreemfs::pbrpc::XLocSet& xlocset);

  /** Deregisters file_id from open_file_table_. */
  void RemoveFileInfoUnmutexed(uint64_t file_id, FileInfo* file_info);

  /** Renew the XCap of every FileHandle before it does expire. */
  void PeriodicXCapRenewal();

  /** Write back file_sizes of every FileInfo object in open_file_table_. */
  void PeriodicFileSizeUpdate();

  void WaitForXLocSetInstallation(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& file_id,
      int expected_version,
      xtreemfs::pbrpc::XLocSet* xlocset);

  /** Reference to Client which did open this volume. */
  ClientImplementation* client_;

  /** UUID Resolver (usually points to the client_) */
  UUIDResolver* uuid_resolver_;

  /** UUID of the Client (needed to distinguish Locks of different clients). */
  const std::string& client_uuid_;

  /** UUID Iterator which contains the UUIDs of all MRC replicas of this
   *  volume. */
  boost::scoped_ptr<UUIDIterator> mrc_uuid_iterator_;

  /** Name of the corresponding Volume. */
  const std::string volume_name_;

  /** SSL options used for connections to the MRC and OSDs. */
  const xtreemfs::rpc::SSLOptions* volume_ssl_options_;

  /** libxtreemfs Options object which includes all program options */
  const Options& volume_options_;

  /** Disabled retry and interrupt functionality. */
  RPCOptions periodic_threads_options_;

  /** The PBRPC protocol requires an Auth & UserCredentials object in every
   *  request. However there are many operations which do not check the content
   *  of this operation and therefore we use bogus objects then.
   *  auth_bogus_ will always be set to the type AUTH_NONE.
   *
   *  @remark Cannot be set to const because it's modified inside the
   *          constructor VolumeImplementation(). */
  xtreemfs::pbrpc::Auth auth_bogus_;

  /** The PBRPC protocol requires an Auth & UserCredentials object in every
   *  request. However there are many operations which do not check the content
   *  of this operation and therefore we use bogus objects then.
   *  user_credentials_bogus will only contain a user "xtreemfs".
   *
   *  @remark Cannot be set to const because it's modified inside the
   *          constructor VolumeImplementation(). */
  xtreemfs::pbrpc::UserCredentials user_credentials_bogus_;

  /** The RPC Client processes requests from a queue and executes callbacks in
   *  its thread. */
  boost::scoped_ptr<xtreemfs::rpc::Client> network_client_;
  boost::scoped_ptr<boost::thread> network_client_thread_;

  /** An MRCServiceClient is a wrapper for an RPC Client. */
  boost::scoped_ptr<xtreemfs::pbrpc::MRCServiceClient> mrc_service_client_;

  /** A OSDServiceClient is a wrapper for an RPC Client. */
  boost::scoped_ptr<xtreemfs::pbrpc::OSDServiceClient> osd_service_client_;

  /** Maps file_id -> FileInfo* for every open file. */
  std::map<uint64_t, FileInfo*> open_file_table_;
  /**
   * @attention If a function uses open_file_table_mutex_ and
   *            file_handle_list_mutex_, file_handle_list_mutex_ has to be
   *            locked first to avoid a deadlock.
   */
  boost::mutex open_file_table_mutex_;

  /** Metadata cache (stat, dir_entries, xattrs) by path. */
  MetadataCache metadata_cache_;

  /** Available Striping policies. */
  std::map<xtreemfs::pbrpc::StripingPolicyType,
           StripeTranslator*> stripe_translators_;

  /** Periodically renews the XCap of every FileHandle before it expires. */
  boost::scoped_ptr<boost::thread> xcap_renewal_thread_;

  /** Periodically writes back pending file sizes updates to the MRC service. */
  boost::scoped_ptr<boost::thread> filesize_writeback_thread_;

  FRIEND_TEST(VolumeImplementationTest,
              StatCacheCorrectlyUpdatedAfterRenameWriteAndClose);
};

}  // namespace xtreemfs

#endif  // CPP_INCLUDE_LIBXTREEMFS_VOLUME_IMPLEMENTATION_H_
