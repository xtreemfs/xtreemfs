#include "file_replica.h"
#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/client/osd_proxy.h"
#include "org/xtreemfs/client/osd_proxy_factory.h"
using namespace org::xtreemfs::client;


FileReplica::FileReplica( SharedFile& parent_shared_file, const std::string& osd_uuid, uint64_t osd_uuid_version, const org::xtreemfs::interfaces::StripingPolicy& striping_policy )
  : parent_shared_file( parent_shared_file ), osd_uuid( osd_uuid ), osd_uuid_version( osd_uuid_version), striping_policy( striping_policy )
{
  osd_proxy = NULL;
}

FileReplica::~FileReplica()
{
  delete osd_proxy;
}

YIELD::Stat FileReplica::fgetattr()
{
  return parent_shared_file.fgetattr();
}

OSDProxy& FileReplica::get_osd_proxy()
{
  if ( osd_proxy != NULL )
    return *osd_proxy;
  else
  {
    osd_proxy = &parent_shared_file.get_osd_proxy_factory().createOSDProxy( osd_uuid, osd_uuid_version );
    return *osd_proxy;
  }
}

void FileReplica::ftruncate( uint64_t new_size, const org::xtreemfs::interfaces::FileCredentials& file_credentials )
{
 org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
 get_osd_proxy().truncate( file_credentials.get_xcap().get_file_id(), file_credentials, new_size, osd_write_response, get_osd_proxy_operation_timeout_ms() );
 get_mrc_proxy().xtreemfs_update_file_size( file_credentials.get_xcap(), osd_write_response );
}

size_t FileReplica::read( char* rbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials )
{
  YIELD::SerializableString* data = static_cast<YIELD::SerializableString*>( get_osd_proxy().read( file_credentials.get_xcap().get_file_id(), file_credentials, 0, 0, offset, size, get_osd_proxy_operation_timeout_ms() ).get_data().release() );
  if ( data )
  {
    size_t read_size = data->getSize();
    memcpy( rbuf, data->getString(), read_size );
    YIELD::SharedObject::decRef( data );
    return read_size;
  }
  else
    return 0;
}

size_t FileReplica::write( const char* wbuf, size_t size, off_t offset, const org::xtreemfs::interfaces::FileCredentials& file_credentials )
{
  org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
  get_osd_proxy().write( file_credentials.get_xcap().get_file_id(), file_credentials, 0, 0, offset, 0, org::xtreemfs::interfaces::ObjectData( "", 0, false, new YIELD::STLString( wbuf, size ) ), osd_write_response, get_osd_proxy_operation_timeout_ms() );
  get_mrc_proxy().xtreemfs_update_file_size( file_credentials.get_xcap(), osd_write_response );
  return size;
}
