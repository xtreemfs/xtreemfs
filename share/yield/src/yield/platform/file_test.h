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

#include "yield/platform.h"
#include "yunit.h"


#define YIELD_PLATFORM_FILE_TEST_FILE_NAME "file_test.txt"
#define YIELD_PLATFORM_FILE_TEST_FILE_OPEN_FLAGS O_CREAT|O_TRUNC|O_RDWR
#define YIELD_PLATFORM_FILE_TEST_STRING "file_test"
#define YIELD_PLATFORM_FILE_TEST_STRING_LEN 11
#define YIELD_PLATFORM_FILE_TEST_XATTR_NAME "file_test_xattr_name"
#define YIELD_PLATFORM_FILE_TEST_XATTR_VALUE "file_test_xattr_value"


namespace YIELD
{
  namespace platform
  {
    class FileTestCase : public yunit::TestCase
    {
    public:
      FileTestCase
      (
        const std::string& name,
        YIELD::platform::auto_Volume volume = NULL
      )
        : yunit::TestCase( name )
      {
        if ( volume != NULL )
          this->volume = volume;
        else
          this->volume = new Volume;
      }

      virtual ~FileTestCase()
      { }

      FileTestCase& operator=( const FileTestCase& ) 
      { 
        return *this; 
      }

      void setUp()
      {
        tearDown();

        file = volume->open
              (
                YIELD_PLATFORM_FILE_TEST_FILE_NAME,
                YIELD_PLATFORM_FILE_TEST_FILE_OPEN_FLAGS
              );

        if ( file == NULL )
          throw Exception();
      }

      void tearDown()
      {
        file.reset( NULL );
        volume->unlink( YIELD_PLATFORM_FILE_TEST_FILE_NAME );
      }

    protected:
      auto_File get_file() const { return file; }

    private:
      YIELD::platform::auto_Volume volume;
      auto_File file;
    };

#define YIELD_PLATFORM_FILE_TEST_CASE( TestCaseName ) \
    class File_##TestCaseName##Test : public FileTestCase \
    { \
    public:\
      File_##TestCaseName##Test( YIELD::platform::auto_Volume volume = NULL ) \
        : FileTestCase( "File_" # TestCaseName "Test", volume ) \
      { } \
      void runTest(); \
    };\
      inline void File_##TestCaseName##Test::runTest()


