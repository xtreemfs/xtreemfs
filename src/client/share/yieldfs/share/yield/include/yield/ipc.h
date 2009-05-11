// Copyright 2003-2008 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef YIELD_IPC_H
#define YIELD_IPC_H

#include "yield/arch.h"


#if defined(__FreeBSD__) || defined(__OpenBSD__) || defined(__MACH__)
#define YIELD_HAVE_FREEBSD_KQUEUE 1
struct kevent;
#elif defined(__linux__)
#define YIELD_HAVE_LINUX_EPOLL 1
struct epoll_event;
// epoll_event's context (.data) member is a union of fd, u32, u64, and a Object*, which means
// that (unlike with kqueue) you can either get the context or the fd back, but not both
// In order to keep both I also a hash_map for fd->context. This may become a performance issue.
// If so, we'll have to find another way to satisfy callers who need the fd and/or the context.
#define YIELD_LINUX_EPOLL_RETURN_FD 1
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
#include <openssl/err.h>
#include <openssl/pem.h>
#include <openssl/pkcs12.h>
#include <openssl/rsa.h>
#include <openssl/ssl.h>
#include <openssl/x509.h>
#ifdef _WIN32
#pragma comment( lib, "libeay32.lib" )
#pragma comment( lib, "ssleay32.lib" )
#endif
#endif

#ifdef YIELD_HAVE_ZLIB
#ifdef _WIN32
#pragma comment( lib, "zlib.lib" )
#undef ZLIB_WINAPI // So zlib doesn't #include windows.h
#endif
#include "zlib.h"
#endif

struct in6_addr;
struct sockaddr_storage;
struct sockaddr_in6;
struct UriUriStructA;
struct yajl_gen_t;
typedef struct yajl_gen_t* yajl_gen;


#define YIELD_IPC_RFC822_HEADERS_STACK_BUFFER_LENGTH 128
#define YIELD_IPC_RFC822_HEADERS_STACK_IOVECS_LENGTH 32


namespace YIELD
{
  class FDAndInternalEventQueue;
  class ONCRPCRecordInputStream;
#ifdef _WIN32
  typedef unsigned int socket_t;
#else
  typedef int socket_t;
#endif
  class URI;


  class SocketAddress : public Object
  {
  public:
    SocketAddress();
    SocketAddress( uint16_t port, bool loopback = true ); // port is in host byte order, loopback = false => INADDR_ANY
    SocketAddress( const char* hostname );
    SocketAddress( const char* hostname, uint16_t port );
    SocketAddress( const URI& ); // URI ports should obviously be in host byte order
    SocketAddress( const struct sockaddr_storage& );
    SocketAddress( const SocketAddress& );
    ~SocketAddress();

    bool getnameinfo( std::string& out_hostname, bool numeric = true ) const;
    bool getnameinfo( char* out_hostname, uint32_t out_hostname_len, bool numeric = true ) const;
    uint16_t get_port() const;
    operator struct sockaddr_storage() const;
    bool operator==( const SocketAddress& other ) const;
    bool operator!=( const SocketAddress& other ) const;

  private:
    struct sockaddr_storage* _sockaddr_storage;

    void init( const char* hostname, uint16_t port );
    void init( const struct in6_addr&, uint16_t port );
    void init( const struct sockaddr_storage& );
  };

  static inline std::ostream& operator<<( std::ostream& os, const SocketAddress& sockaddr )
  {
    char nameinfo[1025];
    if ( sockaddr.getnameinfo( nameinfo, 1025, true ) )
      os << "[" << nameinfo << "]";
    else
      os << "[could not resolve socket address]";
    os << ":" << sockaddr.get_port();
    return os;
  }


  class Socket : public Event, public InputStream, public OutputStream
  {
  public:
    Socket( int domain, int type, int protocol, auto_Object<Log> log = NULL );
    Socket( socket_t _socket, auto_Object<Log> log = NULL );

    bool bind( const SocketAddress& bind_to_sockaddr );
    virtual Stream::Status connect( const SocketAddress& connect_to_sockaddr );
    virtual bool close();
    bool get_blocking_mode() const { return blocking_mode; }
    auto_Object<Log> get_log() const { return log; }
    auto_Object<SocketAddress> getpeername() const;
    auto_Object<SocketAddress> getsockname() const;
    operator socket_t() const { return _socket; }
    bool operator==( socket_t _socket ) const { return this->_socket == _socket; }
    std::ostream& operator<<( std::ostream& os ) const { os << "socket #" << static_cast<int>( _socket ); return os; }
    OutputStream& operator<<( OutputStream& output_stream ) const { output_stream.write( "socket" ); return output_stream; }
    bool set_blocking_mode( bool blocking );
    virtual bool shutdown() { return true; }

    // InputStream
    virtual Stream::Status read( void* buffer, size_t buffer_len, size_t* out_bytes_read = 0 );

    // OutputStream
    virtual Stream::Status writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written = 0 );

  protected:
    virtual ~Socket()
    {
      close();
    }

    auto_Object<Log> log;

  private:
    Socket( const Socket& other )
    {
      DebugBreak();
    } // Prevent copying

    int domain, type, protocol;
    socket_t _socket;

    bool blocking_mode;

