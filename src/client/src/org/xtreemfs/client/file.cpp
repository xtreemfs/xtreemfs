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


namespace org
{
  namespace xtreemfs
  {
    namespace client
    {
      class FileReadBuffer : public YIELD::FixedBuffer
      {
      public:
        FileReadBuffer( void* buf, size_t len )
          : FixedBuffer( len )
        {
          iov.iov_base = buf;
        }
      };


      class FileReadONCRPCResponse : public YIELD::ONCRPCResponse
      {
      public:
        FileReadONCRPCResponse( YIELD::auto_Object<YIELD::ONCRPCRequest> oncrpc_request, YIELD::auto_Buffer buffer )
          : YIELD::ONCRPCResponse( oncrpc_request, 
                                   new org::xtreemfs::interfaces::OSDInterface::readResponse(
                                     org::xtreemfs::interfaces::ObjectData( 0, false, 0, buffer ) ) )
        { }

      private:
        ~FileReadONCRPCResponse() { }
      };


      class FileReadONCRPCRequest : public YIELD::ONCRPCRequest
      {
      public:
        FileReadONCRPCRequest( YIELD::auto_Interface osd_interface, YIELD::auto_Struct read_request, YIELD::auto_Buffer buffer )
          : ONCRPCRequest( osd_interface, read_request ), buffer( buffer )
        { }

        YIELD::auto_ONCRPCResponse createProtocolResponse() { return new FileReadONCRPCResponse( this->incRef(), buffer ); }

      private:
        YIELD::auto_Buffer buffer;
      };      


      class FileWriteBuffer : public YIELD::FixedBuffer
      {
      public:
        FileWriteBuffer( const void* buf, size_t len )
          : FixedBuffer( len )
        {
          iov.iov_base = const_cast<void*>( buf );
          iov.iov_len = len;
        }
      };
    };
  }
};


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
  std::vector<FileReadONCRPCResponse*> file_read_oncrpc_responses;
  YIELD::auto_Log log( parent_volume->get_log() );
  ssize_t ret = 0;

  try
  {
#ifdef _DEBUG
    if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == Volume::VOLUME_FLAG_TRACE_FILE_IO )
      log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::File::read( rbuf, size=" << size << ", offset=" << offset << " )";
#endif

    char *rbuf_start = static_cast<char*>( rbuf ), *rbuf_p = static_cast<char*>( rbuf ), *rbuf_end = static_cast<char*>( rbuf ) + size;
    uint64_t current_file_offset = offset;
    uint32_t stripe_size = file_credentials.get_xlocs().get_replicas()[0].get_striping_policy().get_stripe_size() * 1024;    

    YIELD::auto_Object<YIELD::OneSignalEventQueue> read_response_queue( new YIELD::OneSignalEventQueue );
    size_t expected_read_response_count = 0;

    while ( rbuf_p < rbuf_end )
    {      
      uint64_t object_number = current_file_offset / stripe_size;
      uint32_t object_offset = current_file_offset % stripe_size;
      size_t object_size = static_cast<size_t>( rbuf_end - rbuf_p );
      if ( object_offset + object_size > stripe_size )
        object_size = stripe_size - object_offset;

#ifdef _DEBUG
      if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == Volume::VOLUME_FLAG_TRACE_FILE_IO )
      {
        log->getStream( YIELD::Log::LOG_INFO ) << 
          "org::xtreemfs::client::File: issuing read # " << ( expected_read_response_count + 1 ) <<
          " for " << object_size << 
          " bytes from object number " << object_number <<
          " in file " << file_credentials.get_xcap().get_file_id() <<
          " (object offset = " << object_offset <<
          ", file offset = " << current_file_offset <<
          ", remaining buffer size = " << static_cast<size_t>( rbuf_end - rbuf_p ) <<
          ").";
      }
#endif

      org::xtreemfs::interfaces::OSDInterface::readRequest* read_request = new org::xtreemfs::interfaces::OSDInterface::readRequest( file_credentials, file_credentials.get_xcap().get_file_id(), object_number, 0, object_offset, static_cast<uint32_t>( object_size ) );
      FileReadONCRPCRequest* file_read_oncrpc_request = new FileReadONCRPCRequest( parent_volume->get_osd_proxy_mux()->incRef(), read_request, new FileReadBuffer( rbuf_p, object_size ) );
      file_read_oncrpc_request->set_response_target( read_response_queue->incRef() );

      parent_volume->get_osd_proxy_mux()->send( *file_read_oncrpc_request );
      expected_read_response_count++;

      rbuf_p += object_size;
      current_file_offset += object_size;
    }

#ifdef _DEBUG
    if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == Volume::VOLUME_FLAG_TRACE_FILE_IO )
      log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::File: issued " << expected_read_response_count << " parallel reads.";
