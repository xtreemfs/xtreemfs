// Copyright 2003-2008 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _YIELD_IPC_H
#define _YIELD_IPC_H

#include "yield/concurrency.h"

#ifdef YIELD_HAVE_OPENSSL
#include <openssl/ssl.h>
#endif
#ifdef YIELD_HAVE_ZLIB
#ifdef _WIN32
#undef ZLIB_WINAPI // So zlib doesn't #include windows.h
#pragma comment( lib, "zdll.lib" )
#endif
#include "zlib.h"
#endif


struct addrinfo;
struct sockaddr;
struct sockaddr_storage;
struct UriUriStructA;
struct yajl_gen_t;
typedef struct yajl_gen_t* yajl_gen;


#define YIELD_RFC822_HEADERS_STACK_BUFFER_LENGTH 128
#define YIELD_RFC822_HEADERS_STACK_IOVECS_LENGTH 32


#define YIELD_SOCKET_PROTOTYPES \
virtual void aio_read( auto_Object<AIOReadControlBlock> aio_read_control_block ); \
virtual void aio_write( auto_Object<AIOWriteControlBlock> aio_write_control_block ); \
virtual bool bind( auto_SocketAddress to_sockaddr ); \
virtual bool close(); \
virtual bool connect( auto_SocketAddress to_sockaddr ); \
virtual bool get_blocking_mode() const; \
virtual auto_SocketAddress getpeername(); \
virtual auto_SocketAddress getsockname(); \
virtual operator int() const; \
virtual ssize_t read( void* buffer, size_t buffer_len ); \
virtual bool set_blocking_mode( bool blocking ); \
virtual bool shutdown(); \
virtual bool want_connect() const; \
virtual bool want_read() const; \
virtual bool want_write() const; \
virtual ssize_t writev( const struct iovec* buffers, uint32_t buffers_count );


namespace YIELD
{
  class HTTPRequest;
  class ONCRPCRequest;
  class Socket;
  class URI;


#ifdef YIELD_HAVE_ZLIB
  class Diefleder
  {
  public:
    static auto_Buffer deflate( auto_Buffer buffer, int level = Z_BEST_COMPRESSION )
    {
      z_stream zstream;
      zstream.zalloc = Z_NULL;
      zstream.zfree = Z_NULL;
      zstream.opaque = Z_NULL;

      if ( deflateInit( &zstream, level ) == Z_OK )
      {
        zstream.next_in = static_cast<Bytef*>( *buffer );
        zstream.avail_in = buffer->size();

        Bytef zstream_out[4096];
        zstream.next_out = zstream_out;
        zstream.avail_out = 4096;

        auto_Buffer out_buffer( new StringBuffer );

        while ( ::deflate( &zstream, Z_NO_FLUSH ) == Z_OK )
        {
          if ( zstream.avail_out == 0 ) // Filled zstream_out, copy it into out_buffer and keep deflating
          {
            out_buffer->put( zstream_out, sizeof( zstream_out ) );
            zstream.next_out = zstream_out;
            zstream.avail_out = sizeof( zstream_out );
          }
          else // deflate returned Z_OK without filling zstream_out -> done
          {
            int deflate_ret;
            while ( ( deflate_ret = ::deflate( &zstream, Z_FINISH ) ) == Z_OK ) // Z_OK = need more buffer space to finish compression, Z_STREAM_END = really done
            {
              out_buffer->put( zstream_out, sizeof( zstream_out ) );
              zstream.next_out = zstream_out;              
              zstream.avail_out = sizeof( zstream_out );
            }

            if ( deflate_ret == Z_STREAM_END )
            {
              if ( deflateEnd( &zstream ) == Z_OK ) // Deallocate zstream
              {
                if ( zstream.avail_out < sizeof( zstream_out ) )
                  out_buffer->put( zstream_out, sizeof( zstream_out ) - zstream.avail_out );

                return out_buffer;
              }
              else
                return NULL;
            }
            else
              break;
          }
        }

        deflateEnd( &zstream ); // Deallocate ztream
      }

      return NULL;
    }
  };
#endif


  class SocketAddress : public Object
  {
  public:
    SocketAddress( struct addrinfo& addrinfo_list ); // Takes ownership of addrinfo_list
    SocketAddress( const struct sockaddr_storage& _sockaddr_storage ); // Copies _sockaddr_storage

    static auto_Object<SocketAddress> create( const char* hostname ) { return create( hostname, 0 ); }
    static auto_Object<SocketAddress> create( const char* hostname, uint16_t port ); // hostname can be NULL for INADDR_ANY
    static auto_Object<SocketAddress> create( const URI& );

#ifdef _WIN32
    bool as_struct_sockaddr( int family, struct sockaddr*& out_sockaddr, int32_t& out_sockaddrlen );
#else
    bool as_struct_sockaddr( int family, struct sockaddr*& out_sockaddr, uint32_t& out_sockaddrlen );
#endif
    bool getnameinfo( std::string& out_hostname, bool numeric = true ) const;
    bool getnameinfo( char* out_hostname, uint32_t out_hostname_len, bool numeric = true ) const;
    uint16_t get_port() const;
    bool operator==( const SocketAddress& ) const;
    bool operator!=( const SocketAddress& other ) const { return !operator==( other ); }

