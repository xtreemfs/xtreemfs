// Copyright 2003-2008 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _YIELD_PLATFORM_H_
#define _YIELD_PLATFORM_H_

#define __STDC_LIMIT_MACROS
#ifdef _WIN32
#include <hash_map>
#include <msstdint.h>
typedef int ssize_t;
extern "C"
{
  __declspec( dllimport ) void __stdcall DebugBreak();
  __declspec( dllimport ) long __stdcall InterlockedCompareExchange( volatile long* current_value, long new_value, long old_value );
  __declspec( dllimport ) long long __stdcall InterlockedCompareExchange64( volatile long long* current_value, long long new_value, long long old_value );
  __declspec( dllimport ) long __stdcall InterlockedIncrement( volatile long* );
  __declspec( dllimport ) long __stdcall InterlockedDecrement( volatile long* );
}
#else
#if defined(__GNUC__) && ( ( __GNUC__ == 4 && __GNUC_MINOR__ >= 2 ) || __GNUC__ > 4 )
#define __YIELD_HAVE_GNUC_ATOMIC_OPS_INTRINSICS 1
#elif defined(__sun)
#include <atomic.h>
#endif
#include <errno.h>
#if !defined(__sun) && ( __GNUC__ >= 4 || ( __GNUC__ == 4 && __GNUC_MINOR__ >= 3 ) )
#include <tr1/unordered_map>
#else
#include <ext/hash_map>
#endif
#include <limits.h>
#include <pthread.h>
#if defined(__MACH__)
#include <mach/semaphore.h>
#elif !defined(_WIN32)
#include <semaphore.h>
#endif
#ifdef YIELD_HAVE_POSIX_FILE_AIO
#include <signal.h> // For sigval_t
#endif
#include <stdint.h>
#include <sys/uio.h> // For struct iovec
#include <unistd.h>
#endif

#include <algorithm>
#include <cstring>
#include <exception>
#include <fcntl.h>
#include <memory>
#include <iostream>
#include <sstream>
#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <sys/stat.h>
#include <sys/types.h>
#include <utility>
#include <vector>

#ifdef _WIN32
#include <banned.h>
#endif

#ifdef _WIN32
#ifndef DLLEXPORT
#define DLLEXPORT extern "C" __declspec(dllexport)
#endif
#ifndef PATH_MAX
#define PATH_MAX 260
#endif
#define O_SYNC 010000
#define O_ASYNC 020000
#define O_DIRECT 040000
#define O_HIDDEN 0100000
#define PATH_SEPARATOR '\\'
#define PATH_SEPARATOR_STRING "\\"
#define PATH_SEPARATOR_WIDE_STRING L"\\"
#ifndef SHLIBSUFFIX
#define SHLIBSUFFIX "dll"
#endif
#ifndef UNICODE
#define UNICODE 1
#endif
#else
#ifndef DLLEXPORT
#if defined(__GNUC__) && __GNUC__ >= 4
#define DLLEXPORT extern "C" __attribute__ ( ( visibility( "default" ) ) )
#else
#define DLLEXPORT extern "C"
#endif
#endif
#define PATH_SEPARATOR '/'
#define PATH_SEPARATOR_STRING "/"
#ifndef SHLIBSUFFIX
#if defined(__MACH__)
#define SHLIBSUFFIX "dylib"
#else
#define SHLIBSUFFIX "so"
#endif
#ifdef __sun
#define YIELD yield_
#else
#define YIELD yield
#endif
#endif
#endif

#define ASSERT_TRUE( stat ) { if ( !( ( stat ) == true ) ) throw YIELD::AssertionException( __FILE__, __LINE__, #stat" != true" ); }
#define ASSERT_FALSE( stat ) { if ( !( ( stat ) == false ) ) throw YIELD::AssertionException( __FILE__, __LINE__, #stat" != false" ); }
#define ASSERT_EQUAL( stat_a, stat_b ) { if ( !( ( stat_a ) == ( stat_b ) ) ) throw YIELD::AssertionException( __FILE__, __LINE__, #stat_a" != "#stat_b ); }
#define ASSERT_NOTEQUAL( stat_a, stat_b ) { if ( !( ( stat_a ) != ( stat_b ) ) ) throw YIELD::AssertionException( __FILE__, __LINE__, #stat_a" == "#stat_b ); }
#define FAIL() throw YIELD::AssertionException( __FILE__, __LINE__ );

#define NS_IN_US 1000ULL
#define NS_IN_MS 1000000ULL
#define NS_IN_S  1000000000ULL
#define MS_IN_S  1000
#define US_IN_S  1000000

#define YIELD_CUCKOO_HASH_TABLE_MAX_LG_TABLE_SIZE_IN_BINS 20

// #define YIELD_DEBUG_REFERENCE_COUNTING 1

#define YIELD_EXCEPTION_WHAT_BUFFER_LENGTH 128

#define YIELD_FILE_PROTOTYPES \
  virtual bool close(); \
  bool datasync(); \
  bool flush(); \
  virtual YIELD::auto_Object<YIELD::Stat> getattr(); \
  virtual bool getxattr( const std::string& name, std::string& out_value ); \
  virtual bool listxattr( std::vector<std::string>& out_names ); \
  ssize_t read( void* buffer, size_t buffer_len, uint64_t offset ); \
  virtual bool removexattr( const std::string& name ); \
  virtual bool setxattr( const std::string& name, const std::string& value, int flags ); \
  virtual bool sync(); \
  virtual bool truncate( uint64_t offset ); \
  ssize_t writev( const iovec* buffers, uint32_t buffers_count, uint64_t offset );

#define YIELD_MARSHALLER_PROTOTYPES \
  virtual void writeBool( const Declaration& decl, bool value ); \
  virtual void writeDouble( const Declaration& decl, double value ); \
  virtual void writeInt64( const Declaration& decl, int64_t value ); \
  virtual void writeMap( const Declaration& decl, YIELD::Object& value ); \
  virtual void writeSequence( const Declaration& decl, YIELD::Object& value ); \
  virtual void writeString( const Declaration&, const char* value, size_t value_len ); \
  virtual void writeStruct( const Declaration& decl, YIELD::Object& value );

