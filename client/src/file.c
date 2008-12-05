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
 *  C Implementation: file
 *
 * Description: 
 *
 *
 * Authors: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007
 *          Erich Focht <efocht@hpce.nec.com>
 *
 * Copyright: See COPYING file that comes with this distribution
 *
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>

#include <openssl/sha.h>

#include "xtreemfs.h"
#include "xtreemfs_utils.h"

#include "file.h"
#include "filerw.h"
#include "stripe.h"
#include "stripingpolicy.h"
#include "xcap_inv.h"
#include "xcap.h"
#include "xloc.h"
#include "list.h"

#include "lock_utils.h"
#include "logger.h"


/**
 * Allocate a new fileid structure.
 * It is assumed that fileid_structs have a use count of zero on
 * creation. This indicates that they could be removed from the
 * inventory if they were to be put into it.
 * When someone retrieves entities from the inventory, these entities
 * get an increase in use_count to indicate that someone uses that entry.
 * So if a file is to be inserted into the inventory the inserter
 * has to increase the use_count before doing so.
 */
struct fileid_struct *
fileid_struct_new(char *fileid, char *path, int protection, char *xnewsize,
		  off_t size)
{
	struct fileid_struct *rv = NULL;
	
	rv = (struct fileid_struct *)malloc(sizeof(struct fileid_struct));
	if (rv) {
		memset((void *)rv, 0, sizeof(struct fileid_struct));
		if (fileid)
			rv->fileid = strdup(fileid);
		if(path)
			rv->path = strdup(path);
		rv->protection = protection;
/*		if (xnewsize)
			rv->xnewsize = strdup(xnewsize);*/
		rv->sz.size = size;
		
		if (   (fileid && !rv->fileid)
		    || (path && !rv->path)) {
			fileid_struct_delete(rv);
			rv = NULL;
			goto finish;
		}

		spin_init(&rv->lock, PTHREAD_PROCESS_PRIVATE);
		spin_init(&rv->flush, PTHREAD_PROCESS_PRIVATE);
		atomic_set(&rv->sz.needs_updating, 0);
		atomic_set(&rv->sz.order, 0);
		/* atomic_set(&rv->use_count, 0); */
		atomic_set(&rv->to_be_deleted, 0);
		atomic_set(&rv->to_be_erased, 0);
		INIT_LIST_HEAD(&rv->replica_list);

		hash_table_entry_init(&rv->entry, rv->fileid);
	}
	
finish:
	return rv;
}


#if 0
int
fileid_struct_init(struct fileid_struct *fs)
{
	fs->fileid = NULL;
	fs->path = NULL;
	fs->protection = 0;
	fs->xnewsize = NULL;
	
	spin_init(&fs->lock, PTHREAD_PROCESS_PRIVATE);
	
	fs->sz.size = 0;
	fs->sz.order = 0;
	atomic_set(&fs->sz.needs_updating, 0);
	atomic_set(&fs->order, 0);
	atomic_set(&fs->use_count, 0);
	INIT_LIST_HEAD(&fs->replica_list);
	return 0;
}
#endif


void
fileid_struct_delete_replicas(struct fileid_struct *fs)
{
	struct list_head *iter;
	struct file_replica *replica = NULL;
	
	while(!list_empty(&fs->replica_list)) {
		iter = fs->replica_list.next;
		replica = container_of(iter, struct file_replica, head);
		list_del(iter);
		file_replica_delete_user_files(replica);
		spin_lock(&replica->lock);
		file_replica_destroy(replica);
	}
}


void
fileid_struct_clear(struct fileid_struct *fs)
{
	if (list_empty(&fs->replica_list))
		dbg_msg("No active replica for file '%s'\n", fs->fileid);
	fileid_struct_delete_replicas(fs);
	free(fs->fileid);
	free(fs->path);
	// free(fs->xnewsize);
	/* Eventually clear replica list, too. */
}

void fileid_struct_delete(struct fileid_struct *fs)
{
	fileid_struct_clear(fs);
	if (spin_destroy(&fs->lock))
		err_msg("Spin still in use on destroy!\n");
	spin_destroy(&fs->flush);
	free(fs);
}

void fileid_struct_del_entry(struct hash_table_entry *hte)
{
	struct fileid_struct *fileid;

	fileid = container_of(hte, struct fileid_struct, entry);
	hash_table_entry_del_content(hte);
	fileid_struct_delete(fileid);
}

void fileid_struct_print(struct fileid_struct *fileid)
{
	spin_lock(&fileid->lock);

	info_msg("File '%s'\n", fileid->path);
	info_msg("\tID:   %s\n", fileid->fileid);
	info_msg("\tSize: %d\n", fileid->sz.size);
	info_msg("\tUse count: %d\n", fileid_struct_read_uc(fileid));
 
	spin_unlock(&fileid->lock);
}


