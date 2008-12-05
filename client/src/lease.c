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
   C Implementation: lease

   Description:
 */

#include <stdlib.h>
#include <stdio.h>

#include <json.h>

#include "lease.h"

char *lease_ops_str[] = {
	[LEASE_OP_UNDEFINED] = "undefined",
	[LEASE_OP_READ]      = "r",
	[LEASE_OP_WRITE]     = "w"
};

struct lease *lease_new(struct obj_set *os, time_t expires, int alloc)
{
	struct lease *rv = NULL;

	rv = (struct lease *)malloc(sizeof(struct lease));
	if (rv && lease_init(rv, os, expires, alloc)) {
		lease_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int lease_init(struct lease *l, struct obj_set *os, time_t expires, int alloc)
{
	int err = 0;

	l->clientId    = NULL;
	l->leaseId     = NULL;
	l->fileId      = NULL;

	/* Right now only a very simple object set is used. This will
	   change in the future. (Complex RAID level might require that) */
	l->obj_set     = os;

	l->op          = LEASE_OP_UNDEFINED;
	l->expires     = expires;

	l->alloc       = alloc;

out:
	return err;
}

/**
 * Create a lease for one object id.
 *
 * @param obj_num Object id
 * @param expires Time when this lease should expire
 *
 * @return New lease or NULL if lease cannot be created.
 */
struct lease *lease_new_num(int obj_num, time_t expires)
{
	struct lease *rv = NULL;
	struct obj_set *os = NULL;

	os = obj_set_new_num(obj_num);
	if (!os)
		goto out;

	rv = lease_new(os, expires, 1);
out:
	return rv;
}

void lease_destroy(struct lease *l)
{
	if (l->alloc)
		obj_set_destroy(l->obj_set);
	free(l);
}

/**
 * Check if a lease is still valid
 *
 * @param l Lease to check
 *
 * @return 1 if lease is valid, 0 otherwise
 */
int lease_is_valid(struct lease *l)
{
	int rv = 1;	/* For debugging 1, otherwise 0 */
	time_t now = time(NULL);

	if (!l)
		goto out;

	rv = (now < l->expires ? 1 : 0);
out:
	return rv;
}

/**
 * Convert a lease structure to a json object
 *
 * @param l Lease to convert
 *
 * @return New JSON object if object can be created,
 *         JSON error object or NULL if not.
 */
struct json_object *lease_to_json(struct lease *l)
{
	struct json_object *rv = NULL;
	int start_num;
	int end_num;
	char *lease_op_str;

	rv = json_object_new_object();

	json_object_object_add(rv, "clientId", json_object_new_string(l->clientId));
	json_object_object_add(rv, "leaseId",  json_object_new_string(l->leaseId));
	json_object_object_add(rv, "fileId",   json_object_new_string(l->fileId));

	/* Right now we have no specific format for an object set, so we use the
	   information from that set and add it to the lease object. This is
	   likely to become a JSON object of its own. */
	start_num = l->obj_set->entries[0].start_num;
	end_num = l->obj_set->entries[0].end_num;

	json_object_object_add(rv, "firstObject", json_object_new_int(start_num));
	json_object_object_add(rv, "lastObject",  json_object_new_int(end_num));

	json_object_object_add(rv, "expires",  json_object_new_int(l->expires));
	lease_op_str = lease_ops_str[l->op];

	json_object_object_add(rv, "operation", json_object_new_string(lease_op_str));

	return rv;
}

/**
 * Convert a JSON object into a lease
 *
 * @param jo JSON object to convert
 *
 * @return New lease structure or NULL if an error occurred.
 */
struct lease *json_to_lease(struct json_object *jo)
{
	struct lease *rv = NULL;

	return rv;
}
