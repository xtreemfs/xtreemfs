// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _YIELD_PLATFORM_VOLUME_TEST_H_
#define _YIELD_PLATFORM_VOLUME_TEST_H_

#include "yield/platform.h"
#include "file_test.h"
#ifndef _WIN32
#include <sys/statvfs.h>
#endif


#define YIELD_PLATFORM_VOLUME_TEST_DIR_NAME "volume_test"
#define YIELD_PLATFORM_VOLUME_TEST_LINK_NAME "volume_test_link.txt"


namespace YIELD
{
  namespace platform
  {
    template <class VolumeType>
    class VolumeTestCase : public yunit::TestCase
    {
    public:
      VolumeTestCase<VolumeType> operator=( const VolumeTestCase<VolumeType>& ) { }

      virtual ~VolumeTestCase()
      { }

      yidl::runtime::auto_Object<VolumeType> get_volume() const { return volume; }

    protected:
      VolumeTestCase( const std::string& name, yidl::runtime::auto_Object<VolumeType> volume )
        : yunit::TestCase( name ), volume( volume )
      { }

      void setUp()
      {
        tearDown();
        volume->creat( YIELD_FILE_TEST_FILE_NAME );
      }

      void tearDown()
      {
        volume->unlink( YIELD_FILE_TEST_FILE_NAME );
        volume->unlink( YIELD_PLATFORM_VOLUME_TEST_LINK_NAME );
        volume->rmtree( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME );
      }

