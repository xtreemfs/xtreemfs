// Revision: 1551

#include "yield/ipc.h"
using namespace YIELD;
using std::memset;


// client.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#define ETIMEDOUT WSAETIMEDOUT
#endif
template <class ProtocolRequestType, class ProtocolResponseType>
Client<ProtocolRequestType, ProtocolResponseType>::Client( const URI& absolute_uri, auto_Object<Log> log, uint8_t operation_retries_max, const Time& operation_timeout, auto_Object<SocketAddress> peername, auto_Object<SSLContext> ssl_context )
  : Peer<ProtocolResponseType, ProtocolRequestType>( log ), operation_retries_max( operation_retries_max ), operation_timeout( operation_timeout ), peername( peername ), ssl_context( ssl_context )
{
  this->absolute_uri = new URI( absolute_uri );
}
template <class ProtocolRequestType, class ProtocolResponseType>
Client<ProtocolRequestType, ProtocolResponseType>::~Client()
{
  for ( typename std::vector<Connection*>::iterator idle_connection_i = idle_connections.begin(); idle_connection_i != idle_connections.end(); idle_connection_i++ )
    Object::decRef( **idle_connection_i );
}
template <class ProtocolRequestType, class ProtocolResponseType>
void Client<ProtocolRequestType, ProtocolResponseType>::handleDeserializedProtocolMessage( auto_Object<ProtocolResponseType> protocol_response )
{
  this->get_helper_peer_stage()->send( *protocol_response->get_connection().release() );
  protocol_response->get_protocol_request()->set_connection( NULL );
  protocol_response->get_protocol_request()->respond( *protocol_response.release() );
}
template <class ProtocolRequestType, class ProtocolResponseType>
void Client<ProtocolRequestType, ProtocolResponseType>::handleEvent( Event& ev )
{
  switch ( ev.get_tag() )
  {
    case YIELD_OBJECT_TAG( TCPSocket::AIOConnectControlBlock ):
    {
      TCPSocket::AIOConnectControlBlock& aio_connect_control_block = static_cast<TCPSocket::AIOConnectControlBlock&>( ev );
      ProtocolRequestType* protocol_request = static_cast<ProtocolRequestType*>( aio_connect_control_block.get_context().release() );
      this->detach( protocol_request->get_connection() );
      get_protocol_request_writer_stage()->send( *protocol_request );
      Object::decRef( ev );
    }
    break;
    case YIELD_OBJECT_TAG( Connection ):
    {
      idle_connections.push_back( static_cast<Connection*>( &ev ) );
    }
    return;
    case YIELD_OBJECT_TAG( FDEventQueue::POLLOUTEvent ):
    {
      FDEventQueue::POLLOUTEvent& pollout_event = static_cast<FDEventQueue::POLLOUTEvent&>( ev );
      auto_Object<> context( pollout_event.get_context() );
      if ( context->get_tag() == YIELD_OBJECT_TAG( ProtocolRequestType ) ) // Non-blocking connection completed
      {
        auto_Object<ProtocolRequestType> protocol_request( static_cast<ProtocolRequestType*>( context.release() ) );
        auto_Object<Connection> connection = protocol_request->get_connection();
        this->detach( connection );
        if ( connection->get_socket()->connect( peername ) )
        {
          connection->get_socket()->set_blocking_mode( true );
          get_protocol_request_writer_stage()->send( *protocol_request.release() );
        }
        else
        {
          protocol_request->set_connection( NULL );
          protocol_request->respond( *( new ExceptionResponse ) );
        }
        Object::decRef( ev );
      }
      else
        Peer<ProtocolResponseType, ProtocolRequestType>::handleEvent( ev );
    }
    break;
    case YIELD_OBJECT_TAG( FDEventQueue::POLLERREvent ):
    {
      FDEventQueue::POLLERREvent& pollerr_event = static_cast<FDEventQueue::POLLERREvent&>( ev );
      auto_Object<> context( pollerr_event.get_context() );
      if ( context->get_tag() == YIELD_OBJECT_TAG( ProtocolRequestType ) ) // Non-blocking connection completed
      {
        static_cast<ProtocolRequestType*>( context.get() )->respond( *( new ExceptionResponse( pollerr_event.get_errno() ) ) );
        Object::decRef( ev );
      }
      else
        Peer<ProtocolResponseType, ProtocolRequestType>::handleEvent( ev );
    }
    break;
    case YIELD_OBJECT_TAG( FDEventQueue::TimerEvent ):
    {
      DebugBreak();
      //FDEventQueue::TimerEvent& timer_event = static_cast<FDEventQueue::TimerEvent&>( ev );
      //protocol_request = static_cast<ProtocolRequestType*>( timer_event.get_context().release() );
      //Object::decRef( ev );
    }
    break;
    case YIELD_OBJECT_TAG( ProtocolRequestType ):
    {
      if ( static_cast<ProtocolRequestType&>( ev ).get_connection() == NULL )
      {
        auto_Object<ProtocolRequestType> protocol_request( static_cast<ProtocolRequestType&>( ev ) );
        if ( !idle_connections.empty() )
        {
          protocol_request->set_connection( idle_connections.back()->incRef() );
          idle_connections.pop_back();
          get_protocol_request_writer_stage()->send( *protocol_request.release() );
        }
        else
        {
          auto_Object<Socket> _socket;
#ifdef YIELD_HAVE_OPENSSL
          if ( absolute_uri->get_scheme()[absolute_uri->get_scheme().size()-1] == 's' &&
               ssl_context != NULL )
            _socket = SSLSocket::create( ssl_context ).release();
          else
#endif
          if ( absolute_uri->get_scheme()[absolute_uri->get_scheme().size()-1] == 'u' )
            _socket = UDPSocket::create().release();
          else
            _socket = TCPSocket::create().release();
          auto_Object<Log> log = this->get_log();
          if ( log != NULL && log->get_level() >= Log::LOG_INFO && static_cast<int>( *_socket ) != -1 )
            _socket = new TracingSocket( _socket, log );
          auto_Object<Connection> connection = new Connection( _socket );
          protocol_request->set_connection( connection );
          _socket->set_blocking_mode( false );
          this->attach( connection, protocol_request->incRef(), false, true );
          bool connect_ret;
          if ( this->haveAIO() )
            connect_ret = _socket->aio_connect( new TCPSocket::AIOConnectControlBlock( protocol_request->incRef(), peername, NULL ) );
          else
            connect_ret = _socket->connect( peername );
          if ( connect_ret )
          {
            this->detach( connection );
            _socket->set_blocking_mode( true);
            get_protocol_request_writer_stage()->send( *protocol_request.release() );
          }
          else if ( connection->get_socket()->want_write() )
            return;
          else
          {
            protocol_request->set_connection( NULL );
            protocol_request->respond( *( new ExceptionResponse() ) );
          }
        }
      }
      else
      {
        Peer<ProtocolResponseType, ProtocolRequestType>::handleEvent( ev );
        return;
      }
    }
    break;
    default:
    {
      Peer<ProtocolResponseType, ProtocolRequestType>::handleEvent( ev );
    }
    return;
  }
}
template <class ProtocolRequestType, class ProtocolResponseType>
ssize_t Client<ProtocolRequestType, ProtocolResponseType>::deserialize( auto_Object<ProtocolResponseType> protocol_response, auto_Object<Buffer> buffer )
{
  ssize_t deserialize_ret = Peer<ProtocolResponseType, ProtocolRequestType>::deserialize( protocol_response, buffer );
  if ( deserialize_ret != 0 )
    protocol_response->get_protocol_request()->respond( *( new ExceptionResponse ) );
  return deserialize_ret;
}
template <class ProtocolRequestType, class ProtocolResponseType>
bool Client<ProtocolRequestType, ProtocolResponseType>::read( auto_Object<ProtocolResponseType> protocol_response, size_t buffer_capacity )
{
  if ( Peer<ProtocolResponseType, ProtocolRequestType>::read( protocol_response, buffer_capacity ) )
    return true;
  else
  {
    protocol_response->get_protocol_request()->respond( *( new ExceptionResponse ) );
    return false;
  }
}
template <class ProtocolRequestType, class ProtocolResponseType>
bool Client<ProtocolRequestType, ProtocolResponseType>::write( auto_Object<ProtocolRequestType> protocol_request, auto_Object<Buffer> buffer )
{
  if ( Peer<ProtocolResponseType, ProtocolRequestType>::write( protocol_request, buffer ) )
  {
    get_protocol_response_reader_stage()->send( *protocol_request->createProtocolResponse().release() );
    return true;
  }
  else
  {
    protocol_request->respond( *( new ExceptionResponse ) );
    return false;
  }
}
template class Client<HTTPRequest, HTTPResponse>;
template class Client<ONCRPCRequest, ONCRPCResponse>;


// eventfd_pipe.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if defined(_WIN32)
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#pragma warning( push )
#elif defined(YIELD_HAVE_LINUX_EVENTFD)
#include <sys/eventfd.h>
#endif
auto_Object<EventFDPipe> EventFDPipe::create()
{
#ifdef YIELD_HAVE_LINUX_EVENTFD
  int fd = eventfd( 0, 0 );
  if ( fd != -1 )
    return new EventFDPipe( fd );
#else
  auto_Object<TCPSocket> listen_socket = TCPSocket::create();
  if ( listen_socket != NULL &&
       listen_socket->bind( SocketAddress::create( "localhost", 0 ) ) &&
       listen_socket->listen() )
  {
    auto_Object<TCPSocket> write_end = TCPSocket::create();
    if ( write_end != NULL )
    {
      if ( write_end->connect( listen_socket->getsockname() ) )
      {
        write_end->set_blocking_mode( false );
        auto_Object<TCPSocket> read_end = listen_socket->accept();
        if ( read_end != NULL )
        {
          read_end->set_blocking_mode( false );
          return new EventFDPipe( read_end, write_end );
        }
      }
    }
  }
#endif
  return NULL;
}
#ifdef YIELD_HAVE_LINUX_EVENTFD
EventFDPipe::EventFDPipe( int fd )
  : fd( fd )
{ }
#else
EventFDPipe::EventFDPipe( auto_Object<TCPSocket> read_end, auto_Object<TCPSocket> write_end )
  : read_end( read_end ), write_end( write_end )
{ }
#endif
EventFDPipe::~EventFDPipe()
{
#ifdef YIELD_HAVE_LINUX_EVENTFD
  ::close( fd );
#endif
}
void EventFDPipe::clear()
{
#ifdef YIELD_HAVE_LINUX_EVENTFD
  uint64_t m;
  ::read( fd, reinterpret_cast<char*>( &m ), sizeof( m ) );
#else
  char m;
  read_end->read( &m, sizeof( m ) );
#endif
}
#ifndef YIELD_HAVE_LINUX_EVENTFD
int EventFDPipe::get_read_end() const
{
  return *read_end;
}
int EventFDPipe::get_write_end() const
{
  return *write_end;
}
#endif
void EventFDPipe::signal()
{
#ifdef YIELD_HAVE_LINUX_EVENTFD
  uint64_t m = 1;
  ::write( fd, reinterpret_cast<char*>( &m ), sizeof( m ) );
#else
  char m = 1;
  write_end->write( &m, sizeof( m ) );
#endif
}


