// Copyright 2003-2008 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _YIELD_PLATFORM_H_
#define _YIELD_PLATFORM_H_

#include "yidl.h"

#ifdef _WIN32
typedef int ssize_t;
#else
#include <errno.h>
#include <limits.h>
#include <pthread.h>
#include <unistd.h>
#if defined(__MACH__)
#include <mach/semaphore.h>
#else
#include <semaphore.h>
#endif
#ifdef __sun
#include <libcpc.h>
#define YIELD_HAVE_PERFORMANCE_COUNTERS 1
#endif
#ifdef YIELD_HAVE_PAPI
#define YIELD_HAVE_PERFORMANCE_COUNTERS 1
#endif
#ifdef YIELD_HAVE_POSIX_FILE_AIO
#include <aio.h>
#endif
#endif

#include <algorithm>
#include <cstring>
#include <exception>
#include <fcntl.h>
#include <memory>
#include <iostream>
#include <queue>
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
#endif
#endif

#define NS_IN_US 1000ULL
#define NS_IN_MS 1000000ULL
#define NS_IN_S  1000000000ULL
#define MS_IN_S  1000
#define US_IN_S  1000000

#define YIELD_EXCEPTION_WHAT_BUFFER_LENGTH 128

#define YIELD_FILE_PROTOTYPES \
  virtual bool close(); \
  virtual bool datasync(); \
  virtual bool getxattr( const std::string& name, std::string& out_value ); \
  virtual bool getlk( bool exclusive, uint64_t offset, uint64_t length ); \
  virtual bool listxattr( std::vector<std::string>& out_names ); \
  virtual ssize_t read( void* buffer, size_t buffer_len, uint64_t offset ); \
  virtual bool removexattr( const std::string& name ); \
  virtual bool setlk( bool exclusive, uint64_t offset, uint64_t length ); \
  virtual bool setlkw( bool exclusive, uint64_t offset, uint64_t length ); \
  virtual bool setxattr( const std::string& name, const std::string& value, int flags ); \
  virtual yidl::auto_Object<YIELD::Stat> stat(); \
  virtual bool sync(); \
  virtual bool truncate( uint64_t offset ); \
  virtual bool unlk( uint64_t offset, uint64_t length ); \
  virtual ssize_t write( const void* buffer, size_t buffer_len, uint64_t offset ); \

#define YIELD_VOLUME_PROTOTYPES \
    virtual bool access( const YIELD::Path& path, int amode ); \
    virtual bool chmod( const YIELD::Path& path, mode_t mode ); \
    virtual bool chown( const YIELD::Path& path, int32_t uid, int32_t gid ); \
    virtual bool getxattr( const YIELD::Path& path, const std::string& name, std::string& out_value ); \
    virtual bool link( const YIELD::Path& old_path, const YIELD::Path& new_path ); \
    virtual bool listxattr( const YIELD::Path& path, std::vector<std::string>& out_names ); \
    virtual bool mkdir( const YIELD::Path& path, mode_t mode ); \
    virtual YIELD::auto_File open( const YIELD::Path& path, uint32_t flags, mode_t mode, uint32_t attributes ); \
    virtual bool readdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, YIELD::Volume::readdirCallback& callback ); \
    virtual YIELD::auto_Path readlink( const YIELD::Path& path ); \
    virtual bool removexattr( const YIELD::Path& path, const std::string& name ); \
    virtual bool rename( const YIELD::Path& from_path, const YIELD::Path& to_path ); \
    virtual bool rmdir( const YIELD::Path& path ); \
    virtual bool setattr( const YIELD::Path& path, uint32_t file_attributes ); \
    virtual bool setxattr( const YIELD::Path& path, const std::string& name, const std::string& value, int flags ); \
    virtual yidl::auto_Object<YIELD::Stat> stat( const YIELD::Path& path ); \
    virtual bool statvfs( const YIELD::Path& path, struct statvfs& ); \
    virtual bool symlink( const YIELD::Path& old_path, const YIELD::Path& new_path ); \
    virtual bool truncate( const YIELD::Path& path, uint64_t new_size ); \
    virtual bool unlink( const YIELD::Path& path ); \
    virtual bool utimens( const YIELD::Path& path, const YIELD::Time& atime, const YIELD::Time& mtime, const YIELD::Time& ctime ); \
    virtual YIELD::Path volname( const YIELD::Path& path );


