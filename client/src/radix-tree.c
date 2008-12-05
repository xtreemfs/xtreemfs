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
*  C Implementation: radix-tree
*
* Description: 
*
*
* Author: Matthias Hess <mhess@hpce.nec.com>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include "logger.h"

#include "radix-tree.h"
#include "list.h"
#include "kernel_substitutes.h"

struct radix_tree_path {
	struct radix_tree_node *node;
	int offset;
};

static struct radix_tree_node *
radix_tree_node_alloc(struct radix_tree_root *root)
{
	struct radix_tree_node *ret = NULL;
	
	ret = (struct radix_tree_node *)malloc(sizeof(*ret));
	if (ret)
		memset((void *)ret, 0, sizeof(*ret));
	spin_init(&ret->lock, PTHREAD_PROCESS_PRIVATE);
	
	return ret;
}

static inline void
radix_tree_node_free(struct radix_tree_node *node)
{
	spin_destroy(&node->lock);
	free(node);
}

static inline unsigned long radix_tree_maxindex(unsigned int height)
{
	unsigned int tmp = height * RADIX_TREE_MAP_SHIFT;
	unsigned long index = (~0UL >> (RADIX_TREE_INDEX_BITS - tmp - 1)) >> 1;
	
	if (tmp >= RADIX_TREE_INDEX_BITS)
		index = ~0UL;
	return index;
}

static inline void tag_set(struct radix_tree_node *node, unsigned int tag,
			   int offset)
{
}

static inline void tag_clear(struct radix_tree_node *node, unsigned int tag,
		int offset)
{
}

static inline int tag_get(struct radix_tree_node *node, unsigned int tag,
		int offset)
{
	return 0;
}

static inline void root_tag_set(struct radix_tree_root *root, unsigned int tag)
{
}

static inline void root_tag_clear(struct radix_tree_root *root, unsigned int tag)
{
}

static inline void root_tag_clear_all(struct radix_tree_root *root)
{
}


static inline int root_tag_get(struct radix_tree_root *root, unsigned int tag)
{
	return 0;
}

static int radix_tree_extend(struct radix_tree_root *root, unsigned long index)
{
	struct radix_tree_node *node;
	unsigned int height;
	int tag;
	
	height = root->height + 1;
	while (index > radix_tree_maxindex(height))
		height++;
	
	if (root->rnode == NULL) {
		root->height = height;
		goto out;
	}
	
	do {
		unsigned int newheight;
		if (!(node = radix_tree_node_alloc(root)))
			return -ENOMEM;
		
		node->slots[0] = radix_tree_direct_to_ptr(root->rnode);
		
		for (tag = 0; tag < RADIX_TREE_MAX_TAGS; tag++) {
			if (root_tag_get(root, tag))
				tag_set(node, tag, 0);
		}
		
		newheight = root->height+1;
		node->height = newheight;
		node->count = 1;
		root->rnode = node;
		root->height = newheight;
	} while (height > root->height);
out:
	return 0;
}

static inline void radix_tree_shrink(struct radix_tree_root *root)
{
	/* try to shrink tree height */
	while (root->height > 0 &&
			root->rnode->count == 1 &&
			root->rnode->slots[0]) {
		struct radix_tree_node *to_free = root->rnode;
		void *newptr;
		
		newptr = to_free->slots[0];
		if (root->height == 1)
			newptr = radix_tree_ptr_to_direct(newptr);
		root->rnode = newptr;
		root->height--;
		/* must only free zeroed nodes into the slab */
		tag_clear(to_free, 0, 0);
		tag_clear(to_free, 1, 0);
		to_free->slots[0] = NULL;
		to_free->count = 0;
		radix_tree_node_free(to_free);
	}
}

