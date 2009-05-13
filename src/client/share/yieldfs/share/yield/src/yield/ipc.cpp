// Revision: 1411

#include "yield/ipc.h"
using namespace YIELD;
using std::memset;


// oncrpc_record_input_stream.h
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).




namespace YIELD
{
  class ONCRPCRecordInputStream : public Object, public InputStream
  {
  public:
    class ReadException : public YIELD::Exception
    {
    public:
      ReadException( Stream::Status status )
        : status( status )
      { }

      ReadException( const char* what )
        : YIELD::Exception( what ), status( Stream::STREAM_STATUS_ERROR )
      { }

      Stream::Status get_status() const { return status; }

    private:
      Stream::Status status;
    };


    ONCRPCRecordInputStream( InputStream& underlying_input_stream )
      : underlying_input_stream( underlying_input_stream )
    {
      current_record_fragment = NULL;
    }

    virtual ~ONCRPCRecordInputStream()
    {
      delete current_record_fragment;
    }

    InputStream& get_underlying_input_stream() const { return underlying_input_stream; }

    // InputStream
    Stream::Status read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
    {
      for ( ;; )
      {
        if ( current_record_fragment == NULL )
        {
          uint32_t record_fragment_marker = 0;
          size_t bytes_read;
          Stream::Status read_status = underlying_input_stream.read( &record_fragment_marker, sizeof( record_fragment_marker ), &bytes_read );
          if ( read_status == STREAM_STATUS_OK )
          {
  #ifdef _DEBUG
            if ( bytes_read != sizeof( record_fragment_marker ) )
              DebugBreak();
  #endif

  #ifdef __MACH__
            record_fragment_marker = ntohl( record_fragment_marker );
  #else
            record_fragment_marker = Machine::ntohl( record_fragment_marker );
  #endif
            bool last_record_fragment;
            uint32_t record_fragment_length;
            if ( record_fragment_marker & ( 1 << 31UL ) )
            {
              last_record_fragment = true;
              record_fragment_length = record_fragment_marker ^ ( 1 << 31UL );
            }
            else
            {
              last_record_fragment = false;
              record_fragment_length = record_fragment_marker;
            }

            if ( record_fragment_length < 32 * 1024 * 1024 )
              current_record_fragment = new ONCRPCRecordFragment( record_fragment_length );
            else
              throw ReadException( "ONCRPCRequest: record_fragment_length exceeds max" );
          }
          else
            throw ReadException( read_status );
        }

        if ( !current_record_fragment->is_complete() )
          current_record_fragment->deserialize( underlying_input_stream );

        Stream::Status read_status = current_record_fragment->read( buffer, buffer_len, out_bytes_read );
        if ( read_status == STREAM_STATUS_WANT_READ ) // We hit the end of this record fragment, start a new one
        {
          delete current_record_fragment;
          current_record_fragment = NULL;
          continue;
        }
        else
          return read_status;
      }
    }

  private:
    InputStream& underlying_input_stream;


    class ONCRPCRecordFragment : public InputStream
    {
    public:
      ONCRPCRecordFragment( size_t length )
        : length( length )
      {
        data = new char[length];
        data_deserialize_p = data_consumed_p = data;
      }

      ~ONCRPCRecordFragment()
      {
        delete [] data;
      }

      Stream::Status deserialize( InputStream& underlying_input_stream )
      {
        size_t target_read_len = length - ( data_deserialize_p - data );

        do
        {
          size_t read_len;
          Stream::Status read_status = underlying_input_stream.read( data_deserialize_p, target_read_len, &read_len );
          if ( read_status == STREAM_STATUS_OK )
          {
            data_deserialize_p += read_len ;
            target_read_len -= read_len;
          }
          else
            throw ReadException( read_status );
        }
        while ( target_read_len > 0 );

        return STREAM_STATUS_OK;
      }

      inline bool is_complete() const
      {
        return static_cast<size_t>( data_deserialize_p - data ) == length;
      }

      // InputStream
      Stream::Status read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
      {
        if ( data_consumed_p < data_deserialize_p )
        {
#ifdef _DEBUG
          if ( buffer_len > static_cast<size_t>( data_deserialize_p - data_consumed_p ) )
            DebugBreak();
#endif

          memcpy( buffer, data_consumed_p, buffer_len );
          data_consumed_p += buffer_len;

          if ( out_bytes_read )
            *out_bytes_read = buffer_len;

          return STREAM_STATUS_OK;
        }
        else
          return STREAM_STATUS_WANT_READ;
      }

    private:
      size_t length;

      char *data, *data_deserialize_p, *data_consumed_p;
    };


    ONCRPCRecordFragment* current_record_fragment;
  };
};


// oncrpc_record_output_stream.h
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).




namespace YIELD
{
  class ONCRPCRecordOutputStream : public String
  {
  public:
    ONCRPCRecordOutputStream()
    {
      resize( 4 ); // Leave space for the record marker
    }

    void freeze()
    {
      uint32_t record_marker = static_cast<uint32_t>( size() - sizeof( record_marker ) );
      record_marker |= ( 1 << 31 ); // Indicate that this is the last fragment
#ifdef __MACH__
      record_marker = htonl( record_marker );
#else
      record_marker = Machine::htonl( record_marker );
#endif
      replace( 0, sizeof( record_marker ), ( const char* )&record_marker, sizeof( record_marker ) );
    }
  };
};


// client.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#include <ws2tcpip.h>
#define ETIMEDOUT WSAETIMEDOUT
#endif
Client::Client( const SocketAddress& peer_sockaddr, auto_Object<SocketFactory> socket_factory, auto_Object<Log> log )
  : peer_sockaddr( peer_sockaddr ), socket_factory( socket_factory ), log( log )
{
  if ( this->socket_factory == NULL )
    this->socket_factory = new TCPSocketFactory;
  reconnect_tries = 0;
  reconnect_tries_max = static_cast<uint8_t>( -1 );
  operation_timeout_ns = 5 * NS_IN_S;
  fd_event_queue = NULL;
//  connection_activity_timer = new ConnectionActivityCheckTimer( *this );
}
Client::~Client()
{
  for ( std::vector<Connection*>::iterator connection_i = connections.begin(); connection_i != connections.end(); connection_i++ )
    Object::decRef( **connection_i );
}
Client::Connection* Client::createConnection()
{
  auto_Object<Socket> _socket = socket_factory->createSocket().release();
  _socket->set_blocking_mode( false );
  return new Connection( _socket );
}
void Client::destroyConnection( Client::Connection& connection )
{
  connection.shutdown();
  connection.close();
  YIELD::Object::decRef( connection );
}
void Client::handleEvent( Event& ev )
{
  Connection* connection = NULL;
  uint32_t fd_event_error_code = 0;
  switch ( ev.get_type_id() )
  {
    case YIELD_OBJECT_TYPE_ID( StageStartupEvent ):
    {
      fd_event_queue = static_cast<FDAndInternalEventQueue*>( &static_cast<StageStartupEvent&>( ev ).get_stage().get_event_queue() );
      Object::decRef( ev );
      return;
    }
    break;
    case YIELD_OBJECT_TYPE_ID( StageShutdownEvent ):
    {
      Object::decRef( ev );
      return;
    }
    break;
    case YIELD_OBJECT_TYPE_ID( FDEvent ):
    {
      FDEvent& fd_ev = static_cast<FDEvent&>( ev );
      switch ( fd_ev.get_context()->get_general_type() )
      {
        case YIELD::Object::UNKNOWN:
        {
          connection = static_cast<Connection*>( fd_ev.get_context() );
          fd_event_error_code = fd_ev.get_error_code();
        }
        break;
        default: YIELD::DebugBreak();
      }
    }
    break;
    case YIELD_OBJECT_TYPE_ID( ConnectionActivityCheckRequest ):
    {
      for ( std::vector<Connection*>::iterator connection_i = connections.begin(); connection_i != connections.end(); )
      {
        Connection* connection = *connection_i;
        if ( connection->get_state() != Connection::IDLE )
        {
          Time connection_idle_time = Time() - connection->get_last_activity_time();
          if ( connection_idle_time > operation_timeout_ns )
          {
            if ( log != NULL )
              log->getStream( YIELD::Log::LOG_ERR ) << getEventHandlerName() << ": connection to " << peer_sockaddr << " exceeded idle timeout (idle for " << connection_idle_time.as_unix_time_s() << " seconds, last activity at " << connection->get_last_activity_time() << "), dropping.";
            auto_Object<Request> protocol_request = connection->get_protocol_request();
            if ( protocol_request != NULL )
            {
              if ( log != NULL )
                log->getStream( YIELD::Log::LOG_DEBUG ) << getEventHandlerName() << ": connection timed out while trying to " << ( connection->get_state() == Connection::READING ? "read" : "write" ) << " ONC-RPC request, responding with exception.";
              connection->set_protocol_request( NULL );
              this->send( *protocol_request.release() ); // Re-enqueue the request
            }
            fd_event_queue->detach( *connection, connection );
            connection->shutdown();
            connection->close();
            YIELD::Object::decRef( *connection );
            connection_i = connections.erase( connection_i );
          }
          else
             ++connection_i;
        }
        else
          ++connection_i;
      }
      // Don't decRef ev, it's on the timer's stack
      return;
    }
    break;
    default:
    {
      auto_Object<Request> protocol_request = createProtocolRequest( auto_Object<>( ev ) ); // Give it the original reference to ev
      if ( !connections.empty() )
      {
        for ( std::vector<Connection*>::iterator connection_i = connections.begin(); connection_i != connections.end(); connection_i++ )
        {
          if ( ( *connection_i )->get_state() == Connection::IDLE )
          {
            connection = *connection_i;
            connection->set_state( Connection::WRITING );
            connection->touch(); // Update last_activity_time
            break;
          }
        }
        if ( connection == NULL )
        {
          connection = createConnection();
          connections.push_back( connection );
        }
      }
      else
      {
        connection = createConnection();
        connections.push_back( connection );
      }
      connection->set_protocol_request( protocol_request );
      // Don't attach the socket to fd_event_queue until it's really necessary, in case the socket has to be re-created on a fallback to IPv4
    }
    break;
  }
  for ( ;; )
  {
    if ( fd_event_error_code == 0 )
    {
      Stream::Status read_write_status = Stream::STREAM_STATUS_OK;
      switch ( connection->get_state() )
      {
        case Connection::IDLE:
        case Connection::CONNECTING:
        {
          if ( log != NULL )
            log->getStream( YIELD::Log::LOG_DEBUG ) << getEventHandlerName() << ": trying to connect to " << peer_sockaddr << ", attempt #" << static_cast<uint32_t>( reconnect_tries ) << ".";
          Stream::Status connect_status = connection->connect( peer_sockaddr );
          if ( connect_status == Stream::STREAM_STATUS_OK )
          {
            if ( log != NULL )
              log->getStream( YIELD::Log::LOG_INFO ) << getEventHandlerName() << ": successfully connected to " << peer_sockaddr << ".";
            reconnect_tries = 0; // Start from 0 tries every time we successfully connect so that the count doesn't reach the max in a long-running client
            fd_event_queue->detach( *connection, connection );
            connection->set_state( Connection::WRITING );
            // Drop down to WRITING
      	  }
          else if ( connect_status == Stream::STREAM_STATUS_WANT_WRITE ) // Wait for non-blocking connect() to complete
          {
            if ( log != NULL )
              log->getStream( YIELD::Log::LOG_DEBUG ) << getEventHandlerName() << ": waiting for non-blocking connect() to " << peer_sockaddr << " to complete.";
            fd_event_queue->attach( *connection, connection, false, true );
            connection->set_state( Connection::CONNECTING );
            return;
          }
          else if ( log != NULL )
          {
            log->getStream( YIELD::Log::LOG_ERR ) << getEventHandlerName() << ": connection attempt #" << static_cast<uint32_t>( reconnect_tries ) << " to  " << peer_sockaddr << " failed: " << Exception::strerror();
            break; // out of the switch, to check error codes
          }
        }
        // No break here, to allow drop down to WRITING
        case Connection::WRITING:
        {
          auto_Object<Request> protocol_request = connection->get_protocol_request();
          read_write_status = protocol_request->serialize( *connection );
          if ( read_write_status == Stream::STREAM_STATUS_OK )
          {
            if ( log != NULL )
              log->getStream( YIELD::Log::LOG_INFO ) << getEventHandlerName() << ": successfully wrote " << protocol_request->get_type_name() << " to " << peer_sockaddr << ".";
            connection->set_protocol_response( createProtocolResponse() );
            fd_event_queue->detach( *connection, connection );
            connection->set_state( Connection::READING );
            // Drop down to READING
          }
          else
            break; // out of the switch, to check error codes
        }
        // No break here, to allow drop down to READING
        case Connection::READING:
        {
          auto_Object<Response> protocol_response = connection->get_protocol_response();
          if ( log != NULL )
            log->getStream( YIELD::Log::LOG_DEBUG ) << getEventHandlerName() << ": trying to read " << protocol_response->get_type_name() << " from " << peer_sockaddr << ".";
          read_write_status = protocol_response->deserialize( *connection );
          if ( read_write_status == Stream::STREAM_STATUS_OK )
          {
            respond( connection->get_protocol_request(), connection->get_protocol_response() );
            connection->set_protocol_request( NULL );
            connection->set_protocol_response( NULL );
            fd_event_queue->detach( *connection, connection );
            connection->set_state( Connection::IDLE );
            return; // Done
          }
          else
            break; // out of the switch, to check error codes
        }
        break;
        default: DebugBreak();
      }
      // Read or write failed
      if ( read_write_status == Stream::STREAM_STATUS_WANT_READ || read_write_status == Stream::STREAM_STATUS_WANT_WRITE )
      {
        if ( !fd_event_queue->attach( *connection, connection, read_write_status == Stream::STREAM_STATUS_WANT_READ, read_write_status == Stream::STREAM_STATUS_WANT_WRITE ) )
        {
          if ( !fd_event_queue->toggle( *connection, connection, read_write_status == Stream::STREAM_STATUS_WANT_READ, read_write_status == Stream::STREAM_STATUS_WANT_WRITE ) )
            DebugBreak();
        }
        return;
      }
      else if ( log != NULL )
        log->getStream( YIELD::Log::LOG_ERR ) << getEventHandlerName() << ": lost connection to " << peer_sockaddr << " on " << ( connection->get_state() == Connection::READING ? "read" : "write" ) << ", error: " << Exception::strerror();
        // Drop down
    }
    else if ( log != NULL ) // fd_event_error_code != 0
      log->getStream( YIELD::Log::LOG_ERR ) << getEventHandlerName() << ": connection attempt #" << static_cast<uint32_t>( reconnect_tries ) << " to " << peer_sockaddr << " failed: " << Exception::strerror( fd_event_error_code );
      // Drop down
    // Clean up the connection and prepare for another try if we're under the reconnect limit
    for ( std::vector<Connection*>::iterator connection_i = connections.begin(); connection_i != connections.end(); connection_i++ )
    {
      if ( **connection_i == *connection )
      {
        auto_Object<Request> protocol_request = connection->get_protocol_request();
        if ( ++reconnect_tries < reconnect_tries_max )
        {
          destroyConnection( *connection );
          connection = createConnection();
          *connection_i = connection; // Don't erase and push_back from connections, just re-use the same position
          connection->set_protocol_request( protocol_request );
          fd_event_queue->attach( *connection, connection, false, false );
          fd_event_error_code = 0;
          break; // Out of the for connections loop, back up to the main loop to retry the connection
        }
        else
        {
          if ( log != NULL )
            log->getStream( YIELD::Log::LOG_ERR ) << getEventHandlerName() << ": exhausted connection retries to " << peer_sockaddr << ".";
          destroyConnection( *connection );
          connections.erase( connection_i );
          // We've lost errno here
#ifdef _WIN32
          respond( protocol_request, new ExceptionResponse( "exhausted connection retries" ) );
#else
          respond( protocol_request, new ExceptionResponse( "exhausted connection retries" ) );
#endif
          return;
        }
      }
    }
  }
}
Client::Connection::Connection( auto_Object<Socket> _socket )
  : _socket( _socket )
{
  state = IDLE;
}
Stream::Status Client::Connection::connect( const SocketAddress& connect_to_sockaddr )
{
  last_activity_time = Time();
  return _socket->connect( connect_to_sockaddr );
}
Stream::Status Client::Connection::read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
{
  last_activity_time = Time();
  return _socket->read( buffer, buffer_len, out_bytes_read );
}
Stream::Status Client::Connection::writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
{
  last_activity_time = Time();
  return _socket->writev( buffers, buffers_count, out_bytes_written );
}
Client::ConnectionActivityCheckTimer::ConnectionActivityCheckTimer( Client& client )
  : Timer( 5 * NS_IN_S, 5 * NS_IN_S ), client( client )
{ }
void Client::ConnectionActivityCheckTimer::fire()
{
  client.send( stack_connection_activity_check_request );
}


