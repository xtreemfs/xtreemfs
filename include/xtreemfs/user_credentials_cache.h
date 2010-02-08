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

    void getCurrentUserCredentials
    (
      org::xtreemfs::interfaces::UserCredentials& out_user_credentials
    );

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
