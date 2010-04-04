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

#ifdef YIELD_PLATFORM_HAVE_OPENSSL
#include <openssl/ssl.h>
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

#include <stack>

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

#define YIELD_PLATFORM_EXCEPTION_SUBCLASS( ClassName ) \
  class ClassName : public yield::platform::Exception \
  { \
  public: \
    ClassName() { }\
    ClassName( uint32_t error_code ) : Exception( error_code ) { } \
    ClassName( const char* error_message ) : Exception( error_message ) { } \
    ClassName( const string& error_message ) : Exception( error_message ) { } \
    ClassName( uint32_t error_code, const char* error_message ) \
      : Exception( error_code, error_message ) \
    { } \
    ClassName( uint32_t error_code, const string& error_message ) \
      : Exception( error_code, error_message ) \
    { } \
    ClassName( const ClassName& other ) : Exception( other ) { } \
  }; 

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

struct _SYSTEMTIME;
typedef _SYSTEMTIME SYSTEMTIME;

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
struct tm;
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

    class BIOQueue;
    class IOQueue;
    class Path;
    class SocketAddress;
    class Stat;
    class Time;
#ifdef _WIN32
    class Win32AIOQueue;
#endif
    using std::stack;
    using yidl::runtime::Buffer;
    using yidl::runtime::Map;
    using yidl::runtime::MarshallableObject;
    using yidl::runtime::Marshaller;
    using yidl::runtime::Object;
    using yidl::runtime::RTTIObject;
    using yidl::runtime::Sequence;
    using yidl::runtime::Unmarshaller;


    class Buffers : public Object, private vector<struct iovec>
    {
    public:
      Buffers();
      Buffers( Buffer& buffer );
      Buffers( const struct iovec* iov, uint32_t iov_size );
      Buffers( const Buffers& other );
      Buffers( const Buffers& head, const Buffers& tail );
      virtual ~Buffers();
      bool empty() const { return vector<struct iovec>::empty(); }
      void extend( const Buffers& other );
      size_t get( void* buf, size_t len );
      void insert( size_t i, Buffer& buffer );
      void insert( size_t i, Buffer* buffer ); // Steals this reference
      void insert( size_t i, char* buf ); // Copies
      void insert( size_t i, const char* buf );
      void insert( size_t i, const string& buf ); // Copies
      void insert( size_t i, void* buf, size_t len ); // Copies
      void insert( size_t i, const void* buf, size_t len );
      void insert( size_t i, const struct iovec& iov );
      Buffer& join();
      size_t join_size() const;
      struct iovec operator[]( int iov_i ) const;
      operator const struct iovec*();
      operator char*();
      operator unsigned char*();
      size_t position() const { return _position; }
      void position( size_t new_position );
      void push_back( Buffer& buffer );
      void push_back( Buffer* buffer ); // Steals this reference
      void push_back( char* buf ); // Copies
      void push_back( const char* buf );
      void push_back( const string& buf ); // Copies
      void push_back( void* buf, size_t len ); // Copies
      void push_back( const void* buf, size_t len );
      void push_back( const struct iovec& iov );
      uint32_t size() const;

      // Object
      Buffers& inc_ref() { return Object::inc_ref( *this ); }

    private:
      Buffer* get_Buffer( size_t iov_i ) const;
      char* get_iov_base( size_t iov_i ) const;
      size_t get_iov_len( size_t iov_i ) const;

      void init();

    private:
      struct iovec* finalized_iov;
      Buffer* joined_buffer;
      size_t _position;
    };


    class BufferedMarshaller : public Marshaller
    {
    public:
      BufferedMarshaller();
      virtual ~BufferedMarshaller();

      virtual Buffers& get_buffers() { return *buffers; }

      virtual void write( Buffer& value );
      virtual void write( Buffers& value );
      virtual void write( char* value, size_t value_len );
      virtual void write( const char* value, size_t value_len );

      // Marshaller
      virtual void write( const Key& key, Buffer& value );
      virtual void write( const Key& key, char* value, size_t value_len );
      virtual void write( const Key& key, const char* value, size_t );

    private:
      Buffers* buffers;
    };


    class Channel : public Object
    {
    public:
      virtual ~Channel() { }

      class AIOReadCallback : public virtual Object
      {
      public:
        // buffer is not a new reference; callees should inc_ref() their own
        // references as necessary
        virtual void onReadCompletion( Buffer& buffer, void* context ) = 0;
        virtual void onReadError( uint32_t error_code, void* context ) = 0;

        // Object
        AIOReadCallback& inc_ref() { return Object::inc_ref( *this ); }
      };

      virtual void 
      aio_read
      ( 
        Buffer& buffer, // Steals this reference
        AIOReadCallback& callback,
        void* callback_context = NULL
      );

      class AIOWriteCallback : public virtual Object
      {
      public:
        // Completed writes/writev are guaranteed to have transferred
        // the full buffer into the kernel
        // The bytes_written passed to the callback is only for logging
        // or statistics
        virtual void
        onWriteCompletion
        ( 
          size_t bytes_written, 
          void* context
        ) = 0;

        virtual void onWriteError( uint32_t error_code, void* context ) = 0;

        // Object
        AIOWriteCallback& inc_ref() { return Object::inc_ref( *this ); }
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

      virtual ssize_t read( Buffer& buffer );
      virtual ssize_t read( void* buf, size_t buflen ) = 0;

      // Unlike aio_write/aio_writev, write and writev are not guaranteed
      // to write the full buffer(s), even in non-blocking mode, i.e.
      // they have POSIX semantics.
      virtual ssize_t write( const Buffer& buffer );
      // All non-pure virtual *write* methods delegate to the pure write
      virtual ssize_t write( const void* buf, size_t buflen ) = 0;
      virtual ssize_t writev( Buffers& buffers );
      virtual ssize_t writev( const struct iovec* iov, uint32_t iovlen );
    };


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
      virtual const char* get_error_message() const throw();
      static uint32_t get_last_error();

      operator const char*() const throw() { return get_error_message(); }

      void set_error_code( uint32_t error_code );
      void set_error_message( const char* error_message );
      void set_error_message( const string& error_message );
      static void set_last_error( uint32_t error_code );

      // std::exception
      const char* what() const throw();

    private:
      uint32_t error_code;
      char* error_message;
    };


    class FDEventPoller : public Object
    {
    public:
#ifdef _WIN32
      typedef socket_t fd_t;
#endif

    public:
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

    public:
      virtual ~FDEventPoller() { }

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

    
    class File : public Channel
    {
    public:
      const static uint32_t ATTRIBUTES_DEFAULT = 0;
      const static uint32_t FLAGS_DEFAULT = O_RDONLY;
      const static mode_t MODE_DEFAULT = S_IREAD|S_IWRITE;

    public:
      File( fd_t fd ); // Takes ownership of the fd
      virtual ~File() { close(); }

      YIELD_PLATFORM_FILE_PROTOTYPES;

      virtual void
      aio_read
      (
        Buffer& buffer,
        uint64_t offset,
        AIOReadCallback& callback,
        void* callback_context = NULL
      );

      virtual void
      aio_write
      (
        Buffer& buffer,
        uint64_t offset,
        AIOWriteCallback& callback,
        void* callback_context = NULL
      );

      virtual void
      aio_writev
      (
        Buffers& buffers,
        uint64_t offset,
        AIOWriteCallback& callback,
        void* callback_context = NULL
      );

      virtual bool associate( IOQueue& io_queue );
      static bool close( fd_t fd );
      virtual size_t getpagesize();
      inline operator fd_t() const { return fd; }
      virtual ssize_t read( Buffer& buffer, uint64_t offset );
      virtual bool seek( uint64_t offset ); // SEEK_SET
      virtual bool seek( uint64_t offset, unsigned char whence );
      Stat* stat() { return getattr(); }
      virtual uint64_t tell();
      virtual ssize_t write( const Buffer& buffer, uint64_t offset );
      virtual ssize_t writev( Buffers& buffers, uint64_t offset );
      virtual ssize_t writev( const struct iovec*, uint32_t, uint64_t offset );

      // Channel
      // read/write write from/to the current file position
      virtual void aio_read( Buffer&, AIOReadCallback&, void* = NULL );
      virtual void aio_write( Buffer&, AIOWriteCallback&, void* = NULL );
      virtual void aio_writev( Buffers& buffers, AIOWriteCallback&, void* );
      virtual ssize_t read( Buffer& buffer );
      virtual ssize_t read( void* buf, size_t buflen );     
      virtual ssize_t write( const Buffer& buffer );
      virtual ssize_t write( const void* buf, size_t buflen );
      virtual ssize_t writev( Buffers& buffers );
      virtual ssize_t writev( const struct iovec* iov, uint32_t iovlen );

      // Object
      File& inc_ref() { return Object::inc_ref( *this ); }

    protected:
      File();      

    private:
      File( const File& ) { DebugBreak(); } // Prevent copying

      BIOQueue* get_bio_queue() const;
#ifdef _WIN32
      Win32AIOQueue* get_win32_aio_queue() const;
#endif

    private:
      fd_t fd;
      IOQueue* io_queue;

    private:
      template <class> class IOCB;
      class IOReadCB;
      class IOWriteCB;
      class IOWriteVCB;
      class BIOReadCB;
      class BIOWriteCB;
      class BIOWriteVCB;
#ifdef _WIN32
      class Win32AIOReadCB;
      class Win32AIOWriteCB;
#endif
    };


#ifndef htons
    // htons is a macro on OS X.
    static inline uint16_t htons( uint16_t x )
    {
#ifdef __BIG_ENDIAN__
      return x;
#else
      return ( x >> 8 ) | ( x << 8 );
#endif
    }
#endif

#ifndef htonl
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


    class iconv : public Object
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


    class IOCB : public Object
    {
    public:
      enum State 
      { 
        STATE_UNKNOWN,
        STATE_WANT_CONNECT, 
        STATE_WANT_READ, 
        STATE_WANT_WRITE, 
        STATE_COMPLETE, 
        STATE_ERROR 
      };

    public:
      virtual ~IOCB() { }

      State get_state() const { return state; }
      void set_state( State state ) { this->state = state; }

    protected:
      IOCB() 
        : state( STATE_UNKNOWN )
      { }

    private:
      State state;
    };


    class IOQueue : public RTTIObject
    {
    public:
      virtual ~IOQueue() { }

      // Object
      IOQueue& inc_ref() { return Object::inc_ref( *this ); }

    protected:
      IOQueue() { }      
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
      static BIOQueue* create();
      void submit( BIOCB& biocb ); // Takes ownership of biocb

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( BIOQueue, 0 );
      
    private:
      BIOQueue() { }

      class WorkerThread;
    };


    class Log : public Object
    {
    public:
      class Level
      {
      public:
        Level( const char* level );
        Level( const string& level );
        Level( uint8_t level );
        Level( const char* level_string, uint8_t level_uint8 );
        Level( const Level& other );

        inline operator const string&() const { return level_string; }
        inline operator const char*() const { return level_string.c_str(); }
        inline operator uint8_t() const { return level_uint8; }

        inline bool operator<( const Level& other ) const
        {
          return level_uint8 < other.level_uint8;
        }

        inline bool operator<=( const Level& other ) const
        {
          return level_uint8 <= other.level_uint8;
        }

        inline bool operator==( const Level& other ) const
        {
          return level_uint8 == other.level_uint8;
        }

        inline bool operator>( const Level& other ) const
        {
          return level_uint8 > other.level_uint8;
        }

        inline bool operator>=( const Level& other ) const
        {
          return level_uint8 >= other.level_uint8;
        }

      private:
        void init( const char* level );

      private:
        string level_string;
        uint8_t level_uint8;
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

    public:
      // Adapted from syslog levels
      static Level LOG_EMERG;
      static Level LOG_ALERT;
      static Level LOG_CRIT;
      static Level LOG_ERR;
      static Level LOG_WARNING;
      static Level LOG_INFO;
      static Level LOG_DEBUG;

    public:
      virtual ~Log() { }
      static Log& open( ostream&, const Level& level = LOG_ERR );

      static Log& 
      open
      ( 
        const Path& file_path, 
        const Level& level = LOG_ERR, 
        bool lazy_open = false 
      );

      const Level& get_level() const { return level; }
      Stream get_stream() { return Stream( inc_ref(), level ); }
      Stream get_stream( Level level ) { return Stream( inc_ref(), level ); }
      void set_level( const Level& level ) { this->level = level; }

      void write( const char* str, const Level& level );
      void write( const string& str, const Level& level );
      void write( const void* str, size_t str_len, const Level& level );
      void write( const unsigned char* str, size_t str_len, const Level& level );
      void write( const char* str, size_t str_len, const Level& level );

      // Object
      Log& inc_ref() { return Object::inc_ref( *this ); }

    protected:
      Log( const Level& level );

      virtual void write( const char* str, size_t str_len ) = 0;

    private:
      Level level;
    };


    class MemoryMappedFile : public Object
    {
    public:
      virtual ~MemoryMappedFile();

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
      ~NamedPipe() { }

      static NamedPipe*
      open
      (
        const Path& path,
        uint32_t flags = O_RDWR,
        mode_t mode = File::MODE_DEFAULT
      );

#ifdef _WIN32
      // Channel
      ssize_t read( void* buf, size_t buflen );
      ssize_t write( const void* buf, size_t buflen );
#endif

    private:
#ifdef WIN32
      NamedPipe( fd_t fd, bool connected );
#else
      NamedPipe( fd_t fd );
#endif      

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
    };


    class NBIOQueue : public IOQueue
    {
    public:
      ~NBIOQueue();

      static NBIOQueue* create( int16_t thread_count = 1 );

      uint16_t get_thread_count() const;
      void submit( NBIOCB& nbiocb ); // Takes ownership of nbiocb
      
      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( NBIOQueue, 2 );

    private:
      class Thread;
      NBIOQueue( const vector<Thread*>& worker_threads );      

    private:
      vector<Thread*> threads;
    };


    class NOPLock
    {
    public:
      inline bool acquire() { return true; }
      inline bool acquire( const Time& ) { return true; }
      inline void release() { }
      inline bool try_acquire() { return true; }      
    };


#ifndef ntohs
    static inline uint16_t ntohs( uint16_t x )
    {
#ifdef __BIG_ENDIAN__
      return x;
#else
      return ( x >> 8 ) | ( x << 8 );
#endif
    }
#endif

#ifndef ntohl
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


    // Modelled after Python's optparse.OptionParser class
    class OptionParser : public Object
    {
    public:
      class Option
      {
      public:
        Option
        ( 
          const string& option, 
          const string& help, 
          bool require_argument = true
        );

        Option( const string& option, bool require_argument = true );

        const string& get_help() const { return help; }
        bool get_require_argument() const { return require_argument; }

        operator const char*() const { return option.c_str(); }
        operator const string&() const { return option; }
        bool operator==( const string& option ) const;
        bool operator==( const char* option ) const;
        bool operator==( const Option& other ) const;      
        bool operator<( const Option& other ) const; // For sorting

      private:
        string help, option;
        bool require_argument;
      };


      class Options : public vector<Option>
      {
      public:
        void add
        ( 
          const string& option, 
          const string& help, 
          bool require_argument = true
        );

        void add( const string& option, bool require_argument = true );
        void add( const Option& option );
        void add( const Options& options );
      };


      class ParsedOption : public Option
      {
      public:
        ParsedOption( Option& option );
        ParsedOption( Option& option, const string& argument );

        const string& get_argument() const { return argument; }

      private:
        string argument;
      };

      typedef vector<ParsedOption> ParsedOptions;


      YIELD_PLATFORM_EXCEPTION_SUBCLASS( DuplicateOptionException );
      YIELD_PLATFORM_EXCEPTION_SUBCLASS( InvalidValueException );
      YIELD_PLATFORM_EXCEPTION_SUBCLASS( MissingValueException );
      YIELD_PLATFORM_EXCEPTION_SUBCLASS( UnexpectedValueException );
      YIELD_PLATFORM_EXCEPTION_SUBCLASS( UnregisteredOptionException );

    public:
      void 
      add_option
      ( 
        const string& option, 
        const string& help, 
        bool require_argument = true
      );

      void add_option( const string& option, bool require_argument = true );
      void add_option( const Option& option );
      void add_options( const Options& options );

      void 
      parse_args
      ( 
        int argc, 
        char** argv, 
        ParsedOptions& out_parsed_options,
        vector<string>& out_positional_arguments
      );

      string usage();

    private:
      Options options;
    };


    class Path : public Object
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


#ifdef YIELD_PLATFORM_HAVE_PERFORMANCE_COUNTERS
    class PerformanceCounterSet : public Object
    {
    public:
      enum Event
      {
        EVENT_L1_DCM, // L1 data cache miss
        EVENT_L2_DCM, // L2 data cache miss
        EVENT_L2_ICM // L2 instruction cache miss
      };

    public:
      ~PerformanceCounterSet();

      static PerformanceCounterSet* create();

      bool addEvent( Event event );
      bool addEvent( const char* name );
      void startCounting();
      void stopCounting( uint64_t* counts );

    private:
#if defined(__sun)
      PerformanceCounterSet( cpc_t* cpc, cpc_set_t* cpc_set );
#elif defined(YIELD_PLATFORM_HAVE_PAPI)
      PerformanceCounterSet( int papi_eventset );
#endif

    private:
#if defined(__sun)
      cpc_t* cpc; cpc_set_t* cpc_set;
      vector<int> event_indices;
      cpc_buf_t* start_cpc_buf;
#elif defined(YIELD_PLATFORM_HAVE_PAPI)
      int papi_eventset;
#endif
    };
#endif


    class Pipe : public Channel
    {
    public:
      ~Pipe();

      static Pipe& create(); 

      bool close();
      fd_t get_read_end() const { return ends[0]; }
      fd_t get_write_end() const { return ends[1]; }
      fd_t operator[]( size_t n ) const { return ends[n]; }
      bool set_read_blocking_mode( bool blocking );
      bool set_write_blocking_mode( bool blocking );

      // Channel
      ssize_t read( Buffer& buffer );
      ssize_t read( void* buf, size_t buflen );      
      ssize_t write( const Buffer& buffer );
      ssize_t write( const void* buf, size_t buflen );

    private:
      Pipe( fd_t ends[2] );      

    private:
      fd_t ends[2];
    };


    class Process : public Object
    {
    public:
      ~Process();

      static Process& create( const Path& executable_file_path );
      static Process& create( int argc, char** argv );
      static Process& create( const vector<char*>& argv );

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

    private:
#ifdef _WIN32
      void *hChildProcess, *hChildThread;
#else
      int child_pid;
#endif
      Pipe *child_stdin, *child_stdout, *child_stderr;
    };


    class ProcessorSet : public Object
    {
    public:
      ProcessorSet();
      ProcessorSet( uint32_t from_mask );
      ~ProcessorSet();

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

    private:
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


    class SharedLibrary : public Object
    {
    public:
#ifdef _WIN32
      const static wchar_t* SHLIBSUFFIX;
#else
      const static char* SHLIBSUFFIX;
#endif

    public:
      ~SharedLibrary();

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
      
    private:
      void* handle;
    };


    class Socket : public Channel
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

    public:
      Socket( int domain, int type, int protocol, socket_t socket_ );
      virtual ~Socket();      

      typedef AIOReadCallback AIORecvCallback;

      virtual void
      aio_recv
      ( 
        Buffer& buffer, // Steals this reference
        int flags,
        AIORecvCallback& callback,
        void* callback_context = NULL
      );

      typedef AIOWriteCallback AIOSendCallback;

      virtual void 
      aio_send
      (
        Buffer& buffer, // Steals this reference
        int flags,
        AIOSendCallback& callback,
        void* callback_context = NULL
      );

      virtual void
      aio_sendmsg
      (
        Buffers& buffers, // Steals this reference
        int flags,
        AIOSendCallback& callback,
        void* callback_context = NULL
      );

      virtual bool associate( IOQueue& io_queue );
      virtual bool bind( const SocketAddress& to_sockaddr );
      virtual bool close();
      static bool close( socket_t );
      virtual bool connect( const SocketAddress& peername );
      static Socket* create( int domain, int type, int protocol );
      virtual bool get_blocking_mode() const;
      int get_domain() const { return domain; }
      static string getfqdn();
      static string gethostname();
      SocketAddress* getpeername() const;
      static SocketAddress* getpeername( socket_t );
      int get_protocol() const { return protocol; }
      SocketAddress* getsockname() const;
      static SocketAddress* getsockname( socket_t );
      int get_type() const { return type; }
      bool operator==( const Socket& other ) const;
      inline operator socket_t() const { return socket_; }
      bool recreate();
      bool recreate( int domain );
      ssize_t recv( Buffer& buffer, int flags = 0 );
      virtual ssize_t recv( void* buf, size_t buflen, int flags = 0 );
      ssize_t send( const Buffer& buffer, int flags = 0 );
      virtual ssize_t send( const void* buf, size_t len, int flags = 0 );
      ssize_t sendmsg( Buffers& buffers, int flags = 0 );
      virtual ssize_t sendmsg( const struct iovec*, uint32_t, int flags = 0 );
      virtual bool set_blocking_mode( bool blocking );
      static bool set_blocking_mode( bool blocking, socket_t );
      virtual bool setsockopt( Option option, bool onoff );
      virtual bool shutdown( bool shut_rd = true, bool shut_wr = true );
      virtual bool want_recv() const;
      virtual bool want_send() const;

      // Object
      Socket& inc_ref() { return Object::inc_ref( *this ); }

      // Channel
      void aio_read( Buffer&, AIOReadCallback&, void* = NULL );
      void aio_write( Buffer&, AIOWriteCallback&, void* = NULL );
      void aio_writev( Buffers&, AIOWriteCallback&, void* = NULL );
      ssize_t read( Buffer& buffer );
      ssize_t read( void* buf, size_t buflen );
      ssize_t write( const Buffer& buffer );
      ssize_t write( const void* buf, size_t buflen );
      ssize_t writev( Buffers& buffers );
      ssize_t writev( const struct iovec* iov, uint32_t iovlen );

    protected:
      static socket_t create( int* domain, int type, int protocol );

      BIOQueue* get_bio_queue() const; // Tries to cast, can return NULL
      IOQueue* get_io_queue() const { return io_queue; }
      NBIOQueue* get_nbio_queue() const; // Tries to cast, can return NULL
      static int get_platform_recv_flags( int flags );
      static int get_platform_send_flags( int flags );
#ifdef _WIN32
      Win32AIOQueue* get_win32_aio_queue() const;
#endif
#ifdef _WIN64
      void iovecs_to_wsabufs( const iovec*, vector<iovec64>& );
#endif
      void set_io_queue( IOQueue& io_queue );

    protected:
      template <class AIOCallbackType>
      class IOCB
      {
      public:
        AIOCallbackType& get_callback() const { return callback; }
        void* get_callback_context() const { return callback_context; }

      protected:
        IOCB( AIOCallbackType& callback, void* callback_context )
          : callback( callback.inc_ref() ),
            callback_context( callback_context )
        { }

        virtual ~IOCB()
        {
          AIOCallbackType::dec_ref( callback );
        }

      private:
        AIOCallbackType& callback;
        void* callback_context;
      };


      class IORecvCB : public IOCB<AIORecvCallback>
      {
      public:
        Buffer& get_buffer() const { return buffer; }
        int get_flags() const { return flags; }
        Socket& get_socket() const { return socket_; }

        void onRecvCompletion();
        void onRecvError();
        void onRecvError( uint32_t error_code );

      protected:
        IORecvCB
        ( 
          Buffer& buffer, 
          AIORecvCallback& callback, 
          void* callback_context,
          int flags, 
          Socket& socket_
        );

        virtual ~IORecvCB();

        bool execute( bool blocking_mode );

      private:
        Buffer& buffer;
        int flags;
        Socket& socket_;
      };


      class IOSendCB : public IOCB<AIOSendCallback>
      {
      public:
        const Buffer& get_buffer() const { return buffer; }
        int get_flags() const { return flags; }
        Socket& get_socket() const { return socket_; }

        void onSendCompletion();
        void onSendError();
        void onSendError( uint32_t error_code );

      protected:
        IOSendCB
        ( 
          Buffer& buffer,
          AIOSendCallback& callback, 
          void* callback_context,
          int flags, 
          Socket& socket_
        );

        virtual ~IOSendCB();

        bool execute( bool blocking_mode );

      private:
        Buffer& buffer;
        int flags;
        size_t partial_send_len;
        Socket& socket_;
      };


      class IOSendMsgCB : public IOCB<AIOSendCallback>
      {
      public:
        Buffers& get_buffers() const { return buffers; }
        size_t get_buffers_len() const { return buffers_len; }
        int get_flags() const { return flags; }
        Socket& get_socket() const { return socket_; }

        void onSendMsgCompletion();
        void onSendMsgError();
        void onSendMsgError( uint32_t error_code );

      protected:
        IOSendMsgCB
        ( 
          Buffers& buffers,
          AIOSendCallback& callback,
          void* callback_context,
          int flags, 
          Socket& socket_
        );

        virtual ~IOSendMsgCB();

        bool execute( bool blocking_mode );

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

      bool blocking_mode;
      IOQueue* io_queue;

    private:
      class BIORecvCB;
      class BIOSendCB;
      class BIOSendMsgCB;
      class NBIORecvCB;
      class NBIOSendCB;
      class NBIOSendMsgCB;
    };


    class SocketAddress : public Object // immutable
    {
    public:
      SocketAddress(); // INADDR_ANY
      SocketAddress( uint16_t port ); // INADDR_ANY, port
      SocketAddress( struct addrinfo& ); // Takes ownership
      SocketAddress( const struct sockaddr_storage& ); // Copies
      ~SocketAddress();

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
      operator string() const;
      bool operator==( const SocketAddress& ) const;
      bool operator!=( const SocketAddress& other ) const;

      // Object
      SocketAddress& inc_ref() { return Object::inc_ref( *this ); }

    private:
      SocketAddress( const SocketAddress& ) 
      { 
        DebugBreak(); // Prevent copying
      }      

    private:
      // Linked sockaddr's obtained from getaddrinfo(3)
      // Will be NULL if _sockaddr_storage is used
      struct addrinfo* addrinfo_list;

      // A single sockaddr passed in the constructor and copied
      // Will be NULL if addrinfo_list is used
      struct sockaddr_storage* _sockaddr_storage;

      static struct addrinfo* getaddrinfo( const char* hostname, uint16_t port );
    };


    class SocketPair : public Object
    {
    public:
      ~SocketPair();

      static SocketPair& create();

      Socket& first() const { return first_socket; }
      Socket& second() const { return second_socket; }

    private:
      SocketPair( Socket& first_socket, Socket& second_socket );      

    private:
      Socket &first_socket, &second_socket;
    };


#ifdef YIELD_PLATFORM_HAVE_OPENSSL
    class SSLContext : public Object
    {
    public:
      ~SSLContext();

      static SSLContext&
      create
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method = SSLv23_client_method()
      );

      static SSLContext&
      create
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method,
        const Path& pem_certificate_file_path,
        const Path& pem_private_key_file_path,
        const string& pem_private_key_passphrase
      );

      static SSLContext&
      create
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method,
        const string& pem_certificate_str,
        const string& pem_private_key_str,
        const string& pem_private_key_passphrase
      );

      static SSLContext&
      create
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method,
        const Path& pkcs12_file_path,
        const string& pkcs12_passphrase
      );

      operator SSL_CTX*() const { return ctx; }

      // Object
      SSLContext& inc_ref() { return Object::inc_ref( *this ); }

  private:
      SSLContext( SSL_CTX* ctx );

      static SSL_CTX* createSSL_CTX
      (
#if OPENSSL_VERSION_NUMBER >= 0x10000000L
        const
#endif
        SSL_METHOD* method
      );

      SSL_CTX* ctx;
    };