// fd_and_internal_event_queue.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
FDAndInternalEventQueue::FDAndInternalEventQueue()
{
  dequeue_blocked = true;
}
bool FDAndInternalEventQueue::enqueue( Event& ev )
{
  bool result = NonBlockingFiniteQueue<Event*, 2048>::enqueue( &ev );
  if ( dequeue_blocked )
    FDEventQueue::break_blocking_dequeue();
  return result;
}
Event* FDAndInternalEventQueue::dequeue()
{
  dequeue_blocked = true; // Induces extra signals, but avoids this race:
// D: dequeue from NBFQ, NULL
// E: enqueue to NBFQ
// E: check blocked, = false, no signal
// D: set blocked = true, block with event in NBFQ but no signal
  Event* ev = NonBlockingFiniteQueue<Event*, 2048>::try_dequeue();
  if ( ev != NULL )
  {
    dequeue_blocked = false;
    return ev;
  }
  ev = FDEventQueue::dequeue();
  dequeue_blocked = false;
  if ( ev != NULL )
    return ev;
  return NonBlockingFiniteQueue<Event*, 2048>::try_dequeue();
}
Event* FDAndInternalEventQueue::try_dequeue()
{
  Event* ev = NonBlockingFiniteQueue<Event*, 2048>::try_dequeue();
  if ( ev != NULL )
    return ev;
  ev = FDEventQueue::try_dequeue();
  if ( ev )
    return ev;
  return NonBlockingFiniteQueue<Event*, 2048>::try_dequeue();
}
Event* FDAndInternalEventQueue::timed_dequeue( timeout_ns_t timeout_ns )
{
  dequeue_blocked = true;
  Event* ev = NonBlockingFiniteQueue<Event*, 2048>::try_dequeue();
  if ( ev != NULL )
  {
    dequeue_blocked = false;
    return ev;
  }
  ev = FDEventQueue::timed_dequeue( timeout_ns );
  dequeue_blocked = false;
  if ( ev )
    return ev;
  return NonBlockingFiniteQueue<Event*, 2048>::try_dequeue();
}


// fd_event_queue.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#ifndef FD_SETSIZE
#define FD_SETSIZE 1024
#endif
#include <ws2tcpip.h>
#pragma warning( push )
#pragma warning( disable: 4127 ) // Warning in the FD_* macros
#else
#include <errno.h>
#include <unistd.h>
#if defined(YIELD_HAVE_LINUX_EPOLL)
#include <sys/epoll.h>
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
#include <sys/types.h>
#include <sys/event.h>
#include <sys/time.h>
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
#include <port.h>
#include <sys/poll.h>
#else
#include <vector>
#endif
#ifdef YIELD_HAVE_LINUX_EVENTFD
#include <sys/eventfd.h>
#endif
#endif
#include <cstring>
#include <iostream>
#define MAX_EVENTS_PER_POLL 8192
void FDEvent::serialize( StructuredOutputStream& output_stream )
{
  output_stream.writeInt32( "fd", static_cast<int32_t>( get_socket() ) );
  output_stream.writeBool( "want_read", want_read() );
  output_stream.writeUint32( "error_code", static_cast<uint32_t>( get_error_code() ) );
}
FDEventQueue::FDEventQueue()
{
#ifndef YIELD_HAVE_LINUX_EVENTFD
  signal_read_socket = NULL;
#endif
#if defined(YIELD_HAVE_LINUX_EPOLL)
  poll_fd = epoll_create( 32768 );
  returned_events = new epoll_event[MAX_EVENTS_PER_POLL];
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
  poll_fd = kqueue();
  returned_events = new struct kevent[MAX_EVENTS_PER_POLL];
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
  poll_fd = port_create();
  returned_events = new port_event_t[MAX_EVENTS_PER_POLL];
#elif defined(_WIN32)
  read_fds = new fd_set; FD_ZERO( read_fds );
  read_fds_copy = new fd_set; FD_ZERO( read_fds_copy );
  write_fds = new fd_set; FD_ZERO( write_fds );
  write_fds_copy = new fd_set; FD_ZERO( write_fds_copy );
  except_fds = new fd_set; FD_ZERO( except_fds );
  except_fds_copy = new fd_set; FD_ZERO( except_fds_copy );
#endif
  active_fds = 0;
#ifdef YIELD_HAVE_LINUX_EVENTFD
  signal_eventfd = eventfd( 0, 0 );
  if ( signal_eventfd != -1 &&
       attach( signal_eventfd, NULL ) )
      return;
   else
     throw Exception();
#else
  auto_Object<TCPSocket> signal_listen_socket = new TCPSocket;
  if ( !signal_listen_socket->bind( SocketAddress() ) )
    throw Exception();
  if ( !signal_listen_socket->listen() )
    throw Exception();
  signal_write_socket = new TCPSocket;
  signal_write_socket->connect( *signal_listen_socket->getsockname() );
  signal_write_socket->set_blocking_mode( false );
  signal_read_socket = signal_listen_socket->accept();
  signal_read_socket->set_blocking_mode( false );
  if ( !attach( *signal_read_socket, NULL ) )
    throw Exception();
#endif
}
FDEventQueue::~FDEventQueue()
{
#if defined(YIELD_HAVE_LINUX_EPOLL) || defined(YIELD_HAVE_FREEBSD_KQUEUE)
  close( poll_fd );
  delete [] returned_events;
#elif defined(_WIN32)
  delete read_fds; delete read_fds_copy;
  delete write_fds; delete write_fds_copy;
  delete except_fds; delete except_fds_copy;
#endif
#ifdef YIELD_HAVE_LINUX_EVENTFD
  ::close( signal_eventfd );
#endif
}
#ifdef _WIN32
bool FDEventQueue::attach( socket_t fd, Object* context, bool enable_read, bool enable_write )
#else
bool FDEventQueue::attach( fd_t fd, Object* context, bool enable_read, bool enable_write )
#endif
{
#if defined(_WIN32)
  if ( fd_to_context_map.find( fd ) == NULL )
  {
    fd_to_context_map.insert( fd, context );
    if ( enable_read ) FD_SET( fd, read_fds );
    if ( enable_write ) { FD_SET( fd, write_fds ); FD_SET( fd, except_fds ); }
    next_fd_to_check = fd_to_context_map.begin();
    return true;
  }
  else
    return false;
#elif defined(YIELD_HAVE_LINUX_EPOLL)
  struct epoll_event change_event;
  memset( &change_event, 0, sizeof( change_event ) );
#ifdef YIELD_LINUX_EPOLL_RETURN_FD
  change_event.data.fd = fd;
  fd_to_context_map.insert( fd, context );
#else
  change_event.data.ptr = context;
#endif
  change_event.events = 0;
  if ( enable_read ) change_event.events |= EPOLLIN;
  if ( enable_write ) change_event.events |= EPOLLOUT;
  return epoll_ctl( poll_fd, EPOLL_CTL_ADD, fd, &change_event ) != -1;
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
  struct kevent change_events[2];
  EV_SET( &change_events[0], fd, EVFILT_READ, enable_read ? EV_ENABLE : EV_DISABLE, 0, 0, context );
  EV_SET( &change_events[1], fd, EVFILT_WRITE, enable_write ? EV_ENABLE : EV_DISABLE, 0, 0, context );
  return kevent( poll_fd, change_events, 2, 0, 0, NULL ) != -1;
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
  if ( enable_read || enable_write )
  {
    int events = 0;
    if ( enable_read ) events |= POLLIN;
    if ( enable_write ) events |= POLLOUT;
    return port_associate( poll_fd, PORT_SOURCE_FD, fd, events, context ) != -1;
  }
  else
    return true;
#elif !defined(_WIN32)
  std::vector<struct pllfd::size_type pollfd_i_max = pollfds.size();
  for ( std::vector<struct pollfd>::size_type pollfd_i = 0; pollfd_i < pollfd_i_max; pollfd_i++ )
  {
    if ( pollfds[pollfd_i].fd == fd )
      return false;
  }
  struct pollfd attach_pollfd;
  attach_pollfd.fd = fd;
  attach_pollfd.events = 0;
  if ( enable_read ) attach_pollfd.events |= POLLIN;
  if ( enable_write ) attach_pollfd.events |= POLLOUT;
  attach_pollfd.revents = 0;
  pollfds.push_back( attach_pollfd );
  fd_to_context_map.insert( fd, context );
  return true;
#endif
}
void FDEventQueue::break_blocking_dequeue()
{
#ifdef YIELD_HAVE_LINUX_EVENTFD
  uint64_t m = 1;
  ::write( signal_eventfd, &m, sizeof( m ) );
#else
  char m = 'M';
  signal_write_socket->write( &m, sizeof( m ) );
#endif
}
#if defined(YIELD_HAVE_LINUX_EPOLL) || defined(YIELD_HAVE_FREEBSD_KQUEUE) || defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
void FDEventQueue::clearReturnedEvents( fd_t fd, Object* context )
{
  for ( int i = active_fds; i >= 0; i-- )
  {
#if defined(YIELD_HAVE_LINUX_EPOLL)
    if ( returned_events[i].data.ptr == context )
    {
      memset( &returned_events[i].data, 0, sizeof( returned_events[i].data ) );
      active_fds--;
    }
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
    if ( returned_events[i].ident == fd )
    {
      returned_events[i].ident = 0;
      active_fds--;
    }
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
    if ( static_cast<fd_t>( returned_events[i].portev_object ) == fd )
    {
      returned_events[i].portev_object = 0;
      active_fds--;
    }
#endif
  }
}
#endif
void FDEventQueue::clearSignal()
{
#ifdef YIELD_HAVE_LINUX_EVENTFD
  uint64_t m;
  ::read( signal_eventfd, &m, sizeof( m ) );
#else
  char m;
  signal_read_socket->read( reinterpret_cast<char*>( &m ), sizeof( m ) );
#endif
}
Event* FDEventQueue::dequeue()
{
  if ( active_fds <= 0 )
  {
    active_fds = poll();
    if ( active_fds <= 0 )
      return NULL;
  }
  fillStackFDEvent();
  if ( stack_fd_event.get_fd() != 0 )
  {
#ifdef YIELD_HAVE_LINUX_EVENTFD
    if ( stack_fd_event.get_fd() != signal_eventfd )
#else
    if ( stack_fd_event.get_socket() != *signal_read_socket )
#endif
      return &stack_fd_event;
    else
    {
      clearSignal();
#ifdef YIELD_HAVE_SOLARIS_EVENT_PORTS
      port_associate( poll_fd, PORT_SOURCE_FD, *signal_read_socket, POLLIN, NULL ); // Re-associate the signal pipe
#endif
    }
  }
  return NULL;
}
#ifdef _WIN32
void FDEventQueue::detach( socket_t fd, Object* context, bool will_keep_fd_open )
#else
void FDEventQueue::detach( fd_t fd, Object* context, bool will_keep_fd_open )
#endif
{
#if defined(_WIN32)
  FD_CLR( fd, read_fds ); FD_CLR( fd, read_fds_copy );
  FD_CLR( fd, write_fds ); FD_CLR( fd, write_fds_copy );
  FD_CLR( fd, except_fds ); FD_CLR( fd, except_fds_copy );
  fd_to_context_map.remove( fd );
  next_fd_to_check = fd_to_context_map.begin();
  active_fds = 0;
#elif defined(YIELD_HAVE_LINUX_EPOLL)
  clearReturnedEvents( fd, context );
  if ( will_keep_fd_open )
  {
    struct epoll_event change_event;
    memset( &change_event, 0, sizeof( change_event ) );
#ifdef YIELD_LINUX_EPOLL_RETURN_FD
    change_event.data.fd = fd;
#else
    change_event.data.ptr = context;
#endif
    epoll_ctl( poll_fd, EPOLL_CTL_DEL, fd, &change_event );
  }
  // else close() will remove the fd from the polling set
#ifdef YIELD_LINUX_EPOLL_RETURN_FD
  fd_to_context_map.remove( fd );
#endif
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
  clearReturnedEvents( fd, context );
  if ( will_keep_fd_open )
  {
    struct kevent change_events[2];
    EV_SET( &change_events[0], fd, EVFILT_READ, EV_DELETE, 0, 0, context );
    EV_SET( &change_events[1], fd, EVFILT_WRITE, EV_DELETE, 0, 0, context );
    kevent( poll_fd, change_events, 2, 0, 0, NULL );
  }
  // else close() will remove the fd from the polling set
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
  clearReturnedEvents( fd, context );
  port_dissociate( poll_fd, PORT_SOURCE_FD, fd );
#elif !defined(_WIN32)
  if ( fd_to_context_map.remove( fd ) != 0 )
  {
    pollfd_vector::size_type j = ( next_pollfd_to_check > 0 ) ? next_pollfd_to_check - 1 : 0;
    pollfd_vector::size_type j_max = pollfds.size();
    for ( unsigned char i = 0; i < 2; i++ )
    {
      for ( ; j < j_max; j++ )
      {
        if ( pollfds[j].fd == fd )
        {
          if ( pollfds[j].revents != 0 )
            active_fds--;
          pollfds.erase( pollfds.begin() + j );
          return;
        }
      }
      if ( next_pollfd_to_check > 1 )
      {
        j = 0;
        j_max = next_pollfd_to_check - 1;
      }
      else
        return;
    }
  }
#else
  DebugBreak();
#endif
}
bool FDEventQueue::enqueue( Event& ev )
{
#ifdef _DEBUG
  std::cerr << "FDEventQueue: discarding enqueued event " << ev.get_type_name() << "." << std::endl;
#endif
  Object::decRef( ev );
  return true;
}
void FDEventQueue::fillStackFDEvent()
{
#if defined(YIELD_HAVE_LINUX_EPOLL) || defined(YIELD_HAVE_FREEBSD_KQUEUE) || defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
  do
  {
    active_fds--;
#if defined(YIELD_HAVE_LINUX_EPOLL)
    if ( returned_events[active_fds].data.fd != 0  ) // The event hasn't been cleared by clearReturnedEvents
    {
      stack_fd_event.fd = returned_events[active_fds].data.fd;
#ifdef YIELD_LINUX_EPOLL_RETURN_FD
#ifdef YIELD_HAVE_LINUX_EVENTFD
      if ( stack_fd_event.fd != signal_eventfd )
#else
      if ( stack_fd_event.fd != *signal_read_socket )
#endif
        stack_fd_event.context = fd_to_context_map.find( stack_fd_event.fd );
#else
      stack_fd_event.fd = 0;
      stack_fd_event.context = returned_events[active_fds].data.ptr;
#endif
      stack_fd_event._want_read = ( ( returned_events[active_fds].events & EPOLLIN ) == EPOLLIN );
      if ( ( ( returned_events[active_fds].events & EPOLLERR ) != EPOLLERR ) &&
         ( ( returned_events[active_fds].events & EPOLLHUP ) != EPOLLHUP ) )
           stack_fd_event.error_code = 0;
      else
          stack_fd_event.error_code = 1;
      break;
    }
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
    if ( returned_events[active_fds].ident != 0 )
    {
      stack_fd_event.fd = returned_events[active_fds].ident;
      stack_fd_event.context = static_cast<Object*>( returned_events[active_fds].udata );
      stack_fd_event._want_read = ( returned_events[active_fds].filter == EVFILT_READ );
      stack_fd_event.error_code = 0;
      break;
    }
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
    if ( returned_events[active_fds].portev_source == PORT_SOURCE_FD )
    {
      if ( returned_events[active_fds].portev_object != 0 )
      {
        stack_fd_event.fd = returned_events[active_fds].portev_object;
        stack_fd_event.context = static_cast<Object*>( returned_events[active_fds].portev_user );
        stack_fd_event._want_read = ( returned_events[active_fds].portev_events & POLLIN ) || ( returned_events[active_fds].portev_events & POLLRDNORM );
        if ( ( returned_events[active_fds].portev_events & POLLERR ) != POLLERR && ( returned_events[active_fds].portev_events & POLLHUP ) != POLLHUP )
          stack_fd_event.error_code = 0;
        else
          stack_fd_event.error_code = 1;
        memset( &returned_events[active_fds], 0, sizeof( returned_events[active_fds] ) );
      }
    }
#endif
  } while ( active_fds > 0 );
#else
#ifdef _WIN32
  while ( next_fd_to_check != fd_to_context_map.end() )
  {
    socket_t fd = ( socket_t )next_fd_to_check->first;
    if ( FD_ISSET( fd, read_fds_copy ) )
    {
      stack_fd_event._want_read = true;
      FD_CLR( fd, read_fds_copy );
      stack_fd_event.error_code = 0;
    }
    else if ( FD_ISSET( fd, write_fds_copy ) )
    {
      stack_fd_event._want_read = false;
      FD_CLR( fd, write_fds_copy );
      stack_fd_event.error_code = 0;
    }
    else if ( FD_ISSET( fd, except_fds_copy ) )
    {
      stack_fd_event._want_read = false;
      FD_CLR( fd, except_fds_copy );
      int so_error; int so_error_len = sizeof( so_error );
      ::getsockopt( fd, SOL_SOCKET, SO_ERROR, reinterpret_cast<char*>( &so_error ), &so_error_len );
      stack_fd_event.error_code = so_error;
    }
    else
    {
      next_fd_to_check++;
      continue;
    }
    stack_fd_event._socket = fd;
    stack_fd_event.context = fd_to_context_map.find( fd );
    active_fds--;
    break;
  }
  if ( next_fd_to_check == fd_to_context_map.end() )
    active_fds = 0;
#else
  pollfd_vector::size_type pollfds_size = pollfds.size();
  while ( next_pollfd_to_check < pollfds_size )
  {
    if ( pollfds[next_pollfd_to_check].revents != 0 )
    {
      active_fds--;
      stack_fd_event.fd = pollfds[next_pollfd_to_check].fd;
      if ( stack_fd_event.fd != *signal_read_socket )
      {
        int revents = pollfds[next_pollfd_to_check].revents;
        stack_fd_event._want_read = revents & POLLIN;
        if ( ( revents & POLLERR ) != POLLERR && ( revents & POLLHUP ) != POLLHUP )
          stack_fd_event.error_code = 0;
        else
          stack_fd_event.error_code = 1;
        stack_fd_event.context = fd_to_context_map.find( stack_fd_event.fd );
      }
      pollfds[next_pollfd_to_check].revents = 0;
      next_pollfd_to_check++;
      break;
    }
    else
      next_pollfd_to_check++;
  }
  if ( next_pollfd_to_check == pollfds_size )
    active_fds = 0;
#endif
#endif
}
Event* FDEventQueue::timed_dequeue( timeout_ns_t timeout_ns )
{
  if ( active_fds <= 0 )
  {
    active_fds = poll( timeout_ns );
    if ( active_fds <= 0 )
      return NULL;
  }
  fillStackFDEvent();
  if ( stack_fd_event.get_fd() != 0 )
  {
#ifdef YIELD_HAVE_LINUX_EVENTFD
    if ( stack_fd_event.get_fd() != signal_eventfd )
#else
    if ( stack_fd_event.get_socket() != *signal_read_socket )
#endif
      return &stack_fd_event;
    else
    {
      clearSignal();
#ifdef YIELD_HAVE_SOLARIS_EVENT_PORTS
      port_associate( poll_fd, PORT_SOURCE_FD, *signal_read_socket, POLLIN, NULL ); // Re-associate the signal pipe
#endif
    }
  }
  return NULL;
}
int FDEventQueue::poll()
{
#if defined(YIELD_HAVE_LINUX_EPOLL)
  return epoll_wait( poll_fd, returned_events, MAX_EVENTS_PER_POLL, -1 );
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
  return kevent( poll_fd, 0, 0, returned_events, MAX_EVENTS_PER_POLL, NULL );
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
  uint_t nget = 1;
//active_fds = port_getn( poll_fd, returned_events, MAX_EVENTS_PER_POLL, &nget, NULL );
  int active_fds = port_get( poll_fd, returned_events, NULL );
  if ( active_fds == 0 )
    active_fds = ( int )nget;
  return active_fds;
#elif defined(_WIN32)
  memcpy( read_fds_copy, read_fds, sizeof( *read_fds ) );
  memcpy( write_fds_copy, write_fds, sizeof( *write_fds ) );
  memcpy( except_fds_copy, except_fds, sizeof( *except_fds ) );
  next_fd_to_check = fd_to_context_map.begin();
  return select( 0, read_fds_copy, write_fds_copy, except_fds_copy, NULL );
#else
  next_pollfd_to_check = 0;
  return ::poll( &pollfds[0], pollfds.size(), -1 );
#endif
}
int FDEventQueue::poll( timeout_ns_t timeout_ns )
{
#if defined(YIELD_HAVE_LINUX_EPOLL)
    return epoll_wait( poll_fd, returned_events, MAX_EVENTS_PER_POLL, static_cast<int>( timeout_ns / NS_IN_MS ) );
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
    struct timespec poll_tv = Time( timeout_ns );
    return kevent( poll_fd, 0, 0, returned_events, MAX_EVENTS_PER_POLL, &poll_tv );
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
    uint_t nget = 1;
    struct timespec poll_tv = Time( timeout_ns );
//			active_fds = port_getn( poll_fd, returned_events, MAX_EVENTS_PER_POLL, &nget, &poll_tv );
    int active_fds = port_get( poll_fd, returned_events, &poll_tv );
    if ( active_fds == 0 )
      active_fds = ( int )nget;
    return active_fds;
#elif defined(_WIN32)
    memcpy( read_fds_copy, read_fds, sizeof( *read_fds ) );
    memcpy( write_fds_copy, write_fds, sizeof( *write_fds ) );
    memcpy( except_fds_copy, except_fds, sizeof( *except_fds ) );
    next_fd_to_check = fd_to_context_map.begin();
    struct timeval poll_tv = Time( timeout_ns );
    return select( 0, read_fds_copy, write_fds_copy, except_fds_copy, &poll_tv );
#else
    next_pollfd_to_check = 0;
    return ::poll( &pollfds[0], pollfds.size(), static_cast<int>( timeout_ns / NS_IN_MS ) );
#endif
}
#ifdef _WIN32
bool FDEventQueue::toggle( socket_t fd, Object* context, bool enable_read, bool enable_write )
#else
bool FDEventQueue::toggle( fd_t fd, Object* context, bool enable_read, bool enable_write )
#endif
{
#if defined(_WIN32)
  if ( enable_read )
    FD_SET( fd, read_fds );
  else
    FD_CLR( fd, read_fds );
  if ( enable_write )
  {
    FD_SET( fd, write_fds );
    FD_SET( fd, except_fds );
  }
  else
  {
    FD_CLR( fd, write_fds );
    FD_CLR( fd, except_fds );
  }
  return true;
#elif defined(YIELD_HAVE_LINUX_EPOLL)
  struct epoll_event change_event;
  memset( &change_event, 0, sizeof( change_event ) );
#ifdef YIELD_LINUX_EPOLL_RETURN_FD
  change_event.data.fd = fd;
#else
  change_event.data.ptr = context;
#endif
  change_event.events = 0;
  if ( enable_read ) change_event.events |= EPOLLIN;
  if ( enable_write ) change_event.events |= EPOLLOUT;
  return epoll_ctl( poll_fd, EPOLL_CTL_MOD, fd, &change_event ) != -1;
#elif defined YIELD_HAVE_FREEBSD_KQUEUE
  struct kevent change_events[2];
  EV_SET( &change_events[0], fd, EVFILT_READ, enable_read ? EV_ENABLE : EV_DISABLE, 0, 0, context );
  EV_SET( &change_events[1], fd, EVFILT_WRITE, enable_write ? EV_ENABLE : EV_DISABLE, 0, 0, context );
  return kevent( poll_fd, change_events, 2, 0, 0, NULL ) != -1;
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
  if ( enable_read || enable_write )
  {
    int events = 0;
    if ( enable_read ) events |= POLLIN;
    if ( enable_write ) events |= POLLOUT;
    return port_associate( poll_fd, PORT_SOURCE_FD, fd, events, context ) != -1;
  }
  else
    return port_dissociate( poll_fd, PORT_SOURCE_FD, fd ) != -1;
#elif !defined(_WIN32)
  pollfd_vector::size_type j = ( next_pollfd_to_check > 0 ) ? next_pollfd_to_check - 1 : 0;
  pollfd_vector::size_type j_max = pollfds.size();
  for ( unsigned char i = 0; i < 2; i++ )
  {
    for ( ; j < j_max; j++ )
    {
      if ( pollfds[j].fd == fd )
      {
        pollfds[j].events = 0;
        if ( enable_read ) pollfds[j].events |= POLLIN;
        if ( enable_write ) pollfds[j].events |= POLLOUT;
        if ( pollfds[j].revents != 0 )
        {
          active_fds--;
          pollfds[j].revents = 0;
        }
        return true;
      }
    }
    if ( next_pollfd_to_check > 1 )
    {
      j = 0;
      j_max = next_pollfd_to_check - 1;
    }
    else
      break;
  }
  return false;
#else
  DebugBreak();
  return false;
#endif
}
#ifdef _WIN32
#pragma warning( pop )
#endif


