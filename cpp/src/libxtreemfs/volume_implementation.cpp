/*
 * Copyright (c) 2011-2012 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/volume_implementation.h"

#include <algorithm>
#include <boost/bind.hpp>
#include <boost/thread/thread.hpp>
#include <google/protobuf/stubs/common.h>
#include <limits>
#include <map>
#include <string>

#include "libxtreemfs/client_implementation.h"
#include "libxtreemfs/execute_sync_request.h"
#include "libxtreemfs/file_handle_implementation.h"
#include "libxtreemfs/file_info.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/object_encryptor.h"
#include "libxtreemfs/stripe_translator.h"
#include "libxtreemfs/uuid_iterator.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "rpc/client.h"
#include "util/error_log.h"
#include "util/logging.h"
#include "xtreemfs/MRC.pb.h"
#include "xtreemfs/MRCServiceClient.h"
#include "xtreemfs/OSDServiceClient.h"

using namespace std;
using namespace xtreemfs::util;
using namespace xtreemfs::pbrpc;

// Fix ambiguous map error on Solaris (see
// http://groups.google.com/group/xtreemfs/msg/b44605dbbd7b6d0f)
using std::map;

namespace xtreemfs {

VolumeImplementation::VolumeImplementation(
    ClientImplementation* client,
    const std::string& client_uuid,
    UUIDIterator* mrc_uuid_iterator,
    const std::string& volume_name,
    const xtreemfs::rpc::SSLOptions* ssl_options,
    const Options& options)
    : client_(client),
      uuid_resolver_(client->GetUUIDResolver()),
      client_uuid_(client_uuid),
      volume_name_(volume_name),
      volume_ssl_options_(ssl_options),
      volume_options_(options),
      // Disable retries and interrupted querying for periodic threads.
      periodic_threads_options_(1, 40, false, NULL),
      metadata_cache_(options.metadata_cache_size,
                      options.metadata_cache_ttl_s),
      file_key_distribution_(this) {
  // Set AuthType to AUTH_NONE as it's currently not used.
  auth_bogus_.set_auth_type(AUTH_NONE);
  // Set username "xtreemfs" as it does not get checked at server side.
  user_credentials_bogus_.set_username("xtreemfs");

  mrc_uuid_iterator_.reset(mrc_uuid_iterator);
}

VolumeImplementation::~VolumeImplementation() {
  // Warn the user about open files.
  if (open_file_table_.size() != 0) {
    string error = "Volume::~Volume(): The volume object will be deleted while"
        " there are open FileHandles left. This will result in memory leaks.";
    Logging::log->getLog(LEVEL_ERROR) << error << endl;
    ErrorLog::error_log->AppendError(error);
  }

  // Remove StripingPolicy objects.
  for (map<StripingPolicyType, StripeTranslator*>::iterator it
       = stripe_translators_.begin(); it != stripe_translators_.end(); ++it) {
    delete it->second;
  }
}

void VolumeImplementation::Start() {
  // start network (rpc) client
  network_client_.reset(new xtreemfs::rpc::Client(
      volume_options_.connect_timeout_s,  // Connect timeout.
      volume_options_.request_timeout_s,  // Request timeout.
      volume_options_.linger_timeout_s,  // Linger timeout.
      volume_ssl_options_));

  // Create thread which runs the network client.
  network_client_thread_.reset(
      new boost::thread(boost::bind(&xtreemfs::rpc::Client::run,
                                    network_client_.get())));
  // Create MRC and OSDServiceClient wrapper.
  mrc_service_client_.reset(new MRCServiceClient(network_client_.get()));
  osd_service_client_.reset(new OSDServiceClient(network_client_.get()));

  // Register StripingPolicies.
  stripe_translators_[STRIPING_POLICY_RAID0] = new StripeTranslatorRaid0();

  // Start periodic threads.
  xcap_renewal_thread_.reset(new boost::thread(boost::bind(
      &xtreemfs::VolumeImplementation::PeriodicXCapRenewal,
      this)));
  filesize_writeback_thread_.reset(new boost::thread(boost::bind(
      &xtreemfs::VolumeImplementation::PeriodicFileSizeUpdate,
      this)));
}

/**
 * @throws OpenFileHandlesLeftException
 */
void VolumeImplementation::CloseInternal() {
  // Stop periodic threads.
  filesize_writeback_thread_->interrupt();
  xcap_renewal_thread_->interrupt();
  filesize_writeback_thread_->join();
  xcap_renewal_thread_->join();

  boost::mutex::scoped_lock lock_oft(open_file_table_mutex_);

  // There must not be any FileInfo object left.
  if (open_file_table_.size() != 0) {
    string error = "Volume::Close(): THERE ARE OPEN FILE HANDLES LEFT. MAKE IN"
        " YOUR APPLICATION SURE THAT ALL FILE HANDLES ARE CLOSED BEFORE CLOSING"
        " THE VOLUME!";
    Logging::log->getLog(LEVEL_ERROR) << error << endl;
    ErrorLog::error_log->AppendError(error);
  }

  // Shutdown network client.
  network_client_->shutdown();
  network_client_thread_->join();
}

void VolumeImplementation::Close() {
  CloseInternal();

  // Let client_ unregister and delete the Volume object.
  client_->CloseVolume(this);
}

StatVFS* VolumeImplementation::StatFS(
    const xtreemfs::pbrpc::UserCredentials& user_credentials) {
  statvfsRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_known_etag(0);
  boost::scoped_ptr<rpc::SyncCallbackBase > response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::statvfs_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));

  // Delete everything except the response.
  delete[] response->data();
  delete response->error();
  return static_cast<StatVFS*>(response->response());
}

void VolumeImplementation::ReadLink(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      std::string* link_target_path) {
  readlinkRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::readlink_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));

  readlinkResponse* readlink_response =
      static_cast<readlinkResponse*>(response->response());

  // The XtreemFS MRC always returns one resolved target or throws an EINVAL.
  assert(readlink_response->link_target_path_size() == 1);
  *link_target_path = readlink_response->link_target_path(0);
  response->DeleteBuffers();
}

