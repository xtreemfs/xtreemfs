// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _YIELD_PLATFORM_FILE_TEST_H_
#define _YIELD_PLATFORM_FILE_TEST_H_

#include "yield/platform.h"
#include "yunit.h"


#define YIELD_FILE_TEST_FILE_NAME "file_test.txt"
#define YIELD_FILE_TEST_FILE_OPEN_FLAGS O_CREAT|O_TRUNC|O_RDWR
#define YIELD_FILE_TEST_STRING "file_test"
#define YIELD_FILE_TEST_STRING_LEN 11
#define YIELD_FILE_TEST_XATTR_NAME "file_test_xattr_name"
#define YIELD_FILE_TEST_XATTR_VALUE "file_test_xattr_value"


namespace YIELD
{
  namespace platform
  {
    class FileTestCase : public yunit::TestCase
    {
    public:
      FileTestCase( const std::string& name, YIELD::platform::auto_Volume volume = NULL )
        : yunit::TestCase( name )
      {
        if ( volume != NULL )
          this->volume = volume;
        else
          this->volume = new Volume;
      }

      virtual ~FileTestCase()
      { }

      FileTestCase& operator=( const FileTestCase& ) { return *this; }

      void setUp()
      {
        tearDown();
        file = volume->open( YIELD_FILE_TEST_FILE_NAME, YIELD_FILE_TEST_FILE_OPEN_FLAGS );
        if ( file == NULL ) 
          throw Exception();
      }

      void tearDown()
      {
        file.reset( NULL );
        volume->unlink( YIELD_FILE_TEST_FILE_NAME );
      }

    protected:
      auto_File get_file() const { return file; }

    private:
      YIELD::platform::auto_Volume volume;
      auto_File file;
    };

#define YIELD_FILE_TEST_CASE( TestCaseName ) \
    class File_##TestCaseName##Test : public FileTestCase \
    { \
    public:\
      File_##TestCaseName##Test( YIELD::platform::auto_Volume volume = NULL ) \
        : FileTestCase( "File_" # TestCaseName "Test", volume ) \
      { } \
      void runTest(); \
    };\
      inline void File_##TestCaseName##Test::runTest()


    YIELD_FILE_TEST_CASE( close )
    {
      if ( !get_file()->close() ) 
        throw Exception();
      ASSERT_FALSE( get_file()->close() );
    }

    YIELD_FILE_TEST_CASE( datasync )
    {
      get_file()->write( YIELD_FILE_TEST_STRING, YIELD_FILE_TEST_STRING_LEN, 0 );
      if ( !get_file()->datasync() ) 
        throw Exception();
      ASSERT_TRUE( get_file()->stat()->get_size() >= YIELD_FILE_TEST_STRING_LEN );
    }

    YIELD_FILE_TEST_CASE( getpagesize )
    {
      size_t pagesize = get_file()->getpagesize();
      ASSERT_EQUAL( pagesize % 2, 0 );
    }

    YIELD_FILE_TEST_CASE( get_size )
    {
      uint64_t size = get_file()->get_size();
      ASSERT_EQUAL( size, 0 );
    }

    YIELD_FILE_TEST_CASE( getxattr )
    {
      if ( get_file()->setxattr( YIELD_FILE_TEST_XATTR_NAME, YIELD_FILE_TEST_XATTR_VALUE, 0 ) )
      {
        std::string value;
        get_file()->getxattr( YIELD_FILE_TEST_XATTR_NAME, value );
        ASSERT_EQUAL( value, YIELD_FILE_TEST_XATTR_VALUE );
      }
#ifdef YIELD_HAVE_XATTR_H
      else
        throw Exception();
#endif
    }

    YIELD_FILE_TEST_CASE( listxattr )
    {
      if ( get_file()->setxattr( YIELD_FILE_TEST_XATTR_NAME, YIELD_FILE_TEST_XATTR_VALUE, 0 ) )
      {
        std::vector<std::string> names;
        get_file()->listxattr( names );
        ASSERT_TRUE( names.size() >= 1 );
        for ( std::vector<std::string>::const_iterator name_i = names.begin(); name_i != names.end(); name_i++ )
        {
          if ( *name_i == YIELD_FILE_TEST_XATTR_NAME )
            return;
        }
        FAIL();
      }
#ifdef YIELD_HAVE_XATTR_H
      else
        throw Exception();
#endif
    }

    YIELD_FILE_TEST_CASE( operatorint )
    {
#ifdef _WIN32
      static_cast<void*>( *get_file() );
#else
      static_cast<int>( *get_file() );
#endif
    }

