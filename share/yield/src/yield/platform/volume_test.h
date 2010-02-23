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


#ifndef _YIELD_PLATFORM_VOLUME_TEST_H_
#define _YIELD_PLATFORM_VOLUME_TEST_H_

#include "directory_test.h"
#include "file_test.h"
#ifndef _WIN32
#include <sys/statvfs.h>
#endif


#define YIELD_PLATFORM_VOLUME_TEST_LINK_NAME "volume_test_link.txt"


namespace YIELD
{
  namespace platform
  {
    template <class VolumeType>
    class VolumeTestCase : public yunit::TestCase
    {
    public:
      VolumeTestCase<VolumeType> operator=( const VolumeTestCase<VolumeType>& )
      { }

      virtual ~VolumeTestCase()
      { }

      yidl::runtime::auto_Object<VolumeType> get_volume() const
      {
        return volume;
      }

    protected:
      VolumeTestCase
      (
        const std::string& name,
        yidl::runtime::auto_Object<VolumeType> volume
      )
        : yunit::TestCase( name ), volume( volume )
      { }

      void setUp()
      {
        tearDown();
        volume->creat( YIELD_PLATFORM_FILE_TEST_FILE_NAME );
      }

      void tearDown()
      {
        volume->rmtree( YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME );
        volume->unlink( YIELD_PLATFORM_FILE_TEST_FILE_NAME );
        volume->unlink( YIELD_PLATFORM_VOLUME_TEST_LINK_NAME );
      }

    private:
      yidl::runtime::auto_Object<VolumeType> volume;
    };

#define YIELD_PLATFORM_VOLUME_TEST_CASE( TestCaseName ) \
    template <class VolumeType> \
    class Volume_##TestCaseName##Test \
      : public YIELD::platform::VolumeTestCase<VolumeType> \
    { \
    public:\
      Volume_##TestCaseName##Test \
      ( \
        yidl::runtime::auto_Object<VolumeType> volume \
      ) \
        : VolumeTestCase<VolumeType> \
          ( \
            "Volume_" # TestCaseName "Test", \
            volume \
          ) \
      { }\
      void runTest(); \
    };\
    template <class VolumeType> \
    inline void Volume_##TestCaseName##Test<VolumeType>::runTest()


#ifndef _WIN32
    YIELD_PLATFORM_VOLUME_TEST_CASE( access )
    {
      ASSERT_TRUE
      (
        this->get_volume()->access( YIELD_PLATFORM_FILE_TEST_FILE_NAME, O_RDONLY )
      );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( chmod )
    {
      if
      (
        !this->get_volume()->chmod
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          File::MODE_DEFAULT
        )
      )
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( chown )
    {
      if
      (
        !this->get_volume()->chown
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          ::getuid(),
          ::getgid()
        )
      )
        throw Exception();
    }
#endif

    YIELD_PLATFORM_VOLUME_TEST_CASE( exists )
    {
      ASSERT_TRUE
      (
        this->get_volume()->exists
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME
        )
      );

