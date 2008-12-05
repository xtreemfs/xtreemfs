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
*  C Implementation: creds
*
* Description: 
*
*
* Author: Matthias Hess <mhess@laptopmh.stg.hpce.tld>, (C) 2008
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdlib.h>
#include <string.h>

#include <errno.h>
#include <sys/types.h>
#ifndef _WIN32
#include <pwd.h>
#include <grp.h>
#else
#include <windows.h>
#define boolean json_boolean
#endif

#include <json_object.h>

#include "logger.h"

#include "creds.h"

int creds_init(struct creds *c)
{
	c->uid = -1;
	c->gid = -1;
	c->pid = -1;

	c->guid = NULL;
	c->ggid = NULL;
	c->subgrp = NULL;

	return 0;
}

void creds_delete_content(struct creds *c)
{
	if (c) {
		free(c->guid);
		free(c->ggid);
		free(c->subgrp);
	}
}

/**
 * Convert JSON object to credentials.
 *
 * TODO: Implementation!
 */
int json_to_creds(struct creds *c, struct json_object *jo)
{
	int err = 0;

	return err;
}

int json_to_creds_ip(struct json_object *jo, struct creds *c)
{
	int err = 0;
	return err;
}

struct json_object *creds_to_json(struct creds *c)
{
	int err = 0;

	struct json_object *rv = NULL;
	struct json_object *grp = NULL;

#ifndef _WIN32
	struct passwd      passwd;
	struct passwd      *pwd_res;
	struct group       group;
	char pwd_str[1024];
	struct group       *grp_res;
	char group_str[1024];
#endif /* _WIN32 */

	rv = json_object_new_object();
	if (!rv)
		goto out;

	/* So far, only 'nullauth' is supported */	
	json_object_object_add(rv, "mechanism", json_object_new_string("nullauth"));
	grp = json_object_new_array();
	if (!grp || is_error(grp)) {
		err = 1;
		goto out;
	}

	if (c->guid && c->ggid) {
		json_object_object_add(rv, "guid", json_object_new_string(c->guid));

		json_object_array_add(grp, json_object_new_string(c->ggid));
	} else {

#ifndef _WIN32
		getpwuid_r(c->uid, &passwd, pwd_str, 1024, &pwd_res);
		dbg_msg("Errno after getpwuid: %d\n", errno);
		errno = 0;
		getgrgid_r(c->gid, &group, group_str, 1024, &grp_res);
		dbg_msg("Errno after getgrgid: %d\n", errno);

		if (!pwd_res || !grp_res) {
			dbg_msg("Cannot determine user name or group name\n");
			err = 3;
			goto out;
		}
		json_object_object_add(rv, "guid", json_object_new_string(pwd_res->pw_name));
		json_object_array_add(grp, json_object_new_string(grp_res->gr_name));
#else
		char buffer[100];
		DWORD len = 100;
		GetUserName(buffer, &len);
		json_object_object_add(rv, "guid", json_object_new_string(buffer));
		json_object_array_add(grp, json_object_new_string(""));

#endif /* _WIN32 */
	}
	json_object_object_add(rv, "ggids", grp);

out:
	if (err) {
		if (rv) {
			json_object_put(rv);
			rv = NULL;
		}
	}

	return rv;
}

char *creds_to_str(struct creds *c)
{
	char *rv = NULL;
	struct json_object *jo;

	jo = creds_to_json(c);
	if (jo) {
	rv = strdup(json_object_to_json_string(jo));
	json_object_put(jo);
	} else {
		err_msg("Cannot create JSON from creds.\n");
	}

	return rv;
}

int creds_to_str_ip(struct creds *c, char *str, int n)
{
	int err = 0;
	char *dummy = creds_to_str(c);

	if (dummy) {
		strncpy(str, dummy, n);
		free(dummy);
	} else
		err = 1;

	return err;
}

int creds_copy(struct creds *dest, struct creds *src)
{
	int err = 0;

	dest->uid = src->uid;
	dest->gid = src->gid;
	dest->pid = src->pid;

	if (dest->guid) free(dest->guid);
	if (dest->ggid) free(dest->ggid);
	if (dest->subgrp) free(dest->subgrp);

	if (src->guid) {
		dbg_msg("guid: %s\n", src->guid);
		dest->guid = strdup(src->guid);
	} else
		dest->guid = NULL;
	if (src->ggid) {
		dbg_msg("ggid: %s\n", src->ggid);
		dest->ggid = strdup(src->ggid);
	} else
		dest->ggid = NULL;
	if (src->subgrp) {
		dbg_msg("subgrp: %s\n", src->subgrp);
		dest->subgrp = strdup(src->subgrp);
	} else
		dest->subgrp = NULL;

	return err;
}

int creds_dbg(struct creds *c)
{
	dbg_msg("uid:    %d\n", c->uid);
	dbg_msg("gid:    %d\n", c->gid);
	dbg_msg("guid:   %p\n", c->guid);
	dbg_msg("ggid:   %p\n", c->ggid);
	return 0;
}
