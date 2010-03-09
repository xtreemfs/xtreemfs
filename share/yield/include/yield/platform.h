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

#ifdef __sun
// Solaris's unistd.h defines a function yield that conflicts with the 
// namespace yield. This ugliness is necessary to get around that.
#ifdef __XOPEN_OR_POSIX
#error
#endif
#define __XOPEN_OR_POSIX 1

#ifdef __EXTENSIONS__
#undef __EXTENSIONS__
#include <unistd.h>
#define __EXTENSIONS__ 1
#else
#include <unistd.h>
#endif

#undef __XOPEN_OR_POSIX

#endif

#include "yidl.h"

#ifdef _WIN32
typedef int ssize_t;
extern "C" { __declspec( dllimport ) unsigned long __stdcall GetLastError(); }
#include <string>
using std::wstring;
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
// #define YIELD_PLATFORM_HAVE_PERFORMANCE_COUNTERS 1
#endif
#ifdef YIELD_PLATFORM_HAVE_PAPI
#define YIELD_PLATFORM_HAVE_PERFORMANCE_COUNTERS 1
#endif
//#ifdef YIELD_PLATFORM_HAVE_POSIX_AIO
//#include <aio.h>
//#endif
#endif

#include <exception> // for std::exception

#include <fcntl.h> // For O_RDONLY, O_RDWR

#include <iostream>
using std::cerr;
using std::endl;
using std::cout;

#include <ostream>
using std::ostream;

#include <sstream>
using std::ostringstream;

#include <sys/stat.h>
#include <sys/types.h>

#include <utility>
using std::make_pair;
using std::pair;


#ifdef _WIN32
#ifndef DLLEXPORT
#define DLLEXPORT extern "C" __declspec(dllexport)
#endif
#ifndef NOMINMAX
#define NOMINMAX 1
#endif
#define O_SYNC 010000
#define O_ASYNC 020000
#define O_DIRECT 040000
#define O_HIDDEN 0100000
#ifndef PATH_MAX
#define PATH_MAX 260
#endif
#ifndef UNICODE
#define UNICODE 1
#endif
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN 1
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

#define YIELD_PLATFORM_DIRECTORY_PROTOTYPES \
  virtual yield::platform::Directory::Entry* readdir();


#define YIELD_PLATFORM_FILE_PROTOTYPES \
  virtual bool close(); \
  virtual bool datasync(); \
  virtual yield::platform::Stat* getattr(); \
  virtual bool getlk( bool exclusive, uint64_t offset, uint64_t length ); \
  virtual bool getxattr( const string& name, string& out_value ); \
  virtual bool listxattr( vector<string>& out_names ); \
  virtual ssize_t read( void* buf, size_t buflen, uint64_t offset ); \
  virtual bool removexattr( const string& name ); \
  virtual bool setlk( bool exclusive, uint64_t offset, uint64_t length ); \
  virtual bool setlkw( bool exclusive, uint64_t offset, uint64_t length ); \
  virtual bool \
  setxattr \
  ( \
    const string& name, \
    const string& value, \
    int flags \
  ); \
  virtual bool sync(); \
  virtual bool truncate( uint64_t offset ); \
  virtual bool unlk( uint64_t offset, uint64_t length ); \
  virtual ssize_t write( const void* buf, size_t buflen, uint64_t offset );

#define YIELD_PLATFORM_VOLUME_PROTOTYPES \
    virtual bool access( const yield::platform::Path& path, int amode ); \
    virtual yield::platform::Stat* \
    getattr \
    ( \
      const yield::platform::Path& path \
    ); \
    virtual bool \
    getxattr \
    ( \
      const yield::platform::Path& path, \
      const string& name, \
      string& out_value \
    ); \
    virtual bool \
    link \
    ( \
      const yield::platform::Path& old_path, \
      const yield::platform::Path& new_path \
    ); \
    virtual bool \
    listxattr \
    ( \
      const yield::platform::Path& path, \
      vector<string>& out_names \
    ); \
    virtual bool mkdir( const yield::platform::Path& path, mode_t mode ); \
    virtual yield::platform::File* \
    open \
    ( \
      const yield::platform::Path& path, \
      uint32_t flags, \
      mode_t mode, \
      uint32_t attributes \
    ); \
    virtual yield::platform::Directory* \
    opendir \
    ( \
      const yield::platform::Path& path \
    ); \
    virtual yield::platform::Path* \
    readlink \
    ( \
      const yield::platform::Path& path \
    ); \
    virtual bool \
    removexattr \
    ( \
      const yield::platform::Path& path, \
      const string& name \
    ); \
    virtual bool \
    rename \
    ( \
      const yield::platform::Path& from_path, \
      const yield::platform::Path& to_path \
    ); \
    virtual bool rmdir( const yield::platform::Path& path ); \
    virtual bool \
    setattr \
    ( \
      const yield::platform::Path& path, \
      const yield::platform::Stat& stbuf, \
      uint32_t to_set \
    ); \
    virtual bool \
    setxattr \
    ( \
      const yield::platform::Path& path, \
      const string& name, \
      const string& value, \
      int flags \
    ); \
    virtual bool \
    statvfs \
    ( \
      const yield::platform::Path& path, \
      struct statvfs& \
    ); \
    virtual bool \
    symlink \
    ( \
      const yield::platform::Path& old_path, \
      const yield::platform::Path& new_path \
    ); \
    virtual bool \
    truncate \
    ( \
      const yield::platform::Path& path, \
      uint64_t new_size \
    ); \
    virtual bool unlink( const yield::platform::Path& path ); \
    virtual yield::platform::Path volname( const yield::platform::Path& path );


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
struct addrinfo;
struct sockaddr;
struct sockaddr_storage;
struct timeval;


namespace yield
{
  namespace platform
  {
#ifdef _WIN32
    typedef void* fd_t;
#ifndef INVALID_FD
#define INVALID_FD reinterpret_cast<yield::platform::fd_t>( -1 )
#endif
#ifdef _WIN64
    typedef uint64_t socket_t;    
#else
    typedef uint32_t socket_t;
#endif
#ifndef INVALID_SOCKET
#define INVALID_SOCKET static_cast<yield::platform::socket_t>( -1 )
#endif
#else // Unix
    typedef int fd_t;
#ifndef INVALID_FD
#define INVALID_FD -1
#endif
    typedef int socket_t;
#ifndef INVALID_SOCKET
#define INVALID_SOCKET -1
#endif
#endif

  
    class SocketAddress;
    class Stat;


    using yidl::runtime::Buffer;
    using yidl::runtime::Buffers;
  


    class Stream
    {
    public:
      virtual bool close() = 0;

    protected:
      // Helper methods
      static bool close( fd_t fd );
#ifdef _WIN32
      static bool close( socket_t socket_ );
#endif

      // Helper methods
#ifdef _WIN32
      static bool set_blocking_mode( bool blocking, socket_t socket_ );
#else
      static bool set_blocking_mode( bool blocking, fd_t fd );
#endif
    };


