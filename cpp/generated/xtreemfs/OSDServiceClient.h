//automatically generated from OSD.proto at Wed Nov 11 16:01:25 CET 2015
//(c) 2015. See LICENSE file for details.

#ifndef OSDSERVICECLIENT_H
#define OSDSERVICECLIENT_H

#include <stdint.h>
#include "pbrpc/RPC.pb.h"
#include "rpc/client.h"
#include "rpc/sync_callback.h"
#include "rpc/callback_interface.h"
#include "xtreemfs/OSD.pb.h"
#include "include/Common.pb.h"
#include "xtreemfs/GlobalTypes.pb.h"


namespace xtreemfs {
namespace pbrpc {
        using ::xtreemfs::rpc::Client;
        using ::xtreemfs::rpc::CallbackInterface;
        using ::xtreemfs::rpc::SyncCallback;

        class OSDServiceClient {

        public:
            OSDServiceClient(Client* client) : client_(client) {
            }

            virtual ~OSDServiceClient() {
            }

            void read(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::readRequest* request,
                CallbackInterface<xtreemfs::pbrpc::ObjectData> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 10,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ObjectData(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::ObjectData>* read_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::readRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::ObjectData>* sync_cb = new SyncCallback<xtreemfs::pbrpc::ObjectData>();
                client_->sendRequest(address, 30001, 10,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ObjectData(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void truncate(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::truncateRequest* request,
                CallbackInterface<xtreemfs::pbrpc::OSDWriteResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 11,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::OSDWriteResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::OSDWriteResponse>* truncate_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::truncateRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::OSDWriteResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::OSDWriteResponse>();
                client_->sendRequest(address, 30001, 11,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::OSDWriteResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void unlink(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::unlink_osd_Request* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 12,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* unlink_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::unlink_osd_Request* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 12,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void write(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::writeRequest* request,const char* data, uint32_t data_length,
                CallbackInterface<xtreemfs::pbrpc::OSDWriteResponse> *callback, void *context = NULL) {
                client_->sendRequest(address, 30001, 13,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::OSDWriteResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::OSDWriteResponse>* write_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::writeRequest* request, const char* data, uint32_t data_length) {
                SyncCallback<xtreemfs::pbrpc::OSDWriteResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::OSDWriteResponse>();
                client_->sendRequest(address, 30001, 13,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::OSDWriteResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_broadcast_gmax(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_broadcast_gmaxRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 20,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_broadcast_gmax_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_broadcast_gmaxRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 20,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_check_object(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_check_objectRequest* request,
                CallbackInterface<xtreemfs::pbrpc::ObjectData> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 21,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ObjectData(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::ObjectData>* xtreemfs_check_object_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_check_objectRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::ObjectData>* sync_cb = new SyncCallback<xtreemfs::pbrpc::ObjectData>();
                client_->sendRequest(address, 30001, 21,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ObjectData(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_cleanup_get_results(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_cleanup_get_resultsResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 30001, 30,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_cleanup_get_resultsResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_cleanup_get_resultsResponse>* xtreemfs_cleanup_get_results_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_cleanup_get_resultsResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_cleanup_get_resultsResponse>();
                client_->sendRequest(address, 30001, 30,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_cleanup_get_resultsResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_cleanup_is_running(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_cleanup_is_runningResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 30001, 31,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_cleanup_is_runningResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_cleanup_is_runningResponse>* xtreemfs_cleanup_is_running_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_cleanup_is_runningResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_cleanup_is_runningResponse>();
                client_->sendRequest(address, 30001, 31,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_cleanup_is_runningResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_cleanup_start(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_cleanup_startRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 32,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_cleanup_start_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_cleanup_startRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 32,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_cleanup_status(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_cleanup_statusResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 30001, 33,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_cleanup_statusResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_cleanup_statusResponse>* xtreemfs_cleanup_status_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_cleanup_statusResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_cleanup_statusResponse>();
                client_->sendRequest(address, 30001, 33,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_cleanup_statusResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_cleanup_stop(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 30001, 34,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_cleanup_stop_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 34,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_cleanup_versions_start(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 30001, 35,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_cleanup_versions_start_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 35,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_finalize_vouchers(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_finalize_vouchersRequest* request,
                CallbackInterface<xtreemfs::pbrpc::OSDFinalizeVouchersResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 22,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::OSDFinalizeVouchersResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::OSDFinalizeVouchersResponse>* xtreemfs_finalize_vouchers_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_finalize_vouchersRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::OSDFinalizeVouchersResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::OSDFinalizeVouchersResponse>();
                client_->sendRequest(address, 30001, 22,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::OSDFinalizeVouchersResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_repair_object(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_repair_objectRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 36,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_repair_object_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_repair_objectRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 36,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_rwr_fetch(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_rwr_fetchRequest* request,
                CallbackInterface<xtreemfs::pbrpc::ObjectData> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 73,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ObjectData(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::ObjectData>* xtreemfs_rwr_fetch_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_rwr_fetchRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::ObjectData>* sync_cb = new SyncCallback<xtreemfs::pbrpc::ObjectData>();
                client_->sendRequest(address, 30001, 73,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ObjectData(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_rwr_flease_msg(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_rwr_flease_msgRequest* request,const char* data, uint32_t data_length,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                client_->sendRequest(address, 30001, 71,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_rwr_flease_msg_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_rwr_flease_msgRequest* request, const char* data, uint32_t data_length) {
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 71,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_rwr_notify(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::FileCredentials* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 75,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_rwr_notify_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::FileCredentials* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 75,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_rwr_set_primary_epoch(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_rwr_set_primary_epochRequest* request,
                CallbackInterface<xtreemfs::pbrpc::ObjectData> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 78,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ObjectData(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::ObjectData>* xtreemfs_rwr_set_primary_epoch_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_rwr_set_primary_epochRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::ObjectData>* sync_cb = new SyncCallback<xtreemfs::pbrpc::ObjectData>();
                client_->sendRequest(address, 30001, 78,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ObjectData(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_rwr_status(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_rwr_statusRequest* request,
                CallbackInterface<xtreemfs::pbrpc::ReplicaStatus> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 76,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ReplicaStatus(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::ReplicaStatus>* xtreemfs_rwr_status_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_rwr_statusRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::ReplicaStatus>* sync_cb = new SyncCallback<xtreemfs::pbrpc::ReplicaStatus>();
                client_->sendRequest(address, 30001, 76,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ReplicaStatus(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_rwr_truncate(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_rwr_truncateRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 74,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_rwr_truncate_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_rwr_truncateRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 74,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_rwr_update(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_rwr_updateRequest* request,const char* data, uint32_t data_length,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                client_->sendRequest(address, 30001, 72,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_rwr_update_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_rwr_updateRequest* request, const char* data, uint32_t data_length) {
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 72,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_rwr_auth_state(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_rwr_auth_stateRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 79,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_rwr_auth_state_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_rwr_auth_stateRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 79,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_rwr_reset_complete(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_rwr_reset_completeRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 80,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_rwr_reset_complete_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_rwr_reset_completeRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 80,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_internal_get_gmax(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_internal_get_gmaxRequest* request,
                CallbackInterface<xtreemfs::pbrpc::InternalGmax> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 40,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::InternalGmax(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::InternalGmax>* xtreemfs_internal_get_gmax_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_internal_get_gmaxRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::InternalGmax>* sync_cb = new SyncCallback<xtreemfs::pbrpc::InternalGmax>();
                client_->sendRequest(address, 30001, 40,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::InternalGmax(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_internal_truncate(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::truncateRequest* request,
                CallbackInterface<xtreemfs::pbrpc::OSDWriteResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 41,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::OSDWriteResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::OSDWriteResponse>* xtreemfs_internal_truncate_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::truncateRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::OSDWriteResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::OSDWriteResponse>();
                client_->sendRequest(address, 30001, 41,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::OSDWriteResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_internal_get_file_size(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_internal_get_file_sizeRequest* request,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_internal_get_file_sizeResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 42,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_internal_get_file_sizeResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_internal_get_file_sizeResponse>* xtreemfs_internal_get_file_size_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_internal_get_file_sizeRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_internal_get_file_sizeResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_internal_get_file_sizeResponse>();
                client_->sendRequest(address, 30001, 42,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_internal_get_file_sizeResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_internal_read_local(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_internal_read_localRequest* request,
                CallbackInterface<xtreemfs::pbrpc::InternalReadLocalResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 43,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::InternalReadLocalResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::InternalReadLocalResponse>* xtreemfs_internal_read_local_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_internal_read_localRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::InternalReadLocalResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::InternalReadLocalResponse>();
                client_->sendRequest(address, 30001, 43,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::InternalReadLocalResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_internal_get_object_set(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_internal_get_object_setRequest* request,
                CallbackInterface<xtreemfs::pbrpc::ObjectList> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 44,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ObjectList(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::ObjectList>* xtreemfs_internal_get_object_set_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_internal_get_object_setRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::ObjectList>* sync_cb = new SyncCallback<xtreemfs::pbrpc::ObjectList>();
                client_->sendRequest(address, 30001, 44,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ObjectList(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_internal_get_fileid_list(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_internal_get_fileid_listResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 30001, 45,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_internal_get_fileid_listResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_internal_get_fileid_listResponse>* xtreemfs_internal_get_fileid_list_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_internal_get_fileid_listResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_internal_get_fileid_listResponse>();
                client_->sendRequest(address, 30001, 45,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_internal_get_fileid_listResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_lock_acquire(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::lockRequest* request,
                CallbackInterface<xtreemfs::pbrpc::Lock> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 50,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::Lock(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::Lock>* xtreemfs_lock_acquire_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::lockRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::Lock>* sync_cb = new SyncCallback<xtreemfs::pbrpc::Lock>();
                client_->sendRequest(address, 30001, 50,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::Lock(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_lock_check(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::lockRequest* request,
                CallbackInterface<xtreemfs::pbrpc::Lock> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 51,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::Lock(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::Lock>* xtreemfs_lock_check_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::lockRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::Lock>* sync_cb = new SyncCallback<xtreemfs::pbrpc::Lock>();
                client_->sendRequest(address, 30001, 51,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::Lock(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_lock_release(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::lockRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 52,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_lock_release_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::lockRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 52,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_ping(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_pingMesssage* request,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_pingMesssage> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 60,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_pingMesssage(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_pingMesssage>* xtreemfs_ping_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_pingMesssage* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_pingMesssage>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_pingMesssage>();
                client_->sendRequest(address, 30001, 60,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_pingMesssage(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_shutdown(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 30001, 70,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_shutdown_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 70,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_xloc_set_invalidate(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_xloc_set_invalidateRequest* request,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_xloc_set_invalidateResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 81,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_xloc_set_invalidateResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_xloc_set_invalidateResponse>* xtreemfs_xloc_set_invalidate_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_xloc_set_invalidateRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_xloc_set_invalidateResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_xloc_set_invalidateResponse>();
                client_->sendRequest(address, 30001, 81,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_xloc_set_invalidateResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_rwr_auth_state_invalidated(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_rwr_auth_stateRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 30001, 82,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_rwr_auth_state_invalidated_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_rwr_auth_stateRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 30001, 82,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

        private:
            Client* client_;
        };
    }
}
#endif //OSDSERVICECLIENT_H