      ASSERT_FALSE( this->get_volume()->exists( "some other file.txt" ) );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( getattr )
    {
      auto_Stat stbuf
        = this->get_volume()->getattr( YIELD_PLATFORM_FILE_TEST_FILE_NAME );
      ASSERT_TRUE( stbuf != NULL );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( getxattr )
    {
      if
      (
        this->get_volume()->setxattr
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_VALUE,
          0
        )
      )
      {
        std::string value;
        this->get_volume()->getxattr
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_NAME,
          value
        );
        ASSERT_EQUAL( value, YIELD_PLATFORM_FILE_TEST_XATTR_VALUE );
      }
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
      else if ( errno != ENOTSUP )
        throw Exception();
#endif
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( isdir )
    {
      this->get_volume()->mkdir
      (
        YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME,
        Volume::DIRECTORY_MODE_DEFAULT
      );

      ASSERT_TRUE
      (
        this->get_volume()->isdir( YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME )
      );

      ASSERT_FALSE
      (
        this->get_volume()->isdir( YIELD_PLATFORM_FILE_TEST_FILE_NAME )
      );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( isfile )
    {
      this->get_volume()->mkdir
      (
        YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME,
        Volume::DIRECTORY_MODE_DEFAULT
      );

      ASSERT_TRUE
      (
        this->get_volume()->isfile( YIELD_PLATFORM_FILE_TEST_FILE_NAME )
      );

      ASSERT_FALSE
      (
        this->get_volume()->isfile( YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME )
      );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( link )
    {
      if
      (
        !this->get_volume()->link
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          YIELD_PLATFORM_VOLUME_TEST_LINK_NAME
        )
      )
        throw Exception();

      this->get_volume()->unlink( YIELD_PLATFORM_VOLUME_TEST_LINK_NAME );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( listxattr )
    {
      if
      (
        this->get_volume()->setxattr
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_VALUE,
          0
        )
      )
      {
        std::vector<std::string> names;
        this->get_volume()->listxattr
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          names
        );

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

    YIELD_PLATFORM_VOLUME_TEST_CASE( mkdir )
    {
      if
      (
        !this->get_volume()->mkdir
        (
          YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME,
          Volume::DIRECTORY_MODE_DEFAULT
        )
      )
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( mktree )
    {
      Path subdir_path( Path( "volume_test" ) + Path( "subdir" ) );
      if
      (
        !static_cast<Volume*>( this->get_volume().get() )
          ->mktree( subdir_path )
      )
        throw Exception();
      ASSERT_TRUE( this->get_volume()->exists( subdir_path ) );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( open )
    {
      auto_File file
        = this->get_volume()->open
          (
            YIELD_PLATFORM_FILE_TEST_FILE_NAME,
            YIELD_PLATFORM_FILE_TEST_FILE_OPEN_FLAGS,
            File::MODE_DEFAULT,
            File::ATTRIBUTES_DEFAULT
          );
      ASSERT_TRUE( file != NULL );
    }

#ifndef _WIN32
    YIELD_PLATFORM_VOLUME_TEST_CASE( readlink )
    {
      if
      (
        !this->get_volume()->symlink
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          YIELD_PLATFORM_VOLUME_TEST_LINK_NAME
        )
      )
        throw Exception();

      auto_Path target_path
        = this->get_volume()->readlink( YIELD_PLATFORM_VOLUME_TEST_LINK_NAME );

      ASSERT_TRUE( *target_path == YIELD_PLATFORM_FILE_TEST_FILE_NAME );
    }
#endif

    YIELD_PLATFORM_VOLUME_TEST_CASE( removexattr )
    {
      if
      (
        this->get_volume()->setxattr
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_VALUE,
          0
        )
      )
      {
        if
        (
          !this->get_volume()->removexattr
          (
            YIELD_PLATFORM_FILE_TEST_FILE_NAME,
            YIELD_PLATFORM_FILE_TEST_XATTR_NAME
          )
        )
          throw Exception();
      }
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
      else if ( errno != ENOTSUP )
        throw Exception();
#endif
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( rename )
    {
      if
      (
        !this->get_volume()->rename
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          "volume_test2.txt"
        )
      )
        throw Exception();

      this->get_volume()->unlink( "volume_test2.txt" );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( rmdir )
    {
      this->get_volume()->mkdir
      (
        YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME,
        Volume::DIRECTORY_MODE_DEFAULT
      );

      if ( !this->get_volume()->rmdir( YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME ) )
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( setxattr )
    {
      if
      (
        this->get_volume()->setxattr
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_VALUE,
          0
        )
      )
      {
        std::string value;
        this->get_volume()->getxattr
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          YIELD_PLATFORM_FILE_TEST_XATTR_NAME,
          value
        );

        ASSERT_EQUAL( value, YIELD_PLATFORM_FILE_TEST_XATTR_VALUE );
      }
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
      else if ( errno != ENOTSUP )
        throw Exception();
#endif
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( statvfs )
    {
      this->get_volume()->mkdir
      (
        YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME,
        Volume::DIRECTORY_MODE_DEFAULT
      );

      struct statvfs stvfsbuf;
      if
      (
        !this->get_volume()->statvfs
        (
          YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME,
          stvfsbuf
        )
      )
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
      if
      (
        !this->get_volume()->symlink
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          YIELD_PLATFORM_VOLUME_TEST_LINK_NAME
        )
      )
        throw Exception();
    }
#endif

    YIELD_PLATFORM_VOLUME_TEST_CASE( truncate )
    {
      auto_File file
        = this->get_volume()->open
          (
            YIELD_PLATFORM_FILE_TEST_FILE_NAME,
            YIELD_PLATFORM_FILE_TEST_FILE_OPEN_FLAGS,
            File::MODE_DEFAULT,
            File::ATTRIBUTES_DEFAULT
          );

      if ( file != NULL )
      {
        file->write
        (
          YIELD_PLATFORM_FILE_TEST_STRING,
          YIELD_PLATFORM_FILE_TEST_STRING_LEN,
          0
        );

        file = NULL;

        auto_Stat stbuf
          = this->get_volume()->getattr( YIELD_PLATFORM_FILE_TEST_FILE_NAME );
        ASSERT_EQUAL( stbuf->get_size(), YIELD_PLATFORM_FILE_TEST_STRING_LEN );
        stbuf = NULL;

        this->get_volume()->truncate( YIELD_PLATFORM_FILE_TEST_FILE_NAME, 0 );
        stbuf
          = this->get_volume()->getattr( YIELD_PLATFORM_FILE_TEST_FILE_NAME );
        ASSERT_EQUAL( stbuf->get_size(), 0 );
      }
      else
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( unlink )
    {
      if ( !this->get_volume()->unlink( YIELD_PLATFORM_FILE_TEST_FILE_NAME ) )
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( utime )
    {
      Time atime, mtime, ctime;

      if
      (
        this->get_volume()->utime
        (
          YIELD_PLATFORM_FILE_TEST_FILE_NAME,
          atime,
          mtime,
          ctime
        )
      )
      {
        auto_Stat stbuf
          = this->get_volume()->getattr( YIELD_PLATFORM_FILE_TEST_FILE_NAME );
        ASSERT_NOTEQUAL( stbuf, NULL );
        ASSERT_TRUE( stbuf->get_atime() - atime <= Time::NS_IN_S );
        ASSERT_TRUE( stbuf->get_mtime() - mtime <= Time::NS_IN_S );
#ifdef _WIN32
        ASSERT_TRUE( stbuf->get_ctime() - ctime <= Time::NS_IN_S );
#endif
      }
      else
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( volname )
    {
      Path volname
        = this->get_volume()->volname( YIELD_PLATFORM_FILE_TEST_FILE_NAME );
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
        addTest( new Volume_getattrTest<VolumeType>( volume ) );
        addTest( new Volume_getxattrTest<VolumeType>( volume ) );
        addTest( new Volume_isdirTest<VolumeType>( volume ) );
        addTest( new Volume_isfileTest<VolumeType>( volume ) );
        addTest( new Volume_linkTest<VolumeType>( volume ) );
        addTest( new Volume_listxattrTest<VolumeType>( volume ) );
        addTest( new Volume_mkdirTest<VolumeType>( volume ) );
        addTest( new Volume_mktreeTest<VolumeType>( volume ) );
        addTest( new Volume_openTest<VolumeType>( volume ) );
#ifndef _WIN32
        addTest( new Volume_readlinkTest<VolumeType>( volume ) );
#endif
        addTest( new Volume_removexattrTest<VolumeType>( volume ) );
        addTest( new Volume_renameTest<VolumeType>( volume ) );
        addTest( new Volume_rmdirTest<VolumeType>( volume ) );
        addTest( new Volume_setxattrTest<VolumeType>( volume ) );
        addTest( new Volume_statvfsTest<VolumeType>( volume ) );
#ifndef _WIN32
        addTest( new Volume_symlinkTest<VolumeType>( volume ) );
#endif
        addTest( new Volume_truncateTest<VolumeType>( volume ) );
        addTest( new Volume_unlinkTest<VolumeType>( volume ) );
        addTest( new Volume_utimeTest<VolumeType>( volume ) );
        addTest( new Volume_volnameTest<VolumeType>( volume ) );
      }
    };
  };
};


#endif
