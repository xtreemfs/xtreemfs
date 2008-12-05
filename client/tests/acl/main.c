/*
*  C Implementation: main
*
* Description: 
*
*
* Author: Matthias Hess <mhess@hpce.nec.com>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <json_object.h>

#include "acl.h"
#include "logger.h"

int
main(int argc, char **argv)
{
	/* struct acl_entry *ae; */
	struct acl_list al;
	int fd;
	struct stat statbuf;
	char *filename = "/tmp/test.dat";
	struct json_object *jo;
	
	acl_list_init(&al);
	
	fd = open(filename, O_CREAT, S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH);
	if (fd < 0) {
		fprintf(stderr, "Cannot create temporary file '%s'", filename);
		exit(1);
	}
	close(fd);
	stat(filename, &statbuf);
	
	mode_to_acl_list(statbuf.st_mode, &al);
	acl_list_print(&al);
	
	jo = acl_list_to_json(&al);
	if(jo) {
		printf("JSON string: '%s'\n", json_object_to_json_string(jo));
		json_object_put(jo);
	}
	
	acl_list_clean(&al);
	
	unlink(filename);
	
	return 0;
}
