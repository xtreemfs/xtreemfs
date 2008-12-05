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
*  C Implementation: hashtable
*
* Description: 
*
*
* Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <string.h>
#include <pthread.h>

#include <hashtable.h>

#include "lock_utils.h"
#include "logger.h"


struct hash_table_entry *
hash_table_entry_new(void *key)
{
	struct hash_table_entry *rv = NULL;
	
	rv = (struct hash_table_entry *)malloc(sizeof(struct hash_table_entry));
	if(rv != NULL && hash_table_entry_init(rv, key) != 0)
		hash_table_entry_delete(&rv);
	
	return rv;
}

int
hash_table_entry_init(struct hash_table_entry *hte, void *key)
{
	INIT_LIST_HEAD(&hte->head);
	hte->key = key;
	spin_init(&hte->lock, PTHREAD_PROCESS_PRIVATE);
	atomic_set(&hte->use_count, 0);
	return 0;
}

void hash_table_entry_del_content(struct hash_table_entry *hte)
{
	spin_destroy(&hte->lock);
}

void
hash_table_entry_delete(struct hash_table_entry **hte)
{
	if(*hte) {
		hash_table_entry_del_content(*hte);
		free(*hte);
		*hte = NULL;
	}
}


struct hash_table *
hash_table_new(size_t initial_size,
	       hash_func *func, hash_cmp *cmp, hash_del *del)
{
	struct hash_table *rv = NULL;
	
	rv = (struct hash_table *)malloc(sizeof(struct hash_table));
	if(rv != NULL && hash_table_init(rv, initial_size, func, cmp, del) != 0) {
		hash_table_delete(rv);
		rv = NULL;
	}
		
	return rv;
}


int
hash_table_init(struct hash_table *ht, size_t initial_size,
		hash_func *func, hash_cmp *cmp, hash_del *del)
{
	int rv = 0;
	int i;
	
	memset((void *)ht, 0, sizeof(struct hash_table));

	ht->func    = func;
	ht->cmp     = cmp;
	ht->del_entry = del;
	ht->size    = initial_size;
	ht->entries = (struct list_head *)malloc(sizeof(struct list_head) * ht->size);
	if(ht->entries == NULL) {
		rv = 1;
		goto finish;
	}
	
	ht->locks = (spinlock_t *)malloc(sizeof(spinlock_t) * ht->size);
	if (!ht->locks) {
		rv = 2;
		goto finish;
	}

	for(i=0; i<ht->size; i++) {
		INIT_LIST_HEAD(&ht->entries[i]);
		spin_init(&ht->locks[i], PTHREAD_PROCESS_PRIVATE);
	}

finish:
	return rv;
}


/**
 * Clear all entries in the hash table.
 * This functions frees all memory occupied by the hash
 * table. The caller has to make sure that noone
 * still accesses this table.
 */
void
hash_table_clear(struct hash_table *ht)
{
	int i;
	
	if(ht->entries) {
		free(ht->entries);
		if (ht->locks) {
			for (i=0; i<ht->size; i++)
				spin_destroy(&ht->locks[i]);
			free(ht->locks);
		}
		ht->entries = NULL;
	}
}


void
hash_table_delete(struct hash_table *ht)
{
	int i;

	if(ht) {
		hash_table_clear(ht);
		for(i=0; i<ht->size; i++)
			spin_destroy(&ht->locks[i]);
		free(ht);
	}
}


void 
hash_table_flush(struct hash_table* ht)
{
	struct list_head *iter, *n;
	int i;

	for(i=0; i<ht->size; i++) {
		list_for_each_safe(iter, n, (struct list_head *)&ht->entries[i]) {
			list_del(iter);
			ht->del_entry((struct hash_table_entry*)iter);
		}
	}
}


hash_idx_t
hash_table_idx(struct hash_table *ht, void *key)
{
	hash_idx_t rv;
	
	rv = ht->func(key);
	rv %= ht->size;
	
	return rv;
}


/**
 * Lock the whole hash table.
 * As this function should be rarely used and in order to 
 * circumvent double locks in the remaining functions that
 * require locking of a single row, this function tries
 * to lock all row locks.
 * If one of the locking operations fails we release the previously
 * acquired locks. But this is bad anyway and the program can
 * probably not recover from this.
 */
int
hash_table_lock(struct hash_table *ht)
{
	int rv = 0;
	int i;
	
	dbg_msg("Locking complete hash table.\n");

	for(i=0; i<ht->size; i++) {
		if (spin_lock(&ht->locks[i])) {
			i--;
			while(i >= 0) {
				spin_unlock(&ht->locks[i]);
				i--;
			}
			break;
		}
	}

	if (i != ht->size)
		rv = 1;

	return rv;
}


/**
 * Unlock the whole hash table.
 * This is done by unlocking every row. If one of the unlocking
 * operations does not succeed, the program cannot recover from this,
 * as there will be no second try to unlock that failing row lock.
 */
