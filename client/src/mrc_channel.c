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
   C Implementation: mrc_channel

   Description:
   The protocol between client and MRC is transported over a communication
   channel. This file contains one possible implementation of such a channel
   namely, the protocol is represented in a JSON fashion and transported
   over an (extended) http connection.

   Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007
           Erich Focht <efocht at hpce dot nec dot com>

   Copyright: See COPYING file that comes with this distribution

*/

#include <stdlib.h>
#include <string.h>

#include <errno.h>
#include <sys/types.h>
#ifndef _WIN32
#include <pwd.h>
#include <grp.h>
#endif


/* Even if struct ANSI is not required, we need to define it
   here because JSONs foreach construct uses C99 which we do
   not like here... */
#ifndef __STRICT_ANSI__
#define __STRICT_ANSI__
#include <json_object.h>
#undef __STRICT_ANSI__
#else
#include <json_object.h>
#endif

#include <ne_request.h>

#include "xtreemfs_security.h"
#include "xtreemfs_utils.h"

#include "mrc.h"
#include "mrc_channel.h"

#include "stripe.h"
#include "stripingpolicy.h"
#include "statinfo.h"
#include "xloc.h"
#include "xcap.h"
#include "xcap_inv.h"
#include "xattr.h"

#include "logger.h"

/* All MRC channel functions start with a prefix MRCC and have the same set
   of parameters. So we can make function declaration very efficient. */
#define DECL_MRCC_FUNC(x) static int MRCC_##x(struct MRC_Channel *mc, struct MRC_payload *p)

DECL_MRCC_FUNC(initFS);

DECL_MRCC_FUNC(createVol);
DECL_MRCC_FUNC(deleteVol);
DECL_MRCC_FUNC(getVols);

DECL_MRCC_FUNC(addReplica);

DECL_MRCC_FUNC(changeAccessMode);
DECL_MRCC_FUNC(checkAccessMode);
DECL_MRCC_FUNC(changeOwner);

DECL_MRCC_FUNC(setACLEntries);
DECL_MRCC_FUNC(remACLEntries);

DECL_MRCC_FUNC(getXAttr);
DECL_MRCC_FUNC(setXAttrs);
DECL_MRCC_FUNC(remXAttrs);

DECL_MRCC_FUNC(stat);
DECL_MRCC_FUNC(move);

DECL_MRCC_FUNC(createDir);
DECL_MRCC_FUNC(readDir);

DECL_MRCC_FUNC(createFile);
DECL_MRCC_FUNC(delete);
DECL_MRCC_FUNC(open);
DECL_MRCC_FUNC(updateFileSize);
DECL_MRCC_FUNC(createSymLink);
DECL_MRCC_FUNC(createLink);

DECL_MRCC_FUNC(renew);


/* This must be synchronized with the structures in 'mrc_request.h' */

#define EXEC_MAP_ENTRY(x,y,z) [x] = { x, y, z }

struct MRCC_exec_map MRCC_exec_map[] = {
	EXEC_MAP_ENTRY(COM_UNDEFINED,       "undefined",          NULL),
	
	EXEC_MAP_ENTRY(COM_INIT_FS,         "initFileSystem",     MRCC_initFS),
	
	EXEC_MAP_ENTRY(COM_CREATE_VOLUME,		"createVolume",       MRCC_createVol),
	EXEC_MAP_ENTRY(COM_VOLUME_INFO,			"volumeInfo",         NULL),
	EXEC_MAP_ENTRY(COM_DELETE_VOLUME,		"deleteVolume",       MRCC_deleteVol),
	EXEC_MAP_ENTRY(COM_GET_VOLUMES,			"getLocalVolumes",    MRCC_getVols),

	EXEC_MAP_ENTRY(COM_ADD_REPLICA,			"addReplica",         MRCC_addReplica),

	EXEC_MAP_ENTRY(COM_ACCESS_MODE,			"changeAccessMode",   MRCC_changeAccessMode),
	EXEC_MAP_ENTRY(COM_CHECK_ACCESS,		"checkAccess",        MRCC_checkAccessMode),
	EXEC_MAP_ENTRY(COM_CHANGE_OWNER,		"changeOwner",        MRCC_changeOwner),
	EXEC_MAP_ENTRY(COM_SET_ACL_ENTRIES,		"setACLEntries",      MRCC_setACLEntries),
	EXEC_MAP_ENTRY(COM_REM_ACL_ENTRIES,		"removeACLEntries",   MRCC_remACLEntries),
	EXEC_MAP_ENTRY(COM_GET_XATTR,			"getXAttr",           MRCC_getXAttr),
	EXEC_MAP_ENTRY(COM_SET_XATTRS,			"setXAttrs",          MRCC_setXAttrs),
	EXEC_MAP_ENTRY(COM_REM_XATTRS,			"removeXAttrs",       MRCC_remXAttrs),
	EXEC_MAP_ENTRY(COM_STAT,				"stat",               MRCC_stat),
	EXEC_MAP_ENTRY(COM_MOVE,				"move",               MRCC_move),

	EXEC_MAP_ENTRY(COM_CREATE_DIR,			"createDir",          MRCC_createDir),
	EXEC_MAP_ENTRY(COM_READ_DIR,			"readDir",            MRCC_readDir),
	EXEC_MAP_ENTRY(COM_READ_DIR_AND_STAT,	"readDirAndStat",     MRCC_readDir), /* has the same input signature */

	EXEC_MAP_ENTRY(COM_CREATE_FILE,			"createFile",         MRCC_createFile),
	EXEC_MAP_ENTRY(COM_DELETE,				"delete",             MRCC_delete),
	EXEC_MAP_ENTRY(COM_OPEN,				"open",               MRCC_open),