// http_client.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
namespace YIELD
{
  class SyncHTTPRequest : public HTTPRequest
  {
  public:
    SyncHTTPRequest( const char* method, const URI& absolute_uri, auto_Object<> body = NULL )
      : HTTPRequest( method, absolute_uri, body )
    { }
    // Request
    bool respond( Response& response )
    {
      return http_response_queue.enqueue( response );
    }
    HTTPResponse& waitForHTTPResponse( timeout_ns_t timeout_ns )
    {
      if ( timeout_ns == static_cast<timeout_ns_t>( -1 ) )
        return http_response_queue.dequeue_typed<HTTPResponse>();
      else
        return http_response_queue.timed_dequeue_typed<HTTPResponse>( timeout_ns );
    }
  private:
    OneSignalEventQueue< NonBlockingFiniteQueue<Event*, 16 > > http_response_queue;
  };
};
auto_Object<Request> HTTPClient::createProtocolRequest( auto_Object<> body )
{
  if ( body->get_type_id() == YIELD_OBJECT_TYPE_ID( HTTPRequest ) )
    return static_cast<HTTPRequest*>( body.release() );
  else
    return NULL;
}
auto_Object<Response> HTTPClient::createProtocolResponse()
{
  return new HTTPResponse;
}
auto_Object<HTTPResponse> HTTPClient::GET( const URI& absolute_uri, auto_Object<Log> log )
{
  return sendHTTPRequest( "GET", absolute_uri, NULL, log );
}
auto_Object<HTTPResponse> HTTPClient::PUT( const URI& absolute_uri, auto_Object<> body, auto_Object<Log> log )
{
  return sendHTTPRequest( "PUT", absolute_uri, body, log );
}
auto_Object<HTTPResponse> HTTPClient::PUT( const URI& absolute_uri, const Path& body_file_path, auto_Object<Log> log )
{
  auto_Object<File> file = new File( body_file_path );
  auto_Object<String> body = new String( static_cast<size_t>( file->getattr()->get_size() ) );
  file->read( const_cast<char*>( body->c_str() ), body->size(), NULL );
  return sendHTTPRequest( "PUT", absolute_uri, body.release(), log );
}
void HTTPClient::respond( auto_Object<Request> protocol_request, auto_Object<Response> response )
{
  HTTPRequest* http_request = static_cast<HTTPRequest*>( protocol_request.get() );
  if ( response.get()->get_type_id() == YIELD_OBJECT_TYPE_ID( HTTPResponse ) )
    http_request->respond( *static_cast<HTTPResponse*>( response.release() ) );
  else
    DebugBreak();
}
auto_Object<HTTPResponse> HTTPClient::sendHTTPRequest( const char* method, const YIELD::URI& absolute_uri, auto_Object<> body, auto_Object<Log> log )
{
  auto_Object<SEDAStageGroup> stage_group = new SEDAStageGroup( "HTTPClient", 0, NULL, log );
  YIELD::URI checked_absolute_uri( absolute_uri );
  if ( checked_absolute_uri.get_port() == 0 )
    checked_absolute_uri.set_port( 80 );
  auto_Object<HTTPClient> http_client = HTTPClient::create( stage_group, checked_absolute_uri, NULL, log );
  auto_Object<SyncHTTPRequest> http_request = new SyncHTTPRequest( method, checked_absolute_uri, body );
  http_request->set_header( "User-Agent", "Flog 0.99" );
  http_client->send( http_request->incRef() );
  auto_Object<HTTPResponse> http_response = http_request->waitForHTTPResponse( static_cast<timeout_ns_t>( -1 ) );
  return http_response;
}


