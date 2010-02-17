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
#endif


#define YIELD_PLATFORM_FILE_PROTOTYPES \
  virtual bool close(); \
  virtual bool datasync(); \
  virtual yidl::runtime::auto_Object<YIELD::platform::Stat> getattr(); \
  virtual bool getlk( bool exclusive, uint64_t offset, uint64_t length ); \
  virtual bool getxattr( const std::string& name, std::string& out_value ); \
  virtual bool listxattr( std::vector<std::string>& out_names ); \
  virtual ssize_t read( void* buffer, size_t buffer_len, uint64_t offset ); \
  virtual bool removexattr( const std::string& name ); \
  virtual bool setlk( bool exclusive, uint64_t offset, uint64_t length ); \
  virtual bool setlkw( bool exclusive, uint64_t offset, uint64_t length ); \
  virtual bool \
  setxattr \
  ( \
    const std::string& name, \
    const std::string& value, \
    int flags \
  ); \
  virtual bool sync(); \
  virtual bool truncate( uint64_t offset ); \
  virtual bool unlk( uint64_t offset, uint64_t length ); \
  virtual ssize_t \
  write \
  ( \
    const void* buffer, \
    size_t buffer_len, \
    uint64_t offset \
  );

#define YIELD_PLATFORM_VOLUME_PROTOTYPES \
    virtual bool access( const YIELD::platform::Path& path, int amode ); \
    virtual yidl::runtime::auto_Object<YIELD::platform::Stat> \
    getattr \
    ( \
      const YIELD::platform::Path& path \
    ); \
    virtual bool \
    getxattr \
    ( \
      const YIELD::platform::Path& path, \
      const std::string& name, \
      std::string& out_value \
    ); \
    virtual bool \
    link \
    ( \
      const YIELD::platform::Path& old_path, \
      const YIELD::platform::Path& new_path \
    ); \
    virtual bool \
    listxattr \
    ( \
      const YIELD::platform::Path& path, \
      std::vector<std::string>& out_names \
    ); \
    virtual bool mkdir( const YIELD::platform::Path& path, mode_t mode ); \
    virtual YIELD::platform::auto_File \
    open \
    ( \
      const YIELD::platform::Path& path, \
      uint32_t flags, \
      mode_t mode, \
      uint32_t attributes \
    ); \
    virtual bool \
    readdir \
    ( \
      const YIELD::platform::Path& path, \
      const YIELD::platform::Path& match_file_name_prefix, \
      YIELD::platform::Volume::readdirCallback& callback \
    ); \
    virtual YIELD::platform::auto_Path \
    readlink \
    ( \
      const YIELD::platform::Path& path \
    ); \
    virtual bool \
    removexattr \
    ( \
      const YIELD::platform::Path& path, \
      const std::string& name \
    ); \
    virtual bool \
    rename \
    ( \
      const YIELD::platform::Path& from_path, \
      const YIELD::platform::Path& to_path \
    ); \
    virtual bool rmdir( const YIELD::platform::Path& path ); \
    virtual bool \
    setattr \
    ( \
      const YIELD::platform::Path& path, \
      yidl::runtime::auto_Object<YIELD::platform::Stat> stbuf, \
      uint32_t to_set \
    ); \
    virtual bool \
    setxattr \
    ( \
      const YIELD::platform::Path& path, \
      const std::string& name, \
      const std::string& value, \
      int flags \
    ); \
    virtual bool \
    statvfs \
    ( \
      const YIELD::platform::Path& path, \
      struct statvfs& \
    ); \
    virtual bool \
    symlink \
    ( \
      const YIELD::platform::Path& old_path, \
      const YIELD::platform::Path& new_path \
    ); \
    virtual bool \
    truncate \
    ( \
      const YIELD::platform::Path& path, \
      uint64_t new_size \
    ); \
    virtual bool unlink( const YIELD::platform::Path& path ); \
    virtual YIELD::platform::Path volname( const YIELD::platform::Path& path );


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

typedef unsigned short mode_t;
typedef short nlink_t;

struct _OVERLAPPED;
typedef _OVERLAPPED OVERLAPPED;

// POSIX statvfs
struct statvfs
{
  unsigned long f_bsize;    // File system block size.
  unsigned long f_frsize;   // Fundamental file system block size.
  fsblkcnt_t    f_blocks;   // Total number of blocks on file system
                            // in units of f_frsize.
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
  namespace platform
  {
    class Path;
    class Stat;


    class Exception : public std::exception
    {
    public:
      // error_message is always copied
      Exception();
      Exception( uint32_t error_code ); // Use a system error message
      Exception( const char* error_message );
      Exception( const std::string& error_message );
      Exception( uint32_t error_code, const char* error_message );
      Exception( uint32_t error_code, const std::string& error_message );
      Exception( const Exception& other );
      virtual ~Exception() throw();

      virtual uint32_t get_error_code() const { return error_code; }
      virtual const char* get_error_message() throw();

      operator const char*() throw() { return get_error_message(); }

      // std::exception
      const char* what() const throw()
      {
        return const_cast<Exception*>( this )->get_error_message();
      }

    protected:
      void set_error_code( uint32_t error_code );
      void set_error_message( const char* error_message );

    private:
      uint32_t error_code;
      char* error_message;
    };


    class Time
    {
    public:
      const static uint64_t NS_IN_US = 1000ULL;
      const static uint64_t NS_IN_MS = 1000000ULL;
      const static uint64_t NS_IN_S = 1000000000ULL;


      Time(); // Current time

      Time( uint64_t unix_time_ns )
        : unix_time_ns( unix_time_ns )
      { }

      Time( double unix_time_s )
        : unix_time_ns
          (
            static_cast<uint64_t>
            (
              unix_time_s * static_cast<double>( NS_IN_S )
            )
          )
      { }

      Time( const struct timeval& );
#ifdef _WIN32
      Time( const FILETIME& );
      Time( const FILETIME* );
#else
      Time( const struct timespec& );
#endif

      Time( const Time& other )
        : unix_time_ns( other.unix_time_ns )
       { }

