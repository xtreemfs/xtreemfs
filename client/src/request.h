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


#ifndef __XTRFS_REQUEST_H__
#define __XTRFS_REQUEST_H__

/**
  \file request.h
  Common request header.
  
  Rationale:
  The general architecture of the client is built as a set of worker components
  which receive their work requests in FIFO queues. This architecture is
  scalable and extensible. New components can be added in between the old ones
  without the need to rewrite or adapt the old components. Each component can
  have several worker threads and thus be scaled to provide higher throughput.

  A request's life cycle:
   - A work request is added to the tail of the input queue of a particular
   component.
   - The worker component's thread (or one of its threads) picks a request
   from the head of the input queue, chains it into the "wait-for-completion"
   queue (the output queue) and processes the request. During processing
   children requests can be created. These are linked into the original
   request's structure.
   - The work to be done by the worker thread is defined in the *data element
   of the request.
   - When the request is finished, it is dequeued from the output queue. A
   request is considered unfinished if it has outstanding child requests.
   - A request specific callback is invoked. It's tasks are:
     - Unlink the request from the "wait-for-completion" queue.
     - Finish up the request's work.
     - Unlink the request from the parent's children queue.
     - Delete the request.
     - If the parent request's state is "WAIT_CHLD" and the children
     queue is empty, execute the parent request's callback.

  Callbacks are invoked with the request structure as argument. This allow them
  to dequeue themselves and access the request data. The request data is
  usually a pointer to a structure containing the parameters/arguments required
  for the request work as well as space for the results.

  Deleting a request does not imply deleting the data. Sometimes the request
  data is a result that is needed (for example a file object, it will be added
  to the file-objects belonging to an open-file). If the data needs to be
  deleted, this is the task of the request specific callback. Being request
  (type) specific it will have the full information on how to handle the
  deletion correctly.

  The linking between request parents and children is needed in order to handle
  in simple way the waiting for finishing children as well as propagating
  errors and request aborts.

  The described structure allows for asynchronous communication between worker
  components, request chains and handles parent-child interrequest
  relationships in reasonably simple and generic way. It can handle 

  An example for a request chain is:
  [file] -> fileop -> file_obj -> stripe_obj -> osd_comm


  @author: Erich Focht <efocht@hpce.nec.com>
*/

#include <stdlib.h>
#include <pthread.h>

#include "kernel_substitutes.h"
#include "list.h"
#include "lock_utils.h"

#define REQ_STATE_DONE(x) \
        (((x)->state != REQ_STATE_NEW) && \
         ((x)->state != REQ_STATE_PROCESSING) && \
         ((x)->state != REQ_STATE_WAIT_CHLD))
 
/* Forward declaration */
struct work_queue;

/**
   \enum req_state
   Request states.
   During its processing cycle a request goes through a few states.
   - NEW : request is NEW, just queued into the incoming queue
   - PROCESSING : request is being processed (and usually in the output queue)
   - WAIT_CHLD : waiting for the child requests to be finished
   - FINISHED : request has been finished without errors
   - CHLD_ERR : children had an error
   - ERROR  : this request is lost and has produced an error
   - TIMEOUT ? Shouldn't this be implicit?
   - DEQUEUING ? Is this needed?

   In PROCESSING state the callback can't be executed. Before a worker thread
   is finished with a request it needs to check for the existence of children.
   If children exist, the request will be switched to the state WAIT_CHLD and
   the callback will be executed by the last child who dequeues itself from the
   children queue. If no children exist, the request will go to CALLBACK mode
   and its callback will be executed by the worker thread.

   Some states could be avoided by dequeueing the request while processing it.
   Rethink the states!
*/
enum req_state {
	REQ_STATE_NEW = 0,
	REQ_STATE_QUEUED,
	REQ_STATE_PROCESSING,
	REQ_STATE_WAIT_CHLD,
	REQ_STATE_FINISHED,
	REQ_STATE_CHLD_ERR,
	REQ_STATE_TIMEOUT,
	REQ_STATE_ERROR,
	REQ_STATE_DELETED	/* For debugging purposes only */
};


extern char *reqstate_name[];

/**
 * Request types
 *
 * The work for one request can be splitted in several pieces,
 * the splitting being motivated by blocking operations boundaries.
 * We are sorting the request types in order to be able to check the
 * validity of requests easilly in the stage(s).
 */

