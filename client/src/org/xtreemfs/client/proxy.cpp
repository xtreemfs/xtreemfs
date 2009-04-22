// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/proxy.h"
#include "policy_container.h"
using namespace org::xtreemfs::client;

#include "org/xtreemfs/interfaces/constants.h"

#include <algorithm>

#ifdef _WIN32
#include "yield/platform/windows.h"
#define ETIMEDOUT WSAETIMEDOUT
#else
#include <errno.h>
#endif

#define ORG_XTREEMFS_CLIENT_PROXY_CONNECTION_TIMEOUT_MS 5000


Proxy::Proxy( const YIELD::URI& uri, uint16_t default_oncrpc_port )
  : uri( uri )
{
  ssl_context = NULL;
  log = NULL;
  init( default_oncrpc_port );
}

Proxy::Proxy( const YIELD::URI& uri, YIELD::Log& log, uint16_t default_oncrpc_port )
  : uri( uri ), log( &log )
{
  ssl_context = NULL;
  init( default_oncrpc_port );
}

Proxy::Proxy( const YIELD::URI& uri, YIELD::SSLContext& ssl_context, uint16_t default_oncrpcs_port )
: uri( uri ), ssl_context( &ssl_context )
{
  log = NULL;
  init( default_oncrpcs_port );
}

Proxy::Proxy( const YIELD::URI& uri, YIELD::SSLContext& ssl_context, YIELD::Log& log, uint16_t default_oncrpcs_port )
: uri( uri ), ssl_context( &ssl_context ), log( &log )
{
  init( default_oncrpcs_port );
}

void Proxy::init( uint16_t default_port )
{
  if ( this->uri.get_port() == 0 )
    this->uri.set_port( default_port );

  peer_sockaddr = this->uri;

  policies = new PolicyContainer;

  flags = PROXY_DEFAULT_FLAGS;
  reconnect_tries_max = PROXY_DEFAULT_RECONNECT_TRIES_MAX;
  operation_timeout_ms = PROXY_DEFAULT_OPERATION_TIMEOUT_MS;

  conn = NULL;

  org::xtreemfs::interfaces::Exceptions().registerObjectFactories( object_factories );
}

Proxy::~Proxy()
{
  delete policies;
  clearConnectionState();
  YIELD::Object::decRef( ssl_context );
  YIELD::Object::decRef( log );
}