    static bool _close( socket_t );
  };


  class SocketFactory : public Object
  {
  public:
    virtual auto_Object<Socket> createSocket( auto_Object<Log> log = NULL ) = 0;

  protected:
    SocketFactory( auto_Object<Log> log = NULL )
      : log( log )
    { }

    virtual ~SocketFactory() { }

    auto_Object<Log> log;
  };


  class TCPSocket : public Socket
  {
  public:
    TCPSocket( auto_Object<Log> log = NULL );

    auto_Object<TCPSocket> accept();
    virtual bool listen();
    virtual bool shutdown();

    // Object
    YIELD_OBJECT_TYPE_INFO( EVENT, "TCPSocket", 2622352664UL );
    inline TCPSocket& incRef() { return Object::incRef( *this ); }

    // OutputStream
    virtual Stream::Status writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written = 0 );

  protected:
    TCPSocket( socket_t, auto_Object<Log> log );
    virtual ~TCPSocket() { }

    socket_t _accept();

  private:
    size_t partial_write_len;
  };


  class TCPSocketFactory : public SocketFactory
  {
  public:
    TCPSocketFactory( auto_Object<Log> log = NULL )
      : SocketFactory( log )
    { }

    // SocketFactory
    auto_Object<Socket> createSocket( auto_Object<Log> log = NULL )
    {
      return new TCPSocket( log != NULL ? log : this->log );
    }

  private:
    ~TCPSocketFactory() { }
  };


  class FDEvent : public Event
  {
  public:
    inline Object* get_context() const { return context; }
    inline uint32_t get_error_code() const { return error_code; }
    fd_t get_fd() const { return fd; }
    inline socket_t get_socket() const { return _socket; }
    inline bool want_read() const { return _want_read; }

    // Object
    YIELD_OBJECT_TYPE_INFO( EVENT, "FDEvent", 3294357755UL )
    void serialize( StructuredOutputStream& );

  private:
    friend class FDEventQueue;

    union
    {
      fd_t fd;
      socket_t _socket;
    };

    Object* context;
    bool _want_read;
    uint32_t error_code;
  };


  class FDEventQueue : public EventQueue
  {
  public:
    FDEventQueue();

#ifdef _WIN32
    bool attach( socket_t fd, Object* context, bool enable_read = true, bool enable_write = false );
    void detach( socket_t fd, Object* context, bool will_keep_fd_open = true );
    bool toggle( socket_t fd, Object* context, bool enable_read, bool enable_write );
#else
    bool attach( fd_t fd, Object* context,  bool enable_read = true, bool enable_write = false );
    void detach( fd_t fd, Object* context, bool will_keep_fd_open = true );
    bool toggle( fd_t fd, Object* context, bool enable_read, bool enable_write );
#endif

    virtual void break_blocking_dequeue();

    // EventQueue
    virtual EventQueue* clone() const { return new FDEventQueue; }
    virtual bool enqueue( Event& ); // Discards events
    virtual Event* dequeue();
    virtual Event* try_dequeue() { return FDEventQueue::timed_dequeue( 0 ); }
    virtual Event* timed_dequeue( timeout_ns_t timeout_ns );

  protected:
    virtual ~FDEventQueue();

  private:
#ifdef YIELD_HAVE_LINUX_EVENTFD
    int signal_eventfd;
#else
    auto_Object<TCPSocket> signal_read_socket, signal_write_socket;
#endif
    int active_fds;

#if defined(YIELD_HAVE_LINUX_EPOLL) || defined(YIELD_HAVE_FREEBSD_KQUEUE) || defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
    fd_t poll_fd;
#endif

#if defined(YIELD_HAVE_LINUX_EPOLL)
    struct epoll_event* returned_events;
#ifdef YIELD_LINUX_EPOLL_RETURN_FD
    STLHashMap<Object*> fd_to_context_map;
#endif
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
    struct kevent* returned_events;
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
    port_event_t* returned_events;
#else
    STLHashMap<Object*> fd_to_context_map;
#ifdef _WIN32
    fd_set *read_fds, *read_fds_copy, *write_fds, *write_fds_copy, *except_fds, *except_fds_copy;
    STLHashMap<Object*>::iterator next_fd_to_check;
#else
    typedef std::vector<pollfd> pollfd_vector;
    pollfd_vector::size_type next_pollfd_to_check;
    pollfd_vector pollfds;
#endif
#endif

    FDEvent stack_fd_event;
    void fillStackFDEvent();

#if defined(YIELD_HAVE_LINUX_EPOLL) || defined(YIELD_HAVE_FREEBSD_KQUEUE) || defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
    void clearReturnedEvents( fd_t, Object* );
#endif
    void clearSignal();
    int poll();
    int poll( timeout_ns_t timeout_ns );
  };


  class FDAndInternalEventQueue : public FDEventQueue, private NonBlockingFiniteQueue<Event*, 2048>
  {
  public:
    FDAndInternalEventQueue();

    // EventQueue
    virtual EventQueue* clone() const { return new FDAndInternalEventQueue; }
    bool enqueue( Event& );
    Event* dequeue();
    Event* try_dequeue();
    Event* timed_dequeue( timeout_ns_t timeout_ns );

  private:
    ~FDAndInternalEventQueue() { }

    bool dequeue_blocked;
  };


  class Client : public EventHandler
  {
  public:
    auto_Object<Log> get_log() const { return log; }
    uint8_t get_reconnect_tries_max() const { return reconnect_tries_max; }
    uint64_t get_operation_timeout_ns() const { return operation_timeout_ns; }
    auto_Object<SocketFactory> get_socket_factory() const { return socket_factory; }
    void set_reconnect_tries_max( uint8_t reconnect_tries_max ) { this->reconnect_tries_max = reconnect_tries_max; }
    void set_operation_timeout_ns( uint64_t operation_timeout_ns ) { this->operation_timeout_ns = operation_timeout_ns; }

    // EventHandler
    virtual void handleEvent( Event& );

  protected:
    template <class ClientType, class StageGroupType>
    static auto_Object<ClientType> create( auto_Object<StageGroupType> stage_group, const SocketAddress& peer_sockaddr, auto_Object<SocketFactory> socket_factory = NULL, auto_Object<Log> log = NULL )
    {
      ClientType* client = new ClientType( peer_sockaddr, socket_factory, log );
      static_cast<StageGroup*>( stage_group.get() )->createStage<ClientType, FDAndInternalEventQueue>( client->incRef(), new FDAndInternalEventQueue );
      return client;
    }

    Client( const SocketAddress& peer_sockaddr, auto_Object<SocketFactory> socket_factory, auto_Object<Log> log );
    virtual ~Client();

    virtual auto_Object<Request> createProtocolRequest( auto_Object<Object> body ) = 0;
    virtual auto_Object<Response> createProtocolResponse() = 0;
    virtual void respond( auto_Object<Request> protocol_request, auto_Object<Response> response ) = 0;

  private:
    SocketAddress peer_sockaddr;
    auto_Object<SocketFactory> socket_factory;
    auto_Object<Log> log;

    uint8_t reconnect_tries, reconnect_tries_max;
    uint64_t operation_timeout_ns;

    FDAndInternalEventQueue* fd_event_queue;


    class Connection : public Object, public InputStream, public OutputStream
    {
    public:
      enum State { IDLE = 0, CONNECTING, WRITING, READING };

      Connection( auto_Object<Socket> _socket );

      inline State get_state() const { return state; }
      const Time& get_last_activity_time() const { return last_activity_time; }
      auto_Object<Request> get_protocol_request() const { return protocol_request; }
      void set_protocol_request( auto_Object<Request> protocol_request ) { this->protocol_request = protocol_request; }
      auto_Object<Response> get_protocol_response() const { return protocol_response; }
      void set_protocol_response( auto_Object<Response> protocol_response ) { this->protocol_response = protocol_response; }
      inline void set_state( State state ) { this->state = state; }

      operator socket_t() const { return *_socket; }
      bool operator==( const Connection& other ) const { return _socket == other._socket; }

      bool close() { return _socket->close(); }
      Stream::Status connect( const SocketAddress& connect_to_sockaddr );
      bool shutdown() { return _socket->shutdown(); }
      void touch() { last_activity_time = Time(); }

      // InputStream
      Stream::Status read( void* buffer, size_t buffer_len, size_t* out_bytes_read = 0 );

      // OutputStream
      Stream::Status writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written = 0 );

    private:
      ~Connection() { }

      auto_Object<Socket> _socket;

      State state;
      Time last_activity_time;

      auto_Object<Request> protocol_request;
      auto_Object<Response> protocol_response;
    };

    std::vector<Connection*> connections;
    Connection* createConnection();
    void destroyConnection( Connection& connection );


    class ConnectionActivityCheckRequest : public Request
    {
    public:
      // Object
      YIELD_OBJECT_TYPE_INFO( REQUEST, "ConnectionActivityCheckRequest", 3667953665 );
    };

    class ConnectionActivityCheckTimer : public Timer
    {
    public:
      ConnectionActivityCheckTimer( Client& client );

      // Timer
      void fire();

    private:
      Client& client;

      ConnectionActivityCheckRequest stack_connection_activity_check_request;
    };

    auto_Object<ConnectionActivityCheckTimer> connection_activity_timer;
  };


  class RFC822Headers
  {
  public:
    RFC822Headers();
    virtual ~RFC822Headers();

    Stream::Status deserialize( InputStream& input_stream, size_t* out_bytes_read );
    char* get_header( const char* header_name, const char* default_value="" );
    char* operator[]( const char* header_name ) { return get_header( header_name ); }
    void set_header( const char* header, size_t header_len ); // Mutable header with name: value in one string, will copy both
    void set_header( const char* header_name, const char* header_value ); // Literal header
    void set_header( const char* header_name, char* header_value ); // Mutable header, will copy value
    void set_header( char* header_name, char* header_value ); // Mutable name and mutable value, will copy both
    void set_header( const std::string& header_name, const std::string& header_value ); // Mutable name and mutable value, will copy both
    Stream::Status serialize( OutputStream& output_stream, size_t* out_bytes_written );

  protected:
    void copy_iovec( const char* data, size_t len );
    void set_iovec( const char* data, size_t len ) { struct iovec _iovec; _iovec.iov_base = const_cast<char*>( data ); _iovec.iov_len = len; set_iovec( _iovec ); }
    void set_iovec( const struct iovec& out_iovec );

  private:
    enum { DESERIALIZING_LEADING_WHITESPACE, DESERIALIZING_HEADER_NAME, DESERIALIZING_HEADER_NAME_VALUE_SEPARATOR, DESERIALIZING_HEADER_VALUE, DESERIALIZING_HEADER_VALUE_TERMINATOR, DESERIALIZING_TRAILING_CRLF, DESERIALIZE_DONE } deserialize_state;
    char stack_buffer[YIELD_IPC_RFC822_HEADERS_STACK_BUFFER_LENGTH], *heap_buffer, *buffer_p;
    size_t heap_buffer_len;
    iovec stack_iovecs[YIELD_IPC_RFC822_HEADERS_STACK_IOVECS_LENGTH], *heap_iovecs; uint8_t iovecs_filled;

    inline void advanceBufferPointer()
    {
      buffer_p++;

      if ( heap_buffer == NULL )
      {
        if ( buffer_p - stack_buffer < YIELD_IPC_RFC822_HEADERS_STACK_BUFFER_LENGTH )
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


  class HTTPRequest : public Request, public RFC822Headers
  {
  public:
    HTTPRequest(); // Incoming
    HTTPRequest( const char* method, const char* relative_uri, const char* host, auto_Object<> body = NULL ); // Outgoing
    HTTPRequest( const char* method, const URI& absolute_uri, auto_Object<> body = NULL ); // Outgoing

    uint8_t get_http_version() const { return http_version; }
    const char* get_method() const { return method; }
    const char* get_uri() const { return uri; }
    auto_Object<> get_body() const { return body; }

    bool respond( uint16_t status_code );
    bool respond( uint16_t status_code, auto_Object<Object> body );
    virtual bool respond( Response& response ) { return Request::respond( response ); }

    // Object
    YIELD_OBJECT_TYPE_INFO( REQUEST, "HTTPRequest", 2869724743UL );
    Stream::Status deserialize( InputStream&, size_t* out_bytes_read = NULL );
    Stream::Status serialize( OutputStream&, size_t* out_bytes_written = NULL );

  protected:
    virtual ~HTTPRequest();

  private:
    void init( const char* method, const char* relative_uri, const char* host, auto_Object<> body );

    char method[16];
    char* uri; size_t uri_len;
    uint8_t http_version;
    auto_Object<> body;

    enum { DESERIALIZING_METHOD, DESERIALIZING_URI, DESERIALIZING_HTTP_VERSION, DESERIALIZING_HEADERS, DESERIALIZING_BODY, DESERIALIZE_DONE } deserialize_state;
    enum { SERIALIZING_METHOD, SERIALIZING_METHOD_URI_SEPARATOR, SERIALIZING_URI, SERIALIZING_HTTP_VERSION, SERIALIZING_HEADERS, SERIALIZING_BODY, SERIALIZE_DONE } serialize_state;
  };


  class HTTPResponse : public Response, protected RFC822Headers
  {
  public:
    HTTPResponse(); // Incoming
    HTTPResponse( uint16_t status_code ); // Outgoing
    HTTPResponse( uint16_t status_code, auto_Object<> body ); // Outgoing

    auto_Object<> get_body() const { return body; }
    uint8_t get_http_version() const { return http_version; }
    uint16_t get_status_code() const { return status_code; }
    void set_body( auto_Object<> body ) { this->body = body; }
    void set_status_code( uint16_t status_code ) { this->status_code = status_code; }

    // Object
    YIELD_OBJECT_TYPE_INFO( RESPONSE, "HTTPResponse", 231649460UL );
    HTTPResponse& incRef() { return Object::incRef( *this ); }
    Stream::Status deserialize( InputStream&, size_t* out_bytes_read = NULL );
    Stream::Status serialize( OutputStream&, size_t* out_bytes_written = NULL );

  private:
    ~HTTPResponse() { }

    uint8_t http_version;
    union { char status_code_str[4]; uint16_t status_code; };
    auto_Object<> body;

    enum { DESERIALIZING_HTTP_VERSION, DESERIALIZING_STATUS_CODE, DESERIALIZING_REASON, DESERIALIZING_HEADERS, DESERIALIZING_BODY, DESERIALIZE_DONE } deserialize_state;
    enum { SERIALIZING_STATUS_LINE, SERIALIZING_HEADERS, SERIALIZING_BODY, SERIALIZE_DONE } serialize_state;
  };


  class HTTPClient : public Client
  {
  public:
    template <class StageGroupType>
    static auto_Object<HTTPClient> create( auto_Object<StageGroupType>& stage_group, const SocketAddress& peer_sockaddr, auto_Object<SocketFactory> socket_factory = NULL, auto_Object<Log> log = NULL )
    {
      return Client::create<HTTPClient, StageGroupType>( stage_group, peer_sockaddr, socket_factory, log );
    }

    static auto_Object<HTTPResponse> GET( const URI& absolute_uri, auto_Object<Log> log = NULL );
    static auto_Object<HTTPResponse> PUT( const URI& absolute_uri, auto_Object<> body, auto_Object<Log> log = NULL );
    static auto_Object<HTTPResponse> PUT( const URI& absolute_uri, const Path& body_file_path, auto_Object<Log> log = NULL );

    // Object
    HTTPClient& incRef() { return Object::incRef( *this ); }

    // EventHandler
    virtual const char* getEventHandlerName() const { return "HTTPClient"; }

  protected:
    friend class Client;

    HTTPClient( const SocketAddress& peer_sockaddr, auto_Object<SocketFactory> socket_factory, auto_Object<Log> log )
      : Client( peer_sockaddr, socket_factory, log )
    { }

    virtual ~HTTPClient() { }

    virtual auto_Object<Request> createProtocolRequest( auto_Object<> body );
    virtual auto_Object<Response> createProtocolResponse();
    virtual void respond( auto_Object<Request> protocol_request, auto_Object<Response> response );

  private:
    static auto_Object<HTTPResponse> sendHTTPRequest( const char* method, const YIELD::URI& uri, auto_Object<> body, auto_Object<Log> log );
  };


  class JSONValue;

  class JSONInputStream : public StructuredInputStream
  {
  public:
    JSONInputStream( InputStream& underlying_input_stream );
    virtual ~JSONInputStream();

    // StructuredInputStream
    YIELD_STRUCTURED_INPUT_STREAM_PROTOTYPES;

  protected:
    JSONInputStream( const Declaration& root_decl, JSONValue& root_json_value );

  private:
    const Declaration* root_decl;
    JSONValue *root_json_value, *next_json_value; // next_json_value is for arrays and maps

    virtual void readSequence( Object& );
    virtual void readMap( Object& );
    virtual void readStruct( Object& );
    JSONValue* readJSONValue( const Declaration&, Object::GeneralType = Object::UNKNOWN );
  };


  class JSONOutputStream : public StructuredOutputStream
  {
  public:
    JSONOutputStream( OutputStream& underlying_output_stream, bool write_empty_strings = true );
    virtual ~JSONOutputStream(); // If the stream is wrapped in map, sequence, etc. then the constructor will append the final } or [, so the underlying output stream should not be deleted before this object!

    // StructuredOutputStream
    YIELD_STRUCTURED_OUTPUT_STREAM_PROTOTYPES;
    virtual void writePointer( const Declaration& decl, void* value );

  protected:
    JSONOutputStream( OutputStream& underlying_output_stream, bool write_empty_strings, yajl_gen writer, const Declaration& root_decl );

    virtual void writeDeclaration( const Declaration& );
    virtual void writeSequence( Object* ); // Can be NULL for empty arrays
    virtual void writeMap( Object* ); // Can be NULL for empty maps
    virtual void writeStruct( Object* );

  private:
    OutputStream& underlying_output_stream;
    bool write_empty_strings;

    const Declaration* root_decl; // Mostly for debugging, also used to indicate if this is the root JSONOutputStream
    yajl_gen writer;
    bool in_map;

    void flushYAJLBuffer();
  };


  class ObjectFactory : public Object
  {
  public:
    virtual Object* createObject() const = 0;

    // Object
    ObjectFactory& incRef() { return Object::incRef( *this ); }

  protected:
    virtual ~ObjectFactory() { }
  };

  template <class ObjectType>
  class ObjectFactoryImpl : public ObjectFactory
  {
  public:
    // ObjectFactory
    Object* createObject() const { return new ObjectType; }

  private:
    ~ObjectFactoryImpl() { }
  };

  class ObjectFactories : public Object, private CuckooHashTable<ObjectFactory*>
  {
  public:
    auto_Object<> createObject( const std::string& type_name ) { return createObject( type_name.c_str() ); }
    auto_Object<> createObject( const char* type_name ) { return createObject( string_hash( type_name ) ); }
    auto_Object<> createObject( uint32_t type_id )
    {
      auto_Object<ObjectFactory> object_factory = getObjectFactory( type_id );
      if ( object_factory != NULL )
        return object_factory->createObject();
      else
        return 0;
    }

    const auto_Object<ObjectFactory> getObjectFactory( const std::string& type_name ) { return getObjectFactory( type_name.c_str() ); }
    const auto_Object<ObjectFactory> getObjectFactory( const char* type_name ) { return getObjectFactory( string_hash( type_name ) ); }
    const auto_Object<ObjectFactory> getObjectFactory( uint32_t type_id )
    {
      ObjectFactory* object_factory = CuckooHashTable<ObjectFactory*>::find( type_id );
      if ( object_factory != NULL )
        return object_factory->incRef();
      else
        return NULL;
    }

    void registerObjectFactory( const std::string& type_name, auto_Object<ObjectFactory> object_factory ) { return registerObjectFactory( type_name.c_str(), object_factory ); }
    void registerObjectFactory( const char* type_name, auto_Object<ObjectFactory> object_factory ) { registerObjectFactory( string_hash( type_name ), object_factory ); }
    void registerObjectFactory( uint32_t type_id, auto_Object<ObjectFactory> object_factory )
    {
      Object::decRef( CuckooHashTable<ObjectFactory*>::erase( type_id ) );
      CuckooHashTable<ObjectFactory*>::insert( type_id, object_factory.release() );
    }

  private:
    ~ObjectFactories()
    {
      for ( CuckooHashTable<ObjectFactory*>::iterator object_factory_i = begin(); object_factory_i != end(); object_factory_i++ )
        Object::decRef( *object_factory_i );
    }
  };


  class ONCRPCClient : public Client
  {
  public:
    static auto_Object<ONCRPCClient>create( auto_Object<StageGroup> stage_group, const SocketAddress& peer_sockaddr, auto_Object<SocketFactory> socket_factory = NULL, auto_Object<Log> log = NULL )
    {
      return Client::create<ONCRPCClient>( stage_group, peer_sockaddr, socket_factory, log );
    }

    // Object
    ONCRPCClient& incRef() { return Object::incRef( *this ); }

    // EventHandler
    virtual const char* getEventHandlerName() const { return "ONCRPCClient"; }

  protected:
    friend class Client;

    ONCRPCClient( const SocketAddress& peer_sockaddr, auto_Object<SocketFactory> socket_factory, auto_Object<Log> log );
    virtual ~ONCRPCClient() { }

    auto_Object<ObjectFactories> object_factories;

    // Client
    virtual auto_Object<Request> createProtocolRequest( auto_Object<> body );
    virtual auto_Object<Response> createProtocolResponse();
    virtual void respond( auto_Object<Request> protocol_request, auto_Object<Response> response );
  };


  class ONCRPCMessage
  {
  public:
    auto_Object<> get_body() const { return body; }
    uint32_t get_xid() const { return xid; }

  protected:
    ONCRPCMessage( auto_Object<> body, auto_Object<Log> log )
      : body( body ), log( log )
    {
      xid = 0;
      oncrpc_record_input_stream = NULL;
    }

    ONCRPCMessage( auto_Object<ObjectFactories> object_factories, auto_Object<Log> log )
      : object_factories( object_factories ), log( log )
    {
      xid = 0;
      oncrpc_record_input_stream = NULL;
    }

    ONCRPCMessage( uint32_t credential_auth_flavor, auto_Object<> credential, auto_Object<> body, auto_Object<Log> log ) // Outgoing
      : body( body ), log( log )
    {
      xid = 0;
      oncrpc_record_input_stream = NULL;
    }

    virtual ~ONCRPCMessage();


    auto_Object<ObjectFactories> object_factories;
    auto_Object<> body;
    auto_Object<Log> log;

    uint32_t xid;

    ONCRPCRecordInputStream& get_oncrpc_record_input_stream( InputStream& underlying_input_stream );

  private:
    ONCRPCRecordInputStream* oncrpc_record_input_stream;
  };


  class ONCRPCRequest : public Request, public ONCRPCMessage
  {
  public:
    const static uint32_t AUTH_NONE = 0;


    ONCRPCRequest( auto_Object<> body, auto_Object<Log> log = NULL )
      : ONCRPCMessage( body, log )
    {
      credential_auth_flavor = AUTH_NONE;
    }

    ONCRPCRequest( auto_Object<ObjectFactories> object_factories, auto_Object<Log> log = NULL )
      : ONCRPCMessage( object_factories, log )
    {
      credential_auth_flavor = AUTH_NONE;
    }


    ONCRPCRequest( uint32_t credential_auth_flavor, auto_Object<> credential, auto_Object<> body, auto_Object<Log> log = NULL )
      : ONCRPCMessage( body, log ), credential_auth_flavor( credential_auth_flavor ), credential( credential )
    { }

    uint32_t get_credential_auth_flavor() const { return credential_auth_flavor; }
    auto_Object<> get_credential() const { return credential; }

    // Object
    YIELD_OBJECT_TYPE_INFO( REQUEST, "ONCRPCRequest", 3095736087UL );
    ONCRPCRequest& incRef() { return Object::incRef( *this ); }
    Stream::Status deserialize( InputStream&, size_t* out_bytes_read = 0 );
    Stream::Status serialize( OutputStream&, size_t* out_bytes_read = 0 );

  private:
    ~ONCRPCRequest() { }

    uint32_t credential_auth_flavor;
    auto_Object<> credential;
  };


  class ONCRPCResponse : public Response, public ONCRPCMessage
  {
  public:
    ONCRPCResponse( auto_Object<ObjectFactories> object_factories, auto_Object<Log> log = NULL ) // Incoming
      : ONCRPCMessage( object_factories, log )
    { }

    ONCRPCResponse( uint32_t xid, auto_Object<> body, auto_Object<Log> log = NULL ) // Outgoing
      : ONCRPCMessage( body, log )
    {
      this->xid = xid;
    }

    // Object
    YIELD_OBJECT_TYPE_INFO( REQUEST, "ONCRPCResponse", 2752670386UL );
    Stream::Status deserialize( InputStream&, size_t* out_bytes_read = 0 );
    Stream::Status serialize( OutputStream&, size_t* out_bytes_read = 0 );

  private:
    ~ONCRPCResponse() { }
  };


#ifdef YIELD_HAVE_OPENSSL

  class SSLContext : public Object
	{
	public:
    SSLContext( SSL_METHOD* method, const Path& pem_certificate_file_path, const Path& pem_private_key_file_path, const std::string& pem_private_key_passphrase )
      : pem_private_key_passphrase( pem_private_key_passphrase )
    {
      ctx = createSSL_CTX( method );

      if ( SSL_CTX_use_certificate_file( ctx, pem_certificate_file_path, SSL_FILETYPE_PEM ) > 0 )
      {
        if ( !pem_private_key_passphrase.empty() )
        {
          SSL_CTX_set_default_passwd_cb( ctx, pem_password_callback );
          SSL_CTX_set_default_passwd_cb_userdata( ctx, this );
        }

        if ( SSL_CTX_use_PrivateKey_file( ctx, pem_private_key_file_path, SSL_FILETYPE_PEM ) > 0 )
          return;
      }

      throwOpenSSLException();
    }

    SSLContext( SSL_METHOD* method, const std::string& pem_certificate_str, const std::string& pem_private_key_str, const std::string& pem_private_key_passphrase )
      : pem_private_key_passphrase( pem_private_key_passphrase )
    {
      ctx = createSSL_CTX( method );

      BIO* pem_certificate_bio = BIO_new_mem_buf( reinterpret_cast<void*>( const_cast<char*>( pem_certificate_str.c_str() ) ), static_cast<int>( pem_certificate_str.size() ) );
      if ( pem_certificate_bio != NULL )
      {
        X509* cert = PEM_read_bio_X509( pem_certificate_bio, NULL, pem_password_callback, this );
        if ( cert != NULL )
        {
          SSL_CTX_use_certificate( ctx, cert );

          BIO* pem_private_key_bio = BIO_new_mem_buf( reinterpret_cast<void*>( const_cast<char*>( pem_private_key_str.c_str() ) ), static_cast<int>( pem_private_key_str.size() ) );
          if ( pem_private_key_bio != NULL )
          {
            EVP_PKEY* pkey = PEM_read_bio_PrivateKey( pem_private_key_bio, NULL, pem_password_callback, this );
            if ( pkey != NULL )
            {
              SSL_CTX_use_PrivateKey( ctx, pkey );

              BIO_free( pem_certificate_bio );
              BIO_free( pem_private_key_bio );

              return;
            }

            BIO_free( pem_private_key_bio );
          }
        }

        BIO_free( pem_certificate_bio );
      }

      throwOpenSSLException();
    }

    SSLContext( SSL_METHOD* method, const Path& pkcs12_file_path, const std::string& pkcs12_passphrase )
    {
      ctx = createSSL_CTX( method );

      BIO* bio = BIO_new_file( pkcs12_file_path, "rb" );
      if ( bio != NULL )
      {
        PKCS12* p12 = d2i_PKCS12_bio( bio, NULL );
        if ( p12 != NULL )
        {
          EVP_PKEY* pkey = NULL;
          X509* cert = NULL;
          STACK_OF( X509 )* ca = NULL;
          if ( PKCS12_parse( p12, pkcs12_passphrase.c_str(), &pkey, &cert, &ca ) )
          {
            if ( pkey != NULL && cert != NULL && ca != NULL )
            {
              SSL_CTX_use_certificate( ctx, cert );
              SSL_CTX_use_PrivateKey( ctx, pkey );

              X509_STORE* store = SSL_CTX_get_cert_store( ctx );
              for ( int i = 0; i < sk_X509_num( ca ); i++ )
              {
                X509* store_cert = sk_X509_value( ca, i );
                X509_STORE_add_cert( store, store_cert );
              }

              BIO_free( bio );

              return;
            }
            else
            {
              BIO_free( bio );
              throw Exception( "invalid PKCS#12 file or passphrase" );
            }
          }
        }

        BIO_free( bio );
      }

      throwOpenSSLException();
    }

    SSL_CTX* get_ssl_ctx() const { return ctx; }

    // Object
    inline SSLContext& incRef() { return Object::incRef( *this ); }

  private:
    ~SSLContext()
    {
      SSL_CTX_free( ctx );
    }


    std::string pem_private_key_passphrase;

    SSL_CTX* ctx;


    SSL_CTX* createSSL_CTX( SSL_METHOD* method )
    {
      SSL_library_init();
      OpenSSL_add_all_algorithms();

      SSL_CTX* ctx = SSL_CTX_new( method );
      if ( ctx != NULL )
      {
#ifdef SSL_OP_NO_TICKET
        SSL_CTX_set_options( ctx, SSL_OP_ALL|SSL_OP_NO_TICKET );
#else
        SSL_CTX_set_options( ctx, SSL_OP_ALL );
#endif
        SSL_CTX_set_verify( ctx, SSL_VERIFY_NONE, NULL );
        return ctx;
      }
      else
      {
        throwOpenSSLException();
        return NULL;
      }
    }

    static int pem_password_callback( char *buf, int size, int rwflag, void *userdata )
    {
      SSLContext* this_ = static_cast<SSLContext*>( userdata );
      if ( size > static_cast<int>( this_->pem_private_key_passphrase.size() ) )
        size = static_cast<int>( this_->pem_private_key_passphrase.size() );
      std::memcpy( buf, this_->pem_private_key_passphrase.c_str(), size );
      return size;
    }

    void throwOpenSSLException()
    {
      SSL_load_error_strings();
      throw Exception( ERR_error_string( ERR_get_error(), NULL ) );
    }
	};


  class SSLSocket : public TCPSocket
  {
  public:
    SSLSocket( auto_Object<SSLContext> ctx, auto_Object<Log> log = NULL ) // Steals a reference to ctx
      : TCPSocket( log ), ctx( ctx )
    {
      ssl = SSL_new( ctx->get_ssl_ctx() );
      SSL_set_fd( ssl, *this );
//      SSL_set_mode( ssl, SSL_MODE_ENABLE_PARTIAL_WRITE|SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER );
      init( log );
    }

    // Object
    YIELD_OBJECT_TYPE_INFO( EVENT, "SSLSocket", 2540210862UL );
    inline SSLSocket& incRef() { return Object::incRef( *this ); }

    // InputStream
    Stream::Status read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
    {
      int SSL_read_ret = SSL_read( ssl, buffer, static_cast<int>( buffer_len ) );
      if ( SSL_read_ret > 0 )
      {
        if ( log != NULL )
        {
          Log::Stream log_stream = log->getStream( Log::LOG_DEBUG );
          log_stream << "SSLSocket: read on " << this << ": ";
          log_stream.write( buffer, static_cast<size_t>( SSL_read_ret ) );
        }

        if ( out_bytes_read )
          *out_bytes_read = static_cast<size_t>( SSL_read_ret );

        return STREAM_STATUS_OK;
      }
      else
        return returnSSLStatus();
    }

    // OutputStream
    Stream::Status writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
    {
      int SSL_write_ret;
      if ( buffers_count == 1 ) // && buffers[0].iov_len < SSL_MAX_CONTENT_LEN )
      {
        SSL_write_ret = SSL_write( ssl, buffers[0].iov_base, static_cast<int>( buffers[0].iov_len ) );
        if ( SSL_write_ret > 0 && out_bytes_written )
          *out_bytes_written = static_cast<size_t>( SSL_write_ret );
      }
      else // Concatenate buffers into a single write_buffer and write that
      {
        if ( write_buffer == NULL )
        {
          for ( unsigned int buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
 	    write_buffer_len += buffers[buffer_i].iov_len;
          write_buffer_p = write_buffer = new unsigned char[write_buffer_len];
          for ( unsigned int buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
          {
            memcpy( write_buffer_p, buffers[buffer_i].iov_base, buffers[buffer_i].iov_len );
            write_buffer_p += buffers[buffer_i].iov_len;
          }
          write_buffer_p = write_buffer;
        }

        size_t total_bytes_written = 0;
//	      for ( ;; ) // SSL_write multiple times if the buffer is > the max record len
//        {
          int SSL_write_len = static_cast<int>( write_buffer_len - static_cast<size_t>( write_buffer_p - write_buffer ) );
    //			if ( SSL_write_len > SSL_MAX_CONTENT_LEN ) SSL_write_len = SSL_MAX_CONTENT_LEN;
          SSL_write_ret = SSL_write( ssl, reinterpret_cast<unsigned char*>( write_buffer_p ), SSL_write_len );
          if ( SSL_write_ret > 0 )
          {
            write_buffer_p += SSL_write_ret;
            total_bytes_written += SSL_write_ret;

            if ( static_cast<size_t>( write_buffer_p - write_buffer ) == write_buffer_len )
            {
              delete [] write_buffer;
              write_buffer_p = write_buffer = NULL;
              write_buffer_len = 0;

              if ( out_bytes_written )
                *out_bytes_written = total_bytes_written;

              // break;
            }
            else
              DebugBreak(); // continue;
  	      }
//          else
//            break;
//        }
      }

      if ( SSL_write_ret > 0 )
      {
        if ( log != NULL )
        {
          Log::Stream log_stream = log->getStream( Log::LOG_DEBUG );
          log_stream << "SSLSocket: write on " << this << ": ";
          log_stream.write( buffers, buffers_count );
        }

        return STREAM_STATUS_OK;
      }
      else
        return returnSSLStatus();
    }

    // TCPSocket
    auto_Object<SSLSocket> accept()
    {
      socket_t peer_socket = TCPSocket::_accept();
      if ( peer_socket != static_cast<socket_t>( -1 ) )
      {
        SSL* peer_ssl = SSL_new( ctx->get_ssl_ctx() );
        SSL_set_fd( peer_ssl, peer_socket );
        SSL_set_accept_state( peer_ssl );
        return new SSLSocket( *peer_ssl, log );
      }
      else
        return NULL;
    }

    Stream::Status connect( const SocketAddress& peer_sockaddr )
    {
      Stream::Status connect_status = TCPSocket::connect( peer_sockaddr );
      if ( connect_status == STREAM_STATUS_OK )
      {
        SSL_set_fd( ssl, *this ); // Have to SSL_set_fd again in case connect had to re-create the socket falling back to IPv4 to IPv6
        SSL_set_connect_state( ssl );
      }
      return connect_status;
    }

    bool shutdown()
    {
      if ( SSL_shutdown( ssl ) != -1 )
        return TCPSocket::shutdown();
      else
        return false;
    }

  private:
    SSLSocket( SSL& ssl, auto_Object<Log> log )
      : TCPSocket( static_cast<socket_t>( SSL_get_fd( &ssl ) ), log ),
        ssl( &ssl )
    {
      ctx = NULL;
      init( log );
    }

    ~SSLSocket()
    {
      SSL_free( ssl );
      delete [] write_buffer;
    }

    void init( auto_Object<Log> log )
    {
      write_buffer = write_buffer_p = NULL;
      write_buffer_len = 0;
      if ( log != NULL )
      {
        SSL_set_app_data( ssl, reinterpret_cast<char*>( this ) );
        SSL_set_info_callback( ssl, info_callback );
      }
    }

    auto_Object<SSLContext> ctx;
    SSL* ssl;
    unsigned char *write_buffer, *write_buffer_p; size_t write_buffer_len;


    static void info_callback( const SSL* ssl, int where, int ret )
    {
      std::ostringstream info;

      int w = where & ~SSL_ST_MASK;
      if ( ( w & SSL_ST_CONNECT ) == SSL_ST_CONNECT ) info << "SSL_connect:";
      else if ( ( w & SSL_ST_ACCEPT ) == SSL_ST_ACCEPT ) info << "SSL_accept:";
      else info << "undefined:";

      if ( ( where & SSL_CB_LOOP ) == SSL_CB_LOOP )
        info << SSL_state_string_long( ssl );
      else if ( ( where & SSL_CB_ALERT ) == SSL_CB_ALERT )
      {
        if ( ( where & SSL_CB_READ ) == SSL_CB_READ )
          info << "read:";
        else
          info << "write:";
        info << "SSL3 alert" << SSL_alert_type_string_long( ret ) << ":" << SSL_alert_desc_string_long( ret );
      }
      else if ( ( where & SSL_CB_EXIT ) == SSL_CB_EXIT )
      {
        if ( ret == 0 )
          info << "failed in " << SSL_state_string_long( ssl );
        else
          info << "error in " << SSL_state_string_long( ssl );
      }
      else
        return;

      reinterpret_cast<SSLSocket*>( SSL_get_app_data( const_cast<SSL*>( ssl ) ) )->log->getStream( Log::LOG_NOTICE ) << "SSLSocket: " << info.str();
    }

    Stream::Status returnSSLStatus()
    {
      if ( SSL_want_read( ssl ) == 1 )
      {
        if ( log != NULL && log->get_level() >= Log::LOG_INFO )
          log->getStream( Log::LOG_INFO ) << "SSLSocket: would block on read on socket #" << this << ".";
        return STREAM_STATUS_WANT_READ;
      }
      if ( SSL_want_write( ssl ) == 1 )
      {
        if ( log != NULL && log->get_level() >= Log::LOG_INFO )
          log->getStream( Log::LOG_INFO ) << "SSLSocket: would block on write on " << this << ".";
        return STREAM_STATUS_WANT_WRITE;
      }
      else
      {
        if ( log != NULL && log->get_level() >= Log::LOG_INFO )
          log->getStream( Log::LOG_INFO ) << "SSLSocket: lost connection on " << this << ", error = " << Exception::strerror() << ".";
        return STREAM_STATUS_ERROR;
      }
    }
  };


  class SSLSocketFactory : public SocketFactory
  {
  public:
    SSLSocketFactory( auto_Object<SSLContext> ssl_context, auto_Object<Log> log = NULL )
      : SocketFactory( log ), ssl_context( ssl_context )
    { }

    // SocketFactory
    virtual auto_Object<Socket> createSocket( auto_Object<Log> log = NULL )
    {
      return new SSLSocket( ssl_context, log != NULL ? log : this->log );
    }

  private:
    ~SSLSocketFactory() { }

    auto_Object<SSLContext> ssl_context;
  };

#endif


  class UDPSocket : public Socket
  {
  public:
    UDPSocket( auto_Object<Log> log = NULL );

    bool joinMulticastGroup( const SocketAddress& multicast_group_sockaddr, bool loopback );
    bool leaveMulticastGroup( const SocketAddress& multicast_group_sockaddr );

    // Object
    YIELD_OBJECT_TYPE_INFO( EVENT, "UDPSocket", 2607589533UL );
    inline UDPSocket& incRef() { return Object::incRef( *this ); }

    // InputStream
    Stream::Status read( void* buffer, size_t buffer_len, size_t* out_bytes_read = 0 );

  private:
    ~UDPSocket() { }
  };


  class URI : public Object
  {
  public:
    // Factory methods return NULL instead of throwing exceptions
    static auto_Object<URI> parse( const char* uri ) { return parse( uri, std::strlen( uri ) ); }
    static auto_Object<URI> parse( const std::string& uri ) { return parse( uri.c_str(), uri.size() ); }
    static auto_Object<URI> parse( const char* uri, size_t uri_len );

    // Constructors throw exceptions
    URI( const char* uri ) { init( uri, std::strlen( uri ) ); }
    URI( const std::string& uri ) { init( uri.c_str(), uri.size() ); }
    URI( const char* uri, size_t uri_len ) { init( uri, uri_len ); }
    URI( const URI& other );
    virtual ~URI() { }

    const std::string& get_scheme() const { return scheme; }
    const std::string& get_host() const { return host; }
    const std::string& get_password() const { return password; }
    unsigned short get_port() const { return port; }
    const std::string& get_resource() const { return resource; }
    const std::string& get_user() const { return user; }
    void set_port( unsigned short port ) { this->port = port; }

    // Object
    URI& incRef() { return Object::incRef( *this ); }

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


#ifdef YIELD_HAVE_ZLIB
  class zlibOutputStream : public OutputStream
  {
  public:
    zlibOutputStream()
    {
      zstream.zalloc = Z_NULL;
      zstream.zfree = Z_NULL;
      zstream.opaque = Z_NULL;
      out_ev = NULL;
    }

    auto_Object<String> serialize( String& s, int level = Z_BEST_COMPRESSION )
    {
      if ( deflateInit( &zstream, level ) == Z_OK )
      {
        zstream.next_out = reinterpret_cast<Bytef*>( zout );
        zstream.avail_out = sizeof( zout );
        total_bytes_written = 0;
        out_ev = new String();

        s.serialize( *this, NULL );

        int deflate_ret;

        while ( ( deflate_ret = deflate( &zstream, Z_FINISH ) ) == Z_OK ) // Z_OK = need more buffer space to finish compression, Z_STREAM_END = really done
        {
          out_ev->append( zout, sizeof( zout ) );
          zstream.next_out = reinterpret_cast<Bytef*>( zout );
          zstream.avail_out = sizeof( zout );
        }

        if ( deflate_ret == Z_STREAM_END )
        {
          if ( ( deflate_ret = deflateEnd( &zstream ) ) == Z_OK )
          {
            if ( zstream.avail_out < sizeof( zout ) )
              out_ev->append( zout, sizeof( zout ) - zstream.avail_out );

            if ( out_ev->size() < total_bytes_written ) // i.e. the compressed buffer is smaller than the input buffer(s)
              return out_ev;
          }
        }

        Object::decRef( *out_ev );
      }

      return NULL;
    }

    // OutputStream
    Stream::Status writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
    {
      int deflate_ret;
      size_t bytes_written = 0;

      for ( size_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
      {
        zstream.next_in = reinterpret_cast<Bytef*>( buffers[buffer_i].iov_base );
        zstream.avail_in = buffers[buffer_i].iov_len;
        bytes_written += buffers[buffer_i].iov_len;
        total_bytes_written += buffers[buffer_i].iov_len;

        while ( ( deflate_ret = deflate( &zstream, Z_NO_FLUSH ) ) == Z_OK )
        {
          if ( zstream.avail_out > 0 )
          {
            if ( out_bytes_written )
              *out_bytes_written = bytes_written;
            return STREAM_STATUS_OK;
          }
          else
          {
            out_ev->append( zout, sizeof( zout ) );
            zstream.next_out = reinterpret_cast<Bytef*>( zout );
            zstream.avail_out = sizeof( zout );
          }
        }
      }

      if ( deflate_ret == Z_OK )
      {
        if ( out_bytes_written )
          *out_bytes_written = bytes_written;
        return STREAM_STATUS_OK;
      }
      else
        return STREAM_STATUS_ERROR;
    }

  private:
    z_stream zstream;
    char zout[4096]; size_t total_bytes_written;
    String* out_ev;
  };
#endif
};

#endif
