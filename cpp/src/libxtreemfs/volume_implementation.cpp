/*
 * Copyright (c) 2011 by Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#include "libxtreemfs/volume_implementation.h"

#include <algorithm>
#include <boost/bind.hpp>
#include <boost/thread/thread.hpp>
#include <map>
#include <string>

#include "libxtreemfs/callback/execute_sync_request.h"
#include "libxtreemfs/client_implementation.h"
#include "libxtreemfs/file_handle_implementation.h"
#include "libxtreemfs/file_info.h"
#include "libxtreemfs/helper.h"
#include "libxtreemfs/options.h"
#include "libxtreemfs/stripe_translator.h"
#include "libxtreemfs/xtreemfs_exception.h"
#include "rpc/client.h"
#include "util/logging.h"
#include "xtreemfs/MRC.pb.h"
#include "xtreemfs/MRCServiceClient.h"
#include "xtreemfs/OSDServiceClient.h"

using namespace std;
using namespace xtreemfs::util;
using namespace xtreemfs::pbrpc;

namespace xtreemfs {

VolumeImplementation::VolumeImplementation(
    ClientImplementation* client,
    const std::string& client_uuid,
    const std::string& mrc_uuid,
    const std::string& volume_name,
    const xtreemfs::rpc::SSLOptions* ssl_options,
    const Options& options)
    : client_(client),
      uuid_resolver_(client),
      client_uuid_(client_uuid),
      mrc_uuid_(mrc_uuid),
      volume_name_(volume_name),
      volume_ssl_options_(ssl_options),
      volume_options_(options),
      metadata_cache_(options.metadata_cache_size,
                      options.metadata_cache_ttl_s) {
  // Set AuthType to AUTH_NONE as it's currently not used.
  auth_bogus_.set_auth_type(AUTH_NONE);
  // Set username "xtreemfs" as it does not get checked at server side.
  user_credentials_bogus_.set_username("xtreemfs");
}

VolumeImplementation::~VolumeImplementation() {
  // Make sure we were shutdown properly and there are no open files.
  assert(open_file_table_.size() == 0);

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
      &xtreemfs::VolumeImplementation::PeriodicXCapRenewal, this)));
  filesize_writeback_thread_.reset(new boost::thread(boost::bind(
      &xtreemfs::VolumeImplementation::PeriodicFileSizeUpdate, this)));
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
  assert(open_file_table_.size() == 0);

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
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  statvfsRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_known_etag(0);
  boost::scoped_ptr< SyncCallback<StatVFS> > response(
      ExecuteSyncRequest< SyncCallback<StatVFS>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::statvfs_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));

  // Delete everything except the response.
  delete[] response->data();
  delete response->error();
  return response->response();
}

void VolumeImplementation::ReadLink(
      const xtreemfs::pbrpc::UserCredentials& user_credentials,
      const std::string& path,
      std::string* link_target_path) {
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  readlinkRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  boost::scoped_ptr< SyncCallback<readlinkResponse> > response(
      ExecuteSyncRequest< SyncCallback<readlinkResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::readlink_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));

  // The XtreemFS MRC always returns one resolved target or throws an EINVAL.
  assert(response->response()->link_target_path_size() == 1);
  *link_target_path = response->response()->link_target_path(0);
  response->DeleteBuffers();
}

void VolumeImplementation::Symlink(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& target_path,
    const std::string& link_path) {
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  symlinkRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_target_path(target_path);
  rq.set_link_path(link_path);
  boost::scoped_ptr< SyncCallback<timestampResponse> > response(
      ExecuteSyncRequest< SyncCallback<timestampResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::symlink_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));

  const string parent_dir = ResolveParentDirectory(link_path);
  metadata_cache_.UpdateStatTime(
      parent_dir,
      response->response()->timestamp_s(),
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
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  linkRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_target_path(target_path);
  rq.set_link_path(link_path);
  boost::scoped_ptr< SyncCallback<timestampResponse> > response(
      ExecuteSyncRequest< SyncCallback<timestampResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::link_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));

  const string parent_dir = ResolveParentDirectory(link_path);
  metadata_cache_.UpdateStatTime(
      parent_dir,
      response->response()->timestamp_s(),
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
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  accessRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  rq.set_flags(flags);

  boost::scoped_ptr< SyncCallback<emptyResponse> > response(
      ExecuteSyncRequest< SyncCallback<emptyResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::access_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));

  response->DeleteBuffers();
}

FileHandle* VolumeImplementation::OpenFile(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags) {
  return OpenFile(user_credentials, path, flags, 0, 0);
}

FileHandle* VolumeImplementation::OpenFile(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags,
    boost::uint32_t mode) {
  return OpenFile(user_credentials, path, flags, mode, 0);
}

/**
 * @throws IOException
 */
