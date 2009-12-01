// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _XTREEMFS_PROXY_H_
#define _XTREEMFS_PROXY_H_

#include "xtreemfs/proxy_exception_response.h"
#include "xtreemfs/policy.h"
#include "xtreemfs/interfaces/types.h"

#include "yield.h"


namespace xtreemfs
{
#ifndef _WIN32
  class PolicyContainer;
#endif


  template <class ProxyType, class InterfaceType>
  class Proxy : public YIELD::ipc::ONCRPCClient<InterfaceType>
  {
  public:
    const static uint32_t PROXY_FLAG_TRACE_IO = YIELD::ipc::Client<YIELD::ipc::ONCRPCRequest, YIELD::ipc::ONCRPCResponse>::CLIENT_FLAG_TRACE_IO;
    const static uint32_t PROXY_FLAG_TRACE_OPERATIONS = YIELD::ipc::Client<YIELD::ipc::ONCRPCRequest, YIELD::ipc::ONCRPCResponse>::CLIENT_FLAG_TRACE_OPERATIONS;
    const static uint32_t PROXY_FLAG_TRACE_AUTH = 8;

    // YIELD::concurrency::EventTarget
    virtual void send( YIELD::concurrency::Event& ev );

  protected:
    Proxy
    ( 
      uint16_t concurrency_level,
      uint32_t flags, 
      YIELD::platform::auto_Log log, 
      const YIELD::platform::Time& operation_timeout, 
      YIELD::ipc::auto_SocketAddress peername, 
      uint8_t reconnect_tries_max,
      YIELD::ipc::auto_SocketFactory socket_factory 
    );

    virtual ~Proxy();

    static YIELD::ipc::auto_SocketFactory 
      createSocketFactory
      ( 
        const YIELD::ipc::URI& absolute_uri, 
        YIELD::ipc::auto_SSLContext ssl_context 
      );

    virtual void 
      getCurrentUserCredentials
      ( 
        org::xtreemfs::interfaces::UserCredentials& out_user_credentials 
      );

#ifndef _WIN32    
    void getpasswdFromUserCredentials
    ( 
      const std::string& user_id,
      const std::string& group_id, 
      int& out_uid, 
      int& out_gid 
    );

    bool getUserCredentialsFrompasswd
    ( 
      int uid, 
      int gid, 
      org::xtreemfs::interfaces::UserCredentials& out_user_credentials 
    );
#endif

  private:
    YIELD::platform::auto_Log log;

#ifndef _WIN32
    PolicyContainer* policy_container;

    get_passwd_from_user_credentials_t get_passwd_from_user_credentials;
    std::map<std::string,std::map<std::string,std::pair<int, int>*>*> 
      user_credentials_to_passwd_cache;

    get_user_credentials_from_passwd_t get_user_credentials_from_passwd;
    std::map<int,std::map<int,org::xtreemfs::interfaces::UserCredentials*>*> 
      passwd_to_user_credentials_cache;
#endif

    std::vector<YIELD::platform::SharedLibrary*> policy_shared_libraries;

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
        policy_shared_library->getFunction<PolicyFunctionType>( policy_function_name );
      if ( policy_function != NULL )
      {
        log->getStream( YIELD::platform::Log::LOG_INFO ) << 
          "xtreemfs::Proxy: using " << policy_function_name << 
          " from " << policy_shared_library_path << ".";

        out_policy_function = policy_function;
        return true;
      }
      else
        return false;
    }
  };
};

#endif
