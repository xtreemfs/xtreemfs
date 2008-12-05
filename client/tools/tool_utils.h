/* Copyright (c) 2007, 2008  Matthias Hess
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
C Interface: tool_utils

Description: 

This is a collection of tools that are shared between different
tools. Multiple implementations of the same functionality are to
be prevented. This collection is tool specific and cannot be used
for other components. There is xtreemfs_utils for such things.

Author: Matthias Hess <mhess@hpce.nec.com>, (C) 2007

Copyright: See COPYING file that comes with this distribution

*/

#ifndef __XTRFS_TOOL_UTILS_H__
#define __XTRFS_TOOL_UTILS_H__

#include <errno.h>

#include <stripingpolicy.h>


enum error_causes {
	ERR_NO_ERROR=0,
	ERR_ONLY_POLICY,
	ERR_NO_STRIPE_WIDTH,
	ERR_STRIPE_SIZE_NOT_A_NUMBER,
	ERR_STRIPE_WIDTH_NOT_A_NUMBER,
	ERR_TOO_MANY_ARGUMENTS,
	ERR_NO_VOLUME_URL,
	ERR_NO_MEM,
	ERR_UNKNOWN_POLICY,
	ERR_UNSUPPORTED_SCHEME,
	ERR_PARSE_ERROR
};

extern char *progname(char *arg);

extern int analyse_striping_policy_str_ip(char *sp_str, int *err, struct striping_policy *sp);

#endif
