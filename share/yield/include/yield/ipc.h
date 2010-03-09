// Copyright (c) 2010 Minor Gordon
// With original implementations and ideas contributed by Felix Hupfeld
// All rights reserved
// 
// This source file is part of the Yield project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the Yield project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#ifndef _YIELD_IPC_H_
#define _YIELD_IPC_H_

#include "yield/concurrency.h"

#ifdef YIELD_IPC_HAVE_OPENSSL
#include <openssl/ssl.h>
#endif


struct UriUriStructA;
struct yajl_gen_t;
typedef struct yajl_gen_t* yajl_gen;


#define YIELD_RFC822_HEADERS_STACK_BUFFER_LENGTH 128
#define YIELD_RFC822_HEADERS_STACK_IOVECS_LENGTH 32


namespace yield
{
  namespace ipc
  {
    class HTTPRequest;
    class HTTPResponse;
    class ONCRPCRequest;
    class ONCRPCResponse;
    class SocketFactory;
    class SSLContext;
    class URI;

    using std::multimap;

    using yidl::runtime::Buffer;
    using yidl::runtime::Buffers;
    using yidl::runtime::MarshallableObject;
    using yidl::runtime::MarshallableObjectFactory;
    using yidl::runtime::Marshaller;
    using yidl::runtime::Unmarshaller;
    using yidl::runtime::XDRMarshaller;
    using yidl::runtime::XDRUnmarshaller;

    using yield::concurrency::Event;
    using yield::concurrency::EventHandler;
    using yield::concurrency::EventFactory;
    using yield::concurrency::EventTarget;
    using yield::concurrency::ExceptionResponse;
    using yield::concurrency::Request;
    using yield::concurrency::Response;

    using yield::platform::IOQueue;
    using yield::platform::Log;    
    using yield::platform::Socket;
    using yield::platform::SocketAddress;
    using yield::platform::TCPSocket;
    using yield::platform::UDPSocket;
    using yield::platform::Time;

    

    class SocketPeer
    {
    public:
      const static uint32_t FLAG_TRACE_NETWORK_IO = 1;
      const static uint32_t FLAG_TRACE_OPERATIONS = 2;
      const static uint32_t FLAGS_DEFAULT = 0;

      uint32_t get_flags() const { return flags; }

    protected:
      SocketPeer
      (
        uint32_t flags,
        IOQueue& io_queue, // Steals this reference
        Log* log
      );

      virtual ~SocketPeer();

      // Two helpers for subclass factory methods
      static IOQueue& createIOQueue();
      static SocketAddress& createSocketAddress( const URI& absolute_uri );      

      bool has_flag( uint32_t flag ) { return ( flags & flag ) == flag; }
      IOQueue& get_io_queue() const { return io_queue; }
      Log* get_log() const { return log; }

    private:
      uint32_t flags;
      IOQueue& io_queue;
      Log* log;
    };


    template <class RequestType, class ResponseType>
    class SocketClient : public EventHandler, public SocketPeer
    {
    public:
      const static uint16_t CONCURRENCY_LEVEL_DEFAULT = 4;
      const static uint64_t OPERATION_TIMEOUT_DEFAULT = 5 * Time::NS_IN_S;
      const static uint16_t RECONNECT_TRIES_MAX_DEFAULT = 2;

      SocketAddress& get_peername() const { return peername; }
      const Time& get_operation_timeout() const { return operation_timeout; }
      uint16_t get_reconnect_tries_max() const { return reconnect_tries_max; }

      // yidl::runtime::Object
      SocketClient<RequestType, ResponseType>& inc_ref() 
      {
        return Object::inc_ref( *this );
      }

      // yield::concurrency::EventHandler
      virtual void handleEvent( Event& );

    protected:
      SocketClient
      (
        uint16_t concurrency_level, // e.g. the # of simultaneous conns for TCP
        uint32_t flags,
        IOQueue& io_queue, // Steals this reference
        Log* log,
        const Time& operation_timeout,
        SocketAddress& peername, // Steals this reference
        uint16_t reconnect_tries_max,
        SocketFactory& socket_factory // Steals this reference
      );

      virtual ~SocketClient();

