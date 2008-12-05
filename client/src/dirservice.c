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
   C Implementation: dirservice

   Description:
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#ifdef _WIN32
#include <Winsock2.h>
#define boolean json_boolean
#else
#include <netdb.h>
#endif

#include <ne_session.h>
#include <ne_request.h>

#ifndef __STRICT_ANSI__
#define __STRICT_ANSI__
#include <json.h>
#include <json_object.h>
#undef __STRICT_ANSI__
#else
#include <json.h>
#include <json_object.h>
#endif

#include "creds.h"

#include "xtreemfs_utils.h"
#include "xtreemfs_security.h"
#include "dirservice.h"
#include "logger.h"

struct ds_entry *ds_entry_new(char *uuid, char *version, char *owner,
			      time_t lastUpdated)
{
	struct ds_entry *rv = NULL;

	rv = (struct ds_entry *)malloc(sizeof(struct ds_entry));
	if (rv && ds_entry_init(rv, uuid, version, owner, lastUpdated)) {
		ds_entry_destroy(rv);
		rv = NULL;
	}
	return rv;
}

int ds_entry_init(struct ds_entry *de, char *uuid, char *version, char *owner,
		  time_t lastUpdated)
{
	int err = 0;

	if (uuid)
		de->uuid = strdup(uuid);
	else
		de->uuid = NULL;

	if (version)
		de->version = strdup(version);
	else
		de->version = NULL;

	if (owner)
		de->owner = strdup(owner);
	else
		de->owner = NULL;

	de->lastUpdated = lastUpdated;

	de->max_num_attrs = 2;
	de->num_attrs = 0;
	de->keys = (char **)malloc(sizeof(char *) * de->max_num_attrs);
	de->values = (char **)malloc(sizeof(char *) * de->max_num_attrs);

	if (!de->keys || !de->values) {
		err = 1;
		goto out;
	}

	memset((void *)de->keys, 0, sizeof(char *) * de->max_num_attrs);
	memset((void *)de->values, 0, sizeof(char *) * de->max_num_attrs);

	INIT_LIST_HEAD(&de->head);
out:
	return err;
}

void ds_entry_del_contents(struct ds_entry *de)
{
	int i;

	free(de->uuid);
	free(de->version);
	free(de->owner);

	if (de->keys) {
		for(i=0; i<de->num_attrs; i++)
			free(de->keys[i]);
		free(de->keys);
		de->keys = NULL;
	}

	if (de->values) {
		for(i=0; i<de->num_attrs; i++)
			free(de->values[i]);
		free(de->values);
		de->values = NULL;
	}
}

void ds_entry_destroy(struct ds_entry *de)
{
	ds_entry_del_contents(de);
	free(de);
}

int ds_entry_set_uuid(struct ds_entry *de, char *uuid)
{
	int err = 0;

	if (de->uuid)
		free(de->uuid);
	de->uuid = strdup(uuid);

	return err;
}

int ds_entry_set_version(struct ds_entry *de, char *version)
{
	int err = 0;

	if (de->version)
		free(de->version);
	de->version = strdup(version);

	return err;
}

int ds_entry_set_owner(struct ds_entry *de, char *owner)
{
	int err = 0;

	if (de->owner)
		free(de->owner);
	de->owner = strdup(owner);

	return err;
}

int ds_entry_set_value(struct ds_entry *de, int num, char *value)
{
	int err = 0;

	if (de->values[num])
		free(de->values[num]);
	de->values[num] = strdup(value);

	return err;
}

int ds_entry_add_key_value(struct ds_entry *de, char *key, char *value)
{
	int err = 0;
	int act = de->num_attrs;
	int new_max;;
	int num;
	int i;

	/* See if the key is one of the required keys */
	if (!strcmp(key, "uuid")) {
		ds_entry_set_uuid(de, value);
		goto out;
	} else if (!strcmp(key, "version")) {
		ds_entry_set_version(de, value);
		goto out;
	} else if (!strcmp(key, "owner")) {
		ds_entry_set_owner(de, value);
		goto out;
	}

	/* Now let's see if we have the key already and want to change
	   its value only. */
	for (i=0; i<de->num_attrs && strcmp(de->keys[i], key); i++) ;

	if (i < de->num_attrs) {
		ds_entry_set_value(de, i, value);
		goto out;
	}

	/* Nope, we must create a new entry! */
		
	de->keys[act] = strdup(key);
	de->values[act] = strdup(value);

	if (++de->num_attrs >= de->max_num_attrs) {
		de->max_num_attrs += 2;
		new_max = de->max_num_attrs;
		de->keys = (char **)realloc((void *)de->keys,
					    new_max * sizeof(char *));
		de->values = (char **)realloc((void *)de->values,
					       new_max * sizeof(char *));
		act = de->num_attrs;
		num = new_max - act;
		memset((void *)&de->keys[act], 0, sizeof(char *) * num);
		memset((void *)&de->values[act], 0, sizeof(char *) * num);
	}
out:
	return err;
}

