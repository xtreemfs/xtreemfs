// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/path.h"
using namespace xtreemfs;


Path::Path
( 
  const std::string& volume_name,
  const YIELD::platform::Path& local_path 
)
: volume_name( volume_name ), 
  local_path( local_path ), 
  global_path( volume_name )
{
  if ( !local_path.empty() )
  {
#if PATH_SEPARATOR == '/'
    if ( static_cast<const char*>( local_path )[0] == PATH_SEPARATOR )
      global_path += static_cast<const std::string&>( this->local_path );
    else
    {
      global_path.append( "/", 1 );
//      global_path.append( this->local_path.get_utf8_path() );
      global_path += static_cast<const std::string&>( this->local_path );       
    }
#else
    global_path.append( "/", 1 );

    const std::string& 
      local_path_str = static_cast<const std::string&>( this->local_path );

    if ( local_path_str.size() > 1 )
    {
      if ( local_path_str[0] == PATH_SEPARATOR )
      {
        global_path.append
        ( 
          local_path_str.c_str() + 1, 
          local_path_str.size() - 1 
        );
      }
      else
        global_path.append( local_path_str );

      std::string::size_type next_sep = global_path.find( PATH_SEPARATOR );
      while ( next_sep != std::string::npos )
      {
        global_path[next_sep] = '/';
        next_sep = global_path.find( PATH_SEPARATOR, next_sep );
      }
   }
#endif
  }
  else
    global_path.append( "/", 1 );
}

Path::Path( const std::string& global_path )
: global_path( global_path )
{
  std::string::size_type first_slash = global_path.find( '/' );
  if ( first_slash != std::string::npos )
  {
    volume_name = global_path.substr( 0, first_slash );
#ifdef _WIN32
    std::string temp_local_path = global_path.substr( first_slash + 1 );
    std::string::size_type next_slash = temp_local_path.find( '/' );
    while ( next_slash != std::string::npos )
    {
      temp_local_path[next_slash] = PATH_SEPARATOR;
      next_slash = temp_local_path.find( '/', next_slash );
    }
    local_path = YIELD::platform::Path( temp_local_path );
#else
    // TODO: decode the UTF-8 here? or Path::fromUTF8?
    // YIELD::platform::Path( global_path.substr( first_slash + 1 ), false );
    local_path = 
      YIELD::platform::Path( global_path.substr( first_slash + 1 ) );
#endif
  }
  else
  {
    volume_name = global_path;
    local_path = "/";
    this->global_path.append( "/" );
  }
}
