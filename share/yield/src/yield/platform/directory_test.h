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

#include "file_test.h"

#define YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME "directory_test"


namespace YIELD
{
  namespace platform
  {
    class DirectoryTestCase : public yunit::TestCase
    {
    public:
      DirectoryTestCase
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

      virtual ~DirectoryTestCase()
      { }

      DirectoryTestCase& operator=( const DirectoryTestCase& ) 
      { 
        return *this; 
      }

      void setUp()
      {
        tearDown();

        if ( !volume->mkdir( YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME ) )
          throw Exception();

        if 
        ( 
           !volume->touch
           ( 
             YIELD::platform::Path( YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME )
             / YIELD_PLATFORM_FILE_TEST_FILE_NAME 
           )
         )
          throw Exception();

        directory = volume->opendir( YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME );
        if ( directory == NULL )
          throw Exception();
      }

      void tearDown()
      {
        directory.reset( NULL );
        volume->rmtree( YIELD_PLATFORM_DIRECTORY_TEST_DIR_NAME );
      }

    protected:
      YIELD::platform::auto_Directory get_directory() const 
      { 
        return directory; 
      }

    private:
      YIELD::platform::auto_Directory directory;
      YIELD::platform::auto_Volume volume;
    };


#define YIELD_PLATFORM_DIRECTORY_TEST_CASE( TestCaseName ) \
    class Directory_##TestCaseName##Test : public DirectoryTestCase \
    { \
    public:\
      Directory_##TestCaseName##Test( YIELD::platform::auto_Volume volume = NULL ) \
        : DirectoryTestCase( "Directory_" # TestCaseName "Test", volume ) \
      { } \
      void runTest(); \
    };\
      inline void Directory_##TestCaseName##Test::runTest()


    YIELD_PLATFORM_DIRECTORY_TEST_CASE( readdir )
    {
      Directory::auto_Entry entry = get_directory()->readdir();
      ASSERT_EQUAL( entry->get_name(), "." );
      entry = get_directory()->readdir();
      ASSERT_EQUAL( entry->get_name(), ".." );
      entry = get_directory()->readdir();
      ASSERT_EQUAL( entry->get_name(), YIELD_PLATFORM_FILE_TEST_FILE_NAME );
    }


    template <class VolumeType = Volume>
    class DirectoryTestSuite : public yunit::TestSuite
    {
    public:
      DirectoryTestSuite( const std::string& name )
        : TestSuite( name )
      {
        YIELD::platform::auto_Volume volume = new VolumeType;
        addTest( new Directory_readdirTest( volume ) );
      }
    };
  };
};


#endif