void ds_entry_print(struct ds_entry *de)
{
	int i;

	if (de->uuid)
		info_msg("UUID:        %s\n", de->uuid);
	if (de->version)
		info_msg("Version:     %s\n", de->version);
	if (de->owner)
		info_msg("Owner:       %s\n", de->owner);

	for(i=0; i<de->num_attrs; i++) {
		info_msg("%s -> %s\n", de->keys[i], de->values[i]);
	}
}

/* DS Entry Set */

struct ds_entry_set *ds_entry_set_new()
{
	struct ds_entry_set *rv = NULL;

	rv = (struct ds_entry_set *)malloc(sizeof(struct ds_entry_set));
	if (rv && ds_entry_set_init(rv)) {
		ds_entry_set_destroy(rv);
		rv = NULL;
	}

	return rv;
}

void ds_entry_set_del_contents(struct ds_entry_set *des)
{

}

void ds_entry_set_destroy(struct ds_entry_set *des)
{
	ds_entry_set_del_contents(des);
	free(des);
}

struct ds_entry_set *json_to_ds_entry_set(struct json_object *jo)
{
	struct ds_entry_set *rv = NULL;
	struct ds_entry *new_entry = NULL;
	int err = 0;

	rv = ds_entry_set_new();
	if (!rv) {
		err_msg("Cannot create entry set as return value.\n");
		goto out;
	}

	if (!jo || is_error(jo)) {
		err_msg("No valid JSON object!\n");
		err = 1;
		goto out;
	}

	/* Check type of object */
	if (!json_object_is_type(jo, json_type_object)) {
		err_msg("JSON object has wrong type.\n");
		err = 2;
		goto out;
	}

	json_object_object_foreach(jo, key, val) {
		dbg_msg("uuid: %s\n", key);
		if (!json_object_is_type(val, json_type_object)) {
			err_msg("JSON object must be an object.\n");
			err = 3;
			break;
		}
		new_entry = ds_entry_new(key, NULL, NULL, 0L);
		json_object_object_foreach(val, attr, aval) {
			dbg_msg("Attr: %s\n", attr);
			ds_entry_add_key_value(new_entry, attr, json_object_to_json_string(aval));
		}
		ds_entry_set_add(rv, new_entry);
	}
 
out:
	if (err) {
		err_msg("Error during creation of ds entry set.\n");
		ds_entry_set_destroy(rv);
		rv = NULL;
	}
	return rv;
}

void ds_entry_set_print(struct ds_entry_set *des)
{
	struct list_head *iter;
	struct ds_entry *de = NULL;

	list_for_each(iter, &des->elems) {
		de = container_of(iter, struct ds_entry, head);
		ds_entry_print(de);		
	}
}



/* Queries */

struct ds_query *ds_query_new()
{
	struct ds_query *rv = NULL;

	rv = (struct ds_query *)malloc(sizeof(struct ds_query));
	if (rv && ds_query_init(rv)) {
		ds_query_destroy(rv);
		rv = NULL;
	}
	return rv;
}

int ds_query_init(struct ds_query *dq)
{
	int err = 0;

	dq->max_num = 2;
	dq->num = 0;
	dq->names = NULL;
	dq->values = NULL;
	dq->names = (char **)malloc(sizeof(char *) * dq->max_num);
	if (!(dq->names)) {
		err_msg("No memory for names\n");
		err = 1;
		goto out;
	}
	memset((void *)dq->names, 0, sizeof(char *) * dq->max_num);

	dq->values = (char **)malloc(sizeof(char *) * dq->max_num);
	if (!(dq->values)) {
		err_msg("No memory for values\n");
		err = 2;
		goto out;
	}
	memset((void *)dq->values, 0, sizeof(char *) * dq->max_num);

out:
	dbg_msg("Returning %d\n", err);
	return err;
}

void ds_query_del_contents(struct ds_query *dq)
{
	int i;

	if (dq->names) {
		for(i=0; i<dq->num; i++)
			free(dq->names[i]);
		free(dq->names);
		dq->names = NULL;
	}
	if (dq->values) {
		for(i=0; i<dq->num; i++)
			free(dq->values[i]);
		free(dq->values);
		dq->values = NULL;
	}	
}

void ds_query_destroy(struct ds_query *dq)
{
	ds_query_del_contents(dq);
	free(dq);
}

int ds_query_add_query(struct ds_query *dq, char *name, char *value)
{
	int err = 0;
	int act = dq->num;
	int new_max;

	dq->names[act] = strdup(name);
	dq->values[act] = strdup(value);

	if (++dq->num >= dq->max_num) {
		dq->max_num += 2;
		new_max = dq->max_num;
		dq->names = (char **)realloc((void *)dq->names, sizeof(char *) * new_max);
		dq->values = (char **)realloc((void *)dq->values, sizeof(char *) * new_max);
		if (!dq->names || !dq->values)
			err = 1;
	}
 
	return err;
}

struct json_object *ds_query_to_json(struct ds_query *dq)
{
	struct json_object *rv = NULL;
	int i;

	rv = json_object_new_object();
	if (!rv)
		goto out;

	for(i=0; i<dq->num; i++) {
		json_object_object_add(rv, dq->names[i], json_object_new_string(dq->values[i]));
	}	
out:
	return rv;
}


/* Addresses */