      virtual ResponseType& createResponse( RequestType& request ) = 0;

    private:
      uint16_t concurrency_level;
      Time operation_timeout;
      SocketAddress& peername;
      uint16_t reconnect_tries_max;
      yield::concurrency::SynchronizedSTLQueue<Socket*> sockets;
      SocketFactory& socket_factory;

    private:
      class Connection;
    };


    template <class RequestType, class ResponseType>
    class SocketServer 
      : public yidl::runtime::Object,
        public SocketPeer,
        public TCPSocket::AIOAcceptCallback,
        public UDPSocket::AIORecvFromCallback
    {
    public:
      // yidl::runtime::Object
      SocketServer<RequestType, ResponseType>& inc_ref()
      {
        return Object::inc_ref( *this ); 
      }

    protected:
      SocketServer
      ( 
        uint32_t flags,
        IOQueue& io_queue, // Steals this reference
        TCPSocket& listen_tcp_socket, // Steals this reference
        Log* log, 
        EventTarget& request_target // Steals this reference
      );

      SocketServer
      ( 
        uint32_t flags,
        IOQueue& io_queue,
        Log* log, 
        EventTarget& request_target, 
        UDPSocket& udp_socket 
      );

      virtual ~SocketServer();

      // Subclasses must implement this
      virtual RequestType* createRequest() const = 0;

      EventTarget& get_request_target() const { return request_target; }

      // Subclasses can override this to e.g. unwrap a request
      // and send its body (as in ONC-RPC and JSON-RPC)
      virtual void sendRequest( RequestType& request );

    private:
      // TCPSocket::AIOAcceptCallback
      void onAcceptCompletion( TCPSocket& accepted_tcp_socket, void* );
      void onAcceptError( uint32_t error_code, void* );

      // UDPSocket::AIORecvFromCallback
      void onRecvFromCompletion( Buffer& buffer, SocketAddress& peername, void* );
      void onRecvFromError( uint32_t, void* );

    private:
      class TCPConnection;
      class UDPConnection;

    private:
      TCPSocket* listen_tcp_socket;
      EventTarget& request_target;
      UDPSocket* udp_socket;
    };


    class RPCMessage
    {
    public:
      MarshallableObject& get_body() const;

    protected:
      // Incoming
      RPCMessage( MarshallableObjectFactory& );

      // Outgoing
      RPCMessage( MarshallableObject& body ); // Steals this reference

      virtual ~RPCMessage();

      MarshallableObjectFactory& get_marshallable_object_factory() const;

      void marshal_body
      ( 
        const char* key, 
        uint32_t tag, 
        Marshaller& marshaller
      ) const;

      bool unmarshal_new_Request_body
      ( 
        const char* key, 
        uint32_t tag,
        uint32_t type_id,
        Unmarshaller& unmarshaller
      );

      bool unmarshal_new_Response_body
      ( 
        const char* key, 
        uint32_t tag,
        uint32_t type_id,
        Unmarshaller& unmarshaller
      );

      bool
      unmarshal_new_ExceptionResponse_body
      ( 
        const char* key, 
        uint32_t tag,
        uint32_t type_id, 
        Unmarshaller& unmarshaller
      );

      void set_body( yidl::runtime::MarshallableObject* body );

    private:
      MarshallableObject* body;
      MarshallableObjectFactory* marshallable_object_factory;
    };


    class HTTPClient : public SocketClient<HTTPRequest, HTTPResponse>
    {
    public:
      static HTTPClient&
      create
      (
        const URI& absolute_uri,
        uint16_t concurrency_level = CONCURRENCY_LEVEL_DEFAULT,
        uint32_t flags = FLAGS_DEFAULT,
        Log* log = NULL,
        const Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT,
        uint16_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT,
        SSLContext* ssl_context = NULL // Steals this reference, to allow *new
      );

      static HTTPResponse& GET( const URI& absolute_uri, Log* log = NULL );

      static
      HTTPResponse&
      PUT
      (
        const URI& absolute_uri,
        Buffer& body, // Steals this reference
        Log* log = NULL
      );

      static
      HTTPResponse&
      PUT
      (
        const URI& absolute_uri,
        const yield::platform::Path& body_file_path,
        Log* log = NULL
      );

