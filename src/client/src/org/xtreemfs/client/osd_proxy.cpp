// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/osd_proxy.h"
using namespace org::xtreemfs::client;

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "org/xtreemfs/interfaces/osd_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif


OSDProxy::OSDProxy( YIELD::auto_Object<YIELD::Log> log, const YIELD::Time& operation_timeout, YIELD::auto_Object<YIELD::SocketAddress> peer_sockaddr, uint8_t reconnect_tries_max, YIELD::auto_Object<YIELD::SocketFactory> socket_factory )
  : YIELD::ONCRPCClient( new org::xtreemfs::interfaces::OSDInterface, log, operation_timeout, peer_sockaddr, reconnect_tries_max, socket_factory )
{ }

void OSDProxy::read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint32_t length, org::xtreemfs::interfaces::ObjectData& object_data )
{
  static_cast<org::xtreemfs::interfaces::OSDInterface&>( *_interface ).read( file_credentials, file_id, object_number, object_version, offset, length, object_data, this );
}

void OSDProxy::truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t new_file_size, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response )
{
  static_cast<org::xtreemfs::interfaces::OSDInterface&>( *_interface ).truncate( file_credentials, file_id, new_file_size, osd_write_response, this );
}

void OSDProxy::unlink( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id )
{
  static_cast<org::xtreemfs::interfaces::OSDInterface&>( *_interface ).unlink( file_credentials, file_id, this );
}

void OSDProxy::write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const std::string& file_id, uint64_t object_number, uint64_t object_version, uint32_t offset, uint64_t lease_timeout, const org::xtreemfs::interfaces::ObjectData& object_data, org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response )
{
  static_cast<org::xtreemfs::interfaces::OSDInterface&>( *_interface ).write( file_credentials, file_id, object_number, object_version, offset, lease_timeout, object_data, osd_write_response, this );
}
