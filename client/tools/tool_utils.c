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
*  C Implementation: tool_utils
*
* Description: 
*
*
* Author: Matthias Hess <mhess@hpce.nec.com>, (C) 2007
*
* Copyright: See COPYING file that comes with this distribution
*
*/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "tool_utils.h"

char *
progname(char *arg)
{
	char *rv;
	char *basename;
	
	basename = strrchr(arg, '/');
	if(basename != NULL)
		rv = strdup(&basename[1]);
	else
		rv = strdup(arg);
		
	return rv;
}

int
analyse_striping_policy_str_ip(char *sp_str, int *err, struct striping_policy *sp)
{
	int rv = 0;
	char *i_str;
	char *endptr;
	char *policy_name = NULL;
	int stripe_size, stripe_width;
	int i;
	
	*err = 0;
	
	
	/* Analyse policy string */
	
	i_str = strsep(&sp_str, ",");
	if(sp_str == NULL) {
		*err = ERR_ONLY_POLICY;
		goto finish;
	}
	policy_name = strdup(i_str);
	
	/* Do we know the policy? */
	for(i=0; striping_policy_mapping[i].name != NULL; i++) {
		if(!strcmp(striping_policy_mapping[i].name, policy_name)) {
			sp->id = striping_policy_mapping[i].id;
			break;
		}
	}
	if(striping_policy_mapping[i].name == NULL) {
		*err = ERR_UNKNOWN_POLICY;
		goto finish;
	}
	
	/* Stripe size */
	
	i_str = strsep(&sp_str, ",");
	if(sp_str == NULL) {
		*err = ERR_NO_STRIPE_WIDTH;
		goto finish;
	}
	errno = 0;
	stripe_size = strtol(i_str, &endptr, 10);
	if(*endptr != '\0') {
		*err = ERR_STRIPE_SIZE_NOT_A_NUMBER;
		goto finish;
	}
	
	
	i_str = strsep(&sp_str, ",");
	if(sp_str == NULL) {    /* This is good! No more arguments expected */
		errno = 0;
		stripe_width = strtol(i_str, &endptr, 10);
		if(*endptr != '\0') {
			*err = ERR_STRIPE_WIDTH_NOT_A_NUMBER;
			goto finish;
		}
	}
	else {
		*err = ERR_TOO_MANY_ARGUMENTS;
		goto finish;
	}
			
	sp->stripe_size  = stripe_size * 1024;
	sp->width        = stripe_width;
	
finish:
	free(policy_name);
	
	return rv;
}
