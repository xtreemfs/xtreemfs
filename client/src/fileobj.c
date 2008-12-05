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
 * File object processing stage
 *
 * In this stage file object requests are processed. They generate file
 * stripe requests. File objects are mapped to stripes and OSDs. Striping
 * patterns other than RAID0 can generate parity object requests...
 * 
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
#include "file.h"
#include "filerw.h"
#include "fobj_cache.h"
#include "fileobj.h"
#include "stripe.h"
#include "logger.h"

#ifdef ITAC
#include <xtreemfs_itac.h>
#endif

struct work_queue *fobj_wq;
struct work_queue *client_wq;

static void fobj_handle(struct req *req);
static void fobj_sobj_done(struct req *req);

char *fobjop_name[] = {
	[REQ_FOBJ_READ]  = "FOBJ_READ",
	[REQ_FOBJ_WRITE] = "FOBJ_WRITE",
	[REQ_FOBJ_SOBJ_DONE] = "FOBJ_SOBJ_DONE" };

/**
 * Create a response data structure
 *
 */
static struct fobj_result *fobj_result_new()
{
	struct fobj_result *rv = NULL;

	rv = (struct fobj_result *)malloc(sizeof(struct fobj_result));
	if (rv) {
		rv->num_bytes = -1;
		rv->new_size = -1;
		rv->epoch = -1;
	}
	return rv;
}

static void fobj_result_delete(void *r)
{
	free(r);
}

static void fobj_payload_delete(void *p)
{
	free(p);
}

static struct fobj_sobj_done_data *
fobj_sobj_done_data_new(int sobj_id, loff_t offset, size_t size, size_t num_bytes,
			size_t new_size, long epoch)
{
	struct fobj_sobj_done_data *rv = NULL;

	rv = (struct fobj_sobj_done_data *)malloc(sizeof(struct fobj_sobj_done_data));
	if (rv) {
		rv->sobj_id   = sobj_id;
		rv->offset    = offset;
		rv->size      = size;
		rv->num_bytes = num_bytes;
		rv->new_size  = new_size;
		rv->epoch     = epoch;
	}
	return rv;
}

static void fobj_sobj_done_data_delete(void *fsd)
{
	free(fsd);
}


/**
 * create a fobj request and queue it.
 *
 */
struct req *fobj_req_create(int type, int fobj_id, loff_t offset,
			    size_t size, void *buffer, struct user_file *fd,
			    int use_cache, struct req *parent)
{
	struct req *req = NULL;
	struct fobj_payload *p;
	struct fobj_result *result = NULL;

	p = (struct fobj_payload *)malloc(sizeof(struct fobj_payload));
	if (!p) {
		err_msg("Failed to allocate fobj payload struct\n");
		goto out;
	}
	p->fobj_id = fobj_id;
	p->offset = offset;
	p->size = size;
	p->rsize = 0;
	p->buffer = buffer;
	p->fd = fd;
	p->use_cache = use_cache;
	p->new_cache_entry = 0;
	p->fce = NULL;

	result = fobj_result_new();

	req = req_create(type, (void *)p, (void *)result, parent);
	req->del_data = fobj_payload_delete;
	req->del_result = fobj_result_delete;

 out:
	return req;
}

/**
 * Create a fobj_sobj_done request and queue it.
 * Called from a finishing sobj request.
 *
 */
struct req *fobj_sobj_done_req(int sobj_id,
			       loff_t offset, size_t size, size_t num_bytes,
			       size_t new_size, long epoch,
			       struct req *parent)
{
	struct req *req = NULL;
	struct fobj_sobj_done_data *data = NULL;

	data = fobj_sobj_done_data_new(sobj_id, offset, size, num_bytes,
				       new_size, epoch);
	if (!data)
		goto out;

	req = req_create(REQ_FOBJ_SOBJ_DONE, (void *)data, NULL, parent);
	req->del_data = fobj_sobj_done_data_delete;
	req->del_result = NULL;

out:
	return req;
}

/**
 * Main file obj worker task.
 *
 * Create stripe object requests or serve request from fobj cache (once
 * this will be there...).
 *
 */
