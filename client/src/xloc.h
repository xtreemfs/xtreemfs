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
C Interface: xloc

Description: 

Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_XLOC_H__
#define __XTRFS_XLOC_H__

#include <json_object.h>

#include "dirservice.h"
#include "file.h"
#include "list.h"
#include "lock_utils.h"

struct file_replica;


/**
 * Xloc list
 *
 * Contains striping policy and the URLs of the participating OSDs.
 */
struct xlocs {
	struct list_head head;
	struct striping_policy *sp; /*!< striping policy */
	char **osds;                /*!< Array of OSD ids */
	int num_osds;               /*!< Number of currently defined OSDs */
	char *repr;                 /*!< Representation of x-location */
};

extern struct xlocs *xlocs_new();
extern int xlocs_init(struct xlocs *xl);
extern void xlocs_clear(struct xlocs *xl);
extern void xlocs_delete(struct xlocs *xl);
extern int xlocs_copy(struct xlocs *dst, struct xlocs *src);

void xlocs_add_osd(struct xlocs *xl, char *osd_url);

/**
 * Xloc list of lists
 * Contains an additional version number.
 */
struct xloc_list {
	spinlock_t lock;
	struct list_head head;
	int version;
#if PROT_VERSION > 2
	char *repUpdatePolicy;
#endif
	char *repr;
};

extern struct xloc_list *xloc_list_new();
extern void xloc_list_delete(struct xloc_list *xl);

extern int xlocs_list_add_xloc(struct xloc_list *xll, struct xlocs *xl);

extern struct xlocs *xloc_list_get_idx_str(char *xloc_list_str, int i);

extern int json_to_xlocs_list(struct json_object *jo, struct xloc_list *xll);
extern struct json_object *xlocs_list_to_json(struct xloc_list *xll);

extern struct json_object *xloc_to_json(struct xlocs *xl);
extern struct xlocs *str_to_xlocs(char *str);
extern char *xlocs_to_str(struct xlocs *xl);

extern struct xloc_list *str_to_xlocs_list(char *xlocs);

extern void xlocs_list_delete(struct list_head *xlocs_list);

/*
 * Handling of xlocation header components, related to stripe info.
 */

/**
 * Extract xlocation instance string from full xlocation header.
 *
 * @param xloc the full XLocation header JSON string
 * @param indx the index of the xloc instance (replica instance)
 * @return pointer to newly allocated string with xlock_instance JSON string
 */
extern char *xloc_inst_extract(char *xloc, int indx);

/** Parse xlocation header instance string.
 *
 * @param ofile the target open_file structure for parsed results
 * @param xlock_instance the xloc instance JSON string
 * @return non-zero if any error occured
 */
extern int xloc_inst_parse(struct file_replica *ofile,
			   char *xloc_instance, struct dirservice *ds);

extern void xlocs_print(struct xlocs *xl);

#endif
