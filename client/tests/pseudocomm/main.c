#include <sys/types.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include "pseudocomm.h"
#include "file.h"
#include "filerw.h"
#include "request.h"

#define OBJSIZE 256
#define NOBJS 10

int main(int argc, char **argv)
{
	char *buffer[NOBJS];
	enum filerw_op op;
	char url[] = "testurl";
	int o;
	loff_t offset = 0;
	size_t size;
	struct user_file *fd = NULL;

	printf("Starting pseudocomm workqueue & sleeping for 5s\n");
	pseudocomm_init();
	sleep(5);

	printf("First loop\n");
	op = OP_FILERW_WRITE;
	for (o = 0; o < NOBJS; o++) {
		int err;
		struct req *req;

		printf("\titeration %d\n",o);
		buffer[o] = (char *) malloc(OBJSIZE);
		if (!buffer[o]) {
			perror("Could not allocate buffer\n");
			exit(1);
		}
		memset(buffer[o], 0, OBJSIZE);
		sprintf(buffer[o], "Buffer %d data\n", o);
		size = strlen(buffer[o]) + 10;
		req = pseudocomm_req_create(op, url, o, offset,
					    size, buffer[o], fd, 1);
		
		err = (int) req_wait(req, 1);
		if (err)
			fprintf(stderr,"Error %d return from waiting\n", err);
		else
			fprintf(stderr,"wait returned, request %d done\n", o);
	}
	printf("Second loop\n");
	// and now read the objects
	op = OP_FILERW_READ;
	for (o = 0; o < NOBJS; o++) {
		int err;
		struct req *req;

		printf("\titeration %d\n",o);
		memset(buffer[o], 0, OBJSIZE);
		req = pseudocomm_req_create(op, url, o, offset,
					    OBJSIZE, buffer[o], fd, 1);
		
		err = (int)req_wait(req, 1);
		if (err)
			fprintf(stderr,"Error %d return from waiting\n", err);
		else
			fprintf(stderr,"wait returned, request %d done\n", o);
		printf("Object %d\n%s\n-------------\n", o, buffer[o]);
	}
	exit(0);
}
