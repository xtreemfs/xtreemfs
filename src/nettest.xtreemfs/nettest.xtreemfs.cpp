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
#include "xtreemfs.h"
using namespace org::xtreemfs::interfaces;
using namespace xtreemfs;

#include "yidl.h"
using yidl::runtime::auto_Object;
using yidl::runtime::Buffer;

#include "yield.h"
using yield::platform::Thread;
using yield::platform::HeapBuffer;


// Constants
#define RATE_DEFAULT 0
#define NUM_CALLS_DEFAULT 1


int main( int argc, char** argv )
{
  OptionParser::Options nettest_options;

  nettest_options.add( "-n", "number of RPCs to send" );
  nettest_options.add( "--num-calls", "number of RPCs to send" );

  nettest_options.add
  ( 
    "-r", 
    "rate to send RPCs in RPCs/s (default = 0 = as fast as the server will go)"
  );
  nettest_options.add
  ( 
    "--rate", 
    "rate to send RPCs in RPCs/s (default = 0 = as fast as the server will go)"
  );

  nettest_options.add
  (
    "--recv-buffer",
    "receive buffers with the given size (in K, power of 2, up to 2MB)"
  );

  nettest_options.add
  (
    "--send-buffer",
    "send buffers with the given size (in K)"
  );


  if ( argc == 1 )
  {
    cout << "nettest.xtreemfs:" <<
            " test the network connection to an XtreemFS server" << endl;
    cout << "Usage: nettest.xtreemfs <options>" <<
            " [oncrpc://]<host>[:port]" << endl;
    cout << Options::usage( nettest_options );
    return 0;
  }

  try
  {
    uint32_t num_calls = NUM_CALLS_DEFAULT;
    uint32_t rate = RATE_DEFAULT;
    
    Options options = Options::parse( argc, argv, nettest_options );

    auto_Object<NettestProxy> nettest_proxy = NettestProxy::create( options );
    
    for 
    (
      Options::const_iterator parsed_option_i = options.begin();
      parsed_option_i != options.end();
      ++parsed_option_i
    )
    {
      const OptionParser::ParsedOption& popt = *parsed_option_i;

      if ( popt == "-n" || popt == "--num-calls" )
      {
        num_calls = atoi( popt.get_argument().c_str() );
        if ( num_calls == 0 )
          num_calls = NUM_CALLS_DEFAULT;
      }
      else if ( popt == "-r" || popt == "--rate" )
      {
        rate = atoi( popt.get_argument().c_str() );
        if ( rate == 0 )
          rate = RATE_DEFAULT;
      }
    }


    uint64_t sleep_after_each_call_ns;
    if ( rate > 0 )
    {
      sleep_after_each_call_ns
        = static_cast<uint64_t>( ( 1.0 / static_cast<double>( rate ) )
          * static_cast<double>( Time::NS_IN_S ) );
    }
    else
      sleep_after_each_call_ns = 0;


    uint64_t io_total_kb = 0;
    Time rpc_time_total( static_cast<uint64_t>( 0 ) );
    Time start_wall_time;

    for 
    (
      Options::const_iterator parsed_option_i = options.begin();
      parsed_option_i != options.end();
      ++parsed_option_i
    )
    {
      const OptionParser::ParsedOption& popt = *parsed_option_i;

      if ( popt == "--recv-buffer" )
      {
        uint32_t capacity_kb = atoi( popt.get_argument().c_str() );
        if ( capacity_kb > 0 )
        {
          // Round capacity up to the nearest power of 2
          uint32_t rounded_capacity_kb = 1;
          while ( rounded_capacity_kb < capacity_kb )
            rounded_capacity_kb <<= 1;
          capacity_kb = rounded_capacity_kb;
        }
        else
          capacity_kb = 1;

        auto_Object<Buffer> recv_buffer = new HeapBuffer( capacity_kb * 1024 );

        cout << "nettest: sending " << num_calls << " recv_buffers's:" << endl;

        start_wall_time = Time();

        for ( uint32_t call_i = 0; call_i < num_calls; call_i++ )
        {
          Time rpc_time_start;

          nettest_proxy->recv_buffer
          ( 
            recv_buffer->capacity(),
            &recv_buffer.get()
          );

          io_total_kb += recv_buffer->size() / 1024;

          rpc_time_total += Time() - rpc_time_start;

          cout << "." << std::flush;

          if ( sleep_after_each_call_ns > 0 )
            Thread::nanosleep( sleep_after_each_call_ns );
        }
      }
      else if ( popt == "--send-buffer" )
      {
        uint32_t capacity_kb = atoi( popt.get_argument().c_str() );
        if ( capacity_kb == 0 )
          capacity_kb = 1;

        auto_Object<Buffer> send_buffer = new HeapBuffer( capacity_kb * 1024 );
        for ( uint32_t byte_i = 0; byte_i < capacity_kb * 1024; byte_i++ )
          send_buffer->put( "m", 1 );

        cout << "nettest: sending " << num_calls << " send_buffer's:" << endl;

        start_wall_time = Time();

        for ( uint32_t call_i = 0; call_i < num_calls; call_i++ )
        {
          Time rpc_time_start;

          nettest_proxy->send_buffer( &send_buffer.get() );
          io_total_kb += send_buffer->size() / 1024;

          rpc_time_total += Time() - rpc_time_start;

          cout << "." << std::flush;

          if ( sleep_after_each_call_ns > 0 )
            Thread::nanosleep( sleep_after_each_call_ns );
        }
      }
      else
      {
        cout << "nettest: sending " << num_calls << " nop's:" << endl;

        start_wall_time = Time();

        for ( uint32_t call_i = 0; call_i < num_calls; call_i++ )
        {
          Time rpc_time_start;

          nettest_proxy->nop();

          rpc_time_total += Time() - rpc_time_start;

          cout << "." << std::flush;

          if ( sleep_after_each_call_ns > 0 )
            Thread::nanosleep( sleep_after_each_call_ns );
        }
      }
    }

    Time end_wall_time;
    Time wall_time_total( end_wall_time - start_wall_time );

    cout << endl << endl;

    cout << "Elapsed wall time: " <<
       wall_time_total.as_unix_time_ms() << "ms" << endl;

    cout << "Elapsed time spent in RPCs: " <<
      rpc_time_total.as_unix_time_ms() << "ms" << endl;

    cout << "Average time per RPC: " <<
      ( rpc_time_total.as_unix_time_ms() /
        static_cast<double>( num_calls ) ) <<
      "ms" << endl;

    double io_total_mb = static_cast<double>( io_total_kb ) / 1024.0;
    cout << "MB transferred: " << io_total_mb << endl;
/*
    double kb_per_s
      = static_cast<double>( io_total_kb ) / wall_time_total_s;
    cout << "KB/s: " << kb_per_s << endl;
*/
    double mb_per_s
      = static_cast<double>( io_total_kb ) / 1024.0
        / wall_time_total.as_unix_time_s();
    cout << "MB/s: " << mb_per_s << endl;

    return 0;
  }
  catch ( Exception& exception )
  {
    cerr << "nettest.xtreemfs: error: " << exception.what() << endl;
    return exception.get_error_code();      
  }
}
