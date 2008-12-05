#include "work.h"
#include "request.h"
#include "logger.h"


extern struct work_queue stage;

void step1(struct req *req)
{
	int *a = (int *)req->data;

	dbg_msg("Stage1\n");

	/* Increment data by 10 */
	(*a)++;
	
	/* And go to stage 2 */
	req->type = TEST_STEP2;
	submit_request(&stage, req);
}

void step2(struct req *req)
{
	int *a = (int *)req->data;

	dbg_msg("Stage2\n");

	(*a) *=2 ;

	finish_request(req);
}


struct request *create_test_request(int *a, int *b)
{
	return req_create(TEST_STEP1, (void *)a, (void *)b, NULL);
}

int do_work(int a)
{
	int rv = 0;
	struct request *req;

	req = create_test_request(&a, &rv);
	if (!req) {
		dbg_msg("Cannot create new request.\n");
		rv = -1;
		goto out;
	}

	execute_req(&step1, req);

	/* Analyze results */
	info_msg("Work result: %d\n", a);
	rv = a;

out:
	if (req)
		req_del(req);

	return rv;
}
