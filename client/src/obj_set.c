/* Copyright (c) 2007, 2008  Matthias Hess

   Author: Matthias Hess

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
   C Implementation: obj_set

   Description:
 */

#include <stdlib.h>
#include <string.h>

#include <json.h>

#include "obj_set.h"

#define OBJ_SET_INCR_SIZE   10


struct obj_set *obj_set_new(int init_num)
{
	struct obj_set *rv = NULL;

	rv = (struct obj_set *)malloc(sizeof(struct obj_set));
	if (rv && obj_set_init(rv, init_num)) {
		obj_set_destroy(rv);
		rv = NULL;
	}

	return rv;
}

/**
 * Create an object set for a single object only
 */
struct obj_set *obj_set_new_num(int obj_num)
{
	struct obj_set *rv = NULL;

	rv = obj_set_new(2);	/* Reserve memory for a single entry */
	if (!rv)
		goto out;

	rv->entries[0].start_num = obj_num;
	rv->entries[0].end_num   = obj_num;
	rv->entries[0].stride = 1;
	
out:
	return rv;
}

int obj_set_init(struct obj_set *os, int init_num)
{
	int err = 0;

	os->max_num_entries = init_num;
	os->num_entries     = 0;
	os->entries = (struct obj_set_entry *)
		malloc(sizeof(struct obj_set_entry) * os->max_num_entries);
	if (!os->entries) {
		err = 1;
		goto out;
	}

	memset((void *)os->entries, 0, sizeof(struct obj_set_entry) * os->max_num_entries);

out:
	return err;
}

void obj_set_del_contents(struct obj_set *os)
{
	free(os->entries);
	os->entries = NULL;
	os->max_num_entries = 0;
	os->num_entries = 0;
}

void obj_set_destroy(struct obj_set *os)
{
	obj_set_del_contents(os);
	free(os);
}

int obj_set_add_entry(struct obj_set *os,
		      int start_num, int end_num, int stride)
{
	int err = 0;
	int n = os->num_entries;
	int l;
	
	if (stride == 0)
		stride = 1;

	os->entries[n].start_num = start_num;
	os->entries[n].end_num   = end_num;
	os->entries[n].stride    = stride;
	/* And alloc data pointer array */
	l = (end_num - start_num) / stride + 1;
	os->entries[n].data = (void **)malloc(sizeof(void *) * l);
	memset((void *)os->entries[n].data, 0, sizeof(void *) * l);

	if (++os->num_entries >= os->max_num_entries) {
		os->max_num_entries += OBJ_SET_INCR_SIZE;
		os->entries = (struct obj_set_entries *)
			realloc((void *)os->entries,
				sizeof(struct obj_set_entry) * os->max_num_entries);
		if (!os->entries) {
			err = 1;
			goto out;
		}
		l = os->max_num_entries - os->num_entries;
		memset((void *)&os->entries[n+1], 0, l * sizeof(struct obj_set_entry));
	}

out:
	return err;
}

int obj_set_add_num(struct obj_set *os, int num)
{
	return obj_set_add_entry(os, num, num, 1);
}

int obj_set_add_range(struct obj_set *os, int start, int end)
{
	return obj_set_add_entry(os, start, end, 1);
}

struct json_object *obj_set_to_json(struct obj_set *os)
{
	struct json_object *rv = NULL;

	
	return rv;
}
