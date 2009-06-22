// Copyright 2003-2009 Minor Gordon, with original implementations and ideas contributed by Felix Hupfeld.
// This source comes from the Yield project. It is licensed under the GPLv2 (see COPYING for terms and conditions).

#ifndef _ATOMIC_H_
#define _ATOMIC_H_

#define __STDC_LIMIT_MACROS
#ifdef _WIN32
#include "msstdint.h"
#else
#include <stdint.h>
#endif

#ifdef _WIN32
extern "C"
{
  __declspec( dllimport ) long __stdcall InterlockedCompareExchange( volatile long* current_value, long new_value, long old_value );
  __declspec( dllimport ) long long __stdcall InterlockedCompareExchange64( volatile long long* current_value, long long new_value, long long old_value );
  __declspec( dllimport ) long __stdcall InterlockedIncrement( volatile long* );
  __declspec( dllimport ) long __stdcall InterlockedDecrement( volatile long* );
}
#else
#if defined(__GNUC__) && ( ( __GNUC__ == 4 && __GNUC_MINOR__ >= 2 ) || __GNUC__ > 4 )
#define __HAVE_GNUC_ATOMIC_OPS_INTRINSICS 1
#elif defined(__sun)
#include <atomic.h>
#endif
#endif


static inline int32_t atomic_cas( volatile int32_t* current_value, int32_t new_value, int32_t old_value )
{
#if defined(_WIN32)
  return InterlockedCompareExchange( reinterpret_cast<volatile long*>( current_value ), new_value, old_value );
#elif defined(__HAVE_GNUC_ATOMIC_OPS_INTRINSICS)
  return __sync_val_compare_and_swap( current_value, old_value, new_value );
#elif defined(__sun)
  return atomic_cas_32( current_value, old_value, new_value );
#elif defined(__i386__) || defined(__x86_64__)
  int32_t prev;
  asm volatile(	"lock\n"
          "cmpxchgl %1,%2\n"
        : "=a" ( prev )
              : "r" ( new_value ), "m" ( *current_value ) , "0" ( old_value )
              : "memory"
            );
  return prev;
#elif defined(__ppc__)
  int32_t prev;
  asm volatile(	"					\n\
          1:	lwarx   %0,0,%2 \n\
          cmpw    0,%0,%3 \n\
          bne     2f		\n\
          stwcx.  %4,0,%2 \n\
          bne-    1b		\n\
          sync\n"
          "2:"
        : "=&r" ( prev ), "=m" ( *current_value )
              : "r" ( current_value ), "r" ( old_value ), "r" ( new_value ), "m" ( *current_value )
              : "cc", "memory"
            );
  return prev;
#else
#error
#endif
}

static inline int64_t atomic_cas( volatile int64_t* current_value, int64_t new_value, int64_t old_value )
{
#if defined(_WIN32)
  return InterlockedCompareExchange64( current_value, new_value, old_value );
#elif defined(__HAVE_GNUC_ATOMIC_OPS_INTRINSICS)
  return __sync_val_compare_and_swap( current_value, old_value, new_value );
#elif defined(__sun)
  return atomic_cas_64( current_value, old_value, new_value );
#elif defined(__x86_64__)
  int64_t prev;
  asm volatile(	"lock\n"
          "cmpxchgq %1,%2\n"
        : "=a" ( prev )
              : "r" ( new_value ), "m" ( *current_value ) , "0" ( old_value )
              : "memory"
            );
  return prev;
#elif defined(__ppc__)
  int64_t prev;
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
#else
  // 32-bit systems
  *((int*)0) = 0xabadcafe;
  return 0;
#endif
}

static inline int32_t atomic_dec( volatile int32_t* current_value )
{
#if defined(_WIN32)
  return InterlockedDecrement( reinterpret_cast<volatile long*>( current_value ) );
#elif defined(__HAVE_GNUC_ATOMIC_OPS_INTRINSICS)
  return __sync_sub_and_fetch( current_value, 1 );
#elif defined(__sun)
  return atomic_dec_32_nv( current_value );
#else
  int32_t old_value, new_value;

  do
  {
    old_value = *current_value;
#ifdef _DEBUG
    if ( old_value == 0 )  { *((int*)0) = 0xabadcafe; }
#endif
    new_value = old_value - 1;
  }
  while ( atomic_cas( current_value, new_value, old_value ) != old_value );

  return new_value;
#endif
}

static inline int32_t atomic_inc( volatile int32_t* current_value )
{
#if defined(_WIN32)
  return InterlockedIncrement( reinterpret_cast<volatile long*>( current_value ) );
#elif defined(__HAVE_GNUC_ATOMIC_OPS_INTRINSICS)
  return __sync_add_and_fetch( current_value, 1 );
#elif defined(__sun)
  return atomic_inc_32_nv( current_value );
#else
  int32_t old_value, new_value;

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
