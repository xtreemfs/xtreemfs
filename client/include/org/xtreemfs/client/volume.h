// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_VOLUME_H
#define ORG_XTREEMFS_CLIENT_VOLUME_H

#include "yield.h"

#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/client/path.h"

#include <string>


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class DIRProxy;
      class MRCProxy;
      class OSDProxyFactory;
      class SharedFile;
      class OpenFile;
      class PolicyContainer;


      class SharedFileCallbackInterface
      {
      public:
        virtual ~SharedFileCallbackInterface() { }

      private:
        friend class SharedFile;

        virtual void release( SharedFile& ) = 0;
      };


      class Volume : public YIELD::Volume, public SharedFileCallbackInterface
      {
      public:
        const static uint32_t VOLUME_FLAG_CACHE_FILES = 1;
        const static uint32_t VOLUME_FLAG_CACHE_METADATA = 2;
           

        Volume( const std::string& name, DIRProxy&, MRCProxy&, OSDProxyFactory&, uint32_t flags = 0 );
        virtual ~Volume() { }

        DIRProxy& get_dir_proxy() const { return dir_proxy; }
        uint32_t get_flags() const { return flags; }
        MRCProxy& get_mrc_proxy() const { return mrc_proxy; }
        OSDProxyFactory& get_osd_proxy_factory() const { return osd_proxy_factory; }

        YIELD_PLATFORM_VOLUME_PROTOTYPES;
        bool listdir( const YIELD::Path& path, listdirCallback& callback ) { return listdir( path, Path(), callback ); }
        bool listdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, listdirCallback& callback );

        YIELD::auto_Object<YIELD::Stat> getattr( const Path& path );
        
      private:
        std::string name;
        DIRProxy& dir_proxy;
        MRCProxy& mrc_proxy;
        OSDProxyFactory& osd_proxy_factory;
        uint32_t flags;

        YIELD::STLHashMap<SharedFile*> in_use_shared_files;
        YIELD::Mutex in_use_shared_files_lock;
        void osd_unlink( const org::xtreemfs::interfaces::FileCredentialsSet& );

      private:
        // SharedFileCallbackInterface
        void release( SharedFile& );
      };
    };
  };
};

#endif