	EXEC_MAP_ENTRY(COM_CREATE_SYMLINK,		"createSymbolicLink", MRCC_createSymLink),
	EXEC_MAP_ENTRY(COM_CREATE_LINK,			"createLink",         MRCC_createLink),

	EXEC_MAP_ENTRY(COM_UPD_FILESIZE,		"updateFileSize",     MRCC_updateFileSize),
	EXEC_MAP_ENTRY(COM_RENEW,				"renew",              MRCC_renew),
};

	
struct MRC_Channel_header *
MRC_Channel_header_new(char *key, char *value)
{
	struct MRC_Channel_header *rv = NULL;
	
	dbg_msg("Creating header for %s -> %s\n", key, value);
	
	rv = (struct MRC_Channel_header *)
		malloc(sizeof(struct MRC_Channel_header));
	if (rv) {
		rv->key = strdup(key);
		rv->value = strdup(value);
		INIT_LIST_HEAD(&rv->head);
	}
	return rv;
}

void
MRC_Channel_header_clear(struct MRC_Channel_header *head)
{
	free(head->key);
	free(head->value);
}

struct MRC_Channel *
MRC_Channel_new(char *mrc_url)
{
	struct MRC_Channel *rv = NULL;

	rv = (struct MRC_Channel *)malloc(sizeof(struct MRC_Channel));
	if (rv)
		if (MRC_Channel_init(rv, mrc_url)) {
			MRC_Channel_delete(rv);
			rv = NULL;
		}
	return rv;
}

int
MRC_Channel_init(struct MRC_Channel *mc, char *mrc_url)
{
	int rv = 0;

	memset((void *)mc, 0, sizeof(struct MRC_Channel));

	if (ne_uri_parse(mrc_url, &mc->mrc_uri) || !mc->mrc_uri.host) {
		rv = -1;
		goto finish;
	}
	if (!mc->mrc_uri.scheme)
		mc->mrc_uri.scheme = strdup("http");
	if (!mc->mrc_uri.port)
		mc->mrc_uri.port = MRC_DEFAULT_PORT;
	if (!mc->mrc_uri.path)
		mc->mrc_uri.path = strdup("/");

	mc->channel_session = ne_session_create(mc->mrc_uri.scheme,
						mc->mrc_uri.host,
						mc->mrc_uri.port);
	if (!mc->channel_session) {
		err_msg("Cannot create session with MRC '%s'\n",
			mc->mrc_uri.host);
		rv = -2;
		goto finish;
	}

	/* Check if we have an https session and a certificate. */
	if (!strcmp(mc->mrc_uri.scheme, "https")) {
		if (xtreemfs_client_cert) {
			dbg_msg("Setting client certificate.\n");
			ne_ssl_set_clicert(mc->channel_session,
					   xtreemfs_client_cert);
			// if (xtreemfs_ca_cert)
			//	ne_ssl_trust_cert(mc->channel_session, xtreemfs_ca_cert);
			ne_ssl_trust_default_ca(mc->channel_session);
			ne_ssl_set_verify(mc->channel_session,
					  xtreemfs_verify_cert,
					  mc->mrc_uri.host);
		} else {
			dbg_msg("https selected but no client certificate!\n");
		}
	}
	mc->prot_vers = -1;
	mc->resp_buf = ne_buffer_create();
	INIT_LIST_HEAD(&mc->resp_headers);

	pthread_cond_init(&mc->wait_cond, NULL);
	pthread_mutex_init(&mc->wait_mutex, NULL);

	mc->drift.tv_sec = 0;
	mc->drift.tv_usec = 0;

finish:
	return rv;
}

void
MRC_Channel_clear_headers(struct MRC_Channel *mc)
{
	struct list_head *iter;
	struct MRC_Channel_header *head;

	while (!list_empty(&mc->resp_headers)) {
		iter = mc->resp_headers.next;
		list_del(iter);
		head = (struct MRC_Channel_header *)iter;
		dbg_msg("Clearing header %s -> %s\n", head->key, head->value);
		MRC_Channel_header_clear(head);
		free(head);
	}
}

void
MRC_Channel_delete_contents(struct MRC_Channel *mc)
{
	ne_uri_free(&mc->mrc_uri);
	ne_buffer_destroy(mc->resp_buf);
	ne_session_destroy(mc->channel_session);
}

void
MRC_Channel_delete(struct MRC_Channel *mc)
{
	if (mc) {
		pthread_cond_destroy(&mc->wait_cond);
		pthread_mutex_destroy(&mc->wait_mutex);
		free(mc);
		mc = NULL;
	}
}


int
MRC_Channel_req_resp_block_reader(void *userdata, const char *buf, size_t len)
{
	struct MRC_Channel *mc = (struct MRC_Channel *)userdata;

	dbg_msg("Reading response.\n");

	if (len > 0) {
		ne_buffer_append(mc->resp_buf, buf, len);
	} else {
		dbg_msg("Request has finished\n");
		pthread_cond_broadcast(&mc->wait_cond);
	}
	return NE_OK;
}


int
MRC_Channel_test_java_exception(struct MRC_Channel *mc)
{
	int rv = 0;

	dbg_msg("Checking for java exception in response.\n");

	if (mc->resp_buf->used > 10) {
		if (!strncmp(mc->resp_buf->data, "\"java.lang", 10)) {
			if (strstr(mc->resp_buf->data, "Exception:")) {
				dbg_msg("Found java exception\n");
				rv = 1;
			}
		} else
			dbg_msg("No java language id\n");
	}
	return rv;
}

