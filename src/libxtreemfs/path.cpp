// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the XtreemFS project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the XtreemFS project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


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