int radix_tree_insert(struct radix_tree_root *root,
		      unsigned long index, void *item)
{
	struct radix_tree_node *node = NULL, *slot = NULL;
	unsigned int height, shift;
	int offset;
	int error;
	
	if (radix_tree_is_direct_ptr(item)) {
		dbg_msg("Item is a direct pointer.\n");
		return 1;
	}
	
	/* Make sure the tree is high enough */
	if (index > radix_tree_maxindex(root->height)) {
		error = radix_tree_extend(root, index);
		if (error) {
			dbg_msg("Error while extending radix tree.");
			return error;
		}
	}
	
	slot = root->rnode;
	height = root->height;
	shift = (height-1) * RADIX_TREE_MAP_SHIFT;
	
	offset = 0;
	while (height > 0) {
		if (slot == NULL) {
			if (!(slot = radix_tree_node_alloc(root))) {
				dbg_msg("Cannot allocate new slot.\n");
				return -ENOMEM;
			}
			slot->height = height;
			if (node) {
				spin_lock(&node->lock);
				node->slots[offset] = slot;
				node->count++;
				spin_unlock(&node->lock);
			} else
				root->rnode = slot;
		}
		
		offset = (index >> shift) & RADIX_TREE_MAP_MASK;
		node = slot;
		spin_lock(&node->lock);
		slot = node->slots[offset];
		spin_unlock(&node->lock);
		shift -= RADIX_TREE_MAP_SHIFT;
		height--;
	}
	
	if (slot != NULL) {
		dbg_msg("Slot already exists.\n");
		return -EEXIST;
	}
	
	if (node) {
		spin_lock(&node->lock);
		node->count++;
		node->slots[offset] = item;
		spin_unlock(&node->lock);
	} else {
		root->rnode = radix_tree_ptr_to_direct(item);
	}

	dbg_msg("Inserted item successfully.\n");
	
	return 0;
}

void *radix_tree_delete(struct radix_tree_root *root, unsigned long index)
{
	struct radix_tree_path path[RADIX_TREE_MAX_PATH], *pathp = path;
	struct radix_tree_node *slot = NULL;
	struct radix_tree_node *to_free;
	unsigned int height, shift;
	int tag;
	int offset;
	
	height = root->height;
	if (index > radix_tree_maxindex(height))
		goto out;
	
	slot = root->rnode;
	if (height == 0 && root->rnode) {
		slot = radix_tree_direct_to_ptr(slot);
		root_tag_clear_all(root);
		root->rnode = NULL;
		goto out;
	}
	
	shift = (height - 1) * RADIX_TREE_MAP_SHIFT;
	pathp->node = NULL;
	
	do {
		if (slot == NULL)
			goto out;
		
		pathp++;
		offset = (index >> shift) & RADIX_TREE_MAP_MASK;
		pathp->offset = offset;
		pathp->node = slot;
		slot = slot->slots[offset];
		shift -= RADIX_TREE_MAP_SHIFT;
		height--;
	} while (height > 0);
	
	if (slot == NULL)
		goto out;
	
	for (tag = 0; tag < RADIX_TREE_MAX_TAGS; tag++) {
		if (tag_get(pathp->node, tag, pathp->offset))
			radix_tree_tag_clear(root, index, tag);
	}
	
	to_free = NULL;
	/* Now free the nodes we do not need anymore */
	while (pathp->node) {
		pathp->node->slots[pathp->offset] = NULL;
		pathp->node->count--;
		/*
		 * Queue the node for deferred freeing after the
		 * last reference to it disappears (set NULL, above)
		 */
		if (to_free)
			radix_tree_node_free(to_free);
		
		if(pathp->node->count) {
			if (pathp->node == root->rnode)
				radix_tree_shrink(root);
			goto out;
		}
		
		/* Node with zero slots in use so free it */
		to_free = pathp->node;
		pathp--;
	}
	root_tag_clear_all(root);
	root->height = 0;
	root->rnode = NULL;
	if (to_free)
		radix_tree_node_free(to_free);
	
out:
	return slot;
}

void *radix_tree_lookup(struct radix_tree_root *root, unsigned long index)
{
	unsigned int height, shift;
	struct radix_tree_node *node, **slot;
	
	node = root->rnode;
	if (node == NULL)
		return NULL;
	
	if (radix_tree_is_direct_ptr(node)) {
		if (index > 0)
			return NULL;
		return radix_tree_direct_to_ptr(node);
	}
	
	height = node->height;
	if (index > radix_tree_maxindex(height))
		return NULL;
	
	shift = (height-1) * RADIX_TREE_MAP_SHIFT;
	
	do {
		spin_lock(&node->lock);
		slot = (struct radix_tree_node **)
			(node->slots + ((index>>shift) & RADIX_TREE_MAP_MASK));
		spin_unlock(&node->lock);
		node = *slot;
		if (node == NULL)
			return NULL;
		
		shift -= RADIX_TREE_MAP_SHIFT;
		height--;
	} while (height > 0);
	
	return node;
}

void *radix_tree_tag_clear(struct radix_tree_root *root,
			unsigned long index, unsigned int tag)
{
	return NULL;
}

void radix_tree_print(struct radix_tree_root *root)
{

}
