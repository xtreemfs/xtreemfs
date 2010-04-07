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

#include "yield/platform.h"
#include "yunit.h"
#ifndef _WIN32
#include <sys/statvfs.h>
#endif


namespace yield
{
  namespace platform
  {
    class VolumeTestCase : public yunit::TestCase
    {
    public:
      virtual ~VolumeTestCase() { }

    protected:
      VolumeTestCase( const string& name, Volume& volume )
        : yunit::TestCase( name ),
          test_dir_name( "volume_test" ),
          test_file_name( "volume_test.txt" ),
          test_link_name( "volume_test_link.txt" ),
          test_xattr_name( "test_xattr_name" ),
          test_xattr_value( "test_xattr_value" ),
          volume( volume.inc_ref() )
      { }

      const Path& get_test_dir_name() const { return test_dir_name; }
      const Path& get_test_file_name() const { return test_file_name; }
      const Path& get_test_link_name() const { return test_link_name; }
      const string& get_test_xattr_name() const { return test_xattr_name; }
      const string& get_test_xattr_value() const { return test_xattr_value; }
      Volume& get_volume() const { return volume; }

      bool set_test_xattr()
      {
        return get_volume().setxattr
               ( 
                 test_file_name,
                 test_xattr_name,
                 test_xattr_value, 
                 0 
               );
      }

      // yunit::TestCase
      void setUp()
      {
        tearDown();
        volume.touch( test_file_name );
      }

      void tearDown()
      {
        volume.rmtree( test_dir_name );
        volume.unlink( test_file_name );
        volume.unlink( test_link_name );
      }

