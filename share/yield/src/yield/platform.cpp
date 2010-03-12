#include "yield/platform.h"
using namespace yield::platform;


// bio_queue.cpp
class BIOQueue::WorkerThread : public Thread
{
public:
  WorkerThread( BIOCB& biocb )
    : biocb( biocb )
  { }

  // Thread
  void run()
  {
    biocb.execute();
    BIOCB::dec_ref( biocb );
  }

private:
  BIOCB& biocb;
};


BIOQueue& BIOQueue::create()
{
  return *new BIOQueue;
}

void BIOQueue::submit( BIOCB& biocb )
{
  WorkerThread* worker_thread = new WorkerThread( biocb );
  worker_thread->start();
}


// directory.cpp
#ifdef _WIN32
#include <windows.h>
#else
#include <dirent.h>
#endif


Directory::Directory()
#ifdef _WIN32
  : hDirectory( INVALID_HANDLE_VALUE )
#else
  : dirp( NULL )
#endif
{ }

#ifdef _WIN32
Directory::Directory
(
  HANDLE hDirectory,
  const WIN32_FIND_DATA& first_find_data
)
  : hDirectory( hDirectory )
{
  this->first_find_data = new WIN32_FIND_DATA;
  memcpy_s
  (
    this->first_find_data,
    sizeof( *this->first_find_data ),
    &first_find_data,
    sizeof( first_find_data )
  );
}
#else
Directory::Directory( void* dirp )
  : dirp( dirp )
{
}
#endif

Directory::~Directory()
{
#ifdef _WIN32
  if ( hDirectory != INVALID_HANDLE_VALUE )
  {
    FindClose( hDirectory );
    delete first_find_data;
  }
#else
  if ( dirp != NULL )
    closedir( static_cast<DIR*>( dirp ) );
#endif
}

Directory::Entry* Directory::readdir()
{
#ifdef _WIN32
  if ( first_find_data != NULL )
  {
    Entry* entry
      = new Entry( first_find_data->cFileName, *new Stat( *first_find_data ) );
    delete first_find_data;
    first_find_data = NULL;
    return entry;
  }

  WIN32_FIND_DATA next_find_data;
  if ( FindNextFileW( hDirectory, &next_find_data ) )
    return new Entry( next_find_data.cFileName, *new Stat( next_find_data ) );
#else
  struct dirent* next_dirent = ::readdir( static_cast<DIR*>( dirp ) );
  if ( next_dirent != NULL )
    return new Entry( next_dirent->d_name );
#endif

  return NULL;
}


Directory::Entry::Entry( const Path& name )
  : name( name ), stbuf( NULL )
{ }

Directory::Entry::Entry( const Path& name, Stat& stbuf )
  : name( name ), stbuf( &stbuf )
{ }

Directory::Entry::~Entry()
{
  Stat::dec_ref( stbuf );
}


// exception.cpp
#ifdef _WIN32
#include <lmerr.h>
#include <windows.h>
#else
#include <errno.h>
#endif


Exception::Exception()
  : error_message( NULL )
{
#ifdef _WIN32
  error_code = static_cast<uint32_t>( ::GetLastError() );
#else
  error_code = static_cast<uint32_t>( errno );
#endif
}

Exception::Exception( uint32_t error_code )
  : error_code( error_code ), error_message( NULL )
{ }

Exception::Exception( const char* error_message )
  : error_code( 0 ), error_message( NULL )
{
  set_error_message( error_message );
}

Exception::Exception( const string& error_message )
  : error_code( 0 ), error_message( NULL )
{
  set_error_message( error_message.c_str() );
}

Exception::Exception( uint32_t error_code, const char* error_message )
  : error_code( error_code ), error_message( NULL )
{
  set_error_message( error_message );
}

Exception::Exception( uint32_t error_code, const string& error_message )
  : error_code( error_code ), error_message( NULL )
{
  set_error_message( error_message.c_str() );
}

Exception::Exception( const Exception& other )
  : error_code( other.error_code ), error_message( NULL )
{
  set_error_message( other.error_message );
}

Exception::~Exception() throw()
{
#ifdef _WIN32
  LocalFree( error_message );
#else
  delete [] error_message;
#endif
}

const char* Exception::get_error_message() throw()
{
  if ( error_message != NULL )
    return error_message;
  else if ( error_code != 0 )
  {
#ifdef _WIN32
    DWORD dwMessageLength
      = FormatMessageA
      (
        FORMAT_MESSAGE_ALLOCATE_BUFFER|
          FORMAT_MESSAGE_FROM_SYSTEM|
          FORMAT_MESSAGE_IGNORE_INSERTS,
        NULL,
        error_code,
        MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ),
        error_message,
        0,
        NULL
      );

    if ( dwMessageLength > 0 )
    {
      if ( dwMessageLength > 2 )
        error_message[dwMessageLength - 2] = 0; // Cut off trailing \r\n

      return error_message;
    }
    else if ( error_code >= NERR_BASE || error_code <= MAX_NERR )
    {
      HMODULE hModule
        = LoadLibraryEx
        (
          TEXT( "netmsg.dll" ),
          NULL,
          LOAD_LIBRARY_AS_DATAFILE
        ); // Let's hope this is cheap..

      if ( hModule != NULL )
      {
        dwMessageLength
          = FormatMessageA
          (
            FORMAT_MESSAGE_ALLOCATE_BUFFER|
              FORMAT_MESSAGE_FROM_SYSTEM|
              FORMAT_MESSAGE_IGNORE_INSERTS,
            hModule,
            error_code,
            MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ),
            error_message,
            0,
            NULL
          );

        if ( dwMessageLength > 0 )
        {
          FreeLibrary( hModule );

          if ( dwMessageLength > 2 )
            error_message[dwMessageLength - 2] = 0; // Cut off trailing \r\n

          return error_message;
        }
        else
          FreeLibrary( hModule );
      }
    }

    // Could not get an error_message for error_code from FormatMessage
    // Set error_message to a dummy value so we don't have to try this again
    error_message = static_cast<char*>( LocalAlloc( LMEM_FIXED, 19 ) );
    sprintf_s( error_message, 19, "errno = %u", error_code );
    return error_message;
#else
    // strerror_r is more or less unusable in a portable way,
    // thanks to the GNU-specific implementation.
    // You have to define _XOPEN_SOURCE to get the POSIX implementation,
    // but that apparently breaks libstdc++.
    // So we just use strerror.
    set_error_message( strerror( error_code ) );
    return error_message;
#endif
  }
  else
    return "(unknown)";
}

void Exception::set_error_code( uint32_t error_code )
{
  this->error_code = error_code;
}

void Exception::set_error_message( const char* error_message )
{
#ifdef _WIN32
  LocalFree( this->error_message );
#else
  delete [] this->error_message;
#endif

  if ( error_message != NULL )
  {
    size_t error_message_len = strlen( error_message );
#ifdef _WIN32
    this->error_message
      = static_cast<char*>( LocalAlloc( LMEM_FIXED, error_message_len+1 ) );
#else
    this->error_message = new char[error_message_len+1];
#endif
    memcpy( this->error_message, error_message, error_message_len+1 );
  }
  else
    this->error_message = NULL;
}


// fd_event_poller.cpp
#ifdef _WIN32
#ifndef FD_SETSIZE
#define FD_SETSIZE 1024
#endif
#undef INVALID_SOCKET
#pragma warning( push )
#pragma warning( disable: 4365 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#pragma warning( push )
#define INVALID_SOCKET  (SOCKET)(~0)
#pragma warning( disable: 4127 4389 ) // Warnings in the FD_* macros
#else
#include <unistd.h>
#include <sys/poll.h>
#include <vector>
#if defined(__FreeBSD__) || defined(__MACH__) || defined(__OpenBSD__)
#define YIELD_PLATFORM_HAVE_KQUEUE 1
#include <sys/types.h>
#include <sys/event.h>
#include <sys/time.h>
#elif defined(__linux__)
#define YIELD_PLATFORM_HAVE_LINUX_EPOLL 1
#include <sys/epoll.h>
#elif defined(YIELD_PLATFORM_HAVE_SOLARIS_EVENT_PORTS)
#include <port.h>
#endif
#endif


namespace yield
{
  namespace platform
  {
    class FDEventPollerImpl : public FDEventPoller
    {
    protected:
      FDEventPollerImpl()
      { }

      bool associate( fd_t fd, void* context )
      {
        FDToContextMap::const_iterator fd_i = fd_to_context_map.find( fd );
        if ( fd_i == fd_to_context_map.end() )
        {
          fd_to_context_map[fd] = context;
          return true;
        }
        else
        {
#ifdef _WIN32
          WSASetLastError( WSA_INVALID_PARAMETER );
#else
          errno = EEXIST;
#endif
          return false;
        }
      }

      bool find_context( fd_t fd, void** out_context = NULL )
      {
        FDToContextMap::const_iterator fd_i = fd_to_context_map.find( fd );
        if ( fd_i != fd_to_context_map.end() )
        {
          if ( out_context != NULL )
            *out_context = fd_i->second;
          return true;
        }
        else
        {
#ifdef _WIN32
          WSASetLastError( WSA_INVALID_PARAMETER );
#else
          errno = ENOENT;
#endif
          return false;
        }
      }

      // FDEventPoller
      virtual bool dissociate( fd_t fd )
      {
        FDToContextMap::iterator fd_i = fd_to_context_map.find( fd );
        if ( fd_i != fd_to_context_map.end() )
        {
          fd_to_context_map.erase( fd_i );
          return true;
        }
        else
        {
#ifdef _WIN32
          WSASetLastError( WSA_INVALID_PARAMETER );
#else
          errno = ENOENT;
#endif
          return false;
        }
      }

    protected:
      // Need an fd_to_context_map even for poll primitives
      // like kqueue that keep a context pointer in the kernel so that
      // toggle doesn't have to take the context
      // The map is protected and not private because the select()
      // implementation iterates over the fd's
      typedef map<fd_t,void*> FDToContextMap;
      FDToContextMap fd_to_context_map;
    };


#ifdef YIELD_PLATFORM_HAVE_LINUX_EPOLL
    class epollFDEventPoller : public FDEventPollerImpl
    {
    public:
      ~epollFDEventPoller()
      {
        close( epfd );
      }

      static epollFDEventPoller& create()
      {
        int epfd = epoll_create( 32768 );
        if ( epfd != -1 )
          return *new epollFDEventPoller( epfd );
        else
          throw Exception();
      }

      // FDEventPoller
      bool associate( fd_t fd, void* context, bool want_read, bool want_write )
      {
        if ( FDEventPollerImpl::associate( fd, context ) )
        {
          struct epoll_event epoll_event_;
          memset( &epoll_event_, 0, sizeof( epoll_event_ ) );
          epoll_event_.data.fd = fd;
          if ( want_read ) epoll_event_.events |= EPOLLIN;
          if ( want_write ) epoll_event_.events |= EPOLLOUT;

          if ( epoll_ctl( epfd, EPOLL_CTL_ADD, fd, &epoll_event_ ) != -1 )
            return true;
          else
          {
            FDEventPollerImpl::dissociate( fd );
            return false;
          }
        }
        else
          return false;
      }

      bool dissociate( fd_t fd )
      {
        if ( FDEventPollerImpl::dissociate( fd ) )
        {
          // From the man page: In kernel versions before 2.6.9,
          // the EPOLL_CTL_DEL operation required a non-NULL pointer in event,
          // even though this argument is ignored. Since kernel 2.6.9,
          // event can be specified as NULL when using EPOLL_CTL_DEL.
          struct epoll_event epoll_event_;
          return epoll_ctl( epfd, EPOLL_CTL_DEL, fd, &epoll_event_ ) != -1;
        }
        else
          return false;
      }

      int poll( FDEvent* fd_events, int fd_events_len, const Time* timeout )
      {
        if ( epoll_events.capacity() < static_cast<size_t>( fd_events_len ) )
          epoll_events.reserve( static_cast<size_t>( fd_events_len ) );

        int active_fds_count
          = epoll_wait
            (
              epfd,
              &epoll_events[0],
              fd_events_len,
              timeout == NULL
                ? -1
                : static_cast<int>( timeout->as_unix_time_ms() )
            );

        if ( active_fds_count > 0 )
        {
          for
          (
            int active_fd_i = 0;
            active_fd_i < active_fds_count;
            active_fd_i++
          )
          {
            const struct epoll_event& epoll_event_
              = epoll_events[active_fd_i];

            void* context;
            if ( find_context( epoll_event_.data.fd, &context ) )
            {
              fd_events[active_fd_i].fill
              (
                context,
                epoll_event_.data.fd,
                ( epoll_event_.events & EPOLLIN ) == EPOLLIN,
                ( epoll_event_.events & EPOLLOUT ) == EPOLLOUT
              );
            }
            else
              DebugBreak();
          }
        }

        return active_fds_count;
      }

      bool toggle( fd_t fd, bool want_read, bool want_write )
      {
        struct epoll_event epoll_event_;
        memset( &epoll_event_, 0, sizeof( epoll_event_ ) );
        epoll_event_.data.fd = fd;
        if ( want_read ) epoll_event_.events |= EPOLLIN;
        if ( want_write ) epoll_event_.events |= EPOLLOUT;

        if ( epoll_ctl( epfd, EPOLL_CTL_MOD, fd, &epoll_event_ ) != -1 )
        {
#ifdef _DEBUG
          if ( !find_context( fd ) ) DebugBreak();
#endif
          return true;
        }
        else
          return false;
      }

    private:
      epollFDEventPoller( int epfd )
        : epfd( epfd )
      { }

    private:
      int epfd;
      vector<struct epoll_event> epoll_events;
    };
#endif


#ifdef YIELD_PLATFORM_HAVE_SOLARIS_EVENT_PORTS
    class EventPortFDEventPoller : public FDEventPollerImpl
    {
    public:
      ~EventPortFDEventPoller()
      {
        close( port );
      }

      static EventPortFDEventPoller& create()
      {
        int port = port_create();
        if ( port != -1 )
          return *new EventPortFDEventPoller( port );
        else
          throw Exception();
      }

      // FDEventPoller
      bool associate( fd_t fd, void* context, bool want_read, bool want_write )
      {
        if ( FDEventPollerImpl::associate( fd, context ) )
        {
          int events = 0;
          if ( want_read ) events |= POLLIN;
          if ( want_write ) events |= POLLOUT;

          if
          (
            port_associate( port, PORT_SOURCE_FD, fd, events, context ) != -1
          )
            return true;
          else
          {
            FDEventPollerImpl::dissociate( fd );
            return false;
          }
        }
        else
          return false;
      }

      bool dissociate( fd_t fd )
      {
        if ( FDEventPollerImpl::dissociate( fd ) )
          return port_dissociate( port, PORT_SOURCE_FD, fd ) != -1;
        else
          return false;
      }

      int poll( FDEvent* fd_events, int fd_events_len, const Time* timeout )
      {
        if ( port_events.capacity() < fd_events_len )
          port_events.reserve( fd_events_len );

        // port_getn doesn't seem to work -> only one event at a time
        int active_fds_count;
        if ( timeout == NULL )
          active_fds_count = port_get( port, &port_events[0], NULL );
        else
        {
          struct timespec timeout_ts = *timeout;
          active_fds_count = port_get( port, &port_events[0], &timeout_ts );
        }

        if ( active_fds_count > 0 )
        {
          for
          (
            int active_fd_i = 0;
            active_fd_i < active_fds_count;
            active_fd_i++
          )
          (
            const port_event_t& port_event = port_events[active_fd_i];

            fd_events[active_fd_i].fill
            (
              port_event.portev_user
              port_event.portev_object
              ( port_event.portev_events & POLLIN ) == POLLIN,
              ( port_event.portev_events & POLLOUT ) == POLLOUT
            );
          }
        }

        return active_fds_count;
      }

      bool toggle( fd_t fd, bool want_read, bool want_write )
      {
        if ( want_read || want_write )
        {
          void* context;
          if ( find_context( fd, &context ) )
          {
            int events = 0
            if ( want_read ) events |= POLLIN;
            if ( want_write ) events |= POLLOUT;
            return port_associate( poll_fd, PORT_SOURCE_FD, fd, events, context ) != -1;
          }
          else
            return false;
        }
        else
          return port_dissociate( poll_fd, PORT_SOURCE_FD, fd ) != -1;
      }

    private:
      EventPortFDEventPoller( int port )
        : port( port )
      { }

    private:
      int port;
      vector<port_event_t> port_events;
    };
#endif


#ifdef YIELD_PLATFORM_HAVE_KQUEUE
    class kqueueFDEventPoller : public FDEventPollerImpl
    {
    public:
      ~kqueueFDEventPoller()
      {
        close( kq );
      }

      static kqueueFDEventPoller& create()
      {
        int kq = kqueue();
        if ( kq != -1 )
          return *new kqueueFDEventPoller( kq );
        else
          throw Exception();
      }

      // FDEventPoller
      bool associate( fd_t fd, void* context, bool want_read, bool want_write )
      {
        if ( FDEventPollerImpl::associate( fd, context ) )
        {
          struct kevent kevents[2];
          int nchanges = 0;

          if ( want_read )
          {
            EV_SET
            (
              &kevents[nchanges],
              fd,
              EVFILT_READ,
              EV_ENABLE
              0,
              0,
              context
            );

            nchanges++;
          }

          if ( want_write )
          {
            EV_SET
            (
              &kevents[nchanges],
              fd,
              EVFILT_WRITE,
              EV_ENABLE,
              0,
              0,
              context
            );

            nchanges++;
          }

          if ( kevent( kq, kevents, nchanges - 1, 0, 0, NULL ) != -1 )
            return true;
          else
          {
            FDEventPollerImpl::dissociate( fd );
            return false;
          }
        }
        else
          return false;
      }

      bool dissociate( fd_t fd )
      {
        if ( FDEventPollerImpl::dissociate( fd ) )
        {
          struct kevent kevents[2];
          EV_SET( &kevents[0], fd, EVFILT_READ, EV_DELETE, 0, 0, NULL );
          EV_SET( &kevents[1], fd, EVFILT_WRITE, EV_DELETE, 0, 0, NULL );
          kevent( kq, change_events, 2, 0, 0, NULL );
        }
        else
          return false;
      }

      int poll( FDEvent* fd_events, int fd_events_len, const Time* timeout )
      {
        if ( kevents.capacity() < fd_events_len )
          kevents.reserve( fd_events_len );

        int active_fds_count;

        if ( timeout == NULL )
        {
          active_fds_count
            = kevent( epfd, 0, 0, &kevents[0], fd_events_len, NULL );
        }
        else
        {
          struct timespec timeout_ts = *timeout;
          active_fds_count
            = kevent( epfd, 0, 0, &kevents[0], fd_events_len, &timeout_ts );
        }

        if ( active_fds_count > 0 )
        {
          for
          (
            int active_fd_i = 0;
            active_fd_i < active_fds_count;
            active_fd_i++
          )
          (
            const struct kevent& kevent_ = kevents[active_fd_i];

            fd_events[active_fd_i].fill
            (
              kevent_.udata,
              kevent_.ident,
              kevent_.filter == EVFILT_READ,
              kevent_.filter != EVFILT_READ
            );
          }
        }

        return active_fds_count;
      }

      bool toggle( fd_t fd, bool want_read, bool want_write )
      {
        void* context;
        if ( find_context( fd, &context ) )
        {
          struct kevent kevents[2];

          EV_SET
          (
            &kevents[nchanges],
            fd,
            EVFILT_READ, want_read ? EV_ENABLE : EV_DISABLE,
            0,
            0,
            fd_i->second
          );

          EV_SET
          (
            &kevents[nchanges],
            fd,
            EVFILT_WRITE,
            want_write ? EV_ENABLE : EV_DISABLE,
            0,
            0,
            fd_i->second
          );

          return kevent( kq, kevents, 2, 0, 0, NULL ) != -1;
        }
        else
          return false;
      }

    private:
      kqueueFDEventPoller( int kq )
        : epfd( kq )
      { }

    private:
      vector<struct kevent> kevents;
      int kq;
    };
#endif


#ifndef _WIN32
    class pollFDEventPoller : public FDEventPollerImpl
    {
    public:
      static pollFDEventPoller& create()
      {
        return *new pollFDEventPoller;
      }

      // FDEventPoller
      bool associate( fd_t fd, void* context, bool want_read, bool want_write )
      {
        if ( FDEventPollerImpl::associate( fd, context ) )
        {
          fd_to_context_map[fd] = context;

          struct pollfd new_pollfd;
          memset( &new_pollfd, 0, sizeof( new_pollfd ) );
          new_pollfd.fd = fd;
          if ( want_read ) new_pollfd.events |= POLLIN;
          if ( want_write ) new_pollfd.events |= POLLOUT;
          pollfds.push_back( new_pollfd );

          return true;
        }
        else
          return false;
      }

      bool dissociate( fd_t fd )
      {
        if ( FDEventPollerImpl::dissociate( fd ) )
        {
          for
          (
            vector<struct pollfd>::iterator pollfd_i = pollfds.begin();
            pollfd_i != pollfds.end();
            ++pollfd_i
          )
          {
            if ( ( *pollfd_i ).fd == fd )
            {
              pollfds.erase( pollfd_i );
              return true;
            }
          }

          DebugBreak();
        }
        else
          return false;
      }

      int poll( FDEvent* fd_events, int fd_events_len, const Time* timeout )
      {
        int active_fds_count
          = ::poll
            (
              &pollfds[0],
              pollfds.size(),
              timeout == NULL ? -1 : timeout->as_unix_time_ms()
            );

        if ( active_fds_count > 0 )
        {
          int fd_event_i = 0;
          vector<struct pollfd>::const_iterator pollfd_i
            = pollfds.begin();

          while
          (
            active_fds_count > 0
            &&
            fd_event_i < fd_events_len
            &&
            pollfd_i != pollfds.end()
          )
          {
            const struct pollfd& pollfd_ = *pollfd_i;

            if ( pollfd_.revents != 0 )
            {
#ifdef _DEBUG
              if ( ( pollfd_.revents & POLLERR ) == POLLERR )
                DebugBreak();
              if ( ( pollfd_.revents & POLLHUP ) == POLLHUP )
                DebugBreak();
              if ( ( pollfd_.revents & POLLPRI ) == POLLPRI )
                DebugBreak();
#endif

              void* context;
              if ( find_context( pollfd_.fd, &context ) )
              {
                fd_events[fd_event_i].fill
                (
                  context,
                  pollfd_.fd,
                  ( pollfd_.revents & POLLIN ) == POLLIN,
                  ( pollfd_.revents & POLLOUT ) == POLLOUT
                );

                fd_event_i++;
              }
              else
                DebugBreak();

//              pollfd_.revents = 0;

              active_fds_count--;
            }

            ++pollfd_i;
          }

          return fd_event_i;
        }
        else
          return active_fds_count;
      }

      bool toggle( fd_t fd, bool want_read, bool want_write )
      {
        void* context;
        if ( find_context( fd, &context ) )
        {
          for
          (
            vector<struct pollfd>::iterator pollfd_i = pollfds.begin();
            pollfd_i != pollfds.end();
            ++pollfd_i
          )
          {
            if ( ( *pollfd_i ).fd == fd )
            {
              ( *pollfd_i ).events = 0;
              if ( want_read ) ( *pollfd_i ).events |= POLLIN;
              if ( want_write ) ( *pollfd_i ).events |= POLLOUT;
              return true;
            }
          }

          DebugBreak();
        }
        else
          return false;
      }

    private:
      pollFDEventPoller()
      { }

    private:
      vector<struct pollfd> pollfds;
    };
#endif


    class selectFDEventPoller : public FDEventPollerImpl
    {
    public:
      static selectFDEventPoller& create()
      {
        return *new selectFDEventPoller;
      }

      // FDEventPoller
      bool associate( fd_t fd, void* context, bool want_read, bool want_write )
      {
        if ( FDEventPollerImpl::associate( fd, context ) )
        {
          if ( want_read )
            FD_SET( fd, &read_fds );

          if ( want_write )
          {
            //FD_SET( fd, &except_fds );
            FD_SET( fd, &write_fds );
          }

          return true;
        }
        else
          return false;
      }

      bool dissociate( fd_t fd )
      {
        if ( FDEventPollerImpl::dissociate( fd ) )
        {
          //FD_CLR( fd, &except_fds );
          FD_CLR( fd, &read_fds );
          FD_CLR( fd, &write_fds );
          return true;
        }
        else
          return false;
      }

      int poll( FDEvent* fd_events, int fd_events_len, const Time* timeout )
      {
        fd_set except_fds_copy, read_fds_copy, write_fds_copy;

        memcpy_s
        (
          &except_fds_copy,
          sizeof( except_fds_copy ),
          &except_fds,
          sizeof( except_fds )
        );

        memcpy_s
        (
          &read_fds_copy,
          sizeof( read_fds_copy ),
          &read_fds,
          sizeof( read_fds )
        );

        memcpy_s
        (
          &write_fds_copy,
          sizeof( write_fds_copy ),
          &write_fds,
          sizeof( write_fds )
        );

        int active_fds_count;
        if ( timeout == NULL )
        {
          active_fds_count
            = select
              (
                0,
                &read_fds_copy,
                &write_fds_copy,
                &except_fds_copy,
                NULL
              );
        }
        else
        {
          struct timeval timeout_tv = *timeout;
          active_fds_count
            = select
              (
                0,
                &read_fds_copy,
                &write_fds_copy,
                &except_fds_copy,
                &timeout_tv
              );
        }

        if ( active_fds_count > 0 )
        {
          FDToContextMap::const_iterator fd_i = fd_to_context_map.begin();
          int fd_event_i = 0;

          while
          (
            active_fds_count > 0
            &&
            fd_event_i < fd_events_len
            &&
            fd_i != fd_to_context_map.end()
          )
          {
            bool want_except, want_read, want_write;

            //if ( FD_ISSET( fd_i->first, &except_fds_copy ) )
            //{
            //  want_except = true;
            //  active_fds_count--; // one for every fd event, not every fd
            //}
            //else
              want_except = false;

            if ( FD_ISSET( fd_i->first, &read_fds_copy ) )
            {
              want_read = true;
              active_fds_count--;
            }
            else
              want_read = false;

            if
            (
              active_fds_count > 0
              &&
              FD_ISSET( fd_i->first, &write_fds_copy )
            )
            {
              want_write = true;
              active_fds_count--;
            }
            else
              want_write = false;

            if ( want_except || want_read || want_write )
            {
              fd_events[fd_event_i].fill
              (
                fd_i->second,
                fd_i->first,
                want_read,
                want_except | want_write
              );

              fd_event_i++;
            }

            ++fd_i;
          }

          return fd_event_i;
        }
        else
          return active_fds_count;
      }

      bool toggle( fd_t fd, bool want_read, bool want_write )
      {
        void* context;
        if ( find_context( fd, &context ) )
        {
          if ( want_read )
            FD_SET( fd, &read_fds );
          else
            FD_CLR( fd, &read_fds );

          if ( want_write )
          {
            //FD_SET( fd, &except_fds );
            FD_SET( fd, &write_fds );
          }
          else
          {
            //FD_CLR( fd, &except_fds );
            FD_CLR( fd, &write_fds );
          }

          return true;
        }
        else
          return false;
      }

    private:
      selectFDEventPoller()
      {
        FD_ZERO( &except_fds );
        FD_ZERO( &read_fds );
        FD_ZERO( &write_fds );
      }

    private:
      fd_set except_fds, read_fds, write_fds;
    };
  };
};


FDEventPoller& FDEventPoller::create()
{
#if defined(_WIN32)
  return selectFDEventPoller::create();
#elif defined(YIELD_HAVE_KQUEUE)
  return kqueueFDEventPoller::create();
#elif defined(YIELD_PLATFORM_HAVE_LINUX_EPOLL)
  return epollFDEventPoller::create();
#elif defined(YIELD_PLATFORM_HAVE_SOLARIS_EVENT_PORTS)
  return EventPortFDEventPoller::create();
#else
  return pollFDEventPoller::create();
#endif
}

bool FDEventPoller::poll()
{
  FDEvent fd_event;
  return poll( &fd_event, 1, NULL ) == 1;
}

bool FDEventPoller::poll( const Time& timeout )
{
  FDEvent fd_event;
  return poll( &fd_event, 1, &timeout ) == 1;
}

bool FDEventPoller::poll( FDEvent& fd_event )
{
  return poll( &fd_event, 1, NULL ) == 1;
}

bool FDEventPoller::poll( FDEvent& fd_event, const Time& timeout )
{
  return poll( &fd_event, 1, &timeout ) == 1;
}

int FDEventPoller::poll( FDEvent* fd_events, int fd_events_len )
{
  return poll( fd_events, fd_events_len, NULL );
}

int
FDEventPoller::poll
(
  FDEvent* fd_events,
  int fd_events_len,
  const Time& timeout
)
{
  if ( fd_events_len > 0 )
    return poll( fd_events, fd_events_len, &timeout );
  else
    return 0;
}

bool FDEventPoller::try_poll()
{
  FDEvent fd_event;
  Time timeout( 0 * Time::NS_IN_S );
  return poll( &fd_event, 1, &timeout ) == 1;
}

bool FDEventPoller::try_poll( FDEvent& fd_event )
{
  Time timeout( 0 * Time::NS_IN_S );
  return poll( &fd_event, 1, &timeout ) == 1;
}

int FDEventPoller::try_poll( FDEvent* fd_events, int fd_events_len )
{
  Time timeout( 0 * Time::NS_IN_S );
  return poll( fd_events, fd_events_len, &timeout );
}

#ifdef _WIN32
#pragma warning( pop )
#endif


// file.cpp
#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#else
#include <stdlib.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/uio.h> // For writev
#include <unistd.h>
#if defined(__linux__)
// Mac OS X's off_t is already 64-bit
#define lseek lseek64
#elif defined(__sun)
extern off64_t lseek64(int, off64_t, int);
#define lseek lseek64
#endif
#ifdef YIELD_PLATFORM_HAVE_POSIX_AIO
#include <aio.h>
#endif
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
#if defined(__linux__)
#include <sys/xattr.h>
#define FLISTXATTR ::flistxattr
#define FGETXATTR ::fgetxattr
#define FSETXATTR ::fsetxattr
#define FREMOVEXATTR ::fremovexattr
#elif defined(__MACH__)
#include <sys/xattr.h>
#define FLISTXATTR( fd, namebuf, size ) \
  ::flistxattr( fd, namebuf, size, 0 )
#define FGETXATTR( fd, name, value, size ) \
  ::fgetxattr( fd, name, value, size, 0, 0 )
#define FSETXATTR( fd, name, value, size, flags ) \
  ::fsetxattr( fd, name, value, size, 0, flags )
#define FREMOVEXATTR( fd, name ) \
  ::fremovexattr( fd, name, 0 )
#endif
#endif
#endif


File::File()
  : fd( INVALID_FD )
{ }

File::File( fd_t fd )
  : fd( fd )
{ }

File::File( const File& other )
{
  DebugBreak();
}

bool File::close()
{
  if ( Stream::close( fd ) )
  {
    fd = INVALID_FD;
    return true;
  }
  else
    return false;
}

bool File::datasync()
{
#if defined(_WIN32)
  return FlushFileBuffers( fd ) != 0;
#elif defined(__linux__) || defined(__sun)
  return fdatasync( fd ) != -1;
#else
  return true;
#endif
}

size_t File::getpagesize()
{
#ifdef _WIN32
  SYSTEM_INFO system_info;
  GetSystemInfo( &system_info );
  return system_info.dwPageSize;
#else
  return ::getpagesize();
#endif
}

Stat* File::getattr()
{
#ifdef _WIN32
  BY_HANDLE_FILE_INFORMATION by_handle_file_information;
  if ( GetFileInformationByHandle( fd, &by_handle_file_information ) != 0 )
    return new Stat( by_handle_file_information );
#else
  struct stat stbuf;
  if ( fstat( fd, &stbuf ) != -1 )
    return new Stat( stbuf );
#endif
  return NULL;
}

bool File::getlk( bool exclusive, uint64_t offset, uint64_t length )
{
#ifdef _WIN32
  return false;
#else
  struct flock flock_;
  flock_.l_type   = exclusive ? F_WRLCK : F_RDLCK;
  flock_.l_whence = SEEK_SET;
  flock_.l_start  = offset;
  flock_.l_len    = length;
  flock_.l_pid    = getpid();
  if ( fcntl( fd, F_GETLK, &flock_ ) != -1 )
    return flock_.l_type != F_UNLCK;
  else
    return false;
#endif
}

bool File::getxattr( const string& name, string& out_value )
{
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
  ssize_t value_len = FGETXATTR( fd, name.c_str(), NULL, 0 );
  if ( value_len != -1 )
  {
    char* value = new char[value_len];
    FGETXATTR( fd, name.c_str(), value, value_len );
    out_value.assign( value, value_len );
    delete [] value;
    return true;
  }
  else
    return false;
#else
  return false;
#endif
}

bool File::listxattr( vector<string>& out_names )
{
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
  size_t names_len = FLISTXATTR( fd, NULL, 0 );
  if ( names_len > 0 )
  {
    char* names = new char[names_len];
    FLISTXATTR( fd, names, names_len );
    char* name = names;
    do
    {
      size_t name_len = strlen( name );
      out_names.push_back( string( name, name_len ) );
      name += name_len;
    }
    while ( static_cast<size_t>( name - names ) < names_len );
    delete [] names;
  }
  return true;
#else
  return false;
#endif
}

ssize_t File::read( Buffer& buffer )
{
  return IStream::read( buffer );
}

ssize_t File::read( void* buf, size_t buflen )
{
#ifdef _WIN32
  DWORD dwBytesRead;
  if
  (
    ReadFile
    (
      *this,
      buf,
      static_cast<DWORD>( buflen ),
      &dwBytesRead,
      NULL
    )
  )
    return static_cast<ssize_t>( dwBytesRead );
  else
    return -1;
#else
  return ::read( *this, buf, buflen );
#endif
}

ssize_t File::read( void* buf, size_t buflen, uint64_t offset )
{
  if ( seek( offset, SEEK_SET ) )
    return read( buf, buflen );
  else
    return -1;
}

bool File::removexattr( const string& name )
{
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
  return FREMOVEXATTR( fd, name.c_str() ) != -1;
#else
  return false;
#endif
}

bool File::seek( uint64_t offset )
{
  return seek( offset, SEEK_SET );
}

bool File::seek( uint64_t offset, unsigned char whence )
{
#ifdef _WIN32
  ULARGE_INTEGER uliOffset;
  uliOffset.QuadPart = offset;
  if
  (
    SetFilePointer
    (
      fd,
      uliOffset.LowPart,
      ( PLONG )&uliOffset.HighPart,
      whence
    ) != INVALID_SET_FILE_POINTER
  )
    return true;
  else
    return false;
#else
  off_t new_offset = lseek( fd, static_cast<off_t>( offset ), whence );
  if ( new_offset != -1 )
  {
//    offset = new_offset;
    return true;
  }
  else
    return false;
#endif
}

