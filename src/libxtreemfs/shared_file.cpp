#include "shared_file.h"
using namespace org::xtreemfs::interfaces;
using namespace xtreemfs;

#include "yieldfs.h"

#include <errno.h>

#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif


#define SHARED_FILE_OPERATION_BEGIN \
  try \
  {

#define SHARED_FILE_OPERATION_END \
  } \
  catch ( ProxyExceptionResponse& proxy_exception_response ) \
  { \
    YIELD::platform::Exception::set_errno \
    ( \
      proxy_exception_response.get_platform_error_code() \
    ); \
    return false; \
  } \
  catch ( std::exception& ) \
  { \
    YIELD::platform::Exception::set_errno( EIO ); \
    return false; \
  }


class SharedFile::ReadBuffer : public yidl::runtime::FixedBuffer
{
public:
  ReadBuffer( void* buf, size_t len )
    : FixedBuffer( len )
  {
    iov.iov_base = buf;
  }
};

class SharedFile::ReadResponse : public OSDInterface::readResponse
{
public:
  ReadResponse( yidl::runtime::auto_Buffer buffer )
    : OSDInterface::readResponse
        ( ObjectData( 0, false, 0, buffer ) )
  { }
};

class SharedFile::ReadRequest : public OSDInterface::readRequest
{
public:
  ReadRequest
  ( 
    const FileCredentials& file_credentials, 
    const std::string& file_id, 
    uint64_t object_number, 
    uint64_t object_version, 
    uint32_t offset, 
    uint32_t length, 
    yidl::runtime::auto_Buffer buffer 
  ) 
    : OSDInterface::readRequest
      ( 
        file_credentials, 
        file_id, 
        object_number, 
        object_version, 
        offset, 
        length 
       ),
    buffer( buffer )
  { }

  YIELD::concurrency::auto_Response createResponse() 
  { 
    return new ReadResponse( buffer ); 
  }

private:
  yidl::runtime::auto_Buffer buffer;
};


class SharedFile::WriteBuffer : public yidl::runtime::FixedBuffer
{
public:
  WriteBuffer( const void* buf, size_t len )
    : FixedBuffer( len )
  {
    iov.iov_base = const_cast<void*>( buf );
    iov.iov_len = len;
  }
};


SharedFile::SharedFile
(
  auto_Volume parent_volume,
  const YIELD::platform::Path& path
) : parent_volume( parent_volume ),
    path( path )
{
  selected_file_replica = 0;
}
      
bool
SharedFile::getlk
( 
  const org::xtreemfs::interfaces::FileCredentials& file_credentials,
  bool exclusive, 
  uint64_t offset, 
  uint64_t length 
)
{
  Lock lock = 
    parent_volume->get_osd_proxy_mux()->xtreemfs_lock_check
    ( 
      file_credentials,
      parent_volume->get_uuid(),
      yieldfs::FUSE::getpid(), 
      file_credentials.get_xcap().get_file_id(), 
      offset, 
      length, 
      exclusive 
    );

  return lock.get_client_pid() != yieldfs::FUSE::getpid();
}

bool SharedFile::getxattr( const std::string& name, std::string& out_value )
{
  return parent_volume->getxattr( path, name, out_value );
}

bool SharedFile::listxattr( std::vector<std::string>& out_names )
{
  return parent_volume->listxattr( path, out_names );
}

