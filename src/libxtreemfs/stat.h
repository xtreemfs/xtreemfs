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
      class OSDWriteResponse;
    };
  };
};


namespace xtreemfs
{
  class UserCredentialsCache;


  class Stat : public YIELD::platform::Stat
  {
  public:
    Stat( const YIELD::platform::Stat& stbuf );

    Stat
    (
      const YIELD::platform::Stat& stbuf,
      UserCredentialsCache& user_credentials_cache
    );

    Stat
    ( 
      const org::xtreemfs::interfaces::Stat& stbuf,
      UserCredentialsCache& user_credentials_cache
    );

    Stat
    ( 
      const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response 
    );

    operator org::xtreemfs::interfaces::Stat() const;

  private:
    std::string group_id, user_id;
    uint32_t truncate_epoch;
  };
};

#endif
