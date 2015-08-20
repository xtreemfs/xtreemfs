//automatically generated from DIR.proto at Thu Aug 20 09:17:20 CEST 2015
//(c) 2015. See LICENSE file for details.

#ifndef DIRSERVICECLIENT_H
#define DIRSERVICECLIENT_H

#include <stdint.h>
#include "pbrpc/RPC.pb.h"
#include "rpc/client.h"
#include "rpc/sync_callback.h"
#include "rpc/callback_interface.h"
#include "include/Common.pb.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/DIR.pb.h"


namespace xtreemfs {
namespace pbrpc {
        using ::xtreemfs::rpc::Client;
        using ::xtreemfs::rpc::CallbackInterface;
        using ::xtreemfs::rpc::SyncCallback;

        class DIRServiceClient {

        public:
            DIRServiceClient(Client* client) : client_(client) {
            }

            virtual ~DIRServiceClient() {
            }

            void xtreemfs_address_mappings_get(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::addressMappingGetRequest* request,
                CallbackInterface<xtreemfs::pbrpc::AddressMappingSet> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 10001, 1,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::AddressMappingSet(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::AddressMappingSet>* xtreemfs_address_mappings_get_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::addressMappingGetRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::AddressMappingSet>* sync_cb = new SyncCallback<xtreemfs::pbrpc::AddressMappingSet>();
                client_->sendRequest(address, 10001, 1,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::AddressMappingSet(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_address_mappings_remove(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::addressMappingGetRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 10001, 2,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_address_mappings_remove_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::addressMappingGetRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 10001, 2,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_address_mappings_set(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::AddressMappingSet* request,
                CallbackInterface<xtreemfs::pbrpc::addressMappingSetResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 10001, 3,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::addressMappingSetResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::addressMappingSetResponse>* xtreemfs_address_mappings_set_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::AddressMappingSet* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::addressMappingSetResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::addressMappingSetResponse>();
                client_->sendRequest(address, 10001, 3,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::addressMappingSetResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_discover_dir(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::DirService> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 10001, 4,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::DirService(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::DirService>* xtreemfs_discover_dir_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::DirService>* sync_cb = new SyncCallback<xtreemfs::pbrpc::DirService>();
                client_->sendRequest(address, 10001, 4,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::DirService(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_global_time_s_get(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::globalTimeSGetResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 10001, 5,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::globalTimeSGetResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::globalTimeSGetResponse>* xtreemfs_global_time_s_get_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::globalTimeSGetResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::globalTimeSGetResponse>();
                client_->sendRequest(address, 10001, 5,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::globalTimeSGetResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_service_deregister(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::serviceDeregisterRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 10001, 6,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_service_deregister_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::serviceDeregisterRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 10001, 6,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_service_get_by_name(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::serviceGetByNameRequest* request,
                CallbackInterface<xtreemfs::pbrpc::ServiceSet> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 10001, 7,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ServiceSet(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::ServiceSet>* xtreemfs_service_get_by_name_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::serviceGetByNameRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::ServiceSet>* sync_cb = new SyncCallback<xtreemfs::pbrpc::ServiceSet>();
                client_->sendRequest(address, 10001, 7,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ServiceSet(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_service_get_by_type(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::serviceGetByTypeRequest* request,
                CallbackInterface<xtreemfs::pbrpc::ServiceSet> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 10001, 8,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ServiceSet(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::ServiceSet>* xtreemfs_service_get_by_type_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::serviceGetByTypeRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::ServiceSet>* sync_cb = new SyncCallback<xtreemfs::pbrpc::ServiceSet>();
                client_->sendRequest(address, 10001, 8,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ServiceSet(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_service_get_by_uuid(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::serviceGetByUUIDRequest* request,
                CallbackInterface<xtreemfs::pbrpc::ServiceSet> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 10001, 9,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ServiceSet(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::ServiceSet>* xtreemfs_service_get_by_uuid_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::serviceGetByUUIDRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::ServiceSet>* sync_cb = new SyncCallback<xtreemfs::pbrpc::ServiceSet>();
                client_->sendRequest(address, 10001, 9,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::ServiceSet(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_service_offline(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::serviceGetByUUIDRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 10001, 10,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_service_offline_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::serviceGetByUUIDRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 10001, 10,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_service_register(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::serviceRegisterRequest* request,
                CallbackInterface<xtreemfs::pbrpc::serviceRegisterResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 10001, 11,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::serviceRegisterResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::serviceRegisterResponse>* xtreemfs_service_register_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::serviceRegisterRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::serviceRegisterResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::serviceRegisterResponse>();
                client_->sendRequest(address, 10001, 11,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::serviceRegisterResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_checkpoint(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 10001, 20,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_checkpoint_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 10001, 20,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_shutdown(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 10001, 21,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_shutdown_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 10001, 21,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_configuration_get(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::configurationGetRequest* request,
                CallbackInterface<xtreemfs::pbrpc::Configuration> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 10001, 22,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::Configuration(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::Configuration>* xtreemfs_configuration_get_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::configurationGetRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::Configuration>* sync_cb = new SyncCallback<xtreemfs::pbrpc::Configuration>();
                client_->sendRequest(address, 10001, 22,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::Configuration(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_configuration_set(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::Configuration* request,
                CallbackInterface<xtreemfs::pbrpc::configurationSetResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 10001, 23,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::configurationSetResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::configurationSetResponse>* xtreemfs_configuration_set_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::Configuration* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::configurationSetResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::configurationSetResponse>();
                client_->sendRequest(address, 10001, 23,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::configurationSetResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_vivaldi_client_update(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::VivaldiCoordinates* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 10001, 24,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_vivaldi_client_update_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::VivaldiCoordinates* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 10001, 24,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

        private:
            Client* client_;
        };
    }
}
#endif //DIRSERVICECLIENT_H
