#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

#include <json_tokener.h>

#include <stripe.h>
#include <stripingpolicy.h>
#include <xloc.h>

#include "list.h"


int
main(int argc, char **argv)
{
	struct list_head xlocs_list, *iter;
	struct xlocs *xl;
	struct json_object *jo = NULL;
	int err_code;
	
	struct list_head xlocations;	
	char *xlocs_strs[] = {
		"[ [ { \"policy\": \"RAID0\", \"stripe-size\": 4, \"width\": 1 }, [ \"localhost\" ] ], [ { \"policy\": \"RAID5\", \"stripe-size\": 4, \"width\": 5 }, [ \"host1\", \"host2\", \"host3\", \"host4\", \"host5\" ] ] ]"
	};
		
	jo = json_tokener_parse(xlocs_strs[0]);
	if(jo == NULL) {
		fprintf(stderr, "Cannot parse string '%s'\n", xlocs_strs[0]);
		exit(1);
	}
	
	INIT_LIST_HEAD(&xlocs_list);
	
	if((err_code = json_to_xlocs_list(jo, &xlocs_list)) != 0) {
		fprintf(stderr, "Cannot convert JSON object to xlocation list. Error code %d\n", err_code);
	}
	
	if(!list_empty(&xlocs_list)) {
		list_for_each(iter, &xlocs_list) {
			striping_policy_print(((struct xlocs *)iter)->sp);
			printf("\n");
		}
	}
	
	iter = xlocs_list.next;
	while(iter != &xlocs_list) {
		list_del(iter);
		xlocs_delete((struct xlocs *)iter);
		iter = xlocs_list.next;
	}
	
	json_object_put(jo);
	
	return 0;
}
