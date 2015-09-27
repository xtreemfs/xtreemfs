//automatically generated from Scheduler.proto at Sun Sep 27 13:50:13 CEST 2015
//(c) 2015. See LICENSE file for details.

#ifndef SCHEDULERSERVICECLIENT_H
#define SCHEDULERSERVICECLIENT_H

#include <stdint.h>
#include "pbrpc/RPC.pb.h"
#include "rpc/client.h"
#include "rpc/sync_callback.h"
#include "rpc/callback_interface.h"
#include "xtreemfs/OSD.pb.h"
#include "xtreemfs/MRC.pb.h"
#include "include/Common.pb.h"
#include "xtreemfs/Scheduler.pb.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/DIR.pb.h"


namespace xtreemfs {
namespace pbrpc {
        using ::xtreemfs::rpc::Client;
        using ::xtreemfs::rpc::CallbackInterface;
        using ::xtreemfs::rpc::SyncCallback;

        class SchedulerServiceClient {

        public:
            SchedulerServiceClient(Client* client) : client_(client) {
            }

            virtual ~SchedulerServiceClient() {
            }

            void scheduleReservation(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::reservation* request,
                CallbackInterface<xtreemfs::pbrpc::osdSet> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 40001, 101,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::osdSet(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::osdSet>* scheduleReservation_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::reservation* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::osdSet>* sync_cb = new SyncCallback<xtreemfs::pbrpc::osdSet>();
                client_->sendRequest(address, 40001, 101,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::osdSet(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void removeReservation(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::volumeIdentifier* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 40001, 102,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* removeReservation_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::volumeIdentifier* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 40001, 102,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void getSchedule(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::volumeIdentifier* request,
                CallbackInterface<xtreemfs::pbrpc::osdSet> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 40001, 103,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::osdSet(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::osdSet>* getSchedule_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::volumeIdentifier* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::osdSet>* sync_cb = new SyncCallback<xtreemfs::pbrpc::osdSet>();
                client_->sendRequest(address, 40001, 103,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::osdSet(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void getVolumes(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::osdIdentifier* request,
                CallbackInterface<xtreemfs::pbrpc::volumeSet> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 40001, 104,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::volumeSet(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::volumeSet>* getVolumes_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::osdIdentifier* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::volumeSet>* sync_cb = new SyncCallback<xtreemfs::pbrpc::volumeSet>();
                client_->sendRequest(address, 40001, 104,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::volumeSet(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void getAllVolumes(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::reservationSet> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 40001, 105,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::reservationSet(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::reservationSet>* getAllVolumes_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::reservationSet>* sync_cb = new SyncCallback<xtreemfs::pbrpc::reservationSet>();
                client_->sendRequest(address, 40001, 105,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::reservationSet(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void getFreeResources(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::freeResourcesResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 40001, 106,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::freeResourcesResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::freeResourcesResponse>* getFreeResources_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::freeResourcesResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::freeResourcesResponse>();
                client_->sendRequest(address, 40001, 106,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::freeResourcesResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

        private:
            Client* client_;
        };
    }
}
#endif //SCHEDULERSERVICECLIENT_H