ssize_t 
SharedFile::read
( 
  const org::xtreemfs::interfaces::FileCredentials& file_credentials,
  void* rbuf, 
  size_t size, 
  uint64_t offset 
)
{
  std::vector<ReadResponse*> read_responses;
  YIELD::platform::auto_Log log( parent_volume->get_log() );
  ssize_t ret = 0;

  try
  {
#ifdef _DEBUG
    if 
    ( 
      ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
         Volume::VOLUME_FLAG_TRACE_FILE_IO 
    )
      log->getStream( YIELD::platform::Log::LOG_INFO ) << 
        "xtreemfs::SharedFile::read( rbuf, size=" << size << 
        ", offset=" << offset << 
        " )";
#endif

    char *rbuf_start = static_cast<char*>( rbuf ), 
         *rbuf_p = static_cast<char*>( rbuf ), 
         *rbuf_end = static_cast<char*>( rbuf ) + size;
    uint64_t current_file_offset = offset;
    uint32_t stripe_size = file_credentials.get_xlocs().get_replicas()[0]
                            .get_striping_policy().get_stripe_size() * 1024;    

    YIELD::concurrency::auto_ResponseQueue<ReadResponse> 
      read_response_queue
      ( 
        new YIELD::concurrency::ResponseQueue<ReadResponse> 
      );
    size_t expected_read_response_count = 0;

    while ( rbuf_p < rbuf_end )
    {      
      uint64_t object_number = current_file_offset / stripe_size;
      uint32_t object_offset = current_file_offset % stripe_size;
      size_t object_size = static_cast<size_t>( rbuf_end - rbuf_p );
      if ( object_offset + object_size > stripe_size )
        object_size = stripe_size - object_offset;

#ifdef _DEBUG
      if 
      ( 
        ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
        Volume::VOLUME_FLAG_TRACE_FILE_IO 
      )
      {
        log->getStream( YIELD::platform::Log::LOG_INFO ) << 
          "xtreemfs::SharedFile: issuing read # " << 
          ( expected_read_response_count + 1 ) <<
          " for " << object_size << 
          " bytes from object number " << object_number <<
          " in file " << file_credentials.get_xcap().get_file_id() <<
          " (object offset = " << object_offset <<
          ", file offset = " << current_file_offset <<
          ", remaining buffer size = " << 
          static_cast<size_t>( rbuf_end - rbuf_p ) << ").";
      }
#endif

      ReadRequest* read_request = 
        new ReadRequest
        ( 
          file_credentials, 
          file_credentials.get_xcap().get_file_id(), 
          object_number, 
          0, 
          object_offset, 
          static_cast<uint32_t>( object_size ), 
          new ReadBuffer( rbuf_p, object_size ) 
        );
      read_request->set_response_target( read_response_queue->incRef() );
      read_request->set_selected_file_replica( selected_file_replica );

      parent_volume->get_osd_proxy_mux()->send( *read_request );
      expected_read_response_count++;

      rbuf_p += object_size;
      current_file_offset += object_size;
    }

#ifdef _DEBUG
    if 
    ( 
      ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
      Volume::VOLUME_FLAG_TRACE_FILE_IO 
    )
      log->getStream( YIELD::platform::Log::LOG_INFO ) << 
        "xtreemfs::SharedFile: issued " << expected_read_response_count << 
        " parallel reads.";
#endif

    for 
    ( 
      size_t read_response_i = 0; 
      read_response_i < expected_read_response_count; 
      read_response_i++ 
    )
    {
      ReadResponse& read_response = read_response_queue->dequeue();
      // Object::decRef( read_response );
      read_responses.push_back( &read_response );

      yidl::runtime::auto_Buffer data( read_response.get_object_data().get_data() );
      rbuf_p = static_cast<char*>( static_cast<void*>( *data ) );
      uint32_t zero_padding = read_response.get_object_data().get_zero_padding();
      if ( read_response.get_selected_file_replica() != 0 )
        selected_file_replica = read_response.get_selected_file_replica();

#ifdef _DEBUG
      if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
            Volume::VOLUME_FLAG_TRACE_FILE_IO )
      {
        log->getStream( YIELD::platform::Log::LOG_INFO ) << 
          "xtreemfs::SharedFile: read " << data->size() <<
          " bytes from file " << file_credentials.get_xcap().get_file_id() <<            
          " with " << zero_padding << " bytes of zero padding" <<
          ", starting from buffer offset " << 
          static_cast<size_t>( rbuf_p - rbuf_start ) << 
          ", read # " << ( read_response_i + 1 ) << " of " << 
          expected_read_response_count << " parallel reads.";
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
          log->getStream( YIELD::platform::Log::LOG_ERR ) << 
            "xtreemfs::SharedFile: received zero_padding (data size=" << 
            data->size() << ", zero_padding=" << zero_padding << 
            ") larger than available buffer space (" << 
            static_cast<size_t>( rbuf_end - rbuf_p ) << ")";
          YIELD::concurrency::ExceptionResponse::set_errno( EIO );
          ret = -1;
          break;
        }
      }
    }