    class IStream : public Stream
    {
    public:
      virtual ~IStream() { }

      class AIOReadCallback
      {
      public:
        // buffer is not a new reference; callees should inc_ref() their own
        // references as necessary
        virtual void onReadCompletion( Buffer& buffer, void* context ) = 0;
        virtual void onReadError( uint32_t error_code, void* context ) = 0;
      };

      virtual void 
      aio_read
      ( 
        Buffer& buffer, // Steals this reference
        AIOReadCallback& callback,
        void* callback_context = NULL
      );

      virtual ssize_t read( Buffer& buffer );
      virtual ssize_t read( void* buf, size_t buflen ) = 0;
    };


    class OStream : public Stream
    {
    public:
      virtual ~OStream() { }

      class AIOWriteCallback
      {
      public:
        // Completed writes/writev are guaranteed to have transferred
        // the full buffer into the kernel
        // -> no partial writes -> no bytes_transferred to the callback        
        virtual void onWriteCompletion( void* context ) = 0;
        virtual void onWriteError( uint32_t error_code, void* context ) = 0;
      };

      virtual void
      aio_write
      ( 
        Buffer& buffer, // Steals this reference
        AIOWriteCallback& callback,
        void* callback_context = NULL
      );

      virtual void 
      aio_writev
      ( 
        Buffers& buffers, // Steals this reference
        AIOWriteCallback& callback,
        void* callback_context = NULL
      );

      // Unlike aio_write/aio_writev, write and writev are not guaranteed
      // to write the full buffer(s), even in non-blocking mode, i.e.
      // they have POSIX semantics.
      virtual ssize_t write( const Buffer& buffer );
      // All non-pure virtual *write* methods delegate to the pure write
      virtual ssize_t write( const void* buf, size_t buflen ) = 0;
      virtual ssize_t writev( const Buffers& buffers );
      virtual ssize_t writev( const struct iovec* iov, uint32_t iovlen );
    };

    
    class IOStream 
      : public yidl::runtime::Object, 
        public IStream, 
        public OStream
    {
    public:
      virtual ~IOStream() { }
    };


    class iconv : public yidl::runtime::Object
    {
    public:
      ~iconv();

      enum Code
      {
        CODE_CHAR,
        CODE_ISO88591,
        CODE_UTF8
      };

#ifdef _WIN32
      static unsigned int Code_to_win32_code_page( Code code );
#else
      static const char* Code_to_iconv_code( Code code );
#endif

      static iconv* open( Code tocode, Code fromcode );

      // Returns ( size_t )-1 on failure, like iconv.3
      size_t
      operator()
      (
        const char** inbuf,
        size_t* inbytesleft,
        char** outbuf,
        size_t* outbytesleft
      );

      // Other operator()'s return false on failure
      bool operator()( const string& inbuf, string& outbuf );
#ifdef _WIN32
      bool operator()( const string& inbuf, wstring& outbuf );
      bool operator()( const wstring& inbuf, string& outbuf );
#endif

    private:
#ifdef _WIN32
      iconv( unsigned int from_code_page, unsigned int to_code_page );
#else
      iconv( void* cd );
      bool reset();
#endif

    private:
#ifdef _WIN32
      unsigned int from_code_page, to_code_page;
#else
      void* cd;
#endif
    };


    class Path : public yidl::runtime::Object
    {
      // Path objects are currently immutable
    public:
#ifdef _WIN32
      typedef wstring string_type;
      const static wchar_t SEPARATOR = L'\\';
#else
      typedef string string_type;
      const static char SEPARATOR = '/';
#endif

      Path() { }

      Path
      ( 
        char narrow_path,
        iconv::Code narrow_path_code = iconv::CODE_CHAR
      );

      Path
      ( 
        const char* narrow_path, 
        iconv::Code narrow_path_code = iconv::CODE_CHAR
      );

      Path
      ( 
        const char* narrow_path, 
        size_t narrow_path_len, 
        iconv::Code narrow_path_code = iconv::CODE_CHAR
      );

      Path
      ( 
        const string& narrow_path, 
        iconv::Code narrow_path_code = iconv::CODE_CHAR
      );
#ifdef _WIN32
      Path( wchar_t wide_path );
      Path( const wchar_t* wide_path );
      Path( const wchar_t* wide_path, size_t wide_path_len );
      Path( const wstring& wide_path );
#endif

      Path( const Path& path );

      Path abspath() const;
      bool empty() const { return path.empty(); }
      string encode( iconv::Code tocode = iconv::CODE_CHAR ) const;
      Path extension() const;
      Path filename() const;
      operator const string_type&() const { return path; }
      operator const string_type::value_type*() const { return path.c_str(); }
#ifdef _WIN32
      operator string() const { return encode( iconv::CODE_CHAR ); }
#endif
      string_type::value_type operator[]( string_type::size_type i ) const;
      bool operator==( const Path& path ) const;
      bool operator==( const string_type& path ) const;
      bool operator==( string_type::value_type path ) const;
      bool operator==( const string_type::value_type* path ) const;
      bool operator!=( const Path& path ) const;
      bool operator!=( const string_type& path ) const;
      bool operator!=( string_type::value_type path ) const;
      bool operator!=( const string_type::value_type* path ) const;
      bool operator<( const Path& path ) const; // For sorting
      Path operator+( const Path& path ) const; // Appends without adding a sep
      Path operator+( const string_type& path ) const;
      Path operator+( string_type::value_type path ) const;
      Path operator+( const string_type::value_type* path ) const;
      Path parent_path() const;
      Path root_path() const;
      size_t size() const { return path.size(); }
      pair<Path, Path> split() const; // head, tail
      void splitall( vector<Path>& ) const; // parts between separator
      pair<Path, Path> splitext() const;
      Path stem();

    private:
      void init( const char*, size_t, iconv::Code );

    private:
      string_type path; // wide path on Win32, host charset path elsewhere
    };

    static inline ostream& operator<<( ostream& os, const Path& path )
    {
#ifdef _WIN32
      os << static_cast<string>( path );
#else
      os << static_cast<const string&>( path );
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

      Time( double unix_time_s );

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
        return static_cast<double>( unix_time_ns ) / NS_IN_US;
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
      operator string() const;

      Time& operator=( const Time& );
      Time& operator=( uint64_t unix_time_ns );
      Time& operator=( double unix_time_s );
      Time operator+( const Time& ) const;
      Time operator+( uint64_t unix_time_ns ) const;
      Time operator+( double unix_time_s ) const;
      Time& operator+=( const Time& );
      Time& operator+=( uint64_t unix_time_ns );
      Time& operator+=( double unix_time_s );
      Time operator-( const Time& ) const;
      Time operator-( uint64_t unix_time_ns ) const;
      Time operator-( double unix_time_s ) const;
      Time& operator-=( const Time& );
      Time& operator-=( uint64_t unix_time_ns );
      Time& operator-=( double unix_time_s );
      Time& operator*=( const Time& );
      bool operator==( const Time& ) const;
      bool operator==( uint64_t unix_time_ns ) const;
      bool operator==( double unix_time_s ) const;
      bool operator!=( const Time& ) const;
      bool operator!=( uint64_t unix_time_ns ) const;
      bool operator!=( double unix_time_s ) const;
      bool operator<( const Time& ) const;
      bool operator<( uint64_t unix_time_ns ) const;
      bool operator<( double unix_time_s ) const;
      bool operator<=( const Time& ) const;
      bool operator<=( uint64_t unix_time_ns ) const;
      bool operator<=( double unix_time_s ) const;
      bool operator>( const Time& ) const;
      bool operator>( uint64_t unix_time_ns ) const;
      bool operator>( double unix_time_s ) const;
      bool operator>=( const Time& ) const;
      bool operator>=( uint64_t unix_time_ns ) const;
      bool operator>=( double unix_time_s ) const;

    private:
      // The time is stored internally as a Unix epoch time, i.e.
      // nanoseconds since January 1, 1970
      uint64_t unix_time_ns;
    };