// http_request.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
HTTPRequest::HTTPRequest()
{
  method[0] = 0;
  uri = new char[2];
  uri[0] = 0;
  uri_len = 2;
  http_version = 1;
  deserialize_state = DESERIALIZING_METHOD;
  serialize_state = SERIALIZING_METHOD;
}
HTTPRequest::HTTPRequest( const char* method, const char* relative_uri, const char* host, auto_Object<> body )
  : body( body )
{
  init( method, relative_uri, host, body );
}
HTTPRequest::HTTPRequest( const char* method, const URI& absolute_uri, auto_Object<> body )
  : body( body )
{
  init( method, absolute_uri.get_resource().c_str(), absolute_uri.get_host().c_str(), body );
}
void HTTPRequest::init( const char* method, const char* relative_uri, const char* host, auto_Object<> body )
{
  strncpy( this->method, method, 16 );
  uri_len = std::strlen( relative_uri );
  this->uri = new char[uri_len + 1];
  memcpy( this->uri, relative_uri, uri_len + 1 );
  http_version = 1;
  set_header( "Host", const_cast<char*>( host ) );
  deserialize_state = DESERIALIZE_DONE;
  serialize_state = SERIALIZING_METHOD;
}
HTTPRequest::~HTTPRequest()
{
  delete [] uri;
}
Stream::Status HTTPRequest::deserialize( InputStream& input_stream, size_t* out_bytes_read )
{
  switch ( deserialize_state )
  {
    case DESERIALIZING_METHOD:
    {
      char* method_p = method + std::strlen( method );
      for ( ;; )
      {
        Stream::Status read_status = input_stream.read( method_p, 1, NULL );
        if ( read_status == Stream::STREAM_STATUS_OK )
        {
          if ( *method_p != ' ' )
            method_p++;
          else
          {
            *method_p = 0;
            deserialize_state = DESERIALIZING_URI;
            break;
          }
        }
        else
        {
          *method_p = 0;
          return read_status;
        }
      }
      // Fall through
    }
    case DESERIALIZING_URI:
    {
      char* uri_p = uri + std::strlen( uri );
      for ( ;; )
      {
        Stream::Status read_status = input_stream.read( uri_p, 1, NULL );
        if ( read_status == Stream::STREAM_STATUS_OK )
        {
          if ( *uri_p == ' ' )
          {
            *uri_p = 0;
            uri_len = uri_p - uri;
            deserialize_state = DESERIALIZING_HTTP_VERSION;
            break;
          }
          else
          {
            uri_p++;
            if ( static_cast<size_t>( uri_p - uri ) == uri_len )
            {
              size_t new_uri_len = uri_len * 2;
              char* new_uri = new char[new_uri_len];
              memcpy( new_uri, uri, uri_len );
              delete [] uri;
              uri = new_uri;
              uri_p = uri + uri_len;
              uri_len = new_uri_len;
            }
          }
        }
        else
        {
          *uri_p = 0;
          return read_status;
        }
      }
      // Fall through
    }
    case DESERIALIZING_HTTP_VERSION:
    {
      for ( ;; )
      {
        uint8_t test_http_version;
        Stream::Status read_status = input_stream.read( &test_http_version, 1, NULL );
        if ( read_status == Stream::STREAM_STATUS_OK )
        {
          if ( test_http_version != '\r' )
          {
            http_version = test_http_version;
            continue;
          }
          else
          {
            http_version = http_version == '1' ? 1 : 0;
            deserialize_state = DESERIALIZING_URI;
            break;
          }
        }
        else
          return read_status;
      }
    }
    // Fall through
    case DESERIALIZING_HEADERS:
    {
      Stream::Status read_status = RFC822Headers::deserialize( input_stream, NULL );
      if ( read_status == Stream::STREAM_STATUS_OK )
      {
        if ( strcmp( get_header( "Transfer-Encoding" ), "chunked" ) == 0 )
          return Stream::STREAM_STATUS_ERROR; // DebugBreak();
        else
        {
          const char* content_length_header_value = get_header( "Content-Length", NULL ); // Most browsers
          if ( content_length_header_value == NULL )
            content_length_header_value = get_header( "Content-length" ); // httperf
          size_t content_length = atoi( content_length_header_value );
          if ( content_length == 0 )
          {
            deserialize_state = DESERIALIZE_DONE;
            return Stream::STREAM_STATUS_OK;
          }
          else
          {
            body = new String( content_length );
            deserialize_state = DESERIALIZING_BODY;
            if ( strcmp( get_header( "Expect" ), "100-continue" ) == 0 )
              return Stream::STREAM_STATUS_OK;
            // else fall through
          }
        }
      }
      else
        return read_status;
    }
    case DESERIALIZING_BODY:
    {
      Stream::Status read_status = static_cast<String*>( body.get() )->deserialize( input_stream, out_bytes_read );
      if ( read_status == Stream::STREAM_STATUS_OK )
        deserialize_state = DESERIALIZE_DONE;
      return read_status;
    }
    case DESERIALIZE_DONE: return Stream::STREAM_STATUS_OK;
    default: DebugBreak(); return Stream::STREAM_STATUS_ERROR;
  }
}
bool HTTPRequest::respond( uint16_t status_code )
{
  return respond( *( new HTTPResponse( status_code ) ) );
}
bool HTTPRequest::respond( uint16_t status_code, auto_Object<> body )
{
  return respond( *( new HTTPResponse( status_code, body ) ) );
}
Stream::Status HTTPRequest::serialize( OutputStream& output_stream, size_t* out_bytes_written )
{
  switch ( serialize_state )
  {
    case SERIALIZING_METHOD:
    {
      Stream::Status write_status = output_stream.write( method, out_bytes_written );
      if ( write_status == Stream::STREAM_STATUS_OK )
        serialize_state = SERIALIZING_URI;
      else
        return write_status;
    }
    // Fall through
    case SERIALIZING_METHOD_URI_SEPARATOR:
    {
      Stream::Status write_status = output_stream.write( " ", 1, out_bytes_written );
      if ( write_status == Stream::STREAM_STATUS_OK )
        serialize_state = SERIALIZING_URI;
      else
        return write_status;
    }
    // Fall through
    case SERIALIZING_URI:
    {
      Stream::Status write_status = output_stream.write( uri, uri_len, out_bytes_written );
      if ( write_status == Stream::STREAM_STATUS_OK )
        serialize_state = SERIALIZING_HTTP_VERSION;
      else
        return write_status;
    }
    // Fall through
    case SERIALIZING_HTTP_VERSION:
    {
      Stream::Status write_status = output_stream.write( " HTTP/1.1\r\n", 11, out_bytes_written );
      if ( write_status == Stream::STREAM_STATUS_OK )
      {
        if ( body != NULL && body->get_general_type() == Object::STRING && get_header( "Content-Length", NULL ) == NULL )
        {
          char content_length_str[32];
#ifdef _WIN32
          sprintf_s( content_length_str, 32, "%u", static_cast<String*>( body.get() )->size() );
#else
          snprintf( content_length_str, 32, "%zu", static_cast<String*>( body.get() )->size() );
#endif
          set_header( "Content-Length", content_length_str );
        }
        RFC822Headers::set_iovec( "\r\n", 2 );
        serialize_state = SERIALIZING_HEADERS;
      }
      else
        return write_status;
    }
    // Fall through
    case SERIALIZING_HEADERS:
    {
      Stream::Status write_status = RFC822Headers::serialize( output_stream, out_bytes_written );
      if ( write_status == Stream::STREAM_STATUS_OK )
      {
        if ( body != NULL )
          serialize_state = SERIALIZING_BODY;
        else
        {
          serialize_state = SERIALIZE_DONE;
          return Stream::STREAM_STATUS_OK;
        }
      }
      else
        return write_status;
    }
    // Fall through
    case SERIALIZING_BODY:
    {
      Stream::Status write_status = body->serialize( output_stream, out_bytes_written );
      if ( write_status == Stream::STREAM_STATUS_OK )
        serialize_state = SERIALIZE_DONE;
      else
        return write_status;
    }
    // Fall through
    case SERIALIZE_DONE: return Stream::STREAM_STATUS_OK;
    default: DebugBreak(); return Stream::STREAM_STATUS_ERROR;
  }
}


// http_response.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
HTTPResponse::HTTPResponse()
{
  memset( status_code_str, 0, sizeof( status_code_str ) );
  deserialize_state = DESERIALIZING_HTTP_VERSION;
  serialize_state = SERIALIZING_STATUS_LINE;
}
HTTPResponse::HTTPResponse( uint16_t status_code )
  : status_code( status_code )
{
  deserialize_state = DESERIALIZE_DONE;
  serialize_state = SERIALIZING_STATUS_LINE;
}
HTTPResponse::HTTPResponse( uint16_t status_code, auto_Object<> body )
  : status_code( status_code ), body( body )
{
  deserialize_state = DESERIALIZE_DONE;
  serialize_state = SERIALIZING_STATUS_LINE;
}
Stream::Status HTTPResponse::deserialize( InputStream& input_stream, size_t* out_bytes_read )
{
  switch ( deserialize_state )
  {
    case DESERIALIZING_HTTP_VERSION:
    {
      for ( ;; )
      {
        uint8_t test_http_version;
        Stream::Status read_status = input_stream.read( &test_http_version, 1, NULL );
        if ( read_status == Stream::STREAM_STATUS_OK )
        {
          if ( test_http_version != ' ' )
          {
            http_version = test_http_version;
            continue;
          }
          else
          {
            http_version = http_version == '1' ? 1 : 0;
            deserialize_state = DESERIALIZING_STATUS_CODE;
            break;
          }
        }
        else
          return read_status;
      }
    }
    // Fall through
    case DESERIALIZING_STATUS_CODE:
    {
      char* status_code_str_p = status_code_str + std::strlen( status_code_str );
      for ( ;; )
      {
        Stream::Status read_status = input_stream.read( status_code_str_p, 1, NULL );
        if ( read_status == Stream::STREAM_STATUS_OK )
        {
          if ( *status_code_str_p != ' ' )
          {
            status_code_str_p++;
            if ( static_cast<uint8_t>( status_code_str_p - status_code_str ) == 4 )
            {
              deserialize_state = DESERIALIZE_DONE;
              return Stream::STREAM_STATUS_ERROR;
            }
          }
          else
          {
            *status_code_str_p = 0;
            status_code = static_cast<uint16_t>( atoi( status_code_str ) );
            if ( status_code == 0 )
              status_code = 500;
            deserialize_state = DESERIALIZING_REASON;
            break;
          }
        }
        else
          return read_status;
      }
    }
    // Fall through
    case DESERIALIZING_REASON:
    {
      char c;
      for ( ;; )
      {
        Stream::Status read_status = input_stream.read( &c, 1, NULL );
        if ( read_status == Stream::STREAM_STATUS_OK )
        {
          if ( c == '\r' )
          {
            deserialize_state = DESERIALIZING_HEADERS;
            break;
          }
        }
        else
          return read_status;
      }
    }
    // Fall through
    case DESERIALIZING_HEADERS:
    {
      Stream::Status read_status = RFC822Headers::deserialize( input_stream, NULL );
      if ( read_status == Stream::STREAM_STATUS_OK )
      {
        if ( strcmp( get_header( "Transfer-Encoding" ), "chunked" ) == 0 )
          return Stream::STREAM_STATUS_ERROR; // DebugBreak();
        else
        {
          size_t content_length = atoi( get_header( "Content-Length" ) );
          if ( content_length == 0 )
          {
            deserialize_state = DESERIALIZE_DONE;
            return Stream::STREAM_STATUS_OK;
          }
          else
          {
            body = new String( content_length );
            deserialize_state = DESERIALIZING_BODY;
          }
        }
      }
      else
        return read_status;
    }
    case DESERIALIZING_BODY:
    {
      Stream::Status read_status = static_cast<String*>( body.get() )->deserialize( input_stream, out_bytes_read );
      if ( read_status == Stream::STREAM_STATUS_OK )
        deserialize_state = DESERIALIZE_DONE;
      return read_status;
    }
    case DESERIALIZE_DONE: return Stream::STREAM_STATUS_OK;
    default: DebugBreak(); return Stream::STREAM_STATUS_ERROR;
  }
}
Stream::Status HTTPResponse::serialize( OutputStream& output_stream, size_t* out_bytes_written )
{
  switch ( serialize_state )
  {
    case SERIALIZING_STATUS_LINE:
    {
      const char* status_line;
      size_t status_line_len;
      switch ( status_code )
      {
        case 100: status_line = "HTTP/1.1 100 Continue\r\n"; status_line_len = 23; break;
        case 200: status_line = "HTTP/1.1 200 OK\r\n"; status_line_len = 17; break;
        case 201: status_line = "HTTP/1.1 201 Created\r\n"; status_line_len = 22; break;
        case 202: status_line = "HTTP/1.1 202 Accepted\r\n"; status_line_len = 23; break;
        case 203: status_line = "HTTP/1.1 203 Non-Authoritative Information\r\n"; status_line_len = 44; break;
        case 204: status_line = "HTTP/1.1 204 No Content\r\n"; status_line_len = 25; break;
        case 205: status_line = "HTTP/1.1 205 Reset Content\r\n"; status_line_len = 28; break;
        case 206: status_line = "HTTP/1.1 206 Partial Content\r\n"; status_line_len = 30; break;
        case 207: status_line = "HTTP/1.1 207 Multi-Status\r\n"; status_line_len = 27; break;
        case 300: status_line = "HTTP/1.1 300 Multiple Choices\r\n"; status_line_len = 31; break;
        case 301: status_line = "HTTP/1.1 301 Moved Permanently\r\n"; status_line_len = 32; break;
        case 302: status_line = "HTTP/1.1 302 Found\r\n"; status_line_len = 20; break;
        case 303: status_line = "HTTP/1.1 303 See Other\r\n"; status_line_len = 24; break;
        case 304: status_line = "HTTP/1.1 304 Not Modified\r\n"; status_line_len = 27; break;
        case 305: status_line = "HTTP/1.1 305 Use Proxy\r\n"; status_line_len = 24; break;
        case 307: status_line = "HTTP/1.1 307 Temporary Redirect\r\n"; status_line_len = 33; break;
        case 400: status_line = "HTTP/1.1 400 Bad Request\r\n"; status_line_len = 26; break;
        case 401: status_line = "HTTP/1.1 401 Unauthorized\r\n"; status_line_len = 27; break;
        case 403: status_line = "HTTP/1.1 403 Forbidden\r\n"; status_line_len = 24; break;
        case 404: status_line = "HTTP/1.1 404 Not Found\r\n"; status_line_len = 24; break;
        case 405: status_line = "HTTP/1.1 405 Method Not Allowed\r\n"; status_line_len = 33; break;
        case 406: status_line = "HTTP/1.1 406 Not Acceptable\r\n"; status_line_len = 29; break;
        case 407: status_line = "HTTP/1.1 407 Proxy Authentication Required\r\n"; status_line_len = 44; break;
        case 408: status_line = "HTTP/1.1 408 Request Timeout\r\n"; status_line_len = 30; break;
        case 409: status_line = "HTTP/1.1 409 Conflict\r\n"; status_line_len = 23; break;
        case 410: status_line = "HTTP/1.1 410 Gone\r\n"; status_line_len = 19; break;
        case 411: status_line = "HTTP/1.1 411 Length Required\r\n"; status_line_len = 30; break;
        case 412: status_line = "HTTP/1.1 412 Precondition Failed\r\n"; status_line_len = 34; break;
        case 413: status_line = "HTTP/1.1 413 Request Entity Too Large\r\n"; status_line_len = 39; break;
        case 414: status_line = "HTTP/1.1 414 Request-URI Too Long\r\n"; status_line_len = 35; break;
        case 415: status_line = "HTTP/1.1 415 Unsupported Media Type\r\n"; status_line_len = 37; break;
        case 416: status_line = "HTTP/1.1 416 Request Range Not Satisfiable\r\n"; status_line_len = 44; break;
        case 417: status_line = "HTTP/1.1 417 Expectation Failed\r\n"; status_line_len = 33; break;
        case 422: status_line = "HTTP/1.1 422 Unprocessable Entitiy\r\n"; status_line_len = 36; break;
        case 423: status_line = "HTTP/1.1 423 Locked\r\n"; status_line_len = 21; break;
        case 424: status_line = "HTTP/1.1 424 Failed Dependency\r\n"; status_line_len = 32; break;
        case 500: status_line = "HTTP/1.1 500 Internal Server Error\r\n"; status_line_len = 36; break;
        case 501: status_line = "HTTP/1.1 501 Not Implemented\r\n"; status_line_len = 30; break;
        case 502: status_line = "HTTP/1.1 502 Bad Gateway\r\n"; status_line_len = 26; break;
        case 503: status_line = "HTTP/1.1 503 Service Unavailable\r\n"; status_line_len = 34; break;
        case 504: status_line = "HTTP/1.1 504 Gateway Timeout\r\n"; status_line_len = 30; break;
        case 505: status_line = "HTTP/1.1 505 HTTP Version Not Supported\r\n"; status_line_len = 41; break;
        case 507: status_line = "HTTP/1.1 507 Insufficient Storage\r\n"; status_line_len = 35; break;
        default: status_line = "HTTP/1.1 500 Internal Server Error\r\n"; status_line_len = 36; break;
      }
      Stream::Status write_status = output_stream.write( status_line, status_line_len, out_bytes_written );
      if ( write_status == Stream::STREAM_STATUS_OK )
      {
        char http_datetime[32];
        Time().as_http_date_time( http_datetime, 32 );
        RFC822Headers::set_header( "Date", http_datetime );
        RFC822Headers::set_iovec( "\r\n", 2 );
        serialize_state = SERIALIZING_HEADERS;
      }
      else
        return write_status;
    }
    // Fall through
    case SERIALIZING_HEADERS:
    {
      Stream::Status write_status = RFC822Headers::serialize( output_stream, out_bytes_written );
      if ( write_status == Stream::STREAM_STATUS_OK )
      {
        if ( body != NULL )
          serialize_state = SERIALIZING_BODY;
        else
        {
          serialize_state = SERIALIZE_DONE;
          return Stream::STREAM_STATUS_OK;
        }
      }
      else
        return write_status;
    }
    // Fall through
    case SERIALIZING_BODY:
    {
      Stream::Status write_status = body->serialize( output_stream, out_bytes_written );
      if ( write_status == Stream::STREAM_STATUS_OK )
        serialize_state = SERIALIZE_DONE;
      else
        return write_status;
    }
    // Fall through
    case SERIALIZE_DONE: return Stream::STREAM_STATUS_OK;
    default: DebugBreak(); return Stream::STREAM_STATUS_ERROR;
  }
}


