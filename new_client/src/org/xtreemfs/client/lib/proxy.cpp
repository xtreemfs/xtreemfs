#include "org/xtreemfs/client/proxy.h"
#include "policy_container.h"
using namespace org::xtreemfs::client;

#include "org/xtreemfs/interfaces/constants.h"

#include <errno.h>
#ifdef _WIN32
#include "yield/ipc/sockets.h"
#define ETIMEDOUT WSAETIMEDOUT
#endif


Proxy::Proxy( const YIELD::URI& uri, uint16_t default_oncrpc_port, uint16_t default_oncrpcs_port ) : uri( uri )
{
  if ( strcmp( uri.getScheme(), org::xtreemfs::interfaces::ONCRPC_SCHEME ) == 0 || strcmp( uri.getScheme(), org::xtreemfs::interfaces::ONCRPCS_SCHEME ) == 0 )
  {
    if ( this->uri.getPort() == 0 )
    {
      if ( strcmp( this->uri.getScheme(), org::xtreemfs::interfaces::ONCRPC_SCHEME ) == 0 )
        this->uri.setPort( default_oncrpc_port );
      else if ( strcmp( this->uri.getScheme(), org::xtreemfs::interfaces::ONCRPCS_SCHEME ) == 0 )
        this->uri.setPort( default_oncrpcs_port );
      else
        YIELD::DebugBreak();
    }

    this->reconnect_tries_max = PROXY_DEFAULT_RECONNECT_TRIES_MAX;
    this->flags = PROXY_DEFAULT_FLAGS;
    this->peer_ip = 0;
    this->conn = 0;
  
    org::xtreemfs::interfaces::Exceptions().registerSerializableFactories( serializable_factories );
  }
  else
    throw YIELD::Exception( "unknown URI scheme" );
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
            YIELD::auto_SharedObject<org::xtreemfs::interfaces::UserCredentials> user_credentials = get_user_credentials();
            YIELD::ONCRPCRequest oncrpc_req( YIELD::SharedObject::incRef( req ), serializable_factories, user_credentials.get() ? org::xtreemfs::interfaces::ONCRPC_AUTH_FLAVOR : 0, user_credentials.get() );

            uint64_t timeout_ms = req.getResponseTimeoutMS();
            uint64_t original_timeout_ms = timeout_ms;
            uint8_t reconnect_tries_left = reconnect_tries_max;

            if ( conn == NULL )
              reconnect_tries_left = reconnect( original_timeout_ms, reconnect_tries_left );

            if ( timeout_ms == static_cast<uint64_t>( -1 ) ) // Blocking
            {
              YIELD::SocketLib::setBlocking( conn->getSocket() );

              for ( uint8_t serialize_tries = 0; serialize_tries < 2; serialize_tries++ ) // Try, allow for one reconnect, then try again
              {
                oncrpc_req.serialize( *conn );
                if ( conn->getStatus() == YIELD::SocketConnection::SCS_READY )
                {
                  for ( uint8_t deserialize_tries = 0; deserialize_tries < 2; deserialize_tries++ )
                  {
                    oncrpc_req.deserialize( *conn );
                    if ( conn->getStatus() == YIELD::SocketConnection::SCS_READY )
                    {
                      req.respond( static_cast<YIELD::Event&>( YIELD::SharedObject::incRef( *oncrpc_req.getInBody() ) ) );
                      YIELD::SharedObject::decRef( req );
                      return;
                    }
                    else if ( conn->getStatus() == YIELD::SocketConnection::SCS_CLOSED )
                      reconnect_tries_left = reconnect( original_timeout_ms, reconnect_tries_left );
                    else
                      YIELD::DebugBreak();
                  }
                }
                else if ( conn->getStatus() == YIELD::SocketConnection::SCS_CLOSED )
                  reconnect_tries_left = reconnect( original_timeout_ms, reconnect_tries_left );
                else
                  YIELD::DebugBreak();
              }
            }
            else // Non-blocking/timed
            {
              YIELD::SocketLib::setNonBlocking( conn->getSocket() );

              bool have_written = false; // Use the variable so the read and write attempt loops can be combined and eliminate some code duplication

              while ( true ) // Loop for read and write attempts
              {
                if ( !have_written )
                  oncrpc_req.serialize( *conn );
                else
                  oncrpc_req.deserialize( *conn );

                // Use if statements instead of a switch so the break after a successful read will exit the loop
                if ( conn->getStatus() == YIELD::SocketConnection::SCS_READY )
                {
                  if ( !have_written )
                    have_written = true;
                  else
                    break;
                }
                else if ( conn->getStatus() == YIELD::SocketConnection::SCS_CLOSED )
                  reconnect_tries_left = reconnect( original_timeout_ms, reconnect_tries_left );
                else if ( timeout_ms > 0 )
                {
                  bool enable_read = conn->getStatus() == YIELD::SocketConnection::SCS_BLOCKED_ON_READ;
                  fd_event_queue.toggleSocketEvent( conn->getSocket(), conn, enable_read, !enable_read );
                  double start_epoch_time_ms = YIELD::Time::getCurrentEpochTimeMS();
                  YIELD::FDEvent* fd_event = static_cast<YIELD::FDEvent*>( fd_event_queue.timed_dequeue( static_cast<YIELD::timeout_ns_t>( timeout_ms ) * NS_IN_MS ) );
                  if ( fd_event )
                  {
                    if ( fd_event->error_code == 0 )
                      timeout_ms -= std::min( static_cast<uint64_t>( YIELD::Time::getCurrentEpochTimeMS() - start_epoch_time_ms ), timeout_ms );
                    else
                      reconnect_tries_left = reconnect( original_timeout_ms, reconnect_tries_left );
                  }
                  else
                    throwExceptionEvent( new YIELD::PlatformExceptionEvent( ETIMEDOUT ) );
                }
                else
                  throwExceptionEvent( new YIELD::PlatformExceptionEvent( ETIMEDOUT ) );
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

uint8_t Proxy::reconnect( uint64_t timeout_ms, uint8_t reconnect_tries_left )
{
  if ( peer_ip == 0 )
  {
    peer_ip = YIELD::SocketLib::resolveHost( uri.getHost() );
    if ( peer_ip == 0 && ( strcmp( uri.getHost(), "localhost" ) == 0 || strcmp( uri.getHost(), "127.0.0.1" ) == 0 ) )
      peer_ip = YIELD::SocketLib::resolveHost( YIELD::SocketLib::getLocalHostFQDN() );
    if ( peer_ip == 0 )
      throw new YIELD::PlatformExceptionEvent();
  }

  if ( conn != NULL ) // This is a reconnect, not the first connect
  {
    fd_event_queue.detachSocket( conn->getSocket(), conn );
    conn->close();
    delete conn;
    conn = NULL;
  }

  while ( reconnect_tries_left > 0 )
  {
    reconnect_tries_left--;


    // Create the conn object based on the URI type
    if ( strcmp( uri.getScheme(), "oncrpc" ) == 0 )
      conn = new YIELD::TCPConnection( peer_ip, uri.getPort(), NULL );
    else
      YIELD::DebugBreak();


    // Attach the socket to the fd_event_queue even if we're doing a blocking connect, in case a later read/write is non-blocking
    fd_event_queue.attachSocket( conn->getSocket(), conn, false, false ); // Attach without read or write notifications enabled


    // Now try the actual connect
    if ( timeout_ms == static_cast<uint64_t>( -1 ) ) // Blocking
    {
      YIELD::SocketLib::setBlocking( conn->getSocket() );

      if ( conn->connect() == YIELD::SocketConnection::SCS_READY )
        return reconnect_tries_left;
    }
    else // Non-blocking/timed
    {
      YIELD::SocketLib::setNonBlocking( conn->getSocket() );

      switch ( conn->connect() )
      {
        case YIELD::SocketConnection::SCS_READY: break;
        case YIELD::SocketConnection::SCS_BLOCKED_ON_WRITE:
        {
          fd_event_queue.toggleSocketEvent( conn->getSocket(), conn, false, true ); // Write readiness = the connect() operation is complete
          double start_epoch_time_ms = YIELD::Time::getCurrentEpochTimeMS();
          YIELD::FDEvent* fd_event = static_cast<YIELD::FDEvent*>( fd_event_queue.timed_dequeue( static_cast<YIELD::timeout_ns_t>( timeout_ms ) * NS_IN_MS ) );
          if ( fd_event && fd_event->error_code == 0 &&
               conn->connect() == YIELD::SocketConnection::SCS_READY )
          {
            fd_event_queue.toggleSocketEvent( conn->getSocket(), conn, false, false );
            return reconnect_tries_left;
          }
          else
          {
            timeout_ms -= std::min( static_cast<uint64_t>( YIELD::Time::getCurrentEpochTimeMS() - start_epoch_time_ms ), timeout_ms );
            if ( timeout_ms == 0 ) { reconnect_tries_left = 0; break; }
          }
        }
      }
    }

    // Clear the connection state for the next try
    fd_event_queue.detachSocket( conn->getSocket(), conn );
    conn->close();
    delete conn;
    conn = NULL;
  }

  unsigned long error_code = YIELD::PlatformException::errno_();
  if ( error_code == 0 )
    error_code = ETIMEDOUT;
  throw new YIELD::PlatformExceptionEvent( error_code );
}

void Proxy::throwExceptionEvent( YIELD::ExceptionEvent* exc_ev )
{
  if ( conn )
  {
    fd_event_queue.detachSocket( conn->getSocket(), conn );
    conn->close();
    delete conn;
    conn = NULL;
  }

  throw exc_ev;
}