#ifdef _DEBUG
    if ( static_cast<size_t>( ret ) > size ) 
      DebugBreak();

    if 
    ( 
      ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
       Volume::VOLUME_FLAG_TRACE_FILE_IO 
    )
    {
      log->getStream( YIELD::platform::Log::LOG_INFO ) << 
        "xtreemfs::SharedFile: read " << ret <<
        " bytes from file " << file_credentials.get_xcap().get_file_id() <<
        " in total, returning from read().";           
    }
#endif
  }
  catch ( ProxyExceptionResponse& proxy_exception_response )
  {
#ifdef _DEBUG
    if 
    (   
      ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
       Volume::VOLUME_FLAG_TRACE_FILE_IO 
    )
    {
      log->getStream( YIELD::platform::Log::LOG_INFO ) <<
        "xtreemfs::SharedFile: read threw ProxyExceptionResponse: " <<
        "errno=" << proxy_exception_response.get_platform_error_code() <<
        ", strerror=" << YIELD::platform::Exception::strerror( 
          proxy_exception_response.get_platform_error_code() ) << ".";
    }
#endif

    YIELD::platform::Exception::set_errno
    ( 
      proxy_exception_response.get_platform_error_code() 
    );

    ret = -1;
  }
  catch ( std::exception& exc )
  {
    log->getStream( YIELD::platform::Log::LOG_ERR ) <<
      "xtreemfs::SharedFile::read threw std::exception: " << exc.what() << ".";

    YIELD::platform::Exception::set_errno( EIO );

    ret = -1;
  }

  for 
  ( 
    std::vector<ReadResponse*>::iterator read_response_i = read_responses.begin(); 
    read_response_i != read_responses.end(); 
    read_response_i++ 
  )
    yidl::runtime::Object::decRef( **read_response_i );

  return ret;
}

bool SharedFile::removexattr( const std::string& name )
{
  return parent_volume->removexattr( path, name );
}

bool
SharedFile::setlk
( 
  const org::xtreemfs::interfaces::FileCredentials& file_credentials, 
  bool exclusive, 
  uint64_t offset, 
  uint64_t length 
)
{
  SHARED_FILE_OPERATION_BEGIN;

  Lock lock = 
    parent_volume->get_osd_proxy_mux()->xtreemfs_lock_acquire
    ( 
      file_credentials, 
      parent_volume->get_uuid(), 
      yieldfs::FUSE::getpid(), 
      file_credentials.get_xcap().get_file_id(), 
      offset, length, 
      exclusive 
    );

  locks.push_back( lock );

  return true;

  SHARED_FILE_OPERATION_END;
}

bool
SharedFile::setlkw
( 
  const org::xtreemfs::interfaces::FileCredentials& file_credentials,
  bool exclusive, 
  uint64_t offset, 
  uint64_t length 
)
{
  SHARED_FILE_OPERATION_BEGIN;

  for ( ;; )
  {
    try
    {
       Lock lock = 
          parent_volume->get_osd_proxy_mux()->xtreemfs_lock_acquire
          ( 
            file_credentials, 
            parent_volume->get_uuid(), 
            yieldfs::FUSE::getpid(), 
            file_credentials.get_xcap().get_file_id(), 
            offset, 
            length, 
            exclusive 
          );

      locks.push_back( lock );

      return true;
    }
    catch ( ProxyExceptionResponse& )
    { }
  }

  SHARED_FILE_OPERATION_END;
}

bool SharedFile::setxattr
( 
  const std::string& name, 
  const std::string& value, 
  int flags 
)
{
  return parent_volume->setxattr( path, name, value, flags );
}

YIELD::platform::auto_Stat SharedFile::stat()
{
  //if ( !latest_osd_write_response.get_new_file_size().empty() )
  //{
  //  YIELD::platform::auto_Stat stbuf = parent_volume->stat( path );
  //  stbuf->set_size( latest_osd_write_response.get_new_file_size().get_size() );
  //  return stbuf;
  //}
  //else
    return parent_volume->stat( path );
}

