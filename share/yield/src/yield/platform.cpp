#include "yield/platform.h"
using namespace YIELD::platform;


// counting_semaphore.cpp
#ifdef _WIN32
#define NOMINMAX
#include <windows.h>
#else
#include <unistd.h>
#ifdef __MACH__
#include <mach/clock.h>
#include <mach/mach_init.h>
#include <mach/task.h>
#endif
#endif


CountingSemaphore::CountingSemaphore()
{
#if defined(_WIN32)
  hSemaphore = CreateSemaphore( NULL, 0, LONG_MAX, NULL );
#elif defined(__MACH__)
  semaphore_create( mach_task_self(), &sem, SYNC_POLICY_FIFO, 0 );
#else
  sem_init( &sem, 0, 0 );
#endif
}

CountingSemaphore::~CountingSemaphore()
{
#if defined(_WIN32)
  CloseHandle( hSemaphore );
#elif defined(__MACH__)
  semaphore_destroy( mach_task_self(), sem );
#else
  sem_destroy( &sem );
#endif
}

bool CountingSemaphore::acquire()
{
#if defined(_WIN32)
  DWORD dwRet = WaitForSingleObjectEx( hSemaphore, INFINITE, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#elif defined(__MACH__)
  return semaphore_wait( sem ) == KERN_SUCCESS;
#else
  return sem_wait( &sem ) == 0;
#endif
}

bool CountingSemaphore::timed_acquire( const Time& timeout )
{
#if defined(_WIN32)
  DWORD timeout_ms = static_cast<DWORD>( timeout.as_unix_time_ms() );
  DWORD dwRet = WaitForSingleObjectEx( hSemaphore, timeout_ms, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#elif defined(__MACH__)
  mach_timespec_t timeout_m_ts
    = {
        timeout.as_unix_time_ns() / Time::NS_IN_S,
        timeout.as_unix_time_ns() % Time::NS_IN_S
      };
  return semaphore_timedwait( sem, timeout_m_ts ) == KERN_SUCCESS;
#else
  struct timespec timeout_ts = Time() + timeout;
  return sem_timedwait( &sem, &timeout_ts ) == 0;
#endif
}

bool CountingSemaphore::try_acquire()
{
#if defined(_WIN32)
  DWORD dwRet = WaitForSingleObjectEx( hSemaphore, 0, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#elif defined(__MACH__)
  mach_timespec_t timeout_m_ts = { 0, 0 };
  return semaphore_timedwait( sem, timeout_m_ts ) == KERN_SUCCESS;
#else
  return sem_trywait( &sem ) == 0;
#endif
}

void CountingSemaphore::release()
{
#if defined(_WIN32)
  ReleaseSemaphore( hSemaphore, 1, NULL );
#elif defined(__MACH__)
  semaphore_signal( sem );
#else
  sem_post( &sem );
#endif
}


// directory.cpp
#ifdef _WIN32
#include <windows.h>
#else
#include <dirent.h>
#endif


Directory::Directory()
#ifdef _WIN32
  : hDirectory( INVALID_HANDLE_VALUE )
#else
  : dirp( NULL )
#endif
{ }

#ifdef _WIN32
Directory::Directory
(
  HANDLE hDirectory,
  const WIN32_FIND_DATA& first_find_data
)
  : hDirectory( hDirectory )
{
  this->first_find_data = new WIN32_FIND_DATA;
  memcpy_s
  (
    this->first_find_data,
    sizeof( *this->first_find_data ),
    &first_find_data,
    sizeof( first_find_data )
  );
}
#else
Directory::Directory( void* dirp )
  : dirp( dirp )
{
}
#endif

Directory::~Directory()
{
#ifdef _WIN32
  if ( hDirectory != INVALID_HANDLE_VALUE )
  {
    FindClose( hDirectory );
    delete first_find_data;
  }
#else
  if ( dirp != NULL )
    closedir( static_cast<DIR*>( dirp ) );
#endif
}

Directory::auto_Entry Directory::readdir()
{
#ifdef _WIN32
  if ( first_find_data != NULL )
  {
    Entry* entry
      = new Entry( first_find_data->cFileName, new Stat( *first_find_data ) );
    delete first_find_data;
    first_find_data = NULL;
    return entry;
  }

  WIN32_FIND_DATA next_find_data;
  if ( FindNextFileW( hDirectory, &next_find_data ) )
    return new Entry( next_find_data.cFileName, new Stat( next_find_data ) );
#else
  struct dirent* next_dirent = ::readdir( static_cast<DIR*>( dirp ) );
  if ( next_dirent != NULL )
    return new Entry( next_dirent->d_name, next_dirent->d_type );
#endif

  return NULL;
}


Directory::Entry::Entry( const Path& name )
  : name( name )
{
#ifndef _WIN32
  d_type = DT_UNKNOWN;
#endif
}

Directory::Entry::Entry( const Path& name, auto_Stat stbuf )
  : name( name ), stbuf( stbuf )
{
#ifndef _WIN32
  d_type = DT_UNKNOWN;
#endif
}

#ifndef _WIN32
Directory::Entry::Entry( const char* d_name, unsigned char d_type )
  : name( d_name ), d_type( d_type )
{ }
#endif

bool Directory::Entry::ISDIR() const
{
  if ( stbuf != NULL )
    return stbuf->ISDIR();
  else
#ifdef _WIN32
    return false;
#else
    return d_type == DT_DIR;
#endif
}

#ifndef _WIN32
bool Directory::Entry::ISLNK() const
{
  if ( stbuf != NULL )
    return stbuf->ISLNK();
  else
#ifdef _WIN32
    return false;
#else
    return d_type == DT_LNK;
#endif
}
#endif

bool Directory::Entry::ISREG() const
{
  if ( stbuf != NULL )
    return stbuf->ISREG();
  else
#ifdef _WIN32
    return false;
#else
    return d_type == DT_REG;
#endif
}


// exception.cpp
#ifdef _WIN32
#include <windows.h>
#include <lmerr.h>
#else
#include <errno.h>
#endif


Exception::Exception()
  : error_message( NULL )
{
#ifdef _WIN32
  error_code = static_cast<uint32_t>( ::GetLastError() );
#else
  error_code = static_cast<uint32_t>( errno );
#endif
}

Exception::Exception( uint32_t error_code )
  : error_code( error_code ), error_message( NULL )
{ }

Exception::Exception( const char* error_message )
  : error_code( 0 ), error_message( NULL )
{
  set_error_message( error_message );
}

Exception::Exception( const std::string& error_message )
  : error_code( 0 ), error_message( NULL )
{
  set_error_message( error_message.c_str() );
}

Exception::Exception( uint32_t error_code, const char* error_message )
  : error_code( error_code ), error_message( NULL )
{
  set_error_message( error_message );
}

Exception::Exception( uint32_t error_code, const std::string& error_message )
  : error_code( error_code ), error_message( NULL )
{
  set_error_message( error_message.c_str() );
}

Exception::Exception( const Exception& other )
  : error_code( other.error_code ), error_message( NULL )
{
  set_error_message( other.error_message );
}

Exception::~Exception() throw()
{
#ifdef _WIN32
  LocalFree( error_message );
#else
  delete [] error_message;
#endif
}

const char* Exception::get_error_message() throw()
{
  if ( error_message != NULL )
    return error_message;
  else if ( error_code != 0 )
  {
#ifdef _WIN32
    DWORD dwMessageLength
      = FormatMessageA
      (
        FORMAT_MESSAGE_ALLOCATE_BUFFER|
          FORMAT_MESSAGE_FROM_SYSTEM|
          FORMAT_MESSAGE_IGNORE_INSERTS,
        NULL,
        error_code,
        MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ),
        error_message,
        0,
        NULL
      );

    if ( dwMessageLength > 0 )
    {
      if ( dwMessageLength > 2 )
        error_message[dwMessageLength - 2] = 0; // Cut off trailing \r\n

      return error_message;
    }
    else if ( error_code >= NERR_BASE || error_code <= MAX_NERR )
    {
      HMODULE hModule
        = LoadLibraryEx
        (
          TEXT( "netmsg.dll" ),
          NULL,
          LOAD_LIBRARY_AS_DATAFILE
        ); // Let's hope this is cheap..

      if ( hModule != NULL )
      {
        dwMessageLength
          = FormatMessageA
          (
            FORMAT_MESSAGE_ALLOCATE_BUFFER|
              FORMAT_MESSAGE_FROM_SYSTEM|
              FORMAT_MESSAGE_IGNORE_INSERTS,
            hModule,
            error_code,
            MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ),
            error_message,
            0,
            NULL
          );

        if ( dwMessageLength > 0 )
        {
          FreeLibrary( hModule );

          if ( dwMessageLength > 2 )
            error_message[dwMessageLength - 2] = 0; // Cut off trailing \r\n

          return error_message;
        }
        else
          FreeLibrary( hModule );
      }
    }

    // Could not get an error_message for error_code from FormatMessage
    // Set error_message to a dummy value so we don't have to try this again
    error_message = static_cast<char*>( LocalAlloc( LMEM_FIXED, 19 ) );
    sprintf_s( error_message, 19, "errno = %u", error_code );
    return error_message;
#else
    // strerror_r is more or less unusable in a portable way,
    // thanks to the GNU-specific implementation.
    // You have to define _XOPEN_SOURCE to get the POSIX implementation,
    // but that apparently breaks libstdc++.
    // So we just use strerror.
    set_error_message( strerror( error_code ) );
    return error_message;
#endif
  }
  else
    return "(unknown)";
}

void Exception::set_error_code( uint32_t error_code )
{
  this->error_code = error_code;
}

void Exception::set_error_message( const char* error_message )
{
#ifdef _WIN32
  LocalFree( this->error_message );
#else
  delete [] this->error_message;
#endif

  if ( error_message != NULL )
  {
    size_t error_message_len = strlen( error_message );
#ifdef _WIN32
    this->error_message
      = static_cast<char*>( LocalAlloc( LMEM_FIXED, error_message_len+1 ) );
#else
    this->error_message = new char[error_message_len+1];
#endif
    memcpy( this->error_message, error_message, error_message_len+1 );
  }
  else
    this->error_message = NULL;
}


// file.cpp
#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#else
#include <stdlib.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/uio.h> // For writev
#include <unistd.h>
#if defined(__linux__)
// Mac OS X's off_t is already 64-bit
#define lseek lseek64
#elif defined(__sun)
extern off64_t lseek64(int, off64_t, int);
#define lseek lseek64
#endif
#ifdef YIELD_HAVE_POSIX_FILE_AIO
#include <aio.h>
#endif
#ifdef YIELD_HAVE_XATTR_H
#if defined(__linux__)
#include <sys/xattr.h>
#define FLISTXATTR ::flistxattr
#define FGETXATTR ::fgetxattr
#define FSETXATTR ::fsetxattr
#define FREMOVEXATTR ::fremovexattr
#elif defined(__MACH__)
#include <sys/xattr.h>
#define FLISTXATTR( fd, namebuf, size ) \
  ::flistxattr( fd, namebuf, size, 0 )
#define FGETXATTR( fd, name, value, size ) \
  ::fgetxattr( fd, name, value, size, 0, 0 )
#define FSETXATTR( fd, name, value, size, flags ) \
  ::fsetxattr( fd, name, value, size, 0, flags )
#define FREMOVEXATTR( fd, name ) \
  ::fremovexattr( fd, name, 0 )
#endif
#endif
#endif


File::File()
#ifdef _WIN32
  : fd( INVALID_HANDLE_VALUE )
#else
  : fd( -1 )
#endif
{ }

#ifdef _WIN32
File::File( void* fd )
  : fd( fd )
#else
File::File( int fd )
  : fd( fd )
#endif
{ }

File::File( const File& other )
{
  DebugBreak();
}

bool File::close()
{
#ifdef _WIN32
  if ( fd != INVALID_HANDLE_VALUE && CloseHandle( fd ) != 0 )
  {
    fd = INVALID_HANDLE_VALUE;
#else
  if ( fd != -1 && ::close( fd ) >= 0 )
  {
    fd = -1;
#endif
    return true;
  }
  else
    return false;
}

bool File::datasync()
{
#if defined(_WIN32)
  return FlushFileBuffers( fd ) != 0;
#elif defined(__linux__) || defined(__sun)
  return fdatasync( fd ) != -1;
#else
  return true;
#endif
}

size_t File::getpagesize()
{
#ifdef _WIN32
  SYSTEM_INFO system_info;
  GetSystemInfo( &system_info );
  return system_info.dwPageSize;
#else
  return ::getpagesize();
#endif
}

auto_Stat File::getattr()
{
#ifdef _WIN32
  BY_HANDLE_FILE_INFORMATION by_handle_file_information;
  if ( GetFileInformationByHandle( fd, &by_handle_file_information ) != 0 )
    return new Stat( by_handle_file_information );
#else
  struct stat stbuf;
  if ( fstat( fd, &stbuf ) != -1 )
    return new Stat( stbuf );
#endif
  return NULL;
}

bool File::getlk( bool exclusive, uint64_t offset, uint64_t length )
{
#ifdef _WIN32
  return false;
#else
  struct flock flock_;
  flock_.l_type   = exclusive ? F_WRLCK : F_RDLCK;
  flock_.l_whence = SEEK_SET;
  flock_.l_start  = offset;
  flock_.l_len    = length;
  flock_.l_pid    = getpid();
  if ( fcntl( fd, F_GETLK, &flock_ ) != -1 )
    return flock_.l_type != F_UNLCK;
  else
    return false;
#endif
}

bool File::getxattr( const std::string& name, std::string& out_value )
{
#ifdef YIELD_HAVE_XATTR_H
  ssize_t value_len = FGETXATTR( fd, name.c_str(), NULL, 0 );
  if ( value_len != -1 )
  {
    char* value = new char[value_len];
    FGETXATTR( fd, name.c_str(), value, value_len );
    out_value.assign( value, value_len );
    delete [] value;
    return true;
  }
  else
    return false;
#else
  return false;
#endif
}

bool File::listxattr( std::vector<std::string>& out_names )
{
#ifdef YIELD_HAVE_XATTR_H
  size_t names_len = FLISTXATTR( fd, NULL, 0 );
  if ( names_len > 0 )
  {
    char* names = new char[names_len];
    FLISTXATTR( fd, names, names_len );
    char* name = names;
    do
    {
      size_t name_len = strlen( name );
      out_names.push_back( std::string( name, name_len ) );
      name += name_len;
    }
    while ( static_cast<size_t>( name - names ) < names_len );
    delete [] names;
  }
  return true;
#else
  return false;
#endif
}

ssize_t File::read( yidl::runtime::auto_Buffer buffer )
{
  ssize_t read_ret = read( static_cast<void*>( *buffer ), buffer->capacity() );
  buffer->put( NULL, read_ret );
  return read_ret;
}

ssize_t File::read( void* buffer, size_t buffer_len, uint64_t offset )
{
  if ( seek( offset, SEEK_SET ) )
    return read( buffer, buffer_len );
  else
    return -1;
}

ssize_t File::read( void* buffer, size_t buffer_len )
{
#ifdef _WIN32
  DWORD dwBytesRead;
  if
  (
    ReadFile
    (
      fd,
      buffer,
      static_cast<DWORD>( buffer_len ),
      &dwBytesRead,
      NULL
    )
  )
    return dwBytesRead;
  else
    return -1;
#else
  return ::read( fd, buffer, buffer_len );
#endif
}

bool File::removexattr( const std::string& name )
{
#ifdef YIELD_HAVE_XATTR_H
  return FREMOVEXATTR( fd, name.c_str() ) != -1;
#else
  return false;
#endif
}

bool File::seek( uint64_t offset )
{
  return seek( offset, SEEK_SET );
}

bool File::seek( uint64_t offset, unsigned char whence )
{
#ifdef _WIN32
  ULARGE_INTEGER uliOffset;
  uliOffset.QuadPart = offset;
  if
  (
    SetFilePointer
    (
      fd,
      uliOffset.LowPart,
      ( PLONG )&uliOffset.HighPart,
      whence
    ) != INVALID_SET_FILE_POINTER
  )
    return true;
  else
    return false;
#else
  off_t new_offset = lseek( fd, static_cast<off_t>( offset ), whence );
  if ( new_offset != -1 )
  {
//    offset = new_offset;
    return true;
  }
  else
    return false;
#endif
}

bool File::setlk( bool exclusive, uint64_t offset, uint64_t length )
{
#ifdef _WIN32
  return setlkw( exclusive, offset, length );
#else
  struct flock flock_;
  flock_.l_type   = exclusive ? F_WRLCK : F_RDLCK;
  flock_.l_whence = SEEK_SET;
  flock_.l_start  = offset;
  flock_.l_len    = length;
  flock_.l_pid    = getpid();
  return fcntl( fd, F_SETLK, &flock_ ) != -1;
#endif
}

bool File::setlkw( bool exclusive, uint64_t offset, uint64_t length )
{
#ifdef _WIN32
  if ( exclusive )
  {
    ULARGE_INTEGER uliOffset, uliLength;
    uliOffset.QuadPart = offset;
    uliLength.QuadPart = length;
    return LockFile
    (
      fd,
      uliOffset.LowPart,
      uliOffset.HighPart,
      uliLength.LowPart,
      uliLength.HighPart
    ) == TRUE;
  }
  else
    return false;
#else
  struct flock flock_;
  flock_.l_type = exclusive ? F_WRLCK : F_RDLCK;
  flock_.l_whence = SEEK_SET;
  flock_.l_start = offset;
  flock_.l_len = length;
  flock_.l_pid = getpid();
  return fcntl( fd, F_SETLKW, &flock_ ) != -1;
#endif
}

bool File::setxattr
(
  const std::string& name,
  const std::string& value,
  int flags
)
{
#ifdef YIELD_HAVE_XATTR_H
  return FSETXATTR
  (
    fd,
    name.c_str(),
    value.c_str(),
    value.size(),
    flags
  ) != -1;
#else
  return false;
#endif
}

auto_Stat File::stat()
{
  return getattr();
}

bool File::sync()
{
#ifdef _WIN32
  return FlushFileBuffers( fd ) != 0;
#else
  return fsync( fd ) != -1;
#endif
}

bool File::truncate( uint64_t new_size )
{
#ifdef _WIN32
  ULARGE_INTEGER uliNewSize;
  uliNewSize.QuadPart = new_size;
  if
  (
    SetFilePointer
    (
      fd,
      uliNewSize.LowPart,
      ( PLONG )&uliNewSize.HighPart,
      SEEK_SET
    ) != INVALID_SET_FILE_POINTER
  )
    return SetEndOfFile( fd ) != 0;
  else
    return false;
#else
  return ::ftruncate( fd, new_size ) != -1;
#endif
}

bool File::unlk( uint64_t offset, uint64_t length )
{
#ifdef _WIN32
  ULARGE_INTEGER uliOffset, uliLength;
  uliOffset.QuadPart = offset;
  uliLength.QuadPart = length;
  return UnlockFile
  (
    fd,
    uliOffset.LowPart,
    uliOffset.HighPart,
    uliLength.LowPart,
    uliLength.HighPart
  ) == TRUE;
#else
  struct flock flock_;
  flock_.l_type   = F_UNLCK;
  flock_.l_whence = SEEK_SET;
  flock_.l_start  = offset;
  flock_.l_len    = length;
  flock_.l_pid    = getpid();
  return fcntl( fd, F_SETLK, &flock_ ) != -1;
#endif
}

ssize_t File::write( yidl::runtime::auto_Buffer buffer )
{
  return write( static_cast<void*>( *buffer ), buffer->size() );
}

ssize_t File::write( const void* buffer, size_t buffer_len )
{
#ifdef _WIN32
  DWORD dwBytesWritten;
  if
  (
    WriteFile
    (
      fd,
      buffer,
      static_cast<DWORD>( buffer_len ),
      &dwBytesWritten,
      NULL
    )
  )
    return static_cast<ssize_t>( dwBytesWritten );
  else
    return -1;
#else
  return ::write( fd, buffer, buffer_len );
#endif
}

ssize_t File::write( const void* buffer, size_t buffer_len, uint64_t offset )
{
  if ( seek( offset ) )
    return write( buffer, buffer_len );
  else
    return -1;
}

#ifdef _WIN32
#pragma warning( pop )
#endif


// iconv.cpp
#ifdef _WIN32
#include <windows.h>
#else
#include <iconv.h>
#if defined(__FreeBSD__) || defined(__OpenBSD__) || defined(__sun)
#define ICONV_SOURCE_CAST const char**
#else
#define ICONV_SOURCE_CAST char**
#endif
#endif


#ifdef _WIN32
iconv::iconv( UINT fromcode, UINT tocode )
  : fromcode( fromcode ), tocode( tocode )
{ }
#else
iconv::iconv( iconv_t cd )
 : cd( cd )
{ }
#endif

iconv::~iconv()
{
#ifndef _WIN32
  iconv_close( cd );
#endif
}

#ifdef _WIN32
UINT iconv::iconv_code_to_win32_code( const char* iconv_code )
{
  if ( strcmp( iconv_code, "") == 0 || strcmp( iconv_code, "char" ) == 0 )
    return GetACP();
  else if ( strcmp( iconv_code, "ISO-8859-1" ) == 0 )
    return CP_ACP;
  else if ( strcmp( iconv_code, "UTF-8") == 0 )
    return CP_UTF8;
  else
    throw Exception( "iconv: unsupported code" );
}
#endif

auto_iconv iconv::open( const char* tocode, const char* fromcode )
{
#ifdef _WIN32
  UINT tocode_uint = iconv_code_to_win32_code( tocode );
  UINT fromcode_uint = iconv_code_to_win32_code( fromcode );
  return new iconv( fromcode_uint, tocode_uint );
#else
  iconv_t cd = ::iconv_open( tocode, fromcode );
  if ( cd != reinterpret_cast<iconv_t>( -1 ) )
    return new iconv( cd );
  else
    throw Exception();
#endif
}

size_t
iconv::operator()
(
  const char** inbuf,
  size_t* inbytesleft,
  char** outbuf,
  size_t* outbytesleft
)
{
#ifdef _WIN32
  int inbuf_w_len
    = MultiByteToWideChar
      (
        fromcode,
        0,
        *inbuf,
        static_cast<int>( *inbytesleft ),
        NULL,
        0
      );

  if ( inbuf_w_len > 0 )
  {
    wchar_t* inbuf_w = new wchar_t[inbuf_w_len];

    inbuf_w_len
      = MultiByteToWideChar
        (
          fromcode,
          0,
          *inbuf,
          static_cast<int>( *inbytesleft ),
          inbuf_w,
          inbuf_w_len
        );

    if ( inbuf_w_len > 0 )
    {
      int outbyteswritten
        = WideCharToMultiByte
          (
            tocode,
            0,
            inbuf_w,
            inbuf_w_len,
            *outbuf,
            *outbytesleft,
            0,
            0
          );

      delete [] inbuf_w;

      if ( outbyteswritten > 0 )
      {
        *inbuf += *inbytesleft;
        *inbytesleft = 0;
        *outbytesleft -= outbyteswritten;
        return outbyteswritten;
      }
    }
    else
      delete [] inbuf_w;
  }

  return static_cast<size_t>( -1 );
#else
  // Reset the converter
  if ( ::iconv( cd, NULL, 0, NULL, 0 ) != static_cast<size_t>( -1 ) )
  {
    // Now try to convert; will return ( size_t )-1 on failure
    return ::iconv
           (
             cd,
             ( ICONV_SOURCE_CAST )inbuf,
             inbytesleft,
             outbuf,
             outbytesleft
           );
  }
  else
    return static_cast<size_t>( -1 );
#endif
}

bool iconv::operator()( const std::string& inbuf, std::string& outbuf )
{
#ifdef _WIN32
  int inbuf_w_len
    = MultiByteToWideChar
      (
        fromcode,
        0,
        inbuf.c_str(),
        inbuf.size(),
        NULL,
        0
      );

  if ( inbuf_w_len > 0 )
  {
    wchar_t* inbuf_w = new wchar_t[inbuf_w_len];

    inbuf_w_len
      = MultiByteToWideChar
        (
          fromcode,
          0,
          inbuf.c_str(),
          inbuf.size(),
          inbuf_w,
          inbuf_w_len
        );

    if ( inbuf_w_len > 0 )
    {
      int outbuf_c_len
        = WideCharToMultiByte
          (
            tocode,
            0,
            inbuf_w,
            inbuf_w_len,
            NULL,
            0,
            0,
            0
          );

      if ( outbuf_c_len > 0 )
      {
        char* outbuf_c = new char[outbuf_c_len];

        outbuf_c_len
          = WideCharToMultiByte
            (
              tocode,
              0,
              inbuf_w,
              inbuf_w_len,
              outbuf_c,
              outbuf_c_len,
              0,
              0
            );

        if ( outbuf_c_len > 0 )
        {
          outbuf.append( outbuf_c, outbuf_c_len );
          delete [] outbuf_c;
          return true;
        }
        else
          delete [] outbuf_c;
      }
      else
        delete [] inbuf_w;
    }
    else
      delete [] inbuf_w;
  }

  return false;
#else
  // Reset the converter
  if ( ::iconv( cd, NULL, 0, NULL, 0 ) != static_cast<size_t>( -1 ) )
  {
    char* inbuf_c = const_cast<char*>( inbuf.c_str() );
    size_t inbytesleft = inbuf.size();
    size_t outbuf_c_len = inbuf.size();

    for ( ;; ) // Loop as long as ::iconv returns E2BIG
    {
      char* outbuf_c = new char[outbuf_c_len];
      char* outbuf_c_dummy = outbuf_c;
      size_t outbytesleft = outbuf_c_len;

      size_t iconv_ret
        = ::iconv
          (
            cd,
            ( ICONV_SOURCE_CAST )&inbuf_c,
            &inbytesleft,
            &outbuf_c_dummy,
            &outbytesleft
          );

      if ( iconv_ret != static_cast<size_t>( -1 ) )
      {
        outbuf.append( outbuf_c, outbuf_c_len - outbytesleft );
        delete [] outbuf_c;
        return true;
      }
      else if ( errno == E2BIG )
      {
#ifdef _DEBUG
        if ( outbytesleft != 0 ) DebugBreak();
#endif
        outbuf.append( outbuf_c, outbuf_c_len );
        delete [] outbuf_c;
        outbuf_c_len *= 2;
        continue;
      }
      else
      {
        delete [] outbuf_c;
        return false;
      }
    }
  }
  else
    return false;
#endif
}

#ifdef _WIN32
bool iconv::operator()( const std::string& inbuf, std::wstring& outbuf )
{
  int outbuf_w_len
    = MultiByteToWideChar
      (
        fromcode,
        0,
        inbuf.c_str(),
        inbuf.size(),
        NULL,
        0
      );

  if ( outbuf_w_len > 0 )
  {
    wchar_t* outbuf_w = new wchar_t[outbuf_w_len];

    outbuf_w_len
      = MultiByteToWideChar
        (
          fromcode,
          0,
          inbuf.c_str(),
          inbuf.size(),
          outbuf_w,
          outbuf_w_len
        );

    if ( outbuf_w_len > 0 )
    {
      outbuf.append( outbuf_w, outbuf_w_len );
      delete [] outbuf_w;
    }
    else
      delete [] outbuf_w;
  }

  return false;
}

bool iconv::operator()( const std::wstring& inbuf, std::string& outbuf )
{
  int outbuf_c_len
    = WideCharToMultiByte
      (
        tocode,
        0,
        inbuf.c_str(),
        inbuf.size(),
        NULL,
        0,
        0,
        0
      );

  if ( outbuf_c_len > 0 )
  {
    char* outbuf_c = new char[outbuf_c_len];

    outbuf_c_len
      = WideCharToMultiByte
        (
          tocode,
          0,
          inbuf.c_str(),
          inbuf.size(),
          outbuf_c,
          outbuf_c_len,
          0,
          0
        );

    if ( outbuf_c_len > 0 )
    {
      outbuf.append( outbuf_c, outbuf_c_len );
      delete [] outbuf_c;
      return true;
    }
    else
      delete [] outbuf_c;
  }

  return false;
}
#endif


// log.cpp
namespace YIELD
{
  namespace platform
  {
    class FileLog : public Log
    {
    public:
      FileLog( auto_File file, Level level )
        : Log( level ), file( file )
      { }

      FileLog( const Path& file_path, Level level ) // Lazy open
        : Log( level ), file_path( file_path )
      { }

      // Log
      void write( const char* str, size_t str_len )
      {
        if ( file == NULL ) // Lazy open
        {
          file = Volume().open( file_path, O_CREAT|O_WRONLY|O_APPEND );
          if ( file == NULL )
            return;
        }

       file->write( str, str_len );
      }

    private:
      auto_File file;
      Path file_path;
    };


    class ostreamLog : public Log
    {
    public:
      ostreamLog( std::ostream& underlying_ostream, Level level )
        : Log( level ), underlying_ostream( underlying_ostream )
      { }

      ostreamLog& operator=( const ostreamLog& ) { return *this; }

      // Log
      void write( const char* str, size_t str_len )
      {
        underlying_ostream.write( str, str_len );
      }

    private:
      std::ostream& underlying_ostream;
    };
  };
};


Log::Stream::Stream( auto_Log log, Log::Level level )
  : log( log ), level( level )
{ }

Log::Stream::Stream( const Stream& other )
  : log( other.log ), level( other.level )
{ }

Log::Stream::~Stream()
{
  if ( level <= log->get_level() && !oss.str().empty() )
  {
    std::ostringstream stamped_oss;
    stamped_oss << static_cast<std::string>( Time() );
    stamped_oss << " ";
    const char* level_str;
    switch ( level )
    {
      case LOG_EMERG: level_str = "EMERG"; break;
      case LOG_ALERT: level_str = "ALERT"; break;
      case LOG_CRIT: level_str = "CRIT"; break;
      case LOG_ERR: level_str = "ERR"; break;
      case LOG_WARNING: level_str = "WARNING"; break;
      case LOG_NOTICE: level_str = "NOTICE"; break;
      case LOG_INFO: level_str = "INFO"; break;
      default: level_str = "DEBUG"; break;
    }
    stamped_oss << level_str;
    stamped_oss << ": ";
    stamped_oss << oss.str();
    stamped_oss << std::endl;

    log->write( stamped_oss.str(), level );
  }
}

auto_Log Log::open( std::ostream& underlying_ostream, Level level )
{
  return new ostreamLog( underlying_ostream, level );
}

auto_Log Log::open( const Path& file_path, Level level, bool lazy )
{
  if ( lazy )
    return new FileLog( file_path, level );
  else
  {
    auto_File file = Volume().open( file_path, O_CREAT|O_WRONLY|O_APPEND );
    if ( file != NULL )
      return new FileLog( file, level );
    else
      return NULL;
  }
}

void Log::write( const unsigned char* str, size_t str_len, Level level )
{
  if ( level <= this->level )
  {
    bool str_is_printable = true;
    for ( size_t str_i = 0; str_i < str_len; str_i++ )
    {
      if
      (
        str[str_i] == '\r' ||
        str[str_i] == '\n' ||
        ( str[str_i] >= 32 && str[str_i] <= 126 )
      )
        continue;
      else
      {
        str_is_printable = false;
        break;
      }
    }

    if ( str_is_printable )
      write( reinterpret_cast<const char*>( str ), str_len, level );
    else
    {
      char* printable_str = new char[str_len * 3];
      size_t printable_str_len = 0;

      for ( size_t str_i = 0; str_i < str_len; str_i++ )
      {
        char hex_digit = ( str[str_i] >> 4 ) & 0x0F;
        if ( hex_digit >= 0 && hex_digit <= 9 )
          printable_str[printable_str_len++] = '0' + hex_digit;
        else
          printable_str[printable_str_len++] = 'A' + hex_digit - 10;

        hex_digit = str[str_i] & 0x0F;
        if ( hex_digit >= 0 && hex_digit <= 9 )
          printable_str[printable_str_len++] = '0' + hex_digit;
        else
          printable_str[printable_str_len++] = 'A' + hex_digit - 10;

        printable_str[printable_str_len++] = ' ';
      }

      write( printable_str, printable_str_len );

      delete [] printable_str;
    }
  }
}


// machine.cpp
#ifdef _WIN32
#include <windows.h>
#else
#if defined(__MACH__)
#include <mach/mach.h>
#include <mach/mach_error.h>
#elif defined(__sun)
#include <sys/processor.h> // For p_online
#include <kstat.h> // For kstat
#endif
#endif


uint16_t Machine::getLogicalProcessorsPerPhysicalProcessor()
{
  return getOnlineLogicalProcessorCount() / getOnlinePhysicalProcessorCount();
}

uint16_t Machine::getOnlineLogicalProcessorCount()
{
  uint16_t online_logical_processor_count = 0;

#if defined(_WIN32)
  SYSTEM_INFO available_info;
  GetSystemInfo( &available_info );
  online_logical_processor_count
    = static_cast<uint16_t>( available_info.dwNumberOfProcessors );
#elif defined(__linux__)
  long _online_logical_processor_count = sysconf( _SC_NPROCESSORS_ONLN );
  if ( _online_logical_processor_count != -1 )
    online_logical_processor_count = _online_logical_processor_count;
#elif defined(__MACH__)
  host_basic_info_data_t basic_info;
  host_info_t info = (host_info_t)&basic_info;
  host_flavor_t flavor = HOST_BASIC_INFO;
  mach_msg_type_number_t count = HOST_BASIC_INFO_COUNT;

  if ( host_info( mach_host_self(), flavor, info, &count ) == KERN_SUCCESS )
    online_logical_processor_count = basic_info.avail_cpus;
#elif defined(__sun)
  online_logical_processor_count = 0;
  processorid_t cpuid_max = sysconf( _SC_CPUID_MAX );
  for ( processorid_t cpuid_i = 0; cpuid_i <= cpuid_max; cpuid_i++)
  {
    if ( p_online( cpuid_i, P_STATUS ) == P_ONLINE )
      online_logical_processor_count++;
  }
#endif

  if ( online_logical_processor_count > 0 )
    return online_logical_processor_count;
  else
    return 1;
}

uint16_t Machine::getOnlinePhysicalProcessorCount()
{
#if defined(__sun)
  kstat_ctl_t* kc;

  kc = kstat_open();
  if ( kc )
  {
    uint16_t online_physical_processor_count = 1;

    kstat* ksp = kstat_lookup( kc, "cpu_info", -1, NULL );
    int32_t last_core_id = 0;
    while ( ksp )
    {
      kstat_read( kc, ksp, NULL );
      kstat_named_t* knp;
      knp = ( kstat_named_t* )kstat_data_lookup( ksp, "core_id" );
      if ( knp )
      {
        int32_t this_core_id = knp->value.i32;
        if ( this_core_id != last_core_id )
        {
          online_physical_processor_count++;
          last_core_id = this_core_id;
        }
      }
      ksp = ksp->ks_next;
    }

    kstat_close( kc );

    return online_physical_processor_count;
  }
#endif

  return getOnlineLogicalProcessorCount();
}


// memory_mapped_file.cpp
#ifdef _WIN32
#define NOMINMAX
#include <windows.h>
#else
#include <sys/mman.h>
#endif


MemoryMappedFile::MemoryMappedFile
(
  auto_File underlying_file,
  uint32_t open_flags
)
  : underlying_file( underlying_file ),
    open_flags( open_flags )
{
#ifdef _WIN32
  mapping = NULL;
#endif
  size = 0;
  start = NULL;
}

bool MemoryMappedFile::close()
{
  if ( start != NULL )
  {
    sync();
#ifdef _WIN32
    UnmapViewOfFile( start );
#else
    munmap( start, size );
#endif
    start = NULL;
  }

#ifdef _WIN32
  if ( mapping != NULL )
  {
    CloseHandle( mapping );
    mapping = NULL;
  }
#endif

  return underlying_file->close();
}

auto_MemoryMappedFile
MemoryMappedFile::open( const Path& path )
{
  return open
         (
           path,
           File::FLAGS_DEFAULT,
           File::MODE_DEFAULT,
           File::ATTRIBUTES_DEFAULT,
           0
          );
}

auto_MemoryMappedFile
MemoryMappedFile::open( const Path& path, uint32_t flags )
{
  return open
         (
           path,
           flags,
           File::MODE_DEFAULT,
           File::ATTRIBUTES_DEFAULT,
           0
         );
}

auto_MemoryMappedFile
MemoryMappedFile::open
(
  const Path& path,
  uint32_t flags,
  mode_t mode,
  uint32_t attributes,
  size_t minimum_size
)
{
  auto_File file( Volume().open( path, flags, mode, attributes ) );

  if ( file != NULL )
  {
    size_t current_file_size;
    if ( ( flags & O_TRUNC ) != O_TRUNC )
    {
#ifdef _WIN32
      ULARGE_INTEGER uliFileSize;
      uliFileSize.LowPart = GetFileSize( *file, &uliFileSize.HighPart );
      current_file_size = static_cast<size_t>( uliFileSize.QuadPart );
#else
      auto_Stat stbuf = file->stat();
      if ( stbuf != NULL )
        current_file_size = stbuf->get_size();
      else
        current_file_size = 0;
#endif
    }
    else
      current_file_size = 0;

    auto_MemoryMappedFile memory_mapped_file
      = new MemoryMappedFile( file, flags );

    if
    (
      memory_mapped_file->resize
      (
        std::max( minimum_size, current_file_size )
      )
    )
      return memory_mapped_file;
    else
      return NULL;
  }
  else
    return NULL;
}

bool MemoryMappedFile::resize( size_t new_size )
{
  if ( new_size > 0 )
  {
#ifdef _WIN32
    if ( start != NULL )
    {
      if ( UnmapViewOfFile( start ) != TRUE )
        return false;
    }

    if ( mapping != NULL )
    {
      if ( CloseHandle( mapping ) != TRUE )
        return false;
    }
#else
    if ( start != NULL )
    {
      sync();
      if ( munmap( start, size ) == -1 )
        return false;
    }
#endif

    if ( size == new_size ||
         underlying_file->truncate( new_size ) )
    {
#ifdef _WIN32
      unsigned long map_flags = PAGE_READONLY;
      if ( ( open_flags & O_RDWR ) == O_RDWR ||
           ( open_flags & O_WRONLY ) == O_WRONLY )
        map_flags = PAGE_READWRITE;

      ULARGE_INTEGER uliNewSize; uliNewSize.QuadPart = new_size;

      mapping = CreateFileMapping
                (
                  *underlying_file,
                  NULL, map_flags,
                  uliNewSize.HighPart,
                  uliNewSize.LowPart,
                  NULL
                );

      if ( mapping != NULL )
      {
        map_flags = FILE_MAP_READ;
        if( ( open_flags & O_RDWR ) || ( open_flags & O_WRONLY ) )
          map_flags = FILE_MAP_ALL_ACCESS;

        start = static_cast<char*>( MapViewOfFile( mapping, map_flags, 0, 0, 0 ) );
        if ( start != NULL )
        {
          size = new_size;
          return true;
        }
      }
#else
      unsigned long mmap_flags = PROT_READ;
      if( ( open_flags & O_RDWR ) == O_RDWR ||
          ( open_flags & O_WRONLY ) == O_WRONLY )
        mmap_flags |= PROT_WRITE;

      void* mmap_ret = mmap
                      (
                        0,
                        new_size,
                        mmap_flags,
                        MAP_SHARED,
                        *underlying_file,
                        0
                      );

      if ( mmap_ret != MAP_FAILED )
      {
        start = static_cast<char*>( mmap_ret );
        size = new_size;
        return true;
      }
#endif
    }
  }
  else
    return true;

  return false;
}

bool MemoryMappedFile::sync()
{
#ifdef _WIN32
  return sync
         (
           static_cast<size_t>( 0 ),
           static_cast<size_t>( 0 )
         ); // length 0 = flush to end of mapping
#else
  return sync( static_cast<size_t>( 0 ), size );
#endif
}

bool MemoryMappedFile::sync( size_t offset, size_t length )
{
  return sync( start + offset, length );
}

bool MemoryMappedFile::sync( void* ptr, size_t length )
{
#if defined(_WIN32)
  return FlushViewOfFile( ptr, length ) == TRUE;
#elif defined(__sun)
  return msync( static_cast<char*>( ptr ), length, MS_SYNC ) == 0;
#else
  return msync( ptr, length, MS_SYNC ) == 0;
#endif
}


// mutex.cpp
#if defined(_WIN32)
#include <windows.h>
#elif defined(__linux__) || defined(__FreeBSD__) || defined(__sun)
#define YIELD_HAVE_PTHREAD_MUTEX_TIMEDLOCK
#endif


Mutex::Mutex()
{
#ifdef _WIN32
  if ( ( hMutex = CreateEvent( NULL, FALSE, TRUE, NULL ) ) == NULL )
    DebugBreak();
#else
  if ( pthread_mutex_init( &pthread_mutex, NULL ) != 0 )
    DebugBreak();
#endif
}

Mutex::~Mutex()
{
#ifdef _WIN32
  if ( hMutex ) CloseHandle( hMutex );
#else
  pthread_mutex_destroy( &pthread_mutex );
#endif
}

bool Mutex::acquire()
{
#ifdef _WIN32
  DWORD dwRet = WaitForSingleObjectEx( hMutex, INFINITE, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#else
  pthread_mutex_lock( &pthread_mutex );
  return true;
#endif
}

bool Mutex::try_acquire()
{
#ifdef _WIN32
  DWORD dwRet = WaitForSingleObjectEx( hMutex, 0, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#else
  return pthread_mutex_trylock( &pthread_mutex ) == 0;
#endif
}

bool Mutex::timed_acquire( const Time& timeout )
{
#ifdef _WIN32
  DWORD timeout_ms = static_cast<DWORD>( timeout.as_unix_time_ms() );
  DWORD dwRet = WaitForSingleObjectEx( hMutex, timeout_ms, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#else
#ifdef YIELD_HAVE_PTHREAD_MUTEX_TIMEDLOCK
  struct timespec timeout_ts = Time() + timeout;
  return ( pthread_mutex_timedlock( &pthread_mutex, &timeout_ts ) == 0 );
#else
  if ( pthread_mutex_trylock( &pthread_mutex ) == 0 )
    return true;
  else
  {
    usleep( timeout.as_unix_time_us() );
    return pthread_mutex_trylock( &pthread_mutex ) == 0;
  }
#endif
#endif
}

void Mutex::release()
{
#ifdef _WIN32
  SetEvent( hMutex );
#else
  pthread_mutex_unlock( &pthread_mutex );
#endif
}


// path.cpp
#ifdef _WIN32
#include <windows.h>
#endif


#ifdef _WIN32
Path::Path( char narrow_path )
{
  init( &narrow_path, 1 );
}

Path::Path( const char* narrow_path )
{
  init( narrow_path, strlen( narrow_path ) );
}

Path::Path( const char* narrow_path, size_t narrow_path_len )
{
  init( narrow_path, narrow_path_len );
}

Path::Path( const std::string& narrow_path )
{
  init( narrow_path.c_str(), narrow_path.size() );
}

Path::Path( wchar_t wide_path )
  : path( 1, wide_path )
{ }

Path::Path( const wchar_t* wide_path )
  : path( wide_path )
{ }

Path::Path( const wchar_t* wide_path, size_t wide_path_len )
  : path( wide_path, wide_path_len )
{ }

Path::Path( const std::wstring& wide_path )
  : path( wide_path )
{ }
#else
Path::Path( char narrow_path )
  : path( 1, narrow_path )
{ }

Path::Path( const char* narrow_path )
  : path( narrow_path )
{ }

Path::Path( const char* narrow_path, size_t narrow_path_len )
  : path( narrow_path, narrow_path_len )
{ }

Path::Path( const std::string& narrow_path )
  : path( narrow_path )
{ }
#endif

Path Path::abspath() const
{
  string_type::value_type abspath_[PATH_MAX];
#ifdef _WIN32
  DWORD abspath__len
    = GetFullPathNameW
      (
        *this,
        PATH_MAX,
        abspath_,
        NULL
      );
  return Path( abspath_, abspath__len );
#else
  realpath( *this, abspath_ );
  return Path( abspath_ );
#endif
}

#ifdef _WIN32
void Path::init( const char* narrow_path, size_t narrow_path_len )
{
  wchar_t wide_path[PATH_MAX];
  this->path.assign
  (
    wide_path,
    MultiByteToWideChar
    (
      GetACP(),
      0,
      narrow_path,
      static_cast<int>( narrow_path_len ),
      wide_path,
      PATH_MAX
    )
  );
}

Path::operator std::string() const
{
  char narrow_path[PATH_MAX];

  int narrow_path_len
    = WideCharToMultiByte
    (
      GetACP(),
      0,
      *this,
      ( int )size(),
      narrow_path,
      PATH_MAX,
      0,
      0
    );

  return std::string( narrow_path, narrow_path_len );
}
#endif

Path Path::operator+( const Path& path ) const
{
  return operator+( path.path );
}

Path Path::operator+( const string_type& path ) const
{
  string_type combined_path( this->path );
  combined_path.append( path );
  return Path( combined_path );
}

Path Path::operator+( string_type::value_type path ) const
{
  string_type combined_path( this->path );
  combined_path.append( path, 1 );
  return Path( combined_path );
}

Path Path::operator+( const string_type::value_type* path ) const
{
  string_type combined_path( this->path );
  combined_path.append( path );
  return Path( combined_path );
}

Path Path::parent_path() const
{
  if ( *this != SEPARATOR )
  {
    std::vector<YIELD::platform::Path> parts;
    splitall( parts );
    if ( parts.size() > 1 )
      return parts[parts.size()-2];
    else
      return YIELD::platform::Path( SEPARATOR );
  }
  else
    return YIELD::platform::Path( SEPARATOR );
}

Path Path::root_path() const
{
#ifdef _WIN32
  std::vector<Path> path_parts;
  abspath().splitall( path_parts );
  return path_parts[0] + SEPARATOR;
#else
  return Path( "/" );
#endif
}

std::pair<Path, Path> Path::split() const
{
  string_type::size_type last_sep
    = path.find_last_of( SEPARATOR );

  if ( last_sep != string_type::npos )
    return std::make_pair
          (
            path.substr( 0, last_sep ),
            path.substr( last_sep + 1 )
          );
  else
    return std::make_pair( Path(), *this );
}

void Path::splitall( std::vector<Path>& parts ) const
{
  string_type::size_type last_sep
    = path.find_first_not_of( SEPARATOR, 0 );

  string_type::size_type next_sep
    = path.find_first_of( SEPARATOR, last_sep );

  while ( next_sep != string_type::npos || last_sep != string_type::npos )
  {
    parts.push_back
    (
      path.substr( last_sep, next_sep - last_sep )
    );

    last_sep
      = path.find_first_not_of( SEPARATOR, next_sep );

    next_sep
      = path.find_first_of( SEPARATOR, last_sep );
  }
}

std::pair<Path, Path> Path::splitext() const
{
  string_type::size_type last_dot;
#ifdef _WIN32
  last_dot = path.find_last_of( L"." );
#else
  last_dot = path.find_last_of( "." );
#endif

  if ( last_dot == 0 || last_dot == string_type::npos )
    return std::make_pair( *this, Path() );
  else
    return std::make_pair
           (
             path.substr( 0, last_dot ),
             path.substr( last_dot )
           );
}


// performance_counter_set.cpp
#ifdef YIELD_HAVE_PERFORMANCE_COUNTERS

#if defined(__sun)
#include <libcpc.h>
#elif defined(YIELD_HAVE_PAPI)
#include <papi.h>
#include <pthread.h>
#endif


auto_PerformanceCounterSet PerformanceCounterSet::create()
{
#if defined(__sun)
  cpc_t* cpc = cpc_open( CPC_VER_CURRENT );
  if ( cpc != NULL )
  {
    cpc_set_t* cpc_set = cpc_set_create( cpc );
    if ( cpc_set != NULL )
      return new PerformanceCounterSet( cpc, cpc_set );
    else
      cpc_close( cpc );
  }
#elif defined(YIELD_HAVE_PAPI)
  if ( PAPI_library_init( PAPI_VER_CURRENT ) == PAPI_VER_CURRENT )
  {
    if ( PAPI_thread_init( pthread_self ) == PAPI_OK )
    {
      int papi_eventset = PAPI_NULL;
      if ( PAPI_create_eventset( &papi_eventset ) == PAPI_OK )
        return new PerformanceCounterSet( papi_eventset );
    }
  }
#endif

  return NULL;
}

#if defined(__sun)
PerformanceCounterSet::PerformanceCounterSet( cpc_t* cpc, cpc_set_t* cpc_set )
  : cpc( cpc ), cpc_set( cpc_set )
{
  start_cpc_buf = NULL;
}
#elif defined(YIELD_HAVE_PAPI)
PerformanceCounterSet::PerformanceCounterSet( int papi_eventset )
  : papi_eventset( papi_eventset )
{ }
#endif

PerformanceCounterSet::~PerformanceCounterSet()
{
#if defined(__sun)
  cpc_set_destroy( cpc, cpc_set );
  cpc_close( cpc );
#elif defined(YIELD_HAVE_PAPI)
  PAPI_cleanup_eventset( papi_eventset );
  PAPI_destroy_eventset( &papi_eventset );
#endif
}

bool PerformanceCounterSet::addEvent( Event event )
{
#if defined(__sun)
  switch ( event )
  {
    case EVENT_L1_DCM: return addEvent( "DC_miss" );
    case EVENT_L2_DCM: return addEvent( "DC_refill_from_system" );
    case EVENT_L2_ICM: return addEvent( "IC_refill_from_system" );
    default: DebugBreak(); return false;
  }
#elif defined(YIELD_HAVE_PAPI)
  switch ( event )
  {
    case EVENT_L1_DCM: return addEvent( "PAPI_l1_dcm" );
    case EVENT_L2_DCM: return addEvent( "PAPI_l2_dcm" );
    case EVENT_L2_ICM: return addEvent( "PAPI_l2_icm" );
    default: DebugBreak(); return false;
  }
#endif
}

bool PerformanceCounterSet::addEvent( const char* name )
{
#if defined(__sun)
  int event_index
    = cpc_set_add_request( cpc, cpc_set, name, 0, CPC_COUNT_USER, 0, NULL );

  if ( event_index != -1 )
  {
    event_indices.push_back( event_index );
    return true;
  }
#elif defined(YIELD_HAVE_PAPI)
  int papi_event_code;
  if
  (
    PAPI_event_name_to_code
    (
      const_cast<char*>( name ),
      &papi_event_code
    ) == PAPI_OK
  )
  {
     if ( PAPI_add_event( papi_eventset, papi_event_code ) == PAPI_OK )
       return true;
  }
#endif

  return false;
}

void PerformanceCounterSet::startCounting()
{
#if defined(__sun)
  if ( start_cpc_buf == NULL )
    start_cpc_buf = cpc_buf_create( cpc, cpc_set );
  cpc_bind_curlwp( cpc, cpc_set, 0 );
  cpc_set_sample( cpc, cpc_set, start_cpc_buf );
#elif defined(YIELD_HAVE_PAPI)
  PAPI_start( papi_eventset );
#endif
}

void PerformanceCounterSet::stopCounting( uint64_t* counts )
{
#if defined(__sun)
  cpc_buf_t* stop_cpc_buf = cpc_buf_create( cpc, cpc_set );
  cpc_set_sample( cpc, cpc_set, stop_cpc_buf );

  cpc_buf_t* diff_cpc_buf = cpc_buf_create( cpc, cpc_set );
  cpc_buf_sub( cpc, diff_cpc_buf, stop_cpc_buf, start_cpc_buf );

  for
  (
    std::vector<int>::size_type event_index_i = 0;
    event_index_i < event_indices.size();
    event_index_i++
  )
  {
    cpc_buf_get
    (
      cpc,
      diff_cpc_buf,
      event_indices[event_index_i],
      &counts[event_index_i]
    );
  }

  cpc_unbind( cpc, cpc_set );
#elif defined(YIELD_HAVE_PAPI)
  PAPI_stop( papi_eventset, reinterpret_cast<long long int*>( counts ) );
#endif
}

#endif


// processor_set.cpp
#if defined(_WIN32)
#include <windows.h>
#elif defined(__linux)
#include <sched.h>
#elif defined(__sun)
#include <sys/pset.h>
#endif


ProcessorSet::ProcessorSet()
{
#if defined(_WIN32)
  mask = 0;
#elif defined(__linux)
  cpu_set = new cpu_set_t;
  CPU_ZERO( static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  psetid = PS_NONE; // Don't pset_create until we actually use the set,
                    // to avoid leaving state in the system
#else
  DebugBreak();
#endif
}

ProcessorSet::ProcessorSet( uint32_t from_mask )
{
#if defined(_WIN32)
  this->mask = from_mask;
#else
#if defined(__linux)
  cpu_set = new cpu_set_t;
  CPU_ZERO( static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  psetid = PS_NONE;
#else
  DebugBreak();
#endif
  if ( from_mask != 0 )
  {
    for ( uint32_t processor_i = 0; processor_i < 32; processor_i++ )
    {
      if ( ( 1UL << processor_i ) & from_mask )
        set( processor_i );
    }
  }
#endif
}

ProcessorSet::~ProcessorSet()
{
#if defined(__linux)
  delete static_cast<cpu_set_t*>( cpu_set );
#elif defined(__sun)
  if ( psetid != PS_NONE ) pset_destroy( psetid );
#endif
}

void ProcessorSet::clear()
{
#if defined(_WIN32)
  mask = 0;
#elif defined(__linux)
  CPU_ZERO( static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  if ( psetid != PS_NONE )
  {
    pset_destroy( psetid );
    psetid = PS_NONE;
  }
#endif
}

void ProcessorSet::clear( uint16_t processor_i )
{
#if defined(_WIN32)
  unsigned long bit = ( 1L << processor_i );
  if ( ( bit & mask ) == bit )
    mask ^= bit;
#elif defined(__linux)
  CPU_CLR( processor_i, static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  if ( psetid != PS_NONE )
    pset_assign( PS_NONE, processor_i, NULL );
#endif
}

uint16_t ProcessorSet::count() const
{
  uint16_t count = 0;
  for
  (
    uint16_t processor_i = 0;
    processor_i < static_cast<uint16_t>( -1 );
    processor_i++
  )
  {
    if ( isset( processor_i ) )
      count++;
  }
  return count;
}

bool ProcessorSet::empty() const
{
#if defined(_WIN32)
  return mask == 0;
#else
  return count() == 0;
#endif
}

bool ProcessorSet::isset( uint16_t processor_i ) const
{
#if defined(_WIN32)
  if ( processor_i < 32 )
  {
    unsigned long bit = ( 1L << processor_i );
    return ( bit & mask ) == bit;
  }
#elif defined(__linux)
  return CPU_ISSET( processor_i, static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  if ( psetid != PS_NONE )
  {
    psetid_t check_psetid;
    return pset_assign
           (
             PS_QUERY,
             processor_i,
             &check_psetid
           ) == 0
           &&
           check_psetid == psetid;
  }
#endif
  return false;
}

bool ProcessorSet::set( uint16_t processor_i )
{
#if defined(_WIN32)
  mask |= ( 1L << processor_i );
#elif defined(__linux)
  CPU_SET( processor_i, static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  if ( psetid == PS_NONE )
  {
    if ( pset_create( &psetid ) != 0 )
      return false;
  }

  if ( pset_assign( psetid, processor_i, NULL ) != 0 )
    return false;
#endif

  return isset( processor_i );
}


// shared_library.cpp
#ifdef _WIN32
#include <windows.h>
#else
#include <dlfcn.h>
#include <cctype>
#endif


#ifdef _WIN32
#define DLOPEN( file_path ) \
    LoadLibraryEx( file_path, 0, LOAD_WITH_ALTERED_SEARCH_PATH )
#else
#define DLOPEN( file_path ) \
    dlopen( file_path, RTLD_NOW|RTLD_GLOBAL )
#endif


#if defined(_WIN32)
const Path SharedLibrary::SHLIBSUFFIX( "dll" );
#elif defined(__MACH__)
const Path SharedLibrary::SHLIBSUFFIX( "dylib" );
#else
const Path SharedLibrary::SHLIBSUFFIX( "so" );
#endif


SharedLibrary::SharedLibrary( void* handle )
  : handle( handle )
{ }

SharedLibrary::~SharedLibrary()
{
  if ( handle != NULL )
  {
#ifdef _WIN32
    FreeLibrary( ( HMODULE )handle );
#else
#ifndef _DEBUG
    dlclose( handle ); // Don't dlclose when debugging,
                       // because that causes valgrind to lose symbols
#endif
#endif
  }
}

void* SharedLibrary::getFunction
(
  const char* function_name,
  void* missing_function_return_value
)
{
  void* function_handle;
#ifdef _WIN32
  function_handle = GetProcAddress( ( HMODULE )handle, function_name );
#else
  function_handle = dlsym( handle, function_name );
#endif
  if ( function_handle )
    return function_handle;
  else
    return missing_function_return_value;
}

auto_SharedLibrary SharedLibrary::open
(
  const Path& file_prefix,
  const char* argv0
)
{
  void* handle;
  if ( ( handle = DLOPEN( file_prefix ) ) != NULL )
    return new SharedLibrary( handle );
  else
  {
    Path file_path = "lib" / file_prefix + SHLIBSUFFIX;

    if ( ( handle = DLOPEN( file_path ) ) != NULL )
      return new SharedLibrary( handle );
    else
    {
      Path file_path = file_prefix + SHLIBSUFFIX;

      if ( ( handle = DLOPEN( file_path ) ) != NULL )
        return new SharedLibrary( handle );
      else
      {
        if ( argv0 != NULL )
        {
          const char* last_slash = strrchr( argv0, Path::SEPARATOR );
          while ( last_slash != NULL && last_slash != argv0 )
          {
            Path file_path = Path( argv0, last_slash - argv0 + 1 )
                             + file_prefix + SHLIBSUFFIX;

            if ( ( handle = DLOPEN( file_path ) ) != NULL )
              return new SharedLibrary( handle );
            else
            {
              Path file_path = Path( argv0, last_slash - argv0 + 1 )
                               / "lib" + file_prefix + SHLIBSUFFIX;

              if ( ( handle = DLOPEN( file_path ) ) != NULL )
                return new SharedLibrary( handle );
            }

            last_slash--;
            while ( *last_slash != Path::SEPARATOR ) last_slash--;
          }
        }
      }
    }
  }

  return NULL;
}


// stat.cpp
#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif


Stat::Stat()
:
#ifndef _WIN32
  dev( static_cast<dev_t>( -1 ) ),
  ino( static_cast<ino_t>( -1 ) ),
#endif
  mode( static_cast<mode_t>( -1 ) ),
  nlink( static_cast<nlink_t>( -1 ) ),
#ifndef _WIN32
  uid( static_cast<uid_t>( -1 ) ),
  gid( static_cast<gid_t>( -1 ) ),
  rdev( static_cast<dev_t>( -1 ) ),
#endif
  size( static_cast<uint64_t>( -1 ) ),
  atime( static_cast<uint64_t>( 0 ) ),
  mtime( static_cast<uint64_t>( 0 ) ),
  ctime( static_cast<uint64_t>( 0 ) ),
#ifndef _WIN32
  blksize( static_cast<blksize_t>( -1 ) ),
  blocks( static_cast<blkcnt_t>( -1 ) )
#else
  attributes( static_cast<uint32_t>( -1 ) )
#endif
{ }

Stat::Stat( const Stat& stbuf )
:
#ifndef _WIN32
  dev( stbuf.get_dev() ),
  ino( stbuf.get_ino() ),
#endif
  mode( stbuf.get_mode() ),
  nlink( stbuf.get_nlink() ),
#ifndef _WIN32
  uid( stbuf.get_uid() ),
  gid( stbuf.get_gid() ),
  rdev( stbuf.get_rdev() ),
#endif
  size( stbuf.get_size() ),
  atime( stbuf.get_atime() ),
  mtime( stbuf.get_mtime() ),
  ctime( stbuf.get_ctime() ),
#ifndef _WIN32
  blksize( stbuf.get_blksize() ),
  blocks( stbuf.get_blocks() )
#else
  attributes( stbuf.get_attributes() )
#endif
{ }

#ifdef _WIN32
Stat::Stat
(
  mode_t mode,
  nlink_t nlink,
  uint64_t size,
  const Time& atime,
  const Time& mtime,
  const Time& ctime,
  uint32_t attributes
)
  : mode( mode ),
    nlink( nlink ),
    size( size ),
    atime( atime ),
    mtime( mtime ),
    ctime( ctime ),
    attributes( attributes )
{ }

Stat::Stat( const BY_HANDLE_FILE_INFORMATION& bhfi )
  : mode
    (
      ( bhfi.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY ) ?
      S_IFDIR :
      S_IFREG
    ),
    nlink( static_cast<nlink_t>( bhfi.nNumberOfLinks ) ),
    atime( bhfi.ftLastAccessTime ),
    mtime( bhfi.ftLastWriteTime ),
    ctime( bhfi.ftCreationTime ),
    attributes( bhfi.dwFileAttributes )
{
  ULARGE_INTEGER size;
  size.LowPart = bhfi.nFileSizeLow;
  size.HighPart = bhfi.nFileSizeHigh;
  this->size = static_cast<size_t>( size.QuadPart );
}

Stat::Stat( const WIN32_FIND_DATA& find_data )
  : mode
    (
      ( find_data.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY ) ?
      S_IFDIR :
      S_IFREG
    ),
    nlink( 1 ), // WIN32_FIND_DATA doesn't have a nNumberOfLinks
    atime( find_data.ftLastAccessTime ),
    mtime( find_data.ftLastWriteTime ),
    ctime( find_data.ftCreationTime ),
    attributes( find_data.dwFileAttributes )
{
  ULARGE_INTEGER size;
  size.LowPart = find_data.nFileSizeLow;
  size.HighPart = find_data.nFileSizeHigh;
  this->size = static_cast<size_t>( size.QuadPart );
}

Stat::Stat
(
  uint32_t nNumberOfLinks,
  uint32_t nFileSizeHigh,
  uint32_t nFileSizeLow,
  const FILETIME* ftLastAccessTime,
  const FILETIME* ftLastWriteTime,
  const FILETIME* ftCreationTime,
  uint32_t dwFileAttributes
)
  : mode
    (
      ( dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY ) ? S_IFDIR : S_IFREG
    ),
    nlink( static_cast<nlink_t>( nNumberOfLinks ) ),
    atime( ftLastAccessTime ),
    mtime( ftLastWriteTime ),
    ctime( ftCreationTime ),
    attributes( dwFileAttributes )
{
  ULARGE_INTEGER size;
  size.LowPart = nFileSizeLow;
  size.HighPart = nFileSizeHigh;
  this->size = static_cast<size_t>( size.QuadPart );
}
#else
Stat::Stat
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
)
: dev( dev ),
  ino( ino ),
  mode( mode ),
  nlink( nlink ),
  uid( uid ),
  gid( gid ),
  rdev( rdev ),
  size( size ),
  atime( atime ),
  mtime( mtime ),
  ctime( ctime ),
  blksize( blksize ),
  blocks( blocks )
{ }
#endif

Stat::Stat( const struct stat& stbuf )
  :
#ifndef _WIN32
    dev( stbuf.st_dev ),
    ino( stbuf.st_ino ),
#endif
    mode( stbuf.st_mode ),
    nlink( stbuf.st_nlink ),
#ifndef _WIN32
    uid( stbuf.st_uid ),
    gid( stbuf.st_gid ),
    rdev( stbuf.st_rdev ),
#endif
    size( stbuf.st_size ),
    atime( static_cast<double>( stbuf.st_atime ) ),
    mtime( static_cast<double>( stbuf.st_mtime ) ),
    ctime( static_cast<double>( stbuf.st_ctime ) )
#ifndef _WIN32
    , blksize( stbuf.st_blksize ),
    blocks( stbuf.st_blocks )
#endif
{ }

#ifdef _WIN32
uint32_t Stat::get_attributes() const
{
#ifdef _WIN32
  DWORD dwFileAttributes = attributes;
  if ( ( get_mode() & S_IFREG ) == S_IFREG )
    dwFileAttributes |= FILE_ATTRIBUTE_NORMAL;
  else if ( ( get_mode() & S_IFDIR ) == S_IFDIR )
    dwFileAttributes |= FILE_ATTRIBUTE_DIRECTORY;
#ifdef _DEBUG
  if ( dwFileAttributes == 0 )
    DebugBreak();
#endif
  return dwFileAttributes;
#else
  return attributes;
#endif
}
#endif

Stat& Stat::operator=( const Stat& other )
{
#ifndef _WIN32
  set_dev( other.get_dev() );
  set_ino( other.get_ino() );
#endif
  set_mode( other.get_mode() );
  set_nlink( other.get_nlink() );
#ifndef _WIN32
  set_uid( other.get_uid() );
  set_gid( other.get_gid() );
  set_rdev( other.get_rdev() );
#endif
  set_size( other.get_size() );
  set_atime( other.get_atime() );
  set_mtime( other.get_mtime() );
  set_ctime( other.get_ctime() );
#ifdef _WIN32
  set_attributes( other.get_attributes() );
#else
  set_blksize( other.get_blksize() );
  set_blocks( other.get_blocks() );
#endif
  return *this;
}

bool Stat::operator==( const Stat& other ) const
{
  return
#ifndef _WIN32
         get_dev() == other.get_dev() &&
         get_ino() == other.get_ino() &&
#endif
         get_mode() == other.get_mode() &&
         get_nlink() == other.get_nlink() &&
#ifndef _WIN32
         get_uid() == other.get_uid() &&
         get_gid() == other.get_gid() &&
         get_rdev() == other.get_rdev() &&
#endif
         get_size() == other.get_size() &&
         get_atime() == other.get_atime() &&
         get_mtime() == other.get_mtime() &&
         get_ctime() == other.get_ctime() &&
#ifdef _WIN32
         get_attributes() == other.get_attributes();
#else
         get_blksize() == other.get_blksize() &&
         get_blocks() == other.get_blocks();
#endif
}

Stat::operator std::string() const
{
  std::ostringstream os;
  operator<<( os, *this );
  return os.str();
}

Stat::operator struct stat() const
{
  struct stat stbuf;
  memset( &stbuf, 0, sizeof( stbuf ) );
#ifndef _WIN32
  stbuf.st_dev = get_dev();
  stbuf.st_ino = get_ino();
#endif
  stbuf.st_mode = get_mode();
  stbuf.st_nlink = get_nlink();
#ifndef _WIN32
  stbuf.st_uid = get_uid();
  stbuf.st_gid = get_gid();
  stbuf.st_rdev = get_rdev();
#endif
  stbuf.st_size = static_cast<off_t>( get_size() );
  stbuf.st_atime = get_atime().as_unix_time_s();
  stbuf.st_mtime = get_mtime().as_unix_time_s();
  stbuf.st_ctime = get_ctime().as_unix_time_s();
#ifndef _WIN32
  stbuf.st_blksize = get_blksize();
  stbuf.st_blocks = get_blocks();
#endif
  return stbuf;
}

#ifdef _WIN32
Stat::operator BY_HANDLE_FILE_INFORMATION() const
{
  BY_HANDLE_FILE_INFORMATION bhfi;
  memset( &bhfi, 0, sizeof( bhfi ) );
  bhfi.dwFileAttributes = get_attributes();
  bhfi.ftCreationTime = get_ctime();
  bhfi.ftLastWriteTime = get_mtime();
  bhfi.ftLastWriteTime = get_mtime();
  ULARGE_INTEGER size; size.QuadPart = get_size();
  bhfi.nFileSizeLow = size.LowPart;
  bhfi.nFileSizeHigh = size.HighPart;
  bhfi.nNumberOfLinks = get_nlink();
  return bhfi;
}

Stat::operator WIN32_FIND_DATA() const
{
  WIN32_FIND_DATA find_data;
  memset( &find_data, 0, sizeof( find_data ) );
  find_data.dwFileAttributes = get_attributes();
  find_data.ftCreationTime = get_ctime();
  find_data.ftLastAccessTime = get_atime();
  find_data.ftLastWriteTime = get_mtime();
  ULARGE_INTEGER size; size.QuadPart = get_size();
  find_data.nFileSizeLow = size.LowPart;
  find_data.nFileSizeHigh = size.HighPart;
  return find_data;
}
#endif

void Stat::set( const Stat& other, uint32_t to_set )
{
  if ( ( to_set & Volume::SETATTR_MODE ) == Volume::SETATTR_MODE )
    set_mode( other.get_mode() );

#ifndef _WIN32
  if ( ( to_set & Volume::SETATTR_UID ) == Volume::SETATTR_UID )
    set_uid( other.get_uid() );

  if ( ( to_set & Volume::SETATTR_GID ) == Volume::SETATTR_GID )
    set_gid( other.get_gid() );
#endif

  if ( ( to_set & Volume::SETATTR_SIZE ) == Volume::SETATTR_SIZE )
    set_size( other.get_size() );

  if ( ( to_set & Volume::SETATTR_ATIME ) == Volume::SETATTR_ATIME )
    set_atime( other.get_atime() );

  if ( ( to_set & Volume::SETATTR_MTIME ) == Volume::SETATTR_MTIME )
    set_mtime( other.get_mtime() );

  if ( ( to_set & Volume::SETATTR_CTIME ) == Volume::SETATTR_CTIME )
    set_ctime( other.get_ctime() );

#ifdef _WIN32
  if ( ( to_set & Volume::SETATTR_ATTRIBUTES ) == Volume::SETATTR_ATTRIBUTES )
    set_attributes( other.get_attributes() );
#endif
}

#ifndef _WIN32
void Stat::set_dev( dev_t dev )
{
  this->dev = dev;
}

void Stat::set_ino( ino_t ino )
{
  this->ino = ino;
}
#endif

void Stat::set_mode( mode_t mode )
{
  this->mode = mode;
}

void Stat::set_nlink( nlink_t nlink )
{
  this->nlink = nlink;
}

#ifndef _WIN32
void Stat::set_uid( uid_t uid )
{
  this->uid = uid;
}

void Stat::set_gid( gid_t gid )
{
  this->gid = gid;
}

void Stat::set_rdev( dev_t )
{
  this->rdev = rdev;
}
#endif

void Stat::set_size( uint64_t size )
{
  this->size = size;
}

void Stat::set_atime( const Time& atime )
{
  this->atime = atime;
}

void Stat::set_mtime( const Time& mtime )
{
  this->mtime = mtime;
}

void Stat::set_ctime( const Time& ctime )
{
  this->ctime = ctime;
}
#ifdef _WIN32
void Stat::set_attributes( uint32_t attributes )
{
  this->attributes = attributes;
}
#else
void Stat::set_blksize( blksize_t blksize )
{
  this->blksize = blksize;
}

void Stat::set_blocks( blkcnt_t blocks )
{
  this->blocks = blocks;
}
#endif

#ifdef _WIN32
#pragma warning( pop )
#endif


// thread.cpp
#ifdef _WIN32
#include <windows.h>
#else
#if defined(__linux__)
#include <sched.h>
#include <sys/syscall.h>
#elif defined(__sun)
#include <thread.h>
#include <sys/processor.h>
#include <sys/pset.h>
#endif
#endif


Thread::Thread()
{
  handle = 0;
  id = 0;
}

Thread::~Thread()
{
#ifdef _WIN32
  if ( handle ) CloseHandle( handle );
#endif
}

void* Thread::getspecific( unsigned long key )
{
#ifdef _WIN32
    return TlsGetValue( key );
#else
    return pthread_getspecific( key );
#endif
}

unsigned long Thread::gettid()
{
#if defined(_WIN32)
  return GetCurrentThreadId();
#elif defined(__linux__)
  return syscall( SYS_gettid );
#elif defined(__sun)
  return thr_self();
#else
  return 0;
#endif
}

unsigned long Thread::key_create()
{
#ifdef _WIN32
  return TlsAlloc();
#else
  unsigned long key;
  pthread_key_create( reinterpret_cast<pthread_key_t*>( &key ), NULL );
  return key;
#endif
}

void Thread::nanosleep( const Time& timeout )
{
#ifdef _WIN32
  Sleep( static_cast<DWORD>( timeout.as_unix_time_ms() ) );
#else
  struct timespec timeout_ts = timeout;
  ::nanosleep( &timeout_ts, NULL );
#endif
}

#ifdef _WIN32
//
// Usage: SetThreadName (-1, "MainThread");
// from http://msdn.microsoft.com/library/default.asp?url=/library/en-us/vsdebug/html/vxtsksettingthreadname.asp
//
typedef struct tagTHREADNAME_INFO
{
  DWORD dwType; // must be 0x1000
  LPCSTR szName; // pointer to name (in user addr space)
  DWORD dwThreadID; // thread ID (-1=caller thread)
  DWORD dwFlags; // reserved for future use, must be zero
}
THREADNAME_INFO;
#endif

void Thread::set_name( const char* thread_name )
{
#ifdef _WIN32
  THREADNAME_INFO info;
  info.dwType = 0x1000;
  info.szName = thread_name;
  info.dwThreadID = id;
  info.dwFlags = 0;

  __try
  {
      RaiseException
      (
        0x406D1388,
        0,
        sizeof( info ) / sizeof( DWORD ),
        reinterpret_cast<const ULONG_PTR*>( &info )
      );
  }
  __except( EXCEPTION_CONTINUE_EXECUTION )
  {}
#endif
}

bool Thread::set_processor_affinity( unsigned short logical_processor_i )
{
  if ( id != 0 )
  {
#if defined(_WIN32)
    return SetThreadAffinityMask( handle, ( 1L << logical_processor_i ) ) != 0;
#elif defined(__linux__)
    cpu_set_t cpu_set;
    CPU_ZERO( &cpu_set );
    CPU_SET( logical_processor_i, &cpu_set );
    return sched_setaffinity( 0, sizeof( cpu_set ), &cpu_set ) == 0;
#elif defined(__sun)
    return processor_bind( P_LWPID, id, logical_processor_i, NULL ) == 0;
#else
    return false;
#endif
  }
  else
    return false;
}

bool Thread::set_processor_affinity
(
  const ProcessorSet& logical_processor_set
)
{
  if ( id != 0 )
  {
#if defined(_WIN32)
    return SetThreadAffinityMask( handle, logical_processor_set.mask ) != 0;
#elif defined(__linux__)
    return sched_setaffinity
     (
       0,
       sizeof( cpu_set_t ),
       static_cast<cpu_set_t*>( logical_processor_set.cpu_set )
      ) == 0;
#elif defined(__sun)
    return pset_bind( logical_processor_set.psetid, P_LWPID, id, NULL ) == 0;
#else
    return false;
#endif
  }
  else
    return false;
}

void Thread::setspecific( unsigned long key, void* value )
{
#ifdef _WIN32
    TlsSetValue( key, value );
#else
  pthread_setspecific( key, value );
#endif
}

void Thread::start()
{
#ifdef _WIN32
  handle = CreateThread( NULL, 0, thread_stub, this, NULL, &id );
#else
  pthread_attr_t attr;
  pthread_attr_init( &attr );
  pthread_attr_setdetachstate( &attr, PTHREAD_CREATE_DETACHED );
  pthread_create( &handle, &attr, &thread_stub, this );
  pthread_attr_destroy( &attr );
#endif
}

#ifdef _WIN32
unsigned long __stdcall Thread::thread_stub( void* this_ )
#else
void* Thread::thread_stub( void* this_ )
#endif
{
#if defined(__linux__)
  static_cast<Thread*>( this_ )->id = syscall( SYS_gettid );
#elif defined(__MACH__)
  static_cast<Thread*>( this_ )->id = 0; // ???
#elif defined(__sun)
  static_cast<Thread*>( this_ )->id = thr_self();
#endif

  static_cast<Thread*>( this_ )->run();

  return 0;
}

void Thread::yield()
{
#if defined(_WIN32)
  SwitchToThread();
#elif defined(__MACH__)
  pthread_yield_np();
#elif defined(__sun)
  sleep( 0 );
#else
  pthread_yield();
#endif
}


// time.cpp
#if defined(_WIN32)
#include <windows.h> // For FILETIME
#include <winsock.h> // For timeval
#elif defined(__MACH__)
#include <sys/time.h> // For gettimeofday
#endif


const char* HTTPDaysOfWeek[]
  = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

const char* ISOMonths[]
  = {
      "Jan", "Feb", "Mar", "Apr", "May", "Jun",
      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };


#ifdef _WIN32
static inline ULONGLONG FILETIMEToUnixTimeNS( const FILETIME& file_time )
{
  ULARGE_INTEGER file_time_combined;
  file_time_combined.LowPart = file_time.dwLowDateTime;
  file_time_combined.HighPart = file_time.dwHighDateTime;
  // Subtract the number of 100-ns intervals between
  // January 1, 1601 and January 1, 1970
  file_time_combined.QuadPart -= 116444736000000000;
  file_time_combined.QuadPart *= 100; // Into nanoseconds
  return file_time_combined.QuadPart;
}

static inline ULONG FILETIMEToUnixTimeS( const FILETIME& file_time )
{
  return static_cast<ULONG>( FILETIMEToUnixTimeNS( file_time ) / 1000000000 );
}

static inline ULONGLONG FILETIMEToUnixTimeNS( const FILETIME* file_time )
{
  if ( file_time )
    return FILETIMEToUnixTimeNS( *file_time );
  else
    return 0;
}

// Adapted from http://support.microsoft.com/kb/167296
static inline FILETIME UnixTimeSToFILETIME( uint32_t unix_time_s )
{
 LONGLONG ll = Int32x32To64( unix_time_s, 10000000 ) + 116444736000000000;
 FILETIME file_time;
 file_time.dwLowDateTime = static_cast<DWORD>( ll );
 file_time.dwHighDateTime = ll >> 32;
 return file_time;
}

static inline FILETIME UnixTimeNSToFILETIME( uint64_t unix_time_ns )
{
  // Add the difference in nanoseconds between
  // January 1, 1601 (start of the Windows epoch) and
  // January 1, 1970 (start of the Unix epoch)
  unix_time_ns += 11644473600000000000;
  uint64_t unix_time_100_ns_intervals = unix_time_ns / 100;
  FILETIME file_time;
  file_time.dwLowDateTime = static_cast<DWORD>( unix_time_100_ns_intervals );
  file_time.dwHighDateTime = unix_time_100_ns_intervals >> 32;
  return file_time;
}

static inline SYSTEMTIME UnixTimeNSToUTCSYSTEMTIME( uint64_t unix_time_ns )
{
  FILETIME file_time = UnixTimeNSToFILETIME( unix_time_ns );
  SYSTEMTIME system_time;
  FileTimeToSystemTime( &file_time, &system_time );
  return system_time;
}

static inline SYSTEMTIME UnixTimeNSToLocalSYSTEMTIME( uint64_t unix_time_ns )
{
  SYSTEMTIME utc_system_time = UnixTimeNSToUTCSYSTEMTIME( unix_time_ns );
  TIME_ZONE_INFORMATION time_zone_information;
  GetTimeZoneInformation( &time_zone_information );
  SYSTEMTIME local_system_time;
  SystemTimeToTzSpecificLocalTime
  (
    &time_zone_information,
    &utc_system_time,
    &local_system_time
  );
  return local_system_time;
}
#endif


Time::Time()
{
#if defined(_WIN32)
  FILETIME file_time;
  GetSystemTimeAsFileTime( &file_time );
  unix_time_ns = FILETIMEToUnixTimeNS( file_time );
#elif defined(__MACH__)
  struct timeval tv;
  gettimeofday( &tv, NULL );
  unix_time_ns = tv.tv_sec * NS_IN_S + tv.tv_usec * NS_IN_US;
#else
  // POSIX real time
  struct timespec ts;
  clock_gettime( CLOCK_REALTIME, &ts );
  unix_time_ns = ts.tv_sec * NS_IN_S + ts.tv_nsec;
#endif
}

Time::Time( const struct timeval& tv )
{
  unix_time_ns = tv.tv_sec * NS_IN_S + tv.tv_usec * NS_IN_US;
}

#ifdef _WIN32
Time::Time( const FILETIME& file_time )
{
  unix_time_ns = FILETIMEToUnixTimeNS( file_time );
}

Time::Time( const FILETIME* file_time )
{
  unix_time_ns = FILETIMEToUnixTimeNS( file_time );
}
#else
Time::Time( const struct timespec& ts )
{
  unix_time_ns = ts.tv_sec * NS_IN_S + ts.tv_nsec;
}
#endif

void Time::as_common_log_date_time( char* out_str, uint8_t out_str_len ) const
{
#ifdef _WIN32
  SYSTEMTIME local_system_time = UnixTimeNSToLocalSYSTEMTIME( unix_time_ns );
  TIME_ZONE_INFORMATION win_tz;
  GetTimeZoneInformation( &win_tz );

  // 10/Oct/2000:13:55:36 -0700
  _snprintf_s( out_str, out_str_len, _TRUNCATE,
          "%02d/%s/%04d:%02d:%02d:%02d %+0.4d",
            local_system_time.wDay,
            ISOMonths[local_system_time.wMonth-1],
            local_system_time.wYear,
            local_system_time.wHour,
            local_system_time.wMinute,
            local_system_time.wSecond,
            ( win_tz.Bias / 60 ) * -100 );
#else
  time_t unix_time_s = static_cast<time_t>( unix_time_ns / NS_IN_S );
  struct tm unix_tm;
  localtime_r( &unix_time_s, &unix_tm );

  snprintf( out_str, out_str_len,
            "%02d/%s/%04d:%02d:%02d:%02d %d",
            unix_tm.tm_mday,
            ISOMonths[unix_tm.tm_mon],
            unix_tm.tm_year + 1900,
            unix_tm.tm_hour,
            unix_tm.tm_min,
            unix_tm.tm_sec,
            0 ); // Could use the extern timezone,
                 // which is supposed to be secs west of GMT.
#endif
}

void Time::as_http_date_time( char* out_str, uint8_t out_str_len ) const
{
#ifdef _WIN32
  SYSTEMTIME utc_system_time = UnixTimeNSToUTCSYSTEMTIME( unix_time_ns );

  _snprintf_s
  (
    out_str,
    out_str_len,
    _TRUNCATE,
    "%s, %02d %s %04d %02d:%02d:%02d GMT",
    HTTPDaysOfWeek[utc_system_time.wDayOfWeek],
    utc_system_time.wDay,
    ISOMonths[utc_system_time.wMonth-1],
    utc_system_time.wYear,
    utc_system_time.wHour,
    utc_system_time.wMinute,
    utc_system_time.wSecond
  );
#else
  time_t unix_time_s = static_cast<time_t>( unix_time_ns / NS_IN_S );
  struct tm unix_tm;
  gmtime_r( &unix_time_s, &unix_tm );

  snprintf
  (
    out_str,
    out_str_len,
    "%s, %02d %s %04d %02d:%02d:%02d GMT",
    HTTPDaysOfWeek[unix_tm.tm_wday],
    unix_tm.tm_mday,
    ISOMonths[unix_tm.tm_mon],
    unix_tm.tm_year + 1900,
    unix_tm.tm_hour,
    unix_tm.tm_min,
    unix_tm.tm_sec
  );
#endif
}

/*
uint64_t Time::parseHTTPDateTimeToUnixTimeNS( const char* date_str )
{
  char day[4], month[4];

#ifdef _WIN32
  SYSTEMTIME utc_system_time;

  int sf_ret
    = sscanf
      (
        date_str,
        "%03s, %02d %03s %04d %02d:%02d:%02d GMT",
        &day,
        &utc_system_time.wDay,
        &month,
        &utc_system_time.wYear,
        &utc_system_time.wHour,
        &utc_system_time.wMinute,
        &utc_system_time.wSecond
      );

  if ( sf_ret != 7 )
    return 0;

  for
  (
    utc_system_time.wDayOfWeek = 0;
    utc_system_time.wDayOfWeek < 7;
    utc_system_time.wDayOfWeek++
  )
  {
    if ( strcmp( day, HTTPDaysOfWeek[utc_system_time.wDayOfWeek] ) == 0 )
      break;
  }

  for
  (
    utc_system_time.wMonth = 0;
    utc_system_time.wMonth < 12;
    utc_system_time.wMonth++
  )
  {
    if ( strcmp( month, ISOMonths[utc_system_time.wMonth] ) == 0 )
      break;
  }
  utc_system_time.wMonth++; // Windows starts the months from 1

  FILETIME file_time;
  SystemTimeToFileTime( &utc_system_time, &file_time );
  return FILETIMEToUnixTimeNS( file_time );
#else
  struct tm unix_tm;

  int sf_ret
    = sscanf
      (
        date_str,
        "%03s, %02d %03s %04d %02d:%02d:%02d GMT",
        &day,
        &unix_tm.tm_mday,
        &month,
        &unix_tm.tm_year,
        &unix_tm.tm_hour,
        &unix_tm.tm_min,
        &unix_tm.tm_sec
      );

  if ( sf_ret != 7 )
    return 0;

  unix_tm.tm_year -= 1900;

  for ( unix_tm.tm_wday = 0; unix_tm.tm_wday < 7; unix_tm.tm_wday++ )
    if ( strcmp( day, HTTPDaysOfWeek[unix_tm.tm_wday] ) == 0 )
      break;

  for ( unix_tm.tm_mon = 0; unix_tm.tm_mon < 12; unix_tm.tm_mon++ )
    if ( strcmp( month, ISOMonths[unix_tm.tm_mon] ) == 0 )
      break;

  time_t unix_time_s = mktime( &unix_tm ); // mktime is thread-safe

  return unix_time_s * NS_IN_S;
#endif
}
*/

void Time::as_iso_date( char* out_str, uint8_t out_str_len ) const
{
#ifdef _WIN32
  SYSTEMTIME local_system_time = UnixTimeNSToLocalSYSTEMTIME( unix_time_ns );
  _snprintf_s
  (
    out_str,
    out_str_len,
    _TRUNCATE,
    "%04d-%02d-%02d",
    local_system_time.wYear,
    local_system_time.wMonth,
    local_system_time.wDay
  );
#else
  time_t unix_time_s = static_cast<time_t>( unix_time_ns / NS_IN_S );
  struct tm unix_tm;
  localtime_r( &unix_time_s, &unix_tm );
  snprintf
  (
    out_str,
    out_str_len,
    "%04d-%02d-%02d",
    unix_tm.tm_year + 1900,
    unix_tm.tm_mon + 1,
    unix_tm.tm_mday
  );
#endif
}

void Time::as_iso_date_time( char* out_str, uint8_t out_str_len ) const
{
#ifdef _WIN32
  SYSTEMTIME local_system_time = UnixTimeNSToLocalSYSTEMTIME( unix_time_ns );

  _snprintf_s
  (
    out_str,
    out_str_len,
    _TRUNCATE,
    "%04d-%02d-%02dT%02d:%02d:%02d.000Z",
    local_system_time.wYear,
    local_system_time.wMonth,
    local_system_time.wDay,
    local_system_time.wHour,
    local_system_time.wMinute,
    local_system_time.wSecond
  );
#else
  time_t unix_time_s = static_cast<time_t>(  unix_time_ns / NS_IN_S );
  struct tm unix_tm;
  localtime_r( &unix_time_s, &unix_tm );

  snprintf
  (
    out_str,
    out_str_len,
    "%04d-%02d-%02dT%02d:%02d:%02d.000Z",
    unix_tm.tm_year + 1900,
    unix_tm.tm_mon + 1,
    unix_tm.tm_mday,
    unix_tm.tm_hour,
    unix_tm.tm_min,
    unix_tm.tm_sec
  );
#endif
}

Time::operator struct timeval() const
{
  struct timeval tv;
#ifdef _WIN32
  tv.tv_sec = static_cast<long>( unix_time_ns / NS_IN_S );
#else
  tv.tv_sec = static_cast<time_t>( unix_time_ns / NS_IN_S );
#endif
  tv.tv_usec = ( unix_time_ns % NS_IN_S ) / NS_IN_US;
  return tv;
}

#ifdef _WIN32
Time::operator FILETIME() const
{
  return UnixTimeNSToFILETIME( unix_time_ns );
}
#else
Time::operator struct timespec() const
{
  struct timespec ts;
  ts.tv_sec = unix_time_ns / NS_IN_S;
  ts.tv_nsec = unix_time_ns % NS_IN_S;
  return ts;
}
#endif

Time::operator std::string() const
{
  char iso_date_time[30];
  as_iso_date_time( iso_date_time, 30 );
  return iso_date_time;
}


// timer_queue.cpp
#ifdef _WIN32
#include <windows.h>
#endif

#include <queue>
#include <utility>


TimerQueue* TimerQueue::default_timer_queue = NULL;


TimerQueue::TimerQueue()
{
#ifdef _WIN32
  hTimerQueue = CreateTimerQueue();
#else
  thread.start();
#endif
}

#ifdef _WIN32
TimerQueue::TimerQueue( HANDLE hTimerQueue )
  : hTimerQueue( hTimerQueue )
{ }
#endif

TimerQueue::~TimerQueue()
{
#ifdef _WIN32
  if ( hTimerQueue != NULL )
    DeleteTimerQueueEx( hTimerQueue, NULL );
#else
  thread.stop();
#endif
}

void TimerQueue::addTimer( yidl::runtime::auto_Object<Timer> timer )
{
#ifdef _WIN32
  timer->hTimerQueue = hTimerQueue;
  CreateTimerQueueTimer
  (
    &timer->hTimer,
    hTimerQueue,
    Timer::WaitOrTimerCallback,
    &timer->incRef(),
    static_cast<DWORD>( timer->get_timeout().as_unix_time_ms() ),
    static_cast<DWORD>( timer->get_period().as_unix_time_ms() ),
    WT_EXECUTEDEFAULT
  );
#else
  thread.addTimer( timer.release() );
#endif
}

void TimerQueue::destroyDefaultTimerQueue()
{
  if ( default_timer_queue != NULL )
    delete default_timer_queue;
}

TimerQueue& TimerQueue::getDefaultTimerQueue()
{
  if ( default_timer_queue == NULL )
#ifdef _WIN32
    default_timer_queue = new TimerQueue( NULL );
#else
    default_timer_queue = new TimerQueue;
#endif
  return *default_timer_queue;
}


TimerQueue::Timer::Timer( const Time& timeout )
  : period( static_cast<uint64_t>( 0 ) ), timeout( timeout )
{
#ifdef _WIN32
  hTimer = hTimerQueue = NULL;
#endif
}

TimerQueue::Timer::Timer( const Time& timeout, const Time& period )
  : period( period ), timeout( timeout )
{
#ifdef _WIN32
  hTimer = hTimerQueue = NULL;
#else
  deleted = false;
#endif
}

TimerQueue::Timer::~Timer()
{ }

void TimerQueue::Timer::delete_()
{
#ifdef _WIN32
  CancelTimerQueueTimer( hTimerQueue, hTimer );
#else
  deleted = true;
#endif
}

#ifndef _WIN32
TimerQueue::Thread::Thread()
{
   should_run = true;
}

void TimerQueue::Thread::run()
{
  set_name( "TimerQueueThread" );

  while ( should_run )
  {
    if ( timers.empty() )
    {
      TimerQueue::Timer* new_timer = new_timers_queue.dequeue();
      if ( new_timer != NULL )
      {
        timers.push
        (
          std::make_pair
          (
            Time() + new_timer->get_timeout(),
            new_timer
          )
        );
      }
      else
        break;
    }
    else
    {
      Time current_time;
      if ( timers.top().first <= current_time )
      // Earliest timer has expired, fire it
      {
        TimerQueue::Timer* timer = timers.top().second;
        timers.pop();

        if ( !timer->deleted )
        {
          timer->fire();

          if ( timer->get_period() != static_cast<uint64_t>( 0 ) )
          {
            timer->last_fire_time = Time();
            timers.push
            (
              std::make_pair
              (
                timer->last_fire_time + timer->get_period(),
                timer
              )
            );
          }
          else
            TimerQueue::Timer::decRef( *timer );
        }
        else
          TimerQueue::Timer::decRef( *timer );
      }
      else // Wait on the new timers queue until a new timer arrives
           // or it's time to fire the next timer
      {
        TimerQueue::Timer* new_timer
          = new_timers_queue.timed_dequeue
            (
              timers.top().first - current_time
            );

        if ( new_timer != NULL )
        {
          timers.push
          (
            std::make_pair
            (
              Time() + new_timer->get_timeout(),
              new_timer
            )
          );
        }
      }
    }
  }
}

void TimerQueue::Thread::stop()
{
  should_run = false;
  new_timers_queue.enqueue( NULL );
}
#endif

#ifdef _WIN32
VOID CALLBACK TimerQueue::Timer::WaitOrTimerCallback
(
  PVOID lpParameter,
  BOOLEAN
)
{
  Timer* this_ = static_cast<Timer*>( lpParameter );

  Time elapsed_time( Time() - this_->last_fire_time );
  if ( elapsed_time > 0ULL )
  {
    this_->fire();

    if ( this_->get_period() == 0ULL )
      TimerQueue::Timer::decRef( *this_ );
    else
      this_->last_fire_time = Time();
  }
  else
    this_->last_fire_time = Time();
}
#endif


// volume.cpp
#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#else
#include <dirent.h>
#include <sys/statvfs.h>
#include <sys/time.h>
#include <utime.h>
#ifdef YIELD_HAVE_XATTR_H
#if defined(__linux__)
#include <sys/xattr.h>
#define LISTXATTR ::listxattr
#define GETXATTR ::getxattr
#define SETXATTR ::setxattr
#define REMOVEXATTR ::removexattr
#elif defined(__MACH__)
#include <sys/xattr.h>
#define LISTXATTR( path, namebuf, size ) \
  ::listxattr( path, namebuf, size, 0 )
#define GETXATTR( path, name, value, size ) \
  ::getxattr( path, name, value, size, 0, 0 )
#define SETXATTR( path, name, value, size, flags ) \
  ::setxattr( path, name, value, size, 0, flags )
#define REMOVEXATTR( path, name ) \
  ::removexattr( path, name, 0 )
#endif
#endif
#endif


bool Volume::access( const Path& path, int amode )
{
#ifdef _WIN32
  ::SetLastError( ERROR_NOT_SUPPORTED );
  return false;
#else
  return ::access( path, amode ) >= 0;
#endif
}

#ifndef _WIN32
bool Volume::chmod( const Path& path, mode_t mode )
{
  auto_Stat stbuf( new Stat );
  stbuf->set_mode( mode );
  return setattr( path, stbuf, SETATTR_MODE );
}

bool Volume::chown( const Path& path, uid_t uid, uid_t gid )
{
  auto_Stat stbuf( new Stat );
  stbuf->set_uid( uid );
  stbuf->set_gid( gid );
  return setattr( path, stbuf, SETATTR_UID|SETATTR_GID );
}
#endif

auto_File Volume::creat( const Path& path )
{
  return creat( path, FILE_MODE_DEFAULT );
}

auto_File Volume::creat( const Path& path, mode_t mode )
{
  return open( path, O_CREAT|O_WRONLY|O_TRUNC, mode );
}

bool Volume::exists( const Path& path )
{
  return getattr( path ) != NULL;
}

bool Volume::isdir( const Path& path )
{
  auto_Stat stbuf( getattr( path ) );
  return stbuf != NULL && stbuf->ISDIR();
}

bool Volume::isfile( const Path& path )
{
  auto_Stat stbuf( getattr( path ) );
  return stbuf != NULL && stbuf->ISREG();
}

auto_Stat Volume::getattr( const Path& path )
{
#ifdef _WIN32
  WIN32_FIND_DATA find_data;
  HANDLE hFindFirstFile = FindFirstFile( path, &find_data );
  if ( hFindFirstFile != INVALID_HANDLE_VALUE )
  {
    FindClose( hFindFirstFile );
    return new Stat( find_data );
  }
#else
  struct stat stbuf;
  if ( ::stat( path, &stbuf ) != -1 )
    return new Stat( stbuf );
#endif
  return NULL;
}

bool Volume::getxattr
(
  const Path& path,
  const std::string& name,
  std::string& out_value
)
{
#if defined(YIELD_HAVE_XATTR_H)
  ssize_t value_len = GETXATTR( path, name.c_str(), NULL, 0 );
  if ( value_len != -1 )
  {
    char* value = new char[value_len];
    GETXATTR( path, name.c_str(), value, value_len );
    out_value.assign( value, value_len );
    delete [] value;
    return true;
  }
#elif defined(_WIN32)
  ::SetLastError( ERROR_NOT_SUPPORTED );
#else
  errno = ENOTSUP;
#endif

  return false;
}

bool Volume::link( const Path& old_path, const Path& new_path )
{
#ifdef _WIN32
  return CreateHardLink( new_path, old_path, NULL ) != 0;
#else
  return ::symlink( old_path, new_path ) != -1;
#endif
}

bool Volume::listxattr( const Path& path, std::vector<std::string>& out_names )
{
#if defined(YIELD_HAVE_XATTR_H)
  size_t names_len = LISTXATTR( path, NULL, 0 );
  if ( names_len > 0 )
  {
    char* names = new char[names_len];
    LISTXATTR( path, names, names_len );
    char* name = names;
    do
    {
      size_t name_len = strlen( name );
      out_names.push_back( std::string( name, name_len ) );
      name += name_len;
    }
    while ( static_cast<size_t>( name - names ) < names_len );
    delete [] names;
  }
  return true;
#elif defined(_WIN32)
  ::SetLastError( ERROR_NOT_SUPPORTED );
#else
  errno = ENOTSUP;
#endif

  return false;
}

bool Volume::makedirs( const Path& path )
{
  return mktree( path, DIRECTORY_MODE_DEFAULT );
}

bool Volume::makedirs( const Path& path, mode_t mode )
{
  return mktree( path, mode );
}

bool Volume::mkdir( const Path& path )
{
  return mkdir( path, DIRECTORY_MODE_DEFAULT );
}

bool Volume::mkdir( const Path& path, mode_t mode )
{
#ifdef _WIN32
  return CreateDirectoryW( path, NULL ) != 0;
#else
  return ::mkdir( path, mode ) != -1;
#endif
}

bool Volume::mktree( const Path& path )
{
  return mktree( path, DIRECTORY_MODE_DEFAULT );
}

bool Volume::mktree( const Path& path, mode_t mode )
{
  bool ret = true;

  std::pair<Path, Path> path_parts = path.split();
  if ( !path_parts.first.empty() )
    ret &= mktree( path_parts.first, mode );

  if ( !exists( path ) && !mkdir( path, mode ) )
      return false;

  return ret;
}

auto_File Volume::open( const Path& path )
{
  return open( path, O_RDONLY, FILE_MODE_DEFAULT, 0 );
}

auto_File Volume::open( const Path& path, uint32_t flags )
{
  return open( path, flags, FILE_MODE_DEFAULT, 0 );
}

auto_File Volume::open( const Path& path, uint32_t flags, mode_t mode )
{
  return open( path, flags, mode, 0 );
}

auto_File
Volume::open
(
  const Path& path,
  uint32_t flags,
  mode_t mode,
  uint32_t attributes
)
{
#ifdef _WIN32
  DWORD file_access_flags = 0,
        file_create_flags = 0,
        file_open_flags = attributes|FILE_FLAG_SEQUENTIAL_SCAN;

  if ( ( flags & O_APPEND ) == O_APPEND )
    file_access_flags |= FILE_APPEND_DATA;
  else if ( ( flags & O_RDWR ) == O_RDWR )
    file_access_flags |= GENERIC_READ|GENERIC_WRITE;
  else if ( ( flags & O_WRONLY ) == O_WRONLY )
    file_access_flags |= GENERIC_WRITE;
  else
    file_access_flags |= GENERIC_READ;

  if ( ( flags & O_CREAT ) == O_CREAT )
  {
    if ( ( flags & O_TRUNC ) == O_TRUNC )
      file_create_flags = CREATE_ALWAYS;
    else
      file_create_flags = OPEN_ALWAYS;
  }
  else
    file_create_flags = OPEN_EXISTING;

//  if ( ( flags & O_SPARSE ) == O_SPARSE )
//    file_open_flags |= FILE_ATTRIBUTE_SPARSE_FILE;

  if ( ( flags & O_SYNC ) == O_SYNC )
    file_open_flags |= FILE_FLAG_WRITE_THROUGH;

  if ( ( flags & O_DIRECT ) == O_DIRECT )
    file_open_flags |= FILE_FLAG_NO_BUFFERING;

  if ( ( flags & O_ASYNC ) == O_ASYNC )
    file_open_flags |= FILE_FLAG_OVERLAPPED;

  if ( ( flags & O_HIDDEN ) == O_HIDDEN )
    file_open_flags = FILE_ATTRIBUTE_HIDDEN;

  HANDLE fd = CreateFileW
              (
                path,
                file_access_flags,
                FILE_SHARE_DELETE|FILE_SHARE_READ|FILE_SHARE_WRITE,
                NULL,
                file_create_flags,
                file_open_flags,
                NULL
              );

  if ( fd != INVALID_HANDLE_VALUE )
  {
    if ( ( flags & O_TRUNC ) == O_TRUNC && ( flags & O_CREAT ) != O_CREAT )
    {
      SetFilePointer( fd, 0, NULL, FILE_BEGIN );
      SetEndOfFile( fd );
    }

    return new File( fd );
  }
#else
  int fd = ::open( path, flags, mode );
  if ( fd != -1 )
    return new File( fd );
#endif

  return NULL;
}

auto_Directory Volume::opendir( const Path& path )
{
#ifdef _WIN32
  std::wstring search_pattern( path );
  if ( search_pattern.size() > 0 &&
       search_pattern[search_pattern.size()-1] != L'\\' )
    search_pattern.append( L"\\" );
  search_pattern.append( L"*" );

  WIN32_FIND_DATA find_data;
  HANDLE hDirectory = FindFirstFileW( search_pattern.c_str(), &find_data );
  if ( hDirectory != INVALID_HANDLE_VALUE )
    return new Directory( hDirectory, find_data );
#elif !defined(__sun)
  DIR* dirp = ::opendir( path );
  if ( dirp != NULL )
    return new Directory( dirp );
#else
  errno = ENOTSUP;
#endif

  return NULL;
}

auto_Path Volume::readlink( const Path& path )
{
#ifdef _WIN32
  ::SetLastError( ERROR_NOT_SUPPORTED );
  return NULL;
#else
  char out_path[PATH_MAX];
  ssize_t out_path_len = ::readlink( path, out_path, PATH_MAX );
  if ( out_path_len > 0 )
    return new Path( out_path, out_path_len );
  else
    return NULL;
#endif
}

bool Volume::removexattr( const Path& path, const std::string& name )
{
#if defined(YIELD_HAVE_XATTR_H)
  return REMOVEXATTR( path, name.c_str() ) != -1;
#elif defined(_WIN32)
  ::SetLastError( ERROR_NOT_SUPPORTED );
#else
  errno = ENOTSUP;
#endif
  return false;
}

bool Volume::rename( const Path& from_path, const Path& to_path )
{
#ifdef _WIN32
  return MoveFileExW( from_path, to_path, MOVEFILE_REPLACE_EXISTING ) != 0;
#else
  return ::rename( from_path, to_path ) != -1;
#endif
}

bool Volume::rmdir( const Path& path )
{
#ifdef _WIN32
  return RemoveDirectoryW( path ) != 0;
#else
  return ::rmdir( path ) != -1;
#endif
}

bool Volume::rmtree( const Path& path )
{
  auto_Directory dir = opendir( path );
  if ( dir != NULL )
  {
    Directory::auto_Entry dirent = dir->readdir();
    while ( dirent != NULL )
    {
      if ( dirent->ISDIR() )
      {
        if
        (
          dirent->get_name() != Path( "." )
          &&
          dirent->get_name() != Path( ".." )
        )
        {
          if ( !rmtree( path / dirent->get_name() ) )
            return false;
        }
      }
      else
      {
        if ( !unlink( path / dirent->get_name() ) )
          return false;
      }

      dirent = dir->readdir();
    }

    return rmdir( path );
  }
  else
    return false;
}

bool Volume::setattr( const Path& path, auto_Stat stbuf, uint32_t to_set )
{
#ifdef _WIN32
  if
  (
    ( to_set & SETATTR_ATIME ) == SETATTR_ATIME ||
    ( to_set & SETATTR_MTIME ) == SETATTR_MTIME ||
    ( to_set & SETATTR_CTIME ) == SETATTR_CTIME
  )
  {
    auto_File file = open( path, O_WRONLY );
    if ( file!= NULL )
    {
      FILETIME ftCreationTime = stbuf->get_ctime(),
               ftLastAccessTime = stbuf->get_atime(),
               ftLastWriteTime = stbuf->get_mtime();

      return SetFileTime
             (
               *file,
               ( to_set & SETATTR_CTIME ) == SETATTR_CTIME
                 ? &ftCreationTime : NULL,
               ( to_set & SETATTR_ATIME ) == SETATTR_ATIME
                 ? &ftLastAccessTime : NULL,
               ( to_set & SETATTR_MTIME ) == SETATTR_MTIME
                 ? &ftLastWriteTime : NULL
             ) != 0;
    }
    else
      return false;
  }

  if ( ( to_set & SETATTR_ATTRIBUTES ) == SETATTR_ATTRIBUTES )
  {
    if ( SetFileAttributes( path, stbuf->get_attributes() ) == 0 )
      return false;
  }
#else
  if ( ( to_set & SETATTR_MODE ) == SETATTR_MODE )
  {
    if ( ::chmod( path, stbuf->get_mode() ) == -1 )
      return false;
  }

  if ( ( to_set & SETATTR_UID ) == SETATTR_UID )
  {
    if ( ( to_set & SETATTR_GID ) == SETATTR_GID ) // Change both
    {
      if ( ::chown( path, stbuf->get_uid(), stbuf->get_gid() ) == -1 )
        return false;
    }
    else // Only change the uid
    {
      if ( ::chown( path, stbuf->get_uid(), -1 ) == -1 )
        return false;
    }
  }
  else if ( ( to_set & SETATTR_GID ) == SETATTR_GID ) // Only change the gid
  {
    if ( ::chown( path, -1, stbuf->get_gid() ) == -1 )
      return false;
  }

  if
  (
    ( to_set & SETATTR_ATIME ) == SETATTR_ATIME ||
    ( to_set & SETATTR_MTIME ) == SETATTR_MTIME
  )
  {
    struct timeval tv[2];
    tv[0] = stbuf->get_atime();
    tv[1] = stbuf->get_mtime();
    if ( ::utimes( path, tv ) == -1 )
      return false;
  }
#endif

  return true;
}

bool
Volume::setxattr
(
  const Path& path,
  const std::string& name,
  const std::string& value,
  int flags
)
{
#if defined(YIELD_HAVE_XATTR_H)
  return SETXATTR
         (
           path,
           name.c_str(),
           value.c_str(),
           value.size(),
           flags
         ) != -1;
#elif defined(_WIN32)
  ::SetLastError( ERROR_NOT_SUPPORTED );
#else
  errno = ENOTSUP;
#endif
  return false;
}

auto_Stat Volume::stat( const Path& path )
{
  return getattr( path );
}

bool Volume::statvfs( const Path& path, struct statvfs& buffer )
{
#ifdef _WIN32
  ULARGE_INTEGER uFreeBytesAvailableToCaller,
                 uTotalNumberOfBytes,
                 uTotalNumberOfFreeBytes;
  if
  (
    GetDiskFreeSpaceEx
    (
      path,
      &uFreeBytesAvailableToCaller,
      &uTotalNumberOfBytes,
      &uTotalNumberOfFreeBytes
    ) != 0
  )
  {
    buffer.f_bsize = 4096;
    buffer.f_frsize = 4096;

    buffer.f_blocks
      = static_cast<fsblkcnt_t>( uTotalNumberOfBytes.QuadPart / 4096 );

    buffer.f_bfree
      = static_cast<fsblkcnt_t>( uTotalNumberOfFreeBytes.QuadPart / 4096 );

    buffer.f_bavail
      = static_cast<fsblkcnt_t>( uFreeBytesAvailableToCaller.QuadPart / 4096 );

    buffer.f_namemax = PATH_MAX;
    return true;
  }
  else
    return false;
#else
  return ::statvfs( path, &buffer ) == 0;
#endif
}

bool Volume::symlink( const Path& old_path, const Path& new_path )
{
#ifdef _WIN32
  ::SetLastError( ERROR_NOT_SUPPORTED );
  return false;
#else
  return ::symlink( old_path, new_path ) != -1;
#endif
}

bool Volume::touch( const Path& path )
{
  return touch( path, FILE_MODE_DEFAULT );
}

bool Volume::touch( const Path& path, mode_t mode )
{
  auto_File file = creat( path, mode );
  return file != NULL;
}

bool Volume::truncate( const Path& path, uint64_t new_size )
{
#ifdef _WIN32
  auto_File file = Volume::open( path, O_CREAT|O_WRONLY, FILE_MODE_DEFAULT );
  if ( file!= NULL )
  {
    file->truncate( new_size );
    return true;
  }
  else
    return false;
#else
  return ::truncate( path, new_size ) >= 0;
#endif
}

bool Volume::unlink( const Path& path )
{
#ifdef _WIN32
  return DeleteFileW( path ) != 0;
#else
  return ::unlink( path ) >= 0;
#endif
}

bool
Volume::utime
(
  const Path& path,
  const Time& atime,
  const Time& mtime
)
{
  auto_Stat stbuf = new Stat;
  stbuf->set_atime( atime );
  stbuf->set_mtime( mtime );
  return setattr( path, stbuf, SETATTR_ATIME|SETATTR_MTIME );
}

bool
Volume::utime
(
  const Path& path,
  const Time& atime,
  const Time& mtime,
  const Time& ctime
)
{
  auto_Stat stbuf = new Stat;
  stbuf->set_atime( atime );
  stbuf->set_mtime( mtime );
  stbuf->set_ctime( ctime );
  return setattr( path, stbuf, SETATTR_ATIME|SETATTR_MTIME|SETATTR_CTIME );
}

Path Volume::volname( const Path& path )
{
#ifdef _WIN32
  wchar_t file_system_name[PATH_MAX], volume_name[PATH_MAX];

  if
  (
    GetVolumeInformation
    (
      path.root_path(),
      volume_name,
      PATH_MAX,
      NULL,
      NULL,
      NULL,
      file_system_name,
      PATH_MAX
    ) != 0
    &&
    wcsnlen( volume_name, PATH_MAX ) > 0
  )
    return Path( volume_name );
  else
#endif
    return path.root_path();
}

#ifdef _WIN32
#pragma warning( pop )
#endif


// xdr_marshaller.cpp
XDRMarshaller::XDRMarshaller()
{
  buffer = new yidl::runtime::StringBuffer;
}

void XDRMarshaller::writeKey( const char* key )
{
  if ( !in_map_stack.empty() && in_map_stack.back() && key != NULL )
    Marshaller::writeString( NULL, 0, key );
}

void XDRMarshaller::writeBoolean( const char* key, uint32_t tag, bool value )
{
  writeInt32( key, tag, value ? 1 : 0 );
}

void
XDRMarshaller::writeBuffer
(
  const char* key, uint32_t tag,
  yidl::runtime::auto_Buffer value
)
{
  writeInt32( key, tag, static_cast<int32_t>( value->size() ) );

  buffer->put( static_cast<void*>( *value ), value->size() );

  if ( value->size() % 4 != 0 )
  {
    static char zeros[] = { 0, 0, 0 };
    buffer->put( static_cast<const void*>( zeros ), 4 - ( value->size() % 4 ) );
  }
}

void XDRMarshaller::writeDouble( const char* key, uint32_t, double value )
{
  writeKey( key );
  uint64_t uint64_value;
  memcpy_s( &uint64_value, sizeof( uint64_value ), &value, sizeof( value ) );
  uint64_value = Machine::htonll( uint64_value );
  buffer->put( &uint64_value, sizeof( uint64_value ) );
}

void XDRMarshaller::writeFloat( const char* key, uint32_t, float value )
{
  writeKey( key );
  uint32_t uint32_value;
  memcpy_s( &uint32_value, sizeof( uint32_value ), &value, sizeof( value ) );
#ifdef __MACH__
  uint32_value = htonl( uint32_value );
#else
  uint32_value = Machine::htonl( uint32_value );
#endif
  buffer->put( &uint32_value, sizeof( uint32_value ) );
}

void XDRMarshaller::writeInt32( const char* key, uint32_t, int32_t value )
{
  writeKey( key );
#ifdef __MACH__
  value = htonl( value );
#else
  value = Machine::htonl( value );
#endif
  buffer->put( &value, sizeof( value ) );
}

void XDRMarshaller::writeInt64( const char* key, uint32_t, int64_t value )
{
  writeKey( key );
  value = Machine::htonll( value );
  buffer->put( &value, sizeof( value ) );
}

void
XDRMarshaller::writeMap
(
  const char* key,
  uint32_t tag,
  const yidl::runtime::Map& value
)
{
  writeInt32( key, tag, static_cast<int32_t>( value.get_size() ) );
  in_map_stack.push_back( true );
  value.marshal( *this );
  in_map_stack.pop_back();
}

void
XDRMarshaller::writeSequence
(
  const char* key,
  uint32_t tag,
  const yidl::runtime::Sequence& value
)
{
  writeInt32( key, tag, static_cast<int32_t>( value.get_size() ) );
  value.marshal( *this );
}

void
XDRMarshaller::writeString
(
  const char* key,
  uint32_t tag,
  const char* value,
  size_t value_len
)
{
  writeInt32( key, tag, static_cast<int32_t>( value_len ) );
  buffer->put( static_cast<const void*>( value ), value_len );
  if ( value_len % 4 != 0 )
  {
    static char zeros[] = { 0, 0, 0 };
    buffer->put( static_cast<const void*>( zeros ), 4 - ( value_len % 4 ) );
  }
}

void
XDRMarshaller::writeStruct
(
  const char* key,
  uint32_t,
  const yidl::runtime::Struct& value
)
{
  writeKey( key );
  value.marshal( *this );
}


// xdr_unmarshaller.cpp
XDRUnmarshaller::XDRUnmarshaller( yidl::runtime::auto_Buffer buffer )
  : buffer( buffer )
{ }

void XDRUnmarshaller::read( void* buffer, size_t buffer_len )
{
//#ifdef _DEBUG
//  if ( this->buffer->size() - this->buffer->position() < buffer_len )
//    DebugBreak();
//#endif
  this->buffer->get( buffer, buffer_len );
}

bool XDRUnmarshaller::readBoolean( const char* key, uint32_t tag )
{
  return readInt32( key, tag ) == 1;
}

void
XDRUnmarshaller::readBuffer
(
  const char* key,
  uint32_t tag,
  yidl::runtime::auto_Buffer value
)
{
  size_t size = readInt32( key, tag );
  if ( value->capacity() - value->size() < size ) DebugBreak();
  read( static_cast<void*>( *value ), size );
  value->put( NULL, size );
  if ( size % 4 != 0 )
  {
    char zeros[3];
    read( zeros, 4 - ( size % 4 ) );
  }
}

double XDRUnmarshaller::readDouble( const char*, uint32_t )
{
  uint64_t uint64_value;
  read( &uint64_value, sizeof( uint64_value ) );
  uint64_value = Machine::ntohll( uint64_value );
  double double_value;
  memcpy_s
  (
    &double_value,
    sizeof( double_value ),
    &uint64_value,
    sizeof( uint64_value )
  );
  return double_value;
}

float XDRUnmarshaller::readFloat( const char*, uint32_t )
{
  uint32_t uint32_value;
  read( &uint32_value, sizeof( uint32_value ) );
#ifdef __MACH__
  uint32_value = ntohl( uint32_value );
#else
  uint32_value = Machine::ntohl( uint32_value );
#endif
  float float_value;
  memcpy_s
  (
    &float_value,
    sizeof( float_value ),
    &uint32_value,
    sizeof( uint32_value )
  );
  return float_value;
}

int32_t XDRUnmarshaller::readInt32( const char*, uint32_t )
{
  int32_t value;
  read( &value, sizeof( value ) );
#ifdef __MACH__
  return ntohl( value );
#else
  return Machine::ntohl( value );
#endif
}

int64_t XDRUnmarshaller::readInt64( const char*, uint32_t )
{
  int64_t value;
  read( &value, sizeof( value ) );
  return Machine::ntohll( value );
}

void
XDRUnmarshaller::readMap
(
  const char* key,
  uint32_t tag,
  yidl::runtime::Map& value
)
{
  size_t size = readInt32( key, tag );
  for ( size_t i = 0; i < size; i++ )
    value.unmarshal( *this );
}

void
XDRUnmarshaller::readSequence
(
  const char* key,
  uint32_t tag,
  yidl::runtime::Sequence& value
)
{
  size_t size = readInt32( key, tag );
  if ( size <= UINT16_MAX )
  {
    for ( size_t i = 0; i < size; i++ )
      value.unmarshal( *this );
  }
}

void
XDRUnmarshaller::readString
(
  const char* key,
  uint32_t tag,
  std::string& value
)
{
  size_t str_len = readInt32( key, tag );

  if ( str_len < UINT16_MAX )
  {
    if ( str_len != 0 )
    {
      size_t padded_str_len = str_len % 4;
      if ( padded_str_len == 0 )
        padded_str_len = str_len;
      else
        padded_str_len = str_len + 4 - padded_str_len;

      value.resize( padded_str_len );
      read( const_cast<char*>( value.c_str() ), padded_str_len );
      value.resize( str_len );
    }
  }
}

void
XDRUnmarshaller::readStruct
(
  const char*,
  uint32_t,
  yidl::runtime::Struct& value
)
{
  value.unmarshal( *this );
}

