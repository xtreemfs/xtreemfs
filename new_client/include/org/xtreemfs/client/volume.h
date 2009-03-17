#ifndef ORG_XTREEMFS_CLIENT_XTREEMFS_FUSE_H
#define ORG_XTREEMFS_CLIENT_XTREEMFS_FUSE_H

#include "yieldfs/volume_interface.h"

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


      class SharedFileCallbackInterface
      {
      private:
        friend class SharedFile;

        virtual void close( SharedFile& ) = 0;
      };


      class Volume : public yieldfs::VolumeInterface, public SharedFileCallbackInterface
      {
      public:
        Volume( const std::string& name, DIRProxy&, MRCProxy&, OSDProxyFactory& );
        virtual ~Volume() { }

        DIRProxy& get_dir_proxy() const { return dir_proxy; }
        uint64_t get_dir_proxy_operation_timeout_ms() const { return dir_proxy_operation_timeout_ms; }
        MRCProxy& get_mrc_proxy() const { return mrc_proxy; }
        uint64_t get_mrc_proxy_operation_timeout_ms() const { return mrc_proxy_operation_timeout_ms; }
        OSDProxyFactory& get_osd_proxy_factory() const { return osd_proxy_factory; }
        uint64_t get_osd_proxy_operation_timeout_ms() const { return osd_proxy_operation_timeout_ms; }

        YIELDFS_VOLUMEINTERFACE_PROTOTYPES;

        YIELD::Stat getattr( const Path& path );

      private:
        std::string name;
        DIRProxy& dir_proxy; uint64_t dir_proxy_operation_timeout_ms;
        MRCProxy& mrc_proxy; uint64_t mrc_proxy_operation_timeout_ms;
        OSDProxyFactory& osd_proxy_factory; uint64_t osd_proxy_operation_timeout_ms;
        org::xtreemfs::interfaces::Context test_context;

        OpenFile& mrc_and_local_open( const Path& path, uint32_t flags, mode_t mode );
        YIELD::HashMap<SharedFile*> in_use_shared_files;
        OpenFile& local_open( const Path& path, uint64_t open_flags, const org::xtreemfs::interfaces::FileCredentials& file_credentials ); // Works with in_use_shared_files
        void osd_unlink( const org::xtreemfs::interfaces::FileCredentialsSet& );

      private:
        // SharedFileCallbackInterface
        void close( SharedFile& );
      };
    };
  };
};

#endif
