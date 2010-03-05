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

#ifdef YIELD_IPC_HAVE_LIBUUID
#include <uuid/uuid.h>
#endif
#ifdef YIELD_IPC_HAVE_OPENSSL
#include <openssl/ssl.h>
#endif


struct UriUriStructA;
struct yajl_gen_t;
typedef struct yajl_gen_t* yajl_gen;


#define YIELD_RFC822_HEADERS_STACK_BUFFER_LENGTH 128
#define YIELD_RFC822_HEADERS_STACK_IOVECS_LENGTH 32


namespace YIELD
{
  namespace ipc
  {
    class HTTPClient;
    typedef yidl::runtime::auto_Object<HTTPClient> auto_HTTPClient;

    class HTTPRequest;
    typedef yidl::runtime::auto_Object<HTTPRequest> auto_HTTPRequest;

    class HTTPResponse;
    typedef yidl::runtime::auto_Object<HTTPResponse> auto_HTTPResponse;

    class HTTPServer;
    typedef yidl::runtime::auto_Object<HTTPServer> auto_HTTPServer;
	
    class ONCRPCClient;
    typedef yidl::runtime::auto_Object<ONCRPCClient> auto_ONCRPCClient;

    class ONCRPCRequest;
	  typedef yidl::runtime::auto_Object<ONCRPCRequest> auto_ONCRPCRequest;

    class ONCRPCResponse;
    typedef yidl::runtime::auto_Object<ONCRPCResponse> auto_ONCRPCResponse;	

    class ONCRPCServer;
    typedef yidl::runtime::auto_Object<ONCRPCServer> auto_ONCRPCServer;	

    class SocketFactory;
    typedef yidl::runtime::auto_Object<SocketFactory> auto_SocketFactory;

    class SSLContext;
    typedef yidl::runtime::auto_Object<SSLContext> auto_SSLContext;

    class SSLSocket;
    typedef yidl::runtime::auto_Object<SSLSocket> auto_SSLSocket;

    class URI;
    typedef yidl::runtime::auto_Object<URI> auto_URI;

    class UUID;
    typedef yidl::runtime::auto_Object<UUID> auto_UUID;	


    template <class RequestType, class ResponseType>
    class Client : public YIELD::concurrency::EventHandler
    {
    public:
      const static uint16_t CONCURRENCY_LEVEL_DEFAULT = 4;
      const static uint32_t FLAG_TRACE_NETWORK_IO = 1;
      const static uint32_t FLAG_TRACE_OPERATIONS = 2;
      const static uint64_t OPERATION_TIMEOUT_DEFAULT
        = 5 * YIELD::platform::Time::NS_IN_S;
      const static uint16_t RECONNECT_TRIES_MAX_DEFAULT = 2;

      uint32_t get_flags() const { return flags; }

      YIELD::platform::auto_SocketAddress get_peername() const 
      { 
        return peername;
      }

      const YIELD::platform::Time& get_operation_timeout() const
      { 
        return operation_timeout; 
      }

      uint16_t get_reconnect_tries_max() const { return reconnect_tries_max; }

      // YIELD::concurrency::EventHandler
      virtual void handleEvent( YIELD::concurrency::Event& );

    protected:
      Client
      (
        uint16_t concurrency_level, // e.g. the # of simultaneous conns for TCP
        uint32_t flags,
        YIELD::platform::auto_Log log,
        const YIELD::platform::Time& operation_timeout,
        YIELD::platform::auto_SocketAddress peername,
        uint16_t reconnect_tries_max,
        auto_SocketFactory socket_factory
      );

      virtual ~Client();

      YIELD::platform::auto_Log get_log() const { return log; }

    private:
      uint16_t concurrency_level;
      uint32_t flags;
      YIELD::platform::auto_Log log;
      YIELD::platform::Time operation_timeout;
      YIELD::platform::auto_SocketAddress peername;
      uint16_t reconnect_tries_max;
      YIELD::concurrency
        ::SynchronizedSTLQueue<YIELD::platform::Socket*> sockets;
      auto_SocketFactory socket_factory;

    private:
      class Connection;
    };


