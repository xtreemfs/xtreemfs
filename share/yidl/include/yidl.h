// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the yidl project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the yidl project nor the
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


#ifndef _YIDL_H_
#define _YIDL_H_

#define __STDC_LIMIT_MACROS

#ifdef _WIN32
// msstdint.h
// ISO C9x  compliant stdint.h for Microsoft Visual Studio
// Based on ISO/IEC 9899:TC2 Committee draft (May 6, 2005) WG14/N1124
//
//  Copyright (c) 2006-2008 Alexander Chemeris
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
//   1. Redistributions of source code must retain the above copyright notice,
//      this list of conditions and the following disclaimer.
//
//   2. Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.
//
//   3. The name of the author may be used to endorse or promote products
//      derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
// EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
// OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
// OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
///////////////////////////////////////////////////////////////////////////////

#include <limits.h>

// For Visual Studio 6 in C++ mode wrap <wchar.h> include with 'extern "C++" {}'
// or compiler give many errors like this:
//   error C2733: second C linkage of overloaded function 'wmemchr' not allowed
#if (_MSC_VER < 1300) && defined(__cplusplus)
   extern "C++" {
#endif
#     include <wchar.h>
#if (_MSC_VER < 1300) && defined(__cplusplus)
   }
#endif

// Define _W64 macros to mark types changing their size, like intptr_t.
#ifndef _W64
#  if !defined(__midl) && (defined(_X86_) || defined(_M_IX86)) && _MSC_VER >= 1300
#     define _W64 __w64
#  else
#     define _W64
#  endif
#endif


// 7.18.1 Integer types

// 7.18.1.1 Exact-width integer types
typedef __int8            int8_t;
typedef __int16           int16_t;
typedef __int32           int32_t;
typedef __int64           int64_t;
typedef unsigned __int8   uint8_t;
typedef unsigned __int16  uint16_t;
typedef unsigned __int32  uint32_t;
typedef unsigned __int64  uint64_t;

// 7.18.1.2 Minimum-width integer types
typedef int8_t    int_least8_t;
typedef int16_t   int_least16_t;
typedef int32_t   int_least32_t;
typedef int64_t   int_least64_t;
typedef uint8_t   uint_least8_t;
typedef uint16_t  uint_least16_t;
typedef uint32_t  uint_least32_t;
typedef uint64_t  uint_least64_t;

// 7.18.1.3 Fastest minimum-width integer types
typedef int8_t    int_fast8_t;
typedef int16_t   int_fast16_t;
typedef int32_t   int_fast32_t;
typedef int64_t   int_fast64_t;
typedef uint8_t   uint_fast8_t;
typedef uint16_t  uint_fast16_t;
typedef uint32_t  uint_fast32_t;
typedef uint64_t  uint_fast64_t;

// 7.18.1.4 Integer types capable of holding object pointers
#ifdef _WIN64 // [
   typedef __int64           intptr_t;
   typedef unsigned __int64  uintptr_t;
#else // _WIN64 ][
   typedef _W64 int               intptr_t;
   typedef _W64 unsigned int      uintptr_t;
#endif // _WIN64 ]

// 7.18.1.5 Greatest-width integer types
typedef int64_t   intmax_t;
typedef uint64_t  uintmax_t;


// 7.18.2 Limits of specified-width integer types

#if !defined(__cplusplus) || defined(__STDC_LIMIT_MACROS) // [   See footnote 220 at page 257 and footnote 221 at page 259

// 7.18.2.1 Limits of exact-width integer types
#define INT8_MIN     ((int8_t)_I8_MIN)
#define INT8_MAX     _I8_MAX
#define INT16_MIN    ((int16_t)_I16_MIN)
#define INT16_MAX    _I16_MAX
#define INT32_MIN    ((int32_t)_I32_MIN)
#define INT32_MAX    _I32_MAX
#define INT64_MIN    ((int64_t)_I64_MIN)
#define INT64_MAX    _I64_MAX
#define UINT8_MAX    _UI8_MAX
#define UINT16_MAX   _UI16_MAX
#define UINT32_MAX   _UI32_MAX
#define UINT64_MAX   _UI64_MAX

// 7.18.2.2 Limits of minimum-width integer types
#define INT_LEAST8_MIN    INT8_MIN
#define INT_LEAST8_MAX    INT8_MAX
#define INT_LEAST16_MIN   INT16_MIN
#define INT_LEAST16_MAX   INT16_MAX
#define INT_LEAST32_MIN   INT32_MIN
#define INT_LEAST32_MAX   INT32_MAX
#define INT_LEAST64_MIN   INT64_MIN
#define INT_LEAST64_MAX   INT64_MAX
#define UINT_LEAST8_MAX   UINT8_MAX
#define UINT_LEAST16_MAX  UINT16_MAX
#define UINT_LEAST32_MAX  UINT32_MAX
#define UINT_LEAST64_MAX  UINT64_MAX

// 7.18.2.3 Limits of fastest minimum-width integer types
#define INT_FAST8_MIN    INT8_MIN
#define INT_FAST8_MAX    INT8_MAX
#define INT_FAST16_MIN   INT16_MIN
#define INT_FAST16_MAX   INT16_MAX
#define INT_FAST32_MIN   INT32_MIN
#define INT_FAST32_MAX   INT32_MAX
#define INT_FAST64_MIN   INT64_MIN
#define INT_FAST64_MAX   INT64_MAX
#define UINT_FAST8_MAX   UINT8_MAX
#define UINT_FAST16_MAX  UINT16_MAX
#define UINT_FAST32_MAX  UINT32_MAX
#define UINT_FAST64_MAX  UINT64_MAX

