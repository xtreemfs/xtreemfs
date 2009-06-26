// Copyright 2003-2008 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _YIELD_IPC_H
#define _YIELD_IPC_H

#include "yield/arch.h"


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
virtual bool aio_connect( YIELD::auto_Object<AIOConnectControlBlock> aio_connect_control_block ); \
virtual ssize_t aio_read( YIELD::auto_Object<AIOReadControlBlock> aio_read_control_block ); \
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
virtual bool want_read() const; \
virtual bool want_write() const; \
virtual ssize_t write( const void* buffer, size_t buffer_len ); \
virtual ssize_t writev( const struct iovec* buffers, uint32_t buffers_count );


namespace YIELD
{
  class Connection;
  class HTTPRequest;
  class ONCRPCRequest;
  class TCPSocket;
  class URI;


  class EventFDPipe : public YIELD::Object
  {
  public:
    static YIELD::auto_Object<EventFDPipe> create();

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
    EventFDPipe( YIELD::auto_Object<TCPSocket> read_end, YIELD::auto_Object<TCPSocket> write_end );
#endif
    ~EventFDPipe();

#ifdef YIELD_HAVE_LINUX_EVENTFD
    int fd;
#else
    YIELD::auto_Object<TCPSocket> read_end, write_end;
#endif
  };

  typedef YIELD::auto_Object<EventFDPipe> auto_EventFDPipe;


  class FDEventQueue : public TimerEventQueue
  {
  public:
    class POLLERREvent : public Event
    {
    public:
      POLLERREvent( YIELD::auto_Object<> context, int32_t errno_ )
        : context( context ), errno_( errno_ )
      { }

      inline YIELD::auto_Object<> get_context() const { return context; }
      inline int32_t get_errno() const { return errno_; }

      // Object
      YIELD_OBJECT_PROTOTYPES( POLLERREvent, 224 );

    private:
      YIELD::auto_Object<> context;        
      int32_t errno_;
    };


    class POLLINEvent : public Event
    {
    public:
      POLLINEvent( YIELD::auto_Object<> context )
        : context( context )
      { }

      inline YIELD::auto_Object<> get_context() const { return context; }

      // Object
      YIELD_OBJECT_PROTOTYPES( POLLINEvent, 225 );

    private:
      YIELD::auto_Object<> context;        
    };


    class POLLOUTEvent : public Event
    {
    public:
      POLLOUTEvent( YIELD::auto_Object<> context )
        : context( context )
      { }

      inline YIELD::auto_Object<> get_context() const { return context; }

      // Object
      YIELD_OBJECT_PROTOTYPES( POLLOUTEvent, 226 );

    private:
      YIELD::auto_Object<> context;        
    };


    FDEventQueue();

    bool attach( int fd, YIELD::auto_Object<> context = NULL, bool enable_read = true, bool enable_write = false );
    void detach( int fd );
    bool toggle( int fd, bool enable_read, bool enable_write );

    // Object
    YIELD_OBJECT_PROTOTYPES( FDEventQueue, 203 );

    // EventQueue
    virtual bool enqueue( Event& );
    virtual Event* dequeue( uint64_t timeout_ns );

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
    YIELD::auto_Object<EventFDPipe> eventfd_pipe;
    STLHashMap<Object*> fd_to_context_map;
  };


  class IOCompletionPort : public TimerEventQueue
  {
  public:
    IOCompletionPort();
    ~IOCompletionPort();

    bool attach( int fd );

    // Object
    YIELD_OBJECT_PROTOTYPES( IOCompletionPort, 228 );

    // EventQueue
    bool enqueue( Event& );
    virtual Event* dequeue( uint64_t timeout_ns );
    
  private:
#ifdef _WIN32
    void* hIoCompletionPort;
#endif
  };


  template <class ReadProtocolMessageType, class WriteProtocolMessageType>
  class Peer
  {
  public:
    // EventTarget
    virtual bool send( Event& ev );

  protected:
    Peer( auto_Log log );

