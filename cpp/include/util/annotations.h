/*
 * Copyright (c) 2013 by Felix Hupfeld.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifdef __clang__
// TODO(fhupfeld): add annotations to synchronization primitives.
// Taken from http://www.mail-archive.com/linuxkernelnewbies@googlegroups.com/msg01455.html.
#define GUARDED_BY(x)          __attribute__ ((guarded_by(x)))
#define GUARDED_VAR            __attribute__ ((guarded))
#define PT_GUARDED_BY(x)       __attribute__ ((point_to_guarded_by(x)))
#define PT_GUARDED_VAR         __attribute__ ((point_to_guarded))
#define ACQUIRED_AFTER(...)    __attribute__ ((acquired_after(__VA_ARGS__)))
#define ACQUIRED_BEFORE(...)   __attribute__ ((acquired_before(__VA_ARGS__)))
#define LOCKABLE               __attribute__ ((lockable))
#define SCOPED_LOCKABLE        __attribute__ ((scoped_lockable))
#define EXCLUSIVE_LOCK_FUNCTION(...)    __attribute__ ((exclusive_lock(__VA_ARGS__)))
#define SHARED_LOCK_FUNCTION(...)       __attribute__ ((shared_lock(__VA_ARGS__)))
#define EXCLUSIVE_TRYLOCK_FUNCTION(...) __attribute__ ((exclusive_trylock(__VA_ARGS__)))
#define SHARED_TRYLOCK_FUNCTION(...)    __attribute__ ((shared_trylock(__VA_ARGS__)))
#define UNLOCK_FUNCTION(...)            __attribute__ ((unlock(__VA_ARGS__)))
#define LOCK_RETURNED(x)                __attribute__ ((lock_returned(x)))
#define LOCKS_EXCLUDED(...)             __attribute__ ((locks_excluded(__VA_ARGS__)))
#define EXCLUSIVE_LOCKS_REQUIRED(...)   __attribute__ ((exclusive_locks_required(__VA_ARGS__)))
#define SHARED_LOCKS_REQUIRED(...)      __attribute__ ((shared_locks_required(__VA_ARGS__)))
#define NO_THREAD_SAFETY_ANALYSIS       __attribute__ ((no_thread_safety_analysis))
#else
#define GUARDED_BY(x)
#define GUARDED_VAR
#define PT_GUARDED_BY(x)
#define PT_GUARDED_VAR
#define ACQUIRED_AFTER(...)
#define ACQUIRED_BEFORE(...)
#define LOCKABLE
#define SCOPED_LOCKABLE
#define EXCLUSIVE_LOCK_FUNCTION(...)
#define SHARED_LOCK_FUNCTION(...)
#define EXCLUSIVE_TRYLOCK_FUNCTION(...)
#define SHARED_TRYLOCK_FUNCTION(...)
#define UNLOCK_FUNCTION(...)
#define LOCK_RETURNED(x)
#define LOCKS_EXCLUDED(...)
#define EXCLUSIVE_LOCKS_REQUIRED(...)
#define SHARED_LOCKS_REQUIRED(...)
#define NO_THREAD_SAFETY_ANALYSIS
#endif