      // yidl::runtime::Object
      HTTPClient& inc_ref() { return Object::inc_ref( *this ); }

    private:
      HTTPClient
      (
        uint16_t concurrency_level,
        uint32_t flags,
        IOQueue& io_queue,
        Log* log,
        const Time& operation_timeout,
        SocketAddress& peername,
        uint16_t reconnect_tries_max,
        SocketFactory& socket_factory
      );

      virtual ~HTTPClient() { }

      // SocketClient
      HTTPResponse& createResponse( HTTPRequest& http_request );

      static
      HTTPResponse&
      sendHTTPRequest
      (
        const char* method,
        const URI& uri,
        Buffer* body,
        Log* log
      );
    };


    class RFC822Headers
    {
    public:
      RFC822Headers( uint8_t reserve_iovecs_count = 0 );
      virtual ~RFC822Headers();

      ssize_t deserialize( Buffer& );

      char* get_header
      (
        const char* header_name,
        const char* default_value=""
      );

      char* operator[]( const char* header_name )
      {
        return get_header( header_name );
      }

      // set_header( ... ): char* copies into a buffer, const char* does not
      void set_header( const char* header_name, const char* header_value );
      void set_header( const char* header_name, char* header_value );
      void set_header( char* header_name, char* header_value );

      void set_header
      (
        const string& header_name,
        const string& header_value
      );

      Buffers& serialize();

    protected:
      const struct iovec* get_iovecs() const
      {
        return heap_iovecs != NULL ? heap_iovecs : stack_iovecs;
      }

      uint8_t get_iovecs_filled() const { return iovecs_filled; }

      void set_iovec( uint8_t iovec_i, const char* data, size_t len );
      void set_next_iovec( char* data, size_t len ); // Copies data
      void set_next_iovec( const char* data, size_t len ); // No copy
      void set_next_iovec( const struct iovec& out_iovec );

    private:
      enum
      {
        DESERIALIZING_LEADING_WHITESPACE,
        DESERIALIZING_HEADER_NAME,
        DESERIALIZING_HEADER_NAME_VALUE_SEPARATOR,
        DESERIALIZING_HEADER_VALUE,
        DESERIALIZING_HEADER_VALUE_TERMINATOR,
        DESERIALIZING_TRAILING_CRLF,
        DESERIALIZE_DONE
      } deserialize_state;

      char stack_buf[YIELD_RFC822_HEADERS_STACK_BUFFER_LENGTH],
           *heap_buf,
           *buf_p;
      size_t heap_buflen;

      struct iovec stack_iovecs[YIELD_RFC822_HEADERS_STACK_IOVECS_LENGTH],
                   *heap_iovecs;
      uint8_t iovecs_filled;

      inline void advanceBufferPointer()
      {
        buf_p++;

        if ( heap_buf == NULL )
        {
          if ( buf_p - stack_buf < YIELD_RFC822_HEADERS_STACK_BUFFER_LENGTH )
            return;
          else
            allocateHeapBuffer();
        }
        else if ( static_cast<size_t>( buf_p - heap_buf ) < heap_buflen )
          return;
        else
          allocateHeapBuffer();
      }

      void allocateHeapBuffer();
    };


    class HTTPMessage : public RFC822Headers
    {
    public:
      Buffer* get_body() const { return body; }
      uint8_t get_http_version() const { return http_version; }

    protected:
      HTTPMessage( uint8_t reserve_iovecs_count = 0, Buffer* body = NULL );
      HTTPMessage( const HTTPMessage& ) { DebugBreak(); } // Prevent copying
      virtual ~HTTPMessage() { }

      enum
      {
        DESERIALIZE_DONE,
        DESERIALIZING_BODY,
        DESERIALIZING_METHOD,
        DESERIALIZING_HEADERS,
        DESERIALIZING_HTTP_VERSION,
        DESERIALIZING_REASON,
        DESERIALIZING_STATUS_CODE,
        DESERIALIZING_URI
      } deserialize_state;

      virtual ssize_t deserialize( Buffer& );
      virtual Buffers& serialize();
      void set_http_version( uint8_t http_version );

    private:
      Buffer* body;
      uint8_t http_version;
    };