/**
 * Translate error messages into our error code.
 *
 * @param mc Channel for which to analyze error message
 * @param errMsg The error message itself.
 * @returns Integer error code.
 */
int
MRC_Channel_translate_errors(struct MRC_Channel *mc, char *errMsg)
{
	int rv = -1;
	char *errnum = NULL, *p = NULL;
	char *endpt = NULL;
	
	/* Firstly, we try to identify 'errno' */
	errnum = strstr(errMsg, "errno");
	if (errnum) {
		dbg_msg("Found 'errno' string in error message.\n");
		p = (char *)errnum + strlen("errno");
		while(*p == ' ' || *p == '\t') p++;
		dbg_msg("Rest of the string is '%s'\n", p);
		if (*p == '=') {
			p++;
			/* Next thing should be a number. */
			rv = strtol(p, &endpt, 10);
			dbg_msg("rv is now %d\n", rv);

			if (p == endpt)
				rv = -1;
		}
	}
	
	
	if (rv == -1) {
		dbg_msg("Need to test other method of determining the error reason.\n");
	}
	return rv;
}

/**
 * Test for an MRC exception.
 *
 * This function tests if there is an (handled) MRC exception. 
 * It assumes that if there is an exception it is JSON format,
 * so it is better called after 'MRC_Channel_test_java_exception'
 */
int
MRC_Channel_test_mrc_exception(struct MRC_Channel *mc)
{
	int rv = 0;
	struct json_object *jo;
	enum json_type obj_type;

	if (mc->resp_buf->used == 0)
		goto finish;

	jo = json_tokener_parse(mc->resp_buf->data);
#if 0
	for (i=0; i<mc->resp_buf->length && i<2047; i++) {
		dummy_buf[i] = mc->resp_buf->data[i];
	}
	dummy_buf[i] = '\0';
	dbg_msg("Buffer: %s\n", dummy_buf);
#endif

	if (!jo)
		goto finish;

	if (jo && !is_error(jo)) {
		obj_type = json_object_get_type(jo);

		/* Error messages are contained in an object */
		if (obj_type == json_type_object) {
			json_object_object_foreach(jo, key, val) {
				if (!strcmp(key, "errorMessage")) {
					dbg_msg("errorMessage: %s\n", json_object_to_json_string(val));
					rv = MRC_Channel_translate_errors(mc,
							json_object_to_json_string(val));
					if(rv == -1)
						rv = 2;
				} else if (!strcmp(key, "exceptionName")) {
					if (rv == 0)
						rv = 2;
					dbg_msg("exceptionName: %s\n", json_object_to_json_string(val));
				} else if (!strcmp(key, "stackTrace")) {
					if (rv == 0)
						rv = 2;
					dbg_msg("%s\n", json_object_to_json_string(val));
				}
			}
		}
		json_object_put(jo);
	} else {
		dbg_msg("Error state for json object: %ld %d\n", jo, is_error(jo));
		rv = 1;
	}
finish:
	return rv;
}


static int MRC_Channel_get_headers(struct MRC_Channel *mc, ne_request *req)
{
	int err = 0;
	void *ne_cursor = NULL;
	char *header_name = NULL;
	char *header_value = NULL;
	struct MRC_Channel_header *head;

	ne_cursor = NULL;
	INIT_LIST_HEAD(&mc->resp_headers);

	while ((ne_cursor =
		ne_response_header_iterate(req, ne_cursor,
					   (const char **)&header_name,
					   (const char **)&header_value))) {
		if (!header_name || !header_value)
			dbg_msg("Empty return values.\n");

		head = MRC_Channel_header_new(header_name, header_value);
		list_add(&head->head, &mc->resp_headers);

		dbg_msg("Header:   %s -> %s\n", header_name, header_value);
	}

	return err;
}


/**
 * Analyse response from MRC.
 * The analysis here is done on the level of JSON and HTTP. So we
 * look here into errors that are directly related to JSON over HTTP.
 * Analysis on the protocol level (i.e. error and responses from the
 * protocol specification) is done somewhere else.
 * @param mc MRC channel used for the communication
 * @param func Function name
 * @return 0 or error code
 */
int
MRC_Channel_analyse_resp(struct MRC_Channel *mc, char *func)
{
	int rv = 0, err = 0;

	/* Before analyzing the response, the headers must be present! */
#if 0
	ne_cursor = NULL;

	if (list_empty(&mc->resp_headers)) {
		MRC_Channel_get_headers(mc, req);
	}
#endif

	if (MRC_Channel_test_java_exception(mc)) {
		dbg_msg("Unhandled JAVA exception... should NOT happen!\n");
		rv = 1;
		goto finish;
	}
	if ((err = MRC_Channel_test_mrc_exception(mc))) {
		rv = err;
		dbg_msg("Error code from channel exception: %d\n", err);
		goto finish;
	}
	/* So at this point we can be sure to have no error from MRC! */
finish:
	return rv;
}

/**
 * Check for exception on MRC side.
 */
static int MRC_Channel_exception(struct MRC_Channel *mc, ne_request *req)
{
	int err = 0;

#if 0
	while ((ne_cursor =
		ne_response_header_iterate(req, ne_cursor,
					   (const char **)&header_name,
					   (const char **)&header_value))) {
		head = MRC_Channel_header_new(header_name, header_value);
		list_add(&head->head, &mc->resp_headers);

		dbg_msg("Header:   %s -> %s\n", header_name, header_value);
	}
#endif

	if (MRC_Channel_test_java_exception(mc)) {
		dbg_msg("Unhandled JAVA exception... should NOT happen!\n");
		err = 1;
		goto finish;
	}

	if ((err = MRC_Channel_test_mrc_exception(mc))) {
		dbg_msg("Error code from testing channel exception: %d\n", err);
		goto finish;
	}
	/* So at this point we can be sure to have no error from MRC! */
finish:
	return err;
}

