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
C Interface: file_inv

Description: 


Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_FILE_CACHE_H__
#define __XTRFS_FILE_CACHE_H__

#include <pthread.h>

#include "file.h"
#include "xcap.h"
#include "xloc.h"
#include "hashtable.h"

#define FILE_CACHE_HASH_TABLE_SIZE   100

#ifndef LOCK
#define LOCK 1
#else
#error "LOCK was defined before!!! Need to rename it!!!"
#endif

#ifndef NO_LOCK
#define NO_LOCK 0
#else
#error "NO_LOCK was defined before!!! Need to rename it!!!"
#endif


/**
 * File inventory.
 *
 * This structure serves as an entry point to all the
 * files that this instance of xtreemfs is using or
 * has recently been. Files are identified by their
 * fileid within the file system which serves as a key
 * to the corresponding hash table.
 *
 * So far only an alias to 'struct hash_table' but during
 * the course of development there will be more to it.
 */
struct file_inv {
	struct hash_table files; /*!< Key is fileid, value is
				   'struct fileid_struct' */
	size_t max_entries;	 /*!< Maximum number of entries in inventory */

	mutex_t lock;		 /*!< The big lock for the whole inventory */
};

struct file_inv *file_inv_new();
int file_inv_init(struct file_inv *fc);
void file_inv_clear(struct file_inv *fc);
void file_inv_destroy(struct file_inv *fc);

void file_inv_add_fileid(struct file_inv *fc, struct fileid_struct *fs, int lock);
int file_inv_remove(struct file_inv *fc, struct fileid_struct *fs);
int file_inv_rename(struct file_inv *fc, char *from, char *to);

struct fileid_struct *file_inv_get_fileid(struct file_inv *fc, char *path, int lock);
struct fileid_struct *file_inv_get_by_path(struct file_inv *fc, char *path, int lock);

int file_inv_add_user_file(struct file_inv *fc, char *fullpath, int prots,
			   struct user_file *uf, int cache, struct dirservice *ds);
int file_inv_remove_user_file(struct file_inv *fc, struct user_file *uf);
int file_inv_release_user_file(struct file_inv *fi, struct user_file *uf);

struct hash_table_entry *file_inv_get_entry(struct file_inv *fc, char *path, int lock);

struct hash_table_entry *file_inv_entry_new(char *path,
					    struct fileid_struct *fs);

void file_inv_print(struct file_inv *fi);

#endif
