// Copyright 2009 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "xtreemfs/file.h"
#include "xtreemfs/mrc_proxy.h"
#include "xtreemfs/volume.h"
using namespace org::xtreemfs::interfaces;
using namespace xtreemfs;
#include <typeinfo>
#include <iostream>
using namespace std;

#include "yieldfs.h"

#include <errno.h>

#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#else
#include <errno.h>
#endif


#define FILE_OPERATION_BEGIN \
  try \
  {

#define FILE_OPERATION_END \
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


class File::ReadBuffer : public yidl::runtime::FixedBuffer
{
public:
  ReadBuffer( void* buf, size_t len )
    : FixedBuffer( len )
  {
    iov.iov_base = buf;
  }
};

class File::ReadResponse : public OSDInterface::readResponse
{
public:
  ReadResponse( yidl::runtime::auto_Buffer buffer )
    : OSDInterface::readResponse
        ( ObjectData( 0, false, 0, buffer ) )
  { }
};

class File::ReadRequest : public OSDInterface::readRequest
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


class File::WriteBuffer : public yidl::runtime::FixedBuffer
{
public:
  WriteBuffer( const void* buf, size_t len )
    : FixedBuffer( len )
  {
    iov.iov_base = const_cast<void*>( buf );
    iov.iov_len = len;
  }
};


class File::XCapTimer : public YIELD::platform::TimerQueue::Timer
{
public:
  XCapTimer( auto_File file, const YIELD::platform::Time& timeout )
    : YIELD::platform::TimerQueue::Timer( timeout ),
      file( file )
  { }

  XCapTimer& operator=( XCapTimer& )
  {
    return *this;
  }

  // YIELD::platform::TimerQueue::Timer
  void fire()
  {
    if ( !file->closed ) // See note in File::File re: the rationale for this
    {
      try
      {
        XCap renewed_xcap;

        file->parent_volume->get_log()->
          getStream( YIELD::platform::Log::LOG_INFO ) << 
          "xtreemfs::File: renewing XCap for file " << 
          file->file_credentials.get_xcap().get_file_id() << ".";

        file->parent_volume->get_mrc_proxy()->xtreemfs_renew_capability
        ( 
          file->file_credentials.get_xcap(),
          renewed_xcap      
        );

        file->parent_volume->get_log()->
          getStream( YIELD::platform::Log::LOG_INFO ) << 
          "xtreemfs::File: successfully renewed XCap for file " <<
          file->file_credentials.get_xcap().get_file_id() << ".";

        file->file_credentials.set_xcap( renewed_xcap );

        if 
        ( 
          renewed_xcap.get_expire_timeout_s() > 
          XCAP_EXPIRE_TIMEOUT_S_MIN 
        )
        {
          // Add another timer for the renewed xcap
          // Don't use periods here on the pessimistic assumption that
          // most xcaps will never be renewed
          YIELD::platform::TimerQueue::getDefaultTimerQueue().addTimer
          (
            new XCapTimer
            (
              file,
              ( renewed_xcap.get_expire_timeout_s() - 
                XCAP_EXPIRE_TIMEOUT_S_MIN ) 
              * NS_IN_S
            )
          );
        }
        else 
          file->parent_volume->get_log()->
            getStream( YIELD::platform::Log::LOG_ERR ) <<
              "xtreemfs::File: received xcap for file " << 
              renewed_xcap.get_file_id() <<
              "that expires in less than " << 
              XCAP_EXPIRE_TIMEOUT_S_MIN << 
              " seconds, will not try to renew.";
      }
      catch ( std::exception& exc )
      {
        file->parent_volume->get_log()->
          getStream( YIELD::platform::Log::LOG_ERR ) << 
          "xtreemfs::File: caught exception trying to renew XCap for file " <<
          file->file_credentials.get_xcap().get_file_id() << 
          ": " << exc.what() << ".";
      }
    }
  }

private:
  auto_File file;
};


File::File
( 
  auto_Volume parent_volume, 
  const YIELD::platform::Path& path, 
  const FileCredentials& file_credentials 
)
: parent_volume( parent_volume ), 
  path( path ), 
  file_credentials( file_credentials )
{
  closed = false;

  selected_file_replica = 0;

  if 
  (
    file_credentials.get_xcap().get_expire_timeout_s() >
    XCAP_EXPIRE_TIMEOUT_S_MIN
  )
  {
    // Do not keep a reference to the xcap timer, since that would create
    // circular references with this object
    // Instead the timer will always fire, check whether the File is closed,
    // and take the last reference to the File with it
    // That means that the File will not be deleted until the xcap expires!
    // -> it's important to explicitly close() instead of relying on the destructor
    // close(). (The FUSE interface explicitly close()s on release()).
    YIELD::platform::TimerQueue::getDefaultTimerQueue().addTimer
    ( 
      new XCapTimer
      (
        incRef(), 
        ( file_credentials.get_xcap().get_expire_timeout_s() - 
          XCAP_EXPIRE_TIMEOUT_S_MIN ) 
        * NS_IN_S 
      )
    );  
  }
  else 
    parent_volume->get_log()->getStream( YIELD::platform::Log::LOG_ERR ) <<
      "xtreemfs::File: received xcap that expires in less than " <<
      XCAP_EXPIRE_TIMEOUT_S_MIN << 
      " seconds, will not try to renew.";
}

File::~File()
{
  close();
}

bool File::close()
{
  if ( !closed )
  {
    try
    {
      if ( !latest_osd_write_response.get_new_file_size().empty() )  
      {
        parent_volume->get_mrc_proxy()->xtreemfs_update_file_size
        ( 
          file_credentials.get_xcap(), 
          latest_osd_write_response 
        );

        latest_osd_write_response.set_new_file_size( NewFileSize() );
      }
        
      if ( file_credentials.get_xcap().get_replicate_on_close() )
      {
        parent_volume->get_mrc_proxy()->close
        ( 
          parent_volume->get_vivaldi_coordinates(), 
          file_credentials.get_xcap() 
        );
      }
    }
    catch ( std::exception& )
    { }

    closed = true;
  }

  return true; // ??
}

bool File::datasync()
{
  return sync();
}

bool File::getlk( bool exclusive, uint64_t offset, uint64_t length )
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

size_t File::getpagesize()
{
  return file_credentials.get_xlocs().get_replicas()[0]
    .get_striping_policy().get_stripe_size() * 1024;
}

bool File::getxattr( const std::string& name, std::string& out_value )
{
  return parent_volume->getxattr( path, name, out_value );
}

bool File::listxattr( std::vector<std::string>& out_names )
{
  return parent_volume->listxattr( path, out_names );
}

//function is called from somewhere multiple times. size (stripe size is always 128 KB)
ssize_t File::read( void* rbuf, size_t size, uint64_t offset )
{
  std::vector<ReadResponse*> read_responses;        //vector of read responses
  YIELD::platform::auto_Log log( parent_volume->get_log() );
  ssize_t ret = 0;
  int stripCount = 1;
  int i;
  
  try
  {
#ifdef _DEBUG
    if 
    ( 
      ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
         Volume::VOLUME_FLAG_TRACE_FILE_IO 
    )
      log->getStream( YIELD::platform::Log::LOG_INFO ) << 
        "xtreemfs::File::read( rbuf, size=" << size << 
        ", offset=" << offset << 
        " )";

#endif

    unsigned char *rbuf_start = static_cast<unsigned char*>( rbuf ),      // set pointers in the read buffer
         *rbuf_p = static_cast<unsigned char*>( rbuf ), 
         *rbuf_end = static_cast<unsigned char*>( rbuf ) + size;
    uint64_t current_file_offset = offset;
    // translate strip_size from KB into Bytes
    uint32_t strip_size = file_credentials.get_xlocs().get_replicas()[0]
                            .get_striping_policy().get_stripe_size() * 1024;
    //test
    org::xtreemfs::interfaces::StripingPolicyType stripe_type = file_credentials.get_xlocs().get_replicas()[0]
                            .get_striping_policy().get_type();      // replica has a stripingPolicy and StripingPolicy has a StripingPolicyType
    unsigned char redbuf[strip_size];
    int stripe_width =  file_credentials.get_xlocs().get_replicas()[0].get_striping_policy().get_width(); // replica has a striping Policy and this has a width
    int data_width = stripe_width-1;                                   // number of data strips
    int red_width = 1;                                                 //  number of redundancy strips
    //uint32_t stripe_num = offset/(strip_size*data_width);   
    // EC:offset correction        
    //current_file_offset=(offset/data_width)*stripe_width*strip_size;
    //current_file_offset=(offset/strip_size)/data_width*stripe_width*strip_size;
            
            
#ifdef _DEBUG
    if 
    ( 
      ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
         Volume::VOLUME_FLAG_TRACE_FILE_IO 
    )
      log->getStream( YIELD::platform::Log::LOG_INFO ) << 
        "xtreemfs::File::read( rbuf, size=" << size << 
        ", corrected offset=" << current_file_offset << 
        " )";
#endif       
                 
    //test
#ifdef _DEBUG
     log->getStream( YIELD::platform::Log::LOG_INFO ) <<
       "strip_size read in this function=" << strip_size << " ,stripe_type in this function: " << stripe_type << " )"; 
#endif

    YIELD::concurrency::auto_ResponseQueue<ReadResponse> 
      read_response_queue
      ( 
        new YIELD::concurrency::ResponseQueue<ReadResponse> 
      );
    size_t expected_read_response_count = 0;

    while ( rbuf_p < rbuf_end )
    {
      // calculate the offset and number of bytes to read from one object
      // result parameters: object_number, object_offset, object_size
      uint64_t object_number = (current_file_offset/strip_size)+(((current_file_offset/strip_size)/data_width));       // translate the physically ask offset into the distribution corresponding real data offset
      uint32_t object_offset = current_file_offset % strip_size;       // offset in the current strip
      size_t object_size = static_cast<size_t>( rbuf_end - rbuf_p );    
      if ( object_offset + object_size > strip_size )
        object_size = strip_size - object_offset;

#ifdef _DEBUG
      if 
      ( 
        ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
        Volume::VOLUME_FLAG_TRACE_FILE_IO 
      )
      {
        log->getStream( YIELD::platform::Log::LOG_INFO ) << 
          "xtreemfs::File: issuing read # " << 
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
        // build a new read request
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
              
      
      // TODO: if last readRequest for data was send-> send additionally request for redundancy strip
      if (stripCount==data_width){       
        object_size=strip_size;
        object_number++;
#ifdef _DEBUG
        if 
        ( 
          ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
          Volume::VOLUME_FLAG_TRACE_FILE_IO 
        )
        {
          log->getStream( YIELD::platform::Log::LOG_INFO ) << 
            "xtreemfs::File: issuing read # " << 
            ( expected_read_response_count + 1 ) <<
            " for " << object_size << 
            " bytes from redundant data as object number " << object_number <<
            " in file " << file_credentials.get_xcap().get_file_id() <<
            " (object offset = " << object_offset <<
            ", file offset = " << current_file_offset <<").";
        }
#endif
        // build a new read request
        ReadRequest* read_request = 
          new ReadRequest
          ( 
            file_credentials, 
            file_credentials.get_xcap().get_file_id(), 
            object_number, 
            0, 
            object_offset, 
            static_cast<uint32_t>( object_size ),
            new ReadBuffer( redbuf, object_size ) 
          );
        read_request->set_response_target( read_response_queue->incRef() );
        read_request->set_selected_file_replica( selected_file_replica );

        parent_volume->get_osd_proxy_mux()->send( *read_request );
        expected_read_response_count++;      
                                 
      } // end if redundancy strip        
              
      stripCount++;        
    } // end while (rbuf_p < rbuf_end)

#ifdef _DEBUG
    if 
    ( 
      ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
      Volume::VOLUME_FLAG_TRACE_FILE_IO 
    )
      log->getStream( YIELD::platform::Log::LOG_INFO ) << 
        "xtreemfs::File: issued " << expected_read_response_count << 
        " parallel reads.";
#endif

    // EC later: wait only until the minimal number of data strips arrived   
    // TODO: only while loop necessary: while (read_response_i<data_width)  read_response_i++;           
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

      // zero padding is defined by POSIX semantics
      yidl::runtime::auto_Buffer data( read_response.get_object_data().get_data() );
      rbuf_p = static_cast<unsigned char*>( static_cast<void*>( *data ) );          // put data into the buffer
      uint32_t zero_padding = read_response.get_object_data().get_zero_padding();   // do zero padding
      if ( read_response.get_selected_file_replica() != 0 )
        selected_file_replica = read_response.get_selected_file_replica();

#ifdef _DEBUG
      if ( ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
            Volume::VOLUME_FLAG_TRACE_FILE_IO )
      {
        log->getStream( YIELD::platform::Log::LOG_INFO ) << 
          "xtreemfs::File: read " << data->size() <<
          " bytes from file " << file_credentials.get_xcap().get_file_id() <<            
          " with " << zero_padding << " bytes of zero padding" <<
          ", starting from buffer offset " << 
          static_cast<size_t>( rbuf_p - rbuf_start ) << 
          ", read # " << ( read_response_i + 1 ) << " of " << 
          expected_read_response_count << " parallel reads.";
      }
#endif
              
      // recovery
//       if (redStrip_initialized) { 
//         for (i=object_offset; i<(object_offset+object_size); i++) {
//           redbuf[i]=redbuf[i]^wbuf_p[i];
//         }      
//       } 
//       else {
//         for (i=0;i<object_offset; i++){
//           redbuf[i]=0;
//         }
//         for (i=object_offset; i<object_offset+object_size; i++) { 
//           redbuf[i]=wbuf_p[i];
//         }
//         for (i=object_offset+object_size; i<strip_size; i++){ 
//           redbuf[i]=0;
//         }
//         redStrip_initialized=1;  
//       }
             
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
            "xtreemfs::File: received zero_padding (data size=" << 
            data->size() << ", zero_padding=" << zero_padding << 
            ") larger than available buffer space (" << 
            static_cast<size_t>( rbuf_end - rbuf_p ) << ")";
          YIELD::concurrency::ExceptionResponse::set_errno( EIO );
          ret = -1;
          break;
        }
      }
      
    } // end for all read responses
    // EC: correct ret (size counter)
    ret -= strip_size; 
    if (ret<0)
       ret += strip_size;               
            
#ifdef _DEBUG
      log->getStream( YIELD::platform::Log::LOG_INFO ) << 
        "DEBUG TEST 1 ret:" << ret << " size: " << size ;
#endif
              
#ifdef _DEBUG
    if ( static_cast<size_t>( ret ) > size ) 
      DebugBreak();
      
    log->getStream( YIELD::platform::Log::LOG_INFO ) << 
      "DEBUG TEST 2" ;
    
    if 
    ( 
      ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
       Volume::VOLUME_FLAG_TRACE_FILE_IO 
    )
    {
      log->getStream( YIELD::platform::Log::LOG_INFO ) << 
        "xtreemfs::File: read " << ret <<
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
        "xtreemfs::File: read threw ProxyExceptionResponse: " <<
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
      "xtreemfs::File::read threw std::exception: " << exc.what() << ".";

    YIELD::platform::Exception::set_errno( EIO );

    ret = -1;
  }
#ifdef _DEBUG
      log->getStream( YIELD::platform::Log::LOG_INFO ) << 
        "DEBUG TEST 3" ;
#endif
  for 
  ( 
    std::vector<ReadResponse*>::iterator read_response_i = read_responses.begin(); 
    read_response_i != read_responses.end(); 
    read_response_i++ 
  )
    yidl::runtime::Object::decRef( **read_response_i );
#ifdef _DEBUG
      log->getStream( YIELD::platform::Log::LOG_INFO ) << 
        "DEBUG TEST 4" ;
#endif
  return ret;
}

