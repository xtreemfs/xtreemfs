// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the yidl project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the yidl project nor the
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


#ifndef _YUNIT_H_
#define _YUNIT_H_

#include <cstdio>
#include <iostream>
#include <string>
#include <vector>


#define ASSERT_TRUE( stat ) { if ( !( ( stat ) == true ) ) throw yunit::AssertionException( __FILE__, __LINE__, #stat" != true" ); }
#define ASSERT_FALSE( stat ) { if ( !( ( stat ) == false ) ) throw yunit::AssertionException( __FILE__, __LINE__, #stat" != false" ); }
#define ASSERT_EQUAL( stat_a, stat_b ) { if ( !( ( stat_a ) == ( stat_b ) ) ) throw yunit::AssertionException( __FILE__, __LINE__, #stat_a" != "#stat_b ); }
#define ASSERT_NOTEQUAL( stat_a, stat_b ) { if ( !( ( stat_a ) != ( stat_b ) ) ) throw yunit::AssertionException( __FILE__, __LINE__, #stat_a" == "#stat_b ); }
#define FAIL() throw yunit::AssertionException( __FILE__, __LINE__ );

#define TEST_SUITE_EX( TestSuiteName, TestSuiteType ) \
  yunit::TestSuite& TestSuiteName##TestSuite() { static TestSuiteType* ts = new TestSuiteType( #TestSuiteName ); return *ts; } \
class TestSuiteName##TestSuiteDest { public: ~TestSuiteName##TestSuiteDest() { delete &TestSuiteName##TestSuite(); } }; \
TestSuiteName##TestSuiteDest TestSuiteName##TestSuiteDestObj;

#define TEST_SUITE( TestSuiteName ) TEST_SUITE_EX( TestSuiteName, yunit::TestSuite )

#define TEST_CASE_EX( TestSuiteName, TestCaseName, TestCaseType ) \
extern yunit::TestSuite& TestSuiteName##TestSuite(); \
class TestSuiteName##_##TestCaseName##Test : public TestCaseType \
{ \
public:\
  TestSuiteName##_##TestCaseName##Test() \
  : TestCaseType( # TestSuiteName "_" # TestCaseName "Test" ) \
  { \
    TestSuiteName##TestSuite().addTest( this ); \
  } \
  void runTest(); \
};\
TestSuiteName##_##TestCaseName##Test* TestSuiteName##_##TestCaseName##Test_inst = new TestSuiteName##_##TestCaseName##Test;\
void TestSuiteName##_##TestCaseName##Test::runTest()

#define TEST_CASE( TestSuiteName, TestCaseName ) TEST_CASE_EX( TestSuiteName, TestCaseName, yunit::TestCase )

#ifdef BUILDING_STANDALONE_TEST
#define TEST_MAIN( TestSuiteName ) \
  int main( int argc, char** argv ) { return yunit::TestRunner().run( TestSuiteName##TestSuite() ); }
#else
#define TEST_MAIN( TestSuiteName )
#endif


namespace yunit
{
  class TestResult;
  class TestSuite;


  class AssertionException : public std::exception
  {
  public:
    AssertionException( const char* file_name, int line_number, const char* info = "" )
    {
#ifdef _WIN32
      _snprintf_s( what_buffer, 1024, "line number %d in %s (%s)", line_number, file_name, info );
#else
      snprintf( what_buffer, 1024, "line number %d in %s (%s)", line_number, file_name, info );
#endif
    }

    // std::exception
    virtual const char* what() const throw() { return what_buffer; }

  private:
    char what_buffer[1024];
  };


  class TestCase
  {
  public:
    TestCase( const std::string& name )
      : name( name )
    { }

    virtual ~TestCase() { }

    virtual void setUp() { }
    virtual void runTest() { }
    virtual void run( TestResult& ) { runTest(); }
    virtual void tearDown() { }
    const char* shortDescription() const { return name.c_str(); }

  protected:
    std::string name;
  };


  class TestResult
  {
  public:
    virtual ~TestResult() { }
  };


  class TestRunner
  {
  public:
    virtual ~TestRunner() { }

    virtual int run( TestSuite& test_suite );
  };


  class TestSuite : private std::vector<TestCase*>
  {
  public:
    TestSuite( const std::string& name )
      : name( name )
    { }

    virtual ~TestSuite()
    {
      for ( iterator test_case_i = begin(); test_case_i != end(); test_case_i++ )
        delete *test_case_i;
    }

    void addTest( TestCase* test_case )
    {
      push_back( test_case );
    }

    const std::string& get_name() const { return name; }

    virtual void run( TestResult& test_result )
    {
      for ( iterator test_i = begin(); test_i != end(); test_i++ )
      {
        bool called_runTest = false, called_tearDown = false;

        try
        {
          std::cout << ( *test_i )->shortDescription();
          ( *test_i )->setUp();
          called_runTest = true;
          ( *test_i )->run( test_result );
          called_tearDown = true;
          ( *test_i )->tearDown();
          std::cout << ": passed";
        }
        catch ( yunit::AssertionException& exc )
        {
          std::cout << " failed: " << exc.what();
        }
        catch ( std::exception& exc )
        {
          std::cout << " threw exception: " << exc.what();
        }
        catch ( ... )
        {
          std::cout << " threw unknown non-exception";
        }

        std::cout << std::endl;

        if ( called_runTest && !called_tearDown )
          try { ( *test_i )->tearDown(); } catch ( ... ) { }

       // ret_code |= 1;
      }
    }

  private:
    std::string name;
  };


  inline int TestRunner::run( TestSuite& test_suite )
  {
    TestResult* test_result = new TestResult;
    test_suite.run( *test_result );
    return 0;
  }
};


#endif