    void attach( YIELD::auto_Object<Connection> connection, YIELD::auto_Object<> context, bool enable_read, bool enable_write );
    virtual ssize_t deserialize( YIELD::auto_Object<ReadProtocolMessageType> protocol_message, YIELD::auto_Buffer buffer );    
    void detach( YIELD::auto_Object<Connection> connection );
    auto_Stage get_helper_peer_stage() const { return helper_peer_stage != NULL ? helper_peer_stage : my_stage; }    
    auto_Log get_log() const { return log; }
    virtual void handleDeserializedProtocolMessage( YIELD::auto_Object<ReadProtocolMessageType> protocol_message ) = 0;
    virtual void handleEvent( Event& );
    inline bool haveAIO() const { return io_completion_port != NULL; }
    virtual bool read( YIELD::auto_Object<ReadProtocolMessageType> protocol_message, size_t buffer_capacity = 1024 );
    void set_helper_peer_stage( auto_Stage helper_peer_stage ) { this->helper_peer_stage = helper_peer_stage; }    
    virtual YIELD::auto_Buffer serialize( YIELD::auto_Object<WriteProtocolMessageType> protocol_message );
    virtual bool write( YIELD::auto_Object<WriteProtocolMessageType> protocol_message, YIELD::auto_Buffer buffer );

  private:
    auto_Log log;
    YIELD::auto_Object<FDEventQueue> fd_event_queue;
    YIELD::auto_Object<IOCompletionPort> io_completion_port;
    auto_Stage my_stage;
    auto_Stage helper_peer_stage;
  };


  class ProtocolRequest : public Request
  { 
  public:
    virtual ~ProtocolRequest() { }

    YIELD::auto_Object<Connection> get_connection() const { return connection; }
    void set_connection( YIELD::auto_Object<Connection> connection ) { this->connection = connection; }

  protected:
    ProtocolRequest() // Outgoing
    { }
    
    ProtocolRequest( YIELD::auto_Object<Connection> connection ) // Incoming
      : connection( connection )
    { }

    ProtocolRequest( const ProtocolRequest& )
    {
      DebugBreak(); // Prevent copying
    }

  private:
    YIELD::auto_Object<Connection> connection;
  };

  
  template <class ProtocolRequestType>
  class ProtocolResponse : public Response
  { 
  public:
    virtual ~ProtocolResponse() { }

    YIELD::auto_Object<Connection> get_connection() const { return protocol_request->get_connection(); }
    YIELD::auto_Object<ProtocolRequestType> get_protocol_request() const { return protocol_request; }

  protected:
    ProtocolResponse( YIELD::auto_Object<ProtocolRequestType> protocol_request )
      : protocol_request( protocol_request )
    { }

    ProtocolResponse( const ProtocolResponse<ProtocolRequestType>& )
    { 
      DebugBreak(); // Prevent copying
    } 
  
  private:
    YIELD::auto_Object<ProtocolRequestType> protocol_request;
  };


  class SocketAddress : public YIELD::Object
  {
  public:
    SocketAddress( struct addrinfo& addrinfo_list ); // Takes ownership of addrinfo_list
    SocketAddress( const struct sockaddr_storage& _sockaddr_storage ); // Copies _sockaddr_storage

    static YIELD::auto_Object<SocketAddress> create( const char* hostname ) { return create( hostname, 0 ); }
    static YIELD::auto_Object<SocketAddress> create( const char* hostname, uint16_t port ); // hostname can be NULL for INADDR_ANY
    static YIELD::auto_Object<SocketAddress> create( const URI& );

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

  typedef YIELD::auto_Object<SocketAddress> auto_SocketAddress;


  class Socket : public Event
  {
  public:
    class AIOConnectControlBlock : public AIOControlBlock<Event>
    {
    public:
      AIOConnectControlBlock( YIELD::auto_Object<> context, auto_SocketAddress peername, YIELD::auto_Buffer write_buffer )
        : context( context ), peername( peername ), write_buffer( write_buffer )
      { }

      YIELD::auto_Object<> get_context() const { return context; }
      auto_SocketAddress get_peername() const { return peername; }
      YIELD::auto_Buffer get_write_buffer() const { return write_buffer; }

      // Object
      YIELD_OBJECT_PROTOTYPES( AIOConnectControlBlock, 223 );

    private:
      YIELD::auto_Object<> context;
      auto_SocketAddress peername;
      YIELD::auto_Buffer write_buffer;
    };


    class AIOReadControlBlock : public AIOControlBlock<Event>
    {
    public:
      AIOReadControlBlock( YIELD::auto_Buffer buffer, YIELD::auto_Object<> context )
        : buffer( buffer ), context( context )
      { }

      YIELD::auto_Buffer get_buffer() const { return buffer; }
      YIELD::auto_Object<> get_context() const { return context; }

      // Object
      YIELD_OBJECT_PROTOTYPES( AIOReadControlBlock, 227 );

      // AIOControlBlock
      virtual void onCompletion( size_t bytes_transferred )
      { 
        AIOControlBlock<Event>::onCompletion( bytes_transferred );
        buffer->put( NULL, bytes_transferred ); 
      }

    private:
      YIELD::auto_Buffer buffer;
      YIELD::auto_Object<> context;
    };


