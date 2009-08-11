// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "policy_container.h"
using namespace org::xtreemfs::client;


PolicyContainer::~PolicyContainer()
{
  for ( std::vector<YIELD::SharedLibrary*>::iterator policy_shared_library_i = policy_shared_libraries.begin(); policy_shared_library_i != policy_shared_libraries.end(); policy_shared_library_i++ )
    YIELD::Object::decRef( **policy_shared_library_i );
}

void* PolicyContainer::getPolicyFunction( const char* name )
{
  for ( std::vector<YIELD::SharedLibrary*>::iterator policy_shared_library_i = policy_shared_libraries.begin(); policy_shared_library_i != policy_shared_libraries.end(); policy_shared_library_i++ )
  {
    void* policy_function = ( *policy_shared_library_i )->getFunction( name );
    if ( policy_function != NULL )
      return policy_function;
  }

  std::vector<YIELD::Path> policy_dir_paths;
  policy_dir_paths.push_back( YIELD::Path() );
#ifdef _WIN32
  policy_dir_paths.push_back( "..\\policies\\lib" );
#else
  policy_dir_paths.push_back( "../policies/lib" );
  policy_dir_paths.push_back( "/lib/xtreemfs/policies/" );
#endif

  YIELD::auto_Volume volume = new YIELD::Volume;
  for ( std::vector<YIELD::Path>::iterator policy_dir_path_i = policy_dir_paths.begin(); policy_dir_path_i != policy_dir_paths.end(); policy_dir_path_i++ )
  {
    //if ( log != NULL )
    //  log->getStream( YIELD::Log::LOG_DEBUG ) << "org::xtreemfs::client::Proxy: scanning " << *policy_dir_path_i << " for policy shared libraries.";

    std::vector<YIELD::Path> file_names;
    volume->listdir( *policy_dir_path_i, file_names );

    for ( std::vector<YIELD::Path>::iterator file_name_i = file_names.begin(); file_name_i != file_names.end(); file_name_i++ )
    {
      const std::string& file_name = static_cast<const std::string&>( *file_name_i );
      std::string::size_type dll_pos = file_name.find( SHLIBSUFFIX );
      if ( dll_pos != std::string::npos && dll_pos != 0 && file_name[dll_pos-1] == '.' )
      {
        YIELD::Path policy_shared_library_path = *policy_dir_path_i  + file_name;

        //if ( log != NULL )
        //  log->getStream( YIELD::Log::LOG_DEBUG ) << "org::xtreemfs::client::Proxy: checking " << policy_shared_library_path << " for policy functions.";

        YIELD::auto_Object<YIELD::SharedLibrary> policy_shared_library = YIELD::SharedLibrary::open( policy_shared_library_path );

        if ( policy_shared_library != NULL )
        {
          void* policy_function = policy_shared_library->getFunction( name );
          if ( policy_function != NULL )
            policy_shared_libraries.push_back( policy_shared_library.release() );
          return policy_function;
        }
      }
    }
  }

  return NULL;
}
