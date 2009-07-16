// Copyright 2003-2008 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _YIELD_PLATFORM_H_
#define _YIELD_PLATFORM_H_

#include "yield/base.h"

#ifdef _WIN32
#include <hash_map>
typedef int ssize_t;
extern "C"
{
  __declspec( dllimport ) void __stdcall DebugBreak();
}
#else
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
#include <aio.h>
#endif
#include <unistd.h>
#endif
#ifdef __sun
#include <libcpc.h>
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
#endif
#endif

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
  virtual ssize_t read( void* buffer, size_t buffer_len, uint64_t offset ); \
  virtual bool removexattr( const std::string& name ); \
  virtual bool setxattr( const std::string& name, const std::string& value, int flags ); \
  virtual bool sync(); \
  virtual bool truncate( uint64_t offset ); \
  virtual ssize_t write( const void* buffer, size_t buffer_len, uint64_t offset ); \
  virtual ssize_t writev( const iovec* buffers, uint32_t buffers_count, uint64_t offset );

#define YIELD_STRING_HASH_NEXT( c, hash ) hash = hash ^ ( ( hash << 5 ) + ( hash >> 2 ) + c )

#define YIELD_VOLUME_PROTOTYPES \
    virtual bool access( const YIELD::Path& path, int amode ); \
    virtual bool chmod( const YIELD::Path& path, mode_t mode ); \
    virtual bool chown( const YIELD::Path& path, int32_t tag, int32_t gid ); \
    virtual YIELD::auto_Object<YIELD::Stat> getattr( const YIELD::Path& path ); \
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
    virtual bool statvfs( const YIELD::Path& path, struct statvfs* ); \
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
  class Path;
  class Stat;


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


  template <class ParentType>
  class AIOControlBlock : public ParentType
  {
  public:
    AIOControlBlock()
    {
#if defined(_WIN32) || defined(YIELD_HAVE_POSIX_FILE_AIO)
      memset( &aiocb_, 0, sizeof( aiocb_ ) );
      aiocb_.this_ = this;
#endif
      complete = false;
    }

    inline bool isComplete() const { return complete; }
    virtual void onCompletion( size_t ) { complete = true; }
#if defined(_WIN32)
    operator OVERLAPPED*() { return reinterpret_cast<OVERLAPPED*>( &aiocb_ ); }
#elif defined(YIELD_HAVE_POSIX_FILE_AIO)
    operator ::aiocb*() { return &aiocb_; }
#endif

  protected:
    virtual ~AIOControlBlock() { }

  private:
    friend class IOCompletionPort;

#if defined(_WIN32) || defined(YIELD_HAVE_POSIX_FILE_AIO)
    struct aiocb : ::aiocb
    {
      AIOControlBlock* this_;
    } aiocb_;
#endif

    bool complete;
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

    static YIELD::auto_Object<File> open( const Path& path ) { return open( path, DEFAULT_FLAGS, DEFAULT_MODE ); }
    static YIELD::auto_Object<File> open( const Path& path, uint32_t flags ) { return open( path, flags, DEFAULT_MODE ); }
    static YIELD::auto_Object<File> open( const Path& path, uint32_t flags, mode_t mode ) { return open( path, flags, mode, DEFAULT_ATTRIBUTES ); }
    static YIELD::auto_Object<File> open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes );

    virtual uint64_t get_size();
#ifdef _WIN32
    operator void*() const { return fd; }
#else
    operator int() const { return fd; }
