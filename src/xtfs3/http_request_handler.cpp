#include "http_request_handler.h"
#include "bucket.h"
#include "exceptions.h"
#include "object.h"
#include "xml_marshaller.h"
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
  YIELD::auto_HTTPResponse http_response;

  try
  {
    YIELD::auto_URI http_request_uri = YIELD::URI::parse( http_request.get_uri() );

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
        throw exceptions::InvalidURI();
    }

    if ( bucket == NULL )
    {
      // Bucket name = parts between two slashes
      std::string::size_type next_slash_i = http_request_uri->get_resource().find( '/', 1 );
      if ( next_slash_i != std::string::npos )
      {
        bucket = new Bucket( http_request_uri->get_resource().substr( 1, next_slash_i - 1 ), volume );
        if ( next_slash_i + 1 != http_request_uri->get_resource().size() )
          object = new xtfs3::Object( bucket, http_request_uri->get_resource().substr( next_slash_i + 1 ), volume );
      }
      else
        throw exceptions::InvalidURI();
    }

    switch ( http_request.get_method()[0] )
    {
      case 'G': // GET
      {
        yidl::auto_Buffer http_response_body;
        if ( object != NULL )
          http_response_body = object->get();
        else if ( bucket != NULL )
        {
          std::string prefix = http_request_uri->get_query_value( "prefix" );
          std::string marker = http_request_uri->get_query_value( "marker" );
          std::string max_keys_string = http_request_uri->get_query_value( "max-keys" );
          uint32_t max_keys;
          if ( max_keys_string.empty() )
            max_keys = static_cast<uint32_t>( -1 );
          else
            max_keys = static_cast<uint32_t>( atoi( max_keys_string.c_str() ) );
          std::string delimiter = http_request_uri->get_query_value( "delimiter" );

          http_response_body = bucket->get( prefix, marker, max_keys, delimiter );
        }
        else
          throw exceptions::NotImplemented();

        http_response = new YIELD::HTTPResponse( 200 );
        http_response->set_body( http_response_body );
      }
      break;

      case 'P': // PUT
      {
        if ( object != NULL )
        {
          if ( http_request.get_header( "Expect", NULL ) != NULL )
          {
            http_request.respond( 100 );
            yidl::Object::decRef( http_request );
            return;
          }
          else
            object->put( http_request.get_body() );
        }
        else if ( bucket != NULL )
          bucket->put();
        else
          throw exceptions::MethodNotAllowed();
      }
      break;

      default: throw exceptions::NotImplemented();
    }
  }
  catch ( xtfs3::Exception& exc )
  {
    http_response = new YIELD::HTTPResponse( exc.get_http_status_code() );
    http_response->set_header( "Content-Type", "application/xml" );
    XMLMarshaller xml_marshaller;
    xml_marshaller.writeStruct( "Error", 0, exc );
    http_response->set_body( xml_marshaller.get_buffer() );
  }
  catch ( std::exception& exc )
  {
    http_response = new YIELD::HTTPResponse( 500 );
    http_response->set_header( "Content-Type", "text/plain" );
    http_response->set_body( new yidl::StringBuffer( exc.what() ) );
  }

  if ( http_response == NULL )
    http_response = new YIELD::HTTPResponse( 200 );

  http_response->set_header( "Connection", "close" );
  if ( http_response->get_body() == NULL )
    http_response->set_header( "Content-Length", "0" );
  http_response->set_header( "x-amz-id-2", "id" );
  http_response->set_header( "x-amz-request-id", "request_id" );

  http_request.respond( *http_response.release() );

  yidl::Object::decRef( http_request );
}
