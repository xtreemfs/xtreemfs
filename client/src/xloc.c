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
   C Implementation: xloc

   Description: 


   Copyright: See COPYING file that comes with this distribution

 */

#include <stdlib.h>
#include <stdio.h>

#include <string.h>
#include <errno.h>

#include "stripingpolicy.h"
#include "xloc.h"
#include "dirservice.h"

#include "lock_utils.h"
#include "logger.h"


struct xlocs *xlocs_new()
{
	struct xlocs *rv = NULL;
	
	rv = (struct xlocs *)malloc(sizeof(struct xlocs));
	if(rv != NULL && xlocs_init(rv) != 0) {
		xlocs_delete(rv);
		rv = NULL;
	}
	
	return rv;
}

int xlocs_init(struct xlocs *xl)
{
	int rv = 0;

	INIT_LIST_HEAD(&xl->head);
	xl->sp       = NULL;
	xl->osds     = NULL;
	xl->num_osds = 0;
	xl->repr     = NULL;

	return rv;
}

void xlocs_clear(struct xlocs *xl)
{
	int i;
	
	if(xl->sp) {
		for(i=0; i<xl->sp->width; i++) {
			free(xl->osds[i]);
		}
		free(xl->sp);
	}
	free(xl->osds);
	xl->osds = NULL;
	xl->num_osds = 0;
	free(xl->repr);
}

void xlocs_delete(struct xlocs *xl)
{
	xlocs_clear(xl);
	free(xl);
}

void xlocs_new_repr(struct xlocs *xl)
{
	if(xl->repr) free(xl->repr);
	xl->repr = xlocs_to_str(xl);
}

char *xlocs_full_uri(char *uri)
{
	char *rv = NULL;
	char *pnt = NULL;
	size_t len, real_len;
	size_t scheme_size = strlen("http://");

	pnt = strstr(uri, "://");
	if (!pnt) {   /* No scheme given */
		len = strlen(uri);
		real_len = len + scheme_size;
		rv = (char *)malloc(real_len + 1);
		strncpy(rv, "http://", scheme_size + 1);
		rv = strncat(rv, uri, len);
		dbg_msg("New URI is '%s'\n", rv);
	} else {
		rv = strdup(uri);
	}

	return rv;
}


void xlocs_add_osd(struct xlocs *xl, char *osd_id)
{
	if (xl->num_osds == 0) {
	
		/* In a consistent state, this should not be 
		   necessary, because xl->osds would always be NULL
		   if xl->num_osds == 0 */
		if (xl->osds)
			free(xl->osds);
		xl->osds = (char **)malloc(sizeof(char *) * xl->sp->width);
		memset((void *)xl->osds, 0, xl->sp->width * sizeof(char *));
	}
	
	if (xl->num_osds < xl->sp->width) {
		xl->osds[xl->num_osds] = /* xlocs_full_uri */strdup(osd_id);
		xl->num_osds++;
	}
	
}


/**
 * Extract one xlocation from a JSON object.
 *
 * The JSON object is usually one part of a complete xlocation
 * description.
 *
 * @param 
 */ 
struct xlocs *json_to_xloc(struct json_object *jo)
{
	struct xlocs *rv = NULL;
	struct json_object *osds_obj;
	int i;
	
	if (json_object_get_type(jo) == json_type_array &&
	    json_object_array_length(jo) == 2) {
		rv = xlocs_new();
		if(rv == NULL) {
			err_msg("Cannot create new xlocs structure.\n");
			goto finish;
		}
		rv->sp = json_to_striping_policy(json_object_array_get_idx(jo, 0));
		if(rv->sp == NULL) {
			err_msg("Cannot create striping policy!\n");
			goto err;
		}

		/* Now an array with OSD urls must follow */
		osds_obj = json_object_array_get_idx(jo, 1);
		if(json_object_get_type(osds_obj) != json_type_array) {
			err_msg("Next object is not an array!\n");
			goto err;
		}

		/* Check for right size of array */
		if(json_object_array_length(osds_obj) != rv->sp->width) {
			err_msg("Wrong number of OSD strings in JSON object.\n");
			goto err;
		}
			
		for(i=0; i<rv->sp->width; i++) {
			xlocs_add_osd(rv, json_object_get_string(json_object_array_get_idx(osds_obj, i)));
		}

		rv->repr = strdup(json_object_to_json_string(jo));
		dbg_msg("Representation of x-loc: %s\n", rv->repr);
		goto finish;
		
	}
err:
	free(rv->sp);
	rv->sp = NULL;
	xlocs_delete(rv);
	rv = NULL;
	
finish:
	return rv;
}

struct json_object *xloc_to_json(struct xlocs *xl)
{
	struct json_object *rv = NULL;

	rv = json_tokener_parse(xl->repr);
	
	return rv;
}

struct xlocs *str_to_xlocs(char *str)
{
	struct xlocs *rv = NULL;
	struct json_object *jo;
	
