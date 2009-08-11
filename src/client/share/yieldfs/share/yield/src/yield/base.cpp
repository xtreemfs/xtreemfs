// Revision: 1796

#include "yield/base.h"
using namespace YIELD;


// buffer.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
Buffer::Buffer()
{
  _position = 0;
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
size_t Buffer::position() const
{
  return _position;
}
void Buffer::position( size_t new_position )
{
  if ( new_position < size() )
    _position = new_position;
  else
    _position = size();
}
size_t Buffer::put( const char* from_string )
{
  return put( from_string, strlen( from_string ) );
}
size_t Buffer::put( const std::string& from_string )
{
  return put( from_string.c_str(), from_string.size() );
}


// fixed_buffer.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
FixedBuffer::FixedBuffer( size_t capacity )
  : _capacity( capacity )
{
  iov.iov_len = 0;
}
size_t FixedBuffer::capacity() const
{
  return _capacity;
}
FixedBuffer::operator void*() const
{
  return iov.iov_base;
}
size_t FixedBuffer::get( void* into_buffer, size_t into_buffer_len )
{
  if ( iov.iov_len - position() < into_buffer_len )
    into_buffer_len = iov.iov_len - position();
  if ( into_buffer != NULL )
    memcpy_s( into_buffer, into_buffer_len, static_cast<uint8_t*>( iov.iov_base ) + position(), into_buffer_len );
  position( position() + into_buffer_len );
  return into_buffer_len;
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
  return iov.iov_len;
}


// gather_buffer.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
GatherBuffer::GatherBuffer( const struct iovec* iovecs, uint32_t iovecs_len )
  : iovecs( iovecs ), iovecs_len( iovecs_len )
{ }
GatherBuffer::operator void*() const
{
  *((int*)0) = 0xabadcafe;
  return NULL;
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


// pretty_printer.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
void PrettyPrinter::writeBoolean( const char*, uint32_t, bool value )
{
  if ( value )
    os << "true, ";
  else
    os << "false, ";
}
void PrettyPrinter::writeBuffer( const char*, uint32_t, auto_Buffer )
{ }
void PrettyPrinter::writeDouble( const char*, uint32_t, double value )
{
  os << value << ", ";
}
void PrettyPrinter::writeInt64( const char*, uint32_t, int64_t value )
{
  os << value << ", ";
}
void PrettyPrinter::writeMap( const char*, uint32_t, const Map& value )
{
  os << value.get_type_name() << "( ";
  value.marshal( *this );
  os << " ), ";
}
void PrettyPrinter::writeSequence( const char*, uint32_t, const Sequence& value )
{
  os << "[ ";
  value.marshal( *this );
  os << " ], ";
}
void PrettyPrinter::writeString( const char*, uint32_t, const char* value, size_t value_len )
{
  os.write( value, value_len );
  os << ", ";
}
void PrettyPrinter::writeStruct( const char*, uint32_t, const Struct& value )
{
  os << value.get_type_name() << "( ";
  value.marshal( *this );
  os << " ), ";
}


// string_buffer.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
StringBuffer::StringBuffer()
{ }
StringBuffer::StringBuffer( size_t capacity )
{
  _string.reserve( capacity );
}
StringBuffer::StringBuffer( const std::string& str )
  : _string( str )
{ }
StringBuffer::StringBuffer( const char* str )
 : _string( str )
{ }
StringBuffer::StringBuffer( const char* str, size_t str_len )
  : _string( str, str_len )
{ }
size_t StringBuffer::get( void* into_buffer, size_t into_buffer_len )
{
  if ( size() - position() < into_buffer_len )
    into_buffer_len = size() - position();
  memcpy_s( into_buffer, into_buffer_len, _string.c_str() + position(), into_buffer_len );
  position( position() + into_buffer_len );
  return into_buffer_len;
}
size_t StringBuffer::put( const void* from_buffer, size_t from_buffer_len )
{
  _string.append( static_cast<const char*>( from_buffer ), from_buffer_len );
  return from_buffer_len;
}
size_t StringBuffer::size() const
{
  return _string.size();
}


// string_literal_buffer.cpp
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).
StringLiteralBuffer::StringLiteralBuffer( const char* string_literal )
  : FixedBuffer( strnlen( string_literal, UINT16_MAX ) )
{
  iov.iov_base = const_cast<char*>( string_literal );
  iov.iov_len = capacity();
}
StringLiteralBuffer::StringLiteralBuffer( const char* string_literal, size_t string_literal_len )
  : FixedBuffer( string_literal_len )
{
  iov.iov_base = const_cast<char*>( string_literal );
  iov.iov_len = string_literal_len;
}
StringLiteralBuffer::StringLiteralBuffer( const void* string_literal, size_t string_literal_len )
  : FixedBuffer( string_literal_len )
{
  iov.iov_base = const_cast<void*>( string_literal );
  iov.iov_len = string_literal_len;
}