bool File::removexattr( const std::string& name )
{
  return parent_volume->removexattr( path, name );
}

bool File::setlk( bool exclusive, uint64_t offset, uint64_t length )
{
  FILE_OPERATION_BEGIN;

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

  FILE_OPERATION_END;
}

bool File::setlkw( bool exclusive, uint64_t offset, uint64_t length )
{
  FILE_OPERATION_BEGIN;

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

  FILE_OPERATION_END;
}

bool File::setxattr
( 
  const std::string& name, 
  const std::string& value, 
  int flags 
)
{
  return parent_volume->setxattr( path, name, value, flags );
}

YIELD::platform::auto_Stat File::stat()
{
  if ( !latest_osd_write_response.get_new_file_size().empty() )
  {
    YIELD::platform::auto_Stat stbuf = parent_volume->stat( path );
    stbuf->set_size( latest_osd_write_response.get_new_file_size().get_size() );
    return stbuf;
  }
  else
    return parent_volume->stat( path );
}

bool File::sync()
{
  FILE_OPERATION_BEGIN;

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

  FILE_OPERATION_END;
}

bool File::truncate( uint64_t new_size )
{
  FILE_OPERATION_BEGIN;

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

  FILE_OPERATION_END;
}

bool File::unlk( uint64_t offset, uint64_t length )
{
  FILE_OPERATION_BEGIN;

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

  FILE_OPERATION_END;
}