/**
 * Analyse the result of request execution on request level.
 * (In contrast to the result on protocol level)
 */
static int
MRC_Channel_analyse_p_response(struct MRC_Channel *mc, struct MRC_payload *p)
{
	int rv = 0;
	struct list_head *iter1;
	struct MRC_Channel_header *header;
	char *header_name, *header_value;
	struct json_object *jo;
	char *xlocs = NULL, *xcaps = NULL;
	int i;

	/* Check for general errors... tbd */

	/* Extract xcapabilities and xlocations, if available.
	   The strings will be allocated whether used or not, and freed
	   at the end of the routine. Set pointers to NULL if used anywhere!
	*/

	list_for_each(iter1, &mc->resp_headers) {
		header = (struct MRC_Channel_header *)iter1;
		header_name = header->key;
		header_value = header->value;
		if(!strcasecmp(header_name, "x-locations")) {
			dbg_msg("- xloc found: %s\n", header_value);
			xlocs = strdup(header_value);
		} else if (!strcasecmp(header_name, "x-capability")) {
			dbg_msg("- xcap found: %s\n",header_value);
			xcaps = strdup(header_value);
		} else {
			dbg_msg("header %s not parsed in response\n",header_name);
			dbg_msg("header value: %s\n", header_value);
		}
	}

	/* Request specific answers */
	switch(p->command) {
	case COM_CREATE_FILE: {
		struct MRC_Req_create_file_resp *resp = p->resp.createfile;

		/* Create file does not always return xcaps or xlocs.
		   Only when the file is opened.
		 */
		if (xlocs && xcaps) {
			dbg_msg("xlocs=%s xcaps=%s\n", xlocs, xcaps);
			resp->xloc = xlocs;
			resp->xcap  = xcaps;
			xlocs = NULL;
			xcaps = NULL;
		} else {
			resp->xloc = NULL;
			resp->xcap = NULL;
		}
	}	
		break;

	case COM_OPEN: {
		struct MRC_Req_open_resp *resp = p->resp.open;

		dbg_msg("xlocs=%s xcaps=%s\n",xlocs, xcaps);
		/* Open always requires returning X-Locations and
		   X-Capabilities. So if these are not present, we
		   have an error condition. We assume this happens
		   when the open operation is not allowed. */
		if (!xlocs || !xcaps) {
			dbg_msg("No x-location or x-capability returned.\n");
			rv = -EACCES;
			break;
		}
		
		resp->xloc = xlocs;
		resp->xcap = xcaps;
		xlocs = NULL;
		xcaps = NULL;
	}
		break;

	case COM_GET_VOLUMES: {
		struct MRC_Req_get_volumes_resp *resp =
			p->resp.get_volumes;
		int i, num_vols = 0;

		if (!resp)
			break;
		jo = json_tokener_parse(mc->resp_buf->data);
		if (!jo)
			break;
		if (is_error(jo) ||
		    json_object_get_type(jo) != json_type_object) {
			err_msg("Cannot parse response.\n");
			json_object_put(jo);
			break;
		}
		dbg_msg("Volumes: %s\n", json_object_get_string(jo));
		{
			// need this block
			json_object_object_foreach(jo, key, val) {
				num_vols++;
			}
		}
		resp->num_vols = num_vols;
		resp->ids = (char **)malloc(sizeof(char *)
					    * resp->num_vols);
		memset((void *)resp->ids, 0, sizeof(char *) * resp->num_vols);
		resp->names = (char **)malloc(sizeof(char *)
					      * resp->num_vols);
		memset((void *)resp->names, 0, sizeof(char *)
		       * resp->num_vols);
		i = 0;
		json_object_object_foreach(jo, key, val) {
			resp->ids[i] = strdup(key);
			resp->names[i] = strdup(json_object_get_string(val));
			i++;
		}
		json_object_put(jo);
	}
		break;

	case COM_DELETE: {
		struct MRC_Req_delete_resp *resp = p->resp.delete;

		if (!xlocs) // when removing a directory, this is okay
			break;
		resp->xlocs = str_to_xlocs_list(xlocs);
		resp->xcap = str_to_xcap(xcaps);
	}
		break;

	case COM_READ_DIR: {
		struct MRC_Req_readDir_resp *resp = p->resp.readdir;
		/* Body of reply should contain a JSON encoded list 
		   of directory entries. Request reply data contain
		   an empty list_head */
		jo = json_tokener_parse(mc->resp_buf->data);
		if (!is_error(jo) &&
		    json_object_get_type(jo) == json_type_array) {
			int len = json_object_array_length(jo);
			struct json_object *dir_str;
			
			resp->num_entries = len;
				resp->dir_entries = (char **)malloc(sizeof(char *) * len);
				for (i = 0; i < len; i++) {
					dir_str = json_object_array_get_idx(jo, i);
					resp->dir_entries[i] = strdup(json_object_get_string(dir_str));
				}
		} else {
			resp->dir_entries = NULL;
			dbg_msg("Len is zero bytes\n");
		}
		json_object_put(jo);
	}
		break;

	case COM_READ_DIR_AND_STAT: {
		struct MRC_Req_readDirAndStat_resp *resp = p->resp.readdirandstat;
		/* Body of reply should contain a JSON map/object 
		   of directory entries to stat info */
		jo = json_tokener_parse(mc->resp_buf->data);
		if (!is_error(jo) &&
		    json_object_is_type(jo, json_type_object)) {
			int len = 0, i = 0;
			{	/* for scoping the in-macro variable definitions*/
				json_object_object_foreach(jo,key,val)
					len++;
			}

			resp->num_entries = len;
			if (len) {
				resp->dir_entries = (char **)malloc(sizeof(char *) * len);
				resp->stat_entries = (struct xtreemfs_statinfo*)malloc(
					sizeof(struct xtreemfs_statinfo) * len);

				json_object_object_foreach(jo,key,val) {
					resp->dir_entries[i] = strdup(key);
					xtreemfs_statinfo_init(&(resp->stat_entries[i]));
					json_to_xtreemfs_statinfo_ip(val, &(resp->stat_entries[i]));
					i++;
				}
			} else {
				resp->dir_entries = NULL;
				resp->stat_entries = NULL;
				dbg_msg("Len is zero bytes\n");
			}
		}
		json_object_put(jo);
	}
		break;

	case COM_RENEW: {
		struct MRC_Req_renew_data *data = &p->data.renew;
		struct xcap *xcap = data->xcap;
		if (xcap && xcaps) {
			dbg_msg("Updating x-capability\n");
			dbg_msg("Old truncate epoch: %d\n", xcap->truncateEpoch);
			xcap_clear(xcap);
			xcap_values_from_str(xcap, xcaps);
			dbg_msg("New truncate epoch: %d\n", xcap->truncateEpoch);
		}
	}
		break;

	case COM_STAT:
		dbg_msg("response: %s\n",mc->resp_buf->data);
		jo = json_tokener_parse(mc->resp_buf->data);
		if (jo) {
			json_to_xtreemfs_statinfo_ip(jo, p->resp.statinfo);
			json_object_put(jo);
		} else
			rv = -ENOENT;
		break;

	case COM_GET_XATTR: {
		struct MRC_Req_get_xattr_data *data = &p->data.get_xattr;
		struct MRC_Req_get_xattr_resp *resp = p->resp.get_xattr;

		jo = json_tokener_parse(mc->resp_buf->data);
		if (jo &&
		    json_object_get_type(jo) == json_type_string) {
			resp->value = strdup(json_object_get_string(jo));
			json_object_put(jo);
		}
	}
		break;

	case COM_CHECK_ACCESS: {
		struct MRC_Req_check_access_resp *resp = p->resp.chk_access;

		jo = json_tokener_parse(mc->resp_buf->data);
		if (jo &&
		    json_object_get_type(jo) == json_type_boolean) {
			resp->grant = json_object_get_boolean(jo);
			json_object_put(jo);
		} else
			dbg_msg("Wrong return value.\n");
	}
		break;

	default:
		break;
	}
	if (xlocs)
		free(xlocs);
	if (xcaps)
		free(xcaps);
	return rv;
}