// json_input_stream.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
extern "C"
{
  #include <yajl.h>
};
namespace YIELD
{
  class JSONValue
  {
  public:
    JSONValue( String* identifier, Object::GeneralType general_type )
      : identifier( identifier ), general_type( general_type )
    {
      parent = child = prev = next = NULL;
      have_read = false;
      as_integer = 0;
    }
    virtual ~JSONValue()
    {
      delete child;
      delete next;
      Object::decRef( identifier );
      if ( general_type == Object::STRING )
        Object::decRef( as_string );
    }
    String* identifier;
    Object::GeneralType general_type;
    union
    {
      String* as_string;
      double as_double;
      int64_t as_integer;
    };
    JSONValue *parent, *child, *prev, *next;
    bool have_read;
  protected:
    JSONValue() : identifier( NULL ), general_type( Object::UNKNOWN )
    {
      parent = child = prev = next = NULL;
      have_read = false;
      as_integer = 0;
    }
  };
  class JSONObject : public Object, public JSONValue
  {
  public:
    JSONObject( InputStream& input_stream )
    {
      current_json_value = parent_json_value = NULL;
      reader = yajl_alloc( &JSONObject_yajl_callbacks, NULL, this );
      next_map_key = NULL; next_map_key_len = 0;
      type_id = 0;
      unsigned char read_buffer[4096];
      for ( ;; )
      {
        size_t read_len;
        if ( input_stream.read( read_buffer, 4096, &read_len ) == Stream::STREAM_STATUS_OK )
        {
          switch( yajl_parse( reader, read_buffer, static_cast<unsigned int>( read_len ) ) )
          {
            case yajl_status_ok: return;
            case yajl_status_insufficient_data: continue;
            default:
            {
              unsigned char* yajl_error_str = yajl_get_error( reader, 1, read_buffer, static_cast<unsigned int>( read_len ) );
              std::ostringstream what;
              what << __FILE__ << ":" << __LINE__ << ": JSON parsing error: " << reinterpret_cast<char*>( yajl_error_str ) << std::endl;
              yajl_free_error( yajl_error_str );
              throw Exception( what.str() );
            }
            break;
          }
        }
        else
          throw Exception( "error reading underlying_input_stream before parsing complete" );
      }
    }
    ~JSONObject()
    {
      yajl_free( reader );
    }
    std::string type_name;
    uint32_t type_id;
    // Object
    Object::GeneralType get_general_type() const { return general_type; }
    const char* get_type_name() const { return !type_name.empty() ? type_name.c_str() : "JSONObject";  }
    uint32_t get_type_id() const { return type_id ? type_id : 2571531487UL; }
  private:
    yajl_handle reader;
    // Parsing state
    JSONValue *current_json_value, *parent_json_value;
    const char* next_map_key; size_t next_map_key_len;
    // yajl callbacks
    static int handle_yajl_null( void* _self )
    {
      JSONObject* self = static_cast<JSONObject*>( _self );
      self->createNextJSONValue().as_integer = 0;
      return 1;
    }
    static int handle_yajl_boolean( void* _self, int value )
    {
      JSONObject* self = static_cast<JSONObject*>( _self );
      self->createNextJSONValue().as_integer = value;
      return 1;
    }
    static int handle_yajl_integer( void* _self, long value )
    {
      JSONObject* self = static_cast<JSONObject*>( _self );
      self->createNextJSONValue().as_integer = value;
      return 1;
    }
    static int handle_yajl_double( void* _self, double value )
    {
      JSONObject* self = static_cast<JSONObject*>( _self );
      self->createNextJSONValue().as_double = value;
      return 1;
    }
    static int handle_yajl_string( void* _self, const unsigned char* buffer, unsigned int len )
    {
      JSONObject* self = static_cast<JSONObject*>( _self );
      JSONValue& json_value = self->createNextJSONValue( Object::STRING );
      json_value.as_string = new String( reinterpret_cast<const char*>( buffer ), len );
      return 1;
    }
    static int handle_yajl_start_map( void* _self )
    {
      JSONObject* self = static_cast<JSONObject*>( _self );
      JSONValue& json_value = self->createNextJSONValue( Object::MAP );
      self->parent_json_value = &json_value;
      self->current_json_value = json_value.child;
      return 1;
    }
    static int handle_yajl_map_key( void* _self, const unsigned char* map_key, unsigned int map_key_len )
    {
      JSONObject* self = static_cast<JSONObject*>( _self );
      self->next_map_key = reinterpret_cast<const char*>( map_key );
      self->next_map_key_len = map_key_len;
      return 1;
    }
    static int handle_yajl_end_map( void* _self )
    {
      JSONObject* self = static_cast<JSONObject*>( _self );
      if ( self->current_json_value == NULL ) // Empty map
        self->current_json_value = self->parent_json_value;
      else
        self->current_json_value = self->current_json_value->parent;
      self->parent_json_value = NULL;
      return 1;
    }
    static int handle_yajl_start_array( void* _self )
    {
      JSONObject* self = static_cast<JSONObject*>( _self );
      JSONValue& json_value = self->createNextJSONValue( Object::SEQUENCE );
      self->parent_json_value = &json_value;
      self->current_json_value = json_value.child;
      return 1;
    }
    static int handle_yajl_end_array( void* _self )
    {
      JSONObject* self = static_cast<JSONObject*>( _self );
      if ( self->current_json_value == NULL ) // Empty array
        self->current_json_value = self->parent_json_value;
      else
        self->current_json_value = self->current_json_value->parent;
      self->parent_json_value = NULL;
      return 1;
    }
    JSONValue& createNextJSONValue( Object::GeneralType general_type = Object::UNKNOWN )
    {
      String* identifier = next_map_key_len != 0 ? new String( next_map_key, next_map_key_len ) : NULL;
      next_map_key = NULL; next_map_key_len = 0;
      if ( current_json_value == NULL )
      {
        if ( parent_json_value ) // This is the first value of an array or struct
        {
          current_json_value = new JSONValue( identifier, general_type );
          current_json_value->parent = parent_json_value;
          parent_json_value->child = current_json_value;
        }
        else // This is the first value of the whole object
        {
#ifdef _DEBUG
          if ( identifier != NULL ) DebugBreak();
#endif
          this->general_type = general_type;
          current_json_value = this;
        }
      }
      else
      {
        JSONValue* next_json_value;
        next_json_value = new JSONValue( identifier, general_type );
        next_json_value->parent = current_json_value->parent;
        next_json_value->prev = current_json_value;
        current_json_value->next = next_json_value;
        current_json_value = next_json_value;
      }
      return *current_json_value;
    }
    static yajl_callbacks JSONObject_yajl_callbacks;
  };
};
yajl_callbacks JSONObject::JSONObject_yajl_callbacks =
{
  handle_yajl_null,
  handle_yajl_boolean,
  handle_yajl_integer,
  handle_yajl_double,
  NULL,
  handle_yajl_string,
  handle_yajl_start_map,
  handle_yajl_map_key,
  handle_yajl_end_map,
  handle_yajl_start_array,
  handle_yajl_end_array
};
JSONInputStream::JSONInputStream( InputStream& underlying_input_stream )
{
  root_decl = NULL;
  root_json_value = new JSONObject( underlying_input_stream );
  next_json_value = root_json_value->child;
}
JSONInputStream::JSONInputStream( const Declaration& root_decl, JSONValue& root_json_value )
  : root_decl( &root_decl ), root_json_value( &root_json_value ),
    next_json_value( root_json_value.child )
{ }
JSONInputStream::~JSONInputStream()
{
  if ( root_decl == NULL )
    delete root_json_value;
}
void JSONInputStream::readString( const Declaration& decl, std::string& str )
{
  JSONValue* json_value = readJSONValue( decl, Object::STRING );
  if ( json_value )
  {
    if ( decl.identifier ) // Read the value
      str.assign( *json_value->as_string );
    else // Read the identifier
      str.assign( *json_value->identifier );
  }
}
bool JSONInputStream::readBool( const Declaration& decl )
{
  JSONValue* json_value = readJSONValue( decl );
  if ( json_value )
  {
    if ( decl.identifier ) // Read the value
      return json_value->as_integer != 0;
    else // Read the identifier
      return false; // Doesn't make any sense
  }
  else
    return false;
}
int64_t JSONInputStream::readInt64( const Declaration& decl )
{
  JSONValue* json_value = readJSONValue( decl );
  if ( json_value )
  {
    if ( decl.identifier ) // Read the value
      return json_value->as_integer;
    else // Read the identifier
      return atoi( json_value->identifier->c_str() );
  }
  else
    return 0;
}
double JSONInputStream::readDouble( const Declaration& decl )
{
  JSONValue* json_value = readJSONValue( decl );
  if ( json_value )
  {
    if ( decl.identifier ) // Read the value
      return json_value->as_double;
    else // Read the identifier
      return atof( json_value->identifier->c_str() );
  }
  else
    return 0;
}
Object* JSONInputStream::readObject( const Declaration& decl, Object* value, Object::GeneralType value_general_type )
{
  if ( value )
  {
    if ( value_general_type == Object::UNKNOWN )
    value_general_type = value->get_general_type();
    JSONValue* json_value;
    if ( decl.identifier )
    {
    json_value = readJSONValue( decl, value_general_type );
    if ( json_value == NULL )
      return value;
    }
    else if ( root_json_value && !root_json_value->have_read )
    {
    if ( value_general_type == Object::SEQUENCE && root_json_value->general_type == Object::SEQUENCE )
      json_value = root_json_value;
    else if ( value_general_type != Object::STRING && root_json_value->general_type == Object::MAP )
      json_value = root_json_value;
    else
      return value;
    }
    else
    return value;
    JSONInputStream child_input_stream( decl, *json_value );
    switch ( value_general_type )
    {
    case Object::SEQUENCE: child_input_stream.readSequence( *value ); break;
    case Object::MAP: child_input_stream.readMap( *value ); break;
    default: child_input_stream.readStruct( *value ); break;
    }
    json_value->have_read = true;
  }
  return value;
}
void JSONInputStream::readSequence( Object& s )
{
  while ( next_json_value )
    s.deserialize( *this );
}
void JSONInputStream::readMap( Object& s )
{
  while ( next_json_value )
    s.deserialize( *this );
}
void JSONInputStream::readStruct( Object& s )
{
  s.deserialize( *this );
}
JSONValue* JSONInputStream::readJSONValue( const Declaration& decl, Object::GeneralType expected_general_type )
{
  if ( root_json_value->general_type == Object::SEQUENCE )
  {
    if (
         next_json_value && !next_json_value->have_read &&
         (
           expected_general_type == Object::UNKNOWN ||
           next_json_value->general_type == expected_general_type ||
           ( expected_general_type == Object::STRUCT && next_json_value->general_type == Object::MAP )
         )
       )
    {
      JSONValue* json_value = next_json_value;
      next_json_value = json_value->next;
      json_value->have_read = true;
      return json_value;
    }
  }
  else if ( root_json_value->general_type == Object::MAP )
  {
    if ( decl.identifier ) // Given a key, reading a value
    {
      JSONValue* child_json_value = root_json_value->child;
      while ( child_json_value )
      {
        if (
             !child_json_value->have_read &&
             strcmp( child_json_value->identifier->c_str(), decl.identifier ) == 0 &&
             ( expected_general_type == Object::UNKNOWN || child_json_value->general_type == expected_general_type ||
             ( child_json_value->general_type == Object::MAP && expected_general_type == Object::STRUCT ) )
           )
        {
          child_json_value->have_read = true;
          return child_json_value;
        }
        child_json_value = child_json_value->next;
      }
    }
    else if ( next_json_value && !next_json_value->have_read ) // Reading the next key
    {
      JSONValue* json_value = next_json_value;
      next_json_value = json_value->next;
      return json_value;
    }
  }
  return NULL;
}


// json_output_stream.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
extern "C"
{
  #include <yajl.h>
};
JSONOutputStream::JSONOutputStream( OutputStream& underlying_output_stream, bool write_empty_strings )
: underlying_output_stream( underlying_output_stream ), write_empty_strings( write_empty_strings )
{
  root_decl = NULL;
  writer = yajl_gen_alloc( NULL );
}
JSONOutputStream::JSONOutputStream( OutputStream& underlying_output_stream, bool write_empty_strings, yajl_gen writer, const Declaration& root_decl )
  : underlying_output_stream( underlying_output_stream ), write_empty_strings( write_empty_strings ), root_decl( &root_decl ), writer( writer )
{ }
JSONOutputStream::~JSONOutputStream()
{
  if ( root_decl == NULL ) // This is the root JSONOutputStream
    yajl_gen_free( writer );
}
void JSONOutputStream::flushYAJLBuffer()
{
  const unsigned char * buffer;
  unsigned int len;
  yajl_gen_get_buf( writer, &buffer, &len );
  underlying_output_stream.write( buffer, len );
  yajl_gen_clear( writer );
}
void JSONOutputStream::writeBool( const Declaration& decl, bool value )
{
  writeDeclaration( decl );
  yajl_gen_bool( writer, ( int )value );
  flushYAJLBuffer();
}
void JSONOutputStream::writeDeclaration( const Declaration& decl )
{
  if ( in_map && decl.identifier )
    yajl_gen_string( writer, reinterpret_cast<const unsigned char*>( decl.identifier ), static_cast<unsigned int>( strlen( decl.identifier ) ) );
}
void JSONOutputStream::writeDouble( const Declaration& decl, double value )
{
  writeDeclaration( decl );
  yajl_gen_double( writer, value );
  flushYAJLBuffer();
}
void JSONOutputStream::writeInt64( const Declaration& decl, int64_t value )
{
  writeDeclaration( decl );
  yajl_gen_integer( writer, ( long )value );
  flushYAJLBuffer();
}
void JSONOutputStream::writeMap( Object* s )
{
  yajl_gen_map_open( writer );
  in_map = true;
  if ( s )
    s->serialize( *this );
  yajl_gen_map_close( writer );
  flushYAJLBuffer();
}
void JSONOutputStream::writeObject( const Declaration& decl, Object& value, Object::GeneralType value_general_type )
{
  if ( value_general_type == Object::UNKNOWN )
    value_general_type = value.get_general_type();
  switch ( value_general_type )
  {
    case Object::STRING: writeString( decl, static_cast<String&>( value ).c_str(), static_cast<String&>( value ).size() ); break;
    case Object::SEQUENCE: writeDeclaration( decl ); JSONOutputStream( underlying_output_stream, write_empty_strings, writer, decl ).writeSequence( &value ); break;
    case Object::MAP: writeDeclaration( decl ); JSONOutputStream( underlying_output_stream, write_empty_strings, writer, decl ).writeMap( &value ); break;
    default: writeDeclaration( decl ); JSONOutputStream( underlying_output_stream, write_empty_strings, writer, decl ).writeStruct( &value ); break;
  }
}
void JSONOutputStream::writePointer( const Declaration& decl, void* value )
{
  writeDeclaration( decl );
  yajl_gen_null( writer );
  flushYAJLBuffer();
}
void JSONOutputStream::writeSequence( Object* s )
{
  yajl_gen_array_open( writer );
  in_map = false;
  if ( s )
    s->serialize( *this );
  yajl_gen_array_close( writer );
  flushYAJLBuffer();
}
void JSONOutputStream::writeString( const Declaration& decl, const char* value, size_t value_len )
{
  if ( value_len > 0 || write_empty_strings )
  {
    writeDeclaration( decl );
    yajl_gen_string( writer, reinterpret_cast<const unsigned char*>( value ), static_cast<unsigned int>( value_len ) );
    flushYAJLBuffer();
  }
}
void JSONOutputStream::writeStruct( Object* s )
{
  writeMap( s );
}


// oncrpc_client.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
ONCRPCClient::ONCRPCClient( const SocketAddress& peer_sockaddr, auto_Object<SocketFactory> socket_factory, auto_Object<Log> log )
  : Client( peer_sockaddr, socket_factory, log )
{
  object_factories = new ObjectFactories;
}
auto_Object<Request> ONCRPCClient::createProtocolRequest( auto_Object<> body )
{
  return new ONCRPCRequest( body, get_log() );
}
auto_Object<Response> ONCRPCClient::createProtocolResponse()
{
  return new ONCRPCResponse( object_factories, get_log() );
}
void ONCRPCClient::respond( auto_Object<Request> protocol_request, auto_Object<Response> response )
{
  ONCRPCRequest* oncrpc_request = static_cast<ONCRPCRequest*>( protocol_request.get() );
  switch ( oncrpc_request->get_body()->get_general_type() )
  {
    case REQUEST:
    {
      Request* request = static_cast<Request*>( oncrpc_request->get_body().get() );
      if ( response->get_type_id() == YIELD_OBJECT_TYPE_ID( ONCRPCResponse ) )
      {
        ONCRPCResponse* oncrpc_response = static_cast<ONCRPCResponse*>( response.get() );
        switch ( oncrpc_response->get_body()->get_general_type() )
        {
          case RESPONSE:
          case EXCEPTION_RESPONSE:
          {
            Response* response = static_cast<Response*>( oncrpc_response->get_body().get() );
            request->respond( response->incRef() );
          }
          break;
          default: break;
        }
      }
      else if ( response->get_general_type() == EXCEPTION_RESPONSE )
        request->respond( *response.release() );
    }
    break;
    default: break;
  }
}


// oncrpc_message.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
ONCRPCMessage::~ONCRPCMessage()
{
  delete oncrpc_record_input_stream;
}
ONCRPCRecordInputStream& ONCRPCMessage::get_oncrpc_record_input_stream( InputStream& underlying_input_stream )
{
  if ( oncrpc_record_input_stream == NULL )
    oncrpc_record_input_stream = new ONCRPCRecordInputStream( underlying_input_stream );
  else if ( &oncrpc_record_input_stream->get_underlying_input_stream() != &underlying_input_stream )
  {
    delete oncrpc_record_input_stream;
    oncrpc_record_input_stream = new ONCRPCRecordInputStream( underlying_input_stream );
  }
  return *oncrpc_record_input_stream;
}


