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
   C Implementation: xtreemfs_prot

   Description:
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>

#include "ne_request.h"

#include "xtreemfs_prot.h"
#include "xtreemfs.h"
#include "creds.h"
#include "logger.h"

/* Array that contains the supported protocol version identifiers.
   This array is sorted in such a way that preferred protocols
   are stored towards the end. (This should make it easier to maintain
   the protocol list) */
int client_prot_vers[] = {
	39
};
int client_num_prot_vers = sizeof(client_prot_vers) / sizeof(int);

struct prot_list *client_vers = NULL;


/**
 * Generate client protocol version list
 */
struct prot_list *prot_list_create_client_list()
{
	struct prot_list *rv = NULL;
	int err = 0;
	int i;

	rv = prot_list_new(client_num_prot_vers);
	if (!rv) {
		err = -ENOMEM;
		goto out;
	}

	dbg_msg("Number of client versions: %d\n", client_num_prot_vers);

	for (i=0; i<client_num_prot_vers; i++) {
		dbg_msg("Adding version %d\n", client_prot_vers[i]);
		err |= prot_list_add_vers(rv, client_prot_vers[i]);
	}
out:
	return rv;
}


/* Functions for handling protocol lists */

/**
 * Create a new protocol list with space for 'init_num' entries
 */
struct prot_list *prot_list_new(int init_num)
{
	struct prot_list *rv = NULL;

	rv = (struct prot_list *)malloc(sizeof(struct prot_list));
	if (rv && prot_list_init(rv, init_num)) {
		prot_list_destroy(rv);
		rv = NULL;
	}

	return rv;
}

/**
 * Initialise protocol list
 */
int prot_list_init(struct prot_list *pl, int init_num)
{
	int err = 0;
	int i;

	memset((void *)pl, 0, sizeof(struct prot_list));
	pl->max_num_vers = init_num;
	pl->num_vers = 0;
	pl->vers = (int *)malloc(sizeof(int) * pl->max_num_vers);
	if (!pl->vers) {
		err = 1;
		goto out;
	}

	for (i=0; i<pl->max_num_vers; i++) {
		pl->vers[i] = -1;
	}	
out:	
	return err;
}

/**
 * Delete contents of a protocol list
 */
void prot_list_del_contents(struct prot_list *pl)
{
	free(pl->vers);
	pl->vers = NULL;
}

/**
 * Destroy a protocol version
 */
void prot_list_destroy(struct prot_list *pl)
{
	prot_list_del_contents(pl);
	free(pl);
}


int prot_list_add_vers(struct prot_list *pl, int vers)
{
	int err = 0;
	int i, j;

	for (i=0; i<pl->num_vers && pl->vers[i] != -1 && pl->vers[i] > vers; i++);

	/* We must add the version only, if we do not have it already */
	if (pl->vers[i] == vers)
		goto out;

	for (j=pl->num_vers; j>i; j--)
		pl->vers[j] = pl->vers[j-1];

	pl->vers[i] = vers;

	/* Resize version array, eventually */
	if (++pl->num_vers >= pl->max_num_vers) {
		pl->max_num_vers += 10;
		pl->vers = realloc((void *)pl->vers, sizeof(int) * pl->max_num_vers);
		if (!pl->vers) {
			err = 1;
			goto out;
		}

		for(i=pl->num_vers; i<pl->max_num_vers; i++)
			pl->vers[i] = -1;
	}

out:	
	return err;
}

/**
 * Retrieve the greatest common version from two lists
 */
