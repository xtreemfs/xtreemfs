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


#ifndef __XTRFS_STRIPE_H__
#define __XTRFS_STRIPE_H__

/**
 * Stripe objects and related stuff.
 *
 * @author: Erich Focht <efocht@hpce.nec.com>
 */

#include <json_object.h>

#include "fileops.h"
#include "list.h"


/**
 * Striping policy types.
 * Defined so far:
 *   RAID0 (just striped)
 *   RAID5 (one parity stripe block per parity block of "width" stripes)
 */
enum striping_policy_t {
	SPOL_UNKNOWN = 0,
	SPOL_RAID_0,
	SPOL_RAID_5,	/* not implemented */
};

/**
 * Striping policy info.
 * This thing must _always_ exist. It describes the distribution of
 * stripes (AKA stripe objects) across the OSDs.
 * For RAID5 and alike the parity blocks are just normal addressable
 * stripes, the OSD doesn't know about them (except for file size
 * calculation).
 */
struct striping_policy {
	enum striping_policy_t id;	/*!< policy ID */
	size_t stripe_size;		/*!< size of stripes in bytes! */
	int width;			/*!< number of OSDs to use */
};

struct file_replica;

void sobj_init(int num_threads);
void sobj_close(void);

struct user_file;
struct req *sobj_req_create(int type, int sobj_id, loff_t offset,
			    size_t size, void *buffer, char *url,
			    struct user_file *fd,
			    struct req *parent);

extern struct work_queue *sobj_wq;

struct sobj_payload {
	enum filerw_op op;	/*!< Operation (read/write) */
	int sobj_id;		/*!< File object ID */
	loff_t offset;		/*!< offset in the file object */
	size_t size;		/*!< transfer size (bytes) */
	size_t rsize;		/*!< real transfer size (bytes) */
	char *osd;		/*!< targetted OSD */
	void *buffer;		/*!< pointer to buffer for data */
	struct user_file *fd; 	/*!< file descriptor of open file */
	unsigned int order;	/*!< request order */
	long epoch;		/*!< epoch in which this request was issued. */
};

/**
 * Dump info on one sobj request to log.
 *
 * @param req pointer to request to be shown
 */
void print_sobj_info(struct req *req);


#endif // __XTRFS_STRIPE_H__