      void as_common_log_date_time( char* out_str, uint8_t out_str_len ) const;
      void as_http_date_time( char* out_str, uint8_t out_str_len ) const;
      void as_iso_date( char* out_str, uint8_t out_str_len ) const;
      void as_iso_date_time( char* out_str, uint8_t out_str_len ) const;

      inline double as_unix_time_ms() const
      {
        return static_cast<double>( unix_time_ns )
               / static_cast<double>( NS_IN_MS );
      }

      inline uint64_t as_unix_time_ns() const
      {
        return unix_time_ns;
      }

      inline double as_unix_time_s() const
      {
        return static_cast<double>( unix_time_ns )
               / static_cast<double>( NS_IN_S );
      }

      inline double as_unix_time_us() const
      {
        return static_cast<double>( unix_time_ns )
               / static_cast<double>( NS_IN_US );
      }

      inline operator uint64_t() const
      {
        return as_unix_time_ns();
      }

      inline operator double() const
      {
        return as_unix_time_s();
      }

      operator struct timeval() const;
#ifdef _WIN32
      operator FILETIME() const;
#else
      operator struct timespec() const;
#endif
      operator std::string() const;

      inline Time& operator=( const Time& other )
      {
        unix_time_ns = other.unix_time_ns;
        return *this;
      }

      inline Time& operator=( uint64_t unix_time_ns )
      {
        this->unix_time_ns = unix_time_ns;
        return *this;
      }

      inline Time& operator=( double unix_time_s )
      {
        this->unix_time_ns =
          static_cast<uint64_t>
          (
            unix_time_s * static_cast<double>( NS_IN_S )
          );
        return *this;
      }

      inline Time operator+( const Time& other ) const
      {
        return Time( unix_time_ns + other.unix_time_ns );
      }

      inline Time operator+( uint64_t unix_time_ns ) const
      {
        return Time( this->unix_time_ns + unix_time_ns );
      }

      inline Time operator+( double unix_time_s ) const
      {
        return Time( as_unix_time_s() + unix_time_s );
      }

      inline Time& operator+=( const Time& other )
      {
        unix_time_ns += other.unix_time_ns;
        return *this;
      }

      inline Time& operator+=( uint64_t unix_time_ns )
      {
        this->unix_time_ns += unix_time_ns;
        return *this;
      }

      inline Time& operator+=( double unix_time_s )
      {
        this->unix_time_ns +=
          static_cast<uint64_t>
          (
            unix_time_s * static_cast<double>( NS_IN_S )
          );

        return *this;
      }

      inline Time operator-( const Time& other ) const
      {
        if ( unix_time_ns >= other.unix_time_ns )
          return Time( unix_time_ns - other.unix_time_ns );
        else
          return Time( static_cast<uint64_t>( 0 ) );
      }

      inline Time operator-( uint64_t unix_time_ns ) const
      {
        if ( this->unix_time_ns >= unix_time_ns )
          return Time( this->unix_time_ns - unix_time_ns );
        else
          return Time( static_cast<uint64_t>( 0 ) );
      }

      inline Time operator-( double unix_time_s ) const
      {
        double this_unix_time_s = as_unix_time_s();
        if ( this_unix_time_s >= unix_time_s )
          return Time( this_unix_time_s - unix_time_s );
        else
          return Time( static_cast<uint64_t>( 0 ) );
      }

      inline Time& operator-=( const Time& other )
      {
        if ( unix_time_ns >= other.unix_time_ns )
          unix_time_ns -= other.unix_time_ns;
        else
          unix_time_ns = 0;

        return *this;
      }

      inline Time& operator-=( uint64_t unix_time_ns )
      {
        if ( this->unix_time_ns >= unix_time_ns )
          this->unix_time_ns -= unix_time_ns;
        else
          this->unix_time_ns = 0;

        return *this;
      }

      inline Time& operator-=( double unix_time_s )
      {
        double this_unix_time_s = as_unix_time_s();
        if ( this_unix_time_s >= unix_time_s )
          this->unix_time_ns -= static_cast<uint64_t>( unix_time_s * NS_IN_S );
        else
          this->unix_time_ns = 0;

        return *this;
      }

      inline Time& operator*=( const Time& other )
      {
        unix_time_ns *= other.unix_time_ns;
        return *this;
      }

      inline bool operator==( const Time& other ) const
      {
        return unix_time_ns == other.unix_time_ns;
      }

      inline bool operator==( uint64_t unix_time_ns ) const
      {
        return this->unix_time_ns == unix_time_ns;
      }

      inline bool operator==( double unix_time_s ) const
      {
        return as_unix_time_s() == unix_time_s;
      }

      inline bool operator!=( const Time& other ) const
      {
        return unix_time_ns != other.unix_time_ns;
      }

      inline bool operator!=( uint64_t unix_time_ns ) const
      {
        return this->unix_time_ns != unix_time_ns;
      }

      inline bool operator!=( double unix_time_s ) const
      {
        return as_unix_time_s() != unix_time_s;
      }

      inline bool operator<( const Time& other ) const
      {
        return unix_time_ns < other.unix_time_ns;
      }

      inline bool operator<( uint64_t unix_time_ns ) const
      {
        return this->unix_time_ns < unix_time_ns;
      }

      inline bool operator<( double unix_time_s ) const
      {
        return as_unix_time_s() < unix_time_s;
      }

      inline bool operator<=( const Time& other ) const
      {
        return unix_time_ns <= other.unix_time_ns;
      }

      inline bool operator<=( uint64_t unix_time_ns ) const
      {
        return this->unix_time_ns <= unix_time_ns;
      }

      inline bool operator<=( double unix_time_s ) const
      {
        return as_unix_time_s() <= unix_time_s;
      }

      inline bool operator>( const Time& other ) const
      {
        return unix_time_ns > other.unix_time_ns;
      }

      inline bool operator>( uint64_t unix_time_ns ) const
      {
        return this->unix_time_ns > unix_time_ns;
      }

