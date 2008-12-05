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
C Interface: xcap

Description: 


Author: Matthias Hess <mhess at hpce dot nec dot de>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_XCAP_H__
#define __XTRFS_XCAP_H__

#include <pthread.h>

#include "creds.h"
#include "list.h"
#include "lock_utils.h"

/**
 * XCapability structure.
 */
struct xcap {
	char *fileID;
	char *accessMode;
	long expires;
	char *clientIdentity;
	int truncateEpoch;
	char *serverSignature;
	char *repr;		/*!< String representation of xcap */
	struct creds creds;
	spinlock_t lock;
	struct list_head head;
};

extern struct xcap *xcap_new();
extern int xcap_init(struct xcap *xc);
extern void xcap_clear(struct xcap *xc);
extern void xcap_delete(struct xcap *xc);
extern int xcap_values_from_str(struct xcap *xc, char *val_str);
extern int xcap_copy_values_from_str(struct xcap *xc, char *val_str);

extern void xcap_copy(struct xcap *dst, struct xcap *src);

extern int xcap_check_sign(struct xcap *xc);

extern struct json_object *xcap_to_json(struct xcap *xc);
extern struct xcap *json_to_xcap(struct json_object *jo);
extern struct xcap *str_to_xcap(char *str);
extern char *xcap_to_str(struct xcap *xc);

extern void xcap_print(struct xcap *xc);

#endif
