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
*  C Implementation: osd_proxy
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

#include <string.h>

#include "osd_proxy.h"
#include "dirservice.h"
#include "logger.h"


struct bench_data_collection osd_proxy_bandwidth;


struct OSD_Proxy *
OSD_Proxy_new(char *osd_url, int num_channels, struct dirservice *ds)
{
	struct OSD_Proxy *rv = NULL;
	
	rv = (struct OSD_Proxy *)malloc(sizeof(struct OSD_Proxy));
	if(rv && OSD_Proxy_init(rv, osd_url, num_channels, ds)) {
		OSD_Proxy_delete(rv);
		rv = NULL;
	}
	
	return rv;
}


/**
 * Initialize osd proxy structure.
 *
 * This function expects a full uri (ie. including the scheme)
 */
int
OSD_Proxy_init(struct OSD_Proxy *osd, char *osd_id, int num_channels,
	       struct dirservice *ds)
{
	int rv = 0;
	int i;
#if 0
	char *osd_url = NULL;
#endif
	osd->num_channels = 0;
	osd->channel = NULL;
	osd->osd_id = strdup(osd_id);

#if 0
	if (ds) {
		osd_url = dirservice_resolve_uuid(ds, osd_id, OSD_DEFAULT_PORT);
	} else {
		osd_url = strdup(osd_id);
	}

	if(ne_uri_parse(osd_url, &osd->osd_uri) || !osd->osd_uri.host) {
		rv = -1;
		goto finish;
	}
#endif
	osd->num_channels = num_channels;
	osd->channel = (struct OSD_Channel **)malloc(sizeof(struct OSD_Channel *) * osd->num_channels);
	if (!osd->channel) {
		rv =-2;
		goto finish;
	}
	memset((void *)osd->channel, 0, sizeof(struct OSD_Channel *) * osd->num_channels);
	
	for(i=0; i<osd->num_channels; i++) {
		osd->channel[i] = OSD_Channel_new(osd_id, ds);
		if(osd->channel[i] == NULL) {
			rv = -2;
			goto finish;
		}
	}
	
	osd->next_free = 0;
	spin_init(&osd->lock, PTHREAD_PROCESS_PRIVATE);
	
finish:
#if 0
	free(osd_url);
#endif
	return rv;
}


void
OSD_Proxy_clear(struct OSD_Proxy *op)
{
	int i;
	
	if(op->channel) {
		for(i=0; i<op->num_channels; i++)
			if (op->channel[i])
				OSD_Channel_delete(op->channel[i]);
		free(op->channel);
	}
	free(op->osd_id);
#if 0
	ne_uri_free(&op->osd_uri);
#endif
}


void
OSD_Proxy_delete(struct OSD_Proxy *op)
{
	if(op) {
		OSD_Proxy_clear(op);
		spin_destroy(&op->lock);
		free(op);
	}
}

int
OSD_Proxy_get(struct OSD_Proxy *op, char *req_id,
	      struct user_file *uf,
	      int obj_num, off_t first_byte, off_t last_byte,
	      void *buf, struct OSD_Channel_resp *resp)
{
	int rv = 0;
	struct OSD_Channel *oc = NULL;
	int idx;
	struct bench_timer osd_proxy_timer;
		
	
	/* TODO Check if channel is still alive, recreate it otherwise */
	
	bench_timer_init(&osd_proxy_timer);
	bench_timer_start(&osd_proxy_timer);
	
	spin_lock(&op->lock);
	idx = op->next_free;
	if(++(op->next_free) >= op->num_channels)
		op->next_free = 0;
	spin_unlock(&op->lock);
	
	oc = op->channel[idx];
	
	pthread_mutex_lock(&oc->wait_mutex);
	rv = OSD_Channel_get(oc, req_id, uf, obj_num, first_byte, last_byte, buf, resp);
	pthread_mutex_unlock(&oc->wait_mutex);
		
	bench_timer_stop(&osd_proxy_timer);
	
	bench_data_collection_add(&osd_proxy_bandwidth,
				  (double)(last_byte - first_byte + 1)
					  /bench_timer_ms(&osd_proxy_timer));

	return rv;
}


int
OSD_Proxy_put(struct OSD_Proxy *op, char *req_id,
	      struct user_file *uf,
	      int obj_num, off_t first_byte, off_t last_byte,
	      void *buf, struct OSD_Channel_resp *resp)
{
	int rv = 0;
	struct OSD_Channel *oc = NULL;
	int idx;
	struct bench_timer osd_proxy_timer;
	
	bench_timer_init(&osd_proxy_timer);
	bench_timer_start(&osd_proxy_timer);
	
	/* TODO Check if channel is still alive, recreate it otherwise */

	dbg_msg("Putting data from %lld to %lld\n", first_byte, last_byte);
	if (uf) {
		dbg_msg("user file x-location: %s\n", uf->xloc->repr);
	}
	spin_lock(&op->lock);
	idx = op->next_free;
	if(++(op->next_free) >= op->num_channels)
		op->next_free = 0;
	spin_unlock(&op->lock);
	oc = op->channel[idx];
	
	pthread_mutex_lock(&oc->wait_mutex);
	rv = OSD_Channel_put(oc, req_id, uf, obj_num, first_byte, last_byte, buf, resp);
#if 0
	if(rv != 0) {
		rv = OSD_Channel_restart(oc);
		if (!rv) {
			rv = OSD_Channel_put(oc, uf, obj_num, first_byte, last_byte, buf, resp);
		}
	}
#endif
	pthread_mutex_unlock(&oc->wait_mutex);
	bench_timer_stop(&osd_proxy_timer);
	
	bench_data_collection_add(&osd_proxy_bandwidth,
				  (double)(last_byte - first_byte + 1)
					  / bench_timer_ms(&osd_proxy_timer));
	
	return rv;
}

int
OSD_Proxy_del(struct OSD_Proxy *op,
	      char *fileid, struct xloc_list *xlocs_list,
	      struct xcap *xcap, struct OSD_Channel_resp *resp)
{
	int rv = 0;
	int idx;
	struct OSD_Channel *oc;

	spin_lock(&op->lock);
	idx = op->next_free;
	if(++(op->next_free) >= op->num_channels)
		op->next_free = 0;
	spin_unlock(&op->lock);
	oc = op->channel[idx];
	
	pthread_mutex_lock(&oc->wait_mutex);
	rv = OSD_Channel_del(oc, fileid, xlocs_list, xcap, resp);
	pthread_mutex_unlock(&oc->wait_mutex);
	
	return rv;
}

int
OSD_Proxy_trunc(struct OSD_Proxy *op,
		char *fileid, off_t new_size,
		struct xloc_list *xlocs_list, struct xcap *xcap,
		struct OSD_Channel_resp *resp)
{
	int err = 0;
	int idx;
	struct OSD_Channel *oc;
	
	spin_lock(&op->lock);
	idx = op->next_free;
	if (++(op->next_free) >= op->num_channels)
		op->next_free = 0;
	spin_unlock(&op->lock);
	oc = op->channel[idx];
	
	pthread_mutex_lock(&oc->wait_mutex);
	err = OSD_Channel_trunc(oc, fileid, new_size, xlocs_list, xcap, resp);
	pthread_mutex_unlock(&oc->wait_mutex);
	
	return err;
}
