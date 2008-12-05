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
 * Workqueue-related functions.
 *
 * @author Erich Focht <efocht@hpce.nec.com>
 */

#include <stdio.h>
#include <errno.h>
#include <signal.h>
#include <string.h>
#include <pthread.h>

#include "request.h"
#include "workqueue.h"
#include "list.h"
#include "lock_utils.h"
#include "logger.h"

#ifdef ITAC
#include <VT.h>
#endif

/**
 * Pick first request on a queue, dequeue and return it.
 *
 * Lock will have already been taken
 *
 * @param wq the work queue
 * @return a pointer to a req structure or NULL
 */
static struct req *get_one_req(struct work_queue *wq)
{
	struct req *req = NULL;
	struct list_head *req_head = NULL;

	dbg_msg("Get request from '%s'\n", wq->name);

	if (!list_empty(&wq->input)) {
		req_head = wq->input.next;
		req = list_entry(req_head, struct req, q);
		list_del(req_head);
		req->wq = NULL;
		req->state = REQ_STATE_PROCESSING;
		INIT_LIST_HEAD(&req->q);
		list_add_tail(&req->q, &wq->rlist);
		req->wq = wq;
	} else {
		dbg_msg("Queue empty.\n");
	}

	return req;
}

/**
 * Workqueue thread main loop.
 *
 * This function contains the logic of request processing:
 * - pick req from input queue, if empty: sleep
 * - set state of req to PROCESSING
 * - enqueue req into in-flight queue (actually just a list)
 * - execute the request function
 * - loop
 */
static void *workqueue_main_loop(void *data)
{
	struct work_queue *wq = (struct work_queue *)data;
	struct req *req = NULL;

	dbg_msg("Starting main loop of work queue '%s'.\n",
		wq->name);

	while (!wq->stopping) {

		pthread_mutex_lock(&wq->lock);

		/* Before going to sleep eventually check if the queue is not
		   stopped and if there are requests available. In the latter case
		   we do not have to sleep at all but can proceed directly to
		   request handling. */

		if (wq->stopping)
			goto process_req;

		req = get_one_req(wq);
		if (req)
			goto process_req;

		/* Go to sleep until someone wakes us up. */
		atomic_inc(&wq->signal_ready);
		dbg_msg("worker thread of wq '%s' going to sleep\n", wq->name);
		pthread_cond_wait(&wq->wake, &wq->lock);
		dbg_msg("worker thread of wq '%s' woke up\n", wq->name);

		/* pick one req from the input queue */
		if (!wq->stopping)
			req = get_one_req(wq);
		else
			req = NULL;

		/* Now we are no longer ready to receive signals. */
		atomic_dec(&wq->signal_ready);

	process_req:
		pthread_mutex_unlock(&wq->lock);

		if (req) {
			int type = req->type;
			wq_func_t function;

			/* Find the function that is to be executed for the request */
			if (((function = wq->funcs[type & REQ_IDX_MASK]) == NULL)
			    || ((type & REQ_IDX_MASK) > wq->maxidx)) {
				err_msg("Found illegal req type %d in wq %s\n",
					type, wq->name);
				abort();
			}

			/* For debugging purposes we could 'set the request aside'.
			   But if all goes well we would always have a pointer to
			   the request so we will not lose it. */

			/* Execute the work associated with the request */
			function(req);

			/* Indicate that we do not own the request any longer. */
			req = NULL;
		}
	}
	dbg_msg("Worker thread finished.\n");

	return NULL;
}

/**
 * Add a thread as worker to a workqueue.
 *
 * The thread's task is to execute generic_worker as long as it lives.
 *
 * @param wq the target work queue
 * @return zero if successful, non-zero if failed.
 */
static int
workqueue_add_thread(struct work_queue *wq)
{
	struct work_thread *t;
	int err = 0;

	t = (struct work_thread *) malloc(sizeof(struct work_thread));
	if (!t) {
		err_msg("no memory for work_thread structure in %s.\n",
			wq->name);
		return ENOMEM;
	}
	INIT_LIST_HEAD(&t->tlist);
	t->data = NULL;

	pthread_mutex_lock(&wq->lock); /* Prevent the newly created thread from
					  prematurely getting new requests. */
	err = pthread_create(&t->id, NULL, workqueue_main_loop, (void *)wq);
	if (err) {
		free(t);
		err_msg("pthread create failed in %s\n", wq->name);
		return err;
	}
	list_add_tail(&t->tlist, &wq->threads);
	wq->nthreads++;
	pthread_mutex_unlock(&wq->lock);
	dbg_msg("Added thread %p to wq %s\n", t->id, wq->name);
	return err;
}

/**
 * Initialize a work_queue structure.
 *
 * @param wq work queue to be initialized.
 * @return error
 */
