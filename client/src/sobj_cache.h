/* This file is part of the XtreemFS client.

   XtreemFS client is free software: you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation, either version 2 of
   the License, or (at your option) any later version.

   The XtreemFS client is distributed in the hope that it will be
   useful, but WITHOUT ANY WARRANTY; without even the implied warranty
   of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with XtreemFS client.  If not, see <http://www.gnu.org/licenses/>.
 */


/*
   C Interface: sobj_cache

   Description: 


   Author: Matthias Hess <mhess@laptopmh.stg.hpce.tld>, (C) 2008

   Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_SOBJ_CACHE_H__
#define __XTRFS_SOBJ_CACHE_H__

#include <osd_proxy.h>

#include "radix-tree.h"
#include "file.h"

enum sobj_cache_entry_state {
	SCE_UNDEFINED = 0,
	SCE_VALID,
};

/**
 * Entry for a replica in the stripe object cache
 * All stripes belonging to one replica can be accessed
 * via this entry.
 */
struct sobj_cache_repl_entry {
	struct file_replica          *replica;
	enum sobj_cache_entry_state   state;
	
	struct radix_tree_root        entries;	/*!< Cached objects that belong to this replica
						     referenced by obj id */
	struct hash_table_entry	      hentry;	/*!< Entry in hash table */
};

struct sobj_cache_repl_entry *sob_cache_repl_entry_new(struct file_replica *replica);

int sobj_cache_repl_entry_init(struct sobj_cache_repl_entry *scre,
			       struct file_replica *replica);
void sobj_cache_repl_entry_del_contents(struct sobj_cache_repl_entry *scre);
void sobj_cache_repl_entry_destroy(struct sobj_cache_repl_entry *scre);

/**
 * Data entry in the stripe object cache
 * This entry contains actual data that is stored in
 * the cache.
 */
struct sobj_cache_data_entry {
	void *data;
	int   version;
	enum sobj_cache_entry_state state;
	struct lease *lease;		/*!< Pointer to associated lease (if any) */
	struct list_head head;
};

struct sobj_cache_data_entry *sobj_cache_data_entry_new(int obj_size);
int sobj_cache_data_entry_init(struct sobj_cache_data_entry *scde,
			       int obj_size);
void sobj_cache_data_entry_del_contents(struct sobj_cache_data_entry *scde);
void sobj_cache_data_entry_destroy(struct sobj_cache_data_entry *scde);


/**
 * Stripe object cache
 *
 * Objects are organized by replica first and then their object id
 * within that replica.
 */
struct sobj_cache {
	/* Container for replica specific information */
	struct hash_table *replica;

	/* Object handling. Objects are identified by fileid
	   and object number. */
};

extern struct sobj_cache *sobj_cache_new();
extern int sobj_cache_init(struct sobj_cache *soc);
extern void sobj_cache_del_contents(struct sobj_cache *soc);
extern void sobj_cache_destroy(struct sobj_cache *soc);


/* Functions callable by modules */

/* Functions to be used by 'lower' end entities */

extern int sobj_cache_flush(struct sobj_cache *soc);

extern void *sobj_cache_start_op(struct sobj_cache *soc,
				 struct file_replica *repl);
extern int sobj_cache_exec_op(void *shandle);

extern int sobj_cache_write_sobj(void *shandle,
				 off_t obj_num,
				 void *data);
extern int sobj_cache_read_sobj(void *shandle,
				off_t obj_num,
				void *data);

#endif
