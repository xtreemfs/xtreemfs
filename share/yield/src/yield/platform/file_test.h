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


#ifndef _YIELD_PLATFORM_FILE_TEST_H_
#define _YIELD_PLATFORM_FILE_TEST_H_

#include "channel_test.h"


namespace yield
{
  namespace platform
  {
    class FileTestCase : public ChannelTestCase
    {
    public:
      FileTestCase( const string& name, Volume* volume = NULL )
        : ChannelTestCase( name ),
          test_xattr_name( "test_xattr_name" ),
          test_xattr_value( "test_xattr_value" )
      {
        file = NULL;

        if ( volume != NULL )
          this->volume = &volume->inc_ref();
        else
          this->volume = new Volume;
      }

      virtual ~FileTestCase()
      {
        Volume::dec_ref( *volume );
      }

      // yunit::TestCase
      void setUp()
      {
        volume->unlink( "file_test.txt" );
      }

      void tearDown()
      {
        File::dec_ref( file );
        volume->unlink( "file_test.txt" );
      }

    protected:
      File& get_file( uint32_t open_flags = O_CREAT|O_TRUNC|O_RDWR ) 
      {
        if ( file == NULL )
        {
          file = volume->open( "file_test.txt", open_flags );
          if ( file == NULL ) throw Exception();
        }

        return *file; 
      }

      const string& get_test_xattr_name() const { return test_xattr_name; }
      const string& get_test_xattr_value() const { return test_xattr_value; }

      bool set_test_xattr()
      {
        return get_file().setxattr( test_xattr_name, test_xattr_value, 0 );
      }

    private:
      File* file;
      string test_xattr_name, test_xattr_value;
      Volume* volume;
    };

#define YIELD_PLATFORM_FILE_TEST_CASE( TestCaseName ) \
    class File_##TestCaseName##Test : public FileTestCase \
    { \
    public:\
      File_##TestCaseName##Test( yield::platform::Volume* volume = NULL ) \
        : FileTestCase( "File_" # TestCaseName "Test", volume ) \
      { } \
      void runTest(); \
    };\
      inline void File_##TestCaseName##Test::runTest()


    YIELD_PLATFORM_FILE_TEST_CASE( aio_read_bio )
    {
      auto_Object<BIOQueue> bio_queue( BIOQueue::create() );
      if ( !get_file().associate( *bio_queue ) ) throw Exception();

      auto_Object<Buffer> write_buffer( get_write_buffer() );
      ssize_t write_ret = get_file().write( *write_buffer, 0 );
      check_write( write_ret );

      get_file().aio_read( get_read_buffer(), 0, *this );

      wait_for_aio();
    }

