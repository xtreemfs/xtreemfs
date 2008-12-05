#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

#include <mrc_proxy.h>
#include <stripingpolicy.h>
#include <statinfo.h>
#include <file.h>

pthread_attr_t *xpattr;

int
main(int argc, char **argv)
{
	struct MRC_Proxy *mrc_proxy;
	struct MRC_Request *mrc_req;
	struct MRC_Req_readDir_resp readdir_resp;
	
	struct striping_policy *defPolicy;
	struct xtreemfs_statinfo si;
	
	mrc_req_handle_t req_handle;
	
	ne_sock_init();
	
	mrc_proxy = MRC_Proxy_new("http://localhost:32636", 2);
	if(mrc_proxy == NULL) {
		fprintf(stderr, "Cannot create proxy for 'localhost'\n");
		exit(1);
	}
	MRC_Proxy_start(mrc_proxy);
	sleep(2);
	
	defPolicy = striping_policy_new();
	defPolicy->id          = SPOL_RAID_0;
	defPolicy->stripe_size = 4096;
	defPolicy->width       = 1;
	
	mrc_req = MRC_Request_new();
	MRC_Request__initFileSystem(mrc_req);
	req_handle = MRC_Proxy_add_and_wait(mrc_proxy, mrc_req);
	MRC_Request_clear(mrc_req);
	 
	MRC_Request_init(mrc_req);
	MRC_Request__createVolume(mrc_req, "TEST", 1, defPolicy, 1, 1, NULL);
	req_handle = MRC_Proxy_add_and_wait(mrc_proxy, mrc_req);
	MRC_Request_clear(mrc_req);
	
	MRC_Request_init(mrc_req);
	MRC_Request__createFile(mrc_req, "TEST/testfile", NULL, NULL, NULL, 0755);
	req_handle = MRC_Proxy_add_and_wait(mrc_proxy, mrc_req);
	MRC_Request_clear(mrc_req);

	printf("Testing file...\n");
	
	MRC_Request_init(mrc_req);
	MRC_Request__stat(mrc_req, "TEST/testfile", 0, 0, 0);
	mrc_req->resp_data = (void *)&si;
	req_handle = MRC_Proxy_add_and_wait(mrc_proxy, mrc_req);
	MRC_Request_clear(mrc_req);
	
	xtreemfs_statinfo_print(&si);
	
	MRC_Request_init(mrc_req);
	MRC_Request__readDir(mrc_req, "TEST/");
	mrc_req->resp_data = (void *)&readdir_resp;
	req_handle = MRC_Proxy_add_and_wait(mrc_proxy, mrc_req);
	MRC_Request_clear(mrc_req);
	
	
	printf("Opening the file....\n");
	
	MRC_Request_init(mrc_req);
	MRC_Request__open(mrc_req, "TEST/testfile", "w");
	req_handle = MRC_Proxy_add_and_wait(mrc_proxy, mrc_req);
	MRC_Request_clear(mrc_req);
	
	printf("Wait for request finished.\n");
	
	
	/* Cleaning up all used resources */
		
	MRC_Proxy_stop(mrc_proxy);
	printf("MRC proxy stopped now.\n");
	
	MRC_Request_delete(mrc_req);

	MRC_Proxy_delete(mrc_proxy);
	
	ne_sock_exit();
	
	return 0;
}
