// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_FILE_H
#define ORG_XTREEMFS_CLIENT_FILE_H

#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/client/osd_proxy.h"
#include "org/xtreemfs/interfaces/mrc_osd_types.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class Volume;


      class File : public YIELD::File
      {
      public:
        File& operator=( const File& ) { return *this; }

        YIELD_FILE_PROTOTYPES;

      private:
        friend class Volume;

        File( Volume& parent_volume, YIELD::auto_Object<MRCProxy> mrc_proxy, const YIELD::Path& path, const org::xtreemfs::interfaces::FileCredentials& file_credentials );        
        ~File();

        Volume& parent_volume;
        YIELD::auto_Object<MRCProxy> mrc_proxy;
        YIELD::Path path;
        org::xtreemfs::interfaces::FileCredentials file_credentials;

        YIELD::auto_Object<OSDProxy> get_osd_proxy( uint64_t object_number );
 
        org::xtreemfs::interfaces::OSDWriteResponse latest_osd_write_response;
        void processOSDWriteResponse( const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );
      };
    };
  };
};

#endif
