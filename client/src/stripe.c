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
 * Stripe objects access helper functions.
 *
 * @author: Erich Focht <efocht@hpce.nec.com>
 */

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <errno.h>

#include <json.h>

#include "xtreemfs.h"
#include "xtreemfs_conf.h"

#ifdef ITAC
#include <xtreemfs_itac.h>
#endif

#include "stripe.h"
#include "stripingpolicy.h"

#include "file.h"
#include "filerw.h"
#include "request.h"
#include "workqueue.h"

#include "osd_manager.h"
#include "osd_proxy.h"
#include "osd_channel.h"

#include "logger.h"
#include "kernel_substitutes.h"


/*
 * Stripe object stage related stuff follows
 */

struct work_queue *sobj_wq;

static void sobj_handle(struct req *req);

/**
 * Start the stripe object stage. It is multi-threaded!
 *
 * We use neon for the http communication and neon is not asynchronous,
 * therefore each thread in this stage is doing work synchonously. When
 * the transfer is finished, the thread finishes.
 */
void sobj_init(int num_threads)
{
	sobj_wq = workqueue_new("sobj", num_threads);
	if (!sobj_wq) {
		err_msg("Failed to create sobj workqueue!\n");
		exit(-ENOMEM);
	}
	workqueue_add_func(sobj_wq, REQ_SOBJ_READ, sobj_handle);
	workqueue_add_func(sobj_wq, REQ_SOBJ_WRITE, sobj_handle);
}

void sobj_close(void)
{
	workqueue_del(sobj_wq);
}

/**
 * Delete sobj request payload
 */
static void sobj_req_payload_delete(void *data)
{
	struct sobj_payload *p = (struct sobj_payload *)data;
	free(p->osd);
	free(p);
}

/**
 * create a sobj request and queue it.
 *
 */
struct req *sobj_req_create(int type, int sobj_id, loff_t offset,
			    size_t size, void *buffer, char *url,
			    struct user_file *fd,
			    struct req *parent)
{
	struct req *req = NULL;
	struct sobj_payload *p;
	void *response = NULL;
	char req_id[256];

	p = (struct sobj_payload *)malloc(sizeof(struct sobj_payload));
	if (!p) {
		err_msg("Failed to allocate sobj payload struct\n");
		goto out;
	}
	p->sobj_id = sobj_id;
	p->offset = offset;
	p->size = size;
	p->rsize = size;
	p->buffer = buffer;
	p->fd = fd;
	p->osd = strdup(url);
	p->order = NEW_FILEID_ORDER(fd);
	p->epoch = GET_EPOCH(fd);

	req = req_create(type, (void *)p, (void *)response, parent);
	req->del_data = sobj_req_payload_delete;
	req->del_result = NULL;

	if (conf.req_ids != 0) {
		snprintf(req_id, 256, "%s-%d", fd->file->fileid_s->fileid, p->sobj_id);
		req->id = strdup(req_id);
	}

out:
	return req;
}
/**
 * Main stripe obj worker task.
 *
 * Parent of the request is still the originating filerw
 * request.
 */
