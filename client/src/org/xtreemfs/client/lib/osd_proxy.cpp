#include "org/xtreemfs/client/osd_proxy.h"
using namespace org::xtreemfs::client;


OSDProxy::OSDProxy( const YIELD::URI& uri )
: Proxy( uri, org::xtreemfs::interfaces::OSDInterface::DEFAULT_ONCRPC_PORT, org::xtreemfs::interfaces::OSDInterface::DEFAULT_ONCRPCS_PORT )
{
  osd_interface.registerSerializableFactories( serializable_factories );
}

OSDProxy::~OSDProxy()
{ }

org::xtreemfs::interfaces::ObjectData OSDProxy::read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length )
{
  return osd_interface.read( file_credentials, file_id, object_number, object_version, offset, length, this );
}

void OSDProxy::truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response )
{
  osd_interface.truncate( file_credentials, file_id, new_file_size, osd_write_response, this );
}

void OSDProxy::unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id )
{
  osd_interface.unlink( file_credentials, file_id, this );
}

void OSDProxy::write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response )
{
  osd_interface.write( file_credentials, file_id, object_number, object_version, offset, lease_timeout, object_data, osd_write_response, this );
}