// fd_event_queue.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#ifndef FD_SETSIZE
#define FD_SETSIZE 1024
#endif
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
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
#endif
#include <cstring>
#include <iostream>
#define MAX_EVENTS_PER_POLL 8192
FDEventQueue::FDEventQueue()
{
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
  eventfd_pipe = EventFDPipe::create();
  if ( eventfd_pipe == NULL ||
       !attach( eventfd_pipe->get_read_end(), eventfd_pipe->incRef() ) )
    throw Exception();
}
FDEventQueue::~FDEventQueue()
{
#if defined(_WIN32)
  delete read_fds; delete read_fds_copy;
  delete write_fds; delete write_fds_copy;
  delete except_fds; delete except_fds_copy;
#elif defined(YIELD_HAVE_LINUX_EPOLL) || defined(YIELD_HAVE_FREEBSD_KQUEUE)
  close( poll_fd );
  delete [] returned_events;
#endif
}
bool FDEventQueue::attach( int fd, auto_Object<> context, bool enable_read, bool enable_write )
{
  if ( fd_to_context_map.find( fd ) == NULL )
  {
    Object* released_context = context.release();
#if defined(_WIN32)
    if ( enable_read ) FD_SET( ( SOCKET )fd, read_fds );
    if ( enable_write ) { FD_SET( ( SOCKET )fd, write_fds ); FD_SET( ( SOCKET )fd, except_fds ); }
    next_fd_to_check = fd_to_context_map.begin();
    fd_to_context_map.insert( fd, released_context );
    return true;
#elif defined(YIELD_HAVE_LINUX_EPOLL)
    struct epoll_event change_event;
    memset( &change_event, 0, sizeof( change_event ) );
    change_event.data.ptr = released_context;
    change_event.events = 0;
    if ( enable_read ) change_event.events |= EPOLLIN;
    if ( enable_write ) change_event.events |= EPOLLOUT;
    if ( epoll_ctl( poll_fd, EPOLL_CTL_ADD, fd, &change_event ) != -1 )
    {
      fd_to_context_map.insert( fd, released_context );
      return true;
    }
    else
      return false;
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
    struct kevent change_events[2];
    EV_SET( &change_events[0], fd, EVFILT_READ, enable_read ? EV_ENABLE : EV_DISABLE, 0, 0, released_context );
    EV_SET( &change_events[1], fd, EVFILT_WRITE, enable_write ? EV_ENABLE : EV_DISABLE, 0, 0, released_context );
    if ( kevent( poll_fd, change_events, 2, 0, 0, NULL ) != -1 )
    {
      fd_to_context_map.insert( fd, released_context );
      return true;
    }
    else
      return false;
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
    if ( enable_read || enable_write )
    {
      int events = 0;
      if ( enable_read ) events |= POLLIN;
      if ( enable_write ) events |= POLLOUT;
      if ( port_associate( poll_fd, PORT_SOURCE_FD, fd, events, released_context ) != -1 )
      {
        fd_to_context_map.insert( fd, released_context );
        return true;
      }
      else
        return false;
    }
    else
      return true;
#else
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
    fd_to_context_map.insert( fd, released_context );
    return true;
#endif
  }
  else
    return toggle( fd, enable_read, enable_write );
}
Event* FDEventQueue::dequeue( uint64_t timeout_ns )
{
  Event* ev = TimerEventQueue::try_dequeue();
  if ( ev != NULL )
    return ev;
  if ( active_fds <= 0 )
  {
    uint64_t ns_until_next_timer = TimerEventQueue::getNSUntilNextTimer();
    if ( ns_until_next_timer < timeout_ns )
      timeout_ns = ns_until_next_timer;
#if defined(_WIN32)
    memcpy_s( read_fds_copy, sizeof( *read_fds_copy ), read_fds, sizeof( *read_fds ) );
    memcpy_s( write_fds_copy, sizeof( *write_fds_copy ), write_fds, sizeof( *write_fds ) );
    memcpy_s( except_fds_copy, sizeof( *except_fds_copy ), except_fds, sizeof( *except_fds ) );
    next_fd_to_check = fd_to_context_map.begin();
    struct timeval poll_tv = Time( timeout_ns );
    active_fds = select( 0, read_fds_copy, write_fds_copy, except_fds_copy, &poll_tv );
#elif defined(YIELD_HAVE_LINUX_EPOLL)
    active_fds = epoll_wait( poll_fd, returned_events, MAX_EVENTS_PER_POLL, timeout_ns == static_cast<uint64_t>( -1 ) ? -1 : static_cast<int>( timeout_ns / NS_IN_MS ) );
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
    if ( timeout_ns == static_cast<uint64_t>( -1 )
      active_fds = kevent( poll_fd, 0, 0, returned_events, MAX_EVENTS_PER_POLL, NULL );
    else
    {
      struct timespec poll_tv = Time( timeout_ns );
      active_fds = kevent( poll_fd, 0, 0, returned_events, MAX_EVENTS_PER_POLL, &poll_tv );
    }
    else
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
    uint_t nget = 1;
    if ( timeout_ns == static_cast<uint64_t>( -1 ) )
      active_fds = port_get( poll_fd, returned_events, NULL );
    else
    {
      struct timespec poll_tv = Time( timeout_ns );
    //			active_fds = port_getn( poll_fd, returned_events, MAX_EVENTS_PER_POLL, &nget, &poll_tv );
      active_fds = port_get( poll_fd, returned_events, &poll_tv );
    }
    if ( active_fds == 0 )
      active_fds = ( int )nget;
#else
    next_pollfd_to_check = 0;
    active_fds = poll( &pollfds[0], pollfds.size(), timeout_ns == static_cast<uint64_t>( -1 ) ? -1 : static_cast<int>( timeout_ns / NS_IN_MS ) );
#endif
    if ( active_fds <= 0 )
      return NULL;
  }
#if defined(_WIN32)
  while ( active_fds > 0 && next_fd_to_check != fd_to_context_map.end() )
  {
    unsigned int fd = next_fd_to_check->first;
    if ( FD_ISSET( fd, read_fds_copy ) )
    {
      FD_CLR( fd, read_fds_copy );
      active_fds--;
      if ( fd == static_cast<unsigned int>( eventfd_pipe->get_read_end() ) )
      {
        eventfd_pipe->clear();
        next_fd_to_check++;
        return NULL;
      }
      else
        return new POLLINEvent( Object::incRef( fd_to_context_map.find( fd ) ) );
    }
    else if ( FD_ISSET( fd, write_fds_copy ) )
    {
      FD_CLR( fd, write_fds_copy );
      active_fds--;
      return new POLLOUTEvent( Object::incRef( fd_to_context_map.find( fd ) ) );
    }
    else if ( FD_ISSET( fd, except_fds_copy ) )
    {
      FD_CLR( fd, except_fds_copy );
      active_fds--;
      int so_error; int so_error_len = sizeof( so_error );
      ::getsockopt( fd, SOL_SOCKET, SO_ERROR, reinterpret_cast<char*>( &so_error ), &so_error_len );
      return new POLLERREvent( Object::incRef( fd_to_context_map.find( fd ) ), so_error );
    }
    else
    {
      next_fd_to_check++;
      continue;
    }
  }
#elif defined(YIELD_HAVE_LINUX_EPOLL) || defined(YIELD_HAVE_FREEBSD_KQUEUE) || defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
  while ( active_fds > 0 )
  {
    active_fds--;
#if defined(YIELD_HAVE_LINUX_EPOLL)
    if ( returned_events[active_fds].data.ptr != eventfd_pipe.get() )
    {
      Object* context = Object::incRef( static_cast<Object*>( returned_events[active_fds].data.ptr ) );
      if ( ( returned_events[active_fds].events & EPOLLIN ) == EPOLLIN )
        return new POLLINEvent( context );
      else
        return new POLLOUTEvent( context );
    }
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
    if ( returned_events[active_fds].ident != eventfd_pipe->get_read_end() )
    {
      Object* context = Object::incRef( static_cast<Object*>( returned_events[active_fds].udata ) );
      if ( returned_events[active_fds].filter == EVFILT_READ )
        return new POLLINEvent( context );
      else
        return new POLLOUTEvent( context );
    }
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
    if ( returned_events[active_fds].portev_source == PORT_SOURCE_FD )
    {
      int fd = returned_events[active_fds].portev_object;
      if ( fd != eventfd_pipe->get_read_end() )
      {
        Object* context = Object::incRef( static_cast<Object*>( returned_events[active_fds].portev_user ) );
        int portev_events = returned_events[active_fds].portev_events;
        memset( &returned_events[active_fds], 0, sizeof( returned_events[active_fds] ) );
        if ( ( portev_events & POLLIN ) == POLLIN ||
             ( portev_events & POLLRDNORM ) == POLLRDNORM )
          return new POLLINEvent( context );
        else
          return new POLLOUTEvent( context );
      }
    }
    else
      continue;
#endif
    // The signal was set
    eventfd_pipe->clear();
  }
#else
  pollfd_vector::size_type pollfds_size = pollfds.size();
  while ( active_fds > 0 && next_pollfd_to_check < pollfds_size )
  {
    if ( pollfds[next_pollfd_to_check].revents != 0 )
    {
      int fd = pollfds[next_pollfd_to_check].fd;
      int revents = [next_pollfd_to_check].revents;
      pollfds[next_pollfd_to_check].revents = 0;
      next_pollfd_to_check++;
      active_fds--;
      if ( fd == eventfd_pipe->get_read_end() )
      {
        eventfd_pipe->clear();
        return NULL;
      }
      else
      {
        Object* context = Object::incRef( fd_to_context_map.find( fd ) );
        if ( ( revents & POLLIN ) == POLLIN )
          return new POLLINEvent( context );
        else if ( ( revents & POLLOUT ) == POLLOUT )
          return new POLLOUTEvent( context );
        else if ( ( revents & POLLERR ) == POLLERR || ( revents & POLLHUP ) == POLLHUP )
          return new POLLERREvent( context );
        else
          DebugBreak();
    }
    else
      next_pollfd_to_check++;
  }
  active_fds = 0;
#endif
  return NULL;
}
void FDEventQueue::detach( int fd )
{
  active_fds = 0; // Have to discard all returned events because the fd may be in them and we have to assume its context is now invalid
  Object::decRef( fd_to_context_map.remove( fd ) );
#if defined(_WIN32)
  FD_CLR( ( SOCKET )fd, read_fds ); FD_CLR( ( SOCKET )fd, read_fds_copy );
  FD_CLR( ( SOCKET )fd, write_fds ); FD_CLR( ( SOCKET )fd, write_fds_copy );
  FD_CLR( ( SOCKET )fd, except_fds ); FD_CLR( ( SOCKET )fd, except_fds_copy );
  next_fd_to_check = fd_to_context_map.begin();
#elif defined(YIELD_HAVE_LINUX_EPOLL)
  struct epoll_event change_event; // From the man page: In kernel versions before 2.6.9, the EPOLL_CTL_DEL operation required a non-NULL pointer in event, even though this argument is ignored. Since kernel 2.6.9, event can be specified as NULL when using EPOLL_CTL_DEL.
  epoll_ctl( poll_fd, EPOLL_CTL_DEL, fd, &change_event );
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
  struct kevent change_events[2];
  EV_SET( &change_events[0], fd, EVFILT_READ, EV_DELETE, 0, 0, NULL );
  EV_SET( &change_events[1], fd, EVFILT_WRITE, EV_DELETE, 0, 0, NULL );
  kevent( poll_fd, change_events, 2, 0, 0, NULL );
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
  port_dissociate( poll_fd, PORT_SOURCE_FD, fd );
#else
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
#endif
}
bool FDEventQueue::enqueue( Event& ev )
{
  bool result = TimerEventQueue::enqueue( ev );
  eventfd_pipe->signal();
  return result;
}
bool FDEventQueue::toggle( int fd, bool enable_read, bool enable_write )
{
#if defined(_WIN32)
  if ( enable_read )
    FD_SET( ( SOCKET )fd, read_fds );
  else
    FD_CLR( ( SOCKET )fd, read_fds );
  if ( enable_write )
  {
    FD_SET( ( SOCKET )fd, write_fds );
    FD_SET( ( SOCKET )fd, except_fds );
  }
  else
  {
    FD_CLR( ( SOCKET )fd, write_fds );
    FD_CLR( ( SOCKET )fd, except_fds );
  }
  return true;
#elif defined(YIELD_HAVE_LINUX_EPOLL)
  struct epoll_event change_event;
  memset( &change_event, 0, sizeof( change_event ) );
  change_event.data.ptr = fd_to_context_map.find( fd );
  change_event.events = 0;
  if ( enable_read ) change_event.events |= EPOLLIN;
  if ( enable_write ) change_event.events |= EPOLLOUT;
  return epoll_ctl( poll_fd, EPOLL_CTL_MOD, fd, &change_event ) != -1;
#elif defined YIELD_HAVE_FREEBSD_KQUEUE
  struct kevent change_events[2];
  Object* context = fd_to_context_map.find( fd );
  EV_SET( &change_events[0], fd, EVFILT_READ, enable_read ? EV_ENABLE : EV_DISABLE, 0, 0, context );
  EV_SET( &change_events[1], fd, EVFILT_WRITE, enable_write ? EV_ENABLE : EV_DISABLE, 0, 0, context );
  return kevent( poll_fd, change_events, 2, 0, 0, NULL ) != -1;
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
  if ( enable_read || enable_write )
  {
    int events = 0;
    if ( enable_read ) events |= POLLIN;
    if ( enable_write ) events |= POLLOUT;
    Object* context = fd_to_context_map.find( fd );
    return port_associate( poll_fd, PORT_SOURCE_FD, fd, events, context ) != -1;
  }
  else
    return port_dissociate( poll_fd, PORT_SOURCE_FD, fd ) != -1;
#else
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
#endif
}
#ifdef _WIN32
#pragma warning( pop )
#endif


// http_client.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
auto_Object<HTTPClient> HTTPClient::create( const URI& absolute_uri,
                                            auto_Object<StageGroup> stage_group,
                                            auto_Object<Log> log,
                                            uint8_t operation_retries_max,
                                            const Time& operation_timeout,
                                            auto_Object<SSLContext> ssl_context )
{
  YIELD::URI checked_absolute_uri( absolute_uri );
  if ( checked_absolute_uri.get_port() == 0 )
    checked_absolute_uri.set_port( 80 );
  auto_Object<SocketAddress> peername = SocketAddress::create( absolute_uri );
  if ( peername != NULL )
  {
#ifdef YIELD_HAVE_OPENSSL
    if ( absolute_uri.get_scheme() == "https" && ssl_context == NULL )
      ssl_context = new SSLContext( SSLv23_client_method() );
#endif
    auto_Object<EventQueue> http_client_event_queue, http_request_writer_event_queue, http_response_reader_event_queue;
#ifdef _WIN32
    http_client_event_queue = new IOCompletionPort;
    http_request_writer_event_queue = new IOCompletionPort;
    http_response_reader_event_queue = new IOCompletionPort;
#else
    http_client_event_queue = new FDEventQueue;
    http_request_writer_event_queue = new FDEventQueue;
    http_response_reader_event_queue = new FDEventQueue;
#endif
    auto_Object<HTTPClient> http_client = new HTTPClient( absolute_uri, log, operation_retries_max, operation_timeout, peername, ssl_context );
    auto_Object<Stage> http_client_stage = stage_group->createStage( http_client->incRef(), 1, http_client_event_queue, NULL, log );
    auto_Object<HTTPClient> http_request_writer = new HTTPClient( absolute_uri, log, operation_retries_max, operation_timeout, peername, ssl_context );
    auto_Object<Stage> http_request_writer_stage = stage_group->createStage( http_request_writer->incRef(), 1, http_request_writer_event_queue, NULL, log );
    auto_Object<HTTPClient> http_response_reader = new HTTPClient( absolute_uri, log, operation_retries_max, operation_timeout, peername, ssl_context );
    auto_Object<Stage> http_response_reader_stage = stage_group->createStage( http_response_reader->incRef(), 1, http_response_reader_event_queue, NULL, log );
    http_client->set_http_request_writer_stage( http_request_writer_stage );
    http_request_writer->set_http_response_reader_stage( http_response_reader_stage );
    http_response_reader->set_helper_peer_stage( http_client_stage );
    return http_client;
  }
  return NULL;
}
auto_Object<HTTPResponse> HTTPClient::GET( const URI& absolute_uri, auto_Object<Log> log )
{
  return sendHTTPRequest( "GET", absolute_uri, NULL, log );
}
auto_Object<HTTPResponse> HTTPClient::PUT( const URI& absolute_uri, auto_Object<Buffer> body, auto_Object<Log> log )
{
  return sendHTTPRequest( "PUT", absolute_uri, body, log );
}
auto_Object<HTTPResponse> HTTPClient::PUT( const URI& absolute_uri, const Path& body_file_path, auto_Object<Log> log )
{
  auto_Object<File> file = File::open( body_file_path );
  size_t file_size = static_cast<size_t>( file->getattr()->get_size() );
  auto_Object<HeapBuffer> body = new HeapBuffer( file_size );
  file->read( *body, file_size );
  return sendHTTPRequest( "PUT", absolute_uri, body.release(), log );
}
auto_Object<HTTPResponse> HTTPClient::sendHTTPRequest( const char* method, const YIELD::URI& absolute_uri, auto_Object<Buffer> body, auto_Object<Log> log )
{
  auto_Object<StageGroup> stage_group = new SEDAStageGroup( "HTTPClient", 0, NULL, log );
  auto_Object<HTTPClient> http_client = HTTPClient::create( absolute_uri, stage_group, log );
  auto_Object<HTTPRequest> http_request = new HTTPRequest( method, absolute_uri, body );
  http_request->set_header( "User-Agent", "Flog 0.99" );
  auto_Object< OneSignalEventQueue< NonBlockingFiniteQueue<Event*, 16 > > > http_response_queue( new OneSignalEventQueue< NonBlockingFiniteQueue<Event*, 16 > > );
  http_request->set_response_target( http_response_queue->incRef() );
  http_client->send( http_request->incRef() );
  return http_response_queue->dequeue_typed<HTTPResponse>();
}


// http_message.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
HTTPMessage::HTTPMessage()
{
  http_version = 1;
}
HTTPMessage::HTTPMessage( auto_Object<Buffer> body )
  : body( body )
{
  http_version = 1;
}
ssize_t HTTPMessage::deserialize( auto_Object<Buffer> buffer )
{
  switch ( deserialize_state )
  {
    case DESERIALIZING_HEADERS:
    {
      ssize_t RFC822Headers_deserialize_ret = RFC822Headers::deserialize( buffer );
      if ( RFC822Headers_deserialize_ret == 0 )
      {
        if ( strcmp( get_header( "Transfer-Encoding" ), "chunked" ) == 0 )
          return 0;
        else
        {
          const char* content_length_header_value = get_header( "Content-Length", NULL ); // Most browsers
          if ( content_length_header_value == NULL )
            content_length_header_value = get_header( "Content-length" ); // httperf
          size_t content_length = atoi( content_length_header_value );
          if ( content_length == 0 )
          {
            deserialize_state = DESERIALIZE_DONE;
            return 0;
          }
          else
          {
            deserialize_state = DESERIALIZING_BODY;
            if ( strcmp( get_header( "Expect" ), "100-continue" ) == 0 )
              return 0;
            // else fall through
          }
        }
      }
      else
        return RFC822Headers_deserialize_ret;
    }
    case DESERIALIZING_BODY:
    {
      if ( body == NULL )
        body = buffer;
      else
        DebugBreak(); // Chain buffers
      deserialize_state = DESERIALIZE_DONE;
    }
    case DESERIALIZE_DONE: return 0;
    default: DebugBreak(); return -1;
  }
}
auto_Object<Buffer> HTTPMessage::serialize()
{
  // Finalize headers
  if ( body != NULL && get_header( "Content-Length", NULL ) == NULL )
  {
    char content_length_str[32];
#ifdef _WIN32
    sprintf_s( content_length_str, 32, "%u", body->size() );
#else
    snprintf( content_length_str, 32, "%zu", body->size() );
#endif
    set_header( "Content-Length", content_length_str );
  }
  set_iovec( "\r\n", 2 );
  auto_Object<Buffer> buffer = RFC822Headers::serialize();
  buffer->set_next_buffer( body );
  return buffer;
}


// http_request.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
HTTPRequest::HTTPRequest( auto_Object<Connection> connection )
  : ProtocolRequest( connection )
{
  method[0] = 0;
  uri = new char[2];
  uri[0] = 0;
  uri_len = 2;
  http_version = 1;
  deserialize_state = DESERIALIZING_METHOD;
}
HTTPRequest::HTTPRequest( const char* method, const char* relative_uri, const char* host, auto_Object<Buffer> body )
  : HTTPMessage( body )
{
  init( method, relative_uri, host, body );
}
HTTPRequest::HTTPRequest( const char* method, const URI& absolute_uri, auto_Object<Buffer> body )
  : HTTPMessage( body )
{
  init( method, absolute_uri.get_resource().c_str(), absolute_uri.get_host().c_str(), body );
}
void HTTPRequest::init( const char* method, const char* relative_uri, const char* host, auto_Object<Buffer> body )
{
#ifdef _WIN32
  strncpy_s( this->method, 16, method, 16 );
#else
  strncpy( this->method, method, 16 );
#endif
  uri_len = strnlen( relative_uri, UINT16_MAX );
  this->uri = new char[uri_len + 1];
  memcpy_s( this->uri, uri_len + 1, relative_uri, uri_len + 1 );
  http_version = 1;
  set_header( "Host", const_cast<char*>( host ) );
  deserialize_state = DESERIALIZE_DONE;
}
HTTPRequest::~HTTPRequest()
{
  delete [] uri;
}
ssize_t HTTPRequest::deserialize( auto_Object<Buffer> buffer )
{
  switch ( deserialize_state )
  {
    case DESERIALIZING_METHOD:
    {
      char* method_p = method + strnlen( method, 16 );
      for ( ;; )
      {
        if ( buffer->get( method_p, 1 ) == 1 )
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
          return 1;
        }
      }
      // Fall through
    }
    case DESERIALIZING_URI:
    {
      char* uri_p = uri + strnlen( uri, UINT16_MAX );
      for ( ;; )
      {
        if ( buffer->get( uri_p, 1 ) == 1 )
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
              memcpy_s( new_uri, new_uri_len, uri, uri_len );
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
          return 1;
        }
      }
      // Fall through
    }
    case DESERIALIZING_HTTP_VERSION:
    {
      for ( ;; )
      {
        uint8_t test_http_version;
        if ( buffer->get( &test_http_version, 1 ) == 1 )
        {
          if ( test_http_version != '\r' )
          {
            http_version = test_http_version;
            continue;
          }
          else
          {
            http_version = http_version == '1' ? 1 : 0;
            deserialize_state = DESERIALIZING_HEADERS;
            break;
          }
        }
        else
          return 1;
      }
    }
    // Fall through
    default: return HTTPMessage::deserialize( buffer );
  }
}
bool HTTPRequest::respond( uint16_t status_code )
{
  return respond( *( new HTTPResponse( incRef(), status_code ) ) );
}
bool HTTPRequest::respond( uint16_t status_code, auto_Object<Buffer> body )
{
  return respond( *( new HTTPResponse( incRef(), status_code, body ) ) );
}
auto_Object<Buffer> HTTPRequest::serialize()
{
  size_t method_len = strnlen( method, 16 );
  auto_Object<Buffer> buffer = new HeapBuffer( method_len + 1 + uri_len + 11 );
  buffer->put( method, method_len );
  buffer->put( " ", 1 );
  buffer->put( uri, uri_len );
  buffer->put( " HTTP/1.1\r\n", 11 );
  buffer->set_next_buffer( HTTPMessage::serialize() );
  return buffer;
}


