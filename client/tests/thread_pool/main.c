#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

#include <thread_pool.h>

#define NUM_THREAD_BLOCKS      10
#define NUM_THREADS_PER_BLOCK  10


pthread_attr_t *xpattr;

void *
thr_func1(void *arg)
{
	unsigned int sleep_secs = *((unsigned int *)arg);
	 
	printf("Started thread function 1. Waiting for %d secs\n", sleep_secs);
	while((sleep_secs = sleep(sleep_secs)) > 0);
	
	return NULL;
}

void *
thr_func2(void *arg)
{
	unsigned int sleep_secs = *((unsigned int *)arg);
	
	printf("Started thread function 2. Waiting for %d secs\n", sleep_secs);
	while((sleep_secs = sleep(sleep_secs)) > 0);
	
	return NULL;
}

int
main(int argc, char **argv)
{
	struct thread_pool *tp;
	int i, j, gi;
	unsigned int sleep_time[NUM_THREAD_BLOCKS * NUM_THREADS_PER_BLOCK];
	void *(*func)(void *);
	
	tp = thread_pool_new(5);
	
	for(i=0; i<NUM_THREAD_BLOCKS; i++) {
		for(j=0; j<NUM_THREADS_PER_BLOCK; j++) {
			if(j % 2 == 0) func = thr_func1;
			else func = thr_func2;
			gi = i*NUM_THREADS_PER_BLOCK + j;
			sleep_time[gi] = (unsigned int)(20.0*drand48());
			if(thread_pool_new_thread(tp, func, (void *)&sleep_time[gi]) == -1) {
				fprintf(stderr, "Cannot create new thread\n");
			}
		}
		sleep(1);
	}
	
	thread_pool_wait_for_all(tp);
	thread_pool_delete(tp);
	tp = NULL;
	
	return 0;
}

