// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ORG_XTREEMFS_CLIENT_VOLUME_H_
#define _ORG_XTREEMFS_CLIENT_VOLUME_H_

#include "org/xtreemfs/client/dir_proxy.h"
#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/client/osd_proxy_mux.h"
#include "org/xtreemfs/client/path.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class OSDProxy;


      class Volume : public YIELD::Volume
      {
      public:
        const static uint32_t VOLUME_FLAG_CACHE_FILES = 1;
        const static uint32_t VOLUME_FLAG_CACHE_METADATA = 2;

        Volume( const YIELD::URI& dir_uri, const std::string& name, uint32_t flags = 0, YIELD::auto_Log log = NULL, YIELD::auto_Object<YIELD::SSLContext> ssl_context = NULL );

        uint32_t get_flags() const { return flags; }
        YIELD::auto_Log get_log() const { return log; }
        YIELD::auto_Object<OSDProxyMux> get_osd_proxy_mux() const { return osd_proxy_mux; }

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( Volume, 0 );

        // YIELD::Volume
        YIELD_VOLUME_PROTOTYPES;
        YIELD::auto_Stat getattr( const Path& path );
        bool listdir( const YIELD::Path& path, listdirCallback& callback ) { return listdir( path, Path(), callback ); }
        bool listdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, listdirCallback& callback );

      private:
        ~Volume() { }

        std::string name;
        uint32_t flags;
        YIELD::auto_Log log;

        YIELD::auto_Object<DIRProxy> dir_proxy;
        YIELD::auto_Object<MRCProxy> mrc_proxy;
        YIELD::auto_Object<OSDProxyMux> osd_proxy_mux;

        void osd_unlink( const org::xtreemfs::interfaces::FileCredentialsSet& );
        void set_errno( const char* operation_name, ProxyExceptionResponse& proxy_exception_response );
        void set_errno( const char* operation_name, std::exception& exc );
      };
    };
  };
};

#endif