void VolumeImplementation::Symlink(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& target_path,
    const std::string& link_path) {
  symlinkRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_target_path(target_path);
  rq.set_link_path(link_path);
  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::symlink_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));
  timestampResponse* ts_response = static_cast<timestampResponse*>(
      response->response());

  const string parent_dir = ResolveParentDirectory(link_path);
  metadata_cache_.UpdateStatTime(
      parent_dir,
      ts_response->timestamp_s(),
      static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));
  // TODO(mberlin): Retrieve stat as optional member of the response instead
  //                and update cached DirectoryEntries accordingly.
  metadata_cache_.InvalidateDirEntries(parent_dir);

  response->DeleteBuffers();
}

void VolumeImplementation::Link(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& target_path,
    const std::string& link_path) {
  linkRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_target_path(target_path);
  rq.set_link_path(link_path);
  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::link_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));
  timestampResponse* ts_response = static_cast<timestampResponse*>(
      response->response());

  const string parent_dir = ResolveParentDirectory(link_path);
  metadata_cache_.UpdateStatTime(
      parent_dir,
      ts_response->timestamp_s(),
      static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));
  // TODO(mberlin): Retrieve stat as optional member of the response instead
  //                and update cached DirectoryEntries accordingly.
  metadata_cache_.InvalidateDirEntries(parent_dir);

  // Invalidate caches as we don't cache links and their targets.
  metadata_cache_.Invalidate(link_path);
  metadata_cache_.Invalidate(target_path);

  response->DeleteBuffers();
}

void VolumeImplementation::Access(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const xtreemfs::pbrpc::ACCESS_FLAGS flags) {
  accessRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  rq.set_flags(flags);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::access_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));

  response->DeleteBuffers();
}

FileHandle* VolumeImplementation::OpenFile(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags) {
  return OpenFileWithTruncateSize(user_credentials, path, flags, 0, 0, 0);
}

FileHandle* VolumeImplementation::OpenFile(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags,
    uint32_t mode) {
  return OpenFileWithTruncateSize(user_credentials, path, flags, mode, 0, 0);
}

FileHandle* VolumeImplementation::OpenFile(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags,
    uint32_t mode,
    uint32_t attributes) {
  return OpenFileWithTruncateSize(user_credentials, path, flags, mode, attributes, 0);
}

/**
 * @throws IOException
 */
FileHandle* VolumeImplementation::OpenFileWithTruncateSize(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags,
    uint32_t mode,
    uint32_t attributes,
    int truncate_new_file_size) {
  bool async_writes_enabled = volume_options_.enable_async_writes;

  if (flags & SYSTEM_V_FCNTL_H_O_SYNC) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "open called with O_SYNC, async writes were disabled." << endl;
    }
    async_writes_enabled = false;
  }

  openRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  rq.set_flags(flags);
  rq.set_mode(mode);
  rq.set_attributes(attributes);

  // set vivaldi coordinates if vivaldi is enabled
  if (volume_options_.vivaldi_enable) {
    rq.mutable_coordinates()->CopyFrom(this->client_->GetVivaldiCoordinates());
  }

  // encrypted files need read access for write
  if (volume_options_.encryption && (flags & SYSTEM_V_FCNTL_H_O_WRONLY)
      && !ObjectEncryptor::IsEncMetaFile(path)) {
    rq.set_flags(
        (flags & ~SYSTEM_V_FCNTL_H_O_WRONLY) | SYSTEM_V_FCNTL_H_O_RDWR);
  }

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::open_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));

  openResponse* open_response = static_cast<openResponse*>(
      response->response());
  // We must have obtained file credentials.
  assert(open_response->has_creds());

  if (open_response->creds().xlocs().replicas_size() == 0) {
    string error = "MRC assigned no OSDs to file on open: " + path +
        ", xloc: " + open_response->creds().xlocs().DebugString();
    Logging::log->getLog(LEVEL_ERROR) << error << endl;
    ErrorLog::error_log->AppendError(error);
    throw PosixErrorException(POSIX_ERROR_EIO, error);
  }

  FileHandleImplementation* file_handle = NULL;
  FileInfo* file_info = NULL;
  // Create a FileInfo object if it does not exist yet.
  {
    boost::mutex::scoped_lock lock(open_file_table_mutex_);

    file_info = GetFileInfoOrCreateUnmutexed(
        ExtractFileIdFromXCap(open_response->creds().xcap()),
        path,
        open_response->creds().xcap().replicate_on_close(),
        open_response->creds().xlocs());
    file_handle = file_info->CreateFileHandle(open_response->creds().xcap(),
                                              async_writes_enabled);
  }

  if (volume_options_.encryption && !ObjectEncryptor::IsEncMetaFile(path)) {
    pbrpc::FileLockbox lockbox;
    FileHandle* meta_file = file_key_distribution_.OpenMetaFile(
        user_credentials, open_response->creds().xcap(), path, mode, &lockbox);
    file_handle->SetObjectEncryptor(
        std::auto_ptr<ObjectEncryptor>(
            new ObjectEncryptor(
                user_credentials,
                lockbox,
                meta_file,
                this,
                file_info,
                open_response->creds().xlocs().replicas(0).striping_policy()
                    .stripe_size())));
  }

  // Copy timestamp and free response memory.
  uint64_t timestamp_s = open_response->timestamp_s();
  response->DeleteBuffers();

  // If O_CREAT is set and the file did not previously exist, upon successful
  // completion, open() shall mark for update the st_atime, st_ctime, and
  // st_mtime fields of the file and the st_ctime and st_mtime fields of
  // the parent directory.
  if ((flags & SYSTEM_V_FCNTL_H_O_CREAT)) {
    const string parent_dir = ResolveParentDirectory(path);
    metadata_cache_.UpdateStatTime(
        parent_dir,
        timestamp_s,
        static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));
    // TODO(mberlin): Retrieve stat as optional member of openResponse instead
    //                and update cached DirectoryEntries accordingly.
    metadata_cache_.InvalidateDirEntries(parent_dir);
  }

  // If O_TRUNC was set, go on processing the truncate request.
  if ((flags & SYSTEM_V_FCNTL_H_O_TRUNC)) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "open called with O_TRUNC." << endl;
    }

    // Update mtime and ctime of the file if O_TRUNC was set.
    metadata_cache_.UpdateStatTime(
        path,
        timestamp_s,
        static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));

    try {
      file_handle->TruncatePhaseTwoAndThree(user_credentials,
                                            truncate_new_file_size);
    } catch(const XtreemFSException&) {
      // Truncate did fail, close file again.
      file_handle->Close();
      throw;  // Rethrow error.
    }
  }

  return file_handle;
}

