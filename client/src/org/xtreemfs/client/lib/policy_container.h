// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ECTS_XTREEMFS_CLIENT_SRC_ORG_XTREEMFS_CLIENT_LIB_POLICY_CONTAINER_H
#define ECTS_XTREEMFS_CLIENT_SRC_ORG_XTREEMFS_CLIENT_LIB_POLICY_CONTAINER_H

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
        void getpasswdFromUserCredentials( const std::string& user_id, const std::string& group_id, int& out_uid, int& out_gid );
        void getUserCredentialsFrompasswd( int uid, int gid, org::xtreemfs::interfaces::UserCredentials& out_user_credentials ) const;

      private:
        get_passwd_from_user_credentials_t get_passwd_from_user_credentials;
        get_user_credentials_from_passwd_t get_user_credentials_from_passwd;

        void loadPolicySharedLibraries( const YIELD::Path& policy_shared_libraries_dir_path );
        friend class PolicyContainerreaddirCallback;
        void loadPolicySharedLibrary( const YIELD::Path& policy_shared_library_file_path );
        std::vector<YIELD::SharedLibrary*> policy_shared_libraries;
      };
    };
  };
};

#endif