static ne_request *MRCC_prepare(struct MRC_Channel *mc, struct MRC_payload *p)
{
	int err = 0, errno;
#if 0
	struct passwd      passwd;
	struct passwd      *pwd_res;
	struct group       group;
	char pwd_str[1024];
	struct group       *grp_res;
	char group_str[1024];
#endif
	char auth_str[1024];
	ne_request *ne_req = NULL;

	creds_dbg(p->creds);

	ne_req = ne_request_create(mc->channel_session, "POST",
				   MRCC_exec_map[p->command].fname);
	if (!ne_req) {
		err_msg("Cannot create request for cmd=%d\n", p->command);
		err = 1;
		goto error;
	}

	memset((void *)auth_str, 0, 1024);
	errno = 0;

	if (creds_to_str_ip(p->creds, auth_str, 1024))
		goto error;

	dbg_msg("Authorization string: %s\n", auth_str);

	ne_add_request_header(ne_req, "Authorization", auth_str);

	goto done;

 error:
	if (ne_req) {
		ne_request_destroy(ne_req);
		ne_req = NULL;
	}
 done:
	return ne_req;
}

static int MRCC_http_accept(void *userdata, ne_request *req, const ne_status *st)
{
	int acc = 0;
	
	if (st->klass == 2) {
		acc = 1;
		dbg_msg("Accept 200 class\n");
		goto finish;
	}

	if (st->klass == 4) {
		if (st->code == 420) {
			acc = 1;
			dbg_msg("Accept 420 code.\n");
			goto finish;
		}
	}

	if (st->klass == 5 && st->code == 500) {
		acc = 1;
		goto finish;
	}

	dbg_msg("HTTP code: %d\n", st->code);
	if (st->reason_phrase)
		dbg_msg("HTTP reason: %s\n", st->reason_phrase);

finish:
	return acc;
}
	
static int MRCC_run(struct MRC_Channel *mc, ne_request *ne_req,
		    struct json_object *req_params)
{
	char *req_params_str = NULL;
	int err = 0;


	/* If we do not have parameters create an empty json object.
	   This indicates that we do not have parameters...         */
	if (!req_params)
		req_params = json_object_new_array();

	/* Set parameters in body, JSON encoded. */
	req_params_str = json_object_to_json_string(req_params);
	dbg_msg("JSON parameter string: %s\n", req_params_str);

	/* If we have parameters add a body to the request */
	if (req_params && req_params_str)
		ne_set_request_body_buffer(ne_req, req_params_str,
					   strlen(req_params_str));

	/* How to receive the answer */
	ne_add_response_body_reader(ne_req, /* ne_accept_always */ MRCC_http_accept,
				    MRC_Channel_req_resp_block_reader,
				    (void *)mc);

