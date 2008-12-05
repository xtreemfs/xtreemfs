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
   C Interface: file_obj_cache

   Description: 


   Author: Matthias Hess <matthiash@acm.org>, (C) 2007

   Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_FOBJ_CACHE_H__
#define __XTRFS_FOBJ_CACHE_H__

#define FOBJ_DIRTY_BIT 0

#include <pthread.h>
#include <sys/types.h>
#include <time.h>

#include "radix-tree.h"
#include "list.h"
#include "lock_utils.h"

#define FOBJ_CACHE_INITIAL_SIZE (5*1024*1024)

struct fobj_cache_entry {
	int flags;
	time_t atime;	/*!< Last access time of this entry */
	void *data;	/*!< Data contained in the entry */
	long idx;	/*!< For inverse mapping from cache entry
			     to object id. This is needed for cache
			     flushing, for instance. */
	struct list_head lru;
};

struct fobj_cache_entry *fobj_cache_entry_new(long idx, long obj_size);
void fobj_cache_entry_destroy(struct fobj_cache_entry *fce);

void fobj_cache_entry_set_dirty(struct fobj_cache_entry *fce);
int fobj_cache_entry_is_dirty(struct fobj_cache_entry *fce);
int fobj_cache_entry_set_clean(struct fobj_cache_entry *fce);

enum fobj_cache_policy {
	FOBJ_CACHE_WRITE_THROUGH=0,
	FOBJ_CACHE_WRITE_BACK
};

struct fobj_cache {
	long obj_size;	/*!< Size of objects in cache */
	struct radix_tree_root entries;
	struct list_head lru;
	spinlock_t lru_lock;
	spinlock_t lock;
	long read_misses;
	long write_misses;
	enum fobj_cache_policy policy;
	time_t expires;			/*! Expiry time of this cache */
//	int enabled;			/*!< Cache switched on? */
	size_t size;
	size_t max_size;
};

struct fobj_cache *fobj_cache_new(long obj_size);
void fobj_cache_destroy(struct fobj_cache *fc);

struct fobj_cache_entry *fobj_cache_create(struct fobj_cache *fc, long obj_num);

struct fobj_cache_entry *fobj_cache_get(struct fobj_cache *fc, long obj_num);

struct fobj_cache_entry *fobj_cache_read(struct fobj_cache *fc,
					 long obj_num,
					 int *err);
struct fobj_cache_entry *fobj_cache_write(struct fobj_cache *fc,
					  long obj_num,
					  void *data,
					  int *err);
struct fobj_cache_entry *fobj_cache_write_partial(struct fobj_cache *fc,
						  long obj_num,
						  off_t off,
						  size_t size,
						  void *data,
						  int *err);
int fobj_cache_flush(struct fobj_cache *fc);

void fobj_cache_set_dirty(struct fobj_cache *fc, long obj_num);
int fobj_cache_is_dirty(struct fobj_cache *fc, long obj_num);
void fobj_cache_set_clean(struct fobj_cache *fc, long obj_num);

void fobj_cache_print_lru(struct fobj_cache *fc);

#endif
