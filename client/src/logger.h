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


#ifndef __XTRFS_LOGGER_H__
#define __XTRFS_LOGGER_H__

#include <stdio.h>
#include <pthread.h>


typedef enum {
	LOGLEV_NO_LOG=0,
	LOGLEV_ERROR,
	LOGLEV_INFO,
	LOGLEV_TRACE,
	LOGLEV_DEBUG,
} log_level_t;

typedef enum {
	stdout_log,
	syslog_log,
	file_log
} log_type_t;

struct logger {
	log_level_t level;     /*!< Current level of messages. All messages above this
				  level are discarded.                               */
	log_type_t type;       /*!< Type of current logging facility */
	FILE *logfile;
	int lock_init;         /*!< Cannot assign init'ed mutex. So keep track of init */
	pthread_mutex_t lock;
	int func_align;
};

extern int log_init(struct logger *l, log_type_t type, log_level_t level, char *filename);
extern int log_msg(struct logger *l, log_level_t level, char *func, const char *format, ...);
extern void log_set_level(struct logger *l, log_level_t level);

extern void log_end(struct logger *l);

extern struct logger logit;  /*!< Globally usable logger, thread safe */

#define log_on() log_set_level(&logit, LOGLEV_DEBUG)
#define log_off() log_set_level(&logit, LOGLEV_NO_LOG)

#ifdef __GNUC__

#ifndef NO_DEBUG
#define dbg_msg(f,...)   log_msg(&logit, LOGLEV_DEBUG, (char *)__FUNCTION__, \
				 f, ##__VA_ARGS__)
#else
#define dbg_msg(f,...)   do { } while(0)
#endif

#ifndef NO_TRACE
#define trace_msg(f,...) log_msg(&logit, LOGLEV_TRACE, (char *)__FUNCTION__, \
				 f, ##__VA_ARGS__)
#else
#define trace_msg(f,...) do { } while(0)
#endif

#define force_msg(f,...) log_msg(&logit, LOGLEV_NO_LOG, (char *)__FUNCTION__, \
				 f, ##__VA_ARGS__)

#define err_msg(f,...)   log_msg(&logit, LOGLEV_ERROR, (char *)__FUNCTION__, \
				 f, ##__VA_ARGS__)

#define info_msg(f,...)  log_msg(&logit, LOGLEV_INFO, (char *)__FUNCTION__, \
				  f, ##__VA_ARGS__)

/* This is for ordinary information that have to be printed
   without the function name. */
#define print_msg(f,...) log_msg(&logit, LOGLEV_INFO, NULL, f, ##__VA_ARGS__)

#ifdef TIMINGS
#define timing_msg(f,...) log_msg(&logit, LOGLEV_TRACE, (char *)__FUNCTION__, \
				  f, ##__VA_ARGS__)
#else
#define timing_msg(f,...)
#endif

#endif

#endif
