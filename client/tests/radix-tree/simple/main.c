/*
*  C Implementation: main
*
* Description: 
* Test program for radix trees.
*
* Author: Matthias Hess <mhess@hpce.nec.com>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdlib.h>
#include <stdio.h>

#include <radix-tree.h>

#define NUM_ELEMS 1000000

int
main(int argc, char **argv)
{
	struct radix_tree_root root;
	unsigned long idx[NUM_ELEMS];
	int i, j, r;
	double d[NUM_ELEMS], z;
	
	/* Do a random permutation of the index array */
	for(i=0; i<NUM_ELEMS; i++) {
		idx[i] = i;
	}
	
	for(i=0; i<NUM_ELEMS; i++) {
		r = (int)(drand48() * NUM_ELEMS);
		j = idx[r];
		idx[r] = idx[i];
		idx[i] = j;
	}
	
	/* Build up tree */

	INIT_RADIX_TREE(&root, 0);
	
	for(i=0; i<NUM_ELEMS; i++) {
		d[i] = (double)i;
		radix_tree_insert(&root, idx[i], &d[i]);
	}
	
	
	/* And retrieve it */
	for(i=0; i<NUM_ELEMS; i++) {
		z = *((double *)radix_tree_lookup(&root, idx[i]));
		if ((unsigned long)z != i) {
			fprintf(stderr, "Wrong datum for index %d: %g but expected %ld\n", i, z, i);
		}
	}
	
	return 0;
}
