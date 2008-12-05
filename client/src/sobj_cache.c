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


/* C Implementation: sobj_cache

   Description:
   The stripe object cache is used to accelerate operations on stripe
   objects, as they occur in RAID operations, for instance.
   As different replica of the same file might have different stripe
   layouts having one cache per replica makes handling of stripe
   objects easier. Otherwise there must be conversion units that convert
   one stripe layout into another. This could be one of the next steps,
   though.
   The cache is organized in two levels: One level manages different
   replica and associated with this top level is a second level that
   manages the data within each replica.

   The original idea was to have two very simple functions to access the
   cache 'sobj_cache_{write|read}_sobj'. These functions would have been
   synchronous and as such they do not fit into the new scheme with single
   workqueues.
   So an asynchronous interface must be developed...

   Author: Matthias Hess <mhess@laptopmh.stg.hpce.tld>, (C) 2008

   Copyright: See COPYING file that comes with this distribution
 */

#include <stdlib.h>
#include <string.h>

#include "kernel_substitutes.h"
#include "sobj_cache.h"
#include "obj_set.h"

/**
 * This is the 'handle' that is used for assembling
 * sobj cache operations that need to be done quasi-atomically
 * (like gathering several stripe objects at once for RAID calc.)
 */
struct sobj_handle {
	struct sobj_cache *cache;	/*!< Object cache for an operation
					     (in cluster mode there might
					     be several) */
	struct file_replica *repl;	/*!< This is an indicator into the
					     first level of sobj cache */
	int op;				/*!< Operation type for handle (read/write) */
	struct obj_set *objects;	/*!< Obj set affected by an operation */
	struct request *req;		/*!< Request to be pushed into transl
					     stage */
};

static void sobj_handle_destroy(struct sobj_handle *shandle)
{
	free(shandle);
}

static int sobj_handle_init(struct sobj_handle *shandle,
			    struct sobj_cache *sobj_cache,
			    struct file_replica *repl)
{
	int err = 0;

	shandle->cache = sobj_cache;
	shandle->repl  = repl;
	shandle->op    = -1;

	shandle->objects = obj_set_new(5);
	if (!shandle) {
		err = 1;
		goto out;
	}
	
	
out:
	return err;
}


static struct sobj_handle *sobj_handle_new(struct sobj_cache *sobj_cache,
					   struct file_replica *repl)
{
	struct sobj_handle *rv = NULL;

	rv = (struct sobj_handle *)malloc(sizeof(struct sobj_handle));
	if (rv && sobj_handle_init(rv, sobj_cache, repl)) {
		sobj_handle_destroy(rv);
		rv = NULL;
	}

	return rv;
}



