// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _LIBXTREEMFS_POLICY_CONTAINER_H_
#define _LIBXTREEMFS_POLICY_CONTAINER_H_

#include "yield.h"


namespace xtreemfs
{
  class PolicyContainer
  {
  public:
    ~PolicyContainer();

    void* getPolicyFunction( const char* name );

  private:
    std::vector<YIELD::platform::SharedLibrary*> policy_shared_libraries;
  };
};

#endif
