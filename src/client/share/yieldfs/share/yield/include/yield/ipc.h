// Copyright 2003-2008 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _YIELD_IPC_H
#define _YIELD_IPC_H

#include "yield/concurrency.h"


#if defined(__FreeBSD__) || defined(__OpenBSD__) || defined(__MACH__)
#define YIELD_HAVE_FREEBSD_KQUEUE 1
struct kevent;
#elif defined(__linux__)
#define YIELD_HAVE_LINUX_EPOLL 1
struct epoll_event;
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
struct port_event;
typedef port_event port_event_t;
#endif

#if defined(_WIN32)
struct fd_set;
#elif !defined(YIELD_HAVE_QUEUE) && !defined(YIELD_HAVE_LINUX_EPOLL) && !defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
#include <poll.h>
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


#define YIELD_SOCKET_PROTOTYPES \
virtual ConnectStatus aio_connect( auto_Object<AIOConnectControlBlock> aio_connect_control_block ); \
virtual ssize_t aio_read( auto_Object<AIOReadControlBlock> aio_read_control_block ); \
virtual bool bind( auto_SocketAddress to_sockaddr ); \
virtual bool close(); \
virtual ConnectStatus connect( auto_SocketAddress to_sockaddr ); \
virtual bool get_blocking_mode() const; \
virtual auto_SocketAddress getpeername(); \
virtual auto_SocketAddress getsockname(); \
virtual operator int() const; \
virtual ssize_t read( void* buffer, size_t buffer_len ); \
virtual bool set_blocking_mode( bool blocking ); \
virtual bool shutdown(); \
virtual bool want_read() const; \
virtual bool want_write() const; \
virtual ssize_t write( const void* buffer, size_t buffer_len ); \
virtual ssize_t writev( const struct iovec* buffers, uint32_t buffers_count );


namespace YIELD
{
  class Connection;
  class HTTPRequest;
  class ONCRPCRequest;
  class TCPSocketPair;
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
        auto_Buffer first_out_buffer = new StackBuffer<4096>;
        auto_Buffer current_out_buffer = first_out_buffer;
        zstream.next_out = static_cast<Bytef*>( static_cast<void*>( *current_out_buffer ) );
        zstream.avail_out = current_out_buffer->capacity();

/*
        std::vector<struct iovec> iovecs;

        for ( size_t iovec_i = 0; iovec_i < iovecs.size(); iovec_i++ )
        {
          zstream.next_in = reinterpret_cast<Bytef*>( iovecs[iovec_i].iov_base );
          zstream.avail_in = iovecs[iovec_i].iov_len;

          int deflate_ret;
          while ( ( deflate_ret = ::deflate( &zstream, Z_NO_FLUSH ) ) == Z_OK )
          {
            if ( zstream.avail_out > 0 )
            {
              while ( ( deflate_ret = ::deflate( &zstream, Z_FINISH ) ) == Z_OK ) // Z_OK = need more buffer space to finish compression, Z_STREAM_END = really done
              {
                current_out_buffer->put( NULL, current_out_buffer->capacity() );
                auto_Buffer new_out_buffer = new StackBuffer<4096>;
                current_out_buffer->set_next_buffer( new_out_buffer );
                current_out_buffer = new_out_buffer;
                zstream.next_out = static_cast<Bytef*>( static_cast<void*>( *current_out_buffer ) );
                zstream.avail_out = current_out_buffer->capacity();
              }

              if ( deflate_ret == Z_STREAM_END )
              {
                if ( ( deflate_ret = deflateEnd( &zstream ) ) == Z_OK )
                {
                  if ( zstream.avail_out < current_out_buffer->capacity() )
                    current_out_buffer->put( NULL, current_out_buffer->capacity() - zstream.avail_out );

                  return first_out_buffer;
                }
              }
            }
            else
            {
              current_out_buffer->put( NULL, current_out_buffer->capacity() );
              StackBuffer<4096> new_out_buffer = new StackBuffer<4096>;
              current_out_buffer->set_next_buffer( new_out_buffer );
              current_out_buffer = new_out_buffer;
              zstream.next_out = static_cast<Bytef*>( static_cast<void*>( *current_out_buffer ) );
              zstream.avail_out = current_out_buffer->capacity();
            }
          }
        }
        */
      }

      return NULL;
    }
  };