void VolumeImplementation::Truncate(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    off_t new_file_size) {
  // Open file with O_TRUNC.
  const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags = static_cast<SYSTEM_V_FCNTL>(
      SYSTEM_V_FCNTL_H_O_TRUNC | SYSTEM_V_FCNTL_H_O_WRONLY);
  FileHandle* file_handle = OpenFileWithTruncateSize(user_credentials,
                                                     path,
                                                     flags,
                                                     0,
                                                     0,
                                                     new_file_size);

  // Close file.
  file_handle->Close();
}

/**
 * @throws FileInfoNotFoundException
 * @throws FileHandleNotFoundException
 */
void VolumeImplementation::CloseFile(
    uint64_t file_id,
    FileInfo* file_info,
    FileHandleImplementation* file_handle) {
  // Put file_handle into a scoped_ptr as it definitely has to be deleted.
  boost::scoped_ptr<FileHandleImplementation> file_handle_ptr(file_handle);

  // Remove file_info if it has no more open file handles.
  boost::mutex::scoped_lock lock(open_file_table_mutex_);
  if (file_info->DecreaseReferenceCount() == 0) {
    RemoveFileInfoUnmutexed(file_id, file_info);
    // file_info is no longer visible: it's safe to unlock the open_file_table_.
    lock.unlock();

    // The last file handle of this file was closed: Release all locks.
    // All locks for the process of this file handle have to be released.
    try {
      file_info->ReleaseAllLocks(file_handle_ptr.get());
    } catch(const XtreemFSException&) {
      // Ignore errors.
    }

    file_info->WaitForPendingFileSizeUpdates();

    // Write back the OSDWriteResponse to the stat cache.
    OSDWriteResponse response;
    string path;
    file_info->GetOSDWriteResponse(&response);
    file_info->GetPath(&path);
    metadata_cache_.UpdateStatFromOSDWriteResponse(path, response);

    delete file_info;
  }
}

void VolumeImplementation::GetAttr(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    xtreemfs::pbrpc::Stat* stat_buffer) {
  GetAttr(user_credentials, path, false, stat_buffer, NULL);
}

void VolumeImplementation::GetAttr(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    bool ignore_metadata_cache,
    xtreemfs::pbrpc::Stat* stat_buffer) {
  GetAttr(user_credentials, path, ignore_metadata_cache, stat_buffer, NULL);
}

void VolumeImplementation::GetAttrHelper(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    bool ignore_metadata_cache,
    xtreemfs::pbrpc::Stat* stat_buffer) {
  if (!ignore_metadata_cache) {
    // Check if the information was cached.
    MetadataCache::GetStatResult stat_cached =
        metadata_cache_.GetStat(path, stat_buffer);

    if (stat_cached == MetadataCache::kStatCached) {
      // Found in StatCache.
      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
            << "getattr: serving from stat-cache " << path
            << " " << stat_buffer->size() << endl;
      }
      return;
    } else if (stat_cached == MetadataCache::kPathDoesntExist) {
      throw PosixErrorException(
          POSIX_ERROR_ENOENT,
          "Path was not found in the cached parent directory. Path: " + path);
    }
  }

  // Not found in StatCache, retrieve from MRC.
  getattrRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  rq.set_known_etag(0);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::getattr_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));
  getattrResponse* getattr = static_cast<getattrResponse*>(
      response->response());

  stat_buffer->CopyFrom(getattr->stbuf());
  if (stat_buffer->nlink() > 1) {  // Do not cache hard links.
    metadata_cache_.Invalidate(path);
  } else {
    metadata_cache_.UpdateStat(path, *stat_buffer);
  }

  response->DeleteBuffers();
}

void VolumeImplementation::GetAttr(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    bool ignore_metadata_cache,
    xtreemfs::pbrpc::Stat* stat_buffer,
    FileInfo* file_info) {
  // Retrieve stat object from cache or MRC.
  GetAttrHelper(user_credentials, path, ignore_metadata_cache, stat_buffer);

  // Wait until async writes have finished and merge StatCache object with
  // possibly newer information from FileInfo.
  if (file_info == NULL) {
    // Unknown if this file at "path" is open - look it up by its file_id.
    boost::mutex::scoped_lock oft_lock(open_file_table_mutex_);

    map<uint64_t, FileInfo*>::const_iterator it
        = open_file_table_.find(stat_buffer->ino());  // ino = file_id.
    if (it != open_file_table_.end()) {
      // File at "path" is opened.

      // Wait for pending asynchronous writes which haven't finished yet and
      // whose new file size is not considered yet by the stat object.

      // To avoid longer locking periods of the open file table, we will
      // register an observer at the file and get notified later.
      bool wait_completed = false;
      boost::mutex wait_completed_mutex;
      boost::mutex::scoped_lock wait_completed_lock(wait_completed_mutex);
      boost::condition wait_completed_condition;
      if (it->second->WaitForPendingAsyncWritesNonBlocking(
              &wait_completed_condition,
              &wait_completed,
              &wait_completed_mutex)) {
        // Wait would have blocked and did register our observer.

        oft_lock.unlock();

        while (!wait_completed) {
          wait_completed_condition.wait(wait_completed_lock);
        }

        oft_lock.lock();

        // As wait did unlock the open file table, the previously
        // found FileInfo object may be removed and deleted meanwhile, i.e.
        // search again for it.
        map<uint64_t, FileInfo*>::const_iterator it2
            = open_file_table_.find(stat_buffer->ino());  // ino = file_id.
        if (it2 != open_file_table_.end()) {
          it2->second->MergeStatAndOSDWriteResponse(stat_buffer);
        } else {
          // We dont find the previous FileInfo object anymore. This means we
          // have to retrieve the file size once again from the MRC or stat cache.
          // Return lock on open_file_table_.
          oft_lock.unlock();
          GetAttrHelper(user_credentials,
                        path,
                        ignore_metadata_cache,
                        stat_buffer);
        }
      } else {
        // Open file table was never unlocked and it's still safe to access the
        // file info object.
        it->second->MergeStatAndOSDWriteResponse(stat_buffer);
      }
    }
  } else {
    file_info->WaitForPendingAsyncWrites();
    file_info->MergeStatAndOSDWriteResponse(stat_buffer);
  }
}

