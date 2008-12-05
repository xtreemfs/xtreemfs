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


#ifndef __XTRFS_METADATA_CACHE_H__
#define __XTRFS_METADATA_CACHE_H__

#include <time.h>

#include "hashtable.h"
#include "statinfo.h"

struct metadata_cache {
	struct hash_table filename_to_statinfo;
	spinlock_t lock; /* for flushing */
	clock_t entries_registered;
};

extern struct metadata_cache md_cache; /* The Singleton */


extern void metadata_cache_init(struct metadata_cache* md_cache);
extern void metadata_cache_flush(struct metadata_cache* md_cache);
extern void metadata_cache_clear(struct metadata_cache* md_cache);

extern void metadata_cache_insert_entries(struct metadata_cache* , 
								   const char* dirname, int num_entries,
								   char** names, 
								   struct xtreemfs_statinfo* entries);

extern int metadata_cache_get(struct metadata_cache* md_cache, 
							  const char* filename, 
							  struct xtreemfs_statinfo*);


#endif /* __XTRFS_METADATA_CACHE_H__ */