FileHandle* VolumeImplementation::OpenFile(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags,
    boost::uint32_t mode,
    int truncate_new_file_size) {
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);


  // Handle o_sync.
  // TODO(mberlin): Currently no special handling needed?
  if (flags & SYSTEM_V_FCNTL_H_O_SYNC) {
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG) << "open called with O_SYNC." << endl;
    }
  }

  openRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  rq.set_flags(flags);
  rq.set_mode(mode);
  rq.set_attributes(0);

  boost::scoped_ptr< SyncCallback<openResponse> > response(
      ExecuteSyncRequest< SyncCallback<openResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::open_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));

  openResponse* open_response = response->response();
  // We must have obtained file credentials.
  assert(open_response->has_creds());

  FileHandleImplementation* file_handle = NULL;
  // Create a FileInfo object if it does not exist yet.
  {
    boost::mutex::scoped_lock lock(open_file_table_mutex_);

    FileInfo* file_info = GetFileInfoOrCreateUnmutexed(
        ExtractFileIdFromXCap(open_response->creds().xcap()),
        path,
        open_response->creds().xcap().replicate_on_close(),
        open_response->creds().xlocs());
    file_handle = file_info->CreateFileHandle(open_response->creds().xcap());
  }

  // If O_CREAT is set and the file did not previously exist, upon successful
  // completion, open() shall mark for update the st_atime, st_ctime, and
  // st_mtime fields of the file and the st_ctime and st_mtime fields of
  // the parent directory.
  if ((flags & SYSTEM_V_FCNTL_H_O_CREAT)) {
    const string parent_dir = ResolveParentDirectory(path);
    metadata_cache_.UpdateStatTime(
        parent_dir,
        open_response->timestamp_s(),
        static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));
    // TODO(mberlin): Retrieve stat as optional member of openResponse instead
    //                and update cached DirectoryEntries accordingly.
    metadata_cache_.InvalidateDirEntries(parent_dir);
  }

  // If O_TRUNC was set, go on processing the truncate request.
  if ((flags & SYSTEM_V_FCNTL_H_O_TRUNC)) {
    // Update mtime and ctime of the file if O_TRUNC was set.
    metadata_cache_.UpdateStatTime(
        path,
        open_response->timestamp_s(),
        static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));

    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "open called with O_TRUNC." << endl;
    }

    try {
      file_handle->TruncatePhaseTwoAndThree(user_credentials,
                                            truncate_new_file_size);
    } catch(const XtreemFSException& e) {
      // Truncate did fail, close file again.
      file_handle->Close();
      throw;  // Rethrow error.
    }
  }

  response->DeleteBuffers();

  return file_handle;
}

void VolumeImplementation::Truncate(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    off_t new_file_size) {
  // Open file with O_TRUNC.
  const xtreemfs::pbrpc::SYSTEM_V_FCNTL flags = static_cast<SYSTEM_V_FCNTL>(
      SYSTEM_V_FCNTL_H_O_TRUNC | SYSTEM_V_FCNTL_H_O_WRONLY);
  FileHandle* file_handle
      = OpenFile(user_credentials, path, flags, 0, new_file_size);

  // Close file.
  file_handle->Close();
}

/**
 * @throws FileInfoNotFoundException
 * @throws FileHandleNotFoundException
 */