    static inline ostream& operator<<( ostream& os, const Time& time )
    {
      char iso_date_time[64];
      time.as_iso_date_time( iso_date_time, 64 );
      os << iso_date_time;
      return os;
    }

  
    class Exception : public std::exception
    {
    public:
      // error_message is always copied
      Exception();
      Exception( uint32_t error_code ); // Use a system error message
      Exception( const char* error_message );
      Exception( const string& error_message );
      Exception( uint32_t error_code, const char* error_message );
      Exception( uint32_t error_code, const string& error_message );
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


    class Directory : public yidl::runtime::Object
    {
    public:
      class Entry : public yidl::runtime::Object
      {
        // has-a Stat instead of is-a Stat because
        // (1) readdir on Unix returns a struct dirent,
        //     which is only a subset of struct stat ->
        //     the Stat member will be NULL
        // (2) subclasses of Directory can attach their own
        //     subclasses of Stat
      public:
        Entry( const Path& name );
        Entry( const Path& name, Stat& stbuf ); // Steals reference to Stat
        ~Entry();

        const Path& get_name() const { return name; }
        Stat* get_stat() const { return stbuf; }

      private:
        Path name;
        Stat* stbuf;

        // This class used to have ISDIR(), ISREG(), etc. 
        // methods like Stat to take advantage of Linux's d_type
        // struct dirent member for optimizing things like
        // rmtree (which reads a directory and branches according
        // to the Entry type). d_type is not in POSIX's struct dirent
        // and more strict POSIX implementations (like Sun's), so
        // it's been removed here.
      };

      YIELD_PLATFORM_DIRECTORY_PROTOTYPES;

      // yidl::runtime::Object
      Directory& inc_ref() { return Object::inc_ref( *this ); }

    protected:
      Directory();
      virtual ~Directory();

    private:
      friend class Volume;
#ifdef _WIN32
      Directory( void* hDirectory, const WIN32_FIND_DATA& first_find_data );
#else
      Directory( void* dirp );
#endif

#ifdef _WIN32
      void* hDirectory;
      WIN32_FIND_DATA* first_find_data;
#else
      void* dirp;
#endif
    };


    class File : public IOStream
    {
    public:
      const static uint32_t ATTRIBUTES_DEFAULT = 0;
      const static uint32_t FLAGS_DEFAULT = O_RDONLY;
      const static mode_t MODE_DEFAULT = S_IREAD|S_IWRITE;


      File( fd_t fd ); // Takes ownership of the fd

      YIELD_PLATFORM_FILE_PROTOTYPES;
      virtual size_t getpagesize();
      inline operator fd_t() const { return fd; }
      virtual bool seek( uint64_t offset ); // SEEK_SET
      virtual bool seek( uint64_t offset, unsigned char whence );
      Stat* stat() { return getattr(); }
      
      // IStream
      // read from the current file position
      virtual ssize_t read( Buffer& buffer );
      virtual ssize_t read( void* buf, size_t buflen );
      
      // OStream
      // write to the current file position
      virtual ssize_t write( const Buffer& buffer );
      virtual ssize_t write( const void* buf, size_t buflen );
#ifndef _WIN32
      virtual ssize_t writev( const Buffers& buffers );
      virtual ssize_t writev( const struct iovec* iov, uint32_t iovlen );
#endif

      // yidl::runtime::Object
      File& inc_ref() { return Object::inc_ref( *this ); }

    protected:
      File();
      virtual ~File() { close(); }

    private:
      File( const File& ); // Prevent copying

    private:
      fd_t fd;
    };


    class IOCB : public yidl::runtime::Object
    {
    protected:
      IOCB() { }
      virtual ~IOCB() { }
    };


    class IOQueue : public yidl::runtime::RTTIObject
    {
    public:
      // yidl::runtime::Object
      IOQueue& inc_ref() { return Object::inc_ref( *this ); }

    protected:
      IOQueue() { }
      virtual ~IOQueue() { }
    };


    class BIOCB : public IOCB
    {
    public:
      virtual ~BIOCB() { }
      virtual void execute() = 0;
    };


    class BIOQueue : public IOQueue
    {
    public:
      static BIOQueue& create();
      void submit( BIOCB& biocb ); // Takes ownership of biocb

      // yidl::runtime::RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( BIOQueue, 0 );
      
    private:
      BIOQueue() { }

      class WorkerThread;
    };


    class FDEventPoller : public yidl::runtime::Object
    {
    public:
#ifdef _WIN32
      typedef socket_t fd_t;
#endif


      class FDEvent
      {
      public:
        inline void* get_context() const { return context; }
        inline fd_t get_fd() const { return fd; }
        // want_read and want_write can be true at the same time
        inline bool want_read() const { return want_read_; }
        inline bool want_write() const { return want_write_; }

        void fill( void* context, fd_t fd, bool want_read_, bool want_write_ )
        {
          this->context = context;
          this->fd = fd;
          this->want_read_ = want_read_;
          this->want_write_ = want_write_;
        }

      private:
        void* context;
        fd_t fd;
        bool want_read_, want_write_;
      };


      static FDEventPoller& create();

      bool associate( fd_t fd, bool want_read = true, bool want_write = false )
      {
        return associate( fd, NULL, want_read, want_write );
      }

      virtual bool
      associate
      (        
        fd_t fd,
        void* context,
        bool want_read = true,
        bool want_write = false
      ) = 0;

      virtual bool dissociate( fd_t fd ) = 0;

      // poll methods do not cache any state, i.e.
      // they always make a new system call

      // Blocking poll to check if any FD is active
      bool poll();

      // Timed poll to check if FD any active
      bool poll( const Time& timeout );

      // Blocking poll for a single FDEvent
      bool poll( FDEvent& fd_event );

      // Timed poll for a single FDEvent
      bool poll( FDEvent& fd_event, const Time& timeout ); 

      // Blocking poll for multiple FDEvents
      int poll( FDEvent* fd_events, int fd_events_len );

