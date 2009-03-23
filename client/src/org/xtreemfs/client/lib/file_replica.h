#ifndef ORG_XTREEMFS_CLIENT_FILE_REPLICA_H
#define ORG_XTREEMFS_CLIENT_FILE_REPLICA_H

#include "shared_file.h"
#include "org/xtreemfs/interfaces/mrc_osd_types.h"


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
        MRCProxy& get_mrc_proxy() const { return parent_shared_file.get_mrc_proxy(); }

        size_t read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, void* rbuf, size_t size, off_t offset );
        void truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, off_t new_size );
        size_t write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const void* wbuf, size_t size, off_t offset );

      private:
        SharedFile& parent_shared_file;
        org::xtreemfs::interfaces::StripingPolicy striping_policy;
        std::vector<std::string> osd_uuids;

        OSDProxy& get_osd_proxy( uint64_t object_number );
        std::vector<OSDProxy*> osd_proxies;
      };
    };
  };
};

#endif