// http_response.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
HTTPResponse::HTTPResponse( auto_Object<HTTPRequest> http_request )
  : ProtocolResponse<HTTPRequest>( http_request )
{
  memset( status_code_str, 0, sizeof( status_code_str ) );
  deserialize_state = DESERIALIZING_HTTP_VERSION;
}
HTTPResponse::HTTPResponse( auto_Object<HTTPRequest> http_request, uint16_t status_code )
  : ProtocolResponse<HTTPRequest>( http_request ), status_code( status_code )
{
  http_version = 1;
  deserialize_state = DESERIALIZE_DONE;
}
HTTPResponse::HTTPResponse( auto_Object<HTTPRequest> http_request, uint16_t status_code, auto_Object<Buffer> body )
  : ProtocolResponse<HTTPRequest>( http_request ), HTTPMessage( body ), status_code( status_code )
{
  deserialize_state = DESERIALIZE_DONE;
}
ssize_t HTTPResponse::deserialize( auto_Object<Buffer> buffer )
{
  switch ( deserialize_state )
  {
    case DESERIALIZING_HTTP_VERSION:
    {
      for ( ;; )
      {
        uint8_t test_http_version;
        if ( buffer->get( &test_http_version, 1 ) == 1 )
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
          return 1;
      }
    }
    // Fall through
    case DESERIALIZING_STATUS_CODE:
    {
      char* status_code_str_p = status_code_str + strnlen( status_code_str, 3 );
      for ( ;; )
      {
        if ( buffer->get( status_code_str_p, 1 ) == 1 )
        {
          if ( *status_code_str_p != ' ' )
          {
            status_code_str_p++;
            if ( static_cast<uint8_t>( status_code_str_p - status_code_str ) == 4 )
            {
              deserialize_state = DESERIALIZE_DONE;
              return -1;
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
          return 1;
      }
    }
    // Fall through
    case DESERIALIZING_REASON:
    {
      char c;
      for ( ;; )
      {
        if ( buffer->get( &c, 1 ) == 1 )
        {
          if ( c == '\r' )
          {
            deserialize_state = DESERIALIZING_HEADERS;
            break;
          }
        }
        else
          return 1;
      }
    }
    // Fall through
    default: return HTTPMessage::deserialize( buffer );
  }
}
auto_Object<Buffer> HTTPResponse::serialize()
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
  auto_Object<Buffer> buffer = new StringLiteralBuffer( status_line, status_line_len );
  char date[32];
  Time().as_http_date_time( date, 32 );
  set_header( "Date", date );
  buffer->set_next_buffer( HTTPMessage::serialize() );
  return buffer;
}


// http_server.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
template <class StageGroupType>
auto_Object<HTTPServer> HTTPServer::create( const URI& absolute_uri,
                                            auto_Object<EventTarget> http_request_target,
                                            auto_Object<StageGroupType> stage_group,
                                            auto_Object<Log> log,
                                            auto_Object<SSLContext> ssl_context )
{
  auto_Object<SocketAddress> sockname = SocketAddress::create( absolute_uri );
  if ( sockname != NULL )
  {
    auto_Object<TCPListenQueue> tcp_listen_queue;
#ifdef YIELD_HAVE_OPENSSL
    if ( absolute_uri.get_scheme() == "https" && ssl_context != NULL )
      tcp_listen_queue = SSLListenQueue::create( sockname, ssl_context ).release();
    else
#endif
      tcp_listen_queue = TCPListenQueue::create( sockname );
    if ( tcp_listen_queue != NULL )
    {
      auto_Object<EventQueue> http_request_reader_event_queue, http_response_writer_event_queue;
#ifdef _WIN32
      http_request_reader_event_queue = new IOCompletionPort;
      http_response_writer_event_queue = new IOCompletionPort;
#else
      http_request_reader_event_queue = new FDEventQueue;
      http_response_writer_event_queue = new FDEventQueue;
#endif
      auto_Object<HTTPServer> http_request_reader = new HTTPServer( http_request_target, log );
      auto_Object<Stage> http_request_reader_stage = stage_group->createStage( http_request_reader, 1, http_request_reader_event_queue, NULL, log );
      auto_Object<HTTPServer> http_response_writer = new HTTPServer( http_request_target, log );
      auto_Object<Stage> http_response_writer_stage = stage_group->createStage( http_response_writer, 1, http_response_writer_event_queue, NULL, log );
      auto_Object<HTTPServer> http_server = new HTTPServer( http_request_target, log );
      stage_group->createStage( http_server, 1, tcp_listen_queue, NULL, log );
      http_server->set_http_request_reader_stage( http_request_reader_stage );
      http_request_reader->set_http_response_writer_stage( http_response_writer_stage );
      http_response_writer->set_http_request_reader_stage( http_request_reader_stage );
      return http_server;
    }
  }
  return NULL;
}
template
auto_Object<HTTPServer> HTTPServer::create<SEDAStageGroup>( const URI& absolute_uri,
                                            auto_Object<EventTarget> http_request_target,
                                            auto_Object<SEDAStageGroup> stage_group,
                                            auto_Object<Log> log,
                                            auto_Object<SSLContext> ssl_context );
void HTTPServer::handleDeserializedProtocolMessage( auto_Object<HTTPRequest> http_request )
{
  http_request->set_response_target( get_http_response_writer_stage()->incRef() );
  http_request_target->send( *http_request.release() );
}


// io_completion_port.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#endif
IOCompletionPort::IOCompletionPort()
{
#ifdef _WIN32
  hIoCompletionPort = CreateIoCompletionPort( INVALID_HANDLE_VALUE, NULL, NULL, 0 );
#endif
}
IOCompletionPort::~IOCompletionPort()
{
#ifdef _WIN32
  CloseHandle( hIoCompletionPort );
#endif
}
bool IOCompletionPort::attach( int fd )
{
#ifdef _WIN32
  return CreateIoCompletionPort( ( HANDLE )fd, hIoCompletionPort, 1, 0 ) != INVALID_HANDLE_VALUE;
#else
  return false;
#endif
}
Event* IOCompletionPort::dequeue( uint64_t timeout_ns )
{
  Event* ev = TimerEventQueue::try_dequeue();
  if ( ev != NULL )
    return ev;
#ifdef _WIN32
  DWORD dwNumberOfBytesTransferred;
  ULONG_PTR ulpCompletionKey;
  struct AIOControlBlock<Event>::aiocb* aiocb_;
  if ( GetQueuedCompletionStatus( hIoCompletionPort, &dwNumberOfBytesTransferred, &ulpCompletionKey, ( LPOVERLAPPED* )&aiocb_, timeout_ns == static_cast<uint64_t>( -1 ) ? INFINITE : static_cast<DWORD>( timeout_ns / NS_IN_MS ) ) )
  {
    if ( ulpCompletionKey == 0 ) // Signal
      return NULL; // TODO: dequeue from AIOEnqueueContralBlock
    else if ( ulpCompletionKey == 1 ) // I/O completion
    {
      AIOControlBlock<Event>* aio_control_block = aiocb_->this_;
      if ( aio_control_block->isComplete() ) // The I/O operation returned immediately and has already been processed; throw away the AIOCB and try to dequeue again
      {
        Object::decRef( *aio_control_block );
        return NULL;
      }
      else
      {
        aio_control_block->onCompletion( dwNumberOfBytesTransferred );
        return aio_control_block;
      }
    }
    else
    {
      DebugBreak();
      return NULL;
    }
  }
  else if ( GetLastError() == WAIT_TIMEOUT )
    return NULL;
  else
  {
    AIOControlBlock<Event>* aio_control_block = aiocb_->this_;
    if ( aio_control_block->isComplete() ) DebugBreak();
    // aio_control_block->onError( GetLastError() );
    return aio_control_block;
  }
#else
  DebugBreak();
  return NULL;
#endif
}
bool IOCompletionPort::enqueue( Event& ev )
{
  bool result = TimerEventQueue::enqueue( ev );
#ifdef _WIN32
  PostQueuedCompletionStatus( hIoCompletionPort, 0, 0, NULL );
#endif
  return result;
}


// json_marshaller.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
extern "C"
{
  #include <yajl.h>
};
JSONMarshaller::JSONMarshaller( bool write_empty_strings )
: write_empty_strings( write_empty_strings )
{
  root_decl = NULL;
  writer = yajl_gen_alloc( NULL );
}
JSONMarshaller::JSONMarshaller( JSONMarshaller& parent_json_marshaller, const Declaration& root_decl )
  : BufferedMarshaller( parent_json_marshaller ), root_decl( &root_decl ),
    write_empty_strings( parent_json_marshaller.write_empty_strings ), writer( parent_json_marshaller.writer )
{ }
JSONMarshaller::~JSONMarshaller()
{
  if ( root_decl == NULL ) // This is the root JSONMarshaller
    yajl_gen_free( writer );
}
void JSONMarshaller::flushYAJLBuffer()
{
  const unsigned char* buffer;
  unsigned int len;
  yajl_gen_get_buf( writer, &buffer, &len );
  BufferedMarshaller::write( buffer, len );
  yajl_gen_clear( writer );
}
void JSONMarshaller::writeBool( const Declaration& decl, bool value )
{
  writeDeclaration( decl );
  yajl_gen_bool( writer, ( int )value );
  flushYAJLBuffer();
}
void JSONMarshaller::writeBuffer( const Declaration& decl, auto_Object<Buffer> value )
{
  writeDeclaration( decl );
  DebugBreak();
}
void JSONMarshaller::writeDeclaration( const Declaration& decl )
{
  if ( in_map && decl.get_identifier() )
    yajl_gen_string( writer, reinterpret_cast<const unsigned char*>( decl.get_identifier() ), static_cast<unsigned int>( strnlen( decl.get_identifier(), UINT16_MAX ) ) );
}
void JSONMarshaller::writeDouble( const Declaration& decl, double value )
{
  writeDeclaration( decl );
  yajl_gen_double( writer, value );
  flushYAJLBuffer();
}
void JSONMarshaller::writeInt64( const Declaration& decl, int64_t value )
{
  writeDeclaration( decl );
  yajl_gen_integer( writer, ( long )value );
  flushYAJLBuffer();
}
void JSONMarshaller::writeMap( const Declaration& decl, const Map& value )
{
  writeDeclaration( decl );
  JSONMarshaller( *this, decl ).writeMap( &value );
}
void JSONMarshaller::writeMap( const Map* value )
{
  writeStruct( static_cast<const Object*>( value ) );
}
void JSONMarshaller::writeStruct( const Declaration& decl, const Object& value )
{
  writeDeclaration( decl );
  JSONMarshaller( *this, decl ).writeStruct( &value );
}
void JSONMarshaller::writeStruct( const Object* value )
{
  yajl_gen_map_open( writer );
  in_map = true;
  if ( value )
    value->marshal( *this );
  yajl_gen_map_close( writer );
  flushYAJLBuffer();
}
void JSONMarshaller::writeSequence( const Declaration& decl, const Sequence& value )
{
  writeDeclaration( decl );
  JSONMarshaller( *this, decl ).writeSequence( &value );
}
void JSONMarshaller::writeSequence( const Sequence* value )
{
  yajl_gen_array_open( writer );
  in_map = false;
  if ( value )
    value->marshal( *this );
  yajl_gen_array_close( writer );
  flushYAJLBuffer();
}
void JSONMarshaller::writeString( const Declaration& decl, const char* value, size_t value_len )
{
  if ( value_len > 0 || write_empty_strings )
  {
    writeDeclaration( decl );
    yajl_gen_string( writer, reinterpret_cast<const unsigned char*>( value ), static_cast<unsigned int>( value_len ) );
    flushYAJLBuffer();
  }
}


// json_unmarshaller.cpp
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
    JSONValue( auto_Object<StringBuffer> identifier, bool is_map )
      : identifier( identifier ), is_map( is_map )
    {
      parent = child = prev = next = NULL;
      have_read = false;
      as_integer = 0;
    }
    virtual ~JSONValue()
    {
      delete child;
      delete next;
    }
    auto_Object<StringBuffer> identifier;
    bool is_map;
    auto_Object<StringBuffer> as_string;
    union
    {
      double as_double;
      int64_t as_integer;
    };
    JSONValue *parent, *child, *prev, *next;
    bool have_read;
  protected:
    JSONValue()
    {
      is_map = true;
      parent = child = prev = next = NULL;
      have_read = false;
      as_integer = 0;
    }
  };
  class JSONObject : public JSONValue
  {
  public:
    JSONObject( auto_Object<Buffer> source_buffer )
    {
      current_json_value = parent_json_value = NULL;
      reader = yajl_alloc( &JSONObject_yajl_callbacks, NULL, this );
      next_map_key = NULL; next_map_key_len = 0;
      while ( source_buffer != NULL )
      {
        if ( source_buffer->get_tag() == YIELD_OBJECT_TAG( FixedBuffer ) )
        {
          const unsigned char* jsonText = static_cast<const unsigned char*>( static_cast<void*>( static_cast<FixedBuffer&>( *source_buffer ) ) );
          unsigned int jsonTextLength = static_cast<unsigned int>( source_buffer->size() );
          yajl_status yajl_parse_status = yajl_parse( reader, jsonText, jsonTextLength );
          if ( yajl_parse_status == yajl_status_ok )
            return;
          else
          {
            unsigned char* yajl_error_str = yajl_get_error( reader, 1, jsonText, jsonTextLength );
            std::ostringstream what;
            what << __FILE__ << ":" << __LINE__ << ": JSON parsing error: " << reinterpret_cast<char*>( yajl_error_str ) << std::endl;
            yajl_free_error( yajl_error_str );
            throw Exception( what.str() );
          }
        }
        else
          DebugBreak();
        source_buffer = source_buffer->get_next_buffer();
      }
    }
    ~JSONObject()
    {
      yajl_free( reader );
    }
  private:
    yajl_handle reader;
    std::string type_name;
    uint32_t tag;
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
      JSONValue& json_value = self->createNextJSONValue();
      json_value.as_string = new StringBuffer( reinterpret_cast<const char*>( buffer ), len );
      return 1;
    }
    static int handle_yajl_start_map( void* _self )
    {
      JSONObject* self = static_cast<JSONObject*>( _self );
      JSONValue& json_value = self->createNextJSONValue( true );
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
      JSONValue& json_value = self->createNextJSONValue();
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
    JSONValue& createNextJSONValue( bool is_map = false )
    {
      auto_Object<StringBuffer> identifier = next_map_key_len != 0 ? new StringBuffer( next_map_key, next_map_key_len ) : NULL;
      next_map_key = NULL; next_map_key_len = 0;
      if ( current_json_value == NULL )
      {
        if ( parent_json_value ) // This is the first value of an array or map
        {
          current_json_value = new JSONValue( identifier, is_map );
          current_json_value->parent = parent_json_value;
          parent_json_value->child = current_json_value;
        }
        else // This is the first value of the whole object
        {
#ifdef _DEBUG
          if ( identifier != NULL ) DebugBreak();
#endif
          current_json_value = this;
        }
      }
      else
      {
        JSONValue* next_json_value = new JSONValue( identifier, is_map );
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
JSONUnmarshaller::JSONUnmarshaller( auto_Object<Buffer> source_buffer )
{
  root_decl = NULL;
  root_json_value = new JSONObject( source_buffer );
  next_json_value = root_json_value->child;
}
JSONUnmarshaller::JSONUnmarshaller( const Declaration& root_decl, JSONValue& root_json_value )
  : Unmarshaller( NULL ),
    root_decl( &root_decl ), root_json_value( &root_json_value ),
    next_json_value( root_json_value.child )
{ }
JSONUnmarshaller::~JSONUnmarshaller()
{
  if ( root_decl == NULL )
    delete root_json_value;
}
bool JSONUnmarshaller::readBool( const Declaration& decl )
{
  JSONValue* json_value = readJSONValue( decl );
  if ( json_value )
  {
    if ( decl.get_identifier() ) // Read the value
      return json_value->as_integer != 0;
    else // Read the identifier
      return false; // Doesn't make any sense
  }
  else
    return false;
}
double JSONUnmarshaller::readDouble( const Declaration& decl )
{
  JSONValue* json_value = readJSONValue( decl );
  if ( json_value )
  {
    if ( decl.get_identifier() ) // Read the value
      return json_value->as_double;
    else // Read the identifier
      return atof( json_value->identifier->c_str() );
  }
  else
    return 0;
}
int64_t JSONUnmarshaller::readInt64( const Declaration& decl )
{
  JSONValue* json_value = readJSONValue( decl );
  if ( json_value )
  {
    if ( decl.get_identifier() ) // Read the value
      return json_value->as_integer;
    else // Read the identifier
      return atoi( json_value->identifier->c_str() );
  }
  else
    return 0;
}
Map* JSONUnmarshaller::readMap( const Declaration& decl, Map* value )
{
  if ( value )
  {
    JSONValue* json_value;
    if ( decl.get_identifier() )
    {
      json_value = readJSONValue( decl );
      if ( json_value == NULL )
        return value;
    }
    else if ( root_json_value && !root_json_value->have_read )
    {
      if ( root_json_value->is_map )
        json_value = root_json_value;
      else
        return value;
    }
    else
      return value;
    JSONUnmarshaller child_source_istream( decl, *json_value );
    child_source_istream.readMap( *value );
    json_value->have_read = true;
  }
  return value;
}
void JSONUnmarshaller::readMap( Map& value )
{
  while ( next_json_value )
    value.unmarshal( *this );
}
Sequence* JSONUnmarshaller::readSequence( const Declaration& decl, Sequence* value )
{
  if ( value )
  {
    JSONValue* json_value;
    if ( decl.get_identifier() )
    {
      json_value = readJSONValue( decl );
      if ( json_value == NULL )
        return value;
    }
    else if ( root_json_value && !root_json_value->have_read )
    {
      if ( !root_json_value->is_map )
        json_value = root_json_value;
      else
        return value;
    }
    else
      return value;
    JSONUnmarshaller child_source_istream( decl, *json_value );
    child_source_istream.readSequence( *value );
    json_value->have_read = true;
  }
  return value;
}
Object* JSONUnmarshaller::readStruct( const Declaration& decl, Object* value )
{
  if ( value )
  {
    JSONValue* json_value;
    if ( decl.get_identifier() )
    {
      json_value = readJSONValue( decl );
      if ( json_value == NULL )
        return value;
    }
    else if ( root_json_value && !root_json_value->have_read )
    {
      if ( root_json_value->is_map )
        json_value = root_json_value;
      else
        return value;
    }
    else
      return value;
    JSONUnmarshaller child_source_istream( decl, *json_value );
    child_source_istream.readStruct( *value );
    json_value->have_read = true;
  }
  return value;
}
void JSONUnmarshaller::readSequence( Sequence& value )
{
  while ( next_json_value )
    value.unmarshal( *this );
}
void JSONUnmarshaller::readString( const Declaration& decl, std::string& str )
{
  JSONValue* json_value = readJSONValue( decl );
  if ( json_value )
  {
    if ( decl.get_identifier() ) // Read the value
    {
      if ( json_value->as_string != NULL )
        str.assign( static_cast<const std::string&>( *json_value->as_string ) );
    }
    else // Read the identifier
      str.assign( static_cast<const std::string&>( *json_value->identifier ) );
  }
}
void JSONUnmarshaller::readStruct( Object& s )
{
  s.unmarshal( *this );
}
JSONValue* JSONUnmarshaller::readJSONValue( const Declaration& decl )
{
  if ( root_json_value->is_map )
  {
    if ( decl.get_identifier() ) // Given a key, reading a value
    {
      JSONValue* child_json_value = root_json_value->child;
      while ( child_json_value )
      {
        if ( !child_json_value->have_read && *child_json_value->identifier == decl.get_identifier() )
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
  else
  {
    if ( next_json_value != NULL && !next_json_value->have_read )
    {
      JSONValue* json_value = next_json_value;
      next_json_value = json_value->next;
      json_value->have_read = true;
      return json_value;
    }
  }
  return NULL;
}


// named_pipe.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
auto_Object<NamedPipe> NamedPipe::open( const Path& path, uint32_t flags, mode_t mode )
{
#ifdef _WIN32
  Path named_pipe_base_dir_path( TEXT( "\\\\.\\pipe" ) );
  Path named_pipe_path( named_pipe_base_dir_path + path );
  if ( ( flags & O_CREAT ) == O_CREAT ) // Server
  {
    HANDLE hPipe = CreateNamedPipe( named_pipe_path, PIPE_ACCESS_DUPLEX, PIPE_TYPE_BYTE|PIPE_READMODE_BYTE|PIPE_WAIT, PIPE_UNLIMITED_INSTANCES, 4096, 4096, 0, NULL );
    if ( hPipe != INVALID_HANDLE_VALUE )
      return new NamedPipe( new File( hPipe ), false );
  }
  else // Client
  {
    auto_Object<File> underlying_file = File::open( named_pipe_path, flags );
    if ( underlying_file != NULL )
      return new NamedPipe( underlying_file, true );
  }
#else
  if ( ( flags & O_CREAT ) == O_CREAT )
  {
    if ( ::mkfifo( path, mode ) != -1 )
      flags ^= O_CREAT;
    else
      return NULL;
  }
  auto_Object<File> underlying_file = File::open( path, flags );
  if ( underlying_file != NULL )
    return new NamedPipe( underlying_file );
#endif
  return NULL;
}
#ifdef _WIN32
NamedPipe::NamedPipe( auto_Object<File> underlying_file, bool connected )
  : underlying_file( underlying_file ), connected( connected )
{ }
#else
NamedPipe::NamedPipe( auto_Object<File> underlying_file )
  : underlying_file( underlying_file )
{ }
#endif
#ifdef _WIN32
bool NamedPipe::connect()
{
  if ( connected )
    return true;
  else
  {
    if ( ConnectNamedPipe( *underlying_file, NULL ) != 0 ||
         GetLastError() == ERROR_PIPE_CONNECTED )
    {
      connected = true;
      return true;
    }
    else
      return false;
  }
}
#endif
ssize_t NamedPipe::read( void* buffer, size_t buffer_len )
{
#ifdef _WIN32
  if ( connect() )
    return underlying_file->read( buffer, buffer_len );
  else
    return -1;
#else
  return underlying_file->read( buffer, buffer_len );
#endif
}
ssize_t NamedPipe::write( const void* buffer, size_t buffer_len )
{
#ifdef _WIN32
  if ( connect() )
    return underlying_file->write( buffer, buffer_len );
  else
    return -1;
#else
  return underlying_file->write( buffer, buffer_len );
#endif
}
ssize_t NamedPipe::writev( const iovec* buffers, uint32_t buffers_count )
{
#ifdef _WIN32
  if ( connect() )
    return underlying_file->writev( buffers, buffers_count );
  else
    return -1;
#else
  return underlying_file->writev( buffers, buffers_count );
#endif
}
#ifdef _WIN32
#pragma warning( pop )
#endif


// oncrpc_message.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
template <class ONCRPCMessageType>
ONCRPCMessage<ONCRPCMessageType>::ONCRPCMessage()
{
  xid = 0;
  deserialize_state = DESERIALIZING_RECORD_FRAGMENT_MARKER;
}
template <class ONCRPCMessageType>
ONCRPCMessage<ONCRPCMessageType>::ONCRPCMessage( uint32_t xid, auto_Object<> body )
  : xid( xid ), body( body )
{
  deserialize_state = DESERIALIZING_RECORD_FRAGMENT_MARKER;
}
template <class ONCRPCMessageType>
ONCRPCMessage<ONCRPCMessageType>::~ONCRPCMessage()
{ }
template <class ONCRPCMessageType>
ssize_t ONCRPCMessage<ONCRPCMessageType>::deserialize( auto_Object<Buffer> buffer )
{
  switch ( deserialize_state )
  {
    case DESERIALIZING_RECORD_FRAGMENT_MARKER:
    {
      uint32_t record_fragment_marker = 0;
      size_t record_fragment_marker_filled = buffer->get( &record_fragment_marker, sizeof( record_fragment_marker ) );
      if ( record_fragment_marker_filled == sizeof( record_fragment_marker ) )
      {
#ifdef __MACH__
        record_fragment_marker = ntohl( record_fragment_marker );
#else
        record_fragment_marker = Machine::ntohl( record_fragment_marker );
#endif
        bool last_record_fragment;
        if ( record_fragment_marker & ( 1 << 31UL ) )
        {
          last_record_fragment = true;
          expected_record_fragment_length = record_fragment_marker ^ ( 1 << 31 );
        }
        else
        {
          last_record_fragment = false;
          expected_record_fragment_length = record_fragment_marker;
        }
        if ( expected_record_fragment_length > 32 * 1024 * 1024 ) DebugBreak();
        deserialize_state = DESERIALIZING_RECORD_FRAGMENT;
      }
      else if ( record_fragment_marker_filled == 0 )
        return sizeof( record_fragment_marker );
      else
      {
        DebugBreak();
        return sizeof( record_fragment_marker );
      }
    }
    // Drop down
    case DESERIALIZING_RECORD_FRAGMENT:
    {
      received_record_fragment_length = buffer->size();
      if ( received_record_fragment_length == expected_record_fragment_length ) // Common case
      {
        XDRUnmarshaller xdr_unmarshaller( buffer );
        static_cast<ONCRPCMessageType*>( this )->unmarshal( xdr_unmarshaller );
        deserialize_state = DESERIALIZE_DONE;
        return 0;
      }
      else
      {
        deserialize_state = DESERIALIZING_LONG_RECORD_FRAGMENT;
        current_record_fragment_buffer = first_record_fragment_buffer = buffer;
        return expected_record_fragment_length - received_record_fragment_length;
      }
    }
    break;
    case DESERIALIZING_LONG_RECORD_FRAGMENT:
    {
      current_record_fragment_buffer->set_next_buffer( buffer );
      current_record_fragment_buffer = buffer;
      received_record_fragment_length += buffer->size();
      if ( received_record_fragment_length == expected_record_fragment_length )
      {
        XDRUnmarshaller xdr_unmarshaller( first_record_fragment_buffer );
        static_cast<ONCRPCMessageType*>( this )->unmarshal( xdr_unmarshaller );
        deserialize_state = DESERIALIZE_DONE;
        return 0;
      }
      else
        return expected_record_fragment_length - received_record_fragment_length;
    }
    break;
    case DESERIALIZE_DONE: return 0;
  }
  DebugBreak();
  return -1;
}
template <class ONCRPCMessageType>
auto_Object<Buffer> ONCRPCMessage<ONCRPCMessageType>::serialize()
{
  XDRMarshaller xdr_marshaller;
  static_cast<ONCRPCMessageType*>( this )->marshal( xdr_marshaller );
  auto_Object<Buffer> xdr_buffer = xdr_marshaller.get_buffer();
  // Calculate the record fragment length from the sizes of all the buffers in the chain
  uint32_t record_fragment_length = 0;
  auto_Object<Buffer> next_xdr_buffer = xdr_buffer;
  while ( next_xdr_buffer != NULL )
  {
    record_fragment_length += next_xdr_buffer->size();
    next_xdr_buffer = next_xdr_buffer->get_next_buffer();
  }
  if ( record_fragment_length > 32 * 1024 * 1024 ) DebugBreak();
  uint32_t record_fragment_marker = record_fragment_length | ( 1 << 31 ); // Indicate that this is the last fragment
#ifdef __MACH__
  record_fragment_marker = htonl( record_fragment_marker );
#else
  record_fragment_marker = Machine::htonl( record_fragment_marker );
#endif
  auto_Object<Buffer> record_fragment_length_buffer = new StackBuffer<4>( &record_fragment_marker );
  record_fragment_length_buffer->set_next_buffer( xdr_buffer );
  return record_fragment_length_buffer;
}
template class ONCRPCMessage<ONCRPCRequest>;
template class ONCRPCMessage<ONCRPCResponse>;


// oncrpc_request.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
ONCRPCRequest::ONCRPCRequest( auto_Object<Connection> connection, auto_Object<Interface> _interface )
  : ProtocolRequest( connection ), _interface( _interface )
{ }
ONCRPCRequest::ONCRPCRequest( auto_Object<Interface> _interface, uint32_t prog, uint32_t proc, uint32_t vers, auto_Object<> body )
  : ONCRPCMessage<ONCRPCRequest>( static_cast<uint32_t>( Time::getCurrentUnixTimeS() ), body ),
    _interface( _interface ),
    prog( prog ), proc( proc ), vers( vers )
{
  credential_auth_flavor = AUTH_NONE;
}
ONCRPCRequest::ONCRPCRequest( auto_Object<Interface> _interface, uint32_t prog, uint32_t proc, uint32_t vers, uint32_t credential_auth_flavor, auto_Object<> credential, auto_Object<> body )
  : ONCRPCMessage<ONCRPCRequest>( static_cast<uint32_t>( Time::getCurrentUnixTimeS() ), body ),
    _interface( _interface ),
    prog( prog ), proc( proc ), vers( vers ),
    credential_auth_flavor( credential_auth_flavor ), credential( credential )
{ }
void ONCRPCRequest::marshal( Marshaller& marshaller )
{
  marshaller.writeInt32( "xid", xid );
  marshaller.writeInt32( "msg_type", 0 ); // MSG_CALL
  marshaller.writeInt32( "rpcvers", 2 );
  marshaller.writeInt32( "prog", prog );
  marshaller.writeInt32( "vers", vers );
  marshaller.writeInt32( "proc", proc );
  marshaller.writeInt32( "credential_auth_flavor", credential_auth_flavor );
  if ( credential_auth_flavor == AUTH_NONE || credential == NULL )
    marshaller.writeInt32( "credential_auth_body_length", 0 );
  else
  {
    XDRMarshaller credential_auth_body_xdr_marshaller;
    credential->marshal( credential_auth_body_xdr_marshaller );
    marshaller.writeBuffer( "credential_auth_body", credential_auth_body_xdr_marshaller.get_buffer() );
  }
  marshaller.writeInt32( "verf_auth_flavor", AUTH_NONE );
  marshaller.writeInt32( "verf_auth_body_length", 0 );
  marshaller.writeStruct( XDRMarshaller::Declaration(), *body );
}
bool ONCRPCRequest::respond( Response& response )
{
  if ( this->get_response_target() == NULL )
  {
    auto_Object<> body = get_body();
    Request* interface_request = _interface->checkRequest( *body );
    if ( interface_request != NULL )
    {
      if ( response.get_tag() == YIELD_OBJECT_TAG( ONCRPCResponse ) )
      {
        ONCRPCResponse& oncrpc_response = static_cast<ONCRPCResponse&>( response );
        auto_Object<> oncrpc_response_body = oncrpc_response.get_body();
        Response* interface_response = _interface->checkResponse( *oncrpc_response_body );
        if ( interface_response != NULL )
        {
          Object::decRef( response );
          return interface_request->respond( interface_response->incRef() );
        }
        else if ( oncrpc_response_body->get_tag() == YIELD_OBJECT_TAG( ExceptionResponse ) )
        {
          Object::decRef( response );
          return interface_request->respond( static_cast<ExceptionResponse&>( *oncrpc_response_body.release() ) );
        }
      }
      else
        return interface_request->respond( response );
    }
  }
  return ProtocolRequest::respond( response );
}
void ONCRPCRequest::unmarshal( Unmarshaller& unmarshaller )
{
  xid = unmarshaller.readUint32( "xid" );
  int32_t msg_type = unmarshaller.readInt32( "msg_type" );
  if ( msg_type == 0 ) // CALL
  {
    uint32_t rpcvers = unmarshaller.readUint32( "rpcvers" );
    if ( rpcvers == 2 )
    {
      unmarshaller.readUint32( "prog" );
      unmarshaller.readUint32( "vers" );
      uint32_t proc = unmarshaller.readUint32( "proc" );
      unmarshaller.readUint32( "credential_auth_flavor" );
      std::string credential_auth_body;
      unmarshaller.readString( "credential_auth_body", credential_auth_body );
      unmarshaller.readUint32( "verf_auth_flavor" );
      uint32_t verf_auth_body_length = unmarshaller.readUint32( "credential_auth_body_length" );
      if ( verf_auth_body_length > 0 )
        DebugBreak();
      if ( body != NULL )
        unmarshaller.readStruct( Unmarshaller::Declaration(), body.get() );
      else
      {
        if ( _interface != NULL )
        {
          body = _interface->createRequest( proc ).release();
          unmarshaller.readStruct( Unmarshaller::Declaration(), body.get() );
        }
      }
    }
  }
}


// oncrpc_response.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
ONCRPCResponse::ONCRPCResponse( auto_Object<ONCRPCRequest> oncrpc_request, auto_Object<> body )
  : ProtocolResponse<ONCRPCRequest>( oncrpc_request ),
    ONCRPCMessage<ONCRPCResponse>( oncrpc_request->get_xid(), body )
{ }
ONCRPCResponse::ONCRPCResponse( uint32_t xid, auto_Object<> body )
  : ProtocolResponse<ONCRPCRequest>( NULL ), ONCRPCMessage<ONCRPCResponse>( xid, body )
{ }
void ONCRPCResponse::marshal( Marshaller& marshaller )
{
  marshaller.writeInt32( "xid", xid );
  marshaller.writeInt32( "msg_type", 1 ); // MSG_REPLY
  marshaller.writeInt32( "reply_stat", 0 ); // MSG_ACCEPTED
  marshaller.writeInt32( "verf_auth_flavor", 0 );
  marshaller.writeInt32( "verf_authbody_length", 0 );
  if ( body == NULL || body->get_tag() != YIELD_OBJECT_TAG( ExceptionResponse ) )
    marshaller.writeInt32( "accept_stat", 0 ); // SUCCESS
  else
    marshaller.writeInt32( "accept_stat", 5 ); // SYSTEM_ERR
  marshaller.writeStruct( Marshaller::Declaration(), *body );
}
void ONCRPCResponse::unmarshal( Unmarshaller& unmarshaller )
{
  xid = unmarshaller.readUint32( "xid" );
  int32_t msg_type = unmarshaller.readInt32( "msg_type" );
  if ( msg_type == 1 ) // REPLY
  {
    uint32_t reply_stat = unmarshaller.readUint32( "reply_stat" );
    if ( reply_stat == 0 ) // MSG_ACCEPTED
    {
      uint32_t verf_auth_flavor = unmarshaller.readUint32( "verf_auth_flavor" );
      uint32_t verf_authbody_length = unmarshaller.readUint32( "verf_authbody_length" );
      if ( verf_auth_flavor == 0 && verf_authbody_length == 0 )
      {
        uint32_t accept_stat = unmarshaller.readUint32( "accept_stat" );
        switch ( accept_stat )
        {
          case 0:
          {
            if ( body != NULL )
              unmarshaller.readStruct( XDRUnmarshaller::Declaration(), body.get() );
            else
            {
              body = get_protocol_request()->get_interface()->createResponse( get_protocol_request()->get_body()->get_tag() ).release();
              if ( body != NULL )
                unmarshaller.readStruct( XDRUnmarshaller::Declaration(), body.get() );
            }
          }
          break;
          case 1: body = new ExceptionResponse( "ONC-RPC exception: program unavailable" ); break;
          case 2: body = new ExceptionResponse( "ONC-RPC exception: program mismatch" ); break;
          case 3: body = new ExceptionResponse( "ONC-RPC exception: procedure unavailable" ); break;
          case 4: body = new ExceptionResponse( "ONC-RPC exception: garbage arguments" ); break;
          case 5: body = new ExceptionResponse( "ONC-RPC exception: system error" ); break;
          default:
          {
            body = get_protocol_request()->get_interface()->createExceptionResponse( accept_stat ).release();
            if ( body != NULL )
              unmarshaller.readStruct( XDRUnmarshaller::Declaration(), body.get() );
            else
              body = new ExceptionResponse( "ONC-RPC exception: system error" );
          }
          break;
        }
      }
      else
        body = new ExceptionResponse( "ONC-RPC exception: received unexpected verification body on response" );
    }
    else if ( reply_stat == 1 ) // MSG_REJECTED
      body = new ExceptionResponse( "ONC-RPC exception: received MSG_REJECTED reply_stat" );
    else
      body = new ExceptionResponse( "ONC-RPC exception: received unknown reply_stat" );
  }
  else
    body = new ExceptionResponse( "ONC-RPC exception: received unknown msg_type" );
}


// oncrpc_server.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
namespace YIELD
{
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
      return oncrpc_response_writer_stage->send( *( new ONCRPCResponse( oncrpc_request, ev ) ) );
    }
  private:
    auto_Object<ONCRPCRequest> oncrpc_request;
    auto_Object<Stage> oncrpc_response_writer_stage;
  };
};
auto_Object<ONCRPCServer> ONCRPCServer::create( const URI& absolute_uri,
                                                auto_Object<Interface> _interface,
                                                auto_Object<StageGroup> stage_group,
                                                auto_Object<Log> log,
                                                auto_Object<SSLContext> ssl_context )
{
  auto_Object<SocketAddress> sockname = SocketAddress::create( absolute_uri );
  if ( sockname != NULL )
  {
    auto_Object<EventQueue> oncrpc_server_event_queue;
    if ( absolute_uri.get_scheme() == "oncrpcu" )
      oncrpc_server_event_queue = UDPRecvFromQueue::create( sockname ).release();
    else
    {
      auto_Object<TCPListenQueue> tcp_listen_queue;
#ifdef YIELD_HAVE_OPENSSL
      if ( absolute_uri.get_scheme() == "oncrpcs" && ssl_context != NULL )
        oncrpc_server_event_queue = SSLListenQueue::create( sockname, ssl_context ).release();
      else
#endif
        oncrpc_server_event_queue = TCPListenQueue::create( sockname ).release();
    }
    if ( oncrpc_server_event_queue != NULL )
    {
      auto_Object<ONCRPCServer> oncrpc_server = new ONCRPCServer( _interface, log );
      stage_group->createStage( oncrpc_server->incRef(), 1, oncrpc_server_event_queue.release(), NULL, log );
      return oncrpc_server;
    }
  }
  return NULL;
}
void ONCRPCServer::handleDeserializedProtocolMessage( auto_Object<ONCRPCRequest> oncrpc_request )
{
  auto_Object<> oncrpc_request_body = oncrpc_request->get_body();
  Request* interface_request = _interface->checkRequest( *oncrpc_request_body );
  if ( interface_request != NULL )
  {
    oncrpc_request_body.release();
    interface_request->set_response_target( new ONCRPCResponder( oncrpc_request, get_oncrpc_response_writer_stage() ) );
    _interface->send( *interface_request );
  }
  else
    DebugBreak();
}


// peer.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
template <class ReadProtocolMessageType, class WriteProtocolMessageType>
Peer<ReadProtocolMessageType, WriteProtocolMessageType>::Peer( auto_Object<Log> log )
  : log( log )
{ }
template <class ReadProtocolMessageType, class WriteProtocolMessageType>
void Peer<ReadProtocolMessageType, WriteProtocolMessageType>::attach( auto_Object<Connection> connection, auto_Object<> context, bool enable_read, bool enable_write )
{
  if ( fd_event_queue != NULL )
    fd_event_queue->attach( *connection->get_socket(), context, enable_read, enable_write );
  else
    io_completion_port->attach( *connection->get_socket() );
}
template <class ReadProtocolMessageType, class WriteProtocolMessageType>
ssize_t Peer<ReadProtocolMessageType, WriteProtocolMessageType>::deserialize( auto_Object<ReadProtocolMessageType> protocol_message, auto_Object<Buffer> buffer )
{
  return protocol_message->deserialize( buffer );
}
template <class ReadProtocolMessageType, class WriteProtocolMessageType>
void Peer<ReadProtocolMessageType, WriteProtocolMessageType>::detach( auto_Object<Connection> connection )
{
  if ( fd_event_queue != NULL )
    fd_event_queue->detach( *connection->get_socket() );
}
template <class ReadProtocolMessageType, class WriteProtocolMessageType>
void Peer<ReadProtocolMessageType, WriteProtocolMessageType>::handleEvent( Event& ev )
{
  switch ( ev.get_tag() )
  {
    case YIELD_OBJECT_TAG( Socket::AIOReadControlBlock ):
    {
      Socket::AIOReadControlBlock& aio_read_control_block = static_cast<Socket::AIOReadControlBlock&>( ev );
      auto_Object<ReadProtocolMessageType> protocol_message( static_cast<ReadProtocolMessageType*>( aio_read_control_block.get_context().release() ) );
      auto_Object<Buffer> buffer = aio_read_control_block.get_buffer();
      ssize_t deserialize_ret = deserialize( protocol_message, buffer );
      if ( deserialize_ret == 0 )
      {
        detach( protocol_message->get_connection() );
        handleDeserializedProtocolMessage( protocol_message );
      }
      else if ( deserialize_ret > 0 )
        read( protocol_message, deserialize_ret );
      Object::decRef( ev );
    }
    break;
    case YIELD_OBJECT_TAG( FDEventQueue::POLLINEvent ):
    {
      FDEventQueue::POLLINEvent& pollin_event = static_cast<FDEventQueue::POLLINEvent&>( ev );
      read( static_cast<ReadProtocolMessageType*>( pollin_event.get_context().release() ) );
      Object::decRef( ev );
    }
    break;
    case YIELD_OBJECT_TAG( ReadProtocolMessageType ):
    {
      read( static_cast<ReadProtocolMessageType&>( ev ) );
    }
    break;
    case YIELD_OBJECT_TAG( StageStartupEvent ):
    {
      StageStartupEvent& stage_startup_event = static_cast<StageStartupEvent&>( ev );
      my_stage = stage_startup_event.get_stage();
      auto_Object<EventQueue> my_event_queue = my_stage->get_event_queue();
      switch ( my_event_queue->get_tag() )
      {
        case YIELD_OBJECT_TAG( FDEventQueue ): fd_event_queue = static_cast<FDEventQueue*>( my_event_queue.release() ); break;
        case YIELD_OBJECT_TAG( IOCompletionPort ): io_completion_port = static_cast<IOCompletionPort*>( my_event_queue.release() ); break;
        default: DebugBreak();
      }
      Object::decRef( ev );
    }
    return;
    case YIELD_OBJECT_TAG( StageShutdownEvent ):
    {
//      my_stage = NULL;
      Object::decRef( ev );
    }
    return;
    case YIELD_OBJECT_TAG( WriteProtocolMessageType ):
    {
      auto_Object<WriteProtocolMessageType> protocol_message( static_cast<WriteProtocolMessageType&>( ev ) );
      auto_Object<Buffer> buffer = serialize( protocol_message );
      write( protocol_message, buffer );
    }
    break;
    default:
    {
      Object::decRef( ev );
    }
    break;
  }
}
template <class ReadProtocolMessageType, class WriteProtocolMessageType>
bool Peer<ReadProtocolMessageType, WriteProtocolMessageType>::read( auto_Object<ReadProtocolMessageType> protocol_message, size_t buffer_capacity )
{
  auto_Object<Connection> connection( protocol_message->get_connection() );
  connection->get_socket()->set_blocking_mode( false );
  attach( connection, protocol_message->incRef(), false, false );
  for ( ;; )
  {
    auto_Object<Buffer> buffer( new HeapBuffer( buffer_capacity ) );
    ssize_t read_ret;
    if ( haveAIO() )
      read_ret = connection->get_socket()->aio_read( new Socket::AIOReadControlBlock( buffer, protocol_message->incRef() ) );
    else
      read_ret = connection->get_socket()->read( buffer );
    if ( read_ret > 0 )
    {
      ssize_t deserialize_ret = deserialize( protocol_message, buffer );
      if ( deserialize_ret == 0 )
      {
        detach( connection );
        connection->get_socket()->set_blocking_mode( true );
        handleDeserializedProtocolMessage( protocol_message );
        return true;
      }
      else if ( deserialize_ret > 0 )
      {
        buffer_capacity = static_cast<size_t>( deserialize_ret );
        continue;
      }
      else
        break;
    }
    else if ( read_ret < 0 )
    {
      if ( connection->get_socket()->want_read() )
      {
        if ( fd_event_queue != NULL )
          fd_event_queue->toggle( *connection->get_socket(), true, false );
        return true;
      }
      else if ( connection->get_socket()->want_write() )
      {
        if ( fd_event_queue != NULL )
          fd_event_queue->toggle( *connection->get_socket(), false, true );
        return true;
      }
      else
        break;
    }
    else
      break;
  }
  detach( connection );
  connection->get_socket()->close();
  return false;
}
template <class ReadProtocolMessageType, class WriteProtocolMessageType>
auto_Object<Buffer>  Peer<ReadProtocolMessageType, WriteProtocolMessageType>::serialize( auto_Object<WriteProtocolMessageType> protocol_message )
{
  return protocol_message->serialize();
}
template <class ReadProtocolMessageType, class WriteProtocolMessageType>
bool Peer<ReadProtocolMessageType, WriteProtocolMessageType>::write( auto_Object<WriteProtocolMessageType> protocol_message, auto_Object<Buffer> buffer )
{
  auto_Object<Connection> connection( protocol_message->get_connection() );
  connection->get_socket()->set_blocking_mode( true );
  ssize_t write_ret = connection->get_socket()->write( buffer );
  return write_ret >= 0;
}
template class Peer<HTTPRequest, HTTPResponse>;
template class Peer<HTTPResponse, HTTPRequest>;
template class Peer<ONCRPCRequest, ONCRPCResponse>;
template class Peer<ONCRPCResponse, ONCRPCRequest>;
#ifdef _WIN32
#pragma warning( pop )
#endif


// pipe.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#include <windows.h>
#endif
auto_Object<Pipe> Pipe::create()
{
#ifdef _WIN32
  SECURITY_ATTRIBUTES pipe_security_attributes;
  pipe_security_attributes.nLength = sizeof( SECURITY_ATTRIBUTES );
  pipe_security_attributes.bInheritHandle = TRUE;
  pipe_security_attributes.lpSecurityDescriptor = NULL;
  void* ends[2];
  if ( CreatePipe( &ends[0], &ends[1], &pipe_security_attributes, 0 ) )
  {
    if ( SetHandleInformation( ends[0], HANDLE_FLAG_INHERIT, 0 ) &&
         SetHandleInformation( ends[1], HANDLE_FLAG_INHERIT, 0 ) )
      return new Pipe( ends );
    else
    {
      CloseHandle( ends[0] );
      CloseHandle( ends[1] );
    }
  }
#else
  int ends[2];
  if ( ::pipe( ends ) != -1 )
    return new Pipe( ends );
#endif
  return NULL;
}
#ifdef _WIN32
Pipe::Pipe( void* ends[2] )
#else
Pipe::Pipe( int ends[2] )
#endif
{
  this->ends[0] = ends[0];
  this->ends[1] = ends[1];
}
ssize_t Pipe::read( void* buffer, size_t buffer_len )
{
#ifdef _WIN32
  DWORD dwBytesRead;
  if ( ::ReadFile( ends[0], buffer, buffer_len, &dwBytesRead, NULL ) )
    return static_cast<ssize_t>( dwBytesRead );
  else
    return -1;
#else
  return ::read( ends[0], buffer, buffer_len );
#endif
}
ssize_t Pipe::write( const void* buffer, size_t buffer_len )
{
#ifdef _WIN32
  DWORD dwBytesWritten;
  if ( ::WriteFile( ends[1], buffer, buffer_len, &dwBytesWritten, NULL ) )
    return static_cast<ssize_t>( dwBytesWritten );
  else
    return -1;
#else
  return ::write( ends[1], buffer, buffer_len );
#endif
}


// process.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if defined(_WIN32)
#include <windows.h>
#else
#include <signal.h>
#include <sys/wait.h> // For waitpid
#endif
auto_Object<Process> Process::create( const Path& command_line )
{
#ifdef _WIN32
  auto_Object<Pipe> child_stdin, child_stdout, child_stderr;
  //auto_Object<Pipe> child_stdin = Pipe::create(),
  //                  child_stdout = Pipe::create(),
  //                  child_stderr = Pipe::create();
  STARTUPINFO startup_info;
  ZeroMemory( &startup_info, sizeof( STARTUPINFO ) );
  startup_info.cb = sizeof( STARTUPINFO );
  //startup_info.hStdInput = *child_stdin->get_input_stream()->get_file();
  //startup_info.hStdOutput = *child_stdout->get_output_stream()->get_file();
  //startup_info.hStdError = *child_stdout->get_output_stream()->get_file();
  //startup_info.dwFlags = STARTF_USESTDHANDLES;
  PROCESS_INFORMATION proc_info;
  ZeroMemory( &proc_info, sizeof( PROCESS_INFORMATION ) );
  if ( CreateProcess( NULL, const_cast<wchar_t*>( command_line.get_wide_path().c_str() ) , NULL, NULL, TRUE, CREATE_NO_WINDOW, NULL, NULL, &startup_info, &proc_info ) )
    return new Process( proc_info.hProcess, proc_info.hThread, child_stdin, child_stdout, child_stderr );
  else
    return NULL;
#else
  const char* argv[] = { static_cast<const char*>( NULL ) };
  return create( command_line, argv );
#endif
}
auto_Object<Process> Process::create( int argc, char** argv )
{
  std::vector<char*> argvv;
  for ( int arg_i = 1; arg_i < argc; arg_i++ )
    argvv.push_back( argv[arg_i] );
  argvv.push_back( NULL );
  return create( argv[0], const_cast<const char**>( &argvv[0] ) );
}
auto_Object<Process> Process::create( const Path& executable_file_path, const char** null_terminated_argv )
{
#ifdef _WIN32
  const std::string& executable_file_path_str = static_cast<const std::string&>( executable_file_path );
  std::string command_line;
  if ( executable_file_path_str.find( ' ' ) == -1 )
    command_line.append( executable_file_path_str );
  else
  {
    command_line.append( "\"", 1 );
    command_line.append( executable_file_path_str );
    command_line.append( "\"", 1 );
  }
  size_t arg_i = 0;
  while ( null_terminated_argv[arg_i] != NULL )
  {
    command_line.append( " ", 1 );
    command_line.append( null_terminated_argv[arg_i] );
    arg_i++;
  }
  return create( command_line );
#else
  auto_Object<Pipe> child_stdin, child_stdout, child_stderr;
  //auto_Object<Pipe> child_stdin = Pipe::create(),
  //                  child_stdout = Pipe::create(),
  //                  child_stderr = Pipe::create();
  pid_t child_pid = fork();
  if ( child_pid == -1 )
    return NULL;
  else if ( child_pid == 0 ) // Child
  {
    //close( STDIN_FILENO );
    //dup2( *child_stdin->get_input_stream()->get_file(), STDIN_FILENO ); // Set stdin to read end of stdin pipe
    //close( STDOUT_FILENO );
    //dup2( *child_stdout->get_output_stream()->get_file(), STDOUT_FILENO ); // Set stdout to write end of stdout pipe
    //close( STDERR_FILENO );
    //dup2( *child_stderr->get_output_stream()->get_file(), STDERR_FILENO ); // Set stderr to write end of stderr pipe
    std::vector<char*> argv_with_executable_file_path;
    argv_with_executable_file_path.push_back( const_cast<char*>( static_cast<const char*>( executable_file_path ) ) );
    size_t arg_i = 0;
    while ( null_terminated_argv[arg_i] != NULL )
    {
      argv_with_executable_file_path.push_back( const_cast<char*>( null_terminated_argv[arg_i] ) );
      arg_i++;
    }
    argv_with_executable_file_path.push_back( NULL );
    execv( executable_file_path, &argv_with_executable_file_path[0] );
    return NULL;
  }
  else // Parent
    return new Process( child_pid, child_stdin, child_stdout, child_stderr );
#endif
}
#ifdef _WIN32
Process::Process( HANDLE hChildProcess, HANDLE hChildThread, auto_Object<Pipe> child_stdin, auto_Object<Pipe> child_stdout, auto_Object<Pipe> child_stderr )
  : hChildProcess( hChildProcess ), hChildThread( hChildThread ),
    child_stdin( child_stdin ), child_stdout( child_stdout ), child_stderr( child_stderr )
{ }
#else
Process::Process( pid_t child_pid, auto_Object<Pipe> child_stdin, auto_Object<Pipe> child_stdout, auto_Object<Pipe> child_stderr )
  : child_pid( child_pid ),
    child_stdin( child_stdin ), child_stdout( child_stdout ), child_stderr( child_stderr )
{ }
#endif
Process::~Process()
{
#ifdef _WIN32
  CloseHandle( hChildProcess );
  CloseHandle( hChildThread );
#endif
}
bool Process::kill()
{
#ifdef _WIN32
  return TerminateProcess( hChildProcess, 0 ) == TRUE;
#else
  return ::kill( child_pid, SIGKILL ) == 0;
#endif
}
bool Process::poll( int* out_return_code )
{
#ifdef _WIN32
  if ( WaitForSingleObject( hChildProcess, 0 ) != WAIT_TIMEOUT )
  {
    if ( out_return_code )
    {
      DWORD dwChildExitCode;
      GetExitCodeProcess( hChildProcess, &dwChildExitCode );
      *out_return_code = ( int )dwChildExitCode;
    }
    return true;
  }
  else
    return false;
#else
  return waitpid( child_pid, out_return_code, WNOHANG ) >= 0;
#endif
}
bool Process::terminate()
{
#ifdef _WIN32
  return TerminateProcess( hChildProcess, 0 ) == TRUE;
#else
  return ::kill( child_pid, SIGTERM ) == 0;
#endif
}
int Process::wait()
{
#ifdef _WIN32
  WaitForSingleObject( hChildProcess, INFINITE );
  DWORD dwChildExitCode;
  GetExitCodeProcess( hChildProcess, &dwChildExitCode );
  return ( int )dwChildExitCode;
#else
  int stat_loc;
  if ( waitpid( child_pid, &stat_loc, 0 ) >= 0 )
    return stat_loc;
  else
    return -1;
#endif
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
    memcpy_s( heap_buffer, heap_buffer_len, stack_buffer, buffer_p - stack_buffer );
    buffer_p = heap_buffer + ( buffer_p - stack_buffer );
  }
  else
  {
    heap_buffer_len += 512;
    char* new_heap_buffer = new char[heap_buffer_len];
    memcpy_s( new_heap_buffer, heap_buffer_len, heap_buffer, buffer_p - heap_buffer );
    buffer_p = new_heap_buffer + ( buffer_p - heap_buffer );
    delete [] heap_buffer;
    heap_buffer = new_heap_buffer;
  }
}
void RFC822Headers::copy_iovec( const char* data, size_t len )
{
  if ( heap_buffer == NULL )
  {
    if ( ( buffer_p + len - stack_buffer ) > YIELD_RFC822_HEADERS_STACK_BUFFER_LENGTH )
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
    memcpy_s( new_heap_buffer, heap_buffer_len, heap_buffer, buffer_p - heap_buffer );
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
  memcpy_s( buffer_p, len, data, len );
  buffer_p += len;
  if ( data[len-1] == 0 ) len--;
  set_iovec( buffer_p_before, len );
}
ssize_t RFC822Headers::deserialize( auto_Object<Buffer> buffer )
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
          if ( buffer->get( &c, 1 ) == 1 )
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
            return 1;
        }
      }
      // Fall through
      case DESERIALIZING_HEADER_NAME:
      {
        char c;
        if ( buffer->get( &c, 1 ) == 1 )
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
                if ( buffer->get( buffer_p, 1 ) )
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
                  return 1;
              }
            }
            break;
          }
        }
        else
          return 1;
      }
      // Fall through
      case DESERIALIZING_HEADER_NAME_VALUE_SEPARATOR:
      {
        char c;
        for ( ;; )
        {
          if ( buffer->get( &c, 1 ) == 1 )
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
            return 1;
        }
      }
      // Fall through
      case DESERIALIZING_HEADER_VALUE:
      {
        for ( ;; )
        {
          if ( buffer->get( buffer_p, 1 ) == 1 )
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
            return 1;
        }
      }
      // Fall through
      case DESERIALIZING_HEADER_VALUE_TERMINATOR:
      {
        char c;
        for ( ;; )
        {
          if ( buffer->get( &c, 1 ) == 1 )
          {
            if ( c == '\n' )
            {
              deserialize_state = DESERIALIZING_HEADER_NAME;
              break;
            }
          }
          else
            return 1;
        }
      }
      continue; // To the next header name
      case DESERIALIZING_TRAILING_CRLF:
      {
        char c;
        for ( ;; )
        {
          if ( buffer->get( &c, 1 ) == 1 )
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
                size_t header_name_len = strnlen( header_name, UINT16_MAX );
                temp_buffer_p += header_name_len + 1;
                const char* header_value = temp_buffer_p;
                size_t header_value_len = strnlen( header_value, UINT16_MAX );
                temp_buffer_p += header_value_len + 1;
                set_iovec( header_name, header_name_len );
                set_iovec( ": ", 2 );
                set_iovec( header_value, header_value_len );
                set_iovec( "\r\n", 2 );
              }
              deserialize_state = DESERIALIZE_DONE;
              return 0;
            }
          }
          else
            return 1;
        }
        case DESERIALIZE_DONE: return 0;
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
auto_Object<Buffer> RFC822Headers::serialize()
{
  return new GatherBuffer( heap_iovecs != NULL ? heap_iovecs : stack_iovecs, iovecs_filled );
}
//void RFC822Headers::set_header( const char* header, size_t header_len )
//{
//  DebugBreak(); // TODO: Separate header name and value
//  /*
//  if ( header[header_len-1] != '\n' )
//  {
//    copy_iovec( header, header_len );
//    set_iovec( "\r\n", 2 );
//  }
//  else
//    copy_iovec( header, header_len );
//    */
//}
void RFC822Headers::set_header( const char* header_name, const char* header_value )
{
  set_iovec( header_name, strnlen( header_name, UINT16_MAX ) );
  set_iovec( ": ", 2 );
  set_iovec( header_value, strnlen( header_value, UINT16_MAX ) );
  set_iovec( "\r\n", 2 );
}
void RFC822Headers::set_header( const char* header_name, char* header_value )
{
  set_iovec( header_name, strnlen( header_name, UINT16_MAX ) );
  set_iovec( ": ", 2 );
  copy_iovec( header_value, strnlen( header_value, UINT16_MAX ) );
  set_iovec( "\r\n", 2 );
}
void RFC822Headers::set_header( char* header_name, char* header_value )
{
  copy_iovec( header_name, strnlen( header_name, UINT16_MAX ) );
  set_iovec( ": ", 2 );
  copy_iovec( header_value, strnlen( header_value, UINT16_MAX ) );
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
    if ( iovecs_filled < YIELD_RFC822_HEADERS_STACK_IOVECS_LENGTH )
      stack_iovecs[iovecs_filled] = iovec;
    else
    {
      heap_iovecs = new struct iovec[UCHAR_MAX];
      memcpy_s( heap_iovecs, sizeof( *heap_iovecs ), stack_iovecs, sizeof( stack_iovecs ) );
      heap_iovecs[iovecs_filled] = iovec;
    }
  }
  else if ( iovecs_filled < UCHAR_MAX )
    heap_iovecs[iovecs_filled] = iovec;
  else
    DebugBreak();
  iovecs_filled++;
}