void VolumeImplementation::CloseFile(
    boost::uint64_t file_id,
    FileInfo* file_info) {
  // Remove file_info if it has no more open file handles.
  boost::mutex::scoped_lock lock(open_file_table_mutex_);
  if (file_info->DecreaseReferenceCount() == 0) {
    file_info->WaitForPendingFileSizeUpdates();
    RemoveFileInfoUnmutexed(file_id, file_info);
    // file_info is no longer visible: it's safe to unlock the open_file_table_.
    lock.unlock();

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
  GetAttr(user_credentials, path, stat_buffer, NULL);
}

void VolumeImplementation::GetAttr(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    xtreemfs::pbrpc::Stat* stat_buffer,
    FileInfo* file_info) {
  // Check if the information was cached.
  if (metadata_cache_.GetStat(path, stat_buffer)) {
    // Found in StatCache.
    if (Logging::log->loggingActive(LEVEL_DEBUG)) {
      Logging::log->getLog(LEVEL_DEBUG)
          << "getattr: serving from stat-cache " << path
          << " " << stat_buffer->size() << endl;
    }
  } else {
    // Not found in StatCache, retrieve from MRC.
    string mrc_address;
    uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

    getattrRequest rq;
    rq.set_volume_name(volume_name_);
    rq.set_path(path);
    rq.set_known_etag(0);

    boost::scoped_ptr< SyncCallback<getattrResponse> > response(
        ExecuteSyncRequest< SyncCallback<getattrResponse>* >(
            boost::bind(
                &xtreemfs::pbrpc::MRCServiceClient::getattr_sync,
                mrc_service_client_.get(),
                boost::cref(mrc_address),
                boost::cref(auth_bogus_),
                boost::cref(user_credentials),
                &rq),
            volume_options_.max_tries,
            volume_options_));

    stat_buffer->CopyFrom(response->response()->stbuf());
    if (stat_buffer->nlink() > 1) {  // Do not cache hard links.
      metadata_cache_.Invalidate(path);
    } else {
      metadata_cache_.UpdateStat(path, *stat_buffer);
    }

    response->DeleteBuffers();
  }

  // Merge StatCache object with possibly newer information from FileInfo.
  if (file_info == NULL) {
    MergeStatAndOSDWriteResponseFromFileInfo(stat_buffer->ino(), stat_buffer);
  } else {
    file_info->MergeStatAndOSDWriteResponse(stat_buffer);
  }
}

void VolumeImplementation::SetAttr(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const xtreemfs::pbrpc::Stat& stat,
    xtreemfs::pbrpc::Setattrs to_set) {
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  setattrRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  rq.mutable_stbuf()->CopyFrom(stat);
  rq.set_to_set(to_set);

  boost::scoped_ptr< SyncCallback<timestampResponse> > response(
      ExecuteSyncRequest< SyncCallback<timestampResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::setattr_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));

  // "chmod" or "chown" operations result into updating the ctime attribute.
  if ((to_set &  SETATTR_MODE) || (to_set &  SETATTR_UID) ||
      (to_set &  SETATTR_GID)) {
    to_set = static_cast<Setattrs>(to_set | SETATTR_CTIME);
    rq.mutable_stbuf()->set_ctime_ns(static_cast<boost::uint64_t>(
        response->response()->timestamp_s()) * 1000000000);
  }

  // Do not cache hardlinks or chmod operations which try to set the SGID bit
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
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  // 1. Delete file at MRC.
  unlinkRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);

  boost::scoped_ptr< SyncCallback<unlinkResponse> > response(
      ExecuteSyncRequest< SyncCallback<unlinkResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::unlink_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));

  // 2. Invalidate metadata caches.
  metadata_cache_.Invalidate(path);
  const string parent_dir = ResolveParentDirectory(path);
  metadata_cache_.UpdateStatTime(
      parent_dir,
      response->response()->timestamp_s(),
      static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));
  metadata_cache_.InvalidateDirEntry(parent_dir, GetBasename(path));

  // 3. Delete objects of all replicas on the OSDs.
  if (response->response()->has_creds()) {
    UnlinkAtOSD(response->response()->creds(), path);
  }

  response->DeleteBuffers();
}

