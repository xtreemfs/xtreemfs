// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "file_replica.h"
#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/client/osd_proxy.h"
#include "org/xtreemfs/client/osd_proxy_factory.h"
using namespace org::xtreemfs::client;


FileReplica::FileReplica( SharedFile& parent_shared_file, const org::xtreemfs::interfaces::StripingPolicy& striping_policy, const std::vector<std::string>& osd_uuids )
  : parent_shared_file( parent_shared_file ), striping_policy( striping_policy ), osd_uuids( osd_uuids )
{
  if ( striping_policy.get_width() != osd_uuids.size() )
    YIELD::DebugBreak();
}

FileReplica::~FileReplica()
{
  for ( std::vector<OSDProxy*>::iterator osd_proxy_i = osd_proxies.begin(); osd_proxy_i != osd_proxies.end(); osd_proxy_i++ )
    YIELD::SharedObject::decRef( *osd_proxy_i );
}

bool FileReplica::read( const org::xtreemfs::interfaces::FileCredentials& file_credentials, void* rbuf, size_t size, uint64_t offset, size_t* out_bytes_read )
{
  char* rbuf_p = static_cast<char*>( rbuf );
  uint64_t file_offset = offset, file_offset_max = offset + size;
  uint32_t stripe_size = striping_policy.get_stripe_size() * 1024;

  while ( file_offset < file_offset_max )
  {
    uint64_t object_number = file_offset / stripe_size;
    uint32_t object_offset = file_offset % stripe_size;
    uint64_t object_size = file_offset_max - file_offset;
    if ( object_offset + object_size > stripe_size )
      object_size = stripe_size - object_offset;

    OSDProxy& osd_proxy = get_osd_proxy( object_number );
    org::xtreemfs::interfaces::ObjectData object_data;
    osd_proxy.read( file_credentials, file_credentials.get_xcap().get_file_id(), object_number, 0, object_offset, static_cast<uint32_t>( object_size ), object_data );

    YIELD::SerializableString* data = static_cast<YIELD::SerializableString*>( object_data.get_data().get() );
    if ( data && !data->empty() )
    {
      memcpy( rbuf_p, data->c_str(), data->size() );
      rbuf_p += data->size();
      file_offset += data->size();
    }

    uint32_t zero_padding = object_data.get_zero_padding();
    if ( zero_padding > 0 )
    {
      if ( zero_padding > size )
        zero_padding = static_cast<uint32_t>( size );
      memset( rbuf_p, 0, zero_padding );
      rbuf_p += zero_padding;
      file_offset += zero_padding;
    }

    if ( data && data->size() < object_size )
      break;
  }

  if ( out_bytes_read )
    *out_bytes_read = file_offset - offset;

  return true;
}

void FileReplica::truncate( const org::xtreemfs::interfaces::FileCredentials& file_credentials, uint64_t new_size )
{
 org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
 get_osd_proxy( 0 ).truncate( file_credentials, file_credentials.get_xcap().get_file_id(), new_size, osd_write_response );
 if ( !osd_write_response.get_new_file_size().empty() )
   get_mrc_proxy().update_file_size( file_credentials.get_xcap(), osd_write_response );
}

bool FileReplica::write( const org::xtreemfs::interfaces::FileCredentials& file_credentials, const void* wbuf, size_t size, uint64_t offset, size_t* out_bytes_written )
{
  const char* wbuf_p = static_cast<const char*>( wbuf );
  uint64_t file_offset = offset, file_offset_max = offset + size;
  uint32_t stripe_size = striping_policy.get_stripe_size() * 1024;
  org::xtreemfs::interfaces::OSDWriteResponse newest_osd_write_response;

  while ( file_offset < file_offset_max )
  {
    uint64_t object_number = file_offset / stripe_size;
    uint32_t object_offset = file_offset % stripe_size;
    uint64_t object_size = file_offset_max - file_offset;
    if ( object_offset + object_size > stripe_size )
      object_size = stripe_size - object_offset;
    org::xtreemfs::interfaces::ObjectData object_data( new YIELD::SerializableString( wbuf_p, static_cast<uint32_t>( object_size ) ), 0, 0, false );

    OSDProxy& osd_proxy = get_osd_proxy( object_number );
    org::xtreemfs::interfaces::OSDWriteResponse temp_osd_write_response;
    osd_proxy.write( file_credentials, file_credentials.get_xcap().get_file_id(), object_number, 0, object_offset, 0, object_data, temp_osd_write_response );

    wbuf_p += object_size;
    file_offset += object_size;

    // Newer OSDWriteResponse = higher truncate epoch or same truncate epoch and higher file size
    if ( !temp_osd_write_response.get_new_file_size().empty() )
    {
      if ( newest_osd_write_response.get_new_file_size().empty() )
        newest_osd_write_response = temp_osd_write_response;
      else if ( temp_osd_write_response.get_new_file_size()[0].get_truncate_epoch() > newest_osd_write_response.get_new_file_size()[0].get_truncate_epoch() )
        newest_osd_write_response = temp_osd_write_response;
      else if ( temp_osd_write_response.get_new_file_size()[0].get_truncate_epoch() == newest_osd_write_response.get_new_file_size()[0].get_truncate_epoch() &&
                temp_osd_write_response.get_new_file_size()[0].get_size_in_bytes() > newest_osd_write_response.get_new_file_size()[0].get_size_in_bytes() )
        newest_osd_write_response = temp_osd_write_response;
    }
  }

  if ( !newest_osd_write_response.get_new_file_size().empty() )
    get_mrc_proxy().update_file_size( file_credentials.get_xcap(), newest_osd_write_response );

  if ( out_bytes_written )
    *out_bytes_written = file_offset - offset;

  return true;
}

OSDProxy& FileReplica::get_osd_proxy( uint64_t object_number )
{
  switch ( striping_policy.get_policy() )
  {
    case org::xtreemfs::interfaces::STRIPING_POLICY_RAID0:
    {
      size_t osd_i = object_number % striping_policy.get_width();
      if ( osd_proxies.size() > osd_i && osd_proxies[osd_i] != NULL )
        return *osd_proxies[osd_i];
      else
      {
        OSDProxy& osd_proxy = parent_shared_file.get_osd_proxy_factory().createOSDProxy( osd_uuids[osd_i] );
        osd_proxies.resize( osd_i+1 );
        osd_proxies[osd_i] = &osd_proxy;
        return osd_proxy;
      }
    }

    default: throw YIELD::NotSupportedException(); break;
  }
}