    class RFC822Headers
    {
    public:
      RFC822Headers( uint8_t reserve_iovecs_count = 0 );
      virtual ~RFC822Headers();

      ssize_t deserialize( yidl::runtime::auto_Buffer );

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
        const std::string& header_name,
        const std::string& header_value
      );

      yidl::runtime::auto_Buffers serialize();

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

      char stack_buffer[YIELD_RFC822_HEADERS_STACK_BUFFER_LENGTH],
           *heap_buffer,
           *buffer_p;
      size_t heap_buffer_len;

      struct iovec stack_iovecs[YIELD_RFC822_HEADERS_STACK_IOVECS_LENGTH],
                   *heap_iovecs;
      uint8_t iovecs_filled;

      inline void advanceBufferPointer()
      {
        buffer_p++;

        if ( heap_buffer == NULL )
        {
          if
          (
            buffer_p - stack_buffer <
              YIELD_RFC822_HEADERS_STACK_BUFFER_LENGTH
          )
            return;
          else
            allocateHeapBuffer();
        }
        else if
        (
          static_cast<size_t>( buffer_p - heap_buffer ) < heap_buffer_len
        )
          return;
        else
          allocateHeapBuffer();
      }

      void allocateHeapBuffer();
    };


    class HTTPMessage : public RFC822Headers
    {
    public:
      yidl::runtime::auto_Buffer get_body() const { return body; }
      uint8_t get_http_version() const { return http_version; }

    protected:
      HTTPMessage( uint8_t reserve_iovecs_count );
      HTTPMessage( uint8_t reserve_iovecs_count, yidl::runtime::auto_Buffer );
      HTTPMessage( const HTTPMessage& ) { DebugBreak(); } // Prevent copying
      virtual ~HTTPMessage() { }

      yidl::runtime::auto_Buffer body;

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

      uint8_t http_version;

      virtual ssize_t deserialize( yidl::runtime::auto_Buffer );
      virtual yidl::runtime::auto_Buffers serialize();
    };


    class HTTPResponse
      : public YIELD::concurrency::Response, public HTTPMessage
    {
    public:
      // Incoming
      HTTPResponse();

      // Outgoing
      HTTPResponse( uint16_t status_code );
      HTTPResponse( uint16_t status_code, yidl::runtime::auto_Buffer body );

      ssize_t deserialize( yidl::runtime::auto_Buffer );
      uint16_t get_status_code() const { return status_code; }
      yidl::runtime::auto_Buffers serialize();
      void set_body( yidl::runtime::auto_Buffer body );
      void set_status_code( uint16_t status_code );

      // yidl::runtime::RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( HTTPResponse, 206 );

      // yidl::runtime::MarshallableObject
      void marshal( yidl::runtime::Marshaller& ) const { }
      void unmarshal( yidl::runtime::Unmarshaller& ) { }

    protected:
      virtual ~HTTPResponse() { }

    private:
      HTTPResponse( const HTTPResponse& other ) // Prevent copying
        : HTTPMessage( other )
      { }

      uint8_t http_version;
      union { char status_code_str[4]; uint16_t status_code; };
    };


    class HTTPRequest
      : public YIELD::concurrency::Request, public HTTPMessage
    {
    public:
      HTTPRequest(); // Incoming

      HTTPRequest // Outgoing
      (
        const char* method,
        const char* relative_uri,
        const char* host,
        yidl::runtime::auto_Buffer body = NULL
      );

      HTTPRequest // Outgoing
      (
        const char* method,
        const URI& absolute_uri,
        yidl::runtime::auto_Buffer body = NULL
      );

      virtual auto_HTTPResponse createResponse(); // Used by Client
      ssize_t deserialize( yidl::runtime::auto_Buffer );
      uint8_t get_http_version() const { return http_version; }
      const char* get_method() const { return method; }
      const char* get_uri() const { return uri; }
      virtual void respond( uint16_t status_code );

      virtual void respond
      (
        uint16_t status_code,
        yidl::runtime::auto_Buffer body
      );

      virtual void respond( YIELD::concurrency::Response& response );
      yidl::runtime::auto_Buffers serialize();      

      // yidl::runtime::RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( HTTPRequest, 205 );

      // yidl::runtime::MarshallableObject
      void marshal( yidl::runtime::Marshaller& ) const { }
      void unmarshal( yidl::runtime::Unmarshaller& ) { }

    protected:
      virtual ~HTTPRequest();

    private:
      HTTPRequest( const HTTPRequest& other ) // Prevent copying
        : HTTPMessage( other )
      { }

      void init
      (
        const char* method,
        const char* relative_uri,
        const char* host,
        yidl::runtime::auto_Buffer body
      );

      char method[16];
      char* uri; size_t uri_len;
    };