    class HTTPRequest : public Request, public HTTPMessage
    {
    public:
      // Incoming
      HTTPRequest();

      // Outgoing
      HTTPRequest 
      (
        const char* method,
        const char* relative_uri,
        const char* host
      );

      HTTPRequest
      (
        const char* method,
        const char* relative_uri,
        const char* host,
        Buffer& body // Steals this reference
      );

      HTTPRequest
      (
        const char* method,
        const URI& absolute_uri
      );

      HTTPRequest
      (
        const char* method,
        const URI& absolute_uri,
        Buffer& body // Steals this reference
      );

      ssize_t deserialize( Buffer& );
      const char* get_method() const { return method; }
      const char* get_uri() const { return uri; }
      void respond( HTTPResponse& http_response ); // Steals this reference
      void respond( uint16_t status_code );
      void respond( uint16_t status_code, Buffer& body ); // Steals this ref
      void respond( ExceptionResponse& exception_response ); // Steals tihs ref
      Buffers& serialize();

      // yidl::runtime::RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( HTTPRequest, 205 );

      // yidl::runtime::MarshallableObject
      void marshal( Marshaller& ) const { }
      void unmarshal( Unmarshaller& ) { }

    protected:
      virtual ~HTTPRequest();

    private:
      HTTPRequest( const HTTPRequest& other ) // Prevent copying
        : HTTPMessage( other )
      { }

      void init( const char*, const char*, const char* );

    private:
      char method[16];
      char* uri; size_t uri_len;
    };


    class HTTPResponse : public Response, public HTTPMessage
    {
    public:
      // Incoming
      HTTPResponse(); 

      // Outgoing
      HTTPResponse( uint16_t status_code );
      HTTPResponse( uint16_t status_code, Buffer& body ); // Steals this ref

      ssize_t deserialize( Buffer& );
      uint16_t get_status_code() const { return status_code; }
      Buffers& serialize();

      // yidl::runtime::RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( HTTPResponse, 206 );

      // yidl::runtime::MarshallableObject
      void marshal( Marshaller& ) const { }
      void unmarshal( Unmarshaller& ) { }

    protected:
      virtual ~HTTPResponse() { }

    private:
      HTTPResponse( const HTTPResponse& other ) // Prevent copying
        : HTTPMessage( other )
      {
        DebugBreak();
      }

    private:
      union { char status_code_str[4]; uint16_t status_code; };
    };


    class HTTPServer : public SocketServer<HTTPRequest, HTTPResponse>
    {
    public:
      static HTTPServer&
      create
      (
        const URI& absolute_uri,
        EventTarget& http_request_target, // Steals this reference
        uint32_t flags = FLAGS_DEFAULT,
        Log* log = NULL,
        SSLContext* ssl_context = NULL // Steals this reference, to allow *new
      );

      // yidl::runtime::Object
      HTTPServer& inc_ref() { return Object::inc_ref( *this ); }

    private:
      HTTPServer
      (
        uint32_t flags,
        EventTarget& http_request_target,
        IOQueue& io_queue,
        TCPSocket& listen_tcp_socket,
        Log* log
      );

      // SocketServer
      HTTPRequest* createRequest() const { return new HTTPRequest; }

    private:
      class Connection;
    };


    class JSONMarshaller : public Marshaller
    {
    public:
      JSONMarshaller( bool write_empty_strings = true );
      virtual ~JSONMarshaller();

      Buffer& get_buffer() const { return *buffer; }

      // Marshaller
      YIDL_MARSHALLER_PROTOTYPES;

    protected:
      JSONMarshaller
      (
        JSONMarshaller& parent_json_marshaller,
        const char* root_key
      );

      virtual void write_key( const char* );
      virtual void write( const yidl::runtime::Map* ); // NULL = empty
      virtual void write( const yidl::runtime::Sequence* );
      virtual void write( const yidl::runtime::MarshallableObject* );

    private:
      void flushYAJLBuffer();

    private:
      Buffer* buffer;
      bool in_map;
      const char* root_key;
      bool write_empty_strings;
      yajl_gen writer;

    };


    class JSONUnmarshaller : public Unmarshaller
    {
    public:
      JSONUnmarshaller( Buffer& buffer );
      virtual ~JSONUnmarshaller();

