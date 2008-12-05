/*
*  C Implementation: main
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

#include <json.h>

#include <stripingpolicy.h>


int
main(int argc, char **argv)
{
	struct striping_policy sp, *reconstructed;
	struct json_object *jo;
	
	striping_policy_init(&sp);
	sp.id = SPOL_RAID_5;
	sp.stripe_size = 16384;
	sp.width = 6;
	
	printf("Original striping policy:\n");
	striping_policy_print(&sp);
	
	jo = striping_policy_to_json(&sp);
	printf("JSON string: %s\n", json_object_to_json_string(jo));
	
	reconstructed = json_to_striping_policy(jo);
	if(reconstructed != NULL) {
		striping_policy_print(reconstructed);
		free(reconstructed);
	}
	
	json_object_put(jo);
	
	return 0;
}