bool 
SharedFile::sync
( 
  const org::xtreemfs::interfaces::FileCredentials& file_credentials 
)
{
  SHARED_FILE_OPERATION_BEGIN;

  if ( !latest_osd_write_response.get_new_file_size().empty() )  
  {
    parent_volume->get_mrc_proxy()->xtreemfs_update_file_size
    ( 
      file_credentials.get_xcap(), 
      latest_osd_write_response 
    );

    latest_osd_write_response.set_new_file_size( NewFileSize() );
  }  

  return true;

  SHARED_FILE_OPERATION_END;
}

bool 
SharedFile::truncate
( 
  org::xtreemfs::interfaces::FileCredentials& file_credentials,
  uint64_t new_size 
)
{
  SHARED_FILE_OPERATION_BEGIN;

  XCap truncate_xcap;
  parent_volume->get_mrc_proxy()->
    ftruncate( file_credentials.get_xcap(), truncate_xcap );
  file_credentials.set_xcap( truncate_xcap );
  OSDWriteResponse osd_write_response;

  parent_volume->get_osd_proxy_mux()->truncate
  ( 
    file_credentials, 
    file_credentials.get_xcap().get_file_id(), 
    new_size, 
    osd_write_response 
  );

  if 
  ( 
    ( parent_volume->get_flags() & Volume::VOLUME_FLAG_CACHE_METADATA ) != 
     Volume::VOLUME_FLAG_CACHE_METADATA 
  )
  {
    parent_volume->get_mrc_proxy()->xtreemfs_update_file_size
    ( 
      file_credentials.get_xcap(), 
      osd_write_response 
    );
  }
  else if ( osd_write_response > latest_osd_write_response )
    latest_osd_write_response = osd_write_response;

  return true;

  SHARED_FILE_OPERATION_END;
}

bool 
SharedFile::unlk
( 
  const org::xtreemfs::interfaces::FileCredentials& file_credentials,
  uint64_t offset, 
  uint64_t length 
)
{
  SHARED_FILE_OPERATION_BEGIN;

  if ( locks.empty() )
    return true;
  else
  {
    parent_volume->get_osd_proxy_mux()->xtreemfs_lock_release
    ( 
      file_credentials, 
      file_credentials.get_xcap().get_file_id(), 
      locks.back() 
    );

    locks.pop_back();

    return true;
  }

  SHARED_FILE_OPERATION_END;
}

