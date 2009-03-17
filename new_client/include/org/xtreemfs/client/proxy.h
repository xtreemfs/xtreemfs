#ifndef ORG_XTREEMFS_CLIENT_PROXY_H
#define ORG_XTREEMFS_CLIENT_PROXY_H

#include "yield/ipc.h"

#include "org/xtreemfs/client/proxy_exception_event.h"
#include "org/xtreemfs/interfaces/exceptions.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class Proxy : public YIELD::EventHandler
      {
      public:
        const static uint8_t PROXY_DEFAULT_RECONNECT_TRIES_MAX = 3;
        const static uint32_t PROXY_FLAG_PRINT_OPERATIONS = 1;
        const static uint32_t PROXY_DEFAULT_FLAGS = 0;


        virtual ~Proxy();

        YIELD::Request* createRequest( const char* type_name ) { return static_cast<YIELD::Request*>( serializable_factories.createSerializable( type_name ) ); }

      protected:
        Proxy(); // Must be a default constructor because xInterface inherits from Proxy
        void init( const YIELD::URI&, uint8_t reconnect_tries_max = PROXY_DEFAULT_RECONNECT_TRIES_MAX, uint32_t flags = PROXY_DEFAULT_FLAGS ); // Called by subclasses to bypass EventHandler

        YIELD::URI* uri;
        uint8_t reconnect_tries_max;
        uint32_t flags;

        YIELD::SerializableFactories serializable_factories;

        // EventHandler
        virtual void handleEvent( YIELD::Event& ev );

      private:
        YIELD::FDEventQueue fd_event_queue;
        unsigned int peer_ip; YIELD::SocketConnection* conn;

        void sendProtocolRequest( YIELD::ProtocolRequest&, uint64_t timeout_ms );
        uint8_t reconnect( uint64_t timeout_ms, uint8_t reconnect_tries_left ); // Returns the new value of reconnect_tries_left
        void throwExceptionEvent( YIELD::ExceptionEvent* );
      };
    };
  };
};

#endif

