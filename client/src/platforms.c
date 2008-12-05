/* Copyright (c) 2008  Felix Hupfeld
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
* C Interface: xtreemfs
* 
* Description: 
*
*
* Author: Felix Hupfeld <hupfeld at zib dot de>, (C) 2008
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdio.h>
#include <stdlib.h>

#include "platforms.h"

#ifdef _WIN32
int 
geteuid() 
{
	return 0;
}

int 
getegid() 
{
	return 0;
}

static FILE* syslog_file;

int 
openlog(const char* ident, int logopt, int facility) 
{
	char logname[50];
	snprintf(logname,50,"%s.syslog",ident);
	syslog_file = fopen(logname, "a+");
	if(!syslog_file) {
		printf("abort: can not open %s\n", logname);
		abort();
	}
	return 0;
}

void 
closelog() 
{
	fclose(syslog_file);
}

void 
vsyslog(int priority, const char* message, va_list args) 
{
	vfprintf(syslog_file, message, args);
}

void setlinebuf(FILE* f) {}

void sleep(int time)
{
	_sleep(time);
}

#define boolean win_boolean
#include <winsock2.h>

int usleep(long usec)
{
    struct timeval tv;
    fd_set dummy;
    SOCKET s = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    FD_ZERO(&dummy);
    FD_SET(s, &dummy);
    tv.tv_sec = usec/1000000L;
    tv.tv_usec = usec%1000000L;
    return select(0, 0, 0, &dummy, &tv);
}
#endif /* WIN32 */

#ifdef __APPLE__

int pthread_spin_init(pthread_spinlock_t *__lock, int __pshared) {
        pthread_mutex_init(__lock,NULL);
        return 0;
}

int pthread_spin_destroy(pthread_spinlock_t *__lock) {
        pthread_mutex_destroy(__lock);
        return 0;
}

int pthread_spin_lock(pthread_spinlock_t *__lock) {
        return pthread_mutex_lock(__lock);
}

int pthread_spin_trylock (pthread_spinlock_t *__lock) {
        return pthread_mutex_trylock(__lock);
}

int pthread_spin_unlock (pthread_spinlock_t *__lock) {
        return pthread_mutex_unlock(__lock);
} 

#endif
