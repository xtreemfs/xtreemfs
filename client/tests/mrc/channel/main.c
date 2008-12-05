#include <stdlib.h>
#include <stdio.h>

#include <mrc_channel.h>


void *
finish_callback(void *arg)
{

	return NULL;
}


int
main(int argc, char **argv)
{
	struct MRC_Channel *mrc_channel;
	struct MRC_Request *mrc_req;
	
	mrc_channel = MRC_Channel_new("http://localhost:32636");
	if(mrc_channel == NULL) {
		fprintf(stderr, "Cannot open channel to MRC '%s'\n", "localhost");
		exit(1);
	}
	
	mrc_req = MRC_Request_new();
	if(mrc_req == NULL) {
		MRC_Channel_delete(&mrc_channel);
		fprintf(stderr, "Cannot create new MRC request.\n");
		exit(1);
	}
	mrc_req->finish_cb = finish_callback;
	
	MRC_Request__createVolume(mrc_req, "TEST", 0, NULL, 0, 0, NULL);
	MRC_Channel_exec_req(mrc_channel, mrc_req);
	
	MRC_Request__open(mrc_req, "/TEST/test", "w");
	MRC_Channel_exec_req(mrc_channel, mrc_req);
	
	
	MRC_Request_delete(mrc_req);
	mrc_req = NULL;
	MRC_Channel_delete(&mrc_channel);
	
	return 0;
}
