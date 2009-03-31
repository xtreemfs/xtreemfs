#ifndef ORG_XTREEMFS_CLIENT_FILE_REPLICA_H
#define ORG_XTREEMFS_CLIENT_FILE_REPLICA_H

#include "shared_file.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class OSDProxy;
      class OSDProxyFactory;    


      class FileReplica
      {
      public:
        FileReplica( SharedFile& parent_shared_file, const org::xtreemfs::interfaces::StripingPolicy& striping_policy, const std::vector<std::string>& osd_uuids );
        virtual ~FileReplica();

        SharedFile& get_parent_shared_file() const { return parent_shared_file; }
        Volume& get_parent_volume() const { return parent_shared_file.get_parent_volume(); }
        const Path& get_path() const { return parent_shared_file.get_path(); }

        uint64_t read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, void* rbuf, uint64_t size, uint64_t offset );
        void truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, uint64_t new_size );
        uint64_t write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const void* wbuf, uint64_t size, uint64_t offset );

      private:
        SharedFile& parent_shared_file;
        org::xtreemfs::interfaces::StripingPolicy striping_policy;
        std::vector<std::string> osd_uuids;

        MRCProxy& get_mrc_proxy() const { return parent_shared_file.get_mrc_proxy(); }

        OSDProxy& get_osd_proxy( uint64_t object_number );
        std::vector<OSDProxy*> osd_proxies;
      };
    };
  };
};

#endif
