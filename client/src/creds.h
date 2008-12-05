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
 C Interface: creds

 Description: 


 Author: Matthias Hess <mhess@hpce.nec.com>, (C) 2007

 Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_CREDS_H__
#define __XTRFS_CREDS_H__

#include <unistd.h>
#include <sys/types.h>

#include <json.h>

#if defined(_WIN32) && !defined(_FUSE_WIN_H_)
#define UID_GID_DEF
typedef unsigned int gid_t;
typedef unsigned int uid_t;
#endif

/**
 * Credentials used in MRC communication.
 * Passed in to requests, come either from the fuse context or the local
 * process.
 */
struct creds {
	uid_t uid;	/** User ID of the calling process */
	gid_t gid;	/** Group ID of the calling process */
	pid_t pid;	/** Thread ID of the calling process */

	/** grid identification certificate stuff should be added below */
	char  *guid;	/** Global user id */
	char  *ggid;	/** Global group id */
	char  *subgrp;
};

extern int creds_init(struct creds *c);
extern void creds_delete_content(struct creds *c);

extern int json_to_creds(struct creds *c, struct json_object *jo);
extern struct json_object *creds_to_json(struct creds *c);
extern char *creds_to_str(struct creds *c);
extern int creds_to_str_ip(struct creds *c, char *str, int n);

extern int creds_copy(struct creds *dest, struct creds *src);

extern int creds_dbg(struct creds *c);

#endif