    class HTTPClient : public Client<HTTPRequest, HTTPResponse>
    {
    public:
      // create( ... ) throws an exception instead of returning NULL
      static auto_HTTPClient
      create
      (
        const URI& absolute_uri,
        uint16_t concurrency_level = CONCURRENCY_LEVEL_DEFAULT,
        uint32_t flags = 0,
        YIELD::platform::auto_Log log = NULL,
        const YIELD::platform::Time& operation_timeout
          = OPERATION_TIMEOUT_DEFAULT,
        uint16_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT,
        auto_SSLContext ssl_context = NULL
      );

      static auto_HTTPResponse
      GET
      (
        const URI& absolute_uri,
        YIELD::platform::auto_Log log = NULL
      );

      static auto_HTTPResponse
      PUT
      (
        const URI& absolute_uri,
        yidl::runtime::auto_Buffer body,
        YIELD::platform::auto_Log log = NULL
      );

      static auto_HTTPResponse
      PUT
      (
        const URI& absolute_uri,
        const YIELD::platform::Path& body_file_path,
        YIELD::platform::auto_Log log = NULL
      );

    private:
      HTTPClient
      (
        uint16_t concurrency_level,
        uint32_t flags,
        YIELD::platform::auto_Log log,
        const YIELD::platform::Time& operation_timeout,
        YIELD::platform::auto_SocketAddress peername,
        uint16_t reconnect_tries_max,
        auto_SocketFactory socket_factory
      )
        : Client<HTTPRequest, HTTPResponse>
          (
            concurrency_level,
            flags,
            log,
            operation_timeout,
            peername,
            reconnect_tries_max,
            socket_factory
          )
      { }

      virtual ~HTTPClient() { }

      static auto_HTTPResponse
      sendHTTPRequest
      (
        const char* method,
        const URI& uri,
        yidl::runtime::auto_Buffer body,
        YIELD::platform::auto_Log log
      );
    };


    class HTTPServer 
      : public yidl::runtime::Object,
        public YIELD::platform::TCPSocket::AIOAcceptCallback
    {
    public:
      static auto_HTTPServer
      create
      (
        const URI& absolute_uri,
        YIELD::concurrency::auto_EventTarget http_request_target,
        YIELD::platform::auto_Log log = NULL,
        auto_SSLContext ssl_context = NULL
      );

    private:
      HTTPServer
      (
        YIELD::concurrency::auto_EventTarget http_request_target,
        YIELD::platform::auto_TCPSocket listen_tcp_socket,
        YIELD::platform::auto_Log log
      );

      YIELD::concurrency::auto_EventTarget http_request_target;
      YIELD::platform::auto_TCPSocket listen_tcp_socket;
      YIELD::platform::auto_Log log;

    private:
      // YIELD::platform::TCPSocket::AIOAcceptCallback
      void
      onAcceptCompletion
      ( 
        YIELD::platform::auto_TCPSocket accepted_tcp_socket, 
        void* 
      );

      void onAcceptError( uint32_t error_code, void* );

    private:
      class Connection;
    };

    
    template <class RPCRequestType, class RPCResponseType>
    class RPCClient : public Client<RPCRequestType, RPCResponseType>
    {
    protected:
      RPCClient
      (
        uint16_t concurrency_level,
        uint32_t flags,
        YIELD::platform::auto_Log log,
        yidl::runtime::auto_MarshallableObjectFactory
          marshallable_object_factory,
        const YIELD::platform::Time& operation_timeout,
        YIELD::platform::auto_SocketAddress peername,
        uint16_t reconnect_tries_max,
        auto_SocketFactory socket_factory
      )
        : Client<RPCRequestType, RPCResponseType>
          (
            concurrency_level,
            flags,
            log,
            operation_timeout,
            peername,
            reconnect_tries_max,
            socket_factory
          )
      { }

