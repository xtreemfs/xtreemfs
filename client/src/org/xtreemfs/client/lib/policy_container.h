#ifndef ORG_XTREEMFS_CLIENT_POLICY_CONTAINER_H
#define ORG_XTREEMFS_CLIENT_POLICY_CONTAINER_H

#include "org/xtreemfs/client/policy.h"
#include "org/xtreemfs/interfaces/types.h"

#include "yield.h"

#include <vector>


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class PolicyContainer
      {
      public:
        PolicyContainer();
        virtual ~PolicyContainer();

        void getCurrentUserCredentials( org::xtreemfs::interfaces::UserCredentials& out_user_credentials ) const;
        void getUserCredentialsFrompasswd( int uid, int gid, org::xtreemfs::interfaces::UserCredentials& out_user_credentials ) const;
        void getpasswdFromUserCredentials( const std::string& user_id, const std::string& group_id, int& out_uid, int& out_gid );

      private:        
        get_user_credentials_from_passwd_t get_user_credentials_from_passwd;
        get_passwd_from_user_credentials_t get_passwd_from_user_credentials;

        void loadPolicySharedLibraries( const YIELD::Path& policy_shared_libraries_dir_path );
        friend class PolicyContainerreaddirCallback;
        void loadPolicySharedLibrary( const YIELD::Path& policy_shared_library_file_path ); 
        std::vector<YIELD::SharedLibrary*> policy_shared_libraries;
      };
    };
  };
};

#endif

