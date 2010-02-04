// Copyright 2009-2010 Minor Gordon.
// This source comes from the XtreemFS project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#include "nettest_proxy.h"
#include "xtreemfs/main.h"

#include <iostream>


// Constants
#define RATE_DEFAULT 1
#define NUM_CALLS_DEFAULT 1


namespace nettest_xtreemfs
{
  class Main : public xtreemfs::Main
  {
  public:
    Main()
      : xtreemfs::Main
        ( 
          "nettest.xtreemfs", 
          "test the network connection to an XtreemFS server",
          "[oncrpc://]<host>:port"
        )
    {
      addOption
      (
        NETTEST_XTREEMFS_OPTION_NUM_CALLS, 
        "-n",
        "--num-calls",
        "number of RPCs to send"        
      );
      num_calls = NUM_CALLS_DEFAULT;

      addOption
      (
        NETTEST_XTREEMFS_OPTION_RATE, 
        "-r",
        "--rate",
        "rate to send RPCs in RPCs/s"
      );
      rate = RATE_DEFAULT;

      addOption
      (
        NETTEST_XTREEMFS_OPTION_RECV_BUFFER, 
        "--recv-buffer",
        NULL,
        "receive buffers with the given size (power of 2, up to 2MB)"
      );

      addOption
      (
        NETTEST_XTREEMFS_OPTION_SEND_BUFFER, 
        "--send-buffer",
        NULL,
        "send buffers with the given size"
      );
    }

  private:
    enum
    {
      NETTEST_XTREEMFS_OPTION_NUM_CALLS = 20,
      NETTEST_XTREEMFS_OPTION_RATE = 21,
      NETTEST_XTREEMFS_OPTION_RECV_BUFFER = 22,
      NETTEST_XTREEMFS_OPTION_SEND_BUFFER = 23,
    };

    uint32_t num_calls;
    uint32_t rate;
    yidl::runtime::auto_Buffer recv_buffer;
    yidl::runtime::auto_Buffer send_buffer;
    YIELD::ipc::auto_URI uri;


    // YIELD::Main
    int _main( int, char** )
    {
      auto_NettestProxy 
        nettest_proxy
        ( 
          NettestProxy::create
          ( 
            *uri,
            NettestProxy::CONCURRENCY_LEVEL_DEFAULT,
            0,
            get_log(),
            get_operation_timeout(),
            NettestProxy::RECONNECT_TRIES_MAX_DEFAULT,
            get_proxy_ssl_context()
          ) 
        );         

      uint64_t sleep_ns 
        = static_cast<uint64_t>( ( 1.0 / static_cast<double>( rate ) ) 
          * static_cast<double>( NS_IN_S ) );

      for ( uint32_t call_i = 0; call_i < num_calls; call_i++ )
      {
        if ( recv_buffer != NULL )
          nettest_proxy->recv_buffer( recv_buffer->capacity(), recv_buffer );
        else if ( send_buffer != NULL )
          nettest_proxy->send_buffer( send_buffer );
        else
          nettest_proxy->nop();

        YIELD::platform::Thread::nanosleep( sleep_ns );
      }

      return 0;
    }

    void parseFiles( int files_count, char** files )
    {
      if ( files_count >= 1 )
      {
        uri = parseURI( files[0] );
        if ( uri->get_port() == 0 )
          throw YIELD::platform::Exception( "must specify a port" );
      }
      else
        throw YIELD::platform::Exception( "must specify a URI" );
    }

    void parseOption( int id, char* arg )
    {
      switch ( id )
      {
        case NETTEST_XTREEMFS_OPTION_NUM_CALLS:
        {
          num_calls = atoi( arg );
          if ( num_calls == 0 )
            num_calls = NUM_CALLS_DEFAULT;
        }
        break;

        case NETTEST_XTREEMFS_OPTION_RATE:
        {
          rate = atoi( arg );
          if ( rate == 0 )
            rate = RATE_DEFAULT;
        }
        break;

        case NETTEST_XTREEMFS_OPTION_RECV_BUFFER:
        {
          // arg should be a power of 2 up to 2MB
          // Then allocate recv_buffer
        }
        break;

        case NETTEST_XTREEMFS_OPTION_SEND_BUFFER:
        {
          uint32_t capacity = atoi( arg );
          if ( capacity > 0 )
          {
            send_buffer 
              = new yidl::runtime::HeapBuffer( capacity );
            for ( uint32_t byte_i = 0; byte_i < capacity; byte_i++ )
              send_buffer->put( "m", 1 );
          }
        }
        break;

        default:
        {
          xtreemfs::Main::parseOption( id, arg );
        }
        break;
      }
    }
  };
};

int main( int argc, char** argv )
{
  return nettest_xtreemfs::Main().main( argc, argv );
}