    int get_domain() const { return domain; }
    int get_protocol() const { return protocol; }
    int get_type() const { return type; }
    bool operator==( const Socket& other ) const { return static_cast<int>( *this ) == static_cast<int>( other ); } \
    virtual ssize_t read( YIELD::auto_Buffer buffer );
    void set_read_buffer( YIELD::auto_Buffer buffer ) { this->read_buffer = read_buffer; }
    virtual ssize_t write( YIELD::auto_Buffer buffer );
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
    YIELD::auto_Buffer read_buffer;
    int _socket;

    bool blocking_mode;
  };

  typedef YIELD::auto_Object<Socket> auto_Socket;


  class TCPSocket : public Socket
  {
  public:
    class AIOAcceptControlBlock : public AIOControlBlock<Event>
    {
    public:
      AIOAcceptControlBlock( YIELD::auto_Object<TCPSocket> accepted_tcp_socket, YIELD::auto_Buffer read_buffer );

      YIELD::auto_Object<TCPSocket> get_accepted_tcp_socket() const { return accepted_tcp_socket; }
      YIELD::auto_Buffer get_read_buffer() const { return read_buffer; }

      // Object
      YIELD_OBJECT_PROTOTYPES( AIOAcceptControlBlock, 222 );

      // AIOControlBlock
      virtual void onCompletion( size_t bytes_transferred );

    private:
      ~AIOAcceptControlBlock() { }

      YIELD::auto_Object<TCPSocket> accepted_tcp_socket;
      YIELD::auto_Buffer read_buffer;
    };


    static YIELD::auto_Object<TCPSocket> create(); // Defaults to domain = AF_INET6
    virtual YIELD::auto_Object<TCPSocket> accept();
#ifdef _WIN32
    virtual bool aio_accept( YIELD::auto_Object<AIOAcceptControlBlock> aio_accept_control_block );
    virtual bool aio_connect( YIELD::auto_Object<AIOConnectControlBlock> aio_connect_control_block );
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

  typedef YIELD::auto_Object<TCPSocket> auto_TCPSocket;


  class TCPListenQueue : public FDEventQueue
  {
  public:
    static YIELD::auto_Object<TCPListenQueue> create( auto_SocketAddress sockname );
 
    // EventQueue
    Event* dequeue() { return dequeue( static_cast<uint64_t>( -1 ) ); }
    Event* dequeue( uint64_t timeout_ns );

  protected:
    TCPListenQueue( YIELD::auto_Object<TCPSocket> listen_tcp_socket );
    ~TCPListenQueue() { }

  private:
    YIELD::auto_Object<TCPSocket> listen_tcp_socket;
  };

  typedef YIELD::auto_Object<TCPListenQueue> auto_TCPListenQueue;


  class SSLContext : public YIELD::Object
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

  typedef YIELD::auto_Object<SSLContext> auto_SSLContext;


#ifdef YIELD_HAVE_OPENSSL

  class SSLSocket : public TCPSocket
  {
  public:
    static YIELD::auto_Object<SSLSocket> create( auto_SSLContext ctx );

    // Object
    YIELD_OBJECT_PROTOTYPES( SSLSocket, 216 );

    // Socket
    virtual ssize_t read( void* buffer, size_t buffer_len );
    virtual ssize_t write( const void* buffer, size_t buffer_len );
    virtual ssize_t writev( const struct iovec* buffers, uint32_t buffers_count );
    virtual bool want_read() const;
    virtual bool want_write() const;

    // TCPSocket
    YIELD::auto_Object<TCPSocket> accept();
    bool connect( auto_SocketAddress peername );
    bool shutdown();

  private:
    SSLSocket( int domain, int _socket, auto_SSLContext ctx, SSL* ssl );
    ~SSLSocket();

    auto_SSLContext ctx;
    SSL* ssl;
  };

  typedef YIELD::auto_Object<SSLSocket> auto_SSLSocket;


  class SSLListenQueue : public TCPListenQueue
  {
  public:
    static YIELD::auto_Object<SSLListenQueue> create( auto_SocketAddress sockname, auto_SSLContext ssl_context );
 
  private:
    SSLListenQueue( YIELD::auto_Object<SSLSocket> listen_ssl_socket );
    ~SSLListenQueue() { }
  };

  typedef YIELD::auto_Object<SSLListenQueue> auto_SSLListenQueue;

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
    static YIELD::auto_Object<UDPSocket> create();

    // Object
    YIELD_OBJECT_PROTOTYPES( UDPSocket, 219 );

  private:
    UDPSocket( int domain, int _socket );    
    ~UDPSocket() { }
  };

