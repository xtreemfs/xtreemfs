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
*  C Implementation: lock_utils
*
* Description: 
*
*
* Author: Matthias Hess <mhess@laptopmh.stg.hpce.tld>, (C) 2008
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <pthread.h>

#ifdef ITAC_SPIN
#include <VT.h>
#include <xtreemfs_itac.h>
#endif

#include "lock_utils.h"

#ifdef ITAC_SPIN

extern int itac_spin_class;

int
itac_spin_init(spinlock_t *lock, int shared)
{
	int res;
	char func_name[128];

	res = pthread_spin_init(&lock->lock, shared);

	snprintf(func_name, 128, "lock_%d", lock->lock);
	dbg_msg("%s\n", func_name);
	VT_scopedef(func_name, itac_spin_class, VT_NOSCL, VT_NOSCL, &lock->lock_handle);

	snprintf(func_name, 128, "locked_%d", lock->lock);
	dbg_msg("%s\n", func_name);
	VT_scopedef(func_name, itac_spin_class, VT_NOSCL, VT_NOSCL, &lock->locked_handle);

	snprintf(func_name, 128, "unlock_%d", lock->lock);
	dbg_msg("%s\n", func_name);
	VT_scopedef("unlock", itac_spin_class, VT_NOSCL, VT_NOSCL, &lock->unlock_handle);

	return res;
}

int
itac_spin_destroy(spinlock_t *lock)
{
	return pthread_spin_destroy(&lock->lock);
}

int
itac_spin_lock(spinlock_t *lock)
{
	int res;

	VT_scopebegin(lock->lock_handle, VT_NOSCL, NULL);
	res = pthread_spin_lock(&lock->lock);
	VT_scopeend(lock->lock_handle, 0, VT_NOSCL);
	VT_scopebegin(lock->locked_handle, VT_NOSCL, NULL);

	return res;
}

int
itac_spin_unlock(spinlock_t *lock)
{
	int res;

	VT_scopebegin(lock->unlock_handle, VT_NOSCL, NULL);
	res = pthread_spin_unlock(&lock->lock);
	VT_scopeend(lock->unlock_handle, 0, VT_NOSCL);
	VT_scopeend(lock->locked_handle, 0, VT_NOSCL);

	return res;
}


#endif


int
log_spin_lock(spinlock_t *lock, char *funcname)
{
	int rv;

	log_msg(&logit, LOGLEV_ERROR, funcname,
		"Locking spin %p...\n", lock);
	rv = __spin_lock(lock);
	if (rv) {
		log_msg(&logit, LOGLEV_ERROR, funcname,
			"SPIN LOCK ERROR %d\n", rv);
	}
	log_msg(&logit, LOGLEV_ERROR, funcname,
		"locking done (%p)\n", lock);

	return rv;
}

int
log_spin_unlock(spinlock_t *lock, char *funcname)
{
	int rv;

	log_msg(&logit, LOGLEV_ERROR, funcname,
		"Unlocking spin %p...\n", lock);
	rv = __spin_unlock(lock);
	if (rv) {
		log_msg(&logit, LOGLEV_ERROR, funcname,
			"SPIN UNLOCK ERROR %d\n", rv);
	}
	log_msg(&logit, LOGLEV_ERROR, funcname,
		"unlocking done (%p)\n", lock);

	return rv;
}