// oncrpc_request.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
Stream::Status ONCRPCRequest::deserialize( InputStream& input_stream, size_t* out_bytes_read )
{
  ONCRPCRecordInputStream oncrpc_record_input_stream = get_oncrpc_record_input_stream( input_stream );
  XDRInputStream xdr_input_stream( oncrpc_record_input_stream );
  try
  {
    xid = xdr_input_stream.readUint32( "xid" );
    int32_t msg_type = xdr_input_stream.readInt32( "msg_type" );
    if ( msg_type == 0 ) // CALL
    {
      uint32_t rpcvers = xdr_input_stream.readUint32( "rpcvers" );
      if ( rpcvers == 2 )
      {
        uint32_t prog = xdr_input_stream.readUint32( "prog" );
        uint32_t vers = xdr_input_stream.readUint32( "vers" );
        uint32_t proc = xdr_input_stream.readUint32( "proc" );
        uint32_t credential_auth_flavor = xdr_input_stream.readUint32( "credential_auth_flavor" );
        uint32_t credential_auth_body_length = xdr_input_stream.readUint32( "credential_auth_body_length" );
        if ( credential_auth_body_length > 0 ) // TODO: read into credentials here
        {
          if ( log != NULL )
            log->getStream( Log::LOG_WARNING ) << get_type_name() << ": received credential body, no processing implemented.";
          char* credential_auth_body = new char[credential_auth_body_length];
          oncrpc_record_input_stream.read( credential_auth_body, credential_auth_body_length, NULL );
          delete [] credential_auth_body;
        }
        uint32_t verf_auth_flavor = xdr_input_stream.readUint32( "verf_auth_flavor" );
        uint32_t verf_auth_body_length = xdr_input_stream.readUint32( "credential_auth_body_length" );
        if ( verf_auth_body_length > 0 )
        {
          if ( log != NULL )
            log->getStream( Log::LOG_WARNING ) << get_type_name() << ": received verification body, no processing implemented.";
          char* verf_auth_body = new char[verf_auth_body_length];
          oncrpc_record_input_stream.read( verf_auth_body, verf_auth_body_length, NULL );
          delete [] verf_auth_body;
        }
        if ( body == NULL && object_factories != NULL )
          body = object_factories->createObject( proc );
        if ( body != NULL )
          xdr_input_stream.readObject( XDRInputStream::Declaration(), body.get() );
        else
        {
          if ( log != NULL )
            log->getStream( Log::LOG_WARNING ) << get_type_name() << ": unknown procedure: " << proc << ".";
          return Stream::STREAM_STATUS_ERROR;
        }
        if ( log != NULL )
          log->getStream( Log::LOG_INFO ) << get_type_name() << ": successfully read " << body->get_type_name() << " body.";
        return Stream::STREAM_STATUS_OK;
      }
      else
      {
        if ( log != NULL )
          log->getStream( Log::LOG_WARNING ) << get_type_name() << ": unknown RPC version: " << rpcvers << ".";
        return Stream::STREAM_STATUS_ERROR;
      }
    }
    else
    {
      if ( log != NULL )
        log->getStream( Log::LOG_WARNING ) << get_type_name() << ": unknown msg_type: " << msg_type << ".";
      return Stream::STREAM_STATUS_ERROR;
    }
  }
  catch ( ONCRPCRecordInputStream::ReadException& exc )
  {
    if ( log != NULL )
      log->getStream( Log::LOG_INFO ) << get_type_name() << ": caught ONCRPCRecordInputStream::ReadException with status = " << static_cast<uint32_t>( exc.get_status() ) << ".";
    return exc.get_status();
  }
}
Stream::Status ONCRPCRequest::serialize( OutputStream& output_stream, size_t* out_bytes_written )
{
  ONCRPCRecordOutputStream oncrpc_record_output_stream;
  XDROutputStream xdr_output_stream( oncrpc_record_output_stream );
  uint32_t prog, proc, vers;
  if ( body != NULL )
  {
    if ( body->get_general_type() == Object::REQUEST )
    {
      Request* req = static_cast<Request*>( body.get() );
      xid = req->getDefaultResponseTypeId();
      proc = req->getOperationNumber();
      if ( proc == 0 )
        proc = req->get_type_id();
      prog = 0x20000000 + req->getInterfaceNumber(); // 0x20000000 = start of user-defined program numbers, from the RFC
      vers = req->getInterfaceNumber();
    }
    else
    {
      proc = body->get_type_id();
      prog = vers = 0;
    }
  }
  else
    proc = prog = vers = 0;
  xdr_output_stream.writeUint32( "xid", xid );
  xdr_output_stream.writeUint32( "msg_type", 0 ); // MSG_CALL
  xdr_output_stream.writeUint32( "rpcvers", 2 );
  xdr_output_stream.writeUint32( "prog", prog );
  xdr_output_stream.writeUint32( "vers", vers );
  xdr_output_stream.writeUint32( "proc", proc );
  xdr_output_stream.writeUint32( "credential_auth_flavor", credential_auth_flavor );
  if ( credential_auth_flavor == AUTH_NONE || credential == NULL )
    xdr_output_stream.writeUint32( "credential_auth_body_length", 0 );
  else
  {
    auto_Object<String> credential_auth_body = new String;
    XDROutputStream credential_auth_body_xdr_output_stream( *credential_auth_body );
    credential->serialize( credential_auth_body_xdr_output_stream );
    xdr_output_stream.writeString( "credential_auth_body", *credential_auth_body );
  }
  xdr_output_stream.writeUint32( "verf_auth_flavor", AUTH_NONE );
  xdr_output_stream.writeUint32( "verf_auth_body_length", 0 );
  xdr_output_stream.writeObject( XDROutputStream::Declaration(), *body );
  oncrpc_record_output_stream.freeze();
  Stream::Status write_status = output_stream.write( oncrpc_record_output_stream, out_bytes_written );
  if ( write_status == Stream::STREAM_STATUS_OK && log != NULL )
  {
    if ( log->get_level() >= Log::LOG_DEBUG )
    {
      Log::Stream log_stream = log->getStream( Log::LOG_DEBUG );
      log_stream << get_type_name() << ": wrote body = ";
      PrettyPrintOutputStream pretty_print_log_stream( log_stream );
      pretty_print_log_stream.writeObject( PrettyPrintOutputStream::Declaration(), *body );
      log_stream << ".";
    }
    else
      log->getStream( Log::LOG_INFO ) << get_type_name() << ": successfully wrote body = " << body->get_type_name() << ".";
  }
  return write_status;
}


// oncrpc_response.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
Stream::Status ONCRPCResponse::deserialize( InputStream& input_stream, size_t* out_bytes_read )
{
  XDRInputStream xdr_input_stream( get_oncrpc_record_input_stream( input_stream ) );
  try
  {
    xid = xdr_input_stream.readUint32( "xid" );
    int32_t msg_type = xdr_input_stream.readInt32( "msg_type" );
    if ( msg_type == 1 ) // REPLY
    {
      uint32_t reply_stat = xdr_input_stream.readUint32( "reply_stat" );
      if ( reply_stat == 0 ) // MSG_ACCEPTED
      {
        uint32_t verf_auth_flavor = xdr_input_stream.readUint32( "verf_auth_flavor" );
        uint32_t verf_auth_body_length = xdr_input_stream.readUint32( "verf_auth_body_length" );
        if ( verf_auth_flavor == 0 && verf_auth_body_length == 0 )
        {
          uint32_t accept_stat = xdr_input_stream.readUint32( "accept_stat" );
          if ( accept_stat == 0 ) // SUCCESS
          {
            if ( body == NULL )
              body = object_factories->createObject( xid );
          }
          else
          {
            try
            {
              std::string exception_type_name; xdr_input_stream.readString( "exception_type_name", exception_type_name );
              body = object_factories->createObject( exception_type_name.c_str() );
            }
            catch ( ONCRPCRecordInputStream::ReadException& )
            { }
            if ( body == NULL )
            {
              switch ( accept_stat )
              {
                case 1: body = new ExceptionResponse( "ONC-RPC exception: program unavailable" ); break;
                case 2: body = new ExceptionResponse( "ONC-RPC exception: program mismatch" ); break;
                case 3: body = new ExceptionResponse( "ONC-RPC exception: procedure unavailable" ); break;
                case 4: body = new ExceptionResponse( "ONC-RPC exception: garbage arguments" ); break;
                case 5:
                default: body = new ExceptionResponse( "ONC-RPC exception: system error" ); break;
              }
              if ( log != NULL )
                log->getStream( Log::LOG_WARNING ) << get_type_name() << ": " << static_cast<ExceptionResponse*>( body.get() )->what();
              return Stream::STREAM_STATUS_OK;
            }
          }
        }
        else
        {
          body = new ExceptionResponse( "ONC-RPC exception: received unexpected verification body on response" );
          if ( log != NULL )
            log->getStream( Log::LOG_WARNING ) << get_type_name() << ": " << static_cast<ExceptionResponse*>( body.get() )->what();
          return Stream::STREAM_STATUS_OK;
        }
      }
      else
      {
        if ( reply_stat == 1 ) // MSG_REJECTED
          body = new ExceptionResponse( "ONC-RPC exception: received MSG_REJECTED reply_stat" );
        else
          body = new ExceptionResponse( "ONC-RPC exception: received unknown reply_stat" );
        if ( log != NULL )
          log->getStream( Log::LOG_WARNING ) << get_type_name() << ": " << static_cast<ExceptionResponse*>( body.get() )->what();
        return Stream::STREAM_STATUS_OK;
      }
    }
    else
    {
      body = new ExceptionResponse( "ONC-RPC exception: received unknown msg_type" );
      if ( log != NULL )
        log->getStream( Log::LOG_WARNING ) << get_type_name() << ": " << static_cast<ExceptionResponse*>( body.get() )->what();
      return Stream::STREAM_STATUS_OK;
    }
    xdr_input_stream.readObject( XDRInputStream::Declaration(), body.get() );
    if ( log != NULL )
    {
      if ( log->get_level() >= Log::LOG_DEBUG )
      {
        Log::Stream log_stream = log->getStream( Log::LOG_DEBUG );
        log_stream << get_type_name() << ": successfully read body = ";
        PrettyPrintOutputStream pretty_print_log_stream( log_stream );
        pretty_print_log_stream.writeObject( PrettyPrintOutputStream::Declaration(), *body );
        log_stream << ".";
      }
      else
        log->getStream( Log::LOG_INFO ) << get_type_name() << ": successfully read body = " << body->get_type_name() << ".";
    }
    return Stream::STREAM_STATUS_OK;
  }
  catch ( ONCRPCRecordInputStream::ReadException& exc )
  {
    if ( log != NULL )
      log->getStream( Log::LOG_INFO ) << get_type_name() << ": caught ONCRPCRecordInputStream::ReadException with status = " << static_cast<uint32_t>( exc.get_status() ) << ".";
    return exc.get_status();
  }
}
Stream::Status ONCRPCResponse::serialize( OutputStream& output_stream, size_t* out_bytes_written )
{
  ONCRPCRecordOutputStream oncrpc_record_output_stream;
  XDROutputStream xdr_output_stream( oncrpc_record_output_stream );
  xdr_output_stream.writeUint32( "xid", xid ); // xid of original request
  xdr_output_stream.writeUint32( "msg_type", 1 ); // MSG_REPLY
  xdr_output_stream.writeUint32( "reply_stat", 0 ); // MSG_ACCEPTED
  xdr_output_stream.writeUint32( "verf_auth_flavor", 0 );
  xdr_output_stream.writeUint32( "verf_auth_body_length", 0 );
  if ( body->get_general_type() != Object::EXCEPTION_RESPONSE )
    xdr_output_stream.writeUint32( "accept_stat", 0 ); // SUCCESS
  else
    xdr_output_stream.writeUint32( "accept_stat", 5 ); // SYSTEM_ERR
  xdr_output_stream.writeObject( XDROutputStream::Declaration(), *body );
  oncrpc_record_output_stream.freeze();
  return output_stream.write( oncrpc_record_output_stream, out_bytes_written );
}


