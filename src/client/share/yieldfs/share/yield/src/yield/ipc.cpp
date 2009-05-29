// Revision: 1496

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

    ONCRPCRecordInputStream& operator=( const ONCRPCRecordInputStream& ) { return *this; }

    // Object
    YIELD_OBJECT_PROTOTYPES( ONCRPCRecordInputStream, 0 );

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
              record_fragment_length = record_fragment_marker ^ ( 1 << 31 );
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

          memcpy_s( buffer, buffer_len, data_consumed_p, buffer_len );
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
      if ( write_end->connect( listen_socket->getsockname() ) == Stream::STREAM_STATUS_OK )
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
  read_end->read( &m, sizeof( m ), NULL );
#endif
}
#if defined(_WIN32)
unsigned int EventFDPipe::get_read_end() const
{
  return *read_end;
}
unsigned int EventFDPipe::get_write_end() const
{
  return *write_end;
}
#elif !defined(YIELD_HAVE_LINUX_EVENTFD)
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
  write_end->write( &m, sizeof( m ), NULL );
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
static bool CompareTimerEvents( const TimerEvent* left, const TimerEvent* right )
{
  return left->get_fire_time() < right->get_fire_time();
}
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
    std::make_heap( timers.begin(), timers.end(), CompareTimerEvents );
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
#ifdef _WIN32
bool FDEventQueue::attach( unsigned int fd, auto_Object<> context, bool enable_read, bool enable_write )
#else
bool FDEventQueue::attach( int fd, auto_Object<> context, bool enable_read, bool enable_write )
#endif
{
  if ( fd_to_context_map.find( fd ) == NULL )
  {
    Object* released_context = context.release();
#if defined(_WIN32)
    if ( enable_read ) FD_SET( fd, read_fds );
    if ( enable_write ) { FD_SET( fd, write_fds ); FD_SET( fd, except_fds ); }
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
FDEvent* FDEventQueue::dequeueFDEvent()
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
      if ( fd == eventfd_pipe->get_read_end() )
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
TimerEvent* FDEventQueue::dequeueTimerEvent()
{
  if ( !timers.empty() )
  {
    Time current_time;
    if ( timers.back()->get_fire_time() <= current_time )
    {
      TimerEvent* timer_event = timers.back();
      timers.pop_back();
      make_heap( timers.begin(), timers.end(), CompareTimerEvents );
      if ( timer_event->get_period() != 0 )
      {
        TimerEvent* next_timer_event = new TimerEvent( timer_event->get_period(), timer_event->get_period(), timer_event->get_context() );
        timers.push_back( next_timer_event );
        std::push_heap( timers.begin(), timers.end(), CompareTimerEvents );
      }
      return timer_event;
    }
  }
  return NULL;
}
#ifdef _WIN32
void FDEventQueue::detach( unsigned int fd )
#else
void FDEventQueue::detach( int fd )
#endif
{
  active_fds = 0; // Have to discard all returned events because the fd may be in them and we have to assume its context is now invalid
  Object::decRef( fd_to_context_map.remove( fd ) );
#if defined(_WIN32)
  FD_CLR( fd, read_fds ); FD_CLR( fd, read_fds_copy );
  FD_CLR( fd, write_fds ); FD_CLR( fd, write_fds_copy );
  FD_CLR( fd, except_fds ); FD_CLR( fd, except_fds_copy );
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
auto_Object<TimerEvent> FDEventQueue::timer_create( const Time& timeout, const Time& period, auto_Object<> context )
{
  TimerEvent* timer_event = new TimerEvent( timeout, period, context );
  timers.push_back( timer_event );
  std::push_heap( timers.begin(), timers.end(), CompareTimerEvents );
  return timer_event->incRef();
}
#ifdef _WIN32
bool FDEventQueue::toggle( unsigned int fd, bool enable_read, bool enable_write )
#else
bool FDEventQueue::toggle( int fd, bool enable_read, bool enable_write )
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
      char* method_p = method + strnlen( method, 16 );
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
      char* uri_p = uri + strnlen( uri, UINT16_MAX );
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
  http_version = 1;
  deserialize_state = DESERIALIZE_DONE;
  serialize_state = SERIALIZING_STATUS_LINE;
}
HTTPResponse::HTTPResponse( uint16_t status_code, auto_Object<> body )
  : status_code( status_code ), body( body )
{
  http_version = 1;
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
      char* status_code_str_p = status_code_str + strnlen( status_code_str, 3 );
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
        set_header( "Date", http_datetime );
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
      tcp_listen_queue = SSLListenQueue::create( sockname, ssl_context, log ).release();
    else
#endif
      tcp_listen_queue = TCPListenQueue::create( sockname, log );
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
                                            auto_Object<Log> log
#ifdef YIELD_HAVE_OPENSSL
                                            , auto_Object<SSLContext> ssl_context
#endif
                                             );


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
    JSONObject( InputStream& input_stream )
    {
      current_json_value = parent_json_value = NULL;
      reader = yajl_alloc( &JSONObject_yajl_callbacks, NULL, this );
      next_map_key = NULL; next_map_key_len = 0;
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
bool JSONInputStream::readBool( const Declaration& decl )
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
double JSONInputStream::readDouble( const Declaration& decl )
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
int64_t JSONInputStream::readInt64( const Declaration& decl )
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
Object* JSONInputStream::readMap( const Declaration& decl, Object* value )
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
    JSONInputStream child_input_stream( decl, *json_value );
    child_input_stream.readMap( *value );
    json_value->have_read = true;
  }
  return value;
}
void JSONInputStream::readMap( Object& s )
{
  while ( next_json_value )
    s.deserialize( *this );
}
Object* JSONInputStream::readSequence( const Declaration& decl, Object* value )
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
    JSONInputStream child_input_stream( decl, *json_value );
    child_input_stream.readSequence( *value );
    json_value->have_read = true;
  }
  return value;
}
Object* JSONInputStream::readStruct( const Declaration& decl, Object* value )
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
    JSONInputStream child_input_stream( decl, *json_value );
    child_input_stream.readStruct( *value );
    json_value->have_read = true;
  }
  return value;
}
void JSONInputStream::readSequence( Object& s )
{
  while ( next_json_value )
    s.deserialize( *this );
}
void JSONInputStream::readString( const Declaration& decl, std::string& str )
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
void JSONInputStream::readStruct( Object& s )
{
  s.deserialize( *this );
}
JSONValue* JSONInputStream::readJSONValue( const Declaration& decl )
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
  if ( in_map && decl.get_identifier() )
    yajl_gen_string( writer, reinterpret_cast<const unsigned char*>( decl.get_identifier() ), static_cast<unsigned int>( strnlen( decl.get_identifier(), UINT16_MAX ) ) );
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
void JSONOutputStream::writeMap( const Declaration& decl, Object& value )
{
  writeDeclaration( decl );
  JSONOutputStream( underlying_output_stream, write_empty_strings, writer, decl ).writeMap( &value );
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
void JSONOutputStream::writePointer( const Declaration& decl, void* )
{
  writeDeclaration( decl );
  yajl_gen_null( writer );
  flushYAJLBuffer();
}
void JSONOutputStream::writeSequence( const Declaration& decl, Object& value )
{
  writeDeclaration( decl );
  JSONOutputStream( underlying_output_stream, write_empty_strings, writer, decl ).writeSequence( &value );
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
void JSONOutputStream::writeStruct( const Declaration& decl, Object& value )
{
  writeDeclaration( decl );
  JSONOutputStream( underlying_output_stream, write_empty_strings, writer, decl ).writeStruct( &value );
}
void JSONOutputStream::writeStruct( Object* s )
{
  writeMap( s );
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
ONCRPCRequest::ONCRPCRequest( auto_Object<Interface> _interface, auto_Object<Log> log )
  : ONCRPCMessage( 0, _interface, NULL, log )
{ }
ONCRPCRequest::ONCRPCRequest( uint32_t prog, uint32_t proc, uint32_t vers, auto_Object<> body, auto_Object<Log> log )
  : ONCRPCMessage( static_cast<uint32_t>( Time::getCurrentUnixTimeS() ), NULL, body, log ),
    prog( prog ), proc( proc ), vers( vers )
{
  credential_auth_flavor = AUTH_NONE;
}
ONCRPCRequest::ONCRPCRequest( uint32_t prog, uint32_t proc, uint32_t vers, uint32_t credential_auth_flavor, auto_Object<> credential, auto_Object<> body, auto_Object<Log> log )
  : ONCRPCMessage( static_cast<uint32_t>( Time::getCurrentUnixTimeS() ), NULL, body, log ),
    prog( prog ), proc( proc ), vers( vers ),
    credential_auth_flavor( credential_auth_flavor ), credential( credential )
{ }
Stream::Status ONCRPCRequest::deserialize( InputStream& input_stream, size_t* )
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
        xdr_input_stream.readUint32( "prog" );
        xdr_input_stream.readUint32( "vers" );
        uint32_t proc = xdr_input_stream.readUint32( "proc" );
        xdr_input_stream.readUint32( "credential_auth_flavor" );
        uint32_t credential_auth_body_length = xdr_input_stream.readUint32( "credential_auth_body_length" );
        if ( credential_auth_body_length > 0 ) // TODO: read into credentials here
        {
          if ( log != NULL )
            log->getStream( Log::LOG_WARNING ) << get_type_name() << ": received credential body, no processing implemented.";
          char* credential_auth_body = new char[credential_auth_body_length];
          oncrpc_record_input_stream.read( credential_auth_body, credential_auth_body_length, NULL );
          delete [] credential_auth_body;
        }
        xdr_input_stream.readUint32( "verf_auth_flavor" );
        uint32_t verf_auth_body_length = xdr_input_stream.readUint32( "credential_auth_body_length" );
        if ( verf_auth_body_length > 0 )
        {
          if ( log != NULL )
            log->getStream( Log::LOG_WARNING ) << get_type_name() << ": received verification body, no processing implemented.";
          char* verf_auth_body = new char[verf_auth_body_length];
          oncrpc_record_input_stream.read( verf_auth_body, verf_auth_body_length, NULL );
          delete [] verf_auth_body;
        }
        if ( body == NULL && _interface != NULL )
          body = _interface->createRequest( proc ).release();
        if ( body != NULL )
          xdr_input_stream.readStruct( XDRInputStream::Declaration(), body.get() );
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
    xdr_output_stream.writeString( "credential_auth_body", credential_auth_body->c_str(), credential_auth_body->size() );
  }
  xdr_output_stream.writeUint32( "verf_auth_flavor", AUTH_NONE );
  xdr_output_stream.writeUint32( "verf_auth_body_length", 0 );
  xdr_output_stream.writeStruct( XDROutputStream::Declaration(), *body );
  oncrpc_record_output_stream.freeze();
  Stream::Status write_status = output_stream.write( oncrpc_record_output_stream, out_bytes_written );
  if ( write_status == Stream::STREAM_STATUS_OK && log != NULL )
  {
    if ( log->get_level() >= Log::LOG_DEBUG )
    {
      Log::Stream log_stream = log->getStream( Log::LOG_DEBUG );
      log_stream << get_type_name() << ": wrote body = ";
      PrettyPrintOutputStream pretty_print_log_stream( log_stream );
      pretty_print_log_stream.writeStruct( PrettyPrintOutputStream::Declaration(), *body );
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
ONCRPCResponse::ONCRPCResponse( auto_Object<Interface> _interface, auto_Object<> body, auto_Object<Log> log )
  : ONCRPCMessage( 0, _interface, body, log )
{ }
ONCRPCResponse::ONCRPCResponse( uint32_t xid, auto_Object<> body, auto_Object<Log> log )
  : ONCRPCMessage( xid, NULL, body, log )
{ }
Stream::Status ONCRPCResponse::deserialize( InputStream& input_stream, size_t* )
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
    xdr_input_stream.readStruct( XDRInputStream::Declaration(), body.get() );
    if ( log != NULL )
    {
      if ( log->get_level() >= Log::LOG_DEBUG )
      {
        Log::Stream log_stream = log->getStream( Log::LOG_DEBUG );
        log_stream << get_type_name() << ": successfully read body = ";
        PrettyPrintOutputStream pretty_print_log_stream( log_stream );
        pretty_print_log_stream.writeStruct( PrettyPrintOutputStream::Declaration(), *body );
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
  if ( body->get_tag() != YIELD_OBJECT_TAG( ExceptionResponse ) )
    xdr_output_stream.writeUint32( "accept_stat", 0 ); // SUCCESS
  else
    xdr_output_stream.writeUint32( "accept_stat", 5 ); // SYSTEM_ERR
  xdr_output_stream.writeStruct( XDROutputStream::Declaration(), *body );
  oncrpc_record_output_stream.freeze();
  return output_stream.write( oncrpc_record_output_stream, out_bytes_written );
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
      auto_Object<UDPRecvFromQueue> udp_recvfrom_queue = UDPRecvFromQueue::create( sockname, log );
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
        tcp_listen_queue = SSLListenQueue::create( sockname, ssl_context, log ).release();
      else
#endif
        tcp_listen_queue = TCPListenQueue::create( sockname, log );
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
Stream::Status RFC822Headers::deserialize( InputStream& input_stream, size_t* )
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
#ifdef _WIN32
Socket::Socket( int domain, int type, int protocol, unsigned int _socket, auto_Object<Log> log )
#else
Socket::Socket( int domain, int type, int protocol, int _socket, auto_Object<Log> log )
#endif
  : log( log ), domain( domain ), type( type ), protocol( protocol ), _socket( _socket )
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
#ifdef _WIN32
      if ( _socket == static_cast<unsigned int>( -1 ) ) DebugBreak();
#else
      if ( _socket == -1 ) DebugBreak();
#endif
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
Stream::Status Socket::connect( auto_Object<SocketAddress> to_sockaddr )
{
  for ( ;; )
  {
    struct sockaddr* name; socklen_t namelen;
    if ( to_sockaddr->as_struct_sockaddr( domain, name, namelen ) )
    {
      Stream::Status connect_status;
      if ( ::connect( *this, name, namelen ) != -1 )
        connect_status = STREAM_STATUS_OK;
      else
      {
#ifdef _WIN32
        switch ( ::WSAGetLastError() )
        {
          case WSAEISCONN: connect_status = STREAM_STATUS_OK; break;
          case WSAEWOULDBLOCK:
          case WSAEINPROGRESS:
          case WSAEINVAL: connect_status = STREAM_STATUS_WANT_WRITE; break;
          case WSAEAFNOSUPPORT:
#else
        switch ( errno )
        {
          case EISCONN: connect_status = STREAM_STATUS_OK; break;
          case EWOULDBLOCK:
          case EINPROGRESS: connect_status = STREAM_STATUS_WANT_WRITE; break;
          case EAFNOSUPPORT:
#endif
          {
            if ( domain == AF_INET6 )
            {
              close();
              domain = AF_INET; // Fall back to IPv4
              _socket = ::socket( domain, type, protocol );
              continue; // Try to connect again
            }
            else
              connect_status = STREAM_STATUS_ERROR;
          }
          break;
          default: connect_status = STREAM_STATUS_ERROR; break;
        }
      }
      return connect_status;
    }
    else if ( domain == AF_INET6 )
    {
      close();
      domain = AF_INET; // Fall back to IPv4
      _socket = ::socket( domain, type, protocol );
      continue; // Try to connect again
    }
    else
      return STREAM_STATUS_ERROR;
  }
}
#ifdef _WIN32
unsigned int Socket::create( int& domain, int type, int protocol )
#else
int Socket::create( int& domain, int type, int protocol )
#endif
{
#ifdef _WIN32
  SOCKET _socket = ::socket( domain, type, protocol );
  if ( domain == AF_INET6 )
  {
    if ( _socket != INVALID_SOCKET )
    {
      DWORD ipv6only = 0; // Allow dual-mode sockets
      setsockopt( _socket, IPPROTO_IPV6, IPV6_V6ONLY, ( char* )&ipv6only, sizeof( ipv6only ) );
    }
    else if ( ::WSAGetLastError() == WSAEAFNOSUPPORT )
    {
      domain = AF_INET;
      _socket = ::socket( AF_INET, type, protocol );
    }
  }
#else
  int _socket = ::socket( AF_INET6, type, protocol );
  if ( _socket == -1 && errno == EAFNOSUPPORT )
  {
    domain = AF_INET;
    _socket = ::socket( AF_INET, type, protocol );
  }
#endif
  return _socket;
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
Stream::Status Socket::read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
{
  ssize_t recv_ret = this->recv( buffer, buffer_len );
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
  else if ( want_read() )
  {
    if ( log != NULL )
      log->getStream( Log::LOG_INFO ) << "Socket: would block on read on " << this << ", tried to read buffer_len = " << buffer_len << ".";
    return STREAM_STATUS_WANT_READ;
  }
  else if ( want_write() )
  {
    if ( log != NULL && log->get_level() >= Log::LOG_INFO )
      log->getStream( Log::LOG_INFO ) << "Socket: would block on write on " << this << ", tried to read.";
    return STREAM_STATUS_WANT_WRITE;
  }
  else
  {
    if ( log != NULL && log->get_level() >= Log::LOG_INFO )
      log->getStream( Log::LOG_INFO ) << "Socket: lost connection on read on " << this << ", error = " << Exception::strerror() << ".";
    return STREAM_STATUS_ERROR;
  }
}
ssize_t Socket::recv( void* buffer, size_t buffer_len )
{
#if defined(_WIN32)
  return ::recv( *this, static_cast<char*>( buffer ), static_cast<int>( buffer_len ), 0 ); // No real advantage to WSARecv on Win32 for one buffer..
#elif defined(__linux)
  return ::recv( *this, buffer, buffer_len, MSG_NOSIGNAL );
#else
  return ::recv( *this, buffer, buffer_len, 0 );
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
ssize_t Socket::send( const struct iovec* buffers, uint32_t buffers_count )
{
#ifdef _WIN32
  DWORD dwWrittenLength;
  ssize_t send_ret = ::WSASend( *this, reinterpret_cast<WSABUF*>( const_cast<struct iovec*>( buffers ) ), buffers_count, &dwWrittenLength, 0, NULL, NULL );
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
  return ::sendmsg( *this, &_msghdr, MSG_NOSIGNAL ); // MSG_NOSIGNAL = disable SIGPIPE
#else
  return ::sendmsg( *this, &_msghdr, 0 );
#endif
#endif
}
Stream::Status Socket::writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
{
  ssize_t send_ret = this->send( buffers, buffers_count );
  if ( send_ret >= 0 )
  {
    if ( log != NULL )
    {
      Log::Stream log_stream = log->getStream( Log::LOG_DEBUG );
      log_stream << "Socket: write on " << this << ": ";
      log_stream.writev( buffers, buffers_count );
    }
    if ( out_bytes_written )
      *out_bytes_written = static_cast<size_t>( send_ret );
    return STREAM_STATUS_OK;
  }
  else if ( want_write() )
  {
    if ( log != NULL && log->get_level() >= Log::LOG_INFO )
      log->getStream( Log::LOG_INFO ) << "Socket: would block on write on " << this << ".";
    return STREAM_STATUS_WANT_WRITE;
  }
  else if ( want_read() )
  {
    if ( log != NULL )
      log->getStream( Log::LOG_INFO ) << "Socket: would block on read on " << this << ", tried to write.";
    return STREAM_STATUS_WANT_READ;
  }
  else
  {
    if ( log != NULL && log->get_level() >= Log::LOG_INFO )
      log->getStream( Log::LOG_INFO ) << "Socket: lost connection on write on " << this << ", error = " << Exception::strerror() << ".";
    return STREAM_STATUS_ERROR;
  }
}
bool Socket::want_read() const
{
#ifdef _WIN32
  return ::WSAGetLastError() == WSAEWOULDBLOCK || ::WSAGetLastError() == WSAEINPROGRESS;
#else
  return errno == EWOULDBLOCK;
#endif
}
bool Socket::want_write() const
{
  return want_read();
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
    case YIELD_OBJECT_TAG( StageShutdownEvent ): Object::decRef( ev ); return;
    case YIELD_OBJECT_TAG( FDEvent ):
    {
      FDEvent& fd_event = static_cast<FDEvent&>( ev );
      connection = static_cast<Connection*>( fd_event.get_context().release() );
      fd_event_error_code = fd_event.get_error_code();
      Object::decRef( ev );
    }
    break;
    case YIELD_OBJECT_TAG( TimerEvent ):
    {
      TimerEvent& timer_event = static_cast<TimerEvent&>( ev );
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
                log->getStream( YIELD::Log::LOG_ERR ) << "Client: connection to " << peername << " exceeded idle timeout (idle for " << connection_idle_time.as_unix_time_s() << " seconds, last activity at " << connection->get_last_activity_time() << "), dropping.";
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
          break;
        }
      }
      if ( connection == NULL )
      {
        auto_Object<Socket> _socket;
#ifdef YIELD_HAVE_OPENSSL
        if ( absolute_uri->get_scheme()[absolute_uri->get_scheme().size()-1] == 's' &&
             ssl_context != NULL )
          _socket = SSLSocket::create( ssl_context, log ).release();
        else
#endif
        if ( absolute_uri->get_scheme()[absolute_uri->get_scheme().size()-1] == 'u' )
          _socket = UDPSocket::create( log ).release();
        else
          _socket = TCPSocket::create( log ).release();
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
        if ( log != NULL )
          log->getStream( YIELD::Log::LOG_DEBUG ) << "Client: trying to connect to " << peername << ", attempt #" << static_cast<uint32_t>( reconnect_tries_max - connection->get_reconnect_tries_left() ) << ".";
        Stream::Status connect_status = connection->connect( peername );
        if ( connect_status == Stream::STREAM_STATUS_OK ) // Use ifs here instead of a switch to allow simple drop-throughs
        {
          if ( log != NULL )
            log->getStream( YIELD::Log::LOG_INFO ) << "Client: successfully connected to " << peername << ".";
          fd_event_queue->attach( *connection->get_socket(), connection->incRef() ); // Only attach AFTER connecting, in case the socket has to be re-created
          connection->set_state( Connection::WRITING );
          // Drop down to WRITING
  	    }
        else if ( connect_status == Stream::STREAM_STATUS_WANT_WRITE ) // Wait for non-blocking connect() to complete
        {
          if ( log != NULL )
            log->getStream( YIELD::Log::LOG_DEBUG ) << "Client: waiting for non-blocking connect() to " << peername << " to complete.";
          fd_event_queue->attach( *connection->get_socket(), connection->incRef(), false, true );
          connection->set_state( Connection::CONNECTING );
          return;
        }
        else
        {
          if ( log != NULL )
            log->getStream( YIELD::Log::LOG_ERR ) << "Client: connection attempt #" << static_cast<uint32_t>( reconnect_tries_max - connection->get_reconnect_tries_left() ) << " to  " << peername << " failed: " << Exception::strerror();
          break; // Drop down to try to reconnect
        }
      }
      // No break here, to allow drop down to WRITING
      case Connection::WRITING:
      {
        auto_Object<ProtocolRequestType> protocol_request = connection->get_protocol_request();
        Stream::Status write_status = protocol_request->serialize( *connection );
        if ( write_status == Stream::STREAM_STATUS_OK )
        {
          if ( log != NULL )
            log->getStream( YIELD::Log::LOG_INFO ) << "Client: successfully wrote " << protocol_request->get_type_name() << " to " << peername << ".";
          connection->set_protocol_response( createProtocolResponse( protocol_request ) );
          connection->set_state( Connection::READING );
          // Drop down to READING
        }
        else if ( write_status == Stream::STREAM_STATUS_WANT_WRITE )
        {
          fd_event_queue->toggle( *connection->get_socket(), true, true );
          return;
        }
        else if ( write_status == Stream::STREAM_STATUS_WANT_READ )
        {
          fd_event_queue->toggle( *connection->get_socket(), true, false );
          return;
        }
        else if ( write_status == Stream::STREAM_STATUS_ERROR )
        {
          if ( log != NULL )
            log->getStream( YIELD::Log::LOG_ERR ) << "Client: lost connection to " << peername << " on write, error: " << Exception::strerror();
          break; // Drop down to reconnect
        }
        else
          DebugBreak();
      }
      // No break here, to allow drop down to READING
      case Connection::READING:
      {
        auto_Object<ProtocolResponseType> protocol_response = connection->get_protocol_response();
        if ( log != NULL )
          log->getStream( YIELD::Log::LOG_DEBUG ) << "Client: trying to read " << protocol_response->get_type_name() << " from " << peername << ".";
        Stream::Status read_status = protocol_response->deserialize( *connection );
        switch ( read_status )
        {
          case Stream::STREAM_STATUS_OK:
          {
            respond( connection->get_protocol_request(), connection->get_protocol_response() );
            connection->set_protocol_request( NULL );
            connection->set_protocol_response( NULL );
            fd_event_queue->detach( *connection->get_socket() );
            connection->set_state( Connection::IDLE );
          }
          return;
          case Stream::STREAM_STATUS_WANT_READ:
          {
            fd_event_queue->toggle( *connection->get_socket(), true, false );
          }
          return;
          case Stream::STREAM_STATUS_WANT_WRITE:
          {
            fd_event_queue->toggle( *connection->get_socket(), true, true );
          }
          return;
          default:
          {
            if ( log != NULL )
              log->getStream( YIELD::Log::LOG_ERR ) << "Client: lost connection to " << peername << " on read, error: " << Exception::strerror();
          }
          break;
        }
        break; // Drop down to reconnect
      }
      break;
      default: DebugBreak();
    }
  }
  else if ( log != NULL ) // fd_event_error_code != 0
    log->getStream( YIELD::Log::LOG_ERR ) << "Client: connection attempt #" << static_cast<uint32_t>( reconnect_tries_max - connection->get_reconnect_tries_left() ) << " to " << peername << " failed: " << Exception::strerror( fd_event_error_code );
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
        fd_event_queue->timer_create( Time( 1 * NS_IN_S ), connection->incRef() ); // Wait for a delay to attach, add to connections, and try to connect again
      }
      else
      {
        if ( log != NULL )
          log->getStream( YIELD::Log::LOG_ERR ) << "Client: exhausted connection retries to " << peername << ".";
        if ( protocol_request != NULL )
        {
          // We've lost errno here
#ifdef _WIN32
          respond( protocol_request, new ExceptionResponse( "exhausted connection retries" ) );
#else
          respond( protocol_request, new ExceptionResponse( "exhausted connection retries" ) );
#endif
        }
      }
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
template <class ProtocolRequestType, class ProtocolResponseType>
bool SocketClient<ProtocolRequestType, ProtocolResponseType>::Connection::close()
{
  return _socket->close();
}
template <class ProtocolRequestType, class ProtocolResponseType>
Stream::Status SocketClient<ProtocolRequestType, ProtocolResponseType>::Connection::connect( auto_Object<SocketAddress> connect_to_sockaddr )
{
  last_activity_time = Time();
  _socket->set_blocking_mode( false );
  return _socket->connect( connect_to_sockaddr );
}
template <class ProtocolRequestType, class ProtocolResponseType>
Stream::Status SocketClient<ProtocolRequestType, ProtocolResponseType>::Connection::read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
{
  last_activity_time = Time();
  _socket->set_blocking_mode( false );
  return _socket->read( buffer, buffer_len, out_bytes_read );
}
template <class ProtocolRequestType, class ProtocolResponseType>
bool SocketClient<ProtocolRequestType, ProtocolResponseType>::Connection::shutdown()
{
  return _socket->shutdown();
}
template <class ProtocolRequestType, class ProtocolResponseType>
Stream::Status SocketClient<ProtocolRequestType, ProtocolResponseType>::Connection::writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
{
  last_activity_time = Time();
  _socket->set_blocking_mode( false );
  return _socket->writev( buffers, buffers_count, out_bytes_written );
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
    case YIELD_OBJECT_TAG( FDEvent ):
    {
      FDEvent& fd_event = static_cast<FDEvent&>( ev );
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
    case YIELD_OBJECT_TAG( UDPSocket ):
    {
      _socket = static_cast<Socket*>( &ev );
      protocol_request = createProtocolRequest( _socket );
      fd_event_queue->detach( *_socket );
      fd_event_queue->attach( *_socket, protocol_request->incRef(), true );
    }
    break; // Drop down to try to deserialize

    default: handleUnknownEvent( ev ); return;
  }

  _socket->set_blocking_mode( false );
  Stream::Status deserialize_status = protocol_request->deserialize( *_socket );

  switch ( deserialize_status )
  {
    case Stream::STREAM_STATUS_OK:
    {
      fd_event_queue->detach( *_socket );
      fd_event_queue->attach( *_socket, &_socket->incRef(), true );
      sendProtocolRequest( static_cast<ProtocolRequestType&>( protocol_request->incRef() ) );
    }
    break;

    case Stream::STREAM_STATUS_WANT_READ:
    {
      fd_event_queue->toggle( *_socket, true, false );
    }
    break;

    case Stream::STREAM_STATUS_WANT_WRITE:
    {
      fd_event_queue->toggle( *_socket, true, true );
    }
    break;

    case Stream::STREAM_STATUS_ERROR:
    {
      fd_event_queue->detach( *_socket );
      _socket->close();
      Object::decRef( *protocol_request ); // The reference that's attached to fd_event_queue
      // tcp_socket should be dead here, since http_request should have had the last reference to it
    }
    break;

    default: break;
  }
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

      _socket->set_blocking_mode( true );
      protocol_response.serialize( *_socket );
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
auto_Object<SSLListenQueue> SSLListenQueue::create( auto_Object<SocketAddress> sockname,
                                                    auto_Object<SSLContext> ssl_context,
                                                    auto_Object<Log> log )
{
  auto_Object<SSLSocket> listen_ssl_socket = SSLSocket::create( ssl_context, log );
  if ( listen_ssl_socket != NULL &&
       listen_ssl_socket->bind( sockname ) &&
       listen_ssl_socket->listen() )
    return new SSLListenQueue( listen_ssl_socket, log );
  else
    return NULL;
}
SSLListenQueue::SSLListenQueue( auto_Object<SSLSocket> listen_ssl_socket, auto_Object<Log> log )
  : TCPListenQueue( listen_ssl_socket.release(), log )
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
auto_Object<SSLSocket> SSLSocket::create( auto_Object<SSLContext> ctx, auto_Object<Log> log )
{
  SSL* ssl = SSL_new( ctx->get_ssl_ctx() );
  if ( ssl != NULL )
  {
    int domain = AF_INET6;
#ifdef _WIN32
    SOCKET _socket = Socket::create( domain, SOCK_STREAM, IPPROTO_TCP );
    if ( _socket != INVALID_SOCKET )
#else
    int _socket = Socket::create( domain, SOCK_STREAM, IPPROTO_TCP );
    if ( _socket != -1 )
#endif
      return new SSLSocket( domain, _socket, log, ctx, *ssl );
  }
  return NULL;
}
#ifdef _WIN32
SSLSocket::SSLSocket( int domain, unsigned int _socket, auto_Object<Log> log, auto_Object<SSLContext> ctx, SSL& ssl )
#else
SSLSocket::SSLSocket( int domain, int _socket, auto_Object<Log> log, auto_Object<SSLContext> ctx, SSL& ssl )
#endif
  : TCPSocket( domain, _socket, log ), ctx( ctx ), ssl( &ssl )
{
  write_buffer = NULL; write_buffer_len = 0;
  if ( log != NULL )
  {
    SSL_set_app_data( this->ssl, reinterpret_cast<char*>( this ) );
    SSL_set_info_callback( this->ssl, info_callback );
  }
}
SSLSocket::~SSLSocket()
{
  SSL_free( ssl );
  delete [] write_buffer;
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
    return new SSLSocket( get_domain(), peer_socket, log, ctx, *peer_ssl );
  }
  else
    return NULL;
}
Stream::Status SSLSocket::connect( auto_Object<SocketAddress> peer_sockaddr )
{
  Stream::Status connect_status = TCPSocket::connect( peer_sockaddr );
  if ( connect_status == STREAM_STATUS_OK )
  {
    SSL_set_fd( ssl, *this );
    SSL_set_connect_state( ssl );
  }
  return connect_status;
}
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
ssize_t SSLSocket::recv( void* buffer, size_t buffer_len )
{
  return SSL_read( ssl, buffer, static_cast<int>( buffer_len ) );
}
ssize_t SSLSocket::send( const struct iovec* buffers, uint32_t buffers_count )
{
  if ( buffers_count == 1 ) // && buffers[0].iov_len < SSL_MAX_CONTENT_LEN )
    return SSL_write( ssl, buffers[0].iov_base, static_cast<int>( buffers[0].iov_len ) );
  else // Concatenate buffers into a single write_buffer and write that
  {
    if ( write_buffer == NULL )
    {
      for ( unsigned int buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
        write_buffer_len += buffers[buffer_i].iov_len;
      write_buffer = new unsigned char[write_buffer_len];
      unsigned char* write_buffer_p = write_buffer;
      for ( unsigned int buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
      {
        memcpy_s( write_buffer_p, buffers[buffer_i].iov_len, buffers[buffer_i].iov_base, buffers[buffer_i].iov_len );
        write_buffer_p += buffers[buffer_i].iov_len;
      }
    }
    int SSL_write_ret = SSL_write( ssl, reinterpret_cast<unsigned char*>( write_buffer ), write_buffer_len );
    if ( SSL_write_ret > 0 )
    {
#ifdef _DEBUG
      if ( SSL_write_ret != static_cast<int>( write_buffer_len ) ) DebugBreak();
#endif
      delete [] write_buffer;
      write_buffer = NULL; write_buffer_len = 0;
    }
    return SSL_write_ret;
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
auto_Object<TCPListenQueue> TCPListenQueue::create( auto_Object<SocketAddress> sockname,
                                                    auto_Object<Log> log )
{
  auto_Object<TCPSocket> listen_tcp_socket = TCPSocket::create( log );
  if ( listen_tcp_socket != NULL &&
       listen_tcp_socket->bind( sockname ) &&
       listen_tcp_socket->listen() )
    return new TCPListenQueue( listen_tcp_socket, log );
  else
    return NULL;
}
TCPListenQueue::TCPListenQueue( auto_Object<TCPSocket> listen_tcp_socket, auto_Object<Log> log )
  : listen_tcp_socket( listen_tcp_socket ), log( log )
{
  this->attach( *listen_tcp_socket, NULL );
#if !defined(_WIN32) && defined(_DEBUG)
  seen_too_many_open_files = false;
#endif
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
  return accept();
}
Event* TCPListenQueue::dequeue( uint64_t timeout_ns )
{
  if ( FDEventQueue::dequeue( timeout_ns ) )
  {
#ifdef YIELD_HAVE_SOLARIS_EVENT_PORTS
    Event* ev = accept();
    // The event port automatically dissociates events, so we have to re-associate here
    toggle( *listen_tcp_socket, NULL, true, false );
    return ev;
#else
    return accept();
#endif
  }
  else
    return NULL;
}
TCPSocket* TCPListenQueue::accept()
{
  TCPSocket* peer_tcp_socket = listen_tcp_socket->accept().release();
  if ( peer_tcp_socket != NULL )
    return peer_tcp_socket;
#if !defined(_WIN32) && defined(_DEBUG)
  else
  {
    switch ( errno )
    {
      case EWOULDBLOCK: break;
      case EMFILE: if ( seen_too_many_open_files ) break; else seen_too_many_open_files = true;
      default: log->getStream( Log::LOG_ERR ) << "TCPListenQueue: error on accept: " << Exception::strerror(); break;
    }
  }
#endif
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
auto_Object<TCPSocket> TCPSocket::create( auto_Object<Log> log )
{
  int domain = AF_INET6;
#ifdef _WIN32
  SOCKET _socket = Socket::create( domain, SOCK_STREAM, IPPROTO_TCP );
  if ( _socket != INVALID_SOCKET )
#else
  int _socket = Socket::create( domain, SOCK_STREAM, IPPROTO_TCP );
  if ( _socket != -1 )
#endif
    return new TCPSocket( domain, _socket, log );
  else
    return NULL;
}
#ifdef _WIN32
TCPSocket::TCPSocket( int domain, unsigned int _socket, auto_Object<Log> log )
#else
TCPSocket::TCPSocket( int domain, int _socket, auto_Object<Log> log )
#endif
  : Socket( domain, SOCK_STREAM, IPPROTO_TCP, _socket, log )
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
    return new TCPSocket( get_domain(), peer_socket, log );
  else
    return NULL;
}
#ifdef _WIN32
unsigned int TCPSocket::_accept()
#else
int TCPSocket::_accept()
#endif
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
ssize_t TCPSocket::send( const struct iovec* buffers, uint32_t buffers_count )
{
  size_t buffers_len = 0;
  for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
    buffers_len += buffers[buffer_i].iov_len;
  ssize_t ret = 0;
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
    ssize_t Socket_send_ret;
    if ( wrote_until_buffer_i_pos == 0 ) // Writing whole buffers
      Socket_send_ret = Socket::send( &buffers[wrote_until_buffer_i], buffers_count - wrote_until_buffer_i );
    else // Writing part of a buffer
    {
      struct iovec temp_iovec;
      temp_iovec.iov_base = static_cast<char*>( buffers[wrote_until_buffer_i].iov_base ) + wrote_until_buffer_i_pos;
      temp_iovec.iov_len = buffers[wrote_until_buffer_i].iov_len - wrote_until_buffer_i_pos;
      Socket_send_ret = Socket::send( &temp_iovec, 1 );
    }
    if ( Socket_send_ret > 0 )
    {
      ret += Socket_send_ret;
      partial_write_len += Socket_send_ret;
      if ( partial_write_len == buffers_len )
      {
        partial_write_len = 0;
        return ret;
      }
      else
        continue; // A large write filled the socket buffer, try to write again until we finish or get an error
    }
    else
      return Socket_send_ret;
  }
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
auto_Object<UDPRecvFromQueue> UDPRecvFromQueue::create( auto_Object<SocketAddress> sockname, auto_Object<Log> log )
{
  auto_Object<UDPSocket> udp_socket = UDPSocket::create( log );
  if ( udp_socket != NULL )
  {
    int so_reuseaddr = 1;
    if ( ::setsockopt( *udp_socket, SOL_SOCKET, SO_REUSEADDR, ( char* )&so_reuseaddr, sizeof( so_reuseaddr ) ) != -1 &&
         udp_socket->bind( sockname ) )
      return new UDPRecvFromQueue( sockname, udp_socket, log );
  }
  return NULL;
}
UDPRecvFromQueue::UDPRecvFromQueue( auto_Object<SocketAddress> recvfrom_sockname, auto_Object<UDPSocket> recvfrom_socket, auto_Object<Log> log )
  : recvfrom_sockname( recvfrom_sockname ), recvfrom_socket( recvfrom_socket ), log( log )
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
    auto_Object<UDPSocket> udp_socket = UDPSocket::create( log );
    if ( udp_socket != NULL )
    {
      int so_reuseaddr = 1;
      if ( ::setsockopt( *udp_socket, SOL_SOCKET, SO_REUSEADDR, ( char* )&so_reuseaddr, sizeof( so_reuseaddr ) ) != -1 &&
           udp_socket->bind( recvfrom_sockname ) && // Need to bind to the address we're doing the recvfrom on so that response sends also come from that address (= allows UDP clients to filter all but the server's responses)
           udp_socket->connect( new SocketAddress( recvfrom_peername ) ) == Stream::STREAM_STATUS_OK )
      {
        udp_socket->set_recv_buffer( recvfrom_buffer, static_cast<size_t>( recvfrom_ret ) );
        return udp_socket.release();
      }
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
auto_Object<UDPSocket> UDPSocket::create( auto_Object<Log> log )
{
  int domain = AF_INET;
#ifdef _WIN32
  SOCKET _socket = Socket::create( domain, SOCK_DGRAM, IPPROTO_UDP );
  if ( _socket != INVALID_SOCKET )
#else
  int _socket = Socket::create( domain, SOCK_DGRAM, IPPROTO_UDP );
  if ( _socket != -1 )
#endif
    return new UDPSocket( domain, _socket, log );
  else
    return NULL;
}
#ifdef _WIN32
UDPSocket::UDPSocket( int domain, unsigned int _socket, auto_Object<Log> log )
#else
UDPSocket::UDPSocket( int domain, int _socket, auto_Object<Log> log )
#endif
  : Socket( domain, SOCK_DGRAM, IPPROTO_UDP, _socket, log )
{
  recv_buffer_len = recv_buffer_remaining = 0;
}
//bool UDPSocket::joinMulticastGroup( auto_Object<SocketAddress> multicast_group_sockaddr, bool loopback )
//{
//  struct ip_mreq mreq;
//  memset( &mreq, 0, sizeof( mreq ) );
//  mreq.imr_interface.s_addr = INADDR_ANY;
//  mreq.imr_multiaddr.s_addr = htonl( multicast_group_sockaddr.get_ipv4() );
//  bool success = setsockopt( this, IPPROTO_IP, IP_ADD_MEMBERSHIP, reinterpret_cast<char*>( &mreq ), sizeof( mreq ) ) == 0;
//  if ( success && !loopback )
//  {
//    unsigned char loop = 0;
//    setsockopt( this, IPPROTO_IP, IP_MULTICAST_LOOP, reinterpret_cast<const char*>( &loop ), sizeof( loop ) );
//  }
//  return success;
//}
//
//bool UDPSocket::leaveMulticastGroup( auto_Object<SocketAddress> multicast_group_sockaddr )
//{
//  /*
//  struct ip_mreq mreq;
//  memset( &mreq, 0, sizeof( mreq ) );
//  mreq.imr_interface.s_addr = INADDR_ANY;
//  mreq.imr_multiaddr.s_addr = htonl( multicast_group_sockaddr.get_ipv4() );
//  return setsockopt( this, IPPROTO_IP, IP_DROP_MEMBERSHIP, reinterpret_cast<char*>( &mreq ), sizeof( mreq ) ) == 0;
//  */
//  return false;
//}
ssize_t UDPSocket::recv( void* buffer, size_t buffer_len )
{
  if ( recv_buffer_len == 0 )
  {
    ssize_t recv_ret = Socket::recv( recv_buffer, 1024 );
    if ( recv_ret > 0 )
      recv_buffer_len = recv_buffer_remaining = static_cast<size_t>( recv_ret );
    else
      return recv_ret;
  }
  if ( buffer_len >= recv_buffer_remaining )
  {
    buffer_len = recv_buffer_remaining;
    memcpy_s( buffer, buffer_len, recv_buffer + recv_buffer_len - recv_buffer_remaining, recv_buffer_remaining );
    recv_buffer_len = recv_buffer_remaining = 0;
  }
  else
  {
    memcpy_s( buffer, buffer_len, recv_buffer + recv_buffer_len - recv_buffer_remaining, buffer_len );
    recv_buffer_remaining -= buffer_len;
  }
  return buffer_len;
}
void UDPSocket::set_recv_buffer( char* recv_buffer, size_t recv_buffer_len )
{
  memcpy_s( this->recv_buffer, 1024, recv_buffer, recv_buffer_len );
  this->recv_buffer_len = recv_buffer_remaining = recv_buffer_len;
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


// zlib_output_stream.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef YIELD_HAVE_ZLIB
#ifdef _WIN32
#pragma comment( lib, "zdll.lib" )
#endif
zlibOutputStream::zlibOutputStream()
{
  zstream.zalloc = Z_NULL;
  zstream.zfree = Z_NULL;
  zstream.opaque = Z_NULL;
  out_s = NULL;
}
auto_Object<String> zlibOutputStream::serialize( String& s, int level )
{
  if ( deflateInit( &zstream, level ) == Z_OK )
  {
    zstream.next_out = reinterpret_cast<Bytef*>( zout );
    zstream.avail_out = sizeof( zout );
    total_bytes_written = 0;
    out_s = new String();
    s.serialize( *this, NULL );
    int deflate_ret;
    while ( ( deflate_ret = deflate( &zstream, Z_FINISH ) ) == Z_OK ) // Z_OK = need more buffer space to finish compression, Z_STREAM_END = really done
    {
      out_s->append( zout, sizeof( zout ) );
      zstream.next_out = reinterpret_cast<Bytef*>( zout );
      zstream.avail_out = sizeof( zout );
    }
    if ( deflate_ret == Z_STREAM_END )
    {
      if ( ( deflate_ret = deflateEnd( &zstream ) ) == Z_OK )
      {
        if ( zstream.avail_out < sizeof( zout ) )
          out_s->append( zout, sizeof( zout ) - zstream.avail_out );
        if ( out_s->size() < total_bytes_written ) // i.e. the compressed buffer is smaller than the input buffer(s)
          return out_s;
      }
    }
    Object::decRef( *out_s );
  }
  return NULL;
}
Stream::Status zlibOutputStream::writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
{
  int deflate_ret = Z_OK;
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
        out_s->append( zout, sizeof( zout ) );
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
#endif

