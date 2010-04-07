// Copyright (c) 2010 Minor Gordon
// With original implementations and ideas contributed by Felix Hupfeld
// All rights reserved
// 
// This source file is part of the Yield project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the Yield project nor the
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


#ifndef _YIELD_PLATFORM_CHANNEL_TEST_H_
#define _YIELD_PLATFORM_CHANNEL_TEST_H_

#include "yield/platform.h"
#include "yunit.h"


namespace yield
{
  namespace platform
  {
    class ChannelTestCase
      : public yunit::TestCase,
        public Channel::AIOReadCallback,
        public Channel::AIOWriteCallback
    {
    public:
      void wait_for_aio()
      {
        wait_for_aio_lock.acquire();

        if ( last_error_code != 0 )
          throw Exception( last_error_code );

        if ( last_read_buffer != NULL )
          check_read( last_read_buffer->size(), *last_read_buffer );
      }

    protected:
      ChannelTestCase( const string& name )
        : yunit::TestCase( name )
      {
        last_error_code = 0;
        last_read_buffer = NULL;
      }

      void
      check_read
      ( 
        ssize_t read_ret, 
        Buffer& read_buffer 
      )
      {
        if ( read_ret < 0 )
          throw Exception();
        else
        {
          ASSERT_EQUAL( static_cast<size_t>( read_ret ), 512 );
          ASSERT_EQUAL( read_buffer.size(), 512 );
        }
      }

      void check_write( ssize_t write_ret )
      {
        if ( write_ret < 0 )
          throw Exception();
        else
        {
          ASSERT_EQUAL( static_cast<size_t>( write_ret ), 512 );
        }
      }

      Buffer& get_read_buffer() const
      {
        return *new StringBuffer( 1024 );
      }

      Buffer& get_write_buffer() const
      {
        Buffer* buffer = new StringBuffer( 1024 );
        buffer->resize( 512 );
        return *buffer;
      }

      Buffers& get_write_buffers() const
      {
        return *new Buffers( get_write_buffer() );
      }

    private:
      // Channel::AIOReadCallback
      void onReadCompletion( Buffer& buffer, void* )
      {
        last_read_buffer = &buffer.inc_ref();
        wait_for_aio_lock.release();
      }

      void onReadError( uint32_t error_code, void* )
      {
        last_error_code = error_code;
        wait_for_aio_lock.release();
      }

      // Channel::AIOWriteCallback
      void onWriteCompletion( size_t, void* )
      {
        wait_for_aio_lock.release();
      }

      void onWriteError( uint32_t error_code, void* )
      {
        last_error_code = error_code;
        wait_for_aio_lock.release();
      }

    private:
      Buffer* last_read_buffer;
      uint32_t last_error_code;
      Semaphore wait_for_aio_lock;
    };
  };
};

#endif
