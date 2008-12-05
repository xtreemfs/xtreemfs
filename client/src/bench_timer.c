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
*  C Implementation: bench_timer
*
* Description: 
*
*
* Author: Matthias Hess <mhess at hpce dot nec dot de>, Copyright 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>

#include <sys/time.h>

#include "bench_timer.h"


#ifdef TIMINGS

struct bench_timer *
bench_timer_new()
{
	struct bench_timer *rv = NULL;
	
	rv = (struct bench_timer *)malloc(sizeof(struct bench_timer));
	if (rv && bench_timer_init(rv)) {
		free(rv);
		rv = NULL;
	}
	
	return rv;
}

int
bench_timer_init(struct bench_timer *bt)
{
	int rv = 0;
	double prec = 0.0;
	
	bench_timer_start(bt);
	do {
		bench_timer_stop(bt);
		prec = bench_timer_ms(bt);
	} while(prec == 0.0);
	
	bt->precision = prec;
	
	return rv;
}

void
bench_timer_start(struct bench_timer *bt)
{
	gettimeofday(&bt->start_time, NULL);
}

void
bench_timer_stop(struct bench_timer *bt)
{
	gettimeofday(&bt->stop_time, NULL);
}

double
bench_timer_s(struct bench_timer *bt)
{
	double rv = 0.0;
	
	rv = (double)bt->stop_time.tv_sec + (double)bt->stop_time.tv_usec / 1000000.0;
	rv -= ((double)bt->start_time.tv_sec + (double)bt->start_time.tv_usec / 1000000.0);
	
	return rv;
}

double
bench_timer_ms(struct bench_timer *bt)
{
	return bench_timer_s(bt) * 1000.0;
}


struct bench_data_collection *
bench_data_collection_new()
{
	struct bench_data_collection *rv = NULL;
	
	rv = (struct bench_data_collection *)malloc(sizeof(struct bench_data_collection));
	if(rv && bench_data_collection_init(rv)) {
		bench_data_collection_delete(rv);
		rv = NULL;
	}
	
	return rv;
}

int
bench_data_collection_init(struct bench_data_collection *bdc)
{
	bdc->max_num_data = 0;
	bdc->num_data     = 0;
	bdc->data         = NULL;
	spin_init(&bdc->lock, PTHREAD_PROCESS_SHARED);
	return 0;
}

void
bench_data_collection_clear(struct bench_data_collection *bdc)
{
	free(bdc->data);
	bdc->num_data = 0;
	bdc->max_num_data = 0;
}

void
bench_data_collection_delete(struct bench_data_collection *bdc)
{
	if(bdc) {
		bench_data_collection_clear(bdc);
		spin_destroy(&bdc->lock);
		free(bdc);
	}
}

void
bench_data_collection_add(struct bench_data_collection *bdc, double value)
{
	pthread_spin_lock(&bdc->lock);
	if(bdc->num_data >= bdc->max_num_data) {
		bdc->max_num_data += 100;
		bdc->data = (double *)realloc((void *)bdc->data,
					      sizeof(double) * bdc->max_num_data);
	}
	bdc->data[bdc->num_data++] = value;
	spin_unlock(&bdc->lock);
}

double
bench_data_collection_avg(struct bench_data_collection *bdc)
{
	double rv = 0.0;
	int i;
	
	for(i=0; i<bdc->num_data; i++) rv += bdc->data[i];
	if(i > 0) rv /= (double)i;
	
	return rv;
}

double
bench_data_collection_sum(struct bench_data_collection *bdc)
{
	double rv = 0.0;
	int i;
	
	for(i=0; i<bdc->num_data; i++) rv += bdc->data[i];
	
	return rv;
}


struct bench_timings *
bench_timings_new()
{
	struct bench_timings *rv = NULL;
	
	rv = (struct bench_timings *)malloc(sizeof(struct bench_timings));
	if (rv && bench_timings_init(rv)) {
		bench_timings_delete(rv);
		rv = NULL;
	}
	
	return rv;
}

int
bench_timings_init(struct bench_timings *bt)
{
	int rv = 0;
	rv = bench_data_collection_init(&bt->times_ms);	
	return rv;
}

void
bench_timings_clear(struct bench_timings *bt)
{
	bench_data_collection_clear(&bt->times_ms);
}

void
bench_timings_delete(struct bench_timings *bt)
{
	bench_timings_clear(bt);
	free(bt);
}


void
bench_timings_add_s(struct bench_timings *bt, double time_s)
{
	bench_data_collection_add(&bt->times_ms, time_s * 1000.0);
}

void
bench_timings_add_ms(struct bench_timings *bt, double time_ms)
{
	bench_data_collection_add(&bt->times_ms, time_ms);
}


double
bench_timings_avg_s(struct bench_timings *bt)
{
	return bench_timings_avg_ms(bt) / 1000.0;
}

double
bench_timings_avg_ms(struct bench_timings *bt)
{
	return bench_data_collection_avg(&bt->times_ms);
}

double
bench_timings_sum_s(struct bench_timings *bt)
{
	return bench_timings_sum_ms(bt) / 1000.0;
}

double
bench_timings_sum_ms(struct bench_timings *bt)
{
	return bench_data_collection_sum(&bt->times_ms);
}

#endif