    protected:
      yidl::runtime::auto_MarshallableObjectFactory
        marshallable_object_factory;
    };


    class RPCMessage
    {
    public:
      yidl::runtime::auto_MarshallableObject get_body() const 
      { 
        return body;
      }

    protected:
      // Incoming
      RPCMessage( yidl::runtime::auto_MarshallableObjectFactory );

      // Outgoing
      RPCMessage( yidl::runtime::auto_MarshallableObject body );
      
      yidl::runtime::auto_MarshallableObjectFactory
      get_marshallable_object_factory() const
      {
        return marshallable_object_factory;
      }

      void
      marshal_body
      ( 
        const char* key, 
        uint32_t tag, 
        yidl::runtime::Marshaller& marshaller
      ) const;

      bool unmarshal_new_Request_body
      ( 
        const char* key, 
        uint32_t tag,
        uint32_t type_id,
        yidl::runtime::Unmarshaller& unmarshaller
      );

      bool unmarshal_new_Response_body
      ( 
        const char* key, 
        uint32_t tag,
        uint32_t type_id,
        yidl::runtime::Unmarshaller& unmarshaller
      );

      bool
      unmarshal_new_ExceptionResponse_body
      ( 
        const char* key, 
        uint32_t tag,
        uint32_t type_id, 
        yidl::runtime::Unmarshaller& unmarshaller
      );

      void set_body( yidl::runtime::MarshallableObject* body );

    private:
      yidl::runtime::auto_MarshallableObject body;
      yidl::runtime::auto_MarshallableObjectFactory 
        marshallable_object_factory;
    };
   

    class RPCServer : public yidl::runtime::Object
    {
    protected:
      RPCServer
      ( 
        YIELD::platform::auto_Log log,
        yidl::runtime::auto_MarshallableObjectFactory 
          marshallable_object_factory
      )
        : log( log ), 
          marshallable_object_factory( marshallable_object_factory )
      { }

    protected:
      YIELD::platform::auto_Log log;
      yidl::runtime::auto_MarshallableObjectFactory 
          marshallable_object_factory;
    };


    class JSONMarshaller : public yidl::runtime::Marshaller
    {
    public:
      JSONMarshaller( bool write_empty_strings = true );
      virtual ~JSONMarshaller();

      yidl::runtime::auto_Buffer get_buffer() const { return buffer; }

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
      bool in_map;
      const char* root_key;
      bool write_empty_strings;
      yajl_gen writer;

      void flushYAJLBuffer();
      yidl::runtime::auto_Buffer buffer;
    };


    class JSONUnmarshaller : public yidl::runtime::Unmarshaller
    {
    public:
      JSONUnmarshaller( yidl::runtime::auto_Buffer buffer );
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


    class ONCRPCGarbageArgumentsError
      : public YIELD::concurrency::ExceptionResponse
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
      virtual ssize_t deserialize( yidl::runtime::auto_Buffer );
      uint32_t get_xid() const { return xid; }
      yidl::runtime::auto_Buffers serialize();

    protected:
      // Incoming
      ONCRPCMessage
      ( 
        yidl::runtime::auto_MarshallableObjectFactory, 
        uint32_t xid
      );

      // Outgoing
      ONCRPCMessage( yidl::runtime::auto_MarshallableObject, uint32_t xid );

      virtual ~ONCRPCMessage();

      enum
      {
        DESERIALIZING_RECORD_FRAGMENT_MARKER,
        DESERIALIZING_RECORD_FRAGMENT,
        DESERIALIZING_LONG_RECORD_FRAGMENT,
        DESERIALIZE_DONE
      } deserialize_state;

      ssize_t deserializeRecordFragmentMarker( yidl::runtime::auto_Buffer );
      ssize_t deserializeRecordFragment( yidl::runtime::auto_Buffer );
      ssize_t deserializeLongRecordFragment( yidl::runtime::auto_Buffer );