void VolumeImplementation::SetAttr(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const xtreemfs::pbrpc::Stat& stat,
    xtreemfs::pbrpc::Setattrs to_set) {
  // Based on possibly cached stat, find out which attributes actually have
  // to be updated.
  Setattrs actual_to_set = metadata_cache_.SimulateSetStatAttributes(path,
                                                                     stat,
                                                                     to_set);
  if (actual_to_set == 0) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG) << "Skipped setting attributes since"
          " the to be changed attributes are identical to the cached ones."
          "Path: " << path << endl;
    }
    return;
  }
  if (!volume_options_.enable_atime && actual_to_set == SETATTR_ATIME) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG) << "Skipped setting attributes since"
          " the only changed attribute would have been atime and atime updates"
          " are currently ignored. Path: " << path << endl;
    }
    return;
  }

  if (volume_options_.encryption && !ObjectEncryptor::IsEncMetaFile(path)) {
    file_key_distribution_.ChangeAccessRights(user_credentials, path, stat,
                                              to_set);
  }

  setattrRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  rq.mutable_stbuf()->CopyFrom(stat);
  rq.set_to_set(to_set);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::setattr_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));
  timestampResponse* ts_response = static_cast<timestampResponse*>(
      response->response());

  // "chmod" or "chown" operations result into updating the ctime attribute.
  if ((to_set &  SETATTR_MODE) || (to_set &  SETATTR_UID) ||
      (to_set &  SETATTR_GID)) {
    to_set = static_cast<Setattrs>(to_set | SETATTR_CTIME);
    rq.mutable_stbuf()->set_ctime_ns(static_cast<uint64_t>(
        ts_response->timestamp_s()) * 1000000000);
  }

  // Do not cache hard links or chmod operations which try to set the SGID bit
  // as it might get cleared by the MRC.
  if (rq.stbuf().nlink() > 1 ||
      ((to_set & SETATTR_MODE) && (rq.stbuf().mode() & (1 << 10)))) {
    metadata_cache_.Invalidate(path);
  } else {
    metadata_cache_.UpdateStatAttributes(path, rq.stbuf(), to_set);
  }
  response->DeleteBuffers();
}

void VolumeImplementation::Unlink(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path) {
  // 1. Delete file at MRC.
  unlinkRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::unlink_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));
  unlinkResponse* unlink_response = static_cast<unlinkResponse*>(
      response->response());

  // 2. Invalidate metadata caches.
  metadata_cache_.Invalidate(path);
  const string parent_dir = ResolveParentDirectory(path);
  metadata_cache_.UpdateStatTime(
      parent_dir,
      unlink_response->timestamp_s(),
      static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));
  metadata_cache_.InvalidateDirEntry(parent_dir, GetBasename(path));

  // 3. Delete objects of all replicas on the OSDs.
  if (unlink_response->has_creds()) {
    UnlinkAtOSD(unlink_response->creds(), path);
    if (volume_options_.encryption && !ObjectEncryptor::IsEncMetaFile(path)) {
      ObjectEncryptor::Unlink(
          user_credentials, this,
          ExtractFileIdFromXCap(unlink_response->creds().xcap()));
    }
  }

  response->DeleteBuffers();
}

void VolumeImplementation::UnlinkAtOSD(const FileCredentials& fc,
                                       const std::string& path) {
  const XLocSet& xlocs = fc.xlocs();

  unlink_osd_Request rq_osd;
  rq_osd.mutable_file_credentials()->CopyFrom(fc);
  rq_osd.set_file_id(fc.xcap().file_id());


  // Remove _all_ replicas.
  for (int k = 0; k < xlocs.replicas_size(); k++) {
    SimpleUUIDIterator osd_uuid_iterator;
    osd_uuid_iterator.AddUUID(GetOSDUUIDFromXlocSet(xlocs, k, 0));

    boost::scoped_ptr<rpc::SyncCallbackBase> response(
        ExecuteSyncRequest(
            boost::bind(&xtreemfs::pbrpc::OSDServiceClient::unlink_sync,
                osd_service_client_.get(),
                _1,
                boost::cref(auth_bogus_),
                boost::cref(user_credentials_bogus_),
                &rq_osd),
            &osd_uuid_iterator,
            uuid_resolver_,
            RPCOptionsFromOptions(volume_options_)));
    response->DeleteBuffers();
  }
}

void VolumeImplementation::Rename(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const std::string& new_path) {
  if (path == new_path) {
    return;  // Do nothing.
  }
  // 1. Issue rename at MRC.
  renameRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_source_path(path);
  rq.set_target_path(new_path);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::rename_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));
  renameResponse* rename_response = static_cast<renameResponse*>(
      response->response());

  // 2. Remove file content of any previous files at "new_path".
  if (rename_response->has_creds()) {
    UnlinkAtOSD(rename_response->creds(), new_path);
    if (volume_options_.encryption && !ObjectEncryptor::IsEncMetaFile(path)) {
      ObjectEncryptor::Unlink(
          user_credentials, this,
          ExtractFileIdFromXCap(rename_response->creds().xcap()));
    }
  }

  // 3. Update caches
  // Update the timestamps of parents of both directories.
  const std::string parent_path = ResolveParentDirectory(path);
  const std::string parent_new_path = ResolveParentDirectory(new_path);
  if (rename_response->timestamp_s() != 0) {
    metadata_cache_.UpdateStatTime(
        parent_path,
        rename_response->timestamp_s(),
        static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));
    metadata_cache_.UpdateStatTime(
        parent_new_path,
        rename_response->timestamp_s(),
        static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));
  }
  metadata_cache_.InvalidateDirEntry(parent_path, GetBasename(path));
  // TODO(mberlin): Add DirEntry instead to parent_new_path if stat available.
  metadata_cache_.InvalidateDirEntries(parent_new_path);
  // Overwrite an existing entry; no Prefix() operation needed because:
  //    "If new names an existing directory, it shall be required to be an empty
  //     directory."
  //    see http://pubs.opengroup.org/onlinepubs/009695399/functions/rename.html
  metadata_cache_.Invalidate(new_path);
  // Rename all affected entries.
  metadata_cache_.RenamePrefix(path, new_path);
  // http://pubs.opengroup.org/onlinepubs/009695399/functions/rename.html:
  // "Some implementations mark for update the st_ctime field of renamed files
  //  and some do not."
  // => XtreemFS does so, i.e. update the client's cache, too.
  metadata_cache_.UpdateStatTime(new_path,
                                 rename_response->timestamp_s(),
                                 static_cast<Setattrs>(SETATTR_CTIME));

  // Rename path in all open FileInfo objects.
  {
    boost::mutex::scoped_lock lock(open_file_table_mutex_);
    map<uint64_t, FileInfo*>::iterator it;
    for (it = open_file_table_.begin();
         it != open_file_table_.end(); ++it) {
      it->second->RenamePath(path, new_path);
    }
  }

  response->DeleteBuffers();
}

