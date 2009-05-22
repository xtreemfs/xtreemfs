// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/file.h"
#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/client/osd_proxy.h"
#include "org/xtreemfs/client/volume.h"
using namespace org::xtreemfs::client;

#ifdef _WIN32
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
#include "org/xtreemfs/interfaces/osd_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif


#define ORG_XTREEMFS_CLIENT_FILE_OPERATION_BEGIN( OperationName ) \
  try

#define ORG_XTREEMFS_CLIENT_FILE_OPERATION_END( OperationName ) \
  catch ( ProxyExceptionResponse& proxy_exception_response ) \
  { \
    YIELD::Exception::set_errno( proxy_exception_response.get_platform_error_code() ); \
  } \


File::File( Volume& parent_volume, YIELD::auto_Object<MRCProxy> mrc_proxy, const YIELD::Path& path, const org::xtreemfs::interfaces::FileCredentials& file_credentials )
: parent_volume( parent_volume ), mrc_proxy( mrc_proxy ), path( path ), file_credentials( file_credentials )
{ }

File::~File()
{
  flush();
}

bool File::datasync()
{
  flush();
  return true;
}

bool File::close()
{
  flush();
  return true;
}

bool File::flush()
{
  ORG_XTREEMFS_CLIENT_FILE_OPERATION_BEGIN( flush )
  {
    if ( !latest_osd_write_response.get_new_file_size().empty() )  
    {
      mrc_proxy->xtreemfs_update_file_size( file_credentials.get_xcap(), latest_osd_write_response );
      latest_osd_write_response.set_new_file_size( org::xtreemfs::interfaces::NewFileSize() );
    }  
    return true;
  }
  ORG_XTREEMFS_CLIENT_FILE_OPERATION_END( flush );
  return false;
}

YIELD::auto_Object<YIELD::Stat> File::getattr()
{
  return parent_volume.getattr( path );
}

YIELD::auto_Object<OSDProxy> File::get_osd_proxy( uint64_t object_number )
{
  const org::xtreemfs::interfaces::StripingPolicy& striping_policy = file_credentials.get_xlocs().get_replicas()[0].get_striping_policy();

  switch ( striping_policy.get_type() )
  {
    case org::xtreemfs::interfaces::STRIPING_POLICY_RAID0:
    {      
      size_t osd_i = object_number % striping_policy.get_width();
      const std::string& osd_uuid = file_credentials.get_xlocs().get_replicas()[0].get_osd_uuids()[osd_i];
      return parent_volume.get_osd_proxy( osd_uuid );
    }

    default: YIELD::DebugBreak(); throw YIELD::Exception(); break;
  }
}

bool File::getxattr( const std::string& name, std::string& out_value )
{
  return parent_volume.getxattr( path, name, out_value );
}

bool File::listxattr( std::vector<std::string>& out_names )
{
  return parent_volume.listxattr( path, out_names );
}

void File::processOSDWriteResponse( const org::xtreemfs::interfaces::OSDWriteResponse& osd_write_response )
{
  // Newer OSDWriteResponse = higher truncate epoch or same truncate epoch and higher file size
  if ( !osd_write_response.get_new_file_size().empty() )
  {
    if ( latest_osd_write_response.get_new_file_size().empty() )
      latest_osd_write_response = osd_write_response;
    else if ( osd_write_response.get_new_file_size()[0].get_truncate_epoch() > latest_osd_write_response.get_new_file_size()[0].get_truncate_epoch() )
      latest_osd_write_response = osd_write_response;
    else if ( osd_write_response.get_new_file_size()[0].get_truncate_epoch() == latest_osd_write_response.get_new_file_size()[0].get_truncate_epoch() &&
              osd_write_response.get_new_file_size()[0].get_size_in_bytes() > latest_osd_write_response.get_new_file_size()[0].get_size_in_bytes() )
   
   latest_osd_write_response = osd_write_response;
  }
}

