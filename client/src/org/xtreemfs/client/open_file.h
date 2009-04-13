// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ECTS_XTREEMFS_CLIENT_SRC_ORG_XTREEMFS_CLIENT_LIB_OPEN_FILE_H
#define ECTS_XTREEMFS_CLIENT_SRC_ORG_XTREEMFS_CLIENT_LIB_OPEN_FILE_H

#include "file_replica.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class OpenFile : public YIELD::File
      {
      public:
        OpenFile( const org::xtreemfs::interfaces::FileCredentials& file_credentials, FileReplica& attached_to_file_replica );
        virtual ~OpenFile();

        Volume& get_parent_volume() const { return attached_to_file_replica.get_parent_volume(); }
        const Path& get_path() const { return attached_to_file_replica.get_path(); }

        YIELD_PLATFORM_FILE_PROTOTYPES;

      private:
        org::xtreemfs::interfaces::FileCredentials file_credentials;
        FileReplica& attached_to_file_replica;
      };
    };
  };
};

#endif
