/* Copyright (c) 2007, 2008  Erich Focht, Matthias Hess
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


#ifndef __XTRFS_OPEN_FILE_H__
#define __XTRFS_OPEN_FILE_H__
/**
 * Structures for keeping track of open files.
 *
 * @author: Erich Focht <efocht@hpce.nec.com>
 */

#ifdef DO_KERNEL
#include <asm/alternative.h>
#include <asm/atomic.h>
#else
#include "kernel_substitutes.h"
#endif

#include <pthread.h>

#include "request.h"
#include "xloc.h"
#include "xcap.h"
#include "hashtable.h"
#include "list.h"
#include "stripe.h"
#include "fobj_cache.h"

struct xlocs;


/**
 * FileID structure keeping track of all used instances of a file replica.
 *
 */

struct file_size {
	off_t size;
	unsigned int epoch;	/*!< Epoch of last update, maybe the same as 'order'? */
	atomic_t order;		/*!< order of the latest size update */
	atomic_t needs_updating;
};

struct fileid_struct {
	char *fileid;
	char *path;
	int protection;		/*!< decoded POSIX protection bits */
	/* extended attributes? */
	/* global user IDs ? VO IDs? */
	struct xcap *xcap;	/*!< Capabilities of this file */
	struct file_size sz;	/*!< Contains file size update info */
	int border;		/*!< Defines local requests order */
	spinlock_t lock;
	spinlock_t flush;	/*!< Locked if someone is flushing this file. */
	/* atomic_t use_count; */
	atomic_t to_be_deleted;	/*!< Entry is to be deleted from inventory. */
	atomic_t to_be_erased;	/*!< Entry is to be erased from file system. */
	struct list_head replica_list; /*!< list of file_replicas */
	struct hash_table_entry entry;
};

#define NEW_FILEID_ORDER(uf) \
    ((unsigned int)atomic_inc_return(&(uf)->file->fileid_s->sz.order))
#define GET_EPOCH(uf) \
    ((uf)->xcap->truncateEpoch)

/**
 * File replica object.
 * Created when opening a file, destroyed when last used instance is closed.
 * Holds several unparsed header strings used in communication requests
 * which need to be forwarded to MRC or OSDs.
 * All file replicas are members of a linked list inside a fileid_struct.
 */
struct file_replica {
	struct fileid_struct *fileid_s; /*!< pointer to parent structure */
	char *id;		/*!< Identify replica by id */

	struct transl_mod *mod;	/*!< Module to use for this replica */

	spinlock_t lock;	/*!< protect stripe_policy, osd_urls, lists */
	atomic_t use_count;	/*!< avoid freeing while used */

	/*
	  decoded info describing the currently used replica instance
	*/
	struct striping_policy sp;
	char **osd_ids;

	/* 
	   The XLocation instance is the first element in the xlocation
	   array and identifies the replica instance among all which have the
	   same fileid.
	*/
	char *xloc_inst;	/*!< pointer to XLocation instance string */

	struct fobj_cache *fobj_cache;	/*!< File object cache for this replica. */


	struct list_head ufiles; /*!< List with child file descriptors */
	struct list_head head;
};


struct file_replica *file_replica_new(struct fileid_struct *fileid,
				      char **osd_urls,
				      char *xloc_inst,
				      int cache);
int file_replica_is_the_same(struct file_replica *r1, struct xlocs *xl);
void file_replica_delete_user_files(struct file_replica *r);
void file_replica_destroy(struct file_replica *r);

int file_replica_cache_flush(struct file_replica *r);

void file_replica_print(struct file_replica *r);

/**
 * User file descriptor structure.
 *
 * User specific parts of file information, contains pointer to an
 * open_file structure which contains the details of a file which
 * are user independent.
 * [EF: maybe find a more appropriate name...]
 */
struct user_file {
	struct file_replica *file; /*!< pointer to parent structure */

	/* The XCap string is user dependent!
	   XLocation _can_ be user dependent, too
	*/

