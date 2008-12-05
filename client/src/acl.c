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
 * C Implementation: acl
 *
 * Description: 
 *
 *
 * Author: Matthias Hess mhess at hpce dot nec dot com
 *
 * Copyright: Matthias Hess, 2007, 2008
 *
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>

#include <json.h>

#include "acl.h"

#include "lock_utils.h"
#include "logger.h"

/* Definition taken from MRC code. */
#define ACL_MODE_READ    0x0001
#define ACL_MODE_WRITE   0x0002
#define ACL_MODE_EXEC    0x0004
#define ACL_MODE_APPEND  0x0008
#define ACL_MODE_GAPP    0x0010
#define ACL_MODE_CREAT   0x0020
#define ACL_MODE_TRUNC   0x0040
#define ACL_MODE_SREAD   0x0080
#define ACL_MODE_DEL     0x0100


/**
 * Convert mode_str to ACL mode (see above).
 * The string must contain characters 'r', 'w', 'x' and '-' only.
 */
int mode_str_to_mode(char *mode_str)
{
	int rv = 0;
	
	/*! \todo: For full compliance with withdrawn POSIX ACL standard, some more
	           checks have to be done (like checking length).
	 */
	while(*mode_str != '\0' && rv >= 0) {
		switch(*mode_str) {
		case 'r': rv |= ACL_MODE_READ;  break;
		case 'w': rv |= ACL_MODE_WRITE; break;
		case 'x': rv |= ACL_MODE_EXEC;  break;
		case '-': /* ignore */ break;
		default:
			dbg_msg("Ill defined permission string.\n");
			rv = -1;
		break;
		}
		mode_str++;
	}
	
	return rv;
}

/**
 * Create a new ACL entry and initialize it.
 */
struct acl_entry *acl_entry_new()
{
	struct acl_entry *rv = NULL;
	
	rv = (struct acl_entry *)malloc(sizeof(*rv));
	if (!rv)
		goto finish;

	rv->tag       = NULL;
	rv->qualifier = NULL;
	rv->perms     = NULL;

	INIT_LIST_HEAD(&rv->head);

finish:
	return rv;
}

/**
 * Destroy an ACL entry (and free all memory).
 */
void acl_entry_destroy(struct acl_entry *ae)
{
	free(ae->tag);
	free(ae->qualifier);
	free(ae->perms);
	free(ae);
}

/**
 * Convert an ACL entry to JSON representation.
 */
struct json_object *acl_entry_to_json(struct acl_entry *ae)
{
	struct json_object *rv = NULL;
	char entity_str[1024];
	long pmode;
	
	rv = json_object_new_object();
	if (!rv)
		goto finish;
	
	pmode = mode_str_to_mode(ae->perms);
	snprintf(entity_str, 1024, "%s:%s:", ae->tag, ae->qualifier);
	
	json_object_object_add(rv, entity_str, json_object_new_int(pmode));
	
finish:
	return rv;
}

/**
 * Convert a JSON object representing an ACL entry to the ACL entry struct.
 */
struct acl_entry *json_to_acl_entry(struct json_object *jo)
{
	struct acl_entry *rv = NULL;
	
	return rv;
}

/**
 * Create a string from tag and qualifier field of an ACL entry.
 */
char *acl_entry_tq_to_str(struct acl_entry *ae)
{
	char *rv = NULL;
	size_t len = 0, tlen = 0, qlen = 0, consistency;
	
	if (ae->tag) {
		tlen = strlen(ae->tag);
		len += tlen;
	}
	if (ae->qualifier) {
		qlen = strlen(ae->qualifier);
		len += qlen;
	}
	len += 2 + 1;	/* Add two ':' characters and a '\0' */
	consistency = len;
	
	rv = (char *)malloc(sizeof(char) * len);
	if (!rv)
		goto finish;
		
	len = 0;
	if (ae->tag) {
		strcpy(rv, ae->tag);
		len += tlen;
	}
	strcpy(&rv[len], ":");
	len++;
	if (ae->qualifier) {
		strcpy(&rv[len], ae->qualifier);
		len += qlen;
	}
	strcpy(&rv[len], ":");
	len++;
	rv[len] = '\0';

	if (consistency != len+1) {
		dbg_msg("Error! %d should be %d\n", consistency, len+1);
	}

finish:
	return rv;
}


/**
 * Print an ACL entry in info mode.
 */
void acl_entry_print(struct acl_entry *ae)
{
	char out_str[256];
	int len = 0;
	size_t slen;
	
	out_str[0] = '\0';
	if (ae->tag) {
		slen = strlen(ae->tag);
		if (slen < 256) {	
			strcpy(out_str, ae->tag);
			len += slen;
		}
	}
	if (len + 1 < 256) {
		strcpy(&out_str[len], ":");
		len++;
	}
	if (ae->qualifier) {
		slen = strlen(ae->qualifier);
		if (len + slen < 256) {
			strcpy(&out_str[len], ae->qualifier);
			len += slen;
		}
	}
	if (len + 1 < 256) {
		strcpy(&out_str[len], ":");
		len++;
	}
	if (ae->perms) {
		slen = strlen(ae->perms);
		if (len + slen < 256)
			strcpy(&out_str[len], ae->perms);
	}
	info_msg("%s\n", out_str);
}