    YIELD_FILE_TEST_CASE( read )
    {
      ssize_t bytes_written = get_file()->write( YIELD_FILE_TEST_STRING, YIELD_FILE_TEST_STRING_LEN, 0 );
      if ( bytes_written <= 0 ) throw Exception();
      if ( !get_file()->sync() ) throw Exception();
      ASSERT_EQUAL( bytes_written, YIELD_FILE_TEST_STRING_LEN );
      char test_str[YIELD_FILE_TEST_STRING_LEN];
      for ( uint8_t read_i = 0; read_i < 8; read_i++ ) // Read multiple times to test caching files
      {
        ssize_t bytes_read = get_file()->read( test_str, YIELD_FILE_TEST_STRING_LEN, 0 );
        if ( bytes_read <= 0 ) throw Exception();
        ASSERT_EQUAL( bytes_read, YIELD_FILE_TEST_STRING_LEN );
        ASSERT_TRUE( strncmp( test_str, YIELD_FILE_TEST_STRING, YIELD_FILE_TEST_STRING_LEN ) == 0 );
      }
    }

    YIELD_FILE_TEST_CASE( removexattr )
    {
      if ( get_file()->setxattr( YIELD_FILE_TEST_XATTR_NAME, YIELD_FILE_TEST_XATTR_VALUE, 0 ) )
      {
        if ( !get_file()->removexattr( YIELD_FILE_TEST_XATTR_NAME ) ) 
          throw Exception();
      }
#ifdef YIELD_HAVE_XATTR_H
      else
        throw Exception();
#endif
    }

    YIELD_FILE_TEST_CASE( setlk )
    {
      if ( !get_file()->setlk( true, 0, 256 ) ) throw Exception();
    }

    YIELD_FILE_TEST_CASE( setlkw )
    {
      if ( !get_file()->setlkw( true, 0, 256 ) ) throw Exception();
    }

    YIELD_FILE_TEST_CASE( setxattr )
    {
      if ( get_file()->setxattr( YIELD_FILE_TEST_XATTR_NAME, YIELD_FILE_TEST_XATTR_VALUE, 0 ) )
      {
        std::string value;
        get_file()->getxattr( YIELD_FILE_TEST_XATTR_NAME, value );
        ASSERT_EQUAL( value, YIELD_FILE_TEST_XATTR_VALUE );
      }
#ifdef YIELD_HAVE_XATTR_H
      else
        throw Exception();
#endif
    }

    YIELD_FILE_TEST_CASE( stat )
    {
      auto_Stat stbuf = get_file()->stat();
      ASSERT_TRUE( stbuf->ISREG() );
      ASSERT_EQUAL( stbuf->get_size(), 0 );
      ASSERT_NOTEQUAL( stbuf->get_atime(), 0 );
      ASSERT_NOTEQUAL( stbuf->get_mtime(), 0 );
      ASSERT_NOTEQUAL( stbuf->get_ctime(), 0 );
    }

    YIELD_FILE_TEST_CASE( sync )
    {
      get_file()->write( YIELD_FILE_TEST_STRING, YIELD_FILE_TEST_STRING_LEN, 0 );
      if ( !get_file()->sync() ) throw Exception();
      ASSERT_TRUE( get_file()->stat()->get_size() >= YIELD_FILE_TEST_STRING_LEN );
    }

    YIELD_FILE_TEST_CASE( truncate )
    {
      if ( !get_file()->write( YIELD_FILE_TEST_STRING, YIELD_FILE_TEST_STRING_LEN, 0 ) ) throw YIELD::platform::Exception();
      if ( !get_file()->sync() ) throw Exception();
      ASSERT_TRUE( get_file()->stat()->get_size() >= YIELD_FILE_TEST_STRING_LEN );
      if ( !get_file()->truncate( 0 ) ) throw Exception();
      if ( !get_file()->sync() ) throw Exception();
      ASSERT_EQUAL( get_file()->stat()->get_size(), 0 );
    }

    YIELD_FILE_TEST_CASE( unlk )
    {
      if ( !get_file()->setlkw( true, 0, 256 ) ) throw Exception();
#ifndef _WIN32
      if ( get_file()->getlk( true, 0, 256 ) ) throw Exception(); // getlk will not be true because we're using the same pid as the one that acquired the lock
#endif
      if ( !get_file()->unlk( 0, 256 ) ) throw Exception();
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
        addTest( new File_get_sizeTest( volume ) );
        addTest( new File_getxattrTest( volume  ) );
        addTest( new File_listxattrTest( volume ) );
        addTest( new File_operatorintTest( volume ) );
        addTest( new File_readTest( volume ) );
        addTest( new File_removexattrTest( volume ) );
        addTest( new File_setlkTest( volume ) );
        addTest( new File_setlkwTest( volume ) );
        addTest( new File_setxattrTest( volume ) );
        addTest( new File_statTest( volume ) );
        addTest( new File_syncTest( volume ) );
        addTest( new File_truncateTest( volume ) );
        addTest( new File_unlkTest( volume ) );
      }
    };
  };
};

#endif
