// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _YUNIT_H_
#define _YUNIT_H_

#include <cstdio>
#include <iostream>


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
  : TestCaseType( TestSuiteName##TestSuite(), # TestCaseName ) \
  { } \
  void runTest(); \
};\
TestSuiteName##_##TestCaseName##Test TestSuiteName##_##TestCaseName##Test_inst;\
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
    TestCase( TestSuite& test_suite, const std::string& name );
    virtual ~TestCase() { }

    virtual void setUp() { }
    virtual void runTest() { }
    virtual void run( TestResult& ) { runTest(); }
    virtual void tearDown() { }
    const char* shortDescription() const { return short_description.c_str(); }

  protected:
    std::string short_description;
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
      for ( std::vector<TestCase*>::size_type test_case_i = 0; test_case_i < size(); test_case_i++ )
      {
        if ( own_test_cases[test_case_i] )
          delete at( test_case_i );
      }
    }

    void addTest( TestCase* test_case, bool own_test_case = true ) // for addTest( new ... )
    {
      push_back( test_case );
      own_test_cases.push_back( own_test_case );
    }

    void addTest( TestCase& test_case, bool own_test_case = false ) // for addTest( *this )
    {
      push_back( &test_case );
      own_test_cases.push_back( own_test_case );
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

    std::vector<bool> own_test_cases;
  };


  inline TestCase::TestCase( TestSuite& test_suite, const std::string& name )
    : short_description( test_suite.get_name() + "_" + name )
  {
    test_suite.addTest( *this );
  }


  inline int TestRunner::run( TestSuite& test_suite )
  {
    TestResult* test_result = new TestResult;
    test_suite.run( *test_result );
    return 0;
  }
};

#endif
