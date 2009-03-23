#ifndef ORG_XTREEMFS_CLIENT_OPEN_FILE_H
#define ORG_XTREEMFS_CLIENT_OPEN_FILE_H

#include "org/xtreemfs/interfaces/mrc_osd_types.h"
#include "yield.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class FileReplica;


      class OpenFile : public YIELD::File
      {
      public:
        OpenFile( const org::xtreemfs::interfaces::FileCredentials& file_credentials, FileReplica& attached_to_file_replica );
        virtual ~OpenFile();

        // YIELD::File
        virtual YIELD::Stat* getattr();
        virtual ssize_t read( void* buf, size_t nbyte, off_t offset );
        virtual bool truncate( uint64_t offset );
        virtual ssize_t write( const void* buf, size_t nbyte, off_t offset );

      private:
        org::xtreemfs::interfaces::FileCredentials file_credentials;
        FileReplica& attached_to_file_replica;
      };
    };
  };
};

#endif
