// Revision: 1586

#include "yield/base.h"
using namespace YIELD;


// marshaller_test_types.h
#include <map>
#include <string>
#include <vector>
namespace com
{
  namespace googlecode
  {
    namespace yield
    {
      namespace base
      {
        namespace marshaller_test_types
        {
          class BooleanStruct : public ::YIELD::Struct
          {
          public:
            BooleanStruct() : boolean_data( false ) { }
            BooleanStruct( bool boolean_data ) : boolean_data( boolean_data ) { }
            virtual ~BooleanStruct() { }
            void set_boolean_data( bool boolean_data ) { this->boolean_data = boolean_data; }
            bool get_boolean_data() const { return boolean_data; }
            bool operator==( const BooleanStruct& other ) const { return boolean_data == other.boolean_data; }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( BooleanStruct, 1001 );
            // YIELD::Struct
            void marshal( ::YIELD::Marshaller& marshaller ) const { marshaller.writeBoolean( ::YIELD::Declaration( "boolean_data" ), boolean_data ); }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { boolean_data = unmarshaller.readBoolean( ::YIELD::Declaration( "boolean_data" ) ); }
          protected:
            bool boolean_data;
          };
          class DoubleStruct : public ::YIELD::Struct
          {
          public:
            DoubleStruct() : double_data( 0 ) { }
            DoubleStruct( double double_data ) : double_data( double_data ) { }
            virtual ~DoubleStruct() { }
            void set_double_data( double double_data ) { this->double_data = double_data; }
            double get_double_data() const { return double_data; }
            bool operator==( const DoubleStruct& other ) const { return double_data == other.double_data; }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( DoubleStruct, 1003 );
            // YIELD::Struct
            void marshal( ::YIELD::Marshaller& marshaller ) const { marshaller.writeDouble( ::YIELD::Declaration( "double_data" ), double_data ); }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { double_data = unmarshaller.readDouble( ::YIELD::Declaration( "double_data" ) ); }
          protected:
            double double_data;
          };
          class EmptyStruct : public ::YIELD::Struct
          {
          public:
            EmptyStruct() { }
            virtual ~EmptyStruct() { }
            bool operator==( const EmptyStruct& ) const { return true; }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( EmptyStruct, 1004 );
          };
          class FloatStruct : public ::YIELD::Struct
          {
          public:
            FloatStruct() : float_data( 0 ) { }
            FloatStruct( float float_data ) : float_data( float_data ) { }
            virtual ~FloatStruct() { }
            void set_float_data( float float_data ) { this->float_data = float_data; }
            float get_float_data() const { return float_data; }
            bool operator==( const FloatStruct& other ) const { return float_data == other.float_data; }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( FloatStruct, 1005 );
            // YIELD::Struct
            void marshal( ::YIELD::Marshaller& marshaller ) const { marshaller.writeFloat( ::YIELD::Declaration( "float_data" ), float_data ); }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { float_data = unmarshaller.readFloat( ::YIELD::Declaration( "float_data" ) ); }
          protected:
            float float_data;
          };
          class Int8Struct : public ::YIELD::Struct
          {
          public:
            Int8Struct() : int8_data( 0 ) { }
            Int8Struct( int8_t int8_data ) : int8_data( int8_data ) { }
            virtual ~Int8Struct() { }
            void set_int8_data( int8_t int8_data ) { this->int8_data = int8_data; }
            int8_t get_int8_data() const { return int8_data; }
            bool operator==( const Int8Struct& other ) const { return int8_data == other.int8_data; }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( Int8Struct, 1006 );
            // YIELD::Struct
            void marshal( ::YIELD::Marshaller& marshaller ) const { marshaller.writeInt8( ::YIELD::Declaration( "int8_data" ), int8_data ); }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { int8_data = unmarshaller.readInt8( ::YIELD::Declaration( "int8_data" ) ); }
          protected:
            int8_t int8_data;
          };
          class Int16Struct : public ::YIELD::Struct
          {
          public:
            Int16Struct() : int16_data( 0 ) { }
            Int16Struct( int16_t int16_data ) : int16_data( int16_data ) { }
            virtual ~Int16Struct() { }
            void set_int16_data( int16_t int16_data ) { this->int16_data = int16_data; }
            int16_t get_int16_data() const { return int16_data; }
            bool operator==( const Int16Struct& other ) const { return int16_data == other.int16_data; }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( Int16Struct, 1007 );
            // YIELD::Struct
            void marshal( ::YIELD::Marshaller& marshaller ) const { marshaller.writeInt16( ::YIELD::Declaration( "int16_data" ), int16_data ); }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { int16_data = unmarshaller.readInt16( ::YIELD::Declaration( "int16_data" ) ); }
          protected:
            int16_t int16_data;
          };
          class Int32Struct : public ::YIELD::Struct
          {
          public:
            Int32Struct() : int32_data( 0 ) { }
            Int32Struct( int32_t int32_data ) : int32_data( int32_data ) { }
            virtual ~Int32Struct() { }
            void set_int32_data( int32_t int32_data ) { this->int32_data = int32_data; }
            int32_t get_int32_data() const { return int32_data; }
            bool operator==( const Int32Struct& other ) const { return int32_data == other.int32_data; }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( Int32Struct, 1008 );
            // YIELD::Struct
            void marshal( ::YIELD::Marshaller& marshaller ) const { marshaller.writeInt32( ::YIELD::Declaration( "int32_data" ), int32_data ); }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { int32_data = unmarshaller.readInt32( ::YIELD::Declaration( "int32_data" ) ); }
          protected:
            int32_t int32_data;
          };
          class Int64Struct : public ::YIELD::Struct
          {
          public:
            Int64Struct() : int64_data( 0 ) { }
            Int64Struct( int64_t int64_data ) : int64_data( int64_data ) { }
            virtual ~Int64Struct() { }
            void set_int64_data( int64_t int64_data ) { this->int64_data = int64_data; }
            int64_t get_int64_data() const { return int64_data; }
            bool operator==( const Int64Struct& other ) const { return int64_data == other.int64_data; }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( Int64Struct, 1009 );
            // YIELD::Struct
            void marshal( ::YIELD::Marshaller& marshaller ) const { marshaller.writeInt64( ::YIELD::Declaration( "int64_data" ), int64_data ); }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { int64_data = unmarshaller.readInt64( ::YIELD::Declaration( "int64_data" ) ); }
          protected:
            int64_t int64_data;
          };
          class StringStruct : public ::YIELD::Struct
          {
          public:
            StringStruct() { }
            StringStruct( const std::string& string_data ) : string_data( string_data ) { }
            StringStruct( const char* string_data, size_t string_data_len ) : string_data( string_data, string_data_len ) { }
            virtual ~StringStruct() { }
            void set_string_data( const std::string& string_data ) { set_string_data( string_data.c_str(), string_data.size() ); }
            void set_string_data( const char* string_data, size_t string_data_len ) { this->string_data.assign( string_data, string_data_len ); }
            const std::string& get_string_data() const { return string_data; }
            bool operator==( const StringStruct& other ) const { return string_data == other.string_data; }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( StringStruct, 1010 );
            // YIELD::Struct
            void marshal( ::YIELD::Marshaller& marshaller ) const { marshaller.writeString( ::YIELD::Declaration( "string_data" ), string_data ); }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { unmarshaller.readString( ::YIELD::Declaration( "string_data" ), string_data ); }
          protected:
            std::string string_data;
          };
          class Uint8Struct : public ::YIELD::Struct
          {
          public:
            Uint8Struct() : uint8_data( 0 ) { }
            Uint8Struct( uint8_t uint8_data ) : uint8_data( uint8_data ) { }
            virtual ~Uint8Struct() { }
            void set_uint8_data( uint8_t uint8_data ) { this->uint8_data = uint8_data; }
            uint8_t get_uint8_data() const { return uint8_data; }
            bool operator==( const Uint8Struct& other ) const { return uint8_data == other.uint8_data; }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( Uint8Struct, 1011 );
            // YIELD::Struct
            void marshal( ::YIELD::Marshaller& marshaller ) const { marshaller.writeUint8( ::YIELD::Declaration( "uint8_data" ), uint8_data ); }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { uint8_data = unmarshaller.readUint8( ::YIELD::Declaration( "uint8_data" ) ); }
          protected:
            uint8_t uint8_data;
          };
          class Uint16Struct : public ::YIELD::Struct
          {
          public:
            Uint16Struct() : uint16_data( 0 ) { }
            Uint16Struct( uint16_t uint16_data ) : uint16_data( uint16_data ) { }
            virtual ~Uint16Struct() { }
            void set_uint16_data( uint16_t uint16_data ) { this->uint16_data = uint16_data; }
            uint16_t get_uint16_data() const { return uint16_data; }
            bool operator==( const Uint16Struct& other ) const { return uint16_data == other.uint16_data; }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( Uint16Struct, 1012 );
            // YIELD::Struct
            void marshal( ::YIELD::Marshaller& marshaller ) const { marshaller.writeUint16( ::YIELD::Declaration( "uint16_data" ), uint16_data ); }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { uint16_data = unmarshaller.readUint16( ::YIELD::Declaration( "uint16_data" ) ); }
          protected:
            uint16_t uint16_data;
          };
          class Uint32Struct : public ::YIELD::Struct
          {
          public:
            Uint32Struct() : uint32_data( 0 ) { }
            Uint32Struct( uint32_t uint32_data ) : uint32_data( uint32_data ) { }
            virtual ~Uint32Struct() { }
            void set_uint32_data( uint32_t uint32_data ) { this->uint32_data = uint32_data; }
            uint32_t get_uint32_data() const { return uint32_data; }
            bool operator==( const Uint32Struct& other ) const { return uint32_data == other.uint32_data; }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( Uint32Struct, 1013 );
            // YIELD::Struct
            void marshal( ::YIELD::Marshaller& marshaller ) const { marshaller.writeUint32( ::YIELD::Declaration( "uint32_data" ), uint32_data ); }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { uint32_data = unmarshaller.readUint32( ::YIELD::Declaration( "uint32_data" ) ); }
          protected:
            uint32_t uint32_data;
          };
          class Uint64Struct : public ::YIELD::Struct
          {
          public:
            Uint64Struct() : uint64_data( 0 ) { }
            Uint64Struct( uint64_t uint64_data ) : uint64_data( uint64_data ) { }
            virtual ~Uint64Struct() { }
            void set_uint64_data( uint64_t uint64_data ) { this->uint64_data = uint64_data; }
            uint64_t get_uint64_data() const { return uint64_data; }
            bool operator==( const Uint64Struct& other ) const { return uint64_data == other.uint64_data; }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( Uint64Struct, 1014 );
            // YIELD::Struct
            void marshal( ::YIELD::Marshaller& marshaller ) const { marshaller.writeUint64( ::YIELD::Declaration( "uint64_data" ), uint64_data ); }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { uint64_data = unmarshaller.readUint64( ::YIELD::Declaration( "uint64_data" ) ); }
          protected:
            uint64_t uint64_data;
          };
          class StringSet : public ::YIELD::Sequence, public std::vector<std::string>
          {
          public:
            StringSet() { }
            StringSet( const std::string& first_value ) { std::vector<std::string>::push_back( first_value ); }
            StringSet( size_type size ) : std::vector<std::string>( size ) { }
            virtual ~StringSet() { }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( StringSet, 1015 );
            // YIELD::Sequence
            size_t get_size() const { return size(); }
            void marshal( ::YIELD::Marshaller& marshaller ) const { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeString( ::YIELD::Declaration( "value" ), ( *this )[value_i] ); } }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { std::string value; unmarshaller.readString( ::YIELD::Declaration( "value" ), value ); push_back( value ); }
          };
          class StringStructSet : public ::YIELD::Sequence, public std::vector<com::googlecode::yield::base::marshaller_test_types::StringStruct>
          {
          public:
            StringStructSet() { }
            StringStructSet( const com::googlecode::yield::base::marshaller_test_types::StringStruct& first_value ) { std::vector<com::googlecode::yield::base::marshaller_test_types::StringStruct>::push_back( first_value ); }
            StringStructSet( size_type size ) : std::vector<com::googlecode::yield::base::marshaller_test_types::StringStruct>( size ) { }
            virtual ~StringStructSet() { }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( StringStructSet, 1016 );
            // YIELD::Sequence
            size_t get_size() const { return size(); }
            void marshal( ::YIELD::Marshaller& marshaller ) const { size_type value_i_max = size(); for ( size_type value_i = 0; value_i < value_i_max; value_i++ ) { marshaller.writeStruct( ::YIELD::Declaration( "value" ), ( *this )[value_i] ); } }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { com::googlecode::yield::base::marshaller_test_types::StringStruct value; unmarshaller.readStruct( ::YIELD::Declaration( "value" ), &value ); push_back( value ); }
          };
          class StringMap : public ::YIELD::Map, public std::map<std::string,std::string>
          {
          public:
            virtual ~StringMap() { }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( StringMap, 1017 );
            // YIELD::Map
            size_t get_size() const { return size(); }
            void marshal( ::YIELD::Marshaller& marshaller ) const { for ( const_iterator i = begin(); i != end(); i++ ) { marshaller.writeString( ::YIELD::Declaration( i->first.c_str(), static_cast<uint32_t>( 0 ) ), i->second ); } }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { std::string key; unmarshaller.readString( ::YIELD::Declaration( 0, static_cast<uint32_t>( 0 ) ), key ); if ( !key.empty() ) { std::string value; unmarshaller.readString( ::YIELD::Declaration( key.c_str(), static_cast<uint32_t>( 0 ) ), value ); ( *this )[key] = value; } }
          };
          class StringStructMap : public ::YIELD::Map, public std::map<std::string,com::googlecode::yield::base::marshaller_test_types::StringStruct>
          {
          public:
            virtual ~StringStructMap() { }
            // YIELD::Object
            YIELD_OBJECT_PROTOTYPES( StringStructMap, 1018 );
            // YIELD::Map
            size_t get_size() const { return size(); }
            void marshal( ::YIELD::Marshaller& marshaller ) const { for ( const_iterator i = begin(); i != end(); i++ ) { marshaller.writeStruct( ::YIELD::Declaration( i->first.c_str(), static_cast<uint32_t>( 0 ) ), i->second ); } }
            void unmarshal( ::YIELD::Unmarshaller& unmarshaller ) { std::string key; unmarshaller.readString( ::YIELD::Declaration( 0, static_cast<uint32_t>( 0 ) ), key ); if ( !key.empty() ) { com::googlecode::yield::base::marshaller_test_types::StringStruct value; unmarshaller.readStruct( ::YIELD::Declaration( key.c_str(), static_cast<uint32_t>( 0 ) ), &value ); ( *this )[key] = value; } }
          };
        };
      };
    };
  };
};


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