// 7.18.2.4 Limits of integer types capable of holding object pointers
#ifdef _WIN64 // [
#  define INTPTR_MIN   INT64_MIN
#  define INTPTR_MAX   INT64_MAX
#  define UINTPTR_MAX  UINT64_MAX
#else // _WIN64 ][
#  define INTPTR_MIN   INT32_MIN
#  define INTPTR_MAX   INT32_MAX
#  define UINTPTR_MAX  UINT32_MAX
#endif // _WIN64 ]

// 7.18.2.5 Limits of greatest-width integer types
#define INTMAX_MIN   INT64_MIN
#define INTMAX_MAX   INT64_MAX
#define UINTMAX_MAX  UINT64_MAX

// 7.18.3 Limits of other integer types

#ifdef _WIN64 // [
#  define PTRDIFF_MIN  _I64_MIN
#  define PTRDIFF_MAX  _I64_MAX
#else  // _WIN64 ][
#  define PTRDIFF_MIN  _I32_MIN
#  define PTRDIFF_MAX  _I32_MAX
#endif  // _WIN64 ]

#define SIG_ATOMIC_MIN  INT_MIN
#define SIG_ATOMIC_MAX  INT_MAX

#ifndef SIZE_MAX // [
#  ifdef _WIN64 // [
#     define SIZE_MAX  _UI64_MAX
#  else // _WIN64 ][
#     define SIZE_MAX  _UI32_MAX
#  endif // _WIN64 ]
#endif // SIZE_MAX ]

// WCHAR_MIN and WCHAR_MAX are also defined in <wchar.h>
#ifndef WCHAR_MIN // [
#  define WCHAR_MIN  0
#endif  // WCHAR_MIN ]
#ifndef WCHAR_MAX // [
#  define WCHAR_MAX  _UI16_MAX
#endif  // WCHAR_MAX ]

#define WINT_MIN  0
#define WINT_MAX  _UI16_MAX

#endif // __STDC_LIMIT_MACROS ]


// 7.18.4 Limits of other integer types

#if !defined(__cplusplus) || defined(__STDC_CONSTANT_MACROS) // [   See footnote 224 at page 260

// 7.18.4.1 Macros for minimum-width integer constants

#define INT8_C(val)  val##i8
#define INT16_C(val) val##i16
#define INT32_C(val) val##i32
#define INT64_C(val) val##i64

#define UINT8_C(val)  val##ui8
#define UINT16_C(val) val##ui16
#define UINT32_C(val) val##ui32
#define UINT64_C(val) val##ui64

// 7.18.4.2 Macros for greatest-width integer constants
#define INTMAX_C   INT64_C
#define UINTMAX_C  UINT64_C

#endif // __STDC_CONSTANT_MACROS ]

#else // !_WIN32

#include <stdint.h>
#include <sys/uio.h> // For struct iovec

#endif // _WIN32

#include <cstring>
#include <ostream>
#include <string>
#include <vector>


#if defined(_WIN64)
extern "C"
{
  __declspec( dllimport ) void __stdcall DebugBreak();

  __int64 _InterlockedCompareExchange64
  (
    volatile __int64* current_value,
    __int64 new_value,
    __int64 old_value
  );

  __int64 _InterlockedIncrement64( volatile __int64* current_value );
  __int64 _InterlockedDecrement64( volatile __int64* current_value );
}
#elif defined(_WIN32)
extern "C"
{
  __declspec( dllimport ) void __stdcall DebugBreak();

  __declspec( dllimport ) long __stdcall
  InterlockedCompareExchange
  (
    volatile long* current_value,
    long new_value,
    long old_value
  );

  __declspec( dllimport ) long __stdcall
  InterlockedIncrement
  (
    volatile long* current_value
  );

  __declspec( dllimport ) long __stdcall
  InterlockedDecrement
  (
    volatile long* current_value
  );
}
#endif // _WIN32


#ifdef _WIN32

struct iovec // a WSABUF on 32-bit systems
{
  size_t iov_len; 
  void* iov_base;
};

// The WSABUF .len is actually a ULONG, which is != size_t on Win64.
// That means that we have to copy and truncate an iovec to a WSABUF on Win64.
// That's easier (in terms of warnings, use of sizeof, etc.) than having to
// change size_t to uint32_t everywhere.

#ifdef _WIN64
struct iovec64 // a WSABUF on 64-bit systems
{
  uint32_t iov_len;
  void* iov_base;
};
#endif

#else // !_WIN32

static inline void DebugBreak()
{
  *reinterpret_cast<int*>( 0 ) = 0xabadcafe;
}

inline void memcpy_s
(
  void* dest,
  size_t dest_size,
  const void* src,
  size_t count
)
{
  memcpy( dest, src, count );
}

#ifndef __linux__
inline size_t strnlen( const char* s, size_t maxlen )
{
  return strlen( s );
}
#endif

#if defined(__sun)
#include <atomic.h>
#elif defined(__arm__)
// gcc atomic builtins are not defined on ARM
#elif defined(__GNUC__) && \
      ( ( __GNUC__ == 4 && __GNUC_MINOR__ >= 1 ) || __GNUC__ > 4 )
#define __HAVE_GNUC_ATOMIC_BUILTINS 1
#endif

