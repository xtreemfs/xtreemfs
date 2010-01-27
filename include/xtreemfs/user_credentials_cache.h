// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_USER_CREDENTIALS_CACHE_H_
#define _XTREEMFS_USER_CREDENTIALS_CACHE_H_

#include "xtreemfs/interfaces/types.h"
#include "xtreemfs/policy.h"

#include "yield.h"


namespace xtreemfs
{
  class UserCredentialsCache : public yidl::runtime::Object
  {
  public:
    UserCredentialsCache();
    ~UserCredentialsCache();

#ifndef _WIN32
    void getpasswdFromUserCredentials
    (
      const std::string& user_id,
      const std::string& group_id,
      uid_t& out_uid,
      gid_t& out_gid
    );

    bool getUserCredentialsFrompasswd
    (
      uid_t uid,
      gid_t gid,
      org::xtreemfs::interfaces::UserCredentials& out_user_credentials
    );
#endif

    // yidl::runtime::Object
    YIDL_RUNTIME_OBJECT_PROTOTYPES( UserCredentialsCache, 0 );

  private:
    // The former PolicyContainer
    std::vector<YIELD::platform::SharedLibrary*> policy_shared_libraries;

    void* getPolicyFunction( const char* name );

/*
    template <typename PolicyFunctionType>
    bool getPolicyFunction
    (
      const YIELD::platform::Path& policy_shared_library_path,
      YIELD::platform::auto_SharedLibrary policy_shared_library,
      const char* policy_function_name,
      PolicyFunctionType& out_policy_function
    )
    {
      PolicyFunctionType policy_function =
        policy_shared_library->
          getFunction<PolicyFunctionType>( policy_function_name );

      if ( policy_function != NULL )
      {
        out_policy_function = policy_function;
        return true;
      }
      else
        return false;
    }
*/

    // The actual credentials caches
#ifndef _WIN32
    get_passwd_from_user_credentials_t get_passwd_from_user_credentials;
    std::map<std::string,std::map<std::string,std::pair<uid_t, gid_t>*>*>
      user_credentials_to_passwd_cache;
    YIELD::platform::Mutex user_credentials_to_passwd_cache_lock;

    get_user_credentials_from_passwd_t get_user_credentials_from_passwd;
    std::map<gid_t,std::map<uid_t,org::xtreemfs::interfaces::UserCredentials*>*>
      passwd_to_user_credentials_cache;
    YIELD::platform::Mutex passwd_to_user_credentials_cache_lock;
#endif
  };

  typedef yidl::runtime::auto_Object<UserCredentialsCache> 
    auto_UserCredentialsCache;
};

#endif
