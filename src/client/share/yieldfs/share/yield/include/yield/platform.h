// Copyright 2003-2008 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef YIELD_PLATFORM_H
#define YIELD_PLATFORM_H

#define __STDC_LIMIT_MACROS
#ifdef _WIN32
#include <hash_map>
#include <msstdint.h>
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
#ifndef DLLEXPORT
#define DLLEXPORT extern "C" __declspec(dllexport)
#endif
#ifndef MAX_PATH
#define MAX_PATH 260
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
#define INVALID_HANDLE_VALUE -1
#ifdef __MACH__
#define MAX_PATH PATH_MAX
#else
#define MAX_PATH 512
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

#define YIELD_EXCEPTION_WHAT_BUFFER_LENGTH 128

#define YIELD_FILE_PROTOTYPES \
    virtual bool close(); \
    virtual bool datasync(); \
    virtual bool flush(); \
    virtual YIELD::auto_Object<YIELD::Stat> getattr(); \
    virtual bool getxattr( const std::string& name, std::string& out_value ); \
    virtual bool listxattr( std::vector<std::string>& out_names ); \
    virtual Stream::Status read( void* buffer, size_t buffer_len, uint64_t offset, size_t* out_bytes_read ); \
    virtual bool removexattr( const std::string& name ); \
    virtual bool setxattr( const std::string& name, const std::string& value, int flags ); \
    virtual bool sync(); \
    virtual bool truncate( uint64_t offset ); \
    virtual Stream::Status writev( const iovec* buffers, uint32_t buffers_count, uint64_t offset, size_t* out_bytes_written );

#define YIELD_STRUCTURED_INPUT_STREAM_PROTOTYPES \
virtual bool readBool( const Declaration& decl ); \
virtual double readDouble( const Declaration& decl ); \
virtual int64_t readInt64( const Declaration& decl ); \
virtual YIELD::Object* readObject( const Declaration& decl, YIELD::Object* value = NULL, YIELD::Object::GeneralType value_general_type = YIELD::Object::UNKNOWN ); \
virtual void readString( const Declaration& decl, std::string& ); \

#define YIELD_STRUCTURED_OUTPUT_STREAM_PROTOTYPES \
virtual void writeBool( const Declaration& decl, bool value ); \
virtual void writeDouble( const Declaration& decl, double value ); \
virtual void writeInt64( const Declaration& decl, int64_t value ); \
virtual void writeObject( const Declaration& decl, YIELD::Object& value, YIELD::Object::GeneralType value_general_type = YIELD::Object::UNKNOWN ); \
virtual void writeString( const Declaration&, const char* value, size_t value_len ); \

#define YIELD_STRING_HASH_NEXT( c, hash ) hash = hash ^ ( ( hash << 5 ) + ( hash >> 2 ) + c )

#define YIELD_TEST_SUITE_EX( TestSuiteName, TestSuiteType ) \
  YIELD::TestSuite& TestSuiteName##TestSuite() { static TestSuiteType* ts = new TestSuiteType( #TestSuiteName ); return *ts; } \
class TestSuiteName##TestSuiteDest { public: ~TestSuiteName##TestSuiteDest() { delete &TestSuiteName##TestSuite(); } }; \
TestSuiteName##TestSuiteDest TestSuiteName##TestSuiteDestObj;

#define YIELD_TEST_SUITE( TestSuiteName ) YIELD_TEST_SUITE_EX( TestSuiteName, YIELD::TestSuite )

#define YIELD_TEST_CASE_EX( TestCaseName, TestCaseType, TestSuiteName ) \
extern YIELD::TestSuite& TestSuiteName##TestSuite(); \
class TestCaseName##Test : public TestCaseType \
{ \
public:\
  TestCaseName##Test() : TestCaseType( #TestCaseName "Test", TestSuiteName##TestSuite() ) { }\
  void runTest();\
};\
TestCaseName##Test TestCaseName##Test_inst;\
void TestCaseName##Test::runTest()

#define YIELD_TEST_CASE( TestCaseName, TestSuiteName ) YIELD_TEST_CASE_EX( TestCaseName, YIELD::TestCase, TestSuiteName )