#ifdef _WIN32
    struct aiocb
    {
      unsigned long Internal;
      unsigned long InternalHigh;
      union 
      {
#pragma warning( push )
#pragma warning( disable: 4201 )
        struct 
        {
          unsigned int Offset;
          unsigned int OffsetHigh;
        };
#pragma warning( pop )
        void* Pointer;
      };

      void* hEvent;  
    };

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

typedef int mode_t;

struct _OVERLAPPED;
typedef _OVERLAPPED OVERLAPPED;

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
  template <class> class SynchronizedSTLQueue;
  class Path;
  class Stat;


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
    uint64_t as_unix_time_ms() const { return unix_time_ns / NS_IN_MS; }
    uint32_t as_unix_time_s() const { return static_cast<uint32_t>( unix_time_ns / NS_IN_S ); }
    operator uint64_t() const { return unix_time_ns; }
    operator struct timeval() const;
#ifdef _WIN32
    operator FILETIME() const;
#else
    operator struct timespec() const;
#endif
    Time operator+( const Time& other ) const { return Time( unix_time_ns + other.unix_time_ns ); }
    Time& operator+=( const Time& other ) { unix_time_ns += other.unix_time_ns; return *this; }
    Time operator-( const Time& other ) const { return Time( unix_time_ns - other.unix_time_ns ); }
    Time& operator-=( const Time& other ) { unix_time_ns -= other.unix_time_ns; return *this; }
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


  class AIOControlBlock : public yidl::Object
  {
  public:
    AIOControlBlock()
    {
#if defined(_WIN32) || defined(YIELD_HAVE_POSIX_FILE_AIO)
      memset( &aiocb_, 0, sizeof( aiocb_ ) );
      aiocb_.this_ = this;
#endif
    }

#ifdef _WIN32
    static AIOControlBlock* from_OVERLAPPED( OVERLAPPED* overlapped ) { return reinterpret_cast<struct aiocb*>( overlapped )->this_; }
#endif
    virtual void onCompletion( size_t bytes_transferred ) = 0;
    virtual void onError( uint32_t error_code ) = 0;
#if defined(_WIN32)
    operator OVERLAPPED*() { return reinterpret_cast<OVERLAPPED*>( &aiocb_ ); }
#elif defined(YIELD_HAVE_POSIX_FILE_AIO)
    operator ::aiocb*() { return &aiocb_; }
#endif

  protected:
    virtual ~AIOControlBlock() { }

  private:
#if defined(_WIN32) || defined(YIELD_HAVE_POSIX_FILE_AIO)
    struct aiocb : ::aiocb
    {
      AIOControlBlock* this_;
    } aiocb_;
#endif
  };
  
  typedef yidl::auto_Object<AIOControlBlock> auto_AIOControlBlock;


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


  class File : public yidl::Object
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

    virtual size_t getpagesize();
    virtual uint64_t get_size();
#ifdef _WIN32
    operator void*() const { return fd; }
#else
    operator int() const { return fd; }
