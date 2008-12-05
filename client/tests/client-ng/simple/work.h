#ifndef _WORK_H
#define _WORK_H

#include "request.h"

#define TEST_WORK         0x0000
#define TEST_STEP1        0x0000
#define TEST_STEP2        0x0001

extern void step1(struct req *req);
extern void step2(struct req *req);

#endif
