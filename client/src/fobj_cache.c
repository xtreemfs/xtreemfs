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
 *  C Implementation: fobj_cache
 *
 * Description: 
 *
 *
 * Author: Matthias Hess <matthiash@acm.org>, (C) 2007
 *
 * Copyright: See COPYING file that comes with this distribution
 *
 */

#include <stdlib.h>
#include <string.h>

#include "fobj_cache.h"

#include "lock_utils.h"
#include "logger.h"

#ifndef ENTER
#define ENTER() dbg_msg("Entering.\n")
#endif

#ifndef LEAVE
#define LEAVE() dbg_msg("Leaving.\n")
#endif

inline void fobj_cache_entry_set_dirty(struct fobj_cache_entry *fce)
{
	fce->flags |= (1 << FOBJ_DIRTY_BIT);
}

inline int fobj_cache_entry_is_dirty(struct fobj_cache_entry *fce)
{
	return (fce->flags & (1 << FOBJ_DIRTY_BIT)) != 0;
}

inline int fobj_cache_entry_set_clean(struct fobj_cache_entry *fce)
{
	fce->flags = (fce->flags & ~(1 << FOBJ_DIRTY_BIT));
	return 0;
}

/**
 * Create a new entry for an object cache
 */
struct fobj_cache_entry *fobj_cache_entry_new(long idx, long obj_size)
{
	struct fobj_cache_entry *rv = NULL;
	
	ENTER();
	
	rv = (struct fobj_cache_entry *)malloc(sizeof(*rv));
	if (!rv)
		goto out;

	memset((void *)rv, 0, sizeof(*rv));
	rv->data = malloc(obj_size);
	if (!rv->data) {
		free(rv);
		rv = NULL;
		goto out;
	}
	rv->idx = idx;
	INIT_LIST_HEAD(&rv->lru);
	// pthread_spin_init(&rv->lock, PTHREAD_PROCESS_PRIVATE);
out:
	LEAVE();
	return rv;
}

void fobj_cache_entry_destroy(struct fobj_cache_entry *fce)
{
	free(fce->data);
	// pthread_spin_destroy(&fce->lock);
	free(fce);
}

struct fobj_cache *fobj_cache_new(long obj_size)
{
	struct fobj_cache *rv = NULL;
	
	rv = (struct fobj_cache *)malloc(sizeof(*rv));
	if (!rv)
		goto finish;

	rv->obj_size = obj_size;
	spin_init(&rv->lru_lock, PTHREAD_PROCESS_PRIVATE);
	spin_init(&rv->lock, PTHREAD_PROCESS_PRIVATE);
	INIT_RADIX_TREE(&rv->entries, 0);
	INIT_LIST_HEAD(&rv->lru);
	rv->read_misses = 0;
	rv->write_misses = 0;
	rv->policy = FOBJ_CACHE_WRITE_THROUGH;
	rv->expires = 0L;
	rv->size = 0L;
	rv->max_size = FOBJ_CACHE_INITIAL_SIZE;
	
finish:
	return rv;
}

void fobj_cache_clear(struct fobj_cache *fc)
{
	struct list_head *iter;

	while (!list_empty(&fc->lru)) {
		struct fobj_cache_entry *fce;
		iter = fc->lru.next;
		list_del(iter);
		fce = container_of(iter, struct fobj_cache_entry, lru);
		radix_tree_delete(&fc->entries, fce->idx);
		fobj_cache_entry_destroy(fce);
	}
}

void fobj_cache_destroy(struct fobj_cache *fc)
{
	fobj_cache_clear(fc);
	spin_destroy(&fc->lru_lock);
	spin_destroy(&fc->lock);
	free(fc);
}

struct fobj_cache_entry *fobj_cache_create(struct fobj_cache *fc, long obj_num)
{
	struct fobj_cache_entry *rv = NULL;

	ENTER();
	
	if (fc->size + fc->obj_size < fc->max_size) {
		rv = fobj_cache_entry_new(obj_num, fc->obj_size);
		if (!rv)
			goto finish;
		if (radix_tree_insert(&fc->entries, obj_num, (void *)rv)) {
			err_msg("Cannot insert cache entry into radix tree!\n");
			fobj_cache_entry_destroy(rv);
			rv = NULL;
			goto finish;
		}

		spin_lock(&fc->lru_lock);
		list_add_tail(&rv->lru, &fc->lru);
		spin_unlock(&fc->lru_lock);

		dbg_msg("Successfully created cache entry.\n");
	}

finish:
	LEAVE();
	return rv;
}

struct fobj_cache_entry *fobj_cache_get(struct fobj_cache *fc, long obj_num)
{
	struct fobj_cache_entry *rv = NULL;
	time_t now = time(NULL);

	ENTER();
	now = 0L;
	
