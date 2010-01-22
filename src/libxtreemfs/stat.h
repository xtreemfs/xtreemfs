// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _LIBXTREEMFS_STAT_H_
#define _LIBXTREEMFS_STAT_H_

#include "yield.h"

namespace org
{
  namespace xtreemfs
  {
    namespace interfaces
    {
      class Stat;
    };
  };
};


namespace xtreemfs
{
  class UserCredentialsCache;


  class Stat : public YIELD::platform::Stat
  {
  private:
    friend class Volume;

    Stat
    ( 
      const org::xtreemfs::interfaces::Stat& stbuf,
      UserCredentialsCache& user_credentials_cache
    );

    uint32_t truncate_epoch;
  };
};

#endif
