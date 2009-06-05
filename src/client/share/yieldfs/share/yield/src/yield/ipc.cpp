// Revision: 1512

#include "yield/ipc.h"
using namespace YIELD;
using std::memset;


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
  read_end->recv( &m, sizeof( m ) );
#endif
}
int EventFDPipe::get_read_end() const
{
  return *read_end;
}
int EventFDPipe::get_write_end() const
{
  return *write_end;
}
void EventFDPipe::signal()
{
#ifdef YIELD_HAVE_LINUX_EVENTFD
  uint64_t m = 1;
  ::write( fd, reinterpret_cast<char*>( &m ), sizeof( m ) );
#else
  char m = 1;
  write_end->send( &m, sizeof( m ) );
#endif
}


// fd_and_internal_event_queue.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
FDAndInternalEventQueue::FDAndInternalEventQueue()
{
  dequeue_blocked = true;
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
//    dequeue_blocked = false;
    return ev;
  }
  ev = FDEventQueue::dequeue();
//  dequeue_blocked = false;
  if ( ev != NULL )
    return ev;
  return NonBlockingFiniteQueue<Event*, 2048>::try_dequeue();
}
Event* FDAndInternalEventQueue::dequeue( uint64_t timeout_ns )
{
  dequeue_blocked = true;
  Event* ev = NonBlockingFiniteQueue<Event*, 2048>::try_dequeue();
  if ( ev != NULL )
  {
//    dequeue_blocked = false;
    return ev;
  }
  ev = FDEventQueue::dequeue( timeout_ns );
//  dequeue_blocked = false;
  if ( ev )
    return ev;
  return NonBlockingFiniteQueue<Event*, 2048>::try_dequeue();
}
bool FDAndInternalEventQueue::enqueue( Event& ev )
{
  bool result = NonBlockingFiniteQueue<Event*, 2048>::enqueue( &ev );
  if ( dequeue_blocked )
    FDEventQueue::signal();
  else
    DebugBreak();
  return result;
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
  if ( eventfd_pipe != NULL &&
       attach( eventfd_pipe->get_read_end(), eventfd_pipe->incRef() ) )
  {
    std::make_heap( timers.begin(), timers.end(), compareTimerEvents );
  }
  else
    throw Exception();
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
    fd_to_context_map.insert( fd, released_context );
    return true;
#endif
  }
  else
    return toggle( fd, enable_read, enable_write );
}
bool FDEventQueue::compareTimerEvents( const TimerEvent* left, const TimerEvent* right )
{
  return left->get_fire_time() < right->get_fire_time();
}
Event* FDEventQueue::dequeue()
{
  if ( timers.empty() )
  {
    if ( active_fds <= 0 )
    {
      active_fds = poll();
      if ( active_fds <= 0 )
        return NULL;
    }
    return dequeueFDEvent();
  }
  else
    return dequeue( static_cast<uint64_t>( -1 ) );
}
Event* FDEventQueue::dequeue( uint64_t timeout_ns )
{
  TimerEvent* timer_event = dequeueTimerEvent();
  if ( timer_event != NULL )
    return timer_event;
  if ( active_fds <= 0 )
  {
    if ( timers.empty() )
      active_fds = poll( timeout_ns );
    else
    {
      Time current_time;
      const Time& next_fire_time = timers[0]->get_fire_time();
      if ( next_fire_time > current_time )
      {
        timeout_ns = next_fire_time - current_time;
        active_fds = poll( timeout_ns );
      }
      else
        return dequeueTimerEvent();
    }
    if ( active_fds <= 0 )
      return NULL;
  }
  timer_event = dequeueTimerEvent();
  if ( timer_event != NULL )
    return timer_event;
  else
    return dequeueFDEvent();
}
FDEventQueue::FDEvent* FDEventQueue::dequeueFDEvent()
{
#if defined(YIELD_HAVE_LINUX_EPOLL) || defined(YIELD_HAVE_FREEBSD_KQUEUE) || defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
  while ( active_fds > 0 )
  {
    active_fds--;
#if defined(YIELD_HAVE_LINUX_EPOLL)
    if ( returned_events[active_fds].data.ptr != eventfd_pipe.get() )
      return new FDEvent( Object::incRef( static_cast<Object*>( returned_events[active_fds].data.ptr ) ), 0, ( returned_events[active_fds].events & EPOLLIN ) == EPOLLIN );
#elif defined(YIELD_HAVE_FREEBSD_KQUEUE)
    if ( returned_events[active_fds].ident != eventfd_pipe->get_read_end() )
      return new FDEvent( Object::incRef( static_cast<Object*>( returned_events[active_fds].udata ) ), 0, returned_events[active_fds].filter == EVFILT_READ );
#elif defined(YIELD_HAVE_SOLARIS_EVENT_PORTS)
    if ( returned_events[active_fds].portev_source == PORT_SOURCE_FD )
    {
      int fd = returned_events[active_fds].portev_object;
      if ( fd != eventfd_pipe->get_read_end() )
      {
        Object* context = static_cast<Object*>( returned_events[active_fds].portev_user );
        bool want_read = returned_events[active_fds].portev_events & POLLIN ) || ( returned_events[active_fds].portev_events & POLLRDNORM );
        memset( &returned_events[active_fds], 0, sizeof( returned_events[active_fds] ) );
        return new FDEvent( Object::incRef( context ), 0, want_read );
      }
    }
    else
      continue;
#endif
    // The signal was set
    eventfd_pipe->clear();
    return NULL;
  }
#else
#ifdef _WIN32
  while ( active_fds > 0 && next_fd_to_check != fd_to_context_map.end() )
  {
    unsigned int fd = next_fd_to_check->first;
    uint32_t error_code;
    bool want_read;
    if ( FD_ISSET( fd, read_fds_copy ) )
    {
      FD_CLR( fd, read_fds_copy );
      if ( fd == static_cast<unsigned int>( eventfd_pipe->get_read_end() ) )
      {
        eventfd_pipe->clear();
        next_fd_to_check++;
        return NULL;
      }
      else
      {
        error_code = 0;
        want_read = true;
      }
    }
    else if ( FD_ISSET( fd, write_fds_copy ) )
    {
      FD_CLR( fd, write_fds_copy );
      error_code = 0;
      want_read = false;
    }
    else if ( FD_ISSET( fd, except_fds_copy ) )
    {
      FD_CLR( fd, except_fds_copy );
      int so_error; int so_error_len = sizeof( so_error );
      ::getsockopt( fd, SOL_SOCKET, SO_ERROR, reinterpret_cast<char*>( &so_error ), &so_error_len );
      error_code = so_error;
      want_read = false;
    }
    else
    {
      next_fd_to_check++;
      continue;
    }
    active_fds--;
    return new FDEvent( Object::incRef( fd_to_context_map.find( fd ) ), error_code, want_read );
  }
#else
  pollfd_vector::size_type pollfds_size = pollfds.size();
  while ( active_fds > 0 && next_pollfd_to_check < pollfds_size )
  {
    if ( pollfds[next_pollfd_to_check].revents != 0 )
    {
      int fd = pollfds[next_pollfd_to_check].fd;
      bool want_read = pollfds[next_pollfd_to_check].revents & POLLIN;
      pollfds[next_pollfd_to_check].revents = 0;
      next_pollfd_to_check++;
      active_fds--;
      if ( fd == eventfd_pipe->get_read_end() )
      {
        eventfd_pipe->clear();
        return NULL;
      }
      else
        return new FDEvent( Object::incRef( fd_to_context_map.find( fd ) ), 0, want_read );
    }
    else
      next_pollfd_to_check++;
  }
