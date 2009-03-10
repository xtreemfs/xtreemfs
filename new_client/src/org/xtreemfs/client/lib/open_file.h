#ifndef ORG_XTREEMFS_CLIENT_OPEN_FILE_H
#define ORG_XTREEMFS_CLIENT_OPEN_FILE_H

#include "file_replica.h"
#include "yieldfs/file_interface.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class OpenFile : public YIELD::SharedObject, public yieldfs::FileInterface
      {
      public:
        OpenFile( FileReplica& attached_to_file_replica, uint64_t open_flags, const org::xtreemfs::interfaces::FileCredentials& file_credentials );
        virtual ~OpenFile();

        YIELDFS_FILEINTERFACE_PROTOTYPES;

      private:
        FileReplica& attached_to_file_replica;
        uint64_t open_flags;
        org::xtreemfs::interfaces::FileCredentials file_credentials;
      };
    };
  };
};

#endif
