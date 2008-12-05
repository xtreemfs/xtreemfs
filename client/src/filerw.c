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
 * File R/W stage
 *
 * Read/write requests on the full file are splitted into file object
 * requests and submitted to the next processing stage.
 *
 * @author Erich Focht
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include "request.h"
#include "workqueue.h"
#include "filerw.h"
#include "fileops.h"
#include "file.h"
#include "logger.h"

#ifdef ITAC
#include <xtreemfs_itac.h>
#endif

// #include "fileobj.h"

char *frwop_name[] = {
	[REQ_FILERW_READ]  = "FILERW_READ",
	[REQ_FILERW_WRITE] = "FILERW_WRITE",
	[REQ_FILERW_FOBJ_DONE] = "FILERW_FOBJ_DONE" };

struct work_queue *filerw_wq;
struct work_queue *client_wq;

static void filerw_handle(struct req *req);
static void filerw_finish(struct req *req);
static void filerw_fobj_done(struct req *req);

static struct filerw_fobj_done_data *
filerw_fobj_done_data_new(int fobj_id,
			  int offset,
			  int num_bytes,
			  ssize_t new_size,
			  long epoch)
{
	struct filerw_fobj_done_data *rv = NULL;

	rv = (struct filerw_fobj_done_data *)malloc(sizeof(struct filerw_fobj_done_data));
	if (rv) {
		rv->fobj_id   = fobj_id;
		rv->offset    = offset;
		rv->num_bytes = num_bytes;
		rv->new_size  = new_size;
		rv->epoch     = epoch;
	}

	return rv;
}

static void filerw_fobj_done_data_delete(void *data)
{
	free(data);
}

static void filerw_payload_delete(void *p)
{
	free(p);
}

static void filerw_response_delete(void *r)
{
	free(r);
}


/**
 * Create a filerw request.
 *
 * @param type Type of request. Can be either
 *             REQ_FILERW_READ{_FINISH} for read operations or
 *             REQ_FILERW_WRITE{_FINISH} for write operations.
 * @param offset Offset in file
 * @param size   Number of bytes to either read or write.
 * @param buffer Buffer to write to (read operation) or read from
 *               (write op.)
 * @param fd     User file to operate on
 * @param bytes  Pointer to the response data.
 * @param parent Parent request, NULL if no parent.
 */
struct req *filerw_req_create(int type,
			      loff_t offset, size_t size,
			      void *buffer, struct user_file *fd,
			      size_t *bytes,
			      struct req *parent)
{
	struct req *req = NULL;
	struct filerw_payload *p;
	struct filerw_response *r;
	int err = 0;

	p = (struct filerw_payload *)malloc(sizeof(struct filerw_payload));
	if (!p) {
		err_msg("Failed to allocate payload struct\n");
		err = 1;
		goto out;
	}
	p->offset = offset;
	p->size = size;
	p->buffer = buffer;
	p->fd = fd;

	r = (struct filerw_response *)malloc(sizeof(struct filerw_response));
	if (!r) {
		err_msg("Failed to allocate response structure.\n");
		err = 2;
		goto out;
	}
	r->bytes_ptr = bytes;
	r->new_size  = -1;
	r->epoch     = -1;

	req = req_create(type, (void *)p, (void *)r, parent);
	if (!req) {
		err_msg("Could not create filerw request!\n");
		goto out;
	}
	dbg_msg("***** Creating filerw request %p with size %ld and offset %lld *****\n",
		req, p->size, p->offset);

	req->del_data = filerw_payload_delete;
	req->del_result = filerw_response_delete;

 out:
	return req;
}

/**
 * Create a filerw_fobj_done request.
 *
 */
struct req *filerw_fobj_done_req(int fobj_id, int offset, int num_bytes,
				 ssize_t new_size, long epoch,
				 struct req *parent)
{
	struct req *req = NULL;
	struct filerw_fobj_done_data *data = NULL;

	data = filerw_fobj_done_data_new(fobj_id, offset, num_bytes,
					 new_size, epoch);
	if (!data)
		goto out;

	req = req_create(REQ_FILERW_FOBJ_DONE, (void *)data, NULL, parent);
	if (!req) {
		err_msg("Could not create filerw_fobj_done request!\n");
		goto out;
	}
	dbg_msg("**** Creating filerw_fobj_done request %p ****", req);
	req->del_data = filerw_fobj_done_data_delete;
	req->del_result = NULL;

out:
	return req;
}

/**
 * File R/W handler for the first part of the work.
 *
 * - split request into per fileobject subrequests
 * - submit the per fileobject requests to the fileobj_wq
 *
 * This is a single threaded queue, so we don't worry about locking
 * and order of operations.
 */