void Proxy::handleEvent( YIELD::Event& ev )
{
  switch ( ev.get_type_id() )
  {
    case YIELD_OBJECT_TYPE_ID( YIELD::StageStartupEvent ):
    case YIELD_OBJECT_TYPE_ID( YIELD::StageShutdownEvent ): YIELD::Object::decRef( ev ); break;

    default:
    {
      switch ( ev.get_general_type() )
      {
        case YIELD::Object::REQUEST:
        {
          YIELD::Request& req = static_cast<YIELD::Request&>( ev );
          if ( ( flags & PROXY_FLAG_PRINT_OPERATIONS ) == PROXY_FLAG_PRINT_OPERATIONS )
          {
            YIELD::PrettyPrintOutputStream pretty_print_output_stream( std::cout );
            pretty_print_output_stream.writeObject( YIELD::PrettyPrintOutputStream::Declaration( req.get_type_name() ), req );
          }

          try
          {
            org::xtreemfs::interfaces::UserCredentials user_credentials;
            policies->getCurrentUserCredentials( user_credentials );
            YIELD::auto_Object<YIELD::ONCRPCRequest> oncrpc_req;

            uint8_t reconnect_tries_left = reconnect_tries_max;
            if ( conn == NULL )
              reconnect_tries_left = reconnect( reconnect_tries_left );

            uint64_t remaining_operation_timeout_ms = operation_timeout_ms;
            bool have_written = false;

            for ( ;; )
            {
              if ( oncrpc_req.get() == NULL )
                oncrpc_req = new YIELD::ONCRPCRequest( YIELD::Object::incRef( req ), object_factories, org::xtreemfs::interfaces::ONCRPC_AUTH_FLAVOR, &user_credentials );

              conn->setBlockingMode( operation_timeout_ms == static_cast<uint64_t>( -1 ) );

              if ( !have_written )
              {
                if ( oncrpc_req.get()->serialize( *conn ) )
                {
                  have_written = true;
                  continue;
                }
              }
              else
              {
                if ( oncrpc_req.get()->deserialize( *conn ) )
                {
                  req.respond( static_cast<YIELD::Event&>( YIELD::Object::incRef( *oncrpc_req.get()->getInBody() ) ) );
                  YIELD::Object::decRef( req );
                  return;
                }
              }

              bool conn_want_read = conn->want_read(), conn_want_write = conn_want_read ? false : conn->want_write();
              if ( conn_want_read || conn_want_write )
              {
#ifdef _DEBUG
                if ( remaining_operation_timeout_ms == static_cast<uint64_t>( -1 ) )
                  YIELD::DebugBreak(); // Logic error: the socket was left in non-blocking mode even though the operation timeout is infinite
#endif

                if ( remaining_operation_timeout_ms > 0 )
                {
                  double start_unix_time_ms = YIELD::Time::getCurrentUnixTimeMS();
                  fd_event_queue.toggleSocketEvent( *conn, conn, conn_want_read, conn_want_write );
                  YIELD::FDEvent* fd_event = static_cast<YIELD::FDEvent*>( fd_event_queue.timed_dequeue( static_cast<YIELD::timeout_ns_t>( remaining_operation_timeout_ms ) * NS_IN_MS ) );
                  fd_event_queue.toggleSocketEvent( *conn, conn, false, false );
                  if ( fd_event )
                  {
                    if ( fd_event->error_code == 0 )
                      remaining_operation_timeout_ms -= std::min( static_cast<uint64_t>( YIELD::Time::getCurrentUnixTimeMS() - start_unix_time_ms ), remaining_operation_timeout_ms );
                    else if ( reconnect_tries_left == static_cast<uint8_t>( -1 ) || reconnect_tries_left > 0 )
                    {
                      reconnect_tries_left = reconnect( reconnect_tries_left );
                      have_written = false;
                      oncrpc_req.reset( NULL );
                    }
                  }
                  else
                  {
                    if ( log != NULL )
                      log->getStream( YIELD::Log::LOG_ERR ) << getEventHandlerName() << ": " << ev.get_type_name() << " timed out.";

                    clearConnectionState();
                    throw new org::xtreemfs::interfaces::Exceptions::errnoException( ETIMEDOUT, "timed out", "" );
                  }
                }
                else
                {
                  if ( log != NULL )
                    log->getStream( YIELD::Log::LOG_ERR ) << getEventHandlerName() << ": " << ev.get_type_name() << " timed out.";

                  clearConnectionState();
                  throw new org::xtreemfs::interfaces::Exceptions::errnoException( ETIMEDOUT, "timed out", "" );
                }
              }
              else
              {
                if ( log != NULL )
                {
                  YIELD::PlatformException platform_exc;
                  log->getStream( YIELD::Log::LOG_ERR ) << getEventHandlerName() << ": lost connection while trying to send " << ev.get_type_name() << " (error_code = " << platform_exc.get_error_code() << ", what = " << platform_exc.what() << ").";
                }

                if ( reconnect_tries_left == static_cast<uint8_t>( -1 ) || reconnect_tries_left > 0 )
                {
                  YIELD::Thread::sleep( static_cast<YIELD::timeout_ns_t>( ORG_XTREEMFS_CLIENT_PROXY_CONNECTION_TIMEOUT_MS ) * NS_IN_MS );
                  reconnect_tries_left = reconnect( reconnect_tries_left );
                  have_written = false;
                  oncrpc_req.reset( NULL );
                }
              }
            }
          }
          catch ( YIELD::ExceptionEvent* exc_ev )
          {
            req.respond( *exc_ev );
            YIELD::Object::decRef( req );
          }
        }
        break;

        default: YIELD::DebugBreak(); break;
      }
    }
    break;
  }
}

void Proxy::clearConnectionState()
{
  if ( conn != NULL )
  {
    fd_event_queue.detachSocket( *conn, conn );
    conn->close();
    delete conn;
    conn = NULL;
  }
}

