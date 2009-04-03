#ifndef ORG_XTREEMFS_CLIENT_OSD_PROXY_H
#define ORG_XTREEMFS_CLIENT_OSD_PROXY_H

#include "org/xtreemfs/client/proxy.h"
#include "org/xtreemfs/interfaces/osd_interface.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class OSDProxy : public Proxy
      {
      public:
        OSDProxy( const YIELD::URI& uri );
        OSDProxy( const YIELD::URI& uri, const YIELD::Path& pkcs12_file_path, const std::string& pkcs12_passphrase );
        virtual ~OSDProxy();

        void read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data );
        void truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );
        void unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id );
        void write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );

        // EventHandler
        const char* getEventHandlerName() const { return "OSDProxy"; }

      private:
        org::xtreemfs::interfaces::OSDInterface osd_interface;
      };
    };
  };
};

#endif