#endif


    template <size_t Capacity>
    class StackBuffer : public Buffer
    {
    public:
      StackBuffer() 
      { 
        _size = 0; 
      }

      StackBuffer( const void* buf )
      {
        memcpy_s( buffer, Capacity, buf, Capacity );
        _size = 0;
      }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StackBuffer, 2 );

      // Buffer
      size_t capacity() const { return Capacity; }
      operator void*() const { return const_cast<uint8_t*>( &buffer[0] ); }
      void resize( size_t n ) { _size = ( n <= Capacity ? n : Capacity ); }
      size_t size() const { return _size; }

    private:
      uint8_t buffer[Capacity];
      size_t _size;
    };


    class StreamSocket : public Socket
    {
    public:
      static int TYPE; // SOCK_STREAM

    public:
      virtual StreamSocket* accept();

      class AIOAcceptCallback : public virtual Object
      {
      public:
        // accepted_stream_socket is not a new reference; 
        // callees should inc_ref() their own references as necessary
        virtual void 
        onAcceptCompletion
        ( 
          StreamSocket& accepted_stream_socket,
          void* context,
          Buffer* recv_buffer
        ) = 0;

        virtual void onAcceptError( uint32_t error_code, void* context ) = 0;

        // Object
        AIOAcceptCallback& inc_ref() { return Object::inc_ref( *this ); }
      };

      virtual void 
      aio_accept
      ( 
        AIOAcceptCallback& callback,
        void* callback_context = NULL,
        Buffer* recv_buffer = NULL // Steals this reference
        // recv_buffer must be at least 88 bytes long if it's not NULL
        // ( sizeof( sockaddr_in6 ) + 16 ) * 2 = 88 to store the 
        // peername on Win32 (position() will be set at 89 on the callback).
      );

      class AIOConnectCallback : public virtual Object
      {
      public:
        virtual void
        onConnectCompletion
        ( 
          size_t bytes_sent, 
          void* context
        ) = 0;

        virtual void onConnectError( uint32_t error_code, void* context ) = 0;

        // Object
        AIOConnectCallback& inc_ref() { return Object::inc_ref( *this ); }
      };

      virtual void 
      aio_connect
      ( 
        SocketAddress& peername,
        AIOConnectCallback& callback,
        void* callback_context = NULL,
        Buffer* send_buffer = NULL // Steals this reference
      );

      static StreamSocket* create( int domain, int protocol );

      virtual StreamSocket* dup();
      virtual bool listen();
      virtual bool want_accept() const;
      virtual bool want_connect() const;

      // Object
      StreamSocket& inc_ref() { return Object::inc_ref( *this ); }

      // Socket
      virtual void
      aio_recv
      ( 
        Buffer& buffer, // Steals this reference
        int flags,
        AIORecvCallback& callback,
        void* callback_context = NULL
      );

      virtual void 
      aio_send
      (
        Buffer& buffer, // Steals this reference
        int flags,
        AIOSendCallback& callback,
        void* callback_context = NULL
      );

      virtual void
      aio_sendmsg
      (
        Buffers& buffers, // Steals this reference
        int flags,
        AIOSendCallback& callback,
        void* callback_context = NULL
      );

      virtual bool associate( IOQueue& io_queue );

    protected:
      StreamSocket( int domain, int protocol, socket_t socket_ );

      virtual StreamSocket* dup2( socket_t ); // Called by accept

      // Called by dup2 in subclasses
      template <class StreamSocketType>
      StreamSocketType* dup2( StreamSocketType* stream_socket )
      {
        if ( stream_socket != NULL )
        {
          if ( get_io_queue() != NULL )
          {
            if ( stream_socket->associate( *get_io_queue() ) )
              return stream_socket;
            else
            {
              StreamSocket::dec_ref( *stream_socket );
              return NULL;
            }
          }
          else
            return stream_socket;
        }
        else
          return NULL;
      }

    private:
#ifdef _WIN32
      static void *lpfnAcceptEx, *lpfnConnectEx;
#endif

    private:
      class BIOAcceptCB;
      class BIOConnectCB;
      class IOAcceptCB;
      class IOConnectCB;
      class NBIOAcceptCB;
      class NBIOConnectCB;
#ifdef _WIN32
      class Win32AIOAcceptCB;
      class Win32AIOConnectCB;
      class Win32AIORecvCB;
      class Win32AIOSendCB;
      class Win32AIOSendMsgCB;
#endif
    };


    class StringBuffer : public Buffer, public string
    {
    public:
      StringBuffer() { }
      StringBuffer( size_t capacity );
      StringBuffer( const string& buf );
      StringBuffer( const char* buf );
      StringBuffer( const char* buf, size_t len );
      StringBuffer( Buffer& buf );

      // Buffer
      operator void*() const { return const_cast<char*>( data() ); }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StringBuffer, 3 );

      // Buffer
      size_t capacity() const { return string::capacity(); }
      bool empty() const { return string::empty(); }
      size_t get( void* buf, size_t len );
      size_t put( char buf, size_t repeat_count );
      size_t put( const void* buf, size_t len );
      void resize( size_t n );
      size_t size() const { return string::size(); }
    };


    class Time
    {
    public:
      static Time INVALID_TIME;
      const static uint64_t NS_IN_US = 1000ULL;
      const static uint64_t NS_IN_MS = 1000000ULL;
      const static uint64_t NS_IN_S = 1000000000ULL;

    public:
      Time(); // Current time
      Time( uint64_t unix_time_ns ) : unix_time_ns( unix_time_ns ) { }
      Time( double unix_time_s );
      Time( const struct timeval& );
#ifdef _WIN32
      Time( const FILETIME& );
      Time( const FILETIME* );
      Time( const SYSTEMTIME&, bool local = true );
#else
      Time( const struct timespec& );
      Time( const struct tm&, bool local = true );
#endif      
      Time( const Time& other ) : unix_time_ns( other.unix_time_ns ) { }

      Time
      (
        int tm_sec, // seconds after the minute	0-61*
        int tm_min, // minutes after the hour	0-59
        int tm_hour, //	hours since midnight 0-23
        int tm_mday, //	day of the month 1-31
        int tm_mon, // months since January	0-11
        int tm_year, //	years since 1900
        bool local = true
      );

#ifdef _WIN32
      SYSTEMTIME as_local_SYSTEMTIME() const;
      SYSTEMTIME as_utc_SYSTEMTIME() const;
#else
      struct tm as_local_struct_tm() const;
      struct tm as_utc_struct_tm() const;
#endif

      inline double as_unix_time_ms() const
      {
        return static_cast<double>( unix_time_ns ) / NS_IN_MS;
      }

      inline uint64_t as_unix_time_ns() const { return unix_time_ns; }

      inline double as_unix_time_s() const
      {
        return static_cast<double>( unix_time_ns ) / NS_IN_S;
      }

      inline double as_unix_time_us() const
      {
        return static_cast<double>( unix_time_ns ) / NS_IN_US;
      }

      inline operator uint64_t() const { return as_unix_time_ns(); }
      inline operator double() const { return as_unix_time_s(); }
      operator struct timeval() const;
#ifdef _WIN32
      operator FILETIME() const;
      operator SYSTEMTIME() const; // as_local_SYSTEMTIME
#else
      operator struct timespec() const;
      operator struct tm() const; // as_local_struct_tm
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
#ifdef _WIN32
      void init( const FILETIME& );
      void init( const SYSTEMTIME&, bool local );
#else
      void init( struct tm&, bool local );
#endif

    private:
      // The time is stored internally as a Unix epoch time, i.e.
      // nanoseconds since January 1, 1970
      uint64_t unix_time_ns;
    };

    static inline ostream& operator<<( ostream& os, const Time& time )
    {
      os << static_cast<string>( time );
      return os;
    }


    class TCPSocket : public StreamSocket
    {
    public:
      static int DOMAIN_DEFAULT; // AF_INET6
      const static Option OPTION_TCP_NODELAY = 4;
      static int PROTOCOL; // IPPROTO_TCP

    public:
      virtual ~TCPSocket() { }

      static TCPSocket* create( int domain = DOMAIN_DEFAULT );
      virtual TCPSocket* dup();

      // Object
      TCPSocket& inc_ref() { return Object::inc_ref( *this ); }

      // Socket
      virtual bool setsockopt( Option option, bool onoff );

      // StreamSocket
      class AIOAcceptCallback : public StreamSocket::AIOAcceptCallback
      {
      public:
        virtual void 
        onAcceptCompletion
        ( 
          TCPSocket& accepted_tcp_socket,
          void* context,
          Buffer* recv_buffer
        ) = 0;

        // StreamSocket::AIOAcceptCallback
        void 
        onAcceptCompletion
        ( 
          StreamSocket& accepted_stream_socket,
          void* context,
          Buffer* recv_buffer
        )
        {
          onAcceptCompletion
          (
            static_cast<TCPSocket&>( accepted_stream_socket ),
            context,
            recv_buffer
          );
        }
      };

    protected:
      TCPSocket( int domain, socket_t );

      // StreamSocket
      virtual StreamSocket* dup2( socket_t );
    };


