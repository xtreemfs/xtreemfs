// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _CLIENT_SRC_ORG_XTREEMFS_CLIENT_POLICY_CONTAINER_H_
#define _CLIENT_SRC_ORG_XTREEMFS_CLIENT_POLICY_CONTAINER_H_

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
      class PolicyContainer : public YIELD::Object
      {
      public:
        PolicyContainer();

        void getCurrentUserCredentials( org::xtreemfs::interfaces::UserCredentials& out_user_credentials );
        void getpasswdFromUserCredentials( const std::string& user_id, const std::string& group_id, int& out_uid, int& out_gid );
        void getUserCredentialsFrompasswd( int uid, int gid, org::xtreemfs::interfaces::UserCredentials& out_user_credentials );

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( org::xtreemfs::client::PolicyContainer, 2842142710UL );

      private:
        ~PolicyContainer();


        get_passwd_from_user_credentials_t get_passwd_from_user_credentials;
        YIELD::STLHashMap<YIELD::STLHashMap<std::pair<int, int>*>*> user_credentials_to_passwd_cache;

        get_user_credentials_from_passwd_t get_user_credentials_from_passwd;
        YIELD::STLHashMap<YIELD::STLHashMap<org::xtreemfs::interfaces::UserCredentials*>*> passwd_to_user_credentials_cache;


        void loadPolicySharedLibraries( const YIELD::Path& policy_shared_libraries_dir_path );
        friend class PolicyContainerlistdirCallback;
        void loadPolicySharedLibrary( const YIELD::Path& policy_shared_library_file_path );
        std::vector<YIELD::SharedLibrary*> policy_shared_libraries;
      };
    };
  };
};


#endif