    // Object
    YIELD_OBJECT_PROTOTYPES( SocketAddress, 210 );

  private:
    SocketAddress( const SocketAddress& ) { DebugBreak(); } // Prevent copying
    ~SocketAddress();

    struct addrinfo* addrinfo_list; // Multiple sockaddr's obtained from getaddrinfo(3)
    struct sockaddr_storage* _sockaddr_storage; // A single sockaddr passed in the constructor and copied

    static struct addrinfo* getaddrinfo( const char* hostname, uint16_t port );
  };

  typedef auto_Object<SocketAddress> auto_SocketAddress;


  class Socket : public Event
  {
  public:
    class AIOControlBlock : public ::YIELD::AIOControlBlock
    {
    public:
      auto_Object<Socket> get_socket() const { return socket_; }

      void set_socket( Socket& socket_ ) 
      { 
        if ( this->socket_ == NULL ) 
          this->socket_ = socket_.incRef(); 
      }

    protected:
      AIOControlBlock() { }

    private:
      auto_Object<Socket> socket_;
    };


    class AIOConnectControlBlock : public AIOControlBlock
    {
    public:
      AIOConnectControlBlock( auto_SocketAddress peername )
        : peername( peername )
      { }

      auto_SocketAddress get_peername() const { return peername; }

      // Object
      YIELD_OBJECT_PROTOTYPES( AIOConnectControlBlock, 223 );

    private:
      auto_SocketAddress peername;
    };


    class AIOReadControlBlock : public AIOControlBlock
    {
    public:
      AIOReadControlBlock( auto_Buffer buffer )
        : buffer( buffer )
      { }

      auto_Buffer get_buffer() const { return buffer; }

      // Object
      YIELD_OBJECT_PROTOTYPES( AIOReadControlBlock, 227 );

    private:
      auto_Buffer buffer;
    };


    class AIOWriteControlBlock : public AIOControlBlock
    {
    public:
      AIOWriteControlBlock( auto_Buffer buffer )
        : buffer( buffer )
      { }

      auto_Buffer get_buffer() const { return buffer; }

      // Object
      YIELD_OBJECT_PROTOTYPES( AIOWriteControlBlock, 228 );

    private:
      auto_Buffer buffer;
    };


    virtual void aio_connect( auto_Object<AIOConnectControlBlock> aio_connect_control_block );
    static void destroy();
    int get_domain() const { return domain; }
    int get_protocol() const { return protocol; }
    int get_type() const { return type; }
    static void init();
    bool operator==( const Socket& other ) const { return static_cast<int>( *this ) == static_cast<int>( other ); } \
    virtual ssize_t read( auto_Buffer buffer );
    virtual ssize_t write( auto_Buffer buffer );
    virtual ssize_t write( const void* buffer, size_t buffer_len );
    YIELD_SOCKET_PROTOTYPES;

    // Object
    YIELD_OBJECT_PROTOTYPES( Socket, 211 );

  protected:
    friend class TracingSocket;

    Socket( int domain, int type, int protocol, int socket_ );
    virtual ~Socket();

#ifdef _WIN32
    void aio_read_iocp( auto_Object<AIOReadControlBlock> aio_read_control_block );
    void aio_write_iocp( auto_Object<AIOWriteControlBlock> aio_write_control_block );
#endif
    void aio_connect_nbio( auto_Object<AIOConnectControlBlock> aio_connect_control_block );
    void aio_read_nbio( auto_Object<AIOReadControlBlock> aio_read_control_block );
    void aio_write_nbio( auto_Object<AIOWriteControlBlock> aio_write_control_block );
    static int create( int& domain, int type, int protocol );

    class AIOQueue
    {
    public:
      AIOQueue();
      ~AIOQueue();

#ifdef _WIN32
      void associate( Socket& );
#endif
      void submit( auto_Object<AIOControlBlock> aio_control_block );

    private:
#ifdef _WIN32
     void* hIoCompletionPort;

     class IOCPWorkerThread;
     std::vector<IOCPWorkerThread*> iocp_worker_threads;
#endif

     class NBIOWorkerThread;
     std::vector<NBIOWorkerThread*> nbio_worker_threads;
    };

    AIOQueue& get_aio_queue();

    int domain, socket_;

  private:
    Socket( const Socket& ) { DebugBreak(); } // Prevent copying

    int type, protocol;

    static AIOQueue* aio_queue;
    bool blocking_mode;
  };

  typedef auto_Object<Socket> auto_Socket;


  class TCPSocket : public Socket
  {
  public:
   class AIOAcceptControlBlock : public AIOControlBlock
   {
    public:
      AIOAcceptControlBlock()
      { }

      auto_Object<TCPSocket> get_accepted_tcp_socket() const { return accepted_tcp_socket; }

      // Object
      YIELD_OBJECT_PROTOTYPES( AIOAcceptControlBlock, 222 );

    private:
      friend class Socket;

      auto_Object<TCPSocket> accepted_tcp_socket;      
#ifdef _WIN32
      friend class TCPSocket;
      char peer_sockaddr[88];
#endif
    };

