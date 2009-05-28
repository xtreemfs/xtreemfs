// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ORG_XTREEMFS_CLIENT_PROXY_H_
#define _ORG_XTREEMFS_CLIENT_PROXY_H_

#include "org/xtreemfs/client/proxy_exception_response.h"
#include "org/xtreemfs/client/policy.h"
#include "org/xtreemfs/interfaces/types.h"

#include "yield.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      template <class ProxyType, class InterfaceType>
      class Proxy : public YIELD::ONCRPCClient<InterfaceType>
      {
      public:
        template <class StageGroupType>
        static YIELD::auto_Object<ProxyType> create( const YIELD::URI& absolute_uri,
                                                    YIELD::auto_Object<StageGroupType> stage_group,
                                                    YIELD::auto_Object<YIELD::Log> log = NULL,
                                                    const YIELD::Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT,
                                                    uint8_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT
#ifdef YIELD_HAVE_OPENSSL
                                                    , YIELD::auto_Object<YIELD::SSLContext> ssl_context = NULL
#endif
                                                  )
        {
          return YIELD::ONCRPCClient<InterfaceType>::create<ProxyType>( absolute_uri, stage_group, log, operation_timeout, reconnect_tries_max
#ifdef YIELD_HAVE_OPENSSL
                                                                        , ssl_context
#endif                  
                                                                       );
        }

      protected:
        Proxy( YIELD::auto_Object<YIELD::FDAndInternalEventQueue> fd_event_queue, YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::Socket> _socket );
        virtual ~Proxy();

        void getCurrentUserCredentials( org::xtreemfs::interfaces::UserCredentials& out_user_credentials );
        void getpasswdFromUserCredentials( const std::string& user_id, const std::string& group_id, int& out_uid, int& out_gid );
        void getUserCredentialsFrompasswd( int uid, int gid, org::xtreemfs::interfaces::UserCredentials& out_user_credentials );        

        // YIELD::ONCRPCClient
        virtual YIELD::auto_Object<YIELD::ONCRPCRequest> createProtocolRequest( YIELD::auto_Object<YIELD::Request> body );

      private:
        get_passwd_from_user_credentials_t get_passwd_from_user_credentials;
        YIELD::STLHashMap<YIELD::STLHashMap<std::pair<int, int>*>*> user_credentials_to_passwd_cache;

        get_user_credentials_from_passwd_t get_user_credentials_from_passwd;
        YIELD::STLHashMap<YIELD::STLHashMap<org::xtreemfs::interfaces::UserCredentials*>*> passwd_to_user_credentials_cache;

        std::vector<YIELD::SharedLibrary*> policy_shared_libraries;
      };
    };
  };
};

#endif
