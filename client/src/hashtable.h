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
C Interface: hashtable

Description: 


Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/

#ifndef _HASHTABLE_H
#define _HASHTABLE_H

#include <stdlib.h>
#include <pthread.h>

#include <list.h>

#include <lock_utils.h>

#include <kernel_substitutes.h>


typedef int hash_idx_t;

struct hash_table_entry {
	struct list_head head;
	void *key;
	spinlock_t lock;
	atomic_t use_count;
};

typedef hash_idx_t hash_func(void *key);      /*!< Hash function prototype */
typedef int hash_cmp(void *key1, void *key2); /*!< Hash compare function */
typedef void hash_del(struct hash_table_entry *entry);	/*!< How to delete an entry */

extern struct hash_table_entry *hash_table_entry_new(void *key);
extern int hash_table_entry_init(struct hash_table_entry *hte, void *key);
extern void hash_table_entry_del_content(struct hash_table_entry *hte);
extern void hash_table_entry_delete(struct hash_table_entry **hte);

struct hash_table {
	hash_func *func;
	hash_cmp *cmp;
	hash_del *del_entry;
	size_t size;                  /*!< Number of 'list_heads' in table */
	spinlock_t *locks;
	struct list_head *entries;    /*!< Entries of hash table */
};

extern struct hash_table *hash_table_new(size_t initial_size,
					 hash_func *func,
					 hash_cmp *cmp,
					 hash_del *del);
extern int hash_table_init(struct hash_table *ht,
			   size_t initial_size,
			   hash_func *func,
			   hash_cmp *cmp,
			   hash_del *del);
extern void hash_table_clear(struct hash_table *ht);
extern void hash_table_delete(struct hash_table *ht);
extern void hash_table_flush(struct hash_table* ht);

extern int hash_table_lock(struct hash_table *ht);
extern int hash_table_unlock(struct hash_table *ht);

extern int hash_table_lock_row(struct hash_table *ht, void *key);
extern int hash_table_unlock_row(struct hash_table *ht, void *key);

extern int hash_table_insert_entry(struct hash_table *ht,
				   struct hash_table_entry *hte,
				   int lock);
extern void hash_table_remove_entry(struct hash_table *ht,
				    struct hash_table_entry *hte);

extern int hash_table_delete_entry(struct hash_table *ht, void *key);
extern struct hash_table_entry *hash_table_find(struct hash_table *ht, void *key, int lock);


struct hash_table_iter {
        int idx;
        struct list_head *list_iter;
};

static inline int hash_table_iter_inc(struct hash_table_iter *hti, struct hash_table *ht)
{
        int rv = 1;

        hti->list_iter = hti->list_iter->next;
        while(hti->list_iter == &ht->entries[hti->idx]) {
                hti->idx++;
                if(hti->idx >= ht->size) {
			rv = 0;
			break;
		} else
			hti->list_iter = ht->entries[hti->idx].next;
        }

	return rv;
}

static inline void hash_table_iter_set(struct hash_table_iter *hti, struct hash_table *ht)
{
	hti->idx = 0;
	hti->list_iter = &ht->entries[0];
	hash_table_iter_inc(hti, ht);
}

/* Iterate through a hash table */
#define ht_for_each(it, ht) \
        for (hash_table_iter_set(&it, ht); \
             it.idx<(ht)->size-1 || (it.idx==(ht)->size-1 && it.list_iter != &(ht)->entries[(ht)->size-1]); \
             hash_table_iter_inc(&it, ht))


#endif
