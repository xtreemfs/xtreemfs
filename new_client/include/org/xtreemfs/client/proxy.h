#ifndef ORG_XTREEMFS_CLIENT_PROXY_H
#define ORG_XTREEMFS_CLIENT_PROXY_H

#include "yield/ipc.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class Proxy : public YIELD::EventHandler
      {
      public:
        virtual ~Proxy();

        YIELD::SerializableFactories& getSerializableFactories() { return serializable_factories; }

      protected:
        Proxy(); // Must be a default constructor because xInterface inherits from Proxy
        void init( const YIELD::URI&, uint8_t reconnect_tries_max ); // Called by subclasses to bypass EventHandler

        YIELD::SerializableFactories serializable_factories;

        // EventHandler
        virtual void handleEvent( YIELD::Event& ev );

      private:
        YIELD::URI* uri;
        uint8_t reconnect_tries_max;

        YIELD::FDEventQueue fd_event_queue;
        unsigned int peer_ip; YIELD::SocketConnection* conn;

        // Helper methods shared between the old and new protocols
        void sendProtocolRequest( YIELD::ProtocolRequest&, uint64_t timeout_ms );
        uint8_t reconnect( uint64_t timeout_ms, uint8_t reconnect_tries_left ); // Returns the new value of reconnect_tries_left
        void throwExceptionEvent( YIELD::ExceptionEvent* );
      };


      typedef YIELD::ExceptionEvent ProxyException;
    };
  };
};

#endif

