#include "org/xtreemfs/client/path.h"
using namespace org::xtreemfs::client;


Path::Path( const std::string& volume_name, const YIELD::Path& local_path )
: volume_name( volume_name ), local_path( local_path ), global_path( volume_name )
{
  if ( !local_path.getHostCharsetPath().empty() )
  {
    if ( DISK_PATH_SEPARATOR == '/' )
    {
      if ( local_path.getHostCharsetPath()[0] == DISK_PATH_SEPARATOR )
        global_path.append( this->local_path.getUTF8Path() );
      else
      {
        global_path.append( "/", 1 );
        global_path.append( this->local_path.getUTF8Path().c_str(), this->local_path.getUTF8Path().size() );
      }
    }
    else
    {
      global_path.append( "/", 1 );
      if ( local_path.getHostCharsetPath()[0] == DISK_PATH_SEPARATOR )
        global_path.append( this->local_path.getUTF8Path().c_str() + 1, this->local_path.getUTF8Path().size() -1  );
      else
        global_path.append( this->local_path.getUTF8Path() );

      std::string::size_type next_sep = global_path.find( DISK_PATH_SEPARATOR );
      while ( next_sep != std::string::npos )
      {
        global_path[next_sep] = '/';
        next_sep = global_path.find( DISK_PATH_SEPARATOR, next_sep );
      }
    }
  }
  else
    global_path.append( "/", 1 );
}

Path::Path( const std::string& global_path )
: global_path( global_path )
{
  std::string::size_type first_slash = global_path.find( '/' );
  if ( first_slash != -1 )
  {
    volume_name = global_path.substr( 0, first_slash );
    if ( DISK_PATH_SEPARATOR == '/' )
      local_path = YIELD::Path( global_path.substr( first_slash + 1 ), false );
    else
    {
      std::string temp_local_path = global_path.substr( first_slash + 1 );
      std::string::size_type next_slash = temp_local_path.find( '/' );
      while ( next_slash != std::string::npos )
      {
        temp_local_path[next_slash] = DISK_PATH_SEPARATOR;
        next_slash = temp_local_path.find( '/', next_slash );
      }
      local_path = YIELD::Path( temp_local_path );
    }
  }
  else
  {
    volume_name = global_path;
    local_path = "/";
    this->global_path.append( "/" );
  }
}
