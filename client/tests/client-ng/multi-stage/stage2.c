#include <stdlib.h>
#include <stdio.h>

#include "stage2.h"
#include "stage1.h"

wq_func_t stage2_funcs[] = {
	FUNC_TABLE_ENTRY(STAGE2_STEP1, stage2_step1),
	NULL
};

struct work_queue stage2;


/**
 * Allocate and init new data structure for step1
 */
struct stage2_step1_data *
stage2_step1_data_new(double *values, int num_values, int chunk)
{
	struct stage2_step1_data *rv = NULL;

	rv = (struct stage2_step1_data *)malloc(sizeof(struct stage2_step1_data));
	if (rv) {
		rv->num_values = num_values;
		rv->values     = values;
		rv->chunk      = chunk;
	}

	return rv;
}

/**
 * Destroy the outer data structure for data in step1
 *
 * The values are allocated outside this function so they must
 * not be freed.
 */
static void stage2_step1_data_del(void *data)
{
	free(data);
}

/**
 * Allocate and init response structure
 *
 * @param which Pointer where to store req number
 * @param res Pointer where to store local result
 */
struct stage2_step1_resp *
stage2_step1_resp_new(double *res, int *which)
{
	struct stage2_step1_resp *rv = NULL;

	rv = (struct stage2_step1_resp *)malloc(sizeof(struct stage2_step1_resp));
	if (rv) {
		rv->res   = res;
		rv->which = which;
	}

	return rv;	
}

/**
 * Destroy a response structure for step1
 *
 */
void stage2_step1_resp_del(void *resp)
{
	free(resp);
}


/**
 * Handle a request in step1 of stage2
 *
 * @param req Request for step1 of stage2
 */
void stage2_step1(struct req *req)
{
	struct stage2_step1_data *data = (struct stage2_step1_data *)req->data;
	struct stage2_step1_resp *resp = (struct stage2_step1_resp *)req->result;
	int i;
	double *res = resp->res;
	int *which = resp->which;


	/*** Work part of the request ***/

	dbg_msg("Calculating sum with %d values\n", data->num_values);

	/* Calculate sum of given values */
	*res = 0.0;
	for (i=0; i<data->num_values; i++)
		(*res) += data->values[i];
	dbg_msg("Intermediate result: %g\n", *res);
	*which = data->chunk;


	/* Create an answer request. As there is one answer request
	   per work request and we do not need the work request any
	   longer, we can simply change the type of request instead
	   of creating a new request. */

	/* We may now erase the outer data structure of the initial
	   work request. */
	if (req->del_data)
		req->del_data(req->data);

	/* As an answer we will change the type of the current
	   request and re-queue it into stage1. This requests is
	   finished anyway and this procedures saves an additional
	   request allocation */
	INIT_LIST_HEAD(&req->q);
	req->type = STAGE1_FINISH;
	req->data = req->result;
	req->result = NULL;
	req->del_data = stage2_step1_resp_del;
	req->del_result = NULL;

	dbg_msg("Queueing answer request.\n");
	submit_request(&stage1, req);

	/* If we had not have reused the request it should have
	   been destroyed here... */
}


/**
 * Create a request representing work associated with stage2
 */
struct req *stage2_work_req(double *values, int num_values, int chunk,
			    double *res, int *which,
			    struct req *parent)
{
	struct req *rv = NULL;
	struct stage2_step1_data *data = NULL;
	struct stage2_step1_resp *resp = NULL;

	dbg_msg("Num values: %d    Chunk: %d\n",
		num_values, chunk);

	data = stage2_step1_data_new(values, num_values, chunk);
	resp = stage2_step1_resp_new(res, which);
	
	rv = req_create(STAGE2_STEP1, (void *)data, (void *)resp, parent);

	return rv;
}