YIELD::Stream::Status File::read( void* rbuf, size_t size, uint64_t offset, size_t* out_bytes_read )
{
  ORG_XTREEMFS_CLIENT_FILE_OPERATION_BEGIN( read )
  {
    char* rbuf_p = static_cast<char*>( rbuf );
    uint64_t file_offset = offset, file_offset_max = offset + size;
    uint32_t stripe_size = file_credentials.get_xlocs().get_replicas()[0].get_striping_policy().get_stripe_size() * 1024;

    while ( file_offset < file_offset_max )
    {
      uint64_t object_number = file_offset / stripe_size;
      uint32_t object_offset = file_offset % stripe_size;
      uint64_t object_size = file_offset_max - file_offset;
      if ( object_offset + object_size > stripe_size )
        object_size = stripe_size - object_offset;

      org::xtreemfs::interfaces::ObjectData object_data;
      get_osd_proxy( object_number )->read( file_credentials, file_credentials.get_xcap().get_file_id(), object_number, 0, object_offset, static_cast<uint32_t>( object_size ), object_data );

      YIELD::String* data = object_data.get_data().get();
      if ( !data->empty() )
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

      if ( data->size() < object_size )
        break;
    }

    if ( out_bytes_read )
      *out_bytes_read = static_cast<size_t>( file_offset - offset );

    return STREAM_STATUS_OK;
  }
  ORG_XTREEMFS_CLIENT_FILE_OPERATION_END( read );
  return STREAM_STATUS_ERROR;
}

bool File::removexattr( const std::string& name )
{
  return parent_volume.removexattr( path, name );
}

bool File::setxattr( const std::string& name, const std::string& value, int flags )
{
  return parent_volume.setxattr( path, name, value, flags );
}

bool File::sync()
{
  flush();
  return true;
}

bool File::truncate( uint64_t new_size )
{
  ORG_XTREEMFS_CLIENT_FILE_OPERATION_BEGIN( truncate )
  {
    org::xtreemfs::interfaces::XCap truncate_xcap;
    mrc_proxy->ftruncate( file_credentials.get_xcap(), truncate_xcap );
    file_credentials.set_xcap( truncate_xcap );
    org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
    get_osd_proxy( 0 )->truncate( file_credentials, file_credentials.get_xcap().get_file_id(), new_size, osd_write_response );
    processOSDWriteResponse( osd_write_response );
  //  if ( ( get_parent_shared_file().get_parent_volume().get_flags() & Volume::VOLUME_FLAG_CACHE_METADATA ) != Volume::VOLUME_FLAG_CACHE_METADATA )
      flush();
    return true;
  }
  ORG_XTREEMFS_CLIENT_FILE_OPERATION_END( truncate );
  return false;
}

YIELD::Stream::Status File::writev( const struct iovec* buffers, uint32_t buffers_count, uint64_t offset, size_t* out_bytes_written )
{
  ORG_XTREEMFS_CLIENT_FILE_OPERATION_BEGIN( writev )
  {
    if ( buffers_count != 1 ) 
      YIELD::DebugBreak();

    const char* wbuf_p = static_cast<const char*>( buffers[0].iov_base );
    uint64_t file_offset = offset, file_offset_max = offset + buffers[0].iov_len;
    uint32_t stripe_size = file_credentials.get_xlocs().get_replicas()[0].get_striping_policy().get_stripe_size() * 1024;

    while ( file_offset < file_offset_max )
    {
      uint64_t object_number = file_offset / stripe_size;
      uint32_t object_offset = file_offset % stripe_size;
      uint64_t object_size = file_offset_max - file_offset;
      if ( object_offset + object_size > stripe_size )
        object_size = stripe_size - object_offset;
      org::xtreemfs::interfaces::ObjectData object_data( new YIELD::String( wbuf_p, static_cast<uint32_t>( object_size ) ), 0, 0, false );

      org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
      get_osd_proxy( object_number )->write( file_credentials, file_credentials.get_xcap().get_file_id(), object_number, 0, object_offset, 0, object_data, osd_write_response );

      wbuf_p += object_size;
      file_offset += object_size;
      processOSDWriteResponse( osd_write_response );    
    }

  //  if ( ( get_parent_shared_file().get_parent_volume().get_flags() & Volume::VOLUME_FLAG_CACHE_METADATA ) != Volume::VOLUME_FLAG_CACHE_METADATA )
      flush();

    if ( out_bytes_written )
      *out_bytes_written = static_cast<size_t>( file_offset - offset );

    return STREAM_STATUS_OK;
  }
  ORG_XTREEMFS_CLIENT_FILE_OPERATION_END( writev );
  return STREAM_STATUS_ERROR;
}