	/* So far, it seems, we only have 'global' xcaps. But we might have
	   in the future a 'per-open-file' xcap and therefore we add a reference
	   to a xcap structure here. This allows to deal with both cases. The
	   x-capability referenced here belongs to the xcap inventory anyway
	   and should be manipulated by functions of that inventory only.
	*/
	struct xcap *xcap;		/*!< latest capability seen */
	struct xlocs *xloc;		/*!< pointer to current xloc header */
	struct xloc_list *xloc_list;	/*!< Keep xloc list from MRC */
	/* is protection necessary? */
	spinlock_t lock;	/*!< protect xcap, xloc access */
	atomic_t use_count;		/*!< avoid freeing while used */
	/*
	   When a file with outstanding requests is closed (because the
	   owner process is killed), the outstanding requests need to be
	   cancelled.
	   Provide a linked list to outstanding fileop requests for reverse
	   lookup such that they can be deleted.
	   This is currently done by a pseudo-request structure: reqs
	*/
	struct req *reqs;  /*!< pseudo req struct used as parent req. */
	struct list_head head; /*!< chain into list of user_files belonging to a replica */
};

// resolve fileid in debug messages
#define UFILEID(u) (u)->file->fileid_s->fileid

extern struct fileid_struct *fileid_struct_new(char *fileid,
					       char *path,
					       int protection,
					       char *xnewsize,
					       off_t size);
extern int fileid_struct_init(struct fileid_struct *fs);
extern void fileid_struct_delete_replicas(struct fileid_struct *fs);
extern void fileid_struct_clear(struct fileid_struct *fs);
extern void fileid_struct_delete(struct fileid_struct *fs);
extern void fileid_struct_del_entry(struct hash_table_entry *hte);
#if 0
extern void fileid_struct_inc_uc(struct fileid_struct *fs);
extern void fileid_struct_dec_uc(struct fileid_struct *fs);
#endif

static inline void fileid_struct_inc_uc(struct fileid_struct *fs)
{
	atomic_inc(&fs->entry.use_count);
}

static inline void fileid_struct_dec_uc(struct fileid_struct *fs)
{
	atomic_dec(&fs->entry.use_count);
	if (atomic_read(&fs->entry.use_count) <= 0) {
		dbg_msg("Should delete fileid struct.\n");
		/* file_inv_remove(&file_inv, fs); */
	}
}

static inline int fileid_struct_read_uc(struct fileid_struct *fs)
{
	return atomic_read(&fs->entry.use_count);
}

void fileid_struct_print(struct fileid_struct *fileid);

/**
 * Get a fileid_struct structure for a path.
 *
 * Search for it in the hashtable. If not found, create a new one.
 * Increase use count atomically and avoid deletion (freeing) while
 * another thread is modifying it.
 * open() "gets" a file, close() "puts" it. Other accesses should be
 * enclosed between get and put.
 *
 * @param path the full file path
 * @param xloc_inst the xlocation instance JSON string
 * @return pointer to open file structure, newly allocated if not found in
 *         hashtable
 */
extern struct fileid_struct *get_fileid(char *path);
extern int put_fileid(struct fileid_struct *fid);


extern struct file_replica *get_file_replica(char *fileid, char *xloc_inst);
extern int put_file_replica(struct file_replica *replica);

extern struct user_file *get_user_file(char *path, char *xloc_inst);
extern int put_user_file(struct user_file *ufile);

/**
 * Allocate and init a user_file structure.
 *
 * @return user_file struct pointer or NULL if allocation failed.
 */
struct user_file *user_file_new(struct xcap *xcap,
				struct xlocs *xl,
				struct xloc_list *xll);

/**
 * Destroy and free a user_file structure.
 *
 * @return 0 if all went well, -EBUSY if user_file still in use.
 */
int user_file_destroy(struct user_file *uf);
int user_file_flush_cache(struct user_file *uf);

void user_file_print(struct user_file *uf);

#define file_lock(fileid)	\
{do {				\
	dbg_msg("Locking fileid %p using %p.\n", fileid, &fileid->lock); \
	spin_lock(&fileid->lock);	\
	dbg_msg("Locked fileid %p using %p.\n", fileid, &fileid->lock); \
} while(0); }

#define file_unlock(fileid)	\
{do {				\
	dbg_msg("Unlocking fileid %p using %p.\n", fileid, &fileid->lock); \
	spin_unlock(&fileid->lock);	\
	dbg_msg("Unlocked fileid %p using %p.\n", fileid, &fileid->lock); \
} while(0); }

#endif // __XTRFS_OPEN_FILE_H__
