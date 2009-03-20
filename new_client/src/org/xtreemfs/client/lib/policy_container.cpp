#include "policy_container.h"
using namespace org::xtreemfs::client;

#include "org/xtreemfs/interfaces/exceptions.h"

#ifndef _WIN32
#include "yieldfs.h"
#include <unistd.h>
#include <pwd.h>
#include <grp.h>
#endif


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class PolicyContainerreaddirCallback : public YIELD::Volume::readdirCallback
      {
      public:
        PolicyContainerreaddirCallback( PolicyContainer& policy_container, const YIELD::Path& root_dir_path )
          : policy_container( policy_container ), root_dir_path( root_dir_path )
        { }

        // YIELD::Volume::readdirCallback
        bool operator()( const YIELD::Path& name, const YIELD::Stat& stbuf )
        {
          std::string::size_type dll_pos = name.getHostCharsetPath().find( SHLIBSUFFIX );
          if ( dll_pos != std::string::npos && dll_pos != 0 && name.getHostCharsetPath()[dll_pos-1] == '.' )
            policy_container.loadPolicySharedLibrary( root_dir_path + name );
          return true;
        }

      private:
        PolicyContainer& policy_container;
        YIELD::Path root_dir_path;
      };
    };
  };
};


PolicyContainer::PolicyContainer()
{
  this->_get_user_credentials = NULL;

  loadPolicySharedLibraries( "policies" );
  loadPolicySharedLibraries( "lib" );
  loadPolicySharedLibraries( YIELD::Path() );
}

PolicyContainer::~PolicyContainer()
{
  for ( std::vector<YIELD::SharedLibrary*>::iterator policy_shared_library_i = policy_shared_libraries.begin(); policy_shared_library_i != policy_shared_libraries.end(); policy_shared_library_i++ )
    delete *policy_shared_library_i;
}

void PolicyContainer::loadPolicySharedLibraries( const YIELD::Path& policy_shared_libraries_dir_path )
{
  PolicyContainerreaddirCallback readdir_callback( *this, policy_shared_libraries_dir_path );
  YIELD::Volume().readdir( policy_shared_libraries_dir_path, readdir_callback );
}

void PolicyContainer::loadPolicySharedLibrary( const YIELD::Path& policy_shared_library_file_path )
{
  YIELD::SharedLibrary* policy_shared_library = YIELD::SharedLibrary::open( policy_shared_library_file_path );
  if ( policy_shared_library )
  {
    get_user_credentials_t _get_user_credentials = ( get_user_credentials_t )policy_shared_library->getFunction( "get_user_credentials" );
    if ( _get_user_credentials )
      this->_get_user_credentials = _get_user_credentials;

    policy_shared_libraries.push_back( policy_shared_library );
  }
}

org::xtreemfs::interfaces::UserCredentials PolicyContainer::get_user_credentials() const
{
  int caller_uid, caller_gid;
#ifdef _WIN32
  caller_uid = caller_gid = 0;
#else
  caller_uid = yieldfs::FUSE::geteuid();
  if ( caller_uid < 0 ) caller_uid = ::geteuid();
  caller_gid = yieldfs::FUSE::getegid();
  if ( caller_gid < 0 ) caller_gid = ::getegid();
#endif

  if ( _get_user_credentials )
  {
    char *user_id, **group_ids;
    int _get_user_credentials_ret = _get_user_credentials( caller_uid, caller_gid, &user_id, &group_ids );
    if ( _get_user_credentials_ret >= 0 )
    {
      org::xtreemfs::interfaces::UserCredentials user_credentials;

      user_credentials.set_user_id( user_id );
      free( user_id );

      org::xtreemfs::interfaces::StringSet group_ids_ss;
      for ( int group_id_i = 0; group_ids[group_id_i] != NULL; group_id_i++ )
      {
        char* group_id = group_ids[group_id_i];
        group_ids_ss.push_back( group_id );
        free( group_id );
      }
      user_credentials.set_group_ids( group_ids_ss );
      free( group_ids );

      return user_credentials;
    }
    else
      throw YIELD::PlatformException( _get_user_credentials_ret * -1 );
  }
  else
  {
#ifndef _WIN32
    struct passwd pwd, *pwd_p = &pwd, *temp_pwd_p;
    char pwd_buf[256]; int pwd_buf_len = sizeof( pwd_buf );
    struct group grp, *grp_p = &grp, *temp_grp_p;
    char grp_buf[256]; int grp_buf_len = sizeof( grp_buf );

    if ( getpwuid_r( caller_uid, pwd_p, pwd_buf, pwd_buf_len, &temp_pwd_p ) == 0 &&
         getgrgid_r( caller_gid, grp_p, grp_buf, grp_buf_len, &temp_grp_p ) == 0 )
      return org::xtreemfs::interfaces::UserCredentials( pwd.pw_name, org::xtreemfs::interfaces::StringSet( grp.gr_name ), "" );
#endif

    return org::xtreemfs::interfaces::UserCredentials( "anonymous", org::xtreemfs::interfaces::StringSet( "anonymous" ), "anonymous" );
  }
}