      inline bool operator>( double unix_time_s ) const
      {
        return as_unix_time_s() > unix_time_s;
      }

      inline bool operator>=( const Time& other ) const
      {
        return unix_time_ns >= other.unix_time_ns;
      }

      inline bool operator>=( uint64_t unix_time_ns ) const
      {
        return this->unix_time_ns >= unix_time_ns;
      }

      inline bool operator>=( double unix_time_s ) const
      {
        return as_unix_time_s() >= unix_time_s;
      }

    private:
      // The time is stored internally as a Unix epoch time, i.e.
      // nanoseconds since January 1, 1970
      uint64_t unix_time_ns;
    };

    static inline std::ostream& operator<<( std::ostream& os, const Time& time )
    {
      char iso_date_time[64];
      time.as_iso_date_time( iso_date_time, 64 );
      os << iso_date_time;
      return os;
    }


    class AIOControlBlock : public yidl::runtime::Object
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
      static AIOControlBlock* from_OVERLAPPED( OVERLAPPED* overlapped )
      {
        return reinterpret_cast<struct aiocb*>( overlapped )->this_;
      }
#endif
      virtual void onCompletion( size_t bytes_transferred ) = 0;
      virtual void onError( uint32_t error_code ) = 0;
#if defined(_WIN32)
      operator OVERLAPPED*()
      {
        return reinterpret_cast<OVERLAPPED*>( &aiocb_ );
      }
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

    typedef yidl::runtime::auto_Object<AIOControlBlock> auto_AIOControlBlock;


    class CountingSemaphore
    {
    public:
      CountingSemaphore();
      ~CountingSemaphore();

      bool acquire(); // Blocking
      bool timed_acquire( const Time& timeout ); // May block for timeout
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


    class File : public yidl::runtime::Object
    {
    public:
      const static uint32_t ATTRIBUTES_DEFAULT = 0;
      const static uint32_t FLAGS_DEFAULT = O_RDONLY;
      const static mode_t MODE_DEFAULT = S_IREAD|S_IWRITE;


      // Construct from a platform file descriptor;
      // takes ownership of the descriptor
#ifdef _WIN32
      File( void* fd );
#else
      File( int fd );
#endif

      YIELD_PLATFORM_FILE_PROTOTYPES;

      virtual size_t getpagesize();

#ifdef _WIN32
      operator void*() const { return fd; }
#else
      operator int() const { return fd; }
#endif

      // Reads from the current file pointer
      virtual ssize_t read( yidl::runtime::auto_Buffer buffer );

      // Reads from the current file pointer
      virtual ssize_t read( void* buffer, size_t buffer_len );

      // Seeks from the beginning of the file
      virtual bool seek( uint64_t offset );

      virtual bool seek( uint64_t offset, unsigned char whence );

      // Delegates to getattr
      virtual yidl::runtime::auto_Object<Stat> stat();

      // Writes from the current position
      virtual ssize_t write( yidl::runtime::auto_Buffer buffer );

      virtual ssize_t write( const void* buffer, size_t buffer_len );

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( File, 1 );

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

    typedef yidl::runtime::auto_Object<File> auto_File;


    class Log : public yidl::runtime::Object
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

        Stream( yidl::runtime::auto_Object<Log> log, Level );

        yidl::runtime::auto_Object<Log> log;
        Level level;

        std::ostringstream oss;
      };


      static yidl::runtime::auto_Object<Log> open( std::ostream&, Level );

      static yidl::runtime::auto_Object<Log>
      open
      (
        const Path& file_path,
        Level level,
        bool lazy = false
      );

      Level get_level() const { return level; }
      Stream getStream() { return Stream( incRef(), level ); }
      Stream getStream( Level level ) { return Stream( incRef(), level ); }
      void set_level( Level level ) { this->level = level; }

      void write( const char* str, Level level )
      {
        write( str, strnlen( str, UINT16_MAX ), level );
      }

      void write( const std::string& str, Level level )
      {
        write( str.c_str(), str.size(), level );
      }

      void write( const void* str, size_t str_len, Level level )
      {
        return write
        (
          static_cast<const unsigned char*>( str ),
          str_len,
          level
        );
      }

      void write( const unsigned char* str, size_t str_len, Level level );

      void write( const char* str, size_t str_len, Level level )
      {
        if ( level <= this->level )
          write( str, str_len );
      }

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( Log, 2 );

    protected:
      Log( Level level )
        : level( level )
      { }

      virtual ~Log() { }

      virtual void write( const char* str, size_t str_len ) = 0;

    private:
      Level level;
    };

    typedef yidl::runtime::auto_Object<Log> auto_Log;


    class Machine
    {
    public:
      static uint16_t getLogicalProcessorsPerPhysicalProcessor();
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
        return ( x >> 24 ) |
               ( ( x << 8 ) & 0x00FF0000 ) |
               ( ( x >> 8 ) & 0x0000FF00 ) |
               ( x << 24 );
#endif
      }
#endif

      static inline uint64_t htonll( uint64_t x )
      {
#ifdef __BIG_ENDIAN__
        return x;
#else
        return ( x >> 56 ) |
               ( ( x << 40 ) & 0x00FF000000000000ULL ) |
               ( ( x << 24 ) & 0x0000FF0000000000ULL ) |
               ( ( x << 8 )  & 0x000000FF00000000ULL ) |
               ( ( x >> 8)  & 0x00000000FF000000ULL ) |
               ( ( x >> 24) & 0x0000000000FF0000ULL ) |
               ( ( x >> 40 ) & 0x000000000000FF00ULL ) |
               ( x << 56 );
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
        return ( x >> 24 ) |
               ( ( x << 8 ) & 0x00FF0000 ) |
               ( ( x >> 8 ) & 0x0000FF00 ) |
               ( x << 24 );
#endif
      }
#endif

