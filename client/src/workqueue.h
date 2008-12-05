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


#ifndef __XTRFS_WORKQUEUE_H__
#define __XTRFS_WORKQUEUE_H__

/**
 * Workqueue header file.
 *
 * Default workqueues embed an input req_queue, an output req_queue
 * and a set of pthreads waiting for work to be done.
 *
 * @author Erich Focht <efocht@hpce.nec.com>
 */

#include <pthread.h>
#include "kernel_substitutes.h"
#include "request.h"

#include "lock_utils.h"


/**
 * Work thread structure for keeping track of threads enrolled in a work queue.
 */
struct work_thread {
	struct list_head tlist;	/*!< list head for threads of a work queue */
	pthread_t id;		/*!< thread id */
	void *data;		/*!< thread specific data */
};

typedef void (*wq_func_t)(struct req *);

/**
 * Work queue structure with input and output queue and associated worker
 * threads.
 *
 * This defines an asynchronously controlled worker stage. Input requests
 * come in the input queue. When processed, they are moved over to the
 * output queue, get their state changed to PROCESSING and the function
 * associated to this work queue is exectuted.
 */
struct work_queue {
	char *name;			/*!< work queue name */
	mutex_t lock;			/*!< protect queue ops */

	struct list_head input;		/*!< input queue */
	struct list_head rlist;		/*!< listhead for setting reqs aside */
	wq_func_t *funcs;
	int maxidx;
	pthread_cond_t wake;		/*!< condition for wakeup or workers */
	struct list_head threads;	/*!< list of enrolled threads */
	int nthreads;			/*!< number of threads for wq */
	int req_types;			/*!< offset for request types processed
					     by this queue (eg. REQ_CLIENT_TYPE)*/
	volatile atomic_t signal_ready;	/*!< ready to signal or broadcast.
					     Volatile is necessary because the
					     compiler optimzes away the wait
					     loop */
	int stopping;
};

/**
 * Create a work queue.
 *
 * @param name the workqueue name
 * @param nthreads number of worker threads for this workqueue
 * @return pointer to workqueue, if successfull, NULL otherwise.
 */
struct work_queue *workqueue_new(char *name, int nthreads);

/**
 * Initialize a work_queue structure.
 *
 * @param wq work queue to be initialized.
 * @return error
 */
int workqueue_init(struct work_queue *wq, char *name, int nthreads);

/**
 * Delete a workqueue structure.
 *
 * Deleting a wq structure should actually never happen. If it happens it is
 * during the init phase and the queues have no members, yet.
 *
 * @param wq the work queue pointer
 */
void workqueue_del(struct work_queue *wq);

/**
 * Stop a workqueue
 *
 * Stop all threads associated with this workqueue.
 *
 * @param wq workqueue to stop
 */
void workqueue_stop(struct work_queue *wq);

/**
 * Add a function to a workqueue
 */
int workqueue_add_func(struct work_queue *wq, int idx, wq_func_t func);

/**
 * Find the thread data structure according to thread id.
 */
static inline
struct work_thread *find_work_thread(struct work_queue *wq, pthread_t id)
{
	struct list_head *lh;
	struct work_thread *found = NULL;

	mutex_lock(&wq->lock);
	list_for_each(lh, &wq->threads) {
		struct work_thread *wt;

		wt = list_entry(lh, struct work_thread, tlist);
		if (wt->id == id) {
			found = wt;
			goto out;
		}
	}
out:
	mutex_unlock(&wq->lock);
	return found;
}

/**
 * queue up a request into the input queue of a workqueue and wake up
 * at least one worker thread to come and pick it.
 *
 * @param req the request to be queued
 * @param wq the target work queue
 */
void submit_request(struct work_queue *wq, struct req *req);

/**
 * Execute a given request in a workqueue.
 *
 * This will be a synchronous execution of the request, ie. when
 * this function returns the request is finished.
 */
int execute_req(struct work_queue *wq, struct req *req);

/**
 * Simply dequeue a request, needed for put_req() only.
 */
void workqueue_deq_req(struct work_queue *wq, struct req *req);

/**
 * Finish a request. This will wake up any waiters for that
 * request.
 *
 * @param req Request to finish
 * @return Error
 */
int finish_request(struct req *req);


/**
 * Put a request in the 'aside' list of a workqueue.
 *
 * @param req request to be set aside.
 * @param wq workqueue of the request
 */
static inline void req_set_aside(struct req *req, struct work_queue *wq)
{
	dbg_msg("Setting req %p aside in wq %p\n", req, wq);
	list_add_tail(&req->rlist, &wq->rlist);
}

/**
 * Print info on queued requests.
 *
 * @param wq workqueue to be shown
 * @param print_req stage specific req display function
 */
void print_req_info(struct work_queue *wq, void (*print_req)(struct req *));

#endif // __XTRFS_WORKQUEUE_H__
