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
C Interface: osd_request

Description: 


Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_OSD_REQUEST_H__
#define __XTRFS_OSD_REQUEST_H__

#include <pthread.h>

#include <ne_string.h>

#include "lock_utils.h"
#include "list.h"


typedef enum {
	UNDEFINED = 0,
	OSD_GET,
	OSD_PUT,
	OSD_DELETE,
	OSD_TRUNCATE,
	OSD_GET_PROT_VERS
} OSD_Com_t;


struct OSD_Request {
	struct list_head head;
	int id;
	OSD_Com_t command;
	char *fileID;
	int epoch;
	void *req_data;
	void *resp_data;
	ne_buffer *resp_buffer;
	void *(*error_cb)(void *);
	void *(*finish_cb)(void *);
	pthread_cond_t wait_cond;
	mutex_t wait_mutex;
	
	void (*free_data)(void *data);		
};

extern struct OSD_Request *OSD_Request_new();
extern int OSD_Request_init(struct OSD_Request *or);
extern void OSD_Request_clear(struct OSD_Request *or);
extern void OSD_Request_delete(struct OSD_Request *or);


extern int OSD_Request__getFile(struct OSD_Request *or, char *fileId, int objId, int firstBye, int lastByte);
extern int OSD_Request__putFile(struct OSD_Request *or, char *fileId, int objId, int firstByte, int lastBute);

#endif
