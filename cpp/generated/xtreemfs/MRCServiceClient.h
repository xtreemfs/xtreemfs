//automatically generated from MRC.proto at Wed Dec 02 14:27:49 CET 2015
//(c) 2015. See LICENSE file for details.

#ifndef MRCSERVICECLIENT_H
#define MRCSERVICECLIENT_H

#include <stdint.h>
#include "pbrpc/RPC.pb.h"
#include "rpc/client.h"
#include "rpc/sync_callback.h"
#include "rpc/callback_interface.h"
#include "xtreemfs/MRC.pb.h"
#include "include/Common.pb.h"
#include "xtreemfs/GlobalTypes.pb.h"
#include "xtreemfs/DIR.pb.h"


namespace xtreemfs {
namespace pbrpc {
        using ::xtreemfs::rpc::Client;
        using ::xtreemfs::rpc::CallbackInterface;
        using ::xtreemfs::rpc::SyncCallback;

        class MRCServiceClient {

        public:
            MRCServiceClient(Client* client) : client_(client) {
            }

            virtual ~MRCServiceClient() {
            }

            void fsetattr(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::fsetattrRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 2,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* fsetattr_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::fsetattrRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 20001, 2,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void ftruncate(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::XCap* request,
                CallbackInterface<xtreemfs::pbrpc::XCap> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 3,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::XCap(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::XCap>* ftruncate_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::XCap* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::XCap>* sync_cb = new SyncCallback<xtreemfs::pbrpc::XCap>();
                client_->sendRequest(address, 20001, 3,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::XCap(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void getattr(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::getattrRequest* request,
                CallbackInterface<xtreemfs::pbrpc::getattrResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 4,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::getattrResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::getattrResponse>* getattr_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::getattrRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::getattrResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::getattrResponse>();
                client_->sendRequest(address, 20001, 4,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::getattrResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void getxattr(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::getxattrRequest* request,
                CallbackInterface<xtreemfs::pbrpc::getxattrResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 5,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::getxattrResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::getxattrResponse>* getxattr_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::getxattrRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::getxattrResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::getxattrResponse>();
                client_->sendRequest(address, 20001, 5,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::getxattrResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void link(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::linkRequest* request,
                CallbackInterface<xtreemfs::pbrpc::timestampResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 6,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::timestampResponse>* link_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::linkRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::timestampResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::timestampResponse>();
                client_->sendRequest(address, 20001, 6,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void listxattr(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::listxattrRequest* request,
                CallbackInterface<xtreemfs::pbrpc::listxattrResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 7,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::listxattrResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::listxattrResponse>* listxattr_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::listxattrRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::listxattrResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::listxattrResponse>();
                client_->sendRequest(address, 20001, 7,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::listxattrResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void mkdir(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::mkdirRequest* request,
                CallbackInterface<xtreemfs::pbrpc::timestampResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 8,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::timestampResponse>* mkdir_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::mkdirRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::timestampResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::timestampResponse>();
                client_->sendRequest(address, 20001, 8,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void open(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::openRequest* request,
                CallbackInterface<xtreemfs::pbrpc::openResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 9,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::openResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::openResponse>* open_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::openRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::openResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::openResponse>();
                client_->sendRequest(address, 20001, 9,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::openResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void readdir(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::readdirRequest* request,
                CallbackInterface<xtreemfs::pbrpc::DirectoryEntries> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 10,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::DirectoryEntries(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::DirectoryEntries>* readdir_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::readdirRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::DirectoryEntries>* sync_cb = new SyncCallback<xtreemfs::pbrpc::DirectoryEntries>();
                client_->sendRequest(address, 20001, 10,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::DirectoryEntries(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void readlink(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::readlinkRequest* request,
                CallbackInterface<xtreemfs::pbrpc::readlinkResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 11,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::readlinkResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::readlinkResponse>* readlink_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::readlinkRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::readlinkResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::readlinkResponse>();
                client_->sendRequest(address, 20001, 11,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::readlinkResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void removexattr(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::removexattrRequest* request,
                CallbackInterface<xtreemfs::pbrpc::timestampResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 12,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::timestampResponse>* removexattr_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::removexattrRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::timestampResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::timestampResponse>();
                client_->sendRequest(address, 20001, 12,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void rename(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::renameRequest* request,
                CallbackInterface<xtreemfs::pbrpc::renameResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 13,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::renameResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::renameResponse>* rename_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::renameRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::renameResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::renameResponse>();
                client_->sendRequest(address, 20001, 13,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::renameResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void rmdir(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::rmdirRequest* request,
                CallbackInterface<xtreemfs::pbrpc::timestampResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 14,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::timestampResponse>* rmdir_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::rmdirRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::timestampResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::timestampResponse>();
                client_->sendRequest(address, 20001, 14,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void setattr(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::setattrRequest* request,
                CallbackInterface<xtreemfs::pbrpc::timestampResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 15,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::timestampResponse>* setattr_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::setattrRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::timestampResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::timestampResponse>();
                client_->sendRequest(address, 20001, 15,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void setxattr(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::setxattrRequest* request,
                CallbackInterface<xtreemfs::pbrpc::timestampResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 16,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::timestampResponse>* setxattr_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::setxattrRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::timestampResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::timestampResponse>();
                client_->sendRequest(address, 20001, 16,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void statvfs(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::statvfsRequest* request,
                CallbackInterface<xtreemfs::pbrpc::StatVFS> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 17,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::StatVFS(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::StatVFS>* statvfs_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::statvfsRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::StatVFS>* sync_cb = new SyncCallback<xtreemfs::pbrpc::StatVFS>();
                client_->sendRequest(address, 20001, 17,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::StatVFS(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void symlink(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::symlinkRequest* request,
                CallbackInterface<xtreemfs::pbrpc::timestampResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 18,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::timestampResponse>* symlink_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::symlinkRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::timestampResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::timestampResponse>();
                client_->sendRequest(address, 20001, 18,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void unlink(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::unlinkRequest* request,
                CallbackInterface<xtreemfs::pbrpc::unlinkResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 19,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::unlinkResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::unlinkResponse>* unlink_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::unlinkRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::unlinkResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::unlinkResponse>();
                client_->sendRequest(address, 20001, 19,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::unlinkResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void access(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::accessRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 20,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* access_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::accessRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 20001, 20,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_checkpoint(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 20001, 30,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_checkpoint_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 20001, 30,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_check_file_exists(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_check_file_existsRequest* request,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_check_file_existsResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 31,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_check_file_existsResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_check_file_existsResponse>* xtreemfs_check_file_exists_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_check_file_existsRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_check_file_existsResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_check_file_existsResponse>();
                client_->sendRequest(address, 20001, 31,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_check_file_existsResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_clear_vouchers(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_clear_vouchersRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 52,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_clear_vouchers_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_clear_vouchersRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 20001, 52,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_dump_database(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_dump_restore_databaseRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 32,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_dump_database_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_dump_restore_databaseRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 20001, 32,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_get_suitable_osds(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_get_suitable_osdsRequest* request,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_get_suitable_osdsResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 33,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_get_suitable_osdsResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_get_suitable_osdsResponse>* xtreemfs_get_suitable_osds_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_get_suitable_osdsRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_get_suitable_osdsResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_get_suitable_osdsResponse>();
                client_->sendRequest(address, 20001, 33,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_get_suitable_osdsResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_internal_debug(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::stringMessage* request,
                CallbackInterface<xtreemfs::pbrpc::stringMessage> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 34,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::stringMessage(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::stringMessage>* xtreemfs_internal_debug_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::stringMessage* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::stringMessage>* sync_cb = new SyncCallback<xtreemfs::pbrpc::stringMessage>();
                client_->sendRequest(address, 20001, 34,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::stringMessage(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_listdir(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_listdirRequest* request,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_listdirResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 35,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_listdirResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_listdirResponse>* xtreemfs_listdir_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_listdirRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_listdirResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_listdirResponse>();
                client_->sendRequest(address, 20001, 35,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_listdirResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_lsvol(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::Volumes> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 20001, 36,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::Volumes(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::Volumes>* xtreemfs_lsvol_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::Volumes>* sync_cb = new SyncCallback<xtreemfs::pbrpc::Volumes>();
                client_->sendRequest(address, 20001, 36,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::Volumes(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_mkvol(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::Volume* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 47,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_mkvol_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::Volume* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 20001, 47,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_renew_capability(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::XCap* request,
                CallbackInterface<xtreemfs::pbrpc::XCap> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 37,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::XCap(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::XCap>* xtreemfs_renew_capability_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::XCap* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::XCap>* sync_cb = new SyncCallback<xtreemfs::pbrpc::XCap>();
                client_->sendRequest(address, 20001, 37,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::XCap(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_renew_capability_and_voucher(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_renew_capabilityRequest* request,
                CallbackInterface<xtreemfs::pbrpc::XCap> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 53,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::XCap(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::XCap>* xtreemfs_renew_capability_and_voucher_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_renew_capabilityRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::XCap>* sync_cb = new SyncCallback<xtreemfs::pbrpc::XCap>();
                client_->sendRequest(address, 20001, 53,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::XCap(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_replication_to_master(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                client_->sendRequest(address, 20001, 38,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_replication_to_master_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 20001, 38,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_replica_add(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_replica_addRequest* request,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_replica_addResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 39,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_replica_addResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_replica_addResponse>* xtreemfs_replica_add_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_replica_addRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_replica_addResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_replica_addResponse>();
                client_->sendRequest(address, 20001, 39,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_replica_addResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_replica_list(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_replica_listRequest* request,
                CallbackInterface<xtreemfs::pbrpc::Replicas> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 40,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::Replicas(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::Replicas>* xtreemfs_replica_list_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_replica_listRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::Replicas>* sync_cb = new SyncCallback<xtreemfs::pbrpc::Replicas>();
                client_->sendRequest(address, 20001, 40,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::Replicas(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_replica_remove(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_replica_removeRequest* request,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_replica_removeResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 41,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_replica_removeResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_replica_removeResponse>* xtreemfs_replica_remove_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_replica_removeRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_replica_removeResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_replica_removeResponse>();
                client_->sendRequest(address, 20001, 41,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_replica_removeResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_restore_database(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_dump_restore_databaseRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 42,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_restore_database_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_dump_restore_databaseRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 20001, 42,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_restore_file(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_restore_fileRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 43,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_restore_file_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_restore_fileRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 20001, 43,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_rmvol(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_rmvolRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 44,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_rmvol_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_rmvolRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 20001, 44,
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
                client_->sendRequest(address, 20001, 45,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_shutdown_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds) {
                const char* data = NULL; uint32_t data_length = 0;
                xtreemfs::pbrpc::emptyRequest* request = NULL;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 20001, 45,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_update_file_size(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_update_file_sizeRequest* request,
                CallbackInterface<xtreemfs::pbrpc::timestampResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 46,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::timestampResponse>* xtreemfs_update_file_size_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_update_file_sizeRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::timestampResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::timestampResponse>();
                client_->sendRequest(address, 20001, 46,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::timestampResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_set_replica_update_policy(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_set_replica_update_policyRequest* request,
                CallbackInterface<xtreemfs::pbrpc::emptyResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 48,
                     creds, auth, request, data, data_length, NULL,
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::emptyResponse>* xtreemfs_set_replica_update_policy_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_set_replica_update_policyRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::emptyResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::emptyResponse>();
                client_->sendRequest(address, 20001, 48,
                     creds, auth, request, data, data_length, NULL,
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_set_read_only_xattr(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_set_read_only_xattrRequest* request,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_set_read_only_xattrResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 49,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_set_read_only_xattrResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_set_read_only_xattrResponse>* xtreemfs_set_read_only_xattr_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_set_read_only_xattrRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_set_read_only_xattrResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_set_read_only_xattrResponse>();
                client_->sendRequest(address, 20001, 49,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_set_read_only_xattrResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_get_file_credentials(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_get_file_credentialsRequest* request,
                CallbackInterface<xtreemfs::pbrpc::FileCredentials> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 50,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::FileCredentials(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::FileCredentials>* xtreemfs_get_file_credentials_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_get_file_credentialsRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::FileCredentials>* sync_cb = new SyncCallback<xtreemfs::pbrpc::FileCredentials>();
                client_->sendRequest(address, 20001, 50,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::FileCredentials(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_get_xlocset(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_get_xlocsetRequest* request,
                CallbackInterface<xtreemfs::pbrpc::XLocSet> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 51,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::XLocSet(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::XLocSet>* xtreemfs_get_xlocset_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_get_xlocsetRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::XLocSet>* sync_cb = new SyncCallback<xtreemfs::pbrpc::XLocSet>();
                client_->sendRequest(address, 20001, 51,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::XLocSet(),
                     NULL, sync_cb);
                return sync_cb;
            }

            void xtreemfs_reselect_osds(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds,
                const xtreemfs::pbrpc::xtreemfs_reselect_osdsRequest* request,
                CallbackInterface<xtreemfs::pbrpc::xtreemfs_reselect_osdsResponse> *callback, void *context = NULL) {
                const char* data = NULL; uint32_t data_length = 0;
                client_->sendRequest(address, 20001, 54,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_reselect_osdsResponse(),
                     context, callback);
            }

            SyncCallback<xtreemfs::pbrpc::xtreemfs_reselect_osdsResponse>* xtreemfs_reselect_osds_sync(const std::string &address,
                const xtreemfs::pbrpc::Auth& auth,
                const xtreemfs::pbrpc::UserCredentials &creds
                , const xtreemfs::pbrpc::xtreemfs_reselect_osdsRequest* request) {
                const char* data = NULL; uint32_t data_length = 0;
                SyncCallback<xtreemfs::pbrpc::xtreemfs_reselect_osdsResponse>* sync_cb = new SyncCallback<xtreemfs::pbrpc::xtreemfs_reselect_osdsResponse>();
                client_->sendRequest(address, 20001, 54,
                     creds, auth, request, data, data_length, new xtreemfs::pbrpc::xtreemfs_reselect_osdsResponse(),
                     NULL, sync_cb);
                return sync_cb;
            }

        private:
            Client* client_;
        };
    }
}
#endif //MRCSERVICECLIENT_H