#endif // _WIN32


namespace yidl
{
  namespace runtime
  {
#if defined(_WIN64)
    typedef __int64 atomic_t;
#elif defined(_WIN32)
    typedef long atomic_t;
#elif defined(__LLP64__) || defined(__LP64__)
    typedef int64_t atomic_t;
#else
    typedef int32_t atomic_t;
#endif

    static inline atomic_t 
    atomic_cas
    (
      volatile atomic_t* current_value,
      atomic_t new_value,
      atomic_t old_value
    )
    {
#if defined(_WIN64)
      return _InterlockedCompareExchange64( current_value, new_value, old_value );
#elif defined(_WIN32)
      return InterlockedCompareExchange( current_value, new_value, old_value );
#elif defined(__sun) 
#if defined(__LLP64__) || defined(__LP64)
      return atomic_cas_64
             ( 
               reinterpret_cast<volatile uint64_t*>( current_value ),
               static_cast<uint64_t>( old_value ), 
               static_cast<uint64_t>( new_value )
             );
#else
      return atomic_cas_32
             ( 
               reinterpret_cast<volatile uint32_t*>( current_value ),
               static_cast<uint32_t>( old_value ), 
               static_cast<uint32_t>( new_value )
             );
#endif
#elif defined(__HAVE_GNUC_ATOMIC_BUILTINS)
      return __sync_val_compare_and_swap( current_value, old_value, new_value );
#elif defined(__arm__)
#if __ARM_ARCH__ >= 6
      atomic_t prev;
      asm volatile( "@ atomic_cmpxchg\n"
                    "ldrex  %1, [%2]\n"
                    "mov    %0, #0\n"
                    "teq    %1, %3\n"
                    "strexeq %0, %4, [%2]\n"
                     : "=&r" ( prev), "=&r" ( old_value )
                     : "r" ( current_value ), "Ir" ( old_value ), "r" ( new_value )
                     : "cc" );
      return prev;
#else // ARM architectures < 6 are uniprocessor only
      if ( *current_value == old_value )
      {
        *current_value = new_value;
        return old_value;
      }
      else
        return *current_value;
#endif
#elif defined(__i386__)
      atomic_t prev;
      asm volatile(  "lock\n"
              "cmpxchgl %1,%2\n"
            : "=a" ( prev )
                  : "r" ( new_value ), "m" ( *current_value ) , "0" ( old_value )
                  : "memory"
                );
      return prev;
#elif defined(__ppc__)
      atomic_t prev;
      asm volatile(  "          \n\
              1:  ldarx   %0,0,%2 \n\
              cmpd    0,%0,%3 \n\
              bne     2f    \n\
              stdcx.  %4,0,%2 \n\
              bne-    1b    \n\
              sync\n"
              "2:"
            : "=&r" ( prev ), "=m" ( *current_value )
                  : "r" ( current_value ), "r" ( old_value ), "r" ( new_value ),
                    "m" ( *current_value )
                  : "cc", "memory"
                );
      return prev;
#elif defined(__x86_64__)
      atomic_t prev;
      asm volatile(  "lock\n"
              "cmpxchgq %1,%2\n"
            : "=a" ( prev )
                  : "r" ( new_value ), "m" ( *current_value ) , "0" ( old_value )
                  : "memory"
                );
      return prev;
#endif
    }

    static inline atomic_t atomic_dec( volatile atomic_t* current_value )
    {
#if defined(_WIN64)
      return _InterlockedDecrement64( current_value );
#elif defined(_WIN32)
      return InterlockedDecrement( current_value );
#elif defined(__sun)
#if defined(__LLP64__) || defined(__LP64)
      return atomic_dec_64_nv
             ( 
               reinterpret_cast<volatile uint64_t*>( current_value )
             );
#else
      return atomic_dec_32_nv
             ( 
               reinterpret_cast<volatile uint32_t*>( current_value )
             );
#endif
#elif defined(__HAVE_GNUC_ATOMIC_BUILTINS)
      return __sync_sub_and_fetch( current_value, 1 );
#else
      atomic_t old_value, new_value;

      do
      {
        old_value = *current_value;
        new_value = old_value - 1;
      }
      while ( atomic_cas( current_value, new_value, old_value ) != old_value );

      return new_value;
#endif
    }

    static inline atomic_t atomic_inc( volatile atomic_t* current_value )
    {
#if defined(_WIN64)
      return _InterlockedIncrement64( current_value );
#elif defined(_WIN32)
      return InterlockedIncrement( current_value );
#elif defined(__sun)
#if defined(__LLP64__) || defined(__LP64)
      return atomic_inc_64_nv
             ( 
               reinterpret_cast<volatile uint64_t*>( current_value )
             );
#else
      return atomic_inc_32_nv
             ( 
               reinterpret_cast<volatile uint32_t*>( current_value )
             );
#endif
#elif defined(__HAVE_GNUC_ATOMIC_BUILTINS)
      return __sync_add_and_fetch( current_value, 1 );
#else
      atomic_t old_value, new_value;

      do
      {
        old_value = *current_value;
        new_value = old_value + 1;
      }
      while ( atomic_cas( current_value, new_value, old_value ) != old_value );

      return new_value;
#endif
    }