ssize_t
SharedFile::write
( 
  const org::xtreemfs::interfaces::FileCredentials& file_credentials,
  const void* wbuf,
  size_t size,
  uint64_t offset
)
{
  YIELD::platform::auto_Log log( parent_volume->get_log() );
  ssize_t ret;
  std::vector<OSDInterface::writeResponse*> write_responses;

#ifdef _DEBUG
  if 
  ( 
    ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
    Volume::VOLUME_FLAG_TRACE_FILE_IO 
  )
    log->getStream( YIELD::platform::Log::LOG_INFO ) << 
      "xtreemfs::SharedFile::write( wbuf, size=" << size << 
      ", offset=" << offset << " )";
#endif

  try
  {
    const char *wbuf_p = static_cast<const char*>( wbuf ), 
               *wbuf_end = static_cast<const char*>( wbuf ) + size;
    uint64_t current_file_offset = offset;
    uint32_t stripe_size = file_credentials.get_xlocs().get_replicas()[0]
                            .get_striping_policy().get_stripe_size() * 1024;

    YIELD::concurrency::auto_ResponseQueue<OSDInterface::writeResponse>
      write_response_queue
      (
        new YIELD::concurrency::ResponseQueue<OSDInterface::writeResponse>
      );
    size_t expected_write_response_count = 0;

    while ( wbuf_p < wbuf_end )
    {
      uint64_t object_number = current_file_offset / stripe_size;
      uint32_t object_offset = current_file_offset % stripe_size;
      uint64_t object_size = static_cast<size_t>( wbuf_end - wbuf_p );
      if ( object_offset + object_size > stripe_size )
        object_size = stripe_size - object_offset;

      ObjectData object_data
      ( 
        0,
        false,
        0,
        new WriteBuffer( wbuf_p, static_cast<uint32_t>( object_size ) )
      );

      OSDInterface::writeRequest* write_request =
        new OSDInterface::writeRequest
        ( 
          file_credentials, 
          file_credentials.get_xcap().get_file_id(), 
          object_number, 
          0, 
          object_offset, 
          0, 
          object_data 
        );
      write_request->set_selected_file_replica( selected_file_replica );

#ifdef _DEBUG
      if 
      ( 
        ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
           Volume::VOLUME_FLAG_TRACE_FILE_IO 
      )
      {
        log->getStream( YIELD::platform::Log::LOG_INFO ) << 
          "xtreemfs::SharedFile: issuing write # " << 
          ( expected_write_response_count + 1 ) <<
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

    for 
    ( 
      size_t write_response_i = 0; 
      write_response_i < expected_write_response_count; 
      write_response_i++ 
    )
    {
      OSDInterface::writeResponse& write_response = 
        write_response_queue->dequeue();

      if ( write_response.get_selected_file_replica() != 0 )
        selected_file_replica = write_response.get_selected_file_replica();

#ifdef _DEBUG
      if 
      ( 
        ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
           Volume::VOLUME_FLAG_TRACE_FILE_IO 
      )
        log->getStream( YIELD::platform::Log::LOG_INFO ) << 
          "xtreemfs::SharedFile::write received response # " << 
          ( write_response_i + 1 ) << " of " << 
          expected_write_response_count << ".";
#endif

      if 
      ( 
        write_response.get_osd_write_response() > latest_osd_write_response 
      )
      {
#ifdef _DEBUG
        if 
        ( 
          ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
              Volume::VOLUME_FLAG_TRACE_FILE_IO 
        )
          log->getStream( YIELD::platform::Log::LOG_INFO ) << 
            "xtreemfs::SharedFile: OSD write response is newer than latest known.";
#endif

        latest_osd_write_response = write_response.get_osd_write_response();
      }

      // yidl::runtime::Object::decRef( write_response );
      write_responses.push_back( &write_response );
    }

    if 
    ( 
      !latest_osd_write_response.get_new_file_size().empty() &&
      ( parent_volume->get_flags() & Volume::VOLUME_FLAG_CACHE_METADATA ) != 
      Volume::VOLUME_FLAG_CACHE_METADATA 
    )
    {
#ifdef _DEBUG
      if 
      ( 
        ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
            Volume::VOLUME_FLAG_TRACE_FILE_IO 
      )
        log->getStream( YIELD::platform::Log::LOG_INFO ) << 
            "xtreemfs::SharedFile: flushing file size updates.";
#endif

      parent_volume->get_mrc_proxy()->xtreemfs_update_file_size
      ( 
        file_credentials.get_xcap(), 
        latest_osd_write_response 
      );

      latest_osd_write_response.set_new_file_size( NewFileSize() );
    }

    ret = static_cast<ssize_t>( current_file_offset - offset );
  }
  catch ( ProxyExceptionResponse& proxy_exception_response )
  {
#ifdef _DEBUG
    if 
    ( 
      ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
      Volume::VOLUME_FLAG_TRACE_FILE_IO 
    )
    {
      log->getStream( YIELD::platform::Log::LOG_INFO ) <<
        "xtreemfs::SharedFile: write threw ProxyExceptionResponse: " <<
        "errno= " << proxy_exception_response.get_platform_error_code() <<
        ", strerror=" << YIELD::platform::Exception::strerror( 
          proxy_exception_response.get_platform_error_code() ) << ".";
    }
#endif

    for 
    ( 
      std::vector<OSDInterface::writeResponse*>::iterator 
        write_response_i = write_responses.begin(); 
      write_response_i != write_responses.end(); 
      write_response_i++ 
    )
      yidl::runtime::Object::decRef( **write_response_i );    

    YIELD::platform::Exception::set_errno
    ( 
      proxy_exception_response.get_platform_error_code() 
    );

    ret = -1;
  }
  catch ( std::exception& exc )
  {
    log->getStream( YIELD::platform::Log::LOG_ERR ) <<
      "xtreemfs::SharedFile: write threw std::exception: " << exc.what() << ".";

    YIELD::platform::Exception::set_errno( EIO );

    ret = -1;
  }

  for 
  ( 
    std::vector<OSDInterface::writeResponse*>::iterator 
      write_response_i = write_responses.begin(); 
    write_response_i != write_responses.end(); 
    write_response_i++ 
   )
    yidl::runtime::Object::decRef( **write_response_i );

  return ret;
}