#define YIELD_OBJECT_PROTOTYPES( type_name, tag ) \
    type_name & incRef() { return YIELD::Object::incRef( *this ); } \
    const static uint32_t __tag = static_cast<uint32_t>( tag ); \
    virtual uint32_t get_tag() const { return __tag; } \
    const char* get_type_name() const { return #type_name; }

#define YIELD_OBJECT_TAG( type ) type::__tag

#define YIELD_STRING_HASH_NEXT( c, hash ) hash = hash ^ ( ( hash << 5 ) + ( hash >> 2 ) + c )

#define YIELD_TEST_SUITE_EX( TestSuiteName, TestSuiteType ) \
  YIELD::TestSuite& TestSuiteName##TestSuite() { static TestSuiteType* ts = new TestSuiteType( #TestSuiteName ); return *ts; } \
class TestSuiteName##TestSuiteDest { public: ~TestSuiteName##TestSuiteDest() { delete &TestSuiteName##TestSuite(); } }; \
TestSuiteName##TestSuiteDest TestSuiteName##TestSuiteDestObj;

#define YIELD_TEST_SUITE( TestSuiteName ) YIELD_TEST_SUITE_EX( TestSuiteName, YIELD::TestSuite )

#define YIELD_TEST_CASE_EX( TestSuiteName, TestCaseName, TestCaseType ) \
extern YIELD::TestSuite& TestSuiteName##TestSuite(); \
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

#define YIELD_TEST_CASE( TestSuiteName, TestCaseName ) YIELD_TEST_CASE_EX( TestSuiteName, TestCaseName, YIELD::TestCase )

#ifdef YIELD_BUILDING_STANDALONE_TEST
#define YIELD_TEST_MAIN( TestSuiteName ) \
  int main( int argc, char** argv ) { return YIELD::TestRunner().run( TestSuiteName##TestSuite() ); }
#else
#define YIELD_TEST_MAIN( TestSuiteName )
#endif

#define YIELD_UNMARSHALLER_PROTOTYPES \
  virtual bool readBool( const Declaration& decl ); \
  virtual double readDouble( const Declaration& decl ); \
  virtual int64_t readInt64( const Declaration& decl ); \
  virtual Object* readMap( const Declaration& decl, Object* value = NULL ); \
  virtual Object* readSequence( const Declaration& decl, Object* value = NULL ); \
  virtual void readString( const Declaration& decl, std::string& ); \
  virtual Object* readStruct( const Declaration& decl, Object* value = NULL );  

#define YIELD_VOLUME_PROTOTYPES \
    virtual bool access( const YIELD::Path& path, int amode ); \
    virtual bool chmod( const YIELD::Path& path, mode_t mode ); \
    virtual bool chown( const YIELD::Path& path, int32_t tag, int32_t gid ); \
    virtual YIELD::auto_Object<YIELD::Stat> getattr( const YIELD::Path& path ); \
    virtual bool getxattr( const YIELD::Path& path, const std::string& name, std::string& out_value ); \
    virtual bool link( const YIELD::Path& old_path, const YIELD::Path& new_path ); \
    virtual bool listxattr( const YIELD::Path& path, std::vector<std::string>& out_names ); \
    virtual bool mkdir( const YIELD::Path& path, mode_t mode ); \
    virtual YIELD::auto_Object<YIELD::File> open( const YIELD::Path& path, uint32_t flags, mode_t mode, uint32_t attributes ); \
    virtual bool readdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, YIELD::Volume::readdirCallback& callback ); \
    virtual YIELD::auto_Object<YIELD::Path> readlink( const YIELD::Path& path ); \
    virtual bool removexattr( const YIELD::Path& path, const std::string& name ); \
    virtual bool rename( const YIELD::Path& from_path, const YIELD::Path& to_path ); \
    virtual bool rmdir( const YIELD::Path& path ); \
    virtual bool setattr( const YIELD::Path& path, uint32_t file_attributes ); \
    virtual bool setxattr( const YIELD::Path& path, const std::string& name, const std::string& value, int flags ); \
    virtual bool statvfs( const YIELD::Path& path, struct statvfs* ); \
    virtual bool symlink( const YIELD::Path& old_path, const YIELD::Path& new_path ); \
    virtual bool truncate( const YIELD::Path& path, uint64_t new_size ); \
    virtual bool unlink( const YIELD::Path& path ); \
    virtual bool utimens( const YIELD::Path& path, const YIELD::Time& atime, const YIELD::Time& mtime, const YIELD::Time& ctime ); \
    virtual YIELD::Path volname( const YIELD::Path& path );


#ifdef _WIN32
struct _BY_HANDLE_FILE_INFORMATION;
typedef _BY_HANDLE_FILE_INFORMATION BY_HANDLE_FILE_INFORMATION;
struct _FILETIME;
typedef _FILETIME FILETIME;
#ifdef _WIN64
typedef uint64_t fsblkcnt_t;
typedef uint64_t fsfilcnt_t;
#else
typedef uint32_t fsblkcnt_t;
typedef uint32_t fsfilcnt_t;
#endif
struct iovec
{
  size_t iov_len;
  void* iov_base;
};
typedef int mode_t;
// POSIX statvfs
struct statvfs
{
  unsigned long f_bsize;    // File system block size.
  unsigned long f_frsize;   // Fundamental file system block size.
  fsblkcnt_t    f_blocks;   // Total number of blocks on file system in units of f_frsize.
  fsblkcnt_t    f_bfree;    // Total number of free blocks.
  fsblkcnt_t    f_bavail;   // Number of free blocks available to
                           // non-privileged process.
  fsfilcnt_t    f_files;    // Total number of file serial numbers.
  fsfilcnt_t    f_ffree;    // Total number of free file serial numbers.
  fsfilcnt_t    f_favail;   // Number of file serial numbers available to
                           // non-privileged process.
  unsigned long f_fsid;     // File system ID.
  unsigned long f_flag;     // Bit mask of f_flag values.
  unsigned long f_namemax;  // Maximum filename length.
};
struct _WIN32_FIND_DATAW;
typedef _WIN32_FIND_DATAW WIN32_FIND_DATAW;
typedef WIN32_FIND_DATAW WIN32_FIND_DATA;
#else
struct statvfs;
struct timespec;
#endif
struct timeval;


#ifndef _WIN32
inline void memcpy_s( void* dest, size_t dest_size, const void* src, size_t count )
{
  memcpy( dest, src, count );
}
#endif


namespace YIELD
{
  class Marshaller;
  class Path;
  class Stat;
  class Unmarshaller;
  class TestResult;
  class TestSuite;


  static inline int32_t atomic_cas( volatile int32_t* current_value, int32_t new_value, int32_t old_value )
  {
#if defined(_WIN32)
    return InterlockedCompareExchange( reinterpret_cast<volatile long*>( current_value ), new_value, old_value );
#elif defined(__YIELD_HAVE_GNUC_ATOMIC_OPS_INTRINSICS)
    return __sync_val_compare_and_swap( current_value, old_value, new_value );
#elif defined(__sun)
    return atomic_cas_32( current_value, old_value, new_value );
#elif defined(__i386__) || defined(__x86_64__)
    int32_t prev;
    asm volatile(	"lock\n"
            "cmpxchgl %1,%2\n"
          : "=a" ( prev )
                : "r" ( new_value ), "m" ( *current_value ) , "0" ( old_value )
                : "memory"
              );
    return prev;
#elif defined(__ppc__)
    int32_t prev;
    asm volatile(	"					\n\
            1:	lwarx   %0,0,%2 \n\
            cmpw    0,%0,%3 \n\
            bne     2f		\n\
            stwcx.  %4,0,%2 \n\
            bne-    1b		\n\
            sync\n"
            "2:"
          : "=&r" ( prev ), "=m" ( *current_value )
                : "r" ( current_value ), "r" ( old_value ), "r" ( new_value ), "m" ( *current_value )
                : "cc", "memory"
              );
    return prev;
#else
#error
#endif
  }

  static inline int64_t atomic_cas( volatile int64_t* current_value, int64_t new_value, int64_t old_value )
  {
#if defined(_WIN32)
    return InterlockedCompareExchange64( current_value, new_value, old_value );
#elif defined(__YIELD_HAVE_GNUC_ATOMIC_OPS_INTRINSICS)
    return __sync_val_compare_and_swap( current_value, old_value, new_value );
#elif defined(__sun)
    return atomic_cas_64( current_value, old_value, new_value );
#elif defined(__x86_64__)
    int64_t prev;
    asm volatile(	"lock\n"
            "cmpxchgq %1,%2\n"
          : "=a" ( prev )
                : "r" ( new_value ), "m" ( *current_value ) , "0" ( old_value )
                : "memory"
              );
    return prev;
#elif defined(__ppc__)
    int64_t prev;
    asm volatile(	"					\n\
            1:	ldarx   %0,0,%2 \n\
            cmpd    0,%0,%3 \n\
            bne     2f		\n\
            stdcx.  %4,0,%2 \n\
            bne-    1b		\n\
            sync\n"
            "2:"
          : "=&r" ( prev ), "=m" ( *current_value )
                : "r" ( current_value ), "r" ( old_value ), "r" ( new_value ), "m" ( *current_value )
                : "cc", "memory"
              );
    return prev;
#else
    // 32-bit systems
    *((int*)0) = 0xabadcafe;
    return 0;
#endif
  }

  static inline int32_t atomic_dec( volatile int32_t* current_value )
  {
#if defined(_WIN32)
    return InterlockedDecrement( reinterpret_cast<volatile long*>( current_value ) );
#elif defined(__YIELD_HAVE_GNUC_ATOMIC_OPS_INTRINSICS)
    return __sync_sub_and_fetch( current_value, 1 );
#elif defined(__sun)
    return atomic_dec_32_nv( current_value );
#else
    int32_t old_value, new_value;

    do
    {
      old_value = *current_value;
#ifdef _DEBUG
      if ( old_value == 0 )  { *((int*)0) = 0xabadcafe; }
#endif
      new_value = old_value - 1;
    }
    while ( atomic_cas( current_value, new_value, old_value ) != old_value );

    return new_value;
#endif
  }

  static inline int32_t atomic_inc( volatile int32_t* current_value )
  {
#if defined(_WIN32)
    return InterlockedIncrement( reinterpret_cast<volatile long*>( current_value ) );
#elif defined(__YIELD_HAVE_GNUC_ATOMIC_OPS_INTRINSICS)
    return __sync_add_and_fetch( current_value, 1 );
#elif defined(__sun)
    return atomic_inc_32_nv( current_value );
#else
    int32_t old_value, new_value;

    do
    {
      old_value = *current_value;
      new_value = old_value + 1;
    }
    while ( atomic_cas( current_value, new_value, old_value ) != old_value );

    return new_value;
#endif
  }

#ifdef _WIN32
  static inline void DebugBreak()
  {
    ::DebugBreak();
  }
#else
  static inline void DebugBreak()
  {
    *((int*)0) = 0xabadcafe;
  }
#endif

  static inline uint32_t string_hash( const char* str )
  {
    uint32_t hash = 0;

    while ( *str != 0 )
    {
      YIELD_STRING_HASH_NEXT( *str, hash );
      str++;
    }

    return hash;
  }

  static inline uint32_t string_hash( const char* str, size_t str_len )
  {
    size_t str_i = 0;
    uint32_t hash = 0;

    while ( str_i < str_len )
    {
      YIELD_STRING_HASH_NEXT( str[str_i], hash );
      str_i++;
    }

    return hash;
  }

  static inline uint32_t string_hash( const std::string& str )
  {
    return string_hash( str.c_str(), str.size() );
  }

  static inline uint32_t string_hash( const unsigned char* str, size_t str_len )
  {
    size_t str_i = 0;
    uint32_t hash = 0;

    while ( str_i < str_len )
    {
      YIELD_STRING_HASH_NEXT( str[str_i], hash );
      str_i++;
    }

    return hash;
  }


  class Exception : public std::exception
  {
  public:
    static uint32_t get_errno();
    static void set_errno( uint32_t error_code );

    static std::string strerror() { return strerror( get_errno() ); }
    static std::string strerror( uint32_t error_code );
    static void strerror( std::string& out_str ) { strerror( get_errno(), out_str ); }
    static void strerror( uint32_t error_code, std::string& out_str );
    static void strerror( char* out_str, size_t out_str_len ) { return strerror( get_errno(), out_str, out_str_len ); }
    static void strerror( uint32_t error_code, char* out_str, size_t out_str_len );

    Exception(); // Gets error_code from errno
    Exception( uint32_t error_code );
    Exception( const char* what ) { init( what ); } // Copies what
    Exception( const std::string& what ) { init( what.c_str() ); } // Copies what
    Exception( const Exception& other ) { init( other.what_buffer ); }
    virtual ~Exception() throw() { }

    // std::exception
    virtual const char* what() const throw() { return what_buffer; }

  protected:
    char what_buffer[YIELD_EXCEPTION_WHAT_BUFFER_LENGTH];

  private:
    void init( const char* what );
  };


  class Object
  {      
  public:
    Object() : refcnt( 1 )
    { }

    static inline void decRef( Object& object )
    {
#ifdef YIELD_DEBUG_REFERENCE_COUNTING
      if ( atomic_dec( &object.refcnt ) < 0 )
        DebugBreak();
#else
      if ( atomic_dec( &object.refcnt ) == 0 )
        delete &object;
#endif
    }

    static inline void decRef( Object* object )
    {
      if ( object )
        Object::decRef( *object );
    }

    template <class ObjectType>
    static inline ObjectType& incRef( ObjectType& object )
    {
#ifdef YIELD_DEBUG_REFERENCE_COUNTING
      if ( object.refcnt <= 0 )
        DebugBreak();
#endif
      atomic_inc( &object.refcnt );
      return object;
    }

    template <class ObjectType>
    static inline ObjectType* incRef( ObjectType* object )
    {
      if ( object )
        incRef( *object );
      return object;
    }

    inline Object& incRef()
    {
      incRef( *this );
      return *this;
    }

    virtual uint64_t get_size() const { return 0; } // For arrays
    virtual uint32_t get_tag() const = 0;
    virtual const char* get_type_name() const = 0;
    virtual void marshal( Marshaller& ) { }
    virtual void unmarshal( Unmarshaller& ) { }

  protected:
    virtual ~Object()
    { }

  private:
    volatile int32_t refcnt;
  };


  class Time
  {
  public:
    // Unix epoch times (from January 1, 1970)
    static uint64_t getCurrentUnixTimeNS();
    static double getCurrentUnixTimeMS() { return static_cast<double>( getCurrentUnixTimeNS() ) / static_cast<double>( NS_IN_MS ); }
    static double getCurrentUnixTimeS() { return static_cast<double>( getCurrentUnixTimeNS() ) / static_cast<double>( NS_IN_S ); }

    Time() : unix_time_ns( getCurrentUnixTimeNS() ) { }
    Time( uint64_t unix_time_ns ) : unix_time_ns( unix_time_ns ) { }
    Time( const struct timeval& );
#ifdef _WIN32
    Time( const FILETIME& );
    Time( const FILETIME* );
#else
    Time( const struct timespec& );
#endif
    Time( const Time& other ) : unix_time_ns( other.unix_time_ns ) { }

    void as_common_log_date_time( char* out_str, uint8_t out_str_len ) const;
    void as_http_date_time( char* out_str, uint8_t out_str_len ) const;
    void as_iso_date( char* out_str, uint8_t out_str_len ) const;
    void as_iso_date_time( char* out_str, uint8_t out_str_len ) const;
    uint64_t as_unix_time_ns() const { return unix_time_ns; }
    uint64_t as_unix_time_ms() const { return unix_time_ns / NS_IN_S; }
    uint32_t as_unix_time_s() const { return static_cast<uint32_t>( unix_time_ns / NS_IN_S ); }
    operator uint64_t() const { return unix_time_ns; }
    operator struct timeval() const;
#ifdef _WIN32
    operator FILETIME() const;
#else
    operator struct timespec() const;
#endif
    Time operator+( const Time& other ) const { return Time( unix_time_ns + other.unix_time_ns ); }
    Time operator-( const Time& other ) const { return Time( unix_time_ns - other.unix_time_ns ); }
    bool operator<( const Time& other ) const { return unix_time_ns < other.unix_time_ns; }
    bool operator>( const Time& other ) const { return unix_time_ns > other.unix_time_ns; }
    bool operator>=( const Time& other ) const { return unix_time_ns >= other.unix_time_ns; }
    Time& operator=( uint64_t unix_time_ns ) { this->unix_time_ns = unix_time_ns; return *this; }
    operator std::string() const;

  private:
    uint64_t unix_time_ns;
  };

  static inline std::ostream& operator<<( std::ostream& os, const Time& time )
  {
    char iso_date_time[64];
    time.as_iso_date_time( iso_date_time, 64 );
    os << iso_date_time;
    return os;
  }


  class AssertionException : public Exception
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


  template <class ObjectType = Object>
  class auto_Object // Like auto_ptr, but using Object::decRef instead of delete; an operator delete( void* ) on Object doesn't work, because the object is destructed before that call
  {
  public:
    auto_Object() : object( 0 ) { }
    auto_Object( ObjectType* object ) : object( object ) { }
    auto_Object( ObjectType& object ) : object( &object ) { }
    auto_Object( const auto_Object<ObjectType>& other ) { object = Object::incRef( other.object ); }
    ~auto_Object() { Object::decRef( object ); }

    inline ObjectType* get() const { return object; }
    auto_Object& operator=( const auto_Object<ObjectType>& other ) { Object::decRef( this->object ); object = Object::incRef( other.object ); return *this; }
    auto_Object& operator=( ObjectType* object ) { Object::decRef( this->object ); this->object = object; return *this; }
    inline bool operator==( const auto_Object<ObjectType>& other ) const { return object == other.object; }
    inline bool operator==( const ObjectType* other ) const { return object == other; }
    inline bool operator!=( const ObjectType* other ) const { return object != other; }
    // operator ObjectType*() const { return object; } // Creates sneaky bugs
    inline ObjectType* operator->() const { return get(); }
    inline ObjectType& operator*() const { return *get(); }
    inline ObjectType* release() { ObjectType* temp_object = object; object = 0; return temp_object; }
    inline void reset( ObjectType* object ) { Object::decRef( this->object ); this->object = object; }

  private:
    ObjectType* object;
  };


  class CountingSemaphore
  {
  public:
    CountingSemaphore();
    ~CountingSemaphore();

    bool acquire(); // Blocking
    bool timed_acquire( uint64_t timeout_ns ); // May block for timeout_ns
    bool try_acquire(); // Never blocks
    void release();

  private:
#if defined(_WIN32)
    void* hSemaphore;
#elif defined(__MACH__)
    semaphore_t sem;
#else
    sem_t sem;
#endif
  };


  template <class ValueType>
  class CuckooHashTable
  {
  public:
    CuckooHashTable( uint8_t lg_table_size_in_bins = 6, uint8_t records_per_bin = 8, uint8_t table_count = 2 )
      : lg_table_size_in_bins( lg_table_size_in_bins ), records_per_bin( records_per_bin ), table_count( table_count )
    {
      per_table_records_filled = new uint32_t[table_count];
      table_size_in_bins = table_size_in_records = 0;
      resizeTables( lg_table_size_in_bins );
      clear();
    }

    ~CuckooHashTable()
    {
      delete [] per_table_records_filled;
      delete [] tables;
    }

    CuckooHashTable<ValueType>& operator=( const CuckooHashTable<ValueType>& ) { return *this; }

    void clear()
    {
      std::memset( tables, 0, sizeof( Record ) * table_size_in_records * table_count );
      for ( uint8_t table_i = 0; table_i < table_count; table_i++ ) per_table_records_filled[table_i] = 0;
      total_records_filled = 0;
    }

    inline bool empty() const { return size() == 0; }

    inline ValueType erase( const std::string& external_key ) { return erase( external_key.c_str() ); }
    inline ValueType erase( const char* external_key ) { return erase( string_hash( external_key ) ); }
    inline ValueType erase( const char* external_key, size_t external_key_len ) { return erase( string_hash( external_key, external_key_len ) ); }
    ValueType erase( uint32_t external_key )
    {
      uint32_t internal_key = external_key;
      Record* record;

      for ( uint8_t table_i = 0; table_i < table_count; table_i++ )
      {
        record = getRecord( table_i, internal_key, external_key );
        if ( record )
        {
          ValueType old_value = record->value;
          std::memset( record, 0, sizeof( Record ) );
          total_records_filled--;
          per_table_records_filled[table_i]--;
          return old_value;
        }
        else
          internal_key = rehashKey( internal_key );
      }

      return 0;
    }

    inline ValueType find( const std::string& external_key ) const { return find( external_key.c_str() ); }
    inline ValueType find( const char* external_key ) const { return find( string_hash( external_key ) ); }
    inline ValueType find( const char* external_key, size_t external_key_len ) const { return find( string_hash( external_key, external_key_len ) ); }
    ValueType find( uint32_t external_key ) const
    {
      uint32_t internal_key = external_key;
      Record* record;

      for ( uint8_t table_i = 0; table_i < table_count; table_i++ )
      {
        record = getRecord( table_i, internal_key, external_key );
        if ( record )
          return record->value;
        else
          internal_key = rehashKey( internal_key );
      }

      return 0;
    }

    inline void insert( const std::string& external_key, ValueType value ) { insert( external_key.c_str(), value ); }
    inline void insert( const char* external_key, ValueType value ) { insert( string_hash( external_key ), value ); }
    inline void insert( const char* external_key, size_t external_key_len, ValueType value ) { insert( string_hash( external_key, external_key_len ), value ); }
    void insert( uint32_t external_key, ValueType value )
    {
      while ( lg_table_size_in_bins < YIELD_CUCKOO_HASH_TABLE_MAX_LG_TABLE_SIZE_IN_BINS )
      {
        if ( insertWithoutResize( external_key, value ) )
          return;
        else
          resizeTables( lg_table_size_in_bins + 1 ); // Will set lg_table_size_in_bins
      }

//			DebugBreak();
    }

    class iterator
    {
    public:
      iterator( CuckooHashTable<ValueType>& cht, size_t record_i ) : cht( cht ), record_i( record_i ) { }
      iterator( const iterator& other ) : cht( other.cht ), record_i( other.record_i ) { }

      iterator& operator=( const iterator& other ) 
      { 
        if ( &cht == &other.cht )
          this->record_i = other.record_i;
        return *this;        
      }

      iterator& operator++()
      {
        return ++( *this );
      }

      iterator& operator++( int )
      {
        record_i++;
        while ( record_i < ( cht.table_count * cht.table_size_in_records ) &&
              cht.tables[record_i].external_key == 0 )
          record_i++;
        return *this;
      }

      ValueType& operator*()
      {
        return cht.tables[record_i].value;
      }

      bool operator!=( const iterator& other ) const
      {
        return record_i != other.record_i;
      }

    private:
      CuckooHashTable<ValueType>& cht;
      size_t record_i;
    };

    iterator begin() { return iterator( *this, 0 ); }

    iterator end()
    {
      if ( total_records_filled > 0 )
      {
        size_t record_i = table_count * table_size_in_records;
        while ( record_i > 0 )
        {
          if ( tables[record_i].external_key != 0 )
            break;
          record_i--;
        }
        return iterator( *this, record_i );
      }
      else
        return iterator( *this, 0 );
    }

    inline size_t size() const { return total_records_filled; }

  private:
    uint8_t lg_table_size_in_bins, records_per_bin, table_count;
    unsigned table_size_in_records, table_size_in_bins;

    struct Record
    {
      uint32_t external_key;
      ValueType value;
    };

    Record* tables;
    uint32_t total_records_filled, *per_table_records_filled;


    Record* getRecord( uint8_t table_i, uint32_t internal_key, uint32_t external_key ) const
    {
      Record* table = tables + ( table_i * table_size_in_records );
      uint32_t bin_i = internal_key & ( table_size_in_bins -1 ), bin_i_end = bin_i + records_per_bin;
      for ( ; bin_i < bin_i_end; bin_i++ )
      {
        if ( table[bin_i].external_key == external_key )
        {
          //if ( bin_i_end - bin_i < records_per_bin ) DebugBreak();
          return &table[bin_i];
        }
      }
      return 0;
    }

    inline uint32_t rehashKey( uint32_t key ) const
    {
      return key ^ ( key >> lg_table_size_in_bins );
    }

    bool insertWithoutResize( uint32_t external_key, ValueType value )
    {
      if ( find( external_key ) == value )
        return true;

      uint32_t internal_key = external_key;
      Record* record;

      for ( uint8_t table_i = 0; table_i < table_count; table_i++ )
      {
        record = getRecord( table_i, internal_key, 0 ); // Get an empty record

        if ( record )
        {
          record->external_key = external_key;
          record->value = value;
          total_records_filled++;
          per_table_records_filled[table_i]++;
          return true;
        }
        else
          internal_key = rehashKey( internal_key );
      }

      return false;
    }

    void resizeTables( uint8_t new_lg_table_size_in_bins )
    {
      Record* old_tables = tables;
      uint8_t old_lg_table_size_in_bins = lg_table_size_in_bins;
  //		uint32_t old_table_size_in_bins = table_size_in_bins;
      uint32_t old_table_size_in_records = table_size_in_records;

      while ( new_lg_table_size_in_bins < YIELD_CUCKOO_HASH_TABLE_MAX_LG_TABLE_SIZE_IN_BINS )
      {
        lg_table_size_in_bins = new_lg_table_size_in_bins;
        table_size_in_bins = 1 << lg_table_size_in_bins;
        table_size_in_records = table_size_in_bins * records_per_bin;
        tables = new Record[table_size_in_records * table_count];
        this->clear();
        total_records_filled = 0;

        if ( new_lg_table_size_in_bins == old_lg_table_size_in_bins ) // We're being called from the constructor
          return;
        else // There are old records
        {
          for ( uint8_t old_table_i = 0; old_table_i < table_count; old_table_i++ )
          {
            Record* old_table = old_tables + ( old_table_i * old_table_size_in_records );
            for ( uint32_t old_record_i = 0; old_record_i < old_table_size_in_records; old_record_i++ )
            {
              Record* old_record = &old_table[old_record_i];
              if ( old_record->external_key != 0 )
              {
                if ( insertWithoutResize( old_record->external_key, old_record->value ) )
                  continue;
                else
                {
                  new_lg_table_size_in_bins++;
                  break; // Out of the old_record_i for loop
                }
              }
            }

            if ( new_lg_table_size_in_bins != lg_table_size_in_bins ) // We were unable to insert an old record without a resize
              break; // Out of the old_table_i for loop
          }

          if ( new_lg_table_size_in_bins == lg_table_size_in_bins ) // We successfully resized the table
          {
            delete [] old_tables;
            return;
          }
          else // We could not insert all of the old records in the resized table, try again
            delete [] tables;
        }
      }

//			DebugBreak(); // We could not insert all of the old records without going past the max lg_table_size.
              // Something is definitely wrong.
    }
  };


  class File : public Object
  {
  public:
    const static uint32_t DEFAULT_FLAGS = O_RDONLY;
    const static mode_t DEFAULT_MODE = S_IREAD|S_IWRITE;
    const static uint32_t DEFAULT_ATTRIBUTES = 0;


    // Construct from a platform file descriptor; takes ownership of the descriptor
#ifdef _WIN32
    File( void* fd );
#else
    File( int fd );
#endif

    static auto_Object<File> open( const Path& path ) { return open( path, DEFAULT_FLAGS, DEFAULT_MODE ); }
    static auto_Object<File> open( const Path& path, uint32_t flags ) { return open( path, flags, DEFAULT_MODE ); }
    static auto_Object<File> open( const Path& path, uint32_t flags, mode_t mode ) { return open( path, flags, mode, DEFAULT_ATTRIBUTES ); }
    static auto_Object<File> open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes );

#ifdef _WIN32
    operator void*() const { return fd; }
#else
    operator int() const { return fd; }
#endif    

    YIELD_FILE_PROTOTYPES;

    virtual ssize_t read( void* buffer, size_t buffer_len ); // Reads from the current file pointer
    virtual bool seek( uint64_t offset ); // Seeks from the beginning of the file
    virtual bool seek( uint64_t offset, unsigned char whence );
    virtual auto_Object<Stat> stat() { return getattr(); }
    virtual ssize_t write( const void* buffer, size_t buffer_len ); // Writes from the current position
    virtual ssize_t write( const void* buffer, size_t buffer_len, uint64_t offset );
    virtual ssize_t writev( const iovec* buffers, uint32_t buffers_count ); // Writes from the current file pointer

    // Object
    YIELD_OBJECT_PROTOTYPES( File, 1 );

  protected:
    File();
    virtual ~File() { close(); }

  private:
    File( const File& ); // Prevent copying

#ifdef _WIN32
    void* fd;
#else
    int fd;
#endif
  };


  // Adapted from N. Askitis and J. Zobel, "Cache-conscious collision resolution in string hash tables", 2005.
  template <class ValueType>
  class StringArrayHashTable
  {
  public:
    class Slot
    {
    public:
      class Entry
      {
      public:
        Entry( const unsigned char* data_p )
        {
          memcpy_s( &key_len, sizeof( key_len ), data_p, sizeof( key_len ) ); data_p += sizeof( key_len );
          key = data_p; data_p += key_len;
          memcpy_s( &value, sizeof( value ), data_p, sizeof( value ) );
        }

        Entry( const unsigned char* key, uint16_t key_len )
          : key( key ), key_len( key_len ), value( 0 )
        { }

        Entry( const unsigned char* key, uint16_t key_len, ValueType value )
          : key( key ), key_len( key_len ), value( value )
        { }

        Entry( const Entry& other )
          : key( other.key ), key_len( other.key_len ), value( other.value )
        { }

        inline bool empty() const { return get_key_len() == 0; }
        inline const unsigned char* get_key() const { return key; }
        inline const uint16_t get_key_len() const { return key_len; }
        inline ValueType get_value() const { return value; }
        inline uint16_t get_value_offset() const { return sizeof( key_len ) + key_len; }

        inline bool operator==( const Entry& other ) const
        {
          return this->key_len == other.key_len &&
                 std::memcmp( this->key, key, key_len ) == 0;
        }

        unsigned char* serialize( unsigned char* data_p ) const
        {
          memcpy_s( data_p, sizeof( key_len ), &key_len, sizeof( key_len ) ); data_p += sizeof( key_len );
          memcpy_s( data_p, key_len, key, key_len ); data_p += key_len;
          memcpy_s( data_p, sizeof( value ), &value, sizeof( value ) ); data_p += sizeof( value );
          return data_p;
        }

        inline uint16_t size() const { return sizeof( key_len ) + key_len + sizeof( value ); }

        inline bool startswith( const unsigned char* key_prefix, uint16_t key_prefix_len ) const
        {
          return this->key_len >= key_prefix_len &&
                 std::memcmp( this->key, key_prefix, key_prefix_len ) == 0;
        }

      private:
        const unsigned char* key;
        uint16_t key_len;
        ValueType value;
      };


      class const_iterator
      {
      public:
        const_iterator( const unsigned char* data_p ) : data_p( data_p ) { }
        const_iterator( const const_iterator& other ) : data_p( other.data_p ) { }

        const_iterator& operator++()
        {
          return ++( *this );
        }

        const_iterator& operator++( int )
        {
          data_p += Entry( data_p ).size();
          return *this;
        }

        Entry operator*() const
        {
          return Entry( data_p );
        }

        bool operator!=( const const_iterator& other ) const
        {
          return data_p != other.data_p;
        }

      private:
        const unsigned char* data_p;
      };


      Slot()
      {
        data = NULL;
        data_len = 0;
      }

      ~Slot()
      {
        delete [] data;
      }

      inline const_iterator begin() const { return const_iterator( data ); }
      inline bool empty() const { return size() == 0; }
      const_iterator end() const { return const_iterator( data+data_len ); }

      ValueType erase( const unsigned char* key, uint16_t key_len )
      {
        if ( data_len > 0 )
        {
          Entry erase_entry( key, key_len );
          unsigned char *data_p = data, *data_end = data + data_len;
          while ( data_p < data_end )
          {
            Entry test_entry( data_p );
            if ( test_entry == erase_entry )
            {
              if ( test_entry.size() > data_len )
              {
                memmove( data_p, data_p + test_entry.size(), data_len - test_entry.size() );
                data_len -= test_entry.size();
                // Don't re-allocate
              }
              else
              {
                delete [] data;
                data = NULL;
                data_len = 0;
              }
#if defined(_WIN32) && defined(_DEBUG)
              if(_heapchk()!=_HEAPOK) DebugBreak();
#endif
              return test_entry.get_value();
            }
            else
              data_p += test_entry.size();
          }
        }
#if defined(_WIN32) && defined(_DEBUG)
        else if ( data != NULL )
          DebugBreak();
#endif

        return 0;
      }

      void erase_by_prefix( const unsigned char* key_prefix, uint16_t key_prefix_len, std::vector<ValueType>* out_values = NULL )
      {
        if ( data_len > 0 )
        {
          // Assume that a large number of entries will match and copy in what remains instead of using memmove
          unsigned char *new_data = new unsigned char[data_len], *new_data_p = new_data;

          unsigned char *data_p = data, *data_end = data + data_len;
          while ( data_p < data_end )
          {
            Entry test_entry( data_p );
            if ( test_entry.startswith( key_prefix, key_prefix_len ) )
            {
              if ( out_values )
                out_values->push_back( test_entry.get_value() );
            }
            else
            {
              memcpy_s( new_data_p, test_entry.size(), data_p, test_entry.size() );
              new_data_p += test_entry.size();
            }

            data_p += test_entry.size();
          }

          delete [] data;
          data_len = new_data_p - new_data;
          if ( data_len > 0 )
            data = new_data;
          else
            data = NULL;
        }
#if defined(_WIN32) && defined(_DEBUG)
        else if ( data != NULL )
          DebugBreak();
#endif

#if defined(_WIN32) && defined(_DEBUG)
        if(_heapchk()!=_HEAPOK) DebugBreak();
#endif
      }

      ValueType find( const unsigned char* key, uint16_t key_len ) const
      {
        if ( data_len > 0 )
        {
          Entry find_entry( key, key_len );
          unsigned char *data_p = data, *data_end = data + data_len;
          while ( data_p < data_end )
          {
            Entry test_entry( data_p );
            if ( test_entry == find_entry )
              return test_entry.get_value();
            else
              data_p += test_entry.size();
          }
        }
#if defined(_WIN32) && defined(_DEBUG)
        else if ( data != NULL )
          DebugBreak();
#endif

        return 0;
      }

      void find_by_prefix( const unsigned char* key_prefix, uint16_t key_prefix_len, std::vector<ValueType>& out_values ) const
      {
        if ( data_len > 0 )
        {
          unsigned char *data_p = data, *data_end = data + data_len;
          while ( data_p < data_end )
          {
            Entry test_entry( data_p );
            if ( test_entry.startswith( key_prefix, key_prefix_len ) )
              out_values.push_back( test_entry.get_value() );
            data_p += test_entry.size();
          }
        }
#if defined(_WIN32) && defined(_DEBUG)
        else if ( data != NULL )
          DebugBreak();
#endif
      }

      void insert( const unsigned char* key, uint16_t key_len, ValueType value )
      {
        Entry insert_entry( key, key_len, value );

        if ( data_len > 0 )
        {
          // Search for a duplicate string in this slot and, if present, replace its value
          unsigned char *data_p = data, *data_end = data + data_len;
          while ( data_p < data_end )
          {
            Entry test_entry( data_p );
            if ( test_entry == insert_entry )
            {
              memcpy_s( data_p + test_entry.get_value_offset(), sizeof( value ), &value, sizeof( value ) );
              return;
            }
            else
              data_p += test_entry.size();
          }

          unsigned char *new_data = new unsigned char[data_len + insert_entry.size()], *new_data_p = new_data;
          memcpy_s( new_data_p, data_len, data, data_len );
          new_data_p += data_len;
          delete [] data;
          new_data_p = insert_entry.serialize( new_data_p );
          data = new_data;
          data_len = new_data_p - new_data;
        }
        else
        {
#if defined(_WIN32) && defined(_DEBUG)
          if ( data != NULL ) DebugBreak();
#endif
          data = new unsigned char[insert_entry.size()];
          insert_entry.serialize( data );
          data_len = insert_entry.size();
        }

#if defined(_WIN32) && defined(_DEBUG)
        if(_heapchk()!=_HEAPOK) DebugBreak();
#endif
      }

      size_t size() const
      {
        if ( data_len > 0 )
        {
          size_t size_ = 0;
          unsigned char *data_p = data, *data_end = data + data_len;
          while ( data_p < data_end )
          {
            data_p += Entry( data_p ).size();
            size_++;
          }
          return size_;
        }
        else
        {
#if defined(_WIN32) && defined(_DEBUG)
          if ( data != NULL ) DebugBreak();
#endif
          return 0;
        }
      }

    private:
      unsigned char* data; size_t data_len;
    };


    StringArrayHashTable( uint32_t slot_count = 1000 )
	  : slot_count( slot_count )
  	{
  	  slots = new Slot[slot_count];
  	}

    ~StringArrayHashTable()
    {
      delete [] slots;
    }

    inline bool empty() const { return size() == 0; }

    inline void erase( const std::string& key ) { erase( key.c_str(), key.size() ); }
    inline void erase( const unsigned char* key ) { erase( key, strnlen( key, UINT16_MAX ) ); }
    inline void erase( const char* key, size_t key_len ) { erase( reinterpret_cast<const unsigned char*>( key ), static_cast<uint16_t>( key_len ) ); }
    ValueType erase( const unsigned char* key, uint16_t key_len )
    {
      uint32_t slot_i = string_hash( key, key_len ) % slot_count;
      return slots[slot_i].erase( key, key_len );
    }

    inline ValueType find( const std::string& key ) const { return find( key.c_str(), key.size() ); }
    inline ValueType find( const char* key ) const { return find( key, strnlen( key, UINT16_MAX ) ); }
    inline ValueType find( const char* key, size_t key_len ) const { return find( reinterpret_cast<const unsigned char*>( key ), static_cast<uint16_t>( key_len ) ); }
    ValueType find( const unsigned char* key, uint16_t key_len ) const
    {
      uint32_t slot_i = string_hash( key, key_len ) % slot_count;
      return slots[slot_i].find( key, key_len );
    }

    inline void insert( const std::string& key, ValueType value ) { insert( key.c_str(), key.size(), value ); }
    inline void insert( const char* key, ValueType value ) { insert( key, strnlen( key, UINT16_MAX ), value ); }
    inline void insert( const char* key, size_t key_len, ValueType value ) { insert( reinterpret_cast<const unsigned char*>( key ), static_cast<uint16_t>( key_len ), value ); }
    void insert( const unsigned char* key, uint16_t key_len, ValueType value )
    {
      uint32_t slot_i = string_hash( key, key_len ) % slot_count;
      slots[slot_i].insert( key, key_len, value );
    }

    size_t size() const
    {
      size_t size_ = 0;
      for ( uint32_t slot_i = 0; slot_i < slot_count; slot_i++ )
        size_ += slots[slot_i].size();
      return size_;
    }

  private:
    uint32_t slot_count;

    Slot* slots;
  };


  // Adapted from N. Askitis and R. Sinha, "HAT-trie: A Cache-conscious Trie-based Data Structure for Strings", 2007.
  template <class ValueType>
  class HATTrie
  {
  public:
    HATTrie( size_t leaf_bucket_size = 256 )
      : root_bucket( leaf_bucket_size )
    { }

    inline bool empty() const { return size() == 0; }

    inline ValueType erase( const std::string& key ) { return erase( key.c_str(), key.size() ); }
    inline ValueType erase( const unsigned char* key ) { return erase( key, strnlen( key, UINT16_MAX ) ); }
    inline ValueType erase( const char* key, size_t key_len ) { return erase( reinterpret_cast<const unsigned char*>( key ), static_cast<uint16_t>( key_len ) ); }
    inline ValueType erase( const unsigned char* key, uint16_t key_len ) { return root_bucket.erase( key, key_len ); }

    inline void erase_by_prefix( const std::string& key_prefix, std::vector<ValueType>* out_values = NULL ) { erase_by_prefix( key_prefix.c_str(), key_prefix.size(), out_values );  }
    inline void erase_by_prefix( const char* key_prefix, std::vector<ValueType>* out_values = NULL ) { erase_by_prefix( key_prefix, strnlen( key_prefix, UINT16_MAX ), out_values );  }
    inline void erase_by_prefix( const char* key_prefix, size_t key_prefix_len, std::vector<ValueType>* out_values = NULL ) { erase_by_prefix( reinterpret_cast<const unsigned char*>( key_prefix ), static_cast<uint16_t>( key_prefix_len ), out_values );  }
    inline void erase_by_prefix( const unsigned char* key_prefix, uint16_t key_prefix_len, std::vector<ValueType>* out_values = NULL ) { root_bucket.erase_by_prefix( key_prefix, key_prefix_len, out_values ); }

    inline ValueType find( const std::string& key ) const { return find( key.c_str(), key.size() ); }
    inline ValueType find( const char* key ) const { return find( key, strnlen( key, UINT16_MAX ) ); }
    inline ValueType find( const char* key, size_t key_len ) const { return find( reinterpret_cast<const unsigned char*>( key ), static_cast<uint16_t>( key_len ) ); }
    inline ValueType find( const unsigned char* key, uint16_t key_len ) const { return root_bucket.find( key, key_len ); }

    inline void find_by_prefix( const std::string& key_prefix, std::vector<ValueType>& out_values ) const { find_by_prefix( key_prefix.c_str(), key_prefix.size(), out_values );  }
    inline void find_by_prefix( const char* key_prefix, std::vector<ValueType>& out_values ) const { find_by_prefix( key_prefix, strnlen( key_prefix, UINT16_MAX ), out_values );  }
    inline void find_by_prefix( const char* key_prefix, size_t key_prefix_len, std::vector<ValueType>& out_values ) const { find_by_prefix( reinterpret_cast<const unsigned char*>( key_prefix ), static_cast<uint16_t>( key_prefix_len ), out_values );  }
    inline void find_by_prefix( const unsigned char* key_prefix, uint16_t key_prefix_len, std::vector<ValueType>& out_values ) const { root_bucket.find_by_prefix( key_prefix, key_prefix_len, out_values ); }

    inline void insert( const std::string& key, ValueType value ) { insert( key.c_str(), key.size(), value ); }
    inline void insert( const char* key, ValueType value ) { insert( key, strnlen( key, UINT16_MAX ), value ); }
    inline void insert( const char* key, size_t key_len, ValueType value ) { insert( reinterpret_cast<const unsigned char*>( key ), static_cast<uint16_t>( key_len ), value ); }
    inline void insert( const unsigned char* key, uint16_t key_len, ValueType value ) { root_bucket.insert( key, key_len, value ); }

    inline size_t size() const { return root_bucket.size(); }

  private:
    class Bucket
    {
    public:
      enum Type { INTERNAL = 1, LEAF };

      virtual ~Bucket() { }

      Type get_type() const { return type; }

    protected:
      Bucket( Type type )
        : type( type )
      { }

    private:
      Type type;
    };


    class LeafBucket : public Bucket, public StringArrayHashTable<ValueType>::Slot
    {
    public:
      LeafBucket() : Bucket( Bucket::LEAF )
      { }
    };


    class InternalBucket : public Bucket
    {
    public:
      InternalBucket( size_t leaf_bucket_size )
        : Bucket( Bucket::INTERNAL ), leaf_bucket_size( leaf_bucket_size )
      {
        memset( child_buckets, 0, sizeof( child_buckets ) );
      }

      ~InternalBucket()
      {
        for ( size_t child_bucket_i = 0; child_bucket_i < 257; child_bucket_i++ )
          delete child_buckets[child_bucket_i];
      }

      inline bool empty() const { return size() == 0; }

      ValueType erase( const unsigned char* key, uint16_t key_len )
      {
        Bucket* child_bucket = findChildBucket( key, key_len );
        if ( child_bucket != NULL )
        {
          ValueType value;

          switch ( child_bucket->get_type() )
          {
            case Bucket::INTERNAL:
            {
#if defined(_WIN32) && defined(_DEBUG)
              if ( key_len == 0 )
                DebugBreak();
#endif
              value = static_cast<InternalBucket*>( child_bucket )->erase( key+1, key_len-1 );
              if ( static_cast<InternalBucket*>( child_bucket )->empty() )
                eraseChildBucket( key, key_len );
            }
            break;

            case Bucket::LEAF:
            {
              if ( key_len > 0 )
                value = static_cast<LeafBucket*>( child_bucket )->erase( key+1, key_len-1 );
              else
                value = static_cast<LeafBucket*>( child_bucket )->erase( key, key_len );

              if ( static_cast<LeafBucket*>( child_bucket )->empty() )
                eraseChildBucket( key, key_len );
            }
            break;

            default: return 0;
          }

          return value;
        }
        else
          return 0;
      }

      void erase_by_prefix( const unsigned char* key_prefix, uint16_t key_prefix_len, std::vector<ValueType>* out_values )
      {
        if ( key_prefix_len > 0 )
        {
          Bucket* child_bucket = findChildBucket( key_prefix, key_prefix_len );
          if ( child_bucket )
          {
            switch ( child_bucket->get_type() )
            {
              case Bucket::INTERNAL: static_cast<InternalBucket*>( child_bucket )->erase_by_prefix( key_prefix+1, key_prefix_len-1, out_values );
              case Bucket::LEAF: static_cast<LeafBucket*>( child_bucket )->erase_by_prefix( key_prefix+1, key_prefix_len-1, out_values );
            }
          }
        }
        else // Everything below this bucket matches the key_prefix
        {
          for ( size_t child_bucket_i = 0; child_bucket_i < 257; child_bucket_i++ )
          {
            if ( child_buckets[child_bucket_i] )
            {
              switch ( child_buckets[child_bucket_i]->get_type() )
              {
                case Bucket::INTERNAL:
                {
                  static_cast<InternalBucket*>( child_buckets[child_bucket_i] )->erase_by_prefix( key_prefix, key_prefix_len, out_values );
                  if ( static_cast<InternalBucket*>( child_buckets[child_bucket_i] )->empty() )
                  {
                    delete child_buckets[child_bucket_i];
                    child_buckets[child_bucket_i] = NULL;
                  }
                }
                break;

                case Bucket::LEAF:
                {
                  static_cast<LeafBucket*>( child_buckets[child_bucket_i] )->erase_by_prefix( key_prefix, key_prefix_len, out_values );
                  if ( static_cast<LeafBucket*>( child_buckets[child_bucket_i] )->empty() )
                  {
                    delete child_buckets[child_bucket_i];
                    child_buckets[child_bucket_i] = NULL;
                  }
                }
              }

            }
          }
        }
      }

      ValueType find( const unsigned char* key, uint16_t key_len ) const
      {
        Bucket* child_bucket = findChildBucket( key, key_len );
        if ( child_bucket != NULL )
        {
          switch ( child_bucket->get_type() )
          {
            case Bucket::INTERNAL:
            {
#if defined(_WIN32) && defined(_DEBUG)
              if ( key_len == 0 )
                DebugBreak();
#endif
              return static_cast<InternalBucket*>( child_bucket )->find( key+1, key_len-1 );
            }
            break;

            case Bucket::LEAF:
            {
              if ( key_len > 0 )
                return static_cast<LeafBucket*>( child_bucket )->find( key+1, key_len-1 );
              else
                return static_cast<LeafBucket*>( child_bucket )->find( key, key_len );
            }
            break;

            default: return 0;
          }
        }
        else
          return 0;
      }

      void find_by_prefix( const unsigned char* key_prefix, uint16_t key_prefix_len, std::vector<ValueType>& out_values ) const
      {
        if ( key_prefix_len > 0 )
        {
          Bucket* child_bucket = findChildBucket( key_prefix, key_prefix_len );
          if ( child_bucket )
          {
            switch ( child_bucket->get_type() )
            {
              case Bucket::INTERNAL: return static_cast<InternalBucket*>( child_bucket )->find_by_prefix( key_prefix+1, key_prefix_len-1, out_values );
              case Bucket::LEAF: return static_cast<LeafBucket*>( child_bucket )->find_by_prefix( key_prefix+1, key_prefix_len-1, out_values );
            }
          }
        }
        else // Everything below this bucket matches the key_prefix
        {
          for ( size_t child_bucket_i = 0; child_bucket_i < 257; child_bucket_i++ )
          {
            if ( child_buckets[child_bucket_i] )
            {
              switch ( child_buckets[child_bucket_i]->get_type() )
              {
                case Bucket::INTERNAL: static_cast<InternalBucket*>( child_buckets[child_bucket_i] )->find_by_prefix( key_prefix, key_prefix_len, out_values ); break;
                case Bucket::LEAF: static_cast<LeafBucket*>( child_buckets[child_bucket_i] )->find_by_prefix( key_prefix, key_prefix_len, out_values ); break;
              }
            }
          }
        }
      }

      void insert( const unsigned char* key, uint16_t key_len, ValueType value )
      {
        Bucket* child_bucket = findChildBucket( key, key_len );
        if ( child_bucket != NULL )
        {
          switch ( child_bucket->get_type() )
          {
            case Bucket::INTERNAL:
            {
#if defined(_WIN32) && defined(_DEBUG)
              if ( key_len == 0 )
                DebugBreak();
#endif
              return static_cast<InternalBucket*>( child_bucket )->insert( key+1, key_len-1, value );
            }
            break;

            case Bucket::LEAF:
            {
              LeafBucket* child_leaf_bucket = static_cast<LeafBucket*>( child_bucket );
              if ( child_leaf_bucket->size() < leaf_bucket_size )
              {
                if ( key_len > 0 )
                  child_leaf_bucket->insert( key+1, key_len-1, value );
                else
                  child_leaf_bucket->insert( key, key_len, value );
              }
              else // Child LeafBucket is full, burst it into A grand-child buckets
              {
#if defined(_WIN32) && defined(_DEBUG)
                if ( key_len == 0 )
                  DebugBreak();
#endif
                InternalBucket* child_internal_bucket = new InternalBucket( leaf_bucket_size );
                for ( typename LeafBucket::const_iterator entry_i = child_leaf_bucket->begin(); entry_i != child_leaf_bucket->end(); entry_i++ )
                {
                  typename LeafBucket::Entry entry = *entry_i;
                  child_internal_bucket->insert( entry.get_key(), entry.get_key_len(), entry.get_value() );
                }
                child_internal_bucket->insert( key+1, key_len-1, value );
                child_buckets[key[0]+1] = child_internal_bucket;
                delete child_leaf_bucket;
              }
            }
            break;
          }
        }
        else if ( key_len > 0 )
        {
          child_bucket = child_buckets[key[0]+1] = new LeafBucket;
          static_cast<LeafBucket*>( child_bucket )->insert( key+1, key_len-1, value );
        }
        else
        {
          child_bucket = child_buckets[0] = new LeafBucket;
          static_cast<LeafBucket*>( child_bucket )->insert( key, key_len, value );
        }
      }

      size_t size() const
      {
        size_t size_ = 0;
        for ( size_t child_bucket_i = 0; child_bucket_i < 257; child_bucket_i++ )
        {
          if ( child_buckets[child_bucket_i] )
          {
            switch ( child_buckets[child_bucket_i]->get_type() )
            {
              case Bucket::INTERNAL: size_ += static_cast<InternalBucket*>( child_buckets[child_bucket_i] )->size(); break;
              case Bucket::LEAF: size_ += static_cast<LeafBucket*>( child_buckets[child_bucket_i] )->size(); break;
            }
          }
        }
        return size_;
      }

    private:
      size_t leaf_bucket_size;

      Bucket* child_buckets[257]; // empty string bucket + one bucket for every possible unsigned char value

      Bucket* findChildBucket( const unsigned char* key, uint16_t key_len ) const
      {
        if ( key_len > 0 )
          return child_buckets[key[0]+1];
        else
          return child_buckets[0];
      }

      void eraseChildBucket( const unsigned char* key, uint16_t key_len )
      {
        if ( key_len > 0 )
        {
          delete child_buckets[key[0]+1];
          child_buckets[key[0]+1] = NULL;
        }
        else
        {
          delete child_buckets[0];
          child_buckets[0] = NULL;
        }
      }
    };


    InternalBucket root_bucket;
  };


  class IOBuffer : public Object
  {
  public:
    IOBuffer( size_t size );
    IOBuffer( size_t size, auto_Object<> context );
    virtual ~IOBuffer();

    size_t consume( void* into_buffer, size_t into_buffer_len );
    size_t consume( std::string& into_string, size_t into_string_len );

//    auto_Object<> get_context() const { return context; }
//    auto_Object<IOBuffer> get_next_io_buffer() const { return next_io_buffer; }
    uint64_t get_size() const { return buffer_len; }    

    operator struct iovec() const;
    operator void*() const { return buffer; }

//    void set_context( auto_Object<> context ) { this->context = context; }
//    void set_next_io_buffer( auto_Object<IOBuffer> );
    void set_size( size_t size );
    size_t size() const { return buffer_len; }

    // Object
    YIELD_OBJECT_PROTOTYPES( IOBuffer, 0 );
        
  private:
    uint8_t* buffer;
    size_t buffer_len, consumed_buffer_len;
//    auto_Object<> context;
//    auto_Object<IOBuffer> next_io_buffer;
  };

  class Log : public Object
  {
  public:
    // Adapted from syslog levels
    enum Level
    {
      LOG_EMERG = 0,
      LOG_ALERT = 1,
      LOG_CRIT = 2,
      LOG_ERR = 3,
      LOG_WARNING = 4,
      LOG_NOTICE = 5,
      LOG_INFO = 6,
      LOG_DEBUG = 7
    };


    class Stream
    {
    public:
      Stream( const Stream& other );
      ~Stream();

      template <typename T>
      Stream& operator<<( T t )
      {
        if ( level <= log->get_level() )
          oss << t;
        return *this;
      }      

    private:
      friend class Log;

      Stream( auto_Object<Log> log, Level level );

      auto_Object<Log> log;
      Level level;

      std::ostringstream oss;
    };


    static auto_Object<Log> open( std::ostream&, Level level );
    static auto_Object<Log> open( const Path& file_path, Level level );

    inline Level get_level() const { return level; }
    Stream getStream() { return Stream( incRef(), level ); }
    Stream getStream( Level level ) { return Stream( incRef(), level ); }
    void set_level( Level level ) { this->level = level; }

    inline void write( const char* str, Level level )
    {
      write( str, strnlen( str, UINT16_MAX ), level );
    }

    inline void write( const std::string& str, Level level )
    {
      write( str.c_str(), str.size(), level );
    }

    inline void write( const void* str, size_t str_len, Level level )
    {
      return write( static_cast<const unsigned char*>( str ), str_len, level );
    }

    inline void write( const unsigned char* str, size_t str_len, Level level )
    {
      char* sanitized_str = sanitize( str, str_len );
      write( sanitized_str, str_len, level );
      delete [] sanitized_str;
    }

    inline void write( const char* str, size_t str_len, Level level )
    {
      struct iovec buffers[1];
      buffers[0].iov_base = ( void* )str;
      buffers[0].iov_len = str_len;
      if ( level <= this->level )
        write( str, str_len );
    }
    
    // Object
    YIELD_OBJECT_PROTOTYPES( Log, 2 );

  protected:
    Log( Level level )
      : level( level )
    { }

    virtual ~Log() { }

    virtual void write( const char* str, size_t str_len ) = 0;

  private:
    Level level;

    static char* sanitize( const unsigned char* str, size_t str_len );
  };


  class Machine
  {
  public:
    static uint16_t getLogicalProcessorsPerPhysicalProcessor()
    {
      return getOnlineLogicalProcessorCount() / getOnlinePhysicalProcessorCount();
    }

    static uint16_t getOnlineLogicalProcessorCount();
    static uint16_t getOnlinePhysicalProcessorCount();

#ifndef __MACH__
    // htons is a macro on OS X.. Thanks guys.
    static inline uint16_t htons( uint16_t x )
    {
#ifdef __BIG_ENDIAN__
      return x;
#else
      return ( x >> 8 ) | ( x << 8 );
#endif
    }

    static inline uint32_t htonl( uint32_t x )
    {
#ifdef __BIG_ENDIAN__
      return x;
#else
      return ( x >> 24 ) | ( ( x << 8 ) & 0x00FF0000 ) | ( ( x >> 8 ) & 0x0000FF00 ) | ( x << 24 );
#endif
    }
#endif

    static inline uint64_t htonll( uint64_t x )
    {
#ifdef __BIG_ENDIAN__
      return x;
#else
      return ( x >> 56 ) | ( ( x << 40 ) & 0x00FF000000000000ULL ) | ( ( x << 24 ) & 0x0000FF0000000000ULL ) | ( ( x << 8 )  & 0x000000FF00000000ULL ) | ( ( x >> 8)  & 0x00000000FF000000ULL ) | ( ( x >> 24) & 0x0000000000FF0000ULL ) | ( ( x >> 40 ) & 0x000000000000FF00ULL ) | ( x << 56 );
#endif
    }

#ifndef __MACH__
    static inline uint16_t ntohs( uint16_t x )
    {
#ifdef __BIG_ENDIAN__
      return x;
#else
      return ( x >> 8 ) | ( x << 8 );
#endif
    }

    static inline uint32_t ntohl( uint32_t x )
    {
#ifdef __BIG_ENDIAN__
      return x;
#else
      return ( x >> 24 ) | ( ( x << 8 ) & 0x00FF0000 ) | ( ( x >> 8 ) & 0x0000FF00 ) | ( x << 24 );
#endif
    }
#endif

    static inline uint64_t ntohll( uint64_t x )
    {
#ifdef __BIG_ENDIAN__
      return x;
#else
      return ( x >> 56 ) | ( ( x << 40 ) & 0x00FF000000000000ULL ) | ( ( x << 24 ) & 0x0000FF0000000000ULL ) | ( ( x << 8 )  & 0x000000FF00000000ULL ) | ( ( x >> 8)  & 0x00000000FF000000ULL ) | ( ( x >> 24) & 0x0000000000FF0000ULL ) | ( ( x >> 40 ) & 0x000000000000FF00ULL ) | ( x << 56 );
#endif
    }
  };


  class Marshaller : public Object
  {
  public:
    class Declaration
    {
    public:
      Declaration() : identifier( 0 ), tag( 0 ) { }
      Declaration( const char* identifier ) : identifier( identifier ), tag( 0 ) { }
      Declaration( const char* identifier, uint32_t tag ) : identifier( identifier ), tag( tag ) { }

      const char* get_identifier() const { return identifier; }
      uint32_t get_tag() const { return tag; }

    private:
      const char* identifier;
      uint32_t tag;
    };


    virtual ~Marshaller() { }

    virtual void writeBool( const Declaration&, bool value ) = 0;
    virtual void writeDouble( const Declaration&, double value ) = 0;
    virtual void writeFloat( const Declaration& decl, float value ) { writeDouble( decl, value ); }
    virtual void writeInt8( const Declaration& decl, int8_t value ) { writeInt16( decl, value ); }
    virtual void writeInt16( const Declaration& decl, int16_t value ) { writeInt32( decl, value ); }
    virtual void writeInt32( const Declaration& decl, int32_t value ) { writeInt64( decl, value ); }
    virtual void writeInt64( const Declaration&, int64_t ) = 0;
    virtual void writeMap( const Declaration& decl, YIELD::Object& value ) = 0;
    virtual void writePointer( const Declaration&, void* ) { }
    virtual void writeSequence( const Declaration& decl, YIELD::Object& value ) = 0;
    virtual void writeString( const Declaration& decl, const std::string& value ) { writeString( decl, value.c_str(), value.size() ); }
    virtual void writeString( const Declaration& decl, const char* value ) { writeString( decl, value, strnlen( value, UINT16_MAX ) ); }
    virtual void writeString( const Declaration&, const char* value, size_t value_len ) = 0;
    virtual void writeStruct( const Declaration& decl, YIELD::Object& value ) = 0;
    virtual void writeUint8( const Declaration& decl, uint8_t value ) { writeInt8( decl, static_cast<int8_t>( value ) ); }
    virtual void writeUint16( const Declaration& decl, uint16_t value ) { writeInt16( decl, static_cast<int16_t>( value ) ); }
    virtual void writeUint32( const Declaration& decl, uint32_t value ) { writeInt32( decl, static_cast<int32_t>( value ) ); }
    virtual void writeUint64( const Declaration& decl, uint64_t value ) { writeInt64( decl, static_cast<int64_t>( value ) ); }
  };


  class MemoryMappedFile : public Object
  {
  public:
    static auto_Object<MemoryMappedFile> open( const Path& path ) { return open( path, File::DEFAULT_FLAGS, File::DEFAULT_MODE, File::DEFAULT_ATTRIBUTES, 0 ); }
    static auto_Object<MemoryMappedFile> open( const Path& path, uint32_t flags ) { return open( path, flags, File::DEFAULT_MODE, File::DEFAULT_ATTRIBUTES, 0 ); }
    static auto_Object<MemoryMappedFile> open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes, size_t minimum_size );

    virtual bool close();
    inline uint64_t get_size() const { return size; }
    inline operator char*() const { return start; }
    inline operator void*() const { return start; }
    bool resize( size_t );
    virtual bool sync();
    virtual bool sync( size_t offset, size_t length );
    virtual bool sync( void* ptr, size_t length );

    // Object
    YIELD_OBJECT_PROTOTYPES( MemoryMappedFile, 3 );

  protected:
    MemoryMappedFile( auto_Object<File> underlying_file, uint32_t open_flags );
    virtual ~MemoryMappedFile() { close(); }

  private:
    auto_Object<File> underlying_file;
    uint32_t open_flags;

#ifdef _WIN32
    void* mapping;
#endif
    char* start;
    size_t size;
  };


  class Mutex
  {
  public:
    Mutex();
    ~Mutex();

    // These calls are modeled after the pthread calls they delegate to
    // Have a separate function for timeout_ns == 0 (never block) to avoid an if branch on a critical path
    bool acquire(); // Blocking
    bool try_acquire(); // Never blocks
    bool timed_acquire( uint64_t timeout_ns ); // May block for timeout_ns
    void release();

  private:
#ifdef _WIN32
    void* hMutex;
#else
    pthread_mutex_t pthread_mutex;
#endif
  };


  class NamedPipe : public Object
  {
  public:
    static auto_Object<NamedPipe> open( const Path& path, uint32_t flags = O_RDWR, mode_t mode = File::DEFAULT_MODE );            

    virtual ssize_t read( void* buffer, size_t buffer_len );
    virtual ssize_t write( const void* buffer, size_t buffer_len );
    virtual ssize_t writev( const iovec* buffers, uint32_t buffers_count );
    
    // Object
    YIELD_OBJECT_PROTOTYPES( NamedPipe, 4 );  

  private:
#ifdef WIN32
    NamedPipe( auto_Object<File> underlying_file, bool connected );
#else
    NamedPipe( auto_Object<File> underlying_file );
#endif
    ~NamedPipe() { }

    auto_Object<File> underlying_file;

#ifdef _WIN32
    bool connected;
    bool connect();
#endif
  };


  template <class ElementType, uint32_t QueueLength>
  class NonBlockingFiniteQueue
  {
  public:
    NonBlockingFiniteQueue()
    {
      head = 0;
      tail = 1;

      for ( size_t element_i = 0; element_i < QueueLength+2; element_i++ )
        elements[element_i] = reinterpret_cast<ElementType>( 0 );

      elements[0] = reinterpret_cast<ElementType>( 1 );
    }

    bool enqueue( ElementType element )
    {
//			if ( ( uint_ptr )element & 0x1 ) DebugBreak();
      element = reinterpret_cast<ElementType>( reinterpret_cast<uint_ptr>( element ) >> 1 );
//			if ( ( uint_ptr )element & PTR_HIGH_BIT ) DebugBreak();

      int32_t copied_tail, last_try_pos, try_pos; // te, ate, temp
      ElementType try_element;

      for ( ;; )
      {
        copied_tail = tail;
        last_try_pos = copied_tail;
        try_element = reinterpret_cast<ElementType>( elements[last_try_pos] );
        try_pos = ( last_try_pos + 1 ) % ( QueueLength + 2 );

        while ( try_element != reinterpret_cast<ElementType>( 0 ) &&
                try_element != reinterpret_cast<ElementType>( 1 ) )
        {
          if ( copied_tail != tail )
            break;

          if ( try_pos == head )
            break;

          try_element = reinterpret_cast<ElementType>( elements[try_pos] );
          last_try_pos = try_pos;
          try_pos = ( last_try_pos + 1 ) % ( QueueLength + 2 );
        }

        if ( copied_tail != tail ) // Someone changed tail while we were looping
          continue;

        if ( try_pos == head )
        {
          last_try_pos = ( try_pos + 1 ) % ( QueueLength + 2 );
          try_element = reinterpret_cast<ElementType>( elements[last_try_pos] );

          if ( try_element != reinterpret_cast<ElementType>( 0 ) &&
               try_element != reinterpret_cast<ElementType>( 1 ) )
            return false;						// Queue is full

          atomic_cas( &head, last_try_pos, try_pos );

          continue;
        }

        if ( copied_tail != tail )
          continue;

        // diff next line
        if ( atomic_cas( ( volatile uint_ptr* )&elements[last_try_pos], ( uint_ptr )( ( try_element == reinterpret_cast<ElementType>( 1 ) ) ? reinterpret_cast<ElementType>( reinterpret_cast<uint_ptr>( element ) | PTR_HIGH_BIT ) : element ), ( uint_ptr )try_element ) == ( uint_ptr )try_element )
        {
          if ( try_pos % 2 == 0 )
            atomic_cas( &tail, try_pos, copied_tail );

          return true;
        }
      }
    }

    ElementType dequeue()
    {
      return try_dequeue();
    }

    ElementType try_dequeue()
    {
      int32_t copied_head, try_pos;
      ElementType try_element;

      for ( ;; )
      {
        copied_head = head;
        try_pos = ( copied_head + 1 ) % ( QueueLength + 2 );
        try_element = reinterpret_cast<ElementType>( elements[try_pos] );

        while ( try_element == reinterpret_cast<ElementType>( 0 ) ||
                try_element == reinterpret_cast<ElementType>( 1 ) )
        {
          if ( copied_head != head )
            break;

          if ( try_pos == tail )
            return 0;

          try_pos = ( try_pos + 1 ) % ( QueueLength + 2 );

          try_element = reinterpret_cast<ElementType>( elements[try_pos] );
        }

        if ( copied_head != head )
          continue;

        if ( try_pos == tail )
        {
          atomic_cas( &tail, ( try_pos + 1 ) % ( QueueLength + 2 ), try_pos );
          continue;
        }

        if ( copied_head != head )
          continue;

        if ( atomic_cas( ( volatile uint_ptr* )&elements[try_pos], ( ( uint_ptr )try_element & ( uint_ptr )0x80000000 ) ? 1 : 0, ( uint_ptr )try_element ) == ( uint_ptr )try_element )
        {
          if ( try_pos % 2 == 0 )
            atomic_cas( &head, try_pos, copied_head );

          return reinterpret_cast<ElementType>( ( ( ( uint_ptr )try_element & PTR_LOW_BITS ) << 1 ) );
        }
      }
    }

  private:
    volatile ElementType elements[QueueLength+2]; // extra 2 for sentinels
    volatile int32_t head, tail;

#if defined(__LLP64__) || defined(__LP64__)
    typedef int64_t uint_ptr;
    const static uint_ptr PTR_HIGH_BIT = 0x8000000000000000;
    const static uint_ptr PTR_LOW_BITS = 0x7fffffffffffffff;
#else
    typedef int32_t uint_ptr;
    const static uint_ptr PTR_HIGH_BIT = 0x80000000;
    const static uint_ptr PTR_LOW_BITS = 0x7fffffff;
#endif
  };


  class NOPLock
  {
  public:
    inline bool acquire() { return true; }
    inline bool try_acquire() { return true; }
    inline bool timed_acquire( uint64_t ) { return true; }
    inline void release() { }
  };


  class Path : public Object
  {
  public:
    Path() { }
    Path( const char* host_charset_path );
    Path( const char* host_charset_path, size_t host_charset_path_len );
    Path( const std::string& host_charset_path );
#ifdef _WIN32
    Path( const wchar_t* wide_path );
    Path( const wchar_t* wide_path, size_t wide_path_len );
    Path( const std::wstring& wide_path );
#endif
    Path( const Path& );
    virtual ~Path() { }

    Path abspath() const;
    inline bool empty() const { return host_charset_path.empty(); }
//    const std::string& get_utf8_path();
#ifdef _WIN32
    const std::wstring& get_wide_path() const { return wide_path; }
    operator const std::wstring&() const { return wide_path; }
    operator const wchar_t*() const { return wide_path.c_str(); }
    bool operator==( const wchar_t* ) const;
    bool operator!=( const wchar_t* ) const;
#endif
    operator const char*() const { return host_charset_path.c_str(); }
    operator const std::string&() const { return host_charset_path; }
    Path operator+( const Path& other ) const { return join( other ); }
    bool operator==( const Path& ) const;
    bool operator!=( const Path& ) const;
    bool operator==( const char* ) const;
    bool operator!=( const char* ) const;

    Path join( const Path& ) const;
    std::pair<Path, Path> split() const; // head, tail
    void split_all( std::vector<Path>& ) const; // parts between separator
    std::pair<Path, Path> splitext() const;
    size_t size() const { return host_charset_path.size(); }

    // Object
    YIELD_OBJECT_PROTOTYPES( Path, 5 );

  private:
    void init_from_host_charset_path();
#ifdef _WIN32
    void init_from_wide_path();
#endif

    std::string host_charset_path;
#ifdef _WIN32
    std::wstring wide_path;
#else
    // void MultiByteToMultiByte( const char* fromcode, const std::string& frompath, const char* tocode, std::string& topath );
#endif
  };

  static inline std::ostream& operator<<( std::ostream& os, const Path& path )
  {
    os << static_cast<const std::string&>( path );
    return os;
  }


  class Pipe : public Object
  {
  public:
    static auto_Object<Pipe> create();

    ssize_t read( void* buffer, size_t buffer_len );
    ssize_t write( const void* buffer, size_t buffer_len );

    // Object
    YIELD_OBJECT_PROTOTYPES( Pipe, 6 );

  private:
#ifdef _WIN32
    Pipe( void* ends[2] );
#else
    Pipe( int ends[2] );
#endif
    ~Pipe() { }

#ifdef _WIN32
    void* ends[2];
#else
    int ends[2];
#endif
  };


  class PrettyPrinter : public Marshaller
  {
  public:
    PrettyPrinter( std::ostream& );
    PrettyPrinter& operator=( const PrettyPrinter& ) { return *this; }

    // Marshaller
    YIELD_MARSHALLER_PROTOTYPES;

    // Object
    YIELD_OBJECT_PROTOTYPES( PrettyPrinter, 0 );

  private:
    std::ostream& os;
  };


  class Process : public Object
  {
  public:
    static auto_Object<Process> create( const Path& executable_file_path ); // No arguments
    static auto_Object<Process> create( int argc, char** argv );    
    static auto_Object<Process> create( const Path& executable_file_path, const char** null_terminated_argv ); // execv style

    auto_Object<Pipe> get_stdin() const { return child_stdin; }
    auto_Object<Pipe> get_stdout() const { return child_stdout; }
    auto_Object<Pipe> get_stderr() const { return child_stderr; }

    bool kill(); // SIGKILL
    bool poll( int* out_return_code = 0 ); // Calls waitpid() but WNOHANG, out_return_code can be NULL    
    bool terminate(); // SIGTERM
    int wait(); // Calls waitpid() and suspends the calling process until the child exits, use carefully

    // Object
    YIELD_OBJECT_PROTOTYPES( Process, 7 );

  private:
#ifdef _WIN32
    Process( void* hChildProcess, void* hChildThread,       
#else
    Process( int child_pid, 
#endif
      auto_Object<Pipe> child_stdin, auto_Object<Pipe> child_stdout, auto_Object<Pipe> child_stderr );

    ~Process();

#ifdef _WIN32
    void *hChildProcess, *hChildThread;
#else
    int child_pid;
#endif
    auto_Object<Pipe> child_stdin, child_stdout, child_stderr;  
  };


  class ProcessorSet : public Object
  {
  public:
    ProcessorSet();
    ProcessorSet( uint32_t from_mask );

    void clear();
    void clear( uint16_t processor_i );
    uint16_t count() const;
    bool empty() const;
    bool isset( uint16_t processor_i ) const;
    void set( uint16_t processor_i );    

    // Object
    YIELD_OBJECT_PROTOTYPES( ProcessorSet, 8 );

  private:
    ProcessorSet( const ProcessorSet& ) { DebugBreak(); } // Prevent copying
    ~ProcessorSet();

    friend class Process;
    friend class Thread;

#if defined(_WIN32)
    unsigned long mask;
#elif defined(__linux)
    void* cpu_set;
#elif defined(__sun)
    int psetid;
#endif
  };


  struct SamplerStatistics
  {
    double min, max;
    double median, mean;
    double ninetieth_percentile;
  };

  template <typename SampleType, size_t ArraySize, class LockType = NOPLock>
  class Sampler
  {
  public:
    Sampler()
    {
      std::memset( samples, 0, sizeof( samples ) );
      samples_pos = samples_count = 0;
      min = static_cast<SampleType>( ULONG_MAX ); max = 0; total = 0;
    }

    void setNextSample( SampleType sample )
    {
      if ( samples_lock.try_acquire() )
      {
        samples[samples_pos] = sample;
        samples_pos = ( samples_pos + 1 ) % ArraySize;
        if ( samples_count < ArraySize ) samples_count++;

        if ( sample < min )
          min = sample;
        if ( sample > max )
          max = sample;
        total += sample;

        samples_lock.release();
      }
    }

    uint32_t getSamplesCount()
    {
      return samples_count;
    }

    SamplerStatistics getStatistics()
    {
      samples_lock.acquire();

      SamplerStatistics stats;

      if ( samples_count > 1 )
      {
        stats.min = static_cast<double>( min );
        stats.max = static_cast<double>( max );
        stats.mean = static_cast<double>( total ) / static_cast<double>( samples_count );

        // Sort for the median and ninetieth percentile
        std::sort( samples, samples + samples_count );

        stats.ninetieth_percentile = static_cast<double>( samples[static_cast<size_t>( 0.9 * static_cast<double>( samples_count ) )] );

        size_t sc_div_2 = samples_count / 2;
        if ( samples_count % 2 == 1 )
          stats.median = static_cast<double>( samples[sc_div_2] );
        else
        {
          SampleType median_temp = samples[sc_div_2] + samples[sc_div_2-1];
          if ( median_temp > 0 )
            stats.median = median_temp / 2.0;
          else
            stats.median = 0;
        }
      }
      else if ( samples_count == 1 )
        stats.min = stats.max = stats.mean = stats.median = stats.ninetieth_percentile = static_cast<double>( samples[0] );
      else
        memset( &stats, 0, sizeof( stats ) );

      samples_lock.release();

      return stats;
    }

  protected:
    SampleType samples[ArraySize+1], min, max; SampleType total;
    uint32_t samples_pos, samples_count;
    LockType samples_lock;
  };


  class SharedLibrary : public Object
  {
  public:
    static auto_Object<SharedLibrary> open( const Path& file_prefix, const char* argv0 = 0 );

    void* getFunction( const char* function_name, void* missing_function_return_value = NULL );

    template <typename FunctionType>
    FunctionType getFunction( const char* function_name, FunctionType missing_function_return_value = NULL )
    {
      return ( FunctionType )getFunction( function_name, ( void* )missing_function_return_value );
    }    

    // Object
    YIELD_OBJECT_PROTOTYPES( SharedLibrary, 11 );

  private:
    SharedLibrary( void* handle );
    ~SharedLibrary();

    void* handle;
  };


  class Stat : public Object
  {
  public:   
    static auto_Object<Stat> stat( const Path& path );

#ifdef _WIN32
    Stat( mode_t mode, uint64_t size, const Time& atime, const Time& mtime, const Time& ctime, uint32_t attributes );
    Stat( const BY_HANDLE_FILE_INFORMATION& );
    Stat( const WIN32_FIND_DATA& );
    Stat( uint32_t nFileSizeHigh, uint32_t nFileSizeLow, const FILETIME* ftLastWriteTime, const FILETIME* ftCreationTime, const FILETIME* ftLastAccessTime, uint32_t dwFileAttributes ); // For doing FILETIME -> Unix conversions in Dokan; deduces mode from dwFileAttributes
#else
    Stat( mode_t mode, nlink_t nlink, uid_t tag, gid_t gid, uint64_t size, const Time& atime, const Time& mtime, const Time& ctime );
#endif
    Stat( const struct stat& stbuf );

    mode_t get_mode() const { return mode; }
#ifndef _WIN32
    nlink_t get_nlink() const { return nlink; }
    uid_t get_uid() const { return tag; }
    gid_t get_gid() const { return gid; }
#endif
    uint64_t get_size() const { return size; }
    const Time& get_atime() const { return atime; }
    const Time& get_mtime() const { return mtime; }
    const Time& get_ctime() const { return ctime; }
#ifdef _WIN32
    uint32_t get_attributes() const;
#endif

    inline bool ISDIR() const { return ( mode & S_IFDIR ) == S_IFDIR; }
    inline bool ISREG() const { return ( mode & S_IFREG ) == S_IFREG; }
#ifndef _WIN32
    inline bool ISLNK() const { return S_ISLNK( mode ); }
#endif

    virtual const bool operator==( const Stat& other ) const { return mode == other.mode && size == other.size && atime == other.atime && mtime == other.mtime && ctime == other.ctime; }
    virtual operator std::string() const;
    virtual operator struct stat() const;
#ifdef _WIN32
    virtual operator BY_HANDLE_FILE_INFORMATION() const;
    virtual operator WIN32_FIND_DATA() const;
#endif

    // Object
    YIELD_OBJECT_PROTOTYPES( Stat, 12 );

  protected:
    virtual ~Stat() { }

#ifdef _WIN32    
    void init( uint32_t nFileSizeHigh, uint32_t nFileSizeLow, const FILETIME* ftLastWriteTime, const FILETIME* ftCreationTime, const FILETIME* ftLastAccessTime, uint32_t dwFileAttributes );
#endif

    mode_t mode;
#ifndef _WIN32
    nlink_t nlink;
    uid_t tag;
    gid_t gid;
#endif
    uint64_t size;
    Time atime, mtime, ctime;
#ifdef _WIN32
    uint32_t attributes;
#endif

  private:
    Stat( const Stat& ) { DebugBreak(); } // Prevent copying
  };


  static inline std::ostream& operator<<( std::ostream& os, const Stat& stbuf )
  {
    os << "{ ";
    mode_t mode = stbuf.get_mode();
    os << "st_mode: " << mode << " (";
#define YIELD_STAT_MODE_BIT_AS_STRING( mode_bit ) { if ( ( mode & mode_bit ) == mode_bit ) os << #mode_bit "|"; }
    YIELD_STAT_MODE_BIT_AS_STRING( S_IFDIR )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IFCHR )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IFREG )
#ifdef _WIN32
    YIELD_STAT_MODE_BIT_AS_STRING( S_IREAD )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IWRITE )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IEXEC )