void VolumeImplementation::UnlinkAtOSD(
    const FileCredentials& fc, const std::string& path) {
  const XLocSet& xlocs = fc.xlocs();
  string osd_address;
  int max_total_tries = volume_options_.max_tries;
  int attempts_so_far = 0;
  // Retry unlink object if failed.
  unlink_osd_Request rq_osd;
  rq_osd.mutable_file_credentials()->CopyFrom(fc);
  rq_osd.set_file_id(fc.xcap().file_id());
  // Remove _all_ replicas.
  for (int k = 0; k < xlocs.replicas_size(); k++) {
    for (int i = 0;
         attempts_so_far < max_total_tries || max_total_tries == 0;
         i++) {
      attempts_so_far++;

      uuid_resolver_->UUIDToAddress(GetOSDUUIDFromXlocSet(
                                        xlocs,
                                        k,
                                        0),
                                    &osd_address);

      boost::scoped_ptr< SyncCallback<emptyResponse> > response_osd;
      try {
        response_osd.reset(ExecuteSyncRequest< SyncCallback<emptyResponse>* >(
            boost::bind(&xtreemfs::pbrpc::OSDServiceClient::unlink_sync,
                        osd_service_client_.get(),
                        boost::cref(osd_address),
                        boost::cref(auth_bogus_),
                        boost::cref(user_credentials_bogus_),
                        &rq_osd),
            1,  // Only one attempt, we retry on our own.
            volume_options_,
            attempts_so_far != max_total_tries));  // Delay all but last try.
        response_osd->DeleteBuffers();
      } catch(const IOException& e) {
        Logging::log->getLog(LEVEL_ERROR)
            << "error in unlink "<< path << endl;
        if (attempts_so_far < max_total_tries || max_total_tries == 0) {
          continue;  // Retry to delete the objects on this replica.
        } else {
          throw;  // Last attempt failed, rethrow exception.
        }
      }

      break;  // Unlink on this replica successful, do not retry again.
    }  // End of retry loop.
  }  // End of loop over all replicas..
}

void VolumeImplementation::Rename(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const std::string& new_path) {
  if (path == new_path) {
    return;  // Do nothing.
  }
  // 1. Issue rename at MRC.
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  renameRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_source_path(path);
  rq.set_target_path(new_path);

  boost::scoped_ptr< SyncCallback<renameResponse> > response(
      ExecuteSyncRequest< SyncCallback<renameResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::rename_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));

  // 2. Remove file content of any previous files at "new_path".
  if (response->response()->has_creds()) {
    UnlinkAtOSD(response->response()->creds(), new_path);
  }

  // 3. Update caches
  // Update the timestamps of parents of both directories.
  const std::string parent_path = ResolveParentDirectory(path);
  const std::string parent_new_path = ResolveParentDirectory(new_path);
  if (response->response()->timestamp_s() != 0) {
    metadata_cache_.UpdateStatTime(
        parent_path,
        response->response()->timestamp_s(),
        static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));
    metadata_cache_.UpdateStatTime(
        parent_new_path,
        response->response()->timestamp_s(),
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
                                 response->response()->timestamp_s(),
                                 static_cast<Setattrs>(SETATTR_CTIME));

  // Rename path in all open FileInfo objects.
  {
    boost::mutex::scoped_lock lock(open_file_table_mutex_);
    map<boost::uint64_t, FileInfo*>::iterator it;
    for (it = open_file_table_.begin();
         it != open_file_table_.end(); ++it) {
      it->second->RenamePath(path, new_path);
    }
  }

  response->DeleteBuffers();
}

void VolumeImplementation::CreateDirectory(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    mode_t mode) {
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  mkdirRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  rq.set_mode(mode);

  boost::scoped_ptr< SyncCallback<timestampResponse> > response(
      ExecuteSyncRequest< SyncCallback<timestampResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::mkdir_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));

  const string parent_dir = ResolveParentDirectory(path);
  metadata_cache_.UpdateStatTime(
      parent_dir,
      response->response()->timestamp_s(),
      static_cast<Setattrs>(SETATTR_CTIME | SETATTR_MTIME));
  // TODO(mberlin): Retrieve stat as optional member of openResponse instead
  //                and update cached DirectoryEntries accordingly.
  metadata_cache_.InvalidateDirEntries(parent_dir);

  response->DeleteBuffers();
}

