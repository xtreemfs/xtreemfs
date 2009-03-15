#include "org/xtreemfs/client.h"
using namespace org::xtreemfs::client;

#include "yield.h"

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

  bool readBool( const Declaration& decl )
  {
    const char* value = readValue( decl );
    return value && ( strcmp( value, "true" ) == 0 || strcmp( value, "t" ) == 0 );
  }

  double readDouble( const Declaration& decl )
  {
    const char* value = readValue( decl );
    if ( value )
      return atof( value );
    else
      return 0;
  }

  int64_t readInt64( const Declaration& decl )
  {
    const char* value = readValue( decl );
    if ( value )
      return atol( value );
    else
      return 0;
  }

  YIELD::Serializable* readSerializable( const Declaration&, YIELD::Serializable* s = NULL )
  {
    if ( s && s->getGeneralType() == YIELD::RTTI::STRING )
      s->deserialize( *this );
    return s;
  }

  void readString( const Declaration& decl, std::string& str )
  {
    const char* value = readValue( decl );
    if ( value )
      str = value;
  }

  uint64_t readUint64( const Declaration& decl )
  {
    const char* value = readValue( decl );
    if ( value )
      return atol( value );
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
  Proxy* proxy = NULL;

  try
  {
    CSimpleOpt args( argc, argv, options );

    // Arguments to be parsed
    bool debug = false; bool dir = false, mrc = true, osd = false;

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

    // rpc_uri_str after - options

    if ( args.FileCount() >= 1 )
    {
      YIELD::URI rpc_uri( args.Files()[0] );
      if ( strlen( rpc_uri.getResource() ) > 1 )
      {
        if ( debug )
          YIELD::SocketConnection::setTraceSocketIO( true );
        
        if ( dir ) { proxy = new DIRProxy( rpc_uri ); }
        else if ( osd ) { proxy = new OSDProxy( rpc_uri ); }
        else { proxy = new MRCProxy( rpc_uri ); }

        std::string req_type_name( "org::xtreemfs::interfaces::" );
        req_type_name.append( proxy->getEventHandlerName() );
        req_type_name.append( "::" );
        req_type_name.append( rpc_uri.getResource() + 1 );
        req_type_name.append( "SyncRequest" );

        YIELD::Request* req = proxy->createRequest( req_type_name.c_str() );
        if ( req != NULL )
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

          YIELD::SharedObject::decRef( *req );  
          delete proxy;

          return 0;
        }
        else
          throw YIELD::Exception( "RPC operation name is not valid for the given server" );
      }
      else
        throw YIELD::Exception( "RPC URI must include an operation name" );
    }
    else
      throw YIELD::Exception( "must specify RPC URI (http://host:port/Operation)" );
  }
  catch ( std::exception& exc )
  {
    std::cerr << "Error on request: " << exc.what() << std::endl;

    delete proxy;

    return 1;
  }
}