#else
    YIELD_STAT_MODE_BIT_AS_STRING( S_IFIFO )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IFBLK )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IFLNK )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IRUSR )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IWUSR )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IXUSR )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IRGRP )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IWGRP )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IXGRP )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IROTH )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IWOTH )
    YIELD_STAT_MODE_BIT_AS_STRING( S_IXOTH )
    YIELD_STAT_MODE_BIT_AS_STRING( S_ISUID )
    YIELD_STAT_MODE_BIT_AS_STRING( S_ISGID )
    YIELD_STAT_MODE_BIT_AS_STRING( S_ISVTX )
  #endif
    os << "0), st_size: " << stbuf.get_size();
    os << ", st_mtime: " << stbuf.get_mtime() << ", st_ctime: " << stbuf.get_ctime() << ", st_atime: " << stbuf.get_atime();
#ifdef _WIN32
    os << ", attributes: " << stbuf.get_attributes();
#else
    os << ", nlink: " << stbuf.get_nlink();
#endif
    os << " }";
    return os;
  }


  template <class ValueType>
  class STLHashMap
  {
  public:
#if defined(_WIN32)
    typedef typename stdext::hash_map<uint32_t, ValueType>::iterator iterator;
    typedef typename stdext::hash_map<uint32_t, ValueType>::const_iterator const_iterator;
#elif defined(__GNUC__)
#if !defined(__sun) && ( __GNUC__ >= 4 || ( __GNUC__ == 4 && __GNUC_MINOR__ >= 3 ) )
    typedef typename std::tr1::unordered_map<uint32_t, ValueType>::iterator iterator;
    typedef typename std::tr1::unordered_map<uint32_t, ValueType>::const_iterator const_iterator;
#else
    typedef typename __gnu_cxx::hash_map<uint32_t, ValueType>::iterator iterator;
    typedef typename __gnu_cxx::hash_map<uint32_t, ValueType>::const_iterator const_iterator;
#endif
#endif


    inline iterator begin() { return std_hash_map.begin(); }
    inline void clear() { std_hash_map.clear(); }
    inline bool empty() const { return std_hash_map.empty(); }
    inline iterator end() { return std_hash_map.end(); }

    inline ValueType erase( const std::string& key ) { return remove( key.c_str() ); }
    inline ValueType erase( const char* key ) { return remove( key ); }

    // Apple's g++ doesn't like find to be const
    inline ValueType find( const std::string& key ) { return find( key.c_str() ); }
    inline ValueType find( const std::string& key, ValueType default_value ) { return find( key.c_str(), default_value ); }
    inline ValueType find( const char* key ) { return find( string_hash( key ) ); }
    inline ValueType find( const char* key, ValueType default_value ) { ValueType value = find( string_hash( key ) ); return value != 0 ? value : default_value; }
    ValueType find( uint32_t key )
    {
      iterator i = std_hash_map.find( key );
      if ( i != std_hash_map.end() )
        return i->second;
      else
        return 0;
    }

    inline void insert( const std::string& key, ValueType value ) { insert( key.c_str(), value ); }
    inline void insert( const char* key, ValueType value ) { insert( string_hash( key ), value ); }
    inline void insert( uint32_t key, ValueType value ) { std_hash_map.insert( std::make_pair( key, value ) ); }

    inline ValueType remove( const std::string& key ) { return remove( key.c_str() ); }
    inline ValueType remove( const char* key ) { return remove( string_hash( key ) ); }
    ValueType remove( uint32_t key )
    {
      iterator i = std_hash_map.find( key );
      if ( i != std_hash_map.end() )
      {
        ValueType value = i->second;
        std_hash_map.erase( i );
        return value;
      }
      else
        return 0;
    }

    inline size_t size() const { return std_hash_map.size(); }

  protected:
#if defined(_WIN32)
    stdext::hash_map<uint32_t, ValueType> std_hash_map;
#elif defined(__GNUC__)
#if !defined(__sun) && ( __GNUC__ >= 4 || ( __GNUC__ == 4 && __GNUC_MINOR__ >= 3 ) )
    std::tr1::unordered_map<uint32_t, ValueType> std_hash_map;
#else
    __gnu_cxx::hash_map<uint32_t, ValueType> std_hash_map;
#endif
#endif
  };


  class String : public Object, public std::string
  {
  public:
    String() { }
    String( size_t str_len ) { resize( str_len ); }
    String( const std::string& str ) : std::string( str ) { }
    String( const char* str ) : std::string( str ) { }
    String( const char* str, size_t str_len ) : std::string( str, str_len ) { }
    virtual ~String() { }

    // Object
    YIELD_OBJECT_PROTOTYPES( String, 13 );
  };


  class Thread : public Object
  {
  public:
    static unsigned long createTLSKey();
    static unsigned long getCurrentThreadId();
    static void* getTLS( unsigned long key );
    static void setCurrentThreadName( const char* thread_name ) { setThreadName( getCurrentThreadId(), thread_name ); }
    static void setTLS( unsigned long key, void* value );
    static void sleep( uint64_t timeout_ns );
    static void yield();


    Thread();

    unsigned long get_id() const { return id; }
    void set_name( const char* name ) { setThreadName( get_id(), name ); }
    bool set_processor_affinity( unsigned short logical_processor_i );
    bool set_processor_affinity( const ProcessorSet& logical_processor_set );
    virtual void start();

    virtual void run() = 0;

    // Object
    YIELD_OBJECT_PROTOTYPES( Thread, 14 );

  protected:
    virtual ~Thread();

  private:
    unsigned long id;
#if defined(_WIN32)
    void* handle;
#else
    pthread_t handle;
#endif

    static void setThreadName( unsigned long, const char* );
#ifdef _WIN32
    static unsigned long __stdcall thread_stub( void* );
#else
    static void* thread_stub( void* );
#endif
  };


  class TestCase
  {
  public:
    TestCase( TestSuite& test_suite, const std::string& name ); 
    virtual ~TestCase() { }

    virtual void setUp() { }
    virtual void runTest() { }
    virtual void run( TestResult& test_result );
    virtual void tearDown() { }
    const char* shortDescription() const { return short_description.c_str(); }

  protected:
    std::string short_description;
  };


  class TestResult
  {
  public:
    TestResult( auto_Object<Log> log )
      : log( log)
    { }

    virtual ~TestResult() { }

    auto_Object<Log> get_log() const { return log; }

  private:
    auto_Object<Log> log;
  };


  class TestRunner
  {
  public:
    TestRunner( Log::Level log_level = Log::LOG_NOTICE );
    virtual ~TestRunner() { }

    virtual int run( TestSuite& test_suite );

  private:
    Log::Level log_level;
  };


  class TestSuite : private std::vector<TestCase*>
  {
  public:
    TestSuite( const std::string& name );
    virtual ~TestSuite();

    void addTest( TestCase* test_case, bool own_test_case = true ); // for addTest( new ... )
    void addTest( TestCase& test_case, bool own_test_case = false ); // for addTest( *this )
    const std::string& get_name() const { return name; }
    virtual void run( TestResult& test_result );

  private:
    std::string name;

    std::vector<bool> own_test_cases;
  };


  class Unmarshaller : public Object
  {
  public:
    class Declaration
    {
    public:
      Declaration() : identifier( 0 ), tag( 0 ) { }
      Declaration( const char* identifier ) : identifier( identifier ), tag( 0 ) { }
      Declaration( const char* identifier, uint32_t tag ) : identifier( identifier ), tag( tag ) { }

      const char* get_identifier() const { return identifier; }
      uint32_t get_tag() const { return tag; }

    private:
      const char* identifier;
      uint32_t tag;
    };


    virtual ~Unmarshaller() { }

    virtual bool readBool( const Declaration& ) = 0;
    virtual double readDouble( const Declaration& ) = 0;
    virtual float readFloat( const Declaration& decl ) { return static_cast<float>( readDouble( decl ) ); }
    virtual int8_t readInt8( const Declaration& decl ) { return static_cast<int8_t>( readInt16( decl ) ); }
    virtual int16_t readInt16( const Declaration& decl ) { return static_cast<int16_t>( readInt32( decl ) ); }
    virtual int32_t readInt32( const Declaration& decl ) { return static_cast<int32_t>( readInt64( decl ) ); }
    virtual int64_t readInt64( const Declaration& decl ) = 0;
    virtual Object* readMap( const Declaration& decl, Object* value = NULL ) = 0;
    virtual void* readPointer( const Declaration& ) { return 0; }
    virtual Object* readSequence( const Declaration& decl, Object* value = NULL ) = 0;
    virtual void readString( const Declaration& decl, std::string& value ) = 0;
    virtual Object* readStruct( const Declaration& decl, Object* value = NULL ) = 0;
    virtual uint8_t readUint8( const Declaration& decl ) { return static_cast<uint8_t>( readInt8( decl ) ); }
    virtual uint16_t readUint16( const Declaration& decl ) { return static_cast<uint16_t>( readInt16( decl ) ); }
    virtual uint32_t readUint32( const Declaration& decl ) { return static_cast<uint32_t>( readInt32( decl ) ); }
    virtual uint64_t readUint64( const Declaration& decl ) { return static_cast<uint64_t>( readInt64( decl ) ); }
  };


  class Volume : public Object
  {
  public:    
    const static mode_t DEFAULT_DIRECTORY_MODE = S_IREAD|S_IWRITE|S_IEXEC;


    class listdirCallback
    {
    public:
      virtual ~listdirCallback() { }

      // Return false to stop listing
      virtual bool operator()( const Path& dirent_name ) = 0;
    };


    class readdirCallback
    {
    public:
      virtual ~readdirCallback() { }

      // Return false to stop reading
      virtual bool operator()( const Path& dirent_name, auto_Object<Stat> stbuf ) = 0;
    };


    YIELD_VOLUME_PROTOTYPES;

    // Convenience methods that don't make any system calls, so subclasses don't have to re-implement them
    virtual auto_Object<File> creat( const Path& path ) { return creat( path, File::DEFAULT_MODE ); }
    virtual auto_Object<File> creat( const Path& path, mode_t mode ) { return open( path, O_CREAT|O_WRONLY|O_TRUNC, mode ); }
    virtual bool exists( const Path& path );
    virtual bool listdir( const Path& path, listdirCallback& callback ) { return listdir( path, Path(), callback ); }
    virtual bool listdir( const Path& path, const Path& match_file_name_prefix, listdirCallback& callback );
    virtual bool listdir( const Path& path, std::vector<Path>& out_names ) { return listdir( path, Path(), out_names ); }
    virtual bool listdir( const Path& path, const Path& match_file_name_prefix, std::vector<Path>& out_names );
    virtual bool makedirs( const Path& path ) { return mktree( path, DEFAULT_DIRECTORY_MODE ); } // Python function name
    virtual bool makedirs( const Path& path, mode_t mode ) { return mktree( path, mode ); }
    virtual bool mkdir( const Path& path ) { return mkdir( path, DEFAULT_DIRECTORY_MODE ); }
    virtual bool mktree( const Path& path ) { return mktree( path, DEFAULT_DIRECTORY_MODE ); }
    virtual bool mktree( const Path& path, mode_t mode );
    virtual auto_Object<File> open( const Path& path ) { return open( path, O_RDONLY, File::DEFAULT_MODE, 0 ); }
    virtual auto_Object<File> open( const Path& path, uint32_t flags ) { return open( path, flags, File::DEFAULT_MODE, 0 ); }
    virtual auto_Object<File> open( const Path& path, uint32_t flags, mode_t mode ) { return open( path, flags, mode, 0 ); }
    virtual bool readdir( const Path& path, readdirCallback& callback ) { return readdir( path, Path(), callback ); }
    virtual bool rmtree( const Path& path );
    virtual auto_Object<Stat> stat( const Path& path ) { return getattr( path ); }
    virtual bool touch( const Path& path ) { return touch( path, File::DEFAULT_MODE ); }
    virtual bool touch( const Path& path, mode_t mode );

    // Object
    YIELD_OBJECT_PROTOTYPES( Volume, 15 );

  protected:
    virtual ~Volume() { }
  };


  class XDRMarshaller : public Marshaller
  {
  public:
    XDRMarshaller( std::ostream& target_ostream, bool in_map = false );

    XDRMarshaller& operator=( const XDRMarshaller& ) { return *this; }

    // Marshaller
    YIELD_MARSHALLER_PROTOTYPES;
    void writeFloat( const Declaration& decl, float value );
    void writeInt32( const Declaration& decl, int32_t value );

    // Object
    YIELD_OBJECT_PROTOTYPES( XDRMarshaller, 0 );

  private:
    std::ostream& target_ostream;
    bool in_map;

    virtual void beforeWrite( const Declaration& decl );
  };


  class XDRUnmarshaller : public Unmarshaller
  {
  public:
    XDRUnmarshaller( std::istream& source_istream );

    XDRUnmarshaller& operator=( const XDRUnmarshaller& ) { return *this; }

    // Unmarshaller
    YIELD_UNMARSHALLER_PROTOTYPES;
    float readFloat( const Declaration& decl );
    int32_t readInt32( const Declaration& decl );

    // Object
    YIELD_OBJECT_PROTOTYPES( XDRUnmarshaller, 0 );

  private:
    std::istream& source_istream;
  };
};

#endif