    // Atomic reference-counted objects
    // Conventions for Objects:
    // Object* method( ... ) -> Object* is a new reference
    //   where method is usually a factory method.
    //   Factory methods that return Object& throw exceptions;
    //     those that return Object* return NULL instead.
    //   The exception to this rule are getters that return an Object*
    //   that is not a new reference
    // method( const Object& ) -> Object is on stack, not a new reference
    // method( Object* or Object* ) -> Object is on heap, but not a
    //   new reference; callee should inc_ref() references for itself
    //   as necessary  
    // Exceptions to these conventions will be marked.
    class Object
    {
    public:
      Object() : refcnt( 1 )
      { }

      static inline void dec_ref( Object& object )
      {
#ifdef YIDL_DEBUG_REFERENCE_COUNTING
        if ( atomic_dec( &object.refcnt ) < 0 )
          DebugBreak();
#else
        if ( atomic_dec( &object.refcnt ) == 0 )
          delete &object;
#endif
      }

      static inline void dec_ref( Object* object )
      {
        if ( object )
          Object::dec_ref( *object );
      }

      template <class ObjectType>
      static inline ObjectType& inc_ref( ObjectType& object )
      {
#ifdef YIDL_DEBUG_REFERENCE_COUNTING
        if ( object.refcnt <= 0 )
          DebugBreak();
#endif
        atomic_inc( &object.refcnt );
        return object;
      }

      template <class ObjectType>
      static inline ObjectType* inc_ref( ObjectType* object )
      {
        if ( object )
          inc_ref( *object );
        return object;
      }

      inline Object& inc_ref()
      {
        inc_ref( *this );
        return *this;
      }

    protected:
      virtual ~Object()
      { }

    private:
      volatile atomic_t refcnt;
    };


    // Similar to auto_ptr, but using object references instead of delete
    // Unlike auto_ptr auto_Object is immutable, so there is no release(),
    // reset(), or operator=().
    // The class is primarily intended for use in testing, where an object
    // should be deleted when it goes out of scope because of an exception.
    template <class ObjectType = Object>
    class auto_Object
    {
    public:
      auto_Object( ObjectType* object )
        : object( *object )
      {
        if ( object == NULL )
          DebugBreak();
      }

      auto_Object( ObjectType& object )
        : object( object )
      { }

      auto_Object( const auto_Object<ObjectType>& other )
        : object( Object::inc_ref( other.object ) )
      { }

      ~auto_Object()
      {
        Object::dec_ref( object );
      }

      ObjectType& get() const
      {
        return object;
      }

      inline ObjectType* operator->() const
      {
        return &object;
      }

      inline ObjectType& operator*() const
      {
        return object;
      }

    private:
      ObjectType& object;
    };


    class RTTIObject : public Object
    {
    public:
      virtual uint32_t get_type_id() const = 0;
      virtual const char* get_type_name() const = 0;