void VolumeImplementation::RemoveDirectory(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path) {
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  rmdirRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);

  boost::scoped_ptr< SyncCallback<timestampResponse> > response(
      ExecuteSyncRequest< SyncCallback<timestampResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::rmdir_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));

  const string parent_dir = ResolveParentDirectory(path);
  metadata_cache_.UpdateStatTime(
      parent_dir,
      response->response()->timestamp_s(),
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
    boost::uint64_t offset,
    boost::uint32_t count,
    bool names_only) {
  DirectoryEntries* result = NULL;

  result = metadata_cache_.GetDirEntries(path, offset, count);
  if (result != NULL) {
    return result;
  }

  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  // Process large requests in multiples of readdir_chunk_size.
  readdirRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_known_etag(0);
  rq.set_path(path);
  rq.set_names_only(names_only);
  for (int current_offset = offset;
       current_offset < offset + count;
       current_offset += volume_options_.readdir_chunk_size) {
    rq.set_seen_directory_entries_count(current_offset);
    // Read complete chunk or only remaining rest.
    rq.set_limit_directory_entries_count(current_offset > offset + count ?
        current_offset - offset - count : volume_options_.readdir_chunk_size);

    boost::scoped_ptr< SyncCallback<DirectoryEntries> > response(
        ExecuteSyncRequest< SyncCallback<DirectoryEntries>* >(
            boost::bind(
                &xtreemfs::pbrpc::MRCServiceClient::readdir_sync,
                mrc_service_client_.get(),
                boost::cref(mrc_address),
                boost::cref(auth_bogus_),
                boost::cref(user_credentials),
                &rq),
            volume_options_.max_tries,
            volume_options_));
    DirectoryEntries* dentries = response->response();

    // Process request and free memory.
    if (current_offset == offset) {
      // First chunk
      result = response->response();

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

  // Cache the first stat buffers that fit into the cache.
  for (int i = 0;
       i < min(volume_options_.metadata_cache_size,
               static_cast<boost::uint64_t>(result->entries_size()));
       i++) {
    if (result->entries(i).stbuf().nlink() > 1) {  // Do not cache hard links.
      metadata_cache_.Invalidate(path);
    } else {
      metadata_cache_.UpdateStat(
          ConcatenatePath(path, result->entries(i).name()),
          result->entries(i).stbuf());
    }
  }

  // Cache the result if it's the complete directory.
  // We can't tell for sure whether result contains all directory entries if
  // it's size is not less than the requested "count".
  // TODO(mberlin): Cache only names and no stat entries and remove names_only
  //                condition.
  // TODO(mberlin): Set an upper bound of dentries, otherwise don't cache it.
  if (offset == 0 && result->entries_size() < count && !names_only) {
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

  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  listxattrRequest rq;
  rq.set_volume_name(volume_name_);
  rq.set_path(path);
  rq.set_names_only(false);

  boost::scoped_ptr< SyncCallback<listxattrResponse> > response(
      ExecuteSyncRequest< SyncCallback<listxattrResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::listxattr_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));

  result = response->response();
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
  rq.set_value(value.c_str());
  rq.set_flags(flags);

  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  boost::scoped_ptr< SyncCallback<timestampResponse> > response(
      ExecuteSyncRequest< SyncCallback<timestampResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::setxattr_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));
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
    string mrc_address;
    uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

    getxattrRequest rq;
    rq.set_volume_name(volume_name_);
    rq.set_path(path);
    rq.set_name(name);

    boost::scoped_ptr< SyncCallback<getxattrResponse> > response(
        ExecuteSyncRequest< SyncCallback<getxattrResponse>* >(
            boost::bind(
                &xtreemfs::pbrpc::MRCServiceClient::getxattr_sync,
                mrc_service_client_.get(),
                boost::cref(mrc_address),
                boost::cref(auth_bogus_),
                boost::cref(user_credentials),
                &rq),
            volume_options_.max_tries,
            volume_options_));
    if (response->response()->has_value()) {
      *value = response->response()->value();
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
          *value = xattrs->xattrs(i).value();
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
          *size = xattrs->xattrs(i).value().size();
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

  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  boost::scoped_ptr< SyncCallback<timestampResponse> > response(
      ExecuteSyncRequest< SyncCallback<timestampResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::removexattr_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &rq),
          volume_options_.max_tries,
          volume_options_));
  response->DeleteBuffers();

  metadata_cache_.InvalidateXAttr(path, name);
}

