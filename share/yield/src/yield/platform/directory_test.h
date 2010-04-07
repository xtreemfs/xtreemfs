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


#ifndef _YIELD_PLATFORM_DIRECTORY_TEST_H_
#define _YIELD_PLATFORM_DIRECTORY_TEST_H_

#include "yield/platform.h"
#include "yunit.h"


namespace yield
{
  namespace platform
  {
    class DirectoryTestCase : public yunit::TestCase
    {
    public:
      virtual ~DirectoryTestCase() { }

    protected:
      DirectoryTestCase( const string& name, Volume* volume = NULL )
        : yunit::TestCase( name ),
          test_dir_name( "directory_test" ),
          test_file_name( "directory_test.txt" ),
          test_file_path( test_dir_name / test_file_name )
      {
        directory = NULL;
          
        if ( volume != NULL )
          this->volume = volume;
        else
          this->volume = new Volume;
      }

      // yunit::TestCase
      void setUp()
      {        
        tearDown();

        if ( !volume->mkdir( get_test_dir_name() ) ) throw Exception();
        if ( !volume->touch( get_test_file_path() ) ) throw Exception();

        directory = volume->opendir( get_test_dir_name() );
        if ( directory == NULL )
          throw Exception();
      }

      void tearDown()
      {
        Directory::dec_ref( directory );
        volume->rmtree( get_test_dir_name() );
      }

    protected:
      Directory& get_directory() const { return *directory; }
      const Path& get_test_dir_name() const { return test_dir_name; }
      const Path& get_test_file_name() const { return test_file_name; }
      const Path& get_test_file_path() const { return test_file_path; }

    private:
      Directory* directory;
      Path test_dir_name, test_file_name, test_file_path;
      Volume* volume;
    };


#define YIELD_PLATFORM_DIRECTORY_TEST_CASE( TestCaseName ) \
    class Directory_##TestCaseName##Test : public DirectoryTestCase \
    { \
    public:\
      Directory_##TestCaseName##Test( yield::platform::Volume* volume = NULL ) \
        : DirectoryTestCase( "Directory_" # TestCaseName "Test", volume ) \
      { } \
      void runTest(); \
    };\
      inline void Directory_##TestCaseName##Test::runTest()


    YIELD_PLATFORM_DIRECTORY_TEST_CASE( close )
    {
      if ( !get_directory().close() ) throw Exception();
    }

    YIELD_PLATFORM_DIRECTORY_TEST_CASE( read )
    {
      Directory::Entry* dirent = get_directory().read();
      ASSERT_NOTEQUAL( dirent, NULL );
      ASSERT_EQUAL( dirent->get_name(), "." );
      dirent = get_directory().read();
      ASSERT_EQUAL( dirent->get_name(), ".." );
      dirent = get_directory().read();
      ASSERT_EQUAL( dirent->get_name(), get_test_file_name() );
      Directory::Entry::dec_ref( *dirent );
    }


    class DirectoryTestSuite : public yunit::TestSuite
    {
    public:
      DirectoryTestSuite( const string& name, Volume* volume = NULL )
        : TestSuite( name )
      {
        if ( volume == NULL )
          volume = new Volume;

        addTest( new Directory_readTest( &volume->inc_ref() ) );

        Volume::dec_ref( *volume );
      }
    };
  };
};


#endif