      // Timed poll for multiple FDEvents
      int
      poll
      ( 
        FDEvent* fd_events,
        int fd_events_len, 
        const Time& timeout 
      );

      virtual bool toggle( fd_t fd, bool want_read, bool want_write ) = 0;

      // Non-blocking poll to check if any FD is active
      bool try_poll();

      // Non-blocking poll for a single FDEvent
      bool try_poll( FDEvent& fd_event );

      // Non-blocking poll for multiple FDEvents
      int try_poll( FDEvent* fd_events, int fd_events_len );

    protected:
      FDEventPoller()
      { }

      // The real poll for implementations to override
      virtual int
      poll
      ( 
        FDEvent* fd_events, 
        int fd_events_len, 
        const Time* timeout // NULL for a blocking poll
      ) = 0;
    };


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
          if ( level <= log.get_level() )
            oss << t;
          return *this;
        }

      private:
        friend class Log;

        Stream( Log& log, Level );

        Log& log;
        Level level;

        ostringstream oss;
      };


      static Log& open( ostream&, Level );
      static Log& open( const Path& path, Level level, bool lazy = false );

      Level get_level() const { return level; }
      Stream get_stream() { return Stream( inc_ref(), level ); }
      Stream get_stream( Level level ) { return Stream( inc_ref(), level ); }
      void set_level( Level level ) { this->level = level; }

      void write( const char* str, Level level );
      void write( const string& str, Level level );
      void write( const void* str, size_t str_len, Level level );
      void write( const unsigned char* str, size_t str_len, Level level );
      void write( const char* str, size_t str_len, Level level );

      // yidl::runtime::Object
      Log& inc_ref() { return Object::inc_ref( *this ); }

    protected:
      Log( Level level )
        : level( level )
      { }

      virtual ~Log() { }

      virtual void write( const char* str, size_t str_len ) = 0;

    private:
      Level level;
    };


    class MemoryMappedFile : public yidl::runtime::Object
    {
    public:
      static MemoryMappedFile* open( const Path& path );
      static MemoryMappedFile* open( const Path& path, uint32_t flags );

      static MemoryMappedFile*
      open
      (
        const Path& path,
        uint32_t flags,
        mode_t mode,
        uint32_t attributes,
        size_t minimum_size
      );

      virtual bool close();
      operator char*() const { return start; }
      operator void*() const { return start; }
      size_t size() const { return size_; }
      bool resize( size_t );
      virtual bool sync();
      virtual bool sync( size_t offset, size_t length );
      virtual bool sync( void* ptr, size_t length );

    protected:
      MemoryMappedFile( File& underlying_file, uint32_t open_flags );
      virtual ~MemoryMappedFile();

    private:
      File& underlying_file;
      uint32_t open_flags;

#ifdef _WIN32
      void* mapping;
#endif
      char* start;
      size_t size_;
    };


    class Mutex
    {
    public:
      Mutex();
      ~Mutex();

      // These calls are modeled after the pthread calls they delegate to
      // Have a separate function for timeout == 0 (never block) to
      // avoid an if branch on a critical path
      bool acquire(); // Blocking
      bool acquire( const Time& timeout ); // May block for timeout
      bool try_acquire(); // Never blocks
      void release();

    private:
#ifdef _WIN32
      void* hMutex;
#else
      pthread_mutex_t pthread_mutex;
#endif
    };


    class NamedPipe : public File
    {
    public:
      static NamedPipe*
      open
      (
        const Path& path,
        uint32_t flags = O_RDWR,
        mode_t mode = File::MODE_DEFAULT
      );

#ifdef _WIN32
      // IStream
      ssize_t read( void* buf, size_t buflen );

      // OStream
      ssize_t write( const void* buf, size_t buflen );
#endif

    private:
#ifdef WIN32
      NamedPipe( fd_t fd, bool connected );
#else
      NamedPipe( fd_t fd );
#endif
      ~NamedPipe() { }

#ifdef _WIN32
      bool connected;
      bool connect();
#endif
    };

    
    class NBIOCB : public IOCB 
    {
    public:
      virtual ~NBIOCB() { }

      virtual void execute() = 0;

#ifdef _WIN32
      virtual socket_t get_fd() const = 0;
#else
      virtual int get_fd() const = 0;
#endif

      enum State 
      { 
        STATE_WANT_CONNECT, 
        STATE_WANT_READ, 
        STATE_WANT_WRITE, 
        STATE_COMPLETE, 
        STATE_ERROR 
      };

      State get_state() const { return state; }

    protected:
      NBIOCB( State state )
        : state( state )
      { }

      void set_state( State state ) { this->state = state; }

    private:
      State state;
    };


    class NBIOQueue : public IOQueue
    {
    public:
      static NBIOQueue& create();

      void submit( NBIOCB& nbiocb ); // Takes ownership of nbiocb
      
      // yidl::runtime::RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( NBIOQueue, 2 );

    private:
      class WorkerThread;

      NBIOQueue( const vector<WorkerThread*>& worker_threads );
      ~NBIOQueue();

    private:
      vector<WorkerThread*> worker_threads;
    };


    class NOPLock
    {
    public:
      inline bool acquire() { return true; }
      inline bool acquire( const Time& ) { return true; }
      inline void release() { }
      inline bool try_acquire() { return true; }      
    };


    // Modelled after Python's optparse.OptionParser class
    class OptionParser : public yidl::runtime::Object
    {
    public:
      class Option
      {
      public:
       Option
        ( 
          const string& arg,
          const string& help,
          bool require_value = false
        )
        : arg( arg ), help( help ), require_value( require_value )
        { }

        const string& get_arg() const { return arg; }
        const string& get_help() const { return help; }
        bool get_require_value() const { return require_value; }
        const string& get_value() const { return value; }

        bool operator<( const Option& other ) const
        {
          return arg.compare( other.arg ) < 0;
        }

        void set_value( const string& value ) { this->value = value; }

      private:
        string arg, help;
        bool require_value;
        string value;
      };


      class InvalidValueException : public Exception
      {
      public:
        InvalidValueException( const string& error_message )
          : Exception( 1, error_message )
        { }
      };


      class MissingValueException : public Exception
      {
      public:
        MissingValueException( const string& error_message )
          : Exception( 1, error_message )
        { }
      };


      class UnexpectedValueException : public Exception
      {
      public:
        UnexpectedValueException( const string& error_message )
          : Exception( 1, error_message )
        { }
      };


      class UnregisteredOptionException : public Exception
      {
      public:
        UnregisteredOptionException( const string& error_message )
          : Exception( 1, error_message )
        { }
      };


      void 
      add_option
      ( 
        const string& arg, 
        bool require_value = false 
      );

      void
      add_option
      ( 
        const string& arg, 
        const string& help, 
        bool require_value = false 
      );

      int // Returns the number of argv strings parsed
      parse_args // Throws exceptions on errors
      ( 
        int argc, 
        char** argv, 
        vector<Option>& parsed_options
      );

      void 
      print_usage
      ( 
        const string& program_name,
        const char* program_description = NULL,
        const char* files_usage = NULL
      );

    private:
      vector<Option> options;
    };


