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
#include <string.h>

#include "file.h"
#include "file_inv.h"
#include "logger.h"

#define NUM_FILES 1024


int
main(int argc, char **argv)
{
	struct file_inv *fc;
	struct hash_table_entry *hte;
	struct fileid_struct *fs;
	int i;
	char filename[256];
	
	fc = file_inv_new();
	
	for(i=0; i<NUM_FILES; i++) {
		fs = (struct fileid_struct *)malloc(sizeof(struct fileid_struct));
		fileid_struct_init(fs);
		sprintf(filename, "%d", i);
		fs->path = strdup(filename);	
		file_inv_add(fc, fs);
	}
	
	for(i=0; i<NUM_FILES; i++) {
		sprintf(filename, "%d", i);
		hte = file_inv_get_entry(fc, filename);
		trace_msg("Checking file number %d\n", i);
		if(hte == NULL) {
			err_msg("File number %d was not stored in cache!\n", i);
			break;
		}
		fs = (struct fileid_struct *)hte->value;
		fileid_struct_delete(fs);
		free(hte->key);
		list_del((struct list_head *)hte);
		hash_table_entry_delete(&hte);
	}
	
	
	file_inv_destroy(fc);
	
	return 0;
}

