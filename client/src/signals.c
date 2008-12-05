/* Copyright (c) 2007, 2008  Erich Focht, Matthias Hess
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


/**
 * Signal handlers.
 *
 * SIGUSR1: display state information of all workqueues and requests.
 *
 * @author (c) 2007 Erich Focht <efocht at hpce dot nec dot com>
 */

#include <signal.h>
#include <string.h>
#include <pthread.h>

#include "xtreemfs.h"
#include "workqueue.h"
#include "filerw.h"
#include "stripe.h"
#include "logger.h"
#include "mrc_comm.h"

#ifndef _WIN32
/* static struct sigaction oldsigusr1, oldalrm; */
static sigset_t newsigset, oldsigset;
static pthread_t usr1_thread;

static atomic_t sighandler_running;

/**
 * Signal handler for SIGUSR1
 *
 * When a signal is received print the workqueue states to the log.
 * Multi-threaded signal handling can be quite tricky... especially
 * with the simple locking schemes we use in XtreemFS. So we introduce
 * a new thread that only awakens on a USR1 signal.
 */
static void *usr1_sighandler_thread(void *data)
{
	sigset_t newset;
	int signum;

	/* USR1 Signals delivered during printing will be ignored. */
	
	while(atomic_read(&sighandler_running)) {
		sigemptyset(&newset);
		sigaddset(&newset, SIGUSR1);\
		if (!sigwait(&newset, &signum) && atomic_read(&sighandler_running)) {
			if (signum != SIGUSR1) {
				err_msg("USR1 handler received a signal that it does not deal with!\n");
				break;
			}
			info_msg("USR1USR1USR1USR1USR1USR1USR1USR1USR1USR1\n");
			workqueue_print(mrccomm_wq, print_mrccomm_info);
			workqueue_print(filerw_wq, print_filerw_info);
			workqueue_print(fobj_wq, print_fobj_info);
			workqueue_print(sobj_wq, print_sobj_info);
			file_inv_print(&file_inv);
			xcap_inv_print(&xcap_inv);
			info_msg("USR1USR1USR1USR1USR1USR1USR1USR1USR1USR1\n");
		}
	}
	return NULL;
}
#endif /* _WIN32 */

/**
 * Install signal handler.
 *
 */
int sighandler_install(void)
{
	int err = 0;
#ifndef _WIN32
	/* struct sigaction sa; */

	/* This is not really part of setting up the usr1 sig handler...
	   ... maybe it should better go into the init of xtreemfs */
	sigemptyset(&newsigset);
	sigaddset(&newsigset, SIGALRM);
	sigaddset(&newsigset, SIGUSR1);
	pthread_sigmask(SIG_BLOCK, &newsigset, &oldsigset);

	atomic_set(&sighandler_running, 1);
	pthread_create(&usr1_thread, NULL, usr1_sighandler_thread, NULL);
	
	/* Setting up a simple signal handler might lead to deadlocks if the
	   handler tries to lock a mutex or spin lock that the interrupted
	   thread already had locked before  So we will remove the following
	   entries. */
	
#if 0
	memset((void *)&sa, 0, sizeof(sa));
	sa.sa_handler = usr1_sighandler;
	sigfillset(&sa.sa_mask);
	err = sigaction(SIGUSR1, &sa, &oldsigusr1);
	if (!err) {
		info_msg("Initialized SIGUSR1 signal handler.\n");
		info_msg("Old handler was set to %p\n",	oldsigusr1.sa_handler);
	} else
		err_msg("Failed to initialize SIGUSR1 signal handler.\n");
#endif

#if 0
	sa.sa_handler = SIG_IGN;
	sigfillset(&sa.sa_mask);
	err = sigaction(SIGALRM, &sa, &oldalrm);
	if (!err)
		info_msg("Initialized SIGALRM handler.\n");
	else
		err_msg("Failed to initialize SIGALRM signal handler.\n");
#endif
#endif

	return err;
}

void sighandler_stop(void)
{
	atomic_set(&sighandler_running, 0);
	pthread_kill(usr1_thread, SIGUSR1);
	pthread_join(usr1_thread, NULL);
}