      // Unmarshaller
      YIDL_UNMARSHALLER_PROTOTYPES;

    private:
      class JSONObject;
      class JSONValue;

      JSONUnmarshaller( const char* root_key, JSONValue& root_json_value );

      const char* root_key;
      JSONValue *root_json_value, *next_json_value;

      void read( yidl::runtime::Map& );
      void read( yidl::runtime::MarshallableObject& );
      void read( yidl::runtime::Sequence& );      
      JSONValue* readJSONValue( const char* key );
    };


    class ONCRPCClient : public SocketClient<ONCRPCRequest, ONCRPCResponse>
    {
    public:
      static ONCRPCClient&
      create
      (
        const URI& absolute_uri,
        MarshallableObjectFactory&, // Steals this reference, to allow *new
        uint32_t prog,
        uint32_t vers,
        uint16_t concurrency_level = CONCURRENCY_LEVEL_DEFAULT,
        uint32_t flags = FLAGS_DEFAULT,
        Log* log = NULL,
        const Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT,
        uint16_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT,
        SSLContext* ssl_context = NULL // Steals this reference, to allow *new
      );

      // yidl::runtime::Object
      ONCRPCClient& inc_ref() { return Object::inc_ref( *this ); }

      // yield::concurrency::EventHandler
      virtual void handleEvent( Event& );

    protected:
      ONCRPCClient
      (
        uint16_t concurrency_level,
        uint32_t flags,
        IOQueue& io_queue,
        Log* log,
        MarshallableObjectFactory& marshallable_object_factory,
        const Time& operation_timeout,
        SocketAddress& peername,
        uint32_t prog,
        uint16_t reconnect_tries_max,
        SocketFactory& socket_factory,
        uint32_t vers
      );

      virtual ~ONCRPCClient() { }

      virtual ONCRPCRequest& createONCRPCRequest( MarshallableObject& body );

      EventFactory* get_event_factory() const;
      MarshallableObjectFactory& get_marshallable_object_factory() const;
      uint32_t get_prog() const { return prog; }
      uint32_t get_vers() const { return vers; }
    
      // SocketClient
      virtual ONCRPCResponse& createResponse( ONCRPCRequest& oncrpc_request );

    private:
      class ONCRPCResponseTarget;

    private:
      MarshallableObjectFactory& marshallable_object_factory;
      uint32_t prog, vers;
    };


    class ONCRPCGarbageArgumentsError : public ExceptionResponse
    {
    public:
      ONCRPCGarbageArgumentsError()
        : ExceptionResponse( 4, "ONC-RPC: garbage arguments" )
      { }
    };


    template <class ONCRPCMessageType> // CRTP
    class ONCRPCMessage : public RPCMessage
    {
    public:
      const static uint32_t AUTH_NONE = 0;

      virtual ssize_t deserialize( Buffer& );
      MarshallableObject* get_verf() const { return verf; }
      uint32_t get_xid() const { return xid; }
      Buffers& serialize();

    protected:
      // Incoming
      ONCRPCMessage
      ( 
        MarshallableObjectFactory& marshallable_object_factory,
        uint32_t xid
      );

      // Outgoing
      ONCRPCMessage
      ( 
        MarshallableObject& body, // Steals this reference
        MarshallableObject* verf, // Steals this reference
        uint32_t xid 
      );

      virtual ~ONCRPCMessage();

      enum
      {
        DESERIALIZING_RECORD_FRAGMENT_MARKER,
        DESERIALIZING_RECORD_FRAGMENT,
        DESERIALIZING_LONG_RECORD_FRAGMENT,
        DESERIALIZE_DONE
      } deserialize_state;

      ssize_t deserializeRecordFragmentMarker( Buffer& );
      ssize_t deserializeRecordFragment( Buffer& );
      ssize_t deserializeLongRecordFragment( Buffer& );

      void marshal_opaque_auth( MarshallableObject*, Marshaller& ) const;      
      void marshal_verf( Marshaller& ) const;
      MarshallableObject* unmarshal_opaque_auth( Unmarshaller& );
      void unmarshal_verf( Unmarshaller& );