struct sobj_cache_data_entry *
sobj_cache_data_entry_new(int obj_size)
{
	struct sobj_cache_data_entry *rv = NULL;

	rv = (struct sobj_cache_data_entry *)
	     malloc(sizeof(struct sobj_cache_data_entry));
	if (rv && sobj_cache_data_entry_init(rv, obj_size)) {
		sobj_cache_data_entry_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int
sobj_cache_data_entry_init(struct sobj_cache_data_entry *scde,
			   int obj_size)
{
	int err = 0;

	scde->data = malloc(obj_size);
	if (!scde->data) {
		err = 1;
		goto out;
	}

	scde->version = -1;
	scde->state = SCE_UNDEFINED;
	scde->lease   = NULL;
	INIT_LIST_HEAD(&scde->head);

out:
	if (err && scde->data)
		free(scde->data);

	return err;
}

void
sobj_cache_data_entry_del_contents(struct sobj_cache_data_entry *scde)
{
	free(scde->data);
	scde->data = NULL;
}

void
sobj_cache_data_entry_destroy(struct sobj_cache_data_entry *scde)
{
	sobj_cache_data_entry_del_contents(scde);
	free(scde);
}


struct sobj_cache_repl_entry *
sobj_cache_repl_entry_new(struct file_replica *replica)
{
	struct sobj_cache_repl_entry *rv = NULL;

	rv = (struct sobj_cache_repl_entry *)
	     malloc(sizeof(struct sobj_cache_repl_entry));
	if (rv && sobj_cache_repl_entry_init(rv, replica)) {
		sobj_cache_repl_entry_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int sobj_cache_repl_entry_init(struct sobj_cache_repl_entry *scre,
			       struct file_replica *replica)
{
	int err = 0;

	err = hash_table_entry_init(&scre->hentry, replica->id);

	return err;
}

void sobj_cache_repl_entry_del_contents(struct sobj_cache_repl_entry *scre)
{

}


void sobj_cache_repl_entry_destroy(struct sobj_cache_repl_entry *scre)
{
	sobj_cache_repl_entry_del_contents(scre);
	free(scre);
}


/**
 * This is the hash function for the stripe object cache.
 * It should most likely be replaced by something more
 * sophisticated.
 */
static hash_idx_t sobj_cache_hash_func(void *key)
{
	hash_idx_t rv = -1;
	unsigned char *str = (unsigned char *)key;
	int l = strlen((char *)str);
	int i;

	for (i=0, rv=0; i<l; i++)
		rv += str[i];

	return rv;
}

static int sobj_cache_hash_cmp(void *key1, void *key2)
{
	return !strcmp((char *)key1, (char *)key2);
}

static void sobj_cache_hash_del(struct hash_table_entry *entry)
{
}


struct sobj_cache *sobj_cache_new()
{
	struct sobj_cache *rv = NULL;

	rv = (struct sobj_cache *)malloc(sizeof(struct sobj_cache));
	if (rv && sobj_cache_init(rv))
		sobj_cache_destroy(rv);
	return rv;
}

int sobj_cache_init(struct sobj_cache *soc)
{
	int err = 0;

	soc->replica = hash_table_new(100,
				      sobj_cache_hash_func,
				      sobj_cache_hash_cmp,
				      sobj_cache_hash_del);
	if (soc->replica)
		err = 1;

	return err;
}

void sobj_cache_del_contents(struct sobj_cache *soc)
{
	
}

void sobj_cache_destroy(struct sobj_cache *soc)
{
	sobj_cache_del_contents(soc);
	free(soc);
}


struct sobj_cache_repl_entry *
sobj_cache_add_replica(struct sobj_cache *soc,
		       struct file_replica *replica,
		       int lock,
		       int *err)
{
	struct sobj_cache_repl_entry *rv = NULL;
	struct hash_table_entry *hte = NULL;
	int ret = 0;

	rv = sobj_cache_repl_entry_new(replica);
	ret = hash_table_insert_entry(soc->replica, &rv->hentry, lock);
	if (!ret) {
		*err = ret;
		goto out;
	}

out:
	if (err && rv) {
		sobj_cache_repl_entry_destroy(rv);
		rv = NULL;
	}

	return rv;
}


struct sobj_cache_repl_entry *
sobj_cache_find_repl_entry(struct sobj_cache *soc,
                           struct file_replica *replica,
			   int lock)
{
	struct sobj_cache_repl_entry *rv = NULL;
	struct hash_table_entry *hte = NULL;
	int err = 0;

	hte = hash_table_find(soc->replica, (void *)replica->id, lock);
	if (!hte)	/* Entry not found */
		goto out;

	rv = container_of(hte, struct sobj_cache_repl_entry, hentry);

out:
	return rv;
}


/**
 * Get an entry for a replica from the stripe object cache.
 *
 * If the entry is not in our cache, we create a new one.
 */
struct sobj_cache_repl_entry *
sobj_cache_get_repl_entry(struct sobj_cache *soc,
		    struct file_replica *replica,
		    int locked)
{
	struct sobj_cache_repl_entry *rv = NULL;
	int err = 0;

	/* First, try to find the replica in our inventory */
	rv = sobj_cache_find_repl_entry(soc, replica, 0);
	if (rv)
		goto out;

	/* Nope, we do not have it. Let's create a new one */
	rv = sobj_cache_add_replica(soc, replica, 0, &err);

out:
	return rv;
}


/**
 * Get a data entry from the cache.
 *
 * If the entry is already in the cache, it is returned. Otherwise
 * a new entry is created in the cache.
 */
struct sobj_cache_data_entry *
sobj_cache_get_data_entry(struct sobj_cache *soc,
			  struct file_replica *replica,
			  int obj_num,
			  int lock)
{
	struct sobj_cache_data_entry *rv = NULL;
	struct sobj_cache_repl_entry *scre = NULL;
	int err = 0;

	/* Get the replica entry first */
	scre = sobj_cache_get_repl_entry(soc, replica, 0);
	if (!scre) {
		err = 1;
		goto out;
	}

	rv = (struct sobj_cache_data_entry *)radix_tree_lookup(&scre->entries, obj_num);
	if (rv) 
		goto out;

	/* We do not have the object cached, yet. */
	rv = sobj_cache_data_entry_new(replica->sp.stripe_size);
	if (!rv) {
		err = 2;
		goto out;
	}

check:
	/* Check if the lease associated with the data entry is valid. */
	if (lease_is_valid(rv->lease)) {

	} else {

	}
out:
	return rv;
}

void *sobj_cache_start_op(struct sobj_cache *cache,
			  struct file_replica *repl)
{
	struct sobj_handle *rv = NULL;

	rv = sobj_handle_new(cache, repl);
	if (!rv)
		goto out;
	
out:
	return rv;
}

int sobj_cache_exec_op(void *shandle)
{
	int err = 0;
	struct sobj_handle *handle = (struct sobj_handle *)shandle;
	struct obj_set *objs = handle->objects;
	struct sobj_cache_data_entry *dentry = NULL;
	int i, j, k;

	/* If no operation has been associated with this handle,
	   we simply skip it. */
	if (handle->op == -1)
		goto out;

	/* For now we do not support combined operations on one object set.
	   So we must resolve the object set into single objects... */

	/* TODO: Here is definitely room for improvement: Operations on an
	   object set should be done in parallel! */

	for (i=0; i<objs->num_entries; i++) {

		/* And get objects for each entry */
		for (j=objs->entries[i].start_num,  k=0;
		     j <= objs->entries[i].end_num;
		     j += objs->entries[i].stride,  k++) {
			dentry = sobj_cache_get_data_entry(handle->cache,
							   handle->repl,
							   j,
							   0);

			/* Check if the dentry is valid */
			if (!lease_is_valid(dentry->lease)) {	/* Nope, not valid */
				/* Steps to do here: Get lease and object */

				/* Either create a request for sobj stage or
				   do it directly. The latter is easier, here */
			}

			/* Now we have an entry and we can read or write to it.
			   Each object in the object set has a data area
			   that belong sto it. */
			switch (handle->op) {
			case REQ_SOBJ_READ:
				memcpy((void *)objs->entries[i].data[k],
				       dentry->data,
				       handle->repl->sp.stripe_size);
				break;
			case REQ_SOBJ_WRITE:
				memcpy((void *)dentry->data,
				       (void *)objs->entries[i].data[k],
				       handle->repl->sp.stripe_size);
				break;
			default:
				break;
			}
		}
	}

out:
	return err;
}

/**
 * Write a stripe object to the file system
 *
 * The cache will take care of the procedure to write
 * to the file system. It will get the required lease
 * if neccessary and write to the OSDs, for instance.
 *
 * @param shandle Handle for stripe object cache operations
 * @param obj_num Stripe object number (might be different
 *                from file object number)
 * @param data    Data to write. The size of this data is
 *                determined by the striping policy.
 * @return        0 if successful, error code otherwise
 */
int sobj_cache_write_sobj(void *shandle,
			  off_t obj_num,
			  void *data)
{
	int err = 0;
	struct sobj_handle *handle = (struct sobj_handle *)shandle;
	struct sobj_cache_data_entry *scde = NULL;

	scde = sobj_cache_get_data_entry(handle->cache,
					 handle->repl,
					 obj_num, 0);
	if (!scde) {	/* Error! */
		dbg_msg("Cannot get new cache data entry.\n");
		err = 2;
		goto out;
	}

	/* Now check if the lease is valid */
	while (!lease_is_valid(scde->lease)) {
		
	}

	/* We may now write the data into the data object */
	memcpy((void *)scde->data, data, handle->repl->sp.stripe_size);

out:
	return err;
}


/**
 * Read a stripe object from file system
 *
 * Reads a stripe object from the file system. If the object
 * is stored in the cache and has a valid lease associated
 * with it, the data of the stripe object is returned
 * immediately. Otherwise it is fetched from the corresponding
 * servers.
 *
 * @param soc     Stripe object cache to read from
 * @param replica Replica the stripe belongs to
 * @param obj_num Stripe object number
 * @param data    Pointer to the memory where the data should
 *                be stored.
 * @return        0 of successful, error code otherwise
 */
int sobj_cache_read_sobj(void *shandle,
			 off_t obj_num,
			 void *data)
{
	int err = 0;
	struct sobj_handle *handle = (struct sobj_handle *)shandle;
	struct sobj_cache_data_entry *scde = NULL;

	scde = sobj_cache_get_data_entry(handle->cache,
					 handle->repl, obj_num, 0);
	if (!scde) {
		err = 1;
		goto out;
	}

	while (!lease_is_valid(scde->lease)) {
	}

	/* And copy the data from the data object into the buffer */
	memcpy((void *)data, scde->data, handle->repl->sp.stripe_size);

out:
	return err;
}


/**
 * Read an object set from the cache
 */
int sobj_cache_read_obj_set(struct sobj_cache *soc,
			    struct file_replica *repl,
			    struct obj_set *oset)
{
	int err = 0;

	return err;
}


/**
 * Write object set data to the cache
 */
int sobj_cache_write_obj_set(struct sobj_cache *soc,
			     struct file_replica *repl,
			     struct obj_set *oset)
{
	int err = 0;

	return err;
}
