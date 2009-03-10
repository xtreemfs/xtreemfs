#include "org/xtreemfs/client/proxy.h"
using namespace org::xtreemfs::client;

//#include "org/xtreemfs/interfaces/mrc_interface.h"
#include "org/xtreemfs/interfaces/exceptions.h"
using namespace org::xtreemfs::interfaces;

#include <errno.h>
#ifdef _WIN32
#include "yield/platform/sockets.h"
#define ETIMEDOUT WSAETIMEDOUT
#endif


Proxy::Proxy()
{
  uri = NULL;
  reconnect_tries_max = 0;
  xtreemfs::interfaces::Exceptions().registerSerializableFactories( serializable_factories );
}

void Proxy::init( const YIELD::URI& uri, uint8_t reconnect_tries_max )
{
  this->uri = new YIELD::URI( uri );
  this->reconnect_tries_max = reconnect_tries_max;
  peer_ip = 0; conn = NULL;
}

Proxy::~Proxy()
{
  delete uri;
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
        case YIELD::RTTI::REQUEST: handleRequest( static_cast<YIELD::Request&>( ev ) ); return;
        default: YIELD::DebugBreak(); break;
      }
    }
    break;
  }
}

void Proxy::handleRequest( YIELD::Request& req )
{
  try
  {
    YIELD::ONCRPCRequest oncrpc_req( YIELD::SharedObject::incRef( req ), serializable_factories );
    sendProtocolRequest( oncrpc_req, req.getResponseTimeoutMS() );
    req.respond( static_cast<YIELD::Event&>( YIELD::SharedObject::incRef( *oncrpc_req.getInBody() ) ) );
  }
  catch ( YIELD::ExceptionEvent* exc_ev )
  {
    req.respond( *exc_ev );
  }

  YIELD::SharedObject::decRef( req );
}

uint8_t Proxy::reconnect( uint64_t timeout_ms, uint8_t reconnect_tries_left )
{
  if ( peer_ip == 0 )
  {
    peer_ip = YIELD::SocketLib::resolveHost( uri->getHost() );
    if ( peer_ip == 0 && ( strcmp( uri->getHost(), "localhost" ) == 0 || strcmp( uri->getHost(), "127.0.0.1" ) == 0 ) )
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
    unsigned short peer_port = uri->getPort();
    if ( strcmp( uri->getScheme(), "http" ) == 0 )
    {
      if ( peer_port == 0 ) peer_port = 80;
      conn = new YIELD::TCPConnection( peer_ip, peer_port, NULL );
    }
    else if ( strcmp( uri->getScheme(), "https" ) == 0 )
    {
      if ( peer_port == 0 ) peer_port = 443;
      throw new YIELD::ExceptionEvent( "https not implemented" );
    }
    else if ( strcmp( uri->getScheme(), "oncrpc" ) == 0 )
    {
      if ( peer_port != 0 )
        conn = new YIELD::TCPConnection( peer_ip, peer_port, NULL );
      else
        throw new YIELD::ExceptionEvent( "must specify port in oncrpc:// URI" );
    }
    else
      throw new YIELD::ExceptionEvent( "unknown URI scheme" );


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

void Proxy::sendProtocolRequest( YIELD::ProtocolRequest& protocol_req, uint64_t timeout_ms )
{
  uint64_t original_timeout_ms = timeout_ms;
  uint8_t reconnect_tries_left = reconnect_tries_max;

  if ( conn == NULL )
    reconnect_tries_left = reconnect( original_timeout_ms, reconnect_tries_left );

  if ( timeout_ms == static_cast<uint64_t>( -1 ) ) // Blocking
  {
    YIELD::SocketLib::setBlocking( conn->getSocket() );

    for ( uint8_t serialize_tries = 0; serialize_tries < 2; serialize_tries++ ) // Try, allow for one reconnect, then try again
    {
      protocol_req.serialize( *conn );
      if ( conn->getStatus() == YIELD::SocketConnection::SCS_READY )
      {
        for ( uint8_t deserialize_tries = 0; deserialize_tries < 2; deserialize_tries++ )
        {
          protocol_req.deserialize( *conn );
          if ( conn->getStatus() == YIELD::SocketConnection::SCS_READY )
            break;
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
        protocol_req.serialize( *conn );
      else
        protocol_req.deserialize( *conn );

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