    private:
      Path test_dir_name, test_file_name, test_link_name;
      string test_xattr_name, test_xattr_value;
      Volume& volume;
    };

#define YIELD_PLATFORM_VOLUME_TEST_CASE( TestCaseName )\
    class Volume_##TestCaseName##Test : public yield::platform::VolumeTestCase\
    {\
    public:\
      Volume_##TestCaseName##Test( Volume& volume )\
        : VolumeTestCase( "Volume_" # TestCaseName "Test", volume )\
      { }\
      void runTest();\
    };\
    inline void Volume_##TestCaseName##Test::runTest()


#ifndef _WIN32
    YIELD_PLATFORM_VOLUME_TEST_CASE( access )
    {
      ASSERT_TRUE( get_volume().access( get_test_file_name(), O_RDONLY ) );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( chmod )
    {
      if ( !get_volume().chmod( get_test_file_name(), File::MODE_DEFAULT ) )
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( chown )
    {
      if ( !get_volume().chown( get_test_file_name(), ::getuid(), ::getgid() ))
        throw Exception();
    }
#endif

    YIELD_PLATFORM_VOLUME_TEST_CASE( exists )
    {
      ASSERT_TRUE( get_volume().exists( get_test_file_name() ) );
      ASSERT_FALSE( get_volume().exists( "some other file.txt" ) );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( getattr )
    {
      Stat* stbuf = get_volume().getattr( get_test_file_name() );
      if ( stbuf != NULL ) 
        Stat::dec_ref( *stbuf );
      else
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( getxattr )
    {
      if ( set_test_xattr() )
      {
        string xattr_value;
        get_volume().getxattr
        (
          get_test_file_name(),
          get_test_xattr_name(),
          xattr_value
        );
        ASSERT_EQUAL( xattr_value, get_test_xattr_value() );
      }
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
      else if ( errno != ENOTSUP )
        throw Exception();
#endif
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( isdir )
    {
      get_volume().mkdir( get_test_dir_name() );
      ASSERT_TRUE( get_volume().isdir( get_test_dir_name() ) );
      ASSERT_FALSE( get_volume().isdir( get_test_file_name() ) );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( isfile )
    {
      ASSERT_TRUE( get_volume().isfile( get_test_file_name() ) );
      get_volume().mkdir( get_test_dir_name() );
      ASSERT_FALSE( get_volume().isfile( get_test_dir_name() ) );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( link )
    {
      if ( !get_volume().link( get_test_file_name(), get_test_link_name() ) )
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( listxattr )
    {
      if ( set_test_xattr() )
      {
        vector<string> names;
        get_volume().listxattr( get_test_file_name(), names );

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

    YIELD_PLATFORM_VOLUME_TEST_CASE( mkdir )
    {
      if ( !get_volume().mkdir( get_test_dir_name() ) )
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( mktree )
    {
      Path subdir_path( Path( "volume_test" ) + Path( "subdir" ) );
      if ( !get_volume().mktree( subdir_path ) ) throw Exception();
      ASSERT_TRUE( get_volume().exists( subdir_path ) );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( open )
    {
      File* file = get_volume().open( get_test_file_name() );
      if ( file != NULL )
        File::dec_ref( *file );
      else
        throw Exception();
    }

#ifndef _WIN32
    YIELD_PLATFORM_VOLUME_TEST_CASE( readlink )
    {
      if ( !get_volume().symlink( get_test_file_name(), get_test_link_name() ))
        throw Exception();

      Path* target_path = get_volume().readlink( get_test_link_name() );
      if ( target_path != NULL )
      {
        ASSERT_EQUAL( *target_path, get_test_file_name() );
        Path::dec_ref( *target_path );
      }
      else
        throw Exception();
    }
#endif

    YIELD_PLATFORM_VOLUME_TEST_CASE( removexattr )
    {
      if ( set_test_xattr() )
      {
        if
        (
          !get_volume().removexattr
          (
            get_test_file_name(),
            get_test_xattr_name()
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
      if ( !get_volume().rename( get_test_file_name(), "volume_test2.txt" ) )
        throw Exception();
      get_volume().unlink( "volume_test2.txt" );
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( rmdir )
    {
      get_volume().mkdir( get_test_dir_name() );
      if ( !get_volume().rmdir( get_test_dir_name() ) )
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( setxattr )
    {
      if ( set_test_xattr() )
      {
        string xattr_value;
        get_volume().getxattr
        (
          get_test_file_name(),
          get_test_xattr_name(),
          xattr_value
        );

        ASSERT_EQUAL( xattr_value, get_test_xattr_value() );
      }
#ifdef YIELD_PLATFORM_HAVE_XATTR_H
      else if ( errno != ENOTSUP )
        throw Exception();
#endif
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( statvfs )
    {
      get_volume().mkdir( get_test_dir_name() );
      struct statvfs stvfsbuf;
      if ( !get_volume().statvfs( get_test_dir_name(), stvfsbuf ) )
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
      if ( !get_volume().symlink( get_test_file_name(), get_test_link_name() ))
        throw Exception();
    }
#endif

    YIELD_PLATFORM_VOLUME_TEST_CASE( truncate )
    {
      File* file = get_volume().open( get_test_file_name() );

      if ( file != NULL )
      {
        file->write( "test", 4, 0 );
        File::dec_ref( *file );

        Stat* stbuf = get_volume().getattr( get_test_file_name() );
        if ( stbuf != NULL )
        {
          ASSERT_EQUAL( stbuf->get_size(), 4 );
          Stat::dec_ref( *stbuf );
        }
        else
          throw Exception();

        ssize_t trunc_ret = get_volume().truncate( get_test_file_name(), 0 );
        ASSERT_EQUAL( trunc_ret, 0 );
        stbuf = get_volume().getattr( get_test_file_name() );
        if ( stbuf != NULL )
        {
          ASSERT_EQUAL( stbuf->get_size(), 0 );
          Stat::dec_ref( *stbuf );
        }
        else
          throw Exception();
      }
      else
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( unlink )
    {
      if ( !get_volume().unlink( get_test_file_name() ) )
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( utime )
    {
      Time atime, mtime, ctime;

      if ( get_volume().utime( get_test_file_name(), atime, mtime, ctime ) )
      {
        Stat* stbuf = get_volume().getattr( get_test_file_name() );

        if ( stbuf != NULL )
        {
          ASSERT_TRUE( stbuf->get_atime() - atime <= Time::NS_IN_S );
          ASSERT_TRUE( stbuf->get_mtime() - mtime <= Time::NS_IN_S );
#ifdef _WIN32
          ASSERT_TRUE( stbuf->get_ctime() - ctime <= Time::NS_IN_S );
#endif
          Stat::dec_ref( *stbuf );
        }
        else
          throw Exception();
      }
      else
        throw Exception();
    }

    YIELD_PLATFORM_VOLUME_TEST_CASE( volname )
    {
      Path volname = get_volume().volname( get_test_file_name() );
#ifdef _WIN32
      if ( volname.empty() ) throw Exception();
#endif
    }


    class VolumeTestSuite : public yunit::TestSuite
    {
    public:
      VolumeTestSuite( const string& name, Volume* volume = NULL )
        : yunit::TestSuite( name )
      {
        if ( volume == NULL )
          volume = new Volume;

#ifndef _WIN32
        addTest( new Volume_accessTest( *volume ) );
        addTest( new Volume_chmodTest( *volume ) );
        addTest( new Volume_chownTest( *volume ) );
#endif
        addTest( new Volume_existsTest( *volume ) );
        addTest( new Volume_getattrTest( *volume ) );
        addTest( new Volume_getxattrTest( *volume ) );
        addTest( new Volume_isdirTest( *volume ) );
        addTest( new Volume_isfileTest( *volume ) );
        addTest( new Volume_linkTest( *volume ) );
        addTest( new Volume_listxattrTest( *volume ) );
        addTest( new Volume_mkdirTest( *volume ) );
        addTest( new Volume_mktreeTest( *volume ) );
        addTest( new Volume_openTest( *volume ) );
#ifndef _WIN32
        addTest( new Volume_readlinkTest( *volume ) );
#endif
        addTest( new Volume_removexattrTest( *volume ));
        addTest( new Volume_renameTest( *volume ) );
        addTest( new Volume_rmdirTest( *volume ) );
        addTest( new Volume_setxattrTest( *volume ) );
        addTest( new Volume_statvfsTest( *volume ) );
#ifndef _WIN32
        addTest( new Volume_symlinkTest( *volume ) );
#endif
        addTest( new Volume_truncateTest( *volume ) );
        addTest( new Volume_unlinkTest( *volume ) );
        addTest( new Volume_utimeTest( *volume ) );
        addTest( new Volume_volnameTest( *volume ) );

        Volume::dec_ref( *volume );
      }
    };
  };
};


#endif