#endif


  class EventFDPipe : public Object 
  {
  public:
    static auto_Object<EventFDPipe> create();

#ifdef YIELD_HAVE_LINUX_EVENTFD
    int get_read_end() const { return fd; }
    int get_write_end() const { return fd; }
#else
    int get_read_end() const;
    int get_write_end() const;
#endif

    void clear();
    void signal();

    // Object
    YIELD_OBJECT_PROTOTYPES( EventFDPipe, 0 );

  private:
#ifdef YIELD_HAVE_LINUX_EVENTFD
    EventFDPipe( int fd );
#else
    EventFDPipe( auto_Object<TCPSocketPair> tcp_socket_pair );
#endif
    ~EventFDPipe();

#ifdef YIELD_HAVE_LINUX_EVENTFD
    int fd;
#else
    auto_Object<TCPSocketPair> tcp_socket_pair;
#endif
  };

  typedef auto_Object<EventFDPipe> auto_EventFDPipe;


  class FDEventQueue : public EventQueue, private NonBlockingFiniteQueue<Event*, 256> 
  {
  public:
    class POLLERREvent : public Event
    {
    public:
      POLLERREvent( auto_Object<> context, int32_t errno_ )
        : context( context ), errno_( errno_ )
      { }

      inline auto_Object<> get_context() const { return context; }
      inline int32_t get_errno() const { return errno_; }

      // Object
      YIELD_OBJECT_PROTOTYPES( POLLERREvent, 224 );

    private:
      auto_Object<> context;        
      int32_t errno_;
    };


    class POLLINEvent : public Event
    {
    public:
      POLLINEvent( auto_Object<> context )
        : context( context )
      { }

      inline auto_Object<> get_context() const { return context; }

      // Object
      YIELD_OBJECT_PROTOTYPES( POLLINEvent, 225 );

    private:
      auto_Object<> context;        
    };


    class POLLOUTEvent : public Event
    {
    public:
      POLLOUTEvent( auto_Object<> context )
        : context( context )
      { }

      inline auto_Object<> get_context() const { return context; }

      // Object
      YIELD_OBJECT_PROTOTYPES( POLLOUTEvent, 226 );

    private:
      auto_Object<> context;        
    };


    FDEventQueue();

    bool attach( int fd, auto_Object<> context = NULL, bool enable_read = true, bool enable_write = false );
    void detach( int fd );
    bool toggle( int fd, bool enable_read, bool enable_write );

    // Object
    YIELD_OBJECT_PROTOTYPES( FDEventQueue, 203 );

    // EventQueue
    virtual Event* dequeue() { return dequeue( static_cast<uint64_t>( -1  ) ); }
    virtual Event* dequeue( uint64_t timeout_ns );
    virtual bool enqueue( Event& );

  protected:
    virtual ~FDEventQueue();

  private:
#if defined(_WIN32)
    fd_set *read_fds, *read_fds_copy, *write_fds, *write_fds_copy, *except_fds, *except_fds_copy;
    STLHashMap<Object*>::iterator next_fd_to_check;
#elif defined(YIELD_HAVE_LINUX_EPOLL)
    int poll_fd;
    struct epoll_event* returned_events;
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
    int poll_fd;
    struct kevent* returned_events;
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
    int poll_fd;
    port_event_t* returned_events;
#else
    typedef std::vector<pollfd> pollfd_vector;
    pollfd_vector::size_type next_pollfd_to_check;
    pollfd_vector pollfds;
#endif

    int active_fds;
    auto_Object<EventFDPipe> eventfd_pipe;
    STLHashMap<Object*> fd_to_context_map;    
  };


  class IOCompletionPort : public EventQueue, private NonBlockingFiniteQueue<Event*, 256>
  {
  public:
    IOCompletionPort();
    ~IOCompletionPort();

    bool attach( int fd );

    // Object
    YIELD_OBJECT_PROTOTYPES( IOCompletionPort, 228 );

    // EventQueue
    virtual Event* dequeue() { return dequeue( static_cast<uint64_t>( -1  ) ); }
    virtual Event* dequeue( uint64_t timeout_ns );
    bool enqueue( Event& );
    
  private:
#ifdef _WIN32
    void* hIoCompletionPort;
#endif
  };


  template <class ReadProtocolMessageType, class WriteProtocolMessageType>
  class Peer
  {
  public:
    const static uint32_t PEER_FLAG_TRACE_IO = 1;
    const static uint32_t PEER_FLAG_TRACE_OPERATIONS = 2;    

    // EventTarget
    virtual void send( Event& ev );

  protected:
    Peer( uint32_t flags, auto_Log log );

    void attach( auto_Object<Connection> connection, auto_Object<> context, bool enable_read, bool enable_write );
    virtual ssize_t deserialize( auto_Object<ReadProtocolMessageType> protocol_message, auto_Buffer buffer );    
    void detach( auto_Object<Connection> connection );
    auto_Stage get_helper_peer_stage() const { return helper_peer_stage != NULL ? helper_peer_stage : my_stage; }    
    uint32_t get_flags() const { return flags; }
    auto_Log get_log() const { return log; }
    virtual void handleDeserializedProtocolMessage( auto_Object<ReadProtocolMessageType> protocol_message ) = 0;
    virtual void handleEvent( Event& );
    inline bool haveAIO() const { return io_completion_port != NULL; }
    virtual bool read( auto_Object<ReadProtocolMessageType> protocol_message, size_t buffer_capacity = 1024 );
    void set_helper_peer_stage( auto_Stage helper_peer_stage ) { this->helper_peer_stage = helper_peer_stage; }    
    virtual auto_Buffer serialize( auto_Object<WriteProtocolMessageType> protocol_message );
    virtual bool write( auto_Object<WriteProtocolMessageType> protocol_message, auto_Buffer buffer );

  private:
    uint32_t flags;
    auto_Log log;
    auto_Object<FDEventQueue> fd_event_queue;
    auto_Object<IOCompletionPort> io_completion_port;
    auto_Stage my_stage;
    auto_Stage helper_peer_stage;
  };


  class ProtocolRequest : public Request
  { 
  public:
    virtual ~ProtocolRequest() { }

    auto_Object<Connection> get_connection() const { return connection; }
    void set_connection( auto_Object<Connection> connection ) { this->connection = connection; }

  protected:
    ProtocolRequest() // Outgoing
    { }
    
    ProtocolRequest( auto_Object<Connection> connection ) // Incoming
      : connection( connection )
    { }

    ProtocolRequest( const ProtocolRequest& )
    {
      DebugBreak(); // Prevent copying
    }

  private:
    auto_Object<Connection> connection;
  };

  
  template <class ProtocolRequestType>
  class ProtocolResponse : public Response
  { 
  public:
    virtual ~ProtocolResponse() { }

    auto_Object<Connection> get_connection() const { return protocol_request->get_connection(); }
    auto_Object<ProtocolRequestType> get_protocol_request() const { return protocol_request; }

  protected:
    ProtocolResponse( auto_Object<ProtocolRequestType> protocol_request )
      : protocol_request( protocol_request )
    { }

    ProtocolResponse( const ProtocolResponse<ProtocolRequestType>& )
    { 
      DebugBreak(); // Prevent copying
    } 
  
  private:
    auto_Object<ProtocolRequestType> protocol_request;
  };


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
    class AIOConnectControlBlock : public AIOControlBlock<Event>
    {
    public:
      AIOConnectControlBlock( auto_Object<> context, auto_SocketAddress peername, auto_Buffer write_buffer )
        : context( context ), peername( peername ), write_buffer( write_buffer )
      { }

      auto_Object<> get_context() const { return context; }
      auto_SocketAddress get_peername() const { return peername; }
      auto_Buffer get_write_buffer() const { return write_buffer; }

      // Object
      YIELD_OBJECT_PROTOTYPES( AIOConnectControlBlock, 223 );

    private:
      auto_Object<> context;
      auto_SocketAddress peername;
      auto_Buffer write_buffer;
    };


    class AIOReadControlBlock : public AIOControlBlock<Event>
    {
    public:
      AIOReadControlBlock( auto_Buffer buffer, auto_Object<> context )
        : buffer( buffer ), context( context )
      { }

      auto_Buffer get_buffer() const { return buffer; }
      auto_Object<> get_context() const { return context; }

      // Object
      YIELD_OBJECT_PROTOTYPES( AIOReadControlBlock, 227 );

      // AIOControlBlock
      virtual void onCompletion( size_t bytes_transferred )
      { 
        AIOControlBlock<Event>::onCompletion( bytes_transferred );
        buffer->put( NULL, bytes_transferred ); 
      }

    private:
      auto_Buffer buffer;
      auto_Object<> context;
    };


    enum ConnectStatus { CONNECT_STATUS_ERROR = -1, CONNECT_STATUS_OK = 0, CONNECT_STATUS_WOULDBLOCK = 1 };
    int get_domain() const { return domain; }
    int get_protocol() const { return protocol; }
    int get_type() const { return type; }
    bool operator==( const Socket& other ) const { return static_cast<int>( *this ) == static_cast<int>( other ); } \
    virtual ssize_t read( auto_Buffer buffer );
    void set_read_buffer( auto_Buffer buffer ) { this->read_buffer = read_buffer; }
    virtual ssize_t write( auto_Buffer buffer );
    YIELD_SOCKET_PROTOTYPES;

    // Object
    YIELD_OBJECT_PROTOTYPES( Socket, 211 );

  protected:
    Socket( int domain, int type, int protocol, int _socket );
    virtual ~Socket();

    static int create( int& domain, int type, int protocol );

  private:
    Socket( const Socket& ) { DebugBreak(); } // Prevent copying

    int domain, type, protocol;
    auto_Buffer read_buffer;
    int _socket;

    bool blocking_mode;
  };

  typedef auto_Object<Socket> auto_Socket;


  class TCPSocket : public Socket
  {
  public:
    class AIOAcceptControlBlock : public AIOControlBlock<Event>
    {
    public:
      AIOAcceptControlBlock( auto_Object<TCPSocket> accepted_tcp_socket, auto_Buffer read_buffer );

      auto_Object<TCPSocket> get_accepted_tcp_socket() const { return accepted_tcp_socket; }
      auto_Buffer get_read_buffer() const { return read_buffer; }

      // Object
      YIELD_OBJECT_PROTOTYPES( AIOAcceptControlBlock, 222 );

      // AIOControlBlock
      virtual void onCompletion( size_t bytes_transferred );

    private:
      ~AIOAcceptControlBlock() { }

      auto_Object<TCPSocket> accepted_tcp_socket;
      auto_Buffer read_buffer;
    };


    static auto_Object<TCPSocket> create(); // Defaults to domain = AF_INET6
    virtual auto_Object<TCPSocket> accept();
#ifdef _WIN32
    virtual bool aio_accept( auto_Object<AIOAcceptControlBlock> aio_accept_control_block );
    virtual ConnectStatus aio_connect( auto_Object<AIOConnectControlBlock> aio_connect_control_block );
#endif
    virtual bool listen();
    virtual bool shutdown();

    // Object
    YIELD_OBJECT_PROTOTYPES( TCPSocket, 212 );

  protected:
    TCPSocket( int domain, int _socket );
    virtual ~TCPSocket() { }

    int _accept();

  private:
#ifdef _WIN32
    static void *lpfnAcceptEx, *lpfnConnectEx;
#endif

    size_t partial_write_len;
  };

  typedef auto_Object<TCPSocket> auto_TCPSocket;


  class TCPSocketPair : public Object
  {
  public:
    static auto_Object<TCPSocketPair> create();

    auto_TCPSocket get_read_end() const { return read_end; }
    auto_TCPSocket get_write_end() const { return write_end; }

    ssize_t read( void* buffer, size_t buffer_len );
    ssize_t write( const void* buffer, size_t buffer_len );

    // Object
    YIELD_OBJECT_PROTOTYPES( TCPSocketPair, 0 );

  private:
    TCPSocketPair( auto_TCPSocket read_end, auto_TCPSocket write_end );
    ~TCPSocketPair() { }

    auto_TCPSocket read_end, write_end;
  };

  typedef auto_Object<TCPSocketPair> auto_TCPSocketPair;


  class TCPListenQueue : public FDEventQueue
  {
  public:
    static auto_Object<TCPListenQueue> create( auto_SocketAddress sockname );
 
    // EventQueue
    Event* dequeue() { return dequeue( static_cast<uint64_t>( -1 ) ); }
    Event* dequeue( uint64_t timeout_ns );

  protected:
    TCPListenQueue( auto_Object<TCPSocket> listen_tcp_socket );
    ~TCPListenQueue() { }

  private:
    auto_Object<TCPSocket> listen_tcp_socket;
  };

  typedef auto_Object<TCPListenQueue> auto_TCPListenQueue;


  class SSLContext : public Object
  {
  public:
#ifdef YIELD_HAVE_OPENSSL
    SSLContext( SSL_METHOD* method = SSLv23_client_method() ); // No certificate
    SSLContext( SSL_METHOD* method, const Path& pem_certificate_file_path, const Path& pem_private_key_file_path, const std::string& pem_private_key_passphrase );
    SSLContext( SSL_METHOD* method, const std::string& pem_certificate_str, const std::string& pem_private_key_str, const std::string& pem_private_key_passphrase );
    SSLContext( SSL_METHOD* method, const Path& pkcs12_file_path, const std::string& pkcs12_passphrase );
#else
    SSLContext();
#endif

#ifdef YIELD_HAVE_OPENSSL
    SSL_CTX* get_ssl_ctx() const { return ctx; }
#endif

    // Object
    YIELD_OBJECT_PROTOTYPES( SSLContext, 215 );

  private:
    ~SSLContext();

    std::string pem_private_key_passphrase;
#ifdef YIELD_HAVE_OPENSSL
    SSL_CTX* ctx;

    SSL_CTX* createSSL_CTX( SSL_METHOD* method );
    static int pem_password_callback( char *buf, int size, int, void *userdata );
    void throwOpenSSLException();
#endif
	};

  typedef auto_Object<SSLContext> auto_SSLContext;


