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
*  C Implementation: stripingpolicy
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

#include "stripe.h"
#include "stripingpolicy.h"


static char *striping_policy_names[] = {
	"UNKNOWN",
	"RAID0",
	"RAID5"
};
#define NUM_STRIPING_POLICIES 3

struct striping_policy_mapping striping_policy_mapping[] = {
	{ "RAID0", SPOL_RAID_0 },
	{ "RAID5", SPOL_RAID_5 },
	{ NULL,    SPOL_UNKNOWN }
};

struct striping_policy_mapping striping_policy_name[] = {
	[SPOL_UNKNOWN] = { NULL,    SPOL_UNKNOWN },
	[SPOL_RAID_0] = { "RAID0", SPOL_RAID_0 },
	[SPOL_RAID_5] = { "RAID5", SPOL_RAID_5 }
};


struct striping_policy *
striping_policy_new()
{
	struct striping_policy *rv = NULL;
	
	rv = (struct striping_policy *)malloc(sizeof(struct striping_policy));
	if(rv != NULL && striping_policy_init(rv) != 0) {
		free(rv);
		rv = NULL;
	}
	
	return rv;
}

int
striping_policy_init(struct striping_policy *sp)
{
	int rv = 0;
	
	sp->id          = SPOL_UNKNOWN;
	sp->stripe_size = 0;
	sp->width       = 0;
	
	return rv;
}


int
striping_policy_cmp(struct striping_policy *s1, struct striping_policy *s2)
{
	int rv = 0;
	
	rv  = (s1->id           == s2->id);
	rv &= (s1->stripe_size  == s2->stripe_size);
	rv &= (s1->width        == s2->width);
	
	return rv;
}

void
striping_policy_copy(struct striping_policy *dest, struct striping_policy *src)
{
	dest->id          = src->id;
	dest->stripe_size = src->stripe_size;
	dest->width       = src->width;
}


struct json_object *
striping_policy_to_json(struct striping_policy *sp)
{
	struct json_object *rv = NULL;
	
	rv = json_object_new_object();
	if(rv == NULL) goto finish;
	
	if(striping_policy_to_json_ip(sp, rv) != 0) {
		free(rv);
		rv = NULL;
	}
	
finish:
	return rv;
}

struct striping_policy *
json_to_striping_policy(struct json_object *jo)
{
	struct striping_policy *rv = NULL;
	
	rv = striping_policy_new();
	if(json_to_striping_policy_ip(jo, rv) != 0) {
		free(rv);
		rv = NULL;
	}
	
	return rv;
}


int
striping_policy_to_json_ip(struct striping_policy *sp, struct json_object *jo)
{
	json_object_object_add(jo, "policy", json_object_new_string(striping_policy_names[sp->id]));
	json_object_object_add(jo, "stripe-size", json_object_new_int(sp->stripe_size / 1024));
	json_object_object_add(jo, "width", json_object_new_int(sp->width));
	
	return 0;
}

int
json_to_striping_policy_ip(struct json_object *jo, struct striping_policy *sp)
{
	int rv = 0;
	char *policy_name;
	int i;
	
	policy_name = json_object_get_string(json_object_object_get(jo, "policy"));
	for(i=0; i<NUM_STRIPING_POLICIES; i++) {
		if(!strncmp(policy_name, striping_policy_names[i], strlen(striping_policy_names[i])))
			break;
	}
	/* 'policy_name' still belongs to the JSON object, so no free is needed here! */
	
	if(i < NUM_STRIPING_POLICIES) sp->id = i;
	else sp->id = 0;
	
	sp->stripe_size = json_object_get_int(json_object_object_get(jo, "stripe-size")) * 1024;
	sp->width       = json_object_get_int(json_object_object_get(jo, "width"));
	
	return rv;
}

int
striping_policy_print(struct striping_policy *sp)
{
	if(sp->id < NUM_STRIPING_POLICIES) printf("Policy:         %s\n", striping_policy_names[sp->id]);
	else printf("Policy:         UNKNOWN\n");
	printf("Stripe size:    %ld\n", sp->stripe_size);
	printf("Width:          %d\n", sp->width);
	
	return 0;
}
