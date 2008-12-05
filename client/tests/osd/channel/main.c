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
#include <string.h>

#include <json_tokener.h>

#include "file.h"
#include "mrc_proxy.h"
#include "mrc_request.h"
#include "osd_channel.h"
#include "xloc.h"
#include "xcap.h"
#include "logger.h"

#define BUF_SIZE 4096

pthread_attr_t *xpattr;

int
do_osd_ops(struct user_file *uf, char *filename, char *fileid_str)
{
	struct OSD_Channel *channel;
	struct OSD_Channel_resp channel_resp;
	unsigned char *write_buf, *read_buf;
	struct fileid_struct fileid;
	struct file_replica replica;
	struct list_head xlocs_list, *iter;
	struct xlocs *xlocs;
	
	struct json_object *jo;
	
	int i;
	
	
	fileid.path = filename;
	fileid.fileid = fileid_str;
	
	replica.fileid_s = &fileid;
	
	uf->file = &replica;
	
	jo = json_tokener_parse(uf->xloc);
	json_to_xlocs_list(jo, &xlocs_list);
	json_object_put(jo);
	
	list_for_each(iter, &xlocs_list) {
		xlocs = (struct xlocs *)iter;
		xlocs_print(xlocs);
	}
	xlocs_list_clear(&xlocs_list);
	
	channel = OSD_Channel_new("http://localhost:32637");
	
	if(channel == NULL) {
		err_msg("Cannot create new channel.\n");
	}
	
	write_buf = (char *)malloc(BUF_SIZE);
	read_buf  = (char *)malloc(BUF_SIZE);
		
	jo = json_tokener_parse(uf->xcap);
	json_object_put(jo);
	
	for(i=0; i<BUF_SIZE; i++) write_buf[i] = i;
	memset((void *)read_buf, 0, BUF_SIZE);
	
	OSD_Channel_resp_init(&channel_resp);
	OSD_Channel_put(channel, uf, 1, 0, 4095, write_buf, &channel_resp);
	OSD_Channel_resp_clear(&channel_resp);
	
	OSD_Channel_resp_init(&channel_resp);
	OSD_Channel_get(channel, uf, 1, 0, 4095, read_buf, &channel_resp);
	OSD_Channel_resp_clear(&channel_resp);
	
	
	for(i=0; i<BUF_SIZE; i++) {
		if(read_buf[i] != (unsigned char)i) {
			err_msg("Data written and read are not the same! (at position %d)\n", i);
			break;
		}
	}
	if(i == BUF_SIZE) {
		printf("Bytes written and bytes received are the same!\n");
	}
	
	OSD_Channel_delete(channel);
	
	free(write_buf);
	free(read_buf);

}


int
main(int argc, char **argv)
{
	struct MRC_Proxy mrc_proxy;
	struct MRC_Request mrc_req;
	struct MRC_Req_open_data *req_data;
	struct MRC_Req_open_resp resp_data;
	mrc_req_handle_t req_handle;
	struct user_file uf;
	
	struct json_object *jo;
	
	char *fileid;
	
	
	MRC_Proxy_init(&mrc_proxy, "http://localhost:32636", 2);
	MRC_Request_init(&mrc_req);
	
	MRC_Proxy_start(&mrc_proxy);
	
	MRC_Request__open(&mrc_req, "TEST/testfile", "w");
	resp_data.xcap = NULL;
	resp_data.xlocs = NULL;
	mrc_req.resp_data = (void *)&resp_data;
	req_handle = MRC_Proxy_add_and_wait(&mrc_proxy, &mrc_req);
	
	if(mrc_req.state != MRC_STATE_ERROR) {
		jo = xcap_to_json(resp_data.xcap);
		fileid = strdup(json_object_get_string(json_object_array_get_idx(jo, 0)));
		uf.xcap = strdup(json_object_get_string(jo));
		json_object_put(jo);
		jo = xlocs_list_to_json(resp_data.xlocs);
		uf.xloc = strdup(json_object_get_string(jo));
		json_object_put(jo);
		
		dbg_msg("Xlocation string: %s\n", uf.xloc);
		dbg_msg("Xcap string:      %s\n", uf.xcap);
		
		do_osd_ops(&uf, "TEST/filename", fileid);
		
		free(uf.xcap);
		free(uf.xloc);
		free(fileid);
	}
	else {
		err_msg("Cannot execute osd operations!\n");
	}
	
	req_data = (struct MRC_Req_open_data *)mrc_req.req_data;
	free(req_data->access_mode);
	free(req_data->path);
	free(mrc_req.req_data);
	
	if(resp_data.xcap != NULL) xcap_delete(resp_data.xcap);
	if(resp_data.xlocs != NULL) xlocs_list_delete(resp_data.xlocs);
	
	MRC_Request_clear(&mrc_req);
	MRC_Proxy_stop(&mrc_proxy);
	MRC_Proxy_clear(&mrc_proxy);
	
	return 0;
}