uint8_t Proxy::reconnect( uint8_t reconnect_tries_left )
{
  if ( conn == NULL ) // This the first connect
  {
    if ( log != NULL )
      log->getStream( YIELD::Log::LOG_NOTICE ) << getEventHandlerName() << ": connecting to " << this->uri.get_host() << ":" << this->uri.get_port() << ".";
  }
  else // This is a reconnect
  {
    clearConnectionState();
    if ( log != NULL )
      log->getStream( YIELD::Log::LOG_WARNING ) << getEventHandlerName() << ": reconnecting to " << this->uri.get_host() << ":" << this->uri.get_port() << ".";
  }

  // Create the conn object based on the URI type
  if ( ssl_context == NULL )
    conn = new YIELD::TCPSocket( YIELD::Object::incRef( log ) );
  else
    conn = new YIELD::SSLSocket( ssl_context->incRef(), YIELD::Object::incRef( log ) );

  conn->setBlockingMode( false ); // So we can do connects() with timeouts shorter than the default OS timeout

  // Attach the socket to the fd_event_queue even if we're doing a blocking connect, in case a later read/write is non-blocking
  fd_event_queue.attachSocket( *conn, conn, false, false ); // Attach without read or write notifications enabled

  for ( ;; )
  {
    if ( log != NULL )
      log->getStream( YIELD::Log::LOG_INFO ) << getEventHandlerName() << ": trying to connect to " << this->uri.get_host() << ":" << this->uri.get_port() << ".";

    if ( conn->connect( peer_sockaddr ) )
    {
      if ( log != NULL )
        log->getStream( YIELD::Log::LOG_INFO ) << getEventHandlerName() << ": successfully connected to " << this->uri.get_host() << ":" << this->uri.get_port() << ".";

      return reconnect_tries_left;
    }
    else if ( reconnect_tries_left == static_cast<uint8_t>( -1 ) || --reconnect_tries_left > 0 )
    {
#ifdef _WIN32
      if ( ::WSAGetLastError() == EINPROGRESS )
#else
      if ( errno == EINPROGRESS )
#endif
      {
        fd_event_queue.toggleSocketEvent( *conn, conn, false, true ); // Write readiness = the connect() operation is complete
        uint64_t start_unix_time_ns = YIELD::Time::getCurrentUnixTimeNS();
        YIELD::FDEvent* fd_event = static_cast<YIELD::FDEvent*>( fd_event_queue.timed_dequeue( static_cast<YIELD::timeout_ns_t>( ORG_XTREEMFS_CLIENT_PROXY_CONNECTION_TIMEOUT_MS ) * NS_IN_MS ) );
        fd_event_queue.toggleSocketEvent( *conn, conn, false, false );
        if ( fd_event == NULL )
        {
          uint64_t elapsed_time_ns = YIELD::Time::getCurrentUnixTimeNS() - start_unix_time_ns;
          if ( elapsed_time_ns < static_cast<YIELD::timeout_ns_t>( ORG_XTREEMFS_CLIENT_PROXY_CONNECTION_TIMEOUT_MS ) * NS_IN_MS )
          {
            // Sometimes select() does not wait for the full time out
            // If we did not get a notification, sleep for the rest of the time out
            uint64_t sleep_time_ns = elapsed_time_ns - static_cast<YIELD::timeout_ns_t>( ORG_XTREEMFS_CLIENT_PROXY_CONNECTION_TIMEOUT_MS ) * NS_IN_MS;
            if ( sleep_time_ns > 2 * NS_IN_MS )
              YIELD::Thread::sleep( sleep_time_ns );
          }
        }
      }
      else if ( log != NULL )
      {
        YIELD::PlatformException platform_exc;
        log->getStream( YIELD::Log::LOG_ERR ) << getEventHandlerName() << ": connect() to " << this->uri.get_host() << ":" << this->uri.get_port() << " failed (error_code = " << platform_exc.get_error_code() << ", what = " << platform_exc.what() << ").";
        YIELD::Thread::sleep( static_cast<YIELD::timeout_ns_t>( ORG_XTREEMFS_CLIENT_PROXY_CONNECTION_TIMEOUT_MS ) * NS_IN_MS );
      }
    }
    else
      break;
  }

#ifdef _WIN32
  if ( GetLastError() != ERROR_SUCCESS )
    throw new org::xtreemfs::interfaces::Exceptions::errnoException( static_cast<uint32_t>( ::GetLastError() ), "", "" );
#else
  if ( errno != 0 )
    throw new org::xtreemfs::interfaces::Exceptions::errnoException( static_cast<uint32_t>( errno ), "", "" );
#endif

  throw new org::xtreemfs::interfaces::Exceptions::errnoException( ETIMEDOUT, "timed out", "" );
}