#endif

    for ( size_t read_response_i = 0; read_response_i < expected_read_response_count; read_response_i++ )
    {
      FileReadONCRPCResponse& file_read_oncrpc_response = read_response_queue->dequeue_typed<FileReadONCRPCResponse>();
      YIELD::auto_Struct body = file_read_oncrpc_response.get_body();
      // Object::decRef( file_read_oncrpc_response );
      file_read_oncrpc_responses.push_back( &file_read_oncrpc_response );

      if ( body->get_tag() == YIELD_OBJECT_TAG( YIELD::ExceptionResponse ) )
          throw static_cast<YIELD::ExceptionResponse&>( *body.release() );
      else if ( body->get_tag() == YIELD_OBJECT_TAG( org::xtreemfs::interfaces::OSDInterface::readResponse ) )
      {
        org::xtreemfs::interfaces::OSDInterface::readResponse& read_response = static_cast<org::xtreemfs::interfaces::OSDInterface::readResponse&>( *body );
        YIELD::auto_Buffer data( read_response.get_object_data().get_data() );
        uint32_t zero_padding = read_response.get_object_data().get_zero_padding();
        rbuf_p = static_cast<char*>( static_cast<void*>( *data ) );

#ifdef _DEBUG
        if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == Volume::VOLUME_FLAG_TRACE_FILE_IO )
        {
          log->getStream( YIELD::Log::LOG_INFO ) << 
            "org::xtreemfs::client::File: read " << data->size() <<
            " bytes from file " << file_credentials.get_xcap().get_file_id() <<            
            " with " << zero_padding << " bytes of zero padding" <<
            ", starting from buffer offset " << static_cast<size_t>( rbuf_p - rbuf_start ) << 
            ", read # " << ( read_response_i + 1 ) << " of " << expected_read_response_count << " parallel reads" <<
            ".";
        }
#endif

        ret += data->size();

        if ( zero_padding > 0 )
        { 
          rbuf_p += data->size();

          if ( rbuf_p + zero_padding <= rbuf_end )
          {
            memset( rbuf_p, 0, zero_padding );
            ret += zero_padding;
          }
          else
          {
            log->getStream( YIELD::Log::LOG_ERR ) << "org::xtreemfs::client::File: received zero_padding (data size=" << data->size() << ", zero_padding=" << zero_padding << ") larger than available buffer space (" << static_cast<size_t>( rbuf_end - rbuf_p ) << ")";
            YIELD::ExceptionResponse::set_errno( EIO );
            ret = -1;
            break;
          }
        }
      }
      else
        DebugBreak();
    }
  }
  catch ( ProxyExceptionResponse& proxy_exception_response )
  {
#ifdef _DEBUG
    if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == Volume::VOLUME_FLAG_TRACE_FILE_IO )
    {
      log->getStream( YIELD::Log::LOG_INFO ) <<
      "org::xtreemfs::client::File: read threw ProxyExceptionResponse " <<
      "(errno= " << proxy_exception_response.get_platform_error_code() <<
      ", strerror=" << YIELD::Exception::strerror( proxy_exception_response.get_platform_error_code() ) << ").";
    }
#endif

    YIELD::Exception::set_errno( proxy_exception_response.get_platform_error_code() );

    ret = -1;
  }
  catch ( std::exception& )
  {
#ifdef _DEBUG
    if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == Volume::VOLUME_FLAG_TRACE_FILE_IO )
    {
      log->getStream( YIELD::Log::LOG_INFO ) <<
      "org::xtreemfs::client::File: read threw std::exception, setting errno to EIO.";
    }
#endif

    YIELD::Exception::set_errno( EIO );

    ret = -1;
  }

  for ( std::vector<FileReadONCRPCResponse*>::iterator file_read_oncrpc_response_i = file_read_oncrpc_responses.begin(); file_read_oncrpc_response_i != file_read_oncrpc_responses.end(); file_read_oncrpc_response_i++ )
    YIELD::Object::decRef( **file_read_oncrpc_response_i );

  return ret;
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

ssize_t File::write( const void* wbuf, size_t size, uint64_t offset )
{
  YIELD::auto_Log log( parent_volume->get_log() );
  ssize_t ret;
  std::vector<org::xtreemfs::interfaces::OSDInterface::writeResponse*> write_responses;

#ifdef _DEBUG
  if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == Volume::VOLUME_FLAG_TRACE_FILE_IO )
    log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::File::write( wbuf, size=" << size << ", offset=" << offset << " )";
