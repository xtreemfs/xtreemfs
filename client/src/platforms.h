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

#ifndef __XTRFS_PLATFORMS_H__
#define __XTRFS_PLATFORMS_H__


#ifdef _WIN32
int geteuid();
int getegid();

// simple syslog.h replacement
#include <stdio.h>
#include <stdarg.h>
#define LOG_NOWAIT 0
#define LOG_INFO 0
#define LOG_USER 0
#define LOG_PID 0
int openlog(const char* ident, int logopt, int facility);
void vsyslog(int priority, const char* message, va_list args);
void closelog();
void setlinebuf(FILE* );
#endif

#ifdef __APPLE__
#include <unistd.h>
#include <libkern/OSAtomic.h>
#include <pthread.h>

typedef off_t loff_t;

typedef pthread_mutex_t pthread_spinlock_t;

int pthread_spin_init(pthread_spinlock_t *__lock, int __pshared);

int pthread_spin_destroy(pthread_spinlock_t *__lock);

int pthread_spin_lock(pthread_spinlock_t *__lock);

int pthread_spin_trylock (pthread_spinlock_t *__lock);

int pthread_spin_unlock (pthread_spinlock_t *__lock);

#endif


#endif