// server.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).



template <class ProtocolRequestType, class ProtocolResponseType>
void Server<ProtocolRequestType, ProtocolResponseType>::handleEvent( Event& ev )
{
  switch ( ev.get_tag() )
  {
#ifdef YIELD_HAVE_OPENSSL
    case YIELD_OBJECT_TAG( SSLSocket ):
#endif
    case YIELD_OBJECT_TAG( TCPSocket ):
    case YIELD_OBJECT_TAG( UDPSocket ):
    {
      auto_Object<Socket> _socket = static_cast<Socket&>( ev );
      auto_Object<Log> log = this->get_log();
      if ( log != NULL && log->get_level() >= Log::LOG_INFO && static_cast<int>( *_socket ) != -1 )
        _socket = new TracingSocket( _socket, log );
      Connection* connection = new Connection( _socket );
      get_protocol_request_reader_stage()->send( *connection );
    }
    break;

    case YIELD_OBJECT_TAG( Connection ):
    {
      auto_Object<Connection> connection = static_cast<Connection&>( ev );
      auto_Object<ProtocolRequestType> protocol_request( createProtocolRequest( connection ) );
      protocol_request->set_connection( connection );
      this->read( protocol_request );
    }
    break;

    default:
    {
      Peer<ProtocolRequestType, ProtocolResponseType>::handleEvent( ev );
    }
    break;
  }
}