	if (now <= fc->expires) {
		rv = (struct fobj_cache_entry *)radix_tree_lookup(&fc->entries, obj_num);
	} else
		dbg_msg("Cache has expired.\n");

	LEAVE();
	return rv;
}

/**
 * Read a file object from the cache.
 * @param fc File cache to search in
 * @param obj_num Object number
 * @param err Pointer to an error variable
 * @return NULL if object has not been found or cache is invalid
 *         The data contained in the cache otherwise.
 */
struct fobj_cache_entry *fobj_cache_read(struct fobj_cache *fc,
					 long obj_num,
					 int *err)
{
	struct fobj_cache_entry *fce = NULL;

	ENTER();
	
	*err = 0;
	fce = fobj_cache_get(fc, obj_num);
	if (!fce)
		goto finish;

	dbg_msg("Found cache entry at %X\n", fce);

	/* Update access time and lru */
	fce->atime = time(NULL);
	dbg_msg("Trying to acquire log.\n");
	spin_lock(&fc->lru_lock);
	info_msg("List before removal:\n");
	fobj_cache_print_lru(fc);
	list_del(&fce->lru);
	INIT_LIST_HEAD(&fce->lru);
	list_add_tail(&fce->lru, &fc->lru);
	dbg_msg("List after insertion.\n");
	fobj_cache_print_lru(fc);
	spin_unlock(&fc->lru_lock);
finish:
	LEAVE();
	return fce;
}

/**
 * Write data to the file object cache.
 * The user of this function must make sure he is allowed to store data
 * in the cache, i.e. the OSDs must have given write caching permission
 * before.
 * @param fc Cache to write to
 * @param obj_num Object number to write
 * @param data Data to be written into the cache.
 * @return 0 if successful, error code otherwise:
 *         1 No more space in cache -> needs removing of old entries
 */
struct fobj_cache_entry *fobj_cache_write(struct fobj_cache *fc,
					  long obj_num,
					  void *data,
					  int *err)
{
	struct fobj_cache_entry *fce = NULL;
	int is_new = 0;
	time_t now = time(NULL);
	
	ENTER();
	
	trace_msg("Try to write to cache.\n");
	
	*err = 0;
	now = 0L;

	if (now > fc->expires) {
		*err = 2;
		dbg_msg("Cache has expired.\n");
		goto finish;
	}
	
	fce = fobj_cache_get(fc, obj_num);
	if (!fce) {	/* Entry noy found, add to cache */
		if (fc->size + fc->obj_size < fc->max_size) {
			fce = fobj_cache_create(fc, obj_num);
			if (!fce) {
				*err = -ENOMEM;
				goto finish;
			}
			memcpy(fce->data, data, fc->obj_size);
			is_new = 1;
		} else {
			dbg_msg("Cache full!\n");
			*err = 1;
			goto finish;
		}
	}

	/* Mark entry as dirty */
	fobj_cache_entry_set_dirty(fce);

	fce->atime = time(NULL);
	dbg_msg("Trying to acquire lock.\n");
	spin_lock(&fc->lru_lock);
	dbg_msg("List before removal:\n");
	fobj_cache_print_lru(fc);
	list_del(&fce->lru);
	INIT_LIST_HEAD(&fce->lru);
	list_add_tail(&fce->lru, &fc->lru);
	dbg_msg("List afrer insertion.\n");
	fobj_cache_print_lru(fc);
	spin_unlock(&fc->lru_lock);
finish:
	LEAVE();
	return fce;
}

struct fobj_cache_entry *fobj_cache_write_partial(struct fobj_cache *fc,
						  long obj_num,
						  off_t off,
						  size_t size,
						  void *data,
						  int *err)
{
	struct fobj_cache_entry *rv = NULL;
	char *buf;

	/* Check if by any chance we are still writing whole objects */
	if (off == 0L && size == fc->obj_size)
		return fobj_cache_write(fc, obj_num, data, err);

	rv = fobj_cache_get(fc, obj_num);
	if (!rv)
		rv = fobj_cache_create(fc, obj_num);
	if (!rv)
		goto finish;

	buf = (char *)rv->data;
	memcpy((void *)&buf[off], data, size);

finish:
	return rv;
}


/**
 * Mark an object in the cache as dirty.
 */
void fobj_cache_set_dirty(struct fobj_cache *fc, long obj_num)
{
}

/**
 * Clear dirty bit in an cache entry.
 */
void fobj_cache_set_clean(struct fobj_cache *fc, long obj_num)
{
}

void fobj_cache_print_lru(struct fobj_cache *fc)
{
	struct list_head *iter;

	list_for_each(iter, &fc->lru) {
		struct fobj_cache_entry *fce;
		fce = container_of(iter, struct fobj_cache_entry, lru);
		info_msg("%ld:    %ld\n", fce->idx, fce->atime);
	}
}
