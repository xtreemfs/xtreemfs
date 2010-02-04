#include "yield/concurrency.h"


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
  
      #ifndef ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_INTERFACE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_INTERFACE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_INTERFACE_PARENT_CLASS ORG_XTREEMFS_INTERFACE_PARENT_CLASS
      #elif defined( ORG_INTERFACE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_INTERFACE_PARENT_CLASS ORG_INTERFACE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_INTERFACE_PARENT_CLASS ::YIELD::concurrency::Interface
      #endif
      #endif
  
      #ifndef ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_REQUEST_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_REQUEST_PARENT_CLASS ORG_XTREEMFS_INTERFACES_REQUEST_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_REQUEST_PARENT_CLASS ORG_XTREEMFS_REQUEST_PARENT_CLASS
      #elif defined( ORG_REQUEST_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_REQUEST_PARENT_CLASS ORG_REQUEST_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_REQUEST_PARENT_CLASS ::YIELD::concurrency::Request
      #endif
      #endif
  
      #ifndef ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_RESPONSE_PARENT_CLASS ORG_XTREEMFS_RESPONSE_PARENT_CLASS
      #elif defined( ORG_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_RESPONSE_PARENT_CLASS ORG_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_RESPONSE_PARENT_CLASS ::YIELD::concurrency::Response
      #endif
      #endif
  
      #ifndef ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_EXCEPTION_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ::YIELD::concurrency::ExceptionResponse
      #endif
      #endif
  
  
  
      class NettestInterface : public ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_INTERFACE_PARENT_CLASS
      {
      public:
        NettestInterface() { }
        virtual ~NettestInterface() { }
  
  
        virtual void nop() { nop( static_cast<uint64_t>( -1 ) ); }
        virtual void nop( uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<nopRequest> __request( new nopRequest() ); ::YIELD::concurrency::auto_ResponseQueue<nopResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<nopResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<nopResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }
  
        virtual void send_buffer( ::yidl::runtime::auto_Buffer data ) { send_buffer( data, static_cast<uint64_t>( -1 ) ); }
        virtual void send_buffer( ::yidl::runtime::auto_Buffer data, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<send_bufferRequest> __request( new send_bufferRequest( data ) ); ::YIELD::concurrency::auto_ResponseQueue<send_bufferResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<send_bufferResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<send_bufferResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); }
  
        virtual void recv_buffer( uint32_t size, ::yidl::runtime::auto_Buffer data ) { recv_buffer( size, data, static_cast<uint64_t>( -1 ) ); }
        virtual void recv_buffer( uint32_t size, ::yidl::runtime::auto_Buffer data, uint64_t response_timeout_ns ) { ::yidl::runtime::auto_Object<recv_bufferRequest> __request( new recv_bufferRequest( size ) ); ::YIELD::concurrency::auto_ResponseQueue<recv_bufferResponse> __response_queue( new ::YIELD::concurrency::ResponseQueue<recv_bufferResponse> ); __request->set_response_target( __response_queue->incRef() ); send( __request->incRef() ); ::yidl::runtime::auto_Object<recv_bufferResponse> __response = response_timeout_ns == static_cast<uint64_t>( -1 ) ? __response_queue->dequeue() : __response_queue->timed_dequeue( response_timeout_ns ); data = __response->get_data().release(); }
  
  
        // Request/response pair definitions for the operations in NettestInterface
  
        class nopResponse : public ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          nopResponse() { }
          virtual ~nopResponse() { }
  
          bool operator==( const nopResponse& ) const { return true; }
  
          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( nopResponse, 2010012514 );
  
        };
  
        class nopRequest : public ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          nopRequest() { }
          virtual ~nopRequest() { }
  
          bool operator==( const nopRequest& ) const { return true; }
  
          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( nopRequest, 2010012514 );
          // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new nopResponse; }
  
        };
  
        class send_bufferResponse : public ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          send_bufferResponse() { }
          virtual ~send_bufferResponse() { }
  
          bool operator==( const send_bufferResponse& ) const { return true; }
  
          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( send_bufferResponse, 2010012515 );
  
        };
  
        class send_bufferRequest : public ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          send_bufferRequest() { }
          send_bufferRequest( ::yidl::runtime::auto_Buffer data ) : data( data ) { }
          virtual ~send_bufferRequest() { }
  
          void set_data( ::yidl::runtime::auto_Buffer data ) { this->data = data; }
          ::yidl::runtime::auto_Buffer get_data() const { return data; }
  
          bool operator==( const send_bufferRequest& other ) const { return *data == *other.data; }
  
          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( send_bufferRequest, 2010012515 );
  
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeBuffer( "data", 0, data ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readBuffer( "data", 0, data ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new send_bufferResponse; }
  
  
        protected:
          ::yidl::runtime::auto_Buffer data;
        };
  
        class recv_bufferResponse : public ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          recv_bufferResponse() { }
          recv_bufferResponse( ::yidl::runtime::auto_Buffer data ) : data( data ) { }
          virtual ~recv_bufferResponse() { }
  
          void set_data( ::yidl::runtime::auto_Buffer data ) { this->data = data; }
          ::yidl::runtime::auto_Buffer get_data() const { return data; }
  
          bool operator==( const recv_bufferResponse& other ) const { return *data == *other.data; }
  
          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( recv_bufferResponse, 2010012516 );
  
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeBuffer( "data", 0, data ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { unmarshaller.readBuffer( "data", 0, data ); }
  
        protected:
          ::yidl::runtime::auto_Buffer data;
        };
  
        class recv_bufferRequest : public ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          recv_bufferRequest() : size( 0 ) { }
          recv_bufferRequest( uint32_t size ) : size( size ) { }
          virtual ~recv_bufferRequest() { }
  
          void set_size( uint32_t size ) { this->size = size; }
          uint32_t get_size() const { return size; }
  
          bool operator==( const recv_bufferRequest& other ) const { return size == other.size; }
  
          // yidl::runtime::Object
          YIDL_RUNTIME_OBJECT_PROTOTYPES( recv_bufferRequest, 2010012516 );
  
          // yidl::Struct
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const { marshaller.writeUint32( "size", 0, size ); }
          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller ) { size = unmarshaller.readUint32( "size", 0 ); }  // YIELD::concurrency::Request
          virtual ::YIELD::concurrency::auto_Response createResponse() { return new recv_bufferResponse; }
  
  
        protected:
          uint32_t size;
        };
  
  
  
        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( NettestInterface, 2010012513 );
  
        // YIELD::concurrency::EventHandler
        virtual void handleEvent( ::YIELD::concurrency::Event& ev )
        {
          try
          {
            // Switch on the event types that this interface handles, unwrap the corresponding requests and delegate to impl
            switch ( ev.get_type_id() )
            {
              case 2010012514UL: handlenopRequest( static_cast<nopRequest&>( ev ) ); return;
              case 2010012515UL: handlesend_bufferRequest( static_cast<send_bufferRequest&>( ev ) ); return;
              case 2010012516UL: handlerecv_bufferRequest( static_cast<recv_bufferRequest&>( ev ) ); return;
              default: handleUnknownEvent( ev ); return;
            }
          }
          catch( ::YIELD::concurrency::ExceptionResponse* exception_response )
          {
            static_cast< ::YIELD::concurrency::Request& >( ev ).respond( *exception_response );
          }
          catch ( ::YIELD::concurrency::ExceptionResponse& exception_response )
          {
            static_cast< ::YIELD::concurrency::Request& >( ev ).respond( *exception_response.clone() );
          }
          catch ( ::YIELD::platform::Exception& exception )
          {
            static_cast< ::YIELD::concurrency::Request& >( ev ).respond( *( new ::YIELD::concurrency::ExceptionResponse( exception ) ) );
          }
  
          ::yidl::runtime::Object::decRef( ev );
        }
  
  
        // YIELD::concurrency::Interface
          virtual ::YIELD::concurrency::Request* checkRequest( Object& request )
          {
            switch ( request.get_type_id() )
            {
              case 2010012514: return static_cast<nopRequest*>( &request );
              case 2010012515: return static_cast<send_bufferRequest*>( &request );
              case 2010012516: return static_cast<recv_bufferRequest*>( &request );
              default: return NULL;
            }
          }
  
          virtual ::YIELD::concurrency::Response* checkResponse( Object& response )
          {
            switch ( response.get_type_id() )
            {
              case 2010012514: return static_cast<nopResponse*>( &response );
              case 2010012515: return static_cast<send_bufferResponse*>( &response );
              case 2010012516: return static_cast<recv_bufferResponse*>( &response );
              default: return NULL;
            }
          }
  
          virtual ::YIELD::concurrency::auto_Request createRequest( uint32_t tag )
          {
            switch ( tag )
            {
              case 2010012514: return new nopRequest;
              case 2010012515: return new send_bufferRequest;
              case 2010012516: return new recv_bufferRequest;
              default: return NULL;
            }
          }
  
          virtual ::YIELD::concurrency::auto_Response createResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 2010012514: return new nopResponse;
              case 2010012515: return new send_bufferResponse;
              case 2010012516: return new recv_bufferResponse;
              default: return NULL;
            }
          }
  
          virtual ::YIELD::concurrency::auto_ExceptionResponse createExceptionResponse( uint32_t ) { return NULL; }
  
  
      protected:
        virtual void handlenopRequest( nopRequest& req ) { ::yidl::runtime::auto_Object<nopResponse> resp( new nopResponse ); _nop(); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlesend_bufferRequest( send_bufferRequest& req ) { ::yidl::runtime::auto_Object<send_bufferResponse> resp( new send_bufferResponse ); _send_buffer( req.get_data() ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
        virtual void handlerecv_bufferRequest( recv_bufferRequest& req ) { ::yidl::runtime::auto_Object<recv_bufferResponse> resp( new recv_bufferResponse ); ::yidl::runtime::auto_Buffer data; _recv_buffer( req.get_size(), data ); resp->set_data( data ); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
  
      virtual void _nop() { }
        virtual void _send_buffer( ::yidl::runtime::auto_Buffer  ) { }
        virtual void _recv_buffer( uint32_t, ::yidl::runtime::auto_Buffer  ) { }
      };
  
      // Use this macro in an implementation class to get all of the prototypes for the operations in NettestInterface
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_PROTOTYPES \
      virtual void _nop();\
      virtual void _send_buffer( ::yidl::runtime::auto_Buffer data );\
      virtual void _recv_buffer( uint32_t size, ::yidl::runtime::auto_Buffer data );
  
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_HANDLEEVENT_PROTOTYPES \
      virtual void handlenopRequest( nopRequest& req );\
      virtual void handlesend_bufferRequest( send_bufferRequest& req );\
      virtual void handlerecv_bufferRequest( recv_bufferRequest& req );
  
    };
  
  
  
  };
  
  

};
