// Revision: 1568

#include "yield/base.h"
using namespace YIELD;


// buffer.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
void Buffer::as_iovecs( std::vector<struct iovec>& out_iovecs ) const
{
  if ( next_buffer != NULL )
    next_buffer->as_iovecs( out_iovecs );
}
bool Buffer::operator==( const Buffer& other ) const
{
  if ( size() == other.size() )
  {
    void* this_base = static_cast<void*>( *this );
    void* other_base = static_cast<void*>( other );
    if ( this_base != NULL && other_base != NULL )
      return memcmp( this_base, other_base, size() ) == 0;
    else
      return false;
  }
  else
    return false;
}
void Buffer::set_next_buffer( auto_Buffer next_buffer )
{
//  if ( this->next_buffer != NULL ) DebugBreak();
  this->next_buffer = next_buffer;
}


// buffered_marshaller.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
void BufferedMarshaller::write( const void* buffer, size_t buffer_len )
{
  if ( current_buffer == NULL )
    current_buffer = first_buffer = new HeapBuffer( buffer_len >= 16 ? buffer_len : 16 );
  for ( ;; )
  {
    size_t put_len = current_buffer->put( buffer, buffer_len );
    if ( put_len == buffer_len )
      return;
    else
    {
      buffer_len -= put_len;
      buffer = static_cast<const uint8_t*>( buffer ) + put_len;
      YIELD::auto_Buffer next_buffer = new HeapBuffer( current_buffer->capacity() * 2 );
      current_buffer->set_next_buffer( next_buffer );
      current_buffer = next_buffer;
    }
  }
}
void BufferedMarshaller::write( YIELD::auto_Buffer buffer )
{
  current_buffer->set_next_buffer( buffer );
  current_buffer = new HeapBuffer( current_buffer->capacity() );
  while ( buffer->get_next_buffer() != NULL )
    buffer = buffer->get_next_buffer();
  buffer->set_next_buffer( current_buffer );
}


// buffered_unmarshaller.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
namespace YIELD
{
  class PartialBuffer : public FixedBuffer
  {
  public:
    PartialBuffer( YIELD::auto_Buffer underlying_buffer, size_t size )
      : FixedBuffer( size ), underlying_buffer( underlying_buffer )
    {
      iov.iov_base = *underlying_buffer;
      iov.iov_len = size;
    }
  private:
    YIELD::auto_Buffer underlying_buffer;
  };
};
YIELD::auto_Buffer BufferedUnmarshaller::readBuffer( size_t size )
{
  size_t source_buffer_size = source_buffer->size();
  if ( source_buffer_size < size )
  {
    auto_Buffer buffer( new HeapBuffer( size ) );
    readBytes( *buffer, size );
    buffer->put( NULL, size );
    return buffer;
  }
  else if ( source_buffer_size == size )
  {
    YIELD::auto_Buffer buffer( source_buffer );
    source_buffer = source_buffer->get_next_buffer();
    return buffer;
  }
  else
  {
    YIELD::auto_Buffer buffer( new PartialBuffer( source_buffer, size ) );
    source_buffer->get( NULL, size );
    return buffer;
  }
}
void BufferedUnmarshaller::readBytes( void* into_buffer, size_t into_buffer_len )
{
  while ( into_buffer_len > 0 )
  {
    size_t consumed_len = source_buffer->get( into_buffer, into_buffer_len );
    if ( consumed_len == into_buffer_len )
      return;
    else
    {
      into_buffer = static_cast<char*>( into_buffer ) + consumed_len;
      into_buffer_len -= consumed_len;
      source_buffer = source_buffer->get_next_buffer();
    }
  }
}


