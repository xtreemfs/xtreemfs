// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_VOLUME_H
#define ORG_XTREEMFS_CLIENT_VOLUME_H

#include "org/xtreemfs/client/dir_proxy.h"
#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/client/osd_proxy_factory.h"
#include "org/xtreemfs/client/path.h"

#include <string>


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class Volume : public YIELD::Volume
      {
      public:
        const static uint32_t VOLUME_FLAG_CACHE_FILES = 1;
        const static uint32_t VOLUME_FLAG_CACHE_METADATA = 2;
           

        Volume( const std::string& name, YIELD::auto_Object<DIRProxy> dir_proxy, YIELD::auto_Object<MRCProxy> mrc_proxy, YIELD::auto_Object<OSDProxyFactory> osd_proxy_factory, uint32_t flags = 0, YIELD::auto_Object<YIELD::Log> log = NULL );        

        YIELD::auto_Object<DIRProxy> get_dir_proxy() const { return dir_proxy; }
        uint32_t get_flags() const { return flags; }
        YIELD::auto_Object<MRCProxy> get_mrc_proxy() const { return mrc_proxy; }
        YIELD::auto_Object<OSDProxyFactory> get_osd_proxy_factory() const { return osd_proxy_factory; }

        // YIELD::Volume
        YIELD_PLATFORM_VOLUME_PROTOTYPES;
        YIELD::auto_Object<YIELD::Stat> getattr( const Path& path );
        bool listdir( const YIELD::Path& path, listdirCallback& callback ) { return listdir( path, Path(), callback ); }
        bool listdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, listdirCallback& callback );
        
      private:
        friend class File;

        ~Volume() { }

        std::string name;
        YIELD::auto_Object<DIRProxy> dir_proxy;
        YIELD::auto_Object<MRCProxy> mrc_proxy;
        YIELD::auto_Object<OSDProxyFactory> osd_proxy_factory;
        uint32_t flags;
        YIELD::auto_Object<YIELD::Log> log;

        void osd_unlink( const org::xtreemfs::interfaces::FileCredentialsSet& );
        void set_errno( const char* operation_name, ProxyExceptionResponse& proxy_exception_response );
        void set_errno( const char* operation_name, std::exception& exc );
      };
    };
  };
};

#endif