      static inline uint64_t ntohll( uint64_t x )
      {
#ifdef __BIG_ENDIAN__
        return x;
#else
        return ( x >> 56 ) |
               ( ( x << 40 ) & 0x00FF000000000000ULL ) |
               ( ( x << 24 ) & 0x0000FF0000000000ULL ) |
               ( ( x << 8 )  & 0x000000FF00000000ULL ) |
               ( ( x >> 8 )  & 0x00000000FF000000ULL ) |
               ( ( x >> 24) & 0x0000000000FF0000ULL ) |
               ( ( x >> 40 ) & 0x000000000000FF00ULL ) |
               ( x << 56 );
#endif
      }
    };


    class MemoryMappedFile : public yidl::runtime::Object
    {
    public:
      static yidl::runtime::auto_Object<MemoryMappedFile>
      open( const Path& path );

      static yidl::runtime::auto_Object<MemoryMappedFile>
      open( const Path& path, uint32_t flags );

      static yidl::runtime::auto_Object<MemoryMappedFile>
      open
      (
        const Path& path,
        uint32_t flags,
        mode_t mode,
        uint32_t attributes,
        size_t minimum_size
      );

      virtual bool close();
      size_t get_size() const { return size; }
      operator char*() const { return start; }
      operator void*() const { return start; }
      bool resize( size_t );
      virtual bool sync();
      virtual bool sync( size_t offset, size_t length );
      virtual bool sync( void* ptr, size_t length );

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( MemoryMappedFile, 3 );

    protected:
      MemoryMappedFile
      (
        yidl::runtime::auto_Object<File> underlying_file,
        uint32_t open_flags
      );

      virtual ~MemoryMappedFile() { close(); }

    private:
      yidl::runtime::auto_Object<File> underlying_file;
      uint32_t open_flags;

#ifdef _WIN32
      void* mapping;
#endif
      char* start;
      size_t size;
    };

    typedef yidl::runtime::auto_Object<MemoryMappedFile> auto_MemoryMappedFile;


    class Mutex
    {
    public:
      Mutex();
      ~Mutex();

      // These calls are modeled after the pthread calls they delegate to
      // Have a separate function for timeout == 0 (never block) to
      // avoid an if branch on a critical path
      bool acquire(); // Blocking
      bool timed_acquire( const Time& timeout ); // May block for timeout
      bool try_acquire(); // Never blocks
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
      inline bool timed_acquire( const Time& ) { return true; }
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
        yidl::runtime::atomic_t copied_head, try_pos;
        ElementType try_element;

        for ( ;; )
        {
          copied_head = head;
          try_pos = ( copied_head + 1 ) % ( QueueLength + 2 );
          try_element = reinterpret_cast<ElementType>( elements[try_pos] );

          while
          (
            try_element == reinterpret_cast<ElementType>( 0 ) ||
            try_element == reinterpret_cast<ElementType>( 1 )
          )
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
            yidl::runtime::atomic_cas
            (
              &tail,
              ( try_pos + 1 ) % ( QueueLength + 2 ),
              try_pos
            );

            continue;
          }

          if ( copied_head != head )
            continue;

          if
          (
            yidl::runtime::atomic_cas
            (
              // Memory
              reinterpret_cast<volatile yidl::runtime::atomic_t*>
              (
                &elements[try_pos]
              ),

              // New value
              (
                reinterpret_cast<yidl::runtime::atomic_t>
                (
                  try_element
                ) & POINTER_HIGH_BIT
              ) ? 1 : 0,

              // New value
              reinterpret_cast<yidl::runtime::atomic_t>( try_element )

            ) // Test against old value
            == reinterpret_cast<yidl::runtime::atomic_t>( try_element )
          )
          {
            if ( try_pos % 2 == 0 )
            {
              yidl::runtime::atomic_cas
              (
                &head,
                try_pos,
                copied_head
              );
            }

            return
              reinterpret_cast<ElementType>
              (
                (
                  reinterpret_cast<yidl::runtime::atomic_t>( try_element )
                  & POINTER_LOW_BITS
              ) << 1
            );
          }
        }
      }

      bool enqueue( ElementType element )
      {
#ifdef _DEBUG
        if ( reinterpret_cast<yidl::runtime::atomic_t>( element ) & 0x1 )
          DebugBreak();
#endif

        element = reinterpret_cast<ElementType>
        (
          reinterpret_cast<yidl::runtime::atomic_t>( element ) >> 1
        );

#ifdef _DEBUG
        if
        (
          reinterpret_cast<yidl::runtime::atomic_t>( element ) &
            POINTER_HIGH_BIT
        )
          DebugBreak();
#endif

        yidl::runtime::atomic_t copied_tail,
                                last_try_pos,
                                try_pos; // te, ate, temp
        ElementType try_element;

        for ( ;; )
        {
          copied_tail = tail;
          last_try_pos = copied_tail;
          try_element
            = reinterpret_cast<ElementType>
            (
              elements[last_try_pos]
            );
          try_pos = ( last_try_pos + 1 ) % ( QueueLength + 2 );

          while
          (
            try_element != reinterpret_cast<ElementType>( 0 ) &&
            try_element != reinterpret_cast<ElementType>( 1 )
          )
          {
            if ( copied_tail != tail )
              break;

            if ( try_pos == head )
              break;

            try_element = reinterpret_cast<ElementType>( elements[try_pos] );
            last_try_pos = try_pos;
            try_pos = ( last_try_pos + 1 ) % ( QueueLength + 2 );
          }

          if ( copied_tail != tail ) // Someone changed tail
            continue;                // while we were looping

          if ( try_pos == head )
          {
            last_try_pos = ( try_pos + 1 ) % ( QueueLength + 2 );
            try_element
              = reinterpret_cast<ElementType>( elements[last_try_pos] );

            if ( try_element != reinterpret_cast<ElementType>( 0 ) &&
                 try_element != reinterpret_cast<ElementType>( 1 ) )
              return false; // Queue is full

            yidl::runtime::atomic_cas
            (
              &head,
              last_try_pos,
              try_pos
            );

            continue;
          }

          if ( copied_tail != tail )
            continue;

          // diff next line
          if
          (
            yidl::runtime::atomic_cas
            (
              // Memory
              reinterpret_cast<volatile yidl::runtime::atomic_t*>
              (
                &elements[last_try_pos]
              ),

              // New value
              try_element == reinterpret_cast<ElementType>( 1 ) ?
                ( reinterpret_cast<yidl::runtime::atomic_t>( element )
                  | POINTER_HIGH_BIT ) :
                reinterpret_cast<yidl::runtime::atomic_t>( element ),

              // Old value
              reinterpret_cast<yidl::runtime::atomic_t>( try_element )

            ) // Test against old value
            == reinterpret_cast<yidl::runtime::atomic_t>( try_element )
          )
          {
            if ( try_pos % 2 == 0 )
            {
              yidl::runtime::atomic_cas
              (
                &tail,
                try_pos,
                copied_tail
              );
            }

            return true;
          }
        }
      }

    private:
      volatile ElementType elements[QueueLength+2]; // extra 2 for sentinels
      volatile yidl::runtime::atomic_t head, tail;

