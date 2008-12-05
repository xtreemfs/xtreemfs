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
*  C Implementation: xcap_inv
*
* Description: 
*
* Rationale:
* Like the file inventory takes care of file related entries
* the xcap inventory takes care of managing x-capabilities,
* especially the automated renewal of such capabilities.
* Renewal can take place automatically (because the client
* gets a signal that a capability is about to time out) or
* manually because an OSD says it must be renewed because of
* file size changes.
* X-capabilities are not on a per user file basis but on a
* per file basis, ie. for all replica and user files of a 
* specific file there is only one x-capability, right now.
* Therefore user files should reference the same x-cap and
* not copies of the same one and the xcap inventory is repsonsible
* of keeping track of these x-caps.
* Handling of renewal should be done exclusively in the inventory,
* but every other thread should be allowed to initiate renewals.
*
* Author: Matthias Hess <mhess@hpce.nec.com>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <signal.h>
#include <sys/time.h>
#include <time.h>

#include <pthread.h>

#include "mrc_request.h"
#include "mrc.h"
#include "xcap_inv.h"
#include "list.h"
#include "lock_utils.h"
#include "logger.h"

#ifdef _WIN32
static int renew_interval = 2;
#else
struct itimerval timval, oldtimeval;
struct sigaction newact, oldact;
#endif
pthread_t timer_thread;

sigset_t newset, oldset;

int xcap_inv_renew(struct xcap_inv *xr, long duration, int lock);


/**
 * This thread waits for SIGALRM and renews some capabilities and
 * resets the timer.
 */
void *xcap_inv_thread(void *data)
{
	struct xcap_inv *xi = (struct xcap_inv *)data;
	sigset_t newset;
	int signum;
	time_t now;
	
	while(1) {
#ifdef _WIN32
		if(renew_interval == 0)
			break;
		sleep(renew_interval);
#else
		sigemptyset(&newset);
		sigaddset(&newset, SIGALRM);\
		if (sigwait(&newset, &signum)) {
			err_msg("Error in sigwait.");
			continue;
		}

		if(signum != SIGALRM) {
			err_msg("Unhandled signal.\n");
			continue;
		}
#endif
		now = time(NULL);
		// dbg_msg("ALARM seen. Starting to work on x-caps.\n");
		// dbg_msg("Diff between last and now: %ld\n", now - xi->last_signal);
		spin_lock(&xi->lock);
		xcap_inv_renew(xi, 5, 0);
		spin_unlock(&xi->lock);

		xi->last_signal = now;
	}
	return NULL;
}

void xcap_inv_sighandler(int signum)
{
	dbg_msg("ALARM received. This should not happen!\n");
}

void xcap_inv_init(struct xcap_inv *xi)
{
	INIT_LIST_HEAD(&xi->renewals);
	spin_init(&xi->lock, PTHREAD_PROCESS_PRIVATE);
	
	xi->earliest_renewal         = 0L;
	xi->last_signal              = 0L;
	xi->last_renewal_duration    = 0L;
	xi->longest_renewal_duration = 0L;

#ifndef _WIN32
#if 1
	memset((void *)&newact, 0, sizeof(newact));
	sigemptyset(&newact.sa_mask);
	newact.sa_handler = xcap_inv_sighandler;
	sigaction(SIGALRM, &newact, &oldact);
#endif

	/* In the beginning let the timer interrupt occur every twenty seconds.
	   This will be changed later once we receive the first xcaps. */
	timval.it_interval.tv_sec = 2;
	timval.it_interval.tv_usec = 0;
	timval.it_value.tv_sec = 2;
	timval.it_value.tv_usec = 0;
#endif

	xi->last_signal = time(NULL);
	
	pthread_create(&timer_thread, NULL, xcap_inv_thread, (void *)xi);
#ifndef _WIN32
	setitimer(ITIMER_REAL, &timval, &oldtimeval);
#endif
}

void xcap_inv_clear(struct xcap_inv *xi)
{
	struct list_head *iter;
	struct xcap *xcap;
	
	// pthread_join(&timer_thread, NULL);
#ifdef _WIN32
	renew_interval = 0;
#else
	timval.it_value.tv_sec  = 0;
	timval.it_value.tv_usec = 0;
	setitimer(ITIMER_REAL, &timval, &oldtimeval);
#endif
	
	spin_lock(&xi->lock);
	if (list_empty(&xi->renewals))
		dbg_msg("Renewal list is empty.\n");
	
	while(!list_empty(&xi->renewals)) {
		iter = xi->renewals.next;
		list_del(iter);
		xcap = container_of(iter, struct xcap, head);
		dbg_msg("Deleting xcap for file '%s'\n", xcap->fileID);
		xcap_delete(xcap);
	}
	spin_unlock(&xi->lock);
}

void xcap_inv_destroy(struct xcap_inv *xi)
{
	xcap_inv_clear(xi);
	spin_destroy(&xi->lock);
	free(xi);
}


/**
 * Add an xcapability to the queue.
 * This function already sorts the queue to have the most urgent
 * entries at the front.
 * This function is not locking the inventory. */