#endif    
    virtual ssize_t read( auto_Buffer buffer ); // Reads from the current file pointer
    virtual ssize_t read( void* buffer, size_t buffer_len ); // Reads from the current file pointer
    virtual bool seek( uint64_t offset ); // Seeks from the beginning of the file
    virtual bool seek( uint64_t offset, unsigned char whence );
    virtual YIELD::auto_Object<Stat> stat() { return getattr(); }
    virtual ssize_t write( auto_Buffer buffer );
    virtual ssize_t write( const void* buffer, size_t buffer_len ); // Writes from the current position
    virtual ssize_t writev( const iovec* buffers, uint32_t buffers_count ); // Writes from the current file pointer

    YIELD_FILE_PROTOTYPES;

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

  typedef YIELD::auto_Object<File> auto_File;


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

      Stream( YIELD::auto_Object<Log> log, Level level );

      YIELD::auto_Object<Log> log;
      Level level;

      std::ostringstream oss;
    };


    static YIELD::auto_Object<Log> open( std::ostream&, Level level );
    static YIELD::auto_Object<Log> open( const Path& file_path, Level level );

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
  };

  typedef YIELD::auto_Object<Log> auto_Log;


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


  class MemoryMappedFile : public Object
  {
  public:
    static YIELD::auto_Object<MemoryMappedFile> open( const Path& path ) { return open( path, File::DEFAULT_FLAGS, File::DEFAULT_MODE, File::DEFAULT_ATTRIBUTES, 0 ); }
    static YIELD::auto_Object<MemoryMappedFile> open( const Path& path, uint32_t flags ) { return open( path, flags, File::DEFAULT_MODE, File::DEFAULT_ATTRIBUTES, 0 ); }
    static YIELD::auto_Object<MemoryMappedFile> open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes, size_t minimum_size );

    virtual bool close();
    inline size_t get_size() const { return size; }
    inline operator char*() const { return start; }
    inline operator void*() const { return start; }
    bool resize( size_t );
    virtual bool sync();
    virtual bool sync( size_t offset, size_t length );
    virtual bool sync( void* ptr, size_t length );

    // Object
    YIELD_OBJECT_PROTOTYPES( MemoryMappedFile, 3 );

  protected:
    MemoryMappedFile( YIELD::auto_Object<File> underlying_file, uint32_t open_flags );
    virtual ~MemoryMappedFile() { close(); }

  private:
    YIELD::auto_Object<File> underlying_file;
    uint32_t open_flags;

#ifdef _WIN32
    void* mapping;