#if defined(__LLP64__) || defined(__LP64__)
      const static yidl::runtime::atomic_t POINTER_HIGH_BIT
        = 0x8000000000000000;

      const static yidl::runtime::atomic_t POINTER_LOW_BITS
        = 0x7fffffffffffffff;
#else
      const static yidl::runtime::atomic_t POINTER_HIGH_BIT = 0x80000000;
      const static yidl::runtime::atomic_t POINTER_LOW_BITS = 0x7fffffff;
#endif
    };


    class Path : public yidl::runtime::Object
    {
      // Path objects are currently immutable
    public:
#ifdef _WIN32
      typedef std::wstring string_type;
      const static wchar_t SEPARATOR = L'\\';
#else
      typedef std::string string_type;
      const static char SEPARATOR = '/';
#endif

      Path() { }
      Path( char narrow_path );
      Path( const char* narrow_path );
      Path( const char* narrow_path, size_t narrow_path_len );
      Path( const std::string& narrow_path );
#ifdef _WIN32
      Path( wchar_t wide_path );
      Path( const wchar_t* wide_path );
      Path( const wchar_t* wide_path, size_t wide_path_len );
      Path( const std::wstring& wide_path );
#endif

      Path( const Path& path )
        : path( path.path )
      { }

      ~Path() { }

      Path abspath() const;
      bool empty() const { return path.empty(); }
      Path extension() const;
      Path filename() const;

      operator const string_type&() const { return path; }
      operator const string_type::value_type*() const { return path.c_str(); }
#ifdef _WIN32
      operator std::string() const;
#endif
      string_type::value_type operator[]( string_type::size_type i ) const
      {
        return path[i];
      }

      bool operator==( const Path& path ) const
      {
        return this->path == path.path;
      }

      bool operator==( const string_type& path ) const
      {
        return this->path == path;
      }

      bool operator==( string_type::value_type path ) const
      {
        return this->path.size() == 1 && this->path[0] == path;
      }

      bool operator==( const string_type::value_type* path ) const
      {
        return this->path == path;
      }

      bool operator!=( const Path& path ) const
      {
        return this->path != path.path;
      }

      bool operator!=( const string_type& path ) const
      {
        return this->path != path;
      }

      bool operator!=( string_type::value_type path ) const
      {
        return this->path.size() != 1 || this->path[0] != path;
      }

      bool operator!=( const string_type::value_type* path ) const
      {
        return this->path != path;
      }

      bool operator<( const Path& path ) const // For sorting
      {
        return this->path.compare( path.path ) < 0;
      }

      Path operator+( const Path& path ) const; // Appends to the path
                                           // without adding a separator
      Path operator+( const string_type& path ) const;
      Path operator+( string_type::value_type path ) const;
      Path operator+( const string_type::value_type* path ) const;

      Path parent_path() const;
      Path root_path() const;
      size_t size() const { return path.size(); }
      std::pair<Path, Path> split() const; // head, tail
      void splitall( std::vector<Path>& ) const; // parts between separator
      std::pair<Path, Path> splitext() const;
      Path stem();

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( Path, 5 );

    private:
#ifdef _WIN32
      void init( const char* narrow_path, size_t narrow_path_len );
#endif

      string_type path;
    };

    static inline std::ostream& operator<<( std::ostream& os, const Path& path )
    {
#ifdef _WIN32
      os << static_cast<std::string>( path );
#else
      os << static_cast<const std::string&>( path );
#endif
      return os;
    }

    // Joins two paths, adding a separator as necessary
    static inline Path
    operator/
    (
      const Path& left_path,
      const Path& right_path
    )
    {
      if ( left_path.empty() )
        return right_path;
      else if ( right_path.empty() )
        return left_path;
      else
      {
        Path::string_type
          combined_path( static_cast<const Path::string_type&>( left_path ) );

        if
        (
          left_path[left_path.size()-1] != Path::SEPARATOR
          &&
          static_cast<const Path::string_type&>( right_path )[0]
            != Path::SEPARATOR
        )
          combined_path += Path::SEPARATOR;

        combined_path.append
        (
          static_cast<const Path::string_type&>( right_path )
        );

        return Path( combined_path );
      }
    }

    typedef yidl::runtime::auto_Object<Path> auto_Path;