      // yidl::runtime::Marshallable
      void marshal( yidl::runtime::Marshaller& marshaller ) const;
      void unmarshal( yidl::runtime::Unmarshaller& unmarshaller );

    private:
      uint32_t record_fragment_length;
      yidl::runtime::auto_Buffer record_fragment_buffer;
      uint32_t xid;
    };


    class ONCRPCMessageRejectedError
      : public YIELD::concurrency::ExceptionResponse
    {
    public:
      ONCRPCMessageRejectedError()
        : ExceptionResponse( 1, "ONC-RPC: message rejected" )
      { }
    };


    class ONCRPCProcedureUnavailableError
      : public YIELD::concurrency::ExceptionResponse
    {
    public:
      ONCRPCProcedureUnavailableError()
        : ExceptionResponse( 3, "ONC-RPC: procedure unavailable" )
      { }
    };


    class ONCRPCProgramMismatchError
      : public YIELD::concurrency::ExceptionResponse
    {
    public:
      ONCRPCProgramMismatchError()
        : ExceptionResponse( 2, "ONC-RPC: program mismatch" )
      { }
    };


    class ONCRPCProgramUnavailableError
      : public YIELD::concurrency::ExceptionResponse
    {
    public:
      ONCRPCProgramUnavailableError()
        : ExceptionResponse( 1, "ONC-RPC: program unavailable" )
      { }
    };


    class ONCRPCRequest 
      : public YIELD::concurrency::Request,
        public ONCRPCMessage<ONCRPCRequest>
    {
    public:
      const static uint32_t AUTH_NONE = 0;

      // Incoming
      ONCRPCRequest( yidl::runtime::auto_MarshallableObjectFactory );

      // Outgoing
      ONCRPCRequest
      (
        uint32_t prog,
        uint32_t proc,
        uint32_t vers,
        yidl::runtime::auto_MarshallableObject body,
        yidl::runtime::auto_MarshallableObject cred = NULL, // AUTH_NONE
        yidl::runtime::auto_MarshallableObject verf = NULL // AUTH_NONE
      );

      virtual ~ONCRPCRequest() { }

      virtual auto_ONCRPCResponse createResponse(); // Used by Client
      yidl::runtime::auto_MarshallableObject get_cred() const { return cred; }
      uint32_t get_prog() const { return prog; }
      uint32_t get_proc() const { return proc; }      
      yidl::runtime::auto_MarshallableObject get_verf() const { return verf; }
      uint32_t get_vers() const { return vers; }

      // yidl::runtime::RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ONCRPCRequest, 213 );

      // yidl::runtime::MarshallableObject
      virtual void marshal( yidl::runtime::Marshaller& ) const;
      virtual void unmarshal( yidl::runtime::Unmarshaller& );

    private:      
      yidl::runtime::auto_MarshallableObject cred;
      uint32_t prog, proc;
      yidl::runtime::auto_MarshallableObject verf;
      uint32_t vers;
    };


    class ONCRPCResponse 
      : public YIELD::concurrency::Response, 
        public ONCRPCMessage<ONCRPCResponse>
    {
    public:
      // Incoming
      ONCRPCResponse
      ( 
        uint32_t default_body_type_id,
        yidl::runtime::auto_MarshallableObjectFactory,
        uint32_t xid
      );

      // Outgoing
      ONCRPCResponse
      (
        yidl::runtime::auto_MarshallableObject body,
        uint32_t xid
      );
      
      virtual ~ONCRPCResponse() { }

      // yidl::runtime::RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ONCRPCResponse, 208 );

      // yidl::runtime::MarshallableObject
      virtual void marshal( yidl::runtime::Marshaller& ) const;
      virtual void unmarshal( yidl::runtime::Unmarshaller& );

    private:
      uint32_t default_body_type_id;
    };


    class ONCRPCClient : public RPCClient<ONCRPCRequest, ONCRPCResponse>
    {
    public:
      static auto_ONCRPCClient
      create
      (
        const URI& absolute_uri,
        yidl::runtime::auto_MarshallableObjectFactory,
        uint16_t concurrency_level = CONCURRENCY_LEVEL_DEFAULT,
        uint32_t flags = 0,
        YIELD::platform::auto_Log log = NULL,
        const YIELD::platform::Time& operation_timeout
          = OPERATION_TIMEOUT_DEFAULT,
        uint16_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT,
        auto_SSLContext ssl_context = NULL
      );