#ifdef YIELD_PLATFORM_HAVE_OPENSSL
    class SSLSocket : public TCPSocket
    {
    public:
      virtual ~SSLSocket();

      static SSLSocket* create( SSLContext& ); // Steals this ref
      static SSLSocket* create( int domain, SSLContext& ); // Steals this ref
      virtual SSLSocket* dup();
      operator SSL*() const { return ssl; }

      // Socket
      // Will only associate with BIO or NBIO queues
      virtual bool associate( IOQueue& io_queue );
      virtual bool connect( const SocketAddress& peername );
      virtual ssize_t recv( void* buf, size_t buflen, int );
      virtual ssize_t send( const void* buf, size_t buflen, int );
      virtual ssize_t sendmsg( const struct iovec* iov, uint32_t iovlen, int );
      virtual bool want_send() const;
      virtual bool want_recv() const;
      
      // StreamSocket
      class AIOAcceptCallback : public StreamSocket::AIOAcceptCallback
      {
      public:
        virtual void
        onAcceptCompletion
        ( 
          SSLSocket& accepted_ssl_socket,
          void* context,
          Buffer* recv_buffer
        ) = 0;

        // StreamSocket::AIOAcceptCallback
        void 
        onAcceptCompletion
        ( 
          StreamSocket& accepted_stream_socket,
          void* context,
          Buffer* recv_buffer
        )
        {
          onAcceptCompletion
          (
            static_cast<SSLSocket&>( accepted_stream_socket ),
            context,
            recv_buffer
          );
        }
      };

      virtual bool listen(); 
      virtual bool shutdown();

    protected:
      SSLSocket( int domain, socket_t, SSL*, SSLContext& );

      SSLContext& get_ssl_context() const { return ssl_context; }
      
    private:
      // StreamSocket
      virtual StreamSocket* dup2( socket_t );

    private:
      SSL* ssl;
      SSLContext& ssl_context;
    };