	/* Clear response buffer */
	ne_buffer_clear(mc->resp_buf);

	/* Submit request */
	dbg_msg("Dispatching request.\n");
	if ((err = ne_request_dispatch(ne_req)) != NE_OK) {
		err_msg("Error while dispatching request %p (%d)\n",
			ne_req, err);
		err_msg("Error was: %s\n", ne_get_error(mc->channel_session));

		goto finish;
	}
	dbg_msg("Request dispatched successfully.\n");
	
	/* Store headers in a linked list for later use in the
	   different analysis phases.                           */
	MRC_Channel_get_headers(mc, ne_req);

	err = MRC_Channel_exception(mc, ne_req);

	/* We now have prepared the MRC Channel in such a way, that
	   all further processing should be independent of NEON.    */

 finish:
	if (ne_req)
		ne_request_destroy(ne_req);

	/* if (req_params_str)
		free(req_params_str); */
	if (req_params)
		json_object_put(req_params);

	return err;
}

static int MRCC_open(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	struct json_object *req_params = NULL;
	int err = 0;
	char *path = p->data.open.path;
	char *access_mode = p->data.open.access_mode;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}
	req_params = json_object_new_array();

	json_object_array_add(req_params, json_object_new_string(path));
	json_object_array_add(req_params, json_object_new_string(access_mode));

	err = MRCC_run(mc, ne_req, req_params);
	if (err)
		err = -EIO;
	else
		err = MRC_Channel_analyse_resp(mc, MRCC_exec_map[p->command].fname);
 out:
	return err;
}

static int MRCC_updateFileSize(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	struct MRC_Req_upd_fs_data *nd =
		(struct MRC_Req_upd_fs_data *)&p->data;
	struct xcap *xc;
	off_t newsize;
	int epoch;
	char newsize_str[70];
	int err = 0;

	xc      = nd->xcap;
	newsize = nd->new_size;
	epoch   = nd->epoch;

	dbg_msg("Updating file size: % in epoch %d\n",
		newsize, epoch);

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}
	ne_add_request_header(ne_req, "X-Capability", xc->repr);	
	
	if (newsize >= 0) {
		sprintf(newsize_str, "[%lld,%d]", newsize, epoch);
		ne_add_request_header(ne_req, "X-New-File-Size", newsize_str);
		dbg_msg("New size is %lld\n", newsize);
	} else {
		dbg_msg("No new size specified!\n");
	}

	err = MRCC_run(mc, ne_req, NULL);
	if (err)
		err = -EIO;
	else
		err = MRC_Channel_analyse_resp(mc, MRCC_exec_map[p->command].fname);
out:
	return err;
}

static int MRCC_createSymLink(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	struct json_object *req_params = NULL;
	int err = 0;
	char *from = p->data.create_symlink.from;
	char *to = p->data.create_symlink.to;

	dbg_msg("Creating symbolic link from '%s' to '%s'\n", from, to);

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}
	req_params = json_object_new_array();

	json_object_array_add(req_params, json_object_new_string(from));
	json_object_array_add(req_params, json_object_new_string(to));

	err = MRCC_run(mc, ne_req, req_params);
 out:
	return err;
}

static int MRCC_createLink(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	struct json_object *req_params = NULL;
	int err = 0;
	char *from = p->data.create_link.from;
	char *to = p->data.create_link.to;

	dbg_msg("Creating symbolic link from '%s' to '%s'\n", from, to);

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}
	req_params = json_object_new_array();

	json_object_array_add(req_params, json_object_new_string(from));
	json_object_array_add(req_params, json_object_new_string(to));

	err = MRCC_run(mc, ne_req, req_params);
 out:
	return err;
}

static int MRCC_createFile(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	struct json_object *req_params = NULL;
	int err = 0;
	char *path = p->data.create_file.path;
	void *xAttrs = p->data.create_file.xAttrs;
	struct striping_policy *sp = p->data.create_file.sp;
	void *acl = p->data.create_file.acl;
	int mode = p->data.create_file.mode;
	int do_open = p->data.create_file.open;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}
	req_params = json_object_new_array();

	json_object_array_add(req_params, json_object_new_string(path));
	if (!xAttrs)
		json_object_array_add(req_params, json_object_new_object());
	if (sp)
		json_object_array_add(req_params, striping_policy_to_json(sp));
	else
		json_object_array_add(req_params, json_object_new_object());
	/* WARNING: COMMENTED OUT THIS STATEMENT IN ORDER TO BE CONSISTENT W/ MRC PROTOCOL V1 (Jan)
	if (!acl)
		json_object_array_add(req_params, json_object_new_object());
	*/
	if (acl) {
		mode = acl_list_to_mode(acl);
	} else {
		json_object_array_add(req_params, json_object_new_int(mode));
	}

	json_object_array_add(req_params, json_object_new_boolean(do_open));

	err = MRCC_run(mc, ne_req, req_params);
 out:
	return err;
}

static int MRCC_createDir(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	struct json_object *req_params = NULL;
	int err = 0;
	char *path = p->data.create_dir.dirPath;
	void *xattrs = p->data.create_dir.xAttrs;
	/* void *acl = p->data.create_dir.acl; */
	int umask = p->data.create_dir.umask;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}
	req_params = json_object_new_array();

	json_object_array_add(req_params, json_object_new_string(path));
	if (!xattrs) {
		json_object_array_add(req_params, json_object_new_object());
	} // how should this look like with xattrs?
	/* WARNING: COMMENTED OUT THIS STATEMENT IN ORDER TO BE CONSISTENT W/ MRC PROTOCOL V1 (Jan)
	if (!acl) {
		json_object_array_add(req_params, json_object_new_object());
	} // how does this look like with acl
	*/
	json_object_array_add(req_params, json_object_new_int(umask));

	err = MRCC_run(mc, ne_req, req_params);
 out:
	return err;
}