  typedef YIELD::auto_Object<UDPSocket> auto_UDPSocket;


  class UDPRecvFromQueue : public FDEventQueue
  {
  public:
    static YIELD::auto_Object<UDPRecvFromQueue> create( auto_SocketAddress sockname );
 
    // Object
    YIELD_OBJECT_PROTOTYPES( UDPRecvFromQueue, 0 );

    // EventQueue
    Event* dequeue( uint64_t timeout_ns );

  private:
    UDPRecvFromQueue( auto_SocketAddress recvfrom_sockname, YIELD::auto_Object<UDPSocket> recvfrom_udp_socket );
    ~UDPRecvFromQueue() { }

    auto_SocketAddress recvfrom_sockname;
    YIELD::auto_Object<UDPSocket> recvfrom_udp_socket;
  };

  typedef YIELD::auto_Object<UDPRecvFromQueue> auto_UDPRecvFromQueue;


  template <class ProtocolRequestType, class ProtocolResponseType>
  class Client : public Peer<ProtocolResponseType, ProtocolRequestType>
  {
  public:    
    const static uint64_t OPERATION_TIMEOUT_DEFAULT = 30 * NS_IN_S;
    const static uint8_t OPERATION_RETRIES_MAX_DEFAULT = UINT8_MAX;    

  protected:
    Client( const URI& absolute_uri, auto_Log log, uint8_t operation_retries_max, const Time& operation_timeout, auto_SocketAddress peername, auto_SSLContext ssl_context );
    virtual ~Client();

    auto_Stage get_protocol_request_writer_stage() const { return this->get_helper_peer_stage(); }
    auto_Stage get_protocol_response_reader_stage() const { return this->get_helper_peer_stage(); }
    void set_protocol_request_writer_stage( auto_Stage protocol_request_writer_stage ) { this->set_helper_peer_stage( protocol_request_writer_stage ); }
    void set_protocol_response_reader_stage( auto_Stage protocol_response_reader_stage ) { this->set_helper_peer_stage( protocol_response_reader_stage ); }

    // Peer
    virtual ssize_t deserialize( YIELD::auto_Object<ProtocolResponseType> protocol_response, YIELD::auto_Buffer buffer );    
    virtual void handleEvent( Event& );
    virtual void handleDeserializedProtocolMessage( YIELD::auto_Object<ProtocolResponseType> protocol_response );
    virtual bool read( YIELD::auto_Object<ProtocolResponseType> protocol_response, size_t buffer_capacity );
    virtual bool write( YIELD::auto_Object<ProtocolRequestType> protocol_request, YIELD::auto_Buffer buffer );

  private:
    YIELD::auto_Object<URI> absolute_uri;
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
    bool send( Event& ev ) { return Peer<ProtocolRequestType, ProtocolResponseType>::send( ev ); }

  protected:
    Server( auto_Log log ) 
      : Peer<ProtocolRequestType, ProtocolResponseType>( log )
    { }

    virtual YIELD::auto_Object<ProtocolRequestType> createProtocolRequest( YIELD::auto_Object<Connection> connection ) = 0;
    auto_Stage get_protocol_request_reader_stage() const { return this->get_helper_peer_stage(); }
    auto_Stage get_protocol_response_writer_stage() const { return this->get_helper_peer_stage(); }
    void set_protocol_request_reader_stage( auto_Stage protocol_request_reader_stage ) { this->set_helper_peer_stage( protocol_request_reader_stage ); }
    void set_protocol_response_writer_stage( auto_Stage protocol_response_writer_stage ) { this->set_helper_peer_stage( protocol_response_writer_stage ); }

    // Peer
    virtual bool write( YIELD::auto_Object<ProtocolResponseType> protocol_response, YIELD::auto_Buffer buffer );
  };


  class RFC822Headers
  {
  public:
    RFC822Headers();
    virtual ~RFC822Headers();

    ssize_t deserialize( YIELD::auto_Buffer );
    char* get_header( const char* header_name, const char* default_value="" );
    char* operator[]( const char* header_name ) { return get_header( header_name ); }
    // void set_header( const char* header, size_t header_len ); // Mutable header with name: value in one string, will copy both
    void set_header( const char* header_name, const char* header_value ); // Literal header
    void set_header( const char* header_name, char* header_value ); // Mutable header, will copy value
    void set_header( char* header_name, char* header_value ); // Mutable name and mutable value, will copy both
    void set_header( const std::string& header_name, const std::string& header_value ); // Mutable name and mutable value, will copy both
    YIELD::auto_Buffer serialize();

