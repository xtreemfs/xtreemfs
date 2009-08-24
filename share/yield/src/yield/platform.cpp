#include "yield/platform.h"
using namespace YIELD;


// counting_semaphore.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
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
bool CountingSemaphore::timed_acquire( uint64_t timeout_ns )
{
#if defined(_WIN32)
  DWORD timeout_ms = static_cast<DWORD>( timeout_ns / NS_IN_MS );
  DWORD dwRet = WaitForSingleObjectEx( hSemaphore, timeout_ms, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#elif defined(__MACH__)
  mach_timespec_t timeout_m_ts = { timeout_ns / NS_IN_S, timeout_ns % NS_IN_S };
  return semaphore_timedwait( sem, timeout_m_ts ) == KERN_SUCCESS;
#else
  struct timespec timeout_ts = ( Time() + Time( timeout_ns ) );
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


// exception.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#include <windows.h>
#include <lmerr.h>
#else
#include <errno.h>
#endif
uint32_t Exception::get_errno()
{
#ifdef _WIN32
  return static_cast<uint32_t>( ::GetLastError() );
#else
  return static_cast<uint32_t>( errno );
#endif
}
void Exception::set_errno( uint32_t error_code )
{
#ifdef _WIN32
  ::SetLastError( static_cast<DWORD>( error_code ) );
#else
  errno = static_cast<int>( error_code );
#endif
}
std::string Exception::strerror( uint32_t error_code )
{
  std::string out_str;
  strerror( error_code, out_str );
  return out_str;
}
void Exception::strerror( uint32_t error_code, std::string& out_str )
{
  char strerror_buffer[YIELD_EXCEPTION_WHAT_BUFFER_LENGTH];
  strerror( error_code, strerror_buffer, YIELD_EXCEPTION_WHAT_BUFFER_LENGTH-1 );
  out_str.assign( strerror_buffer );
}
void Exception::strerror( uint32_t error_code, char* out_str, size_t out_str_len )
{
#ifdef _WIN32
  if ( out_str_len > 0 )
  {
    DWORD dwMessageLength = FormatMessageA( FORMAT_MESSAGE_FROM_SYSTEM|FORMAT_MESSAGE_IGNORE_INSERTS, NULL, error_code, MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ), out_str, static_cast<DWORD>( out_str_len ), NULL );
    if ( dwMessageLength > 0 )
    {
      if ( dwMessageLength > 2 )
        out_str[dwMessageLength - 2] = 0; // Cut off trailing \r\n
      return;
    }
    else if ( GetLastError() == ERROR_INSUFFICIENT_BUFFER )
    {
      LPSTR cMessage;
      dwMessageLength = FormatMessageA( FORMAT_MESSAGE_ALLOCATE_BUFFER|FORMAT_MESSAGE_FROM_SYSTEM|FORMAT_MESSAGE_IGNORE_INSERTS, NULL, error_code, MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ), ( LPSTR )&cMessage, 0, NULL );
      if ( dwMessageLength > 0 )
      {
        if ( dwMessageLength > 2 )
          cMessage[dwMessageLength - 2] = 0;
        strncpy_s( out_str, out_str_len, cMessage, out_str_len - 1 );
        out_str[out_str_len - 1] = 0;
        LocalFree( cMessage );
        return;
      }
    }
    else if ( error_code >= NERR_BASE || error_code <= MAX_NERR )
    {
      HMODULE hModule = LoadLibraryEx( TEXT( "netmsg.dll" ), NULL, LOAD_LIBRARY_AS_DATAFILE ); // Let's hope this is cheap..
      if ( hModule != NULL )
      {
        dwMessageLength = FormatMessageA( FORMAT_MESSAGE_FROM_HMODULE|FORMAT_MESSAGE_IGNORE_INSERTS, hModule, error_code, MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ), out_str, static_cast<DWORD>( out_str_len ), NULL );
        if ( dwMessageLength > 0 )
        {
          if ( dwMessageLength > 2 )
            out_str[dwMessageLength - 2] = 0; // Cut off trailing \r\n
          FreeLibrary( hModule );
          return;
        }
        else if ( GetLastError() == ERROR_INSUFFICIENT_BUFFER )
        {
          LPSTR cMessage;
          dwMessageLength = FormatMessageA( FORMAT_MESSAGE_FROM_HMODULE|FORMAT_MESSAGE_ALLOCATE_BUFFER|FORMAT_MESSAGE_FROM_SYSTEM|FORMAT_MESSAGE_IGNORE_INSERTS, NULL, error_code, MAKELANGID( LANG_NEUTRAL, SUBLANG_DEFAULT ), ( LPSTR )&cMessage, 0, NULL );
          if ( dwMessageLength > 0 )
          {
            if ( dwMessageLength > 2 )
              cMessage[dwMessageLength - 2] = 0;
            strncpy_s( out_str, out_str_len, cMessage, out_str_len - 1 );
            out_str[out_str_len - 1] = 0;
            LocalFree( cMessage );
            FreeLibrary( hModule );
            return;
          }
        }
        else
          FreeLibrary( hModule );
      }
    }
    sprintf_s( out_str, out_str_len, "error_code = %u", error_code );
  }
#else
  snprintf( out_str, out_str_len, "errno = %u, strerror = %s", error_code, std::strerror( error_code ) );
