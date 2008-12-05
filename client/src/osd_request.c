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
*  C Implementation: osd_request
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
#include <unistd.h>

#include <pthread.h>
#include <ne_string.h>

#include "osd_request.h"


struct OSD_Request *
OSD_Request_new()
{
	struct OSD_Request *rv = NULL;
	
	rv = (struct OSD_Request *)malloc(sizeof(struct OSD_Request));
	if(rv != NULL && OSD_Request_init(rv) != 0) {
		OSD_Request_delete(rv);
		rv = NULL;
	}
	
	return rv;
}

int
OSD_Request_init(struct OSD_Request *or)
{
	int rv = 0;
	
	INIT_LIST_HEAD(&or->head);
	or->id = -1;
	or->command = UNDEFINED;
	or->req_data = NULL;
	or->resp_data = NULL;
	
	if((or->resp_buffer = ne_buffer_create()) == NULL) {
		rv = 1;
		goto finish;
	}
	or->error_cb = NULL;
	or->finish_cb = NULL;
	
	pthread_mutex_init(&or->wait_mutex, NULL);
	pthread_cond_init(&or->wait_cond, NULL);
	
finish:
	return rv;
}

void
OSD_Request_clear(struct OSD_Request *or)
{

}

void
OSD_Request_delete(struct OSD_Request *or)
{
	OSD_Request_clear(or);
	pthread_cond_destroy(&or->wait_cond);
	pthread_mutex_destroy(&or->wait_mutex);
	free(or);
}