      // yidl::runtime::Marshallable
      void marshal( Marshaller& marshaller ) const;
      void unmarshal( Unmarshaller& unmarshaller );

    private:
      uint32_t record_fragment_length;
      Buffer* record_fragment_buffer;
      MarshallableObject* verf;
      uint32_t xid;
    };


    class ONCRPCMessageRejectedError : public ExceptionResponse
    {
    public:
      ONCRPCMessageRejectedError()
        : ExceptionResponse( 1, "ONC-RPC: message rejected" )
      { }
    };


    class ONCRPCProcedureUnavailableError : public ExceptionResponse
    {
    public:
      ONCRPCProcedureUnavailableError()
        : ExceptionResponse( 3, "ONC-RPC: procedure unavailable" )
      { }
    };


    class ONCRPCProgramMismatchError : public ExceptionResponse
    {
    public:
      ONCRPCProgramMismatchError()
        : ExceptionResponse( 2, "ONC-RPC: program mismatch" )
      { }
    };


    class ONCRPCProgramUnavailableError : public ExceptionResponse
    {
    public:
      ONCRPCProgramUnavailableError()
        : ExceptionResponse( 1, "ONC-RPC: program unavailable" )
      { }
    };


    class ONCRPCRequest : public Request, public ONCRPCMessage<ONCRPCRequest>
    {
    public:
      // Incoming
      ONCRPCRequest( MarshallableObjectFactory& );

      // Outgoing
      ONCRPCRequest
      (
        MarshallableObject& body, // Steals this reference
        uint32_t proc,
        uint32_t prog,
        uint32_t vers,
        MarshallableObject* cred = NULL, // = AUTH_NONE, steals this reference
        MarshallableObject* verf = NULL // = AUTH_NONE, steals this reference
      );

      virtual ~ONCRPCRequest();

      MarshallableObject* get_cred() const { return cred; }
      uint32_t get_prog() const { return prog; }
      uint32_t get_proc() const { return proc; }      
      uint32_t get_vers() const { return vers; }
      void respond( ONCRPCResponse& oncrpc_response ); // Steals this reference
      void respond( MarshallableObject& response_body ); // Steals this ref
      void respond( ExceptionResponse& exception_response ); // Steals this ref

      // yidl::runtime::RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ONCRPCRequest, 213 );

      // yidl::runtime::MarshallableObject
      virtual void marshal( Marshaller& marshaller ) const;
      virtual void unmarshal( Unmarshaller& unmarshaller );

    private:      
      MarshallableObject* cred;
      uint32_t prog, proc;
      uint32_t vers;
    };


    class ONCRPCResponse 
      : public Response, 
        public ONCRPCMessage<ONCRPCResponse>
    {
    public:
      // Incoming
      ONCRPCResponse
      ( 
        uint32_t default_body_type_id,
        MarshallableObjectFactory& marshallable_object_factory,
        uint32_t xid
      );

      // Outgoing
      ONCRPCResponse
      ( 
        MarshallableObject& body, // Steals this reference
        uint32_t xid,
        MarshallableObject* verf = NULL // = AUTH_NONE, steals this reference
      );
      
      virtual ~ONCRPCResponse() { }

      // yidl::runtime::RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ONCRPCResponse, 208 );

      // yidl::runtime::MarshallableObject
      virtual void marshal( Marshaller& ) const;
      virtual void unmarshal( Unmarshaller& );

    private:
      uint32_t default_body_type_id;
    };


    class ONCRPCServer : public SocketServer<ONCRPCRequest, ONCRPCResponse>
    {
    public:
      static ONCRPCServer&
      create
      (
        const URI& absolute_uri,
        MarshallableObjectFactory& marshallable_object_factory, // Steals this
        EventTarget& request_target, // Steals this reference, to allow *new
        uint32_t flags = FLAGS_DEFAULT,
        Log* log = NULL,
        bool send_oncrpc_requests = false, // false = send only bodies
        SSLContext* ssl_context = NULL // Steals this reference, to allow *new
      );

      // yidl::runtime::Object
      ONCRPCServer& inc_ref() { return Object::inc_ref( *this ); }