#endif
}
Exception::Exception()
{
  strerror( get_errno(), what_buffer, YIELD_EXCEPTION_WHAT_BUFFER_LENGTH-1 );
}
Exception::Exception( uint32_t error_code )
{
  strerror( error_code, what_buffer, YIELD_EXCEPTION_WHAT_BUFFER_LENGTH-1 );
}
void Exception::init( const char* what )
{
#ifdef _WIN32
  strncpy_s(
#else
  strncpy(
#endif
     what_buffer, what, YIELD_EXCEPTION_WHAT_BUFFER_LENGTH-1 );
}


// file.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#else
#define _XOPEN_SOURCE 600
#define _LARGEFILE64_SOURCE 1
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
#define FLISTXATTR( fd, namebuf, size ) ::flistxattr( fd, namebuf, size, 0 )
#define FGETXATTR( fd, name, value, size ) ::fgetxattr( fd, name, value, size, 0, 0 )
#define FSETXATTR( fd, name, value, size, flags ) ::fsetxattr( fd, name, value, size, 0, flags )
#define FREMOVEXATTR( fd, name ) ::fremovexattr( fd, name, 0 )
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
#if defined(__linux__) || defined(__sun)
  return fdatasync( fd ) != -1;
#else
  return true;
#endif
}
bool File::flush()
{
#ifdef _WIN32
  return FlushFileBuffers( fd ) != 0;
#else
  return true;
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
uint64_t File::get_size()
{
  auto_Stat stbuf = getattr();
  if ( stbuf != NULL )
    return stbuf->get_size();
  else
    return 0;
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
auto_File File::open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
{
#ifdef _WIN32
  DWORD file_access_flags = 0,
        file_create_flags = 0,
        file_open_flags = attributes|FILE_FLAG_SEQUENTIAL_SCAN;
  if ( ( flags & O_RDWR ) == O_RDWR )
    file_access_flags |= GENERIC_READ|GENERIC_WRITE;
  else if ( ( flags & O_WRONLY ) == O_WRONLY )
  {
    file_access_flags |= GENERIC_WRITE;
    if ( ( flags & O_APPEND ) == O_APPEND )
      file_access_flags |= FILE_APPEND_DATA;
  }
  else if ( ( flags & O_APPEND ) == O_APPEND )
      file_access_flags |= GENERIC_WRITE|FILE_APPEND_DATA;
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
  HANDLE fd = CreateFileW( path, file_access_flags, FILE_SHARE_READ|FILE_SHARE_WRITE, NULL, file_create_flags, file_open_flags, NULL );
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
ssize_t File::read( yidl::auto_Buffer buffer )
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
  if ( ReadFile( fd, buffer, static_cast<DWORD>( buffer_len ), &dwBytesRead, NULL ) )
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
  if ( SetFilePointer( fd, uliOffset.LowPart, ( PLONG )&uliOffset.HighPart, whence ) != INVALID_SET_FILE_POINTER )
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
    return LockFile( fd, uliOffset.LowPart, uliOffset.HighPart, uliLength.LowPart, uliLength.HighPart ) == TRUE;
  }
  else
    return false;
#else
  struct flock flock_;
  flock_.l_type   = exclusive ? F_WRLCK : F_RDLCK;
  flock_.l_whence = SEEK_SET;
  flock_.l_start  = offset;
  flock_.l_len    = length;
  flock_.l_pid    = getpid();
  return fcntl( fd, F_SETLKW, &flock_ ) != -1;
#endif
}
bool File::setxattr( const std::string& name, const std::string& value, int flags )
{
#ifdef YIELD_HAVE_XATTR_H
  return FSETXATTR( fd, name.c_str(), value.c_str(), value.size(), flags ) != -1;
#else
  return false;
#endif
}
bool File::sync()
{
#ifdef _WIN32
  return true;
#else
  return fsync( fd ) != -1;
#endif
}
bool File::truncate( uint64_t new_size )
{
#ifdef _WIN32
  ULARGE_INTEGER uliNewSize;
  uliNewSize.QuadPart = new_size;
  if ( SetFilePointer( fd, uliNewSize.LowPart, ( PLONG )&uliNewSize.HighPart, SEEK_SET ) != INVALID_SET_FILE_POINTER )
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
  return UnlockFile( fd, uliOffset.LowPart, uliOffset.HighPart, uliLength.LowPart, uliLength.HighPart ) == TRUE;
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
ssize_t File::write( yidl::auto_Buffer buffer )
{
  return write( static_cast<void*>( *buffer ), buffer->size() );
}
ssize_t File::write( const void* buffer, size_t buffer_len )
{
#ifdef _WIN32
  DWORD dwBytesWritten;
  if ( WriteFile( fd, buffer, buffer_len, &dwBytesWritten, NULL ) )
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


// log.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
namespace YIELD
{
  class FileLog : public Log
  {
  public:
    FileLog( auto_File file, Level level )
      : Log( level ), file( file )
    { }
    // Log
    void write( const char* str, size_t str_len )
    {
      file->write( str, str_len );
    }
  private:
    auto_File file;
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
auto_Log Log::open( const Path& file_path, Level level )
{
  auto_File file = File::open( file_path, O_CREAT|O_WRONLY|O_APPEND );
  if ( file != NULL )
    return new FileLog( file, level );
  else
    return NULL;
}
void Log::write( const unsigned char* str, size_t str_len, Level level )
{
  if ( level <= this->level )
  {
    bool str_is_printable = true;
    for ( size_t str_i = 0; str_i < str_len; str_i++ )
    {
      if ( str[str_i] == '\r' || str[str_i] == '\n' || ( str[str_i] >= 32 && str[str_i] <= 126 ) )
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
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
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
uint16_t Machine::getOnlineLogicalProcessorCount()
{
  uint16_t online_logical_processor_count = 0;
#if defined(_WIN32)
  SYSTEM_INFO available_info;
  GetSystemInfo( &available_info );
  online_logical_processor_count = static_cast<uint16_t>( available_info.dwNumberOfProcessors );
#elif defined(__linux__)
  long _online_logical_processor_count = sysconf( _SC_NPROCESSORS_ONLN );
  if ( _online_logical_processor_count != -1 ) online_logical_processor_count = _online_logical_processor_count;
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
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#define NOMINMAX
#include <windows.h>
#else
#include <sys/mman.h>
#endif
auto_MemoryMappedFile MemoryMappedFile::open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes, size_t minimum_size )
{
  auto_File file = File::open( path, flags, mode, attributes );
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
    yidl::auto_Object<MemoryMappedFile> memory_mapped_file = new MemoryMappedFile( file, flags );
    if ( memory_mapped_file->resize( std::max( minimum_size, current_file_size ) ) )
      return memory_mapped_file;
    else
      return NULL;
  }
  else
    return NULL;
}
MemoryMappedFile::MemoryMappedFile( auto_File underlying_file, uint32_t open_flags )
  : underlying_file( underlying_file ), open_flags( open_flags )
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
    if ( size <= new_size || underlying_file->truncate( new_size ) )
    {
#ifdef _WIN32
      unsigned long map_flags = PAGE_READONLY;
      if ( ( open_flags & O_RDWR ) == O_RDWR || ( open_flags & O_WRONLY ) == O_WRONLY )
        map_flags = PAGE_READWRITE;
      ULARGE_INTEGER uliNewSize; uliNewSize.QuadPart = new_size;
      mapping = CreateFileMapping( *underlying_file, NULL, map_flags, uliNewSize.HighPart, uliNewSize.LowPart, NULL );
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
      if( ( open_flags & O_RDWR ) == O_RDWR || ( open_flags & O_WRONLY ) == O_WRONLY )
        mmap_flags |= PROT_WRITE;
      void* mmap_ret = mmap( 0, new_size, mmap_flags, MAP_SHARED, *underlying_file, 0 );
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
  return FlushViewOfFile( start, 0 ) == TRUE;
#else
  return msync( start, size, MS_SYNC ) == 0;
#endif
}
bool MemoryMappedFile::sync( size_t offset, size_t length )
{
#ifdef _WIN32
  return FlushViewOfFile( start + offset, length ) == TRUE;
#else
  return msync( start + offset, length, MS_SYNC ) == 0;
#endif
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
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if defined(_WIN32)
#include <windows.h>
#elif defined(__linux__) || defined(__FreeBSD__) || defined(__sun)
#define YIELD_HAVE_PTHREAD_MUTEX_TIMEDLOCK
#endif
Mutex::Mutex()
{
#ifdef _WIN32
  if ( ( hMutex = CreateEvent( NULL, FALSE, TRUE, NULL ) ) == NULL ) DebugBreak();
#else
  if ( pthread_mutex_init( &pthread_mutex, NULL ) != 0 ) DebugBreak();
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
bool Mutex::timed_acquire( uint64_t timeout_ns )
{
#ifdef _WIN32
  DWORD timeout_ms = static_cast<DWORD>( timeout_ns / NS_IN_MS );
  DWORD dwRet = WaitForSingleObjectEx( hMutex, timeout_ms, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#else
#ifdef YIELD_HAVE_PTHREAD_MUTEX_TIMEDLOCK
  struct timespec timeout_ts = ( Time() + Time( timeout_ns ) );
  return ( pthread_mutex_timedlock( &pthread_mutex, &timeout_ts ) == 0 );
#else
  if ( pthread_mutex_trylock( &pthread_mutex ) == 0 )
    return true;
  else
  {
    usleep( timeout_ns / 1000 );
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
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#include <windows.h>
#else
//#include <iconv.h>
//#if defined(__FreeBSD__) || defined(__OpenBSD__) || defined(__sun)
//#define ICONV_SOURCE_CAST const char**
//#else
//#define ICONV_SOURCE_CAST char**
//#endif
#endif
// Paths from UTF-8
//#ifdef _WIN32
//    wide_path.assign( _wide_path, MultiByteToWideChar( CP_UTF8, 0, utf8_path.c_str(), ( int )utf8_path.size(), _wide_path, PATH_MAX ) );
//#else
//    MultiByteToMultiByte( "UTF-8", utf8_path, "", host_charset_path );
//#endif
Path::Path( const char* host_charset_path )
  : host_charset_path( host_charset_path )
{
  init_from_host_charset_path();
}
Path::Path( const char* host_charset_path, size_t host_charset_path_len )
  : host_charset_path( host_charset_path, host_charset_path_len )
{
  init_from_host_charset_path();
}
Path::Path( const std::string& host_charset_path )
  : host_charset_path( host_charset_path )
{
  init_from_host_charset_path();
}
void Path::init_from_host_charset_path()
{
#ifdef _WIN32
  wchar_t _wide_path[PATH_MAX];
  wide_path.assign( _wide_path, MultiByteToWideChar( GetACP(), 0, host_charset_path.c_str(), static_cast<int>( host_charset_path.size() ), _wide_path, PATH_MAX ) );
#endif
}
#ifdef _WIN32
Path::Path( const wchar_t* wide_path )
  : wide_path( wide_path )
{
  init_from_wide_path();
}
Path::Path( const wchar_t* wide_path, size_t wide_path_len )
  : wide_path( wide_path, wide_path_len )
{
  init_from_wide_path();
}
Path::Path( const std::wstring& wide_path )
  : wide_path( wide_path )
{
  init_from_wide_path();
}
void Path::init_from_wide_path()
{
  char _host_charset_path[PATH_MAX];
  int _host_charset_path_len = WideCharToMultiByte( GetACP(), 0, this->wide_path.c_str(), ( int )this->wide_path.size(), _host_charset_path, PATH_MAX, 0, 0 );
  host_charset_path.assign( _host_charset_path, _host_charset_path_len );
}
#endif
Path::Path( const Path &other )
: host_charset_path( other.host_charset_path )
#ifdef _WIN32
, wide_path( other.wide_path )
#endif
{ }
/*
const std::string& Path::get_utf8_path()
{
  if ( utf8_path.empty() )
  {
#ifdef _WIN32
    if ( !wide_path.empty() )
    {
      char _utf8_path[PATH_MAX];
      int _utf8_path_len = WideCharToMultiByte( CP_UTF8, 0, wide_path.c_str(), ( int )wide_path.size(), _utf8_path, PATH_MAX, 0, 0 );
      utf8_path.assign( _utf8_path, _utf8_path_len );
    }
#else
    if ( !host_charset_path.empty() )
     MultiByteToMultiByte( "", host_charset_path, "UTF-8", utf8_path ); // "" = local host charset
#endif
  }
  return utf8_path;
}
#ifndef _WIN32
void Path::MultiByteToMultiByte( const char* fromcode, const std::string& frompath, const char* tocode, std::string& topath )
{
  iconv_t converter;
  if ( ( converter = iconv_open( fromcode, tocode ) ) != ( iconv_t )-1 )
  {
    char* _frompath = const_cast<char*>( frompath.c_str() ); char _topath[PATH_MAX], *_topath_p = _topath;
    size_t _frompath_size = frompath.size(), _topath_size = PATH_MAX;
	//::iconv( converter, NULL, 0, NULL, 0 ) != -1 &&
    size_t iconv_ret;
    if ( ( iconv_ret = ::iconv( converter, ( ICONV_SOURCE_CAST )&_frompath, &_frompath_size, &_topath_p, &_topath_size ) ) != static_cast<size_t>( -1 ) )
      topath.assign( _topath, PATH_MAX - _topath_size );
    else
    {
//			cerr << "Path: iconv could not convert path " << frompath << " from code " << fromcode << " to code " << tocode;
      topath = frompath;
    }
    iconv_close( converter );
  }
  else
    DebugBreak();
}
#endif
*/
#ifdef _WIN32
bool Path::operator==( const wchar_t* other ) const
{
  return wide_path == other;
}
bool Path::operator!=( const wchar_t* other ) const
{
  return wide_path != other;
}
#endif
bool Path::operator==( const Path& other ) const
{
#ifdef _WIN32
  return wide_path == other.wide_path;
#else
  return host_charset_path == other.host_charset_path;
#endif
}
bool Path::operator!=( const Path& other ) const
{
#ifdef _WIN32
  return wide_path != other.wide_path;
#else
  return host_charset_path != other.host_charset_path;
#endif
}
bool Path::operator==( const char* other ) const
{
  return host_charset_path == other;
}
bool Path::operator!=( const char* other ) const
{
  return host_charset_path != other;
}
Path Path::join( const Path& other ) const
{
#ifdef _WIN32
  if ( wide_path.empty() )
    return other;
  else if ( other.wide_path.empty() )
    return *this;
  else
  {
    std::wstring combined_wide_path( wide_path );
    if ( combined_wide_path[combined_wide_path.size()-1] != PATH_SEPARATOR &&
       other.wide_path[0] != PATH_SEPARATOR )
      combined_wide_path.append( PATH_SEPARATOR_WIDE_STRING, 1 );
    combined_wide_path.append( other.wide_path );
    return Path( combined_wide_path );
  }
#else
/*
  if ( !utf8_path.empty() && !other.utf8_path.empty() )
  {
    std::string combined_utf8_path( utf8_path );
    if ( combined_utf8_path[combined_utf8_path.size()-1] != PATH_SEPARATOR &&
       other.utf8_path[0] != PATH_SEPARATOR )
      combined_utf8_path.append( PATH_SEPARATOR_STRING, 1 );
    combined_utf8_path.append( other.utf8_path );
    return Path( combined_utf8_path );
  }
  else
  {
*/
    std::string combined_host_charset_path( host_charset_path );
    if ( combined_host_charset_path[combined_host_charset_path.size()-1] != PATH_SEPARATOR &&
       other.host_charset_path[0] != PATH_SEPARATOR )
      combined_host_charset_path.append( PATH_SEPARATOR_STRING, 1 );
    combined_host_charset_path.append( other.host_charset_path );
    return Path( combined_host_charset_path );
//  }
#endif
}
std::pair<Path, Path> Path::split() const
{
  std::string::size_type last_sep = host_charset_path.find_last_of( PATH_SEPARATOR );
  if ( last_sep == host_charset_path.length() )
    return std::make_pair( *this, Path() );
  else if ( last_sep != std::string::npos )
    return std::make_pair( host_charset_path.substr( 0, last_sep ), host_charset_path.substr( last_sep + 1 ) );
  else
    return std::make_pair( Path(), *this );
}
void Path::split_all( std::vector<Path>& parts ) const
{
  std::string::size_type last_sep = host_charset_path.find_first_not_of( PATH_SEPARATOR, 0 );
  std::string::size_type next_sep = host_charset_path.find_first_of( PATH_SEPARATOR, last_sep );
  while ( next_sep != std::string::npos || last_sep != std::string::npos )
  {
    parts.push_back( host_charset_path.substr( last_sep, next_sep - last_sep ) );
    last_sep = host_charset_path.find_first_not_of( PATH_SEPARATOR, next_sep );
    next_sep = host_charset_path.find_first_of( PATH_SEPARATOR, last_sep );
  }
}
std::pair<Path, Path> Path::splitext() const
{
  std::string::size_type last_dot = host_charset_path.find_last_of( "." );
  if ( last_dot == 0 || last_dot == std::string::npos )
    return std::make_pair( *this, Path() );
  else
    return std::make_pair( host_charset_path.substr( 0, last_dot ), host_charset_path.substr( last_dot ) );
}
Path Path::abspath() const
{
#ifdef _WIN32
  wchar_t abspath_buffer[PATH_MAX];
  DWORD abspath_buffer_len = GetFullPathNameW( wide_path.c_str(), PATH_MAX, abspath_buffer, NULL );
  return Path( abspath_buffer, abspath_buffer_len );
#else
  char abspath_buffer[PATH_MAX];
  realpath( host_charset_path.c_str(), abspath_buffer );
  return Path( abspath_buffer );
#endif
}


// performance_counter_set.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef YIELD_HAVE_PERFORMANCE_COUNTERS
#if defined(__sun)
#include <libcpc.h>
#elif defined(YIELD_HAVE_PAPI)
#include <papi.h>
#include <pthread.h>
#endif
auto_Object<PerformanceCounterSet> PerformanceCounterSet::create()
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
  int event_index = cpc_set_add_request( cpc, cpc_set, name, 0, CPC_COUNT_USER, 0, NULL );
  if ( event_index != -1 )
  {
    event_indices.push_back( event_index );
    return true;
  }
#elif defined(YIELD_HAVE_PAPI)
  int papi_event_code;
  if ( PAPI_event_name_to_code( const_cast<char*>( name ), &papi_event_code ) == PAPI_OK )
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
  for ( std::vector<int>::size_type event_index_i = 0; event_index_i < event_indices.size(); event_index_i++ )
    cpc_buf_get( cpc, diff_cpc_buf, event_indices[event_index_i], &counts[event_index_i] );
  cpc_unbind( cpc, cpc_set );
#elif defined(YIELD_HAVE_PAPI)
  PAPI_stop( papi_eventset, reinterpret_cast<long long int*>( counts ) );
#endif
}
#endif


// processor_set.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
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
  psetid = PS_NONE; // Don't pset_create until we actually use the set, to avoid leaving state in the system
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
  for ( uint16_t processor_i = 0; processor_i < static_cast<uint16_t>( -1 ); processor_i++ )
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
    return pset_assign( PS_QUERY, processor_i, &check_psetid ) == 0 &&
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


// rrd.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
RRD::Record::Record( double value )
  : value( value )
{ }
RRD::Record::Record( const Time& time, double value )
  : time( time ), value( value )
{ }
void RRD::Record::marshal( yidl::Marshaller& marshaller )
{
  marshaller.writeUint64( "time", 0, time );
  marshaller.writeDouble( "value", 0, value );
}
void RRD::Record::unmarshal( yidl::Unmarshaller& unmarshaller )
{
  time = unmarshaller.readUint64( "time", 0 );
  value = unmarshaller.readDouble( "time", 0 );
}
RRD::RecordSet::~RecordSet()
{
  for ( iterator record_i = begin(); record_i != end(); record_i++ )
    Object::decRef( *record_i );
}
RRD::RRD( const Path& file_path )
  : current_file_path( file_path )
{ }
RRD::~RRD()
{ }
void RRD::append( double value )
{
  XDRMarshaller xdr_marshaller;
  Record( value ).marshal( xdr_marshaller );
  auto_File current_file( File::open( current_file_path, O_CREAT|O_WRONLY|O_APPEND ) );
  if ( current_file != NULL )
    current_file->write( xdr_marshaller.get_buffer().release() );
}
auto_RRD RRD::creat( const Path& file_path )
{
  if ( !Volume().exists( file_path ) ||
       Volume().unlink( file_path ) )
    return new RRD( file_path );
  else
    return NULL;
}
void RRD::fetch_all( RecordSet& out_records )
{
  auto_File current_file( File::open( current_file_path ) );
  if ( current_file != NULL )
  {
    for ( ;; )
    {
      yidl::StackBuffer<16> xdr_buffer;
      if ( current_file->read( xdr_buffer.incRef() ) == 16 )
      {
        XDRUnmarshaller xdr_unmarshaller( xdr_buffer.incRef() );
        Record* record = new Record( static_cast<uint64_t>( 0 ), 0 );
        record->unmarshal( xdr_unmarshaller );
        out_records.push_back( record );
      }
      else
        break;
    }
  }
}
void RRD::fetch_from( const Time& start_time, RecordSet& out_records )
{
  RecordSet all_records;
  fetch_all( all_records );
  for ( RecordSet::iterator record_i = all_records.begin(); record_i != all_records.end(); )
  {
    if ( ( *record_i )->get_time() >= start_time )
    {
      out_records.push_back( *record_i );
      record_i = all_records.erase( record_i );
    }
    else
      ++record_i;
  }
}
void RRD::fetch_range( const Time& start_time, const Time& end_time, RecordSet& out_records )
{
  RecordSet all_records;
  fetch_all( all_records );
  for ( RecordSet::iterator record_i = all_records.begin(); record_i != all_records.end(); )
  {
    if ( ( *record_i )->get_time() >= start_time && ( *record_i )->get_time() <= end_time )
    {
      out_records.push_back( *record_i );
      record_i = all_records.erase( record_i );
    }
    else
      ++record_i;
  }
}
void RRD::fetch_until( const Time& end_time, RecordSet& out_records )
{
  RecordSet all_records;
  fetch_all( all_records );
  for ( RecordSet::iterator record_i = all_records.begin(); record_i != all_records.end(); )
  {
    if ( ( *record_i )->get_time() <= end_time )
    {
      out_records.push_back( *record_i );
      record_i = all_records.erase( record_i );
    }
    else
      ++record_i;
  }
}
auto_RRD RRD::open( const Path& file_path )
{
  if ( Volume().isfile( file_path ) )
    return new RRD( file_path );
  else
    return NULL;
}


// shared_library.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#include <windows.h>
#define snprintf _snprintf_s
#else
#include <dlfcn.h>
#include <cctype>
#endif
#ifdef _WIN32
#define DLOPEN( file_path ) LoadLibraryExA( file_path, 0, LOAD_WITH_ALTERED_SEARCH_PATH )
#else
#define DLOPEN( file_path ) dlopen( file_path, RTLD_NOW|RTLD_GLOBAL )
#endif
auto_SharedLibrary SharedLibrary::open( const Path& file_prefix, const char* argv0 )
{
  char file_path[PATH_MAX];
  void* handle;
  if ( ( handle = DLOPEN( file_prefix ) ) != NULL )
    return new SharedLibrary( handle );
  else
  {
    snprintf( file_path, PATH_MAX, "lib%c%s.%s", PATH_SEPARATOR, static_cast<const char*>( file_prefix ), SHLIBSUFFIX );
    if ( ( handle = DLOPEN( file_path ) ) != NULL )
      return new SharedLibrary( handle );
    else
    {
      snprintf( file_path, PATH_MAX, "%s.%s", static_cast<const char*>( file_prefix ), SHLIBSUFFIX );
      if ( ( handle = DLOPEN( file_path ) ) != NULL )
        return new SharedLibrary( handle );
      else
      {
        if ( argv0 != NULL )
        {
          const char* last_slash = strrchr( argv0, PATH_SEPARATOR );
          while ( last_slash != NULL && last_slash != argv0 )
          {
            snprintf( file_path, PATH_MAX, "%.*s%s.%s", static_cast<int>( last_slash - argv0 + 1 ), argv0, static_cast<const char*>( file_prefix ), SHLIBSUFFIX );
            if ( ( handle = DLOPEN( file_path ) ) != NULL )
              return new SharedLibrary( handle );
            else
            {
              snprintf( file_path, PATH_MAX, "%.*slib%c%s.%s", static_cast<int>( last_slash - argv0 + 1 ), argv0, PATH_SEPARATOR, static_cast<const char*>( file_prefix ), SHLIBSUFFIX );
              if ( ( handle = DLOPEN( file_path ) ) != NULL )
                return new SharedLibrary( handle );
            }
            last_slash--;
            while ( *last_slash != PATH_SEPARATOR ) last_slash--;
          }
        }
      }
    }
  }
  return NULL;
}
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
    dlclose( handle ); // Don't dlclose when debugging, because that causes valgrind to lose symbols
#endif
#endif
  }
}
void* SharedLibrary::getFunction( const char* function_name, void* missing_function_return_value )
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


// stat.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
auto_Stat Stat::stat( const Path& path )
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
#ifdef _WIN32
Stat::Stat( mode_t mode, uint64_t size, const Time& atime, const Time& mtime, const Time& ctime, uint32_t attributes )
: mode( mode ), size( size ), atime( atime ), mtime( mtime ), ctime( ctime ), attributes( attributes )
{ }
Stat::Stat( const BY_HANDLE_FILE_INFORMATION& by_handle_file_information )
{
  mode = ( by_handle_file_information.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY ) ? S_IFDIR : S_IFREG;
  ULARGE_INTEGER size;
  size.LowPart = by_handle_file_information.nFileSizeLow;
  size.HighPart = by_handle_file_information.nFileSizeHigh;
  this->size = static_cast<size_t>( size.QuadPart );
  ctime = by_handle_file_information.ftCreationTime;
  atime = by_handle_file_information.ftLastAccessTime;
  mtime = by_handle_file_information.ftLastWriteTime;
  attributes = by_handle_file_information.dwFileAttributes;
}
Stat::Stat( const WIN32_FIND_DATA& find_data )
{
  init( find_data.nFileSizeHigh, find_data.nFileSizeLow, &find_data.ftLastWriteTime, &find_data.ftCreationTime, &find_data.ftLastAccessTime, find_data.dwFileAttributes );
}
Stat::Stat( uint32_t nFileSizeHigh, uint32_t nFileSizeLow, const FILETIME* ftLastWriteTime, const FILETIME* ftCreationTime, const FILETIME* ftLastAccessTime, uint32_t dwFileAttributes )
{
  init( nFileSizeHigh, nFileSizeLow, ftLastWriteTime, ftCreationTime, ftLastAccessTime, dwFileAttributes );
}
#else
Stat::Stat( dev_t dev, ino_t ino, mode_t mode, nlink_t nlink, uid_t uid, gid_t gid, uint64_t size, const Time& atime, const Time& mtime, const Time& ctime )
: dev( dev ), ino( ino ), mode( mode ), nlink( nlink ), uid( uid ), gid( gid ), size( size ), atime( atime ), mtime( mtime ), ctime( ctime )
{ }
#endif
Stat::Stat( const struct stat& stbuf )
{
#ifndef _WIN32
  dev = stbuf.st_dev;
  ino = stbuf.st_ino;
#endif
  mode = stbuf.st_mode;
  size = stbuf.st_size;
  ctime = static_cast<uint32_t>( stbuf.st_ctime );
  atime = static_cast<uint32_t>( stbuf.st_atime );
  mtime = static_cast<uint32_t>( stbuf.st_mtime );
#ifndef _WIN32
  nlink = stbuf.st_nlink;
#endif
}
#ifdef _WIN32
void Stat::init( uint32_t nFileSizeHigh, uint32_t nFileSizeLow, const FILETIME* ftLastWriteTime, const FILETIME* ftCreationTime, const FILETIME* ftLastAccessTime, uint32_t dwFileAttributes )
{
  mode = ( ( dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY ) == FILE_ATTRIBUTE_DIRECTORY ) ? S_IFDIR : S_IFREG;
  ULARGE_INTEGER size;
  size.LowPart = nFileSizeLow;
  size.HighPart = nFileSizeHigh;
  this->size = static_cast<size_t>( size.QuadPart );
  if ( ftLastWriteTime )
    mtime = *ftLastWriteTime;
  if ( ftCreationTime )
    ctime = *ftCreationTime;
  if ( ftLastAccessTime )
    atime = *ftLastAccessTime;
  attributes = dwFileAttributes;
}
uint32_t Stat::get_attributes() const
{
#ifdef _WIN32
  DWORD dwFileAttributes = attributes;
  if ( ( mode & S_IFREG ) == S_IFREG )
    dwFileAttributes |= FILE_ATTRIBUTE_NORMAL;
  else if ( ( mode & S_IFDIR ) == S_IFDIR )
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
  stbuf.st_dev = dev;
  stbuf.st_ino = ino;
#endif
#ifdef _WIN32
  stbuf.st_mode = static_cast<unsigned short>( mode );
#else
  stbuf.st_mode = mode;
#endif
  stbuf.st_size = static_cast<off_t>( size );
#ifndef _WIN32
  stbuf.st_nlink = nlink;
  stbuf.st_uid = uid;
  stbuf.st_gid = gid;
#endif
  stbuf.st_atime = atime.as_unix_time_s();
  stbuf.st_mtime = mtime.as_unix_time_s();
  stbuf.st_ctime = ctime.as_unix_time_s();
  return stbuf;
}
#ifdef _WIN32
Stat::operator BY_HANDLE_FILE_INFORMATION() const
{
  BY_HANDLE_FILE_INFORMATION HandleFileInformation;
  memset( &HandleFileInformation, 0, sizeof( HandleFileInformation ) );
  HandleFileInformation.ftCreationTime = get_ctime();
  HandleFileInformation.ftLastWriteTime = get_mtime();
  HandleFileInformation.ftLastAccessTime = get_atime();
  ULARGE_INTEGER size; size.QuadPart = get_size();
  HandleFileInformation.nFileSizeLow = size.LowPart;
  HandleFileInformation.nFileSizeHigh = size.HighPart;
  HandleFileInformation.dwFileAttributes = get_attributes();
  return HandleFileInformation;
}
Stat::operator WIN32_FIND_DATA() const
{
  WIN32_FIND_DATA find_data;
  memset( &find_data, 0, sizeof( find_data ) );
  find_data.ftCreationTime = get_ctime();
  find_data.ftLastWriteTime = get_mtime();
  find_data.ftLastAccessTime = get_atime();
  ULARGE_INTEGER size; size.QuadPart = get_size();
  find_data.nFileSizeLow = size.LowPart;
  find_data.nFileSizeHigh = size.HighPart;
  find_data.dwFileAttributes = get_attributes();
  return find_data;
}
#endif
#ifdef _WIN32
#pragma warning( pop )
#endif


// thread.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
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
unsigned long Thread::createTLSKey()
{
#ifdef _WIN32
  return TlsAlloc();
#else
  unsigned long key;
  pthread_key_create( reinterpret_cast<pthread_key_t*>( &key ), NULL );
  return key;
#endif
}
unsigned long Thread::getCurrentThreadId()
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
void* Thread::getTLS( unsigned long key )
{
#ifdef _WIN32
    return TlsGetValue( key );
#else
    return pthread_getspecific( key );
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
void Thread::setThreadName( unsigned long id, const char* thread_name )
{
#ifdef _WIN32
  THREADNAME_INFO info;
  info.dwType = 0x1000;
  info.szName = thread_name;
  info.dwThreadID = id;
  info.dwFlags = 0;
  __try
  {
      RaiseException( 0x406D1388, 0, sizeof( info ) / sizeof( DWORD ), reinterpret_cast<DWORD*>( &info ) );
  }
  __except( EXCEPTION_CONTINUE_EXECUTION )
  {}
#endif
}
void Thread::setTLS( unsigned long key, void* value )
{
#ifdef _WIN32
    TlsSetValue( key, value );
#else
  pthread_setspecific( key, value );
#endif
}
void Thread::sleep( uint64_t timeout_ns )
{
#ifdef _WIN32
  Sleep( static_cast<DWORD>( timeout_ns / NS_IN_MS ) );
#else
  struct timespec timeout_ts = Time( timeout_ns );
  nanosleep( &timeout_ts, NULL );
#endif
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
bool Thread::set_processor_affinity( const ProcessorSet& logical_processor_set )
{
  if ( id != 0 )
  {
#if defined(_WIN32)
    return SetThreadAffinityMask( handle, logical_processor_set.mask ) != 0;
#elif defined(__linux__)
    return sched_setaffinity( 0, sizeof( cpu_set_t ), static_cast<cpu_set_t*>( logical_processor_set.cpu_set ) ) == 0;
#elif defined(__sun)
    return pset_bind( logical_processor_set.psetid, P_LWPID, id, NULL ) == 0;
#else
    return false;
#endif
  }
  else
    return false;
}
void Thread::start()
{
#ifdef _WIN32
  handle = CreateThread( NULL, 0, thread_stub, this, NULL, &id );
#else
  pthread_attr_t attr;
  pthread_attr_init( &attr );
  pthread_attr_setdetachstate( &attr, PTHREAD_CREATE_DETACHED );
  pthread_create( &handle, &attr, &thread_stub, ( void* )this );
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


// time.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if defined(_WIN32)
#include <windows.h> // For FILETIME
#include <winsock.h> // For timeval
#elif defined(__MACH__)
#include <sys/time.h> // For gettimeofday
#endif
const char* HTTPDaysOfWeek[] = {  "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
const char* ISOMonths[] = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
#ifdef _WIN32
static inline ULONGLONG FILETIMEToUnixTimeNS( const FILETIME& file_time )
{
  ULARGE_INTEGER file_time_combined;
  file_time_combined.LowPart = file_time.dwLowDateTime;
  file_time_combined.HighPart = file_time.dwHighDateTime;
  file_time_combined.QuadPart -= 116444736000000000; // The number of 100-ns intervals between January 1, 1601 and January 1, 1970
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
  unix_time_ns += 11644473600000000000; // The difference in ns between January 1, 1601 (start of the Windows epoch) and January 1, 1970 (start of the Unix epoch)
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
  SystemTimeToTzSpecificLocalTime( &time_zone_information, &utc_system_time, &local_system_time );
  return local_system_time;
}
#endif
uint64_t Time::getCurrentUnixTimeNS()
{
#if defined(_WIN32)
  FILETIME file_time;
  GetSystemTimeAsFileTime( &file_time );
  return FILETIMEToUnixTimeNS( file_time );
#elif defined(__MACH__)
  struct timeval tv;
  gettimeofday( &tv, NULL );
  return tv.tv_sec * NS_IN_S + tv.tv_usec * NS_IN_US;
#else
  // POSIX real time
  struct timespec ts;
  clock_gettime( CLOCK_REALTIME, &ts );
  return ts.tv_sec * NS_IN_S + ts.tv_nsec;
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
            ISOMonths[ local_system_time.wMonth-1 ],
            local_system_time.wYear,
            local_system_time.wHour,
            local_system_time.wMinute,
            local_system_time.wSecond,
            ( win_tz.Bias / 60 ) * -100 );
#else
  time_t unix_time_s = ( time_t )( unix_time_ns / NS_IN_S );
  struct tm unix_tm;
  localtime_r( &unix_time_s, &unix_tm );
  snprintf( out_str, out_str_len,
          "%02d/%s/%04d:%02d:%02d:%02d %d",
            unix_tm.tm_mday,
            ISOMonths [ unix_tm.tm_mon ],
            unix_tm.tm_year + 1900,
            unix_tm.tm_hour,
            unix_tm.tm_min,
            unix_tm.tm_sec,
            0 ); // Could use the extern timezone, which is supposed to be secs west of GMT..
#endif
}
void Time::as_http_date_time( char* out_str, uint8_t out_str_len ) const
{
#ifdef _WIN32
  SYSTEMTIME utc_system_time = UnixTimeNSToUTCSYSTEMTIME( unix_time_ns );
  _snprintf_s( out_str, out_str_len, _TRUNCATE,
          "%s, %02d %s %04d %02d:%02d:%02d GMT",
            HTTPDaysOfWeek [ utc_system_time.wDayOfWeek ],
            utc_system_time.wDay,
            ISOMonths [ utc_system_time.wMonth-1 ],
            utc_system_time.wYear,
            utc_system_time.wHour,
            utc_system_time.wMinute,
            utc_system_time.wSecond );
#else
  time_t unix_time_s = ( time_t )( unix_time_ns / NS_IN_S );
  struct tm unix_tm;
  gmtime_r( &unix_time_s, &unix_tm );
  snprintf( out_str, out_str_len,
          "%s, %02d %s %04d %02d:%02d:%02d GMT",
            HTTPDaysOfWeek [ unix_tm.tm_wday ],
            unix_tm.tm_mday,
            ISOMonths [ unix_tm.tm_mon ],
            unix_tm.tm_year + 1900,
            unix_tm.tm_hour,
            unix_tm.tm_min,
            unix_tm.tm_sec );
#endif
}
/*
uint64_t Time::parseHTTPDateTimeToUnixTimeNS( const char* date_str )
{
  char day[4], month[4];
#ifdef _WIN32
  SYSTEMTIME utc_system_time;
  int sf_ret = sscanf( date_str, "%03s, %02d %03s %04d %02d:%02d:%02d GMT",
                       &day,
                       &utc_system_time.wDay,
                       &month,
                       &utc_system_time.wYear,
                       &utc_system_time.wHour,
                       &utc_system_time.wMinute,
                       &utc_system_time.wSecond );
  if ( sf_ret != 7 )
    return 0;
  for ( utc_system_time.wDayOfWeek = 0; utc_system_time.wDayOfWeek < 7; utc_system_time.wDayOfWeek++ )
    if ( strcmp( day, HTTPDaysOfWeek[utc_system_time.wDayOfWeek] ) == 0 ) break;
  for ( utc_system_time.wMonth = 0; utc_system_time.wMonth < 12; utc_system_time.wMonth++ )
    if ( strcmp( month, ISOMonths[utc_system_time.wMonth] ) == 0 ) break;
  utc_system_time.wMonth++; // Windows starts the months from 1
  FILETIME file_time;
  SystemTimeToFileTime( &utc_system_time, &file_time );
  return FILETIMEToUnixTimeNS( file_time );
#else
  struct tm unix_tm;
  int sf_ret = sscanf( date_str, "%03s, %02d %03s %04d %02d:%02d:%02d GMT",
                       &day,
                       &unix_tm.tm_mday,
                       &month,
                       &unix_tm.tm_year,
                       &unix_tm.tm_hour,
                       &unix_tm.tm_min,
                       &unix_tm.tm_sec );
  if ( sf_ret != 7 )
    return 0;
  unix_tm.tm_year -= 1900;
  for ( unix_tm.tm_wday = 0; unix_tm.tm_wday < 7; unix_tm.tm_wday++ )
    if ( strcmp( day, HTTPDaysOfWeek[unix_tm.tm_wday] ) == 0 ) break;
  for ( unix_tm.tm_mon = 0; unix_tm.tm_mon < 12; unix_tm.tm_mon++ )
    if ( strcmp( month, ISOMonths[unix_tm.tm_mon] ) == 0 ) break;
  time_t unix_time_s = mktime( &unix_tm ); // mktime is thread-safe
  return unix_time_s * NS_IN_S;
#endif
}
*/
void Time::as_iso_date( char* out_str, uint8_t out_str_len ) const
{
#ifdef _WIN32
  SYSTEMTIME local_system_time = UnixTimeNSToLocalSYSTEMTIME( unix_time_ns );
  _snprintf_s( out_str, out_str_len, _TRUNCATE, "%04d-%02d-%02d", local_system_time.wYear, local_system_time.wMonth, local_system_time.wDay );
#else
  time_t unix_time_s = ( time_t )( unix_time_ns / NS_IN_S );
  struct tm unix_tm;
  localtime_r( &unix_time_s, &unix_tm );
  snprintf( out_str, out_str_len, "%04d-%02d-%02d", unix_tm.tm_year + 1900, unix_tm.tm_mon + 1, unix_tm.tm_mday );
#endif
}
void Time::as_iso_date_time( char* out_str, uint8_t out_str_len ) const
{
#ifdef _WIN32
  SYSTEMTIME local_system_time = UnixTimeNSToLocalSYSTEMTIME( unix_time_ns );
  _snprintf_s( out_str, out_str_len, _TRUNCATE,
          "%04d-%02d-%02dT%02d:%02d:%02d.000Z",
        local_system_time.wYear,
        local_system_time.wMonth,
        local_system_time.wDay,
        local_system_time.wHour,
        local_system_time.wMinute,
        local_system_time.wSecond );
#else
  time_t unix_time_s = ( time_t )( unix_time_ns / NS_IN_S );
  struct tm unix_tm;
  localtime_r( &unix_time_s, &unix_tm );
  snprintf( out_str, out_str_len,
          "%04d-%02d-%02dT%02d:%02d:%02d.000Z",
        unix_tm.tm_year + 1900,
        unix_tm.tm_mon + 1,
        unix_tm.tm_mday,
        unix_tm.tm_hour,
        unix_tm.tm_min,
        unix_tm.tm_sec );
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
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).


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

void TimerQueue::addTimer( yidl::auto_Object<Timer> timer )
{
#ifdef _WIN32
  timer->hTimerQueue = hTimerQueue;
  CreateTimerQueueTimer( &timer->hTimer,
                         hTimerQueue,
                         WaitOrTimerCallback,
                         &timer->incRef(),
                         static_cast<DWORD>( timer->get_timeout().as_unix_time_ms() ),
                         static_cast<DWORD>( timer->get_period().as_unix_time_ms() ),
                         WT_EXECUTEDEFAULT );
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

#ifdef _WIN32
VOID CALLBACK TimerQueue::WaitOrTimerCallback( PVOID lpParameter, BOOLEAN )
{
  TimerQueue::Timer* timer = static_cast<TimerQueue::Timer*>( lpParameter );

  Time elapsed_time( Time() - timer->last_fire_time );
  if ( elapsed_time > 0 )
  {
    bool keep_firing = timer->fire( elapsed_time );

    if ( timer->get_period() == 0 )
      Object::decRef( *timer );
    else if ( keep_firing )
      timer->last_fire_time = Time();
    else
      CancelTimerQueueTimer( timer->hTimerQueue, timer->hTimer );
  }
  else
    timer->last_fire_time = Time();
}
#endif

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
#endif
}

TimerQueue::Timer::~Timer()
{ }


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
        timers.push( std::make_pair( Time() + new_timer->get_timeout(), new_timer ) );
      else
        break;
    }
    else
    {
      uint64_t current_unix_time_ns = Time::getCurrentUnixTimeNS();
      if ( timers.top().first <= current_unix_time_ns ) // Earliest timer has expired, fire it
      {
        TimerQueue::Timer* timer = timers.top().second;
        timers.pop();

        bool keep_firing = timer->fire( Time() - timer->last_fire_time );

        if ( timer->get_period() != 0 && keep_firing )
        {
          timer->last_fire_time = Time();
          timers.push( std::make_pair( Time() + timer->get_period(), timer ) );
        }
        else
          Object::decRef( *timer );
      }
      else // Wait on the new timers queue until a new timer arrives or it's time to fire the next timer
      {
        uint64_t timeout_ns = timers.top().first - current_unix_time_ns;
        TimerQueue::Timer* new_timer = new_timers_queue.timed_dequeue( timeout_ns );
        if ( new_timer != NULL )
          timers.push( std::make_pair( Time() + new_timer->get_timeout(), new_timer ) );
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


// volume.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
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
#define LISTXATTR( path, namebuf, size ) ::listxattr( path, namebuf, size, 0 )
#define GETXATTR( path, name, value, size ) ::getxattr( path, name, value, size, 0, 0 )
#define SETXATTR( path, name, value, size, flags ) ::setxattr( path, name, value, size, 0, flags )
#define REMOVEXATTR( path, name ) ::removexattr( path, name, 0 )
#endif
#endif
#endif
namespace YIELD
{
  class readdir_to_listdirCallback : public Volume::readdirCallback
  {
  public:
    readdir_to_listdirCallback( Volume::listdirCallback& listdir_callback )
      : listdir_callback( listdir_callback )
    { }
    readdir_to_listdirCallback& operator=( const readdir_to_listdirCallback& ) { return *this; }
    // Volume::readdirCallback
    bool operator()( const Path& dirent_name, auto_Stat stbuf )
    {
      return listdir_callback( dirent_name );
    }
  private:
    Volume::listdirCallback& listdir_callback;
  };
  class SynclistdirCallback : public Volume::listdirCallback
  {
  public:
    SynclistdirCallback( std::vector<Path>& out_names )
      : out_names( out_names )
    { }
    SynclistdirCallback& operator=( const SynclistdirCallback& ) { return *this; }
    // Volume::listdirCallback
    bool operator()( const Path& name )
    {
      out_names.push_back( name );
      return true;
    }
  private:
    std::vector<Path>& out_names;
  };
};
bool Volume::access( const YIELD::Path& path, int amode )
{
#ifdef _WIN32
  ::SetLastError( ERROR_NOT_SUPPORTED );
  return false;
#else
  return ::access( path, amode ) >= 0;
#endif
}
bool Volume::chmod( const YIELD::Path& path, mode_t mode )
{
#ifdef _WIN32
  ::SetLastError( ERROR_NOT_SUPPORTED );
  return false;
#else
  return ::chmod( path, mode ) != -1;
#endif
}
bool Volume::chown( const YIELD::Path& path, int32_t tag, int32_t gid )
{
#ifdef _WIN32
  ::SetLastError( ERROR_NOT_SUPPORTED );
  return false;
#else
  return ::chown( path, tag, gid ) != -1;
#endif
}
bool Volume::exists( const Path& path )
{
#ifdef _WIN32
  return GetFileAttributesW( path ) != INVALID_FILE_ATTRIBUTES;
#else
  struct stat stbuf;
  return ::stat( path, &stbuf ) == 0;
#endif
}
bool Volume::isdir( const Path& path )
{
#ifdef _WIN32
  DWORD dwFileAttributes = GetFileAttributesW( path );
  return dwFileAttributes != INVALID_FILE_ATTRIBUTES && ( dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY ) == FILE_ATTRIBUTE_DIRECTORY;
#else
  struct stat stbuf;
  return ::stat( path, &stbuf ) == 0 && S_ISDIR( stbuf.st_mode );
#endif
}
bool Volume::isfile( const Path& path )
{
#ifdef _WIN32
  DWORD dwFileAttributes = GetFileAttributesW( path );
  return dwFileAttributes != INVALID_FILE_ATTRIBUTES && ( dwFileAttributes & FILE_ATTRIBUTE_NORMAL ) == FILE_ATTRIBUTE_NORMAL;
#else
  struct stat stbuf;
  return ::stat( path, &stbuf ) == 0 && S_ISREG( stbuf.st_mode );
#endif
}
yidl::auto_Object<YIELD::Stat> Volume::getattr( const Path& path )
{
  return Stat::stat( path );
}
bool Volume::getxattr( const Path& path, const std::string& name, std::string& out_value )
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
bool Volume::listdir( const YIELD::Path& path, const YIELD::Path& match_file_name_prefix, listdirCallback& callback )
{
  readdir_to_listdirCallback readdir_callback( callback );
  return readdir( path, match_file_name_prefix, readdir_callback );
}
bool Volume::listdir( const Path& path, const Path& match_file_name_prefix, std::vector<Path>& out_names )
{
  SynclistdirCallback listdir_callback( out_names );
  return listdir( path, match_file_name_prefix, listdir_callback );
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
bool Volume::mkdir( const Path& path, mode_t mode )
{
#ifdef _WIN32
  return CreateDirectoryW( path, NULL ) != 0;
#else
  return ::mkdir( path, mode ) != -1;
#endif
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
auto_File Volume::open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
{
  return File::open( path, flags, mode, attributes );
}
bool Volume::readdir( const Path& path, const YIELD::Path& match_file_name_prefix, readdirCallback& callback )
{
#ifdef _WIN32
  std::wstring search_pattern( path );
  if ( search_pattern.size() > 0 && search_pattern[search_pattern.size()-1] != L'\\' ) search_pattern.append( L"\\" );
  search_pattern.append( static_cast<const std::wstring&>( match_file_name_prefix ) ).append( L"*" );
  WIN32_FIND_DATA find_data;
  HANDLE dir_handle = FindFirstFileW( search_pattern.c_str(), &find_data );
  if ( dir_handle != INVALID_HANDLE_VALUE )
  {
    do
    {
      if ( find_data.cFileName[0] != '.' )
      {
        if ( !callback( find_data.cFileName, new Stat( find_data ) ) )
        {
          FindClose( dir_handle );
          return false;
        }
      }
    } while ( FindNextFileW( dir_handle, &find_data ) );
    FindClose( dir_handle );
    return true;
  }
  else
    return false;
#elif !defined(__sun)
  DIR* dir_handle = opendir( path );
  if ( dir_handle != NULL )
  {
    struct dirent* next_dirent = ::readdir( dir_handle );
    while ( next_dirent != NULL )
    {
      if ( next_dirent->d_name[0] != '.' && ( next_dirent->d_type == DT_DIR || next_dirent->d_type == DT_REG ) )
      {
        if ( match_file_name_prefix.empty() ||
             strstr( next_dirent->d_name, match_file_name_prefix ) == next_dirent->d_name )
        {
          auto_Stat stbuf = stat( path + next_dirent->d_name );
          if ( stbuf != NULL )
          {
            if ( !callback( next_dirent->d_name, stbuf ) )
            {
              closedir( dir_handle );
              return false;
            }
          }
        }
      }
      next_dirent = ::readdir( dir_handle );
    }
    closedir( dir_handle );
    return true;
  }
  else
    return false;
#else
  return false;
#endif
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
namespace YIELD
{
  class rmtree_readdirCallback : public Volume::readdirCallback
  {
  public:
    rmtree_readdirCallback( Volume& volume ) : volume( volume )
    { }
    rmtree_readdirCallback& operator=( const rmtree_readdirCallback& ) { return *this; }
    virtual bool operator()( const Path& path, auto_Stat stbuf )
    {
      if ( stbuf->ISDIR() )
        return volume.rmtree( path );
      else
        return volume.unlink( path );
    }
  private:
    Volume& volume;
  };
};
bool Volume::rmtree( const Path& path )
{
  auto_Stat path_stat = Stat::stat( path );
  if ( path_stat != NULL && path_stat->ISDIR() )
  {
    rmtree_readdirCallback readdir_callback( *this );
    if ( readdir( path, readdir_callback ) )
      return rmdir( path );
    else
      return false;
  }
  else
    return unlink( path );
}
bool Volume::setattr( const Path& path, uint32_t file_attributes )
{
#ifdef _WIN32
  return SetFileAttributes( path, file_attributes ) != 0;
#else
  return false;
#endif
}
bool Volume::setxattr( const Path& path, const std::string& name, const std::string& value, int flags )
{
#if defined(YIELD_HAVE_XATTR_H)
  return SETXATTR( path, name.c_str(), value.c_str(), value.size(), flags ) != -1;
#elif defined(_WIN32)
  ::SetLastError( ERROR_NOT_SUPPORTED );
#else
  errno = ENOTSUP;
#endif
  return false;
}
bool Volume::statvfs( const Path& path, struct statvfs* buffer )
{
  if ( buffer )
  {
    memset( buffer, 0, sizeof( *buffer ) );
#ifdef _WIN32
    ULARGE_INTEGER uFreeBytesAvailableToCaller, uTotalNumberOfBytes, uTotalNumberOfFreeBytes;
    if ( GetDiskFreeSpaceEx( path, &uFreeBytesAvailableToCaller, &uTotalNumberOfBytes, &uTotalNumberOfFreeBytes ) != 0 )
    {
      buffer->f_bsize = 4096;
      buffer->f_frsize = 4096;
      buffer->f_blocks = static_cast<fsblkcnt_t>( uTotalNumberOfBytes.QuadPart / 4096 );
      buffer->f_bfree = static_cast<fsblkcnt_t>( uTotalNumberOfFreeBytes.QuadPart / 4096 );
      buffer->f_bavail = static_cast<fsblkcnt_t>( uFreeBytesAvailableToCaller.QuadPart / 4096 );
      buffer->f_namemax = PATH_MAX;
      return true;
    }
    else
      return false;
#else
    return ::statvfs( path, buffer ) == 0;
#endif
  }
  else
    return false;
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
bool Volume::touch( const Path& path, mode_t mode )
{
  auto_File file = creat( path, mode );
  return file != NULL;
}
bool Volume::truncate( const Path& path, uint64_t new_size )
{
#ifdef _WIN32
  auto_File file = Volume::open( path, O_CREAT|O_WRONLY, File::DEFAULT_MODE );
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
bool Volume::utimens( const Path& path, const YIELD::Time& atime, const YIELD::Time& mtime, const YIELD::Time& ctime )
{
#ifdef _WIN32
  auto_File file = open( path, O_WRONLY );
  if ( file!= NULL )
  {
    FILETIME ftCreationTime = ctime, ftLastAccessTime = atime, ftLastWriteTime = mtime;
    return SetFileTime( *file, ctime != 0 ? &ftCreationTime : NULL, atime != 0 ? &ftLastAccessTime : NULL, mtime != 0 ? &ftLastWriteTime : NULL ) != 0;
  }
  else
    return false;
#else
  struct timeval tv[2];
  tv[0] = atime;
  tv[1] = mtime;
  return ::utimes( path, tv ) != 0;
#endif
}
Path Volume::volname( const Path& path )
{
#ifdef _WIN32
  std::vector<Path> path_parts;
  path.abspath().split_all( path_parts );
  if ( !path_parts.empty() )
  {
    Path root_dir_path( static_cast<const std::string&>( path_parts[0] ) + PATH_SEPARATOR_STRING );
    wchar_t volume_name[PATH_MAX], file_system_name[PATH_MAX];
    if ( GetVolumeInformation( root_dir_path, volume_name, PATH_MAX, NULL, NULL, NULL, file_system_name, PATH_MAX ) != 0 )
    {
      if ( wcsnlen( volume_name, PATH_MAX ) > 0 )
        return Path( volume_name );
      else
        return static_cast<const std::string&>( path_parts[0] );
    }
  }
#endif
  return Path();
}
#ifdef _WIN32
#pragma warning( pop )
#endif


// xdr_marshaller.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
XDRMarshaller::XDRMarshaller()
{
  buffer = new yidl::StringBuffer;
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
void XDRMarshaller::writeBuffer( const char* key, uint32_t tag, yidl::auto_Buffer value )
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
  buffer->put( &value, sizeof( value ) );
}
void XDRMarshaller::writeFloat( const char* key, uint32_t, float value )
{
  writeKey( key );
  buffer->put( &value, sizeof( value ) );
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
void XDRMarshaller::writeMap( const char* key, uint32_t tag, const yidl::Map& value )
{
  writeInt32( key, tag, static_cast<int32_t>( value.get_size() ) );
  in_map_stack.push_back( true );
  value.marshal( *this );
  in_map_stack.pop_back();
}
void XDRMarshaller::writeSequence( const char* key, uint32_t tag, const yidl::Sequence& value )
{
  writeInt32( key, tag, static_cast<int32_t>( value.get_size() ) );
  value.marshal( *this );
}
void XDRMarshaller::writeString( const char* key, uint32_t tag, const char* value, size_t value_len )
{
  writeInt32( key, tag, static_cast<int32_t>( value_len ) );
  buffer->put( static_cast<const void*>( value ), value_len );
  if ( value_len % 4 != 0 )
  {
    static char zeros[] = { 0, 0, 0 };
    buffer->put( static_cast<const void*>( zeros ), 4 - ( value_len % 4 ) );
  }
}
void XDRMarshaller::writeStruct( const char* key, uint32_t, const yidl::Struct& value )
{
  writeKey( key );
  value.marshal( *this );
}


// xdr_unmarshaller.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
XDRUnmarshaller::XDRUnmarshaller( yidl::auto_Buffer buffer )
  : buffer( buffer )
{ }
void XDRUnmarshaller::read( void* buffer, size_t buffer_len )
{
#ifdef _DEBUG
  if ( this->buffer->size() - this->buffer->position() < buffer_len ) DebugBreak();
#endif
  this->buffer->get( buffer, buffer_len );
}
bool XDRUnmarshaller::readBoolean( const char* key, uint32_t tag )
{
  return readInt32( key, tag ) == 1;
}
void XDRUnmarshaller::readBuffer( const char* key, uint32_t tag, yidl::auto_Buffer value )
{
  size_t size = readInt32( key, tag );
  if ( value->capacity() - value->size() < size ) DebugBreak();
  read( static_cast<void*>( *value ), size );
  value->put( NULL, size );
}
double XDRUnmarshaller::readDouble( const char*, uint32_t )
{
  double value;
  read( &value, sizeof( value ) );
  return value;
}
float XDRUnmarshaller::readFloat( const char*, uint32_t )
{
  float value;
  read( &value, sizeof( value ) );
  return value;
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
void XDRUnmarshaller::readMap( const char* key, uint32_t tag, yidl::Map& value )
{
  size_t size = readInt32( key, tag );
  for ( size_t i = 0; i < size; i++ )
    value.unmarshal( *this );
}
void XDRUnmarshaller::readSequence( const char* key, uint32_t tag, yidl::Sequence& value )
{
  size_t size = readInt32( key, tag );
  if ( size <= UINT16_MAX )
  {
    for ( size_t i = 0; i < size; i++ )
      value.unmarshal( *this );
  }
}
void XDRUnmarshaller::readString( const char* key, uint32_t tag, std::string& value )
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
void XDRUnmarshaller::readStruct( const char*, uint32_t, yidl::Struct& value )
{
  value.unmarshal( *this );
}

