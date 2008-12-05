/*
*  C Implementation: main
*
* Description: 
*
*
* Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>

#include <hashtable.h>

#define MAX_STR_LEN           20
#define NUM_CHARS             (('z'-'a') + ('Z'-'A') + ('9'-'0') + 3)
#define NUM_RANDOM_STRINGS    1024

char chars[NUM_CHARS];


hash_idx_t
hfunc(void *data)
{
	char *str = (char *)data;
	hash_idx_t rv = 0;
	int i;
	
	for(i=0; i<strlen(str); i++) {
		rv += (hash_idx_t)str[i];
	}
	
	return rv;
}

int
hcmp(void *key1, void *key2)
{
	return !strcmp(key1, key2);
}


char *
random_string()
{
	char *rv = NULL;
	size_t len;
	int i;
		
	len = (MAX_STR_LEN * drand48() + 1) + 1;
	rv = (char *)malloc(len);
	if(rv == NULL) goto finish;
	
	for(i=0; i<len-1; i++) {
		rv[i] = chars[(int)(NUM_CHARS * drand48())];
	}
	rv[i] = '\0';
	
finish:
	return rv;
}

void
hash_table_print(struct hash_table *ht)
{
	int i;
	struct list_head *iter;
	struct hash_table_entry *entry;
	
	for(i=0; i<ht->size; i++) {
		list_for_each(iter, &ht->entries[i]) {
			entry = (struct hash_table_entry *)iter;
			printf("%s -> %d\n", (char *)entry->key, (int)entry->value);
		}
	}
}

int
main(int argc, char **argv)
{
	struct hash_table *ht;
	int i, p;
	char *new_str;
	
	for(i=0, p=0; i<=('z'-'a'); i++, p++) chars[p] = 'a' + (char)i;
	for(i=0; i<=('Z'-'A'); i++, p++)      chars[p] = 'A' + (char)i;
	for(i=0; i<=('9'-'0'); i++, p++)      chars[p] = '0' + (char)i;

	printf("Number of characters: %d, %d\n", NUM_CHARS, (int)('9'-'0'));

	ht = hash_table_new(20, hfunc, hcmp);
	
	for(i=0; i<NUM_RANDOM_STRINGS; i++) {
		new_str = random_string();
		printf("String: %s\n", new_str);
		hash_table_insert_entry(ht, new_str, (void *)i, 1);
	}
	
	hash_table_print(ht);
	
	hash_table_delete(ht);
	
	return 0;
}
