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


#include "file.h"
#include "stat.h"
#include "xtreemfs/volume.h"
using org::xtreemfs::interfaces::FileCredentials;
using org::xtreemfs::interfaces::ObjectData;
using org::xtreemfs::interfaces::OSDInterfaceEvents;
using org::xtreemfs::interfaces::OSDWriteResponse;
using org::xtreemfs::interfaces::XCAP_EXPIRE_TIMEOUT_S_MIN;
using namespace xtreemfs;

#include "yield.h"
using yield::concurrency::ExceptionResponse;
using yield::concurrency::ResponseQueue;
using yield::platform::TimerQueue;

#include "yieldfs.h"


#define FILE_OPERATION_BEGIN( OperationName ) \
  try \
  {

#define FILE_OPERATION_END( OperationName ) \
  } \
  catch ( ExceptionResponse& exception_response ) \
  { \
    parent_volume.set_errno( #OperationName, exception_response ); \
    return false; \
  } \
  catch ( std::exception& exc ) \
  { \
    parent_volume.set_errno( #OperationName, exc ); \
    return false; \
  }


class File::WriteBuffer : public yidl::runtime::FixedBuffer
{
public:
  WriteBuffer( const void* buf, size_t len )
    : FixedBuffer( len )
  {
    iov.iov_base = const_cast<void*>( buf );
    iov.iov_len = len;
  }

  // yidl::runtime::RTTIObject
  YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( File::WriteBuffer, 0 );  
};


class File::XCapTimer : public TimerQueue::Timer
{
public:
  XCapTimer( File& file, const Time& timeout )
    : TimerQueue::Timer( timeout ),
      file( file.inc_ref() )
  { }

  // TimerQueue::Timer
  void fire()
  {
    if ( !file.closed )
    {                         
      try
      {
        XCap renewed_xcap;

        //file.parent_volume->get_log()->
        //  get_stream( Log::LOG_INFO ) <<
        //  "xtreemfs::File: renewing XCap for file " <<
        //  file.file_credentials.get_xcap().get_file_id() << ".";

        file.parent_volume.get_mrc_proxy().xtreemfs_renew_capability
        (
          file.xcap,
          renewed_xcap
        );

        //file.parent_volume->get_log()->
        //  get_stream( Log::LOG_INFO ) <<
        //  "xtreemfs::File: successfully renewed XCap for file " <<
        //  file.file_credentials.get_xcap().get_file_id() << ".";

        file.xcap = renewed_xcap;

        if ( renewed_xcap.get_expire_timeout_s() > XCAP_EXPIRE_TIMEOUT_S_MIN )
        {
          // Add another timer for the renewed xcap
          // Don't use periods here on the pessimistic assumption that
          // most xcaps will never be renewed
          TimerQueue::getDefaultTimerQueue().addTimer
          (
            *new XCapTimer
             (
               file,
               ( renewed_xcap.get_expire_timeout_s() -
                 XCAP_EXPIRE_TIMEOUT_S_MIN )
               * Time::NS_IN_S
             )
          );
        }
        //else
        //  file.parent_volume->get_log()->
        //    get_stream( Log::LOG_ERR ) <<
        //      "xtreemfs::File: received xcap for file " <<
        //      renewed_xcap.get_file_id() <<
        //      "that expires in less than " <<
        //      XCAP_EXPIRE_TIMEOUT_S_MIN <<
        //      " seconds, will not try to renew.";
      }
      catch ( std::exception& )
      {
        //file.parent_volume->get_log()->
        //  get_stream( Log::LOG_ERR ) <<
        //  "xtreemfs::File: caught exception trying to renew XCap for file " <<
        //  file.file_credentials.get_xcap().get_file_id() <<
        //  ": " << exc.what() << ".";
      }
    }
  }

private:
  File& file;
};


File::File
( 
  Volume& parent_volume, 
  const Path& path, 
  const XCap& xcap, 
  const XLocSet& xlocs 
) : parent_volume( parent_volume ), path( path ), xcap( xcap ), xlocs( xlocs )
{
  closed = false;
  selected_file_replica_i = 0;

  if
  (
    xcap.get_expire_timeout_s() >
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
    TimerQueue::getDefaultTimerQueue().addTimer
    (
      *new XCapTimer
       (
         inc_ref(),
 //        10 * NS_IN_S
         ( xcap.get_expire_timeout_s() - XCAP_EXPIRE_TIMEOUT_S_MIN )
         * Time::NS_IN_S
       )
    );
  }
  //else
  //  parent_volume->get_log()->get_stream( Log::LOG_ERR ) <<
  //    "xtreemfs::File: received xcap that expires in less than " <<
  //    XCAP_EXPIRE_TIMEOUT_S_MIN <<
  //    " seconds, will not try to renew.";
}

File::~File()
{
  close();
}

bool File::close()
{
  if ( !closed )
  {
    parent_volume.release( *this );
    closed = true;
  }

  return true;
}

bool File::datasync()
{
  return sync();
}

yield::platform::Stat* File::getattr()
{
  return parent_volume.getattr( path );
}

bool File::getlk( bool exclusive, uint64_t offset, uint64_t length )
{
  DebugBreak();
  //Lock lock =
  //  parent_volume.get_osd_proxy_mux().xtreemfs_lock_check
  //  (
  //    FileCredentials( xcap, xlocs ),
  //    parent_volume.get_uuid(),
  //    yieldfs::FUSE::getpid(),
  //    xcap.get_file_id(),
  //    offset,
  //    length,
  //    exclusive
  //  );

  //return lock.get_client_pid() != yieldfs::FUSE::getpid();
  return false;
}

size_t File::getpagesize()
{
  return xlocs.get_replicas()[0].
           get_striping_policy().get_stripe_size() * 1024;
}

bool File::getxattr( const string& name, string& out_value )
{
  return parent_volume.getxattr( path, name, out_value );
}

bool File::listxattr( vector<string>& out_names )
{
  return parent_volume.listxattr( path, out_names );
}

ssize_t File::read( void* rbuf, size_t size, uint64_t offset )
{
//  vector<ReadResponse*> read_responses;
//  ssize_t ret = 0;
//
//  try
//  {
//#ifdef _DEBUG
//    if ( parent_volume.has_flag( Volume::FLAG_TRACE_FILE_IO ) )
//    {
//      parent_volume.get_log()->get_stream( Log::LOG_INFO ) <<
//        "xtreemfs::SharedFile::read( rbuf, size=" << size <<
//        ", offset=" << offset <<
//        " )";
//    }
//#endif
//
//    char *rbuf_start = static_cast<char*>( rbuf ),
//         *rbuf_p = static_cast<char*>( rbuf ),
//         *rbuf_end = static_cast<char*>( rbuf ) + size;
//    uint64_t current_file_offset = offset;
//    uint32_t stripe_size = xlocs.get_replicas()[0].get_striping_policy().
//                             get_stripe_size() * 1024;
//
//    yield::concurrency::auto_ResponseQueue<ReadResponse>
//      read_response_queue
//      (
//        new yield::concurrency::ResponseQueue<ReadResponse>
//      );
//    size_t expected_read_response_count = 0;
//
//    while ( rbuf_p < rbuf_end )
//    {
//      uint64_t object_number = current_file_offset / stripe_size;
//      uint32_t object_offset = current_file_offset % stripe_size;
//      size_t object_size = static_cast<size_t>( rbuf_end - rbuf_p );
//      if ( object_offset + object_size > stripe_size )
//        object_size = stripe_size - object_offset;
//
//#ifdef _DEBUG
//      if ( parent_volume.has_flag( Volume::FLAG_TRACE_FILE_IO ) )
//      {
//        parent_volume.get_log()->get_stream( Log::LOG_INFO ) <<
//          "xtreemfs::SharedFile: issuing read # " <<
//          ( expected_read_response_count + 1 ) <<
//          " for " << object_size <<
//          " bytes from object number " << object_number <<
//          " in file " << xcap.get_file_id() <<
//          " (object offset = " << object_offset <<
//          ", file offset = " << current_file_offset <<
//          ", remaining buffer size = " <<
//          static_cast<size_t>( rbuf_end - rbuf_p ) << ").";
//      }
//#endif
//
//      ReadRequest* read_request =
//        new ReadRequest
//        (
//          FileCredentials( xcap, xlocs ),
//          xcap.get_file_id(),
//          object_number,
//          0,
//          object_offset,
//          static_cast<uint32_t>( object_size ),
//          new ReadBuffer( rbuf_p, object_size )
//        );
//      read_request->set_response_target( read_response_queue->inc_ref() );
//      read_request->set_selected_file_replica( selected_file_replica );
//
//      parent_volume.get_osd_proxy_mux().send( *read_request );
//      expected_read_response_count++;
//
//      rbuf_p += object_size;
//      current_file_offset += object_size;
//    }
//
//#ifdef _DEBUG
//    if ( parent_volume.has_flag( Volume::FLAG_TRACE_FILE_IO ) )
//    {
//      parent_volume.get_log()->get_stream( Log::LOG_INFO ) <<
//        "xtreemfs::SharedFile: issued " << expected_read_response_count <<
//        " parallel reads.";
//    }
//#endif
//
//    for
//    (
//      size_t read_response_i = 0;
//      read_response_i < expected_read_response_count;
//      read_response_i++
//    )
//    {
//      ReadResponse& read_response = read_response_queue->dequeue();
//      // Object::dec_ref( read_response );
//      read_responses.push_back( &read_response );
//
//      yidl::runtime::auto_Buffer data( read_response.get_object_data().get_data() );
//      rbuf_p = static_cast<char*>( static_cast<void*>( *data ) );
//      uint32_t zero_padding = read_response.get_object_data().get_zero_padding();
//      if ( read_response.get_selected_file_replica() != 0 )
//        selected_file_replica = read_response.get_selected_file_replica();
//
//#ifdef _DEBUG
//      if ( parent_volume.has_flag( Volume::FLAG_TRACE_FILE_IO ) )
//      {
//        parent_volume.get_log()->get_stream( Log::LOG_INFO ) <<
//          "xtreemfs::SharedFile: read " << data->size() <<
//          " bytes from file " << xcap.get_file_id() <<
//          " with " << zero_padding << " bytes of zero padding" <<
//          ", starting from buffer offset " <<
//          static_cast<size_t>( rbuf_p - rbuf_start ) <<
//          ", read # " << ( read_response_i + 1 ) << " of " <<
//          expected_read_response_count << " parallel reads.";
//      }
//#endif
//
//      ret += data->size();
//
//      if ( zero_padding > 0 )
//      {
//        rbuf_p += data->size();
//
//        if ( rbuf_p + zero_padding <= rbuf_end )
//        {
//          memset( rbuf_p, 0, zero_padding );
//          ret += zero_padding;
//        }
//        else
//        {
//          parent_volume.get_log()->get_stream( Log::LOG_ERR ) <<
//            "xtreemfs::SharedFile: received zero_padding (data size=" <<
//            data->size() << ", zero_padding=" << zero_padding <<
//            ") larger than available buffer space (" <<
//            static_cast<size_t>( rbuf_end - rbuf_p ) << ")";
//
//#ifdef _WIN32
//          SetLastError( EIO );
//#else
//          errno = EIO;
//#endif
//          ret = -1;
//
//          break;
//        }
//      }
//    }
//
//#ifdef _DEBUG
//    if ( static_cast<size_t>( ret ) > size )
//      DebugBreak();
//
//    if ( parent_volume.has_flag( Volume::FLAG_TRACE_FILE_IO ) )
//    {
//      parent_volume.get_log()->get_stream( Log::LOG_INFO ) <<
//        "xtreemfs::SharedFile: read " << ret <<
//        " bytes from file " << xcap.get_file_id() <<
//        " in total, returning from read().";
//    }
//#endif
//  }
//  catch ( ProxyExceptionResponse& proxy_exception_response )
//  {
//    parent_volume.set_errno( "read", proxy_exception_response );
//    ret = -1;
//  }
//  catch ( std::exception& exc )
//  {
//    parent_volume.set_errno( "read", exc );
//    ret = -1;
//  }
//
//  for
//  (
//    vector<ReadResponse*>::iterator read_response_i = read_responses.begin();
//    read_response_i != read_responses.end();
//    read_response_i++
//  )
//    ReadResponse::dec_ref( **read_response_i );
//
//  return ret;
  return -1;
}

bool File::removexattr( const string& name )
{
  return parent_volume.removexattr( path, name );
}

bool File::setlk( bool exclusive, uint64_t offset, uint64_t length )
{
  FILE_OPERATION_BEGIN( setlk );

  DebugBreak();
  //Lock lock =
    //parent_volume.get_osd_proxy_mux().xtreemfs_lock_acquire
    //(
    //  FileCredentials( xcap, xlocs ),
    //  parent_volume.get_uuid(),
    //  yieldfs::FUSE::getpid(),
    //  xcap.get_file_id(),
    //  offset, length,
    //  exclusive
    //);

  //locks.push_back( lock );

  return true;

  FILE_OPERATION_END( setlk );
}

bool File::setlkw( bool exclusive, uint64_t offset, uint64_t length )
{
  FILE_OPERATION_BEGIN( setlkw );

  DebugBreak();
  //for ( ;; )
  //{
  //  try
  //  {
  //     //Lock lock =
  //        parent_volume.get_osd_proxy_mux().xtreemfs_lock_acquire
  //        (
  //          FileCredentials( xcap, xlocs ),
  //          parent_volume.get_uuid(),
  //          yieldfs::FUSE::getpid(),
  //          xcap.get_file_id(),
  //          offset,
  //          length,
  //          exclusive
  //        );

  //    //locks.push_back( lock );

  //    return true;
  //  }
  //  catch ( ProxyExceptionResponse& )
  //  { }
  //}

  FILE_OPERATION_END( setlkw );
}

bool File::setxattr( const string& name, const string& value, int flags )
{
  return parent_volume.setxattr( path, name, value, flags );
}

bool File::sync()
{
  FILE_OPERATION_BEGIN( sync );

  parent_volume.metadatasync( path, xcap );

  return true;

  FILE_OPERATION_END( sync );
}

bool File::truncate( uint64_t new_size )
{
  FILE_OPERATION_BEGIN( truncate );

  DebugBreak();
  //XCap truncate_xcap;
  //parent_volume.get_mrc_proxy().
  //  ftruncate( xcap, truncate_xcap );
  //xcap = truncate_xcap;

  //OSDWriteResponse osd_write_response;
  //parent_volume.get_osd_proxy_mux().truncate
  //(
  //  FileCredentials( xcap, xlocs ),
  //  xcap.get_file_id(),
  //  new_size,
  //  osd_write_response
  //);

  //if ( !osd_write_response.get_new_file_size().empty() )
  //{
  //  parent_volume.fsetattr
  //  (
  //    path,
  //    new Stat( osd_write_response ),
  //    yield::platform::Volume::SETATTR_SIZE,
  //    xcap
  //  );
  //}

  return true;

  FILE_OPERATION_END( truncate );
}

bool File::unlk( uint64_t offset, uint64_t length )
{
  //FILE_OPERATION_BEGIN( unlk );

  //if ( locks.empty() )
  //  return true;
  //else
  //{
  //  parent_volume.get_osd_proxy_mux().xtreemfs_lock_release
  //  (
  //    file_credentials,
  //    xcap.get_file_id(),
  //    locks.back()
  //  );

  //  locks.pop_back();

  //  return true;
  //}

  //FILE_OPERATION_END( unlk );

  return true;
}

ssize_t File::write( const void* wbuf, size_t size, uint64_t offset )
{
  ssize_t ret;
  vector<OSDInterfaceEvents::writeResponse*> write_responses;
  
#ifdef _DEBUG
  if ( parent_volume.has_flag( Volume::FLAG_TRACE_FILE_IO ) )
  {
    parent_volume.get_log()->get_stream( Log::LOG_INFO ) <<
      "xtreemfs::SharedFile::write( wbuf, size=" << size <<
      ", offset=" << offset << " )";
  }
#endif

  try
  {
    const char *wbuf_p = static_cast<const char*>( wbuf ),
               *wbuf_end = static_cast<const char*>( wbuf ) + size;
    uint64_t current_file_offset = offset;
    uint32_t stripe_size = xlocs.get_replicas()[0].get_striping_policy().
                             get_stripe_size() * 1024;

    size_t expected_write_response_count = 0;
    ResponseQueue<OSDInterfaceEvents::writeResponse> write_response_queue;    

    while ( wbuf_p < wbuf_end )
    {
      uint64_t object_number = current_file_offset / stripe_size;
      uint32_t object_offset = current_file_offset % stripe_size;
      uint64_t object_size = static_cast<size_t>( wbuf_end - wbuf_p );
      if ( object_offset + object_size > stripe_size )
        object_size = stripe_size - object_offset;

      yidl::runtime::auto_Object<OSDProxy> osd_proxy
        = parent_volume.get_osd_proxies().get_osd_proxy
          (
            object_number,
            selected_file_replica_i,
            xlocs
          );

      ObjectData object_data
      (
        0,
        false,
        0,
        new WriteBuffer( wbuf_p, static_cast<uint32_t>( object_size ) )
      );

      OSDInterfaceEvents::writeRequest* write_request =
        new OSDInterfaceEvents::writeRequest
        (
          FileCredentials( xcap, xlocs ),
          xcap.get_file_id(),
          object_number,
          0,
          object_offset,
          0,
          object_data
        );

      write_request->set_response_target( &write_response_queue );

#ifdef _DEBUG
      if ( parent_volume.has_flag( Volume::FLAG_TRACE_FILE_IO ) )
      {
        parent_volume.get_log()->get_stream( Log::LOG_INFO ) <<
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

      osd_proxy->send( *write_request );
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
      OSDInterfaceEvents::writeResponse& write_response =
        write_response_queue.dequeue();

#ifdef _DEBUG
      if ( parent_volume.has_flag( Volume::FLAG_TRACE_FILE_IO ) )
      {
        parent_volume.get_log()->get_stream( Log::LOG_INFO ) <<
          "xtreemfs::SharedFile::write received response # " <<
          ( write_response_i + 1 ) << " of " <<
          expected_write_response_count << ".";
      }
#endif

      if
      (
        write_response.get_osd_write_response() > latest_osd_write_response
      )
      {
#ifdef _DEBUG
        if ( parent_volume.has_flag( Volume::FLAG_TRACE_FILE_IO ) )
        {
          parent_volume.get_log()->get_stream( Log::LOG_INFO ) <<
            "xtreemfs::SharedFile: OSD write response is newer than latest known.";
        }
#endif

        latest_osd_write_response = write_response.get_osd_write_response();
      }

      // yidl::runtime::Object::dec_ref( write_response );
      write_responses.push_back( &write_response );
    }

    if ( !latest_osd_write_response.get_new_file_size().empty() )
    {
      parent_volume.fsetattr
      (
        path,
        latest_osd_write_response,
        yield::platform::Volume::SETATTR_SIZE,
        xcap
      );
    }

    ret = static_cast<ssize_t>( current_file_offset - offset );
  }
  catch ( ExceptionResponse& exception_response )
  {
    parent_volume.set_errno( "write", exception_response );
    ret = -1;
  }
  catch ( std::exception& exc )
  {
    parent_volume.set_errno( "write", exc );
    ret = -1;
  }

  for
  (
    vector<OSDInterfaceEvents::writeResponse*>::iterator
      write_response_i = write_responses.begin();
    write_response_i != write_responses.end();
    write_response_i++
   )
     OSDInterfaceEvents::writeResponse::dec_ref( **write_response_i );

  return ret;
}


//void OSDProxies::handlereadRequest( readRequest& req )
//{
//  OSDProxy& osd_proxy
//    = get_osd_proxy
//      (
//        req,
//        req.get_file_credentials(),
//        req.get_object_number()
//      );
//
//  req.set_response_target( new ReadResponseTarget( *this, req ) );
//
//  static_cast<EventTarget&>( osd_proxy ).send( req );
//
//  OSDProxy::dec_ref( osd_proxy );
//}
//
//void OSDProxies::handletruncateRequest( truncateRequest& req )
//{
//  const ReplicaSet& replicas
//    = req.get_file_credentials().get_xlocs().get_replicas();
//
//  if ( req.get_response_target() != NULL )
//  {
//    req.set_response_target
//    (
//      new TruncateResponseTarget( replicas.size(), *req.get_response_target() )
//    );
//  }
//
//  for
//  (
//    ReplicaSet::const_iterator replica_i = replicas.begin();
//    replica_i != replicas.end();
//    replica_i++
//  )
//  {
//    OSDProxy& osd_proxy = get_osd_proxy( ( *replica_i ).get_osd_uuids()[0] );
//    osd_proxy.send( req.inc_ref() );
//    OSDProxy::dec_ref( osd_proxy );
//  }
//
//  truncateRequest::dec_ref( req );
//}
//
//void OSDProxies::handleunlinkRequest( unlinkRequest& req )
//{
//  const ReplicaSet&
//    replicas = req.get_file_credentials().get_xlocs().get_replicas();
//
//  if ( req.get_response_target() != NULL )
//  {
//    req.set_response_target
//    (
//      new TruncateResponseTarget( replicas.size(), *req.get_response_target() )
//    );
//  }
//
//  for
//  (
//    ReplicaSet::const_iterator replica_i = replicas.begin();
//    replica_i != replicas.end();
//    replica_i++
//  )
//  {
//    OSDProxy& osd_proxy = get_osd_proxy( ( *replica_i ).get_osd_uuids()[0] );
//    osd_proxy.send( req.inc_ref() );
//    OSDProxy::dec_ref( osd_proxy );
//  }
//
//  unlinkRequest::dec_ref( req );
//}
//
//void OSDProxies::handlewriteRequest( writeRequest& req )
//{
//  OSDProxy& osd_proxy
//    = get_osd_proxy
//      (
//        req,
//        req.get_file_credentials(),
//        req.get_object_number()
//      );
//
//  osd_proxy.send( req );
//
//  OSDProxy::dec_ref( osd_proxy );
//}
//
//void OSDProxies::handlextreemfs_lock_acquireRequest
//(
//  xtreemfs_lock_acquireRequest& req
//)
//{
//  OSDProxy& osd_proxy
//    = get_osd_proxy
//      (
//        req,
//        req.get_file_credentials(),
//        0
//      );
//
//  osd_proxy.send( req );
//
//  OSDProxy::dec_ref( osd_proxy );
//}
//
//void OSDProxies::handlextreemfs_lock_checkRequest
//(
//  xtreemfs_lock_checkRequest& req
//)
//{
//  OSDProxy& osd_proxy
//    = get_osd_proxy
//      (
//        req,
//        req.get_file_credentials(),
//        0
//      );
//
//  osd_proxy.send( req );
//
//  OSDProxy::dec_ref( osd_proxy );
//}
//
//void OSDProxies::handlextreemfs_lock_releaseRequest
//(
//  xtreemfs_lock_releaseRequest& req
//)
//{
//  OSDProxy& osd_proxy
//    = get_osd_proxy
//      (
//        req,
//        req.get_file_credentials(),
//        0
//      );
//
//  osd_proxy.send( req );
//
//  OSDProxy::dec_ref( osd_proxy );
//}
