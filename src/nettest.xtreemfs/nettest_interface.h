#ifndef _501049470_H_
#define _501049470_H_


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
      const static uint32_t TAG = 2010031316;

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


      #ifndef ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_PARENT_CLASS
      #if defined( ORG_XTREEMFS_INTERFACES_EXCEPTION_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_PARENT_CLASS ORG_XTREEMFS_INTERFACES_EXCEPTION_PARENT_CLASS
      #elif defined( ORG_XTREEMFS_EXCEPTION_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_PARENT_CLASS ORG_XTREEMFS_EXCEPTION_PARENT_CLASS
      #elif defined( ORG_EXCEPTION_PARENT_CLASS )
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_PARENT_CLASS ORG_EXCEPTION_PARENT_CLASS
      #else
      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_EXCEPTION_PARENT_CLASS ::yield::concurrency::Exception
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
            return get_data() == other.get_data();
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( send_bufferRequest, 2010031318 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            if ( get_data() != NULL ) marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "data", 0 ), *get_data() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            if ( data != NULL ) unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ), *data ); else data = unmarshaller.read_buffer( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ) );
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
            return get_size() == other.get_size()
                   &&
                   get_data() == other.get_data();
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( recv_bufferRequest, 2010031319 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "size", 0 ), get_size() );
            if ( get_data() != NULL ) marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "data", 0 ), *get_data() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            size = unmarshaller.read_uint32( ::yidl::runtime::Unmarshaller::StringLiteralKey( "size", 0 ) );
            if ( data != NULL ) unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ), *data ); else data = unmarshaller.read_buffer( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ) );
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
            return get_data() == other.get_data();
          }

          // yidl::runtime::RTTIObject
          YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( recv_bufferResponse, 2010031319 );

          // yidl::runtime::MarshallableObject
          void marshal( ::yidl::runtime::Marshaller& marshaller ) const
          {
            if ( get_data() != NULL ) marshaller.write( ::yidl::runtime::Marshaller::StringLiteralKey( "data", 0 ), *get_data() );
          }

          void unmarshal( ::yidl::runtime::Unmarshaller& unmarshaller )
          {
            if ( data != NULL ) unmarshaller.read( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ), *data ); else data = unmarshaller.read_buffer( ::yidl::runtime::Unmarshaller::StringLiteralKey( "data", 0 ) );
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

        virtual ::yield::concurrency::Request*
        createRequest
        (
          const char* type_name,
          size_t type_name_len
        )
        {
          if ( type_name_len == 10 && strncmp( type_name, "nopRequest", 10 ) == 0 ) return new nopRequest;
          else if ( type_name_len == 18 && strncmp( type_name, "send_bufferRequest", 18 ) == 0 ) return new send_bufferRequest;
          else if ( type_name_len == 18 && strncmp( type_name, "recv_bufferRequest", 18 ) == 0 ) return new recv_bufferRequest;
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

        virtual ::yield::concurrency::Response*
        createResponse
        (
          const char* type_name,
          size_t type_name_len
        )
        {
          if ( type_name_len == 11 && strncmp( type_name, "nopResponse", 11 ) == 0 ) return new nopResponse;
          else if ( type_name_len == 19 && strncmp( type_name, "send_bufferResponse", 19 ) == 0 ) return new send_bufferResponse;
          else if ( type_name_len == 19 && strncmp( type_name, "recv_bufferResponse", 19 ) == 0 ) return new recv_bufferResponse;
          else return NULL;
        }


        // yidl::runtime::MarshallableObjectFactory

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

        // yidl::runtime::RTTIObject
        virtual const char* get_type_name() const
        {
          return "NettestInterface";
        }

        // yield::concurrency::RequestHandler
        virtual void handle( ::yield::concurrency::Request& request )
        {
          // Switch on the request types that this interface handles, unwrap the corresponding requests and delegate to _interface
          switch ( request.get_type_id() )
          {
            case 2010031317UL: handle( static_cast<nopRequest&>( request ) ); return;
            case 2010031318UL: handle( static_cast<send_bufferRequest&>( request ) ); return;
            case 2010031319UL: handle( static_cast<recv_bufferRequest&>( request ) ); return;
          }
        }

      protected:

        virtual void handle( nopRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->nop();
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( send_bufferRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              _interface->send_buffer( __request.get_data() );
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

        virtual void handle( recv_bufferRequest& __request )
        {
          if ( _interface != NULL )
          {
            try
            {
              ::yidl::runtime::Buffer* data( __request.get_data() );

              _interface->recv_buffer( __request.get_size(), data );

              __request.respond( data );
            }
            catch( ::yield::concurrency::Exception* exception )
            {
              __request.respond( *exception );
            }
            catch ( ::yield::concurrency::Exception& exception )
            {
              __request.respond( exception.clone() );
            }
            catch ( ::yield::platform::Exception& exception )
            {
              __request.respond( *( new ::yield::concurrency::Exception( exception ) ) );
            }
          }
        }

      private:
        NettestInterface* _interface;
      };

      #define ORG_XTREEMFS_INTERFACES_NETTESTINTERFACE_REQUEST_HANDLER_PROTOTYPES \
      virtual void handle( nopRequest& __request );\
      virtual void handle( send_bufferRequest& __request );\
      virtual void handle( recv_bufferRequest& __request );


      class NettestInterfaceProxy
        : public NettestInterface,
          public ::yield::concurrency::RequestHandler,
          private NettestInterfaceMessages
      {
      public:
        NettestInterfaceProxy( ::yield::concurrency::EventHandler& request_handler )
          : __request_handler( request_handler )
        { }

        ~NettestInterfaceProxy()
        {
          ::yield::concurrency::EventHandler::dec_ref( __request_handler );
        }

        // yidl::runtime::RTTIObject
        virtual const char* get_type_name() const
        {
          return "NettestInterfaceProxy";
        }

        // yield::concurrency::RequestHandler
        virtual void handle( ::yield::concurrency::Request& request )
        {
          __request_handler.handle( request );
        }

        // NettestInterface
        virtual void nop()
        {
          nopRequest* __request = new nopRequest;

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<nopResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<nopResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<nopResponse> __response = __response_queue->dequeue();
        }

        virtual void send_buffer( ::yidl::runtime::Buffer* data )
        {
          send_bufferRequest* __request = new send_bufferRequest( data );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<send_bufferResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<send_bufferResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<send_bufferResponse> __response = __response_queue->dequeue();
        }

        virtual void recv_buffer( uint32_t size, ::yidl::runtime::Buffer* data )
        {
          recv_bufferRequest* __request = new recv_bufferRequest( size, data );

          ::yidl::runtime::auto_Object< ::yield::concurrency::ResponseQueue<recv_bufferResponse> >
            __response_queue( new ::yield::concurrency::ResponseQueue<recv_bufferResponse> );
          __request->set_response_handler( *__response_queue );

          handle( *__request );

          ::yidl::runtime::auto_Object<recv_bufferResponse> __response = __response_queue->dequeue();
          data = __response->get_data();
        }

      private:
        // __request_handler is not a counted reference, since that would create
        // a reference cycle when __request_handler is a subclass of NettestInterfaceProxy
        ::yield::concurrency::EventHandler& __request_handler;
      };};
  };
};
#endif