#ifdef YIELD_HAVE_OPENSSL

  class SSLSocket : public TCPSocket
  {
  public:
    static auto_Object<SSLSocket> create( auto_SSLContext ctx );

    // Object
    YIELD_OBJECT_PROTOTYPES( SSLSocket, 216 );

    // Socket
    virtual ssize_t read( void* buffer, size_t buffer_len );
    virtual bool want_read() const;
    virtual bool want_write() const;
    virtual ssize_t write( const void* buffer, size_t buffer_len );
    virtual ssize_t writev( const struct iovec* buffers, uint32_t buffers_count );

    // TCPSocket
    auto_Object<TCPSocket> accept();
    ConnectStatus connect( auto_SocketAddress peername );
    bool shutdown();

  private:
    SSLSocket( int domain, int _socket, auto_SSLContext ctx, SSL* ssl );
    ~SSLSocket();

    auto_SSLContext ctx;
    SSL* ssl;
  };

  typedef auto_Object<SSLSocket> auto_SSLSocket;


  class SSLListenQueue : public TCPListenQueue
  {
  public:
    static auto_Object<SSLListenQueue> create( auto_SocketAddress sockname, auto_SSLContext ssl_context );
 
  private:
    SSLListenQueue( auto_Object<SSLSocket> listen_ssl_socket );
    ~SSLListenQueue() { }
  };

  typedef auto_Object<SSLListenQueue> auto_SSLListenQueue;

