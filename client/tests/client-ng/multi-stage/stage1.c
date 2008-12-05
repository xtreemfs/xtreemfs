#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "workqueue.h"
#include "stage1.h"
#include "stage2.h"

/**
 * Function table for stage 1
 * This table has only two entries. The end of the table is
 * indicated by a NULL pointer.
 */
wq_func_t stage1_funcs[] = {
	FUNC_TABLE_ENTRY (STAGE1_STEP1,  stage1_step1),
	FUNC_TABLE_ENTRY (STAGE1_FINISH, stage1_finish),
	NULL
};

struct work_queue stage1;


/**
 * Allocate and init data structure for step1 of stage1
 */
static struct stage1_step1_data *
stage1_step1_data_new(double *values, int num_values, int num_reqs)
{
	struct stage1_step1_data *rv = NULL;

	rv = (struct stage1_step1_data *)malloc(sizeof(struct stage1_step1_data));
	if (rv) {
		rv->num_values = num_values;
		rv->values     = values;
		rv->num_reqs   = num_reqs;
	}

	return rv;
}


/**
 * Work to be done for stage 1, step 1
 *
 * This basically creates a number of sub-requests for stage2 that
 * calculate the sum of the values passed in the data section of
 * the request.
 * In order to store the intermediate results an array with result
 * data is allocated.
 */
void stage1_step1(struct req *req)
{
	struct stage1_step1_data *data = (struct stage1_step1_data *)req->data;
	double *result = (double *)req->result;
	int num_reqs = data->num_reqs;
	int chunk_size;
	int off;
	double *sub_res;
	int *sub_which;
	struct req *sub_req = NULL;

	int i;

	dbg_msg("Num values: %d   Num reqs: %d\n",
		data->num_values, data->num_reqs);

	chunk_size = (data->num_values + num_reqs - 1) / num_reqs;

	dbg_msg("Doing stage1 / step1 with %d reqs and chunk size %d\n",
		num_reqs, chunk_size);

	sub_res = (double *)malloc(sizeof(double) * num_reqs);
	memset((void *)sub_res, 0, sizeof(double) * num_reqs);

	sub_which = (int *)malloc(sizeof(int) * num_reqs);
	memset((void *)sub_which, 0, sizeof(int) * num_reqs);

	off = 0;
	for (i=0; i<num_reqs-1; i++) {
		dbg_msg("Create sub request with %d %d\n", chunk_size, off);
		sub_req = stage2_work_req(&data->values[off], chunk_size, i,
					  &sub_res[i], &sub_which[i],
					  req);
		submit_request(&stage2, sub_req);
		off += chunk_size;
	}

	/* The last chunk might be smaller than the full chunk size */
	if (off + chunk_size >= data->num_values)
		chunk_size = data->num_values - off;
	dbg_msg("Create sub request with %d %d\n", chunk_size, off);
	sub_req = stage2_work_req(&data->values[off], chunk_size, i,
				  &sub_res[i], &sub_which[i],
				  req);
	submit_request(&stage2, sub_req);
}

/**
 * Create a request for step1 of stage1
 *
 * A data structure for the request will be allocated in the following
 * format:
 *         values      double array of values
 *         num_values  int indicating the number of values
 *         num_reqs    int Number of sub request to generate
 * The result will be stored in the location indicated by 'res'
 */
struct req *stage1_step1_req(double *values, int num_values, int num_reqs, double *res)
{
	struct req *req = NULL;
	struct stage1_step1_data *data = NULL;

	data = stage1_step1_data_new(values, num_values, num_reqs);

	req = req_create(STAGE1_STEP1, (void *)data, (void *)res, NULL);

	return req;	
}

/**
 * Executed when stage2 has finished its work.
 */
void stage1_finish(struct req *req)
{
	/* We will get the result of a sub calculation as our data */
	struct stage2_step1_resp *data = (struct stage2_step1_resp *)req->data;
	double *pres = NULL;
	double *loc_res = NULL;		/*!< Pointer to beginning of array. */
	int    *loc_chunk = NULL;

	struct req *parent = req->parent;

	/* We need a parent request, otherwise calculations do not make sense */
	if (!parent)
		goto out;

	pres = (double *)parent->result;
	/* dbg_msg("Parent result: %p\n", pres); */

	/* Sum the partial results in the parent */
	*pres += *(data->res);
	dbg_msg("Partial result: %g\n", *pres);

	dbg_msg("Number of children: %d\n", atomic_read(&parent->active_children));

	atomic_dec(&parent->active_children);
	if (atomic_read(&parent->active_children) != 0) {
		dbg_msg("I am not the last child.\n");
		goto out;
	}

	dbg_msg("Going to finish request.\n");

	/* This section is only executed by the last child */
	finish_request(parent);

	if (req->del_data)
		req->del_data(req->data);

out:
	return;	
}


/**
 * Calculate the sum of an array of doubles
 *
 * This function is used to hide the complexities of
 * stages from the user. The user may simply call
 * 'stage1_work' in order to obtain the result.
 */
double stage1_work(double *values, int num_values, int num_reqs)
{
	double rv = 0.0;
	struct req *req = NULL;

	dbg_msg("Result pointer: %p\n", &rv);

	req = stage1_step1_req(values, num_values, num_reqs, &rv);
	
	execute_req(&stage1, req);

	return rv;
}