#endif    
    virtual ssize_t read( yidl::auto_Buffer buffer ); // Reads from the current file pointer
    virtual ssize_t read( void* buffer, size_t buffer_len ); // Reads from the current file pointer
    virtual bool seek( uint64_t offset ); // Seeks from the beginning of the file
    virtual bool seek( uint64_t offset, unsigned char whence );
    virtual ssize_t write( yidl::auto_Buffer buffer );
    virtual ssize_t write( const void* buffer, size_t buffer_len ); // Writes from the current position
    YIELD_FILE_PROTOTYPES;

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( File, 1 );

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

  typedef yidl::auto_Object<File> auto_File;

  
  class Log : public yidl::Object
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

      Stream( yidl::auto_Object<Log> log, Level level );

      yidl::auto_Object<Log> log;
      Level level;

      std::ostringstream oss;
    };


    static yidl::auto_Object<Log> open( std::ostream&, Level level );
    static yidl::auto_Object<Log> open( const Path& file_path, Level level );

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

    void write( const unsigned char* str, size_t str_len, Level level );

    inline void write( const char* str, size_t str_len, Level level )
    {
      if ( level <= this->level )
        write( str, str_len );
    }
    
    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( Log, 2 );

  protected:
    Log( Level level )
      : level( level )
    { }

    virtual ~Log() { }

    virtual void write( const char* str, size_t str_len ) = 0;

  private:
    Level level;
  };

  typedef yidl::auto_Object<Log> auto_Log;


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


  class MemoryMappedFile : public yidl::Object
  {
  public:
    static yidl::auto_Object<MemoryMappedFile> open( const Path& path ) { return open( path, File::DEFAULT_FLAGS, File::DEFAULT_MODE, File::DEFAULT_ATTRIBUTES, 0 ); }
    static yidl::auto_Object<MemoryMappedFile> open( const Path& path, uint32_t flags ) { return open( path, flags, File::DEFAULT_MODE, File::DEFAULT_ATTRIBUTES, 0 ); }
    static yidl::auto_Object<MemoryMappedFile> open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes, size_t minimum_size );

    virtual bool close();
    inline size_t get_size() const { return size; }
    inline operator char*() const { return start; }
    inline operator void*() const { return start; }
    bool resize( size_t );
    virtual bool sync();
    virtual bool sync( size_t offset, size_t length );
    virtual bool sync( void* ptr, size_t length );

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( MemoryMappedFile, 3 );

  protected:
    MemoryMappedFile( yidl::auto_Object<File> underlying_file, uint32_t open_flags );
    virtual ~MemoryMappedFile() { close(); }

  private:
    yidl::auto_Object<File> underlying_file;
    uint32_t open_flags;

#ifdef _WIN32
    void* mapping;
