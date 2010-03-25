#ifndef _1624936007_H_
#define _1624936007_H_


#include "yield/concurrency.h"
#include "yidl.h"


namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
      class NettestInterface
      {
      public:
          const static uint32_t ONCRPC_PORT_DEFAULT = 32638;
        const static uint32_t ONCRPCG_PORT_DEFAULT = 32638;
        const static uint32_t ONCRPCS_PORT_DEFAULT = 32638;
        const static uint32_t ONCRPCU_PORT_DEFAULT = 32638;const static uint32_t TAG = 2010031316;

        virtual ~NettestInterface() { }

        uint32_t get_tag() const { return 2010031316; }



        virtual void nop() { }

        virtual void send_buffer( ::yidl::runtime::Buffer* data ) { }

        virtual void recv_buffer( uint32_t size, ::yidl::runtime::Buffer* data ) { }
      };


      // Use this macro in an implementation class to get all of the prototypes for the operations in NettestInterface
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_PROTOTYPES\
      virtual void nop();\
      virtual void send_buffer( ::yidl::runtime::Buffer* data );\
      virtual void recv_buffer( uint32_t size, ::yidl::runtime::Buffer* data );\


      #ifndef ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_RESPONSE_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_RESPONSE_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ORG_EXCEPTION_RESPONSE_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_RESPONSE_PARENT_CLASS ::yield::concurrency::ExceptionResponse
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
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_REQUEST_PARENT_CLASS ::yield::concurrency::Request
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
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_RESPONSE_PARENT_CLASS ::yield::concurrency::Response
      #endif
      #endif


      class NettestInterfaceMessages
      {
      public:
      // Request/response pair definitions for the operations in NettestInterface
        class nopRequest : public ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          nopRequest() { }
          virtual ~nopRequest() {  }


          virtual void respond()
          {
            respond( *new nopResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const nopRequest& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( nopRequest, 2010031317 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class nopResponse : public ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          nopResponse() { }
          virtual ~nopResponse() {  }

          bool operator==( const nopResponse& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( nopResponse, 2010031317 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class send_bufferRequest : public ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          send_bufferRequest()
            : data( NULL )
          { }

          send_bufferRequest( ::yidl::runtime::Buffer* data )
            : data( ::yidl::runtime::Object::inc_ref( data ) )
          { }

          virtual ~send_bufferRequest() { ::yidl::runtime::Buffer::dec_ref( data ); }

          ::yidl::runtime::Buffer* get_data() const { return data; }
          void set_data( ::yidl::runtime::Buffer* data ) { ::yidl::runtime::Buffer::dec_ref( this->data ); this->data = ::yidl::runtime::Object::inc_ref( data ); }


          virtual void respond()
          {
            respond( *new send_bufferResponse() );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const send_bufferRequest& other ) const
          {
            return data == other.data;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( send_bufferRequest, 2010031318 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            if ( data != NULL ) marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "data", 0 ), *data );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            if ( data != NULL ) unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ), *data );
          }

        protected:
          ::yidl::runtime::Buffer* data;
        };


        class send_bufferResponse : public ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          send_bufferResponse() { }
          virtual ~send_bufferResponse() {  }

          bool operator==( const send_bufferResponse& ) const { return true; }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( send_bufferResponse, 2010031318 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& ) const { }
          void unmarshal( ::yidl::runtime::Unmarshaller& ) { }
        };


        class recv_bufferRequest : public ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_REQUEST_PARENT_CLASS
        {
        public:
          recv_bufferRequest()
            : size( 0 ), data( NULL )
          { }

          recv_bufferRequest( uint32_t size, ::yidl::runtime::Buffer* data )
            : size( size ), data( ::yidl::runtime::Object::inc_ref( data ) )
          { }

          virtual ~recv_bufferRequest() { ::yidl::runtime::Buffer::dec_ref( data ); }

          uint32_t get_size() const { return size; }
          ::yidl::runtime::Buffer* get_data() const { return data; }
          void set_size( uint32_t size ) { this->size = size; }
          void set_data( ::yidl::runtime::Buffer* data ) { ::yidl::runtime::Buffer::dec_ref( this->data ); this->data = ::yidl::runtime::Object::inc_ref( data ); }


          virtual void respond( ::yidl::runtime::Buffer* data )
          {
            respond( *new recv_bufferResponse( data ) );
          }

          virtual void respond( ::yield::concurrency::Response& response )
          {
            Request::respond( response );
          }

          bool operator==( const recv_bufferRequest& other ) const
          {
            return size == other.size
                   &&
                   data == other.data;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( recv_bufferRequest, 2010031319 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "size", 0 ), size );
            if ( data != NULL ) marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "data", 0 ), *data );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            size = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "size", 0 ) );
            if ( data != NULL ) unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ), *data );
          }

        protected:
          uint32_t size;
          ::yidl::runtime::Buffer* data;
        };


        class recv_bufferResponse : public ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_RESPONSE_PARENT_CLASS
        {
        public:
          recv_bufferResponse()
            : data( NULL )
          { }

          recv_bufferResponse( ::yidl::runtime::Buffer* data )
            : data( ::yidl::runtime::Object::inc_ref( data ) )
          { }

          virtual ~recv_bufferResponse() { ::yidl::runtime::Buffer::dec_ref( data ); }

          ::yidl::runtime::Buffer* get_data() const { return data; }
          void set_data( ::yidl::runtime::Buffer* data ) { ::yidl::runtime::Buffer::dec_ref( this->data ); this->data = ::yidl::runtime::Object::inc_ref( data ); }

          bool operator==( const recv_bufferResponse& other ) const
          {
            return data == other.data;
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( recv_bufferResponse, 2010031319 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            if ( data != NULL ) marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "data", 0 ), *data );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            if ( data != NULL ) unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ), *data );
          }

        protected:
          ::yidl::runtime::Buffer* data;
        };
      };


      class NettestInterfaceMessageFactory
        : public ::yield::concurrency::MessageFactory,
          private NettestInterfaceMessages
      {
      public:
        // yield::concurrency::MessageFactory
        virtual ::yield::concurrency::Request* createRequest( uint32_t type_id )
        {
          switch ( type_id )
          {
            case 2010031317: return new nopRequest;
            case 2010031318: return new send_bufferRequest;
            case 2010031319: return new recv_bufferRequest;
            default: return NULL;
          }
        }

        virtual ::yield::concurrency::Request* createRequest( const char* type_name )
        {
          if ( strcmp( type_name, "nopRequest" ) == 0 ) return new nopRequest;
          else if ( strcmp( type_name, "send_bufferRequest" ) == 0 ) return new send_bufferRequest;
          else if ( strcmp( type_name, "recv_bufferRequest" ) == 0 ) return new recv_bufferRequest;
          else return NULL;
        }

        virtual ::yield::concurrency::Response* createResponse( uint32_t type_id )
        {
          switch ( type_id )
          {
            case 2010031317: return new nopResponse;
            case 2010031318: return new send_bufferResponse;
            case 2010031319: return new recv_bufferResponse;
            default: return NULL;
          }
        }

        virtual ::yield::concurrency::Response* createResponse( const char* type_name )
        {
          if ( strcmp( type_name, "nopResponse" ) == 0 ) return new nopResponse;
          else if ( strcmp( type_name, "send_bufferResponse" ) == 0 ) return new send_bufferResponse;
          else if ( strcmp( type_name, "recv_bufferResponse" ) == 0 ) return new recv_bufferResponse;
          else return NULL;
        }

      };


      class NettestInterfaceRequestHandler
        : public ::yield::concurrency::RequestHandler,
          protected NettestInterfaceMessages
      {
      public:
        NettestInterfaceRequestHandler()  // Subclasses must implement
          : _interface( NULL ) // all relevant handle*Request methods
        { }

        // Steals interface_ to allow for *new
        NettestInterfaceRequestHandler( NettestInterface& _interface )
          : _interface( &_interface )
        { }

        virtual ~NettestInterfaceRequestHandler()
        {
          delete _interface;
        }

        // yield::concurrency::EventHandler
        virtual const char* get_name() const
        {
          return "NettestInterface";
        }

        // yield::concurrency::RequestHandler

        virtual void handleRequest( ::yield::concurrency::Request& request )
        {
          // Switch on the request types that this interface handles, unwrap the corresponding requests and delegate to _interface
          switch ( request.get_type_id() )
          {
            case 2010031317UL: handlenopRequest( static_cast<nopRequest&>( request ) ); return;
            case 2010031318UL: handlesend_bufferRequest( static_cast<send_bufferRequest&>( request ) ); return;
            case 2010031319UL: handlerecv_bufferRequest( static_cast<recv_bufferRequest&>( request ) ); return;
            default: ::yield::concurrency::Request::dec_ref( request ); return;
          }
        }

      protected:
        virtual void handlenopRequest( nopRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->nop();
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }

          nopRequest::dec_ref( __request );
        }

        virtual void handlesend_bufferRequest( send_bufferRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->send_buffer( __request.get_data() );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }

          send_bufferRequest::dec_ref( __request );
        }

        virtual void handlerecv_bufferRequest( recv_bufferRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::Buffer* data( __request.get_data() );

              _interface->recv_buffer( __request.get_size(), data );

              __request.respond( data );
            }
            catch( ::yield::concurrency::ExceptionResponse* exception_response )
            {
              __request.respond( *exception_response );
            }
            catch ( ::yield::concurrency::ExceptionResponse& exception_response )
            {
              __request.respond( *exception_response.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::ExceptionResponse( exception ) ) );
            }
          }

          recv_bufferRequest::dec_ref( __request );
        }

      private:
        NettestInterface* _interface;
      };

      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EVENT_HANDLER_PROTOTYPES \
      virtual void handlenopRequest( nopRequest& __request );\
      virtual void handlesend_bufferRequest( send_bufferRequest& __request );\
      virtual void handlerecv_bufferRequest( recv_bufferRequest& __request );


      class NettestInterfaceRequestSender : public NettestInterface, private NettestInterfaceMessages
      {
      public:
        NettestInterfaceRequestSender() // Used when the request_target is a subclass
          : __request_target( NULL )
        { }

        NettestInterfaceRequestSender( ::yield::concurrency::EventTarget& request_target )
          : __request_target( &request_target )
        { }


        virtual void nop()
        {
          nopRequest* __request = new nopRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<nopResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<nopResponse> );
          __request->set_response_target( &__response_queue.get() );

          __request_target->send( *__request );

          ::yidl::runtime::auto_Object<nopResponse> __response = __response_queue->dequeue();
        }

        virtual void send_buffer( ::yidl::runtime::Buffer* data )
        {
          send_bufferRequest* __request = new send_bufferRequest( data );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<send_bufferResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<send_bufferResponse> );
          __request->set_response_target( &__response_queue.get() );

          __request_target->send( *__request );

          ::yidl::runtime::auto_Object<send_bufferResponse> __response = __response_queue->dequeue();
        }

        virtual void recv_buffer( uint32_t size, ::yidl::runtime::Buffer* data )
        {
          recv_bufferRequest* __request = new recv_bufferRequest( size, data );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<recv_bufferResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<recv_bufferResponse> );
          __request->set_response_target( &__response_queue.get() );

          __request_target->send( *__request );

          ::yidl::runtime::auto_Object<recv_bufferResponse> __response = __response_queue->dequeue();
          data = __response->get_data();
        }

        void set_request_target( ::yield::concurrency::EventTarget& request_target )
        {
          this->__request_target = &request_target;
        }

      private:
        // __request_target is not a counted reference, since that would create
        // a reference cycle when __request_target is a subclass of NettestInterfaceRequestSender
        ::yield::concurrency::EventTarget* __request_target;
      };
    };
  };
};
#endif