static int MRCC_readDir(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	struct json_object *req_params = NULL;
	int err = 0;
	char *path = (char *)p->data.data;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}
	req_params = json_object_new_array();

	json_object_array_add(req_params, json_object_new_string(path));

	err = MRCC_run(mc, ne_req, req_params);
 out:

	return err;
}

static int MRCC_stat(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	struct json_object *req_params = NULL;
	int err = 0;
	char *path 	 = p->data.stat.path;
	int inclReplicas = p->data.stat.inclReplicas;
	int inclXAttrs   = p->data.stat.inclXAttrs;
	int inclACLs     = p->data.stat.inclACLs;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}
	req_params = json_object_new_array();

	json_object_array_add(req_params, json_object_new_string(path));
	json_object_array_add(req_params, json_object_new_boolean(inclReplicas));
	json_object_array_add(req_params, json_object_new_boolean(inclXAttrs));
	json_object_array_add(req_params, json_object_new_boolean(inclACLs));

	err = MRCC_run(mc, ne_req, req_params);

	if (err)
		err = -ENOENT;
	else
		err = MRC_Channel_analyse_resp(mc, MRCC_exec_map[p->command].fname);
 out:
	return err;
}

static int MRCC_renew(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	struct json_object *req_params = NULL;
	struct MRC_Req_renew_data *rd = (struct MRC_Req_renew_data *)&p->data;
	struct xcap *xc;
	off_t newsize;
	int epoch;
	char newsize_str[70];
	int err = 0;

	xc = rd->xcap;
	newsize = rd->new_size;
	epoch   = rd->epoch;
	
	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}
	/* req_params = json_object_new_array(); */

	ne_add_request_header(ne_req, "X-Capability", xc->repr);
	
	if (newsize >= 0) {
		sprintf(newsize_str, "[%lld,%d]", newsize, epoch);
		ne_add_request_header(ne_req, "X-New-File-Size", newsize_str);
		dbg_msg("New size is %ld\n", newsize);
	} else {
		dbg_msg("No new size specified!\n");
	}
	
	err = MRCC_run(mc, ne_req, req_params);
	if (err)
		err = -EIO;
	else
		err = MRC_Channel_analyse_resp(mc, MRCC_exec_map[p->command].fname);

out:
	return err;
}

static int MRCC_move(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	struct json_object *req_params = NULL;
	struct MRC_Req_move_data *data = &p->data.move;
	int err = 0;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	req_params = json_object_new_array();
	json_object_array_add(req_params,
			      json_object_new_string(data->from));
	json_object_array_add(req_params,
			      json_object_new_string(data->to));
		
	err = MRCC_run(mc, ne_req, req_params);
 out:
	return err;
}

static int MRCC_delete(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	struct json_object *req_params = NULL;
	char *path = (char *)p->data.data;
	int err = 0;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	req_params = json_object_new_array();
	json_object_array_add(req_params, json_object_new_string(path));

	err = MRCC_run(mc, ne_req, req_params);
 out:
	return err;
}

static int MRCC_createVol(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	struct json_object *req_params = NULL;
	struct MRC_Req_create_volume_data *d = &p->data.create_vol;
	int err = 0;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	req_params = json_object_new_array();

	json_object_array_add(req_params,
			      json_object_new_string(d->volumeName));
	json_object_array_add(req_params,
			      json_object_new_int(d->osdSelectionPolicyId));
	if(d->defaultStripingPolicy) {
		struct striping_policy *sp = d->defaultStripingPolicy;
		json_object_array_add(req_params, striping_policy_to_json(sp));
	} else {
		json_object_array_add(req_params, json_object_new_object());
	}
	json_object_array_add(req_params, json_object_new_int(d->acPolicyId));
	json_object_array_add(req_params,
			      json_object_new_int(d->partitioningPolicyId));
	if(!d->acl) {
		json_object_array_add(req_params, json_object_new_object());
	} else {
		json_object_array_add(req_params, acl_list_to_json(d->acl));
	}

	err = MRCC_run(mc, ne_req, req_params);
 out:
	return err;
}

static int MRCC_deleteVol(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	struct json_object *req_params = NULL;
	char *volName = (char *)p->data.data;
	int err = 0;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	req_params = json_object_new_array();
	json_object_array_add(req_params, json_object_new_string(volName));

	err = MRCC_run(mc, ne_req, req_params);
 out:
	return err;
}

static int MRCC_getVols(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	int err = 0;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}
	err = MRCC_run(mc, ne_req, NULL);
 out:
	return err;
}

static int MRCC_addReplica(struct MRC_Channel *mc, struct MRC_payload *p)
{
	int err = 0;
	ne_request *ne_req;
	struct json_object *req_params;
	struct json_object *osd_array;
	char *fileid = p->data.add_replica.fileid;
	struct striping_policy *sp = p->data.add_replica.sp;
	int num_osds = p->data.add_replica.num_osds;
	char **osds = p->data.add_replica.osds;
	int i;
	
	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}
	
	req_params = json_object_new_array();
	json_object_array_add(req_params, json_object_new_string(fileid));
	json_object_array_add(req_params, striping_policy_to_json(sp));
	
	osd_array = json_object_new_array();
	for(i=0; i<num_osds; i++) {
		json_object_array_add(osd_array, json_object_new_string(osds[i]));
	}
	json_object_array_add(req_params, osd_array);
	
	err = MRCC_run(mc, ne_req, req_params);
	