      // Object
      RTTIObject& inc_ref() { return Object::inc_ref( *this ); }
    };

#define YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( type_name, type_id ) \
    const static uint32_t TYPE_ID = static_cast<uint32_t>( type_id ); \
    virtual uint32_t get_type_id() const { return TYPE_ID; } \
    virtual const char* get_type_name() const { return #type_name; }


    class Buffer : public RTTIObject
    {
    public:
      virtual ~Buffer() { }

      // capacity: the number of bytes available in the buffer >= size
      virtual size_t capacity() const = 0;

      inline bool empty() const { return size() == 0; }

      // get: copy out of the buffer, advancing position
      virtual size_t get( void* into_buffer, size_t into_buffer_len ) = 0;

      virtual bool is_fixed() const = 0;

      // Casts to get at the underlying buffer
      inline operator char*() const
      {
        return static_cast<char*>( static_cast<void*>( *this ) );
      }

      inline operator unsigned char*() const
      {
        return static_cast<unsigned char*>( static_cast<void*>( *this ) );
      }

      virtual operator struct iovec() const
      {
        struct iovec iov;
        iov.iov_base = static_cast<char*>( *this );
        iov.iov_len = size();
        return iov;
      }

#ifdef _WIN64
      operator struct iovec64() const
      {
        struct iovec64 iov;
        iov.iov_base = static_cast<char*>( *this );
        iov.iov_len = static_cast<uint32_t>( size() );
        return iov;
      }
#endif

      virtual operator void*() const = 0;

      bool operator==( const Buffer& other ) const
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

      // position: get and set the get() position
      size_t position() const { return _position; }

      void position( size_t new_position )
      {
        if ( new_position < size() )
          _position = new_position;
        else
          _position = size();
      }

      // put: append bytes to the buffer, increases size but not position
      size_t put( const Buffer& from_buffer )
      {
        return put( static_cast<void*>( from_buffer ), from_buffer.size() );
      }

      virtual size_t put( char from_char, size_t repeat_count )
      {
        size_t total_put_ret = 0;
        for ( size_t char_i = 0; char_i < repeat_count; char_i++ )
        {
          size_t put_ret = put( &from_char, 1 );
          if ( put_ret == 1 )
            total_put_ret++;
          else
            break;
        }
        return total_put_ret;
      }

      size_t put( const struct iovec& from_iovec )
      {
        return put( from_iovec.iov_base, from_iovec.iov_len );
      }

      size_t put( const char* from_string )
      {
        return put( from_string, strlen( from_string ) );
      }

      size_t put( const std::string& from_string )
      {
        return put( from_string.c_str(), from_string.size() );
      }

      virtual size_t put( const void* from_buf, size_t from_buf_len ) = 0;

      // put( size_t ): increase the size, usually after copying data
      // directly into the buffer
      virtual void put( size_t len ) = 0;

      // size: the number of filled bytes, <= capacity
      virtual size_t size() const = 0;

      // Object
      Buffer& inc_ref() { return Object::inc_ref( *this ); }

    protected:
      Buffer()
      {
        _position = 0;
      }

    private:
      size_t _position;
    };


    class Buffers : public Object
    {
    public:
      Buffers( Buffer& buffer )
        : buffers( 1 ),
          iovecs( 1 )
      {
        iovecs[0] = buffer;
        buffers[0] = &buffer.inc_ref();
      }

      Buffers( const struct iovec* iovecs, uint32_t iovecs_len )
        : iovecs( iovecs_len )
      { 
        memcpy_s
        (
          &this->iovecs[0],
          this->iovecs.size() * sizeof( struct iovec ),
          iovecs,
          iovecs_len * sizeof( iovecs )
        );
      }

      virtual ~Buffers()
      {
        for 
        ( 
          std::vector<Buffer*>::iterator buffer_i = buffers.begin(); 
          buffer_i != buffers.end(); 
          buffer_i++ 
        )
          Buffer::dec_ref( **buffer_i );          
      }

      operator const struct iovec*() const
      { 
        return &iovecs[0];
      }

      void push_back( Buffer& buffer )
      {
        iovecs.push_back( buffer );
        buffers.push_back( &buffer.inc_ref() );
      }

      void push_back( const struct iovec& iovec )
      {
        iovecs.push_back( iovec );
      }

      uint32_t size() const
      { 
        return static_cast<uint32_t>( iovecs.size() );
      }

      // Object
      Buffers& inc_ref() { return Object::inc_ref( *this ); }

    private:
      std::vector<Buffer*> buffers;
      std::vector<struct iovec> iovecs;
    };


    class FixedBuffer : public Buffer
    {
    public:
      bool operator==( const FixedBuffer& other ) const
      {
        return iov.iov_len == other.iov.iov_len &&
               memcmp( iov.iov_base, other.iov.iov_base, iov.iov_len ) == 0;
      }

      // Buffer
      size_t get( void* into_buffer, size_t into_buffer_len )
      {
        if ( size() - position() < into_buffer_len )
          into_buffer_len = size() - position();

        if ( into_buffer != NULL )
        {
          memcpy_s
          (
            into_buffer,
            into_buffer_len,
            static_cast<uint8_t*>( iov.iov_base ) + position(),
            into_buffer_len
          );
        }

        position( position() + into_buffer_len );

        return into_buffer_len;
      }

      size_t capacity() const { return _capacity; }
      bool is_fixed() const { return true; }
      operator struct iovec() const { return iov; }
      operator void*() const { return iov.iov_base; }

      virtual size_t put( const void* from_buffer, size_t from_buffer_len )
      {
        size_t put_len;
        if ( capacity() - size() >= from_buffer_len )
          put_len = from_buffer_len;
        else
          put_len = capacity() - size();

        memcpy_s
        (
          static_cast<uint8_t*>( iov.iov_base ) + size(),
          capacity() - size(),
          from_buffer,
          put_len
        );

        iov.iov_len += put_len;

        return put_len;
      }

      virtual void put( size_t put_len )
      {
        iov.iov_len += put_len;
      }

      size_t size() const { return iov.iov_len; }

    protected:
      FixedBuffer( size_t capacity )
        : _capacity( capacity )
      {
        iov.iov_len = 0;
      }

      struct iovec iov;

    private:
      size_t _capacity;
    };


    class HeapBuffer : public FixedBuffer
    {
    public:
      HeapBuffer( size_t capacity )
        : FixedBuffer( capacity )
      {
        iov.iov_base = new uint8_t[capacity];
      }

      virtual ~HeapBuffer()
      {
        delete [] static_cast<uint8_t*>( iov.iov_base );
      }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( HeapBuffer, 1 );
    };


    class Marshaller;
    class Unmarshaller;
  
    class MarshallableObject : public RTTIObject
    {
    public:
      virtual void marshal( Marshaller& marshaller ) const = 0;
      virtual void unmarshal( Unmarshaller& unmarshaller ) = 0;

      // Object
      MarshallableObject& inc_ref() { return Object::inc_ref( *this ); }
    };


    class MarshallableObjectFactory : public RTTIObject
    {
    public:
      virtual MarshallableObject* createMarshallableObject( uint32_t type_id )
      {
        return NULL;
      }

      // Object
      MarshallableObjectFactory& inc_ref() { return Object::inc_ref( *this ); }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( MarshallableObjectFactory, 1 );
    };


    class Map : public MarshallableObject
    {
    public:
      virtual size_t get_size() const = 0;
    };


    class Sequence;
    class Struct;

    class Marshaller : public Object
    {
    public:
      virtual ~Marshaller() { }

      // bool
      virtual void write( const char* key, uint32_t tag, bool value ) = 0;


      // Buffer
      virtual void
      write
      (
        const char* key,
        uint32_t tag,
        const Buffer& value
      ) = 0;


      // float and double
      virtual void write( const char* key, uint32_t tag, float value )
      {
        write( key, tag, static_cast<double>( value ) );
      }

      virtual void write( const char* key, uint32_t tag, double value ) = 0;


      // Signed integers
      virtual void write( const char* key, uint32_t tag, int8_t value )
      {
        write( key, tag, static_cast<int16_t>( value ) );
      }

      virtual void write( const char* key, uint32_t tag, int16_t value )
      {
        write( key, tag, static_cast<int32_t>( value ) );
      }

      virtual void write( const char* key, uint32_t tag, int32_t value )
      {
        write( key, tag, static_cast<int64_t>( value ) );
      }

      virtual void write( const char* key, uint32_t tag, int64_t value ) = 0;


      // Map
      virtual void
      write
      (
        const char* key,
        uint32_t tag,
        const Map& value
      ) = 0;


      // MarshallableObject
      virtual void
      write
      (
        const char* key,
        uint32_t tag,
        const MarshallableObject& value
      ) = 0;


      // Sequence
      virtual void        
      write
      (
        const char* key,
        uint32_t tag,
        const Sequence& value
      ) = 0;


      // Strings
      void
      write
      (
        const char* key,
        uint32_t tag,
        const std::string& value
      )
      {
        write( key, tag, value.c_str(), value.size() );
      }

      void
      write
      (
        const char* key,
        uint32_t tag,
        const char* value
      )
      {
        write( key, tag, value, strnlen( value, UINT16_MAX ) );
      }

      virtual void
      write
      (
        const char* key,
        uint32_t tag,
        const char* value,
        size_t value_len
      ) = 0;


      // Unsigned integers
      virtual void write( const char* key, uint32_t tag, uint8_t value )
      {
        write( key, tag, static_cast<uint16_t>( value ) );
      }

      virtual void write( const char* key, uint32_t tag, uint16_t value )
      {
        write( key, tag, static_cast<uint32_t>( value ) );
      }

      virtual void write( const char* key, uint32_t tag, uint32_t value )
      {
        write( key, tag, static_cast<uint64_t>( value ) );
      }

      virtual void write( const char* key, uint32_t tag, uint64_t value )
      {
        write( key, tag, static_cast<int64_t>( value ) );
      }
    };

#define YIDL_MARSHALLER_PROTOTYPES \
    virtual void write( const char* key, uint32_t tag, bool value ); \
    virtual void \
    write \
    ( \
      const char* key, \
      uint32_t tag, \
      const ::yidl::runtime::Buffer& value \
    ); \
    virtual void write( const char* key, uint32_t tag, double value ); \
    virtual void write( const char* key, uint32_t tag, int64_t value ); \
    virtual void \
    write \
    ( \
      const char* key, \
      uint32_t tag, \
      const ::yidl::runtime::Map& value \
    ); \
    virtual void \
    write \
    ( \
      const char* key, \
      uint32_t tag, \
      const ::yidl::runtime::MarshallableObject& value \
    ); \
    virtual void \
    write \
    ( \
      const char* key, \
      uint32_t tag, \
      const ::yidl::runtime::Sequence& value \
    ); \
    virtual void \
    write \
    ( \
      const char* key, \
      uint32_t tag, \
      const char* value, \
      size_t value_len \
    );


    class PrettyPrinter : public Marshaller
    {
    public:
      PrettyPrinter( std::ostream& os )
        : os( os )
      { }

      PrettyPrinter& operator=( const PrettyPrinter& )
      {
        return *this;
      }

      // Marshaller
      void write( const char*, uint32_t, bool value )
      {
        if ( value )
          os << "true, ";
        else
          os << "false, ";
      }

      void write( const char*, uint32_t, const Buffer& )
      { }

      void write( const char*, uint32_t, double value )
      {
        os << value << ", ";
      }

      void write( const char*, uint32_t, int64_t value )
      {
        os << value << ", ";
      }

      void write( const char*, uint32_t, const Map& value )
      {
        os << value.get_type_name() << "( ";
        value.marshal( *this );
        os << " ), ";
      }

      void write( const char*, uint32_t, const Sequence& value );

      void 
      write
      (
        const char*,
        uint32_t,
        const char* value,
        size_t value_len
      )
      {
        os.write( value, value_len );
        os << ", ";
      }

      void write( const char*, uint32_t, const MarshallableObject& value )
      {
        os << value.get_type_name() << "( ";
        value.marshal( *this );
        os << " ), ";
      }

    private:
      std::ostream& os;
    };


    class Sequence : public MarshallableObject
    { 
    public:
      virtual size_t get_size() const = 0;
    };

    inline void
    PrettyPrinter::write
    (
      const char*,
      uint32_t,
      const Sequence& value
    )
    {
      os << "[ ";
      value.marshal( *this );
      os << " ], ";
    }


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

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StackBuffer, 2 );

