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
   C Interface: xattr

   Description:
*/

#ifndef __XTRFS_XATTR_H__
#define __XTRFS_XATTR_H__

#include "lock_utils.h"

#include "list.h"

struct xattr {
	struct list_head head;
	char *key;
	char *value;
	size_t size;
};

extern struct xattr *xattr_new(char *key, char *value, size_t size);
extern int xattr_init(struct xattr *x, char *key, char *name, size_t size);
extern void xattr_destroy(struct xattr *x);

struct xattr_list {
	struct list_head elems;
	spinlock_t lock;
	size_t key_size;
};

extern struct xattr_list *xattr_list_new();
extern int xattr_list_init(struct xattr_list *xl);
extern void xattr_list_del_contents(struct xattr_list *xl);
extern void xattr_list_destroy(struct xattr_list *xl);
extern struct xattr_list *xattr_list_clone(struct xattr_list *xl);

extern int xattr_list_add(struct xattr_list *xl, struct xattr *x);
extern int xattr_list_add_values(struct xattr_list *xl, char *key, char *value, size_t size);

extern struct json_object *xattr_list_to_json(struct xattr_list *xl);
extern struct xattr_list *json_to_xattr_list(struct json_object *jo);

extern char *xattr_list_key_list(struct xattr_list *xl, size_t *size);

extern int xattr_list_print(struct xattr_list *xl);

#endif