out:
	return err;
}

static int MRCC_changeAccessMode(struct MRC_Channel *mc, struct MRC_payload *p)
{
	int err = 0;
	ne_request *ne_req;
	struct json_object *req_params;
	char *path = p->data.chg_accmode.path;
	int mode   = p->data.chg_accmode.mode;
	
	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}
	
	req_params = json_object_new_array();
	json_object_array_add(req_params, json_object_new_string(path));
	json_object_array_add(req_params, json_object_new_int(mode));
	
	err = MRCC_run(mc, ne_req, req_params);
	
out:
	return err;
}

static int MRCC_checkAccessMode(struct MRC_Channel *mc, struct MRC_payload *p)
{
	int err = 0;
	ne_request *ne_req;
	struct json_object *req_params;
	char *path = p->data.chk_access.path;
	char *mode = p->data.chk_access.mode;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	req_params = json_object_new_array();
	json_object_array_add(req_params, json_object_new_string(path));
	json_object_array_add(req_params, json_object_new_string(mode));

	err = MRCC_run(mc, ne_req, req_params);
out:
	return err;
}

static int MRCC_changeOwner(struct MRC_Channel *mc, struct MRC_payload *p)
{
	int err = 0;
	ne_request *ne_req;
	struct json_object *req_params;
	char *path = p->data.chg_owner.path;
	char *userId = p->data.chg_owner.userId;
	char *groupId = p->data.chg_owner.groupId;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	req_params = json_object_new_array();
	json_object_array_add(req_params, json_object_new_string(path));
	if (userId)
		json_object_array_add(req_params, json_object_new_string(userId));
	else
		json_object_array_add(req_params, NULL);

	if (groupId)
		json_object_array_add(req_params, json_object_new_string(groupId));
	else
		json_object_array_add(req_params, NULL);

	err = MRCC_run(mc, ne_req, req_params);
out:
	return err;
}

static int MRCC_setACLEntries(struct MRC_Channel *mc, struct MRC_payload *p)
{
	int err = 0;
	
	return err;
}

static int MRCC_remACLEntries(struct MRC_Channel *mc, struct MRC_payload *p)
{
	int err = 0;
	
	return err;
}

static int MRCC_getXAttr(struct MRC_Channel *mc, struct MRC_payload *p)
{
	int err = 0;
	ne_request *ne_req;
	struct json_object *req_params;
	char *path = p->data.get_xattr.path;
	char *key  = p->data.get_xattr.key;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	dbg_msg("Getting value for key '%s' and path '%s'\n", key, path);
	req_params = json_object_new_array();
	json_object_array_add(req_params, json_object_new_string(path));
	json_object_array_add(req_params, json_object_new_string(key));

	err = MRCC_run(mc, ne_req, req_params);
out:
	return err;
}

static int MRCC_setXAttrs(struct MRC_Channel *mc, struct MRC_payload *p)
{
	int err = 0;
	ne_request *ne_req;
	struct json_object *req_params;
	char *path = p->data.set_xattrs.path;
	struct xattr_list *xl = p->data.set_xattrs.attrs;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	req_params = json_object_new_array();
	json_object_array_add(req_params, json_object_new_string(path));
	json_object_array_add(req_params, xattr_list_to_json(xl));

	err = MRCC_run(mc, ne_req, req_params);
out:
	return err;
}

static int MRCC_remXAttrs(struct MRC_Channel *mc, struct MRC_payload *p)
{
	int err = 0;
	ne_request *ne_req;
	struct json_object *req_params;
	struct json_object *key_array;

	char *path = p->data.rem_xattrs.path;
	char *key = p->data.rem_xattrs.key;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}

	req_params = json_object_new_array();
	json_object_array_add(req_params, json_object_new_string(path));
	key_array = json_object_new_array();
	json_object_array_add(key_array, json_object_new_string(key));
	json_object_array_add(req_params, key_array);

	err = MRCC_run(mc, ne_req, req_params);
out:
	return err;
}

static int MRCC_initFS(struct MRC_Channel *mc, struct MRC_payload *p)
{
	ne_request *ne_req;
	int err = 0;

	ne_req = MRCC_prepare(mc, p);
	if (!ne_req) {
		err = -ENOMEM;
		goto out;
	}
	err = MRCC_run(mc, ne_req, NULL);
 out:
	return err;
}

/* EF: is this blocking ? */
int MRC_Channel_exec_p(struct MRC_Channel *mc, struct MRC_payload *p,
		       struct req *req)
{
	int err = 0;
	int (*func)(struct MRC_Channel *mc, struct MRC_payload *p);

	dbg_msg("\n******\n** EXECUTING %s req:%p fid:%s\n******\n",
		MRCC_exec_map[p->command].fname, req,
		p->uf ? p->uf->file->fileid_s->fileid : NULL);
	
	func = MRCC_exec_map[p->command].func;
	if (func) {
		err = func(mc, p);
	} else {
		dbg_msg("Function not yet implemented or unknown command.\n");
	}

	if (!err)
		err = MRC_Channel_analyse_p_response(mc, p);
	if (err) {
		req_state_set(req, REQ_STATE_ERROR);
		req->error = err;
		dbg_msg("Error code from exec %d\n", err);
	}

	/* Reset channel for next request */
	dbg_msg("Clearing headers for MRC channel.\n");
	MRC_Channel_clear_headers(mc);
	ne_buffer_clear(mc->resp_buf);

	return err;
}