// fixed_buffer.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
FixedBuffer::FixedBuffer( size_t capacity )
  : _capacity( capacity )
{
  _consumed = 0;
  iov.iov_len = 0;
}
void FixedBuffer::as_iovecs( std::vector<struct iovec>& out_iovecs ) const
{
  out_iovecs.push_back( iov );
  Buffer::as_iovecs( out_iovecs );
}
size_t FixedBuffer::capacity() const
{
  return _capacity;
}
FixedBuffer::operator void*() const
{
  return static_cast<uint8_t*>( iov.iov_base ) + _consumed;
}
size_t FixedBuffer::get( void* into_buffer, size_t into_buffer_len )
{
  if ( iov.iov_len - _consumed < into_buffer_len )
    into_buffer_len = iov.iov_len - _consumed;
  if ( into_buffer != NULL )
    memcpy_s( into_buffer, into_buffer_len, static_cast<uint8_t*>( iov.iov_base ) + _consumed, into_buffer_len );
  _consumed += into_buffer_len;
  return into_buffer_len;
}
size_t FixedBuffer::get( std::string& into_string, size_t into_string_len )
{
  if ( iov.iov_len - _consumed < into_string_len )
    into_string_len = iov.iov_len - _consumed;
  into_string.append( reinterpret_cast<char*>( static_cast<uint8_t*>( iov.iov_base ) + _consumed ), into_string_len );
  _consumed += into_string_len;
  return into_string_len;
}
bool FixedBuffer::operator==( const FixedBuffer& other ) const
{
  return iov.iov_len == other.iov.iov_len &&
         memcmp( iov.iov_base, other.iov.iov_base, iov.iov_len ) == 0;
}
size_t FixedBuffer::put( const void* from_buffer, size_t from_buffer_len )
{
  if ( capacity() - iov.iov_len < from_buffer_len )
    from_buffer_len = capacity() - iov.iov_len;
  if ( from_buffer != NULL && from_buffer != iov.iov_base )
    memcpy_s( static_cast<uint8_t*>( iov.iov_base ) + iov.iov_len, capacity() - iov.iov_len, from_buffer, from_buffer_len );
  iov.iov_len += from_buffer_len;
  return from_buffer_len;
}
size_t FixedBuffer::size() const
{
  return iov.iov_len - _consumed;
}


// gather_buffer.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
GatherBuffer::GatherBuffer( const struct iovec* iovecs, uint32_t iovecs_len )
  : iovecs( iovecs ), iovecs_len( iovecs_len )
{ }
void GatherBuffer::as_iovecs( std::vector<struct iovec>& out_iovecs ) const
{
  for ( uint32_t iovec_i = 0; iovec_i < iovecs_len; iovec_i++ )
    out_iovecs.push_back( iovecs[iovec_i] );
  Buffer::as_iovecs( out_iovecs );
}
size_t GatherBuffer::size() const
{
  size_t _size = 0;
  for ( uint32_t iovec_i = 0; iovec_i < iovecs_len; iovec_i++ )
    _size += iovecs[iovec_i].iov_len;
  return _size;
}


// heap_buffer.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
HeapBuffer::HeapBuffer( size_t capacity )
  : FixedBuffer( capacity )
{
  iov.iov_base = new uint8_t[capacity];
}
HeapBuffer::~HeapBuffer()
{
  delete [] static_cast<uint8_t*>( iov.iov_base );
}


// page_aligned_buffer.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#ifdef _WIN32
#include <windows.h>
#else
#include <stdlib.h>
#endif
size_t PageAlignedBuffer::page_size = 0;
PageAlignedBuffer::PageAlignedBuffer( size_t capacity )
  : FixedBuffer( capacity )
{
  if ( page_size == 0 )
  {
#ifdef _WIN32
    SYSTEM_INFO system_info;
    GetSystemInfo( &system_info );
    page_size = system_info.dwPageSize;
#else
    page_size = sysconf( _SC_PAGESIZE );
#endif
  }
#ifdef _WIN32
  iov.iov_base = static_cast<uint8_t*>( _aligned_malloc( capacity, page_size ) );
#else
  posix_memalign( &iov.iov_base, page_size, capacity );
#endif
}
PageAlignedBuffer::~PageAlignedBuffer()
{
  free( iov.iov_base );
}


