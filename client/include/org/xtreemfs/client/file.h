// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_FILE_H
#define ORG_XTREEMFS_CLIENT_FILE_H

#include "yield.h"
#include "org/xtreemfs/interfaces/mrc_osd_types.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class OSDProxy;
      class Volume;


      class File : public YIELD::File
      {
      public:
        File( Volume& parent_volume, const YIELD::Path& path, const org::xtreemfs::interfaces::FileCredentials& file_credentials );        

        YIELD_PLATFORM_FILE_PROTOTYPES;

      private:
        ~File();

        Volume& parent_volume;
        YIELD::Path path;
        org::xtreemfs::interfaces::FileCredentials file_credentials;

        OSDProxy& get_osd_proxy( uint64_t object_number );
        OSDProxy** osd_proxies;
 
        org::xtreemfs::interfaces::OSDWriteResponse latest_osd_write_response;
        void processOSDWriteResponse( const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );
      };
    };
  };
};

#endif
