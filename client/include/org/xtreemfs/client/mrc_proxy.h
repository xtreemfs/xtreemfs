#ifndef ORG_XTREEMFS_CLIENT_MRC_PROXY_H
#define ORG_XTREEMFS_CLIENT_MRC_PROXY_H

#include "org/xtreemfs/client/proxy.h"
#include "org/xtreemfs/interfaces/mrc_interface.h"


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class PolicyContainer;


      class MRCProxy : public Proxy
      {
      public:
        MRCProxy( const YIELD::URI& uri );
        virtual ~MRCProxy();

        // EventHandler
        const char* getEventHandlerName() const { return "MRCProxy"; }

        bool access( const std::string& path, uint32_t mode );
        void chmod( const std::string& path, uint32_t mode );
        void chown( const std::string& path, const std::string& userId, const std::string& groupId );
        void create( const std::string& path, uint32_t mode );
        void ftruncate( const org::xtreemfs::interfaces::XCap& write_xcap, org::xtreemfs::interfaces::XCap& truncate_xcap );
        void getattr( const std::string& path, org::xtreemfs::interfaces::stat_& stbuf );
        std::string getxattr( const std::string& path, const std::string& name );
        void link( const std::string& target_path, const std::string& link_path );
        void listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names );
        void mkdir( const std::string& path, uint32_t mode );
        void mkvol( const std::string& volume_name, uint32_t osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint32_t access_control_policy );
        void rmvol( const std::string& volume_name );
        void open( const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& credentials );
        void readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries );
        void removexattr( const std::string& path, const std::string& name );
        void rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& credentials );
        void renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap );
        void rmdir( const std::string& path );
        void setattr( const std::string& path, const org::xtreemfs::interfaces::stat_& stbuf );
        void setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags );
        void statfs( const std::string& volume_name, org::xtreemfs::interfaces::statfs_& statfsbuf );
        void symlink( const std::string& target_path, const std::string& link_path );
        void unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& credentials );
        void update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response );
        void utime( const std::string& path, uint64_t ctime, uint64_t atime, uint64_t mtime );

      protected:
        // Proxy
        YIELD::auto_SharedObject<org::xtreemfs::interfaces::UserCredentials> get_user_credentials() const;

      private:
        org::xtreemfs::interfaces::MRCInterface mrc_interface;
        PolicyContainer* policies;
      };
    };
  };
};

#endif