// string_buffer.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).



StringBuffer::StringBuffer()
{
  _consumed = 0;
}

StringBuffer::StringBuffer( size_t capacity )
{
  _string.reserve( capacity );
  _consumed = 0;
}

StringBuffer::StringBuffer( const std::string& str )
  : _string( str )
{
  _consumed = 0;
}

StringBuffer::StringBuffer( const char* str )
 : _string( str )
{
  _consumed = 0;
}

StringBuffer::StringBuffer( const char* str, size_t str_len )
  : _string( str, str_len )
{
  _consumed = 0;
}

void StringBuffer::as_iovecs( std::vector<struct iovec>& out_iovecs ) const
{
  struct iovec iov;
  iov.iov_base = const_cast<char*>( _string.c_str() );
  iov.iov_len = _string.size();
  out_iovecs.push_back( iov );
  Buffer::as_iovecs( out_iovecs );
}

size_t StringBuffer::get( void* into_buffer, size_t into_buffer_len )
{
  if ( size() - _consumed < into_buffer_len )
    into_buffer_len = size() - _consumed;
  memcpy_s( into_buffer, into_buffer_len, _string.c_str() + _consumed, into_buffer_len );
  _consumed += into_buffer_len;
  return into_buffer_len;
}

size_t StringBuffer::get( std::string& into_string, size_t into_string_len )
{
  if ( size() - _consumed < into_string_len )
    into_string_len = size() - _consumed;
  into_string.append( _string.c_str() + _consumed, into_string_len );
  _consumed += into_string_len;
  return into_string_len;
}

size_t StringBuffer::put( const void* from_buffer, size_t from_buffer_len )
{
  _string.append( static_cast<const char*>( from_buffer ), from_buffer_len );
  return from_buffer_len;
}


// xdr_marshaller.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if defined(_WIN32)
#include <windows.h>
#elif !defined(__MACH__)
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
void XDRMarshaller::writeDeclaration( const YIELD::Declaration& decl )
{
  if ( !in_map_stack.empty() && in_map_stack.back() && decl.get_identifier() )
    YIELD::Marshaller::writeString( YIELD::Declaration(), decl.get_identifier() );
}
void XDRMarshaller::writeBoolean( const YIELD::Declaration& decl, bool value )
{
  writeInt32( decl, value ? 1 : 0 );
}
void XDRMarshaller::writeBuffer( const YIELD::Declaration& decl, YIELD::auto_Buffer value )
{
  size_t value_size = 0;
  YIELD::auto_Buffer next_buffer = value;
  while ( next_buffer != NULL )
  {
    value_size += next_buffer->size();
    next_buffer = next_buffer->get_next_buffer();
  }
  writeInt32( decl, static_cast<int32_t>( value_size ) );
  BufferedMarshaller::write( value );
  if ( value_size % 4 != 0 )
  {
    static char zeros[] = { 0, 0, 0 };
    write( static_cast<const void*>( zeros ), 4 - ( value_size % 4 ) );
  }
}
void XDRMarshaller::writeDouble( const YIELD::Declaration& decl, double value )
{
  writeDeclaration( decl );
  write( &value, sizeof( value ) );
}
void XDRMarshaller::writeFloat( const YIELD::Declaration& decl, float value )
{
  writeDeclaration( decl );
  write( &value, sizeof( value ) );
}
void XDRMarshaller::writeInt32( const YIELD::Declaration& decl, int32_t value )
{
  writeDeclaration( decl );
  value = htonl( value );
  write( &value, sizeof( value ) );
}
void XDRMarshaller::writeInt64( const YIELD::Declaration& decl, int64_t value )
{
  writeDeclaration( decl );
  value = htonll( value );
  write( &value, sizeof( value ) );
}
void XDRMarshaller::writeMap( const YIELD::Declaration& decl, const YIELD::Map& value )
{
  writeInt32( decl, static_cast<int32_t>( value.get_size() ) );
  in_map_stack.push_back( true );
  value.marshal( *this );
  in_map_stack.pop_back();
}
void XDRMarshaller::writeSequence( const YIELD::Declaration& decl, const YIELD::Sequence& value )
{
  writeInt32( decl, static_cast<int32_t>( value.get_size() ) );
  value.marshal( *this );
}
void XDRMarshaller::writeString( const YIELD::Declaration& decl, const char* value, size_t value_len )
{
  writeInt32( decl, static_cast<int32_t>( value_len ) );
  write( static_cast<const void*>( value ), value_len );
  if ( value_len % 4 != 0 )
  {
    static char zeros[] = { 0, 0, 0 };
    write( static_cast<const void*>( zeros ), 4 - ( value_len % 4 ) );
  }
}
void XDRMarshaller::writeStruct( const YIELD::Declaration& decl, const YIELD::Struct& value )
{
  writeDeclaration( decl );
  value.marshal( *this );
}