    protected:
      ONCRPCServer
      ( 
        uint32_t flags,
        IOQueue& io_queue,
        TCPSocket& listen_tcp_socket,
        Log* log,
        MarshallableObjectFactory& marshallable_object_factory,
        EventTarget& request_target,
        bool send_oncrpc_requests
      );

      ONCRPCServer
      ( 
        uint32_t flags,
        IOQueue& io_queue,
        Log* log,
        MarshallableObjectFactory& marshallable_object_factory,
        EventTarget& request_target,
        bool send_oncrpc_requests,
        UDPSocket& udp_socket  
      );

      EventFactory* get_event_factory() const;
      MarshallableObjectFactory& get_marshallable_object_factory() const;

      // SocketServer
      virtual ONCRPCRequest* createRequest() const
      { 
        return new ONCRPCRequest( marshallable_object_factory );
      }

      virtual void sendRequest( ONCRPCRequest& oncrpc_request );


    private:
      class ONCRPCResponseTarget;

    private:
      MarshallableObjectFactory& marshallable_object_factory;
      bool send_oncrpc_requests;
    };


    class ONCRPCSystemError : public ExceptionResponse
    {
    public:
      ONCRPCSystemError()
        : ExceptionResponse( 5, "ONC-RPC: system error" )
      { }
    };


    class SocketFactory : public yidl::runtime::Object
    {
    public:
      virtual Socket* createSocket() = 0;

      // yidl::runtime::Object
      SocketFactory& inc_ref() { return Object::inc_ref( *this ); }
    };


    class SSLContext : public yidl::runtime::Object
    {
    public:
#ifdef YIELD_IPC_HAVE_OPENSSL
      static SSLContext&
      create
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method = SSLv23_client_method()
      );

      static SSLContext&
      create
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method,
        const yield::platform::Path& pem_certificate_file_path,
        const yield::platform::Path& pem_private_key_file_path,
        const string& pem_private_key_passphrase
      );

      static SSLContext&
      create
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method,
        const string& pem_certificate_str,
        const string& pem_private_key_str,
        const string& pem_private_key_passphrase
      );

      static SSLContext&
      create
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method,
        const yield::platform::Path& pkcs12_file_path,
        const string& pkcs12_passphrase
      );
#else
      static SSLContext& create();
#endif

#ifdef YIELD_IPC_HAVE_OPENSSL
       operator SSL_CTX*() const { return ctx; }
#endif

       // yidl::runtime::Object
       SSLContext& inc_ref() { return Object::inc_ref( *this ); }

  private:
#ifdef YIELD_IPC_HAVE_OPENSSL
      SSLContext( SSL_CTX* ctx );
#else
      SSLContext();
#endif
      ~SSLContext();

#ifdef YIELD_IPC_HAVE_OPENSSL
      static SSL_CTX* createSSL_CTX
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method
      );

      SSL_CTX* ctx;
#endif
    };


#ifdef YIELD_IPC_HAVE_OPENSSL
    class SSLSocket : public TCPSocket
    {
    public:
      static SSLSocket* create( SSLContext& ssl_context );
      static SSLSocket* create( int domain, SSLContext& ssl_context );

      operator SSL*() const { return ssl; }

      // Socket
      // Will only associate with BIO or NBIO queues
      bool associate( yield::platform::IOQueue& io_queue );
      bool connect( const SocketAddress& peername );
      virtual ssize_t recv( void* buf, size_t buflen, int );
      virtual ssize_t send( const void* buf, size_t buflen, int );

      // Delegates to send, since OpenSSL has no gather I/O
      virtual ssize_t sendmsg( const struct iovec* iov, uint32_t iovlen, int );

      // yield::platform::IStream
      virtual bool want_read() const;

      // yield::platform::OStream
      virtual bool want_write() const;
      
      // TCPSocket
      virtual TCPSocket* accept();
      virtual bool shutdown();

    protected:
      SSLSocket( int, yield::platform::socket_t, SSL*, SSLContext& );
      ~SSLSocket();

    private:
      SSL* ssl;
      SSLContext& ssl_context;
    };


    class SSLSocketFactory : public SocketFactory
    {
    public:
      SSLSocketFactory( SSLContext& ssl_context )
        : ssl_context( ssl_context.inc_ref() )
      { }

      ~SSLSocketFactory()
      {
        SSLContext::dec_ref( ssl_context );
      }

      // SocketFactory
      Socket* createSocket() { return SSLSocket::create( ssl_context ); }

    private:
      SSLContext& ssl_context;
    };
