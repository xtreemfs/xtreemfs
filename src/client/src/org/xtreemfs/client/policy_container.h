// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _CLIENT_SRC_ORG_XTREEMFS_CLIENT_POLICY_CONTAINER_H_
#define _CLIENT_SRC_ORG_XTREEMFS_CLIENT_POLICY_CONTAINER_H_

#include "yield.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class PolicyContainer
      {
      public:
        ~PolicyContainer();

        void* getPolicyFunction( const char* name );

      private:
        std::vector<YIELD::SharedLibrary*> policy_shared_libraries;
      };
    };
  };
};

#endif