    private:
      uint8_t _stack_buffer[Capacity];
    };


    class StringBuffer : public Buffer
    {
    public:
      StringBuffer()
      { }

      StringBuffer( size_t capacity )
      {
        _string.reserve( capacity );
      }

      StringBuffer( const std::string& str )
        : _string( str )
      { }

      StringBuffer( const char* str )
       : _string( str )
      { }

      StringBuffer( const char* str, size_t str_len )
        : _string( str, str_len )
      { }

      operator std::string&() { return _string; }
      operator const std::string&() const { return _string; }

      // Buffer
      operator void*() const { return const_cast<char*>( _string.c_str() ); }

      bool operator==( const StringBuffer& other ) const
      {
        return _string == other._string;
      }

      bool operator==( const char* other ) const
      {
        return _string == other;
      }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StringBuffer, 3 );

      // Buffer
      size_t capacity() const { return _string.capacity(); }

      size_t get( void* into_buffer, size_t into_buffer_len )
      {
        size_t get_len;
        if ( size() - position() >= into_buffer_len )
          get_len = into_buffer_len;
        else
          get_len = size() - position();

        memcpy_s
        (
          into_buffer,
          into_buffer_len,
          _string.c_str() + position(),
          get_len
        );

        position( position() + get_len );

        return get_len;
      }