template <class ProtocolRequestType, class ProtocolResponseType>
bool Server<ProtocolRequestType, ProtocolResponseType>::write( auto_Object<ProtocolResponseType> protocol_response, auto_Object<Buffer> buffer )
{
  if ( Peer<ProtocolRequestType, ProtocolResponseType>::write( protocol_response, buffer ) )
  {
    get_protocol_request_reader_stage()->send( *protocol_response->get_connection().release() );
    return true;
  }
  else
    return false;
}


template class Server<HTTPRequest, HTTPResponse>;
template class Server<ONCRPCRequest, ONCRPCResponse>;


// socket.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#pragma comment( lib, "ws2_32.lib" )
#else
#include <sys/socket.h>
#endif
Socket::Socket( int domain, int type, int protocol, int _socket )
  : domain( domain ), type( type ), protocol( protocol ), _socket( _socket )
{
  blocking_mode = true;
}
Socket::~Socket()
{
  close();
}
bool Socket::aio_connect( auto_Object<AIOConnectControlBlock> aio_connect_control_block )
{
  return connect( aio_connect_control_block->get_peername() );
}
ssize_t Socket::aio_read( auto_Object<AIOReadControlBlock> aio_read_control_block )
{
  if ( read_buffer != NULL )
    return read( aio_read_control_block->get_buffer() );
  else
  {
#ifdef _WIN32
    auto_Object<Buffer> buffer = aio_read_control_block->get_buffer();
    WSABUF wsabuf[1];
    wsabuf[0].buf = static_cast<CHAR*>( static_cast<void*>( *buffer ) );
    wsabuf[0].len = buffer->capacity() - buffer->size();
    DWORD dwNumberOfBytesReceived, dwFlags = 0;
    if ( ::WSARecv( _socket, wsabuf, 1, &dwNumberOfBytesReceived, &dwFlags, *aio_read_control_block, NULL ) == 0 )
    {
      // The read returned immediately with data
      aio_read_control_block->onCompletion( dwNumberOfBytesReceived );
      aio_read_control_block.release(); // The AIOCB will still come back to the completion port, so keep a reference
      return static_cast<ssize_t>( dwNumberOfBytesReceived );
    }
    else if ( ::WSAGetLastError() == WSA_IO_PENDING )
      aio_read_control_block.release(); // No data available, the AIOCB will come back to the completion port when it is; keep a reference
    return -1;
#else
    return read( aio_read_control_block->get_buffer() );
#endif
  }
}
bool Socket::bind( auto_Object<SocketAddress> to_sockaddr )
{
  for ( ;; )
  {
    struct sockaddr* name; socklen_t namelen;
    if ( to_sockaddr->as_struct_sockaddr( domain, name, namelen ) )
    {
      if ( ::bind( *this, name, namelen ) != -1 )
        return true;
    }
    if ( domain == AF_INET6 &&
#ifdef _WIN32
        ::WSAGetLastError() == WSAEAFNOSUPPORT )
#else
        errno == EAFNOSUPPORT )
#endif
    {
      close();
      this->domain = AF_INET;
      _socket = ::socket( AF_INET, type, protocol );
      if ( !blocking_mode )
        set_blocking_mode( false );
    }
    else
      return false;
  }
}
bool Socket::close()
{
#ifdef _WIN32
  return ::closesocket( _socket ) != SOCKET_ERROR;
#else
  return ::close( _socket ) != -1;
#endif
}
bool Socket::connect( auto_Object<SocketAddress> to_sockaddr )
{
  for ( ;; )
  {
    struct sockaddr* name; socklen_t namelen;
    if ( to_sockaddr->as_struct_sockaddr( domain, name, namelen ) )
    {
      if ( ::connect( *this, name, namelen ) != -1 )
        return true;
      else
      {
#ifdef _WIN32
        switch ( ::WSAGetLastError() )
        {
          case WSAEISCONN: return true;
          case WSAEWOULDBLOCK:
          case WSAEINPROGRESS:
          case WSAEINVAL: return false;
          case WSAEAFNOSUPPORT:
#else
        switch ( errno )
        {
          case EISCONN: return true;
          case EWOULDBLOCK:
          case EINPROGRESS: return false;
          case EAFNOSUPPORT:
#endif
          {
            if ( domain == AF_INET6 )
            {
              close();
              domain = AF_INET; // Fall back to IPv4
              _socket = ::socket( domain, type, protocol );
              if ( !blocking_mode )
                set_blocking_mode( false );
              continue; // Try to connect again
            }
            else
              return false;
          }
          break;
          default: return false;
        }
      }
    }
    else if ( domain == AF_INET6 )
    {
      close();
      domain = AF_INET; // Fall back to IPv4
      _socket = ::socket( domain, type, protocol );
      if ( !blocking_mode )
        set_blocking_mode( false );
      continue; // Try to connect again
    }
    else
      return false;
  }
}
int Socket::create( int& domain, int type, int protocol )
{
#ifdef _WIN32
  SOCKET _socket = ::socket( domain, type, protocol );
  if ( _socket != INVALID_SOCKET )
  {
    if ( domain == AF_INET6 )
    {
      DWORD ipv6only = 0; // Allow dual-mode sockets
      setsockopt( _socket, IPPROTO_IPV6, IPV6_V6ONLY, ( char* )&ipv6only, sizeof( ipv6only ) );
    }
    return ( int )_socket;
  }
  else if ( domain == AF_INET6 && ::WSAGetLastError() == WSAEAFNOSUPPORT )
  {
    domain = AF_INET;
    return ( int )::socket( AF_INET, type, protocol );
  }
  else
    return false;
#else
  int _socket = ::socket( AF_INET6, type, protocol );
  if ( _socket != -1 )
    return _socket;
  else if ( domain == AF_INET6 && errno == EAFNOSUPPORT )
  {
    domain = AF_INET;
    return ::socket( AF_INET, type, protocol );
  }
  else
    return -1;
#endif
}
bool Socket::get_blocking_mode() const
{
  return blocking_mode;
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
Socket::operator int() const
{
  return _socket;
}
ssize_t Socket::read( auto_Object<Buffer> buffer )
{
  ssize_t read_ret = read( static_cast<void*>( *buffer ), buffer->capacity() - buffer->size() );
  if ( read_ret > 0 )
    buffer->put( static_cast<void*>( *buffer ), read_ret );
  return read_ret;
}
ssize_t Socket::read( void* buffer, size_t buffer_len )
{
  if ( read_buffer != NULL )
  {
    if ( buffer_len > read_buffer->size() )
      buffer_len = read_buffer->size();
    read_buffer->get( buffer, buffer_len );
    if ( read_buffer->size() == 0 )
      read_buffer = NULL;
    return buffer_len;
  }
#if defined(_WIN32)
  return ::recv( _socket, static_cast<char*>( buffer ), static_cast<int>( buffer_len ), 0 ); // No real advantage to WSARecv on Win32 for one buffer
#elif defined(__linux)
  return ::recv( _socket, buffer, buffer_len, MSG_NOSIGNAL );
#else
  return ::recv( _socket, buffer, buffer_len, 0 );
#endif
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
bool Socket::shutdown()
{
  return true;
}
bool Socket::want_read() const
{
#ifdef _WIN32
  switch ( ::WSAGetLastError() )
  {
    case WSAEWOULDBLOCK:
    case WSA_IO_PENDING: return true;
    default: return false;
  }
#else
  return errno == EWOULDBLOCK;
#endif
}
bool Socket::want_write() const
{
#ifdef _WIN32
  switch ( ::WSAGetLastError() )
  {
    case WSAEINPROGRESS:
    case WSAEWOULDBLOCK:
    case WSA_IO_PENDING: return true;
    default: return false;
  }
#else
  switch ( errno )
  {
    case EINPROGRESS:
    case EWOULDBLOCK: return true;
    default: return false;
  }
#endif
}
ssize_t Socket::write( auto_Object<Buffer> buffer )
{
  std::vector<struct iovec> iovecs;
  buffer->as_iovecs( iovecs );
  return writev( &iovecs[0], iovecs.size() );
}
ssize_t Socket::write( const void* buffer, size_t buffer_len )
{
#ifdef _WIN32
  struct iovec buffers[1];
  buffers[0].iov_base = const_cast<void*>( buffer );
  buffers[0].iov_len = buffer_len;
  return writev( buffers, 1 );
#else
#ifdef __linux
  return ::send( _socket, buffer, buffer_len, MSG_NOSIGNAL );
#else
  return ::send( _socket, buffer, buffer_len, 0 );
#endif
#endif
}
ssize_t Socket::writev( const struct iovec* buffers, uint32_t buffers_count )
{
#ifdef _WIN32
  DWORD dwWrittenLength;
  ssize_t write_ret = ::WSASend( _socket, reinterpret_cast<WSABUF*>( const_cast<struct iovec*>( buffers ) ), buffers_count, &dwWrittenLength, 0, NULL, NULL );
  if ( write_ret >= 0 )
    return static_cast<ssize_t>( dwWrittenLength );
  else
    return write_ret;
#else
  // Use sendmsg instead of writev to pass flags on Linux
  struct msghdr _msghdr;
  memset( &_msghdr, 0, sizeof( _msghdr ) );
  _msghdr.msg_iov = const_cast<iovec*>( buffers );
  _msghdr.msg_iovlen = buffers_count;
#ifdef __linux
  return ::sendmsg( _socket, &_msghdr, MSG_NOSIGNAL ); // MSG_NOSIGNAL = disable SIGPIPE
#else
  return ::sendmsg( _socket, &_msghdr, 0 );
#endif
#endif
}


// socket_address.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#else
#include <arpa/inet.h>
#include <netdb.h>
#include <sys/socket.h>
#endif
SocketAddress::SocketAddress( struct addrinfo& addrinfo_list )
  : addrinfo_list( &addrinfo_list ), _sockaddr_storage( NULL )
{ }
SocketAddress::SocketAddress( const struct sockaddr_storage& _sockaddr_storage )
{
  addrinfo_list = NULL;
  this->_sockaddr_storage = new struct sockaddr_storage;
  memcpy_s( this->_sockaddr_storage, sizeof( *this->_sockaddr_storage ), &_sockaddr_storage, sizeof( _sockaddr_storage ) );
}
auto_Object<SocketAddress> SocketAddress::create( const URI& uri )
{
  if ( uri.get_host() == "*" )
    return create( NULL, uri.get_port() );
  else
    return create( uri.get_host().c_str(), uri.get_port() );
}
auto_Object<SocketAddress> SocketAddress::create( const char* hostname, uint16_t port )
{
  struct addrinfo* addrinfo_list = getaddrinfo( hostname, port );
  if ( addrinfo_list != NULL )
    return new SocketAddress( *addrinfo_list );
  else
    return NULL;
}
SocketAddress::~SocketAddress()
{
  if ( addrinfo_list != NULL )
    freeaddrinfo( addrinfo_list );
  else if ( _sockaddr_storage != NULL )
    delete _sockaddr_storage;
}
bool SocketAddress::as_struct_sockaddr( int family, struct sockaddr*& out_sockaddr, socklen_t& out_sockaddrlen )
{
  if ( addrinfo_list != NULL )
  {
    struct addrinfo* addrinfo_p = addrinfo_list;
    while ( addrinfo_p != NULL )
    {
      if ( addrinfo_p->ai_family == family )
      {
        out_sockaddr = addrinfo_p->ai_addr;
        out_sockaddrlen = addrinfo_p->ai_addrlen;
        return true;
      }
      else
        addrinfo_p = addrinfo_p->ai_next;
    }
  }
  else if ( _sockaddr_storage->ss_family == family )
  {
    out_sockaddr = reinterpret_cast<struct sockaddr*>( _sockaddr_storage );
    out_sockaddrlen = sizeof( *_sockaddr_storage );
    return true;
  }
#ifdef _WIN32
  ::WSASetLastError( WSAEAFNOSUPPORT );
#else
  errno = EAFNOSUPPORT;
#endif
  return false;
}
struct addrinfo* SocketAddress::getaddrinfo( const char* hostname, uint16_t port )
{
  std::ostringstream servname; // ltoa is not very portable
  servname << port; // servname = decimal port or service name. Great interface, guys.
  struct addrinfo addrinfo_hints;
  memset( &addrinfo_hints, 0, sizeof( addrinfo_hints ) );
  addrinfo_hints.ai_family = AF_UNSPEC;
  if ( hostname == NULL )
    addrinfo_hints.ai_flags |= AI_PASSIVE; // To get INADDR_ANYs
  struct addrinfo* addrinfo_list;
  int getaddrinfo_ret = ::getaddrinfo( hostname, servname.str().c_str(), &addrinfo_hints, &addrinfo_list );
  if ( getaddrinfo_ret == 0 )
    return addrinfo_list;
  else
    return NULL;
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
  if ( addrinfo_list != NULL )
  {
    struct addrinfo* addrinfo_p = addrinfo_list;
    while ( addrinfo_p != NULL )
    {
      if ( ::getnameinfo( addrinfo_p->ai_addr, addrinfo_p->ai_addrlen, out_hostname, out_hostname_len, NULL, 0, numeric ? NI_NUMERICHOST : 0 ) == 0 )
        return true;
      else
        addrinfo_p = addrinfo_p->ai_next;
    }
    return false;
  }
  else
    return ::getnameinfo( reinterpret_cast<sockaddr*>( _sockaddr_storage ), static_cast<socklen_t>( sizeof( *_sockaddr_storage ) ), out_hostname, out_hostname_len, NULL, 0, numeric ? NI_NUMERICHOST : 0 ) == 0;
}
uint16_t SocketAddress::get_port() const
{
  if ( addrinfo_list != NULL )
  {
    switch ( addrinfo_list->ai_family )
    {
      case AF_INET: return ntohs( reinterpret_cast<struct sockaddr_in*>( addrinfo_list->ai_addr )->sin_port );
      case AF_INET6: return ntohs( reinterpret_cast<struct sockaddr_in6*>( addrinfo_list->ai_addr )->sin6_port );
      default: DebugBreak(); return 0;
    }
  }
  else
  {
    switch ( _sockaddr_storage->ss_family )
    {
      case AF_INET: return ntohs( reinterpret_cast<struct sockaddr_in*>( _sockaddr_storage )->sin_port );
      case AF_INET6: return ntohs( reinterpret_cast<struct sockaddr_in6*>( _sockaddr_storage )->sin6_port );
      default: DebugBreak(); return 0;
    }
  }
}
bool SocketAddress::operator==( const SocketAddress& other ) const
{
  if ( addrinfo_list != NULL )
  {
    if ( other.addrinfo_list != NULL )
    {
      struct addrinfo* addrinfo_p = addrinfo_list;
      while ( addrinfo_p != NULL )
      {
        struct addrinfo* other_addrinfo_p = other.addrinfo_list;
        while ( other_addrinfo_p != NULL )
        {
          if ( addrinfo_p->ai_addrlen == other_addrinfo_p->ai_addrlen &&
               memcmp( addrinfo_p->ai_addr, other_addrinfo_p->ai_addr, addrinfo_p->ai_addrlen ) == 0 &&
               addrinfo_p->ai_family == other_addrinfo_p->ai_family &&
               addrinfo_p->ai_protocol == other_addrinfo_p->ai_protocol &&
               addrinfo_p->ai_socktype == other_addrinfo_p->ai_socktype
             )
               break;
          else
            other_addrinfo_p = other_addrinfo_p->ai_next;
        }
        if ( other_addrinfo_p != NULL ) // i.e. we found the addrinfo in the other's list
          addrinfo_p = addrinfo_p->ai_next;
        else
          return false;
      }
      return true;
    }
    else
      return false;
  }
  else if ( other._sockaddr_storage != NULL )
    return memcmp( _sockaddr_storage, other._sockaddr_storage, sizeof( *_sockaddr_storage ) ) == 0;
  else
    return false;
}


// ssl_context.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
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
#ifdef YIELD_HAVE_OPENSSL
SSLContext::SSLContext( SSL_METHOD* method )
{
  ctx = createSSL_CTX( method );
}
SSLContext::SSLContext( SSL_METHOD* method, const Path& pem_certificate_file_path, const Path& pem_private_key_file_path, const std::string& pem_private_key_passphrase )
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
SSLContext::SSLContext( SSL_METHOD* method, const std::string& pem_certificate_str, const std::string& pem_private_key_str, const std::string& pem_private_key_passphrase )
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
SSLContext::SSLContext( SSL_METHOD* method, const Path& pkcs12_file_path, const std::string& pkcs12_passphrase )
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
#else
SSLContext::SSLContext()
{ }
#endif
SSLContext::~SSLContext()
{
#ifdef YIELD_HAVE_OPENSSL
  SSL_CTX_free( ctx );
#endif
}
#ifdef YIELD_HAVE_OPENSSL
SSL_CTX* SSLContext::createSSL_CTX( SSL_METHOD* method )
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
int SSLContext::pem_password_callback( char *buf, int size, int, void *userdata )
{
  SSLContext* this_ = static_cast<SSLContext*>( userdata );
  if ( size > static_cast<int>( this_->pem_private_key_passphrase.size() ) )
    size = static_cast<int>( this_->pem_private_key_passphrase.size() );
  memcpy_s( buf, size, this_->pem_private_key_passphrase.c_str(), size );
  return size;
}
void SSLContext::throwOpenSSLException()
{
  SSL_load_error_strings();
  throw Exception( ERR_error_string( ERR_get_error(), NULL ) );
}
#endif


// ssl_listen_queue.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef YIELD_HAVE_OPENSSL
auto_Object<SSLListenQueue> SSLListenQueue::create( auto_Object<SocketAddress> sockname, auto_Object<SSLContext> ssl_context )
{
  auto_Object<SSLSocket> listen_ssl_socket = SSLSocket::create( ssl_context );
  if ( listen_ssl_socket != NULL &&
       listen_ssl_socket->bind( sockname ) &&
       listen_ssl_socket->listen() )
    return new SSLListenQueue( listen_ssl_socket );
  else
    return NULL;
}
SSLListenQueue::SSLListenQueue( auto_Object<SSLSocket> listen_ssl_socket )
  : TCPListenQueue( listen_ssl_socket.release() )
{ }
#endif


// ssl_socket.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef YIELD_HAVE_OPENSSL
#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#else
#include <netinet/in.h> // For the IPPROTO_* constants
#endif
auto_Object<SSLSocket> SSLSocket::create( auto_Object<SSLContext> ctx )
{
  SSL* ssl = SSL_new( ctx->get_ssl_ctx() );
  if ( ssl != NULL )
  {
    int domain = AF_INET6;
    int _socket = Socket::create( domain, SOCK_STREAM, IPPROTO_TCP );
    if ( _socket != -1 )
      return new SSLSocket( domain, _socket, ctx, *ssl );
  }
  return NULL;
}
SSLSocket::SSLSocket( int domain, int _socket, auto_Object<SSLContext> ctx, SSL& ssl )
  : TCPSocket( domain, _socket ), ctx( ctx ), ssl( &ssl )
{ }
SSLSocket::~SSLSocket()
{
  SSL_free( ssl );
}
auto_Object<TCPSocket> SSLSocket::accept()
{
  SSL_set_fd( ssl, *this );
  int peer_socket = static_cast<int>( TCPSocket::_accept() );
  if ( peer_socket != -1 )
  {
    SSL* peer_ssl = SSL_new( ctx->get_ssl_ctx() );
    SSL_set_fd( peer_ssl, peer_socket );
    SSL_set_accept_state( peer_ssl );
    return new SSLSocket( get_domain(), peer_socket, ctx, *peer_ssl );
  }
  else
    return NULL;
}
bool SSLSocket::connect( auto_Object<SocketAddress> peer_sockaddr )
{
  if ( TCPSocket::connect( peer_sockaddr ) )
  {
    SSL_set_fd( ssl, *this );
    SSL_set_connect_state( ssl );
    return true;
  }
  else
    return false;
}
/*
void SSLSocket::info_callback( const SSL* ssl, int where, int ret )
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
*/
ssize_t SSLSocket::read( void* buffer, size_t buffer_len )
{
  return SSL_read( ssl, buffer, static_cast<int>( buffer_len ) );
}
bool SSLSocket::shutdown()
{
  if ( SSL_shutdown( ssl ) != -1 )
    return TCPSocket::shutdown();
  else
    return false;
}
bool SSLSocket::want_read() const
{
  return SSL_want_read( ssl ) == 1;
}
bool SSLSocket::want_write() const
{
  return SSL_want_write( ssl ) == 1;
}
ssize_t SSLSocket::write( const void* buffer, size_t buffer_len )
{
  return SSL_write( ssl, buffer, static_cast<int>( buffer_len ) );
}
ssize_t SSLSocket::writev( const struct iovec* buffers, uint32_t buffers_count )
{
  if ( buffers_count == 1 )
    return write( buffers[0].iov_base, buffers[0].iov_len );
  else
  {
    std::string buffer;
    for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
      buffer.append( static_cast<const char*>( buffers[buffer_i].iov_base ), buffers[buffer_i].iov_len );
    return write( buffer.c_str(), buffer.size() );
  }
}
#endif


// tcp_listen_queue.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if !defined(_WIN32) && defined(_DEBUG)
#include <errno.h>
#endif
auto_Object<TCPListenQueue> TCPListenQueue::create( auto_Object<SocketAddress> sockname )
{
  auto_Object<TCPSocket> listen_tcp_socket = TCPSocket::create();
  if ( listen_tcp_socket != NULL &&
       listen_tcp_socket->bind( sockname ) &&
       listen_tcp_socket->listen() )
    return new TCPListenQueue( listen_tcp_socket );
  else
    return NULL;
}
TCPListenQueue::TCPListenQueue( auto_Object<TCPSocket> listen_tcp_socket )
  : listen_tcp_socket( listen_tcp_socket )
{
  attach( *listen_tcp_socket, listen_tcp_socket->incRef() );
}
Event* TCPListenQueue::dequeue( uint64_t timeout_ns )
{
  Event* ev = FDEventQueue::dequeue( timeout_ns );
  if ( ev != NULL )
  {
    switch ( ev->get_tag() )
    {
      case YIELD_OBJECT_TAG( FDEventQueue::POLLINEvent ):
      {
        FDEventQueue::POLLINEvent* pollin_event = static_cast<FDEventQueue::POLLINEvent*>( ev );
        if ( pollin_event->get_context() == static_cast<Object*>( listen_tcp_socket.get() ) )
        {
          auto_Object<TCPSocket> accepted_tcp_socket = listen_tcp_socket->accept();
#ifdef YIELD_HAVE_SOLARIS_EVENT_PORTS
        // The event port automatically dissociates events, so we have to re-associate here
        toggle( *listen_tcp_socket, NULL, true, false );
#endif
          Object::decRef( *ev );
          return accepted_tcp_socket.release();
        }
      }
      break;
    }
  }
  return ev;
}


// tcp_socket.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if defined(_WIN32)
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#include <mswsock.h>
#pragma warning( pop )
#else
#include <netinet/in.h> // For the IPPROTO_* constants
#include <netinet/tcp.h> // For the TCP_* constants
#include <sys/socket.h>
#endif
#include <cstring>
#ifdef _WIN32
void* TCPSocket::lpfnAcceptEx = NULL;
void* TCPSocket::lpfnConnectEx = NULL;
#endif
TCPSocket::TCPSocket( int domain, int _socket )
  : Socket( domain, SOCK_STREAM, IPPROTO_TCP, _socket )
{
  partial_write_len = 0;
}
auto_Object<TCPSocket> TCPSocket::accept()
{
#ifdef _WIN32
  unsigned int peer_socket = _accept();
  if ( peer_socket != INVALID_SOCKET )
#else
  int peer_socket = _accept();
  if ( peer_socket != -1 )
#endif
    return new TCPSocket( get_domain(), peer_socket );
  else
    return NULL;
}
int TCPSocket::_accept()
{
  sockaddr_storage peer_sockaddr_storage;
  socklen_t peer_sockaddr_storage_len = sizeof( peer_sockaddr_storage );
  return ::accept( *this, ( struct sockaddr* )&peer_sockaddr_storage, &peer_sockaddr_storage_len );
}
#ifdef _WIN32
bool TCPSocket::aio_accept( auto_Object<AIOAcceptControlBlock> aio_accept_control_block )
{
  if ( TCPSocket::lpfnAcceptEx == NULL )
  {
    GUID GuidAcceptEx = WSAID_ACCEPTEX;
    LPFN_ACCEPTEX lpfnAcceptEx;
    DWORD dwBytes;
    WSAIoctl( *this, SIO_GET_EXTENSION_FUNCTION_POINTER, &GuidAcceptEx, sizeof( GuidAcceptEx ), &lpfnAcceptEx, sizeof( lpfnAcceptEx ), &dwBytes, NULL, NULL );
    TCPSocket::lpfnAcceptEx = lpfnAcceptEx;
  }
  auto_Object<TCPSocket> accepted_tcp_socket = aio_accept_control_block->get_accepted_tcp_socket();
  auto_Object<Buffer> read_buffer = aio_accept_control_block->get_read_buffer();
  size_t sizeof_sockaddr = ( get_domain() == AF_INET6 ) ? sizeof( sockaddr_in6 ) : sizeof( sockaddr_in );
  if ( read_buffer->capacity() < ( sizeof_sockaddr + 16 ) * 2 ) DebugBreak();
  DWORD dwBytesReceived;
  if ( static_cast<LPFN_ACCEPTEX>( lpfnAcceptEx )( *this, *accepted_tcp_socket, *read_buffer, 0, sizeof_sockaddr + 16, sizeof_sockaddr + 16, &dwBytesReceived, ( LPOVERLAPPED )*aio_accept_control_block ) )
  {
    aio_accept_control_block->onCompletion( dwBytesReceived );
    aio_accept_control_block.release(); // Keep this reference in the queue
    return true;
  }
  else if ( ::WSAGetLastError() == WSA_IO_PENDING )
  {
    aio_accept_control_block.release();
    return true;
  }
  else
    return false;
}
bool TCPSocket::aio_connect( auto_Object<AIOConnectControlBlock> aio_connect_control_block )
{
  if ( TCPSocket::lpfnConnectEx == NULL )
  {
    GUID GuidConnectEx = WSAID_CONNECTEX;
    LPFN_CONNECTEX lpfnConnectEx;
    DWORD dwBytes;
    WSAIoctl( *this, SIO_GET_EXTENSION_FUNCTION_POINTER, &GuidConnectEx, sizeof( GuidConnectEx ), &lpfnConnectEx, sizeof( lpfnConnectEx ), &dwBytes, NULL, NULL );
    TCPSocket::lpfnConnectEx = lpfnConnectEx;
  }
  if ( bind( SocketAddress::create( NULL, 0 ) ) )
  {
    for ( ;; )
    {
      struct sockaddr* name; socklen_t namelen;
      if ( aio_connect_control_block->get_peername()->as_struct_sockaddr( get_domain(), name, namelen ) )
      {
        PVOID lpSendBuffer;
        DWORD dwSendDataLength;
        auto_Object<Buffer> write_buffer = aio_connect_control_block->get_write_buffer();
        if ( write_buffer != NULL )
        {
          lpSendBuffer = *write_buffer;
          dwSendDataLength = write_buffer->size();
        }
        else
        {
          lpSendBuffer = NULL;
          dwSendDataLength = 0;
        }
        DWORD dwBytesSent;
        if ( static_cast<LPFN_CONNECTEX>( lpfnConnectEx )( *this, name, namelen, lpSendBuffer, dwSendDataLength, &dwBytesSent, *aio_connect_control_block ) )
        {
          aio_connect_control_block->onCompletion( dwBytesSent );
          aio_connect_control_block.release(); // Keep this reference in the queue
          return true;
        }
        else if ( ::WSAGetLastError() == WSA_IO_PENDING )
        {
          aio_connect_control_block.release(); // Keep this reference in the queue
          return false;
        }
        else
          return false;
      }
      else
        return false;
    }
  }
  else
    return false;
}
#endif
auto_Object<TCPSocket> TCPSocket::create()
{
  int domain = AF_INET6;
  int _socket = Socket::create( domain, SOCK_STREAM, IPPROTO_TCP );
  if ( _socket != -1 )
    return new TCPSocket( domain, _socket );
  else
    return NULL;
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
TCPSocket::AIOAcceptControlBlock::AIOAcceptControlBlock( auto_Object<TCPSocket> accepted_tcp_socket, auto_Object<Buffer> read_buffer )
  : accepted_tcp_socket( accepted_tcp_socket ), read_buffer( read_buffer )
{ }
void TCPSocket::AIOAcceptControlBlock::onCompletion( size_t bytes_transferred )
{
  read_buffer->put( NULL, bytes_transferred );
  size_t sizeof_sockaddr = ( accepted_tcp_socket->get_domain() == AF_INET6 ) ? sizeof( sockaddr_in6 ) : sizeof( sockaddr_in );
  read_buffer->get( NULL, ( sizeof_sockaddr + 16 ) * 2 );
}


// tracing_socket.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
TracingSocket::TracingSocket( auto_Object<Socket> underlying_socket, auto_Object<Log> log )
  : Socket( underlying_socket->get_domain(), underlying_socket->get_type(), underlying_socket->get_protocol(), -1 ),
    underlying_socket( underlying_socket ), log( log )
{ }
bool TracingSocket::aio_connect( auto_Object<AIOConnectControlBlock> aio_connect_control_block )
{
  return underlying_socket->aio_connect( aio_connect_control_block );
}
ssize_t TracingSocket::aio_read( auto_Object<AIOReadControlBlock> aio_read_control_block )
{
  return underlying_socket->aio_read( aio_read_control_block );
}
bool TracingSocket::bind( auto_Object<SocketAddress> to_sockaddr )
{
  std::string to_hostname;
  if ( to_sockaddr->getnameinfo( to_hostname ) )
    log->getStream( Log::LOG_INFO ) << "yield::TracingSocket: binding socket #" << ( int )*this << " to " << to_hostname << ".";
  return underlying_socket->bind( to_sockaddr );
}
bool TracingSocket::close()
{
  log->getStream( Log::LOG_INFO ) << "yield::TracingSocket: closing socket #" << ( int )*this << ".";
  return underlying_socket->close();
}
bool TracingSocket::connect( auto_Object<SocketAddress> to_sockaddr )
{
  std::string to_hostname;
  if ( to_sockaddr->getnameinfo( to_hostname ) )
    log->getStream( Log::LOG_INFO ) << "yield::TracingSocket: connecting socket #" << ( int )*this << " to " << to_hostname << ".";
  return underlying_socket->connect( to_sockaddr );
}
bool TracingSocket::get_blocking_mode() const
{
  return underlying_socket->get_blocking_mode();
}
auto_Object<SocketAddress> TracingSocket::getpeername()
{
  return underlying_socket->getpeername();
}
auto_Object<SocketAddress> TracingSocket::getsockname()
{
  return underlying_socket->getsockname();
}
TracingSocket::operator int() const
{
  return underlying_socket->operator int();
}
ssize_t TracingSocket::read( void* buffer, size_t buffer_len )
{
  log->getStream( Log::LOG_DEBUG ) << "yield::TracingSocket: trying to read " << buffer_len << " bytes from socket #" << ( int )*this << ".";
  ssize_t read_ret = underlying_socket->read( buffer, buffer_len );
  if ( read_ret > 0 )
  {
    log->getStream( Log::LOG_INFO ) << "yield::TracingSocket: read " << read_ret << " bytes from socket #" << ( int )*this << ".";
    log->write( buffer, read_ret, Log::LOG_DEBUG );
    log->write( "\n", Log::LOG_DEBUG );
  }
  else if ( read_ret == 0 || ( !underlying_socket->want_read() && !underlying_socket->want_write() ) )
    log->getStream( Log::LOG_DEBUG ) << "yield::TracingSocket: lost connection while trying to read socket #" <<  ( int )*this << ".";
  return read_ret;
}
bool TracingSocket::set_blocking_mode( bool blocking )
{
  log->getStream( Log::LOG_INFO ) << "yield::TracingSocket: setting socket #" << ( int )*this << " to " << ( ( blocking ) ? "blocking mode." : "non-blocking mode." );
  return underlying_socket->set_blocking_mode( blocking );
}
bool TracingSocket::shutdown()
{
  log->getStream( Log::LOG_INFO ) << "yield::TracingSocket: shutting down socket #" << ( int )*this << ".";
  return underlying_socket->shutdown();
}
bool TracingSocket::want_read() const
{
  bool want_read_ret = underlying_socket->want_read();
  if ( want_read_ret )
    log->getStream( Log::LOG_DEBUG ) << "yield::TracingSocket: would block on read on socket #" << ( int )*this << ".";
  return want_read_ret;
}
bool TracingSocket::want_write() const
{
  bool want_write_ret = underlying_socket->want_write();
  if ( want_write_ret )
    log->getStream( Log::LOG_DEBUG ) << "yield::TracingSocket: would block on write on socket #" << ( int )*this << ".";
  return want_write_ret;
}
ssize_t TracingSocket::write( const void* buffer, size_t buffer_len )
{
  log->getStream( Log::LOG_DEBUG ) << "yield::TracingSocket: trying to write " << buffer_len << " bytes to socket #" << ( int )*this << ".";
  ssize_t write_ret = underlying_socket->write( buffer, buffer_len );
  if ( write_ret >= 0 )
  {
    log->getStream( Log::LOG_INFO ) << "yield::TracingSocket: wrote " << write_ret << " bytes to socket #" << ( int )*this << ".";
    log->write( buffer, write_ret, Log::LOG_DEBUG );
    log->write( "\n", Log::LOG_DEBUG );
  }
  else if ( !underlying_socket->want_read() && !underlying_socket->want_write() )
    log->getStream( Log::LOG_DEBUG ) << "yield::TracingSocket: lost connection while trying to write to socket #" <<  ( int )*this << ".";
  return write_ret;
}
ssize_t TracingSocket::writev( const struct iovec* buffers, uint32_t buffers_count )
{
  size_t buffers_len = 0;
  for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
    buffers_len += buffers[buffer_i].iov_len;
  log->getStream( Log::LOG_DEBUG ) << "yield::TracingSocket: trying to write " << buffers_len << " bytes to socket #" << ( int )*this << ".";
  ssize_t writev_ret = underlying_socket->writev( buffers, buffers_count );
  if ( writev_ret >= 0 )
  {
    size_t temp_sendmsg_ret = static_cast<size_t>( writev_ret );
    log->getStream( Log::LOG_INFO ) << "yield::TracingSocket: wrote " << writev_ret << " bytes to socket #" << ( int )*this << ".";
    for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
    {
      if ( buffers[buffer_i].iov_len <= temp_sendmsg_ret )
      {
        log->write( buffers[buffer_i].iov_base, buffers[buffer_i].iov_len, Log::LOG_DEBUG );
        temp_sendmsg_ret -= buffers[buffer_i].iov_len;
      }
      else
      {
        log->write( buffers[buffer_i].iov_base, temp_sendmsg_ret, Log::LOG_DEBUG );
        break;
      }
    }
    log->write( "\n", Log::LOG_DEBUG );
  }
  else if ( !underlying_socket->want_read() && !underlying_socket->want_write() )
    log->getStream( Log::LOG_DEBUG ) << "yield::TracingSocket: lost connection while trying to write to socket #" <<  ( int )*this << ".";
  return writev_ret;
}


// udp_recvfrom_queue.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if defined(_WIN32)
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#else
#include <sys/socket.h>
#endif
auto_Object<UDPRecvFromQueue> UDPRecvFromQueue::create( auto_Object<SocketAddress> sockname )
{
  auto_Object<UDPSocket> udp_socket = UDPSocket::create();
  if ( udp_socket != NULL )
  {
    int so_reuseaddr = 1;
    if ( ::setsockopt( *udp_socket, SOL_SOCKET, SO_REUSEADDR, ( char* )&so_reuseaddr, sizeof( so_reuseaddr ) ) != -1 &&
         udp_socket->bind( sockname ) )
      return new UDPRecvFromQueue( sockname, udp_socket );
  }
  return NULL;
}
UDPRecvFromQueue::UDPRecvFromQueue( auto_Object<SocketAddress> recvfrom_sockname, auto_Object<UDPSocket> recvfrom_udp_socket )
  : recvfrom_sockname( recvfrom_sockname ), recvfrom_udp_socket( recvfrom_udp_socket )
{
  this->attach( *recvfrom_udp_socket, NULL );
}
Event* UDPRecvFromQueue::dequeue( uint64_t timeout_ns )
{
  Event* ev = FDEventQueue::dequeue( timeout_ns );
  if ( ev != NULL )
  {
    if ( ev->get_tag() == YIELD_OBJECT_TAG( FDEventQueue::POLLINEvent ) )
    {
#ifdef YIELD_HAVE_SOLARIS_EVENT_PORTS
      // The event port automatically dissociates events, so we have to re-associate here
      toggle( *recvfrom_udp_socket, NULL, true, false );
#endif
      Object::decRef( ev );
      auto_Object<Buffer> recvfrom_buffer = new HeapBuffer( 1024 );
      struct sockaddr_storage recvfrom_peername;
      socklen_t recvfrom_peername_len = sizeof( recvfrom_peername );
      ssize_t recvfrom_ret = ::recvfrom( *recvfrom_udp_socket, *recvfrom_buffer, recvfrom_buffer->capacity(), 0, reinterpret_cast<struct sockaddr*>( &recvfrom_peername ), &recvfrom_peername_len );
      if ( recvfrom_ret > 0 )
      {
        auto_Object<UDPSocket> udp_socket = UDPSocket::create();
        if ( udp_socket != NULL )
        {
          int so_reuseaddr = 1;
          if ( ::setsockopt( *udp_socket, SOL_SOCKET, SO_REUSEADDR, ( char* )&so_reuseaddr, sizeof( so_reuseaddr ) ) != -1 &&
               udp_socket->bind( recvfrom_sockname ) && // Need to bind to the address we're doing the recvfrom on so that response sends also come from that address (= allows UDP clients to filter all but the server's responses)
               udp_socket->connect( new SocketAddress( recvfrom_peername ) ) )
          {
            recvfrom_buffer->put( NULL, recvfrom_ret );
            udp_socket->set_read_buffer( recvfrom_buffer );
            return udp_socket.release();
          }
        }
      }
    }
  }
  return ev;
}


// udp_socket.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if defined(_WIN32)
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#else
#include <netinet/in.h> // For the IPPROTO_* constants
#include <sys/socket.h>
#endif
auto_Object<UDPSocket> UDPSocket::create()
{
  int domain = AF_INET6;
  int _socket = Socket::create( domain, SOCK_DGRAM, IPPROTO_UDP );
  if ( _socket != -1 )
    return new UDPSocket( domain, _socket );
  else
    return NULL;
}
UDPSocket::UDPSocket( int domain, int _socket )
  : Socket( domain, SOCK_DGRAM, IPPROTO_UDP, _socket )
{ }


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
URI::operator std::string() const
{
  std::ostringstream oss;
  oss << *this;
  return oss.str();
}