struct ds_addr *ds_addr_new(char *protocol, char *address, int port,
			   char *match_network, int ttl)
{
	struct ds_addr *rv = NULL;

	rv = (struct ds_addr *)malloc(sizeof(struct ds_addr));
	if (rv && ds_addr_init(rv, protocol, address, port, match_network, ttl)) {
		ds_addr_destroy(rv);
		rv = NULL;
	}
	return rv;
}

int ds_addr_init(struct ds_addr *da, char *protocol, char *address,
		 int port, char *match_network, int ttl)
{
	int err = 0;

	da->protocol = (protocol ? strdup(protocol) : NULL);
	da->address  = (address  ? strdup(address)  : NULL);
	da->port     = port;
	da->match_network =
		       (match_network ? strdup(match_network) : NULL);
	da->ttl      = ttl;

	INIT_LIST_HEAD(&da->head);

	return err;
}

void ds_addr_del_contents(struct ds_addr *da)
{
	free(da->protocol);
	free(da->address);
	free(da->match_network);
}

void ds_addr_destroy(struct ds_addr *da)
{
	ds_addr_del_contents(da);
	free(da);
}

struct json_object *ds_addr_to_json(struct ds_addr *da)
{
	struct json_object *rv = NULL;

	rv = json_object_new_object();
	if (!rv || is_error(rv)) {
		err_msg("Cannot create new JSON object.\n");
		goto out;
	}

	if (da->protocol)
		json_object_object_add(rv, "protocol",
				       json_object_new_string(da->protocol));
	if (da->address)
		json_object_object_add(rv, "address",
				       json_object_new_string(da->address));
	json_object_object_add(rv, "port", json_object_new_int(da->port));
	if (da->match_network)
		json_object_object_add(rv, "match_network",
				       json_object_new_string(da->match_network));
	json_object_object_add(rv, "ttl", json_object_new_int(da->ttl));

out:	
	return rv;	
}

struct ds_addr *json_to_ds_addr(struct json_object *jo)
{
	struct ds_addr *rv = NULL;

	if (!jo || is_error(jo)) {
		err_msg("Invalid JSON object\n");
		goto out;
	}

	if (!json_object_is_type(jo, json_type_object)) {
		err_msg("JSON object has wrong type.\n");
		goto out;
	}

	rv = ds_addr_new(NULL, NULL, 0, NULL, 0);
	if (!rv) {
		err_msg("Cannot create new addr\n");
		goto out;
	}

	json_object_object_foreach(jo, key, val) {
		if (!strcmp(key, "protocol")) {
			rv->protocol = strdup(json_object_get_string(val));
		} else if (!strcmp(key, "address")) {
			rv->address  = strdup(json_object_get_string(val));
		} else if (!strcmp(key, "port")) {
			rv->port = json_object_get_int(val);
		} else if (!strcmp(key, "match_network")) {
			rv->match_network = strdup(json_object_get_string(val));
		} else if (!strcmp(key, "ttl")) {
			rv->ttl = json_object_get_int(val);
		}		
	}
out:
	return rv;
}



/* Address mapping */

struct ds_addr_map *ds_addr_map_new()
{
	struct ds_addr_map *rv = NULL;

