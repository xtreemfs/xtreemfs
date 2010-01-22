// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "open_file.h"
using namespace org::xtreemfs::interfaces;
using namespace xtreemfs;


class OpenFile::XCapTimer : public YIELD::platform::TimerQueue::Timer
{
public:
  XCapTimer( auto_OpenFile open_file, const YIELD::platform::Time& timeout )
    : YIELD::platform::TimerQueue::Timer( timeout ),
      open_file( open_file )
  { }

  XCapTimer& operator=( XCapTimer& )
  {
    return *this;
  }

  // YIELD::platform::TimerQueue::Timer
  void fire()
  {
    if ( !open_file->closed ) // See note in OpenFile::OpenFile
    {                         // re: the rationale for this
      try
      {
        XCap renewed_xcap;

        //open_file->parent_volume->get_log()->
        //  getStream( YIELD::platform::Log::LOG_INFO ) << 
        //  "xtreemfs::OpenFile: renewing XCap for file " << 
        //  open_file->file_credentials.get_xcap().get_file_id() << ".";

        open_file->parent_shared_file->get_parent_volume()->get_mrc_proxy()
          ->xtreemfs_renew_capability
        ( 
          open_file->file_credentials.get_xcap(),
          renewed_xcap      
        );

        //open_file->parent_volume->get_log()->
        //  getStream( YIELD::platform::Log::LOG_INFO ) << 
        //  "xtreemfs::OpenFile: successfully renewed XCap for open_file " <<
        //  open_file->file_credentials.get_xcap().get_file_id() << ".";

        open_file->file_credentials.set_xcap( renewed_xcap );

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
              open_file,
              ( renewed_xcap.get_expire_timeout_s() - 
                XCAP_EXPIRE_TIMEOUT_S_MIN ) 
              * NS_IN_S
            )
          );
        }
        //else 
        //  open_file->parent_volume->get_log()->
        //    getStream( YIELD::platform::Log::LOG_ERR ) <<
        //      "xtreemfs::OpenFile: received xcap for file " << 
        //      renewed_xcap.get_file_id() <<
        //      "that expires in less than " << 
        //      XCAP_EXPIRE_TIMEOUT_S_MIN << 
        //      " seconds, will not try to renew.";
      }
      catch ( std::exception& )
      {
        //open_file->parent_volume->get_log()->
        //  getStream( YIELD::platform::Log::LOG_ERR ) << 
        //  "xtreemfs::OpenFile: caught exception trying to renew XCap for file " <<
        //  open_file->file_credentials.get_xcap().get_file_id() << 
        //  ": " << exc.what() << ".";
      }
    }
  }

private:
  auto_OpenFile open_file;
};


OpenFile::OpenFile
( 
  const FileCredentials& file_credentials,
  auto_SharedFile parent_shared_file
)
: file_credentials( file_credentials ),
  parent_shared_file( parent_shared_file )
{
  closed = false;

  if 
  (
    file_credentials.get_xcap().get_expire_timeout_s() >
    XCAP_EXPIRE_TIMEOUT_S_MIN
  )
  {
    // Do not keep a reference to the xcap timer, since that would create
    // circular references with this object
    // Instead the timer will always fire, check whether the OpenFile is closed,
    // and take the last reference to the OpenFile with it
    // That means that the OpenFile will not be deleted until the xcap expires!
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
  //else 
  //  parent_volume->get_log()->getStream( YIELD::platform::Log::LOG_ERR ) <<
  //    "xtreemfs::OpenFile: received xcap that expires in less than " <<
  //    XCAP_EXPIRE_TIMEOUT_S_MIN << 
  //    " seconds, will not try to renew.";
}

OpenFile::~OpenFile()
{
  close();
}

bool OpenFile::close()
{
  if ( !closed )
  {
    //try
    //{
    //  if ( !latest_osd_write_response.get_new_file_size().empty() )  
    //  {
    //    parent_shared_file->get_parent_volume()->get_mrc_proxy()
    //      ->xtreemfs_update_file_size
    //    ( 
    //      file_credentials.get_xcap(), 
    //      latest_osd_write_response 
    //    );

    //    latest_osd_write_response.set_new_file_size( NewFileSize() );
    //  }
    //    
    //  if ( file_credentials.get_xcap().get_replicate_on_close() )
    //  {
    //    parent_shared_file->get_parent_volume()->get_mrc_proxy()
    //      ->close
    //    ( 
    //      parent_volume->get_vivaldi_coordinates(), 
    //      file_credentials.get_xcap() 
    //    );
    //  }
    //}
    //catch ( std::exception& )
    //{ }

    closed = true;
  }

  return true; // ??
}

bool OpenFile::datasync()
{
  return sync();
}

YIELD::platform::auto_Stat OpenFile::getattr()
{
  return parent_shared_file->getattr();
}

bool OpenFile::getlk( bool exclusive, uint64_t offset, uint64_t length )
{
  return parent_shared_file->getlk
         (
           file_credentials,
           exclusive, 
           offset,
           length
         );
}

size_t OpenFile::getpagesize()
{
  return file_credentials.get_xlocs().get_replicas()[0]
    .get_striping_policy().get_stripe_size() * 1024;
}

bool OpenFile::getxattr( const std::string& name, std::string& out_value )
{
  return parent_shared_file->getxattr( name, out_value );
}

bool OpenFile::listxattr( std::vector<std::string>& out_names )
{
  return parent_shared_file->listxattr( out_names );
}

ssize_t OpenFile::read( void* rbuf, size_t size, uint64_t offset )
{
  return parent_shared_file->read( file_credentials, rbuf, size, offset );
}

bool OpenFile::removexattr( const std::string& name )
{
  return parent_shared_file->removexattr( name );
}

bool OpenFile::setlk( bool exclusive, uint64_t offset, uint64_t length )
{
  return parent_shared_file->setlk
         ( 
           file_credentials, 
           exclusive, 
           offset, 
           length 
         );
}

bool OpenFile::setlkw( bool exclusive, uint64_t offset, uint64_t length )
{
  return parent_shared_file->setlkw
         ( 
           file_credentials, 
           exclusive, 
           offset, 
           length 
         );
}

bool OpenFile::setxattr
( 
  const std::string& name, 
  const std::string& value, 
  int flags 
)
{
  return parent_shared_file->setxattr( name, value, flags );
}

bool OpenFile::sync()
{
  return parent_shared_file->sync( file_credentials );
}

bool OpenFile::truncate( uint64_t new_size )
{
  return parent_shared_file->truncate( file_credentials, new_size );
}

bool OpenFile::unlk( uint64_t offset, uint64_t length )
{
  return parent_shared_file->unlk( file_credentials, offset, length );
}

ssize_t OpenFile::write( const void* wbuf, size_t size, uint64_t offset )
{
  return parent_shared_file->write( file_credentials, wbuf, size, offset );
}

