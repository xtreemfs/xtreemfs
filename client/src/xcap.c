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
*  C Implementation: xcap
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
#include <string.h>
#include <pthread.h>

#include <json_object.h>
#include <json_tokener.h>
#include <bits.h>

#include "xcap.h"

#include "logger.h"


struct xcap * xcap_new()
{
	struct xcap *rv = NULL;
	
	rv = (struct xcap *)malloc(sizeof(struct xcap));
	if(rv != NULL && xcap_init(rv) != 0) {
		xcap_delete(rv);
		rv = NULL;
	}
	
	return rv;
}

int
xcap_init(struct xcap *xc)
{
	int rv = 0;
	
	xc->fileID          = NULL;
	xc->accessMode      = NULL;
	xc->expires         = 0;
	xc->clientIdentity  = NULL;
	xc->truncateEpoch   = 0;
	xc->serverSignature = NULL;
	xc->repr            = NULL;
	spin_init(&xc->lock, PTHREAD_PROCESS_PRIVATE);
	INIT_LIST_HEAD(&xc->head);

	creds_init(&xc->creds);

	return rv;
}

void
xcap_clear(struct xcap *xc)
{
	if(xc) {
		free(xc->fileID);
		free(xc->accessMode);
		free(xc->clientIdentity);
		free(xc->serverSignature);
		free(xc->repr);

		free(xc->creds.guid);
		free(xc->creds.ggid);
		xc->creds.guid = NULL;
		xc->creds.ggid = NULL;
		xc->fileID = NULL;
		xc->accessMode = NULL;
		xc->clientIdentity = NULL;
		xc->serverSignature = NULL;
		xc->repr = NULL;
	}
}

void
xcap_delete(struct xcap *xc)
{
	xcap_clear(xc);
	spin_destroy(&xc->lock);
	free(xc);
}

int xcap_values_from_json(struct xcap *xc, struct json_object *jo)
{
	int err = 0;
	struct json_object *array_obj;

	if(json_object_get_type(jo) != json_type_array && json_object_array_length(jo) != 5) {
		err = 1;
		goto finish;
	}
	
	array_obj = json_object_array_get_idx(jo, 0);
	xc->fileID = strdup(json_object_get_string(array_obj));
	
	array_obj = json_object_array_get_idx(jo, 1);
	xc->accessMode = strdup(json_object_get_string(array_obj));
	
	array_obj = json_object_array_get_idx(jo, 2);	
	xc->expires    = json_object_get_int(array_obj);
	
	array_obj = json_object_array_get_idx(jo, 3);
	xc->clientIdentity = strdup(json_object_get_string(array_obj));

	array_obj = json_object_array_get_idx(jo, 4);
	xc->truncateEpoch = json_object_get_int(array_obj);
	
	array_obj = json_object_array_get_idx(jo, 5);
	xc->serverSignature = strdup(json_object_get_string(array_obj));

	xc->repr = strdup(json_object_to_json_string(jo));

finish:
	return err;
}


/**
 * Fill in the values of xcap with the ones corr. to 'val_str'.
 */
int xcap_values_from_str(struct xcap *xc, char *val_str)
{
	int err = 0;
	struct json_object *val_obj = NULL;

	val_obj = json_tokener_parse(val_str);
	if (!val_obj || is_error(val_obj)) {
		err = 1;
		err_msg("Cannot parse string '%s'\n", val_str);
		val_obj = NULL;
		goto finish;
	}

	xcap_values_from_json(xc, val_obj);
		
finish:
	if (val_obj)
		json_object_put(val_obj);

	return err; 
}

int xcap_copy_values_from_str(struct xcap *xc, char *val_str)
{
	int err = 0;
	
	// pthread_spin_lock(&xc->lock);
	xcap_clear(xc);
	xcap_values_from_str(xc, val_str);
	// pthread_spin_unlock(&xc->lock);
	
	return err;
}

void xcap_copy(struct xcap *dst, struct xcap *src)
{
	/* Consistency check: xcaps shopuld belong to the same file */
	if (strcmp(dst->fileID, src->fileID))
		return;

	xcap_clear(dst);
}


struct json_object *
xcap_to_json(struct xcap *xc)
{
	struct json_object *rv = NULL;
		
	if (!xc->repr) {
		rv = json_object_new_array();
		if(rv == NULL) goto finish;

		json_object_array_add(rv, json_object_new_string(xc->fileID));
		json_object_array_add(rv, json_object_new_string(xc->accessMode));
		json_object_array_add(rv, json_object_new_int((int)(xc->expires)));
		json_object_array_add(rv, json_object_new_string(xc->clientIdentity));
		json_object_array_add(rv, json_object_new_int((int)(xc->truncateEpoch)));
		json_object_array_add(rv, json_object_new_string(xc->serverSignature));
		xc->repr = strdup(json_object_to_json_string(rv));
	} else {
		rv = json_tokener_parse(xc->repr);
	}
finish:
	return rv;
}

struct xcap *
json_to_xcap(struct json_object *jo)
{
		struct xcap *rv = NULL;
		struct json_object *array_obj;
		
		if(json_object_get_type(jo) != json_type_array && json_object_array_length(jo) != 6)
			goto finish;
			
		rv = xcap_new();
		array_obj = json_object_array_get_idx(jo, 0);
		xcap_values_from_json(rv, jo);
finish:
		return rv;
}


char *
xcap_to_str(struct xcap *xcap)
{
	char *rv = NULL;
	struct json_object *jo;
	
	jo = xcap_to_json(xcap);
	if(jo == NULL)
		goto finish;	
	rv = strdup(json_object_get_string(jo));
	json_object_put(jo);
	
finish:
	return rv;
}


struct xcap *
str_to_xcap(char *str)
{
	struct xcap *rv = NULL;
	struct json_object *jo;
	
	dbg_msg("JSON string is '%s':\n", str);
	jo = json_tokener_parse(str);
	if(jo == NULL)
		goto finish;
	rv = json_to_xcap(jo);
	dbg_msg("JSON object is '%s':\n", json_object_get_string(jo));
	
	json_object_put(jo);

finish:
	return rv;
}


void
xcap_print(struct xcap *xc)
{
	info_msg("File ID:      %s\n", xc->fileID);
	info_msg("Access mode:  %s\n", xc->accessMode);
	info_msg("Expires:      %ld\n", xc->expires);
	info_msg("Client ID:    %s\n", xc->clientIdentity);
	info_msg("Trunc. Epoch: %d\n", xc->truncateEpoch);
	info_msg("Server Sign.  %s\n", xc->serverSignature);
}
