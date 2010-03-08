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


#include "nettest_proxy.h"
#include "xtreemfs/main.h"

#include <iostream>


// Constants
#define RATE_DEFAULT 0
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
        "rate to send RPCs in RPCs/s (default = 0 = as fast as the server will go)"
      );
      rate = RATE_DEFAULT;

      addOption
      (
        NETTEST_XTREEMFS_OPTION_RECV_BUFFER,
        "--recv-buffer",
        NULL,
        "receive buffers with the given size (in K, power of 2, up to 2MB)"
      );

      addOption
      (
        NETTEST_XTREEMFS_OPTION_SEND_BUFFER,
        "--send-buffer",
        NULL,
        "send buffers with the given size (in K)"
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
    yield::ipc::auto_URI uri;


    // yield::Main
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

      uint64_t sleep_after_each_call_ns;
      if ( rate > 0 )
      {
        sleep_after_each_call_ns
          = static_cast<uint64_t>( ( 1.0 / static_cast<double>( rate ) )
            * static_cast<double>( yield::platform::Time::NS_IN_S ) );
      }
      else
        sleep_after_each_call_ns = 0;


      std::cout << "nettest: sending " << num_calls << " ";
      if ( recv_buffer != NULL )
        std::cout << "recv_buffer's";
      else if ( send_buffer != NULL )
        std::cout << "send_buffer's";
      else
        std::cout << "nop's";
      std::cout << ":" << std::endl;

      uint64_t io_total_kb = 0;
      yield::platform::Time rpc_time_total( static_cast<uint64_t>( 0 ) );
      yield::platform::Time start_wall_time;

      for ( uint32_t call_i = 0; call_i < num_calls; call_i++ )
      {
        yield::platform::Time rpc_time_start;

        if ( recv_buffer != NULL )
        {
          nettest_proxy->recv_buffer( recv_buffer->capacity(), recv_buffer );
          io_total_kb += recv_buffer->size() / 1024;
        }
        else if ( send_buffer != NULL )
        {
          nettest_proxy->send_buffer( send_buffer );
          io_total_kb += send_buffer->size() / 1024;
        }
        else
          nettest_proxy->nop();

        rpc_time_total += yield::platform::Time() - rpc_time_start;

        std::cout << "." << std::flush;

        if ( sleep_after_each_call_ns > 0 )
          yield::platform::Thread::nanosleep( sleep_after_each_call_ns );
      }

      yield::platform::Time end_wall_time;
      yield::platform::Time wall_time_total( end_wall_time - start_wall_time );

      std::cout << std::endl << std::endl;

      std::cout << "Elapsed wall time: " <<
         wall_time_total.as_unix_time_ms() << "ms" << std::endl;

      std::cout << "Elapsed time spent in RPCs: " <<
        rpc_time_total.as_unix_time_ms() << "ms" << std::endl;

      std::cout << "Average time per RPC: " <<
        ( rpc_time_total.as_unix_time_ms() / 
          static_cast<double>( num_calls ) ) <<
        "ms" << std::endl;

      double io_total_mb = static_cast<double>( io_total_kb ) / 1024.0;
      std::cout << "MB transferred: " << io_total_mb << std::endl;
/*
      double kb_per_s
        = static_cast<double>( io_total_kb ) / wall_time_total_s;
      std::cout << "KB/s: " << kb_per_s << std::endl;
*/
      double mb_per_s
        = static_cast<double>( io_total_kb ) / 1024.0 
          / wall_time_total.as_unix_time_s();
      std::cout << "MB/s: " << mb_per_s << std::endl;

      return 0;
    }

    void parseFiles( int files_count, char** files )
    {
      if ( files_count >= 1 )
      {
        uri = parseURI( files[0] );
        if ( uri->get_port() == 0 )
          throw yield::platform::Exception( "must specify a port" );
      }
      else
        throw yield::platform::Exception( "must specify a URI" );
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
          uint32_t capacity_kb = atoi( arg );
          if ( capacity_kb > 0 )
          {
            // Round capacity up to the nearest power of 2
            uint32_t rounded_capacity_kb = 1;
            while ( rounded_capacity_kb < capacity_kb )
              rounded_capacity_kb <<= 1;

            recv_buffer
              = new yidl::runtime::HeapBuffer( rounded_capacity_kb * 1024 );
          }
        }
        break;

        case NETTEST_XTREEMFS_OPTION_SEND_BUFFER:
        {
          uint32_t capacity_kb = atoi( arg );
          if ( capacity_kb > 0 )
          {
            send_buffer
              = new yidl::runtime::HeapBuffer( capacity_kb * 1024 );
            for ( uint32_t byte_i = 0; byte_i < capacity_kb * 1024; byte_i++ )
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