#endif


  try
  {
    const char *wbuf_p = static_cast<const char*>( wbuf ), *wbuf_end = static_cast<const char*>( wbuf ) + size;
    uint64_t current_file_offset = offset;
    uint32_t stripe_size = file_credentials.get_xlocs().get_replicas()[0].get_striping_policy().get_stripe_size() * 1024;

    YIELD::auto_Object<YIELD::OneSignalEventQueue> write_response_queue( new YIELD::OneSignalEventQueue );
    size_t expected_write_response_count = 0;

    while ( wbuf_p < wbuf_end )
    {
      uint64_t object_number = current_file_offset / stripe_size;
      uint32_t object_offset = current_file_offset % stripe_size;
      uint64_t object_size = static_cast<size_t>( wbuf_end - wbuf_p );
      if ( object_offset + object_size > stripe_size )
        object_size = stripe_size - object_offset;
      org::xtreemfs::interfaces::ObjectData object_data( 0, false, 0, new FileWriteBuffer( wbuf_p, static_cast<uint32_t>( object_size ) ) );
      org::xtreemfs::interfaces::OSDInterface::writeRequest* write_request = new org::xtreemfs::interfaces::OSDInterface::writeRequest( file_credentials, file_credentials.get_xcap().get_file_id(), object_number, 0, object_offset, 0, object_data );

#ifdef _DEBUG
      if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == Volume::VOLUME_FLAG_TRACE_FILE_IO )
      {
        log->getStream( YIELD::Log::LOG_INFO ) << 
          "org::xtreemfs::client::File: issuing write # " << ( expected_write_response_count + 1 ) <<
          " of " << object_size << 
          " bytes to object number " << object_number <<
          " in file " << file_credentials.get_xcap().get_file_id() <<
          " (object offset = " << object_offset <<
          ", file offset = " << current_file_offset  <<
          ").";
      }
#endif

      write_request->set_response_target( write_response_queue->incRef() );
      parent_volume->get_osd_proxy_mux()->send( *write_request );
      expected_write_response_count++;

      wbuf_p += object_size;
      current_file_offset += object_size;
    }

    for ( size_t write_response_i = 0; write_response_i < expected_write_response_count; write_response_i++ )
    {
      org::xtreemfs::interfaces::OSDInterface::writeResponse& write_response = write_response_queue->dequeue_typed<org::xtreemfs::interfaces::OSDInterface::writeResponse>();

#ifdef _DEBUG
      if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == Volume::VOLUME_FLAG_TRACE_FILE_IO )
        log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::File: write received response # " << ( write_response_i + 1 ) << " of " << expected_write_response_count << ".";
#endif

      if ( write_response.get_osd_write_response() > latest_osd_write_response )
      {
#ifdef _DEBUG
        if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == Volume::VOLUME_FLAG_TRACE_FILE_IO )
          log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::File: OSD write response is newer than latest known.";
#endif

        latest_osd_write_response = write_response.get_osd_write_response();
      }

      // YIELD::Object::decRef( write_response );
      write_responses.push_back( &write_response );
    }

    if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_CACHE_METADATA ) != Volume::VOLUME_FLAG_CACHE_METADATA )
    {
#ifdef _DEBUG
      if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == Volume::VOLUME_FLAG_TRACE_FILE_IO )
        log->getStream( YIELD::Log::LOG_INFO ) << "org::xtreemfs::client::File: flushing file size updates.";
#endif

      flush();
    }

    ret = static_cast<ssize_t>( current_file_offset - offset );
  }
  catch ( ProxyExceptionResponse& proxy_exception_response )
  {
#ifdef _DEBUG
    if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == Volume::VOLUME_FLAG_TRACE_FILE_IO )
    {
      log->getStream( YIELD::Log::LOG_INFO ) <<
      "org::xtreemfs::client::File: write threw ProxyExceptionResponse " <<
      "(errno= " << proxy_exception_response.get_platform_error_code() <<
      ", strerror=" << YIELD::Exception::strerror( proxy_exception_response.get_platform_error_code() ) << ").";
    }
#endif

    for ( std::vector<org::xtreemfs::interfaces::OSDInterface::writeResponse*>::iterator write_response_i = write_responses.begin(); write_response_i != write_responses.end(); write_response_i++ )
      YIELD::Object::decRef( **write_response_i );    

    YIELD::Exception::set_errno( proxy_exception_response.get_platform_error_code() );

    ret = -1;
  }
  catch ( std::exception& )
  {
#ifdef _DEBUG
    if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == Volume::VOLUME_FLAG_TRACE_FILE_IO )
    {
      log->getStream( YIELD::Log::LOG_INFO ) <<
      "org::xtreemfs::client::File: write threw std::exception, setting errno to EIO.";
    }
#endif

    YIELD::Exception::set_errno( EIO );

    ret = -1;
  }

  for ( std::vector<org::xtreemfs::interfaces::OSDInterface::writeResponse*>::iterator write_response_i = write_responses.begin(); write_response_i != write_responses.end(); write_response_i++ )
    YIELD::Object::decRef( **write_response_i );    

  return ret;
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
