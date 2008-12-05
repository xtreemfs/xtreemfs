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
 * MRC communication stage
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
#include "filerw.h"
#include "lock_utils.h"
#include "logger.h"
#include "mrc_request.h"
#include "mrc_channel.h"
#include "xtreemfs.h"
#include "xtreemfs_prot.h"

static void mrccomm_handle(struct req *req);
static void mrccomm_finish(struct req *req);

struct work_queue *mrccomm_wq;

/**
 * create a mrccomm request and queue it.
 *
 */
struct req *mrccomm_req_create(int *err, struct MRC_payload *p,
			       struct req *parent, int wait)
{
	struct req *req;

	creds_dbg(p->creds);

	req = req_create(REQ_MRC_EXEC_FUNC, (void *)p, NULL, parent);
	if (!req) {
		err_msg("Could not create mrccomm request!\n");
		*err = -ENOMEM;
		goto out;
	}

	req->del_data = MRC_payload_free;
	req->del_result = NULL;
out:
	return req;
}

/**
 * Dump info on one mrccomm request to log.
 *
 * @param req pointer to request to be shown
 */
void print_mrccomm_info(struct req *req)
{
	struct MRC_payload *p = (struct MRC_payload *)req->data;

	info_msg("req %p state=%s cmd=%s\n", req, reqstate_name[req->state],
		 MRCC_exec_map[p->command].fname);
}

/**
 * Main MRC Comm worker task.
 *
 * 
 */
static void mrccomm_handle(struct req *req)
{
	struct MRC_payload *p = (struct MRC_payload *)req->data;
	struct MRC_Channel *mc;
	struct work_thread *wt;

	int err;

	dbg_msg("mrccomm_handle start\n");

	wt = find_work_thread(mrccomm_wq, pthread_self());
	mc = (struct MRC_Channel *)wt->data;

	/* Are these locks necessary? */
	pthread_mutex_lock(&mc->wait_mutex);
	/* EF: is this blocking? */
	err = MRC_Channel_exec_p(mc, p, req);
	pthread_mutex_unlock(&mc->wait_mutex);

	dbg_msg("mrccomm_handle end, err=%d\n", err);
	req->error = err;

	finish_request(req);
}

static void mrccomm_finish(struct req *req)
{
	struct MRC_payload *p = (struct MRC_payload *)req->data;

	dbg_msg("callback for mrccomm start, state=%d\n", req_state(req));
	// generic_cb_head(req);

	// Error handling:
	// Don't know how to handle child errors now, so
	// simply switch to ERROR state if a child failed.
	if (req_state(req) == REQ_STATE_CHLD_ERR) {
		dbg_msg("CHLD_ERR state in mrccomm stage.\n");
		dbg_msg("Switching to ERROR state.\n");
		req->state = REQ_STATE_ERROR;
	}

	// free payload and request structures
	MRC_payload_free(p);

	// generic_cb_tail(req);
	dbg_msg("callback for mrccomm end\n");
}

void mrccomm_init(char *mrc_url, int num_threads)
{
	struct list_head *lh;
	struct prot_list *pl;
	struct prot_list *cl = NULL;
	int do_not_delete = 1;

	trace_msg("initializing mrccomm\n");

	mrccomm_wq = workqueue_new("mrccomm", num_threads);

	if (!mrccomm_wq) {
		err_msg("Failed to create mrccomm workqueue!\n");
		exit(1);
	}

	workqueue_add_func(mrccomm_wq, REQ_MRC_EXEC_FUNC,        mrccomm_handle);
	workqueue_add_func(mrccomm_wq, REQ_MRC_EXEC_FUNC_FINISH, mrccomm_finish);

	/* The tools do not need all the client's framework. So we
	   have to create a client protocol list, eventually */
	if (!client_vers) {
		cl = prot_list_create_client_list();
		do_not_delete = 0;
	} else {
		cl = client_vers;
	}

	mutex_lock(&mrccomm_wq->lock);
	list_for_each(lh, &mrccomm_wq->threads) {
		struct work_thread *wt;
		struct MRC_Channel *mc;

		wt = list_entry(lh, struct work_thread, tlist);
		dbg_msg("Connecting to MRC '%s'\n", mrc_url);
		mc = MRC_Channel_new(mrc_url);
		if (!mc) {
			err_msg("No memory for MRC Channel!\n");
			exit(1);
		}
		pl = prot_get_versions(mc->channel_session, cl);
		if (pl) {
			mc->prot_vers = prot_list_cmp(pl, cl);
			dbg_msg("Common protocol version is '%d'\n", mc->prot_vers);
			prot_list_destroy(pl);
		} else {
			dbg_msg("Cannot get protocol list from MRC '%s'\n", mrc_url);
		}
		wt->data = (void *)mc;
	}
	mutex_unlock(&mrccomm_wq->lock);

	if (!do_not_delete && cl)
		prot_list_destroy(cl);

	trace_msg("done: initialized mrccomm\n");
}


void mrccomm_destroy(void)
{
	struct list_head *lh;
	struct list_head *temp;

	workqueue_stop(mrccomm_wq);

	mutex_lock(&mrccomm_wq->lock);
	dbg_msg("Workqueue locked.\n");

	while (!list_empty(&mrccomm_wq->threads)) {
		list_for_each_safe(lh, temp, &mrccomm_wq->threads) {
			struct work_thread *wt;
			struct MRC_Channel *mc;

			wt = list_entry(lh, struct work_thread, tlist);
			mc = (struct MRC_Channel *)wt->data;
			list_del(&wt->tlist);
			dbg_msg("Deleting channel.\n");
			MRC_Channel_delete_contents(mc);
			free(mc);
		}
	}
	mutex_unlock(&mrccomm_wq->lock);
	dbg_msg("Workqueue unlocked (and probably no threads)\n");
	workqueue_del(mrccomm_wq);
	mrccomm_wq = NULL;
}

