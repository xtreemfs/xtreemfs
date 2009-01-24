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
*  C Implementation: osd_channel
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
#include <errno.h>

#include <json.h>
#include <json_tokener.h>

#include <ne_session.h>
#include <ne_request.h>
#include <ne_string.h>

#ifdef ITAC
#include <xtreemfs_itac.h>
#endif

#include "xtreemfs_security.h"
#include "xtreemfs_utils.h"

#include "osd_channel.h"
#include "osd_request.h"
#include "mrc_request.h"

#include "logger.h"
#include "bench_timer.h"

struct bench_timings neon_timings;
struct bench_timer   neon_timer;

int
OSD_Channel_resp_init(struct OSD_Channel_resp *ocr)
{
	int rv = 0;

	ocr->location = NULL;
	ocr->new_size = -1;
	ocr->length   = -1;
	ocr->req_id   = NULL;
	ocr->err      = 0;

	return rv;
}

void
OSD_Channel_resp_clear(struct OSD_Channel_resp *ocr)
{
	free(ocr->location);
	free(ocr->req_id);
}

int
OSD_Channel_http_accept(void *userdata, ne_request *req, const ne_status *st)
{
	int acc = 0;
	struct OSD_Channel *oc = (struct OSD_Channel *)userdata;

	oc->err = OSC_ERR_NO_ERROR;

	if (st->klass == 2) {
		dbg_msg("Accept 200 class\n");
		switch (st->code) {
			case 200:
			case 201:
			case 202:
			case 203:
			case 204:
			case 205:
			case 206:
				acc = 1;
			break;
			default:
				acc = 0;
			break;
		}
		goto finish;
	}

	if (st->klass == 4) {
		dbg_msg("400 class.\n");
		switch (st->code) {
			case 403:
				dbg_msg("Access denied.\n");
				oc->err = OSC_ERR_FORBIDDEN;
			break;
			case 404:
				dbg_msg("Object does not exits on OSD.\n");
				oc->err = OSC_ERR_NOEXIST;
			break;
			default:
				acc = 0;
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


struct OSD_Channel *
OSD_Channel_new(char *osd_id, struct dirservice *ds)
{
	struct OSD_Channel *rv = NULL;

	rv = (struct OSD_Channel *)malloc(sizeof(struct OSD_Channel));
	if(rv && OSD_Channel_init(rv, osd_id, ds)) {
		OSD_Channel_delete(rv);
		rv = NULL;
	}

	return rv;
}

int
OSD_Channel_init(struct OSD_Channel *oc, char *osd_id, struct dirservice *ds)
{
	int rv = 0;

	memset((void *)oc, 0, sizeof(*oc));
	oc->resp_buf = NULL;
	oc->channel_session = NULL;
	pthread_cond_init(&oc->wait_cond, NULL);
	pthread_mutex_init(&oc->wait_mutex, NULL);
	spin_init(&oc->lock, PTHREAD_PROCESS_PRIVATE);

	if (str_to_uri_ds(osd_id, &oc->osd_uri, 0, ds)) {
		rv = -1;
		goto finish;
	}

	if(!oc->osd_uri.host) {
		rv = -1;
		goto finish;
	}

	if(oc->osd_uri.scheme == NULL)
		oc->osd_uri.scheme = strdup("http");
	if(oc->osd_uri.port   == 0)
		oc->osd_uri.port   = OSD_DEFAULT_PORT;
	if(oc->osd_uri.path   == NULL)
		oc->osd_uri.path   = strdup("/");

	oc->channel_session = ne_session_create(oc->osd_uri.scheme, oc->osd_uri.host, oc->osd_uri.port);
	if(oc->channel_session == NULL) {
		rv = -2;
		goto finish;
	}

	/* Check if we have an https session and a certificate. */
	if (!strcmp(oc->osd_uri.scheme, "https")) {
		if (xtreemfs_client_cert) {
			dbg_msg("Setting client certificate for OSD channel.\n");
			ne_ssl_set_clicert(oc->channel_session,
					   xtreemfs_client_cert);
#if 0
			if (!oc->channel_session->client_cert) {
				err_msg("Cannot set certificate for OSD channel!\n");
				rv = -3;
				goto finish;
			}
#endif
			// if (xtreemfs_ca_cert)
			//	ne_ssl_trust_cert(mc->channel_session, xtreemfs_ca_cert);
			ne_ssl_trust_default_ca(oc->channel_session);
			ne_ssl_set_verify(oc->channel_session,
					  xtreemfs_verify_cert,
					  oc->osd_uri.host);
		} else {
			dbg_msg("https selected but no client certificate!\n");
		}
	}

	oc->err = OSC_ERR_NO_ERROR;

	oc->resp_buf = ne_buffer_create();
	oc->prot_vers = -1;

finish:
	return rv;
}

void
OSD_Channel_close(struct OSD_Channel *oc)
{
	if (oc->channel_session)
		ne_session_destroy(oc->channel_session);
	oc->channel_session = NULL;
}


void
OSD_Channel_delete_contents(struct OSD_Channel *oc)
{
	ne_uri_free(&oc->osd_uri);
	if (oc->resp_buf)
		ne_buffer_destroy(oc->resp_buf);
	if (oc->channel_session)
		ne_session_destroy(oc->channel_session);
}

void
OSD_Channel_delete(struct OSD_Channel *oc)
{
	OSD_Channel_delete_contents(oc);
	pthread_cond_destroy(&oc->wait_cond);
	spin_destroy(&oc->lock);
	pthread_mutex_destroy(&oc->wait_mutex);
	free(oc);
}


int
OSD_Channel_restart(struct OSD_Channel *oc)
{
	int rv = 0;

	if(oc->channel_session != NULL) {
		ne_session_destroy(oc->channel_session);
		oc->channel_session = ne_session_create(oc->osd_uri.scheme,
							oc->osd_uri.host,
							oc->osd_uri.port);
		if (!oc->channel_session)
			rv = 1;
	}

	return rv;
}


static int
OSD_Channel_req_resp_reader(void *userdata, const char *buf, size_t len)
{
	struct OSD_Channel *oc = (struct OSD_Channel *)userdata;
	char *ubuf = (char *)oc->user_buf;

	dbg_msg("Reading OSD response.\n");

	if(len > 0) {
		if (oc->use_buf == 0)
			ne_buffer_append(oc->resp_buf, buf, len);
		else {
			if (oc->user_buf_len + len <= oc->user_buf_size) {
				memcpy(&ubuf[oc->user_buf_len], (void *)buf, len);
				oc->user_buf_len += len;
			} else {
				ne_buffer_append(oc->resp_buf, oc->user_buf, oc->user_buf_len);
				ne_buffer_append(oc->resp_buf, buf, len);
				oc->use_buf = 0;
			}
		}
	} else {
		dbg_msg("Request has finished\n");
		// pthread_cond_broadcast(&oc->wait_cond);
	}

	return NE_OK;
}


static int
OSD_Channel_analyse_req_response(struct OSD_Channel *oc, ne_request *req,
				 struct OSD_Channel_resp *resp)
{
	int rv = 0;
	void *ne_cursor = NULL;
	char *header_name;
	char *header_value;
	struct json_object *renew_obj, *size_obj, *epoch_obj;

	resp->new_size = -1;
	resp->epoch = -1;

	while((ne_cursor = ne_response_header_iterate(req, ne_cursor,
						     (const char **)&header_name,
						     (const char **)&header_value))) {
		if(!strcasecmp(header_name, "x-new-file-size")) {
			dbg_msg("Renew msg size string: '%s'\n", header_value);
			// resp->xnewsize = strdup(header_value);
			renew_obj = json_tokener_parse(header_value);
			if (!renew_obj || is_error(renew_obj)) {
				dbg_msg("Invalid new size string.\n");
				resp->err = OSC_ERR_INV_NEWSIZE;
				goto finish;
			}
			size_obj = json_object_array_get_idx(renew_obj, 0);
			epoch_obj = json_object_array_get_idx(renew_obj, 1);

			if (!size_obj || !epoch_obj) {
				resp->err = OSC_ERR_INV_NEWSIZE;
				goto finish;
			}

			resp->new_size = (off_t)json_object_get_int(size_obj);
			resp->epoch    = json_object_get_int(epoch_obj);
			json_object_put(renew_obj);

			dbg_msg("New size: %lld\n", resp->new_size);
			dbg_msg("Epoch:    %d\n", resp->epoch);
		} else if(!strcasecmp(header_name, "location")) {
			resp->location = strdup(header_value);
		} else if(!strcasecmp(header_name, "content-length")) {
			char *endpt;
			resp->length = strtol(header_value, &endpt, 10);
			if(*endpt != '\0') {
				resp->length = -1;
				resp->err    =  OSC_ERR_INV_LENGTH;
			}
		} else if (!strcasecmp(header_name, "x-request-id")) {
			resp->req_id = strdup(header_value);
			dbg_msg("Request Id: '%s'\n", resp->req_id);
		}
		dbg_msg("OSD response headers: %s -> %s\n", header_name, header_value);
	}

	if (oc->err == OSC_ERR_NOEXIST)
		resp->err = oc->err;

	if (oc->err == OSC_ERR_FORBIDDEN)
		resp->err = oc->err;

finish:
	return rv;
}


static int
OSD_Channel_exec(struct OSD_Channel *oc, int command, char *req_id,
		 struct user_file *uf,
		 int obj_num, loff_t firstByte, loff_t lastByte,
		 void *buf, struct OSD_Channel_resp *resp)
{
	int rv = 0;
	char *fileid = uf->file->fileid_s->fileid;
	struct xloc_list *xll = uf->xloc_list;
	struct xcap *xcap = uf->xcap;
	ne_request *req = NULL;
	ne_status const *status = NULL;
	int ne_err;
	char int_num_str[22];      /* Maximum number 2^64 has 20 decimal places */
	char byte_range_str[128];
	char test_xloc[1024];

#ifdef ITAC
	VT_begin(itac_osd_exec_hdl);
#endif

	dbg_msg("Operation %d for file id '%s'\n", command, fileid);
	dbg_msg("First byte %lld and last byte %lld\n", firstByte, lastByte);


	switch(command) {
	case OSD_GET:
		req = ne_request_create(oc->channel_session, "GET", fileid);
		oc->user_buf = buf;
		oc->user_buf_len = 0;
		oc->user_buf_size = lastByte - firstByte + 1;
		oc->use_buf = 1;
		break;

	case OSD_PUT:
		req = ne_request_create(oc->channel_session, "PUT", fileid);
		if (!req)
			break;
		/* Set the data, if we have any */
		if(buf != NULL && lastByte - firstByte >= 0)
			ne_set_request_body_buffer(req, buf, lastByte - firstByte + 1);
		oc->use_buf = 0;
		break;

	case OSD_DELETE:
		err_msg("Delete is now done in a different place!\n");
		/* req = ne_request_create(oc->channel_session, "DELETE", fileid); */
		/* ne_add_request_header(req, "X-Locations", oc->xlocs); */
		break;

	case OSD_TRUNCATE:
		err_msg("Truncate is not handled by '%s'.\n", __FUNCTION__);
		break;

	default:
		break;
	}

	if(req == NULL) {
		rv = 1;
		goto finish;
	}

	/* Set HTTP request headers */
	/* \todo remove snprintf for performance reasons */

	if(command == OSD_PUT || command == OSD_GET) {
		snprintf(int_num_str, 22, "%d", obj_num);
		ne_add_request_header(req, "X-Object-Number", int_num_str);
		dbg_msg("X-Object-Number: %s\n", int_num_str);

		snprintf(byte_range_str, 128, "bytes %lld-%lld/*", firstByte, lastByte);
		ne_add_request_header(req, "Content-Range", byte_range_str);
		dbg_msg("Content-Range: %s\n", byte_range_str);

#if 0
		snprintf(int_num_str, 22, "%ld", firstByte);
		ne_add_request_header(req, "firstByte", int_num_str);
		dbg_msg("firstByte: %s\n", int_num_str);

		snprintf(int_num_str, 22, "%ld", lastByte);
		ne_add_request_header(req, "lastByte", int_num_str);
		dbg_msg("lastByte: %s\n", int_num_str);
#endif

	}

	strncpy(test_xloc, xll->repr, 1024);

	ne_add_request_header(req, "X-Locations", test_xloc);
	dbg_msg("X-Locations: %s\n", test_xloc);

	ne_add_request_header(req, "X-Capability", xcap->repr);
	dbg_msg("X-Capability: %s\n", xcap->repr);

	if (req_id) {
		ne_add_request_header(req, "X-Request-Id", req_id);
		dbg_msg("X-Request-Id: %s\n", req_id);
	}

	/* How to receive the answer */
	ne_add_response_body_reader(req, OSD_Channel_http_accept,
				    OSD_Channel_req_resp_reader, (void *)oc);

	/* Clear response buffer */
	ne_buffer_clear(oc->resp_buf);

	bench_timer_start(&neon_timer);

	/* Submit request */
	if((ne_err = ne_request_dispatch(req)) != NE_OK) {
		err_msg("Error %d while dispatching the request.\n", ne_err);
		status = ne_get_status(req);
		err_msg("Reason: %s\n", ne_get_error(oc->channel_session));
		rv = OSC_ERR_FAIL;
		resp->err = -ENODEV;
		if (ne_err == NE_TIMEOUT || ne_err == NE_CONNECT)
			rv = OSC_ERR_RETRY;
		err_msg("ne_err=%d err=%d resp->err=%d\n", ne_err, rv, resp->err);
		goto finish;
	}

	bench_timer_stop(&neon_timer);
	bench_timings_add_ms(&neon_timings,
			     (double)(lastByte - firstByte + 1) / bench_timer_ms(&neon_timer));

	if(OSD_Channel_analyse_req_response(oc, req, resp) != 0) {
		if (resp->err == 0)
			rv = 1;
		else
			rv = resp->err;
		goto finish;
	}

	/* Check if we have to copy response data */
	/*!< \todo give the request the location of the data s.t. they are
	   written immediately to the right place, without copying! */

	if(command == OSD_GET) {
		if(buf != NULL && oc->use_buf == 0) {
			size_t copy_size = lastByte - firstByte + 1;
			if (copy_size > resp->length)
				copy_size = resp->length;
			memcpy((void *)buf, (void *)oc->resp_buf->data, copy_size);
		}
	}

finish:
	ne_request_destroy(req);
#ifdef ITAC
	VT_end(itac_osd_exec_hdl);
#endif

	return rv;
}

ne_request *OSD_Channel_neon_req(struct OSD_Channel *oc,
				 OSD_Com_t func, char *fileid,
				 struct xloc_list *xlocs_list,
				 struct xcap *xcap)
{
	ne_request *rv = NULL;
	struct json_object *jo, *json_xlocs;

	switch(func) {
		case OSD_DELETE:
			rv = ne_request_create(oc->channel_session, "DELETE", fileid);
		break;
		case OSD_TRUNCATE:
			rv = ne_request_create(oc->channel_session, "POST", "truncate");
		break;
		default:
		break;
	}

	if (!rv)
		goto finish;

	/* Set HTTP request headers */

	json_xlocs = xlocs_list_to_json(xlocs_list);
	ne_add_request_header(rv, "X-Locations",
			      json_object_to_json_string(json_xlocs));
	dbg_msg("X-Locations: %s\n", json_object_to_json_string(json_xlocs));
	json_object_put(json_xlocs);

	jo = xcap_to_json(xcap);
	ne_add_request_header(rv, "X-Capability", json_object_get_string(jo));
	dbg_msg("X-Capability: %s\n", json_object_get_string(jo));
	json_object_put(jo);

	/* How to receive the answer: do we need this? */
	ne_add_response_body_reader(rv, ne_accept_2xx,
				    OSD_Channel_req_resp_reader, (void *)oc);

	/* Clear response buffer */
	ne_buffer_clear(oc->resp_buf);

finish:
	return rv;
}

int OSD_Channel_submit_req(struct OSD_Channel *oc,
			   ne_request *req, struct OSD_Channel_resp *resp)
{
	int err = 0;
	ne_status const *status;
	int ne_err;

	/* Submit request */
	if((ne_err = ne_request_dispatch(req)) != NE_OK) {
		err_msg("Error %d while dispatching the request.\n", ne_err);
		status = ne_get_status(req);
		err_msg("Reason: %s\n", ne_get_error(oc->channel_session));
		err = OSC_ERR_FAIL;
		resp->err = -ENODEV;
		if (ne_err == NE_TIMEOUT || ne_err == NE_CONNECT)
			err = OSC_ERR_RETRY;
		err_msg("ne_err=%d err=%d resp->err=%d\n", ne_err, err, resp->err);
		goto finish;
	}

	if(OSD_Channel_analyse_req_response(oc, req, resp) != 0) {
		err = 1;
		goto finish;
	}

finish:
	ne_request_destroy(req);
	return err;
}

/**
 * Delete is special, it doesn't come with a user_file struct!
 */
int
OSD_Channel_del(struct OSD_Channel *oc,
		char *fileid, struct xloc_list *xlocs_list,
		struct xcap *xcap, struct OSD_Channel_resp *resp)
{
	int rv = 0;
	ne_request *req;
	/* ne_status *status; */
	/* int ne_err; */

	dbg_msg("Operation DELETE for file id '%s'\n", fileid);

#if 0
	req = ne_request_create(oc->channel_session, "DELETE", fileid);
	if(req == NULL) {
		rv = 1;
		goto finish;
	}

	/* Set HTTP request headers */

	json_xlocs = xlocs_list_to_json(xlocs_list);
	ne_add_request_header(req, "X-Locations",
			      json_object_to_json_string(json_xlocs));
	dbg_msg("X-Locations: %s\n", json_object_to_json_string(json_xlocs));
	json_object_put(json_xlocs);

	jo = xcap_to_json(xcap);
	ne_add_request_header(req, "X-Capability", json_object_get_string(jo));
	dbg_msg("X-Capability: %s\n", json_object_get_string(jo));
	json_object_put(jo);

	/* How to receive the answer: do we need this? */
	ne_add_response_body_reader(req, ne_accept_2xx,
				    OSD_Channel_req_resp_reader, (void *)oc);

	/* Clear response buffer */
	ne_buffer_clear(oc->resp_buf);
#else
	req = OSD_Channel_neon_req(oc, OSD_DELETE, fileid, xlocs_list, xcap);
#endif

#if 0
	/* Submit request */
	if((ne_err = ne_request_dispatch(req)) != NE_OK) {
		err_msg("Error %d while dispatching the request.\n", ne_err);
		status = ne_get_status(req);
		err_msg("Reason: %s\n", ne_get_error(oc->channel_session));
		rv = 2;
		goto finish;
	}

	if(OSD_Channel_analyse_req_response(oc, req, NULL, resp) != 0) {
		rv = 1;
		goto finish;
	}
#endif

	OSD_Channel_submit_req(oc, req, resp);

#if 0
finish:
	ne_request_destroy(req);
#endif
	return rv;
}


int
OSD_Channel_get(struct OSD_Channel *oc, char *req_id, struct user_file *uf,
		int obj_num, loff_t firstByte, loff_t lastByte,
		void *buf, struct OSD_Channel_resp *resp)
{
	int rv = 0;

#ifdef ITAC
	VT_begin(itac_osd_get_hdl);
#endif
	rv = OSD_Channel_exec(oc, OSD_GET, req_id, uf,
			      obj_num, firstByte, lastByte, buf, resp);

#ifdef ITAC
	VT_end(itac_osd_get_hdl);
#endif
	return rv;
}

int
OSD_Channel_put(struct OSD_Channel *oc, char *req_id, struct user_file *uf,
		int obj_num, loff_t firstByte, loff_t lastByte,
		void *buf, struct OSD_Channel_resp *resp)
{
	int rv = 0;
#ifdef ITAC
	VT_begin(itac_osd_put_hdl);
#endif
	dbg_msg("Putting data from %lld to %lld\n", firstByte, lastByte);
	rv = OSD_Channel_exec(oc, OSD_PUT, req_id, uf,
			      obj_num, firstByte, lastByte, buf, resp);

#ifdef ITAC
	VT_end(itac_osd_put_hdl);
#endif
	return rv;
}

int
OSD_Channel_trunc(struct OSD_Channel *oc,
		  char *fileid, off_t new_size,
		  struct xloc_list *xlocs_list, struct xcap *xcap,
		  struct OSD_Channel_resp *resp)
{
	int err = 0;
	ne_request *req;
	struct json_object *jo = NULL;
	char *req_params_str = NULL;

	req = OSD_Channel_neon_req(oc, OSD_TRUNCATE, fileid, xlocs_list, xcap);
	if (!req) {
		err_msg("Cannot create neon request!\n");
		err = -ENOMEM;
		goto finish;
	}

	/* Add new file size to request */
	jo = json_object_new_array();
	json_object_array_add(jo, json_object_new_string(fileid));
	json_object_array_add(jo, json_object_new_int(new_size));

	req_params_str = strdup(json_object_to_json_string(jo));
	json_object_put(jo);
	dbg_msg("JSON parameter string: %s\n", req_params_str);
	ne_set_request_body_buffer(req, req_params_str,
				   strlen(req_params_str));

	err = OSD_Channel_submit_req(oc, req, resp);
	free(req_params_str);
finish:
	return err;
}


/**
 * Send 'heartbeat' to OSD for a specific to indicate we still want
 * to use that file. This prevents the OSD from assuming we have died
 *
 * @param oc OSD channel to use for heartbeat
 * @param uf User file which is still open and needs heartbeat
 *
 * @return 0 on success, error code otherwise
 */
int
OSD_Channel_heartbeat(struct OSD_Channel *oc, struct user_file *uf)
{
	int rv = 0;

	return rv;
}