    YIELD_PLATFORM_FILE_TEST_CASE( aio_read_no_io_queue )
    {
      auto_Object<Buffer> write_buffer( get_write_buffer() );
      ssize_t write_ret = get_file().write( *write_buffer, 0 );
      check_write( write_ret );

      auto_Object<Buffer> read_buffer( get_read_buffer() );
      get_file().aio_read( read_buffer->inc_ref(), 0, *this );
      check_read( read_buffer->size(), *read_buffer );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( aio_write_bio )
    {
      auto_Object<BIOQueue> bio_queue( BIOQueue::create() );
      if ( !get_file().associate( *bio_queue ) ) throw Exception();

      get_file().aio_write( get_write_buffer(), 0, *this );
      wait_for_aio();

      auto_Object<Buffer> read_buffer( get_read_buffer() );  
      ssize_t read_ret = get_file().read( *read_buffer, 0 );
      check_read( read_ret, *read_buffer );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( aio_write_no_io_queue )
    {
      get_file().aio_write( get_write_buffer(), 0, *this );

      auto_Object<Buffer> read_buffer( get_read_buffer() );  
      ssize_t read_ret = get_file().read( *read_buffer, 0 );
      check_read( read_ret, *read_buffer );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( close )
    {
      if ( !get_file().close() ) throw Exception();
      ASSERT_FALSE( get_file().close() );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( datasync )
    {
      auto_Object<Buffer> write_buffer( get_write_buffer() );
      ssize_t write_ret = get_file().write( *write_buffer, 0 );
      check_write( write_ret );
      if ( !get_file().datasync() ) throw Exception();
      ASSERT_TRUE( get_file().getattr()->get_size() >= write_buffer->size() );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( getpagesize )
    {
      size_t pagesize = get_file().getpagesize();
      ASSERT_EQUAL( pagesize % 2, 0 );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( getattr )
    {
      Stat* stbuf = get_file().getattr();
      if ( stbuf == NULL ) throw Exception();
      ASSERT_TRUE( stbuf->ISREG() );
      ASSERT_EQUAL( stbuf->get_size(), 0 );
      ASSERT_NOTEQUAL( stbuf->get_atime(), static_cast<uint64_t>( 0 ) );
      ASSERT_NOTEQUAL( stbuf->get_mtime(), static_cast<uint64_t>( 0 ) );
      ASSERT_NOTEQUAL( stbuf->get_ctime(), static_cast<uint64_t>( 0 ) );
      Stat::dec_ref( *stbuf );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( getxattr )
    {
      if ( set_test_xattr() )
      {
        string xattr_value;
        get_file().getxattr( get_test_xattr_name(), xattr_value );
        ASSERT_EQUAL( xattr_value, get_test_xattr_value() );
      }
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
      else if ( errno != ENOTSUP )
        throw Exception();
#endif
    }

    YIELD_PLATFORM_FILE_TEST_CASE( listxattr )
    {
      if ( set_test_xattr() )
      {
        vector<string> names;
        get_file().listxattr( names );
        ASSERT_TRUE( names.size() >= 1 );
        for
        (
          vector<string>::const_iterator name_i = names.begin();
          name_i != names.end();
          name_i++
        )
        {
          if ( *name_i == get_test_xattr_name() )
            return;
        }
        FAIL();
      }
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
      else if ( errno != ENOTSUP )
        throw Exception();
#endif
    }

    YIELD_PLATFORM_FILE_TEST_CASE( operatorint )
    {
#ifdef _WIN32
      static_cast<void*>( get_file() );
#else
      static_cast<int>( get_file() );
#endif
    }

    YIELD_PLATFORM_FILE_TEST_CASE( read )
    {
      auto_Object<Buffer> write_buffer( get_write_buffer() );
      ssize_t write_ret = get_file().write( *write_buffer, 0 );
      check_write( write_ret );

      for ( uint8_t read_i = 0; read_i < 8; read_i++ )
      {
        auto_Object<Buffer> read_buffer( get_read_buffer() );
        ssize_t read_ret = get_file().read( *read_buffer, 0 );
        check_read( read_ret, *read_buffer );
      }
    }

    YIELD_PLATFORM_FILE_TEST_CASE( removexattr )
    {
      if ( set_test_xattr() )
      {
        if ( !get_file().removexattr( get_test_xattr_name() ) )
          throw Exception();
      }
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
      else if ( errno != ENOTSUP )
        throw Exception();
#endif
    }

    YIELD_PLATFORM_FILE_TEST_CASE( setlk )
    {
      if ( !get_file().setlk( true, 0, 256 ) ) throw Exception();
    }

    YIELD_PLATFORM_FILE_TEST_CASE( setlkw )
    {
      if ( !get_file().setlkw( true, 0, 256 ) ) throw Exception();
    }

    YIELD_PLATFORM_FILE_TEST_CASE( setxattr )
    {
      if ( set_test_xattr() )
      {
        string xattr_value;
        get_file().getxattr( get_test_xattr_name(), xattr_value );
        ASSERT_EQUAL( xattr_value, get_test_xattr_value() );
      }
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
      else if ( errno != ENOTSUP )
        throw Exception();
#endif
    }

    YIELD_PLATFORM_FILE_TEST_CASE( sync )
    {
      auto_Object<Buffer> write_buffer( get_write_buffer() );
      ssize_t write_ret = get_file().write( *write_buffer, 0 );
      check_write( write_ret );
      if ( !get_file().sync() ) throw Exception();
      ASSERT_TRUE( get_file().getattr()->get_size() >= write_buffer->size() );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( truncate )
    {
      auto_Object<Buffer> write_buffer( get_write_buffer() );
      ssize_t write_ret = get_file().write( *write_buffer, 0 );
      check_write( write_ret );
      if ( !get_file().sync() ) throw Exception();
      ASSERT_TRUE( get_file().getattr()->get_size() >= write_buffer->size() );
      if ( !get_file().truncate( 0 ) ) throw Exception();
      if ( !get_file().sync() ) throw Exception();
      ASSERT_EQUAL( get_file().getattr()->get_size(), 0 );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( unlk )
    {
      if ( !get_file().setlkw( true, 0, 256 ) )
        throw Exception();
#ifndef _WIN32
      // getlk will not be true because we're using the same pid
      // as the one that acquired the lock
      if ( get_file().getlk( true, 0, 256 ) )
        throw Exception();
#endif
      if ( !get_file().unlk( 0, 256 ) )
        throw Exception();
    }


    class FileTestSuite : public yunit::TestSuite
    {
    public:
      FileTestSuite( const string& name, Volume* volume = NULL )
        : TestSuite( name )
      {
        if ( volume == NULL )
          volume = new Volume;

        addTest( new File_aio_read_bioTest( volume ) );
        addTest( new File_aio_read_no_io_queueTest( volume ) );
        addTest( new File_aio_write_bioTest( volume ) );
        addTest( new File_aio_write_no_io_queueTest( volume ) );
        addTest( new File_closeTest( volume ) );
        addTest( new File_datasyncTest( volume ) );
        addTest( new File_getpagesizeTest( volume ) );
        addTest( new File_getattrTest( volume ) );
        addTest( new File_getxattrTest( volume  ) );
        addTest( new File_listxattrTest( volume ) );
        addTest( new File_operatorintTest( volume ) );
        addTest( new File_readTest( volume ) );
        addTest( new File_removexattrTest( volume ) );
        addTest( new File_setlkTest( volume ) );
        addTest( new File_setlkwTest( volume ) );
        addTest( new File_setxattrTest( volume ) );
        addTest( new File_syncTest( volume ) );
        addTest( new File_truncateTest( volume ) );
        addTest( new File_unlkTest( volume ) );

        Volume::dec_ref( *volume );
      }
    };
  };
};


#endif