// rfc822_headers.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
RFC822Headers::RFC822Headers()
{
  deserialize_state = DESERIALIZING_LEADING_WHITESPACE;
//#ifdef _DEBUG
//  memset( stack_buffer, 0, sizeof ( stack_buffer ) );
//#endif
  buffer_p = stack_buffer;
  heap_buffer = NULL;
  heap_buffer_len = 0;
  heap_iovecs = NULL;
  iovecs_filled = 0;
}
RFC822Headers::~RFC822Headers()
{
  delete [] heap_buffer;
}
void RFC822Headers::allocateHeapBuffer()
{
  if ( heap_buffer_len == 0 )
  {
    heap_buffer = new char[512];
    heap_buffer_len = 512;
    memcpy( heap_buffer, stack_buffer, buffer_p - stack_buffer );
    buffer_p = heap_buffer + ( buffer_p - stack_buffer );
  }
  else
  {
    heap_buffer_len += 512;
    char* new_heap_buffer = new char[heap_buffer_len];
    memcpy( new_heap_buffer, heap_buffer, buffer_p - heap_buffer );
    buffer_p = new_heap_buffer + ( buffer_p - heap_buffer );
    delete [] heap_buffer;
    heap_buffer = new_heap_buffer;
  }
}
void RFC822Headers::copy_iovec( const char* data, size_t len )
{
  if ( heap_buffer == NULL )
  {
    if ( ( buffer_p + len - stack_buffer ) > YIELD_IPC_RFC822_HEADERS_STACK_BUFFER_LENGTH )
    {
      heap_buffer = new char[len];
      heap_buffer_len = len;
      // Don't need to copy anything from the stack buffer or change pointers, since we're not deleting that memory or parsing over it again
      buffer_p = heap_buffer;
    }
  }
  else if ( static_cast<size_t>( buffer_p + len - heap_buffer ) > heap_buffer_len )
  {
    heap_buffer_len += len;
    char* new_heap_buffer = new char[heap_buffer_len];
    memcpy( new_heap_buffer, heap_buffer, buffer_p - heap_buffer );
    // Since we're copying the old heap_buffer and deleting its contents we need to adjust the pointers
    struct iovec* iovecs = ( heap_iovecs == NULL ) ? stack_iovecs : heap_iovecs;
    for ( uint8_t iovec_i = 0; iovec_i < iovecs_filled; iovec_i++ )
    {
      if ( iovecs[iovec_i].iov_base >= heap_buffer && iovecs[iovec_i].iov_base <= buffer_p )
        iovecs[iovec_i].iov_base = new_heap_buffer + ( static_cast<char*>( iovecs[iovec_i].iov_base ) - heap_buffer );
    }
    buffer_p = new_heap_buffer + ( buffer_p - heap_buffer );
    delete [] heap_buffer;
    heap_buffer = new_heap_buffer;
  }
  char* buffer_p_before = buffer_p;
  memcpy( buffer_p, data, len );
  buffer_p += len;
  if ( data[len-1] == 0 ) len--;
  set_iovec( buffer_p_before, len );
}
Stream::Status RFC822Headers::deserialize( InputStream& input_stream, size_t* bytes_read )
{
  for ( ;; )
  {
    switch ( deserialize_state )
    {
      case DESERIALIZING_LEADING_WHITESPACE:
      {
        char c;
        for ( ;; )
        {
          Stream::Status read_status = input_stream.read( &c, 1, NULL );
          if ( read_status == Stream::STREAM_STATUS_OK )
          {
            if ( isspace( c ) )
              continue;
            else
            {
              *buffer_p = c;
              buffer_p++; // Don't need to check the end of the buffer here
              deserialize_state = DESERIALIZING_HEADER_NAME;
              break;
            }
          }
          else
            return read_status;
        }
      }
      // Fall through
      case DESERIALIZING_HEADER_NAME:
      {
        char c;
        Stream::Status read_status = input_stream.read( &c, 1, NULL );
        if ( read_status == Stream::STREAM_STATUS_OK )
        {
          switch ( c )
          {
            case '\r':
            case '\n': deserialize_state = DESERIALIZING_TRAILING_CRLF; continue;
            // TODO: support folded lines here (look for isspace( c ), if so it's an extension of the previous line
            default:
            {
              *buffer_p = c;
              advanceBufferPointer();
              for ( ;; )
              {
                read_status = input_stream.read( buffer_p, 1, NULL );
                if ( read_status == Stream::STREAM_STATUS_OK )
                {
                  if ( *buffer_p == ':' )
                  {
                    *buffer_p = 0;
                    advanceBufferPointer();
                    deserialize_state = DESERIALIZING_HEADER_NAME_VALUE_SEPARATOR;
                    break;
                  }
                  else
                    advanceBufferPointer();
                }
                else
                  return read_status;
              }
            }
            break;
          }
        }
        else
          return read_status;
      }
      // Fall through
      case DESERIALIZING_HEADER_NAME_VALUE_SEPARATOR:
      {
        char c;
        for ( ;; )
        {
          Stream::Status read_status = input_stream.read( &c, 1, NULL );
          if ( read_status == Stream::STREAM_STATUS_OK )
          {
            if ( isspace( c ) )
              continue;
            else
            {
              *buffer_p = c;
              advanceBufferPointer();
              deserialize_state = DESERIALIZING_HEADER_VALUE;
              break;
            }
          }
          else
            return read_status;
        }
      }
      // Fall through
      case DESERIALIZING_HEADER_VALUE:
      {
        for ( ;; )
        {
          Stream::Status read_status = input_stream.read( buffer_p, 1, NULL );
          if ( read_status == Stream::STREAM_STATUS_OK )
          {
            if ( *buffer_p == '\r' )
            {
              *buffer_p = 0;
              advanceBufferPointer();
              deserialize_state = DESERIALIZING_HEADER_VALUE_TERMINATOR;
              break;
            }
            else
              advanceBufferPointer();
          }
          else
            return read_status;
        }
      }
      // Fall through
      case DESERIALIZING_HEADER_VALUE_TERMINATOR:
      {
        char c;
        for ( ;; )
        {
          Stream::Status read_status = input_stream.read( &c, 1, NULL );
          if ( read_status == Stream::STREAM_STATUS_OK )
          {
            if ( c == '\n' )
            {
              deserialize_state = DESERIALIZING_HEADER_NAME;
              break;
            }
          }
          else
            return read_status;
        }
      }
      continue; // To the next header name
      case DESERIALIZING_TRAILING_CRLF:
      {
        char c;
        for ( ;; )
        {
          Stream::Status read_status = input_stream.read( &c, 1, NULL );
          if ( read_status == Stream::STREAM_STATUS_OK )
          {
            if ( c == '\n' )
            {
              *buffer_p = 0;
              // Fill the iovecs so get_header will work
              // TODO: do this as we're parsing
              const char* temp_buffer_p = heap_buffer ? heap_buffer : stack_buffer;
              while ( temp_buffer_p < buffer_p )
              {
                const char* header_name = temp_buffer_p;
                size_t header_name_len = std::strlen( header_name );
                temp_buffer_p += header_name_len + 1;
                const char* header_value = temp_buffer_p;
                size_t header_value_len = std::strlen( header_value );
                temp_buffer_p += header_value_len + 1;
                set_iovec( header_name, header_name_len );
                set_iovec( ": ", 2 );
                set_iovec( header_value, header_value_len );
                set_iovec( "\r\n", 2 );
              }
              deserialize_state = DESERIALIZE_DONE;
              return Stream::STREAM_STATUS_OK;
            }
          }
          else
            return read_status;
        }
        case DESERIALIZE_DONE: return Stream::STREAM_STATUS_OK;
      }
    } // switch
  } // for ( ;; )
}
char* RFC822Headers::get_header( const char* header_name, const char* default_value )
{
  struct iovec* iovecs = heap_iovecs != NULL ? heap_iovecs : stack_iovecs;
  for ( uint8_t iovec_i = 0; iovec_i < iovecs_filled; iovec_i += 4 )
  {
    if ( strncmp( static_cast<const char*>( iovecs[iovec_i].iov_base ), header_name, iovecs[iovec_i].iov_len ) == 0 )
      return static_cast<char*>( iovecs[iovec_i+2].iov_base );
  }
  return const_cast<char*>( default_value );
}
Stream::Status RFC822Headers::serialize( OutputStream& output_stream, size_t* out_bytes_written )
{
  struct iovec* iovecs = heap_iovecs != NULL ? heap_iovecs : stack_iovecs;
  return output_stream.writev( iovecs, iovecs_filled, out_bytes_written );
}
void RFC822Headers::set_header( const char* header, size_t header_len )
{
  DebugBreak(); // TODO: Separate header name and value
  /*
  if ( header[header_len-1] != '\n' )
  {
    copy_iovec( header, header_len );
    set_iovec( "\r\n", 2 );
  }
  else
    copy_iovec( header, header_len );
    */
}
void RFC822Headers::set_header( const char* header_name, const char* header_value )
{
  set_iovec( header_name, strlen( header_name ) );
  set_iovec( ": ", 2 );
  set_iovec( header_value, strlen( header_value ) );
  set_iovec( "\r\n", 2 );
}
void RFC822Headers::set_header( const char* header_name, char* header_value )
{
  set_iovec( header_name, strlen( header_name ) );
  set_iovec( ": ", 2 );
  copy_iovec( header_value, strlen( header_value ) );
  set_iovec( "\r\n", 2 );
}
void RFC822Headers::set_header( char* header_name, char* header_value )
{
  copy_iovec( header_name, strlen( header_name ) );
  set_iovec( ": ", 2 );
  copy_iovec( header_value, strlen( header_value ) );
  set_iovec( "\r\n", 2 );
}
void RFC822Headers::set_header( const std::string& header_name, const std::string& header_value )
{
  copy_iovec( header_name.c_str(), header_name.size() );
  set_iovec( ": ", 2 );
  copy_iovec( header_value.c_str(), header_value.size() );
  set_iovec( "\r\n", 2 );
}
void RFC822Headers::set_iovec( const struct iovec& iovec )
{
  if ( heap_iovecs == NULL )
  {
    if ( iovecs_filled < YIELD_IPC_RFC822_HEADERS_STACK_IOVECS_LENGTH )
      stack_iovecs[iovecs_filled] = iovec;
    else
    {
      heap_iovecs = new struct iovec[UCHAR_MAX];
      memcpy( heap_iovecs, stack_iovecs, sizeof( stack_iovecs ) );
      heap_iovecs[iovecs_filled] = iovec;
    }
  }
  else if ( iovecs_filled < UCHAR_MAX )
    heap_iovecs[iovecs_filled] = iovec;
  else
    DebugBreak();
  iovecs_filled++;
}


// socket.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).


#ifdef _WIN32
#include <ws2tcpip.h>
#pragma comment( lib, "ws2_32.lib" )
#else
#include <netinet/in.h> // For the IPPROTO_* constants
#include <netinet/tcp.h> // For the TCP_* constants
#include <sys/socket.h>
#endif


Socket::Socket( int domain, int type, int protocol, auto_Object<Log> log )
  : log( log ), domain( domain ), type( type ), protocol( protocol )
{
  _socket = static_cast<socket_t>( -1 );
  blocking_mode = true;
}

Socket::Socket( socket_t _socket, auto_Object<Log> log )
  : log( log ), _socket( _socket )
{
  domain = type = protocol = 0;
  blocking_mode = true;
}

bool Socket::bind( const SocketAddress& bind_to_sockaddr )
{
  // SO_REUSEADDR caused a double binding on Win32 (= old socket intercepted new socket's conns)
//	int flag = 1;
//	setsockopt( bound_socket, SOL_SOCKET, SO_REUSEADDR, ( const char* ) & flag, 1 );
  // No SO_REUSEADDR: we want the bind to fail if an old port is still active, otherwise we won't be able to accept connections correctly
  if ( _socket == static_cast<socket_t>( -1 ) )
    domain = bind_to_sockaddr.get_family();
  struct sockaddr_storage bind_to_sockaddr_storage = bind_to_sockaddr;
  return ::bind( *this, reinterpret_cast<struct sockaddr*>( &bind_to_sockaddr_storage ), sizeof( bind_to_sockaddr_storage ) ) != -1;
}

bool Socket::close()
{
  if ( _socket != static_cast<socket_t>( -1 ) )
  {
    if ( _close( _socket ) )
    {
      _socket = static_cast<socket_t>( -1 );
      return true;
    }
    else
      return false;
  }
  else
    return true;
}

bool Socket::_close( socket_t _socket )
{
#ifdef _WIN32
    return ::closesocket( _socket ) != SOCKET_ERROR;
#else
    return ::close( _socket ) != -1;
#endif
}

Stream::Status Socket::connect( const SocketAddress& connect_to_sockaddr )
{
  if ( _socket == static_cast<socket_t>( -1 ) )
    domain = connect_to_sockaddr.get_family();

  for ( ;; )
  {
    struct sockaddr_storage connect_to_sockaddr_storage = connect_to_sockaddr;
    if ( ::connect( *this, reinterpret_cast<struct sockaddr*>( &connect_to_sockaddr_storage ), sizeof( connect_to_sockaddr_storage ) ) != -1 )
      return STREAM_STATUS_OK;
    else
    {
#ifdef _WIN32
      switch ( ::WSAGetLastError() )
      {
        case WSAEISCONN: return STREAM_STATUS_OK;
        case WSAEWOULDBLOCK:
        case WSAEINPROGRESS:
        case WSAEINVAL: return STREAM_STATUS_WANT_WRITE;
        case WSAEAFNOSUPPORT:
        {
          if ( domain == AF_INET6 )
          {
            ::closesocket( _socket );
            _socket = static_cast<socket_t>( -1 );
            domain = AF_INET; // Fall back to IPv4
            continue; // Try to connect again
          }
        }
        break;
      }
#else
      switch ( errno )
      {
        case EISCONN: return STREAM_STATUS_OK;
        case EWOULDBLOCK:
        case EINPROGRESS: return STREAM_STATUS_WANT_WRITE;
        case EAFNOSUPPORT:
        {
          if ( domain == AF_INET6 )
          {
            ::close( _socket );
            _socket = static_cast<socket_t>( -1 );
            domain = AF_INET; // Fall back to IPv4
            continue; // Try to connect again
          }
        }
        break;
      }
  #endif

      return STREAM_STATUS_ERROR;
    }
  }
}

auto_Object<SocketAddress> Socket::getpeername()
{
  struct sockaddr_storage peername_sockaddr_storage;
  memset( &peername_sockaddr_storage, 0, sizeof( peername_sockaddr_storage ) );
  socklen_t peername_sockaddr_storage_len = sizeof( peername_sockaddr_storage );
  if ( ::getpeername( *this, reinterpret_cast<struct sockaddr*>( &peername_sockaddr_storage ), &peername_sockaddr_storage_len ) != -1 )
    return new SocketAddress( peername_sockaddr_storage );
  else
    return NULL;
}

auto_Object<SocketAddress> Socket::getsockname()
{
  struct sockaddr_storage sockname_sockaddr_storage;
  memset( &sockname_sockaddr_storage, 0, sizeof( sockname_sockaddr_storage ) );
  socklen_t sockname_sockaddr_storage_len = sizeof( sockname_sockaddr_storage );
  if ( ::getsockname( *this, reinterpret_cast<struct sockaddr*>( &sockname_sockaddr_storage ), &sockname_sockaddr_storage_len ) != -1 )
    return new SocketAddress( sockname_sockaddr_storage );
  else
    return NULL;
}

Socket::operator socket_t()
{
  if  ( _socket == static_cast<socket_t>( -1 ) )
  {
    _socket = static_cast<socket_t>( ::socket( domain, type, protocol ) );
#ifdef _WIN32
    if ( _socket != static_cast<socket_t>( -1 ) )
    {
      // Allow dual-stack sockets
      DWORD ipv6only = 0;
      setsockopt( _socket, IPPROTO_IPV6, IPV6_V6ONLY, ( char* )&ipv6only, sizeof( ipv6only ) );
    }
    else if ( domain == AF_INET6 && ::WSAGetLastError() == WSAEAFNOSUPPORT )
      _socket = static_cast<socket_t>( ::socket( AF_INET, type, protocol ) );
#else
    if ( _socket == static_cast<socket_t>( -1 ) && domain == AF_INET6 && errno == EAFNOSUPPORT )
      _socket = static_cast<socket_t>( ::socket( AF_INET, type, protocol ) );
#endif

    if ( !get_blocking_mode() )
      set_blocking_mode( false );
  }

  return _socket;
}

Stream::Status Socket::read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
{
#if defined(_WIN32)
  int recv_ret = ::recv( *this, static_cast<char*>( buffer ), static_cast<int>( buffer_len ), 0 ); // No real advantage to WSARecv on Win32 for one buffer..
#elif defined(__linux)
  ssize_t recv_ret = ::recv( *this, buffer, buffer_len, MSG_NOSIGNAL );
#else
  ssize_t recv_ret = ::recv( *this, buffer, buffer_len, 0 );
#endif

  if ( recv_ret > 0 )
  {
    if ( log != NULL )
    {
      Log::Stream log_stream = log->getStream( Log::LOG_DEBUG );
      log_stream << "Socket: read " << this << ": ";
      log_stream.write( buffer, buffer_len );
    }

    if ( out_bytes_read )
      *out_bytes_read = static_cast<size_t>( recv_ret );

    return STREAM_STATUS_OK;
  }
  else if ( recv_ret == 0 )
  {
    if ( log != NULL )
      log->getStream( Log::LOG_INFO ) << "Socket: lost connection with recv_ret = 0.";

#ifdef _WIN32
    ::WSASetLastError( WSAECONNRESET );
#else
    errno = ECONNRESET;
#endif

    return STREAM_STATUS_ERROR;
  }
#ifdef _WIN32
  else if ( ::WSAGetLastError() == WSAEWOULDBLOCK || ::WSAGetLastError() == WSAEINPROGRESS )
#else
  else if ( errno == EWOULDBLOCK )
#endif
  {
    if ( log != NULL )
      log->getStream( Log::LOG_INFO ) << "Socket: would block on read on " << this << ", tried to read buffer_len = " << buffer_len << ".";

    return STREAM_STATUS_WANT_READ;
  }
  else
  {
    if ( log != NULL && log->get_level() >= Log::LOG_INFO )
      log->getStream( Log::LOG_INFO ) << "Socket: lost connection on read on " << this << ", error = " << Exception::strerror() << ".";

    return STREAM_STATUS_ERROR;
  }
}

bool Socket::set_blocking_mode( bool blocking )
{
#ifdef _WIN32
  unsigned long val = blocking ? 0 : 1;
  if ( ioctlsocket( *this, FIONBIO, &val ) != SOCKET_ERROR )
  {
    this->blocking_mode = blocking;
    return true;
  }
  else
    return false;
#else
  int current_fcntl_flags = fcntl( *this, F_GETFL, 0 );
  if ( blocking )
  {
    if ( ( current_fcntl_flags & O_NONBLOCK ) == O_NONBLOCK )
    {
      if ( fcntl( *this, F_SETFL, current_fcntl_flags ^ O_NONBLOCK ) != -1 )
      {
        this->blocking_mode = true;
        return true;
      }
      else
        return false;
    }
    else
      return true;
  }
  else
  {
    if ( fcntl( *this, F_SETFL, current_fcntl_flags | O_NONBLOCK ) != -1 )
    {
      this->blocking_mode = false;
      return true;
    }
    else
      return false;
  }
#endif
}

Stream::Status Socket::writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
{
#ifdef _WIN32
  DWORD dwWrittenLength;
  int send_ret = ::WSASend( *this, reinterpret_cast<WSABUF*>( const_cast<struct iovec*>( buffers ) ), buffers_count, &dwWrittenLength, 0, NULL, NULL );
#else
  // Use sendmsg instead of writev to pass flags on Linux
  struct msghdr _msghdr;
  memset( &_msghdr, 0, sizeof( _msghdr ) );
  _msghdr.msg_iov = const_cast<iovec*>( buffers );
  _msghdr.msg_iovlen = buffers_count;
#ifdef __linux
  ssize_t send_ret = ::sendmsg( *this, &_msghdr, MSG_NOSIGNAL ); // MSG_NOSIGNAL = disable SIGPIPE
#else
  ssize_t send_ret = ::sendmsg( *this, &_msghdr, 0 );
#endif
#endif
  if ( send_ret >= 0 )
  {
    if ( log != NULL )
    {
      Log::Stream log_stream = log->getStream( Log::LOG_DEBUG );
      log_stream << "Socket: write on " << this << ": ";
      log_stream.writev( buffers, buffers_count );
    }

    if ( out_bytes_written )
#ifdef _WIN32
      *out_bytes_written = static_cast<size_t>( dwWrittenLength );
#else
      *out_bytes_written = static_cast<size_t>( send_ret );
#endif

    return STREAM_STATUS_OK;
  }
#ifdef _WIN32
  else if ( ::WSAGetLastError() == WSAEWOULDBLOCK || ::WSAGetLastError() == WSAEINPROGRESS )
#else
  else if ( errno == EWOULDBLOCK )
#endif
  {
    if ( log != NULL && log->get_level() >= Log::LOG_INFO )
      log->getStream( Log::LOG_INFO ) << "Socket: would block on write on " << this << ".";

    return STREAM_STATUS_WANT_WRITE;
  }
  else
  {
    if ( log != NULL && log->get_level() >= Log::LOG_INFO )
      log->getStream( Log::LOG_INFO ) << "Socket: lost connection on write on " << this << ", error = " << Exception::strerror() << ".";

    return STREAM_STATUS_ERROR;
  }
}


