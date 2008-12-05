/*
*  C Implementation: main
*
* Description: 
*
*
* Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

#include "bench_timer.h"


int
main(int argc, char **argv)
{
	struct bench_timer test_timer;
	int to_sleep = 20;
	
	bench_timer_init(&test_timer);
	
	printf("Precision: %g ms\n", test_timer.precision);
	
	bench_timer_start(&test_timer);
	while((to_sleep = sleep(to_sleep)) > 0);
	bench_timer_stop(&test_timer);
	
	printf("Waited %g s\n", bench_timer_s(&test_timer));
	
	return 0;
}
