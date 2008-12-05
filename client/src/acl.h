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
 C Interface: acl

 Description: 


 Author: Matthias Hess <mhess@hpce.nec.com>, (C) 2007

 Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_ACL_H__
#define __XTRFS_ACL_H__

#include <pthread.h>

#include <sys/types.h>

#include "list.h"
#include "lock_utils.h"

struct acl_entry {
	char *tag;
	char *qualifier;
	char *perms;
	struct list_head head;
};

extern struct acl_entry *acl_entry_new();
extern void acl_entry_destroy(struct acl_entry *ae);
extern struct json_object *acl_entry_to_json(struct acl_entry *ae);
extern struct acl_entry *json_to_acl_entry(struct json_object *jo);

struct acl_list {
	struct list_head acls;
	spinlock_t lock;
};

extern int acl_list_init(struct acl_list *al);
extern void acl_list_clean(struct acl_list *al);
extern void acl_list_print(struct acl_list *al);

extern mode_t acl_list_to_mode(struct acl_list *al);
extern int mode_to_acl_list(mode_t mode, struct acl_list *al);

extern struct json_object *acl_list_to_json(struct acl_list *al);
extern int json_to_acl_list(struct json_object *jo, struct acl_list *al);

#endif
