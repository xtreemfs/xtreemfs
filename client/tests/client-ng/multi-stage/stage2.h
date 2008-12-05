#ifndef _STAGE2_H
#define _STAGE2_H

#include "workqueue.h"

#define STAGE2_WORK      0x0000
#define STAGE2_STEP1     0x0000
#define STAGE2_STEP2     0x0001

struct stage2_step1_data {
	int num_values;
	double *values;
	int chunk;
};

struct stage2_step1_resp {
	int *which;
	double *res;
};

extern struct work_queue  stage2;
extern wq_func_t          stage2_funcs[];

extern void stage2_step1(struct req *req);

extern struct req *stage2_work_req(double *values, int num_values, int chunk,
				   double *res, int *which,
				   struct req *parent);

#endif
