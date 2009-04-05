// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_PROXY_H
#define ORG_XTREEMFS_CLIENT_PROXY_H

#include "yield.h"

#include "org/xtreemfs/client/proxy_exception_event.h"
#include "org/xtreemfs/interfaces/exceptions.h"
#include "org/xtreemfs/interfaces/types.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class Proxy : public YIELD::EventHandler, public YIELD::SharedObject
      {
      public:
        const static uint8_t PROXY_DEFAULT_RECONNECT_TRIES_MAX = 3;
        const static uint32_t PROXY_FLAG_PRINT_OPERATIONS = 1;
        const static uint32_t PROXY_DEFAULT_FLAGS = 0;
        const static uint64_t PROXY_DEFAULT_OPERATION_TIMEOUT_MS = static_cast<uint64_t>( -1 );

        virtual ~Proxy();

        uint32_t get_flags() const { return flags; }
        void set_flags( uint32_t flags ) { this->flags = flags; }
        uint8_t get_reconnect_tries_max() const { return reconnect_tries_max; }
        void set_reconnect_tries_max( uint8_t reconnect_tries_max ) { this->reconnect_tries_max = reconnect_tries_max; }
        uint64_t get_operation_timeout_ms() const { return operation_timeout_ms; }
        void set_operation_timeout_ms( uint64_t operation_timeout_ms ) { this->operation_timeout_ms = operation_timeout_ms; }

        // EventHandler
        virtual void handleEvent( YIELD::Event& ev );

      protected:
        Proxy( const YIELD::URI& uri, uint16_t default_oncrpc_port );
        Proxy( const YIELD::URI& uri, const YIELD::Path& pkcs12_file_path, const std::string& pkcs12_passphrase, uint16_t default_oncrpcs_port );

        virtual bool getCurrentUserCredentials( org::xtreemfs::interfaces::UserCredentials& out_user_credentials ) const { return false; }

        YIELD::SerializableFactories serializable_factories;

      private:
        void init();

        YIELD::URI uri;

        uint32_t flags;
        uint8_t reconnect_tries_max;
        uint64_t operation_timeout_ms;
        SSL_CTX* ssl_ctx;

        YIELD::FDEventQueue fd_event_queue;
        unsigned int peer_ip; YIELD::SocketConnection* conn;

        uint8_t reconnect( uint8_t reconnect_tries_left ); // Returns the new value of reconnect_tries_left
        void throwExceptionEvent( YIELD::ExceptionEvent* );
      };
    };
  };
};

#endif

