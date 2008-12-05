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
*  C Implementation: osd_manager
*
* Description: 
*
*
* Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdlib.h>
#include <stdio.h>

#include "osd_manager.h"
#include "osd_proxy.h"
#include "dirservice.h"


struct OSD_Manager *
OSD_Manager_new()
{
	struct OSD_Manager *rv = NULL;
	
	rv = (struct OSD_Manager *)malloc(sizeof(struct OSD_Manager));
	if(rv != NULL && OSD_Manager_init(rv) != 0) {
		OSD_Manager_delete(rv);
		rv = NULL;
	}
	
	return rv;
}

int
OSD_Manager_init(struct OSD_Manager *om)
{
	INIT_LIST_HEAD(&om->proxies);
	return 0;
}

void
OSD_Manager_clear(struct OSD_Manager *om)
{
	struct list_head *iter;
	struct OSD_Proxy *proxy;
	
	while(!list_empty(&om->proxies)) {
		iter = om->proxies.next;
		list_del(iter);
		proxy = ((struct OSD_Manager_elem *)iter)->proxy;
		OSD_Proxy_delete(proxy);
		free(iter);
	}
}

void
OSD_Manager_delete(struct OSD_Manager *om)
{
	OSD_Manager_clear(om);
	free(om);
}


int
OSD_Manager_add_proxy(struct OSD_Manager *om, struct OSD_Proxy *op)
{
	int rv = 0;
	struct OSD_Manager_elem *elem = NULL;
	
	elem = (struct OSD_Manager_elem *)malloc(sizeof(struct OSD_Manager_elem));
	if(elem == NULL) {
		rv = 1;
		goto finish;
	}
	
	INIT_LIST_HEAD(&elem->head);
	elem->proxy = op;

	list_add_tail(&elem->head, &om->proxies);
	
finish:
	return rv;
}


struct OSD_Proxy *
OSD_Manager_find_proxy(struct OSD_Manager *om, char *osd_id)
{
	struct OSD_Proxy *rv = NULL, *ip;
	struct list_head *iter;
	ne_uri list_uri;

	/* First check for UUIDs only */
	list_for_each(iter, &om->proxies) {
		ip = ((struct OSD_Manager_elem *)iter)->proxy;
		dbg_msg("Comparing IDs: %s and %s\n", ip->osd_id, osd_id);
		if (!strcmp(ip->osd_id, osd_id)) {
			rv = ip;
			break;
		}
	}

	/* And check for old URI based comparison */

#if 0
	if (!rv) {
		if (!ne_uri_parse(osd_id, &list_uri)) {
			list_for_each(iter, &om->proxies) {
				ip = ((struct OSD_Manager_elem *)iter)->proxy;
				if(ne_uri_cmp(&list_uri, &ip->osd_uri) == 0) {
					rv = ip;
					break;
				}
			}
		}
		ne_uri_free(&list_uri);
	}
#endif
	return rv;
}

struct OSD_Proxy *
OSD_Manager_get_proxy(struct OSD_Manager *om, char *osd_id, struct dirservice *ds)
{
	struct OSD_Proxy *rv = NULL;
	
	rv = OSD_Manager_find_proxy(om, osd_id);
	if (!rv) {   /* We do not have that proxy */
		dbg_msg("Creating new proxy for '%s'\n",
			osd_id);
		rv = OSD_Proxy_new(osd_id, NUM_PROXIES_PER_CHANNEL, ds);
		if (rv) {
			dbg_msg("Adding proxy to manager.\n");
			OSD_Manager_add_proxy(om, rv);
		}
	}
	
	return rv;
}