void VolumeImplementation::MakeDirectory(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    unsigned int mode) {
  mkdirRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  rq.set_mode(mode);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::mkdir_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));
  timestampResponse* ts_response = static_cast<timestampResponse*>(
      response->response());

  const string parent_dir = ResolveParentDirectory(path);
  metadata_cache_.UpdateStatTime(
      parent_dir,
      ts_response->timestamp_s(),
      static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));
  // TODO(mberlin): Retrieve stat as optional member of openResponse instead
  //                and update cached DirectoryEntries accordingly.
  metadata_cache_.InvalidateDirEntries(parent_dir);

  response->DeleteBuffers();
}

void VolumeImplementation::DeleteDirectory(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path) {
  rmdirRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::rmdir_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));
  timestampResponse* ts_response = static_cast<timestampResponse*>(
      response->response());

  const string parent_dir = ResolveParentDirectory(path);
  metadata_cache_.UpdateStatTime(
      parent_dir,
      ts_response->timestamp_s(),
      static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));
  metadata_cache_.InvalidatePrefix(path);
  metadata_cache_.InvalidateDirEntry(parent_dir, GetBasename(path));

  response->DeleteBuffers();
}

/**
 * Larger readdir requests are split up into chunks of size "volume_options_.
 * readdir_chunk_size". Avoid to exceed this limit when specifying "count" -
 * otherwise the chunks have to be copied to get merged into one Directory-
 * Entries object.
 *
 * @attention If you don't read the whole directory with one request and use an
 *            offset != 0 to resume a readdir operation, the content may change
 *            between two readdir requests as the MRC does not keep any state of
 *            the previous operation.
 *            Possible side effects are:
 *            - Entries may show up twice (if a newly created file did "push" an
 *              already seen file at least till "offset" in the directory).
 *            - Entries may not be visible at all (if a seen file was deleted
 *              and a file at "offset" or higher does move before "offset").
 *
 *            If required, the caller of this function has to make sure on its
 *            own he's not delivering duplicate entries. Keep in mind duplicates
 *            have to be checked by their file id AND their name. (For instance,
 *            a renamed file will not be filtered by a duplicate check based on
 *            the name. Checks based on the file id won't recognize files which
 *            have been deleted and created with the same name (resulting in a
 *            new file id).)
 */
xtreemfs::pbrpc::DirectoryEntries* VolumeImplementation::ReadDir(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    uint64_t offset,
    uint32_t count,
    bool names_only) {
  DirectoryEntries* result = NULL;

  if (count == 0) {
    count = numeric_limits<uint32_t>::max();
  }

  result = metadata_cache_.GetDirEntries(path, offset, count);
  if (result != NULL) {
    return result;
  }

  // Process large requests in multiples of readdir_chunk_size.
  readdirRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_known_etag(0);
  rq.set_path(path);
  rq.set_names_only(names_only);
  for (uint64_t current_offset = offset;
       current_offset < offset + count;
       current_offset += volume_options_.readdir_chunk_size) {
    rq.set_seen_directory_entries_count(current_offset);
    // Read complete chunk or only remaining rest.
    rq.set_limit_directory_entries_count(static_cast<uint32_t>(
        current_offset > offset + count ?
        current_offset - offset - count : volume_options_.readdir_chunk_size));

    boost::scoped_ptr<rpc::SyncCallbackBase> response(
        ExecuteSyncRequest(
            boost::bind(
                &xtreemfs::pbrpc::MRCServiceClient::readdir_sync,
                mrc_service_client_.get(),
                _1,
                boost::cref(auth_bogus_),
                boost::cref(user_credentials),
                &rq),
            mrc_uuid_iterator_.get(),
            uuid_resolver_,
            RPCOptionsFromOptions(volume_options_)));
    DirectoryEntries* dentries = static_cast<DirectoryEntries*>(
        response->response());

    // Process request and free memory.
    if (current_offset == offset) {
      // First chunk
      result = dentries;

      // Delete everything except the response.
      delete[] response->data();
      delete response->error();
    } else {
      // Further chunks. Merge them into first chunk.
      for (int i = 0; i < dentries->entries_size(); i++) {
        result->add_entries()->CopyFrom(dentries->entries(i));
      }
      response->DeleteBuffers();
    }

    // Break if this is the last chunk.
    if (result->entries_size() <
        (current_offset + volume_options_.readdir_chunk_size)) {
      break;
    }
  }

  // TODO(mberlin): Merge possible pending file size updates of files into
  //                the stat entries of listed files.

  // Cache the first stat buffers that fit into the cache.
  for (int i = 0;
       i < min(volume_options_.metadata_cache_size,
               static_cast<uint64_t>(result->entries_size()));
       i++) {
    const DirectoryEntry& dentry = result->entries(i);
    if (dentry.has_stbuf()) {
      if (dentry.name() == ".") {
        metadata_cache_.UpdateStat(path, dentry.stbuf());
      } else if (dentry.name() == ".." && path != "/") {
        string parent_dir = ResolveParentDirectory(path);
        metadata_cache_.UpdateStat(parent_dir, dentry.stbuf());
      } else if (dentry.stbuf().nlink() > 1) {  // Do not cache hard links.
        metadata_cache_.Invalidate(path);
      } else {
        metadata_cache_.UpdateStat(
            ConcatenatePath(path, dentry.name()),
            dentry.stbuf());
      }
    }
  }

  // Cache the result if it's the complete directory.
  // We can't tell for sure whether result contains all directory entries if
  // it's size is not less than the requested "count".
  // TODO(mberlin): Cache only names and no stat entries and remove names_only
  //                condition.
  // TODO(mberlin): Set an upper bound of dentries, otherwise don't cache it.
  if (offset == 0 &&
      static_cast<uint32_t>(result->entries_size()) < count &&
      !names_only) {
    metadata_cache_.UpdateDirEntries(path, *result);
  }

  return result;
}