ssize_t File::write( const void* wbuf, size_t size, uint64_t offset )       // File write Operation
{
  YIELD::platform::auto_Log log( parent_volume->get_log() );
  ssize_t ret;
  std::vector<OSDInterface::writeResponse*> write_responses;
  // redundant strip initialization state
  char redStrip_initialized=0;
  int i;

#ifdef _DEBUG
  if 
  ( 
    ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
    Volume::VOLUME_FLAG_TRACE_FILE_IO 
  )
    log->getStream( YIELD::platform::Log::LOG_INFO ) << 
      "xtreemfs::File::write( wbuf, stripe size=" << size << 
      ", buffer offset=" << offset << " )";
#endif

  try
  {
    const unsigned char *wbuf_p = static_cast<const unsigned char*>( wbuf ),              // pointer to the first element of wbuf                      
               *wbuf_end = static_cast<const unsigned char*>( wbuf ) + size;     // pointer to the end of the wbuf
    uint64_t current_file_offset = offset;
    uint32_t strip_size = file_credentials.get_xlocs().get_replicas()[0]
                            .get_striping_policy().get_stripe_size() * 1024;    // transform strip size in KB to strip size in Byte (default 128KB)
    // EC: declare strip for redundancy array
    unsigned char redbuf[strip_size];                                                        
    //char redbuf[strip_size]; 
    
    // test
    unsigned char redbuf_test2[] ={5,1,2,3};  
    
    org::xtreemfs::interfaces::StripingPolicyType stripe_type = file_credentials.get_xlocs().get_replicas()[0]
                            .get_striping_policy().get_type();      // replica has a stripingPolicy and StripingPolicy has a StripingPolicyTyp
    int stripe_width =  file_credentials.get_xlocs().get_replicas()[0].get_striping_policy().get_width(); // replica has a striping Policy and this has a width
    int data_width = stripe_width-1;                                   // number of data strips 
    int red_width = 1;                                                 //  number of redundancy strips
    uint32_t stripe_num = offset/(strip_size*data_width);                        
        
#ifdef _DEBUG
     log->getStream( YIELD::platform::Log::LOG_INFO ) <<
       "xtreemfs::File::write(): strip_size=" << strip_size << "Byte, stripe_type= " << stripe_type << ", stripe_width=" << stripe_width << ", stripe_num=" << stripe_num;
#endif
#ifdef _DEBUG
     log->getStream( YIELD::platform::Log::LOG_INFO ) <<
       "xtreemfs::File::write(): content wbuf array: "<< (int)wbuf_p[0] << " " << (int)wbuf_p[strip_size-1]<< " " << (int)wbuf_p[strip_size]<< " " << (int)wbuf_p[2*strip_size-1]<< " " << (int)wbuf_p[2*strip_size]<< " " << (int)wbuf_p[3*strip_size-1];
#endif

// char output test
//     cout << "TEST: redbuf_test2[0]" << (int)redbuf_test2[0] << "\n";
//     redbuf_test2[0]=redbuf_test2[0]^redbuf_test2[3];
//     cout << "TEST: redbuf_test2[0]=6: " << (int)redbuf_test2[0] << " redbuf_test2[3]: " << (int)redbuf_test2[3] << "\n";
//     redbuf_test2[0]=redbuf_test2[0]^redbuf_test2[2];
//     cout << "TEST: redbuf_test2[0]=4: " << (int)redbuf_test2[0] << "\n";


    YIELD::concurrency::auto_ResponseQueue<OSDInterface::writeResponse>         // create new writeResponse
      write_response_queue
      (
        new YIELD::concurrency::ResponseQueue<OSDInterface::writeResponse>
      );
    size_t expected_write_response_count = 0;
    size_t strip_count = 0;
    
    while ( wbuf_p < wbuf_end )                                         // comparison of data structures and not between pointers
                                                                        // buffer has still data: set variables for this object and send( *write_request );
    {      
      uint64_t object_number = current_file_offset / strip_size + (stripe_num*red_width);       // calculate the number of the object (=strip) in the file
      uint32_t object_offset = current_file_offset % strip_size;       // calculate offset inside of the object = offset inside of the strip
      uint64_t object_size = static_cast<size_t>( wbuf_end - wbuf_p );  // object size left in the buffer to write from 
      if ( object_offset + object_size > strip_size )                  // test if remaining data is larger than space in the next strip
        object_size = strip_size - object_offset;                      // set object size to the new value

#ifdef _DEBUG
     log->getStream( YIELD::platform::Log::LOG_INFO ) <<
       "xtreemfs::File::write(): parameters: global object_number " << object_number << " strip_offset " << object_offset << " size to write in strip " << object_size << " strip_size " << strip_size << " current_file_offset " << current_file_offset;
#endif
          
/*#ifdef _DEBUG
     log->getStream( YIELD::platform::Log::LOG_INFO ) <<
       "xtreemfs::File::write(): DEBUG redStrip_initialized: " << redStrip_initialized << " redbuf[0]:" << redbuf[0] << " redbuf[strip_size] " << redbuf[strip_size-1] << " wbuf begin, end: " << current_file_offset+object_offset << " " << current_file_offset+object_offset+object_size-1 << " content " << wbuf_p[0] << wbuf_p[strip_size-1];
#endif */         

      // EC: build redundant data 
      // if object_number % d_width==0 copy data into the r_buffer (filled with zeros if smaller than a full strip)
      // else make XOR operation 
      // TODO: Catch situation when write does not start at position 0 in the current strip
      if (redStrip_initialized) {      
        for (i=object_offset; i<(object_offset+object_size); i++) {
          //redbuf[i]=redbuf[i]^static_cast<unsigned char>(wbuf_p[i]);
          redbuf[i]=redbuf[i]^wbuf_p[i];
          //redbuf[i]=redbuf[i]^static_cast<char>(wbuf_p[i]);
        }
#ifdef _DEBUG
     log->getStream( YIELD::platform::Log::LOG_INFO ) <<
                   "xtreemfs::File::write(): test redbuf[0] after xor ops, w_buffers: " << (int)redbuf[0] << " ," << (int)wbuf_p[0]<< " ," << (int)wbuf_p[object_size-1];
#endif         
      }
      else {
        for (i=0;i<object_offset; i++){
          redbuf[i]=0; 
        }
/*#ifdef _DEBUG
     log->getStream( YIELD::platform::Log::LOG_INFO ) <<
       "xtreemfs::File::write(): test redbuf " << redbuf;
#endif*/            
        for (i=object_offset; i<object_offset+object_size; i++) {
          //redbuf[i]=static_cast<unsigned char>(wbuf_p[i]);
          redbuf[i]=wbuf_p[i];
          //redbuf[i]=static_cast<char>(wbuf_p[i]);
        }  
/*#ifdef _DEBUG
     log->getStream( YIELD::platform::Log::LOG_INFO ) <<
                   "xtreemfs::File::write(): test redbuf " << redbuf;
#endif */         
        for (i=object_offset+object_size; i<strip_size; i++){
          redbuf[i]=0;
        }  
#ifdef _DEBUG
     log->getStream( YIELD::platform::Log::LOG_INFO ) <<
                   "xtreemfs::File::write(): test redbuf[0] after init " << (int)redbuf[0] << " " << (int)redbuf[strip_size-1];
#endif          
        redStrip_initialized=1;
      }

      ObjectData object_data                                            // create object_data object and set all informations, specially the writeBuffer
      ( 
        0,
        false,
        0,
        new WriteBuffer( wbuf_p, static_cast<uint32_t>( object_size ) )     // create a new WriteBuffer (inherits from fixedBuffer) for the object data
      );    
            
      OSDInterface::writeRequest* write_request =                           // create a writeRequest: file_credentials, fileID, object_number, object_version, offset, lease_timeout, object_data
        new OSDInterface::writeRequest
        ( 
          file_credentials, 
          file_credentials.get_xcap().get_file_id(), 
          object_number, 
          0, //object_version
          object_offset, 
          0, //lease_timeout
          object_data 
        );
      write_request->set_selected_file_replica( selected_file_replica );    // set the selected replica into the write_request          

#ifdef _DEBUG
      if 
      ( 
        ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
           Volume::VOLUME_FLAG_TRACE_FILE_IO 
      )
      {
        log->getStream( YIELD::platform::Log::LOG_INFO ) << 
          "xtreemfs::File: issuing write # " << 
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
              
//       log->getStream( YIELD::platform::Log::LOG_INFO ) << 
//           "xtreemfs::get_osd_proxy_mux " << 
//           " result: " << parent_volume->get_osd_proxy_mux() << 
//           " bytes to object number " << object_number <<
//           " in file " << file_credentials.get_xcap().get_file_id() <<
//           " (object offset = " << object_offset <<
//           ", file offset = " << current_file_offset  <<
//           ").";        
      parent_volume->get_osd_proxy_mux()->send( *write_request );                   // send write_request to the volume
      expected_write_response_count++;
      strip_count++;
      
      // write redundant data strip        
      if ( (strip_count==stripe_width-1) || (wbuf_p+object_size>=wbuf_end)) {
        ObjectData red_object_data                                            // create object_data object and set all informations, specially the writeBuffer
        ( 
          0,
          false,
          0,
          new WriteBuffer( redbuf, static_cast<uint32_t>( strip_size ) )     // create a new WriteBuffer (inherits from fixedBuffer) for the object data
        );
              
        OSDInterface::writeRequest* red_write_request =                           // create a writeRequest: file_credentials, fileID, object_number, object_version, offset, lease_timeout, object_data
            new OSDInterface::writeRequest
            ( 
            file_credentials, 
            file_credentials.get_xcap().get_file_id(), 
            data_width+(stripe_num*stripe_width), //file global object_number for parity strip; sequential numbers for blocks depending on buffer offset
            0, //object_version
            object_offset, 
            0, //lease_timeout
            red_object_data 
            );
        red_write_request->set_selected_file_replica( selected_file_replica );    // set the selected replica into the write_request            
#ifdef _DEBUG
      if 
      ( 
        ( parent_volume->get_flags() & Volume::VOLUME_FLAG_TRACE_FILE_IO ) == 
           Volume::VOLUME_FLAG_TRACE_FILE_IO 
      )
      {
        log->getStream( YIELD::platform::Log::LOG_INFO ) << 
          "xtreemfs::File: issuing write # " << 
          ( expected_write_response_count + 1 ) <<
          " of " << object_size << 
          " bytes to object number " << object_number <<
          " in file " << file_credentials.get_xcap().get_file_id() <<
          " (object offset = " << object_offset <<
          ", file offset = " << current_file_offset  <<
          ").";
      }
#endif     
        red_write_request->set_response_target( write_response_queue->incRef() );              
        parent_volume->get_osd_proxy_mux()->send( *red_write_request );
        expected_write_response_count++;
      
#ifdef _DEBUG
     log->getStream( YIELD::platform::Log::LOG_INFO ) <<
                   "xtreemfs::File::write(): contend redbuf[0]: "<< (int)redbuf[0] << ", " << (int)redbuf[strip_size-1];
#endif
      }// end if end of stripe -> write redundancy strip             

      wbuf_p += object_size;
      current_file_offset += object_size;
              
      // EC: write redundant data if (1) wbuf_p>wbuf_end oder (2) object_number % d_width==d_width
      // redStrip_initialized=0;        
              
    }// end while
   
             
              
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
          "xtreemfs::File::write received response # " << 
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
            "xtreemfs::File: OSD write response is newer than latest known.";
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
            "xtreemfs::File: flushing file size updates.";
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
        "xtreemfs::File: write threw ProxyExceptionResponse: " <<
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
      "xtreemfs::File: write threw std::exception: " << exc.what() << ".";

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