bool File::setlk( bool exclusive, uint64_t offset, uint64_t length )
{
#ifdef _WIN32
  return setlkw( exclusive, offset, length );
#else
  struct flock flock_;
  flock_.l_type   = exclusive ? F_WRLCK : F_RDLCK;
  flock_.l_whence = SEEK_SET;
  flock_.l_start  = offset;
  flock_.l_len    = length;
  flock_.l_pid    = getpid();
  return fcntl( fd, F_SETLK, &flock_ ) != -1;
#endif
}

bool File::setlkw( bool exclusive, uint64_t offset, uint64_t length )
{
#ifdef _WIN32
  if ( exclusive )
  {
    ULARGE_INTEGER uliOffset, uliLength;
    uliOffset.QuadPart = offset;
    uliLength.QuadPart = length;
    return LockFile
    (
      fd,
      uliOffset.LowPart,
      uliOffset.HighPart,
      uliLength.LowPart,
      uliLength.HighPart
    ) == TRUE;
  }
  else
    return false;
#else
  struct flock flock_;
  flock_.l_type = exclusive ? F_WRLCK : F_RDLCK;
  flock_.l_whence = SEEK_SET;
  flock_.l_start = offset;
  flock_.l_len = length;
  flock_.l_pid = getpid();
  return fcntl( fd, F_SETLKW, &flock_ ) != -1;
#endif
}

bool File::setxattr
(
  const string& name,
  const string& value,
  int flags
)
{
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
  return FSETXATTR
  (
    fd,
    name.c_str(),
    value.c_str(),
    value.size(),
    flags
  ) != -1;
#else
  return false;
#endif
}

bool File::sync()
{
#ifdef _WIN32
  return FlushFileBuffers( fd ) != 0;
#else
  return fsync( fd ) != -1;
#endif
}

bool File::truncate( uint64_t new_size )
{
#ifdef _WIN32
  ULARGE_INTEGER uliNewSize;
  uliNewSize.QuadPart = new_size;
  if
  (
    SetFilePointer
    (
      fd,
      uliNewSize.LowPart,
      ( PLONG )&uliNewSize.HighPart,
      SEEK_SET
    ) != INVALID_SET_FILE_POINTER
  )
    return SetEndOfFile( fd ) != 0;
  else
    return false;
#else
  return ::ftruncate( fd, new_size ) != -1;
#endif
}

bool File::unlk( uint64_t offset, uint64_t length )
{
#ifdef _WIN32
  ULARGE_INTEGER uliOffset, uliLength;
  uliOffset.QuadPart = offset;
  uliLength.QuadPart = length;
  return UnlockFile
  (
    fd,
    uliOffset.LowPart,
    uliOffset.HighPart,
    uliLength.LowPart,
    uliLength.HighPart
  ) == TRUE;
#else
  struct flock flock_;
  flock_.l_type   = F_UNLCK;
  flock_.l_whence = SEEK_SET;
  flock_.l_start  = offset;
  flock_.l_len    = length;
  flock_.l_pid    = getpid();
  return fcntl( fd, F_SETLK, &flock_ ) != -1;
#endif
}

ssize_t File::write( const Buffer& buffer )
{
  return OStream::write( buffer );
}

ssize_t File::write( const void* buf, size_t buflen )
{
#ifdef _WIN32
  DWORD dwBytesWritten;
  if
  (
    WriteFile
    (
      *this,
      buf,
      static_cast<DWORD>( buflen ),
      &dwBytesWritten,
      NULL
    )
  )
    return static_cast<ssize_t>( dwBytesWritten );
  else
    return -1;
#else
  return ::write( *this, buf, buflen );
#endif
}

ssize_t File::write( const void* buf, size_t buflen, uint64_t offset )
{
  if ( seek( offset ) )
    return write( buf, buflen );
  else
    return -1;
}

#ifndef _WIN32
ssize_t File::writev( Buffers& buffers )
{
  return OStream::writev( buffers );
}

ssize_t File::writev( const struct iovec* iov, uint32_t iovlen )
{
  return ::writev( *this, iov, iovlen );
}
#endif

#ifdef _WIN32
#pragma warning( pop )
#endif


// iconv.cpp
#ifdef _WIN32
#include <windows.h>
#else
#include <iconv.h>
#ifdef __sun
#undef iconv
#endif
#if defined(__FreeBSD__) || defined(__OpenBSD__) || defined(__sun)
#define ICONV_SOURCE_CAST const char**
#else
#define ICONV_SOURCE_CAST char**
#endif
#endif


#ifdef _WIN32
iconv::iconv( UINT from_code_page, UINT to_code_page )
  : from_code_page( from_code_page ), to_code_page( to_code_page )
{ }
#else
iconv::iconv( iconv_t cd )
 : cd( cd )
{ }
#endif

iconv::~iconv()
{
#ifndef _WIN32
  iconv_close( cd );
#endif
}

#ifdef _WIN32
UINT iconv::Code_to_win32_code_page( Code code )
{
  switch ( code )
  {
    case CODE_CHAR: return GetACP();
    case CODE_ISO88591: return CP_ACP;
    case CODE_UTF8: return CP_UTF8;
    default: return GetACP();
  }
}
#else
const char* iconv::Code_to_iconv_code( Code code )
{
  switch ( code )
  {
    case CODE_CHAR: return "";
    case CODE_ISO88591: return "ISO-8859-1";
    case CODE_UTF8: return "UTF-8";
    default: return "";
  }
}
#endif

yield::platform::iconv* iconv::open( Code tocode, Code fromcode )
{
#ifdef _WIN32
  UINT to_code_page = Code_to_win32_code_page( tocode );
  UINT from_code_page = Code_to_win32_code_page( fromcode );
  return new iconv( from_code_page, to_code_page );
#else
  iconv_t cd
    = ::iconv_open
      (
        Code_to_iconv_code( tocode ),
        Code_to_iconv_code( fromcode )
      );

  if ( cd != reinterpret_cast<iconv_t>( -1 ) )
    return new iconv( cd );
  else
    return NULL;
#endif
}

size_t
iconv::operator()
(
  const char** inbuf,
  size_t* inbytesleft,
  char** outbuf,
  size_t* outbytesleft
)
{
#ifdef _WIN32
  int inbuf_w_len
    = MultiByteToWideChar
      (
        from_code_page,
        0,
        *inbuf,
        static_cast<int>( *inbytesleft ),
        NULL,
        0
      );

  if ( inbuf_w_len > 0 )
  {
    wchar_t* inbuf_w = new wchar_t[inbuf_w_len];

    inbuf_w_len
      = MultiByteToWideChar
        (
          from_code_page,
          0,
          *inbuf,
          static_cast<int>( *inbytesleft ),
          inbuf_w,
          inbuf_w_len
        );

    if ( inbuf_w_len > 0 )
    {
      int outbyteswritten
        = WideCharToMultiByte
          (
            to_code_page,
            0,
            inbuf_w,
            inbuf_w_len,
            *outbuf,
            *outbytesleft,
            0,
            0
          );

      delete [] inbuf_w;

      if ( outbyteswritten > 0 )
      {
        *inbuf += *inbytesleft;
        *inbytesleft = 0;
        *outbytesleft -= outbyteswritten;
        return outbyteswritten;
      }
    }
    else
      delete [] inbuf_w;
  }

  return static_cast<size_t>( -1 );
#else
  if ( reset() )
  {
    // Now try to convert; will return ( size_t )-1 on failure
#ifdef __sun
    return ::libiconv
#else
    return ::iconv
#endif
           (
             cd,
             ( ICONV_SOURCE_CAST )inbuf,
             inbytesleft,
             outbuf,
             outbytesleft
           );
  }
  else
    return static_cast<size_t>( -1 );
#endif
}

bool iconv::operator()( const string& inbuf, string& outbuf )
{
#ifdef _WIN32
  int inbuf_w_len
    = MultiByteToWideChar
      (
        from_code_page,
        0,
        inbuf.c_str(),
        inbuf.size(),
        NULL,
        0
      );

  if ( inbuf_w_len > 0 )
  {
    wchar_t* inbuf_w = new wchar_t[inbuf_w_len];

    inbuf_w_len
      = MultiByteToWideChar
        (
          from_code_page,
          0,
          inbuf.c_str(),
          inbuf.size(),
          inbuf_w,
          inbuf_w_len
        );

    if ( inbuf_w_len > 0 )
    {
      int outbuf_c_len
        = WideCharToMultiByte
          (
            to_code_page,
            0,
            inbuf_w,
            inbuf_w_len,
            NULL,
            0,
            0,
            0
          );

      if ( outbuf_c_len > 0 )
      {
        char* outbuf_c = new char[outbuf_c_len];

        outbuf_c_len
          = WideCharToMultiByte
            (
              to_code_page,
              0,
              inbuf_w,
              inbuf_w_len,
              outbuf_c,
              outbuf_c_len,
              0,
              0
            );

        if ( outbuf_c_len > 0 )
        {
          outbuf.append( outbuf_c, outbuf_c_len );
          delete [] outbuf_c;
          return true;
        }
        else
          delete [] outbuf_c;
      }
      else
        delete [] inbuf_w;
    }
    else
      delete [] inbuf_w;
  }

  return false;
#else
  // Reset the converter
  if ( reset() )
  {
    char* inbuf_c = const_cast<char*>( inbuf.c_str() );
    size_t inbytesleft = inbuf.size();
    size_t outbuf_c_len = inbuf.size();

    for ( ;; ) // Loop as long as ::iconv returns E2BIG
    {
      char* outbuf_c = new char[outbuf_c_len];
      char* outbuf_c_dummy = outbuf_c;
      size_t outbytesleft = outbuf_c_len;

      size_t iconv_ret
#ifdef __sun
        = ::libiconv
#else
        = ::iconv
#endif
          (
            cd,
            ( ICONV_SOURCE_CAST )&inbuf_c,
            &inbytesleft,
            &outbuf_c_dummy,
            &outbytesleft
          );

      if ( iconv_ret != static_cast<size_t>( -1 ) )
      {
        outbuf.append( outbuf_c, outbuf_c_len - outbytesleft );
        delete [] outbuf_c;
        return true;
      }
      else if ( errno == E2BIG )
      {
#ifdef _DEBUG
        if ( outbytesleft != 0 ) DebugBreak();
#endif
        outbuf.append( outbuf_c, outbuf_c_len );
        delete [] outbuf_c;
        outbuf_c_len *= 2;
        continue;
      }
      else
      {
        delete [] outbuf_c;
        return false;
      }
    }
  }
  else
    return false;
#endif
}

#ifdef _WIN32
bool iconv::operator()( const string& inbuf, wstring& outbuf )
{
  int outbuf_w_len
    = MultiByteToWideChar
      (
        from_code_page,
        0,
        inbuf.c_str(),
        inbuf.size(),
        NULL,
        0
      );

  if ( outbuf_w_len > 0 )
  {
    wchar_t* outbuf_w = new wchar_t[outbuf_w_len];

    outbuf_w_len
      = MultiByteToWideChar
        (
          from_code_page,
          0,
          inbuf.c_str(),
          inbuf.size(),
          outbuf_w,
          outbuf_w_len
        );

    if ( outbuf_w_len > 0 )
    {
      outbuf.append( outbuf_w, outbuf_w_len );
      delete [] outbuf_w;
    }
    else
      delete [] outbuf_w;
  }

  return false;
}

bool iconv::operator()( const wstring& inbuf, string& outbuf )
{
  int outbuf_c_len
    = WideCharToMultiByte
      (
        to_code_page,
        0,
        inbuf.c_str(),
        inbuf.size(),
        NULL,
        0,
        0,
        0
      );

  if ( outbuf_c_len > 0 )
  {
    char* outbuf_c = new char[outbuf_c_len];

    outbuf_c_len
      = WideCharToMultiByte
        (
          to_code_page,
          0,
          inbuf.c_str(),
          inbuf.size(),
          outbuf_c,
          outbuf_c_len,
          0,
          0
        );

    if ( outbuf_c_len > 0 )
    {
      outbuf.append( outbuf_c, outbuf_c_len );
      delete [] outbuf_c;
      return true;
    }
    else
      delete [] outbuf_c;
  }

  return false;
}
#endif

#ifndef _WIN32
bool iconv::reset()
{
#ifdef __sun
  return ::libiconv( cd, NULL, 0, NULL, 0 ) != static_cast<size_t>( -1 );
#else
  return ::iconv( cd, NULL, 0, NULL, 0 ) != static_cast<size_t>( -1 );
#endif
}
#endif


// istream.cpp
void
IStream::aio_read
(
  Buffer& buffer,
  AIOReadCallback& callback,
  void* callback_context
)
{
  ssize_t read_ret = read( buffer );
  if ( read_ret >= 0 )
  {
    callback.onReadCompletion
    (
      buffer,
      callback_context
    );
  }
  else
  {
#ifdef _WIN32
    callback.onReadError( GetLastError(), callback_context );
#else
    callback.onReadError( errno, callback_context );
#endif
  }

  Buffer::dec_ref( buffer );
}

ssize_t IStream::read( Buffer& buffer )
{
  ssize_t read_ret
    = read
      (
        static_cast<char*>( buffer ) + buffer.size(),
        buffer.capacity() - buffer.size()
      );

  if ( read_ret > 0 )
    buffer.put( static_cast<size_t>( read_ret ) );

  return read_ret;
}


// log.cpp
Log::Level Log::LOG_EMERG( "EMERG", 0 );
Log::Level Log::LOG_ALERT( "ALERT", 1 );
Log::Level Log::LOG_CRIT( "CRIT", 2 );
Log::Level Log::LOG_ERR( "ERR", 3 );
Log::Level Log::LOG_WARNING( "WARNING", 4 );
Log::Level Log::LOG_INFO( "INFO", 5 );
Log::Level Log::LOG_DEBUG( "DEBUG", 6 );


namespace yield
{
  namespace platform
  {
    class FileLog : public Log
    {
    public:
      FileLog( File& file, const Level& level )
        : Log( level ), file( &file )
      { }

      FileLog( const Path& file_path, const Level& level ) // Lazy open
        : Log( level ), file_path( file_path )
      { }

      ~FileLog()
      {
        File::dec_ref( file );
      }

      // Log
      void write( const char* str, size_t str_len )
      {
        if ( file == NULL ) // Lazy open
        {
          file = Volume().open( file_path, O_CREAT|O_WRONLY|O_APPEND );
          if ( file == NULL )
            return;
        }

        file->write( str, str_len );
      }

    private:
      File* file;
      Path file_path;
    };


    class ostreamLog : public Log
    {
    public:
      ostreamLog( ostream& underlying_ostream, const Level& level )
        : Log( level ), underlying_ostream( underlying_ostream )
      { }

      // Log
      void write( const char* str, size_t str_len )
      {
        underlying_ostream.write( str, str_len );
      }

    private:
      ostream& underlying_ostream;
    };
  };
};


Log::Level::Level( const char* level )
  : level_string( level )
{
  level_uint8 = static_cast<uint8_t>( atoi( level ) );
  if ( level_uint8 == 0 )
  {
    if
    (
      strcmp( level, "LOG_EMERG" ) == 0 ||
      strcmp( level, "EMERG" ) == 0 ||
      strcmp( level, "EMERGENCY" ) == 0 ||
      strcmp( level, "FATAL" ) == 0 ||
      strcmp( level, "FAIL" ) == 0
    )
      level_uint8 = 0;

    else if
    (
      strcmp( level, "LOG_ALERT" ) == 0 ||
      strcmp( level, "ALERT" ) == 0
    )
      level_uint8 = 1;

    else if
    (
      strcmp( level, "LOG_CRIT" ) == 0 ||
      strcmp( level, "CRIT" ) == 0 ||
      strcmp( level, "CRITICAL" ) == 0
    )
      level_uint8 = 1;

    else if
    (
      strcmp( level, "LOG_ERR" ) == 0 ||
      strcmp( level, "ERR" ) == 0 ||
      strcmp( level, "ERROR" ) == 0
    )
      level_uint8 = 2;

    else if
    (
      strcmp( level, "LOG_WARNING" ) == 0 ||
      strcmp( level, "WARNING" ) == 0 ||
      strcmp( level, "WARN" ) == 0
    )
      level_uint8 = 3;

    else if
    (
      strcmp( level, "LOG_NOTICE" ) == 0 ||
      strcmp( level, "NOTICE" ) == 0
    )
      level_uint8 = 4;

    else if
    (
      strcmp( level, "LOG_INFO" ) == 0 ||
      strcmp( level, "INFO" ) == 0
    )
      level_uint8 = 5;

    else if
    (
      strcmp( level, "LOG_DEBUG" ) == 0 ||
      strcmp( level, "DEBUG" ) == 0 ||
      strcmp( level, "TRACE" ) == 0
    )
      level_uint8 = 6;

    else
      level_uint8 = 7;
  }
}

Log::Level::Level( uint8_t level )
  : level_uint8( level )
{
  switch ( level )
  {
    case 0: level_string = "EMERG"; break;
    case 1: level_string = "ALERT"; break;
    case 2: level_string = "CRIT"; break;
    case 3: level_string = "ERR"; break;
    case 4: level_string = "WARNING"; break;
    case 5: level_string = "NOTICE"; break;
    case 6: level_string = "INFO"; break;
    default: level_string = "DEBUG"; break;
  }
}

Log::Level::Level( const char* level_string, uint8_t level_uint8 )
: level_string( level_string ), level_uint8( level_uint8 )
{ }

Log::Level::Level( const Level& other )
: level_string( other.level_string ), level_uint8( other.level_uint8 )
{ }

Log::Stream::Stream( Log& log, Log::Level level )
  : log( log ), level( level )
{ }

Log::Stream::Stream( const Stream& other )
  : log( other.log.inc_ref() ), level( other.level )
{ }

Log::Stream::~Stream()
{
  if ( level <= log.get_level() && !oss.str().empty() )
  {
    ostringstream stamped_oss;
    stamped_oss << static_cast<string>( Time() );
    stamped_oss << " ";
    stamped_oss << static_cast<const char*>( log.get_level() );
    stamped_oss << ": ";
    stamped_oss << oss.str();
    stamped_oss << endl;

    log.write( stamped_oss.str(), level );
  }

  Log::dec_ref( log );
}


Log::Log( const Level& level )
  : level( level )
{ }

Log& Log::open( ostream& underlying_ostream, const Level& level )
{
  return *new ostreamLog( underlying_ostream, level );
}

Log& Log::open( const Path& file_path, const Level& level, bool lazy_open )
{
  if ( file_path == "-" )
    return *new ostreamLog( cout, level );
  else if ( lazy_open )
    return *new FileLog( file_path, level );
  else
  {
    File* file = Volume().open( file_path, O_CREAT|O_WRONLY|O_APPEND );
    if ( file != NULL )
      return *new FileLog( *file, level );
    else
      throw Exception();
  }
}

void Log::write( const char* str, const Level& level )
{
  write( str, strnlen( str, UINT16_MAX ), level );
}

void Log::write( const string& str, const Level& level )
{
  write( str.c_str(), str.size(), level );
}

void Log::write( const char* str, size_t str_len, const Level& level )
{
  if ( level <= this->level )
    write( str, str_len );
}

void Log::write( const void* str, size_t str_len, const Level& level )
{
  return write
  (
    static_cast<const unsigned char*>( str ),
    str_len,
    level
  );
}

void Log::write( const unsigned char* str, size_t str_len, const Level& level )
{
  if ( level <= this->level )
  {
    bool str_is_printable = true;
    for ( size_t str_i = 0; str_i < str_len; str_i++ )
    {
      if
      (
        str[str_i] == '\r' ||
        str[str_i] == '\n' ||
        ( str[str_i] >= 32 && str[str_i] <= 126 )
      )
        continue;
      else
      {
        str_is_printable = false;
        break;
      }
    }

    if ( str_is_printable )
      write( reinterpret_cast<const char*>( str ), str_len, level );
    else
    {
      char* printable_str = new char[str_len * 3];
      size_t printable_str_len = 0;

      for ( size_t str_i = 0; str_i < str_len; str_i++ )
      {
        char hex_digit = ( str[str_i] >> 4 ) & 0x0F;
        if ( hex_digit >= 0 && hex_digit <= 9 )
          printable_str[printable_str_len++] = '0' + hex_digit;
        else
          printable_str[printable_str_len++] = 'A' + hex_digit - 10;

        hex_digit = str[str_i] & 0x0F;
        if ( hex_digit >= 0 && hex_digit <= 9 )
          printable_str[printable_str_len++] = '0' + hex_digit;
        else
          printable_str[printable_str_len++] = 'A' + hex_digit - 10;

        printable_str[printable_str_len++] = ' ';
      }

      write( printable_str, printable_str_len );

      delete [] printable_str;
    }
  }
}


// memory_mapped_file.cpp
using std::max;

#ifdef _WIN32
#include <windows.h>
#else
#include <sys/mman.h>
#endif


MemoryMappedFile::MemoryMappedFile
(
  File& underlying_file,
  uint32_t open_flags
)
  : underlying_file( underlying_file ),
    open_flags( open_flags )
{
#ifdef _WIN32
  mapping = NULL;
#endif
  size_ = 0;
  start = NULL;
}

MemoryMappedFile::~MemoryMappedFile()
{
  close();
  File::dec_ref( underlying_file );
}

bool MemoryMappedFile::close()
{
  if ( start != NULL )
  {
    sync();
#ifdef _WIN32
    UnmapViewOfFile( start );
#else
    munmap( start, size() );
#endif
    start = NULL;
  }

#ifdef _WIN32
  if ( mapping != NULL )
  {
    CloseHandle( mapping );
    mapping = NULL;
  }
#endif

  return underlying_file.close();
}

MemoryMappedFile* MemoryMappedFile::open( const Path& path )
{
  return open
         (
           path,
           File::FLAGS_DEFAULT,
           File::MODE_DEFAULT,
           File::ATTRIBUTES_DEFAULT,
           0
          );
}

MemoryMappedFile* MemoryMappedFile::open( const Path& path, uint32_t flags )
{
  return open
         (
           path,
           flags,
           File::MODE_DEFAULT,
           File::ATTRIBUTES_DEFAULT,
           0
         );
}

MemoryMappedFile*
MemoryMappedFile::open
(
  const Path& path,
  uint32_t flags,
  mode_t mode,
  uint32_t attributes,
  size_t minimum_size
)
{
  File* file = Volume().open( path, flags, mode, attributes );

  if ( file != NULL )
  {
    size_t current_file_size;
    if ( ( flags & O_TRUNC ) != O_TRUNC )
    {
#ifdef _WIN32
      ULARGE_INTEGER uliFileSize;
      uliFileSize.LowPart = GetFileSize( *file, &uliFileSize.HighPart );
      current_file_size = static_cast<size_t>( uliFileSize.QuadPart );
#else
      Stat* stbuf = file->stat();
      if ( stbuf != NULL )
      {
        current_file_size = stbuf->get_size();
        Stat::dec_ref( *stbuf );
      }
      else
        current_file_size = 0;
#endif
    }
    else
      current_file_size = 0;

    MemoryMappedFile* memory_mapped_file
      = new MemoryMappedFile( *file, flags );

    if ( memory_mapped_file->resize( max( minimum_size, current_file_size ) ) )
      return memory_mapped_file;
    else
    {
      delete memory_mapped_file;
      return NULL;
    }
  }
  else
    return NULL;
}

