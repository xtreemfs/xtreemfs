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
   C Interface: dirservice

   Description:
*/

#ifndef __XTRFS_DIRSERVICE_H__
#define __XTRFS_DIRSERVICE_H__

#include <ne_session.h>
#include <ne_string.h>
#include <ne_uri.h>

#include "creds.h"
#include "list.h"
#include "lock_utils.h"

#define DS_DEFAULT_PORT  32638

#define DSC_ERR_NO_ERROR  0
#define DSC_ERR_NOEXIST   1

/* Entry in the DS */

struct ds_entry {
	char *uuid;
	char *version;
	char *owner;
	time_t lastUpdated;

	/* TODO: This is a crude implementation and should be refined.
	   Possibly be something like a patricia trie. */
	int max_num_attrs;
	int num_attrs;
	char **keys;
	char **values;

	struct list_head head;	/* Embed entries into list */
};

struct ds_entry *ds_entry_new(char *uuid, char *version, char *owner,
			      time_t lastUpdated);
int ds_entry_init(struct ds_entry *de, char *uuid, char *version, char *owner,
		  time_t lastUpdated);
void ds_entry_del_contents(struct ds_entry *de);
void ds_entry_destroy(struct ds_entry *de);

int ds_entry_set_uuid(struct ds_entry *de, char *uuid);
int ds_entry_set_version(struct ds_entry *de, char *version);
int ds_entry_set_owner(struct ds_entry *de, char *owner);
int ds_entry_set_value(struct ds_entry *de, int num, char *value);

int ds_entry_add_key_value(struct ds_entry *de, char *key, char *value);


/* So far this only implemented as a list but could be something else */
struct ds_entry_set {
	struct list_head elems;
};

struct ds_entry_set *ds_entry_set_new();
void ds_entry_set_destroy(struct ds_entry_set *des);

static inline int ds_entry_set_init(struct ds_entry_set *des)
{
	int err = 0;
	INIT_LIST_HEAD(&des->elems);
	return err;
}

static inline int ds_entry_set_add(struct ds_entry_set *des, struct ds_entry *de)
{
	int err = 0;
	list_add(&de->head, &des->elems);
	return err;
}

static inline int ds_entry_set_rem_entry(struct ds_entry_set *des, struct ds_entry *de)
{
	int err = 0;
	list_del(&de->head);
	return err;
}

struct ds_entry_set *json_to_ds_entry_set(struct json_object *jo);

void ds_entry_set_print(struct ds_entry_set *des);


/* Queries for DS */

struct ds_query {
	int max_num;
	int num;
	char **names;
	char **values;
};

struct ds_query *ds_query_new();
int ds_query_init(struct ds_query *dq);
void ds_query_del_contents(struct ds_query *dq);
void ds_query_destroy(struct ds_query *dq);

int ds_query_add_query(struct ds_query *dq, char *name, char *value);

struct json_object *ds_query_to_json(struct ds_query *dq);
struct ds_query *json_to_ds_query(struct json_object *jo);


/* Address mappings */

struct ds_addr {
	char *protocol;
	char *address;
	int port;
	char *match_network;
	int ttl;

	struct list_head head;
};

struct ds_addr *ds_addr_new(char *protocol, char *address, int port,
			   char *match_network, int ttl);
int ds_addr_init(struct ds_addr *da, char *protocol, char *address,
		 int port, char *match_network, int ttl);
void ds_addr_del_contents(struct ds_addr *da);
void ds_addr_destroy(struct ds_addr *da);

struct json_object *ds_addr_to_json(struct ds_addr *da);
struct ds_addr *json_to_ds_addr(struct json_object *jo);


struct ds_addr_map {
	struct list_head elems;

	/* These entries will not be converted to JSON as they are
	   not part of the specification. They will have to be filled
	   in manually. */
	char *uuid;
	int version;

	/* To be inserted into collections */
	struct list_head head;
};

struct ds_addr_map *ds_addr_map_new();
int ds_addr_map_init(struct ds_addr_map *dam);
void ds_addr_map_del_contents(struct ds_addr_map *dam);
void ds_addr_map_destroy(struct ds_addr_map *dam);

