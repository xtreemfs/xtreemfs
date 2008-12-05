/* Copyright (c) 2007, 2008  Matthias Hess, Erich Focht
   This file is part of XtreemFS.

   XtreemFS is part of XtreemOS, a Linux-based Grid Operating
   System, see <http://www.xtreemos.eu> for more details. The
   XtreemOS project has been developed with the financial support
   of the European Commission's IST program under contract
   #FP6-033576.

   XtreemFS is free software: you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation, either version 2 of
   the License, or (at your option) any later version.

   XtreemFS is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of 
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with XtreemFS.  If not, see <http://www.gnu.org/licenses/>.
 */


/*
   C Interface: lock_utils

   Description: 
   All spin locking are abstracted away from their actual implementation
   like pthread_spinlocks. This way it is possible to do logging of spins.
   This done if source files are compiled with 'SPIN_LOG' defined.
   If - in addition - ITAC is enabled, spin locks are also shown in the
   time line.
   If no logging is required, there is no overhead because the generic
   'spin_{un}lock' is just an alias for 'pthread_spin_{un}lock'.

   Author: Matthias Hess <mhess@laptopmh.stg.hpce.tld>, (C) 2008

   Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_LOCK_UTILS_H__
#define __XTRFS_LOCK_UTILS_H__

#include <pthread.h>

#include "logger.h"

/* ITAC needs some handles assoicated with the spin. So we define out
   own spinlock_t here. If ITAC is disabled this is simply an alias
   to 'pthread_spinlock_t'
 */

#ifndef ITAC_SPIN
typedef pthread_spinlock_t spinlock_t;
#else
typedef struct {
	pthread_spinlock_t lock;
	int lock_handle;
	int locked_handle;
	int unlock_handle;
} spinlock_t;
#endif

#ifndef ITAC_SPIN
#define spin_init(x,y) pthread_spin_init((x),(y))
#define spin_destroy(x) pthread_spin_destroy((x))
#else
int itac_spin_init(spinlock_t *lock, int shared);
int itac_spin_destroy(spinlock_t *lock);
#define spin_init(x,y) itac_spin_init((x),(y))
#define spin_destroy(x) itac_spin_destroy((x))
#endif

#ifndef ITAC_SPIN
#define __spin_lock(x) pthread_spin_lock((x))
#define __spin_unlock(x) pthread_spin_unlock((x))
#else
extern int itac_spin_lock(spinlock_t *lock);
extern int itac_spin_unlock(spinlock_t *lock);
#define __spin_lock(x) itac_spin_lock((x))
#define __spin_unlock(x) itac_spin_unlock((x))
#endif

extern int log_spin_lock(spinlock_t *lock, char *funcname);
extern int log_spin_unlock(spinlock_t *lock, char *funcname);

#ifdef SPIN_LOG
#define spin_lock(x) log_spin_lock((x), (char *)__FUNCTION__)
#define spin_unlock(x) log_spin_unlock((x), (char *)__FUNCTION__)
#else
#define spin_lock(x) __spin_lock(x)
#define spin_unlock(x) __spin_unlock(x)
#endif

/* Generic spin logging enabled. This can be used even if 'SPIN_LOG'
   is not defined. This way it is possible to log only specific
   spins without the rest of it. This requires extra care by the
   programmer as all logs of locks need the corresposnding log of
   unlock. */

#define spin_lock_log(x) log_spin_lock((x), (char *)__FUNCTION__)
#define spin_unlock_log(x) log_spin_unlock((x), (char *)__FUNCTION__)

typedef pthread_mutex_t mutex_t;

#define mutex_init(x,y) pthread_mutex_init((x),(y))
#define mutex_lock(x) pthread_mutex_lock((x))
#define mutex_unlock(x) pthread_mutex_unlock((x))

#endif
