// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "org/xtreemfs/client/file.h"
#include "org/xtreemfs/client/mrc_proxy.h"
#include "org/xtreemfs/client/osd_proxy.h"
#include "org/xtreemfs/client/volume.h"
#include "multi_response_target.h"
using namespace org::xtreemfs::client;

#include <errno.h>

#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#else
#include <errno.h>
#endif
#include "org/xtreemfs/interfaces/osd_interface.h"
#ifdef _WIN32
#pragma warning( pop )
#endif


File::File( YIELD::auto_Object<Volume> parent_volume, YIELD::auto_Object<MRCProxy> mrc_proxy, const YIELD::Path& path, const org::xtreemfs::interfaces::FileCredentials& file_credentials )
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
  try
  {
    if ( !latest_osd_write_response.get_new_file_size().empty() )  
    {
      mrc_proxy->xtreemfs_update_file_size( file_credentials.get_xcap(), latest_osd_write_response );
      latest_osd_write_response.set_new_file_size( org::xtreemfs::interfaces::NewFileSize() );
    }  
    return true;
  }
  catch ( ProxyExceptionResponse& proxy_exception_response )
  {
    YIELD::Exception::set_errno( proxy_exception_response.get_platform_error_code() );
  }
  catch ( std::exception& )
  {
    YIELD::Exception::set_errno( EIO );
  }

  return false;
}

YIELD::auto_Stat File::getattr()
{
  return parent_volume->getattr( path );
}

uint64_t File::get_size()
{
  if ( !latest_osd_write_response.get_new_file_size().empty() )  
    return latest_osd_write_response.get_new_file_size()[0].get_size_in_bytes();
  else
    return YIELD::File::get_size();
}

bool File::getxattr( const std::string& name, std::string& out_value )
{
  return parent_volume->getxattr( path, name, out_value );
}

bool File::listxattr( std::vector<std::string>& out_names )
{
  return parent_volume->listxattr( path, out_names );
}

ssize_t File::read( void* rbuf, size_t size, uint64_t offset )
{
  try
  {
    YIELD::auto_Log log = parent_volume->get_log();
    char *rbuf_start = static_cast<char*>( rbuf ), *rbuf_p = static_cast<char*>( rbuf );
#define RBUF_REMAINING ( size - static_cast<size_t>( rbuf_p - rbuf_start ) )
    uint32_t stripe_size = file_credentials.get_xlocs().get_replicas()[0].get_striping_policy().get_stripe_size() * 1024;

    for ( ;; )
    {      
      uint64_t object_number = offset / stripe_size;
      uint32_t object_offset = offset % stripe_size;
      uint64_t object_size = RBUF_REMAINING;
      if ( object_offset + object_size > stripe_size )
        object_size = stripe_size - object_offset;

#ifdef _DEBUG
      log->getStream( YIELD::Log::LOG_INFO ) << 
        "org::xtreemfs::client::File: reading " << object_size << 
        " bytes from offset " << object_offset <<
        " in object number " << object_number << 
        " of file " << file_credentials.get_xcap().get_file_id() <<
        ", file offset = " << offset <<
        ", remaining buffer size = " << RBUF_REMAINING <<
        ".";
#endif

      org::xtreemfs::interfaces::ObjectData object_data;
      parent_volume->get_osd_proxy_mux()->read( file_credentials, file_credentials.get_xcap().get_file_id(), object_number, 0, object_offset, static_cast<uint32_t>( object_size ), object_data );
      YIELD::auto_Buffer data = object_data.get_data();
      uint32_t zero_padding = object_data.get_zero_padding();

#ifdef _DEBUG
      log->getStream( YIELD::Log::LOG_INFO ) << 
        "org::xtreemfs::client::File: read " << data->size() <<
        " bytes from file " << file_credentials.get_xcap().get_file_id() <<
        " with " << zero_padding << " bytes of zero padding.";
#endif

      if ( !data->empty() )
      {
        if ( data->size() <= RBUF_REMAINING )
        {
          memcpy_s( rbuf_p, RBUF_REMAINING, static_cast<void*>( *data ), data->size() );
          rbuf_p += data->size();
          offset += data->size();
        }
        else
        {
          log->getStream( YIELD::Log::LOG_ERR ) << "org::xtreemfs::client::File: received data (data size=" << data->size() << ", zero_padding=" << zero_padding << ") larger than available buffer space (" << RBUF_REMAINING << ")";
          YIELD::ExceptionResponse::set_errno( EIO );
          return -1;
        }
      }

      if ( zero_padding > 0 )
      {
        if ( zero_padding <= RBUF_REMAINING )
        {
          memset( rbuf_p, 0, zero_padding );
          rbuf_p += zero_padding;
          offset += zero_padding;
        }
        else
        {
          log->getStream( YIELD::Log::LOG_ERR ) << "org::xtreemfs::client::File: received zero_padding (data size=" << data->size() << ", zero_padding=" << zero_padding << ") larger than available buffer space (" << RBUF_REMAINING << ")";
          YIELD::ExceptionResponse::set_errno( EIO );
          return -1;
        }
      }

      if ( data->size() < object_size || RBUF_REMAINING == 0 )
        break;
    }

#ifdef _DEBUG
    if ( static_cast<size_t>( rbuf_p - rbuf_start ) > size ) YIELD::DebugBreak();
#endif
    return static_cast<ssize_t>( rbuf_p - rbuf_start );
  }
  catch ( ProxyExceptionResponse& proxy_exception_response )
  {
    YIELD::Exception::set_errno( proxy_exception_response.get_platform_error_code() );
  }
  catch ( std::exception& )
  {
    YIELD::Exception::set_errno( EIO );
  }

  return -1;
}

