#include "http_request_handler.h"
#include "bucket.h"
#include "object.h"
using namespace xtfs3;


HTTPRequestHandler::HTTPRequestHandler( const std::string& virtual_host_name, YIELD::auto_Volume volume )
  : virtual_host_name( virtual_host_name ), volume( volume )
{ }

void HTTPRequestHandler::handleEvent( YIELD::Event& ev )
{
  switch ( ev.get_type_id() )
  {
    case YIDL_OBJECT_TYPE_ID( YIELD::HTTPRequest ):
    {
      handleHTTPRequest( static_cast<YIELD::HTTPRequest&>( ev ) );
    }
    break;

    default: handleUnknownEvent( ev );
  }
}

void HTTPRequestHandler::handleHTTPRequest( YIELD::HTTPRequest& http_request )
{  
  YIELD::auto_URI http_request_uri = YIELD::URI::parse( http_request.get_uri() );
  if ( http_request_uri == NULL )
  {
    http_request.respond( 400 );
    yidl::Object::decRef( http_request );
    return;
  }


  auto_Bucket bucket;
  auto_Object object;

  // Get the bucket from the Host header or the URI
  const char* host_header = http_request.get_header( "Host", NULL );
  if ( host_header != NULL )
  {
    const char* virtual_host_name_pos = strstr( host_header, virtual_host_name.c_str() );
    if ( virtual_host_name_pos != NULL )
    {
      if ( virtual_host_name_pos != host_header ) // Bucket name = prefix to virtual host name
        bucket = new Bucket( std::string( host_header, virtual_host_name_pos - host_header ), volume );
    }
    else
    {
      http_request.respond( 404 );
      yidl::Object::decRef( http_request );
      return;
    }
  }

  if ( bucket == NULL )
  {
    // Bucket name = parts between two slashes
    std::string::size_type next_slash_i = http_request_uri->get_resource().find( '/', 1 );
    if ( next_slash_i != std::string::npos )
    {
      bucket = new Bucket( http_request_uri->get_resource().substr( 1, next_slash_i ), volume );
      if ( next_slash_i + 1 != http_request_uri->get_resource().size() )
        object = new xtfs3::Object( http_request_uri->get_resource().substr( next_slash_i + 1 ), volume );
    }
    else
    {
      http_request.respond( 404 );
      yidl::Object::decRef( http_request );
      return;
    }
  }


  try
  {
    switch ( http_request.get_method()[0] )
    {
      case 'G': // GET
      {
        if ( object != NULL )
        {
          yidl::auto_Buffer http_response_body = object->get();
          http_request.respond( 200, http_response_body );
        }
        else if ( bucket != NULL )
        {
          yidl::auto_Buffer http_response_body = bucket->get();
          http_request.respond( 200, http_response_body );
        }
        else
        {
          // List buckets
          DebugBreak();
        }
      }
      break;

      case 'P': // PUT
      {
        if ( object != NULL )
        {
          if ( http_request.get_header( "Expect", NULL ) != NULL )
            http_request.respond( 100 );
          else
          {
            object->put( http_request.get_body() );
            http_request.respond( 200 );
          }
        }
        else if ( bucket != NULL )
        {
          bucket->put();
          http_request.respond( 200 );
        }
        else
          http_request.respond( 400 );
      }
      break;

      default: DebugBreak();
    }
  }
  catch ( std::exception& exc )
  {
    YIELD::HTTPResponse* http_response = new YIELD::HTTPResponse( 500 );
    http_response->set_header( "Content-Type", "text/plain" );
    http_response->set_body( new yidl::StringBuffer( exc.what() ) );
    http_request.respond( *http_response );
  }

  yidl::Object::decRef( http_request );
}
