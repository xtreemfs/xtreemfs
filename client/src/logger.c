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
*  C Implementation: logger
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
#include <stdarg.h>
#include <string.h>

#ifndef _WIN32
#include <syslog.h>
#else
#include <platforms.h>
#endif

#include "logger.h"

struct logger logit = {
	.level          = LOGLEV_DEBUG,
	.type           = stdout_log,
	.logfile        = NULL,
	.lock_init      = 0,
	.func_align     = 24
};


int
log_init(struct logger *l, log_type_t type, log_level_t level, char *filename)
{
	l->logfile = NULL;
	l->lock_init = 0;
	
	switch(type) {
	case syslog_log:
		openlog("xtreemfs", LOG_NOWAIT, LOG_USER);
		break;
	case file_log:
		if (filename) {
			l->logfile = fopen(filename, "w");
			setlinebuf(l->logfile);
		}
		break;
	default:
		break;
	}
	
	l->level = level;
	l->type  = type;
	
	pthread_mutex_init(&l->lock, NULL);
	l->lock_init = 1;
	l->func_align = -1;
	
	return 0;
}

void
log_set_level(struct logger *l, log_level_t level)
{
	pthread_mutex_lock(&l->lock);
	l->level = level;
	pthread_mutex_unlock(&l->lock);
}

void
log_end(struct logger *l)
{
	switch(l->type) {
	case syslog_log:
		closelog();
		break;
	case file_log:
		if (l->logfile)
			fclose(l->logfile);
		break;
	default:
		break;
	}
	
	pthread_mutex_destroy(&l->lock);
}

int
log_msg(struct logger *l, log_level_t level, char *func, const char *format, ...)
{
	va_list arg;
	int done = 0;
	char *my_format = NULL;
	size_t my_len;
	size_t func_len;
	char *my_func = NULL;
	pthread_t my_thread = pthread_self();
	char my_thread_str[23];
	int i;

	if (level <= l->level) {

		/* In case 'log_init' was not called. */
		if(!l->lock_init) {
			pthread_mutex_init(&l->lock, NULL);
			l->lock_init = 1;
		}

		pthread_mutex_lock(&l->lock);	
		va_start(arg, format);
		if (func != NULL) {
			func_len = strlen(func);
			if(l->func_align > 0 && func_len + 3 < l->func_align) {
				my_func = (char *)malloc(l->func_align+1);
				snprintf(my_func, l->func_align+1, "(%s):", func);
				func_len = strlen(my_func);
				for(i=func_len; i<l->func_align; i++)
					my_func[i] = ' ';
				my_func[i] = '\0';
			} else {
				my_func = (char *)malloc(func_len + strlen("():") + 1);
				snprintf(my_func, func_len+strlen("():")+1, "(%s):", func);
			}
			func_len = strlen(my_func);
			my_thread = pthread_self();
#ifdef _WIN32
			snprintf(my_thread_str, 23, "[%04x]", ((int **)&my_thread)[0][0]);
#else
			snprintf(my_thread_str, 23, "[%16p]", (void *)my_thread);
#endif
			my_len = strlen(format) + strlen(my_thread_str) + 2 + func_len + strlen(" ") + 1;
			my_format = (char *)malloc(my_len);
			if(my_format == NULL)
				goto finish;
			snprintf(my_format, my_len, "%s: %s %s", my_thread_str, my_func, format);
			free(my_func);
		} else
			my_format = strdup(format);

#if 1
		switch(l->type) {
		case stdout_log:
			done = vfprintf(stdout, (const char *)my_format, arg);
			fflush(stdout);
			break;
		case syslog_log:
			vsyslog(LOG_INFO, (const char *)my_format, arg);
			done = 1;
			break;
		case file_log:
			if(l->logfile) {
				done = vfprintf(l->logfile, (const char *)my_format, arg);
				fflush(l->logfile);
			}
			break;
		default:
			break;
		}
#endif
		free(my_format);
		va_end(arg);
		pthread_mutex_unlock(&l->lock);
	}
	
finish:
	return done;
}