  protected:
    void copy_iovec( const char* data, size_t len );
    void set_iovec( const char* data, size_t len ) { struct iovec _iovec; _iovec.iov_base = const_cast<char*>( data ); _iovec.iov_len = len; set_iovec( _iovec ); }
    void set_iovec( const struct iovec& out_iovec );

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


  class HTTPMessage : public RFC822Headers
  {
  public:
    YIELD::auto_Buffer get_body() const { return body; }
    uint8_t get_http_version() const { return http_version; }

  protected:
    HTTPMessage();
    HTTPMessage( YIELD::auto_Buffer body );
    virtual ~HTTPMessage() { }


    YIELD::auto_Buffer body;

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

    virtual ssize_t deserialize( YIELD::auto_Buffer );
    virtual YIELD::auto_Buffer serialize();
  };


  class HTTPResponse : public ProtocolResponse<HTTPRequest>, public HTTPMessage
  {
  public:
    HTTPResponse( YIELD::auto_Object<HTTPRequest> http_request ); // Incoming
    HTTPResponse( YIELD::auto_Object<HTTPRequest> http_request, uint16_t status_code ); // Outgoing
    HTTPResponse( YIELD::auto_Object<HTTPRequest> http_request, uint16_t status_code, YIELD::auto_Buffer body ); // Outgoing

    ssize_t deserialize( YIELD::auto_Buffer );
    uint16_t get_status_code() const { return status_code; }
    YIELD::auto_Buffer serialize();
    void set_body( YIELD::auto_Buffer body ) { this->body = body; }
    void set_status_code( uint16_t status_code ) { this->status_code = status_code; }

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPResponse, 206 );

  protected:
    virtual ~HTTPResponse() { }

  private:
    HTTPResponse( const HTTPResponse& http_response )
      : ProtocolResponse<HTTPRequest>( http_response )
    { }

    uint8_t http_version;
    union { char status_code_str[4]; uint16_t status_code; };
  };

  typedef YIELD::auto_Object<HTTPResponse> auto_HTTPResponse;


  class HTTPRequest : public ProtocolRequest, public HTTPMessage
  {
  public:
    HTTPRequest( YIELD::auto_Object<Connection> connection ); // Incoming
    HTTPRequest( const char* method, const char* relative_uri, const char* host, YIELD::auto_Buffer body = NULL ); // Outgoing
    HTTPRequest( const char* method, const URI& absolute_uri, YIELD::auto_Buffer body = NULL ); // Outgoing

    ssize_t deserialize( YIELD::auto_Buffer );
    uint8_t get_http_version() const { return http_version; }
    const char* get_method() const { return method; }
    const char* get_uri() const { return uri; }    
    virtual bool respond( uint16_t status_code );
    virtual bool respond( uint16_t status_code, YIELD::auto_Buffer body );
    virtual bool respond( Response& response ) { return Request::respond( response ); }
    YIELD::auto_Buffer serialize();

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPRequest, 205 );

    // ProtocolRequest
    auto_HTTPResponse createProtocolResponse() { return new HTTPResponse( incRef() ); }

  protected:
    virtual ~HTTPRequest();

  private:
    HTTPRequest( const HTTPRequest& http_request )
      : ProtocolRequest( http_request )
    { }

    void init( const char* method, const char* relative_uri, const char* host, YIELD::auto_Buffer body );

    char method[16];
    char* uri; size_t uri_len;
  };

  typedef YIELD::auto_Object<HTTPRequest> auto_HTTPRequest;


  class HTTPClient : public EventHandler, public Client<HTTPRequest, HTTPResponse>
  {
  public:
    static YIELD::auto_Object<HTTPClient> create( const URI& absolute_uri, 
                                           YIELD::auto_Object<StageGroup> stage_group, 
                                           auto_Log log = NULL, 
                                           uint8_t operation_retries_max = OPERATION_RETRIES_MAX_DEFAULT,
                                           const Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, 
                                           auto_SSLContext ssl_context = NULL );

    static auto_HTTPResponse GET( const URI& absolute_uri, auto_Log log = NULL );
    static auto_HTTPResponse PUT( const URI& absolute_uri, YIELD::auto_Buffer body, auto_Log log = NULL );
    static auto_HTTPResponse PUT( const URI& absolute_uri, const Path& body_file_path, auto_Log log = NULL );

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPClient, 207 );

    // EventHandler
    virtual void handleEvent( Event& ev ) { Client<HTTPRequest, HTTPResponse>::handleEvent( ev ); }

    // EventTarget
    virtual bool send( Event& ev ) { return Client<HTTPRequest, HTTPResponse>::send( ev ); }