void VolumeImplementation::AddReplica(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const xtreemfs::pbrpc::Replica& new_replica) {
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  xtreemfs_replica_addRequest replica_addRequest;
  replica_addRequest.set_volume_name(volume_name_);
  replica_addRequest.set_path(path);
  replica_addRequest.mutable_new_replica()->CopyFrom(new_replica);

  // Add replica to file in MRC.
  boost::scoped_ptr< SyncCallback<xtreemfs::pbrpc::emptyResponse> > response(
      ExecuteSyncRequest< SyncCallback<xtreemfs::pbrpc::emptyResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::xtreemfs_replica_add_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &replica_addRequest),
          volume_options_.max_tries,
          volume_options_));

  response->DeleteBuffers();

  // Trigger the replication at this point by reading at least one byte.
  FileHandle* file_handle = OpenFile(user_credentials,
                                     path,
                                     SYSTEM_V_FCNTL_H_O_RDONLY);
  file_handle->PingReplica(user_credentials, new_replica.osd_uuids(0));
  file_handle->Close();
}

xtreemfs::pbrpc::Replicas* VolumeImplementation::ListReplicas(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path) {
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  xtreemfs_replica_listRequest replica_listRequest;
  replica_listRequest.set_volume_name(volume_name_);
  replica_listRequest.set_path(path);

  // Retrieve list of replicas.
  boost::scoped_ptr< SyncCallback<xtreemfs::pbrpc::Replicas> > response(
      ExecuteSyncRequest< SyncCallback<xtreemfs::pbrpc::Replicas>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::xtreemfs_replica_list_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &replica_listRequest),
          volume_options_.max_tries,
          volume_options_));

  // Delete everything except the response.
  delete[] response->data();
  delete response->error();

  // Return the Replicas object.
  return response->response();
}

void VolumeImplementation::RemoveReplica(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    const std::string& osd_uuid) {
  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  xtreemfs_replica_removeRequest replica_removeRequest;
  replica_removeRequest.set_volume_name(volume_name_);
  replica_removeRequest.set_path(path);
  replica_removeRequest.set_osd_uuid(osd_uuid);

  // Remove replica.
  boost::scoped_ptr< SyncCallback<FileCredentials> > response_mrc(
      ExecuteSyncRequest< SyncCallback<FileCredentials>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::xtreemfs_replica_remove_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &replica_removeRequest),
          volume_options_.max_tries,
          volume_options_));

  // Resolve OSD UUID.
  string osd_address;
  uuid_resolver_->UUIDToAddress(osd_uuid, &osd_address);

  // Now unlink the replica at the OSD.
  unlink_osd_Request unlink_osd_Request;
  unlink_osd_Request.set_file_id(response_mrc->response()->xcap().file_id());
  unlink_osd_Request.mutable_file_credentials()->CopyFrom(
      *(response_mrc->response()));

  OSDServiceClient osd_service_client(network_client_.get());
  boost::scoped_ptr< SyncCallback<emptyResponse> > response_osd(
      ExecuteSyncRequest< SyncCallback<emptyResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::OSDServiceClient::unlink_sync,
              &osd_service_client,
              boost::cref(osd_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &unlink_osd_Request),
          volume_options_.max_tries,
          volume_options_));

  // Cleanup.
  response_mrc->DeleteBuffers();
  response_osd->DeleteBuffers();
}