struct file_replica *file_replica_new(struct fileid_struct *fileid,
				      char **osd_ids,
				      char *xloc_inst,
				      int cache)
{
	struct file_replica *rv = NULL;
	struct json_object *jo;
	struct striping_policy *sp;
	SHA_CTX ctx;
	unsigned char digest[SHA_DIGEST_LENGTH];
 	char *fid = NULL;

	rv = (struct file_replica *)malloc(sizeof(struct file_replica));
	if (!rv)
		goto finish;
	
	memset((void *)rv, 0, sizeof(struct file_replica));
	rv->fileid_s = fileid;
	if (fileid == NULL) {
		dbg_msg("Warning: Fileid for replica is NULL\n");
	} else {
		fid = fileid->fileid;
	}

	spin_init(&rv->lock, PTHREAD_PROCESS_PRIVATE);
	atomic_set(&rv->use_count, 0);
	INIT_LIST_HEAD(&rv->ufiles);
	INIT_LIST_HEAD(&rv->head);
	rv->osd_ids = osd_ids;
	rv->xloc_inst = strdup(xloc_inst);

	jo = json_tokener_parse(xloc_inst);
	sp = json_to_striping_policy(json_object_array_get_idx(jo, 0));
	if (cache)
		rv->fobj_cache = fobj_cache_new(sp->stripe_size);

	/* And calculate replica id. This is just the SHA1 sum for the string
	   <file id><xloc> */
	SHA1_Init(&ctx);
	if (fid != NULL)
		SHA1_Update(&ctx, (void *)fid, strlen(fid));
	SHA1_Update(&ctx, (void *)xloc_inst, strlen(xloc_inst));
	SHA1_Final(digest, &ctx);

	rv->id = sha1_digest_to_str(digest);
	dbg_msg("Replica ID: %s\n", rv->id);

	/* And find translation module to use. */
	rv->mod = transl_engine_find_by_type(&transl_engine,
					     sp->id);
	if (rv->mod) {
		dbg_msg("Found a translation module!\n");
	}

	free(sp);
	json_object_put(jo);
	
finish:
	return rv;
}

int file_replica_is_the_same(struct file_replica *r1, struct xlocs *xl)
{
	int rv = 0;
	int i;
	
	/* Compare striping policy */
	rv = striping_policy_cmp(&r1->sp, xl->sp);

	if (rv) {
		/* Compare OSDs. This is based on their respective UUIDs */
		for(i = 0; i < r1->sp.width; i++) {
			if (strcmp(r1->osd_ids[i], xl->osds[i])) {
				rv = 0;
				break;
			}
		}
	}
	return rv;
}

void file_replica_delete_user_files(struct file_replica *r)
{
	struct list_head *iter;
	struct user_file *uf = NULL;
	
	spin_lock(&r->lock);

	while(!list_empty(&r->ufiles)) {
		iter = r->ufiles.next;
		uf = container_of(iter, struct user_file, head);
		spin_lock(&uf->lock);
		list_del(iter);
		spin_unlock(&uf->lock);
		user_file_destroy(uf);
	}

	spin_unlock(&r->lock);
}


void file_replica_destroy(struct file_replica *r)
{
	int i;
	
	for(i=0; i<r->sp.width; i++)
		free(r->osd_ids[i]);
	free(r->osd_ids);
	free(r->xloc_inst);
	/* Striping policy does not require to be freed. */
	if (r->fobj_cache) {
		// file_replica_cache_flush(r);
		/* Eventually the last user file might be flushing the
		   cache, so try to acquire a lock before actually
		   deleting the cache. */
		spin_lock(&r->fobj_cache->lock);
		spin_unlock(&r->fobj_cache->lock);
		if (r->fobj_cache)
			fobj_cache_destroy(r->fobj_cache);
	}
	free(r->id);
	
	dbg_msg("freeing replica %p\n", r);
	spin_unlock(&r->lock);
	spin_destroy(&r->lock);
	free(r);
}


void file_replica_dec_uc(struct file_replica *r)
{
	struct fileid_struct *fileid = r->fileid_s;
	
	spin_lock(&r->lock);
	atomic_dec(&r->use_count);
	if (atomic_read(&r->use_count) <= 0) {
		dbg_msg("Deleting replica.\n");
		file_replica_destroy(r);
		fileid_struct_dec_uc(fileid);
	} else {
		spin_unlock(&r->lock);
	}
}

int file_replica_cache_flush(struct file_replica *r)
{
	int err = 0;
	struct fobj_cache *fc = r->fobj_cache;
	struct list_head *iter;
	struct fobj_cache_entry *fce;
	
	list_for_each(iter, &fc->lru) {
		fce = container_of(iter, struct fobj_cache_entry, lru);
		
	}

	return err;
}

void file_replica_print(struct file_replica *r)
{
	info_msg("\t\txloc inst:        %s\n", r->xloc_inst);
	info_msg("\t\tuse count:        %d\n", atomic_read(&r->use_count));
}