/**
 * @warning This implementation does return cached values for "xtreemfs.*"
 *          attributes. Use ListXAttrs(user_credentials, path, false) to make
 *          sure that no entries are retrieved from the cache.
 *
 *          Alternatively, direct operations on "xtreemfs." attributes (like
 *          GetXAttr()) always retrieve the latest value from the MRC.
 */
xtreemfs::pbrpc::listxattrResponse* VolumeImplementation::ListXAttrs(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path) {
  return ListXAttrs(user_credentials, path, true);
}

xtreemfs::pbrpc::listxattrResponse* VolumeImplementation::ListXAttrs(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    bool use_cache) {
  xtreemfs::pbrpc::listxattrResponse* result;

  // Check if the information was cached.
  if (use_cache) {
    result = metadata_cache_.GetXAttrs(path);
    if (result != NULL) {
      return result;
    }
  }

  listxattrRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  rq.set_names_only(false);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::listxattr_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));

  result = static_cast<listxattrResponse*>(response->response());
  // Delete everything except the response.
  delete[] response->data();
  delete response->error();

  // Cache the result.
  metadata_cache_.UpdateXAttrs(path, *result);

  return result;
}

void VolumeImplementation::SetXAttr(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const std::string& name,
    const std::string& value,
    xtreemfs::pbrpc::XATTR_FLAGS flags) {
  setxattrRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  rq.set_name(name);
  // For unknown reasons this fails if c_str() is not used, for instance if the
  // value is set to the character '\002'.
  if (google::protobuf::internal::IsStructurallyValidUTF8(value.c_str(),
                                                          value.size())) {
    // only set value string if it is valid UTF8
    rq.set_value(value.c_str());
  } else {
    rq.set_value("");
  }
  rq.set_value_bytes_string(value.c_str(), value.size());
  rq.set_flags(flags);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::setxattr_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));
  response->DeleteBuffers();

  metadata_cache_.UpdateXAttr(path, name, value);
}

bool VolumeImplementation::GetXAttr(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const std::string& name,
    std::string* value) {
  // Try to get it from cache first.
  bool xattrs_cached;
  bool xtreemfs_attribute_requested = (name.substr(0, 9) == "xtreemfs.");

  if (xtreemfs_attribute_requested) {
    // Retrieve only the value of the requested attribute, not the whole list.
    getxattrRequest rq;
    rq.set_volume_name(volume_name_);
    rq.set_path(path);
    rq.set_name(name);

    boost::scoped_ptr<rpc::SyncCallbackBase> response(
        ExecuteSyncRequest(
            boost::bind(
                &xtreemfs::pbrpc::MRCServiceClient::getxattr_sync,
                mrc_service_client_.get(),
                _1,
                boost::cref(auth_bogus_),
                boost::cref(user_credentials),
                &rq),
            mrc_uuid_iterator_.get(),
            uuid_resolver_,
            RPCOptionsFromOptions(volume_options_)));
    getxattrResponse* get_response = static_cast<getxattrResponse*>(
        response->response());
    if (get_response->has_value_bytes_string()) {
      *value = get_response->value_bytes_string();
      response->DeleteBuffers();
      return true;
    } else if (get_response->has_value()) {
      *value = get_response->value();
      response->DeleteBuffers();
      return true;
    } else {
      response->DeleteBuffers();
      return false;
    }
  } else {
    // No "xtreemfs." attribute, lookup metadata cache.
    if (!metadata_cache_.GetXAttr(path, name, value, &xattrs_cached)) {
      // XAttr not found in cache.
      if (xattrs_cached) {
        // All attributes were cached but the requested attribute was not found,
        // i.e. it won't exist on the server.
        return false;
      }
    }

    // Retrieve complete list of xattrs
    // (because xattrs were not cached)
    boost::scoped_ptr<listxattrResponse> xattrs(
        ListXAttrs(user_credentials, path));

    if (xattrs.get() != NULL) {
      for (int i = 0; i < xattrs->xattrs_size(); i++) {
        if (xattrs->xattrs(i).name() == name) {
          assert(xattrs->xattrs(i).has_value());
          if (xattrs->xattrs(i).has_value_bytes_string
              ()) {
            *value = xattrs->xattrs(i).value_bytes_string();
          } else {
            *value = xattrs->xattrs(i).value();
          }
          return true;
        }
      }
    }
    return false;
  }
}

bool VolumeImplementation::GetXAttrSize(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const std::string& name,
    int* size) {
  // Try to get it from cache first.
  // We also return the size of cached "xtreemfs." attributes. However, the
  // actual size may differ as GetXAttr() does never return "xtreemfs."
  // attributes from the cache.
  bool xattrs_cached;
  bool xtreemfs_attribute_requested = (name.substr(0, 9) == "xtreemfs.");

  // Differ between ".xtreemfs" attributes and user attributes.
  if (xtreemfs_attribute_requested) {
    // Always retrieve the size of ".xtreemfs" attributes from the server.
    string value;
    if (GetXAttr(user_credentials, path, name, &value)) {
      *size = value.size();
      return true;
    } else {
      return false;
    }
  } else {
    // User attribute.
    if (metadata_cache_.GetXAttrSize(path, name, size, &xattrs_cached)) {
      return true;
    }

    if (xattrs_cached) {
      return false;  // Cached but specific attribute not found.
    }

    // Retrieve complete list of xattrs.
    boost::scoped_ptr<listxattrResponse> xattrs(
        ListXAttrs(user_credentials, path));

    if (xattrs.get() != NULL) {
      for (int i = 0; i < xattrs->xattrs_size(); i++) {
        if (xattrs->xattrs(i).name() == name) {
          assert(xattrs->xattrs(i).has_value());
          if (xattrs->xattrs(i).has_value_bytes_string()) {
            *size = xattrs->xattrs(i).value_bytes_string().size();
          } else {
            *size = xattrs->xattrs(i).value().size();
          }
          return true;
        }
      }
    }
    return false;
  }
}

