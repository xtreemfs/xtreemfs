// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef ORG_XTREEMFS_CLIENT_MRC_PROXY_H
#define ORG_XTREEMFS_CLIENT_MRC_PROXY_H

#include "org/xtreemfs/client/path.h"
#include "org/xtreemfs/client/proxy_exception_response.h"
#include "org/xtreemfs/interfaces/mrc_interface.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class PolicyContainer;


      class MRCProxy : public YIELD::ONCRPCClient
      {
      public:       
        template <class StageGroupType>
        static YIELD::auto_Object<MRCProxy> create( YIELD::auto_Object<StageGroupType> stage_group, const YIELD::SocketAddress& peer_sockaddr, YIELD::auto_Object<YIELD::Log> log = NULL, const YIELD::Time& operation_timeout = OPERATION_TIMEOUT_DEFAULT, uint8_t reconnect_tries_max = RECONNECT_TRIES_MAX_DEFAULT, YIELD::auto_Object<YIELD::SocketFactory> socket_factory = NULL )
        {
          return YIELD::Client::create<MRCProxy, StageGroupType>( stage_group, log, operation_timeout, peer_sockaddr, reconnect_tries_max, socket_factory );
        }

        bool access( const Path& path, uint32_t mode );
        void chmod( const Path& path, uint32_t mode );
        void chown( const Path& path, int uid, int gid );
        void chown( const Path& path, const std::string& user_id, const std::string& group_id );
        void create( const Path& path, uint32_t mode );
        void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap );
        void getattr( const Path& path, org::xtreemfs::interfaces::Stat& stbuf );
        void getxattr( const Path& path, const std::string& name, std::string& value );
        void link( const std::string& target_path, const std::string& link_path );
        void listdir( const Path& path, org::xtreemfs::interfaces::StringSet& names );
        void listxattr( const Path& path, org::xtreemfs::interfaces::StringSet& names );
        void mkdir( const Path& path, uint32_t mode );
        void lsvol( org::xtreemfs::interfaces::VolumeSet& volumes );
        void mkvol( const org::xtreemfs::interfaces::Volume& volume );
        void open( const Path& path, uint32_t flags, uint32_t mode, uint32_t attributes, org::xtreemfs::interfaces::FileCredentials& credentials );
        void readdir( const Path& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries );
        void removexattr( const Path& path, const std::string& name );
        void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& credentials );
        void renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap );
        void replica_list( const std::string& file_id, org::xtreemfs::interfaces::ReplicaSet& replicas );
        void rmdir( const Path& path );
        void rmvol( const std::string& volume_name );
        void setattr( const Path& path, const org::xtreemfs::interfaces::Stat& stbuf );
        void setxattr( const Path& path, const std::string& name, const std::string& value, int32_t flags );
        void statvfs( const std::string& volume_name, org::xtreemfs::interfaces::StatVFS& );
        void symlink( const std::string& target_path, const std::string& link_path );
        void unlink( const Path& path, org::xtreemfs::interfaces::FileCredentialsSet& credentials );
        void update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );
        void utimens( const Path& path, uint64_t atime_ns, uint64_t mtime_ns, uint64_t ctime_ns );

        // YIELD::Object
        YIELD_OBJECT_PROTOTYPES( org::xtreemfs::client::MRCProxy, 722274302UL );

      private:
        friend class YIELD::Client;

        MRCProxy( YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, const YIELD::SocketAddress& peer_sockaddr, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::SocketFactory> socket_factory );
        ~MRCProxy();


        org::xtreemfs::interfaces::MRCInterface mrc_interface;
        PolicyContainer* policies;


        // YIELD::Client
        YIELD::auto_Object<YIELD::Request> createProtocolRequest( YIELD::auto_Object<> body );
      };
    };
  };
};

#endif