	rv = (struct ds_addr_map *)malloc(sizeof(struct ds_addr_map));
	if (rv && ds_addr_map_init(rv)) {
		ds_addr_map_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int ds_addr_map_init(struct ds_addr_map *dam)
{
	int err = 0;

	INIT_LIST_HEAD(&dam->elems);

	/* Non protocol types */
	dam->uuid     = NULL;
	dam->version  = 0;

	return err;
}

void ds_addr_map_del_contents(struct ds_addr_map *dam)
{
	struct list_head *act;
	struct ds_addr *da;

	while (!list_empty(&dam->elems)) {
		act = dam->elems.next;
		da = container_of(act, struct ds_addr, head);
		list_del(act);
		ds_addr_destroy(da);		
	}

	free(dam->uuid);
}

void ds_addr_map_destroy(struct ds_addr_map *dam)
{
	ds_addr_map_del_contents(dam);
	free(dam);
}

int ds_addr_map_add(struct ds_addr_map *dam, struct ds_addr *da)
{
	int err = 0;
	list_add(&da->head, &dam->elems);
	return err;
}

int ds_addr_map_del(struct ds_addr_map *dam, struct ds_addr *da)
{
	int err = 0;
	list_del(&da->head);
	return err;
}


struct json_object *ds_addr_map_to_json(struct ds_addr_map *dam)
{
	struct json_object *rv = NULL;
	struct list_head *iter;
	struct ds_addr *da;

	rv = json_object_new_object();
	if (!rv || is_error(rv)) {
		rv = NULL;
		goto out;
	}

	list_for_each(iter, &dam->elems) {
		da = container_of(iter, struct ds_addr, head);
		json_object_array_add(rv, ds_addr_to_json(da));
	}

out:
	return rv;
}

struct ds_addr_map *json_to_ds_addr_map(struct json_object *jo)
{
	struct ds_addr_map *rv = NULL;
	struct ds_addr *da = NULL;
	int i, len;
	struct json_object *a;

	if (!jo || is_error(jo)) {
		err_msg("JSON object error.\n");
		goto out;
	}

	if (!json_object_is_type(jo, json_type_array)) {
		err_msg("Wrong JSON type.\n");
		goto out;
	}

	rv = ds_addr_map_new();
	if (!rv) {
		err_msg("Cannot create address map.\n");
		goto out;
	}

	len = json_object_array_length(jo);
	for(i=0; i<len; i++) {
		a = json_object_array_get_idx(jo, i);
		da = json_to_ds_addr(a);
		if (!da) {
			err_msg("Error while converting JSON to addr.\n");
			break;
		}
		ds_addr_map_add(rv, da);
	}
out:
	return rv;
}


/* Address mapping sets */

struct ds_addr_map_set *ds_addr_map_set_new()
{
	struct ds_addr_map_set *rv = NULL;

	rv = (struct ds_addr_map_set *)malloc(sizeof(struct ds_addr_map_set));
	if (rv && ds_addr_map_set_init(rv)) {
		ds_addr_map_set_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int ds_addr_map_set_init(struct ds_addr_map_set *dams)
{
	int err = 0;

	INIT_LIST_HEAD(&dams->elems);

	return err;
}

void ds_addr_map_set_del_contents(struct ds_addr_map_set *dams)
{
	struct list_head *iter;
	struct ds_addr_map *dam;

	while (!list_empty(&dams->elems)) {
		iter = dams->elems.next;
		dam = container_of(iter, struct ds_addr_map, head);
		list_del(iter);
		ds_addr_map_destroy(dam);
	}		
}

void ds_addr_map_set_destroy(struct ds_addr_map_set *dams)
{
	ds_addr_map_set_del_contents(dams);
	free(dams);
}

int ds_addr_map_set_add(struct ds_addr_map_set *dams,
			struct ds_addr_map *dam)
{
	int err = 0;
	list_add(&dam->head, &dams->elems);
	return err;
}

int ds_addr_map_set_del(struct ds_addr_map_set *dams,
			struct ds_addr_map *dam)
{
	int err = 0;
	list_del(&dam->head);
	return err;
}

struct json_object *ds_addr_map_set_to_json(struct ds_addr_map_set *dams)
{
	struct json_object *rv = NULL;

	return rv;
}

struct ds_addr_map_set *json_to_ds_addr_map_set(struct json_object *jo)
{
	struct ds_addr_map_set *rv = NULL;
	struct ds_addr_map *new_map = NULL;
	struct json_object *vers = NULL, *map = NULL;

	if (!jo || is_error(jo))
		goto out;

	if (!json_object_is_type(jo, json_type_object))
		goto out;

	rv = ds_addr_map_set_new();
	if (!rv)
		goto out;

	json_object_object_foreach(jo, key, val) {
		if (!json_object_is_type(val, json_type_array)) {
			err_msg("JSON object is of wrong type.\n");
			break;
		}
		map = json_object_array_get_idx(val, 1);
		new_map = json_to_ds_addr_map(map);
		if (!new_map) {
			err_msg("Cannot create map from JSON object.\n");
			break;
		}
		new_map->uuid = strdup(key);
		vers = json_object_array_get_idx(val, 0);
		new_map->version = json_object_get_int(vers);
		ds_addr_map_set_add(rv, new_map);	
	}
out:
	return rv;
}


/* JSON Channel */

struct ds_channel *ds_channel_new(char *ds_uri, ne_ssl_client_cert *cert,
				  struct creds *creds)
{
	struct ds_channel *rv = NULL;

	rv = (struct ds_channel *)malloc(sizeof(struct ds_channel));
	if (rv && ds_channel_init(rv, ds_uri, cert, creds)) {
		ds_channel_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int ds_channel_init(struct ds_channel *dsc,
		    char *ds_uri, ne_ssl_client_cert *cert,
		    struct creds *creds)
{
	int err = 0;

	dsc->session = NULL;
	dsc->cert = cert;
	dsc->use_ssl = 0;

	str_to_uri(ds_uri, &dsc->uri, DS_DEFAULT_PORT);

	if (dsc->uri.scheme) {
		dbg_msg("DS scheme:   %s\n", dsc->uri.scheme);
		if (!strcmp(dsc->uri.scheme, "https")) {
			dsc->use_ssl = 1;
			dsc->cert    = cert;
		}
	}

	if (dsc->uri.host)
		dbg_msg("DS hostname: %s\n", dsc->uri.host);
	dbg_msg("DS port:     %d\n", dsc->uri.port);

	creds_init(&dsc->creds);
	creds_copy(&dsc->creds, creds);

	dsc->resp_buf = ne_buffer_create();

	dsc->user_buf      = NULL;
	dsc->user_buf_size = 0;
	dsc->user_buf_len  = 0;
	dsc->use_buf       = 0;

	spin_init(&dsc->lock, PTHREAD_PROCESS_SHARED);

	return err;
}

void ds_channel_delete_contents(struct ds_channel *dsc)
{
	ne_close_connection(dsc->session);
	ne_session_destroy(dsc->session);
	if (dsc->resp_buf)
		ne_buffer_destroy(dsc->resp_buf);
	ne_uri_free(&dsc->uri);
	creds_delete_content(&dsc->creds);
}

void ds_channel_destroy(struct ds_channel *dsc)
{
	ds_channel_delete_contents(dsc);
	spin_destroy(&dsc->lock);
	free(dsc);
}

int ds_channel_connect(struct ds_channel *dsc)
{
	int err = 0;

	spin_lock(&dsc->lock);
	if (!dsc->session) {
		dsc->session = ne_session_create(dsc->uri.scheme,
						 dsc->uri.host,
						 dsc->uri.port);
		if (!dsc->session) {
			err = 1;
			goto out;
		}

		if (dsc->use_ssl) {
			if (dsc->cert) {
				ne_ssl_set_clicert(dsc->session, dsc->cert);
				ne_ssl_trust_default_ca(dsc->session);
				ne_ssl_set_verify(dsc->session,
						  xtreemfs_verify_cert,
						  dsc->uri.host);

			} else {
				err_msg("Need a valid certificate.\n");
				err = 2;
			}
		}
	}
out:
	spin_unlock(&dsc->lock);

	return err;
}

int ds_channel_disconnect(struct ds_channel *dsc)
{
	int err = 0;

	spin_lock(&dsc->lock);
	if (dsc->session) {
		ne_close_connection(dsc->session);
	}
	spin_unlock(&dsc->lock);

	return err;
}

int ds_channel_http_accept(void *userdata, ne_request *req, const ne_status *st)
{
	int acc = 0;
	struct ds_channel *dsc = (struct ds_channel *)userdata;

	dsc->err = DSC_ERR_NO_ERROR;
	
	if (st->klass == 2) {
		acc = 1;
		dbg_msg("Accept 200 class\n");
		goto finish;
	}

	if (st->klass == 4) {
		dbg_msg("400 class.\n");
		switch (st->code) {
			case 404:
				dbg_msg("Object does not exits on DS.\n");
				dsc->err = DSC_ERR_NOEXIST;
			break;
			default:
				acc = 1;
			break;
		}
		goto finish;
	}

	dbg_msg("HTTP code: %d\n", st->code);
	if (st->reason_phrase)
		dbg_msg("HTTP reason: %s\n", st->reason_phrase);

finish:
	return acc;
}

int ds_channel_req_resp_reader(void *userdata, const char *buf, size_t len)
{
	struct ds_channel *dsc = (struct ds_channel *)userdata;
	char *ubuf = (char *)dsc->user_buf;

	dbg_msg("Reading DSC response.\n");

	if(len > 0) {
		if (dsc->use_buf == 0)
			ne_buffer_append(dsc->resp_buf, buf, len);
		else {
			if (dsc->user_buf_len + len <= dsc->user_buf_size) {
				memcpy(&ubuf[dsc->user_buf_len], (void *)buf, len);
				dsc->user_buf_len += len;
			} else {
				ne_buffer_append(dsc->resp_buf, dsc->user_buf, dsc->user_buf_len);
				ne_buffer_append(dsc->resp_buf, buf, len);
				dsc->use_buf = 0;
			}
		}
	} else {
		dbg_msg("Request has finished\n");
		if (dsc->use_buf)
			dbg_msg("%s\n", dsc->user_buf);
		else
			dbg_msg("%s\n", dsc->resp_buf);
		// pthread_cond_broadcast(&oc->wait_cond);
	}
	
	return NE_OK;
}

int ds_channel_resp_headers(struct ds_channel *dsc, ne_request *req)
{
	int err = 0;
	void *ne_cursor = NULL;
	char *header_name = NULL;
	char *header_value = NULL;

	ne_cursor = NULL;

	while ((ne_cursor =
		ne_response_header_iterate(req, ne_cursor,
					   (const char **)&header_name,
					   (const char **)&header_value))) {
		if (!header_name || !header_value)
			dbg_msg("Empty return values.\n");

		dbg_msg("Header:   %s -> %s\n", header_name, header_value);
	}


	return err;
}

ne_request *ds_channel_new_req(struct ds_channel *dsc, char *funcname)
{
	ne_request *rv = NULL;
	char auth_str[1024];

	spin_lock(&dsc->lock);
	
	rv = ne_request_create(dsc->session, "POST", funcname);
	if (!rv)
		goto out;

	memset((void *)auth_str, 0, 1024);

	if (creds_to_str_ip(&dsc->creds, auth_str, 1024)) {
		ne_request_destroy(rv);
		rv = NULL;
		goto out;
	}

	/* snprintf(auth_str, 1024, "nullauth blabla"); */
	dbg_msg("Authorization string: '%s'\n", auth_str);

	ne_add_request_header(rv, "Authorization", auth_str);

out:
	spin_unlock(&dsc->lock);

	return rv;
}

int ds_channel_dispatch_req(struct ds_channel *dsc,
			    ne_request *ne_req, char *req_param_str)
{
	int err = 0;
	int ne_err;
	ne_status const *status = NULL;

	spin_lock(&dsc->lock);

	ne_set_request_body_buffer(ne_req, req_param_str,
				   strlen(req_param_str));

	/* How to receive the answer */
	ne_add_response_body_reader(ne_req, ds_channel_http_accept,
				    ds_channel_req_resp_reader, (void *)dsc);

	/* Clear response buffer */
	ne_buffer_clear(dsc->resp_buf);
	
	/* Submit request */
	if((ne_err = ne_request_dispatch(ne_req)) != NE_OK) {
		err_msg("Error %d while dispatching the request.\n", ne_err);
		status = ne_get_status(ne_req);
		err_msg("Reason: %s\n", ne_get_error(dsc->session));
		err = 2;
		goto out;
	}

	/* Get headers from request ans prepare them for analyzation */
	err = ds_channel_resp_headers(dsc, ne_req);

out:
	spin_unlock(&dsc->lock);

	return err;
}

struct json_object *ds_channel_get_answer(struct ds_channel *dsc)
{
	struct json_object *rv;

	/* And analyse the answer */
	if (dsc->use_buf) {
		dbg_msg("Buffer: %s\n", dsc->user_buf);
		dbg_msg("Buf len: %d\n", dsc->user_buf_len);
		rv = json_tokener_parse(dsc->user_buf);
	} else {
		dbg_msg("Buffer: %s\n", dsc->resp_buf->data);
		dbg_msg("Buf len: %d\n", dsc->resp_buf->used);
		rv = json_tokener_parse(dsc->resp_buf->data);
	}

	return rv;
}

int ds_channel_registerEntity(struct ds_channel *dsc, char *uuid,
			      char *attr, char *val, int oldVersion)
{
	int err = 0;
	ne_request *ne_req = NULL;
	struct json_object *req_param;
	char *req_param_str = NULL;
	struct json_object *data_obj;

	ne_req = ds_channel_new_req(dsc, "registerEntity");
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	req_param = json_object_new_array();
	if (!req_param) {
		err = 3;
		goto out;
	}

	/* Create parameter object */
	json_object_array_add(req_param, json_object_new_string(uuid));
	data_obj = json_object_new_object();
	json_object_object_add(data_obj, attr, json_object_new_string(val));
	json_object_array_add(req_param, data_obj);
	json_object_array_add(req_param, json_object_new_int(oldVersion));
	req_param_str = strdup(json_object_to_json_string(req_param));
	json_object_put(req_param);

	dbg_msg("Msg string: '%s'\n", req_param_str);

	err = ds_channel_dispatch_req(dsc, ne_req, req_param_str);

out:
	if (ne_req) {
		ne_request_destroy(ne_req);
	}
	free(req_param_str);
	
	return err;
}

int ds_channel_deregisterEntity(struct ds_channel *dsc, char *uuid)
{
	int err = 0;
	ne_request *ne_req = NULL;
	struct json_object *req_param;
	char *req_param_str = NULL;

	ne_req = ds_channel_new_req(dsc, "deregisterEntity");
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	req_param = json_object_new_array();
	if (!req_param) {
		err = 3;
		goto out;
	}

	/* Create parameter object */
	json_object_array_add(req_param, json_object_new_string(uuid));
	req_param_str = strdup(json_object_to_json_string(req_param));
	json_object_put(req_param);

	dbg_msg("Msg string: '%s'\n", req_param_str);

	err = ds_channel_dispatch_req(dsc, ne_req, req_param_str);

out:
	if (ne_req) {
		ne_request_destroy(ne_req);
	}
	free(req_param_str);

	return err;
}

struct ds_entry_set *ds_channel_getEntities(struct ds_channel *dsc,
					    struct ds_query *dq,
					    char **attrs,
					    int num_attrs)
{
	struct ds_entry_set *rv = NULL;
	int err = 0;
	ne_request *ne_req = NULL;
	struct json_object *req_param;
	struct json_object *attrs_obj = NULL;
	struct json_object *answer_obj = NULL;
	char *req_param_str = NULL;
	int i;

	ne_req = ds_channel_new_req(dsc, "getEntities");
	if (!ne_req) {
		err_msg("Cannot create new neon request.\n");
		err = -ENOMEM;
		goto out;
	}

	req_param = json_object_new_array();
	if (!req_param) {
		err_msg("Cannot create parameter object.\n");
		err = 3;
		goto out;
	}

	/* Fill in parameter object */
	json_object_array_add(req_param, ds_query_to_json(dq));
	attrs_obj = json_object_new_array();
	if (attrs) {
		for(i=0; i<num_attrs; i++) {
			json_object_array_add(attrs_obj, json_object_new_string(attrs[i]));
			dbg_msg("Adding '%s'\n", attrs[i]);
		}
	}
	json_object_array_add(req_param, attrs_obj);
	req_param_str = strdup(json_object_to_json_string(req_param));
	json_object_put(req_param);

	dbg_msg("Msg string: '%s'\n", req_param_str);

	err = ds_channel_dispatch_req(dsc, ne_req, req_param_str);
	if (err)
		goto out;

	answer_obj = ds_channel_get_answer(dsc);

	/* And analyse the answer */
	if (!answer_obj) {
		err_msg("Cannot create JSON answer object.\n");
		err = 3;
		goto out;
	}

	rv = json_to_ds_entry_set(answer_obj);
 
out:
	if (ne_req)
		ne_request_destroy(ne_req);

	free(req_param_str);

	if (err && rv) {
		ds_entry_set_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int ds_channel_registerAddressMapping(struct ds_channel *dsc,
				      char *uuid,
				      struct ds_addr_map *dam,
				      int oldVersion)
{
	int err = 0;
	ne_request *ne_req = NULL;
	struct json_object *req_param = NULL;
	char *req_param_str = NULL;

	ne_req = ds_channel_new_req(dsc, "registerAddressMapping");
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	req_param = json_object_new_array();
	if (!req_param) {
		err_msg("Cannot create parameter object.\n");
		err = 3;
		goto out;
	}

	json_object_array_add(req_param, json_object_new_string(uuid));
	json_object_array_add(req_param, ds_addr_map_to_json(dam));
	json_object_array_add(req_param, json_object_new_int(oldVersion));
	req_param_str = strdup(json_object_to_json_string(req_param));
	json_object_put(req_param);

	err = ds_channel_dispatch_req(dsc, ne_req, req_param_str);

out:
	if (ne_req)
		ne_request_destroy(ne_req);

	free(req_param_str);
	return err;
}

int ds_channel_deregisterAddressMapping(struct ds_channel *dsc,
					char *uuid)
{
	int err = 0;
	ne_request *ne_req = NULL;
	struct json_object *req_param = NULL;
	char *req_param_str = NULL;

	ne_req = ds_channel_new_req(dsc, "deregisterAddressMapping");
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	req_param = json_object_new_array();
	if (!req_param) {
		err_msg("Cannot create parameter object.\n");
		err = 3;
		goto out;
	}

	json_object_array_add(req_param, json_object_new_string(uuid));
	req_param_str = strdup(json_object_to_json_string(req_param));
	json_object_put(req_param);

	err = ds_channel_dispatch_req(dsc, ne_req, req_param_str);

out:
	if (ne_req)
		ne_request_destroy(ne_req);

	free(req_param_str);
	return err;
}

struct ds_addr_map_set *ds_channel_getAddressMapping(struct ds_channel *dsc,
						 char *uuid)
{
	struct ds_addr_map_set *rv = NULL;
	int err = 0;
	ne_request *ne_req = NULL;
	struct json_object *req_param = NULL;
	struct json_object *answer_obj = NULL;
	char *req_param_str = NULL;
	char *empty = "";

	ne_req = ds_channel_new_req(dsc, "getAddressMapping");
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	req_param = json_object_new_array();
	if (!req_param) {
		err_msg("Cannot create parameter object.\n");
		err = 3;
		goto out;
	}

	/* An empty uuid is possible */
	if (uuid)
		json_object_array_add(req_param, json_object_new_string(uuid));
	else
		json_object_array_add(req_param, json_object_new_string(empty));

	req_param_str = strdup(json_object_to_json_string(req_param));
	json_object_put(req_param);

	dbg_msg("Parameter string: %s\n", req_param_str);

	err = ds_channel_dispatch_req(dsc, ne_req, req_param_str);

	answer_obj = ds_channel_get_answer(dsc);
	if (!answer_obj || is_error(answer_obj)) {
		err_msg("Cannot get answer.\n");
		goto out;
	}
	rv = json_to_ds_addr_map_set(answer_obj);
out:
	if (ne_req)
		ne_request_destroy(ne_req);

	if (req_param_str)
		free(req_param_str);

	if (answer_obj && !is_error(answer_obj))
		json_object_put(answer_obj);

	return rv;
}


struct dirservice *dirservice_new(char *ds_uri, ne_ssl_client_cert *cert)
{
	struct dirservice *rv = NULL;

	rv = (struct dirservice *)malloc(sizeof(struct dirservice));
	if (rv && dirservice_init(rv, ds_uri, cert)) {
		dirservice_destroy(rv);
		rv = NULL;
	}

	return rv;
}

int dirservice_init(struct dirservice *ds,
		    char *ds_uri, ne_ssl_client_cert *cert)
{
	int err = 0;

	if (creds_init(&ds->creds)) {
		err = 1;
		goto out;
	}
	get_local_creds(&ds->creds);

	ds->name = strdup(ds_uri);
	str_to_uri(ds_uri, &ds->uri, DS_DEFAULT_PORT);
	ds->channel = ds_channel_new(ds_uri, cert, &ds->creds);
	if (!ds->channel) {
		err = 1;
		goto out;
	}

out:
	return err;
}

void dirservice_destroy(struct dirservice *ds)
{
	if (ds->channel) {
		ds_channel_destroy(ds->channel);
	}

	creds_delete_content(&ds->creds);
	ne_uri_free(&ds->uri);
	free(ds->name);
	free(ds);
}

int dirservice_connect(struct dirservice *ds)
{
	return ds_channel_connect(ds->channel);
}

int dirservice_disconnect(struct dirservice *ds)
{
	return ds_channel_disconnect(ds->channel);
}


int dirservice_registerEntity(struct dirservice *ds, char *uuid,
			      char *attr, char *val, int oldVersion)
{
	return ds_channel_registerEntity(ds->channel, uuid, attr,
					 val, oldVersion);
}

int dirservice_deregisterEntity(struct dirservice *ds, char *uuid)
{
	return ds_channel_deregisterEntity(ds->channel, uuid);
}

struct ds_entry_set *dirservice_getEntities(struct dirservice *ds,
					    struct ds_query *dq,
					    char **attrs,
					    int num_attrs)
{
	return ds_channel_getEntities(ds->channel, dq, attrs, num_attrs);
}

struct ds_addr_map_set *dirservice_getAddressMapping(struct dirservice *ds,
						 char *uuid)
{
	return ds_channel_getAddressMapping(ds->channel, uuid);
}

int dirservice_register_host(struct dirservice *ds, int port)
{
	int err = 0;
	char hostname[1024];
	struct hostent hent, *res;
	char buf[1024];
	char *act_addr = NULL;

	gethostname(hostname, 1024);

#ifdef __linux__
	if (gethostbyname_r(hostname, &hent, buf, 1024, &res, &err)) {
		err_msg("Cannot get host information.\n");
		err = 1;
		goto out;
	}

	if (!res) {
		dbg_msg("Host not found in host database (which is kind of strange)\n");
		err = 2;
		goto out;
	}

	dbg_msg("Official hostname: %s\n", hent.h_name);
	dbg_msg("Host address type: %d\n", hent.h_addrtype);

	act_addr = hent.h_addr_list[0];
	while (act_addr) {
		
		act_addr++;
	}
out:
#endif 
	return err;
}

int dirservice_unregister_host(struct dirservice *ds, int port)
{
	int err = 0;

	return err;
}

char *dirservice_get_hostaddress(struct dirservice *ds, char *uuid)
{
	char *rv = NULL;
	struct ds_addr_map_set *map = NULL;
	struct ds_addr_map *dam = NULL;
	struct ds_addr *da = NULL;
	char port_str[22];
	int len;

	map = ds_channel_getAddressMapping(ds->channel, uuid);
	if (map == NULL) {
		goto out;
	}

	/* TODO: Better address handling, matching of networks etc. So far we only
		 take the first entry... */
	if (!list_empty(&map->elems)) {
		dam = container_of(map->elems.next, struct ds_addr_map, head);
		da = container_of(dam->elems.next, struct ds_addr, head);
		snprintf(port_str, 7, "%d", da->port);
		dbg_msg("Protocol    '%s'\n", da->protocol);
		dbg_msg("Address:    '%s'\n", da->address);
		dbg_msg("Port:       '%s'\n", port_str);

		len = strlen(da->protocol) + 3 + strlen(da->address) + 1 + strlen(port_str) + 1;
		rv = (char *)malloc(sizeof(char) * (len + 1));
		snprintf(rv, len, "%s://%s:%s\n", da->protocol, da->address, port_str);
		rv[len-1] = '\0';
	}
	
out:
	if (map)
		ds_addr_map_set_destroy(map);

	return rv;
}

/**
 * Tranlsate UUID to URI
 */
int dirservice_get_uri(struct dirservice *ds, char *uuid, ne_uri *uri)
{
	int err = 0;
	char *url = NULL;

	url = dirservice_get_hostaddress(ds, uuid);
	if (!url) {
		err = 1;
		goto out;
	}

	err = str_to_uri(url, uri, 0);

out:
	return err;
}

/**
 * Resolve an UUID into the corresponding URL
 *
 * This functions first tries to resolve the given UUID directly
 * at the directory service. If that does not succeed it tries to
 * treat the UUID as a pseudo UUID (meaning a stripped down URL).
 * If that does not succeed either, it tries to determine, if the
 * given UUID is actually a URL itself. The latter case resembles
 * a transition phase, where UUID could potentially be (pseudo)
 * URLs.
 */
char *dirservice_resolve_uuid(struct dirservice *ds, char *uuid, int def_port)
{
	char *rv = NULL;
	char *pseudo_uuid = NULL;
	int len;
	ne_uri uri;
	char port_str[7];	/* At most 6 + '\0' chars */

	rv = dirservice_get_hostaddress(ds, uuid);
	if (rv)
		goto out;

	/* Nope, it was not that easy... Maybe the UUID is a pseudo
	   URL. */

	pseudo_uuid = extract_pseudo_uuid(uuid);
	rv = dirservice_get_hostaddress(ds, pseudo_uuid);
	if (rv)
		goto out;

	/* Still no success. As a last resort we try to check if
	   the given 'uuid' is a proper URL itself. */
	if (str_to_uri(uuid, &uri, def_port)) {
		dbg_msg("No success in translating UUID into something meaningful...\n");
		goto out;
	}

	/* Construct a URL from uri. */

	len =  strlen(uri.scheme);
	len += 3;
	len += strlen(uri.host);
	if (uri.port != 0) {
		snprintf(port_str, 8, "%d", uri.port);
		len += 1 + strlen(port_str);
	}
	len ++;

	rv = (char *)malloc(sizeof(char) * (len + 1));
	if (!rv)
		goto out;

	if (uri.port)
		snprintf(rv, len, "%s://%s:%s", uri.scheme, uri.host, port_str);
	else
		snprintf(rv, len, "%s://%s", uri.scheme, uri.host);

out:
	free(pseudo_uuid);

	return rv;
}