void VolumeImplementation::RemoveXAttr(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const std::string& name) {
  removexattrRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  rq.set_name(name);

  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::removexattr_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));
  response->DeleteBuffers();

  metadata_cache_.InvalidateXAttr(path, name);
}

void VolumeImplementation::AddReplica(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const xtreemfs::pbrpc::Replica& new_replica) {
  xtreemfs_replica_addRequest replica_addRequest;
  replica_addRequest.set_volume_name(volume_name_);
  replica_addRequest.set_path(path);
  replica_addRequest.mutable_new_replica()->CopyFrom(new_replica);

  // Add replica to file in MRC.
  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::xtreemfs_replica_add_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &replica_addRequest),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));

  xtreemfs_replica_addResponse* replica_addResponse;
  replica_addResponse = static_cast<xtreemfs_replica_addResponse*>(response->response());

  assert(replica_addResponse);

  int expected_version = replica_addResponse->expected_xlocset_version();
  string global_file_id(replica_addResponse->file_id());

  response->DeleteBuffers();

  XLocSet new_xlocset;
  WaitForXLocSetInstallation(user_credentials, global_file_id, expected_version, &new_xlocset);

  // Update the local XLocSet cached at FileInfo if it exists.
  uint64_t file_id = ExtractFileIdFromGlobalFileId(global_file_id);
  map<uint64_t, FileInfo*>::const_iterator it = open_file_table_.find(file_id);
  if (it != open_file_table_.end()) {
    // File has already been opened: refresh the xlocset.
    it->second->UpdateXLocSetAndRest(new_xlocset);
  }

  // Trigger the ronly replication at this point by reading at least one byte.
  if (new_xlocset.replica_update_policy() == "ronly") {
    FileHandle* file_handle = OpenFile(user_credentials,
                                       path,
                                       SYSTEM_V_FCNTL_H_O_RDONLY);
    try {
      file_handle->PingReplica(new_replica.osd_uuids(0));
    } catch (const exception&) {
      file_handle->Close();  // Cleanup temporary file handle.
      throw;  // Rethrow exception.
    }
    file_handle->Close();
  }
}

xtreemfs::pbrpc::Replicas* VolumeImplementation::ListReplicas(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path) {
  xtreemfs_get_xlocsetRequest get_xlocsetRequest;
  get_xlocsetRequest.set_volume_name(volume_name_);
  get_xlocsetRequest.set_path(path);

  // Retrieve list of replicas.
  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::xtreemfs_get_xlocset_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &get_xlocsetRequest),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));

  // Create new replicas object to fill from the returned xlocset.
  xtreemfs::pbrpc::Replicas* replicas = new xtreemfs::pbrpc::Replicas();

  // Extract the replicas from the XLocSet
  xtreemfs::pbrpc::XLocSet* xlocset = static_cast<xtreemfs::pbrpc::XLocSet*>(response->response());
  for (int i = 0; i < xlocset->replicas_size(); i++) {
    replicas->add_replicas()->CopyFrom(xlocset->replicas(i));
  }

  // Cleanup.
  response->DeleteBuffers();

  // Return the Replicas object.
  return replicas;
}

void VolumeImplementation::GetXLocSet(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& file_id,
    xtreemfs::pbrpc::XLocSet* xlocset) {
  xtreemfs_get_xlocsetRequest get_xlocsetRequest;
  get_xlocsetRequest.set_file_id(file_id);

  // Retrieve list of replicas.
  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::xtreemfs_get_xlocset_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &get_xlocsetRequest),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));

  // Copy the xlocset
  xlocset->CopyFrom(*(response->response()));

  // Cleanup.
  response->DeleteBuffers();
}

void VolumeImplementation::RemoveReplica(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const std::string& osd_uuid) {
  xtreemfs_replica_removeRequest replica_removeRequest;
  replica_removeRequest.set_volume_name(volume_name_);
  replica_removeRequest.set_path(path);
  replica_removeRequest.set_osd_uuid(osd_uuid);

  // Remove replica.
  boost::scoped_ptr<rpc::SyncCallbackBase> response_mrc(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::xtreemfs_replica_remove_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &replica_removeRequest),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));

  xtreemfs_replica_removeResponse* replica_removeResponse;
  replica_removeResponse = static_cast<xtreemfs_replica_removeResponse*>(response_mrc->response());

  assert(replica_removeResponse);
  assert(replica_removeResponse->has_unlink_xloc());
  assert(replica_removeResponse->has_unlink_xcap());

  int expected_version = replica_removeResponse->expected_xlocset_version();
  string global_file_id(replica_removeResponse->file_id());

  XLocSet new_xlocset;
  WaitForXLocSetInstallation(user_credentials, global_file_id, expected_version, &new_xlocset);

  // Now unlink the replica at the OSD.
  SimpleUUIDIterator osd_uuid_iterator;
  osd_uuid_iterator.AddUUID(osd_uuid);

  unlink_osd_Request unlink_osd_Request;

  unlink_osd_Request.set_file_id(global_file_id);
  FileCredentials* creds = unlink_osd_Request.mutable_file_credentials();
  creds->mutable_xlocs()->CopyFrom(replica_removeResponse->unlink_xloc());
  creds->mutable_xcap()->CopyFrom(replica_removeResponse->unlink_xcap());

  OSDServiceClient osd_service_client(network_client_.get());
  boost::scoped_ptr<rpc::SyncCallbackBase> response_osd(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::OSDServiceClient::unlink_sync,
              &osd_service_client,
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &unlink_osd_Request),
          &osd_uuid_iterator,
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));

  // Update the local XLocSet cached at FileInfo if it exists.
  uint64_t file_id = ExtractFileIdFromGlobalFileId(global_file_id);
  map<uint64_t, FileInfo*>::const_iterator it = open_file_table_.find(file_id);
  if (it != open_file_table_.end()) {
    // File has already been opened: refresh the xlocset.
    it->second->UpdateXLocSetAndRest(new_xlocset);
  }

  // Cleanup.
  response_mrc->DeleteBuffers();
  response_osd->DeleteBuffers();
}