#define REQ_BLOCK_MASK			0x00000100
#define REQ_IDX_MASK			0x000000ff

#define REQ_BLOCKING			0x00000100

#define REQ_FILERW_READ			(REQ_BLOCKING + 0)
#define REQ_FILERW_WRITE		(REQ_BLOCKING + 1)
#define REQ_FILERW_FOBJ_DONE		(2)

#define REQ_FOBJ_READ			(3)
#define REQ_FOBJ_WRITE			(4)
#define REQ_FOBJ_SOBJ_DONE		(5)

#define REQ_TRANSL_READ			(0)
#define REQ_TRANSL_WRITE		(1)

#define REQ_SOBJ_READ			(0)
#define REQ_SOBJ_WRITE			(1)
#define REQ_SOBJ_READ_CACHE		(2)
#define REQ_SOBJ_WRITE_CACHE		(3)

#define REQ_HTTP_READ_HEAD		(0)
#define REQ_HTTP_READ_FINISH		(1)

#define REQ_MRC_EXEC_FUNC		(REQ_BLOCKING + 0)
#define REQ_MRC_EXEC_FUNC_FINISH	(1)


/**
 * Request structure common header.
 *
 * Common header for every request. The lock needs to be held for changing the
 * request state or manipulating the children list. The data pointer contains
 * the payload of a request. The work which needs to be done for the request
 * is defined by the work-queue in which the request is queued (i.e. the
 * handler or stage it is targetted for. Details for the work to be done are
 * in the payload.
 */

struct req {
	/* Managing request queues */
	struct list_head q;	/*!< list head for queueing a request */
	struct list_head rlist;	/*!< list head for setting request aside
				  while processing */
	struct work_queue *wq;	/*!< To which queue does this request
				     belong? Used to delete a request
				     properly. */

	char *id;		/*!< For tracing request IDs can be used.
				     if id == NULL it will be neglectedt. */
	/* Context and state of request */
	int type;		/*!< request type */
	enum req_state state;
	void *data;		/*!< payload of the request, usually input
				 data and results data for all children */
	void *result;		/*!< pointer to result data of THIS request */
	int error;		/*!< error info from THIS request */
	struct req *parent;	/*!< pointer to parent request */

	atomic_t active_children;	/*!< number of active children */
	atomic_t use_count;		/*!< right now only for waiter */
	atomic_t wait;

	/* Make it possible to wait for this request */
	mutex_t waitm;		/*!< mutex for waiting threads */
	pthread_cond_t waitc;	/*!< condition variable for waiting threads */

	void (*del_data)(void *data);		/*!< If set indicates that the request
						     owns the data and can delete it
						     once the request is finished. */
	void (*del_result)(void *result);	/*!< If set indicates that the request
						     outer response structure may be
						     deleted when the request finishes. */
};

/**
 * Create a request with payload.
 *
 * @param data the payload: pointer to a structure describing the request
 *        work and parameters
 * @param callback callback running when the request finishes
 * @param parent parent request (if any)
 * @return pointer to a new request structure
 */
extern struct req *req_create(int type, void *data, void *response,
			      struct req *parent);

extern void req_put(struct req *req);

/**
 * Set a request's state.
 *
 */
static inline void req_state_set(struct req *req, enum req_state state)
{
	req->state = state;
}

/**
 * Get a request's state.
 *
 */
static inline enum req_state req_state(struct req *req)
{
	return req->state;
}

/**
 * Mark a request and its children as ABORTED.
 *
 * This routine is called recursively. The parent's lock is held
 * during the recursion, so make sure this doesn't deadlock!
 */
void req_abort(struct req *req);

/**
 * Wait until a request finishes.
 *
 * Put the current task to sleep and wait to be woken up.
 *
 * There is a risk that the request is not there when getting into this
 * routine. In order to find it we'd need to know the workqueue, take the
 * work queue locks and scan the input and output queues for the request.
 * Not sure this is ever needed (more than one waiter for the req).
 *
 * @param r the request we want to wait for
 * @return 0 if all went well, req->error (non-zero) if error occured.
 */
extern int req_wait(struct req *r);

extern void req_del(struct req *r);

/**
 * Wake up threads waiting for one request.
 *
 * @param r request on which threads are waiting.
 */
extern void wake_req_waiters(struct req *r);

/**
 * Output information contained in a request
 *
 * @param req Request to print
 */
void req_print(struct req *req);


#endif // __XTRFS_REQUEST_H__