int prot_list_cmp(struct prot_list *pl1, struct prot_list *pl2)
{
	int rv = -1;
	int l1;
	int l2;
	int *v1, *v2, *dv;
	int i, j, di;

	/* Do the lists exist? */
	if (!pl1 || !pl2) {
		dbg_msg("One of the lists does not exist.\n");
		goto out;
	}

	l1 = pl1->num_vers;
	l2 = pl2->num_vers;

	/* Is one of the lists empty? */
	if (l1==0 || l2==0) {
		dbg_msg("one of the lists does not exist.\n");
		goto out;
	}

	i = 0;
	v1 = pl1->vers;
	j = 0;
	v2 = pl2->vers;

	while (i<l1 && j<l2 && v1[i]!=v2[j]) {

		/* Make sure v1[i] > v2[j] */
		if (v1[i] < v2[j]) {
			di = i;
			i = j;
			j = di;
			di = l1;
			l1 = l2;
			l2 = di;
			dv = v1;
			v1 = v2;
			v2 = dv;
		}

		while (i<=l1 && v1[i]>v2[j]) {
			dbg_msg("Comparing %d with %d\n", v1[i], v2[j]);
			i++;
		}
	}

	if (v1[i] == v2[j])
		rv = v1[i];

out:
	return rv;
}


/* Specifics for JSON channel */

/**
 * Check for an JSON error
 *
 * This function will go into a general 'json channel' that can be
 * shared among OSD and MRC channels...
 */
int json_channel_check_error(ne_session *sess, ne_request *req)
{
	int err = 0;

	return err;
}


/**
 * Convert JSON object to protocol list
 */
struct prot_list *json_to_prot_list(struct json_object *jo)
{
	struct prot_list *rv = NULL;
	int len = 0;

	if (json_object_get_type(jo) != json_type_int) {
		dbg_msg("JSON object does not have the right type.\n");
		goto out;
	}

	len = 1;
	rv = prot_list_new(len);

	if (json_to_prot_list_ip(rv, jo)) {
		prot_list_destroy(rv);
		rv = NULL;
	}

out:
	return rv;
}

/**
 * Convert a JSON object to protocol list in-place
 */
int json_to_prot_list_ip(struct prot_list *pl, struct json_object *jo)
{
	int err = 0;
	struct json_object *idx = NULL;
	int len;
	int i;

	if (json_object_get_type(jo) != json_type_int) {
		dbg_msg("JSON object does not have the right type.\n");
		err = 1;
		goto out;
	}

	err |= prot_list_add_vers(pl, json_object_get_int(jo));

out:
	return err;
}

/**
 * Convert a protocol list into JSON object
 */
struct json_object *prot_list_to_json(struct prot_list *pl)
{
	struct json_object *rv = NULL;
	int i;

	rv = json_object_new_array();
	if (!rv || !pl)
		goto out;

	for (i=0; i<pl->num_vers; i++) {
		json_object_array_add(rv, json_object_new_int(pl->vers[i]));
	}

out:
	return rv;
}


/**
 * Protocol response reader
 *
 * This function simply reads the response of a HTTP request
 * into the given buffer.
 */
int prot_read_resp(void *userdata, const char *buf, size_t len)
{
	ne_buffer *ne_buf = (ne_buffer *)userdata;

	dbg_msg("Reading response.\n");

	if (len > 0) {
		ne_buffer_append(ne_buf, buf, len);
	}

	return NE_OK;
}

/**
 * Accept HTTP request for protcol
 */
static int prot_http_accept(void *userdata, ne_request *req, const ne_status *st)
{
	int acc = 0;
	
	if (st->klass == 2) {
		acc = 1;
		dbg_msg("Accept 200 class\n");
		goto out;
	}

	if (st->klass == 4) {
		if (st->code == 420) {
			acc = 1;
			dbg_msg("Accept 420 code.\n");
			goto out;
		}
	}

	if (st->klass == 5 && st->code == 500) {
		acc = 1;
		goto out;
	}

	dbg_msg("HTTP code: %d\n", st->code);
	if (st->reason_phrase)
		dbg_msg("HTTP reason: %s\n", st->reason_phrase);

out:
	return acc;
}

/**
 * Create a 'getProtocolVersion' request
 *
 * This function creates are neon request to get supported protocol
 * versions from a server. It will send the given versions that
 * the client claims to support.
 * This function does not dispatch the request. That and retrieving
 * the answer must be done somewhere else.
 */
