/* This file is part of the XtreemFS client.

   XtreemFS client is free software: you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation, either version 2 of
   the License, or (at your option) any later version.

   The XtreemFS client is distributed in the hope that it will be
   useful, but WITHOUT ANY WARRANTY; without even the implied warranty
   of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with XtreemFS client.  If not, see <http://www.gnu.org/licenses/>.
 */


/*
C Interface: lease

Description: 


Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_LEASE_H__
#define __XTRFS_LEASE_H__

#include <stdlib.h>
#include <time.h>

#include <json.h>

#include "obj_set.h"

enum lease_ops {
	LEASE_OP_UNDEFINED=0,
	LEASE_OP_READ,
	LEASE_OP_WRITE
};

extern char *lease_ops_str[];

struct lease {
	char *clientId;
	char *leaseId;
	char *fileId;
	struct obj_set *obj_set;
	enum lease_ops op;
	time_t expires;
	int alloc;			/*!< Indicates if object set has been
					     allocated internally (1) or
					     externally (0) */
};

extern struct lease *lease_new(struct obj_set *os, time_t expires, int alloc);
extern int lease_init(struct lease *l, struct obj_set *os, time_t expires, int alloc);
extern void lease_destroy(struct lease *l);

extern struct lease *lease_new_num(int obj_num, time_t expires);

extern int lease_is_valid(struct lease *l);

extern struct json_object *lease_to_json(struct lease *l);
extern struct lease *json_to_lease(struct json_object *jo);

#endif
