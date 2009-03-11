#include "org/xtreemfs/client.h"
using namespace org::xtreemfs::client;

#include "yield/arch.h"

#include "SimpleOpt.h"


enum { OPT_DEBUG, OPT_DIR, OPT_MRC, OPT_OSD };

CSimpleOpt::SOption options[] = {
  { OPT_DEBUG, "-d", SO_NONE },
  { OPT_DEBUG, "--debug", SO_NONE },
  { OPT_DIR, "--dir", SO_NONE },
  { OPT_MRC, "--mrc", SO_NONE },
  { OPT_OSD, "--osd", SO_NONE },
  SO_END_OF_OPTIONS
};


class argvInputStream : public YIELD::StructuredInputStream
{
public:
  argvInputStream( CSimpleOpt& args, int next_arg_i ) : args( args ), next_arg_i( next_arg_i )
  { }

  size_t readString( const Declaration& decl, std::string& str )
  {
    const char* value = readValue( decl );
    if ( value )
      str = value;
    return str.size();
  }

  size_t readBuffer( const Declaration& decl, void* buffer, size_t buffer_len )
  {
    const char* value = readValue( decl );
    if ( value )
    {
      buffer_len = std::min( buffer_len, strlen( value ) + 1 );
      memcpy( buffer, value, buffer_len );
      return buffer_len;
    }
    else
      return 0;
  }

  bool readBool( const Declaration& decl )
  {
    const char* value = readValue( decl );
    return value && ( strcmp( value, "true" ) == 0 || strcmp( value, "t" ) == 0 );
  }

  YIELD::Serializable* readSerializable( const Declaration&, YIELD::Serializable* s = NULL )
  {
    if ( s && s->getGeneralType() == YIELD::RTTI::STRING )
      s->deserialize( *this );
    return s;
  }

  int64_t readInt64( const Declaration& decl )
  {
    const char* value = readValue( decl );
    if ( value )
      return atol( value );
    else
      return 0;
  }

  uint64_t readUint64( const Declaration& decl )
  {
    const char* value = readValue( decl );
    if ( value )
      return atol( value );
    else
      return 0;
  }

  double readDouble( const Declaration& decl )
  {
    const char* value = readValue( decl );
    if ( value )
      return atof( value );
    else
      return 0;
  }

private:
  CSimpleOpt& args;
  int next_arg_i;

  const char* readValue( const Declaration& decl )
  {
    if ( next_arg_i < args.FileCount() )
      return args.Files()[next_arg_i++];
    else
      return NULL;
  }
};


int main( int argc, char** argv )
{
  CSimpleOpt args( argc, argv, options );

  int ret = 0;

  // Arguments to be parsed
  std::string rpc_uri;
  YIELD::URI* parsed_rpc_uri = NULL;
  bool debug = false; bool dir = false, mrc = true, osd = false;

  try
  {
    // - options
    while ( args.Next() )
    {
      if ( args.LastError() == SO_SUCCESS )
      {
        switch ( args.OptionId() )
        {
          case OPT_DEBUG: debug = true; break;
          case OPT_DIR: dir = true; break;
          case OPT_MRC: mrc = true; break;
          case OPT_OSD: osd = true; break;
        }
      }
    }

    // rpc_uri after - options
    if ( args.FileCount() >= 1 )
      rpc_uri = args.Files()[0];
    else
      throw YIELD::Exception( "must specify RPC URI (http://host:port/Operation)" );

    parsed_rpc_uri = new YIELD::URI( rpc_uri );
    if ( strlen( parsed_rpc_uri->getResource() ) <= 1 )
      throw YIELD::Exception( "RPC URI must include an operation name" );
  }
  catch ( std::exception& exc )
  {
    std::cerr << "Error parsing command line arguments: " << exc.what() << std::endl;
    delete parsed_rpc_uri;
    return 1;
  }

  if ( debug )
    YIELD::SocketConnection::setTraceSocketIO( true );

  Proxy* proxy = NULL;
  if ( dir ) { proxy = new DIRProxy( *parsed_rpc_uri ); }
  else if ( mrc ) { proxy = new MRCProxy( *parsed_rpc_uri ); }
  else if ( osd ) { proxy = new OSDProxy( *parsed_rpc_uri ); }

  std::string req_type_name( "org::xtreemfs::interfaces::" );
  req_type_name.append( proxy->getEventHandlerName() );
  req_type_name.append( "::" );
  req_type_name.append( parsed_rpc_uri->getResource() + 1 );
  req_type_name.append( "SyncRequest" );

  YIELD::Request* req = static_cast<YIELD::Request*>( proxy->getSerializableFactories().createSerializable( req_type_name.c_str() ) );
  if ( req != NULL )
  {
    try
    {
      argvInputStream in_binding( args, 1 );
      req->deserialize( in_binding );
      req->setResponseTimeoutMS( 2000 );

      YIELD::SharedObject::incRef( *req );
      proxy->send( *req );

      YIELD::Event& resp = req->waitForDefaultResponse( 2000 );
      std::cout << resp.getTypeName() << "( ";
      YIELD::PrettyPrintOutputStream out_binding( std::cout );
      resp.serialize( out_binding );
      std::cout << " )" << std::endl;
      YIELD::SharedObject::decRef( resp );
    }
    catch ( YIELD::ExceptionEvent& exc_ev )
    {
      std::cerr << "Exception: " << exc_ev.getTypeName() << "( ";
      YIELD::PrettyPrintOutputStream out_binding( std::cerr );
      exc_ev.serialize( out_binding );
      std::cerr << " ), error_code = " << exc_ev.get_error_code() << ", what = " << exc_ev.what() << std::endl;
      ret = 1;
    }
    catch ( YIELD::Exception& exc )
    {
      std::cerr << "Exception: error_code = " << exc.get_error_code() << ", what = " << exc.what() << std::endl;
      ret = 1;
    }

    YIELD::SharedObject::decRef( *req );
  }
  else
  {
    std::cerr << "RPC operation " << parsed_rpc_uri->getResource() << " is not valid for the given server" << std::endl;
    ret = 1;
  }

  delete proxy;
  delete parsed_rpc_uri;
  return ret;
};
