// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/proxy.h"
#include "policy_container.h"
#include "platform_exception_event.h"
using namespace org::xtreemfs::client;

#include "org/xtreemfs/interfaces/constants.h"

#include <errno.h>
#ifdef _WIN32
#include "yield/ipc/sockets.h"
#define ETIMEDOUT WSAETIMEDOUT
#endif

#include <openssl/err.h>
#include <openssl/pkcs12.h>
#include <openssl/ssl.h>



Proxy::Proxy( const YIELD::URI& uri, uint16_t default_oncrpc_port )
  : uri( uri )
{
  if ( strcmp( uri.get_scheme(), org::xtreemfs::interfaces::ONCRPC_SCHEME ) == 0  )
  {
    if ( this->uri.get_port() == 0 )
      this->uri.set_port( default_oncrpc_port );

    ssl_context = NULL;
    init();
  }
  else
    throw YIELD::Exception( "invalid URI scheme" );
}

Proxy::Proxy( const YIELD::URI& uri, const YIELD::SSLContext& ssl_context, uint16_t default_oncrpcs_port )
: uri( uri )
{
  if ( strcmp( uri.get_scheme(), org::xtreemfs::interfaces::ONCRPCS_SCHEME ) == 0 )
  {
    if ( this->uri.get_port() == 0 )
      this->uri.set_port( default_oncrpcs_port );

    this->ssl_context = new YIELD::SSLContext( ssl_context );
    init();
  }
  else
    throw YIELD::Exception( "invalid URI scheme" );
}

void Proxy::init()
{
  this->flags = PROXY_DEFAULT_FLAGS;
  this->reconnect_tries_max = PROXY_DEFAULT_RECONNECT_TRIES_MAX;
  this->operation_timeout_ms = PROXY_DEFAULT_OPERATION_TIMEOUT_MS;

  this->peer_ip = 0;
  this->conn = 0;

  org::xtreemfs::interfaces::Exceptions().registerSerializableFactories( serializable_factories );
}

Proxy::~Proxy()
{
  delete conn;
}

