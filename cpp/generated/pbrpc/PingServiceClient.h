//automatically generated from Ping.proto at Wed Dec 02 12:26:59 CET 2015
//(c) 2015. See LICENSE file for details.

#ifndef PINGSERVICECLIENT_H
#define PINGSERVICECLIENT_H

#include <stdint.h>
#include "pbrpc/RPC.pb.h"
#include "rpc/client.h"
#include "rpc/sync_callback.h"
#include "rpc/callback_interface.h"
#include "pbrpc/Ping.pb.h"


namespace xtreemfs {
namespace pbrpc {
        using ::xtreemfs::rpc::Client;
        using ::xtreemfs::rpc::CallbackInterface;
        using ::xtreemfs::rpc::SyncCallback;

        class PingServiceClient {

        public:
            PingServiceClient(Client* client) : client_(client) {
            }

            virtual ~PingServiceClient() {
            }

            void doPing(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::PingRequest* request,const char* data, uint32_t data_length,
                CallbackInterface<xtreemfs::pbrpc::PingResponse> *callback, void *context = NULL) {
                client_->sendRequest(address, 1, 1,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::PingResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::PingResponse>* doPing_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::PingRequest* request, const char* data, uint32_t data_length) {
                SyncCallback<xtreemfs::pbrpc::PingResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::PingResponse>();
                client_->sendRequest(address, 1, 1,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::PingResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void emptyPing(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::Ping_emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::Ping_emptyRequest* request = NULL;
                client_->sendRequest(address, 1, 2,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::Ping_emptyResponse>* emptyPing_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::Ping_emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::Ping_emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::Ping_emptyResponse>();
                client_->sendRequest(address, 1, 2,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

        private:
            Client* client_;
        };
    }
}
#endif //PINGSERVICECLIENT_H
