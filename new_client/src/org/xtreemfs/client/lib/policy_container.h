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

        org::xtreemfs::interfaces::UserCredentials get_user_credentials() const;

      private:        
        get_user_credentials_t _get_user_credentials;

        void loadPolicySharedLibraries( const YIELD::Path& policy_shared_libraries_dir_path );
        friend class PolicyContainerreaddirCallback;
        void loadPolicySharedLibrary( const YIELD::Path& policy_shared_library_file_path ); 
        std::vector<YIELD::SharedLibrary*> policy_shared_libraries;
      };
    };
  };
};

#endif