bool MemoryMappedFile::resize( size_t new_size )
{
  if ( new_size > 0 )
  {
#ifdef _WIN32
    if ( start != NULL )
    {
      if ( UnmapViewOfFile( start ) != TRUE )
        return false;
    }

    if ( mapping != NULL )
    {
      if ( CloseHandle( mapping ) != TRUE )
        return false;
    }
#else
    if ( start != NULL )
    {
      sync();
      if ( munmap( start, size() ) == -1 )
        return false;
    }
#endif

    if
    (
      size() == new_size
      ||
      underlying_file.truncate( new_size ) )
    {
#ifdef _WIN32
      unsigned long map_flags = PAGE_READONLY;
      if ( ( open_flags & O_RDWR ) == O_RDWR ||
           ( open_flags & O_WRONLY ) == O_WRONLY )
        map_flags = PAGE_READWRITE;

      ULARGE_INTEGER uliNewSize; uliNewSize.QuadPart = new_size;

      mapping = CreateFileMapping
                (
                  underlying_file,
                  NULL, map_flags,
                  uliNewSize.HighPart,
                  uliNewSize.LowPart,
                  NULL
                );

      if ( mapping != NULL )
      {
        map_flags = FILE_MAP_READ;
        if( ( open_flags & O_RDWR ) || ( open_flags & O_WRONLY ) )
          map_flags = FILE_MAP_ALL_ACCESS;

        start = static_cast<char*>( MapViewOfFile( mapping, map_flags, 0, 0, 0 ) );
        if ( start != NULL )
        {
      }
#else
      unsigned long mmap_flags = PROT_READ;
      if( ( open_flags & O_RDWR ) == O_RDWR ||
          ( open_flags & O_WRONLY ) == O_WRONLY )
        mmap_flags |= PROT_WRITE;

      void* mmap_ret = mmap
                       (
                         0,
                         new_size,
                         mmap_flags,
                         MAP_SHARED,
                         underlying_file,
                         0
                       );

      if ( mmap_ret != MAP_FAILED )
      {
        start = static_cast<char*>( mmap_ret );
#endif
        this->size_ = new_size;
        return true;
      }
    }
  }
  else
    return true;

  return false;
}

bool MemoryMappedFile::sync()
{
#ifdef _WIN32
  return sync
         (
           static_cast<size_t>( 0 ),
           static_cast<size_t>( 0 )
         ); // length 0 = flush to end of mapping
#else
  return sync( static_cast<size_t>( 0 ), size() );
#endif
}

bool MemoryMappedFile::sync( size_t offset, size_t length )
{
  return sync( start + offset, length );
}

bool MemoryMappedFile::sync( void* ptr, size_t length )
{
#if defined(_WIN32)
  return FlushViewOfFile( ptr, length ) == TRUE;
#elif defined(__sun)
  return msync( static_cast<char*>( ptr ), length, MS_SYNC ) == 0;
#else
  return msync( ptr, length, MS_SYNC ) == 0;
#endif
}


// mutex.cpp
#if defined(_WIN32)
#include <windows.h>
#elif defined(__linux__) || defined(__FreeBSD__) || defined(__sun)
#define YIELD_PLATFORM_HAVE_PTHREAD_MUTEX_TIMEDLOCK
#endif


Mutex::Mutex()
{
#ifdef _WIN32
  if ( ( hMutex = CreateEvent( NULL, FALSE, TRUE, NULL ) ) == NULL )
    DebugBreak();
#else
  if ( pthread_mutex_init( &pthread_mutex, NULL ) != 0 )
    DebugBreak();
#endif
}

Mutex::~Mutex()
{
#ifdef _WIN32
  if ( hMutex ) CloseHandle( hMutex );
#else
  pthread_mutex_destroy( &pthread_mutex );
#endif
}

bool Mutex::acquire()
{
#ifdef _WIN32
  DWORD dwRet = WaitForSingleObjectEx( hMutex, INFINITE, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#else
  pthread_mutex_lock( &pthread_mutex );
  return true;
#endif
}

bool Mutex::acquire( const Time& timeout )
{
#ifdef _WIN32
  DWORD timeout_ms = static_cast<DWORD>( timeout.as_unix_time_ms() );
  DWORD dwRet = WaitForSingleObjectEx( hMutex, timeout_ms, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#else
#ifdef YIELD_PLATFORM_HAVE_PTHREAD_MUTEX_TIMEDLOCK
  struct timespec timeout_ts = Time() + timeout;
  return ( pthread_mutex_timedlock( &pthread_mutex, &timeout_ts ) == 0 );
#else
  if ( pthread_mutex_trylock( &pthread_mutex ) == 0 )
    return true;
  else
  {
    usleep( timeout.as_unix_time_us() );
    return pthread_mutex_trylock( &pthread_mutex ) == 0;
  }
#endif
#endif
}

void Mutex::release()
{
#ifdef _WIN32
  SetEvent( hMutex );
#else
  pthread_mutex_unlock( &pthread_mutex );
#endif
}

bool Mutex::try_acquire()
{
#ifdef _WIN32
  DWORD dwRet = WaitForSingleObjectEx( hMutex, 0, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#else
  return pthread_mutex_trylock( &pthread_mutex ) == 0;
#endif
}


// named_pipe.cpp
#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif


NamedPipe* NamedPipe::open( const Path& path, uint32_t flags, mode_t mode )
{
#ifdef _WIN32
  Path named_pipe_base_dir_path( TEXT( "\\\\.\\pipe" ) );
  Path named_pipe_path( named_pipe_base_dir_path + path );

  if ( ( flags & O_CREAT ) == O_CREAT ) // Server
  {
    HANDLE hPipe
      = CreateNamedPipe
        (
          named_pipe_path,
          PIPE_ACCESS_DUPLEX|FILE_FLAG_OVERLAPPED,
          PIPE_TYPE_BYTE|PIPE_READMODE_BYTE|PIPE_WAIT,
          PIPE_UNLIMITED_INSTANCES,
          4096,
          4096,
          0,
          NULL
        );

    if ( hPipe != INVALID_HANDLE_VALUE )
      return new NamedPipe( hPipe, false );
  }
  else // Client
  {
    File* underlying_file = Volume().open( named_pipe_path, flags );
    if ( underlying_file != NULL )
    {
      fd_t fd;

      DuplicateHandle
      (
        GetCurrentProcess(),
        *underlying_file,
        GetCurrentProcess(),
        &fd,
        0,
        FALSE,
        DUPLICATE_SAME_ACCESS
      );

      NamedPipe* named_pipe = new NamedPipe( fd, true );

      File::dec_ref( *underlying_file );

      return named_pipe;
    }
  }
#else
  if ( ( flags & O_CREAT ) == O_CREAT )
  {
    if
    (
      ::mkfifo( path, mode ) != -1
      ||
      errno == EEXIST
    )
      flags ^= O_CREAT;
    else
      return NULL;
  }

  File* underlying_file = Volume().open( path, flags );
  if ( underlying_file != NULL )
  {
    NamedPipe* named_pipe = new NamedPipe( dup( *underlying_file ) );
    File::dec_ref( *underlying_file );
    return named_pipe;
  }
#endif

  return NULL;
}

#ifdef _WIN32
NamedPipe::NamedPipe( fd_t fd, bool connected )
  : File( fd ), connected( connected )
{ }
#else
NamedPipe::NamedPipe( fd_t fd )
  : File( fd )
{ }
#endif

#ifdef _WIN32
bool NamedPipe::connect()
{
  if ( connected )
    return true;
  else
  {
    if
    (
      ConnectNamedPipe( *this, NULL ) != 0
      ||
      GetLastError() == ERROR_PIPE_CONNECTED
    )
    {
      connected = true;
      return true;
    }
    else
      return false;
  }
}

ssize_t NamedPipe::read( void* buf, size_t buflen )
{
  if ( connect() )
    return File::read( buf, buflen );
  else
    return -1;
}

ssize_t NamedPipe::write( const void* buf, size_t buflen )
{
  if ( connect() )
    return File::write( buf, buflen );
  else
    return -1;
}
#endif

#ifdef _WIN32
#pragma warning( pop )
#endif


// nbio_queue.cpp
class NBIOQueue::WorkerThread : public Thread
{
public:
  ~WorkerThread()
  {
    FDEventPoller::dec_ref( fd_event_poller );
    SocketPair::dec_ref( submit_pipe );
  }

  static WorkerThread& create()
  {
    FDEventPoller& fd_event_poller = FDEventPoller::create();
    SocketPair& submit_pipe = SocketPair::create();

    if
    (
      submit_pipe.first().set_blocking_mode( false )
      &&
      fd_event_poller.associate( submit_pipe.first(), true, false )
    )
      return *new WorkerThread( fd_event_poller, submit_pipe );
    else
      throw Exception();
  }

  void submit( NBIOCB* nbiocb )
  {
    submit_pipe.second().write( &nbiocb, sizeof( nbiocb ) );
  }

  // Thread
  void run()
  {
    set_name( "NBIOQueue::WorkerThread" );

    FDEventPoller::FDEvent fd_events[64];

    for ( ;; )
    {
      int fd_events_count = fd_event_poller.poll( fd_events, 64 );
      if ( fd_events_count > 0 )
      {
        for ( int fd_event_i = 0; fd_event_i < fd_events_count; fd_event_i++ )
        {
          const FDEventPoller::FDEvent& fd_event = fd_events[fd_event_i];

          if ( fd_event.get_fd() == submit_pipe.first() )
          {
            // Read submitted NBIOCB's
            NBIOCB* nbiocb;
            for ( ;; )
            {
              ssize_t read_ret
                = submit_pipe.first().read( &nbiocb, sizeof( nbiocb ) );

              if ( read_ret == sizeof( nbiocb ) )
              {
                if ( nbiocb != NULL )
                {
                  switch ( nbiocb->get_state() )
                  {
                    case NBIOCB::STATE_WANT_CONNECT:
                    case NBIOCB::STATE_WANT_WRITE:
                    {
                      fd_event_poller.associate
                      (
                        nbiocb->get_fd(),
                        nbiocb,
                        false,
                        true
                      );
                    }
                    break;

                    case NBIOCB::STATE_WANT_READ:
                    {
                      fd_event_poller.associate
                      (
                        nbiocb->get_fd(),
                        nbiocb,
                        true
                      );
                    }
                    break;

                    default:
                    {
                      delete nbiocb;
                    }
                    break;
                  }
                }
                else // NULL nbiocb = the stop signal
                  return;
              }
              else if ( read_ret <= 0 )
                break;
              else
                DebugBreak();
            }
          }
          else
          {
            NBIOCB* nbiocb = static_cast<NBIOCB*>( fd_event.get_context() );

            nbiocb->execute();

            switch ( nbiocb->get_state() )
            {
              case NBIOCB::STATE_WANT_CONNECT:
              case NBIOCB::STATE_WANT_WRITE:
              {
                fd_event_poller.toggle( nbiocb->get_fd(), false, true );
              }
              break;

              case NBIOCB::STATE_WANT_READ:
              {
                fd_event_poller.toggle( nbiocb->get_fd(), true, false );
              }
              break;

              case NBIOCB::STATE_COMPLETE:
              case NBIOCB::STATE_ERROR:
              {
                fd_event_poller.dissociate( nbiocb->get_fd() );
                delete nbiocb;
              }
              break;
            }
          }
        }
      }
      else if ( fd_events_count < 0 )
      {
#ifndef _WIN32
        if ( errno != EINTR )
#endif
          cerr << "NBIOQueue::WorkerThread: " <<
            "error on poll: " << Exception() << "." << endl;
      }
    }
  }

private:
  WorkerThread( FDEventPoller& fd_event_poller, SocketPair& submit_pipe )
    : fd_event_poller( fd_event_poller ), submit_pipe( submit_pipe )
  { }

private:
  FDEventPoller& fd_event_poller;
  SocketPair& submit_pipe;
};


NBIOQueue::NBIOQueue( const vector<WorkerThread*>& worker_threads )
  : worker_threads( worker_threads )
{ }

NBIOQueue::~NBIOQueue()
{
  for
  (
    vector<WorkerThread*>::iterator
      worker_thread_i = worker_threads.begin();
    worker_thread_i != worker_threads.end();
    worker_thread_i++
  )
  {
    ( *worker_thread_i )->submit( NULL );
    ( *worker_thread_i )->join();
#ifndef _WIN32
    Thread::nanosleep( 10 * Time::NS_IN_MS );
#endif
    WorkerThread::dec_ref( **worker_thread_i );
  }
}

NBIOQueue& NBIOQueue::create()
{
  vector<WorkerThread*> worker_threads;
  uint16_t worker_thread_count
    = ProcessorSet::getOnlineLogicalProcessorCount();
  // uint16_t worker_thread_count = 1;
  for
  (
    uint16_t worker_thread_i = 0;
    worker_thread_i < worker_thread_count;
    worker_thread_i++
  )
  {
    WorkerThread& worker_thread = WorkerThread::create();
    worker_thread.start();
    worker_threads.push_back( &worker_thread );
  }

  return *new NBIOQueue( worker_threads );
}

void NBIOQueue::submit( NBIOCB& nbiocb )
{
  worker_threads[nbiocb.get_fd() % worker_threads.size()]->submit( &nbiocb );
}


// option_parser.cpp
#include <algorithm>
using std::sort;

/*! @file SimpleOpt.h

    Copyright (c) 2006-2007, Brodie Thiesfield

    Permission is hereby granted, free of charge, to any person obtaining a
    copy of this software and associated documentation files (the "Software"),
    to deal in the Software without restriction, including without limitation
    the rights to use, copy, modify, merge, publish, distribute, sublicense,
    and/or sell copies of the Software, and to permit persons to whom the
    Software is furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included
    in all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
    OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
    IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
    CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
    TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
    SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/


// Default the max arguments to a fixed value. If you want to be able to
// handle any number of arguments, then predefine this to 0 and it will
// use an internal dynamically allocated buffer instead.
#ifdef SO_MAX_ARGS
# define SO_STATICBUF   SO_MAX_ARGS
#else
# include <stdlib.h>    // malloc, free
# include <string.h>    // memcpy
# define SO_STATICBUF   50
#endif

//! Error values
typedef enum _ESOError
{
    //! No error
    SO_SUCCESS          =  0,

    /*! It looks like an option (it starts with a switch character), but
        it isn't registered in the option table. */
    SO_OPT_INVALID      = -1,

    /*! Multiple options matched the supplied option text.
        Only returned when NOT using SO_O_EXACT. */
    SO_OPT_MULTIPLE     = -2,

    /*! Option doesn't take an argument, but a combined argument was
        supplied. */
    SO_ARG_INVALID      = -3,

    /*! SO_REQ_CMB style-argument was supplied to a SO_REQ_SEP option
        Only returned when using SO_O_PEDANTIC. */
    SO_ARG_INVALID_TYPE = -4,

    //! Required argument was not supplied
    SO_ARG_MISSING      = -5,

    /*! Option argument looks like another option.
        Only returned when NOT using SO_O_NOERR. */
    SO_ARG_INVALID_DATA = -6
} ESOError;

//! Option flags
enum _ESOFlags
{
    /*! Disallow partial matching of option names */
    SO_O_EXACT       = 0x0001,

    /*! Disallow use of slash as an option marker on Windows.
        Un*x only ever recognizes a hyphen. */
    SO_O_NOSLASH     = 0x0002,

    /*! Permit arguments on single letter options with no equals sign.
        e.g. -oARG or -o[ARG] */
    SO_O_SHORTARG    = 0x0004,

    /*! Permit single character options to be clumped into a single
        option string. e.g. "-a -b -c" <==> "-abc" */
    SO_O_CLUMP       = 0x0008,

    /*! Process the entire argv array for options, including the
        argv[0] entry. */
    SO_O_USEALL      = 0x0010,

    /*! Do not generate an error for invalid options. errors for missing
        arguments will still be generated. invalid options will be
        treated as files. invalid options in clumps will be silently
        ignored. */
    SO_O_NOERR       = 0x0020,

    /*! Validate argument type pedantically. Return an error when a
        separated argument "-opt arg" is supplied by the user as a
        combined argument "-opt=arg". By default this is not considered
        an error. */
    SO_O_PEDANTIC    = 0x0040,

    /*! Case-insensitive comparisons for short arguments */
    SO_O_ICASE_SHORT = 0x0100,

    /*! Case-insensitive comparisons for long arguments */
    SO_O_ICASE_LONG  = 0x0200,

    /*! Case-insensitive comparisons for word arguments
        i.e. arguments without any hyphens at the start. */
    SO_O_ICASE_WORD  = 0x0400,

    /*! Case-insensitive comparisons for all arg types */
    SO_O_ICASE       = 0x0700
};

/*! Types of arguments that options may have. Note that some of the _ESOFlags
    are not compatible with all argument types. SO_O_SHORTARG requires that
    relevant options use either SO_REQ_CMB or SO_OPT. SO_O_CLUMP requires
    that relevant options use only SO_NONE.
 */
typedef enum _ESOArgType {
    /*! No argument. Just the option flags.
        e.g. -o         --opt */
    SO_NONE,

    /*! Required separate argument.
        e.g. -o ARG     --opt ARG */
    SO_REQ_SEP,

    /*! Required combined argument.
        e.g. -oARG      -o=ARG      --opt=ARG  */
    SO_REQ_CMB,

    /*! Optional combined argument.
        e.g. -o[ARG]    -o[=ARG]    --opt[=ARG] */
    SO_OPT,

    /*! Multiple separate arguments. The actual number of arguments is
        determined programatically at the time the argument is processed.
        e.g. -o N ARG1 ARG2 ... ARGN    --opt N ARG1 ARG2 ... ARGN */
    SO_MULTI
} ESOArgType;

//! this option definition must be the last entry in the table
#define SO_END_OF_OPTIONS   { -1, NULL, SO_NONE }

#ifdef _DEBUG
# ifdef _MSC_VER
#  include <crtdbg.h>
#  define SO_ASSERT(b)  _ASSERTE(b)
# else
#  include <assert.h>
#  define SO_ASSERT(b)  assert(b)
# endif
#else
# define SO_ASSERT(b)   //!< assertion used to test input data
#endif

// ---------------------------------------------------------------------------
//                              MAIN TEMPLATE CLASS
// ---------------------------------------------------------------------------

/*! @brief Implementation of the SimpleOpt class */
template<class SOCHAR>
class CSimpleOptTempl
{
public:
    /*! @brief Structure used to define all known options. */
    struct SOption {
        /*! ID to return for this flag. Optional but must be >= 0 */
        int nId;

        /*! arg string to search for, e.g.  "open", "-", "-f", "--file"
            Note that on Windows the slash option marker will be converted
            to a hyphen so that "-f" will also match "/f". */
        const SOCHAR * pszArg;

        /*! type of argument accepted by this option */
        ESOArgType nArgType;
    };

    /*! @brief Initialize the class. Init() must be called later. */
    CSimpleOptTempl()
        : m_rgShuffleBuf(NULL)
    {
        Init(0, NULL, NULL, 0);
    }

    /*! @brief Initialize the class in preparation for use. */
    CSimpleOptTempl(
        int             argc,
        SOCHAR *        argv[],
        const SOption * a_rgOptions,
        int             a_nFlags = 0
        )
        : m_rgShuffleBuf(NULL)
    {
        Init(argc, argv, a_rgOptions, a_nFlags);
    }

#ifndef SO_MAX_ARGS
    /*! @brief Deallocate any allocated memory. */
    ~CSimpleOptTempl() { if (m_rgShuffleBuf) free(m_rgShuffleBuf); }
#endif

    /*! @brief Initialize the class in preparation for calling Next.

        The table of options pointed to by a_rgOptions does not need to be
        valid at the time that Init() is called. However on every call to
        Next() the table pointed to must be a valid options table with the
        last valid entry set to SO_END_OF_OPTIONS.

        NOTE: the array pointed to by a_argv will be modified by this
        class and must not be used or modified outside of member calls to
        this class.

        @param a_argc       Argument array size
        @param a_argv       Argument array
        @param a_rgOptions  Valid option array
        @param a_nFlags     Optional flags to modify the processing of
                            the arguments

        @return true        Successful
        @return false       if SO_MAX_ARGC > 0:  Too many arguments
                            if SO_MAX_ARGC == 0: Memory allocation failure
    */
    bool Init(
        int             a_argc,
        SOCHAR *        a_argv[],
        const SOption * a_rgOptions,
        int             a_nFlags = 0
        );

    /*! @brief Change the current options table during option parsing.

        @param a_rgOptions  Valid option array
     */
    inline void SetOptions(const SOption * a_rgOptions) {
        m_rgOptions = a_rgOptions;
    }

    /*! @brief Change the current flags during option parsing.

        Note that changing the SO_O_USEALL flag here will have no affect.
        It must be set using Init() or the constructor.

        @param a_nFlags     Flags to modify the processing of the arguments
     */
    inline void SetFlags(int a_nFlags) { m_nFlags = a_nFlags; }

    /*! @brief Query if a particular flag is set */
    inline bool HasFlag(int a_nFlag) const {
        return (m_nFlags & a_nFlag) == a_nFlag;
    }

    /*! @brief Advance to the next option if available.

        When all options have been processed it will return false. When true
        has been returned, you must check for an invalid or unrecognized
        option using the LastError() method. This will be return an error
        value other than SO_SUCCESS on an error. All standard data
        (e.g. OptionText(), OptionArg(), OptionId(), etc) will be available
        depending on the error.

        After all options have been processed, the remaining files from the
        command line can be processed in same order as they were passed to
        the program.

        @return true    option or error available for processing
        @return false   all options have been processed
    */
    bool Next();

    /*! Stops processing of the command line and returns all remaining
        arguments as files. The next call to Next() will return false.
     */
    void Stop();

    /*! @brief Return the last error that occurred.

        This function must always be called before processing the current
        option. This function is available only when Next() has returned true.
     */
    inline ESOError LastError() const  { return m_nLastError; }

    /*! @brief Return the nId value from the options array for the current
        option.

        This function is available only when Next() has returned true.
     */
    inline int OptionId() const { return m_nOptionId; }

    /*! @brief Return the pszArg from the options array for the current
        option.

        This function is available only when Next() has returned true.
     */
    inline const SOCHAR * OptionText() const { return m_pszOptionText; }

    /*! @brief Return the argument for the current option where one exists.

        If there is no argument for the option, this will return NULL.
        This function is available only when Next() has returned true.
     */
    inline SOCHAR * OptionArg() const { return m_pszOptionArg; }

    /*! @brief Validate and return the desired number of arguments.

        This is only valid when OptionId() has return the ID of an option
        that is registered as SO_MULTI. It may be called multiple times
        each time returning the desired number of arguments. Previously
        returned argument pointers are remain valid.

        If an error occurs during processing, NULL will be returned and
        the error will be available via LastError().

        @param n    Number of arguments to return.
     */
    SOCHAR ** MultiArg(int n);

    /*! @brief Returned the number of entries in the Files() array.

        After Next() has returned false, this will be the list of files (or
        otherwise unprocessed arguments).
     */
    inline int FileCount() const { return m_argc - m_nLastArg; }

    /*! @brief Return the specified file argument.

        @param n    Index of the file to return. This must be between 0
                    and FileCount() - 1;
     */
    inline SOCHAR * File(int n) const {
        SO_ASSERT(n >= 0 && n < FileCount());
        return m_argv[m_nLastArg + n];
    }

    /*! @brief Return the array of files. */
    inline SOCHAR ** Files() const { return &m_argv[m_nLastArg]; }

private:
    CSimpleOptTempl(const CSimpleOptTempl &); // disabled
    CSimpleOptTempl & operator=(const CSimpleOptTempl &); // disabled

    SOCHAR PrepareArg(SOCHAR * a_pszString) const;
    bool NextClumped();
    void ShuffleArg(int a_nStartIdx, int a_nCount);
    int LookupOption(const SOCHAR * a_pszOption) const;
    int CalcMatch(const SOCHAR *a_pszSource, const SOCHAR *a_pszTest) const;

    // Find the '=' character within a string.
    inline SOCHAR * FindEquals(SOCHAR *s) const {
        while (*s && *s != (SOCHAR)'=') ++s;
        return *s ? s : NULL;
    }
    bool IsEqual(SOCHAR a_cLeft, SOCHAR a_cRight, int a_nArgType) const;

    inline void Copy(SOCHAR ** ppDst, SOCHAR ** ppSrc, int nCount) const {
#ifdef SO_MAX_ARGS
        // keep our promise of no CLIB usage
        while (nCount-- > 0) *ppDst++ = *ppSrc++;
#else
        memcpy(ppDst, ppSrc, nCount * sizeof(SOCHAR*));
#endif
    }

private:
    const SOption * m_rgOptions;     //!< pointer to options table
    int             m_nFlags;        //!< flags
    int             m_nOptionIdx;    //!< current argv option index
    int             m_nOptionId;     //!< id of current option (-1 = invalid)
    int             m_nNextOption;   //!< index of next option
    int             m_nLastArg;      //!< last argument, after this are files
    int             m_argc;          //!< argc to process
    SOCHAR **       m_argv;          //!< argv
    const SOCHAR *  m_pszOptionText; //!< curr option text, e.g. "-f"
    SOCHAR *        m_pszOptionArg;  //!< curr option arg, e.g. "c:\file.txt"
    SOCHAR *        m_pszClump;      //!< clumped single character options
    SOCHAR          m_szShort[3];    //!< temp for clump and combined args
    ESOError        m_nLastError;    //!< error status from the last call
    SOCHAR **       m_rgShuffleBuf;  //!< shuffle buffer for large argc
};

// ---------------------------------------------------------------------------
//                                  IMPLEMENTATION
// ---------------------------------------------------------------------------

template<class SOCHAR>
bool
CSimpleOptTempl<SOCHAR>::Init(
    int             a_argc,
    SOCHAR *        a_argv[],
    const SOption * a_rgOptions,
    int             a_nFlags
    )
{
    m_argc           = a_argc;
    m_nLastArg       = a_argc;
    m_argv           = a_argv;
    m_rgOptions      = a_rgOptions;
    m_nLastError     = SO_SUCCESS;
    m_nOptionIdx     = 0;
    m_nOptionId      = -1;
    m_pszOptionText  = NULL;
    m_pszOptionArg   = NULL;
    m_nNextOption    = (a_nFlags & SO_O_USEALL) ? 0 : 1;
    m_szShort[0]     = (SOCHAR)'-';
    m_szShort[2]     = (SOCHAR)'\0';
    m_nFlags         = a_nFlags;
    m_pszClump       = NULL;

#ifdef SO_MAX_ARGS
	if (m_argc > SO_MAX_ARGS) {
        m_nLastError = SO_ARG_INVALID_DATA;
        m_nLastArg = 0;
		return false;
	}
#else
    if (m_rgShuffleBuf) {
        free(m_rgShuffleBuf);
    }
    if (m_argc > SO_STATICBUF) {
        m_rgShuffleBuf = (SOCHAR**) malloc(sizeof(SOCHAR*) * m_argc);
        if (!m_rgShuffleBuf) {
            return false;
        }
    }
#endif

    return true;
}

template<class SOCHAR>
bool
CSimpleOptTempl<SOCHAR>::Next()
{
#ifdef SO_MAX_ARGS
    if (m_argc > SO_MAX_ARGS) {
        SO_ASSERT(!"Too many args! Check the return value of Init()!");
        return false;
    }
#endif

    // process a clumped option string if appropriate
    if (m_pszClump && *m_pszClump) {
        // silently discard invalid clumped option
        bool bIsValid = NextClumped();
        while (*m_pszClump && !bIsValid && HasFlag(SO_O_NOERR)) {
            bIsValid = NextClumped();
        }

        // return this option if valid or we are returning errors
        if (bIsValid || !HasFlag(SO_O_NOERR)) {
            return true;
        }
    }
    SO_ASSERT(!m_pszClump || !*m_pszClump);
    m_pszClump = NULL;

    // init for the next option
    m_nOptionIdx    = m_nNextOption;
    m_nOptionId     = -1;
    m_pszOptionText = NULL;
    m_pszOptionArg  = NULL;
    m_nLastError    = SO_SUCCESS;

    // find the next option
    SOCHAR cFirst;
    int nTableIdx = -1;
    int nOptIdx = m_nOptionIdx;
    while (nTableIdx < 0 && nOptIdx < m_nLastArg) {
        SOCHAR * pszArg = m_argv[nOptIdx];
        m_pszOptionArg  = NULL;

        // find this option in the options table
        cFirst = PrepareArg(pszArg);
        if (pszArg[0] == (SOCHAR)'-') {
            // find any combined argument string and remove equals sign
            m_pszOptionArg = FindEquals(pszArg);
            if (m_pszOptionArg) {
                *m_pszOptionArg++ = (SOCHAR)'\0';
            }
        }
        nTableIdx = LookupOption(pszArg);

        // if we didn't find this option but if it is a short form
        // option then we try the alternative forms
        if (nTableIdx < 0
            && !m_pszOptionArg
            && pszArg[0] == (SOCHAR)'-'
            && pszArg[1]
            && pszArg[1] != (SOCHAR)'-'
            && pszArg[2])
        {
            // test for a short-form with argument if appropriate
            if (HasFlag(SO_O_SHORTARG)) {
                m_szShort[1] = pszArg[1];
                int nIdx = LookupOption(m_szShort);
                if (nIdx >= 0
                    && (m_rgOptions[nIdx].nArgType == SO_REQ_CMB
                        || m_rgOptions[nIdx].nArgType == SO_OPT))
                {
                    m_pszOptionArg = &pszArg[2];
                    pszArg         = m_szShort;
                    nTableIdx      = nIdx;
                }
            }

            // test for a clumped short-form option string and we didn't
            // match on the short-form argument above
            if (nTableIdx < 0 && HasFlag(SO_O_CLUMP))  {
                m_pszClump = &pszArg[1];
                ++m_nNextOption;
                if (nOptIdx > m_nOptionIdx) {
                    ShuffleArg(m_nOptionIdx, nOptIdx - m_nOptionIdx);
                }
                return Next();
            }
        }

        // The option wasn't found. If it starts with a switch character
        // and we are not suppressing errors for invalid options then it
        // is reported as an error, otherwise it is data.
        if (nTableIdx < 0) {
            if (!HasFlag(SO_O_NOERR) && pszArg[0] == (SOCHAR)'-') {
                m_pszOptionText = pszArg;
                break;
            }

            pszArg[0] = cFirst;
            ++nOptIdx;
            if (m_pszOptionArg) {
                *(--m_pszOptionArg) = (SOCHAR)'=';
            }
        }
    }

    // end of options
    if (nOptIdx >= m_nLastArg) {
        if (nOptIdx > m_nOptionIdx) {
            ShuffleArg(m_nOptionIdx, nOptIdx - m_nOptionIdx);
        }
        return false;
    }
    ++m_nNextOption;

    // get the option id
    ESOArgType nArgType = SO_NONE;
    if (nTableIdx < 0) {
        m_nLastError    = (ESOError) nTableIdx; // error code
    }
    else {
        m_nOptionId     = m_rgOptions[nTableIdx].nId;
        m_pszOptionText = m_rgOptions[nTableIdx].pszArg;

        // ensure that the arg type is valid
        nArgType = m_rgOptions[nTableIdx].nArgType;
        switch (nArgType) {
        case SO_NONE:
            if (m_pszOptionArg) {
                m_nLastError = SO_ARG_INVALID;
            }
            break;

        case SO_REQ_SEP:
            if (m_pszOptionArg) {
                // they wanted separate args, but we got a combined one,
                // unless we are pedantic, just accept it.
                if (HasFlag(SO_O_PEDANTIC)) {
                    m_nLastError = SO_ARG_INVALID_TYPE;
                }
            }
            // more processing after we shuffle
            break;

        case SO_REQ_CMB:
            if (!m_pszOptionArg) {
                m_nLastError = SO_ARG_MISSING;
            }
            break;

        case SO_OPT:
            // nothing to do
            break;

        case SO_MULTI:
            // nothing to do. Caller must now check for valid arguments
            // using GetMultiArg()
            break;
        }
    }

    // shuffle the files out of the way
    if (nOptIdx > m_nOptionIdx) {
        ShuffleArg(m_nOptionIdx, nOptIdx - m_nOptionIdx);
    }

    // we need to return the separate arg if required, just re-use the
    // multi-arg code because it all does the same thing
    if (   nArgType == SO_REQ_SEP
        && !m_pszOptionArg
        && m_nLastError == SO_SUCCESS)
    {
        SOCHAR ** ppArgs = MultiArg(1);
        if (ppArgs) {
            m_pszOptionArg = *ppArgs;
        }
    }

    return true;
}

template<class SOCHAR>
void
CSimpleOptTempl<SOCHAR>::Stop()
{
    if (m_nNextOption < m_nLastArg) {
        ShuffleArg(m_nNextOption, m_nLastArg - m_nNextOption);
    }
}

template<class SOCHAR>
SOCHAR
CSimpleOptTempl<SOCHAR>::PrepareArg(
    SOCHAR * a_pszString
    ) const
{
#ifdef _WIN32
    // On Windows we can accept the forward slash as a single character
    // option delimiter, but it cannot replace the '-' option used to
    // denote stdin. On Un*x paths may start with slash so it may not
    // be used to start an option.
    if (!HasFlag(SO_O_NOSLASH)
        && a_pszString[0] == (SOCHAR)'/'
        && a_pszString[1]
        && a_pszString[1] != (SOCHAR)'-')
    {
        a_pszString[0] = (SOCHAR)'-';
        return (SOCHAR)'/';
    }
#endif
    return a_pszString[0];
}

template<class SOCHAR>
bool
CSimpleOptTempl<SOCHAR>::NextClumped()
{
    // prepare for the next clumped option
    m_szShort[1]    = *m_pszClump++;
    m_nOptionId     = -1;
    m_pszOptionText = NULL;
    m_pszOptionArg  = NULL;
    m_nLastError    = SO_SUCCESS;

    // lookup this option, ensure that we are using exact matching
    int nSavedFlags = m_nFlags;
    m_nFlags = SO_O_EXACT;
    int nTableIdx = LookupOption(m_szShort);
    m_nFlags = nSavedFlags;

    // unknown option
    if (nTableIdx < 0) {
        m_nLastError = (ESOError) nTableIdx; // error code
        return false;
    }

    // valid option
    m_pszOptionText = m_rgOptions[nTableIdx].pszArg;
    ESOArgType nArgType = m_rgOptions[nTableIdx].nArgType;
    if (nArgType == SO_NONE) {
        m_nOptionId = m_rgOptions[nTableIdx].nId;
        return true;
    }

    if (nArgType == SO_REQ_CMB && *m_pszClump) {
        m_nOptionId = m_rgOptions[nTableIdx].nId;
        m_pszOptionArg = m_pszClump;
        while (*m_pszClump) ++m_pszClump; // must point to an empty string
        return true;
    }

    // invalid option as it requires an argument
    m_nLastError = SO_ARG_MISSING;
    return true;
}

// Shuffle arguments to the end of the argv array.
//
// For example:
//      argv[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8" };
//
//  ShuffleArg(1, 1) = { "0", "2", "3", "4", "5", "6", "7", "8", "1" };
//  ShuffleArg(5, 2) = { "0", "1", "2", "3", "4", "7", "8", "5", "6" };
//  ShuffleArg(2, 4) = { "0", "1", "6", "7", "8", "2", "3", "4", "5" };
template<class SOCHAR>
void
CSimpleOptTempl<SOCHAR>::ShuffleArg(
    int a_nStartIdx,
    int a_nCount
    )
{
    SOCHAR * staticBuf[SO_STATICBUF];
    SOCHAR ** buf = m_rgShuffleBuf ? m_rgShuffleBuf : staticBuf;
    int nTail = m_argc - a_nStartIdx - a_nCount;

    // make a copy of the elements to be moved
    Copy(buf, m_argv + a_nStartIdx, a_nCount);

    // move the tail down
    Copy(m_argv + a_nStartIdx, m_argv + a_nStartIdx + a_nCount, nTail);

    // append the moved elements to the tail
    Copy(m_argv + a_nStartIdx + nTail, buf, a_nCount);

    // update the index of the last unshuffled arg
    m_nLastArg -= a_nCount;
}

// match on the long format strings. partial matches will be
// accepted only if that feature is enabled.
template<class SOCHAR>
int
CSimpleOptTempl<SOCHAR>::LookupOption(
    const SOCHAR * a_pszOption
    ) const
{
    int nBestMatch = -1;    // index of best match so far
    int nBestMatchLen = 0;  // matching characters of best match
    int nLastMatchLen = 0;  // matching characters of last best match

    for (int n = 0; m_rgOptions[n].nId >= 0; ++n) {
        // the option table must use hyphens as the option character,
        // the slash character is converted to a hyphen for testing.
        SO_ASSERT(m_rgOptions[n].pszArg[0] != (SOCHAR)'/');

        int nMatchLen = CalcMatch(m_rgOptions[n].pszArg, a_pszOption);
        if (nMatchLen == -1) {
            return n;
        }
        if (nMatchLen > 0 && nMatchLen >= nBestMatchLen) {
            nLastMatchLen = nBestMatchLen;
            nBestMatchLen = nMatchLen;
            nBestMatch = n;
        }
    }

    // only partial matches or no match gets to here, ensure that we
    // don't return a partial match unless it is a clear winner
    if (HasFlag(SO_O_EXACT) || nBestMatch == -1) {
        return SO_OPT_INVALID;
    }
    return (nBestMatchLen > nLastMatchLen) ? nBestMatch : SO_OPT_MULTIPLE;
}

// calculate the number of characters that match (case-sensitive)
// 0 = no match, > 0 == number of characters, -1 == perfect match
template<class SOCHAR>
int
CSimpleOptTempl<SOCHAR>::CalcMatch(
    const SOCHAR *  a_pszSource,
    const SOCHAR *  a_pszTest
    ) const
{
    if (!a_pszSource || !a_pszTest) {
        return 0;
    }

    // determine the argument type
    int nArgType = SO_O_ICASE_LONG;
    if (a_pszSource[0] != '-') {
        nArgType = SO_O_ICASE_WORD;
    }
    else if (a_pszSource[1] != '-' && !a_pszSource[2]) {
        nArgType = SO_O_ICASE_SHORT;
    }

    // match and skip leading hyphens
    while (*a_pszSource == (SOCHAR)'-' && *a_pszSource == *a_pszTest) {
        ++a_pszSource;
        ++a_pszTest;
    }
    if (*a_pszSource == (SOCHAR)'-' || *a_pszTest == (SOCHAR)'-') {
        return 0;
    }

    // find matching number of characters in the strings
    int nLen = 0;
    while (*a_pszSource && IsEqual(*a_pszSource, *a_pszTest, nArgType)) {
        ++a_pszSource;
        ++a_pszTest;
        ++nLen;
    }

    // if we have exhausted the source...
    if (!*a_pszSource) {
        // and the test strings, then it's a perfect match
        if (!*a_pszTest) {
            return -1;
        }

        // otherwise the match failed as the test is longer than
        // the source. i.e. "--mant" will not match the option "--man".
        return 0;
    }

    // if we haven't exhausted the test string then it is not a match
    // i.e. "--mantle" will not best-fit match to "--mandate" at all.
    if (*a_pszTest) {
        return 0;
    }

    // partial match to the current length of the test string
    return nLen;
}

template<class SOCHAR>
bool
CSimpleOptTempl<SOCHAR>::IsEqual(
    SOCHAR  a_cLeft,
    SOCHAR  a_cRight,
    int     a_nArgType
    ) const
{
    // if this matches then we are doing case-insensitive matching
    if (m_nFlags & a_nArgType) {
        if (a_cLeft  >= 'A' && a_cLeft  <= 'Z') a_cLeft  += 'a' - 'A';
        if (a_cRight >= 'A' && a_cRight <= 'Z') a_cRight += 'a' - 'A';
    }
    return a_cLeft == a_cRight;
}

// calculate the number of characters that match (case-sensitive)
// 0 = no match, > 0 == number of characters, -1 == perfect match
template<class SOCHAR>
SOCHAR **
CSimpleOptTempl<SOCHAR>::MultiArg(
    int a_nCount
    )
{
    // ensure we have enough arguments
    if (m_nNextOption + a_nCount > m_nLastArg) {
        m_nLastError = SO_ARG_MISSING;
        return NULL;
    }

    // our argument array
    SOCHAR ** rgpszArg = &m_argv[m_nNextOption];

    // Ensure that each of the following don't start with an switch character.
    // Only make this check if we are returning errors for unknown arguments.
    if (!HasFlag(SO_O_NOERR)) {
        for (int n = 0; n < a_nCount; ++n) {
            SOCHAR ch = PrepareArg(rgpszArg[n]);
            if (rgpszArg[n][0] == (SOCHAR)'-') {
                rgpszArg[n][0] = ch;
                m_nLastError = SO_ARG_INVALID_DATA;
                return NULL;
            }
            rgpszArg[n][0] = ch;
        }
    }

    // all good
    m_nNextOption += a_nCount;
    return rgpszArg;
}


// ---------------------------------------------------------------------------
//                                  TYPE DEFINITIONS
// ---------------------------------------------------------------------------

/*! @brief ASCII/MBCS version of CSimpleOpt */
typedef CSimpleOptTempl<char>    CSimpleOptA;

/*! @brief wchar_t version of CSimpleOpt */
typedef CSimpleOptTempl<wchar_t> CSimpleOptW;

#if defined(_UNICODE)
/*! @brief TCHAR version dependent on if _UNICODE is defined */
# define CSimpleOpt CSimpleOptW
#else
/*! @brief TCHAR version dependent on if _UNICODE is defined */
# define CSimpleOpt CSimpleOptA
#endif

/* end SimpleOpt.h */


void
OptionParser::add_option
(
  const string& option,
  const string& help,
  bool require_argument
)
{
  options.add( option, help, require_argument );
}

void OptionParser::add_option( const Option& option )
{
  options.add( option );
}

void OptionParser::add_options( const Options& options )
{
  this->options.add( options );
}

void
OptionParser::parse_args
(
  int argc,
  char** argv,
  vector<ParsedOption>& out_parsed_options,
  vector<string>& out_positional_arguments
)
{
  vector<CSimpleOpt::SOption> simpleopt_options;

  for
  (
    vector<Option>::size_type option_i = 0;
    option_i < options.size();
    option_i++
  )
  {
    CSimpleOpt::SOption simpleopt_option
      =
      {
        option_i,
        options[option_i],
        options[option_i].get_require_argument() ? SO_REQ_SEP : SO_NONE
      };

    simpleopt_options.push_back( simpleopt_option );
  }

  CSimpleOpt::SOption sentinel_simpleopt_option = SO_END_OF_OPTIONS;
  simpleopt_options.push_back( sentinel_simpleopt_option );

  // Make copies of the strings in argv so that
  // SimpleOpt can punch holes in them
  vector<char*> argvv( argc );
  for ( int arg_i = 0; arg_i < argc; arg_i++ )
  {
    size_t arg_len = strnlen( argv[arg_i], SIZE_MAX ) + 1;
    argvv[arg_i] = new char[arg_len];
    memcpy_s( argvv[arg_i], arg_len, argv[arg_i], arg_len );
  }

  CSimpleOpt args( argc, &argvv[0], &simpleopt_options[0] );

  while ( args.Next() )
  {
    switch ( args.LastError() )
    {
      case SO_SUCCESS:
      {
        for
        (
          vector<Option>::iterator option_i = options.begin();
          option_i != options.end();
          ++option_i
        )
        {
          Option& option = *option_i;

          if ( option == args.OptionText() )
          {
            if ( option.get_require_argument() )
            {
              out_parsed_options.push_back
              (
                ParsedOption( option, args.OptionArg() )
              );
            }
            else
              out_parsed_options.push_back( ParsedOption( option ) );
          }
        }
      }
      break;

      case SO_OPT_INVALID:
      {
        string error_message( "unregistered option " );
        error_message.append( args.OptionText() );
        throw UnregisteredOptionException( error_message );
      }
      break;

      case SO_ARG_INVALID:
      {
        string error_message( "unexpected value to option " );
        error_message.append( args.OptionText() );
        throw UnexpectedValueException( error_message );
      }
      break;

      case SO_ARG_MISSING:
      {
        string error_message( "missing value to option " );
        error_message.append( args.OptionText() );
        throw MissingValueException( error_message );
      }
      break;

      case SO_ARG_INVALID_DATA: // Argument looks like another option
      {
        ostringstream error_message;
        error_message << args.OptionText() <<
          "requires a value, but you appear to have passed another option.";
        throw InvalidValueException( error_message.str() );
      }
      break;

      default:
      {
        DebugBreak();
      }
      break;
    }
  }

  for ( int arg_i = argc - args.FileCount(); arg_i < argc; arg_i++ )
    out_positional_arguments.push_back( argv[arg_i] );

  for
  (
    vector<char*>::iterator arg_i = argvv.begin();
    arg_i != argvv.end();
    arg_i++
  )
    delete [] *arg_i;
  argvv.clear();
}

string OptionParser::usage()
{
  ostringstream usage;

  usage << "Options:" << endl;

  sort( options.begin(), options.end() );
  for
  (
    vector<Option>::const_iterator option_i = options.begin();
    option_i != options.end();
    option_i++
  )
  {
    const Option& option = *option_i;
    usage << "  " << option;
    if ( !option.get_help().empty() )
      usage << "\t" << option.get_help();
    usage << endl;
  }

  usage << endl;

  return usage.str();
}


OptionParser::Option::Option
(
  const string& option,
  const string& help,
  bool require_argument
)
  : option( option ), help( help ), require_argument( require_argument )
{ }

bool OptionParser::Option::operator==( const string& option ) const
{
  return this->option == option;
}

bool OptionParser::Option::operator==( const char* option ) const
{
  return this->option == option;
}

bool OptionParser::Option::operator==( const Option& other ) const
{
  return this->option == other.option;
}

bool OptionParser::Option::operator<( const Option& other ) const
{
  return option.compare( other.option ) < 0;
}


void
OptionParser::Options::add
(
  const string& option,
  const string& help,
  bool require_argument
)
{
  add( Option( option, help, require_argument ) );
}

void OptionParser::Options::add( const Option& option )
{
  for ( const_iterator i = begin(); i != end(); ++i )
  {
    if ( *i == option )
      return;
  }

  push_back( option );
}

void OptionParser::Options::add( const Options& options )
{
  for ( const_iterator i = options.begin(); i != options.end(); ++i )
    add( *i );
}


OptionParser::ParsedOption::ParsedOption( Option& option )
  : Option( option )
{ }

OptionParser::ParsedOption::ParsedOption( Option& option, const string& arg )
  : Option( option ), argument( arg )
{ }


// ostream.cpp
void
OStream::aio_write
(
  Buffer& buffer,
  AIOWriteCallback& callback,
  void* callback_context
)
{
  ssize_t write_ret = write( buffer );
  if ( write_ret >= 0 )
  {
#ifdef _DEBUG
    if ( static_cast<size_t>( write_ret ) != buffer.size() )
      DebugBreak();
#endif
    callback.onWriteCompletion( callback_context );
  }
  else
#ifdef _WIN32
    callback.onWriteError( GetLastError(), callback_context );
#else
    callback.onWriteError( errno, callback_context );
#endif

  Buffer::dec_ref( buffer );
}

void
OStream::aio_writev
(
  Buffers& buffers,
  AIOWriteCallback& callback,
  void* callback_context
)
{
  ssize_t writev_ret = writev( buffers );
  if ( writev_ret >= 0 )
    callback.onWriteCompletion( callback_context );
  else
#ifdef _WIN32
    callback.onWriteError( GetLastError(), callback_context );
#else
    callback.onWriteError( errno, callback_context );
#endif
  Buffer::dec_ref( buffers );
}

ssize_t OStream::write( const Buffer& buffer )
{
  return write( static_cast<void*>( buffer ), buffer.size() );
}

ssize_t OStream::writev( const Buffers& buffers )
{
  return writev( buffers, buffers.size() );
}

ssize_t OStream::writev( const struct iovec* iov, uint32_t iovlen )
{
  if ( iovlen == 1 )
    return write( iov[0].iov_base, iov[0].iov_len );
  else
  {
    string buffer;
    for ( uint32_t iov_i = 0; iov_i < iovlen; iov_i++ )
    {
      buffer.append
      (
        static_cast<const char*>( iov[iov_i].iov_base ),
        iov[iov_i].iov_len
      );
    }

    return write( buffer.c_str(), buffer.size() );
  }
}


// path.cpp
#ifdef _WIN32
#include <windows.h>
#else
#include <stdlib.h> // For realpath
#endif


Path::Path( char narrow_path, iconv::Code narrow_path_code )
{
  init( &narrow_path, 1, narrow_path_code );
}

Path::Path( const char* narrow_path, iconv::Code narrow_path_code )
{
  init( narrow_path, strlen( narrow_path ), narrow_path_code );
}

Path::Path
(
  const char* narrow_path,
  size_t narrow_path_len,
  iconv::Code narrow_path_code
)
{
  init( narrow_path, narrow_path_len, narrow_path_code );
}

Path::Path( const string& narrow_path, iconv::Code narrow_path_code )
{
  init( narrow_path.c_str(), narrow_path.size(), narrow_path_code );
}

#ifdef _WIN32
Path::Path( wchar_t wide_path )
  : path( 1, wide_path )
{ }

Path::Path( const wchar_t* wide_path )
  : path( wide_path )
{ }

Path::Path( const wchar_t* wide_path, size_t wide_path_len )
  : path( wide_path, wide_path_len )
{ }

Path::Path( const wstring& wide_path )
  : path( wide_path )
{ }
#endif

Path::Path( const Path& path )
  : path( path.path )
{ }

Path Path::abspath() const
{
  string_type::value_type abspath_[PATH_MAX];
#ifdef _WIN32
  DWORD abspath__len
    = GetFullPathNameW
      (
        *this,
        PATH_MAX,
        abspath_,
        NULL
      );
  return Path( abspath_, abspath__len );
#else
  realpath( *this, abspath_ );
  return Path( abspath_ );
#endif
}

string Path::encode( iconv::Code tocode ) const
{
#ifdef _WIN32
  char narrow_path[PATH_MAX];

  int narrow_path_len
    = WideCharToMultiByte
      (
        iconv::Code_to_win32_code_page( tocode ),
        0,
        *this,
        ( int )size(),
        narrow_path,
        PATH_MAX,
        0,
        0
      );

  return string( narrow_path, narrow_path_len );
#else
  if ( tocode == iconv::CODE_CHAR )
    return path;
  else
  {
    iconv* iconv = iconv::open( tocode, iconv::CODE_CHAR );
    if ( iconv != NULL )
    {
      string encoded_path;
      ( *iconv )( path, encoded_path );
      delete iconv;
      return encoded_path;
    }
    else
    {
      DebugBreak();
      return path;
    }
  }
#endif
}

void Path::init
(
  const char* narrow_path,
  size_t narrow_path_len,
  iconv::Code narrow_path_code
)
{
#ifdef _WIN32
  wchar_t wide_path[PATH_MAX];
  this->path.assign
  (
    wide_path,
    MultiByteToWideChar
    (
      iconv::Code_to_win32_code_page( narrow_path_code ),
      0,
      narrow_path,
      static_cast<int>( narrow_path_len ),
      wide_path,
      PATH_MAX
    )
  );
#else
  if ( narrow_path_code == iconv::CODE_CHAR )
    this->path.assign( narrow_path, narrow_path_len );
  else
  {
    iconv* iconv = iconv::open( iconv::CODE_CHAR, narrow_path_code );
    if ( iconv != NULL )
    {
      if ( ( *iconv )( string( narrow_path, narrow_path_len ), path ) )
      {
        delete iconv;
        return;
      }
      else
        DebugBreak();
    }
    else
      DebugBreak();
  }
#endif
}

Path::string_type::value_type Path::operator[]( string_type::size_type i )const
{
  return path[i];
}

bool Path::operator==( const Path& path ) const
{
  return this->path == path.path;
}

bool Path::operator==( const string_type& path ) const
{
  return this->path == path;
}

bool Path::operator==( string_type::value_type path ) const
{
  return this->path.size() == 1 && this->path[0] == path;
}

bool Path::operator==( const string_type::value_type* path ) const
{
  return this->path == path;
}

bool Path::operator!=( const Path& path ) const
{
  return this->path != path.path;
}

bool Path::operator!=( const string_type& path ) const
{
  return this->path != path;
}

bool Path::operator!=( string_type::value_type path ) const
{
  return this->path.size() != 1 || this->path[0] != path;
}

bool Path::operator!=( const string_type::value_type* path ) const
{
  return this->path != path;
}

bool Path::operator<( const Path& path ) const
{
  return this->path.compare( path.path ) < 0;
}

Path Path::operator+( const Path& path ) const
{
  return operator+( path.path );
}

Path Path::operator+( const string_type& path ) const
{
  string_type combined_path( this->path );
  combined_path.append( path );
  return Path( combined_path );
}

Path Path::operator+( string_type::value_type path ) const
{
  string_type combined_path( this->path );
  combined_path.append( path, 1 );
  return Path( combined_path );
}

Path Path::operator+( const string_type::value_type* path ) const
{
  string_type combined_path( this->path );
  combined_path.append( path );
  return Path( combined_path );
}

Path Path::parent_path() const
{
  if ( *this != SEPARATOR )
  {
    vector<Path> parts;
    splitall( parts );
    if ( parts.size() > 1 )
      return parts[parts.size()-2];
    else
      return Path( SEPARATOR );
  }
  else
    return Path( SEPARATOR );
}

Path Path::root_path() const
{
#ifdef _WIN32
  vector<Path> path_parts;
  abspath().splitall( path_parts );
  return path_parts[0] + SEPARATOR;
#else
  return Path( "/" );
#endif
}

pair<Path, Path> Path::split() const
{
  string_type::size_type sep = path.find_last_of( SEPARATOR );
  if ( sep != string_type::npos )
    return make_pair( path.substr( 0, sep ), path.substr( sep + 1 ) );
  else
    return make_pair( Path(), *this );
}

void Path::splitall( vector<Path>& parts ) const
{
  string_type::size_type last_sep = path.find_first_not_of( SEPARATOR, 0 );
  string_type::size_type next_sep = path.find_first_of( SEPARATOR, last_sep );

  while ( next_sep != string_type::npos || last_sep != string_type::npos )
  {
    parts.push_back( path.substr( last_sep, next_sep - last_sep ) );
    last_sep = path.find_first_not_of( SEPARATOR, next_sep );
    next_sep = path.find_first_of( SEPARATOR, last_sep );
  }
}

pair<Path, Path> Path::splitext() const
{
  string_type::size_type last_dot;
#ifdef _WIN32
  last_dot = path.find_last_of( L"." );
#else
  last_dot = path.find_last_of( "." );
#endif

  if ( last_dot == 0 || last_dot == string_type::npos )
    return make_pair( *this, Path() );
  else
    return make_pair
           (
             path.substr( 0, last_dot ),
             path.substr( last_dot )
           );
}


// performance_counter_set.cpp
#ifdef YIELD_PLATFORM_HAVE_PERFORMANCE_COUNTERS

#if defined(__sun)
#include <libcpc.h>
#elif defined(YIELD_PLATFORM_HAVE_PAPI)
#include <papi.h>
#include <pthread.h>
#endif


PerformanceCounterSet* PerformanceCounterSet::create()
{
#if defined(__sun)
  cpc_t* cpc = cpc_open( CPC_VER_CURRENT );
  if ( cpc != NULL )
  {
    cpc_set_t* cpc_set = cpc_set_create( cpc );
    if ( cpc_set != NULL )
      return new PerformanceCounterSet( cpc, cpc_set );
    else
      cpc_close( cpc );
  }
#elif defined(YIELD_PLATFORM_HAVE_PAPI)
  if ( PAPI_library_init( PAPI_VER_CURRENT ) == PAPI_VER_CURRENT )
  {
    if ( PAPI_thread_init( pthread_self ) == PAPI_OK )
    {
      int papi_eventset = PAPI_NULL;
      if ( PAPI_create_eventset( &papi_eventset ) == PAPI_OK )
        return new PerformanceCounterSet( papi_eventset );
    }
  }
#endif

  return NULL;
}

#if defined(__sun)
PerformanceCounterSet::PerformanceCounterSet( cpc_t* cpc, cpc_set_t* cpc_set )
  : cpc( cpc ), cpc_set( cpc_set )
{
  start_cpc_buf = NULL;
}
#elif defined(YIELD_PLATFORM_HAVE_PAPI)
PerformanceCounterSet::PerformanceCounterSet( int papi_eventset )
  : papi_eventset( papi_eventset )
{ }
#endif

PerformanceCounterSet::~PerformanceCounterSet()
{
#if defined(__sun)
  cpc_set_destroy( cpc, cpc_set );
  cpc_close( cpc );
#elif defined(YIELD_PLATFORM_HAVE_PAPI)
  PAPI_cleanup_eventset( papi_eventset );
  PAPI_destroy_eventset( &papi_eventset );
#endif
}

bool PerformanceCounterSet::addEvent( Event event )
{
#if defined(__sun)
  switch ( event )
  {
    case EVENT_L1_DCM: return addEvent( "DC_miss" );
    case EVENT_L2_DCM: return addEvent( "DC_refill_from_system" );
    case EVENT_L2_ICM: return addEvent( "IC_refill_from_system" );
    default: DebugBreak(); return false;
  }
#elif defined(YIELD_PLATFORM_HAVE_PAPI)
  switch ( event )
  {
    case EVENT_L1_DCM: return addEvent( "PAPI_l1_dcm" );
    case EVENT_L2_DCM: return addEvent( "PAPI_l2_dcm" );
    case EVENT_L2_ICM: return addEvent( "PAPI_l2_icm" );
    default: DebugBreak(); return false;
  }
#endif
}

bool PerformanceCounterSet::addEvent( const char* name )
{
#if defined(__sun)
  int event_index
    = cpc_set_add_request( cpc, cpc_set, name, 0, CPC_COUNT_USER, 0, NULL );

  if ( event_index != -1 )
  {
    event_indices.push_back( event_index );
    return true;
  }
#elif defined(YIELD_PLATFORM_HAVE_PAPI)
  int papi_event_code;
  if
  (
    PAPI_event_name_to_code
    (
      const_cast<char*>( name ),
      &papi_event_code
    ) == PAPI_OK
  )
  {
     if ( PAPI_add_event( papi_eventset, papi_event_code ) == PAPI_OK )
       return true;
  }
#endif

  return false;
}

void PerformanceCounterSet::startCounting()
{
#if defined(__sun)
  if ( start_cpc_buf == NULL )
    start_cpc_buf = cpc_buf_create( cpc, cpc_set );
  cpc_bind_curlwp( cpc, cpc_set, 0 );
  cpc_set_sample( cpc, cpc_set, start_cpc_buf );
#elif defined(YIELD_PLATFORM_HAVE_PAPI)
  PAPI_start( papi_eventset );
#endif
}

void PerformanceCounterSet::stopCounting( uint64_t* counts )
{
#if defined(__sun)
  cpc_buf_t* stop_cpc_buf = cpc_buf_create( cpc, cpc_set );
  cpc_set_sample( cpc, cpc_set, stop_cpc_buf );

  cpc_buf_t* diff_cpc_buf = cpc_buf_create( cpc, cpc_set );
  cpc_buf_sub( cpc, diff_cpc_buf, stop_cpc_buf, start_cpc_buf );

  for
  (
    vector<int>::size_type event_index_i = 0;
    event_index_i < event_indices.size();
    event_index_i++
  )
  {
    cpc_buf_get
    (
      cpc,
      diff_cpc_buf,
      event_indices[event_index_i],
      &counts[event_index_i]
    );
  }

  cpc_unbind( cpc, cpc_set );
#elif defined(YIELD_PLATFORM_HAVE_PAPI)
  PAPI_stop( papi_eventset, reinterpret_cast<long long int*>( counts ) );
#endif
}

#endif


// pipe.cpp
#ifdef _WIN32
#include <windows.h>
#endif


Pipe::Pipe( fd_t ends[2] )
{
  this->ends[0] = ends[0];
  this->ends[1] = ends[1];
}

Pipe::~Pipe()
{
  close();
}

bool Pipe::close()
{
  if
  (
    Stream::close( ends[0] )
    &&
    Stream::close( ends[1] )
  )
  {
    ends[0] = INVALID_FD;
    ends[1] = INVALID_FD;
    return true;
  }
  else
    return false;
}

Pipe& Pipe::create()
{
  fd_t ends[2];
#ifdef _WIN32
  SECURITY_ATTRIBUTES pipe_security_attributes;
  pipe_security_attributes.nLength = sizeof( SECURITY_ATTRIBUTES );
  pipe_security_attributes.bInheritHandle = TRUE;
  pipe_security_attributes.lpSecurityDescriptor = NULL;
  if ( CreatePipe( &ends[0], &ends[1], &pipe_security_attributes, 0 ) )
  {
    if
    (
      SetHandleInformation( ends[0], HANDLE_FLAG_INHERIT, 0 ) &&
      SetHandleInformation( ends[1], HANDLE_FLAG_INHERIT, 0 )
    )
      return *new Pipe( ends );
    else
    {
      CloseHandle( ends[0] );
      CloseHandle( ends[1] );
    }
  }
#else
  if ( ::pipe( ends ) != -1 )
    return *new Pipe( ends );
#endif

  throw Exception();
}

ssize_t Pipe::read( void* buf, size_t buflen )
{
#ifdef _WIN32
  DWORD dwBytesRead;
  if
  (
    ReadFile
    (
      ends[0],
      buf,
      static_cast<DWORD>( buflen ),
      &dwBytesRead,
      NULL
    )
  )
    return static_cast<ssize_t>( dwBytesRead );
  else
    return -1;
#else
  return ::read( ends[0], buf, buflen );
#endif
}

bool Pipe::set_read_blocking_mode( bool blocking )
{
#ifdef _WIN32
  return false;
#else
  return Stream::set_blocking_mode( blocking, ends[0] );
#endif
}

bool Pipe::set_write_blocking_mode( bool blocking )
{
#ifdef _WIN32
  return false;
#else
  return Stream::set_blocking_mode( blocking, ends[1] );
#endif
}

ssize_t Pipe::write( const void* buf, size_t buflen )
{
#ifdef _WIN32
  DWORD dwBytesWritten;
  if
  (
    WriteFile
    (
      ends[1],
      buf,
      static_cast<DWORD>( buflen ),
      &dwBytesWritten,
      NULL
    )
  )
    return static_cast<ssize_t>( dwBytesWritten );
  else
    return -1;
#else
  return ::write( ends[1], buf, buflen );
#endif
}


// process.cpp
#if defined(_WIN32)
#include <windows.h>
#else
#include <signal.h>
#include <sys/wait.h> // For waitpid
#endif


Process& Process::create( const Path& command_line )
{
#ifdef _WIN32
  Pipe *child_stdin = NULL, *child_stdout = NULL, *child_stderr = NULL;
  //Pipe* child_stdin = Pipe::create(),
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

  if
  (
    CreateProcess
    (
      NULL,
      const_cast<wchar_t*>( static_cast<const wchar_t*>( command_line ) ),
      NULL,
      NULL,
      TRUE,
      CREATE_NO_WINDOW,
      NULL,
      NULL,
      &startup_info,
      &proc_info
    )
  )
  {
    return *new Process
                (
                  proc_info.hProcess,
                  proc_info.hThread,
                  child_stdin,
                  child_stdout,
                  child_stderr
                );
  }
  else
    throw Exception();
#else
  const char* argv[] = { static_cast<const char*>( NULL ) };
  return create( command_line, argv );
#endif
}

Process& Process::create( int argc, char** argv )
{
  vector<char*> argv_copy;
  for ( int arg_i = 1; arg_i < argc; arg_i++ )
    argv_copy.push_back( argv[arg_i] );
  argv_copy.push_back( NULL );
  return create( argv[0], const_cast<const char**>( &argv_copy[0] ) );
}

Process& Process::create( const vector<char*>& argv )
{
  vector<char*> argv_copy( argv );
  argv_copy.push_back( NULL );
  return create( argv[0], const_cast<const char**>( &argv_copy[0] ) );
}

Process&
Process::create
(
  const Path& executable_file_path,
  const char** null_terminated_argv
)
{
#ifdef _WIN32
  const string& executable_file_path_str
    = static_cast<const string&>( executable_file_path );

  string command_line;
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
  Pipe *child_stdin = NULL, *child_stdout = NULL, *child_stderr = NULL;
  //Pipe* child_stdin = Pipe::create(),
  //                  child_stdout = Pipe::create(),
  //                  child_stderr = Pipe::create();

  pid_t child_pid = fork();
  if ( child_pid == -1 )
    throw Exception();
  else if ( child_pid == 0 ) // Child
  {
    //close( STDIN_FILENO );
    // Set stdin to read end of stdin pipe
    //dup2( *child_stdin->get_input_stream()->get_file(), STDIN_FILENO );

    //close( STDOUT_FILENO );
    // Set stdout to write end of stdout pipe
    //dup2( *child_stdout->get_output_stream()->get_file(), STDOUT_FILENO );

    //close( STDERR_FILENO );
    // Set stderr to write end of stderr pipe
    //dup2( *child_stderr->get_output_stream()->get_file(), STDERR_FILENO );

    vector<char*> argv_with_executable_file_path;
    argv_with_executable_file_path.push_back
    (
      const_cast<char*>( static_cast<const char*>( executable_file_path ) )
    );
    size_t arg_i = 0;
    while ( null_terminated_argv[arg_i] != NULL )
    {
      argv_with_executable_file_path.push_back
      (
        const_cast<char*>( null_terminated_argv[arg_i] )
      );
      arg_i++;
    }
    argv_with_executable_file_path.push_back( NULL );

    execv( executable_file_path, &argv_with_executable_file_path[0] );

    throw Exception(); // Should never be reached
  }
  else // Parent
    return *new Process( child_pid, child_stdin, child_stdout, child_stderr );
#endif
}

#ifdef _WIN32
Process::Process
(
  HANDLE hChildProcess,
  HANDLE hChildThread,
  Pipe* child_stdin,
  Pipe* child_stdout,
  Pipe* child_stderr
)
  : hChildProcess( hChildProcess ),
    hChildThread( hChildThread ),
#else
Process::Process
(
  pid_t child_pid,
  Pipe* child_stdin,
  Pipe* child_stdout,
  Pipe* child_stderr
)
  : child_pid( child_pid ),
#endif
    child_stdin( child_stdin ),
    child_stdout( child_stdout ),
    child_stderr( child_stderr )
{ }

Process::~Process()
{
#ifdef _WIN32
  CloseHandle( hChildProcess );
  CloseHandle( hChildThread );
#endif
  Pipe::dec_ref( child_stdin );
  Pipe::dec_ref( child_stdout );
  Pipe::dec_ref( child_stderr );
}

unsigned long Process::getpid()
{
#ifdef _WIN32
  return GetCurrentProcessId();
#else
  return ::getpid();
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
  if ( waitpid( child_pid, out_return_code, WNOHANG ) > 0 )
  {
    // "waitpid() was successful. The value returned indicates the process ID
    // of the child process whose status information was recorded in the
    // storage pointed to by stat_loc."
#if defined(__FreeBSD__) || defined(__sun)
    if ( WIFEXITED( *out_return_code ) ) // Child exited normally
    {
      *out_return_code = WEXITSTATUS( *out_return_code );
#else
    if ( WIFEXITED( out_return_code ) ) // Child exited normally
    {
      *out_return_code = WEXITSTATUS( out_return_code );
#endif
      return true;
    }
    else
      return false;
  }
  // 0 = WNOHANG was specified on the options parameter, but no child process
  // was immediately available.
  // -1 = waitpid() was not successful. The errno value is set
  // to indicate the error.
  else
    return false;
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


// processor_set.cpp
#if defined(_WIN32)
#include <windows.h>
#elif defined(__linux)
#include <sched.h>
#elif defined(__MACH__)
#include <mach/mach.h>
#include <mach/mach_error.h>
#elif defined(__sun)
#include <kstat.h> // For kstat
#include <sys/processor.h> // For p_online
#include <sys/pset.h>
#endif


ProcessorSet::ProcessorSet()
{
#if defined(_WIN32)
  mask = 0;
#elif defined(__linux)
  cpu_set = new cpu_set_t;
  CPU_ZERO( static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  psetid = PS_NONE; // Don't pset_create until we actually use the set,
                    // to avoid leaving state in the system
#else
  DebugBreak();
#endif
}

ProcessorSet::ProcessorSet( uint32_t from_mask )
{
#if defined(_WIN32)
  this->mask = from_mask;
#else
#if defined(__linux)
  cpu_set = new cpu_set_t;
  CPU_ZERO( static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  psetid = PS_NONE;
#else
  DebugBreak();
#endif
  if ( from_mask != 0 )
  {
    for ( uint32_t processor_i = 0; processor_i < 32; processor_i++ )
    {
      if ( ( 1UL << processor_i ) & from_mask )
        set( processor_i );
    }
  }
#endif
}

ProcessorSet::~ProcessorSet()
{
#if defined(__linux)
  delete static_cast<cpu_set_t*>( cpu_set );
#elif defined(__sun)
  if ( psetid != PS_NONE ) pset_destroy( psetid );
#endif
}

void ProcessorSet::clear()
{
#if defined(_WIN32)
  mask = 0;
#elif defined(__linux)
  CPU_ZERO( static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  if ( psetid != PS_NONE )
  {
    pset_destroy( psetid );
    psetid = PS_NONE;
  }
#endif
}

void ProcessorSet::clear( uint16_t processor_i )
{
#if defined(_WIN32)
  unsigned long bit = ( 1L << processor_i );
  if ( ( bit & mask ) == bit )
    mask ^= bit;
#elif defined(__linux)
  CPU_CLR( processor_i, static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  if ( psetid != PS_NONE )
    pset_assign( PS_NONE, processor_i, NULL );
#endif
}

uint16_t ProcessorSet::count() const
{
  uint16_t count = 0;
  for
  (
    uint16_t processor_i = 0;
    processor_i < static_cast<uint16_t>( -1 );
    processor_i++
  )
  {
    if ( isset( processor_i ) )
      count++;
  }
  return count;
}

bool ProcessorSet::empty() const
{
#if defined(_WIN32)
  return mask == 0;
#else
  return count() == 0;
#endif
}

uint16_t ProcessorSet::getLogicalProcessorsPerPhysicalProcessor()
{
  return getOnlineLogicalProcessorCount() / getOnlinePhysicalProcessorCount();
}

uint16_t ProcessorSet::getOnlineLogicalProcessorCount()
{
  uint16_t online_logical_processor_count = 0;

#if defined(_WIN32)
  SYSTEM_INFO available_info;
  GetSystemInfo( &available_info );
  online_logical_processor_count
    = static_cast<uint16_t>( available_info.dwNumberOfProcessors );
#elif defined(__linux__)
  long _online_logical_processor_count = sysconf( _SC_NPROCESSORS_ONLN );
  if ( _online_logical_processor_count != -1 )
    online_logical_processor_count = _online_logical_processor_count;
#elif defined(__MACH__)
  host_basic_info_data_t basic_info;
  host_info_t info = (host_info_t)&basic_info;
  host_flavor_t flavor = HOST_BASIC_INFO;
  mach_msg_type_number_t count = HOST_BASIC_INFO_COUNT;

  if ( host_info( mach_host_self(), flavor, info, &count ) == KERN_SUCCESS )
    online_logical_processor_count = basic_info.avail_cpus;
#elif defined(__sun)
  online_logical_processor_count = 0;
  processorid_t cpuid_max = sysconf( _SC_CPUID_MAX );
  for ( processorid_t cpuid_i = 0; cpuid_i <= cpuid_max; cpuid_i++)
  {
    if ( p_online( cpuid_i, P_STATUS ) == P_ONLINE )
      online_logical_processor_count++;
  }
#endif

  if ( online_logical_processor_count > 0 )
    return online_logical_processor_count;
  else
    return 1;
}

uint16_t ProcessorSet::getOnlinePhysicalProcessorCount()
{
#if defined(__sun)
  kstat_ctl_t* kc;

  kc = kstat_open();
  if ( kc )
  {
    uint16_t online_physical_processor_count = 1;

    kstat* ksp = kstat_lookup( kc, "cpu_info", -1, NULL );
    int32_t last_core_id = 0;
    while ( ksp )
    {
      kstat_read( kc, ksp, NULL );
      kstat_named_t* knp;
      knp = ( kstat_named_t* )kstat_data_lookup( ksp, "core_id" );
      if ( knp )
      {
        int32_t this_core_id = knp->value.i32;
        if ( this_core_id != last_core_id )
        {
          online_physical_processor_count++;
          last_core_id = this_core_id;
        }
      }
      ksp = ksp->ks_next;
    }

    kstat_close( kc );

    return online_physical_processor_count;
  }
#endif

  return getOnlineLogicalProcessorCount();
}

bool ProcessorSet::isset( uint16_t processor_i ) const
{
#if defined(_WIN32)
  if ( processor_i < 32 )
  {
    unsigned long bit = ( 1L << processor_i );
    return ( bit & mask ) == bit;
  }
#elif defined(__linux)
  return CPU_ISSET( processor_i, static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  if ( psetid != PS_NONE )
  {
    psetid_t check_psetid;
    return pset_assign
           (
             PS_QUERY,
             processor_i,
             &check_psetid
           ) == 0
           &&
           check_psetid == psetid;
  }
#endif
  return false;
}

bool ProcessorSet::set( uint16_t processor_i )
{
#if defined(_WIN32)
  mask |= ( 1L << processor_i );
#elif defined(__linux)
  CPU_SET( processor_i, static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  if ( psetid == PS_NONE )
  {
    if ( pset_create( &psetid ) != 0 )
      return false;
  }

  if ( pset_assign( psetid, processor_i, NULL ) != 0 )
    return false;
#endif

  return isset( processor_i );
}


// semaphore.cpp
#ifdef _WIN32
#include <windows.h>
#else
#include <unistd.h>
#ifdef __MACH__
#include <mach/clock.h>
#include <mach/mach_init.h>
#include <mach/task.h>
#endif
#endif


Semaphore::Semaphore()
{
#if defined(_WIN32)
  hSemaphore = CreateSemaphore( NULL, 0, LONG_MAX, NULL );
#elif defined(__MACH__)
  semaphore_create( mach_task_self(), &sem, SYNC_POLICY_FIFO, 0 );
#else
  sem_init( &sem, 0, 0 );
#endif
}

Semaphore::~Semaphore()
{
#if defined(_WIN32)
  CloseHandle( hSemaphore );
#elif defined(__MACH__)
  semaphore_destroy( mach_task_self(), sem );
#else
  sem_destroy( &sem );
#endif
}

bool Semaphore::acquire()
{
#if defined(_WIN32)
  DWORD dwRet = WaitForSingleObjectEx( hSemaphore, INFINITE, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#elif defined(__MACH__)
  return semaphore_wait( sem ) == KERN_SUCCESS;
#else
  return sem_wait( &sem ) == 0;
#endif
}

bool Semaphore::acquire( const Time& timeout )
{
#if defined(_WIN32)
  DWORD timeout_ms = static_cast<DWORD>( timeout.as_unix_time_ms() );
  DWORD dwRet = WaitForSingleObjectEx( hSemaphore, timeout_ms, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#elif defined(__MACH__)
  mach_timespec_t timeout_m_ts
    = {
        timeout.as_unix_time_ns() / Time::NS_IN_S,
        timeout.as_unix_time_ns() % Time::NS_IN_S
      };
  return semaphore_timedwait( sem, timeout_m_ts ) == KERN_SUCCESS;
#else
  struct timespec timeout_ts = Time() + timeout;
  return sem_timedwait( &sem, &timeout_ts ) == 0;
#endif
}

void Semaphore::release()
{
#if defined(_WIN32)
  ReleaseSemaphore( hSemaphore, 1, NULL );
#elif defined(__MACH__)
  semaphore_signal( sem );
#else
  sem_post( &sem );
#endif
}

bool Semaphore::try_acquire()
{
#if defined(_WIN32)
  DWORD dwRet = WaitForSingleObjectEx( hSemaphore, 0, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#elif defined(__MACH__)
  mach_timespec_t timeout_m_ts = { 0, 0 };
  return semaphore_timedwait( sem, timeout_m_ts ) == KERN_SUCCESS;
#else
  return sem_trywait( &sem ) == 0;
#endif
}


// shared_library.cpp
#ifdef _WIN32
#include <windows.h>
#else
#include <dlfcn.h>
#include <cctype>
#endif


#ifdef _WIN32
#define DLOPEN( file_path ) \
    LoadLibraryEx( file_path, 0, LOAD_WITH_ALTERED_SEARCH_PATH )
#else
#define DLOPEN( file_path ) \
    dlopen( file_path, RTLD_NOW|RTLD_GLOBAL )
#endif


#if defined(_WIN32)
const wchar_t* SharedLibrary::SHLIBSUFFIX = L"dll";
#elif defined(__MACH__)
const char* SharedLibrary::SHLIBSUFFIX = "dylib";
#else
const char* SharedLibrary::SHLIBSUFFIX = "so";
#endif


SharedLibrary::SharedLibrary( void* handle )
  : handle( handle )
{ }

SharedLibrary::~SharedLibrary()
{
  if ( handle != NULL )
  {
#ifdef _WIN32
    FreeLibrary( ( HMODULE )handle );
#else
#ifndef _DEBUG
    dlclose( handle ); // Don't dlclose when debugging,
                       // because that causes valgrind to lose symbols
#endif
#endif
  }
}

void* SharedLibrary::getFunction
(
  const char* function_name,
  void* missing_function_return_value
)
{
  void* function_handle;
#ifdef _WIN32
  function_handle = GetProcAddress( ( HMODULE )handle, function_name );
#else
  function_handle = dlsym( handle, function_name );
#endif
  if ( function_handle )
    return function_handle;
  else
    return missing_function_return_value;
}

SharedLibrary* SharedLibrary::open( const Path& file_prefix, const char* argv0 )
{
  void* handle;
  if ( ( handle = DLOPEN( file_prefix ) ) != NULL )
    return new SharedLibrary( handle );
  else
  {
    Path file_path = "lib" / file_prefix + SHLIBSUFFIX;

    if ( ( handle = DLOPEN( file_path ) ) != NULL )
      return new SharedLibrary( handle );
    else
    {
      Path file_path = file_prefix + SHLIBSUFFIX;

      if ( ( handle = DLOPEN( file_path ) ) != NULL )
        return new SharedLibrary( handle );
      else
      {
        if ( argv0 != NULL )
        {
          const char* last_slash = strrchr( argv0, Path::SEPARATOR );
          while ( last_slash != NULL && last_slash != argv0 )
          {
            Path file_path = Path( argv0, last_slash - argv0 + 1 )
                             + file_prefix + SHLIBSUFFIX;

            if ( ( handle = DLOPEN( file_path ) ) != NULL )
              return new SharedLibrary( handle );
            else
            {
              Path file_path = Path( argv0, last_slash - argv0 + 1 )
                               / "lib" + file_prefix + SHLIBSUFFIX;

              if ( ( handle = DLOPEN( file_path ) ) != NULL )
                return new SharedLibrary( handle );
            }

            last_slash--;
            while ( *last_slash != Path::SEPARATOR ) last_slash--;
          }
        }
      }
    }
  }

  return NULL;
}


// socket.cpp
#ifdef _WIN32
#undef INVALID_SOCKET
#pragma warning( push )
#pragma warning( disable: 4365 4995 )
#include <ws2tcpip.h>
#pragma comment( lib, "ws2_32.lib" )
#pragma warning( pop )
#define INVALID_SOCKET  (SOCKET)(~0)
#define ECONNABORTED WSAECONNABORTED
#else
#include <arpa/inet.h>
#include <errno.h>
#include <netdb.h>
#include <netinet/in.h>
#include <signal.h>
#include <sys/socket.h>
#include <unistd.h>
#endif


int Socket::DOMAIN_DEFAULT = AF_INET6;


Socket::IOConnectCB::IOConnectCB
(
  SocketAddress& peername,
  Socket& socket_,
  AIOConnectCallback& callback,
  void* callback_context
)
  : IOCB<AIOConnectCallback>( callback, callback_context ),
    peername( peername ),
    socket_( socket_ )
{ }

Socket::IOConnectCB::~IOConnectCB()
{
  SocketAddress::dec_ref( peername );
  Socket::dec_ref( socket_ );
}

void Socket::IOConnectCB::onConnectCompletion()
{
  callback.onConnectCompletion( callback_context );
}

void Socket::IOConnectCB::onConnectError()
{
  onConnectError( get_last_error() );
}

void Socket::IOConnectCB::onConnectError( uint32_t error_code )
{
  callback.onConnectError( error_code, callback_context );
}


Socket::IORecvCB::IORecvCB
(
  Buffer& buffer,
  int flags,
  Socket& socket_,
  AIOReadCallback& callback,
  void* callback_context
)
  : IOCB<AIOReadCallback>( callback, callback_context ),
    buffer( buffer ),
    flags( flags ),
    socket_( socket_ )
{ }

Socket::IORecvCB::~IORecvCB()
{
  Buffer::dec_ref( buffer );
  Socket::dec_ref( socket_ );
}

void Socket::IORecvCB::onReadCompletion()
{
  // Assumes buffer->put( recv_ret ) has already been called
  callback.onReadCompletion( buffer, callback_context );
}

void Socket::IORecvCB::onReadError()
{
  onReadError( get_last_error() );
}

void Socket::IORecvCB::onReadError( uint32_t error_code )
{
  callback.onReadError( error_code, callback_context );
}


Socket::IOSendCB::IOSendCB
(
  Buffer& buffer,
  int flags,
  Socket& socket_,
  AIOWriteCallback& callback,
  void* callback_context
)
  : IOCB<AIOWriteCallback>( callback, callback_context ),
    buffer( buffer ),
    flags( flags ),
    socket_( socket_ )
{
  partial_send_len = 0;
}

Socket::IOSendCB::~IOSendCB()
{
  Buffer::dec_ref( buffer );
  Socket::dec_ref( socket_ );
}

bool Socket::IOSendCB::execute( bool blocking_mode )
{
  if ( get_socket().set_blocking_mode( blocking_mode ) )
  {
    for ( ;; ) // Keep trying partial sends
    {
      ssize_t send_ret
        = get_socket().send
          (
            static_cast<const char*>( get_buffer() ) + partial_send_len,
            get_flags()
          );

      if ( send_ret >= 0 )
      {
        partial_send_len += send_ret;
        if ( partial_send_len == get_buffer().size() )
          return true;
        else
          continue;
      }
      else
        return false;
    }
  }
  else
    return false;
}

void Socket::IOSendCB::onWriteCompletion()
{
  callback.onWriteCompletion( callback_context );
}

void Socket::IOSendCB::onWriteError()
{
  onWriteError( get_last_error() );
}

void Socket::IOSendCB::onWriteError( uint32_t error_code )
{
  callback.onWriteError( error_code, callback_context );
}


Socket::IOSendMsgCB::IOSendMsgCB
(
  Buffers& buffers,
  int flags,
  Socket& socket_,
  AIOWriteCallback& callback,
  void* callback_context
)
  : IOCB<AIOWriteCallback>( callback, callback_context ),
    buffers( buffers ),
    flags( flags ),
    socket_( socket_ )
{
  buffers_len = 0;
  const struct iovec* iov = get_buffers();
  uint32_t iovlen = get_buffers().size();
  for ( uint32_t iov_i = 0; iov_i < iovlen; iov_i++ )
    buffers_len += iov[iov_i].iov_len;

  partial_send_len = 0;
}

Socket::IOSendMsgCB::~IOSendMsgCB()
{
  Buffers::dec_ref( buffers );
  Socket::dec_ref( socket_ );
}

bool Socket::IOSendMsgCB::execute( bool blocking_mode )
{
  if ( get_socket().set_blocking_mode( blocking_mode ) )
  {
    for ( ;; ) // Keep trying partial sends
    {
      ssize_t sendmsg_ret;
      if ( partial_send_len == 0 )
        sendmsg_ret = get_socket().sendmsg( buffers, get_flags() );
      else
      {
        const struct iovec* iov = buffers;
        uint32_t iovlen = buffers.size();

        uint32_t sent_until_iov_i = 0;
        size_t sent_until_iov_i_pos = 0;
        size_t temp_partial_send_len = partial_send_len;
        for ( ;; ) // Calculate write_until_iov_*
        {
          if ( iov[sent_until_iov_i].iov_len < temp_partial_send_len )
          {
            // The buffer and part of the next was already written
            temp_partial_send_len -= iov[sent_until_iov_i].iov_len;
            sent_until_iov_i++;
          }
          else if ( iov[sent_until_iov_i].iov_len == temp_partial_send_len )
          {
            // The buffer was already written, but none of the next
            temp_partial_send_len = 0;
            sent_until_iov_i++;
            break;
          }
          else // Part of the buffer was written
          {
            sent_until_iov_i_pos = temp_partial_send_len;
            break;
          }
        }

        if ( sent_until_iov_i_pos == 0 ) // Writing whole buffers
        {
          sendmsg_ret
            = get_socket().sendmsg
              (
                &iov[sent_until_iov_i],
                iovlen - sent_until_iov_i,
                get_flags()
              );
        }
        else // Writing part of a buffer
        {
          struct iovec temp_iov;

          temp_iov.iov_base
            = static_cast<char*>( iov[sent_until_iov_i].iov_base )
              + sent_until_iov_i_pos;

          temp_iov.iov_len = iov[sent_until_iov_i].iov_len
                               - sent_until_iov_i_pos;

          sendmsg_ret = get_socket().sendmsg( &temp_iov, 1, get_flags() );
        }
      }

      if ( sendmsg_ret >= 0 )
      {
        partial_send_len += sendmsg_ret;

        if ( partial_send_len == buffers_len )
          return true;
        else
          continue;
      }
      else
        return false;
    }
  }
  else
    return false;
}

void Socket::IOSendMsgCB::onWriteCompletion()
{
  callback.onWriteCompletion( callback_context );
}

void Socket::IOSendMsgCB::onWriteError()
{
  onWriteError( get_last_error() );
}

void Socket::IOSendMsgCB::onWriteError( uint32_t error_code )
{
  callback.onWriteError( error_code, callback_context );
}


class Socket::BIOConnectCB : public BIOCB, public IOConnectCB
{
public:
  BIOConnectCB
  (
    SocketAddress& peername,
    Socket& socket_,
    AIOConnectCallback& callback,
    void* callback_context
  )
    : IOConnectCB( peername, socket_, callback, callback_context )
  { }

  // BIOCB
  void execute()
  {
    if ( get_socket().set_blocking_mode( true ) )
    {
      if ( get_socket().connect( get_peername() ) )
        onConnectCompletion();
      else
        onConnectError();
    }
    else
      onConnectError();
  }
};


class Socket::BIORecvCB : public BIOCB, public IORecvCB
{
public:
  BIORecvCB
  (
    Buffer& buffer,
    int flags,
    Socket& socket_,
    AIOReadCallback& callback,
    void* callback_context
  )
    : IORecvCB( buffer, flags, socket_, callback, callback_context )
  { }

  // BIOCB
  void execute()
  {
    if ( get_socket().set_blocking_mode( true ) )
    {
      ssize_t recv_ret = get_socket().recv( get_buffer(), get_flags() );
      if ( recv_ret > 0 )
        onReadCompletion();
      else if ( recv_ret == 0 )
        onReadError( ECONNABORTED );
      else
        onReadError();
    }
    else
      onReadError();
  }
};


class Socket::BIOSendCB : public BIOCB, public IOSendCB
{
public:
  BIOSendCB
  (
    Buffer& buffer,
    int flags,
    Socket& socket_,
    AIOWriteCallback& callback,
    void* callback_context
  )
    : IOSendCB( buffer, flags, socket_, callback, callback_context )
  { }

  // BIOCB
  void execute()
  {
    if ( IOSendCB::execute( true ) )
      onWriteCompletion();
    else
      onWriteError();
  }
};


class Socket::BIOSendMsgCB
  : public BIOCB, public IOSendMsgCB
{
public:
  BIOSendMsgCB
  (
    Buffers& buffers,
    int flags,
    Socket& socket_,
    AIOWriteCallback& callback,
    void* callback_context
  )
    : IOSendMsgCB( buffers, flags, socket_, callback, callback_context )
  { }

  // BIOCB
  void execute()
  {
    if ( IOSendMsgCB::execute( true ) )
      onWriteCompletion();
    else
      onWriteError();
  }
};


class Socket::NBIOConnectCB : public NBIOCB, public IOConnectCB
{
public:
  NBIOConnectCB
  (
    SocketAddress& peername,
    Socket& socket_,
    AIOConnectCallback& callback,
    void* callback_context
  )
  : NBIOCB( STATE_WANT_CONNECT ),
    IOConnectCB( peername, socket_, callback, callback_context )
  { }

  // NBIOCB
  void execute()
  {
    if ( get_socket().set_blocking_mode( false ) )
    {
      if ( get_socket().connect( get_peername() ) )
      {
        set_state( STATE_COMPLETE );
        onConnectCompletion();
      }
      else if ( get_socket().want_connect() )
        set_state( STATE_WANT_CONNECT );
      else
      {
        set_state( STATE_ERROR );
        onConnectError();
      }
    }
    else
    {
      set_state( STATE_ERROR );
      onConnectError();
    }
  }

  socket_t get_fd() const { return get_socket(); }
};


class Socket::NBIORecvCB : public NBIOCB, public IORecvCB
{
public:
  NBIORecvCB
  (
    State state,
    Buffer& buffer,
    int flags,
    Socket& socket_,
    AIOReadCallback& callback,
    void* callback_context
  )
    : NBIOCB( state ),
      IORecvCB( buffer, flags, socket_, callback, callback_context )
  { }

  // NBIOCB
  void execute()
  {
    if ( get_socket().set_blocking_mode( false ) )
    {
      ssize_t recv_ret = get_socket().recv( get_buffer(), get_flags() );
      if ( recv_ret > 0 )
      {
        set_state( STATE_COMPLETE );
        onReadCompletion();
      }
      else if ( recv_ret == 0 )
      {
        set_state( STATE_ERROR );
        onReadError( ECONNABORTED );
      }
      else if ( get_socket().want_read() )
        set_state( STATE_WANT_READ );
      else if ( get_socket().want_write() )
        set_state( STATE_WANT_WRITE );
      else
      {
        set_state( STATE_ERROR );
        onReadError();
      }
    }
    else
    {
      set_state( STATE_ERROR );
      onReadError();
    }
  }

  socket_t get_fd() const { return get_socket(); }
};


class Socket::NBIOSendCB : public NBIOCB, public IOSendCB
{
public:
  NBIOSendCB
  (
    State state,
    Buffer& buffer,
    int flags,
    Socket& socket_,
    AIOWriteCallback& callback,
    void* callback_context
  )
    : NBIOCB( state ),
      IOSendCB( buffer, flags, socket_, callback, callback_context )
  { }

  // NBIOCB
  void execute()
  {
    if ( IOSendCB::execute( false ) )
    {
      set_state( STATE_COMPLETE );
      onWriteCompletion();
    }
    else if ( get_socket().want_write() )
      set_state( STATE_WANT_WRITE );
    else if ( get_socket().want_read() )
      set_state( STATE_WANT_READ );
    else
    {
      set_state( STATE_ERROR );
      onWriteError();
    }
  }

  socket_t get_fd() const { return get_socket(); }
};


class Socket::NBIOSendMsgCB : public NBIOCB, public IOSendMsgCB
{
public:
  NBIOSendMsgCB
  (
    State state,
    Buffers& buffers,
    int flags,
    Socket& socket_,
    AIOWriteCallback& callback,
    void* callback_context
  )
    : NBIOCB( state ),
      IOSendMsgCB( buffers, flags, socket_, callback, callback_context )
  { }

  // NBIOCB
  void execute()
  {
    if ( IOSendMsgCB::execute( false ) )
    {
      set_state( STATE_COMPLETE );
      onWriteCompletion();
    }
    else if ( get_socket().want_write() )
      set_state( STATE_WANT_WRITE );
    else if ( get_socket().want_read() )
      set_state( STATE_WANT_READ );
    else
    {
      set_state( STATE_ERROR );
      onWriteError();
    }
  }

  socket_t get_fd() const { return get_socket(); }
};


Socket::Socket( int domain, int type, int protocol, socket_t socket_ )
  : domain( domain ), type( type ), protocol( protocol ), socket_( socket_ )
{
  blocking_mode = true;
  connected = false;
  io_queue = NULL;
}

Socket::~Socket()
{
  close();
  IOQueue::dec_ref( io_queue );
}

void
Socket::aio_connect
(
  SocketAddress& peername,
  AIOConnectCallback& callback,
  void* callback_context
)
{
  // Try a non-blocking connect first (for e.g. localhost)
  if ( set_blocking_mode( false ) )
  {
    if ( connect( peername ) )
    {
      callback.onConnectCompletion( callback_context );
      return;
    }
    else if ( !want_connect() )
    {
      callback.onConnectError( get_last_error(), callback_context );
      return;
    }
  }

  if ( io_queue != NULL )
  {
    switch ( io_queue->get_type_id() )
    {
      case BIOQueue::TYPE_ID:
      {
        static_cast<BIOQueue*>( io_queue )
          ->submit
            (
              *new BIOConnectCB
                   (
                     peername.inc_ref(),
                     inc_ref(),
                     callback,
                     callback_context
                   )
            );
      }
      return;

      case NBIOQueue::TYPE_ID:
      {
        static_cast<NBIOQueue*>( io_queue )
          ->submit
            (
              *new NBIOConnectCB
                    (
                      peername.inc_ref(),
                      inc_ref(),
                      callback,
                      callback_context
                    )
            );
      }
      return;
    }
  }

  set_blocking_mode( true );
  if ( connect( peername ) )
    callback.onConnectCompletion( callback_context );
  else
    callback.onConnectError( get_last_error(), callback_context );
}

void
Socket::aio_recv
(
  Buffer& buffer,
  int flags,
  AIOReadCallback& callback,
  void* callback_context
)
{
  // Try a non-blocking recv first
  if ( set_blocking_mode( false ) )
  {
    ssize_t recv_ret = recv( buffer, flags );
    if ( recv_ret > 0 )
    {
      callback.onReadCompletion( buffer, callback_context );
      Buffer::dec_ref( buffer );
      return;
    }
    else if ( recv_ret == 0 )
    {
      callback.onReadError( ECONNABORTED, callback_context );
      Buffer::dec_ref( buffer );
      return;
    }
    else if ( !want_read() && !want_write() )
    {
      callback.onReadError( get_last_error(), callback_context );
      Buffer::dec_ref( buffer );
      return;
    }
  }

  // Next try to offload the recv to an IOQueue
  if ( io_queue != NULL )
  {
    switch ( io_queue->get_type_id() )
    {
      case BIOQueue::TYPE_ID:
      {
        static_cast<BIOQueue*>( io_queue )
          ->submit
            (
              *new BIORecvCB
                   (
                     buffer,
                     flags,
                     inc_ref(),
                     callback,
                     callback_context
                   )
            );
      }
      return;

      case NBIOQueue::TYPE_ID:
      {
        static_cast<NBIOQueue*>( io_queue )
          ->submit
            (
              *new NBIORecvCB
                   (
                     want_read()
                       ? NBIORecvCB::STATE_WANT_READ
                       : NBIORecvCB::STATE_WANT_WRITE,
                     buffer,
                     flags,
                     inc_ref(),
                     callback,
                     callback_context
                   )
            );
      }
      return;
    }
  }

  // Nothing worked, return an error
  callback.onReadError( get_last_error(), callback_context );
  Buffer::dec_ref( buffer );
}

void
Socket::aio_send
(
  Buffer& buffer,
  int flags,
  AIOWriteCallback& callback,
  void* callback_context
)
{
  // Don't translate flags here, since they'll be translated again
  // on the ->send call (except on Win32)

  // Try a non-blocking send first
  if ( set_blocking_mode( false ) )
  {
    ssize_t send_ret = send( buffer, flags );
    if ( send_ret >= 0 )
    {
#ifdef _WIN32
      if ( static_cast<size_t>( send_ret ) != buffer.size() )
        DebugBreak();
#endif
      callback.onWriteCompletion( callback_context );
      Buffer::dec_ref( buffer );
      return;
    }
    else if ( !want_write() && !want_read() )
    {
      callback.onWriteError( get_last_error(), callback_context );
      Buffer::dec_ref( buffer );
      return;
    }
  }

  // Next try to offload the send to an IOQueue
  if ( io_queue != NULL )
  {
    switch ( io_queue->get_type_id() )
    {
      case BIOQueue::TYPE_ID:
      {
        static_cast<BIOQueue*>( io_queue )
          ->submit
            (
              *new BIOSendCB
                   (
                     buffer,
                     flags,
                     inc_ref(),
                     callback,
                     callback_context
                   )
            );
      }
      return;

      case NBIOQueue::TYPE_ID:
      {
        static_cast<NBIOQueue*>( io_queue )
          ->submit
            (
               *new NBIOSendCB
                    (
                      want_write()
                        ? NBIOSendCB::STATE_WANT_WRITE
                        : NBIOSendCB::STATE_WANT_READ,
                      buffer,
                      flags,
                      inc_ref(),
                      callback,
                      callback_context
                    )
            );
      }
      return;

    }
  }

  // Finally, try a blocking send
  set_blocking_mode( true );
  ssize_t send_ret = send( buffer, flags );
  if ( send_ret >= 0 )
  {
#ifdef _DEBUG
    if ( static_cast<size_t>( send_ret ) != buffer.size() )
      DebugBreak();
#endif
    callback.onWriteCompletion( callback_context );
  }
  else
    callback.onWriteError( get_last_error(), callback_context );
  Buffer::dec_ref( buffer );
}

void Socket::aio_sendmsg
(
  Buffers& buffers,
  int flags,
  AIOWriteCallback& callback,
  void* callback_context
)
{
  // Try a non-blocking sendmsg first
  if ( set_blocking_mode( false ) )
  {
    ssize_t sendmsg_ret = sendmsg( buffers, flags );
    if ( sendmsg_ret >= 0 )
    {
      callback.onWriteCompletion( callback_context );
      Buffer::dec_ref( buffers );
      return;
    }
    else if ( !want_write() && !want_read() )
    {
      callback.onWriteError( get_last_error(), callback_context );
      Buffer::dec_ref( buffers );
      return;
    }
  }

  // Next try to offload the sendmsg to an IOQueue
  if ( io_queue != NULL )
  {
    switch ( io_queue->get_type_id() )
    {
      case BIOQueue::TYPE_ID:
      {
        static_cast<BIOQueue*>( io_queue )
          ->submit
            (
              *new BIOSendMsgCB
                   (
                     buffers,
                     flags,
                     inc_ref(),
                     callback,
                     callback_context
                   )
            );
      }
      return;

      case NBIOQueue::TYPE_ID:
      {
        static_cast<NBIOQueue*>( io_queue )
          ->submit
            (
              *new NBIOSendMsgCB
                   (
                     want_write()
                       ? NBIOSendCB::STATE_WANT_WRITE
                       : NBIOSendCB::STATE_WANT_READ,
                     buffers,
                     flags,
                     inc_ref(),
                     callback,
                     callback_context
                   )
            );
      }
      return;

      // default: unknown io_queue, drop down
    }
  }

  // Finally, try a blocking sendmsg
  set_blocking_mode( true );
  ssize_t sendmsg_ret = sendmsg( buffers, flags );
  if ( sendmsg_ret >= 0 )
    callback.onWriteCompletion( callback_context );
  else
    callback.onWriteError( get_last_error(), callback_context );
  Buffer::dec_ref( buffers );
}

bool Socket::associate( IOQueue& io_queue )
{
  if ( this->io_queue == NULL )
  {
    switch ( io_queue.get_type_id() )
    {
      case BIOQueue::TYPE_ID:
      case NBIOQueue::TYPE_ID:
      {
        set_io_queue( io_queue );
        return true;
      }
      break;

      default: return false;
    }
  }
  else // this Socket is already associated with an IOQueue
    return false;
}

bool Socket::bind( const SocketAddress& to_sockaddr )
{
  for ( ;; )
  {
    struct sockaddr* name; socklen_t namelen;
    if ( to_sockaddr.as_struct_sockaddr( domain, name, namelen ) )
    {
      if ( ::bind( *this, name, namelen ) != -1 )
        return true;
    }

    if
    (
      domain == AF_INET6
      &&
#ifdef _WIN32
        WSAGetLastError() == WSAEAFNOSUPPORT
#else
        errno == EAFNOSUPPORT
#endif
    )
    {
      if ( recreate( AF_INET ) )
        continue;
      else
        return false;
    }
    else
      return false;
  }
}

bool Socket::close()
{
  if ( Stream::close( socket_ ) )
  {
    connected = false;
    // socket_ = INVALID_SOCKET;
    // Don't set socket_ to INVALID socket_ so it can be dissociated from an
    // event queue after it's close()'d
    return true;
  }
  else
    return false;
}

bool Socket::connect( const SocketAddress& peername )
{
  if ( !connected )
  {
    for ( ;; )
    {
      struct sockaddr* name; socklen_t namelen;
      if ( peername.as_struct_sockaddr( domain, name, namelen ) )
      {
        if ( ::connect( *this, name, namelen ) != -1 )
        {
          connected = true;
          return true;
        }
        else
        {
#ifdef _WIN32
          switch ( WSAGetLastError() )
          {
            case WSAEISCONN: connected = true; return true;
            case WSAEAFNOSUPPORT:
#else
          switch ( errno )
          {
            case EISCONN: connected = true; return true;
            case EAFNOSUPPORT:
#endif
            {
              if
              (
                domain == AF_INET6 &&
                recreate( AF_INET )
              )
                continue;
              else
                return false;
            }
            break;

            default: return false;
          }
        }
      }
      else if
      (
        domain == AF_INET6 &&
        recreate( AF_INET )
      )
        continue;
      else
        return false;
    }
  }
  else
    return true;
}

Socket* Socket::create( int type, int protocol )
{
  return create( DOMAIN_DEFAULT, type, protocol );
}

Socket* Socket::create( int domain, int type, int protocol )
{
  socket_t socket_ = create( &domain, type, protocol );
  if ( socket_ != INVALID_SOCKET )
    return new Socket( domain, type, protocol, socket_ );
  else
    return NULL;
}

socket_t Socket::create( int* domain, int type, int protocol )
{
  socket_t socket_ = ::socket( *domain, type, protocol );

#ifdef _WIN32
  if ( socket_ == INVALID_SOCKET && WSAGetLastError() == WSANOTINITIALISED )
  {
    WORD wVersionRequested = MAKEWORD( 2, 2 );
    WSADATA wsaData;
    WSAStartup( wVersionRequested, &wsaData );

    socket_ = ::socket( *domain, type, protocol );
  }

  if ( socket_ != INVALID_SOCKET )
  {
    if ( *domain == AF_INET6 )
    {
      DWORD ipv6only = 0; // Allow dual-mode sockets
      ::setsockopt
      (
        socket_,
        IPPROTO_IPV6,
        IPV6_V6ONLY,
        ( char* )&ipv6only,
        sizeof( ipv6only )
      );
    }
  }
  else if ( *domain == AF_INET6 && WSAGetLastError() == WSAEAFNOSUPPORT )
  {
    *domain = AF_INET;
    socket_ = ::socket( AF_INET, type, protocol );
    if ( socket_ == INVALID_SOCKET )
      return INVALID_SOCKET;
  }
  else
    return INVALID_SOCKET;

  return socket_;
#else
  if ( socket_ != -1 )
    return socket_;
  else if ( *domain == AF_INET6 && errno == EAFNOSUPPORT )
  {
    *domain = AF_INET;
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

string Socket::getfqdn()
{
#ifdef _WIN32
  DWORD dwFQDNLength = 0;
  GetComputerNameExA( ComputerNameDnsHostname, NULL, &dwFQDNLength );
  if ( dwFQDNLength > 0 )
  {
    char* fqdn_temp = new char[dwFQDNLength];
    if
    (
      GetComputerNameExA
      (
        ComputerNameDnsFullyQualified,
        fqdn_temp,
        &dwFQDNLength
      )
    )
    {
      string fqdn( fqdn_temp, dwFQDNLength );
      delete [] fqdn_temp;
      return fqdn;
    }
    else
      delete [] fqdn_temp;
  }

  return string();
#else
  char fqdn[256];
  ::gethostname( fqdn, 256 );
  char* first_dot = strstr( fqdn, "." );
  if ( first_dot != NULL ) *first_dot = 0;

  // getnameinfo does not return aliases, which means we get "localhost"
  // on Linux if that's the first
  // entry for 127.0.0.1 in /etc/hosts

#ifndef __sun
  char domainname[256];
  // getdomainname is not a public call on Solaris, apparently
  if ( getdomainname( domainname, 256 ) == 0 &&
       domainname[0] != 0 &&
       strcmp( domainname, "(none)" ) != 0 &&
       strcmp( domainname, fqdn ) != 0 &&
       strstr( domainname, "localdomain" ) == NULL )
         strcat( fqdn, domainname );
  else
  {
#endif
    // Try gethostbyaddr, like Python
    uint32_t local_host_addr = inet_addr( "127.0.0.1" );
    struct hostent* hostents
      = gethostbyaddr
      (
        reinterpret_cast<char*>( &local_host_addr ),
        sizeof( uint32_t ),
        AF_INET
      );

    if ( hostents != NULL )
    {
      if
      (
        strchr( hostents->h_name, '.' ) != NULL &&
        strstr( hostents->h_name, "localhost" ) == NULL
      )
      {
        strncpy( fqdn, hostents->h_name, 256 );
      }
      else
      {
        for ( unsigned char i = 0; hostents->h_aliases[i] != NULL; i++ )
        {
          if
          (
            strchr( hostents->h_aliases[i], '.' ) != NULL &&
            strstr( hostents->h_name, "localhost" ) == NULL
          )
          {
            strncpy( fqdn, hostents->h_aliases[i], 256 );
            break;
          }
        }
      }
    }
#ifndef __sun
  }
#endif
  return fqdn;
#endif
}

string Socket::gethostname()
{
#ifdef _WIN32
  DWORD dwHostNameLength = 0;
  GetComputerNameExA( ComputerNameDnsHostname, NULL, &dwHostNameLength );
  if ( dwHostNameLength > 0 )
  {
    char* hostname_temp = new char[dwHostNameLength];
    if
    (
      GetComputerNameExA
      (
        ComputerNameDnsHostname,
        hostname_temp,
        &dwHostNameLength
      )
    )
    {
      string hostname( hostname_temp, dwHostNameLength );
      delete [] hostname_temp;
      return hostname;
    }
    else
      delete [] hostname_temp;
  }

  return string();
#else
  char hostname[256];
  ::gethostname( hostname, 256 );
  return hostname;
#endif
}

uint32_t Socket::get_last_error()
{
#ifdef _WIN32
  return static_cast<uint32_t>( WSAGetLastError() );
#else
  return static_cast<uint32_t>( errno );
#endif
}

SocketAddress* Socket::getpeername() const
{
  struct sockaddr_storage peername_sockaddr_storage;
  memset( &peername_sockaddr_storage, 0, sizeof( peername_sockaddr_storage ) );
  socklen_t peername_sockaddr_storage_len = sizeof( peername_sockaddr_storage );
  if
  (
    ::getpeername
    (
      *this,
      reinterpret_cast<struct sockaddr*>( &peername_sockaddr_storage ),
      &peername_sockaddr_storage_len
    ) != -1
  )
    return new SocketAddress( peername_sockaddr_storage );
  else
    return NULL;
}

int Socket::get_platform_recv_flags( int flags )
{
  int platform_recv_flags = 0;

  if ( ( flags & RECV_FLAG_MSG_OOB ) == RECV_FLAG_MSG_OOB )
  {
    platform_recv_flags |= MSG_OOB;
    flags ^= RECV_FLAG_MSG_OOB;
  }

  if ( ( flags & RECV_FLAG_MSG_PEEK ) == RECV_FLAG_MSG_PEEK )
  {
    platform_recv_flags |= MSG_PEEK;
    flags ^= RECV_FLAG_MSG_PEEK;
  }

  platform_recv_flags |= flags;

#ifdef __linux
  platform_recv_flags |= MSG_NOSIGNAL;
#endif

  return platform_recv_flags;
}

int Socket::get_platform_send_flags( int flags )
{
  int platform_send_flags = 0;

  if ( ( flags & SEND_FLAG_MSG_OOB ) == SEND_FLAG_MSG_OOB )
  {
    platform_send_flags |= MSG_OOB;
    flags ^= SEND_FLAG_MSG_OOB;
  }

  if ( ( flags & SEND_FLAG_MSG_DONTROUTE ) == SEND_FLAG_MSG_DONTROUTE )
  {
    platform_send_flags |= MSG_DONTROUTE;
    flags ^= SEND_FLAG_MSG_DONTROUTE;
  }

#ifdef __linux
  platform_send_flags |= MSG_NOSIGNAL;
#endif

  return platform_send_flags;
}

SocketAddress* Socket::getsockname() const
{
  struct sockaddr_storage sockname_sockaddr_storage;
  memset( &sockname_sockaddr_storage, 0, sizeof( sockname_sockaddr_storage ) );
  socklen_t sockname_sockaddr_storage_len = sizeof( sockname_sockaddr_storage );
  if
  (
    ::getsockname
    (
      *this,
      reinterpret_cast<struct sockaddr*>( &sockname_sockaddr_storage ),
      &sockname_sockaddr_storage_len
    ) != -1
  )
    return new SocketAddress( sockname_sockaddr_storage );
  else
    return NULL;
}

bool Socket::is_closed() const
{
  return socket_ == -1;
}

#ifdef _WIN64
void
Socket::iovecs_to_wsabufs
(
   const struct iovec* iov,
   vector<struct iovec64>& wsabufs
)
{
  for ( uint32_t iov_i = 0; iov_i < wsabufs.size(); iov_i++ )
  {
    wsabufs[iov_i].buf = static_cast<char*>( iov[iov_i].iov_base );
    wsabufs[iov_i].len = static_cast<ULONG>( iov[iov_i].iov_len );
  }
}
#endif

bool Socket::listen()
{
  return ::listen( *this, SOMAXCONN ) != -1;
}

bool Socket::operator==( const Socket& other ) const
{
  return socket_ == other.socket_;
}

bool Socket::recreate()
{
  return recreate( AF_INET6 );
}

bool Socket::recreate( int domain )
{
  close();
  socket_ = ::socket( domain, type, protocol );
  if ( socket_ != -1 )
  {
    if ( !blocking_mode )
      set_blocking_mode( false );
    this->domain = domain;
    return true;
  }
  else
    return false;
}

ssize_t Socket::recv( Buffer& buffer, int flags )
{
  ssize_t recv_ret
    = recv
      (
        static_cast<char*>( buffer ) + buffer.size(),
        buffer.capacity() - buffer.size(),
        flags
      );

  if ( recv_ret > 0 )
    buffer.put( static_cast<size_t>( recv_ret ) );

  return recv_ret;
}

ssize_t Socket::recv( void* buf, size_t buflen, int flags )
{
  flags = get_platform_recv_flags( flags );

#ifdef _WIN32
  return ::recv
         (
           *this,
           static_cast<char*>( buf ),
           static_cast<int>( buflen ),
           flags
         ); // No real advantage to WSARecv on Win32 for one buffer
#else
  return ::recv( *this, buf, buflen, flags );
#endif
}

ssize_t Socket::send( const void* buf, size_t buflen, int flags )
{
  flags = get_platform_send_flags( flags );

#if defined(_WIN32)
  DWORD dwWrittenLength;
  WSABUF wsabuf;
  wsabuf.len = static_cast<ULONG>( buflen );
  wsabuf.buf = const_cast<char*>( static_cast<const char*>( buf ) );

  ssize_t send_ret
    = WSASend
      (
        *this,
        &wsabuf,
        1,
        &dwWrittenLength,
        static_cast<DWORD>( flags ),
        NULL,
        NULL
      );

  if ( send_ret >= 0 )
    return static_cast<ssize_t>( dwWrittenLength );
  else
    return send_ret;
#else
  return ::send( *this, buf, buflen, flags );
#endif
}

ssize_t Socket::sendmsg( const struct iovec* iov, uint32_t iovlen, int flags )
{
  flags = get_platform_send_flags( flags );

#ifdef _WIN32
  DWORD dwWrittenLength;
#ifdef _WIN64
  vector<WSABUF> wsabufs( iovlen );
  iovecs_to_wsabufs( iov, iovlen );
#endif

  ssize_t send_ret
    = WSASend
      (
        *this,
#ifdef _WIN64
        &wsabufs[0],
#else
        reinterpret_cast<WSABUF*>( const_cast<struct iovec*>( iov ) ),
#endif
        iovlen,
        &dwWrittenLength,
        static_cast<DWORD>( flags ),
        NULL,
        NULL
      );

  if ( send_ret >= 0 )
    return static_cast<ssize_t>( dwWrittenLength );
  else
    return send_ret;
#else
  struct msghdr msghdr_;
  memset( &msghdr_, 0, sizeof( msghdr_ ) );
  msghdr_.msg_iov = const_cast<iovec*>( iov );
  msghdr_.msg_iovlen = iovlen;
  return ::sendmsg( *this, &msghdr_, flags );
#endif
}

bool Socket::set_blocking_mode( bool blocking )
{
#ifdef _WIN32
  unsigned long val = blocking ? 0UL : 1UL;
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

void Socket::set_io_queue( IOQueue& io_queue )
{
  if ( this->io_queue == NULL )
    this->io_queue = &io_queue.inc_ref();
  else
    DebugBreak();
}

bool Socket::setsockopt( Option option, bool onoff )
{
  if ( option == OPTION_SO_KEEPALIVE )
  {
    int optval = onoff ? 1 : 0;
    return ::setsockopt
           (
             *this,
             SOL_SOCKET,
             SO_KEEPALIVE,
             reinterpret_cast<char*>( &optval ),
             static_cast<int>( sizeof( optval ) )
           ) == 0;
  }
  else if ( option == OPTION_SO_LINGER )
  {
    linger optval;
    optval.l_onoff = onoff ? 1 : 0;
    optval.l_linger = 0;
    return ::setsockopt
           (
             *this,
             SOL_SOCKET,
             SO_LINGER,
             reinterpret_cast<char*>( &optval ),
             static_cast<int>( sizeof( optval ) )
           ) == 0;
  }
  else
    return false;
}

bool Socket::shutdown( bool shut_rd, bool shut_wr )
{
  int how;
#ifdef _WIN32
  if ( shut_rd && shut_wr ) how = SD_BOTH;
  else if ( shut_rd ) how = SD_RECEIVE;
  else if ( shut_wr ) how = SD_SEND;
  else return false;

  if ( ::shutdown( *this, how ) == 0 )
#else
  if ( shut_rd && shut_wr ) how = SHUT_RDWR;
  else if ( shut_rd ) how = SHUT_RD;
  else if ( shut_wr ) how = SHUT_WR;
  else return false;

  if ( ::shutdown( *this, how ) != -1 )
#endif
  {
    connected = false;
    return true;
  }
  else
    return false;
}

bool Socket::want_connect() const
{
#ifdef _WIN32
  switch ( WSAGetLastError() )
  {
    case WSAEALREADY:
    case WSAEINPROGRESS:
    case WSAEINVAL:
    case WSAEWOULDBLOCK: return true;
    default: return false;
  }
#else
  switch ( errno )
  {
    case EALREADY:
    case EINPROGRESS:
    case EWOULDBLOCK: return true;
    default: return false;
  }
#endif
}

bool Socket::want_read() const
{
#ifdef _WIN32
  return WSAGetLastError() == WSAEWOULDBLOCK;
#else
  return errno == EWOULDBLOCK;
#endif
}

bool Socket::want_write() const
{
#ifdef _WIN32
  return WSAGetLastError() == WSAEWOULDBLOCK;
#else
  return errno == EWOULDBLOCK;
#endif
}



// socket_address.cpp
#ifdef _WIN32
#undef INVALID_SOCKET
#pragma warning( push )
#pragma warning( disable: 4365 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#define INVALID_SOCKET  (SOCKET)(~0)
#else
#include <arpa/inet.h>
#include <netdb.h>
#include <netinet/in.h>
#include <sys/socket.h>
#endif


SocketAddress::SocketAddress( struct addrinfo& addrinfo_list )
  : addrinfo_list( &addrinfo_list ), _sockaddr_storage( NULL )
{ }

SocketAddress::~SocketAddress()
{
  if ( addrinfo_list != NULL )
    freeaddrinfo( addrinfo_list );
  else if ( _sockaddr_storage != NULL )
    delete _sockaddr_storage;
}

SocketAddress::SocketAddress
(
  const struct sockaddr_storage& _sockaddr_storage
)
{
  addrinfo_list = NULL;
  this->_sockaddr_storage = new struct sockaddr_storage;
  memcpy_s
  (
    this->_sockaddr_storage,
    sizeof( *this->_sockaddr_storage ),
    &_sockaddr_storage,
    sizeof( _sockaddr_storage )
  );
}

SocketAddress* SocketAddress::create()
{
  return create( NULL );
}

SocketAddress* SocketAddress::create( const char* hostname )
{
  return create( hostname, 0 );
}

SocketAddress* SocketAddress::create( const char* hostname, uint16_t port )
{
  if ( hostname != NULL && strcmp( hostname, "*" ) == 0 )
    hostname = NULL;

  struct addrinfo* addrinfo_list = getaddrinfo( hostname, port );
  if ( addrinfo_list != NULL )
    return new SocketAddress( *addrinfo_list );
  else
    return NULL;
}

bool SocketAddress::as_struct_sockaddr
(
  int family,
  struct sockaddr*& out_sockaddr,
  socklen_t& out_sockaddrlen
) const
{
  if ( addrinfo_list != NULL )
  {
    struct addrinfo* addrinfo_p = addrinfo_list;
    while ( addrinfo_p != NULL )
    {
      if ( addrinfo_p->ai_family == family )
      {
        out_sockaddr = addrinfo_p->ai_addr;
        out_sockaddrlen = static_cast<socklen_t>( addrinfo_p->ai_addrlen );
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

struct addrinfo*
SocketAddress::getaddrinfo( const char* hostname, uint16_t port )
{
  const char* servname;
#ifdef __sun
  if ( hostname == NULL )
    hostname = "0.0.0.0";
  servname = NULL;
#else
  ostringstream servname_oss; // ltoa is not very portable
  servname_oss << port; // servname = decimal port or service name.
  string servname_str = servname_oss.str();
  servname = servname_str.c_str();
#endif

  struct addrinfo addrinfo_hints;
  memset( &addrinfo_hints, 0, sizeof( addrinfo_hints ) );
  addrinfo_hints.ai_family = AF_UNSPEC;
  if ( hostname == NULL )
    addrinfo_hints.ai_flags |= AI_PASSIVE; // To get INADDR_ANYs

  struct addrinfo* addrinfo_list;

  int getaddrinfo_ret
    = ::getaddrinfo( hostname, servname, &addrinfo_hints, &addrinfo_list );

#ifdef _WIN32
  if ( getaddrinfo_ret == WSANOTINITIALISED )
  {
    WORD wVersionRequested = MAKEWORD( 2, 2 );
    WSADATA wsaData;
    WSAStartup( wVersionRequested, &wsaData );

    getaddrinfo_ret
      = ::getaddrinfo( hostname, servname, &addrinfo_hints, &addrinfo_list );
  }
#endif

  if ( getaddrinfo_ret == 0 )
  {
#ifdef __sun
    struct addrinfo* addrinfo_p = addrinfo_list;
    while ( addrinfo_p != NULL )
    {
      switch ( addrinfo_p->ai_family )
      {
        case AF_INET:
        {
          reinterpret_cast<struct sockaddr_in*>( addrinfo_p->ai_addr )
            ->sin_port = htons( port );
        }
        break;

        case AF_INET6:
        {
          reinterpret_cast<struct sockaddr_in6*>( addrinfo_p->ai_addr )
            ->sin6_port = htons( port );
        }
        break;

        default: DebugBreak();
      }

      addrinfo_p = addrinfo_p->ai_next;
    }
#endif

    return addrinfo_list;
  }
  else
    return NULL;
}

bool SocketAddress::getnameinfo
(
  string& out_hostname,
  bool numeric
) const
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

bool SocketAddress::getnameinfo
(
  char* out_hostname,
  uint32_t out_hostname_len,
  bool numeric
) const
{
  if ( addrinfo_list != NULL )
  {
    struct addrinfo* addrinfo_p = addrinfo_list;
    while ( addrinfo_p != NULL )
    {
      if
      (
        ::getnameinfo
        (
          addrinfo_p->ai_addr,
          static_cast<socklen_t>( addrinfo_p->ai_addrlen ),
          out_hostname,
          out_hostname_len,
          NULL,
          0,
          numeric ? NI_NUMERICHOST : 0
        ) == 0
      )
        return true;
      else
        addrinfo_p = addrinfo_p->ai_next;
    }
    return false;
  }
  else
    return ::getnameinfo
           (
             reinterpret_cast<sockaddr*>( _sockaddr_storage ),
             static_cast<socklen_t>( sizeof( *_sockaddr_storage ) ),
             out_hostname,
             out_hostname_len,
             NULL,
             0,
             numeric ? NI_NUMERICHOST : 0
           ) == 0;
}

uint16_t SocketAddress::get_port() const
{
  if ( addrinfo_list != NULL )
  {
    switch ( addrinfo_list->ai_family )
    {
      case AF_INET:
      {
        return ntohs
               (
                 reinterpret_cast<struct sockaddr_in*>
                 (
                   addrinfo_list->ai_addr
                 )->sin_port
               );
      }

      case AF_INET6:
      {
        return ntohs
               (
                 reinterpret_cast<struct sockaddr_in6*>
                 (
                   addrinfo_list->ai_addr
                 )->sin6_port
               );
      }

      default:
      {
        DebugBreak();
        return 0;
      }
    }
  }
  else
  {
    switch ( _sockaddr_storage->ss_family )
    {
      case AF_INET:
      {
        return ntohs
               (
                 reinterpret_cast<struct sockaddr_in*>( _sockaddr_storage )
                   ->sin_port
               );
      }

      case AF_INET6:
      {
        return ntohs
               (
                 reinterpret_cast<struct sockaddr_in6*>( _sockaddr_storage )
                  ->sin6_port
               );
      }

      default:
      {
        DebugBreak();
        return 0;
      }
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
          if
          (
            addrinfo_p->ai_addrlen == other_addrinfo_p->ai_addrlen &&
            memcmp
            (
              addrinfo_p->ai_addr,
              other_addrinfo_p->ai_addr,
              addrinfo_p->ai_addrlen
            ) == 0 &&
            addrinfo_p->ai_family == other_addrinfo_p->ai_family &&
            addrinfo_p->ai_protocol == other_addrinfo_p->ai_protocol &&
            addrinfo_p->ai_socktype == other_addrinfo_p->ai_socktype
          )
            break;
          else
            other_addrinfo_p = other_addrinfo_p->ai_next;
        }

        if ( other_addrinfo_p != NULL ) // i.e. we found the addrinfo
                                        // in the other's list
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
    return memcmp
           (
             _sockaddr_storage,
             other._sockaddr_storage,
             sizeof( *_sockaddr_storage )
           ) == 0;
  else
    return false;
}


// socket_pair.cpp
#ifdef _WIN32
#undef INVALID_SOCKET
#pragma warning( push )
#pragma warning( disable: 4365 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#define INVALID_SOCKET  (SOCKET)(~0)
#else
#include <sys/socket.h>
#endif


SocketPair::SocketPair( Socket& first_socket, Socket& second_socket )
  : first_socket( first_socket ), second_socket( second_socket )
{ }

SocketPair::~SocketPair()
{
  Socket::dec_ref( first_socket );
  Socket::dec_ref( second_socket );
}

SocketPair& SocketPair::create()
{
  socket_t sv[2];

#ifdef WIN32
  socket_t listen_socket = ::socket( AF_INET, SOCK_STREAM, IPPROTO_TCP );

  if
  (
    listen_socket == INVALID_SOCKET
    &&
    WSAGetLastError() == WSANOTINITIALISED
  )
  {
    WORD wVersionRequested = MAKEWORD( 2, 2 );
    WSADATA wsaData;
    WSAStartup( wVersionRequested, &wsaData );

    listen_socket = ::socket( AF_INET, SOCK_STREAM, 0 );
  }

  if ( listen_socket != INVALID_SOCKET )
  {
    struct sockaddr_in addr;
    socklen_t addr_len = sizeof( addr );
    memset( &addr, 0, sizeof( addr ) );
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = htonl( 0x7f000001 );
    addr.sin_port = 0;

    if
    (
      ::bind( listen_socket, ( struct sockaddr* )&addr, sizeof( addr ) )!= -1
      &&
      ::listen( listen_socket, 1 ) != -1
      &&
      ::getsockname( listen_socket, ( struct sockaddr* )&addr, &addr_len )
        != -1
    )
    {
      sv[0] = ::socket( AF_INET, SOCK_STREAM, 0 );
      if ( sv[0] != INVALID_SOCKET )
      {
        if
        (
          ::connect
          (
            sv[0],
            ( struct sockaddr* )&addr,
            sizeof( addr )
          ) != -1
          &&
          (
            sv[1]
            = ::accept( listen_socket, NULL, NULL )
          ) != -1
        )
        {
          closesocket( listen_socket );

          Socket* first_socket
            = new Socket( AF_INET, SOCK_STREAM, IPPROTO_TCP, sv[0] );

          Socket* second_socket
            = new Socket( AF_INET, SOCK_STREAM, IPPROTO_TCP, sv[1] );

          return *new SocketPair( *first_socket, *second_socket );
        }
        else
        {
          closesocket( sv[0] );
          closesocket( listen_socket );
        }
      }
      else
        closesocket( listen_socket );
    }
    else
      closesocket( listen_socket );
  }

  throw Exception();
#else
  if ( socketpair( AF_UNIX, SOCK_STREAM, 0, sv ) != -1 )
  {
    Socket* first_socket = new Socket( AF_UNIX, SOCK_STREAM, 0, sv[0] );
    Socket* second_socket = new Socket( AF_UNIX, SOCK_STREAM, 0, sv[1] );
    return *new SocketPair( *first_socket, *second_socket );
  }
  else
    throw Exception();
#endif
}



// stat.cpp
#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif


Stat::Stat()
:
#ifndef _WIN32
  dev( static_cast<dev_t>( -1 ) ),
  ino( static_cast<ino_t>( -1 ) ),
#endif
  mode( static_cast<mode_t>( -1 ) ),
  nlink( static_cast<nlink_t>( -1 ) ),
#ifndef _WIN32
  uid( static_cast<uid_t>( -1 ) ),
  gid( static_cast<gid_t>( -1 ) ),
  rdev( static_cast<dev_t>( -1 ) ),
#endif
  size( static_cast<uint64_t>( -1 ) ),
  atime( static_cast<uint64_t>( 0 ) ),
  mtime( static_cast<uint64_t>( 0 ) ),
  ctime( static_cast<uint64_t>( 0 ) ),
#ifndef _WIN32
  blksize( static_cast<blksize_t>( -1 ) ),
  blocks( static_cast<blkcnt_t>( -1 ) )
#else
  attributes( static_cast<uint32_t>( -1 ) )
#endif
{ }

Stat::Stat( const Stat& stbuf )
:
#ifndef _WIN32
  dev( stbuf.get_dev() ),
  ino( stbuf.get_ino() ),
#endif
  mode( stbuf.get_mode() ),
  nlink( stbuf.get_nlink() ),
#ifndef _WIN32
  uid( stbuf.get_uid() ),
  gid( stbuf.get_gid() ),
  rdev( stbuf.get_rdev() ),
#endif
  size( stbuf.get_size() ),
  atime( stbuf.get_atime() ),
  mtime( stbuf.get_mtime() ),
  ctime( stbuf.get_ctime() ),
#ifndef _WIN32
  blksize( stbuf.get_blksize() ),
  blocks( stbuf.get_blocks() )
#else
  attributes( stbuf.get_attributes() )
#endif
{ }

#ifdef _WIN32
Stat::Stat
(
  mode_t mode,
  nlink_t nlink,
  uint64_t size,
  const Time& atime,
  const Time& mtime,
  const Time& ctime,
  uint32_t attributes
)
  : mode( mode ),
    nlink( nlink ),
    size( size ),
    atime( atime ),
    mtime( mtime ),
    ctime( ctime ),
    attributes( attributes )
{ }

Stat::Stat( const BY_HANDLE_FILE_INFORMATION& bhfi )
  : mode
    (
      ( bhfi.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY ) ?
      S_IFDIR :
      S_IFREG
    ),
    nlink( static_cast<nlink_t>( bhfi.nNumberOfLinks ) ),
    atime( bhfi.ftLastAccessTime ),
    mtime( bhfi.ftLastWriteTime ),
    ctime( bhfi.ftCreationTime ),
    attributes( bhfi.dwFileAttributes )
{
  ULARGE_INTEGER size;
  size.LowPart = bhfi.nFileSizeLow;
  size.HighPart = bhfi.nFileSizeHigh;
  this->size = static_cast<size_t>( size.QuadPart );
}

Stat::Stat( const WIN32_FIND_DATA& find_data )
  : mode
    (
      ( find_data.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY ) ?
      S_IFDIR :
      S_IFREG
    ),
    nlink( 1 ), // WIN32_FIND_DATA doesn't have a nNumberOfLinks
    atime( find_data.ftLastAccessTime ),
    mtime( find_data.ftLastWriteTime ),
    ctime( find_data.ftCreationTime ),
    attributes( find_data.dwFileAttributes )
{
  ULARGE_INTEGER size;
  size.LowPart = find_data.nFileSizeLow;
  size.HighPart = find_data.nFileSizeHigh;
  this->size = static_cast<size_t>( size.QuadPart );
}

Stat::Stat
(
  uint32_t nNumberOfLinks,
  uint32_t nFileSizeHigh,
  uint32_t nFileSizeLow,
  const FILETIME* ftLastAccessTime,
  const FILETIME* ftLastWriteTime,
  const FILETIME* ftCreationTime,
  uint32_t dwFileAttributes
)
  : mode
    (
      ( dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY ) ? S_IFDIR : S_IFREG
    ),
    nlink( static_cast<nlink_t>( nNumberOfLinks ) ),
    atime( ftLastAccessTime ),
    mtime( ftLastWriteTime ),
    ctime( ftCreationTime ),
    attributes( dwFileAttributes )
{
  ULARGE_INTEGER size;
  size.LowPart = nFileSizeLow;
  size.HighPart = nFileSizeHigh;
  this->size = static_cast<size_t>( size.QuadPart );
}
#else
Stat::Stat
(
  dev_t dev,
  ino_t ino,
  mode_t mode,
  nlink_t nlink,
  uid_t uid,
  gid_t gid,
  dev_t rdev,
  uint64_t size,
  const Time& atime,
  const Time& mtime,
  const Time& ctime,
  blksize_t blksize,
  blkcnt_t blocks
)
: dev( dev ),
  ino( ino ),
  mode( mode ),
  nlink( nlink ),
  uid( uid ),
  gid( gid ),
  rdev( rdev ),
  size( size ),
  atime( atime ),
  mtime( mtime ),
  ctime( ctime ),
  blksize( blksize ),
  blocks( blocks )
{ }
#endif

Stat::Stat( const struct stat& stbuf )
  :
#ifndef _WIN32
    dev( stbuf.st_dev ),
    ino( stbuf.st_ino ),
#endif
    mode( stbuf.st_mode ),
    nlink( stbuf.st_nlink ),
#ifndef _WIN32
    uid( stbuf.st_uid ),
    gid( stbuf.st_gid ),
    rdev( stbuf.st_rdev ),
#endif
    size( stbuf.st_size ),
    atime( static_cast<double>( stbuf.st_atime ) ),
    mtime( static_cast<double>( stbuf.st_mtime ) ),
    ctime( static_cast<double>( stbuf.st_ctime ) )
#ifndef _WIN32
    , blksize( stbuf.st_blksize ),
    blocks( stbuf.st_blocks )
#endif
{ }

#ifdef _WIN32
uint32_t Stat::get_attributes() const
{
#ifdef _WIN32
  DWORD dwFileAttributes = attributes;
  if ( ( get_mode() & S_IFREG ) == S_IFREG )
    dwFileAttributes |= FILE_ATTRIBUTE_NORMAL;
  else if ( ( get_mode() & S_IFDIR ) == S_IFDIR )
    dwFileAttributes |= FILE_ATTRIBUTE_DIRECTORY;
#ifdef _DEBUG
  if ( dwFileAttributes == 0 )
    DebugBreak();
#endif
  return dwFileAttributes;
#else
  return attributes;
#endif
}
#endif

Stat& Stat::operator=( const Stat& other )
{
#ifndef _WIN32
  set_dev( other.get_dev() );
  set_ino( other.get_ino() );
#endif
  set_mode( other.get_mode() );
  set_nlink( other.get_nlink() );
#ifndef _WIN32
  set_uid( other.get_uid() );
  set_gid( other.get_gid() );
  set_rdev( other.get_rdev() );
#endif
  set_size( other.get_size() );
  set_atime( other.get_atime() );
  set_mtime( other.get_mtime() );
  set_ctime( other.get_ctime() );
#ifdef _WIN32
  set_attributes( other.get_attributes() );
#else
  set_blksize( other.get_blksize() );
  set_blocks( other.get_blocks() );
#endif
  return *this;
}

bool Stat::operator==( const Stat& other ) const
{
  return
#ifndef _WIN32
         get_dev() == other.get_dev() &&
         get_ino() == other.get_ino() &&
#endif
         get_mode() == other.get_mode() &&
         get_nlink() == other.get_nlink() &&
#ifndef _WIN32
         get_uid() == other.get_uid() &&
         get_gid() == other.get_gid() &&
         get_rdev() == other.get_rdev() &&
#endif
         get_size() == other.get_size() &&
         get_atime() == other.get_atime() &&
         get_mtime() == other.get_mtime() &&
         get_ctime() == other.get_ctime() &&
#ifdef _WIN32
         get_attributes() == other.get_attributes();
#else
         get_blksize() == other.get_blksize() &&
         get_blocks() == other.get_blocks();
#endif
}

Stat::operator struct stat() const
{
  struct stat stbuf;
  memset( &stbuf, 0, sizeof( stbuf ) );
#ifndef _WIN32
  stbuf.st_dev = get_dev();
  stbuf.st_ino = get_ino();
#endif
  stbuf.st_mode = get_mode();
  stbuf.st_nlink = get_nlink();
#ifndef _WIN32
  stbuf.st_uid = get_uid();
  stbuf.st_gid = get_gid();
  stbuf.st_rdev = get_rdev();
#endif
  stbuf.st_size = static_cast<off_t>( get_size() );
  stbuf.st_atime = static_cast<time_t>( get_atime().as_unix_time_s() );
  stbuf.st_mtime = static_cast<time_t>( get_mtime().as_unix_time_s() );
  stbuf.st_ctime = static_cast<time_t>( get_ctime().as_unix_time_s() );
#ifndef _WIN32
  stbuf.st_blksize = get_blksize();
  stbuf.st_blocks = get_blocks();
#endif
  return stbuf;
}

#ifdef _WIN32
Stat::operator BY_HANDLE_FILE_INFORMATION() const
{
  BY_HANDLE_FILE_INFORMATION bhfi;
  memset( &bhfi, 0, sizeof( bhfi ) );
  bhfi.dwFileAttributes = get_attributes();
  bhfi.ftCreationTime = get_ctime();
  bhfi.ftLastWriteTime = get_mtime();
  bhfi.ftLastWriteTime = get_mtime();
  ULARGE_INTEGER size; size.QuadPart = get_size();
  bhfi.nFileSizeLow = size.LowPart;
  bhfi.nFileSizeHigh = size.HighPart;
  bhfi.nNumberOfLinks = get_nlink();
  return bhfi;
}

Stat::operator WIN32_FIND_DATA() const
{
  WIN32_FIND_DATA find_data;
  memset( &find_data, 0, sizeof( find_data ) );
  find_data.dwFileAttributes = get_attributes();
  find_data.ftCreationTime = get_ctime();
  find_data.ftLastAccessTime = get_atime();
  find_data.ftLastWriteTime = get_mtime();
  ULARGE_INTEGER size; size.QuadPart = get_size();
  find_data.nFileSizeLow = size.LowPart;
  find_data.nFileSizeHigh = size.HighPart;
  return find_data;
}
#endif

void Stat::set( const Stat& other, uint32_t to_set )
{
  if ( ( to_set & Volume::SETATTR_MODE ) == Volume::SETATTR_MODE )
    set_mode( other.get_mode() );

#ifndef _WIN32
  if ( ( to_set & Volume::SETATTR_UID ) == Volume::SETATTR_UID )
    set_uid( other.get_uid() );

  if ( ( to_set & Volume::SETATTR_GID ) == Volume::SETATTR_GID )
    set_gid( other.get_gid() );
#endif

  if ( ( to_set & Volume::SETATTR_SIZE ) == Volume::SETATTR_SIZE )
    set_size( other.get_size() );

  if ( ( to_set & Volume::SETATTR_ATIME ) == Volume::SETATTR_ATIME )
    set_atime( other.get_atime() );

  if ( ( to_set & Volume::SETATTR_MTIME ) == Volume::SETATTR_MTIME )
    set_mtime( other.get_mtime() );

  if ( ( to_set & Volume::SETATTR_CTIME ) == Volume::SETATTR_CTIME )
    set_ctime( other.get_ctime() );

#ifdef _WIN32
  if ( ( to_set & Volume::SETATTR_ATTRIBUTES ) == Volume::SETATTR_ATTRIBUTES )
    set_attributes( other.get_attributes() );
#endif
}

#ifndef _WIN32
void Stat::set_dev( dev_t dev )
{
  this->dev = dev;
}

void Stat::set_ino( ino_t ino )
{
  this->ino = ino;
}
#endif

void Stat::set_mode( mode_t mode )
{
  this->mode = mode;
}

void Stat::set_nlink( nlink_t nlink )
{
  this->nlink = nlink;
}

#ifndef _WIN32
void Stat::set_uid( uid_t uid )
{
  this->uid = uid;
}

void Stat::set_gid( gid_t gid )
{
  this->gid = gid;
}

void Stat::set_rdev( dev_t )
{
  this->rdev = rdev;
}
#endif

void Stat::set_size( uint64_t size )
{
  this->size = size;
}

void Stat::set_atime( const Time& atime )
{
  this->atime = atime;
}

void Stat::set_mtime( const Time& mtime )
{
  this->mtime = mtime;
}

void Stat::set_ctime( const Time& ctime )
{
  this->ctime = ctime;
}
#ifdef _WIN32
void Stat::set_attributes( uint32_t attributes )
{
  this->attributes = attributes;
}
#else
void Stat::set_blksize( blksize_t blksize )
{
  this->blksize = blksize;
}

void Stat::set_blocks( blkcnt_t blocks )
{
  this->blocks = blocks;
}
#endif

#ifdef _WIN32
#pragma warning( pop )
#endif


// stream.cpp
#ifdef _WIN32
#undef INVALID_SOCKET
#pragma warning( push )
#pragma warning( disable: 4365 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#define INVALID_SOCKET  (SOCKET)(~0)
#endif


bool Stream::close( fd_t fd )
{
  if ( fd != INVALID_FD )
#ifdef _WIN32
    return CloseHandle( fd ) == TRUE;
#else
    return ::close( fd ) != -1;
#endif
  else
  {
#ifdef _WIN32
    SetLastError( ERROR_INVALID_HANDLE );
#else
    errno = EBADF;
#endif
    return false;
  }
}

#ifdef _WIN32
bool Stream::close( socket_t socket_ )
{
  if ( socket_ != INVALID_SOCKET )
    return ::closesocket( socket_ ) != -1;
  else
  {
#ifdef _WIN32
    WSASetLastError( WSAENOTSOCK );
#else
    errno = EBADF;
#endif
    return false;
  }
}
#endif

#ifdef _WIN32
bool Stream::set_blocking_mode( bool blocking, socket_t socket_ )
{
  unsigned long val = blocking ? 0UL : 1UL;
  return ::ioctlsocket( socket_, FIONBIO, &val ) != SOCKET_ERROR;
}
#else
bool Stream::set_blocking_mode( bool blocking, fd_t fd )
{
  int current_fcntl_flags = fcntl( fd, F_GETFL, 0 );
  if ( blocking )
  {
    if ( ( current_fcntl_flags & O_NONBLOCK ) == O_NONBLOCK )
      return fcntl( fd, F_SETFL, current_fcntl_flags ^ O_NONBLOCK ) != -1;
    else
      return true;
  }
  else
    return fcntl( fd, F_SETFL, current_fcntl_flags | O_NONBLOCK ) != -1;
}
#endif



// tcp_socket.cpp
#if defined(_WIN32)
#undef INVALID_SOCKET
#undef WSABUF
#pragma warning( push )
#pragma warning( disable: 4365 4995 )
#include <ws2tcpip.h>
#include <mswsock.h>
#pragma warning( pop )
#define INVALID_SOCKET  (SOCKET)(~0)
#define ECONNABORTED WSAECONNABORTED
#else
#include <netinet/in.h> // For the IPPROTO_* constants
#include <netinet/tcp.h> // For the TCP_* constants
#include <sys/socket.h>
#endif


int TCPSocket::PROTOCOL = IPPROTO_TCP;
int TCPSocket::TYPE = SOCK_STREAM;

#ifdef _WIN32
void* TCPSocket::lpfnAcceptEx = NULL;
void* TCPSocket::lpfnConnectEx = NULL;
#endif


TCPSocket::IOAcceptCB::IOAcceptCB
(
  TCPSocket& listen_tcp_socket,
  AIOAcceptCallback& callback,
  void* callback_context
)
  : IOCB<AIOAcceptCallback>( callback, callback_context ),
    listen_tcp_socket( listen_tcp_socket )
{ }

TCPSocket::IOAcceptCB::~IOAcceptCB()
{
  TCPSocket::dec_ref( listen_tcp_socket );
}

void TCPSocket::IOAcceptCB::onAcceptCompletion( TCPSocket& accepted_tcp_socket )
{
  callback.onAcceptCompletion( accepted_tcp_socket, callback_context );
}

void TCPSocket::IOAcceptCB::onAcceptError()
{
  onAcceptError( get_last_error() );
}

void TCPSocket::IOAcceptCB::onAcceptError( uint32_t error_code )
{
  callback.onAcceptError( error_code, callback_context );
}


class TCPSocket::BIOAcceptCB : public BIOCB, public IOAcceptCB
{
public:
  BIOAcceptCB
  (
    TCPSocket& listen_tcp_socket,
    AIOAcceptCallback& callback,
    void* callback_context
  ) : IOAcceptCB( listen_tcp_socket, callback, callback_context )
  { }

  // BIOCB
  void execute()
  {
    if ( get_listen_tcp_socket().set_blocking_mode( true ) )
    {
      TCPSocket* accepted_tcp_socket = get_listen_tcp_socket().accept();
      if ( accepted_tcp_socket != NULL )
      {
        onAcceptCompletion( *accepted_tcp_socket );
        TCPSocket::dec_ref( *accepted_tcp_socket );
      }
      else
        onAcceptError();
    }
    else
      onAcceptError();
  }
};


class TCPSocket::NBIOAcceptCB : public NBIOCB, public IOAcceptCB
{
public:
  NBIOAcceptCB
  (
    TCPSocket& listen_tcp_socket,
    AIOAcceptCallback& callback,
    void* callback_context
  ) : NBIOCB( STATE_WANT_READ ),
      IOAcceptCB( listen_tcp_socket, callback, callback_context )
  { }

  // NBIOCB
  void execute()
  {
    if ( get_listen_tcp_socket().set_blocking_mode( false ) )
    {
      TCPSocket* accepted_tcp_socket = get_listen_tcp_socket().accept();
      if ( accepted_tcp_socket != NULL )
      {
        set_state( STATE_COMPLETE );
        onAcceptCompletion( *accepted_tcp_socket );
        TCPSocket::dec_ref( *accepted_tcp_socket );
      }
      else if ( get_listen_tcp_socket().want_accept() )
        set_state( STATE_WANT_READ );
      else
      {
        set_state( STATE_ERROR );
        onAcceptError();
      }
    }
    else
    {
      set_state( STATE_ERROR );
      onAcceptError();
    }
  }

  socket_t get_fd() const { return get_listen_tcp_socket(); }
};


#ifdef _WIN32
class TCPSocket::Win32AIOAcceptCB : public Win32AIOCB, public IOAcceptCB
{
public:
  Win32AIOAcceptCB
  (
    TCPSocket& accepted_tcp_socket,
    TCPSocket& listen_tcp_socket,
    AIOAcceptCallback& callback,
    void* callback_context
  ) : IOAcceptCB( listen_tcp_socket, callback, callback_context ),
      accepted_tcp_socket( accepted_tcp_socket )
  { }

  // Win32AIOCB
  void onCompletion( DWORD )
  {
    onAcceptCompletion( accepted_tcp_socket );
  }

  void onError( DWORD dwErrorCode )
  {
    onAcceptError( static_cast<uint32_t>( dwErrorCode ) );
  }


  TCPSocket& accepted_tcp_socket;
  char peername[88]; // ( sizeof( sockaddr_in6 ) + 16 ) * 2
};


class TCPSocket::Win32AIOConnectCB : public Win32AIOCB, IOConnectCB
{
public:
  Win32AIOConnectCB
  (
    SocketAddress& peername,
    Socket& socket_,
    AIOConnectCallback& callback,
    void* callback_context
  ) : IOConnectCB( peername, socket_, callback, callback_context )
  { }

  // Win32AIOCB
  void onCompletion( DWORD )
  {
    onConnectCompletion();
  }

  void onError( DWORD dwErrorCode )
  {
    onConnectError( static_cast<uint32_t>( dwErrorCode ) );
  }
};

class TCPSocket::Win32AIORecvCB : public Win32AIOCB, public IORecvCB
{
public:
  Win32AIORecvCB
  (
    Buffer& buffer,
    Socket& socket_,
    AIOReadCallback& callback,
    void* callback_context
  )
    : IORecvCB( buffer, 0, socket_, callback, callback_context )
  { }

  // Win32AIOCB
  void onCompletion( DWORD dwNumberOfBytesTransferred )
  {
    if ( dwNumberOfBytesTransferred > 0 )
    {
      get_buffer().put( dwNumberOfBytesTransferred );
      onReadCompletion();
    }
    else
      onReadError( ECONNABORTED );
  }

  void onError( DWORD dwErrorCode )
  {
    onReadError( static_cast<uint32_t>( dwErrorCode ) );
  }
};


class TCPSocket::Win32AIOSendCB : public Win32AIOCB, public IOSendCB
{
public:
  Win32AIOSendCB
  (
    Buffer& buffer,
    Socket& socket_,
    AIOWriteCallback& callback,
    void* callback_context
  )
    : IOSendCB( buffer, 0, socket_, callback, callback_context )
  { }

  // Win32AIOCB
  void onCompletion( DWORD dwNumberOfBytesTransferred )
  {
#ifdef _WIN32
    if ( dwNumberOfBytesTransferred != get_buffer().size() )
      DebugBreak();
#endif

    onWriteCompletion();
  }

  void onError( DWORD dwErrorCode )
  {
    onWriteError( dwErrorCode );
  }
};


class TCPSocket::Win32AIOSendMsgCB : public Win32AIOCB, public IOSendMsgCB
{
public:
  Win32AIOSendMsgCB
  (
    Buffers& buffers,
    Socket& socket_,
    AIOWriteCallback& callback,
    void* callback_context
  )
    : IOSendMsgCB( buffers, 0, socket_, callback, callback_context )
  { }

  // Win32AIOCB
  void onCompletion( DWORD dwNumberOfBytesTransferred )
  {
#ifdef _DEBUG
    if ( dwNumberOfBytesTransferred < get_buffers_len() )
      DebugBreak();
#endif

    onWriteCompletion();
  }

  void onError( DWORD dwErrorCode )
  {
    onWriteError( dwErrorCode );
  }
};
#endif


TCPSocket::TCPSocket( int domain, socket_t socket_ )
  : Socket( domain, TYPE, PROTOCOL, socket_ )
{ }

TCPSocket* TCPSocket::accept()
{
  socket_t peer_socket = _accept();
  if ( peer_socket != -1 )
    return new TCPSocket( get_domain(), peer_socket );
  else
    return NULL;
}

socket_t TCPSocket::_accept()
{
  sockaddr_storage peername;
  socklen_t peername_len = sizeof( peername );
  return ::accept
         (
           *this,
           reinterpret_cast<struct sockaddr*>( &peername ),
           &peername_len
         );
}

void
TCPSocket::aio_accept
(
  AIOAcceptCallback& callback,
  void* callback_context
)
{
#ifdef _WIN32
  if
  (
    get_io_queue() != NULL
    &&
    get_io_queue()->get_type_id() == Win32AIOQueue::TYPE_ID
  )
  {
    if ( lpfnAcceptEx == NULL )
    {
      GUID GuidAcceptEx = WSAID_ACCEPTEX;
      DWORD dwBytes;
      WSAIoctl
      (
        *this,
        SIO_GET_EXTENSION_FUNCTION_POINTER,
        &GuidAcceptEx,
        sizeof( GuidAcceptEx ),
        &lpfnAcceptEx,
        sizeof( lpfnAcceptEx ),
        &dwBytes,
        NULL,
        NULL
      );
    }

    TCPSocket* accepted_tcp_socket = TCPSocket::create( get_domain() );
    if ( accepted_tcp_socket != NULL )
    {
      DWORD peername_len;
      if ( get_domain() == AF_INET6 )
        peername_len = sizeof( sockaddr_in6 );
      else
        peername_len = sizeof( sockaddr_in );

      DWORD dwBytesReceived;

      Win32AIOAcceptCB* aiocb
        = new Win32AIOAcceptCB
              (
                *accepted_tcp_socket,
                inc_ref(),
                callback,
                callback_context
              );

      if
      (
        static_cast<LPFN_ACCEPTEX>( lpfnAcceptEx )
        (
          *this,
          *accepted_tcp_socket,
          aiocb->peername,
          0,
          peername_len + 16,
          peername_len + 16,
          &dwBytesReceived,
          *aiocb
        )
        ||
        WSAGetLastError() == WSA_IO_PENDING
      )
        return;
      else
        delete aiocb;
    }
    // else the TCPSocket::create failed

    callback.onAcceptError( get_last_error(), callback_context );

    return;
  }
#endif

  // Try a non-blocking accept first
  if ( set_blocking_mode( false ) )
  {
    TCPSocket* accepted_tcp_socket = accept();
    if ( accepted_tcp_socket != NULL )
    {
      callback.onAcceptCompletion( *accepted_tcp_socket, callback_context );
      TCPSocket::dec_ref( *accepted_tcp_socket );
      return;
    }
    else if ( !want_accept() )
    {
      callback.onAcceptError( get_last_error(), callback_context );
      return;
    }
  }

  // Next try to offload the accept to an IOQueue
  if ( get_io_queue() != NULL )
  {
    switch ( get_io_queue()->get_type_id() )
    {
      case BIOQueue::TYPE_ID:
      {
        static_cast<BIOQueue*>( get_io_queue() )
          ->submit
            (
              *new BIOAcceptCB( inc_ref(), callback, callback_context )
            );
      }
      return;

      case NBIOQueue::TYPE_ID:
      {
        static_cast<NBIOQueue*>( get_io_queue() )
          ->submit
            (
              *new NBIOAcceptCB( inc_ref(), callback, callback_context )
            );
      }
      return;
    }
  }

  // Give up instead of blocking on accept
  callback.onAcceptError( get_last_error(), callback_context );
}

void
TCPSocket::aio_connect
(
  SocketAddress& peername,
  AIOConnectCallback& callback,
  void* callback_context
)
{
#ifdef _WIN32
  if
  (
    get_io_queue() != NULL
    &&
    get_io_queue()->get_type_id() == Win32AIOQueue::TYPE_ID
  )
  {
    if ( lpfnConnectEx == NULL )
    {
      GUID GuidConnectEx = WSAID_CONNECTEX;
      DWORD dwBytes;
      WSAIoctl
      (
        *this,
        SIO_GET_EXTENSION_FUNCTION_POINTER,
        &GuidConnectEx,
        sizeof( GuidConnectEx ),
        &lpfnConnectEx,
        sizeof( lpfnConnectEx ),
        &dwBytes,
        NULL,
        NULL
      );
    }

    for ( ;; )
    {
      struct sockaddr* name; socklen_t namelen;
      if ( peername.as_struct_sockaddr( get_domain(), name, namelen ) )
      {
        SocketAddress* ephemeral_sockname = SocketAddress::create();

        if ( ephemeral_sockname != NULL )
        {
          if ( bind( *ephemeral_sockname ) )
          {
            DWORD dwBytesSent;

            Win32AIOConnectCB* aiocb
              = new Win32AIOConnectCB
                    (
                      peername.inc_ref(),
                      inc_ref(),
                      callback,
                      callback_context
                    );

            if
            (
              static_cast<LPFN_CONNECTEX>( lpfnConnectEx )
              (
                *this,
                name,
                namelen,
                NULL,
                0,
                &dwBytesSent,
                *aiocb
              )
              ||
              WSAGetLastError() == WSA_IO_PENDING
            )
              return;
            else
            {
              delete aiocb;
              break;
            }
          }
          else
          {
            SocketAddress::dec_ref( *ephemeral_sockname );
            break;
          }
        }
        else
          break;
      }
      else if ( get_domain() == AF_INET6 )
      {
        if ( recreate( AF_INET ) )
          continue;
        else
          break;
      }
      else
        break;
    }

    callback.onConnectError( get_last_error(), callback_context );

    return;
  }
#endif

  Socket::aio_connect( peername, callback, callback_context );
}

void TCPSocket::aio_recv
(
  Buffer& buffer,
  int flags,
  AIOReadCallback& callback,
  void* callback_context
)
{
#ifdef _WIN32
  if
  (
    get_io_queue() != NULL
    &&
    get_io_queue()->get_type_id() == Win32AIOQueue::TYPE_ID
  )
  {
    WSABUF wsabuf[1];
    wsabuf[0].buf = static_cast<char*>( buffer ) + buffer.size();
    wsabuf[0].len = static_cast<ULONG>( buffer.capacity() - buffer.size() );

    DWORD dwNumberOfBytesReceived,
          dwFlags = static_cast<DWORD>( get_platform_recv_flags( flags ) );

    Win32AIORecvCB* aiocb
      = new Win32AIORecvCB
            (
              buffer,
              inc_ref(),
              callback,
              callback_context
            );

    if
    (
      WSARecv
      (
        *this,
        wsabuf,
        1,
        &dwNumberOfBytesReceived,
        &dwFlags,
        *aiocb,
        NULL // Win32AIORecvCB::WSAOverlappedCompletionRoutine
      ) == 0
      ||
      WSAGetLastError() == WSA_IO_PENDING
    )
      return;
    else
    {
      delete aiocb;
      callback.onReadError( get_last_error(), callback_context );
      return;
    }
  }
#endif

  Socket::aio_recv( buffer, flags, callback, callback_context );
}

void
TCPSocket::aio_send
(
  Buffer& buffer,
  int flags,
  AIOWriteCallback& callback,
  void* callback_context
)
{
  // Don't translate flags here, since they'll be translated again
  // on the ->send call (except on Win32)

#ifdef _WIN32
  if
  (
    get_io_queue() != NULL
    &&
    get_io_queue()->get_type_id() == Win32AIOQueue::TYPE_ID
  )
  {
#ifdef _WIN32
    struct iovec wsabuf = buffer;
#else
    struct iovec64 wsabuf = buffer;
#endif

    DWORD dwNumberOfBytesSent;

    Win32AIOSendCB* aiocb
      = new Win32AIOSendCB
            (
              buffer,
              inc_ref(),
              callback,
              callback_context
            );

    if
    (
      WSASend
      (
        *this,
        reinterpret_cast<LPWSABUF>( &wsabuf ),
        1,
        &dwNumberOfBytesSent,
        static_cast<DWORD>( get_platform_send_flags( flags ) ),
        *aiocb,
        NULL // Win32AIOSendCB::WSAOverlappedCompletionRoutine
      ) == 0
      ||
      WSAGetLastError() == WSA_IO_PENDING
    )
      return;
    else
    {
      delete aiocb;
      callback.onWriteError( get_last_error(), callback_context );
      return;
    }
  }
#endif

  Socket::aio_send( buffer, flags, callback, callback_context );
}

void
TCPSocket::aio_sendmsg
(
  Buffers& buffers,
  int flags,
  AIOWriteCallback& callback,
  void* callback_context
)
{
  // Don't translate flags here, since they'll be translated again
  // on the ->sendmsg call (except on Win32)

#ifdef _WIN32
  if
  (
    get_io_queue() != NULL
    &&
    get_io_queue()->get_type_id() == Win32AIOQueue::TYPE_ID
  )
  {
    DWORD dwNumberOfBytesSent;

    Win32AIOSendMsgCB* aiocb
      = new Win32AIOSendMsgCB( buffers, inc_ref(), callback, callback_context );

#ifdef _WIN64
    vector<iovec64> wsabufs( buffers->get_iovecs_len() );
    iovecs_to_wsabufs( buffers->get_iovecs(), wsabufs );
#endif

    if
    (
      WSASend
      (
        *this,
#ifdef _WIN64
        reinterpret_cast<LPWSABUF>( &wsabufs[0] ),
#else
        reinterpret_cast<LPWSABUF>
        (
          const_cast<struct iovec*>
          (
            static_cast<const struct iovec*>( buffers )
          )
        ),
#endif
        buffers.size(),
        &dwNumberOfBytesSent,
        static_cast<DWORD>( get_platform_send_flags( flags ) ),
        *aiocb,
        NULL // Win32AIOSendMsgCB::WSAOverlappedCompletionRoutine
      ) == 0
      ||
      WSAGetLastError() == WSA_IO_PENDING
    )
      return;
    else
    {
      delete aiocb;
      callback.onWriteError( get_last_error(), callback_context );
      return;
    }
  }
#endif

  Socket::aio_sendmsg( buffers, flags, callback, callback_context );
}

bool TCPSocket::associate( IOQueue& io_queue )
{
  if ( get_io_queue() == NULL )
  {
    switch ( io_queue.get_type_id() )
    {
#ifdef _WIN32
      case Win32AIOQueue::TYPE_ID:
      {
        if
        (
          static_cast<Win32AIOQueue&>( io_queue ).associate( *this )
        )
        {
          set_io_queue( io_queue );
          return true;
        }
        else
          return false;
      }
      break;
#endif

      default: return Socket::associate( io_queue );
    }
  }
  else
    return false;
}

TCPSocket* TCPSocket::create()
{
  return create( DOMAIN_DEFAULT );
}

TCPSocket* TCPSocket::create( int domain )
{
  socket_t socket_ = create( &domain );
  if ( socket_ != -1 )
    return new TCPSocket( domain, socket_ );
  else
    return NULL;
}

socket_t TCPSocket::create( int* domain )
{
  return Socket::create( domain, TYPE, PROTOCOL );
}

bool TCPSocket::want_accept() const
{
#ifdef _WIN32
  return WSAGetLastError() == WSAEWOULDBLOCK;
#else
  return errno == EWOULDBLOCK;
#endif
}

bool TCPSocket::setsockopt( Option option, bool onoff )
{
  if ( option == OPTION_TCP_NODELAY )
  {
    int optval = onoff ? 1 : 0;
    return ::setsockopt
           (
             *this,
             IPPROTO_TCP,
             TCP_NODELAY,
             reinterpret_cast<char*>( &optval ),
             static_cast<int>( sizeof( optval ) )
           ) == 0;
  }
  else
    return Socket::setsockopt( option, onoff );
}


// thread.cpp
#ifdef _WIN32
#include <windows.h>
#else
#if defined(__linux__)
#include <sched.h>
#include <sys/syscall.h>
#elif defined(__sun)
#include <thread.h>
#include <sys/processor.h>
#include <sys/pset.h>
#endif
#endif


Thread::Thread()
{
  handle = 0;
  id = 0;
  state = STATE_READY;
}

Thread::~Thread()
{
#ifdef _WIN32
  if ( handle ) CloseHandle( handle );
#endif
}

void* Thread::getspecific( unsigned long key )
{
#ifdef _WIN32
    return TlsGetValue( key );
#else
    return pthread_getspecific( key );
#endif
}

unsigned long Thread::gettid()
{
#if defined(_WIN32)
  return GetCurrentThreadId();
#elif defined(__linux__)
  return syscall( SYS_gettid );
#elif defined(__sun)
  return thr_self();
#else
  return 0;
#endif
}

bool Thread::join()
{
#ifdef _WIN32
  return WaitForSingleObject( handle, INFINITE ) == WAIT_OBJECT_0;
#else
  return pthread_join( handle, NULL ) != -1;
#endif
}

unsigned long Thread::key_create()
{
#ifdef _WIN32
  return TlsAlloc();
#else
  unsigned long key;
  pthread_key_create( reinterpret_cast<pthread_key_t*>( &key ), NULL );
  return key;
#endif
}

void Thread::nanosleep( const Time& timeout )
{
#ifdef _WIN32
  Sleep( static_cast<DWORD>( timeout.as_unix_time_ms() ) );
#else
  struct timespec timeout_ts = timeout;
  ::nanosleep( &timeout_ts, NULL );
#endif
}

#ifdef _WIN32
//
// Usage: SetThreadName (-1, "MainThread");
// from http://msdn.microsoft.com/library/default.asp?url=/library/en-us/vsdebug/html/vxtsksettingthreadname.asp
//
typedef struct tagTHREADNAME_INFO
{
  DWORD dwType; // must be 0x1000
  LPCSTR szName; // pointer to name (in user addr space)
  DWORD dwThreadID; // thread ID (-1=caller thread)
  DWORD dwFlags; // reserved for future use, must be zero
}
THREADNAME_INFO;
#endif

void Thread::set_name( const char* thread_name )
{
#ifdef _WIN32
  THREADNAME_INFO info;
  info.dwType = 0x1000;
  info.szName = thread_name;
  info.dwThreadID = id;
  info.dwFlags = 0;

  __try
  {
      RaiseException
      (
        0x406D1388,
        0,
        sizeof( info ) / sizeof( DWORD ),
        reinterpret_cast<const ULONG_PTR*>( &info )
      );
  }
  __except( EXCEPTION_CONTINUE_EXECUTION )
  {}
#endif
}

bool Thread::set_processor_affinity( unsigned short logical_processor_i )
{
  if ( id != 0 )
  {
#if defined(_WIN32)
    return SetThreadAffinityMask( handle, ( 1L << logical_processor_i ) ) != 0;
#elif defined(__linux__)
    cpu_set_t cpu_set;
    CPU_ZERO( &cpu_set );
    CPU_SET( logical_processor_i, &cpu_set );
    return sched_setaffinity( 0, sizeof( cpu_set ), &cpu_set ) == 0;
#elif defined(__sun)
    return processor_bind( P_LWPID, id, logical_processor_i, NULL ) == 0;
#else
    return false;
#endif
  }
  else
    return false;
}

bool Thread::set_processor_affinity
(
  const ProcessorSet& logical_processor_set
)
{
  if ( id != 0 )
  {
#if defined(_WIN32)
    return SetThreadAffinityMask( handle, logical_processor_set.mask ) != 0;
#elif defined(__linux__)
    return sched_setaffinity
     (
       0,
       sizeof( cpu_set_t ),
       static_cast<cpu_set_t*>( logical_processor_set.cpu_set )
      ) == 0;
#elif defined(__sun)
    return pset_bind( logical_processor_set.psetid, P_LWPID, id, NULL ) == 0;
#else
    return false;
#endif
  }
  else
    return false;
}

void Thread::setspecific( unsigned long key, void* value )
{
#ifdef _WIN32
    TlsSetValue( key, value );
#else
  pthread_setspecific( key, value );
#endif
}

void Thread::start()
{
#ifdef _WIN32
  handle = CreateThread( NULL, 0, thread_stub, this, NULL, &id );
#else
  pthread_attr_t attr;
  pthread_attr_init( &attr );
  pthread_attr_setdetachstate( &attr, PTHREAD_CREATE_DETACHED );
  pthread_create( &handle, &attr, &thread_stub, this );
  pthread_attr_destroy( &attr );
#endif

  while ( state == STATE_READY )
    yield();
}

#ifdef _WIN32
unsigned long __stdcall Thread::thread_stub( void* this_ )
#else
void* Thread::thread_stub( void* this_ )
#endif
{
#if defined(__linux__)
  static_cast<Thread*>( this_ )->id = syscall( SYS_gettid );
#elif defined(__MACH__)
  static_cast<Thread*>( this_ )->id = 0; // ???
#elif defined(__sun)
  static_cast<Thread*>( this_ )->id = thr_self();
#endif

  static_cast<Thread*>( this_ )->state = STATE_RUNNING;

  static_cast<Thread*>( this_ )->run();

  static_cast<Thread*>( this_ )->state = STATE_STOPPED;

  return 0;
}

void Thread::yield()
{
#if defined(_WIN32)
  SwitchToThread();
#elif defined(__MACH__)
  pthread_YIELD_np();
#elif defined(__sun)
  sleep( 0 );
#else
  pthread_yield();
#endif
}


// time.cpp
#ifdef _WIN32
#undef INVALID_SOCKET
#include <windows.h> // For FILETIME
#include <winsock.h> // For timeval
#define INVALID_SOCKET  (SOCKET)(~0)
#else
#include <stdio.h> // For snprintf
#if defined(__MACH__)
#include <sys/time.h> // For gettimeofday
#endif
#endif

const char* HTTPDaysOfWeek[]
  = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

const char* ISOMonths[]
  = {
      "Jan", "Feb", "Mar", "Apr", "May", "Jun",
      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };


#ifdef _WIN32
static ULONGLONG FILETIMEToUnixTimeNS( const FILETIME& file_time )
{
  ULARGE_INTEGER file_time_combined;
  file_time_combined.LowPart = file_time.dwLowDateTime;
  file_time_combined.HighPart = file_time.dwHighDateTime;
  // Subtract the number of 100-ns intervals between
  // January 1, 1601 and January 1, 1970
  file_time_combined.QuadPart -= 116444736000000000;
  file_time_combined.QuadPart *= 100; // Into nanoseconds
  return file_time_combined.QuadPart;
}

// Adapted from http://support.microsoft.com/kb/167296
//static FILETIME UnixTimeSToFILETIME( uint32_t unix_time_s )
//{
// LONGLONG ll = Int32x32To64( unix_time_s, 10000000 ) + 116444736000000000;
// FILETIME file_time;
// file_time.dwLowDateTime = static_cast<DWORD>( ll );
// file_time.dwHighDateTime = ll >> 32;
// return file_time;
//}

static FILETIME UnixTimeNSToFILETIME( uint64_t unix_time_ns )
{
  // Add the difference in nanoseconds between
  // January 1, 1601 (start of the Windows epoch) and
  // January 1, 1970 (start of the Unix epoch)
  unix_time_ns += 11644473600000000000;
  uint64_t unix_time_100_ns_intervals = unix_time_ns / 100;
  FILETIME file_time;
  file_time.dwLowDateTime = static_cast<DWORD>( unix_time_100_ns_intervals );
  file_time.dwHighDateTime = unix_time_100_ns_intervals >> 32;
  return file_time;
}

static SYSTEMTIME UnixTimeNSToUTCSYSTEMTIME( uint64_t unix_time_ns )
{
  FILETIME file_time = UnixTimeNSToFILETIME( unix_time_ns );
  SYSTEMTIME system_time;
  FileTimeToSystemTime( &file_time, &system_time );
  return system_time;
}

static SYSTEMTIME UnixTimeNSToLocalSYSTEMTIME( uint64_t unix_time_ns )
{
  SYSTEMTIME utc_system_time = UnixTimeNSToUTCSYSTEMTIME( unix_time_ns );
  TIME_ZONE_INFORMATION time_zone_information;
  GetTimeZoneInformation( &time_zone_information );
  SYSTEMTIME local_system_time;
  SystemTimeToTzSpecificLocalTime
  (
    &time_zone_information,
    &utc_system_time,
    &local_system_time
  );
  return local_system_time;
}
#endif


Time::Time()
{
#if defined(_WIN32)
  FILETIME file_time;
  GetSystemTimeAsFileTime( &file_time );
  unix_time_ns = FILETIMEToUnixTimeNS( file_time );
#elif defined(__MACH__)
  struct timeval tv;
  gettimeofday( &tv, NULL );
  unix_time_ns = tv.tv_sec * NS_IN_S + tv.tv_usec * NS_IN_US;
#else
  // POSIX real time
  struct timespec ts;
  clock_gettime( CLOCK_REALTIME, &ts );
  unix_time_ns = ts.tv_sec * NS_IN_S + ts.tv_nsec;
#endif
}

Time::Time( double unix_time_s )
  : unix_time_ns
    (
      static_cast<uint64_t>
      (
        unix_time_s * static_cast<double>( NS_IN_S )
      )
    )
{ }

Time::Time( const struct timeval& tv )
{
  unix_time_ns = tv.tv_sec * NS_IN_S + tv.tv_usec * NS_IN_US;
}

#ifdef _WIN32
Time::Time( const FILETIME& file_time )
{
  unix_time_ns = FILETIMEToUnixTimeNS( file_time );
}

Time::Time( const FILETIME* file_time )
{
  if ( file_time != NULL )
    unix_time_ns = FILETIMEToUnixTimeNS( *file_time );
  else
    unix_time_ns = 0;
}
#else
Time::Time( const struct timespec& ts )
{
  unix_time_ns = ts.tv_sec * NS_IN_S + ts.tv_nsec;
}
#endif

void Time::as_common_log_date_time( char* out_str, uint8_t out_str_len ) const
{
#ifdef _WIN32
  SYSTEMTIME local_system_time = UnixTimeNSToLocalSYSTEMTIME( unix_time_ns );
  TIME_ZONE_INFORMATION win_tz;
  GetTimeZoneInformation( &win_tz );

  // 10/Oct/2000:13:55:36 -0700
  _snprintf_s( out_str, out_str_len, _TRUNCATE,
          "%02d/%s/%04d:%02d:%02d:%02d %+0.4d",
            local_system_time.wDay,
            ISOMonths[local_system_time.wMonth-1],
            local_system_time.wYear,
            local_system_time.wHour,
            local_system_time.wMinute,
            local_system_time.wSecond,
            ( win_tz.Bias / 60 ) * -100 );
#else
  time_t unix_time_s = static_cast<time_t>( unix_time_ns / NS_IN_S );
  struct tm unix_tm;
  localtime_r( &unix_time_s, &unix_tm );

  snprintf( out_str, out_str_len,
            "%02d/%s/%04d:%02d:%02d:%02d %d",
            unix_tm.tm_mday,
            ISOMonths[unix_tm.tm_mon],
            unix_tm.tm_year + 1900,
            unix_tm.tm_hour,
            unix_tm.tm_min,
            unix_tm.tm_sec,
            0 ); // Could use the extern timezone,
                 // which is supposed to be secs west of GMT.
#endif
}

void Time::as_http_date_time( char* out_str, uint8_t out_str_len ) const
{
#ifdef _WIN32
  SYSTEMTIME utc_system_time = UnixTimeNSToUTCSYSTEMTIME( unix_time_ns );

  _snprintf_s
  (
    out_str,
    out_str_len,
    _TRUNCATE,
    "%s, %02d %s %04d %02d:%02d:%02d GMT",
    HTTPDaysOfWeek[utc_system_time.wDayOfWeek],
    utc_system_time.wDay,
    ISOMonths[utc_system_time.wMonth-1],
    utc_system_time.wYear,
    utc_system_time.wHour,
    utc_system_time.wMinute,
    utc_system_time.wSecond
  );
#else
  time_t unix_time_s = static_cast<time_t>( unix_time_ns / NS_IN_S );
  struct tm unix_tm;
  gmtime_r( &unix_time_s, &unix_tm );

  snprintf
  (
    out_str,
    out_str_len,
    "%s, %02d %s %04d %02d:%02d:%02d GMT",
    HTTPDaysOfWeek[unix_tm.tm_wday],
    unix_tm.tm_mday,
    ISOMonths[unix_tm.tm_mon],
    unix_tm.tm_year + 1900,
    unix_tm.tm_hour,
    unix_tm.tm_min,
    unix_tm.tm_sec
  );
#endif
}

/*
uint64_t Time::parseHTTPDateTimeToUnixTimeNS( const char* date_str )
{
  char day[4], month[4];

#ifdef _WIN32
  SYSTEMTIME utc_system_time;

  int sf_ret
    = sscanf
      (
        date_str,
        "%03s, %02d %03s %04d %02d:%02d:%02d GMT",
        &day,
        &utc_system_time.wDay,
        &month,
        &utc_system_time.wYear,
        &utc_system_time.wHour,
        &utc_system_time.wMinute,
        &utc_system_time.wSecond
      );

  if ( sf_ret != 7 )
    return 0;

  for
  (
    utc_system_time.wDayOfWeek = 0;
    utc_system_time.wDayOfWeek < 7;
    utc_system_time.wDayOfWeek++
  )
  {
    if ( strcmp( day, HTTPDaysOfWeek[utc_system_time.wDayOfWeek] ) == 0 )
      break;
  }

  for
  (
    utc_system_time.wMonth = 0;
    utc_system_time.wMonth < 12;
    utc_system_time.wMonth++
  )
  {
    if ( strcmp( month, ISOMonths[utc_system_time.wMonth] ) == 0 )
      break;
  }
  utc_system_time.wMonth++; // Windows starts the months from 1

  FILETIME file_time;
  SystemTimeToFileTime( &utc_system_time, &file_time );
  return FILETIMEToUnixTimeNS( file_time );
#else
  struct tm unix_tm;

  int sf_ret
    = sscanf
      (
        date_str,
        "%03s, %02d %03s %04d %02d:%02d:%02d GMT",
        &day,
        &unix_tm.tm_mday,
        &month,
        &unix_tm.tm_year,
        &unix_tm.tm_hour,
        &unix_tm.tm_min,
        &unix_tm.tm_sec
      );

  if ( sf_ret != 7 )
    return 0;

  unix_tm.tm_year -= 1900;

  for ( unix_tm.tm_wday = 0; unix_tm.tm_wday < 7; unix_tm.tm_wday++ )
    if ( strcmp( day, HTTPDaysOfWeek[unix_tm.tm_wday] ) == 0 )
      break;

  for ( unix_tm.tm_mon = 0; unix_tm.tm_mon < 12; unix_tm.tm_mon++ )
    if ( strcmp( month, ISOMonths[unix_tm.tm_mon] ) == 0 )
      break;

  time_t unix_time_s = mktime( &unix_tm ); // mktime is thread-safe

  return unix_time_s * NS_IN_S;
#endif
}
*/

void Time::as_iso_date( char* out_str, uint8_t out_str_len ) const
{
#ifdef _WIN32
  SYSTEMTIME local_system_time = UnixTimeNSToLocalSYSTEMTIME( unix_time_ns );
  _snprintf_s
  (
    out_str,
    out_str_len,
    _TRUNCATE,
    "%04d-%02d-%02d",
    local_system_time.wYear,
    local_system_time.wMonth,
    local_system_time.wDay
  );
#else
  time_t unix_time_s = static_cast<time_t>( unix_time_ns / NS_IN_S );
  struct tm unix_tm;
  localtime_r( &unix_time_s, &unix_tm );
  snprintf
  (
    out_str,
    out_str_len,
    "%04d-%02d-%02d",
    unix_tm.tm_year + 1900,
    unix_tm.tm_mon + 1,
    unix_tm.tm_mday
  );
#endif
}

void Time::as_iso_date_time( char* out_str, uint8_t out_str_len ) const
{
#ifdef _WIN32
  SYSTEMTIME local_system_time = UnixTimeNSToLocalSYSTEMTIME( unix_time_ns );

  _snprintf_s
  (
    out_str,
    out_str_len,
    _TRUNCATE,
    "%04d-%02d-%02dT%02d:%02d:%02d.000Z",
    local_system_time.wYear,
    local_system_time.wMonth,
    local_system_time.wDay,
    local_system_time.wHour,
    local_system_time.wMinute,
    local_system_time.wSecond
  );
#else
  time_t unix_time_s = static_cast<time_t>(  unix_time_ns / NS_IN_S );
  struct tm unix_tm;
  localtime_r( &unix_time_s, &unix_tm );

  snprintf
  (
    out_str,
    out_str_len,
    "%04d-%02d-%02dT%02d:%02d:%02d.000Z",
    unix_tm.tm_year + 1900,
    unix_tm.tm_mon + 1,
    unix_tm.tm_mday,
    unix_tm.tm_hour,
    unix_tm.tm_min,
    unix_tm.tm_sec
  );
#endif
}

Time::operator struct timeval() const
{
  struct timeval tv;
#ifdef _WIN32
  tv.tv_sec = static_cast<long>( unix_time_ns / NS_IN_S );
#else
  tv.tv_sec = static_cast<time_t>( unix_time_ns / NS_IN_S );
#endif
  tv.tv_usec = ( unix_time_ns % NS_IN_S ) / NS_IN_US;
  return tv;
}

#ifdef _WIN32
Time::operator FILETIME() const
{
  return UnixTimeNSToFILETIME( unix_time_ns );
}
#else
Time::operator struct timespec() const
{
  struct timespec ts;
  ts.tv_sec = unix_time_ns / NS_IN_S;
  ts.tv_nsec = unix_time_ns % NS_IN_S;
  return ts;
}
#endif

Time::operator string() const
{
  char iso_date_time[30];
  as_iso_date_time( iso_date_time, 30 );
  return iso_date_time;
}

Time& Time::operator=( const Time& other )
{
  unix_time_ns = other.unix_time_ns;
  return *this;
}

Time& Time::operator=( uint64_t unix_time_ns )
{
  this->unix_time_ns = unix_time_ns;
  return *this;
}

Time& Time::operator=( double unix_time_s )
{
  this->unix_time_ns =
    static_cast<uint64_t>
    (
      unix_time_s * static_cast<double>( NS_IN_S )
    );
  return *this;
}

Time Time::operator+( const Time& other ) const
{
  return Time( unix_time_ns + other.unix_time_ns );
}

Time Time::operator+( uint64_t unix_time_ns ) const
{
  return Time( this->unix_time_ns + unix_time_ns );
}

Time Time::operator+( double unix_time_s ) const
{
  return Time( as_unix_time_s() + unix_time_s );
}

Time& Time::operator+=( const Time& other )
{
  unix_time_ns += other.unix_time_ns;
  return *this;
}

Time& Time::operator+=( uint64_t unix_time_ns )
{
  this->unix_time_ns += unix_time_ns;
  return *this;
}

Time& Time::operator+=( double unix_time_s )
{
  this->unix_time_ns +=
    static_cast<uint64_t>
    (
      unix_time_s * static_cast<double>( NS_IN_S )
    );

  return *this;
}

Time Time::operator-( const Time& other ) const
{
  if ( unix_time_ns >= other.unix_time_ns )
    return Time( unix_time_ns - other.unix_time_ns );
  else
    return Time( static_cast<uint64_t>( 0 ) );
}

Time Time::operator-( uint64_t unix_time_ns ) const
{
  if ( this->unix_time_ns >= unix_time_ns )
    return Time( this->unix_time_ns - unix_time_ns );
  else
    return Time( static_cast<uint64_t>( 0 ) );
}

Time Time::operator-( double unix_time_s ) const
{
  double this_unix_time_s = as_unix_time_s();
  if ( this_unix_time_s >= unix_time_s )
    return Time( this_unix_time_s - unix_time_s );
  else
    return Time( static_cast<uint64_t>( 0 ) );
}

Time& Time::operator-=( const Time& other )
{
  if ( unix_time_ns >= other.unix_time_ns )
    unix_time_ns -= other.unix_time_ns;
  else
    unix_time_ns = 0;

  return *this;
}

Time& Time::operator-=( uint64_t unix_time_ns )
{
  if ( this->unix_time_ns >= unix_time_ns )
    this->unix_time_ns -= unix_time_ns;
  else
    this->unix_time_ns = 0;

  return *this;
}

Time& Time::operator-=( double unix_time_s )
{
  double this_unix_time_s = as_unix_time_s();
  if ( this_unix_time_s >= unix_time_s )
    this->unix_time_ns -= static_cast<uint64_t>( unix_time_s * NS_IN_S );
  else
    this->unix_time_ns = 0;

  return *this;
}

Time& Time::operator*=( const Time& other )
{
  unix_time_ns *= other.unix_time_ns;
  return *this;
}

bool Time::operator==( const Time& other ) const
{
  return unix_time_ns == other.unix_time_ns;
}

bool Time::operator==( uint64_t unix_time_ns ) const
{
  return this->unix_time_ns == unix_time_ns;
}

bool Time::operator==( double unix_time_s ) const
{
  return as_unix_time_s() == unix_time_s;
}

bool Time::operator!=( const Time& other ) const
{
  return unix_time_ns != other.unix_time_ns;
}

bool Time::operator!=( uint64_t unix_time_ns ) const
{
  return this->unix_time_ns != unix_time_ns;
}

bool Time::operator!=( double unix_time_s ) const
{
  return as_unix_time_s() != unix_time_s;
}

bool Time::operator<( const Time& other ) const
{
  return unix_time_ns < other.unix_time_ns;
}

bool Time::operator<( uint64_t unix_time_ns ) const
{
  return this->unix_time_ns < unix_time_ns;
}

bool Time::operator<( double unix_time_s ) const
{
  return as_unix_time_s() < unix_time_s;
}

bool Time::operator<=( const Time& other ) const
{
  return unix_time_ns <= other.unix_time_ns;
}

bool Time::operator<=( uint64_t unix_time_ns ) const
{
  return this->unix_time_ns <= unix_time_ns;
}

bool Time::operator<=( double unix_time_s ) const
{
  return as_unix_time_s() <= unix_time_s;
}

bool Time::operator>( const Time& other ) const
{
  return unix_time_ns > other.unix_time_ns;
}

bool Time::operator>( uint64_t unix_time_ns ) const
{
  return this->unix_time_ns > unix_time_ns;
}

bool Time::operator>( double unix_time_s ) const
{
  return as_unix_time_s() > unix_time_s;
}

bool Time::operator>=( const Time& other ) const
{
  return unix_time_ns >= other.unix_time_ns;
}

bool Time::operator>=( uint64_t unix_time_ns ) const
{
  return this->unix_time_ns >= unix_time_ns;
}

bool Time::operator>=( double unix_time_s ) const
{
  return as_unix_time_s() >= unix_time_s;
}


// timer_queue.cpp
#ifdef _WIN32
#include <windows.h>
#endif

#include <queue>
#include <utility>


TimerQueue* TimerQueue::default_timer_queue = NULL;


#ifndef _WIN32
class TimerQueue::Thread : public yield::platform::Thread
{
public:
  Thread()
   : fd_event_poller( FDEventPoller::create() ),
     new_timers_pipe( Pipe::create() )
  {
    fd_event_poller.associate( new_timers_pipe.get_read_end() );
    should_run = true;
  }

  ~Thread()
  {
    FDEventPoller::dec_ref( fd_event_poller );
    Pipe::dec_ref( new_timers_pipe );
  }

  void addTimer( Timer* timer )
  {
    new_timers_pipe.write( timer, sizeof( timer ) );
  }

  void stop()
  {
    should_run = false;
    addTimer( NULL );
  }

  // yield::platform::Thread
  void run()
  {
    set_name( "TimerQueueThread" );

    while ( should_run )
    {
      if ( timers.empty() )
      {
        if ( fd_event_poller.poll() )
        {
          Timer* new_timer;
          new_timers_pipe.read( &new_timer, sizeof( new_timer ) );
          if ( new_timer != NULL )
          {
            timers.push
            (
              make_pair( Time() + new_timer->get_timeout(), new_timer )
            );
          }
          else
            break;
        }
      }
      else
      {
        Time current_time;
        if ( timers.top().first <= current_time )
        // Earliest timer has expired, fire it
        {
          TimerQueue::Timer* timer = timers.top().second;
          timers.pop();

          if ( !timer->deleted )
          {
            timer->fire();

            if ( timer->get_period() != static_cast<uint64_t>( 0 ) )
            {
              timer->last_fire_time = Time();
              timers.push
              (
                make_pair
                (
                  timer->last_fire_time + timer->get_period(),
                  timer
                )
              );
            }
            else
              TimerQueue::Timer::dec_ref( *timer );
          }
          else
            TimerQueue::Timer::dec_ref( *timer );
        }
        else // Wait on the new timers queue until a new timer arrives
             // or it's time to fire the next timer
        {
          if ( fd_event_poller.poll( timers.top().first - current_time ) )
          {
            TimerQueue::Timer* new_timer;
            new_timers_pipe.read( &new_timer, sizeof( new_timer ) );
            if ( new_timer != NULL )
            {
              timers.push
              (
                make_pair( Time() + new_timer->get_timeout(), new_timer )
              );
            }
            else
              break;
          }
        }
      }
    }
  }

private:
  FDEventPoller& fd_event_poller;
  Pipe& new_timers_pipe;
  bool should_run;
  std::priority_queue
  <
    pair<Time, Timer*>,
    vector< pair<Time, Timer*> >,
    std::greater< pair<Time, Timer*> >
  > timers;
};
#endif


TimerQueue::TimerQueue()
{
#ifdef _WIN32
  hTimerQueue = CreateTimerQueue();
#else
  thread = new Thread;
  thread->start();
#endif
}

#ifdef _WIN32
TimerQueue::TimerQueue( HANDLE hTimerQueue )
  : hTimerQueue( hTimerQueue )
{ }
#endif

TimerQueue::~TimerQueue()
{
#ifdef _WIN32
  if ( hTimerQueue != NULL )
    DeleteTimerQueueEx( hTimerQueue, NULL );
#else
  thread->stop();
  delete thread;
#endif
}

void TimerQueue::addTimer( Timer& timer )
{
#ifdef _WIN32
  timer.hTimerQueue = hTimerQueue;
  CreateTimerQueueTimer
  (
    &timer.hTimer,
    hTimerQueue,
    Timer::WaitOrTimerCallback,
    &timer.inc_ref(),
    static_cast<DWORD>( timer.get_timeout().as_unix_time_ms() ),
    static_cast<DWORD>( timer.get_period().as_unix_time_ms() ),
    WT_EXECUTEDEFAULT
  );
#else
  thread->addTimer( &timer.inc_ref() );
#endif
}

void TimerQueue::destroyDefaultTimerQueue()
{
  if ( default_timer_queue != NULL )
    delete default_timer_queue;
}

TimerQueue& TimerQueue::getDefaultTimerQueue()
{
  if ( default_timer_queue == NULL )
#ifdef _WIN32
    default_timer_queue = new TimerQueue( NULL );
#else
    default_timer_queue = new TimerQueue;
#endif
  return *default_timer_queue;
}


TimerQueue::Timer::Timer( const Time& timeout )
  : period( static_cast<uint64_t>( 0 ) ), timeout( timeout )
{
#ifdef _WIN32
  hTimer = hTimerQueue = NULL;
#endif
}

TimerQueue::Timer::Timer( const Time& timeout, const Time& period )
  : period( period ), timeout( timeout )
{
#ifdef _WIN32
  hTimer = hTimerQueue = NULL;
#else
  deleted = false;
#endif
}

TimerQueue::Timer::~Timer()
{ }

void TimerQueue::Timer::delete_()
{
#ifdef _WIN32
  CancelTimerQueueTimer( hTimerQueue, hTimer );
#else
  deleted = true;
#endif
}

#ifdef _WIN32
VOID CALLBACK TimerQueue::Timer::WaitOrTimerCallback
(
  PVOID lpParameter,
  BOOLEAN
)
{
  Timer* this_ = static_cast<Timer*>( lpParameter );

  Time elapsed_time( Time() - this_->last_fire_time );
  if ( elapsed_time > 0ULL )
  {
    this_->fire();

    if ( this_->get_period() == 0ULL )
      TimerQueue::Timer::dec_ref( *this_ );
    else
      this_->last_fire_time = Time();
  }
  else
    this_->last_fire_time = Time();
}
#endif


// udp_socket.cpp
#if defined(_WIN32)
#undef INVALID_SOCKET
#pragma warning( push )
#pragma warning( disable: 4365 4995 )
#include <ws2tcpip.h>
#pragma warning( pop )
#define INVALID_SOCKET  (SOCKET)(~0)
#else
#include <netinet/in.h> // For the IPPROTO_* constants
#include <sys/socket.h>
#endif


int UDPSocket::PROTOCOL = IPPROTO_UDP;
int UDPSocket::TYPE = SOCK_DGRAM;


class UDPSocket::IORecvFromCB : public IOCB<AIORecvFromCallback>
{
protected:
  IORecvFromCB
  (
    Buffer& buffer,
    int flags,
    UDPSocket& udp_socket,
    AIORecvFromCallback& callback,
    void* callback_context
  )
  : IOCB<AIORecvFromCallback>( callback, callback_context ),
    buffer( buffer ),
    flags( flags ),
    udp_socket( udp_socket )
  { }

  ~IORecvFromCB()
  {
    Buffer::dec_ref( buffer );
    UDPSocket::dec_ref( udp_socket );
  }

  Buffer& get_buffer() const { return buffer; }
  int get_flags() const { return flags; }
  UDPSocket& get_udp_socket() const { return udp_socket; }

  void onRecvFromCompletion( struct sockaddr_storage& peername )
  {
    SocketAddress* peername_sa = new SocketAddress( peername );

    callback.onRecvFromCompletion
    (
      buffer,
      *peername_sa,
      callback_context
    );

    SocketAddress::dec_ref( *peername_sa );
  }

  void onRecvFromError()
  {
    onRecvFromError( get_last_error() );
  }

  void onRecvFromError( uint32_t error_code )
  {
    callback.onRecvFromError( error_code, callback_context );
  }

private:
  Buffer& buffer;
  int flags;
  UDPSocket& udp_socket;
};


class UDPSocket::BIORecvFromCB
  : public BIOCB, public IORecvFromCB
{
public:
  BIORecvFromCB
  (
    Buffer& buffer,
    int flags,
    UDPSocket& udp_socket,
    AIORecvFromCallback& callback,
    void* callback_context
  ) : IORecvFromCB( buffer, flags, udp_socket, callback, callback_context )
  { }

private:
  // BIOCB
  void execute()
  {
    if ( get_udp_socket().set_blocking_mode( true ) )
    {
      struct sockaddr_storage peername;
      ssize_t recvfrom_ret
        = get_udp_socket().recvfrom( get_buffer(), get_flags(), peername );
      if ( recvfrom_ret > 0 )
        onRecvFromCompletion( peername );
      else
        onRecvFromError();
    }
    else
      onRecvFromError();
  }
};


class UDPSocket::NBIORecvFromCB
  : public NBIOCB, public IORecvFromCB
{
public:
  NBIORecvFromCB
  (
    Buffer& buffer,
    int flags,
    UDPSocket& udp_socket,
    AIORecvFromCallback& callback,
    void* callback_context
  ) : NBIOCB( STATE_WANT_READ ),
      IORecvFromCB( buffer, flags, udp_socket, callback, callback_context )
  { }

private:
  // NBIOCB
  void execute()
  {
    if ( get_udp_socket().set_blocking_mode( false ) )
    {
      struct sockaddr_storage peername;
      ssize_t recvfrom_ret
        = get_udp_socket().recvfrom( get_buffer(), get_flags(), peername );

      if ( recvfrom_ret > 0 )
      {
        set_state( STATE_COMPLETE );
        onRecvFromCompletion( peername );
      }
      else if ( get_udp_socket().want_read() )
        set_state( STATE_WANT_READ );
      else
      {
        set_state( STATE_ERROR );
        onRecvFromError();
      }
    }
    else
    {
      set_state( STATE_ERROR );
      onRecvFromError();
    }
  }

  socket_t get_fd() const { return get_udp_socket(); }
};


#ifdef _WIN32
class UDPSocket::Win32AIORecvFromCB
  : public Win32AIOCB, public IORecvFromCB
{
public:
  Win32AIORecvFromCB
  (
    Buffer& buffer,
    UDPSocket& udp_socket,
    AIORecvFromCallback& callback,
    void* callback_context
  ) : IORecvFromCB( buffer, 0, udp_socket, callback, callback_context )
  { }

  struct sockaddr_storage peername;

private:
  // Win32AIOCB
  void onCompletion( DWORD dwNumberOfBytesTransferred )
  {
    get_buffer().put( dwNumberOfBytesTransferred );
    onRecvFromCompletion( peername );
  }

  void onError( DWORD dwErrorCode )
  {
    onRecvFromError( static_cast<uint32_t>( dwErrorCode ) );
  }
};
#endif


UDPSocket::UDPSocket( int domain, socket_t socket_ )
  : Socket( domain, TYPE, PROTOCOL, socket_ )
{ }

void
UDPSocket::aio_recvfrom
(
  Buffer& buffer,
  int flags,
  AIORecvFromCallback& callback,
  void* callback_context
)
{
#ifdef _WIN32
  if
  (
    get_io_queue() != NULL
    &&
    get_io_queue()->get_type_id() == Win32AIOQueue::TYPE_ID
  )
  {
    WSABUF wsabuf;
    wsabuf.buf = static_cast<CHAR*>( buffer ) + buffer.size();
    wsabuf.len = static_cast<ULONG>( buffer.capacity() - buffer.size() );

    DWORD dwNumberOfBytesReceived,
          dwFlags = static_cast<DWORD>( get_platform_recv_flags( flags ) );

    Win32AIORecvFromCB* aiocb
     = new Win32AIORecvFromCB
           (
             buffer,
             inc_ref(),
             callback,
             callback_context
           );

    socklen_t peername_len = sizeof( aiocb->peername );

    if
    (
      WSARecvFrom
      (
        *this,
        &wsabuf,
        1,
        &dwNumberOfBytesReceived,
        &dwFlags,
        reinterpret_cast<struct sockaddr*>( &aiocb->peername ),
        &peername_len,
        *aiocb,
        NULL
      ) == 0
      ||
      WSAGetLastError() == WSA_IO_PENDING
    )
      return;
    else
    {
      delete aiocb;
      callback.onRecvFromError( get_last_error(), callback_context );
      return;
    }
  }
#endif

  // Try a non-blocking recvfrom first
  if ( set_blocking_mode( false ) )
  {
    struct sockaddr_storage peername;
    ssize_t recvfrom_ret = recvfrom( buffer, flags, peername );
    if ( recvfrom_ret > 0 )
    {
      SocketAddress* peername_sa = new SocketAddress( peername );
      callback.onRecvFromCompletion( buffer, *peername_sa, callback_context );
      SocketAddress::dec_ref( *peername_sa );
      Buffer::dec_ref( buffer );
      return;
    }
    else if ( !want_read( ))
    {
      callback.onRecvFromError( get_last_error(), callback_context );
      Buffer::dec_ref( buffer );
      return;
    }
  }

  // Next try to offload the recvfrom to an IOQueue
  if ( get_io_queue() != NULL )
  {
    switch ( get_io_queue()->get_type_id() )
    {
      case BIOQueue::TYPE_ID:
      {
        static_cast<BIOQueue*>( get_io_queue() )
          ->submit
            (
              *new BIORecvFromCB
                   (
                     buffer,
                     flags,
                     inc_ref(),
                     callback,
                     callback_context
                   )
            );
      }
      return;

      case NBIOQueue::TYPE_ID:
      {
        static_cast<NBIOQueue*>( get_io_queue() )
          ->submit
            (
               *new NBIORecvFromCB
                    (
                      buffer,
                      flags,
                      inc_ref(),
                      callback,
                      callback_context
                    )
            );
      }
      return;
    }
  }

  // Give up instead of blocking on recvfrom
  callback.onRecvFromError( get_last_error(), callback_context );
  Buffer::dec_ref( buffer );
}

UDPSocket* UDPSocket::create()
{
  int domain = AF_INET6;
  socket_t socket_ = Socket::create( &domain, TYPE, PROTOCOL );
  if ( socket_ != -1 )
    return new UDPSocket( domain, socket_ );
  else
    return NULL;
}

ssize_t
UDPSocket::recvfrom
(
  Buffer& buffer,
  int flags,
  struct sockaddr_storage& peername
) const
{
  ssize_t recvfrom_ret
    = recvfrom
      (
        static_cast<char*>( buffer ) + buffer.size(),
        buffer.capacity() - buffer.size(),
        flags,
        peername
      );

  if ( recvfrom_ret > 0 )
    buffer.put( static_cast<size_t>( recvfrom_ret ) );

  return recvfrom_ret;
}

ssize_t
UDPSocket::recvfrom
(
  void* buf,
  size_t buflen,
  int flags,
  struct sockaddr_storage& peername
) const
{
  socklen_t peername_len = sizeof( peername );
  return ::recvfrom
         (
           *this,
           static_cast<char*>( buf ),
           buflen,
           flags,
           reinterpret_cast<struct sockaddr*>( &peername ),
           &peername_len
         );
}

ssize_t
UDPSocket::sendmsg
(
  const struct iovec* iov,
  uint32_t iovlen,
  const SocketAddress& peername,
  int flags
) const
{
  flags = get_platform_send_flags( flags );

  struct sockaddr* name; socklen_t namelen;
  if ( peername.as_struct_sockaddr( get_domain(), name, namelen ) )
  {
#ifdef _WIN32
    DWORD dwWrittenLength;
#ifdef _WIN64
    vector<struct iovec64> wsabufs( iovlen );
    iovecs_to_wsabufs( buffers, wsabufs );
#endif

    ssize_t sendto_ret
      = WSASendTo
        (
          *this,
#ifdef _WIN64
          reinterpret_cast<LPWSABUF>( &wsabufs[0] ),
#else
          reinterpret_cast<LPWSABUF>( const_cast<struct iovec*>( iov ) ),
#endif
          iovlen,
          &dwWrittenLength,
          static_cast<DWORD>( flags ),
          name,
          namelen,
          NULL,
          NULL
        );

    if ( sendto_ret >= 0 )
      return static_cast<ssize_t>( dwWrittenLength );
    else
      return sendto_ret;
#else
    struct msghdr msghdr_;
    memset( &msghdr_, 0, sizeof( msghdr_ ) );
    msghdr_.msg_name = name;
    msghdr_.msg_namelen = namelen;
    msghdr_.msg_iov = const_cast<iovec*>( iov );
    msghdr_.msg_iovlen = iovlen;
    return ::sendmsg( *this, &msghdr_, flags );
#endif
  }
  else
    return -1;
}

ssize_t
UDPSocket::sendto
(
  const void* buf,
  size_t buflen,
  int flags,
  const SocketAddress& _peername
) const
{
  struct sockaddr* peername; socklen_t peername_len;
  if ( _peername.as_struct_sockaddr( get_domain(), peername, peername_len ) )
  {
    return ::sendto
           (
             *this,
             static_cast<const char*>( buf ),
             buflen,
             flags,
             peername,
             peername_len
           );
  }
  else
    return -1;
}


// volume.cpp
#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#else
#include <dirent.h> // for DIR
#include <stdio.h>
#include <sys/statvfs.h>
#include <sys/time.h>
#include <utime.h>
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
#if defined(__linux__)
#include <sys/xattr.h>
#define LISTXATTR ::listxattr
#define GETXATTR ::getxattr
#define SETXATTR ::setxattr
#define REMOVEXATTR ::removexattr
#elif defined(__MACH__)
#include <sys/xattr.h>
#define LISTXATTR( path, namebuf, size ) \
  ::listxattr( path, namebuf, size, 0 )
#define GETXATTR( path, name, value, size ) \
  ::getxattr( path, name, value, size, 0, 0 )
#define SETXATTR( path, name, value, size, flags ) \
  ::setxattr( path, name, value, size, 0, flags )
#define REMOVEXATTR( path, name ) \
  ::removexattr( path, name, 0 )
#endif
#endif
#endif


bool Volume::access( const Path& path, int amode )
{
#ifdef _WIN32
  ::SetLastError( ERROR_NOT_SUPPORTED );
  return false;
#else
  return ::access( path, amode ) >= 0;
#endif
}

#ifndef _WIN32
bool Volume::chmod( const Path& path, mode_t mode )
{
  Stat stbuf;
  stbuf.set_mode( mode );
  return setattr( path, stbuf, SETATTR_MODE );
}

bool Volume::chown( const Path& path, uid_t uid, uid_t gid )
{
  Stat stbuf;
  stbuf.set_uid( uid );
  stbuf.set_gid( gid );
  return setattr( path, stbuf, SETATTR_UID|SETATTR_GID );
}
#endif

File* Volume::creat( const Path& path )
{
  return creat( path, FILE_MODE_DEFAULT );
}

File* Volume::creat( const Path& path, mode_t mode )
{
  return open( path, O_CREAT|O_WRONLY|O_TRUNC, mode );
}

bool Volume::exists( const Path& path )
{
  return getattr( path ) != NULL;
}

bool Volume::isdir( const Path& path )
{
  Stat* stbuf = getattr( path );
  if ( stbuf != NULL )
  {
    bool isdir = stbuf->ISDIR();
    Stat::dec_ref( *stbuf );
    return isdir;
  }
  else
    return false;
}

bool Volume::isfile( const Path& path )
{
  Stat* stbuf = getattr( path );
  if ( stbuf != NULL )
  {
    bool isfile = stbuf->ISREG();
    Stat::dec_ref( *stbuf );
    return isfile;
  }
  else
    return false;
}

Stat* Volume::getattr( const Path& path )
{
#ifdef _WIN32
  WIN32_FIND_DATA find_data;
  HANDLE hFindFirstFile = FindFirstFile( path, &find_data );
  if ( hFindFirstFile != INVALID_HANDLE_VALUE )
  {
    FindClose( hFindFirstFile );
    return new Stat( find_data );
  }
#else
  struct stat stbuf;
  if ( ::stat( path, &stbuf ) != -1 )
    return new Stat( stbuf );
#endif
  return NULL;
}

bool Volume::getxattr
(
  const Path& path,
  const string& name,
  string& out_value
)
{
#if defined(YIELD_PLATFORM_HAVE_XATTR_H)
  ssize_t value_len = GETXATTR( path, name.c_str(), NULL, 0 );
  if ( value_len != -1 )
  {
    char* value = new char[value_len];
    GETXATTR( path, name.c_str(), value, value_len );
    out_value.assign( value, value_len );
    delete [] value;
    return true;
  }
#elif defined(_WIN32)
  ::SetLastError( ERROR_NOT_SUPPORTED );
#else
  errno = ENOTSUP;
#endif

  return false;
}

bool Volume::link( const Path& old_path, const Path& new_path )
{
#ifdef _WIN32
  return CreateHardLink( new_path, old_path, NULL ) != 0;
#else
  return ::symlink( old_path, new_path ) != -1;
#endif
}

bool Volume::listxattr( const Path& path, vector<string>& out_names )
{
#if defined(YIELD_PLATFORM_HAVE_XATTR_H)
  size_t names_len = LISTXATTR( path, NULL, 0 );
  if ( names_len > 0 )
  {
    char* names = new char[names_len];
    LISTXATTR( path, names, names_len );
    char* name = names;
    do
    {
      size_t name_len = strlen( name );
      out_names.push_back( string( name, name_len ) );
      name += name_len;
    }
    while ( static_cast<size_t>( name - names ) < names_len );
    delete [] names;
  }
  return true;
#elif defined(_WIN32)
  ::SetLastError( ERROR_NOT_SUPPORTED );
#else
  errno = ENOTSUP;
#endif

  return false;
}

bool Volume::makedirs( const Path& path )
{
  return mktree( path, DIRECTORY_MODE_DEFAULT );
}

bool Volume::makedirs( const Path& path, mode_t mode )
{
  return mktree( path, mode );
}

bool Volume::mkdir( const Path& path )
{
  return mkdir( path, DIRECTORY_MODE_DEFAULT );
}

bool Volume::mkdir( const Path& path, mode_t mode )
{
#ifdef _WIN32
  return CreateDirectoryW( path, NULL ) != 0;
#else
  return ::mkdir( path, mode ) != -1;
#endif
}

bool Volume::mktree( const Path& path )
{
  return mktree( path, DIRECTORY_MODE_DEFAULT );
}

bool Volume::mktree( const Path& path, mode_t mode )
{
  bool ret = true;

  pair<Path, Path> path_parts = path.split();
  if ( !path_parts.first.empty() )
    ret &= mktree( path_parts.first, mode );

  if ( !exists( path ) && !mkdir( path, mode ) )
      return false;

  return ret;
}

File* Volume::open( const Path& path )
{
  return open( path, O_RDONLY, FILE_MODE_DEFAULT, 0 );
}

File* Volume::open( const Path& path, uint32_t flags )
{
  return open( path, flags, FILE_MODE_DEFAULT, 0 );
}

File* Volume::open( const Path& path, uint32_t flags, mode_t mode )
{
  return open( path, flags, mode, 0 );
}

File*
Volume::open
(
  const Path& path,
  uint32_t flags,
  mode_t mode,
  uint32_t attributes
)
{
#ifdef _WIN32
  DWORD file_access_flags = 0,
        file_create_flags = 0,
        file_open_flags = attributes|FILE_FLAG_SEQUENTIAL_SCAN;

  if ( ( flags & O_APPEND ) == O_APPEND )
    file_access_flags |= FILE_APPEND_DATA;
  else if ( ( flags & O_RDWR ) == O_RDWR )
    file_access_flags |= GENERIC_READ|GENERIC_WRITE;
  else if ( ( flags & O_WRONLY ) == O_WRONLY )
    file_access_flags |= GENERIC_WRITE;
  else
    file_access_flags |= GENERIC_READ;

  if ( ( flags & O_CREAT ) == O_CREAT )
  {
    if ( ( flags & O_TRUNC ) == O_TRUNC )
      file_create_flags = CREATE_ALWAYS;
    else
      file_create_flags = OPEN_ALWAYS;
  }
  else
    file_create_flags = OPEN_EXISTING;

//  if ( ( flags & O_SPARSE ) == O_SPARSE )
//    file_open_flags |= FILE_ATTRIBUTE_SPARSE_FILE;

  if ( ( flags & O_SYNC ) == O_SYNC )
    file_open_flags |= FILE_FLAG_WRITE_THROUGH;

  if ( ( flags & O_DIRECT ) == O_DIRECT )
    file_open_flags |= FILE_FLAG_NO_BUFFERING;

  if ( ( flags & O_ASYNC ) == O_ASYNC )
    file_open_flags |= FILE_FLAG_OVERLAPPED;

  if ( ( flags & O_HIDDEN ) == O_HIDDEN )
    file_open_flags = FILE_ATTRIBUTE_HIDDEN;

  HANDLE fd = CreateFileW
              (
                path,
                file_access_flags,
                FILE_SHARE_DELETE|FILE_SHARE_READ|FILE_SHARE_WRITE,
                NULL,
                file_create_flags,
                file_open_flags,
                NULL
              );

  if ( fd != INVALID_HANDLE_VALUE )
  {
    if ( ( flags & O_TRUNC ) == O_TRUNC && ( flags & O_CREAT ) != O_CREAT )
    {
      SetFilePointer( fd, 0, NULL, FILE_BEGIN );
      SetEndOfFile( fd );
    }

    return new File( fd );
  }
#else
  int fd = ::open( path, flags, mode );
  if ( fd != -1 )
    return new File( fd );
#endif

  return NULL;
}

Directory* Volume::opendir( const Path& path )
{
#ifdef _WIN32
  wstring search_pattern( path );
  if ( search_pattern.size() > 0 &&
       search_pattern[search_pattern.size()-1] != L'\\' )
    search_pattern.append( L"\\" );
  search_pattern.append( L"*" );

  WIN32_FIND_DATA find_data;
  HANDLE hDirectory = FindFirstFileW( search_pattern.c_str(), &find_data );
  if ( hDirectory != INVALID_HANDLE_VALUE )
    return new Directory( hDirectory, find_data );
#else
  DIR* dirp = ::opendir( path );
  if ( dirp != NULL )
    return new Directory( dirp );
#endif

  return NULL;
}

Path* Volume::readlink( const Path& path )
{
#ifdef _WIN32
  ::SetLastError( ERROR_NOT_SUPPORTED );
  return NULL;
#else
  char out_path[PATH_MAX];
  ssize_t out_path_len = ::readlink( path, out_path, PATH_MAX );
  if ( out_path_len > 0 )
    return new Path( out_path, out_path_len );
  else
    return NULL;
#endif
}

bool Volume::removexattr( const Path& path, const string& name )
{
#if defined(YIELD_PLATFORM_HAVE_XATTR_H)
  return REMOVEXATTR( path, name.c_str() ) != -1;
#elif defined(_WIN32)
  ::SetLastError( ERROR_NOT_SUPPORTED );
#else
  errno = ENOTSUP;
#endif
  return false;
}

bool Volume::rename( const Path& from_path, const Path& to_path )
{
#ifdef _WIN32
  return MoveFileExW( from_path, to_path, MOVEFILE_REPLACE_EXISTING ) != 0;
#else
  return ::rename( from_path, to_path ) != -1;
#endif
}

bool Volume::rmdir( const Path& path )
{
#ifdef _WIN32
  return RemoveDirectoryW( path ) != 0;
#else
  return ::rmdir( path ) != -1;
#endif
}

bool Volume::rmtree( const Path& path )
{
  Directory* dir = opendir( path );
  if ( dir != NULL )
  {
    Directory::Entry* dirent = dir->readdir();
    while ( dirent != NULL )
    {
      bool isdir;
      if ( dirent->get_stat() != NULL )
        isdir = dirent->get_stat()->ISDIR();
      else
        isdir = this->isdir( path / dirent->get_name() );

      if ( isdir )
      {
        if
        (
          dirent->get_name() != Path( "." )
          &&
          dirent->get_name() != Path( ".." )
        )
        {
          if ( !rmtree( path / dirent->get_name() ) )
          {
            Directory::Entry::dec_ref( *dirent );
            Directory::dec_ref( *dir );
            return false;
          }
        }
      }
      else
      {
        if ( !unlink( path / dirent->get_name() ) )
        {
          Directory::Entry::dec_ref( *dirent );
          Directory::dec_ref( *dir );
          return false;
        }
      }

      Directory::Entry::dec_ref( *dirent );

      dirent = dir->readdir();
    }

    Directory::dec_ref( *dir );

    return rmdir( path );
  }
  else
    return false;
}

bool Volume::setattr( const Path& path, const Stat& stbuf, uint32_t to_set )
{
#ifdef _WIN32
  if
  (
    ( to_set & SETATTR_ATIME ) == SETATTR_ATIME ||
    ( to_set & SETATTR_MTIME ) == SETATTR_MTIME ||
    ( to_set & SETATTR_CTIME ) == SETATTR_CTIME
  )
  {
    File* file = open( path, O_WRONLY );
    if ( file!= NULL )
    {
      FILETIME ftCreationTime = stbuf.get_ctime(),
               ftLastAccessTime = stbuf.get_atime(),
               ftLastWriteTime = stbuf.get_mtime();

      bool ret = SetFileTime
                 (
                   *file,
                   ( to_set & SETATTR_CTIME ) == SETATTR_CTIME
                     ? &ftCreationTime : NULL,
                   ( to_set & SETATTR_ATIME ) == SETATTR_ATIME
                     ? &ftLastAccessTime : NULL,
                   ( to_set & SETATTR_MTIME ) == SETATTR_MTIME
                     ? &ftLastWriteTime : NULL
                 ) != 0;

      File::dec_ref( * file );

      return ret;

    }
    else
      return false;
  }

  if ( ( to_set & SETATTR_ATTRIBUTES ) == SETATTR_ATTRIBUTES )
  {
    if ( SetFileAttributes( path, stbuf.get_attributes() ) == 0 )
      return false;
  }
#else
  if ( ( to_set & SETATTR_MODE ) == SETATTR_MODE )
  {
    if ( ::chmod( path, stbuf.get_mode() ) == -1 )
      return false;
  }

  if ( ( to_set & SETATTR_UID ) == SETATTR_UID )
  {
    if ( ( to_set & SETATTR_GID ) == SETATTR_GID ) // Change both
    {
      if ( ::chown( path, stbuf.get_uid(), stbuf.get_gid() ) == -1 )
        return false;
    }
    else // Only change the uid
    {
      if ( ::chown( path, stbuf.get_uid(), -1 ) == -1 )
        return false;
    }
  }
  else if ( ( to_set & SETATTR_GID ) == SETATTR_GID ) // Only change the gid
  {
    if ( ::chown( path, -1, stbuf.get_gid() ) == -1 )
      return false;
  }

  if
  (
    ( to_set & SETATTR_ATIME ) == SETATTR_ATIME ||
    ( to_set & SETATTR_MTIME ) == SETATTR_MTIME
  )
  {
    struct timeval tv[2];
    tv[0] = stbuf.get_atime();
    tv[1] = stbuf.get_mtime();
    if ( ::utimes( path, tv ) == -1 )
      return false;
  }
#endif

  return true;
}

bool
Volume::setxattr
(
  const Path& path,
  const string& name,
  const string& value,
  int flags
)
{
#if defined(YIELD_PLATFORM_HAVE_XATTR_H)
  return SETXATTR
         (
           path,
           name.c_str(),
           value.c_str(),
           value.size(),
           flags
         ) != -1;
#elif defined(_WIN32)
  ::SetLastError( ERROR_NOT_SUPPORTED );
#else
  errno = ENOTSUP;
#endif
  return false;
}

Stat* Volume::stat( const Path& path )
{
  return getattr( path );
}

bool Volume::statvfs( const Path& path, struct statvfs& buffer )
{
#ifdef _WIN32
  ULARGE_INTEGER uFreeBytesAvailableToCaller,
                 uTotalNumberOfBytes,
                 uTotalNumberOfFreeBytes;
  if
  (
    GetDiskFreeSpaceEx
    (
      path,
      &uFreeBytesAvailableToCaller,
      &uTotalNumberOfBytes,
      &uTotalNumberOfFreeBytes
    ) != 0
  )
  {
    buffer.f_bsize = 4096;
    buffer.f_frsize = 4096;

    buffer.f_blocks
      = static_cast<fsblkcnt_t>( uTotalNumberOfBytes.QuadPart / 4096 );

    buffer.f_bfree
      = static_cast<fsblkcnt_t>( uTotalNumberOfFreeBytes.QuadPart / 4096 );

    buffer.f_bavail
      = static_cast<fsblkcnt_t>( uFreeBytesAvailableToCaller.QuadPart / 4096 );

    buffer.f_namemax = PATH_MAX;
    return true;
  }
  else
    return false;
#else
  return ::statvfs( path, &buffer ) == 0;
#endif
}

bool Volume::symlink( const Path& old_path, const Path& new_path )
{
#ifdef _WIN32
  ::SetLastError( ERROR_NOT_SUPPORTED );
  return false;
#else
  return ::symlink( old_path, new_path ) != -1;
#endif
}

bool Volume::touch( const Path& path )
{
  File* file = creat( path );
  if ( file != NULL )
  {
    File::dec_ref( *file );
    return true;
  }
  else
    return false;
}

bool Volume::truncate( const Path& path, uint64_t new_size )
{
#ifdef _WIN32
  File* file = Volume::open( path, O_CREAT|O_WRONLY, FILE_MODE_DEFAULT );
  if ( file!= NULL )
  {
    file->truncate( new_size );
    File::dec_ref( *file );
    return true;
  }
  else
    return false;
#else
  return ::truncate( path, new_size ) >= 0;
#endif
}

bool Volume::unlink( const Path& path )
{
#ifdef _WIN32
  return DeleteFileW( path ) != 0;
#else
  return ::unlink( path ) >= 0;
#endif
}

bool
Volume::utime
(
  const Path& path,
  const Time& atime,
  const Time& mtime
)
{
  Stat stbuf;
  stbuf.set_atime( atime );
  stbuf.set_mtime( mtime );
  return setattr( path, stbuf, SETATTR_ATIME|SETATTR_MTIME );
}

bool
Volume::utime
(
  const Path& path,
  const Time& atime,
  const Time& mtime,
  const Time& ctime
)
{
  Stat stbuf;
  stbuf.set_atime( atime );
  stbuf.set_mtime( mtime );
  stbuf.set_ctime( ctime );
  return setattr( path, stbuf, SETATTR_ATIME|SETATTR_MTIME|SETATTR_CTIME );
}

Path Volume::volname( const Path& path )
{
#ifdef _WIN32
  wchar_t file_system_name[PATH_MAX], volume_name[PATH_MAX];

  if
  (
    GetVolumeInformation
    (
      path.root_path(),
      volume_name,
      PATH_MAX,
      NULL,
      NULL,
      NULL,
      file_system_name,
      PATH_MAX
    ) != 0
    &&
    wcsnlen( volume_name, PATH_MAX ) > 0
  )
    return Path( volume_name );
  else
#endif
    return path.root_path();
}

#ifdef _WIN32
#pragma warning( pop )
#endif


// win32_aio_queue.cpp
#ifdef _WIN32


#include <windows.h>


class Win32AIOQueue::WorkerThread : public Thread
{
public:
  WorkerThread( HANDLE hIoCompletionPort )
    : hIoCompletionPort( hIoCompletionPort )
  { }

  // Thread
  void run()
  {
    set_name( "Win32AIOQueue::WorkerThread" );

    for ( ;; )
    {
      DWORD dwBytesTransferred;
      ULONG_PTR ulCompletionKey;
      LPOVERLAPPED lpOverlapped;

      if
      (
        GetQueuedCompletionStatus
        (
          hIoCompletionPort,
          &dwBytesTransferred,
          &ulCompletionKey,
          &lpOverlapped,
          INFINITE
        )
      )
      {
        Win32AIOCB* aiocb = Win32AIOCB::from_OVERLAPPED( lpOverlapped );
        aiocb->onCompletion( dwBytesTransferred );
        delete aiocb;
      }
      else if ( lpOverlapped != NULL )
      {
        Win32AIOCB* aiocb = Win32AIOCB::from_OVERLAPPED( lpOverlapped );
        aiocb->onError( ::GetLastError() );
        delete aiocb;
      }
      else
        break;
    }
  }

private:
  HANDLE hIoCompletionPort;
};


Win32AIOQueue::Win32AIOQueue( HANDLE hIoCompletionPort )
  : hIoCompletionPort( hIoCompletionPort )
{
  uint16_t worker_thread_count
    = ProcessorSet::getOnlineLogicalProcessorCount();

  for
  (
    uint16_t worker_thread_i = 0;
    worker_thread_i < worker_thread_count;
    worker_thread_i++
  )
  {
    WorkerThread* worker_thread = new WorkerThread( hIoCompletionPort );
    worker_threads.push_back( worker_thread );
    worker_thread->start();
  }
}

Win32AIOQueue::~Win32AIOQueue()
{
  CloseHandle( hIoCompletionPort );

  for
  (
    vector<WorkerThread*>::iterator
      worker_thread_i = worker_threads.begin();
    worker_thread_i != worker_threads.end();
    worker_thread_i++
  )
  {
    ( *worker_thread_i )->join();
    WorkerThread::dec_ref( **worker_thread_i );
  }
}

bool Win32AIOQueue::associate( socket_t socket_ )
{
  return associate( reinterpret_cast<void*>( socket_ ) );
}

bool Win32AIOQueue::associate( HANDLE handle )
{
  return CreateIoCompletionPort
         (
           handle,
           hIoCompletionPort,
           0,
           0
         ) != INVALID_HANDLE_VALUE;
}

Win32AIOQueue& Win32AIOQueue::create()
{
  HANDLE hIoCompletionPort
    = CreateIoCompletionPort( INVALID_HANDLE_VALUE, NULL, 0, 0 );

  if ( hIoCompletionPort != INVALID_HANDLE_VALUE )
    return *new Win32AIOQueue( hIoCompletionPort );
  else
    throw Exception();
}

bool
Win32AIOQueue::post
(
  Win32AIOCB& win32_aiocb,
  DWORD dwNumberOfBytesTransferred,
  ULONG_PTR dwCompletionKey
)
{
  return PostQueuedCompletionStatus
         (
           hIoCompletionPort,
           dwNumberOfBytesTransferred,
           dwCompletionKey,
           win32_aiocb
         ) == TRUE;
}

#endif


// win32_aiocb.cpp
#ifdef _WIN32



Win32AIOCB::Win32AIOCB()
{
  memset( &overlapped, 0, sizeof( overlapped ) );
  overlapped.this_ = this;
}

void
Win32AIOCB::OverlappedCompletionRoutine
(
  unsigned long dwErrorCode,
  unsigned long dwNumberOfBytesTransferred,
  ::OVERLAPPED* lpOverlapped
)
{
  Win32AIOCB* aiocb = from_OVERLAPPED( lpOverlapped );
  if ( dwErrorCode == 0 )
    aiocb->onCompletion( dwNumberOfBytesTransferred );
  else
    aiocb->onError( dwErrorCode );
}

void
Win32AIOCB::WSAOverlappedCompletionRoutine
(
  unsigned long dwErrorCode,
  unsigned long dwNumberOfBytesTransferred,
  ::OVERLAPPED* lpOverlapped,
  unsigned long // dwFlags
)
{
  Win32AIOCB* aiocb = from_OVERLAPPED( lpOverlapped );
  if ( dwErrorCode == 0 )
    aiocb->onCompletion( dwNumberOfBytesTransferred );
  else
    aiocb->onError( dwErrorCode );
}

Win32AIOCB* Win32AIOCB::from_OVERLAPPED( ::OVERLAPPED* overlapped )
{
  return reinterpret_cast<OVERLAPPED*>( overlapped )->this_;
}

Win32AIOCB::operator OVERLAPPED*()
{
  return reinterpret_cast<::OVERLAPPED*>( &overlapped );
}

#endif