#endif


    class Thread : public Object
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


    class TimerQueue : public Object
    {
#ifndef _WIN32
    private:
      class Thread;
#endif

    public:
      class Timer : public Object
      {
      public:
        Timer( const Time& timeout );
        Timer( const Time& timeout, const Time& period );
        virtual ~Timer();

        void delete_();
        const Time& get_period() const { return period; }
        const Time& get_timeout() const { return timeout; }
        virtual void fire() = 0;

        // Object
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

    public:
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
      static int DOMAIN_DEFAULT; // AF_INET6
      static int PROTOCOL; // IPPROTO_UDP
      static int TYPE; // SOCK_DGRAM

    public:
      virtual ~UDPSocket() { }

      class AIORecvFromCallback : public virtual Object
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

        // Object
        AIORecvFromCallback& inc_ref() { return Object::inc_ref( *this ); }
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

      static UDPSocket* create( int domain = DOMAIN_DEFAULT );

      ssize_t
      recvfrom
      ( 
        Buffer& buffer,
        struct sockaddr_storage& peername 
      )
      {
        return recvfrom( buffer, 0, peername );
      }

      ssize_t recvfrom
      (
        Buffer& buffer,
        int flags,
        struct sockaddr_storage& peername
      );

      ssize_t
      recvfrom
      (
        void* buf,
        size_t buflen,
        struct sockaddr_storage& peername
      )
      {
        return recvfrom( buf, buflen, 0, peername );
      }

      virtual ssize_t
      recvfrom
      (
        void* buf,
        size_t buflen,
        int flags,
        struct sockaddr_storage& peername
      );

      ssize_t
      sendmsg
      (
        Buffers& buffers,
        const SocketAddress& peername,
        int flags = 0
      )
      {
        return sendmsg( buffers, buffers.size(), peername, flags );
      }

      // sendmsg analog to sendto
      virtual ssize_t
      sendmsg
      (
        const struct iovec* iov,
        uint32_t iovlen,
        const SocketAddress& peername,
        int flags = 0
      );

      ssize_t 
      sendto
      ( 
        const Buffer& buffer, 
        const SocketAddress& peername 
      )
      {
        return sendto( buffer, 0, peername );
      }

      ssize_t 
      sendto
      (
        const Buffer& buffer,
        int flags,
        const SocketAddress& peername
      )
      {
        return sendto( buffer, buffer.size(), flags, peername );
      }

      ssize_t sendto
      (
        const void* buf,
        size_t buflen,
        const SocketAddress& peername
      )
      {
        return sendto( buf, buflen, 0, peername );
      }

      virtual ssize_t
      sendto
      (
        const void* buf,
        size_t buflen,
        int flags,
        const SocketAddress& peername
      );

      // Object
      UDPSocket& inc_ref() { return Object::inc_ref( *this ); }

    protected:
      UDPSocket( int domain, socket_t );

    private:
      class BIORecvFromCB;
      class IORecvFromCB;
      class NBIORecvFromCB;
#ifdef _WIN32
      class Win32AIORecvFromCB;
#endif
    };


    class UUID : public Object
    {
    public:
      UUID();
      UUID( const string& uuid_from_string );
      ~UUID();

      bool operator==( const UUID& ) const;
      operator string() const;

    private:
#if defined(_WIN32)
      void* win32_uuid;
#elif defined(YIELD_PLATFORM_HAVE_LINUX_LIBUUID)
      void* linux_libuuid_uuid;
#elif defined(__sun)
      void* sun_uuid;
#else
      char generic_uuid[256];
#endif
    };


    class Directory : public Object
    {
    public:
      class Entry : public Object
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

    public:
      virtual ~Directory();

      YIELD_PLATFORM_DIRECTORY_PROTOTYPES;

      // Object
      Directory& inc_ref() { return Object::inc_ref( *this ); }

    protected:
      Directory();      

    private:
      friend class Volume;
#ifdef _WIN32
      Directory( void* hDirectory, const WIN32_FIND_DATA& first_find_data );
#else
      Directory( void* dirp );
#endif

    private:
#ifdef _WIN32
      void* hDirectory;
      WIN32_FIND_DATA* first_find_data;
#else
      void* dirp;
#endif
    };


    class Stat : public Object
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

      // Object
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


    class Volume : public Object
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

    public:
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

      // Object
      Volume& inc_ref() { return Object::inc_ref( *this ); }
    };


