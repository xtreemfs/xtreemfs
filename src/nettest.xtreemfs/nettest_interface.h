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
              default: return NULL;
            }
          }
  
          virtual ::YIELD::concurrency::Response* checkResponse( Object& response )
          {
            switch ( response.get_type_id() )
            {
              case 2010012514: return static_cast<nopResponse*>( &response );
              default: return NULL;
            }
          }
  
          virtual ::YIELD::concurrency::auto_Request createRequest( uint32_t tag )
          {
            switch ( tag )
            {
              case 2010012514: return new nopRequest;
              default: return NULL;
            }
          }
  
          virtual ::YIELD::concurrency::auto_Response createResponse( uint32_t tag )
          {
            switch ( tag )
            {
              case 2010012514: return new nopResponse;
              default: return NULL;
            }
          }
  
          virtual ::YIELD::concurrency::auto_ExceptionResponse createExceptionResponse( uint32_t ) { return NULL; }
  
  
      protected:
        virtual void handlenopRequest( nopRequest& req ) { ::yidl::runtime::auto_Object<nopResponse> resp( new nopResponse ); _nop(); req.respond( *resp.release() ); ::yidl::runtime::Object::decRef( req ); }
  
      virtual void _nop() { }
      };
  
      // Use this macro in an implementation class to get all of the prototypes for the operations in NettestInterface
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_PROTOTYPES \
      virtual void _nop();
  
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_HANDLEEVENT_PROTOTYPES \
      virtual void handlenopRequest( nopRequest& req );
  
    };
  
  
  
  };
  
  

};
