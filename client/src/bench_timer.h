/* Copyright (c) 2007, 2008  Matthias Hess, Erich Focht
   This file is part of XtreemFS.

   XtreemFS is part of XtreemOS, a Linux-based Grid Operating
   System, see <http://www.xtreemos.eu> for more details. The
   XtreemOS project has been developed with the financial support
   of the European Commission's IST program under contract
   #FP6-033576.

   XtreemFS is free software: you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation, either version 2 of
   the License, or (at your option) any later version.

   XtreemFS is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of 
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with XtreemFS.  If not, see <http://www.gnu.org/licenses/>.
 */


/*
C Interface: bench_timer

Description: 


Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_BENCH_TIMER_H__
#define __XTRFS_BENCH_TIMER_H__

#include <sys/time.h>

#include <pthread.h>

#include "lock_utils.h"

struct bench_timer {
	struct timeval start_time;
	struct timeval stop_time;
	double precision;
	double overhead;
};

#ifdef TIMINGS
extern struct bench_timer *bench_timer_new();
extern int bench_timer_init(struct bench_timer *bt);

extern void bench_timer_start(struct bench_timer *bt);
extern void bench_timer_stop(struct bench_timer *bt);
extern double bench_timer_s(struct bench_timer *bt);
extern double bench_timer_ms(struct bench_timer *bt);
#else
#define bench_timer_new() NULL
#define bench_timer_init(x) do { } while(0)
#define bench_timer_start(x)
#define bench_timer_stop(x)
#define bench_timer_s(x) 0.0
#define bench_timer_ms(x) 0.0
#endif


struct bench_data_collection {
	int max_num_data;
	int num_data;
	double *data;
	spinlock_t lock;
};

#ifdef TIMINGS
extern struct bench_data_collection *bench_data_collection_new();
extern int bench_data_collection_init(struct bench_data_collection *bdc);
extern void bench_data_collection_clear(struct bench_data_collection *bdc);
extern void bench_data_collection_delete(struct bench_data_collection *bdc);

extern void bench_data_collection_add(struct bench_data_collection *bdc, double value);
extern double bench_data_collection_avg(struct bench_data_collection *bdc);
extern double bench_data_collection_sum(struct bench_data_collection *bdc);
#else
#define bench_data_collection_new() NULL
#define bench_data_collection_init(x) do { } while(0)
#define bench_data_collection_clear(x)
#define bench_data_collection_delete(x)
#define bench_data_collection_add(x,y)
#define bench_data_collection_avg(x) 0.0
#define bench_data_collection_sum(x) 0.0
#endif


struct bench_timings {
	struct bench_data_collection times_ms;
};

#ifdef TIMINGS
extern struct bench_timings *bench_timings_new();
extern int bench_timings_init(struct bench_timings *bt);
extern void bench_timings_clear(struct bench_timings *bt);
extern void bench_timings_delete(struct bench_timings *bt);

extern void bench_timings_add_s(struct bench_timings *bt, double time_s);
extern void bench_timings_add_ms(struct bench_timings *bt, double time_ms);
 
extern double bench_timings_avg_s(struct bench_timings *bt);
extern double bench_timings_avg_ms(struct bench_timings *bt);

extern double bench_timings_sum_s(struct bench_timings *bt);
extern double bench_timings_sum_ms(struct bench_timings *bt);
#else
#define bench_timings_new() NULL
#define bench_timings_init(x) do { } while(0)
#define bench_timings_clear(x) do { } while(0)
#define bench_timings_delete(x) do { } while(0)
#define bench_timings_add_s(x,y) do { } while(0)
#define bench_timings_add_ms(x,y) do { } while(0)
#define bench_timings_avg_s(x) 0.0
#define bench_timings_avg_ms(x) 0.0
#define bench_timings_sum_s(x) 0.0
#define bench_timings_sum_ms(x) 0.0
#endif

#endif
