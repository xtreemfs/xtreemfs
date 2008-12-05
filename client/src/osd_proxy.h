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
C Interface: osd_proxy

Description: 


Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_OSD_PROXY_H__
#define __XTRFS_OSD_PROXY_H__

#include <ne_uri.h>

#include "file.h"
#include "osd_channel.h"
#include "dirservice.h"
#include "bench_timer.h"


extern struct bench_data_collection osd_proxy_bandwidth;

struct OSD_Proxy {
	char      *osd_id;
#if 0
	ne_uri     osd_uri;
#endif
	int        num_channels;
	int        next_free;
	struct OSD_Channel **channel;
	spinlock_t lock;
};

extern struct OSD_Proxy *OSD_Proxy_new(char *osd_id, int num_channels,
				       struct dirservice *ds);
extern int OSD_Proxy_init(struct OSD_Proxy *op, char *osd_id, int num_channels,
			  struct dirservice *ds);
extern void OSD_Proxy_delete(struct OSD_Proxy *op);

extern int OSD_Proxy_get(struct OSD_Proxy *op, char *req_id, struct user_file *uf,
			 int obj_num, off_t first_byte, off_t last_byte,
			 void *buf, struct OSD_Channel_resp *resp);
extern int OSD_Proxy_put(struct OSD_Proxy *op, char *req_id, struct user_file *uf,
			 int obj_num, off_t first_byte, off_t last_byte,
			 void *buf, struct OSD_Channel_resp *resp);
extern int OSD_Proxy_del(struct OSD_Proxy *op, char *fileid,
			 struct xloc_list *xlocs_list, struct xcap *xcap,
			 struct OSD_Channel_resp *resp);
extern int OSD_Proxy_trunc(struct OSD_Proxy *op, char *fileid, off_t new_szie,
			   struct xloc_list *xlocs_list, struct xcap *xcap,
			   struct OSD_Channel_resp *resp);
extern int OSD_Proxy_heartbeat(struct OSD_Proxy *op, struct user_file);

#endif