int workqueue_init(struct work_queue *wq, char *name, int nthreads)
{
	int err = 0;
	int i;

	memset((void *)wq, 0, sizeof(struct work_queue));
	
	wq->name = strdup(name);
	INIT_LIST_HEAD(&wq->input);
	INIT_LIST_HEAD(&wq->rlist);

	/* Before adding functions we must set default values */
	wq->maxidx = 0;
	wq->funcs = NULL;

	pthread_mutex_init(&wq->lock, NULL);
	pthread_cond_init(&wq->wake, NULL);
	INIT_LIST_HEAD(&wq->threads);
	atomic_set(&wq->signal_ready, 0);
	wq->stopping = 0;
	for (i = 0; i < nthreads; i++) {
		int rc = workqueue_add_thread(wq);
		if (rc) {
			err_msg("failed to create %d threads for "
				"wq %s\n", nthreads, name);
			/* This is a serious failure and can only happen
			   at initialization time. */
			err = 1;
		}
	}	
	return err;
}

/**
 * Create a work queue.
 * TODO for client-ng!!!
 *
 * @param name the workqueue name
 * @param function the function to be applied to each request
 * @param nthreads number of worker threads for this workqueue
 * @return pointer to workqueue, if successfull, NULL otherwise.
 */
struct work_queue *workqueue_new(char *name, int nthreads)
{
	struct work_queue *wq;

	wq = (struct work_queue *) malloc(sizeof(struct work_queue));
	if (wq && workqueue_init(wq, name, nthreads)) {
		err_msg("work queue malloc failed: wq %s\n", name);
		workqueue_del(wq);
		wq = NULL;
	}
	return wq;
}

/**
 * Delete allocated memory from a work queue.
 *
 * @param wq Work queue to be deleted.
 */
void workqueue_del_contents(struct work_queue *wq)
{
#ifdef ITAC
	VT_flush();
#endif
	if (!wq)
		return;

	workqueue_stop(wq);
	free(wq->name);
	free(wq->funcs);
}

/**
 * Delete a workqueue structure.
 *
 * Deleting a wq structure should actually never happen. If it happens it is
 * during the init phase and the queues have no members, yet.
 *
 * @param wq the work queue pointer
 */
void workqueue_del(struct work_queue *wq)
{
	workqueue_del_contents(wq);
	free(wq);
}

/**
 * Stop a workqueue
 *
 * Stop all threads associated with this workqueue.
 *
 * @param wq workqueue to stop
 */
void workqueue_stop(struct work_queue *wq)
{
	struct list_head *iter;
	struct work_thread *wt;

	pthread_mutex_lock(&wq->lock);
	wq->stopping = 1;
	pthread_cond_broadcast(&wq->wake);
	pthread_mutex_unlock(&wq->lock);

	list_for_each(iter, &wq->threads) {
		wt = container_of(iter, struct work_thread, tlist);
		pthread_join(wt->id, NULL);
		
	}
	wq->stopping = 2;
}

/**
 * Add a function to a workqueue
 */
int workqueue_add_func(struct work_queue *wq, int idx, wq_func_t func)
{
	int midx = idx & REQ_IDX_MASK;

	trace_msg("Adding function for idx %d to wq %s\n", idx, wq->name);

	/* First check if array is big enough. Resize it
	   eventually. */
	if (!wq->funcs) {
		wq->funcs = (wq_func_t *) malloc(sizeof(wq_func_t) * 2);
		if (!wq->funcs)
			return -ENOMEM;
	}

	if ((sizeof(wq->funcs) / sizeof(wq_func_t) < midx)) {
		wq->funcs = (wq_func_t *)
			realloc((void *)wq->funcs,
				sizeof(wq_func_t) * (midx + 4));
	}
	wq->funcs[midx] = func;
	if (midx > wq->maxidx)
		wq->maxidx = midx ;

	return 0;
}

/**
 * Simply dequeue a request, needed for put_req() only.
 */
void workqueue_deq_req(struct work_queue *wq, struct req *req)
{
	mutex_lock(&wq->lock);
	list_del(&req->q);
	mutex_unlock(&wq->lock);
}

/**
 * queue up a request and wake up the workqueue worker threads
 * to come and pick it.
 *
 * @param wq Workqueue to use
 * @param req Pointer to request to put in queue. The type of the
 *            request must match that one of the queue.
 */
void submit_request(struct work_queue *wq, struct req *req)
{
	trace_msg("queue req %p to wq %s\n", req, wq->name);

	pthread_mutex_lock(&wq->lock);

	list_add_tail(&req->q, &wq->input);
	req->wq = wq;
	dbg_msg("Added request type: %d\n", req->type);

	req->state = REQ_STATE_QUEUED;

	/* This is a quick hack to ensure that at least one thread is
	   ready to be signalled. Otherwise the broadcast of the signal
	   might be lost and the queue waits forever if there is only
	   one command to be executed (like in the tools).
	*/
	if (atomic_read(&wq->signal_ready) > 0) {
		dbg_msg("Signalling worker(s).\n");
		pthread_cond_signal(&wq->wake);
	}
	pthread_mutex_unlock(&wq->lock);
}

