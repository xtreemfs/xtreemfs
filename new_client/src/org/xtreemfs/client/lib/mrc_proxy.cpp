#include "org/xtreemfs/client/mrc_proxy.h"
#include "policy_container.h"
using namespace org::xtreemfs::client;


MRCProxy::MRCProxy( const YIELD::URI& uri )
: Proxy( uri, org::xtreemfs::interfaces::MRCInterface::DEFAULT_ONCRPC_PORT, org::xtreemfs::interfaces::MRCInterface::DEFAULT_ONCRPCS_PORT )
{
  policies = new PolicyContainer;
  mrc_interface.registerSerializableFactories( serializable_factories );
}

MRCProxy::~MRCProxy()
{
  delete policies;
}

void MRCProxy::handleEvent( YIELD::Event& ev )
{
  if ( ev.getGeneralType() == YIELD::RTTI::REQUEST  )
  {
    MRCProxyRequest& mrc_proxy_req = static_cast<MRCProxyRequest&>( ev );
    const org::xtreemfs::interfaces::UserCredentials& user_credentials = mrc_proxy_req.get_user_credentials();
    if ( user_credentials.get_user_id().empty() )
      mrc_proxy_req.set_user_credentials( policies->get_user_credentials() );
  }  
  else
    Proxy::handleEvent( ev );
}

bool MRCProxy::access( const std::string& path, uint32_t mode )
{
  return mrc_interface.access( policies->get_user_credentials(), path, mode, this );
}

void MRCProxy::chmod( const std::string& path, uint32_t mode )
{
  mrc_interface.chmod( policies->get_user_credentials(), path, mode, this );
}

void MRCProxy::chown( const std::string& path, const std::string& user_id, const std::string& group_id )
{
  mrc_interface.chown( policies->get_user_credentials(), path, user_id, group_id, this );
}

void MRCProxy::create( const std::string& path, uint32_t mode )
{
  mrc_interface.create( policies->get_user_credentials(), path, mode, this );
}

void MRCProxy::getattr( const std::string& path, org::xtreemfs::interfaces::stat_& stbuf )
{
  mrc_interface.getattr( policies->get_user_credentials(), path, stbuf, this );
}

std::string MRCProxy::getxattr( const std::string& path, const std::string& name )
{
  return mrc_interface.getxattr( policies->get_user_credentials(), path, name, this );
}

void MRCProxy::link( const std::string& target_path, const std::string& link_path )
{
  mrc_interface.link( policies->get_user_credentials(), target_path, link_path, this );
}

void MRCProxy::listxattr( const std::string& path, org::xtreemfs::interfaces::StringSet& names )
{
  mrc_interface.listxattr( policies->get_user_credentials(), path, names, this );
}

void MRCProxy::mkdir( const std::string& path, uint32_t mode )
{
  mrc_interface.mkdir( policies->get_user_credentials(), path, mode, this );
}

void MRCProxy::mkvol( const std::string& volume_name, uint32_t osd_selection_policy, const org::xtreemfs::interfaces::StripingPolicy& default_striping_policy, uint32_t access_control_policy )
{
  mrc_interface.xtreemfs_mkvol( policies->get_user_credentials(), volume_name, osd_selection_policy, default_striping_policy, access_control_policy, this );
}

void MRCProxy::open( const std::string& path, uint32_t flags, uint32_t mode, org::xtreemfs::interfaces::FileCredentials& file_credentials )
{
  mrc_interface.open( policies->get_user_credentials(), path, flags, mode, file_credentials, this );
}

void MRCProxy::readdir( const std::string& path, org::xtreemfs::interfaces::DirectoryEntrySet& directory_entries )
{
  mrc_interface.readdir( policies->get_user_credentials(), path, directory_entries, this );
}

void MRCProxy::removexattr( const std::string& path, const std::string& name )
{
  mrc_interface.removexattr( policies->get_user_credentials(), path, name, this );
}

void MRCProxy::rename( const std::string& source_path, const std::string& target_path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials )
{
  mrc_interface.rename( policies->get_user_credentials(), source_path, target_path, file_credentials, this );
}

void MRCProxy::renew_capability( const org::xtreemfs::interfaces::XCap& old_xcap, org::xtreemfs::interfaces::XCap& renewed_xcap )
{
  mrc_interface.xtreemfs_renew_capability( policies->get_user_credentials(), old_xcap, renewed_xcap, this );
}

void MRCProxy::rmdir( const std::string& path )
{
  mrc_interface.rmdir( policies->get_user_credentials(), path, this );
}

void MRCProxy::rmvol( const std::string& volume_name )
{
  mrc_interface.xtreemfs_rmvol( policies->get_user_credentials(), volume_name, this );
}

void MRCProxy::setattr( const std::string& path, const org::xtreemfs::interfaces::stat_& stbuf )
{
  mrc_interface.setattr( policies->get_user_credentials(), path, stbuf, this );
}

void MRCProxy::setxattr( const std::string& path, const std::string& name, const std::string& value, int32_t flags )
{
  mrc_interface.setxattr( policies->get_user_credentials(), path, name, value, flags, this );
}

void MRCProxy::statfs( const std::string& volume_name, org::xtreemfs::interfaces::statfs_& statfsbuf )
{
  mrc_interface.statfs( policies->get_user_credentials(), volume_name, statfsbuf, this );
}

void MRCProxy::symlink( const std::string& target_path, const std::string& link_path )
{
  mrc_interface.symlink( policies->get_user_credentials(), target_path, link_path, this );
}

void MRCProxy::unlink( const std::string& path, org::xtreemfs::interfaces::FileCredentialsSet& file_credentials )
{
  mrc_interface.unlink( policies->get_user_credentials(), path, file_credentials, this );
}

void MRCProxy::update_file_size( const org::xtreemfs::interfaces::XCap& xcap, const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response )
{
  mrc_interface.xtreemfs_update_file_size( policies->get_user_credentials(), xcap, osd_write_response, this );
}

void MRCProxy::utime( const std::string& path, uint64_t ctime, uint64_t atime, uint64_t mtime )
{
  mrc_interface.utime( policies->get_user_credentials(), path, ctime, atime, mtime, this );
}
