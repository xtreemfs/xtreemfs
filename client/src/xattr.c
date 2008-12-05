/* Copyright (c) 2007, 2008  Matthias Hess

   Author: Matthias Hess

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
   C Implementation: xattr

   Description:
 */

#include <stdlib.h>
#include <stdio.h>

#include <string.h>

#ifndef __STRICT_ANSI__
#define __STRICT_ANSI__
#endif

#include <json.h>

#include "xattr.h"

struct xattr *xattr_new(char *key, char *value, size_t size)
{
	struct xattr *rv = NULL;

	rv = (struct xattr *)malloc(sizeof(struct xattr));
	if (rv && xattr_init(rv, key, value, size)) {
		xattr_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int xattr_init(struct xattr *x, char *key, char *value, size_t size)
{
	int err = 0;

	dbg_msg("New key '%s'\n", key);
	dbg_msg("New value '%s'\n", value);

	x->key   = strdup(key);
	x->value = (char *)malloc(size+1);
	memcpy((void *)x->value, value, size);

	/* So far we assume we have only strings! */
	x->value[size] = '\0';

	if (!x->key || !x->value) {
		err = 1;
		goto out;
	}
	INIT_LIST_HEAD(&x->head);
out:
	return err;
}

void xattr_del_contents(struct xattr *x)
{
	free(x->key);
	free(x->value);
}

void xattr_destroy(struct xattr *x)
{
	xattr_del_contents(x);
	free(x);
}


struct xattr_list *xattr_list_new()
{
	struct xattr_list *rv = NULL;

	rv = (struct xattr_list *)malloc(sizeof(struct xattr_list));
	if (rv && xattr_list_init(rv)) {
		xattr_list_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int xattr_list_init(struct xattr_list *xl)
{
	int err = 0;

	INIT_LIST_HEAD(&xl->elems);
	spin_init(&xl->lock, PTHREAD_PROCESS_SHARED);
	xl->key_size = 0;

	return err;
}

void xattr_list_del_contents(struct xattr_list *xl)
{
	struct list_head *i, *n;
	struct xattr *x;

	spin_lock(&xl->lock);
	list_for_each_safe(i, n, &xl->elems) {
		x = container_of(i, struct xattr, head);
		list_del(&x->head);
		xattr_destroy(x);		
	}	
	spin_unlock(&xl->lock);
}

void xattr_list_destroy(struct xattr_list *xl)
{
	xattr_list_del_contents(xl);
	spin_destroy(&xl->lock);
	free(xl);
}

struct xattr_list *xattr_list_clone(struct xattr_list *xl)
{
	struct list_head *i;
	struct xattr_list *target = xattr_list_new();

	list_for_each(i, &xl->elems) {
		struct xattr *element = (struct xattr*)i;
		struct xattr *new_attr = xattr_new(element->key, element->value,
										   strlen(element->value));
		xattr_list_add(target,new_attr);
	}

	return target;
}

int xattr_list_add(struct xattr_list *xl, struct xattr *x)
{
	int err = 0;

	spin_lock(&xl->lock);
	list_add(&x->head, &xl->elems);
	xl->key_size += strlen(x->key) + 1;
	spin_unlock(&xl->lock);

	return err;
}

int xattr_list_add_values(struct xattr_list *xl, char *key, char *val, size_t size)
{
	int err = 0;
	struct xattr *x;

	x = xattr_new(key, val, size);
	if (x)
		err = xattr_list_add(xl, x);
	else
		err = 1;
	
	return err;
}

struct xattr_list *xattr_list_find(struct xattr_list *xl, char *key)
{
	struct xattr_list *rv = NULL;
	struct xattr *new_attr;
	struct list_head *i;
	struct xattr *x;

	rv = xattr_list_new();

	spin_lock(&xl->lock);

	list_for_each(i, &xl->elems) {
		x = container_of(i, struct xattr, head);
		if (!strcmp(x->key, key)) {
			new_attr = xattr_new(key, x->value, strlen(x->value));
			if (new_attr) {
				xattr_list_add(rv, new_attr);
			}
		}
	}	
	spin_unlock(&xl->lock);

	return rv;
}

struct json_object *xattr_list_to_json(struct xattr_list *xl)
{
	struct json_object *rv = NULL;
	struct list_head *i;
	struct xattr *x;

	rv = json_object_new_object();
	if (!rv)
		goto out;

	spin_lock(&xl->lock);
	list_for_each(i, &xl->elems) {
		x = container_of(i, struct xattr, head);
		dbg_msg("Key: %s   Value: %s\n",
			x->key, x->value);
		json_object_object_add(rv, x->key, json_object_new_string(x->value));
	}
	spin_unlock(&xl->lock);
out:
	return rv;
}

struct xattr_list *json_to_xattr_list(struct json_object *jo)
{
	struct xattr_list *rv = NULL;
	char *val_str = NULL;

	dbg_msg("JSON object: %s\n", json_object_to_json_string(jo));

	rv = xattr_list_new();
	if (!rv)
		goto out;

	json_object_object_foreach(jo, key, val) {
		val_str = json_object_to_json_string(val);
		dbg_msg("Adding '%s' -> '%s'\n", key, val_str);
		if (xattr_list_add_values(rv, key, val_str, strlen(val_str))) {
			xattr_list_destroy(rv);
			rv = NULL;
			break;
		}
	}

out:
	return rv;
}

char *xattr_list_key_list(struct xattr_list *xl, size_t *size)
{
	char *rv = NULL;
	char *p;
	struct list_head *i;
	struct xattr *x;

	spin_lock(&xl->lock);
	*size = xl->key_size;
	rv = (char *)malloc(sizeof(char) * *size);
	if (!rv)
		goto out;
	p = rv;
	list_for_each(i, &xl->elems) {
		x = container_of(i, struct xattr, head);
		strcpy(p, x->key);
		dbg_msg("Adding key '%s' to list.\n", x->key);
		p += strlen(x->key) + 1;
	}
	spin_unlock(&xl->lock);

out:
	return rv;
}


int
xattr_list_print(struct xattr_list *xl)
{
	struct list_head *i;
	struct xattr *x;

	spin_lock(&xl->lock);
	list_for_each(i, &xl->elems) {
		x = container_of(i, struct xattr, head);
		dbg_msg("'%s' -> '%s'\n", x->key, x->value);
	}	
	spin_unlock(&xl->lock);

	return 0;
}