    virtual void aio_accept( auto_Object<AIOAcceptControlBlock> aio_accept_control_block );
    virtual void aio_connect( auto_Object<AIOConnectControlBlock> aio_connect_control_block );
    static auto_Object<TCPSocket> create(); // Defaults to domain = AF_INET6
    static auto_Object<TCPSocket> create( int domain );
    virtual auto_Object<TCPSocket> accept();
    virtual bool listen();
    virtual bool shutdown();
    virtual ssize_t writev( const struct iovec* buffers, uint32_t buffers_count );

    // Object
    YIELD_OBJECT_PROTOTYPES( TCPSocket, 212 );

  protected:
    TCPSocket( int domain, int socket_ );
    virtual ~TCPSocket() { }

    int _accept();

    // Socket
#ifdef _WIN32
    void aio_accept_iocp( auto_Object<AIOAcceptControlBlock> aio_accept_control_block );
    void aio_connect_iocp( auto_Object<AIOConnectControlBlock> aio_connect_control_block );
#endif
    void aio_accept_nbio( auto_Object<AIOAcceptControlBlock> aio_accept_control_block );

  private:
#ifdef _WIN32
    static void *lpfnAcceptEx, *lpfnConnectEx;
#endif

    size_t partial_write_len;
  };

  typedef auto_Object<TCPSocket> auto_TCPSocket;


  class SSLContext : public Object
  {
  public:
#ifdef YIELD_HAVE_OPENSSL
    static auto_Object<SSLContext> create( SSL_METHOD* method = SSLv23_client_method() ); // No certificate
    static auto_Object<SSLContext> create( SSL_METHOD* method, const Path& pem_certificate_file_path, const Path& pem_private_key_file_path, const std::string& pem_private_key_passphrase );
    static auto_Object<SSLContext> create( SSL_METHOD* method, const std::string& pem_certificate_str, const std::string& pem_private_key_str, const std::string& pem_private_key_passphrase );
    static auto_Object<SSLContext> create( SSL_METHOD* method, const Path& pkcs12_file_path, const std::string& pkcs12_passphrase );
#else
    static auto_Object<SSLContext> create();
#endif

#ifdef YIELD_HAVE_OPENSSL
    SSL_CTX* get_ssl_ctx() const { return ctx; }
#endif

    // Object
    YIELD_OBJECT_PROTOTYPES( SSLContext, 215 );

  private:
#ifdef YIELD_HAVE_OPENSSL
    SSLContext( SSL_CTX* ctx );
#else
    SSLContext();
#endif
    ~SSLContext();

#ifdef YIELD_HAVE_OPENSSL    
    static SSL_CTX* createSSL_CTX( SSL_METHOD* method );
    SSL_CTX* ctx;
#endif
	};

  typedef auto_Object<SSLContext> auto_SSLContext;


#ifdef YIELD_HAVE_OPENSSL

  class SSLSocket : public TCPSocket
  {
  public:
    static auto_Object<SSLSocket> create( auto_SSLContext ctx ); // Defaults to domain = AF_INET6
    static auto_Object<SSLSocket> create( int domain, auto_SSLContext ctx );

    // Object
    YIELD_OBJECT_PROTOTYPES( SSLSocket, 216 );

    // Socket
    void aio_read( auto_Object<AIOReadControlBlock> aio_read_control_block );
    void aio_write( auto_Object<AIOWriteControlBlock> aio_write_control_block );
    ssize_t read( void* buffer, size_t buffer_len );
    bool want_read() const;
    bool want_write() const;
    ssize_t write( const void* buffer, size_t buffer_len );
    ssize_t writev( const struct iovec* buffers, uint32_t buffers_count );

    // TCPSocket
    auto_TCPSocket accept();
    void aio_accept( auto_Object<AIOAcceptControlBlock> aio_accept_control_block );
    bool connect( auto_SocketAddress peername );
    bool shutdown();

  private:
    SSLSocket( int domain, int socket_, auto_SSLContext ctx, SSL* ssl );
    ~SSLSocket();

    auto_SSLContext ctx;
    SSL* ssl;
  };

  typedef auto_Object<SSLSocket> auto_SSLSocket;

#endif

  class TracingSocket : public Socket
  {
  public:
    TracingSocket( auto_Socket underlying_socket, auto_Log log );
  
    // Object
    virtual uint32_t get_type_id() const { return underlying_socket->get_type_id(); }
    const char* get_type_name() const { return underlying_socket->get_type_name(); }

    // Socket
    void aio_connect( auto_Object<AIOConnectControlBlock> aio_connect_control_block );
    YIELD_SOCKET_PROTOTYPES;

  private:
    ~TracingSocket() { }

    auto_Socket underlying_socket;
    auto_Log log;
  };


  class UDPSocket : public Socket
  {
  public:
    class AIORecvFromControlBlock : public AIOControlBlock
    {
    public:
      AIORecvFromControlBlock( auto_Buffer buffer );

      auto_Buffer get_buffer() const { return buffer; }
      auto_SocketAddress get_peer_sockaddr() const;

      // Object
      YIELD_OBJECT_PROTOTYPES( UDPSocket::AIORecvFromControlBlock, 0 );

    protected:
      ~AIORecvFromControlBlock();

    private:
      auto_Buffer buffer;

