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
 C Interface: radix-tree

 Description: 

 This is the XtreemFS adaption of the Linux kernel radix tree.
 Radix trees will be used for file object caching.
 
 Changes done by Matthias Hess mhess at hpce dot nec dot com

 Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_RADIX_TREE__
#define __XTRFS_RADIX_TREE__

#include <stdint.h>
#include <errno.h>

#include "pseudo_rcu.h"
#include "lock_utils.h"

#define RADIX_TREE_MAP_SHIFT	6
#define RADIX_TREE_MAP_SIZE	(1UL << RADIX_TREE_MAP_SHIFT)
#define RADIX_TREE_MAP_MASK	(RADIX_TREE_MAP_SIZE-1)

#define RADIX_TREE_TAG_LONGS	\
	((RADIX_TREE_MAP_SIZE + (sizeof(long)<<3) - 1) / (sizeof(long) << 3))
#define RADIX_TREE_MAX_TAGS	2

#define RADIX_TREE_INDEX_BITS	(8 * sizeof(unsigned long))
#define RADIX_TREE_MAX_PATH (RADIX_TREE_INDEX_BITS/RADIX_TREE_MAP_SHIFT + 2)

/* In order to indicate the different kinds of slots, the linux kernel uses
   a bit in an address to indicate the kind of pointer (direct or further node) */
#define RADIX_TREE_DIRECT_PTR	1

static inline void *radix_tree_ptr_to_direct(void *ptr)
{
	return (void *)((uintptr_t)ptr | RADIX_TREE_DIRECT_PTR);
}

static inline void *radix_tree_direct_to_ptr(void *ptr)
{
	return (void *)((uintptr_t)ptr & ~RADIX_TREE_DIRECT_PTR);
}

static inline int radix_tree_is_direct_ptr(void *ptr)
{
	return (int)((uintptr_t)ptr & RADIX_TREE_DIRECT_PTR);
}


struct radix_tree_node {
	unsigned int	height;
	unsigned int	count;
	void		*slots[RADIX_TREE_MAP_SIZE];
	unsigned long	tags[RADIX_TREE_MAX_TAGS][RADIX_TREE_TAG_LONGS];
	
	/* Deviation from kernel interface */
	spinlock_t lock;
};


struct radix_tree_root {
	unsigned int 		height;
	unsigned int		gfp_mask;
	struct radix_tree_node 	*rnode;
};

#define RADIX_TREE_INIT(mask)   {					\
	.height = 0,							\
	.gfp_mask = (mask),						\
	.rnode = NULL,							\
}

#define RADIX_TREE(name, mask) \
	struct radix_tree_root name = RADIX_TREE_INIT(mask)

#define INIT_RADIX_TREE(root, mask)					\
do {									\
	(root)->height = 0;						\
	(root)->gfp_mask = (mask);					\
	(root)->rnode = NULL;						\
} while (0)


extern int radix_tree_insert(struct radix_tree_root *root,
			     unsigned long index, void *item);
extern void *radix_tree_delete(struct radix_tree_root *root,
			       unsigned long index);

extern void **radix_tree_lookup_slot(struct radix_tree_root *root,
				     unsigned long index);
extern void *radix_tree_lookup(struct radix_tree_root *root,
			       unsigned long index);

extern void *radix_tree_tag_set(struct radix_tree_root *root,
				unsigned long index, unsigned int tag);
extern void *radix_tree_tag_clear(struct radix_tree_root *root,
				  unsigned long index, unsigned int tag);
extern int radix_tree_tag_get(struct radix_tree_root *root,
			      unsigned long index, unsigned int tag);

extern int radix_tree_tagged(struct radix_tree_root *root, unsigned int tag);

#endif