static void fobj_handle(struct req *req)
{
	struct fobj_payload *p = (struct fobj_payload *)req->data;
	struct file_replica *replica = p->fd->file;
	struct fobj_cache *fc = replica->fobj_cache;
	struct req *sr;
	int osd_id, err = 0;
	int order;
	int type;

#ifdef ITAC
	VT_begin(itac_fobj_handle_hdl);
#endif
	dbg_msg("fobj_handle start, state=%d, fobj_id=%d\n",
		req_state(req), p->fobj_id);
	
	if (req_state(req) == REQ_STATE_ERROR)
		goto out;

	p->new_cache_entry = 0;

	// check striping policy of file
	switch (replica->sp.id) {
	case SPOL_RAID_0:
		// map id to OSD
		osd_id = p->fobj_id % replica->sp.width;

		// EF: here we should check whether the osd_id is valid and
		// the corresponding OSD is alive. For this we'll need a
		// mechanism to track OSD states (based on the noops and
		// results of other requests...?)

		if (req->type == REQ_FOBJ_READ)
			type = REQ_SOBJ_READ;
		if (req->type == REQ_FOBJ_WRITE)
			type = REQ_SOBJ_WRITE;

		dbg_msg("Creating sobj request with type 0x%x\n", type);
		sr = sobj_req_create(type, p->fobj_id,
				     p->offset, p->size, p->buffer,
				     replica->osd_ids[osd_id],
				     p->fd, req);
		if (sr) {
			atomic_inc(&req->active_children);
			submit_request(sobj_wq, sr);
		}

		break;
	case SPOL_RAID_5:
		// not supported, yet
		err_msg("Unable to deal with RAID5 files!\n");
		err = -EINVAL;
		break;
	default:
		err_msg("Unsupported striping policy %d!\n", replica->sp.id);
		err = -EINVAL;
		break;
	}
 out:
	dbg_msg("fobj_handle end, err=%d\n", err);
#ifdef ITAC
	VT_end(itac_fobj_handle_hdl);
#endif
}

/**
 * Finish a sobj request.
 *
 * The request to finish is passed as the parent of the given request 'req'.
 * When this function is called, it 'owns' the parent request and can
 * freely access all its data elements. All other finish requests by other
 * children are handled sequentially!
 *
 * The parent request of 'req' will always be a request of type
 * FOBJ_{READ|WRITE} and the parent's parent is a FILERW_{READ|WRITE}
 */
static void fobj_sobj_done(struct req *req)
{
	struct fobj_sobj_done_data *d = (struct fobj_sobj_done_data *)req->data;

	struct req *fobj_req = req->parent;
	struct fobj_result  *pr = (struct fobj_result *)fobj_req->result;

	dbg_msg("sobj done, fobj=%p id=%d state=%d\n",
		fobj_req, d->sobj_id, req_state(req));

	/* Check the processed bytes of the stripe object. */
	if (d->size != d->num_bytes) {

	}

	/* Check new size */
	if ((d->epoch > pr->epoch) ||
	    ((d->epoch == pr->epoch) && (d->new_size > pr->new_size))) {
		pr->epoch = d->epoch;
		pr->new_size = d->new_size;
	}
 
	dbg_msg("Fobj-req %p has %d child(ren)\n", fobj_req,
		atomic_read(&fobj_req->active_children));

	/* If this is the last child, we must finish the parent fobj req */
	if (atomic_dec_return(&fobj_req->active_children) == 0) {
		struct fobj_payload *fp = (struct fobj_payload *)fobj_req->data;
		struct req *answer = filerw_fobj_done_req(fp->fobj_id,
							  fp->offset,
							  d->num_bytes,
							  pr->new_size,
							  pr->epoch,
							  fobj_req->parent);
		submit_request(filerw_wq, answer);
		finish_request(fobj_req);
	}
	/* work for this request is done, throw it away */
	finish_request(req);
}

void fobj_init(void)
{
	fobj_wq = workqueue_new("fobj", 1);
	if (!fobj_wq) {
		err_msg("Failed to create fobj workqueue!\n");
		exit(1);
	}
	workqueue_add_func(fobj_wq, REQ_FOBJ_READ,      fobj_handle);
	workqueue_add_func(fobj_wq, REQ_FOBJ_WRITE,     fobj_handle);
	workqueue_add_func(fobj_wq, REQ_FOBJ_SOBJ_DONE, fobj_sobj_done);
	trace_msg("'fobj_wq' successfully created (%X)\n", fobj_wq);
}

void fobj_close(void)
{
	workqueue_del(fobj_wq);
}

/**
 * Dump info on one fobj request to log.
 *
 * @param req pointer to request to be shown
 */
void print_fobj_info(struct req *req)
{
	struct fobj_payload *p = (struct fobj_payload *)req->data;

	info_msg("fobj_id=%d state=%s op=%s offs=%ld buffer=%p fileid=%s\n",
		 p->fobj_id, reqstate_name[req->state], frwop_name[p->op],
		 p->offset, p->buffer, UFILEID(p->fd));
}
