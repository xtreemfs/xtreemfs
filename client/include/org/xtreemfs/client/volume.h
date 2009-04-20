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
        Volume( const std::string& name, DIRProxy&, MRCProxy&, OSDProxyFactory& );
        virtual ~Volume() { }

        DIRProxy& get_dir_proxy() const { return dir_proxy; }
        MRCProxy& get_mrc_proxy() const { return mrc_proxy; }
        OSDProxyFactory& get_osd_proxy_factory() const { return osd_proxy_factory; }

        YIELD_PLATFORM_VOLUME_PROTOTYPES;
        bool listdir( const YIELD::Path& path, listdirCallback& callback ) { return listdir( path, Path(), callback ); }
        bool listdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, listdirCallback& callback );

        YIELD::auto_Object<YIELD::Stat> getattr( const Path& path );
        
      private:
        std::string name;
        DIRProxy& dir_proxy; uint64_t dir_proxy_operation_timeout_ms;
        MRCProxy& mrc_proxy; uint64_t mrc_proxy_operation_timeout_ms;
        OSDProxyFactory& osd_proxy_factory; uint64_t osd_proxy_operation_timeout_ms;

        YIELD::STLHashMap<SharedFile*> in_use_shared_files;
        void osd_unlink( const org::xtreemfs::interfaces::FileCredentialsSet& );
        void set_errno( YIELD::Exception& );

      private:
        // SharedFileCallbackInterface
        void release( SharedFile& );
      };
    };
  };
};

#endif
