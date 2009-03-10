#ifndef ORG_XTREEMFS_CLIENT_SHARED_FILE_H
#define ORG_XTREEMFS_CLIENT_SHARED_FILE_H

#include "file_interface.h"
#include "org/xtreemfs/client/volume.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class Volume;
      class MRCProxy;
      class OSDProxyFactory;
      class FileReplica;
      class OpenFile;


      class SharedFile : public YIELD::SharedObject, public FileInterface
      {
      public:
        SharedFile( Volume& parent_volume, const Path& path, const org::xtreemfs::interfaces::XLocSet& xlocs );
        virtual ~SharedFile(); 

        const Path& get_path() const { return path; }
        const org::xtreemfs::interfaces::XLocSet& get_xlocs() const { return xlocs; }
        MRCProxy& get_mrc_proxy() const { return parent_volume.get_mrc_proxy(); }
        uint64_t get_mrc_proxy_operation_timeout_ms() const { return parent_volume.get_mrc_proxy_operation_timeout_ms(); }
        OSDProxyFactory& get_osd_proxy_factory() const { return parent_volume.get_osd_proxy_factory(); }
        uint64_t get_osd_proxy_operation_timeout_ms() const { return parent_volume.get_osd_proxy_operation_timeout_ms(); }

        OpenFile& open( uint64_t open_flags, const org::xtreemfs::interfaces::FileCredentials& file_credentials );

        ORG_XTREEMFS_CLIENT_FILEINTERFACE_PROTOTYPES;

      private:
        Volume& parent_volume;
        Path path;
        xtreemfs::interfaces::XLocSet xlocs;

        std::vector<FileReplica*> file_replicas;
      };
    };
  };
};

#endif