int ds_addr_map_add(struct ds_addr_map *dam, struct ds_addr *da);
int ds_addr_map_del(struct ds_addr_map *dam, struct ds_addr *da);

struct json_object *ds_addr_map_to_json(struct ds_addr_map *dam);
struct ds_addr_map *json_to_ds_addr_map(struct json_object *jo);


struct ds_addr_map_set {
	struct list_head elems;
};

struct ds_addr_map_set *ds_addr_map_set_new();
int ds_addr_map_set_init(struct ds_addr_map_set *dams);
void ds_addr_map_set_del_contents(struct ds_addr_map_set *dams);
void ds_addr_map_set_destroy(struct ds_addr_map_set *dams);

int ds_addr_map_set_add(struct ds_addr_map_set *dams, struct ds_addr_map *dam);
int ds_addr_map_set_del(struct ds_addr_map_set *dams, struct ds_addr_map *dam);

struct ds_addr_map_set *json_to_ds_addr_map_set(struct json_object *jo);


/* Generic channel. */

struct ds_gen_channel {
	int (*connect)(struct ds_gen_channel *c);
	int (*disconnect)(struct ds_gen_channel *c);

	int (*registerEntity)(struct ds_gen_channel *c, char *uuid, char *attr, char *val);
	int (*deregisterEntity)(struct ds_gen_channel *c, char *uuid);
};

/* Currently there is only one channel supported so we name it just 'channel'
   It will end up something like 'ds_json_channel' */

struct ds_channel {
	ne_uri uri;
	struct creds         creds;
	ne_session *         session;
	ne_ssl_client_cert * cert;
	int use_ssl;
	ne_buffer *          resp_buf;

	char       *user_buf;
	size_t     user_buf_size;	/* Max size of user_buf */
	size_t     user_buf_len;	/* Actual size */
	int        use_buf;		/* Will be 0 for resp_buf,
                                           1 for user_buf */

	pthread_cond_t wait_cond;
	mutex_t        wait_mutex;
	spinlock_t     lock;

	int err;
};

struct ds_channel *ds_channel_new(char *ds_uri, ne_ssl_client_cert *cert,
				  struct creds *creds);
int ds_channel_init(struct ds_channel *dsc,
		    char *ds_uri, ne_ssl_client_cert *cert,
		    struct creds *creds);
void ds_channel_delete_contents(struct ds_channel *dsc);
void ds_channel_destroy(struct ds_channel *dsc);

int ds_channel_connect(struct ds_channel *dsc);
int ds_channel_disconnect(struct ds_channel *dsc);

int ds_channel_registerEntity(struct ds_channel *dsc, char *uuid,
			      char *attr, char *val, int oldVersion);
int ds_channel_deregisterEntity(struct ds_channel *dsc, char *uuid);
struct ds_entry_set *ds_channel_getEntities(struct ds_channel *dsc,
					    struct ds_query *query,
					    char **attrs, int num_attrs);


struct dirservice {
	char *name;
	ne_uri uri;

	struct ds_channel *channel;

	struct creds creds;	/* Credentials for communication with DS */
};

struct dirservice *dirservice_new(char *ds_uri, ne_ssl_client_cert *cert);
int dirservice_init(struct dirservice *ds,
		    char *ds_uri, ne_ssl_client_cert *cert);
void dirservice_destroy(struct dirservice *ds);

int dirservice_connect(struct dirservice *ds);
int dirservice_disconnect(struct dirservice *ds);

int dirservice_registerEntity(struct dirservice *ds, char *uuid,
			      char *attr, char *val, int oldVersion);
int dirservice_deregisterEntity(struct dirservice *ds, char *uuid);
struct ds_entry_set *dirservice_getEntities(struct dirservice *ds,
					    struct ds_query *query,
					    char **attrs, int num_attrs);
struct ds_addr_map_set *dirservice_getAddressMapping(struct dirservice *dsc,
						 char *uuid);

int dirservice_register_host(struct dirservice *ds, int port);
int dirservice_unregister_host(struct dirservice *ds, int port);

char *dirservice_get_hostaddress(struct dirservice *ds, char *uuid);
int dirservice_get_uri(struct dirservice *ds, char *uuid, ne_uri *uri);
char *dirservice_resolve_uuid(struct dirservice *ds, char *uuid, int def_port);

#endif
