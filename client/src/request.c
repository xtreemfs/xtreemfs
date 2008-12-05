/* Copyright (c) 2007, 2008  Erich Focht, Matthias Hess
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


/**
 * Request related functions. 
 *
 *
 * @author: Erich Focht <efocht@hpce.nec.com>, Matthias Hess
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>
#include <errno.h>

#include "request.h"
#include "workqueue.h"
#include "lock_utils.h"
#include "logger.h"

char *reqstate_name[] = {
	"NEW",
	"PROCESSING",
	"WAIT_CHLD",
	"FINISHED",
	"CHLD_ERR",
	"TIMEOUT",
	"ERROR"
};

#ifdef _WIN32
#include <winsock2.h>
#define ECANCELED WSAECANCELLED
#endif

/**
 * Create a request with payload.
 *
 * @param data the payload: pointer to a structure describing the request
 *        work and parameters
 * @param result storage for the result of the operation described by
 *               the request
 * @return pointer to a new request structure
 */
struct req *req_create(int type, void *data, void *result,
		       struct req *parent)
{
	struct req *r;

	r = (struct req *) malloc(sizeof(struct req));
	if (r) {
		INIT_LIST_HEAD(&r->q);
		INIT_LIST_HEAD(&r->rlist);
		r->wq          = NULL;

		r->id          = NULL;
		r->type        = type;
		r->state       = REQ_STATE_NEW;
		r->data        = data;
		r->result      = result;
		r->error       = 0;

		r->parent      = parent;
		atomic_set(&r->use_count, 1);
		atomic_set(&r->active_children, 0);
		atomic_set(&r->wait, 0);

		pthread_cond_init(&r->waitc, NULL);
		pthread_mutex_init(&r->waitm, NULL);

		r->del_data    = NULL;
		r->del_result  = NULL;

		dbg_msg("created req %p\n",r);
	}

	return r;
}

/**
 * Mark a request and its children as ABORTED, i.e. set it to ERROR state
 * and set the error value to ECANCELED.
 */
void req_abort(struct req *req)
{
	req->state = REQ_STATE_ERROR;
	req->error = ECANCELED;
}

/**
 * Wait until a request finishes.
 *
 * Put the current task to sleep and wait to be woken up.
 * The request use_count is decremented here with req_put(), but could actually
 * be taken out and used by the caller of req_wait() and give it the chance to
 * use the request and data inside it.
 *
 * The request must be there when we enter this routine. It means we must
 * enqueue the request with nonzero "wait".
 *
 * @param r the request we want to wait for
 * @return 0 if all went well, req->error (non-zero) if error occured.
 */
int req_wait(struct req *r)
{
	int res = 0;
	struct req *or = r;

	dbg_msg("going to sleep on request %p\n", r);
	atomic_inc(&r->use_count); /* increment use_count for each waiter */
	dbg_msg("Use count: %d\n", atomic_read(&r->use_count));

	if (pthread_mutex_lock(&r->waitm)) {
		err_msg("req_wait pthread_mutex_lock failed, tid %p\n",
			pthread_self());
		//pthread_exit(NULL);
		return -1;
	}

	if (REQ_STATE_DONE(r)) {
		dbg_msg("Not going to sleep!\n");
		goto out;
	}

	dbg_msg("Really going to sleep on request %p\n", r);

	if (pthread_cond_wait(&r->waitc, &r->waitm)) {
		err_msg("something went wrong while waking up waiters\n");
		res = -1;
	}

out:
	if (r->error)
		res = r->error;
	else if (r->state == REQ_STATE_ERROR)
		res = -1;

	pthread_mutex_unlock(&r->waitm);
	req_put(r);
	dbg_msg("returned from sleep on request %p\n", or);
	return res;
}

void req_del(struct req *r)
{
	dbg_msg("Deleting req %p\n", r);
	req_state_set(r, REQ_STATE_DELETED);
	if (r->del_data && r->data)
		r->del_data(r->data);
	if (r->del_result && r->result)
		r->del_result(r->result);
	free(r->id);
	free(r);
}

/**
 * Decrement use count of a request, if it reaches zero, free the request.
 * Don't free the request if we are in an error condition. (Why actually not?)
 *
 * @param r request
 * @return 0 if no error condition, error code otherwise
 */
void req_put(struct req *r)
{
	dbg_msg("Request use count %d with %d children\n",
		atomic_read(&r->use_count),
		atomic_read(&r->active_children));

	if (atomic_dec_return(&r->use_count) == 0) {
		dbg_msg("Destroying request (active children: %d)\n",
			atomic_read(&r->active_children));
		// dequeue from workqueue, if still enqueued
		if (r->wq)
			workqueue_deq_req(r->wq, r);
		req_del(r);
	} else {
		dbg_msg("Not destroying request %p (%d)\n",
			r, atomic_read(&r->use_count));
	}
}

/**
 * Wake up threads waiting for one request.
 *
 * @param r request on which threads are waiting.
 */
void wake_req_waiters(struct req *r)
{
	pthread_mutex_lock(&r->waitm); // TODO: is this really needed?
	trace_msg("waking up waiters on request %p\n", r);
	pthread_cond_broadcast(&r->waitc);
	pthread_mutex_unlock(&r->waitm); // TODO: is this really needed?
}

/**
 * Print request info
 *
 * @param req Request to print
 */
void req_print(struct req *req)
{
	info_msg("Req addr:         %p\n", req);
	info_msg("Req type:         %d\n", req->type);
	info_msg("Req state:        %d\n", req->state);
	info_msg("Req num children: %d\n", atomic_read(&req->active_children));
	info_msg("Req use count:    %d\n", atomic_read(&req->use_count));
	info_msg("Req parent:       %p\n", req->parent);
	info_msg("- - -\n");
}
