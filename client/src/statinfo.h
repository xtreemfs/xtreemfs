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
C Interface: statinfo

Description: 


Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_STATINFO_H__

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
#define __XTRFS_STATINFO_H__

#include "xattr.h"
#include "list.h"


typedef enum {
	OBJ_UNDEFINED=0,
	OBJ_FILE,
	OBJ_DIR,
	OBJ_SYMLINK
} xtreemfs_obj_type_t;

struct xtreemfs_statinfo {
	char *              fileId;
	char *              ownerId;
	char *              groupId;
	xtreemfs_obj_type_t objType;
	off_t               size;
	long                epoch;
	time_t              atime;
	time_t              ctime;
	time_t              mtime;
	int                 posixAccessMode;
	char *              linkTarget;
	struct xattr_list * xAttrs;
	struct list_head    replicas;
	struct list_head    acl;
	int                 linkCount;
};

extern int xtreemfs_statinfo_init(struct xtreemfs_statinfo *si);
extern void xtreemfs_statinfo_clear(struct xtreemfs_statinfo *si);
extern int xtreemfs_statinfo_copy(struct xtreemfs_statinfo *to, struct xtreemfs_statinfo* from);

extern int xtreemfs_statinfo_to_json_ip(struct xtreemfs_statinfo *si, struct json_object *jo);
extern struct json_object *xtreemfs_statinfo_to_json(struct xtreemfs_statinfo *);

extern int json_to_xtreemfs_statinfo_ip(struct json_object *jo, struct xtreemfs_statinfo *si);
extern struct xtreemfs_statinfo *json_to_xtreemfs_statinfo(struct json_object *);

extern int xtreemfs_statinfo_print(struct xtreemfs_statinfo *si);

#endif