      bool is_fixed() const { return false; }

      size_t put( char from_char, size_t repeat_count )
      {
        _string.append( repeat_count, from_char );
        return repeat_count;
      }

      size_t put( const void* from_buffer, size_t from_buffer_len )
      {
        _string.append
        (
          static_cast<const char*>( from_buffer ),
          from_buffer_len
        );

        return from_buffer_len;
      }

      void put( size_t ) { DebugBreak(); }
      size_t size() const { return _string.size(); }

    private:
      std::string _string;
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

      StringLiteralBuffer
      (
        const char* string_literal,
        size_t string_literal_len
      )
        : FixedBuffer( string_literal_len )
      {
        iov.iov_base = const_cast<char*>( string_literal );
        iov.iov_len = string_literal_len;
      }

      StringLiteralBuffer
      (
        const void* string_literal,
        size_t string_literal_len
      )
        : FixedBuffer( string_literal_len )
      {
        iov.iov_base = const_cast<void*>( string_literal );
        iov.iov_len = string_literal_len;
      }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StringLiteralBuffer, 4 );

      // Buffer
      size_t put( const void*, size_t ) { return 0; }
      void put( size_t ) { DebugBreak(); }
    };


    class Struct : public MarshallableObject
    { };


    class Unmarshaller : public Object
    {
    public:
      virtual ~Unmarshaller() { }

      // bool
      virtual bool read_bool( const char* key, uint32_t tag ) = 0;


      // Buffer
      virtual void
      read
      (
        const char* key,
        uint32_t tag,
        Buffer& value
      ) = 0;


      // float and double
      virtual float read_float( const char* key, uint32_t tag )
      {
        return static_cast<float>( read_double( key, tag ) );
      }

      virtual double read_double( const char* key, uint32_t tag ) = 0;


      // Signed integers
      virtual int8_t read_int8( const char* key, uint32_t tag )
      {
        return static_cast<int8_t>( read_int16( key, tag ) );
      }

      virtual int16_t read_int16( const char* key, uint32_t tag )
      {
        return static_cast<int16_t>( read_int32( key, tag ) );
      }

      virtual int32_t read_int32( const char* key, uint32_t tag )
      {
        return static_cast<int32_t>( read_int64( key, tag ) );
      }

      virtual int64_t read_int64( const char* key, uint32_t tag ) = 0;


      // Map
      virtual void read( const char* key, uint32_t tag, Map& value ) = 0;


      // MarshallableObject
      virtual void
      read
      (
        const char* key,
        uint32_t tag,
        MarshallableObject& value
      ) = 0;


      // Sequence
      virtual void
      read
      (
        const char* key,
        uint32_t tag,
        Sequence& value
      ) = 0;


      // String
      virtual void
      read
      (
        const char* key,
        uint32_t tag,
        std::string& value
      ) = 0;


      // Unsigned integers
      virtual uint8_t read_uint8( const char* key, uint32_t tag )
      {
        return static_cast<uint8_t>( read_int8( key, tag ) );
      }

      virtual uint16_t read_uint16( const char* key, uint32_t tag )
      {
        return static_cast<uint16_t>( read_int16( key, tag ) );
      }

      virtual uint32_t read_uint32( const char* key, uint32_t tag )
      {
        return static_cast<uint32_t>( read_int32( key, tag ) );
      }

      virtual uint64_t read_uint64( const char* key, uint32_t tag )
      {
        return static_cast<uint64_t>( read_int64( key, tag ) );
      }
    };