#ifdef YIELD_HAVE_PERFORMANCE_COUNTERS
    class PerformanceCounterSet : public yidl::runtime::Object
    {
    public:
      enum Event
      {
        EVENT_L1_DCM, // L1 data cache miss
        EVENT_L2_DCM, // L2 data cache miss
        EVENT_L2_ICM // L2 instruction cache miss
      };

      static yidl::runtime::auto_Object<PerformanceCounterSet> create();

      bool addEvent( Event event );
      bool addEvent( const char* name );
      void startCounting();
      void stopCounting( uint64_t* counts );

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( PerformanceCounterSet, 0 );

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

    typedef yidl::runtime::auto_Object<PerformanceCounterSet>
      auto_PerformanceCounterSet;
#endif


    class ProcessorSet : public yidl::runtime::Object
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

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( ProcessorSet, 8 );

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

    typedef yidl::runtime::auto_Object<ProcessorSet> auto_ProcessorSet;


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
          mean = static_cast<SampleType>
                 (
                   static_cast<double>( total ) /
                   static_cast<double>( samples_count )
                 );
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
              median = static_cast<SampleType>
                       (
                         static_cast<double>( median_temp ) / 2.0
                       );
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
        if ( percentile > 0 && percentile < 100 )
        {
          lock.acquire();
          SampleType value;

          if ( samples_count > 0 )
          {
            std::sort( samples, samples + samples_count );
            value =
              samples[static_cast<size_t>( percentile *
                static_cast<double>( samples_count ) )];
          }
          else
            value = 0;

          lock.release();
          return value;
        }
        else
          return 0;
      }

      uint32_t get_samples_count() const
      {
        return samples_count;
      }

      void set_next_sample( SampleType sample )
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
    class SynchronizedNonBlockingFiniteQueue
      : private NonBlockingFiniteQueue<ElementType, QueueLength>
    {
    public:
      ElementType dequeue()
      {
        ElementType element =
          NonBlockingFiniteQueue<ElementType, QueueLength>::dequeue();

        while ( element == 0 )
        {
          signal.acquire();
          element = NonBlockingFiniteQueue<ElementType, QueueLength>::dequeue();
        }

        return element;
      }

      bool enqueue( ElementType element )
      {
        bool enqueued =
          NonBlockingFiniteQueue<ElementType, QueueLength>::enqueue( element );
        signal.release();
        return enqueued;
      }

      ElementType timed_dequeue( const Time& timeout )
      {
        ElementType element
          = NonBlockingFiniteQueue<ElementType, QueueLength>::dequeue();

        if ( element != 0 )
          return element;
        else
        {
          signal.timed_acquire( timeout );
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

      ElementType timed_dequeue( const Time& timeout )
      {
        Time timeout_left( timeout );

        for ( ;; )
        {
          Time start_time;

          if ( signal.timed_acquire( timeout_left ) )
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

          Time elapsed_time; elapsed_time -= start_time;
          if ( elapsed_time < timeout_left )
            timeout_left -= elapsed_time;
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


    class SharedLibrary : public yidl::runtime::Object
    {
    public:
      const static Path SHLIBSUFFIX;


      static yidl::runtime::auto_Object<SharedLibrary>
        open( const Path& file_prefix, const char* argv0 = 0 );

      void* getFunction
      (
        const char* function_name,
        void* missing_function_return_value = NULL
      );

      template <typename FunctionType>
      FunctionType
      getFunction
      (
        const char* function_name,
        FunctionType missing_function_return_value = NULL
      )
      {
        return static_cast<FunctionType>
        (
          getFunction( function_name, missing_function_return_value )
        );
      }

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( SharedLibrary, 11 );

    private:
      SharedLibrary( void* handle );
      ~SharedLibrary();

      void* handle;
    };

    typedef yidl::runtime::auto_Object<SharedLibrary> auto_SharedLibrary;


    class Stat : public yidl::runtime::Object
    {
    public:
      Stat(); // -> set each member individually, for setattr
      Stat( const Stat& );

#ifdef _WIN32
      Stat
      (
        mode_t mode,
        nlink_t nlink,
        uint64_t size,
        const Time& atime,
        const Time& mtime,
        const Time& ctime,
        uint32_t attributes
      );

      Stat( const BY_HANDLE_FILE_INFORMATION& );

      Stat( const WIN32_FIND_DATA& );

      Stat
      (
        uint32_t nNumberOfLinks,
        uint32_t nFileSizeHigh,
        uint32_t nFileSizeLow,
        const FILETIME* ftLastAccessTime,
        const FILETIME* ftLastWriteTime,
        const FILETIME* ftCreationTime,
        uint32_t dwFileAttributes
      );
#else
      // POSIX field order; Linux, FreeBSD, et al. all have different orders
      Stat
      (
        dev_t dev,
        ino_t ino,
        mode_t mode,
        nlink_t nlink,
        uid_t uid,
        gid_t gid,
        dev_t rdev,
        uint64_t size,
        const Time& atime,
        const Time& mtime,
        const Time& ctime,
        blksize_t blksize,
        blkcnt_t blocks
      );
#endif
      Stat( const struct stat& stbuf );

#ifndef _WIN32
      dev_t get_dev() const { return dev; }
      ino_t get_ino() const { return ino; }
#endif
      mode_t get_mode() const { return mode; }
      nlink_t get_nlink() const { return nlink; }
#ifndef _WIN32
      uid_t get_uid() const { return uid; }
      gid_t get_gid() const { return gid; }
      dev_t get_rdev() const { return rdev; }
#endif
      uint64_t get_size() const { return size; }
      const Time& get_atime() const { return atime; }
      const Time& get_mtime() const { return mtime; }
      const Time& get_ctime() const { return ctime; }
#ifndef _WIN32
      blksize_t get_blksize() const { return blksize; }
      blkcnt_t get_blocks() const { return blocks; }
#else
      uint32_t get_attributes() const;
#endif

      bool ISDIR() const { return ( get_mode() & S_IFDIR ) == S_IFDIR; }
      bool ISREG() const { return ( get_mode() & S_IFREG ) == S_IFREG; }
#ifndef _WIN32
      bool ISLNK() const { return S_ISLNK( get_mode() ); }
#endif

      virtual Stat& operator=( const Stat& );
      virtual bool operator==( const Stat& ) const;
      virtual operator std::string() const;
      operator struct stat() const;
#ifdef _WIN32
      operator BY_HANDLE_FILE_INFORMATION() const;
      operator WIN32_FIND_DATA() const;
#endif

      // to_set = bitmask of Volume::SETATTR_* constants
      virtual void set( const Stat&, uint32_t to_set );
#ifndef _WIN32
      virtual void set_dev( dev_t dev );
      virtual void set_ino( ino_t ino );
#endif
      virtual void set_mode( mode_t mode );
      virtual void set_nlink( nlink_t nlink );
#ifndef _WIN32
      virtual void set_uid( uid_t uid );
      virtual void set_gid( gid_t gid );
      virtual void set_rdev( dev_t );
#endif
      virtual void set_size( uint64_t size );
      virtual void set_atime( const Time& atime );
      virtual void set_mtime( const Time& mtime );
      virtual void set_ctime( const Time& ctime );
#ifndef _WIN32
      virtual void set_blksize( blksize_t blksize );
      virtual void set_blocks( blkcnt_t blocks );
#else
      virtual void set_attributes( uint32_t attributes );
#endif

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( Stat, 12 );

    protected:
      virtual ~Stat() { }

    private:
      // POSIX field order; Linux, FreeBSD, et al. all have different orders
#ifndef _WIN32
      dev_t dev;
      ino_t ino;
#endif
      mode_t mode;
      nlink_t nlink;
#ifndef _WIN32
      uid_t uid;
      gid_t gid;
      dev_t rdev;
#endif
      uint64_t size;
      Time atime;
      Time mtime;
      Time ctime;
#ifndef _WIN32
      blksize_t blksize;
      blkcnt_t blocks;
#else
      uint32_t attributes;
#endif
    };

    static inline std::ostream& operator<<( std::ostream& os, const Stat& stbuf )
    {
      os << "{ ";
#ifndef _WIN32
      os << "st_dev: " << stbuf.get_dev() << ", ";
      os << "st_ino: " << stbuf.get_ino() << ", ";
#endif
      os << "st_mode: " << stbuf.get_mode() << " (";
      if ( ( stbuf.get_mode() & S_IFDIR ) == S_IFDIR ) os << "S_IFDIR|";
      if ( ( stbuf.get_mode() & S_IFCHR ) == S_IFCHR ) os << "S_IFCHR|";
      if ( ( stbuf.get_mode() & S_IFREG ) == S_IFREG ) os << "S_IFREG|";
#ifdef _WIN32
      if ( ( stbuf.get_mode() & S_IREAD ) == S_IREAD ) os << "S_IREAD|";
      if ( ( stbuf.get_mode() & S_IWRITE ) == S_IWRITE ) os << "S_IWRITE|";
      if ( ( stbuf.get_mode() & S_IEXEC ) == S_IEXEC ) os << "S_IEXEC|";
#else
      if ( ( stbuf.get_mode() & S_IFBLK ) == S_IFBLK ) os << "S_IFBLK|";
      if ( ( stbuf.get_mode() & S_IFLNK ) == S_IFLNK ) os << "S_IFLNK|";
      if ( ( stbuf.get_mode() & S_IRUSR ) == S_IFDIR ) os << "S_IRUSR|";
      if ( ( stbuf.get_mode() & S_IWUSR ) == S_IWUSR ) os << "S_IWUSR|";
      if ( ( stbuf.get_mode() & S_IXUSR ) == S_IXUSR ) os << "S_IXUSR|";
      if ( ( stbuf.get_mode() & S_IRGRP ) == S_IRGRP ) os << "S_IRGRP|";
      if ( ( stbuf.get_mode() & S_IWGRP ) == S_IWGRP ) os << "S_IWGRP|";
      if ( ( stbuf.get_mode() & S_IXGRP ) == S_IXGRP ) os << "S_IXGRP|";
      if ( ( stbuf.get_mode() & S_IROTH ) == S_IROTH ) os << "S_IROTH|";
      if ( ( stbuf.get_mode() & S_IWOTH ) == S_IWOTH ) os << "S_IWOTH|";
      if ( ( stbuf.get_mode() & S_IXOTH ) == S_IXOTH ) os << "S_IXOTH|";
      if ( ( stbuf.get_mode() & S_ISUID ) == S_ISUID ) os << "S_ISUID|";
      if ( ( stbuf.get_mode() & S_ISGID ) == S_ISGID ) os << "S_ISGID|";
      if ( ( stbuf.get_mode() & S_ISVTX ) == S_ISVTX ) os << "S_ISVTX|";
#endif
      os << "0), ";
#ifndef _WIN32
      os << "st_nlink: " << stbuf.get_nlink() << ", ";
      os << "st_uid: " << stbuf.get_uid() << ", ";
      os << "st_gid: " << stbuf.get_gid() << ", ";
      os << "st_rdev: " << stbuf.get_rdev() << ", ";
#endif
      os << "st_size: " << stbuf.get_size() << ", ";
      os << "st_atime: " << stbuf.get_atime() << ", ";
      os << "st_mtime: " << stbuf.get_mtime() << ", ";
      os << "st_ctime: " << stbuf.get_ctime() << ", ";
#ifndef _WIN32
      os << "st_blksize: " << stbuf.get_blksize() << ", ";
      os << "st_blocks: " << stbuf.get_blocks() << ", ";
#else
      os << "attributes: " << stbuf.get_attributes() << ", ";
#endif
      os << " 0 }";
      return os;
    }

    typedef yidl::runtime::auto_Object<Stat> auto_Stat;


    class Thread : public yidl::runtime::Object
    {
    public:
      Thread();
      virtual ~Thread();

      unsigned long get_id() const { return id; }
      static void* getspecific( unsigned long key ); // Get TLS
      static unsigned long gettid(); // Get current thread ID
      static unsigned long key_create(); // Create TLS key
      static void nanosleep( const Time& );
      void set_name( const char* name );
      bool set_processor_affinity( unsigned short logical_processor_i );
      bool set_processor_affinity( const ProcessorSet& logical_processor_set );
      static void setspecific( unsigned long key, void* value ); // Set TLS
      virtual void run() = 0;
      virtual void start();
      static void yield();

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( Thread, 14 );

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


    class TimerQueue : public yidl::runtime::Object
    {
    public:
      class Timer;

      TimerQueue();
      ~TimerQueue();

      void addTimer( yidl::runtime::auto_Object<Timer> timer );
      static void destroyDefaultTimerQueue();
      static TimerQueue& getDefaultTimerQueue();

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( TimerQueue, 0 );

    private:
#ifdef _WIN32
      TimerQueue( void* hTimerQueue );

      void* hTimerQueue;
#else
      class Thread : public YIELD::platform::Thread
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
        std::priority_queue
        <
          std::pair<Time, Timer*>,
          std::vector< std::pair<Time, Timer*> >,
          std::greater< std::pair<Time, Timer*> >
        > timers;
      };

      Thread thread;
#endif

      static TimerQueue* default_timer_queue;

    public:
      class Timer : public yidl::runtime::Object
      {
      public:
        Timer( const Time& timeout );
        Timer( const Time& timeout, const Time& period );
        virtual ~Timer();

        void delete_();
        const Time& get_period() const { return period; }
        const Time& get_timeout() const { return timeout; }
        virtual void fire() = 0;

        // yidl::runtime::Object
        YIDL_RUNTIME_OBJECT_PROTOTYPES( Timer, 0 );

      private:
        friend class TimerQueue;
#ifndef _WIN32
        friend class TimerQueue::Thread;
#endif

        Time period, timeout;

#ifdef _WIN32
        void *hTimer, *hTimerQueue;
        static void __stdcall WaitOrTimerCallback( void*, unsigned char );
#else
        bool deleted;
#endif
        Time last_fire_time;
      };
    };

    typedef yidl::runtime::auto_Object<TimerQueue> auto_TimerQueue;


    class Volume : public yidl::runtime::Object
    {
    public:
      const static mode_t FILE_MODE_DEFAULT = File::MODE_DEFAULT;
      const static mode_t DIRECTORY_MODE_DEFAULT = S_IREAD|S_IWRITE|S_IEXEC;

      // Flags for setattr's to_set
      const static uint32_t SETATTR_MODE = 1;
#ifndef _WIN32
      const static uint32_t SETATTR_UID = 2;
      const static uint32_t SETATTR_GID = 4;
#endif
      const static uint32_t SETATTR_SIZE = 8;
      const static uint32_t SETATTR_ATIME = 16;
      const static uint32_t SETATTR_MTIME = 32;
      const static uint32_t SETATTR_CTIME = 64;
#ifdef _WIN32
      const static uint32_t SETATTR_ATTRIBUTES = 128;
#endif


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

      YIELD_PLATFORM_VOLUME_PROTOTYPES;

      // Convenience methods that don't make any system calls but delegate to
      // YIELD_PLATFORM_VOLUME_PROTOTYPES,
      // which should be implemented by subclasses

#ifndef _WIN32
      // Delegates to setattr
      virtual bool chmod( const Path& path, mode_t mode );

      // Delegates to setattr
      virtual bool chown( const Path& path, uid_t uid, gid_t gid );
#endif

      // Delegate to open
      virtual auto_File creat( const Path& path );
      virtual auto_File creat( const Path& path, mode_t mode );

      // Delegate to getattr
      virtual bool exists( const Path& path );
      virtual bool isdir( const Path& path );
      virtual bool isfile( const Path& path );

      // Delegates to full listdir
      virtual bool listdir( const Path& path, listdirCallback& );

      // Delegates to full listdir
      virtual bool listdir( const Path& path, std::vector<Path>& out_names );

      // Delegates to readdir
      virtual bool
      listdir
      (
        const Path& path,
        const Path& match_file_name_prefix,
        listdirCallback&
      );

      // Delegates to readdir
      virtual bool
      listdir
      (
        const Path& path,
        const Path& match_file_name_prefix,
        std::vector<Path>& out_names
      );

      // Recursive mkdir
      virtual bool makedirs( const Path& path ); // Python function name
      virtual bool makedirs( const Path& path, mode_t mode );
      virtual bool mkdir( const Path& path );
      virtual bool mktree( const Path& path );
      virtual bool mktree( const Path& path, mode_t mode );

      // Delegates to full open
      virtual auto_File open( const Path& path );
      virtual auto_File open( const Path& path, uint32_t flags );
      virtual auto_File open( const Path& path, uint32_t flags, mode_t mode );

      // Delegates to full readdir
      virtual bool readdir( const Path& path, readdirCallback& );

      // Recursive rmdir + unlink
      virtual bool rmtree( const Path& path );

      // Delegates to getattr
      virtual auto_Stat stat( const Path& path );

      // Delegate to creat
      virtual bool touch( const Path& path );
      virtual bool touch( const Path& path, mode_t mode );

      // Delegate to setattr
      // Uses the most accurate system call available
      virtual bool
      utime
      (
        const YIELD::platform::Path& path,
        const YIELD::platform::Time& atime,
        const YIELD::platform::Time& mtime
      );

      virtual bool
      utime
      (
        const YIELD::platform::Path& path,
        const YIELD::platform::Time& atime,
        const YIELD::platform::Time& mtime,
        const YIELD::platform::Time& ctime
      );

      // yidl::runtime::Object
      YIDL_RUNTIME_OBJECT_PROTOTYPES( Volume, 15 );
    };

    typedef yidl::runtime::auto_Object<Volume> auto_Volume;


    class XDRMarshaller : public yidl::runtime::Marshaller
    {
    public:
      XDRMarshaller();

      yidl::runtime::auto_StringBuffer get_buffer() const { return buffer; }

      // Marshaller
      YIDL_MARSHALLER_PROTOTYPES;
      void writeFloat( const char* key, uint32_t tag, float value );
      void writeInt32( const char* key, uint32_t tag, int32_t value );

    protected:
      virtual void writeKey( const char* key );

    private:
      yidl::runtime::auto_StringBuffer buffer;
      std::vector<bool> in_map_stack;
    };


    class XDRUnmarshaller : public yidl::runtime::Unmarshaller
    {
    public:
      XDRUnmarshaller( yidl::runtime::auto_Buffer buffer );

      // Unmarshaller
      YIDL_UNMARSHALLER_PROTOTYPES;
      float readFloat( const char* key, uint32_t tag );
      int32_t readInt32( const char* key, uint32_t tag );

    private:
      yidl::runtime::auto_Buffer buffer;

      void read( void*, size_t );
    };
  };
};


#endif