#ifdef YIELD_PLATFORM_HAVE_PERFORMANCE_COUNTERS
    class PerformanceCounterSet : public yidl::runtime::Object
    {
    public:
      enum Event
      {
        EVENT_L1_DCM, // L1 data cache miss
        EVENT_L2_DCM, // L2 data cache miss
        EVENT_L2_ICM // L2 instruction cache miss
      };

      static PerformanceCounterSet* create();

      bool addEvent( Event event );
      bool addEvent( const char* name );
      void startCounting();
      void stopCounting( uint64_t* counts );

    private:
#if defined(__sun)
      PerformanceCounterSet( cpc_t* cpc, cpc_set_t* cpc_set );
      cpc_t* cpc; cpc_set_t* cpc_set;

      vector<int> event_indices;
      cpc_buf_t* start_cpc_buf;
#elif defined(YIELD_PLATFORM_HAVE_PAPI)
      PerformanceCounterSet( int papi_eventset );
      int papi_eventset;
#endif

      ~PerformanceCounterSet();
    };
#endif


    class Pipe : public IOStream
    {
    public:
      static Pipe& create(); 

      // Stream
      bool close();

      fd_t get_read_end() const { return ends[0]; }
      fd_t get_write_end() const { return ends[1]; }
      fd_t operator[]( size_t n ) const { return ends[n]; }
      bool set_read_blocking_mode( bool blocking );
      bool set_write_blocking_mode( bool blocking );

      // IStream
      ssize_t read( void* buf, size_t buflen );
      
      // OStream
      ssize_t write( const void* buf, size_t buflen );

    private:
      Pipe( fd_t ends[2] );
      ~Pipe();

    private:
      fd_t ends[2];
    };


    class Process : public yidl::runtime::Object
    {
    public:
      static Process& create( const Path& executable_file_path );
      static Process& create( int argc, char** argv );

      static Process&
      create
      (
        const Path& executable_file_path,
        const char** null_terminated_argv
      );

      Pipe* get_stdin() const { return child_stdin; }
      Pipe* get_stdout() const { return child_stdout; }
      Pipe* get_stderr() const { return child_stderr; }

      static unsigned long getpid(); // Get current pid
      bool kill(); // SIGKILL
      bool poll( int* out_return_code = 0 ); // Calls waitpid() but WNOHANG
      bool terminate(); // SIGTERM
      int wait(); // Calls waitpid() and blocks

    private:
      Process
      (
#ifdef _WIN32
        void* hChildProcess,
        void* hChildThread,
#else
        pid_t child_pid,
#endif
        Pipe* child_stdin,
        Pipe* child_stdout,
        Pipe* child_stderr
      );

      ~Process();

#ifdef _WIN32
      void *hChildProcess, *hChildThread;
#else
      int child_pid;
#endif
      Pipe *child_stdin, *child_stdout, *child_stderr;
    };


    class ProcessorSet : public yidl::runtime::Object
    {
    public:
      ProcessorSet();
      ProcessorSet( uint32_t from_mask );

      void clear();
      void clear( uint16_t processor_i );
      uint16_t count() const;
      bool empty() const;
      static uint16_t getLogicalProcessorsPerPhysicalProcessor();
      static uint16_t getOnlineLogicalProcessorCount();
      static uint16_t getOnlinePhysicalProcessorCount();
      bool isset( uint16_t processor_i ) const;
      bool set( uint16_t processor_i );

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


    class Semaphore
    {
    public:
      Semaphore();
      ~Semaphore();

      bool acquire(); // Blocking
      bool acquire( const Time& timeout ); // May block for timeout
      void release();
      bool try_acquire(); // Never blocks

    private:
#if defined(_WIN32)
      void* hSemaphore;
#elif defined(__MACH__)
      semaphore_t sem;
#else
      sem_t sem;
#endif
    };


    class SharedLibrary : public yidl::runtime::Object
    {
    public:
#ifdef _WIN32
      const static wchar_t* SHLIBSUFFIX;
#else
      const static char* SHLIBSUFFIX;
#endif

      static SharedLibrary*
      open
      ( 
        const Path& file_prefix, 
        const char* argv0 = 0 
      );

      void*
      getFunction
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

    private:
      SharedLibrary( void* handle );
      ~SharedLibrary();

      void* handle;
    };


    class Socket : public IOStream
    {
    public:
      static int DOMAIN_DEFAULT; // AF_INET6
      
      typedef uint32_t Option;
      const static Option OPTION_SO_KEEPALIVE = 1;
      const static Option OPTION_SO_LINGER = 2;

      // Platform-independent flag constants for recv
      // These will be translated to platform-specific constants
      // Any other bits set in recv( ..., flags ) will be passed through
      const static int RECV_FLAG_MSG_OOB = 1;
      const static int RECV_FLAG_MSG_PEEK = 2;

      // Platform-independent flag constants for send
      const static int SEND_FLAG_MSG_DONTROUTE = 1;
      const static int SEND_FLAG_MSG_OOB = 2;


      Socket( int domain, int type, int protocol, socket_t socket_ );
      virtual ~Socket();      

      // aio_connect is here and not in TCPSocket even though it's not 
      // very useful (for e.g. UDPSockets), so that upper layers can
      // be agnostic of which type of Socket they're connect()'ing
      class AIOConnectCallback
      {
      public:
        virtual void onConnectCompletion( void* context ) = 0;
        virtual void onConnectError( uint32_t error_code, void* context ) = 0;
      };

      virtual void 
      aio_connect
      ( 
        SocketAddress& peername,
        AIOConnectCallback& callback,
        void* callback_context = NULL
      );

      virtual void
      aio_recv
      ( 
        Buffer& buffer, // Steals this reference
        int flags,
        AIOReadCallback& callback,
        void* callback_context = NULL
      );

      virtual void 
      aio_send
      (
        Buffer& buffer, // Steals this reference
        int flags,
        AIOWriteCallback& callback,
        void* callback_context = NULL
      );

      virtual void
      aio_sendmsg
      (
        Buffers& buffers, // Steals this reference
        int flags,
        AIOWriteCallback& callback,
        void* callback_context = NULL
      );

      virtual bool associate( IOQueue& io_queue );
      virtual bool bind( const SocketAddress& to_sockaddr );
      virtual bool close();
      virtual bool connect( const SocketAddress& peername );
      static Socket* create( int type, int protocol );
      static Socket* create( int domain, int type, int protocol );
      virtual bool get_blocking_mode() const;
      int get_domain() const { return domain; }
      static string getfqdn();
      static string gethostname();
      SocketAddress* getpeername() const;
      int get_protocol() const { return protocol; }
      SocketAddress* getsockname() const;
      int get_type() const { return type; }
      bool is_closed() const;
      inline bool is_connected() const { return connected; }
      virtual bool listen();
      bool operator==( const Socket& other ) const;
      inline operator socket_t() const { return socket_; }
      bool recreate();
      bool recreate( int domain );

      ssize_t recv( Buffer& buffer, int flags = 0 );

      // The real recv method, can be overridden by subclasses
      virtual ssize_t recv( void* buf, size_t buflen, int flags = 0 );

      ssize_t send( const Buffer& buffer, int flags = 0 )
      {
        return send( buffer, buffer.size(), flags );
      }

      // The real send method, can be overridden by subclasses
      virtual ssize_t send( const void* buf, size_t len, int flags = 0 );

      ssize_t sendmsg( const Buffers& buffers, int flags = 0 )
      {
        return sendmsg( buffers, buffers.size(), flags );
      }

      // The real sendmsg method, can be overridden by subclasses
      virtual ssize_t
      sendmsg
      ( 
        const struct iovec* iov,
        uint32_t iovlen,
        int flags = 0
      );

      virtual bool set_blocking_mode( bool blocking );
      virtual bool setsockopt( Option option, bool onoff );
      virtual bool shutdown( bool shut_rd = true, bool shut_wr = true );
      virtual bool want_connect() const;
      virtual bool want_read() const;
      virtual bool want_write() const;

      // yidl::runtime::Object
      Socket& inc_ref() { return Object::inc_ref( *this ); }

      // IStream
      void
      aio_read
      ( 
        Buffer& buffer, // Steals this reference
        AIOReadCallback& callback,
        void* callback_context = NULL
      )
      {
        aio_recv( buffer, 0, callback, callback_context );
      }

      ssize_t read( Buffer& buffer )
      {
        return IStream::read( buffer );
      }

      ssize_t read( void* buf, size_t buflen )
      {
        return recv( buf, buflen, 0 );
      }

      // OStream
      void
      aio_write
      ( 
        Buffer& buffer, // Steals this reference
        AIOWriteCallback& callback,
        void* callback_context = NULL
      )
      {
        aio_send( buffer, 0, callback, callback_context );
      }

      void 
      aio_writev
      ( 
        Buffers& buffers, // Steals this reference
        AIOWriteCallback& callback,
        void* callback_context = NULL
      )
      {
        aio_sendmsg( buffers, 0, callback, callback_context );
      }

      ssize_t write( const Buffer& buffer )
      {
        return OStream::write( buffer );
      }

      ssize_t write( const void* buf, size_t buflen )
      {
        return send( buf, buflen, 0 );
      }

      ssize_t writev( const Buffers& buffers )
      {
        return OStream::writev( buffers );
      }

      ssize_t writev( const struct iovec* iov, uint32_t iovlen )
      {
        return sendmsg( iov, iovlen, 0 );
      }

    protected:
      static socket_t create( int* domain, int type, int protocol );

      IOQueue* get_io_queue() const { return io_queue; }
      static uint32_t get_last_error(); // WSAGetLastError / errno
      static int get_platform_recv_flags( int flags );
      static int get_platform_send_flags( int flags );

#ifdef _WIN64
      void iovecs_to_wsabufs( const iovec*, vector<iovec64>& );
#endif

      void set_io_queue( IOQueue& io_queue );

    protected:
      template <class AIOCallbackType>
      class IOCB
      {
      protected:
        IOCB( AIOCallbackType& callback, void* callback_context )
          : callback( callback ), callback_context( callback_context )
        { }

        AIOCallbackType& callback;
        void* callback_context;
      };


      class IOConnectCB : public IOCB<AIOConnectCallback>
      {
      protected:
        IOConnectCB
        ( 
          SocketAddress& peername, 
          Socket& socket_, 
          AIOConnectCallback& callback, 
          void* callback_context
        );

        virtual ~IOConnectCB();

        const SocketAddress& get_peername() const { return peername; }
        Socket& get_socket() const { return socket_; }

        void onConnectCompletion();
        void onConnectError();
        void onConnectError( uint32_t error_code );

      private:
        SocketAddress& peername;
        Socket& socket_;
      };


      class IORecvCB : public IOCB<AIOReadCallback>
      {
      protected:
        IORecvCB
        ( 
          Buffer& buffer, 
          int flags, 
          Socket& socket_, 
          AIOReadCallback& callback, 
          void* callback_context
        );

        virtual ~IORecvCB();

        Buffer& get_buffer() const { return buffer; }
        int get_flags() const { return flags; }
        Socket& get_socket() const { return socket_; }

        void onReadCompletion();
        void onReadError();
        void onReadError( uint32_t error_code );

      private:
        Buffer& buffer;
        int flags;
        Socket& socket_;
      };


      class IOSendCB : public IOCB<AIOWriteCallback>
      {
      protected:
        IOSendCB
        ( 
          Buffer& buffer, 
          int flags, 
          Socket& socket_, 
          AIOWriteCallback& callback, 
          void* callback_context
        );

        virtual ~IOSendCB();

        bool execute( bool blocking_mode );

        const Buffer& get_buffer() const { return buffer; }
        int get_flags() const { return flags; }
        Socket& get_socket() const { return socket_; }

        void onWriteCompletion();
        void onWriteError();
        void onWriteError( uint32_t error_code );

      private:
        Buffer& buffer;
        int flags;
        size_t partial_send_len;
        Socket& socket_;
      };


      class IOSendMsgCB : public IOCB<AIOWriteCallback>
      {
      protected:
        IOSendMsgCB
        ( 
          Buffers& buffers,
          int flags, 
          Socket& socket_, 
          AIOWriteCallback& callback, 
          void* callback_context
        );

        virtual ~IOSendMsgCB();

        bool execute( bool blocking_mode );

        const Buffers& get_buffers() const { return buffers; }
        size_t get_buffers_len() const { return buffers_len; }
        int get_flags() const { return flags; }
        Socket& get_socket() const { return socket_; }

        void onWriteCompletion();
        void onWriteError();
        void onWriteError( uint32_t error_code );

      private:
        Buffers& buffers;
        size_t buffers_len;
        int flags;
        size_t partial_send_len;
        Socket& socket_;
      };

    private:
      Socket( const Socket& ) { DebugBreak(); } // Prevent copying

    private:
      int domain, type, protocol;
      socket_t socket_;

      bool blocking_mode, connected;
      IOQueue* io_queue;

    private:
      class BIOConnectCB;
      class BIORecvCB;
      class BIOSendCB;
      class BIOSendMsgCB;
      class NBIOConnectCB;
      class NBIORecvCB;
      class NBIOSendCB;
      class NBIOSendMsgCB;
    };


    class SocketAddress : public yidl::runtime::Object // immutable
    {
    public:
      SocketAddress( struct addrinfo& ); // Takes ownership
      SocketAddress( const struct sockaddr_storage& ); // Copies

      static SocketAddress* create(); // INADDR_ANY
      static SocketAddress* create( const char* hostname );
      static SocketAddress* create( const char* hostname, uint16_t port );

#ifdef _WIN32
      bool as_struct_sockaddr
      (
        int family,
        struct sockaddr*& out_sockaddr,
        int32_t& out_sockaddrlen
      ) const;
#else
      bool as_struct_sockaddr
      (
        int family,
        struct sockaddr*& out_sockaddr,
        uint32_t& out_sockaddrlen
      ) const;
#endif

      bool
      getnameinfo
      (
        string& out_hostname,
        bool numeric = true
      ) const;

      bool getnameinfo
      (
        char* out_hostname,
        uint32_t out_hostname_len,
        bool numeric = true
      ) const;

      uint16_t get_port() const;
      bool operator==( const SocketAddress& ) const;

      bool operator!=( const SocketAddress& other ) const
      {
        return !operator==( other );
      }

      // yidl::runtime::Object
      SocketAddress& inc_ref() { return Object::inc_ref( *this ); }

    private:
      SocketAddress( const SocketAddress& ) 
      { 
        DebugBreak(); // Prevent copying
      }

      ~SocketAddress();

      // Linked sockaddr's obtained from getaddrinfo(3)
      // Will be NULL if _sockaddr_storage is used
      struct addrinfo* addrinfo_list;

      // A single sockaddr passed in the constructor and copied
      // Will be NULL if addrinfo_list is used
      struct sockaddr_storage* _sockaddr_storage;

      static struct addrinfo* getaddrinfo( const char* hostname, uint16_t port );
    };


    class SocketPair : public yidl::runtime::Object
    {
    public:
      static SocketPair& create();

      Socket& first() const { return first_socket; }
      Socket& second() const { return second_socket; }

    private:
      SocketPair( Socket& first_socket, Socket& second_socket );
      ~SocketPair();

    private:
      Socket &first_socket, &second_socket;
    };


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

      virtual ~Stat() { }

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
#ifndef _WIN32
      bool ISLNK() const { return S_ISLNK( get_mode() ); }
#endif
      bool ISREG() const { return ( get_mode() & S_IFREG ) == S_IFREG; }

      virtual Stat& operator=( const Stat& );
      virtual bool operator==( const Stat& ) const;
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
      Stat& inc_ref() { return Object::inc_ref( *this ); }

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


    class TCPSocket : public Socket
    {
    public:
      const static int OPTION_TCP_NODELAY = 4;
      static int PROTOCOL; // IPPROTO_TCP
      static int TYPE; // SOCK_STREAM

      virtual ~TCPSocket() { }

      virtual TCPSocket* accept();

      class AIOAcceptCallback
      {
      public:
        // accepted_tcp_socket is not a new reference; 
        // callees should inc_ref() their own references as necessary
        virtual void 
        onAcceptCompletion
        ( 
          TCPSocket& accepted_tcp_socket,
          void* context
        ) = 0;

        virtual void onAcceptError( uint32_t error_code, void* context ) = 0;
      };

      virtual void 
      aio_accept
      ( 
        AIOAcceptCallback& callback, 
        void* callback_context = NULL
      );

      static TCPSocket* create(); // AF_INET6
      static TCPSocket* create( int domain );

      // yidl::runtime::Object
      TCPSocket& inc_ref() { return Object::inc_ref( *this ); }

      // Socket
      virtual void 
      aio_connect
      ( 
        SocketAddress& peername,
        AIOConnectCallback& callback,
        void* callback_context = NULL
      );

      virtual void
      aio_recv
      ( 
        Buffer& buffer, // Steals this reference
        int flags,
        AIOReadCallback& callback,
        void* callback_context = NULL
      );

      virtual void 
      aio_send
      (
        Buffer& buffer, // Steals this reference
        int flags,
        AIOWriteCallback& callback,
        void* callback_context = NULL
      );

      virtual void
      aio_sendmsg
      (
        Buffers& buffers, // Steals this reference
        int flags,
        AIOWriteCallback& callback,
        void* callback_context = NULL
      );

      virtual bool associate( IOQueue& io_queue );
      virtual bool setsockopt( Option option, bool onoff );
      virtual bool want_accept() const;

    protected:
      TCPSocket( int domain, socket_t );      

      // _accept -> socket method used by subclasses (e.g. SSLSocket)
      socket_t _accept();
      static socket_t create( int* domain );

    protected:
      class IOAcceptCB : public IOCB<AIOAcceptCallback>
      {
      protected:
        IOAcceptCB( TCPSocket&, AIOAcceptCallback& callback, void* );
        virtual ~IOAcceptCB();

        TCPSocket& get_listen_tcp_socket() const
        {
          return listen_tcp_socket;
        }

        void onAcceptCompletion( TCPSocket& accepted_tcp_socket );
        void onAcceptError();
        void onAcceptError( uint32_t error_code );

      private:
        TCPSocket& listen_tcp_socket;
      };

    private:
#ifdef _WIN32
      static void *lpfnAcceptEx, *lpfnConnectEx;
#endif

    private:
      class BIOAcceptCB;
      class NBIOAcceptCB;
#ifdef _WIN32
      class Win32AIOAcceptCB;
      class Win32AIOConnectCB;
      class Win32AIORecvCB;
      class Win32AIOSendCB;
      class Win32AIOSendMsgCB;
#endif
    };


    class Thread : public yidl::runtime::Object
    {
    public:
      Thread();
      virtual ~Thread();

      unsigned long get_id() const { return id; }
      static void* getspecific( unsigned long key ); // Get TLS
      static unsigned long gettid(); // Get current thread ID
      inline bool is_running() const { return state == STATE_RUNNING; }
      bool join(); // See pthread_join
      static unsigned long key_create(); // Create TLS key
      static void nanosleep( const Time& );
      virtual void run() = 0;
      void set_name( const char* name );
      bool set_processor_affinity( unsigned short logical_processor_i );
      bool set_processor_affinity( const ProcessorSet& logical_processor_set );
      static void setspecific( unsigned long key, void* value ); // Set TLS
      virtual void start(); // Only returns after run() has been called
      static void yield();

    private:
#ifdef _WIN32
      static unsigned long __stdcall thread_stub( void* );
#else
      static void* thread_stub( void* );
#endif

    private:
#if defined(_WIN32)
      void* handle;
#else
      pthread_t handle;
#endif
      unsigned long id;
      enum { STATE_READY, STATE_RUNNING, STATE_STOPPED } state;
    };


    class TimerQueue : public yidl::runtime::Object
    {
#ifndef _WIN32
    private:
      class Thread;
#endif

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
        Timer& inc_ref() { return Object::inc_ref( *this ); }

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


      TimerQueue();
      ~TimerQueue();

      void addTimer( Timer& timer );
      static void destroyDefaultTimerQueue();
      static TimerQueue& getDefaultTimerQueue();

    private:
#ifdef _WIN32
      TimerQueue( void* hTimerQueue );
#endif

   private:
      static TimerQueue* default_timer_queue;
#ifdef _WIN32
      void* hTimerQueue;
#else
      Thread* thread;
#endif
    };


    class UDPSocket : public Socket
    {
    public:
      static int PROTOCOL; // IPPROTO_UDP
      static int TYPE; // SOCK_DGRAM


      virtual ~UDPSocket() { }

      class AIORecvFromCallback
      {
      public:
        // buffer and peername are not new references; 
        // callees should inc_ref() their own references as necessary
        virtual void
        onRecvFromCompletion
        ( 
          Buffer& buffer, 
          SocketAddress& peername,
          void* context 
        ) = 0;

        virtual void
        onRecvFromError
        (
          uint32_t error_code,
          void* context
        ) = 0;
      };

      void 
      aio_recvfrom
      ( 
        Buffer& buffer, // Steals this reference
        AIORecvFromCallback& callback,
        void* callback_context = NULL
      )
      {
        aio_recvfrom( buffer, 0, callback, callback_context );
      }

      void 
      aio_recvfrom
      ( 
        Buffer& buffer, // Steals this reference
        int flags,
        AIORecvFromCallback& callback,
        void* callback_context = NULL
      );

      static UDPSocket* create();

      ssize_t
      recvfrom
      ( 
        Buffer& buffer,
        struct sockaddr_storage& peername 
      ) const
      {
        return recvfrom( buffer, 0, peername );
      }

      ssize_t recvfrom
      (
        Buffer& buffer,
        int flags,
        struct sockaddr_storage& peername
      ) const;

      ssize_t
      recvfrom
      (
        void* buf,
        size_t buflen,
        struct sockaddr_storage& peername
      ) const
      {
        return recvfrom( buf, buflen, 0, peername );
      }

      // The real recvfrom method
      ssize_t
      recvfrom
      (
        void* buf,
        size_t buflen,
        int flags,
        struct sockaddr_storage& peername
      ) const;

      ssize_t
      sendmsg
      (
        const Buffers& buffers,
        const SocketAddress& peername,
        int flags = 0
      ) const
      {
        return sendmsg( buffers, buffers.size(), peername, flags );
      }

      // sendmsg analog to sendto
      ssize_t
      sendmsg
      (
        const struct iovec* iov,
        uint32_t iovlen,
        const SocketAddress& peername,
        int flags = 0
      ) const;

      ssize_t 
      sendto
      ( 
        const Buffer& buffer, 
        const SocketAddress& peername 
      ) const
      {
        return sendto( buffer, 0, peername );
      }

      ssize_t 
      sendto
      (
        const Buffer& buffer,
        int flags,
        const SocketAddress& peername
      ) const
      {
        return sendto( buffer, buffer.size(), flags, peername );
      }

      ssize_t sendto
      (
        const void* buf,
        size_t buflen,
        const SocketAddress& peername
      ) const
      {
        return sendto( buf, buflen, 0, peername );
      }

      ssize_t
      sendto
      (
        const void* buf,
        size_t buflen,
        int flags,
        const SocketAddress& peername
      ) const;

      // yidl::runtime::Object
      UDPSocket& inc_ref() { return Object::inc_ref( *this ); }

    private:
      UDPSocket( int domain, socket_t );

    private:
      class BIORecvFromCB;
      class IORecvFromCB;
      class NBIORecvFromCB;
#ifdef _WIN32
      class Win32AIORecvFromCB;
#endif
    };


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

      virtual ~Volume() { }

      YIELD_PLATFORM_VOLUME_PROTOTYPES;

      // The following are all convenience methods that don't make any system calls
      //  but delegate to YIELD_PLATFORM_VOLUME_PROTOTYPES,
      // which should be implemented by subclasses

#ifndef _WIN32
      // Delegates to setattr
      virtual bool chmod( const Path& path, mode_t mode );

      // Delegates to setattr
      virtual bool chown( const Path& path, uid_t uid, gid_t gid );
#endif

      // Delegate to open
      virtual File* creat( const Path& path );
      virtual File* creat( const Path& path, mode_t mode );

      // Delegate to getattr
      virtual bool exists( const Path& path );
      virtual bool isdir( const Path& path );
      virtual bool isfile( const Path& path );

      // Recursive mkdir
      virtual bool makedirs( const Path& path ); // Python function name
      virtual bool makedirs( const Path& path, mode_t mode );
      virtual bool mkdir( const Path& path );
      virtual bool mktree( const Path& path );
      virtual bool mktree( const Path& path, mode_t mode );

      // Delegate to full open
      virtual File* open( const Path& path );
      virtual File* open( const Path& path, uint32_t flags );
      virtual File* open( const Path& path, uint32_t flags, mode_t mode );

      // Recursive rmdir + unlink
      virtual bool rmtree( const Path& path );

      // Delegates to getattr
      virtual Stat* stat( const Path& path );

      // Delegates to open
      bool touch( const Path& path ); 

      // Delegate to setattr
      virtual bool
      utime
      (
        const Path& path,
        const Time& atime,
        const Time& mtime
      );

      virtual bool
      utime
      (
        const Path& path,
        const Time& atime,
        const Time& mtime,
        const Time& ctime
      );

      // yidl::runtime::Object
      Volume& inc_ref() { return Object::inc_ref( *this ); }
    };