bool File::removexattr( const std::string& name )
{
  return parent_volume->removexattr( path, name );
}

bool File::setxattr( const std::string& name, const std::string& value, int flags )
{
  return parent_volume->setxattr( path, name, value, flags );
}

bool File::sync()
{
  flush();
  return true;
}

bool File::truncate( uint64_t new_size )
{
  try
  {
    org::xtreemfs::interfaces::XCap truncate_xcap;
    mrc_proxy->ftruncate( file_credentials.get_xcap(), truncate_xcap );
    file_credentials.set_xcap( truncate_xcap );
    org::xtreemfs::interfaces::OSDWriteResponse osd_write_response;
    parent_volume->get_osd_proxy_mux()->truncate( file_credentials, file_credentials.get_xcap().get_file_id(), new_size, osd_write_response );
    if ( osd_write_response > latest_osd_write_response )
      latest_osd_write_response = osd_write_response;
    if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_CACHE_METADATA ) != Volume::VOLUME_FLAG_CACHE_METADATA )
      flush();
    return true;
  }
  catch ( ProxyExceptionResponse& proxy_exception_response )
  {
    YIELD::Exception::set_errno( proxy_exception_response.get_platform_error_code() );
  }
  catch ( std::exception& )
  {
    YIELD::Exception::set_errno( EIO );
  }

  return false;
}

ssize_t File::write( const void* buffer, size_t buffer_len, uint64_t offset )
{
  try
  {
    YIELD::auto_Object< YIELD::OneSignalEventQueue<> > write_response_queue( new YIELD::OneSignalEventQueue<> );

    const char* wbuf_p = static_cast<const char*>( buffer );
    uint64_t file_offset = offset, file_offset_max = offset + buffer_len;
    uint32_t stripe_size = file_credentials.get_xlocs().get_replicas()[0].get_striping_policy().get_stripe_size() * 1024;
    size_t expected_write_response_count = 0;

    while ( file_offset < file_offset_max )
    {
      uint64_t object_number = file_offset / stripe_size;
      uint32_t object_offset = file_offset % stripe_size;
      uint64_t object_size = file_offset_max - file_offset;
      if ( object_offset + object_size > stripe_size )
        object_size = stripe_size - object_offset;
      org::xtreemfs::interfaces::ObjectData object_data( 0, false, 0, new YIELD::StringLiteralBuffer( wbuf_p, static_cast<uint32_t>( object_size ) ) );
      org::xtreemfs::interfaces::OSDInterface::writeRequest* write_request = new org::xtreemfs::interfaces::OSDInterface::writeRequest( file_credentials, file_credentials.get_xcap().get_file_id(), object_number, 0, object_offset, 0, object_data );

      write_request->set_response_target( write_response_queue->incRef() );
      parent_volume->get_osd_proxy_mux()->send( *write_request );
      expected_write_response_count++;

      wbuf_p += object_size;
      file_offset += object_size;
    }

    for ( size_t write_response_i = 0; write_response_i < expected_write_response_count; write_response_i++ )
    {
      org::xtreemfs::interfaces::OSDInterface::writeResponse& write_response = write_response_queue->dequeue_typed<org::xtreemfs::interfaces::OSDInterface::writeResponse>();
      if ( write_response.get_osd_write_response() > latest_osd_write_response )
        latest_osd_write_response = write_response.get_osd_write_response();
      YIELD::Object::decRef( write_response );
    }

    if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_CACHE_METADATA ) != Volume::VOLUME_FLAG_CACHE_METADATA )
      flush();

    return static_cast<ssize_t>( file_offset - offset );
  }
  catch ( ProxyExceptionResponse& proxy_exception_response )
  {
    YIELD::Exception::set_errno( proxy_exception_response.get_platform_error_code() );
  }
  catch ( std::exception& )
  {
    YIELD::Exception::set_errno( EIO );
  }

  return -1;
}

ssize_t File::writev( const struct iovec* buffers, uint32_t buffers_count, uint64_t offset )
{
  if ( buffers_count == 1 )
    return write( buffers[0].iov_base, buffers[0].iov_len, offset );
  else
  {
#ifdef _WIN32
    ::SetLastError( ERROR_NOT_SUPPORTED );
#else
    errno = ENOTSUP;
#endif
    return -1;
  }
}
