// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/proxy.h"
#include "policy_container.h"
#include "platform_exception_event.h"
using namespace org::xtreemfs::client;

#include "org/xtreemfs/interfaces/constants.h"

#include <algorithm>

#ifdef _WIN32
#include "yield/platform/windows.h"
#define ETIMEDOUT WSAETIMEDOUT
#else
#include <errno.h>
#endif


Proxy::Proxy( const YIELD::URI& uri, uint16_t default_oncrpc_port )
  : uri( uri )
{
  if ( strcmp( uri.get_scheme(), org::xtreemfs::interfaces::ONCRPC_SCHEME ) == 0  )
  {
    if ( this->uri.get_port() == 0 )
      this->uri.set_port( default_oncrpc_port );

    peer_sockaddr = this->uri;

    ssl_context = NULL;
    init();
  }
  else
    throw YIELD::Exception( "invalid URI scheme" );
}

Proxy::Proxy( const YIELD::URI& uri, YIELD::SSLContext& ssl_context, uint16_t default_oncrpcs_port )
: uri( uri ), ssl_context( &ssl_context )
{
  if ( strcmp( uri.get_scheme(), org::xtreemfs::interfaces::ONCRPCS_SCHEME ) == 0 )
  {
    if ( this->uri.get_port() == 0 )
      this->uri.set_port( default_oncrpcs_port );

    peer_sockaddr = this->uri;

    init();
  }
  else
    throw YIELD::Exception( "invalid URI scheme" );
}

void Proxy::init()
{
  flags = PROXY_DEFAULT_FLAGS;
  reconnect_tries_max = PROXY_DEFAULT_RECONNECT_TRIES_MAX;
  operation_timeout_ms = PROXY_DEFAULT_OPERATION_TIMEOUT_MS;

  conn = NULL;

  org::xtreemfs::interfaces::Exceptions().registerObjectFactories( object_factories );
}

Proxy::~Proxy()
{
  delete conn;
  YIELD::Object::decRef( ssl_context );
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
            bool have_user_credentials = getCurrentUserCredentials( user_credentials );
            YIELD::ONCRPCRequest oncrpc_req( YIELD::Object::incRef( req ), object_factories, have_user_credentials ? org::xtreemfs::interfaces::ONCRPC_AUTH_FLAVOR : 0, &user_credentials );

            uint8_t reconnect_tries_left = reconnect_tries_max;
            if ( conn == NULL )
              reconnect_tries_left = reconnect( reconnect_tries_left );

            uint64_t remaining_operation_timeout_ms = operation_timeout_ms;
            if ( operation_timeout_ms == static_cast<uint64_t>( -1 ) || ssl_context != NULL )
              conn->setBlocking();
            else
              conn->setBlocking();

            bool have_written = false;

            for ( ;; )
            {   
              if ( !have_written )
              {
                if ( oncrpc_req.serialize( *conn ) )
                {
                  have_written = true;
                  continue;
                }
              }
              else
              {
                if ( oncrpc_req.deserialize( *conn ) )
                {
                  req.respond( static_cast<YIELD::Event&>( YIELD::Object::incRef( *oncrpc_req.getInBody() ) ) );
                  YIELD::Object::decRef( req );
                  return;
                }
              }

              if ( YIELD::Socket::WOULDBLOCK() )
              {
                if ( remaining_operation_timeout_ms > 0 )
                {
                  fd_event_queue.toggleSocketEvent( *conn, conn, have_written, !have_written );
                  double start_epoch_time_ms = YIELD::Time::getCurrentUnixTimeMS();
                  YIELD::FDEvent* fd_event = static_cast<YIELD::FDEvent*>( fd_event_queue.timed_dequeue( static_cast<YIELD::timeout_ns_t>( remaining_operation_timeout_ms ) * NS_IN_MS ) );
                  if ( fd_event )
                  {
                    if ( fd_event->error_code == 0 )
                      remaining_operation_timeout_ms -= std::min( static_cast<uint64_t>( YIELD::Time::getCurrentUnixTimeMS() - start_epoch_time_ms ), remaining_operation_timeout_ms );
                    else
                      reconnect_tries_left = reconnect( reconnect_tries_left );
                  }
                  else
                    throwExceptionEvent( new PlatformExceptionEvent( ETIMEDOUT ) );
                }
                else
                  throwExceptionEvent( new PlatformExceptionEvent( ETIMEDOUT ) );
              }
              else
                reconnect_tries_left = reconnect( reconnect_tries_left );
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

uint8_t Proxy::reconnect( uint8_t reconnect_tries_left )
{
  if ( conn != NULL ) // This is a reconnect, not the first connect
  {
    fd_event_queue.detachSocket( *conn, conn );
    conn->close();
    delete conn;
    conn = NULL;
  }

  while( reconnect_tries_left == static_cast<uint8_t>( -1 ) ||
         --reconnect_tries_left > 0 )
  {
    // Create the conn object based on the URI type
    if ( ssl_context == NULL )
      conn = new YIELD::TCPSocket;
    else
      conn = new YIELD::SSLSocket( *ssl_context );

    // Attach the socket to the fd_event_queue even if we're doing a blocking connect, in case a later read/write is non-blocking
    fd_event_queue.attachSocket( *conn, conn, false, false ); // Attach without read or write notifications enabled

    // Now try the actual connect
    if ( operation_timeout_ms == static_cast<uint64_t>( -1 ) || ssl_context != NULL ) // Blocking
    {
      conn->setBlocking();
      if ( conn->connect( peer_sockaddr ) )
        return reconnect_tries_left;
    }
    else // Non-blocking/timed
    {
      uint64_t remaining_operation_timeout_ms = operation_timeout_ms;
      conn->setNonBlocking();

      if ( conn->connect( peer_sockaddr ) )
        break;
      else if ( YIELD::Socket::WOULDBLOCK() )
      {
        fd_event_queue.toggleSocketEvent( *conn, conn, false, true ); // Write readiness = the connect() operation is complete
        double start_epoch_time_ms = YIELD::Time::getCurrentUnixTimeMS();
        YIELD::FDEvent* fd_event = static_cast<YIELD::FDEvent*>( fd_event_queue.timed_dequeue( static_cast<YIELD::timeout_ns_t>( remaining_operation_timeout_ms ) * NS_IN_MS ) );
        if ( fd_event && fd_event->error_code == 0 && conn->connect( peer_sockaddr ) )
        {
          fd_event_queue.toggleSocketEvent( *conn, conn, false, false );
          return reconnect_tries_left;
        }
        else
        {
          remaining_operation_timeout_ms -= std::min( static_cast<uint64_t>( YIELD::Time::getCurrentUnixTimeMS() - start_epoch_time_ms ), remaining_operation_timeout_ms );
          if ( remaining_operation_timeout_ms == 0 ) { reconnect_tries_left = 0; break; }
        }
      }
    }

    // Clear the connection state for the next try
    fd_event_queue.detachSocket( *conn, conn );
    conn->close();
    delete conn;
    conn = NULL;
  }

  unsigned long error_code = YIELD::PlatformException::errno_();
  if ( error_code == 0 )
    error_code = ETIMEDOUT;
  throw new PlatformExceptionEvent( error_code );
}

void Proxy::throwExceptionEvent( YIELD::ExceptionEvent* exc_ev )
{
  if ( conn )
  {
    fd_event_queue.detachSocket( *conn, conn );
    conn->close();
    delete conn;
    conn = NULL;
  }

  throw exc_ev;
}