/**
 * Execute a request, i.e. enqueue it and wait for termination.
 *
 * The request must be freed outside this routine, by the caller. Careful
 * locking for avoiding races or missed signals is needed, we use the
 * variable req->use_count for this, that should only be modified while the
 * req->waitm mutex is taken. Typically the finishing part of a request
 * should take the mutex, decrement the use count and send the signal for
 * waking up the waiter(s), then release the mutex.
 *
 */
int execute_req(struct work_queue *wq, struct req *req)
{
	int err = 0;
	int ret = 0;
	struct timespec until;
	int num_waits = 0;

	trace_msg("signalling wq %s to wake up workers\n", wq->name);

	/* after req was enqueued, use_count can only be modified with taken
	   req->waitm. We will have two req_puts: One from 'finish_request'
	   and the other one from this function. So we must make sure the
	   request use count is set appropriately. */
	atomic_inc(&req->use_count);

	/* Use count at this point must be 2. One from request creation
	   and the other one from above inc command. */

	submit_request(wq, req);

	if (req->type & REQ_BLOCK_MASK) {
		pthread_mutex_lock(&req->waitm);

		/* If use count is greater than one, we know someone else
		   uses the request and we must wait. */
		while (req->state != REQ_STATE_FINISHED) { // work not finished, yet
			dbg_msg("Going to sleep on request %p\n", req);

			clock_gettime(CLOCK_REALTIME, &until);
			until.tv_sec += 5;

			atomic_inc(&req->wait);
			/* Worker thread must acquire req->waitm in order to
			   decrement use_count and send wakeup signal. */
			ret = pthread_cond_timedwait(&req->waitc, &req->waitm, &until);
			num_waits++;
			atomic_dec(&req->wait);

			dbg_msg("Current use count: %d\n", atomic_read(&req->use_count));
			dbg_msg("returned from sleep on request %p\n", req);

			if (num_waits > 30) {
				req->error = -1;
				kill(0, SIGUSR1);	/* Print workqueues */
				break;
			}
		}
		pthread_mutex_unlock(&req->waitm);
	}

	err = req->error;
	dbg_msg("Error code %d\n", err);
	dbg_msg("Use count of req %p at exec end: %d\n", req,
		atomic_read(&req->use_count));

	req_put(req);

	return err;
}

/**
 * Finish request.
 *
 * This is a generic function to finish a request. It eventually wakes
 * up all threads that are waiting for the request to be finished.
 *
 * @param req Request to be finished.
 */
int finish_request(struct req *req)
{
	int err = 0;

	dbg_msg("Finish request %p type %d.\n", req, req->type);

	pthread_mutex_lock(&req->waitm);
	req->state = REQ_STATE_FINISHED;

	if (req->type & REQ_BLOCK_MASK) {
		dbg_msg("Act. children: %d\n", atomic_read(&req->active_children));
		dbg_msg("Use count:     %d\n", atomic_read(&req->use_count));
		if (atomic_read(&req->wait)) {
			dbg_msg("Waking up waiting thread on %p.\n", req);
			err = req->error;
			pthread_cond_signal(&req->waitc);
		} else {
			dbg_msg("Not waking anyone for req %p.\n");
		}
	}
	pthread_mutex_unlock(&req->waitm);
	req_put(req);

	return err;
}


/**
 * Print info on queued requests.
 * TODO for client-ng
 *
 * @param wq workqueue to be shown
 * @param print_req stage specific req display function
 */
void print_req_info(struct work_queue *wq, void (*print_req)(struct req *))
{
	struct list_head *lh;
	struct req *r;

	pthread_mutex_lock(&wq->lock);

	info_msg("Workqueue %s input queue:\n", wq->name);
	if (!list_empty(&wq->input)) {
		list_for_each(lh, &wq->input) {
			r = list_entry(lh, struct req, q);
			print_req(r);
		}
	} else
		info_msg("- no requests\n");

	pthread_mutex_unlock(&wq->lock);
}

/**
 * Output requests in queue
 *
 * @param wq Work queue to print
 */
void workqueue_print(struct work_queue *wq)
{
	struct list_head *lh;
	struct req *r;

	pthread_mutex_lock(&wq->lock);

	info_msg("Workqueue '%s' input queue:\n", wq->name);
	if (!list_empty(&wq->input)) {
		list_for_each(lh, &wq->input) {
			r = list_entry(lh, struct req, q);
			req_print(r);
		}
	} else
		info_msg("- no requests\n");

	info_msg("Workqueue '%s' in-flight queue:\n", wq->name);
	if (!list_empty(&wq->rlist)) {
		list_for_each(lh, &wq->rlist) {
			r = list_entry(lh, struct req, q);
			req_print(r);
		}
	} else
		info_msg("- no requests\n");

	pthread_mutex_unlock(&wq->lock);	
}
