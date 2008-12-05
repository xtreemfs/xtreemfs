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
   C++ Interface: xcap_inv

   Description: 


   Author: Matthias Hess <mhess@hpce.nec.com>, (C) 2007

   Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_XCAP_INV_H__
#define __XTRFS_XCAP_INV_H__

#include <pthread.h>

#include "xcap.h"

struct xcap_inv {
	struct list_head renewals;
	spinlock_t lock;
	long earliest_renewal;		/*!< When must be the earliest next renewal? */
	long last_signal;		/*!< When did we get the last signal? */
	long last_renewal_duration;	/*!< Keep track of how long the last renewal took */
	long longest_renewal_duration;	/*!< What is the longest renewal time that we have seen. */
};

extern struct xcap_inv xcap_inv;

void xcap_inv_init(struct xcap_inv *xi);
void xcap_inv_clear(struct xcap_inv *xi);
void xcap_inv_destroy(struct xcap_inv *xi);

void xcap_inv_sighandler(int signum);
int xcap_inv_add(struct xcap_inv *xr, struct xcap *xc, int lock);
int xcap_inv_remove(struct xcap_inv *xi, struct xcap *xc);
struct xcap *xcap_inv_find(struct xcap_inv *xi, char *fileid);
int xcap_inv_renew_immediate(struct xcap_inv *xi,
			     char *fileid, struct xcap *xc,
			     off_t new_size, int epoch, int order, int lock);
struct xcap *xcap_inv_exchange(struct xcap_inv *xi, char *fileid, struct xcap *new_xcap);

void xcap_inv_print(struct xcap_inv *xi);

#endif
