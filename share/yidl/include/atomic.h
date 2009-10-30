
// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ATOMIC_H_
#define _ATOMIC_H_

#ifdef _WIN32
#include "msstdint.h"
#else
#include <stdint.h>
#endif

#if defined(_WIN64)
typedef __int64 atomic_t;
extern "C"
{
	atomic_t _InterlockedCompareExchange64( volatile atomic_t* current_value, atomic_t new_value, atomic_t old_value );
	atomic_t _InterlockedIncrement64( volatile atomic_t* );
	atomic_t _InterlockedDecrement64( volatile atomic_t* );
}
#elif defined(_WIN32)
typedef long atomic_t;
extern "C"
{
  __declspec( dllimport ) atomic_t __stdcall InterlockedCompareExchange( volatile atomic_t* current_value, atomic_t new_value, atomic_t old_value );
  __declspec( dllimport ) atomic_t __stdcall InterlockedIncrement( volatile atomic_t* );
  __declspec( dllimport ) atomic_t __stdcall InterlockedDecrement( volatile atomic_t* );
}
#else
#if defined(__LLP64__) || defined(__LP64__)
typedef int64_t atomic_t;
#else
typedef int32_t atomic_t;
#endif
#if defined(__sun)
#include <atomic.h>
#elif defined(__arm__)
// gcc atomic builtins are not defined on ARM
#elif defined(__GNUC__) && ( ( __GNUC__ == 4 && __GNUC_MINOR__ >= 1 ) || __GNUC__ > 4 )
#define __HAVE_GNUC_ATOMIC_BUILTINS 1
#endif
#endif

static inline atomic_t atomic_cas( volatile atomic_t* current_value, atomic_t new_value, atomic_t old_value )
{
#if defined(_WIN64)
  return _InterlockedCompareExchange64( current_value, new_value, old_value );
#elif defined(_WIN32)
  return InterlockedCompareExchange( current_value, new_value, old_value );
#elif defined(__sun)
  return atomic_cas_32( current_value, old_value, new_value );
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
  asm volatile(	"lock\n"
          "cmpxchgl %1,%2\n"
        : "=a" ( prev )
              : "r" ( new_value ), "m" ( *current_value ) , "0" ( old_value )
              : "memory"
            );
  return prev;
#elif defined(__ppc__)
  atomic_t prev;
  asm volatile(	"					\n\
          1:	ldarx   %0,0,%2 \n\
          cmpd    0,%0,%3 \n\
          bne     2f		\n\
          stdcx.  %4,0,%2 \n\
          bne-    1b		\n\
          sync\n"
          "2:"
        : "=&r" ( prev ), "=m" ( *current_value )
              : "r" ( current_value ), "r" ( old_value ), "r" ( new_value ), "m" ( *current_value )
              : "cc", "memory"
            );
  return prev;
#elif defined(__x86_64__)
  atomic_t prev;
  asm volatile(	"lock\n"
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
  return atomic_dec_32_nv( current_value );
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
  return atomic_inc_32_nv( current_value );
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

#endif
