// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/osd_proxy.h"
#include "org/xtreemfs/interfaces/exceptions.h"
using namespace org::xtreemfs::client;


OSDProxy::OSDProxy( const YIELD::URI& uri, YIELD::SSLContext* ssl_context, YIELD::Log* log )
  : YIELD::ONCRPCProxy( uri, ssl_context, log )
{
  osd_interface.registerObjectFactories( object_factories );
  org::xtreemfs::interfaces::Exceptions().registerObjectFactories( object_factories );
}

void OSDProxy::read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data )
{
  osd_interface.read( file_credentials, file_id, object_number, object_version, offset, length, object_data, this );
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