int
hash_table_unlock(struct hash_table *ht)
{
	int rv = 0;
	int i;

	dbg_msg("Unlocking hash table.\n");
	for(i=ht->size-1; i>=0; i--)
		rv |= spin_unlock(&ht->locks[i]);

	return rv;
}


/**
 * Lock the row corresponding to 'key'.
 * As it is possible to lock each row within the table, it must
 * be possible to lock a row corresponding to a key.
 * Usually the caller does not (and should not!) know the
 * row corresponding to the key.
 */
int
hash_table_lock_row(struct hash_table *ht, void *key)
{
	hash_idx_t idx = hash_table_idx(ht, key);
	dbg_msg("Locking row %d of hash table.\n", idx);
	return spin_lock(&ht->locks[idx]);
}


/**
 * Release a lock of the row corr. to 'key'.
 * This is the inverse function to 'hash_table_lock_row'
 */
int
hash_table_unlock_row(struct hash_table *ht, void *key)
{
	hash_idx_t idx = hash_table_idx(ht, key);
	dbg_msg("Unlocking row %d of hash table.\n", idx);
	return spin_unlock(&ht->locks[idx]);
}


int
hash_table_insert_entry(struct hash_table *ht,
			struct hash_table_entry *hte,
			int lock)
{
	int rv = 0;
	hash_idx_t idx;
	
	idx = hash_table_idx(ht, hte->key);
	
	if(lock)
		spin_lock(&ht->locks[idx]);
	spin_lock(&hte->lock);
	list_add((struct list_head *)hte, &ht->entries[idx]);
	atomic_inc(&hte->use_count);	/* Entry is now added to list.
					   it can only be deleted, if it
					   has been removed from that list. */
	spin_unlock(&hte->lock);
	if(lock)
		spin_unlock(&ht->locks[idx]);
	
	return rv;
}


/**
 * Find an entry corresponding to 'key' in the hash table.
 * If no such entry exists, NULL is returned.
 *
 * Usually the caller wants to manipulate the entry after
 * finding them, so in order to keep the hash table consistent
 * the corresponding table entry can be locked before with
 * a call to 'hash_table_lock_row' before calling this function.
 * (lock == 0).
 * In another case the caller simply wants to check if an element
 * is available and locking can be done inside this function.
 * (lock==1)
 */
struct hash_table_entry *
hash_table_find(struct hash_table *ht, void *key, int lock)
{
	struct hash_table_entry *rv = NULL, *entry;
	struct list_head *iter;
	hash_idx_t idx;
	
	idx = hash_table_idx(ht, key);
	
	if (lock)
		spin_lock(&ht->locks[idx]);
	list_for_each(iter, (struct list_head *)&ht->entries[idx]) {
		entry = (struct hash_table_entry *)iter;
		if(ht->cmp(entry->key, key)) {
			rv = entry;
			break;
		}
	}

	if (rv)
		atomic_inc(&rv->use_count);	/* Entry is handed out */

	if (lock)
		spin_unlock(&ht->locks[idx]);

	return rv;
}

void hash_table_remove_entry_by_key(struct hash_table *ht,
				    void *key)
{
	struct hash_table_entry *entry = NULL;
	struct list_head *iter, *n;
	hash_idx_t idx;

	idx = hash_table_idx(ht, key);

	spin_lock(&ht->locks[idx]);
	list_for_each_safe(iter, n, (struct list_head *)&ht->entries[idx]) {
		entry = (struct hash_table_entry *)iter;
		spin_lock(&entry->lock);
		if (ht->cmp(entry->key, key)) {
			list_del(iter);
			atomic_dec(&entry->use_count);	/* Entry is no longer in list. */
			if (atomic_read(&entry->use_count) <= 0) {
				spin_unlock(&entry->lock);
				ht->del_entry(entry);
				break;
			}
		}
		spin_unlock(&entry->lock);
	}
	spin_unlock(&ht->locks[idx]);
}

void hash_table_remove_entry(struct hash_table *ht,
			     struct hash_table_entry *hte)
{
	struct hash_table_entry *entry = NULL;
	struct list_head *iter, *n;
	hash_idx_t idx;

	idx = hash_table_idx(ht, hte->key);

	spin_lock(&ht->locks[idx]);
	list_for_each_safe(iter, n, (struct list_head *)&ht->entries[idx]) {
		entry = (struct hash_table_entry *)iter;
		spin_lock(&entry->lock);
		if (ht->cmp(entry->key, hte->key)) {
			if (entry == hte) {
				list_del(iter);
				atomic_dec(&entry->use_count);
				if (atomic_read(&entry->use_count) <= 0) {
					spin_unlock(&entry->lock);
					ht->del_entry(entry);
					break;
				}
			} else {
				dbg_msg("Double entry for key '%s'\n", hte->key);
			}
		}
		spin_unlock(&entry->lock);
	}
	spin_unlock(&ht->locks[idx]);
}