void VolumeImplementation::WaitForXLocSetInstallation(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& file_id,
    int expected_version,
    xtreemfs::pbrpc::XLocSet* xlocset) {
  // The delay to wait between two pings in seconds.
  int64_t delay_ms = volume_options_.xLoc_install_poll_interval_s * 1000;

  // The initial call is made without delay.
  GetXLocSet(user_credentials, file_id, xlocset);

  // Periodically ping the MRC to request the current XLocSet.
  while (xlocset->version() < expected_version) {
    // Delay the xLocSet renewal and the next run of the operation.
    Interruptibilizer::SleepInterruptible(delay_ms, volume_options_.was_interrupted_function);

    GetXLocSet(user_credentials, file_id, xlocset);
  }

  if (xlocset->version() > expected_version) {
    string msg("Missed the expected xLocSet after installing a new view. Please check if the xLocSet is correct.");
    Logging::log->getLog(LEVEL_NOTICE) << "WaitForXLocSetInstallation: " << msg << endl;
    throw IOException(msg);
  }
}

void VolumeImplementation::GetSuitableOSDs(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    int number_of_osds,
    std::list<std::string>* list_of_osd_uuids) {
  assert(list_of_osd_uuids);

  xtreemfs_get_suitable_osdsRequest get_suitable_osdsRequest;
  get_suitable_osdsRequest.set_volume_name(volume_name_);
  get_suitable_osdsRequest.set_path(path);
  get_suitable_osdsRequest.set_num_osds(number_of_osds);

  // Retrieve the list of volumes from the MRC.
  boost::scoped_ptr<rpc::SyncCallbackBase> response(
      ExecuteSyncRequest(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::
                  xtreemfs_get_suitable_osds_sync,
              mrc_service_client_.get(),
              _1,
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &get_suitable_osdsRequest),
          mrc_uuid_iterator_.get(),
          uuid_resolver_,
          RPCOptionsFromOptions(volume_options_)));

  // Write back list of UUIDs to list_of_osd_uuids.
  xtreemfs_get_suitable_osdsResponse* osds =
      static_cast<xtreemfs_get_suitable_osdsResponse*>(response->response());
  for (int i = 0; i < osds->osd_uuids_size(); i++) {
    list_of_osd_uuids->push_back(osds->osd_uuids(i));
  }

  // Cleanup.
  response->DeleteBuffers();
}

/**
 * @remark Ownership is NOT transferred to the caller.
 *
 * @remark Assumes that open_file_table_mutex_ is already locked.
 */
FileInfo* VolumeImplementation::GetFileInfoOrCreateUnmutexed(
    uint64_t file_id,
    const std::string& path,
    bool replicate_on_close,
    const xtreemfs::pbrpc::XLocSet& xlocset) {
  // Check if the file is already open and a FileInfo object exists for it.
  map<uint64_t, FileInfo*>::const_iterator it = open_file_table_.find(file_id);
  if (it != open_file_table_.end()) {
    // File has already been opened.
    it->second->UpdateXLocSetAndRest(xlocset, replicate_on_close);
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG) << "GetFileInfoOrCreateUnmutexed: "
          << "Updated the FileInfo object with the file_id: "
          << file_id << endl;
    }
    return it->second;
  } else {
    // File has not been opened yet, add it.
    FileInfo* file_info(new FileInfo(client_,
                                     this,
                                     file_id,
                                     path,
                                     replicate_on_close,
                                     xlocset,
                                     client_uuid_));
    open_file_table_[file_id] = file_info;
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG) << "GetFileInfoOrCreateUnmutexed: "
          << "Created a new FileInfo object for the file_id: "
          << file_id << endl;
    }
    return file_info;
  }
}

/**
 * @throws FileInfoNotFoundException
 *
 * @remark Assumes that open_file_table_mutex_ is already locked.
 */
void VolumeImplementation::RemoveFileInfoUnmutexed(
    uint64_t file_id, FileInfo* file_info) {
  // Find the correct entry and delete it
  std::map<uint64_t, FileInfo*>::iterator it;
  it = open_file_table_.find(file_id);
  // The entry has to be found or throw an exception.
  if (it == open_file_table_.end()) {
    throw FileInfoNotFoundException(file_id);
  }
  // Lets be sure we speak about the same FileInfo object.
  assert(it->second == file_info);
  open_file_table_.erase(it);
}

void VolumeImplementation::PeriodicXCapRenewal() {
  while (true) {
    // Send thread to sleep (by default for 1 minute).
    boost::posix_time::seconds interval(
        volume_options_.periodic_xcap_renewal_interval_s);
    boost::this_thread::sleep(interval);

    {
      boost::mutex::scoped_lock lock(open_file_table_mutex_);

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
            << "START open_file_table: Periodic XCap renewal for "
            << open_file_table_.size()
            << " open files." << endl;
      }

      // Iterate over the open_file_table_.
      map<uint64_t, FileInfo*>::iterator it;
      for (it = open_file_table_.begin();
           it != open_file_table_.end(); ++it) {
        it->second->RenewXCapsAsync(periodic_threads_options_);
      }

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
            << "END open_file_table: Periodic XCap renewal for "
            << open_file_table_.size()
            << " open files." << std::endl;
      }
    }
  }
}

void VolumeImplementation::PeriodicFileSizeUpdate() {
  while (true) {
    // Send thread to sleep.
    boost::posix_time::seconds interval(
        volume_options_.periodic_file_size_updates_interval_s);
    boost::this_thread::sleep(interval);

    {
      boost::mutex::scoped_lock lock(open_file_table_mutex_);

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
            << "START open_file_table: Periodic filesize update for "
            << open_file_table_.size()
            << " open files." << std::endl;
      }

      // Iterate over the open_file_table_.
      map<uint64_t, FileInfo*>::iterator it;
      for (it = open_file_table_.begin();
           it != open_file_table_.end(); ++it) {
        it->second->WriteBackFileSizeAsync(periodic_threads_options_);
      }

      if (Logging::log->loggingActive(LEVEL_DEBUG)) {
        Logging::log->getLog(LEVEL_DEBUG)
            << "END open_file_table: Periodic filesize update for "
            << open_file_table_.size()
            << " open files." << std::endl;
      }
    }
  }
}

}  // namespace xtreemfs