#endif

  class TracingSocket : public Socket
  {
  public:
    TracingSocket( auto_Socket underlying_socket, auto_Log log );
  
    // Object
    virtual uint32_t get_tag() const { return underlying_socket->get_tag(); }
    const char* get_type_name() const { return underlying_socket->get_type_name(); }

    // Socket
    YIELD_SOCKET_PROTOTYPES;

  private:
    ~TracingSocket() { }

    auto_Socket underlying_socket;
    auto_Log log;
  };


  class UDPSocket : public Socket
  {
  public:
    static auto_Object<UDPSocket> create();

    // Object
    YIELD_OBJECT_PROTOTYPES( UDPSocket, 219 );

  private:
    UDPSocket( int domain, int _socket );    
    ~UDPSocket() { }
  };

  typedef auto_Object<UDPSocket> auto_UDPSocket;


  class UDPRecvFromQueue : public FDEventQueue
  {
  public:
    static auto_Object<UDPRecvFromQueue> create( auto_SocketAddress sockname );
 
    // Object
    YIELD_OBJECT_PROTOTYPES( UDPRecvFromQueue, 0 );

    // EventQueue
    Event* dequeue( uint64_t timeout_ns );

  private:
    UDPRecvFromQueue( auto_SocketAddress recvfrom_sockname, auto_Object<UDPSocket> recvfrom_udp_socket );
    ~UDPRecvFromQueue() { }

    auto_SocketAddress recvfrom_sockname;
    auto_Object<UDPSocket> recvfrom_udp_socket;
  };

  typedef auto_Object<UDPRecvFromQueue> auto_UDPRecvFromQueue;


  template <class ProtocolRequestType, class ProtocolResponseType>
  class Client : public Peer<ProtocolResponseType, ProtocolRequestType>
  {
  public:    
    const static uint64_t OPERATION_TIMEOUT_DEFAULT = 30 * NS_IN_S;
    const static uint8_t OPERATION_RETRIES_MAX_DEFAULT = UINT8_MAX;    

  protected:
    Client( const URI& absolute_uri, uint32_t flags, auto_Log log, uint8_t operation_retries_max, const Time& operation_timeout, auto_SocketAddress peername, auto_SSLContext ssl_context );
    virtual ~Client();

    auto_Stage get_protocol_request_writer_stage() const { return this->get_helper_peer_stage(); }
    auto_Stage get_protocol_response_reader_stage() const { return this->get_helper_peer_stage(); }
    void set_protocol_request_writer_stage( auto_Stage protocol_request_writer_stage ) { this->set_helper_peer_stage( protocol_request_writer_stage ); }
    void set_protocol_response_reader_stage( auto_Stage protocol_response_reader_stage ) { this->set_helper_peer_stage( protocol_response_reader_stage ); }

    // Peer
    virtual ssize_t deserialize( auto_Object<ProtocolResponseType> protocol_response, auto_Buffer buffer );    
    virtual void handleEvent( Event& );
    virtual void handleDeserializedProtocolMessage( auto_Object<ProtocolResponseType> protocol_response );
    virtual bool read( auto_Object<ProtocolResponseType> protocol_response, size_t buffer_capacity );
    virtual bool write( auto_Object<ProtocolRequestType> protocol_request, auto_Buffer buffer );

  private:
    auto_Object<URI> absolute_uri;
    uint8_t operation_retries_max;
    Time operation_timeout;
    auto_SocketAddress peername;
    auto_SSLContext ssl_context;

    std::vector<Connection*> idle_connections;
  };


  class Connection : public Event
  {
  public:
    Connection( auto_Socket _socket )
      : _socket( _socket )
    { }

    auto_Socket get_socket() const { return _socket; }

    bool operator==( const Connection& other ) const { return _socket == other._socket; }

    // Object
    YIELD_OBJECT_PROTOTYPES( Connection, 201 );

  private:
    ~Connection() { }

    auto_Socket _socket;
  };


  template <class ProtocolRequestType, class ProtocolResponseType>
  class Server : public EventHandler, public Peer<ProtocolRequestType, ProtocolResponseType>
  {
  public:
    // EventHandler
    virtual void handleEvent( Event& );

    // EventTarget
    void send( Event& ev ) { Peer<ProtocolRequestType, ProtocolResponseType>::send( ev ); }

  protected:
    Server( auto_Log log ) 
      : Peer<ProtocolRequestType, ProtocolResponseType>( 0, log )
    { }

    virtual auto_Object<ProtocolRequestType> createProtocolRequest( auto_Object<Connection> connection ) = 0;
    auto_Stage get_protocol_request_reader_stage() const { return this->get_helper_peer_stage(); }
    auto_Stage get_protocol_response_writer_stage() const { return this->get_helper_peer_stage(); }
    void set_protocol_request_reader_stage( auto_Stage protocol_request_reader_stage ) { this->set_helper_peer_stage( protocol_request_reader_stage ); }
    void set_protocol_response_writer_stage( auto_Stage protocol_response_writer_stage ) { this->set_helper_peer_stage( protocol_response_writer_stage ); }

    // Peer
    virtual bool write( auto_Object<ProtocolResponseType> protocol_response, auto_Buffer buffer );
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
    static auto_Object<HTTPBenchmarkDriver> create( auto_EventTarget http_request_target, uint8_t in_flight_request_count, const Path& wlog_file_path );
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

    auto_Stage my_stage;
    Mutex wait_signal;

    void sendHTTPRequest();

    // Statistics
    Mutex statistics_lock;
    static TimerQueue statistics_timer_queue;
    uint32_t requests_sent_in_period, responses_received_in_period;
    std::vector<double> request_rates, response_rates;

    class StatisticsTimer : public TimerQueue::Timer
    {
    public:
      StatisticsTimer( auto_Object<HTTPBenchmarkDriver> http_benchmark_driver )
        : Timer( 5 * NS_IN_S, 5 * NS_IN_S ), http_benchmark_driver( http_benchmark_driver )
      { }

      // Timer
      bool fire( const Time& elapsed_time ) 
      {
        http_benchmark_driver->calculateStatistics( elapsed_time );
        return true;
      }

    private:
      auto_Object<HTTPBenchmarkDriver> http_benchmark_driver;
    };

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


  class HTTPResponse : public ProtocolResponse<HTTPRequest>, public HTTPMessage
  {
  public:
    HTTPResponse( auto_Object<HTTPRequest> http_request ); // Incoming
    HTTPResponse( auto_Object<HTTPRequest> http_request, uint16_t status_code ); // Outgoing
    HTTPResponse( auto_Object<HTTPRequest> http_request, uint16_t status_code, auto_Buffer body ); // Outgoing

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
    HTTPResponse( const HTTPResponse& http_response ) // Prevent copying
      : ProtocolResponse<HTTPRequest>( http_response ), HTTPMessage( 0 )
    { }

    uint8_t http_version;
    union { char status_code_str[4]; uint16_t status_code; };
  };

  typedef auto_Object<HTTPResponse> auto_HTTPResponse;


  class HTTPRequest : public ProtocolRequest, public HTTPMessage
  {
  public:
    HTTPRequest( auto_Object<Connection> connection ); // Incoming
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

    // ProtocolRequest
    auto_HTTPResponse createProtocolResponse() { return new HTTPResponse( incRef() ); }

  protected:
    virtual ~HTTPRequest();

  private:
    HTTPRequest( const HTTPRequest& http_request ) // Prevent copying
      : ProtocolRequest( http_request ), HTTPMessage( 0 )
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
                                           auto_Object<StageGroup> stage_group, 
                                           uint32_t flags = 0,
                                           auto_Log log = NULL, 
                                           uint8_t operation_retries_max = OPERATION_RETRIES_MAX_DEFAULT,
                                           const Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, 
                                           auto_SSLContext ssl_context = NULL );

    static auto_HTTPResponse GET( const URI& absolute_uri, auto_Log log = NULL );
    static auto_HTTPResponse PUT( const URI& absolute_uri, auto_Buffer body, auto_Log log = NULL );
    static auto_HTTPResponse PUT( const URI& absolute_uri, const Path& body_file_path, auto_Log log = NULL );

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPClient, 207 );

    // EventHandler
    virtual void handleEvent( Event& ev ) { Client<HTTPRequest, HTTPResponse>::handleEvent( ev ); }

    // EventTarget
    virtual void send( Event& ev ) { Client<HTTPRequest, HTTPResponse>::send( ev ); }

  private:
    HTTPClient( const URI& absolute_uri, uint32_t flags, auto_Log log, uint8_t operation_retries_max, const Time& operation_timeout, auto_SocketAddress peername, auto_SSLContext ssl_context )
      : Client<HTTPRequest, HTTPResponse>( absolute_uri, flags, log, operation_retries_max, operation_timeout, peername, ssl_context )
    { }

    virtual ~HTTPClient() { }

    static auto_HTTPResponse sendHTTPRequest( const char* method, const URI& uri, auto_Buffer body, auto_Log log );

    void set_http_request_writer_stage( auto_Stage http_request_writer_stage ) { set_protocol_request_writer_stage( http_request_writer_stage ); }
    void set_http_response_reader_stage( auto_Stage http_response_reader_stage ) { set_protocol_response_reader_stage( http_response_reader_stage ); }
  };

  typedef auto_Object<HTTPClient> auto_HTTPClient;
    

  class HTTPServer : public Server<HTTPRequest, HTTPResponse>
  {
  public:
    template <class StageGroupType> 
    static auto_Object<HTTPServer> create( const URI& absolute_uri,
                                           auto_EventTarget http_request_target, 
                                           auto_Object<StageGroupType> stage_group,                        
                                           auto_Log log = NULL, 
                                           auto_SSLContext ssl_context = NULL );

    template <class StageGroupType>
    static auto_Object<HTTPServer> create( auto_EventTarget http_request_target,
                                           auto_Object<StageGroupType> stage_group,
                                           auto_Object<TCPListenQueue> tcp_listen_queue,
                                           auto_Log log = NULL );

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPServer, 0 );

  protected:
    HTTPServer( auto_EventTarget http_request_target, auto_Log log ) 
      : Server<HTTPRequest, HTTPResponse>( log ), http_request_target( http_request_target )
    { }

    auto_Stage get_http_request_reader_stage() const { return get_protocol_request_reader_stage(); }
    auto_Stage get_http_response_writer_stage() const { return get_protocol_response_writer_stage(); }

    // Peer
    void handleDeserializedProtocolMessage( auto_HTTPRequest http_request );

    // Server
    virtual auto_HTTPRequest createProtocolRequest( auto_Object<Connection> connection )
    {
      return new HTTPRequest( connection );
    }

  private:
    auto_EventTarget http_request_target;

    void set_http_request_reader_stage( auto_Stage http_request_reader_stage ) { set_protocol_request_reader_stage( http_request_reader_stage ); }
    void set_http_response_writer_stage( auto_Stage http_response_writer_stage ) { set_protocol_response_writer_stage( http_response_writer_stage ); }
  };

  typedef auto_Object<HTTPServer> auto_HTTPServer;


  class JSONValue;

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

  protected:
    JSONUnmarshaller( const char* root_key, JSONValue& root_json_value );

  private:
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
    auto_Struct get_body() const { return body; }
    uint32_t get_xid() const { return xid; }
    virtual ssize_t deserialize( auto_Buffer );
    virtual auto_Buffer serialize();
    void set_body( auto_Struct body ) { this->body = body; }

  protected:
    ONCRPCMessage();
    ONCRPCMessage( uint32_t xid, auto_Struct body );
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

    uint32_t xid;
    auto_Struct body;
  };


  class ONCRPCResponse : public ProtocolResponse<ONCRPCRequest>, public ONCRPCMessage<ONCRPCResponse>
  {
  public:
    ONCRPCResponse( auto_Object<ONCRPCRequest> oncrpc_request, auto_Struct body );
    ONCRPCResponse( uint32_t xid, auto_Struct body ); // For testing

    // Object
    YIELD_OBJECT_PROTOTYPES( ONCRPCResponse, 208 );
    virtual void marshal( Marshaller& );
    virtual void unmarshal( Unmarshaller& );

  protected:
    virtual ~ONCRPCResponse() { }

  private:
    template <class> friend class ONCRPCMessage;
  };

  typedef auto_Object<ONCRPCResponse> auto_ONCRPCResponse;


  class ONCRPCRequest : public ProtocolRequest, public ONCRPCMessage<ONCRPCRequest>
  {
  public:
    const static uint32_t AUTH_NONE = 0;


    ONCRPCRequest( auto_Object<Connection> connection, auto_Interface _interface ); // Incoming
    ONCRPCRequest( auto_Interface _interface, auto_Struct body ); // Outgoing
    ONCRPCRequest( auto_Interface _interface, uint32_t credential_auth_flavor, auto_Struct credential, auto_Struct body ); // Outgoing
    ONCRPCRequest( uint32_t prog, uint32_t proc, uint32_t vers, auto_Struct body ); // For testing
    ONCRPCRequest( uint32_t prog, uint32_t proc, uint32_t vers, uint32_t credential_auth_flavor, auto_Struct credential, auto_Struct body ); // For testing

    uint32_t get_credential_auth_flavor() const { return credential_auth_flavor; }
    auto_Struct get_credential() const { return credential; }
    auto_Interface get_interface() const { return _interface; }
    uint32_t get_prog() const { return prog; }
    uint32_t get_proc() const { return proc; }
    uint32_t get_vers() const { return vers; }

    // Object
    YIELD_OBJECT_PROTOTYPES( ONCRPCRequest, 213 );    
    virtual void marshal( Marshaller& );
    virtual void unmarshal( Unmarshaller& );

    // Request
    virtual void respond( Response& );

    // ProtocolRequest
    virtual auto_ONCRPCResponse createProtocolResponse() { return new ONCRPCResponse( incRef(), _interface->createResponse( get_body()->get_tag() ).release() ); }

  protected:
    virtual ~ONCRPCRequest() { }

  private:
    auto_Interface _interface;
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
                                                 auto_Object<StageGroup> stage_group, 
                                                 uint32_t flags = 0,
                                                 auto_Log log = NULL, 
                                                 const Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, 
                                                 uint8_t operation_retries_max = OPERATION_RETRIES_MAX_DEFAULT,
                                                 auto_SSLContext ssl_context = NULL )
    {
      auto_SocketAddress peername = SocketAddress::create( absolute_uri );
      if ( peername != NULL && peername->get_port() != 0 )
      {
        auto_Object<ONCRPCClientType> oncrpc_client = new ONCRPCClientType( absolute_uri, flags, log, operation_retries_max, operation_timeout, peername, ssl_context );
        auto_Stage oncrpc_client_stage = stage_group->createStage( oncrpc_client->incRef(), new FDEventQueue, 1 );
        return oncrpc_client;
      }
      else
        return NULL;
    }

    // EventTarget
    virtual void send( Event& ev ) 
    { 
      if ( InterfaceType::checkRequest( ev ) != NULL )
      {
        ONCRPCRequest* oncrpc_request = new ONCRPCRequest( this->incRef(), ev );
#ifdef _DEBUG
        if ( ( this->get_flags() & this->PEER_FLAG_TRACE_OPERATIONS ) == this->PEER_FLAG_TRACE_OPERATIONS && this->get_log() != NULL )
          this->get_log()->getStream( Log::LOG_INFO ) << "yield::ONCRPCClient: creating new ONCRPCRequest/" << reinterpret_cast<uint64_t>( oncrpc_request ) << " (xid=" << oncrpc_request->get_xid() << ") for interface request " << ev.get_type_name() << ".";
#endif

        Client<ONCRPCRequest, ONCRPCResponse>::send( *oncrpc_request );
      }
      else
      {
#ifdef _DEBUG
        if ( ( this->get_flags() & this->PEER_FLAG_TRACE_OPERATIONS ) == this->PEER_FLAG_TRACE_OPERATIONS && this->get_log() != NULL )
        {
          switch ( ev.get_tag() )
          {
            case YIELD_OBJECT_TAG( ONCRPCRequest ): this->get_log()->getStream( Log::LOG_INFO ) << "yield::ONCRPCClient: send()'ing ONCRPCRequest/" << reinterpret_cast<uint64_t>( &ev ) << " (xid=" << static_cast<ONCRPCRequest&>( ev ).get_xid() << ")."; break;
            case YIELD_OBJECT_TAG( ONCRPCResponse ): this->get_log()->getStream( Log::LOG_INFO ) << "yield::ONCRPCClient: send()'ing ONCRPCRequest/" << reinterpret_cast<uint64_t>( &ev ) << " (xid=" << static_cast<ONCRPCResponse&>( ev ).get_xid() << ")."; break;
          }
        }
#endif

        Client<ONCRPCRequest, ONCRPCResponse>::send( ev );
      }
    }

    // EventHandler
    // Have to override so Stage::StartupEvent doesn't go to InterfaceType
    virtual void handleEvent( Event& ev )
    {
      Client<ONCRPCRequest, ONCRPCResponse>::handleEvent( ev );
    }

  protected:
    ONCRPCClient( const URI& absolute_uri, uint32_t flags, auto_Log log, uint8_t operation_retries_max, const Time& operation_timeout, auto_SocketAddress peername, auto_SSLContext ssl_context )
      : Client<ONCRPCRequest, ONCRPCResponse>( absolute_uri, flags, log, operation_retries_max, operation_timeout, peername, ssl_context )
    { }

    virtual ~ONCRPCClient() { }
  };

  
  class ONCRPCServer : public Server<ONCRPCRequest, ONCRPCResponse>
  {
  public:
    static auto_Object<ONCRPCServer> create( const URI& absolute_uri,
                                             auto_Interface _interface,
                                             auto_Object<StageGroup> stage_group, 
                                             auto_Log log = NULL, 
                                             auto_SSLContext ssl_context = NULL );

    // Object
    YIELD_OBJECT_PROTOTYPES( ONCRPCServer, 0 );
   
  protected:
    ONCRPCServer( auto_Interface _interface, auto_Log log ) 
      : Server<ONCRPCRequest, ONCRPCResponse>( log ), _interface( _interface )
    { }

    auto_Stage get_oncrpc_request_reader_stage() const { return get_protocol_request_reader_stage(); }
    auto_Stage get_oncrpc_response_writer_stage() const { return get_protocol_response_writer_stage(); }

    // Peer
    void handleDeserializedProtocolMessage( auto_Object<ONCRPCRequest> oncrpc_request );

    // Server
    auto_Object<ONCRPCRequest> createProtocolRequest( auto_Object<Connection> connection )
    {
      return new ONCRPCRequest( connection, _interface );
    }

  private:
    auto_Interface _interface;

    void set_oncrpc_request_reader_stage( auto_Stage oncrpc_request_reader_stage ) { set_protocol_request_reader_stage( oncrpc_request_reader_stage ); }
    void set_oncrpc_response_writer_stage( auto_Stage oncrpc_response_writer_stage ) { set_protocol_response_writer_stage( oncrpc_response_writer_stage ); }
  };

  typedef auto_Object<ONCRPCServer> auto_ONCRPCServer;


  class Pipe : public Object
  {
  public:
    static auto_Object<Pipe> create();

    ssize_t read( void* buffer, size_t buffer_len );
    ssize_t write( const void* buffer, size_t buffer_len );

    // Object
    YIELD_OBJECT_PROTOTYPES( Pipe, 6 );

  private:
#ifdef _WIN32
    Pipe( void* ends[2] );
#else
    Pipe( int ends[2] );
#endif
    ~Pipe() { }

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
