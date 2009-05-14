// Revision: 1416

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
bool CountingSemaphore::timed_acquire( timeout_ns_t timeout_ns )
{
#if defined(_WIN32)
  DWORD timeout_ms = static_cast<DWORD>( timeout_ns / NS_IN_MS );
  DWORD dwRet = WaitForSingleObjectEx( hSemaphore, timeout_ms, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#elif defined(__MACH__)
  mach_timespec_t timeout_m_ts = { timeout_ns / NS_IN_S, timeout_ns % NS_IN_S };
  return semaphore_timedwait( sem, timeout_m_ts ) == KERN_SUCCESS;
#else
  struct timespec timeout_ts = Time( timeout_ns );
  return sem_timedwait( &sem, &timeout_ts ) == 0;
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
        strncpy( out_str, cMessage, out_str_len - 1 );
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
            strncpy( out_str, cMessage, out_str_len - 1 );
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
/*
#if defined(_WIN32) || defined(YIELD_HAVE_POSIX_FILE_AIO)
typedef struct
{
#if defined(_WIN32)
  OVERLAPPED overlapped;
#elif defined(YIELD_HAVE_POSIX_FILE_AIO)
  struct aiocb aiocb;
#endif
  File::aio_read_completion_routine_t aio_read_completion_routine;
  void* aio_read_completion_routine_context;
} aio_read_operation_t;
typedef struct
{
#if defined(_WIN32)
  OVERLAPPED overlapped;
#elif defined(YIELD_HAVE_POSIX_FILE_AIO)
  struct aiocb aiocb;
#endif
  File::aio_write_completion_routine_t aio_write_completion_routine;
  void* aio_write_completion_routine_context;
} aio_write_operation_t;
#endif
*/
File::File()
  : fd( INVALID_HANDLE_VALUE )
{ }
File::File( const Path& path )
{
  fd = _open( path, DEFAULT_FLAGS, DEFAULT_MODE, DEFAULT_ATTRIBUTES );
  if ( fd == INVALID_HANDLE_VALUE )
    throw YIELD::Exception();
}
File::File( const Path& path, uint32_t flags )
{
  fd = _open( path, flags, DEFAULT_MODE, DEFAULT_ATTRIBUTES );
  if ( fd == INVALID_HANDLE_VALUE )
    throw YIELD::Exception();
}
File::File( const Path& path, uint32_t flags, mode_t mode )
{
  fd = _open( path, flags, mode, DEFAULT_ATTRIBUTES );
  if ( fd == INVALID_HANDLE_VALUE )
    throw YIELD::Exception();
}
File::File( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
{
  fd = _open( path, flags, mode, attributes );
  if ( fd == INVALID_HANDLE_VALUE )
    throw YIELD::Exception();
}
#ifdef _WIN32
File::File( char* path )
{
  fd = _open( path, DEFAULT_FLAGS, DEFAULT_MODE, DEFAULT_ATTRIBUTES );
  if ( fd == INVALID_HANDLE_VALUE )
    throw YIELD::Exception();
}
File::File( char* path, uint32_t flags )
{
  fd = _open( path, flags, DEFAULT_MODE, DEFAULT_ATTRIBUTES );
  if ( fd == INVALID_HANDLE_VALUE )
    throw YIELD::Exception();
}
File::File( const char* path )
{
  fd = _open( path, DEFAULT_FLAGS, DEFAULT_MODE, DEFAULT_ATTRIBUTES );
  if ( fd == INVALID_HANDLE_VALUE  )
    throw YIELD::Exception();
}
File::File( const char* path, uint32_t flags )
{
  fd = _open( path, flags, DEFAULT_MODE, DEFAULT_ATTRIBUTES );
  if ( fd == INVALID_HANDLE_VALUE  )
    throw YIELD::Exception();
}
#endif
File::File( fd_t fd )
: fd( fd )
{ }
auto_Object<File> File::open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
{
  fd_t fd = _open( path, flags, mode, attributes );
  if ( fd != INVALID_HANDLE_VALUE )
    return new File( fd );
  else
    return NULL;
}
/*
int File::aio_read( void* buffer, size_t buffer_len, aio_read_completion_routine_t aio_read_completion_routine, void* aio_read_completion_routine_context )
{
#if defined(_WIN32) || defined(YIELD_HAVE_POSIX_FILE_AIO)
  aio_read_operation_t* aio_read_op = new aio_read_operation_t;
  memset( aio_read_op, 0, sizeof( *aio_read_op ) );
  aio_read_op->aio_read_completion_routine = aio_read_completion_routine;
  aio_read_op->aio_read_completion_routine_context = aio_read_completion_routine_context;
#if defined(_WIN32)
  if ( ReadFileEx( fd, buffer, static_cast<DWORD>( buffer_len ), ( LPOVERLAPPED )aio_read_op, ( LPOVERLAPPED_COMPLETION_ROUTINE )overlapped_read_completion ) != 0 )
    return 0;
  else
    return -1;
#elif defined(YIELD_HAVE_POSIX_FILE_AIO)
  aio_read_op->aiocb.aio_fildes = fd;
  aio_read_op->aiocb.aio_buf = buffer;
  aio_read_op->aiocb.aio_nbytes = buffer_len;
  aio_read_op->aiocb.aio_offset = 0;
  aio_read_op->aiocb.aio_sigevent.sigev_notify = SIGEV_THREAD;
  aio_read_op->aiocb.aio_sigevent.sigev_notify_function = aio_read_notify;
  aio_read_op->aiocb.aio_sigevent.sigev_value.sival_ptr = aio_read_op;
  return ::aio_read( &aio_read_op->aiocb );
#endif
#else
  DebugBreak();
  // TODO: do a normal read
  return -1;
#endif
}
int File::aio_write( const void* buffer, size_t buffer_len, aio_write_completion_routine_t aio_write_completion_routine, void* aio_write_completion_routine_context )
{
#if defined(_WIN32) || defined(YIELD_HAVE_POSIX_FILE_AIO)
  aio_write_operation_t* aio_write_op = new aio_write_operation_t;
  memset( aio_write_op, 0, sizeof( *aio_write_op ) );
  aio_write_op->aio_write_completion_routine = aio_write_completion_routine;
  aio_write_op->aio_write_completion_routine_context = aio_write_completion_routine_context;
#if defined(_WIN32)
  if ( WriteFileEx( fd, buffer, static_cast<DWORD>( buffer_len ), ( LPOVERLAPPED )aio_write_op, ( LPOVERLAPPED_COMPLETION_ROUTINE )overlapped_write_completion ) != 0 )
    return 0;
  else
    return -1;
#elif defined(YIELD_HAVE_POSIX_FILE_AIO)
  aio_write_op->aiocb.aio_fildes = fd;
  aio_write_op->aiocb.aio_buf = const_cast<void*>( buffer );
  aio_write_op->aiocb.aio_nbytes = buffer_len;
  aio_write_op->aiocb.aio_offset = 0;
  aio_write_op->aiocb.aio_sigevent.sigev_notify = SIGEV_THREAD;
  aio_write_op->aiocb.aio_sigevent.sigev_notify_function = aio_write_notify;
  aio_write_op->aiocb.aio_sigevent.sigev_value.sival_ptr = aio_write_op;
  return ::aio_write( &aio_write_op->aiocb );
#endif
#else
  DebugBreak();
  // TODO: do a normal write
  return -1;
#endif
}
#if defined(_WIN32)
void __stdcall File::overlapped_read_completion( DWORD dwError, DWORD dwBytesTransferred, LPVOID lpOverlapped )
{
  aio_read_operation_t* aio_read_op = reinterpret_cast<aio_read_operation_t*>( lpOverlapped );
  aio_read_op->aio_read_completion_routine( dwError, dwBytesTransferred, aio_read_op->aio_read_completion_routine_context );
  delete aio_read_op;
}
void __stdcall File::overlapped_write_completion( DWORD dwError, DWORD dwBytesTransferred, LPVOID lpOverlapped )
{
  aio_write_operation_t* aio_write_op = reinterpret_cast<aio_write_operation_t*>( lpOverlapped );
  aio_write_op->aio_write_completion_routine( dwError, dwBytesTransferred, aio_write_op->aio_write_completion_routine_context );
  delete aio_write_op;
}
#elif defined(YIELD_HAVE_POSIX_FILE_AIO)
void File::aio_read_notify( sigval_t sigval )
{
  aio_read_operation_t* aio_read_op = reinterpret_cast<aio_read_operation_t*>( sigval.sival_ptr );
  aio_read_op->aio_read_completion_routine( static_cast<unsigned long>( aio_error( &aio_read_op->aiocb ) ), static_cast<size_t>( aio_return( &aio_read_op->aiocb ) ), aio_read_op->aio_read_completion_routine_context );
  delete aio_read_op;
}
void File::aio_write_notify( sigval_t sigval )
{
  aio_write_operation_t* aio_write_op = reinterpret_cast<aio_write_operation_t*>( sigval.sival_ptr );
  aio_write_op->aio_write_completion_routine( static_cast<unsigned long>( aio_error( &aio_write_op->aiocb ) ), static_cast<size_t>( aio_return( &aio_write_op->aiocb ) ), aio_write_op->aio_write_completion_routine_context );
  delete aio_write_op;
}
#endif
*/
bool File::close()
{
  if ( fd != INVALID_HANDLE_VALUE ) // Have to test this, since CloseHandle on an invalid handle crashes instead of simply failing
  {
    if ( _close( fd ) )
    {
      fd = INVALID_HANDLE_VALUE;
      return true;
    }
    else
      return false;
  }
  else
    return false;
}
bool File::_close( fd_t fd )
{
#ifdef _WIN32
  return CloseHandle( fd ) != 0;
#else
  return ::close( fd ) >= 0;
#endif
}
bool File::datasync()
{
#if defined(_WIN32) || defined(__MACH__)
  return true;
#else
  return fdatasync( fd ) != -1;
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
auto_Object<Stat> File::getattr()
{
  return new Stat( fd );
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
fd_t File::_open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
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
  if ( ( flags & O_TRUNC ) == O_TRUNC && ( flags & O_CREAT ) != O_CREAT )
  {
    SetFilePointer( fd, 0, NULL, FILE_BEGIN );
    SetEndOfFile( fd );
  }
#else
  int fd = ::open( path, flags, mode );
#endif
  return fd;
}
Stream::Status File::read( void* buffer, size_t buffer_len, uint64_t offset, size_t* out_bytes_read )
{
  if ( seek( offset, SEEK_SET ) )
    return read( buffer, buffer_len, out_bytes_read );
  else
    return STREAM_STATUS_ERROR;
}
Stream::Status File::read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
{
#ifdef _WIN32
  DWORD dwBytesRead;
  if ( ReadFile( fd, buffer, static_cast<DWORD>( buffer_len ), &dwBytesRead, NULL ) )
  {
    if ( out_bytes_read )
      *out_bytes_read = dwBytesRead;
    return STREAM_STATUS_OK;
  }
  else
    return STREAM_STATUS_ERROR;
#else
  ssize_t read_ret = ::read( fd, buffer, buffer_len );
  if ( read_ret >= 0 )
  {
    if ( out_bytes_read )
      *out_bytes_read = static_cast<uint64_t>( read_ret );
    return STREAM_STATUS_OK;
  }
  else
    return STREAM_STATUS_ERROR;
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
bool File::setxattr( const std::string& name, const std::string& value, int flags )
{
#ifdef YIELD_HAVE_XATTR_H
  return FSETXATTR( fd, name.c_str(), value.c_str(), value.size(), flags ) != -1;
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
  DWORD dwDistanceToMoveLow = SetFilePointer( fd, uliOffset.LowPart, ( PLONG )&uliOffset.HighPart, whence ) != 0;
  if ( dwDistanceToMoveLow != INVALID_SET_FILE_POINTER )
  {
    /*
    ULARGE_INTEGER uliNewOffset;
    uliNewOffset.LowPart = dwDistanceToMoveLow;
    uliNewOffset.HighPart = uliOffset.HighPart;
    offset = uliNewOffset.Quadpart;
    */
    return true;
  }
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
  if ( seek( new_size ) )
    return SetEndOfFile( fd ) != 0;
  else
    return false;
#else
  return ::ftruncate( fd, new_size ) != -1;
#endif
}
Stream::Status File::writev( const iovec* buffers, uint32_t buffers_count, uint64_t offset, size_t* out_bytes_written )
{
  if ( seek( offset ) )
    return writev( buffers, buffers_count, out_bytes_written );
  else
    return STREAM_STATUS_ERROR;
}
Stream::Status File::writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
{
#ifdef _WIN32
  // WriteFileGather requires the buffers to be aligned on page boundaries and overlapped completion.. not worth the trouble currently.
  size_t total_bytes_written = 0;
  for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
  {
    DWORD dwBytesWritten;
    if ( WriteFile( fd, buffers[buffer_i].iov_base, static_cast<DWORD>( buffers[buffer_i].iov_len ), &dwBytesWritten, NULL ) )
      total_bytes_written += dwBytesWritten;
    else
      return STREAM_STATUS_ERROR;
  }
  if ( out_bytes_written )
    *out_bytes_written = total_bytes_written;
  return STREAM_STATUS_OK;
#else
  ssize_t writev_ret = ::writev( fd, reinterpret_cast<const ::iovec*>( buffers ), buffers_count );
  if ( writev_ret >= 0 )
  {
    if ( out_bytes_written )
      *out_bytes_written = static_cast<uint64_t>( writev_ret );
    return STREAM_STATUS_OK;
  }
  else
    return STREAM_STATUS_ERROR;
#endif
}


// log.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
namespace YIELD
{
  class ostreamWrapper : public Object, public OutputStream
  {
  public:
    ostreamWrapper( std::ostream& os )
      : os( os )
    { }
    // OutputStream
    Stream::Status writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written = 0 )
    {
      size_t bytes_written = 0;
      for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
      {
        os.write( static_cast<char*>( buffers[buffer_i].iov_base ), static_cast<std::streamsize>( buffers[buffer_i].iov_len ) );
        bytes_written += buffers[buffer_i].iov_len;
      }
      if ( out_bytes_written )
        *out_bytes_written = bytes_written;
      return STREAM_STATUS_OK;
    }
  private:
    std::ostream& os;
  };
};
Log::Stream::Stream( auto_Object<Log> log, Log::Level level )
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
OutputStream::Status Log::Stream::writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
{
  if ( level <= log->get_level() )
  {
    for ( uint32_t buffer_i = 0; buffer_i < buffers_count; buffer_i++ )
    {
      char* sanitized_buffer = sanitize( static_cast<const unsigned char*>( buffers[buffer_i].iov_base ), buffers[buffer_i].iov_len );
      oss.write( sanitized_buffer, static_cast<std::streamsize>( buffers[buffer_i].iov_len ) );
      delete [] sanitized_buffer;
      if ( out_bytes_written )
        *out_bytes_written += buffers[buffer_i].iov_len;
    }
  }
  return STREAM_STATUS_OK;
}
Log::Log( const Path& file_path, Level level )
  : level( level )
{
  underlying_output_stream.reset( new File( file_path, O_CREAT|O_WRONLY|O_APPEND ) );
}
Log::Log( std::ostream& os, Level level )
  : level( level )
{
  underlying_output_stream.reset( new ostreamWrapper( os ) );
}
Log::Log( std::auto_ptr<OutputStream> underlying_output_stream, Level level )
  : underlying_output_stream( underlying_output_stream ), level( level )
{ }
char* Log::sanitize( const unsigned char* str, size_t str_len )
{
  char *sanitized_str = new char[str_len];
  for ( size_t str_i = 0; str_i < str_len; str_i++ )
  {
    if ( str[str_i] == '\r' || str[str_i] == '\n' || ( str[str_i] >= 32 && str[str_i] <= 126 ) )
      sanitized_str[str_i] = str[str_i];
    else
      sanitized_str[str_i] = '.';
  }
  return sanitized_str;
}
void Log::write( const unsigned char* str, size_t str_len )
{
  char* sanitized_str = sanitize( str, str_len );
  OutputStream::write( sanitized_str, str_len );
  delete [] sanitized_str;
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
    uint16_t online_physical_processor_count = 0;
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
    if ( online_physical_processor_count > 0 )
      return online_physical_processor_count;
    else
      return 1;
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


MemoryMappedFile::MemoryMappedFile( const Path& path )
  : File( path, O_RDWR|O_SYNC ), flags( O_RDWR|O_SYNC )
{
  init( path, 0, flags );
}

MemoryMappedFile::MemoryMappedFile( const Path& path, size_t minimum_size )
  : File( path, flags ), flags( O_RDWR|O_SYNC )
{
  init( path, 0, flags );
}

MemoryMappedFile::MemoryMappedFile( const Path& path, size_t minimum_size, uint32_t flags )
  : File( path, flags ), flags( flags )
{
  init( path, minimum_size, flags );
}

void MemoryMappedFile::init( const Path& path, size_t minimum_size, uint32_t flags )
{
  if ( ( flags & O_TRUNC ) == 0 )
  {
#ifdef _WIN32
    ULARGE_INTEGER size;
    size.LowPart = GetFileSize( *this, &size.HighPart );
    this->size = static_cast<size_t>( size.QuadPart );
#else
    struct stat temp_stat;
    if ( stat( path, &temp_stat ) != -1 )
      size = temp_stat.st_size;
    else
      size = 0;
#endif
  }
  else
    size = 0;

  start = NULL;
#ifdef _WIN32
  mapping = NULL;
#endif

  resize( std::max( minimum_size, size ) );
}

bool MemoryMappedFile::close()
{
  if ( start != NULL )
  {
    writeBack();
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

  return File::close();
}

void MemoryMappedFile::resize( size_t new_size )
{
  if ( new_size > 0 )
  {
    if ( new_size < 4 )
      new_size = 4;

#ifdef _WIN32
    if ( start != NULL )
      UnmapViewOfFile( start );

    if ( mapping != NULL )
      CloseHandle( mapping );

    if ( size == new_size || truncate( new_size ) )
    {
      unsigned long map_flags = PAGE_READONLY;
      if ( ( flags & O_RDWR ) == O_RDWR || ( flags & O_WRONLY ) == O_WRONLY )
        map_flags = PAGE_READWRITE;

      ULARGE_INTEGER uliNewSize; uliNewSize.QuadPart = new_size;
      mapping = CreateFileMapping( *this, NULL, map_flags, uliNewSize.HighPart, uliNewSize.LowPart, NULL );
      if ( mapping != NULL )
      {
        map_flags = FILE_MAP_READ;
        if( ( flags & O_RDWR ) || ( flags & O_WRONLY ) )
          map_flags = FILE_MAP_ALL_ACCESS;

        start = static_cast<char*>( MapViewOfFile( mapping, map_flags, 0, 0, 0 ) );
        if ( start != NULL )
          size = new_size;
        else
          throw Exception();
      }
      else
        throw Exception();
    }
    else
      throw Exception();
#else
    if ( start != NULL )
    {
      writeBack();
      if ( munmap( start, size ) == -1 )
        throw Exception();
    }

    if ( size == new_size || truncate( new_size ) )
    {
      unsigned long mmap_flags = PROT_READ;
      if( ( flags & O_RDWR ) == O_RDWR || ( flags & O_WRONLY ) == O_WRONLY )
        mmap_flags |= PROT_WRITE;

      void* mmap_ret = mmap( 0, new_size, mmap_flags, MAP_SHARED, *this, 0 );
      if ( mmap_ret != MAP_FAILED )
      {
        start = static_cast<char*>( mmap_ret );
        size = new_size;
      }
      else
        throw Exception();
    }
    else
      throw Exception();
#endif
  }
}

void MemoryMappedFile::writeBack()
{
#ifdef _WIN32
  FlushViewOfFile( start, 0 );
#else
  msync( start, size, MS_SYNC );
#endif
}

void MemoryMappedFile::writeBack( size_t offset, size_t length )
{
#ifdef _WIN32
  FlushViewOfFile( start + offset, length );
#else
  msync( start + offset, length, MS_SYNC );
#endif
}

void MemoryMappedFile::writeBack( void* ptr, size_t length )
{
#if defined(_WIN32)
  FlushViewOfFile( ptr, length );
#elif defined(__sun)
  msync( static_cast<char*>( ptr ), length, MS_SYNC );
#else
  msync( ptr, length, MS_SYNC );
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
  if ( ( os_handle = CreateEvent( NULL, FALSE, TRUE, NULL ) ) == NULL ) DebugBreak();
#else
  os_handle = new pthread_mutex_t;
  if ( pthread_mutex_init( static_cast<pthread_mutex_t*>( os_handle ), NULL ) != 0 ) DebugBreak();
#endif
}
Mutex::~Mutex()
{
#ifdef _WIN32
  if ( os_handle ) CloseHandle( os_handle );
#else
  if ( os_handle ) pthread_mutex_destroy( static_cast<pthread_mutex_t*>( os_handle ) );
  delete ( pthread_mutex_t* ) os_handle;
#endif
}
bool Mutex::acquire()
{
#ifdef _WIN32
  DWORD dwRet = WaitForSingleObjectEx( os_handle, INFINITE, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#else
  pthread_mutex_lock( static_cast<pthread_mutex_t*>( os_handle ) );
  return true;
#endif
}
bool Mutex::try_acquire()
{
#ifdef _WIN32
  DWORD dwRet = WaitForSingleObjectEx( os_handle, 0, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#else
  return pthread_mutex_trylock( static_cast<pthread_mutex_t*>( os_handle ) ) == 0;
#endif
}
bool Mutex::timed_acquire( timeout_ns_t timeout_ns )
{
#ifdef _WIN32
  DWORD timeout_ms = static_cast<DWORD>( timeout_ns / NS_IN_MS );
  DWORD dwRet = WaitForSingleObjectEx( os_handle, timeout_ms, TRUE );
  return dwRet == WAIT_OBJECT_0 || dwRet == WAIT_ABANDONED;
#else
#ifdef YIELD_HAVE_PTHREAD_MUTEX_TIMEDLOCK
  struct timespec timeout_ts = Time( timeout_ns );
  return ( pthread_mutex_timedlock( static_cast<pthread_mutex_t*>( os_handle ), &timeout_ts ) == 0 );
#else
  if ( pthread_mutex_trylock( static_cast<pthread_mutex_t*>( os_handle ) ) == 0 )
    return true;
  else
  {
    usleep( timeout_ns / 1000 );
    return ( pthread_mutex_trylock( static_cast<pthread_mutex_t*>( os_handle ) ) == 0 );
  }
#endif
#endif
}
void Mutex::release()
{
#ifdef _WIN32
  SetEvent( os_handle );
#else
  pthread_mutex_unlock( static_cast<pthread_mutex_t*>( os_handle ) );
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
//    wide_path.assign( _wide_path, MultiByteToWideChar( CP_UTF8, 0, utf8_path.c_str(), ( int )utf8_path.size(), _wide_path, MAX_PATH ) );
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
  wchar_t _wide_path[MAX_PATH];
  wide_path.assign( _wide_path, MultiByteToWideChar( GetACP(), 0, host_charset_path.c_str(), static_cast<int>( host_charset_path.size() ), _wide_path, MAX_PATH ) );
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
  char _host_charset_path[MAX_PATH];
  int _host_charset_path_len = WideCharToMultiByte( GetACP(), 0, this->wide_path.c_str(), ( int )this->wide_path.size(), _host_charset_path, MAX_PATH, 0, 0 );
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
      char _utf8_path[MAX_PATH];
      int _utf8_path_len = WideCharToMultiByte( CP_UTF8, 0, wide_path.c_str(), ( int )wide_path.size(), _utf8_path, MAX_PATH, 0, 0 );
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
    char* _frompath = const_cast<char*>( frompath.c_str() ); char _topath[MAX_PATH], *_topath_p = _topath;
    size_t _frompath_size = frompath.size(), _topath_size = MAX_PATH;
	//::iconv( converter, NULL, 0, NULL, 0 ) != -1 &&
    size_t iconv_ret;
    if ( ( iconv_ret = ::iconv( converter, ( ICONV_SOURCE_CAST )&_frompath, &_frompath_size, &_topath_p, &_topath_size ) ) != static_cast<size_t>( -1 ) )
      topath.assign( _topath, MAX_PATH - _topath_size );
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
  wchar_t abspath_buffer[MAX_PATH];
  DWORD abspath_buffer_len = GetFullPathNameW( wide_path.c_str(), MAX_PATH, abspath_buffer, NULL );
  return Path( abspath_buffer, abspath_buffer_len );
#else
  char abspath_buffer[MAX_PATH];
  realpath( host_charset_path.c_str(), abspath_buffer );
  return Path( abspath_buffer );
#endif
}


// pretty_print_output_stream.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
PrettyPrintOutputStream::PrettyPrintOutputStream( OutputStream& underlying_output_stream )
  : underlying_output_stream( underlying_output_stream )
{ }
void PrettyPrintOutputStream::writeBool( const Declaration& decl, bool value )
{
  underlying_output_stream.write( ( ( value ) ? "true, " : "false, " ) );
}
void PrettyPrintOutputStream::writeDouble( const Declaration& decl, double value )
{
  std::ostringstream value_oss; value_oss << value << ", ";
  underlying_output_stream.write( value_oss.str() );
}
void PrettyPrintOutputStream::writeInt64( const Declaration& decl, int64_t value )
{
  std::ostringstream value_oss; value_oss << value << ", ";
  underlying_output_stream.write( value_oss.str() );
}
void PrettyPrintOutputStream::writeObject( const Declaration& decl, Object& value, Object::GeneralType value_general_type )
{
  if ( value_general_type == Object::UNKNOWN )
    value_general_type = value.get_general_type();
  if ( value_general_type == Object::SEQUENCE )
  {
    underlying_output_stream.write( "[ " );
    value.serialize( *this );
    underlying_output_stream.write( " ], " );
  }
  else
  {
    underlying_output_stream.write( value.get_type_name() );
    underlying_output_stream.write( "( " );
    value.serialize( *this );
    underlying_output_stream.write( " ), " );
  }
}
void PrettyPrintOutputStream::writeString( const Declaration& decl, const char* value, size_t value_len )
{
  underlying_output_stream.write( value, value_len );
  underlying_output_stream.write( ", " );
}


// process.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if defined(_WIN32)
#include <windows.h>
#else
#include <sys/wait.h> // For waitpid
#ifdef __MACH__
#include <mach-o/dyld.h> // For _NSGetExecutablePath
#endif
#endif
Path Process::getExeFilePath( const char* argv0 )
{
#if defined(_WIN32)
  char exe_file_path[MAX_PATH];
  if ( GetModuleFileNameA( NULL, exe_file_path, MAX_PATH ) )
    return exe_file_path;
#elif defined(__linux__)
  char exe_file_path[MAX_PATH];
  int ret;
  if ( ( ret = readlink( "/proc/self/exe", exe_file_path, MAX_PATH ) ) != -1 )
  {
    exe_file_path[ret] = 0;
    return exe_file_path;
  }
#elif defined(__MACH__)
  char exe_file_path[MAX_PATH];
  uint32_t bufsize = MAX_PATH;
  if ( _NSGetExecutablePath( exe_file_path, &bufsize ) == 0 )
  {
    exe_file_path[bufsize] = 0;
    char linked_exe_file_path[MAX_PATH]; int ret;
    if ( ( ret = readlink( exe_file_path, linked_exe_file_path, MAX_PATH ) ) != -1 )
    {
      linked_exe_file_path[ret] = 0;
      return linked_exe_file_path;
    }
    char absolute_exe_file_path[MAX_PATH];
    if ( realpath( exe_file_path, absolute_exe_file_path ) != NULL )
      return absolute_exe_file_path;
    return exe_file_path;
  }
#endif
  return argv0;
}
Path Process::getExeDirPath( const char* argv0 )
{
  Path exe_file_path = getExeFilePath( argv0 );
  return exe_file_path.split().first;
}
#ifdef _WIN32
CountingSemaphore pause_semaphore;
BOOL CtrlHandler( DWORD fdwCtrlType )
{
  if ( fdwCtrlType == CTRL_C_EVENT )
  {
    pause_semaphore.release();
    return TRUE;
  }
  else
    return FALSE;
}
#endif
void Process::pause()
{
#ifdef _WIN32
  SetConsoleCtrlHandler( ( PHANDLER_ROUTINE )CtrlHandler, TRUE );
  pause_semaphore.acquire();
#else
  ::pause();
#endif
}
#ifdef _WIN32
int Process::getArgvFromCommandLine( const char* command_line, argv_vector& argv )
{
  char exec_name[MAX_PATH];
  GetModuleFileNameA( NULL, exec_name, MAX_PATH );
  argv.push_back( new char[ strlen( exec_name ) + 1 ] );
  strcpy( argv.back(), exec_name );
  const char *start_p = command_line, *p = start_p;
  while ( *p != 0 )
  {
    while ( *p != ' ' && *p != 0 ) p++;
    char* arg = new char[ ( p - start_p + 1 ) ];
    memcpy( arg, start_p, p - start_p );
    arg[p-start_p] = 0;
    argv.push_back( arg );
    if ( *p != 0 )
    {
      p++;
      start_p = p;
    }
  }
  return ( int )argv.size();
}
int Process::WinMainTomain( LPSTR lpszCmdLine, int ( *main )( int, char** ) )
{
  argv_vector argv;
  getArgvFromCommandLine( lpszCmdLine, argv );
  int ret = main( ( int )argv.size(), &argv[0] );
  return ret;
}
#endif
Process::Process( const Path& executable_file_path, const char** argv )
{
#ifdef _WIN32
  const std::string& executable_file_path_str = static_cast<const std::string&>( executable_file_path );
  std::string catted_argv;
  if ( executable_file_path_str.find( ' ' ) == -1 )
    catted_argv.append( executable_file_path_str );
  else
  {
    catted_argv.append( "\"", 1 );
    catted_argv.append( executable_file_path_str );
    catted_argv.append( "\"", 1 );
  }
  size_t argv_i = 0;
  while ( argv[argv_i] != NULL )
  {
    catted_argv.append( " ", 1 );
    catted_argv.append( argv[argv_i] );
    argv_i++;
  }
  {
    SECURITY_ATTRIBUTES pipe_security_attributes;
    pipe_security_attributes.nLength = sizeof( SECURITY_ATTRIBUTES );
    pipe_security_attributes.bInheritHandle = TRUE;
    pipe_security_attributes.lpSecurityDescriptor = NULL;
    if ( !CreatePipe( &hChildStdInput_read, &hChildStdInput_write, &pipe_security_attributes, 0 ) ) throw Exception();
    if ( !SetHandleInformation( hChildStdInput_write, HANDLE_FLAG_INHERIT, 0 ) ) throw Exception();
    if ( !CreatePipe( &hChildStdOutput_read, &hChildStdOutput_write, &pipe_security_attributes, 0 ) ) throw Exception();
    if ( !SetHandleInformation( hChildStdOutput_read, HANDLE_FLAG_INHERIT, 0 ) ) throw Exception();
    if ( !CreatePipe( &hChildStdError_read, &hChildStdError_write, &pipe_security_attributes, 0 ) ) throw Exception();
    if ( !SetHandleInformation( hChildStdError_read, HANDLE_FLAG_INHERIT, 0 ) ) throw Exception();
  }
  STARTUPINFO startup_info;
  ZeroMemory( &startup_info, sizeof( STARTUPINFO ) );
  startup_info.cb = sizeof( STARTUPINFO );
  startup_info.hStdInput = hChildStdInput_read;
  startup_info.hStdOutput = hChildStdOutput_write;
  startup_info.hStdError = hChildStdError_write;
  startup_info.dwFlags = STARTF_USESTDHANDLES;
  PROCESS_INFORMATION proc_info;
  ZeroMemory( &proc_info, sizeof( PROCESS_INFORMATION ) );
  if ( CreateProcess( executable_file_path, ( LPWSTR )catted_argv.c_str(), NULL, NULL, TRUE, CREATE_NO_WINDOW, NULL, NULL, &startup_info, &proc_info ) )
  {
    hChildProcess = proc_info.hProcess;
    hChildThread = proc_info.hThread;
  }
  else
    throw Exception();
#else
  if ( pipe( child_stdin_pipe ) == -1 ) throw Exception();
  if ( pipe( child_stdout_pipe ) == -1 ) throw Exception();
  if ( pipe( child_stderr_pipe ) == -1 ) throw Exception();
  child_pid = fork();
  if ( child_pid == -1 )
    throw Exception();
  else if ( child_pid == 0 ) // Child
  {
    close( STDIN_FILENO );
    dup2( child_stdin_pipe[0], STDIN_FILENO ); // Set stdin to read end of stdin pipe
    close( child_stdin_pipe[0] );
    close( child_stdin_pipe[1] );
    close( STDOUT_FILENO );
    dup2( child_stdout_pipe[1], STDOUT_FILENO ); // Set stdout to write end of stdout pipe
    close( child_stdout_pipe[0] );
    close( child_stdout_pipe[1] );
    close( STDERR_FILENO );
    dup2( child_stderr_pipe[1], STDERR_FILENO ); // Set stderr to write end of stderr pipe
    close( child_stderr_pipe[0] );
    close( child_stderr_pipe[1] );
    std::vector<char*> argv_with_executable_file_path;
    argv_with_executable_file_path.push_back( const_cast<char*>( static_cast<const char*>( executable_file_path ) ) );
    size_t argv_i = 0;
    while ( argv[argv_i] != NULL )
    {
      argv_with_executable_file_path.push_back( const_cast<char*>( argv[argv_i] ) );
      argv_i++;
    }
    argv_with_executable_file_path.push_back( NULL );
    execv( executable_file_path, &argv_with_executable_file_path[0] );
  }
  else // Parent
  {
    close( child_stdin_pipe[0] ); // Close read end of stdin pipe
    close( child_stdout_pipe[1] ); // Close write end of stdout
    close( child_stderr_pipe[1] ); // Close write end of stderr
  }
#endif
}
Process::~Process()
{
#ifdef _WIN32
  TerminateProcess( hChildProcess, 0 );
  WaitForSingleObject( hChildProcess, INFINITE );
  CloseHandle( hChildProcess );
  CloseHandle( hChildThread );
  CloseHandle( hChildStdInput_read );
  CloseHandle( hChildStdInput_write );
  CloseHandle( hChildStdOutput_read );
  CloseHandle( hChildStdOutput_write );
#else
  close( child_stdin_pipe[1] );
  close( child_stdout_pipe[0] );
  close( child_stderr_pipe[0] );
#endif
}
bool Process::poll( int* out_return_code )
{
#ifdef _WIN32
  if ( WaitForSingleObject( hChildProcess, 0 ) != WAIT_TIMEOUT )
  {
    if ( out_return_code )
    {
      DWORD dwChildExitCode;
      GetExitCodeProcess( hChildProcess, &dwChildExitCode );
      *out_return_code = ( int )dwChildExitCode;
    }
    return true;
  }
  else
    return false;
#else
  return waitpid( child_pid, out_return_code, WNOHANG ) >= 0;
#endif
}
Stream::Status Process::read_stderr( void* buffer, size_t buffer_len, size_t* out_bytes_read )
{
#ifdef _WIN32
  return _read( hChildStdOutput_read, buffer, buffer_len, out_bytes_read );
#else
  return _read( child_stdout_pipe[0], buffer, buffer_len, out_bytes_read );
#endif
}
Stream::Status Process::read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
{
#ifdef _WIN32
  return _read( hChildStdError_read, buffer, buffer_len, out_bytes_read );
#else
  return _read( child_stderr_pipe[0], buffer, buffer_len, out_bytes_read );
#endif
}
Stream::Status Process::_read( fd_t fd, void* buffer, size_t buffer_len, size_t* out_bytes_read )
{
#ifdef _WIN32
  DWORD dwBytesRead;
  if ( ReadFile( fd, buffer, static_cast<DWORD>( buffer_len ), &dwBytesRead, NULL ) )
  {
    if ( out_bytes_read )
      *out_bytes_read = dwBytesRead;
    return STREAM_STATUS_OK;
  }
  else
    return STREAM_STATUS_ERROR;
#else
  ssize_t read_ret = ::read( child_stdout_pipe[0], buffer, buffer_len );
  if ( read_ret >= 0 )
  {
    if ( out_bytes_read )
      *out_bytes_read = static_cast<size_t>( read_ret );
    return STREAM_STATUS_OK;
  }
  else
    return STREAM_STATUS_ERROR;
#endif
}
Stream::Status Process::write( const void* buffer, size_t buffer_len, size_t* out_bytes_written )
{
#ifdef _WIN32
  DWORD dwBytesWritten;
  if ( WriteFile( hChildStdInput_write, buffer, static_cast<DWORD>( buffer_len ), &dwBytesWritten, NULL ) )
  {
    if ( out_bytes_written )
      *out_bytes_written = dwBytesWritten;
    return STREAM_STATUS_OK;
  }
  else
    return STREAM_STATUS_ERROR;
#else
  ssize_t write_ret = ::write( child_stdin_pipe[1], buffer, buffer_len );
  if ( write_ret >= 0 )
  {
    if ( out_bytes_written )
      *out_bytes_written = static_cast<size_t>( write_ret );
    return STREAM_STATUS_OK;
  }
  else
    return STREAM_STATUS_ERROR;
#endif
}
int Process::wait()
{
#ifdef _WIN32
  WaitForSingleObject( hChildProcess, INFINITE );
  DWORD dwChildExitCode;
  GetExitCodeProcess( hChildProcess, &dwChildExitCode );
  return ( int )dwChildExitCode;
#else
  int stat_loc;
  if ( waitpid( child_pid, &stat_loc, 0 ) >= 0 )
    return stat_loc;
  else
    throw Exception();
#endif
}


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
  YIELD::DebugBreak();
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
  YIELD::DebugBreak();
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
ProcessorSet::ProcessorSet( const ProcessorSet& other )
{
#if defined(_WIN32)
  mask = other.mask;
#elif defined(__linux__)
  cpu_set = new cpu_set_t;
  std::memcpy( cpu_set, other.cpu_set, sizeof( cpu_set_t ) );
#elif defined(__sun)
  if ( other.psetid == PS_NONE )
    psetid = PS_NONE;
  else
    YIELD::DebugBreak(); // Processors cannot belong to more than one processor set on Solaris, so it makes no sense to copy
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
  else
    return false;
#elif defined(__linux)
  return CPU_ISSET( processor_i, static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  return psetid != PS_NONE && pset_assign( PS_QUERY, processor_i, NULL ) == psetid;
#else
  return false;
#endif
}
void ProcessorSet::set( uint16_t processor_i )
{
#if defined(_WIN32)
  mask |= ( 1L << processor_i );
#elif defined(__linux)
  CPU_SET( processor_i, static_cast<cpu_set_t*>( cpu_set ) );
#elif defined(__sun)
  if ( psetid == PS_NONE ) pset_create( &psetid );
  pset_assign( psetid, processor_i, NULL );
#endif
}


// rrd.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
Stream::Status RRD::Record::deserialize( InputStream& input_stream )
{
  size_t bytes_read;
  uint64_t time_uint64;
  Stream::Status read_status = input_stream.read( &time_uint64, sizeof( time_uint64 ), &bytes_read );
  if ( read_status == Stream::STREAM_STATUS_OK )
  {
    time = time_uint64;
    return input_stream.read( &value, sizeof( value ), &bytes_read );
  }
  else
    return read_status;
}
Stream::Status RRD::Record::serialize( OutputStream& output_stream )
{
  uint64_t time_uint64 = time;
  Stream::Status write_status = output_stream.write( &time_uint64, sizeof( time_uint64 ) );
  if ( write_status == Stream::STREAM_STATUS_OK )
    return output_stream.write( &value, sizeof( value ) );
  else
    return write_status;
}
RRD::RRD( const Path& file_path, uint32_t file_flags )
  : current_file_path( file_path )
{
  current_file = new File( current_file_path, file_flags|O_CREAT|O_WRONLY|O_APPEND );
}
RRD::~RRD()
{
  Object::decRef( current_file );
}
void RRD::append( double value )
{
  Record( value ).serialize( *current_file );
}
void RRD::fetch( std::vector<Record>& out_records )
{
  File* current_file = new File( current_file_path );
  Record record;
  while ( record.deserialize( *current_file ) )
    out_records.push_back( record );
  Object::decRef( *current_file );
}
void RRD::fetch( const Time& start_time, std::vector<Record>& out_records )
{
  File* current_file = new File( current_file_path );
  Record record;
  while ( record.deserialize( *current_file ) )
  {
    if ( record.get_time() >= start_time )
      out_records.push_back( record );
  }
  Object::decRef( *current_file );
}
void RRD::fetch( const Time& start_time, const Time& end_time, std::vector<Record>& out_records )
{
  File* current_file = new File( current_file_path );
  Record record;
  while ( record.deserialize( *current_file ) )
  {
    if ( record.get_time() >= start_time && record.get_time() <= end_time )
      out_records.push_back( record );
  }
  Object::decRef( *current_file );
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
SharedLibrary* SharedLibrary::open( const Path& file_prefix, const char* argv0 )
{
  SharedLibrary* shared_library = new SharedLibrary;
  if ( shared_library->init( file_prefix, argv0 ) )
    return shared_library;
  else
  {
    delete shared_library;
    return NULL;
  }
}
SharedLibrary::SharedLibrary() : handle( NULL )
{ }
SharedLibrary::SharedLibrary( const Path& file_prefix, const char* argv0 ) : handle( NULL )
{
  if ( !init( file_prefix, argv0 ) )
    throw Exception();
}
SharedLibrary::~SharedLibrary()
{
  if ( handle )
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
bool SharedLibrary::init( const Path& file_prefix, const char* argv0 )
{
  char file_path[MAX_PATH];
  if ( ( handle = DLOPEN( file_prefix ) ) != NULL )
    return true;
  else
  {
    snprintf( file_path, MAX_PATH, "lib%c%s.%s", PATH_SEPARATOR, static_cast<const char*>( file_prefix ), SHLIBSUFFIX );
    if ( ( handle = DLOPEN( file_path ) ) != NULL )
      return true;
    else
    {
      snprintf( file_path, MAX_PATH, "%s.%s", static_cast<const char*>( file_prefix ), SHLIBSUFFIX );
      if ( ( handle = DLOPEN( file_path ) ) != NULL )
        return true;
      else
      {
        if ( argv0 != NULL )
        {
          const char* last_slash = strrchr( argv0, PATH_SEPARATOR );
          while ( last_slash != NULL && last_slash != argv0 )
          {
            snprintf( file_path, MAX_PATH, "%.*s%s.%s", static_cast<int>( last_slash - argv0 + 1 ), argv0, static_cast<const char*>( file_prefix ), SHLIBSUFFIX );
            if ( ( handle = DLOPEN( file_path ) ) != NULL )
              return true;
            else
            {
              snprintf( file_path, MAX_PATH, "%.*slib%c%s.%s", static_cast<int>( last_slash - argv0 + 1 ), argv0, PATH_SEPARATOR, static_cast<const char*>( file_prefix ), SHLIBSUFFIX );
              if ( ( handle = DLOPEN( file_path ) ) != NULL )
                return true;
            }
            last_slash--;
            while ( *last_slash != PATH_SEPARATOR ) last_slash--;
          }
        }
      }
    }
  }
  return false;
}
void* SharedLibrary::getFunction( const char* func_name )
{
  void* func_handle;
#ifdef _WIN32
  func_handle = GetProcAddress( ( HMODULE )handle, func_name );
#else
  func_handle = dlsym( handle, func_name );
#endif
  if ( func_handle )
    return func_handle;
  else
    return NULL;
//    throw Exception();
}


// stat.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#include <windows.h>
#endif
#ifdef _WIN32
Stat::Stat( mode_t mode, uint64_t size, const Time& atime, const Time& mtime, const Time& ctime, uint32_t attributes )
: mode( mode ), size( size ), atime( atime ), mtime( mtime ), ctime( ctime ), attributes( attributes )
{ }
Stat::Stat( const WIN32_FIND_DATA& find_data )
{
  init( find_data.nFileSizeHigh, find_data.nFileSizeLow, &find_data.ftLastWriteTime, &find_data.ftCreationTime, &find_data.ftLastAccessTime, find_data.dwFileAttributes );
}
Stat::Stat( uint32_t nFileSizeHigh, uint32_t nFileSizeLow, const FILETIME* ftLastWriteTime, const FILETIME* ftCreationTime, const FILETIME* ftLastAccessTime, uint32_t dwFileAttributes )
{
  init( nFileSizeHigh, nFileSizeLow, ftLastWriteTime, ftCreationTime, ftLastAccessTime, dwFileAttributes );
}
Stat::Stat( const Stat& other )
: mode( other.mode ), size( other.size ), atime( other.atime ), mtime( other.mtime ), ctime( other.ctime ), attributes( other.attributes )
{ }
#else
Stat::Stat( mode_t mode, nlink_t nlink, uid_t uid, gid_t gid, uint64_t size, const Time& atime, const Time& mtime, const Time& ctime )
: mode( mode ), nlink( nlink ), uid( uid ), gid( gid ), size( size ), atime( atime ), mtime( mtime ), ctime( ctime )
{ }
Stat::Stat( const Stat& other )
: mode( other.mode ), nlink( other.nlink ), uid( other.uid ), gid( other.gid ), size( other.size ), atime( other.atime ), mtime( other.mtime ), ctime( other.ctime )
{ }
#endif
void Stat::init( const Path& path )
{
  fd_t fd = File::_open( path, O_RDONLY, File::DEFAULT_MODE, File::DEFAULT_ATTRIBUTES );
  if ( fd != INVALID_HANDLE_VALUE )
  {
    try
    {
      init( fd );
      File::_close( fd );
    }
    catch ( std::exception& )
    {
      File::_close( fd );
      throw;
    }
  }
  else
    throw Exception();
}
void Stat::init( fd_t fd )
{
#ifdef _WIN32
  BY_HANDLE_FILE_INFORMATION temp_file_info;
  if ( GetFileInformationByHandle( fd, &temp_file_info ) != 0 )
  {
    mode = ( temp_file_info.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY ) ? S_IFDIR : S_IFREG;
    ULARGE_INTEGER size;
    size.LowPart = temp_file_info.nFileSizeLow;
    size.HighPart = temp_file_info.nFileSizeHigh;
    this->size = static_cast<size_t>( size.QuadPart );
    ctime = temp_file_info.ftCreationTime;
    atime = temp_file_info.ftLastAccessTime;
    mtime = temp_file_info.ftLastWriteTime;
    attributes = temp_file_info.dwFileAttributes;
  }
  else
    throw Exception();
#else
  struct stat temp_stat;
  if ( fstat( fd, &temp_stat ) != -1 )
  {
    mode = temp_stat.st_mode;
    size = temp_stat.st_size;
    ctime = static_cast<uint32_t>( temp_stat.st_ctime );
    atime = static_cast<uint32_t>( temp_stat.st_atime );
    mtime = static_cast<uint32_t>( temp_stat.st_mtime );
    nlink = temp_stat.st_nlink;
  }
  else
    throw Exception();
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
#ifdef _WIN32
  stbuf.st_mode = static_cast<unsigned short>( mode );
  stbuf.st_size = static_cast<off_t>( size );
#else
  stbuf.st_mode = mode;
  stbuf.st_size = static_cast<size_t>( size );
#endif
  stbuf.st_atime = atime.as_unix_time_s();
  stbuf.st_mtime = mtime.as_unix_time_s();
  stbuf.st_ctime = ctime.as_unix_time_s();
#ifndef _WIN32
  stbuf.st_nlink = nlink;
  stbuf.st_uid = uid;
  stbuf.st_gid = gid;
#endif
  return stbuf;
}
#ifdef _WIN32
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
#endif


// test_runner.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#include <iostream>
int TestRunner::run( TestSuite& test_suite )
{
  int ret_code = 0;
  for ( TestSuite::iterator i = test_suite.begin(); i != test_suite.end(); i++ )
  {
    bool called_runTest = false, called_tearDown = false;
    try
    {
      std::cerr << ( *i )->shortDescription();
      ( *i )->setUp();
      called_runTest = true;
      ( *i )->runTest();
      called_tearDown = true;
      ( *i )->tearDown();
      std::cerr << ": passed" << std::endl;
      continue;
    }
    catch ( YIELD::AssertionException& exc )
    {
      std::cerr << " failed: " << exc.what() << std::endl;
    }
    catch ( std::exception& exc )
    {
      std::cerr << " threw unknown exception: " << exc.what() << std::endl;
    }
    catch ( ... )
    {
      std::cerr << " threw unknown non-exception" << std::endl;
    }
    if ( called_runTest && !called_tearDown )
      try { ( *i )->tearDown(); } catch ( ... ) { }
    ret_code |= 1;
  }
  return ret_code;
}


// test_suite.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
TestSuite::~TestSuite()
{
  for ( std::vector<TestCase*>::size_type test_case_i = 0; test_case_i < size(); test_case_i++ )
  {
    if ( own_test_cases[test_case_i] )
      delete at( test_case_i );
  }
}
void TestSuite::addTest( TestCase* test_case, bool own_test_case )
{
  if ( test_case )
  {
    push_back( test_case );
    own_test_cases.push_back( own_test_case );
  }
}
void TestSuite::addTest( TestCase& test_case, bool own_test_case )
{
  push_back( &test_case );
  own_test_cases.push_back( own_test_case );
}


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

void Thread::sleep( timeout_ns_t timeout_ns )
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
  _is_running = false;
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
    return processor_bind( P_LWPID, thr_self(), logical_processor_i, NULL ) == 0;
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
  if ( !this->_is_running )
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
}

#ifdef _WIN32
unsigned long __stdcall Thread::thread_stub( void* pnt )
#else
void* Thread::thread_stub( void* pnt )
#endif
{
  Thread* this_thread = static_cast<Thread*>( pnt );
  if ( !this_thread->_is_running )
  {
    this_thread->_is_running = true;
#if defined(__linux__)
    this_thread->id = syscall( SYS_gettid );
#elif defined(__MACH__)
    this_thread->id = 0; // ???
#elif defined(__sun)
    this_thread->id = thr_self();
#endif
    this_thread->run();
    this_thread->_is_running = false;
    return 0;
  }
  else
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


// volume.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#include <windows.h>
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
    // Volume::readdirCallback
    bool operator()( const Path& dirent_name, const Stat& stbuf )
    {
      return listdir_callback( dirent_name );
    }
  private:
    Volume::listdirCallback& listdir_callback;
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
bool Volume::chown( const YIELD::Path& path, int32_t uid, int32_t gid )
{
#ifdef _WIN32
  ::SetLastError( ERROR_NOT_SUPPORTED );
  return false;
#else
  return ::chown( path, uid, gid ) != -1;
#endif
}
bool Volume::exists( const Path& path )
{
#ifdef _WIN32
  return GetFileAttributesW( path ) != INVALID_FILE_ATTRIBUTES;
#else
  struct stat temp_stat;
  return ::stat( path, &temp_stat ) == 0;
#endif
}
YIELD::auto_Object<YIELD::Stat> Volume::getattr( const Path& path )
{
  return new Stat( path );
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
auto_Object<File> Volume::open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
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
        if ( !callback( find_data.cFileName, Stat( find_data ) ) )
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
          if ( !callback( next_dirent->d_name, Stat( path + next_dirent->d_name ) ) )
          {
            closedir( dir_handle );
            return false;
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
auto_Object<Path> Volume::readlink( const Path& path )
{
#ifdef _WIN32
  ::SetLastError( ERROR_NOT_SUPPORTED );
  return NULL;
#else
  char out_path[MAX_PATH];
  ssize_t out_path_len = ::readlink( path, out_path, MAX_PATH );
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
    virtual bool operator()( const Path& path, const Stat& stbuf )
    {
      if ( stbuf.ISDIR() )
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
  Stat path_stat( path );
  if ( path_stat.ISDIR() )
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
      buffer->f_namemax = MAX_PATH;
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
  auto_Object<File> file = creat( path, mode );
  return file != NULL;
}
bool Volume::truncate( const Path& path, uint64_t new_size )
{
#ifdef _WIN32
  auto_Object<File> file = Volume::open( path, O_CREAT|O_WRONLY, DEFAULT_FILE_MODE );
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
  auto_Object<File> file = open( path, O_WRONLY );
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
    wchar_t volume_name[MAX_PATH], file_system_name[MAX_PATH];
    if ( GetVolumeInformation( root_dir_path, volume_name, MAX_PATH, NULL, NULL, NULL, file_system_name, MAX_PATH ) != 0 )
    {
      if ( wcslen( volume_name ) > 0 )
        return Path( volume_name );
      else
        return static_cast<const std::string&>( path_parts[0] );
    }
  }
#endif
  return Path();
}


// xdr_input_stream.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#define XDR_ARRAY_LENGTH_MAX 16 * 1024
// #define XDR_STRING_LENGTH_MAX 32 * 1024
XDRInputStream::XDRInputStream( InputStream& underlying_input_stream )
  : underlying_input_stream( underlying_input_stream )
{ }
Object* XDRInputStream::readObject( const Declaration& decl, Object* value, Object::GeneralType value_general_type )
{
  if ( value )
  {
    if ( value_general_type == Object::UNKNOWN )
      value_general_type = value->get_general_type();
    switch ( value_general_type )
    {
      case Object::SEQUENCE:
      {
        size_t size = readInt32( decl );
        if ( size <= XDR_ARRAY_LENGTH_MAX )
        {
          for ( size_t i = 0; i < size; i++ )
            value->deserialize( *this );
        }
        else
          throw Exception( "read array length beyond maximum" );
      }
      break;
      case Object::MAP:
      {
        size_t size = readInt32( decl );
        for ( size_t i = 0; i < size; i++ )
          value->deserialize( *this );
      }
      break;
      default:
      {
        beforeRead( decl );
        value->deserialize( *this );
      }
      break;
    }
  }
  else // Reading an arbitrary Object, assume it's a string
  {
    value = new YIELD::String();
    try
    {
      readString( decl, static_cast<String&>( *value ) );
    }
    catch ( ... )
    {
      Object::decRef( value );
      throw;
    }
  }
  return value;
}
void XDRInputStream::readString( const Declaration& decl, std::string& str )
{
  size_t str_len = readInt32( decl );
//  if ( str_len < XDR_STRING_LENGTH_MAX ) // Sanity check
//  {
// Bypassing the sanity check, since the XDR_STRING_LENGTH of 32K is too small for file system reads
    if ( str_len != 0 )
    {
      size_t padded_str_len = str_len % 4;
      if ( padded_str_len == 0 )
        padded_str_len = str_len;
      else
        padded_str_len = str_len + 4 - padded_str_len;
      str.resize( padded_str_len );
      underlying_input_stream.read( const_cast<char*>( str.c_str() ), padded_str_len );
      str.resize( str_len );
    }
//  }
}
int32_t XDRInputStream::_readInt32()
{
  int32_t value;
  underlying_input_stream.read( &value, sizeof( value ) );
#ifdef __MACH__
  return ntohl( value );
#else
  return Machine::ntohl( value );
#endif
}
int64_t XDRInputStream::_readInt64()
{
  int64_t value;
  underlying_input_stream.read( &value, sizeof( value ) );
  return Machine::ntohll( value );
}


// xdr_output_stream.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
XDROutputStream::XDROutputStream( OutputStream& underlying_output_stream, bool in_map )
  : underlying_output_stream( underlying_output_stream ), in_map( in_map )
{ }
void XDROutputStream::beforeWrite( const Declaration& decl )
{
  if ( in_map && decl.identifier )
    writeString( Declaration(), decl.identifier );
}
void XDROutputStream::writeObject( const Declaration& decl, Object& value, Object::GeneralType value_type )
{
  if ( value_type == Object::UNKNOWN )
    value_type = value.get_general_type();
  switch ( value_type )
  {
    case Object::MAP:
    {
      writeInt32( decl, static_cast<int32_t>( value.get_size() ) );
      XDROutputStream child_xdr_underlying_output_stream( underlying_output_stream, true );
      value.serialize( child_xdr_underlying_output_stream );
    }
    break;
    case Object::SEQUENCE:
    {
      writeInt32( decl, static_cast<int32_t>( value.get_size() ) );
      value.serialize( *this );
    }
    break;
    case Object::STRING:
    {
      writeString( decl, static_cast<String&>( value ) );
    }
    break;
    default:
    {
      beforeWrite( decl );
      value.serialize( *this );
    }
    break;
  }
}
void XDROutputStream::writeString( const Declaration& decl, const char* value, size_t value_len )
{
  beforeWrite( decl );
  _writeInt32( static_cast<int32_t>( value_len ) );
  underlying_output_stream.write( value, value_len );
  if ( value_len % 4 != 0 )
  {
    static char zeros[] = { 0, 0, 0 };
    underlying_output_stream.write( zeros, 4 - ( value_len % 4 ) );
  }
}
void XDROutputStream::_writeInt32( int32_t value )
{
#ifdef __MACH__
  value = htonl( value );
#else
  value = Machine::htonl( value );
#endif
  underlying_output_stream.write( &value, 4 );
}
void XDROutputStream::_writeInt64( int64_t value )
{
  value = Machine::htonll( value );
  underlying_output_stream.write( &value, 8 );
}