#ifdef _WIN32
    class Win32AIOCB : public IOCB
    {
    public:
      Win32AIOCB();
      virtual ~Win32AIOCB() { }

      // LPOVERLAPPED_COMPLETION_ROUTINE
      static void __stdcall 
      OverlappedCompletionRoutine
      (
        unsigned long dwErrorCode,
        unsigned long dwNumberOfBytesTransferred,
        ::OVERLAPPED* lpOverlapped
      );

      // LPWSAOVERLAPPED_COMPLETION_ROUTINE
      static void __stdcall
      WSAOverlappedCompletionRoutine
      (
        unsigned long dwErrorCode,
        unsigned long dwNumberOfBytesTransferred,
        ::OVERLAPPED* lpOverlapped,
        unsigned long dwFlags
      );

      static Win32AIOCB* from_OVERLAPPED( ::OVERLAPPED* overlapped );
      operator ::OVERLAPPED*();

      virtual void onCompletion( unsigned long dwNumberOfBytesTransferred ) =0;
      virtual void onError( unsigned long dwErrorCode ) = 0;

    private:
      typedef struct 
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

        Win32AIOCB* this_;
      } OVERLAPPED;
      OVERLAPPED overlapped;
    };


    class Win32AIOQueue : public IOQueue
    {
    public:
      static Win32AIOQueue& create();

      bool associate( socket_t socket_ );
      bool associate( void* handle );

      bool 
      post // PostQueuedCompletionStatus
      ( 
        Win32AIOCB& win32_aiocb, // Takes ownership of win32_aiocb
        unsigned long dwNumberOfBytesTransferred = 0,
        unsigned long dwCompletionKey = 0
      );

      // yidl::runtime::RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( Win32AIOQueue, 3 );

    private:      
      Win32AIOQueue( void* hIoCompletionPort );
      ~Win32AIOQueue();

      void* hIoCompletionPort;

      class WorkerThread;
      vector<WorkerThread*> worker_threads;
    };
#endif
  };
};


#endif
