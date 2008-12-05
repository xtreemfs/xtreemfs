/**
 * Test program for multiple stages.
 *
 * This program calculates the sum of an array of double values using
 * two stages. The second stage has several threads s.t. the calculation
 * can proceed concurrently.
 *
 * This program is used to mimic some properties of reading from a file.
 * The value array is split into different chunks that correspond to
 * file objects. For each chunk (fobj) the sum is caclulated in a
 * different thread, mimicing the read access to a file. The sum of
 * each chunk corresponds to the return value of file read operation
 * (for instance for prematurely getting an EOF).
 */

#include <stdlib.h>
#include <stdio.h>

#include "stage1.h"
#include "stage2.h"
#include "workqueue.h"
#include "logger.h"

#define NUM_DATA 1024
#define NUM_REQS 32

int
main(int argc, char **argv)
{
	double *data;
	double sum = 0.0;
	int i;

	/*** Initialization part. The logger and the workqueues are setup ***/

	log_init(&logit, stdout_log, LOGLEV_DEBUG, NULL);
	logit.func_align = 28;

	/* For now we ignore the work queue type */
	workqueue_init(&stage1, "stage1", 0, stage1_funcs, 1);
	workqueue_init(&stage2, "stage2", 0, stage2_funcs, 8);

	/*** Prepare the compute data ***/

	data = (double *)malloc(sizeof(double) * NUM_DATA);
	if (!data)
		goto out;
	for (i=0; i<NUM_DATA; i++)
		data[i] = (double)i;

	/*** And calculate the sum ***/

	/* In the end initiating work in several stages should look
	   as simple as a function call. */
	sum = stage1_work(data, NUM_DATA, NUM_REQS);

	printf("Sum %g\n", sum);
	
out:
	/* Stop and free the workqueues */

	workqueue_stop(&stage2);
	workqueue_stop(&stage1);
	workqueue_del_contents(&stage1);
	workqueue_del_contents(&stage2);

	return 0;
}