// xdr_unmarshaller.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
#if defined(_WIN32)
#include <windows.h>
#elif !defined(__MACH__)
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
bool XDRUnmarshaller::readBoolean( const YIELD::Declaration& decl )
{
  return readInt32( decl ) == 1;
}
YIELD::auto_Buffer XDRUnmarshaller::readBuffer( const YIELD::Declaration& decl )
{
  size_t size = readInt32( decl );
  if ( size % 4 == 0 )
    return BufferedUnmarshaller::readBuffer( size );
  else
    return BufferedUnmarshaller::readBuffer( size + 4 - ( size % 4 ) );
}
double XDRUnmarshaller::readDouble( const YIELD::Declaration& )
{
  double value;
  readBytes( &value, sizeof( value ) );
  return value;
}
float XDRUnmarshaller::readFloat( const YIELD::Declaration& )
{
  float value;
  readBytes( &value, sizeof( value ) );
  return value;
}
int32_t XDRUnmarshaller::readInt32( const YIELD::Declaration& )
{
  int32_t value;
  readBytes( &value, sizeof( value ) );
  return ntohl( value );
}
int64_t XDRUnmarshaller::readInt64( const YIELD::Declaration& )
{
  int64_t value;
  readBytes( &value, sizeof( value ) );
  return ntohll( value );
}
YIELD::Map* XDRUnmarshaller::readMap( const YIELD::Declaration& decl, YIELD::Map* value )
{
  if ( value )
  {
    size_t size = readInt32( decl );
    for ( size_t i = 0; i < size; i++ )
      value->unmarshal( *this );
  }
  return value;
}
YIELD::Sequence* XDRUnmarshaller::readSequence( const YIELD::Declaration& decl, YIELD::Sequence* value )
{
  if ( value )
  {
    size_t size = readInt32( decl );
    if ( size <= UINT16_MAX )
    {
      for ( size_t i = 0; i < size; i++ )
        value->unmarshal( *this );
    }
  }
  return value;
}
void XDRUnmarshaller::readString( const YIELD::Declaration& decl, std::string& str )
{
  size_t str_len = readInt32( decl );
  if ( str_len < UINT16_MAX )
  {
    if ( str_len != 0 )
    {
      size_t padded_str_len = str_len % 4;
      if ( padded_str_len == 0 )
        padded_str_len = str_len;
      else
        padded_str_len = str_len + 4 - padded_str_len;
      str.resize( padded_str_len );
      readBytes( const_cast<char*>( str.c_str() ), padded_str_len );
      str.resize( str_len );
    }
  }
}
YIELD::Struct* XDRUnmarshaller::readStruct( const YIELD::Declaration&, YIELD::Struct* value )
{
  if ( value )
    value->unmarshal( *this );
  return value;
}