      friend class UDPSocket;
      struct sockaddr_storage* peer_sockaddr;
    };


    void aio_recvfrom( auto_Object<AIORecvFromControlBlock> aio_recvfrom_control_block );
    static auto_Object<UDPSocket> create();    
    ssize_t recvfrom( auto_Buffer buffer, struct sockaddr_storage& peer_sockaddr );
    ssize_t recvfrom( void* buffer, size_t buffer_len, struct sockaddr_storage& peer_sockaddr );
    ssize_t sendto( auto_Buffer buffer, auto_SocketAddress peer_sockaddr );
    ssize_t sendto( const void* buffer, size_t buffer_len, auto_SocketAddress peer_sockaddr );

    // Object
    YIELD_OBJECT_PROTOTYPES( UDPSocket, 219 );

  protected:
#ifdef _WIN32
    void aio_recvfrom_iocp( auto_Object<AIORecvFromControlBlock> aio_recvfrom_control_block );
#endif
    void aio_recvfrom_nbio( auto_Object<AIORecvFromControlBlock> aio_recvfrom_control_block );

  private:
    UDPSocket( int domain, int socket_ );    
    ~UDPSocket() { }
  };

  typedef auto_Object<UDPSocket> auto_UDPSocket;


  template <class RequestType, class ResponseType>
  class Client
  {
  public:    
    const static uint32_t CLIENT_FLAG_TRACE_IO = 1;
    const static uint32_t CLIENT_FLAG_TRACE_OPERATIONS = 2;

    const static uint64_t OPERATION_TIMEOUT_DEFAULT = 5 * NS_IN_S;
    
    // EventHandler
    virtual void handleEvent( Event& );

  protected:
    Client( const URI& absolute_uri, uint32_t flags, auto_Log log, const Time& operation_timeout, auto_SocketAddress peername, auto_SSLContext ssl_context );
    virtual ~Client();

    uint32_t get_flags() const { return flags; }
    auto_Log get_log() const { return log; }

  private:
    auto_Object<URI> absolute_uri;
    uint32_t flags;
    auto_Log log;
    Time operation_timeout;
    auto_SocketAddress peername;
    auto_SSLContext ssl_context;
    
    SynchronizedSTLQueue<Socket*> idle_sockets;

    class AIOConnectControlBlock;
    class AIOReadControlBlock;
    class AIOWriteControlBlock;
    
    class OperationTimer;
  };


  class RFC822Headers
  {
  public:
    RFC822Headers( uint8_t reserve_iovecs_count = 0 );
    virtual ~RFC822Headers();

    ssize_t deserialize( auto_Buffer );
    char* get_header( const char* header_name, const char* default_value="" );
    char* operator[]( const char* header_name ) { return get_header( header_name ); }
    // void set_header( const char* header, size_t header_len ); // Mutable header with name: value in one string, will copy both
    void set_header( const char* header_name, const char* header_value ); // Literal header
    void set_header( const char* header_name, char* header_value ); // Mutable header, will copy value
    void set_header( char* header_name, char* header_value ); // Mutable name and mutable value, will copy both
    void set_header( const std::string& header_name, const std::string& header_value ); // Mutable name and mutable value, will copy both
    auto_Buffer serialize();

  protected:
    void set_iovec( uint8_t iovec_i, const char* data, size_t len );
    void set_next_iovec( char* data, size_t len ); // Copies data
    void set_next_iovec( const char* data, size_t len ); // Does not copy data
    void set_next_iovec( const struct iovec& out_iovec );

  private:
    enum { DESERIALIZING_LEADING_WHITESPACE, DESERIALIZING_HEADER_NAME, DESERIALIZING_HEADER_NAME_VALUE_SEPARATOR, DESERIALIZING_HEADER_VALUE, DESERIALIZING_HEADER_VALUE_TERMINATOR, DESERIALIZING_TRAILING_CRLF, DESERIALIZE_DONE } deserialize_state;
    char stack_buffer[YIELD_RFC822_HEADERS_STACK_BUFFER_LENGTH], *heap_buffer, *buffer_p;
    size_t heap_buffer_len;
    iovec stack_iovecs[YIELD_RFC822_HEADERS_STACK_IOVECS_LENGTH], *heap_iovecs; uint8_t iovecs_filled;

    inline void advanceBufferPointer()
    {
      buffer_p++;

      if ( heap_buffer == NULL )
      {
        if ( buffer_p - stack_buffer < YIELD_RFC822_HEADERS_STACK_BUFFER_LENGTH )
          return;
        else
          allocateHeapBuffer();
      }
      else if ( static_cast<size_t>( buffer_p - heap_buffer ) < heap_buffer_len )
        return;
      else
        allocateHeapBuffer();
    }

    void allocateHeapBuffer();
  };


  class HTTPBenchmarkDriver : public EventHandler
  {
  public:
    static auto_Object<HTTPBenchmarkDriver> create( auto_EventTarget http_request_target, uint8_t in_flight_request_count, const Path& wlog_file_path, uint32_t wlog_uris_length_max = static_cast<uint32_t>( -1 ), uint8_t wlog_repetitions_count = 1 );
    virtual ~HTTPBenchmarkDriver();

