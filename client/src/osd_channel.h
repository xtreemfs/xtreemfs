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
C Interface: osd_channel

Description: 


Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_OSD_CHANNEL_H__
#define __XTRFS_OSD_CHANNEL_H__

#include <ne_session.h>
#include <ne_string.h>

#include "file.h"
#include "xloc.h"
#include "xcap.h"


#define OSD_DEFAULT_PORT 32637

enum OSD_Channel_errs {
	OSC_ERR_NO_ERROR = 0,
	OSC_ERR_REDIRECT,
	OSC_ERR_MALFORMED_REQUEST,
	OSC_ERR_CAP_EXPIRED,
	OSC_ERR_FORBIDDEN,
	OSC_ERR_NOEXIST,
	OSC_ERR_INV_RANGE,
	OSC_ERR_INTERNAL,
	OSC_ERR_OVERLOAD,
	OSC_ERR_INV_NEWSIZE,
	OSC_ERR_INV_EPOCHE,
	OSC_ERR_INV_LENGTH,
	OSC_ERR_GENERAL			/*!< Not able to identify error more closely */
};

/**
 * Response of channel operation.
 */
struct OSD_Channel_resp {
	char *location;
	off_t new_size;
	long epoch;
	size_t length;
	char *req_id;
	int err;
};

extern int OSD_Channel_resp_init(struct OSD_Channel_resp *ocr);
extern void OSD_Channel_resp_clear(struct OSD_Channel_resp *ocr);


struct OSD_Channel {
	ne_uri osd_uri;
	ne_session *channel_session;

	/* Buffer descriptions for this channel and the currently running
	   operation.
	 */
	ne_buffer  *resp_buf;
	char       *user_buf;
	size_t     user_buf_size;	/* Max size of user_buf */
	size_t     user_buf_len;	/* Actual size */
	int        use_buf;		/* Will be 0 for resp_buf,
                                           1 for user_buf */

	pthread_cond_t  wait_cond;
	mutex_t         wait_mutex;
	spinlock_t      lock;

	int prot_vers;

	enum OSD_Channel_errs err;
};

extern struct OSD_Channel *OSD_Channel_new(char *osd_id, struct dirservice *ds);
extern int OSD_Channel_init(struct OSD_Channel *oc, char *osd_id,
			    struct dirservice *ds);
extern void OSD_Channel_delete_contents(struct OSD_Channel *oc);
extern void OSD_Channel_delete(struct OSD_Channel *oc);

extern int OSD_Channel_restart(struct OSD_Channel *oc);

int OSD_Channel_get(struct OSD_Channel *oc, char *req_id, struct user_file *uf,
		    int obj_num, off_t firstByte, off_t lastByte,
		    void *buf, struct OSD_Channel_resp *resp);
int OSD_Channel_put(struct OSD_Channel *oc, char *req_id, struct user_file *uf,
		    int obj_num, off_t firstByte, off_t lastByte,
		    void *buf, struct OSD_Channel_resp *resp);
int OSD_Channel_del(struct OSD_Channel *oc,	char *fileid,
		    struct xloc_list *xlocs_list, struct xcap *xcap,
		    struct OSD_Channel_resp *resp);
int OSD_Channel_trunc(struct OSD_Channel *oc,
		      char *fileid, off_t new_size,
		      struct xloc_list *xlocs_list, struct xcap *xcap,
		      struct OSD_Channel_resp *resp);

#endif