ne_request *prot_getProtocolVersion_req(ne_session *sess,
				        struct creds *creds,
				        struct json_object *req_params)
{
	ne_request *rv = NULL;
	int err = 0;
	char *req_params_str = NULL;
	char auth_str[1024];
	int i;

	/* If there is no own list, skip it. */
	if (!req_params) {
		dbg_msg("No JSON parameter object.\n");
		goto out;
	}

	rv = ne_request_create(sess, "POST", "getProtocolVersion");
	if (!rv) {
		dbg_msg("Cannot create neon request.\n");
		goto out;
	}

	/* It would be great if we could create the body buffer ourselves
	   (and not rely on the JSON object supplying that buffer). BUT
	   there is no way to delete the buffer once the request has been
	   created. So we have to use this hack here. */

	req_params_str = json_object_to_json_string(req_params);
	if (!req_params_str) {
		dbg_msg("Cannot convert JSON object to string.\n");
		goto out;
	}

	ne_set_request_body_buffer(rv, req_params_str,
				   strlen(req_params_str));

	dbg_msg("Params: %s\n", req_params_str);

	if (creds_to_str_ip(creds, auth_str, 1024)) {
		err = 1;
		goto out;
	}

	ne_add_request_header(rv, "Authorization", auth_str);

out:

	return rv;
}

struct prot_list *prot_get_versions(ne_session *sess, struct prot_list *pl)
{
	struct prot_list *rv = NULL;
	int err = 0;
	ne_buffer *buf = NULL;
	ne_request *req = NULL;
	struct creds creds;
	struct json_object *req_params = NULL;
	struct json_object *resp = NULL;
	struct prot_list *me = NULL;

	get_local_creds(&creds);

	if (!pl) {
		dbg_msg("No list to send. Going to create one.\n");
		me = prot_list_create_client_list();
		if (!me) {
			dbg_msg("Cannot create new list.\n");
			goto out;
		}
	} else {
		dbg_msg("Using supplied protocol list.\n");
		me = pl;
	}

	buf = ne_buffer_create();
	if (!buf) {
		dbg_msg("Cannot create neon buffer.\n");
		err = 1;
		goto out;
	}

	req_params = json_object_new_array();
	if (!req_params) {
		dbg_msg("Cannot create JSON parameter array.\n");
		goto out;
	}
	json_object_array_add(req_params, prot_list_to_json(me));

	req = prot_getProtocolVersion_req(sess, &creds, req_params);
	if (!req) {
		dbg_msg("Cannot get protocol version request.\n");
		err = 2;
		goto out;
	}

	ne_buffer_clear(buf);

	/* How to receive the answer */
	ne_add_response_body_reader(req, prot_http_accept,
				    prot_read_resp,
				    (void *)buf);

	if ((err = ne_request_dispatch(req)) != NE_OK) {
		err_msg("Error while dispatching request %p (%d)\n",
			req, err);
		err_msg("Error was: %s\n", ne_get_error(sess));
		goto out;
	}

	print_neon_buffer(buf);

	/* And analyse output */
	if ((err = json_channel_check_error(sess, req))) {
		goto out;
	}

	resp = json_tokener_parse(buf->data);
	if (!resp || is_error(resp)) {
		err = 3;
		goto out;
	}

	if (!(rv = json_to_prot_list(resp))) {
		err = 4;
		goto out;
	}

out:
	creds_delete_content(&creds);

	if (buf)
		ne_buffer_destroy(buf);
	if (req) {
		ne_request_destroy(req);
	}

	if (err && rv) {
		prot_list_destroy(rv);
		rv = NULL;
	}

	if (resp)
		json_object_put(resp);

	if (req_params)
		json_object_put(req_params);

	if (me && !pl)
		prot_list_destroy(me);

	return rv;
}
