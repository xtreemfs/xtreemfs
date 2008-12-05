#ifndef _STAGE1_H
#define _STAGE1_H

#include "workqueue.h"

#define STAGE1_WORK      0x0000
#define STAGE1_STEP1     0x0000
#define STAGE1_FINISH    0x0001

struct stage1_step1_data {
	int num_values;
	double *values;
	int num_reqs;
};

struct stage1_step1_resp {
	int num_reqs;
	double *sums;
};

extern struct work_queue  stage1;
extern wq_func_t          stage1_funcs[];

extern void stage1_step1(struct req *req);
extern void stage1_step2(struct req *req);
extern void stage1_finish(struct req *req);

extern double stage1_work(double *values, int num_values, int num_reqs);

#endif