	jo = json_tokener_parse(str);
	if(jo == NULL)
		goto finish;
	
	rv = json_to_xloc(jo);
	json_object_put(jo);
	
finish:
	return rv;
}

char *xlocs_to_str(struct xlocs *xl)
{
	char *rv = NULL;

	rv = strdup(xl->repr);
	
	dbg_msg("xlocs_to_str is %s\n", rv);

	return rv;
}


struct xloc_list *xloc_list_new()
{
	struct xloc_list *rv = NULL;

	rv = (struct xloc_list *)malloc(sizeof(struct xloc_list));
	if (rv) {
		INIT_LIST_HEAD(&rv->head);
		rv->version = 0;
		spin_init(&rv->lock, PTHREAD_PROCESS_PRIVATE);
#if PROT_VERSION > 2
		rv->repUpdatePolicy = NULL;
#endif
		rv->repr = NULL;
	}

	return rv;
}

void xloc_list_delete_entries(struct xloc_list *xl)
{
	struct list_head *iter, *in;
	struct xlocs *x;

	iter = xl->head.next;

	spin_lock(&xl->lock);
	while(iter != &xl->head) {
		in = iter->next;
		list_del(iter);
		x = container_of(iter, struct xlocs, head);
		xlocs_delete(x);
		iter = in;
	}
#if PROT_VERSION > 2
	free(xl->repUpdatePolicy);
#endif
	free(xl->repr);
	spin_unlock(&xl->lock);
}

void xloc_list_delete(struct xloc_list *xl)
{
	xloc_list_delete_entries(xl);
	spin_destroy(&xl->lock);
	free(xl);
}


struct xlocs *xloc_list_get_idx_str(char *xloc_list_str, int i)
{
	struct xlocs *rv = NULL;
	struct json_object *lo, *jo, *array;

	lo = json_tokener_parse(xloc_list_str);
	
	if (!lo || is_error(lo)) {
		lo = NULL;
		goto finish;
	}

	array = json_object_array_get_idx(lo, 0);
	
	if (i < 0 || i >= json_object_array_length(array))
		goto finish;

	jo = json_object_array_get_idx(array, i);
	rv = json_to_xloc(jo);

finish:
	if (lo)
		json_object_put(lo);

	return rv;
}

/**
 * convert JSON object to xlocation list.
 * @param jo the JSON object to convert
 * @param lh the resulting list of xlocs
 * @returns 0 on success, error code otherwise
 */
int json_to_xlocs_list(struct json_object *jo, struct xloc_list *xll)
{
	int rv = 0;
	struct xlocs *xl;
	struct json_object *jo2;
#if PROT_VERSION > 2
	struct json_object *rup;
#endif
	struct json_object *array_obj;
	int i;
	
	INIT_LIST_HEAD(&xll->head);
	if (xll->repr)
		free(xll->repr);

	/* Object type must be an array of arrays */
	if(json_object_get_type(jo) != json_type_array) {
		rv = 1;
		goto finish;
	}
	
#if PROT_VERSION > 2
	if (json_object_array_length(jo) != 3) {
		rv = 3;
		goto finish;
	}
	jo2 = json_object_array_get_idx(jo, 0);
#else
	if (json_object_array_length(jo) != 2) {
		rv = 3;
		goto finish;
	}
	jo2 = json_object_array_get_idx(jo, 0);
#endif
	for(i=0; i<json_object_array_length(jo2); i++) {
		array_obj = json_object_array_get_idx(jo2, i);
		xl = json_to_xloc(array_obj);
		if(xl == NULL) {
			rv = 2;
			break;
		}
		list_add_tail(&xl->head, &xll->head);
	}
	
	xll->version = json_object_get_int(json_object_array_get_idx(jo, 1));
#if PROT_VERSION > 2
	rup = json_object_array_get_idx(jo, 2);
	xll->repUpdatePolicy = strdup(json_object_get_string(rup));
#endif
	xll->repr = strdup(json_object_to_json_string(jo));
	
finish:
	if(rv != 0) {
		err_msg("Error %d in converting JSON to xloc_list\n", rv);
		xloc_list_delete_entries(xll);
	}

	return rv;
}


struct json_object *
xlocs_list_to_json(struct xloc_list *xll)
{
	struct json_object *rv = NULL, *array;
	struct xlocs *xl;
	struct list_head *iter;

	spin_lock(&xll->lock);

	rv = json_object_new_array();
	
	array = json_object_new_array();

	list_for_each(iter, &xll->head) {
		xl = container_of(iter, struct xlocs, head);
		json_object_array_add(array, xloc_to_json(xl));
	}
	
	json_object_array_add(rv, array);
	json_object_array_add(rv, json_object_new_int(xll->version));
#if PROT_VERSION > 2
	json_object_array_add(rv, json_object_new_string(xll->repUpdatePolicy));
#endif
	spin_unlock(&xll->lock);

	return rv;
}

