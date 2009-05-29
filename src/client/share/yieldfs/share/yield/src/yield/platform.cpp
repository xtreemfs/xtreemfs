// Revision: 1496

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
  struct timespec timeout_ts = Time( timeout_ns );
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
auto_Object<File> File::open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes )
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
#ifdef _WIN32
  if ( fd != INVALID_HANDLE_VALUE && CloseHandle( fd ) != 0 )
  {
    fd = INVALID_HANDLE_VALUE;
#else
  if ( ::close( fd ) >= 0 )
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
#ifdef _WIN32
#pragma warning( pop )
#endif


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
    ostreamWrapper& operator=( const ostreamWrapper& ) { return *this; }
    // Object
    YIELD_OBJECT_PROTOTYPES( ostreamWrapper, 0 );
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
auto_Object<Log> Log::open( const Path& file_path, Level level )
{
  auto_Object<File> file = File::open( file_path, O_CREAT|O_WRONLY|O_APPEND );
  if ( file != NULL )
    return new Log( std::auto_ptr<OutputStream>( file.release() ), level );
  else
    return NULL;
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
OutputStream::Status Log::writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
{
  return underlying_output_stream->writev( buffers, buffers_count, out_bytes_written );
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
auto_Object<MemoryMappedFile> MemoryMappedFile::open( const Path& path, uint32_t flags, mode_t mode, uint32_t attributes, size_t minimum_size )
{
  auto_Object<File> file = File::open( path, flags, mode, attributes );
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
      auto_Object<Stat> stbuf = file->stat();
      if ( stbuf != NULL )
        current_file_size = stbuf->get_size();
      else
        current_file_size = 0;
#endif
    }
    else
      current_file_size = 0;
    auto_Object<MemoryMappedFile> memory_mapped_file = new MemoryMappedFile( file, flags );
    if ( memory_mapped_file->resize( std::max( minimum_size, current_file_size ) ) )
      return memory_mapped_file;
    else
      return NULL;
  }
  else
    return NULL;
}
MemoryMappedFile::MemoryMappedFile( auto_Object<File> underlying_file, uint32_t open_flags )
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
    if ( size == new_size || underlying_file->truncate( new_size ) )
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
  struct timespec timeout_ts = Time( timeout_ns );
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


// named_pipe.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#include <windows.h>
#pragma warning( push )
#pragma warning( disable: 4100 )
#endif
auto_Object<NamedPipe> NamedPipe::open( const Path& path, uint32_t flags, mode_t mode )
{
#ifdef _WIN32
  Path named_pipe_base_dir_path( TEXT( "\\\\.\\pipe" ) );
  Path named_pipe_path( named_pipe_base_dir_path + path );
  if ( ( flags & O_CREAT ) == O_CREAT ) // Server
  {
    HANDLE hPipe = CreateNamedPipe( named_pipe_path, PIPE_ACCESS_DUPLEX, PIPE_TYPE_BYTE|PIPE_READMODE_BYTE|PIPE_WAIT, PIPE_UNLIMITED_INSTANCES, 4096, 4096, 0, NULL );
    if ( hPipe != INVALID_HANDLE_VALUE )
      return new NamedPipe( new File( hPipe ), false );
  }
  else // Client
  {
    auto_Object<File> underlying_file = File::open( named_pipe_path, flags );
    if ( underlying_file != NULL )
      return new NamedPipe( underlying_file, true );
  }
#else
  if ( ( flags & O_CREAT ) == O_CREAT )
  {
    if ( ::mkfifo( path, mode ) != -1 )
      flags ^= O_CREAT;
    else
      return NULL;
  }
  auto_Object<File> underlying_file = File::open( path, flags );
  if ( underlying_file != NULL )
    return new NamedPipe( underlying_file );
#endif
  return NULL;
}
#ifdef _WIN32
NamedPipe::NamedPipe( auto_Object<File> underlying_file, bool connected )
  : underlying_file( underlying_file ), connected( connected )
{ }
#else
NamedPipe::NamedPipe( auto_Object<File> underlying_file )
  : underlying_file( underlying_file )
{ }
#endif
Stream::Status NamedPipe::read( void* buffer, size_t buffer_len, size_t* out_bytes_read  )
{
#ifdef _WIN32
  if ( !connected )
  {
    if ( ConnectNamedPipe( *underlying_file, NULL ) != 0 ||
         GetLastError() == ERROR_PIPE_CONNECTED )
      connected = true;
    else
      return STREAM_STATUS_ERROR;
  }
#endif
  return underlying_file->read( buffer, buffer_len, out_bytes_read );
}
Stream::Status NamedPipe::writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
{
#ifdef _WIN32
  if ( !connected )
  {
    if ( ConnectNamedPipe( *underlying_file, NULL ) )
      connected = true;
    else
      return STREAM_STATUS_ERROR;
  }
#endif
  return underlying_file->writev( buffers, buffers_count, out_bytes_written );
}
#ifdef _WIN32
#pragma warning( pop )
#endif


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


// pipe.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).


#ifdef _WIN32
#include <windows.h>
#endif


auto_Object<Pipe> Pipe::create()
{
#ifdef _WIN32
  SECURITY_ATTRIBUTES pipe_security_attributes;
  pipe_security_attributes.nLength = sizeof( SECURITY_ATTRIBUTES );
  pipe_security_attributes.bInheritHandle = TRUE;
  pipe_security_attributes.lpSecurityDescriptor = NULL;
  void* ends[2];
  if ( CreatePipe( &ends[0], &ends[1], &pipe_security_attributes, 0 ) )
  {
    if ( SetHandleInformation( ends[0], HANDLE_FLAG_INHERIT, 0 ) &&
         SetHandleInformation( ends[1], HANDLE_FLAG_INHERIT, 0 ) )
      return new Pipe( ends );
    else
    {
      CloseHandle( ends[0] );
      CloseHandle( ends[1] );
    }
  }
#else
  int ends[2];
  if ( ::pipe( ends ) != -1 )
    return new Pipe( ends );
#endif
  return NULL;
}

#ifdef _WIN32
Pipe::Pipe( void* ends[2] )
#else
Pipe::Pipe( int ends[2] )
#endif
{
  memcpy_s( this->ends, sizeof( this->ends ), ends, sizeof( this->ends ) );
}

Pipe::~Pipe()
{
#ifdef _WIN32
  CloseHandle( ends[0] );
  CloseHandle( ends[1] );
#else
  ::close( ends[0] );
  ::close( ends[1] );
#endif
}

Stream::Status Pipe::read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
{
#ifdef _WIN32
  DWORD dwBytesRead;
  if ( ReadFile( ends[0], buffer, static_cast<DWORD>( buffer_len ), &dwBytesRead, NULL ) )
  {
    if ( out_bytes_read )
      *out_bytes_read = dwBytesRead;
    return STREAM_STATUS_OK;
  }
  else
    return STREAM_STATUS_ERROR;
#else
  ssize_t read_ret = ::read( ends[0], buffer, buffer_len );
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

Stream::Status Pipe::writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
{
#ifdef _WIN32
  if ( buffers_count == 1 )
  {
    DWORD dwBytesWritten;
    if ( WriteFile( ends[1], buffers[0].iov_base, static_cast<DWORD>( buffers[0].iov_len ), &dwBytesWritten, NULL ) )
    {
      if ( out_bytes_written )
        *out_bytes_written = dwBytesWritten;
      return STREAM_STATUS_OK;
    }
    else
      return STREAM_STATUS_ERROR;
  }
  else
  {
    ::SetLastError( ERROR_NOT_SUPPORTED );
    return STREAM_STATUS_ERROR;
  }
#else
  ssize_t writev_ret = ::writev( ends[1], buffers, buffers_count );
  if ( writev_ret >= 0 )
  {
    if ( out_bytes_written )
      *out_bytes_written = static_cast<size_t>( writev_ret );
    return STREAM_STATUS_OK;
  }
  else
    return STREAM_STATUS_ERROR;
#endif
}


// pretty_print_output_stream.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
PrettyPrintOutputStream::PrettyPrintOutputStream( OutputStream& underlying_output_stream )
  : underlying_output_stream( underlying_output_stream )
{ }
void PrettyPrintOutputStream::writeBool( const Declaration&, bool value )
{
  underlying_output_stream.write( ( ( value ) ? "true, " : "false, " ) );
}
void PrettyPrintOutputStream::writeDouble( const Declaration&, double value )
{
  std::ostringstream value_oss; value_oss << value << ", ";
  underlying_output_stream.write( value_oss.str() );
}
void PrettyPrintOutputStream::writeInt64( const Declaration&, int64_t value )
{
  std::ostringstream value_oss; value_oss << value << ", ";
  underlying_output_stream.write( value_oss.str() );
}
void PrettyPrintOutputStream::writeMap( const Declaration&, Object& value )
{
  underlying_output_stream.write( value.get_type_name() );
  underlying_output_stream.write( "( " );
  value.serialize( *this );
  underlying_output_stream.write( " ), " );
}
void PrettyPrintOutputStream::writeSequence( const Declaration&, Object& value )
{
  underlying_output_stream.write( "[ " );
  value.serialize( *this );
  underlying_output_stream.write( " ], " );
}
void PrettyPrintOutputStream::writeString( const Declaration&, const char* value, size_t value_len )
{
  underlying_output_stream.write( value, value_len );
  underlying_output_stream.write( ", " );
}
void PrettyPrintOutputStream::writeStruct( const Declaration&, Object& value )
{
  underlying_output_stream.write( value.get_type_name() );
  underlying_output_stream.write( "( " );
  value.serialize( *this );
  underlying_output_stream.write( " ), " );
}


// process.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if defined(_WIN32)
#include <windows.h>
#else
#include <sys/wait.h> // For waitpid
#endif
auto_Object<Process> Process::create( const Path& executable_file_path )
{
  const char* argv[] = { static_cast<const char*>( NULL ) };
  return create( executable_file_path, argv );
}
auto_Object<Process> Process::create( int argc, char** argv )
{
  std::vector<char*> argvv;
  for ( int arg_i = 1; arg_i < argc; arg_i++ )
    argvv.push_back( argv[arg_i] );
  argvv.push_back( NULL );
  return create( argv[0], const_cast<const char**>( &argvv[0] ) );
}
auto_Object<Process> Process::create( const Path& executable_file_path, const char** null_terminated_argv )
{
#ifdef _WIN32
  const std::string& executable_file_path_str = static_cast<const std::string&>( executable_file_path );
  std::string catted_args;
  if ( executable_file_path_str.find( ' ' ) == -1 )
    catted_args.append( executable_file_path_str );
  else
  {
    catted_args.append( "\"", 1 );
    catted_args.append( executable_file_path_str );
    catted_args.append( "\"", 1 );
  }
  size_t arg_i = 0;
  while ( null_terminated_argv[arg_i] != NULL )
  {
    catted_args.append( " ", 1 );
    catted_args.append( null_terminated_argv[arg_i] );
    arg_i++;
  }
  return create( executable_file_path, catted_args.c_str() );
#else
  auto_Object<Pipe> child_stdin_pipe = Pipe::create(),
                    child_stdout_pipe = Pipe::create(),
                    child_stderr_pipe = Pipe::create();
  pid_t child_pid = fork();
  if ( child_pid == -1 )
    return NULL;
  else if ( child_pid == 0 ) // Child
  {
    close( STDIN_FILENO );
    dup2( child_stdin_pipe->get_read_end(), STDIN_FILENO ); // Set stdin to read end of stdin pipe
    close( STDOUT_FILENO );
    dup2( child_stdout_pipe->get_write_end(), STDOUT_FILENO ); // Set stdout to write end of stdout pipe
    close( STDERR_FILENO );
    dup2( child_stderr_pipe->get_write_end(), STDERR_FILENO ); // Set stderr to write end of stderr pipe
    std::vector<char*> argv_with_executable_file_path;
    argv_with_executable_file_path.push_back( const_cast<char*>( static_cast<const char*>( executable_file_path ) ) );
    size_t arg_i = 0;
    while ( null_terminated_argv[arg_i] != NULL )
    {
      argv_with_executable_file_path.push_back( const_cast<char*>( null_terminated_argv[arg_i] ) );
      arg_i++;
    }
    argv_with_executable_file_path.push_back( NULL );
    execv( executable_file_path, &argv_with_executable_file_path[0] );
    return NULL;
  }
  else // Parent
    return new Process( child_pid, child_stdin_pipe, child_stdout_pipe, child_stderr_pipe );
#endif
}
#ifdef _WIN32
auto_Object<Process> Process::create( const Path& executable_file_path, const char* catted_args )
{
  auto_Object<Pipe> child_stdin_pipe = Pipe::create(),
                    child_stdout_pipe = Pipe::create(),
                    child_stderr_pipe = Pipe::create();
  STARTUPINFO startup_info;
  ZeroMemory( &startup_info, sizeof( STARTUPINFO ) );
  startup_info.cb = sizeof( STARTUPINFO );
  startup_info.hStdInput = child_stdin_pipe->get_read_end();
  startup_info.hStdOutput = child_stdout_pipe->get_write_end();
  startup_info.hStdError = child_stdout_pipe->get_write_end();
  startup_info.dwFlags = STARTF_USESTDHANDLES;
  PROCESS_INFORMATION proc_info;
  ZeroMemory( &proc_info, sizeof( PROCESS_INFORMATION ) );
  if ( CreateProcess( executable_file_path, ( LPWSTR )catted_args, NULL, NULL, TRUE, CREATE_NO_WINDOW, NULL, NULL, &startup_info, &proc_info ) )
    return new Process( proc_info.hProcess, proc_info.hThread, child_stdin_pipe, child_stdout_pipe, child_stderr_pipe );
  else
    return NULL;
}
Process::Process( HANDLE hChildProcess, HANDLE hChildThread, auto_Object<Pipe> child_stdin_pipe, auto_Object<Pipe> child_stdout_pipe, auto_Object<Pipe> child_stderr_pipe )
  : hChildProcess( hChildProcess ), hChildThread( hChildThread ),
    child_stdin_pipe( child_stdin_pipe ), child_stdout_pipe( child_stdout_pipe ), child_stderr_pipe( child_stderr_pipe )
{ }
#else
Process::Process( pid_t child_pid, auto_Object<Pipe> child_stdin_pipe, auto_Object<Pipe> child_stdout_pipe, auto_Object<Pipe> child_stderr_pipe )
  : child_pid( child_pid ),
    child_stdin_pipe( child_stdin_pipe ), child_stdout_pipe( child_stdout_pipe ), child_stderr_pipe( child_stderr_pipe )
{ }
#endif
Process::~Process()
{
#ifdef _WIN32
  TerminateProcess( hChildProcess, 0 );
  WaitForSingleObject( hChildProcess, INFINITE );
  CloseHandle( hChildProcess );
  CloseHandle( hChildThread );
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
Stream::Status Process::read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
{
  return child_stdout_pipe->read( buffer, buffer_len, out_bytes_read );
}
Stream::Status Process::read_stderr( void* buffer, size_t buffer_len, size_t* out_bytes_read )
{
  return child_stderr_pipe->read( buffer, buffer_len, out_bytes_read );
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
    return -1;
#endif
}
Stream::Status Process::writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
{
  return child_stdin_pipe->writev( buffers, buffers_count, out_bytes_written );
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
auto_Object<RRD> RRD::open( const Path& file_path, uint32_t file_open_flags )
{
  auto_Object<File> current_file = File::open( file_path, file_open_flags|O_CREAT|O_WRONLY|O_APPEND );
  if ( current_file != NULL )
    return new RRD( file_path, current_file );
  else
    return NULL;
}
RRD::RRD( const Path& current_file_path, auto_Object<File> current_file )
  : current_file_path( current_file_path ), current_file( current_file )
{ }
void RRD::append( double value )
{
  Record( value ).serialize( *current_file );
}
void RRD::fetch( std::vector<Record>& out_records )
{
  auto_Object<File> current_file = File::open( current_file_path );
  if ( current_file != NULL )
  {
    Record record;
    while ( record.deserialize( *current_file ) )
      out_records.push_back( record );
  }
}
void RRD::fetch( const Time& start_time, std::vector<Record>& out_records )
{
  auto_Object<File> current_file = File::open( current_file_path );
  if ( current_file != NULL )
  {
    Record record;
    while ( record.deserialize( *current_file ) )
    {
      if ( record.get_time() >= start_time )
        out_records.push_back( record );
    }
  }
}
void RRD::fetch( const Time& start_time, const Time& end_time, std::vector<Record>& out_records )
{
  auto_Object<File> current_file = File::open( current_file_path );
  if ( current_file != NULL )
  {
    Record record;
    while ( record.deserialize( *current_file ) )
    {
      if ( record.get_time() >= start_time && record.get_time() <= end_time )
        out_records.push_back( record );
    }
  }
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
auto_Object<SharedLibrary> SharedLibrary::open( const Path& file_prefix, const char* argv0 )
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
auto_Object<Stat> Stat::stat( const Path& path )
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
Stat::Stat( mode_t mode, nlink_t nlink, uid_t tag, gid_t gid, uint64_t size, const Time& atime, const Time& mtime, const Time& ctime )
: mode( mode ), nlink( nlink ), tag( tag ), gid( gid ), size( size ), atime( atime ), mtime( mtime ), ctime( ctime )
{ }
#endif
Stat::Stat( const struct stat& stbuf )
{
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
  stbuf.st_uid = tag;
  stbuf.st_gid = gid;
#endif
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


// string.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
Stream::Status String::deserialize( InputStream& input_stream, size_t* out_bytes_read )
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
Stream::Status String::read( void* buffer, size_t buffer_len, size_t* out_bytes_read )
{
  size_t readable_len = size() - read_pos;
  if ( readable_len > 0 )
  {
    if ( buffer_len > readable_len )
      buffer_len = readable_len;
    memcpy_s( buffer, buffer_len, c_str()+read_pos, buffer_len );
    read_pos += buffer_len;
    if ( out_bytes_read )
      *out_bytes_read = buffer_len;
    return STREAM_STATUS_OK;
  }
  else
    return STREAM_STATUS_ERROR;
}
Stream::Status String::serialize( OutputStream& output_stream, size_t* out_bytes_written )
{
  return output_stream.write( c_str(), size(), out_bytes_written );
}
Stream::Status String::writev( const struct iovec* buffers, uint32_t buffers_count, size_t* out_bytes_written )
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


// test_case.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
TestCase::TestCase( TestSuite& test_suite, const std::string& name )
  : short_description( test_suite.get_name() + "_" + name )
{
  test_suite.addTest( *this );
}
void TestCase::run( TestResult& )
{
  runTest();
}


// test_runner.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#include <iostream>
TestRunner::TestRunner( Log::Level log_level )
  : log_level( log_level )
{ }
int TestRunner::run( TestSuite& test_suite )
{
  TestResult* test_result = new TestResult( new Log( std::cout, log_level ) );
  test_suite.run( *test_result );
  return 0;
}


// test_suite.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
TestSuite::TestSuite( const std::string& name )
  : name( name )
{ }
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
void TestSuite::run( TestResult& test_result )
{
  for ( iterator test_i = begin(); test_i != end(); test_i++ )
  {
    Log::Stream test_result_log_stream = test_result.get_log()->getStream();
    bool called_runTest = false, called_tearDown = false;
    try
    {
      test_result_log_stream << ( *test_i )->shortDescription();
      ( *test_i )->setUp();
      called_runTest = true;
      ( *test_i )->run( test_result );
      called_tearDown = true;
      ( *test_i )->tearDown();
      test_result_log_stream << ": passed";
      continue;
    }
    catch ( YIELD::AssertionException& exc )
    {
      test_result_log_stream << " failed: " << exc.what();
    }
    catch ( std::exception& exc )
    {
      test_result_log_stream << " threw unknown exception: " << exc.what();
    }
    catch ( ... )
    {
      test_result_log_stream << " threw unknown non-exception";
    }
    if ( called_runTest && !called_tearDown )
      try { ( *test_i )->tearDown(); } catch ( ... ) { }
   // ret_code |= 1;
  }
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
    bool operator()( const Path& dirent_name, auto_Object<Stat> stbuf )
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
  struct stat temp_stat;
  return ::stat( path, &temp_stat ) == 0;
#endif
}
YIELD::auto_Object<YIELD::Stat> Volume::getattr( const Path& path )
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
          auto_Object<Stat> stbuf = stat( path + next_dirent->d_name );
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
auto_Object<Path> Volume::readlink( const Path& path )
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
    virtual bool operator()( const Path& path, auto_Object<Stat> stbuf )
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
  auto_Object<Stat> path_stat = Stat::stat( path );
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
  auto_Object<File> file = creat( path, mode );
  return file != NULL;
}
bool Volume::truncate( const Path& path, uint64_t new_size )
{
#ifdef _WIN32
  auto_Object<File> file = Volume::open( path, O_CREAT|O_WRONLY, File::DEFAULT_MODE );
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


// xdr_input_stream.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#define XDR_ARRAY_LENGTH_MAX 16 * 1024
// #define XDR_STRING_LENGTH_MAX 32 * 1024
XDRInputStream::XDRInputStream( InputStream& underlying_input_stream )
  : underlying_input_stream( underlying_input_stream )
{ }
bool XDRInputStream::readBool( const Declaration& decl )
{
  return readInt32( decl ) == 1;
}
double XDRInputStream::readDouble( const Declaration& )
{
  double value;
  underlying_input_stream.read( &value, sizeof( value ) );
  return value;
}
float XDRInputStream::readFloat( const Declaration& )
{
  float value;
  underlying_input_stream.read( &value, sizeof( value ) );
  return value;
}
int32_t XDRInputStream::readInt32( const Declaration& )
{
  int32_t value;
  underlying_input_stream.read( &value, sizeof( value ) );
#ifdef __MACH__
  return ntohl( value );
#else
  return Machine::ntohl( value );
#endif
}
int64_t XDRInputStream::readInt64( const Declaration& )
{
  int64_t value;
  underlying_input_stream.read( &value, sizeof( value ) );
  return Machine::ntohll( value );
}
Object* XDRInputStream::readMap( const Declaration& decl, Object* value )
{
  if ( value )
  {
    size_t size = readInt32( decl );
    for ( size_t i = 0; i < size; i++ )
      value->deserialize( *this );
  }
  return value;
}
Object* XDRInputStream::readSequence( const Declaration& decl, Object* value )
{
  if ( value )
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
Object* XDRInputStream::readStruct( const Declaration&, Object* value )
{
  if ( value )
    value->deserialize( *this );
  return value;
}


// xdr_output_stream.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
XDROutputStream::XDROutputStream( OutputStream& underlying_output_stream, bool in_map )
  : underlying_output_stream( underlying_output_stream ), in_map( in_map )
{ }
void XDROutputStream::beforeWrite( const Declaration& decl )
{
  if ( in_map && decl.get_identifier() )
    StructuredOutputStream::writeString( Declaration(), decl.get_identifier() );
}
void XDROutputStream::writeBool( const Declaration& decl, bool value )
{
  writeInt32( decl, value ? 1 : 0 );
}
void XDROutputStream::writeDouble( const Declaration& decl, double value )
{
  beforeWrite( decl );
  underlying_output_stream.write( &value, sizeof( value ) );
}
void XDROutputStream::writeFloat( const Declaration& decl, float value )
{
  beforeWrite( decl );
  underlying_output_stream.write( &value, sizeof( value ) );
}
void XDROutputStream::writeInt32( const Declaration& decl, int32_t value )
{
  beforeWrite( decl );
#ifdef __MACH__
  value = htonl( value );
#else
  value = Machine::htonl( value );
#endif
  underlying_output_stream.write( &value, 4 );
}
void XDROutputStream::writeInt64( const Declaration& decl, int64_t value )
{
  beforeWrite( decl );
  value = Machine::htonll( value );
  underlying_output_stream.write( &value, 8 );
}
void XDROutputStream::writeMap( const Declaration& decl, Object& value )
{
  writeInt32( decl, static_cast<int32_t>( value.get_size() ) );
  XDROutputStream child_xdr_underlying_output_stream( underlying_output_stream, true );
  value.serialize( child_xdr_underlying_output_stream );
}
void XDROutputStream::writeString( const Declaration& decl, const char* value, size_t value_len )
{
  writeInt32( decl, value_len );
  underlying_output_stream.write( value, value_len );
  if ( value_len % 4 != 0 )
  {
    static char zeros[] = { 0, 0, 0 };
    underlying_output_stream.write( zeros, 4 - ( value_len % 4 ) );
  }
}
void XDROutputStream::writeSequence( const Declaration& decl, Object& value )
{
  writeInt32( decl, static_cast<int32_t>( value.get_size() ) );
  value.serialize( *this );
}
void XDROutputStream::writeStruct( const Declaration& decl, Object& value )
{
  beforeWrite( decl );
  value.serialize( *this );
}