    void get_request_rates( std::vector<double>& out_request_rates );
    void get_response_rates( std::vector<double>& out_response_rates );
    void wait();

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPBenchmarkDriver, 0 );

    // EventHandler
    void handleEvent( Event& );

  private:
    HTTPBenchmarkDriver( auto_EventTarget http_request_target, uint8_t in_flight_http_request_count, const std::vector<URI*>& wlog_uris );

    auto_EventTarget http_request_target;
    uint8_t in_flight_http_request_count;

    std::vector<URI*> wlog_uris;
    uint8_t wlog_repetitions_count;

    auto_Stage my_stage;
    Mutex wait_signal;

    void sendHTTPRequest();

    // Statistics
    Mutex statistics_lock;
    class StatisticsTimer;
    uint32_t requests_sent_in_period, responses_received_in_period;
    std::vector<double> request_rates, response_rates;

    void calculateStatistics( const Time& elapsed_time );
  };


  class HTTPMessage : public RFC822Headers
  {
  public:
    auto_Buffer get_body() const { return body; }
    uint8_t get_http_version() const { return http_version; }

  protected:
    HTTPMessage( uint8_t reserve_iovecs_count );
    HTTPMessage( uint8_t reserve_iovecs_count, auto_Buffer body );
    HTTPMessage( const HTTPMessage& ) { DebugBreak(); } // Prevent copying
    virtual ~HTTPMessage() { }

    auto_Buffer body;

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

    virtual ssize_t deserialize( auto_Buffer );
    virtual auto_Buffer serialize();
  };


  class HTTPResponse : public Response, public HTTPMessage
  {
  public:
    HTTPResponse(); // Incoming
    HTTPResponse( uint16_t status_code ); // Outgoing
    HTTPResponse( uint16_t status_code, auto_Buffer body ); // Outgoing

    ssize_t deserialize( auto_Buffer );
    uint16_t get_status_code() const { return status_code; }
    auto_Buffer serialize();
    void set_body( auto_Buffer body ) { this->body = body; }
    void set_status_code( uint16_t status_code ) { this->status_code = status_code; }

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPResponse, 206 );

  protected:
    virtual ~HTTPResponse() { }

  private:
    HTTPResponse( const HTTPResponse& other ) // Prevent copying
      : HTTPMessage( other ) 
    { }

    uint8_t http_version;
    union { char status_code_str[4]; uint16_t status_code; };
  };

  typedef auto_Object<HTTPResponse> auto_HTTPResponse;


  class HTTPRequest : public Request, public HTTPMessage
  {
  public:
    HTTPRequest(); // Incoming
    HTTPRequest( const char* method, const char* relative_uri, const char* host, auto_Buffer body = NULL ); // Outgoing
    HTTPRequest( const char* method, const URI& absolute_uri, auto_Buffer body = NULL ); // Outgoing

    ssize_t deserialize( auto_Buffer );
    uint8_t get_http_version() const { return http_version; }
    const char* get_method() const { return method; }
    const char* get_uri() const { return uri; }    
    virtual void respond( uint16_t status_code );
    virtual void respond( uint16_t status_code, auto_Buffer body );
    virtual void respond( Response& response ) { Request::respond( response ); }
    auto_Buffer serialize();

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPRequest, 205 );

    // Request
    auto_Response createResponse() { return new HTTPResponse; }

  protected:
    virtual ~HTTPRequest();

  private:
    HTTPRequest( const HTTPRequest& other ) // Prevent copying
      : HTTPMessage( other )
    { }

    void init( const char* method, const char* relative_uri, const char* host, auto_Buffer body );

    char method[16];
    char* uri; size_t uri_len;
  };

  typedef auto_Object<HTTPRequest> auto_HTTPRequest;


  class HTTPClient : public EventHandler, public Client<HTTPRequest, HTTPResponse>
  {
  public:
    static auto_Object<HTTPClient> create( const URI& absolute_uri, 
                                           uint32_t flags = 0,
                                           auto_Log log = NULL, 
                                           const Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, 
                                           auto_SSLContext ssl_context = NULL );

    static auto_HTTPResponse GET( const URI& absolute_uri, auto_Log log = NULL );
    static auto_HTTPResponse PUT( const URI& absolute_uri, auto_Buffer body, auto_Log log = NULL );
    static auto_HTTPResponse PUT( const URI& absolute_uri, const Path& body_file_path, auto_Log log = NULL );

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPClient, 207 );

    // EventHandler
    virtual void handleEvent( Event& ev ) { Client<HTTPRequest, HTTPResponse>::handleEvent( ev ); }

  private:
    HTTPClient( const URI& absolute_uri, uint32_t flags, auto_Log log, const Time& operation_timeout, auto_SocketAddress peername, auto_SSLContext ssl_context )
      : Client<HTTPRequest, HTTPResponse>( absolute_uri, flags, log, operation_timeout, peername, ssl_context )
    { }

    virtual ~HTTPClient() { }

    static auto_HTTPResponse sendHTTPRequest( const char* method, const URI& uri, auto_Buffer body, auto_Log log );
  };

  typedef auto_Object<HTTPClient> auto_HTTPClient;
    

  class HTTPServer : public Object
  {
  public:
    static auto_Object<HTTPServer> create( const URI& absolute_uri,
                                           auto_EventTarget http_request_target, 
                                           auto_Log log = NULL, 
                                           auto_SSLContext ssl_context = NULL );

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPServer, 0 );

  private:
    HTTPServer( auto_EventTarget http_request_target, auto_TCPSocket listen_tcp_socket ) ;

    auto_EventTarget http_request_target;
    auto_TCPSocket listen_tcp_socket;

    class AIOAcceptControlBlock;
    class AIOReadControlBlock;
    class AIOWriteControlBlock;

    class HTTPResponseTarget;
  };

  typedef auto_Object<HTTPServer> auto_HTTPServer;


  class JSONMarshaller : public Marshaller
  {
  public:
    JSONMarshaller( bool write_empty_strings = true );
    virtual ~JSONMarshaller(); // If the stream is wrapped in map, sequence, etc. then the constructor will append the final } or [, so the underlying output stream should not be deleted before this object!

    auto_StringBuffer get_buffer() const { return buffer; }

    // Marshaller
    YIELD_MARSHALLER_PROTOTYPES;

  protected:
    JSONMarshaller( JSONMarshaller& parent_json_marshaller, const char* root_key );

    virtual void writeKey( const char* );
    virtual void writeMap( const Map* ); // Can be NULL for empty maps
    virtual void writeSequence( const Sequence* ); // Can be NULL for empty sequences
    virtual void writeStruct( const Struct* ); // Can be NULL for empty maps

  private:
    bool in_map;
    const char* root_key; // Mostly for debugging, also used to indicate if this is the root JSONMarshaller
    bool write_empty_strings;
    yajl_gen writer;

    void flushYAJLBuffer();
    auto_StringBuffer buffer;
  };


  class JSONUnmarshaller : public Unmarshaller
  {
  public:
    JSONUnmarshaller( auto_Buffer buffer );
    virtual ~JSONUnmarshaller();

    // Unmarshaller
    YIELD_UNMARSHALLER_PROTOTYPES;

  private:
    class JSONObject;
    class JSONValue;

    JSONUnmarshaller( const char* root_key, JSONValue& root_json_value );

    const char* root_key;
    JSONValue *root_json_value, *next_json_value;

    void readMap( Map& );
    void readSequence( Sequence& );
    void readStruct( Struct& );
    JSONValue* readJSONValue( const char* key );
  };


  class NamedPipe : public Object
  {
  public:
    static auto_Object<NamedPipe> open( const Path& path, uint32_t flags = O_RDWR, mode_t mode = File::DEFAULT_MODE );            

    virtual ssize_t read( void* buffer, size_t buffer_len );
    virtual ssize_t write( const void* buffer, size_t buffer_len );
    virtual ssize_t writev( const iovec* buffers, uint32_t buffers_count );
    
    // Object
    YIELD_OBJECT_PROTOTYPES( NamedPipe, 4 );  

  private:
#ifdef WIN32
    NamedPipe( auto_File underlying_file, bool connected );
#else
    NamedPipe( auto_File underlying_file );
#endif
    ~NamedPipe() { }

    auto_File underlying_file;

#ifdef _WIN32
    bool connected;
    bool connect();
#endif
  };

  typedef auto_Object<NamedPipe> auto_NamedPipe;


  template <class ONCRPCMessageType> // CRTP
  class ONCRPCMessage
  {
  public:
    virtual ssize_t deserialize( auto_Buffer );
    auto_Struct get_body() const { return body; }
    auto_Interface get_interface() const { return interface_; }
    uint32_t get_xid() const { return xid; }    
    virtual auto_Buffer serialize();
    void set_body( auto_Struct body ) { this->body = body; }

  protected:
    ONCRPCMessage( auto_Interface interface_ ); // Incoming
    ONCRPCMessage( auto_Interface interface_, uint32_t xid, auto_Struct body ); // Outgoing
    virtual ~ONCRPCMessage();

    enum 
    { 
      DESERIALIZING_RECORD_FRAGMENT_MARKER, 
      DESERIALIZING_RECORD_FRAGMENT, 
      DESERIALIZING_LONG_RECORD_FRAGMENT, 
      DESERIALIZE_DONE 
    } deserialize_state;

    ssize_t deserializeRecordFragmentMarker( auto_Buffer );
    ssize_t deserializeRecordFragment( auto_Buffer );
    ssize_t deserializeLongRecordFragment( auto_Buffer );

    // Object
    void marshal( Marshaller& marshaller );
    void unmarshal( Unmarshaller& unmarshaller );

  private:
    uint32_t record_fragment_length;
    auto_Buffer record_fragment_buffer;

    auto_Interface interface_;
    uint32_t xid;
    auto_Struct body;
  };


  class ONCRPCResponse : public Response, public ONCRPCMessage<ONCRPCResponse>
  {
  public:
    ONCRPCResponse( auto_Interface interface_ ); // Incoming, creates the body from the interface on demand
    ONCRPCResponse( auto_Interface interface_, uint32_t xid, auto_Struct body ); // Outgoing
    virtual ~ONCRPCResponse() { }

    // Object
    YIELD_OBJECT_PROTOTYPES( ONCRPCResponse, 208 );
    virtual void marshal( Marshaller& );
    virtual void unmarshal( Unmarshaller& );
  };

  typedef auto_Object<ONCRPCResponse> auto_ONCRPCResponse;


  class ONCRPCRequest : public Request, public ONCRPCMessage<ONCRPCRequest>
  {
  public:
    const static uint32_t AUTH_NONE = 0;

    ONCRPCRequest( auto_Interface interface_ ); // Incoming
    ONCRPCRequest( auto_Interface interface_, auto_Struct body ); // Outgoing
    ONCRPCRequest( auto_Interface interface_, uint32_t credential_auth_flavor, auto_Struct credential, auto_Struct body ); // Outgoing
    ONCRPCRequest( uint32_t prog, uint32_t proc, uint32_t vers, auto_Struct body ); // For testing
    ONCRPCRequest( uint32_t prog, uint32_t proc, uint32_t vers, uint32_t credential_auth_flavor, auto_Struct credential, auto_Struct body ); // For testing
    virtual ~ONCRPCRequest() { }

    uint32_t get_credential_auth_flavor() const { return credential_auth_flavor; }
    auto_Struct get_credential() const { return credential; }
    uint32_t get_prog() const { return prog; }
    uint32_t get_proc() const { return proc; }
    uint32_t get_vers() const { return vers; }

    // Object
    YIELD_OBJECT_PROTOTYPES( ONCRPCRequest, 213 );    
    virtual void marshal( Marshaller& );
    virtual void unmarshal( Unmarshaller& );

    // Request
    virtual auto_Response createResponse();
    virtual void respond( Response& );

  private:
    uint32_t prog, proc, vers, credential_auth_flavor;
    auto_Struct credential;
  };

  typedef auto_Object<ONCRPCRequest> auto_ONCRPCRequest;


  template <class InterfaceType>
  class ONCRPCClient : public InterfaceType, public Client<ONCRPCRequest, ONCRPCResponse>
  {
  public:
    template <class ONCRPCClientType>
    static auto_Object<ONCRPCClientType> create( const URI& absolute_uri, 
                                                 uint32_t flags = 0,
                                                 auto_Log log = NULL, 
                                                 const Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, 
                                                 auto_SSLContext ssl_context = NULL )
    {           
      auto_SocketAddress peername = SocketAddress::create( absolute_uri );
      if ( peername != NULL && peername->get_port() != 0 )
        return new ONCRPCClientType( absolute_uri, flags, log, operation_timeout, peername, ssl_context );        
      else
        return NULL;
    }

    // EventHandler
    virtual void handleEvent( Event& ev )
    {
      if ( InterfaceType::checkRequest( ev ) != NULL )
      {
        ONCRPCRequest* oncrpc_request = new ONCRPCRequest( this->incRef(), ev );
#ifdef _DEBUG
        if ( ( this->get_flags() & this->CLIENT_FLAG_TRACE_OPERATIONS ) == this->CLIENT_FLAG_TRACE_OPERATIONS && this->get_log() != NULL )
          this->get_log()->getStream( Log::LOG_INFO ) << "yield::ONCRPCClient: creating new ONCRPCRequest/" << reinterpret_cast<uint64_t>( oncrpc_request ) << " (xid=" << oncrpc_request->get_xid() << ") for interface request " << ev.get_type_name() << ".";
#endif

        Client<ONCRPCRequest, ONCRPCResponse>::handleEvent( *oncrpc_request );
      }
      else
      {
#ifdef _DEBUG
        if ( ( this->get_flags() & this->CLIENT_FLAG_TRACE_OPERATIONS ) == this->CLIENT_FLAG_TRACE_OPERATIONS && this->get_log() != NULL )
        {
          switch ( ev.get_type_id() )
          {
            case YIELD_OBJECT_TYPE_ID( ONCRPCRequest ): this->get_log()->getStream( Log::LOG_INFO ) << "yield::ONCRPCClient: send()'ing ONCRPCRequest/" << reinterpret_cast<uint64_t>( &ev ) << " (xid=" << static_cast<ONCRPCRequest&>( ev ).get_xid() << ")."; break;
            case YIELD_OBJECT_TYPE_ID( ONCRPCResponse ): this->get_log()->getStream( Log::LOG_INFO ) << "yield::ONCRPCClient: send()'ing ONCRPCRequest/" << reinterpret_cast<uint64_t>( &ev ) << " (xid=" << static_cast<ONCRPCResponse&>( ev ).get_xid() << ")."; break;
          }
        }
#endif

        Client<ONCRPCRequest, ONCRPCResponse>::handleEvent( ev );
      }
    }

  protected:
    ONCRPCClient( const URI& absolute_uri, uint32_t flags, auto_Log log, const Time& operation_timeout, auto_SocketAddress peername, auto_SSLContext ssl_context )
      : Client<ONCRPCRequest, ONCRPCResponse>( absolute_uri, flags, log, operation_timeout, peername, ssl_context )
    { }

    virtual ~ONCRPCClient() { }
  };

  
  class ONCRPCServer : public Object
  {
  public:
    static auto_Object<ONCRPCServer> create( const URI& absolute_uri,
                                             auto_Interface interface_,
                                             auto_Log log = NULL, 
                                             auto_SSLContext ssl_context = NULL );

    // Object
    YIELD_OBJECT_PROTOTYPES( ONCRPCServer, 0 );
   
  protected:
    ONCRPCServer( auto_Interface interface_, auto_Socket socket_ );

  private:
    auto_Interface interface_;
    auto_Socket socket_;

    class AIOAcceptControlBlock;
    class AIOReadControlBlock;
    class AIORecvFromControlBlock;
    class AIOWriteControlBlock;

    class ONCRPCResponseTarget;
  };

  typedef auto_Object<ONCRPCServer> auto_ONCRPCServer;


  class Pipe : public Object
  {
  public:
    static auto_Object<Pipe> create();

    void close();
#ifdef _WIN32
    void* get_read_end() const { return ends[0]; }
    void* get_write_end() const { return ends[1]; }
#else
    int get_read_end() const { return ends[0]; }
    int get_write_end() const { return ends[1]; }
#endif
    ssize_t read( void* buffer, size_t buffer_len );
    bool set_blocking_mode( bool blocking );
    ssize_t write( const void* buffer, size_t buffer_len );

    // Object
    YIELD_OBJECT_PROTOTYPES( Pipe, 6 );

  private:
#ifdef _WIN32
    Pipe( void* ends[2] );
#else
    Pipe( int ends[2] );
#endif
    ~Pipe();

#ifdef _WIN32
    void* ends[2];
#else
    int ends[2];
#endif
  };

  typedef auto_Object<Pipe> auto_Pipe;


  class Process : public Object
  {
  public:
    static auto_Object<Process> create( const Path& executable_file_path ); // No arguments
    static auto_Object<Process> create( int argc, char** argv );    
    static auto_Object<Process> create( const Path& executable_file_path, const char** null_terminated_argv ); // execv style

    auto_Object<Pipe> get_stdin() const { return child_stdin; }
    auto_Object<Pipe> get_stdout() const { return child_stdout; }
    auto_Object<Pipe> get_stderr() const { return child_stderr; }

    bool kill(); // SIGKILL
    bool poll( int* out_return_code = 0 ); // Calls waitpid() but WNOHANG, out_return_code can be NULL    
    bool terminate(); // SIGTERM
    int wait(); // Calls waitpid() and suspends the calling process until the child exits, use carefully

    // Object
    YIELD_OBJECT_PROTOTYPES( Process, 7 );

  private:
#ifdef _WIN32
    Process( void* hChildProcess, void* hChildThread,       
#else
    Process( pid_t child_pid, 
#endif
      auto_Object<Pipe> child_stdin, auto_Object<Pipe> child_stdout, auto_Object<Pipe> child_stderr );

    ~Process();

#ifdef _WIN32
    void *hChildProcess, *hChildThread;
#else
    int child_pid;
#endif
    auto_Object<Pipe> child_stdin, child_stdout, child_stderr;  
  };

  typedef auto_Object<Process> auto_Process;


  class URI : public Object
  {
  public:
    // Factory methods return NULL instead of throwing exceptions
    static auto_Object<URI> parse( const char* uri ) { return parse( uri, strnlen( uri, UINT16_MAX ) ); }
    static auto_Object<URI> parse( const std::string& uri ) { return parse( uri.c_str(), uri.size() ); }
    static auto_Object<URI> parse( const char* uri, size_t uri_len );

    // Constructors throw exceptions
    URI( const char* uri ) { init( uri, strnlen( uri, UINT16_MAX ) ); }
    URI( const std::string& uri ) { init( uri.c_str(), uri.size() ); }
    URI( const char* uri, size_t uri_len ) { init( uri, uri_len ); }

    URI( const char* scheme, const char* host, uint16_t port ) // For testing
      : scheme( scheme ), host( host ), port( port ), resource( "/" )
    { }    

    URI( const char* scheme, const char* host, uint16_t port, const char* resource ) // For testing
      : scheme( scheme ), host( host ), port( port ), resource( resource )
    { }

    URI( const URI& other );
    virtual ~URI() { }

    const std::string& get_scheme() const { return scheme; }
    const std::string& get_host() const { return host; }
    const std::string& get_password() const { return password; }
    unsigned short get_port() const { return port; }
    const std::string& get_resource() const { return resource; }
    const std::string& get_user() const { return user; }
    operator std::string() const;
    void set_host( const std::string& host ) { this->host = host; }
    void set_port( unsigned short port ) { this->port = port; }
    void set_password( const std::string& password ) { this->password = password; }
    void set_resource( const std::string& resource ) { this->resource = resource; }
    void set_scheme( const std::string& scheme ) { this->scheme = scheme; }
    void set_user( const std::string& user ) { this->user = user; }

    // Object
    YIELD_OBJECT_PROTOTYPES( URI, 221 );

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
  };

  static inline std::ostream& operator<<( std::ostream& os, const URI& uri )
  {
    os << uri.get_scheme();
    os << "://";
    os << uri.get_host();
    if ( uri.get_port() != 0 )
      os << ":" << uri.get_port();
    os << uri.get_resource();
    return os;
  }

  typedef auto_Object<URI> auto_URI;
};

#endif
