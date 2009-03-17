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


      class FileReplica : public FileInterface
      {
      public:
        FileReplica( SharedFile& parent_shared_file, const org::xtreemfs::interfaces::StripingPolicy& striping_policy, const std::vector<std::string>& osd_uuids );
        virtual ~FileReplica();

        SharedFile& get_parent_shared_file() const { return parent_shared_file; }
        MRCProxy& get_mrc_proxy() const { return parent_shared_file.get_mrc_proxy(); }
        uint64_t get_mrc_proxy_operation_timeout_ms() const { return parent_shared_file.get_mrc_proxy_operation_timeout_ms(); }
        uint64_t get_osd_proxy_operation_timeout_ms() const { return parent_shared_file.get_osd_proxy_operation_timeout_ms(); }

        ORG_XTREEMFS_CLIENT_FILEINTERFACE_PROTOTYPES;

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