    protected:
      ONCRPCClient
      (
        uint16_t concurrency_level,
        uint32_t flags,
        YIELD::platform::auto_Log log,
        yidl::runtime::auto_MarshallableObjectFactory
          marshallable_object_factory,
        const YIELD::platform::Time& operation_timeout,
        YIELD::platform::auto_SocketAddress peername,
        uint16_t reconnect_tries_max,
        auto_SocketFactory socket_factory
      )
        : RPCClient<ONCRPCRequest, ONCRPCResponse>
          (
            concurrency_level,
            flags,
            log,
            marshallable_object_factory,
            operation_timeout,
            peername,
            reconnect_tries_max,
            socket_factory
          )
      { }

      virtual ~ONCRPCClient() { }
    };


    class ONCRPCServer : public RPCServer
    {
    public:
      static auto_ONCRPCServer
      create
      (
        const URI& absolute_uri,
        yidl::runtime::auto_MarshallableObjectFactory,
        YIELD::concurrency::auto_EventTarget oncrpc_request_target,
        YIELD::platform::auto_Log log = NULL,
        auto_SSLContext ssl_context = NULL
      ); // Throws exceptions

    protected:
      ONCRPCServer
      ( 
        YIELD::platform::auto_Log log,
        yidl::runtime::auto_MarshallableObjectFactory,
        YIELD::concurrency::auto_EventTarget oncrpc_request_target          
      );

    protected:
      YIELD::concurrency::auto_EventTarget oncrpc_request_target;
    };


    class ONCRPCSystemError
      : public YIELD::concurrency::ExceptionResponse
    {
    public:
      ONCRPCSystemError()
        : ExceptionResponse( 5, "ONC-RPC: system error" )
      { }
    };


    class SocketFactory : public yidl::runtime::Object
    {
    public:
      virtual YIELD::platform::auto_Socket createSocket() = 0;
    };


    class SSLContext : public yidl::runtime::Object
    {
    public:
#ifdef YIELD_IPC_HAVE_OPENSSL
      // create( ... ) factory methods throw exceptions

      static auto_SSLContext
      create
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method = SSLv23_client_method()
      );

      static auto_SSLContext
      create
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method,
        const YIELD::platform::Path& pem_certificate_file_path,
        const YIELD::platform::Path& pem_private_key_file_path,
        const std::string& pem_private_key_passphrase
      );

      static auto_SSLContext
      create
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method,
        const std::string& pem_certificate_str,
        const std::string& pem_private_key_str,
        const std::string& pem_private_key_passphrase
      );

      static auto_SSLContext
      create
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method,
        const YIELD::platform::Path& pkcs12_file_path,
        const std::string& pkcs12_passphrase
      );
#else
      static auto_SSLContext create();
#endif

#ifdef YIELD_IPC_HAVE_OPENSSL
      SSL_CTX* get_ssl_ctx() const { return ctx; }
#endif

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
    class SSLSocket : public YIELD::platform::TCPSocket
    {
    public:
      // create( ... ) factory methods return NULL
      // instead of throwing exceptions
      static auto_SSLSocket create( auto_SSLContext );
      static auto_SSLSocket create( int domain, auto_SSLContext ssl_context );

      // Socket
      // Will only associate with BIO or NBIO queues
      bool associate( YIELD::platform::auto_IOQueue io_queue );
      bool connect( const YIELD::platform::SocketAddress& peername );
      virtual ssize_t recv( void* buffer, size_t buffer_len, int );
      virtual ssize_t send( const void* buffer, size_t buffer_len, int );

      // Delegates to send, since OpenSSL has no gather I/O
      virtual ssize_t 
      sendmsg
      ( 
        const struct iovec* buffers, 
        uint32_t buffers_count, 
        int flags
      );

      // YIELD::platform::IStream
      virtual bool want_read() const;

      // YIELD::platform::OStream
      virtual bool want_write() const;
      
      // YIELD::platform::TCPSocket
      YIELD::platform::auto_TCPSocket accept();
      virtual bool shutdown();

    protected:
      SSLSocket( int, YIELD::platform::socket_t, auto_SSLContext, SSL* );
      ~SSLSocket();

      auto_SSLContext ctx;
      SSL* ssl;
    };


    class SSLSocketFactory : public SocketFactory
    {
    public:
      SSLSocketFactory( auto_SSLContext ssl_context )
        : ssl_context( ssl_context )
      { }

      // SocketFactory
      YIELD::platform::auto_Socket createSocket()
      {
        return SSLSocket::create( ssl_context ).release();
      }

    private:
      auto_SSLContext ssl_context;
    };
