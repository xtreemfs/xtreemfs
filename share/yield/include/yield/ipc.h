// Copyright 2003-2008 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _YIELD_IPC_H_
#define _YIELD_IPC_H_

#include "yield/concurrency.h"

#ifdef YIELD_HAVE_LIBUUID
#include <uuid/uuid.h>
#endif
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


namespace YIELD
{
  namespace ipc
  {
    class HTTPRequest;
    class ONCRPCRequest;
    class Socket;
    class URI;


#ifdef YIELD_HAVE_ZLIB
    static inline yidl::runtime::auto_Buffer 
      deflate( 
        yidl::runtime::auto_Buffer buffer, 
        int level = Z_BEST_COMPRESSION 
      )
    {
      z_stream zstream;
      zstream.zalloc = Z_NULL;
      zstream.zfree = Z_NULL;
      zstream.opaque = Z_NULL;

      if ( deflateInit( &zstream, level ) == Z_OK )
      {
        zstream.next_in = static_cast<Bytef*>( *buffer );
        zstream.avail_in = static_cast<uInt>( buffer->size() );

        Bytef zstream_out[4096];
        zstream.next_out = zstream_out;
        zstream.avail_out = 4096;

        yidl::runtime::auto_Buffer out_buffer( new yidl::runtime::StringBuffer );

        while ( ::deflate( &zstream, Z_NO_FLUSH ) == Z_OK )
        {
          if ( zstream.avail_out == 0 ) 
          {
            // Filled zstream_out, copy it into out_buffer and keep deflating
            out_buffer->put( zstream_out, sizeof( zstream_out ) );
            zstream.next_out = zstream_out;
            zstream.avail_out = sizeof( zstream_out );
          }
          else // deflate returned Z_OK without filling zstream_out -> done
          {
            int deflate_ret;
            // Z_OK = need more buffer space to finish compression, 
            // Z_STREAM_END = really done
            while ( ( deflate_ret = ::deflate( &zstream, Z_FINISH ) ) == Z_OK ) 
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
                  out_buffer->put( zstream_out, 
                                   sizeof( zstream_out ) - zstream.avail_out );

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
#endif


    class SocketAddress : public yidl::runtime::Object
    {
    public:
      SocketAddress( struct addrinfo& addrinfo_list ); // Takes ownership
      SocketAddress( const struct sockaddr_storage& _sockaddr_storage ); // Copies

      // create( ... ) factory methods return NULL instead of throwing exceptions
      // hostname can be NULL for INADDR_ANY
      static yidl::runtime::auto_Object<SocketAddress> create( const char* hostname )
      { 
        return create( hostname, 0 ); 
      }

      static yidl::runtime::auto_Object<SocketAddress> create( const char* hostname, uint16_t port ); 
      static yidl::runtime::auto_Object<SocketAddress> create( const URI& );

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

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( SocketAddress, 0 );

    private:
      SocketAddress( const SocketAddress& ) { DebugBreak(); } // Prevent copying
      ~SocketAddress();

      struct addrinfo* addrinfo_list; // Linked sockaddr's obtained from getaddrinfo(3)
      struct sockaddr_storage* _sockaddr_storage; // A single sockaddr passed in the constructor and copied

      static struct addrinfo* getaddrinfo( const char* hostname, uint16_t port );
    };

    typedef yidl::runtime::auto_Object<SocketAddress> auto_SocketAddress;


    class Socket : public yidl::runtime::Object
    {
    public:
      class AIOControlBlock : public ::YIELD::platform::AIOControlBlock
      {
      public:
        virtual ~AIOControlBlock() { }

        enum ExecuteStatus 
        { 
          EXECUTE_STATUS_DONE = 0, 
          EXECUTE_STATUS_WANT_READ, 
          EXECUTE_STATUS_WANT_WRITE 
        };

        virtual ExecuteStatus execute() = 0;

        yidl::runtime::auto_Object<Socket> get_socket() const { return socket_; }

        void set_socket( Socket& socket_ ) 
        { 
          if ( this->socket_ == NULL ) 
            this->socket_ = socket_.incRef(); 
        }

      protected:
        AIOControlBlock() { }

      private:
        yidl::runtime::auto_Object<Socket> socket_;
      };


      class AIOConnectControlBlock : public AIOControlBlock
      {
      public:
        AIOConnectControlBlock( auto_SocketAddress peername )
          : peername( peername )
        { }

        virtual ~AIOConnectControlBlock()
        { }

        auto_SocketAddress get_peername() const { return peername; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( AIOConnectControlBlock, 223 );

        // Socket::AIOControlBlock
        ExecuteStatus execute()
        {
          if ( get_socket()->connect( get_peername() ) )
            onCompletion( 0 );
          else if ( get_socket()->want_connect() )
            return EXECUTE_STATUS_WANT_WRITE;
          else
            onError( YIELD::platform::Exception::get_errno() );

          return EXECUTE_STATUS_DONE;
        }

      private:
        auto_SocketAddress peername;
      };

      typedef yidl::runtime::auto_Object<AIOConnectControlBlock> auto_AIOConnectControlBlock;


      class AIOReadControlBlock : public AIOControlBlock
      {
      public:
        AIOReadControlBlock( yidl::runtime::auto_Buffer buffer )
          : buffer( buffer )
        { }

        virtual ~AIOReadControlBlock()
        { }

        yidl::runtime::auto_Buffer get_buffer() const { return buffer; }
        void unlink_buffer() { buffer = NULL; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( AIOReadControlBlock, 227 );

        // Socket::AIOControlBlock
        ExecuteStatus execute();

      private:
        yidl::runtime::auto_Buffer buffer;
      };

      typedef yidl::runtime::auto_Object<AIOReadControlBlock> auto_AIOReadControlBlock;


      class AIOWriteControlBlock : public AIOControlBlock
      {
      public:
        AIOWriteControlBlock( yidl::runtime::auto_Buffer buffer )
          : buffer( buffer )
        { }

        virtual ~AIOWriteControlBlock()
        { }

        yidl::runtime::auto_Buffer get_buffer() const { return buffer; }
        void unlink_buffer() { buffer = NULL; }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( AIOWriteControlBlock, 228 );

        // Socket::AIOControlBlock
        ExecuteStatus execute()
        {
          ssize_t write_ret = get_socket()->write( get_buffer() );

          if ( write_ret >= 0 )
            onCompletion( static_cast<size_t>( write_ret ) );
          else if ( get_socket()->want_write() )
            return EXECUTE_STATUS_WANT_WRITE;
          else if ( get_socket()->want_read() )
            return EXECUTE_STATUS_WANT_READ;
          else
            onError( YIELD::platform::Exception::get_errno() );

          return EXECUTE_STATUS_DONE;        
        }

      private:
        yidl::runtime::auto_Buffer buffer;
      };

      typedef yidl::runtime::auto_Object<AIOWriteControlBlock> auto_AIOWriteControlBlock;

#if defined(_WIN64)
      Socket( int domain, int type, int protocol, uint64_t socket_ );
#elif defined(_WIN32)
      Socket( int domain, int type, int protocol, uint32_t socket_ );
#else
      Socket( int domain, int type, int protocol, int socket_ );
#endif

      virtual void aio_connect( Socket::auto_AIOConnectControlBlock );
      virtual void aio_read( yidl::runtime::auto_Object<AIOReadControlBlock> );
      virtual void aio_write( yidl::runtime::auto_Object<AIOWriteControlBlock> );
      virtual bool bind( auto_SocketAddress to_sockaddr );
      virtual bool close();
      virtual bool connect( auto_SocketAddress to_sockaddr );
      static void destroy();
      virtual bool get_blocking_mode() const;
      int get_domain() const { return domain; }
      static std::string getfqdn();
      static std::string gethostname();
      auto_SocketAddress getpeername();
      int get_protocol() const { return protocol; }
      auto_SocketAddress getsockname();
      int get_type() const { return type; }
      static void init();
      bool operator==( const Socket& other ) const;
#if defined(_WIN64)
      inline operator uint64_t() const { return socket_; }
#elif defined(_WIN32)
      inline operator uint32_t() const { return socket_; }
#else
      inline operator int() const { return socket_; }
#endif
      virtual ssize_t read( yidl::runtime::auto_Buffer buffer );
      virtual ssize_t read( void* buffer, size_t buffer_len );
      virtual bool set_blocking_mode( bool blocking );
      virtual bool shutdown();
      virtual bool want_connect() const;
      virtual bool want_read() const;
      virtual bool want_write() const;
      virtual ssize_t write( yidl::runtime::auto_Buffer buffer );
      virtual ssize_t write( const void* buffer, size_t buffer_len );    
      virtual ssize_t writev( const struct iovec* buffers, uint32_t buffers_count );

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( Socket, 211 );

    protected:
      friend class TracingSocket;

      virtual ~Socket();

#ifdef _WIN32
      void aio_read_iocp( yidl::runtime::auto_Object<AIOReadControlBlock> );
      void aio_write_iocp( yidl::runtime::auto_Object<AIOWriteControlBlock> );
#endif
      void aio_connect_nbio( Socket::auto_AIOConnectControlBlock );
      void aio_read_nbio( yidl::runtime::auto_Object<AIOReadControlBlock> );
      void aio_write_nbio( yidl::runtime::auto_Object<AIOWriteControlBlock> );

#if defined(_WIN64)
      static uint64_t create( int& domain, int type, int protocol );
#elif defined(_WIN32)
      static uint32_t create( int& domain, int type, int protocol );
#else
      static int create( int& domain, int type, int protocol );
#endif
      bool recreate( int domain );

    private:
      Socket( const Socket& ) { DebugBreak(); } // Prevent copying

      int domain, type, protocol;

#if defined(_WIN64)
      uint64_t socket_;
#elif defined(_WIN32)
      uint32_t socket_;
#else
      int socket_;
#endif

      bool blocking_mode;

    private:
      class AIOQueue
      {
      public:
        AIOQueue();
        ~AIOQueue();

#ifdef _WIN32
        void associate( Socket& );
#endif
        void submit( yidl::runtime::auto_Object<AIOControlBlock> );

      private:
#ifdef _WIN32
       void* hIoCompletionPort;

       class IOCPWorkerThread;
       std::vector<IOCPWorkerThread*> iocp_worker_threads;
#endif

       class NBIOWorkerThread;
       std::vector<NBIOWorkerThread*> nbio_worker_threads;
      };

      static AIOQueue* aio_queue;

    protected:
      AIOQueue& get_aio_queue();
    };

    typedef yidl::runtime::auto_Object<Socket> auto_Socket;

  
    class SocketFactory : public yidl::runtime::Object
    {
    public:
      virtual auto_Socket createSocket() = 0;
    };

    typedef yidl::runtime::auto_Object<SocketFactory> auto_SocketFactory;


    class SSLContext : public yidl::runtime::Object
    {
    public:
#ifdef YIELD_HAVE_OPENSSL
      // create( ... ) factory methods throw exceptions

      static yidl::runtime::auto_Object<SSLContext> 
        create( SSL_METHOD* method = SSLv23_client_method() ); // No certificate

      static yidl::runtime::auto_Object<SSLContext> 
        create
        ( 
          SSL_METHOD* method, 
          const YIELD::platform::Path& pem_certificate_file_path, 
          const YIELD::platform::Path& pem_private_key_file_path, 
          const std::string& pem_private_key_passphrase 
        );

      static yidl::runtime::auto_Object<SSLContext> 
        create
        ( 
          SSL_METHOD* method, 
          const std::string& pem_certificate_str, 
          const std::string& pem_private_key_str, 
          const std::string& pem_private_key_passphrase 
        );

      static yidl::runtime::auto_Object<SSLContext> 
        create
        ( 
          SSL_METHOD* method, 
          const YIELD::platform::Path& pkcs12_file_path, 
          const std::string& pkcs12_passphrase 
        );
#else
      static yidl::runtime::auto_Object<SSLContext> create();
#endif

#ifdef YIELD_HAVE_OPENSSL
      SSL_CTX* get_ssl_ctx() const { return ctx; }
#endif

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( SSLContext, 215 );

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

    typedef yidl::runtime::auto_Object<SSLContext> auto_SSLContext;


    class TCPSocket : public Socket
    {
    public:
     class AIOAcceptControlBlock : public AIOControlBlock
     {
      public:
        AIOAcceptControlBlock()
        { }

        yidl::runtime::auto_Object<TCPSocket> get_accepted_tcp_socket() const 
        { 
          return accepted_tcp_socket; 
        }

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( AIOAcceptControlBlock, 222 );

        // Socket::AIOControlBlock
        ExecuteStatus execute()
        {
          accepted_tcp_socket = 
            static_cast<TCPSocket*>( get_socket().get() )->accept();

          if ( accepted_tcp_socket != NULL )
            onCompletion( 0 );
#ifdef _WIN32
          else if ( YIELD::platform::Exception::get_errno() == 10035 )
#else
          else if ( YIELD::platform::Exception::get_errno() == EWOULDBLOCK )
#endif
            return EXECUTE_STATUS_WANT_READ;
          else
            onError( YIELD::platform::Exception::get_errno() );

          return EXECUTE_STATUS_DONE;
        }

      private:
        yidl::runtime::auto_Object<TCPSocket> accepted_tcp_socket;      

#ifdef _WIN32
        friend class TCPSocket;
        char peername[88];
#endif
      };


      virtual void aio_accept( yidl::runtime::auto_Object<AIOAcceptControlBlock> );    
      virtual void aio_connect( Socket::auto_AIOConnectControlBlock );
      // create( ... ) methods return NULL instead of throwing exceptions, 
      // since they're on the critical path of clients and servers
      static yidl::runtime::auto_Object<TCPSocket> create(); // domain = AF_INET6; 
      static yidl::runtime::auto_Object<TCPSocket> create( int domain );
      virtual yidl::runtime::auto_Object<TCPSocket> accept();
      virtual bool listen();
      virtual bool shutdown();
      virtual ssize_t writev( const struct iovec* buffers, uint32_t buffers_count );

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( TCPSocket, 212 );

    protected:
#if defined(_WIN64)
      TCPSocket( int domain, uint64_t socket_ );
#elif defined(_WIN32)
      TCPSocket( int domain, uint32_t socket_ );      
#else
      TCPSocket( int domain, int socket_ );
#endif

      virtual ~TCPSocket() { }

#if defined(_WIN64)
      uint64_t _accept();
#elif defined(_WIN32)
      uint32_t _accept();
#else
      int _accept();
#endif

      // Socket
#ifdef _WIN32
      void aio_accept_iocp( yidl::runtime::auto_Object<AIOAcceptControlBlock> );
      void aio_connect_iocp( Socket::auto_AIOConnectControlBlock );
#endif
      void aio_accept_nbio( yidl::runtime::auto_Object<AIOAcceptControlBlock> );

    private:
#ifdef _WIN32
      static void *lpfnAcceptEx, *lpfnConnectEx;
#endif

      size_t partial_write_len;
    };

    typedef yidl::runtime::auto_Object<TCPSocket> auto_TCPSocket;


    template <class RequestType, class ResponseType>
    class Client
    {
    public:    
      const static uint32_t CLIENT_FLAG_TRACE_IO = 1;
      const static uint32_t CLIENT_FLAG_TRACE_OPERATIONS = 2;

      const static uint64_t OPERATION_TIMEOUT_DEFAULT = 5 * NS_IN_S;
      const static uint8_t RECONNECT_TRIES_MAX_DEFAULT = 3;      
      
      // YIELD::concurrency::EventHandler
      virtual void handleEvent( YIELD::concurrency::Event& );

    protected:
      Client
      ( 
        uint32_t flags, 
        YIELD::platform::auto_Log log, 
        const YIELD::platform::Time& operation_timeout, 
        auto_SocketAddress peername, 
        uint8_t reconnect_tries_max, 
        auto_SocketFactory socket_factory 
      );
      virtual ~Client();

      uint32_t get_flags() const { return flags; }
      YIELD::platform::auto_Log get_log() const { return log; }

    private:
      uint32_t flags;
      YIELD::platform::auto_Log log;
      YIELD::platform::Time operation_timeout;
      auto_SocketAddress peername;
      uint8_t reconnect_tries_max;
      auto_SocketFactory socket_factory;
      
      YIELD::platform::SynchronizedSTLQueue<Socket*> idle_sockets;

      class AIOConnectControlBlock;
      class AIOReadControlBlock;
      class AIOWriteControlBlock;
      
      class OperationTimer;
    };


    class GatherBuffer : public yidl::runtime::Buffer
    {
    public:
      GatherBuffer( const struct iovec* iovecs, uint32_t iovecs_len );

      const struct iovec* get_iovecs() const { return iovecs; }
      uint32_t get_iovecs_len() const { return iovecs_len; }

      // Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( GatherBuffer, 3 );

      // Buffer
      size_t capacity() const { return size(); }
      size_t get( void* into_buffer, size_t into_buffer_len );
      size_t put( const void*, size_t ) { return 0; }
      operator void*() const { DebugBreak(); return NULL; }
      size_t size() const;

    private:
      const struct iovec* iovecs;
      uint32_t iovecs_len;
    };

    typedef yidl::runtime::auto_Object<GatherBuffer> auto_GatherBuffer;


    class RFC822Headers
    {
    public:
      RFC822Headers( uint8_t reserve_iovecs_count = 0 );
      virtual ~RFC822Headers();

      ssize_t deserialize( yidl::runtime::auto_Buffer );
      char* get_header( const char* header_name, const char* default_value="" );
      char* operator[]( const char* header_name ) { return get_header( header_name ); }
      // set_header( ... ): char* copies into a buffer, const char* does not
      void set_header( const char* header_name, const char* header_value );
      void set_header( const char* header_name, char* header_value );
      void set_header( char* header_name, char* header_value );
      void set_header( const std::string& header_name, const std::string& header_value );
      yidl::runtime::auto_Buffer serialize();

    protected:
      void set_iovec( uint8_t iovec_i, const char* data, size_t len );
      void set_next_iovec( char* data, size_t len ); // Copies data
      void set_next_iovec( const char* data, size_t len ); // Does not copy data
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

      char stack_buffer[YIELD_RFC822_HEADERS_STACK_BUFFER_LENGTH], *heap_buffer, *buffer_p;
      size_t heap_buffer_len;

      iovec stack_iovecs[YIELD_RFC822_HEADERS_STACK_IOVECS_LENGTH], *heap_iovecs; 
      uint8_t iovecs_filled;

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


    class HTTPBenchmarkDriver : public YIELD::concurrency::EventHandler
    {
    public:
      static yidl::runtime::auto_Object<HTTPBenchmarkDriver> 
        create
        ( 
          YIELD::concurrency::auto_EventTarget http_request_target, 
          uint8_t in_flight_request_count, 
          const YIELD::platform::Path& wlog_file_path, 
          uint32_t wlog_uris_length_max = static_cast<uint32_t>( -1 ), 
          uint8_t wlog_repetitions_count = 1 
        );

      virtual ~HTTPBenchmarkDriver();

      void get_request_rates( std::vector<double>& out_request_rates );
      void get_response_rates( std::vector<double>& out_response_rates );
      void wait();

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( HTTPBenchmarkDriver, 0 );

      // YIELD::concurrency::EventHandler
      void handleEvent( YIELD::concurrency::Event& );

    private:
      HTTPBenchmarkDriver
      ( 
        YIELD::concurrency::auto_EventTarget http_request_target, 
        uint8_t in_flight_http_request_count, 
        const std::vector<URI*>& wlog_uris 
      );

      YIELD::concurrency::auto_EventTarget http_request_target;
      uint8_t in_flight_http_request_count;

      std::vector<URI*> wlog_uris;
      uint8_t wlog_repetitions_count;

      YIELD::concurrency::auto_Stage my_stage;
      YIELD::platform::Mutex wait_signal;

      void sendHTTPRequest();

      // Statistics
      YIELD::platform::Mutex statistics_lock;
      class StatisticsTimer;
      uint32_t requests_sent_in_period, responses_received_in_period;
      std::vector<double> request_rates, response_rates;

      void calculateStatistics( const YIELD::platform::Time& elapsed_time );
    };


    class HTTPMessage : public RFC822Headers
    {
    public:
      yidl::runtime::auto_Buffer get_body() const { return body; }
      uint8_t get_http_version() const { return http_version; }

    protected:
      HTTPMessage( uint8_t reserve_iovecs_count );
      HTTPMessage( uint8_t reserve_iovecs_count, yidl::runtime::auto_Buffer body );
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
      virtual yidl::runtime::auto_Buffer serialize();
    };


    class HTTPResponse : public YIELD::concurrency::Response, public HTTPMessage
    {
    public:
      HTTPResponse(); // Incoming
      HTTPResponse( uint16_t status_code ); // Outgoing
      HTTPResponse( uint16_t status_code, yidl::runtime::auto_Buffer body ); // Outgoing

      ssize_t deserialize( yidl::runtime::auto_Buffer );
      uint16_t get_status_code() const { return status_code; }
      yidl::runtime::auto_Buffer serialize();
      void set_body( yidl::runtime::auto_Buffer body ) { this->body = body; }
      void set_status_code( uint16_t status_code ) { this->status_code = status_code; }

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( HTTPResponse, 206 );

    protected:
      virtual ~HTTPResponse() { }

    private:
      HTTPResponse( const HTTPResponse& other ) // Prevent copying
        : HTTPMessage( other ) 
      { }

      uint8_t http_version;
      union { char status_code_str[4]; uint16_t status_code; };
    };

    typedef yidl::runtime::auto_Object<HTTPResponse> auto_HTTPResponse;


    class HTTPRequest : public YIELD::concurrency::Request, public HTTPMessage
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

      uint8_t get_reconnect_tries() const { return reconnect_tries; }
      ssize_t deserialize( yidl::runtime::auto_Buffer );
      uint8_t get_http_version() const { return http_version; }
      const char* get_method() const { return method; }
      const char* get_uri() const { return uri; }    
      virtual void respond( uint16_t status_code );
      virtual void respond( uint16_t status_code, yidl::runtime::auto_Buffer body );
      virtual void respond( YIELD::concurrency::Response& response );
      yidl::runtime::auto_Buffer serialize();
      void set_reconnect_tries( uint8_t reconnect_tries );

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( HTTPRequest, 205 );

      // YIELD::concurrency::Request
      YIELD::concurrency::auto_Response createResponse() { return new HTTPResponse; }

    protected:
      virtual ~HTTPRequest();

    private:
      HTTPRequest( const HTTPRequest& other ) // Prevent copying
        : HTTPMessage( other )
      {
        reconnect_tries = 0;
      }

      void init
      ( 
        const char* method, 
        const char* relative_uri, 
        const char* host, 
        yidl::runtime::auto_Buffer body 
      );

      char method[16];
      uint8_t reconnect_tries;
      char* uri; size_t uri_len;
    };

    typedef yidl::runtime::auto_Object<HTTPRequest> auto_HTTPRequest;


    class HTTPClient : public YIELD::concurrency::EventHandler, public Client<HTTPRequest, HTTPResponse>
    {
    public:
      // create( ... ) throws an exception instead of returning NULL
      static yidl::runtime::auto_Object<HTTPClient> 
        create
        ( 
          const URI& absolute_uri, 
          uint32_t flags = 0,
          YIELD::platform::auto_Log log = NULL,                                                             
          const YIELD::platform::Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, 
          uint8_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT,
          auto_SSLContext ssl_context = NULL 
        );

      static auto_HTTPResponse 
        GET( const URI& absolute_uri, YIELD::platform::auto_Log log = NULL );

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

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( HTTPClient, 207 );

      // YIELD::concurrency::EventHandler
      virtual void handleEvent( YIELD::concurrency::Event& ev ) 
      { 
        Client<HTTPRequest, HTTPResponse>::handleEvent( ev ); 
      }

    private:
      HTTPClient
      (
        uint32_t flags, 
        YIELD::platform::auto_Log log, 
        const YIELD::platform::Time& operation_timeout, 
        auto_SocketAddress peername, 
        uint8_t reconnect_tries_max, 
        auto_SocketFactory socket_factory
      )
        : Client<HTTPRequest, HTTPResponse>
          ( 
            flags, log, operation_timeout, peername, 
            reconnect_tries_max, socket_factory 
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

    typedef yidl::runtime::auto_Object<HTTPClient> auto_HTTPClient;
      

    class HTTPServer : public yidl::runtime::Object
    {
    public:
      static yidl::runtime::auto_Object<HTTPServer> 
        create
        ( 
          const URI& absolute_uri,
          YIELD::concurrency::auto_EventTarget http_request_target, 
          YIELD::platform::auto_Log log = NULL, 
          auto_SSLContext ssl_context = NULL 
        );

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( HTTPServer, 0 );

    private:
      HTTPServer
      ( 
        YIELD::concurrency::auto_EventTarget http_request_target, 
        auto_TCPSocket listen_tcp_socket, 
        YIELD::platform::auto_Log log 
      );

      YIELD::concurrency::auto_EventTarget http_request_target;
      auto_TCPSocket listen_tcp_socket;
      YIELD::platform::auto_Log log;

      class AIOAcceptControlBlock;
      class AIOReadControlBlock;
      class AIOWriteControlBlock;

      class HTTPResponseTarget;
    };

    typedef yidl::runtime::auto_Object<HTTPServer> auto_HTTPServer;


    class JSONMarshaller : public yidl::runtime::Marshaller
    {
    public:
      JSONMarshaller( bool write_empty_strings = true );
      virtual ~JSONMarshaller();

      yidl::runtime::auto_StringBuffer get_buffer() const { return buffer; }

      // Marshaller
      YIDL_MARSHALLER_PROTOTYPES;

    protected:
      JSONMarshaller( JSONMarshaller& parent_json_marshaller, const char* root_key );

      virtual void writeKey( const char* );
      virtual void writeMap( const yidl::runtime::Map* ); // NULL = empty
      virtual void writeSequence( const yidl::runtime::Sequence* );
      virtual void writeStruct( const yidl::runtime::Struct* );

    private:
      bool in_map;
      const char* root_key;
      bool write_empty_strings;
      yajl_gen writer;

      void flushYAJLBuffer();
      yidl::runtime::auto_StringBuffer buffer;
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

      void readMap( yidl::runtime::Map& );
      void readSequence( yidl::runtime::Sequence& );
      void readStruct( yidl::runtime::Struct& );
      JSONValue* readJSONValue( const char* key );
    };


    class NamedPipe : public yidl::runtime::Object
    {
    public:
      // open returns NULL instead of throwing exceptions
      static yidl::runtime::auto_Object<NamedPipe> 
        open
        ( 
          const YIELD::platform::Path& path, 
          uint32_t flags = O_RDWR, 
          mode_t mode = YIELD::platform::File::DEFAULT_MODE 
        ); 
            
      virtual ssize_t read( void* buffer, size_t buffer_len );
      virtual ssize_t write( const void* buffer, size_t buffer_len );
      
      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( NamedPipe, 4 );  

    private:
#ifdef WIN32
      NamedPipe( YIELD::platform::auto_File underlying_file, bool connected );
#else
      NamedPipe( YIELD::platform::auto_File underlying_file );
#endif
      ~NamedPipe() { }

      YIELD::platform::auto_File underlying_file;

#ifdef _WIN32
      bool connected;
      bool connect();
#endif
    };

    typedef yidl::runtime::auto_Object<NamedPipe> auto_NamedPipe;


    template <class ONCRPCMessageType> // CRTP
    class ONCRPCMessage
    {
    public:
      virtual ssize_t deserialize( yidl::runtime::auto_Buffer );
      yidl::runtime::auto_Struct get_body() const { return body; }
      YIELD::concurrency::auto_Interface get_interface() const { return interface_; }
      uint32_t get_xid() const { return xid; }    
      virtual yidl::runtime::auto_Buffer serialize();
      void set_body( yidl::runtime::auto_Struct body ) { this->body = body; }

    protected:
      ONCRPCMessage( YIELD::concurrency::auto_Interface interface_ ); // Incoming

      ONCRPCMessage // Outgoing 
      ( 
        YIELD::concurrency::auto_Interface interface_, 
        uint32_t xid, 
        yidl::runtime::auto_Struct body 
      );

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

      // yidl::runtime::Object
      void marshal( yidl::runtime::Marshaller& marshaller );
      void unmarshal( yidl::runtime::Unmarshaller& unmarshaller );

    private:
      uint32_t record_fragment_length;
      yidl::runtime::auto_Buffer record_fragment_buffer;

      YIELD::concurrency::auto_Interface interface_;
      uint32_t xid;
      yidl::runtime::auto_Struct body;
    };


    class ONCRPCResponse : public YIELD::concurrency::Response, public ONCRPCMessage<ONCRPCResponse>
    {
    public:
      ONCRPCResponse( YIELD::concurrency::auto_Interface interface_ ); // Incoming, creates the body from the interface on demand

      ONCRPCResponse // Outgoing
      (
        YIELD::concurrency::auto_Interface interface_, 
        uint32_t xid, 
        yidl::runtime::auto_Struct body 
      );

      virtual ~ONCRPCResponse() { }

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( ONCRPCResponse, 208 );
      virtual void marshal( yidl::runtime::Marshaller& );
      virtual void unmarshal( yidl::runtime::Unmarshaller& );
    };

    typedef yidl::runtime::auto_Object<ONCRPCResponse> auto_ONCRPCResponse;


    class ONCRPCRequest : public YIELD::concurrency::Request, public ONCRPCMessage<ONCRPCRequest>
    {
    public:
      const static uint32_t AUTH_NONE = 0;

      ONCRPCRequest( YIELD::concurrency::auto_Interface interface_ ); // Incoming

      ONCRPCRequest // Outgoing
      ( 
        YIELD::concurrency::auto_Interface interface_, 
        yidl::runtime::auto_Struct body 
      );

      ONCRPCRequest // Outgoing
      (
        YIELD::concurrency::auto_Interface interface_, 
        uint32_t credential_auth_flavor, 
        yidl::runtime::auto_Struct credential, 
        yidl::runtime::auto_Struct body 
      ); 

      ONCRPCRequest // For testing
      (
        uint32_t prog, 
        uint32_t proc, 
        uint32_t vers, 
        yidl::runtime::auto_Struct body 
      ); 

      ONCRPCRequest // For testing
      ( 
        uint32_t prog, 
        uint32_t proc, 
        uint32_t vers, 
        uint32_t credential_auth_flavor, 
        yidl::runtime::auto_Struct credential, 
        yidl::runtime::auto_Struct body 
      ); 

      virtual ~ONCRPCRequest() { }

      uint32_t get_credential_auth_flavor() const { return credential_auth_flavor; }
      yidl::runtime::auto_Struct get_credential() const { return credential; }
      uint32_t get_prog() const { return prog; }
      uint32_t get_proc() const { return proc; }
      uint8_t get_reconnect_tries() const { return reconnect_tries; }
      uint32_t get_vers() const { return vers; }
      void set_reconnect_tries( uint8_t reconnect_tries );

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( ONCRPCRequest, 213 );    
      virtual void marshal( yidl::runtime::Marshaller& );
      virtual void unmarshal( yidl::runtime::Unmarshaller& );

      // YIELD::concurrency::Request
      virtual YIELD::concurrency::auto_Response createResponse();
      virtual void respond( YIELD::concurrency::Response& );

    private:
      uint32_t prog, proc, vers, credential_auth_flavor;
      yidl::runtime::auto_Struct credential;
      uint8_t reconnect_tries;
    };

    typedef yidl::runtime::auto_Object<ONCRPCRequest> auto_ONCRPCRequest;


    template <class InterfaceType>
    class ONCRPCClient : public InterfaceType, public Client<ONCRPCRequest, ONCRPCResponse>
    {
    public:
      ONCRPCClient
      ( 
        uint32_t flags, 
        YIELD::platform::auto_Log log, 
        const YIELD::platform::Time& operation_timeout, 
        auto_SocketAddress peername, 
        uint8_t reconnect_tries_max, 
        auto_SocketFactory socket_factory 
      )
        : Client<ONCRPCRequest, ONCRPCResponse>
          ( 
            flags, log, operation_timeout, peername, 
            reconnect_tries_max, socket_factory 
          )
      { }

      static yidl::runtime::auto_Object< ONCRPCClient<InterfaceType> > 
        create
        ( 
          const URI& absolute_uri, 
          uint32_t flags = 0,
          YIELD::platform::auto_Log log = NULL, 
          const YIELD::platform::Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, 
          uint8_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT,
          auto_SSLContext ssl_context = NULL 
        );

      // YIELD::concurrency::EventHandler
      virtual void handleEvent( YIELD::concurrency::Event& ev )
      {
        if ( InterfaceType::checkRequest( ev ) != NULL )
        {
          ONCRPCRequest* oncrpc_request = new ONCRPCRequest( this->incRef(), ev );
#ifdef _DEBUG
          if ( ( this->get_flags() & this->CLIENT_FLAG_TRACE_OPERATIONS ) == 
               this->CLIENT_FLAG_TRACE_OPERATIONS && this->get_log() != NULL )
            this->get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << 
              "yield::ipc::ONCRPCClient: creating new ONCRPCRequest/" << 
              reinterpret_cast<uint64_t>( oncrpc_request ) << 
              " (xid=" << oncrpc_request->get_xid() << 
              ") for interface request " << ev.get_type_name() << ".";
#endif

          Client<ONCRPCRequest, ONCRPCResponse>::handleEvent( *oncrpc_request );
        }
        else
        {
#ifdef _DEBUG
          if ( ( this->get_flags() & this->CLIENT_FLAG_TRACE_OPERATIONS ) == 
            this->CLIENT_FLAG_TRACE_OPERATIONS && this->get_log() != NULL )
          {
            switch ( ev.get_type_id() )
            {
              case YIDL_RUNTIME_OBJECT_TYPE_ID( ONCRPCRequest ):
              {
                this->get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << 
                  "yield::ipc::ONCRPCClient: send()'ing ONCRPCRequest/" << 
                  reinterpret_cast<uint64_t>( &ev ) << 
                  " (xid=" << static_cast<ONCRPCRequest&>( ev ).get_xid() << ")."; 
              }
              break;

              case YIDL_RUNTIME_OBJECT_TYPE_ID( ONCRPCResponse ): 
              {
                this->get_log()->getStream( YIELD::platform::Log::LOG_INFO ) << 
                  "yield::ipc::ONCRPCClient: send()'ing ONCRPCRequest/" << 
                  reinterpret_cast<uint64_t>( &ev ) << 
                  " (xid=" << static_cast<ONCRPCResponse&>( ev ).get_xid() << ")."; 
              }
              break;
            }
          }
#endif

          Client<ONCRPCRequest, ONCRPCResponse>::handleEvent( ev );
        }
      }

    protected:
      virtual ~ONCRPCClient() { }
    };

    
    class ONCRPCServer : public yidl::runtime::Object
    {
    public:
      static yidl::runtime::auto_Object<ONCRPCServer>         
        create
        ( 
          const URI& absolute_uri,
          YIELD::concurrency::auto_Interface interface_,
          YIELD::platform::auto_Log log = NULL, 
          auto_SSLContext ssl_context = NULL 
        ); // Throws exceptions

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( ONCRPCServer, 0 );
     
    protected:
      ONCRPCServer( YIELD::concurrency::auto_Interface interface_, auto_Socket socket_ );

    private:
      YIELD::concurrency::auto_Interface interface_;
      auto_Socket socket_;

      class AIOAcceptControlBlock;
      class AIOReadControlBlock;
      class AIORecvFromControlBlock;
      class AIOWriteControlBlock;

      class ONCRPCResponseTarget;
    };

    typedef yidl::runtime::auto_Object<ONCRPCServer> auto_ONCRPCServer;


    class Pipe : public yidl::runtime::Object
    {
    public:
      static yidl::runtime::auto_Object<Pipe> create(); // Throws exceptions

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

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( Pipe, 6 );

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

    typedef yidl::runtime::auto_Object<Pipe> auto_Pipe;


    class Process : public yidl::runtime::Object
    {
    public:
      // create( ... ) factory methods throw exceptions
      static yidl::runtime::auto_Object<Process> 
        create( const YIELD::platform::Path& executable_file_path );

      static yidl::runtime::auto_Object<Process> 
        create( int argc, char** argv );

      static yidl::runtime::auto_Object<Process> 
        create
        ( 
          const YIELD::platform::Path& executable_file_path, 
          const char** null_terminated_argv 
        );

      auto_Pipe get_stdin() const { return child_stdin; }
      auto_Pipe get_stdout() const { return child_stdout; }
      auto_Pipe get_stderr() const { return child_stderr; }

      bool kill(); // SIGKILL
      bool poll( int* out_return_code = 0 ); // Calls waitpid() but WNOHANG
      bool terminate(); // SIGTERM
      int wait(); // Calls waitpid() and blocks

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( Process, 7 );

    private:
#ifdef _WIN32
      Process( void* hChildProcess, void* hChildThread,       
#else
      Process( pid_t child_pid, 
#endif
        auto_Pipe child_stdin, auto_Pipe child_stdout, auto_Pipe child_stderr );

      ~Process();

#ifdef _WIN32
      void *hChildProcess, *hChildThread;
#else
      int child_pid;
#endif
      auto_Pipe child_stdin, child_stdout, child_stderr;  
    };

    typedef yidl::runtime::auto_Object<Process> auto_Process;


#ifdef YIELD_HAVE_OPENSSL
    class SSLSocket : public TCPSocket
    {
    public:
      // create( ... ) factory methods return NULL instead of throwing exceptions
      static yidl::runtime::auto_Object<SSLSocket> create( auto_SSLContext );
      static yidl::runtime::auto_Object<SSLSocket> create( int domain, auto_SSLContext );

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( SSLSocket, 216 );

      // Socket
      void aio_connect( Socket::auto_AIOConnectControlBlock );
      void aio_read( yidl::runtime::auto_Object<AIOReadControlBlock> );
      void aio_write( yidl::runtime::auto_Object<AIOWriteControlBlock> );
      bool connect( auto_SocketAddress peername );
      virtual ssize_t read( void* buffer, size_t buffer_len );
      virtual bool want_read() const;
      virtual bool want_write() const;
      virtual ssize_t write( const void* buffer, size_t buffer_len );
      virtual ssize_t writev( const struct iovec* buffers, uint32_t buffers_count );

      // TCPSocket
      auto_TCPSocket accept();
      void aio_accept( yidl::runtime::auto_Object<AIOAcceptControlBlock> );    
      virtual bool shutdown();

    protected:
#if defined(_WIN64)
      SSLSocket( int domain, uint64_t socket_, auto_SSLContext, SSL* );
#elif defined(_WIN32)
      SSLSocket( int domain, uint32_t socket_, auto_SSLContext, SSL* );
#else
      SSLSocket( int domain, int socket_, auto_SSLContext, SSL* );
#endif
      ~SSLSocket();

      auto_SSLContext ctx;
      SSL* ssl;
    };

    typedef yidl::runtime::auto_Object<SSLSocket> auto_SSLSocket;

    
    class SSLSocketFactory : public SocketFactory
    {
    public:
      SSLSocketFactory( auto_SSLContext ssl_context )
        : ssl_context( ssl_context )
      { }

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( SSLSocketFactory, 0 );

      // SocketFactory
      auto_Socket createSocket()
      {
        return SSLSocket::create( ssl_context ).release();
      }

    private:
      auto_SSLContext ssl_context;
    };

    typedef yidl::runtime::auto_Object<SSLSocketFactory> auto_SSLSocketFactory;
#endif


    class TCPSocketFactory : public SocketFactory
    {
    public:
      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( TCPSocketFactory, 0 );

      // SocketFactory
      auto_Socket createSocket()
      {
        return TCPSocket::create().release();
      }
    };

    typedef yidl::runtime::auto_Object<TCPSocketFactory> auto_TCPSocketFactory;


    class TracingSocket : public Socket
    {
    public:
      TracingSocket( auto_Socket underlying_socket, YIELD::platform::auto_Log log );
    
      // yidl::runtime::Object
      virtual uint32_t get_type_id() const { return underlying_socket->get_type_id(); }
      const char* get_type_name() const { return underlying_socket->get_type_name(); }

      // Socket
      void aio_connect( Socket::auto_AIOConnectControlBlock aio_connect_control_block );
      void aio_read( Socket::auto_AIOReadControlBlock aio_read_control_block );
      void aio_write( Socket::auto_AIOWriteControlBlock aio_write_control_block );
      bool bind( auto_SocketAddress to_sockaddr ) { return underlying_socket->bind( to_sockaddr ); }
      bool close() { return underlying_socket->close(); }
      bool connect( auto_SocketAddress to_sockaddr );
      bool get_blocking_mode() const { return underlying_socket->get_blocking_mode(); }
      auto_SocketAddress getpeername() { return underlying_socket->getpeername(); }
      auto_SocketAddress getsockname() { return underlying_socket->getsockname(); }      
      ssize_t read( void* buffer, size_t buffer_len );
      bool set_blocking_mode( bool blocking ) { return underlying_socket->set_blocking_mode( blocking ); }
      bool want_connect() const;
      bool want_read() const;
      bool want_write() const;
      ssize_t writev( const struct iovec* buffers, uint32_t buffers_count );

    private:
      ~TracingSocket() { }

      auto_Socket underlying_socket;
      YIELD::platform::auto_Log log;
    };


    class UDPSocket : public Socket
    {
    public:
      class AIORecvFromControlBlock : public AIOControlBlock
      {
      public:
        AIORecvFromControlBlock( yidl::runtime::auto_Buffer buffer );

        yidl::runtime::auto_Buffer get_buffer() const { return buffer; }
        auto_SocketAddress get_peername() const;

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( UDPSocket::AIORecvFromControlBlock, 0 );

        // Socket::AIOControlBlock
        ExecuteStatus execute()
        {
          ssize_t recvfrom_ret = 
            static_cast<UDPSocket*>( get_socket().get() )->
              recvfrom( buffer, *peername );

          if ( recvfrom_ret > 0 )
            onCompletion( static_cast<size_t>( recvfrom_ret ) );
          else if ( recvfrom_ret == 0 )
            DebugBreak();
          else if ( get_socket()->want_read() )
            return EXECUTE_STATUS_WANT_READ;
          else
            onError( YIELD::platform::Exception::get_errno() );          

          return EXECUTE_STATUS_DONE;
        }

      protected:
        ~AIORecvFromControlBlock();

      private:
        friend class UDPSocket;

        yidl::runtime::auto_Buffer buffer;
        struct sockaddr_storage* peername;
      };


      void aio_recvfrom( yidl::runtime::auto_Object<AIORecvFromControlBlock> aio_recvfrom_control_block );
      static yidl::runtime::auto_Object<UDPSocket> create(); // returns NULL instead of throwing exceptions
      ssize_t recvfrom( yidl::runtime::auto_Buffer buffer, struct sockaddr_storage& peername );
      ssize_t recvfrom( void* buffer, size_t buffer_len, struct sockaddr_storage& peername );
      ssize_t sendto( yidl::runtime::auto_Buffer buffer, auto_SocketAddress peername );
      ssize_t sendto( const void* buffer, size_t buffer_len, auto_SocketAddress peername );

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( UDPSocket, 219 );

    protected:
#ifdef _WIN32
      void aio_recvfrom_iocp( yidl::runtime::auto_Object<AIORecvFromControlBlock> );
#endif
      void aio_recvfrom_nbio( yidl::runtime::auto_Object<AIORecvFromControlBlock> );

    private:
#if defined(_WIN64)
      UDPSocket( int domain, uint64_t socket_ );    
#elif defined(_WIN32)
      UDPSocket( int domain, uint32_t socket_ );
#else
      UDPSocket( int domain, int socket_ );
#endif
      ~UDPSocket() { }
    };

    typedef yidl::runtime::auto_Object<UDPSocket> auto_UDPSocket;


    class UDPSocketFactory : public SocketFactory
    {
    public:
      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( UDPSocketFactory, 0 );

      // SocketFactory
      auto_Socket createSocket()
      {
        return UDPSocket::create().release();
      }
    };

    typedef yidl::runtime::auto_Object<UDPSocketFactory> auto_UDPSocketFactory;


    class URI : public yidl::runtime::Object
    {
    public:
      // parse( ... ) factory methods return NULL instead of throwing exceptions
      static yidl::runtime::auto_Object<URI> parse( const char* uri );
      static yidl::runtime::auto_Object<URI> parse( const std::string& uri );
      static yidl::runtime::auto_Object<URI> parse( const char* uri, size_t uri_len );

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
      const std::multimap<std::string, std::string>& get_query() const { return query; }
      std::string get_query_value( const std::string& key, const char* default_query_value = "" ) const; 
      std::multimap<std::string, std::string>::const_iterator get_query_values( const std::string& key ) const;
      void set_port( unsigned short port ) { this->port = port; }

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( URI, 221 );

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

    typedef yidl::runtime::auto_Object<URI> auto_URI;


    class UUID : public yidl::runtime::Object
    {
    public:
      UUID();
      UUID( const std::string& uuid_from_string );
      ~UUID();

      bool operator==( const UUID& ) const;
      operator std::string() const;

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( UUID, 222 );

    private:
#if defined(_WIN32)
      void* win32_uuid;
#elif defined(YIELD_HAVE_LIBUUID)
      uuid_t libuuid_uuid;
#else
      char generic_uuid[256];
#endif
    };


      template <class InterfaceType>
      yidl::runtime::auto_Object< ONCRPCClient<InterfaceType> > 
        ONCRPCClient<InterfaceType>::create( 
          const URI& absolute_uri,
          uint32_t flags,
          YIELD::platform::auto_Log log, 
          const YIELD::platform::Time& operation_timeout,
          uint8_t reconnect_tries_max,
          auto_SSLContext ssl_context 
        )
      {
        auto_SocketAddress peername = SocketAddress::create( absolute_uri );
        if ( peername != NULL )
        {
          auto_SocketFactory socket_factory;

#ifdef YIELD_HAVE_OPENSSL
          if ( absolute_uri.get_scheme() == "oncrpcs" )
          {
            if ( ssl_context != NULL )
              socket_factory = new SSLSocketFactory( ssl_context );
            else
            {
              ssl_context = SSLContext::create( SSLv23_client_method() );
              if ( ssl_context != NULL )
                socket_factory = new SSLSocketFactory( ssl_context ); 
              else
                throw YIELD::platform::Exception();
            }
          }
          else
#endif
          if ( absolute_uri.get_scheme() == "oncrpcu" )
            socket_factory = new UDPSocketFactory;
          else
            socket_factory = new TCPSocketFactory;

          return new ONCRPCClient<InterfaceType>
          ( 
            flags, log, operation_timeout, peername, 
            reconnect_tries_max, socket_factory 
          );
        }
        else
          throw YIELD::platform::Exception();
      }
  };
};

#endif