int xcap_inv_add(struct xcap_inv *xi, struct xcap *xc, int lock)
{
	int err = 0;
	struct list_head *iter = NULL, *iter2;
	struct xcap *xcl;

	if (lock)
		spin_lock(&xi->lock);

	list_for_each_safe(iter, iter2, &xi->renewals) {
		xcl = container_of(iter, struct xcap, head);
		if(xcl->expires > xc->expires) {
			list_add(&xc->head, iter->prev);
			break;
		}
	}
	if (iter == &xi->renewals) {
		list_add_tail(&xc->head, &xi->renewals);
		dbg_msg("Xcap added to inventory.\n");
	}
			
	if (lock)
		spin_unlock(&xi->lock);

	return err;
}

int xcap_inv_remove(struct xcap_inv *xi, struct xcap *xc)
{
	int err = 0;
	
	spin_lock(&xi->lock);
	list_del(&xc->head);
	spin_unlock(&xi->lock);

	xcap_delete(xc);
	
	return err;
}

int xcap_inv_requeue(struct xcap_inv *xi, struct xcap *xc, int lock)
{
	int err = 0;
	/* struct list_head *iter; */

	if (lock)
		spin_lock(&xi->lock);
	list_del(&xc->head);
	INIT_LIST_HEAD(&xc->head);
	if (lock)
		spin_unlock(&xi->lock);
	err = xcap_inv_add(xi, xc, lock);

	return err;
}

/**
 * Find a xcap by the corresponding file id.
 */
struct xcap *xcap_inv_find(struct xcap_inv *xi, char *fileid)
{
	struct xcap *rv = NULL;
	struct list_head *iter;

	spin_lock(&xi->lock);
	list_for_each(iter, &xi->renewals) {
		struct xcap *xc;
		xc = container_of(iter, struct xcap, head);
		if (!strcmp(xc->fileID, fileid)) {
			rv = xc;
			break;
		}
	}
	spin_unlock(&xi->lock);

	return rv;
}


/**
 * Renew a x-capability immediately.
 * If 'new_size' is negative, we do not update the file size but only the
 * capability.
 */
int xcap_inv_renew_immediate(struct xcap_inv *xi,
			     char *fileid, struct xcap *xc,
			     off_t new_size, int epoch, int order,
			     int lock)
{
	int err = 0;
	struct req *req;
	struct MRC_Req_renew_resp resp;
	struct creds c;
	time_t now = time(NULL);
	time_t start_renewal, end_renewal;
	
	start_renewal = time(NULL);
	dbg_msg("Renewing with size %ld\n", new_size);

	c.uid  = xc->creds.uid;
	c.gid  = xc->creds.gid;
	c.pid  = xc->creds.pid;
	c.guid = xc->creds.guid;
	c.ggid = xc->creds.ggid;
	
	if (xc->expires - now < 0) {
		err_msg("ERROR: Trying to renew an expired x-capability.\n");
	}
	
	req = MRC_Request__renew(&err, &resp, xc, new_size, epoch, &c, NULL, order, 1);
	if (req)
		err = execute_req(mrccomm_wq, req);
	else
		err = ENOMEM;

	if (err)
		goto finish;
	
	xcap_inv_requeue(xi, xc, lock);
	end_renewal = time(NULL);
	
	xi->last_renewal_duration = end_renewal - start_renewal;
	if (xi->last_renewal_duration > xi->longest_renewal_duration)
		xi->longest_renewal_duration = xi->last_renewal_duration;

finish:
	return err;
}

/**
 * Renew capabilities from the capability list.
 * This function renews all capabilities that fall within the
 * time period indicated by 'duration' of the current time.
 */
int xcap_inv_renew(struct xcap_inv *xi, long duration, int lock)
{
	int err = 0;
	struct list_head *iter, *iter2;
	struct xcap *xc;
	time_t now = time(NULL), diff;

	list_for_each_safe(iter, iter2, &xi->renewals) {
		xc = container_of(iter, struct xcap, head);
		spin_lock(&xc->lock);
		diff = xc->expires - now;
		// dbg_msg("xcap expires: %ld (%ld)\n", diff, xc->expires);
		if (diff < 0) {
			spin_unlock(&xc->lock);
			err_msg("X-Capability expired!\n");
			dbg_msg("xcap expires: %ld (%ld). Now %ld\n", diff, xc->expires, now);
		} else {
			if (diff < duration) {
				dbg_msg("Should renew the capability.\n");
				xcap_inv_renew_immediate(xi, NULL, xc, -1, -1, 0, lock);
				spin_unlock(&xc->lock);
			} else {
				spin_unlock(&xc->lock);
				break;
			}
		}
	}

	return err;
}


void xcap_inv_print(struct xcap_inv *xi)
{	
	struct list_head *iter;
	struct xcap *xc;
		
	list_for_each(iter, &xi->renewals) {
		xc = container_of(iter, struct xcap, head);
		xcap_print(xc);	
	}
}