#ifdef _WIN32
    class Win32AIOCB : public IOCB
    {
    public:
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

    protected:
      Win32AIOCB();
      Win32AIOCB( uint64_t offset );

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
      ~Win32AIOQueue();

      bool associate( socket_t socket_ );
      bool associate( void* handle );

      static Win32AIOQueue* create( int16_t thread_count = 1 );

      uint16_t get_thread_count() const;

      bool 
      post // PostQueuedCompletionStatus
      ( 
        Win32AIOCB& win32_aiocb, // Takes ownership of win32_aiocb
        unsigned long dwNumberOfBytesTransferred = 0,
        unsigned long dwCompletionKey = 0
      );

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( Win32AIOQueue, 3 );

    private:      
      class Thread;
      Win32AIOQueue( void* hIoCompletionPort, const vector<Thread*>& threads );      

    private:
      void* hIoCompletionPort;
      vector<Thread*> threads;
    };
#endif


    class XDRMarshaller : public BufferedMarshaller
    {
    public:
      XDRMarshaller();
      virtual ~XDRMarshaller();

      void write( bool value );
      void write( double value );
      void write( float value );
      void write( int32_t value );
      void write( int64_t value );
      void write( const Map& value );
      void write( const Sequence& value );
      void write( const MarshallableObject& value );
      void write( uint32_t value );
      void write( uint64_t value );

      // Marshaller
      void write( const Key& key, bool value );
      void write( const Key& key, Buffer& value );
      void write( const Key& key, double value );
      void write( const Key& key, float value );
      void write( const Key& key, int32_t value );
      void write( const Key& key, int64_t value );
      void write( const Key& key, const Map& value );
      void write( const Key& key, const Sequence& value );
      void write( const Key& key, const MarshallableObject& value );
      void write( const Key& key, char* value, size_t value_len );
      void write( const Key& key, const char* value, size_t value_len );
      void write( const Key& key, uint32_t value );
      void write( const Key& key, uint64_t value );

      // BufferedMarshaller
      Buffers& get_buffers();
      void write( Buffer& value );
      void write( Buffers& value );
      void write( char* value, size_t value_len );
      void write( const char* value, size_t value_len );

    private:
      void write( const Key& key );
      void write( const void* value, size_t value_len );

    private:
      stack<bool> in_map_stack;
      StringBuffer* scratch_buffer;
    };


    class XDRUnmarshaller : public Unmarshaller
    {
    public:
      XDRUnmarshaller( Buffer& buffer );
      ~XDRUnmarshaller();

      bool read_bool() { return read_int32() == 1; }
      void read( Buffer& value );
      Buffer* read_buffer();
      double read_double() { double value; read( value ); return value; }
      void read( double& value );
      float read_float();
      int32_t read_int32();
      int64_t read_int64() { int64_t value; read( value ); return value; }
      void read( int64_t& value );
      void read( Map& value );
      void read( MarshallableObject& value );
      void read( Sequence& value );
      void read( string& value );
      uint32_t read_uint32() { return static_cast<uint32_t>( read_int32() ); }
      void read( uint64_t& value ) { read( reinterpret_cast<int64_t&>( value ) ); }

      // Unmarshaller
      bool read_bool( const Key& ) { return read_bool(); }
      void read( const Key&, Buffer& value ) { read( value ); }
      Buffer* read_buffer( const Key& ) { return read_buffer(); }
      void read( const Key&, double& value ) { read( value ); }
      float read_float( const Key& ) { return read_float(); }
      int32_t read_int32( const Key& ) { return read_int32(); }
      void read( const Key&, int64_t& value ) { read( value ); }
      Key* read( Key::Type key_type );
      void read( const Key&, Map& value ) { read( value ); }
      void read( const Key&, MarshallableObject& value ) { read( value ); }
      void read( const Key&, Sequence& value ) { read( value ); }
      void read( const Key&, string& value ) { read( value ); }

    private:
      void read( void* buffer, size_t buffer_len );

    private:
      Buffer& buffer;
    };
  };
};


#endif
