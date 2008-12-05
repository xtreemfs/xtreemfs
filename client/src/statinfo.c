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
*  C Implementation: statinfo
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

/* If __STRICT_ANSI__ is not set, the JSON headers assume
 * C99 conformity which we do not want!
 */
#ifndef __STRICT_ANSI__
#define __STRICT_ANSI__
#include <json_object.h>
#undef __STRICT_ANSI__
#else
#include <json_object.h>
#endif
#include <linkhash.h>

#include "statinfo.h"
#include "xattr.h"
#include "list.h"


struct xtreemfs_statinfo *
xtreemfs_statinfo_new()
{
	struct xtreemfs_statinfo *rv = NULL;
	
	rv = (struct xtreemfs_statinfo *)malloc(sizeof(struct xtreemfs_statinfo));
	if(rv != NULL && xtreemfs_statinfo_init(rv) != 0) {
		xtreemfs_statinfo_clear(rv);
		free(rv);
		rv = NULL;
	}
	
	return rv;
}

int
xtreemfs_statinfo_init(struct xtreemfs_statinfo *si)
{
	si->fileId          = NULL;
	si->ownerId         = NULL;
	si->groupId         = NULL;
	si->objType         = OBJ_UNDEFINED;
	si->size            = 0;
	si->epoch           = 0;
	si->atime           = 0;
	si->ctime           = 0;
	si->mtime           = 0;
	si->posixAccessMode = 0;
	si->linkTarget      = NULL;
	si->xAttrs          = NULL;
	INIT_LIST_HEAD(&si->replicas);
	INIT_LIST_HEAD(&si->acl);
	si->linkCount       = 0;
	
	return 0;
}

void
xtreemfs_statinfo_clear(struct xtreemfs_statinfo *si)
{
	free(si->fileId);
	free(si->ownerId);
	free(si->groupId);
	free(si->linkTarget);

	if (si->xAttrs)
		xattr_list_destroy(si->xAttrs);

	/* And free replica and acl lists */
}


int
xtreemfs_statinfo_copy(struct xtreemfs_statinfo *to, struct xtreemfs_statinfo *from)
{
	*to = *from;

	if(from->fileId)
		to->fileId = strdup(from->fileId);
	if(from->ownerId)
		to->ownerId = strdup(from->ownerId);
	if(from->groupId)
		to->groupId = strdup(from->groupId);
	if(from->linkTarget)
		to->linkTarget = strdup(from->linkTarget);
	if(from->xAttrs)
		to->xAttrs = xattr_list_clone(from->xAttrs);

	INIT_LIST_HEAD(&to->replicas);
	INIT_LIST_HEAD(&to->acl);

	return 0;
}


int
xtreemfs_statinfo_to_json_ip(struct xtreemfs_statinfo *si, struct json_object *jo)
{
	int rv = 0;
	
	json_object_object_add(jo, "fileId", json_object_new_string(si->fileId));
	json_object_object_add(jo, "ownerId", json_object_new_string(si->ownerId));
	json_object_object_add(jo, "size", json_object_new_int(si->size));
	json_object_object_add(jo, "epoch", json_object_new_int(si->epoch));
	json_object_object_add(jo, "atime", json_object_new_int(si->atime));
	json_object_object_add(jo, "ctime", json_object_new_int(si->ctime));
	json_object_object_add(jo, "mtime", json_object_new_int(si->mtime));
	json_object_object_add(jo, "posixAccessMode", json_object_new_int(si->posixAccessMode));
	if(si->linkTarget)
		json_object_object_add(jo, "linkTarget", json_object_new_string(si->linkTarget));
#if 0
	if(si->xAttrs)
		json_object_object_add(jo, "xAttrs", xattrs_to_json(si->xAttrs));
	if(!list_empty(&si->replicas))
		json_object_object_add(jo, "replicas", xlocs_to_json(&si->replicas));
	if(!list_empty(&si->acl))
		json_object_object_add(jo, "acl", acls_to_json(&si->acl));
#endif

	if (si->xAttrs)
		json_object_object_add(jo, "xAttrs", xattr_list_to_json(si->xAttrs));
	json_object_object_add(jo, "linkCount", json_object_new_int(si->linkCount));

	return rv;
}

struct json_object *
xtreemfs_statinfo_to_json(struct xtreemfs_statinfo *si)
{
	struct json_object *rv = NULL;
	
	rv = json_object_new_object();
	if(rv == NULL)
		goto finish;
		
	if(xtreemfs_statinfo_to_json_ip(si, rv) != 0) {
		json_object_put(rv);
		rv = NULL;
	}
	
finish:
	return rv;
}


int
json_to_xtreemfs_statinfo_ip(struct json_object *jo, struct xtreemfs_statinfo *si)
{
	int rv = 0;
	
	json_object_object_foreach(jo, key, val) {
		if(!strcmp(key, "fileId")) {
			si->fileId = strdup(json_object_get_string(val));
		} else if(!strcmp(key, "ownerId")) {
			si->ownerId = strdup(json_object_get_string(val));
		} else if(!strcmp(key, "groupId")) {
			si->groupId = strdup(json_object_get_string(val));
		} else if(!strcmp(key, "objType")) {
			si->objType = json_object_get_int(val);
		} else if(!strcmp(key, "size")) {
			si->size = json_object_get_int(val);
		} else if(!strcmp(key, "epoch")) {
			si->epoch = json_object_get_int(val);
		} else if(!strcmp(key, "atime")) {
			si->atime = json_object_get_int(val);
		} else if(!strcmp(key, "ctime")) {
			si->ctime = json_object_get_int(val);
		} else if(!strcmp(key, "mtime")) {
			si->mtime = json_object_get_int(val);
		} else if(!strcmp(key, "posixAccessMode")) {
			si->posixAccessMode = json_object_get_int(val);
		} else if(!strcmp(key, "linkTarget")) {
			si->linkTarget = strdup(json_object_get_string(val));
		} else if(!strcmp(key, "xAttrs")) {
			si->xAttrs = json_to_xattr_list(val);
		} else if(!strcmp(key, "replicas")) {
		
		} else if(!strcmp(key, "acl")) {
		
		} else if(!strcmp(key, "linkCount")) {
			si->linkCount = json_object_get_int(val);
		}
	}

	return rv;
}


struct xtreemfs_statinfo *
json_to_xtreemfs_statinfo(struct json_object *jo)
{
	struct xtreemfs_statinfo *rv = NULL;
	
	rv = xtreemfs_statinfo_new();
	if (rv) {
		if (json_to_xtreemfs_statinfo_ip(jo, rv)) {
			xtreemfs_statinfo_clear(rv);
			free(rv);
			rv = NULL;
		}
	}
	return rv;
}

int
xtreemfs_statinfo_print(struct xtreemfs_statinfo *si)
{
	printf("fileId:         %s\n", si->fileId);
	printf("ownerId:        %s\n", si->ownerId);
	printf("groupId:        %s\n", si->groupId);
	printf("objType:        %d\n", si->objType);
	printf("size:           %ld\n", si->size);
	printf("epoch:          %ld\n", si->epoch);
	printf("posixAccessMode %d\n", si->posixAccessMode);
	return 0;
}

