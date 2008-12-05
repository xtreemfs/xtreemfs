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
   C Interface: obj_set

   Description:
*/

#ifndef __XTRFS_OBJ_SET_H__
#define __XTRFS_OBJ_SET_H__

#include "list.h"

struct obj_set_entry {
	int start_num;
	int end_num;
	int stride;
	void **data;	/*!< Data associated with the objects, if any */
};

struct obj_set_entry *obj_set_entry_new();
int obj_set_entry_init(struct obj_set_entry *ose);
void obj_set_entry_destroy(struct obj_set_entry *ose);


/**
 * Object Set Description
 */
struct obj_set {
	int max_num_entries;
	int num_entries;

	struct obj_set_entry *entries;
};

struct obj_set *obj_set_new(int init_num);
struct obj_set *obj_set_new_num(int obj_num);
int obj_set_init(struct obj_set *os, int init_num);
void obj_set_del_contents(struct obj_set *os);
void obj_set_destroy(struct obj_set *os);

int obj_set_add_num(struct obj_set *os, int num);
int obj_set_add_range(struct obj_set *os, int start, int end);

#endif
