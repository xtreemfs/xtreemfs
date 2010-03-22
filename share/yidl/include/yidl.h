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

#include <cstring>
using std::memcmp;
using std::memcpy;
using std::strlen;

#include <stdint.h>
#include <sys/uio.h> // For struct iovec

#ifdef __linux__
#include <string.h> // For strnlen
#endif

#endif // _WIN32

#include <string>
using std::string;

#include <map> // For Map subclasses to use
using std::map;

#include <vector>
using std::vector;


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
        if ( atomic_dec( &object.refcnt ) == 0 )
          delete &object;
      }

      static inline void dec_ref( Object* object )
      {
        if ( object != NULL )
          Object::dec_ref( *object );
      }

      template <class ObjectType>
      static inline ObjectType& inc_ref( ObjectType& object )
      {
        atomic_inc( &object.refcnt );
        return object;
      }

      template <class ObjectType>
      static inline ObjectType* inc_ref( ObjectType* object )
      {
        if ( object != NULL )
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
      virtual size_t get( void* buf, size_t len ) = 0;

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

      char operator[]( int n )
      {
        return static_cast<char*>( *this )[n];
      }

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
      size_t put( Buffer& buf )
      {
        size_t put_ret
          = put
            (
              static_cast<char*>( buf ) + buf.position(),
              buf.size() - buf.position()
            );

        buf.position( buf.position() + put_ret );

        return put_ret;
      }

      virtual size_t put( char buf, size_t repeat_count )
      {
        size_t total_put_ret = 0;
        for ( size_t char_i = 0; char_i < repeat_count; char_i++ )
        {
          size_t put_ret = put( &buf, 1 );
          if ( put_ret == 1 )
            total_put_ret++;
          else
            break;
        }
        return total_put_ret;
      }

      size_t put( const struct iovec& iov )
      {
        return put( iov.iov_base, iov.iov_len );
      }

      size_t put( const char* buf )
      {
        return put( buf, strlen( buf ) );
      }

      size_t put( const string& buf )
      {
        return put( buf.c_str(), buf.size() );
      }

      virtual size_t put( const void* buf, size_t len ) = 0;

      virtual void resize( size_t n ) = 0;

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


    class Buffers : public Object, private vector<struct iovec>
    {
    public:
      Buffers()
      {
        init();
      }

      Buffers( Buffer& buffer )
        : vector<struct iovec>( 1 )
      {
        vector<struct iovec>::at( 0 ).iov_base = &buffer.inc_ref();
        vector<struct iovec>::at( 0 ).iov_len = static_cast<size_t>( -1 );
        init();
      }

      Buffers( const struct iovec* iov, uint32_t iov_size )
        : vector<struct iovec>( iov_size )
      {
        memcpy_s
        (
          &vector<struct iovec>::front(),
          size() * sizeof( struct iovec ),
          iov,
          iov_size * sizeof( iov )
        );

        init();
      }

      Buffers( const Buffers& other )
        : vector<struct iovec>( other )
      {
        for ( size_t iov_i = 0; iov_i < size(); iov_i++ )
          Object::inc_ref( get_Buffer( iov_i ) );

        init();
      }

      Buffers( const Buffers& head, const Buffers& tail )
        : vector<struct iovec>( head )
      {
        vector<struct iovec>::insert( end(), tail.begin(), tail.end() );

        for ( size_t iov_i = 0; iov_i < size(); iov_i++ )
          Object::inc_ref( get_Buffer( iov_i ) );

        init();
      }

      virtual ~Buffers()
      {
        for ( size_t iov_i = 0; iov_i < size(); iov_i++ )
          Buffer::dec_ref( get_Buffer( iov_i ) );

        delete [] finalized_iov;
        Buffer::dec_ref( joined_buffer );
      }

      inline bool empty() const
      {
        return vector<struct iovec>::empty();
      }

      void extend( const Buffers& other )
      {
        size_t start_size = size();
        vector<struct iovec>::insert( end(), other.begin(), other.end() );
        for ( size_t iov_i = start_size; iov_i < size(); iov_i++ )
          Object::inc_ref( get_Buffer( iov_i ) );
      }

      size_t get( void* buf, size_t len )
      {
        char* buf_p = static_cast<char*>( buf );

        uint32_t iov_i = 0;
        size_t iov_off = position();
        size_t iov_len = get_iov_len( 0 );
        while ( iov_off >= iov_len )
        {
          iov_off -= iov_len;
          if ( ++iov_i < size() )
            iov_len = get_iov_len( iov_i );
          else
            return 0;
        }

        for ( ;; )
        {
          if ( iov_len - iov_off < len )
          {
            // len is larger than the current iovec
            size_t copy_len = iov_len - iov_off;
            memcpy_s
            (
              buf_p,
              len,
              get_iov_base( iov_i ) + iov_off,
              copy_len
            );

            buf_p += copy_len;
            _position += copy_len;

            if ( ++iov_i < size() )
            {
              len -= copy_len;
              iov_off = 0;
              iov_len = get_iov_len( iov_i );
            }
            else
              break;
          }
          else
          {
            // len is smaller than the current iovec
            memcpy_s
            (
              buf_p,
              len,
              get_iov_base( iov_i ) + iov_off,
              len
            );

            buf_p += len;
            _position += len;

            break;
          }
        }

        return buf_p - static_cast<char*>( buf );
      }

      Buffer& join();

      size_t join_size() const
      {
        size_t join_size = 0;
        for ( uint32_t iov_i = 0; iov_i < size(); iov_i++ )
          join_size += get_iov_len( iov_i );
        return join_size;
      }

      struct iovec operator[]( int iov_i ) const
      {
        Buffer* buffer = get_Buffer( iov_i );
        if ( buffer != NULL )
          return *buffer;
        else
          return vector<struct iovec>::at( iov_i );
      }

      operator const struct iovec*()
      {
        for ( uint32_t iov_i = 0; iov_i < size(); iov_i++ )
        {
          Buffer* buffer = get_Buffer( iov_i );
          if ( buffer != NULL )
          {
            delete [] finalized_iov;

            finalized_iov = new struct iovec[size()];

            for ( uint32_t iov_j = 0; iov_j < iov_i; iov_j++ )
              finalized_iov[iov_j] = at( iov_j );

            finalized_iov[iov_i] = *buffer;

            for ( ; iov_i < size(); iov_i++ )
            {
              buffer = get_Buffer( iov_i );
              if ( buffer != NULL )
                finalized_iov[iov_i] = *buffer;
              else
                finalized_iov[iov_i] = vector<struct iovec>::at( iov_i );
            }

            return finalized_iov;
          }
        }

        // No Buffers
        return &front();
      }

      operator char*()
      {
        Buffer& joined_buffer = join();
        Buffer::dec_ref( joined_buffer );
        return joined_buffer;
      }

      operator unsigned char*()
      {
        Buffer& joined_buffer = join();
        Buffer::dec_ref( joined_buffer );
        return joined_buffer;
      }

      // position: get and set the get() position
      size_t position() const { return _position; }

      void position( size_t new_position )
      {
        size_t join_size = this->join_size();
        if ( new_position <= join_size )
          _position = new_position;
        else
          _position = join_size;
      }

      void push_back( Buffer& buffer )
      {
        struct iovec iov;
        iov.iov_base = &buffer.inc_ref();
        iov.iov_len = static_cast<size_t>( -1 );
        push_back( iov );
      }

      void push_back( Buffer* buffer ) // Steals this reference
      {
        struct iovec iov;
        iov.iov_base = buffer;
        iov.iov_len = static_cast<size_t>( -1 );
        push_back( iov );
      }

      void push_back( char* buf ); // Copies

      void push_back( const char* buf )
      {
        push_back( buf, strnlen( buf, UINT16_MAX ) );
      }

      void push_back( const string& buf ); // Copies

      void push_back( void* buf, size_t len ); // Copies

      void push_back( const void* buf, size_t len )
      {
        struct iovec iov;
        iov.iov_base = const_cast<void*>( buf );
        iov.iov_len = len;
        push_back( iov );
      }

      void push_back( const struct iovec& iov )
      {
        vector<struct iovec>::push_back( iov );
      }

      inline uint32_t size() const
      {
        return static_cast<uint32_t>( vector<struct iovec>::size() );
      }

      // Object
      Buffers& inc_ref() { return Object::inc_ref( *this ); }

    private:
      inline Buffer* get_Buffer( size_t iov_i ) const
      {
        const struct iovec& iov = at( iov_i );
        if ( iov.iov_len == static_cast<size_t>( -1 ) )
          return static_cast<Buffer*>( iov.iov_base );
        else
          return NULL;
      }

      inline char* get_iov_base( size_t iov_i ) const
      {
        Buffer* buffer = get_Buffer( iov_i );
        if ( buffer != NULL )
          return static_cast<char*>( *buffer );
        else
          return static_cast<char*>( at( iov_i ).iov_base );
      }

      inline size_t get_iov_len( size_t iov_i ) const
      {
        Buffer* buffer = get_Buffer( iov_i );
        if ( buffer != NULL )
          return buffer->size();
        else
          return at( iov_i ).iov_len;
      }

      void init()
      {
        finalized_iov = NULL;
        joined_buffer = NULL;
        _position = 0;
      }

    private:
      struct iovec* finalized_iov;
      Buffer* joined_buffer;
      size_t _position;
    };


    class FixedBuffer : public Buffer
    {
    public:
      FixedBuffer( void* buffer, size_t capacity, size_t size )
        : _capacity( capacity )
      {
        iov.iov_base = buffer;
        iov.iov_len = size;
      }

      bool operator==( const FixedBuffer& other ) const
      {
        return iov.iov_len == other.iov.iov_len &&
               memcmp( iov.iov_base, other.iov.iov_base, iov.iov_len ) == 0;
      }

      // Buffer
      size_t get( void* buf, size_t len )
      {
        if ( size() - position() < len )
          len = size() - position();

        if ( buf != NULL )
        {
          memcpy_s
          (
            buf,
            len,
            static_cast<uint8_t*>( iov.iov_base ) + position(),
            len
          );
        }

        position( position() + len );

        return len;
      }

      size_t capacity() const { return _capacity; }
      bool is_fixed() const { return true; }
      operator struct iovec() const { return iov; }
      operator void*() const { return iov.iov_base; }

      virtual size_t put( const void* buf, size_t len )
      {
        size_t put_len;
        if ( capacity() - size() >= len )
          put_len = len;
        else
          put_len = capacity() - size();

        memcpy_s
        (
          static_cast<uint8_t*>( iov.iov_base ) + size(),
          capacity() - size(),
          buf,
          put_len
        );

        iov.iov_len += put_len;

        return put_len;
      }

      void resize( size_t n ) { iov.iov_len = n; }
      size_t size() const { return iov.iov_len; }

    private:
      struct iovec iov;
      size_t _capacity;
    };


    class HeapBuffer : public FixedBuffer
    {
    public:
      HeapBuffer( void* buffer, size_t capacity, size_t size )
        : FixedBuffer( new uint8_t[capacity], capacity, 0 )
      {
        Buffer::put( buffer, size );
      }

      HeapBuffer( size_t capacity )
        : FixedBuffer( new uint8_t[capacity], capacity, 0 )
      { }

      HeapBuffer( Buffer& buffer )
        : FixedBuffer
          (
            new uint8_t[buffer.size() - buffer.position()],
            buffer.size() - buffer.position(),
            0
          )
      {
        Buffer::put( buffer );
      }

      virtual ~HeapBuffer()
      {
        delete [] static_cast<char*>( *this );
      }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( HeapBuffer, 1 );
    };


    inline Buffer& Buffers::join()
    {
      if ( size() == 1 )
      {
        Buffer* buffer = get_Buffer( 0 );
        if ( buffer != NULL )
          return buffer->inc_ref();
      }

      size_t join_size = this->join_size();

      if ( joined_buffer != NULL && joined_buffer->size() == join_size )
      {
        char* joined_buffer_p = *joined_buffer;

        uint32_t iov_i;
        for ( iov_i = 0; iov_i < size(); iov_i++ )
        {
          char* iov_base = get_iov_base( iov_i );
          size_t iov_len = get_iov_len( iov_i );
          if ( memcmp( joined_buffer_p, iov_base, iov_len ) == 0 )
            joined_buffer_p += iov_len;
          else
            break;
        }

        if ( iov_i == size() ) // The joined_buffer has not changed
          return joined_buffer->inc_ref();
        else
          Buffer::dec_ref( *joined_buffer );
      }

      joined_buffer = new HeapBuffer( join_size );
      char* joined_buffer_p = *joined_buffer;

      for ( uint32_t iov_i = 0; iov_i < size(); iov_i++ )
      {
        char* iov_base = get_iov_base( iov_i );
        size_t iov_len = get_iov_len( iov_i );
        joined_buffer->put( iov_base, iov_len );
        joined_buffer_p += iov_len;
      }

      return joined_buffer->inc_ref();
    }


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


    class MarshallableObjectFactory : public Object
    {
    public:
      virtual MarshallableObject* createMarshallableObject( uint32_t type_id )
      {
        return NULL;
      }

      // Object
      MarshallableObjectFactory& inc_ref() { return Object::inc_ref( *this ); }
    };


    class Map : public MarshallableObject
    {
    public:
      virtual size_t get_size() const = 0;
    };


    class MarshallerKeyTypes
    {
    public:
      class Key : public Object
      {
      public:
        Key() : tag( 0 ) { }

        inline uint32_t get_tag() const { return tag; }

        enum Type
        { 
          TYPE_ANONYMOUS,
          TYPE_DOUBLE,
          TYPE_INT32,
          TYPE_INT64,
          TYPE_STRING,
          TYPE_STRING_LITERAL
        };

        virtual Type get_type() const { return TYPE_ANONYMOUS; }

      protected:
        Key( uint32_t tag ) : tag( tag ) { }

      private:
        uint32_t tag;
      };

      class StringKey : public Key, public string
      {
      public:
        StringKey() { }
        StringKey( const string& key ) : string( key ) { }

        StringKey( const char* key, size_t key_len )
          : string( key, key_len )
        { }

        StringKey( const string& key, uint32_t tag )
          : Key( tag ), string( key )
        { }

        StringKey( const char* key, const char* key_len, uint32_t tag )
          : Key( tag ), string( key, key_len )
        { }

        // Key
        Type get_type() const { return TYPE_STRING; }
      };

#define YIDL_RUNTIME_KEY_TYPE( ClassPrefix, CPPName, TYPE )\
      class ClassPrefix ## Key : public Key\
      {\
      public:\
        ClassPrefix ## Key( CPPName key ) : key( key ) { }\
        ClassPrefix ## Key( CPPName key, uint32_t tag )\
          : Key( tag ), key( key )\
        { }\
        inline operator CPPName() const { return key; }\
        Type get_type() const { return TYPE_ ## TYPE; }\
      private:\
        CPPName key;\
      };

      YIDL_RUNTIME_KEY_TYPE( Double, double, DOUBLE );
      YIDL_RUNTIME_KEY_TYPE( Int32, int32_t, INT32 );
      YIDL_RUNTIME_KEY_TYPE( Int64, int64_t, INT64 );
      YIDL_RUNTIME_KEY_TYPE( StringLiteral, const char*, STRING_LITERAL );
    };

    class Sequence;
    class Struct;

    class Marshaller : public Object, public MarshallerKeyTypes
    {
    public:
      virtual ~Marshaller() { }

      // bool
      void write( const char* key, bool value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, bool value ) { }

      // Buffer
      void write( const char* key, Buffer& value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, Buffer& value )
      {
        write( key, static_cast<char*>( value ), value.size() );
      }

      // Buffers
      void write( const char* key, Buffers& value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, Buffers& value )
      {
        auto_Object<Buffer> joined_value = value.join();
        write( key, *joined_value );
      }

      // float and double
      void write( const char* key, float value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, float value )
      {
        write( key, static_cast<double>( value ) );
      }

      void write( const char* key, double value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, double value ) { }

      // Signed integers
      void write( const char* key, int8_t value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, int8_t value )
      {
        write( key, static_cast<int16_t>( value ) );
      }

      void write( const char* key, int16_t value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, int16_t value )
      {
        write( key, static_cast<int32_t>( value ) );
      }

      void write( const char* key, int32_t value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, int32_t value )
      {
        write( key, static_cast<int64_t>( value ) );
      }

      void write( const char* key, int64_t value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, int64_t value ) { }

      // Map
      void write( const char* key, const Map& value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, const Map& value )
      {
        write( key, static_cast<const MarshallableObject&>( value ) );
      }

      // MarshallableObject
      void write( const char* key, const MarshallableObject& value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, const MarshallableObject& value )
      {
        value.marshal( *this );
      }

      // Sequence
      void write( const char* key, const Sequence& value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, const Sequence& value )
      { }

      // strings
      void write( const char* key, char* value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, char* value )
      {
        write( key, value, strnlen( value, UINT16_MAX ) );
      }

      void write( const char* key, const char* value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, const char* value )
      {
        write( key, value, strnlen( value, UINT16_MAX ) );
      }

      void write( const char* key, const string& value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, const string& value )
      {
        write( key, const_cast<char*>( value.c_str() ), value.size() );
      }

      void write( const char* key, char* value, size_t value_len )
      {
        write( StringLiteralKey( key ), value, value_len );
      }

      virtual void write( const Key& key, char* value, size_t value_len )
      {
        write( key, const_cast<const char*>( value ), value_len );
      }

      void write( const char* key, const char* value, size_t value_len )
      {
        write( StringLiteralKey( key ), value, value_len );
      }

      virtual void write( const Key& key, const char* value, size_t value_len )
      { }

      // Unsigned integers
      void write( const char* key, uint8_t value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, uint8_t value )
      {
        write( key, static_cast<uint16_t>( value ) );
      }

      void write( const char* key, uint16_t value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, uint16_t value )
      {
        write( key, static_cast<uint32_t>( value ) );
      }

      void write( const char* key, uint32_t value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, uint32_t value )
      {
        write( key, static_cast<uint64_t>( value ) );
      }

      void write( const char* key, uint64_t value )
      {
        write( StringLiteralKey( key ), value );
      }

      virtual void write( const Key& key, uint64_t value )
      {
        write( key, static_cast<int64_t>( value ) );
      }
    };


    class Sequence : public MarshallableObject
    {
    public:
      virtual size_t get_size() const = 0;
    };


    template <size_t Capacity>
    class StackBuffer : public FixedBuffer
    {
    public:
      StackBuffer()
        : FixedBuffer( _stack_buffer, Capacity, 0 )
      { }

      StackBuffer( const void* buf )
        : FixedBuffer( _stack_buffer, Capacity, Capacity )
      {
        memcpy_s( _stack_buffer, Capacity, buf, Capacity );
      }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StackBuffer, 2 );

    private:
      uint8_t _stack_buffer[Capacity];
    };


    class StringBuffer : public Buffer, public string
    {
    public:
      StringBuffer() { }
      StringBuffer( size_t capacity ) { reserve( capacity ); }
      StringBuffer( const string& buf ) : string( buf ) { }
      StringBuffer( const char* buf ) : string( buf ) { }
      StringBuffer( const char* buf, size_t len ) : string( buf, len ) { }

      StringBuffer( Buffer& buf )
        : string
          (
            static_cast<char*>( buf ) + buf.position(),
            buf.size() - buf.position()
          )
      {
        buf.position( buf.size() );
      }

      // Buffer
      operator void*() const { return const_cast<char*>( c_str() ); }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( StringBuffer, 3 );

      // Buffer
      size_t capacity() const { return string::capacity(); }
      bool empty() const { return string::empty(); }

      size_t get( void* buf, size_t len )
      {
        size_t get_len;
        if ( size() - position() >= len )
          get_len = len;
        else
          get_len = size() - position();

        memcpy_s( buf, len, c_str() + position(), get_len );

        position( position() + get_len );

        return get_len;
      }

      bool is_fixed() const { return false; }

      size_t put( char buf, size_t repeat_count )
      {
        append( repeat_count, buf );
        return repeat_count;
      }

      size_t put( const void* buf, size_t len )
      {
        append( static_cast<const char*>( buf ), len );
        return len;
      }

      void resize( size_t n ) { string::resize( n ); }
      size_t size() const { return string::size(); }
    };


    inline void Buffers::push_back( char* buf )
    {
      push_back( new StringBuffer( buf ) );
    }

    inline void Buffers::push_back( const string& buf )
    {
      push_back( new StringBuffer( buf ) );
    }

    inline void Buffers::push_back( void* buf, size_t len )
    {
      push_back( new StringBuffer( static_cast<char*>( buf ), len ) );
    }


    class Struct : public MarshallableObject
    { };


    class SubBuffer : public FixedBuffer
    {
    public:
      SubBuffer
      (
        Buffer& parent_buffer,
        size_t parent_buffer_offset,
        size_t capacity,
        size_t size
      )
        : FixedBuffer
          (
            static_cast<char*>( parent_buffer ) + parent_buffer_offset,
            capacity,
            size
          ),
          parent_buffer( parent_buffer.inc_ref() )
      { }

      ~SubBuffer()
      {
        Buffer::dec_ref( parent_buffer );
      }

      // RTTIObject
      YIDL_RUNTIME_RTTI_OBJECT_PROTOTYPES( SubBuffer, 5 );

    private:
      Buffer& parent_buffer;
    };


    class Unmarshaller : public Object, public MarshallerKeyTypes
    {
    public:
      virtual ~Unmarshaller() { }

      // bool
      bool read_bool( const char* key )
      {
        return read_bool( StringLiteralKey( key ) );
      }

      virtual bool read_bool( const Key& key ) { return false; }

      // Buffer
      void read( const char* key, Buffer& value )
      {
        read( StringLiteralKey( key ), value );
      }

      virtual void read( const Key& key, Buffer& value ) { }

      // float and double
      float read_float( const char* key )
      {
        return read_float( StringLiteralKey( key ) );
      }

      virtual float read_float( const Key& key )
      {
        double double_value;
        read( key, double_value );
        return static_cast<float>( double_value );
      }

      void read( const char* key, double& value )
      {
        read( StringLiteralKey( key ), value );
      }

      virtual void read( const Key& key, double& value ) 
      { 
        value = 0;
      }

      // Signed integers
      int8_t read_int8( const char* key )
      {
        return read_int8( StringLiteralKey( key ) );
      }

      virtual int8_t read_int8( const Key& key )
      {
        return static_cast<int8_t>( read_int16( key ) );
      }

      int16_t read_int16( const char* key )
      {
        return read_int16( StringLiteralKey( key ) );
      }

      virtual int16_t read_int16( const Key& key )
      {
        return static_cast<int16_t>( read_int32( key ) );
      }

      int32_t read_int32( const char* key )
      {
        return read_int32( StringLiteralKey( key ) );
      }

      virtual int32_t read_int32( const Key& key )
      {
        int64_t int64_value;
        read( key, int64_value );
        return static_cast<int32_t>( int64_value );
      }

      void read( const char* key, int64_t& value )
      {
        read( StringLiteralKey( key ), value );
      }

      virtual void read( const Key& key, int64_t& value ) { value = 0; }

      // Key
      virtual Key* read( Key::Type ) { return NULL; }

      // Map
      void read( const char* key, Map& value )
      {
        read( StringLiteralKey( key ), value );
      }

      virtual void read( const Key& key, Map& value ) { }

      // MarshallableObject
      void read( const char* key, MarshallableObject& value )
      {
        read( StringLiteralKey( key ), value );
      }

      virtual void read( const Key& key, MarshallableObject& value ) { }

      // Sequence
      void read( const char* key, Sequence& value )
      {
        read( StringLiteralKey( key ), value );
      }

      virtual void read( const Key& key, Sequence& value ) { }

      // string
      void read( const char* key, string& value )
      {
        read( StringLiteralKey( key ), value );
      }

      virtual void read( const Key& key, string& value ) { }

      // Unsigned integers
      uint8_t read_uint8( const char* key )
      {
        return read_uint8( StringLiteralKey( key ) );
      }

      virtual uint8_t read_uint8( const Key& key )
      {
        return read_int8( key );
      }

      uint16_t read_uint16( const char* key )
      {
        return read_uint16( StringLiteralKey( key ) );
      }

      virtual uint16_t read_uint16( const Key& key )
      {
        return static_cast<uint16_t>( read_int16( key ) );
      }

      uint32_t read_uint32( const char* key )
      {
        return read_uint32( StringLiteralKey( key ) );
      }

      virtual uint32_t read_uint32( const Key& key )
      {
        return static_cast<uint32_t>( read_int32( key ) );
      }

      void read( const char* key, uint64_t& value )
      {
        read( StringLiteralKey( key ), value );
      }

      virtual void read( const Key& key, uint64_t& value )
      {
        read( key, reinterpret_cast<int64_t&>( value ) );
      }
    };


    class BufferedMarshaller : public Marshaller
    {
    public:
      BufferedMarshaller()
      {
        buffers = new Buffers;
      }

      virtual ~BufferedMarshaller()
      {
        Buffers::dec_ref( *buffers );
      }

      Buffers& get_buffers() const { return *buffers; }

      // Marshaller
      virtual void write( const Key&, Buffer& value )
      { 
        buffers->push_back( value );
      }

      virtual void write( const Key&, Buffers& value )
      {
        buffers->extend( value );
      }

      virtual void write( const Key&, char* value, size_t value_len )
      {
        buffers->push_back( value, value_len );
      }

      virtual void write( const Key&, const char* value, size_t value_len )
      {
        buffers->push_back( value, value_len );
      }

    private:
      Buffers* buffers;
    };


    class XDRMarshaller : public BufferedMarshaller
    {
    public:
      XDRMarshaller()
      {
        buffer = new StringBuffer;
      }

      XDRMarshaller( Buffer& buffer )
        : buffer( &buffer.inc_ref() )
      { }

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
      void write( const Key& key, bool value )
      {
        write( key );
        write( value );
      }

      void write( bool value )
      {
        write( value ? 1 : 0 );
      }

      void write( const Key& key, Buffer& value )
      {
        write( key );
        write( value );
      }

      void write( Buffer& value )
      {
        buffer->put( static_cast<char*>( value ), value.size() );
      }

      void write( const Key& key, double value )
      {
        write( key );
        write( value );
      }

      void write( double value )
      {
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

      void write( const Key& key, float value )
      {
        write( key );
        write( value );
      }

      void write( float value )
      {
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

      void write( const Key& key, int32_t value )
      {
        write( key );
        write( value );
      }

      void write( int32_t value )
      {
        value = htonl( value );
        buffer->put( &value, sizeof( value ) );
      }

      void write( const Key& key, int64_t value )
      {
        write( key );
        write( value );
      }

      void write( int64_t value )
      {
        value = htonll( value );
        buffer->put( &value, sizeof( value ) );
      }

      void write( const Key& key, const Map& value )
      {
        write( key );
        write( value );
      }

      void write( const Map& value )
      {
        write( static_cast<int32_t>( value.get_size() ) );
        in_map_stack.push_back( true );
        value.marshal( *this );
        in_map_stack.pop_back();
      }

      void write( const Key& key, const Sequence& value )
      {
        write( key );
        write( value );
      }

      void write( const Sequence& value )
      {
        write( static_cast<int32_t>( value.get_size() ) );
        value.marshal( *this );
      }

      void write( const Key& key, const MarshallableObject& value )
      {
        write( key );
        write( value );
      }

      void write( const MarshallableObject& value )
      {
        value.marshal( *this );
      }

      void write( const Key& key, char* value, size_t value_len )
      {
        write( key );
        write( value, value_len );
      }

      void write( const Key& key, const char* value, size_t value_len )
      {
        write( key );
        write( value, value_len );
      }

      void write( const char* value, size_t value_len )
      {
        write( static_cast<int32_t>( value_len ) );

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

      void write( const Key& key, uint32_t value )
      {
        write( key );
        write( value );
      }

      void write( uint32_t value )
      {
        write( static_cast<int32_t>( value ) );
      }

      void write( const Key& key, uint64_t value )
      {
        write( key );
        write( value );
      }

      void write( uint64_t value )
      {
        write( static_cast<int64_t>( value ) );
      }

    protected:
      void write( const Key& key )
      {
        if ( !in_map_stack.empty() && in_map_stack.back() )
        {
          Key::Type key_type = key.get_type();
          if ( key_type == Key::TYPE_DOUBLE )
            write( static_cast<const DoubleKey&>( key ) );
          else if ( key_type == Key::TYPE_INT32 )
            write( static_cast<const Int32Key&>( key ) );
          else if ( key_type == Key::TYPE_INT64 )
            write( static_cast<const Int64Key&>( key ) );
          else if ( key_type == Key::TYPE_STRING )
            write( static_cast<const StringKey&>( key ) );
          else if ( key_type == Key::TYPE_STRING_LITERAL )
            write( static_cast<const StringLiteralKey&>( key ) );
          else
            DebugBreak();
        }
      }

    private:
      Buffer* buffer;
      vector<bool> in_map_stack;
    };


    class XDRUnmarshaller : public Unmarshaller
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
      bool read_bool( const Key& ) { return read_bool(); }
      bool read_bool() { return read_int32() == 1; }

      void read( const Key&, Buffer& value ) { read( value ); }
      void read( Buffer& value )
      {
        size_t size = read_int32();
        if ( value.capacity() - value.size() < size ) DebugBreak();
        read( static_cast<void*>( value ), size );
        value.resize( size );
        if ( size % 4 != 0 )
        {
          char zeros[3];
          read( zeros, 4 - ( size % 4 ) );
        }
      }

      void read( const Key&, double& value ) { read( value ); }
      double read_double() { double value; read( value ); return value; }
      void read( double& value )
      {
        uint64_t uint64_value;
        read( &uint64_value, sizeof( uint64_value ) );
        uint64_value = ntohll( uint64_value );
        memcpy_s
        (
          &value,
          sizeof( value ),
          &uint64_value,
          sizeof( uint64_value )
        );
      }

      float read_float( const Key& ) { return read_float(); }
      float read_float()
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

      int32_t read_int32( const Key& ) { return read_int32(); }
      int32_t read_int32()
      {
        int32_t value;
        read( &value, sizeof( value ) );
        return ntohl( value );
      }

      void read( const Key&, int64_t& value ) { read( value ); }
      int64_t read_int64() { int64_t value; read( value ); return value; }
      void read( int64_t& value )
      {
        read( &value, sizeof( value ) );
        value = ntohll( value );
      }

      Key* read( Key::Type key_type )
      {
        if ( key_type == Key::TYPE_DOUBLE )
          return new DoubleKey( read_double() );
        else if ( key_type == Key::TYPE_INT32 )
          return new Int32Key( read_int32() );
        else if ( key_type == Key::TYPE_INT64 )
          return new Int64Key( read_int64() );
        else if ( key_type == Key::TYPE_STRING )
          { StringKey* key = new StringKey; read( *key ); return key; }
        else
          { DebugBreak(); return NULL; }
      }

      void read( const Key&, Map& value ) { read( value ); }
      void read( Map& value )
      {
        size_t size = read_int32();
        for ( size_t i = 0; i < size; i++ )
          value.unmarshal( *this );
      }

      void read( const Key&, MarshallableObject& value )
      {
        read( value );
      }

      void read( MarshallableObject& value )
      {
        value.unmarshal( *this );
      }

      void read( const Key&, Sequence& value ) { read( value ); }
      void read( Sequence& value )
      {
        size_t size = read_int32();
        if ( size <= UINT16_MAX )
          for ( size_t i = 0; i < size; i++ )
            value.unmarshal( *this );
      }

      void read( const Key&, string& value ) { read( value ); }
      void read( string& value )
      {
        size_t size = read_int32();
        if ( size > 0 && size < UINT16_MAX )
        {
          size_t padded_size = size % 4;
          if ( padded_size == 0 )
            padded_size = size;
          else
            padded_size = size + 4 - padded_size;

          value.resize( padded_size );
          read( const_cast<char*>( value.c_str() ), padded_size );
          value.resize( size );
        }
      }

      uint32_t read_uint32() { return static_cast<uint32_t>( read_int32() ); }
      void read( uint64_t& value ) { read( reinterpret_cast<int64_t&>( value ) ); }

    private:
      void read( void* buffer, size_t buffer_len )
      {
        this->buffer.get( buffer, buffer_len );
      }

    private:
      Buffer& buffer;
    };
  };
};

#endif
