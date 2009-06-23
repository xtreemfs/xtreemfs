// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _YIELD_BASE_H_
#define _YIELD_BASE_H_

#define __STDC_LIMIT_MACROS
#ifdef _WIN32
#include "msstdint.h"
#else
#include <stdint.h>
#endif

#include <cstring>
#include <ostream>
#include <string>
#include <vector>

#ifndef _WIN32
#include <sys/uio.h> // For struct iovec
#endif

#include "atomic.h"

#ifdef __sun
#define YIELD yield_
#else
#define YIELD yield
#endif

// #define YIELD_DEBUG_REFERENCE_COUNTING 1

#define YIELD_OBJECT_PROTOTYPES( type_name, tag ) \
    type_name & incRef() { return YIELD::Object::incRef( *this ); } \
    const static uint32_t __tag = static_cast<uint32_t>( tag ); \
    virtual uint32_t get_tag() const { return __tag; } \
    const char* get_type_name() const { return #type_name; }

#define YIELD_OBJECT_TAG( type ) type::__tag


#ifdef _WIN32
struct iovec
{
  size_t iov_len;
  void* iov_base;
};

#else
inline void memcpy_s( void* dest, size_t dest_size, const void* src, size_t count )
{
  memcpy( dest, src, count );
}
#endif


namespace YIELD
{
  class Marshaller;
  class Unmarshaller;


  class Object
  {
  public:
    Object() : refcnt( 1 )
    { }

    static inline void decRef( Object& object )
    {
//#ifdef YIELD_DEBUG_REFERENCE_COUNTING
//      if ( atomic_dec( &object.refcnt ) < 0 )
//        DebugBreak();
//#else
      if ( atomic_dec( &object.refcnt ) == 0 )
        delete &object;
//#endif
    }

    static inline void decRef( Object* object )
    {
      if ( object )
        Object::decRef( *object );
    }

    template <class ObjectType>
    static inline ObjectType& incRef( ObjectType& object )
    {
//#ifdef YIELD_DEBUG_REFERENCE_COUNTING
//      if ( object.refcnt <= 0 )
//        DebugBreak();
//#endif
      atomic_inc( &object.refcnt );
      return object;
    }

    template <class ObjectType>
    static inline ObjectType* incRef( ObjectType* object )
    {
      if ( object )
        incRef( *object );
      return object;
    }

    inline Object& incRef()
    {
      incRef( *this );
      return *this;
    }

    virtual uint32_t get_tag() const = 0;
    virtual const char* get_type_name() const = 0;
    virtual void marshal( Marshaller& ) const { }
    virtual void unmarshal( Unmarshaller& ) { }

  protected:
    virtual ~Object()
    { }

  private:
    volatile int32_t refcnt;
  };


  template <class ObjectType = Object>
  class auto_Object // Like auto_ptr, but using Object::decRef instead of delete; an operator delete( void* ) on Object doesn't work, because the object is destructed before that call
  {
  public:
    auto_Object() : object( 0 ) { }
    auto_Object( ObjectType* object ) : object( object ) { }
    auto_Object( ObjectType& object ) : object( &object ) { }
    auto_Object( const auto_Object<ObjectType>& other ) { object = Object::incRef( other.object ); }
    ~auto_Object() { Object::decRef( object ); }

    inline ObjectType* get() const { return object; }
    auto_Object& operator=( const auto_Object<ObjectType>& other ) { Object::decRef( this->object ); object = Object::incRef( other.object ); return *this; }
    auto_Object& operator=( ObjectType* object ) { Object::decRef( this->object ); this->object = object; return *this; }
    inline bool operator==( const auto_Object<ObjectType>& other ) const { return object == other.object; }
    inline bool operator==( const ObjectType* other ) const { return object == other; }
    inline bool operator!=( const ObjectType* other ) const { return object != other; }
    // operator ObjectType*() const { return object; } // Creates sneaky bugs
    inline ObjectType* operator->() const { return get(); }
    inline ObjectType& operator*() const { return *get(); }
    inline ObjectType* release() { ObjectType* temp_object = object; object = 0; return temp_object; }
    inline void reset( ObjectType* object ) { Object::decRef( this->object ); this->object = object; }

  private:
    ObjectType* object;
  };


  class Buffer : public Object
  {
  public:
    virtual ~Buffer() { }

    virtual void as_iovecs( std::vector<struct iovec>& out_iovecs ) const;
    virtual size_t capacity() const = 0;
    bool empty() const { return size() == 0; }
    virtual size_t get( void* into_buffer, size_t into_buffer_len ) = 0;
    virtual size_t get( std::string& into_string, size_t into_string_len ) = 0;
    auto_Object<Buffer> get_next_buffer() const { return next_buffer; }
    operator char*() const { return static_cast<char*>( static_cast<void*>( *this ) ); }
    operator unsigned char*() const { return static_cast<unsigned char*>( static_cast<void*>( *this ) ); }
    virtual operator void*() const { return NULL; }
    bool operator==( const Buffer& other ) const;
    size_t put( const std::string& from_string ) { return put( from_string.c_str(), from_string.size() ); }
    virtual size_t put( const void* from_buffer, size_t from_buffer_len ) = 0;
    void set_next_buffer( auto_Object<Buffer> next_buffer );
    virtual size_t size() const = 0;

    // Object
    YIELD_OBJECT_PROTOTYPES( Buffer, 0 );

  private:
    auto_Object<Buffer> next_buffer;
  };

  typedef auto_Object<Buffer> auto_Buffer;


  class FixedBuffer : public Buffer
  {
  public:
    bool operator==( const FixedBuffer& other ) const;

    // Buffer
    size_t get( void* into_buffer, size_t into_buffer_len );
    size_t get( std::string&, size_t into_string_len );
    void as_iovecs( std::vector<struct iovec>& out_iovecs ) const;
    size_t capacity() const;
    operator void*() const;
    virtual size_t put( const void* from_buffer, size_t from_buffer_len );
    size_t size() const;

  protected:
    FixedBuffer( size_t capacity );

    struct iovec iov;

  private:
    size_t _capacity;
    size_t _consumed; // Total number of bytes consumed by get()
  };


  class GatherBuffer : public Buffer
  {
  public:
    GatherBuffer( const struct iovec* iovecs, uint32_t iovecs_len );

    // Buffer
    void as_iovecs( std::vector<struct iovec>& out_iovecs ) const;
    size_t capacity() const { return size(); }
    size_t get( void*, size_t ) { return 0; }
    size_t get( std::string&, size_t ) { return 0; }
    size_t put( const void*, size_t ) { return 0; }
    size_t size() const;

  private:
    const struct iovec* iovecs;
    uint32_t iovecs_len;
  };


  class HeapBuffer : public FixedBuffer
  {
  public:
    HeapBuffer( size_t capacity );
    virtual ~HeapBuffer();
  };


  class Map : public Object
  {
  public:
    virtual size_t get_size() const = 0;
  };


  class PageAlignedBuffer : public FixedBuffer
  {
  public:
    PageAlignedBuffer( size_t capacity );
    virtual ~PageAlignedBuffer();

  private:
    static size_t page_size;
  };


  class Sequence : public Object
  {
  public:
    virtual size_t get_size() const = 0;
  };


  template <size_t Capacity>
  class StackBuffer : public FixedBuffer
  {
  public:
    StackBuffer()
      : FixedBuffer( Capacity )
    {
      iov.iov_base = _stack_buffer;
    }

    StackBuffer( const void* from_buffer )
      : FixedBuffer( Capacity )
    {
      iov.iov_base = _stack_buffer;
      memcpy_s( _stack_buffer, Capacity, from_buffer, Capacity );
      iov.iov_len = Capacity;
    }

  private:
    uint8_t _stack_buffer[Capacity];
  };


  class StringBuffer : public Buffer
  {
  public:
    StringBuffer();
    StringBuffer( size_t capacity );
    StringBuffer( const std::string& );
    StringBuffer( const char* );
    StringBuffer( const char*, size_t );

    const char* c_str() const { return _string.c_str(); }
    operator std::string&() { return _string; }
    operator const std::string&() const { return _string; }
    bool operator==( const StringBuffer& other ) const { return _string == other._string; }
    bool operator==( const char* other ) const { return _string == other; }

    // Buffer
    void as_iovecs( std::vector<struct iovec>& out_iovecs ) const;
    size_t capacity() const { return _string.capacity(); }
    size_t get( void* into_buffer, size_t into_buffer_len );
    size_t get( std::string& into_string, size_t into_string_len );
    size_t put( const void*, size_t );
    size_t size() const { return _string.size(); }

  private:
    std::string _string;

    size_t _consumed;
  };


  class StringLiteralBuffer : public FixedBuffer
  {
  public:
    StringLiteralBuffer( const char* string_literal )
      : FixedBuffer( strnlen( string_literal, UINT16_MAX ) )
    {
      iov.iov_base = const_cast<char*>( string_literal );
      iov.iov_len = capacity();
    }

    StringLiteralBuffer( const char* string_literal, size_t string_literal_len )
      : FixedBuffer( string_literal_len )
    {
      iov.iov_base = const_cast<char*>( string_literal );
      iov.iov_len = string_literal_len;
    }

    StringLiteralBuffer( const void* string_literal, size_t string_literal_len )
      : FixedBuffer( string_literal_len )
    {
      iov.iov_base = const_cast<void*>( string_literal );
      iov.iov_len = string_literal_len;
    }

    // Buffer
    size_t put( const void*, size_t ) { return 0; }
  };


  class Struct : public Object
  { };


  class Declaration
  {
  public:
    Declaration() : identifier( 0 ), tag( 0 ) { }
    Declaration( const char* identifier ) : identifier( identifier ), tag( 0 ) { }
    Declaration( const char* identifier, uint32_t tag ) : identifier( identifier ), tag( tag ) { }

    const char* get_identifier() const { return identifier; }
    uint32_t get_tag() const { return tag; }

  private:
    const char* identifier;
    uint32_t tag;
  };


  class Marshaller
  {
  public:
     virtual ~Marshaller() { }

    virtual void writeBoolean( const Declaration& decl, bool value ) = 0;
    virtual void writeBuffer( const Declaration&, auto_Buffer ) { }
    virtual void writeFloat( const Declaration& decl, float value ) { writeDouble( decl, value ); }
    virtual void writeDouble( const Declaration& decl, double value ) = 0;
    virtual void writeInt8( const Declaration& decl, int8_t value ) { writeInt16( decl, value ); }
    virtual void writeInt16( const Declaration& decl, int16_t value ) { writeInt32( decl, value ); }
    virtual void writeInt32( const Declaration& decl, int32_t value ) { writeInt64( decl, value ); }
    virtual void writeInt64( const Declaration& decl, int64_t value ) = 0;
    virtual void writeMap( const Declaration& decl, const Map& value ) = 0;
    virtual void writeSequence( const Declaration& decl, const Sequence& value ) = 0;
    virtual void writeStruct( const Declaration& decl, const Struct& value ) = 0;
    virtual void writeString( const Declaration& decl, const std::string& value ) { writeString( decl, value.c_str(), value.size() ); }
    virtual void writeString( const Declaration& decl, const char* value ) { writeString( decl, value, strnlen( value, UINT16_MAX ) ); }
    virtual void writeString( const Declaration&, const char* value, size_t value_len ) = 0;
    virtual void writeUint8( const Declaration& decl, uint8_t value ) { writeInt8( decl, static_cast<int8_t>( value ) ); }
    virtual void writeUint16( const Declaration& decl, uint16_t value ) { writeInt16( decl, static_cast<int16_t>( value ) ); }
    virtual void writeUint32( const Declaration& decl, uint32_t value ) { writeInt32( decl, static_cast<int32_t>( value ) ); }
    virtual void writeUint64( const Declaration& decl, uint64_t value ) { writeInt64( decl, static_cast<int64_t>( value ) ); }
  };

#define YIDL_MARSHALLER_PROTOTYPES \
  virtual void writeBoolean( const YIELD::Declaration& decl, bool value ); \
  virtual void writeDouble( const YIELD::Declaration& decl, double value ); \
  virtual void writeInt64( const YIELD::Declaration& decl, int64_t value ); \
  virtual void writeMap( const YIELD::Declaration& decl, const YIELD::Map& value ); \
  virtual void writeSequence( const YIELD::Declaration& decl, const YIELD::Sequence& value ); \
  virtual void writeString( const YIELD::Declaration& decl, const char* value, size_t value_len ); \
  virtual void writeStruct( const YIELD::Declaration& decl, const YIELD::Struct& value );


  class PrettyPrinter : public Marshaller
  {
  public:
    PrettyPrinter( std::ostream& os )
      : os( os )
    { }

    PrettyPrinter& operator=( const PrettyPrinter& ) { return *this; }

    // Marshaller
    void writeBoolean( const Declaration&, bool value )
    {
      if ( value )
        os << "true, ";
      else
        os << "false, ";
    }

    void writeDouble( const Declaration&, double value )
    {
      os << value << ", ";
    }

    void writeInt64( const Declaration&, int64_t value )
    {
      os << value << ", ";
    }

    void writeMap( const Declaration&, const Map& value )
    {
      os << value.get_type_name() << "( ";
      value.marshal( *this );
      os << " ), ";
    }

    void writeSequence( const Declaration&, const Sequence& value )
    {
      os << "[ ";
      value.marshal( *this );
      os << " ], ";
    }

    void writeString( const Declaration&, const char* value, size_t value_len )
    {
      os.write( value, value_len );
      os << ", ";
    }

    void writeStruct( const Declaration&, const Struct& value )
    {
      os << value.get_type_name() << "( ";
      value.marshal( *this );
      os << " ), ";
    }

  private:
    std::ostream& os;
  };


  class Unmarshaller
  {
  public:
    virtual ~Unmarshaller() { }

    virtual bool readBoolean( const Declaration& decl ) = 0;
    virtual auto_Buffer readBuffer( const Declaration& ) { return NULL; }
    virtual double readDouble( const Declaration& ) = 0;
    virtual float readFloat( const Declaration& decl ) { return static_cast<float>( readDouble( decl ) ); }
    virtual int8_t readInt8( const Declaration& decl ) { return static_cast<int8_t>( readInt16( decl ) ); }
    virtual int16_t readInt16( const Declaration& decl ) { return static_cast<int16_t>( readInt32( decl ) ); }
    virtual int32_t readInt32( const Declaration& decl ) { return static_cast<int32_t>( readInt64( decl ) ); }
    virtual int64_t readInt64( const Declaration& decl ) = 0;
    virtual Map* readMap( const Declaration& decl, Map* value = NULL ) = 0;
    virtual Sequence* readSequence( const Declaration& decl, Sequence* value = NULL ) = 0;
    virtual void readString( const Declaration& decl, std::string& value ) = 0;
    virtual Struct* readStruct( const Declaration& decl, Struct* value = NULL ) = 0;
    virtual uint8_t readUint8( const Declaration& decl ) { return static_cast<uint8_t>( readInt8( decl ) ); }
    virtual uint16_t readUint16( const Declaration& decl ) { return static_cast<uint16_t>( readInt16( decl ) ); }
    virtual uint32_t readUint32( const Declaration& decl ) { return static_cast<uint32_t>( readInt32( decl ) ); }
    virtual uint64_t readUint64( const Declaration& decl ) { return static_cast<uint64_t>( readInt64( decl ) ); }
  };

#define YIDL_UNMARSHALLER_PROTOTYPES \
  virtual bool readBoolean( const YIELD::Declaration& decl ); \
  virtual double readDouble( const YIELD::Declaration& decl ); \
  virtual int64_t readInt64( const YIELD::Declaration& decl ); \
  virtual YIELD::Map* readMap( const YIELD::Declaration& decl, YIELD::Map* value = NULL ); \
  virtual YIELD::Sequence* readSequence( const YIELD::Declaration& decl, YIELD::Sequence* value = NULL ); \
  virtual void readString( const YIELD::Declaration& decl, std::string& ); \
  virtual YIELD::Struct* readStruct( const YIELD::Declaration& decl, YIELD::Struct* value = NULL );


  class BufferedMarshaller : public YIELD::Marshaller
  {
  public:
    YIELD::auto_Buffer get_buffer() const { return first_buffer; }

  protected:
    BufferedMarshaller()
    { }

    BufferedMarshaller( BufferedMarshaller& parent_buffered_marshaller )
      : current_buffer( parent_buffered_marshaller.current_buffer )
    { }

    void write( const void* buffer, size_t buffer_len );
    void write( YIELD::auto_Buffer buffer );

  private:
    YIELD::auto_Buffer first_buffer, current_buffer;
  };


  class BufferedUnmarshaller : public YIELD::Unmarshaller
  {
  public:
    virtual ~BufferedUnmarshaller() { }

  protected:
    BufferedUnmarshaller( YIELD::auto_Buffer source_buffer )
        : source_buffer( source_buffer )
    { }

    void readBytes( void*, size_t );
    YIELD::auto_Buffer readBuffer( size_t size );

  private:
    YIELD::auto_Buffer source_buffer;
  };


  class XDRMarshaller : public BufferedMarshaller
  {
  public:
    // Marshaller
    YIDL_MARSHALLER_PROTOTYPES;
    void writeBuffer( const Declaration& decl, auto_Buffer value );
    void writeFloat( const Declaration& decl, float value );
    void writeInt32( const Declaration& decl, int32_t value );

  protected:
    inline void write( const void* buffer, size_t buffer_len ) { BufferedMarshaller::write( buffer, buffer_len ); }
    virtual void writeDeclaration( const Declaration& decl );

  private:
    std::vector<bool> in_map_stack;
  };


  class XDRUnmarshaller : public BufferedUnmarshaller
  {
  public:
    XDRUnmarshaller( auto_Buffer source_buffer )
        : BufferedUnmarshaller( source_buffer )
    { }

    // Unmarshaller
    YIDL_UNMARSHALLER_PROTOTYPES;
    auto_Buffer readBuffer( const Declaration& decl );
    float readFloat( const Declaration& decl );
    int32_t readInt32( const Declaration& decl );
  };
};

#endif