void Proxy::handleEvent( YIELD::Event& ev )
{
  switch ( ev.getTypeId() )
  {
    case TYPE_ID( YIELD::StageStartupEvent ):
    case TYPE_ID( YIELD::StageShutdownEvent ): YIELD::SharedObject::decRef( ev ); break;

    default:
    {
      switch ( ev.getGeneralType() )
      {
        case YIELD::RTTI::REQUEST:
        {
          YIELD::Request& req = static_cast<YIELD::Request&>( ev );
          if ( ( flags & PROXY_FLAG_PRINT_OPERATIONS ) == PROXY_FLAG_PRINT_OPERATIONS )
          {
            YIELD::PrettyPrintOutputStream pretty_print_output_stream( std::cout );
            pretty_print_output_stream.writeSerializable( YIELD::PrettyPrintOutputStream::Declaration( req.getTypeName() ), req );
          }

          try
          {
            org::xtreemfs::interfaces::UserCredentials user_credentials;
            bool have_user_credentials = getCurrentUserCredentials( user_credentials );
            YIELD::ONCRPCRequest oncrpc_req( YIELD::SharedObject::incRef( req ), serializable_factories, have_user_credentials ? org::xtreemfs::interfaces::ONCRPC_AUTH_FLAVOR : 0, &user_credentials );

            uint8_t reconnect_tries_left = reconnect_tries_max;

            if ( conn == NULL )
              reconnect_tries_left = reconnect( reconnect_tries_left );

            if ( operation_timeout_ms == static_cast<uint64_t>( -1 ) || ssl_context != NULL ) // Blocking
            {
              YIELD::SocketLib::setBlocking( conn->get_socket() );

              for ( uint8_t serialize_tries = 0; serialize_tries < 2; serialize_tries++ ) // Try, allow for one reconnect, then try again
              {
                oncrpc_req.serialize( *conn, NULL );

                if ( conn->get_status() == YIELD::SocketConnection::SCS_READY )
                {
                  for ( uint8_t deserialize_tries = 0; deserialize_tries < 2; deserialize_tries++ )
                  {
                    oncrpc_req.deserialize( *conn );

                    if ( conn->get_status() == YIELD::SocketConnection::SCS_READY )
                    {
                      req.respond( static_cast<YIELD::Event&>( YIELD::SharedObject::incRef( *oncrpc_req.getInBody() ) ) );
                      YIELD::SharedObject::decRef( req );
                      return;
                    }
                    else if ( conn->get_status() == YIELD::SocketConnection::SCS_CLOSED )
                      reconnect_tries_left = reconnect( reconnect_tries_left );
                    else
                      YIELD::DebugBreak();
                  }

                  YIELD::SharedObject::decRef( req );
                  return;
                }
                else if ( conn->get_status() == YIELD::SocketConnection::SCS_CLOSED )
                  reconnect_tries_left = reconnect( reconnect_tries_left );
                else
                  YIELD::DebugBreak();
              }

              YIELD::SharedObject::decRef( req );
              return;
            }
            else // Non-blocking/timed
            {
              uint64_t remaining_operation_timeout_ms = operation_timeout_ms;
              YIELD::SocketLib::setNonBlocking( conn->get_socket() );

              bool have_written = false; // Use the variable so the read and write attempt loops can be combined and eliminate some code duplication

              for ( ;; ) // Loop for read and write attempts
              {
                if ( !have_written )
                  oncrpc_req.serialize( *conn );
                else
                  oncrpc_req.deserialize( *conn );

                // Use if statements instead of a switch so the break after a successful read will exit the loop
                if ( conn->get_status() == YIELD::SocketConnection::SCS_READY )
                {
                  if ( !have_written )
                    have_written = true;
                  else
                  {
                    YIELD::SharedObject::decRef( req );
                    return;
                  }
                }
                else if ( conn->get_status() == YIELD::SocketConnection::SCS_CLOSED )
                  reconnect_tries_left = reconnect( reconnect_tries_left );
                else if ( remaining_operation_timeout_ms > 0 )
                {
                  bool enable_read = conn->get_status() == YIELD::SocketConnection::SCS_BLOCKED_ON_READ;
                  fd_event_queue.toggleSocketEvent( conn->get_socket(), conn, enable_read, !enable_read );
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
            }
          }
          catch ( YIELD::ExceptionEvent* exc_ev )
          {
            req.respond( *exc_ev );
            YIELD::SharedObject::decRef( req );
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
  if ( peer_ip == 0 )
  {
    peer_ip = YIELD::SocketLib::resolveHost( uri.get_host() );
    if ( peer_ip == 0 && ( strcmp( uri.get_host(), "localhost" ) == 0 || strcmp( uri.get_host(), "127.0.0.1" ) == 0 ) )
      peer_ip = YIELD::SocketLib::resolveHost( YIELD::SocketLib::getLocalHostFQDN() );
    if ( peer_ip == 0 )
      throw new PlatformExceptionEvent();
  }

  if ( conn != NULL ) // This is a reconnect, not the first connect
  {
    fd_event_queue.detachSocket( conn->get_socket(), conn );
    conn->close();
    delete conn;
    conn = NULL;
  }

  while ( reconnect_tries_left > 0 )
  {
    reconnect_tries_left--;

    // Create the conn object based on the URI type
    if ( ssl_context == NULL )
      conn = new YIELD::TCPConnection( peer_ip, uri.get_port(), NULL );
    else
      conn = new YIELD::SSLConnection( peer_ip, uri.get_port(), ssl_context );

    // Attach the socket to the fd_event_queue even if we're doing a blocking connect, in case a later read/write is non-blocking
    fd_event_queue.attachSocket( conn->get_socket(), conn, false, false ); // Attach without read or write notifications enabled

    // Now try the actual connect
    if ( operation_timeout_ms == static_cast<uint64_t>( -1 ) || ssl_context != NULL ) // Blocking
    {
      YIELD::SocketLib::setBlocking( conn->get_socket() );

      if ( conn->connect() == YIELD::SocketConnection::SCS_READY )
        return reconnect_tries_left;
    }
    else // Non-blocking/timed
    {
      uint64_t remaining_operation_timeout_ms = operation_timeout_ms;
      YIELD::SocketLib::setNonBlocking( conn->get_socket() );

      switch ( conn->connect() )
      {
        case YIELD::SocketConnection::SCS_READY: break;
        case YIELD::SocketConnection::SCS_BLOCKED_ON_WRITE:
        {
          fd_event_queue.toggleSocketEvent( conn->get_socket(), conn, false, true ); // Write readiness = the connect() operation is complete
          double start_epoch_time_ms = YIELD::Time::getCurrentUnixTimeMS();
          YIELD::FDEvent* fd_event = static_cast<YIELD::FDEvent*>( fd_event_queue.timed_dequeue( static_cast<YIELD::timeout_ns_t>( remaining_operation_timeout_ms ) * NS_IN_MS ) );
          if ( fd_event && fd_event->error_code == 0 &&
               conn->connect() == YIELD::SocketConnection::SCS_READY )
          {
            fd_event_queue.toggleSocketEvent( conn->get_socket(), conn, false, false );
            return reconnect_tries_left;
          }
          else
          {
            remaining_operation_timeout_ms -= std::min( static_cast<uint64_t>( YIELD::Time::getCurrentUnixTimeMS() - start_epoch_time_ms ), remaining_operation_timeout_ms );
            if ( remaining_operation_timeout_ms == 0 ) { reconnect_tries_left = 0; break; }
          }
        }
      }
    }

    // Clear the connection state for the next try
    fd_event_queue.detachSocket( conn->get_socket(), conn );
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
    fd_event_queue.detachSocket( conn->get_socket(), conn );
    conn->close();
    delete conn;
    conn = NULL;
  }

  throw exc_ev;
}
