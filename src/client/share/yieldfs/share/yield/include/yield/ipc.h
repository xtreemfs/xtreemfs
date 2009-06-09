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
virtual bool bind( auto_Object<SocketAddress> to_sockaddr ); \
virtual bool close(); \
virtual bool connect( auto_Object<SocketAddress> to_sockaddr ); \
virtual bool get_blocking_mode() const; \
virtual auto_Object<SocketAddress> getpeername(); \
virtual auto_Object<SocketAddress> getsockname(); \
virtual operator int() const; \
virtual ssize_t recv( void* buffer, size_t buffer_len ); \
virtual ssize_t send( const void* buffer, size_t buffer_len ); \
virtual ssize_t sendmsg( const struct iovec* buffers, uint32_t buffers_count ); \
virtual bool set_blocking_mode( bool blocking ); \
virtual bool shutdown() { return true; } \
virtual bool want_read() const; \
virtual bool want_write() const;


namespace YIELD
{
  class FDEventQueue;
  class FDAndInternalEventQueue;
  class ONCRPCRecordInputStream;
  class Socket;
  class SocketAddress;
  class SSLContext;
#ifdef YIELD_HAVE_OPENSSL
  class SSLSocket;
#endif
  class TCPSocket;
  class URI;


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
    EventFDPipe( auto_Object<TCPSocket> read_end, auto_Object<TCPSocket> write_end );
#endif
    ~EventFDPipe();

#ifdef YIELD_HAVE_LINUX_EVENTFD
    int fd;
#else
    auto_Object<TCPSocket> read_end, write_end;
#endif
  };


  class FDEventQueue : public EventQueue
  {
  public:
    class FDEvent : public Event
    {
    public:
      FDEvent( auto_Object<> context, uint32_t error_code, bool _want_read )
        : context( context ), error_code( error_code ), _want_read( _want_read )
      { }

      inline auto_Object<> get_context() const { return context; }
      inline uint32_t get_error_code() const { return error_code; }
      inline bool want_read() const { return _want_read; }

      // Object
      YIELD_OBJECT_PROTOTYPES( FDEvent, 202 );

    private:
      ~FDEvent() { }

      auto_Object<> context;
      uint32_t error_code;
      bool _want_read;
    };


    class TimerEvent : public Event
    {
    public:
      TimerEvent( const Time& timeout, const Time& period, auto_Object<> context = NULL )
        : context( context ), 
          fire_time( Time() + timeout ), 
          timeout( timeout ), period( period )
      { }

      auto_Object<> get_context() const { return context; }
      const Time& get_fire_time() const { return fire_time; }
      const Time& get_period() const { return period; }
      const Time& get_timeout() const { return timeout; }

      // Object
      YIELD_OBJECT_PROTOTYPES( TimerEvent, 218 );    

    private:
      ~TimerEvent() { }

      auto_Object<> context;
      Time fire_time, timeout, period;
    };



    FDEventQueue();

    bool attach( int fd, auto_Object<> context = NULL, bool enable_read = true, bool enable_write = false );
    void detach( int fd );
    bool toggle( int fd, bool enable_read, bool enable_write );

    void signal() { eventfd_pipe->signal(); }
    auto_Object<TimerEvent> timer_create( const Time& timeout, auto_Object<> context = NULL ) { return timer_create( timeout, Time( static_cast<uint64_t>( 0 ) ), context ); }
    auto_Object<TimerEvent> timer_create( const Time& timeout, const Time& period, auto_Object<> context = NULL );

    // Object
    YIELD_OBJECT_PROTOTYPES( FDEventQueue, 203 );

    // EventQueue
    virtual bool enqueue( Event& ); // Discards events
    virtual Event* dequeue();
    virtual Event* dequeue( uint64_t timeout_ns );

  protected:
    virtual ~FDEventQueue();

  private:
#if defined(YIELD_HAVE_LINUX_EPOLL)
    int poll_fd;
    struct epoll_event* returned_events;
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
    int poll_fd;
    struct kevent* returned_events;
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
    int poll_fd;
    port_event_t* returned_events;
#else
#ifdef _WIN32
    fd_set *read_fds, *read_fds_copy, *write_fds, *write_fds_copy, *except_fds, *except_fds_copy;
    STLHashMap<Object*>::iterator next_fd_to_check;
#else
    typedef std::vector<pollfd> pollfd_vector;
    pollfd_vector::size_type next_pollfd_to_check;
    pollfd_vector pollfds;
#endif
#endif

    int active_fds;
    auto_Object<EventFDPipe> eventfd_pipe;
    STLHashMap<Object*> fd_to_context_map;
    std::vector<TimerEvent*> timers;


    static bool compareTimerEvents( const TimerEvent*, const TimerEvent* );
    FDEvent* dequeueFDEvent();
    TimerEvent* dequeueTimerEvent();
    int poll();
    int poll( uint64_t timeout_ns );
  };


  class FDAndInternalEventQueue : public FDEventQueue, private NonBlockingFiniteQueue<Event*, 2048>
  {
  public:
    FDAndInternalEventQueue();

    // Object
    YIELD_OBJECT_PROTOTYPES( FDAndInternalEventQueue, 204 );

    // EventQueue
    Event* dequeue();
    Event* dequeue( uint64_t timeout_ns );
    bool enqueue( Event& );
    Event* try_dequeue();    

  private:
    ~FDAndInternalEventQueue() { }

    bool dequeue_blocked;
  };


  template <class ProtocolRequestType, class ProtocolResponseType>
  class SocketClient
  {
  public:    
    const static uint64_t OPERATION_TIMEOUT_DEFAULT = 30 * NS_IN_S;
    const static uint8_t RECONNECT_TRIES_MAX_DEFAULT = UINT8_MAX;

  protected:
    SocketClient( const URI& absolute_uri, auto_Object<FDAndInternalEventQueue> fd_event_queue, auto_Object<Log> log, const Time& operation_timeout, auto_Object<SocketAddress> peername, uint8_t reconnect_tries_max, auto_Object<SSLContext> ssl_context );
    virtual ~SocketClient();


    virtual auto_Object<ProtocolRequestType> createProtocolRequest( auto_Object<Request> request ) = 0;
    virtual auto_Object<ProtocolResponseType> createProtocolResponse( auto_Object<ProtocolRequestType> protocol_request ) = 0;

    virtual void respond( auto_Object<ProtocolRequestType> protocol_request, auto_Object<ProtocolResponseType> protocol_response ) = 0;
    virtual void respond( auto_Object<ProtocolRequestType> protocol_request, auto_Object<ExceptionResponse> exception_response ) = 0;

    // EventHandler
    void handleEvent( Event& );

    // EventTarget
    virtual bool send( Event& ev ) { return my_stage->send( ev ); }

  private:
    auto_Object<URI> absolute_uri;
    auto_Object<FDAndInternalEventQueue> fd_event_queue;
    auto_Object<Log> log;
    Time operation_timeout;
    auto_Object<SocketAddress> peername;
    uint8_t reconnect_tries_max;
    auto_Object<SSLContext> ssl_context;

    auto_Object<Stage> my_stage;


    class Connection : public Object
    {
    public:
      enum State { IDLE = 0, CONNECTING = 1, WRITING = 2, READING = 3 };

      Connection( auto_Object<Socket> _socket, uint8_t reconnect_tries_max );

      const Time& get_last_activity_time() const { return last_activity_time; }
      auto_Object<ProtocolRequestType> get_protocol_request() const { return protocol_request; }
      inline State get_state() const { return state; }
      void set_protocol_request( auto_Object<ProtocolRequestType> protocol_request ) { this->protocol_request = protocol_request; }
      auto_Object<ProtocolResponseType> get_protocol_response() const { return protocol_response; }      
      auto_Object<Socket> get_socket() const { return _socket; }
      uint8_t get_reconnect_tries_left() const { return reconnect_tries_left; }
      void set_protocol_response( auto_Object<ProtocolResponseType> protocol_response ) { this->protocol_response = protocol_response; }
      void set_reconnect_tries_left( uint8_t reconnect_tries_left ) { this->reconnect_tries_left = reconnect_tries_left; }
      inline void set_state( State state ) { this->state = state; }      
      void touch() { this->last_activity_time = Time(); }       

      bool operator==( const Connection& other ) const { return _socket == other._socket; }

      // Object
      YIELD_OBJECT_PROTOTYPES( Connection, 201 );

    private:
      ~Connection() { }

      auto_Object<Socket> _socket;
      uint8_t reconnect_tries_left;

      State state;
      Time last_activity_time;

      auto_Object<ProtocolRequestType> protocol_request;
      auto_Object<ProtocolResponseType> protocol_response;
    };


    auto_Object<Connection> createConnection();
    void recreateConnection( auto_Object<Connection> );
    std::vector<Connection*> connections;
  };


  class SocketServer : public EventHandler
  {
  public:
    // EventHandler
    void handleEvent( Event& );

  protected:
    SocketServer( auto_Object<Stage> protocol_request_reader_stage ) 
      : protocol_request_reader_stage( protocol_request_reader_stage )
    { }


    template <class ProtocolRequestType>
    class ProtocolRequestReader : public EventHandler
    {
    public:
      // Object
      YIELD_OBJECT_PROTOTYPES( ProtocolRequestReader<ProtocolRequestType>, 0 );    

      // EventHandler
      void handleEvent( Event& );

    protected:      
      ProtocolRequestReader( auto_Object<FDAndInternalEventQueue> fd_event_queue, auto_Object<Log> log )
        : log( log ), fd_event_queue( fd_event_queue )
      { }

      virtual ~ProtocolRequestReader() { }


      auto_Object<Log> log;


      virtual auto_Object<ProtocolRequestType> createProtocolRequest( auto_Object<Socket> _socket ) = 0;
      virtual bool sendProtocolRequest( auto_Object<ProtocolRequestType> protocol_request ) = 0;

    private:
      auto_Object<FDAndInternalEventQueue> fd_event_queue;
    };


    template <class ProtocolResponseType>
    class ProtocolResponseWriter : public EventHandler
    {
    public:
      ProtocolResponseWriter( auto_Object<Log> log = NULL ) 
        : log ( log )
      { }

      void set_protocol_request_reader_stage( auto_Object<Stage> protocol_request_reader_stage ) { this->protocol_request_reader_stage = protocol_request_reader_stage; }

      // Object
      YIELD_OBJECT_PROTOTYPES( ProtocolResponseWriter<ProtocolResponseType>, 0 );

      // EventHandler
      void handleEvent( Event& );

    protected:
      virtual ~ProtocolResponseWriter() { }

      auto_Object<Log> log;

    private:
      auto_Object<Stage> protocol_request_reader_stage;
    };


  private:
    auto_Object<Stage> protocol_request_reader_stage;
  };


  class RFC822Headers
  {
  public:
    RFC822Headers();
    virtual ~RFC822Headers();

    ssize_t deserialize( auto_Object<Buffer> );
    char* get_header( const char* header_name, const char* default_value="" );
    char* operator[]( const char* header_name ) { return get_header( header_name ); }
    // void set_header( const char* header, size_t header_len ); // Mutable header with name: value in one string, will copy both
    void set_header( const char* header_name, const char* header_value ); // Literal header
    void set_header( const char* header_name, char* header_value ); // Mutable header, will copy value
    void set_header( char* header_name, char* header_value ); // Mutable name and mutable value, will copy both
    void set_header( const std::string& header_name, const std::string& header_value ); // Mutable name and mutable value, will copy both
    auto_Object<Buffer> serialize();

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
    auto_Object<Buffer> get_body() const { return body; }
    uint8_t get_http_version() const { return http_version; }

  protected:
    HTTPMessage();
    HTTPMessage( auto_Object<Buffer> body );
    virtual ~HTTPMessage() { }


    auto_Object<Buffer> body;

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

    virtual ssize_t deserialize( auto_Object<Buffer> );
    virtual auto_Object<Buffer> serialize();
  };


  class HTTPRequest : public Request, public HTTPMessage
  {
  public:
    HTTPRequest(); // Incoming
    HTTPRequest( const char* method, const char* relative_uri, const char* host, auto_Object<Buffer> body = NULL ); // Outgoing
    HTTPRequest( const char* method, const URI& absolute_uri, auto_Object<Buffer> body = NULL ); // Outgoing

    ssize_t deserialize( auto_Object<Buffer> );
    uint8_t get_http_version() const { return http_version; }
    const char* get_method() const { return method; }
    const char* get_uri() const { return uri; }    
    virtual bool respond( uint16_t status_code );
    virtual bool respond( uint16_t status_code, auto_Object<Buffer> body );
    virtual bool respond( Response& response ) { return Request::respond( response ); }
    auto_Object<Buffer> serialize();

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPRequest, 205 );

  protected:
    virtual ~HTTPRequest();

  private:
    HTTPRequest( const HTTPRequest& ) { DebugBreak(); } // Prevent copying

    void init( const char* method, const char* relative_uri, const char* host, auto_Object<Buffer> body );

    char method[16];
    char* uri; size_t uri_len;
  };


  class HTTPResponse : public Response, public HTTPMessage
  {
  public:
    HTTPResponse(); // Incoming
    HTTPResponse( uint16_t status_code ); // Outgoing
    HTTPResponse( uint16_t status_code, auto_Object<Buffer> body ); // Outgoing

    ssize_t deserialize( auto_Object<Buffer> );
    uint16_t get_status_code() const { return status_code; }
    auto_Object<Buffer> serialize();
    void set_body( auto_Object<Buffer> body ) { this->body = body; }
    void set_status_code( uint16_t status_code ) { this->status_code = status_code; }

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPResponse, 206 );

  protected:
    virtual ~HTTPResponse() { }

  private:
    HTTPResponse( const HTTPResponse& ) { DebugBreak(); } // Prevent copying

    uint8_t http_version;
    union { char status_code_str[4]; uint16_t status_code; };
  };


  class HTTPClient : public EventHandler, public SocketClient<HTTPRequest, HTTPResponse>
  {
  public:
    static auto_Object<HTTPClient> create( const URI& absolute_uri, 
                                           auto_Object<StageGroup> stage_group, 
                                           auto_Object<Log> log = NULL, 
                                           const Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, 
                                           uint8_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT,
                                           auto_Object<SSLContext> ssl_context = NULL );

    static auto_Object<HTTPResponse> GET( const URI& absolute_uri, auto_Object<Log> log = NULL );
    static auto_Object<HTTPResponse> PUT( const URI& absolute_uri, auto_Object<Buffer> body, auto_Object<Log> log = NULL );
    static auto_Object<HTTPResponse> PUT( const URI& absolute_uri, const Path& body_file_path, auto_Object<Log> log = NULL );

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPClient, 207 );

    // EventHandler
    virtual void handleEvent( Event& ev ) { SocketClient<HTTPRequest, HTTPResponse>::handleEvent( ev ); }

    // EventTarget
    virtual bool send( Event& ev ) { return SocketClient<HTTPRequest, HTTPResponse>::send( ev ); }

  private:
    HTTPClient( const URI& absolute_uri, auto_Object<FDAndInternalEventQueue> fd_event_queue, auto_Object<Log> log, const Time& operation_timeout, auto_Object<SocketAddress> peername, uint8_t reconnect_tries_max, auto_Object<SSLContext> ssl_context );
    virtual ~HTTPClient() { }

    static auto_Object<HTTPResponse> sendHTTPRequest( const char* method, const YIELD::URI& uri, auto_Object<Buffer> body, auto_Object<Log> log );

    // Client
    virtual auto_Object<HTTPRequest> createProtocolRequest( auto_Object<Request> request );
    virtual auto_Object<HTTPResponse> createProtocolResponse( auto_Object<HTTPRequest> http_request );
    virtual void respond( auto_Object<HTTPRequest> http_request, auto_Object<HTTPResponse> http_response );
    virtual void respond( auto_Object<HTTPRequest> http_request, auto_Object<ExceptionResponse> exception_response );
  };
    

  class HTTPServer : public SocketServer
  {
  public:
    template <class StageGroupType> 
    static auto_Object<HTTPServer> create( const URI& absolute_uri,
                                           auto_Object<EventTarget> http_request_target, 
                                           auto_Object<StageGroupType> stage_group,                        
                                           auto_Object<Log> log = NULL, 
                                           auto_Object<SSLContext> ssl_context = NULL );

    // Object
    YIELD_OBJECT_PROTOTYPES( HTTPServer, 0 );

  protected:
    HTTPServer( auto_Object<Stage> http_request_reader_stage ) 
      : SocketServer( http_request_reader_stage )
    { }


    class HTTPRequest : public YIELD::HTTPRequest
    {
    public:
      HTTPRequest( auto_Object<Stage> http_response_writer_stage, auto_Object<Socket> tcp_socket )
        : http_response_writer_stage( http_response_writer_stage ), tcp_socket( tcp_socket )
      { }

      auto_Object<Socket> get_socket() const { return tcp_socket; }

      // HTTPRequest
      bool respond( uint16_t status_code )
      {
        return http_response_writer_stage->send( *( new HTTPServer::HTTPResponse( status_code, tcp_socket ) ) );
      }

      bool respond( uint16_t status_code, auto_Object<Buffer> body )
      {
        return http_response_writer_stage->send( *( new HTTPServer::HTTPResponse( status_code, body, tcp_socket ) ) );
      }

      bool respond( Response& ) { DebugBreak(); return false; }

    private:
      auto_Object<Stage> http_response_writer_stage;
      auto_Object<Socket> tcp_socket;
    };


    class HTTPRequestReader : public ProtocolRequestReader<HTTPRequest>
    {
    public:
      HTTPRequestReader( auto_Object<FDAndInternalEventQueue> fd_event_queue, auto_Object<EventTarget> http_request_target, auto_Object<Stage> http_response_writer_stage, auto_Object<Log> log )
        : ProtocolRequestReader<HTTPRequest>( fd_event_queue, log ), http_request_target( http_request_target ), http_response_writer_stage( http_response_writer_stage )
      { }

      // Object
      YIELD_OBJECT_PROTOTYPES( HTTPRequestReader, 0 );    

    private:
      ~HTTPRequestReader() { }

      auto_Object<EventTarget> http_request_target;
      auto_Object<Stage> http_response_writer_stage;

      // ProtocolRequestReader
      auto_Object<HTTPRequest> createProtocolRequest( auto_Object<Socket> _socket )
      {
        return new HTTPRequest( http_response_writer_stage, _socket );
      }

      bool sendProtocolRequest( auto_Object<HTTPRequest> http_request )
      {
        return http_request_target->send( *http_request.release() );
      }
    };


    class HTTPResponse : public YIELD::HTTPResponse
    {
    public:
      HTTPResponse( uint16_t status_code, auto_Object<Socket> _socket )
        : YIELD::HTTPResponse( status_code ), tcp_socket( tcp_socket )
      { }

      HTTPResponse( uint16_t status_code, auto_Object<Buffer> body, auto_Object<Socket> tcp_socket )
        : YIELD::HTTPResponse( status_code, body ), tcp_socket( tcp_socket )
      { }

      auto_Object<Socket> get_socket() const { return tcp_socket; }

    private:
      ~HTTPResponse() { }

      auto_Object<Socket> tcp_socket;
    };


    typedef ProtocolResponseWriter<HTTPResponse> HTTPResponseWriter;
  };


  class JSONValue;

  class JSONMarshaller : public Marshaller
  {
  public:
    JSONMarshaller( bool write_empty_strings = true );
    virtual ~JSONMarshaller(); // If the stream is wrapped in map, sequence, etc. then the constructor will append the final } or [, so the underlying output stream should not be deleted before this object!

    // Marshaller
    YIELD_MARSHALLER_PROTOTYPES;

    // Object
    YIELD_OBJECT_PROTOTYPES( JSONMarshaller, 0 );

  protected:
    JSONMarshaller( JSONMarshaller& parent_json_marshaller, const Declaration& root_decl );

    virtual void write( const Declaration& );
    virtual void write( const Map* ); // Can be NULL for empty maps
    virtual void write( const Object* ); // Can be NULL for empty maps
    virtual void write( const Sequence* ); // Can be NULL for empty sequences

  private:
    bool write_empty_strings;

    bool in_map;
    const Declaration* root_decl; // Mostly for debugging, also used to indicate if this is the root JSONMarshaller
    yajl_gen writer;

    void flushYAJLBuffer();
  };


  class JSONUnmarshaller : public Unmarshaller
  {
  public:
    JSONUnmarshaller( auto_Object<Buffer> source_buffer );
    virtual ~JSONUnmarshaller();

    // Unmarshaller
    YIELD_UNMARSHALLER_PROTOTYPES;

    // Object
    YIELD_OBJECT_PROTOTYPES( JSONUnmarshaller, 0 );

  protected:
    JSONUnmarshaller( const Declaration& root_decl, JSONValue& root_json_value );

  private:
    const Declaration* root_decl;
    JSONValue *root_json_value, *next_json_value;

    void readMap( Map& );
    void readSequence( Sequence& );
    void readStruct( Object& );
    JSONValue* readJSONValue( const Declaration& decl );
  };


  template <class ONCRPCMessageType> // CRTP
  class ONCRPCMessage
  {
  public:
    auto_Object<> get_body() const { return body; }
    uint32_t get_xid() const { return xid; }

    ssize_t deserialize( auto_Object<Buffer> );
    auto_Object<Buffer> serialize();

  protected:
    ONCRPCMessage( uint32_t xid, auto_Object<Interface> _interface, auto_Object<> body );
    virtual ~ONCRPCMessage();

    auto_Object<> body;
    auto_Object<Interface> _interface;

  private:
    enum 
    { 
      DESERIALIZING_RECORD_FRAGMENT_MARKER, 
      DESERIALIZING_RECORD_FRAGMENT, 
      DESERIALIZING_LONG_RECORD_FRAGMENT, 
      DESERIALIZE_DONE 
    } deserialize_state;

    uint32_t record_fragment_length;
    std::string record_fragment_string;
    uint32_t xid;
  };


  class ONCRPCRequest : public Request, public ONCRPCMessage<ONCRPCRequest>
  {
  public:
    const static uint32_t AUTH_NONE = 0;

    ONCRPCRequest( auto_Object<Interface> _interface ); // Incoming
    ONCRPCRequest( uint32_t prog, uint32_t proc, uint32_t vers, auto_Object<> body ); // Outgoing
    ONCRPCRequest( uint32_t prog, uint32_t proc, uint32_t vers, uint32_t credential_auth_flavor, auto_Object<> credential, auto_Object<> body ); // Outgoing

    uint32_t get_credential_auth_flavor() const { return credential_auth_flavor; }
    auto_Object<> get_credential() const { return credential; }
    uint32_t get_prog() const { return prog; }
    uint32_t get_proc() const { return proc; }
    uint32_t get_vers() const { return vers; }
    void set_credential_auth_flavor( uint32_t credential_auth_flavor ) { this->credential_auth_flavor = credential_auth_flavor; }
    void set_credential( auto_Object<> credential ) { this->credential = credential; }

    // Object
    YIELD_OBJECT_PROTOTYPES( ONCRPCRequest, 213 );    

  protected:
    virtual ~ONCRPCRequest() { }

  private:
    template <class> friend class ONCRPCMessage;

    // ONCRPCMessage
    void deserializeONCRPCRequestResponseHeader( XDRUnmarshaller& );
    void serializeONCRPCRequestResponseHeader( XDRMarshaller& );


    uint32_t prog, proc, vers, credential_auth_flavor;
    auto_Object<> credential;
  };


  class ONCRPCResponse : public Response, public ONCRPCMessage<ONCRPCResponse>
  {
  public:
    ONCRPCResponse( auto_Object<Interface> _interface, auto_Object<> body ); // Incoming
    ONCRPCResponse( uint32_t xid, auto_Object<> body ); // Outgoing

    // Object
    YIELD_OBJECT_PROTOTYPES( ONCRPCResponse, 208 );

  protected:
    virtual ~ONCRPCResponse() { }

  private:
    template <class> friend class ONCRPCMessage;

    // ONCRPCMessage
    void deserializeONCRPCRequestResponseHeader( XDRUnmarshaller& );
    void serializeONCRPCRequestResponseHeader( XDRMarshaller& );
  };


  template <class InterfaceType>
  class ONCRPCClient : public InterfaceType, public SocketClient<ONCRPCRequest, ONCRPCResponse>
  {
  public:
    template <class ONCRPCClientType>
    static auto_Object<ONCRPCClientType> create( const URI& absolute_uri, 
                                                 auto_Object<StageGroup> stage_group, 
                                                 auto_Object<Log> log = NULL, 
                                                 const Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, 
                                                 uint8_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT,
                                                 auto_Object<SSLContext> ssl_context = NULL );
    // EventHandler
    virtual void handleEvent( Event& ev ) { SocketClient<ONCRPCRequest, ONCRPCResponse>::handleEvent( ev ); }

    // EventTarget
    virtual bool send( Event& ev ) { return SocketClient<ONCRPCRequest, ONCRPCResponse>::send( ev ); }

  protected:
    ONCRPCClient( const URI& absolute_uri, auto_Object<FDAndInternalEventQueue> fd_event_queue, auto_Object<Log> log, const Time& operation_timeout, auto_Object<SocketAddress> peername, uint8_t reconnect_tries_max, auto_Object<SSLContext> ssl_context )
      : SocketClient<ONCRPCRequest, ONCRPCResponse>( absolute_uri, fd_event_queue, log, operation_timeout, peername, reconnect_tries_max, ssl_context )
    { }

    virtual ~ONCRPCClient() { }

    // Client
    virtual auto_Object<ONCRPCRequest> createProtocolRequest( auto_Object<Request> request )
    {
      if ( request->get_tag() == YIELD_OBJECT_TAG( ONCRPCRequest ) )
        return static_cast<ONCRPCRequest*>( request.release() );
      else
        return new ONCRPCRequest( 0x20000000 + InterfaceType::get_tag(), request->get_tag(), InterfaceType::get_tag(), request.release() );
    }

    virtual auto_Object<ONCRPCResponse> createProtocolResponse( auto_Object<ONCRPCRequest> oncrpc_request )
    {
      auto_Object<Response> response = InterfaceType::createResponse( static_cast<Request*>( oncrpc_request->get_body().get() )->get_tag() );
      if ( response != NULL )
        return new ONCRPCResponse( this->incRef(), response.release() );
      else
        return NULL;
    }

    virtual void respond( auto_Object<ONCRPCRequest> oncrpc_request, auto_Object<ONCRPCResponse> oncrpc_response )
    {
      static_cast<Request*>( oncrpc_request->get_body().get() )->respond( static_cast<Response*>( oncrpc_response->get_body().get() )->incRef() );
    }

    virtual void respond( auto_Object<ONCRPCRequest> oncrpc_request, auto_Object<ExceptionResponse> exception_response )
    {
      static_cast<Request*>( oncrpc_request->get_body().get() )->respond( *exception_response.release() );
    }
  };

  
  class ONCRPCServer : public SocketServer
  {
  public:
    static auto_Object<ONCRPCServer> create( const URI& absolute_uri,
                                             auto_Object<Interface> _interface,
                                             auto_Object<StageGroup> stage_group, 
                                             auto_Object<Log> log = NULL, 
                                             auto_Object<SSLContext> ssl_context = NULL );

    // Object
    YIELD_OBJECT_PROTOTYPES( ONCRPCServer, 0 );
   
  protected:
    ONCRPCServer( auto_Object<Stage> oncrpc_request_reader_stage ) 
      : SocketServer( oncrpc_request_reader_stage )
    { }


    class ONCRPCRequest : public YIELD::ONCRPCRequest
    {
    public:
      ONCRPCRequest( auto_Object<Interface> _interface, auto_Object<Socket> _socket )
        : YIELD::ONCRPCRequest( _interface ), _socket( _socket )
      { }

      auto_Object<Socket> get_socket() const { return _socket; }

    private:
      auto_Object<Socket> _socket;
    };


    class ONCRPCResponse : public YIELD::ONCRPCResponse
    {
    public:
      ONCRPCResponse( uint32_t xid, auto_Object<> body, auto_Object<Socket> _socket )
        : YIELD::ONCRPCResponse( xid, body ), _socket( _socket )
      { }

      auto_Object<Socket> get_socket() const { return _socket; }

    private:
      ~ONCRPCResponse() { }

      auto_Object<Socket> _socket;
    };


    class ONCRPCResponder : public EventTarget
    {
    public:
      ONCRPCResponder( auto_Object<ONCRPCRequest> oncrpc_request, auto_Object<Stage> oncrpc_response_writer_stage )
        : oncrpc_request( oncrpc_request ), oncrpc_response_writer_stage( oncrpc_response_writer_stage )
      { }

      // Object    
      YIELD_OBJECT_PROTOTYPES( ONCRPCResponder, 0 );

      // EventTarget
      bool send( Event& ev )
      {
        ONCRPCResponse* oncrpc_response = new ONCRPCResponse( oncrpc_request->get_xid(), ev, oncrpc_request->get_socket() );
        return oncrpc_response_writer_stage->send( *oncrpc_response );
      }

    private:
      auto_Object<ONCRPCRequest> oncrpc_request;
      auto_Object<Stage> oncrpc_response_writer_stage;
    };


    class ONCRPCRequestReader : public ProtocolRequestReader<ONCRPCRequest>
    {
    public:
      ONCRPCRequestReader( auto_Object<FDAndInternalEventQueue> fd_event_queue, auto_Object<Interface> _interface, auto_Object<Log> log, auto_Object<Stage> oncrpc_response_writer_stage )
        : ProtocolRequestReader<ONCRPCRequest>( fd_event_queue, log ), _interface( _interface ), oncrpc_response_writer_stage( oncrpc_response_writer_stage )
      { }

    private:
      ~ONCRPCRequestReader() { }

      auto_Object<Interface> _interface;
      auto_Object<Stage> oncrpc_response_writer_stage;

      // ProtocolRequestReader
      auto_Object<ONCRPCRequest> createProtocolRequest( auto_Object<Socket> _socket )
      {
        return new ONCRPCRequest( _interface, _socket );
      }

      bool sendProtocolRequest( auto_Object<ONCRPCRequest> oncrpc_request )
      {
        Request* body = static_cast<Request*>( oncrpc_request->get_body().release() ); // Dangerous
        body->set_response_target( new ONCRPCResponder( oncrpc_request, oncrpc_response_writer_stage ) );
        return _interface->send( *body );
      }
    };


    typedef ProtocolResponseWriter<ONCRPCResponse> ONCRPCResponseWriter;
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


  class Socket : public Event
  {
  public:
    int get_domain() const { return domain; }
    int get_protocol() const { return protocol; }
    int get_type() const { return type; }
    bool operator==( const Socket& other ) const { return static_cast<int>( *this ) == static_cast<int>( other ); } \
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
    int _socket;

    bool blocking_mode;
  };


  class TCPSocket : public Socket
  {
  public:
    static auto_Object<TCPSocket> create(); // Defaults to domain = AF_INET6

    virtual auto_Object<TCPSocket> accept();
    virtual bool listen();
    virtual bool shutdown();

    // Object
    YIELD_OBJECT_PROTOTYPES( TCPSocket, 212 );

  protected:
    TCPSocket( int domain, int _socket ); // Accepted socket
    virtual ~TCPSocket() { }

    int _accept();

  private:
    size_t partial_write_len;
  };


  class TCPListenQueue : public FDEventQueue
  {
  public:
    static auto_Object<TCPListenQueue> create( auto_Object<SocketAddress> sockname );
 
    // Object
    YIELD_OBJECT_PROTOTYPES( TCPListenQueue, 0 );

    // EventQueue
    bool enqueue( Event& );
    Event* dequeue();
    Event* dequeue( uint64_t );
    Event* try_dequeue() { return dequeue( 0 ); }

  protected:
    TCPListenQueue( auto_Object<TCPSocket> listen_tcp_socket );
    ~TCPListenQueue() { }

  private:
    auto_Object<TCPSocket> listen_tcp_socket;
  };


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


#ifdef YIELD_HAVE_OPENSSL

  class SSLSocket : public TCPSocket
  {
  public:
    static auto_Object<SSLSocket> create( auto_Object<SSLContext> ctx );

    // Object
    YIELD_OBJECT_PROTOTYPES( SSLSocket, 216 );

    // Socket
    virtual ssize_t recv( void* buffer, size_t buffer_len );
    virtual ssize_t send( const void* buffer, size_t buffer_len );
    virtual ssize_t sendmsg( const struct iovec* buffers, uint32_t buffers_count );
    virtual bool want_read() const;
    virtual bool want_write() const;

    // TCPSocket
    auto_Object<TCPSocket> accept();
    bool connect( auto_Object<SocketAddress> peername );
    bool shutdown();

  private:
    SSLSocket( int domain, int _socket, auto_Object<SSLContext> ctx, SSL& ssl );
    ~SSLSocket();

//    static void info_callback( const SSL* ssl, int where, int ret );


    auto_Object<SSLContext> ctx;
    SSL* ssl;
  };


  class SSLListenQueue : public TCPListenQueue
  {
  public:
    static auto_Object<SSLListenQueue> create( auto_Object<SocketAddress> sockname, auto_Object<SSLContext> ssl_context );
 
    // Object
    YIELD_OBJECT_PROTOTYPES( SSLListenQueue, 0 );

  private:
    SSLListenQueue( auto_Object<SSLSocket> listen_ssl_socket );
    ~SSLListenQueue() { }
  };

#endif

  class TracingSocket : public Socket
  {
  public:
    TracingSocket( auto_Object<Socket> underlying_socket, auto_Object<Log> log );
  
    // Object
    virtual uint32_t get_tag() const { return underlying_socket->get_tag(); }
    const char* get_type_name() const { return underlying_socket->get_type_name(); }

    // Socket
    YIELD_SOCKET_PROTOTYPES;

  private:
    ~TracingSocket() { }

    auto_Object<Socket> underlying_socket;
    auto_Object<Log> log;
  };


  class UDPSocket : public Socket
  {
  public:
    static auto_Object<UDPSocket> create();
    static auto_Object<UDPSocket> create( char* recvfrom_buffer, size_t recvfrom_buffer_len );

    // Object
    YIELD_OBJECT_PROTOTYPES( UDPSocket, 219 );

    // Socket
    virtual ssize_t recv( void* buffer, size_t buffer_len );

  private:
    UDPSocket( int domain, int _socket );
    UDPSocket( int domain, char* recvfrom_buffer, size_t recvfrom_buffer_len, int _socket );    
    ~UDPSocket();

    char* recvfrom_buffer;
    size_t recvfrom_buffer_len, recvfrom_buffer_remaining;
  };


  class UDPRecvFromQueue : public FDEventQueue
  {
  public:
    static auto_Object<UDPRecvFromQueue> create( auto_Object<SocketAddress> sockname );
 
    // Object
    YIELD_OBJECT_PROTOTYPES( UDPRecvFromQueue, 0 );

    // EventQueue
    bool enqueue( Event& );
    Event* dequeue();
    Event* dequeue( uint64_t );
    Event* try_dequeue() { return dequeue( 0 ); }

  private:
    UDPRecvFromQueue( auto_Object<SocketAddress> recvfrom_sockname, auto_Object<UDPSocket> recvfrom_socket );
    ~UDPRecvFromQueue() { }

    auto_Object<SocketAddress> recvfrom_sockname;
    auto_Object<UDPSocket> recvfrom_socket;

    UDPSocket* recvfrom();
  };


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


   template <class InterfaceType>
   template <class ONCRPCClientType>
   auto_Object<ONCRPCClientType> ONCRPCClient<InterfaceType>::create( const URI& absolute_uri, 
                                                                      auto_Object<StageGroup> stage_group, 
                                                                      auto_Object<Log> log, 
                                                                      const Time& operation_timeout, 
                                                                      uint8_t reconnect_tries_max,
                                                                      auto_Object<SSLContext> ssl_context )
   {
     auto_Object<SocketAddress> peername = SocketAddress::create( absolute_uri );
     if ( peername != NULL && peername->get_port() != 0 )
     {
#ifdef YIELD_HAVE_OPENSSL
       if ( absolute_uri.get_scheme() == "oncrpcs" && ssl_context == NULL )
         ssl_context = new SSLContext( SSLv23_client_method() );
#endif
       auto_Object<FDAndInternalEventQueue> fd_event_queue = new FDAndInternalEventQueue;
       auto_Object<ONCRPCClientType> oncrpc_client = new ONCRPCClientType( absolute_uri, fd_event_queue, log, operation_timeout, peername, reconnect_tries_max, ssl_context );
       stage_group->createStage( oncrpc_client->incRef(), 1, fd_event_queue->incRef(), NULL, log );
       return oncrpc_client;
     }

     return NULL;
   }
};

#endif