    YIELD_PLATFORM_FILE_TEST_CASE( close )
    {
      if ( !get_file()->close() )
        throw Exception();
      ASSERT_FALSE( get_file()->close() );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( datasync )
    {
      get_file()->write
      (
        YIELD_PLATFORM_FILE_TEST_STRING,
        YIELD_PLATFORM_FILE_TEST_STRING_LEN,
        0
      );

      if ( !get_file()->datasync() )
        throw Exception();

      ASSERT_TRUE
      (
        get_file()->getattr()->get_size()
          >= YIELD_PLATFORM_FILE_TEST_STRING_LEN
      );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( getpagesize )
    {
      size_t pagesize = get_file()->getpagesize();
      ASSERT_EQUAL( pagesize % 2, 0 );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( getattr )
    {
      auto_Stat stbuf = get_file()->getattr();
      ASSERT_TRUE( stbuf->ISREG() );
      ASSERT_EQUAL( stbuf->get_size(), 0 );
      ASSERT_NOTEQUAL( stbuf->get_atime(), static_cast<uint64_t>( 0 ) );
      ASSERT_NOTEQUAL( stbuf->get_mtime(), static_cast<uint64_t>( 0 ) );
      ASSERT_NOTEQUAL( stbuf->get_ctime(), static_cast<uint64_t>( 0 ) );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( getxattr )
    {
      if
      (
        get_file()->setxattr
        (
          YIELD_PLATFORM_FILE_TEST_XATTR_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_VALUE,
          0
        )
      )
      {
        std::string value;
        get_file()->getxattr( YIELD_PLATFORM_FILE_TEST_XATTR_NAME, value );
        ASSERT_EQUAL( value, YIELD_PLATFORM_FILE_TEST_XATTR_VALUE );
      }
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
      else if ( errno != ENOTSUP )
        throw Exception();
#endif
    }

    YIELD_PLATFORM_FILE_TEST_CASE( listxattr )
    {
      if
      (
        get_file()->setxattr
        (
          YIELD_PLATFORM_FILE_TEST_XATTR_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_VALUE,
          0
        )
      )
      {
        std::vector<std::string> names;
        get_file()->listxattr( names );
        ASSERT_TRUE( names.size() >= 1 );
        for
        (
          std::vector<std::string>::const_iterator name_i = names.begin();
          name_i != names.end();
          name_i++
        )
        {
          if ( *name_i == YIELD_PLATFORM_FILE_TEST_XATTR_NAME )
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
      static_cast<void*>( *get_file() );
#else
      static_cast<int>( *get_file() );
#endif
    }

    YIELD_PLATFORM_FILE_TEST_CASE( read )
    {
      ssize_t bytes_written
        = get_file()->write
          (
            YIELD_PLATFORM_FILE_TEST_STRING,
            YIELD_PLATFORM_FILE_TEST_STRING_LEN,
            0
          );
      if ( bytes_written <= 0 ) throw Exception();
      if ( !get_file()->sync() ) throw Exception();
      ASSERT_EQUAL( bytes_written, YIELD_PLATFORM_FILE_TEST_STRING_LEN );

      char test_str[YIELD_PLATFORM_FILE_TEST_STRING_LEN];
      for ( uint8_t read_i = 0; read_i < 8; read_i++ )
      {
        // Read multiple times to test caching files
        ssize_t bytes_read
          = get_file()->read
            (
              test_str,
              YIELD_PLATFORM_FILE_TEST_STRING_LEN,
              0
            );
        if ( bytes_read <= 0 ) throw Exception();
        ASSERT_EQUAL( bytes_read, YIELD_PLATFORM_FILE_TEST_STRING_LEN );
        ASSERT_TRUE
        (
          strncmp
          (
            test_str,
            YIELD_PLATFORM_FILE_TEST_STRING,
            YIELD_PLATFORM_FILE_TEST_STRING_LEN
          ) == 0
        );
      }
    }

    YIELD_PLATFORM_FILE_TEST_CASE( removexattr )
    {
      if
      (
        get_file()->setxattr
        (
          YIELD_PLATFORM_FILE_TEST_XATTR_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_VALUE,
          0
        )
      )
      {
        if ( !get_file()->removexattr( YIELD_PLATFORM_FILE_TEST_XATTR_NAME ) )
          throw Exception();
      }
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
      else if ( errno != ENOTSUP )
        throw Exception();
#endif
    }

    YIELD_PLATFORM_FILE_TEST_CASE( setlk )
    {
      if ( !get_file()->setlk( true, 0, 256 ) ) throw Exception();
    }

    YIELD_PLATFORM_FILE_TEST_CASE( setlkw )
    {
      if ( !get_file()->setlkw( true, 0, 256 ) ) throw Exception();
    }

    YIELD_PLATFORM_FILE_TEST_CASE( setxattr )
    {
      if
      (
        get_file()->setxattr
        (
          YIELD_PLATFORM_FILE_TEST_XATTR_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_VALUE,
          0
        )
      )
      {
        std::string value;
        get_file()->getxattr( YIELD_PLATFORM_FILE_TEST_XATTR_NAME, value );
        ASSERT_EQUAL( value, YIELD_PLATFORM_FILE_TEST_XATTR_VALUE );
      }
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
      else if ( errno != ENOTSUP )
        throw Exception();
#endif
    }

    YIELD_PLATFORM_FILE_TEST_CASE( sync )
    {
      get_file()->write
      (
        YIELD_PLATFORM_FILE_TEST_STRING,
        YIELD_PLATFORM_FILE_TEST_STRING_LEN,
        0
      );

      if ( !get_file()->sync() )
        throw Exception();

      ASSERT_TRUE
      (
        get_file()->getattr()->get_size()
          >= YIELD_PLATFORM_FILE_TEST_STRING_LEN
      );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( truncate )
    {
      if
      (
        !get_file()->write
        (
          YIELD_PLATFORM_FILE_TEST_STRING,
          YIELD_PLATFORM_FILE_TEST_STRING_LEN,
          0
        )
      )
        throw YIELD::platform::Exception();

      if ( !get_file()->sync() )
        throw Exception();

      ASSERT_TRUE
      (
        get_file()->getattr()->get_size()
          >= YIELD_PLATFORM_FILE_TEST_STRING_LEN
      );

      if ( !get_file()->truncate( 0 ) )
        throw Exception();

      if ( !get_file()->sync() )
        throw Exception();

      ASSERT_EQUAL( get_file()->getattr()->get_size(), 0 );
    }

    YIELD_PLATFORM_FILE_TEST_CASE( unlk )
    {
      if ( !get_file()->setlkw( true, 0, 256 ) )
        throw Exception();
#ifndef _WIN32
      // getlk will not be true because we're using the same pid
      // as the one that acquired the lock
      if ( get_file()->getlk( true, 0, 256 ) )
        throw Exception();
#endif
      if ( !get_file()->unlk( 0, 256 ) )
        throw Exception();
    }


    template <class VolumeType = Volume>
    class FileTestSuite : public yunit::TestSuite
    {
    public:
      FileTestSuite( const std::string& name )
        : TestSuite( name )
      {
        YIELD::platform::auto_Volume volume = new VolumeType;
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
      }
    };
  };
};


#endif