struct xloc_list *str_to_xlocs_list(char *xlocs)
{
	struct xloc_list *xlocs_list;
	struct json_object *jo;

	xlocs_list = xloc_list_new();
	if (!xlocs_list) {
		err_msg("unable to allocate lh\n");
		return NULL;
	}
	jo = json_tokener_parse(xlocs);
	if (!jo) {
		err_msg("Error in parsing the x-locs string.\n");
		goto error;
	}
	if (json_to_xlocs_list(jo, xlocs_list) != 0) {
		err_msg("Cannot extract xlocation list.\n");
		goto error;
	}
	json_object_put(jo);
	return xlocs_list;
 error:
	free(xlocs_list);
	return NULL;
}

/**
 * Extract xlocation instance string from full xlocation header.
 *
 * @param xloc the full XLocation header JSON string
 * @param idx the index of the xloc instance (replica instance)
 * @return pointer to newly allocated string with xloc_instance JSON string
 */
char *xloc_inst_extract(char *xloc, int idx)
{
	struct json_object *obj, *sobj, *aobj;
	char *inst = NULL;
	int nobjs;

	obj = json_tokener_parse(xloc);

	aobj = json_object_array_get_idx(obj, 0);

	nobjs = json_object_array_length(aobj);
	if (idx < 0 || idx >= nobjs) {
		err_msg("ERROR in %s: idx=%d out of range [0,%d)\n",
			__FUNCTION__, idx, nobjs);
		goto out;
	}
	sobj = json_object_array_get_idx(aobj, idx);
	inst = strdup(json_object_to_json_string(sobj));

	/* does sobj need to be freed, too? */
	/* json_object_put(sobj); */

 out:
	json_object_put(obj); 
	return inst;
}

/**
 * Parse xlocation header instance string.
 *
 * @param ofile the target open_file structure for parsed results
 * @param xloc_instance the xloc instance JSON string
 * @return non-zero if any error occured
 */
int xloc_inst_parse(struct file_replica *ofile, char *xloc_instance,
		    struct dirservice *ds)
{
	struct json_object *obj, *sobj, *osds, *o;
	char *policy, **ids;
	int owidth, i, nosds, err = ENOENT;
	char *id_str = NULL;

	dbg_msg("Parsing x location message: %s\n", xloc_instance);
	
	obj = json_tokener_parse(xloc_instance);
	if (json_object_array_length(obj) != 2) {
		err_msg("ERROR in %s: xloc_inst no of elements? %s\n",
			__FUNCTION__, json_object_to_json_string(obj));
		goto out;
	}

	/* parse striping policy */
	sobj = json_object_array_get_idx(obj, 0);
	o = json_object_object_get(sobj, "policy");
	if (!(o = json_object_object_get(sobj, "policy")))
		goto fail;
	policy = json_object_get_string(o);
	if (!strcmp(policy, "RAID0"))
		ofile->sp.id = SPOL_RAID_0;
	else if (!strcmp(policy, "RAID5"))
		ofile->sp.id = SPOL_RAID_5;
	else
		ofile->sp.id = SPOL_UNKNOWN;

	if (!(o = json_object_object_get(sobj, "stripe-size")))
		goto fail;
	ofile->sp.stripe_size = (size_t)(json_object_get_int(o) * 1024);

	if (!(o = json_object_object_get(sobj, "width")))
		goto fail;
	owidth = ofile->sp.width;
	ofile->sp.width = json_object_get_int(o);
	/* json_object_put(sobj); */

	/* parse OSD list */
	osds = json_object_array_get_idx(obj, 1);
	nosds = json_object_array_length(osds);
	ids = ofile->osd_ids;
	if (ids) {
		/* free old strings */
		for (i = 0; i < owidth; i++)
			if (ids[i])
				free(ids[i]);
	}
	if (nosds > owidth || !ids) {
		/* need to realloc pointer array: free elements first */
		ids = (char **)realloc((void *)ids, sizeof(char *) * nosds);
		if (!ids)
			goto fail_osds;
	}
	/* now populate pointer array */
	for (i = 0; i < nosds; i++) {
		o = json_object_array_get_idx(osds, i);
		id_str = json_object_get_string(o);
		ids[i] = strdup(id_str);
#if 0
		if (ds) {
			ids[i] = dirservice_resolve_uuid(ds, url_str, 0);
		} else {
			urls[i] = /* xlocs_full_uri */strdup(url_str);
		}
#endif
		if (!ids[i])
			goto fail_osds;
	}

	ofile->osd_ids = ids;
	err = 0;

 fail_osds:
	// json_object_put(osds);
 fail:
	// json_object_put(o);
 out:
	json_object_put(obj);
	return err;
}


void
xlocs_print(struct xlocs *xl)
{
	int i;
	
	for(i=0; i<xl->sp->width; i++) {
		printf("osd[%d] = %s\n", i, xl->osds[i]);
	}
}