#endif


    class TCPSocketFactory : public SocketFactory
    {
    public:
      // SocketFactory
      Socket* createSocket() { return TCPSocket::create(); }
    };


    // The underlying_X pattern doesn't work as well with Sockets as it does
    // with Files, since Sockets have more state (like blocking mode, 
    // bins, etc.). Thus TracingXSocket inherits from XSocket instead of trying
    // to delegate everything.    
    class TracingTCPSocket : public TCPSocket
    {
    public:
      virtual ~TracingTCPSocket();

      static TracingTCPSocket* create( Log& log );
      static TracingTCPSocket* create( int domain, Log& log );

      // Socket
      bool connect( const SocketAddress& peername );
      ssize_t recv( void* buf, size_t buflen, int );
      ssize_t send( const void* buf, size_t buflen, int );
      ssize_t sendmsg( const struct iovec* iov, uint32_t iovlen, int );
      bool want_connect() const;
      bool want_read() const;
      bool want_write() const;

      // TCPSocket
      TCPSocket* accept();

    private:
      TracingTCPSocket( int domain, Log& log, yield::platform::socket_t );

    private:
      Log& log;
    };


    class TracingTCPSocketFactory : public SocketFactory
    {
    public:
      TracingTCPSocketFactory( Log& log )
        : log( log.inc_ref() )
      { }

      ~TracingTCPSocketFactory()
      {
        Log::dec_ref( log );
      }

      // SocketFactory
      Socket* createSocket() { return TracingTCPSocket::create( log ); }

    private:
      Log& log;
    };


    class UDPSocketFactory : public SocketFactory
    {
    public:
      // SocketFactory
      Socket* createSocket() { return UDPSocket::create(); }
    };


    class URI : public yidl::runtime::Object
    {
    public:
      URI();
      URI( const char* scheme, const char* host, uint16_t port );
      URI( const char* scheme, const char* host, uint16_t port, const char* resource );
      URI( const URI& other );
      // Parsing constructors throw exceptions
      URI( const char* uri );
      URI( const string& uri );
      URI( const char* uri, size_t uri_len );
      virtual ~URI() { }

      const string& get_scheme() const { return scheme; }
      const string& get_host() const { return host; }
      const string& get_user() const { return user; }
      const string& get_password() const { return password; }
      unsigned short get_port() const { return port; }
      const string& get_resource() const { return resource; }
      const multimap<string, string>& get_query() const { return query; }

      string get_query_value
      (
        const string& key,
        const char* default_value = ""
      ) const;

      multimap<string, string>::const_iterator
      get_query_values
      ( 
        const string& key 
      ) const;

      URI& operator=( const URI& );

      static URI* parse( const char* uri );
      static URI* parse( const string& uri );
      static URI* parse( const char* uri, size_t uri_len );

      void set_scheme( const string& scheme ) { this->scheme = scheme; }
      void set_host( const string& host ) { this->host = host; }
      void set_user( const string& user ) { this->user = user; }
      void set_password( const string& password );
      void set_port( uint16_t port ) { this->port = port; }
      void set_resource( const string& resource );

    private:
      URI( UriUriStructA& parsed_uri );

      void init( const char* uri, size_t uri_len );
      void init( UriUriStructA& parsed_uri );

    private:
      string scheme, user, password, host;
      unsigned short port;
      string resource;
      multimap<string, string> query;
    };


    class UUID : public yidl::runtime::Object
    {
    public:
      UUID();
      UUID( const string& uuid_from_string );
      ~UUID();

      bool operator==( const UUID& ) const;
      operator string() const;

    private:
#if defined(_WIN32)
      void* win32_uuid;
#elif defined(YIELD_IPC_HAVE_LINUX_LIBUUID)
      void* linux_libuuid_uuid;
#elif defined(__sun)
      void* sun_uuid;
#else
      char generic_uuid[256];
#endif
    };
  };
};


#endif