    private:
      yidl::runtime::auto_Object<VolumeType> volume;
    };

#define YIELD_PLATFORM_VOLUME_TEST_CASE( TestCaseName ) \
    template <class VolumeType> \
    class Volume_##TestCaseName##Test : public YIELD::platform::VolumeTestCase<VolumeType> \
    { \
    public:\
      Volume_##TestCaseName##Test( yidl::runtime::auto_Object<VolumeType> volume ) \
        : VolumeTestCase<VolumeType>( "Volume_" # TestCaseName "Test", volume ) \
      { }\
      void runTest(); \
    };\
    template <class VolumeType> \
    inline void Volume_##TestCaseName##Test<VolumeType>::runTest()


#ifndef _WIN32
    YIELD_PLATFORM_VOLUME_TEST_CASE( access )
    {
      ASSERT_TRUE( this->get_volume()->access( YIELD_FILE_TEST_FILE_NAME, O_RDONLY ) );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( chmod )
    {
      if ( !this->get_volume()->chmod( YIELD_FILE_TEST_FILE_NAME, File::DEFAULT_MODE ) ) 
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( chown )
    {
      if ( !this->get_volume()->chown( YIELD_FILE_TEST_FILE_NAME, ::getuid(), ::getgid() ) )
        throw Exception();
    }
#endif

    YIELD_PLATFORM_VOLUME_TEST_CASE( exists )
    {
      ASSERT_TRUE( this->get_volume()->exists( YIELD_FILE_TEST_FILE_NAME ) );
      ASSERT_FALSE( this->get_volume()->exists( "some other file.txt" ) );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( getxattr )
    {
      if ( this->get_volume()->setxattr( YIELD_FILE_TEST_FILE_NAME, YIELD_FILE_TEST_XATTR_NAME, YIELD_FILE_TEST_XATTR_VALUE, 0 ) )
      {
        std::string value;
        this->get_volume()->getxattr( YIELD_FILE_TEST_FILE_NAME, YIELD_FILE_TEST_XATTR_NAME, value );
        ASSERT_EQUAL( value, YIELD_FILE_TEST_XATTR_VALUE );
      }
#ifdef YIELD_HAVE_XATTR_H
      else
        throw Exception();
#endif
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( isdir )
    {
      this->get_volume()->mkdir( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME, Volume::DEFAULT_DIRECTORY_MODE );
      ASSERT_TRUE( this->get_volume()->isdir( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME ) );
      ASSERT_FALSE( this->get_volume()->isdir( YIELD_FILE_TEST_FILE_NAME ) );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( isfile )
    {
      this->get_volume()->mkdir( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME, Volume::DEFAULT_DIRECTORY_MODE );
      ASSERT_TRUE( this->get_volume()->isfile( YIELD_FILE_TEST_FILE_NAME ) );
      ASSERT_FALSE( this->get_volume()->isfile( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME ) );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( link )
    {
      if ( !this->get_volume()->link( YIELD_FILE_TEST_FILE_NAME, YIELD_PLATFORM_VOLUME_TEST_LINK_NAME ) ) 
        throw Exception();
      this->get_volume()->unlink( YIELD_PLATFORM_VOLUME_TEST_LINK_NAME );
    }

    class DummylistdirCallback : public Volume::listdirCallback
    {
    public:
      bool operator()( const Path& )
      {
        return true;
      }
    };

    YIELD_PLATFORM_VOLUME_TEST_CASE( listdir )
    {
      this->get_volume()->mkdir( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME, Volume::DEFAULT_DIRECTORY_MODE );
      DummylistdirCallback listdir_callback;
      if ( !this->get_volume()->listdir( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME, Path(), listdir_callback ) ) 
        throw Exception();

      std::vector<YIELD::platform::Path> names;
      if ( static_cast<Volume*>( this->get_volume().get() )->listdir( ".", Path(), names ) )
      {
        ASSERT_FALSE( names.empty() );
        for ( std::vector<YIELD::platform::Path>::iterator name_i = names.begin(); name_i != names.end(); name_i++ )
        {
          if ( *name_i == YIELD_FILE_TEST_FILE_NAME )
            return;
        }
        FAIL();
      }
      else
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( listxattr )
    {
      if ( this->get_volume()->setxattr( YIELD_FILE_TEST_FILE_NAME, YIELD_FILE_TEST_XATTR_NAME, YIELD_FILE_TEST_XATTR_VALUE, 0 ) )
      {
        std::vector<std::string> names; this->get_volume()->listxattr( YIELD_FILE_TEST_FILE_NAME, names );
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

    YIELD_PLATFORM_VOLUME_TEST_CASE( mkdir )
    {
      if ( !this->get_volume()->mkdir( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME, Volume::DEFAULT_DIRECTORY_MODE ) )
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( mktree )
    {
      Path subdir_path( Path( "volume_test" ) + Path( "subdir" ) );
      if ( !static_cast<Volume*>( this->get_volume().get() )->mktree( subdir_path ) )
        throw Exception();
      ASSERT_TRUE( this->get_volume()->exists( subdir_path ) );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( open )
    {
      auto_File file = this->get_volume()->open( YIELD_FILE_TEST_FILE_NAME, YIELD_FILE_TEST_FILE_OPEN_FLAGS, File::DEFAULT_MODE, File::DEFAULT_ATTRIBUTES );
      ASSERT_TRUE( file.get() != NULL );
    }

    class DummyreaddirCallback : public Volume::readdirCallback
    {
    public:
      bool operator()( const Path&, auto_Stat )
      {
        return true;
      }
    };

    YIELD_PLATFORM_VOLUME_TEST_CASE( readdir )
    {
      this->get_volume()->mkdir( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME, Volume::DEFAULT_DIRECTORY_MODE );
      DummyreaddirCallback readdir_callback;
      if ( !this->get_volume()->readdir( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME, Path(), readdir_callback ) )
        throw Exception();
    }

#ifndef _WIN32
    YIELD_PLATFORM_VOLUME_TEST_CASE( readlink )
    {
      if ( !this->get_volume()->symlink( YIELD_FILE_TEST_FILE_NAME, YIELD_PLATFORM_VOLUME_TEST_LINK_NAME ) )
        throw Exception();
      auto_Path target_path = this->get_volume()->readlink( YIELD_PLATFORM_VOLUME_TEST_LINK_NAME );
      ASSERT_TRUE( *target_path == YIELD_FILE_TEST_FILE_NAME );
    }
#endif

    YIELD_PLATFORM_VOLUME_TEST_CASE( removexattr )
    {
      if ( this->get_volume()->setxattr( YIELD_FILE_TEST_FILE_NAME, YIELD_FILE_TEST_XATTR_NAME, YIELD_FILE_TEST_XATTR_VALUE, 0 ) )
      {
        bool removexattr_ret = this->get_volume()->removexattr( YIELD_FILE_TEST_FILE_NAME, YIELD_FILE_TEST_XATTR_NAME );
        ASSERT_TRUE( removexattr_ret );
      }
#ifdef YIELD_HAVE_XATTR_H
      else
        throw Exception();
#endif
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( rename )
    {
      if ( !this->get_volume()->rename( YIELD_FILE_TEST_FILE_NAME, "volume_test2.txt" ) )
        throw Exception();
      this->get_volume()->unlink( "volume_test2.txt" );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( rmdir )
    {
      this->get_volume()->mkdir( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME, Volume::DEFAULT_DIRECTORY_MODE );
      if ( !this->get_volume()->rmdir( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME ) )
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( setattr )
    {
#ifdef _WIN32
      if ( !this->get_volume()->setattr( YIELD_FILE_TEST_FILE_NAME, 0x00000080 ) ) throw Exception(); // FILE_ATTRIBUTE_NORMAL
#else
      ASSERT_FALSE( this->get_volume()->setattr( YIELD_FILE_TEST_FILE_NAME, 0 ) );
#endif
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( setxattr )
    {
      if ( this->get_volume()->setxattr( YIELD_FILE_TEST_FILE_NAME, YIELD_FILE_TEST_XATTR_NAME, YIELD_FILE_TEST_XATTR_VALUE, 0 ) )
      {
        std::string value; this->get_volume()->getxattr( YIELD_FILE_TEST_FILE_NAME, YIELD_FILE_TEST_XATTR_NAME, value );
        ASSERT_EQUAL( value, YIELD_FILE_TEST_XATTR_VALUE );
      }
#ifdef YIELD_HAVE_XATTR_H
      else
        throw Exception();
#endif
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( stat )
    {
      auto_Stat stbuf = this->get_volume()->stat( YIELD_FILE_TEST_FILE_NAME );
      ASSERT_TRUE( stbuf != NULL );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( statvfs )
    {
      this->get_volume()->mkdir( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME, Volume::DEFAULT_DIRECTORY_MODE );
      struct statvfs stvfsbuf;
      if ( !this->get_volume()->statvfs( YIELD_PLATFORM_VOLUME_TEST_DIR_NAME, stvfsbuf ) )
        throw Exception();
      ASSERT_TRUE( stvfsbuf.f_bsize > 0 );
      ASSERT_TRUE( stvfsbuf.f_blocks > 0 );
      ASSERT_TRUE( stvfsbuf.f_bfree > 0 );
      ASSERT_TRUE( stvfsbuf.f_blocks >= stvfsbuf.f_bfree );
      ASSERT_TRUE( stvfsbuf.f_namemax > 0 );
    }

#ifndef _WIN32
    YIELD_PLATFORM_VOLUME_TEST_CASE( symlink )
    {
      if ( !this->get_volume()->symlink( YIELD_FILE_TEST_FILE_NAME, YIELD_PLATFORM_VOLUME_TEST_LINK_NAME ) )
        throw Exception();
    }
#endif

    YIELD_PLATFORM_VOLUME_TEST_CASE( truncate )
    {
      auto_File file = this->get_volume()->open( YIELD_FILE_TEST_FILE_NAME, YIELD_FILE_TEST_FILE_OPEN_FLAGS, File::DEFAULT_MODE, File::DEFAULT_ATTRIBUTES );
      if ( file.get() != NULL )
      {
        file->write( YIELD_FILE_TEST_STRING, YIELD_FILE_TEST_STRING_LEN, 0 );
        file.reset( NULL );
        auto_Stat stbuf = this->get_volume()->stat( YIELD_FILE_TEST_FILE_NAME );
        ASSERT_EQUAL( stbuf->get_size(), YIELD_FILE_TEST_STRING_LEN );
        stbuf.reset( NULL );
        this->get_volume()->truncate( YIELD_FILE_TEST_FILE_NAME, 0 );
        stbuf = this->get_volume()->stat( YIELD_FILE_TEST_FILE_NAME );
        ASSERT_EQUAL( stbuf->get_size(), 0 );
      }
      else
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( unlink )
    {
      if ( !this->get_volume()->unlink( YIELD_FILE_TEST_FILE_NAME ) )
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( utimens )
    {
      if ( !this->get_volume()->utimens( YIELD_FILE_TEST_FILE_NAME, Time(), Time(), Time() ) )
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( volname )
    {
      Path volname = this->get_volume()->volname( YIELD_FILE_TEST_FILE_NAME );
#ifdef _WIN32
      if ( volname.empty() )
        throw Exception();
#endif
    }


    template <class VolumeType = Volume>
    class VolumeTestSuite : public yunit::TestSuite
    {
    public:
      VolumeTestSuite( const std::string& name )
        : yunit::TestSuite( name )
      {
        yidl::runtime::auto_Object<VolumeType> volume = new VolumeType;
#ifndef _WIN32
        addTest( new Volume_accessTest<VolumeType>( volume ) );
        addTest( new Volume_chmodTest<VolumeType>( volume ) );
        addTest( new Volume_chownTest<VolumeType>( volume ) );
#endif
        addTest( new Volume_existsTest<VolumeType>( volume ) );
        addTest( new Volume_getxattrTest<VolumeType>( volume ) );
        addTest( new Volume_isdirTest<VolumeType>( volume ) );
        addTest( new Volume_isfileTest<VolumeType>( volume ) );
        addTest( new Volume_linkTest<VolumeType>( volume ) );
        addTest( new Volume_listdirTest<VolumeType>( volume ) );
        addTest( new Volume_listxattrTest<VolumeType>( volume ) );
        addTest( new Volume_mkdirTest<VolumeType>( volume ) );
        addTest( new Volume_mktreeTest<VolumeType>( volume ) );
        addTest( new Volume_openTest<VolumeType>( volume ) );
        addTest( new Volume_readdirTest<VolumeType>( volume ) );
#ifndef _WIN32
        addTest( new Volume_readlinkTest<VolumeType>( volume ) );
#endif
        addTest( new Volume_removexattrTest<VolumeType>( volume ) );
        addTest( new Volume_renameTest<VolumeType>( volume ) );
        addTest( new Volume_rmdirTest<VolumeType>( volume ) );
        addTest( new Volume_setattrTest<VolumeType>( volume ) );
        addTest( new Volume_setxattrTest<VolumeType>( volume ) );
        addTest( new Volume_statTest<VolumeType>( volume ) );
        addTest( new Volume_statvfsTest<VolumeType>( volume ) );
#ifndef _WIN32
        addTest( new Volume_symlinkTest<VolumeType>( volume ) );
#endif
        addTest( new Volume_truncateTest<VolumeType>( volume ) );
        addTest( new Volume_unlinkTest<VolumeType>( volume ) );
        addTest( new Volume_utimensTest<VolumeType>( volume ) );
        addTest( new Volume_volnameTest<VolumeType>( volume ) );
      }
    };
  };
};

#endif