#endif
#endif
  active_fds = 0;
  return NULL;
}
FDEventQueue::TimerEvent* FDEventQueue::dequeueTimerEvent()
{
  if ( !timers.empty() )
  {
    Time current_time;
    if ( timers.back()->get_fire_time() <= current_time )
    {
      TimerEvent* timer_event = timers.back();
      timers.pop_back();
      make_heap( timers.begin(), timers.end(), compareTimerEvents );
      if ( timer_event->get_period() != 0 )
      {
        TimerEvent* next_timer_event = new TimerEvent( timer_event->get_period(), timer_event->get_period(), timer_event->get_context() );
        timers.push_back( next_timer_event );
        std::push_heap( timers.begin(), timers.end(), compareTimerEvents );
      }
      return timer_event;
    }
  }
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
  memcpy_s( read_fds_copy, sizeof( *read_fds_copy ), read_fds, sizeof( *read_fds ) );
  memcpy_s( write_fds_copy, sizeof( *write_fds_copy ), write_fds, sizeof( *write_fds ) );
  memcpy_s( except_fds_copy, sizeof( *except_fds_copy ), except_fds, sizeof( *except_fds ) );
  next_fd_to_check = fd_to_context_map.begin();
  return select( 0, read_fds_copy, write_fds_copy, except_fds_copy, NULL );
#else
  next_pollfd_to_check = 0;
  return ::poll( &pollfds[0], pollfds.size(), -1 );
#endif
}
int FDEventQueue::poll( uint64_t timeout_ns )
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
  memcpy_s( read_fds_copy, sizeof( *read_fds_copy ), read_fds, sizeof( *read_fds ) );
  memcpy_s( write_fds_copy, sizeof( *write_fds_copy ), write_fds, sizeof( *write_fds ) );
  memcpy_s( except_fds_copy, sizeof( *except_fds_copy ), except_fds, sizeof( *except_fds ) );
  next_fd_to_check = fd_to_context_map.begin();
  struct timeval poll_tv = Time( timeout_ns );
  return select( 0, read_fds_copy, write_fds_copy, except_fds_copy, &poll_tv );
#else
  next_pollfd_to_check = 0;
  return ::poll( &pollfds[0], pollfds.size(), static_cast<int>( timeout_ns / NS_IN_MS ) );