  private:
    HTTPClient( const URI& absolute_uri, auto_Log log, uint8_t operation_retries_max, const Time& operation_timeout, auto_SocketAddress peername, auto_SSLContext ssl_context )
      : Client<HTTPRequest, HTTPResponse>( absolute_uri, log, operation_retries_max, operation_timeout, peername, ssl_context )
    { }

    virtual ~HTTPClient() { }

    static auto_HTTPResponse sendHTTPRequest( const char* method, const YIELD::URI& uri, YIELD::auto_Buffer body, auto_Log log );

    void set_http_request_writer_stage( auto_Stage http_request_writer_stage ) { set_protocol_request_writer_stage( http_request_writer_stage ); }
    void set_http_response_reader_stage( auto_Stage http_response_reader_stage ) { set_protocol_response_reader_stage( http_response_reader_stage ); }
  };

  typedef YIELD::auto_Object<HTTPClient> auto_HTTPClient;
    

  class HTTPServer : public Server<HTTPRequest, HTTPResponse>
  {
  public:
    template <class StageGroupType> 
    static YIELD::auto_Object<HTTPServer> create( const URI& absolute_uri,
                                           auto_EventTarget http_request_target, 
                                           YIELD::auto_Object<StageGroupType> stage_group,                        
                                           auto_Log log = NULL, 
                                           auto_SSLContext ssl_context = NULL );

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
    virtual auto_HTTPRequest createProtocolRequest( YIELD::auto_Object<Connection> connection )
    {
      return new HTTPRequest( connection );
    }

  private:
    auto_EventTarget http_request_target;

    void set_http_request_reader_stage( auto_Stage http_request_reader_stage ) { set_protocol_request_reader_stage( http_request_reader_stage ); }
    void set_http_response_writer_stage( auto_Stage http_response_writer_stage ) { set_protocol_response_writer_stage( http_response_writer_stage ); }
  };

  typedef YIELD::auto_Object<HTTPServer> auto_HTTPServer;


  class JSONValue;

  class JSONMarshaller : public YIELD::BufferedMarshaller
  {
  public:
    JSONMarshaller( bool write_empty_strings = true );
    virtual ~JSONMarshaller(); // If the stream is wrapped in map, sequence, etc. then the constructor will append the final } or [, so the underlying output stream should not be deleted before this object!

    // Marshaller
    YIDL_MARSHALLER_PROTOTYPES;

  protected:
    JSONMarshaller( JSONMarshaller& parent_json_marshaller, const YIELD::Declaration& root_decl );

    virtual void writeDeclaration( const YIELD::Declaration& );
    virtual void writeMap( const YIELD::Map* ); // Can be NULL for empty maps
    virtual void writeSequence( const YIELD::Sequence* ); // Can be NULL for empty sequences
    virtual void writeStruct( const YIELD::Struct* ); // Can be NULL for empty maps

  private:
    bool in_map;
    const YIELD::Declaration* root_decl; // Mostly for debugging, also used to indicate if this is the root JSONMarshaller
    bool write_empty_strings;
    yajl_gen writer;

    void flushYAJLBuffer();
  };


  class JSONUnmarshaller : public YIELD::Unmarshaller
  {
  public:
    JSONUnmarshaller( YIELD::auto_Buffer source_buffer );
    virtual ~JSONUnmarshaller();

    // Unmarshaller
    YIDL_UNMARSHALLER_PROTOTYPES;

  protected:
    JSONUnmarshaller( const YIELD::Declaration& root_decl, JSONValue& root_json_value );

  private:
    const YIELD::Declaration* root_decl;
    JSONValue *root_json_value, *next_json_value;

    void readMap( YIELD::Map& );
    void readSequence( YIELD::Sequence& );
    void readStruct( YIELD::Struct& );
    JSONValue* readJSONValue( const YIELD::Declaration& decl );
  };


  class NamedPipe : public YIELD::Object
  {
  public:
    static YIELD::auto_Object<NamedPipe> open( const Path& path, uint32_t flags = O_RDWR, mode_t mode = File::DEFAULT_MODE );            

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

  typedef YIELD::auto_Object<NamedPipe> auto_NamedPipe;


  template <class ONCRPCMessageType> // CRTP
  class ONCRPCMessage
  {
  public:
    YIELD::auto_Object<> get_body() const { return body; }
    uint32_t get_xid() const { return xid; }

    ssize_t deserialize( YIELD::auto_Buffer );
    YIELD::auto_Buffer serialize();

  protected:
    ONCRPCMessage();
    ONCRPCMessage( uint32_t xid, YIELD::auto_Object<> body );
    virtual ~ONCRPCMessage();

    uint32_t xid;
    YIELD::auto_Object<> body;

  private:
    enum 
    { 
      DESERIALIZING_RECORD_FRAGMENT_MARKER, 
      DESERIALIZING_RECORD_FRAGMENT, 
      DESERIALIZING_LONG_RECORD_FRAGMENT, 
      DESERIALIZE_DONE 
    } deserialize_state;

    YIELD::auto_Buffer first_record_fragment_buffer, current_record_fragment_buffer;
    size_t expected_record_fragment_length, received_record_fragment_length;
  };


  class ONCRPCResponse : public ProtocolResponse<ONCRPCRequest>, public ONCRPCMessage<ONCRPCResponse>
  {
  public:
    ONCRPCResponse( YIELD::auto_Object<ONCRPCRequest> oncrpc_request, YIELD::auto_Object<> body );
    ONCRPCResponse( uint32_t xid, YIELD::auto_Object<> body ); // For testing

    // Object
    YIELD_OBJECT_PROTOTYPES( ONCRPCResponse, 208 );
    void marshal( YIELD::Marshaller& );
    void unmarshal( YIELD::Unmarshaller& );

  private:
    template <class> friend class ONCRPCMessage;

    ~ONCRPCResponse() { }
  };

  typedef YIELD::auto_Object<ONCRPCResponse> auto_ONCRPCResponse;


  class ONCRPCRequest : public ProtocolRequest, public ONCRPCMessage<ONCRPCRequest>
  {
  public:
    const static uint32_t AUTH_NONE = 0;


    ONCRPCRequest( YIELD::auto_Object<Connection> connection, YIELD::auto_Object<Interface> _interface ); // Incoming
    ONCRPCRequest( YIELD::auto_Object<Interface> _interface, uint32_t prog, uint32_t proc, uint32_t vers, YIELD::auto_Object<> body ); // Outgoing
    ONCRPCRequest( YIELD::auto_Object<Interface> _interface, uint32_t prog, uint32_t proc, uint32_t vers, uint32_t credential_auth_flavor, YIELD::auto_Object<> credential, YIELD::auto_Object<> body ); // Outgoing

    uint32_t get_credential_auth_flavor() const { return credential_auth_flavor; }
    YIELD::auto_Object<> get_credential() const { return credential; }
    YIELD::auto_Object<Interface> get_interface() const { return _interface; }
    uint32_t get_prog() const { return prog; }
    uint32_t get_proc() const { return proc; }
    uint32_t get_vers() const { return vers; }

    // Object
    YIELD_OBJECT_PROTOTYPES( ONCRPCRequest, 213 );    
    void marshal( YIELD::Marshaller& );
    void unmarshal( YIELD::Unmarshaller& );

    // Request
    virtual bool respond( Response& response );

    // ProtocolRequest
    YIELD::auto_Object<ONCRPCResponse> createProtocolResponse() { return new ONCRPCResponse( incRef(), _interface->createResponse( get_body()->get_tag() ).release() ); }

  private:
    ~ONCRPCRequest() { }

    YIELD::auto_Object<Interface> _interface;
    uint32_t prog, proc, vers, credential_auth_flavor;
    YIELD::auto_Object<> credential;
  };

  typedef YIELD::auto_Object<ONCRPCRequest> auto_ONCRPCRequest;


  template <class InterfaceType>
  class ONCRPCClient : public InterfaceType, public Client<ONCRPCRequest, ONCRPCResponse>
  {
  public:
    template <class ONCRPCClientType>
    static YIELD::auto_Object<ONCRPCClientType> create( const URI& absolute_uri, 
                                                 YIELD::auto_Object<StageGroup> stage_group, 
                                                 auto_Log log = NULL, 
                                                 const Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, 
                                                 uint8_t operation_retries_max = OPERATION_RETRIES_MAX_DEFAULT,
                                                 auto_SSLContext ssl_context = NULL )
    {
      auto_SocketAddress peername = SocketAddress::create( absolute_uri );
      if ( peername != NULL && peername->get_port() != 0 )
      {
        YIELD::auto_Object<ONCRPCClientType> oncrpc_client = new ONCRPCClientType( absolute_uri, log, operation_retries_max, operation_timeout, peername, ssl_context );
        auto_Stage oncrpc_client_stage = stage_group->createStage( oncrpc_client->incRef(), 1, new FDEventQueue, NULL, log );
        return oncrpc_client;
      }
      else
        return NULL;
    }

    // EventTarget
    virtual bool send( Event& ev ) 
    { 
      if ( InterfaceType::checkRequest( ev ) != NULL )
        return Client<ONCRPCRequest, ONCRPCResponse>::send( *( new ONCRPCRequest( this->incRef(), 0x20000000 + InterfaceType::get_tag(), ev.get_tag(), InterfaceType::get_tag(), ev ) ) );
      else
        return Client<ONCRPCRequest, ONCRPCResponse>::send( ev );
    }

    // EventHandler
    // Have to override so StageStartupEvent doesn't go to InterfaceType
    virtual void handleEvent( Event& ev )
    {
      Client<ONCRPCRequest, ONCRPCResponse>::handleEvent( ev );
    }

  protected:
    ONCRPCClient( const URI& absolute_uri, auto_Log log, uint8_t operation_retries_max, const Time& operation_timeout, auto_SocketAddress peername, auto_SSLContext ssl_context )
      : Client<ONCRPCRequest, ONCRPCResponse>( absolute_uri, log, operation_retries_max, operation_timeout, peername, ssl_context )
    { }

    virtual ~ONCRPCClient() { }
  };

  
  class ONCRPCServer : public Server<ONCRPCRequest, ONCRPCResponse>
  {
  public:
    static YIELD::auto_Object<ONCRPCServer> create( const URI& absolute_uri,
                                             YIELD::auto_Object<Interface> _interface,
                                             YIELD::auto_Object<StageGroup> stage_group, 
                                             auto_Log log = NULL, 
                                             auto_SSLContext ssl_context = NULL );

    // Object
    YIELD_OBJECT_PROTOTYPES( ONCRPCServer, 0 );
   
  protected:
    ONCRPCServer( YIELD::auto_Object<Interface> _interface, auto_Log log ) 
      : Server<ONCRPCRequest, ONCRPCResponse>( log ), _interface( _interface )
    { }

    auto_Stage get_oncrpc_request_reader_stage() const { return get_protocol_request_reader_stage(); }
    auto_Stage get_oncrpc_response_writer_stage() const { return get_protocol_response_writer_stage(); }

    // Peer
    void handleDeserializedProtocolMessage( YIELD::auto_Object<ONCRPCRequest> oncrpc_request );

    // Server
    YIELD::auto_Object<ONCRPCRequest> createProtocolRequest( YIELD::auto_Object<Connection> connection )
    {
      return new ONCRPCRequest( connection, _interface );
    }

  private:
    YIELD::auto_Object<Interface> _interface;

    void set_oncrpc_request_reader_stage( auto_Stage oncrpc_request_reader_stage ) { set_protocol_request_reader_stage( oncrpc_request_reader_stage ); }
    void set_oncrpc_response_writer_stage( auto_Stage oncrpc_response_writer_stage ) { set_protocol_response_writer_stage( oncrpc_response_writer_stage ); }
  };

  typedef YIELD::auto_Object<ONCRPCServer> auto_ONCRPCServer;


  class Pipe : public YIELD::Object
  {
  public:
    static YIELD::auto_Object<Pipe> create();

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

  typedef YIELD::auto_Object<Pipe> auto_Pipe;


  class Process : public YIELD::Object
  {
  public:
    static YIELD::auto_Object<Process> create( const Path& executable_file_path ); // No arguments
    static YIELD::auto_Object<Process> create( int argc, char** argv );    
    static YIELD::auto_Object<Process> create( const Path& executable_file_path, const char** null_terminated_argv ); // execv style

    YIELD::auto_Object<Pipe> get_stdin() const { return child_stdin; }
    YIELD::auto_Object<Pipe> get_stdout() const { return child_stdout; }
    YIELD::auto_Object<Pipe> get_stderr() const { return child_stderr; }

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
    Process( int child_pid, 
#endif
      YIELD::auto_Object<Pipe> child_stdin, YIELD::auto_Object<Pipe> child_stdout, YIELD::auto_Object<Pipe> child_stderr );

    ~Process();

#ifdef _WIN32
    void *hChildProcess, *hChildThread;
#else
    int child_pid;
#endif
    YIELD::auto_Object<Pipe> child_stdin, child_stdout, child_stderr;  
  };

  typedef YIELD::auto_Object<Process> auto_Process;


  class URI : public YIELD::Object
  {
  public:
    // Factory methods return NULL instead of throwing exceptions
    static YIELD::auto_Object<URI> parse( const char* uri ) { return parse( uri, strnlen( uri, UINT16_MAX ) ); }
    static YIELD::auto_Object<URI> parse( const std::string& uri ) { return parse( uri.c_str(), uri.size() ); }
    static YIELD::auto_Object<URI> parse( const char* uri, size_t uri_len );

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

  typedef YIELD::auto_Object<URI> auto_URI;
};

#endif