#define YIDL_UNMARSHALLER_PROTOTYPES \
    virtual bool read_bool( const char* key, uint32_t tag ); \
    virtual void \
    read \
    ( \
      const char* key, \
      uint32_t tag, \
      ::yidl::runtime::Buffer& value \
    ); \
    virtual double read_double( const char* key, uint32_t tag ); \
    virtual int64_t read_int64( const char* key, uint32_t tag ); \
    virtual void \
    read \
    ( \
      const char* key, \
      uint32_t tag, \
      ::yidl::runtime::Map& value \
    ); \
    virtual void \
    read \
    ( \
      const char* key, \
      uint32_t tag, \
      ::yidl::runtime::MarshallableObject& value \
    ); \
    virtual void \
    read \
    ( \
      const char* key, \
      uint32_t tag, \
      ::yidl::runtime::Sequence& value \
    ); \
    virtual void \
    read \
    ( \
      const char* key, \
      uint32_t tag, \
      std::string& value \
    );


    class XDRMarshaller : public yidl::runtime::Marshaller
    {
    public:
      XDRMarshaller()
      {
        buffer = new yidl::runtime::StringBuffer;
      }

      virtual ~XDRMarshaller()
      {
        Buffer::dec_ref( *buffer );
      }

      Buffer& get_buffer() const { return *buffer; }

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

      // Marshaller
      void write( const char* key, uint32_t tag, bool value )
      {
        write( key, tag, value ? 1 : 0 );
      }

      void write( const char* key, uint32_t tag, const Buffer& value )
      {
        write( key, tag, static_cast<int32_t>( value.size() ) );

        buffer->put( static_cast<void*>( value ), value.size() );

        if ( value.size() % 4 != 0 )
        {
          static char zeros[] = { 0, 0, 0 };
          buffer->put
          ( 
            static_cast<const void*>( zeros ), 
            4 - ( value.size() % 4 ) 
          );
        }
      }

      void write( const char* key, uint32_t, double value )
      {
        write_key( key );
        uint64_t uint64_value;
        memcpy_s
        ( 
          &uint64_value, 
          sizeof( uint64_value ), 
          &value, 
          sizeof( value ) 
        );
        uint64_value = htonll( uint64_value );
        buffer->put( &uint64_value, sizeof( uint64_value ) );
      }

      void write( const char* key, uint32_t, float value )
      {
        write_key( key );
        uint32_t uint32_value;
        memcpy_s
        ( 
          &uint32_value, 
          sizeof( uint32_value ), 
          &value, 
          sizeof( value ) 
        );
        uint32_value = htonl( uint32_value );
        buffer->put( &uint32_value, sizeof( uint32_value ) );
      }

      void write( const char* key, uint32_t, int32_t value )
      {
        write_key( key );
        value = htonl( value );
        buffer->put( &value, sizeof( value ) );
      }

      void write( const char* key, uint32_t, int64_t value )
      {
        write_key( key );
        value = htonll( value );
        buffer->put( &value, sizeof( value ) );
      }

      void write
      (
        const char* key,
        uint32_t tag,
        const yidl::runtime::Map& value
      )
      {
        write( key, tag, static_cast<int32_t>( value.get_size() ) );
        in_map_stack.push_back( true );
        value.marshal( *this );
        in_map_stack.pop_back();
      }

      void write( const char* key, uint32_t tag, const Sequence& value )
      {
        write( key, tag, static_cast<int32_t>( value.get_size() ) );
        value.marshal( *this );
      }

      void write( const char* key, uint32_t, const MarshallableObject& value )
      {
        write_key( key );
        value.marshal( *this );
      }

      void write
      (
        const char* key,
        uint32_t tag,
        const char* value,
        size_t value_len
      )
      {
        write( key, tag, static_cast<int32_t>( value_len ) );
        buffer->put( static_cast<const void*>( value ), value_len );
        if ( value_len % 4 != 0 )
        {
          static char zeros[] = { 0, 0, 0 };
          buffer->put
          ( 
            static_cast<const void*>( zeros ), 
            4 - ( value_len % 4 ) 
          );
        }
      }

      void write( const char* key, uint32_t tag, uint32_t value )
      {
        write( key, tag, static_cast<int32_t>( value ) );
      }
      
    protected:
      virtual void write_key( const char* key )
      {
        if ( !in_map_stack.empty() && in_map_stack.back() && key != NULL )
          Marshaller::write( NULL, 0, key );
      }

    private:
      Buffer* buffer;
      std::vector<bool> in_map_stack;
    };


    class XDRUnmarshaller : public yidl::runtime::Unmarshaller
    {
    public:
      XDRUnmarshaller( Buffer& buffer )
        : buffer( buffer.inc_ref() )
      { }

      ~XDRUnmarshaller()
      {
        Buffer::dec_ref( buffer );
      }

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

      // Unmarshaller
      bool read_bool( const char* key, uint32_t tag )
      {
        return read_int32( key, tag ) == 1;
      }

      void read( const char* key, uint32_t tag, Buffer& value )
      {
        size_t size = read_int32( key, tag );
        if ( value.capacity() - value.size() < size ) DebugBreak();
        read( static_cast<void*>( value ), size );
        value.put( size );
        if ( size % 4 != 0 )
        {
          char zeros[3];
          read( zeros, 4 - ( size % 4 ) );
        }
      }

      double read_double( const char*, uint32_t )
      {
        uint64_t uint64_value;
        read( &uint64_value, sizeof( uint64_value ) );
        uint64_value = ntohll( uint64_value );
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

      float read_float( const char*, uint32_t )
      {
        uint32_t uint32_value;
        read( &uint32_value, sizeof( uint32_value ) );
        uint32_value = ntohl( uint32_value );
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

      int32_t read_int32( const char*, uint32_t )
      {
        int32_t value;
        read( &value, sizeof( value ) );
        return ntohl( value );
      }

      int64_t read_int64( const char*, uint32_t )
      {
        int64_t value;
        read( &value, sizeof( value ) );
        return ntohll( value );
      }

      void read( const char* key, uint32_t tag, Map& value )
      {
        size_t size = read_int32( key, tag );
        for ( size_t i = 0; i < size; i++ )
          value.unmarshal( *this );
      }

      void read( const char*, uint32_t, MarshallableObject& value )
      {
        value.unmarshal( *this );
      }

      void read( const char* key, uint32_t tag, Sequence& value )
      {
        size_t size = read_int32( key, tag );
        if ( size <= UINT16_MAX )
        {
          for ( size_t i = 0; i < size; i++ )
            value.unmarshal( *this );
        }
      }

      void read( const char* key, uint32_t tag, std::string& value )
      {
        size_t str_len = read_int32( key, tag );

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

    private:
      void read( void* buffer, size_t buffer_len )
      {
      //#ifdef _DEBUG
      //  if ( this->buffer->size() - this->buffer->position() < buffer_len )
      //    DebugBreak();
      //#endif
        this->buffer.get( buffer, buffer_len );
      }

    private:
      Buffer& buffer;
    };
  };
};

#endif