#endif
}
auto_Object<FDEventQueue::TimerEvent> FDEventQueue::timer_create( const Time& timeout, const Time& period, auto_Object<> context )
{
  TimerEvent* timer_event = new TimerEvent( timeout, period, context );
  timers.push_back( timer_event );
  std::make_heap( timers.begin(), timers.end(), compareTimerEvents );
  return timer_event->incRef();
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
auto_Object<HTTPClient> HTTPClient::create( const URI& absolute_uri,
                                            auto_Object<StageGroup> stage_group,
                                            auto_Object<Log> log,
                                            const Time& operation_timeout,
                                            uint8_t reconnect_tries_max,
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
    auto_Object<FDAndInternalEventQueue> fd_event_queue = new FDAndInternalEventQueue;
    auto_Object<HTTPClient> http_client = new HTTPClient( absolute_uri, fd_event_queue, log, operation_timeout, peername, reconnect_tries_max, ssl_context );
    stage_group->createStage( http_client->incRef(), 1, fd_event_queue->incRef(), NULL, log );
    return http_client;
  }
  return NULL;
}
HTTPClient::HTTPClient( const URI& absolute_uri, auto_Object<FDAndInternalEventQueue> fd_event_queue, auto_Object<Log> log, const Time& operation_timeout, auto_Object<SocketAddress> peername, uint8_t reconnect_tries_max, auto_Object<SSLContext> ssl_context )
  : SocketClient<HTTPRequest, HTTPResponse>( absolute_uri, fd_event_queue, log, operation_timeout, peername, reconnect_tries_max, ssl_context )
{ }
auto_Object<HTTPRequest> HTTPClient::createProtocolRequest( auto_Object<Request> body )
{
  if ( body->get_tag() == YIELD_OBJECT_TAG( HTTPRequest ) )
    return static_cast<HTTPRequest*>( body.release() );
  else
    return NULL;
}
auto_Object<HTTPResponse> HTTPClient::createProtocolResponse( auto_Object<HTTPRequest> )
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
  auto_Object<File> file = File::open( body_file_path );
  auto_Object<String> body = new String( static_cast<size_t>( file->getattr()->get_size() ) );
  file->read( const_cast<char*>( body->c_str() ), body->size(), NULL );
  return sendHTTPRequest( "PUT", absolute_uri, body.release(), log );
}
void HTTPClient::respond( auto_Object<HTTPRequest> http_request, auto_Object<HTTPResponse> http_response )
{
  http_request->respond( *http_response.release() );
}
void HTTPClient::respond( auto_Object<HTTPRequest> http_request, auto_Object<ExceptionResponse> exception_response )
{
  http_request->respond( *exception_response.release() );
}
auto_Object<HTTPResponse> HTTPClient::sendHTTPRequest( const char* method, const YIELD::URI& absolute_uri, auto_Object<> body, auto_Object<Log> log )
{
  auto_Object<StageGroup> stage_group = new SEDAStageGroup( "HTTPClient", 0, NULL, log );
  auto_Object<HTTPClient> http_client = HTTPClient::create( absolute_uri, stage_group, log, HTTPClient::OPERATION_TIMEOUT_DEFAULT, 3 );
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
HTTPMessage::HTTPMessage( auto_Object<> body )
  : body( body )
{
  http_version = 1;
}
ssize_t HTTPMessage::deserialize( auto_Object<IOBuffer> io_buffer )
{
  switch ( deserialize_state )
  {
    case DESERIALIZING_HEADERS:
    {
      ssize_t RFC822Headers_deserialize_ret = RFC822Headers::deserialize( io_buffer );
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
            body = new String( content_length );
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
      io_buffer->consume( *static_cast<String*>( body.get() ), static_cast<String*>( body.get() )->size() );
      deserialize_state = DESERIALIZE_DONE;
    }
    case DESERIALIZE_DONE: return 0;
    default: DebugBreak(); return -1;
  }
}
void HTTPMessage::serialize( std::ostream& os )
{
  // Finalize headers
  if ( body != NULL &&
       body->get_tag() == YIELD_OBJECT_TAG( String ) &&
       get_header( "Content-Length", NULL ) == NULL )
  {
    char content_length_str[32];
#ifdef _WIN32
    sprintf_s( content_length_str, 32, "%u", static_cast<String*>( body.get() )->size() );
#else
    snprintf( content_length_str, 32, "%zu", static_cast<String*>( body.get() )->size() );
#endif
    set_header( "Content-Length", content_length_str );
  }
  set_iovec( "\r\n", 2 );
  // Serialize headers
  RFC822Headers::serialize( os );
  // Serialize body
  if ( body != NULL && body->get_tag() == YIELD_OBJECT_TAG( String ) )
    os << static_cast<String&>( *body );
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
}
HTTPRequest::HTTPRequest( const char* method, const char* relative_uri, const char* host, auto_Object<> body )
  : HTTPMessage( body )
{
  init( method, relative_uri, host, body );
}
HTTPRequest::HTTPRequest( const char* method, const URI& absolute_uri, auto_Object<> body )
  : HTTPMessage( body )
{
  init( method, absolute_uri.get_resource().c_str(), absolute_uri.get_host().c_str(), body );
}
void HTTPRequest::init( const char* method, const char* relative_uri, const char* host, auto_Object<> body )
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
ssize_t HTTPRequest::deserialize( auto_Object<IOBuffer> io_buffer )
{
  switch ( deserialize_state )
  {
    case DESERIALIZING_METHOD:
    {
      char* method_p = method + strnlen( method, 16 );
      for ( ;; )
      {
        if ( io_buffer->consume( method_p, 1 ) == 1 )
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
        if ( io_buffer->consume( uri_p, 1 ) == 1 )
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
        if ( io_buffer->consume( &test_http_version, 1 ) == 1 )
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
    default: return HTTPMessage::deserialize( io_buffer );
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
void HTTPRequest::serialize( std::ostream& os )
{
  // METHOD URI HTTP/1.1\r\n
  os << method << " ";
  os.write( uri, uri_len );
  os.write( " HTTP/1.1\r\n", 11 );
  // Headers, body
  HTTPMessage::serialize( os );
}


// http_response.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
HTTPResponse::HTTPResponse()
{
  memset( status_code_str, 0, sizeof( status_code_str ) );
  deserialize_state = DESERIALIZING_HTTP_VERSION;
}
HTTPResponse::HTTPResponse( uint16_t status_code )
  : status_code( status_code )
{
  http_version = 1;
  deserialize_state = DESERIALIZE_DONE;
}
HTTPResponse::HTTPResponse( uint16_t status_code, auto_Object<> body )
  : HTTPMessage( body ), status_code( status_code )
{
  deserialize_state = DESERIALIZE_DONE;
}
ssize_t HTTPResponse::deserialize( auto_Object<IOBuffer> io_buffer )
{
  switch ( deserialize_state )
  {
    case DESERIALIZING_HTTP_VERSION:
    {
      for ( ;; )
      {
        uint8_t test_http_version;
        if ( io_buffer->consume( &test_http_version, 1 ) == 1 )
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
        if ( io_buffer->consume( status_code_str_p, 1 ) == 1 )
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
        if ( io_buffer->consume( &c, 1 ) == 1 )
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
    default: return HTTPMessage::deserialize( io_buffer );
  }
}
void HTTPResponse::serialize( std::ostream& os )
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
  os.write( status_line, status_line_len );
  char date[32];
  Time().as_http_date_time( date, 32 );
  set_header( "Date", date );
  HTTPMessage::serialize( os );
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
      auto_Object<HTTPResponseWriter> http_response_writer = new HTTPResponseWriter;
      auto_Object<Stage> http_response_writer_stage = static_cast<StageGroupImpl<StageGroupType>*>( stage_group.get() )->createStage( http_response_writer, log ).release();
      auto_Object<FDAndInternalEventQueue> fd_event_queue = new FDAndInternalEventQueue;
      auto_Object<HTTPRequestReader> http_request_reader = new HTTPRequestReader( fd_event_queue, http_request_target, http_response_writer_stage );
      auto_Object<Stage> http_request_reader_stage = stage_group->createStage( http_request_reader, 1, fd_event_queue, NULL, log );
      http_response_writer->set_protocol_request_reader_stage( http_request_reader_stage );
      auto_Object<HTTPServer> http_server = new HTTPServer( http_request_reader_stage );
      stage_group->createStage( http_server, 1, tcp_listen_queue, NULL, log );
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


// json_marshaller.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
extern "C"
{
  #include <yajl.h>
};
JSONMarshaller::JSONMarshaller( std::ostream& target_ostream, bool write_empty_strings )
: target_ostream( target_ostream ), write_empty_strings( write_empty_strings )
{
  root_decl = NULL;
  writer = yajl_gen_alloc( NULL );
}
JSONMarshaller::JSONMarshaller( std::ostream& target_ostream, bool write_empty_strings, yajl_gen writer, const Declaration& root_decl )
  : target_ostream( target_ostream ), write_empty_strings( write_empty_strings ), root_decl( &root_decl ), writer( writer )
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
  target_ostream.write( reinterpret_cast<const char*>( buffer ), len );
  yajl_gen_clear( writer );
}
void JSONMarshaller::writeBool( const Declaration& decl, bool value )
{
  writeDeclaration( decl );
  yajl_gen_bool( writer, ( int )value );
  flushYAJLBuffer();
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
void JSONMarshaller::writeMap( const Declaration& decl, Object& value )
{
  writeDeclaration( decl );
  JSONMarshaller( target_ostream, write_empty_strings, writer, decl ).writeMap( &value );
}
void JSONMarshaller::writeMap( Object* s )
{
  yajl_gen_map_open( writer );
  in_map = true;
  if ( s )
    s->marshal( *this );
  yajl_gen_map_close( writer );
  flushYAJLBuffer();
}
void JSONMarshaller::writePointer( const Declaration& decl, void* )
{
  writeDeclaration( decl );
  yajl_gen_null( writer );
  flushYAJLBuffer();
}
void JSONMarshaller::writeSequence( const Declaration& decl, Object& value )
{
  writeDeclaration( decl );
  JSONMarshaller( target_ostream, write_empty_strings, writer, decl ).writeSequence( &value );
}
void JSONMarshaller::writeSequence( Object* s )
{
  yajl_gen_array_open( writer );
  in_map = false;
  if ( s )
    s->marshal( *this );
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
void JSONMarshaller::writeStruct( const Declaration& decl, Object& value )
{
  writeDeclaration( decl );
  JSONMarshaller( target_ostream, write_empty_strings, writer, decl ).writeStruct( &value );
}
void JSONMarshaller::writeStruct( Object* s )
{
  writeMap( s );
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
    JSONValue( auto_Object<String> identifier, bool is_map )
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
    auto_Object<String> identifier;
    bool is_map;
    auto_Object<String> as_string;
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
    JSONObject( std::istream& source_istream )
    {
      current_json_value = parent_json_value = NULL;
      reader = yajl_alloc( &JSONObject_yajl_callbacks, NULL, this );
      next_map_key = NULL; next_map_key_len = 0;
      unsigned char read_buffer[4096];
      for ( ;; )
      {
        size_t read_len = source_istream.readsome( reinterpret_cast<char*>( read_buffer ), 4096 );
        if ( source_istream.good() )
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
          throw Exception( "error reading source_istream before parsing complete" );
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
      json_value.as_string = new String( reinterpret_cast<const char*>( buffer ), len );
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
      auto_Object<String> identifier = next_map_key_len != 0 ? new String( next_map_key, next_map_key_len ) : NULL;
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
JSONUnmarshaller::JSONUnmarshaller( std::istream& source_istream )
{
  root_decl = NULL;
  root_json_value = new JSONObject( source_istream );
  next_json_value = root_json_value->child;
}
JSONUnmarshaller::JSONUnmarshaller( const Declaration& root_decl, JSONValue& root_json_value )
  : root_decl( &root_decl ), root_json_value( &root_json_value ),
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
Object* JSONUnmarshaller::readMap( const Declaration& decl, Object* value )
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
void JSONUnmarshaller::readMap( Object& s )
{
  while ( next_json_value )
    s.unmarshal( *this );
}
Object* JSONUnmarshaller::readSequence( const Declaration& decl, Object* value )
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
void JSONUnmarshaller::readSequence( Object& s )
{
  while ( next_json_value )
    s.unmarshal( *this );
}
void JSONUnmarshaller::readString( const Declaration& decl, std::string& str )
{
  JSONValue* json_value = readJSONValue( decl );
  if ( json_value )
  {
    if ( decl.get_identifier() ) // Read the value
    {
      if ( json_value->as_string != NULL )
        str.assign( *json_value->as_string );
    }
    else // Read the identifier
      str.assign( *json_value->identifier );
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


// oncrpc_message.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
template <class ONCRPCMessageType>
ONCRPCMessage<ONCRPCMessageType>::ONCRPCMessage( uint32_t xid, auto_Object<Interface> _interface, auto_Object<> body )
  : xid( xid ), _interface( _interface ), body( body )
{
  record_fragment_length = 0;
}
template <class ONCRPCMessageType>
ONCRPCMessage<ONCRPCMessageType>::~ONCRPCMessage()
{ }
template <class ONCRPCMessageType>
ssize_t ONCRPCMessage<ONCRPCMessageType>::deserialize( auto_Object<IOBuffer> io_buffer )
{
  if ( record_fragment_length == 0 )
  {
    uint32_t record_fragment_marker = 0;
    if ( io_buffer->consume( &record_fragment_marker, sizeof( record_fragment_marker ) ) == sizeof( record_fragment_marker ) )
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
        record_fragment_length = record_fragment_marker ^ ( 1 << 31 );
      }
      else
      {
        last_record_fragment = false;
        record_fragment_length = record_fragment_marker;
      }
      if ( record_fragment_length < 32 * 1024 * 1024 )
        record_fragment.reserve( record_fragment_length );
      else
        DebugBreak();
    }
    else // Could not read 4-byte record fragment marker
    {
      DebugBreak();
      return sizeof( record_fragment_marker );
    }
  }
  io_buffer->consume( record_fragment, record_fragment_length - record_fragment.size() );
  if ( record_fragment.size() == record_fragment_length )
  {
    std::istringstream record_fragment_istringstream( record_fragment );
    XDRUnmarshaller xdr_unmarshaller( record_fragment_istringstream );
    xid = xdr_unmarshaller.readUint32( "xid ");
    if ( record_fragment_istringstream.good( ) )
    {
      static_cast<ONCRPCMessageType*>( this )->deserializeONCRPCRequestResponseHeader( xdr_unmarshaller );
      if ( record_fragment_istringstream.good() )
      {
        if ( body != NULL )
          xdr_unmarshaller.readStruct( XDRUnmarshaller::Declaration(), body.get() );
        else
          return -1;
      }
      else
        return -1;
    }
    return 0;
  }
  else
    return record_fragment_length - record_fragment.size();
}
template <class ONCRPCMessageType>
void ONCRPCMessage<ONCRPCMessageType>::serialize( std::ostream& os )
{
  std::ostringstream xdr_ostringstream;
  XDRMarshaller xdr_marshaller( xdr_ostringstream );
  xdr_marshaller.writeUint32( "record_marker", 0 );
  xdr_marshaller.writeUint32( "xid", get_xid() );
  static_cast<ONCRPCMessageType*>( this )->serializeONCRPCRequestResponseHeader( xdr_marshaller );
  xdr_marshaller.writeStruct( XDRMarshaller::Declaration(), *body );
  std::string xdr_string = xdr_ostringstream.str();
  uint32_t record_marker = static_cast<uint32_t>( xdr_string.size() - sizeof( record_marker ) );
  record_marker |= ( 1 << 31 ); // Indicate that this is the last fragment
#ifdef __MACH__
  record_marker = htonl( record_marker );
#else
  record_marker = Machine::htonl( record_marker );
#endif
  xdr_string.replace( 0, sizeof( record_marker ), ( const char* )&record_marker, sizeof( record_marker ) );
  os << xdr_string;
}
template class ONCRPCMessage<ONCRPCRequest>;
template class ONCRPCMessage<ONCRPCResponse>;


// oncrpc_request.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
ONCRPCRequest::ONCRPCRequest( auto_Object<Interface> _interface )
  : ONCRPCMessage( 0, _interface, NULL )
{ }
ONCRPCRequest::ONCRPCRequest( uint32_t prog, uint32_t proc, uint32_t vers, auto_Object<> body )
  : ONCRPCMessage( static_cast<uint32_t>( Time::getCurrentUnixTimeS() ), NULL, body ),
    prog( prog ), proc( proc ), vers( vers )
{
  credential_auth_flavor = AUTH_NONE;
}
ONCRPCRequest::ONCRPCRequest( uint32_t prog, uint32_t proc, uint32_t vers, uint32_t credential_auth_flavor, auto_Object<> credential, auto_Object<> body )
  : ONCRPCMessage( static_cast<uint32_t>( Time::getCurrentUnixTimeS() ), NULL, body ),
    prog( prog ), proc( proc ), vers( vers ),
    credential_auth_flavor( credential_auth_flavor ), credential( credential )
{ }
void ONCRPCRequest::deserializeONCRPCRequestResponseHeader( XDRUnmarshaller& xdr_unmarshaller )
{
  int32_t msg_type = xdr_unmarshaller.readInt32( "msg_type" );
  if ( msg_type == 0 ) // CALL
  {
    uint32_t rpcvers = xdr_unmarshaller.readUint32( "rpcvers" );
    if ( rpcvers == 2 )
    {
      xdr_unmarshaller.readUint32( "prog" );
      xdr_unmarshaller.readUint32( "vers" );
      uint32_t proc = xdr_unmarshaller.readUint32( "proc" );
      xdr_unmarshaller.readUint32( "credential_auth_flavor" );
      uint32_t credential_auth_body_length = xdr_unmarshaller.readUint32( "credential_auth_body_length" );
      if ( credential_auth_body_length > 0 )
        DebugBreak();
      xdr_unmarshaller.readUint32( "verf_auth_flavor" );
      uint32_t verf_auth_body_length = xdr_unmarshaller.readUint32( "credential_auth_body_length" );
      if ( verf_auth_body_length > 0 )
        DebugBreak();
      body = _interface->createRequest( proc ).release();
    }
  }
}
void ONCRPCRequest::serializeONCRPCRequestResponseHeader( XDRMarshaller& xdr_marshaller )
{
  xdr_marshaller.writeUint32( "msg_type", 0 ); // MSG_CALL
  xdr_marshaller.writeUint32( "rpcvers", 2 );
  xdr_marshaller.writeUint32( "prog", prog );
  xdr_marshaller.writeUint32( "vers", vers );
  xdr_marshaller.writeUint32( "proc", proc );
  xdr_marshaller.writeUint32( "credential_auth_flavor", credential_auth_flavor );
  if ( credential_auth_flavor == AUTH_NONE || credential == NULL )
    xdr_marshaller.writeUint32( "credential_auth_body_length", 0 );
  else
  {
    std::ostringstream credential_auth_body_ostringstream;
    XDRMarshaller credential_auth_body_xdr_marshaller( credential_auth_body_ostringstream );
    credential->marshal( credential_auth_body_xdr_marshaller );
    static_cast<Marshaller&>( xdr_marshaller ).writeString( "credential_auth_body", credential_auth_body_ostringstream.str() );
  }
  xdr_marshaller.writeUint32( "verf_auth_flavor", AUTH_NONE );
  xdr_marshaller.writeUint32( "verf_auth_body_length", 0 );
}


// oncrpc_response.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
ONCRPCResponse::ONCRPCResponse( auto_Object<Interface> _interface, auto_Object<> body )
  : ONCRPCMessage( 0, _interface, body )
{ }
ONCRPCResponse::ONCRPCResponse( uint32_t xid, auto_Object<> body )
  : ONCRPCMessage( xid, NULL, body )
{ }
void ONCRPCResponse::deserializeONCRPCRequestResponseHeader( XDRUnmarshaller& xdr_unmarshaller )
{
  int32_t msg_type = xdr_unmarshaller.readInt32( "msg_type" );
  if ( msg_type == 1 ) // REPLY
  {
    uint32_t reply_stat = xdr_unmarshaller.readUint32( "reply_stat" );
    if ( reply_stat == 0 ) // MSG_ACCEPTED
    {
      uint32_t verf_auth_flavor = xdr_unmarshaller.readUint32( "verf_auth_flavor" );
      uint32_t verf_auth_body_length = xdr_unmarshaller.readUint32( "verf_auth_body_length" );
      if ( verf_auth_flavor == 0 && verf_auth_body_length == 0 )
      {
        uint32_t accept_stat = xdr_unmarshaller.readUint32( "accept_stat" );
        if ( accept_stat != 0 ) // != SUCCESS
        {
          switch ( accept_stat )
          {
            case 1: body = new ExceptionResponse( "ONC-RPC exception: program unavailable" ); break;
            case 2: body = new ExceptionResponse( "ONC-RPC exception: program mismatch" ); break;
            case 3: body = new ExceptionResponse( "ONC-RPC exception: procedure unavailable" ); break;
            case 4: body = new ExceptionResponse( "ONC-RPC exception: garbage arguments" ); break;
            case 5: body = new ExceptionResponse( "ONC-RPC exception: system error" ); break;
            default:
            {
              if ( _interface != NULL )
              {
                body = _interface->createExceptionResponse( accept_stat ).release();
                if ( body == NULL )
                  body = new ExceptionResponse( "ONC-RPC exception: system error" );
              }
              else
                body = new ExceptionResponse( "ONC-RPC exception: system error" );
            }
            break;
          }
        }
      }
      else
        body = new ExceptionResponse( "ONC-RPC exception: received unexpected verification body on response" );
    }
    else
    {
      if ( reply_stat == 1 ) // MSG_REJECTED
        body = new ExceptionResponse( "ONC-RPC exception: received MSG_REJECTED reply_stat" );
      else
        body = new ExceptionResponse( "ONC-RPC exception: received unknown reply_stat" );
    }
  }
  else
    body = new ExceptionResponse( "ONC-RPC exception: received unknown msg_type" );
}
void ONCRPCResponse::serializeONCRPCRequestResponseHeader( XDRMarshaller& xdr_marshaller )
{
  xdr_marshaller.writeUint32( "msg_type", 1 ); // MSG_REPLY
  xdr_marshaller.writeUint32( "reply_stat", 0 ); // MSG_ACCEPTED
  xdr_marshaller.writeUint32( "verf_auth_flavor", 0 );
  xdr_marshaller.writeUint32( "verf_auth_body_length", 0 );
  if ( body->get_tag() != YIELD_OBJECT_TAG( ExceptionResponse ) )
    xdr_marshaller.writeUint32( "accept_stat", 0 ); // SUCCESS
  else
    xdr_marshaller.writeUint32( "accept_stat", 5 ); // SYSTEM_ERR
}


// oncrpc_server.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
auto_Object<ONCRPCServer> ONCRPCServer::create( const URI& absolute_uri,
                                                auto_Object<Interface> _interface,
                                                auto_Object<StageGroup> stage_group,
                                                auto_Object<Log> log,
                                                auto_Object<SSLContext> ssl_context )
{
  auto_Object<SocketAddress> sockname = SocketAddress::create( absolute_uri );
  if ( sockname != NULL )
  {
    if ( absolute_uri.get_scheme() == "oncrpcu" )
    {
      auto_Object<UDPRecvFromQueue> udp_recvfrom_queue = UDPRecvFromQueue::create( sockname );
      if ( udp_recvfrom_queue != NULL )
      {
        auto_Object<ONCRPCResponseWriter> oncrpc_response_writer = new ONCRPCResponseWriter;
        auto_Object<Stage> oncrpc_response_writer_stage = stage_group->createStage( oncrpc_response_writer->incRef(), 1, NULL, NULL, log );
        auto_Object<FDAndInternalEventQueue> fd_event_queue = new FDAndInternalEventQueue;
        auto_Object<Stage> oncrpc_request_reader_stage = stage_group->createStage( new ONCRPCRequestReader( fd_event_queue->incRef(), _interface, log, oncrpc_response_writer_stage ), 1, fd_event_queue->incRef(), NULL, log );
        oncrpc_response_writer->set_protocol_request_reader_stage( oncrpc_request_reader_stage );
        auto_Object<ONCRPCServer> oncrpc_server = new ONCRPCServer( oncrpc_request_reader_stage );
        stage_group->createStage( oncrpc_server->incRef(), 1, udp_recvfrom_queue.release(), NULL, log );
        return oncrpc_server;
      }
    }
    else
    {
      auto_Object<TCPListenQueue> tcp_listen_queue;
#ifdef YIELD_HAVE_OPENSSL
      if ( absolute_uri.get_scheme() == "oncrpcs" && ssl_context != NULL )
        tcp_listen_queue = SSLListenQueue::create( sockname, ssl_context ).release();
      else
#endif
        tcp_listen_queue = TCPListenQueue::create( sockname );
      if ( tcp_listen_queue != NULL )
      {
        auto_Object<ONCRPCResponseWriter> oncrpc_response_writer = new ONCRPCResponseWriter;
        auto_Object<Stage> oncrpc_response_writer_stage = stage_group->createStage( oncrpc_response_writer->incRef(), 1, NULL, NULL, log );
        auto_Object<FDAndInternalEventQueue> fd_event_queue = new FDAndInternalEventQueue;
        auto_Object<Stage> oncrpc_request_reader_stage = stage_group->createStage( new ONCRPCRequestReader( fd_event_queue->incRef(), _interface, log, oncrpc_response_writer_stage ), 1, fd_event_queue->incRef(), NULL, log );
        oncrpc_response_writer->set_protocol_request_reader_stage( oncrpc_request_reader_stage );
        auto_Object<ONCRPCServer> oncrpc_server = new ONCRPCServer( oncrpc_request_reader_stage );
        stage_group->createStage( oncrpc_server->incRef(), 1, tcp_listen_queue.release(), NULL, log );
        return oncrpc_server;
      }
    }
  }
  return NULL;
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
ssize_t RFC822Headers::deserialize( auto_Object<IOBuffer> io_buffer )
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
          if ( io_buffer->consume( &c, 1 ) == 1 )
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
        if ( io_buffer->consume( &c, 1 ) == 1 )
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
                if ( io_buffer->consume( buffer_p, 1 ) )
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
          if ( io_buffer->consume( &c, 1 ) == 1 )
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
          if ( io_buffer->consume( buffer_p, 1 ) == 1 )
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
          if ( io_buffer->consume( &c, 1 ) == 1 )
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
          if ( io_buffer->consume( &c, 1 ) == 1 )
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
void RFC822Headers::serialize( std::ostream& os )
{
  struct iovec* iovecs = heap_iovecs != NULL ? heap_iovecs : stack_iovecs;
  for ( uint8_t iovec_i = 0; iovec_i < iovecs_filled; iovec_i++ )
    os.write( static_cast<const char*>( iovecs[iovec_i].iov_base ), iovecs[iovec_i].iov_len );
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
          case EISCONN: connect_status = Stream::STREAM_STATUS_OK; break;
          case EWOULDBLOCK:
          case EINPROGRESS: connect_status = Stream::STREAM_STATUS_WANT_WRITE; break;
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
ssize_t Socket::recv( void* buffer, size_t buffer_len )
{
#if defined(_WIN32)
  return ::recv( _socket, static_cast<char*>( buffer ), static_cast<int>( buffer_len ), 0 ); // No real advantage to WSARecv on Win32 for one buffer
#elif defined(__linux)
  return ::recv( _socket, buffer, buffer_len, MSG_NOSIGNAL );
#else
  return ::recv( _socket, buffer, buffer_len, 0 );
#endif
}
ssize_t Socket::send( const void* buffer, size_t buffer_len )
{
#ifdef _WIN32
  struct iovec buffers[1];
  buffers[0].iov_base = const_cast<void*>( buffer );
  buffers[0].iov_len = buffer_len;
  return sendmsg( buffers, 1 );
#else
#ifdef __linux
  return ::send( _socket, buffer, buffer_len, MSG_NOSIGNAL );
#else
  return ::send( _socket, buffer, buffer_len, 0 );
#endif
#endif
}
ssize_t Socket::sendmsg( const struct iovec* buffers, uint32_t buffers_count )
{
#ifdef _WIN32
  DWORD dwWrittenLength;
  ssize_t send_ret = ::WSASend( _socket, reinterpret_cast<WSABUF*>( const_cast<struct iovec*>( buffers ) ), buffers_count, &dwWrittenLength, 0, NULL, NULL );
  if ( send_ret >= 0 )
    return static_cast<ssize_t>( dwWrittenLength );
  else
    return send_ret;
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
bool Socket::want_read() const
{
#ifdef _WIN32
  return ::WSAGetLastError() == WSAEWOULDBLOCK || ::WSAGetLastError() == WSAEINPROGRESS;
#else
  return errno == EWOULDBLOCK;
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


// socket_client.cpp
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
SocketClient<ProtocolRequestType, ProtocolResponseType>::SocketClient( const URI& absolute_uri, auto_Object<FDAndInternalEventQueue> fd_event_queue, auto_Object<Log> log, const Time& operation_timeout, auto_Object<SocketAddress> peername, uint8_t reconnect_tries_max, auto_Object<SSLContext> ssl_context )
  : fd_event_queue( fd_event_queue ), log( log ), operation_timeout( operation_timeout ), peername( peername ), reconnect_tries_max( reconnect_tries_max )
{
  this->absolute_uri = new URI( absolute_uri );
  my_stage = NULL;
}
template <class ProtocolRequestType, class ProtocolResponseType>
SocketClient<ProtocolRequestType, ProtocolResponseType>::~SocketClient()
{
  for ( typename std::vector<Connection*>::iterator connection_i = connections.begin(); connection_i != connections.end(); connection_i++ )
    Object::decRef( **connection_i );
}
template <class ProtocolRequestType, class ProtocolResponseType>
void SocketClient<ProtocolRequestType, ProtocolResponseType>::handleEvent( Event& ev )
{
  auto_Object<Connection> connection;
  uint32_t fd_event_error_code = 0;
  switch ( ev.get_tag() )
  {
    case YIELD_OBJECT_TAG( StageStartupEvent ):
    {
      StageStartupEvent& stage_startup_event = static_cast<StageStartupEvent&>( ev );
      my_stage = stage_startup_event.get_stage();
      Object::decRef( ev );
    }
    return;
    case YIELD_OBJECT_TAG( StageShutdownEvent ):
    {
      my_stage = NULL;
      Object::decRef( ev );
    }
    return;
    case YIELD_OBJECT_TAG( FDEventQueue::FDEvent ):
    {
      FDEventQueue::FDEvent& fd_event = static_cast<FDEventQueue::FDEvent&>( ev );
      connection = static_cast<Connection*>( fd_event.get_context().release() );
      connection->touch();
      fd_event_error_code = fd_event.get_error_code();
      Object::decRef( ev );
    }
    break;
    case YIELD_OBJECT_TAG( FDEventQueue::TimerEvent ):
    {
      FDEventQueue::TimerEvent& timer_event = static_cast<FDEventQueue::TimerEvent&>( ev );
      auto_Object<> timer_event_context = timer_event.get_context();
      if ( timer_event_context == NULL ) // Check connection timeouts
      {
        /*
        typename std::vector<Connection*>::size_type connection_i = 0;
        for ( connection_i = 0; connection_i < connections.size(); connection_i++ )
        {
          Connection* connection = connections[connection_i];
          if ( connection->get_state() != Connection::IDLE )
          {
            Time connection_idle_time = Time() - connection->get_last_activity_time();
            if ( connection_idle_time > operation_timeout )
            {
              if ( log != NULL )
                log->getStream( Log::LOG_ERR ) << "SocketClient: connection to " << peername << " exceeded idle timeout (idle for " << connection_idle_time.as_unix_time_s() << " seconds, last activity at " << connection->get_last_activity_time() << "), dropping.";
              DebugBreak();
              //destroyConnection( connection ); // May erase connection from the connections vector, which is why connection_i can't be an iterator
            }
          }
        }
        Object::decRef( ev );
        return;
        */
      }
      else if ( timer_event_context->get_tag() == YIELD_OBJECT_TAG( Connection ) ) // A reconnect try after n seconds
      {
        connection = static_cast<Connection*>( timer_event_context.release() );
        connection->touch();
        Object::decRef( ev );
        // Drop down
      }
      else
        DebugBreak();
    }
    break;
    default:
    {
      auto_Object<ProtocolRequestType> protocol_request = createProtocolRequest( static_cast<Request&>( ev ) ); // Give it the original reference to ev
      for ( typename std::vector<Connection*>::iterator connection_i = connections.begin(); connection_i != connections.end(); connection_i++ )
      {
        if ( ( *connection_i )->get_state() == Connection::IDLE )
        {
          connection = ( *connection_i )->incRef();
          connection->set_state( Connection::CONNECTING );
          connection->touch();
          break;
        }
      }
      if ( connection == NULL )
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
        connection = new Connection( _socket, reconnect_tries_max );
        connections.push_back( &connection->incRef() );
      }
      connection->set_protocol_request( protocol_request );
    }
    break;
  }
  if ( fd_event_error_code == 0 )
  {
    switch ( connection->get_state() )
    {
      case Connection::IDLE:
      case Connection::CONNECTING:
      {
        if ( log != NULL && log->get_level() >= Log::LOG_DEBUG )
          log->getStream( Log::LOG_DEBUG ) << "SocketClient: trying to connect to " << *absolute_uri << ", attempt #" << static_cast<uint32_t>( reconnect_tries_max - connection->get_reconnect_tries_left() ) << ".";
        if ( connection->get_socket()->connect( peername ) )
        {
          if ( log != NULL )
            log->getStream( Log::LOG_INFO ) << "SocketClient: successfully connected to " << *absolute_uri << ".";
          fd_event_queue->attach( *connection->get_socket(), connection->incRef() ); // Only attach AFTER connecting, in case the socket has to be re-created
          connection->set_state( Connection::WRITING );
          // Drop down to WRITING
  	    }
        else if ( connection->get_socket()->want_write() ) // Wait for non-blocking connect() to complete
        {
          if ( log != NULL && log->get_level() >= Log::LOG_DEBUG )
            log->getStream( Log::LOG_DEBUG ) << "SocketClient: waiting for non-blocking connect() to " << *absolute_uri << " to complete.";
          fd_event_queue->attach( *connection->get_socket(), connection->incRef(), false, true );
          connection->set_state( Connection::CONNECTING );
          return;
        }
        else
        {
          if ( log != NULL )
            log->getStream( Log::LOG_ERR ) << "SocketClient: connection attempt #" << static_cast<uint32_t>( reconnect_tries_max - connection->get_reconnect_tries_left() ) << " to  " << *absolute_uri << " failed: " << Exception::strerror();
          break; // Drop down to try to reconnect
        }
      }
      // No break here, to allow drop down to WRITING
      case Connection::WRITING:
      {
        auto_Object<ProtocolRequestType> protocol_request = connection->get_protocol_request();
        std::ostringstream protocol_request_ostringstream;
        protocol_request->serialize( protocol_request_ostringstream );
        connection->get_socket()->set_blocking_mode( true );
        if ( connection->get_socket()->send( protocol_request_ostringstream.str().c_str(), protocol_request_ostringstream.str().size() ) > 0 )
        {
          if ( log != NULL )
            log->getStream( Log::LOG_INFO ) << "SocketClient: successfully wrote " << protocol_request->get_type_name() << " to " << *absolute_uri << ".";
          connection->set_protocol_response( createProtocolResponse( protocol_request ) );
          connection->set_state( Connection::READING );
          // Drop down to READING
        }
        else if ( connection->get_socket()->want_write() )
        {
          fd_event_queue->toggle( *connection->get_socket(), true, true );
          return;
        }
        else if ( connection->get_socket()->want_read() )
        {
          fd_event_queue->toggle( *connection->get_socket(), true, false );
          return;
        }
        else
        {
          if ( log != NULL )
            log->getStream( Log::LOG_ERR ) << "SocketClient: lost connection to " << *absolute_uri << " on write, error: " << Exception::strerror();
          break; // Drop down to reconnect
        }
      }
      // No break here, to allow drop down to READING
      case Connection::READING:
      {
        auto_Object<ProtocolResponseType> protocol_response = connection->get_protocol_response();
        if ( log != NULL && log->get_level() >= Log::LOG_DEBUG )
          log->getStream( Log::LOG_DEBUG ) << "SocketClient: trying to read " << protocol_response->get_type_name() << " from " << *absolute_uri << ".";
        connection->get_socket()->set_blocking_mode( false );
        for ( ;; )
        {
          auto_Object<IOBuffer> protocol_response_buffer = new IOBuffer( 1024 );
          ssize_t recv_ret = connection->get_socket()->recv( *protocol_response_buffer, 1024 );
          if ( recv_ret > 0 )
          {
            protocol_response_buffer->set_size( recv_ret );
            ssize_t deserialize_ret = protocol_response->deserialize( protocol_response_buffer);
            if ( deserialize_ret == 0 )
            {
              respond( connection->get_protocol_request(), connection->get_protocol_response() );
              connection->set_protocol_request( NULL );
              connection->set_protocol_response( NULL );
              connection->set_reconnect_tries_left( reconnect_tries_max );
              connection->set_state( Connection::IDLE );
              fd_event_queue->detach( *connection->get_socket() );
              return;
            }
            else if ( deserialize_ret > 0 )
              continue;
            else
              break; // Drop down to reconnect
          }
          else if ( connection->get_socket()->want_read() )
          {
            fd_event_queue->toggle( *connection->get_socket(), true, false );
            return;
          }
          else if ( connection->get_socket()->want_write() )
          {
            fd_event_queue->toggle( *connection->get_socket(), true, true );
            return;
          }
          else
          {
 //           if ( log != NULL )
 //             log->getStream( Log::LOG_ERR ) << "SocketClient: lost connection to " << *absolute_uri << " on read, error: " << Exception::strerror();
            break;
          }
        }
      }
      // Drop down to reconnect
    }
  }
  else if ( log != NULL ) // fd_event_error_code != 0
    log->getStream( Log::LOG_ERR ) << "SocketClient: connection attempt #" << static_cast<uint32_t>( reconnect_tries_max - connection->get_reconnect_tries_left() ) << " to " << *absolute_uri << " failed: " << Exception::strerror( fd_event_error_code );
    // Drop down to reconnect
  for ( typename std::vector<Connection*>::iterator connection_i = connections.begin(); connection_i != connections.end(); )
  {
    if ( **connection_i == *connection )
    {
      uint8_t reconnect_tries_left = connection->get_reconnect_tries_left();
      auto_Object<ProtocolRequestType> protocol_request = connection->get_protocol_request();
      if ( --reconnect_tries_left > 0 )
      {
        connection->set_reconnect_tries_left( reconnect_tries_left );
        connection->set_state( Connection::CONNECTING );
        fd_event_queue->detach( *connection->get_socket() );
        fd_event_queue->timer_create( Time( 1 * NS_IN_S ), connection->incRef() ); // Wait for a delay to attach, add to connections, and try to connect again
      }
      else
      {
        if ( log != NULL )
          log->getStream( Log::LOG_ERR ) << "SocketClient: exhausted connection retries to " << *absolute_uri << ".";
        if ( protocol_request != NULL )
        {
          connection->set_protocol_request( NULL );
          // We've lost errno here
#ifdef _WIN32
          respond( protocol_request, new ExceptionResponse( "exhausted connection retries" ) );
#else
          respond( protocol_request, new ExceptionResponse( "exhausted connection retries" ) );
#endif
        }
        connection->set_state( Connection::IDLE );
        fd_event_queue->detach( *connection->get_socket() );
      }
      break;
    }
    else
      ++connection_i;
  }
}
template <class ProtocolRequestType, class ProtocolResponseType>
SocketClient<ProtocolRequestType, ProtocolResponseType>::Connection::Connection( auto_Object<Socket> _socket, uint8_t reconnect_tries_max )
  : _socket( _socket ), reconnect_tries_left( reconnect_tries_max )
{
  state = IDLE;
}
template class SocketClient<HTTPRequest, HTTPResponse>;
template class SocketClient<ONCRPCRequest, ONCRPCResponse>;


// socket_server.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).



void SocketServer::handleEvent( Event& ev )
{
  switch ( ev.get_tag() )
  {
#ifdef YIELD_HAVE_OPENSSL
    case YIELD_OBJECT_TAG( SSLSocket ):
#endif
    case YIELD_OBJECT_TAG( TCPSocket ):
    case YIELD_OBJECT_TAG( UDPSocket ):
    {
      protocol_request_reader_stage->send( ev );
    }
    break;

    default: handleUnknownEvent( ev );
  }
}

template <class ProtocolRequestType>
void SocketServer::ProtocolRequestReader<ProtocolRequestType>::handleEvent( Event& ev )
{
  auto_Object<ProtocolRequestType> protocol_request;
  auto_Object<Socket> _socket;


  switch ( ev.get_tag() )
  {
    case YIELD_OBJECT_TAG( FDEventQueue::FDEvent ):
    {
      FDEventQueue::FDEvent& fd_event = static_cast<FDEventQueue::FDEvent&>( ev );
      auto_Object<> context = fd_event.get_context();

      switch ( context->get_tag() )
      {
        case YIELD_OBJECT_TAG( ProtocolRequestType ):
        {
          protocol_request = static_cast<ProtocolRequestType*>( context.release() );
          _socket = protocol_request->get_socket();
          Object::decRef( ev );
          // Drop down to deserialize
        }
        break;

#ifdef YIELD_HAVE_OPENSSL
        case YIELD_OBJECT_TAG( SSLSocket ):
#endif
        case YIELD_OBJECT_TAG( TCPSocket ):
        case YIELD_OBJECT_TAG( UDPSocket ):
        {
          _socket = static_cast<Socket*>( context.release() );
          fd_event_queue->detach( *_socket );
          Object::decRef( ev );
          return;
        }
        break;

        default: DebugBreak(); return;
      }
    }
    break;

#ifdef YIELD_HAVE_OPENSSL
    case YIELD_OBJECT_TAG( SSLSocket ):
#endif
    case YIELD_OBJECT_TAG( TCPSocket ):
    {
      _socket = static_cast<Socket&>( ev );
      protocol_request = createProtocolRequest( _socket );
      fd_event_queue->detach( *_socket );
      fd_event_queue->attach( *_socket, protocol_request->incRef(), true );
    }
    return; // Wait for a read notification

    case YIELD_OBJECT_TAG( UDPSocket ):
    {
      _socket = static_cast<Socket&>( ev );
      protocol_request = createProtocolRequest( _socket );
    }
    break; // Drop down to deserailize

    default: handleUnknownEvent( ev ); return;
  }

  auto_Object<IOBuffer> protocol_request_buffer = new IOBuffer( 1024 );

  ssize_t recv_ret = _socket->recv( *protocol_request_buffer, 1024 );

  if ( recv_ret > 0 )
  {
    protocol_request_buffer->set_size( recv_ret );

    ssize_t deserialize_ret = protocol_request->deserialize( protocol_request_buffer );

    if ( deserialize_ret == 0 )
    {
      fd_event_queue->detach( *_socket );
      fd_event_queue->attach( *_socket, &_socket->incRef(), true );
      sendProtocolRequest( static_cast<ProtocolRequestType&>( protocol_request->incRef() ) );
      return;
    }
    else if ( deserialize_ret > 0 )
    {
      fd_event_queue->toggle( *_socket, true, false );
      return;
    }
  }
  else if ( recv_ret < 0 )
  {
    if ( _socket->want_read() )
    {
      fd_event_queue->toggle( *_socket, true, false );
      return;
    }
    else if ( _socket->want_write() )
    {
      fd_event_queue->toggle( *_socket, true, true );
      return;
    }
  }

  fd_event_queue->detach( *_socket );
  _socket->close();
  Object::decRef( *protocol_request ); // The reference that's attached to fd_event_queue
}


template class SocketServer::ProtocolRequestReader<HTTPServer::HTTPRequest>;
template class SocketServer::ProtocolRequestReader<ONCRPCServer::ONCRPCRequest>;


template <class ProtocolResponseType>
void SocketServer::ProtocolResponseWriter<ProtocolResponseType>::handleEvent( Event& ev )
{
  switch( ev.get_tag() )
  {
    case YIELD_OBJECT_TAG( ProtocolResponseType ):
    {
      ProtocolResponseType& protocol_response = static_cast<ProtocolResponseType&>( ev );
      auto_Object<Socket> _socket = protocol_response.get_socket();

      std::ostringstream protocol_response_ostringstream;
      protocol_response.serialize( protocol_response_ostringstream );
      _socket->send( protocol_response_ostringstream.str().c_str(), protocol_response_ostringstream.str().size() );
      Object::decRef( protocol_response );

      if ( protocol_request_reader_stage != NULL &&
           _socket->get_tag() != YIELD_OBJECT_TAG( UDPSocket ) )
         protocol_request_reader_stage->send( *_socket.release() );
    }
    break;

    default: handleUnknownEvent( ev ); break;
  }
}


template class SocketServer::ProtocolResponseWriter<HTTPServer::HTTPResponse>;
template class SocketServer::ProtocolResponseWriter<ONCRPCServer::ONCRPCResponse>;


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
ssize_t SSLSocket::recv( void* buffer, size_t buffer_len )
{
  return SSL_read( ssl, buffer, static_cast<int>( buffer_len ) );
}
ssize_t SSLSocket::send( const void* buffer, size_t buffer_len )
{
  return SSL_write( ssl, buffer, static_cast<int>( buffer_len ) );
}
ssize_t SSLSocket::sendmsg( const struct iovec* buffers, uint32_t buffers_count )
{
  if ( buffers_count == 1 )
    return send( buffers[0].iov_base, buffers[0].iov_len );
  else
  {
    std::string buffer;
    for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
      buffer.append( static_cast<const char*>( buffers[buffer_i].iov_base ), buffers[buffer_i].iov_len );
    return send( buffer.c_str(), buffer.size() );
  }
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
  this->attach( *listen_tcp_socket, NULL );
}
bool TCPListenQueue::enqueue( Event& ev )
{
  switch ( ev.get_tag() )
  {
    case YIELD_OBJECT_TAG( StageStartupEvent ):	break;
    case YIELD_OBJECT_TAG( StageShutdownEvent ): listen_tcp_socket->close(); break;
  }
  Object::decRef( ev );
  return true;
}
Event* TCPListenQueue::dequeue()
{
  return listen_tcp_socket->accept().release();
}
Event* TCPListenQueue::dequeue( uint64_t timeout_ns )
{
  if ( FDEventQueue::dequeue( timeout_ns ) )
  {
#ifdef YIELD_HAVE_SOLARIS_EVENT_PORTS
    Event* ev = listen_tcp_socket->accept().release();
    // The event port automatically dissociates events, so we have to re-associate here
    toggle( *listen_tcp_socket, NULL, true, false );
    return ev;
#else
    return listen_tcp_socket->accept().release();
#endif
  }
  else
    return NULL;
}


// tcp_socket.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).


#if defined(_WIN32)
#pragma warning( push )
#pragma warning( disable: 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#else
#include <netinet/in.h> // For the IPPROTO_* constants
#include <netinet/tcp.h> // For the TCP_* constants
#include <sys/socket.h>
#endif

#include <cstring>


auto_Object<TCPSocket> TCPSocket::create()
{
  int domain = AF_INET6;
  int _socket = Socket::create( domain, SOCK_STREAM, IPPROTO_TCP );
  if ( _socket != -1 )
    return new TCPSocket( domain, _socket );
  else
    return NULL;
}

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
UDPRecvFromQueue::UDPRecvFromQueue( auto_Object<SocketAddress> recvfrom_sockname, auto_Object<UDPSocket> recvfrom_socket )
  : recvfrom_sockname( recvfrom_sockname ), recvfrom_socket( recvfrom_socket )
{
  this->attach( *recvfrom_socket, NULL );
}
bool UDPRecvFromQueue::enqueue( Event& ev )
{
  switch ( ev.get_tag() )
  {
    case YIELD_OBJECT_TAG( StageStartupEvent ):	break;
    case YIELD_OBJECT_TAG( StageShutdownEvent ): recvfrom_socket->close(); break;
  }
  Object::decRef( ev );
  return true;
}
Event* UDPRecvFromQueue::dequeue()
{
  return recvfrom();
}
Event* UDPRecvFromQueue::dequeue( uint64_t timeout_ns )
{
  if ( FDEventQueue::dequeue( timeout_ns ) )
  {
#ifdef YIELD_HAVE_SOLARIS_EVENT_PORTS
    Event* ev = recvfrom();
    // The event port automatically dissociates events, so we have to re-associate here
    toggle( *recvfrom_socket, NULL, true, false );
    return ev;
#else
    return recvfrom();
#endif
  }
  else
    return NULL;
}
UDPSocket* UDPRecvFromQueue::recvfrom()
{
  char recvfrom_buffer[1024];
  struct sockaddr_storage recvfrom_peername;
  socklen_t recvfrom_peername_len = sizeof( recvfrom_peername );
  ssize_t recvfrom_ret = ::recvfrom( *recvfrom_socket, recvfrom_buffer, 1024, 0, reinterpret_cast<struct sockaddr*>( &recvfrom_peername ), &recvfrom_peername_len );
  if ( recvfrom_ret > 0 )
  {
    auto_Object<UDPSocket> udp_socket = UDPSocket::create( recvfrom_buffer, static_cast<size_t>( recvfrom_ret ) );
    if ( udp_socket != NULL )
    {
      int so_reuseaddr = 1;
      if ( ::setsockopt( *udp_socket, SOL_SOCKET, SO_REUSEADDR, ( char* )&so_reuseaddr, sizeof( so_reuseaddr ) ) != -1 &&
           udp_socket->bind( recvfrom_sockname ) && // Need to bind to the address we're doing the recvfrom on so that response sends also come from that address (= allows UDP clients to filter all but the server's responses)
           udp_socket->connect( new SocketAddress( recvfrom_peername ) ) )
           return udp_socket.release();
    }
  }
  return NULL;
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
auto_Object<UDPSocket> UDPSocket::create( char* recvfrom_buffer, size_t recvfrom_buffer_len )
{
  int domain = AF_INET6;
  int _socket = Socket::create( domain, SOCK_DGRAM, IPPROTO_UDP );
  if ( _socket != -1 )
    return new UDPSocket( domain, recvfrom_buffer, recvfrom_buffer_len, _socket );
  else
    return NULL;
}
UDPSocket::UDPSocket( int domain, int _socket )
  : Socket( domain, SOCK_DGRAM, IPPROTO_UDP, _socket )
{
  recvfrom_buffer = NULL;
}
UDPSocket::UDPSocket( int domain, char* recvfrom_buffer, size_t recvfrom_buffer_len, int _socket )
  : Socket( domain, SOCK_DGRAM, IPPROTO_UDP, _socket )
{
  this->recvfrom_buffer = new char[recvfrom_buffer_len];
  memcpy_s( this->recvfrom_buffer, recvfrom_buffer_len, recvfrom_buffer, recvfrom_buffer_len );
  this->recvfrom_buffer_len = this->recvfrom_buffer_remaining = recvfrom_buffer_len;
}
UDPSocket::~UDPSocket()
{
  delete [] recvfrom_buffer;
}
ssize_t UDPSocket::recv( void* buffer, size_t buffer_len )
{
  if ( recvfrom_buffer != NULL )
  {
    if ( buffer_len >= recvfrom_buffer_remaining )
    {
      buffer_len = recvfrom_buffer_remaining;
      memcpy_s( buffer, buffer_len, recvfrom_buffer, recvfrom_buffer_remaining );
      delete [] recvfrom_buffer;
      recvfrom_buffer = NULL;
      recvfrom_buffer_len = recvfrom_buffer_remaining = 0;
    }
    else
    {
      memcpy_s( buffer, buffer_len, recvfrom_buffer, buffer_len );
      recvfrom_buffer_remaining -= buffer_len;
    }
    return buffer_len;
  }
  else
    return Socket::recv( buffer, buffer_len );
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
URI::operator std::string() const
{
  std::ostringstream oss;
  oss << *this;
  return oss.str();
}