/**
 * Allocate and init a user_file structure.
 *
 * @return user_file struct pointer or NULL if allocation failed.
 */
struct user_file *user_file_new(struct xcap *xcap,
				struct xlocs *xl,
				struct xloc_list *xll)
{
	struct user_file *uf;

	uf = (struct user_file *)malloc(sizeof(struct user_file));
	memset((void *)uf, 0, sizeof(struct user_file));
	if (xcap)
		uf->xcap = xcap;
	if (xl)
		uf->xloc = xl;
	if (xll)
		uf->xloc_list = xll;
	spin_init(&uf->lock, PTHREAD_PROCESS_PRIVATE);
	atomic_set(&uf->use_count, 0);
	INIT_LIST_HEAD(&uf->head);

	return uf;
}

/**
 * Destroy and free a user_file structure.
 *
 * @return 0 if all went well, -EBUSY if user_file still in use.
 */
int user_file_destroy(struct user_file *uf)
{
	int err = 0;
	int val;
		
	spin_lock(&uf->lock);
	dbg_msg("Destroying user file with location '%s'\n", uf->xloc->repr);
	
	if ((val = atomic_read(&uf->use_count)) > 0) {
		dbg_msg("User file is still in use. Count is %d\n", val);
		spin_unlock(&uf->lock);
		err = -EBUSY;
		goto err;
	}

	if (uf->reqs) {
#if 0
		if (list_empty(&uf->reqs->children_list))
			free(uf->reqs);
		else {
			dbg_msg("User file still has unfinshed child requests.\n");
			err = -EBUSY;
		}
#endif
	}
	if (uf->xloc)
		xlocs_delete(uf->xloc);
	if (uf->xloc_list)
		xloc_list_delete(uf->xloc_list);
	
	spin_unlock(&uf->lock);
	spin_destroy(&uf->lock);
	
	if (!err) {
		/* xcap no longer belongs to us! */
		dbg_msg("freeing user_file %p\n", uf);
		free(uf);
	}
 err:
	return err;
}

int user_file_flush_cache(struct user_file *uf)
{
	int err = 0;
	struct fileid_struct *fileid = uf->file->fileid_s;
	struct fobj_cache *fc;
	struct fobj_cache_entry *fce;
	struct list_head *iter;
	struct req *req;
	off_t new_size, real_size, right_size;
	
	dbg_msg("Flushing file cache.\n");
	
	if (!uf->file) {
		dbg_msg("No replica given for user file.\n");
		goto finish;
	}

	fc = uf->file->fobj_cache;
	if (!fc) {
		dbg_msg("No file cache for replica.\n");
		goto finish;
	}

	/* Because the OSD does not necessarily know about the right file
	   size, we must keep the right size here. If caching is allowed
	   only the cache knows about it (unless it is a read only cache) */
	spin_lock(&fc->lock);
	right_size = fileid->sz.size;
	list_for_each(iter, &fc->lru) {
		fce = container_of(iter, struct fobj_cache_entry, lru);
		if (fobj_cache_entry_is_dirty(fce)) {
			// pthread_spin_lock(&fce->lock);
			dbg_msg("Creating flush request for object %ld with size %ld\n", fce->idx, fc->obj_size);
			real_size = fc->obj_size;
			new_size = (off_t)(fce->idx + 1) * (off_t)fc->obj_size;
			// Locking of fileid required?
			if (right_size < new_size) { /* We have the last object! */
				real_size = (off_t)fc->obj_size - (new_size - right_size);
			}
			if (real_size < 0) {
				dbg_msg("Real size is less than zero. This is wrong!\n");
				dbg_msg("New size - right size = %ld - %ld = %ld\n",
					new_size, right_size, new_size - right_size);
				err = 1;
				break;
			}
			dbg_msg("Real size is: %ld, file size is %ld\n", real_size, fileid->sz.size);
			// pthread_spin_unlock(&fce->lock);
			req = fobj_req_create(REQ_FOBJ_WRITE, fce->idx,
					      0L, real_size, fce->data,
					      uf, 0, NULL);
			dbg_msg("Waiting for request.\n");
			err = req_wait(req);
			if (err) {
				err_msg("Cannot flush cache!\n");
				goto finish;
			}
			dbg_msg("Request finished.\n");
			dbg_msg("File size after request: %ld\n", fileid->sz.size);
			fobj_cache_entry_set_clean(fce);
		}
	}

	/* Restore file size for further use... */
	fileid->sz.size = right_size;
	spin_unlock(&fc->lock);
finish:
	return err;
}

void
user_file_print(struct user_file *uf)
{
	info_msg("                xlocs: '%s'\n", uf->xloc->repr);
	info_msg("                xcap:  '%s'\n", uf->xcap->repr);
	info_msg("                use count: %d\n", atomic_read(&uf->use_count));
}
