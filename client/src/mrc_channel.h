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


#ifndef __XTRFS_MRC_CHANNEL_H__
#define __XTRFS_MRC_CHANNEL_H__
/*
  C Interface: mrc_channel

  Description: 


  Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007

  Copyright: See COPYING file that comes with this distribution

*/


#include <ne_session.h>
#include <ne_string.h>
#include "mrc_request.h"

struct MRC_Channel_header {
	struct list_head head;
	char *key;
	char *value;
};

struct MRC_Channel_err_descr {
	int code;
	char *msg;
};

extern struct MRC_Channel_err_descr *MRC_Channel_err_descr_new();
extern int MRC_Channel_err_descr_init(struct MRC_Channel_err_descr *ed);
extern void MRC_Channel_err_descr_delete(struct MRC_Channel_err_descr *ed);


struct MRC_Channel {
	ne_uri mrc_uri;
	ne_session *        channel_session;
	ne_buffer *         resp_buf;

	int                 prot_vers;		/*!< Max. supported prot version */

	struct list_head    resp_headers;	/*!< Needed in order to analyse result */
	
	void (*error_cb)(void *);
	void (*finish_cb)(void *);
	
	pthread_cond_t      wait_cond;
	mutex_t             wait_mutex;

	struct timeval      drift;		/*!< Time drift between MRC and us. */
};

extern struct MRC_Channel *MRC_Channel_new(char *mrc_url);
extern int MRC_Channel_init(struct MRC_Channel *mc, char *mrc_url);
extern void MRC_Channel_delete_contents(struct MRC_Channel *mc);
extern void MRC_Channel_delete(struct MRC_Channel *mc);

int MRC_Channel_exec_p(struct MRC_Channel *mc, struct MRC_payload *p,
		       struct req *req);

/**
 * Execution map.
 * Maps commands to names of the corresponding MRC RPC. This is only
 * required for JSON over HTTP or other protocols that use explicit
 * function names.
 * This map contains also a pointer to an MRCC function that handles
 * the specific command.
 * Eventually we will add a pointer to a response handling function, too.
 * Make the execution map known to at least the communication stage
 */
struct MRCC_exec_map {
	MRC_Com_t comm;
	char *fname;
	int (*func)(struct MRC_Channel *mc, struct MRC_payload *p);
	/* int (*resp_func)(struct MRC_Channel *mc, struct MRC_payload *p); */
};

extern struct MRCC_exec_map MRCC_exec_map[];

#endif /* __XTRFS_MRC_CHANNEL_H__ */
