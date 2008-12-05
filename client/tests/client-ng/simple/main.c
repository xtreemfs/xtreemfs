#include <stdlib.h>

#include "work.h"
#include "workqueue.h"
#include "request.h"
#include "logger.h"

struct work_queue stage;

int
main(int argc, char **argv)
{
	int res;
	int i;

	dbg_msg("Init queue for test stage.\n");
	workqueue_init(&stage, "test", 1);
	workqueue_add_func(&stage, TEST_STEP1, step1);
	workqueue_add_func(&stage, TEST_STEP2, step2);

	dbg_msg("Doing actual work.\n");
	for (i=0; i<10000; i++) {
		res = do_work(i);
		printf("Do work returned: %d\n", res);
	}

	dbg_msg("Stopping queue for stage.\n");
	workqueue_stop(&stage);
	workqueue_del_contents(&stage);

	return 0;
}