/**
 * Initialize an ACL list.
 */
int acl_list_init(struct acl_list *al)
{
	int err = 0;
	
	INIT_LIST_HEAD(&al->acls);
	spin_init(&al->lock, PTHREAD_PROCESS_PRIVATE);
	
	return err;
}

/**
 * Destroy all entries in an ACL list and free their memory.
 */
void acl_list_clean(struct acl_list *al)
{
	struct list_head *iter;
	struct acl_entry *ae;
	
	spin_lock(&al->lock);
	iter = &al->acls;
	while (!list_empty(&al->acls)) {
		iter = al->acls.next;
		list_del(iter);
		ae = container_of(iter, struct acl_entry, head);
		acl_entry_destroy(ae);
	}
	spin_unlock(&al->lock);
	spin_destroy(&al->lock);
}

/**
 * Create an acl entry from given parameters and add it to the list.
 */
int acl_list_add(struct acl_list *al, char *tag, char *qualifier, char *perms)
{
	int err = 0;
	struct acl_entry *ne = NULL;
	
	if (!tag) {
		dbg_msg("No tag specified.\n");
		err = 1;
		goto finish;
	}

	if (!perms) {
		dbg_msg("No permission specified.\n");
		err = 2;
		goto finish;
	}

	ne = acl_entry_new();
	if (!ne) {
		dbg_msg("Cannot allocate memory for new entry.\n");
		err = 3;
		goto finish;
	}

	ne->tag = strdup(tag);
	if (qualifier)
		ne->qualifier = strdup(qualifier);
	ne->perms = strdup(perms);

	list_add(&ne->head, &al->acls);

finish:
	if (err && ne) {
		acl_entry_destroy(ne);
		free(ne);
	}

	return err;
}

/**
 * Print an ACL list in info mode.
 */
void acl_list_print(struct acl_list *al)
{
	struct list_head *iter;
	struct acl_entry *ae;
	
	spin_lock(&al->lock);
	list_for_each(iter, &al->acls) {
		ae = container_of(iter, struct acl_entry, head);
		acl_entry_print(ae);
	}
	spin_unlock(&al->lock);
}


/**
 * Turn an ACL list into the corresponding POSIX mode.
 * This might lead to loss of information because POSIX mode does
 * not necessarily cover all possible ACL entries. (Entries for
 * specific users might be lost, for instance).
 */
mode_t acl_list_to_mode(struct acl_list *al)
{
	mode_t rv = 0;
	/* struct acl_entry *ae; */
	
	return rv;
}

/**
 * Fill in an ACL list with entries corr. to POSIX mode.
 */
int mode_to_acl_list(mode_t mode, struct acl_list *al)
{
#ifndef _WIN32
	int err = 0;
	char *tags[] = { "user", "group", "other", NULL };
	int flags[] = { S_IRUSR, S_IWUSR, S_IXUSR,
			S_IRGRP, S_IWGRP, S_IXGRP,
			S_IROTH, S_IWOTH, S_IXOTH };
	char flags_str[] = { 'r', 'w', 'x' };
	char mode_str[4];
	int i, j;
	struct acl_entry *ae;
	
	mode_str[3] = '\0';
	for (i=0; tags[i] != NULL; i++) {
		ae = acl_entry_new();
		if (!ae) {
			err = -ENOMEM;
			break;
		}
		for (j=0; j<3; j++) {
			if (mode & flags[i*3+j])
				mode_str[j] = flags_str[j];
			else
				mode_str[j] = '-';
		}
		ae->tag = strdup(tags[i]);
		ae->perms = strdup(mode_str);
		/* Qualifier not set because POSIX mode does not contain info
		   on this entry. */
		list_add_tail(&ae->head, &al->acls);
	}
	
	if (err)
		acl_list_clean(al);
		
	return err;
#endif
}

/**
 * Create a JSON object representing the ACL list.
 */
struct json_object *acl_list_to_json(struct acl_list *al)
{
	struct json_object *rv = NULL;
	struct list_head *iter;
	struct acl_entry *ae;
	int mode;
	char *tq_str;
	
	rv = json_object_new_object();
	if (!rv)
		goto finish;
	
	spin_lock(&al->lock);
	list_for_each(iter, &al->acls) {
		ae = container_of(iter, struct acl_entry, head);
		mode = mode_str_to_mode(ae->perms);
		tq_str = acl_entry_tq_to_str(ae);
		if (!tq_str) {
			dbg_msg("No memory for ACL entry string!\n");
			json_object_put(rv);
			rv = NULL;
			goto finish;
		}
		dbg_msg("tq string: %s\n", tq_str);
		json_object_object_add(rv, tq_str, json_object_new_int(mode));
		free(tq_str);
	}
	spin_unlock(&al->lock);
	
finish:
	return rv;
}

/**
 * Fill in the ACL list with entries corr. to the JSON object.
 */
int json_to_acl_list(struct json_object *jo, struct acl_list *al)
{
	int err = 0;
	
	acl_list_init(al);
	
	return err;
}