#endif
    char* start;
    size_t size;
  };

  typedef yidl::auto_Object<MemoryMappedFile> auto_MemoryMappedFile;


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


  class NOPLock
  {
  public:
    inline bool acquire() { return true; }
    inline bool try_acquire() { return true; }
    inline bool timed_acquire( uint64_t ) { return true; }
    inline void release() { }
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

    ElementType dequeue()
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

        if ( 
             atomic_cas( 
                         reinterpret_cast<volatile uint_ptr*>( &elements[try_pos] ), 
                         ( reinterpret_cast<uint_ptr>( try_element ) & PTR_HIGH_BIT ) ? 1 : 0, 
                         reinterpret_cast<uint_ptr>( try_element )
                       ) 
             == reinterpret_cast<uint_ptr>( try_element ) 
           )
        {
          if ( try_pos % 2 == 0 )
            atomic_cas( &head, try_pos, copied_head );

          return reinterpret_cast<ElementType>( ( reinterpret_cast<uint_ptr>( try_element ) & PTR_LOW_BITS ) << 1 );
        }
      }
    }

    bool enqueue( ElementType element )
    {
#ifdef _DEBUG
      if ( reinterpret_cast<uint_ptr>( element ) & 0x1 ) DebugBreak();
#endif

      element = reinterpret_cast<ElementType>( reinterpret_cast<uint_ptr>( element ) >> 1 );

#ifdef _DEBUG
      if ( reinterpret_cast<uint_ptr>( element ) & PTR_HIGH_BIT ) DebugBreak();
#endif

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
            return false; // Queue is full

          atomic_cas( &head, last_try_pos, try_pos );

          continue;
        }

        if ( copied_tail != tail )
          continue;

        // diff next line
        if ( 
             atomic_cas( 
                         reinterpret_cast<volatile uint_ptr*>( &elements[last_try_pos] ), 
                         try_element == reinterpret_cast<ElementType>( 1 ) ? ( reinterpret_cast<uint_ptr>( element ) | PTR_HIGH_BIT ) : reinterpret_cast<uint_ptr>( element ),
                         reinterpret_cast<uint_ptr>( try_element )
                       ) 
             == reinterpret_cast<uint_ptr>( try_element ) 
           )
        {
          if ( try_pos % 2 == 0 )
            atomic_cas( &tail, try_pos, copied_tail );

          return true;
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


  class Path : public yidl::Object
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

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( Path, 5 );

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

  typedef yidl::auto_Object<Path> auto_Path;


#ifdef YIELD_HAVE_PERFORMANCE_COUNTERS
  class PerformanceCounterSet : public yidl::Object
  {
  public:
    enum Event 
    {
      EVENT_L1_DCM, // L1 data cache miss
      EVENT_L2_DCM, // L2 data cache miss
      EVENT_L2_ICM // L2 instruction cache miss
    };

    static yidl::auto_Object<PerformanceCounterSet> create();
    
    bool addEvent( Event event );
    bool addEvent( const char* name );
    void startCounting();
    void stopCounting( uint64_t* counts );

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( PerformanceCounterSet, 0 );

  private:    
#if defined(__sun)
    PerformanceCounterSet( cpc_t* cpc, cpc_set_t* cpc_set );
    cpc_t* cpc; cpc_set_t* cpc_set;

    std::vector<int> event_indices;
    cpc_buf_t* start_cpc_buf;
#elif defined(YIELD_HAVE_PAPI)
    PerformanceCounterSet( int papi_eventset );
    int papi_eventset;
#endif

    ~PerformanceCounterSet();
  };

  typedef yidl::auto_Object<PerformanceCounterSet> auto_PerformanceCounterSet;
#endif


  class ProcessorSet : public yidl::Object
  {
  public:
    ProcessorSet();
    ProcessorSet( uint32_t from_mask );

    void clear();
    void clear( uint16_t processor_i );
    uint16_t count() const;
    bool empty() const;
    bool isset( uint16_t processor_i ) const;
    bool set( uint16_t processor_i );    

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( ProcessorSet, 8 );

  private:
    ProcessorSet( const ProcessorSet& ) { DebugBreak(); } // Prevent copying
    ~ProcessorSet();

    friend class Process;
    friend class Thread;

#if defined(_WIN32)
    unsigned long mask;
#elif defined(__linux__)
    void* cpu_set;
#elif defined(__sun)
    int psetid;
#endif
  };

  typedef yidl::auto_Object<ProcessorSet> auto_ProcessorSet;


  class RRD : public yidl::Object
  {
  public: 
    class Record : public yidl::Object
    {
    public:
      Record( double value );
      Record( const Time& time, double value );

      const Time& get_time() const { return time; }
      double get_value() const { return value; }
      operator double() const { return value; }

      // yidl::Object
      YIDL_OBJECT_PROTOTYPES( RRD::Record, 0 );
      void marshal( yidl::Marshaller& marshaller );
      void unmarshal( yidl::Unmarshaller& unmarshaller );

    private:
      Time time;
      double value;
    };

    
    class RecordSet : public std::vector<Record*>
    {
    public:
      ~RecordSet();
    };


    static yidl::auto_Object<RRD> creat( const Path& file_path );
    static yidl::auto_Object<RRD> open( const Path& file_path );

    void append( double value );
    void fetch_all( RecordSet& out_records );
    void fetch_from( const Time& start_time, RecordSet& out_records );
    void fetch_range( const Time& start_time, const Time& end_time, RecordSet& out_records );
    void fetch_until( const Time& end_time, RecordSet& out_records );

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( RRD, 0 );

  private:
    RRD( const Path& file_path );
    ~RRD();

    Path current_file_path;
  };

  typedef yidl::auto_Object<RRD> auto_RRD;


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

    void clear()
    {
      lock.acquire();
      samples_count = 0;
      lock.release();
    }

    SampleType get_max() const 
    { 
      return max; 
    }

    SampleType get_mean()
    {
      lock.acquire();
      SampleType mean;

      if ( samples_count > 0 )
        mean = static_cast<SampleType>( static_cast<double>( total ) / static_cast<double>( samples_count ) );
      else
        mean = 0;

      lock.release();
      return mean;
    }
 
    SampleType get_median()
    {
      lock.acquire();
      SampleType median;

      if ( samples_count > 0 )
      {
        std::sort( samples, samples + samples_count );
        size_t sc_div_2 = samples_count / 2;
        if ( samples_count % 2 == 1 )
          median = samples[sc_div_2];
        else
        {
          SampleType median_temp = samples[sc_div_2] + samples[sc_div_2-1];
          if ( median_temp > 0 )
            median = static_cast<SampleType>( static_cast<double>( median_temp ) / 2.0 );
          else
            median = 0;
        }
      }
      else
        median = 0;

      lock.release();
      return median;
    }

    SampleType get_min() const 
    { 
      return min; 
    }

    SampleType get_percentile( double percentile )
    {
      lock.acquire();
      SampleType ninetieth_percentile;

      if ( samples_count > 0 )
      {
        std::sort( samples, samples + samples_count );
        ninetieth_percentile = samples[static_cast<size_t>( percentile * static_cast<double>( samples_count ) )];
      }
      else
        ninetieth_percentile = 0;

      lock.release();
      return ninetieth_percentile;
    }

    uint32_t get_samples_count() const
    { 
      return samples_count; 
    }

    void setNextSample( SampleType sample )
    {
      if ( lock.try_acquire() )
      {
        samples[samples_pos] = sample;
        samples_pos = ( samples_pos + 1 ) % ArraySize;
        if ( samples_count < ArraySize ) samples_count++;

        if ( sample < min )
          min = sample;
        if ( sample > max )
          max = sample;
        total += sample;

        lock.release();
      }
    }

  protected:
    SampleType samples[ArraySize+1], min, max; SampleType total;
    uint32_t samples_pos, samples_count;
    LockType lock;
  };


  template <class ElementType, uint32_t QueueLength>
  class SynchronizedNonBlockingFiniteQueue : private NonBlockingFiniteQueue<ElementType, QueueLength>
  {
  public:
    ElementType dequeue()
    {
      ElementType element = NonBlockingFiniteQueue<ElementType, QueueLength>::dequeue();

      while ( element == 0 )
      {
        signal.acquire();
        element = NonBlockingFiniteQueue<ElementType, QueueLength>::dequeue();
      }

      return element;
    }

    bool enqueue( ElementType element )
    {
      bool enqueued = NonBlockingFiniteQueue<ElementType, QueueLength>::enqueue( element );
      signal.release();
      return enqueued;
    }

    ElementType timed_dequeue( uint64_t timeout_ns )
    {
      ElementType element = NonBlockingFiniteQueue<ElementType, QueueLength>::dequeue();

      if ( element != 0 )
        return element;
      else
      {
        signal.timed_acquire( timeout_ns );
        return NonBlockingFiniteQueue<ElementType, QueueLength>::dequeue();
      }
    }

    ElementType try_dequeue()
    {
      return NonBlockingFiniteQueue<ElementType, QueueLength>::dequeue();
    }

  private:
    CountingSemaphore signal;
  };


  template <class ElementType>
  class SynchronizedSTLQueue : private std::queue<ElementType>
  {
  public:
    ElementType dequeue()
    {
      for ( ;; )
      {
        signal.acquire();
        lock.acquire();
        if ( std::queue<ElementType>::size() > 0 )
        {
          ElementType element = std::queue<ElementType>::front();
          std::queue<ElementType>::pop();
          lock.release();
          return element;
        }
        else
          lock.release();
      }
    }

    bool enqueue( ElementType element )
    {
      lock.acquire();
      std::queue<ElementType>::push( element );
      lock.release();
      signal.release();
      return true;
    }

    ElementType timed_dequeue( uint64_t timeout_ns )
    {
      for ( ;; )
      {
        uint64_t start_time_ns = Time::getCurrentUnixTimeNS();

        if ( signal.timed_acquire( timeout_ns ) )
        {
          if ( lock.try_acquire() )
          {
            if ( std::queue<ElementType>::size() > 0 )
            {
              ElementType element = std::queue<ElementType>::front();
              std::queue<ElementType>::pop();
              lock.release();
              return element;
            }
            else
              lock.release();
          }
        }

        uint64_t elapsed_time_ns = Time::getCurrentUnixTimeNS() - start_time_ns;
        if ( elapsed_time_ns < timeout_ns )
          timeout_ns -= elapsed_time_ns;
        else
          return NULL;
      }
    }

    ElementType try_dequeue()
    {
      if ( lock.try_acquire() )
      {
        if ( std::queue<ElementType>::size() > 0 )
        {
          ElementType element = std::queue<ElementType>::front();
          std::queue<ElementType>::pop();
          lock.release();
          return element;
        }
        else
          lock.release();
      }

      return NULL;
    }

  private:
    Mutex lock;
    CountingSemaphore signal;
  };


  class SharedLibrary : public yidl::Object
  {
  public:
    static yidl::auto_Object<SharedLibrary> open( const Path& file_prefix, const char* argv0 = 0 );

    void* getFunction( const char* function_name, void* missing_function_return_value = NULL );

    template <typename FunctionType>
    FunctionType getFunction( const char* function_name, FunctionType missing_function_return_value = NULL )
    {
      return ( FunctionType )getFunction( function_name, ( void* )missing_function_return_value );
    }    

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( SharedLibrary, 11 );

  private:
    SharedLibrary( void* handle );
    ~SharedLibrary();

    void* handle;
  };

  typedef yidl::auto_Object<SharedLibrary> auto_SharedLibrary;


  class Stat : public yidl::Object
  {
  public:
#ifdef _WIN32
    Stat( mode_t mode, uint64_t size, const Time& atime, const Time& mtime, const Time& ctime, uint32_t attributes );
    Stat( const BY_HANDLE_FILE_INFORMATION& );
    Stat( const WIN32_FIND_DATA& );
    Stat( uint32_t nFileSizeHigh, uint32_t nFileSizeLow, const FILETIME* ftLastWriteTime, const FILETIME* ftCreationTime, const FILETIME* ftLastAccessTime, uint32_t dwFileAttributes ); // For doing FILETIME -> Unix conversions in Dokan; deduces mode from dwFileAttributes
#else
    Stat( dev_t dev, ino_t ino, mode_t mode, nlink_t nlink, uid_t uid, gid_t gid, uint64_t size, const Time& atime, const Time& mtime, const Time& ctime );
#endif
    Stat( const struct stat& stbuf );

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
    virtual operator BY_HANDLE_FILE_INFORMATION() const;
    virtual operator WIN32_FIND_DATA() const;
#endif

    void set_size( uint64_t size ) { this->size = size; }

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( Stat, 12 );

  protected:
    virtual ~Stat() { }

#ifndef _WIN32
    dev_t dev;
    ino_t ino;
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

  private:
    Stat( const Stat& ) { DebugBreak(); } // Prevent copying
  };

  typedef yidl::auto_Object<Stat> auto_Stat;


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


  class Thread : public yidl::Object
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
    virtual ~Thread();

    unsigned long get_id() const { return id; }
    void set_name( const char* name ) { setThreadName( get_id(), name ); }
    bool set_processor_affinity( unsigned short logical_processor_i );
    bool set_processor_affinity( const ProcessorSet& logical_processor_set );
    virtual void start();

    virtual void run() = 0;

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( Thread, 14 );

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


  class TimerQueue : public yidl::Object
  {
  public:
    class Timer;

    TimerQueue();
    ~TimerQueue();

    void addTimer( yidl::auto_Object<Timer> timer );
    static void destroyDefaultTimerQueue();
    static TimerQueue& getDefaultTimerQueue();

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( TimerQueue, 0 );

  private:
#ifdef _WIN32
    TimerQueue( void* hTimerQueue );

    void* hTimerQueue;

    static void __stdcall WaitOrTimerCallback( void*, unsigned char );
#else
    class Thread : public ::YIELD::Thread
    {
    public:
      Thread();

      void addTimer( Timer* timer ) { new_timers_queue.enqueue( timer ); }
      void stop();

      // Thread
      void run();

    private:
      SynchronizedSTLQueue<TimerQueue::Timer*> new_timers_queue;
      bool should_run;
      std::priority_queue< std::pair<uint64_t, Timer*>, std::vector< std::pair<uint64_t, Timer*> >, std::greater< std::pair<uint64_t, Timer*> > > timers;
    }; 

    Thread thread;
#endif

    static TimerQueue* default_timer_queue;

  public:
    class Timer : public yidl::Object
    {
    public:
      Timer( const Time& timeout );
      Timer( const Time& timeout, const Time& period );
      virtual ~Timer();
      
      const Time& get_period() const { return period; }
      const Time& get_timeout() const { return timeout; }
      virtual bool fire( const Time& elapsed_time ) = 0;
      void set_period( const Time& period ) { this->period = period; }
      void set_timeout( const Time& timeout ) { this->timeout = timeout; }

      // yidl::Object
      YIDL_OBJECT_PROTOTYPES( Timer, 0 );

    private:
      friend class TimerQueue;
#ifndef _WIN32
      friend class TimerQueue::Thread;
#endif

      Time period, timeout;

#ifdef _WIN32
      void *hTimer, *hTimerQueue;
#endif
      Time last_fire_time;
    };
  };

  typedef yidl::auto_Object<TimerQueue> auto_TimerQueue;


  class Volume : public yidl::Object
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
      virtual bool operator()( const Path& dirent_name, auto_Stat stbuf ) = 0;
    };


    virtual ~Volume() { }

    YIELD_VOLUME_PROTOTYPES;

    // Convenience methods that don't make any system calls, so subclasses don't have to re-implement them
    virtual auto_File creat( const Path& path ) { return creat( path, File::DEFAULT_MODE ); }
    virtual auto_File creat( const Path& path, mode_t mode ) { return open( path, O_CREAT|O_WRONLY|O_TRUNC, mode ); }
    virtual bool exists( const Path& path );
    virtual bool isdir( const Path& path );
    virtual bool isfile( const Path& path );
    virtual bool listdir( const Path& path, listdirCallback& callback ) { return listdir( path, Path(), callback ); }
    virtual bool listdir( const Path& path, const Path& match_file_name_prefix, listdirCallback& callback );
    virtual bool listdir( const Path& path, std::vector<Path>& out_names ) { return listdir( path, Path(), out_names ); }
    virtual bool listdir( const Path& path, const Path& match_file_name_prefix, std::vector<Path>& out_names );
    virtual bool makedirs( const Path& path ) { return mktree( path, DEFAULT_DIRECTORY_MODE ); } // Python function name
    virtual bool makedirs( const Path& path, mode_t mode ) { return mktree( path, mode ); }
    virtual bool mkdir( const Path& path ) { return mkdir( path, DEFAULT_DIRECTORY_MODE ); }
    virtual bool mktree( const Path& path ) { return mktree( path, DEFAULT_DIRECTORY_MODE ); }
    virtual bool mktree( const Path& path, mode_t mode );
    virtual auto_File open( const Path& path ) { return open( path, O_RDONLY, File::DEFAULT_MODE, 0 ); }
    virtual auto_File open( const Path& path, uint32_t flags ) { return open( path, flags, File::DEFAULT_MODE, 0 ); }
    virtual auto_File open( const Path& path, uint32_t flags, mode_t mode ) { return open( path, flags, mode, 0 ); }
    virtual bool readdir( const Path& path, readdirCallback& callback ) { return readdir( path, Path(), callback ); }
    virtual bool rmtree( const Path& path );
    virtual bool touch( const Path& path ) { return touch( path, File::DEFAULT_MODE ); }
    virtual bool touch( const Path& path, mode_t mode );

    // yidl::Object
    YIDL_OBJECT_PROTOTYPES( Volume, 15 );
  };

  typedef yidl::auto_Object<Volume> auto_Volume;


  class XDRMarshaller : public yidl::Marshaller
  {
  public:
    XDRMarshaller();

    yidl::auto_StringBuffer get_buffer() const { return buffer; }

    // Marshaller
    YIDL_MARSHALLER_PROTOTYPES;
    void writeFloat( const char* key, uint32_t tag, float value );
    void writeInt32( const char* key, uint32_t tag, int32_t value );

  protected:
    virtual void writeKey( const char* key );

  private:
    yidl::auto_StringBuffer buffer;
    std::vector<bool> in_map_stack;
  };


  class XDRUnmarshaller : public yidl::Unmarshaller
  {
  public:
    XDRUnmarshaller( yidl::auto_Buffer buffer );

    // Unmarshaller
    YIDL_UNMARSHALLER_PROTOTYPES;
    float readFloat( const char* key, uint32_t tag );
    int32_t readInt32( const char* key, uint32_t tag );

  private:
    yidl::auto_Buffer buffer;

    void read( void*, size_t );
  };
};

#endif
