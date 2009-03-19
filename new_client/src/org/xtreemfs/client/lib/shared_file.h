#ifndef ORG_XTREEMFS_CLIENT_SHARED_FILE_H
#define ORG_XTREEMFS_CLIENT_SHARED_FILE_H

#include "org/xtreemfs/client/path.h"
#include "org/xtreemfs/interfaces/mrc_osd_types.h"

#include "yield.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class FileReplica;
      class MRCProxy;
      class OpenFile;
      class OSDProxyFactory;      
      class Volume;


      class SharedFile : public YIELD::SharedObject
      {
      public:
        SharedFile( Volume& parent_volume, const Path& path, const org::xtreemfs::interfaces::XLocSet& xlocs );
        virtual ~SharedFile(); 

        const Path& get_path() const { return path; }
        const org::xtreemfs::interfaces::XLocSet& get_xlocs() const { return xlocs; }
        MRCProxy& get_mrc_proxy() const;
        OSDProxyFactory& get_osd_proxy_factory() const;

        OpenFile& open( const org::xtreemfs::interfaces::FileCredentials& file_credentials );

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
