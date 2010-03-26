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

#include <deque>
#include <stack>

struct UriUriStructA;
struct yajl_gen_t;
typedef struct yajl_gen_t* yajl_gen;
struct yajl_handle_t;
typedef struct yajl_handle_t* yajl_handle;


#define YIELD_IPC_ONCRPC_ERROR( ClassName, ErrorCode, ErrorMessage ) \
    class ONCRPC ## ClassName ## Error : public Exception\
    {\
    public: \
      const static uint32_t ERROR_CODE = ErrorCode;\
      ONCRPC ## ClassName ## Error()\
        : Exception( ERROR_CODE, "ONC-RPC:" # ErrorMessage )\
      { }\
    };\


namespace yield
{
  namespace ipc
  {
    class HTTPRequest;
    class HTTPResponse;
    class ONCRPCRecordFragment;
    class ONCRPCRequest;
    class ONCRPCResponse;
    class URI;

    using std::multimap;

    using yidl::runtime::Buffer;
    using yidl::runtime::Buffers;
    using yidl::runtime::Map;
    using yidl::runtime::MarshallableObject;
    using yidl::runtime::Marshaller;
    using yidl::runtime::Object;
    using yidl::runtime::RTTIObject;
    using yidl::runtime::Sequence;
    using yidl::runtime::Unmarshaller;
    using yidl::runtime::XDRMarshaller;
    using yidl::runtime::XDRUnmarshaller;

    using yield::concurrency::Message;
    using yield::concurrency::MessageFactory;
    using yield::concurrency::EventHandler;
    using yield::concurrency::Exception;
    using yield::concurrency::Request;
    using yield::concurrency::RequestHandler;
    using yield::concurrency::Response;
    using yield::concurrency::ResponseHandler;
    using yield::concurrency::SynchronizedSTLQueue;

    using yield::platform::IOQueue;
    using yield::platform::IStream;
    using yield::platform::Log;
    using yield::platform::OStream;
    using yield::platform::Path;
    using yield::platform::Socket;
    using yield::platform::SocketAddress;
    using yield::platform::socket_t;
#ifdef YIELD_PLATFORM_HAVE_OPENSSL
    using yield::platform::SSLContext;
    using yield::platform::SSLSocket;
#endif
    using yield::platform::StreamSocket;
    using yield::platform::TCPSocket;
    using yield::platform::UDPSocket;
    using yield::platform::Time;

    
    class RPCPeer
    {
    protected:
      RPCPeer( MessageFactory& message_factory )
        : message_factory( message_factory.inc_ref() )
      { }

      virtual ~RPCPeer()
      {
        MessageFactory::dec_ref( message_factory );
      }

      MessageFactory& get_message_factory() const { return message_factory; }

    private:
      MessageFactory& message_factory;
    };


    template <class RPCRequestType, class RPCResponseType>
    class RPCClient : public RPCPeer
    {
    protected:
      RPCClient( MessageFactory& message_factory )
        : RPCPeer( message_factory )
      { }

    protected:
      class RPCResponseHandler : public ResponseHandler
      {
      public:
        RPCResponseHandler( Request& request )
          : request( request.inc_ref() )            
        { }

        ~RPCResponseHandler()
        {
          Request::dec_ref( request );
        }

        // RTTIObject
        const char* get_type_name() const { return "RPCResponseHandler"; }

        // ResponseHandler
        void handle( Response& response )
        {
          if ( response.get_type_id() == RPCResponseType::TYPE_ID )
          {
            request.respond
            ( 
              static_cast<Response&>
              ( 
                static_cast<RPCResponseType&>( response ).get_body() 
              ).inc_ref()
            );
          }
          else if ( response.is_exception() )
            request.respond( response );
          else
            DebugBreak();
        }

      private:
        Request& request;
      };
    };


    template <class RPCRequestType, class RPCResponseType>
    class RPCServer : public RPCPeer
    {
    protected:
      RPCServer
      ( 
        MessageFactory& message_factory,
        EventHandler& request_handler,
        bool send_rpc_requests = false
      )
        : RPCPeer( message_factory ),
          request_handler( request_handler.inc_ref() ),
          send_rpc_requests( send_rpc_requests )
      { }

      virtual ~RPCServer()
      {
        EventHandler::dec_ref( request_handler );
      }

      void sendRPCRequest( RPCRequestType& rpc_request )
      {
        if ( send_rpc_requests )
          request_handler.handle( rpc_request );
        else
        {
          ResponseHandler* response_handler 
              = new RPCResponseHandler( rpc_request );
          rpc_request.get_body().set_response_handler( response_handler );
          ResponseHandler::dec_ref( *response_handler );

          request_handler.handle( rpc_request.get_body().inc_ref() );
        }
      }

    private:
      class RPCResponseHandler : public ResponseHandler
      {
      public:
        RPCResponseHandler( RPCRequestType& rpc_request )
          : rpc_request( &rpc_request )
        { }

        ~RPCResponseHandler()
        {
          RPCRequestType::dec_ref( rpc_request );
        }

        // RTTIObject
        const char* get_type_name() const { return "RPCResponseHandler"; }

        // ResponseHandler
        void handle( Response& response )
        {
          rpc_request->respond( response );
          RPCRequestType::dec_ref( rpc_request ); // Have to do this to avoid
          rpc_request = NULL;                    // circular references
        }

      private:
        RPCRequestType* rpc_request;
      };

    private:
      EventHandler& request_handler;
      bool send_rpc_requests;
    };

    
    class SocketPeer
    {
    protected:
      SocketPeer( Log* error_log = NULL, Log* trace_log = NULL );
      virtual ~SocketPeer();

      Log* get_error_log() const { return error_log; }
      Log* get_trace_log() const { return trace_log; }

    private:
      Log* error_log;
      Log* trace_log;
    };


    class SocketClient : public SocketPeer
    {
    public:
      SocketAddress& get_peername() const { return peername; }

    protected:
      SocketClient
      (
        SocketAddress& peername, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      virtual ~SocketClient();

      static SocketAddress& createSocketAddress( const URI& absolute_uri );

    private:
      SocketAddress& peername;
    };


    class SocketServer : public Object, public SocketPeer
    {
    public:
      virtual ~SocketServer();

      EventHandler& get_request_handler() const { return request_handler; }

      // Object
      SocketServer& inc_ref() { return Object::inc_ref( *this ); }

    protected:
      SocketServer
      ( 
        EventHandler& request_handler, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

    private:
      EventHandler& request_handler;
    };


    class StreamSocketClient : public SocketClient
    {
    public:
      class Configuration : public Object
      {
      public:
        const static uint16_t CONCURRENCY_LEVEL_DEFAULT = 4;
        const static uint64_t CONNECT_TIMEOUT_DEFAULT = 5 * Time::NS_IN_S;
        const static uint16_t RECONNECT_TRIES_MAX_DEFAULT = 2;
        const static uint64_t RECV_TIMEOUT_DEFAULT = 5 * Time::NS_IN_S;
        const static uint64_t SEND_TIMEOUT_DEFAULT = 5 * Time::NS_IN_S;

      public:
        Configuration
        (
          uint16_t concurrency_level = CONCURRENCY_LEVEL_DEFAULT,
          const Time& connect_timeout = CONNECT_TIMEOUT_DEFAULT,
          uint16_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT,
          const Time& recv_timeout = RECV_TIMEOUT_DEFAULT,
          const Time& send_timeout = SEND_TIMEOUT_DEFAULT
        )
          : concurrency_level( concurrency_level ),
            connect_timeout( connect_timeout ),
            reconnect_tries_max( reconnect_tries_max ),
            recv_timeout( recv_timeout ),
            send_timeout( send_timeout )
        { }

        uint16_t get_concurrency_level() const { return concurrency_level; }
        const Time& get_connect_timeout() const { return connect_timeout; }
        uint16_t get_reconnect_tries_max() const { return reconnect_tries_max; }
        const Time& get_recv_timeout() const { return recv_timeout; }
        const Time& get_send_timeout() const { return send_timeout; }

        // Object
        Configuration& inc_ref() { return Object::inc_ref( *this ); }

      private:
        uint16_t concurrency_level;
        Time connect_timeout;
        uint16_t reconnect_tries_max;
        Time recv_timeout;
        Time send_timeout;
      };

    public:
      Configuration& get_configuration() const { return *configuration; }

    protected:
      StreamSocketClient
      (
        SocketAddress& peername, // Steals this reference
        Configuration* configuration = NULL, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      virtual ~StreamSocketClient();

    protected:
      class Connection
        : public StreamSocket::AIOConnectCallback,
          public StreamSocket::AIORecvCallback,
          public StreamSocket::AIOSendCallback
      {
      protected:
        Connection
        ( 
          SocketAddress& peername,
          StreamSocket& stream_socket, // Steals this reference
          Log* error_log = NULL, 
          Log* trace_log = NULL
        );
        virtual ~Connection();

        Log* get_error_log() const { return error_log; }
        SocketAddress& get_peername() const { return peername; }
        StreamSocket& get_stream_socket() const { return stream_socket; }
        Log* get_trace_log() const { return trace_log; }

        // Socket::AIOConnectCallback
        virtual void onConnectCompletion( size_t bytes_written, void* );
        virtual void onConnectError( uint32_t error_code, void* context );

        // Socket::AIORecvCallback
        virtual void onReadCompletion( Buffer& buffer, void* context );
        virtual void onReadError( uint32_t error_code, void* context );

        // Socket::AIOSendCallback
        virtual void onWriteCompletion( size_t bytes_written, void* context );
        virtual void onWriteError( uint32_t error_code, void* context );

      private:
        Log* error_log;
        SocketAddress& peername;
        uint16_t reconnect_tries;
        StreamSocket& stream_socket;
        Log* trace_log;
      };

    private:
      Configuration* configuration;
    };


    class StreamSocketServer 
      : public SocketServer,
        public StreamSocket::AIOAcceptCallback
    {
    protected:
      StreamSocketServer
      ( 
        StreamSocket& listen_stream_socket, // Steals this reference
        EventHandler& request_handler, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      virtual ~StreamSocketServer();

      StreamSocket& get_listen_stream_socket() const
      { 
        return listen_stream_socket;
      }

      // StreamSocket::AIOAcceptCallback
      virtual void onAcceptError( uint32_t error_code, void* );

    protected:
      class Connection 
        : public ResponseHandler,
          public StreamSocket::AIORecvCallback,          
          public StreamSocket::AIOSendCallback
      {
      public:
        StreamSocket& get_stream_socket() const { return stream_socket; }

        // StreamSocket::AIORecvCallback
        virtual void onReadError( uint32_t error_code, void* context );

        // StreamSocket::AIOSendCallback
        virtual void onWriteError( uint32_t error_code, void* context );

      protected:
        Connection( StreamSocket&, StreamSocketServer& );
        virtual ~Connection();

      private:
        StreamSocket& stream_socket;
        StreamSocketServer& stream_socket_server;
      };

    private:
      void onReadError( Connection&, uint32_t error_code );
      void onWriteError( Connection&, uint32_t error_code );

    private:
      StreamSocket& listen_stream_socket;
    };


    class TCPSocketClient : public StreamSocketClient
    {
    protected:
      TCPSocketClient
      (
        SocketAddress& peername, // Steals this reference
        Configuration* configuration = NULL, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      virtual ~TCPSocketClient() { }

    protected:
      class Connection : public StreamSocketClient::Connection
      {
      protected:
        Connection
        ( 
          SocketAddress& peername,
          TCPSocket& tcp_socket, // Steals this reference
          Log* error_log = NULL,
          Log* trace_log = NULL
        );

        virtual ~Connection() { }

        TCPSocket& get_tcp_socket() const;
      };
    };


    class TCPSocketServer : public StreamSocketServer
    {
    public:
      static TCPSocket&
      createListenTCPSocket
      (
        const SocketAddress& sockname,
        Log* trace_log = NULL
      );

    protected:
      TCPSocketServer
      ( 
        TCPSocket& listen_tcp_socket, // Steals this reference
        EventHandler& request_handler, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      virtual ~TCPSocketServer() { }

      TCPSocket& get_listen_tcp_socket()
      {
        return static_cast<TCPSocket&>( get_listen_stream_socket() );
      }

    protected:
      class Connection : public StreamSocketServer::Connection
      {
      public:
        TCPSocket& get_tcp_socket() const;

      protected:
        Connection( TCPSocket&, TCPSocketServer& );
        virtual ~Connection() { }
      };
    };


#ifdef YIELD_PLATFORM_HAVE_OPENSSL
    class SSLSocketServer : public TCPSocketServer
    {
    public:
      static SSLSocket&
      createListenSSLSocket
      (
        const SocketAddress& sockname,
        SSLContext& ssl_context,
        Log* trace_log = NULL
      );

    protected:
      SSLSocketServer
      ( 
        SSLSocket& listen_ssl_socket, // Steals this reference
        EventHandler& request_handler, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );
    };
#endif


    class UDPSocketClient 
      : public SocketClient,
        public UDPSocket::AIORecvCallback
    {
    public:
      const static uint64_t RECV_TIMEOUT_DEFAULT = 5 * Time::NS_IN_S;

    public:
      const Time& get_recv_timeout() const { return recv_timeout; }

    protected:
      UDPSocketClient
      (
        SocketAddress& peername, // Steals this reference
        UDPSocket& udp_socket, // Steals this reference
        Log* error_log = NULL,
        const Time& recv_timeout = RECV_TIMEOUT_DEFAULT,
        Log* trace_log = NULL
      );

      static UDPSocket& createUDPSocket
      ( 
        const URI& absolute_uri,
        Log* trace_log
      );

      UDPSocket& get_udp_socket() const { return udp_socket; }

      // UDPSocket::AIORecvCallback
      virtual void onReadCompletion( Buffer& buffer, void* context );
      virtual void onReadError( uint32_t error_code, void* context );

    private:
      Time recv_timeout;
      UDPSocket& udp_socket;
    };


    class UDPSocketServer 
      : public SocketServer,
        public UDPSocket::AIORecvFromCallback        
    {
    protected:
      UDPSocketServer
      ( 
        EventHandler& request_handler, // Steals this reference
        UDPSocket& udp_socket, // Steals this reference
        Log* error_log,
        Log* trace_log
      );

      virtual ~UDPSocketServer();

      static UDPSocket& createUDPSocket
      ( 
        const SocketAddress& sockname,
        Log* trace_log = NULL
      );

      UDPSocket& get_udp_socket() const { return udp_socket; }

    protected:
      class ResponseHandler : public yield::concurrency::ResponseHandler
      {
      protected:
        ResponseHandler( SocketAddress& peername, UDPSocket& udp_socket );
        virtual ~ResponseHandler();

        const SocketAddress& get_peername() const { return peername; }
        UDPSocket& get_udp_socket() const { return udp_socket; }

      private:
        SocketAddress& peername;
        UDPSocket& udp_socket;
      };

    private:
      UDPSocket& udp_socket;
    };


    class HTTPMessage
    {
    public:
      typedef pair<uint16_t, uint16_t> FieldOffset;
      typedef vector<FieldOffset> FieldOffsets;

    public:
      Buffer* get_body() const { return body; }

      const char*
      get_field
      ( 
        const char* name, 
        const char* default_value = "" 
      ) const;

      Time get_time_field( const char* name ) const;

      const char* operator[]( const char* name ) { return get_field( name ); }

      void set_body( Buffer* body ); // Steals this reference
      // set_field: char* copies into a buffer, const char* does not
      void set_field( const char* name, const char* value );
      void set_field( const char* name, char* value );
      void set_field( char* name, char* value );
      void set_field( const string& name, const string& value );
      void set_field( const char* name, const Time& value );

    protected:
      HTTPMessage();

      HTTPMessage
      ( 
        Buffer& header,
        const FieldOffsets& field_offsets, 
        Buffer* body = NULL 
      );

      HTTPMessage( Buffer& body );

      virtual ~HTTPMessage();
      
      Buffers& get_header() const { return *header; }

      void marshal( Marshaller& marshaller ) const;

    private:
      template <class, class> friend class HTTPMessageParser;

      static const char* 
      get_field
      (
        Buffer& header,
        const FieldOffsets& field_offsets,
        const char* name,
        const char* default_value = ""
      );

    private:
      Buffer* body;      
      FieldOffsets field_offsets;
      Buffers* header;
    };


    template <class HTTPMessageParserType, class HTTPMessageType>
    class HTTPMessageParser
    {
    public:
      enum ParseStatus { PARSE_COMPLETE, PARSE_ERROR, PARSE_WANT_READ };
      ParseStatus parse( const string& buffer, vector<HTTPMessageType*>& );
      ParseStatus parse( Buffer& buffer, vector<HTTPMessageType*>& );

      // For testing, returns NULL on PARSE_ERROR or PARSE_WANT_READ
      HTTPMessageType* parse( const string& buffer );
      HTTPMessageType* parse( Buffer& buffer );

      static Time parse_http_date( const char* http_date );

    protected:
      HTTPMessageParser();
      virtual ~HTTPMessageParser();

      Buffer* get_body() const { return body; }

    private:
      HTTPMessageType* createHTTPMessage( Buffer* body = NULL );
      size_t get_content_length( Buffer&, const HTTPMessage::FieldOffsets& );
      ParseStatus parse_header( Buffer& buffer );
      void reset();

    private:
      // Parse state
      Buffer* body; bool chunked_body;
      HTTPMessage::FieldOffsets field_offsets;
      Buffer* header;      
    };


    class HTTPRequest : public Request, public HTTPMessage
    {
    public:
      HTTPRequest( const char* method, const char* uri );
      HTTPRequest( const char* method, const char* uri, Buffer& body );
      HTTPRequest( const char* method, const URI& uri );
      HTTPRequest( const char* method, const URI& uri, Buffer& body );     
      virtual ~HTTPRequest();

      const Time& get_creation_time() const { return creation_time; }
      double get_http_version() const; // double to allow literal comparisons
      const char* get_method() const;
      URI& get_parsed_uri();
      const char* get_uri() const;

      void respond( HTTPResponse& http_response ); // Steals this reference
      void respond( uint16_t status_code );
      void respond( uint16_t status_code, Buffer& body ); // Steals this ref
      void respond( Exception& exception ); // Steals this ref

      // Object
      HTTPRequest& inc_ref() { return Object::inc_ref( *this ); }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( HTTPRequest, 205 );

      // MarshallableObject
      void marshal( Marshaller& marshaller ) const;
      void unmarshal( Unmarshaller& ) { }

    private:
      friend class HTTPRequestParser;

      HTTPRequest
      (
        Buffer& header,
        uint16_t method_offset, // Into header
        uint16_t uri_offset,
        uint16_t http_version_offset,
        const FieldOffsets& field_offsets,
        Buffer* body = NULL
      );

      HTTPRequest( const HTTPRequest& ) { DebugBreak(); } // Prevent copying

      void init( const char* method, const char* uri );

    private:
      Time creation_time;
      uint16_t method_offset, uri_offset, http_version_offset;
      URI* parsed_uri;
    };


    class HTTPRequestHandler : public RequestHandler
    {
    public:
      virtual void handle( HTTPRequest& http_request ) = 0;

      // RequestHandler
      virtual void handle( Request& request );
    };


    class HTTPRequestParser 
      : public HTTPMessageParser<HTTPRequestParser, HTTPRequest>
    {
    public:
      HTTPRequestParser();
      
    private:
      template <class, class> friend class HTTPMessageParser;

      // HTTPMessageParser downcalls
      HTTPRequest*
      createHTTPMessage
      (
        Buffer& header,
        const HTTPMessage::FieldOffsets& field_offsets,
        Buffer* body = NULL
      );

      ParseStatus parse_first_header_line( char** inout_p, const char* pe );

    private:
      uint16_t method_offset, uri_offset, http_version_offset;
    };


    class HTTPResponse : public Response, public HTTPMessage
    {
    public:
      HTTPResponse( uint16_t status_code );
      HTTPResponse( uint16_t status_code, Buffer& body ); // Steals this ref

      virtual ~HTTPResponse() { }

      uint16_t get_status_code() const { return status_code; }

      // Object
      HTTPResponse& inc_ref() { return Object::inc_ref( *this ); }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( HTTPResponse, 206 );

      // MarshallableObject
      void marshal( Marshaller& marshaller ) const;
      void unmarshal( Unmarshaller& ) { }

    private:
      friend class HTTPResponseParser;

      HTTPResponse
      (
        Buffer& header,
        const FieldOffsets& field_offsets,
        uint16_t status_code,
        Buffer* body
      );

      HTTPResponse( const HTTPResponse& ) { DebugBreak(); } // Prevent copying

      void init( uint16_t status_code );

    private:
      uint16_t status_code;
    };


    class HTTPResponseParser 
      : public HTTPMessageParser<HTTPResponseParser, HTTPResponse>
    {
    public:
      HTTPResponseParser();

    private:
      template <class, class> friend class HTTPMessageParser;

      // HTTPMessageParser downcalls
      HTTPResponse*
      createHTTPMessage
      (
        Buffer& header,
        const HTTPMessage::FieldOffsets& field_offsets,
        Buffer* body = NULL
      );

      ParseStatus parse_first_header_line( char** inout_p, const char* pe );

    private:
      uint16_t status_code;
    };


    class HTTPClient : public HTTPRequestHandler, public TCPSocketClient
    {
    public:
      HTTPClient
      (
        SocketAddress& peername, // Steals this reference
        TCPSocket& tcp_socket, // Steals this reference
        Configuration* configuration = NULL, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      virtual ~HTTPClient() { }

      static HTTPClient&
      create
      (
        const URI& absolute_uri,
        Configuration* configuration = NULL, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      static HTTPResponse& GET( const URI& absolute_uri );

      static 
      HTTPResponse& 
      PUT
      ( 
        const URI& absolute_uri,
        Buffer& body // Steals this reference
      );

      static
      HTTPResponse&
      PUT
      (
        const URI& absolute_uri,
        const Path& body_file_path
      );

      // Object
      HTTPClient& inc_ref() { return Object::inc_ref( *this ); }

      // RTTIObject
      const char* get_type_name() const { return "HTTPClient"; }

      // HTTPRequestHandler
      virtual void handle( HTTPRequest& http_request );

    protected:
      class Connection 
        : public TCPSocketClient::Connection,
          public HTTPRequestHandler,
          private HTTPResponseParser
      {
      public:
        Connection
        (
          SocketAddress& peername,
          TCPSocket& tcp_socket,
          Log* error_log = NULL,
          Log* trace_log = NULL
        );

        // RTTIObject
        const char* get_type_name() const { return "HTTPClient::Connection"; }

        // HTTPRequestHandler
        virtual void handle( HTTPRequest& http_request );

      protected:
        // Socket::AIOConnectCallback
        void onConnectCompletion( size_t bytes_written, void* context );
        void onConnectError( uint32_t error_code, void* context );

        // Socket::AIORecvCallback
        void onReadCompletion( Buffer& buffer, void* context );
        void onReadError( uint32_t error_code, void* context );

        // Socket::AIOSendCallback
        void onWriteCompletion( size_t bytes_written, void* context );
        void onWriteError( uint32_t error_code, void* context );

      private:
        std::deque<HTTPRequest*> outstanding_http_requests;
      };

    private:
      SynchronizedSTLQueue<Connection*> connections;
    };


    class HTTPServer : public TCPSocketServer
    {
    public:
      class AccessLog : public Object
      {
      public:
        class Format
        {
        public:
          virtual string
          operator()
          ( 
            const HTTPRequest&,
            const HTTPResponse& 
          ) const = 0;
        };

        class CommonFormat : public Format
        {
        public:
          virtual string
          operator()
          ( 
            const HTTPRequest&,
            const HTTPResponse& 
          ) const;
        };

        class CombinedFormat : public CommonFormat
        {
        public:
          string operator()( const HTTPRequest&, const HTTPResponse& ) const;
        };

      public:
        virtual ~AccessLog();

        static AccessLog&
        open
        ( 
          const Path& file_path,
          Format* format = NULL, // Steals this, defaults to CombinedFormat
          bool lazy_open = false 
        );

        static AccessLog&
        open
        ( 
          ostream&,
          Format* format = NULL // Steals this, defaults to CombinedFormat
        );

        virtual void write( const HTTPRequest&, const HTTPResponse& ) = 0;
    
      protected:
        AccessLog( Format& format );

        Format& get_format() { return format; }

      private:
        Format& format;
      };

    public:
      HTTPServer
      (
        EventHandler& http_request_handler, // Steals this reference
        TCPSocket& listen_tcp_socket,
        AccessLog* access_log = NULL,
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      virtual ~HTTPServer() { }

      static HTTPServer&
      create
      (
        EventHandler& http_request_handler, // Steals this reference
        const SocketAddress& sockname,
        AccessLog* access_log = NULL,
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      // Object
      HTTPServer& inc_ref() { return Object::inc_ref( *this ); }

    protected:
      // TCPSocket::AIOAcceptCallback
      virtual void
      onAcceptCompletion
      ( 
        StreamSocket& accepted_stream_socket, 
        void* context, 
        Buffer* recv_buffer 
      );

    protected:
      class Connection 
        : public TCPSocketServer::Connection,
          private HTTPRequestParser
      {
      public:
        Connection( HTTPServer& http_server, TCPSocket& tcp_socket );

        // RTTIObject
        virtual const char* get_type_name() const
        { 
          return "HTTPServer::Connection"; 
        }

        // TCPSocket::AIORecvCallback
        virtual void onReadCompletion( Buffer& buffer, void* context );

        // TCPSocket::AIOSendCallback
        virtual void onWriteCompletion( size_t bytes_written, void* context );
        virtual void onWriteError( uint32_t error_code, void* context );

        // ResponseHandler
        virtual void handle( Response& response );

      private:
        HTTPServer& http_server;
      };

    private:
      class FileAccessLog;
      class ostreamAccessLog;
      AccessLog* access_log;
    };


#ifdef YIELD_PLATFORM_HAVE_OPENSSL
    class HTTPSServer : public HTTPServer
    {
    public:
      HTTPSServer
      (
        EventHandler& http_request_handler, // Steals this reference
        SSLSocket& listen_ssl_socket,
        AccessLog* access_log = NULL,
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      static HTTPSServer&
      create
      (
        EventHandler& http_request_handler, // Steals this reference
        const SocketAddress& sockname,
        SSLContext& ssl_context,
        AccessLog* access_log = NULL,
        Log* error_log = NULL,
        Log* trace_log = NULL
      );
    };
#endif


    class JSONMarshaller : public Marshaller
    {
    public:
      JSONMarshaller( bool write_empty_strings = true );
      virtual ~JSONMarshaller();

      Buffer& get_buffer() const { return *buffer; }

      void write( const MarshallableObject& value ) { write( &value ); }
      virtual void write( const MarshallableObject* value );
      void write( const Sequence& value ) { write( &value ); }
      virtual void write( const Sequence* value );

      // Marshaller
      virtual void write( const Key& key, bool value );
      virtual void write( const Key& key, double value  );
      virtual void write( const Key& key, int64_t value );
      virtual void write( const Key& key, const MarshallableObject& value );
      virtual void write( const Key& key, const Sequence& value );
      virtual void write( const Key& key, const char* value, size_t );

    protected:
      JSONMarshaller( JSONMarshaller&, const Key& root_key );

      virtual void write( const Key& );

    private:
      void flushYAJLBuffer();

    private:
      Buffer* buffer;
      bool in_map;
      const Key* root_key;
      bool write_empty_strings;
      yajl_gen writer;
    };


    class JSONParser
    {
    public:
      class JSONValue : public Object
      {
      public:
        enum Type { TYPE_ARRAY, TYPE_NUMBER, TYPE_OBJECT, TYPE_STRING };
        virtual Type get_type() const { return TYPE_OBJECT; }
        JSONValue& inc_ref() { return Object::inc_ref( *this ); }
      };

      class JSONArray : public JSONValue, public vector<JSONValue*>
      {
      public:
        Type get_type() const { return TYPE_ARRAY; }
      };

      class JSONNumber : public JSONValue
      {
      public:
        JSONNumber( double value ) : value( value ) { }
        inline operator double() const { return value; }
        Type get_type() const { return TYPE_NUMBER; }
      private:
        double value;
      };

      class JSONString : public JSONValue
      {
      public:
        JSONString( Buffer&, const unsigned char*, unsigned int );
        Type get_type() const { return TYPE_STRING; }
        inline operator const unsigned char*() const { return value; }
        operator const char*() const;
        bool operator==( const char* ) const;
        inline unsigned int size() const { return value_len; }
      private:
        Buffer& underlying_buffer;
        const unsigned char* value;
        unsigned int value_len;
      };

      class JSONObject 
        : public JSONValue,
          public vector< pair<JSONString*, JSONValue*> >
      {
      public:
        JSONValue* operator[]( const char* name );
      };

      static JSONValue JSONfalse;
      static JSONValue JSONnull;
      static JSONValue JSONtrue;

    public:
      JSONParser();
      ~JSONParser();

      JSONValue* parse( Buffer& buffer );

    private:
      void handleJSONValue( JSONValue& json_value );

      // yajl callbacks
      static int handle_yajl_boolean( void*, int value );
      static int handle_yajl_double( void*, double value );
      static int handle_yajl_end_array( void* );
      static int handle_yajl_end_map( void* );
      static int handle_yajl_integer( void*, long value );
      static int handle_yajl_map_key( void*, const uint8_t*, unsigned int );
      static int handle_yajl_null( void* );
      static int handle_yajl_start_array( void* );
      static int handle_yajl_start_map( void* );
      static int handle_yajl_string( void*, const uint8_t*, unsigned int );

    private:
      Buffer* buffer;
      std::stack<JSONValue*> json_value_stack;
      JSONString* next_map_key;
      yajl_handle reader;      
    };


    class JSONUnmarshaller : public Unmarshaller
    {
    public:
      JSONUnmarshaller( const JSONParser::JSONValue& root_json_value );
      virtual ~JSONUnmarshaller() { }

      void read( Map& value );
      void read( MarshallableObject& value );
      void read( Sequence& value );

      // Unmarshaller
      virtual bool read_bool( const Key& key );
      virtual void read( const Key& key, double& value );
      virtual void read( const Key& key, int64_t& value );
      virtual Key* read( Key::Type );
      virtual void read( const Key& key, Map& value );
      virtual void read( const Key& key, MarshallableObject& value );
      virtual void read( const Key& key, Sequence& value );
      virtual void read( const Key& key, string& value );

    private:
      const JSONParser::JSONValue* read( const Key& key );

    private:
      const JSONParser::JSONValue& root_json_value;
      size_t next_json_value_i;
    };

  
    
    YIELD_IPC_ONCRPC_ERROR( ProgramUnavailable, 1, "program unavailable" );
    YIELD_IPC_ONCRPC_ERROR( ProgramMismatch, 2, "program mismatch" );
    YIELD_IPC_ONCRPC_ERROR( ProcedureUnavailable, 3, "procedure unavailable" );
    YIELD_IPC_ONCRPC_ERROR( GarbageArguments, 4, "garbage arguments" );    
    YIELD_IPC_ONCRPC_ERROR( System, 5, "system error" );
    YIELD_IPC_ONCRPC_ERROR( RPCMismatch, 6, "RPC version mismatch" );


    class ONCRPCMessage
    {
    public:
      enum auth_flavor { AUTH_NONE = 0, AUTH_SYS = 1, AUTH_SHORT = 2 };
      enum msg_type { CALL = 0, REPLY = 1 };

    public:
      virtual ~ONCRPCMessage();

      MarshallableObject* get_verf() const { return verf; }
      uint32_t get_xid() const { return xid; }

    protected:
      ONCRPCMessage( MarshallableObject* verf, uint32_t xid );

      void marshal_opaque_auth( Marshaller&, MarshallableObject* ) const;

    private:
      MarshallableObject* verf;
      uint32_t xid;
    };


    class ONCRPCMessageParser
    {
    protected:
      ONCRPCMessageParser( MessageFactory& message_factory );
      ~ONCRPCMessageParser();

      MessageFactory& get_message_factory() const { return message_factory; }
      MarshallableObject* unmarshal_opaque_auth( XDRUnmarshaller& );

    private:
      MessageFactory& message_factory;
    };


    class ONCRPCRecordFragment : public yidl::runtime::HeapBuffer
    {
    public:
      ONCRPCRecordFragment( uint32_t nbo_marker );
      ONCRPCRecordFragment( Buffer& buffer, bool is_last = true );

      bool is_last() const;

    private:
      uint32_t hbo_marker;
    };


    class ONCRPCRequest : public Request, public ONCRPCMessage
    {
    public:
      ONCRPCRequest
      (
        Request& body, // Steals this reference
        uint32_t proc,
        uint32_t prog,
        uint32_t vers,
        uint32_t xid,
        MarshallableObject* cred = NULL, // = AUTH_NONE, steals this reference
        MarshallableObject* verf = NULL // = AUTH_NONE, steals this reference
      );

      virtual ~ONCRPCRequest();

      Request& get_body() const { return body; }
      MarshallableObject* get_cred() const { return cred; }
      uint32_t get_prog() const { return prog; }
      uint32_t get_proc() const { return proc; }      
      uint32_t get_vers() const { return vers; }
      void respond( ONCRPCResponse& response ); // Steals this reference
      void respond( Response& response ); // Steals this reference
      void respond( Exception& response ); // Steals this reference

      // Object
      ONCRPCRequest& inc_ref() { return Object::inc_ref( *this ); }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ONCRPCRequest, 213 );

      // MarshallableObject
      void marshal( Marshaller& marshaller ) const;
      void unmarshal( Unmarshaller& unmarshaller ) { }

    private:      
      Request& body;
      MarshallableObject* cred;
      uint32_t proc, prog, vers;
    };


    class ONCRPCRequestHandler : public RequestHandler
    {
    public:
      virtual void handle( ONCRPCRequest& onc_rpc_request ) = 0;

      // RequestHandler
      virtual void handle( Request& request );
    };


    class ONCRPCRequestParser : public ONCRPCMessageParser
    {
    public:
      ONCRPCRequestParser( MessageFactory& );

      Message* parse( ONCRPCRecordFragment& record_fragment );
    };


    class ONCRPCResponse : public Response, public ONCRPCMessage
    {
    public:
      enum accept_stat 
      {
         SUCCESS       = 0,
         PROG_UNAVAIL  = 1,
         PROG_MISMATCH = 2,
         PROC_UNAVAIL  = 3,
         GARBAGE_ARGS  = 4,
         SYSTEM_ERR    = 5
      };

      enum auth_stat 
      {
         AUTH_OK           = 0,
         AUTH_BADCRED      = 1,
         AUTH_REJECTEDCRED = 2,
         AUTH_BADVERF      = 3,
         AUTH_REJECTEDVERF = 4,
         AUTH_TOOWEAK      = 5,
         AUTH_INVALIDRESP  = 6,
         AUTH_FAILED       = 7
      };

      struct mismatch_info
      {
        uint32_t low;
        uint32_t high;
      };

      enum reject_stat { RPC_MISMATCH = 0, AUTH_ERROR = 1 };
      enum reply_stat { MSG_ACCEPTED = 0, MSG_DENIED = 1 };

    public:
      // Accepted reply (MSG_ACCEPTED) - SUCCESS accept_stat
      ONCRPCResponse
      ( 
        Response& body, // Steals this reference
        uint32_t xid,
        MarshallableObject* verf = NULL // = AUTH_NONE, steals this reference
      );

      // Accepted reply (MSG_ACCEPTED) - other accept_stat = body.get_type_id()
      ONCRPCResponse
      (
        Exception& body, // Steals this reference
        uint32_t xid,
        MarshallableObject* verf = NULL // = AUTH_NONE, steals this reference
      );

      // Rejected reply (MSG_DENIED) - RPC_MISMATCH
      ONCRPCResponse( const struct mismatch_info&, uint32_t xid );

      // Rejected reply (MSG_DENIED ) - AUTH_ERROR
      ONCRPCResponse( auth_stat, uint32_t xid );

      virtual ~ONCRPCResponse();

      Response& get_body() const { return body; }

      // Object
      ONCRPCResponse& inc_ref() { return Object::inc_ref( *this ); }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( ONCRPCResponse, 208 );

      // MarshallableObject
      void marshal( Marshaller& marshaller ) const;
      void unmarshal( Unmarshaller& unmarshaller ) { }

    private:
      uint32_t accept_stat_;
      auth_stat auth_stat_;
      Response& body;
      struct mismatch_info mismatch_info_;
      reject_stat reject_stat_;
      reply_stat reply_stat_;
    };


    class ONCRPCResponseParser : public ONCRPCMessageParser
    {
    public:
      ONCRPCResponseParser( MessageFactory& );

      ONCRPCResponse* parse( ONCRPCRecordFragment&, ONCRPCRequest& );

      ONCRPCResponse*
      parse
      ( 
        uint32_t default_body_type_id,
        ONCRPCRecordFragment& record_fragment,
        uint32_t xid
      );
    };


    class ONCRPCAuthError : public Exception
    {
    public:
      const static uint32_t ERROR_CODE = 7;

      ONCRPCAuthError( ONCRPCResponse::auth_stat auth_stat_ ) 
        : Exception( ERROR_CODE, "ONC-RPC: auth error" ),
          auth_stat_( auth_stat_ )
      { }

      ONCRPCResponse::auth_stat get_auth_stat() const { return auth_stat_; }

    private:
      ONCRPCResponse::auth_stat auth_stat_;
    };


    class ONCRPCClient 
      : public ONCRPCRequestHandler,
        public RPCClient<ONCRPCRequest, ONCRPCResponse>
    {
    public:
      virtual ~ONCRPCClient();

      // RequestHandler
      virtual void handle( Request& request );

      // ONCRPCRequestHandler
      virtual void handle( ONCRPCRequest& onc_rpc_request ) = 0;

    protected:
      ONCRPCClient
      (
        MarshallableObject* cred,
        MessageFactory& message_factory,
        uint32_t prog,
        uint32_t vers
      );

      virtual MarshallableObject* get_cred() { return cred; }
      uint32_t get_prog() const { return prog; }
      uint32_t get_vers() const { return vers; }
    
    private:
      MarshallableObject* cred;
      uint32_t prog, vers;
    };


    typedef RPCServer<ONCRPCRequest, ONCRPCResponse> ONCRPCServer;


    class ONCRPCStreamSocketClient 
      : public ONCRPCClient,
        public StreamSocketClient
    {
    public:
      ONCRPCStreamSocketClient
      (
        MessageFactory& message_factory, // Steals this reference
        SocketAddress& peername, // Steals this reference
        uint32_t prog,
        StreamSocket& stream_socket, // Steals this reference
        uint32_t vers,
        Configuration* configuration = NULL, // Steals this reference
        MarshallableObject* cred = NULL, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      virtual ~ONCRPCStreamSocketClient();

      // RTTIObject
      virtual const char* get_type_name() const
      { 
        return "ONCRPCStreamSocketClient"; 
      }

    protected:
      // ONCRPCClient
      virtual void handle( ONCRPCRequest& onc_rpc_request );
    
    protected:
      class Connection
        : public StreamSocketClient::Connection,
          public ONCRPCRequestHandler,
          private ONCRPCResponseParser
      {
      public:
        Connection( ONCRPCStreamSocketClient&, StreamSocket& );

        // RTTIObject
        const char* get_type_name() const
        { 
          return "ONCRPCStreamSocketClient::Connection"; 
        }

        // ONCRPCRequestHandler
        void handle( ONCRPCRequest& onc_rpc_request );

      protected:
        // StreamSocket::AIOConnectCallback
        void onConnectCompletion( size_t bytes_written, void* context );
        void onConnectError( uint32_t error_code, void* context );

        // StreamSocket::AIORecvCallback
        void onReadCompletion( Buffer& buffer, void* context );
        void onReadError( uint32_t error_code, void* context );

        // StreamSocket::AIOSendCallback
        void onWriteCompletion( size_t bytes_written, void* context );
        void onWriteError( uint32_t error_code, void* context );

      private:
        Time connect_timeout;
        map<uint32_t, ONCRPCRequest*> outstanding_onc_rpc_requests;
        Time recv_timeout;
        Time send_timeout;
        ONCRPCStreamSocketClient& onc_rpc_stream_socket_client;
      };

    private:    
      SynchronizedSTLQueue<Connection*> connections;
    };


    class ONCRPCStreamSocketServer 
      : public ONCRPCServer, 
        public StreamSocketServer
    {
    public:
      ONCRPCStreamSocketServer
      ( 
        StreamSocket& listen_stream_socket, // Steals this reference
        MessageFactory& message_factory, // Steals this reference
        EventHandler& onc_rpc_request_handler, // Steals this reference
        Log* error_log = NULL,
        bool send_onc_rpc_requests = false,
        Log* trace_log = NULL
      );

      virtual ~ONCRPCStreamSocketServer() { }

      // RTTIObject
      virtual const char* get_type_name() const
      { 
        return "ONCRPCStreamSocketServer"; 
      }

    private:
      // StreamSocket::AIOAcceptCallback
      void onAcceptCompletion( StreamSocket&, void*, Buffer* );

    private:
      class Connection 
        : public StreamSocketServer::Connection,
          private ONCRPCRequestParser
      {
      public:
        Connection( ONCRPCStreamSocketServer&, StreamSocket& );

        // RTTIObject
        virtual const char* get_type_name() const
        {
          return "ONCRPCStreamSocketServer::Connection";
        }

      private:
        // StreamSocket::AIORecvCallback
        void onReadCompletion( Buffer& buffer, void* context );

        // StreamSocket::AIOSendCallback
        void onWriteCompletion( size_t bytes_written, void* context );

        // ResponseHandler
        void handle( Response& response );

      private:
        ONCRPCStreamSocketServer& onc_rpc_stream_socket_server;
      };
    };


#ifdef YIELD_PLATFORM_HAVE_OPENSSL
    class ONCRPCSSLSocketClient : public ONCRPCStreamSocketClient
    {
    public:
      ONCRPCSSLSocketClient
      (
        MessageFactory& message_factory, // Steals this reference
        SocketAddress& peername, // Steals this reference
        uint32_t prog,
        SSLSocket& ssl_socket, // Steals this reference
        uint32_t vers,
        Configuration* configuration = NULL, // Steals this reference
        MarshallableObject* cred = NULL, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      virtual ~ONCRPCSSLSocketClient() { }

      static ONCRPCSSLSocketClient&
      create
      (
        const URI& absolute_uri,
        MessageFactory& message_factory, // Steals this reference
        SSLContext& ssl_context,
        uint32_t prog,
        uint32_t vers,
        Configuration* configuration = NULL, // Steals this reference
        MarshallableObject* cred = NULL, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      // RTTIObject
      virtual const char* get_type_name() const
      { 
        return "ONCRPCSSLSocketClient"; 
      }    
    };


    class ONCRPCSSLSocketServer : public ONCRPCStreamSocketServer
    {
    public:
      ONCRPCSSLSocketServer
      ( 
        SSLSocket& listen_ssl_socket, // Steals this reference
        MessageFactory& message_factory, // Steals this reference
        EventHandler& onc_rpc_request_handler, // Steals this reference
        Log* error_log = NULL,
        bool send_onc_rpc_requests = false,
        Log* trace_log = NULL
      );

      static ONCRPCSSLSocketServer&
      create
      (        
        MessageFactory& message_factory,
        EventHandler& onc_rpc_request_handler,
        const SocketAddress& sockname,
        SSLContext& ssl_context,
        Log* error_log = NULL,
        bool send_onc_rpc_requests = false,
        Log* trace_log = NULL
      );

      // RTTIObject
      virtual const char* get_type_name() const
      { 
        return "ONCRPCSSLSocketServer"; 
      }
    };
#endif


    class ONCRPCTCPSocketClient : public ONCRPCStreamSocketClient
    {
    public:
      ONCRPCTCPSocketClient
      (
        MessageFactory& message_factory, // Steals this reference
        SocketAddress& peername, // Steals this reference
        uint32_t prog,
        TCPSocket& tcp_socket, // Steals this reference
        uint32_t vers,
        Configuration* configuration = NULL, // Steals this reference
        MarshallableObject* cred = NULL, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      virtual ~ONCRPCTCPSocketClient() { }

      static ONCRPCTCPSocketClient&
      create
      (
        const URI& absolute_uri,
        MessageFactory& message_factory, // Steals this reference
        uint32_t prog,
        uint32_t vers,
        Configuration* configuration = NULL, // Steals this reference
        MarshallableObject* cred = NULL, // Steals this reference
        Log* error_log = NULL,
        Log* trace_log = NULL
      );

      // RTTIObject
      virtual const char* get_type_name() const
      { 
        return "ONCRPCTCPSocketClient"; 
      }    
    };


    class ONCRPCTCPSocketServer : public ONCRPCStreamSocketServer
    {
    public:
      ONCRPCTCPSocketServer
      ( 
        TCPSocket& listen_tcp_socket, // Steals this reference
        MessageFactory& message_factory, // Steals this reference
        EventHandler& onc_rpc_request_handler, // Steals this reference
        Log* error_log = NULL,
        bool send_onc_rpc_requests = false,
        Log* trace_log = NULL
      );

      static ONCRPCTCPSocketServer&
      create
      (
        MessageFactory& message_factory,
        EventHandler& onc_rpc_request_handler,
        const SocketAddress& sockname,
        Log* error_log = NULL,
        bool send_onc_rpc_requests = false,
        Log* trace_log = NULL
      );

      // RTTIObject
      virtual const char* get_type_name() const
      { 
        return "ONCRPCTCPSocketServer"; 
      }
    };


    class ONCRPCUDPSocketClient 
      : public ONCRPCClient,
        public UDPSocketClient,        
        private ONCRPCResponseParser
    {
    public:
      ONCRPCUDPSocketClient
      (
        MessageFactory& message_factory, // Steals this reference
        SocketAddress& peername, // Steals this reference
        uint32_t prog,
        UDPSocket& udp_socket, // Steals this reference
        uint32_t vers,
        MarshallableObject* cred = NULL,
        Log* error_log = NULL,
        const Time& recv_timeout = RECV_TIMEOUT_DEFAULT,
        Log* trace_log = NULL
      );

      static ONCRPCUDPSocketClient&
      create
      (
        const URI& absolute_uri,
        MessageFactory& message_factory, // Steals this reference
        uint32_t prog,
        uint32_t vers,
        MarshallableObject* cred = NULL,
        Log* error_log = NULL,        
        const Time& recv_timeout = RECV_TIMEOUT_DEFAULT,
        Log* trace_log = NULL
      );

      // RTTIObject
      const char* get_type_name() const { return "ONCRPCUDPSocketClient"; }

    protected:
      // ONCRPCClient
      virtual void handle( ONCRPCRequest& onc_rpc_request );

      // UDPSocket::AIORecvCallback
      void onReadCompletion( Buffer& buffer, void* context );
      void onReadError( uint32_t error_code, void* context );
    };


    class ONCRPCUDPSocketServer 
      : public ONCRPCServer,
        public UDPSocketServer,
        private ONCRPCRequestParser
    {
    public:
      ONCRPCUDPSocketServer
      ( 
        MessageFactory& message_factory,
        EventHandler& onc_rpc_request_handler,
        UDPSocket& udp_socket,
        Log* error_log = NULL,
        bool send_onc_rpc_requests = false,
        Log* trace_log = NULL
      );

      static ONCRPCUDPSocketServer&
      create
      (
        MessageFactory& message_factory,
        EventHandler& onc_rpc_request_handler,
        const SocketAddress& sockname,
        Log* error_log = NULL,
        bool send_onc_rpc_requests = false,
        Log* trace_log = NULL
      );

      // RTTIObject
      const char* get_type_name() const { return "ONCRPCUDPSocketServer"; }

    private:
      // UDPSocket::AIORecvFromCallback
      void onRecvFromCompletion( Buffer&, SocketAddress& peername, void* );
      void onRecvFromError( uint32_t error_code, void* context );

    private:
      class ResponseHandler;
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

      // StreamSocket
      StreamSocket* dup();
      bool want_connect() const;
      bool want_recv() const;
      bool want_send() const;

    private:
      TracingTCPSocket( int domain, Log& log, socket_t );

      // StreamSocket
      StreamSocket* dup2( socket_t );

    private:
      Log& log;
    };


    class URI : public Object
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
      operator std::string() const;

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
  };
};


#endif