void VolumeImplementation::GetSuitableOSDs(
    const xtreemfs::pbrpc::UserCredentials& user_credentials,
    const std::string& path,
    int number_of_osds,
    std::list<std::string>* list_of_osd_uuids) {
  assert(list_of_osd_uuids);

  string mrc_address;
  uuid_resolver_->UUIDToAddress(mrc_uuid_, &mrc_address);

  xtreemfs_get_suitable_osdsRequest get_suitable_osdsRequest;
  get_suitable_osdsRequest.set_volume_name(volume_name_);
  get_suitable_osdsRequest.set_path(path);
  get_suitable_osdsRequest.set_num_osds(number_of_osds);

  // Retrieve the list of volumes from the MRC.
  boost::scoped_ptr< SyncCallback<
      xtreemfs::pbrpc::xtreemfs_get_suitable_osdsResponse> > response(
      ExecuteSyncRequest< SyncCallback<
          xtreemfs::pbrpc::xtreemfs_get_suitable_osdsResponse>* >(
          boost::bind(
              &xtreemfs::pbrpc::MRCServiceClient::
                  xtreemfs_get_suitable_osds_sync,
              mrc_service_client_.get(),
              boost::cref(mrc_address),
              boost::cref(auth_bogus_),
              boost::cref(user_credentials),
              &get_suitable_osdsRequest),
          volume_options_.max_tries,
          volume_options_));

  // Write back list of UUIDs to list_of_osd_uuids.
  const xtreemfs_get_suitable_osdsResponse& osds = *response->response();
  for (int i = 0; i < osds.osd_uuids_size(); i++) {
    list_of_osd_uuids->push_back(osds.osd_uuids(i));
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
    boost::uint64_t file_id,
    const std::string& path,
    bool replicate_on_close,
    const xtreemfs::pbrpc::XLocSet& xlocset) {
  // Check if the file is already open and a FileInfo object exists for it.
  map< boost::uint64_t, FileInfo* >::const_iterator it
    = open_file_table_.find(file_id);
  if (it != open_file_table_.end()) {
    // TODO(mberlin): Update xlocset and replicate_on_close information?!
    // File has already been opened.
    return it->second;
  } else {
    // File has not been opened yet, add it.
    FileInfo* file_info(new FileInfo(this,
                                     file_id,
                                     path,
                                     replicate_on_close,
                                     xlocset,
                                     client_uuid_));
    open_file_table_[file_id] = file_info;
    return file_info;
  }
}

/**
 * Needed to merge the getattr result with the OSDWriteResponse if file is open.
 *
 * @remark Ownership is NOT transferred to the caller.
 */
void VolumeImplementation::MergeStatAndOSDWriteResponseFromFileInfo(
    boost::uint64_t file_id,
    xtreemfs::pbrpc::Stat* stat_buffer) {
  boost::mutex::scoped_lock lock(open_file_table_mutex_);

  // Check if the file with file_id is already opened.
  map< boost::uint64_t, FileInfo* >::const_iterator it
    = open_file_table_.find(file_id);
  if (it != open_file_table_.end()) {
    // FileInfo for "file_id" found.
    it->second->MergeStatAndOSDWriteResponse(stat_buffer);
  }
}

/**
 * @throws FileInfoNotFoundException
 *
 * @remark Assumes that open_file_table_mutex_ is already locked.
 */
void VolumeImplementation::RemoveFileInfoUnmutexed(
    boost::uint64_t file_id, FileInfo* file_info) {
  // Find the correct entry and delete it
  std::map< boost::uint64_t, FileInfo* >::iterator it;
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
      map<boost::uint64_t, FileInfo*>::iterator it;
      for (it = open_file_table_.begin();
           it != open_file_table_.end(); ++it) {
        it->second->RenewXCapsAsync();
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
      map<boost::uint64_t, FileInfo*>::iterator it;
      for (it = open_file_table_.begin();
           it != open_file_table_.end(); ++it) {
        it->second->WriteBackFileSizeAsync();
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