#ifdef YIELD_BUILDING_STANDALONE_TEST
#define YIELD_TEST_MAIN( TestSuiteName ) \
  int main( int argc, char** argv ) { return YIELD::TestRunner().run( TestSuiteName##TestSuite() ); }
#else
#define YIELD_TEST_MAIN( TestSuiteName )
#endif

#define YIELD_VOLUME_PROTOTYPES \
    virtual bool access( const YIELD::Path& path, int amode ); \
    virtual bool chmod( const YIELD::Path& path, mode_t mode ); \
    virtual bool chown( const YIELD::Path& path, int32_t uid, int32_t gid ); \
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


namespace YIELD
{
#ifdef _WIN32
  typedef void* fd_t;
  typedef uint64_t timeout_ns_t;
#else
  typedef int fd_t;
#ifdef __sun
  typedef uint32_t timeout_ns_t;
#else
  typedef uint64_t timeout_ns_t;
#endif
#endif
  class Path;
  class Stat;
  class StructuredInputStream;
  class StructuredOutputStream;


  static inline uint32_t atomic_cas( volatile uint32_t* current_value, uint32_t new_value, uint32_t old_value )
  {
#if defined(_WIN32)
    return static_cast<uint32_t>( InterlockedCompareExchange( reinterpret_cast<volatile long*>( reinterpret_cast<volatile int32_t*>( current_value ) ), static_cast<long>( new_value ), static_cast<long>( old_value ) ) );
#elif defined(__YIELD_HAVE_GNUC_ATOMIC_OPS_INTRINSICS)
    return __sync_val_compare_and_swap( current_value, old_value, new_value );
#elif defined(__sun)
    return atomic_cas_32( current_value, old_value, new_value );
#elif defined(__i386__) || defined(__x86_64__)
    uint32_t prev;
    asm volatile(	"lock\n"
            "cmpxchgl %1,%2\n"
          : "=a" ( prev )
                : "r" ( new_value ), "m" ( *current_value ) , "0" ( old_value )
                : "memory"
              );
    return prev;
#elif defined(__ppc__)
    uint32_t prev;
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

  static inline uint64_t atomic_cas( volatile uint64_t* current_value, uint64_t new_value, uint64_t old_value )
  {
#if defined(_WIN32)
    return static_cast<uint64_t>( InterlockedCompareExchange64( reinterpret_cast<volatile long long*>( reinterpret_cast<volatile int64_t*>( current_value ) ), static_cast<long>( new_value ), static_cast<long>( old_value ) ) );
#elif defined(__YIELD_HAVE_GNUC_ATOMIC_OPS_INTRINSICS)
    return __sync_val_compare_and_swap( current_value, old_value, new_value );
#elif defined(__sun)
    return atomic_cas_64( current_value, old_value, new_value );
#elif defined(__x86_64__)
    uint64_t prev;
    asm volatile(	"lock\n"
            "cmpxchgq %1,%2\n"
          : "=a" ( prev )
                : "r" ( new_value ), "m" ( *current_value ) , "0" ( old_value )
                : "memory"
              );
    return prev;
#elif defined(__ppc__)
    uint64_t prev;
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

  static inline uint32_t atomic_inc( volatile uint32_t* current_value )
  {
#if defined(_WIN32)
    return static_cast<uint32_t>( InterlockedIncrement( reinterpret_cast<volatile long*>( reinterpret_cast<volatile int32_t*>( current_value ) ) ) );
#elif defined(__YIELD_HAVE_GNUC_ATOMIC_OPS_INTRINSICS)
    return __sync_add_and_fetch( current_value, 1 );
#elif defined(__sun)
    return atomic_inc_32_nv( current_value );
#else
    uint32_t old_value, new_value;

    do
    {
      old_value = *current_value;
      new_value = old_value + 1;
    }
    while ( atomic_cas( current_value, new_value, old_value ) != old_value );

    return new_value;
#endif
  }

  static inline uint32_t atomic_dec( volatile uint32_t* current_value )
  {
#if defined(_WIN32)
    return static_cast<uint32_t>( InterlockedDecrement( reinterpret_cast<volatile long*>( reinterpret_cast<volatile int32_t*>( current_value ) ) ) );
#elif defined(__YIELD_HAVE_GNUC_ATOMIC_OPS_INTRINSICS)
    return __sync_sub_and_fetch( current_value, 1 );
#elif defined(__sun)
    return atomic_dec_32_nv( current_value );
#else
    uint32_t old_value, new_value;

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


  class Stream
  {
  public:
    enum Status { STREAM_STATUS_ERROR = -1, STREAM_STATUS_OK = 0, STREAM_STATUS_WANT_READ = 1, STREAM_STATUS_WANT_WRITE = 2 };

    virtual ~Stream()
    { }
  };


  class InputStream : public Stream
  {
  public:
    virtual ~InputStream() { }

    virtual Stream::Status read( void* buffer, size_t buffer_len, size_t* out_bytes_read = 0 ) = 0;
  };


  class OutputStream : public Stream
  {
  public:
    virtual ~OutputStream() { }

    OutputStream& operator<<( const char* buffer ) { write( buffer ); return *this; }
    OutputStream& operator<<( const std::string& buffer ) { write( buffer ); return *this; }
    OutputStream& operator<<( const struct iovec& buffer ) { write( buffer ); return *this; }

    virtual Stream::Status write( const char* buffer, size_t* out_bytes_written = 0 ) { return write( buffer, std::strlen( buffer ), out_bytes_written ); }
    virtual Stream::Status write( const std::string& buffer, size_t* out_bytes_written = 0 ) { return write( buffer.c_str(), buffer.size(), out_bytes_written ); }
    virtual Stream::Status write( const void* buffer, size_t buffer_len, size_t* out_bytes_written = 0 ) { struct iovec buffers; buffers.iov_base = const_cast<void*>( buffer ); buffers.iov_len = static_cast<size_t>( buffer_len ); return writev( &buffers, 1, out_bytes_written ); }
    virtual Stream::Status write( const struct iovec& buffer, size_t* out_bytes_written = 0 ) { return writev( &buffer, 1, out_bytes_written ); }
    virtual Stream::Status writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written = 0 ) = 0;
  };


  class Object
  {      
  public:
    Object() : refcnt( 1 )
    { }

    enum GeneralType {
                       UNKNOWN = 0, EVENT,
                       REQUEST, RESPONSE, EXCEPTION_RESPONSE,
                       SEQUENCE, MAP, STRUCT, STRING
                     };


    static inline void decRef( Object& object )
    {
      if ( atomic_dec( &object.refcnt ) == 0 )
        delete &object;
    }

    static inline void decRef( Object* object )
    {
      if ( object )
        Object::decRef( *object );
    }

    template <class ObjectType>
    static inline ObjectType& incRef( ObjectType& object )
    {
      atomic_inc( &object.refcnt );
      return object;
    }

    template <class ObjectType>
    static inline ObjectType* incRef( ObjectType* object )
    {
      if ( object )
        atomic_inc( &object->refcnt );
      return object;
    }

    inline Object& incRef()
    {
      atomic_inc( &this->refcnt );
      return *this;
    }

    virtual Stream::Status deserialize( InputStream& input_stream, size_t* out_bytes_read = 0 ) { return Stream::STREAM_STATUS_ERROR; }
    virtual void deserialize( StructuredInputStream& ) { }
    virtual GeneralType get_general_type() const { return UNKNOWN; };
    virtual uint64_t get_size() const { return 0; } // For arrays
    virtual const char* get_type_name() const { return 0; }
    virtual uint32_t get_type_id() const { return 0; }
    virtual Stream::Status serialize( OutputStream& output_stream, size_t* out_bytes_written = 0 ) { return Stream::STREAM_STATUS_ERROR; }
    virtual void serialize( StructuredOutputStream& ) { }

#define YIELD_OBJECT_TYPE_INFO( general_type, type_name, type_id )\
    virtual const char* get_type_name() const { return type_name; }\
    virtual uint32_t get_type_id() const { return __type_id; }\
    virtual YIELD::Object::GeneralType get_general_type() const { return general_type; }\
    const static uint32_t __type_id = static_cast<uint32_t>( type_id );

#define YIELD_OBJECT_TYPE_ID( type ) type::__type_id

  protected:
    virtual ~Object()
    { }

  private:
    volatile uint32_t refcnt;
  };


  class StructuredStream
  {
  public:
    class Declaration
    {
    public:
      Declaration() : type_name( 0 ), identifier( 0 ), uid( 0 ) { }
      Declaration( const char* identifier ) : type_name( 0 ), identifier( identifier ), uid( 0 ) { }
      Declaration( const char* identifier, uint32_t uid ) : type_name( 0 ), identifier( identifier ), uid( uid ) { }
      Declaration( const char* type_name, const char* identifier ) : type_name( type_name ), identifier( identifier ), uid( 0 ) { }
      Declaration( const char* type_name, const char* identifier, uint32_t uid ) : type_name( type_name ), identifier( identifier ), uid( uid ) { }

      const char* type_name;
      const char* identifier;
      uint32_t uid;
    };
  };



  class StructuredInputStream : public StructuredStream
    {
  public:
    virtual ~StructuredInputStream() { }

    virtual bool readBool( const Declaration& ) = 0;
    virtual double readDouble( const Declaration& ) = 0;
    virtual float readFloat( const Declaration& decl ) { return static_cast<float>( readDouble( decl ) ); }
    virtual int8_t readInt8( const Declaration& decl ) { return static_cast<int8_t>( readInt16( decl ) ); }
    virtual int16_t readInt16( const Declaration& decl ) { return static_cast<int16_t>( readInt32( decl ) ); }
    virtual int32_t readInt32( const Declaration& decl ) { return static_cast<int32_t>( readInt64( decl ) ); }
    virtual int64_t readInt64( const Declaration& decl ) = 0;
    virtual void* readPointer( const Declaration& decl ) { return 0; }
    virtual Object* readObject( const Declaration&, Object* value = NULL, Object::GeneralType object_general_type = Object::UNKNOWN ) = 0; // value_general_type = Object::UKNONWN will check value's get_general_type()
    virtual void readString( const Declaration&, std::string& value ) = 0;
    virtual uint8_t readUint8( const Declaration& decl ) { return static_cast<uint8_t>( readInt8( decl ) ); }
    virtual uint16_t readUint16( const Declaration& decl ) { return static_cast<uint16_t>( readInt16( decl ) ); }
    virtual uint32_t readUint32( const Declaration& decl ) { return static_cast<uint32_t>( readInt32( decl ) ); }
    virtual uint64_t readUint64( const Declaration& decl ) { return static_cast<uint64_t>( readInt64( decl ) ); }
  };


  class StructuredOutputStream : public StructuredStream
  {
  public:
    virtual ~StructuredOutputStream() { }

    virtual void writeBool( const Declaration&, bool value ) = 0;
    virtual void writeDouble( const Declaration&, double value ) = 0;
    virtual void writeFloat( const Declaration& decl, float value ) { writeDouble( decl, value ); }
    virtual void writeInt8( const Declaration& decl, int8_t value ) { writeInt16( decl, value ); }
    virtual void writeInt16( const Declaration& decl, int16_t value ) { writeInt32( decl, value ); }
    virtual void writeInt32( const Declaration& decl, int32_t value ) { writeInt64( decl, value ); }
    virtual void writeInt64( const Declaration&, int64_t ) = 0;
    virtual void writePointer( const Declaration&, void* ) { }
    virtual void writeObject( const Declaration&, Object&, Object::GeneralType = Object::UNKNOWN ) = 0; // value_general_type = Object::UKNONWN will check value's get_general_type()
    virtual void writeString( const Declaration& decl, const std::string& value ) { writeString( decl, value.c_str(), value.size() ); }
    virtual void writeString( const Declaration& decl, const char* value ) { writeString( decl, value, std::strlen( value ) ); }
    virtual void writeString( const Declaration&, const char* value, size_t value_len ) = 0;
    virtual void writeUint8( const Declaration& decl, uint8_t value ) { writeUint16( decl, value ); }
    virtual void writeUint16( const Declaration& decl, uint16_t value ) { writeUint32( decl, value ); }
    virtual void writeUint32( const Declaration& decl, uint32_t value ) { writeUint64( decl, value ); }
    virtual void writeUint64( const Declaration& decl, uint64_t value ) { writeInt64( decl, static_cast<int64_t>( value ) ); }
  };


  class Time
  {
  public:
    // Unix epoch times (from January 1, 1970)
    static uint64_t getCurrentUnixTimeNS();
    static double getCurrentUnixTimeMS() { return static_cast<double>( getCurrentUnixTimeNS() ) / static_cast<double>( NS_IN_MS ); }
    static double getCurrentUnixTimeS() { return static_cast<double>( getCurrentUnixTimeNS() ) / static_cast<double>( NS_IN_S ); }

    Time() : unix_time_ns( getCurrentUnixTimeNS() ) { }
    Time( double unix_time_s ) : unix_time_ns( static_cast<uint64_t>( unix_time_s * NS_IN_S ) ) { }
    Time( uint32_t unix_time_s ) : unix_time_ns( unix_time_s * NS_IN_S ) { }
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
    Time& operator=( uint32_t unix_time_s ) { this->unix_time_ns = unix_time_s * NS_IN_S; return *this; }
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
    ~auto_Object() { if ( object ) Object::decRef( *object ); }

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
    bool try_acquire(); // Never blocks
    bool timed_acquire( timeout_ns_t timeout_ns ); // May block for timeout_ns
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


  class File : public Object, public InputStream, public OutputStream
  {
  public:
//    typedef void ( *aio_read_completion_routine_t )( unsigned long error_code, size_t buffer_len, void* context );
//    typedef void ( *aio_write_completion_routine_t )( unsigned long error_code, size_t buffer_len, void* context );


    const static uint32_t DEFAULT_FLAGS = O_RDONLY;
    const static mode_t DEFAULT_MODE = S_IREAD|S_IWRITE;
    const static uint32_t DEFAULT_ATTRIBUTES = 0;


    // Constructors throw exceptions if an open fails
    explicit File( const Path& path );
    explicit File( const Path& path, uint32_t flags );
    explicit File( const Path& path, uint32_t flags, mode_t mode );
    explicit File( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes );
#ifdef _WIN32
    // Since fd_t is void* on Windows we need these to delegate to the const Path& variant rather than the fd_t one
    explicit File( char* path );
    explicit File( char* path, uint32_t flags );
    explicit File( const char* path );
    explicit File( const char* path, uint32_t flags );
#endif
    explicit File( fd_t fd );
    // A factory method that returns NULL instead of throwing exceptions
    static auto_Object<File> open( const Path& path ) { return open( path, DEFAULT_FLAGS, DEFAULT_MODE ); }
    static auto_Object<File> open( const Path& path, uint32_t flags ) { return open( path, flags, DEFAULT_MODE ); }
    static auto_Object<File> open( const Path& path, uint32_t flags, mode_t mode ) { return open( path, flags, mode, DEFAULT_ATTRIBUTES ); }
    static auto_Object<File> open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes );
    operator fd_t() const { return fd; }

    YIELD_FILE_PROTOTYPES;

//    virtual int aio_read( void* buffer, size_t buffer_len, aio_read_completion_routine_t, void* context );
//    virtual int aio_write( const void* buffer, size_t buffer_len, aio_write_completion_routine_t, void* context );
    virtual bool seek( uint64_t offset ); // from the beginning of the file
    virtual bool seek( uint64_t offset, unsigned char whence );

    virtual Stream::Status write( const void* buffer, size_t buffer_len, uint64_t offset, size_t* out_bytes_written = 0 )
    {
      iovec buffers[1];
      buffers[0].iov_base = static_cast<char*>( const_cast<void*>( buffer ) );
      buffers[0].iov_len = buffer_len;
      return writev( buffers, 1, offset, out_bytes_written );
    }

    // Object
    inline File& incRef() { return Object::incRef( *this ); }

    // InputStream
    virtual Stream::Status read( void* buffer, size_t buffer_len, size_t* out_bytes_read = 0 );

     // OutputStream
    virtual Stream::Status writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written = 0 );

  protected:
    File();
    virtual ~File() { close(); }

  private:
    File( const File& other )  // Prevent copying
    {
      DebugBreak();
    }

    fd_t fd;

    friend class Stat;
    static fd_t _open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes );
    static bool _close( fd_t fd );

    /*
#if defined(_WIN32)
    static void __stdcall overlapped_read_completion( unsigned long, unsigned long, void* );
    static void __stdcall overlapped_write_completion( unsigned long, unsigned long, void* );
#elif defined(YIELD_HAVE_POSIX_FILE_AIO)
    static void aio_read_notify( sigval_t );
    static void aio_write_notify( sigval_t );
#endif
    */
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
          memcpy( &key_len, data_p, sizeof( key_len ) ); data_p += sizeof( key_len );
          key = data_p; data_p += key_len;
          memcpy( &value, data_p, sizeof( value ) );
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
          std::memcpy( data_p, &key_len, sizeof( key_len ) ); data_p += sizeof( key_len );
          std::memcpy( data_p, key, key_len ); data_p += key_len;
          std::memcpy( data_p, &value, sizeof( value ) ); data_p += sizeof( value );
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
              std::memcpy( new_data_p, data_p, test_entry.size() );
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
              memcpy( data_p + test_entry.get_value_offset(), &value, sizeof( value ) );
              return;
            }
            else
              data_p += test_entry.size();
          }

          unsigned char *new_data = new unsigned char[data_len + insert_entry.size()], *new_data_p = new_data;
          memcpy( new_data_p, data, data_len );
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
    inline void erase( const unsigned char* key ) { erase( key, std::strlen( key ) ); }
    inline void erase( const char* key, size_t key_len ) { erase( reinterpret_cast<const unsigned char*>( key ), static_cast<uint16_t>( key_len ) ); }
    ValueType erase( const unsigned char* key, uint16_t key_len )
    {
      uint32_t slot_i = string_hash( key, key_len ) % slot_count;
      return slots[slot_i].erase( key, key_len );
    }

    inline ValueType find( const std::string& key ) const { return find( key.c_str(), key.size() ); }
    inline ValueType find( const char* key ) const { return find( key, std::strlen( key ) ); }
    inline ValueType find( const char* key, size_t key_len ) const { return find( reinterpret_cast<const unsigned char*>( key ), static_cast<uint16_t>( key_len ) ); }
    ValueType find( const unsigned char* key, uint16_t key_len ) const
    {
      uint32_t slot_i = string_hash( key, key_len ) % slot_count;
      return slots[slot_i].find( key, key_len );
    }

    inline void insert( const std::string& key, ValueType value ) { insert( key.c_str(), key.size(), value ); }
    inline void insert( const char* key, ValueType value ) { insert( key, std::strlen( key ), value ); }
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
    inline ValueType erase( const unsigned char* key ) { return erase( key, std::strlen( key ) ); }
    inline ValueType erase( const char* key, size_t key_len ) { return erase( reinterpret_cast<const unsigned char*>( key ), static_cast<uint16_t>( key_len ) ); }
    inline ValueType erase( const unsigned char* key, uint16_t key_len ) { return root_bucket.erase( key, key_len ); }

    inline void erase_by_prefix( const std::string& key_prefix, std::vector<ValueType>* out_values = NULL ) { erase_by_prefix( key_prefix.c_str(), key_prefix.size(), out_values );  }
    inline void erase_by_prefix( const char* key_prefix, std::vector<ValueType>* out_values = NULL ) { erase_by_prefix( key_prefix, std::strlen( key_prefix ), out_values );  }
    inline void erase_by_prefix( const char* key_prefix, size_t key_prefix_len, std::vector<ValueType>* out_values = NULL ) { erase_by_prefix( reinterpret_cast<const unsigned char*>( key_prefix ), static_cast<uint16_t>( key_prefix_len ), out_values );  }
    inline void erase_by_prefix( const unsigned char* key_prefix, uint16_t key_prefix_len, std::vector<ValueType>* out_values = NULL ) { root_bucket.erase_by_prefix( key_prefix, key_prefix_len, out_values ); }

    inline ValueType find( const std::string& key ) const { return find( key.c_str(), key.size() ); }
    inline ValueType find( const char* key ) const { return find( key, std::strlen( key ) ); }
    inline ValueType find( const char* key, size_t key_len ) const { return find( reinterpret_cast<const unsigned char*>( key ), static_cast<uint16_t>( key_len ) ); }
    inline ValueType find( const unsigned char* key, uint16_t key_len ) const { return root_bucket.find( key, key_len ); }

    inline void find_by_prefix( const std::string& key_prefix, std::vector<ValueType>& out_values ) const { find_by_prefix( key_prefix.c_str(), key_prefix.size(), out_values );  }
    inline void find_by_prefix( const char* key_prefix, std::vector<ValueType>& out_values ) const { find_by_prefix( key_prefix, std::strlen( key_prefix ), out_values );  }
    inline void find_by_prefix( const char* key_prefix, size_t key_prefix_len, std::vector<ValueType>& out_values ) const { find_by_prefix( reinterpret_cast<const unsigned char*>( key_prefix ), static_cast<uint16_t>( key_prefix_len ), out_values );  }
    inline void find_by_prefix( const unsigned char* key_prefix, uint16_t key_prefix_len, std::vector<ValueType>& out_values ) const { root_bucket.find_by_prefix( key_prefix, key_prefix_len, out_values ); }

    inline void insert( const std::string& key, ValueType value ) { insert( key.c_str(), key.size(), value ); }
    inline void insert( const char* key, ValueType value ) { insert( key, std::strlen( key ), value ); }
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


  class Log : public Object, public OutputStream
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


    class Stream : public OutputStream
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

      // OutputStream
      OutputStream::Status writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written = 0 );

    private:
      friend class Log;

      Stream( auto_Object<Log> log, Level level );

      auto_Object<Log> log;
      Level level;

      std::ostringstream oss;
    };


    Log( const Path& file_path, Level level );
    Log( std::ostream&, Level level );
    Log( std::auto_ptr<OutputStream> underlying_output_stream, Level level );

    inline Level get_level() const { return level; }
    inline OutputStream& get_underlying_output_stream() const { return *underlying_output_stream; }
    Stream getStream() { return Stream( incRef(), level ); }
    Stream getStream( Level level ) { return Stream( incRef(), level ); }
    void set_level( Level level ) { this->level = level; }

    inline void write( const char* str, Level level )
    {
      write( str, std::strlen( str ), level );
    }

    inline void write( const std::string& str, Level level )
    {
      write( str.c_str(), str.size(), level );
    }

    inline void write( const char* str, size_t str_len, Level level )
    {
      if ( level <= this->level )
        OutputStream::write( str, str_len );
    }

    inline void write( const void* str, size_t str_len, Level level )
    {
      return write( static_cast<const unsigned char*>( str ), str_len, level );
    }

    inline void write( const unsigned char* str, size_t str_len, Level level )
    {
      if ( level <= this->level )
        write( str, str_len );
    }

    void writev( const iovec* buffers, uint32_t buffers_count, Level level )
    {
      if ( level <= this->level )
        writev( buffers, buffers_count );
    }

    // Object
    inline Log& incRef() { return Object::incRef( *this ); }

    // OutputStream
    OutputStream::Status writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written = 0 )
    {
      return underlying_output_stream->writev( buffers, buffers_count, out_bytes_written );
    }

  private:
    ~Log() { }


    std::auto_ptr<OutputStream> underlying_output_stream;
    Level level;


    static char* sanitize( const unsigned char* str, size_t str_len );
    void write( const unsigned char* str, size_t str_len );
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


  class MemoryMappedFile : public File
  {
  public:
    MemoryMappedFile( const Path& path );
    MemoryMappedFile( const Path& path, size_t minimum_size );
    MemoryMappedFile( const Path& path, size_t minimum_size, uint32_t flags );

    void resize( size_t );
    inline char* getRegionStart() { return start; }
    inline char* getRegionEnd() { return start + size; }
    inline size_t getRegionSize() { return size; }

    virtual void writeBack();
    virtual void writeBack( size_t offset, size_t length );
    virtual void writeBack( void* ptr, size_t length );

    // Object
    inline MemoryMappedFile& incRef() { return Object::incRef( *this ); }

    // File
    virtual bool close();

  protected:
    virtual ~MemoryMappedFile() { close(); }

  private:
    uint32_t flags;

    size_t size;
    char* start;
#ifdef _WIN32
    void* mapping;
#endif

    void init( const Path& path, size_t minimum_size, uint32_t flags );
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
    bool timed_acquire( timeout_ns_t timeout_ns ); // May block for timeout_ns
    void release();

  private:
    void* os_handle;
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

      uint32_t copied_tail, last_try_pos, try_pos; // te, ate, temp
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
      uint32_t copied_head, try_pos;
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
    volatile uint32_t head, tail;

#if defined(__LLP64__) || defined(__LP64__)
    typedef uint64_t uint_ptr;
    const static uint_ptr PTR_HIGH_BIT = 0x8000000000000000;
    const static uint_ptr PTR_LOW_BITS = 0x7fffffffffffffff;
#else
    typedef uint32_t uint_ptr;
    const static uint_ptr PTR_HIGH_BIT = 0x80000000;
    const static uint_ptr PTR_LOW_BITS = 0x7fffffff;
#endif
  };


  class NOPLock
  {
  public:
    inline bool acquire() { return true; }
    inline bool try_acquire() { return true; }
    inline bool timed_acquire( timeout_ns_t ) { return true; }
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
    inline Path& incRef() { return Object::incRef( *this ); }

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


  class PrettyPrintOutputStream : public StructuredOutputStream
  {
  public:
    PrettyPrintOutputStream( OutputStream& underlying_output_stream );

    // StructuredOutputStream
    YIELD_STRUCTURED_OUTPUT_STREAM_PROTOTYPES;

  private:
    OutputStream& underlying_output_stream;
  };


  class Process : public InputStream, public OutputStream
  {
  public:
    Process( const Path& executable_file_path, const char** argv );
    ~Process();

    bool poll( int* out_return_code = 0 ); // Calls waitpid() but WNOHANG, out_return_code can be NULL
    Stream::Status read_stderr( void* buffer, size_t buffer_len, size_t* out_bytes_read = 0 );
    int wait(); // Calls waitpid() and suspends the calling process until the child exits, use carefully

    // InputStream
    Stream::Status read( void* buffer, size_t buffer_len, size_t* out_bytes_read = 0 );

    // OutputStream
    Stream::Status write( const void* buffer, size_t buffer_len, size_t* out_bytes_written );

  private:
#ifdef _WIN32
    void *hChildStdInput_read, *hChildStdInput_write, *hChildStdOutput_read, *hChildStdOutput_write, *hChildStdError_read, *hChildStdError_write, *hChildProcess, *hChildThread;
#else
    int child_pid, child_stdin_pipe[2], child_stdout_pipe[2], child_stderr_pipe[2];
#endif

    Stream::Status _read( fd_t fd, void* buffer, size_t buffer_len, size_t* out_bytes_read );
  };


  class ProcessorSet : public Object
  {
  public:
    ProcessorSet();
    ProcessorSet( uint32_t from_mask );
    ProcessorSet( const ProcessorSet& );

    void clear();
    void clear( uint16_t processor_i );
    uint16_t count() const;
    bool empty() const;
    bool isset( uint16_t processor_i ) const;
    void set( uint16_t processor_i );    

  private:
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


  class RRD : public Object
  {
  public:
    class Record : public Object
    {
    public:
      Record()
        : value( 0 )
      { }

      Record( double value )
        : value( value )
      { }

      Record( const Time& time, double value )
        : time( time ), value( value )
      { }

      Stream::Status deserialize( InputStream& input_stream );
      const Time& get_time() const { return time; }
      double get_value() const { return value; }
      operator double() const { return value; }
      Stream::Status serialize( OutputStream& output_stream );

      // Object
      inline Record& incRef() { return Object::incRef( *this ); }

    private:
      Time time;
      double value;
    };


    RRD( const Path& file_path, uint32_t file_flags = File::DEFAULT_FLAGS );

    void append( double value );
    void fetch( std::vector<Record>& out_records );
    void fetch( const Time& start_time, std::vector<Record>& out_records );
    void fetch( const Time& start_time, const Time& end_time, std::vector<Record>& out_records );

    // Object
    inline RRD& incRef() { return Object::incRef( *this ); }

  private:
    ~RRD();


    Path current_file_path;

    File* current_file;
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
    static SharedLibrary* open( const Path& file_prefix, const char* argv0 = 0 ); // Returns NULL if the library cannot be loaded
    SharedLibrary( const Path& file_prefix, const char* argv0 = 0 ); // Throws exceptions if the library cannot be loaded

    void* getHandle() { return handle; }
    void* getFunction( const char* function_name ); // Returns NULL instead of throwing exceptions

    // Object
    SharedLibrary& incRef() { return Object::incRef( *this ); }

  private:
    SharedLibrary();
    ~SharedLibrary();

    bool init( const YIELD::Path& file_prefix, const char* argv0 = 0 );

    void* handle;
  };


  class Stat : public Object
  {
  public:
    Stat( const Path& path ) { init( path ); } // Initialize the object from a stat() system call on path
    Stat( fd_t fd ) { init( fd ); } // Initialize the object from an fstat() system call on fd
#ifdef _WIN32
    // Since fd_t is void* on Windows we need these to delegate constructors to the const Path& variant rather than the fd_t one
    Stat( char* path ) { init( Path( path ) ); }
    Stat( const char* path ) { init( Path( path ) ); }
    Stat( mode_t mode, uint64_t size, const Time& atime, const Time& mtime, const Time& ctime, uint32_t attributes );
    Stat( const WIN32_FIND_DATA& );
    Stat( uint32_t nFileSizeHigh, uint32_t nFileSizeLow, const FILETIME* ftLastWriteTime, const FILETIME* ftCreationTime, const FILETIME* ftLastAccessTime, uint32_t dwFileAttributes ); // For doing FILETIME -> Unix conversions in Dokan; deduces mode from dwFileAttributes
#else
    Stat( mode_t mode, nlink_t nlink, uid_t uid, gid_t gid, uint64_t size, const Time& atime, const Time& mtime, const Time& ctime );
#endif
    Stat( const Stat& );
    virtual ~Stat() { }

    mode_t get_mode() const { return mode; }
#ifndef _WIN32
    nlink_t get_nlink() const { return nlink; }
    uid_t get_uid() const { return uid; }
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
    virtual operator WIN32_FIND_DATA() const;
    virtual operator BY_HANDLE_FILE_INFORMATION() const;
#endif

    // Object
    Stat& incRef() { return Object::incRef( *this ); }

  protected:
    void init( const Path& );  // Opens path, fstats the file descriptor, and closes it
    void init( fd_t ); // fstats the file descriptor but doesn't close it
#ifdef _WIN32
    void init( uint32_t nFileSizeHigh, uint32_t nFileSizeLow, const FILETIME* ftLastWriteTime, const FILETIME* ftCreationTime, const FILETIME* ftLastAccessTime, uint32_t dwFileAttributes );
#endif

    mode_t mode;
#ifndef _WIN32
    nlink_t nlink;
    uid_t uid;
    gid_t gid;
#endif
    uint64_t size;
    Time atime, mtime, ctime;
#ifdef _WIN32
    uint32_t attributes;
#endif
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


  class String : public Object, public InputStream, public OutputStream, public std::string
  {
  public:
    String() : hash( 0 ), read_pos( 0 ) { }
    String( size_t str_len ) : hash( 0 ), read_pos( 0 ) { resize( str_len ); }
    String( const std::string& str ) : std::string( str ), hash( 0 ), read_pos( 0 ) { }
    String( const char* str ) : std::string( str ), hash( 0 ), read_pos( 0 ) { }
    String( const char* str, size_t str_len ) : std::string( str, str_len ), hash( 0 ), read_pos( 0 ) { }

    Stream::Status deserialize( InputStream& input_stream, size_t* out_bytes_read )
    {
      if ( read_pos < size() )
      {
        size_t temp_bytes_read;
        for ( ;; )
        {
          Stream::Status status = input_stream.read( const_cast<char*>( c_str() ) + read_pos, size() - read_pos, &temp_bytes_read );
          if ( status == STREAM_STATUS_OK )
          {
            read_pos += temp_bytes_read;
            if ( read_pos == size() )
            {
              read_pos = 0;              
              if ( out_bytes_read )
                *out_bytes_read = temp_bytes_read;
              return STREAM_STATUS_OK;
            }
          }
          else
            return status;
        }
      }
      else
        return STREAM_STATUS_ERROR;
    }

    uint32_t get_hash()
    {
      if ( hash == 0 )
        hash = string_hash( c_str(), size() );
      return hash;
    }

    Stream::Status serialize( OutputStream& output_stream, size_t* out_bytes_written )
    {
      return output_stream.write( c_str(), size(), out_bytes_written );
    }

    // Object
    YIELD_OBJECT_TYPE_INFO( STRING, "String", 3216070200UL );
    inline String& incRef() { return Object::incRef( *this ); }

    // InputStream
    Stream::Status read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
    {
      size_t readable_len = size() - read_pos;
      if ( readable_len > 0 )
      {
        if ( buffer_len > readable_len )
          buffer_len = readable_len;
        memcpy( buffer, c_str()+read_pos, static_cast<size_t>( buffer_len ) );
        read_pos += buffer_len;
        if ( out_bytes_read )
          *out_bytes_read = buffer_len;
        return STREAM_STATUS_OK;
      }
      else
        return STREAM_STATUS_ERROR;
    }

    // OutputStream
    Stream::Status writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
    {
      size_t total_buffers_len = 0;
      for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
      {
        append( static_cast<char*>( buffers[buffer_i].iov_base ), buffers[buffer_i].iov_len );
        total_buffers_len += buffers[buffer_i].iov_len;
      }

      if ( out_bytes_written )
        *out_bytes_written = total_buffers_len;

      return STREAM_STATUS_OK;
    }

  protected:
    virtual ~String() { }

  private:
    uint32_t hash;
    size_t read_pos;
  };


  class Thread : public Object
  {
  public:
    static unsigned long createTLSKey();
    static unsigned long getCurrentThreadId();
    static void* getTLS( unsigned long key );
    static void setCurrentThreadName( const char* thread_name ) { setThreadName( getCurrentThreadId(), thread_name ); }
    static void setTLS( unsigned long key, void* value );
    static void sleep( timeout_ns_t timeout_ns );
    static void yield();


    Thread();

    unsigned long get_id() const { return id; }
    inline bool is_running() const { return _is_running; }
    void set_name( const char* name ) { setThreadName( get_id(), name ); }
    bool set_processor_affinity( unsigned short logical_processor_i );
    bool set_processor_affinity( const ProcessorSet& logical_processor_set );
    virtual void start();

    virtual void run() = 0;

  protected:
    virtual ~Thread();

  private:
    unsigned long id;
#if defined(_WIN32)
    void* handle;
#else
    pthread_t handle;
#endif
    bool _is_running;

    static void setThreadName( unsigned long, const char* );
#ifdef _WIN32
    static unsigned long __stdcall thread_stub( void* );
#else
    static void* thread_stub( void* );
#endif
  };


  class TestCase;

  class TestSuite : public std::vector<TestCase*>
  {
  public:
    TestSuite( const char* test_suite_name = NULL )
    { }

    virtual ~TestSuite();

    void addTest( TestCase* test_case, bool own_test_case = true ); // for addTest( new ... )
    void addTest( TestCase& test_case, bool own_test_case = false ); // for addTest( *this )

  private:
    std::vector<bool> own_test_cases;
  };


  class TestCase
  {
  public:
    TestCase( const char* test_case_name ) : __short_description( test_case_name ) { }
    TestCase( const char* test_suite_name, const char* test_case_name ) : __short_description( std::string( test_suite_name ) + "_" + std::string( test_case_name ) ) { }
    TestCase( const char* test_case_name, TestSuite& __test_suite ) : __short_description( test_case_name ) { __test_suite.addTest( *this ); }
    virtual ~TestCase() { }

    virtual void setUp() { }
    virtual void runTest() = 0;
    virtual void tearDown() { }
    virtual const char* shortDescription() { return __short_description.c_str(); }

  protected:
    std::string __short_description;
  };


  class TestRunner
  {
  public:
    int run( TestSuite& test_suite );
  };


  class Volume : public Object
  {
  public:
    const static mode_t DEFAULT_FILE_MODE = S_IREAD|S_IWRITE;
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
      virtual bool operator()( const Path& dirent_name, const Stat& stbuf ) = 0;
    };

    // Convenience methods that don't make any system calls, so subclasses don't have to re-implement them
    virtual auto_Object<File> creat( const Path& path ) { return creat( path, DEFAULT_FILE_MODE ); }
    virtual auto_Object<File> creat( const Path& path, mode_t mode ) { return open( path, O_CREAT|O_WRONLY|O_TRUNC, mode ); }
    virtual bool exists( const YIELD::Path& path );
    virtual bool listdir( const YIELD::Path& path, listdirCallback& callback ) { return listdir( path, Path(), callback ); }
    virtual bool listdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, listdirCallback& callback );
    virtual bool makedirs( const Path& path ) { return mktree( path, DEFAULT_DIRECTORY_MODE ); } // Python function name
    virtual bool makedirs( const Path& path, mode_t mode ) { return mktree( path, mode ); }
    virtual bool mkdir( const YIELD::Path& path ) { return mkdir( path, DEFAULT_DIRECTORY_MODE ); }
    virtual bool mktree( const YIELD::Path& path ) { return mktree( path, DEFAULT_DIRECTORY_MODE ); }
    virtual bool mktree( const YIELD::Path& path, mode_t mode );
    virtual YIELD::auto_Object<YIELD::File> open( const YIELD::Path& path ) { return open( path, O_RDONLY, DEFAULT_FILE_MODE, 0 ); }
    virtual YIELD::auto_Object<YIELD::File> open( const YIELD::Path& path, uint32_t flags ) { return open( path, flags, DEFAULT_FILE_MODE, 0 ); }
    virtual YIELD::auto_Object<YIELD::File> open( const YIELD::Path& path, uint32_t flags, mode_t mode ) { return open( path, flags, mode, 0 ); }
    virtual bool readdir( const Path& path, readdirCallback& callback ) { return readdir( path, Path(), callback ); }
    virtual bool rmtree( const YIELD::Path& path );
    virtual auto_Object<Stat> stat( const Path& path ) { return getattr( path ); }
    virtual bool touch( const YIELD::Path& path ) { return touch( path, DEFAULT_FILE_MODE ); }
    virtual bool touch( const YIELD::Path& path, mode_t mode );

    YIELD_VOLUME_PROTOTYPES;

    // Object
    inline Volume& incRef() { return Object::incRef( *this ); }

  protected:
    virtual ~Volume() { }
  };


  class XDRInputStream : public StructuredInputStream
  {
  public:
    XDRInputStream( InputStream& underlying_input_stream );

    // StructuredInputStream
    bool readBool( const Declaration& decl ) { beforeRead( decl ); return ( _readInt32() == 1 ); }
    double readDouble( const Declaration& decl ) { beforeRead( decl ); double value; underlying_input_stream.read( &value, sizeof( value ) ); return value; }
    float readFloat( const Declaration& decl ) { beforeRead( decl ); float value; underlying_input_stream.read( &value, sizeof( value ) ); return value; }
    int32_t readInt32( const Declaration& decl ) { beforeRead( decl ); return _readInt32(); }
    int64_t readInt64( const Declaration& decl ) { beforeRead( decl ); return _readInt64(); }
    virtual Object* readObject( const Declaration& decl, Object* value, Object::GeneralType = Object::UNKNOWN );
    void readString( const Declaration&, std::string& );

  protected:
    InputStream& underlying_input_stream;

    virtual void beforeRead( const Declaration& ) { }
    int32_t _readInt32();
    int64_t _readInt64();
  };


  class XDROutputStream : public StructuredOutputStream
  {
  public:
    XDROutputStream( OutputStream& underlying_output_stream, bool in_map = false );

    // StructuredOutputstream
    void writeBool( const Declaration& decl, bool value ) { beforeWrite( decl ); _writeInt32( value ? 1 : 0 ); }
    void writeDouble( const Declaration& decl, double value ) { beforeWrite( decl ); underlying_output_stream.write( &value, sizeof( value ) ); }
    void writeFloat( const Declaration& decl, float value ) { beforeWrite( decl ); underlying_output_stream.write( &value, sizeof( value ) ); }
    void writeInt32( const Declaration& decl, int32_t value ) { beforeWrite( decl ); _writeInt32( value ); }
    void writeInt64( const Declaration& decl, int64_t value ) { beforeWrite( decl ); _writeInt64( value ); }
    virtual void writeObject( const Declaration&, Object& value, Object::GeneralType value_general_type = Object::UNKNOWN );
    void writeString( const Declaration& decl, const std::string& value ) { writeString( decl, value.c_str(), value.size() ); }
    void writeString( const Declaration& decl, const char* value ) { writeString( decl, value, std::strlen( value ) ); }
    void writeString( const Declaration& decl, const char* value, size_t value_len );
    void writeUint32( const Declaration& decl, uint32_t value ) { beforeWrite( decl ); _writeInt32( value ); }
    void writeUint64( const Declaration& decl, uint64_t value ) { beforeWrite( decl ); _writeInt64( value ); }

  protected:
    OutputStream& underlying_output_stream;

    virtual void beforeWrite( const Declaration& decl );
    void _writeInt32( int32_t value );
    void _writeInt64( int64_t value );

  private:
    bool in_map;
  };
};

#endif
