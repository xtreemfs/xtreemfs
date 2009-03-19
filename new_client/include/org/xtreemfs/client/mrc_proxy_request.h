#ifndef ORG_XTREEMFS_CLIENT_MRC_PROXY_REQUEST_H
#define ORG_XTREEMFS_CLIENT_MRC_PROXY_REQUEST_H

#include "yield.h"

namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
      class UserCredentials;
    }
  };
};


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class MRCProxyRequest : public YIELD::Request
      {
      public:
        virtual void set_user_credentials( const org::xtreemfs::interfaces::UserCredentials& user_credentials ) = 0;
        virtual const org::xtreemfs::interfaces::UserCredentials& get_user_credentials() const = 0;

      protected:
        MRCProxyRequest( uint64_t response_timeout_ms = static_cast<uint64_t>( -1 ) )
          : YIELD::Request( response_timeout_ms )
        { }
      };
    };
  };
};

#endif