// socket_address.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#include <ws2tcpip.h>
#else
#include <arpa/inet.h>
#include <netdb.h>
#include <sys/socket.h>
#endif
SocketAddress::SocketAddress()
{
  init( "localhost", 0 ); // Use "localhost" instead of the IPv6 loopback constant to allow for IPv4-only systems
}
SocketAddress::SocketAddress( uint16_t port, bool loopback )
{
  if ( loopback )
    init( "localhost", port ); // Use "localhost" instead of the IPv6 loopback constant to allow for IPv4-only systems
  else // INADDR_ANY
  {
    _sockaddr_storage = new struct sockaddr_storage;
    memset( _sockaddr_storage, 0, sizeof( *_sockaddr_storage ) );
    socket_t test_socket = static_cast<socket_t>( ::socket( AF_INET6, SOCK_STREAM, IPPROTO_TCP ) );
    if ( test_socket != static_cast<socket_t>( -1 ) ) // Only real way to test address family support, as far as I can tell
      reinterpret_cast<sockaddr_in6*>( _sockaddr_storage )->sin6_family = AF_INET6;
    else
      reinterpret_cast<sockaddr_in6*>( _sockaddr_storage )->sin6_family = AF_INET;
#ifdef _WIN32
    ::closesocket( test_socket );
#else
    ::close( test_socket );
#endif
    reinterpret_cast<sockaddr_in6*>( _sockaddr_storage )->sin6_port = htons( port );
  }
}
SocketAddress::SocketAddress( const char* hostname )
{
  init( hostname, 0 );
}
SocketAddress::SocketAddress( const char* hostname, uint16_t port )
{
  init( hostname, port );
}
SocketAddress::SocketAddress( const URI& uri )
{
  init( uri.get_host().c_str(), uri.get_port() );
}
SocketAddress::SocketAddress( const struct sockaddr_storage& _sockaddr_storage )
{
  init( _sockaddr_storage );
}
SocketAddress::SocketAddress( const SocketAddress& other )
{
  init( *other._sockaddr_storage );
}
SocketAddress::~SocketAddress()
{
  delete _sockaddr_storage;
}
int SocketAddress::get_family() const
{
  return _sockaddr_storage->ss_family;
}
bool SocketAddress::getnameinfo( std::string& out_hostname, bool numeric ) const
{
  char nameinfo[NI_MAXHOST];
  if ( this->getnameinfo( nameinfo, NI_MAXHOST, numeric ) )
  {
    out_hostname.assign( nameinfo );
    return true;
  }
  else
    return false;
}
bool SocketAddress::getnameinfo( char* out_hostname, uint32_t out_hostname_len, bool numeric ) const
{
  return ::getnameinfo( reinterpret_cast<struct sockaddr*>( _sockaddr_storage ), sizeof( *_sockaddr_storage ), out_hostname, out_hostname_len, NULL, 0, numeric ? NI_NUMERICHOST : 0 ) == 0;
}
uint16_t SocketAddress::get_port() const
{
  switch ( _sockaddr_storage->ss_family )
  {
    case AF_INET: return ntohs( reinterpret_cast<struct sockaddr_in*>( _sockaddr_storage )->sin_port );
    case AF_INET6: return ntohs( reinterpret_cast<struct sockaddr_in6*>( _sockaddr_storage )->sin6_port );
    default: return 0;
  }
}
SocketAddress::operator struct sockaddr_storage() const
{
  return *_sockaddr_storage;
}
bool SocketAddress::operator==( const SocketAddress& other ) const
{
  return std::memcmp( _sockaddr_storage, other._sockaddr_storage, sizeof( *_sockaddr_storage ) ) == 0;
}
bool SocketAddress::operator!=( const SocketAddress& other ) const
{
  return std::memcmp( _sockaddr_storage, other._sockaddr_storage, sizeof( *_sockaddr_storage ) ) != 0;
}
void SocketAddress::init( const char* hostname, uint16_t port )
{
  if ( hostname != NULL && std::strlen( hostname ) > 0 )
  {
    std::ostringstream servname; // ltoa is not very portable
    servname << port; // servname = decimal port or service name. Great interface, guys.
    struct addrinfo addrinfo_hints;
    memset( &addrinfo_hints, 0, sizeof( addrinfo_hints ) );
    addrinfo_hints.ai_family = AF_UNSPEC;
    addrinfo_hints.ai_flags = AI_ALL|AI_V4MAPPED;
    struct addrinfo *addrinfo_list = NULL;
    int getaddrinfo_ret = ::getaddrinfo( hostname, servname.str().c_str(), &addrinfo_hints, &addrinfo_list );
    if ( getaddrinfo_ret == 0 )
    {
      _sockaddr_storage = new struct sockaddr_storage;
      memset( _sockaddr_storage, 0, sizeof( *_sockaddr_storage ) );
      memcpy( _sockaddr_storage, addrinfo_list[0].ai_addr, addrinfo_list[0].ai_addrlen );
      freeaddrinfo( addrinfo_list );
    }
    else
    {
      _sockaddr_storage = NULL;
#ifdef _WIN32
      throw Exception( getaddrinfo_ret );
#else
      throw Exception( gai_strerror( getaddrinfo_ret ) );
#endif
    }
  }
  else
    init( "localhost", port );
}
void SocketAddress::init( const struct sockaddr_storage& _sockaddr_storage )
{
  this->_sockaddr_storage = new sockaddr_storage;
  memcpy( this->_sockaddr_storage, &_sockaddr_storage, sizeof( *this->_sockaddr_storage ) );
}


// tcp_socket.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if defined(_WIN32)
#include <ws2tcpip.h>
#else
#include <netinet/in.h> // For the IPPROTO_* constants
#include <netinet/tcp.h> // For the TCP_* constants
#include <sys/socket.h>
#endif
#include <cstring>
TCPSocket::TCPSocket( auto_Object<Log> log )
  : Socket( AF_INET6, SOCK_STREAM, IPPROTO_TCP, log )
{
  partial_write_len = 0;
}
TCPSocket::TCPSocket( socket_t _socket, auto_Object<Log> log )
  : Socket( _socket, log )
{
  partial_write_len = 0;
}
auto_Object<TCPSocket> TCPSocket::accept()
{
  socket_t peer_socket = _accept();
  if ( peer_socket != static_cast<socket_t>( -1 ) )
    return new TCPSocket( peer_socket, log );
  else
    return NULL;
}
socket_t TCPSocket::_accept()
{
  sockaddr_storage peer_sockaddr_storage;
  socklen_t peer_sockaddr_storage_len = sizeof( peer_sockaddr_storage );
  return static_cast<socket_t>( ::accept( *this, ( struct sockaddr* )&peer_sockaddr_storage, &peer_sockaddr_storage_len ) );
}
bool TCPSocket::listen()
{
  int flag = 1;
  setsockopt( *this, IPPROTO_TCP, TCP_NODELAY, reinterpret_cast<char*>( &flag ), sizeof( int ) );
  flag = 1;
  setsockopt( *this, SOL_SOCKET, SO_KEEPALIVE, reinterpret_cast<char*>( &flag ), sizeof( int ) );
  linger lingeropt;
  lingeropt.l_onoff = 1;
  lingeropt.l_linger = 0;
  setsockopt( *this, SOL_SOCKET, SO_LINGER, ( char* )&lingeropt, ( int )sizeof( lingeropt ) );
  return ::listen( *this, SOMAXCONN ) != -1;
}
bool TCPSocket::shutdown()
{
#ifdef _WIN32
  return ::shutdown( *this, SD_BOTH ) == 0;
#else
  return ::shutdown( *this, SHUT_RDWR ) != -1;
#endif
}
Stream::Status TCPSocket::writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
{
  size_t buffers_len = 0;
  for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
    buffers_len += buffers[buffer_i].iov_len;
  if ( out_bytes_written )
    *out_bytes_written = 0;
  for ( ;; )
  {
    // Recalculate these every time we do a socket writev
    // Less efficient than other ways but it reduces the number of (rarely-tested) branches
    uint32_t wrote_until_buffer_i = 0;
    size_t wrote_until_buffer_i_pos = 0;
    if ( partial_write_len > 0 )
    {
      size_t temp_partial_write_len = partial_write_len;
      for ( ;; )
      {
        if ( buffers[wrote_until_buffer_i].iov_len < temp_partial_write_len ) // The buffer and part of the next was already written
        {
          temp_partial_write_len -= buffers[wrote_until_buffer_i].iov_len;
          wrote_until_buffer_i++;
        }
        else if ( buffers[wrote_until_buffer_i].iov_len == temp_partial_write_len ) // The buffer was already written, but none of the next
        {
          temp_partial_write_len = 0;
          wrote_until_buffer_i++;
          break;
        }
        else // Part of the buffer was written
        {
          wrote_until_buffer_i_pos = temp_partial_write_len;
          break;
        }
      }
    }
    Stream::Status writev_status;
    size_t temp_bytes_written = 0;
    if ( wrote_until_buffer_i_pos == 0 ) // Writing whole buffers
      writev_status = Socket::writev( &buffers[wrote_until_buffer_i], buffers_count - wrote_until_buffer_i, &temp_bytes_written );
    else // Writing part of a buffer
    {
      struct iovec temp_iovec;
      temp_iovec.iov_base = static_cast<char*>( buffers[wrote_until_buffer_i].iov_base ) + wrote_until_buffer_i_pos;
      temp_iovec.iov_len = buffers[wrote_until_buffer_i].iov_len - wrote_until_buffer_i_pos;
      writev_status = Socket::writev( &temp_iovec, 1, &temp_bytes_written );
    }
    if ( writev_status == STREAM_STATUS_OK )
    {
      if ( out_bytes_written )
        *out_bytes_written += temp_bytes_written;
      partial_write_len += temp_bytes_written;
      if ( partial_write_len == buffers_len )
      {
        partial_write_len = 0;
        return STREAM_STATUS_OK;
      }
      else
        continue; // A large write filled the socket buffer, try to write again until we finish or get an error
    }
    else
    {
      if ( temp_bytes_written > 0 )
        DebugBreak();
      return writev_status;
    }
  }
}


// udp_socket.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if defined(_WIN32)
#include <ws2tcpip.h>
#else
#include <netinet/in.h> // For the IPPROTO_* constants
#include <sys/socket.h>
#endif
UDPSocket::UDPSocket( auto_Object<Log> log )
  : Socket( AF_INET6, SOCK_DGRAM, IPPROTO_UDP, log )
{ }
bool UDPSocket::joinMulticastGroup( const SocketAddress& multicast_group_sockaddr, bool loopback )
{
  /*
  struct ip_mreq mreq;
  memset( &mreq, 0, sizeof( mreq ) );
  mreq.imr_interface.s_addr = INADDR_ANY;
  mreq.imr_multiaddr.s_addr = htonl( multicast_group_sockaddr.get_ipv4() );
  bool success = setsockopt( this, IPPROTO_IP, IP_ADD_MEMBERSHIP, reinterpret_cast<char*>( &mreq ), sizeof( mreq ) ) == 0;
  if ( success && !loopback )
  {
    unsigned char loop = 0;
    setsockopt( this, IPPROTO_IP, IP_MULTICAST_LOOP, reinterpret_cast<const char*>( &loop ), sizeof( loop ) );
  }
  return success;
  */
  return false;
}
bool UDPSocket::leaveMulticastGroup( const SocketAddress& multicast_group_sockaddr )
{
  /*
  struct ip_mreq mreq;
  memset( &mreq, 0, sizeof( mreq ) );
  mreq.imr_interface.s_addr = INADDR_ANY;
  mreq.imr_multiaddr.s_addr = htonl( multicast_group_sockaddr.get_ipv4() );
  return setsockopt( this, IPPROTO_IP, IP_DROP_MEMBERSHIP, reinterpret_cast<char*>( &mreq ), sizeof( mreq ) ) == 0;
  */
  return false;
}
Stream::Status UDPSocket::read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
{
  sockaddr_storage recvfrom_sockaddr_storage;
  socklen_t recvfrom_sockaddr_storage_len = sizeof( recvfrom_sockaddr_storage );
  int read_ret = ::recvfrom( *this,
                             static_cast<char*>( buffer ),
#ifdef _WIN32
                             static_cast<int>( buffer_len ),
#else
                             buffer_len,
#endif
                             0,
                             reinterpret_cast<struct sockaddr*>( &recvfrom_sockaddr_storage ),
                             &recvfrom_sockaddr_storage_len );
  if ( read_ret > 0 )
  {
    if ( out_bytes_read )
      *out_bytes_read = static_cast<size_t>( read_ret );
    if ( log != NULL )
    {
      log->write( "Socket read: ", Log::LOG_DEBUG );
      log->write( buffer, static_cast<size_t>( read_ret ), Log::LOG_DEBUG );
      log->write( "\n", Log::LOG_DEBUG );
    }
    return STREAM_STATUS_OK;
  }
  else
    return STREAM_STATUS_ERROR;
}


// uri.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
extern "C"
{
  #include <uriparser.h>
};
URI::URI( const URI& other )
: scheme( other.scheme ), user( other.user ), password( other.password ),
  host( other.host ), port( other.port ), resource( other.resource )
{ }
auto_Object<URI> URI::parse( const char* uri, size_t uri_len )
{
  UriParserStateA parser_state;
  UriUriA parsed_uri;
  parser_state.uri = &parsed_uri;
  if ( uriParseUriExA( &parser_state, uri, uri + uri_len ) == URI_SUCCESS )
  {
    URI* uri = new URI( parsed_uri );
    uriFreeUriMembersA( &parsed_uri );
    return uri;
  }
  else
  {
    uriFreeUriMembersA( &parsed_uri );
    return NULL;
  }
}
void URI::init( const char* uri, size_t uri_len )
{
  UriParserStateA parser_state;
  UriUriA parsed_uri;
  parser_state.uri = &parsed_uri;
  if ( uriParseUriExA( &parser_state, uri, uri + uri_len ) == URI_SUCCESS )
  {
    init( parsed_uri );
    uriFreeUriMembersA( &parsed_uri );
  }
  else
  {
    uriFreeUriMembersA( &parsed_uri );
    throw Exception( "invalid URI" );
  }
}
void URI::init( UriUriA& parsed_uri )
{
  scheme.assign( parsed_uri.scheme.first, parsed_uri.scheme.afterLast - parsed_uri.scheme.first );
  host.assign( parsed_uri.hostText.first, parsed_uri.hostText.afterLast - parsed_uri.hostText.first );
  if ( parsed_uri.portText.first != NULL )
    port = static_cast<uint16_t>( strtol( parsed_uri.portText.first, NULL, 0 ) );
  else
    port = 0;
  if ( parsed_uri.userInfo.first != NULL )
  {
    const char* userInfo_p = parsed_uri.userInfo.first;
    while ( userInfo_p < parsed_uri.userInfo.afterLast )
    {
      if ( *userInfo_p == ':' )
      {
        user.assign( parsed_uri.userInfo.first, userInfo_p - parsed_uri.userInfo.first );
        password.assign( userInfo_p + 1, parsed_uri.userInfo.afterLast - userInfo_p - 1 );
        break;
      }
      userInfo_p++;
    }
    if ( user.empty() ) // No colon found => no password, just the user
      user.assign( parsed_uri.userInfo.first, parsed_uri.userInfo.afterLast - parsed_uri.userInfo.first );
  }
  if ( parsed_uri.pathHead != NULL )
  {
    UriPathSegmentA* path_segment = parsed_uri.pathHead;
    do
    {
      resource.append( "/" );
      resource.append( path_segment->text.first, path_segment->text.afterLast - path_segment->text.first );
      path_segment = path_segment->next;
    }
    while ( path_segment != NULL );
    if ( parsed_uri.query.first != NULL )
    {
      resource.append( "?" );
      resource.append( parsed_uri.query.first, parsed_uri.query.afterLast - parsed_uri.query.first );
    }
  }
  else
    resource = "/";
}

