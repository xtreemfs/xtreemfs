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
C Interface: stripingpolicy

Description: 


Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/
 
#ifndef __XTRFS_STRIPINGPOLICY_H__
#define __XTRFS_STRIPINGPOLICY_H__

#include <json.h>

#include "stripe.h"

struct striping_policy_mapping {
	char *name;
	enum striping_policy_t id;
};

extern struct striping_policy_mapping striping_policy_mapping[];
extern struct striping_policy_mapping striping_policy_name[];

extern struct striping_policy *striping_policy_new();
extern int striping_policy_init(struct striping_policy *sp);

extern int striping_policy_cmp(struct striping_policy *s1, struct striping_policy *s2);
extern void striping_policy_copy(struct striping_policy *dest, struct striping_policy *src);

extern struct json_object *striping_policy_to_json(struct striping_policy *sp);
extern struct striping_policy *json_to_striping_policy(struct json_object *jo);

extern int striping_policy_to_json_ip(struct striping_policy *sp, struct json_object *jo);
extern int json_to_striping_policy_ip(struct json_object *jo, struct striping_policy *sp);


/* And for easy debugging */

extern int striping_policy_print(struct striping_policy *sp);

#endif