static void filerw_handle(struct req *req)
{
	struct filerw_payload *p = (struct filerw_payload *)req->data;
	struct file_replica *replica = p->fd->file;
	size_t stripe_size = replica->sp.stripe_size;
	size_t osize;
	size_t count = p->size;
	loff_t head = p->offset;
	loff_t ohead;
	char *obuff = (char *)p->buffer;
	int fobj, err = 0;
	int num_objs;
	int fobj_type;

#ifdef ITAC
	VT_begin(itac_filerw_handle_hdl);
#endif

	dbg_msg("filerw_handle start\n");
	dbg_msg("Request %p use count: %d\n", req, atomic_read(&req->use_count));

	fobj = (int)(head / stripe_size);
	num_objs = 0;

	while (count > 0 && req_state(req) != REQ_STATE_ERROR) {
		struct req *r;

		ohead = head % stripe_size;
		osize = min(stripe_size - ohead, count);

		dbg_msg("head=%lld  fobj=%d   ohead=%lld  osize=%ld,  count=%ld\n", head, fobj, ohead, osize, count);
		//
		// submit fobj child request!
		//
		if (req->type == REQ_FILERW_READ)
			fobj_type = REQ_FOBJ_READ;
		if (req->type == REQ_FILERW_WRITE)
			fobj_type = REQ_FOBJ_WRITE;

		dbg_msg("Creating fobj request with type 0x%x\n", fobj_type);
		r =  fobj_req_create(fobj_type, fobj, ohead, osize, obuff,
				     p->fd, 0, req);
		if (r) {
			atomic_inc(&req->active_children);
			submit_request(fobj_wq, r);
		}
		else
			err = -ENOMEM;

		head  += osize;
		count -= osize;
		obuff += osize;
		fobj++;
		num_objs++;
	}
	dbg_msg("Req %p after children creation: Use count = %d  Active children = %d\n",
		req, atomic_read(&req->use_count),
		atomic_read(&req->active_children));

	/* err_msg("Created %d fobjs\n", num_objs); */
	dbg_msg("filerw_handle end, err=%d\n", err);

#ifdef ITAC
	VT_end(itac_filerw_handle_hdl);
#endif
}

/**
 * File R/W handler for the second part of the work. This is the continuation
 * of the filerw_handle work.
 *
 * Each finishing child request will generate one of these.
 *
 * This is a single threaded queue, so we don't worry about locking
 * and order of operations.
 */
void filerw_fobj_done(struct req *req)
{
	struct filerw_fobj_done_data *data =
		(struct filerw_fobj_done_data *)req->data;
	struct req *frw_req = req->parent;
	struct filerw_payload *pd = NULL;
	struct filerw_response *resp = NULL;

	if (!frw_req) {
		err_msg("No parent!\n");
		finish_request(req);
		return;
	}

	dbg_msg("fobj child-req of %p finished. Req uc=%d ac=%d\n",
		frw_req, atomic_read(&frw_req->use_count),
		atomic_read(&frw_req->active_children));

	resp = frw_req->result;
	pd   = (struct filerw_payload *)frw_req->data;

	/* Check if we have to update the new size and epoch field of
	   the parents response. */
	if (data->epoch != -1) {
		if (   (data->new_size > resp->new_size && data->epoch == resp->epoch)
		    || (data->epoch > resp->epoch)) {
			resp->epoch    = data->epoch;
			resp->new_size = data->new_size;
		}
		dbg_msg("Size now: %ld (%ld)\n", (long)resp->new_size, (long)data->new_size);
	}
	*(resp->bytes_ptr) += data->num_bytes;

	/* And now we can finish the original filerw request, if last child */
	if (atomic_dec_return(&frw_req->active_children) == 0) {
		struct filerw_payload  *data = (struct filerw_payload *)frw_req->data;
		struct filerw_response *resp = (struct filerw_response *)frw_req->result;
		struct fileid_struct *fileid = data->fd->file->fileid_s;

		dbg_msg("Finishing filerw i/o request %p\n", frw_req);
		dbg_msg("Epoch:    %ld\n", resp->epoch);
		dbg_msg("New size: %d\n", (int)resp->new_size);
		dbg_msg("Filerw-req use count: %d\n", atomic_read(&frw_req->use_count));

		if (resp->epoch != -1 && resp->new_size != -1) {
			/* Need a file size update? */
			if ((resp->epoch > fileid->sz.epoch) ||
			    (resp->epoch == fileid->sz.epoch &&
			     resp->new_size > fileid->sz.size)) {
				dbg_msg("Setting new file size %ld (old size %ld).\n",
					resp->new_size, fileid->sz.size);
				fileid->sz.size = resp->new_size;
				/* atomic_set(&fileid->sz.order, data->order); */
				fileid->sz.epoch = resp->epoch;
				/* Indicate that we must send it to the MRC... */
				atomic_set(&fileid->sz.needs_updating, 1);
			}
		}
		finish_request(frw_req);
	} else
		dbg_msg("Req %p still has %d children\n", frw_req,
			atomic_read(&frw_req->active_children));

	/* This request (fobj done) is no longer needed and can be finished. */
	finish_request(req);
}

void filerw_init(void)
{
	/* filerw must be single threaded */
	filerw_wq = workqueue_new("filerw", 1);
	if (!filerw_wq) {
		err_msg("Failed to create filerw workqueue!\n");
		exit(-ENOMEM);
	}
	workqueue_add_func(filerw_wq, REQ_FILERW_READ,  filerw_handle);
	workqueue_add_func(filerw_wq, REQ_FILERW_WRITE, filerw_handle);
	workqueue_add_func(filerw_wq, REQ_FILERW_FOBJ_DONE, filerw_fobj_done);
}

void filerw_close(void)
{
	if (filerw_wq)
		workqueue_del(filerw_wq);
}

/**
 * Dump info on one filerw request to log.
 *
 * @param req pointer to request to be shown
 */
void print_filerw_info(struct req *req)
{
	struct filerw_payload *p = (struct filerw_payload *)req->data;

	info_msg("state=%s offs=%ld size=%ld buffer=%p fileid=%s\n",
		 reqstate_name[req->state],
		 p->offset, p->size, p->buffer, UFILEID(p->fd));
}