#endif
    char* start;
    size_t size;
  };

  typedef YIELD::auto_Object<MemoryMappedFile> auto_MemoryMappedFile;


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


  class PageAlignedBuffer : public FixedBuffer
  {
  public:
    PageAlignedBuffer( size_t capacity );
    virtual ~PageAlignedBuffer();

  private:
    static size_t page_size;
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

  typedef YIELD::auto_Object<Path> auto_Path;


  class PerformanceCounterSet : public Object
  {
  public:
    static auto_Object<PerformanceCounterSet> create();
    
    bool addEvent( const char* name );
    void startCounting();
    void stopCounting( uint64_t* counts );

    // Object
    YIELD_OBJECT_PROTOTYPES( PerformanceCounterSet, 0 );

  private:    
#ifdef __sun
    PerformanceCounterSet( cpc_t* cpc, cpc_set_t* cpc_set );
    cpc_t* cpc; cpc_set_t* cpc_set;

    std::vector<int> event_indices;
    cpc_buf_t* start_cpc_buf;
#else
    PerformanceCounterSet() { }
#endif

    ~PerformanceCounterSet();
  };

  typedef auto_Object<PerformanceCounterSet> auto_PerformanceCounterSet;


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
#elif defined(__linux__)
    void* cpu_set;
#elif defined(__sun)
    int psetid;
#endif
  };

  typedef auto_Object<ProcessorSet> auto_ProcessorSet;


  class RRD : public Object
  {
  public: 
    class Record : public Object
    {
    public:
      Record( double value );
      Record( const Time& time, double value );

      const Time& get_time() const { return time; }
      double get_value() const { return value; }
      operator double() const { return value; }

      // Object
      YIELD_OBJECT_PROTOTYPES( RRD::Record, 0 );
      void marshal( Marshaller& marshaller );
      void unmarshal( Unmarshaller& unmarshaller );

    private:
      Time time;
      double value;
    };

    
    class RecordSet : public std::vector<Record*>
    {
    public:
      ~RecordSet();
    };


    static auto_Object<RRD> creat( const Path& file_path );
    static auto_Object<RRD> open( const Path& file_path );

    void append( double value );
    void fetch_all( RecordSet& out_records );
    void fetch_from( const Time& start_time, RecordSet& out_records );
    void fetch_range( const Time& start_time, const Time& end_time, RecordSet& out_records );
    void fetch_until( const Time& end_time, RecordSet& out_records );

    // Object
    YIELD_OBJECT_PROTOTYPES( RRD, 0 );

  private:
    RRD( const Path& file_path );
    ~RRD();

    Path current_file_path;
  };

  typedef auto_Object<RRD> auto_RRD;


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


  class SharedLibrary : public Object
  {
  public:
    static YIELD::auto_Object<SharedLibrary> open( const Path& file_prefix, const char* argv0 = 0 );

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

  typedef YIELD::auto_Object<SharedLibrary> auto_SharedLibrary;


  class Stat : public Object
  {
  public:   
    static YIELD::auto_Object<Stat> stat( const Path& path );

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

    void set_size( uint64_t size ) { this->size = size; }

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

  typedef YIELD::auto_Object<Stat> auto_Stat;


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
    virtual ~Thread();

    unsigned long get_id() const { return id; }
    void set_name( const char* name ) { setThreadName( get_id(), name ); }
    bool set_processor_affinity( unsigned short logical_processor_i );
    bool set_processor_affinity( const ProcessorSet& logical_processor_set );
    virtual void start();

    virtual void run() = 0;

    // Object
    YIELD_OBJECT_PROTOTYPES( Thread, 14 );

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


  class TimerQueue
  {
  public:
    class Timer : public Object
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

      // Object
      YIELD_OBJECT_PROTOTYPES( Timer, 0 );

    private:
      friend class TimerQueue;

      Time period, timeout;

#ifdef _WIN32
      void *hTimer, *hTimerQueue;
#endif
      Time last_fire_time;
    };


    TimerQueue();
    ~TimerQueue();

    void addTimer( auto_Object<Timer> timer );

  private:
#ifdef _WIN32
    void* hTimerQueue;
    static void __stdcall WaitOrTimerCallback( void*, unsigned char );
#else
    class Thread : public ::YIELD::Thread
    {
    public:
      Thread();
      
      void enqueueTimer( Timer* timer );
      void stop();

      // Thread
      void run();

    private:
      NonBlockingFiniteQueue<TimerQueue::Timer*, 16> new_timers_queue;
      Mutex new_timers_queue_signal;
      bool should_run;
      std::priority_queue< std::pair<uint64_t, Timer*>, std::vector< std::pair<uint64_t, Timer*> >, std::greater< std::pair<uint64_t, Timer*> > > timers;
    }; 

    Thread thread;
#endif
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
    virtual auto_Stat stat( const Path& path ) { return getattr( path ); }
    virtual bool touch( const Path& path ) { return touch( path, File::DEFAULT_MODE ); }
    virtual bool touch( const Path& path, mode_t mode );

    // Object
    YIELD_OBJECT_PROTOTYPES( Volume, 15 );
  };

  typedef YIELD::auto_Object<Volume> auto_Volume;


  class XDRMarshaller : public Marshaller
  {
  public:
    XDRMarshaller();

    auto_StringBuffer get_buffer() const { return buffer; }

    // Marshaller
    YIELD_MARSHALLER_PROTOTYPES;
    void writeFloat( const char* key, uint32_t tag, float value );
    void writeInt32( const char* key, uint32_t tag, int32_t value );

  protected:
    virtual void writeKey( const char* key );

  private:
    auto_StringBuffer buffer;
    std::vector<bool> in_map_stack;
  };


  class XDRUnmarshaller : public Unmarshaller
  {
  public:
    XDRUnmarshaller( auto_Buffer buffer );

    // Unmarshaller
    YIELD_UNMARSHALLER_PROTOTYPES;
    float readFloat( const char* key, uint32_t tag );
    int32_t readInt32( const char* key, uint32_t tag );

  private:
    auto_Buffer buffer;
  };
};

#endif
