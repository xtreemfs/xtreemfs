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
 *  C Implementation: file_inv
 *
 * Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007
 *
 * Copyright: See COPYING file that comes with this distribution
 *
 * Description:
 * The file inventory handles all file structure related aspects of
 * the file system. There are actually three layers of file related
 * strutures:
 * 1) A structure describing the actual file: fileid_struct
 * 2) A structure for each replica of a specific file: file_replica
 * 3) A structure for open files or files the client code has seen
 *    during processing: user_file.
 *
 * There will be only one file inventory for all threads that make up
 * the client code, so the file inventory must be protected.
 * The general idea to keep the inventory consistent is to be able
 * to lock entries in the inventory and indicate their use with a
 * use counter.
 * If a thread wants to change a value in an entry it must acquire
 * the entries lock. If a thread uses an entry for example in fuse's
 * file_info struct the corresponding use counter will be increased
 * s.t. a delete request from another thread does not succeed.
 * Such delete operations must not be executed outside the inventory
 * but only with the inventory's own functions.
 *
 * Adding an entry to the inventory does not increase its use count.
 * If someone wants to add an entry and still uses it after inserting
 * into the inventory he must increase the use count himself.
 * This opens up the possibility to have entries which are currently
 * not used but might be used in the future (indicated by a use count > 0)
 * If there is a need to remove entries (for instance if a maximum
 * number of entries is reached) the zero use count entries can safely
 * be removed.
 * An example is a stat operation for a file. When the file system
 * execs stat it might use the entry in later operations but stat is
 * only an operation like 'look at it' and does not lead to 'use it'.
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>

#include "file_inv.h"
#include "dirservice.h"
#include "stripingpolicy.h"
#include "xloc.h"
#include "xcap_inv.h"
#include "xcap.h"
#include "list.h"
#include "hashtable.h"

#include "lock_utils.h"
#include "logger.h"


hash_idx_t
file_inv_hash_func(void *key)
{
	hash_idx_t rv = -1;
	char *fileid = (char *)key;
	int i;

	rv = 0;
	for(i=0; i<strlen(fileid); i++)
		rv += fileid[i];

	return rv;
}

int
file_inv_hash_cmp(void *key1, void *key2)
{
	return !strcmp((char *)key1, (char *)key2);
}


