/* Copyright (c) 2008  Felix Hupfeld
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

#include <time.h>

#include "metadata_cache.h"

#include "xtreemfs_utils.h"

#define CACHE_MAX_AGE_SECS 5.0  /* time after which entries are flushed */

struct metadata_cache md_cache;

struct metadata_cache_entry {
	char* filename;
	struct xtreemfs_statinfo statinfo;
	clock_t creation_time;
};

static hash_idx_t 
string_hash(struct metadata_cache_entry *key) {
	char *str = key->filename;
	hash_idx_t hash = 0;

	while ( *str != 0 ) {
		hash = hash ^ ( ( hash << 5 ) + ( hash >> 2 ) + *str );
		str++;
	}

    return hash;
}


static int 
entry_compare(struct metadata_cache_entry* k1, struct metadata_cache_entry* k2) {
	return strcmp(k1->filename,k2->filename) == 0;
}


void 
metadata_cache_entry_free(struct hash_table_entry* ht_entry) 
{
	struct metadata_cache_entry* entry;
	entry = (struct metadata_cache_entry*)ht_entry->key;

	free(entry->filename);
	xtreemfs_statinfo_clear(&entry->statinfo);
	free(entry);
	hash_table_entry_del_content(ht_entry);
	free(ht_entry);
}


void 
metadata_cache_init(struct metadata_cache* md_cache) 
{	
	hash_table_init(&md_cache->filename_to_statinfo,
				    100, /* initial size */
				    (hash_func*)string_hash,
				    (hash_cmp*)entry_compare,
				    metadata_cache_entry_free);

	spin_init(&md_cache->lock, PTHREAD_PROCESS_PRIVATE);

	md_cache->entries_registered = 0;
}


void 
metadata_cache_flush(struct metadata_cache* md_cache) 
{	
	spin_lock(&md_cache->lock);
	if(md_cache->entries_registered != 0)
		hash_table_flush(&md_cache->filename_to_statinfo);
	md_cache->entries_registered = 0;
	spin_unlock(&md_cache->lock);
}


void 
metadata_cache_clear(struct metadata_cache* md_cache) 
{	
	metadata_cache_flush(&md_cache->filename_to_statinfo);
	hash_table_clear(&md_cache->filename_to_statinfo);
}


static void 
metadata_cache_add_entry(struct metadata_cache* md_cache, 
						 char* filename_malloced, 
						 struct xtreemfs_statinfo* md) 
{
	struct metadata_cache_entry* entry = NULL;
	struct hash_table_entry* cache_entry = NULL;

	entry = malloc(sizeof(struct metadata_cache_entry));
	entry->filename = filename_malloced;
	xtreemfs_statinfo_copy(&entry->statinfo, md);

	cache_entry = hash_table_entry_new(entry);

	dbg_msg("adding %s to cache\n", entry->filename); 

	spin_lock(&md_cache->lock);
	hash_table_insert_entry(&md_cache->filename_to_statinfo, cache_entry, 1);
	spin_unlock(&md_cache->lock);
}


void 
metadata_cache_insert_entries(struct metadata_cache* md_cache,
							  const char* dirname, int num_entries,
							  char** names,
							  struct xtreemfs_statinfo* entries)
{
	int i;
	
	for(i = 0; i < num_entries; ++i) {
		char* filename = malloc(strlen(dirname) + strlen(names[i]) + 2);

		strcpy(filename,dirname);
		if(strlen(dirname) > 1)
			strcat(filename,"/");
		strcat(filename,names[i]);

		metadata_cache_add_entry(md_cache, filename, &entries[i]);
	}

	md_cache->entries_registered = clock();
}

int						
metadata_cache_get(struct metadata_cache* md_cache, const char* filename, 
                   struct xtreemfs_statinfo* result) 
{
	struct hash_table_entry* cache_entry = NULL;
	struct metadata_cache_entry* entry = NULL;

	/* Flush entries if they are older than N seconds */
	if((((double)(clock() - md_cache->entries_registered)) / CLOCKS_PER_SEC) > CACHE_MAX_AGE_SECS) {
		dbg_msg("Metadata cache older than %f seconds, flushing.\n", CACHE_MAX_AGE_SECS); 
		metadata_cache_flush(md_cache);
		return 0;
	}

	struct metadata_cache_entry key; 
	key.filename = (char*)filename;

	spin_lock(&md_cache->lock);
	cache_entry = hash_table_find(&md_cache->filename_to_statinfo, 
								  (void*)&key, 1);

	if(!cache_entry) {
		dbg_msg("%s not in metadata cache\n", filename); 
		spin_unlock(&md_cache->lock);
		return 0;
	}

	dbg_msg("%s in metadata cache\n", filename); 
	entry = (struct metadata_cache_entry*)cache_entry->key;

	xtreemfs_statinfo_copy(result, &entry->statinfo);
	atomic_dec(&cache_entry->use_count);
	spin_unlock(&md_cache->lock);

	return 1;
}