#endif


    class TCPSocketFactory : public SocketFactory
    {
    public:
      // SocketFactory
      YIELD::platform::auto_Socket createSocket()
      {
        return YIELD::platform::TCPSocket::create().release();
      }
    };


    class TracingSocket : public YIELD::platform::Socket
    {
    public:
      TracingSocket
      (
        YIELD::platform::auto_Log log,
        YIELD::platform::auto_Socket underlying_socket
      );

      // Socket
      bool bind( const YIELD::platform::SocketAddress& to_sockaddr );
      bool close();
      bool connect( const YIELD::platform::SocketAddress& peername );
      bool get_blocking_mode() const;
      YIELD::platform::auto_SocketAddress getpeername();
      YIELD::platform::auto_SocketAddress getsockname();
      ssize_t recv( void* buffer, size_t buffer_len, int );
      ssize_t sendmsg( const struct iovec* buffers, uint32_t buffers_count, int );
      bool set_blocking_mode( bool blocking );
      bool want_connect() const;
      bool want_read() const;
      bool want_write() const;

    private:
      ~TracingSocket() { }

      YIELD::platform::auto_Log log;
      YIELD::platform::auto_Socket underlying_socket;
    };


    class UDPSocketFactory : public SocketFactory
    {
    public:
      // SocketFactory
      YIELD::platform::auto_Socket createSocket()
      {
        return YIELD::platform::UDPSocket::create().release();
      }
    };


    class URI : public yidl::runtime::Object
    {
    public:
      // parse( ... ) factory methods return NULL
      // instead of throwing exceptions
      static auto_URI parse( const char* uri );
      static auto_URI parse( const std::string& uri );
      static auto_URI parse( const char* uri, size_t uri_len );

      // Constructors throw exceptions
      URI( const char* uri );
      URI( const std::string& uri );
      URI( const char* uri, size_t uri_len );
      URI( const char* scheme, const char* host, uint16_t port );
      URI( const char* scheme, const char* host, uint16_t port, const char* resource );
      URI( const URI& other );
      virtual ~URI() { }

      const std::string& get_scheme() const { return scheme; }
      const std::string& get_host() const { return host; }
      const std::string& get_user() const { return user; }
      const std::string& get_password() const { return password; }
      unsigned short get_port() const { return port; }
      const std::string& get_resource() const { return resource; }

      const std::multimap<std::string, std::string>& get_query() const
      {
        return query;
      }

      std::string get_query_value
      (
        const std::string& key,
        const char* default_query_value = ""
      ) const;

      std::multimap<std::string, std::string>::const_iterator
        get_query_values( const std::string& key ) const;

      void set_port( unsigned short port ) { this->port = port; }

    private:
      URI( UriUriStructA& parsed_uri )
      {
        init( parsed_uri );
      }

      void init( const char* uri, size_t uri_len );
      void init( UriUriStructA& parsed_uri );

      std::string scheme, user, password, host;
      unsigned short port;
      std::string resource;
      std::multimap<std::string, std::string> query;
    };


    class UUID : public yidl::runtime::Object
    {
    public:
      UUID();
      UUID( const std::string& uuid_from_string );
      ~UUID();

      bool operator==( const UUID& ) const;
      operator std::string() const;

    private:
#if defined(_WIN32)
      void* win32_uuid;
#elif defined(YIELD_IPC_HAVE_LIBUUID)
      uuid_t libuuid_uuid;
#else
      char generic_uuid[256];
#endif
    };
  };
};


#endif