struct file_inv *
file_inv_new()
{
	struct file_inv *rv = NULL;

	rv = (struct file_inv *)malloc(sizeof(struct file_inv));
	if(rv != NULL && file_inv_init(rv) != 0) {
		file_inv_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int
file_inv_init(struct file_inv *fc)
{
	int rv = 0;

	rv = hash_table_init(&fc->files, FILE_CACHE_HASH_TABLE_SIZE,
		   	     file_inv_hash_func, file_inv_hash_cmp,
			     fileid_struct_del_entry);
	pthread_mutex_init(&fc->lock, NULL);

	return rv;
}

void
file_inv_clear(struct file_inv *fc)
{
	struct list_head *iter;
	struct hash_table_entry *hte;
	struct fileid_struct *fileid = NULL;
	int i;

	// pthread_mutex_lock(&fc->lock);

	for(i=0; i<fc->files.size; i++) {
		while(!list_empty(&fc->files.entries[i])) {
			iter = fc->files.entries[i].next;
			hte = (struct hash_table_entry *)iter;
			fileid = (struct fileid_struct *)
				container_of(hte, struct fileid_struct, entry);
			fileid_struct_delete_replicas(fileid);
			list_del(iter);
			fileid_struct_delete(fileid);
		}
	}
	hash_table_clear(&fc->files);

	// pthread_mutex_unlock(&fc->lock);

}


void
file_inv_destroy(struct file_inv *fc)
{
	file_inv_clear(fc);
	pthread_mutex_destroy(&fc->lock);
	free(fc);
}


/**
 * Add an existing fileid_struct to the inventory. The key
 * is the fileid which is assumed to exist for every new
 * fileid. This implicates that all fileid structs can be
 * created only after contacting the MRC (as this is the
 * entity that holds the authoritative information)
 *
 * The caller can hold the lock and indicates with a set
 * use_count if he still needs the passed fileid_struct.
 */
void
file_inv_add_fileid(struct file_inv *fc, struct fileid_struct *fs, int lock)
{
	hash_table_insert_entry(&fc->files, &fs->entry, LOCK);
}


/**
 * Delete a fileid_struct from the inventory.
 * This functions removes a given fileid struct from the inventory,
 * if it is found. The caller has to make sure that the use_count of
 * that fileid_struct is really zero and no locks are held on the
 * fileid_struct.
 */
int
file_inv_remove(struct file_inv *fc, struct fileid_struct *fs)
{
	int rv = 0;

	hash_table_remove_entry(&fc->files, &fs->entry);

	return rv;
}


int
file_inv_rename(struct file_inv *fc, char *from, char *to)
{
	int rv = 0;
	struct fileid_struct *fileid;
	struct hash_table_entry *hte;

	dbg_msg("Looking for file '%s'\n", from);

	fileid = file_inv_get_by_path(fc, from, LOCK);
	if (fileid) {
		hash_table_lock_row(&fc->files, fileid->fileid);
		hte = hash_table_find(&fc->files, (void *)fileid->fileid, NO_LOCK);
		
		if (hte) {
			list_del(&hte->head);
			atomic_dec(&hte->use_count);	/* Entry no longer in list */
			fileid = container_of(hte, struct fileid_struct, entry);
			free(fileid->path);
			fileid->path = strdup(to);
			dbg_msg("Re-inserting fileid struct.\n");
			hash_table_insert_entry(&fc->files, hte, NO_LOCK);
			atomic_dec(&hte->use_count);	/* Must be removed because we
							   had an increase in use count
							   due to 'find' operation. */
		}
		hash_table_unlock_row(&fc->files, fileid->fileid);
		fileid_struct_dec_uc(fileid);	/* Necessary because of 'get_by_path' */
	} else {
		dbg_msg("File '%s' not found in inventory.\n", from);
		rv = ENOENT;
	}

	return rv;
}


/**
 * Get an entry specified by fileid from the inventory.
 * This function will lock the entry if it is in the inventory.
 * The caller must release this lock when he no longer needs
 * it.
 */
struct fileid_struct *
file_inv_get_fileid(struct file_inv *fc, char *fileid, int lock)
{
	struct fileid_struct *rv = NULL;
	struct hash_table_entry *res;

	res = hash_table_find(&fc->files, (void *)fileid, LOCK);
	if (res) {
		rv = container_of(res, struct fileid_struct, entry);
	}
	return rv;
}


/**
 * Find an inventory entry by path.
 * As the path is not the key of the hash entries and as there
 * are no other orderings, this operation is very costly and should
 * be used as little as possible.
 * Besides, the entry -- if found -- is locked and must be released
 * by the caller.
 */
struct fileid_struct *
file_inv_get_by_path(struct file_inv *fc, char *path, int lock)
{
	struct fileid_struct *rv = NULL, *ifs;
	struct hash_table *ht;
	struct hash_table_entry *hte;
	struct list_head *iter;

	int i;

	dbg_msg("file_inv %p path=%s\n", fc, path);
	ht = &fc->files;

	/* In order to find the entry corresponding to 'path' we walk through
	   the whole hash table in a linear fashion. As there is no ordering
	   there is no way around that. We also have to lock the whole hash
	   table for this operation, so this should be avoided.
	 */
	// hash_table_lock(ht);

	for(i=0; i<ht->size && rv == NULL; i++) {
		spin_lock(&ht->locks[i]);
		list_for_each(iter, &ht->entries[i]) {
			hte = list_entry(iter, struct hash_table_entry, head);
			ifs = container_of(hte, struct fileid_struct, entry);
			if (ifs->path) {
				dbg_msg("Comparing '%s' with '%s'\n", path, ifs->path);
			if(!strcmp(path, ifs->path)) {
				rv = ifs;
					atomic_inc(&hte->use_count);
					break;
				}
			} else {
				err_msg("IFS path should not be null.\n");
			}
		}
		spin_unlock(&ht->locks[i]);
	}
	// hash_table_unlock(ht);

	dbg_msg("Continuing...\n");

	if (rv) {
		dbg_msg("Trying to lock fileid lock.\n");
		dbg_msg("got the fileid lock.\n");
	}

	return rv;
}


/**
 * Add a user file to the inventory.
 */
int file_inv_add_user_file(struct file_inv *fc, char *fullpath, int prots,
			   struct user_file *uf, int cache, struct dirservice *ds)
{
	int err = 0;
	struct fileid_struct *fileid;
	struct file_replica *replica, *ri;
	struct xcap *xcap;
	struct xlocs *xloc;
	struct list_head *iter;
	char *this_xloc;

	dbg_msg("fc=%p fullpath=%s uf=%p\n", fc, fullpath, uf);
	xcap = uf->xcap;
	xloc = uf->xloc;
	this_xloc = xloc->repr;

	/* dbg_msg("this_xloc = %s fc=%p fullpath=%s uf=%p\n", this_xloc, fc,
		fullpath, uf); */

	/* Find the right fileid_struct for this file, if an entry is returned
	   the entry is locked and the use count is increased by one. */
	/* hash_table_lock_row(&fc->files, xcap->fileID); */
	fileid = file_inv_get_fileid(fc, xcap->fileID, 0);
	if (!fileid) {
		dbg_msg("File ID not found in cache, creating new one.\n");
		fileid = fileid_struct_new(xcap->fileID,
					   fullpath,
					   prots,
					   NULL,
					   0);

		if (!fileid) {
			err = 1;
			/* hash_table_unlock_row(&fc->files, xcap->fileID); */
			goto finish;
		}
		dbg_msg("Locking fileid struct.\n");

		/* Keep state of fileid consistent with later 'atomic_dec' */
		fileid_struct_inc_uc(fileid);
		file_inv_add_fileid(fc, fileid, NO_LOCK);
	} else {
		dbg_msg("Found fileid in inventory.\n");
	}

	spin_lock(&fileid->lock);

	/* hash_table_unlock_row(&fc->files, xcap->fileID); */

	/* At this point, the fileid struct is still locked.*/

	/* 'fileid' now points to the right 'fileid_struct'. Now let's
	   see if we already have a replica for this file */
	replica = NULL;
	list_for_each(iter, &fileid->replica_list) {
		ri = container_of(iter, struct file_replica, head);
		dbg_msg("Comparing replica '%s' with '%s'\n",
			ri->xloc_inst, this_xloc);
		if (!strcmp(ri->xloc_inst, this_xloc)) {
			replica = ri;
			dbg_msg("assumed to be equal!\n");
			break;
		} else {
			dbg_msg("not equal\n");
		}
	}

	if (!replica) {
		dbg_msg("No replica for %s. Creating it...\n", fileid->fileid);
		replica = file_replica_new(fileid, NULL, xloc->repr, cache);
		err = xloc_inst_parse(replica, xloc->repr, ds);
		if (err) {
			err_msg("Some error occured for %s xloc=%s\n",
				fileid->fileid, this_xloc);
			err_msg("The replica might be inconsistent!!!\n");
		}
		fileid_struct_inc_uc(fileid);
		list_add_tail(&replica->head, &fileid->replica_list);
	} else {
		dbg_msg("Found replica in inventory.\n");
	}

	spin_unlock(&fileid->lock);
	fileid_struct_dec_uc(fileid);	/* Decreasing count from 'get' operation */
	dbg_msg("Unlocking fileid %p\n", fileid);

	/* Consistency check... */
	if (replica->fileid_s != fileid) {
		err_msg("The file structure of the replica is not identical to the one I figured out in the first step.\n");
		err = 15;
	}
	uf->file = replica;
	spin_lock(&replica->lock);
	list_add_tail(&uf->head, &replica->ufiles);
	atomic_inc(&replica->use_count);
	spin_unlock(&replica->lock);
finish:
	return err;
}

/**
 *
 */
int
file_inv_remove_user_file(struct file_inv *fc, struct user_file *uf)
{
	int err = 0;
	struct fileid_struct *fileid = uf->file->fileid_s;
	struct fileid_struct *nfid = NULL;
	struct file_replica *replica = NULL;

	dbg_msg("Locking fileid %p.\n", fileid);
	spin_lock(&fileid->lock);

	if (atomic_read(&uf->use_count) > 0) {
		err_msg("Not removing user file. File is still in use.\n");
		/* spin_unlock(&fileid->lock); */
		goto finish;
	}

	replica = uf->file;
	spin_lock(&replica->lock);
	list_del(&uf->head);
	atomic_dec(&replica->use_count);
	spin_unlock(&replica->lock);

	user_file_flush_cache(uf);
	xcap_inv_remove(&xcap_inv, uf->xcap);
	user_file_destroy(uf);

	dbg_msg("Use count of replica: %d\n", atomic_read(&replica->use_count));

	spin_lock(&replica->lock);
	if (atomic_read(&replica->use_count) <= 0) {
		if (atomic_read(&replica->use_count) < 0) {
			dbg_msg("Warning: Use counter for replica '%s' is less than zero!\n",
				replica->xloc_inst);
		}
		nfid = replica->fileid_s;
		if (nfid != fileid) {
			err_msg("Inconsistency in deleting file ids!\n");
			/* dbg_msg("Continuing with the replica fileid!\n"); */
		}
		list_del(&replica->head);
		fileid_struct_dec_uc(fileid);
		dbg_msg("Deleting replica '%s'.\n", replica->xloc_inst);
		file_replica_destroy(replica);

			spin_unlock(&fileid->lock);

		if (fileid_struct_read_uc(fileid) <= 0)
			file_inv_remove(fc, fileid);
		else {
			dbg_msg("Fileid is %d time in use. Unlocking fileid %p\n",
				fileid_struct_read_uc(fileid), fileid);
		}

	} else {
		dbg_msg("Unlocking fileid %p\n", fileid);
		spin_unlock(&replica->lock);
		spin_unlock(&fileid->lock);
	}

finish:
	return err;
}


/**
 * Release a user file structure (eg. when a file is closed)
 * @param fi File Inventory
 * @param uf User file to be released
 * @return 0 on success, 1 if file has been removed
 */
int file_inv_release_user_file(struct file_inv *fi, struct user_file *uf)
{
	int rv = 0;

	/* Use counter must be decreased */
	dbg_msg("User file count before decreasing: %d\n", atomic_read(&uf->use_count));
	atomic_dec(&uf->use_count);
	dbg_msg("User file count after decreasing: %d\n", atomic_read(&uf->use_count));

	if (atomic_read(&uf->use_count) <= 0) {
		file_inv_remove_user_file(fi, uf);
		rv = 1;
	}
	return rv;
}


struct hash_table_entry *
file_inv_get_entry(struct file_inv *fc, char *fileid, int lock)
{
	return hash_table_find(&fc->files, (void *)fileid, lock);
}


void
file_inv_print(struct file_inv *fi)
{
	struct hash_table_iter iter;
	struct hash_table_entry *hte;
	struct fileid_struct *fileid;
	struct list_head *liter, *liter2;
	struct list_head *ufiter, *ufiter2;
	struct file_replica *replica;
	struct user_file *uf;

	ht_for_each(iter, &fi->files) {
		hte = container_of(iter.list_iter, struct hash_table_entry, head);
		fileid = container_of(hte, struct fileid_struct, entry);
		fileid_struct_print(fileid);

		/* Print replica list... */
		list_for_each_safe(liter, liter2, &fileid->replica_list) {
			replica = container_of(liter, struct file_replica, head);
			file_replica_print(replica);

			list_for_each_safe(ufiter, ufiter2, &replica->ufiles) {
				uf = container_of(ufiter, struct user_file, head);
				user_file_print(uf);
			}
		}
	}
}