static void sobj_handle(struct req *req)
{
	struct sobj_payload *p = (struct sobj_payload *)req->data;
	struct file_replica *replica = p->fd->file;
	struct fileid_struct *fileid = replica->fileid_s;
	struct OSD_Proxy *op;
	struct OSD_Channel_resp osd_resp = { .new_size = -1,
					     .epoch = -1,    };
	int err = 0;
	struct req *answer = NULL;

	int fobj_type;

#ifdef ITAC
	VT_begin(itac_sobj_handle_hdl);
#endif

	OSD_Channel_resp_init(&osd_resp);

	dbg_msg("sobj_handle req %p start: id=%d \n", req, p->sobj_id);
	dbg_msg("Connecting to OSD '%s'\n", p->osd);
	if (req_state(req) == REQ_STATE_ERROR) {
		dbg_msg("aborting sobj_handler\n");
		goto out;
	}

	op = OSD_Manager_get_proxy(&osd_manager, p->osd, dirservice);
	if (!op) {
		dbg_msg("No OSD proxy found for '%s'\n", p->osd);
		err = -ENOMEM;
		goto out;
	}
	
	switch (req->type) {
	case REQ_SOBJ_READ:
		dbg_msg("Read request with size %ld for req %p\n", p->size, req);
		err = OSD_Proxy_get(op, req->id, p->fd, p->sobj_id, p->offset,
				    p->offset + p->size - 1, p->buffer,
				    &osd_resp);
		dbg_msg("Read request done for req %p\n", req);
		if (osd_resp.length != p->size)
			p->rsize = osd_resp.length;
		break;

	case REQ_SOBJ_WRITE:
		dbg_msg("Write request with size %ld for req %p\n", p->size, req);
		err = OSD_Proxy_put(op, req->id, p->fd, p->sobj_id, p->offset,
				    p->offset + p->size - 1, p->buffer,
				    &osd_resp);
		dbg_msg("Write request done for req %p\n", req);

		/* Check for file size change during write operation */
		if (osd_resp.new_size != -1 &&
		    (osd_resp.err == OSC_ERR_NO_ERROR)) {
			dbg_msg("new filesize = %lld for %s fileid:%s\n",
				osd_resp.new_size, fileid->path,
				fileid->fileid);
			dbg_msg("new epoch = %d\n", osd_resp.epoch);
#if 0
			spin_lock(&fileid->lock);
			/*
			 * Only update filesize if the order of current
			 * request is bigger than the order of the
			 * previous update. This could lead to wrong
			 * file sizes if one request overtakes another
			 * one.
			 * During a truncate epoch the file can only
			 * grow, because the only way to reduce the
			 * file size is via truncate call.
			 */
			if ((p->epoch == osd_resp.epoch &&
			     osd_resp.new_size > fileid->sz.size) ||
			    p->epoch < osd_resp.epoch) {
			    	dbg_msg("Setting new file size: %d\n", osd_resp.new_size);
				fileid->sz.size = osd_resp.new_size;
				atomic_set(&fileid->sz.order, p->order);
				fileid->sz.epoch = osd_resp.epoch;
				/* Indicate that we must send it to the MRC... */
				atomic_set(&fileid->sz.needs_updating, 1);
			} else {
				dbg_msg("ignoring out of order size update\n");
				dbg_msg("Epochs: old: %d  new: %d\n",
					p->epoch, osd_resp.epoch);
				dbg_msg("Sizes:  old: %d  new: %d\n",
					fileid->sz.size, osd_resp.new_size);
			}
			spin_unlock(&fileid->lock);
#endif
		}
		break;

	default:
		err_msg("Unknown operation %d in sobj_handle\n", p->op);
		err = -EINVAL;
		break;
	}

out:
	dbg_msg("Submitting answer for req %p (from %p)\n", req->parent, req);
	answer = fobj_sobj_done_req(p->sobj_id, p->offset, p->size,
				    osd_resp.length, osd_resp.new_size, osd_resp.epoch,
				    req->parent);
	submit_request(fobj_wq, answer);
	if (atomic_read(&req->active_children))
		dbg_msg("There should not be any children.\n");
	if (atomic_read(&req->use_count) != 1)
		dbg_msg("Use count must be 1\n");

	finish_request(req);

#ifdef ITAC
	VT_end(itac_sobj_handle_hdl);
#endif
	dbg_msg("sobj_handle end: OSD channel operation done, err=%d\n", err);
}

/**
 * Dump info on one sobj request to log.
 *
 * @param req pointer to request to be shown
 */
void print_sobj_info(struct req *req)
{
	struct sobj_payload *p = (struct sobj_payload *)req->data;

	info_msg("sobj_id=%d state=%s op=%s osd=%s buffer=%p fileid=%s\n",
		 p->sobj_id, reqstate_name[req->state], frwop_name[p->op],
		 p->osd, p->buffer, UFILEID(p->fd));
}

