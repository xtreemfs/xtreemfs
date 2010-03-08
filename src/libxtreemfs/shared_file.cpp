// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the XtreemFS project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the XtreemFS project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#include "shared_file.h"
#include "open_file.h"
#include "stat.h"
#include "xtreemfs/volume.h"
using namespace xtreemfs;

#include "yieldfs.h"

#include <errno.h>

#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif


#define SHARED_FILE_OPERATION_BEGIN( OperationName ) \
  try \
  {

#define SHARED_FILE_OPERATION_END( OperationName ) \
  } \
  catch ( ProxyExceptionResponse& proxy_exception_response ) \
  { \
    Volume::set_errno( log.get(), #OperationName, proxy_exception_response ); \
    return false; \
  } \
  catch ( std::exception& exc ) \
  { \
    Volume::set_errno( log.get(), #OperationName, exc ); \
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

  yield::concurrency::auto_Response createResponse()
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
  Log& log,
  auto_Volume parent_volume,
  const Path& path
) : log( log ),
    parent_volume( parent_volume ),
    path( path )
{
  reader_count = writer_count = 0;
  selected_file_replica = 0;
}

void SharedFile::close( xtreemfs::OpenFile& open_file )
{
  if ( open_file.get_xcap().get_access_mode() == O_RDONLY )
  {
#ifdef _DEBUG
    if ( reader_count == 0 )
      DebugBreak();
#endif
    --reader_count;
  }
  else
  {
#ifdef _DEBUG
    if ( writer_count == 0 )
      DebugBreak();
#endif
    --writer_count;

    if ( writer_count == 0 )
    {
      parent_volume->metadatasync( path, open_file.get_xcap() );

      if ( open_file.get_xcap().get_replicate_on_close() )
      {
        parent_volume->get_mrc_proxy()->close
        (
          parent_volume->get_vivaldi_coordinates(),
          open_file.get_xcap()
        );
      }
    }
  }

  // Don't dec_ref open_file, since we didn't keep a reference
  // OpenFile::dec_ref( open_file );

  if ( reader_count == 0 && writer_count == 0 )
    parent_volume->release( *this );
}

yield::platform::auto_Stat SharedFile::getattr()
{
  return parent_volume->getattr( path );
}

bool
SharedFile::getlk
(
  bool exclusive,
  uint64_t offset,
  uint64_t length,
  const XCap& xcap
)
{
  Lock lock =
    parent_volume->get_osd_proxy_mux()->xtreemfs_lock_check
    (
      FileCredentials( xcap, xlocs ),
      parent_volume->get_uuid(),
      yieldfs::FUSE::getpid(),
      xcap.get_file_id(),
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

yield::platform::auto_File
SharedFile::open
(
  FileCredentials& file_credentials
)
{
  if ( file_credentials.get_xlocs().get_version() >= xlocs.get_version() )
    xlocs = file_credentials.get_xlocs();

  if ( file_credentials.get_xcap().get_access_mode() == O_RDONLY )
    ++reader_count;
  else
    ++writer_count;

  return new xtreemfs::OpenFile( inc_ref(), file_credentials.get_xcap() );
}

ssize_t
SharedFile::read
(
  void* rbuf,
  size_t size,
  uint64_t offset,
  const XCap& xcap
)
{
  std::vector<ReadResponse*> read_responses;
  Log& log( parent_volume->get_log() );
  ssize_t ret = 0;

  try
  {
#ifdef _DEBUG
    if
    (
      ( parent_volume->get_flags() & Volume::FLAG_TRACE_FILE_IO ) ==
         Volume::FLAG_TRACE_FILE_IO
    )
      log->getStream( yield::platform::Log::LOG_INFO ) <<
        "xtreemfs::SharedFile::read( rbuf, size=" << size <<
        ", offset=" << offset <<
        " )";
#endif

    char *rbuf_start = static_cast<char*>( rbuf ),
         *rbuf_p = static_cast<char*>( rbuf ),
         *rbuf_end = static_cast<char*>( rbuf ) + size;
    uint64_t current_file_offset = offset;
    uint32_t stripe_size = xlocs.get_replicas()[0].get_striping_policy().
                             get_stripe_size() * 1024;

    yield::concurrency::auto_ResponseQueue<ReadResponse>
      read_response_queue
      (
        new yield::concurrency::ResponseQueue<ReadResponse>
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
        ( parent_volume->get_flags() & Volume::FLAG_TRACE_FILE_IO ) ==
        Volume::FLAG_TRACE_FILE_IO
      )
      {
        log->getStream( yield::platform::Log::LOG_INFO ) <<
          "xtreemfs::SharedFile: issuing read # " <<
          ( expected_read_response_count + 1 ) <<
          " for " << object_size <<
          " bytes from object number " << object_number <<
          " in file " << xcap.get_file_id() <<
          " (object offset = " << object_offset <<
          ", file offset = " << current_file_offset <<
          ", remaining buffer size = " <<
          static_cast<size_t>( rbuf_end - rbuf_p ) << ").";
      }
#endif

      ReadRequest* read_request =
        new ReadRequest
        (
          FileCredentials( xcap, xlocs ),
          xcap.get_file_id(),
          object_number,
          0,
          object_offset,
          static_cast<uint32_t>( object_size ),
          new ReadBuffer( rbuf_p, object_size )
        );
      read_request->set_response_target( read_response_queue->inc_ref() );
      read_request->set_selected_file_replica( selected_file_replica );

      parent_volume->get_osd_proxy_mux()->send( *read_request );
      expected_read_response_count++;

      rbuf_p += object_size;
      current_file_offset += object_size;
    }

#ifdef _DEBUG
    if
    (
      ( parent_volume->get_flags() & Volume::FLAG_TRACE_FILE_IO ) ==
      Volume::FLAG_TRACE_FILE_IO
    )
      log->getStream( yield::platform::Log::LOG_INFO ) <<
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
      // Object::dec_ref( read_response );
      read_responses.push_back( &read_response );

      yidl::runtime::auto_Buffer data( read_response.get_object_data().get_data() );
      rbuf_p = static_cast<char*>( static_cast<void*>( *data ) );
      uint32_t zero_padding = read_response.get_object_data().get_zero_padding();
      if ( read_response.get_selected_file_replica() != 0 )
        selected_file_replica = read_response.get_selected_file_replica();

#ifdef _DEBUG
      if ( ( parent_volume->get_flags() & Volume::FLAG_TRACE_FILE_IO ) ==
            Volume::FLAG_TRACE_FILE_IO )
      {
        log->getStream( yield::platform::Log::LOG_INFO ) <<
          "xtreemfs::SharedFile: read " << data->size() <<
          " bytes from file " << xcap.get_file_id() <<
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
          log->getStream( yield::platform::Log::LOG_ERR ) <<
            "xtreemfs::SharedFile: received zero_padding (data size=" <<
            data->size() << ", zero_padding=" << zero_padding <<
            ") larger than available buffer space (" <<
            static_cast<size_t>( rbuf_end - rbuf_p ) << ")";

#ifdef _WIN32
          SetLastError( EIO );
#else
          errno = EIO;
#endif
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
      ( parent_volume->get_flags() & Volume::FLAG_TRACE_FILE_IO ) ==
       Volume::FLAG_TRACE_FILE_IO
    )
    {
      log->getStream( yield::platform::Log::LOG_INFO ) <<
        "xtreemfs::SharedFile: read " << ret <<
        " bytes from file " << xcap.get_file_id() <<
        " in total, returning from read().";
    }
#endif
  }
  catch ( ProxyExceptionResponse& proxy_exception_response )
  {
    Volume::set_errno( log.get(), "read", proxy_exception_response );
    ret = -1;
  }
  catch ( std::exception& exc )
  {
    Volume::set_errno( log.get(), "read", exc );
    ret = -1;
  }

  for
  (
    std::vector<ReadResponse*>::iterator read_response_i = read_responses.begin();
    read_response_i != read_responses.end();
    read_response_i++
  )
    ReadResponse::dec_ref( **read_response_i );

  return ret;
}

bool SharedFile::removexattr( const std::string& name )
{
  return parent_volume->removexattr( path, name );
}

bool
SharedFile::setlk
(
  bool exclusive,
  uint64_t offset,
  uint64_t length,
  const XCap& xcap
)
{
  SHARED_FILE_OPERATION_BEGIN( setlk );

  //Lock lock =
    parent_volume->get_osd_proxy_mux()->xtreemfs_lock_acquire
    (
      FileCredentials( xcap, xlocs ),
      parent_volume->get_uuid(),
      yieldfs::FUSE::getpid(),
      xcap.get_file_id(),
      offset, length,
      exclusive
    );

  //locks.push_back( lock );

  return true;

  SHARED_FILE_OPERATION_END( setlk );
}

bool
SharedFile::setlkw
(
  bool exclusive,
  uint64_t offset,
  uint64_t length,
  const XCap& xcap
)
{
  SHARED_FILE_OPERATION_BEGIN( setlkw );

  for ( ;; )
  {
    try
    {
       //Lock lock =
          parent_volume->get_osd_proxy_mux()->xtreemfs_lock_acquire
          (
            FileCredentials( xcap, xlocs ),
            parent_volume->get_uuid(),
            yieldfs::FUSE::getpid(),
            xcap.get_file_id(),
            offset,
            length,
            exclusive
          );

      //locks.push_back( lock );

      return true;
    }
    catch ( ProxyExceptionResponse& )
    { }
  }

  SHARED_FILE_OPERATION_END( setlkw );
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

bool
SharedFile::sync
(
  const XCap& xcap
)
{
  SHARED_FILE_OPERATION_BEGIN( sync );

  parent_volume->metadatasync( path, xcap );

  return true;

  SHARED_FILE_OPERATION_END( sync );
}

bool
SharedFile::truncate
(
  uint64_t new_size,
  XCap& xcap
)
{
  SHARED_FILE_OPERATION_BEGIN( truncate );

  XCap truncate_xcap;
  parent_volume->get_mrc_proxy()->
    ftruncate( xcap, truncate_xcap );
  xcap = truncate_xcap;

  OSDWriteResponse osd_write_response;
  parent_volume->get_osd_proxy_mux()->truncate
  (
    FileCredentials( xcap, xlocs ),
    xcap.get_file_id(),
    new_size,
    osd_write_response
  );

  if ( !osd_write_response.get_new_file_size().empty() )
  {
    parent_volume->fsetattr
    (
      path,
      new Stat( osd_write_response ),
      yield::platform::Volume::SETATTR_SIZE,
      xcap
    );
  }

  return true;

  SHARED_FILE_OPERATION_END( truncate );
}

bool
SharedFile::unlk
(
  uint64_t offset,
  uint64_t length,
  const XCap& xcap
)
{
  //SHARED_FILE_OPERATION_BEGIN( unlk );

  //if ( locks.empty() )
  //  return true;
  //else
  //{
  //  parent_volume->get_osd_proxy_mux()->xtreemfs_lock_release
  //  (
  //    file_credentials,
  //    xcap.get_file_id(),
  //    locks.back()
  //  );

  //  locks.pop_back();

  //  return true;
  //}

  //SHARED_FILE_OPERATION_END( unlk );

  return true;
}

ssize_t
SharedFile::write
(
  const void* wbuf,
  size_t size,
  uint64_t offset,
  const XCap& xcap
)
{
  ssize_t ret;
  std::vector<OSDInterface::writeResponse*> write_responses;

#ifdef _DEBUG
  if
  (
    ( parent_volume->get_flags() & Volume::FLAG_TRACE_FILE_IO ) ==
    Volume::FLAG_TRACE_FILE_IO
  )
    log->getStream( yield::platform::Log::LOG_INFO ) <<
      "xtreemfs::SharedFile::write( wbuf, size=" << size <<
      ", offset=" << offset << " )";
#endif

  try
  {
    const char *wbuf_p = static_cast<const char*>( wbuf ),
               *wbuf_end = static_cast<const char*>( wbuf ) + size;
    uint64_t current_file_offset = offset;
    uint32_t stripe_size = xlocs.get_replicas()[0].get_striping_policy().
                             get_stripe_size() * 1024;

    yield::concurrency::auto_ResponseQueue<OSDInterface::writeResponse>
      write_response_queue
      (
        new yield::concurrency::ResponseQueue<OSDInterface::writeResponse>
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
          FileCredentials( xcap, xlocs ),
          xcap.get_file_id(),
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
        ( parent_volume->get_flags() & Volume::FLAG_TRACE_FILE_IO ) ==
           Volume::FLAG_TRACE_FILE_IO
      )
      {
        log->getStream( yield::platform::Log::LOG_INFO ) <<
          "xtreemfs::SharedFile: issuing write # " <<
          ( expected_write_response_count + 1 ) <<
          " of " << object_size <<
          " bytes to object number " << object_number <<
          " in file " << xcap.get_file_id() <<
          " (object offset = " << object_offset <<
          ", file offset = " << current_file_offset  <<
          ").";
      }
#endif

      write_request->set_response_target( write_response_queue->inc_ref() );
      parent_volume->get_osd_proxy_mux()->send( *write_request );
      expected_write_response_count++;

      wbuf_p += object_size;
      current_file_offset += object_size;
    }


    OSDWriteResponse latest_osd_write_response;

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
        ( parent_volume->get_flags() & Volume::FLAG_TRACE_FILE_IO ) ==
           Volume::FLAG_TRACE_FILE_IO
      )
        log->getStream( yield::platform::Log::LOG_INFO ) <<
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
          ( parent_volume->get_flags() & Volume::FLAG_TRACE_FILE_IO ) ==
              Volume::FLAG_TRACE_FILE_IO
        )
          log->getStream( yield::platform::Log::LOG_INFO ) <<
            "xtreemfs::SharedFile: OSD write response is newer than latest known.";
#endif

        latest_osd_write_response = write_response.get_osd_write_response();
      }

      // yidl::runtime::Object::dec_ref( write_response );
      write_responses.push_back( &write_response );
    }

    if ( !latest_osd_write_response.get_new_file_size().empty() )
    {
      parent_volume->fsetattr
      (
        path,
        new Stat( latest_osd_write_response ),
        yield::platform::Volume::SETATTR_SIZE,
        xcap
      );
    }

    ret = static_cast<ssize_t>( current_file_offset - offset );
  }
  catch ( ProxyExceptionResponse& proxy_exception_response )
  {
    Volume::set_errno( log.get(), "write", proxy_exception_response );
    ret = -1;
  }
  catch ( std::exception& exc )
  {
    Volume::set_errno( log.get(), "write", exc );
    ret = -1;
  }

  for
  (
    std::vector<OSDInterface::writeResponse*>::iterator
      write_response_i = write_responses.begin();
    write_response_i != write_responses.end();
    write_response_i++
   )
     OSDInterface::writeResponse::dec_ref( **write_response_i );

  return ret;
}

