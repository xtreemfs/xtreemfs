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
C Interface: osd_manager

Description: 


Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_OSD_MANAGER_H__
#define __XTRFS_OSD_MANAGER_H__

#include "osd_proxy.h"
#include "list.h"

#define NUM_PROXIES_PER_CHANNEL 3


struct OSD_Manager_elem {
	struct list_head head;
	struct OSD_Proxy *proxy;
};


struct OSD_Manager {
	struct list_head proxies;
};


extern struct OSD_Manager *OSD_Manager_new();
extern int OSD_Manager_init(struct OSD_Manager *om);
extern void OSD_Manager_clear(struct OSD_Manager *om);
extern void OSD_Manager_delete(struct OSD_Manager *om);

extern int OSD_Manager_update_proxies(struct OSD_Manager *om);

extern int OSD_Manager_add_proxy(struct OSD_Manager *om, struct OSD_Proxy *op);
extern struct OSD_Proxy *OSD_Manager_find_proxy(struct OSD_Manager *om, char *osd_url);
extern struct OSD_Proxy *OSD_Manager_get_proxy(struct OSD_Manager *om, char *osd_url,
					       struct dirservice *ds);

#endif
